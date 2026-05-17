# RetailFlow — Interview Q&A

> Technical questions and answers about the RetailFlow demo, targeting the SQLI Expert Technique Java Back/Full Stack position.

---

## Table of Contents

1. [Architecture](#1-architecture)
2. [Java 21 & Spring Boot 3.4](#2-java-21--spring-boot-34)
3. [Hexagonal Architecture (Ports & Adapters)](#3-hexagonal-architecture-ports--adapters)
4. [Apache Kafka & Event-Driven Design](#4-apache-kafka--event-driven-design)
5. [Database — PostgreSQL & Flyway](#5-database--postgresql--flyway)
6. [MapStruct](#6-mapstruct)
7. [Testing Strategy](#7-testing-strategy)
8. [Angular 18 Frontend](#8-angular-18-frontend)
9. [Docker & DevOps](#9-docker--devops)
10. [SOLID & Clean Code Principles](#10-solid--clean-code-principles)
11. [Microservices Design Decisions](#11-microservices-design-decisions)

---

## 1. Architecture

**Q: Give a high-level overview of RetailFlow.**

RetailFlow is an event-driven retail order management platform composed of three independent Spring Boot 3.4 microservices:
- **product-service** (port 8081): manages the product catalog via REST.
- **order-service** (port 8082): handles order placement and a status workflow (PENDING → CONFIRMED → SHIPPED). On each state change it publishes a Kafka event.
- **inventory-service** (port 8083): consumes `order.created` events from Kafka and reserves stock accordingly.

All services follow **Hexagonal Architecture**, use **Flyway** for schema migrations, and **MapStruct** for mapping. The frontend is **Angular 18** with TanStack Query and Tailwind CSS. The whole stack is orchestrated with **Docker Compose**.

---

**Q: Why did you choose an event-driven architecture instead of synchronous REST calls between services?**

Synchronous REST between microservices creates **tight coupling and cascading failures** — if inventory-service is slow, order-service waits. With Kafka:
- **Decoupling**: order-service publishes a fact ("an order was created") and doesn't care who reacts.
- **Resilience**: if inventory-service is down, messages queue in Kafka and are processed when it recovers.
- **Scalability**: multiple consumers can independently react to the same event (e.g., adding a notification-service later requires zero changes to order-service).
- **Audit trail**: Kafka retains events, giving a natural event log.

The trade-off is operational complexity (Kafka cluster to manage), which Docker Compose handles for local development.

---

**Q: How do the services discover each other?**

They don't need to, by design. Services communicate through **Kafka topics**, not direct HTTP calls. The only exception is the Angular frontend, which calls product-service and order-service via REST. In Docker Compose, service names act as DNS hostnames (`product-service:8081`). For a production setup, a service mesh or API gateway would handle discovery and routing.

---

**Q: What Kafka topics does the system use and who produces/consumes them?**

| Topic | Producer | Consumer |
|---|---|---|
| `order.created` | order-service | inventory-service |
| `order.status-changed` | order-service | *(extensible — e.g., notification-service)* |

The key of each message is the `orderId` (UUID string), which ensures **ordering within a partition** for the same order.

---

## 2. Java 21 & Spring Boot 3.4

**Q: Why Java 21? What features from it did you use?**

Java 21 is the latest **LTS (Long Term Support)** release, which is what SQLI's stack requires. Features used in this project:

- **Records** — all DTOs and Kafka events are Java records (`record OrderCreatedEvent(...) {}`). They are immutable, concise, and ideal for value objects. No Lombok needed for DTOs.
- **`List.of()` / `.stream().toList()`** — modern, immutable collections throughout services.
- **Pattern matching** — implicitly via `instanceof` checks (where applicable).
- **Virtual threads** — available via Spring Boot 3.4's `spring.threads.virtual.enabled=true` for high-throughput scenarios (not enabled by default here but easily added).

---

**Q: What's notable about Spring Boot 3.4?**

Spring Boot 3.4 (built on Spring 6.2) brings:
- **Jakarta EE 10** namespace (`jakarta.*` not `javax.*`).
- **GraalVM Native Image** support for faster startup.
- **Improved Observability** via Micrometer integration with Actuator.
- **Virtual threads** support (Project Loom) via configuration.
- **`ProblemDetail`** (RFC 7807) for structured error responses — used in `GlobalExceptionHandler` throughout this project.
- **Flyway 10** compatibility — used in all three services.

---

**Q: Why do you use `record` for DTOs and events?**

Records are **structurally immutable** — once created, their fields cannot change. This is ideal for:
- **DTOs**: request/response objects should never be mutated after creation.
- **Kafka events**: events are facts that happened; they must be immutable.
- **Concise syntax**: no boilerplate getters, `equals`, `hashCode`, `toString`.
- **Thread safety**: immutable objects are inherently thread-safe.

Example from the project:
```java
public record OrderCreatedEvent(UUID orderId, UUID customerId,
    List<OrderItemDto> items, BigDecimal totalAmount, Instant occurredAt) {}
```

---

**Q: Why is `open-in-view: false` set in application.yml?**

Spring Boot enables **Open Session in View** by default, which keeps the JPA EntityManager open for the entire HTTP request — including the view rendering phase. This causes **lazy loading to silently work in the controller/view**, hiding N+1 query problems. Disabling it forces all data fetching to happen in the `@Transactional` service layer where it belongs. It also prevents connection pool exhaustion under load.

---

## 3. Hexagonal Architecture (Ports & Adapters)

**Q: Explain the hexagonal architecture structure in this project.**

Each service is split into four packages:

```
domain/
├── model/       ← JPA entities, enums — zero framework imports
└── port/
    ├── in/      ← Use case interfaces (what the app can DO)
    └── out/     ← Repository/publisher interfaces (what the app NEEDS)

application/
├── dto/         ← Java records (request/response)
├── event/       ← Kafka event records
├── mapper/      ← MapStruct interfaces
└── service/     ← Business logic; implements `port/in`, uses `port/out`

infrastructure/
├── persistence/ ← JPA repository adapter (implements `port/out`)
└── messaging/   ← Kafka producer/consumer adapters

presentation/
└── controller/  ← REST controllers, GlobalExceptionHandler
```

The rule: **domain has zero framework imports**. It doesn't know Spring, JPA annotations aside from entity mapping, or Kafka. Business logic in `application/service/` is pure Java.

---

**Q: What is the difference between a "port" and an "adapter"?**

- A **port** is an interface that defines a contract. There are two kinds:
  - *Driving port* (`port/in`): what the outside world can ask the application to do (e.g., `PlaceOrderUseCase`).
  - *Driven port* (`port/out`): what the application needs from the outside world (e.g., `OrderRepositoryPort`, `OrderEventPublisherPort`).
- An **adapter** is the implementation that connects a port to a specific technology:
  - `OrderPersistenceAdapter` implements `OrderRepositoryPort` using Spring Data JPA.
  - `KafkaOrderEventPublisher` implements `OrderEventPublisherPort` using Spring Kafka.

This means you can swap Kafka for RabbitMQ by writing a new adapter — the business logic (`OrderService`) doesn't change.

---

**Q: Why does `OrderController` depend on interfaces (`PlaceOrderUseCase`, `GetOrdersUseCase`) rather than `OrderService` directly?**

This is the **Dependency Inversion Principle** (the D in SOLID). The controller depends on an abstraction (the use case interface), not a concrete class. This:
- Makes controllers **independently testable** — mock the interface, not the full service.
- Enforces that controllers **only call what they're allowed to** — the interface exposes exactly the methods a controller needs.
- Allows swapping the implementation (e.g., replacing `OrderService` with a CQRS command bus) without touching the controller.

---

**Q: Where is business logic allowed to live?**

Only in `application/service/`. Strict rules:
- **Controllers**: receive HTTP, validate input, delegate to use case, return response. No `if/else` on business state.
- **Entities**: hold state and JPA mapping. No business methods (except simple lifecycle callbacks like `@PrePersist`).
- **Adapters**: translate between domain objects and external representations (DB rows, Kafka messages). No business logic.

Example of the violation I avoided: putting stock reservation logic inside `OrderCreatedConsumer`. Instead, the consumer delegates to `StockService.reserveStock()`.

---

## 4. Apache Kafka & Event-Driven Design

**Q: How is Kafka configured in Spring Boot?**

In `application.yml`:
```yaml
spring:
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: inventory-service
      auto-offset-reset: earliest
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.sqli.retailflow.*"
```

`JsonSerializer`/`JsonDeserializer` handles serialization automatically. `trusted.packages` is a security control that prevents deserialization of arbitrary classes.

---

**Q: What is a consumer group and why does inventory-service use `groupId = "inventory-service"`?**

A consumer group ensures that **each partition of a topic is consumed by exactly one consumer within the group**. Setting `groupId = "inventory-service"` means:
- All instances of inventory-service share the same group → messages are load-balanced across instances (horizontal scaling).
- If you add a notification-service with a different group ID, it gets its own independent copy of every message — both services see all events.

---

**Q: What happens if inventory-service is down when an order is placed?**

Because `auto-offset-reset: earliest` is set and Kafka retains messages by default (7 days), when inventory-service comes back up, it will **read all messages it missed** from its last committed offset. This is one of the key advantages of Kafka over synchronous HTTP — no data is lost during downtime.

---

**Q: Why are Kafka events defined as separate records in inventory-service instead of sharing a library?**

Deliberately avoiding a **shared-library anti-pattern**. If order-service and inventory-service shared an `events-common` library:
- A change to `OrderCreatedEvent` in order-service forces a recompile/redeploy of inventory-service.
- Services become coupled at the binary level, defeating the purpose of microservices.

Instead, inventory-service defines its own local copy of `OrderCreatedEvent`. This is the **schema evolution** approach — each service owns its view of the contract. Kafka's JSON serialization is flexible enough to handle minor schema differences.

---

**Q: How is the Kafka message key used?**

The key is the `orderId` as a UUID string:
```java
kafkaTemplate.send(TOPIC_ORDER_CREATED, event.orderId().toString(), event);
```

In Kafka, **messages with the same key go to the same partition**, guaranteeing order. All events for a given order (created, confirmed, shipped) will always be processed in order by a consumer — critical for state machine correctness.

---

## 5. Database — PostgreSQL & Flyway

**Q: Why use Flyway instead of `spring.jpa.hibernate.ddl-auto: update`?**

`ddl-auto: update` is **non-deterministic and dangerous**:
- It can silently drop columns it thinks are unused.
- It cannot rename columns — it drops and recreates them (data loss).
- It produces different results on different database states — not reproducible.
- It has no version history — you can't see what changed or when.

Flyway uses **versioned SQL scripts** (`V1__init_products.sql`, `V2__add_index.sql`...) that run in order, are checksummed, and are tracked in a `flyway_schema_history` table. The same migration applied to any environment always produces the same schema.

---

**Q: Why does each service have its own database?**

This is the **Database per Service** pattern, a microservices best practice:
- **Loose coupling**: services can't bypass the API and query each other's tables directly.
- **Independent evolution**: product-service can migrate its schema without affecting order-service.
- **Technology freedom**: each service could use a different DB type if needed.
- **Failure isolation**: a slow query in one DB doesn't block other services.

The trade-off is that joins across services require API calls or event-driven denormalization.

---

**Q: Why is there a `CREATE INDEX` in the Flyway migration?**

Indexes are part of the schema contract and must be versioned. In `V1__init_orders.sql`:
```sql
CREATE INDEX idx_orders_customer   ON orders (customer_id);
CREATE INDEX idx_orders_status     ON orders (status);
CREATE INDEX idx_order_items_order ON order_items (order_id);
```
`customer_id` and `status` are common filter fields. Without indexes, a `WHERE status = 'PENDING'` on a large orders table does a full sequential scan. `order_id` on `order_items` prevents N+1 query behavior when joining.

---

**Q: What does `ddl-auto: validate` do in the services?**

With Flyway running, Hibernate only needs to **validate** that the existing schema matches the entity mappings — it never creates or alters tables. This is the safest production setting: if a migration is missing, Spring Boot fails fast on startup with a clear error rather than silently creating wrong tables.

---

## 6. MapStruct

**Q: Why MapStruct and not manual mapping or ModelMapper?**

| | MapStruct | ModelMapper | Manual |
|---|---|---|---|
| Performance | Compile-time generated code | Reflection at runtime | Fast |
| Error detection | Compile-time errors | Runtime errors | Compile-time |
| Boilerplate | Minimal (interface + annotations) | Minimal | High |
| Debugging | Easy (generated code is readable) | Hard (reflection) | Easy |

MapStruct generates plain Java code at compile time. If a field is missing or the types are incompatible, **the build fails** — you find out immediately, not in production.

---

**Q: How does MapStruct know which fields to map?**

By convention: it matches fields by **name and type**. For mismatches:
- `@Mapping(target = "id", ignore = true)` — skip auto-generated fields.
- `@Mapping(target = "subtotal", expression = "java(...)")` — computed fields.
- `@Mapping(source = "productCategory", target = "category")` — rename fields.

Example in this project (`OrderMapper`):
```java
@Mapping(target = "subtotal",
  expression = "java(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))")
OrderItemResponse toItemResponse(OrderItemEntity item);
```

---

**Q: Why must Lombok be declared before MapStruct in the annotation processor path?**

MapStruct needs to see the **getters and setters generated by Lombok** to generate correct mapping code. Since annotation processors run in the order they're declared, Lombok must generate its getters/setters first, then MapStruct reads them. The wrong order causes `cannot find symbol` compilation errors.

---

## 7. Testing Strategy

**Q: What is the testing pyramid used in this project?**

```
        /\
       /  \   Integration (@EmbeddedKafka)
      /    \
     /------\  Service unit tests (Mockito)
    /        \
   /----------\  (domain validation — inline in entity constructors)
```

- **Unit tests** (`ProductServiceTest`, `OrderServiceTest`, `StockServiceTest`): fast, no Spring context, Mockito mocks all dependencies. They test pure business logic.
- **Integration test** (`OrderServiceIntegrationTest`): uses `@EmbeddedKafka` to spin up an in-memory Kafka broker and verify that placing an order actually publishes an `OrderCreatedEvent` to the topic.
- **No mocking the database**: tested via real SQL in integration tests — mocking JPA leads to false confidence.

---

**Q: How does `@EmbeddedKafka` work?**

`@EmbeddedKafka` starts an **in-memory Kafka broker** as part of the Spring test context:
```java
@EmbeddedKafka(partitions = 1, topics = {"order.created"})
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
```
The test then creates a real `KafkaMessageListenerContainer` that subscribes to the topic and puts received messages into a `BlockingQueue`. After calling `orderService.placeOrder()`, we poll the queue with a timeout and assert on the received event. This verifies the full Kafka producer path without needing a real Kafka cluster.

---

**Q: Why is JaCoCo set to 70% minimum line coverage?**

70% is a pragmatic threshold that:
- Forces coverage of the **critical application service layer**.
- Excludes boilerplate (main class, generated MapStruct code, simple getters) which are hard to test meaningfully.
- Is enforced at **build time** (`mvn verify`) — a failing coverage check breaks the CI pipeline, preventing merges.

In `pom.xml`:
```xml
<limit>
    <counter>LINE</counter>
    <value>COVEREDRATIO</value>
    <minimum>0.70</minimum>
</limit>
```

---

**Q: Why does `OrderServiceTest` mock `OrderMapper` instead of using a real mapper?**

Because `OrderServiceTest` is a **unit test** — it tests `OrderService` in complete isolation. Using a real `OrderMapper` would make the test depend on MapStruct's code generation, turning it into a partial integration test. If `OrderMapper` has a bug, it should be caught in a dedicated mapper test, not in `OrderServiceTest`. Mockito allows us to define exactly what the mapper returns, keeping the test focused.

---

## 8. Angular 18 Frontend

**Q: What is a standalone component in Angular 18?**

Before Angular 14, every component had to belong to an `@NgModule`. Standalone components declare their own imports directly:
```typescript
@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule], // ← direct imports
  template: `...`
})
```
This eliminates `NgModule` boilerplate, makes components self-contained, and enables better tree-shaking. Angular 18 defaults to standalone — the entire frontend in this project has no `NgModule`.

---

**Q: Why TanStack Query instead of managing loading/error state manually?**

Manual `isLoading`, `error`, `data` signals for every HTTP call lead to repetitive boilerplate and common bugs (forgetting to reset `isLoading` on error). TanStack Query provides:
- **Automatic caching** with configurable `staleTime`.
- **Background refetch** when the component mounts.
- **Optimistic updates** and cache invalidation via `queryClient.invalidateQueries()`.
- **Deduplication** of concurrent identical requests.

Example:
```typescript
productsQuery = injectQuery(() => ({
  queryKey: ['products'],
  queryFn: () => lastValueFrom(this.productService.getAll()),
}));
```
After a product is created, `queryClient.invalidateQueries({ queryKey: ['products'] })` re-fetches automatically.

---

**Q: How is server state differentiated from local UI state?**

| State type | Solution |
|---|---|
| Server data (lists, entities) | TanStack Query (`injectQuery`, `injectMutation`) |
| Local UI state (modal open/closed) | Angular `signal()` |
| Form data | `ReactiveFormsModule` (`FormBuilder`, `FormGroup`) |

Example in `ProductListComponent`:
```typescript
showForm = signal(false);          // local UI state — signal
productsQuery = injectQuery(...);  // server state — TanStack Query
form = this.fb.group({...});       // form state — ReactiveFormsModule
```

---

**Q: Why does the nginx.conf proxy API calls to backend services?**

In Docker, the frontend container (nginx) and the backend services run in separate containers. The Angular app is served as static files — it can't directly call `http://product-service:8081` because that hostname only resolves **inside Docker's network**, not in the user's browser.

The solution: nginx proxies `/api/products` → `http://product-service:8081/api/products` server-side. The browser always talks to `http://localhost:3000`, and nginx forwards the request to the correct service inside Docker.

---

## 9. Docker & DevOps

**Q: Explain the multi-stage Dockerfile used for the backend services.**

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build   # Stage 1: compile
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q             # cache dependencies separately
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine           # Stage 2: minimal runtime
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER appuser                                 # non-root for security
EXPOSE 8081
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
```

Benefits:
- **Final image has no Maven or JDK** — only a JRE. Much smaller (≈120MB vs ≈600MB).
- **Layer caching**: `pom.xml` + `dependency:go-offline` is a separate layer — only re-runs if `pom.xml` changes, not on every source change.
- **Non-root user**: security best practice.
- **`-XX:+UseContainerSupport`**: tells the JVM to respect Docker's CPU and memory limits instead of reading host values.

---

**Q: Why are there health checks in docker-compose.yml?**

Without health checks, Docker starts services in declaration order but doesn't wait for them to be **ready**. order-service might start before Kafka is ready to accept connections, causing it to crash on startup.

```yaml
kafka:
  healthcheck:
    test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
    interval: 10s
    retries: 15
```

Combined with `depends_on: condition: service_healthy`, Docker Compose won't start order-service until Kafka passes its health check. Same pattern for PostgreSQL databases.

---

**Q: Explain the GitLab CI/CD pipeline stages.**

The `.gitlab-ci.yml` defines 4 stages:

1. **`build`** — `mvn package -DskipTests` per service + `pnpm build` for the frontend. Produces JAR artifacts shared with the test stage.
2. **`test`** — `mvn verify` per service: runs JUnit tests, enforces JaCoCo ≥70%, runs Checkstyle. Produces JUnit XML reports visible in GitLab's test dashboard.
3. **`security`** — Semgrep (SAST for OWASP top 10) + Trivy (dependency and filesystem vulnerability scan). `allow_failure: true` means security findings don't block the pipeline but are reported.
4. **`docker`** — `docker compose build` to verify all Dockerfiles build successfully. Only runs on `main` and `develop` branches.

---

## 10. SOLID & Clean Code Principles

**Q: Give an example of the Single Responsibility Principle in this codebase.**

`KafkaOrderEventPublisher` has exactly one job: publish order events to Kafka. It doesn't decide *when* to publish, doesn't construct the event payload, and doesn't know about business rules. Compare this to a violation: putting `kafkaTemplate.send(...)` directly in `OrderController`. The controller would then have two responsibilities — handling HTTP and publishing events.

---

**Q: Give an example of the Open/Closed Principle.**

The `OrderEventPublisherPort` interface:
```java
public interface OrderEventPublisherPort {
    void publishOrderCreated(OrderCreatedEvent event);
    void publishOrderStatusChanged(OrderStatusChangedEvent event);
}
```
`OrderService` depends on this interface. To replace Kafka with RabbitMQ, I write a `RabbitOrderEventPublisher` that implements this interface and swap it in Spring's context — **no change to `OrderService`**. The service is closed for modification, open for extension.

---

**Q: Give an example of the Dependency Inversion Principle.**

`OrderService` (high-level) depends on `OrderRepositoryPort` and `OrderEventPublisherPort` (abstractions), not on `OrderPersistenceAdapter` or `KafkaOrderEventPublisher` (low-level details). Spring's dependency injection wires the concrete adapter at runtime. The high-level business logic is never coupled to the infrastructure technology.

---

**Q: What is YAGNI and how did it influence this project?**

YAGNI = *You Aren't Gonna Need It*. Decisions made under this principle:
- **No API Gateway** (Spring Cloud Gateway was in the comprehensive scope — skipped because `nginx` proxying is sufficient for a demo).
- **No caching layer** (Redis) — not proven necessary.
- **No authentication** (JWT/Spring Security) — not in scope for a demo.
- **No Kubernetes** — Docker Compose is sufficient.
- **No shared event library** — added coupling with no current benefit.

Every architectural choice has a "do we need this RIGHT NOW?" gate. Complexity is added only when there's a concrete, proven reason.

---

**Q: How is the DRY principle balanced with avoiding premature abstraction?**

`GlobalExceptionHandler` is duplicated across all three services — each has its own copy. This appears to violate DRY. However, extracting it to a shared library would:
- Create a dependency between independently deployable services.
- Force redeployment of all services when one handler changes.
- Add complexity (Maven multi-module or separate artifact) for three files.

The rule of thumb: **three repetitions justify extraction** only when extraction doesn't create worse problems. Here the duplication cost is low; the coupling cost of extraction is high.

---

## 11. Microservices Design Decisions

**Q: How would you handle a distributed transaction between order-service and inventory-service?**

In this demo, stock reservation is **eventually consistent** — inventory-service reacts to the Kafka event asynchronously. For a production system where we need stronger guarantees, the **Saga pattern** would be used:

- **Choreography-based Saga** (already partially implemented): each service publishes events; compensating events handle rollback (e.g., `OrderCancelledEvent` triggers `StockReleasedEvent`).
- **Orchestration-based Saga**: an `OrderSaga` orchestrator coordinates the workflow, calling each service and handling compensations explicitly.

The two-phase commit (2PC) across microservices is avoided — it creates distributed locks and tight coupling.

---

**Q: How would you scale order-service to handle 10,000 orders per second?**

1. **Horizontal scaling**: run N order-service instances behind a load balancer. They all connect to the same Kafka cluster and PostgreSQL (via connection pooling with HikariCP).
2. **Kafka partitioning**: increase `order.created` topic partitions to N. Each inventory-service instance gets exclusive ownership of a partition subset.
3. **Database connection pooling**: tune HikariCP `maximumPoolSize` per instance.
4. **Virtual threads** (Java 21 + Spring Boot 3.4): enable `spring.threads.virtual.enabled=true` for non-blocking I/O without reactive programming complexity.
5. **Read replicas**: direct `@Transactional(readOnly = true)` queries to a PostgreSQL read replica.

---

**Q: How would you add a new notification-service that sends emails when orders are confirmed?**

Zero changes to existing services. notification-service would:
1. Create a new Spring Boot app with a Kafka consumer.
2. Listen to `order.status-changed` topic with a new consumer group ID (`notification-service`).
3. Filter events where `newStatus == CONFIRMED`.
4. Send email via SMTP/SendGrid.

This is the power of event-driven architecture: **adding a new consumer never touches the producer**.

---

**Q: What monitoring would you add in production?**

- **Distributed tracing**: add `spring-boot-starter-actuator` + Micrometer Tracing + Jaeger/Zipkin. Every request gets a `traceId` propagated through Kafka headers.
- **Metrics**: Actuator exposes Prometheus-compatible metrics at `/actuator/metrics`. Scrape with Prometheus, visualize in Grafana.
- **Kafka consumer lag**: monitor with `kafka-consumer-groups.sh --describe` or Kafka UI. Lag growing = inventory-service can't keep up.
- **Alerts**: error rate > 1%, p99 latency > 2s, consumer lag > 1000 messages, health check failures.

---

*Good luck with the SQLI interview!*
