package com.sqli.retailflow.order.application.service;

import com.sqli.retailflow.order.application.dto.OrderItemRequest;
import com.sqli.retailflow.order.application.dto.PlaceOrderRequest;
import com.sqli.retailflow.order.application.event.OrderCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"order.created", "order.status-changed"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("OrderService Integration — Kafka event publishing")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private BlockingQueue<ConsumerRecord<String, OrderCreatedEvent>> records;
    private KafkaMessageListenerContainer<String, OrderCreatedEvent> container;

    @BeforeEach
    void setUp() {
        records = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString(),
            ConsumerConfig.GROUP_ID_CONFIG, "test-consumer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
            JsonDeserializer.TRUSTED_PACKAGES, "com.sqli.retailflow.*",
            JsonDeserializer.VALUE_DEFAULT_TYPE, OrderCreatedEvent.class.getName()
        );

        var factory = new DefaultKafkaConsumerFactory<String, OrderCreatedEvent>(consumerProps);
        ContainerProperties containerProps = new ContainerProperties("order.created");
        container = new KafkaMessageListenerContainer<>(factory, containerProps);
        container.setupMessageListener((MessageListener<String, OrderCreatedEvent>) records::add);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
    }

    @Test
    @DisplayName("placeOrder should publish OrderCreatedEvent to Kafka")
    void shouldPublishOrderCreatedEventToKafka() throws InterruptedException {
        var request = new PlaceOrderRequest(
            UUID.randomUUID(),
            List.of(new OrderItemRequest(UUID.randomUUID(), "Test Widget", 2, new BigDecimal("15.00")))
        );

        var result = orderService.placeOrder(request);

        ConsumerRecord<String, OrderCreatedEvent> received = records.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.value().orderId()).isEqualTo(result.id());
        assertThat(received.value().totalAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(received.value().items()).hasSize(1);

        container.stop();
    }
}
