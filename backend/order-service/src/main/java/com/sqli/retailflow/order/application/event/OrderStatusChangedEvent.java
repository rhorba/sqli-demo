package com.sqli.retailflow.order.application.event;

import com.sqli.retailflow.order.domain.model.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedEvent(
    UUID orderId,
    OrderStatus previousStatus,
    OrderStatus newStatus,
    Instant occurredAt
) {}
