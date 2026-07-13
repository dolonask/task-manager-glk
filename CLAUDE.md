# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

This repo currently contains **design references** at the root and a **from-scratch Spring Boot backend skeleton** in `task-manager/`. There is no frontend code yet — the frontend is a separate, not-yet-created project.

- `task-manager/` — the actual codebase to build. A fresh, uninitialized Spring Boot (Maven) project: only `TaskManagerApplication.java` and an empty `application.yaml` exist so far. All controllers, entities, services, repositories, security config, etc. still need to be written here.
- `README.md` — handoff doc (in Russian) describing the product ("Диспетчерская задач" / GLK Task Dispatcher) and the design-to-code task. Read this first for product context.
- `task_manager.dc.html` — high-fidelity frontend prototype (all screens, test data, client-side interaction logic) for the SPA that will eventually consume this API. **Not production code** — it's a design reference built with a proprietary prototyping tool. Colors, typography, spacing, and interaction states here are final and should be reproduced faithfully in the real frontend, but the file itself is not meant to be copied or run as part of the product.
- `API спецификация.dc.html` — the authoritative REST API specification for the backend (auth, RBAC, data model, endpoints, error codes). This is the primary spec to implement against in `task-manager/`.
- `support.js`, `doc-page.js` — internal runtime of the prototyping tool, required only to view the `.dc.html` files in a browser. Not part of the product stack; never port these into the implementation.
- `glk-logo.png` — client logo asset (`#C8A068` gold), used at 26px in the sidebar and 34px on the login screen, always `object-fit: contain`.

The `.dc.html` files are plain HTML — read them directly (e.g. with the Read tool) to inspect the spec/prototype; no special tooling is needed.

## Product domain

Task management and execution-discipline system for ОАО «Государственная лизинговая компания» (GLK, Kyrgyz Republic). Three-tier role hierarchy:
- **Правление / `board`** (3 members) — creates tasks for structural departments (СП) and approves deadline-transfer requests for the departments they curate (`curatedDepartmentIds`).
- **Начальник СП / `head`** — decomposes tasks into subtasks, initiates deadline-transfer requests for their own department.
- **`admin`** — full access; the *only* role that can technically apply a new deadline to a task.
- **`employee`** (optional) — view + mark own subtask done.
- **`observer`** (optional) — read-only.

The defining business rule is the **three-stage deadline-transfer procedure**: justification (head) → approval (curating board member) → application (admin). A task's `currentDeadline` can never be edited directly (`PATCH /tasks/{id}` rejects it with 422) — it only changes via `PATCH /transfer-requests/{id}/apply`, and every step is written to an **append-only audit log** (no update/delete endpoints exist for audit entries, by any role, intentionally).

Full endpoint-by-endpoint detail (methods, request/response bodies, per-endpoint access matrix) is in `API спецификация.dc.html` §5–11; the cross-cutting role/action matrix is in §8. Key points to keep in mind when implementing:

- Auth: login against corporate AD domain `glk.kg` via LDAP bind (`POST /auth/login` with `username`/`password`, no domain suffix in the request); server returns JWT access token (15 min TTL) + server-side refresh token (12h TTL). JWT claims: `sub`, `role`, `departmentId`, `curatedDepartmentIds`, `iat`, `exp`.
- `Task.status` (`new`/`in_progress`/`done`/`overdue`) and progress are **always server-computed**, never set directly by clients.
- `TransferRequest.status` lifecycle: `draft` → `pending` → (`approved` → `applied`) | `rejected` | `revision` (back to initiator, resubmittable). Only one active request (`draft`/`pending`/`revision`) per task at a time — a second one returns `409 CONFLICT`.
- Visibility scoping: `admin`/`board`/`observer` see all tasks; `head`/`employee` see only their own department's tasks (`task.departmentId == user.departmentId`).
- Standard error envelope: `{ "error": { "code", "message", "field"? } }` with codes `VALIDATION_ERROR` (400), `UNAUTHENTICATED` (401), `FORBIDDEN` (403), `NOT_FOUND` (404), `CONFLICT` (409), `INVALID_STATE_TRANSITION` (422), `INTERNAL_ERROR` (500), `RATE_LIMITED` (429).
- List endpoints use `page`/`pageSize` (default 20, max 100) pagination and a `{ items, page, pageSize, total, totalPages }` envelope, plus `sort=-fieldName` style sorting.
- Base API path: `/api/v1`; all dates `YYYY-MM-DD`, timestamps `YYYY-MM-DDTHH:mm:ssZ`, IDs are UUID v4.

## Design tokens (for frontend work)

A deliberately small palette (5 tones): background `#FAF8F4`, card surfaces `#FFFFFF`, borders `#E8E2D6`, primary text `#2A241C`, secondary text `#6B6153`, muted text `#8A6A2E`/`#8A8074`, gold accent `#C8A068` (from the logo), status colors: in-progress `#F3E6C9`/`#8A6A2E`, overdue/error `#F4DAD3`/`#9A3B2C`, done `#2A241C` (dark pill/white text), new/neutral `#EDE8DE`/`#6B6153`. Font: Manrope, weights 400–800. Radii: 5–6px cards/inputs, 20px (full) pill badges. Sidebar 240px, topbar 64px. No icons, no animations — instant, static state transitions. Full per-screen layout spec is in `README.md`.

## Backend commands (`task-manager/`)

Java 17, Spring Boot 4.1.0 (parent), Maven wrapper. Run all commands from `task-manager/`.

```bash
./mvnw spring-boot:run       # run the app locally
./mvnw test                  # run all tests
./mvnw test -Dtest=ClassName#methodName   # run a single test
./mvnw package                # build the jar
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

Dependencies already in `pom.xml`: `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-webmvc`, `postgresql` (runtime), `spring-boot-devtools`, plus the corresponding `*-test` starters. No datasource is configured yet in `application.yaml` — it currently only sets `spring.application.name`.
