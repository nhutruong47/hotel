# Enterprise Transformation Plan — Như Villa Booking Platform

> **Mission.** Evolve the current single-property villa booking MVP into an
> enterprise-grade, multi-tenant, payment-enabled, audit-compliant,
> AI-assisted platform on par with **Booking.com**, **Airbnb**, **Agoda**,
> **Oracle OPERA PMS**, and **Cloudbeds**.
>
> **Scope.** 10 sequential phases. Each item is a backlog ticket with
> *why / value / design / data / API / risk / dependency / acceptance /
> complexity*. No code is written in this document; this is the blueprint
> the engineering team will execute.

---

## 0. Reading Guide

- **Complexity** scale: **S** ≤ 1 day · **M** ≤ 1 week · **L** ≤ 1 month · **XL** > 1 month.
- **Priority** is recommended order within the phase; phases themselves run sequentially.
- **Dep.** lists phase dependencies on other items (either within this document or external).
- **AC** = Acceptance Criteria — the conditions the item is "done" against.

A complete roadmap summary is at the bottom (§12).

---

## Phase 1 — Critical Production Readiness

> Goal: launch a system that does not lose money, does not lose data, and
> does not double-book. Everything else builds on top.

### 1.1 Payment Gateway Integration (Stripe + VNPay/MoMo)

- **Why needed.** The current system books rooms but never collects money.
  No business can run without payments.
- **Business value.** Revenue capture; foundation for refunds, invoices,
  channel integrations, and partner commissions.
- **Technical design.**
  - Add a **Payments** bounded context with a `Payment` aggregate (one per
    attempt, many per booking).
  - Use **Stripe Payment Intents** for cards (international) and a
    **VNPay/MoMo** adapter (Vietnam domestic) behind a `PaymentGateway`
    interface (Strategy pattern).
  - Implement **idempotency keys** on every charge so retries are safe.
  - Capture = **manual** (bookings) so funds are held until check-in.
  - Webhook controller validates provider signature, then idempotently
    advances the booking state machine.
- **Database changes.**
  - New `payments` table: id, booking_id, gateway (enum), intent_id,
    method, amount (BigDecimal precision 12, scale 2), currency (ISO-4217),
    status, raw_response JSONB, created_at, captured_at, refunded_at.
  - New `refunds` table linked to `payments`.
  - New `payment_webhook_events` for idempotency (provider_event_id UNIQUE).
  - `bookings.currency` column; switch `totalPrice` to scale 2.
- **API changes.**
  - `POST /api/v1/payments/intent` → returns client secret / redirect URL.
  - Provider-specific callbacks only: `POST /api/v1/payments/webhook/stripe`
    (Stripe signature) and `/sepay` (exact API key). No generic status webhook.
  - `POST /api/v1/payments/{id}/capture` (admin).
  - `POST /api/v1/payments/{id}/refund` (admin or auto by cancellation policy).
  - `GET /api/v1/payments?bookingId=…` (owner / admin).
- **Risks.** PCI scope, gateway downtime, webhook ordering, currency FX
  drift, double-charge on retry.
- **Dependencies.** 1.3 (state machine), 1.6 (idempotency), 2.5 (security headers).
- **AC.** A guest can pay with a test card; webhook flips booking to `PAID`;
  refund reverses payment; webhook replays do not duplicate charges.
- **Complexity.** **XL**.

### 1.2 Tax, Service Charge & Invoice Engine

- **Why needed.** Every invoice needs VAT breakdown; city tax is mandated
  in many jurisdictions.
- **Business value.** Legal compliance; clean accounting; enables B2B
  bookings with invoicing.
- **Technical design.** `TaxRule` entity with conditions (date range,
  room type, guest country), resolution order, rounding mode. Stateless
  `TaxCalculator` service called by `PricingService`. Invoices generated
  server-side (OpenPDF or iText 7) and stored on the booking.
- **Database changes.**
  - `tax_rules` table (id, name, rate_pct, conditions JSONB, priority, active).
  - `invoices` table (booking_id, number, issued_at, totals_json, pdf_path).
  - `bookings.tax_amount`, `bookings.service_charge_amount`, `bookings.net_amount`.
- **API changes.**
  - `GET /api/v1/pricing/quote?roomId&checkIn&checkOut&guests&voucher` →
    full breakdown (net, tax, service, total).
  - `GET /api/v1/bookings/{id}/invoice.pdf`.
- **Risks.** Rounding errors, retroactive rule changes, currency conversion.
- **Dependencies.** 1.1.
- **AC.** Quote endpoint returns line items that sum to total; PDF invoice
  matches the quote; rounding to nearest 1 VND is consistent.
- **Complexity.** **L**.

### 1.3 Booking State Machine + Concurrency Safety

- **Why needed.** Two guests can currently double-book; status transitions
  are unconstrained; the admin override (`PUT /status`) bypasses rules.
- **Business value.** Eliminates overbooking risk; predictable SLA;
  correct refund computation.
- **Technical design.** Define a strict state machine:
  `DRAFT → HOLD → CONFIRMED → CHECKED_IN → CHECKED_OUT → COMPLETED`,
  side branches `CANCELLED` and `NO_SHOW`. Use a `BookingStatusTransition`
  table + `canTransition(from, to, role)` guard.
  - `BookingService.createBooking` starts in `HOLD` for `HOLD_TTL_MINUTES`
    (default 15 min) before releasing to `CONFIRMED` after payment.
  - Optimistic locking via `@Version` on `Booking`.
  - Pessimistic read lock (`SELECT … FOR UPDATE`) on conflict read.
- **Database changes.**
  - `bookings.version` column (Long).
  - `bookings.hold_expires_at` column.
  - `booking_status_transitions` audit table.
- **API changes.** New endpoints: `POST /bookings/{id}/check-in`,
  `POST /bookings/{id}/check-out`, `POST /bookings/{id}/no-show`,
  `POST /bookings/{id}/hold`.
- **Risks.** Migration of existing bookings into the new model; lock contention.
- **Dependencies.** 1.1 (payment sets `CONFIRMED`); 1.4 (cancellation uses FSM).
- **AC.** Two simultaneous POSTs for the same room/dates — exactly one wins;
  every status change writes to `booking_status_transitions`.
- **Complexity.** **L**.

### 1.4 Cancellation Policy Engine

- **Why needed.** Current logic is a single hard-coded 100/50/0% tier.
  Real platforms support per-property policies, non-refundable rates, and
  force majeure.
- **Business value.** Higher margin (non-refundable rates), better guest
  expectations, dispute defensibility.
- **Technical design.** `CancellationPolicy` entity linked to rate plan.
  Engine evaluates `(now, checkInDate, ratePlan, reason)` → `(refundAmount,
  feeAmount)`. Refund flows through payment gateway (1.1).
- **Database changes.** `cancellation_policies`, `rate_plans`,
  `rooms.rate_plan_id`.
- **API changes.** `POST /bookings/{id}/cancel` returns `CancellationQuote`
  first, then `POST /bookings/{id}/cancel/confirm`.
- **Risks.** Edge cases (same-day, time zones, force majeure).
- **Dependencies.** 1.1, 1.3.
- **AC.** Quote matches admin-stored policy; refunds execute via gateway.
- **Complexity.** **M**.

### 1.5 Flyway / Liquibase Migrations

- **Why needed.** Schema managed by `ddl-auto=update` + two hand-rolled
  `.sql` files. No migration history, no rollback, no peer review.
