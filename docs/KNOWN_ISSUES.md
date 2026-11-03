# OpsPilot — Known Issues & Operational Limitations

## 1. Initial Release Scope Constraints
- **Document Format**: Current intake strictly supports PDF format. Scanned image invoices without embedded text or OCR metadata require manual operator review; OCR adapter is architected as an optional SPI.
- **External Network Isolation**: In restricted sandbox environments without live Gemini API keys, the system operates deterministically using `MockAiProvider` with comprehensive synthetic fixtures.
- **Database Concurrency**: In environments where PostgreSQL Docker daemon is not running on port 5432, tests and local execution run against the Spring Boot embedded/H2 in PostgreSQL compatibility mode while retaining identical Flyway SQL syntax.
- **Payment Execution**: OpsPilot intentionally does not perform bank transfers or hold credit cards; export preparation generates structured CSV/JSON payloads and mock ERP integrations.
