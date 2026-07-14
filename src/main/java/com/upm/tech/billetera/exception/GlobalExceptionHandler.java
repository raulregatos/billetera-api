package com.upm.tech.billetera.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // TODO 1: Crea un método parecido al de arriba para manejar RuntimeException
    // Pista: En nuestro Service lanzamos RuntimeException cuando la cuenta no se encuentra.
    // En el mundo web, cuando algo no se encuentra, el código HTTP correcto es 404 (NOT_FOUND).
    // Intenta crear el @ExceptionHandler(RuntimeException.class) que devuelva un HttpStatus.NOT_FOUND.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> manejarCuentaNoEncontrada(RuntimeException ex) {
        Map<String, String> respuesta = new HashMap<>();
        // Extraemos el mensaje del error que pusimos en el Service
        respuesta.put("error", ex.getMessage());

        // Devolvemos un 404 NOT_FOUND
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }
}
