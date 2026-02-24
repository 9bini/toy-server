package com.flashsale.common.redis

/**
 * Redis 키 패턴 중앙 관리.
 * 모든 Redis 키를 여기서 관리하여 키 충돌과 오타를 방지한다.
 *
 * 네이밍 규칙: {도메인}:{엔티티}:{id}
 *
 * 사용 예시:
 * ```kotlin
 * val stockKey = RedisKeys.Stock.remaining("product-123")
 * // → "stock:remaining:product-123"
 * ```
 */
object RedisKeys {
    object Stock {
        /** 상품 잔여 재고 (String) */
        fun remaining(productId: String) = "stock:remaining:$productId"

        /** 재고 차감 Lua Script 잠금 (String) */
        fun lock(productId: String) = "stock:lock:$productId"
    }

    object Queue {
        /** 대기열 (Sorted Set, score = 진입 시각 millis) */
        fun waiting(saleEventId: String) = "queue:waiting:$saleEventId"

        /** 대기열 진입 토큰 (String, TTL로 만료 관리) */
        fun token(
            saleEventId: String,
            userId: String,
        ) = "queue:token:$saleEventId:$userId"

        /** 대기열 처리 상태 (Hash) */
        fun status(saleEventId: String) = "queue:status:$saleEventId"
    }

    object RateLimit {
        /** Token Bucket Rate Limiter (Hash: tokens, lastRefill) */
        fun bucket(clientId: String) = "ratelimit:bucket:$clientId"
    }

    object Order {
        /** 주문 멱등성 키 (String, TTL 24시간) */
        fun idempotencyKey(key: String) = "order:idempotency:$key"

        /** 사용자별 주문 상태 (Hash) */
        fun userOrder(
            userId: String,
            saleEventId: String,
        ) = "order:user:$userId:$saleEventId"
    }

    object Session {
        /** 사용자 세션 (Hash) */
        fun user(sessionId: String) = "session:user:$sessionId"
    }
}
