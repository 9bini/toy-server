# Error Handling Protocol

## Sealed Class Error Definitions

Each service defines error types as sealed interfaces in the `domain` package:

```kotlin
sealed interface OrderError {
    /** When product stock is less than the requested quantity */
    data class InsufficientStock(val available: Int, val requested: Int) : OrderError
    /** Payment gateway timeout (exceeds 3 seconds) */
    data class PaymentTimeout(val orderId: String) : OrderError
    /** When attempting to reprocess an already processed order */
    data class DuplicateOrder(val orderId: String) : OrderError
}
```

### Rules
- Each error type must have KDoc describing the **trigger condition**
- Include context necessary for debugging in the error data
- Do not use generic errors (`GenericError`, `UnknownError`) — define specific types

## Error Propagation Pattern

```
UseCase (sealed class) → Controller (HTTP Status mapping) → Client (ErrorResponse)
```

- UseCase returns `Result<T>` or sealed class (throwing exceptions is prohibited)
- Controller converts sealed class → HTTP Status + ErrorResponse
- Catch external call exceptions in UseCase → convert to sealed class

## Timeout Errors

```kotlin
companion object {
    private val REDIS_OPERATION_TIMEOUT = 100.milliseconds
    private val PAYMENT_API_TIMEOUT = 3.seconds
    private val KAFKA_SEND_TIMEOUT = 5.seconds
}
```

- Timeout values are defined as constants with intent included in the name
- Convert `withTimeout` exceeding into a dedicated error type

## Logging Rules
- **ERROR**: Requires immediate response (payment failure, data consistency broken)
- **WARN**: Abnormal but auto-recoverable (retry success, Rate Limit exceeded)
- **INFO**: Business events (order created, payment completed)
- **DEBUG**: For debugging (enable only in development environment)
