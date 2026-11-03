# OpsPilot — AI-Assisted Accounts Payable Operations Automation Platform

[![CI Pipeline](https://github.com/opspilot/opspilot/actions/workflows/ci.yml/badge.svg)](https://github.com/opspilot/opspilot/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java: 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot: 3.3.5](https://img.shields.io/badge/Spring_Boot-3.3.5-green.svg)](https://spring.io/projects/spring-boot)
[![React: 18](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)

**OpsPilot** is a production-grade, AI-assisted business operations automation SaaS designed specifically for Accounts Payable (AP) and supplier invoice processing. 

OpsPilot is **not** a generic conversational AI bot or an unrestricted agent. It is a strictly controlled operational platform where:
- **AI interprets** unstructured invoice documents.
- **Deterministic backend services** mathematically validate financial totals, taxes, duplicates, and constraints.
- **A durable workflow engine** executes explicitly constructed business processes.
- **Human approvers** authorize high-value transactions.
- **An append-only audit trail** captures an immutable record of every operational event.

---

## 1. High-Level Architecture

OpsPilot is engineered as a **modular monolith** with multi-tenant data isolation enforced by `organization_id` on all database records, indexes, and API endpoints.

```text
                                 +--------------------------------+
                                 |  REACT 18 + TYPESCRIPT SPA     |
                                 |  (Vite, TailwindCSS, TanStack) |
                                 +--------------------------------+
                                                 |
                                         REST API / HTTPS
                                                 |
                                 +--------------------------------+
                                 |      SPRING BOOT MONOLITH      |
                                 +--------------------------------+
                                 |                                |
  +------------------+           |  +--------------------------+  |           +-------------------+
  |   Document Store | <---------|--| Document Intake & Parser |  |---------> | AI Extraction SPI |
  | (Local / S3 SPI) |           |  +--------------------------+  |           | (Mock & Gemini)   |
  +------------------+           |  +--------------------------+  |           +-------------------+
                                 |  | Deterministic Validation |  |
                                 |  +--------------------------+  |
                                 |  +--------------------------+  |           +-------------------+
                                 |  | Purchase Order Matching  |  |           | Accounting Export |
                                 |  +--------------------------+  |---------> | (CSV, JSON, ERP)  |
                                 |  +--------------------------+  |           +-------------------+
                                 |  | Durable Workflow Engine  |  |
                                 |  +--------------------------+  |
                                 |  +--------------------------+  |
                                 |  | Human Approval Inbox     |  |
                                 |  +--------------------------+  |
                                 |  +--------------------------+  |
                                 |  | Audit & Analytics Engine |  |
                                 |  +--------------------------+  |
                                 +--------------------------------+
                                                 |
                                                 v
                                 +--------------------------------+
                                 |    PostgreSQL / Flyway DB      |
                                 |  (Tenant Row-Level Isolation)  |
                                 +--------------------------------+
```

---

## 2. Core Operational Modules

1. **Identity, Multi-Tenancy & RBAC**:
   - Strictly enforced roles: `OWNER`, `ADMIN`, `OPERATOR`, `APPROVER`, `VIEWER`.
   - JWT authentication (HMAC-SHA256 via JJWT 0.12.6) with automatic token rotation via secure HttpOnly refresh cookies.
   - Tenant context bound per request via ThreadLocal `TenantContext`.
2. **Document Intake & Storage**:
   - MIME type and binary `%PDF-` signature verification.
   - SHA-256 fingerprinting for duplicate file submission detection.
   - Built-in pure Java PDF text and stream decompressor.
3. **AI Extraction SPI**:
   - `MockAiProvider`: Deterministic regex and template parsing with simulated latency, errors, and low-confidence modes.
   - `GeminiAiProvider`: Integration with Google Gemini Flash / Pro using structured JSON schemas.
   - Evaluation benchmark verifies 100% field accuracy over 50 synthetic test invoices (p50: 85ms).
4. **Deterministic Validation Engine**:
   - Mathematical line item arithmetic verification: $\sum (\text{lineTotal}) == \text{subtotal}$.
   - Tax rate calculation verification: $\text{subtotal} \times \text{taxRate} == \text{taxAmount}$.
   - Invoice total verification: $\text{subtotal} + \text{taxAmount} == \text{totalAmount}$.
   - Duplicate invoice detection matching `(organization_id, supplier_id, invoice_number)`.
   - Positive amount, future date, and currency conformity checks.
5. **Purchase Order 2-Way & 3-Way Matching**:
   - Automated comparison between invoice totals and authorized purchase orders.
   - Strict configurable tolerance thresholds (e.g. $\pm \$1.00$).
   - Flags discrepancies directly into the review queue.
6. **Visual Workflow Definition & Durable Engine**:
   - Interactive SVG/DOM canvas with nodes: `START`, `AI_EXTRACTION`, `VALIDATE_INVOICE`, `MATCH_PURCHASE_ORDER`, `CONDITION`, `APPROVAL`, `MANUAL_REVIEW`, `EXPORT_INVOICE`, `END`.
   - Graph topology validation (detects cycles, disconnected paths, unhandled condition branches).
   - Immutable version publishing (e.g. `v1`, `v2`).
   - Durable state machine execution tracking with step duration telemetry and output snapshots.
   - Scheduled lease manager for background job recovery.
7. **Human Approval Management**:
   - Real-time inbox for invoices requiring authorization (e.g. total $> \$5,000$).
   - Multi-role decision actions: `APPROVED`, `REJECTED`, `CHANGES_REQUESTED` with audit notes.
8. **Accounting Export & Integration**:
   - Formats: CSV export, structured JSON export, and ERP Simulator adapter.
   - Idempotency key protection prevents duplicate ERP exports.
9. **Append-Only Audit Trail & Telemetry**:
   - Immutable `audit_events` ledger recording actor, action, timestamp, and metadata.
   - Real-time operational KPI analytics (throughput, error rates, cycle times, AI cost).

---

## 3. Pre-Seeded Demo Accounts

The application automatically seeds a realistic tenant organization (**Global Logistics Enterprise**) with distinct role-based accounts:

| User | Email | Password | Role | Permissions |
|---|---|---|---|---|
| **Sarah Connor** | `admin@opspilot.io` | `Password123!` | `OWNER` / `ADMIN` | Full tenant admin, workflow publishing, user management |
| **Michael Scott** | `approver@opspilot.io` | `Password123!` | `APPROVER` | Reviews and authorizes pending invoice approval tasks |
| **Pam Beesly** | `operator@opspilot.io` | `Password123!` | `OPERATOR` | Ingests invoices, inspects extractions, manages line items |
| **Jim Halpert** | `viewer@opspilot.io` | `Password123!` | `VIEWER` | Read-only inspection of invoices, analytics, and audit trail |

> **Tip:** The login screen includes 1-click demo account selector pills for rapid role switching during evaluation.

---

## 4. Running the Application

### Option A: Docker Compose (Recommended)

Run the full stack (PostgreSQL 16, Spring Boot backend, and React Nginx frontend) with one command:

```bash
docker-compose up --build
```

- Frontend: `http://localhost`
- Backend API & Actuator: `http://localhost:8080`
- Swagger / OpenAPI UI: `http://localhost:8080/swagger-ui.html`

### Option B: Local Development

#### Prerequisites
- **JDK 21** or higher
- **Node.js 20** or higher
- **Maven** (included via `./mvnw` / `.\mvnw.cmd`)

#### 1. Start the Backend
```bash
cd backend
.\mvnw.cmd spring-boot:run
```
*Note: By default, the application runs on port `8080` using an embedded in-memory H2 database with the realistic data seeder enabled.*

#### 2. Start the Frontend
```bash
cd frontend
npm.cmd run dev
```
Open `http://localhost:5173` in your browser.

---

## 5. Verification & Automated Tests

### Run Backend Unit & Integration Tests
```bash
cd backend
.\mvnw.cmd test
```
All 16 backend unit, validation, matching, and benchmark tests execute and pass:
```text
[INFO] Running com.opspilot.ai.benchmark.AiExtractionBenchmarkTest
[INFO] Extraction Benchmark Results:
[INFO] - Total Invoices Tested: 50
[INFO] - Overall Field Accuracy: 100.0%
[INFO] - Latency p50: 85 ms | p95: 90 ms
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Build & Typecheck Frontend
```bash
cd frontend
npm.cmd run build
```
Executes strict TypeScript type checking (`tsc`) and Vite production bundling with zero errors.

---

## 6. Project Directory Layout

```text
opspilot/
├── backend/
│   ├── src/main/java/com/opspilot/
│   │   ├── ai/              # AI Provider SPI (Mock & Gemini adapters)
│   │   ├── analytics/       # Operational AP analytics & KPIs
│   │   ├── approval/        # Human approval task inbox & decisions
│   │   ├── audit/           # Append-only immutable audit trail
│   │   ├── common/          # Security, JWT, RFC 9457 exceptions, tenant context
│   │   ├── document/        # PDF upload, signature validation & parser
│   │   ├── export/          # CSV, JSON, and ERP simulator export
│   │   ├── identity/        # Users, organizations, memberships, auth
│   │   ├── invoice/         # Invoices, line items, and lifecycle states
│   │   ├── purchaseorder/   # Purchase orders and line items
│   │   ├── supplier/        # Supplier master registry
│   │   ├── validation/      # Deterministic mathematical financial validation
│   │   └── workflow/        # Durable workflow engine, worker & executors
│   └── src/main/resources/
│       ├── application.yml  # Multi-profile configurations (H2 / PostgreSQL)
│       └── db/migration/    # Flyway V1__init_schema.sql (21 tables)
├── frontend/
│   ├── src/
│   │   ├── api/             # Axios client with JWT refresh interceptor
│   │   ├── components/      # Layout and UI components
│   │   ├── context/         # AuthContext & role checker
│   │   ├── pages/           # Dashboard, Invoices, Detail, Approvals, Workflows...
│   │   └── types/           # Complete TypeScript interface definitions
│   └── package.json
├── docs/
│   ├── ARCHITECTURE.md      # Detailed system architecture specification
│   ├── AI_EVALUATION.md     # AI Benchmark report (50 synthetic invoices)
│   ├── DATABASE_DESIGN.md   # Relational schema specification
│   └── diagrams/            # Mermaid state machines and workflows
├── docker-compose.yml       # Production container orchestration
├── Dockerfile.backend       # Multi-stage JDK 21 build
├── Dockerfile.frontend      # Multi-stage Vite / Nginx alpine build
└── README.md
```

---

## 7. License

OpsPilot is licensed under the [MIT License](LICENSE).
