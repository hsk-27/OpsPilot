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

## 3. Pre-Seeded Evaluation Accounts & Instant Guest Access

The application automatically seeds a realistic tenant organization (**Global Logistics Enterprise**) with distinct role-based accounts:

| User | Email | Password | Role | Operational Responsibilities |
|---|---|---|---|---|
| **Eleanor Vance** | `admin@opspilot.io` | `Password123!` | `OWNER` / `ADMIN` | Full tenant admin, workflow publishing, ERP integrations, policy controls |
| **Marcus Sterling** | `approver@opspilot.io` | `Password123!` | `APPROVER` | Reviews and authorizes pending invoice approval tasks (> $5,000) |
| **David Chen** | `operator@opspilot.io` | `Password123!` | `OPERATOR` | Ingests invoices, inspects extractions, manages line items & discrepancies |
| **Sophia Rodriguez** | `viewer@opspilot.io` | `Password123!` | `VIEWER` | Read-only inspection of invoices, analytics, and audit trail |

> **Instant Guest Access:** For frictionless evaluation, the login page features a 1-click **"Instant Guest Access (Auditor / Admin)"** button that bypasses manual credential typing and grants immediate access to the platform.

---

## 4. Running the Application

### Option A: 1-Click Automated Startup (Recommended for Evaluation)

From the project root directory (`OpsPilot/`), execute the launch script:

```powershell
.\start.ps1
```
*(Or double-click `start.bat` in Windows File Explorer)*

This automatically:
1. Boots the Spring Boot backend on `http://localhost:8080`.
2. Starts the Vite React frontend on `http://localhost:5173`.
3. Opens `http://localhost:5173` directly in your default browser.

### Option B: Manual Local Startup

#### Prerequisites
- **JDK 21** or higher
- **Node.js 20** or higher
- **Maven** (included via `./mvnw` / `.\mvnw.cmd`)

#### 1. Start the Backend
```bash
cd backend
.\mvnw.cmd spring-boot:run
```
*Runs on port `8080` using an embedded in-memory H2 database with the pre-seeded enterprise dataset.*

#### 2. Start the Frontend
```bash
cd frontend
npm.cmd run dev
```
Open `http://localhost:5173` in your browser.

### Option C: Production Docker Compose
```bash
docker-compose up --build
```
- Frontend: `http://localhost`
- Backend API & Actuator: `http://localhost:8080`
- Swagger / OpenAPI UI: `http://localhost:8080/swagger-ui.html`

---

## 5. Verification & Automated Tests

