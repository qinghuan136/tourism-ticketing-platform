# AGENTS.md

## Project

This repository is a full-stack tourism ticketing platform:

- Backend: Java 17, Spring Boot, Maven, MyBatis, MySQL, Redis, Kafka, and Flyway.
- Frontend: two independent Vue 3 + TypeScript + Vite applications for tourists and operators.

Preserve business correctness, data consistency, API compatibility, and readability. Read the nearest `AGENTS.md` and relevant files under `docs/` before editing. If code and documentation conflict, report the conflict instead of guessing.

## Repository Map

```text
tourism-ticketing-platform/
├── tourism-common/       # Shared backend responses, exceptions, constants, utilities
├── tourism-pojo/         # Backend entities, DTOs, VOs, enums
├── tourism-server/       # Spring Boot application and business logic
├── frontend/
│   ├── tourist/          # Visitor-facing Vue application
│   ├── operator/         # Operator/admin Vue application
│   └── AGENTS.md         # Frontend-specific rules
├── docs/                 # Requirements, API contracts, database design, prototypes
└── tests/                # Cross-cutting and performance tests
```

Instructions closer to the edited file take precedence. All work under `frontend/` must also follow `frontend/AGENTS.md`.

## Working Rules

- Inspect related code, tests, manifests, and documentation before editing.
- Make only task-scoped changes; avoid unrelated refactors, formatting, or dependency additions.
- Do not create branches, commit, or push unless explicitly requested.
- Add or update tests for behavior changes and documentation for user-visible changes.
- Never claim a command or test passed unless it was actually run.
- Never commit credentials, tokens, private keys, production secrets, or privileged server configuration.

## Backend Boundaries

- Controllers handle HTTP concerns and validation; Services own workflows and transactions; Mappers handle database access.
- Business domains collaborate through Services, never through another domain's Mapper.
- Use DTOs for requests and VOs for responses; do not expose Entities through APIs.
- Use constructor injection, Jakarta Validation, centralized error handling, `BigDecimal` for money, and `java.time` types for dates and times.
- Use enums or centralized constants for statuses; avoid magic strings and numbers.
- Every schema change requires a new Flyway migration. Never edit or delete an applied versioned migration.

## Frontend and API Boundaries

- The backend implementation and API documentation are the source of truth for endpoints, payloads, enums, pagination, authentication, and errors.
- Do not invent frontend-only API fields or silently work around backend contract mismatches. Report the mismatch and update both sides only when the task authorizes it.
- Keep `frontend/tourist` and `frontend/operator` independently buildable; do not import source files between them.
- Follow `frontend/AGENTS.md` for Vue architecture, UI conventions, API wrappers, state management, and browser validation.

## Validation Commands

Use the narrowest relevant checks. On Windows, run Maven through `mvnw.cmd` from the repository root.

```powershell
# Backend module and dependencies
.\mvnw.cmd -pl tourism-server -am test

# Full backend suite
.\mvnw.cmd clean test

# One frontend application
cd frontend\tourist   # or frontend\operator
npm run type-check
npm run build
```

For frontend integration work, also verify the rendered flow, browser Console, and Network requests. Check each app's `package.json` before assuming any other script exists.

## Task Handoff

Report changed files, key design decisions, checks executed, and remaining risks or unverified behavior.

## Optional Skills

Use only skills relevant to the task and available in the current environment:

- Spring Boot: `tourism-server/.agents/skills/java-springboot/SKILL.md`
- JUnit: `tourism-server/.agents/skills/java-junit/SKILL.md`
- Maven build/testing: `.agents/skills/building-and-testing/SKILL.md`
- Frontend implementation: `build-web-apps:frontend-app-builder`
- Frontend browser testing: `build-web-apps:frontend-testing-debugging`
