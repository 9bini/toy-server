package com.flashsale.learning.kafka

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * === 3. Consumer 상태 조회 ===
 *
 * 학습용: Consumer가 처리한 이벤트 이력을 조회
 */
@RestController
@RequestMapping("/api/kafka/consumer")
class ConsumerStatusController(
    private val orderEventConsumer: OrderEventConsumer
) {

    @GetMapping("/orders")
    fun getProcessedOrders(): Map<String, Any> {
        return mapOf(
            "count" to orderEventConsumer.processedOrders.size,
            "orders" to orderEventConsumer.processedOrders
        )
    }
}
