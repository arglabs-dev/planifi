# Repository Guidelines

This repository captures the evolving product documentation for Planifi; treat every contribution as part of a living specification that other builders will rely on.

## Project Structure & Module Organization
- `docs/1_requerimientos.md` consolidates the Spanish product requirements; update it when scope changes or clarification arrives from stakeholders.
- `docs/1_requirements.md` mirrors the requirements in English; keep both files aligned and call out language-specific nuances inline.
- `docs/2_arquitectura.md` holds architectural decisions; when adding backend, MCP, or conversational flow details, reference related requirement sections for traceability.

## Build, Test, and Development Commands
- `npx markdownlint \"docs/**/*.md\"` checks formatting and heading order; run before pushing to avoid lint noise in reviews.
- `glow docs/<file>.md` (or your preferred Markdown previewer) gives a quick terminal render to verify links, lists, and tables.
- `rg <keyword> docs/` helps confirm that terminology (e.g., “tags” vs “categorías”) stays consistent across documents.

## Coding Style & Naming Conventions
- Write in Markdown with ATX (`#`) headings, 80–100 character lines, and sentence-case headings unless a term is formally capitalized.
- Prefer concise paragraphs plus ordered lists for sequential flows (e.g., user journeys) and unordered lists for enumerations.
- When introducing entities (tags, MCP endpoints, data fields), bold the first mention and reuse the exact casing later to simplify searchability.

## Testing Guidelines
- Treat linting as the primary “test”; a clean `markdownlint` run is the minimum bar before review.
- When adding pseudo-APIs or data schemas, include fenced code blocks with language hints (` ```json `, ` ```http `) so renderers and linters can validate syntax highlighting.
- Cross-link related sections via relative links (e.g., `[Ver requisitos](./docs/1_requirements.md)`) and click-test them in your preview.

## Commit & Pull Request Guidelines
- Follow short, imperative commit subjects (git history currently uses simple statements like “Initial commit”); e.g., `Add MCP section to architecture`.
- Commits should group related documentation changes; avoid mixing requirement updates with architecture edits unless tightly coupled.
- Pull requests need: summary of the change, list of touched files, mention of pending follow-ups, and screenshots or pasted preview output if formatting is non-trivial.
- Reference issue IDs or conversation links in PR descriptions so reviewers can trace the rationale quickly.
