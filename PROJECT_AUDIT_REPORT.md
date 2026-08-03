# PROJECT AUDIT REPORT - NHU VILLAS

Audit date: 2026-08-04
Scope: read-only project audit, except this report file.
Workspace checked: `D:\d\1\hotelNew - Copy`
Important path note: `D:\d\1\hotelNew` is currently a file, not the working repository directory.

## 1. Executive Summary

Nhu Villas is in a demo-capable state, but not yet production-ready or handover-ready.

The project has a modern target architecture in progress: a separated Next.js/React frontend under `frontend/` and a Spring Boot REST API backend under `hotel/`. This matches the enterprise/luxury direction. However, the repository still contains a legacy Spring Boot/Thymeleaf backend under root `src/`, and both source trees use the same package name. This creates operational confusion, duplicate logic, and risk during build/deploy.

Last verified results in this working session show backend tests, legacy tests, frontend build, frontend lint, TypeScript check, and npm audit passing. However, lint still emits warnings, the repository is dirty, generated `.next` artifacts were previously tracked and are now staged as deleted, and production smoke testing has not been proven.

Critical unresolved concerns:

| Severity | Issue | Impact |
| --- | --- | --- |
| Critical | Real repo path is `hotelNew - Copy`; `hotelNew` is a file | High risk of running commands in the wrong target during handover/deploy |
| High | Dual backend source trees: `hotel/` REST API and root `src/` Thymeleaf legacy | Duplicated business rules and ambiguous ownership |
| High | `hotel/src/main/java/com/hsf/hotel/exception/GlobalExceptionHandler.java` appears to miss `@RestControllerAdvice` | API errors may not follow contract at runtime |
| High | Sensitive config keys exist in local env files and application properties | Must be rotated/validated before production; values are not exposed in this report |
| High | Worktree is dirty with many modified/untracked/staged-deleted files | Cannot safely release or hand over until cleaned/committed intentionally |
| Medium | Frontend image/a11y/lint warnings remain | SEO, accessibility, and performance targets are below luxury checklist threshold |

Current project score: 70/100
Demo readiness: YES, with controlled scenario and known environment
Production readiness: NO
Handover readiness: NO

## 2. Tong Quan Du An

Nhu Villas is a luxury villa/hotel booking platform. The product includes public villa discovery, villa detail, content pages, auth, user account, booking, payment, admin operations, reports, reviews, wishlist, notifications, promotions, FAQ/contact, and AI chat/recommendation.

Main directories:

| Path | Purpose | Status |
| --- | --- | --- |
| `frontend/` | Next.js/React frontend | Active frontend |
| `hotel/` | Spring Boot REST API backend | Active target backend |
| `src/` | Legacy Spring Boot/Thymeleaf backend | Still present, should not be deleted until React flow is stable |
| `database/` | SQL/dev DB helper scripts | Contains cleanup helper script |
| `docs/` | UI/UX blueprint and quality checklist | Contains luxury UI governance docs |
| `uploads/` | Uploaded/static runtime files | Needs production storage policy |

Project governance documents found:

| File | Purpose |
| --- | --- |
| `docs/LUXURY_EXPERIENCE_BLUEPRINT.md` | UI/UX luxury blueprint |
| `docs/UI_QUALITY_CHECKLIST.md` | UI sprint scorecard/checklist |
| `PRODUCTION_BLOCKERS.md` | Existing production blocker summary |
| `PRODUCTION_CHECKLIST.md` | Existing production checklist |
| `*_AUDIT.md` files | Feature-level audit notes |

`AI_PROJECT_FRAMEWORK` was not found at the repository root during this audit.

## 3. Cong Nghe Su Dung

Frontend:

| Technology | Evidence |
| --- | --- |
| Next.js | `frontend/package.json`: `next 15.5.22` |
| React | `react ^19.0.0`, `react-dom ^19.0.0` |
| TypeScript | `typescript ^5.0.0` |
| Tailwind CSS v4/PostCSS | `tailwindcss ^4.0.0`, `@tailwindcss/postcss ^4.0.0` |
| React Query | `@tanstack/react-query ^5.101.1` |
| GSAP/Lenis | `gsap ^3.15.0`, `lenis ^1.3.25` |
| Stripe frontend | `@stripe/stripe-js ^4.0.0` |
| Maps | `leaflet`, `react-leaflet` |

