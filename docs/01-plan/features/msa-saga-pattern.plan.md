# [Plan] MSA SAGA Pattern Implementation

## 1. Feature Description
구현하고자 하는 기능은 마이크로서비스 아키텍처(MSA)에서 데이터 일관성을 보장하기 위한 **SAGA 패턴(Orchestration-based)**입니다. 
여러 마이크로서비스(주문, 결제, 재고) 간의 트랜잭션을 관리하며, 실패 시 보상 트랜잭션(Compensating Transaction)을 통해 데이터의 최종 일관성을 유지합니다.

## 2. Goals & Objectives
- **멀티모듈 구성**: 공통 모듈과 각 서비스 모듈을 분리하여 관리.
- **SAGA 패턴 구현**: 주문(Order) 서비스를 오케스트레이터로 사용하여 전체 트랜잭션 흐름 제어.
- **메시징 기반 통신**: Kafka를 활용하여 서비스 간 비동기 이벤트 기반 통신 구현.
- **데이터 저장**: MariaDB를 각 서비스별로 독립된 DB로 사용.
- **환경 구성**: Docker Compose를 활용하여 MariaDB, Kafka 등 인프라 환경 구축.

## 3. Scope & Requirements
### 마이크로서비스 모듈
- `common`: 이벤트 DTO, 공통 유틸리티
- `order-service`: 주문 생성, SAGA 오케스트레이션, 주문 상태 관리
- `payment-service`: 결제 처리, 결제 실패 시 주문 취소 이벤트 발행
- `inventory-service`: 재고 차감, 재고 부족 시 결제 취소/주문 취소 유도

### 주요 요구사항
- **비동기 통신**: Kafka Topic (`order-events`, `payment-events`, `inventory-events`) 사용.
- **보상 트랜잭션**: 결제 실패 또는 재고 부족 시 이전 단계를 취소하는 로직 구현.
- **로깅 및 모니터링**: 트랜잭션 흐름을 파악할 수 있는 로그 기록.

## 4. Architecture & Key Technologies
- **Framework**: Spring Boot 3.x, Spring Cloud Stream (Kafka)
- **Language**: Java 17+
- **Database**: MariaDB 11.x (Docker)
- **Message Broker**: Apache Kafka (Docker)
- **Build Tool**: Gradle (Multi-module)
- **Container**: Docker Compose

## 5. Milestones & Tasks
1. [ ] **Step 1: 인프라 환경 구축**
   - [ ] Docker Compose 파일 작성 (MariaDB, Kafka, Zookeeper)
   - [ ] MariaDB 초기 스키마 정의
2. [ ] **Step 2: 멀티모듈 프로젝트 구조 재구성**
   - [ ] `settings.gradle` 수정 (모듈 등록)
   - [ ] `common`, `order-service`, `payment-service`, `inventory-service` 모듈 생성
3. [ ] **Step 3: Kafka 기반 이벤트 통신 구현**
   - [ ] Kafka Producer/Consumer 설정 (Spring Cloud Stream)
   - [ ] 공통 이벤트 모델 정의 (`OrderCreatedEvent`, `PaymentProcessedEvent`, 등)
4. [ ] **Step 4: SAGA 패턴 로직 개발**
   - [ ] Order 서비스의 Orchestrator 로직 구현
   - [ ] Payment 서비스의 결제 및 보상 로직 구현
   - [ ] Inventory 서비스의 재고 관리 및 보상 로직 구현
5. [ ] **Step 5: 통합 테스트 및 검증**
   - [ ] 정상 시나리오 테스트 (주문 완료)
   - [ ] 실패 시나리오 테스트 (결제 실패, 재고 부족) 및 보상 트랜잭션 확인

## 6. Verification & Validation
- **정상 시나리오**: 주문(PENDING) -> 결제(SUCCESS) -> 재고(RESERVED) -> 주문(COMPLETED)
- **보상 시나리오 1 (결제 실패)**: 주문(PENDING) -> 결제(FAIL) -> 주문(CANCELLED)
- **보상 시나리오 2 (재고 부족)**: 주문(PENDING) -> 결제(SUCCESS) -> 재고(OUT_OF_STOCK) -> 결제(CANCELLED) -> 주문(CANCELLED)

---
*Created by Gemini CLI (PDCA Skill)*
