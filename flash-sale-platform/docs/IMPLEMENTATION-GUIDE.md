# Flash Sale Platform 구현 가이드

> **목적**: 카카오/네이버/쿠팡/토스/배민 지원용 포트폴리오 완성
> **대상**: 주니어~미드 레벨 (2~5년) 백엔드 엔지니어
> **핵심**: 면접에서 "대규모 트래픽", "동시성 제어", "분산 트랜잭션", "실시간 처리"를 경험 기반으로 답변 가능한 프로젝트

---

## 1. 구현 로드맵

### 1-1. 구현 순서 (의존성 기반)

```
Step 1: order-service          ← 가장 핵심. 재고 동시성, Lua Script, Kafka Producer
     ↓ (order.placed 이벤트 발행)
Step 2: payment-service        ← Saga 패턴, 보상 트랜잭션
     ↓ (payment.completed/failed 이벤트 발행)
Step 3: notification-service   ← SSE 실시간 알림, Kafka Consumer
     ↓
Step 4: gateway                ← Token Bucket Rate Limiting
     ↓
Step 5: 부하 테스트 + 성능 최적화 ← 차별화 포인트
```

### 1-2. 커밋 전략

```
# 서비스별로 레이어 단위 커밋 (면접관이 PR을 읽었을 때 설계 과정을 이해할 수 있도록)
feat(order): define domain model (Order, OrderStatus, OrderError)
feat(order): add port interfaces (StockPort, OrderPersistencePort, OrderEventPort)
feat(order): implement PlaceOrderService with Lua Script stock decrement
feat(order): add Redis stock adapter with Lua Script
feat(order): add R2DBC order persistence adapter
feat(order): add Kafka event publisher adapter
feat(order): add OrderController and DTOs
feat(order): add unit tests for PlaceOrderService
feat(order): add integration tests for Redis stock adapter
feat(order): add compensation flow (CancelOrderService, PaymentEventListener)
```

---

## 2. 전체 이벤트 흐름 (Saga Choreography)

### 2-1. 정상 흐름

```
Client → [gateway] → [queue-service] → 대기열 진입 (Redis Sorted Set)
                           ↓ 순번 도달
Client → [gateway] → [order-service] → POST /api/orders
                           │
                           ├─ 1. 멱등성 체크 (Redis)
                           ├─ 2. 재고 차감 (Redis Lua Script, atomic)
                           ├─ 3. 주문 저장 (PostgreSQL R2DBC)
                           └─ 4. 이벤트 발행 → Kafka [flashsale.order.placed]
                                                      ↓
                      [payment-service] ← Kafka Consumer
                           │
                           ├─ 1. 멱등성 체크 (Redis)
                           ├─ 2. PG API 호출 (withTimeout 3s)
                           ├─ 3. 결제 저장 (PostgreSQL)
                           └─ 4. 이벤트 발행 → Kafka [flashsale.payment.completed]
                                                      ↓
                      [notification-service] ← Kafka Consumer
                           └─ SSE로 클라이언트에 실시간 알림
```

### 2-2. 보상 트랜잭션 (결제 실패 시)

```
[payment-service] → Kafka [flashsale.payment.failed]
                          ↓
[order-service] ← PaymentEventListener
    │
    ├─ 1. 주문 상태 → CANCELLED
    ├─ 2. 재고 복구 (Redis INCRBY)
    └─ 3. 이벤트 발행 → Kafka [flashsale.order.cancelled]
                               ↓
[notification-service] → SSE "주문 취소" 알림
```

### 2-3. Kafka 토픽 Producer/Consumer 매핑

| 토픽 | Producer | Consumer |
|------|----------|----------|
| `flashsale.order.placed` | order-service | payment-service |
| `flashsale.order.cancelled` | order-service | notification-service |
| `flashsale.order.completed` | order-service | notification-service |
| `flashsale.payment.completed` | payment-service | order-service, notification-service |
| `flashsale.payment.failed` | payment-service | order-service, notification-service |
| `flashsale.stock.decremented` | order-service | (로깅/모니터링) |
| `flashsale.stock.restored` | order-service | (로깅/모니터링) |

> 참고: 이미 `KafkaTopics.kt`에 모든 토픽 상수가 정의되어 있다.

---

## 3. Step 1: order-service (포트 8082)

### 면접 포인트
- **"동시에 1000명이 주문하면 재고는 어떻게 처리했나요?"**
  → Redis Lua Script로 CHECK + DECREMENT를 atomic하게 처리. DB 락 없이 μs 단위 응답
- **"주문이 중복 생성되면 어떻게 하나요?"**
  → 클라이언트가 전달한 idempotencyKey를 Redis에 저장 (TTL 24시간)
- **"재고 차감 후 DB 저장이 실패하면?"**
  → 보상 로직으로 재고 복구. 이벤트 미발행으로 payment도 진행 안 됨

### 3-1. Domain Layer

**파일**: `services/order-service/src/main/kotlin/com/flashsale/order/domain/`

```kotlin
// Order.kt
package com.flashsale.order.domain

import java.time.Instant

data class Order(
    val id: String,
    val userId: String,
    val saleEventId: String,
    val productId: String,
    val quantity: Int,
    val status: OrderStatus,
    val idempotencyKey: String,
    val createdAt: Instant,
)

enum class OrderStatus {
    PENDING,     // 주문 생성, 결제 대기
    COMPLETED,   // 결제 완료
    CANCELLED,   // 결제 실패 또는 취소
}
```

```kotlin
// OrderError.kt
package com.flashsale.order.domain

sealed interface OrderError {
    /** 재고 부족 */
    data class InsufficientStock(val productId: String, val requested: Int) : OrderError

    /** 멱등성 키 중복 (이미 처리된 주문) */
    data class DuplicateOrder(val idempotencyKey: String) : OrderError

    /** 주문을 찾을 수 없음 */
    data class OrderNotFound(val orderId: String) : OrderError
}
```

> **패턴 참조**: `queue-service`의 `QueueError.kt` — sealed interface로 에러를 타입 안전하게 정의

