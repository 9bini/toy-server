---
name: performance-test
description: Writes and runs performance tests with k6. Measures TPS, p99 latency, and error rate.
argument-hint: [target-endpoint] [concurrent-users]
disable-model-invocation: true
---

$ARGUMENTS Run the performance test.

## Test Process

### 1. Pre-check
- Docker compose infrastructure running: `docker compose ps`
- Target service running
- k6 installed: `k6 version` (provide installation instructions if not installed)

### 2. Write Scenario
Create a JavaScript file in the `tests/performance/k6/` directory:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const orderLatency = new Trend('order_latency');

export const options = {
  stages: [
    { duration: '30s', target: 100 },   // Ramp-up
    { duration: '1m', target: 1000 },    // Peak
    { duration: '30s', target: 0 },      // Ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(99)<200'],    // p99 < 200ms
    errors: ['rate<0.01'],               // Error rate < 1%
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

### 3. Run Test
```bash
k6 run tests/performance/k6/{test-file}.js
```

### 4. Collect and Analyze Results
- TPS (Throughput)
- Latency: p50, p95, p99
- Error Rate
- Trend graph over time

### 5. Bottleneck Analysis
- Identify high-latency segments
- Check Redis/Kafka latency
- Check JVM GC pauses

### 6. Write Report
Record results in `docs/performance/{test-name}-{date}.md`:
- Test conditions (concurrent users, duration)
- Result metrics
- Bottleneck analysis
- Improvement suggestions
