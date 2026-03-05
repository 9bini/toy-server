---
name: check-all
description: Runs a full project quality check. Verifies lint, compilation, tests, and architecture rules all at once.
argument-hint: [--fix]
---

Run the full quality check. $ARGUMENTS

## Check Items (execute in order)

### 1. Code Format Check
```bash
# Auto-format if --fix option is provided
./gradlew ktlintFormat  # with --fix
./gradlew ktlintCheck   # without --fix
```

### 2. Compilation Check
```bash
./gradlew compileKotlin
```

### 3. Full Tests
```bash
./gradlew test
```

### 4. Architecture Rule Check (manual via Grep)
Search for the following violations:
- Direct import of another service's domain from the adapter package
- Import of adapter/application package from the domain package (dependency inversion)
- GlobalScope usage
- runBlocking usage (except in tests)
- External calls without withTimeout
- Deprecated API usage (based on latest versions of Spring Boot/Kotlin/libraries)

### 5. Results Summary
Organize check results in the following table:

| Item | Status | Details |
|------|--------|---------|
| ktlint | PASS/FAIL | N violations |
| Compilation | PASS/FAIL | N errors |
| Tests | PASS/FAIL | M of N passed |
| Architecture | PASS/WARN | N violations |

### 6. Fix Suggestions
Provide specific fix suggestions for FAIL/WARN items.
