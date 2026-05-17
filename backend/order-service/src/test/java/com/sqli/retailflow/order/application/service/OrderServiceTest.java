package com.sqli.retailflow.order.application.service;

import com.sqli.retailflow.order.application.dto.OrderItemRequest;
import com.sqli.retailflow.order.application.dto.OrderItemResponse;
import com.sqli.retailflow.order.application.dto.OrderResponse;
import com.sqli.retailflow.order.application.dto.PlaceOrderRequest;
import com.sqli.retailflow.order.application.event.OrderCreatedEvent;
import com.sqli.retailflow.order.application.mapper.OrderMapper;
import com.sqli.retailflow.order.domain.model.OrderEntity;
import com.sqli.retailflow.order.domain.model.OrderItemEntity;
import com.sqli.retailflow.order.domain.model.OrderStatus;
import com.sqli.retailflow.order.domain.port.out.OrderEventPublisherPort;
import com.sqli.retailflow.order.domain.port.out.OrderRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock private OrderRepositoryPort orderRepository;
    @Mock private OrderEventPublisherPort eventPublisher;
    @Mock private OrderMapper orderMapper;
    @InjectMocks private OrderService orderService;

    private UUID customerId = UUID.randomUUID();
    private UUID productId = UUID.randomUUID();

    private OrderEntity buildOrderEntity() {
        OrderEntity order = new OrderEntity();
        order.setId(UUID.randomUUID());
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("29.99"));
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        OrderItemEntity item = new OrderItemEntity();
        item.setId(UUID.randomUUID());
        item.setProductId(productId);
        item.setProductName("Widget");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("29.99"));
        item.setOrder(order);
        order.getItems().add(item);
        return order;
    }

    private OrderResponse buildOrderResponse(OrderEntity e) {
        var itemResp = new OrderItemResponse(e.getItems().get(0).getId(), productId,
            "Widget", 1, new BigDecimal("29.99"), new BigDecimal("29.99"));
        return new OrderResponse(e.getId(), customerId, e.getStatus(),
            e.getTotalAmount(), List.of(itemResp), e.getCreatedAt(), e.getUpdatedAt());
    }

    @Nested
    @DisplayName("placeOrder")
    class PlaceOrder {

        @Test
        @DisplayName("should persist order and publish OrderCreatedEvent")
        void shouldPersistAndPublishEvent() {
            var request = new PlaceOrderRequest(customerId,
                List.of(new OrderItemRequest(productId, "Widget", 1, new BigDecimal("29.99"))));
            OrderEntity entity = buildOrderEntity();
            OrderResponse response = buildOrderResponse(entity);

            when(orderMapper.toItemEntity(any())).thenAnswer(inv -> {
                OrderItemEntity item = new OrderItemEntity();
                item.setProductId(productId);
                item.setProductName("Widget");
                item.setQuantity(1);
                item.setUnitPrice(new BigDecimal("29.99"));
                return item;
            });
            when(orderRepository.save(any())).thenReturn(entity);
            when(orderMapper.toResponse(entity)).thenReturn(response);

            OrderResponse result = orderService.placeOrder(request);

            assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("29.99"));

            ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(eventPublisher).publishOrderCreated(captor.capture());
            assertThat(captor.getValue().orderId()).isEqualTo(entity.getId());
        }
    }

    @Nested
    @DisplayName("confirmOrder")
    class ConfirmOrder {

        @Test
        @DisplayName("should update status to CONFIRMED and publish status event")
        void shouldConfirmAndPublish() {
            OrderEntity entity = buildOrderEntity();
            when(orderRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(orderMapper.toResponse(any())).thenReturn(buildOrderResponse(entity));

            orderService.confirmOrder(entity.getId());

            verify(eventPublisher).publishOrderStatusChanged(any());
            assertThat(entity.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("should throw when order not found")
        void shouldThrowWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(orderRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.confirmOrder(id))
                .isInstanceOf(NoSuchElementException.class);
        }
    }
}
