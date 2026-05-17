package com.sqli.retailflow.inventory.application.service;

import com.sqli.retailflow.inventory.application.dto.OrderCreatedEvent;
import com.sqli.retailflow.inventory.application.dto.StockResponse;
import com.sqli.retailflow.inventory.application.mapper.StockMapper;
import com.sqli.retailflow.inventory.domain.model.StockEntity;
import com.sqli.retailflow.inventory.domain.port.out.StockRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService")
class StockServiceTest {

    @Mock private StockRepositoryPort stockRepository;
    @Mock private StockMapper stockMapper;
    @InjectMocks private StockService stockService;

    @Test
    @DisplayName("reserveStock should increment reserved quantity for existing product")
    void shouldIncrementReservedForExistingProduct() {
        UUID productId = UUID.randomUUID();
        StockEntity existing = new StockEntity();
        existing.setProductId(productId);
        existing.setProductName("Widget");
        existing.setAvailable(100);
        existing.setReserved(5);

        var event = new OrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID(),
            List.of(new OrderCreatedEvent.OrderItemDto(productId, "Widget", 3, new BigDecimal("10"))),
            new BigDecimal("30"), Instant.now());

        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(existing));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        stockService.reserveStock(event);

        ArgumentCaptor<StockEntity> captor = ArgumentCaptor.forClass(StockEntity.class);
        verify(stockRepository).save(captor.capture());
        assertThat(captor.getValue().getReserved()).isEqualTo(8);
    }

    @Test
    @DisplayName("reserveStock should create new stock entry for unknown product")
    void shouldCreateNewEntryForUnknownProduct() {
        UUID productId = UUID.randomUUID();
        var event = new OrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID(),
            List.of(new OrderCreatedEvent.OrderItemDto(productId, "NewWidget", 2, BigDecimal.TEN)),
            BigDecimal.TEN, Instant.now());

        when(stockRepository.findByProductId(productId)).thenReturn(Optional.empty());
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        stockService.reserveStock(event);

        ArgumentCaptor<StockEntity> captor = ArgumentCaptor.forClass(StockEntity.class);
        verify(stockRepository).save(captor.capture());
        assertThat(captor.getValue().getReserved()).isEqualTo(2);
        assertThat(captor.getValue().getAvailable()).isEqualTo(100);
    }
}
