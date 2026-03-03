package com.flashsale.learning.kafka

import tools.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * === 2. Kafka Consumer (메시지 소비) ===
 *
 * Consumer = Topic에서 메시지를 읽어 처리하는 역할
 *
 * 핵심 개념:
 * - Consumer Group: 같은 그룹의 소비자들이 파티션을 분배받음
 * - Offset: 각 파티션에서 어디까지 읽었는지 추적
 * - Manual Ack: 처리 완료 후 수동으로 오프셋 커밋 (데이터 유실 방지)
 *
 * flash-sale 규칙:
 * - 모든 Consumer는 멱등(idempotent)해야 함
 * - eventId로 중복 체크 → 이미 처리한 이벤트는 스킵
 * - 3회 재시도 후 DLQ로 이동
 */
@Component
class OrderEventConsumer(
    private val objectMapper: ObjectMapper
) {

    // 처리된 eventId 저장소 (실제로는 Redis 사용)
    private val processedEvents = ConcurrentHashMap.newKeySet<String>()

    // 처리 이력 (학습용 조회)
    val processedOrders = mutableListOf<OrderPlacedEvent>()

    /**
     * @KafkaListener: 토픽 구독 및 메시지 처리
     *
     * topics: 구독할 토픽
     * groupId: Consumer Group (같은 그룹의 소비자끼리 파티션 분배)
     * containerFactory: 사용할 리스너 컨테이너 (manual ack 설정)
     */
    @KafkaListener(
        topics = [ProducerController.TOPIC_ORDER_PLACED],
        groupId = "learning-payment-group"
    )
    fun handleOrderPlaced(record: ConsumerRecord<String, String>) {
        println("\n[Consumer] === 메시지 수신 ===")
        println("  Topic: ${record.topic()}")
        println("  Partition: ${record.partition()}")
        println("  Offset: ${record.offset()}")
        println("  Key: ${record.key()}")

        val event = objectMapper.readValue(record.value(), OrderPlacedEvent::class.java)

        // === 멱등성 체크 ===
        // 같은 eventId는 한 번만 처리 (Kafka 재전달 시 중복 방지)
        if (!processedEvents.add(event.eventId)) {
            println("  [SKIP] 이미 처리된 이벤트: ${event.eventId}")
            return
        }

        // === 비즈니스 로직 ===
        println("  [처리] 주문 ${event.orderId}: 결제 처리 시작")
        println("  [처리] 상품: ${event.productId}, 수량: ${event.quantity}")
        println("  [처리] 금액: ${event.totalPrice}원")

        // 실제로는 여기서 payment-service가 결제 API 호출
        processedOrders.add(event)

        println("  [완료] 결제 처리 성공")
    }
}

/**
 * 알림 이벤트 소비자 (다른 Consumer Group)
 *
 * 같은 토픽을 여러 Consumer Group이 구독 가능
 * → 각 그룹이 독립적으로 모든 메시지를 수신
 */
@Component
class NotificationEventConsumer {

    @KafkaListener(
        topics = [ProducerController.TOPIC_ORDER_PLACED],
        groupId = "learning-notification-group"
    )
    fun handleOrderPlacedForNotification(record: ConsumerRecord<String, String>) {
        println("\n[Notification Consumer] 주문 알림 전송")
        println("  Key: ${record.key()}")
        println("  → 사용자에게 주문 접수 알림 전송 (이메일, 푸시)")
    }
}
