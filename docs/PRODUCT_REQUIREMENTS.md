# GPS Guardian Escolar — Functional requirements

This document is the versioned source of truth for the backend-related user stories.
It replaces private or unreviewable references with requirements that can be traced to
an issue, a pull request, an endpoint, a test, or a reproducible deployment command.

## Product problem

Guardians need to register students, link a student device, monitor school trips, receive
safe-zone notifications, and manage account security. The backend must protect minors'
data, expose stable contracts to the web/mobile frontend, and remain reproducible across
development, QA, and production.

## Users and actors

| Actor | Need | Main capabilities |
| --- | --- | --- |
| Guardian | Manage children and monitor trips | Registration, login, students, routes, zones, notifications |
| Student device | Share the active trip location | Device linking, child token, tracking and unlink enforcement |
| Administrator | Operate and audit the platform | Dashboard, audit events, errors and operational controls |
| CI/reviewer | Verify every change before promotion | Tests, quality checks, PR approval and branch protection |

## System scope and contracts

- Authentication: registration, email verification, login, device confirmation, 2FA,
  password recovery, sessions and logout.
- Student linking: QR/manual code, device ownership, one active device and child profile.
- Tracking: trip lifecycle, coordinates, WebSocket updates, route deviation and history.
- Safety: safe zones, assigned routes and notifications.
- Administration: dashboard, audit logs and operational error reporting.
- Public surface: health check only; student profile data requires the intended linked-device
  authorization path.

The executable API contract is defined by the controllers and DTOs under
`src/main/java/com/guardianescolar/api/modules`. Any endpoint change must update its tests,
the frontend endpoint catalog, and the corresponding pull request.

## Active delivery stories

### H-71 — Reconcile the production line

- **User:** team/reviewer.
- **Problem:** delivery branches diverge and do not provide one verifiable promotion line.
- **Acceptance:** `main` contains the approved `dev` work; each repository documents its
  official branches; protected branches require an approved PR and green CI.
- **Evidence:** branch comparison, merged PR URL, and protected-branch settings.

### H-72 — Manual student-device linking

- **User:** student or guardian linking a device without scanning the QR.
- **Problem:** the manual flow can omit `studentId`, while the backend previously required it.
- **Acceptance:** the backend resolves an active student from a valid unexpired code; QR
  linking continues to work; invalid, expired, used, and over-attempted codes are rejected.
- **Evidence:** `StudentLinkCodeServiceTest`, `StudentTransactionTest`, and frontend contract test.

### H-73 — Release artifact and secret policy

- **User:** release manager.
- **Problem:** APKs and API keys must not enter source history.
- **Acceptance:** CI rejects tracked APKs and Google Maps keys; APKs are distributed through
  GitHub Releases and keys come from deployment variables.
- **Evidence:** frontend `security:release` check and GitHub Release asset.

### H-74 — Least-privilege database runtime

- **User:** operator/security reviewer.
- **Problem:** the backend must not run as the PostgreSQL bootstrap superuser.
- **Acceptance:** the backend uses `gps_guardian_app`; Liquibase alone uses bootstrap credentials;
  `current_user` is not `postgres`.
- **Evidence:** Docker Compose configuration and the database session check.

### H-75 — Hash transient security tokens

- **User:** guardian completing verification, recovery, 2FA, or device confirmation.
- **Problem:** raw short-lived bearer tokens must not be recoverable from database tables.
- **Acceptance:** verification, recovery, 2FA and device-confirmation values are hashed at rest,
  expire, and remain single-use where applicable.
- **Evidence:** `TransientTokenHashServiceTest` and the authentication/session test suite.

### H-76 — Version functional requirements

- **User:** new team member, reviewer, or evaluator.
- **Problem:** requirements and acceptance criteria must not depend on private Drive documents.
- **Acceptance:** this document is committed, linked from the README, and every active story
  identifies its user, scope, acceptance criteria, and evidence.
- **Evidence:** this file, the README link, issue references, and PR checks.

### H-79 — Security tests and coverage in CI

- **User:** reviewer.
- **Problem:** critical authorization and module tests must run on every PR with visible coverage.
- **Acceptance:** CI executes backend security tests, frontend Jest tests, and publishes coverage
  artifacts; a failing security test blocks merge.
- **Evidence:** workflow run URL and uploaded coverage artifacts.

### H-80 — Notification deletion and configuration contract

- **User:** guardian managing notifications.
- **Problem:** local deletion must not be undone by a reload and configuration must agree across
  frontend, backend, database, and infrastructure.
- **Acceptance:** deletion persists server-side; SMS is either end-to-end supported or explicitly
  removed; email validation accepts valid public domains.
- **Evidence:** API contract test, reload regression test, and environment matrix.

### H-81 — Versioned OpenAPI and pagination

- **User:** frontend client and API integrator.
- **Problem:** the contract is not versioned and large list endpoints can return unbounded history.
- **Acceptance:** `openapi.json` is versioned and accessible; list endpoints accept bounded
  `page`/`size` values and return pagination metadata; frontend aliases are removed.
- **Evidence:** OpenAPI artifact, controller tests, and a paginated API response.

## Definition of done

A story is complete only when its implementation is merged through a reviewed PR, its automated
checks pass, its acceptance criteria have reproducible evidence, and the issue is moved to `Done`.
Direct pushes to protected delivery branches do not count as completion.

## Verification record

For each story, record the issue URL, PR URL, merge commit, target branch, test command, and
deployment or API evidence in the issue or PR. Do not use private links as the only evidence.