- **Business value.** Reproducible deployments; safe rollbacks; audit trail.
- **Technical design.** Adopt Flyway. Move all `migration.sql` /
  `update_constraint.sql` content into `db/migration/V1__…sql`,
  `V2__…sql`. Enable `baseline-on-migrate` for the existing DB.
  Disable `ddl-auto` in prod.
- **Database changes.** New `flyway_schema_history` table (auto-created).
- **API changes.** None.
- **Risks.** First-time migration against existing data must be non-destructive.
- **Dependencies.** 1.1, 1.3 (schema must be migrated before new columns).
- **AC.** Boot succeeds on a fresh DB; boot succeeds on existing DB;
  rollback to previous version documented.
- **Complexity.** **S**.

### 1.6 Idempotency Layer for POSTs

- **Why needed.** Network retries currently cause duplicate bookings and
  duplicate payment intents.
- **Business value.** Eliminates double-charge, double-booking, double-email.
- **Technical design.** `Idempotency-Key` header on every mutating endpoint;
  the `IdempotencyService` stores `(key, user_id, endpoint, response, status)`
  in Redis (TTL 24 h). On replay, returns the cached response.
- **Database changes.** `idempotency_records` (fallback if Redis down).
- **API changes.** Document `Idempotency-Key` requirement on POST endpoints.
- **Risks.** Cache eviction before TTL; partial writes.
- **Dependencies.** 8.3 (Redis).
- **AC.** Same key + body returns same response; different body returns 409.
- **Complexity.** **M**.

### 1.7 Distributed Session + Distributed Cache (Redis)

- **Why needed.** Spring session is in-memory → no horizontal scale; no
  rate limiting, no idempotency store.
- **Business value.** Horizontal scale, low-latency reads, foundation for
  6.x (security) and 7.x (performance).
- **Technical design.** Spring Session Data Redis; Caffeine for local L1.
- **Database changes.** None.
- **API changes.** None externally.
- **Risks.** Session lost on Redis outage → graceful fallback to memory.
- **Dependencies.** 8.3 (Redis infra).
- **AC.** Restart one backend instance; users remain logged in.
- **Complexity.** **M**.

### 1.8 Currency Support & Minor Units

- **Why needed.** `BigDecimal(precision=12, scale=0)` cannot represent
  USD cents. No `currency` field anywhere.
- **Business value.** Multi-currency support; correct accounting.
- **Technical design.** Add `currency` (ISO-4217) to Room, Booking,
  Payment. Money stored with `scale=2`; display format via `Locale`.
- **Database changes.** `currency CHAR(3) NOT NULL DEFAULT 'VND'` on
  Rooms, Bookings, Payments, Vouchers; widen `BigDecimal` columns to scale 2.
- **API changes.** All money DTOs include `currency`.
- **Risks.** Migration of existing data; report SQL may need rewriting.
- **Dependencies.** 1.1, 1.2.
- **AC.** Existing 3,500,000 VND booking displays unchanged; a USD-priced
  villa shows USD throughout.
- **Complexity.** **S**.

### 1.9 Guest Communication Pipeline

- **Why needed.** Guests receive almost no emails today (no confirmation,
  no cancellation, no reminder). This is the lowest-effort, highest-CSAT
  improvement.
- **Business value.** Reduced no-shows, fewer support tickets, better reviews.
- **Technical design.**
  - Replace hardcoded HTML in `EmailService` with **Handlebars/Mustache
    templates** stored in `resources/templates/email/`.
  - Wire `sendBookingCancelledEmail` (defined but unused).
  - Add `sendBookingConfirmedToGuest`, `sendCheckInReminder`,
    `sendCheckOutThankYou`, `sendReviewRequest`.
  - Persistent `email_log` table (id, recipient, template, booking_id,
    status, provider_message_id, sent_at).
  - Switch from blocking send to **async** via `@Async` or a small queue.
- **Database changes.** `email_log` table.
- **API changes.** None externally; admin endpoint to resend.
- **Risks.** SMTP outage; bounce handling.
- **Dependencies.** 1.1 (email template can reference payment status).
- **AC.** Cancellation triggers an email; guest receives confirmation;
  log row recorded for every send.
- **Complexity.** **M**.

### 1.10 Admin Approval Pipeline Endpoints + UI

- **Why needed.** `approveBooking`, `rejectBooking`, `confirmPayment`
  exist in service but have **no HTTP endpoint or UI**. The admin flow is
  effectively unreachable.
- **Business value.** Allows controlled-by-payment bookings; required for
  most B2B and high-AOV workflows.
- **Technical design.** Add `AdminBookingApi` endpoints, surface them in
  the admin dashboard with state-machine guards (1.3). Cancel endpoint
  reuses policy engine (1.4).
- **Database changes.** None (state machine columns added in 1.3).
- **API changes.** `POST /admin/bookings/{id}/approve`, `…/reject`,
  `…/confirm-payment`, `…/cancel`.
- **Risks.** Authorization bypass — every endpoint must check role + ownership.
- **Dependencies.** 1.3, 1.4.
- **AC.** Admin can approve → guest receives email → guest pays → admin
  confirms → status CHECKED_IN is reachable later.
- **Complexity.** **M**.

### Phase 1 Summary

| ID | Item | Complexity |
|---|---|---|
| 1.1 | Payment Gateway | XL |
| 1.2 | Tax & Invoice | L |
| 1.3 | Booking State Machine + Locks | L |
| 1.4 | Cancellation Policy | M |
| 1.5 | Flyway | S |
| 1.6 | Idempotency Layer | M |
| 1.7 | Distributed Session + Cache | M |
| 1.8 | Currency & Scale | S |
| 1.9 | Guest Communication | M |
| 1.10 | Admin Approval Pipeline | M |

**Phase 1 exit gate:** payments work end-to-end, no race conditions, every
status change audited, all money amounts are correct in the chosen currency.

---

## Phase 2 — Enterprise Architecture

> Goal: make the platform tenanted, modular, observable, and ready to host
> many properties.

### 2.1 Multi-Tenancy (Property Model)

- **Why needed.** Today the system hard-codes one property; SaaS ambitions
  require many.
- **Business value.** Foundation for SaaS revenue.
- **Technical design.** Add `Property` aggregate root. All domain entities
  carry `property_id` (shared-schema multi-tenancy with row-level isolation
  via a Hibernate `@Filter`). Tenant resolution by subdomain
  (`villaA.app.com`) or JWT claim. Migration: `property_id` column added
  to all existing tables; default value from `DataInitializer`.
- **Database changes.** `properties` table; `property_id` FK on every
  domain table; `idx_*_property` indexes.
- **API changes.** New `Property` CRUD for admin; `/api/v1/properties/...`;
  every list endpoint supports `?propertyId=`.
- **Risks.** Data isolation bugs → cross-tenant leaks; massive migration.
- **Dependencies.** 1.5.
- **AC.** Two properties seeded; queries are auto-scoped; manual cross-tenant
  query returns empty.
- **Complexity.** **XL**.

### 2.2 Bounded-Context Modularisation (Modulith → Microservices-ready)

- **Why needed.** All code in one package; not ready for team scaling.
- **Business value.** Independent team velocity; clearer ownership.
- **Technical design.** Move to a Spring Modulith layout: `booking`,
  `payment`, `identity`, `catalog` (villa/type/amenity), `content`,
  `communication`, `reporting`. Each context has its own package, events
  (`BookingConfirmed`, `PaymentCaptured`), and database tables can later
  be split.
