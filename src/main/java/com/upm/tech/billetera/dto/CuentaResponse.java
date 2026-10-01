package com.upm.tech.billetera.dto;

import java.math.BigDecimal;

public record CuentaResponse(Long id, String titular, BigDecimal saldo) {
}
