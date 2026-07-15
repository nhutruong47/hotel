# Master Project Plan - Nhu Villas

## 0. Governance

### 0.1 Source of Truth

Ke hoach nay duoc lap dua tren cac tai lieu da doc:

- `AI_PROJECT_FRAMEWORK/00_PROJECT_RULE.md` den `24_PROJECT_CHECKLIST.md`
- `UI_QUALITY_CHECKLIST.md`
- Hien trang repo: `frontend` la React/Vite/Tailwind v4; `hotel` la Spring Boot 3.4.1, Java 21, SQL Server, Thymeleaf legacy, Spring Security, JPA.

### 0.2 Missing Source Document

`LUXURY_EXPERIENCE_BLUEPRINT.md` duoc user yeu cau doc truoc, nhung khong tim thay trong repo hien tai. Vi vay:

- Khong co mau thuan noi dung de doi chieu truc tiep voi file nay.
- Tam thoi ap dung `00_PROJECT_RULE.md` va `UI_QUALITY_CHECKLIST.md` lam luxury source of truth: Japanese Wabi-Sabi, minimalism, boutique luxury, Aman/Six Senses/Rosewood/Airbnb Luxe quality bar.
- Neu file blueprint duoc bo sung sau nay, moi sprint sau do phai doi chieu lai va cap nhat plan neu co mau thuan.

### 0.3 Quality Threshold Resolution

Tai lieu co mot diem khac nhau:

- `12_TESTING_GUIDE.md` va `24_PROJECT_CHECKLIST.md` co noi Lighthouse Performance >= 90 trong mot so muc.
- User va `UI_QUALITY_CHECKLIST.md` yeu cau Production: SEO >= 98, Performance >= 95, Accessibility >= 95, Responsive >= 95.

Quyet dinh: Production gate lay nguong nghiem hon:

- UI >= 95
- UX >= 95
- Luxury >= 95
- Motion >= 95
- SEO >= 98
- Performance >= 95
- Accessibility >= 95
- Responsive >= 95
- Conversion >= 95

Khong dat bat ky gate nao thi sprint FAILED va khong duoc merge.

### 0.4 Protected Areas

Cho den khi co phe duyet rieng:

- Khong sua database schema.
- Khong sua booking write logic.
- Khong xoa Thymeleaf public flow cu.
- Khong sua admin/report/auth neu sprint khong co scope ro.
- Khong thay doi design system core tokens neu chua co sprint foundation duoc phe duyet.

---

## 1. Master Epic Plan

### EPIC 1 - Project Governance and Architecture Baseline

Muc tieu: tao nen tang dieu hanh du an theo chuan enterprise, co ranh gioi ro giua React frontend, Spring Boot backend, database, legacy Thymeleaf va CI/CD.

Sprints:

- Sprint 1.1: Repo audit and architecture map
- Sprint 1.2: Source-of-truth alignment and ADR setup
- Sprint 1.3: Protected-area policy and sprint approval workflow
- Sprint 1.4: Environment and runbook standardization

### EPIC 2 - Frontend Foundation

Muc tieu: dua frontend React/Vite ve kien truc feature-based, co routing, providers, API layer, state strategy, error boundary, loading/error/empty state foundation.

Sprints:

- Sprint 2.1: App shell, providers, routing baseline
- Sprint 2.2: Design token and Tailwind v4 foundation
- Sprint 2.3: Shared component primitives
- Sprint 2.4: API client and React Query integration
- Sprint 2.5: Global error boundary and system states
- Sprint 2.6: Responsive layout foundation

### EPIC 3 - Luxury Experience

Muc tieu: xay dung public experience dat luxury resort standard, uu tien cam xuc, hinh anh that/art-directed, copy sang trong, motion co muc dich.

Sprints:

- Sprint 3.1: Luxury content and visual direction audit
- Sprint 3.2: Hero
- Sprint 3.3: Navbar
- Sprint 3.4: Villa Collection
- Sprint 3.5: Villa Detail
- Sprint 3.6: Amenities
- Sprint 3.7: Gallery
- Sprint 3.8: Experience
- Sprint 3.9: Reviews and social proof
- Sprint 3.10: Contact and location
- Sprint 3.11: Footer

### EPIC 4 - Booking Experience

Muc tieu: tao booking funnel it buoc, minh bach gia, khong fake urgency, ho tro ca instant booking va concierge request.

Sprints:

- Sprint 4.1: Booking journey audit
- Sprint 4.2: Availability search UI
- Sprint 4.3: Date range calendar
- Sprint 4.4: Guest selector and preferences
- Sprint 4.5: Pricing summary
- Sprint 4.6: Booking review and confirmation
- Sprint 4.7: Success page and email expectation
- Sprint 4.8: Booking edge states

### EPIC 5 - Customer Features

Muc tieu: hoan thien cac tinh nang khach hang khong phai booking core: wishlist, reviews, profile-facing journeys, AI recommendation neu giu lai.

Sprints:

- Sprint 5.1: Customer dashboard IA
- Sprint 5.2: My bookings read experience
- Sprint 5.3: Wishlist
- Sprint 5.4: Reviews and rating flow
- Sprint 5.5: AI recommendation UX guardrails
- Sprint 5.6: Notification center read experience

### EPIC 6 - Authentication and Security

Muc tieu: chuan hoa login/register/session/password reset theo Spring Security, RBAC backend-first, OWASP Top 10, UX khong lam mat cam giac luxury.

Sprints:

