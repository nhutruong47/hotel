# ARCHITECTURE.md

This document explains *why* the project is structured the way it is. It is
intentionally implementation-focused: every claim is supported by a code
pointer you can jump to.

## 1. Layered Topology

```
┌─────────────────────────────────────────────────────────────────────┐
│ Presentation layer ── React SPA (frontend/src/features)             │
├─────────────────────────────────────────────────────────────────────┤
│ Transport layer ── Spring MVC + Spring Security (hotel/src/api)     │
├─────────────────────────────────────────────────────────────────────┤
│ Domain layer ── Services (hotel/src/service)                        │
├─────────────────────────────────────────────────────────────────────┤
│ Persistence layer ── JPA repositories (hotel/src/repository)        │
├─────────────────────────────────────────────────────────────────────┤
│ Storage layer ── PostgreSQL | SQL Server                            │
└─────────────────────────────────────────────────────────────────────┘
```

Cross-cutting:
- **Observability** lives in `hotel/src/observability/RequestIdFilter.java`
  and the `logback-spring.xml` configuration.
- **Error mapping** is centralised in
  `hotel/src/config/GlobalExceptionHandler.java`. The controller body throws
  `ApiException` (or one of its subclasses) and the advice produces the
  uniform envelope.

## 2. Dependency Rules

- **Controllers depend on services, never on repositories.** This lets us
  swap persistence without touching the transport.
- **Services depend on repositories through interfaces** (Spring Data).
- **Repositories depend only on the entity and JPA.**

These rules are enforced socially today; the package layout makes a
violation obvious during review.

## 3. Error Model

```
ApiException (base)
├── ResourceNotFoundException    → 404
├── BusinessRuleException        → 422
└── ForbiddenException           → 403
```

Other runtime exceptions are intercepted and responded to as:

| Cause | Status |
| --- | --- |
| `IllegalArgumentException` / Jackson parse errors / missing query params | 400 |
| `BadCredentialsException` | 401 (INVALID_CREDENTIALS) |
| `AccessDeniedException` | 403 |
| `DataIntegrityViolationException` | 409 |
| Anything else | 500 with stable `INTERNAL_ERROR` |

Every response carries the same envelope (defined in `config/ApiResponse.java`):

```json
{
  "data": null,
  "error": { "code": "ROOM_UNAVAILABLE", "message": "..." },
  "meta": { "timestamp": "2026-07-12T05:15:00Z" }
}
```

The frontend `api/client.ts` reads `data` from the envelope or rejects with
an `ApiError` that already includes `status`, `code`, `message`, and
`requestId`.

## 4. Authentication & Session Lifecycle

1. The SPA calls `/api/v1/auth/login` with username + password.
2. Spring authenticates via Spring Security (and the
   `SessionAuthBridgeFilter` integrates the existing
   `HttpSession.setAttribute("user", …)` model).
3. The session cookie is `HttpOnly`, `SameSite=Lax`. `Secure` is gated on
   the `SESSION_COOKIE_SECURE` environment variable.
4. `RequireAuth` on the SPA consults `SessionProvider`. Any 401 outside
   `/auth/*` triggers a redirect to `/login?redirect=...` while clearing
   `sessionStorage`.
5. `POST /api/v1/auth/logout` invalidates the server-side session via
   `session.invalidate()`.

## 5. Observability

- **Request id** — every request gets a UUID by default; the value is
  pushed into the SLF4J MDC under `requestId`, mirrored in the
  `X-Request-Id` response header, and stored on the `ApiError`.
- **Structured logs** — every line includes `[requestId]` between level
  and logger name. See `hotel/src/main/resources/logback-spring.xml`.
- **Health endpoint** — `/actuator/health` returns the aggregated health
  status. `/api/v1/health` is the lightweight business health.
- **Long-request timeout** — `spring.mvc.async.request-timeout=60s` lets the
  AI recommendation stream complete without timing out.

## 6. Database

- **Schema generation** — `spring.jpa.hibernate.ddl-auto=update` plus
  `INDEX` annotations on entities. Production deployments should switch to
  Flyway/Liquibase migrations; not enabled yet (tracked in section 12).
