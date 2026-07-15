# Villa Booking System – Enterprise Business & Gap Analysis Report

> Analysis only. No code changed. Every claim is anchored to a file or observation.
> Produced after a complete read-only inventory of both `frontend/` and `hotel/`.

---

## 1. Project Snapshot

| Aspect | Reality |
|---|---|
| Product | Single-property villa booking ("Như Hotel"). |
| Stack | Spring Boot 3.4.1 / Java 21 backend, React + Vite + TypeScript frontend, SQL Server (Postgres optional), BCrypt session auth, Google Gemini for AI. |
| Domain entities | User, Room, RoomTypeEntity, Amenity, Booking, Review, WishlistItem, Voucher, Blog, ChatMessage (+ join `room_amenities`). |
| Auth model | HttpSession + Spring Security role bridge. Roles: `USER`, `ADMIN`. No staff/receptionist role. |
| Payments | **None.** No gateway, no `Payment` entity, no `PaymentMethod`. |
| Deployment | Dockerised (backend + frontend + Postgres compose), Nginx serves SPA, `/api` proxied to backend. |

---

## 2. Feature Inventory

Status legend: ✔ Complete · ⚠ Partial · ❌ Missing · 🧪 Placeholder · 🗑 Dead Code · 🔄 Duplicate.

### 2.1 Authentication & Account

| Feature | Status | Evidence / Comment |
|---|---|---|
| Register with email + password | ✔ | `AuthApi.register`, `UserService.registerUser` |
| Email verification (link) | ✔ | `AuthApi.verify?token=`, token expires 24 h |
| Resend verification | ✔ | `AuthApi.resendVerification` |
| Forgot password / reset link | ✔ | `ProfileService.initiatePasswordReset` + `AuthApi.resetPassword` |
| Login (session cookie) | ✔ | `JSESSIONID`, `HttpOnly=true`, `SameSite=lax` |
| Logout | ✔ | Invalidates session |
| Auto-login after register | ✔ | But UX question: account works before verification |
| Session refresh on profile pages | ⚠ | Single `/auth/session` endpoint; no token rotation |
| 2FA / MFA | ❌ | Not present anywhere |
| OAuth / social login | ❌ | — |
| "Remember me" | ❌ | — |
| Account lockout after N failed attempts | ❌ | `BadCredentialsException` → 401, no counter |
| Password complexity rules | ⚠ | Only `MIN_PASSWORD_LENGTH=6`, no complexity regex |
| Login throttling / rate limit | ❌ | No Bucket4j, no gateway limiter |

### 2.2 Profile

| Feature | Status | Evidence |
|---|---|---|
| View profile | ✔ | `ProfileApi.get` |
| Edit name + email | ✔ | `ProfileApi.update` (multipart) |
| Email change triggers re-verification | ✔ | `ProfileService.updateProfile` |
| Change password (with current) | ✔ | `ProfileApi.changePassword`, `PasswordDTO` |
| Avatar upload | ✔ | `FileStorageService`, 5 MB cap, whitelist |
| Email enumeration prevention on forgot-password | ✔ | Always 200 + generic message |
| Account self-deletion (GDPR "right to be forgotten") | ❌ | Only admin can delete |
| Data export | ❌ | No `/users/me/export` |
| Two-factor backup codes | ❌ | — |
| Locale preference | ⚠ | Backend supports `?lang=` but no UI selector |

### 2.3 Villa Browsing

| Feature | Status | Evidence |
|---|---|---|
| List villas | ✔ | `GET /api/v1/rooms` |
| Detail page | ✔ | `VillaDetailPage`, `GET /api/v1/rooms/{id}` |
| Search (date range + price range + type) | ✔ | `RoomRepository.findAvailableRooms` |
| Booked-dates lookup | ✔ | `GET /api/v1/rooms/{id}/booked-dates` |
| Reviews + rating on detail | ✔ | `ReviewApi.forRoom` |
| Multi-image gallery | ⚠ | Single `imageUrl` only — no `RoomImage` table |
| Video / 360° tour | ❌ | — |
| Location / map | ❌ | — |
| Nearby attractions | ❌ | — |
| Compare villas | ❌ | — |
| "Similar villas" | ❌ | — |
| Map view (geo) | ❌ | — |