- Sprint 6.1: Auth security audit
- Sprint 6.2: Login and register UI
- Sprint 6.3: Email verification and password reset
- Sprint 6.4: Session handling and protected routes
- Sprint 6.5: RBAC and admin boundary review

### EPIC 7 - Profile

Muc tieu: profile, avatar, password, preferences, communication settings theo UX an tam va bao mat.

Sprints:

- Sprint 7.1: Profile overview
- Sprint 7.2: Edit profile and avatar
- Sprint 7.3: Change password
- Sprint 7.4: Preferences and communication consent

### EPIC 8 - Admin

Muc tieu: admin chuan operational tool: ro rang, day du, it trang tri, bao mat, audit duoc. Khong ap luxury public UI vao dashboard.

Sprints:

- Sprint 8.1: Admin IA and permission matrix
- Sprint 8.2: Admin layout and navigation
- Sprint 8.3: Booking management
- Sprint 8.4: Villa/room management
- Sprint 8.5: Amenities and vouchers
- Sprint 8.6: Users and roles
- Sprint 8.7: Reports and exports
- Sprint 8.8: Admin audit and monitoring

### EPIC 9 - Marketplace

Muc tieu: de danh. Hien tai khong co bang chung nhu cau marketplace trong source docs hay repo.

Sprints:

- Sprint 9.1: Marketplace discovery decision
- Sprint 9.2: Marketplace IA only if approved
- Sprint 9.3: Marketplace API only if approved
- Sprint 9.4: Marketplace UI only if approved

### EPIC 10 - SEO and Content

Muc tieu: SEO >= 98, crawlable semantic content, structured data hop le, no fake schema, sitemap/robots/canonical dung route that.

Sprints:

- Sprint 10.1: Keyword and search intent map
- Sprint 10.2: Route metadata
- Sprint 10.3: Semantic heading and internal link audit
- Sprint 10.4: Hotel, FAQ, Breadcrumb schema
- Sprint 10.5: Sitemap, robots, canonical
- Sprint 10.6: Open Graph and social previews
- Sprint 10.7: Content QA and copy refinement

### EPIC 11 - Performance

Muc tieu: Performance >= 95, LCP < 2.5s, CLS < 0.1, INP < 200ms, asset strategy production-ready.

Sprints:

- Sprint 11.1: Performance baseline audit
- Sprint 11.2: Image and video optimization
- Sprint 11.3: Font loading optimization
- Sprint 11.4: Route code splitting and bundle budget
- Sprint 11.5: Motion performance profiling
- Sprint 11.6: CDN and cache headers

### EPIC 12 - Accessibility

Muc tieu: Accessibility >= 95, WCAG 2.1 AA, keyboard-first, focus management, reduced motion.

Sprints:

- Sprint 12.1: Accessibility baseline audit
- Sprint 12.2: Semantic landmarks and skip link
- Sprint 12.3: Keyboard navigation
- Sprint 12.4: Form labels and error linkage
- Sprint 12.5: Modal/drawer focus trap
- Sprint 12.6: Reduced motion
- Sprint 12.7: Contrast and screen reader pass

### EPIC 13 - Backend API and Domain

Muc tieu: API contract enterprise-ready, REST v1, DTO ro rang, validation, exceptions, booking/payment/email/notification/logging/monitoring co chuan.

Sprints:

- Sprint 13.1: Backend architecture audit
- Sprint 13.2: API contract and OpenAPI
- Sprint 13.3: DTO and validation standard
- Sprint 13.4: Exception envelope standard
- Sprint 13.5: Booking service hardening
- Sprint 13.6: Payment integration plan
- Sprint 13.7: Email and notification hardening
- Sprint 13.8: Logging and monitoring

### EPIC 14 - Database and Data Governance

Muc tieu: danh gia entity/relationship/index/constraint/migration/audit fields, chi thay schema khi duoc phe duyet.

Sprints:

- Sprint 14.1: ERD and data dictionary
- Sprint 14.2: Relationship and FK audit
- Sprint 14.3: Index and query audit
- Sprint 14.4: Constraint and audit field audit
- Sprint 14.5: Migration strategy
- Sprint 14.6: Backup and data retention

### EPIC 15 - Testing and QA

Muc tieu: test pyramid thuc dung, regression an toan, UI review co scorecard, E2E cho money flows.

Sprints:

- Sprint 15.1: Test strategy setup
- Sprint 15.2: Backend unit/integration tests
- Sprint 15.3: Frontend unit/component tests
- Sprint 15.4: E2E critical flows
- Sprint 15.5: Visual and responsive QA
- Sprint 15.6: Security and accessibility QA

### EPIC 16 - Deployment and Operations

Muc tieu: staging/production separation, CI/CD, rollback, security headers, monitoring, backup.

Sprints:

- Sprint 16.1: Environment strategy
- Sprint 16.2: CI pipeline
- Sprint 16.3: Container/hosting strategy
- Sprint 16.4: Security headers and secrets
- Sprint 16.5: Monitoring and alerting
- Sprint 16.6: Release and rollback drill

---

## 2. Backend Plan

### 2.1 API

Chuan:

- RESTful `/api/v1/*`.
- Resource plural nouns, kebab-case.
- Response envelope:
  - Single object: `{ "data": object }`
  - List: `{ "data": [], "meta": { "page", "limit", "totalRecords", "totalPages" } }`
  - Error: `{ "error": { "code", "message", "details" } }`
