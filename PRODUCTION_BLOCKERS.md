# PRODUCTION_BLOCKERS

## Critical

### 1. CSRF làm vỡ toàn bộ mutation frontend
- Bằng chứng: `CsrfCookieFilter` yêu cầu `X-XSRF-TOKEN` tại `hotel/src/main/java/com/hsf/hotel/security/CsrfCookieFilter.java:58`; API client không gửi header tại `frontend/src/shared/api/client.ts:28`.
- Phải sửa: Client echo cookie token hoặc thay bằng Spring CSRF chuẩn. Test create booking, profile update, wishlist toggle, admin mutation.

### 2. Stripe webhook không production-ready
- Bằng chứng: `GatewaySignatureFilter` áp `/api/v1/payments/webhook` tại `GatewaySignatureFilter.java:39`; Stripe endpoint ở `PaymentApi.java:313`; controller còn nhánh no-signature tại `PaymentApi.java:324`.
- Phải sửa: Stripe signature verification bắt buộc, generic HMAC không chặn Stripe, webhook E2E.

### 3. Flyway V2 migration fail trên schema thật
- Bằng chứng: V2 dùng `Booking` tại `V2__add_performance_indexes.sql:7`, trong khi V1 tạo `bookings` tại `V1__init.sql:176`; V2 dùng `booking_user_id` tại dòng 25 nhưng V1 `payments` không có cột này tại `V1__init.sql:276`.
- Phải sửa: Rewrite V2 theo table/column thật, chạy migration clean DB trong CI.

### 4. Booking status frontend/backend lệch nghiêm trọng
- Bằng chứng: Backend enum tại `BookingStatus.java:3`; frontend hardcode `PENDING/AWAITING_PAYMENT/CONFIRMED/REJECTED` tại `AdminDashboardPage.tsx:93`, `BookingsPage.tsx:124`, `NotificationsPage.tsx:72`.
- Phải sửa: Dùng status enum backend duy nhất, sửa UI actions/filter/notification/i18n.

## High

### 5. Payment confirmation không xác thực amount
- Bằng chứng: `BookingService.confirmPayment` nhận amount tại `BookingService.java:353` nhưng không compare với `booking.totalPrice`.
- Phải sửa: Validate exact/outstanding amount trước khi chuyển `PAID`.

### 6. Contact ticket PII có IDOR
- Bằng chứng: `ContactApi.getContact` tại `ContactApi.java:58`; SecurityConfig chỉ admin-only `/api/v1/admin/**`, còn `/api/v1/**` authenticated tại `SecurityConfig.java:116`.
- Phải sửa: Move admin contact APIs dưới `/api/v1/admin` hoặc require admin.

### 7. Account support form không gửi được ticket
- Bằng chứng: `SupportPage.tsx:72` gọi `api.post('/api/v1/contact')`, client prefix `/api/v1` tại `client.ts:1`.
- Phải sửa: Dùng `API_PATHS.contact`.

### 8. Email uniqueness/profile email update chưa an toàn
- Bằng chứng: V1 `users.email` không unique tại `V1__init.sql:19`; `ProfileService.updateProfile` set email không check duplicate tại `ProfileService.java:79`.
- Phải sửa: Unique index email + validation/catch duplicate.

### 9. Review eligibility sai nghiệp vụ
- Bằng chứng: Backend cho review khi `PAID` hoặc `CHECKED_IN` tại `ReviewService.java:44`.
- Phải sửa: Chỉ cho sau `CHECKED_OUT/COMPLETED`.

### 10. Legacy source tree song song với backend chính
- Bằng chứng: Legacy controllers dưới `src/main/java/com/hsf/hotel/controller`, REST APIs dưới `hotel/src/main/java/com/hsf/hotel/api`.
- Phải sửa: Không package/deploy legacy hoặc archive thành module riêng.
