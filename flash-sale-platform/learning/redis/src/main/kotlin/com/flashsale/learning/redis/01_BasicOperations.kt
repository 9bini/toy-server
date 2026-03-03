package com.flashsale.learning.redis

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.web.bind.annotation.*
import java.time.Duration

/**
 * === 1. Redis 기본 연산 ===
 *
 * ReactiveRedisTemplate: Spring의 리액티브 Redis 클라이언트
 * - opsForValue(): String 타입 연산 (GET, SET, INCR)
 * - opsForHash(): Hash 타입 연산 (HGET, HSET)
 * - opsForList(): List 타입 연산 (LPUSH, RPOP)
 * - opsForSet(): Set 타입 연산 (SADD, SMEMBERS)
 * - opsForZSet(): Sorted Set 연산 (ZADD, ZRANK)
 *
 * Kotlin에서는 .awaitSingle() / .awaitFirstOrNull()로 코루틴 변환
 */
@RestController
@RequestMapping("/api/redis")
class BasicRedisController(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {

    // ============================
    // String 연산
    // ============================

    /**
     * SET: 키-값 저장
     * TTL(Time To Live)을 설정하면 자동 만료
     */
    @PostMapping("/string/{key}")
    suspend fun setString(
        @PathVariable key: String,
        @RequestBody body: Map<String, String>
    ): Map<String, Any> {
        val value = body["value"] ?: ""
        val ttlSeconds = body["ttl"]?.toLongOrNull()

        if (ttlSeconds != null) {
            redisTemplate.opsForValue()
                .set(key, value, Duration.ofSeconds(ttlSeconds))
                .awaitSingle()
        } else {
            redisTemplate.opsForValue()
                .set(key, value)
                .awaitSingle()
        }
        return mapOf("key" to key, "value" to value, "ttl" to (ttlSeconds ?: -1))
    }

    /**
     * GET: 키로 값 조회
     */
    @GetMapping("/string/{key}")
    suspend fun getString(@PathVariable key: String): Map<String, Any?> {
        val value = redisTemplate.opsForValue().get(key).awaitFirstOrNull()
        return mapOf("key" to key, "value" to value)
    }

    /**
     * INCR/DECR: 원자적 증감
     * → 재고 관리, 조회수 카운팅에 사용
     */
    @PostMapping("/counter/{key}/incr")
    suspend fun increment(@PathVariable key: String): Map<String, Any> {
        val newValue = redisTemplate.opsForValue().increment(key).awaitSingle()
        return mapOf("key" to key, "value" to newValue)
    }

    @PostMapping("/counter/{key}/decr")
    suspend fun decrement(@PathVariable key: String): Map<String, Any> {
        val newValue = redisTemplate.opsForValue().decrement(key).awaitSingle()
        return mapOf("key" to key, "value" to newValue)
    }

    // ============================
    // Hash 연산
    // ============================

    /**
     * Hash: 하나의 키 아래 여러 필드-값 쌍 저장
     * → 상품 정보, 사용자 세션 등에 유용
     *
     * 예: product:123 → { name: "스니커즈", price: "199000", stock: "100" }
     */
    @PostMapping("/hash/{key}")
    suspend fun setHash(
        @PathVariable key: String,
        @RequestBody fields: Map<String, String>
    ): Map<String, Any> {
        redisTemplate.opsForHash<String, String>()
            .putAll(key, fields)
            .awaitSingle()
        return mapOf("key" to key, "fields" to fields)
    }

    @GetMapping("/hash/{key}/{field}")
    suspend fun getHashField(
        @PathVariable key: String,
        @PathVariable field: String
    ): Map<String, Any?> {
        val value = redisTemplate.opsForHash<String, String>()
            .get(key, field)
            .awaitFirstOrNull()
        return mapOf("key" to key, "field" to field, "value" to value)
    }

    @GetMapping("/hash/{key}")
    suspend fun getAllHash(@PathVariable key: String): Map<String, Any> {
        val entries = redisTemplate.opsForHash<String, String>()
            .entries(key)
            .collectList()
            .awaitSingle()
        return mapOf("key" to key, "entries" to entries.associate { it.key to it.value })
    }
}
