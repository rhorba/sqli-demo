package com.sqli.retailflow.inventory.application.dto;

import java.time.Instant;
import java.util.UUID;

public record StockResponse(
    UUID productId,
    String productName,
    int available,
    int reserved,
    int actualAvailable,
    Instant lastUpdated
) {}
