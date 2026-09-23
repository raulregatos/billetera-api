package com.upm.tech.billetera.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record MontoRequest(
        @NotNull(message = "El ID de la cuenta no puede ser nulo")
        Long idCuenta,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal monto
) {}
