package com.sqli.retailflow.product.infrastructure.persistence;

import com.sqli.retailflow.product.domain.model.ProductEntity;
import com.sqli.retailflow.product.domain.port.out.ProductRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ProductPersistenceAdapter implements ProductRepositoryPort {

    private final SpringDataProductRepository repository;

    @Override
    public ProductEntity save(ProductEntity product) {
        return repository.save(product);
    }

    @Override
    public Optional<ProductEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<ProductEntity> findAll() {
        return repository.findAll();
    }
}
