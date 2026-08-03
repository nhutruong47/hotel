# ARCHITECTURE DECISION - CANONICAL RUNTIME

Date: 2026-08-04
Status: Active

## Decision

The canonical runtime for Nhu Villas is:

- Frontend: `frontend/` using Next.js/React.
- Backend API: `hotel/` using Spring Boot REST API.
- API base path: `/api/v1`.

The legacy root `src/` Spring Boot/Thymeleaf application remains in the repository only as a temporary compatibility/reference layer. It must not receive new user-facing UI features unless a dedicated migration task explicitly approves it.

## Rules

1. New public UI work must go into `frontend/`.
2. New REST API work must go into `hotel/`.
3. Do not mix Thymeleaf with the React user flow.
4. Do not delete root `src/` until the React flow has fully replaced the legacy flow and has passed regression testing.
5. Deployment commands must target `frontend/` and `hotel/`, not root `src/`.
6. Any legacy change must be limited to security, compatibility, or migration safety.

## Build Commands

Frontend:

```bash
cd frontend
npm run lint
npm run build
```

Backend API:

```bash
cd hotel
./mvnw test
```

Legacy verification only:

```bash
./mvnw test
```

## Reason

This keeps the project aligned with the enterprise architecture track while preserving the legacy application until the React/REST replacement is proven stable.
