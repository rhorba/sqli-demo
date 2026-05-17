package com.sqli.retailflow.product.application.mapper;

import com.sqli.retailflow.product.application.dto.CreateProductRequest;
import com.sqli.retailflow.product.application.dto.ProductResponse;
import com.sqli.retailflow.product.domain.model.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductEntity toEntity(CreateProductRequest request);

    ProductResponse toResponse(ProductEntity entity);
}
