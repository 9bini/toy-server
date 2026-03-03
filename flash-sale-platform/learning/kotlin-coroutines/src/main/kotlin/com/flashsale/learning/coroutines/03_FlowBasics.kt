package com.flashsale.learning.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * === 3. Kotlin Flow ===
 *
 * Flow = 비동기 데이터 스트림 (Cold Stream)
 * - 컬렉션의 Sequence와 유사하지만 비동기 지원
 * - collect()가 호출될 때만 데이터 생성 시작 (Cold)
 *
 * flash-sale에서의 사용:
 * - SSE(Server-Sent Events)로 대기열 순번 실시간 전송
 * - Kafka 메시지 스트림 처리
 * - DB 대량 데이터 스트리밍 조회
 */

// ============================
// 3-1. Flow 생성
// ============================

/**
 * flow { } 빌더: 가장 기본적인 Flow 생성
 * emit()으로 값을 하나씩 방출
 */
fun simpleFlow(): Flow<Int> = flow {
    println("  [Flow] 데이터 생성 시작")
    for (i in 1..3) {
        delay(100) // 비동기 대기 가능
        emit(i)    // 값 방출
        println("  [Flow] $i 방출 완료")
    }
}

/**
 * 다양한 Flow 생성 방법
 */
fun flowCreation() {
    // 컬렉션 → Flow
    val fromList: Flow<Int> = listOf(1, 2, 3).asFlow()

    // 범위 → Flow
    val fromRange: Flow<Int> = (1..10).asFlow()

    // 단일 값 Flow
    val single: Flow<String> = flowOf("hello")

    // 빈 Flow
    val empty: Flow<Nothing> = emptyFlow()
}

// ============================
// 3-2. Flow 연산자 (Operators)
// ============================

suspend fun flowOperatorsExample() {
    println("--- map (변환) ---")
    (1..5).asFlow()
        .map { it * it } // 제곱
        .collect { println("  $it") }

    println("\n--- filter (필터링) ---")
    (1..10).asFlow()
        .filter { it % 2 == 0 } // 짝수만
        .collect { println("  $it") }

    println("\n--- take (제한) ---")
    (1..100).asFlow()
        .take(3) // 처음 3개만
        .collect { println("  $it") }

    println("\n--- transform (유연한 변환) ---")
    (1..3).asFlow()
        .transform { value ->
            emit("처리 시작: $value")
            delay(50)
            emit("처리 완료: ${value * 10}")
        }
        .collect { println("  $it") }

    println("\n--- reduce / fold (집계) ---")
    val sum = (1..5).asFlow()
        .reduce { acc, value -> acc + value }
    println("  합계: $sum")

    val csv = listOf("사과", "바나나", "딸기").asFlow()
        .fold("목록") { acc, value -> "$acc, $value" }
    println("  $csv")
}

// ============================
// 3-3. Flow 에러 처리
// ============================

suspend fun flowErrorHandling() {
    // catch 연산자: 업스트림 예외만 처리
    println("--- catch (에러 처리) ---")
    flow {
        emit(1)
        emit(2)
        throw RuntimeException("데이터 소스 오류!")
    }
        .catch { e -> println("  [catch] 에러 처리: ${e.message}") }
        .collect { println("  수신: $it") }

    // onCompletion: 완료/에러 시 콜백 (try-finally와 유사)
    println("\n--- onCompletion (완료 콜백) ---")
    (1..3).asFlow()
        .onCompletion { cause ->
            if (cause == null) println("  [완료] 정상 종료")
            else println("  [완료] 에러 종료: ${cause.message}")
        }
        .collect { println("  수신: $it") }

    // retry: 재시도
    println("\n--- retry (재시도) ---")
    var attempts = 0
    flow {
        attempts++
        if (attempts < 3) throw RuntimeException("시도 $attempts 실패")
        emit("성공 (시도 $attempts)")
    }
        .retry(3) { cause ->
            println("  [retry] 재시도: ${cause.message}")
            delay(100)
            true
        }
        .collect { println("  결과: $it") }
}

// ============================
// 3-4. SSE 스트리밍 패턴 (flash-sale 핵심)
// ============================

/**
 * 대기열 순번 실시간 스트리밍 시뮬레이션
 * → queue-service에서 SSE로 클라이언트에게 순번 전달
 */
data class QueuePosition(val userId: String, val position: Int, val estimatedWaitSec: Int)

fun queuePositionStream(userId: String): Flow<QueuePosition> = flow {
    var position = 150
    while (position > 0) {
        emit(QueuePosition(userId, position, position * 2))
        delay(500) // 0.5초마다 갱신
        position -= 10 // 10명씩 처리됨
    }
}.onCompletion { println("  [SSE] $userId 스트리밍 종료") }

suspend fun sseStreamingExample() {
    queuePositionStream("user-001")
        .take(5) // 5번만 수신 (데모)
        .collect { pos ->
            println("  [SSE] 순번: ${pos.position}, 예상 대기: ${pos.estimatedWaitSec}초")
        }
}

fun main() = runBlocking {
    println("=== 기본 Flow ===")
    simpleFlow().collect { println("  수신: $it") }

    println("\n=== Flow 연산자 ===")
    flowOperatorsExample()

    println("\n=== Flow 에러 처리 ===")
    flowErrorHandling()

    println("\n=== SSE 스트리밍 패턴 ===")
    sseStreamingExample()
}
