# MSA SAGA Pattern Test Scenarios (Postman)

이 문서는 구현된 SAGA 패턴의 정상 흐름과 에러 흐름(보상 트랜잭션)을 Postman을 통해 테스트하는 방법을 정리합니다.

## 1. 테스트 준비

### 인프라 실행
```bash
docker-compose up -d
```

### 마이크로서비스 실행 (Local)
- **Order Service**: `http://localhost:8081`
- **Payment Service**: `http://localhost:8082`
- **Inventory Service**: `http://localhost:8083`

---

## 2. 테스트 시나리오

### 시나리오 A: 정상 주문 완료 (Success Flow)
결제 금액이 1000 이하이고 재고가 충분한 경우입니다.

- **Method**: `POST`
- **URL**: `http://localhost:8081/orders`
- **Headers**: `Content-Type: application/json`
- **Body**:
  ```json
  {
    "productId": 1,
    "userId": 101,
    "price": 500.00
  }
  ```
- **검증**:
  1. `order_db.orders` 테이블의 해당 주문 `status`가 `COMPLETED`로 변경됨.
  2. `payment_db.payments` 테이블의 결제 `status`가 `SUCCESS`임.
  3. `inventory_db.inventory` 테이블의 `stock_count`가 1 감소함.

### 시나리오 B: 결제 실패 및 주문 취소 (Payment Compensation Flow)
결제 금액이 1000을 초과하여 결제가 실패하는 경우입니다.

- **Method**: `POST`
- **URL**: `http://localhost:8081/orders`
- **Body**:
  ```json
  {
    "productId": 1,
    "userId": 101,
    "price": 1500.00
  }
  ```
- **검증**:
  1. `payment-service`에서 결제 실패(`FAILED`) 이벤트 발행.
  2. `order_db.orders` 테이블의 해당 주문 `status`가 `CANCELLED`로 변경됨.
  3. `inventory-service`는 호출되지 않거나 재고 변화가 없음.

### 시나리오 C: 재고 부족 및 결제 취소 (Inventory Compensation Flow)
재고가 부족하여 결제된 금액을 환불하고 주문을 취소하는 경우입니다.

- **방법**: 시나리오 A를 여러 번 반복하여 재고를 0으로 만들거나, `init.sql`에서 재고를 0으로 설정 후 테스트.
- **Method**: `POST`
- **URL**: `http://localhost:8081/orders`
- **Body**:
  ```json
  {
    "productId": 1,
    "userId": 101,
    "price": 300.00
  }
  ```
- **검증**:
  1. `payment-service`에서 결제는 `SUCCESS`로 처리됨.
  2. `inventory-service`에서 `OUT_OF_STOCK` 이벤트 발행.
  3. `order-service`에서 주문 상태를 `CANCELLED`로 변경하고 `OrderCancelledEvent` 발행.
  4. `payment-service`가 `OrderCancelledEvent`를 소비하여 결제 상태를 `CANCELLED`로 변경(환불).

---

## 3. DB 검증 쿼리

```sql
-- 주문 상태 확인
SELECT * FROM order_db.orders ORDER BY id DESC;

-- 결제 상태 확인
SELECT * FROM payment_db.payments ORDER BY id DESC;

-- 재고 및 이력 확인
SELECT * FROM inventory_db.inventory WHERE product_id = 1;
SELECT * FROM inventory_db.inventory_history ORDER BY id DESC;
```

---
*Created by Gemini CLI (Testing Guide)*
