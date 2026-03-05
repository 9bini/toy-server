# .claude/ Structure

Configuration directory for Claude Code in Flash Sale Platform.

```
.claude/
├── settings.json              # Team shared settings (permissions, hooks)
├── README.md                  # This file
│
├── rules/                     # Rules (always loaded into Claude)
│   ├── 01-agent-behavior.md   #   Agent behavior rules, Intent Lock
│   ├── 02-architecture.md     #   Hexagonal Architecture
│   ├── 04-data-integration.md #   Redis/Kafka/DB patterns
│   ├── 05-api-design.md       #   REST API + WebFlux design
│   ├── 06-security-patterns.md#   Auth/AuthZ/IDOR/Race Condition
│   ├── 07-error-handling.md   #   Sealed class error handling
│   ├── 10-redteam.md          #   Automated security review (10 categories)
│   └── 11-rubric-scoring.md   #   5-dimension quality scoring (D1~D5)
│
├── agents/                    # Sub-agents (10)
│   ├── architect.md           #   Architecture design
│   ├── code-reviewer.md       #   Code readability review
│   ├── devops-engineer.md     #   Infrastructure/deployment
│   ├── doc-writer.md          #   Documentation writing
│   ├── integration-tester.md  #   Integration testing
│   ├── kotlin-expert.md       #   Kotlin expert
│   ├── performance-engineer.md#   Performance optimization
│   ├── project-manager.md     #   Project management
│   ├── security-reviewer.md   #   Security review
│   └── test-engineer.md       #   Test design
│
├── skills/                    # Skills (13)
│   ├── full-feature/          #   /full-feature - Full pipeline
│   ├── hotfix/                #   /hotfix - Quick bug fix
│   ├── implement-api/         #   /implement-api - API implementation
│   ├── write-test/            #   /write-test - Test writing
│   ├── design-service/        #   /design-service - DDD design
│   ├── review-code/           #   /review-code - Code review
│   ├── debug-issue/           #   /debug-issue - Debugging
│   ├── redis-setup/           #   /redis-setup - Redis setup
│   ├── kafka-setup/           #   /kafka-setup - Kafka setup
│   ├── saga-pattern/          #   /saga-pattern - Distributed transactions
│   ├── check-all/             #   /check-all - Full quality check
│   ├── document/              #   /document - Documentation writing
│   └── performance-test/      #   /performance-test - Load testing
│
├── hooks/                     # Automation hooks
│   ├── session-start.sh       #   Environment check on session start
│   ├── pre-commit.sh          #   Build verification before commit
│   └── post-code-quality.sh   #   Quality check after code writing
│
├── redteam/                   # RedTeam security review
│   └── suppressions.json      #   False positive suppression list
│
├── rubric/                    # Quality scoring
│   └── cases/                 #   Scoring examples (to be added)
│
└── docs/error/                # Error knowledge base
    ├── INDEX.md               #   Root index
    ├── architecture/          #   Architecture-related errors
    ├── spring/                #   Spring framework errors
    ├── database/              #   DB/cache errors
    ├── kotlin/                #   Kotlin language errors
    └── security/              #   Security-related errors
```

## Numbering Scheme (rules/)

| Number Range | Area |
|-------------|------|
| 01-03 | Agent behavior + Architecture |
| 04-05 | Data integration + API design |
| 06-09 | Security + Error handling |
| 10-12 | Automation (RedTeam, Rubric) |

## Git Untracked Targets

| Path | Description |
|------|-------------|
| `agent-memory/` | Agent learning records across sessions |
| `worktrees/` | Temporary git worktrees |