Backend target under `hotel/`:

| Technology | Evidence |
| --- | --- |
| Java | `java.version 21` |
| Spring Boot | `3.4.1` |
| REST API | `hotel/src/main/java/com/hsf/hotel/api` |
| Spring Security | `spring-boot-starter-security` |
| JPA/Hibernate | `spring-boot-starter-data-jpa` |
| Validation | `spring-boot-starter-validation` |
| Flyway | `flyway-core`, migration folders |
| Database drivers | MSSQL runtime, PostgreSQL optional runtime, H2 test |
| Cache | Spring cache + Caffeine |
| Payment | Stripe Java |
| Reports | Apache POI, iText |
| Mail | Spring Mail |
| WebFlux | Used for external AI/Gemini calls |

Legacy root backend:

| Technology | Evidence |
| --- | --- |
| Spring Boot 3.4.1 | Root `pom.xml` |
| Thymeleaf | `spring-boot-starter-thymeleaf` |
| Spring Security/JPA | Root `pom.xml` |
| SQL Server local config | Root `src/main/resources/application.properties` |

## 4. Kien Truc He Thong

Target architecture:

```text
Browser
  |
  v
Next.js frontend (`frontend/`)
  |
  v
REST API `/api/v1/*`
  |
  v
Spring Boot backend (`hotel/`)
  |
  v
Service layer -> Repository layer -> Entities -> Database
```

Current actual architecture:

```text
Repository
  |-- frontend/  Next.js UI
  |-- hotel/     Spring Boot REST API target backend
  |-- src/       Legacy Spring Boot + Thymeleaf backend
```

Strengths:

- Frontend and target backend are now physically separated.
- API client centralizes paths in `frontend/src/shared/api/client.ts`.
- Backend has clear packages for API, DTO, service, repository, model, config, exception, security.
- Security headers, CORS, CSRF bridge, session bridge, and rate limiting concepts exist in target backend.
- Flyway migrations exist for main backend.

Architecture risks:

- Two backend applications share package namespace and similar domain logic.
- Root and `hotel/` modules both have `pom.xml`; build/deploy command must be explicit.
- Legacy Thymeleaf should remain until React flow is stable, but it needs a clear freeze policy.
- Current working tree contains many modified/untracked/staged-deleted files; deployment source of truth is unclear.

## 5. Danh Sach Chuc Nang

| Feature | Frontend | Backend/API | Status | Notes |
| --- | --- | --- | --- | --- |
| Public homepage/luxury hero | Yes | Rooms/blog/promotions APIs | In progress | Hero has been upgraded but still needs final score against B3 checklist |
| Villa listing | Yes | `/api/v1/rooms` | Mostly ready | Uses public API |
| Villa detail | Yes | `/api/v1/rooms/{id}`, reviews, booked dates | Mostly ready | Large component needs future cleanup |
| Auth login/register/logout/session | Yes | `/api/v1/auth/*` | Mostly ready | Session-based auth; verify reset flows exist |
| Email verification/reset | Yes | Auth + EmailService | In progress | Needs real SMTP smoke |
| User profile/settings | Yes | `/api/v1/profile`, `/api/v1/users/me` | Mostly ready | Authorization must stay covered |
| Booking create/list/detail/cancel | Yes | `/api/v1/bookings/*` | In progress | Write flow must not be changed without dedicated task |
| Booking payment | Yes | `/api/v1/payments/*`, Stripe | In progress | Needs real Stripe webhook validation in staging |
| Booking success/checkout | Yes | Booking/payment APIs | In progress | UI exists; external flow not fully proven |
| Admin dashboard | Yes | `/api/v1/admin/*` | In progress | Large API/controller surface; needs role smoke |
| Reports export | Admin UI | `/api/v1/admin/reports/*` | Mostly ready | Unit tests exist; production export smoke needed |
| Reviews | Yes | `/api/v1/reviews/*` | Mostly ready | Eligibility logic has tests |
| Wishlist | Yes | `/api/v1/wishlist/*` | Mostly ready | Needs E2E validation |
| Notifications | Yes | `/api/v1/notifications/*` | Mostly ready | Needs UX/a11y pass |
| Contact | Yes | `/api/v1/contact` | Mostly ready | Public POST |
| FAQ | Yes | `/api/v1/faqs/*` | Mostly ready | Good SEO opportunity |
| Promotions/vouchers | Yes | `/api/v1/promotions/*`, `/api/v1/vouchers/*` | Mostly ready | Validation DTOs exist |
| Blog/content pages | Yes | `/api/v1/blogs/*` | In progress | SEO schema/content depth not complete |
| AI recommendation/chat | Yes | `/api/v1/ai/*` | In progress | External API key/rate limits are production risk |

## 6. Phan Tich Frontend

Frontend routes found:

```text
/
/about
/account
/account/bookings
/account/bookings/[id]
/account/notifications
/account/profile
/account/reviews
/account/settings
/account/support
/account/wishlist
/admin
/blogs
/blogs/[id]
/booking/[villaId]
/booking/success
/checkout
/contact
/dining
/experiences
/faq
/forgot-password
/gallery
/login
/my-bookings
/offers
/profile
/register
/reviews
/verify-email
/villas
/villas/[villaId]
/wishlist
```

Large frontend files:

| File | Lines | Risk |
| --- | ---: | --- |
| `frontend/src/features/content/ContentPages.tsx` | 1304 | Too many page sections in one file |
| `frontend/src/shared/i18n/translations.ts` | 1040 | Large translation object |
| `frontend/src/features/admin/AdminDashboardPage.tsx` | 968 | Admin complexity concentrated |
| `frontend/src/features/auth/AuthPages.tsx` | 818 | Auth UI and flow complexity |
| `frontend/src/features/admin/AdminExtendedPanels.tsx` | 681 | Large untracked admin extension |
| `frontend/src/features/villas/VillaDetailPage.tsx` | 591 | Detail page should be split later |
| `frontend/src/features/account/BookingsPage.tsx` | 564 | Account booking flow complexity |
| `frontend/src/features/home/HomePage.tsx` | 506 | Homepage/luxury sections concentrated |

Strengths:

- Frontend is separated from Thymeleaf.
- Routes cover public, account, booking, admin, and content experiences.
- API paths are centralized in `frontend/src/shared/api/client.ts`.
- Fonts use `next/font` with Vietnamese subsets.
- Skip link exists in root layout.
- Public assets exist, including hero and villa imagery.
- `robots.txt` and `sitemap.xml` exist.

Frontend concerns:

- Many `<img>` usages remain; Next.js lint warns about `@next/next/no-img-element`.
- At least one shared image component needs stronger alt handling: `frontend/src/shared/components/Card.tsx`.
- Metadata is minimal: root title/description only says "Nhu Villas" and "A place to breathe."
- Hotel schema, breadcrumb schema, FAQ schema, Open Graph completeness, and canonical strategy are not fully visible.
- Several pages are feature-rich but large; future refactor should split by section/component after UI flow stabilizes.
- Current visual score should be measured with `docs/UI_QUALITY_CHECKLIST.md` before approving B3.

## 7. Phan Tich Backend

Target backend under `hotel/` has a stronger enterprise direction:

- API package: `hotel/src/main/java/com/hsf/hotel/api`
- DTO package: `hotel/src/main/java/com/hsf/hotel/api/dto`
- Service package: `hotel/src/main/java/com/hsf/hotel/service`
- Repository package: inferred through JPA repositories
- Model package: `hotel/src/main/java/com/hsf/hotel/model`
- Exception package: `hotel/src/main/java/com/hsf/hotel/exception`
- Security/config packages: `hotel/src/main/java/com/hsf/hotel/config`, `hotel/src/main/java/com/hsf/hotel/security`

Large backend files:

| File | Lines | Risk |
| --- | ---: | --- |
| `hotel/src/main/java/com/hsf/hotel/service/BookingService.java` | 980 | Too much booking/payment/status responsibility |
| `hotel/src/main/java/com/hsf/hotel/api/AdminApi.java` | 707 | Admin API surface too concentrated |
| `hotel/src/main/java/com/hsf/hotel/api/PaymentApi.java` | 636 | Payment/webhook/API logic concentrated |
| `hotel/src/main/java/com/hsf/hotel/service/EmailService.java` | 578 | Many templates/flows in one service |
| `src/main/java/com/hsf/hotel/controller/AdminController.java` | 456 | Legacy admin complexity remains |

Strengths:

- Java 21 and Spring Boot 3.4.1 are aligned across root and target modules.
- Validation DTOs exist for admin rooms, amenities, vouchers, room types.
- Booking, payment, review, voucher, reports and Stripe adapter tests exist.
- SecurityConfig includes CORS allowlist, security headers, session fixation mitigation, and role rules.
- CSRF is handled by a custom cookie/header filter.

Backend concerns:

- `GlobalExceptionHandler.java` imports `RestControllerAdvice` but the class read during audit does not show `@RestControllerAdvice`. If unchanged, global error handling is not registered.
- Several Java files contain mojibake/encoding corruption in comments/messages.
- Legacy root `src/` contains many `System.out.println` and `printStackTrace` usages.
- `BookingService`, `AdminApi`, and `PaymentApi` are large enough to slow future safe changes.
- Real deployment must choose exactly one backend entry point.

## 8. Phan Tich API

Target API base: `/api/v1`

API groups found:

| Group | Representative endpoints |
| --- | --- |
| Health | `GET /api/v1/health` |
| Auth | `POST /api/v1/auth/login`, `POST /register`, `GET /verify`, `POST /logout`, `GET /session`, `POST /forgot-password`, `POST /reset-password` |
| Rooms | `GET /api/v1/rooms`, `GET /api/v1/rooms/{id}`, `GET /api/v1/rooms/{roomId}/booked-dates` |
| Bookings | `GET/POST /api/v1/bookings`, `GET/PATCH /{id}`, `POST /{id}/cancel`, `POST /{id}/payment`, `GET /pricing-preview`, `GET /vouchers/validate` |
| Payments | `POST /api/v1/payments`, `POST /stripe-checkout`, `GET /stripe-session/{sessionId}`, `POST /intent`, `POST /webhook/stripe`, `POST /{id}/complete`, `POST /{id}/fail`, `POST /{id}/refund` |
| Reviews | `GET /reviews/room/{roomId}`, `GET /mine`, `GET/POST /booking/{bookingId}`, `PUT/DELETE /{reviewId}` |
| Profile | `GET/PUT /api/v1/profile`, `PUT /password`, `GET/PUT /preferences` |
| Wishlist | `GET /api/v1/wishlist`, `POST /toggle` |
| Notifications | `GET /api/v1/notifications`, `POST /{id}/read`, `POST /read-all`, `DELETE /{id}` |
| Contact | `POST /api/v1/contact` |
| Blogs | `GET /api/v1/blogs`, `GET /{id}` |
| FAQ | `GET /api/v1/faqs`, category/search/grouped/helpful endpoints |
| Promotions | all/featured/category/expired/upcoming/countdown/grouped/validate/preview |
| AI | `GET /history`, `POST /recommend`, `DELETE /history` |
| Admin | dashboard, bookings, rooms, vouchers, room-types, amenities, reports, checkin/checkout, promotions, reviews, contacts, audit logs, revenue, occupancy, refunds |
| Users | admin list/detail/role/disabled/delete, self update `/users/me` |

API strengths:

- Clear versioning with `/api/v1`.
- Frontend `API_PATHS` closely mirrors backend endpoints.
- Authenticated API defaults to protected except explicit public endpoints.
- Admin endpoints are guarded by `hasRole("ADMIN")`.
- API response wrapper and request-id handling exist.

API issues:

- Global exception handler registration must be fixed/verified.
- Public/private endpoint contract should be exported as an API contract document before handover.
- Some DTO validation exists, but not every request body has visible enterprise-grade validation.
- Payment webhook endpoints must be smoke-tested with signed webhook payloads.

## 9. Phan Tich Database

Model entities found in target backend:

```text
Amenity
AuditLog
Blog
Booking
BookingStatus
BookingStatusTransition
ChatMessage
ContactMessage
FaqItem
Notification
NotificationType
Payment
Promotion
Review
Room
RoomTypeEntity
User
UserRole
Voucher
WishlistItem
```

Migration files:

```text
hotel/src/main/resources/db/migration/V1__init.sql
hotel/src/main/resources/db/migration/V2__add_performance_indexes.sql
hotel/src/main/resources/db/migration/V3__add_query_path_indexes.sql
hotel/src/main/resources/db/postgresql/V1__init.sql
hotel/src/main/resources/db/postgresql/V4__add_postgresql_search_indexes.sql
hotel/src/main/resources/db/postgresql/V5__performance_indexes.sql
hotel/src/main/resources/db/postgresql-legacy/V1__fix_identity.sql
```

Strengths:

- Flyway exists in target backend.
- H2 test profile exists.
- Index migrations exist for query/performance paths.
- Domain model covers booking, payments, reviews, notifications, promotions, content, and audit logs.

Database concerns:

- SQL Server, PostgreSQL, and H2 support are present simultaneously; production DB target must be declared clearly.
- Legacy root backend likely still relies on Hibernate/update style behavior.
- Seed/default admin behavior must be controlled per profile.
- No destructive DB commands were executed during this audit.
- Existing cleanup script `database/cleanup_development_data.sql` should remain manual/review-only unless explicitly approved.

## 10. Authentication / Authorization

Current target backend auth model:

- Session-based authentication with `JSESSIONID`.
- CSRF token cookie/header model using `XSRF-TOKEN` and `X-XSRF-TOKEN`.
- CORS credentials enabled for configured allowed origins.
- BCrypt password encoder.
- Role checks using `hasRole("ADMIN")` and authenticated endpoints.
- Session fixation mitigation via `ChangeSessionIdAuthenticationStrategy`.
- Security headers include HSTS, CSP, frame deny, referrer policy, and permissions policy.

Strengths:

- Admin API is protected at security configuration level.
- User self endpoint `/api/v1/users/me` is separated from admin user management.
- Public endpoints are explicit.
- Security headers are more mature than default Spring setup.

Risks:

- CSRF protection is custom because Spring CSRF is disabled; this requires dedicated security tests.
- Webhook signature validation and CSRF exemption must be validated together.
- `.env`, `.env.production`, and application properties contain sensitive key names and/or environment placeholders. Values are intentionally not shown.
- Root legacy security config disables CSRF and still exists.
- Role and ownership checks must be maintained in service layer, especially booking/review/profile/user flows.

## 11. Chat Luong Code

Positive findings:

- Clear target separation: `api`, `api/dto`, `service`, `model`, `config`, `exception`.
- Tests exist for core service flows.
- Frontend API client has typed helper functions and centralized paths.
- UI governance docs now exist for luxury experience and review scoring.

Quality issues:

| Issue | Evidence | Impact |
| --- | --- | --- |
| Large classes/components | BookingService 980 lines, AdminApi 707 lines, ContentPages 1304 lines | Harder review, testing, and safe sprint changes |
| Legacy duplicate backend | Root `src/` and `hotel/` both active in repo | Duplicated fixes and deploy ambiguity |
| Console printing in legacy | Many `System.out.println`, `printStackTrace` in root `src/` | Poor production logging and possible sensitive output |
| Encoding/mojibake | Several Java messages/comments show corrupted Vietnamese text | User-facing message quality and maintainability risk |
| Generated artifacts tracked historically | `.next` files staged deleted | Git hygiene issue |
| Missing global advice annotation risk | `GlobalExceptionHandler` read without `@RestControllerAdvice` | Broken API error contract |

## 12. Package / Dependency

Frontend dependency status:

- Next.js is pinned to `15.5.22`.
- React 19 is in use.
- npm overrides pin `postcss 8.5.25` and `sharp 0.35.3`.
- Last verified `npm audit --omit=dev`: PASS, 0 vulnerabilities.

Backend dependency status:

- Spring Boot 3.4.1.
- Java 21.
- Stripe Java configured.
- POI/iText present for reports.

Dependency risks:

- `iText 7.1.17` is older and should be reviewed before production.
- `next lint` is deprecated in newer Next.js direction; lint command should move to ESLint CLI before future upgrades.
- Multiple DB drivers/profiles increase operational complexity.
- Dependency health should be rechecked before production freeze.

## 13. File Cau Hinh / Secrets

Sensitive key locations found. Values are not included in this report.

| File | Notes |
| --- | --- |
| `.env` | Local secrets/config present; should remain ignored |
| `.env.production` | Production-style secrets/config present locally; validate it is ignored and never committed |
| `.env.example`, `env.example` | Templates contain key names/default placeholders |
| `docker-compose.yml` | Contains dev/default secret keys; must be overridden in production |
| `hotel/src/main/resources/application*.properties` | Contains datasource/admin/mail/Gemini/Stripe/webhook config keys |
| `src/main/resources/application.properties` | Legacy datasource/mail config exists |
| `hotel/src/test/resources/application-test.properties` | Test datasource/admin config exists |

Required action before deploy:

- Rotate any real secrets that may have been used in local files.
- Verify `.env` and `.env.production` are ignored and never committed.
- Replace weak/default docker-compose secrets in production environment.
- Use secret manager or deployment environment variables for production.

## 14. Ket Qua Build

Build was not rerun in this audit to avoid creating or changing generated artifacts after the user requested read/analyze-only. The following are last verified results from this same workspace session:

| Command | Last verified result | Notes |
| --- | --- | --- |
| `hotel/.mvnw.cmd test` | PASS | 119 tests, 0 failures/errors |
| root `./mvnw.cmd test` | PASS | 16 tests, 0 failures/errors |
| `frontend/npm run build` | PASS | Next.js 15.5.22 build passed |
| `frontend/npm run lint` | PASS with warnings | Warnings remain |
| `frontend/npx tsc --noEmit` | PASS | TypeScript check passed |
| `frontend/npm audit --omit=dev` | PASS | 0 vulnerabilities |

Build risks:

- Current worktree is dirty, so these results must be rerun on the final release commit.
- `.next` artifacts were previously tracked; build output can pollute Git if ignore rules are not enforced.

## 15. Ket Qua Test

Test files found:

```text
hotel/src/test/java/com/hsf/hotel/api/BookingFlowIntegrationTest.java
hotel/src/test/java/com/hsf/hotel/HotelApplicationTests.java
hotel/src/test/java/com/hsf/hotel/service/BookingServiceTest.java
hotel/src/test/java/com/hsf/hotel/service/payment/StripePaymentAdapterTest.java
hotel/src/test/java/com/hsf/hotel/service/PaymentServiceTest.java
hotel/src/test/java/com/hsf/hotel/service/ReportServiceTest.java
hotel/src/test/java/com/hsf/hotel/service/ReviewServiceTest.java
hotel/src/test/java/com/hsf/hotel/service/VoucherServiceTest.java
src/test/java/com/hsf/hotel/HotelApplicationTests.java
src/test/java/com/hsf/hotel/service/ReportServiceTest.java
src/test/java/com/hsf/hotel/service/ReviewServiceTest.java
```

Coverage strengths:

- Booking flow integration test exists.
- Booking, payment, Stripe adapter, reports, reviews, voucher tests exist in target backend.
- Legacy root tests still compile/run.

Test gaps:

- Need API contract tests for all public/admin endpoints.
- Need security tests for CSRF, admin role, ownership checks, and webhook signature.
- Need frontend E2E tests for hero/nav/villa/booking/account/admin critical flows.
- Need accessibility tests with keyboard and screen reader-friendly assertions.
- Need production-like DB migration test against the selected production database.

## 16. Loi Ton Tai

Unresolved issues:

| Priority | Issue | Evidence | Recommended action |
| --- | --- | --- | --- |
| P0 | Canonical repo path confusion | `D:\d\1\hotelNew` is a file, real repo is `D:\d\1\hotelNew - Copy` | Rename/standardize workspace before handover |
| P0 | Dirty worktree | Many modified/untracked/staged-deleted files | Commit intentionally or split cleanup commits |
| P1 | Global exception handler may not be active | Missing `@RestControllerAdvice` in target handler file as read | Add/verify annotation in a dedicated backend fix sprint |
| P1 | Dual backend trees | `hotel/` and root `src/` both active | Freeze legacy, declare target backend, migrate gradually |
| P1 | Secrets/config hygiene not production-final | Sensitive keys across env/properties/docker-compose | Secret manager/env hardening and rotation |
| P1 | Frontend lint warnings remain | `<img>` and a11y warnings found | Replace with `next/image` or document exceptions; fix alt |
| P2 | Large files/classes | Large file table above | Refactor after feature freeze |
| P2 | SEO metadata shallow | Root metadata minimal; schema not complete | SEO sprint after UI stabilization |
| P2 | Encoding/mojibake | Java messages/comments corrupted | Normalize UTF-8 in controlled cleanup |
| P2 | Production smoke not proven | Last build/test local only | Run staging deploy checklist |

## 17. Rui Ro Du An

| Risk | Severity | Likelihood | Impact | Mitigation |
| --- | --- | --- | --- | --- |
| Wrong folder used during release | Critical | Medium | Bad deploy/handover | Standardize path and document commands |
| Global API errors not handled consistently | High | Medium | Broken frontend error UX | Fix `@RestControllerAdvice` and test |
| Legacy and target backend diverge | High | High | Duplicate bugs | Freeze legacy and define migration map |
| Secrets leak or weak production defaults | High | Medium | Security incident | Rotate secrets and use env/secret manager |
| Payment webhook mismatch | High | Medium | Payment state incorrect | Signed webhook staging test |
| Booking write regressions | High | Medium | Revenue/user trust impact | Keep booking write locked to dedicated tasks |
| UI quality below luxury target | Medium | Medium | Brand mismatch | Enforce checklist for each UI sprint |
| SEO underdeveloped | Medium | High | Low organic visibility | Schema/content/metadata sprint |
| Performance drift | Medium | Medium | Poor LCP/INP | Image optimization and Lighthouse budget |
| Accessibility gaps | Medium | Medium | WCAG risk | Keyboard/focus/reduced-motion pass |

## 18. Backlog Cong Viec

Immediate stabilization backlog:

1. Decide canonical repository path and remove path ambiguity.
2. Review and intentionally commit or split current dirty worktree changes.
3. Fix/verify `GlobalExceptionHandler` registration in target backend.
4. Rerun backend/frontend build and tests on clean working tree.
5. Create an API contract document for `/api/v1`.
6. Add security tests for CSRF, admin role, ownership, and webhook signature.
7. Validate production secrets and rotate any real local credentials.
8. Move generated artifacts out of Git permanently.

Frontend/UI backlog:

1. Finish B3 self-review against `docs/UI_QUALITY_CHECKLIST.md`.
2. Replace or justify `<img>` usage that affects LCP/SEO/lint.
3. Improve metadata, Open Graph, canonical, schema, and FAQ SEO.
4. Add reduced-motion and interaction checks for GSAP/Lenis.
5. Split large UI pages only after visual flow is stable.

Backend backlog:

1. Freeze legacy Thymeleaf write paths.
2. Define target DB profile for production.
3. Add endpoint-level tests for admin and public APIs.
4. Refactor BookingService/PaymentApi/AdminApi by bounded responsibility.
5. Normalize UTF-8 encoding in backend messages/comments.

## 19. Diem Danh Gia Du An

| Category | Score | Target | Notes |
| --- | ---: | ---: | --- |
| Functionality | 19/25 | 23/25 | Broad feature surface exists; staging proof needed |
| Architecture | 7/10 | 9/10 | Target architecture good, dual backend risk remains |
| Code quality | 10/15 | 13/15 | Large files and legacy code reduce score |
| Database | 7/10 | 9/10 | Migrations exist; production DB target must be clarified |
| Security | 6/10 | 9/10 | Good direction; custom CSRF/webhook/secrets need hardening |
| Testing | 8/10 | 9/10 | Unit/integration tests pass; E2E/security tests missing |
| UI/UX | 3/5 | 5/5 | Luxury direction exists; warnings/checklist remain |
| Performance | 3/5 | 5/5 | Build passes; Lighthouse not yet proven |
| Deployment | 3/5 | 5/5 | Docker/nginx files exist; staging deploy not proven |
| Documentation | 4/5 | 5/5 | Many audits/checklists exist; API contract missing |
| Total | 70/100 | 90+/100 | Demo-capable, not production-ready |

Quality target from UI governance:

| Area | Required for UI sprints |
| --- | ---: |
| Architecture | >=95 |
| UI | >=95 |
| UX | >=95 |
| Luxury Experience | >=95 |
| Motion | >=95 |
| SEO | >=98 |
| Performance | >=95 |
| Accessibility | >=95 |
| Responsive | >=95 |
| Conversion | >=95 |

Current audit conclusion: project-level score does not yet satisfy production or luxury sprint approval thresholds.

## 20. Ke Hoach Tiep Theo

Recommended next order:

1. Stabilization Sprint S0 - Repository and release hygiene
   - Confirm canonical repo path.
   - Clean/commit current worktree intentionally.
   - Ensure generated artifacts are ignored.

2. Backend Safety Sprint A-fix
   - Fix/verify `GlobalExceptionHandler`.
   - Add tests for error response contract.
   - Add CSRF/admin/webhook tests.

3. Sprint B3 Review Completion
   - Score current Hero using `docs/UI_QUALITY_CHECKLIST.md`.
   - Fix only Hero issues if any category is below 95.
   - Do not move to Navbar without approval.

4. API Contract Sprint
   - Generate/maintain endpoint contract for `/api/v1`.
   - Document public/protected/admin endpoints.
   - Align frontend `API_PATHS` with backend routes.

5. Staging Deploy Sprint
   - Select DB target.
   - Configure secrets via environment/secret manager.
   - Run build/test/smoke on clean commit.

This order is recommended because the project first needs a stable source of truth, then runtime/API safety, then UI sprint approval, then deployment confidence.

## 21. Ket Luan Demo / Deploy / Handover Readiness

Demo readiness:

- Status: YES, controlled demo only.
- Conditions: use the known working directory, known local environment, and last verified build/test commands.

Deploy readiness:

- Status: NO.
- Blockers: dirty worktree, ambiguous repo path, production secrets not finalized, target DB/deploy profile not proven, webhook/security smoke tests missing.

Handover readiness:

- Status: NO.
- Blockers: dual backend source trees, many untracked audit/task files, missing API contract, and no clean release commit.

Release gate required:

1. Clean Git state.
2. Confirm canonical module: `frontend/` + `hotel/`.
3. Run backend tests, frontend build, lint, TypeScript, audit on the exact release commit.
4. Run staging smoke for auth, public browsing, booking read/write, payment webhook, admin role, reports.
5. Validate secrets and production database.

## Management Summary

Project status: approximately 70% complete for demo, not ready for production.

Open issue count by severity:

| Severity | Count | Summary |
| --- | ---: | --- |
| Critical | 2 | Path ambiguity; dirty release state |
| High | 5 | Dual backend; exception handler risk; secrets; payment webhook proof; security test gaps |
| Medium | 8 | UI lint/a11y/SEO/performance/refactor/encoding/deploy smoke/API contract gaps |
| Low | 4 | Documentation polish, file naming, content depth, legacy cleanup sequencing |

Answer to "co lung van de nao chua duoc giai quyet khong?": YES.

Most important unresolved items:

1. Clean and standardize repository state before any deploy or handover.
2. Fix/verify target backend global exception handling.
3. Keep legacy Thymeleaf but freeze it while React/REST becomes the main flow.
4. Harden secrets/config and verify staging deploy.
5. Complete B3 Hero scoring before moving to Navbar.

No database change, migration, delete/drop/truncate/reset, backend code change, frontend code change, or UI change was performed during this audit. Only this `PROJECT_AUDIT_REPORT.md` file was created.