### Run Backend Unit & Integration Tests
```bash
cd backend
.\mvnw.cmd test
```
All **52 backend integration, unit, validation, matching, and benchmark tests** execute and pass (0 failures, 0 errors, 0 skipped):
```text
[INFO] Running com.opspilot.ai.AiExtractionBenchmarkTest
=======================================================
OPSPILOT AI EXTRACTION BENCHMARK EVALUATION REPORT
=======================================================
Total Synthetic Invoices Evaluated: 50
Overall Field Accuracy:             100.0% (Target: >= 90.0%)
Latency p50:                        85 ms | p95: 88 ms
Missing Field Rate:                 0.0%
Invalid Output Rate:                0.0%
=======================================================
...
[INFO] Results:
[INFO] Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Build & Typecheck Frontend
```bash
cd frontend
npm.cmd run build
```
Executes strict TypeScript type checking (`tsc`) and Vite production bundling with **zero errors**.

---

## 6. Operational Verification & Testing

For complete, module-by-module testing procedures, role authorization boundaries, and failure mode verifications:
👉 See the [**Operational Verification & Testing Guide**](docs/MANUAL_TESTING_GUIDE.md).

Sample invoice PDFs representing standard, high-value, exception, and edge-case operational scenarios are available in [`samples/invoices/`](samples/invoices/):
1. `01_acme_industrial_standard.pdf`: Standard PO matching ($11,800.00) triggering high-value threshold approval.
2. `02_cloudscale_hosting_approval.pdf`: Cloud infrastructure invoice ($3,540.00) matching authorized purchase order terms.
3. `03_apex_office_needs_review.pdf`: Mathematical discrepancy ($600.00 stated total vs $500.00 line total) routing to review.
4. `04_simulated_low_confidence.pdf`: Low OCR/AI confidence simulation validating manual operator intervention flow.

---

## 7. Project Directory Layout

```text
OpsPilot/
├── backend/
│   ├── src/main/java/com/opspilot/
│   │   ├── admin/           # Tenant organization settings & external ERP integration configs
│   │   ├── ai/              # AI Provider SPI (Deterministic Mock & Google Gemini adapters)
│   │   ├── analytics/       # Operational AP throughput, latency & financial KPIs
│   │   ├── approval/        # Role-based human approval inbox, escalation & decisions
│   │   ├── audit/           # Tamper-evident, append-only immutable audit trail
│   │   ├── common/          # Security filters, JWT rotation, RFC 9457 exceptions, TenantContext
│   │   ├── document/        # PDF upload, MIME/signature validation & pure Java text parser
│   │   ├── export/          # Accounting export adapters (CSV, structured JSON, ERP simulator)
│   │   ├── identity/        # User accounts, memberships, authentication & guest access
│   │   ├── invoice/         # Invoices, itemized lines, and lifecycle state transitions
│   │   ├── notification/    # In-app notifications & mock email delivery
│   │   ├── purchaseorder/   # Purchase orders, lines & 2-way line reconciliation
│   │   ├── supplier/        # Supplier master registry & tax identifier verification
│   │   ├── validation/      # Deterministic mathematical, temporal & line arithmetic rules
│   │   └── workflow/        # Durable DAG workflow engine, scheduler & node executors
│   └── src/main/resources/
│       ├── application.yml  # Multi-profile configuration (H2 local / PostgreSQL prod)
│       └── db/migration/    # Flyway schema migrations (21 relational tables)
├── frontend/
│   ├── src/
│   │   ├── api/             # Axios API client with automatic JWT refresh interceptor
│   │   ├── components/      # UI components, layout scaffolding & notification bell
│   │   ├── context/         # AuthContext & role-based permission gatekeeper
│   │   ├── pages/           # Dashboard, Invoices, Detail, Approvals, Workflows, Settings
│   │   └── types/           # Strict TypeScript interface and enum contracts
│   └── package.json
├── samples/
│   ├── invoices/            # Pre-generated valid PDF invoices for operational testing
│   └── README.md            # Sample test scenarios reference guide
├── docs/
│   ├── ARCHITECTURE.md      # Detailed system architecture specification
│   ├── API_DESIGN.md        # RESTful API design & OpenAPI schemas
│   ├── DATABASE_DESIGN.md   # Relational schema specification & entity diagrams
│   ├── CURRENT_STATE.md     # Production architecture & delivery audit
│   ├── AI_EVALUATION.md     # AI Benchmark evaluation report (50 synthetic invoices)
│   ├── MANUAL_TESTING_GUIDE.md # Comprehensive end-to-end operational testing guide
│   ├── SECURITY_MODEL.md    # Multi-tenant isolation & RBAC security model
│   ├── TEST_STRATEGY.md     # Testing strategy & coverage standards
│   └── diagrams/            # Mermaid architecture, state machine & workflow diagrams
├── docker-compose.yml       # Production multi-container orchestration
├── Dockerfile.backend       # Multi-stage JDK 21 container build
├── Dockerfile.frontend      # Multi-stage Vite / Nginx container build
├── start.bat                # 1-click Windows startup script
├── start.ps1                # 1-click PowerShell startup script
└── README.md
```

---

## 8. License

OpsPilot is licensed under the [MIT License](LICENSE).
