package com.upm.tech.billetera.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferenciaRequest(
        @NotNull(message = "El ID de origen no puede ser nulo")
        Long idOrigen,

        @NotNull(message = "El ID de destino no puede ser nulo")
        Long idDestino,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal monto
) {}
