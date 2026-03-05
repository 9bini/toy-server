# Security Patterns

## Flash Sale Specific Security Threats

### Race Condition Prevention
- Stock decrement must use Redis Lua Script (atomic decrement)
- Use Redisson for distributed locks (WATCH/MULTI prohibited)
- Prevent duplicate orders from the same user — idempotency key + TTL

### Rate Limiting Bypass Prevention
- Composite identification using IP + User-Agent + Token
- Token Bucket algorithm (Redis Lua Script)
- Dual defense at Nginx level + Application level

### Queue Manipulation Prevention
- Sorted Set score must use server timestamp (client values prohibited)
- Queue tokens are issued by the server (unpredictable values)

## Input Validation

### Required Validation Items
- Apply Bean Validation to all API inputs
- Never insert user input directly into Redis keys (sanitization required)
- Validate numeric parameter ranges (quantity > 0, quantity <= MAX_QUANTITY)

### IDOR (Insecure Direct Object Reference) Prevention
- Always verify ownership when accessing resources
- When querying by `orderId`, verify the order belongs to the requester
- Never expose internal sequential IDs (use UUID or separate external IDs)

## Secret Management
- Never hardcode secrets in code/configuration files
- Use environment variables or Vault
- Never expose stack traces/internal information in error responses
- Never output PII (personally identifiable information), tokens, or passwords in logs
