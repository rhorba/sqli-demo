package com.sqli.retailflow.product.application.service;

import com.sqli.retailflow.product.application.dto.CreateProductRequest;
import com.sqli.retailflow.product.application.dto.ProductResponse;
import com.sqli.retailflow.product.application.mapper.ProductMapper;
import com.sqli.retailflow.product.domain.model.ProductCategory;
import com.sqli.retailflow.product.domain.model.ProductEntity;
import com.sqli.retailflow.product.domain.port.out.ProductRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService")
class ProductServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private ProductEntity sampleEntity() {
        ProductEntity e = new ProductEntity();
        e.setId(UUID.randomUUID());
        e.setName("MacBook Pro");
        e.setPrice(new BigDecimal("1999.99"));
        e.setStockQuantity(10);
        e.setCategory(ProductCategory.ELECTRONICS);
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        return e;
    }

    private ProductResponse sampleResponse(ProductEntity e) {
        return new ProductResponse(e.getId(), e.getName(), e.getDescription(),
            e.getPrice(), e.getStockQuantity(), e.getCategory(), e.getCreatedAt());
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("should persist and return mapped response")
        void shouldPersistAndReturnMappedResponse() {
            var request = new CreateProductRequest("MacBook Pro", "Apple", new BigDecimal("1999.99"), 10, ProductCategory.ELECTRONICS);
            var entity = sampleEntity();
            var response = sampleResponse(entity);

            when(productMapper.toEntity(request)).thenReturn(entity);
            when(productRepository.save(entity)).thenReturn(entity);
            when(productMapper.toResponse(entity)).thenReturn(response);

            ProductResponse result = productService.createProduct(request);

            assertThat(result.name()).isEqualTo("MacBook Pro");
            assertThat(result.price()).isEqualByComparingTo(new BigDecimal("1999.99"));
            verify(productRepository).save(any(ProductEntity.class));
        }
    }

    @Nested
    @DisplayName("getAllProducts")
    class GetAllProducts {

        @Test
        @DisplayName("should return all products mapped")
        void shouldReturnAllProductsMapped() {
            var entity = sampleEntity();
            var response = sampleResponse(entity);
            when(productRepository.findAll()).thenReturn(List.of(entity));
            when(productMapper.toResponse(entity)).thenReturn(response);

            List<ProductResponse> result = productService.getAllProducts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("MacBook Pro");
        }
    }

    @Nested
    @DisplayName("getProductById")
    class GetProductById {

        @Test
        @DisplayName("should return product when found")
        void shouldReturnProductWhenFound() {
            var entity = sampleEntity();
            var response = sampleResponse(entity);
            when(productRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
            when(productMapper.toResponse(entity)).thenReturn(response);

            ProductResponse result = productService.getProductById(entity.getId());

            assertThat(result.id()).isEqualTo(entity.getId());
        }

        @Test
        @DisplayName("should throw when not found")
        void shouldThrowWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(productRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(id))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(id.toString());
        }
    }
}
