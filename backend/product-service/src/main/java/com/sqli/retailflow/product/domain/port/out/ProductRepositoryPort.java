package com.sqli.retailflow.product.domain.port.out;

import com.sqli.retailflow.product.domain.model.ProductEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepositoryPort {
    ProductEntity save(ProductEntity product);
    Optional<ProductEntity> findById(UUID id);
    List<ProductEntity> findAll();
}