### 2.4 Booking

| Feature | Status | Evidence |
|---|---|---|
| Create booking (CONFIRMED) | ✔ | `BookingApi.create`, `BookingService.createBooking` |
| Same-day check-in / day-use pricing (50%) | ✔ | `DAY_USE_PRICE_FACTOR = 0.5` |
| Voucher / promo code | ✔ | `VoucherService`, public `validateVoucher` |
| Cancel booking (refund tier) | ✔ | 100%/50%/0% tiers by days-to-check-in |
| Booking history (mine) | ✔ | `BookingApi.myBookings` |
| Booking detail | ✔ | `BookingApi.get` |
| Admin list bookings | ✔ | `AdminApi.bookings` |
| Admin change status | ⚠ | Generic setter `updateBookingStatus`, no transition rules |
| Booking modification (change dates / room) | ❌ | Only cancel + rebook |
| Booking hold / lock (TTL) | ❌ | — |
| Partial payment / deposit | ❌ | — |
| Multi-room booking | ❌ | One `Booking` = one `Room` |
| Extra guests surcharge | ❌ | `req.guests` is declared but ignored |
| Group booking | ❌ | — |
| Recurring / long-stay discount | ❌ | — |
| Check-in time-of-day rules | ❌ | Only dates stored |
| Check-out time-of-day rules | ❌ | — |
| Late checkout / early check-in (extra fee) | ❌ | — |
| No-show tracking (auto-cancel) | 🗑 | `BookingScheduler` stubbed |
| Invoice / receipt PDF | ❌ | Only admin reports |
| Auto-generated booking reference | ⚠ | Only `NV-{id}` — no real ref |
| Email confirmation to guest | ⚠ | Admin gets email, guest only gets approval/rejection |
| Email cancellation notice | ❌ | `sendBookingCancelledEmail` exists but is never called |
| T-1 day reminder email | ❌ | `sendPaymentReminders` is a no-op |

### 2.5 Pricing & Revenue Management

| Feature | Status | Evidence |
|---|---|---|
| Per-villa base price | ✔ | `Room.pricePerNight` |
| Day-use pricing | ✔ | 50% of one night |
| Voucher (fixed amount or %) | ✔ | `Voucher.percent` flag |
| Tax / VAT / service charge | ❌ | Not modelled |
| City tax | ❌ | — |
| Weekend pricing | ❌ | — |
| Holiday pricing | ❌ | — |
| Seasonal pricing | ❌ | — |
| Length-of-stay discounts | ❌ | — |
| Early-bird / last-minute | ❌ | — |
| Rate plans (BAR, refundable, non-refund) | ❌ | — |
| Per-guest surcharge | ❌ | `guests` field ignored |
| Currency | ⚠ | Hard-coded VND, no `currency` field, scale 0 (no minor units) |
| Multi-currency | ❌ | — |

### 2.6 Payments & Refunds

| Feature | Status | Evidence |
|---|---|---|
| Stripe / VNPay / MoMo / ZaloPay / PayOS | ❌ | Zero hits |
| `Payment` entity | ❌ | — |
| `PaymentMethod` (card, transfer, e-wallet) | ❌ | — |
| Refund workflow | ❌ | Refund % is computed and stored but no money moves |
| Partial refund | ❌ | — |
| Deposit / second installment | ❌ | — |
| Bank transfer reconciliation | ❌ | — |
| Invoice numbering / tax invoice | ❌ | — |
| Payment webhook endpoint | ❌ | — |
| PCI compliance surface | N/A | (no card data) |

### 2.7 Reviews & Ratings