- **Database changes.** None (table ownership documented).
- **API changes.** None externally; internal event bus.
- **Risks.** Over-modularisation; cycles between modules.
- **Dependencies.** 2.1.
- **AC.** Module boundaries enforced by Modulith tests; no cross-context
  JPA imports.
- **Complexity.** **L**.

### 2.3 CQRS for Heavy Reads (Search, Reporting)

- **Why needed.** Reports currently run on transactional tables → slow,
  lock contention.
- **Business value.** Predictable report latency; protects OLTP.
- **Technical design.** Read model projected from events into a
  `reporting` schema (Postgres `MATERIALIZED VIEW`s for nightly, or
  ClickHouse for analytics). Reports API queries the read model only.
- **Database changes.** New `reporting` schema + materialized views.
- **API changes.** Existing report endpoints switch to read model.
- **Risks.** Eventual consistency; report lag.
- **Dependencies.** 2.2, 1.3.
- **AC.** P95 report latency < 1 s for a year of bookings.
- **Complexity.** **L**.

### 2.4 API Versioning + OpenAPI

- **Why needed.** API is undocumented; breaking changes are likely.
- **Business value.** External partner integrations; partner trust.
- **Technical design.** Adopt springdoc-openapi; publish `/v3/api-docs`;
  enforce URL-based versioning (`/api/v2/...`); deprecation policy with
  `Sunset` headers.
- **Database changes.** None.
- **API changes.** All endpoints decorated; clients can download SDKs.
- **Risks.** Old clients break.
- **Dependencies.** None.
- **AC.** Swagger UI live; SDKs generated; CI fails on undocumented endpoint.
- **Complexity.** **M**.

### 2.5 Security Hardening Baseline

- **Why needed.** Today: no rate limit, no CSRF, no security headers.
- **Business value.** Survives OWASP top-10 baseline.
- **Technical design.**
  - Spring Security with proper `csrf().cookieHttpOnlyRepository()` for
    cookie auth; explicit CORS allow-list via env.
  - Bucket4j filters on `/auth/*` (5 req/min/IP) and `/bookings` (30 req/min/user).
  - Add headers: `Content-Security-Policy`, `Strict-Transport-Security`,
    `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`,
    `Permissions-Policy` minimal.
- **Database changes.** None.
- **API changes.** 429 responses for throttled requests.
- **Risks.** False-positive throttling on shared NATs.
- **Dependencies.** 1.7 (Redis for token bucket).
- **AC.** Security headers present in responses; brute-force locked.
- **Complexity.** **M**.

### 2.6 Audit Log Foundation

- **Why needed.** Compliance (PCI, GDPR) requires who-did-what-when.
- **Business value.** Defensible in disputes; required for SOC2.
- **Technical design.** JPA `AuditingEntityListener` + `AuditorAware`
  backed by session context. Emit events for sensitive actions
  (refund, role change, password reset) into an immutable `audit_log`
  table (append-only, signed hash chain optional later).
- **Database changes.** `audit_log` (append-only, no UPDATE grant in prod).
- **API changes.** `GET /admin/audit-log?entity=Booking&id=…`.
- **Risks.** Volume; PII leakage.
- **Dependencies.** 1.7.
- **AC.** Every booking status change appears in audit_log with actor.
- **Complexity.** **M**.

### 2.7 Observability Stack (Metrics, Tracing, Logging)

- **Why needed.** Single-instance, no metrics, no tracing.
- **Business value.** Detect issues before customers do.
- **Technical design.**
  - Micrometer → Prometheus endpoint (scrape).
  - OpenTelemetry traces; propagate `traceparent` header.
  - Structured JSON logs (already in place) shipped to Loki / ELK.
  - SLOs defined: availability 99.9 %, P95 booking-create < 600 ms.
- **Database changes.** None.
- **API changes.** `/actuator/prometheus` (gated by role).
- **Risks.** Card storage of high-cardinality labels (e.g. userId).
- **Dependencies.** 8.4 (infra).
- **AC.** Grafana dashboard with golden signals.
- **Complexity.** **M**.

### 2.8 Background Job Platform (Quartz / Kafka)

- **Why needed.** `BookingScheduler` is dead; need hold-expiry, reminders,
  reports.
- **Business value.** Reliable, observable async work.
- **Technical design.** Spring Boot + Quartz (DB-backed) for scheduled
  jobs. Kafka or outbox-pattern events for cross-context workflows
  (payment → booking → email).
- **Database changes.** `QRTZ_*` tables (auto-created).
- **API changes.** None.
- **Risks.** Job overlap; missed triggers.
- **Dependencies.** 1.3.
- **AC.** Hold-expiry job marks expired holds as `EXPIRED`.
- **Complexity.** **L**.

### Phase 2 Summary

| ID | Item | Complexity |
|---|---|---|
| 2.1 | Multi-Tenancy | XL |
| 2.2 | Modulith | L |
| 2.3 | CQRS for reads | L |
| 2.4 | API Versioning + OpenAPI | M |
| 2.5 | Security baseline | M |
| 2.6 | Audit log | M |
| 2.7 | Observability | M |
| 2.8 | Background jobs | L |

---

## Phase 3 — Business Logic Completeness

> Goal: parity with PMS-grade workflows.

### 3.1 Rate Plans (BAR, Refundable, Non-Refund, Mobile-Only)

- **Why needed.** Real platforms price the same room differently by plan.
- **Business value.** Higher margin; targeted promotions.
- **Technical design.** `RatePlan` entity per room with restrictions
  (min/max stay, closed-to-arrival, mobile-only flag). `PricingService`
  picks the cheapest applicable plan.
- **Database changes.** `rate_plans` table; `booking_rooms.rate_plan_id`.
- **API changes.** Quote endpoint returns `ratePlanId`.
- **Risks.** UI complexity.
- **Dependencies.** 1.2, 1.4.
- **AC.** Non-refundable rate is 10 % cheaper than BAR; cancelling returns 0.
- **Complexity.** **L**.

### 3.2 Seasonal / Weekend / Holiday Pricing

- **Why needed.** Hard-coded price-per-night is unrealistic.
- **Business value.** Yield management.
- **Technical design.** `RatePlanRule` per (plan, dateRange, dayOfWeek)
  with multiplier or override. Holiday calendar per country.
- **Database changes.** `rate_plan_rules`, `holiday_calendars`.
- **API changes.** Calendar endpoint `/villas/{id}/price-calendar`.
- **Risks.** Rule ordering bugs.
- **Dependencies.** 3.1.
- **AC.** Friday-Saturday prices 20 % higher; Tet dates priced at holiday rate.
- **Complexity.** **M**.

### 3.3 Multi-Room Booking + Add-Ons

- **Why needed.** Today 1 booking = 1 room; no extras.
- **Business value.** Larger baskets; B2B feasibility.
- **Technical design.** `BookingGroup` (cart) holding `BookingLine`s and
  `BookingAddon`s. Re-evaluate pricing/tax at line level.
- **Database changes.** `booking_groups`, `booking_addons`, `addons` catalog.
- **API changes.** Cart endpoints; line-level cancellation.
- **Risks.** Refund complexity.
- **Dependencies.** 1.2, 1.4.
- **AC.** A guest can book 2 villas + breakfast for 3 guests in one transaction.
- **Complexity.** **L**.

