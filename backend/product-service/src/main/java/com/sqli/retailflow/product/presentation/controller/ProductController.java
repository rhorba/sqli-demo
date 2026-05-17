package com.sqli.retailflow.product.presentation.controller;

import com.sqli.retailflow.product.application.dto.CreateProductRequest;
import com.sqli.retailflow.product.application.dto.ProductResponse;
import com.sqli.retailflow.product.domain.port.in.CreateProductUseCase;
import com.sqli.retailflow.product.domain.port.in.GetProductsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
@Tag(name = "Products", description = "Product catalog management")
@RequiredArgsConstructor
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final GetProductsUseCase getProductsUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new product")
    @ApiResponse(responseCode = "201", description = "Product created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        return createProductUseCase.createProduct(request);
    }

    @GetMapping
    @Operation(summary = "Get all products")
    public List<ProductResponse> getAllProducts() {
        return getProductsUseCase.getAllProducts();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(getProductsUseCase.getProductById(id));
    }
}
