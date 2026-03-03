package com.flashsale.learning.testing

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot

/**
 * === 3. 서비스 단위 테스트 (flash-sale 패턴) ===
 *
 * 단위 테스트 전략:
 * - Service(UseCase) 테스트 = Port를 MockK으로 모킹
 * - 비즈니스 로직만 검증 (인프라 의존성 없이)
 * - 성공/실패 시나리오 모두 테스트
 *
 * 구조:
 * - describe("서비스/기능") → 테스트 그룹
 * - context("조건") → 시나리오
 * - it("기대 결과") → 개별 테스트
 */
class OrderServiceTest : DescribeSpec({

    // 모킹된 의존성
    val stockPort = mockk<StockPort>()
    val orderPersistencePort = mockk<OrderPersistencePort>()

    // 테스트 대상
    val orderService = OrderService(stockPort, orderPersistencePort)

    describe("placeOrder") {

        context("재고가 충분할 때") {
            it("주문이 성공적으로 생성된다") {
                // Given: 재고 100, 차감 성공, 저장 성공
                coEvery { stockPort.getStock("P-001") } returns 100
                coEvery { stockPort.decreaseStock("P-001", 2) } returns true

                val orderSlot = slot<Order>()
                coEvery { orderPersistencePort.save(capture(orderSlot)) } answers {
                    orderSlot.captured
                }

                // When
                val result = orderService.placeOrder("P-001", "U-001", 2, 10000)

                // Then
                result.shouldBeInstanceOf<OrderResult.Success<Order>>()
                val order = result.value
                order.productId shouldBe "P-001"
                order.userId shouldBe "U-001"
                order.quantity shouldBe 2
                order.totalPrice shouldBe 20000 // 10000 * 2
                order.status shouldBe OrderStatus.CREATED

                // 호출 검증
                coVerify(exactly = 1) { stockPort.getStock("P-001") }
                coVerify(exactly = 1) { stockPort.decreaseStock("P-001", 2) }
                coVerify(exactly = 1) { orderPersistencePort.save(any()) }
            }
        }

        context("재고가 부족할 때") {
            it("InsufficientStock 에러를 반환한다") {
                // Given: 재고 1개, 주문 수량 5개
                coEvery { stockPort.getStock("P-001") } returns 1

                // When
                val result = orderService.placeOrder("P-001", "U-001", 5, 10000)

                // Then
                result.shouldBeInstanceOf<OrderResult.Failure>()
                val error = result.error
                error.shouldBeInstanceOf<OrderError.InsufficientStock>()
                error.productId shouldBe "P-001"
                error.available shouldBe 1

                // 재고 차감은 호출되지 않아야 함
                coVerify(exactly = 0) { stockPort.decreaseStock(any(), any()) }
                coVerify(exactly = 0) { orderPersistencePort.save(any()) }
            }
        }

        context("재고 차감에 실패할 때 (동시성 경합)") {
            it("InsufficientStock 에러를 반환한다") {
                // Given: 재고 확인 시 1개 있지만, 차감 시점에 이미 소진
                coEvery { stockPort.getStock("P-001") } returns 1
                coEvery { stockPort.decreaseStock("P-001", 1) } returns false

                // When
                val result = orderService.placeOrder("P-001", "U-001", 1, 10000)

                // Then
                result.shouldBeInstanceOf<OrderResult.Failure>()

                // 주문 저장은 호출되지 않아야 함
                coVerify(exactly = 0) { orderPersistencePort.save(any()) }
            }
        }
    }

    describe("getOrder") {

        context("주문이 존재할 때") {
            it("주문을 반환한다") {
                val expectedOrder = Order("ORD-001", "P-001", "U-001", 1, 10000)
                coEvery { orderPersistencePort.findById("ORD-001") } returns expectedOrder

                val result = orderService.getOrder("ORD-001")

                result shouldBe expectedOrder
            }
        }

        context("주문이 존재하지 않을 때") {
            it("null을 반환한다") {
                coEvery { orderPersistencePort.findById("ORD-999") } returns null

                val result = orderService.getOrder("ORD-999")

                result shouldBe null
            }
        }
    }
})
