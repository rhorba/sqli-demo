# RetailFlow — Event-Driven Retail Platform

> Demo application for SQLI Expert Technique Java Back/Full Stack position.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend                                 │
│              Angular 18 · Standalone · TanStack Query           │
│                      http://localhost:3000                       │
└──────────────┬──────────────────┬───────────────────────────────┘
               │                  │
   ┌───────────▼──────┐  ┌────────▼────────┐  ┌──────────────────┐
   │  product-service │  │  order-service  │  │inventory-service │
   │   :8081          │  │   :8082         │  │   :8083          │
   │                  │  │                 │  │                  │
   │  Spring Boot 3.4 │  │ Spring Boot 3.4 │  │ Spring Boot 3.4  │
   │  Hexagonal Arch  │  │ Hexagonal Arch  │  │ Hexagonal Arch   │
   │  Flyway + JPA    │  │ Flyway + JPA    │  │ Flyway + JPA     │
   │  MapStruct       │  │ MapStruct       │  │ MapStruct        │
   │  OpenAPI/Swagger │  │ OpenAPI/Swagger │  │ Kafka Consumer   │
   └────────┬─────────┘  └───────┬─────────┘  └────────┬─────────┘
            │                    │  publishes           │ consumes
   ┌────────▼─────────┐         │  OrderCreated        │
   │   product-db     │  ┌──────▼──────────────────────┘
   │   PostgreSQL:5432│  │         Apache Kafka :9092
   └──────────────────┘  │   Topics: order.created
                         │           order.status-changed
   ┌──────────────────┐  └──────────────────────────────────────┐
   │   order-db       │                                          │
   │   PostgreSQL:5433│  ┌───────────────────────────────────── │
   └──────────────────┘  │   inventory-db  PostgreSQL:5434       │
                         └───────────────────────────────────────┘
```

## Tech Stack

| Layer        | Technology                          |
|--------------|-------------------------------------|
| Language     | Java 21                             |
| Framework    | Spring Boot 3.4                     |
| Architecture | Hexagonal (Ports & Adapters)        |
| Messaging    | Apache Kafka 3.x (Spring Kafka)     |
| Persistence  | Spring Data JPA + Flyway            |
| Mapping      | MapStruct 1.6.3                     |
| Database     | PostgreSQL 16                       |
| API Docs     | OpenAPI 3 / Swagger UI              |
| Frontend     | Angular 18 Standalone + TanStack Query + Tailwind CSS |
| Testing      | JUnit 5 + Mockito + JaCoCo ≥70%    |
| CI/CD        | GitLab CI/CD (`.gitlab-ci.yml`)     |
| DevOps       | Docker + Docker Compose             |

## Project Structure (Hexagonal)

```
backend/{service}/src/main/java/com/sqli/retailflow/{service}/
├── domain/
│   ├── model/          ← JPA Entities (NO framework in logic)
│   └── port/
│       ├── in/         ← Use Case interfaces (driving ports)
│       └── out/        ← Repository/Publisher interfaces (driven ports)
├── application/
│   ├── dto/            ← Request/Response records
│   ├── event/          ← Kafka event records
│   ├── mapper/         ← MapStruct mappers (only mapping layer)
│   └── service/        ← Business logic, implements use cases
├── infrastructure/
│   ├── persistence/    ← JPA adapter (implements repository port)
│   └── messaging/      ← Kafka producer/consumer adapters
└── presentation/
    └── controller/     ← REST controllers + GlobalExceptionHandler
```

## Kafka Event Flow

```
POST /api/orders
      │
      ▼
  OrderService.placeOrder()
      │ publishes
      ▼
  Kafka Topic: order.created
      │
      ▼ consumed by
  inventory-service → StockService.reserveStock()
```

## Quick Start

### Prerequisites
- Docker Desktop

### Run the full stack
```bash
docker compose up --build
```

### Access Points
| Service          | URL                                           |
|------------------|-----------------------------------------------|
| Frontend         | http://localhost:3000                         |
| Kafka UI         | http://localhost:8080                         |
| Product API      | http://localhost:8081/swagger-ui.html         |
| Order API        | http://localhost:8082/swagger-ui.html         |
| Inventory API    | http://localhost:8083/swagger-ui.html         |

### Run tests (per service)
```bash
cd backend/product-service   && mvn clean verify
cd backend/order-service     && mvn clean verify
cd backend/inventory-service && mvn clean verify
```

### Run frontend locally
```bash
cd frontend
pnpm install
pnpm dev   # → http://localhost:4200
```

## Demo Flow

1. Open http://localhost:3000
2. Navigate to **Products** → Create a few products
3. Navigate to **Orders** → Place an order selecting products
4. Watch **Kafka UI** (http://localhost:8080) → see `order.created` event
5. Navigate to **Dashboard** → order appears in recent orders with PENDING status
6. Click **Confirm** → status changes, `order.status-changed` event published
7. Click **Ship** → status changes to SHIPPED

## CI/CD Pipeline (GitLab)

The `.gitlab-ci.yml` defines 4 stages:
1. **build** — Maven package per service + Angular build
2. **test** — `mvn verify` per service (JaCoCo ≥70% enforced)
3. **security** — Semgrep OWASP scan + Trivy filesystem scan
4. **docker** — `docker compose build` (only on `main`/`develop`)

## Architectural Decisions

### Why Hexagonal Architecture?
Decouples business logic from Spring/JPA/Kafka. The `domain/` package has zero framework imports — pure Java. This makes unit tests fast and framework-agnostic.

### Why Flyway over `ddl-auto=update`?
Schema migrations are versioned, reviewable, and reproducible across environments. `ddl-auto=update` is non-deterministic and can silently drop columns.

### Why MapStruct?
Compile-time mapping with zero reflection overhead. Catches mapping errors at compile time (not runtime). Eliminates boilerplate `entity.toDto()` methods.

### Why separate DBs per service?
Each microservice owns its data — no shared schema coupling. Services can evolve their schema independently.
