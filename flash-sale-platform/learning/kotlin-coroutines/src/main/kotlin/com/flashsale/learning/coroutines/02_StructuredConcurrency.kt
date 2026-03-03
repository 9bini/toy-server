package com.flashsale.learning.coroutines

import kotlinx.coroutines.*

/**
 * === 2. Structured Concurrency (구조화된 동시성) ===
 *
 * 핵심 원칙:
 * - 부모 코루틴은 모든 자식 코루틴이 완료될 때까지 종료하지 않음
 * - 자식에서 예외 발생 시 부모와 형제 코루틴도 취소됨
 * - GlobalScope 사용 금지 (flash-sale 규칙)
 *
 * flash-sale에서의 사용:
 * - 주문 처리 시 재고 차감 + 주문 생성을 구조화된 스코프 내에서 실행
 * - 하나라도 실패하면 전체 트랜잭션을 롤백
 */

// ============================
// 2-1. coroutineScope
// ============================

/**
 * coroutineScope: 자식 중 하나라도 실패하면 전체 취소
 * → 모든 작업이 성공해야 하는 경우에 사용
 * → 예: 주문 생성 (재고 차감 + 주문 저장 + 이벤트 발행)
 */
suspend fun coroutineScopeExample() {
    try {
        coroutineScope {
            val stockJob = launch {
                println("  [재고] 차감 시작")
                delay(100)
                println("  [재고] 차감 완료")
            }

            val orderJob = launch {
                println("  [주문] 생성 시작")
                delay(50)
                throw RuntimeException("DB 연결 실패!")
                // 이 예외로 인해 stockJob도 취소됨
            }
        }
    } catch (e: RuntimeException) {
        println("  [에러] 전체 작업 취소됨: ${e.message}")
    }
}

// ============================
// 2-2. supervisorScope
// ============================

/**
 * supervisorScope: 자식 실패가 형제에게 전파되지 않음
 * → 독립적인 작업들을 병렬 실행할 때 사용
 * → 예: 알림 전송 (이메일 실패해도 푸시 알림은 계속)
 */
suspend fun supervisorScopeExample() {
    supervisorScope {
        val emailJob = launch {
            println("  [이메일] 발송 시작")
            delay(50)
            throw RuntimeException("SMTP 서버 오류")
        }

        val pushJob = launch {
            println("  [푸시] 발송 시작")
            delay(100)
            println("  [푸시] 발송 완료") // 이메일 실패와 무관하게 실행됨
        }

        // 각 Job의 예외를 개별 처리
        emailJob.invokeOnCompletion { cause ->
            if (cause != null) println("  [이메일] 실패: ${cause.message}")
        }
    }
}

// ============================
// 2-3. 취소 처리 (Cancellation)
// ============================

/**
 * 코루틴 취소는 협력적(cooperative)임
 * - delay(), yield() 등 suspend 지점에서 취소 확인
 * - 긴 연산 시 isActive 체크 필요
 */
suspend fun cancellationExample() {
    coroutineScope {
        val job = launch {
            repeat(100) { i ->
                if (!isActive) return@launch // 취소 확인
                println("  작업 $i 진행 중...")
                delay(50)
            }
        }

        delay(170) // 3번 정도 실행 후
        println("  → 코루틴 취소 요청")
        job.cancelAndJoin()
        println("  → 코루틴 취소 완료")
    }
}

// ============================
// 2-4. withTimeout
// ============================

/**
 * withTimeout: 지정 시간 내에 완료되지 않으면 TimeoutCancellationException
 * → flash-sale에서 모든 외부 호출에 timeout 필수!
 *
 * 타임아웃 기준 (flash-sale 기준):
 * - Redis 조작: 100ms
 * - DB 트랜잭션: 5000ms
 * - 결제 API: 3000ms
 * - Kafka 전송: 5000ms
 */
suspend fun withTimeoutExample() {
    // withTimeout: 시간 초과 시 예외 발생
    try {
        withTimeout(200) {
            println("  [Redis] 재고 조회 시작")
            delay(500) // 시간 초과!
            println("  이 줄은 실행되지 않음")
        }
    } catch (e: TimeoutCancellationException) {
        println("  [Redis] 타임아웃! 100ms 초과")
    }

    // withTimeoutOrNull: 시간 초과 시 null 반환 (예외 없음)
    val result = withTimeoutOrNull(100) {
        delay(50)
        "성공"
    }
    println("  [결과] ${result ?: "타임아웃"}")
}

// ============================
// 2-5. CoroutineExceptionHandler
// ============================

/**
 * 최상위 코루틴에서 처리되지 않은 예외를 잡는 핸들러
 * → 로깅, 모니터링 알림 등에 활용
 */
suspend fun exceptionHandlerExample() {
    val handler = CoroutineExceptionHandler { _, exception ->
        println("  [ExceptionHandler] 처리되지 않은 예외: ${exception.message}")
    }

    // supervisorScope + handler 조합
    val scope = CoroutineScope(SupervisorJob() + handler)

    scope.launch {
        throw RuntimeException("예상치 못한 오류")
    }

    delay(100) // 핸들러 실행 대기
    scope.cancel()
}

fun main() = runBlocking {
    println("=== coroutineScope (하나 실패 → 전체 취소) ===")
    coroutineScopeExample()

    println("\n=== supervisorScope (독립적 실행) ===")
    supervisorScopeExample()

    println("\n=== 코루틴 취소 ===")
    cancellationExample()

    println("\n=== withTimeout ===")
    withTimeoutExample()

    println("\n=== CoroutineExceptionHandler ===")
    exceptionHandlerExample()
}
