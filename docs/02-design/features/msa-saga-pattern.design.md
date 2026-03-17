# [Design] MSA SAGA Pattern Implementation

## 1. System Architecture
- **Type**: Microservices Architecture (MSA)
- **Pattern**: Orchestration-based SAGA Pattern
- **Communication**: Asynchronous via Kafka
- **Orchestrator**: `order-service`

### Multi-module Structure
- `saga` (Root)
    - `common`: Common DTOs, Event Models
    - `order-service`: Order CRUD, Saga Orchestrator
    - `payment-service`: Payment processing
    - `inventory-service`: Inventory management

## 2. Data Schema (MariaDB)

### Order Service (DB: `order_db`)
- `orders` table
    - `id` (BIGINT, PK, AUTO_INCREMENT)
    - `product_id` (BIGINT)
    - `user_id` (BIGINT)
    - `price` (DECIMAL)
    - `status` (VARCHAR) - `PENDING`, `COMPLETED`, `CANCELLED`
    - `created_at` (DATETIME)

### Payment Service (DB: `payment_db`)
- `payments` table
    - `id` (BIGINT, PK, AUTO_INCREMENT)
    - `order_id` (BIGINT)
    - `amount` (DECIMAL)
    - `status` (VARCHAR) - `SUCCESS`, `FAILED`, `CANCELLED`

### Inventory Service (DB: `inventory_db`)
- `inventory` table
    - `product_id` (BIGINT, PK)
    - `stock_count` (INT)
- `inventory_history` table
    - `id` (BIGINT, PK, AUTO_INCREMENT)
    - `product_id` (BIGINT)
    - `order_id` (BIGINT)
    - `quantity` (INT)
    - `status` (VARCHAR) - `RESERVED`, `RELEASED`

## 3. API & Message Design (Kafka)

### Topics
- `order-events`: Emitted by `order-service`
- `payment-events`: Emitted by `payment-service`
- `inventory-events`: Emitted by `inventory-service`

### Event Models (Common Module)
- `OrderEvent`: `orderId`, `productId`, `userId`, `price`, `status`
- `PaymentEvent`: `orderId`, `paymentId`, `amount`, `status`
- `InventoryEvent`: `orderId`, `productId`, `quantity`, `status`

## 4. SAGA Workflow

### Normal Flow (Success)
1. `order-service` creates `PENDING` order -> Publishes `OrderCreatedEvent`.
2. `payment-service` consumes `OrderCreatedEvent` -> Processes payment -> Publishes `PaymentProcessedEvent` (SUCCESS).
3. `inventory-service` consumes `PaymentProcessedEvent` -> Deducts stock -> Publishes `InventoryReservedEvent`.
4. `order-service` consumes `InventoryReservedEvent` -> Updates order status to `COMPLETED`.

### Compensation Flow (Payment Fail)
1. `order-service` creates `PENDING` order -> Publishes `OrderCreatedEvent`.
2. `payment-service` consumes `OrderCreatedEvent` -> Payment fails -> Publishes `PaymentProcessedEvent` (FAILED).
3. `order-service` consumes `PaymentProcessedEvent` (FAILED) -> Updates order status to `CANCELLED`.

### Compensation Flow (Inventory Fail)
1. `order-service` creates `PENDING` order -> Publishes `OrderCreatedEvent`.
2. `payment-service` consumes `OrderCreatedEvent` -> Processes payment -> Publishes `PaymentProcessedEvent` (SUCCESS).
3. `inventory-service` consumes `PaymentProcessedEvent` -> Stock shortage -> Publishes `InventoryReservedEvent` (OUT_OF_STOCK).
4. `order-service` consumes `InventoryReservedEvent` (OUT_OF_STOCK) -> Updates order status to `CANCELLED` -> Publishes `OrderCancelledEvent`.
5. `payment-service` consumes `OrderCancelledEvent` -> Performs refund (Compensating Transaction) -> Updates payment to `CANCELLED`.

## 5. Infrastructure (Docker Compose)
- **Kafka**: Single node with Zookeeper.
- **MariaDB**: Three separate containers/databases for each service.
- **Network**: `saga-network`.

## 6. Implementation Strategy
- **Framework**: Spring Boot 3.3.x
- **Messaging**: Spring Cloud Stream Kafka Binder
- **Data Access**: Spring Data JPA
- **Validation**: JUnit 5 for unit/integration tests.

---
*Created by Gemini CLI (PDCA Skill)*
