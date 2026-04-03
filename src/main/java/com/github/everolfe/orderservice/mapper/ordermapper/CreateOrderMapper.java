package com.github.everolfe.orderservice.mapper.ordermapper;

import com.github.everolfe.orderservice.dto.order.CreateOrderDto;
import com.github.everolfe.orderservice.entity.Order;
import com.github.everolfe.orderservice.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapper.class)
public interface CreateOrderMapper extends BaseMapper<Order, CreateOrderDto> {
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "deleted", constant = "false")
    Order toEntity(CreateOrderDto dto);
}
