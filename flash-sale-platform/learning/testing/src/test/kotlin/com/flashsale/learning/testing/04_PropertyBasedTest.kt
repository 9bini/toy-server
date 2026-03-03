package com.flashsale.learning.testing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import io.kotest.property.forAll

/**
 * === 4. Property-Based Testing ===
 *
 * 기존 테스트: 특정 입력 → 특정 출력 검증 (Example-based)
 * Property 테스트: 무작위 입력으로 속성(Property)이 항상 성립하는지 검증
 *
 * 장점:
 * - 예상치 못한 엣지 케이스 발견
 * - 불변 규칙(invariant)을 수학적으로 검증
 * - 코드의 견고성 향상
 *
 * flash-sale에서의 활용:
 * - 재고가 음수가 되지 않는지 검증
 * - 주문 금액 = 단가 × 수량 항상 성립
 * - 대기열 순번은 항상 양수
 */
class PropertyBasedTest : FunSpec({

    // ============================
    // forAll: 모든 입력에 대해 조건이 true인지 검증
    // ============================

    test("주문 총 가격은 항상 단가 × 수량이다") {
        forAll<Long, Int>(
            Arb.long(1L..100_000L),  // unitPrice: 1 ~ 100,000
            Arb.int(1..10)            // quantity: 1 ~ 10
        ) { unitPrice, quantity ->
            val order = Order(
                id = "ORD-1",
                productId = "P-1",
                userId = "U-1",
                quantity = quantity,
                totalPrice = unitPrice * quantity
            )
            order.totalPrice == unitPrice * quantity
        }
    }

    // ============================
    // checkAll: 모든 입력에 대해 assertions 실행
    // ============================

    test("주문 수량은 항상 양수여야 한다") {
        checkAll(Arb.int(1..1000)) { quantity ->
            quantity shouldBeGreaterThanOrEqual 1
        }
    }

    // ============================
    // Custom Arb: 도메인 객체 생성기
    // ============================

    test("Order는 항상 유효한 상태로 생성된다") {
        // Arb.int 두 개를 조합하여 Order 생성기 만들기
        val orderArb = Arb.int(1..100).map { quantity ->
            Order(
                id = "ORD-${System.nanoTime()}",
                productId = "P-001",
                userId = "U-001",
                quantity = quantity,
                totalPrice = 10_000L * quantity
            )
        }

        checkAll(orderArb) { order ->
            order.quantity shouldBeGreaterThanOrEqual 1
            order.quantity shouldBeLessThanOrEqual 100
            order.totalPrice shouldBeGreaterThan 0
            order.status shouldBe OrderStatus.CREATED
        }
    }

    // ============================
    // 재고 관리 속성 검증
    // ============================

    test("재고 차감 후 남은 재고는 항상 0 이상이다") {
        forAll<Int, Int>(
            Arb.int(0..1000),    // currentStock
            Arb.int(1..100)      // requestedQuantity
        ) { currentStock, requestedQuantity ->
            // 재고가 충분할 때만 차감
            if (currentStock >= requestedQuantity) {
                val remaining = currentStock - requestedQuantity
                remaining >= 0
            } else {
                true // 재고 부족 시 차감하지 않으므로 항상 true
            }
        }
    }
})
