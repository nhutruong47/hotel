# PAYMENT_AUDIT

> Historical audit snapshot. PAYMENT-001 through PAYMENT-004 were addressed
> by the current provider-specific webhook, payment-correlation, and browser
> flow implementation. See `docs/BOOKING_LIFECYCLE_AND_RBAC.md` for the
> accepted payment rules.

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Manual payment | 65% | User tạo payment pending; admin confirm có nhưng UI chưa đầy đủ. | Back-office payment list/action, amount validation. |
| Stripe Checkout | 45% | Webhook bị filter chặn; client secret sai; mock path chưa hoàn chỉnh. | Sửa webhook, lưu Stripe session/paymentIntent, E2E webhook. |
| Refund | 55% | Refund API có; booking cancel chưa gọi refund gateway. | Refund policy service + gateway refund + audit. |
| Payment query | 60% | Admin GET chỉ trả PAID, không pagination/filter. | Pagination/status/date filters. |

## Bugs

### PAYMENT-001 - Critical - Stripe webhook bị GatewaySignatureFilter chặn trước controller
- Mô tả: Filter yêu cầu `X-Webhook-Timestamp` và `X-Webhook-Signature` cho mọi path bắt đầu `/api/v1/payments/webhook`; Stripe gửi `Stripe-Signature`.
- Nguyên nhân: Generic HMAC filter áp lên Stripe endpoint.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/security/GatewaySignatureFilter.java:39`; `hotel/src/main/java/com/hsf/hotel/security/GatewaySignatureFilter.java:64`; `hotel/src/main/java/com/hsf/hotel/api/PaymentApi.java:313`.
- Cách tái hiện: Gửi Stripe webhook chuẩn đến `/api/v1/payments/webhook/stripe` với `Stripe-Signature`, không có `X-Webhook-*`.
- Ảnh hưởng: Thanh toán Stripe không tự xác nhận booking.
- Hướng khắc phục: Exempt `/webhook/stripe` khỏi generic filter hoặc filter nhận Stripe signature riêng.

### PAYMENT-002 - Critical - Stripe webhook có nhánh parse không verify signature
- Mô tả: Nếu không có `Stripe-Signature`, controller parse event không verify.
- Nguyên nhân: Testing shortcut nằm trong production controller.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/PaymentApi.java:324`.
- Cách tái hiện: Disable generic filter hoặc gọi controller trong test, gửi payload không signature.
- Ảnh hưởng: Có thể giả mạo payment success nếu filter bị cấu hình sai/exempt rộng.
- Hướng khắc phục: Bắt buộc verify Stripe signature; mock webhook chỉ ở profile test/dev.

### PAYMENT-003 - High - `clientSecret` trả sai giá trị
- Mô tả: `/payments/intent` trả `clientSecret = payment.getIntentId()`.
- Nguyên nhân: `PaymentGateway.createIntent` contract chỉ trả intent id, không trả client secret.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/PaymentApi.java:299`.
- Cách tái hiện: Frontend Stripe.js dùng `clientSecret` để confirm payment; giá trị `pi_...` không phải `client_secret`.
- Ảnh hưởng: Custom Stripe payment flow không hoạt động.
- Hướng khắc phục: Adapter trả object gồm `intentId`, `clientSecret`; chỉ expose client secret.

### PAYMENT-004 - Medium - Booking success page cho user tự simulate payment
- Mô tả: UI có nút `simulatePayment` gọi `/bookings/{id}/payment` với ref `MOCK-PAY-*`.
- Nguyên nhân: Dev/test action còn trong production page.
- File/Class/Method: `frontend/src/features/booking/BookingSuccessPage.tsx:151`.
- Cách tái hiện: Tạo booking, vào success page, bấm simulate payment.
- Ảnh hưởng: User có thể tự chuyển booking sang paid qua API nếu endpoint vẫn cho phép.
- Hướng khắc phục: Remove nút khỏi production; payment confirmation user-facing phải qua gateway hoặc manual pending record.
