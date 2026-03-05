# Flash Sale Platform (Real-time First-Come-First-Served Limited Sale System)

## Project Overview
100K concurrent connections, 1,000 limited items first-come-first-served purchase system.
Kotlin + Spring WebFlux + Coroutines based microservice architecture.

## Architecture
- **gateway**: API Gateway + Rate Limiting (Redis Token Bucket)
- **queue-service**: Queue management (Redis Sorted Set + SSE)
- **order-service**: Order processing (Redis Lua Script + Redisson distributed lock)
- **payment-service**: Payment + Saga pattern compensating transactions
- **notification-service**: Notifications (SSE + Push + External API)
- **common/domain**: Shared domain models
- **common/infrastructure**: Shared infrastructure (Redis, Kafka config)
- **infra/nginx**: Nginx L7 reverse proxy (Rate Limiting, load balancing, SSE proxy)

## Build & Run
- Full build: `./gradlew build`
- Specific service: `./gradlew :services:order-service:build`
- Full test: `./gradlew test`
- Specific test: `./gradlew :services:order-service:test --tests "*.OrderServiceTest"`
- Start infra (dev): `docker compose up -d`
- Start infra (HA mode): `docker compose -f docker-compose.yml -f docker-compose.ha.yml up -d`
- Stop infra: `docker compose down`
- Lint check: `./gradlew ktlintCheck`
- Lint format: `./gradlew ktlintFormat`

## Code Conventions
- Kotlin code style: ktlint (official Kotlin style guide)
- All I/O functions must use `suspend fun`, no blocking code
- Use `coroutineScope` / `supervisorScope` appropriately
- Redis operations must ensure atomicity via Lua Script or Redisson
- Kafka messages must be processed idempotently
- All external communication requires `withTimeout`
- External calls (payment API, DB, etc.) require withTimeout + retry
- Define error types with sealed class / sealed interface
- Documentation, comments, and commit messages in English; code (variable/class/function names) in English
- This project is for practicing modern technology — prefer latest stable features of Spring Boot/Kotlin/libraries

## Package Structure (per service)
```
com.flashsale.{service-name}/
├── adapter/
│   ├── in/web/        # Controller (WebFlux)
│   └── out/           # External adapters (Redis, Kafka, DB)
├── application/
│   ├── port/in/       # Use case interfaces
│   └── port/out/      # Output port interfaces
├── domain/            # Domain entities, value objects
└── config/            # Spring configuration
```

## Git Workflow & Commit Strategy

### Commit Principles
- **Minimal logical unit commits**: Each commit contains only one logical change
- **English commit messages**: Written in English using conventional commits format
- **Each commit must pass build** (`./gradlew build` succeeds)
- **Always verify build before committing**

### Commit Message Format
```
{type}({scope}): {English description}

{body - reason for change and key details}

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
```

### Commit Types
| Type | Purpose | Example |
|------|---------|---------|
| `feat` | New feature | `feat(order): implement stock decrement Lua Script` |
| `fix` | Bug fix | `fix(queue): fix queue ranking calculation error` |
| `refactor` | Structural improvement without behavior change | `refactor(payment): extract Saga state machine` |
| `test` | Add/modify tests | `test(order): add concurrent order integration test` |
| `perf` | Performance improvement | `perf(gateway): optimize Rate Limiter Lua Script` |
| `docs` | Documentation change | `docs: update API spec documentation` |
| `chore` | Build/config change | `chore: introduce Gradle Version Catalog` |

### Commit Separation Guidelines
When implementing features, split into these units:
1. **Domain model** — Entity, VO, Error definitions
2. **Ports & Use cases** — Interfaces + business logic
3. **Adapters** — Redis/Kafka/DB implementations
4. **Controllers & Config** — API endpoints + Spring config
5. **Tests** — Unit + integration tests

For infrastructure/build changes:
1. Split by individual config change unit (dependencies, environment settings, automation, etc.)