- OpenAPI 3.0 la bat buoc truoc khi frontend consume.
- Public API: villas/rooms, room detail, reviews summary, content, availability read.
- Protected API: profile, wishlist, my bookings, booking create/cancel/payment action.
- Admin API: bookings, rooms/villas, amenities, vouchers, users, reports.

### 2.2 DTO

Chuan:

- Khong expose entity truc tiep.
- DTO tach request/response.
- DTO dat theo use case: `BookingCreateRequest`, `BookingSummaryResponse`, `VillaDetailResponse`.
- Field nhay cam nhu password hash, reset token, email verification token khong bao gio tra ve.
- DTO versioning theo API v1 neu contract thay doi.

### 2.3 Service

Chuan:

- Service chua business rules, controller chi dieu phoi request/response.
- BookingService la domain critical: availability, overlap, status transition, pricing, cancellation, deadline.
- PaymentService rieng, khong tron logic payment vao controller.
- EmailService/NotificationService idempotent, co retry strategy.

### 2.4 Repository

Chuan:

- Repository chi query data, khong chua business decisions.
- Query phuc tap can dat ten ro va co index review.
- Khong query entity lazy gay N+1 tren API list.

### 2.5 Validation

Chuan:

- Bean Validation tren DTO: required, min/max, date range, phone/email.
- Backend validate lai tat ca input, khong tin frontend.
- Booking validation gom: date order, guest limit, room/villa availability, status transition hop le, price recalc server-side.

### 2.6 Exception

Chuan:

- Global exception handler tra error envelope.
- Khong tra stacktrace production.
- Ma loi domain ro: `VALIDATION_FAILED`, `RESOURCE_NOT_FOUND`, `BOOKING_DATE_UNAVAILABLE`, `PAYMENT_REQUIRED`, `FORBIDDEN`.

### 2.7 Authentication

Chuan:

- Spring Security backend-first.
- BCrypt/Argon2, khong plain password.
- Neu chuyen sang token: access token ngan han, refresh token HttpOnly/Secure/SameSite.
- Password reset token co expiry, one-time use.

### 2.8 Authorization

Chuan:

- RBAC tren backend, frontend chi an/hien UI.
- Admin endpoints bat buoc permission check.
- IDOR test cho booking/profile/review/wishlist.

### 2.9 Booking Logic

Can lam ro:

- Status: `PENDING`, `AWAITING_PAYMENT`, `CONFIRMED`, `REJECTED`, `CANCELLED`, `COMPLETED`.
- Transition matrix bat buoc.
- Availability phai chong race condition khi dat cung ngay.
- Pricing server-side, khong tin client.
- Cancellation policy ro trong UI va API.

### 2.10 Payment Integration

Giai doan hien tai payment co ve dang mo phong. Plan production:

- Chon provider sau approval: Stripe, PayPal, VNPay, MoMo, hoac payment gateway dia phuong.
- Payment intent/order id luu server-side.
- Webhook validation signature.
- Idempotency key cho callback.
- Booking chi `CONFIRMED` khi payment verified.

### 2.11 Email

Chuan:

- Email templates rieng cho booking approved/rejected/cancelled/payment/reset/verify.
- Khong block request qua lau vi email; can async queue hoac retry.
- Log trang thai gui email khong lo secret/token.

### 2.12 Notification

Chuan:

- Notification read model cho customer/admin.
- Admin alert khi booking moi/payment received.
- User notification cho booking status/payment deadline.

### 2.13 Logging

Chuan:

- Structured logs: request id, user id neu co, endpoint, status, latency.
- Khong log password, token, card data, secret.
- Booking/payment status change co audit log.

### 2.14 Monitoring

Chuan:

- Health endpoint cho uptime.
- Error tracking: Sentry hoac tuong duong.
- Metrics: API latency, error rate, booking conversion, payment failure, email failure.
- Alert: 5xx spike, payment webhook fail, DB connection error.

---

## 3. Frontend Plan

### 3.1 Routing

Public routes:

- `/`
- `/villas`
- `/villas/:slug`
- `/experiences`
- `/gallery`
- `/reviews`
- `/contact`
- `/booking`
- `/booking/success`
- `/faq`
- `/privacy`
- `/terms`

Customer routes:

- `/account`
- `/account/bookings`
- `/account/wishlist`
- `/account/reviews`
- `/account/profile`

Auth routes:

- `/login`
- `/register`
- `/forgot-password`
- `/reset-password`

Admin routes:

- `/admin`
- `/admin/bookings`
- `/admin/villas`
- `/admin/amenities`
- `/admin/vouchers`
- `/admin/users`
- `/admin/reports`

### 3.2 Feature Modules

Target structure:

- `src/app`: providers, router, query client, root layout
- `src/domains`: pure domain models/services
- `src/features/home`
- `src/features/villas`
- `src/features/booking`
- `src/features/auth`
- `src/features/profile`
- `src/features/admin`
- `src/features/content`
- `src/shared`: components, hooks, utils, layouts

Rule: feature khong import truc tiep feature khac; shared/domain moi duoc chia se.

### 3.3 API Layer

Chuan:

- Mot API client duy nhat.
- Khong hard-code URL trong UI component.
- Typed request/response.
- 401/403/error envelope parse tap trung.
- Env validation cho `VITE_API_BASE_URL`.

### 3.4 React Query

Chuan:

- Server state bat buoc qua React Query.
- Query key co namespace: `['villas', filters]`, `['booking', id]`.
- Mutations co optimistic update chi khi an toan.
- Loading/error/empty state per section, khong full-page spinner neu chi mot section load.

