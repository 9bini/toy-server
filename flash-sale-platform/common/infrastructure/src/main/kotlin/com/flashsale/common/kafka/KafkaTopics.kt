package com.flashsale.common.kafka

/**
 * Kafka 토픽 중앙 관리.
 * 토픽명 규칙: flashsale.{도메인}.{이벤트}
 *
 * 사용 예시:
 * ```kotlin
 * kafkaTemplate.send(KafkaTopics.Order.PLACED, orderId, event)
 * ```
 */
object KafkaTopics {
    object Order {
        /** 주문 생성됨 */
        const val PLACED = "flashsale.order.placed"

        /** 주문 취소됨 (보상 트랜잭션) */
        const val CANCELLED = "flashsale.order.cancelled"

        /** 주문 완료됨 */
        const val COMPLETED = "flashsale.order.completed"
    }

    object Payment {
        /** 결제 요청됨 */
        const val REQUESTED = "flashsale.payment.requested"

        /** 결제 완료됨 */
        const val COMPLETED = "flashsale.payment.completed"

        /** 결제 실패함 */
        const val FAILED = "flashsale.payment.failed"
    }

    object Stock {
        /** 재고 차감됨 */
        const val DECREMENTED = "flashsale.stock.decremented"

        /** 재고 복원됨 (보상 트랜잭션) */
        const val RESTORED = "flashsale.stock.restored"
    }

    object Notification {
        /** 알림 전송 요청 */
        const val SEND_REQUESTED = "flashsale.notification.send-requested"
    }

    /** DLQ 토픽 생성 (원본 토픽명 + .dlq) */
    fun dlq(originalTopic: String) = "$originalTopic.dlq"
}
