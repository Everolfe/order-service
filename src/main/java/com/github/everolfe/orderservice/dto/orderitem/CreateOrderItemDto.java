package com.github.everolfe.orderservice.dto.orderitem;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderItemDto(

        @NotNull(message = "Item ID must not be null")
        Long itemId,

        @NotNull(message = "Quantity must not be null")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {}
