package com.sqli.retailflow.inventory.infrastructure.messaging;

import com.sqli.retailflow.inventory.application.dto.OrderCreatedEvent;
import com.sqli.retailflow.inventory.application.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class OrderCreatedConsumer {

    private final StockService stockService;

    @KafkaListener(topics = "order.created", groupId = "inventory-service")
    void handle(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent orderId={}", event.orderId());
        stockService.reserveStock(event);
    }
}