### 3.5 State Management

Chuan:

- Local UI state dung `useState`/`useReducer`.
- Global state chi cho auth session, theme/preferences, booking draft neu can.
- Khong dung Redux neu chua co nhu cau that.

### 3.6 Forms

Chuan:

- Label bat buoc, placeholder khong thay label.
- Inline validation linked bang `aria-describedby`.
- Booking forms step-based, it field, pricing ro.
- Error copy than thien, khong do loi nguoi dung.

### 3.7 Error Handling

Chuan:

- Global Error Boundary.
- Feature-level Error Boundary cho booking/admin.
- Empty/error offline/slow network state co CTA tiep theo.

### 3.8 Loading

Chuan:

- Skeleton khop 90% geometry final UI.
- Button loading disable click.
- No full-screen loader cho section-level data.

### 3.9 Empty State

Chuan:

- Khong "No data" khong ngữ cảnh.
- Co ly do va next action: clear filters, choose dates, contact concierge.

### 3.10 Motion

Chuan:

- GSAP/Lenis chi dung cho storytelling co muc dich.
- Transform/opacity only.
- `prefers-reduced-motion` gate bat buoc.
- Cleanup GSAP context bat buoc.

### 3.11 Responsive

Chuan:

- Mobile-first.
- Breakpoints QA: 320, 375, 768, 1024, 1440, 1920.
- Khong horizontal overflow.
- Touch target >= 44px.

---

## 4. Database Plan

### 4.1 Entities To Assess

Current known entities:

- User
- Room/Villa
- RoomType
- Amenity
- Booking
- BookingStatus
- Review
- Voucher
- WishlistItem
- Blog
- ChatMessage

Production naming should gradually align business language from room/hotel to villa/resort, but schema rename requires explicit approval.

### 4.2 Relationships

Assess:

- User 1-N Booking
- Villa/Room 1-N Booking
- Booking 1-1/1-N Payment if payment becomes real
- Villa/Room N-N Amenity
- User 1-N Review
- Booking 1-1 Review eligibility
- User 1-N WishlistItem
- Villa/Room 1-N WishlistItem
- User/Admin audit references

### 4.3 Index

Candidate indexes:

- bookings: `user_id`, `room_id`, `status`, `check_in_date`, `check_out_date`, `created_at`
- rooms/villas: `is_available`, `room_type_id`, `price_per_night`
- reviews: `room_id`, `user_id`, `booking_id`, `created_at`
- users: `email`, `username`, `role`
- wishlist: composite `user_id`, `room_id`

No index added until query audit confirms need.

### 4.4 Constraints

Assess:

- FK enforced at DB layer.
- NOT NULL for required fields.
- UNIQUE for email, username, room number, voucher code where applicable.
- Check constraints for rating 1-5, date order if DB supports.
- Soft delete for money/audit data.

### 4.5 Migration

Chuan:

- No manual DB GUI changes.
- Every schema change via migration file/tool.
- Migration must include rollback notes.
- Staging apply before production.
- Backup before production migration.

### 4.6 Audit Fields

Target:

- `created_at`
- `updated_at`
- `created_by`
- `updated_by`
- `deleted_at` for soft delete where appropriate

Current schema must be audited before any change.

---

## 5. UI/UX Plan

### 5.1 Hero

- UX goal: trong 5 giay dau tao cam giac den mot private villa resort thuc su, khong template.
- Conversion goal: CTA chinh "Check Availability" dan vao booking/villa selection; CTA phu "Explore Villas".
- Motion: media settles, H1 reveal, subcopy, CTA last; reduced motion = static/fade.
- SEO: one H1, crawlable copy, OG image dung hero.
- Responsive: mobile crop rieng, CTA visible without awkward scroll, no text overlap.

### 5.2 Navbar

- UX goal: dieu huong 5-7 item, calm, glass/sticky/shrink co kiem soat.
- Conversion goal: CTA booking visible desktop va mobile drawer.
- Motion: shrink on scroll, reveal on scroll up, menu stagger nhe.
- SEO: semantic `<nav>`, internal links crawlable.
- Responsive: hamburger <= tablet, 44px target, focus trap.

### 5.3 Villa Collection

- UX goal: browse villas nhu editorial collection, anh la trung tam.
- Conversion goal: moi card co "View Villa" va "Check Dates" role khac nhau.
- Motion: hover/focus image scale nhe, section reveal.
- SEO: article semantics, villa names H2/H3 logic, alt text cu the.
- Responsive: asymmetric desktop, stacked mobile, skeleton khop layout.

### 5.4 Villa Detail

- UX goal: giup khach hieu khong gian, suc chua, amenities, view, chinh sach.
- Conversion goal: sticky/visible availability entry khong gay ap luc.
- Motion: gallery mask reveal, detail sections fade/slide.
- SEO: breadcrumbs, LodgingBusiness/Offer data neu data that.
- Responsive: mobile gallery swipe/stack, booking summary khong che noi dung.

### 5.5 Booking

- UX goal: stay planning concierge-like, it buoc, minh bach.
- Conversion goal: reduce anxiety; pricing, policy, next step ro.
- Motion: step transition 0.4-0.6s, no parallax.
- SEO: booking routes noindex neu can, public booking intro crawlable neu huu ich.
- Responsive: mobile bottom sheet/date picker usable, 44px targets.

### 5.6 Experience

