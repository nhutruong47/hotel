# Risk Register — Nhu Villas

| Thuộc tính | Giá trị |
|---|---|
| Ngày tạo | 2026-11-20 |
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
| R-08 | Refund "thành công" giả cho SePay/manual → khách không nhận tiền nhưng hệ thống ghi đã hoàn | `StripePaymentAdapter.java:186-198` | 4 | 4 | 16 | Backend | Trạng thái `MANUAL_PENDING` + quy trình chuyển khoản tay có xác nhận | Mở (P1) |
| R-09 | Khách bị trừ tiền 2 lần do tạo nhiều Stripe Session/Intent | `PaymentApi.java` `/stripe-checkout`, `/intent` | 3 | 4 | 12 | Backend | Reuse session/intent đang mở theo booking; idempotency key Stripe | Mở (P1) |
| R-10 | Tiền SePay sai số tiền không được lưu → mất dấu, khiếu nại | `SepayWebhookApi.java` | 3 | 4 | 12 | Backend | Lưu payment `AMOUNT_MISMATCH` + hàng đợi đối soát | Mở (P1) |
| R-11 | Tài khoản admin mặc định `admin123` khi quên set profile | `AdminCredentialValidator.java:56,163-170` | 2 | 5 | 10 | Backend | Fail-fast nếu thiếu `APP_ADMIN_PASSWORD` ngoài profile dev tường minh | Mở (P1) |
| R-12 | Email verification bị bỏ qua trên prod | `UserService.java:170-172` | 4 | 3 | 12 | Backend | Bắt buộc email provider ở prod hoặc chặn đăng nhập chưa verify | Mở (P1) |
| R-13 | Demo login có password public trên site production | `frontend/src/features/auth/AuthPages.tsx:337,352,447` | 5 | 3 | 15 | Frontend | Ẩn sau feature flag chỉ bật ở dev/staging; đổi mật khẩu tài khoản demo | Mở (P1) |
| R-14 | RBAC thủ công sai/sót quyền | 16 chỗ `"ADMIN".equals` | 3 | 4 | 12 | Backend | `@PreAuthorize` + `RoleHierarchy`; test phân quyền theo ma trận | Mở (P1) |
| R-15 | nginx không TLS/mất security header/CORS `*` | `nginx.conf:54,92,121-124` | 4 | 4 | 16 | DevOps | TLS (Let's Encrypt), header `always` qua include, bỏ CORS ở nginx | Mở (P1/P5) |
| R-16 | Không backup → mất toàn bộ dữ liệu booking/payment khi hỏng volume | — | 2 | 5 | 10 | DevOps | `pg_dump` hằng ngày + WAL/offsite + restore drill hằng tháng | Mở (P5) |
| R-17 | Không monitoring/alert → sự cố (outbox kẹt, webhook lỗi) không ai biết | Actuator `health,info` | 4 | 3 | 12 | DevOps | Prometheus/Grafana, alert outbox backlog, webhook 4xx/5xx, DB pool | Mở (P5) |
| R-18 | Không scale ngang (state in-memory, scheduler chạy trùng) | `RateLimitFilter`, `LoginAttemptService`, `BookingScheduler` | 2 | 3 | 6 | Backend | Spring Session Redis, Bucket4j-Redis, ShedLock | Mở (P1/P3) |
| R-19 | DB/Redis/RabbitMQ lộ port ra Internet với mật khẩu mặc định | `docker-compose.yml:19,32-33,48-52` | 3 | 5 | 15 | DevOps | Bỏ `ports` ở prod compose, secrets bắt buộc, firewall VPS | Mở (P5) |
| R-20 | Promotion cạn lượt sai do không hoàn `currentUses` | `BookingService.java:307` | 3 | 2 | 6 | Backend | Release khi expire/cancel; test | Mở (P1) |
| R-21 | `modifyBooking` làm sai tổng tiền booking PAID/đổi sang ngày bảo trì | `BookingService.java:610-660` | 2 | 4 | 8 | Backend | Viết lại theo pricing + availability đầy đủ; chênh lệch → payment/refund | Mở (P1) |
| R-22 | Nghiệp vụ day-use/late payment/housekeeping chưa được xác nhận → thiết kế sai | [business-analysis.md](../business/business-analysis.md) mục [CẦN XÁC NHẬN] | 3 | 3 | 9 | BA | Workshop xác nhận với chủ villa trước P4 | Mở |
| R-23 | Refactor module lớn làm gãy hành vi đang chạy | P3 | 3 | 4 | 12 | Backend/QA | Strangler từng module, ArchUnit, regression suite xanh trước/sau | Mở (P3) |
| R-24 | Dữ liệu cũ vi phạm constraint mới (V13) làm migration fail khi deploy | Prod DB | 2 | 4 | 8 | DBA | DO-block pre-check báo id vi phạm; chạy preflight query trên bản sao prod trước | **Đã sửa + kiểm thử (feature/p0-hardening, chờ merge)** |
| R-25 | Bus factor/phụ thuộc AI agent: tài liệu `.agents/` lỗi thời gây hướng dẫn sai | `.agents/skills/deploy`, `run-locally` | 3 | 2 | 6 | Repo owner | Đánh dấu stale trong `docs/README.md`; cập nhật ở P5 | Mở |

## 2. Heat map

| P \ I | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|
| **5** | | | R-13 | R-01 | |
| **4** | | | R-12, R-17 | R-04, R-05, R-08, R-15 | R-02 |
| **3** | | R-20, R-25 | R-22 | R-07, R-09, R-10, R-14, R-23 | R-03, R-06, R-19 |
| **2** | | | R-18 | R-21, R-24 | R-11, R-16 |
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

## 4. Rủi ro còn tồn tại sau P0

Sau khi nhánh `feature/p0-hardening` merge, các rủi ro Critical/High còn mở: R-01 (chờ rotate), R-08, R-13, R-15, R-19, R-09, R-10, R-12, R-14, R-17. Đây là đầu vào cho phase P1-Security và P1-Payment.
