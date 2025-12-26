# Planifi — AGENTS.md (Codex + Humans)

> **Purpose:** persistent, repo-native instructions so Codex can ship production-ready changes safely and fast.

## 0) TL;DR (do this every task)
1) Read *this* file + any nearer `AGENTS.md` in subfolders (if present).
2) Confirm scope via Linear issue `PLA-XX` and align with `ARCHITECTURE.md`.
3) Work in a short-lived branch: `feature/PLA-XX-short-title`.
4) Keep changes small, testable, and reviewable; prefer 1 PR per issue.
5) Run the relevant checks locally/CI-equivalent before pushing.
6) In PR description: summary + tests run + risk/rollout + links (Linear + docs sections).

---

## 1) How Codex reads instructions (important)
Codex loads `AGENTS.md` before doing work, and can layer instructions from global → repo → nested folders. Use this file for repo-wide standards; add nested `AGENTS.md` in `mcp/` or `backend/` if you need tighter rules per subproject.  
- Global and per-project discovery/precedence is supported, including `AGENTS.override.md` (highest priority in a given directory scope).  
- If you add nested files, keep them **short** and **specific** (commands, conventions, local gotchas).

---

## 2) Sources of truth (do not drift)
**Primary:** `ARCHITECTURE.md` (end-to-end architecture + standards).  
**Secondary:** requirements docs (`docs/1_requerimientos.md` and `docs/1_requirements.md`) and ADRs.

Rules:
- If you change behavior/contract/security/ops, update docs in the same PR.
- Never introduce new tech or major flows without updating architecture/ADRs first.

---

## 3) Project scope & architecture anchors (v0.1+)
### 3.1 Components
- **MCP Server**: Node.js 20 + TypeScript + OpenAI MCP SDK; Zod validation; HTTP client `undici`; distroless container; OpenTelemetry; stateless.  
- **Backend**: Java 21 + Spring Boot 3.x (Spring MVC, Spring Data JPA, Spring Security, Resilience4j, Micrometer).  
- **DB**: PostgreSQL 16 (ACID) with optional tenant partitioning; optional MongoDB 7 for enriched attachments/audit.
- **Storage**: S3-compatible (S3/MinIO) with private buckets, KMS, expiration, antivirus.
- **Edge/Gateway**: Kong or Nginx Ingress with rate limiting and API-key auth; TLS and optional mTLS.
- **Observability**: OpenTelemetry + Prometheus + Grafana + Loki.
- **CI/CD**: GitHub Actions with security scanning (Trivy, Snyk/Dependabot), signed images.

### 3.2 Non-negotiables (security/ops)
- **Idempotency-Key** required on POST/PUT/DELETE; backend persists replayable responses in `idempotency_keys`.
- **Correlation**: propagate `correlation-id` end-to-end; include `traceId` in error responses.
- **Secrets**: never in repo; use a managed vault; injected via pipeline/env.

---

## 4) Workflow (Linear → GitHub) and how to behave as an agent
### 4.1 Branching & PRs
- Trunk-based: short branches `feature/PLA-XX-*` and small PRs.
- 1 issue → 1 PR unless the issue explicitly bundles multiple features.
- If a follow-up is needed, update the **existing PR** (don’t create PR sprawl).

### 4.2 Binary files policy (Codex Cloud reality)
- **Do not** introduce generated/built artifacts (e.g., `target/`, `.jar`, `.class`, `node_modules/`) into git.
- Prefer storing assets/attachments in S3 (per architecture).
- If Codex is creating PRs via Codex Cloud: avoid adding/modifying binary files in PRs. If absolutely required, describe the steps and leave them for a local/CI commit.

### 4.3 Definition of Done (DoD)
A task is “done” only if:
- Behavior matches requirements + architecture.
- Contracts updated (OpenAPI + MCP/Zod) when behavior changes.
- Tests added/updated and passing.
- Logs/metrics/traces remain correct; correlation/idempotency preserved.
- No secrets added; scanners remain clean; build is reproducible.
- Docs/ADRs updated when applicable.

---

## 5) Contract-first rules (API + MCP)
### 5.1 OpenAPI (backend)
- OpenAPI is the **source of truth** for REST `/api/v1`.
- Any endpoint change must update: OpenAPI → DTO validation → controller/service/repository.
- Keep error format consistent: use ProblemDetails and return `{"errorCode","message","traceId"}` equivalents.

### 5.2 MCP (Node)
- Every MCP action has:
  - Zod input schema (strict; no unknowns unless explicitly allowed).
  - deterministic output schema (stable fields; versioned changes).
  - timeouts + retries (bounded) and idempotency/correlation headers to backend.