### Branch Strategy
| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feature/{service}/{description}` | `feature/order/stock-decrement` |
| Hotfix | `hotfix/{service}/{description}` | `hotfix/queue/ranking-fix` |
| Refactoring | `refactor/{service}/{description}` | `refactor/payment/saga-cleanup` |
| Infrastructure | `chore/{description}` | `chore/gradle-version-catalog` |

## IMPORTANT
- Always run tests after writing them to verify they pass
- Redis/Kafka integration code must have integration tests (using Testcontainers)
- Benchmark recommended for performance-impacting changes
- Verify docker compose infrastructure is running before integration tests

---

## Code Readability Rules (Project-specific)

### Hexagonal Architecture Readability
- Port interfaces express only "what it does" (no implementation details)
- Adapter class names include tech stack: `RedisStockAdapter`, `KafkaOrderEventPublisher`
- UseCase class names reflect business actions: `PlaceOrderUseCase`, `DecrementStockUseCase`

### Coroutine Readability
- When using `coroutineScope`, add a comment explaining "why parallelization"
- `withTimeout` values defined as constants with intent in the name:
  ```kotlin
  companion object {
      // Redis response is typically 1-5ms, over 100ms indicates a problem
      private val REDIS_OPERATION_TIMEOUT = 100.milliseconds
      // External payment API allows up to 3 seconds
      private val PAYMENT_API_TIMEOUT = 3.seconds
  }
  ```

### Redis/Kafka Readability
- Redis key patterns centrally managed as constants in `object RedisKeys`:
  ```kotlin
  object RedisKeys {
      fun stock(productId: String) = "stock:product:$productId"
      fun queue(saleEventId: String) = "queue:sale:$saleEventId"
  }
  ```
- Kafka topic names centrally managed in a constants file

### Sealed Class Error Definitions
- Each error type must have KDoc explaining "when this error occurs":
  ```kotlin
  sealed interface OrderError {
      /** When product stock is less than requested quantity */
      data class InsufficientStock(val available: Int, val requested: Int) : OrderError
      /** Payment gateway timeout (exceeds 3 seconds) */
      data class PaymentTimeout(val orderId: String) : OrderError
  }
  ```

---

## Recurring Issue Log

> Accumulated record of recurring issues. Suggest adding new issues here when discovered.

| Date | Issue | Cause | Resolution Pattern |
|------|-------|-------|--------------------|
| 2026-02-25 | HA/redundancy not considered | Designed for single-instance dev environment | Separated into docker-compose.ha.yml overlay, applied withTimeout + retry pattern |
| 2026-02-25 | Missing traffic ingress layer | Services exposed directly without Nginx | Added Nginx reverse proxy (Rate Limiting + SSE proxy + load balancing) |
| 2026-02-25 | Hooks environment dependency | Hardcoded paths in session-start.sh, jq not installed | Dynamic path discovery, jq/python3/grep fallback chain |

---

## Implementation Order Guide (Required for new features)
Follow this order strictly:
1. **Domain** — Entity, Value Object, sealed interface Error (no external dependencies)
2. **Port Out** — Output Port interfaces (no tech details exposed)
3. **Port In** — UseCase interfaces
4. **UseCase** — Business logic implementation (suspend fun, withTimeout)
5. **Adapter Out** — Redis/Kafka/DB implementations (class names include tech stack)
6. **Adapter In** — Controller (suspend fun, WebFlux)
7. **Config** — Spring bean registration
8. **Test** — Unit tests → Integration tests in order

## Skills Usage Guide
| Task | Skill | Description |
|------|-------|-------------|
| Full feature implementation | `/full-feature` | One-stop: design → implement → test → PR |
| Quick bug fix | `/hotfix` | Analysis → fix → test → PR |
| API endpoint | `/implement-api` | Single API implementation |
| Write tests | `/write-test` | Test specific classes |
| Service design | `/design-service` | DDD-based design |
| Code review | `/review-code` | Code quality inspection |
| Debugging | `/debug-issue` | Systematic debugging |
| Redis setup | `/redis-setup` | Lua Script, distributed locks, queues |
| Kafka setup | `/kafka-setup` | Producer/Consumer setup |
| Saga implementation | `/saga-pattern` | Distributed transactions |
| Full check | `/check-all` | Build + test + lint + architecture |
| Documentation | `/document` | ADR, API docs |
| Performance test | `/performance-test` | k6 load test creation/execution |

## Self-Review Checklist (Required before code submission)

### Functionality
- [ ] Are all requirements implemented?
- [ ] Are edge cases handled (null, empty values, concurrency)?

### Architecture
- [ ] Does it follow Hexagonal Architecture package structure?
- [ ] Do dependencies point toward domain?

### Coroutine Safety
- [ ] Are all I/O operations suspend fun?
- [ ] Is GlobalScope avoided?
- [ ] Is withTimeout set for external calls?

### Concurrency/Consistency
- [ ] Are Redis operations atomic?
- [ ] Are Kafka messages processed idempotently?

### Stability/Redundancy
- [ ] Are withTimeout and retry patterns applied to external calls?
- [ ] Is there fallback/compensating logic for failures?

### Readability
- [ ] Are functions within 30 lines?
- [ ] Do function/variable names clearly express intent?
- [ ] Are complex business logic sections commented?
