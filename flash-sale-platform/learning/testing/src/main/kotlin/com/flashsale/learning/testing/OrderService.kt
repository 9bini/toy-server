package com.flashsale.learning.testing

import org.springframework.stereotype.Service

/**
 * 테스트 대상 코드: 주문 서비스 (간략 버전)
 *
 * 테스트 모듈에서 이 코드를 다양한 방식으로 테스트하는 예제를 학습
 */

// --- 도메인 모델 ---

data class Order(
    val id: String,
    val productId: String,
    val userId: String,
    val quantity: Int,
    val totalPrice: Long,
    val status: OrderStatus = OrderStatus.CREATED
)

enum class OrderStatus { CREATED, CONFIRMED, CANCELLED }

sealed interface OrderError {
    data class InsufficientStock(val productId: String, val available: Int) : OrderError
    data class ProductNotFound(val productId: String) : OrderError
    data class DuplicateOrder(val orderId: String) : OrderError
}

sealed interface OrderResult<out T> {
    data class Success<T>(val value: T) : OrderResult<T>
    data class Failure(val error: OrderError) : OrderResult<Nothing>
}

// --- 포트 (인터페이스) ---

interface StockPort {
    suspend fun getStock(productId: String): Int
    suspend fun decreaseStock(productId: String, quantity: Int): Boolean
}

interface OrderPersistencePort {
    suspend fun save(order: Order): Order
    suspend fun findById(id: String): Order?
}

// --- 서비스 ---

@Service
class OrderService(
    private val stockPort: StockPort,
    private val orderPersistencePort: OrderPersistencePort
) {

    suspend fun placeOrder(
        productId: String,
        userId: String,
        quantity: Int,
        unitPrice: Long
    ): OrderResult<Order> {
        // 1. 재고 확인
        val stock = stockPort.getStock(productId)
        if (stock < quantity) {
            return OrderResult.Failure(
                OrderError.InsufficientStock(productId, stock)
            )
        }

        // 2. 재고 차감
        val decreased = stockPort.decreaseStock(productId, quantity)
        if (!decreased) {
            return OrderResult.Failure(
                OrderError.InsufficientStock(productId, stock)
            )
        }

        // 3. 주문 생성
        val order = Order(
            id = "ORD-${System.currentTimeMillis()}",
            productId = productId,
            userId = userId,
            quantity = quantity,
            totalPrice = unitPrice * quantity
        )

        return OrderResult.Success(orderPersistencePort.save(order))
    }

    suspend fun getOrder(orderId: String): Order? {
        return orderPersistencePort.findById(orderId)
    }
}
