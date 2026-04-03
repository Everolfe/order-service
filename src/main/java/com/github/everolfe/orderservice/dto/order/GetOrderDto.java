package com.github.everolfe.orderservice.dto.order;

import com.github.everolfe.orderservice.dto.user.GetUserDto;

public record GetOrderDto(
        GetOrderDtoWithoutUser getOrderDtoWithoutUser,
        GetUserDto getUserDto
) {}
