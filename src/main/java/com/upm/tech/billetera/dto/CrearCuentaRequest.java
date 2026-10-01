package com.upm.tech.billetera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearCuentaRequest(
        @NotBlank(message = "El titular es obligatorio")
        @Size(max = 100, message = "El titular no puede superar 100 caracteres")
        String titular
) {
}
