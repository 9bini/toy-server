package com.flashsale.learning.testing

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*

/**
 * === 2. MockK 기본 ===
 *
 * MockK = Kotlin 네이티브 모킹 프레임워크
 * - Mockito 대비 Kotlin에 최적화 (suspend fun, data class 등)
 * - every { } / coEvery { } 로 모의 동작 정의
 * - verify { } / coVerify { } 로 호출 검증
 *
 * flash-sale에서의 사용:
 * - UseCase 테스트 시 Output Port를 모킹
 * - Controller 테스트 시 UseCase를 모킹
 * - 외부 서비스 호출을 모킹 (결제 API 등)
 */
class MockKBasicsTest : DescribeSpec({

    describe("MockK 기본 사용법") {

        // ============================
        // coEvery / coVerify: suspend 함수 모킹
        // ============================

        describe("suspend 함수 모킹") {
            it("coEvery로 suspend 함수의 반환값을 지정한다") {
                val stockPort = mockk<StockPort>()

                // suspend 함수는 coEvery 사용 (일반 함수는 every)
                coEvery { stockPort.getStock("P-001") } returns 100
                coEvery { stockPort.decreaseStock("P-001", 1) } returns true

                // 호출
                val stock = stockPort.getStock("P-001")
                stockPort.decreaseStock("P-001", 1)

                // 검증
                stock shouldBe 100

                // coVerify로 호출 여부 확인
                coVerify(exactly = 1) { stockPort.getStock("P-001") }
                coVerify { stockPort.decreaseStock("P-001", 1) }
            }
        }

        // ============================
        // relaxed mock: 미정의 동작도 기본값 반환
        // ============================

        describe("relaxed mock") {
            it("설정하지 않은 메서드도 기본값을 반환한다") {
                val port = mockk<OrderPersistencePort>(relaxed = true)

                // coEvery를 정의하지 않아도 null 반환 (에러 발생 X)
                val result = port.findById("any-id")
                result shouldBe null
            }
        }

        // ============================
        // argument matchers
        // ============================

        describe("argument matchers") {
            it("any()로 임의의 인자를 매칭한다") {
                val stockPort = mockk<StockPort>()

                // 어떤 productId든 100 반환
                coEvery { stockPort.getStock(any()) } returns 100

                stockPort.getStock("P-001") shouldBe 100
                stockPort.getStock("P-999") shouldBe 100
            }

            it("특정 조건에 따라 다른 값을 반환한다") {
                val stockPort = mockk<StockPort>()

                coEvery { stockPort.getStock("P-001") } returns 100
                coEvery { stockPort.getStock("P-002") } returns 0

                stockPort.getStock("P-001") shouldBe 100
                stockPort.getStock("P-002") shouldBe 0
            }
        }

        // ============================
        // verify ordering
        // ============================

        describe("호출 순서 검증") {
            it("coVerifyOrder로 호출 순서를 검증한다") {
                val stockPort = mockk<StockPort>()

                coEvery { stockPort.getStock("P-001") } returns 100
                coEvery { stockPort.decreaseStock("P-001", 1) } returns true

                // 순서대로 호출
                stockPort.getStock("P-001")
                stockPort.decreaseStock("P-001", 1)

                // 순서 검증
                coVerifyOrder {
                    stockPort.getStock("P-001")
                    stockPort.decreaseStock("P-001", 1)
                }
            }
        }

        // ============================
        // slot: 인자 캡처
        // ============================

        describe("인자 캡처") {
            it("slot으로 전달된 인자를 캡처한다") {
                val orderPort = mockk<OrderPersistencePort>()
                val orderSlot = slot<Order>()

                coEvery { orderPort.save(capture(orderSlot)) } answers {
                    orderSlot.captured // 저장된 것을 그대로 반환
                }

                val order = Order("ORD-1", "P-1", "U-1", 2, 20000)
                orderPort.save(order)

                // 캡처된 인자 검증
                val captured = orderSlot.captured
                captured.productId shouldBe "P-1"
                captured.quantity shouldBe 2
                captured.totalPrice shouldBe 20000
            }
        }
    }
})
