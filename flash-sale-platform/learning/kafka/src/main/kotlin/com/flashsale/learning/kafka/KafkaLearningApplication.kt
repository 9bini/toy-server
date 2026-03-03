package com.flashsale.learning.kafka

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * === Spring Kafka 학습 애플리케이션 ===
 *
 * 사전 조건: Kafka 실행 필요
 *   docker run -d --name kafka-learning -p 9093:9092 \
 *     -e KAFKA_CFG_NODE_ID=0 \
 *     -e KAFKA_CFG_PROCESS_ROLES=controller,broker \
 *     -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
 *     -e KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=0@kafka:9093 \
 *     bitnami/kafka:3.6
 *
 * 실행: ./gradlew :learning:kafka:bootRun
 *
 * Kafka 용도 (flash-sale):
 * - 서비스 간 비동기 이벤트 전달 (이벤트 드리븐 아키텍처)
 * - order.placed → payment-service가 결제 처리
 * - payment.completed → notification-service가 알림 전송
 * - DLQ(Dead Letter Queue)로 실패 메시지 격리
 */
@SpringBootApplication
class KafkaLearningApplication

fun main(args: Array<String>) {
    runApplication<KafkaLearningApplication>(*args)
}