### 3.4 Booking Modification

- **Why needed.** Date changes are impossible today; only cancel + rebook.
- **Business value.** Lower friction; better conversion.
- **Technical design.** `POST /bookings/{id}/modify` with `ModificationQuote`
  (new dates/room, fee). Re-uses 1.3 state machine; audit-logged.
- **Database changes.** `booking_modifications` table.
- **API changes.** Modify + confirm endpoints.
- **Risks.** Money already captured must be partially refunded.
- **Dependencies.** 1.1, 1.3, 1.4.
- **AC.** A guest changes dates, sees a quote, confirms, and the payment
  diff is charged or refunded.
- **Complexity.** **L**.

### 3.5 Extra Services (Breakfast, Airport Transfer, Spa)

- **Why needed.** Real platforms upsell; today extras are impossible.
- **Business value.** Ancillary revenue (15-25 % of revenue in industry).
- **Technical design.** `ServiceOffering` entity (per-property). Bookable
  per night or per stay.
- **Database changes.** `service_offerings`, `booking_addons`.
- **API changes.** Quote includes addons.
- **Dependencies.** 3.3.
- **AC.** Add breakfast for 3 nights; invoice reflects it.
- **Complexity.** **M**.

### 3.6 Loyalty Program

- **Why needed.** Retention; differentiating from OTAs.
- **Business value.** Repeat bookings; lower CAC.
- **Technical design.** Points-based: 1 point per 10,000 VND; tiered
  (Silver, Gold, Platinum); earn on `COMPLETED` bookings; redeem as
  voucher. `LoyaltyAccount`, `PointsLedger`.
- **Database changes.** `loyalty_accounts`, `points_ledger`.
- **API changes.** `/me/loyalty`, admin tier configuration.
- **Dependencies.** 1.3, 2.6.
- **AC.** A completed stay awards points; user sees balance; redeemable
  at next booking.
- **Complexity.** **M**.

### 3.7 Housekeeping / Maintenance Schedule

- **Why needed.** OPERA / Cloudbeds parity.
- **Business value.** Ops excellence; reduces booking conflicts.
- **Technical design.** `RoomStatus` enum (CLEAN, DIRTY, INSPECTION,
  OUT_OF_SERVICE). Daily sheet for housekeeping staff.
- **Database changes.** `room_status_log` table.
- **API changes.** `/staff/housekeeping/today`, mark actions.
- **Dependencies.** 3.8 (staff roles).
- **AC.** A DIRTY room cannot be auto-confirmed until cleaned.
- **Complexity.** **M**.

### 3.8 Staff Roles: Receptionist / Manager

- **Why needed.** Receptionist exists in OPERA / Cloudbeds; absent here.
- **Business value.** Real on-property ops.
- **Technical design.** Add roles `RECEPTIONIST`, `MANAGER`. Permission
  matrix (`@PreAuthorize`) per endpoint. Staff dashboard.
- **Database changes.** `roles`, `user_roles` (move from string).
- **API changes.** `/staff/**` endpoints.
- **Risks.** Permission matrix bugs.
- **Dependencies.** 2.5.
- **AC.** A receptionist can check-in a guest; cannot delete rooms.
- **Complexity.** **M**.

### 3.9 Channel Manager (Booking.com, Airbnb, Agoda)

- **Why needed.** OTA distribution; this is how Cloudbeds grew.
- **Business value.** Inventory reach; commission-aware revenue.
- **Technical design.** Pluggable `ChannelAdapter` (XML or REST) with
  bidirectional sync: availability, rates, reservations, content. Queue
  retries with exponential backoff.
- **Database changes.** `channel_connections`, `channel_reservations`.
- **API changes.** Internal webhook ingest.
- **Risks.** Mapping errors; OTA API changes.
- **Dependencies.** 1.3, 1.5.
- **AC.** A Booking.com reservation appears as `Booking`; an internal
  booking updates availability on the channel within 60 s.
- **Complexity.** **XL**.

### 3.10 Group Booking

- **Why needed.** Events, retreats, weddings.
- **Business value.** Large AOV.
- **Technical design.** `GroupBooking` aggregate with multiple room
  holds, one coordinator, single invoice.
- **Database changes.** `group_bookings`.
- **API changes.** `/group-bookings/...`.
- **Dependencies.** 3.3.
- **AC.** A wedding of 30 guests across 10 villas books as one
  transaction.
- **Complexity.** **L**.

### Phase 3 Summary

| ID | Item | Complexity |
|---|---|---|
| 3.1 | Rate plans | L |
| 3.2 | Seasonal/weekend/holiday | M |
| 3.3 | Multi-room + add-ons | L |
| 3.4 | Booking modification | L |
| 3.5 | Extra services | M |
| 3.6 | Loyalty | M |
| 3.7 | Housekeeping | M |
| 3.8 | Staff roles | M |
| 3.9 | Channel manager | XL |
| 3.10 | Group booking | L |

---

## Phase 4 — Customer Experience

> Goal: parity with Airbnb / Agoda / Booking.com on the guest side.

### 4.1 Search Engine (Full-Text + Geo + Filters)

- **Why needed.** Current search is `LIKE` only.
- **Business value.** Discoverability; conversion.
- **Technical design.** Postgres `tsvector` index on room fields; OpenSearch
  for scale. Faceted filters: amenities, price range, guest count, rating,
  property type. Server-side pagination + sort.
- **Database changes.** `tsvector` columns + GIN indexes; or full
  OpenSearch cluster.
- **API changes.** `/villas/search?q&filters&sort&page`.
- **Risks.** Index drift; complex query DSL.
- **Dependencies.** 8.3.
- **AC.** Typo-tolerant search; sub-100 ms P95.
- **Complexity.** **L**.

### 4.2 Multi-Image Gallery + Video + 360° Tour

- **Why needed.** Today one `imageUrl` per villa.
- **Business value.** Higher conversion (Airbnb shows this is critical).
- **Technical design.** `RoomImage` entity with ordering, alt text, primary
  flag. `RoomVideo` (mp4 / hls). 360° via `<model-viewer>` or Marzipano.
- **Database changes.** `room_images`, `room_videos`, `room_tours`.
- **API changes.** Upload endpoints.
- **AC.** A room has ≥ 5 images; drag-reorder in admin.
- **Complexity.** **M**.

### 4.3 Map View + Nearby Attractions

- **Why needed.** Agoda / Booking.com show property on map.
- **Business value.** Higher confidence booking.
- **Technical design.** Geo point on Property; Mapbox / Leaflet; nearby
  POIs via Google Places or a curated table.
- **Database changes.** `properties.geo_lat`, `properties.geo_lng`,
  `attractions` table.
- **API changes.** `/properties/{id}/map`.
- **AC.** Map visible with villa pin and 10 nearby POIs.
- **Complexity.** **M**.

### 4.4 Compare Villas

- **Why needed.** Real OTA feature.
- **Business value.** Decision support.
- **Technical design.** `localStorage` cart of IDs; side-by-side modal.
- **AC.** Compare 2-4 villas side by side.
- **Complexity.** **S**.

### 4.5 Saved Payment Methods

- **Why needed.** Faster checkout; recurring guests.
- **Business value.** Conversion lift.
- **Technical design.** Stripe Setup Intents / VNPay tokenization;
  `PaymentMethod` entity scoped to user.