- Always send:
  - `X-MCP-API-Key` (MCP→backend auth)
  - `Idempotency-Key` (mutations)
  - `correlation-id` (end-to-end tracing)

---

## 6) Implementation guidelines — MCP Server (Node.js 20 / TypeScript)
### 6.1 Structure expectations
- Keep handlers thin; move logic into services.
- No implicit globals; config via env; validate config at startup (Zod).
- Prefer `undici` for HTTP calls; enforce timeouts and retry policy (exponential backoff, cap).

### 6.2 Observability
- OpenTelemetry spans around:
  - MCP action entry/exit
  - outbound backend HTTP call
  - storage operations if any (pre-signed flows)
- Logs are JSON, include: `action`, `correlation-id`, `traceId` (if available), `status`, `latency_ms`.

### 6.3 Security
- Never log API keys, JWTs, raw PII, or raw attachments.
- Treat any user-provided string as untrusted; avoid prompt-injection by sanitizing tool inputs where applicable.

---

## 7) Implementation guidelines — Backend (Java 21 / Spring Boot 3.x)
### 7.1 Layering & domain
- Controllers: HTTP + DTO validation only.
- Services: domain logic, idempotency, transactions, orchestration.
- Repositories: persistence only.

### 7.2 Idempotency (required)
- Mutations require `Idempotency-Key`.
- Persist:
  - request hash (to detect key reuse with different payload)
  - status
  - response body (for replay)
- Ensure idempotency is enforced at service boundary (not only controller).

### 7.3 Errors & responses
- Use centralized exception mapping.
- Return consistent error payload with `traceId`.
- Map upstream failures (storage/db/outbox) to stable error codes (avoid leaking internals).

### 7.4 Resilience
- Use Resilience4j for:
  - timeouts
  - circuit breakers
  - retries (only for safe operations / idempotent semantics)
- Bound retries (no infinite retry loops).

---

## 8) Testing & quality gates
### 8.1 Minimum tests
- Unit tests for service logic (including idempotency behavior).
- Integration tests for repositories + REST slices.
- Contract tests: OpenAPI + MCP schemas stay in sync.
- E2E: MCP conversational flows (createExpense/listExpenses/createTag/auth/api-key).

### 8.2 Commands (choose what exists in repo)
Docs lint:
- `npx markdownlint "docs/**/*.md"`

MCP:
- If lockfile present: `npm ci`
- Otherwise: `npm install`
- `npm test`
- `npx tsc --noEmit`

Backend:
- `./mvnw test`
- `./mvnw spring-boot:run` (only when explicitly needed to validate flows)

Quality gates:
- Coverage target: >= 80% where applicable.
- No critical vulnerabilities; container scans pass.
- Builds reproducible; images signed in CI when configured.

---

## 9) CI/CD & security automation guidance
- Prefer GitHub Actions pipeline: lint → tests → contract checks → container builds → scans → signing → deploy staging → manual prod promotion.
- If you run Codex in GitHub Actions (`openai/codex-action@v1`):
  - Restrict who can trigger workflows.
  - Sanitize any prompt inputs coming from PR bodies/issues (prompt-injection risk).
  - Use the narrowest sandbox that still works (start with `workspace-write`).

---

## 10) Git hygiene, docs style, and repository cleanliness
### 10.1 Commits
- Prefer **small, atomic commits** with imperative subjects.
- Include the Linear id in the subject or body: `PLA-XX`.
- Avoid `WIP`/`temp` commits in shared branches; squash if needed.

### 10.2 .gitignore (keep binaries out of PRs)
- Ensure build outputs are ignored (examples): `target/`, `*.class`, `*.jar`, `node_modules/`, `dist/`.
- If a task accidentally generates artifacts, remove them from git (`git rm --cached …`) and extend `.gitignore`.

### 10.3 Documentation style
- Keep Markdown clean and lintable (`npx markdownlint "docs/**/*.md"`).
- Use fenced blocks with language tags (`json`, `http`, `bash`) for payloads and commands.
- When you change behavior or contracts, update docs/ADRs in the same PR and link sections explicitly.

## 11) PR template (put this in the PR description)
- **Linear:** PLA-XX
- **Summary:** what changed and why
- **Contracts:** OpenAPI updated? MCP/Zod updated?
- **Tests:** commands run + results
- **Risk:** edge cases + failure modes + rollback/feature flag
- **Observability:** logs/metrics/traces impacted? new dashboards/runbooks?
- **Security:** auth/scopes/headers reviewed? secrets handling confirmed?
- **Docs/ADRs:** links to updated sections

---
