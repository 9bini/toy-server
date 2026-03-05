# Error Knowledge Base (Error KB)

> A knowledge base that accumulates causes and resolution patterns for recurring errors.
> When a new error is discovered, add a document to the appropriate category.

## Categories

### architecture/ — Architecture Related
| ID | Title | Root Cause |
|----|-------|-----------|
| ARCH-001 | Hexagonal dependency inversion | Adapter bypasses domain and references directly |
| ARCH-002 | Technical details exposed in Port | Redis/Kafka types used in Port interface |
| ARCH-003 | Circular dependency | Bidirectional calls between services |

### spring/ — Spring Framework Related
| ID | Title | Root Cause |
|----|-------|-----------|
| SPR-001 | Bean circular reference | Constructor injection cycle, design issue when bypassed with @Lazy |
| SPR-002 | @Async + Coroutine conflict | Need to use coroutines instead of @Async |
| SPR-003 | WebFlux blocking call | Blocking I/O call inside suspend fun |
| SPR-004 | R2DBC transaction propagation | Transaction propagation not working in coroutine context |

### database/ — DB/Cache Related
| ID | Title | Root Cause |
|----|-------|-----------|
| DB-001 | Redis non-atomic operation | GET -> compare -> SET pattern (Lua Script needed) |
| DB-002 | Redis connection pool exhaustion | Waiting without withTimeout, insufficient pool size |
| DB-003 | Kafka Consumer reprocessing | Duplicate execution due to missing idempotency handling |

### kotlin/ — Kotlin Language Related
| ID | Title | Root Cause |
|----|-------|-----------|
| KT-001 | Null Safety violation | !! usage, unhandled platform type |
| KT-002 | Coroutine structure violation | GlobalScope, fire-and-forget pattern |

### security/ — Security Related
| ID | Title | Root Cause |
|----|-------|-----------|
| SEC-001 | IDOR | Resource ownership not verified |
| SEC-002 | Race Condition exploitation | Concurrent stock decrement attack |
| SEC-003 | Rate Limiting bypass | Relying on a single identifier |

## Document Template

Follow the format below when writing a new Error KB document:

```markdown
# {ID}: {Title}

## Symptoms
- What error message/behavior occurs

## Cause
- Root cause explanation

## Resolution
- Step-by-step resolution
- Code examples

## Prevention
- Rules/patterns to prevent recurrence

## Related
- Links to related Error KB documents
```