- **Database changes.** `payment_methods`.
- **API changes.** `/me/payment-methods`.
- **Dependencies.** 1.1.
- **AC.** Saved card used at next booking.
- **Complexity.** **M**.

### 4.6 Notification Centre (In-app + Email + Web Push)

- **Why needed.** Engagement today is one-shot emails.
- **Business value.** Higher LTV.
- **Technical design.** `Notification` entity; WebSocket (or SSE) channel
  for in-app bell; Web Push via VAPID keys.
- **Database changes.** `notifications`, `notification_preferences`.
- **API changes.** `/me/notifications`, `/me/notifications/stream` (SSE).
- **Dependencies.** 4.11.
- **AC.** Booking confirmed → bell badge increments.
- **Complexity.** **L**.

### 4.7 Real-time Availability Calendar UI

- **Why needed.** OPERA-style calendar.
- **Business value.** Better planning UX.
- **Technical design.** Month-view component; server returns per-day status.
- **API changes.** `/villas/{id}/availability?month=…`.
- **Complexity.** **M**.

### 4.8 2FA + Magic Link Login

- **Why needed.** Security + UX.
- **Business value.** Account safety.
- **Technical design.** TOTP (RFC 6238) + SMS OTP fallback. Magic-link
  email for passwordless.
- **Database changes.** `user_2fa`, `magic_link_tokens`.
- **AC.** Login requires TOTP after enable.
- **Complexity.** **M**.

### 4.9 Mobile App (PWA → Native)

- **Why needed.** 70 %+ of OTAs are mobile.
- **Business value.** Reach.
- **Technical design.** PWA first (manifest, service worker, offline
  shell). Later React Native / Capacitor.
- **AC.** Installable PWA; offline browse of recent bookings.
- **Complexity.** **L**.

### 4.10 Localization UI (vi/en; zh/ja later)

- **Why needed.** International guests.
- **Business value.** Reach.
- **Technical design.** react-i18next; keys extracted; ICU plural rules.
- **Database changes.** Add `translations` table for DB content.
- **AC.** Language switcher; all UI strings translated.
- **Complexity.** **M**.

### 4.11 Accessibility (WCAG 2.2 AA)

- **Why needed.** Inclusivity + legal in many jurisdictions.
- **Business value.** Compliance.
- **Technical design.** Axe-core CI checks; keyboard nav; screen reader
  audit; color contrast; focus management; prefers-reduced-motion.
- **AC.** Lighthouse a11y ≥ 95; axe-core zero serious.
- **Complexity.** **M**.

### 4.12 SEO + SSR

- **Why needed.** Organic acquisition is OPERA/Booking's main channel.
- **Business value.** Free traffic.
- **Technical design.** Migrate to **Next.js** or **Remix**; per-villa
  meta tags; JSON-LD (`Hotel`, `Product`, `BreadcrumbList`); OG images;
  `sitemap.xml`; `robots.txt`; hreflang.
- **Risks.** Major rewrite.
- **AC.** Lighthouse SEO ≥ 95; pages indexable.
- **Complexity.** **XL**.

### 4.13 Wishlist → "Notify on Price Drop / Availability"

- **Why needed.** Lost-lead recovery.
- **Business value.** Conversion.
- **Technical design.** `WishlistAlert` rules; cron checks; email/SMS push.
- **Database changes.** `wishlist_alerts`.
- **AC.** A wishlisted villa that opens gets an email.
- **Complexity.** **M**.

### 4.14 Customer Support (In-app Chat + Ticketing)

- **Why needed.** No support channel today.
- **Business value.** Retention.
- **Technical design.** `SupportTicket` entity, threaded; staff replies;
  AI assist (10.1).
- **Database changes.** `support_tickets`, `support_messages`.
- **API changes.** `/support/...`.
- **Complexity.** **L**.

### 4.15 Reviews 2.0

- **Why needed.** Verified badge; hotel reply; sub-scores.
- **Business value.** Trust.
- **Technical design.** `Review` extended with `cleanliness`, `comfort`,
  `location`, `service`, `value` sub-scores (1-10). `HotelResponse` text.
  Photos upload via 4.2. Moderation queue.
- **Database changes.** Extend `reviews`, add `review_photos`,
  `review_responses`.
- **AC.** Hotel can reply once; verified badge auto-set.
- **Complexity.** **M**.

### Phase 4 Summary

| ID | Item | Complexity |
|---|---|---|
| 4.1 | Search engine | L |
| 4.2 | Multi-image/video/360 | M |
| 4.3 | Map + nearby | M |
| 4.4 | Compare villas | S |
| 4.5 | Saved payment methods | M |
| 4.6 | Notification centre | L |
| 4.7 | Availability calendar UI | M |
| 4.8 | 2FA + magic link | M |
| 4.9 | Mobile (PWA → native) | L |
| 4.10 | Localization UI | M |
| 4.11 | Accessibility | M |
| 4.12 | SEO + SSR | XL |
| 4.13 | Wishlist alerts | M |
| 4.14 | Customer support | L |
| 4.15 | Reviews 2.0 | M |

---

## Phase 5 — Admin & Analytics

> Goal: PMS-grade back-office.

### 5.1 Admin Dashboard 2.0

- **Why needed.** Today a single page with tabs; no real analytics.
- **Business value.** Decision support.
- **Technical design.** Vue of CQRS read model (2.3). Charts via Recharts /
  Apache ECharts. Saved views, exports.
- **AC.** P95 dashboard load < 1 s.
- **Complexity.** **L**.

### 5.2 Revenue Management Console

- **Why needed.** Today total monthly revenue only.
- **Business value.** Yield optimization.
- **Technical design.** ADR, RevPAR, occupancy %, pickup pace,
  segmentation by channel/source.
- **Database changes.** Materialized views.
- **Complexity.** **L**.

### 5.3 Custom Reports Builder

- **Why needed.** Hard-coded Excel/PDF reports.
- **Business value.** Ad-hoc analysis.
- **Technical design.** Drag-drop field selection; SQL-safe filter
  expressions; scheduled delivery.
- **Database changes.** `report_definitions`, `report_runs`.
- **Complexity.** **XL**.

### 5.4 Forecasting & Demand Patterns

- **Why needed.** OPERA FDC parity.
- **Business value.** Pricing decisions.
- **Technical design.** Time-series forecasting (Prophet or simple moving
  average on seasonality); show on calendar.
- **Complexity.** **L**.

### 5.5 Owner / Channel Reports

- **Why needed.** Property owners want statements.
- **Business value.** B2B retention.
- **Technical design.** Per-property P&L; commissions; payout batch.
- **Database changes.** `owner_statements`.
- **Complexity.** **M**.

### 5.6 Email Template Editor

- **Why needed.** Today HTML hardcoded in Java.
- **Business value.** Marketing agility.
- **Technical design.** WYSIWYG editor with variables; preview with sample
  data; A/B testing.
- **Database changes.** `email_templates` (versioned).
- **Complexity.** **L**.

### 5.7 Staff Schedule & Shift Management

- **Why needed.** OPERA / Cloudbeds parity.
- **Business value.** Ops.
- **Technical design.** `Shift`, `ShiftAssignment` entities.
- **Complexity.** **M**.

### 5.8 System Settings UI

- **Why needed.** Cancellation window, hold TTL, tax rules, rate plans,
  branding — all need to be editable without redeploy.
