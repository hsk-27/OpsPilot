# OpsPilot — Manual Verification & Testing Guide

This guide provides step-by-step procedures to manually test and verify every module, role boundary, exception case, and workflow transition in OpsPilot.

---

## 1. Environment Startup

### 1.1 Start the Backend
Open a terminal in the project directory:
```powershell
cd C:\Users\Dell\.gemini\antigravity\scratch\opspilot\backend
.\mvnw.cmd spring-boot:run
```
*Expected Output:*
- Spring Boot starts on port `8080`.
- Flyway executes schema initialization (21 tables).
- `DataSeeder` logs: `Operational AP data seeded successfully: 4 users, 3 suppliers, 2 POs, 3 invoices, 1 pending approval task.`

### 1.2 Start the Frontend
In a second terminal:
```powershell
cd C:\Users\Dell\.gemini\antigravity\scratch\opspilot\frontend
npm.cmd run dev
```
*Expected Output:*
- Vite starts the dev server at `http://localhost:5173`.
- Open `http://localhost:5173` in your browser (Chrome / Edge / Firefox).

---

## 2. Pre-Seeded Test Accounts

The login screen features **1-click quick selector pills** to instantly populate credentials for any role:

| Persona | Email | Password | Role | Primary Operational Capabilities |
|---|---|---|---|---|
| **Sarah Connor** | `admin@opspilot.io` | `Password123!` | `OWNER` / `ADMIN` | Full organization settings, workflow builder/publishing, user management |
| **Michael Scott** | `approver@opspilot.io` | `Password123!` | `APPROVER` | Reviews and authorizes pending invoice approval tasks, can escalate |
| **Pam Beesly** | `operator@opspilot.io` | `Password123!` | `OPERATOR` | Uploads invoices, reviews AI extractions, edits discrepancies |
| **Jim Halpert** | `viewer@opspilot.io` | `Password123!` | `VIEWER` | Read-only inspection of invoices, analytics, and audit logs |

---

## 3. Step-by-Step Test Scenarios

### Scenario 1: Happy Path Invoice Ingestion, AI Extraction & PO Matching
**Objective:** Verify invoice intake, pure Java PDF text parsing, AI extraction, deterministic math validation, and 2-way PO matching.

1. Log in as **Pam Beesly** (`operator@opspilot.io`).
2. Navigate to **Invoices** from the sidebar (`/invoices`).
3. Click the **Upload Invoice** button in the top right.
4. In the upload modal:
   - Click to browse or drag-and-drop: `samples/invoices/01_acme_industrial_standard.pdf`.
   - In the **Purchase Order** dropdown, select `PO-2026-9001 (Acme Industrial Supplies Ltd - $11,800.00)`.
   - Click **Upload & Process**.
5. **Verify Immediate Pipeline Triggering:**
   - The invoice is stored securely (verifying `%PDF-` signature & SHA-256 fingerprint).
   - AI extraction runs:
     - Supplier extracted: `Acme Industrial Supplies Ltd`
     - Invoice number extracted: `INV-2026-8801`
     - Invoice date: `2026-11-05`
     - Total: `$11,800.00`
   - Deterministic checks pass: mathematical line totals equal subtotal + tax.
   - 2-way PO matching executes: verifies supplier and amount against `PO-2026-9001`.
   - Status updates to `PENDING_APPROVAL` because total amount ($11,800.00) exceeds threshold ($5,000.00).

---

### Scenario 2: Multi-Role Approval, Escalation & Post-Approval Auto-Export
**Objective:** Verify human approval inbox, operator escalation to management, decision recording, and automatic resumption into accounting export.

1. Log in as **Michael Scott** (`approver@opspilot.io`).
2. Navigate to **Approvals** (`/approvals`).
3. Notice the pending approval task in the inbox.
4. **Test Escalation:**
   - Click **Escalate**.
   - Enter reason: *"Total exceeds standard departmental sign-off tier"*.
   - Click **Confirm Escalation**.
   - Notice the assigned role immediately updates from `APPROVER` to `ADMIN`.
   - Notice an in-app notification appears on the notification bell.
