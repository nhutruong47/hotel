# P1 Release Notes — Security & Payment hardening

| Thuộc tính | Giá trị |
|---|---|
| Nhánh | `feature/p1-security-payment` (tách từ `feature/p0-hardening`) |
| Ngày | 2026-11-20 |
| Quyết định nghiệp vụ | [ADR-0009](../adr/0009-business-policy-decisions.md) (Q1–Q7, Q9, Q11) |
| Migration | `V14__payment_review_and_manual_refunds.sql` (additive) + rollback `db/rollback/U14__…sql` |
| Kiểm thử local | 226 unit/H2 test xanh; 10 PostgreSQL IT xanh (PostgreSQL 16.6); rollback U14 → áp lại V14 thành công. Frontend chỉ kiểm chứng qua CI (`tsc`, vitest, `next build`) |
| Rủi ro liên quan | [risk-register.md](../reports/risk-register.md) R-08..R-14, R-20, R-21 (đã sửa), R-09 (chưa test tự động), R-26, R-27 (hành động khi deploy) |

Người đọc: người vận hành/deploy, ADMIN/MANAGER, frontend developer.

---

## 1. Thay đổi cho người vận hành (tóm tắt)

| Lĩnh vực | Thay đổi | Ảnh hưởng vận hành |
|---|---|---|
| Tiền tệ | Hệ thống chỉ dùng **VND**. Giá seed đổi sang VND (×25.000). `config/CurrencySanityCheck.java` log `WARN CURRENCY CHECK …` khi khởi động nếu có villa giá < 100.000 VND. Stripe từ chối tạo phiên thanh toán khi tổng < 10.000 VND (`PRICE_CONFIGURATION_INVALID`) | **Phải đổi giá villa sang VND** nếu môi trường đang dùng giá thang USD (§4 bước 3) |
| Giá | `booking/service/PricingPolicy.java`: phí dịch vụ 8% trên giá **sau** giảm giá; VAT 10% trên (giá sau giảm + phí dịch vụ); phụ thu khách thứ 3 trở đi mặc định 625.000 VND/khách/đêm | Booking **mới** có tổng tiền khác nhẹ so với trước; booking cũ giữ nguyên |
| Đối soát | Tiền trễ/trùng/sai số tiền luôn được ghi `PAID` + cờ `review_required` + lý do; ADMIN nhận thông báo in-app | ADMIN theo dõi hàng đợi "Needs reconciliation" trên tab Analytics/Payments |
| Hoàn tiền | Payment không phải thẻ Stripe thật (SePay, chuyển khoản, manual, mock) → refund `PENDING_MANUAL`. Chỉ khi ADMIN chuyển khoản và xác nhận kèm **mã giao dịch ngân hàng** thì tiền mới được ghi là đã hoàn | ADMIN dùng mục "Manual refunds awaiting bank transfer" (hiển thị tài khoản ngân hàng khách đã cung cấp). SLA mục tiêu 5 ngày làm việc |
| Admin mặc định | `admin/admin123` chỉ được tạo với profile `dev` tường minh. Profile `default`/`postgres` không có `APP_ADMIN_USERNAME`/`APP_ADMIN_PASSWORD` → không tạo admin (log WARN). `prod` vẫn fail-fast | Môi trường local chạy profile `postgres` cần đặt credentials admin hoặc thêm profile `dev` |
| Xác minh email | Tự động xác minh khi không có email provider chỉ khi bật `APP_AUTH_AUTO_VERIFY_WITHOUT_EMAIL=true`. Mặc định: email tắt → **đăng ký trả `REGISTRATION_UNAVAILABLE`** | Cấu hình SMTP + `APP_EMAIL_ENABLED=true` (khuyến nghị) — R-27 |
| Rate limit | Chỉ áp `/api/**`. Bucket chung (`APP_RATE_LIMIT_REQUESTS`, prod 120/phút/IP) + bucket nhạy cảm (`APP_RATE_LIMIT_SENSITIVE_REQUESTS`, mặc định 10/phút/IP) cho `POST` login, register, forgot/reset password, resend verification, AI recommend, contact, tạo booking. Vẫn in-memory (1 instance) | Ảnh/`/uploads` không còn bị giới hạn nhầm |
| Lưu trữ | Audit log `PAYMENT_*` giữ 10 năm, còn lại 180 ngày (job 00:00). Thông tin ngân hàng hoàn tiền bị xóa 180 ngày sau khi hủy nếu không còn refund `PENDING_MANUAL` (job 00:30) | Không cần thao tác |
| Hóa đơn PDF | In bằng VND: Subtotal, Discount, Service fee, VAT, Total (+ Refunded). Sửa lỗi trừ giảm giá 2 lần ("Net Total") | — |
| AI concierge | Prompt báo giá VND và phụ thu theo cấu hình | — |
| Frontend | Nút demo login ẩn mặc định; phiên hết hạn trên server được phản ánh ngay; trang đặt phòng hiển thị giá do server tính (kể cả mã giảm giá) | — |
| CI | Job `secret-scan` (gitleaks 8.21.2, quét working tree theo `.gitleaks.toml`); `.github/dependabot.yml` (maven, npm, GitHub Actions) | PR Dependabot hằng tuần vào `develop` |

