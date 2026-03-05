# REST API Design Rules

## WebFlux + Coroutines

### Controller Rules
- All handlers must be `suspend fun` — blocking code is strictly prohibited
- Request/response DTOs are defined at the Controller level (never expose domain objects directly)
- Use Bean Validation for validation (`@Valid`, `@field:NotBlank`, etc.)

### Response Format
```kotlin
// Success
data class ApiResponse<T>(
    val success: Boolean = true,
    val data: T
)

// Error
data class ErrorResponse(
    val success: Boolean = false,
    val error: ErrorDetail
)
data class ErrorDetail(
    val code: String,      // "ORDER_INSUFFICIENT_STOCK"
    val message: String    // User-facing message
)
```

### HTTP Status Mapping
| Situation | Status | Example |
|-----------|--------|---------|
| Success | 200 OK | Query successful |
| Created | 201 Created | Order created |
| Input error | 400 Bad Request | Validation failure |
| Authentication failure | 401 Unauthorized | Token expired |
| Insufficient permissions | 403 Forbidden | Accessing another user's order |
| Resource not found | 404 Not Found | Non-existent product |
| Business rule violation | 409 Conflict | Insufficient stock, duplicate order |
| Too many requests | 429 Too Many Requests | Rate Limit exceeded |

### SSE (Server-Sent Events)
- Use SSE for queue status notifications
- Connection timeout and reconnection logic are required
- Verify consistency with Nginx SSE proxy configuration
