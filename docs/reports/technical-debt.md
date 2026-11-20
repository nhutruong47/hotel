# Technical Debt Register — Nhu Villas

| Thuộc tính | Giá trị |
|---|---|
| Ngày | 2026-11-20 |
| Baseline | commit `7a3ccde` |
| Thang Effort | S ≤ 1 ngày · M 2–5 ngày · L 1–3 tuần |
| Thang Ưu tiên | P0 chặn go-live · P1 trước go-live · P2 quý tới · P3 khi có dịp |

Loại nợ: **Code** (thiết kế/code smell), **Arch** (kiến trúc/boundary), **Data** (schema/migration), **Test**, **Ops** (hạ tầng/vận hành), **Doc**, **Sec** (bảo mật).

---

## 1. Bảng nợ kỹ thuật

| ID | Loại | Mô tả | Vị trí | Tác động | Effort | Ưu tiên | Phase xử lý |
|---|---|---|---|---|---|---|---|
| TD-01 | Code | Booking 0 đêm hợp lệ nhưng không được overlap query bắt | `booking/service/BookingService.java:949`; `booking/repository/BookingRepository.java:81-83` | Double-booking day-use | S | P0 | P0 |
| TD-02 | Code | Refund cộng đôi vào `bookings.refund_amount` | `payment/service/PaymentService.java:436-438` + `BookingService.java:979` | Báo cáo hoàn tiền sai, đối soát sai | S | P0 | P0 |
| TD-03 | Arch | Controller trả JPA entity | `admin/api/AdminApi.java`, `payment/api/PaymentApi.java`, `booking/api/BookingApi.java`, `account/api/AccountApi.java` | 500 ở prod (lazy), lộ PII, khóa API vào schema | M | P0 | P0 |
| TD-04 | Data | Không có ràng buộc DB chống overlap / ngày ngược | `db/postgresql/V1__init.sql` | Mất lưới an toàn khi code lỗi hoặc ghi ngoài app | S | P0 | P0 |
| TD-05 | Test | CI chỉ chạy H2; không test migration PostgreSQL | `.github/workflows/ci.yml`; `src/test/resources/application-test.properties` | Lỗi migration/mapping chỉ lộ ở prod | M | P0 | P0 |
| TD-06 | Sec | CSRF cookie `HttpOnly=false` | `security/CsrfCookieFilter.java:96` (baseline) | Mutation từ browser có thể bị chặn toàn bộ | S | P0 | P0 |
| TD-07 | Code | Refund adapter trả success giả cho SePay/manual/Stripe tắt | `service/payment/StripePaymentAdapter.java:186-198` | Ghi nhận đã hoàn tiền khi chưa chuyển | S | P1 | P1-Payment |
| TD-08 | Code | Tạo Stripe Session/Intent mới mỗi lần gọi | `payment/api/PaymentApi.java` (`/stripe-checkout`, `/intent`) | Thu tiền 2 lần | M | P1 | P1-Payment |
| TD-09 | Code | SePay amount-mismatch không lưu payment | `payment/api/SepayWebhookApi.java` | Tiền vào không có record | S | P1 | P1-Payment |
| TD-10 | Code | Promotion `currentUses` không hoàn lại khi expire/cancel | `BookingService.java:307,528-536`; `scheduler/BookingScheduler.java` | Mã khuyến mãi cạn sai | S | P1 | P1-Payment |
| TD-11 | Code | `modifyBooking` bỏ extra-guest fee, bỏ maintenance check, đổi total booking PAID | `BookingService.java:610-660` | Doanh thu sai, chồng lịch bảo trì | M | P1 | P1-Payment |
| TD-12 | Code | Không Idempotency-Key cho tạo booking | `booking/api/BookingApi.java:121` | Double-submit tạo 2 hold | M | P1 | P1-Payment |
| TD-13 | Sec | RBAC thủ công `"ADMIN".equals` (16 chỗ/7 file) | `PaymentApi.java`, `AdminApi.java`, `BookingService.java:514,612`, `user/api/UserApi.java` | Sai quyền MANAGER, dễ sót | M | P1 | P1-Security |
| TD-14 | Sec | Admin mặc định `admin123` khi profile null/default | `config/AdminCredentialValidator.java:56,163-170` | Chiếm quyền admin | S | P1 | P1-Security |
| TD-15 | Sec | Email verification bypass khi email tắt | `user/service/UserService.java:170-172` | Tài khoản rác, mạo danh email | S | P1 | P1-Security |
| TD-16 | Sec | Demo login password hardcode ở FE | `frontend/src/features/auth/AuthPages.tsx:337,352,447` | Tài khoản demo lộ trên prod | S | P1 | P1-Security |
| TD-17 | Arch | Session, rate limit, login lockout in-memory | `config/RateLimitFilter.java`, `common/service/LoginAttemptService.java` | Không scale ngang, mất trạng thái khi restart | M | P1 | P1-Security |
| TD-18 | Ops | nginx không TLS; header mất ở location; CORS `*` | `nginx.conf:54,88-103,121-124` | MITM, clickjacking, CORS sai | S | P1 | P1-Security / P5 |
| TD-19 | Data | ID `Integer` (23 entity) vs `BIGINT` | `*/model/*.java`; `db/postgresql/V1__init.sql` | Lỗi `validate`, overflow lâu dài | M | P2 | P2-DB |
| TD-20 | Data | 3 track migration + SQL Server scripts | `db/migration/`, `db/postgresql-legacy/`, `database/*.sql`, `backend/*.sql` | Nhầm lẫn, drift | S | P2 | P2-DB |
| TD-21 | Data | `rooms.slug` không UNIQUE, `rooms.version` nullable | `db/postgresql/V12__align_entity_schema.sql` | URL trùng, optimistic lock vô hiệu với NULL | S | P2 | P2-DB |
| TD-22 | Data | FK/index/CHECK thiếu (approved_by, applied_voucher_code, rating 1–5, enum) | V1, V6, V12 | Toàn vẹn dữ liệu | M | P2 | P2-DB |
| TD-23 | Data | Audit fields không chuẩn (`modified_at` vs `updated_at`, không `created_by`) | V1 | Truy vết khó | M | P2 | P2-DB |
| TD-24 | Data | `villa_inspections` SERIAL/INT + TIMESTAMPTZ lệch chuẩn | `db/postgresql/V11__...sql` | Không nhất quán | S | P2 | P2-DB |
| TD-25 | Data | Refund bank account plaintext | `db/postgresql/V6__...sql` | Rủi ro PII | M | P2 | P2-DB |
| TD-26 | Arch | `BookingService` ~980 dòng gồm analytics | `booking/service/BookingService.java` | Khó bảo trì/test | L | P2 | P3-Modular |
| TD-27 | Arch | `AdminApi` 781 dòng gọi nhiều module | `admin/api/AdminApi.java` | Coupling | L | P2 | P3-Modular |
| TD-28 | Arch | `PaymentService` ghi trực tiếp `BookingRepository`/transition | `payment/service/PaymentService.java:392,452-458` | Vi phạm boundary | M | P2 | P3-Modular |
| TD-29 | Arch | `common/service` grab-bag; adapter payment ngoài module | `common/service/*`, `service/payment/*` | Ownership mờ | M | P2 | P3-Modular |
| TD-30 | Ops | Scheduler không distributed lock; load toàn bộ booking | `scheduler/BookingScheduler.java` | Chạy trùng khi >1 instance; chậm khi dữ liệu lớn | S | P2 | P3-Modular |
| TD-31 | Ops | Không metrics/JSON log/alert | `application-prod.properties:108`, `logback-spring.xml` | Không phát hiện sự cố | M | P1 | P5 |
| TD-32 | Ops | Không backup/restore | — | Mất dữ liệu vĩnh viễn | M | P1 | P5 |
| TD-33 | Ops | Compose: port public, default password, mount sai `init-db.sql`, không staging | `docker-compose.yml:17,19,32-33,48-52` | Lộ dịch vụ; khởi tạo DB sai | S | P1 | P5 |
| TD-34 | Ops | `.env.example` sai engine (SQL Server), thiếu biến | `.env.example` | Onboarding lỗi | S | P2 | P5 |
| TD-35 | Test | Không coverage tool, không concurrency test trên PostgreSQL, FE 1 test | `pom.xml`, `frontend/` | Rủi ro regression | M | P1 | P0/P6 |
| TD-36 | Code | Dead code (MockPaymentAdapter, RateLimiterInterceptor, Validated*, 5 exception, WEBHOOK_QUEUE, WebMvcConfig dist, `@EnableScheduling` x2) | nhiều | Nhiễu | S | P3 | P3-Modular |
| TD-37 | Code | `legacy-mvc/` không dùng (51 class, permitAll) | `legacy-mvc/` | Nhầm lẫn, bề mặt tấn công nếu chạy nhầm | S | P2 | P2-DB (archive) |
| TD-38 | Code | FE component >1000 dòng; trùng `profile` vs `account` | `frontend/src/features/admin/*`, `features/profile/*` | Khó bảo trì | L | P3 | sau P5 |
| TD-39 | Code | FE `useCurrency` heuristic `<100000 = USD` (lặp 4 nơi) | `frontend/src/shared/i18n/hooks.ts:36` | Hiển thị tiền sai | S | P2 | sau P5 |
| TD-40 | Code | FE `SessionProvider` giữ user stale | `frontend/src/shared/auth/SessionProvider.tsx` | UX sai trạng thái đăng nhập | S | P2 | P1-Security |
| TD-41 | Doc | Tài liệu mâu thuẫn (README: GiST, PG17, role CUSTOMER/INSPECTOR; ARCHITECTURE.md DB section; skills deploy/run-locally SQL Server) | `README.md`, `docs/ARCHITECTURE.md`, `.agents/skills/*` | Onboarding sai | S | P2 | Liên tục |
| TD-42 | Ops | Scripts chết trong `tools/` (Python one-off với đường dẫn cá nhân) | `tools/` | Nhiễu | S | P3 | P5 |
| TD-43 | Code | H2 ship trong jar prod (runtime scope) | `backend/pom.xml` | Bề mặt tấn công, kích thước | S | P2 | P2-DB |
| TD-44 | Data/Code | Tiền tệ lẫn lộn: giá seed thang USD (450…) trong hệ thống VND; phụ thu khách "25" | `config/DataInitializer.java`; `BookingService.java:185-192` (baseline) | Thu sai tiền (450 VND), Stripe từ chối | S | P1 | P1-Payment (phát hiện khi làm P1) |
| TD-45 | Code | Hóa đơn PDF in `$` và tính `Net Total = Total − Discount` (trừ giảm giá 2 lần) | `common/service/InvoiceService.java:54,61-63` (baseline) | Chứng từ sai | S | P1 | P1-Payment (phát hiện khi làm P1) |
| TD-46 | Code | Prompt AI concierge báo giá USD, phụ thu "25 USD" | `common/service/GeminiService.java:55,76,203` (baseline) | Tư vấn sai giá | S | P2 | P1-Payment (phát hiện khi làm P1) |
| TD-47 | Sec | Mật khẩu tài khoản demo hardcode trong seed | `config/DataInitializer.java:552` | Ai biết mật khẩu đăng nhập được tài khoản demo trên môi trường chạy profile `seed` | S | P2 | P5 |
| TD-48 | Data | `upsertVilla` ghi đè giá villa mỗi lần khởi động với profile `seed` | `config/DataInitializer.java` (`upsertVilla`) | Giá admin sửa bị mất khi restart | S | P2 | P2-DB |
| TD-49 | Test | Chưa có test phân quyền tham số hóa endpoint × role (AC-06) | `src/test` | Sai quyền khó phát hiện khi thêm endpoint | M | P1 | P6 |
| TD-50 | Test | Reuse Stripe Checkout Session/PaymentIntent không có kiểm thử tự động | `payment/api/PaymentApi.java` (`createStripeCheckout`), `PaymentService.findReusableIntent` | Regression thu tiền 2 lần không bị phát hiện | M | P1 | P6 (Stripe test mode / stripe-mock) |

