package com.sqli.retailflow.order.application.service;

import com.sqli.retailflow.order.application.dto.OrderResponse;
import com.sqli.retailflow.order.application.dto.PlaceOrderRequest;
import com.sqli.retailflow.order.application.event.OrderCreatedEvent;
import com.sqli.retailflow.order.application.event.OrderStatusChangedEvent;
import com.sqli.retailflow.order.application.mapper.OrderMapper;
import com.sqli.retailflow.order.domain.model.OrderEntity;
import com.sqli.retailflow.order.domain.model.OrderItemEntity;
import com.sqli.retailflow.order.domain.model.OrderStatus;
import com.sqli.retailflow.order.domain.port.in.GetOrdersUseCase;
import com.sqli.retailflow.order.domain.port.in.PlaceOrderUseCase;
import com.sqli.retailflow.order.domain.port.out.OrderEventPublisherPort;
import com.sqli.retailflow.order.domain.port.out.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService implements PlaceOrderUseCase, GetOrdersUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OrderEventPublisherPort eventPublisher;
    private final OrderMapper orderMapper;

    @Override
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        OrderEntity order = new OrderEntity();
        order.setCustomerId(request.customerId());
        order.setStatus(OrderStatus.PENDING);

        request.items().forEach(itemReq -> {
            OrderItemEntity item = orderMapper.toItemEntity(itemReq);
            order.addItem(item);
        });

        BigDecimal total = order.getItems().stream()
            .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);

        OrderEntity saved = orderRepository.save(order);

        eventPublisher.publishOrderCreated(toCreatedEvent(saved));

        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
            .map(orderMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        return orderRepository.findById(id)
            .map(orderMapper::toResponse)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
    }

    @Override
    public OrderResponse confirmOrder(UUID id) {
        return updateStatus(id, OrderStatus.CONFIRMED);
    }

    @Override
    public OrderResponse shipOrder(UUID id) {
        return updateStatus(id, OrderStatus.SHIPPED);
    }

    private OrderResponse updateStatus(UUID id, OrderStatus newStatus) {
        OrderEntity order = orderRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
        OrderStatus previous = order.getStatus();
        order.setStatus(newStatus);
        OrderEntity saved = orderRepository.save(order);

        eventPublisher.publishOrderStatusChanged(
            new OrderStatusChangedEvent(saved.getId(), previous, newStatus, Instant.now())
        );
        return orderMapper.toResponse(saved);
    }

    private OrderCreatedEvent toCreatedEvent(OrderEntity order) {
        List<OrderCreatedEvent.OrderItemDto> items = order.getItems().stream()
            .map(i -> new OrderCreatedEvent.OrderItemDto(
                i.getProductId(), i.getProductName(), i.getQuantity(), i.getUnitPrice()))
            .toList();
        return new OrderCreatedEvent(order.getId(), order.getCustomerId(), items,
            order.getTotalAmount(), Instant.now());
    }
}
