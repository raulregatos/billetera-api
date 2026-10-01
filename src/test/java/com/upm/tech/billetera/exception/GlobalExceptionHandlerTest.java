package com.upm.tech.billetera.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void cuentaInexistenteSeRespondeComoNotFound() {
        var response = handler.manejarCuentaNoEncontrada(new CuentaNoEncontradaException(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Cuenta no encontrada: 42");
    }

    @Test
    void argumentoInvalidoSeRespondeComoBadRequest() {
        var response = handler.manejarArgumentosInvalidos(new IllegalArgumentException("Monto inválido"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Monto inválido");
    }
}
