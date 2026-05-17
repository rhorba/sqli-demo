package com.sqli.retailflow.order.domain.port.out;

import com.sqli.retailflow.order.application.event.OrderCreatedEvent;
import com.sqli.retailflow.order.application.event.OrderStatusChangedEvent;

public interface OrderEventPublisherPort {
    void publishOrderCreated(OrderCreatedEvent event);
    void publishOrderStatusChanged(OrderStatusChangedEvent event);
}
