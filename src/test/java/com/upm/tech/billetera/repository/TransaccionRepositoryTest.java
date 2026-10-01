package com.upm.tech.billetera.repository;

import com.upm.tech.billetera.model.Cuenta;
import com.upm.tech.billetera.model.TipoTransaccion;
import com.upm.tech.billetera.model.Transaccion;
import com.upm.tech.billetera.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TransaccionRepositoryTest {

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private TransaccionRepository transaccionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void historialIncluyeOrigenODestinoYAplicaFiltroDeTipoYFechas() {
        Usuario usuario = usuarioRepository.saveAndFlush(new Usuario("historial-test", "hash", true));
        Cuenta alice = cuentaRepository.saveAndFlush(new Cuenta("Historial Alice", new BigDecimal("100.00"), usuario));
        Cuenta bob = cuentaRepository.saveAndFlush(new Cuenta("Historial Bob", new BigDecimal("50.00"), usuario));
        Cuenta carol = cuentaRepository.saveAndFlush(new Cuenta("Historial Carol", new BigDecimal("25.00"), usuario));

        guardar(null, alice, TipoTransaccion.DEPOSITO, "2026-10-01T00:00:00");
        guardar(alice, null, TipoTransaccion.RETIRO, "2026-10-01T23:59:59.999999");
        guardar(bob, alice, TipoTransaccion.TRANSFERENCIA, "2026-10-01T12:00:00");
        guardar(alice, bob, TipoTransaccion.TRANSFERENCIA, "2026-10-02T00:00:00");
        guardar(bob, carol, TipoTransaccion.TRANSFERENCIA, "2026-10-01T15:00:00");
        transaccionRepository.flush();

        LocalDateTime desde = LocalDateTime.parse("2026-10-01T00:00:00");
        LocalDateTime hastaExclusivo = LocalDateTime.parse("2026-10-02T00:00:00");
        Sort orden = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"));
        Page<Transaccion> movimientos = transaccionRepository.findAll(
                TransaccionSpecifications.deCuenta(alice.getId(), null, desde, hastaExclusivo),
                PageRequest.of(0, 20, orden));

        assertThat(movimientos.getTotalElements()).isEqualTo(3);
        assertThat(movimientos.getContent())
                .extracting(Transaccion::getTipo)
                .containsExactly(TipoTransaccion.RETIRO, TipoTransaccion.TRANSFERENCIA, TipoTransaccion.DEPOSITO);

        Page<Transaccion> transferencias = transaccionRepository.findAll(
                TransaccionSpecifications.deCuenta(alice.getId(), TipoTransaccion.TRANSFERENCIA, desde, hastaExclusivo),
                PageRequest.of(0, 20, orden));
        assertThat(transferencias.getTotalElements()).isEqualTo(1);
        assertThat(transferencias.getContent().getFirst().getCuentaOrigen()).isEqualTo(bob);
        assertThat(transferencias.getContent().getFirst().getCuentaDestino()).isEqualTo(alice);
    }

    @Test
    void paginacionEsEstablePorFechaEIdYUnaCuentaSinMovimientosDevuelvePaginaVacia() {
        Usuario usuario = usuarioRepository.saveAndFlush(new Usuario("pagina-test", "hash", true));
        Cuenta alice = cuentaRepository.saveAndFlush(new Cuenta("Página Alice", new BigDecimal("100.00"), usuario));
        Cuenta bob = cuentaRepository.saveAndFlush(new Cuenta("Página Bob", new BigDecimal("50.00"), usuario));
        LocalDateTime fecha = LocalDateTime.parse("2026-10-01T12:00:00");
        guardar(null, alice, TipoTransaccion.DEPOSITO, fecha.toString());
        guardar(null, alice, TipoTransaccion.DEPOSITO, fecha.toString());
        transaccionRepository.flush();

        Sort orden = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"));
        Page<Transaccion> primeraPagina = transaccionRepository.findAll(
                TransaccionSpecifications.deCuenta(alice.getId(), null, null, null), PageRequest.of(0, 1, orden));
        Page<Transaccion> segundaPagina = transaccionRepository.findAll(
                TransaccionSpecifications.deCuenta(alice.getId(), null, null, null), PageRequest.of(1, 1, orden));
        Page<Transaccion> vacia = transaccionRepository.findAll(
                TransaccionSpecifications.deCuenta(bob.getId(), null, null, null), PageRequest.of(0, 20, orden));

        assertThat(primeraPagina.getTotalElements()).isEqualTo(2);
        assertThat(primeraPagina.getContent().getFirst().getId()).isGreaterThan(segundaPagina.getContent().getFirst().getId());
        assertThat(vacia.getContent()).isEmpty();
        assertThat(vacia.getTotalElements()).isZero();
    }

    private void guardar(Cuenta origen, Cuenta destino, TipoTransaccion tipo, String fecha) {
        transaccionRepository.save(new Transaccion(
                origen, destino, new BigDecimal("10.00"), tipo, LocalDateTime.parse(fecha)));
    }
}
