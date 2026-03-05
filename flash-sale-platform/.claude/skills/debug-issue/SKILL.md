---
name: debug-issue
description: Performs a systematic debugging workflow. Proceeds through error log analysis, reproduction, root cause analysis, fix, and verification.
argument-hint: [error-description-or-log]
---

$ARGUMENTS Debug the issue.

## Debugging Process

### 1. Identify Symptoms
- Review the full error message
- Analyze the stack trace
- Document reproduction conditions (when, which request, what situation)
- Determine impact scope

### 2. Narrow Down Scope
- Identify related services
- Identify related classes/methods
- Check recent changes (`git log`, `git diff`)
- Review logs

### 3. Formulate Hypotheses
List at least 3 possible causes:
1. Hypothesis A: ...
2. Hypothesis B: ...
3. Hypothesis C: ...

### 4. Verify Hypotheses
- Add logging to check data
- Write reproduction tests
- Set debug points

### 5. Confirm Root Cause
- Identify the cause, not the symptom
- Repeat "Why?" 5 times (5 Whys)
- Map out the causal chain

### 6. Fix
- Fix with minimal changes
- Check for side effects
- Verify the same issue doesn't exist elsewhere in related code

### 7. Verification
- Confirm all existing tests pass: `./gradlew test`
- Confirm the reproduction test now passes
- Add edge case tests

### 8. Documentation
- Briefly record the cause and solution
- Suggest measures to prevent recurrence
