---
name: performance-test
description: k6로 성능 테스트를 작성하고 실행합니다. TPS, p99 레이턴시, 에러율을 측정합니다.
argument-hint: [target-endpoint] [concurrent-users]
disable-model-invocation: true
---

$ARGUMENTS 성능 테스트를 수행하세요.

## 테스트 프로세스

### 1. 사전 확인
- docker compose 인프라 실행 여부: `docker compose ps`
- 테스트 대상 서비스 실행 여부
- k6 설치 여부: `k6 version` (없으면 설치 안내)

### 2. 시나리오 작성
`tests/performance/k6/` 디렉토리에 JavaScript 파일 생성:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const orderLatency = new Trend('order_latency');

export const options = {
  stages: [
    { duration: '30s', target: 100 },   // 램프업
    { duration: '1m', target: 1000 },    // 피크
    { duration: '30s', target: 0 },      // 램프다운
  ],
  thresholds: {
    http_req_duration: ['p(99)<200'],    // p99 < 200ms
    errors: ['rate<0.01'],               // 에러율 < 1%
  },
};

export default function () {
  const res = http.post('http://localhost:8080/api/v1/orders', JSON.stringify({
    productId: 'PRODUCT-001',
    userId: `USER-${__VU}`,
    quantity: 1,
  }), { headers: { 'Content-Type': 'application/json' } });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });

  orderLatency.add(res.timings.duration);
  errorRate.add(res.status !== 200);
  sleep(0.1);
}
```

### 3. 테스트 실행
```bash
k6 run tests/performance/k6/{test-file}.js
```

### 4. 결과 수집 및 분석
- TPS (Throughput)
- Latency: p50, p95, p99
- Error Rate
- 시간별 추이 그래프

### 5. 병목 분석
- 레이턴시가 높은 구간 식별
- Redis/Kafka 지연 확인
- JVM GC 일시정지 확인

### 6. 보고서 작성
`docs/performance/{test-name}-{date}.md` 에 결과 기록:
- 테스트 조건 (동시 사용자, 기간)
- 결과 메트릭
- 병목 분석
- 개선 제안