- **Connection pooling** — defaults to HikariCP (Spring Boot default).
- **Incompatibilities** — `ChatMessage.userMessage` / `aiResponse` use the
  portable `TEXT` column type so the schema works on both SQL Server and
  PostgreSQL.

### Indexes
| Table | Index | Rationale |
| --- | --- | --- |
| `Users` | `idx_users_username` (unique) | Login lookup |
| `Users` | `idx_users_email` | Password reset / duplicate check |
| `Users` | `idx_users_reset_token` | Reset lookup |
| `Bookings` | `idx_bookings_room_dates` | Availability check |
| `Bookings` | `idx_bookings_user` | "My bookings" |
| `Bookings` | `idx_bookings_status` | Admin workflows |
| `Bookings` | `idx_bookings_created` | "Recent bookings" |
| `Rooms` | `idx_rooms_available` | Search filter |
| `Rooms` | `idx_rooms_room_type` | Search filter |
| `Rooms` | `idx_rooms_room_number` (unique) | Lookup by number |
| `Reviews` | `idx_reviews_room`, `idx_reviews_user`, `idx_reviews_rating` | Aggregations |
| `Vouchers` | `idx_vouchers_code`, `idx_vouchers_expiry` | Validate |
| `Wishlists` | `uq_wishlists_user_room`, `idx_wishlists_user` | Toggle + uniqueness |
| `ChatMessages` | `idx_chat_user`, `idx_chat_created` | History |

## 7. Frontend Architecture

- **Composition root** — `frontend/src/main.tsx` wires
  `ErrorBoundary → QueryClientProvider → SessionProvider → RouterProvider`.
- **State** — TanStack Query owns server cache; a tiny `SessionProvider`
  publishes the current user. No global Redux; no Zustand; one context only.
- **Code splitting** — every non-auth route is loaded via `React.lazy`
  with a `Suspense` fallback.
- **Routing** — `react-router-dom@7` with `createBrowserRouter`. Auth
  guards live in `RequireAuth`; non-auth routes are public.
- **Styling** — Tailwind v4 via `@tailwindcss/vite`. Brand tokens live in
  `index.css` `@theme`. **No Tailwind config file**; tokens are exposed as
  CSS variables.
- **Accessibility** — semantic landmarks, focus-visible outlines, ARIA
  labels on icon-only buttons, body-overflow guard during menu open,
  scroll-to-top anchor links.
- **Performance** — `loading="lazy"` and `decoding="async"` on all
  media tags; `prefers-reduced-motion` hooks ambient animations;
  `IntersectionObserver` pauses off-screen effects.

## 8. Build & Toolchain

- **Backend** — Maven wrapper (`./mvnw`) produces `target/hotel-backend.jar`
  via `spring-boot:repackage`. The Dockerfile builds in two stages so the
  runtime image is JRE-only.
- **Frontend** — Vite 8 with `@tailwindcss/vite`. Build emits hashed chunks
  with cache-friendly `immutable` headers.
- **Container** — `docker-compose.yml` brings up Postgres + backend +
  nginx-fronted SPA.

## 9. Known Trade-offs

- We keep `@Transactional` on services and not controllers because the
  unit of work typically spans service + repository.
- The frontend SPA keeps `/uploads` proxied to the backend even though
  it's static, because we want a single origin for cookie auth.
- `Bookings` are queried with `FetchType.EAGER` for `user`/`room`
  because admin views always need them. Queries that don't can override
  with `JOIN FETCH` if needed.

## 10. Open Items (Future Improvements)

1. Migrate to Flyway / Liquibase for reproducible schema migrations.
2. Add Bucket4j rate limiting on `/auth/login`, `/auth/register`,
   `/api/v1/contact`.
3. Convert AI chat to Server-Sent Events for streaming UX.
4. Add a WebSocket channel for real-time booking status updates.
5. Introduce Refresh Token / JWT hybrid so admin workloads survive
   across multiple SPA tabs.
6. Adopt `@Async` for email + AI recommendation to remove latency from
   critical paths.
