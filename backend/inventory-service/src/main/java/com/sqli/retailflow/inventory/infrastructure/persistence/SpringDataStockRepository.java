package com.sqli.retailflow.inventory.infrastructure.persistence;

import com.sqli.retailflow.inventory.domain.model.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataStockRepository extends JpaRepository<StockEntity, UUID> {
}
