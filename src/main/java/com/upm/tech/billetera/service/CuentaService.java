package com.upm.tech.billetera.service;

import com.upm.tech.billetera.model.Cuenta;
import com.upm.tech.billetera.model.Transaccion;
import com.upm.tech.billetera.model.TipoTransaccion;
import com.upm.tech.billetera.exception.CuentaNoEncontradaException;
import com.upm.tech.billetera.repository.CuentaRepository;
import com.upm.tech.billetera.repository.TransaccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CuentaService {

    // 1. Inyección de dependencias
    // Usamos el repositorio para hablar con la BD. Lo inyectamos por constructor (es la mejor práctica).
    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;

    public CuentaService(CuentaRepository cuentaRepository, TransaccionRepository transaccionRepository) {
        this.cuentaRepository = cuentaRepository;
        this.transaccionRepository = transaccionRepository;
    }

    // 2. Método de lectura simple (no modifica datos)
    public Cuenta obtenerCuenta(Long id) {
        // findById devuelve un Optional; el error específico se transforma en HTTP 404.
        return cuentaRepository.findById(id)
                .orElseThrow(() -> new CuentaNoEncontradaException(id));
    }

    // 3. Método transaccional: Depositar
    @Transactional
    public Cuenta depositar(Long id, BigDecimal monto) {
        // Validar que el monto sea positivo (monto > 0)
        // En BigDecimal: compareTo devuelve 1 si es mayor, 0 si es igual, -1 si es menor.
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a depositar debe ser mayor a cero");
        }

        Cuenta cuenta = cuentaRepository.findByIdConBloqueo(id)
                .orElseThrow(() -> new CuentaNoEncontradaException(id));

        // Sumar al saldo: cuenta.getSaldo() + monto
        BigDecimal nuevoSaldo = cuenta.getSaldo().add(monto);
        cuenta.setSaldo(nuevoSaldo);

        Cuenta cuentaGuardada = cuentaRepository.save(cuenta);

        registrarTransaccion(null, cuentaGuardada, monto, TipoTransaccion.DEPOSITO);

        return cuentaGuardada;
    }

    @Transactional
    public Cuenta retirar(Long id, BigDecimal monto) {
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a retirar debe ser mayor a cero");
        }
        Cuenta cuenta = cuentaRepository.findByIdConBloqueo(id)
                .orElseThrow(() -> new CuentaNoEncontradaException(id));

        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new IllegalArgumentException("El saldo disponible debe ser mayor o igual al monto a retirar");
        }
        BigDecimal nuevoSaldo = cuenta.getSaldo().subtract(monto);
        cuenta.setSaldo(nuevoSaldo);

        Cuenta cuentaGuardada = cuentaRepository.save(cuenta);

        registrarTransaccion(cuentaGuardada, null, monto, TipoTransaccion.RETIRO);

        return cuentaGuardada;
    }

    @Transactional
    public void transferir(Long idOrigen, Long idDestino, BigDecimal monto) {

        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a transferir debe ser mayor a cero");
        }
        if (idOrigen.equals(idDestino)) {
            throw new IllegalArgumentException("No puedes transferir dinero a la misma cuenta");
        }

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