- **Business value.** Ops autonomy.
- **Technical design.** Generic `Setting(key, value, scope)` table; admin UI.
- **Database changes.** `settings`.
- **Complexity.** **M**.

### 5.9 Content Management (CMS)

- **Why needed.** Blog is read-only; `/info/{slug}` returns a placeholder.
- **Business value.** Marketing.
- **Technical design.** Rich-text editor (TipTap / Lexical); media library;
  publish workflow; SEO metadata; multi-language.
- **Database changes.** `cms_pages`, `cms_revisions`, `media_assets`.
- **Complexity.** **L**.

### 5.10 Compliance Console (PCI, GDPR, Audit)

- **Why needed.** Required for enterprise deals.
- **Business value.** Compliance.
- **Technical design.** Search audit_log; export user data (GDPR); forget
  user (anonymize, keep booking amounts); PCI scope report.
- **Database changes.** Use existing `audit_log`.
- **Complexity.** **M**.

### Phase 5 Summary

| ID | Item | Complexity |
|---|---|---|
| 5.1 | Dashboard 2.0 | L |
| 5.2 | Revenue management | L |
| 5.3 | Custom report builder | XL |
| 5.4 | Forecasting | L |
| 5.5 | Owner reports | M |
| 5.6 | Email template editor | L |
| 5.7 | Staff schedule | M |
| 5.8 | Settings UI | M |
| 5.9 | CMS | L |
| 5.10 | Compliance console | M |

---

## Phase 6 — Security

> Goal: SOC2-ready controls.

### 6.1 OAuth2 / Social Login

- **Why needed.** Modern expectation.
- **Business value.** Conversion.
- **Technical design.** spring-security-oauth2-client with Google, Apple,
  Facebook providers.
- **Complexity.** **M**.

### 6.2 WebAuthn / Passkeys

- **Why needed.** Phishing-resistant.
- **Business value.** Security UX.
- **Technical design.** Server-side challenge; stored credential IDs.
- **Complexity.** **M**.

### 6.3 Field-Level Encryption (PII at Rest)

- **Why needed.** Email not encrypted.
- **Business value.** Compliance.
- **Technical design.** AES-GCM with KMS-managed DEKs; searchable
  encryption only if needed (otherwise deterministic hashes for lookup).
- **Database changes.** `email_enc` column (bytea); key rotation job.
- **Complexity.** **M**.

### 6.4 Secrets Manager Integration

- **Why needed.** Env vars on disk is risky.
- **Business value.** Ops safety.
- **Technical design.** Spring Cloud AWS Secrets Manager / HashiCorp Vault;
  on-the-fly resolution.
- **Complexity.** **M**.

### 6.5 Dependency / Image CVE Scanning in CI

- **Why needed.** Known vulnerabilities.
- **Business value.** Defense in depth.
- **Technical design.** Trivy, Snyk, OWASP Dependency-Check; nightly job.
- **Complexity.** **S**.

### 6.6 Pen-Test & Bug Bounty

- **Why needed.** Real-world assurance.
- **Business value.** Trust.
- **Technical design.** Annual pen-test; public bug bounty via HackerOne.
- **Complexity.** **L** (organisational).

### 6.7 WAF + Bot Protection

- **Why needed.** OWASP top 10.
- **Business value.** Reliability.
- **Technical design.** Cloudflare WAF or AWS WAF; bot scoring.
- **Complexity.** **M**.

### 6.8 Fine-Grained Authorization (Permify / OPA / Casbin)

- **Why needed.** Role-only is too coarse.
- **Business value.** B2B.
- **Technical design.** Policy-as-code; `@PreAuthorize` resolves to policy
  engine; ABAC + RBAC hybrid.
- **Complexity.** **L**.

### 6.9 Data Loss Prevention (DLP) in Logs

- **Why needed.** PII may leak to logs.
- **Business value.** Compliance.
- **Technical design.** Logback masking pattern (email, phone, card regex).
- **Complexity.** **S**.

### 6.10 GDPR Toolkit (Export + Forget + Consent)

- **Why needed.** EU/UK law.
- **Business value.** Compliance.
- **Technical design.** User-initiated export (JSON + PDF); forget job
  that anonymizes; consent banner; DPIA template.
- **Database changes.** `consents`.
- **Complexity.** **M**.

### Phase 6 Summary

| ID | Item | Complexity |
|---|---|---|
| 6.1 | OAuth2 / Social | M |
| 6.2 | WebAuthn / Passkeys | M |
| 6.3 | Field encryption | M |
| 6.4 | Secrets manager | M |
| 6.5 | CVE scanning | S |
| 6.6 | Pen-test + bounty | L |
| 6.7 | WAF + bot | M |
| 6.8 | Fine-grained authz | L |
| 6.9 | DLP in logs | S |
| 6.10 | GDPR toolkit | M |

---

## Phase 7 — Performance

> Goal: instant UX at 10× current load.

### 7.1 Edge Caching + CDN

- **Why needed.** Villa detail page should hit CDN.
- **Business value.** P95 latency; SEO.
- **Technical design.** Cloudflare / Fastly cache HTML for 60 s for
  anonymous users; stale-while-revalidate.
- **Complexity.** **M**.

### 7.2 Frontend Performance Budget

- **Why needed.** Bundle is 516 kB gzipped.
- **Business value.** Mobile conversion.
- **Technical design.** Route-level splitting; image lazy-load (already
  partial); prefetch on hover; bundle analyzer in CI; budget ≤ 200 kB
  initial.
- **Complexity.** **M**.

### 7.3 Image Optimization Pipeline

- **Why needed.** Large JPEGs in `/images/`.
- **Business value.** LCP / CLS.
- **Technical design.** On upload: AVIF/WebP variants, srcset, blurhash.
  CDN-side resizing.
- **Complexity.** **M**.

### 7.4 Read-Path Caching (Caffeine + Redis)

- **Why needed.** DB hit on every room list.
- **Business value.** Throughput.
- **Technical design.** `@Cacheable` on room list with 60 s TTL; tag-based
  invalidation on room update.
- **Dependencies.** 1.7.
- **Complexity.** **M**.

### 7.5 Async Writes (Email, Logs, Notifications)

- **Why needed.** SMTP blocks request threads.
- **Business value.** Latency.
- **Technical design.** `@Async` + executor pool; back-pressure.
- **Complexity.** **S**.

### 7.6 DB Performance Tuning

- **Why needed.** Verify indexes; vacuum.
- **Business value.** Stability.
- **Technical design.** `EXPLAIN ANALYZE` review of top 20 queries; add
  missing indexes; partition `bookings` by year.
- **Complexity.** **M**.

### 7.7 Connection Pool Tuning + Read Replicas

- **Why needed.** Read-heavy traffic.
- **Business value.** Throughput.
- **Technical design.** HikariCP per replica; read-only datasource; route
  reads via `@Transactional(readOnly=true)`.
- **Complexity.** **M**.

### 7.8 API Response Compression + Pagination + Filtering

- **Why needed.** Already on compression; pagination missing.
- **Business value.** Bandwidth + latency.
- **Technical design.** Spring Data `Pageable`; cursor-based pagination
  for chat / notifications.
- **Complexity.** **M**.

### 7.9 Frontend State Localisation

- **Why needed.** Avoid refetch storms.
- **Business value.** UX.
- **Technical design.** React Query + Zustand for global; SWR for forms.
- **Complexity.** **M**.