- UX goal: ke cau chuyen nghi duong theo scene: arrival, stillness, dining, pool, sunset.
- Conversion goal: tang desire truoc CTA.
- Motion: scroll storytelling, pin/parallax chi desktop neu performance pass.
- SEO: section headings match search intent.
- Responsive: horizontal story desktop chuyen vertical stack mobile.

### 5.7 Amenities

- UX goal: amenities doc nhanh, co cam giac vat chat va chat luong.
- Conversion goal: giam thac mac truoc booking.
- Motion: group reveal, hover/focus details nhe.
- SEO: amenity names crawlable, no icon-only meaning.
- Responsive: grid 4/2/1, no icon/text overlap.

### 5.8 Gallery

- UX goal: inspect resort/villa with confidence.
- Conversion goal: "see the stay" truoc khi check dates.
- Motion: mask reveal, swipe, manual controls; autoplay only if safe and pausable.
- SEO: alt text cu the, image sitemap if later approved.
- Responsive: desktop masonry/editorial, mobile swipe/stack.

### 5.9 Reviews

- UX goal: social proof tin cay, khong fake.
- Conversion goal: giam risk perception.
- Motion: subtle reveal; no noisy carousel.
- SEO: review schema chi dung neu co data that va policy hop le.
- Responsive: readable quote lengths, no tiny cards.

### 5.10 Contact

- UX goal: de lien he concierge, tim vi tri, hoi truoc khi dat.
- Conversion goal: fallback path cho khach chua san sang booking.
- Motion: map/contact reveal nhe.
- SEO: NAP consistency, local business info.
- Responsive: tap-to-call/email, form labels ro.

### 5.11 Footer

- UX goal: ket thuc trang thanh tinh, du link ho tro va phap ly.
- Conversion goal: newsletter/contact/booking secondary.
- Motion: fade in only.
- SEO: internal links, sitemap support.
- Responsive: columns collapse cleanly.

---

## 6. Quality Plan

### 6.1 Production PASS Gates

| Category | Required | Blocking Conditions |
|---|---:|---|
| UI | >=95 | Template/SaaS/Admin feeling on public UI |
| UX | >=95 | Dead-end flow, unclear next action |
| Luxury | >=95 | Generic stock/template, noisy decoration |
| Motion | >=95 | No reduced motion, jank, purposeless animation |
| SEO | >=98 | Missing H1, invalid schema, wrong canonical/sitemap |
| Performance | >=95 | LCP > 2.5s, CLS > 0.1, INP > 200ms |
| Accessibility | >=95 | Keyboard/contrast/focus/reduced-motion blocker |
| Responsive | >=95 | Horizontal overflow, overlap, broken 320-1920px |
| Conversion | >=95 | CTA unclear, booking path broken |

### 6.2 Mandatory Verification Per Sprint

- Build/typecheck/lint status reported.
- Affected routes manually inspected.
- Responsive at 375, 768, 1024, 1440.
- Keyboard navigation checked.
- Reduced motion checked if motion exists.
- SEO metadata/heading/schema checked if public route touched.
- Performance impact assessed if media/motion/bundle touched.
- File changed and protected-file-not-changed list reported.

---

## 7. Execution Order

Thu tu toi uu:

1. Governance and architecture baseline
2. Frontend foundation
3. Backend API contract audit
4. Database audit
5. Design system and luxury direction
6. Public luxury experience
7. Booking experience
8. Customer account features
9. Auth/profile hardening
10. Admin operational features
11. SEO/content pass
12. Performance pass
13. Accessibility pass
14. Testing/regression suite
15. Deployment/operations

Ly do:

- Foundation truoc UI de tranh sua lai nhieu lan.
- API/database audit truoc booking de khong lam hong money flow.
- Luxury public experience truoc SEO/performance final vi SEO/performance can audit tren UI/media that.
- Accessibility/performance phai chay lien tuc, nhung final hardening lam sau khi feature surface on dinh.
- Deployment cuoi cung vi can CI/test/security/rollback da san sang.

---

## 8. Risk Analysis

### 8.1 Technical Risk

Risks:

- React frontend va Thymeleaf backend song song gay route/API confusion.
- Legacy entity naming "Room" khong khop brand "Villa".
- Motion/Lenis/GSAP co the gay scroll bug.
- API chua co OpenAPI day du.

Mitigation:

- Route ownership map.
- Protected legacy policy.
- ADR cho naming migration.
- Motion QA per sprint.
- OpenAPI contract before integration.

### 8.2 UX Risk

Risks:

- Public UI thanh generic hotel template.
- Booking qua dai, khach bo cuoc.
- CTA spam lam mat luxury.

Mitigation:

- UI scorecard per section.
- Booking funnel test.
- CTA hierarchy rule: 1 primary intent per viewport.

### 8.3 SEO Risk

Risks:

- SPA metadata khong crawl tot neu chi client-render.
- Sitemap co route khong ton tai.
- Schema fake rating/review.

Mitigation:

- Route metadata audit.
- Consider SSR/prerender if needed.
- Schema only from real data.

### 8.4 Performance Risk

Risks:

- Hero media too heavy.
- GSAP/Lenis increases JS and main-thread work.
- Fonts/images cause LCP/CLS.

Mitigation:

- WebP/AVIF, responsive sizes, preload LCP.
- Lazy load heavy routes/sections.
- Bundle budget and Lighthouse gate.

### 8.5 Security Risk

Risks:

- IDOR in profile/booking/admin.
- Secret leakage through env/client.
- Payment webhook spoofing if real payment added.

Mitigation:

