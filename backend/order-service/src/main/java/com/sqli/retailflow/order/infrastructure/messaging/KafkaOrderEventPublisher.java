package com.sqli.retailflow.order.infrastructure.messaging;

import com.sqli.retailflow.order.application.event.OrderCreatedEvent;
import com.sqli.retailflow.order.application.event.OrderStatusChangedEvent;
import com.sqli.retailflow.order.domain.port.out.OrderEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class KafkaOrderEventPublisher implements OrderEventPublisherPort {

    private static final String TOPIC_ORDER_CREATED = "order.created";
    private static final String TOPIC_ORDER_STATUS = "order.status-changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for orderId={}", event.orderId());
        kafkaTemplate.send(TOPIC_ORDER_CREATED, event.orderId().toString(), event);
    }

    @Override
    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Publishing OrderStatusChangedEvent orderId={} status={}", event.orderId(), event.newStatus());
        kafkaTemplate.send(TOPIC_ORDER_STATUS, event.orderId().toString(), event);
    }
}
