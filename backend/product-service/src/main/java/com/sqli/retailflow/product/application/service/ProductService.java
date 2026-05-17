package com.sqli.retailflow.product.application.service;

import com.sqli.retailflow.product.application.dto.CreateProductRequest;
import com.sqli.retailflow.product.application.dto.ProductResponse;
import com.sqli.retailflow.product.application.mapper.ProductMapper;
import com.sqli.retailflow.product.domain.model.ProductEntity;
import com.sqli.retailflow.product.domain.port.in.CreateProductUseCase;
import com.sqli.retailflow.product.domain.port.in.GetProductsUseCase;
import com.sqli.retailflow.product.domain.port.out.ProductRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService implements CreateProductUseCase, GetProductsUseCase {

    private final ProductRepositoryPort productRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse createProduct(CreateProductRequest request) {
        ProductEntity entity = productMapper.toEntity(request);
        return productMapper.toResponse(productRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
            .map(productMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        return productRepository.findById(id)
            .map(productMapper::toResponse)
            .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
    }
}
