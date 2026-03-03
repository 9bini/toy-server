package com.flashsale.learning.kafka

import tools.jackson.databind.ObjectMapper
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

/**
 * === 1. Kafka Producer (메시지 발행) ===
 *
 * Producer = 메시지를 Topic에 발행하는 역할
 *
 * 핵심 개념:
 * - Topic: 메시지 카테고리 (예: "flashsale.order.placed")
 * - Partition: 토픽을 분할한 단위 (병렬 처리의 기본)
 * - Key: 같은 Key의 메시지는 같은 Partition으로 → 순서 보장
 * - Value: 실제 메시지 본문 (보통 JSON)
 *
 * flash-sale 규칙:
 * - Key = aggregateId (예: orderId) → 같은 주문의 이벤트 순서 보장
 * - acks = all → 모든 리플리카 확인 후 성공 (데이터 유실 방지)
 * - enable.idempotence = true → 중복 발행 방지
 */
@RestController
@RequestMapping("/api/kafka")
class ProducerController(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    companion object {
        const val TOPIC_ORDER_PLACED = "learning.order.placed"
        const val TOPIC_PAYMENT_COMPLETED = "learning.payment.completed"
    }

    /**
     * 기본 메시지 발행
     * - send()는 CompletableFuture를 반환
     * - .get()으로 동기 대기 또는 콜백 등록 가능
     */
    @PostMapping("/send/simple")
    suspend fun sendSimple(
        @RequestParam topic: String,
        @RequestParam key: String,
        @RequestParam message: String
    ): Map<String, Any> {
        val result: SendResult<String, String> = kafkaTemplate
            .send(topic, key, message)
            .get()

        val metadata = result.recordMetadata
        return mapOf(
            "topic" to metadata.topic(),
            "partition" to metadata.partition(),
            "offset" to metadata.offset(),
            "key" to key,
            "message" to message
        )
    }

    /**
     * 주문 이벤트 발행 (flash-sale 패턴)
     *
     * DomainEvent 형식:
     * - aggregateId: 파티셔닝 키 (같은 주문 → 같은 파티션 → 순서 보장)
     * - eventType: 이벤트 종류
     * - eventId: 멱등성 키 (중복 처리 방지)
     * - occurredAt: 발생 시각
     */
    @PostMapping("/send/order")
    suspend fun sendOrderEvent(@RequestBody request: OrderRequest): Map<String, Any> {
        val event = OrderPlacedEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = "order.placed",
            aggregateId = request.orderId,
            occurredAt = Instant.now().toString(),
            orderId = request.orderId,
            productId = request.productId,
            userId = request.userId,
            quantity = request.quantity,
            totalPrice = request.totalPrice
        )

        val json = objectMapper.writeValueAsString(event)

        // Key = orderId → 같은 주문의 이벤트는 같은 파티션
        val result = kafkaTemplate.send(TOPIC_ORDER_PLACED, event.aggregateId, json).get()

        return mapOf(
            "topic" to result.recordMetadata.topic(),
            "partition" to result.recordMetadata.partition(),
            "offset" to result.recordMetadata.offset(),
            "event" to event
        )
    }
}

// ============================
// Event DTO
// ============================

data class OrderRequest(
    val orderId: String,
    val productId: String,
    val userId: String,
    val quantity: Int,
    val totalPrice: Long
)

data class OrderPlacedEvent(
    val eventId: String,
    val eventType: String,
    val aggregateId: String,
    val occurredAt: String,
    val orderId: String,
    val productId: String,
    val userId: String,
    val quantity: Int,
    val totalPrice: Long
)
