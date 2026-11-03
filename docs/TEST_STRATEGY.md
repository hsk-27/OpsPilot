# OpsPilot — Test Strategy & Verification Plan

## 1. Testing Philosophy
Tests in OpsPilot validate real business invariants and operations. Trivial tests that simply verify non-null returns or mock out the unit under test are prohibited.

## 2. Test Tiers

### 2.1 Backend Unit Tests
- **Deterministic Validation**:
  - Valid calculations: `subtotal + tax = total` within configurable rounding tolerances.
  - Negative amounts rejection, zero quantity detection.
  - Duplicate detection based on `(organization_id, supplier_id, invoice_number, currency)`.
- **Purchase Order Matching**:
  - Two-way match with matching currency and amounts within tolerance.
  - Quantity/unit price line item matching.
  - Mismatch flagging (supplier mismatch, currency mismatch, amount over budget).
- **Workflow Graph Validation**:
  - Cycle detection in acyclic DAGs.
  - Exact 1 START node, at least 1 reachable END node.
  - Branch condition coverage and unknown node type rejection.
- **Workflow Condition Evaluator**:
  - Typed operations: `EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, `LESS_THAN`, `CONTAINS`, `IS_EMPTY`.
  - Type coercion safety and missing property handling.
- **AI Extraction & Response Validation**:
  - Schema conformance verification.
  - Field confidence routing (`>= 0.90` auto-pass, `0.70-0.90` manual review, `< 0.70` reprocess).
  - Malformed and prompt-injection simulated responses.

### 2.2 Integration & Persistence Tests
- **Multi-Tenant Scoping**:
  - Cross-tenant data leakage tests: Tenant A attempting to access Tenant B's invoices or approvals.
- **Authentication & Refresh Flow**:
  - Login -> Access Token -> Refresh Token rotation -> Session Revocation.
- **Job Queue & Concurrency**:
  - Lease acquisition and `FOR UPDATE SKIP LOCKED` simulation.
  - Concurrent approval decisions (ensuring only one decision succeeds).
  - Duplicate export idempotency key collision handling.
- **Database Migrations**:
  - Flyway schema applied cleanly from an empty state.

### 2.3 AI Synthetic Evaluation Benchmark
- Ground-truth evaluation dataset with 50+ diverse synthetic invoice scenarios (multi-currency, discount lines, tax rates, varied date formats).
- Benchmarking harness reporting:
  - Supplier name accuracy.
  - Invoice number accuracy.
  - Date normalization accuracy.
  - Currency & Financial amounts precision.
  - Latency percentiles (p50, p95).

### 2.4 Frontend Tests (Vitest & Testing Library)
- Authentication state preservation.
- Form validation (Zod schemas for login, supplier creation, PO creation).
- Approval task action buttons (Approve, Reject, Request Changes).
- Workflow canvas graph rendering and node property inspection.
