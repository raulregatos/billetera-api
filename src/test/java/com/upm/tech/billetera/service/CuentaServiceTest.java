package com.upm.tech.billetera.service;

import com.upm.tech.billetera.model.Cuenta;
import com.upm.tech.billetera.model.Transaccion;
import com.upm.tech.billetera.model.TipoTransaccion;
import com.upm.tech.billetera.exception.CuentaNoEncontradaException;
import com.upm.tech.billetera.repository.CuentaRepository;
import com.upm.tech.billetera.repository.TransaccionRepository;
import com.upm.tech.billetera.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CuentaService cuentaService;

    private Cuenta cuentaOrigen;
    private Cuenta cuentaDestino;

    @BeforeEach
    void setUp() {
        cuentaOrigen = new Cuenta("Origen", new BigDecimal("100.00"));
        cuentaOrigen.setId(1L);

        cuentaDestino = new Cuenta("Destino", new BigDecimal("50.00"));
        cuentaDestino.setId(2L);
    }

    @Test
    void transferirExitoso() {
        BigDecimal monto = new BigDecimal("30.00");

        when(cuentaRepository.findByIdAndUsuarioNombreUsuario(1L, "alice")).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findById(2L)).thenReturn(Optional.of(cuentaDestino));
        when(cuentaRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByIdConBloqueo(2L)).thenReturn(Optional.of(cuentaDestino));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cuentaService.transferir(1L, 2L, monto, "alice");

        assertThat(cuentaOrigen.getSaldo()).isEqualTo(new BigDecimal("70.00"));
        assertThat(cuentaDestino.getSaldo()).isEqualTo(new BigDecimal("80.00"));

        verify(cuentaRepository).save(cuentaOrigen);
        verify(cuentaRepository).save(cuentaDestino);
        verify(transaccionRepository).save(argThat(transaccion ->
                transaccion.getTipo() == TipoTransaccion.TRANSFERENCIA
                        && transaccion.getCuentaOrigen() == cuentaOrigen
                        && transaccion.getCuentaDestino() == cuentaDestino));
    }

    @Test
    void transferenciaEnSentidoInversoBloqueaPorIdAscendente() {
        when(cuentaRepository.findByIdAndUsuarioNombreUsuario(2L, "alice")).thenReturn(Optional.of(cuentaDestino));
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByIdConBloqueo(2L)).thenReturn(Optional.of(cuentaDestino));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cuentaService.transferir(2L, 1L, new BigDecimal("10.00"), "alice");

        var inOrder = inOrder(cuentaRepository);
        inOrder.verify(cuentaRepository).findByIdConBloqueo(1L);
        inOrder.verify(cuentaRepository).findByIdConBloqueo(2L);
    }

    @Test
    void depositarBloqueaCuentaYRegistraMovimiento() {
        when(cuentaRepository.findByIdAndUsuarioConBloqueo(1L, "alice")).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = cuentaService.depositar(1L, new BigDecimal("25.00"), "alice");

        assertThat(resultado.saldo()).isEqualByComparingTo("125.00");
        verify(cuentaRepository).findByIdAndUsuarioConBloqueo(1L, "alice");
        verify(cuentaRepository, never()).findById(1L);
        verify(transaccionRepository).save(argThat(transaccion ->
                transaccion.getTipo() == TipoTransaccion.DEPOSITO
                        && transaccion.getCuentaOrigen() == null
                        && transaccion.getCuentaDestino() == cuentaOrigen));
    }

    @Test
    void retirarBloqueaCuentaYRegistraMovimiento() {
        when(cuentaRepository.findByIdAndUsuarioConBloqueo(1L, "alice")).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = cuentaService.retirar(1L, new BigDecimal("25.00"), "alice");

        assertThat(resultado.saldo()).isEqualByComparingTo("75.00");
        verify(cuentaRepository).findByIdAndUsuarioConBloqueo(1L, "alice");
        verify(cuentaRepository, never()).findById(1L);
        verify(transaccionRepository).save(argThat(transaccion ->
                transaccion.getTipo() == TipoTransaccion.RETIRO
                        && transaccion.getCuentaOrigen() == cuentaOrigen
                        && transaccion.getCuentaDestino() == null));
    }

    @Test
    void retirarSaldoInsuficienteNoGuardaCambios() {
        when(cuentaRepository.findByIdAndUsuarioConBloqueo(1L, "alice")).thenReturn(Optional.of(cuentaOrigen));

        assertThrows(IllegalArgumentException.class,
                () -> cuentaService.retirar(1L, new BigDecimal("500.00"), "alice"));

        verify(cuentaRepository, never()).save(any(Cuenta.class));
        verify(transaccionRepository, never()).save(any(Transaccion.class));
    }

    @Test
    void depositarCuentaNoEncontradaUsaExcepcionEspecifica() {
        when(cuentaRepository.findByIdAndUsuarioConBloqueo(99L, "alice")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class,
                () -> cuentaService.depositar(99L, new BigDecimal("1.00"), "alice"));

        verify(cuentaRepository, never()).save(any(Cuenta.class));
        verify(transaccionRepository, never()).save(any(Transaccion.class));
    }

    @Test
    void transferirSaldoInsuficiente() {
        BigDecimal monto = new BigDecimal("500.00");

        when(cuentaRepository.findByIdAndUsuarioNombreUsuario(1L, "alice")).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findById(2L)).thenReturn(Optional.of(cuentaDestino));
        when(cuentaRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByIdConBloqueo(2L)).thenReturn(Optional.of(cuentaDestino));

        assertThrows(IllegalArgumentException.class,
                () -> cuentaService.transferir(1L, 2L, monto, "alice"));

        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }

    @Test
    void transferirCuentaNoEncontrada() {
        BigDecimal monto = new BigDecimal("10.00");

        when(cuentaRepository.findByIdAndUsuarioNombreUsuario(1L, "alice")).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class,
                () -> cuentaService.transferir(1L, 2L, monto, "alice"));

        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }
}
