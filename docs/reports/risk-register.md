# Risk Register — Nhu Villas

| Thuộc tính | Giá trị |
|---|---|
| Ngày tạo | 2026-11-20 |
| Cập nhật | 2026-11-20 — sau P1 (`feature/p1-security-payment`): R-08, R-10..R-14, R-20, R-21 đã sửa; R-09 đã sửa chưa kiểm thử tự động; thêm R-26, R-27 |
| Baseline | commit `7a3ccde` |
| Chu kỳ rà soát | Cuối mỗi phase trong [implementation-plan.md](../roadmap/implementation-plan.md) |

**Thang điểm**: Xác suất (P) 1–5 × Tác động (I) 1–5 = Điểm. ≥15 Critical · 10–14 High · 5–9 Medium · <5 Low.

**Owner**: `Repo owner` (chủ repo/PO), `Backend`, `Frontend`, `DevOps`, `DBA`, `QA`, `BA`.

---

## 1. Danh sách rủi ro

| ID | Rủi ro | Nguồn / Vị trí | P | I | Điểm | Owner | Mitigation | Trạng thái |
|---|---|---|---:|---:|---:|---|---|---|
| R-01 | Gemini API key thật bị lộ trong git history đã push công khai → lạm dụng quota/chi phí | commit `efd7e35` (removed `525e8f6`), file `.env` | 5 | 4 | 20 | Repo owner | Revoke & rotate key trên Google AI Studio; giới hạn quota; thêm gitleaks vào CI; không rewrite history (quyết định 2026-11-20) | **Chờ rotate** |
| R-02 | CSRF cookie bị trình duyệt coi là HttpOnly → FE không đọc được token → mọi POST/PUT/DELETE bị 403 | `backend/.../security/CsrfCookieFilter.java:96` (baseline) | 4 | 5 | 20 | Backend | Bỏ thuộc tính `HttpOnly` khỏi `Set-Cookie`; test header; kiểm thử trên browser thật | **Đã sửa + kiểm thử (feature/p0-hardening, chờ merge)** |
| R-03 | Double-booking với booking 0 đêm (day-use) do overlap query không bắt | `BookingService.java:949`, `BookingRepository.java:81-83` | 3 | 5 | 15 | Backend | Day-use chiếm trọn ngày `[d, d+1)` ở service (`booking/model/Occupancy.java`) + exclusion constraint V13 cùng biểu thức; BA xác nhận có giữ day-use (Q1) | **Đã sửa + kiểm thử (feature/p0-hardening, chờ merge)** |
| R-04 | Refund bị cộng đôi trên booking → báo cáo/đối soát sai, có thể hoàn thừa thủ công | `PaymentService.java:436-438`, `BookingService.java:979` | 4 | 4 | 16 | Backend | Đặt `refundAmount` = tổng refund thực (idempotent); test hủy+hoàn | **Đã sửa + kiểm thử (feature/p0-hardening, chờ merge)** |
| R-05 | Entity trả trực tiếp ra API → 500 `LazyInitializationException` trên prod + lộ refund bank info / gateway raw response | `AdminApi.java`, `PaymentApi.java`, `BookingApi.java` timeline, `AccountApi.java`; `application-prod.properties:33` | 4 | 4 | 16 | Backend | DTO cho mọi response, map trong read-only transaction (`common/tx/ReadOnlyTransactions`); `NoEntityLeakIntegrationTest` chạy với `open-in-view=false` + không có test transaction (đã tái hiện 500 trước khi sửa) | **Đã sửa + kiểm thử (feature/p0-hardening, chờ merge)** |
| R-06 | Không có ràng buộc DB chống overlap → dữ liệu sai khi có bug/ghi ngoài app | `db/postgresql/*` | 3 | 5 | 15 | DBA | Migration V13 `btree_gist` EXCLUDE + CHECK; Testcontainers IT | **Đã sửa + kiểm thử (feature/p0-hardening, chờ merge)** |
| R-07 | Migration PostgreSQL/ánh xạ entity (Integer↔BIGINT) chỉ lỗi khi deploy prod | `.github/workflows/ci.yml`; entity `Integer id` | 3 | 4 | 12 | QA | `PostgresMigrationIT` (Testcontainers) chạy trong CI | **Đã sửa + kiểm thử (feature/p0-hardening, chờ merge)** |
| R-08 | Refund "thành công" giả cho SePay/manual → khách không nhận tiền nhưng hệ thống ghi đã hoàn | `StripePaymentAdapter.java:186-198` | 4 | 4 | 16 | Backend | Refund không phải thẻ Stripe thật → `PENDING_MANUAL`; ADMIN xác nhận kèm `bankReference` mới `SUCCEEDED`; adapter không bao giờ báo success giả (ADR-0009 Q5, V14) | **Đã sửa + kiểm thử (feature/p1-security-payment, chờ merge)** |
| R-09 | Khách bị trừ tiền 2 lần do tạo nhiều Stripe Session/Intent | `PaymentApi.java` `/stripe-checkout`, `/intent` | 3 | 4 | 12 | Backend | Reuse Checkout Session còn `open` cùng số tiền, expire/supersede session cũ; reuse PaymentIntent còn confirm được; idempotency key `checkout-{booking}-{amount}-{attempt}`; session hết hạn sau 31' | **Đã sửa, chưa kiểm thử tự động (cần Stripe test mode)** |
| R-10 | Tiền SePay sai số tiền không được lưu → mất dấu, khiếu nại | `SepayWebhookApi.java` | 3 | 4 | 12 | Backend | Lưu payment `PAID` + `review_required` + lý do, báo ADMIN in-app; booking không được xác nhận (ADR-0009 Q4) | **Đã sửa + kiểm thử (feature/p1-security-payment, chờ merge)** |
| R-11 | Tài khoản admin mặc định `admin123` khi quên set profile | `AdminCredentialValidator.java:56,163-170` | 2 | 5 | 10 | Backend | `admin/admin123` chỉ khi profile `dev` tường minh; profile khác không có credentials → bỏ qua bootstrap (log WARN); `prod` fail-fast + chặn mật khẩu yếu | **Đã sửa + kiểm thử (feature/p1-security-payment, chờ merge)** |
| R-12 | Email verification bị bỏ qua trên prod | `UserService.java:170-172` | 4 | 3 | 12 | Backend | Auto-verify chỉ khi bật tường minh `app.auth.auto-verify-without-email` (mặc định `false`); email tắt + cờ tắt → đăng ký `REGISTRATION_UNAVAILABLE`, đăng nhập chưa verify bị từ chối | **Đã sửa + kiểm thử (feature/p1-security-payment, chờ merge)** |
| R-13 | Demo login có password public trên site production | `frontend/src/features/auth/AuthPages.tsx:337,352,447` | 5 | 3 | 15 | Frontend | Nút demo chỉ hiện khi `NEXT_PUBLIC_ENABLE_DEMO_LOGIN=true` và có `NEXT_PUBLIC_DEMO_LOGIN_PASSWORD`; mật khẩu không còn trong frontend source. Còn lại: mật khẩu tài khoản demo vẫn hardcode trong `backend/.../config/DataInitializer.java:552` (chỉ chạy profile `default/dev/postgres/seed`) — đổi mật khẩu các tài khoản demo trên môi trường đang chạy profile `seed` | **Đã sửa + kiểm thử (feature/p1-security-payment, chờ merge)** |
| R-14 | RBAC thủ công sai/sót quyền | 16 chỗ `"ADMIN".equals` | 3 | 4 | 12 | Backend | Helper `security/Authz.java` theo `UserRole.includes` thay mọi `"ADMIN".equals` dùng để phân quyền; xem booking/payment: owner hoặc STAFF+; danh sách payment: MANAGER+; refund/xác nhận: ADMIN | **Đã sửa + kiểm thử (feature/p1-security-payment, chờ merge)** |
| R-15 | nginx không TLS/mất security header/CORS `*` | `nginx.conf:54,92,121-124` | 4 | 4 | 16 | DevOps | TLS (Let's Encrypt), header `always` qua include, bỏ CORS ở nginx | Mở (P1/P5) |
| R-16 | Không backup → mất toàn bộ dữ liệu booking/payment khi hỏng volume | — | 2 | 5 | 10 | DevOps | `pg_dump` hằng ngày + WAL/offsite + restore drill hằng tháng | Mở (P5) |
| R-17 | Không monitoring/alert → sự cố (outbox kẹt, webhook lỗi) không ai biết | Actuator `health,info` | 4 | 3 | 12 | DevOps | Prometheus/Grafana, alert outbox backlog, webhook 4xx/5xx, DB pool | Mở (P5) |
| R-18 | Không scale ngang (state in-memory, scheduler chạy trùng) | `RateLimitFilter`, `LoginAttemptService`, `BookingScheduler` | 2 | 3 | 6 | Backend | Spring Session Redis, Bucket4j-Redis, ShedLock. P1 chỉ sửa rò bộ nhớ của rate limiter (evict window hết hạn) + thêm bucket nhạy cảm; vẫn in-memory | Mở (P3/P5) |
| R-19 | DB/Redis/RabbitMQ lộ port ra Internet với mật khẩu mặc định | `docker-compose.yml:19,32-33,48-52` | 3 | 5 | 15 | DevOps | Bỏ `ports` ở prod compose, secrets bắt buộc, firewall VPS | Mở (P5) |
| R-20 | Promotion cạn lượt sai do không hoàn `currentUses` | `BookingService.java:307` | 3 | 2 | 6 | Backend | `VoucherService.releaseDiscountCode` trả lượt voucher **hoặc** promotion khi expire/cancel hold | **Đã sửa + kiểm thử (feature/p1-security-payment, chờ merge)** |
| R-21 | `modifyBooking` làm sai tổng tiền booking PAID/đổi sang ngày bảo trì | `BookingService.java:610-660` | 2 | 4 | 8 | Backend | Viết lại: tái định giá đầy đủ (`PricingPolicy`), kiểm tra availability + bảo trì + min/max stay; booking `PAID` đổi tổng → `MODIFICATION_REQUIRES_SUPPORT` (ADR-0009 Q3) | **Đã sửa + kiểm thử (feature/p1-security-payment, chờ merge)** |
| R-22 | Nghiệp vụ day-use/late payment/housekeeping chưa được xác nhận → thiết kế sai | [business-analysis.md](../business/business-analysis.md) mục [CẦN XÁC NHẬN] | 2 | 3 | 6 | BA | Q1–Q7, Q9, Q11 đã chốt trong [ADR-0009](../adr/0009-business-policy-decisions.md) (theo ủy quyền repo owner); còn Q8 (housekeeping), Q10 (SLA) — workshop với chủ villa trước P4. Mọi quyết định ADR-0009 có thể đảo bằng ADR mới | Giảm (còn Q8, Q10) |
| R-23 | Refactor module lớn làm gãy hành vi đang chạy | P3 | 3 | 4 | 12 | Backend/QA | Strangler từng module, ArchUnit, regression suite xanh trước/sau | Mở (P3) |
| R-24 | Dữ liệu cũ vi phạm constraint mới (V13) làm migration fail khi deploy | Prod DB | 2 | 4 | 8 | DBA | DO-block pre-check báo id vi phạm; chạy preflight query trên bản sao prod trước | **Đã sửa + kiểm thử (feature/p0-hardening, chờ merge)** |
| R-25 | Bus factor/phụ thuộc AI agent: tài liệu `.agents/` lỗi thời gây hướng dẫn sai | `.agents/skills/deploy`, `run-locally` | 3 | 2 | 6 | Repo owner | Đánh dấu stale trong `docs/README.md`; cập nhật ở P5 | Mở |
| R-26 | Production đang dùng giá thang USD (450, 720…) → sau deploy hệ thống thu VND, khách bị thu 450 VND hoặc Stripe từ chối | Dữ liệu `rooms.price_per_night` trên môi trường đang chạy; `config/CurrencySanityCheck.java` cảnh báo khi khởi động; `PaymentService.requireChargeableAmount` chặn Stripe < 10.000 VND | 4 | 4 | 16 | Repo owner / DevOps | Khi deploy: đọc log `CURRENCY CHECK`, đổi giá villa sang VND (xem [p1-release-notes.md](../operations/p1-release-notes.md) §4); môi trường chạy profile `seed` tự nhận giá VND mới khi khởi động | Mở (hành động khi deploy) |
| R-27 | `APP_EMAIL_ENABLED=false` trên prod → sau P1 đăng ký mới bị tắt (`REGISTRATION_UNAVAILABLE`) cho tới khi cấu hình SMTP hoặc bật `APP_AUTH_AUTO_VERIFY_WITHOUT_EMAIL` | `application-prod.properties` (`app.email.enabled=${APP_EMAIL_ENABLED:false}`); `UserService.registerUser` | 4 | 3 | 12 | Repo owner / DevOps | Cấu hình SMTP + `APP_EMAIL_ENABLED=true` (khuyến nghị) hoặc chấp nhận rủi ro bằng `APP_AUTH_AUTO_VERIFY_WITHOUT_EMAIL=true` | Mở (hành động khi deploy) |

## 2. Heat map

### 2.1 Baseline (2026-11-20, trước P0)

| P \ I | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| **5** | | | R-13 | R-01 | |
| **4** | | | R-12, R-17 | R-04, R-05, R-08, R-15 | R-02 |
| **3** | | R-20, R-25 | R-22 | R-07, R-09, R-10, R-14, R-23 | R-03, R-06, R-19 |
| **2** | | | R-18 | R-21, R-24 | R-11, R-16 |
| **1** | | | | | |

### 2.2 Rủi ro còn mở sau P0 + P1 (khi hai nhánh được merge)

Không gồm các rủi ro trạng thái "Đã sửa + kiểm thử". R-09 được giữ với xác suất tồn dư thấp hơn vì chưa có kiểm thử tự động với Stripe thật.

| P \ I | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| **5** | | | | R-01 | |
| **4** | | | R-17, R-27 | R-15, R-26 | |
| **3** | | R-25 | | R-23 | R-19 |
| **2** | | | R-18, R-22 | R-09 (tồn dư) | R-16 |
| **1** | | | | | |

## 3. Bằng chứng đóng rủi ro P0 (2026-11-20)

| ID | Bằng chứng |
|---|---|
| R-02 | `CsrfCookieFilterTest` (5 test): `Set-Cookie` không còn `HttpOnly`; POST thiếu/sai token → 403. **Chưa kiểm thử trên browser thật.** |
| R-03 | `DayUseOverlapIntegrationTest` (8 test, JPQL thật trên H2) + `BookingIntegrityIT.exclusionConstraintCoversDayUse` (PostgreSQL 16) |
| R-04 | `PaymentServiceTest.refundAfterPolicyCancellationIsNotDoubleCounted`, `bookingRefundSumsAcrossPayments` |
| R-05 | `NoEntityLeakIntegrationTest`: **tái hiện 500 LazyInitializationException trên 5/5 nhóm endpoint trước khi sửa** (kể cả `GET /api/v1/bookings` của khách); sau sửa 5/5 xanh, không lộ số tài khoản hoàn tiền / raw gateway / password hash |
| R-06 | `BookingIntegrityIT` (5 test, PostgreSQL 16.6): overlap bị DB từ chối khi bỏ qua service; 20 request đồng thời → đúng 1 booking |
| R-07 | `PostgresMigrationIT`: V1→V13 + Hibernate `validate` pass trên PostgreSQL 16.6 (drift Integer↔BIGINT **không** làm validate fail; vẫn để nợ TD ở P2). IT này đã bắt lỗi thật trong V13 (biến PL/pgSQL `overlaps` là từ khóa SQL) trước khi lên CI |
| R-24 | Chạy tay trên PostgreSQL 16.6: rollback `U13` → chèn booking trùng → V13 dừng với `1<->12` + HINT → xóa bản ghi → V13 áp lại thành công |

## 4. Bằng chứng đóng rủi ro P1 (2026-11-20)

Nhánh `feature/p1-security-payment`. Kết quả chạy local: 226 unit/H2 test xanh (`./mvnw -B test`), 10 PostgreSQL IT xanh trên PostgreSQL 16.6 (`./mvnw -B verify` với `IT_JDBC_URL`). Frontend (tsc/vitest/build) chỉ được kiểm chứng trên CI.

| ID | Bằng chứng |
|---|---|
| R-08 | `PaymentServiceTest$ReconciliationAndManualRefundTests` (7 test: refund không phải thẻ → `PENDING_MANUAL`, không gọi gateway, tổng không đổi; refund đang chờ chặn over-refund; xác nhận cần `bankReference`; không xác nhận được refund của payment khác; hủy yêu cầu → `FAILED`); `StripePaymentAdapterTest` (adapter trả `false`, `not_refunded` thay vì success giả khi không có charge Stripe thật); `ManualRefundIT` (PostgreSQL: `PENDING_MANUAL` → xác nhận → `REFUNDED`, CHECK V14) |
| R-09 | Đã hiện thực trong `PaymentApi.createStripeCheckout` + `PaymentService.findReusableIntent/supersedeCheckoutPayment` + `StripePaymentAdapter.retrieveCheckoutSession/expireCheckoutSession/retrieveReusableClientSecret`. **Không có kiểm thử tự động**: các nhánh này chỉ chạy khi Stripe thật được cấu hình. Cần chạy thử với Stripe test mode: gọi `/payments/stripe-checkout` 2 lần → cùng `sessionId`, `reused=true`; sửa booking `PENDING` rồi gọi lại → session cũ bị expire, payment cũ `FAILED` "Superseded" |
| R-10 | `SepayWebhookApiTest.recordsAmountMismatchAsFlaggedPaymentWithoutConfirmingBooking`, `recordsLateTransferWithoutRevivingExpiredBooking`; `PaymentServiceTest$ReconciliationAndManualRefundTests.webhookAmountMismatchIsRecordedNotThrown` (Stripe webhook không còn throw/rollback) |
| R-11 | `AdminCredentialValidatorTest` (6 test: `default`/`postgres` không credentials → không tạo admin; `dev` → `admin`; credentials cấu hình được dùng ngoài dev; `prod` thiếu credentials → fail-fast; `prod` mật khẩu yếu → fail-fast) |
| R-12 | `UserServiceVerificationPolicyTest` (3 test: login tài khoản chưa verify bị từ chối khi cờ tắt; đăng ký `REGISTRATION_UNAVAILABLE`; opt-in tường minh → auto-verify) |
| R-13 | Code review: `frontend/src/features/auth/AuthPages.tsx` đọc `NEXT_PUBLIC_ENABLE_DEMO_LOGIN` + `NEXT_PUBLIC_DEMO_LOGIN_PASSWORD`; literal mật khẩu đã xóa khỏi frontend (vẫn còn trong seed `DataInitializer.java:552`). Kiểm chứng build/type trên CI (`frontend-check`). Chưa có test UI |
| R-14 | `security/Authz.java` thay mọi check phân quyền dạng chuỗi (còn lại `UserService.java:227` và `EmailService.java:538` là so sánh dữ liệu, không phải phân quyền người gọi). Kiểm thử: các case STAFF/MANAGER/ADMIN sẵn có trong `BookingFlowIntegrationTest` + `NoEntityLeakIntegrationTest` (ADMIN đọc payment). **Chưa có test tham số hóa đầy đủ endpoint × role (AC-06)** — để P6 |
| R-20 | `BookingPolicyIntegrationTest.expiredHoldReleasesPromotionUse` (H2, JPQL thật: `currentUses` 1 → 0 khi hold hết hạn) |
| R-21 | `BookingPolicyIntegrationTest` (`pendingModificationReprices`, `paidModificationThatChangesPriceIsRejected`, `minimumStayEnforced`, `maximumStayEnforced`); `BookingServiceTest$ModificationTests` |
| Bổ trợ | `PricingPolicyTest` (6 test: phí/VAT sau giảm giá, cap giảm giá, phụ thu khách VND, cuối tuần +15%, day-use 50%, phụ thu cấu hình được); `BookingFlowIntegrationTest.testDuplicateSubmitIsIdempotent` (cùng khách gửi lại → cùng booking id); `RateLimitFilterTest` (4 test: bucket nhạy cảm riêng, bucket chung, static/health không bị giới hạn và không tạo key, phân biệt method); `PostgresMigrationIT.manualRefundWorkflowSchemaExists` + rollback `U14` (chặn khi còn `PENDING_MANUAL`) → áp lại V14 thành công trên PostgreSQL 16.6 |

## 5. Rủi ro còn tồn tại sau P0 + P1

Sau khi `feature/p0-hardening` và `feature/p1-security-payment` merge:

| Mức | Rủi ro | Phase |
|---|---|---|
| Critical | R-01 (Gemini key — chờ repo owner rotate), R-26 (giá USD trên prod), R-15 (nginx TLS/header), R-19 (port DB/Redis/RabbitMQ public) | Ngay khi deploy / P5 |
| High | R-17 (monitoring), R-27 (đăng ký tắt khi chưa có SMTP), R-23 (refactor P3), R-16 (backup) | P5 / khi deploy / P3 |
| Medium | R-09 tồn dư (cần kiểm thử Stripe test mode), R-18 (rate limit/session/lockout in-memory, scheduler không ShedLock), R-22 (Q8/Q10), R-25 | P3/P5/P6 |

*(Bản ghi lịch sử)* Sau P0, các rủi ro Critical/High còn mở là: R-01, R-08, R-13, R-15, R-19, R-09, R-10, R-12, R-14, R-17 — đầu vào của P1.
