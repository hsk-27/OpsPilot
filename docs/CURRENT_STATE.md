# OpsPilot — Current State Audit (Phase 0 Baseline)

## 1. Environment & Workspace Context
- **Project Directory**: `C:\Users\Dell\.gemini\antigravity\scratch\opspilot`
- **Host OS**: Windows 11 Enterprise (64-bit)
- **Java Runtime**: JDK 23.0.1 (`JAVA_HOME=C:\Program Files\Java\jdk-23`), targeting Java 21 compatibility.
- **Build Tool**: Apache Maven 3.9.6 (via Maven Wrapper `mvnw.cmd`).
- **Node.js**: v24.20.0 with `npm` 11.19.0.
- **Docker**: Docker CLI 29.9.2 present. Local PostgreSQL container configured in `docker-compose.yml`.
- **Active Workspace Status**: Initialized from scratch in the standard antigravity scratch directory.

## 2. Repository Reconnaissance
- Initial audit revealed no pre-existing legacy OpsPilot repository in the scratch directory.
- Cached Maven artifacts for Spring Boot 3.3.5, Spring Security 6.3, Spring Data JPA, Hibernate, PostgreSQL driver, Flyway, JJWT 0.12.6, SpringDoc OpenAPI 2.6.0, Micrometer Prometheus, and H2 are present in `.m2`.
- Cached Node packages for Vite 5, React 18.3, TypeScript 5.6, TanStack Query 5.59, React Hook Form 7.53, Zod 3.23, TailwindCSS, and Vitest are present in local npm cache.

## 3. Architecture Baseline
- **Structure**: Modular Monolith.
  - `backend/`: Spring Boot modular monolith (`com.opspilot.*`), Flyway database migrations, PostgreSQL (production/docker) & H2 in PostgreSQL compatibility mode for lightweight local tests, REST API under `/api/v1`.
  - `frontend/`: React 18 + TypeScript + Vite + TailwindCSS + TanStack Query + React Hook Form + Zod + SVG/React Flow canvas for workflow builder.
  - `docs/`: Comprehensive architecture, database, API, workflow, security, and verification documentation.
- **State**: Starting Phase 0 baseline definition and incremental implementation.
