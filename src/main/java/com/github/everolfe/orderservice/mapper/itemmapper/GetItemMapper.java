package com.github.everolfe.orderservice.mapper.itemmapper;

import com.github.everolfe.orderservice.dto.item.GetItemDto;
import com.github.everolfe.orderservice.entity.Item;
import com.github.everolfe.orderservice.mapper.BaseMapper;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = BaseMapper.class)
public interface GetItemMapper extends BaseMapper<Item, GetItemDto> {
    @Mapping(target = "price", source = "price", qualifiedByName = "toBigDecimal")
    GetItemDto toDto(Item entity);

    @Named("toBigDecimal")
    default BigDecimal toBigDecimal(Long priceCents) {
        if (priceCents == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(priceCents).divide(BigDecimal.valueOf(100));
    }
}
