package com.sqli.retailflow.product.domain.port.in;

import com.sqli.retailflow.product.application.dto.ProductResponse;

import java.util.List;
import java.util.UUID;

public interface GetProductsUseCase {
    List<ProductResponse> getAllProducts();
    ProductResponse getProductById(UUID id);
}
