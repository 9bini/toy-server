package com.flashsale.learning.testing

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * === 1. Kotest 기본 ===
 *
 * Kotest = Kotlin 네이티브 테스트 프레임워크
 * - JUnit5 대체 (더 풍부한 표현력)
 * - 다양한 Spec 스타일 제공
 * - Coroutine 네이티브 지원
 * - Property-based Testing 지원
 *
 * flash-sale에서는 Kotest + MockK 조합으로 모든 테스트 작성
 */

// ============================
// Style 1: StringSpec (가장 간단)
// ============================

/**
 * StringSpec: 문자열로 테스트 이름을 지정
 * 간단한 단위 테스트에 적합
 */
class OrderStatusTest : StringSpec({

    "OrderStatus는 3가지 상태를 가진다" {
        OrderStatus.entries shouldHaveSize 3
    }

    "CREATED가 기본 상태이다" {
        val order = Order("1", "P-1", "U-1", 1, 10000)
        order.status shouldBe OrderStatus.CREATED
    }
})

// ============================
// Style 2: FunSpec (JUnit5와 유사)
// ============================

/**
 * FunSpec: test("이름") 블록으로 테스트 정의
 * JUnit에 익숙한 사람에게 적합
 */
class OrderTest : FunSpec({

    test("주문 생성 시 총 가격이 올바르게 계산되어야 한다") {
        val order = Order(
            id = "ORD-001",
            productId = "P-001",
            userId = "U-001",
            quantity = 3,
            totalPrice = 30000
        )

        order.totalPrice shouldBe 30000
        order.quantity shouldBe 3
    }

    test("주문 ID는 빈 문자열이 아니어야 한다") {
        val order = Order("ORD-001", "P-001", "U-001", 1, 10000)
        order.id shouldNotBe ""
        order.id shouldStartWith "ORD"
    }
})

// ============================
// Style 3: DescribeSpec (RSpec 스타일)
// ============================

/**
 * DescribeSpec: describe/it 구조로 테스트를 계층적으로 구성
 * flash-sale에서 가장 많이 사용하는 스타일
 */
class OrderErrorTest : DescribeSpec({

    describe("OrderError") {
        describe("InsufficientStock") {
            it("상품 ID와 가용 재고를 포함한다") {
                val error = OrderError.InsufficientStock("P-001", 5)
                error.productId shouldBe "P-001"
                error.available shouldBe 5
            }
        }

        describe("ProductNotFound") {
            it("상품 ID를 포함한다") {
                val error = OrderError.ProductNotFound("P-999")
                error.productId shouldBe "P-999"
            }
        }
    }

    describe("OrderResult") {
        it("Success는 값을 담고 있다") {
            val order = Order("1", "P-1", "U-1", 1, 10000)
            val result: OrderResult<Order> = OrderResult.Success(order)

            result.shouldBeInstanceOf<OrderResult.Success<Order>>()
            result.value shouldBe order
        }

        it("Failure는 에러를 담고 있다") {
            val result: OrderResult<Order> = OrderResult.Failure(
                OrderError.InsufficientStock("P-1", 0)
            )

            result.shouldBeInstanceOf<OrderResult.Failure>()
            result.error.shouldBeInstanceOf<OrderError.InsufficientStock>()
        }
    }
})

// ============================
// Style 4: BehaviorSpec (Given/When/Then)
// ============================

/**
 * BehaviorSpec: BDD(Behavior-Driven Development) 스타일
 * Given(조건) - When(행위) - Then(결과) 구조
 */
class OrderBehaviorTest : BehaviorSpec({

    given("주문이 생성되었을 때") {
        val order = Order(
            id = "ORD-001",
            productId = "P-001",
            userId = "U-001",
            quantity = 2,
            totalPrice = 20000
        )

        `when`("상태를 확인하면") {
            then("CREATED 상태여야 한다") {
                order.status shouldBe OrderStatus.CREATED
            }
        }

        `when`("주문 정보를 확인하면") {
            then("상품 ID가 올바르다") {
                order.productId shouldBe "P-001"
            }
            then("수량이 올바르다") {
                order.quantity shouldBe 2
            }
        }
    }
})
