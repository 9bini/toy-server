---
name: kafka-setup
description: Sets up Kafka Producer/Consumer. Includes topic creation, serialization, exactly-once, and DLQ configuration.
argument-hint: [topic-name] [producer|consumer]
---

$ARGUMENTS Set up the Kafka configuration.

## Configuration Items

### 1. Topic Definition
- Topic name: `flashsale.{domain}.{event}` (e.g., `flashsale.order.confirmed`)
- Partition count: Determined based on service instance count
- Replication factor: 1 for development, 3 for production
- Retention period: Configured per event

### 2. Producer Configuration
```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5
```

### 3. Consumer Configuration
```yaml
spring:
  kafka:
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        isolation.level: read_committed
```

### 4. Kotlin Coroutines Integration
- Use `ReactiveKafkaConsumerTemplate`
- Process message streams with `Flow`
- Backpressure control

### 5. DLQ (Dead Letter Queue)
- DLQ topic: `{original-topic}.dlq`
- Retry count: 3 times
- Retry interval: exponential backoff

### 6. Code Location
- Config: `{service}/src/main/kotlin/.../config/KafkaConfig.kt`
- Producer: `{service}/src/main/kotlin/.../adapter/out/kafka/`
- Consumer: `{service}/src/main/kotlin/.../adapter/in/kafka/`
- Topic constants: `common/infrastructure/src/.../kafka/Topics.kt`

## Required
- Message publishing must include an idempotency key
- Consumer must handle idempotently (safe against duplicate messages)
- Integration tests must use Testcontainers Kafka
