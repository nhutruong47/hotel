# PROJECT_BUG_SUMMARY

| ID | Severity | Module | Lỗi | File chính |
|---|---|---|---|---|
| SEC-001 | Critical | Security/Frontend | CSRF filter yêu cầu header nhưng API client không gửi | `CsrfCookieFilter.java:58`, `client.ts:28` |
| PAYMENT-001 | Critical | Payment | Stripe webhook bị generic HMAC filter chặn | `GatewaySignatureFilter.java:39`, `PaymentApi.java:313` |
| PAYMENT-002 | Critical | Payment | Webhook Stripe có nhánh parse không verify signature | `PaymentApi.java:324` |
| DB-001 | Critical | Database | V2 migration dùng tên bảng sai | `V2__add_performance_indexes.sql:7` |
| DB-002 | Critical | Database | V2 reference cột không tồn tại | `V2__add_performance_indexes.sql:25` |
| BOOKING-001 | Critical | Booking/Admin/Notifications | Frontend dùng status không tồn tại backend | `BookingStatus.java:3`, `AdminDashboardPage.tsx:93` |
| PAYMENT-003 | High | Payment | `clientSecret` trả intent id, không phải client secret | `PaymentApi.java:299` |
| BOOKING-003 | High | Booking/Payment | Confirm payment không kiểm tra amount khớp total | `BookingService.java:353` |
| CONTENT-001 | High | Contact/Frontend | Account support gọi `/api/v1/api/v1/contact` | `SupportPage.tsx:72` |
| CONTENT-002 | High | Contact | Contact API thiếu DTO validation | `ContactApi.java:22` |
| CONTENT-003 | High | Contact/Security | User authenticated có thể đọc contact ticket | `ContactApi.java:58` |
| REVIEW-001 | High | Reviews | Backend cho review trước checkout | `ReviewService.java:44` |
| ADMIN-002 | High | Admin/User | Update role không allowlist | `UserApi.java:93` |
| DB-003 | High | Database/Auth | `users.email` không unique | `V1__init.sql:19` |
| PROFILE-001 | High | Profile | Đổi email không kiểm tra trùng | `ProfileService.java:79` |
| LEGACY-001 | High | Legacy | Hai source tree cùng package và flow song song | `src/main/java/...`, `hotel/src/main/java/...` |
| AUTH-001 | Medium | Auth | Resend verification leak email tồn tại | `UserService.java:109` |
| AUTH-003 | Medium | Auth/User | `/users/me` đổi password không cần current password | `UserApi.java:57` |
| ROOM-001 | High | Rooms/Booking | Overlap date chặn checkout/checkin cùng ngày | `BookingRepository.java:60` |
| ADMIN-003 | Medium | Admin/User | User delete hard-delete | `UserApi.java:138` |
| NOTIF-001 | High | Notifications | Frontend không dùng NotificationApi | `NotificationsPage.tsx:145` |
| AI-002 | Medium | AI | Blocking AI request/retry sleep trong servlet thread | `GeminiService.java:118` |
| UPLOAD-001 | High | Upload | Cho phép SVG nhưng sanitizer không decode SVG | `FileStorageService.java:24` |
| FE-004 | Medium | Frontend/Deploy | Rewrite API hardcoded localhost | `next.config.js:5` |
