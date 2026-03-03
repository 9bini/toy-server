package com.flashsale.learning.r2dbc

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * === 1. R2DBC Entity 정의 ===
 *
 * JPA와의 차이점:
 * - @Entity 대신 @Table 사용
 * - 연관관계(OneToMany 등) 미지원 → 직접 JOIN 쿼리 작성
 * - data class 사용 권장 (불변 객체)
 * - @Id가 null이면 INSERT, 값이 있으면 UPDATE
 *
 * flash-sale 컨벤션:
 * - ID는 ULID 기반 String (IdGenerator 사용)
 * - createdAt, updatedAt 필수
 * - 도메인 모델과 DB 엔티티 분리 (Hexagonal)
 */
@Table("products")
data class ProductEntity(
    @Id
    val id: Long? = null, // null이면 자동 생성 (INSERT)

    @Column("name")
    val name: String,

    @Column("price")
    val price: Long,

    @Column("stock")
    val stock: Int,

    @Column("status")
    val status: String = "ACTIVE",

    @CreatedDate
    @Column("created_at")
    val createdAt: Instant? = null,

    @LastModifiedDate
    @Column("updated_at")
    val updatedAt: Instant? = null
)

/**
 * 주문 엔티티 (flash-sale의 order-service 패턴)
 */
@Table("orders")
data class OrderEntity(
    @Id
    val id: Long? = null,

    @Column("order_id")
    val orderId: String, // ULID 기반 비즈니스 ID

    @Column("product_id")
    val productId: Long,

    @Column("user_id")
    val userId: String,

    @Column("quantity")
    val quantity: Int,

    @Column("total_price")
    val totalPrice: Long,

    @Column("status")
    val status: String = "CREATED",

    @CreatedDate
    @Column("created_at")
    val createdAt: Instant? = null
)
