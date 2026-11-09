# OpsPilot — Changelog

## [1.1.0] - 2026-11-09
### Added
- **Notification Subsystem**:
  - In-app notification center with real-time unread badges, popover list, and one-click navigation to relevant records.
  - Multi-channel delivery architecture supporting in-app persistent notifications and mock email delivery.
- **Workflow Reliability & Operations**:
  - On-demand execution retry for paused or failed workflow nodes with exponential backoff handling.
  - Idempotent workflow execution replay.
  - Role escalation in approval tasks (from `APPROVER` to `ADMIN` or `ADMIN` to `OWNER`) with audit notes.
  - Automated post-approval workflow resumption directly proceeding to invoice accounting export.
- **Organization Settings & ERP Configuration**:
  - Configurable tenant operational policies: `approvalThresholdAmount`, `matchingToleranceAmount`, `duplicateWindowDays`, and `autoExportApproved`.
  - Dynamic integration of organization settings into `PurchaseOrderMatchingService` and `DeterministicValidationService`.
  - External ERP connector configuration (SAP S/4HANA, NetSuite SuiteTalk, QuickBooks) with live connection latency health checks.
- **Sample Invoices & Testing Infrastructure**:
  - Pre-generated valid sample PDF invoices in `samples/invoices/` covering standard PO match, high-tier approval, arithmetic mismatch, and low-confidence simulation.
  - Comprehensive `MANUAL_TESTING_GUIDE.md` detailing step-by-step procedures to verify every role, transition, and edge case.
  - Full end-to-end operational pipeline integration test (`EndToEndOperationalPipelineTest`) simulating complete invoice lifecycle from intake to ERP export.
  - Total test suite expanded to **45 passing tests (0 failures, 0 errors, 0 skipped)**.

## [1.0.0] - 2026-11-03
### Added
- **Full Backend Modular Monolith**:
  - Multi-tenant architecture with `organization_id` scoping on all entities, repositories, and controllers.
  - Role-based security model: `OWNER`, `ADMIN`, `OPERATOR`, `APPROVER`, `VIEWER` with JJWT 0.12.6 token rotation.
  - Binary `%PDF-` document intake, checksum SHA-256 fingerprinting, and pure-Java stream decompressor.
  - AI Provider SPI supporting deterministic `MockAiProvider` and structured Google `GeminiAiProvider`.
  - Deterministic financial validation engine enforcing line arithmetic, tax calculations, duplicate checks, and positive amounts.
  - Purchase order matching service with 2-way verification and configurable tolerances.
  - Visual DAG workflow engine with cycle detection, node execution tracking, lease-based job recovery, and immutable version publishing.
  - Human approval task inbox supporting `APPROVED`, `REJECTED`, and `CHANGES_REQUESTED` decisions with required justifications.
  - Accounting export subsystem with CSV, JSON, and ERP simulator targets protected by idempotency keys.
  - Append-only immutable compliance audit trail and real-time operational KPI analytics.
  - DataSeeder with realistic accounts, suppliers, purchase orders, workflows, and invoices.
- **Frontend SPA (React 18, Vite, TypeScript, TailwindCSS, TanStack Query)**:
  - App shell with multi-tenant context, role pills, and user avatar.
  - Quick 1-click demo role switcher on Login page (`admin@opspilot.io`, `approver@opspilot.io`, `operator@opspilot.io`, `viewer@opspilot.io`).
  - Interactive SVG Visual Workflow Builder with bezier connections, branch labels, node palette, and config drawer.
  - Split-screen Invoice Detail page with document preview, raw AI extraction inspector, validation checklist, and PO comparison card.
  - Interactive "Edit Details" modal on Invoice Detail to resolve extraction discrepancies and trigger re-validation.
  - Direct "Export to ERP" trigger on approved invoices.
  - Approvals Inbox with decision dialogs and contextual invoice metadata.
  - Supplier directory and Purchase Orders registry with line items accordion and creation dialogs.
  - Analytics and Audit Trail compliance pages.
- **DevOps & Infrastructure**:
  - Docker Compose orchestrating PostgreSQL 16, Spring Boot backend, and React Nginx frontend.
  - GitHub Actions CI workflow for JDK 21 backend and Node 20 frontend builds.