### 7.10 Load Test + Auto-Scale

- **Why needed.** Capacity planning.
- **Business value.** Reliability.
- **Technical design.** k6 scripts in CI nightly; HPA on K8s.
- **Complexity.** **M**.

### Phase 7 Summary

| ID | Item | Complexity |
|---|---|---|
| 7.1 | Edge CDN | M |
| 7.2 | FE perf budget | M |
| 7.3 | Image pipeline | M |
| 7.4 | Read caching | M |
| 7.5 | Async writes | S |
| 7.6 | DB tuning | M |
| 7.7 | Read replicas | M |
| 7.8 | Pagination + filters | M |
| 7.9 | FE state | M |
| 7.10 | Load test + auto-scale | M |

---

## Phase 8 — DevOps

> Goal: deploy 50× a day with confidence.

### 8.1 CI Pipeline (lint → test → build → scan → sign)

- **Why needed.** No CI today.
- **Business value.** Velocity + safety.
- **Technical design.** GitHub Actions; parallel jobs; OIDC for cloud auth.
- **Complexity.** **M**.

### 8.2 CD Pipeline (Canary + Auto-rollback)

- **Why needed.** Safe deploys.
- **Business value.** Reliability.
- **Technical design.** ArgoCD (K8s) or ECS rolling deploys with canary.
- **Complexity.** **L**.

### 8.3 Infrastructure as Code (Terraform / Pulumi)

- **Why needed.** Repeatable envs.
- **Business value.** Multi-region; dev parity.
- **Technical design.** Modules: VPC, RDS, Redis, S3, CloudFront, EKS.
- **Complexity.** **L**.

### 8.4 Observability Stack (Already in 2.7) — Full SLO Pipeline

- **Why needed.** SLO-based alerting.
- **Business value.** On-call.
- **Technical design.** Multi-window burn-rate alerts.
- **Complexity.** **M**.

### 8.5 Disaster Recovery + Backups

- **Why needed.** Today no backups.
- **Business value.** Continuity.
- **Technical design.** Daily PITR backups; cross-region copy; quarterly
  DR drill.
- **Complexity.** **M**.

### 8.6 Container Security (Distroless, Non-root)

- **Why needed.** Reduce surface.
- **Business value.** Security.
- **Technical design.** Distroless base images; non-root users; read-only
  FS.
- **Complexity.** **S**.

### 8.7 Multi-Region Deployment

- **Why needed.** Latency for global guests.
- **Business value.** Reach.
- **Technical design.** Active-active in 2 regions; Postgres logical
  replication; conflict resolution for bookings.
- **Complexity.** **XL**.

### 8.8 Chaos Testing

- **Why needed.** Resilience.
- **Business value.** Confidence.
- **Technical design.** Chaos Mesh / Litmus; game days.
- **Complexity.** **M**.

### 8.9 Status Page

- **Why needed.** Trust.
- **Business value.** Customer comms.
- **Technical design.** Self-hosted (Statping) or SaaS (Statuspage.io).
- **Complexity.** **S**.

### 8.10 Cost Optimization (FinOps)

- **Why needed.** Avoid runaway bills.
- **Business value.** Unit economics.
- **Technical design.** Tag everything; cost dashboards; rightsizing.
- **Complexity.** **M**.

### Phase 8 Summary

| ID | Item | Complexity |
|---|---|---|
| 8.1 | CI | M |
| 8.2 | CD canary | L |
| 8.3 | IaC | L |
| 8.4 | SLO pipeline | M |
| 8.5 | DR + backups | M |
| 8.6 | Container hardening | S |
| 8.7 | Multi-region | XL |
| 8.8 | Chaos | M |
| 8.9 | Status page | S |
| 8.10 | FinOps | M |

---

## Phase 9 — Testing

> Goal: a green pipeline you can trust.

### 9.1 Unit Test Coverage ≥ 80 %

- **Why needed.** Today 3 test classes, ~16 tests.
- **Business value.** Refactor safety.
- **Technical design.** JUnit 5 + Mockito; AssertJ; architecture tests
  (ArchUnit) for module boundaries.
- **Complexity.** **L** (ongoing).

### 9.2 Integration Tests (Testcontainers)

- **Why needed.** H2 lies about SQL Server behavior.
- **Business value.** Catch schema issues.
- **Technical design.** Testcontainers Postgres; per-test schema;
  WireMock for payment gateway.
- **Complexity.** **M**.

### 9.3 Contract Tests (Pact / Spring Cloud Contract)

- **Why needed.** Frontend ↔ backend drift.
- **Business value.** Safe API evolution.
- **Technical design.** Pact broker; CI gate.
- **Complexity.** **M**.

### 9.4 E2E Tests (Playwright)

- **Why needed.** Real user flows.
- **Business value.** Confidence.
- **Technical design.** Playwright + Percy for visual diff.
- **Complexity.** **L**.

### 9.5 Performance Tests (k6)

- **Why needed.** Capacity.
- **Business value.** Plan.
- **Technical design.** k6 scripts; nightly run; thresholds in CI.
- **Complexity.** **M**.

### 9.6 Security Tests in CI

- **Why needed.** Baseline.
- **Business value.** Compliance.
- **Technical design.** OWASP ZAP baseline scan; Snyk.
- **Complexity.** **M**.

### 9.7 Mutation Testing (PIT)

- **Why needed.** Test quality.
- **Business value.** Trust.
- **Technical design.** PIT runs on PRs; fail if mutation score < 60 %.
- **Complexity.** **M**.

### 9.8 Frontend Tests (Vitest + RTL)

- **Why needed.** No FE tests today.
- **Business value.** Refactor safety.
- **Complexity.** **L**.

### 9.9 Accessibility Tests (axe-core in CI)

- **Why needed.** Ship accessibility.
- **Business value.** Compliance.
- **Complexity.** **S**.

### 9.10 Synthetic Monitoring (Datadog / Checkly)

- **Why needed.** Detect prod issues early.
- **Business value.** Reliability.
- **Complexity.** **M**.

### Phase 9 Summary

| ID | Item | Complexity |
|---|---|---|
| 9.1 | Unit 80 % | L |
| 9.2 | Integration | M |
| 9.3 | Contract | M |
| 9.4 | E2E | L |
| 9.5 | Performance | M |
| 9.6 | Security | M |
| 9.7 | Mutation | M |
| 9.8 | FE tests | L |
| 9.9 | A11y | S |
| 9.10 | Synthetic | M |

---

## Phase 10 — AI Features

> Goal: a true AI concierge.

### 10.1 AI Concierge (beyond Gemini recommendation)

- **Why needed.** Today only room recommendation.
- **Business value.** Differentiation; service.
- **Technical design.** LLM-driven concierge with tool use:
  - search_villa, quote_price, create_booking, modify_booking,
    cancel_booking, send_message_to_human.
  - Memory per guest; trip planning; multi-turn.
- **Risks.** Hallucination → mitigate via tool grounding + fact-check
  guardrail.
- **Complexity.** **XL**.

### 10.2 AI Review Insights

- **Why needed.** Manual reading of reviews is impractical.
- **Business value.** Hotel ops.
- **Technical design.** LLM summarization + theme extraction; flagged
  complaints.
- **Complexity.** **M**.

### 10.3 AI Demand Forecasting

