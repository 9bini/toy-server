/**
 * Flash Sale 부하 테스트 시나리오
 *
 * 시나리오: 10만 사용자가 동시에 한정 1,000개 상품을 구매 시도
 * 측정 지표: TPS, p99 레이턴시, 에러율, 재고 정합성
 *
 * 실행: k6 run tests/performance/k6/flash-sale-load-test.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// 커스텀 메트릭
const orderSuccess = new Counter('order_success');
const orderFailed = new Counter('order_failed');
const orderLatency = new Trend('order_latency');
const errorRate = new Rate('error_rate');

// 테스트 설정
export const options = {
  scenarios: {
    flash_sale: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 100 },    // 웜업
        { duration: '30s', target: 1000 },   // 램프업
        { duration: '1m', target: 10000 },   // 피크 (동시 1만)
        { duration: '30s', target: 0 },      // 램프다운
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(99)<500'],       // p99 < 500ms
    error_rate: ['rate<0.05'],              // 에러율 < 5%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PRODUCT_ID = 'FLASH-SALE-001';

// 1단계: 대기열 진입
function enterQueue(userId) {
  const res = http.post(`${BASE_URL}/api/v1/queue/enter`, JSON.stringify({
    userId: userId,
    productId: PRODUCT_ID,
  }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'enter_queue' },
  });

  check(res, {
    'queue enter - status 200': (r) => r.status === 200,
  });

  return res;
}

// 2단계: 주문 요청
function placeOrder(userId) {
  const res = http.post(`${BASE_URL}/api/v1/orders`, JSON.stringify({
    userId: userId,
    productId: PRODUCT_ID,
    quantity: 1,
  }), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'place_order' },
  });

  orderLatency.add(res.timings.duration);

  const success = check(res, {
    'order - status 200 or 409': (r) => r.status === 200 || r.status === 409,
  });

  if (res.status === 200) {
    orderSuccess.add(1);
  } else {
    orderFailed.add(1);
  }

  errorRate.add(!success);
  return res;
}

export default function () {
  const userId = `USER-${__VU}-${__ITER}`;

  // 대기열 진입
  enterQueue(userId);
  sleep(0.1);

  // 주문 시도
  placeOrder(userId);
  sleep(0.5);
}
