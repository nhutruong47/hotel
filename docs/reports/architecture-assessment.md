# Architecture Assessment — Nhu Villas

| Thuộc tính | Giá trị |
|---|---|
| Ngày | 2026-11-20 |
| Baseline | commit `7a3ccde`, branch `develop` |
| Vai trò thực hiện | Architecture team (Principal/Solution Architect, Security, DBA, DevOps, QA, BA) |
| Liên quan | [current-architecture.md](../architecture/current-architecture.md), [technical-debt.md](technical-debt.md), [risk-register.md](risk-register.md) |

---

## 1. Kết luận điều hành

Hệ thống **không phải viết lại**. Phần lõi nghiệp vụ (booking state machine, pessimistic lock, payment idempotency, transactional outbox, audit log) đã được làm cứng ở mức khá. Vấn đề chính nằm ở:

1. **Một số lỗ hổng tính đúng đắn** ở luồng tiền và tồn kho: booking 0 đêm lọt qua kiểm tra overlap, refund bị cộng đôi, refund "giả lập thành công" cho SePay/manual, có thể tạo nhiều Stripe Session cho một booking.
2. **Không có lưới an toàn ở DB**: không exclusion constraint chống overlap, thiếu CHECK/FK; CI không test migration PostgreSQL.
3. **Không scale ngang được**: session, rate limit, login lockout in-memory; scheduler không có distributed lock.
4. **Vận hành production chưa sẵn sàng**: không TLS ở nginx, không monitoring, không backup, secret thật trong git history.
5. **Boundary module mờ**: feature packages tồn tại nhưng service gọi chéo repository; hai class "God" (`BookingService` 981 dòng, `AdminApi` 781 dòng).

Mức sẵn sàng production tổng thể: **5.5/10** (đủ cho demo/capstone, chưa đủ cho vận hành có tiền thật).

---

## 2. Chấm điểm theo thuộc tính chất lượng

Thang 0–10. "Mục tiêu" là ngưỡng chấp nhận cho go-live.

| Thuộc tính | Điểm | Mục tiêu | Căn cứ chính |
|---|---:|---:|---|
| Maintainability | 5 | 7 | Feature packages tốt; nhưng `BookingService.java` 981 dòng, `AdminApi.java` 781 dòng, `common/service` grab-bag, 3 track migration, `legacy-mvc/` chết, tài liệu mâu thuẫn |
| Scalability | 3 | 6 | Session/rate-limit/lockout in-memory; scheduler không lock; scheduler load toàn bộ booking (`scheduler/BookingScheduler.java`) |
| Security | 5 | 8 | BCrypt, CSRF double-submit, webhook verify tốt; nhưng CSRF cookie lỗi, `admin123` default, email verification bypass, demo login FE, key trong history, nginx không TLS |
| Reliability | 6 | 8 | Outbox + publisher confirms tốt; refund giả lập, double-count, SePay mismatch không lưu, scheduler đa instance trùng chạy |
| Performance | 6 | 7 | Index booking hợp lý; FE nặng (three.js, gsap) + 50 `<img>`; `getAllBookings` trong scheduler |
| Testability | 5 | 8 | ~235 `@Test`/25 file, Mockito tốt; nhưng chỉ H2, không Testcontainers, không coverage, FE 1 test |
| Observability | 3 | 7 | requestId tốt; không metrics, không JSON log, không tracing, không alert |
| Extensibility | 5 | 7 | `PaymentGateway` abstraction có; enum gateway có VNPAY/VIETQR chưa làm; không có `property_id` |
| Business alignment | 7 | 8 | `BOOKING_LIFECYCLE_AND_RBAC.md` rõ ràng và khớp code; thiếu housekeeping/inventory/employee |

---

## 3. Phát hiện theo tầng

### 3.1 Backend

