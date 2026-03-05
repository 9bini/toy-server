---
name: document
description: Generates technical documentation. Creates API docs, architecture docs, operation guides, tradeoff analysis, and interview preparation materials.
argument-hint: [doc-type api|architecture|operation|tradeoff|runbook] [target]
---

$ARGUMENTS Write the documentation.

## Guide by Document Type

### api - API Documentation
- Endpoint list (method, path, description)
- Request/response schema (including JSON examples)
- Error codes and error response format
- Authentication/authorization requirements
- Path: `docs/api/{service-name}.md`

### architecture - Architecture Documentation
- Overall system structure (Mermaid diagram)
- Inter-service communication flow
- Data flow diagram
- Rationale for technology choices
- Path: `docs/architecture/`

### operation - Operation Guide
- Deployment procedure
- Monitoring metrics and alert configuration
- Log inspection methods
- Scaling guide
- Path: `docs/operation/`

### tradeoff - Tradeoff Analysis (Interview Preparation)
- Problem definition (in what situation was this decision needed)
- Alternatives considered
- Comparison table of pros and cons for each alternative
- Final choice and rationale
- Numerical data (performance comparison, benchmarks)
- Expected interview Q&A
- Path: `docs/decisions/`

### runbook - Incident Response Manual
- Failure scenarios (Redis down, Kafka lag, payment timeout, etc.)
- Symptoms and detection methods
- Immediate response procedures
- Root cause analysis guide
- Post-incident actions
- Path: `docs/runbook/`

## Writing Principles
- Clear and concise (no verbose explanations)
- Include diagrams (Mermaid format)
- Specify "why it was done this way" tradeoffs
- Always include code examples
- Write in English
