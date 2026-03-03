// 학습 모듈: Spring Kafka 이벤트 기반 비동기 메시징
plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("tools.jackson.module:jackson-module-kotlin")
}
