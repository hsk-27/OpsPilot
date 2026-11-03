# OpsPilot — Implementation Roadmap & Execution Plan

## Phase 0: Baseline & Infrastructure Setup
- [x] Reconnaissance of local runtime: JDK 23 / Java 21, Maven wrapper, Node v24, npm 11.
- [x] Initialize project structure (`backend`, `frontend`, `docs`, `docker-compose.yml`).
- [x] Author comprehensive Phase 0 engineering documentation.

## Phase 1: Application Foundation & Multi-Tenancy
- [ ] Maven `pom.xml` configuration with pinned, compatible dependencies.
- [ ] Flyway migration `V1__init_schema.sql` covering all initial core entities.
- [ ] Identity, User, Organization, Membership, and Role entities.
- [ ] Spring Security 6 + JJWT with short-lived access tokens + rotating refresh token sessions.
- [ ] Global RFC 9457 Problem Details error handler.
- [ ] TenantContext and tenant filter.
- [ ] OpenAPI 3.0 / Swagger UI configuration.
- [ ] Baseline tests for auth, registration, login, refresh, tenant isolation.

## Phase 2: Document Processing & Invoice Domain
- [ ] Document storage service (SHA-256 checksum, MIME validation, private filesystem storage).
- [ ] Pure Java & PDF text extraction engine (`DocumentTextExtractor`).
- [ ] Supplier management (CRUD, unique codes, status).
- [ ] Purchase Order management (CRUD, lines, calculation).
- [ ] Invoice intake & upload endpoint (`POST /api/v1/invoices/upload`).
- [ ] Invoice lines and metadata persistence.
- [ ] Deterministic validation engine:
  - Subtotal + Tax = Total arithmetic checks with rounding tolerance.
  - Negative amount rejection.
  - Organization-scoped duplicate detection (`normalized_supplier`, `normalized_invoice_number`, `currency`).
- [ ] Purchase Order matching engine (two-way PO comparison, line checks, tolerance).

## Phase 3: AI Invoice Extraction Engine
- [ ] `AiProvider` SPI:
  - `MockAiProvider`: Deterministic, configurable confidence, error simulation, low-confidence fixtures.
  - `GeminiAiProvider`: Structured output schema parsing, Gemini 1.5/2.0 API compatibility, timeout & retry handling.
- [ ] Extraction DTOs, field confidence mapping, evidence extraction.
- [ ] Confidence threshold routing (>= 0.90 auto-progress eligible, 0.70-0.90 review, < 0.70 reprocess).
- [ ] Prompt injection defense (strict untrusted text handling, schema bounds).
- [ ] AI invocation recording & token/cost telemetry.
- [ ] Synthetic evaluation benchmark suite (ground-truth dataset with >= 50 synthetic test cases, accuracy report).

## Phase 4: Durable Workflow Engine
- [ ] Graph data model: Nodes, Directed Edges, Configurations.
- [ ] Graph validation service (single START, terminal END, acyclic reachability, branch validation).
- [ ] Workflow definition & versioning (immutable published versions).
- [ ] Node executor registry & implementations:
  - `START`
  - `AI_EXTRACTION`
  - `VALIDATE_INVOICE`
  - `MATCH_PURCHASE_ORDER`
  - `CONDITION` (typed operators: EQUALS, GREATER_THAN, etc.)
  - `APPROVAL` (creates task, pauses execution)
  - `MANUAL_REVIEW`
  - `NOTIFICATION`
  - `EXPORT_INVOICE`
  - `END`
- [ ] PostgreSQL-backed job queue (`WorkflowJob`) with lease ownership, timeout, and optimistic locking.
- [ ] Exponential backoff retry manager with error classification (TRANSIENT, RATE_LIMITED, PERMANENT).
- [ ] Crash recovery & lease reaper for orphaned RUNNING jobs.
- [ ] Idempotency key tracking.
- [ ] Seed standard AP workflow.

## Phase 5: Human Approvals, Outbox & Accounting Export
- [ ] Approval task lifecycle (`PENDING` -> `APPROVED` | `REJECTED` | `CHANGES_REQUESTED` | `ESCALATED`).
- [ ] Concurrency protection against duplicate decisions.
- [ ] Resume workflow job upon approval completion.
- [ ] Accounting export SPI:
  - CSV exporter.
  - JSON exporter.
  - Mock ERP adapter with simulated timeouts and idempotent retries.
- [ ] Outbox event pattern for reliable domain event dispatching.
- [ ] Append-only audit log recorder for all state changes.

## Phase 6: Frontend Application & Workflow Canvas
- [ ] Vite + React 18 + TypeScript strict setup.
- [ ] Layout, navigation, theme, accessible UI components.
- [ ] Authentication screens (Login, Register).
- [ ] Operational AP Dashboard (real metrics from backend analytics).
- [ ] Invoice Explorer & Upload Modal with PDF preview and field-level confidence indicators.
- [ ] Invoice Detail View with extraction viewer, line items, PO match, validation results, and audit timeline.
- [ ] Approval Inbox with decision modal and change request comments.
- [ ] Supplier & Purchase Order management views.
- [ ] Visual Workflow Builder (drag-and-drop nodes, connection lines, property configuration, validation errors).
- [ ] Execution Inspector with node-level execution timeline and durations.

## Phase 7: Analytics, Observability & Hardening
- [ ] Operational AP analytics endpoints (cycle times, pass rates, AI usage, pending queues).
- [ ] Prometheus metrics configuration via Micrometer.
- [ ] Health checks (DB, Queue worker, AI provider).
- [ ] Comprehensive unit, integration, and security test suites.
- [ ] Docker Compose orchestration (`backend`, `frontend`, `postgres`).
- [ ] CI workflow definition (GitHub Actions).
- [ ] Final verification report.
