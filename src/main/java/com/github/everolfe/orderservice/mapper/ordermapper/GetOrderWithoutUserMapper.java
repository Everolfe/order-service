package com.github.everolfe.orderservice.mapper.ordermapper;

import com.github.everolfe.orderservice.dto.order.GetOrderDtoWithoutUser;
import com.github.everolfe.orderservice.entity.Order;
import com.github.everolfe.orderservice.mapper.BaseMapper;
import com.github.everolfe.orderservice.mapper.orderitemmapper.GetOrderItemMapper;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = BaseMapper.class, uses = GetOrderItemMapper.class)
public interface GetOrderWithoutUserMapper extends BaseMapper<Order, GetOrderDtoWithoutUser> {
    @Mapping(target = "totalPrice", source = "totalPrice", qualifiedByName = "toBigDecimal")
    @Mapping(target = "status", source = "status")
    GetOrderDtoWithoutUser toDto(Order entity);

    @Named("toBigDecimal")
    default BigDecimal toBigDecimal(Long totalPriceCents) {
        if (totalPriceCents == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(totalPriceCents).divide(BigDecimal.valueOf(100));
    }
}
