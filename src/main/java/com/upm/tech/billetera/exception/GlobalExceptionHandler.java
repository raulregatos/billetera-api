package com.upm.tech.billetera.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.upm.tech.billetera.service.AutenticacionService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

// Esta anotación convierte a la clase en un interceptor global de excepciones
@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Manejar errores de negocio (Saldo insuficiente, montos negativos, etc.)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArgumentosInvalidos(IllegalArgumentException ex) {
        Map<String, String> respuesta = new HashMap<>();
        // Extraemos el mensaje del error que pusimos en el Service
        respuesta.put("error", ex.getMessage());

        // Devolvemos un 400 Bad Request
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> manejarCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        Map<String, String> respuesta = new HashMap<>();
        // Extraemos el mensaje del error que pusimos en el Service
        respuesta.put("error", ex.getMessage());

        // Solo la ausencia de una cuenta se traduce a 404. Otros fallos inesperados
        // conservan el tratamiento 500 de Spring.
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

    @ExceptionHandler(UsuarioYaExisteException.class)
    public ResponseEntity<Map<String, String>> manejarUsuarioDuplicado(UsuarioYaExisteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AutenticacionService.CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, String>> manejarCredencialesInvalidas(
            AutenticacionService.CredencialesInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Usuario o contraseña incorrectos"));
    }
}
