---
name: implement-api
description: Implements API endpoints based on Spring WebFlux + Kotlin Coroutines. Creates all layers including Controller, UseCase, Port, and Adapter.
argument-hint: [service-name] [endpoint-description]
---

$ARGUMENTS Implement the API.

## Implementation Order

### 1. Check Design Document
- If `docs/{service-name}/DESIGN.md` exists, check the API spec
- If not, confirm with the user and proceed with design via `/design-service` if needed

### 2. Domain Layer
- Create entities, value objects
- Domain service (if needed)

### 3. Application Layer
- Define Output Port interfaces (Port Out)
- Define Input Port (UseCase) interfaces
- Write UseCase implementation classes
  - Must use `suspend fun`
  - Use `coroutineScope { async { } }` for parallelizable tasks

### 4. Adapter Layer
- Implement Output Adapters (Redis/Kafka/DB)
- Implement Input Adapter (Controller)
  - `@RestController` + `suspend fun` approach
  - Input validation handled at the Controller level

### 5. Configuration
- Spring Configuration classes
- application.yml settings

### 6. Tests
- UseCase unit tests (MockK)
- Controller tests (WebTestClient)
- After writing, must run `./gradlew :services:{service}:test`

## Code Patterns

```kotlin
// UseCase implementation example
@Service
class PlaceOrderUseCase(
    private val stockPort: StockPort,
    private val userPort: UserPort,
    private val orderPort: OrderPort,
) : PlaceOrderPort {

    override suspend fun execute(command: PlaceOrderCommand): OrderResult =
        coroutineScope {
            val stock = async { stockPort.verify(command.productId) }
            val user = async { userPort.validate(command.userId) }
            awaitAll(stock, user)
            orderPort.save(Order.create(command))
        }
}
```

## Required Principles
- All I/O functions must be `suspend fun`
- Handle errors with `sealed class Result<T>` or `sealed interface`
- Request/Response DTOs in separate files
- Input validation at the Controller layer
