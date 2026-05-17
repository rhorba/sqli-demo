package com.sqli.retailflow.inventory.domain.port.out;

import com.sqli.retailflow.inventory.domain.model.StockEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockRepositoryPort {
    StockEntity save(StockEntity stock);
    Optional<StockEntity> findByProductId(UUID productId);
    List<StockEntity> findAll();
}