| Feature | Status | Evidence |
|---|---|---|
| Public room reviews | ✔ | `ReviewApi.forRoom` |
| Average rating | ✔ | `ReviewRepository.getAverageRatingByRoom` |
| Submit review after stay | ✔ | `ReviewService.createReview` |
| Edit / delete own review | ✔ | — |
| Admin delete any review | ✔ | — |
| Verified-stay badge | ❌ | — |
| Photo upload with review | ❌ | — |
| Hotel reply to review | ❌ | — |
| Review moderation queue | ❌ | — |
| Report a review (abuse) | ❌ | — |
| Time-window restriction (e.g. 30 days after stay) | ❌ | Can review a 5-year-old stay |

### 2.8 Wishlist / Favorites

| Feature | Status | Evidence |
|---|---|---|
| Add / remove from wishlist | ✔ | `WishlistService.toggleWishlist` |
| List my wishlist | ✔ | `WishlistApi.list` |
| Price-drop notify on wishlist item | ❌ | — |
| "Notify me when available" | ❌ | — |

### 2.9 Customer Support & Communication

| Feature | Status | Evidence |
|---|---|---|
| Contact form (sends email) | ✔ | `ContactApi.submit` |
| Live chat (WebSocket) | ❌ | — |
| AI room recommender (Gemini) | ✔ | `GeminiService`, `AiApi.recommend` |
| AI chat history | ✔ | `ChatMessageRepository`, persisted per user |
| AI chat clear | ✔ | `AiApi.clear` |
| In-app notification centre | ❌ | — |
| SMS notifications | ❌ | — |
| Push notifications | ❌ | — |
| Email open / click tracking | ❌ | — |
| Help-desk ticketing | ❌ | — |
| Multi-language support (UI) | ❌ | Backend bundles exist, no UI selector |

### 2.10 Admin / Back-office

| Feature | Status | Evidence |
|---|---|---|
| Dashboard (counts, revenue, top-5) | ✔ | `AdminApi.dashboard` |
| Booking list + filter by status | ✔ | `AdminApi.bookings` |
| Update booking status (generic) | ✔ | — |
| Approve / reject booking (PENDING→AWAITING_PAYMENT/REJECTED) | ⚠ | `approveBooking` / `rejectBooking` exist but no controller endpoint exposes them |
| Confirm payment | ⚠ | `confirmPayment` exists, no endpoint |
| Room CRUD | ⚠ | API present (`POST /api/v1/admin/rooms`) but **no UI form** — admin dashboard only deletes |
| Room type CRUD | ⚠ | API present, **no UI** |
| Amenity CRUD | ⚠ | API present, **no UI** |
| Voucher CRUD | ⚠ | API present, **no UI form** — only list + delete? |
| User list + role change | ⚠ | API present, **no UI** |
| User delete | ⚠ | API present, **no UI** |
| Reports: most-booked / top-rated | ✔ | `AdminApi.reports` |
| Excel / PDF export | ✔ | `ReportApi.excel/pdf` |
| Staff management (receptionist / manager roles) | ❌ | Only `ADMIN` |
| Audit log (who changed what, when) | ❌ | `createdAt` / `approvedBy` / `approvedAt` exist on Booking only |
| Revenue by room / channel | ❌ | Only total monthly revenue |
| Occupancy % / ADR / RevPAR | ❌ | — |
| Cancellation rate / source attribution | ❌ | — |
| Email templates editor | ❌ | HTML hardcoded in `EmailService` |
| Scheduled email report | ❌ | — |
| Settings (cancellation policy %, payment deadline hours) | ⚠ | Only via `application.properties` |
| Channel manager sync (Booking.com, Airbnb) | ❌ | — |
| iCal export (`.ics`) | ❌ | — |

### 2.11 System / Platform Features

