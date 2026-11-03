# OpsPilot — Security Architecture & Threat Model

## 1. Authentication & Session Management
- **Password Hashing**: BCrypt with strength factor 12 (or Argon2id). No plaintext passwords ever stored or logged.
- **JWT Signing**: HMAC-SHA256 with 256-bit+ secure random secret configured via `JWT_SECRET`. Tokens expire in 15 minutes.
- **Refresh Token Rotation**: Refresh tokens are single-use, persisted in `refresh_token_sessions` with SHA-256 hashed values. Used or expired tokens are immediately revoked.
- **Cookie Security**: In web environments, refresh tokens are delivered via `HttpOnly`, `Secure`, `SameSite=Lax` cookies.

## 2. Multi-Tenancy & Tenant Isolation
- **Tenant Context**: Injected via Spring Security filter into `TenantContext` ThreadLocal from verified JWT claims.
- **Enforcement**: All database queries MUST include `WHERE organization_id = :orgId`.
- **Cross-Tenant Prevention**: Direct Object Reference (BOLA/IDOR) attacks are defended by validating entity ownership before every update, download, or decision.

## 3. Role-Based Access Control (RBAC)
Permissions mapped to system roles:
- `OWNER`: Full organization control, settings, user role elevation, billing/export settings.
- `ADMIN`: Operational management, workflows publication, suppliers, POs, admin retries.
- `OPERATOR`: Invoice upload, manual review/correction, initiating re-processing.
- `APPROVER`: Reviewing and deciding on assigned approval tasks.
- `VIEWER`: Read-only access to invoices, executions, and audit records.

Spring Security `@PreAuthorize` annotations combined with domain-level assignment checks guarantee that an `APPROVER` cannot modify workflows and an `OPERATOR` cannot approve financial records.

## 4. Secure File Handling (Invoice Intake)
- **File Validation**:
  - Max upload size: 10 MB (enforced at web layer).
  - Extension check (`.pdf`).
  - Magic byte / signature inspection (`%PDF-` header verified, not just MIME header).
  - SHA-256 hash calculated for tamper detection and duplicate file flagging.
- **Storage Isolation**: Files stored in a private directory outside web roots with random UUID keys (`storage_key`). Physical paths are never exposed to clients.
- **Path Traversal Defense**: All filenames sanitized (`FilenameUtils.getName()`); paths constructed strictly from trusted UUID keys.

## 5. AI Security & Prompt Injection Defense
- **Untrusted Input**: Document text extracted from PDFs is treated as untrusted user data.
- **Prompt Isolation**: System instructions strictly separate delimiters and instruct the model that invoice contents must only be treated as data to be extracted, never as instructions to follow.
- **Deterministic Validation Barrier**: AI output is never trusted without passing deterministic schema, numeric, and consistency checks. High confidence scores cannot bypass failed math or invalid supplier rules.
- **No Unrestricted Agents**: AI models have zero tool access to execute database updates or financial approvals.

## 6. Audit Trail & Immutability
- Consequential actions (logins, uploads, corrections, decisions, publications, exports) produce append-only `AuditEvent` records.
- No HTTP endpoints exist to modify or delete audit rows.