### 3-2. Port Out (출력 포트)

**파일**: `services/order-service/src/main/kotlin/com/flashsale/order/application/port/out/`

```kotlin
// StockPort.kt — Redis Lua Script로 구현
interface StockPort {
    /** 재고 atomic 차감. 성공하면 남은 수량, 실패하면 -1 */
    suspend fun decrease(productId: String, quantity: Int): Long

    /** 보상 트랜잭션용 재고 복구 */
    suspend fun restore(productId: String, quantity: Int)
}
```

```kotlin
// OrderPersistencePort.kt — R2DBC로 구현
interface OrderPersistencePort {
    suspend fun save(order: Order): Order
    suspend fun findById(orderId: String): Order?
    suspend fun updateStatus(orderId: String, status: OrderStatus)
}
```

```kotlin
// OrderEventPort.kt — Kafka Producer로 구현
interface OrderEventPort {
    suspend fun publishOrderPlaced(event: OrderPlacedEvent)
    suspend fun publishOrderCancelled(event: OrderCancelledEvent)
}
```

```kotlin
// IdempotencyPort.kt — Redis로 구현
interface IdempotencyPort {
    /** 멱등성 키 존재 여부 확인 + 설정 (atomic). 이미 존재하면 false */
    suspend fun tryAcquire(key: String): Boolean
}
```

### 3-3. Port In (입력 포트 = UseCase)

**파일**: `services/order-service/src/main/kotlin/com/flashsale/order/application/port/in/`

```kotlin
// PlaceOrderUseCase.kt
interface PlaceOrderUseCase {
    suspend fun execute(command: PlaceOrderCommand): Result<PlaceOrderResult, OrderError>
}

data class PlaceOrderCommand(
    val userId: String,
    val saleEventId: String,
    val productId: String,
    val quantity: Int,
    val idempotencyKey: String,
)

data class PlaceOrderResult(val orderId: String, val status: OrderStatus)
```

```kotlin
// CancelOrderUseCase.kt — 보상 트랜잭션용
interface CancelOrderUseCase {
    suspend fun execute(command: CancelOrderCommand): Result<Unit, OrderError>
}

data class CancelOrderCommand(val orderId: String, val reason: String)
```

### 3-4. UseCase Implementation

**파일**: `services/order-service/src/main/kotlin/com/flashsale/order/application/service/`

```kotlin
// PlaceOrderService.kt — 핵심 비즈니스 로직
@Service
class PlaceOrderService(
    private val stockPort: StockPort,
    private val orderPersistencePort: OrderPersistencePort,
    private val orderEventPort: OrderEventPort,
    private val idempotencyPort: IdempotencyPort,
) : PlaceOrderUseCase {
    companion object : Log

    override suspend fun execute(command: PlaceOrderCommand): Result<PlaceOrderResult, OrderError> {
        // 1. 멱등성 체크
        if (!idempotencyPort.tryAcquire(command.idempotencyKey)) {
            return Result.failure(OrderError.DuplicateOrder(command.idempotencyKey))
        }

        // 2. 재고 차감 (Redis Lua Script — atomic)
        val remaining = stockPort.decrease(command.productId, command.quantity)
        if (remaining < 0) {
            return Result.failure(OrderError.InsufficientStock(command.productId, command.quantity))
        }

        // 3. 주문 저장 (PostgreSQL)
        val order = try {
            orderPersistencePort.save(
                Order(
                    id = IdGenerator.generate(),
                    userId = command.userId,
                    saleEventId = command.saleEventId,
                    productId = command.productId,
                    quantity = command.quantity,
                    status = OrderStatus.PENDING,
                    idempotencyKey = command.idempotencyKey,
                    createdAt = Instant.now(),
                )
            )
        } catch (e: Exception) {
            // DB 저장 실패 시 재고 복구 (보상)
            logger.error(e) { "주문 저장 실패, 재고 복구: productId=${command.productId}" }
            stockPort.restore(command.productId, command.quantity)
            throw e
        }

        // 4. 이벤트 발행 (Kafka)
        orderEventPort.publishOrderPlaced(
            OrderPlacedEvent(
                aggregateId = order.id,
                productId = order.productId,
                quantity = order.quantity,
                userId = order.userId,
                saleEventId = order.saleEventId,
            )
        )

        logger.info { "주문 생성 완료: orderId=${order.id}, remaining=$remaining" }
        return Result.success(PlaceOrderResult(order.id, order.status))
    }
}
```

```kotlin
// CancelOrderService.kt — 보상 트랜잭션
@Service
class CancelOrderService(
    private val orderPersistencePort: OrderPersistencePort,
    private val stockPort: StockPort,
    private val orderEventPort: OrderEventPort,
) : CancelOrderUseCase {
    companion object : Log

    override suspend fun execute(command: CancelOrderCommand): Result<Unit, OrderError> {
        val order = orderPersistencePort.findById(command.orderId)
            ?: return Result.failure(OrderError.OrderNotFound(command.orderId))

        // 이미 취소된 주문은 멱등하게 처리
        if (order.status == OrderStatus.CANCELLED) {
            return Result.success(Unit)
        }

        orderPersistencePort.updateStatus(command.orderId, OrderStatus.CANCELLED)
        stockPort.restore(order.productId, order.quantity)
        orderEventPort.publishOrderCancelled(
            OrderCancelledEvent(aggregateId = order.id, reason = command.reason)
        )

        logger.info { "주문 취소 완료: orderId=${order.id}, reason=${command.reason}" }
        return Result.success(Unit)
    }
}
```

### 3-5. Adapter Out — Redis Stock (핵심)

**파일**: `services/order-service/src/main/kotlin/com/flashsale/order/adapter/out/redis/`

