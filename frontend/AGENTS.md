# AGENTS.md

## Overview
Frontend for an existing Java/Spring Boot application.

- `tourist/`: visitor-facing Vue app.
- `operator/`: operator/admin Vue app.
- They share backend APIs but must remain independently buildable.
- Do not import source files across the two apps.

## Stack
Vue 3, TypeScript, Vite, Vue Router, Pinia, Axios, Element Plus, npm.

Use Composition API with `<script setup lang="ts">`.

Do not add another frontend framework, router, state library, HTTP client, or UI library unless explicitly requested.

## Structure
Each app should generally follow:

```text
src/
├── api/          # backend API wrappers by domain
├── assets/
├── components/   # reusable UI
├── layouts/      # persistent route shells
├── router/
├── stores/
├── types/
├── utils/        # e.g. Axios instance
├── views/        # route-level pages
├── App.vue
└── main.ts
```

Conventions:
- `views/`: pages mapped to routes.
- `layouts/`: shared shells such as header/sidebar/footer + `<RouterView />`.
- `components/`: reusable page parts.
- `stores/`: genuinely shared state only; keep page-local UI state local.
- `api/`: backend calls; avoid raw Axios calls scattered through views/components.
- Only extract shared code between `tourist` and `operator` after real duplication appears.

## App Boundaries
**Tourist:** optimize for visitor-facing flows and product usability. Typical areas: browsing, details, booking/purchase, orders, auth, personal center. Do not make it look like an admin console merely because Element Plus is available.

**Operator:** optimize for operational efficiency. Tables, filters, forms, dialogs, pagination, status management, dashboards, and Element Plus are expected.

## Backend Contract
The existing backend is the source of truth.
path: docs/openapi.json

Before implementing API-dependent behavior, inspect the relevant backend code or API docs and confirm:
- endpoint and HTTP method;
- request DTO;
- response DTO/VO;
- enum/status values;
- pagination shape;
- auth/permission requirements;
- error response format.

Never invent backend endpoints, fields, enum values, or permissions.
Do not modify backend code unless the task explicitly requires it.

## Architecture
Prefer:

```text
View / Component / Store
        ↓
      api/*.ts
        ↓
 utils/request.ts
        ↓
       Axios
        ↓
 Spring Boot
```

Centralize Axios base URL, token attachment, and common 401/403 handling.
Use Router guards for navigation control; backend authorization remains the real security boundary.
Never place passwords, DB credentials, private keys, signing secrets, or privileged server secrets in frontend code.

## Coding Rules
- Prefer simple, readable TypeScript over clever abstractions.
- Avoid `any` when a reasonable domain type exists.
- Match frontend types to real backend contracts.
- Use stable IDs for `v-for` keys when available.
- Follow existing project patterns before creating new abstractions.
- Do not refactor unrelated code during a feature task.
- Do not add dependencies when the existing stack is sufficient.

## Feature Workflow
Develop vertically by feature:

1. Define user-visible behavior.
2. Confirm the real backend contract.
3. Implement the smallest working UI/state flow.
4. Integrate with the real backend immediately.
5. Test happy path, important failures, loading/empty states, Console, and Network.
6. Clean up and validate before moving on.

Prefer small, independently verifiable changes over large generated rewrites.

## Validation
Before finishing:
- inspect the app's `package.json`;
- run the narrowest relevant existing checks;
- ensure meaningful changes build successfully when a build script exists;
- inspect browser Console/Network for integration work.

Typical commands may include `npm run dev`, `npm run type-check`, `npm run lint`, `npm run test`, and `npm run build`.

Do not assume a command exists without checking `package.json`. Do not claim a check passed unless it was actually run.

## Agent Rules
Before editing, read the nearest applicable `AGENTS.md`, inspect related frontend code, and inspect backend contracts when relevant.

Keep changes scoped. Reuse existing patterns. Do not generate or rewrite large unrelated parts of the application.

If repository evidence resolves an ambiguity, proceed. If a product/API decision cannot be inferred safely, surface it instead of guessing.