## 2. Thay đổi API

### 2.1 Endpoint mới

| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| GET | `/api/v1/payments?reviewRequired=true` | MANAGER+ | Hàng đợi payment cần đối soát (`reviewRequired=false` hoặc bỏ trống → danh sách `PAID` như cũ) |
| GET | `/api/v1/payments/refunds/pending` | ADMIN | Refund `PENDING_MANUAL` (cũ nhất trước), **kèm** `refundTo { bankName, accountNumber, accountName }` của booking |
| GET | `/api/v1/payments/{id}/refunds` | MANAGER+ | Lịch sử refund của một payment (không kèm thông tin ngân hàng) |
| POST | `/api/v1/payments/{id}/refunds/{refundId}/confirm` | ADMIN | Body `{ "bankReference": "VCB-…" }` (bắt buộc, ≤ 128 ký tự). `PENDING_MANUAL → SUCCEEDED`; cập nhật tổng hoàn payment/booking. Lặp lại với cùng `bankReference` là idempotent |
| POST | `/api/v1/payments/{id}/refunds/{refundId}/reject` | ADMIN | Body tùy chọn `{ "reason": "…" }`. `PENDING_MANUAL → FAILED`; tổng hoàn không đổi |

### 2.2 Endpoint thay đổi

| Endpoint | Trước | Sau |
|---|---|---|
| `POST /payments/{id}/refund` | `data: { payment, message }` | `data: { payment, refund, manual, message }`; `manual=true` khi refund ở `PENDING_MANUAL` (message hướng dẫn chuyển khoản rồi xác nhận). `refund` = `PaymentRefundResponse` (không có thông tin ngân hàng) |
| `GET /bookings/pricing-preview` | `subtotal, serviceFee, taxAmount, total` | Thêm query `code` (voucher/promotion); response thêm `discount`, `currency: "VND"`; khi có `code`: `code`, `codeValid`; mã không hợp lệ: `codeValid=false`, `codeError`, `codeMessage` và giá trả về **không** có giảm giá |
| `GET /payments` | ADMIN | MANAGER+ (chỉ đọc). Complete/fail/refund vẫn ADMIN |
| `GET /bookings/{id}`, `/{id}/timeline`, `/{id}/invoice`; `GET /payments/booking/{bookingId}` | Chủ booking hoặc ADMIN | Chủ booking hoặc **STAFF+** (hàng đợi vận hành theo ma trận RBAC) |
| `POST /bookings` | Gửi lại cùng villa/ngày → 422 `ROOM_UNAVAILABLE` | Cùng khách gửi lại đúng villa/ngày/số khách khi hold còn hiệu lực → **200 với booking cũ** (natural idempotency). Khách khác vẫn 422 |
| `PATCH /bookings/{id}` | Sửa tự do (bỏ phụ thu khách, không kiểm tra bảo trì) | Tái định giá đầy đủ; kiểm tra availability, bảo trì, min/max stay; booking `PAID` đổi tổng → 422 `MODIFICATION_REQUIRES_SUPPORT` |
| `POST /payments/stripe-checkout` | Mỗi lần gọi tạo phiên mới | Reuse phiên còn `open` cùng số tiền (`reused: true`); phiên cũ bị expire nếu tổng đã đổi; phiên hết hạn sau 31 phút |
| `POST /payments/intent` | Mỗi lần tạo intent mới | Reuse intent `PENDING` cùng số tiền còn confirm được |
| `POST /payments/webhook/sepay` | Sai số tiền → 200 `success:false`, **không lưu** | Sai số tiền → 200 `success:false, requiresReview:true, transactionRef`, **lưu** payment `PAID` + `review_required` (booking không được xác nhận) |
| `POST /payments/webhook/stripe` | Sai số tiền → lỗi 400, Stripe retry mãi | Ghi payment `PAID` + `review_required`, trả 200 |
| `PaymentResponse` (mọi response có payment) | — | Thêm `reviewRequired`, `reviewReason` |

