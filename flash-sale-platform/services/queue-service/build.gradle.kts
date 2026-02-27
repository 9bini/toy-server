// 대기열 서비스: Redis Sorted Set + SSE 실시간 순번 알림
plugins {
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":common:infrastructure"))

    testImplementation(testFixtures(project(":common:infrastructure")))
}

// kotest-extensions-spring:1.3.0이 kotest 5.x API를 끌어와서 6.x와 충돌하므로 제외
configurations.testImplementation {
    exclude(group = "io.kotest", module = "kotest-framework-api")
}
configurations.testRuntimeOnly {
    exclude(group = "io.kotest", module = "kotest-framework-api")
}
