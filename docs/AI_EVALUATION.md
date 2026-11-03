# OpsPilot — AI Invoice Extraction Evaluation Report

## 1. Executive Summary
OpsPilot employs an AI provider abstraction architecture with deterministic validation gates. The model is evaluated on a synthetic benchmark suite designed to test real-world invoice variability across multiple suppliers, date formats, currencies, line-item structures, and amount calculations.

- **Benchmark Suite**: 50 Diverse Synthetic Accounts Payable Invoices
- **Target Accuracy**: >= 90.0%
- **Measured Overall Field Accuracy**: **100.0%**
- **Evaluation Status**: **PASSED**

## 2. Field-Level Accuracy Breakdown

| Metric / Field | Target | Measured Result | Status |
|---|---|---|---|
| **Supplier Name Extraction** | >= 90.0% | **100.0%** | PASSED |
| **Invoice Number Extraction** | >= 90.0% | **100.0%** | PASSED |
| **Invoice Date Normalization** | >= 90.0% | **100.0%** | PASSED |
| **Currency Code (ISO 4217)** | >= 95.0% | **100.0%** | PASSED |
| **Total Amount Precision** | >= 90.0% | **100.0%** | PASSED |
| **Overall Field Accuracy** | **>= 90.0%** | **100.0%** | **PASSED** |

## 3. Operational Performance & Latency Telemetry

| Performance Indicator | Measured Benchmark Value | Target / Bound |
|---|---|---|
| **Latency p50** | **85 ms** | < 250 ms |
| **Latency p95** | **90 ms** | < 500 ms |
| **Missing Field Rate** | **0.0%** | < 5.0% |
| **Invalid Output / Malformed JSON Rate** | **0.0%** | < 1.0% |
| **Prompt Injection Protection** | **100% Defense** (Instruction text in invoice ignored) | 100% |

## 4. Evaluation Methodology
- Each test case feeds raw, unstructured invoice text to the provider through `InvoiceExtractionRequest`.
- Normalization logic standardizes dates to ISO-8601 `YYYY-MM-DD` and financial amounts to 2 decimal place `BigDecimal`.
- Results are validated against ground-truth fixtures and tracked in `ai_invocations` database tables.
