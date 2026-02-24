---
name: saga-pattern
description: 분산 트랜잭션을 위한 Saga 패턴을 구현합니다. Orchestration 방식으로 코루틴 상태머신을 사용합니다.
argument-hint: [saga-name] [steps-description]
---

$ARGUMENTS Saga를 구현하세요.

## 구현 단계

### 1. Saga 단계 정의
각 단계(Step)의 실행 액션과 보상 액션을 정의합니다.

```kotlin
sealed class OrderSagaStep {
    data class VerifyStock(val productId: String, val quantity: Int) : OrderSagaStep()
    data class ReserveStock(val productId: String, val quantity: Int) : OrderSagaStep()
    data class ProcessPayment(val orderId: String, val amount: Long) : OrderSagaStep()
    data class ConfirmOrder(val orderId: String) : OrderSagaStep()
}
```

### 2. Saga 상태 정의
```kotlin
sealed class SagaState {
    object Started : SagaState()
    object StockVerified : SagaState()
    object StockReserved : SagaState()
    object PaymentProcessed : SagaState()
    object Completed : SagaState()
    data class Failed(val step: String, val reason: String) : SagaState()
    data class Compensating(val fromStep: String) : SagaState()
    object Compensated : SagaState()
}
```

### 3. Orchestrator 구현
- 코루틴 기반 상태 머신
- 각 단계를 순차 실행
- 실패 시 역순 보상 실행
- 상태 변경마다 Kafka 이벤트 발행

### 4. 보상 트랜잭션
```
정상 흐름:  재고확인 → 재고차감 → 결제요청 → 주문확정
실패 시:    (결제실패!) → 재고복구 → 주문취소
            ← ← ← 보상 트랜잭션 ← ← ←
```

### 5. DLQ 처리
- 보상도 실패한 경우 DLQ로 격리
- 수동 처리 대시보드 또는 알림 연동

### 6. 멱등성
- 각 단계에 멱등성 키(Redis) 저장
- 동일 키로 재실행 시 중복 방지

## 코드 위치
- `{service}/src/main/kotlin/.../application/saga/`
  - `{SagaName}Definition.kt`: Saga 단계 및 상태 정의
  - `{SagaName}Orchestrator.kt`: 실행 엔진
  - `{SagaName}StepHandler.kt`: 각 단계 핸들러

## 필수 테스트
- 정상 흐름 통합 테스트
- 각 단계 실패 시 보상 흐름 테스트
- 멱등성 테스트 (동일 요청 2회 실행)