5. **Test Approval Decision:**
   - Switch user to **Sarah Connor** (`admin@opspilot.io`) or use the assigned approver.
   - In the approval modal, review:
     - Invoice lines & financial totals
     - PO matching outcome (`MATCHED`)
     - AI confidence scores
   - Enter decision comment: *"PO and delivery verified. Approved for payment preparation."*
   - Click **Approve**.
6. **Verify Automatic Workflow Resumption:**
   - The approval task moves to `DECIDED`.
   - The workflow engine automatically unpauses the execution.
   - The pipeline continues to the `EXPORT_INVOICE` node.
   - Navigate to **Invoices**: verify the invoice status is now `EXPORTED` / `APPROVED`.
   - Navigate to **Executions**: verify execution status is `COMPLETED`.

---

### Scenario 3: Exception Handling & Discrepancy Resolution (Arithmetic Mismatch)
**Objective:** Verify that mathematical discrepancies trigger deterministic rule failures and route invoices to `NEEDS_REVIEW`.

1. Log in as **Pam Beesly** (`operator@opspilot.io`).
2. Click **Upload Invoice** and select `samples/invoices/03_apex_office_needs_review.pdf`.
3. Leave Purchase Order unlinked and submit.
4. **Verify Exception Flagging:**
   - In the invoice list, notice the status is **NEEDS_REVIEW** with a red warning badge.
   - Click the invoice row to open **Invoice Detail** (`/invoices/:id`).
   - Notice the **Validation Results** panel:
     - `RULE_TOTAL_MATH_CONSISTENCY` is marked as **FAILED**.
     - Review message shows stated total differs from subtotal + tax.
5. **Test Manual Correction:**
   - Click **Edit Invoice**.
   - Adjust the line items or subtotal so that line items + tax match total.
   - Click **Save Changes**.
   - The system re-evaluates deterministic validation rules in real time.
   - Notice the status updates to `PASSED` / `READY_FOR_APPROVAL`.

---

### Scenario 4: Visual Workflow Builder & Graph Validation
**Objective:** Verify the visual DAG workflow definition canvas and graph topology verification.

1. Log in as **Sarah Connor** (`admin@opspilot.io`).
2. Navigate to **Workflows** (`/workflows`).
3. Inspect the interactive React Flow DAG canvas:
   - Nodes: `START` $\to$ `AI_EXTRACTION` $\to$ `VALIDATE_INVOICE` $\to$ `MATCH_PURCHASE_ORDER` $\to$ `CONDITION` $\to$ `APPROVAL` $\to$ `EXPORT_INVOICE` $\to$ `END`.
4. Click on any node to view and edit its configuration in the right inspector drawer:
   - For `APPROVAL`: inspect `approverRole` and `timeoutHours`.
   - For `EXPORT_INVOICE`: inspect `format` (`CSV`, `JSON`, `MOCK_ERP`).
5. **Test Graph Validation:**
   - Click **Validate Workflow**.
   - The backend checks for cycles, disconnected subgraphs, and reachability.
   - Notice the notification confirming valid DAG topology.
6. **Test Publishing:**
   - Click **Publish New Version**.
   - Notice version bumps (e.g. `v1` $\to$ `v2`) with immutable history preserved.

---

### Scenario 5: Pipeline Telemetry, Retry, and Replay
**Objective:** Verify durable execution tracking, node telemetry, and error recovery.

1. Navigate to **Executions** (`/executions`).
2. Inspect the execution table:
   - Shows Execution ID, Trigger, Invoice Number, Current Node, Status, and Started At.
3. Click on any execution to view step telemetry:
   - Node-by-node execution time in milliseconds.
   - Step input and output JSON snapshots.
4. **Test Actions:**
   - For an execution with a paused or failed step: click **Retry Step** to trigger idempotent resumption.
   - Click **Replay Pipeline** to restart a pipeline with a clean execution run.

---

### Scenario 6: Role-Based Access Control (RBAC) Enforcement
**Objective:** Confirm backend security prevents unauthorized actions across user roles.

1. Log in as **Jim Halpert** (`viewer@opspilot.io`).
2. Verify that write actions are disabled or restricted:
   - Cannot upload invoices.
   - Cannot decide or escalate approval tasks.
   - Cannot edit or publish workflows.
   - Cannot modify organization settings.
