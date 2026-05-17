package com.sqli.retailflow.inventory.infrastructure.persistence;

import com.sqli.retailflow.inventory.domain.model.StockEntity;
import com.sqli.retailflow.inventory.domain.port.out.StockRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class StockPersistenceAdapter implements StockRepositoryPort {

    private final SpringDataStockRepository repository;

    @Override
    public StockEntity save(StockEntity stock) {
        return repository.save(stock);
    }

    @Override
    public Optional<StockEntity> findByProductId(UUID productId) {
        return repository.findById(productId);
    }

    @Override
    public List<StockEntity> findAll() {
        return repository.findAll();
    }
}
