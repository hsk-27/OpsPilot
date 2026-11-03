# OpsPilot — Database Schema Design & Migrations

## 1. Design Principles
- **PostgreSQL 15+** as the authoritative source of truth.
- **UUID Primary Keys** across all business and execution entities (`gen_random_uuid()`).
- **Audit Columns**: `created_at` (TIMESTAMPTZ, default `NOW()`), `updated_at` (TIMESTAMPTZ, default `NOW()`).
- **Financial Precision**: `NUMERIC(15, 2)` or `NUMERIC(18, 4)` for all currency values and tax rates (`BigDecimal` in Java). Floats are strictly prohibited.
- **Strict Multi-Tenancy**: `organization_id` foreign key on every tenant-owned table with composite indices for tenant-scoped lookups.
- **Idempotency & Concurrency**: Unique constraints on composite business keys, lease timestamps for job execution, optimistic locking `version` integers on critical records.

## 2. Entity Relational Model

```
+------------------+         +--------------------------+
|  organizations   |<------->|  organization_memberships|
+--------+---------+         +------------+-------------+
         |                                |
         |                                v
         |                     +--------------------+
         |                     |       users        |
         |                     +--------------------+
         |
         +-----------------------+----------------------+--------------------+
         |                       |                      |                    |
         v                       v                      v                    v
+-----------------+     +------------------+    +----------------+   +----------------------+
|    suppliers    |     | purchase_orders  |    |   documents    |   | workflow_definitions |
+--------+--------+     +--------+---------+    +-------+--------+   +----------+-----------+
         |                       |                      |                       |
         |         +-------------+                      |                       v
         |         |                                    |            +----------------------+
         v         v                                    v            |  workflow_versions   |
+-------------------------------------------------------------+      +----------+-----------+
|                          invoices                           |                 |
+------------------------------+------------------------------+                 v
                               |                                     +----------------------+
                               +------------------------------------>| workflow_executions  |
                                                                     +----------+-----------+
                                                                                |
                                                                                v
                                                                     +----------------------+
                                                                     | workflow_node_execs  |
                                                                     +----------+-----------+
                                                                                |
                                                                                v
                                                                     +----------------------+
                                                                     |    approval_tasks    |
                                                                     +----------+-----------+
                                                                                |
                                                                                v
                                                                     +----------------------+
                                                                     |  approval_decisions  |
                                                                     +----------------------+
```

## 3. Table Catalog

### 3.1 `organizations`
- `id` (UUID PK)
- `name` (VARCHAR(150) NOT NULL)
- `slug` (VARCHAR(100) NOT NULL UNIQUE)
- `default_currency` (VARCHAR(3) NOT NULL DEFAULT 'USD')
- `timezone` (VARCHAR(50) NOT NULL DEFAULT 'UTC')
- `status` (VARCHAR(20) NOT NULL DEFAULT 'ACTIVE')
- `created_at`, `updated_at` (TIMESTAMPTZ NOT NULL)

### 3.2 `users`
- `id` (UUID PK)
- `email` (VARCHAR(255) NOT NULL UNIQUE)
- `password_hash` (VARCHAR(255) NOT NULL)
- `first_name` (VARCHAR(100) NOT NULL)
- `last_name` (VARCHAR(100) NOT NULL)
- `status` (VARCHAR(20) NOT NULL DEFAULT 'ACTIVE')
- `created_at`, `updated_at` (TIMESTAMPTZ NOT NULL)
- `last_login_at` (TIMESTAMPTZ)

### 3.3 `organization_memberships`
- `id` (UUID PK)
- `organization_id` (UUID NOT NULL REFERENCES organizations(id))
- `user_id` (UUID NOT NULL REFERENCES users(id))
- `role` (VARCHAR(30) NOT NULL) -- OWNER, ADMIN, OPERATOR, APPROVER, VIEWER
- `status` (VARCHAR(20) NOT NULL DEFAULT 'ACTIVE')
- `created_at`, `updated_at` (TIMESTAMPTZ NOT NULL)
- *Constraint*: UNIQUE (`organization_id`, `user_id`)

