package com.flashsale.learning.r2dbc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * === Spring Data R2DBC 학습 애플리케이션 ===
 *
 * 사전 조건: PostgreSQL 실행 필요
 *   docker run -d --name pg-learning -p 5433:5432 \
 *     -e POSTGRES_DB=learning -e POSTGRES_USER=user -e POSTGRES_PASSWORD=pass \
 *     postgres:15
 *
 * 실행: ./gradlew :learning:r2dbc:bootRun
 *
 * R2DBC = Reactive Relational Database Connectivity
 * - JDBC의 리액티브 버전 (논블로킹 DB 접근)
 * - Spring Data R2DBC = R2DBC + Spring Data 추상화
 * - flash-sale의 order-service, payment-service에서 사용
 */
@SpringBootApplication
class R2dbcLearningApplication

fun main(args: Array<String>) {
    runApplication<R2dbcLearningApplication>(*args)
}
