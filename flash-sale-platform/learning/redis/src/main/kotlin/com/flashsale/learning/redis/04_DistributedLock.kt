package com.flashsale.learning.redis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.redisson.api.RedissonClient
import org.springframework.web.bind.annotation.*
import java.util.concurrent.TimeUnit

/**
 * === 4. Redisson 분산 락 ===
 *
 * 분산 락이 필요한 이유:
 * - 여러 서버 인스턴스가 동일 리소스에 동시 접근할 때
 * - DB 트랜잭션만으로는 Redis-DB 간 일관성 보장 불가
 *
 * Redisson 분산 락 특징:
 * - Reentrant Lock (재진입 가능)
 * - 자동 만료 (leaseTime) → 데드락 방지
 * - Watchdog: leaseTime 미지정 시 30초마다 자동 갱신
 *
 * flash-sale에서의 사용:
 * - 주문 생성 시 상품별 락 획득 → 재고 차감 → 락 해제
 * - 멱등성 보장을 위한 처리
 */
@RestController
@RequestMapping("/api/lock")
class DistributedLockController(
    private val redissonClient: RedissonClient
) {

    /**
     * 기본 분산 락 사용 예제
     *
     * 패턴:
     * 1. 락 획득 시도 (waitTime 내에 획득 못하면 실패)
     * 2. 비즈니스 로직 실행
     * 3. finally에서 락 해제 (반드시!)
     */
    @PostMapping("/basic/{resourceId}")
    suspend fun basicLock(@PathVariable resourceId: String): Map<String, Any> {
        val lockKey = "learning:lock:$resourceId"
        val lock = redissonClient.getLock(lockKey)

        // tryLock(waitTime, leaseTime, unit)
        // waitTime: 락 획득 대기 시간 (이 시간 내에 못 얻으면 false)
        // leaseTime: 락 유지 시간 (이 시간 후 자동 해제 → 데드락 방지)
        val acquired = withContext(Dispatchers.IO) {
            lock.tryLock(3, 10, TimeUnit.SECONDS)
        }

        if (!acquired) {
            return mapOf(
                "resourceId" to resourceId,
                "success" to false,
                "message" to "락 획득 실패 (다른 프로세스가 사용 중)"
            )
        }

        try {
            // === 임계 구역 (Critical Section) ===
            println("  [Lock] $resourceId 락 획득 → 작업 시작")
            delay(1000) // 비즈니스 로직 시뮬레이션
            println("  [Lock] $resourceId 작업 완료")

            return mapOf(
                "resourceId" to resourceId,
                "success" to true,
                "message" to "작업 완료"
            )
        } finally {
            // 반드시 finally에서 해제!
            if (lock.isHeldByCurrentThread) {
                withContext(Dispatchers.IO) {
                    lock.unlock()
                }
                println("  [Lock] $resourceId 락 해제")
            }
        }
    }

    /**
     * 주문 처리 시뮬레이션 (flash-sale 패턴)
     *
     * 상품별 락을 사용하여 동시 주문 시 재고 일관성 보장
     */
    @PostMapping("/order/{productId}")
    suspend fun orderWithLock(
        @PathVariable productId: String,
        @RequestParam userId: String
    ): Map<String, Any> {
        val lockKey = "learning:lock:order:$productId"
        val lock = redissonClient.getLock(lockKey)

        val acquired = withContext(Dispatchers.IO) {
            lock.tryLock(
                5,  // 5초 대기 (대기열 통과 후이므로 짧게)
                10, // 10초 후 자동 해제
                TimeUnit.SECONDS
            )
        }

        if (!acquired) {
            return mapOf(
                "success" to false,
                "message" to "주문 처리 중입니다. 잠시 후 다시 시도해주세요."
            )
        }

        try {
            // 1. 재고 확인 (Redis)
            println("  [주문] 재고 확인: $productId")
            delay(50)

            // 2. 재고 차감 (Redis Lua Script)
            println("  [주문] 재고 차감: $productId")
            delay(50)

            // 3. 주문 생성 (DB)
            println("  [주문] DB 저장: $productId, $userId")
            delay(100)

            return mapOf(
                "success" to true,
                "orderId" to "ORD-${System.currentTimeMillis()}",
                "productId" to productId,
                "userId" to userId,
                "message" to "주문이 완료되었습니다"
            )
        } finally {
            if (lock.isHeldByCurrentThread) {
                withContext(Dispatchers.IO) {
                    lock.unlock()
                }
            }
        }
    }
}
