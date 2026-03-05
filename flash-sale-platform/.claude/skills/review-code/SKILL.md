---
name: review-code
description: Performs code review. Focuses on concurrency safety, performance, coroutine patterns, and Redis/Kafka usage patterns.
argument-hint: [file-path-or-service-name]
context: fork
---

$ARGUMENTS Review the code.

## Review Checklist

### Concurrency Safety
- [ ] Is it coroutine-safe without shared state?
- [ ] Is atomicity of Redis operations guaranteed? (Lua Script / Redisson)
- [ ] Is distributed lock usage appropriate? (No deadlock risk?)
- [ ] Is there no possibility of race conditions?

### Performance
- [ ] Are there no unnecessary blocking calls? (JDBC, Thread.sleep, etc.)
- [ ] Are parallelizable tasks not being executed sequentially?
- [ ] Are there no N+1 query patterns?
- [ ] Are there no unnecessary memory allocations?

### Error Handling
- [ ] Is `withTimeout` set for external calls?
- [ ] Is compensation handling implemented on failure?
- [ ] Are failures isolated to DLQ?
- [ ] Is error logging appropriate?

### Kotlin / Coroutines
- [ ] Is structured concurrency maintained? (No GlobalScope)
- [ ] Is `SupervisorScope` usage appropriate?
- [ ] Is context switching minimized?
- [ ] Is `Dispatchers.IO` used only for blocking code?
- [ ] Is Flow collection done in the appropriate scope?

### Kafka
- [ ] Is message publishing idempotent?
- [ ] Does the Consumer safely handle duplicate messages?
- [ ] Is DLQ configured?

### Redis
- [ ] Is key naming consistent?
- [ ] Is TTL configured?
- [ ] Is there no risk of memory leaks?

### Technology Currency
- [ ] Are deprecated APIs avoided? (Based on latest versions of Spring Boot/Kotlin/libraries)
- [ ] Are there better alternatives available in the latest stable version?

## Output Format
Classify results by priority:
- **Critical** (must fix): Data consistency, security, concurrency bugs
- **Warning** (recommended fix): Performance issues, potential problems
- **Suggestion** (improvement proposal): Code readability, pattern improvements

Include specific code line references and fix examples for each item.
