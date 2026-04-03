package com.github.everolfe.orderservice.dto.order;

import com.github.everolfe.orderservice.dto.orderitem.CreateOrderItemDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderDto(
        @NotEmpty(message = "User email must not be empty")
        @Email(message = "User email must be valid")
        String userEmail,

        @NotNull(message = "Items must not be null")
        @NotEmpty(message = "Order must contain at least one item")
        @Valid
        List<CreateOrderItemDto> items
) {
}
