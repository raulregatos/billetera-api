package com.upm.tech.billetera.service;

import com.upm.tech.billetera.model.Cuenta;
import com.upm.tech.billetera.repository.CuentaRepository;
import com.upm.tech.billetera.repository.TransaccionRepository;
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

        when(cuentaRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByIdConBloqueo(2L)).thenReturn(Optional.of(cuentaDestino));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        cuentaService.transferir(1L, 2L, monto);

        assertThat(cuentaOrigen.getSaldo()).isEqualTo(new BigDecimal("70.00"));
        assertThat(cuentaDestino.getSaldo()).isEqualTo(new BigDecimal("80.00"));

        verify(cuentaRepository).save(cuentaOrigen);
        verify(cuentaRepository).save(cuentaDestino);
    }

    @Test
    void transferirSaldoInsuficiente() {
        BigDecimal monto = new BigDecimal("500.00");

        when(cuentaRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByIdConBloqueo(2L)).thenReturn(Optional.of(cuentaDestino));

        assertThrows(IllegalArgumentException.class,
                () -> cuentaService.transferir(1L, 2L, monto));

        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }

    @Test
    void transferirCuentaNoEncontrada() {
        BigDecimal monto = new BigDecimal("10.00");

        when(cuentaRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findByIdConBloqueo(2L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> cuentaService.transferir(1L, 2L, monto));

        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }
}
