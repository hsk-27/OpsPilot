# OpsPilot — Changelog

## [1.2.0] - 2026-11-14
### Added
- **Deep 2-Way Line-Level Purchase Order Reconciliation**:
  - Implemented itemized line reconciliation via `LineMatchResult` DTO.
  - Line-by-line comparison of billed quantities, authorized PO limits, unit rates, and variance amounts.
  - Support for partial delivery tracking and unbilled PO line item balances.
- **Deterministic Validation Rule Extensions**:
  - `RULE_DUE_DATE_AFTER_INVOICE_DATE`: Enforces that due dates cannot precede invoice issue dates.
  - `RULE_INVOICE_DATE_NOT_EXTREME_FUTURE`: Rejects invoices with issue dates beyond a 30-day future posting window.
  - `RULE_LINE_ITEM_ARITHMETIC`: Verifies that individual line item totals match quantity multiplied by unit price within tolerance.
- **Enterprise Institutional Design System**:
  - Refined FinTech aesthetic inspired by institutional financial platforms (Obsidian and Emerald theme).
  - Streamlined data tables, status badges, and split-screen document reconciliation.
  - Automated test suite expanded to **52 passing tests across 17 test suites (0 failures, 0 errors, 0 skipped)**.

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
