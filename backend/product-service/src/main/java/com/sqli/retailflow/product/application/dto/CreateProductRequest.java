package com.sqli.retailflow.product.application.dto;

import com.sqli.retailflow.product.domain.model.ProductCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductRequest(
    @NotBlank(message = "Name is required") String name,
    String description,
    @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive") BigDecimal price,
    @Min(value = 0, message = "Stock must be non-negative") int stockQuantity,
    @NotNull(message = "Category is required") ProductCategory category
) {}
