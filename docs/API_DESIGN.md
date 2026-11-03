# OpsPilot — REST API Specification & Contracts

Base URL: `/api/v1`
Error Format: RFC 9457 Problem Details (`application/problem+json`)

```json
{
  "type": "https://opspilot.io/errors/validation-failed",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Invoice line item math does not match invoice total.",
  "instance": "/api/v1/invoices/123",
  "code": "INVOICE_TOTAL_MISMATCH",
  "correlationId": "corr-8f3a9b1c-99d4"
}
```

## 1. Authentication & Organization (`/api/v1/auth`, `/api/v1/organizations`)
- `POST /api/v1/auth/register`: Create user + tenant organization + OWNER membership.
- `POST /api/v1/auth/login`: Authenticate with email/password, return access JWT + set HttpOnly refresh cookie.
- `POST /api/v1/auth/refresh`: Rotate refresh token, issue fresh access JWT.
- `POST /api/v1/auth/logout`: Revoke active refresh session.
- `GET /api/v1/auth/me`: Fetch active user profile, memberships, and role permissions.
- `GET /api/v1/organizations/current`: Current organization details.
- `PATCH /api/v1/organizations/current`: Update organization settings.
- `GET /api/v1/organizations/current/members`: List tenant members with roles.
- `POST /api/v1/organizations/current/members`: Invite/create user membership.
- `PATCH /api/v1/organizations/current/members/{membershipId}`: Update user role.

## 2. Suppliers & Purchase Orders (`/api/v1/suppliers`, `/api/v1/purchase-orders`)
- `GET /api/v1/suppliers`: Paginated supplier list with search/filter.
- `POST /api/v1/suppliers`: Create supplier (unique `supplier_code`).
- `GET /api/v1/suppliers/{id}`: Supplier detail.
- `PATCH /api/v1/suppliers/{id}`: Update supplier details.
- `DELETE /api/v1/suppliers/{id}`: Deactivate supplier.
- `GET /api/v1/purchase-orders`: Paginated list of purchase orders.
- `POST /api/v1/purchase-orders`: Create PO with line items.
- `GET /api/v1/purchase-orders/{id}`: PO detail with lines and status.
- `PATCH /api/v1/purchase-orders/{id}`: Update PO details.

## 3. Invoices & Documents (`/api/v1/invoices`)
- `POST /api/v1/invoices/upload`: Multipart PDF upload (`file`, `purchaseOrderId` optional). Validates MIME/magic bytes, calculates SHA-256, stores document, creates `Invoice` in `RECEIVED` state, and enqueues workflow.
- `GET /api/v1/invoices`: Paginated invoice explorer with status, supplier, and date filtering.
- `GET /api/v1/invoices/{id}`: Complete invoice aggregate (lines, extraction confidence, validation results, matching outcome, workflow history).
- `PATCH /api/v1/invoices/{id}`: Operator correction endpoint (records before/after audit values).
- `POST /api/v1/invoices/{id}/reprocess`: Manually trigger re-extraction and workflow restart.
- `POST /api/v1/invoices/{id}/validate`: Run deterministic validation engine on demand.
- `GET /api/v1/invoices/{id}/document`: Secure streaming download of the uploaded PDF file.
- `GET /api/v1/invoices/{id}/history`: Detailed audit log trail for this specific invoice.

## 4. Workflows & Executions (`/api/v1/workflows`, `/api/v1/executions`)
- `GET /api/v1/workflows`: List workflow definitions in the tenant.
- `POST /api/v1/workflows`: Create workflow definition with graph JSON.
- `GET /api/v1/workflows/{id}`: Workflow graph and metadata.
- `PUT /api/v1/workflows/{id}`: Update workflow draft graph.
- `POST /api/v1/workflows/{id}/validate`: Validate graph topology and node configurations.
- `POST /api/v1/workflows/{id}/publish`: Publish an immutable `WorkflowVersion`.
- `GET /api/v1/workflows/{id}/versions`: Version history.
- `POST /api/v1/workflows/{id}/rollback/{versionId}`: Create a new publication from a previous version.
- `POST /api/v1/workflows/{id}/execute`: Trigger workflow execution manually.
- `GET /api/v1/executions`: List recent workflow executions.
- `GET /api/v1/executions/{executionId}`: Execution status, timing, and context.
- `GET /api/v1/executions/{executionId}/nodes`: Execution node-by-node timeline.
- `POST /api/v1/executions/{executionId}/cancel`: Cancel running/queued execution.
- `POST /api/v1/executions/{executionId}/retry`: Admin retry for failed execution jobs.

## 5. Approvals (`/api/v1/approvals`)
- `GET /api/v1/approvals/inbox`: Pending approval tasks assigned to the caller's role/user.
- `GET /api/v1/approvals/{taskId}`: Approval task detail with associated invoice and validation status.
- `POST /api/v1/approvals/{taskId}/decide`: Submit decision (`APPROVED`, `REJECTED`, `CHANGES_REQUESTED`). Resumes workflow.
- `POST /api/v1/approvals/{taskId}/escalate`: Escalate to admin/owner.

## 6. Exports & Integrations (`/api/v1/exports`, `/api/v1/integrations`)
- `POST /api/v1/invoices/{id}/export`: Export approved invoice (CSV, JSON, or mock ERP sync).
- `GET /api/v1/exports`: List export records and statuses.
- `GET /api/v1/integrations`: List configured external adapters.
- `POST /api/v1/integrations`: Register/configure integration.
- `POST /api/v1/integrations/{id}/test`: Test connectivity.

## 7. Analytics & Audit (`/api/v1/analytics`, `/api/v1/audit-events`)
- `GET /api/v1/analytics/overview`: Operational KPIs (invoices processed, auto-pass rate, cycle time).
- `GET /api/v1/analytics/invoices`: Invoices by status, volume by date.
- `GET /api/v1/analytics/ai-usage`: Total AI tokens, estimated cost, average latency.
- `GET /api/v1/audit-events`: Searchable append-only audit trail with entity and actor filters.
