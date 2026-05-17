package com.sqli.retailflow.order.presentation.controller;

import com.sqli.retailflow.order.application.dto.OrderResponse;
import com.sqli.retailflow.order.application.dto.PlaceOrderRequest;
import com.sqli.retailflow.order.domain.port.in.GetOrdersUseCase;
import com.sqli.retailflow.order.domain.port.in.PlaceOrderUseCase;
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
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@Tag(name = "Orders", description = "Order management with event-driven status workflow")
@RequiredArgsConstructor
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final GetOrdersUseCase getOrdersUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place a new order — publishes OrderCreatedEvent to Kafka")
    @ApiResponse(responseCode = "201", description = "Order placed and event published")
    public OrderResponse placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return placeOrderUseCase.placeOrder(request);
    }

    @GetMapping
    @Operation(summary = "List all orders")
    public List<OrderResponse> getAllOrders() {
        return getOrdersUseCase.getAllOrders();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(getOrdersUseCase.getOrderById(id));
    }

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirm order — publishes OrderStatusChangedEvent")
    public OrderResponse confirmOrder(@PathVariable UUID id) {
        return getOrdersUseCase.confirmOrder(id);
    }

    @PatchMapping("/{id}/ship")
    @Operation(summary = "Ship order — publishes OrderStatusChangedEvent")
    public OrderResponse shipOrder(@PathVariable UUID id) {
        return getOrdersUseCase.shipOrder(id);
    }
}
