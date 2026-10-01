package com.upm.tech.billetera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 32, message = "El nombre de usuario debe tener entre 3 y 32 caracteres")
        @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "El nombre de usuario solo admite letras, números, punto, guion y guion bajo")
        String usuario,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 12, max = 72, message = "La contraseña debe tener entre 12 y 72 caracteres")
        String contrasena,

        @NotBlank(message = "El titular de la cuenta es obligatorio")
        @Size(max = 100, message = "El titular no puede superar 100 caracteres")
        String titular
) {
}