| Feature | Status | Evidence |
|---|---|---|
| Database | ✔ | SQL Server / Postgres / H2 |
| Connection pool | ✔ | HikariCP |
| File storage | ✔ | `FileStorageService`, `uploads/` |
| File download | ✔ | `GET /uploads/{filename}` |
| Caching | ❌ | No Redis / Caffeine / `@Cacheable` |
| Email service | ✔ | `JavaMailSender`, graceful no-op |
| Background jobs | ❌ | Only dead-code scheduler |
| Queue | ❌ | — |
| Search (full-text) | ❌ | SQL `LIKE` only |
| Pagination | ❌ | All list endpoints return full lists |
| Filtering | ⚠ | Only rooms support date/price/type filters |
| Sorting | ❌ | Hard-coded by createdAt desc |
| Localisation | ⚠ | `messages_vi.properties` has mojibake, no UI switcher |
| Dark mode | ❌ | Single light theme |
| Accessibility | ⚠ | Some labels/role attributes, no audit |
| SEO (SSR, meta, sitemap, OG) | ❌ | Pure client-side SPA, no prerender |
| PWA / offline | ❌ | — |
| Structured data (JSON-LD) | ❌ | — |
| Analytics (GA, Hotjar) | ❌ | — |
| Error tracking (Sentry) | ❌ | — |
| Performance monitoring (Prometheus) | ❌ | — |
| Backup / restore | ❌ | DB only, manual |
| Health check | ✔ | `/actuator/health` |
| Structured logging | ✔ | logback-spring.xml |
| Request IDs | ✔ | `RequestIdFilter`, `X-Request-Id` |

---

## 3. Business Flow Analysis

### 3.1 Guest Journey (the one that actually works end-to-end)

```
Landing (/)
   ↓ Browse villas (/villas)
   ↓ Search with dates + price filter
   ↓ Villa detail (/villas/:id)
   ↓ Click "Book now" → /booking?roomId=&checkIn=&checkOut=
   ↓ Fill guest info + optional voucher
   ↓ POST /api/v1/bookings → status = CONFIRMED, total price stored, voucher consumed
   ↓ Booking created (no guest confirmation email)
   ↓ Navigate to My Bookings
```

⚠ **Notes on the happy path**:
- After booking, **no confirmation email is sent to the guest** (only admin is notified of a new booking).
- The guest can later cancel; the cancel email is **never sent** (`sendBookingCancelledEmail` defined but unused).
- No payment step at all — booking is "CONFIRMED" without money moving.

### 3.2 Admin Booking Approval Flow (declared but not exposed)

```
PENDING booking (would be created)
   ↓ Admin clicks "Approve" → POST /api/v1/admin/bookings/{id}/approve (MISSING)
   ↓ status → AWAITING_PAYMENT, approvedBy, approvedAt, paymentDeadline
   ↓ Email "approved" sent to guest
   ↓ Guest pays externally
   ↓ Admin clicks "Confirm payment" → POST /api/v1/admin/bookings/{id}/confirm-payment (MISSING)
   ↓ status → CONFIRMED, paidAt set, admin notified
```

**Reality:** Both `approveBooking` and `confirmPayment` exist in `BookingService` but **no HTTP endpoint maps to them**. The admin can only use `PUT /api/v1/admin/bookings/{id}/status` which is a raw setter with no transition rules and no emails. This flow is functionally dead.

### 3.3 Review Flow

```
Booking CONFIRMED or COMPLETED
   ↓ /profile/bookings
   ↓ "Write review" → ReviewPage
   ↓ POST /api/v1/reviews/booking/{bookingId}
   ↓ Public on /villas/:id reviews
```

⚠ `COMPLETED` is **never set by any code path**, so in practice only `CONFIRMED` bookings are reviewable.

### 3.4 Wishlist Flow

```
Browse → click heart icon → POST /api/v1/wishlist/toggle
   ↓ /profile/wishlist → GET /api/v1/wishlist
```

### 3.5 Auth Flows

```
Register → email verification link → /auth/verify?token=
   ↓ (login allowed regardless of verification)
Login → session cookie → /auth/session
Forgot password → email reset link → /auth/reset-password (token + new password)
```

---

## 4. Role Analysis

