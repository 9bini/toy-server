package com.flashsale.learning.r2dbc

import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing

/**
 * R2DBC Auditing 활성화
 * → @CreatedDate, @LastModifiedDate가 자동으로 값이 채워짐
 */
@Configuration
@EnableR2dbcAuditing
class R2dbcConfig
