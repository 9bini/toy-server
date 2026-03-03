package com.flashsale.learning.redis

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.web.bind.annotation.*

/**
 * === 3. Lua Script (원자적 연산) ===
 *
 * Redis에서 여러 명령을 원자적으로 실행해야 할 때 Lua Script 사용
 *
 * 왜 Lua Script가 필요한가?
 * - GET → 비교 → SET 패턴은 Race Condition 발생 가능
 * - 예: 재고 100 → 두 요청이 동시에 GET(100) → 둘 다 99로 SET
 * - Lua Script는 Redis 서버에서 원자적으로 실행 → Race Condition 방지
 *
 * flash-sale에서의 Lua Script 사용:
 * - 재고 차감: stock >= quantity 확인 + 차감을 원자적으로
 * - Rate Limiting: Token Bucket 알고리즘
 * - 대기열 입장: 중복 확인 + 추가를 원자적으로
 */
@RestController
@RequestMapping("/api/stock")
class LuaScriptController(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {

    /**
     * 재고 초기화
     */
    @PostMapping("/init/{productId}")
    suspend fun initStock(
        @PathVariable productId: String,
        @RequestParam(defaultValue = "100") quantity: Int
    ): Map<String, Any> {
        val key = "learning:stock:$productId"
        redisTemplate.opsForValue().set(key, quantity.toString()).awaitSingle()
        return mapOf("productId" to productId, "stock" to quantity)
    }

    /**
     * Lua Script로 재고 차감 (원자적)
     *
     * 스크립트 동작:
     * 1. 현재 재고 조회 (GET)
     * 2. 요청 수량과 비교
     * 3. 충분하면 차감 (DECRBY) → 남은 재고 반환
     * 4. 부족하면 -1 반환
     *
     * 이 모든 과정이 Redis 서버에서 원자적으로 실행됨!
     */
    @PostMapping("/decrease/{productId}")
    suspend fun decreaseStock(
        @PathVariable productId: String,
        @RequestParam(defaultValue = "1") quantity: Int
    ): Map<String, Any> {
        val key = "learning:stock:$productId"

        val script = RedisScript.of<Long>(
            """
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local requested = tonumber(ARGV[1])

            if current >= requested then
                return redis.call('DECRBY', KEYS[1], requested)
            else
                return -1
            end
            """.trimIndent(),
            Long::class.java
        )

        val result = redisTemplate.execute(
            script,
            listOf(key),
            listOf(quantity.toString())
        ).awaitSingle()

        return if (result >= 0) {
            mapOf(
                "productId" to productId,
                "remaining" to result,
                "decreased" to quantity,
                "success" to true
            )
        } else {
            mapOf(
                "productId" to productId,
                "message" to "재고 부족",
                "success" to false
            )
        }
    }

    /**
     * Rate Limiting: Token Bucket 알고리즘 (Lua Script)
     *
     * Token Bucket:
     * - 일정 주기로 토큰이 충전됨
     * - 요청마다 토큰 1개 소비
     * - 토큰이 없으면 요청 거부
     *
     * flash-sale의 gateway에서 이 패턴으로 API Rate Limiting 구현
     */
    @PostMapping("/ratelimit/check")
    suspend fun checkRateLimit(
        @RequestParam clientId: String,
        @RequestParam(defaultValue = "10") maxTokens: Int,
        @RequestParam(defaultValue = "60") windowSeconds: Int
    ): Map<String, Any> {
        val key = "learning:ratelimit:$clientId"

        val script = RedisScript.of<Long>(
            """
            local key = KEYS[1]
            local max_tokens = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local current = tonumber(redis.call('GET', key) or '0')

            if current < max_tokens then
                redis.call('INCR', key)
                if current == 0 then
                    redis.call('EXPIRE', key, window)
                end
                return max_tokens - current - 1
            else
                return -1
            end
            """.trimIndent(),
            Long::class.java
        )

        val remainingTokens = redisTemplate.execute(
            script,
            listOf(key),
            listOf(maxTokens.toString(), windowSeconds.toString())
        ).awaitSingle()

        return if (remainingTokens >= 0) {
            mapOf(
                "clientId" to clientId,
                "allowed" to true,
                "remainingTokens" to remainingTokens
            )
        } else {
            mapOf(
                "clientId" to clientId,
                "allowed" to false,
                "message" to "Rate limit 초과. 잠시 후 다시 시도하세요."
            )
        }
    }

    /**
     * 현재 재고 조회
     */
    @GetMapping("/{productId}")
    suspend fun getStock(@PathVariable productId: String): Map<String, Any?> {
        val key = "learning:stock:$productId"
        val stock = redisTemplate.opsForValue().get(key)
            .awaitSingle()
        return mapOf("productId" to productId, "stock" to stock)
    }
}
