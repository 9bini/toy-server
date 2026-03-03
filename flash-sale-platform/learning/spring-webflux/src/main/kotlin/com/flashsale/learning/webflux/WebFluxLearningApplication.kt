package com.flashsale.learning.webflux

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * === Spring WebFlux 학습 애플리케이션 ===
 *
 * 실행: ./gradlew :learning:spring-webflux:bootRun
 * 테스트: curl http://localhost:8090/api/hello
 *
 * WebFlux = Spring의 리액티브 웹 프레임워크
 * - 논블로킹 I/O 기반 (Netty 서버)
 * - Kotlin Coroutines와 자연스럽게 통합
 * - flash-sale에서 모든 HTTP 엔드포인트가 WebFlux 기반
 */
@SpringBootApplication
class WebFluxLearningApplication

fun main(args: Array<String>) {
    runApplication<WebFluxLearningApplication>(*args)
}