| ID | Mức | Phát hiện | Vị trí |
|---|---|---|---|
| BE-01 | Critical | Booking 0 đêm (`checkIn == checkOut`) được phép và không bao giờ khớp điều kiện overlap `b.checkInDate < :checkOut` → day-use trùng nhau / trùng ngày check-in của khách khác | `booking/service/BookingService.java:949`, `booking/repository/BookingRepository.java:81-83`, pricing day-use `:171-172` |
| BE-02 | Critical | Refund cộng đôi: cancel set `refundAmount` theo policy, refund cộng thêm số thực | `BookingService.java:979`, `payment/service/PaymentService.java:436-438` |
| BE-03 | Critical | Refund trả `true` không chuyển tiền khi Stripe tắt hoặc payment không có intent (SePay/manual) | `service/payment/StripePaymentAdapter.java:186-198` |
| BE-04 | High | Controller trả JPA entity (Booking, Payment, Transition, ContactMessage, Review, Promotion) trong khi prod `open-in-view=false` → nguy cơ `LazyInitializationException`, lộ refund bank info / `rawResponse` | `admin/api/AdminApi.java`, `payment/api/PaymentApi.java`, `booking/api/BookingApi.java` (timeline), `account/api/AccountApi.java` ; `application-prod.properties:33` |
| BE-05 | High | `POST /admin/promotions` bind entity `Promotion` làm request body, không `@Valid` (mass assignment) | `admin/api/AdminApi.java` |
| BE-06 | High | Mỗi lần gọi `/payments/stripe-checkout` hoặc `/payments/intent` tạo session/intent mới → khách có thể bị trừ tiền 2 lần | `payment/api/PaymentApi.java`, `payment/service/PaymentService.java` |
| BE-07 | High | SePay amount mismatch trả 200 `success:false` và **không lưu** payment → mất dấu tiền thật | `payment/api/SepayWebhookApi.java:65+` |
| BE-08 | High | RBAC thủ công `"ADMIN".equals(role)` (16 chỗ) không theo hierarchy → MANAGER bị chặn ở payment admin; dễ sót | `PaymentApi.java`, `AdminApi.java`, `BookingService.java:514,612` |
| BE-09 | Medium | Promotion `currentUses` không được trả lại khi expire/cancel | `BookingService.java:307` vs `:528-536` |
| BE-10 | Medium | `modifyBooking` bỏ phí extra guest, bỏ kiểm tra maintenance/availability, đổi total của booking PAID không thu/hoàn chênh lệch | `BookingService.java:610-660` |
| BE-11 | Medium | Không có Idempotency-Key cho tạo booking (header có trong CORS nhưng không dùng) | `BookingApi.java:121` |
| BE-12 | Medium | Scheduler không distributed lock; auto-complete/no-show load toàn bộ booking | `scheduler/BookingScheduler.java` |
| BE-13 | Medium | Validation thiếu ở nhiều body admin (Room, Voucher, RoomType, Amenity, maintenance), AiApi `Map` body | `AdminApi.java`, `ai/api/AiApi.java` |
| BE-14 | Low | Dead code: `MockPaymentAdapter`, `RateLimiterInterceptor`, `api/dto/Validated*`, 5 exception class, `WEBHOOK_QUEUE`, `WebMvcConfig` serve `../frontend/dist`, `@EnableScheduling` x2 | nhiều |
| BE-15 | Low | Chuỗi tiếng Việt lỗi encoding | `payment/api/PaymentApi.java:282-290` |

### 3.2 Database

| ID | Mức | Phát hiện | Vị trí |
|---|---|---|---|
| DB-01 | Critical | Không có exclusion constraint chống overlap booking; không CHECK `check_out_date >= check_in_date` | `db/postgresql/V1__init.sql`, V6 |
| DB-02 | High | Drift kiểu ID: 23 entity dùng `Integer id`, cột PostgreSQL là `BIGSERIAL/BIGINT` (chỉ `PaymentRefund`, `NotificationOutbox`, `AuditLog` dùng `Long`) | `*/model/*.java` |
| DB-03 | High | 3 track migration lệch nhau; profile `postgres` tắt Flyway + `ddl-auto=update`; CI chỉ chạy H2 | `application-postgres.properties:10,16`, `.github/workflows/ci.yml` |
| DB-04 | Medium | `rooms.slug` không UNIQUE ở DB (entity khai báo unique); `rooms.version` nullable | `db/postgresql/V12__align_entity_schema.sql` |
| DB-05 | Medium | Thiếu FK: `bookings.approved_by`, `bookings.applied_voucher_code`; thiếu index FK `room_gallery_images.room_id`, `review_photos.review_id`, `promotion_rooms.room_id` | V1, V12 |
| DB-06 | Medium | `villa_inspections` dùng `SERIAL/INT` + `TIMESTAMPTZ` khác phần còn lại | `db/postgresql/V11__add_villa_inspections_workflow.sql` |
| DB-07 | Medium | Enum lưu VARCHAR không CHECK (payments.status/method, reviews.rating 1–5, contact status…) | V1 |
| DB-08 | Medium | Refund bank account lưu plaintext | `db/postgresql/V6__...sql` |
| DB-09 | Low | Không có audit fields chuẩn (`updated_at`, `created_by`) ở nhiều bảng; `bookings` dùng `modified_at` | V1 |
| DB-10 | Low | `database/init-db.sql` hardcode password `hotel_password`, thiếu `btree_gist` | `database/init-db.sql` |

