package com.github.everolfe.orderservice.dto.order;

import com.github.everolfe.orderservice.dto.orderitem.GetOrderItemDto;
import java.math.BigDecimal;
import java.util.Set;

public record GetOrderDtoWithoutUser(
        Long id,
        String status,
        BigDecimal totalPrice,
        Boolean deleted,
        Set<GetOrderItemDto> orderItems
) { }
