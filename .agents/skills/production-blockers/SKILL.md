---
description: List of known blockers for production deployment
---

# PRODUCTION_BLOCKERS

## Critical

### 1. CSRF lÃ m vá»¡ toÃ n bá»™ mutation frontend
- Báº±ng chá»©ng: `CsrfCookieFilter` yÃªu cáº§u `X-XSRF-TOKEN` táº¡i `hotel/src/main/java/com/hsf/hotel/security/CsrfCookieFilter.java:58`; API client khÃ´ng gá»­i header táº¡i `frontend/src/shared/api/client.ts:28`.
- Pháº£i sá»­a: Client echo cookie token hoáº·c thay báº±ng Spring CSRF chuáº©n. Test create booking, profile update, wishlist toggle, admin mutation.

### 2. Stripe webhook khÃ´ng production-ready
- Báº±ng chá»©ng: `GatewaySignatureFilter` Ã¡p `/api/v1/payments/webhook` táº¡i `GatewaySignatureFilter.java:39`; Stripe endpoint á»Ÿ `PaymentApi.java:313`; controller cÃ²n nhÃ¡nh no-signature táº¡i `PaymentApi.java:324`.
- Pháº£i sá»­a: Stripe signature verification báº¯t buá»™c, generic HMAC khÃ´ng cháº·n Stripe, webhook E2E.

### 3. Flyway V2 migration fail trÃªn schema tháº­t
- Báº±ng chá»©ng: V2 dÃ¹ng `Booking` táº¡i `V2__add_performance_indexes.sql:7`, trong khi V1 táº¡o `bookings` táº¡i `V1__init.sql:176`; V2 dÃ¹ng `booking_user_id` táº¡i dÃ²ng 25 nhÆ°ng V1 `payments` khÃ´ng cÃ³ cá»™t nÃ y táº¡i `V1__init.sql:276`.
- Pháº£i sá»­a: Rewrite V2 theo table/column tháº­t, cháº¡y migration clean DB trong CI.

### 4. Booking status frontend/backend lá»‡ch nghiÃªm trá»ng
- Báº±ng chá»©ng: Backend enum táº¡i `BookingStatus.java:3`; frontend hardcode `PENDING/AWAITING_PAYMENT/CONFIRMED/REJECTED` táº¡i `AdminDashboardPage.tsx:93`, `BookingsPage.tsx:124`, `NotificationsPage.tsx:72`.
- Pháº£i sá»­a: DÃ¹ng status enum backend duy nháº¥t, sá»­a UI actions/filter/notification/i18n.

## High

### 5. Payment confirmation khÃ´ng xÃ¡c thá»±c amount
- Báº±ng chá»©ng: `BookingService.confirmPayment` nháº­n amount táº¡i `BookingService.java:353` nhÆ°ng khÃ´ng compare vá»›i `booking.totalPrice`.
- Pháº£i sá»­a: Validate exact/outstanding amount trÆ°á»›c khi chuyá»ƒn `PAID`.

### 6. Contact ticket PII cÃ³ IDOR
- Báº±ng chá»©ng: `ContactApi.getContact` táº¡i `ContactApi.java:58`; SecurityConfig chá»‰ admin-only `/api/v1/admin/**`, cÃ²n `/api/v1/**` authenticated táº¡i `SecurityConfig.java:116`.
- Pháº£i sá»­a: Move admin contact APIs dÆ°á»›i `/api/v1/admin` hoáº·c require admin.

### 7. Account support form khÃ´ng gá»­i Ä‘Æ°á»£c ticket
- Báº±ng chá»©ng: `SupportPage.tsx:72` gá»i `api.post('/api/v1/contact')`, client prefix `/api/v1` táº¡i `client.ts:1`.
- Pháº£i sá»­a: DÃ¹ng `API_PATHS.contact`.

### 8. Email uniqueness/profile email update chÆ°a an toÃ n
- Báº±ng chá»©ng: V1 `users.email` khÃ´ng unique táº¡i `V1__init.sql:19`; `ProfileService.updateProfile` set email khÃ´ng check duplicate táº¡i `ProfileService.java:79`.
- Pháº£i sá»­a: Unique index email + validation/catch duplicate.

### 9. Review eligibility sai nghiá»‡p vá»¥
- Báº±ng chá»©ng: Backend cho review khi `PAID` hoáº·c `CHECKED_IN` táº¡i `ReviewService.java:44`.
- Pháº£i sá»­a: Chá»‰ cho sau `CHECKED_OUT/COMPLETED`.

### 10. Legacy source tree song song vá»›i backend chÃ­nh
- Báº±ng chá»©ng: Legacy controllers dÆ°á»›i `src/main/java/com/hsf/hotel/controller`, REST APIs dÆ°á»›i `hotel/src/main/java/com/hsf/hotel/api`.
- Pháº£i sá»­a: KhÃ´ng package/deploy legacy hoáº·c archive thÃ nh module riÃªng.

