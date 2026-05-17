package com.sqli.retailflow.product.domain.port.in;

import com.sqli.retailflow.product.application.dto.CreateProductRequest;
import com.sqli.retailflow.product.application.dto.ProductResponse;

public interface CreateProductUseCase {
    ProductResponse createProduct(CreateProductRequest request);
}
