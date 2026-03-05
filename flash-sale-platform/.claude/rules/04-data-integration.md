# Data Integration Patterns (DB/Cache/Messaging)

## Redis Patterns

### Atomicity Guarantee
- All Redis operations must guarantee atomicity via **Lua Script** or **Redisson distributed lock**
- Compound operations beyond simple GET/SET must use Lua Script

### Key Management
```kotlin
object RedisKeys {
    fun stock(productId: String) = "stock:product:$productId"
    fun queue(saleEventId: String) = "queue:sale:$saleEventId"
    fun rateLimit(userId: String) = "rate-limit:user:$userId"
}
```
- Redis key patterns are centrally managed as constants in `object RedisKeys`
- Never insert user input directly into Redis keys (prevent Redis Injection)

### Timeouts
```kotlin
companion object {
    private val REDIS_OPERATION_TIMEOUT = 100.milliseconds  // typically 1-5ms
    private val REDIS_LOCK_TIMEOUT = 5.seconds
}
```

## Kafka Patterns

### Idempotency Required
- All Consumers must process idempotently (no side effects when reprocessing the same message)
- Idempotency key: `{aggregate-type}:{aggregate-id}:{event-id}`

### Topic Management
- Topic names are centrally managed in a constants file
- Naming: `{domain}.{event-type}` (e.g., `order.placed`, `payment.completed`)

### DLQ (Dead Letter Queue)
- Move to DLQ after 3 retry failures
- DLQ messages require alerting + manual processing

## DB (R2DBC) Patterns

### Transactions
- Be mindful of coroutine context when using `@Transactional`
- Use the Saga pattern for distributed transactions (no 2PC)

### External Calls Common Rules
- `withTimeout` is required for all external I/O
- Apply exponential backoff for retries
- Circuit Breaker pattern recommended (prevent failure propagation)
