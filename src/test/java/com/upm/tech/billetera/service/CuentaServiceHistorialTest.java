package com.upm.tech.billetera.service;

import com.upm.tech.billetera.exception.CuentaNoEncontradaException;
import com.upm.tech.billetera.dto.DireccionTransaccion;
import com.upm.tech.billetera.model.Cuenta;
import com.upm.tech.billetera.model.TipoTransaccion;
import com.upm.tech.billetera.model.Transaccion;
import com.upm.tech.billetera.repository.CuentaRepository;
import com.upm.tech.billetera.repository.TransaccionRepository;
import com.upm.tech.billetera.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaServiceHistorialTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CuentaService cuentaService;

    private Cuenta alice;
    private Cuenta bob;

    @BeforeEach
    void setUp() {
        alice = new Cuenta("Alice", new BigDecimal("100.00"));
        alice.setId(1L);
        bob = new Cuenta("Bob", new BigDecimal("50.00"));
        bob.setId(2L);
    }

    @Test
    void mapeaEntradasSalidasYCuentaRelacionadaParaTransferencias() {
        when(cuentaRepository.findByIdAndUsuarioNombreUsuario(1L, "alice")).thenReturn(Optional.of(alice));
        Transaccion deposito = transaccion(1L, null, alice, TipoTransaccion.DEPOSITO, "2026-10-01T10:00:00");
        Transaccion retiro = transaccion(2L, alice, null, TipoTransaccion.RETIRO, "2026-10-01T09:00:00");
        Transaccion transferenciaSalida = transaccion(3L, alice, bob, TipoTransaccion.TRANSFERENCIA, "2026-10-01T08:00:00");
        Transaccion transferenciaEntrada = transaccion(4L, bob, alice, TipoTransaccion.TRANSFERENCIA, "2026-10-01T07:00:00");
        when(transaccionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deposito, retiro, transferenciaSalida, transferenciaEntrada), PageRequest.of(0, 20), 4));

        var response = cuentaService.obtenerHistorial(1L, null, null, null, 0, 20, "alice");

        assertThat(response.content()).hasSize(4);
        assertThat(response.content().get(0).direccion()).isEqualTo(DireccionTransaccion.ENTRADA);
        assertThat(response.content().get(0).cuentaRelacionada()).isNull();
        assertThat(response.content().get(1).direccion()).isEqualTo(DireccionTransaccion.SALIDA);
        assertThat(response.content().get(1).cuentaRelacionada()).isNull();
        assertThat(response.content().get(2).direccion()).isEqualTo(DireccionTransaccion.SALIDA);
        assertThat(response.content().get(2).cuentaRelacionada().id()).isEqualTo(2L);
        assertThat(response.content().get(3).direccion()).isEqualTo(DireccionTransaccion.ENTRADA);
        assertThat(response.content().get(3).cuentaRelacionada().id()).isEqualTo(2L);
        assertThat(response.totalElements()).isEqualTo(4);
    }

    @Test
    void aplicaTipoYFechasInclusivasConPaginacionEstable() {
        when(cuentaRepository.findByIdAndUsuarioNombreUsuario(1L, "alice")).thenReturn(Optional.of(alice));
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 3);
        when(transaccionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        var response = cuentaService.obtenerHistorial(1L, TipoTransaccion.TRANSFERENCIA, desde, hasta, 2, 5, "alice");

        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.content()).isEmpty();
        verify(transaccionRepository).findAll(any(Specification.class), argThat((Pageable pageable) ->
                pageable.getPageNumber() == 2 && pageable.getPageSize() == 5));
    }

    @Test
    void ordenaPorFechaEIdDescendentes() {
        when(cuentaRepository.findByIdAndUsuarioNombreUsuario(1L, "alice")).thenReturn(Optional.of(alice));
        when(transaccionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        cuentaService.obtenerHistorial(1L, null, null, null, 0, 20, "alice");

        verify(transaccionRepository).findAll(any(Specification.class), argThat((Pageable pageable) ->
                pageable.getPageNumber() == 0
                        && pageable.getPageSize() == 20
                        && pageable.getSort().getOrderFor("fecha").getDirection() == Sort.Direction.DESC
                        && pageable.getSort().getOrderFor("id").getDirection() == Sort.Direction.DESC));
    }

    @Test
    void cuentaInexistenteProduce404DeDominioYNoConsultaMovimientos() {
        when(cuentaRepository.findByIdAndUsuarioNombreUsuario(99L, "alice")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class,
                () -> cuentaService.obtenerHistorial(99L, null, null, null, 0, 20, "alice"));

        verifyNoInteractions(transaccionRepository);
    }

    @Test
    void rechazaPaginacionYRangoDeFechasInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> cuentaService.obtenerHistorial(1L, null, null, null, -1, 20, "alice"));
        assertThrows(IllegalArgumentException.class,
                () -> cuentaService.obtenerHistorial(1L, null, null, null, 0, 101, "alice"));
        assertThrows(IllegalArgumentException.class,
                () -> cuentaService.obtenerHistorial(1L, null,
                        LocalDate.of(2026, 10, 4), LocalDate.of(2026, 10, 3), 0, 20, "alice"));

        verifyNoInteractions(cuentaRepository, transaccionRepository);
    }

    private Transaccion transaccion(Long id, Cuenta origen, Cuenta destino, TipoTransaccion tipo, String fecha) {
        Transaccion transaccion = new Transaccion(origen, destino, new BigDecimal("10.00"), tipo, LocalDateTime.parse(fecha));
        transaccion.setId(id);
        return transaccion;
    }
}
