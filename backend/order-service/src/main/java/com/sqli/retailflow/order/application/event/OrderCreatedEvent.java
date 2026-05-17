package com.sqli.retailflow.order.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
    UUID orderId,
    UUID customerId,
    List<OrderItemDto> items,
    BigDecimal totalAmount,
    Instant occurredAt
) {
    public record OrderItemDto(UUID productId, String productName, int quantity, BigDecimal unitPrice) {}
}
