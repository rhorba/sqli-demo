package com.sqli.retailflow.inventory.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Mirror of order-service event — kept local to avoid service coupling
public record OrderCreatedEvent(
    UUID orderId,
    UUID customerId,
    List<OrderItemDto> items,
    BigDecimal totalAmount,
    Instant occurredAt
) {
    public record OrderItemDto(UUID productId, String productName, int quantity, BigDecimal unitPrice) {}
}