- Backend RBAC and ownership checks.
- Secret scanning.
- Webhook signature and idempotency.

### 8.6 Deployment Risk

Risks:

- Staging/prod env mismatch.
- Source maps/secrets exposed.
- DB migration without rollback.

Mitigation:

- Separate env config.
- CI deployment checklist.
- Backup and rollback drill.

---

## 9. Definition of Done Template

Every sprint must include this before execution:

```md
### Sprint X.Y - Name

Goal:

Allowed files:

Forbidden files:

Checklist:
- [ ] Scope matches Master Project Plan
- [ ] Protected areas not touched
- [ ] UX/UI/SEO/A11Y/Performance criteria checked as applicable
- [ ] Tests/audits run or limitation reported
- [ ] Scorecard completed

Tests:

Rollback plan:

PASS/FAILED:
```

---

## 10. Sprint Definition of Done Matrix

### EPIC 1

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 1.1 | Audit repo and map systems | Docs only | Runtime source | Inventory frontend/backend/db/legacy | No code tests | Revert doc | Pending |
| 1.2 | Align source of truth and ADR | Docs only | Source/config/schema | Missing docs noted, ADR rules set | Markdown review | Revert doc | Pending |
| 1.3 | Define protected areas | Docs only | Source/config/schema | Protected files listed | Review checklist | Revert doc | Pending |
| 1.4 | Standardize env/runbooks | README/docs only | Secrets, runtime code | Local/staging/prod commands documented | Dry-run commands if approved | Revert docs | Pending |

### EPIC 2

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 2.1 | App shell and routing baseline | `frontend/src/app`, root entry | Backend, DB, booking write | Routes lazy-ready, providers clean | build/lint | Revert frontend changes | Pending |
| 2.2 | Token/Tailwind foundation | `frontend/src/index.css`, config if approved | Backend, DB | Tokens match luxury palette | build, visual check | Revert token diff | Pending |
| 2.3 | Shared primitives | `frontend/src/shared` | Feature business logic | Button/input/card states complete | component tests/build | Revert components | Pending |
| 2.4 | API + React Query | `frontend/src/shared/api`, `src/app` | Backend behavior | Typed client, no hardcoded URLs | build/mock integration | Revert API layer | Pending |
| 2.5 | Error/system states | `frontend/src/shared`, `src/app` | Backend/schema | Error boundary, loading/empty patterns | build/manual crash test | Revert state components | Pending |
| 2.6 | Responsive foundation | Layout/shared CSS | Backend/schema | 320-1920 no overflow | responsive screenshots | Revert layout diff | Pending |

### EPIC 3

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 3.1 | Luxury direction audit | Docs/assets plan | Runtime source | Media/copy/motion direction approved | Design review | Revert doc | Pending |
| 3.2 | Hero | `frontend/src/features/home`, assets | Booking write, admin, DB | Hero score >=96, CTA/A11Y/perf pass | build, Lighthouse, responsive | Revert hero files | Pending |
| 3.3 | Navbar | shared layout/nav | Auth logic, admin, DB | Nav score >=95, mobile focus trap | build, keyboard test | Revert nav files | Pending |
| 3.4 | Villa Collection | villas/home feature | Booking write, schema | Collection score >=96 | build, responsive | Revert collection | Pending |
| 3.5 | Villa Detail | villas feature | Booking write, schema | Detail UX, breadcrumbs, gallery stable | build, route check | Revert detail | Pending |
| 3.6 | Amenities | villas/shared content | Schema unless approved | Icon/text accessible | build/a11y | Revert amenities UI | Pending |
| 3.7 | Gallery | gallery feature/assets | Backend/schema | Swipe/keyboard/reduced motion | build/responsive | Revert gallery | Pending |
| 3.8 | Experience | experiences feature | Backend/schema | Story arc, motion score >=95 | build/motion QA | Revert experience | Pending |
| 3.9 | Reviews | reviews feature | Fake schema/data | Real data only, social proof clear | build/SEO check | Revert reviews UI | Pending |
| 3.10 | Contact | contact feature | Backend email unless approved | Contact paths accessible | build/form a11y | Revert contact | Pending |
| 3.11 | Footer | shared layout/footer | Auth/admin/schema | Internal links, legal links | build/SEO link check | Revert footer | Pending |

### EPIC 4

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 4.1 | Audit booking journey | Docs/read-only code review | Booking write logic | Current flow mapped | No code tests | Revert doc | Pending |
| 4.2 | Availability search UI | booking frontend read UI | Booking write, DB | Clear dates/villa/guest entry | build/manual | Revert UI | Pending |
| 4.3 | Date range calendar | booking UI components | Backend/schema | Disabled states/a11y/mobile | keyboard/responsive | Revert calendar | Pending |
| 4.4 | Guest selector | booking UI | Backend/schema | Steppers, limits visible | build/a11y | Revert selector | Pending |
| 4.5 | Pricing summary | booking UI/API read | Pricing service unless approved | Server price shown clearly | integration/mock | Revert summary | Pending |
| 4.6 | Review confirmation | booking UI | Payment/DB | Itinerary-like summary | build/manual flow | Revert confirmation | Pending |
| 4.7 | Success page | booking success UI/email copy | Email service unless approved | Booking code/next steps | route test | Revert page | Pending |
| 4.8 | Booking edge states | booking UI | Write logic/schema | Unavailable/errors/offline handled | integration/manual | Revert edge states | Pending |

