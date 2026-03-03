package com.flashsale.learning.r2dbc

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * === 2. R2DBC Repository ===
 *
 * CoroutineCrudRepository: 코루틴 기반 CRUD 리포지토리
 * - 메서드가 suspend fun 또는 Flow<T> 반환
 * - JPA의 JpaRepository와 유사한 인터페이스
 *
 * 제공되는 기본 메서드:
 * - save(entity): T       → INSERT 또는 UPDATE
 * - findById(id): T?      → 단건 조회 (없으면 null)
 * - findAll(): Flow<T>    → 전체 조회 (스트리밍)
 * - deleteById(id)        → 삭제
 * - count(): Long         → 전체 개수
 * - existsById(id): Boolean → 존재 여부
 */
interface ProductRepository : CoroutineCrudRepository<ProductEntity, Long> {

    /**
     * 메서드 이름 기반 쿼리 자동 생성
     * findBy{PropertyName}
     */
    fun findByStatus(status: String): Flow<ProductEntity>

    fun findByNameContaining(keyword: String): Flow<ProductEntity>

    suspend fun findByName(name: String): ProductEntity?

    /**
     * @Query: 직접 SQL 작성
     * - R2DBC는 JPQL 미지원 → 네이티브 SQL 사용
     * - :param으로 파라미터 바인딩
     */
    @Query("SELECT * FROM products WHERE stock > :minStock ORDER BY price ASC")
    fun findAvailableProducts(minStock: Int): Flow<ProductEntity>

    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :id AND stock >= :quantity")
    suspend fun decreaseStock(id: Long, quantity: Int): Long // 영향받은 행 수
}

interface OrderRepository : CoroutineCrudRepository<OrderEntity, Long> {

    fun findByUserId(userId: String): Flow<OrderEntity>

    suspend fun findByOrderId(orderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE product_id = :productId AND status = :status")
    fun findByProductIdAndStatus(productId: Long, status: String): Flow<OrderEntity>
}
