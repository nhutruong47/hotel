# Nhu Villas — Hotel Booking Platform

A direct-booking experience for private villa stays. This repository contains
two tightly coupled deliverables:

- **`frontend/`** — React 19 + Vite SPA with a premium editorial aesthetic.
- **`hotel/`** — Spring Boot 3.4 (Java 21) REST API backed by JPA / Hibernate.

> The project uses externalised configuration so the **same artifact** can be
> deployed locally, in containers, or against a hosted database with zero
> recompilation.

---

## 1. Architecture Overview

```
┌───────────────────────┐       JSON over HTTP        ┌──────────────────────────┐
│  React 19 SPA (Vite)  │  ─────────────────────────▶ │  Spring Boot REST API    │
│  - TanStack Query     │  ◀─────────────────────────  │  - JPA / Hibernate       │
│  - React Router 7     │      Set-Cookie session       │  - Spring Security       │
│  - TypeScript strict  │                               │  - Maven build           │
└───────────────────────┘                                └─────────────┬────────────┘
                                                                      │
                                                                      ▼
                                                            ┌────────────────────────┐
                                                            │  SQL Server / Postgres │
                                                            │  JPA entities auto-DDL │
                                                            └────────────────────────┘
```

| Layer | Tech | Responsibility |
| --- | --- | --- |
| Presentation | React 19 + Tailwind 4 | Editorially rich UI, type-safe forms |
| State | TanStack Query + Zustand-less context | Server cache, single source of session truth |
| API | Spring MVC (`@RestController`) | HTTP contract + validation |
| Security | Spring Security + custom filter | Auth + role enforcement |
| Domain | `@Service` classes | Business rules, workflow |
| Persistence | Spring Data JPA | Repository pattern, `FetchType.LAZY` |
| Database | SQL Server / PostgreSQL | Indexed `@Entity` tables |

## 2. Repository Layout

### `docs/`
Contains all architectural, business, and enterprise documentation (e.g., `ARCHITECTURE.md`, `MASTER_PROJECT_PLAN.md`).
All backend endpoints must follow [`docs/API_STANDARDS.md`](docs/API_STANDARDS.md); a build-time architecture test enforces the controller, versioning, response, and admin-authorization rules.

### `database/`
Contains SQL migration scripts and constraints.

### `tools/`
Contains utility scripts used for refactoring and maintenance.

### `frontend/` (TypeScript / React)
```
src/
├── app/              ← Router definition
├── features/         ← Domain-driven modules
│   ├── admin/        ← AdminDashboardPage
│   ├── ai/           ← AI assistant widget
│   ├── auth/         ← Login / Register / Forgot / Verify
│   ├── booking/      ← Booking, checkout, success
│   ├── content/      ← About / Contact / FAQ / Offers / Dining…
│   ├── home/         ← Hero + ambient layers
│   ├── profile/      ← Profile / MyBookings / Wishlist / Reviews
│   └── villas/       ← List + Detail + API client
├── shared/
│   ├── api/          ← fetch wrapper, ApiError, API_PATHS
│   ├── auth/         ← SessionProvider, RequireAuth
│   ├── components/   ← Button, Card, Input, Navbar, Reveal, ErrorBoundary
│   ├── hooks/        ← useAnimations, useSmoothScroll
│   ├── layouts/      ← RootLayout
│   └── utils/        ← shared helpers
├── hooks/            ← animation hooks
├── assets/           ← Static images and icons
├── App.tsx           ← (none — wired directly in main.tsx)
└── main.tsx          ← RootProvider composition
```

### `hotel/` (Java 21 / Spring Boot)
```
src/main/java/com/hsf/hotel/
├── api/              ← REST controllers (one per resource)
├── config/           ← Security, exception handler, DataInitializer, CORS, SPA fallback
├── dto/              ← Request / response DTOs
├── exception/        ← ApiException + typed subclasses
├── model/            ← JPA entities
├── repository/       ← Spring Data repositories
├── service/          ← Business logic
├── observability/    ← RequestIdFilter
├── scheduler/        ← BookingScheduler (auto-cancel expired holds)
└── HotelApplication.java
```

## 3. Local Development

### Prerequisites
- Node.js 20+
- Java 21 (with `JAVA_HOME` set)
- Maven wrapper bundled (`./mvnw`)
- PostgreSQL 16 *or* SQL Server 2019

### One-shot startup
```bash
# Terminal 1 — frontend
cd frontend
npm install
npm run dev            # http://localhost:5173

# Terminal 2 — backend (PostgreSQL profile)
cd hotel
cp ../.env.example .env.local          # tweak if needed
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
# → http://localhost:8080
```

The first backend boot seeds a default admin account (`admin`/`admin@123`).
Override via `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` env vars in production.

### Containerised workflow
```bash
# From repo root
docker compose up --build
# Frontend → http://localhost
# Backend   → http://localhost:8080 (proxied through nginx)
```

## 4. Environment Variables

All sensitive values are externalised; see `.env.example` for the canonical
list.

| Variable | Purpose |
| --- | --- |
| `SPRING_DATASOURCE_URL` | JDBC URL (provider-aware) |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | DB credentials |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated SPA origins |
| `APP_BASE_URL` | Used for email links |
| `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` | Bootstrap admin |
| `APP_ADMIN_EMAIL` | Where admin notifications are sent |
| `APP_BOOKING_DEADLINE_HOURS` | Hours a guest has to pay after approval |
| `GEMINI_API_KEY` | AI recommendation engine |
| `SESSION_COOKIE_SECURE` | `true` in production (HTTPS) |

## 5. Architectural Principles

- **Clean Architecture** — Controllers → Services → Repositories. No entity
  leakage past the service boundary except for read-only views.
- **SOLID + DRY + KISS** — Each service has a single responsibility; common
  helpers (e.g. password validation, request id propagation) live alongside.
- **Repository Pattern** — Spring Data `JpaRepository` for every aggregate.
- **Service Layer** — Every mutation is wrapped in `@Transactional` and lives
  in a service that throws typed `ApiException`s.
- **DTO Pattern** — All public API input/output is a dedicated DTO/record; no
  entity is exposed directly (with the exception of the existing read models
  that the SPA already consumes verbatim).
- **Constructor Injection** — No `@Autowired` field injection; every
  dependency is final and non-null.
- **Observability First** — Structured logs ship a request id; the same id
  is echoed back to the browser in `X-Request-Id` for end-to-end tracing.
- **Security by Default** — Spring Security enforces admin-only routes;
  every mutation is owned by the current user; file uploads validate type +
  size; password reset is user-enumeration safe.

## 6. Testing Strategy

- **Unit Tests** live under `hotel/src/test/java`. Services are fully
  constructor-injected so a Mockito mock can be substituted without
  Spring.
- **Integration Tests** share the same package layout and boot a slim
  Spring context.
- **Frontend** uses `tsc --noEmit` and `vite build` as compile-time
  guardrails. Future iteration: add Vitest for component tests.

## 7. Operational Runbook

### Logs
- Plain JSON-ish logs land in `./logs/hotel-backend.log` with daily
  rotation (`logback-spring.xml`).
- Every line carries `requestId` so support can grep end-to-end.

### Health checks
- Liveness / readiness via `/actuator/health` (publicly reachable).
- Container probes configured to hit `/api/v1/health` for business
  semantics.

### Graceful shutdown
- `server.shutdown=graceful` keeps the JVM alive for in-flight requests
  (up to 30s) when a SIGTERM is received.

## 8. License & Attribution
Internal project — no public license configured.
