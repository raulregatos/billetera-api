package com.upm.tech.billetera.exception;

public class CuentaNoEncontradaException extends RuntimeException {
    public CuentaNoEncontradaException(Long id) {
        super("Cuenta no encontrada: " + id);
    }
}