### 3.3 Frontend

| ID | Mức | Phát hiện | Vị trí |
|---|---|---|---|
| FE-01 | Critical | Nếu cookie CSRF là HttpOnly, `readCookie('XSRF-TOKEN')` trả undefined → mọi mutation bị 403 `CSRF_TOKEN_INVALID` | `frontend/src/shared/api/client.ts:29-55` + BE-SEC-01 |
| FE-02 | High | Demo "Digital Keycard" login với password hardcode ship lên production | `frontend/src/features/auth/AuthPages.tsx:337,352,447` |
| FE-03 | Medium | `SessionProvider.refresh()` giữ user cache khi server trả `user:null` → UI hiển thị đã đăng nhập sau khi session hết hạn | `frontend/src/shared/auth/SessionProvider.tsx` |
| FE-04 | Medium | Không `middleware.ts`; `/admin` chỉ yêu cầu login, lọc tab client-side | `features/admin/AdminDashboardPage.tsx:151-155` |
| FE-05 | Medium | `useCurrency` đoán USD nếu amount < 100000 | `shared/i18n/hooks.ts:36`; heuristic `< 100000` lặp ở `features/admin/AdminDashboardPage.tsx:111`, `AdminExtendedPanels.tsx:96`, `account/AccountDashboardPage.tsx:30`, `profile/ProfilePages.tsx:135` |
| FE-06 | Medium | Component khổng lồ (1000–1500 dòng); trùng `features/profile` vs `features/account` | `features/admin/*`, `features/auth/AuthPages.tsx` |
| FE-07 | Low | i18n không nhất quán (ternary `isVi ?` hàng trăm chỗ, admin hardcode EN); `<html lang="en">` cố định | `shared/i18n/translations.ts` |
| FE-08 | Low | 50 `<img>` thô vs 3 `next/image`; `SettingsProvider` render `opacity-0` trước mount (SEO/LCP) | `shared/settings/*` |
| FE-09 | Low | Test: 1 file vitest; spec Playwright không chạy được | `features/booking/__tests__/` |

### 3.4 Security (tổng hợp, tham chiếu OWASP ASVS 4.0.3)

| ID | ASVS | Mức | Phát hiện | Vị trí |
|---|---|---|---|---|
| SEC-01 | V4.2.2 / V3.5 | Critical | CSRF cookie `HttpOnly=false` → HttpOnly theo RFC 6265 §5.2.6 | `security/CsrfCookieFilter.java:96` (baseline) |
| SEC-02 | V2.10.4 / V14.3 | Critical | Gemini API key thật trong git history (commit `efd7e35`, đã push) | git history |
| SEC-03 | V2.1 / V14.1 | High | `AdminCredentialValidator` coi profile `null`/`default` là dev → tạo `admin/admin123` | `config/AdminCredentialValidator.java:56,163-170` |
| SEC-04 | V2.1.x | High | Email verification tự bỏ qua khi email tắt (mặc định prod) | `user/service/UserService.java:170-172` |
| SEC-05 | V4.1.3 | High | RBAC thủ công không theo hierarchy (BE-08) | |
| SEC-06 | V9.1 / V14.4 | High | nginx không TLS, mất security header ở `location`, `ACAO: *` | `nginx.conf:54,88-103,121-124` |
| SEC-07 | V11.1.4 | Medium | Rate limit in-memory, trust `X-Forwarded-For` từ mọi IP private; `/ai/recommend` public gọi Gemini (chi phí) | `config/RateLimitFilter.java` |
| SEC-08 | V8.3 | Medium | Entity trong response lộ refund bank info, `rawResponse` gateway (BE-04) | |
| SEC-09 | V14.1 | Medium | Compose publish DB/Redis/RabbitMQ ra host, default `guest/guest`, `redis_password` | `docker-compose.yml:19,32-33,48-52` |
| SEC-10 | V7.1 | Low | Log không cấu trúc, không có cảnh báo bảo mật | `logback-spring.xml` |

### 3.5 Infrastructure / DevOps

