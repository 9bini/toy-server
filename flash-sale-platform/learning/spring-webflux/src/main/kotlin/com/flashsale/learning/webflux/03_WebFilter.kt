package com.flashsale.learning.webflux

import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange
import java.util.UUID

/**
 * === 3. WebFilter (요청/응답 인터셉터) ===
 *
 * CoWebFilter = 코루틴 기반 WebFilter
 * - 모든 요청에 대해 전/후처리 수행
 * - Servlet의 Filter와 유사하지만 논블로킹
 *
 * flash-sale에서의 사용:
 * - RequestTracingFilter: traceId 생성 + MDC 설정
 * - Rate Limiting: 요청 제한 검사
 */
@Component
@Order(1)
class RequestTracingFilter : CoWebFilter() {

    /**
     * 모든 요청에 traceId를 부여하고 응답 헤더에 포함
     */
    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        val traceId = exchange.request.headers.getFirst("X-Trace-Id")
            ?: UUID.randomUUID().toString().substring(0, 8)

        // 응답 헤더에 traceId 추가
        exchange.response.headers.add("X-Trace-Id", traceId)

        // 요청 로깅
        val request = exchange.request
        println("[TRACE:$traceId] ${request.method} ${request.path}")

        val startTime = System.currentTimeMillis()

        // 다음 필터 또는 컨트롤러로 전달
        chain.filter(exchange)

        // 응답 로깅
        val duration = System.currentTimeMillis() - startTime
        println("[TRACE:$traceId] 응답 시간: ${duration}ms")
    }
}
