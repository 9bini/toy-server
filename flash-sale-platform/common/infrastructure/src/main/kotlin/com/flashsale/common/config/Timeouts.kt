package com.flashsale.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * 타임아웃 설정. application.yml에서 오버라이드 가능.
 *
 * 기본값은 코드에 정의되고, 운영 환경에서는
 * application.yml로 재배포 없이 조정할 수 있다.
 *
 * ```yaml
 * flashsale:
 *   timeout:
 *     redis-operation-ms: 150
 *     payment-api-ms: 5000
 * ```
 *
 * 사용 예시:
 * ```kotlin
 * @Component
 * class StockAdapter(private val timeouts: TimeoutProperties) {
 *     suspend fun getStock(productId: String): Int =
 *         withTimeout(timeouts.redisOperation) { ... }
 * }
 * ```
 */
@ConfigurationProperties(prefix = "flashsale.timeout")
data class TimeoutProperties(
    /** Redis 단순 연산 (GET/SET). 보통 1-5ms, 100ms 넘으면 문제 상황 */
    @DefaultValue("100") val redisOperationMs: Long = 100,
    /** Redis Lua Script 실행 */
    @DefaultValue("200") val redisLuaScriptMs: Long = 200,
    /** Redisson 분산 락 획득 대기 */
    @DefaultValue("3000") val distributedLockWaitMs: Long = 3000,
    /** 분산 락 유지 시간 (이 시간 내에 작업 완료 필요) */
    @DefaultValue("5000") val distributedLockLeaseMs: Long = 5000,
    /** Kafka 메시지 발행 */
    @DefaultValue("1000") val kafkaProduceMs: Long = 1000,
    /** 외부 결제 API 호출 */
    @DefaultValue("3000") val paymentApiMs: Long = 3000,
    /** DB 쿼리 (R2DBC) */
    @DefaultValue("2000") val dbQueryMs: Long = 2000,
    /** DB 트랜잭션 전체 */
    @DefaultValue("5000") val dbTransactionMs: Long = 5000,
    /** 서비스 간 HTTP 호출 */
    @DefaultValue("2000") val interServiceCallMs: Long = 2000,
    /** SSE 연결 유지 최대 시간 */
    @DefaultValue("300000") val sseConnectionMs: Long = 300000,
) {
    // Duration 변환 프로퍼티 — withTimeout에서 직접 사용
    val redisOperation: Duration get() = redisOperationMs.milliseconds
    val redisLuaScript: Duration get() = redisLuaScriptMs.milliseconds
    val distributedLockWait: Duration get() = distributedLockWaitMs.milliseconds
    val distributedLockLease: Duration get() = distributedLockLeaseMs.milliseconds
    val kafkaProduce: Duration get() = kafkaProduceMs.milliseconds
    val paymentApi: Duration get() = paymentApiMs.milliseconds
    val dbQuery: Duration get() = dbQueryMs.milliseconds
    val dbTransaction: Duration get() = dbTransactionMs.milliseconds
    val interServiceCall: Duration get() = interServiceCallMs.milliseconds
    val sseConnection: Duration get() = sseConnectionMs.milliseconds
}

/**
 * 기본 타임아웃 상수.
 * DI가 불가능한 곳(도메인 레이어, 테스트)에서 사용.
 */
object DefaultTimeouts {
    val REDIS_OPERATION = 100.milliseconds
    val REDIS_LUA_SCRIPT = 200.milliseconds
    val DISTRIBUTED_LOCK_WAIT = 3.seconds
    val DISTRIBUTED_LOCK_LEASE = 5.seconds
    val KAFKA_PRODUCE = 1.seconds
    val PAYMENT_API = 3.seconds
    val DB_QUERY = 2.seconds
    val DB_TRANSACTION = 5.seconds
    val INTER_SERVICE_CALL = 2.seconds
    val SSE_CONNECTION = 300.seconds
}
