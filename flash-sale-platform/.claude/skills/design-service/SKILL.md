---
name: design-service
description: Designs a microservice based on DDD. Includes domain model, port/adapter, API spec, and event design.
argument-hint: [service-name]
---

$ARGUMENTS Design the service based on DDD.

## Design Process

### 1. Domain Analysis
- Identify core domain entities, value objects, and aggregates
- Define Bounded Context boundaries
- Identify domain events

### 2. UseCase Definition
- Write use case interfaces for the Application layer
- Define input/output for each use case
- Specify business rules

### 3. Port/Adapter Design
- Input Port: UseCase interfaces
- Output Port: External system interfaces (Redis, Kafka, DB)
- Web Adapter: WebFlux Controller/Router
- Infrastructure Adapter: Redis, Kafka, DB implementations

### 4. API Spec
- Define REST/SSE endpoints
- Request/Response DTO schema
- Define error response codes

### 5. Event Design
- Kafka topic names, partition strategy
- Message schema (Avro/JSON)
- Publish/subscribe relationship diagram

### 6. Error Handling and Compensation
- List of failure scenarios
- Compensating transaction definitions
- DLQ strategy

## Output
- Write design document in `docs/{service-name}/DESIGN.md` (create docs directory if it doesn't exist)
- Package structure and main class list
- Kafka topic specifications
- Sequence diagram (Mermaid format)
