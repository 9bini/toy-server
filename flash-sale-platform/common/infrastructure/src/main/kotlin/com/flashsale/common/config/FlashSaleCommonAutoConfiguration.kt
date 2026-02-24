package com.flashsale.common.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan

/**
 * common/infrastructure 모듈의 Spring 빈을 자동 등록하는 설정.
 *
 * 각 서비스의 @SpringBootApplication 기본 패키지(com.flashsale.{service})와
 * 다른 패키지(com.flashsale.common)에 위치한 빈들을 스캔한다.
 */
@AutoConfiguration
@ComponentScan(basePackages = ["com.flashsale.common"])
@EnableConfigurationProperties(TimeoutProperties::class)
class FlashSaleCommonAutoConfiguration
