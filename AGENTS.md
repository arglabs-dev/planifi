# Planifi Agent Playbook

Planifi documentation is a live specification; every edit must keep the MCP +
Spring Boot stack coherent, auditable, and production-ready.

## Table of contents
1. [Mission & scope](#1-mission--scope)
2. [Sources of truth](#2-sources-of-truth)
3. [Toolchain & coordination](#3-toolchain--coordination)
4. [Architecture alignment](#4-architecture-alignment)
5. [Agent autonomy](#5-agent-autonomy)
6. [Development best practices](#6-development-best-practices)
7. [DevOps & operational guardrails](#7-devops--operational-guardrails)
8. [CI/CD workflow](#8-cicd-workflow)
9. [SDLC lifecycle](#9-sdlc-lifecycle)
10. [Build, test, and dev commands](#10-build-test-and-dev-commands)
11. [Coding style & naming](#11-coding-style--naming)
12. [Testing guidelines](#12-testing-guidelines)
13. [Commit & PR guidelines](#13-commit--pr-guidelines)
14. [Quality gates & checklists](#14-quality-gates--checklists)

## 1. Mission & scope
- Deliver end-to-end updates (docs, schemas, ADRs) without waiting for manual
  approval unless a task explicitly restricts access.
- Keep requirements, architecture, and operational notes synchronized so other
  contributors can implement features safely.
- Treat security, observability, and compliance guardrails as first-class
  deliverables alongside content changes.

## 2. Sources of truth
- `docs/1_requerimientos.md` and `docs/1_requirements.md` host the Spanish and
  English requirements; always update both when scope changes.
- `docs/2_arquitectura.md` plus `docs/ARCHITECTURE.md` record technical
  decisions; cite the exact section (MCP Server, Backend Spring Boot,
  Seguridad) when altering flows or components.
- ADRs capture exceptions or new standards; reference them whenever you record
  deviations or hard decisions.

## 3. Toolchain & coordination
- Develop inside Codex Web; document shell snippets exactly as they are run in
  the Codex CLI.
- Manage work in Linear; every branch, commit, and PR references the matching
  `PLA-XX` issue and includes relevant notes or acceptance criteria.
- GitHub is the canonical repo: trunk-based flow using `feature/PLA-XX`
  branches, small PRs, and mandatory reviews.
- Sync decisions through Linear comments and GitHub PR threads; mirror any
  architecture/security outcome back into `docs/ARCHITECTURE.md`.

## 4. Architecture alignment
- **MCP Server**: Node.js 20 + TypeScript + OpenAI MCP SDK, Zod validation,
  OpenTelemetry instrumentation, `X-MCP-API-Key`, `Idempotency-Key`, and
  `correlation-id` headers, stateless distroless images.
- **Backend**: Java 21 + Spring Boot 3.x (controller/service/repository),
  PostgreSQL 16, optional MongoDB 7, S3-compatible storage with antivirus/KMS,
  Kafka/RabbitMQ outbox, Snowflake/BigQuery analytics.
- **Security/Observability/CI**: JWT + API keys, TLS/mTLS, Prometheus +
  Micrometer, Grafana/Loki, GitHub Actions with Trivy, Snyk, Dependabot,
  Cosign.
- Never introduce new tech or flows without first updating the architecture
  docs and referencing the relevant section.

## 5. Agent autonomy
- Assume full permission to create branches, push commits, open PRs, and run
  automation; pause only when a task flags restricted resources.
- Run all lint/tests/builds locally before pushing; escalate commands only when
  external credentials or network paths are absolutely required.
- Keep changelogs, ADRs, and documentation synchronized with the code and
  process changes you submit.
- When uncertain, default to action: propose the change, explain assumptions in
  the PR, and link to the relevant Linear issue and architecture sections.

## 6. Development best practices
- Contract-first: update OpenAPI specs for Spring Boot endpoints and Zod
  schemas for the MCP actions before prose or code; cross-link the change to
  `docs/ARCHITECTURE.md` and ADRs if security or contracts shift.
- Deliver incremental, high-signal edits: `npx markdownlint`, MCP tests (`npm
  test`, `tsc --noEmit`), and Spring suites (`./mvnw test` or Gradle) must pass
  locally ahead of any push.
- Preserve telemetry hooks: document OpenTelemetry spans (MCP + backend),
  Micrometer metrics, and correlation-id propagation across Kong/Nginx →
  MCP → Spring → PostgreSQL/S3.
- Describe failure modes, retries, and idempotency behavior next to each flow
  so both MCP handlers and Spring controllers enforce the same responses and
  outbox/idempotency storage.

## 7. DevOps & operational guardrails
- Treat infrastructure changes as code: reference Terraform/Kubernetes modules
  for the MCP service, Spring deployment, PostgreSQL, MongoDB, S3, and
  Kafka/RabbitMQ whenever diagrams or docs evolve.
- Enforce the baseline: JWT scopes, API keys, TLS/mTLS, distroless Node and
  Java images, non-root execution, Dependabot/Snyk alerting, vault-managed
  secrets; document mitigations for any exception.
- Embed observability expectations: specify Prometheus metrics (Micrometer),
  OpenTelemetry exporters (Node + Java), Grafana/Loki dashboards, and
  Snowflake/BigQuery ingestion jobs for every change.
- Keep runbooks adjacent to features, referencing concrete services (Spring
  expense controller, MCP `createExpense`, PostgreSQL partitions), feature
  flags, rollback commands, and on-call contacts.

## 8. CI/CD workflow
- GitHub Actions reference order: markdownlint → Node.js 20 MCP lint/tests →
  Java 21 Spring Boot unit/integration tests → OpenAPI/MCP contract checks →
  container builds (MCP + backend) → Trivy/Snyk/Dependabot scans → Cosign
  signing → staged deploys; record any skipped stage with rationale.
- Document workflow YAML details: secrets (registry, Linear/GitHub tokens),
  build matrices (Node 20.x, Temurin 21), cache paths (`~/.npm`, Maven/Gradle),
  and manual approvals for production promotion.
- Include pipeline evidence in every PR (link or summary), clarify which
  environments are affected, and flag backports or feature flags when MCP
  actions, Spring endpoints, or infra modules change.
- Keep artifacts reproducible: cite container tags, SBOM outputs, git tags
  (`v0.x.y`), and the Terraform/Kubernetes release notes in your updates.

## 9. SDLC lifecycle
- **Discover & define**: capture the user problem, affected MCP/back-end
  touchpoints, and acceptance criteria in Linear (`PLA-XX`) with links to the
  relevant requirement sections.
- **Design & align**: update architecture docs/ADRs, validate OpenAPI + Zod
  schemas, and confirm storage/security implications before implementation.
- **Build & verify**: work in `feature/PLA-XX`, run Node + Java tests, ensure
  telemetry/idempotency hooks are implemented exactly as documented, and keep
  docs in lockstep.
- **Release & operate**: merge via reviewed PRs, confirm the CI/CD pipeline,
  tag MCP/back-end releases, update runbooks/alerts, and monitor
  Prometheus/Grafana/Loki plus Snowflake KPIs; roll back via Kubernetes or
  feature flags if SLOs regress.
- **Reflect & improve**: log retrospectives in Linear, update requirements and
  architecture docs with lessons learned, and close any observability,
  security, or CI/CD gaps discovered in production.

## 10. Build, test, and dev commands
- `npx markdownlint "docs/**/*.md"` validates formatting and heading order.
- `glow docs/<file>.md` previews Markdown (or use your preferred renderer).
- `rg <keyword> docs/` verifies terminology alignment (e.g., “tags” vs
  “categorías”).
- MCP: `npm install`, `npm test`, `tsc --noEmit`.
- Backend: `./mvnw test` (or Gradle), `./mvnw spring-boot:run` only when you
  need to verify flows explicitly documented.

## 11. Coding style & naming
- Use Markdown with ATX (`#`) headings, 80–100 character lines, and
  sentence-case titles unless a proper noun requires capitalization.
- Prefer concise paragraphs and bullet lists; ordered lists for sequential
  flows, unordered for enumerations.
- Bold the first mention of entities (tags, data fields, MCP endpoints) and
  reuse the exact casing afterward to simplify search.

## 12. Testing guidelines
- Treat linting as the minimum bar; include command output in PRs when it
  informs reviewers.
- Provide fenced code blocks with language hints (` ```json `, ` ```http `) for
  pseudo-APIs, schemas, or payloads so renderers and linters apply the right
  syntax highlighting.
- Cross-link related sections using relative links (e.g., `[Ver
  requisitos](./docs/1_requirements.md)`) and click-test them during previews.

## 13. Commit & PR guidelines
- Use short, imperative commit subjects (e.g., `Add MCP section to
  architecture`), referencing the `PLA-XX` issue when relevant.
- Group related documentation changes together; avoid mixing requirement edits
  with architecture updates unless they are inseparable.
- PRs must include: summary, touched files, pipeline evidence, pending
  follow-ups/ADRs, and screenshots or preview output when formatting is
  non-trivial.
- Reference issue IDs or conversations in PR descriptions so reviewers can
  trace the intent quickly.

## 14. Quality gates & checklists
- ✅ `npx markdownlint "docs/**/*.md"` clean
- ✅ MCP: `npm test`, `tsc --noEmit`
- ✅ Spring Boot: `./mvnw test` (or Gradle equivalent)
- ✅ OpenAPI + Zod schemas updated and cross-linked
- ✅ Telemetry hooks (OpenTelemetry spans, Micrometer metrics) documented
- ✅ Security posture confirmed (JWT scopes, API keys, TLS/mTLS, distroless)
- ✅ PR references Linear issue, includes pipeline evidence, and calls out
  ADR/runbook updates when needed
