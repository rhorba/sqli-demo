package com.sqli.retailflow.inventory.presentation.controller;

import com.sqli.retailflow.inventory.application.dto.StockResponse;
import com.sqli.retailflow.inventory.domain.port.in.GetStockUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins = "*")
@Tag(name = "Stock", description = "Inventory stock levels — updated via Kafka events from order-service")
@RequiredArgsConstructor
public class StockController {

    private final GetStockUseCase getStockUseCase;

    @GetMapping
    @Operation(summary = "Get all stock levels")
    public List<StockResponse> getAllStock() {
        return getStockUseCase.getAllStock();
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get stock for a specific product")
    public ResponseEntity<StockResponse> getStockByProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(getStockUseCase.getStockByProductId(productId));
    }
}