### 3.4 `refresh_token_sessions`
- `id` (UUID PK)
- `user_id` (UUID NOT NULL REFERENCES users(id))
- `token_hash` (VARCHAR(255) NOT NULL UNIQUE)
- `expires_at` (TIMESTAMPTZ NOT NULL)
- `revoked_at` (TIMESTAMPTZ)
- `created_at` (TIMESTAMPTZ NOT NULL)

### 3.5 `suppliers`
- `id` (UUID PK)
- `organization_id` (UUID NOT NULL REFERENCES organizations(id))
- `supplier_code` (VARCHAR(50) NOT NULL)
- `name` (VARCHAR(200) NOT NULL)
- `tax_identifier` (VARCHAR(50))
- `email` (VARCHAR(255))
- `address` (TEXT)
- `default_currency` (VARCHAR(3) NOT NULL DEFAULT 'USD')
- `status` (VARCHAR(20) NOT NULL DEFAULT 'ACTIVE')
- `created_at`, `updated_at` (TIMESTAMPTZ NOT NULL)
- *Constraint*: UNIQUE (`organization_id`, `supplier_code`)

### 3.6 `purchase_orders` & `purchase_order_lines`
- `purchase_orders`: `id`, `organization_id`, `supplier_id`, `purchase_order_number`, `issue_date`, `currency`, `subtotal`, `tax_amount`, `total_amount`, `status`, timestamps.
  - *Constraint*: UNIQUE (`organization_id`, `purchase_order_number`)
- `purchase_order_lines`: `id`, `purchase_order_id`, `description`, `quantity`, `unit_price`, `tax_rate`, `line_total`.

### 3.7 `documents`
- `id` (UUID PK)
- `organization_id` (UUID NOT NULL REFERENCES organizations(id))
- `original_filename` (VARCHAR(255) NOT NULL)
- `storage_key` (VARCHAR(255) NOT NULL UNIQUE)
- `mime_type` (VARCHAR(100) NOT NULL)
- `file_size` (BIGINT NOT NULL)
- `checksum_sha256` (VARCHAR(64) NOT NULL)
- `document_type` (VARCHAR(50) NOT NULL DEFAULT 'INVOICE_PDF')
- `processing_status` (VARCHAR(30) NOT NULL DEFAULT 'STORED')
- `uploaded_by` (UUID REFERENCES users(id))
- `created_at` (TIMESTAMPTZ NOT NULL)

### 3.8 `invoices` & `invoice_lines`
- `invoices`: `id`, `organization_id`, `supplier_id`, `purchase_order_id`, `invoice_number`, `invoice_date`, `due_date`, `currency`, `subtotal`, `tax_amount`, `total_amount`, `extracted_data` (JSONB), `validation_status` (PENDING, PASSED, FAILED, WARNING), `approval_status` (NOT_REQUIRED, PENDING, APPROVED, REJECTED, CHANGES_REQUESTED), `processing_status` (RECEIVED, EXTRACTED, VALIDATING, NEEDS_REVIEW, READY_FOR_APPROVAL, PENDING_APPROVAL, APPROVED, REJECTED, EXPORT_PENDING, EXPORTED, FAILED), `document_id`, `workflow_execution_id`, `version` (INT NOT NULL DEFAULT 0), timestamps.
  - *Duplicate Detection Index*: UNIQUE (`organization_id`, `supplier_id`, `invoice_number`, `currency`) WHERE status != 'REJECTED'
- `invoice_lines`: `id`, `invoice_id`, `description`, `quantity`, `unit_price`, `tax_rate`, `line_total`.

### 3.9 `workflow_definitions` & `workflow_versions`
- `workflow_definitions`: `id`, `organization_id`, `name`, `description`, `status` (DRAFT, ACTIVE, ARCHIVED), `created_by`, timestamps.
- `workflow_versions`: `id`, `workflow_definition_id`, `version_number` (INT NOT NULL), `graph_definition_json` (JSONB NOT NULL), `validation_result_json` (JSONB), `published_at`, `published_by`, timestamps.
  - *Constraint*: UNIQUE (`workflow_definition_id`, `version_number`)

