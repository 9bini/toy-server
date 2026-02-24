# Flash Sale Platform (실시간 선착순 한정판매 시스템)

## 프로젝트 개요
10만 동시 접속, 1,000개 한정 상품 선착순 구매 시스템.
Kotlin + Spring WebFlux + Coroutines 기반 마이크로서비스 아키텍처.

## 아키텍처
- **gateway**: API Gateway + Rate Limiting (Redis Token Bucket)
- **queue-service**: 대기열 관리 (Redis Sorted Set + SSE)
- **order-service**: 주문 처리 (Redis Lua Script + Redisson 분산 락)
- **payment-service**: 결제 + Saga 패턴 보상 트랜잭션
- **notification-service**: 알림 (SSE + Push + 외부 API)
- **common/domain**: 공유 도메인 모델
- **common/infrastructure**: 공유 인프라 (Redis, Kafka 설정)

## 빌드 & 실행
- 전체 빌드: `./gradlew build`
- 특정 서비스: `./gradlew :services:order-service:build`
- 전체 테스트: `./gradlew test`
- 특정 테스트: `./gradlew :services:order-service:test --tests "*.OrderServiceTest"`
- 인프라 실행: `docker compose up -d`
- 인프라 종료: `docker compose down`
- 린트 체크: `./gradlew ktlintCheck`
- 린트 포맷: `./gradlew ktlintFormat`

## 코드 컨벤션
- Kotlin 코드 스타일: ktlint (공식 Kotlin 스타일 가이드)
- 모든 I/O 함수는 `suspend fun` 사용, blocking 코드 금지
- `coroutineScope` / `supervisorScope` 적절히 사용
- Redis 연산은 반드시 Lua Script 또는 Redisson으로 원자성 보장
- Kafka 메시지는 반드시 멱등성 처리
- 모든 외부 통신은 `withTimeout` 설정 필수
- sealed class / sealed interface로 에러 타입 정의
- 문서와 주석은 한국어, 코드(변수명/클래스명/함수명)는 영어

## 패키지 구조 (각 서비스)
```
com.flashsale.{service-name}/
├── adapter/
│   ├── in/web/        # Controller (WebFlux)
│   └── out/           # External adapters (Redis, Kafka, DB)
├── application/
│   ├── port/in/       # Use case interfaces
│   └── port/out/      # Output port interfaces
├── domain/            # Domain entities, value objects
└── config/            # Spring configuration
```

## Git 워크플로우
- 커밋 메시지: conventional commits (feat:, fix:, refactor:, test:, docs:, perf:)
- 한국어 커밋 메시지 허용 (예: `feat: 주문 서비스 재고 차감 로직 구현`)
- 브랜치: feature/{service-name}/{feature-description}

## IMPORTANT
- 테스트 작성 후 반드시 실행하여 통과 확인
- Redis/Kafka 연동 코드는 반드시 통합 테스트 작성 (Testcontainers 사용)
- 성능에 영향을 주는 변경은 벤치마크 실행 권장
- docker compose 인프라가 실행 중인지 확인 후 통합 테스트 수행
