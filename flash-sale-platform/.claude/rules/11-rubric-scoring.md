# Rubric Scoring (5-Dimension Quality Scoring)

Score quality across the following 5 dimensions during code review.
Each dimension is scored 1-5, with a maximum total of 25 points.

## D1: Correctness

| Score | Criteria |
|-------|----------|
| 5 | All requirements met, edge cases handled, all tests passing |
| 4 | Core requirements met, some edge cases not handled |
| 3 | Core behavior works but some cases are missing |
| 2 | Malfunctions in some scenarios |
| 1 | Core requirements not met or build failure |

## D2: Architecture Compliance

| Score | Criteria |
|-------|----------|
| 5 | Perfect adherence to Hexagonal structure, correct dependency direction, naming rules followed |
| 4 | Structure followed but minor naming/package location issues |
| 3 | Structure is followed but Port/Adapter roles are mixed |
| 2 | Dependency inversion between layers exists |
| 1 | Architecture ignored (e.g., Controller directly accessing DB) |

## D3: Concurrency Safety

| Score | Criteria |
|-------|----------|
| 5 | All shared state handled atomically, withTimeout applied, structured concurrency |
| 4 | Critical paths are safe but non-critical paths are lacking |
| 3 | Mostly safe but 1 or more potential Race Conditions |
| 2 | Blocking calls exist or GlobalScope used |
| 1 | Concurrency not considered (non-atomic Redis operations, no timeouts) |

## D4: Readability

| Score | Criteria |
|-------|----------|
| 5 | A new developer can understand within 5 minutes, naming clearly expresses intent |
| 4 | Understandable within 10 minutes, some comments needed |
| 3 | Understandable but complex parts lack explanation |
| 2 | Functions excessively long or deeply nested |
| 1 | Incomprehensible — many magic numbers, ambiguous variable names |

## D5: Test Coverage

| Score | Criteria |
|-------|----------|
| 5 | Unit + integration tests complete, all normal/error/edge cases covered |
| 4 | Core logic tests complete, some edge cases lacking |
| 3 | Only happy path tests exist |
| 2 | Tests exist but verification is insufficient (lacking assertions) |
| 1 | No tests or tests failing |

## Scoring Output Format

```
## Rubric Score

| Dimension | Score | Rationale |
|-----------|-------|-----------|
| D1 Correctness | ?/5 | {specific rationale} |
| D2 Architecture | ?/5 | {specific rationale} |
| D3 Concurrency | ?/5 | {specific rationale} |
| D4 Readability | ?/5 | {specific rationale} |
| D5 Test Coverage | ?/5 | {specific rationale} |
| **Total** | **?/25** | |

### Improvement Suggestions (focused on lowest scoring dimensions)
1. ...
2. ...
```

## Passing Criteria
- Total 20 points or above: Pass
- Total 15-19 points: Conditional pass (applying improvement suggestions recommended)
- Total 14 points or below: Rework required
- If CRITICAL security issues exist: Rework required regardless of score
