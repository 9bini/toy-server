package com.flashsale.learning.redis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * === Redis + Redisson 학습 애플리케이션 ===
 *
 * 사전 조건: Redis 실행 필요
 *   docker run -d --name redis-learning -p 6380:6379 redis:7.0
 *
 * 실행: ./gradlew :learning:redis:bootRun
 *
 * Redis 용도 (flash-sale):
 * - 재고 관리: 원자적 재고 차감 (Lua Script)
 * - 대기열: Sorted Set으로 순번 관리
 * - 분산 락: Redisson 분산 락
 * - Rate Limiting: Token Bucket 알고리즘
 * - 멱등성 키: 중복 주문 방지
 */
@SpringBootApplication
class RedisLearningApplication

fun main(args: Array<String>) {
    runApplication<RedisLearningApplication>(*args)
}