```kotlin
// RedisStockAdapter.kt
@Component
class RedisStockAdapter(
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val timeouts: TimeoutProperties,
) : StockPort {
    companion object : Log

    // Lua Script: atomic 재고 확인 + 차감
    // 성공 → 남은 수량 반환, 실패 → -1 반환
    private val decreaseScript = RedisScript.of<Long>(
        """
        local remain = tonumber(redis.call('GET', KEYS[1]))
        if remain == nil then return -1 end
        if remain >= tonumber(ARGV[1]) then
            return redis.call('DECRBY', KEYS[1], ARGV[1])
        end
        return -1
        """.trimIndent(),
        Long::class.java,
    )

    override suspend fun decrease(productId: String, quantity: Int): Long =
        withTimeout(timeouts.redisLuaScript) {
            val key = RedisKeys.Stock.remaining(productId)
            redisTemplate.execute(
                decreaseScript,
                listOf(key),
                listOf(quantity.toString()),
            ).awaitSingle()
        }

    override suspend fun restore(productId: String, quantity: Int) {
        withTimeout(timeouts.redisOperation) {
            val key = RedisKeys.Stock.remaining(productId)
            redisTemplate.opsForValue()
                .increment(key, quantity.toLong())
                .awaitSingle()
        }
    }
}
```

> **왜 Lua Script인가?**
> - `GET` → 비교 → `DECRBY`를 3개 명령으로 보내면 사이에 다른 요청이 끼어들 수 있다
> - Lua Script는 Redis에서 **single-threaded로 atomic 실행**된다
> - 참조: `docs/guides/add-redis-operation.md`

```kotlin
// RedisIdempotencyAdapter.kt
@Component
class RedisIdempotencyAdapter(
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val timeouts: TimeoutProperties,
) : IdempotencyPort {
    companion object : Log

    override suspend fun tryAcquire(key: String): Boolean =
        withTimeout(timeouts.redisOperation) {
            val redisKey = RedisKeys.Order.idempotencyKey(key)
            // SET NX: 키가 없을 때만 설정 (atomic)
            redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", Duration.ofHours(24))
                .awaitSingle()
        }
}
```

### 3-6. Adapter Out — PostgreSQL (R2DBC)

**파일**: `services/order-service/src/main/kotlin/com/flashsale/order/adapter/out/persistence/`

```kotlin
// R2dbcOrderAdapter.kt
@Component
class R2dbcOrderAdapter(
    private val client: DatabaseClient,
    private val timeouts: TimeoutProperties,
) : OrderPersistencePort {

    override suspend fun save(order: Order): Order =
        withTimeout(timeouts.dbQuery) {
            client.sql(
                """
                INSERT INTO orders (id, user_id, sale_event_id, product_id, quantity, status, idempotency_key, created_at)
                VALUES (:id, :userId, :saleEventId, :productId, :quantity, :status, :idempotencyKey, :createdAt)
                """.trimIndent()
            )
                .bind("id", order.id)
                .bind("userId", order.userId)
                .bind("saleEventId", order.saleEventId)
                .bind("productId", order.productId)
                .bind("quantity", order.quantity)
                .bind("status", order.status.name)
                .bind("idempotencyKey", order.idempotencyKey)
                .bind("createdAt", order.createdAt)
                .fetch()
                .rowsUpdated()
                .awaitSingle()
            order
        }

    override suspend fun findById(orderId: String): Order? =
        withTimeout(timeouts.dbQuery) {
            client.sql("SELECT * FROM orders WHERE id = :id")
                .bind("id", orderId)
                .map { row, _ ->
                    Order(
                        id = row.get("id", String::class.java)!!,
                        userId = row.get("user_id", String::class.java)!!,
                        saleEventId = row.get("sale_event_id", String::class.java)!!,
                        productId = row.get("product_id", String::class.java)!!,
                        quantity = row.get("quantity", Int::class.java)!!,
                        status = OrderStatus.valueOf(row.get("status", String::class.java)!!),
                        idempotencyKey = row.get("idempotency_key", String::class.java)!!,
                        createdAt = row.get("created_at", Instant::class.java)!!,
                    )
                }
                .awaitOneOrNull()
        }

    override suspend fun updateStatus(orderId: String, status: OrderStatus) {
        withTimeout(timeouts.dbQuery) {
            client.sql("UPDATE orders SET status = :status WHERE id = :id")
                .bind("status", status.name)
                .bind("id", orderId)
                .fetch()
                .rowsUpdated()
                .awaitSingle()
        }
    }
}
```

### 3-7. Adapter Out — Kafka Producer

**파일**: `services/order-service/src/main/kotlin/com/flashsale/order/adapter/out/kafka/`

```kotlin
// KafkaOrderEventAdapter.kt
@Component
class KafkaOrderEventAdapter(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val timeouts: TimeoutProperties,
) : OrderEventPort {
    companion object : Log

    override suspend fun publishOrderPlaced(event: OrderPlacedEvent) {
        withTimeout(timeouts.kafkaProduce) {
            kafkaTemplate.send(
                KafkaTopics.Order.PLACED,
                event.aggregateId,  // 파티션 키 = orderId
                event,
            ).asDeferred().await()
        }
        logger.info { "이벤트 발행: ${event.eventType}, orderId=${event.aggregateId}" }
    }

    override suspend fun publishOrderCancelled(event: OrderCancelledEvent) {
        withTimeout(timeouts.kafkaProduce) {
            kafkaTemplate.send(
                KafkaTopics.Order.CANCELLED,
                event.aggregateId,
                event,
            ).asDeferred().await()
        }
        logger.info { "이벤트 발행: ${event.eventType}, orderId=${event.aggregateId}" }
    }
}
```

### 3-8. Adapter In — Controller

**파일**: `services/order-service/src/main/kotlin/com/flashsale/order/adapter/in/web/`

