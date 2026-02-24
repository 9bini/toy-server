---
name: kafka-setup
description: Kafka Producer/Consumer를 설정합니다. 토픽 생성, 직렬화, exactly-once, DLQ 설정을 포함합니다.
argument-hint: [topic-name] [producer|consumer]
---

$ARGUMENTS Kafka 구성을 설정하세요.

## 설정 항목

### 1. 토픽 정의
- 토픽명: `flashsale.{domain}.{event}` (예: `flashsale.order.confirmed`)
- 파티션 수: 서비스 인스턴스 수 기반 결정
- 복제 팩터: 개발환경 1, 운영 3
- 보존 기간: 이벤트별 설정

### 2. Producer 설정
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

### 3. Consumer 설정
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

### 4. Kotlin Coroutines 통합
- `ReactiveKafkaConsumerTemplate` 사용
- `Flow`로 메시지 스트림 처리
- 배압(backpressure) 제어

### 5. DLQ (Dead Letter Queue)
- DLQ 토픽: `{원본토픽}.dlq`
- 재시도 횟수: 3회
- 재시도 간격: exponential backoff

### 6. 코드 위치
- Config: `{service}/src/main/kotlin/.../config/KafkaConfig.kt`
- Producer: `{service}/src/main/kotlin/.../adapter/out/kafka/`
- Consumer: `{service}/src/main/kotlin/.../adapter/in/kafka/`
- 토픽 상수: `common/infrastructure/src/.../kafka/Topics.kt`

## 필수 사항
- 메시지 발행은 반드시 멱등성 키 포함
- Consumer는 멱등성 처리 (중복 메시지 안전)
- 통합 테스트는 Testcontainers Kafka 사용
