package com.flashsale.learning.webflux

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * === 1. Coroutine 기반 Controller ===
 *
 * WebFlux + Coroutines 핵심 규칙:
 * - Controller 메서드는 suspend fun으로 선언
 * - 반환 타입에 Mono/Flux 대신 일반 타입 또는 Flow 사용
 * - Spring이 자동으로 코루틴 ↔ 리액티브 변환 처리
 */
@RestController
@RequestMapping("/api")
class CoroutineController {

    // ============================
    // 기본 GET 엔드포인트
    // ============================

    /**
     * suspend fun = 비동기 처리
     * 반환 타입이 그대로 JSON 응답 본문이 됨
     */
    @GetMapping("/hello")
    suspend fun hello(): Map<String, String> {
        delay(10) // 비동기 작업 시뮬레이션
        return mapOf(
            "message" to "Hello, WebFlux + Coroutines!",
            "timestamp" to Instant.now().toString()
        )
    }

    // ============================
    // Path Variable & Query Parameter
    // ============================

    @GetMapping("/products/{id}")
    suspend fun getProduct(
        @PathVariable id: String,
        @RequestParam(defaultValue = "false") detail: Boolean
    ): ProductResponse {
        delay(20) // DB 조회 시뮬레이션
        return ProductResponse(
            id = id,
            name = "한정판 스니커즈",
            price = 199_000,
            stock = 100,
            detail = if (detail) "한정 100족 / 선착순 판매" else null
        )
    }

    // ============================
    // POST with Request Body
    // ============================

    /**
     * @RequestBody로 JSON → DTO 자동 변환 (Jackson)
     * 응답 상태 코드는 ResponseEntity로 제어
     */
    @PostMapping("/orders")
    suspend fun createOrder(
        @RequestBody request: CreateOrderRequest
    ): ResponseEntity<OrderResponse> {
        delay(50) // 주문 처리 시뮬레이션

        val order = OrderResponse(
            orderId = "ORD-${System.currentTimeMillis()}",
            productId = request.productId,
            quantity = request.quantity,
            status = "CREATED"
        )

        // 201 Created + Location 헤더
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header("Location", "/api/orders/${order.orderId}")
            .body(order)
    }

    // ============================
    // SSE (Server-Sent Events) 스트리밍
    // ============================

    /**
     * Flow<T>를 반환하면 SSE 스트리밍으로 자동 변환
     * → flash-sale의 대기열 순번 실시간 전송에 사용
     *
     * MediaType.TEXT_EVENT_STREAM_VALUE = "text/event-stream"
     */
    @GetMapping("/queue/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun queueStream(@RequestParam userId: String): Flow<QueueEvent> = flow {
        var position = 50
        while (position > 0) {
            emit(QueueEvent(userId, position, "WAITING"))
            delay(1000) // 1초마다 갱신
            position -= 5
        }
        emit(QueueEvent(userId, 0, "YOUR_TURN"))
    }

    // ============================
    // 예외 처리
    // ============================

    @GetMapping("/products/{id}/stock")
    suspend fun checkStock(@PathVariable id: String): StockResponse {
        delay(10)
        if (id == "not-found") {
            throw ProductNotFoundException(id)
        }
        return StockResponse(id, remaining = 42)
    }
}

// ============================
// DTO 정의
// ============================

data class ProductResponse(
    val id: String,
    val name: String,
    val price: Long,
    val stock: Int,
    val detail: String? = null
)

data class CreateOrderRequest(
    val productId: String,
    val quantity: Int
)

data class OrderResponse(
    val orderId: String,
    val productId: String,
    val quantity: Int,
    val status: String
)

data class QueueEvent(
    val userId: String,
    val position: Int,
    val status: String
)

data class StockResponse(
    val productId: String,
    val remaining: Int
)

class ProductNotFoundException(val productId: String) :
    RuntimeException("상품을 찾을 수 없습니다: $productId")
