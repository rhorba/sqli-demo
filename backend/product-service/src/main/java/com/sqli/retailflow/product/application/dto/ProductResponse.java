package com.sqli.retailflow.product.application.dto;

import com.sqli.retailflow.product.domain.model.ProductCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String name,
    String description,
    BigDecimal price,
    int stockQuantity,
    ProductCategory category,
    Instant createdAt
) {}
