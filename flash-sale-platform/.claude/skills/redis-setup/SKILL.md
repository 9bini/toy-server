---
name: redis-setup
description: Redis 연산을 설정합니다. Lua Script, 분산 락(Redisson), Sorted Set 대기열, Token Bucket Rate Limiting 등을 구현합니다.
argument-hint: [operation-type lua-script|distributed-lock|sorted-set|rate-limiting|cache] [description]
---

$ARGUMENTS Redis 구성을 설정하세요.

## 연산 유형별 가이드

### lua-script (Lua 스크립트)
- 파일 위치: `{service}/src/main/resources/redis/{script-name}.lua`
- `ReactiveRedisTemplate`의 `execute(RedisScript, ...)` 로 실행
- 원자성 보장: 여러 Redis 커맨드를 하나의 Lua로 묶기

```lua
-- 예: 재고 차감 스크립트 (stock_decrement.lua)
local stock = redis.call('GET', KEYS[1])
if tonumber(stock) >= tonumber(ARGV[1]) then
    redis.call('DECRBY', KEYS[1], ARGV[1])
    return 1
else
    return 0
end
```

```kotlin
// Kotlin에서 Lua Script 실행
suspend fun decrementStock(productId: String, quantity: Int): Boolean {
    val script = RedisScript.of(ClassPathResource("redis/stock_decrement.lua"), Long::class.java)
    return reactiveRedisTemplate.execute(script, listOf("stock:$productId"), listOf(quantity.toString()))
        .awaitFirst() == 1L
}
```

### distributed-lock (분산 락)
- Redisson `RLockReactive` 사용
- 락 획득 타임아웃, 리스 타임아웃 반드시 설정
- `tryLock` + 코루틴 연동

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

### sorted-set (대기열)
- `ZADD`: 진입 시각을 score로 사용
- `ZRANGEBYSCORE`: 순서대로 조회
- `ZREM`: 처리 완료 후 제거
- TTL 기반 만료: 별도 스케줄러로 오래된 항목 정리

### rate-limiting (Token Bucket)
- Lua Script로 토큰 차감 + 리필 원자적 처리
- Gateway에서 요청별 실행
- 429 Too Many Requests 응답 연동

### cache (캐시)
- Spring Cache + Redis 설정
- `@Cacheable` / `@CacheEvict` 어노테이션
- TTL 전략 설정
- Cache-Aside 패턴

## 필수 사항
- 모든 Redis 연산은 Reactive/Coroutine 기반 (`awaitFirst()`, `awaitFirstOrNull()`)
- Lua Script는 반드시 원자성 테스트 작성 (동시 실행 시나리오)
- 통합 테스트는 Testcontainers Redis 사용
