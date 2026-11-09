# OpsPilot Sample Invoices for Manual Verification

This directory contains pre-generated, valid PDF invoices designed for immediate manual end-to-end testing of OpsPilot:

| File | Supplier | Stated Total | Target PO | Expected System Behavior |
|---|---|---|---|---|
| [`01_acme_industrial_standard.pdf`](01_acme_industrial_standard.pdf) | Acme Industrial Supplies Ltd | $11,800.00 | `PO-2026-9001` | **Full Match & Approval**: Matches PO amount and supplier. Triggers approval task for high-tier invoice (>$5,000). Once approved, automatically continues to export. |
| [`02_cloudscale_hosting_approval.pdf`](02_cloudscale_hosting_approval.pdf) | CloudScale Hosting Inc | $3,540.00 | `PO-2026-9002` | **PO Match**: Validates against CloudScale PO. In standard workflow, pauses for human review or auto-resumes. |
| [`03_apex_office_needs_review.pdf`](03_apex_office_needs_review.pdf) | Apex Office Essentials | $600.00 | None | **Deterministic Arithmetic Discrepancy**: Stated total ($600.00) differs from line items ($450 + $50 tax = $500). Flags mathematical discrepancy rule failure and routes to `NEEDS_REVIEW`. |
| [`04_simulated_low_confidence.pdf`](04_simulated_low_confidence.pdf) | Acme Industrial Supplies Ltd | $1,180.00 | None | **Low AI Confidence Simulation**: Triggers the `SIMULATE_LOW_CONFIDENCE` provider branch, producing confidence scores under the threshold to verify exception handling. |

### How to use during manual testing

1. Log in to OpsPilot at `http://localhost:5173` as **Pam Beesly** (`operator@opspilot.io` / `Password123!`).
2. Navigate to **Invoices** and click **Upload Invoice**.
3. Select any of the PDF files above. You can optionally link the corresponding Purchase Order.
4. Watch the pipeline ingest the PDF, extract metadata via AI, execute deterministic checks, and update the invoice status!
