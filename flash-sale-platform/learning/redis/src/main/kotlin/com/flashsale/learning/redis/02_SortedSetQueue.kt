package com.flashsale.learning.redis

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.web.bind.annotation.*

/**
 * === 2. Sorted Set 기반 대기열 ===
 *
 * Sorted Set (ZSet): 점수(score)로 정렬되는 집합
 * - ZADD: 멤버 추가 (score = 타임스탬프 → 선착순)
 * - ZRANK: 멤버의 순위 조회 (0-based)
 * - ZRANGE: 범위 조회 (순위순)
 * - ZREM: 멤버 제거 (대기열 이탈)
 * - ZCARD: 전체 멤버 수
 *
 * flash-sale의 queue-service가 이 패턴을 사용:
 * - score = 등록 시각 (밀리초) → 먼저 등록한 사람이 낮은 순위
 * - 주기적으로 상위 N명을 꺼내서 주문 페이지로 안내
 */
@RestController
@RequestMapping("/api/queue")
class SortedSetQueueController(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {

    companion object {
        private const val QUEUE_KEY = "learning:queue:waiting"
    }

    /**
     * 대기열 등록
     * score = 현재 시각 (밀리초) → 먼저 온 사람이 낮은 점수
     */
    @PostMapping("/join")
    suspend fun joinQueue(@RequestParam userId: String): Map<String, Any> {
        val score = System.currentTimeMillis().toDouble()

        // add()는 새로 추가되면 true, 이미 존재하면 false 반환
        val added = redisTemplate.opsForZSet()
            .add(QUEUE_KEY, userId, score)
            .awaitSingle()

        val rank = redisTemplate.opsForZSet()
            .rank(QUEUE_KEY, userId)
            .awaitFirstOrNull() ?: -1

        return mapOf(
            "userId" to userId,
            "added" to added,
            "position" to (rank + 1), // 1-based 순번
            "message" to if (added) "대기열에 등록되었습니다" else "이미 등록되어 있습니다"
        )
    }

    /**
     * 내 순번 조회
     */
    @GetMapping("/position/{userId}")
    suspend fun getPosition(@PathVariable userId: String): Map<String, Any> {
        val rank = redisTemplate.opsForZSet()
            .rank(QUEUE_KEY, userId)
            .awaitFirstOrNull()

        val totalWaiting = redisTemplate.opsForZSet()
            .size(QUEUE_KEY)
            .awaitSingle()

        return if (rank != null) {
            mapOf(
                "userId" to userId,
                "position" to (rank + 1),
                "totalWaiting" to totalWaiting,
                "estimatedWaitSec" to (rank + 1) * 2 // 1명당 2초 예상
            )
        } else {
            mapOf("userId" to userId, "message" to "대기열에 없습니다")
        }
    }

    /**
     * 상위 N명 꺼내기 (처리 대상)
     * → 실제로는 스케줄러가 주기적으로 호출
     */
    @PostMapping("/process")
    suspend fun processNext(@RequestParam(defaultValue = "5") count: Long): Map<String, Any> {
        // 상위 N명 조회
        val topUsers = redisTemplate.opsForZSet()
            .range(QUEUE_KEY, org.springframework.data.domain.Range.closed(0L, count - 1))
            .collectList()
            .awaitSingle()

        // 조회한 사용자들을 대기열에서 제거
        topUsers.forEach { userId ->
            redisTemplate.opsForZSet().remove(QUEUE_KEY, userId).awaitSingle()
        }

        return mapOf(
            "processed" to topUsers,
            "count" to topUsers.size,
            "message" to "${topUsers.size}명을 처리했습니다"
        )
    }

    /**
     * 대기열 현황
     */
    @GetMapping("/status")
    suspend fun queueStatus(): Map<String, Any> {
        val total = redisTemplate.opsForZSet()
            .size(QUEUE_KEY)
            .awaitSingle()
        return mapOf("totalWaiting" to total, "queueKey" to QUEUE_KEY)
    }
}
