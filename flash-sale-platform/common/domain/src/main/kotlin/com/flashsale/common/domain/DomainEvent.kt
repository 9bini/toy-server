package com.flashsale.common.domain

import java.time.Instant

/**
 * 모든 도메인 이벤트의 공통 필드를 정의하는 인터페이스.
 * Kafka로 발행되며, 서비스 간 통신의 기본 단위.
 *
 * interface로 정의하여 data class 구현체에서
 * equals/hashCode/copy가 모든 필드를 포함하도록 보장한다.
 *
 * 사용 예시:
 * ```kotlin
 * data class OrderPlacedEvent(
 *     override val aggregateId: String,
 *     override val eventType: String = "order.placed",
 *     override val occurredAt: Instant = Instant.now(),
 *     override val eventId: String = IdGenerator.generate(),
 *     val productId: String,
 *     val quantity: Int,
 *     val userId: String,
 * ) : DomainEvent
 * ```
 */
interface DomainEvent {
    /** 이벤트가 속한 애그리거트의 ID (Kafka 파티셔닝 키로 사용) */
    val aggregateId: String

    /** 이벤트 타입 (예: "order.placed", "payment.completed") */
    val eventType: String

    /** 이벤트 발생 시각 */
    val occurredAt: Instant

    /** 멱등성 보장을 위한 이벤트 고유 ID */
    val eventId: String
}