- **Why needed.** Smarter than Prophet on small data.
- **Business value.** Yield.
- **Technical design.** LLM + classical time-series ensemble.
- **Complexity.** **L**.

### 10.4 AI Pricing Recommendations

- **Why needed.** Manual rate setting is slow.
- **Business value.** Margin.
- **Technical design.** Recommendation engine; human approves.
- **Complexity.** **L**.

### 10.5 AI Customer Support Triage

- **Why needed.** Volume.
- **Business value.** Cost to serve.
- **Technical design.** Classify → route to staff with summary.
- **Complexity.** **M**.

### 10.6 AI Translation of Reviews

- **Why needed.** Cross-language.
- **Business value.** Reach.
- **Complexity.** **M**.

### 10.7 AI Fraud Detection

- **Why needed.** Chargebacks.
- **Business value.** Loss prevention.
- **Technical design.** Behavioral features; anomaly scoring; Stripe Radar
  integration.
- **Complexity.** **L**.

### 10.8 AI-Powered Search (Semantic + Hybrid)

- **Why needed.** Lexical search misses intent.
- **Business value.** Conversion.
- **Technical design.** Embeddings + OpenSearch hybrid query.
- **Complexity.** **L**.

### 10.9 Personalised Recommendations

- **Why needed.** Cross-sell.
- **Business value.** AOV.
- **Technical design.** Collaborative filtering on completed bookings.
- **Complexity.** **L**.

### 10.10 AI Image Generation (Marketing)

- **Why needed.** Listings look better.
- **Business value.** Conversion.
- **Complexity.** **M**.

### Phase 10 Summary

| ID | Item | Complexity |
|---|---|---|
| 10.1 | Concierge | XL |
| 10.2 | Review insights | M |
| 10.3 | Demand forecasting | L |
| 10.4 | Pricing recommendations | L |
| 10.5 | Support triage | M |
| 10.6 | Translation | M |
| 10.7 | Fraud detection | L |
| 10.8 | Semantic search | L |
| 10.9 | Recommendations | L |
| 10.10 | Image generation | M |

---

## 11. Reference Mapping to Industry Parity

| Capability | Booking.com | Airbnb | Agoda | OPERA PMS | Cloudbeds | This plan |
|---|---|---|---|---|---|---|
| Multi-property | ✅ | ✅ | ✅ | ✅ | ✅ | 2.1 |
| Payment gateway | ✅ | ✅ | ✅ | ✅ | ✅ | 1.1 |
| Tax / invoice | ✅ | ✅ | ✅ | ✅ | ✅ | 1.2 |
| Booking FSM | ✅ | ✅ | ✅ | ✅ | ✅ | 1.3 |
| Cancellation policies | ✅ | ✅ | ✅ | ✅ | ✅ | 1.4 |
| Rate plans | ✅ | ✅ | ✅ | ✅ | ✅ | 3.1 |
| Seasonal pricing | ✅ | ✅ | ✅ | ✅ | ✅ | 3.2 |
| Multi-room booking | ✅ | ❌ | ✅ | ✅ | ✅ | 3.3 |
| Booking modification | ✅ | ✅ | ✅ | ✅ | ✅ | 3.4 |
| Channel manager | ✅ | ❌ | ✅ | ✅ | ✅ | 3.9 |
| Group booking | ✅ | ✅ | ✅ | ✅ | ✅ | 3.10 |
| Reviews 2.0 | ✅ | ✅ | ✅ | ✅ | ✅ | 4.15 |
| Map + nearby | ✅ | ✅ | ✅ | ✅ | ✅ | 4.3 |
| Saved payment methods | ✅ | ✅ | ✅ | ✅ | ✅ | 4.5 |
| Loyalty | ✅ Genius | ❌ | ✅ | ✅ | ✅ | 3.6 |
| Housekeeping | partial | ❌ | partial | ✅ | ✅ | 3.7 |
| Staff roles | ✅ | ✅ | ✅ | ✅ | ✅ | 3.8 |
| Custom reports | ✅ | ✅ | ✅ | ✅ | ✅ | 5.3 |
| Forecasting | ✅ | ✅ | ✅ | ✅ | ✅ | 5.4 |
| Owner statements | ✅ | ✅ host | ✅ | ✅ | ✅ | 5.5 |
| CMS | ✅ | ✅ | ✅ | ✅ | ✅ | 5.9 |
| Compliance tools | ✅ | ✅ | ✅ | ✅ | ✅ | 6.10 |
| AI concierge | partial | ✅ | partial | ❌ | partial | 10.1 |

---

## 12. Roadmap Summary

| Phase | Theme | Headline items | Aggregated effort |
|---|---|---|---|
| 1 | Critical Production | Payments, taxes, FSM, locks, idempotency, currency, migrations, comms | ~ 4 months, 4 engineers |
| 2 | Enterprise Architecture | Multi-tenancy, modulith, CQRS, OpenAPI, audit, observability, jobs | ~ 4 months |
| 3 | Business Logic | Rate plans, seasonals, multi-room, modification, addons, loyalty, housekeeping, staff, channel manager, group | ~ 6 months |
| 4 | Customer Experience | Search, gallery, map, compare, saved cards, notifications, calendar, 2FA, mobile, i18n, a11y, SEO, support, reviews 2.0 | ~ 6 months |
| 5 | Admin & Analytics | Dashboard 2.0, revenue, custom reports, forecasting, owner statements, email editor, schedule, settings, CMS, compliance | ~ 5 months |
| 6 | Security | OAuth2, WebAuthn, encryption, secrets, CVE, pen-test, WAF, fine-grained authz, DLP, GDPR | ~ 3 months |
| 7 | Performance | CDN, FE budget, image pipeline, caches, async, DB tuning, replicas, pagination, FE state, load tests | ~ 3 months |
| 8 | DevOps | CI, CD canary, IaC, SLO, DR, container hardening, multi-region, chaos, status page, FinOps | ~ 4 months |
| 9 | Testing | Unit 80 %, integration, contract, E2E, perf, security, mutation, FE, a11y, synthetic | continuous |
| 10 | AI | Concierge, insights, forecasting, pricing, triage, translation, fraud, semantic search, recommendations, image gen | ~ 6 months |

**Total elapsed time** with parallelisable streams: **~18 months** to full
parity with Booking.com / OPERA / Cloudbeds in a multi-tenant, multi-region,
AI-augmented, payment-enabled, audit-compliant form.

---

## 13. Guiding Principles for Execution

1. **Ship Phase 1 before anything else.** No Phase 2-10 work is meaningful
   until payments, FSM, and migrations are correct.
2. **Idempotency everywhere.** Every mutating endpoint.
3. **Events, not calls, between contexts.** Booking → Payment → Email is an
   event flow, not a synchronous call chain.
4. **Read models from day one.** Do not let reporting bring down OLTP.
5. **Security is a baseline, not a phase.** Items from Phase 6 are merged
   into each phase's PRs.
6. **Testability is a design constraint.** New components are constructor-
   injected, stateless where possible, and have unit tests committed with
   the feature.
7. **Observability before optimization.** Measure first.
8. **Mobile parity or close.** Every guest-facing feature has a mobile view.
9. **Money code is reviewed twice.** Any PR touching `Booking`, `Payment`,
   `Refund` requires a second reviewer.
10. **Document public APIs.** OpenAPI is the contract.

---

*End of blueprint. Ready for engineering execution.*