```kotlin
// OrderController.kt
@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val placeOrderUseCase: PlaceOrderUseCase,
) {
    @PostMapping
    suspend fun placeOrder(@RequestBody request: PlaceOrderRequest): ResponseEntity<Any> {
        val command = PlaceOrderCommand(
            userId = request.userId,
            saleEventId = request.saleEventId,
            productId = request.productId,
            quantity = request.quantity,
            idempotencyKey = request.idempotencyKey,
        )
        return placeOrderUseCase.execute(command).fold(
            onSuccess = { result ->
                ResponseEntity.status(HttpStatus.CREATED).body(
                    PlaceOrderResponse(orderId = result.orderId, status = result.status.name)
                )
            },
            onFailure = { error -> toErrorResponse(error) },
        )
    }

    private fun toErrorResponse(error: OrderError): ResponseEntity<Any> =
        when (error) {
            is OrderError.InsufficientStock -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse(code = "INSUFFICIENT_STOCK", message = "재고가 부족합니다")
            )
            is OrderError.DuplicateOrder -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse(code = "DUPLICATE_ORDER", message = "이미 처리된 주문입니다")
            )
            is OrderError.OrderNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse(code = "ORDER_NOT_FOUND", message = "주문을 찾을 수 없습니다")
            )
        }
}
```

### 3-9. Adapter In — Kafka Consumer (보상 트랜잭션)

**파일**: `services/order-service/src/main/kotlin/com/flashsale/order/adapter/in/kafka/`

```kotlin
// PaymentEventListener.kt
@Component
class PaymentEventListener(
    private val cancelOrderUseCase: CancelOrderUseCase,
) {
    companion object : Log

    @KafkaListener(topics = [KafkaTopics.Payment.FAILED], groupId = "order-service")
    suspend fun onPaymentFailed(event: PaymentFailedEvent) {
        logger.info { "결제 실패 이벤트 수신: orderId=${event.orderId}" }
        cancelOrderUseCase.execute(
            CancelOrderCommand(orderId = event.orderId, reason = "결제 실패: ${event.reason}")
        )
    }
}
```

### 3-10. Domain Event

**파일**: `services/order-service/src/main/kotlin/com/flashsale/order/domain/`

```kotlin
// OrderEvents.kt
data class OrderPlacedEvent(
    override val aggregateId: String,
    override val eventType: String = "order.placed",
    override val occurredAt: Instant = Instant.now(),
    override val eventId: String = IdGenerator.generate(),
    val productId: String,
    val quantity: Int,
    val userId: String,
    val saleEventId: String,
) : DomainEvent

data class OrderCancelledEvent(
    override val aggregateId: String,
    override val eventType: String = "order.cancelled",
    override val occurredAt: Instant = Instant.now(),
    override val eventId: String = IdGenerator.generate(),
    val reason: String,
) : DomainEvent
```

### 3-11. DB Migration

**파일**: `services/order-service/src/main/resources/db/migration/V1__create_orders_table.sql`

```sql
CREATE TABLE orders (
    id              VARCHAR(36)  PRIMARY KEY,
    user_id         VARCHAR(36)  NOT NULL,
    sale_event_id   VARCHAR(36)  NOT NULL,
    product_id      VARCHAR(36)  NOT NULL,
    quantity        INT          NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(64)  NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_sale_event_id ON orders (sale_event_id);
CREATE INDEX idx_orders_idempotency_key ON orders (idempotency_key);
```

### 3-12. 테스트 전략

| 테스트 | 검증 대상 | 도구 |
|--------|-----------|------|
| **PlaceOrderServiceTest** | 멱등성 체크 → 재고 차감 → DB 저장 → 이벤트 발행 흐름 | MockK |
| **CancelOrderServiceTest** | 주문 취소 → 재고 복구 → 이벤트 발행, 이미 취소된 주문 멱등 처리 | MockK |
| **RedisStockAdapterTest** | Lua Script가 동시 요청에서 정확히 동작하는지 | Testcontainers Redis |
| **OrderControllerTest** | HTTP 요청/응답 매핑, 에러 코드 정확성 | WebTestClient |

**단위 테스트 예시** (queue-service 패턴 참조):
```kotlin
class PlaceOrderServiceTest : DescribeSpec({
    val stockPort = mockk<StockPort>()
    val orderPersistencePort = mockk<OrderPersistencePort>()
    val orderEventPort = mockk<OrderEventPort>()
    val idempotencyPort = mockk<IdempotencyPort>()
    val sut = PlaceOrderService(stockPort, orderPersistencePort, orderEventPort, idempotencyPort)

    describe("execute") {
        val command = PlaceOrderCommand("user-1", "sale-1", "product-1", 1, "idem-key-1")

        context("정상 주문일 때") {
            it("주문을 생성하고 이벤트를 발행한다") {
                coEvery { idempotencyPort.tryAcquire(any()) } returns true
                coEvery { stockPort.decrease(any(), any()) } returns 99L
                coEvery { orderPersistencePort.save(any()) } returnsArgument 0
                coEvery { orderEventPort.publishOrderPlaced(any()) } just runs

                val result = sut.execute(command)

                result.shouldBeInstanceOf<Result.Success<*>>()
                coVerify { orderEventPort.publishOrderPlaced(any()) }
            }
        }

        context("재고가 부족할 때") {
            it("InsufficientStock 에러를 반환한다") {
                coEvery { idempotencyPort.tryAcquire(any()) } returns true
                coEvery { stockPort.decrease(any(), any()) } returns -1L

                val result = sut.execute(command)

                result.shouldBeInstanceOf<Result.Failure<*>>()
                (result as Result.Failure).error.shouldBeInstanceOf<OrderError.InsufficientStock>()
            }
        }

        context("중복 주문일 때") {
            it("DuplicateOrder 에러를 반환한다") {
                coEvery { idempotencyPort.tryAcquire(any()) } returns false

                val result = sut.execute(command)

                result.shouldBeInstanceOf<Result.Failure<*>>()
                (result as Result.Failure).error.shouldBeInstanceOf<OrderError.DuplicateOrder>()
            }
        }
    }
})
```

### 3-13. 재사용할 기존 코드

