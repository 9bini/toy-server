---
name: hotfix
description: Quick bug fix workflow. Performs root cause analysis → minimal change fix → regression test addition → PR creation rapidly.
argument-hint: [error-description-or-log]
---

$ARGUMENTS Apply a hotfix for the bug.

## Process

### 1. Root Cause Analysis
- Extract key information from error logs/stack traces
- Quickly explore related code (Grep + Read)
- Identify root cause (not symptoms)
- Determine impact scope

### 2. Minimal Change Fix
- Apply only fixes that minimize impact scope
- No unrelated refactoring
- Check for side effects
- Add comments in the fix code explaining "why it was fixed this way"

### 3. Add Regression Test
- Write a test that exactly reproduces this bug
- The test should fail before the fix and pass after the fix
- Run all existing tests to confirm they pass:
  ```bash
  ./gradlew :services:{service}:test
  ```

### 4. Verification
```bash
./gradlew :services:{service}:ktlintFormat
./gradlew :services:{service}:build
```

### 5. Commit & PR Creation

**Commit Strategy (minimal unit, English)**
Split hotfixes into the following commit units:
1. `fix({service}): {bug fix description}` - Code fix
2. `test({service}): add regression test for {bug}` - Reproduction test

Each commit must pass the build.

**PR Creation**
- Branch: `hotfix/{service-name}/{bug-description}`
- PR body: Include cause, fix details, reproduction test, and impact scope

### 6. Change Summary
Write summary according to the global CLAUDE.md template
