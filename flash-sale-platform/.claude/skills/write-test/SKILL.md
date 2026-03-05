---
name: write-test
description: Writes unit tests, integration tests, and performance tests. Follows Kotest + MockK + Testcontainers patterns.
argument-hint: [test-type unit|integration|performance] [target-class-or-feature]
---

$ARGUMENTS Write tests.

## Guide by Test Type

### unit (Unit Test)
- **Framework**: Kotest BehaviorSpec (Given/When/Then)
- **Mocking**: MockK
- **Location**: `src/test/kotlin/` (same package as source)
- **Target**: UseCase, Domain logic
- **Rules**: Mock all external dependencies, execute quickly

```kotlin
class PlaceOrderUseCaseTest : BehaviorSpec({
    val stockPort = mockk<StockPort>()
    val orderPort = mockk<OrderPort>()
    val sut = PlaceOrderUseCase(stockPort, orderPort)

    Given("when stock is sufficient") {
        coEvery { stockPort.verify(any()) } returns StockResult.Available(100)

        When("an order is requested") {
            val result = sut.execute(command)

            Then("the order succeeds") {
                result shouldBe OrderResult.Success
            }
        }
    }
})
```

### integration (Integration Test)
- **Framework**: Kotest + Spring Boot Test
- **Infrastructure**: Testcontainers (Redis, Kafka, PostgreSQL)
- **Location**: `src/test/kotlin/.../integration/`
- **Target**: Adapter, end-to-end flows
- **Rules**: Use real infrastructure, isolated environment

```kotlin
@SpringBootTest
class OrderIntegrationTest : BehaviorSpec({
    // Automatically start Redis, Kafka with Testcontainers
    Given("when product stock is 100") {
        // Set stock in Redis
        When("50 users order simultaneously") {
            // Concurrent requests with coroutines
            Then("exactly 50 are decremented") {
                // Verify atomicity
            }
        }
    }
})
```

### performance (Performance/Concurrency Test)
- **Location**: `tests/performance/`
- **Target**: Concurrency scenarios, load tests
- **Rules**: Measure and record metrics

## Required
- After writing tests, must run: `./gradlew test` or specific tests
- Analyze cause and fix on failure
- Must include edge cases:
  - null, empty values, boundary values
  - Concurrency (multiple coroutines running simultaneously)
  - Timeout
  - Network failure
