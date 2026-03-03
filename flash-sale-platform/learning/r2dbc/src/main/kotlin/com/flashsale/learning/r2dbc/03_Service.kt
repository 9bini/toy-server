package com.flashsale.learning.r2dbc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * === 3. 트랜잭션 관리 ===
 *
 * @Transactional: Spring의 선언적 트랜잭션
 * - R2DBC에서도 동일하게 작동
 * - suspend fun에 적용 가능 (Spring 6+)
 * - 예외 발생 시 자동 롤백
 *
 * flash-sale에서의 주의사항:
 * - Redis 작업은 트랜잭션에 포함되지 않음
 * - Kafka 발행도 트랜잭션 외부에서 수행해야 함
 * - DB 작업만 @Transactional로 묶기
 */
@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository
) {

    // ============================
    // 기본 CRUD
    // ============================

    suspend fun createProduct(name: String, price: Long, stock: Int): ProductEntity {
        val product = ProductEntity(name = name, price = price, stock = stock)
        return productRepository.save(product)
    }

    suspend fun getProduct(id: Long): ProductEntity? {
        return productRepository.findById(id)
    }

    fun getAvailableProducts(): Flow<ProductEntity> {
        return productRepository.findAvailableProducts(minStock = 0)
    }

    // ============================
    // 트랜잭션 예제
    // ============================

    /**
     * 주문 생성: 재고 차감 + 주문 저장을 하나의 트랜잭션으로
     * → 재고 차감 실패 시 주문 저장도 롤백
     */
    @Transactional
    suspend fun placeOrder(productId: Long, userId: String, quantity: Int): OrderEntity {
        // 1. 상품 조회
        val product = productRepository.findById(productId)
            ?: throw IllegalArgumentException("상품이 존재하지 않습니다: $productId")

        // 2. 재고 차감 (원자적 UPDATE)
        val updatedRows = productRepository.decreaseStock(productId, quantity)
        if (updatedRows == 0L) {
            throw IllegalStateException("재고가 부족합니다: ${product.name}")
        }

        // 3. 주문 생성
        val order = OrderEntity(
            orderId = "ORD-${System.currentTimeMillis()}",
            productId = productId,
            userId = userId,
            quantity = quantity,
            totalPrice = product.price * quantity
        )
        return orderRepository.save(order)
    }

    /**
     * readOnly = true: 읽기 전용 트랜잭션
     * → DB가 읽기 최적화 수행 가능 (일부 DB에서)
     */
    @Transactional(readOnly = true)
    suspend fun getUserOrders(userId: String): List<OrderEntity> {
        return orderRepository.findByUserId(userId).toList()
    }
}
