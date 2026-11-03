# OpsPilot — Changelog

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
- **Automated Verification & Tests**:
  - 24 comprehensive tests covering AI benchmark (100% accuracy over 50 invoices), approval transitions, export idempotency, arithmetic checks, and PO matching.
  - Zero TypeScript compile or bundling errors on Vite production build.
- **DevOps & Infrastructure**:
  - Docker Compose orchestrating PostgreSQL 16, Spring Boot backend, and React Nginx frontend.
  - GitHub Actions CI workflow for JDK 21 backend and Node 20 frontend builds.
