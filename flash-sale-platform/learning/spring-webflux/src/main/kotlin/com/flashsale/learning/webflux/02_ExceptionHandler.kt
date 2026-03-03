package com.flashsale.learning.webflux

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

/**
 * === 2. 글로벌 예외 처리 ===
 *
 * @RestControllerAdvice = 모든 Controller에 적용되는 예외 핸들러
 * → flash-sale에서는 도메인 에러(sealed interface)를 HTTP 응답으로 매핑
 *
 * flash-sale 에러 응답 형식:
 * {
 *   "code": "PRODUCT_NOT_FOUND",
 *   "message": "상품을 찾을 수 없습니다",
 *   "timestamp": "2024-01-01T00:00:00Z"
 * }
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * 특정 예외를 특정 HTTP 상태 코드로 매핑
     */
    @ExceptionHandler(ProductNotFoundException::class)
    suspend fun handleProductNotFound(
        e: ProductNotFoundException
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    code = "PRODUCT_NOT_FOUND",
                    message = e.message ?: "상품을 찾을 수 없습니다",
                    timestamp = Instant.now().toString()
                )
            )
    }

    /**
     * 처리되지 않은 예외 → 500 Internal Server Error
     */
    @ExceptionHandler(Exception::class)
    suspend fun handleUnexpected(
        e: Exception
    ): ResponseEntity<ErrorResponse> {
        // 실제로는 로그 기록 필수
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    code = "INTERNAL_ERROR",
                    message = "서버 내부 오류가 발생했습니다",
                    timestamp = Instant.now().toString()
                )
            )
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: String
)