### 2.3 Mã lỗi mới (HTTP 422 trừ khi ghi khác)

| Code | Khi nào |
|---|---|
| `MIN_STAY_NOT_MET` / `MAX_STAY_EXCEEDED` | Booking qua đêm ngoài `[minimum_stay, maximum_stay]` của villa (day-use được miễn) |
| `MODIFICATION_REQUIRES_SUPPORT` | Sửa booking `PAID` làm đổi tổng tiền |
| `HOLD_EXPIRED` | Sửa booking `PENDING_PAYMENT` khi hold đã hết hạn |
| `PRICE_CONFIGURATION_INVALID` | Tạo phiên/intent Stripe thật khi tổng < 10.000 VND (dấu hiệu giá còn theo USD) |
| `REGISTRATION_UNAVAILABLE` | Đăng ký khi email tắt và chưa bật auto-verify |
| `BANK_REFERENCE_REQUIRED` | Xác nhận refund thủ công thiếu/quá dài mã giao dịch |
| `REFUND_ALREADY_CONFIRMED` | Xác nhận lại refund với mã giao dịch khác |
| `RATE_LIMIT_EXCEEDED` (429) | Vượt bucket chung hoặc bucket nhạy cảm |

## 3. Biến môi trường mới

| Biến | Mặc định | Phạm vi | Ghi chú |
|---|---|---|---|
| `APP_PRICING_EXTRA_GUEST_FEE` | `625000` | Backend | VND/khách/đêm từ khách thứ 3 (ADR-0009 Q6) |
| `APP_AUTH_AUTO_VERIFY_WITHOUT_EMAIL` | `false` (`true` ở profile `dev`, `postgres`, test) | Backend | Chỉ bật khi chấp nhận tài khoản chưa xác minh email |
| `APP_RATE_LIMIT_SENSITIVE_REQUESTS` | `10` (dev `50`) | Backend | Số request/phút/IP cho endpoint nhạy cảm |
| `NEXT_PUBLIC_ENABLE_DEMO_LOGIN` | không đặt (= tắt) | Frontend, **build-time** | Chỉ `true` ở local/staging |
| `NEXT_PUBLIC_DEMO_LOGIN_PASSWORD` | không đặt | Frontend, build-time | Mật khẩu tài khoản demo đã seed; nút demo chỉ hiện khi cả hai biến được đặt |

> **Lưu ý compose:** `docker-compose.yml` đã truyền ba biến backend ở trên (mặc định 625000 / false / 10). Các biến `NEXT_PUBLIC_*` không có trong `build.args` của frontend nên demo login luôn tắt trong image build bằng compose — đúng mong muốn cho prod.

## 4. Checklist deploy

