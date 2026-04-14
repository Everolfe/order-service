package com.github.everolfe.orderservice.mapper.itemmapper;

import com.github.everolfe.orderservice.dto.item.CreateItemDto;
import com.github.everolfe.orderservice.entity.Item;
import com.github.everolfe.orderservice.mapper.BaseMapper;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(config = BaseMapper.class)
public interface CreateItemMapper extends BaseMapper<Item, CreateItemDto> {

    @Mapping(target = "price", source = "price", qualifiedByName = "toCents")
    Item toEntity(CreateItemDto dto);

    @Mapping(target = "price", source = "price", qualifiedByName = "toCents")
    void merge(@MappingTarget Item item, CreateItemDto dto);

    @Named("toCents")
    default Long toCents(BigDecimal price) {
        if (price == null) return null;
        return price.multiply(BigDecimal.valueOf(100)).longValue();
    }
}