3. Attempting any write operation via direct API curl / REST returns `403 FORBIDDEN`.

---

### Scenario 7: In-App Notification Center
**Objective:** Verify real-time in-app notifications and unread counter badges.

1. Notice the **Notification Bell** icon in the top header.
2. If there are unread notifications, a red badge with the count is displayed.
3. Click the bell to open the popover dropdown:
   - Displays notification title, message, timestamp, and category badge (`APPROVAL_REQUIRED`, `DISCREPANCY_FLAGGED`, `SYSTEM`).
4. Click any notification:
   - Automatically navigates to the relevant page (e.g. `/approvals`).
5. Click **Mark All as Read**:
   - Badge clears and notifications update to read status.

---

### Scenario 8: Organization Settings & ERP Integrations
**Objective:** Verify administrative configuration and integration health checks.

1. Log in as **Sarah Connor** (`admin@opspilot.io`).
2. Navigate to **Settings** (`/settings`).
3. **Operational Policies Tab:**
   - View / Edit **Approval Threshold Amount** (default: `$5,000.00`).
   - View / Edit **PO Matching Tolerance** (default: `$1.00`).
   - View / Edit **Duplicate Window Days** (default: `90`).
   - Toggle **Auto-Export on Approval**.
   - Click **Save Changes** and verify toast confirmation.
4. **ERP & Accounting Integrations Tab:**
   - Inspect configured adapters:
     - `Mock ERP Accounting Adapter` (`MOCK_ERP`)
     - `SAP S/4HANA Connector` (`SAP`)
     - `NetSuite SuiteTalk REST Adapter` (`NETSUITE`)
   - Click **Test Connection** on any integration.
   - Verify connection status turns to `CONNECTED` with latency metric (e.g. `42ms`).

---

### Scenario 9: Audit Trail & Export Verification
**Objective:** Verify the append-only ledger and structured accounting exports.

1. Navigate to **Audit Trail** (`/audit`).
2. Verify that all actions executed in Scenarios 1–8 are logged:
   - `INVOICE_UPLOADED`
   - `INVOICE_UPDATED`
   - `APPROVAL_ESCALATED`
   - `APPROVAL_DECISION_APPROVED`
   - `SETTINGS_UPDATED`
3. Click on any event to inspect:
   - Actor User ID & Type (`USER` vs `SYSTEM`)
   - Entity ID & Target
   - Before and After state JSON payloads
4. **Test Accounting Export:**
   - Navigate to **Invoices** and select an approved invoice.
   - In the export menu, choose **CSV** or **JSON**.
   - Confirm structured financial payload is generated and downloaded.

---

## 4. Problem-Hunting & Edge Cases Checklist

When manually probing for bugs or edge cases, test these specific boundary conditions:

| Check | Steps | Expected Result |
|---|---|---|
| **Corrupted PDF Upload** | Rename an image/text file to `.pdf` or upload an empty file. | Backend rejects with HTTP 400 (`INVALID_PDF_SIGNATURE` or `EMPTY_FILE`). Toast error displayed. |
| **Duplicate Invoice Submission** | Upload `01_acme_industrial_standard.pdf` twice for the same supplier. | Second upload is flagged by deterministic validation as `RULE_DUPLICATE_CHECK` failure. |
| **Double Decision on Approval** | Approve an invoice, then attempt to reject or approve again. | Returns HTTP 409 Conflict (`TASK_ALREADY_DECIDED`). |
| **Zero / Negative Financial Amount** | Manually edit an invoice subtotal to `-100.00` or `0.00`. | Flagged by `RULE_NON_NEGATIVE_SUBTOTAL` or `RULE_POSITIVE_TOTAL`. |
| **Tolerance Boundary** | In Settings, set PO tolerance to `$5.00`. Submit invoice exceeding PO by `$3.00` and `$10.00`. | $3 overage is `PARTIALLY_MATCHED`; $10 overage is `NEEDS_REVIEW`. |
| **JWT Expiry & Refresh** | Leave the app idle or delete access token from localStorage. | App uses refresh cookie to rotate tokens without requiring user re-login. |