1. **Backup**: `pg_dump -Fc` database production, ghi lại tên file ([database-design.md §9](../architecture/database-design.md#9-quy-trình-release-thay-đổi-db)).
2. **Deploy bản mới**: Flyway tự áp V14 khi khởi động (`prod`). V14 chỉ thêm cột/constraint, không sửa dữ liệu. Nếu cần quay lui: xử lý hết refund `PENDING_MANUAL` rồi chạy `db/rollback/U14__payment_review_and_manual_refunds.sql` (mất dữ liệu `bank_reference`/`confirmed_*`/cờ review).
3. **Kiểm tra tiền tệ (R-26)**: tìm `CURRENCY CHECK` trong log khởi động. Nếu có cảnh báo, **kiểm tra trước**, rồi mới chuyển đổi:

   ```sql
   -- (a) Xem villa nào còn giá thang USD
   SELECT id, room_number, price_per_night FROM rooms WHERE price_per_night < 100000 ORDER BY id;
   -- (b) Voucher cố định / promotion cũng có thể đang theo USD
   SELECT id, code, amount FROM vouchers WHERE is_percent = false AND amount < 100000;
   SELECT id, promo_code, discount_amount, minimum_booking_amount FROM promotions
    WHERE discount_amount BETWEEN 1 AND 99999
       OR minimum_booking_amount BETWEEN 1 AND 99999;   -- 0 = không có mức tối thiểu

   -- (c) Chỉ khi đã xác nhận các giá trên là USD: đổi bằng tỷ lệ của ADR-0009 (25.000 VND/USD)
   BEGIN;
   UPDATE rooms SET price_per_night = price_per_night * 25000 WHERE price_per_night < 100000;
   -- kiểm tra lại kết quả (SELECT ở bước a) rồi COMMIT; nếu sai thì ROLLBACK
   COMMIT;
   ```

   Môi trường chạy profile `seed` sẽ tự ghi đè giá các villa seed bằng giá VND mới khi khởi động (`DataInitializer.upsertVilla`); villa do ADMIN tạo thêm vẫn cần chuyển đổi thủ công. Áp dụng tương tự cho voucher/promotion nếu bước (b) có kết quả và đã được xác nhận.
4. **Email (R-27)**: cấu hình SMTP và đặt `APP_EMAIL_ENABLED=true` (khuyến nghị). Nếu chưa có SMTP và chấp nhận rủi ro tài khoản chưa xác minh: `APP_AUTH_AUTO_VERIFY_WITHOUT_EMAIL=true`. Nếu không làm gì, đăng ký mới sẽ bị từ chối.
5. **Admin**: đảm bảo `APP_ADMIN_USERNAME`/`APP_ADMIN_PASSWORD` đã đặt (mật khẩu ≥ 12 ký tự, đủ loại ký tự).
6. **Rotate Gemini key (R-01)** trên Google AI Studio nếu chưa làm; cập nhật `GEMINI_API_KEY`.
7. **Tài khoản demo**: nếu môi trường chạy profile `seed`, đổi mật khẩu các tài khoản demo đã seed (mật khẩu mặc định nằm trong mã nguồn `DataInitializer`, TD-47).
8. **Smoke test**: đặt 1 booking thử → `pricing-preview` khớp tổng booking; với Stripe test mode gọi `/payments/stripe-checkout` 2 lần → cùng `sessionId` (`reused: true`) — đây là phần R-09 chưa có test tự động; tạo refund cho payment chuyển khoản → thấy trong "Manual refunds awaiting bank transfer" → xác nhận với mã giao dịch.

## 5. Thay đổi người dùng sẽ nhận thấy

| Ai | Thay đổi |
|---|---|
| Khách | Tổng tiền booking mới khác nhẹ: VAT nay tính trên (giá sau giảm + phí dịch vụ) và phí dịch vụ tính trên giá sau giảm (Q7). Giá hiển thị trên trang đặt phòng do server tính, kể cả khi áp mã |
| Khách | Không tự sửa được booking **đã thanh toán** nếu thay đổi làm đổi tổng tiền — nhận thông báo liên hệ hỗ trợ hoặc hủy và đặt lại (Q3). Booking chờ thanh toán vẫn sửa được và được tính lại giá |
| Khách | Booking qua đêm phải tuân theo số đêm tối thiểu/tối đa của villa (mặc định 1–30) |
| Khách | Bấm "Đặt" hai lần không còn báo lỗi "đã được đặt" — nhận lại đúng booking đang giữ chỗ |
| Khách | Nút đăng nhập demo (Digital Keycard, Quick fill) không còn hiện trên production |
| Khách | Khi đăng ký trên môi trường chưa cấu hình email: nhận thông báo đăng ký tạm thời không khả dụng |
| ADMIN | Refund cho thanh toán chuyển khoản không còn "thành công" ngay: phải chuyển khoản và xác nhận mã giao dịch. Payment cần đối soát có nhãn "Needs reconciliation" kèm lý do; có thông báo in-app "Thanh toán cần đối soát" / "Yêu cầu hoàn tiền thủ công" |
| MANAGER | Xem được danh sách payment (trước đây 403) |
| STAFF | Xem được chi tiết/timeline/hóa đơn/payment của mọi booking (trước đây chỉ ADMIN) |
