package com.upm.tech.billetera.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio") String usuario,
        @NotBlank(message = "La contraseña es obligatoria") String contrasena
) {
}
