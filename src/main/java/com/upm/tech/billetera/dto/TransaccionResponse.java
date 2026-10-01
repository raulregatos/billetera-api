package com.upm.tech.billetera.dto;

import com.upm.tech.billetera.model.TipoTransaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransaccionResponse(
        Long id,
        TipoTransaccion tipo,
        LocalDateTime fecha,
        BigDecimal monto,
        DireccionTransaccion direccion,
        CuentaRelacionadaResponse cuentaRelacionada
) {
}
