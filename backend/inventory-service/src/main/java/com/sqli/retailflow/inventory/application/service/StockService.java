package com.sqli.retailflow.inventory.application.service;

import com.sqli.retailflow.inventory.application.dto.OrderCreatedEvent;
import com.sqli.retailflow.inventory.application.dto.StockResponse;
import com.sqli.retailflow.inventory.application.mapper.StockMapper;
import com.sqli.retailflow.inventory.domain.model.StockEntity;
import com.sqli.retailflow.inventory.domain.port.in.GetStockUseCase;
import com.sqli.retailflow.inventory.domain.port.out.StockRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StockService implements GetStockUseCase {

    private final StockRepositoryPort stockRepository;
    private final StockMapper stockMapper;

    public void reserveStock(OrderCreatedEvent event) {
        log.info("Reserving stock for orderId={}", event.orderId());
        event.items().forEach(item -> {
            StockEntity stock = stockRepository.findByProductId(item.productId())
                .orElseGet(() -> createStockEntry(item.productId(), item.productName()));
            stock.setReserved(stock.getReserved() + item.quantity());
            stockRepository.save(stock);
            log.debug("Reserved {} units of product={}", item.quantity(), item.productId());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponse> getAllStock() {
        return stockRepository.findAll().stream()
            .map(stockMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StockResponse getStockByProductId(UUID productId) {
        return stockRepository.findByProductId(productId)
            .map(stockMapper::toResponse)
            .orElseThrow(() -> new NoSuchElementException("Stock not found for product: " + productId));
    }

    private StockEntity createStockEntry(UUID productId, String productName) {
        StockEntity stock = new StockEntity();
        stock.setProductId(productId);
        stock.setProductName(productName);
        stock.setAvailable(100);
        stock.setReserved(0);
        return stock;
    }
}