### 3.10 `workflow_executions` & `workflow_node_executions`
- `workflow_executions`: `id`, `organization_id`, `workflow_definition_id`, `workflow_version_id`, `business_reference_type`, `business_reference_id`, `status` (QUEUED, RUNNING, WAITING_APPROVAL, WAITING_RETRY, COMPLETED, FAILED, CANCELLED, DEAD_LETTERED), `trigger_type`, `input_snapshot` (JSONB), `execution_context` (JSONB), `started_at`, `completed_at`, `last_heartbeat_at`, `error_code`, `error_message`, `version`, timestamps.
- `workflow_node_executions`: `id`, `workflow_execution_id`, `node_id`, `node_type`, `status` (PENDING, RUNNING, WAITING_APPROVAL, WAITING_RETRY, COMPLETED, FAILED, SKIPPED, CANCELLED), `attempt_number`, `input_snapshot` (JSONB), `output_snapshot` (JSONB), `error_details` (TEXT), `started_at`, `completed_at`, `duration_ms`, `idempotency_key` (VARCHAR(255) UNIQUE), timestamps.

### 3.11 `workflow_jobs` (Durable Job Queue)
- `id` (UUID PK)
- `organization_id` (UUID NOT NULL)
- `workflow_execution_id` (UUID NOT NULL)
- `node_execution_id` (UUID)
- `job_type` (VARCHAR(50) NOT NULL)
- `status` (VARCHAR(30) NOT NULL) -- PENDING, LEASED, COMPLETED, FAILED, DEAD_LETTERED
- `priority` (INT NOT NULL DEFAULT 0)
- `available_at` (TIMESTAMPTZ NOT NULL)
- `attempt_count` (INT NOT NULL DEFAULT 0)
- `max_attempts` (INT NOT NULL DEFAULT 3)
- `lease_owner` (VARCHAR(100))
- `lease_expires_at` (TIMESTAMPTZ)
- `last_error` (TEXT)
- `created_at`, `updated_at` (TIMESTAMPTZ NOT NULL)

### 3.12 `approval_tasks` & `approval_decisions`
- `approval_tasks`: `id`, `organization_id`, `workflow_execution_id`, `node_execution_id`, `invoice_id`, `assigned_user_id`, `assigned_role`, `status` (PENDING, DECIDED, CANCELLED), `decision_deadline`, timestamps.
- `approval_decisions`: `id`, `approval_task_id`, `decided_by` (UUID NOT NULL), `decision` (VARCHAR(30) NOT NULL), `comments` (TEXT), `previous_data` (JSONB), `modified_data` (JSONB), `created_at`.

### 3.13 `audit_events` & `outbox_events`
- `audit_events`: `id`, `organization_id`, `actor_user_id`, `actor_type` (USER, SYSTEM, AI), `action` (VARCHAR(100) NOT NULL), `entity_type` (VARCHAR(50) NOT NULL), `entity_id` (VARCHAR(100) NOT NULL), `before_state` (JSONB), `after_state` (JSONB), `metadata` (JSONB), `correlation_id` (VARCHAR(100)), `created_at`.
- `outbox_events`: `id`, `organization_id`, `aggregate_type`, `aggregate_id`, `event_type`, `payload` (JSONB), `status` (PENDING, PUBLISHED, FAILED), `created_at`, `published_at`, `attempt_count`, `last_error`.

### 3.14 `ai_invocations` & `export_records`
- `ai_invocations`: `id`, `organization_id`, `invoice_id`, `provider`, `model`, `prompt_version`, `request_timestamp`, `response_timestamp`, `latency_ms`, `input_tokens`, `output_tokens`, `estimated_cost`, `currency`, `status`, `error_code`.
- `export_records`: `id`, `organization_id`, `invoice_id`, `export_format` (CSV, JSON, ERP_API), `destination`, `idempotency_key` (VARCHAR(255) UNIQUE), `status` (PENDING, COMPLETED, FAILED), `payload_summary` (TEXT), `error_message`, `exported_at`, timestamps.
