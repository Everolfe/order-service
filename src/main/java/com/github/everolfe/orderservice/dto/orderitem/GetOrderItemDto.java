package com.github.everolfe.orderservice.dto.orderitem;

import com.github.everolfe.orderservice.dto.item.GetItemDto;
import java.time.LocalDateTime;

public record GetOrderItemDto(
        Long id,
        Integer quantity,
        GetItemDto item,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
