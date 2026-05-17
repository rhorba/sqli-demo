package com.sqli.retailflow.order.domain.port.out;

import com.sqli.retailflow.order.domain.model.OrderEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {
    OrderEntity save(OrderEntity order);
    Optional<OrderEntity> findById(UUID id);
    List<OrderEntity> findAll();
}
