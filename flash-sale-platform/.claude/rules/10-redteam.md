# RedTeam Automated Security Review

Perform an automated security review across the following 10 categories on code changes.
Severity: CRITICAL > HIGH > MEDIUM > LOW

## Review Categories

### RT-01: Race Condition (CRITICAL)
- Stock decrement is not atomic
- Consistency broken under concurrent requests in distributed environment
- Verification: Confirm use of Redis Lua Script or Redisson distributed lock

### RT-02: Input Injection (HIGH)
- User input directly inserted into Redis keys
- SQL/NoSQL Injection
- Command Injection (when executing external processes)
- Verification: Confirm input sanitization, parameterized query usage

### RT-03: Authentication Bypass (CRITICAL)
- Protected endpoints accessible without authentication
- Missing token validation, missing expiration check
- Verification: Confirm SecurityFilterChain configuration

### RT-04: IDOR (HIGH)
- Ownership not verified when accessing resources
- Sequential ID exposure allowing access to other users' data
- Verification: Confirm owner verification logic on all resource queries

### RT-05: Rate Limiting Bypass (HIGH)
- Rate Limiter not applied
- Bypassable identifiers (using only IP, no token)
- Verification: Confirm Nginx + Application dual defense

### RT-06: Secret Exposure (CRITICAL)
- Hardcoded secrets in code/configuration
- Stack trace exposure in error responses
- PII/tokens output in logs
- Verification: Pattern scan with grep (password, secret, token, key)

### RT-07: Timeout Missing (MEDIUM)
- Missing withTimeout on external calls
- I/O operations that can wait indefinitely
- Verification: Confirm timeout constants on all external calls

### RT-08: Idempotency Gap (HIGH)
- Missing idempotency handling in Kafka Consumer
- Side effects that execute duplicately on retry
- Verification: Confirm idempotency key + duplicate check logic

### RT-09: Error Information Leak (MEDIUM)
- Internal exception messages exposed to client
- System information leakage such as DB schema, internal paths
- Verification: Confirm exception transformation in GlobalExceptionHandler

### RT-10: Coroutine Safety (HIGH)
- GlobalScope usage (violation of structured concurrency)
- Blocking calls inside suspend fun
- Inappropriate Dispatcher usage
- Verification: Confirm Dispatchers.IO isolation, structured concurrency

## Review Output Format

```
[RT-{number}] {category} | {severity}
File: {file path}:{line}
Issue: {description}
Attack: {attack scenario}
Fix: {fix method or code}
```

## False Positive Suppression
Items registered in `redteam/suppressions.json` are excluded from the review.