| ID | Mức | Phát hiện | Vị trí |
|---|---|---|---|
| OPS-01 | High | Không backup/restore PostgreSQL | — |
| OPS-02 | High | Không metrics/alert; Actuator chỉ `health,info` | `application-prod.properties:108` |
| OPS-03 | High | CI không test migration PostgreSQL, không build image, không deploy, không secret/dependency scan | `.github/workflows/ci.yml` |
| OPS-04 | Medium | Mount `./init-db.sql` không tồn tại | `docker-compose.yml:17` |
| OPS-05 | Medium | Không staging; compose chỉ chạy profile prod | `docker-compose.yml:69` |
| OPS-06 | Medium | `.env.example` trỏ SQL Server, thiếu `DB_PASSWORD`, `STRIPE_*`, `SMTP_*` | `.env.example` |
| OPS-07 | Low | Scripts chết trong `tools/` (11 Python one-off, đường dẫn cá nhân); `.agents/skills/deploy`, `run-locally` lỗi thời | `tools/`, `.agents/skills/` |

---

## 4. Những phần nên giữ lại

| Thành phần | Lý do |
|---|---|
| `BookingStateMachine` + `booking_status_transitions` | Đúng với nguồn chân lý nghiệp vụ, có test (`BookingStateMachineTest`) |
| Pessimistic lock room row khi tạo booking | Đơn giản, hiệu quả cho tải single-property; có `BookingConcurrencyIntegrationTest` |
| Payment idempotency (`transaction_ref`/`intent_id` UNIQUE, `payment_refunds` idempotency key) | Thiết kế đúng hướng; chỉ cần bịt các lỗ BE-03/06/07 |
| Transactional outbox + RabbitMQ publisher confirms | Mẫu chuẩn enterprise, giữ nguyên |
| `GlobalExceptionHandler` + response envelope + `ApiContractArchitectureTest` | Nền tảng API contract tốt |
| `AuditLogService` (`REQUIRES_NEW`, redact, retention 180 ngày) | Đúng yêu cầu audit |
| Flyway track `db/postgresql` + chính sách forward-only | Giữ làm track duy nhất |
| FE `shared/api/client.ts` tập trung + TanStack Query | Kiến trúc tích hợp API sạch |
| Dockerfile multi-stage, non-root | Đạt baseline container security |

## 5. Những phần cần refactor (giữ hành vi, cải thiện cấu trúc)

- Tách `BookingService` → `BookingCommandService`, `PricingService`, `BookingQueryService`/analytics; tách `AdminApi` theo module.
- Chuyển `service/payment/*` vào module `payment`; giải tán `common/service`.
- RBAC: thay check thủ công bằng `@PreAuthorize` + `RoleHierarchy`.
- Session/rate-limit/lockout → Redis (Spring Session Data Redis, Bucket4j-Redis); scheduler → ShedLock.
- nginx: TLS, header kế thừa đúng, bỏ CORS `*`.
- FE: tách component lớn, hợp nhất `profile`/`account`, i18n chuẩn hóa.

## 6. Những phần cần thiết kế lại

- **Chiến lược migration/DB**: một track PostgreSQL, ID `BIGINT`/`Long` thống nhất, constraint đầy đủ, audit fields chuẩn, `property_id` (multi-property ready).
- **Housekeeping**: module mới (room status workflow, cleaning task, assignment) — hợp nhất với `VillaInspection`/`VillaMaintenance`.
- **Observability**: Micrometer/Prometheus/Grafana, JSON log, alert.
- **Môi trường & deploy**: dev/test/staging/prod, Docker Compose trên VPS với TLS, backup/restore.
- **Test strategy**: Testcontainers PostgreSQL/RabbitMQ, concurrency, chaos, load.

## 7. Định hướng kiến trúc mục tiêu

Modular monolith với 10 module + `content` hỗ trợ, ranh giới được enforce bằng ArchUnit. Xem [target-architecture.md](../architecture/target-architecture.md), [ADR-0001](../adr/0001-modular-monolith.md).

## 8. Hạn chế của đợt audit

- Không chạy được ứng dụng/test cục bộ (máy audit không có Java/Node/Docker) → các phát hiện runtime (LazyInit ở prod, CSRF trên browser, Integer↔BIGINT với `ddl-auto=validate`) là **suy luận từ mã nguồn**, cần xác nhận bằng test Testcontainers/browser.
- Không truy cập môi trường Render/Vercel thật; không kiểm dữ liệu production.
