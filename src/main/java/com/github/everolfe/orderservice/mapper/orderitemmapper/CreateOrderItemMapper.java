package com.github.everolfe.orderservice.mapper.orderitemmapper;

import com.github.everolfe.orderservice.dto.orderitem.CreateOrderItemDto;
import com.github.everolfe.orderservice.entity.OrderItem;
import com.github.everolfe.orderservice.mapper.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapper.class)
public interface CreateOrderItemMapper extends BaseMapper<OrderItem, CreateOrderItemDto> {
}
