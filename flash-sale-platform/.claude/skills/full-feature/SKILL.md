---
name: full-feature
description: Implements a feature end-to-end from design to PR. Executes the full pipeline of design → implementation → testing → review → PR creation.
argument-hint: [service-name] [feature-description]
---

$ARGUMENTS Implement the feature through the full pipeline.

## Full Pipeline (6 Stages)

### Stage 1: Design Review
- Domain model design (Entity, Value Object, Error)
- UseCase definition (Input/Output Port)
- Adapter identification (which of Redis/Kafka/DB are needed)
- API spec (request/response DTO, HTTP method, path)
- Kafka event design (if needed)
- Redis key/operation design (if needed)
- Check if any latest Spring Boot/Kotlin features can be leveraged
- Request design confirmation from the user before proceeding

### Stage 2: Implementation (follow implementation order strictly)
Follow the order below strictly:
1. **Domain**: Entity, Value Object, sealed interface Error (including KDoc)
2. **Port Out**: Output Port interface (no tech details exposed)
3. **Port In**: UseCase interface
4. **UseCase Implementation**: Business logic (suspend fun, withTimeout)
5. **Adapter Out**: Redis/Kafka/DB implementation (class names include tech stack)
6. **Adapter In**: Controller (suspend fun, WebFlux)
7. **Config**: Spring bean registration

### Stage 3: Write Tests + Execute
- UseCase unit tests (Kotest BehaviorSpec + MockK)
  - Normal cases, error cases, edge cases
  - Use coEvery/coVerify
- Controller tests (WebTestClient, if needed)
- Integration tests (Testcontainers, when using Redis/Kafka)
- Run all tests: `./gradlew :services:{service}:test`
- Fix immediately on failure and re-run

### Stage 4: Quality Verification
```bash
./gradlew :services:{service}:ktlintFormat
./gradlew :services:{service}:build
```
- Fix immediately on build failure

### Stage 5: Change Summary
Write according to the change summary template in the global CLAUDE.md:
- What was done (1 line)
- Why it was done this way (key decisions)
- Changed files table
- Core code flow
- Notes/caveats

### Stage 6: Commit & PR Creation

**Commit Strategy (minimal functional unit, English)**
Split into the following units for commits. Each commit must pass the build:
1. `feat({service}): define domain models` - Entity, VO, sealed interface Error
2. `feat({service}): implement ports and use cases` - Port In/Out + UseCase
3. `feat({service}): implement adapters` - Redis/Kafka/DB Adapter
4. `feat({service}): add API endpoints and configuration` - Controller + Config
5. `test({service}): add unit and integration tests` - Test code

Include the key reason for the change in the commit message body.

**PR Creation**
- Branch: `feature/{service-name}/{feature-description}`
- PR title: Under 70 characters, in English
- PR body: Design decisions, changed files, test results

## Abort Conditions
- Build failure → fix immediately
- Test failure → fix immediately
- ktlint violation → auto-format
