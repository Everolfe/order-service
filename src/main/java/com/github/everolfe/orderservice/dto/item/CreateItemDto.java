package com.github.everolfe.orderservice.dto.item;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateItemDto(
        @NotEmpty(message = "Item name must not be empty")
        String name,

        @NotNull(message = "Price must not be null")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price
) {}
