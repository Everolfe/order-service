package com.github.everolfe.orderservice.dto.item;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GetItemDto(
        Long id,
        String name,
        BigDecimal price,
        Boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) { }
