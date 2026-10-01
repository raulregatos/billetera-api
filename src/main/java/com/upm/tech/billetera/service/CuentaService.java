package com.upm.tech.billetera.service;

import com.upm.tech.billetera.model.Cuenta;
import com.upm.tech.billetera.model.Transaccion;
import com.upm.tech.billetera.model.TipoTransaccion;
import com.upm.tech.billetera.model.Usuario;
import com.upm.tech.billetera.exception.CuentaNoEncontradaException;
import com.upm.tech.billetera.dto.CuentaRelacionadaResponse;
import com.upm.tech.billetera.dto.CuentaResponse;
import com.upm.tech.billetera.dto.DireccionTransaccion;
import com.upm.tech.billetera.dto.TransaccionResponse;
import com.upm.tech.billetera.dto.TransaccionesPageResponse;
import com.upm.tech.billetera.repository.CuentaRepository;
import com.upm.tech.billetera.repository.TransaccionRepository;
import com.upm.tech.billetera.repository.TransaccionSpecifications;
import com.upm.tech.billetera.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CuentaService {

    private static final int TAMANO_PAGINA_MAXIMO = 100;

    // 1. Inyección de dependencias
    // Usamos el repositorio para hablar con la BD. Lo inyectamos por constructor (es la mejor práctica).
    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;

    public CuentaService(CuentaRepository cuentaRepository, TransaccionRepository transaccionRepository,
                         UsuarioRepository usuarioRepository) {
        this.cuentaRepository = cuentaRepository;
        this.transaccionRepository = transaccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // 2. Método de lectura simple (no modifica datos)
    public CuentaResponse obtenerCuenta(Long id, String nombreUsuario) {
        // Una cuenta inexistente y una ajena devuelven el mismo 404 para no revelar su existencia.
        Cuenta cuenta = cuentaRepository.findByIdAndUsuarioNombreUsuario(id, nombreUsuario)
                .orElseThrow(() -> new CuentaNoEncontradaException(id));
        return convertirCuenta(cuenta);
    }

    @Transactional(readOnly = true)
    public List<CuentaResponse> listarCuentas(String nombreUsuario) {
        return cuentaRepository.findAllByUsuarioNombreUsuarioOrderByIdAsc(nombreUsuario)
                .stream().map(this::convertirCuenta).toList();
    }

    @Transactional
    public CuentaResponse crearCuenta(String titular, String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new IllegalStateException("El usuario autenticado ya no existe"));
        Cuenta cuenta = cuentaRepository.save(new Cuenta(titular.trim(), BigDecimal.ZERO, usuario));
        return convertirCuenta(cuenta);
    }

    @Transactional(readOnly = true)
    public TransaccionesPageResponse obtenerHistorial(
            Long cuentaId,
            TipoTransaccion tipo,
            LocalDate desde,
            LocalDate hasta,
            int page,
            int size,
            String nombreUsuario
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("La página debe ser mayor o igual a cero");
        }
        if (size < 1 || size > TAMANO_PAGINA_MAXIMO) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y " + TAMANO_PAGINA_MAXIMO);
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }

        obtenerCuenta(cuentaId, nombreUsuario);

        LocalDateTime desdeInclusivo = desde == null ? null : desde.atStartOfDay();
        LocalDateTime hastaExclusivo = hasta == null || hasta.equals(LocalDate.MAX)
                ? null
                : hasta.plusDays(1).atStartOfDay();

        PageRequest pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("fecha"),
                Sort.Order.desc("id")
        ));
        Specification<Transaccion> filtros = TransaccionSpecifications.deCuenta(
                cuentaId, tipo, desdeInclusivo, hastaExclusivo);
        Page<Transaccion> transacciones = transaccionRepository.findAll(filtros, pageable);

        return new TransaccionesPageResponse(
                transacciones.map(transaccion -> convertirTransaccion(transaccion, cuentaId)).getContent(),
                transacciones.getNumber(),
                transacciones.getSize(),
                transacciones.getTotalElements(),
                transacciones.getTotalPages(),
                transacciones.hasNext(),
                transacciones.hasPrevious()
        );
    }

    private TransaccionResponse convertirTransaccion(Transaccion transaccion, Long cuentaId) {
        boolean esOrigen = transaccion.getCuentaOrigen() != null
                && transaccion.getCuentaOrigen().getId().equals(cuentaId);
        DireccionTransaccion direccion = switch (transaccion.getTipo()) {
            case DEPOSITO -> DireccionTransaccion.ENTRADA;
            case RETIRO -> DireccionTransaccion.SALIDA;
            case TRANSFERENCIA -> esOrigen ? DireccionTransaccion.SALIDA : DireccionTransaccion.ENTRADA;
        };

        Cuenta cuentaRelacionada = switch (transaccion.getTipo()) {
            case TRANSFERENCIA -> esOrigen ? transaccion.getCuentaDestino() : transaccion.getCuentaOrigen();
            case DEPOSITO, RETIRO -> null;
        };
        CuentaRelacionadaResponse relacionada = cuentaRelacionada == null
                ? null
                : new CuentaRelacionadaResponse(cuentaRelacionada.getId(), cuentaRelacionada.getTitular());

        return new TransaccionResponse(
                transaccion.getId(), transaccion.getTipo(), transaccion.getFecha(), transaccion.getMonto(),
                direccion, relacionada);
    }

    private CuentaResponse convertirCuenta(Cuenta cuenta) {
        return new CuentaResponse(cuenta.getId(), cuenta.getTitular(), cuenta.getSaldo());
    }

    // 3. Método transaccional: Depositar
    @Transactional
    public CuentaResponse depositar(Long id, BigDecimal monto, String nombreUsuario) {
        // Validar que el monto sea positivo (monto > 0)
        // En BigDecimal: compareTo devuelve 1 si es mayor, 0 si es igual, -1 si es menor.
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a depositar debe ser mayor a cero");
        }

        Cuenta cuenta = cuentaRepository.findByIdAndUsuarioConBloqueo(id, nombreUsuario)
                .orElseThrow(() -> new CuentaNoEncontradaException(id));

        // Sumar al saldo: cuenta.getSaldo() + monto
        BigDecimal nuevoSaldo = cuenta.getSaldo().add(monto);
        cuenta.setSaldo(nuevoSaldo);

        Cuenta cuentaGuardada = cuentaRepository.save(cuenta);

        registrarTransaccion(null, cuentaGuardada, monto, TipoTransaccion.DEPOSITO);

        return convertirCuenta(cuentaGuardada);
    }

    @Transactional
    public CuentaResponse retirar(Long id, BigDecimal monto, String nombreUsuario) {
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a retirar debe ser mayor a cero");
        }
        Cuenta cuenta = cuentaRepository.findByIdAndUsuarioConBloqueo(id, nombreUsuario)
                .orElseThrow(() -> new CuentaNoEncontradaException(id));

        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new IllegalArgumentException("El saldo disponible debe ser mayor o igual al monto a retirar");
        }
        BigDecimal nuevoSaldo = cuenta.getSaldo().subtract(monto);
        cuenta.setSaldo(nuevoSaldo);

        Cuenta cuentaGuardada = cuentaRepository.save(cuenta);

        registrarTransaccion(cuentaGuardada, null, monto, TipoTransaccion.RETIRO);

        return convertirCuenta(cuentaGuardada);
    }

    @Transactional
    public void transferir(Long idOrigen, Long idDestino, BigDecimal monto, String nombreUsuario) {

        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a transferir debe ser mayor a cero");
        }
        if (idOrigen.equals(idDestino)) {
            throw new IllegalArgumentException("No puedes transferir dinero a la misma cuenta");
        }

        cuentaRepository.findByIdAndUsuarioNombreUsuario(idOrigen, nombreUsuario)
                .orElseThrow(() -> new CuentaNoEncontradaException(idOrigen));
        cuentaRepository.findById(idDestino)
                .orElseThrow(() -> new CuentaNoEncontradaException(idDestino));

        // 1. Evitamos Deadlocks ordenando los IDs (Bloqueamos siempre primero el ID más pequeño)
        Long primerId = idOrigen < idDestino ? idOrigen : idDestino;
        Long segundoId = idOrigen < idDestino ? idDestino : idOrigen;

        // 2. Extraemos las cuentas CON BLOQUEO (usando el nuevo método del repositorio)
        Cuenta primeraCuenta = cuentaRepository.findByIdConBloqueo(primerId)
                .orElseThrow(() -> new CuentaNoEncontradaException(primerId));

        Cuenta segundaCuenta = cuentaRepository.findByIdConBloqueo(segundoId)
                .orElseThrow(() -> new CuentaNoEncontradaException(segundoId));

        // 3. Volvemos a identificar quién era el origen y quién el destino
        Cuenta cuentaOrigen = primeraCuenta.getId().equals(idOrigen) ? primeraCuenta : segundaCuenta;
        Cuenta cuentaDestino = primeraCuenta.getId().equals(idDestino) ? primeraCuenta : segundaCuenta;

        if (cuentaOrigen.getSaldo().compareTo(monto) < 0) {
            throw new IllegalArgumentException("El saldo disponible debe ser mayor o igual al monto a retirar");
        }
        BigDecimal nuevoSaldoOrigen = cuentaOrigen.getSaldo().subtract(monto);
        cuentaOrigen.setSaldo(nuevoSaldoOrigen);
        BigDecimal nuevoSaldoDestino = cuentaDestino.getSaldo().add(monto);
        cuentaDestino.setSaldo(nuevoSaldoDestino);
        Cuenta cuentaOrigenGuardada = cuentaRepository.save(cuentaOrigen);
        Cuenta cuentaDestinoGuardada = cuentaRepository.save(cuentaDestino);

        registrarTransaccion(cuentaOrigenGuardada, cuentaDestinoGuardada, monto, TipoTransaccion.TRANSFERENCIA);
    }

    private void registrarTransaccion(Cuenta cuentaOrigen, Cuenta cuentaDestino, BigDecimal monto, TipoTransaccion tipo) {
        Transaccion transaccion = new Transaccion(cuentaOrigen, cuentaDestino, monto, tipo, LocalDateTime.now());
        transaccionRepository.save(transaccion);
    }
}