### EPIC 5

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 5.1 | Customer account IA | Docs/frontend account shell | Admin/backend/schema | Flows mapped | build if UI | Revert docs/UI | Pending |
| 5.2 | My bookings read | account frontend/API read | Booking write | Status readable, actions gated | integration/manual | Revert read UI | Pending |
| 5.3 | Wishlist | wishlist frontend/API read/mutation if approved | Schema | Empty/loading/error states | integration | Revert wishlist | Pending |
| 5.4 | Reviews flow | reviews frontend/API if approved | Fake data/schema | Eligibility clear | integration/e2e | Revert reviews | Pending |
| 5.5 | AI recommendation UX | AI frontend/docs | Secret/model config | Safe fallback, no overclaim | manual/error test | Revert AI UI | Pending |
| 5.6 | Notification center | notification frontend/API if approved | Email internals | Read/unread states clear | integration | Revert notifications | Pending |

### EPIC 6

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 6.1 | Auth security audit | Docs/read-only | Runtime source | OWASP/RBAC gaps listed | No code tests | Revert doc | Pending |
| 6.2 | Login/register UI | auth frontend | SecurityConfig unless approved | Labels/errors/a11y | build/form test | Revert UI | Pending |
| 6.3 | Email reset UX | auth frontend/API if approved | Token schema | Expiry/copy/errors clear | integration | Revert changes | Pending |
| 6.4 | Protected routes | frontend route guards | Backend RBAC | No protected flash | route tests | Revert guards | Pending |
| 6.5 | RBAC review | backend security/docs | DB schema | Backend-first authorization | backend tests | Revert security diff | Pending |

### EPIC 7

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 7.1 | Profile overview | profile frontend/API read | Auth core/schema | Data privacy clear | integration | Revert profile UI | Pending |
| 7.2 | Edit profile/avatar | profile frontend/backend if approved | Storage config/schema unless approved | Upload limits/errors | integration/security | Revert edit flow | Pending |
| 7.3 | Change password | profile/auth frontend/backend if approved | unrelated auth | Old/new validation | backend/frontend tests | Revert password diff | Pending |
| 7.4 | Preferences | profile frontend/docs | Schema unless approved | Consent explicit | manual/a11y | Revert preferences | Pending |

### EPIC 8

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 8.1 | Admin IA/RBAC | Docs/admin read-only | Public luxury UI | Permission matrix | Review | Revert doc | Pending |
| 8.2 | Admin layout | admin frontend/templates if chosen | Public UI | Operational, not luxury marketing | build/responsive | Revert layout | Pending |
| 8.3 | Booking management | admin booking UI/API | Public booking UI | Status actions safe | integration/backend | Revert admin booking | Pending |
| 8.4 | Villa management | admin villa UI/API | Public villa display unless scoped | CRUD states | integration | Revert admin villa | Pending |
| 8.5 | Amenities/vouchers | admin UI/API | Public UI/schema unless approved | Validation/errors | integration | Revert changes | Pending |
| 8.6 | Users/roles | admin users/security | Auth public UI | RBAC verified | security tests | Revert users diff | Pending |
| 8.7 | Reports/exports | admin reports/backend | Public UI | Export stable | backend tests | Revert reports | Pending |
| 8.8 | Admin audit/monitoring | backend/admin docs | Schema unless approved | Audit events defined | tests/log review | Revert audit diff | Pending |

### EPIC 9

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 9.1 | Decide marketplace need | Docs only | Source/schema | Business approval required | Review | Revert doc | Pending |
| 9.2 | Marketplace IA | Docs only | Source/schema | Only if approved | Review | Revert doc | Pending |
| 9.3 | Marketplace API | Backend API if approved | Existing booking logic unless scoped | Contract first | backend tests | Revert API | Pending |
| 9.4 | Marketplace UI | Frontend if approved | Existing public flow unless scoped | UX/SEO gates | build/e2e | Revert UI | Pending |

### EPIC 10

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 10.1 | Keyword/search map | Docs/content | Runtime source | Intent per route | Review | Revert doc | Pending |
| 10.2 | Metadata | route metadata/index | Backend/schema | Unique title/description | SEO audit | Revert metadata | Pending |
| 10.3 | Headings/internal links | public frontend | Admin/backend/schema | One H1, logical H2/H3 | SEO/manual | Revert heading diff | Pending |
| 10.4 | Schema | frontend/server metadata | Fake data | Valid Hotel/FAQ/Breadcrumb | schema validation | Revert schema | Pending |
| 10.5 | Sitemap/robots/canonical | `public`, metadata | Runtime logic | Only real routes | SEO audit | Revert files | Pending |
| 10.6 | OG/social | metadata/assets | Backend/schema | OG image/title match | social preview/manual | Revert OG | Pending |
| 10.7 | Copy refinement | public copy/content | Logic/schema | Brand voice, no stuffing | review/SEO | Revert copy | Pending |

### EPIC 11

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 11.1 | Performance baseline | Docs/audit | Source unless approved | LCP/CLS/INP captured | Lighthouse | Revert doc | Pending |
| 11.2 | Media optimization | assets/public/media usage | Business logic/schema | WebP/AVIF/srcset | Lighthouse/build | Revert assets/usages | Pending |
| 11.3 | Font optimization | index/html/css | Backend/schema | Required weights only | Lighthouse | Revert font diff | Pending |
| 11.4 | Code splitting | route/imports | Business logic | Bundle budget met | build/analyzer | Revert imports | Pending |
| 11.5 | Motion profiling | motion hooks/sections | Backend/schema | 60 FPS, reduced motion | manual/FPS | Revert motion | Pending |
| 11.6 | Cache/CDN | deploy config/docs | Source if not needed | Cache headers plan | preview audit | Revert config | Pending |

