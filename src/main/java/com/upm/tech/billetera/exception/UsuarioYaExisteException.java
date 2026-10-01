package com.upm.tech.billetera.exception;

public class UsuarioYaExisteException extends RuntimeException {
    public UsuarioYaExisteException(String usuario) {
        super("El nombre de usuario ya está registrado: " + usuario);
    }
}
