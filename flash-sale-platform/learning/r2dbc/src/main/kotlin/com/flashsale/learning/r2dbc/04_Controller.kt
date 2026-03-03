package com.flashsale.learning.r2dbc

import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * === 4. R2DBC + WebFlux Controller ===
 *
 * Repository → Service → Controller 전체 흐름을 확인할 수 있는 REST API
 */
@RestController
@RequestMapping("/api")
class ProductController(private val productService: ProductService) {

    @PostMapping("/products")
    suspend fun create(@RequestBody req: CreateProductRequest): ResponseEntity<ProductEntity> {
        val product = productService.createProduct(req.name, req.price, req.stock)
        return ResponseEntity.status(HttpStatus.CREATED).body(product)
    }

    @GetMapping("/products/{id}")
    suspend fun getById(@PathVariable id: Long): ResponseEntity<ProductEntity> {
        val product = productService.getProduct(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(product)
    }

    @GetMapping("/products")
    fun listAvailable(): Flow<ProductEntity> {
        return productService.getAvailableProducts()
    }

    @PostMapping("/orders")
    suspend fun order(@RequestBody req: PlaceOrderRequest): ResponseEntity<OrderEntity> {
        return try {
            val order = productService.placeOrder(req.productId, req.userId, req.quantity)
            ResponseEntity.status(HttpStatus.CREATED).body(order)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    @GetMapping("/users/{userId}/orders")
    suspend fun userOrders(@PathVariable userId: String): List<OrderEntity> {
        return productService.getUserOrders(userId)
    }
}

data class CreateProductRequest(val name: String, val price: Long, val stock: Int)
data class PlaceOrderRequest(val productId: Long, val userId: String, val quantity: Int)
