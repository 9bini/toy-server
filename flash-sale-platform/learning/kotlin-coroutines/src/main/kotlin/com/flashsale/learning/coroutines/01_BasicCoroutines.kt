package com.flashsale.learning.coroutines

import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

/**
 * === 1. Coroutine 기본 개념 ===
 *
 * Coroutine = 경량 스레드 (Light-weight thread)
 * - 스레드를 블로킹하지 않고 일시 중단(suspend) 가능
 * - 수만 개의 코루틴을 동시에 실행해도 메모리 부담이 적음
 * - flash-sale-platform에서는 모든 I/O 작업이 코루틴 기반
 */

// ============================
// 1-1. launch vs async
// ============================

/**
 * launch: 결과를 반환하지 않는 코루틴 (fire-and-forget)
 * - Job을 반환 → join()으로 완료 대기 가능
 * - 예: 로그 기록, 이벤트 발행, 알림 전송
 */
suspend fun launchExample() {
    coroutineScope {
        val job: Job = launch {
            println("[launch] 작업 시작")
            delay(100) // 비동기 대기 (스레드를 블로킹하지 않음)
            println("[launch] 작업 완료")
        }
        job.join() // 코루틴이 끝날 때까지 대기
        println("[launch] join 이후")
    }
}

/**
 * async: 결과를 반환하는 코루틴
 * - Deferred<T>를 반환 → await()으로 결과 수신
 * - 예: API 호출 결과, DB 조회 결과
 */
suspend fun asyncExample() {
    coroutineScope {
        val deferred: Deferred<Int> = async {
            println("[async] 계산 시작")
            delay(100)
            42 // 반환값
        }
        val result = deferred.await()
        println("[async] 결과: $result")
    }
}

// ============================
// 1-2. 순차 실행 vs 병렬 실행
// ============================

suspend fun fetchUserName(): String {
    delay(200) // DB 조회 시뮬레이션
    return "홍길동"
}

suspend fun fetchUserScore(): Int {
    delay(300) // Redis 조회 시뮬레이션
    return 95
}

/**
 * 순차 실행: 총 500ms (200 + 300)
 */
suspend fun sequentialExecution() {
    val time = measureTimeMillis {
        val name = fetchUserName()   // 200ms 대기
        val score = fetchUserScore() // 300ms 대기
        println("$name: $score점")
    }
    println("순차 실행 시간: ${time}ms") // ~500ms
}

/**
 * 병렬 실행: 총 300ms (두 작업 동시 실행, 더 느린 쪽에 맞춤)
 * → flash-sale에서 여러 서비스 호출 시 이 패턴을 사용
 */
suspend fun parallelExecution() {
    val time = measureTimeMillis {
        coroutineScope {
            val name = async { fetchUserName() }
            val score = async { fetchUserScore() }
            println("${name.await()}: ${score.await()}점")
        }
    }
    println("병렬 실행 시간: ${time}ms") // ~300ms
}

// ============================
// 1-3. suspend 함수
// ============================

/**
 * suspend 함수: 코루틴 안에서만 호출 가능한 함수
 * - 일시 중단(suspend) 지점을 포함할 수 있음
 * - flash-sale의 모든 서비스 계층은 suspend fun으로 작성됨
 *
 * 규칙:
 * - I/O 작업은 반드시 suspend fun으로
 * - blocking 코드 사용 금지 (Thread.sleep, blocking I/O 등)
 */
suspend fun processOrder(orderId: String): String {
    // 각 단계가 비동기적으로 실행됨
    val stock = checkStock(orderId)     // Redis 조회
    val order = createOrder(orderId)    // DB 저장
    publishEvent(orderId)                // Kafka 발행
    return "주문 $orderId 처리 완료 (재고: $stock, 주문번호: $order)"
}

private suspend fun checkStock(orderId: String): Int {
    delay(10) // Redis 조회 시뮬레이션
    return 100
}

private suspend fun createOrder(orderId: String): String {
    delay(20) // DB 저장 시뮬레이션
    return "ORD-$orderId"
}

private suspend fun publishEvent(orderId: String) {
    delay(5) // Kafka 발행 시뮬레이션
}

// ============================
// main: 실행 진입점
// ============================
fun main() = runBlocking {
    println("=== launch vs async ===")
    launchExample()
    asyncExample()

    println("\n=== 순차 vs 병렬 실행 ===")
    sequentialExecution()
    parallelExecution()

    println("\n=== suspend 함수 체이닝 ===")
    println(processOrder("12345"))
}