| 코드 | 파일 경로 |
|------|-----------|
| `RedisKeys.Stock.remaining()` | `common/infrastructure/.../redis/RedisKeys.kt:18` |
| `RedisKeys.Order.idempotencyKey()` | `common/infrastructure/.../redis/RedisKeys.kt:45` |
| `KafkaTopics.Order.PLACED` | `common/infrastructure/.../kafka/KafkaTopics.kt:15` |
| `KafkaTopics.Payment.FAILED` | `common/infrastructure/.../kafka/KafkaTopics.kt:31` |
| `TimeoutProperties.redisLuaScript` | `common/infrastructure/.../config/Timeouts.kt:56` |
| `Result<T, E>`, `fold()` | `common/domain/.../Result.kt` |
| `DomainEvent` | `common/domain/.../DomainEvent.kt` |
| `ErrorResponse` | `common/infrastructure/.../web/ErrorResponse.kt` |
| `IntegrationTestBase` | `common/infrastructure/src/testFixtures/.../IntegrationTestBase.kt` |

---

## 4. Step 2: payment-service (포트 8083)

### 면접 포인트
- **"분산 트랜잭션은 어떻게 처리했나요?"**
  → Kafka 기반 Choreography Saga. 각 서비스가 독립적으로 이벤트를 발행/소비
- **"결제 타임아웃이 발생하면?"**
  → `withTimeout(3s)` 후 payment.failed 이벤트 발행 → order-service가 보상 실행
- **"왜 Orchestration이 아니라 Choreography?"**
  → 서비스 간 결합도 최소화. 단점(흐름 추적 어려움)은 Kafka 토픽 네이밍과 로깅으로 보완

### 4-1. Domain Layer

```kotlin
// Payment.kt
data class Payment(
    val id: String,
    val orderId: String,
    val userId: String,
    val amount: Long,  // 원 단위
    val status: PaymentStatus,
    val createdAt: Instant,
)

enum class PaymentStatus {
    REQUESTED,
    COMPLETED,
    FAILED,
}
```

```kotlin
// PaymentError.kt
sealed interface PaymentError {
    /** 외부 PG API 호출 타임아웃 */
    data class PaymentTimeout(val orderId: String) : PaymentError

    /** PG에서 결제 거절 */
    data class PaymentRejected(val orderId: String, val reason: String) : PaymentError

    /** 이미 처리된 결제 (멱등성) */
    data class AlreadyProcessed(val orderId: String) : PaymentError
}
```

### 4-2. Port Out

```kotlin
// PaymentGatewayPort.kt — 외부 PG API 추상화
interface PaymentGatewayPort {
    suspend fun requestPayment(orderId: String, userId: String, amount: Long): PaymentGatewayResult
}

sealed interface PaymentGatewayResult {
    data class Approved(val transactionId: String) : PaymentGatewayResult
    data class Rejected(val reason: String) : PaymentGatewayResult
}
```

```kotlin
// PaymentPersistencePort.kt
interface PaymentPersistencePort {
    suspend fun save(payment: Payment): Payment
    suspend fun findByOrderId(orderId: String): Payment?
}
```

```kotlin
// PaymentEventPort.kt
interface PaymentEventPort {
    suspend fun publishPaymentCompleted(event: PaymentCompletedEvent)
    suspend fun publishPaymentFailed(event: PaymentFailedEvent)
}
```

### 4-3. UseCase Implementation

```kotlin
// ProcessPaymentService.kt
@Service
class ProcessPaymentService(
    private val paymentGatewayPort: PaymentGatewayPort,
    private val paymentPersistencePort: PaymentPersistencePort,
    private val paymentEventPort: PaymentEventPort,
    private val timeouts: TimeoutProperties,
) : ProcessPaymentUseCase {
    companion object : Log

    override suspend fun execute(command: ProcessPaymentCommand): Result<ProcessPaymentResult, PaymentError> {
        // 1. 멱등성 체크
        val existing = paymentPersistencePort.findByOrderId(command.orderId)
        if (existing != null) {
            return Result.failure(PaymentError.AlreadyProcessed(command.orderId))
        }

        // 2. 외부 PG API 호출 (withTimeout)
        val gatewayResult = try {
            withTimeout(timeouts.paymentApi) {
                paymentGatewayPort.requestPayment(command.orderId, command.userId, command.amount)
            }
        } catch (e: TimeoutCancellationException) {
            logger.error { "PG API 타임아웃: orderId=${command.orderId}" }
            // 타임아웃 → 결제 실패 이벤트 발행
            paymentEventPort.publishPaymentFailed(
                PaymentFailedEvent(aggregateId = command.orderId, reason = "PG_TIMEOUT")
            )
            return Result.failure(PaymentError.PaymentTimeout(command.orderId))
        }

        // 3. 결과에 따라 분기
        return when (gatewayResult) {
            is PaymentGatewayResult.Approved -> {
                val payment = paymentPersistencePort.save(
                    Payment(
                        id = IdGenerator.generate(),
                        orderId = command.orderId,
                        userId = command.userId,
                        amount = command.amount,
                        status = PaymentStatus.COMPLETED,
                        createdAt = Instant.now(),
                    )
                )
                paymentEventPort.publishPaymentCompleted(
                    PaymentCompletedEvent(aggregateId = command.orderId, paymentId = payment.id)
                )
                Result.success(ProcessPaymentResult(payment.id, PaymentStatus.COMPLETED))
            }
            is PaymentGatewayResult.Rejected -> {
                paymentEventPort.publishPaymentFailed(
                    PaymentFailedEvent(aggregateId = command.orderId, reason = gatewayResult.reason)
                )
                Result.failure(PaymentError.PaymentRejected(command.orderId, gatewayResult.reason))
            }
        }
    }
}
```

### 4-4. Adapter Out — Fake PG Gateway

```kotlin
// FakePaymentGatewayAdapter.kt — 테스트/시연용
@Component
class FakePaymentGatewayAdapter : PaymentGatewayPort {
    companion object : Log

    override suspend fun requestPayment(orderId: String, userId: String, amount: Long): PaymentGatewayResult {
        // 실제 PG 호출 시뮬레이션 (100~500ms 지연)
        delay(Random.nextLong(100, 500))

        // 90% 성공, 10% 실패 (데모용)
        return if (Random.nextInt(100) < 90) {
            PaymentGatewayResult.Approved(transactionId = "txn-${IdGenerator.generate()}")
        } else {
            PaymentGatewayResult.Rejected(reason = "INSUFFICIENT_BALANCE")
        }
    }
}
```

