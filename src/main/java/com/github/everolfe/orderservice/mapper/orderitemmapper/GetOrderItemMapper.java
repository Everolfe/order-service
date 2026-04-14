package com.github.everolfe.orderservice.mapper.orderitemmapper;

import com.github.everolfe.orderservice.dto.orderitem.GetOrderItemDto;
import com.github.everolfe.orderservice.entity.OrderItem;
import com.github.everolfe.orderservice.mapper.BaseMapper;
import com.github.everolfe.orderservice.mapper.itemmapper.GetItemMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapper.class, uses = GetItemMapper.class)
public interface GetOrderItemMapper extends BaseMapper<OrderItem, GetOrderItemDto> {
}