## 1b. Trạng thái xử lý (cập nhật 2026-11-20)

| ID | Trạng thái | Nhánh | Ghi chú / bằng chứng |
|---|---|---|---|
| TD-01 | ✔ Đã xử lý | `feature/p0-hardening` | `booking/model/Occupancy.java`; `DayUseOverlapIntegrationTest` |
| TD-02 | ✔ Đã xử lý | `feature/p0-hardening` | `PaymentServiceTest` (P0-03) |
| TD-03 | ✔ Đã xử lý | `feature/p0-hardening` | DTO + `ReadOnlyTransactions`; `NoEntityLeakIntegrationTest` |
| TD-04 | ✔ Đã xử lý | `feature/p0-hardening` | V13; `BookingIntegrityIT` |
| TD-05 | ✔ Đã xử lý | `feature/p0-hardening` | Testcontainers/failsafe; CI `./mvnw -B verify` |
| TD-06 | ✔ Đã xử lý | `feature/p0-hardening` | `CsrfCookieFilterTest` |
| TD-07 | ✔ Đã xử lý | `feature/p1-security-payment` | Refund thủ công `PENDING_MANUAL` (V14); adapter không báo success giả; `ManualRefundIT` |
| TD-08 | ⚠ Đã sửa, chưa test tự động | `feature/p1-security-payment` | Reuse session/intent + idempotency key; xem TD-50 |
| TD-09 | ✔ Đã xử lý | `feature/p1-security-payment` | `PaymentService.recordUnappliedTransfer`; `SepayWebhookApiTest` |
| TD-10 | ✔ Đã xử lý | `feature/p1-security-payment` | `VoucherService.releaseDiscountCode`; `BookingPolicyIntegrationTest` |
| TD-11 | ✔ Đã xử lý | `feature/p1-security-payment` | `modifyBooking` viết lại (ADR-0009 Q3); `BookingPolicyIntegrationTest` |
| TD-12 | ⚠ Một phần | `feature/p1-security-payment` | Natural idempotency (cùng khách/villa/ngày/số khách khi hold còn hiệu lực); chưa dùng header `Idempotency-Key` |
| TD-13 | ✔ Đã xử lý | `feature/p1-security-payment` | `security/Authz.java`; còn thiếu test ma trận (TD-49) |
| TD-14 | ✔ Đã xử lý | `feature/p1-security-payment` | `AdminCredentialValidatorTest` |
| TD-15 | ✔ Đã xử lý | `feature/p1-security-payment` | `app.auth.auto-verify-without-email`; `UserServiceVerificationPolicyTest` |
| TD-16 | ✔ Đã xử lý | `feature/p1-security-payment` | `NEXT_PUBLIC_ENABLE_DEMO_LOGIN`; mật khẩu seed còn lại → TD-47 |
| TD-17 | ⚠ Một phần | `feature/p1-security-payment` | Rate limiter: evict window hết hạn, chỉ áp `/api/**`, bucket nhạy cảm riêng (`RateLimitFilterTest`). Vẫn in-memory (session, rate limit, lockout) → P3/P5 |
| TD-18 | ❌ Mở | — | P5 |
| TD-39 | ⚠ Giảm tác động | `feature/p1-security-payment` | Dữ liệu chuyển sang VND (≥ 100.000) nên heuristic `<100000 = USD` không còn kích hoạt với giá villa; heuristic vẫn còn trong FE |
| TD-40 | ✔ Đã xử lý | `feature/p1-security-payment` | `SessionProvider.refresh` coi `user:null` từ server là chính thức |
| TD-44 | ✔ Đã xử lý | `feature/p1-security-payment` | ADR-0009 Q6; `PricingPolicy` + seed VND + `CurrencySanityCheck`; `PricingPolicyTest`. Dữ liệu prod cần chuyển đổi khi deploy (R-26) |
| TD-45 | ✔ Đã xử lý | `feature/p1-security-payment` | `InvoiceService` in Subtotal/Discount/Phí/VAT/Total bằng VND; không trừ giảm giá lần hai. Chưa có test PDF |
| TD-46 | ✔ Đã xử lý | `feature/p1-security-payment` | Prompt dùng VND + phụ thu cấu hình. Chưa có test |

Các mục không liệt kê ở bảng này giữ nguyên trạng thái **Mở** theo phase trong bảng §1.

## 2. Tổng hợp

| Ưu tiên | Số mục | Effort ước lượng |
|---|---:|---|
| P0 | 6 | ~1.5 tuần — **6/6 đã xử lý** (P0) |
| P1 | 15 (+4 mới: TD-44, TD-45, TD-49, TD-50) | ~4 tuần — đã xử lý 12, một phần 3 (TD-08, TD-12, TD-17); còn mở TD-18, TD-31, TD-32, TD-33, TD-35, TD-49, TD-50 |
| P2 | 17 (+4 mới: TD-46, TD-47, TD-48; TD-39, TD-40 đã xử lý/giảm) | ~5 tuần |
| P3 | 5 | ~2 tuần |

## 3. Quy tắc quản lý nợ

- Mỗi PR sửa nợ phải tham chiếu ID `TD-xx` và cập nhật cột trạng thái ở đây (thêm cột khi bắt đầu xử lý).
- Không thêm nợ mới loại **Sec**/**Data** mà không có ADR hoặc risk register entry.
- Rà soát register mỗi cuối phase (xem [implementation-plan.md](../roadmap/implementation-plan.md)).
