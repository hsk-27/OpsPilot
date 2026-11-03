# OpsPilot — Architecture Specification

## 1. Executive Summary
OpsPilot is an AI-assisted business operations automation SaaS platform designed for Accounts Payable (AP) and supplier invoice processing. It pairs probabilistic AI invoice extraction with strict, deterministic business validation, a durable state-machine workflow engine, human-in-the-loop approvals, and controlled accounting export.

OpsPilot is explicitly **not** an unrestricted autonomous agent. It never independently authorizes monetary transactions or alters historical audit logs.

## 2. High-Level Modular Monolith Architecture

```
+-------------------------------------------------------------------------+
|                       REACT 18 + TYPESCRIPT SPA                         |
|   (Dashboard, Invoices, Approvals, Suppliers, POs, Workflows, Canvas)   |
+------------------------------------+------------------------------------+
                                     | REST / HTTPS (JSON & Multipart)
                                     v
+-------------------------------------------------------------------------+
|                    SPRING BOOT 3.3.5 MODULAR MONOLITH                   |
|                                                                         |
|  [Security & Auth]  <-->  [Organization & Tenant Context]                |
|  [Supplier Domain]  <-->  [Purchase Order Domain]                       |
|  [Invoice Intake]   <-->  [Document Storage & PDF Parsing]              |
|  [AI Provider SPI]  <-->  [Mock AI / Gemini API Adapter]                |
|  [Deterministic Validation Engine & PO Matching]                        |
|  [Durable Workflow Engine & PostgreSQL Job Queue]                       |
|  [Human Approval Subsystem & Task Inbox]                                |
|  [Accounting Export Adapters (CSV, JSON, Mock ERP)]                     |
|  [Audit Logging & Outbox Event Publisher]                               |
|  [Prometheus Metrics & Actuator Health Observability]                   |
+------------------------------------+------------------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
|                     POSTGRESQL RELATIONAL DATABASE                      |
| (Multi-tenant schema, Flyway migrations, transactional state, JSONB)    |
+-------------------------------------------------------------------------+
```

## 3. Package & Module Boundaries
Package root: `com.opspilot`
- `common/`: Global configurations, RFC 9457 Problem Details exception handler, Tenant context holder, Base entities, Security utilities.
- `identity/`: Authentication, User, RefreshTokenSession, Role (OWNER, ADMIN, OPERATOR, APPROVER, VIEWER).
- `organization/`: Organization, OrganizationMembership, multi-tenancy enforcement.
- `supplier/`: Supplier entity, supplier code uniqueness, status, currency.
- `purchaseorder/`: PurchaseOrder & PurchaseOrderLine entities, totals, lines, status.
- `document/`: File upload security, SHA-256 checksum, local/private file storage, PDF text extraction SPI.
- `ai/`: Provider abstraction (`AiProvider`), `MockAiProvider`, `GeminiAiProvider`, prompt management, structured extraction DTOs, confidence scoring, usage metering.
- `invoice/`: Invoice & InvoiceLine entities, lifecycle states (`RECEIVED` -> `EXTRACTED` -> `VALIDATING` -> `READY_FOR_APPROVAL` -> `PENDING_APPROVAL` -> `APPROVED` -> `EXPORTED`), correction tracking.
- `validation/`: Deterministic business validator, financial arithmetic, duplicate detection, PO matcher.
- `workflow/`: Durable state machine, graph validation, node executor registry (`START`, `AI_EXTRACTION`, `VALIDATE_INVOICE`, `MATCH_PURCHASE_ORDER`, `CONDITION`, `APPROVAL`, `NOTIFICATION`, `EXPORT_INVOICE`, `MANUAL_REVIEW`, `END`), PostgreSQL job queue (`WorkflowJob`), exponential backoff retries, lease expiration recovery, idempotency enforcement.
- `approval/`: ApprovalTask, ApprovalDecision, concurrency locking, decision validation (`APPROVED`, `REJECTED`, `CHANGES_REQUESTED`, `ESCALATED`).
- `export/`: Accounting export SPI (`AccountingExportProvider`), CSV & JSON exporters, mock ERP adapter, idempotency tracking.
- `audit/`: Append-only audit events, correlation ID tracking.
- `analytics/`: Operational AP metrics (cycle times, pass rates, queue backlog, AI usage).

## 4. Multi-Tenant Isolation Strategy
- All tables containing tenant data store `organization_id` (UUID).
- The `TenantContext` is established via JWT Bearer authentication filter.
- Every repository query or service lookup explicitly scopes by `organization_id`.
- Foreign key references cross-validated against the caller's organization.
