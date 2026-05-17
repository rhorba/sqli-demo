package com.sqli.retailflow.order.domain.port.in;

import com.sqli.retailflow.order.application.dto.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface GetOrdersUseCase {
    List<OrderResponse> getAllOrders();
    OrderResponse getOrderById(UUID id);
    OrderResponse confirmOrder(UUID id);
    OrderResponse shipOrder(UUID id);
}