### 4-5. Adapter In — Kafka Consumer

```kotlin
// OrderEventListener.kt — order.placed 이벤트 수신
@Component
class OrderEventListener(
    private val processPaymentUseCase: ProcessPaymentUseCase,
) {
    companion object : Log

    @KafkaListener(topics = [KafkaTopics.Order.PLACED], groupId = "payment-service")
    suspend fun onOrderPlaced(event: OrderPlacedEvent) {
        logger.info { "주문 생성 이벤트 수신: orderId=${event.aggregateId}" }
        processPaymentUseCase.execute(
            ProcessPaymentCommand(
                orderId = event.aggregateId,
                userId = event.userId,
                amount = calculateAmount(event), // 실제로는 상품 가격 조회 필요
            )
        )
    }
}
```

### 4-6. DB Migration

```sql
-- V1__create_payments_table.sql
CREATE TABLE payments (
    id          VARCHAR(36)  PRIMARY KEY,
    order_id    VARCHAR(36)  NOT NULL UNIQUE,
    user_id     VARCHAR(36)  NOT NULL,
    amount      BIGINT       NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_order_id ON payments (order_id);
```

### 4-7. 테스트 전략

| 테스트 | 검증 대상 |
|--------|-----------|
| **ProcessPaymentServiceTest** | PG 성공 → payment.completed 발행, PG 실패 → payment.failed 발행, 타임아웃 처리 |
| **FakePaymentGatewayAdapterTest** | 응답 지연, 성공/실패 비율 확인 |
| **KafkaPaymentEventAdapterTest** | Kafka 메시지가 올바른 토픽에 올바른 키로 발행되는지 |

---

## 5. Step 3: notification-service (포트 8084)

### 면접 포인트
- **"실시간 알림은 어떻게 구현했나요?"**
  → SSE (Server-Sent Events)로 서버 → 클라이언트 단방향 스트리밍. WebSocket보다 단순하고 HTTP 호환
- **"Kafka 메시지 처리에 실패하면?"**
  → DLQ (Dead Letter Queue)로 이동. 재처리 로직은 별도 배치로 분리
- **"SSE 연결이 끊어지면?"**
  → 클라이언트가 `EventSource`로 자동 재연결. 서버는 `lastEventId`로 놓친 이벤트 전송 (선택 구현)

### 5-1. Domain Layer

```kotlin
// Notification.kt
data class Notification(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val createdAt: Instant,
)

enum class NotificationType {
    ORDER_PLACED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    ORDER_CANCELLED,
}
```

```kotlin
// NotificationError.kt
sealed interface NotificationError {
    data class UserNotConnected(val userId: String) : NotificationError
    data class SendFailed(val userId: String, val reason: String) : NotificationError
}
```

### 5-2. Port Out

```kotlin
// SseEmitterPort.kt
interface SseEmitterPort {
    /** 특정 유저에게 SSE 이벤트 전송 */
    suspend fun send(userId: String, notification: Notification): Boolean

    /** 유저의 SSE 연결 등록 */
    fun subscribe(userId: String): Flux<ServerSentEvent<Notification>>

    /** 유저의 SSE 연결 해제 */
    fun unsubscribe(userId: String)
}
```

### 5-3. UseCase

```kotlin
// SendNotificationUseCase.kt
interface SendNotificationUseCase {
    suspend fun execute(command: SendNotificationCommand): Result<Unit, NotificationError>
}

data class SendNotificationCommand(
    val userId: String,
    val type: NotificationType,
    val title: String,
    val message: String,
)
```

### 5-4. Adapter — SSE Emitter

```kotlin
// SseEmitterAdapter.kt
@Component
class SseEmitterAdapter : SseEmitterPort {
    companion object : Log

    // userId → SSE Sink 매핑
    private val connections = ConcurrentHashMap<String, Sinks.Many<ServerSentEvent<Notification>>>()

    override suspend fun send(userId: String, notification: Notification): Boolean {
        val sink = connections[userId] ?: return false
        val event = ServerSentEvent.builder(notification)
            .id(notification.id)
            .event(notification.type.name)
            .build()
        return sink.tryEmitNext(event).isSuccess
    }

    override fun subscribe(userId: String): Flux<ServerSentEvent<Notification>> {
        val sink = Sinks.many().multicast().onBackpressureBuffer<ServerSentEvent<Notification>>()
        connections[userId] = sink
        logger.info { "SSE 연결: userId=$userId" }
        return sink.asFlux()
            .doFinally { connections.remove(userId) }
    }

    override fun unsubscribe(userId: String) {
        connections.remove(userId)?.tryEmitComplete()
    }
}
```

### 5-5. Adapter In — Controller (SSE)

```kotlin
// SseController.kt
@RestController
@RequestMapping("/api/notifications")
class SseController(
    private val sseEmitterPort: SseEmitterPort,
) {
    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(@RequestParam userId: String): Flux<ServerSentEvent<Notification>> {
        return sseEmitterPort.subscribe(userId)
    }
}
```

### 5-6. Adapter In — Kafka Consumer

```kotlin
// NotificationEventListener.kt
@Component
class NotificationEventListener(
    private val sendNotificationUseCase: SendNotificationUseCase,
) {
    companion object : Log

    @KafkaListener(
        topics = [KafkaTopics.Order.PLACED, KafkaTopics.Payment.COMPLETED, KafkaTopics.Payment.FAILED],
        groupId = "notification-service",
    )
    suspend fun onEvent(event: DomainEvent) {
        val command = when (event.eventType) {
            "order.placed" -> SendNotificationCommand(
                userId = (event as OrderPlacedEvent).userId,
                type = NotificationType.ORDER_PLACED,
                title = "주문 접수",
                message = "주문이 정상적으로 접수되었습니다.",
            )
            "payment.completed" -> SendNotificationCommand(
                userId = extractUserId(event),
                type = NotificationType.PAYMENT_COMPLETED,
                title = "결제 완료",
                message = "결제가 완료되었습니다.",
            )
            "payment.failed" -> SendNotificationCommand(
                userId = extractUserId(event),
                type = NotificationType.PAYMENT_FAILED,
                title = "결제 실패",
                message = "결제에 실패했습니다. 다시 시도해주세요.",
            )
            else -> {
                logger.warn { "알 수 없는 이벤트 타입: ${event.eventType}" }
                return
            }
        }
        sendNotificationUseCase.execute(command)
    }
}
```

