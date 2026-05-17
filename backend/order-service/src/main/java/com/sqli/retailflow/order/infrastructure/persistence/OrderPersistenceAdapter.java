package com.sqli.retailflow.order.infrastructure.persistence;

import com.sqli.retailflow.order.domain.model.OrderEntity;
import com.sqli.retailflow.order.domain.port.out.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class OrderPersistenceAdapter implements OrderRepositoryPort {

    private final SpringDataOrderRepository repository;

    @Override
    public OrderEntity save(OrderEntity order) {
        return repository.save(order);
    }

    @Override
    public Optional<OrderEntity> findById(UUID id) {
        return repository.findByIdWithItems(id);
    }

    @Override
    public List<OrderEntity> findAll() {
        return repository.findAllWithItems();
    }
}
