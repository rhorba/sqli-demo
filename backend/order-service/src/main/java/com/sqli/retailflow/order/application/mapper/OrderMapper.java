package com.sqli.retailflow.order.application.mapper;

import com.sqli.retailflow.order.application.dto.OrderItemRequest;
import com.sqli.retailflow.order.application.dto.OrderItemResponse;
import com.sqli.retailflow.order.application.dto.OrderResponse;
import com.sqli.retailflow.order.domain.model.OrderEntity;
import com.sqli.retailflow.order.domain.model.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    OrderItemEntity toItemEntity(OrderItemRequest request);

    @Mapping(target = "subtotal", expression = "java(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))")
    OrderItemResponse toItemResponse(OrderItemEntity item);

    @Mapping(target = "items", source = "items")
    OrderResponse toResponse(OrderEntity order);
}