### 5-7. 테스트 전략

| 테스트 | 검증 대상 |
|--------|-----------|
| **SendNotificationServiceTest** | 연결된 유저에게 알림 전송, 미연결 유저 에러 처리 |
| **SseEmitterAdapterTest** | subscribe → send → 이벤트 수신 확인, 연결 해제 후 정리 |
| **SSE E2E** | WebTestClient로 SSE 연결 → Kafka 이벤트 발행 → 실시간 수신 확인 |

---

## 6. Step 4: gateway (포트 8080)

### 면접 포인트
- **"Rate Limiting은 어떻게 구현했나요?"**
  → Redis Token Bucket (Lua Script). IP별로 초당 요청 수 제한
- **"왜 Token Bucket인가?"**
  → 버스트 트래픽 허용 (토큰이 쌓여 있으면 한 번에 소진 가능). Fixed Window는 경계 시점에 2배 트래픽 허용 문제가 있음
- **"Rate Limit 우회는 어떻게 막나요?"**
  → IP + User-Agent + Token 조합으로 clientId 생성 (IP만 쓰면 프록시 뒤에서 우회 가능)

### 6-1. Token Bucket Lua Script

```kotlin
// RedisRateLimitAdapter.kt
@Component
class RedisRateLimitAdapter(
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val timeouts: TimeoutProperties,
) {
    companion object : Log

    // Token Bucket Lua Script
    // KEYS[1] = ratelimit:bucket:{clientId}
    // ARGV[1] = maxTokens, ARGV[2] = refillRate (tokens/sec), ARGV[3] = now (epoch millis)
    private val rateLimitScript = RedisScript.of<Long>(
        """
        local key = KEYS[1]
        local maxTokens = tonumber(ARGV[1])
        local refillRate = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])

        local data = redis.call('HMGET', key, 'tokens', 'lastRefill')
        local tokens = tonumber(data[1]) or maxTokens
        local lastRefill = tonumber(data[2]) or now

        -- 시간 경과에 따른 토큰 보충
        local elapsed = (now - lastRefill) / 1000.0
        tokens = math.min(maxTokens, tokens + elapsed * refillRate)

        if tokens >= 1 then
            tokens = tokens - 1
            redis.call('HMSET', key, 'tokens', tokens, 'lastRefill', now)
            redis.call('EXPIRE', key, maxTokens / refillRate + 10)
            return 1  -- allowed
        end

        redis.call('HMSET', key, 'tokens', tokens, 'lastRefill', now)
        redis.call('EXPIRE', key, maxTokens / refillRate + 10)
        return 0  -- rejected
        """.trimIndent(),
        Long::class.java,
    )

    suspend fun isAllowed(clientId: String, maxTokens: Int, refillRate: Int): Boolean =
        withTimeout(timeouts.redisLuaScript) {
            val key = RedisKeys.RateLimit.bucket(clientId)
            val result = redisTemplate.execute(
                rateLimitScript,
                listOf(key),
                listOf(maxTokens.toString(), refillRate.toString(), Instant.now().toEpochMilli().toString()),
            ).awaitSingle()
            result == 1L
        }
}
```

### 6-2. WebFilter

```kotlin
// RateLimitFilter.kt
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RateLimitFilter(
    private val rateLimitAdapter: RedisRateLimitAdapter,
) : CoWebFilter() {

    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        val clientId = extractClientId(exchange.request)
        val allowed = rateLimitAdapter.isAllowed(clientId, maxTokens = 50, refillRate = 10)

        if (!allowed) {
            exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
            exchange.response.headers.add("Retry-After", "1")
            return
        }
        chain.filter(exchange)
    }

    private fun extractClientId(request: ServerHttpRequest): String {
        val ip = request.remoteAddress?.address?.hostAddress ?: "unknown"
        val userAgent = request.headers.getFirst("User-Agent") ?: "unknown"
        // IP + User-Agent 조합으로 우회 방지
        return "$ip:${userAgent.hashCode()}"
    }
}
```

### 6-3. 테스트 전략

| 테스트 | 검증 대상 |
|--------|-----------|
| **RedisRateLimitAdapterTest** | Token Bucket: 한도 내 허용, 한도 초과 차단, 시간 경과 후 토큰 보충 |
| **RateLimitFilterTest** | 429 응답 반환, Retry-After 헤더, 정상 요청 통과 |

---

## 7. Step 5: 차별화 전략 (부하 테스트 + 문서화)

> 이 단계가 다른 포트폴리오와 차별화되는 핵심 포인트.
> 대부분의 토이 프로젝트는 "구현만 하고 끝"이지만, 정량적 결과가 있으면 면접관의 인상이 다르다.

### 7-1. k6 부하 테스트

**파일**: `tests/performance/`

```javascript
// order-load-test.js — 동시 1000명 주문 시나리오
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        flash_sale: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 100 },   // 웜업
                { duration: '30s', target: 1000 },   // 피크
                { duration: '10s', target: 0 },      // 쿨다운
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],  // p95 < 500ms, p99 < 1s
        http_req_failed: ['rate<0.01'],                     // 에러율 < 1%
    },
};

export default function () {
    const userId = `user-${__VU}-${__ITER}`;
    const payload = JSON.stringify({
        userId: userId,
        saleEventId: 'sale-001',
        productId: 'product-001',
        quantity: 1,
        idempotencyKey: `${userId}-${Date.now()}`,
    });

    const res = http.post('http://localhost:8080/api/orders', payload, {
        headers: { 'Content-Type': 'application/json' },
    });

    check(res, {
        'status is 201 or 409': (r) => r.status === 201 || r.status === 409,
        'response time < 500ms': (r) => r.timings.duration < 500,
    });

    sleep(0.1);
}
```

