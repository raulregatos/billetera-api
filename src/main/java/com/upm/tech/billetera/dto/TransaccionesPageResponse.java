package com.upm.tech.billetera.dto;

import java.util.List;

public record TransaccionesPageResponse(
        List<TransaccionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
