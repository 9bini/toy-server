package com.flashsale.learning.coroutines

import kotlinx.coroutines.*
import org.slf4j.MDC

/**
 * === 4. Coroutine Context & Dispatcher ===
 *
 * CoroutineContext: 코루틴의 실행 환경을 정의하는 요소들의 집합
 * - Dispatcher: 어떤 스레드에서 실행할지
 * - Job: 코루틴의 생명주기 관리
 * - CoroutineName: 디버깅용 이름
 * - MDC: 로깅 추적 ID 전파 (flash-sale의 CoroutineMdc)
 */

// ============================
// 4-1. Dispatcher (스레드 풀)
// ============================

/**
 * Dispatchers.Default: CPU 집약 작업 (코어 수만큼 스레드)
 * - 예: 데이터 변환, 정렬, 계산
 *
 * Dispatchers.IO: I/O 작업 (기본 64개 스레드)
 * - 예: 파일 읽기, 블로킹 라이브러리 호출
 * - 주의: WebFlux에서는 대부분 불필요 (이미 논블로킹)
 *
 * Dispatchers.Unconfined: 호출한 스레드에서 시작, 재개 시 달라질 수 있음
 * - 거의 사용하지 않음
 */
suspend fun dispatcherExample() {
    coroutineScope {
        launch(Dispatchers.Default) {
            println("  [Default] 스레드: ${Thread.currentThread().name}")
        }
        launch(Dispatchers.IO) {
            println("  [IO] 스레드: ${Thread.currentThread().name}")
        }
        launch(Dispatchers.Unconfined) {
            println("  [Unconfined] 스레드: ${Thread.currentThread().name}")
        }
    }
}

// ============================
// 4-2. withContext (디스패처 전환)
// ============================

/**
 * withContext: 코루틴 내에서 디스패처를 일시적으로 변경
 * → 블로킹 코드를 호출해야 할 때 유용
 */
suspend fun withContextExample() {
    // 메인 스레드에서 시작
    println("  시작 스레드: ${Thread.currentThread().name}")

    // CPU 집약 작업을 Default로 전환
    val result = withContext(Dispatchers.Default) {
        println("  계산 스레드: ${Thread.currentThread().name}")
        (1..1_000_000).sum()
    }
    println("  결과: $result, 복귀 스레드: ${Thread.currentThread().name}")

    // 블로킹 I/O를 IO로 전환
    val data = withContext(Dispatchers.IO) {
        println("  I/O 스레드: ${Thread.currentThread().name}")
        Thread.sleep(10) // 블로킹 호출 (예: 레거시 라이브러리)
        "파일 데이터"
    }
    println("  데이터: $data")
}

// ============================
// 4-3. CoroutineName (디버깅)
// ============================

/**
 * 코루틴에 이름을 부여하면 로그에서 추적이 쉬움
 * -Dkotlinx.coroutines.debug JVM 옵션과 함께 사용
 */
suspend fun coroutineNameExample() {
    coroutineScope {
        launch(CoroutineName("주문처리")) {
            println("  [${coroutineContext[CoroutineName]?.name}] 실행 중")
        }
        launch(CoroutineName("재고확인")) {
            println("  [${coroutineContext[CoroutineName]?.name}] 실행 중")
        }
    }
}

// ============================
// 4-4. Context 조합 (+)
// ============================

/**
 * CoroutineContext는 + 연산자로 조합 가능
 */
suspend fun contextCombinationExample() {
    coroutineScope {
        val context = Dispatchers.Default + CoroutineName("결제처리")

        launch(context) {
            println("  이름: ${coroutineContext[CoroutineName]?.name}")
            println("  스레드: ${Thread.currentThread().name}")
        }
    }
}

// ============================
// 4-5. MDC 전파 (로깅 추적)
// ============================

/**
 * flash-sale에서는 요청 추적 ID(traceId)를 MDC에 저장하고
 * 코루틴 간 전파해야 함 → CoroutineMdc 유틸리티 사용
 *
 * 문제: 코루틴은 스레드를 넘나들기 때문에 ThreadLocal 기반 MDC가 유실됨
 * 해결: MDCContext를 코루틴 컨텍스트에 추가
 */
suspend fun mdcPropagationExample() {
    // MDC에 추적 ID 설정
    MDC.put("traceId", "req-abc-123")

    coroutineScope {
        // MDC 컨텍스트 없이 실행 → traceId 유실
        launch(Dispatchers.Default) {
            println("  [MDC 없음] traceId = ${MDC.get("traceId")}") // null
        }

        delay(50)

        // kotlinx-coroutines-slf4j의 MDCContext 사용 → traceId 유지
        // (실제 프로젝트에서는 common:infrastructure의 CoroutineMdc 사용)
        // launch(Dispatchers.Default + MDCContext()) {
        //     println("  [MDC 있음] traceId = ${MDC.get("traceId")}") // req-abc-123
        // }
        println("  → 실제 프로젝트에서는 CoroutineMdc.withMdc() 사용")
    }

    MDC.clear()
}

fun main() = runBlocking {
    println("=== Dispatcher ===")
    dispatcherExample()

    println("\n=== withContext ===")
    withContextExample()

    println("\n=== CoroutineName ===")
    coroutineNameExample()

    println("\n=== Context 조합 ===")
    contextCombinationExample()

    println("\n=== MDC 전파 ===")
    mdcPropagationExample()
}