### 7-2. 측정 항목 및 기록 포맷

```markdown
## 성능 테스트 결과

### 환경
- Docker Compose (단일 노드): Redis, Kafka, PostgreSQL
- k6 (로컬 실행), 동시 사용자 1000명

### Before 최적화
| 항목 | 수치 |
|------|------|
| TPS  | 3,200 req/s |
| p50  | 45ms |
| p95  | 320ms |
| p99  | 890ms |
| 에러율 | 2.3% |
| 재고 정합성 | 100개 중 103개 판매 (❌ 오버셀링) |

### After 최적화 (Lua Script 적용)
| 항목 | 수치 |
|------|------|
| TPS  | 12,500 req/s |
| p50  | 12ms |
| p95  | 85ms |
| p99  | 210ms |
| 에러율 | 0.1% |
| 재고 정합성 | 100개 중 정확히 100개 판매 (✅) |

### 개선 포인트
1. **재고 차감**: Redis GET+SET → Lua Script (오버셀링 해결)
2. **DB 병목**: 동기 JDBC → R2DBC (커넥션 효율 4배 향상)
3. **Rate Limiting**: 없음 → Token Bucket (악성 트래픽 차단)
```

### 7-3. 카오스 테스트 시나리오

| 시나리오 | 방법 | 확인 사항 |
|----------|------|-----------|
| **Redis 다운** | `docker stop redis` | 주문 실패 응답, 서비스 크래시 없음, 재연결 후 복구 |
| **Kafka 다운** | `docker stop kafka` | 주문은 DB 저장 성공, 이벤트 발행 실패 시 로그 + 재시도 |
| **PostgreSQL 다운** | `docker stop postgres` | 주문 실패, 재고 복구(보상), 에러 응답 |
| **결제 지연** | FakeGateway delay 10s | 3초 타임아웃 → payment.failed 발행 → 재고 복구 |

### 7-4. 트레이드오프 문서화 (면접용)

면접에서 가장 높이 평가받는 것은 **"왜 그 선택을 했는가"**에 대한 명확한 답변이다.

| 선택 | 대안 | 선택 이유 |
|------|------|-----------|
| **Choreography Saga** | Orchestration Saga | 서비스 간 결합도 최소화. 3개 서비스 규모에서는 Orchestrator 도입 오버헤드가 큼 |
| **Redis Lua Script** | Redisson 분산 락 | Lua Script는 μs 단위, Redisson 락은 ms 단위. 단순 재고 차감은 Lua가 10배 빠름 |
| **SSE** | WebSocket | 서버→클라이언트 단방향이므로 SSE가 적합. WebSocket은 양방향이 필요할 때 |
| **Token Bucket** | Sliding Window | 버스트 허용이 필요. Fixed/Sliding Window는 경계 문제가 있음 |
| **R2DBC** | JPA + HikariCP | WebFlux와 non-blocking 일관성. JPA는 blocking이라 스레드 낭비 |
| **Kotest DescribeSpec** | JUnit 5 | BDD 스타일로 테스트 의도가 명확. 한국어 컨텍스트명 가독성 ↑ |

---

## 8. 참고할 기존 코드 패턴 요약

각 레이어를 구현할 때 queue-service의 같은 레이어를 참조하면 일관성을 유지할 수 있다.

| 레이어 | 참조 파일 | 핵심 패턴 |
|--------|-----------|-----------|
| **Domain** | `queue-service/.../domain/QueueError.kt` | `sealed interface` 에러 정의 |
| **Port In** | `queue-service/.../port/in/EnqueueUserUseCase.kt` | `suspend fun execute(command): Result<T, E>` |
| **Port Out** | `queue-service/.../port/out/QueuePort.kt` | 기술 세부사항 없는 인터페이스 |
| **Service** | `queue-service/.../service/EnqueueUserService.kt` | `@Service`, Port 주입, `Result` 반환 |
| **Redis Adapter** | `queue-service/.../adapter/out/redis/RedisQueueAdapter.kt` | `withTimeout`, `awaitSingle()` |
| **Controller** | `queue-service/.../adapter/in/web/QueueController.kt` | `result.fold(onSuccess, onFailure)`, `ErrorResponse` |
| **Unit Test** | `queue-service/.../service/EnqueueUserServiceTest.kt` | `DescribeSpec`, `mockk`, `coEvery` |
| **Integration Test** | `queue-service/.../redis/RedisQueueAdapterTest.kt` | `@SpringBootTest`, `IntegrationTestBase` |

### 기존 가이드 문서

- `docs/guides/add-redis-operation.md` — Redis Lua Script, Redisson 락, Token Bucket 구현 가이드
- `docs/guides/add-kafka-consumer.md` — Kafka Consumer 추가 가이드
- `docs/guides/add-db-entity.md` — R2DBC + Flyway 엔티티 추가 가이드
- `docs/guides/add-api-endpoint.md` — WebFlux 컨트롤러 추가 가이드
- `docs/guides/add-saga-pattern.md` — Saga 패턴 구현 가이드
- `docs/guides/add-test.md` — 테스트 작성 가이드
- `docs/08-saga-pattern.md` — Saga 패턴 개념 + Choreography vs Orchestration 비교

---

## 변경사항 요약

- `docs/IMPLEMENTATION-GUIDE.md` 신규 작성
  - 4개 서비스(order, payment, notification, gateway)의 상세 구현 스펙
  - 전체 Saga 이벤트 흐름도 및 Kafka 토픽 매핑
  - 각 서비스별 코드 예시 (domain, port, service, adapter, test)
  - 부하 테스트(k6) 시나리오 및 성능 기록 포맷
  - 카오스 테스트 시나리오
  - 트레이드오프 문서화 가이드
  - 면접 포인트 정리
