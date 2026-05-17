package com.sqli.retailflow.order.domain.port.in;

import com.sqli.retailflow.order.application.dto.OrderResponse;
import com.sqli.retailflow.order.application.dto.PlaceOrderRequest;

public interface PlaceOrderUseCase {
    OrderResponse placeOrder(PlaceOrderRequest request);
}