| Role | Permissions | Missing |
|---|---|---|
| Guest (unauthenticated) | Browse villas, view reviews, validate voucher, contact form, register/login | — |
| Customer (USER) | All guest + create/cancel booking, wishlist, reviews, profile, AI chat, logout | Account self-delete, data export, saved payment methods, notification preferences |
| Receptionist | ❌ Does not exist as a role | All receptionist features (check-in/out, in-person payments, housekeeping) |
| Manager | ❌ Does not exist as a role | All property-management features (staff schedules, occupancy, manual price overrides) |
| Admin | All `/api/v1/admin/**`, manage users, rooms, vouchers, room types, amenities, view reports | Audit log, granular permissions, password reset on behalf of user, impersonation |

**Major missing permission surfaces:**
- Receptionist cannot mark a guest as "checked in" because no such status exists (`CHECKED_IN`/`IN_HOUSE`).
- No "owner" permission for refund approval (refund is just a stored number).
- Admin can hard-delete users but cannot anonymise (GDPR right-to-be-forgotten at admin level).

---

## 5. Booking Capabilities Gap Matrix

| Capability | Status | Comment |
|---|---|---|
| Availability calendar (per villa) | ⚠ | Only `booked-dates` list, no admin-facing calendar UI |
| Seasonal pricing | ❌ | — |
| Weekend pricing | ❌ | — |
| Holiday pricing | ❌ | — |
| Promo codes | ✔ | `Voucher` entity |
| Booking lock (TTL hold during checkout) | ❌ | Race window between "select dates" and "confirm" |
| Cancellation policy | ⚠ | Only refund % tiers, no policy doc per villa |
| Refund workflow | 🗑 | Computed, stored, no money movement |
| Partial payment | ❌ | — |
| Deposit | ❌ | — |
| Booking expiration | 🗑 | `BookingScheduler` stubbed |
| Double-booking prevention | ⚠ | Read-then-write race present (no row lock, no `@Version`) |
| Capacity validation | ⚠ | Single-room capacity never enforced (`Room.maxGuests` doesn't exist) |
| Check-in / check-out rules | ❌ | No time-of-day |
| Late check-out fee | ❌ | — |
| Early check-in fee | ❌ | — |
| Booking modification | ❌ | Cancel + rebook only |
| Booking history | ⚠ | Status changes not stored as a log (`BookingHistory` missing) |
| Booking status set | ⚠ | 7 enum values declared, 1 declared but unused (`AWAITING_APPROVAL`), 1 no longer used (`AWAITING_PAYMENT`) |

---

## 6. Database / Schema Gaps

| Missing entity / field | Reason |
|---|---|
| `Payment` | Payment gateway integration absent |
| `PaymentMethod` | "Saved cards" missing |
| `RoomImage` (multi-image per room) | Only `Room.imageUrl` |
| `Hotel`/`Property` | No multi-tenancy |
| `Address` | No street/city/country for property or guest |
| `Room.maxGuests`, `Room.bedCount`, `Room.bathrooms` | Capacity / amenities not enforced |
| `BookingLineItem` | Multi-room / add-ons impossible |
| `BookingGuest[]` | Currently `guestName`/`guestPhone` are denormalised strings |
| `ExtraService` / `AddOn` | Breakfast, transfer, etc. not bookable |
| `SeasonalRate`, `RatePlan` | Dynamic pricing absent |
| `LoyaltyAccount`, `PointsLedger` | Loyalty absent |
| `Notification`, `EmailLog` | No in-app inbox |
| `AuditLog` | No who/when/what audit trail |
| `PromoCodeUsage` | Vouchers lack per-user tracking |
| `ChatbotSession` | `ChatMessage` has no session boundary |

| Missing schema constraint | Reason |
|---|---|
| `@Version` (optimistic lock) on `Booking`, `Voucher` | Race conditions |
| `LockModeType.PESSIMISTIC_WRITE` on conflict read | Race condition |
| `currency` field on `Booking`, `Room` | Multi-currency impossible |
| `softDelete` (`deletedAt`) on every entity | No undo / GDPR safe-delete |

---

## 7. Security Gaps

| Concern | Current state | Gap |
|---|---|---|
| Password hashing | BCrypt cost 10 | OK |
| Login throttling | None | **P0** |
| Account lockout | None | **P0** |
| 2FA | None | **P2** |
| CAPTCHA on register/login | None | **P1** |
| Session fixation | Mitigated by Spring (new ID on auth) | OK |
| CSRF | Disabled globally | Acceptable for token-based / SPA, but no CSRF token used |
| CORS | Whitelist via env | OK |
| Rate limiting | None | **P0** |
| Security headers (CSP, HSTS, X-Frame-Options) | Only `X-Content-Type-Options` on `/uploads/` | **P1** |
| PII at rest | Plain text email, hashed password | Email not encrypted |
| PII in logs | Logs may include request body | Need redaction |
| GDPR data export | None | **P1** |
| GDPR right-to-be-forgotten (self-service) | Admin-only hard delete | **P1** |
| Audit logging | None | **P1** |
| File upload AV scan | None | **P2** |
| OWASP top-10 review | Not done | **P1** |
| Dependency CVE scan | Not done | **P1** |

---

## 8. Production Readiness Gaps

| Area | Status | Gap |
|---|---|---|
| CI/CD | ❌ | No pipeline |
| Migrations (Flyway/Liquibase) | ❌ | `ddl-auto=update` + two hand-rolled `.sql` scripts |
| Backups | ❌ | Manual |
| Restore / DR runbook | ❌ | — |
| Multi-environment config | ⚠ | Only `dev` / `postgres` / `test` profiles |
| Secret manager | ❌ | Env vars only |
| Distributed cache | ❌ | Single-instance in-memory session |
| Distributed session | ❌ | No Spring Session JDBC/Redis |
| Horizontal scaling | ❌ | Session state held in memory |
| Monitoring (metrics, traces) | ⚠ | Actuator health only, no Micrometer/Prometheus |
| Error tracking | ❌ | No Sentry/equivalent |
| Load testing | ❌ | — |
| Penetration test | ❌ | — |
| SLA / SLO definition | ❌ | — |
| Runbook | ⚠ | Only README, no incident playbook |
| Status page | ❌ | — |

---

## 9. UX / Frontend Gaps

| Concern | Status | Comment |
|---|---|---|
| Multi-step checkout | ❌ | Single page (`BookingPage`); no review/summary step |
| Booking summary PDF download | ❌ | — |
| Real-time availability feedback | ❌ | — |
| Search filters UI (price slider, type, guests, amenities) | ⚠ | Backend supports price/type; UI only shows basic |
| Date picker on detail page | ⚠ | URL params only |
| Calendar visualisation | ❌ | No month-view UI |
| Saved payment methods UI | ❌ | — |
| Notification centre UI | ❌ | — |
| Account settings → notification preferences | ❌ | — |
| Locale switcher in navbar | ❌ | Backend supports `?lang=`, no UI |
| Dark mode toggle | ❌ | — |
| Accessibility audit | ❌ | — |
| Loading skeletons everywhere | ⚠ | Some use `<PageFallback>` only |
| Empty-state illustrations | ⚠ | Minimal |
| Onboarding tour | ❌ | — |
| Mobile gestures (swipe gallery) | ❌ | — |
| Push notifications (PWA) | ❌ | — |
| SEO meta tags per page | ❌ | — |
| Open Graph / Twitter card images | ❌ | — |
| Sitemap.xml | ❌ | — |
| robots.txt | ❌ | — |

---

## 10. Customer-Support / Comms Gaps

| Concern | Status |
|---|---|
| In-app inbox | ❌ |
| WebSocket chat | ❌ |
| Help articles tied to pages | ❌ |
| "Need help?" floating button | ❌ (only the AI widget exists) |
| Multi-channel ticket creation | ❌ |
| SLA-aware ticket routing | ❌ |
| Email → ticket conversion | ❌ |

---

## 11. Pricing / Revenue Gaps

| Concern | Status |
|---|---|
| Revenue management dashboard | ❌ (only monthly revenue) |
| ADR / RevPAR metrics | ❌ |
| Pickup-vs-budget report | ❌ |
| Length-of-stay restriction | ❌ |
| Closed-to-arrival / closed-to-departure | ❌ |
| Min-stay-through rule | ❌ |
| Overbooking strategy | ❌ |
| Channel manager (Booking.com, Expedia, Airbnb) | ❌ |
| iCal export for OTA sync | ❌ |
| Currency conversion / display | ❌ |
| Dynamic pricing engine | ❌ |

---

## 12. Prioritised Gap Backlog

### P0 — Critical for production launch

| # | Gap | Why critical |
|---|---|---|
| P0-01 | **Payment gateway integration** (Stripe + VNPay/MoMo) | The "booking" doesn't actually take money. No business is possible. |
| P0-02 | **Payment entity + `Booking.PAYMENT_REQUIRED`/`PARTIALLY_PAID`/`PAID` states** | Without this, refund can't move money. |
| P0-03 | **Tax / VAT / service charge engine** | Required for invoicing and legal compliance. |
| P0-04 | **Optimistic locking on Booking + pessimistic lock on conflict read** | Today, two concurrent requests can double-book. |
| P0-05 | **Transactional voucher consume + booking save** | Currently voucher can be consumed even if the booking save fails. |
| P0-06 | **Login throttling / account lockout / rate limit** | Brute force is trivial today. |
| P0-07 | **Flyway / Liquibase migrations** | Today schema changes are untraceable; live updates of `Bookings.status` constraint exist as ad-hoc `.sql` files. |
| P0-08 | **Email on cancellation, on booking confirmation to guest, on T-1 day reminder** | Guests currently receive almost no communications. |
| P0-09 | **Approve/reject/confirm-payment admin UI + endpoint** | Service methods exist but are unreachable via HTTP. The whole approval pipeline is broken. |
| P0-10 | **Currency field + minor units** | `BigDecimal(precision=12, scale=0)` cannot represent USD/EUR cents. |

### P1 — High priority (post-launch)

| # | Gap |
|---|---|
| P1-01 | Multi-image gallery (`RoomImage` entity + upload UI) |
| P1-02 | Booking modification (change dates/room, with fee) |
| P1-03 | Notification centre (in-app + email preferences) |
| P1-04 | Audit log (who/when/what on every mutation) |
| P1-05 | GDPR data export + self-deletion |
| P1-06 | CI/CD pipeline (lint, test, build, scan, deploy) |
| P1-07 | Micrometer → Prometheus + dashboards |
| P1-08 | Error tracking (Sentry/equivalent) |
| P1-09 | Soft delete on entities |
| P1-10 | WebSocket chat / live support |
| P1-11 | Multi-tenancy scaffolding (Property entity + property_id columns) |
| P1-12 | UI for room type / amenity / voucher admin CRUD |
| P1-13 | Discount engine (weekend / seasonal / length-of-stay / per-guest) |
| P1-14 | i18n UI selector + remove mojibake from `messages_vi.properties` |
| P1-15 | Pagination + filtering + sorting on every list endpoint |
| P1-16 | Email-template editor (instead of hardcoded HTML in Java) |
| P1-17 | Security headers (CSP, HSTS, X-Frame-Options) |
| P1-18 | Dependency CVE scan (OWASP/Snyk) in pipeline |

### P2 — Recommended

| # | Gap |
|---|---|
| P2-01 | Receptionist + Manager roles with dedicated dashboards |
| P2-02 | Housekeeping / maintenance schedule |
| P2-03 | Channel manager (Booking.com, Expedia, Airbnb) sync |
| P2-04 | Loyalty / points program |
| P2-05 | Review photo upload + hotel reply |
| P2-06 | "Notify me when available / price drops" on wishlist |
| P2-07 | iCal export |
| P2-08 | 2FA / MFA |
| P2-09 | CAPTCHA on auth forms |
| P2-10 | Group booking (multi-room) |
| P2-11 | Extra services / add-ons (breakfast, transfer) |
| P2-12 | Scheduled email reports |
| P2-13 | Multi-currency display |
| P2-14 | Map view + nearby attractions |
| P2-15 | Compare villas |

### P3 — Nice to have

| # | Gap |
|---|---|
| P3-01 | Dark mode |
| P3-02 | PWA / offline |
| P3-03 | 360° tour / video per room |
| P3-04 | Push notifications (browser) |
| P3-05 | Onboarding tour |
| P3-06 | Storybook |
| P3-07 | Theme customisation by guest |
| P3-08 | Public profile for staff (receptionist) |

---

## 13. Dead / Placeholder Code Inventory

| File / Symbol | Issue | Recommendation |
|---|---|---|
| `BookingScheduler.autoCancelExpiredBookings` | Prints "skipping", does nothing | Delete or implement against `findExpiredPaymentBookings` |
| `BookingScheduler.sendPaymentReminders` | No-op, even not annotated `@Scheduled` | Delete or implement |
| `EmailService.sendBookingCancelledEmail` | Never called | Wire into `BookingService.cancelBooking` |
| `BookingStatus.AWAITING_APPROVAL` | Never written | Remove or use as the initial booking status |
| `update_constraint.sql` | References `'RENTING'` not in enum; missing `'PENDING'` | Reconcile or remove |
| `InfoApi` | Returns "placeholder content for the {slug} page" | Implement or remove |
| `HotelApplication` | Has broken `DriverManager.println(...)` call | Fix to `System.out.println` or remove |
| `WishlistItem.id` (Integer) vs `WishlistRepository<T, Long>` | Type mismatch | Reconcile to `Long` |
| `BookingApi.createBookingRequest.guests` | Declared, never used | Either persist or remove |
| `GlobalExceptionHandler` | `RuntimeException` → 400 (should be 500) | Map to 500 |
| `DataInitializer.ensureLegacyAdminExists` | Seeds `username=a password=a` ADMIN | Remove from prod profile |
| `ProfileApi` uses `@ModelAttribute` for JSON | Will break JSON-only clients | Switch to `@RequestBody` |
| `UserApi.update` (`/users/me`) | Duplicate of `/profile/password` | Remove or consolidate |

---

## 14. Architecture / Code Smell Inventory

- `BookingApi.create` mutates the persisted booking to apply a voucher after the fact — the voucher apply should be inside `BookingService.createBooking` so the whole flow is transactional.
- `approveBooking` / `rejectBooking` / `confirmPayment` are dead from HTTP.
- `ReportApi` returns raw `InputStreamResource` instead of the `ApiResponse` envelope.
- `BookingRepository.findMostratingRooms` typo (`Mostrating`) and only rates rooms that have a review.
- `messages_vi.properties` is mojibake (`??t phòng khách s?n`).
- `SpaFallbackController` uses a relative path that breaks outside the standard cwd.
- `WishlistItem` PK type mismatch (see above).
- Entity setters are public (`@Data`); Hibernate invariants live only in setter logic for `Review.rating`.
- No `@PrePersist` / `@PreUpdate` lifecycle hooks — createdAt is set in service code.
- No `@EntityListeners(AuditingEntityListener.class)` / `@CreatedBy` / `@LastModifiedBy`.
- No tests for security, no controller slice tests, no integration tests against a real DB.
- No payment, no webhook, no idempotency key on POST endpoints.
- No rate limit, no CAPTCHA, no breach detection.
- API uses cookies; no CSRF token issued because CSRF is disabled — fine for SPA but no protection if extended.

---

## 15. Final Verdict

The product today is a **functional demo / MVP**: a guest can browse villas, book them, write reviews, and an admin can manage rooms and generate reports. But it is **not enterprise-grade** — there is **no payment**, **no audit**, **no concurrency safety**, **no transactional voucher**, **no rate limit**, **no migration system**, **no CI/CD**, **no monitoring**, **no GDPR tooling**, and **no multi-tenancy**.

For a real launch:
1. Solve **P0-01 → P0-10** (payments, taxes, locks, migrations, comms, currency).
2. Then **P1** for production-grade robustness.
3. **P2/P3** based on market need.

The architecture itself is clean (Spring Boot + React, proper layered design, exception hierarchy, session bridge, request IDs). The work ahead is **feature-complete** more than **re-architect**.