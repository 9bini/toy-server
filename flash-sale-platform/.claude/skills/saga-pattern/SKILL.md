---
name: saga-pattern
description: Implements the Saga pattern for distributed transactions. Uses a coroutine state machine with the Orchestration approach.
argument-hint: [saga-name] [steps-description]
---

$ARGUMENTS Implement the Saga.

## Implementation Steps

### 1. Define Saga Steps
Define the execution action and compensation action for each step.

```kotlin
sealed class OrderSagaStep {
    data class VerifyStock(val productId: String, val quantity: Int) : OrderSagaStep()
    data class ReserveStock(val productId: String, val quantity: Int) : OrderSagaStep()
    data class ProcessPayment(val orderId: String, val amount: Long) : OrderSagaStep()
    data class ConfirmOrder(val orderId: String) : OrderSagaStep()
}
```

### 2. Define Saga State
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

### 3. Implement Orchestrator
- Coroutine-based state machine
- Execute each step sequentially
- Execute compensations in reverse order on failure
- Publish Kafka event on each state change

### 4. Compensating Transactions
```
Normal flow:    Verify Stock → Decrement Stock → Request Payment → Confirm Order
On failure:     (Payment failed!) → Restore Stock → Cancel Order
                ← ← ← Compensating transactions ← ← ←
```

### 5. DLQ Handling
- Isolate to DLQ when compensation also fails
- Connect to manual processing dashboard or alerting

### 6. Idempotency
- Store idempotency key (Redis) for each step
- Prevent duplicates when re-executed with the same key

## Code Location
- `{service}/src/main/kotlin/.../application/saga/`
  - `{SagaName}Definition.kt`: Saga step and state definitions
  - `{SagaName}Orchestrator.kt`: Execution engine
  - `{SagaName}StepHandler.kt`: Handler for each step

## Required Tests
- Integration test for the normal flow
- Compensation flow test for each step failure
- Idempotency test (execute the same request twice)
