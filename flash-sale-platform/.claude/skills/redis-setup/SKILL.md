---
name: redis-setup
description: Sets up Redis operations. Implements Lua Script, distributed locks (Redisson), Sorted Set queues, Token Bucket Rate Limiting, and more.
argument-hint: [operation-type lua-script|distributed-lock|sorted-set|rate-limiting|cache] [description]
---

$ARGUMENTS Set up the Redis configuration.

## Guide by Operation Type

### lua-script (Lua Script)
- File location: `{service}/src/main/resources/redis/{script-name}.lua`
- Execute with `ReactiveRedisTemplate`'s `execute(RedisScript, ...)`
- Atomicity guarantee: Bundle multiple Redis commands into a single Lua script

```lua
-- Example: Stock decrement script (stock_decrement.lua)
local stock = redis.call('GET', KEYS[1])
if tonumber(stock) >= tonumber(ARGV[1]) then
    redis.call('DECRBY', KEYS[1], ARGV[1])
    return 1
else
    return 0
end
```

```kotlin
// Executing Lua Script from Kotlin
suspend fun decrementStock(productId: String, quantity: Int): Boolean {
    val script = RedisScript.of(ClassPathResource("redis/stock_decrement.lua"), Long::class.java)
    return reactiveRedisTemplate.execute(script, listOf("stock:$productId"), listOf(quantity.toString()))
        .awaitFirst() == 1L
}
```

### distributed-lock (Distributed Lock)
- Use Redisson `RLockReactive`
- Must set lock acquisition timeout and lease timeout
- `tryLock` + coroutine integration

```kotlin
suspend fun <T> withDistributedLock(
    lockName: String,
    waitTime: Duration = 5.seconds,
    leaseTime: Duration = 10.seconds,
    action: suspend () -> T
): T {
    val lock = redissonClient.getLock(lockName)
    val acquired = lock.tryLockAsync(waitTime.inWholeMilliseconds, leaseTime.inWholeMilliseconds, TimeUnit.MILLISECONDS).awaitFirst()
    if (!acquired) throw LockAcquisitionException(lockName)
    try { return action() } finally { lock.unlockAsync().awaitFirstOrNull() }
}
```

### sorted-set (Queue)
- `ZADD`: Use entry timestamp as score
- `ZRANGEBYSCORE`: Query in order
- `ZREM`: Remove after processing
- TTL-based expiration: Separate scheduler to clean up old entries

### rate-limiting (Token Bucket)
- Atomic token decrement + refill via Lua Script
- Execute per request at the Gateway
- Integrate with 429 Too Many Requests response

### cache (Cache)
- Spring Cache + Redis configuration
- `@Cacheable` / `@CacheEvict` annotations
- TTL strategy configuration
- Cache-Aside pattern

## Required
- All Redis operations must be Reactive/Coroutine-based (`awaitFirst()`, `awaitFirstOrNull()`)
- Lua Scripts must have atomicity tests (concurrent execution scenarios)
- Integration tests must use Testcontainers Redis