### EPIC 12

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 12.1 | A11Y baseline | Docs/audit | Source unless approved | Blockers listed | Lighthouse/aXe if available | Revert doc | Pending |
| 12.2 | Landmarks/skip link | root layout/shared | Backend/schema | Landmarks correct | keyboard/screen reader | Revert layout | Pending |
| 12.3 | Keyboard nav | interactive components | Backend/schema | Tab/Enter/Esc works | manual keyboard | Revert components | Pending |
| 12.4 | Forms a11y | forms/shared inputs | Backend/schema | Labels/describedby | form tests | Revert forms | Pending |
| 12.5 | Focus trap | modal/drawer/nav | Backend/schema | Trap/return focus | keyboard tests | Revert overlay | Pending |
| 12.6 | Reduced motion | motion hooks/css | Backend/schema | Complex motion disabled | OS setting test | Revert motion | Pending |
| 12.7 | Contrast/screen reader | CSS/components | Backend/schema | AA contrast | contrast/manual SR | Revert styles | Pending |

### EPIC 13

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 13.1 | Backend audit | Docs/read-only | Runtime code | Controllers/services mapped | No code tests | Revert doc | Pending |
| 13.2 | OpenAPI contract | backend docs/config if approved | DB schema | v1 contract complete | Swagger validation | Revert docs/config | Pending |
| 13.3 | DTO/validation | backend dto/controller/service | DB schema | No entity exposure | backend tests | Revert DTO diff | Pending |
| 13.4 | Exception envelope | backend config/api | DB schema | Standard errors | integration tests | Revert exception diff | Pending |
| 13.5 | Booking hardening | booking service/tests | UI/schema unless approved | Transition/availability safe | service tests | Revert booking diff | Pending |
| 13.6 | Payment plan | Docs/backend if approved | Real payment secrets/schema unless approved | Provider decision | tests if code | Revert payment diff | Pending |
| 13.7 | Email/notifications | email/notification backend | Templates unrelated | Async/retry/copy | backend tests | Revert email diff | Pending |
| 13.8 | Logging/monitoring | backend config/docs | Secrets | Structured safe logs | log review/tests | Revert logging | Pending |

### EPIC 14

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 14.1 | ERD/data dictionary | Docs only | Schema/source | Entities documented | Review | Revert doc | Pending |
| 14.2 | FK audit | Docs/read-only | Schema | FK gaps listed | DB introspection if approved | Revert doc | Pending |
| 14.3 | Index audit | Docs/read-only | Schema | Query/index plan | query review | Revert doc | Pending |
| 14.4 | Constraint/audit audit | Docs/read-only | Schema | Audit field gaps | review | Revert doc | Pending |
| 14.5 | Migration strategy | Docs/migration plan | Live schema | Rollback plan | review | Revert doc | Pending |
| 14.6 | Backup/retention | Docs/devops | Live infra | RPO/RTO defined | drill if approved | Revert doc | Pending |

### EPIC 15

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 15.1 | Test strategy | Docs/test config if approved | Feature logic | Pyramid defined | sample test | Revert config | Pending |
| 15.2 | Backend tests | `hotel/src/test` | Production logic unless fixing | Critical services covered | maven test | Revert tests | Pending |
| 15.3 | Frontend tests | frontend tests/config | Feature logic unless scoped | Components/hooks covered | npm test if setup | Revert tests | Pending |
| 15.4 | E2E flows | e2e tests/config | App logic unless fixing | Booking/auth/admin smoke | Playwright/Cypress | Revert tests | Pending |
| 15.5 | Visual/responsive QA | tests/docs | Source unless fixing | Screenshots matrix | responsive tests | Revert docs/tests | Pending |
| 15.6 | Security/a11y QA | tests/docs/config | Source unless fixing | OWASP/a11y blockers | audits | Revert tests/docs | Pending |

### EPIC 16

| Sprint | Goal | Allowed Files | Forbidden Files | Checklist | Tests | Rollback Plan | PASS/FAILED |
|---|---|---|---|---|---|---|---|
| 16.1 | Env strategy | docs/env examples | Real secrets | Dev/staging/prod separated | config review | Revert docs | Pending |
| 16.2 | CI pipeline | CI config | Source logic | lint/type/test/build | CI run | Revert CI | Pending |
| 16.3 | Hosting/container | Docker/deploy docs/config | Source logic | Chosen platform documented | build image/preview | Revert config | Pending |
| 16.4 | Headers/secrets | deploy config/docs | Real secrets | CSP/HSTS/CORS | security header check | Revert config | Pending |
| 16.5 | Monitoring | docs/config if approved | Secrets | Uptime/error/metrics | alert test | Revert config | Pending |
| 16.6 | Release rollback | docs/CI | Source/schema | Rollback drill complete | dry-run | Revert docs/config | Pending |

---

## 11. Approval Rule

Sau khi user duyet `MASTER_PROJECT_PLAN.md`:

- Moi task moi phai map vao mot Epic va Sprint trong plan.
- Neu task khong map duoc, phai tao proposal cap nhat plan truoc.
- Neu task mau thuan source-of-truth, phai chi ra truoc khi lam.
- Khong duoc merge sprint FAILED.
- Khong duoc sang sprint tiep theo neu sprint hien tai chua co review va user approval.
