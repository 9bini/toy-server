# Hexagonal Architecture Rules

## Package Structure (per service)

```
com.flashsale.{service-name}/
├── adapter/
│   ├── in/web/        # Controller (WebFlux, suspend fun)
│   └── out/           # External adapters (Redis, Kafka, DB)
├── application/
│   ├── port/in/       # Use case interfaces
│   └── port/out/      # Output port interfaces
├── domain/            # Domain entities, value objects, errors
└── config/            # Spring configuration
```

## Dependency Direction

```
adapter/in → application/port/in → domain
adapter/out → application/port/out → domain
```

- **domain**: No external dependencies (pure Kotlin)
- **port**: Interfaces only (no technology details exposed)
- **adapter**: Implementations (class names include tech stack)

## Implementation Order (new features)

Always follow the order below:
1. **Domain** — Entity, VO, sealed interface Error
2. **Port Out** — Output Port interfaces
3. **Port In** — UseCase interfaces
4. **UseCase** — Business logic (suspend fun, withTimeout)
5. **Adapter Out** — Redis/Kafka/DB implementations
6. **Adapter In** — Controller (suspend fun, WebFlux)
7. **Config** — Spring bean registration
8. **Test** — Unit tests first, then integration tests

## Naming Rules

| Layer | Pattern | Example |
|-------|---------|---------|
| Port In | `{Verb}{Noun}UseCase` | `PlaceOrderUseCase` |
| Port Out | `{Noun}{Verb}Port` | `StockDecrementPort` |
| Adapter | `{Tech}{Noun}Adapter` | `RedisStockAdapter` |
| Controller | `{Noun}Controller` | `OrderController` |
| Error | `sealed interface {Noun}Error` | `OrderError` |
