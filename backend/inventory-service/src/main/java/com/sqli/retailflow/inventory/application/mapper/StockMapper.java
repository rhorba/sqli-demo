package com.sqli.retailflow.inventory.application.mapper;

import com.sqli.retailflow.inventory.application.dto.StockResponse;
import com.sqli.retailflow.inventory.domain.model.StockEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mapping(target = "actualAvailable", expression = "java(entity.getActualAvailable())")
    StockResponse toResponse(StockEntity entity);
}
