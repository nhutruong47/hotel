# PROJECT_MISSING_FEATURES

> Historical discovery snapshot. Several rows below have since been
> implemented; use `docs/BOOKING_LIFECYCLE_AND_RBAC.md` and the executable test
> suite as the current source of truth.
>
> Resolution update (2026-09-11): resend verification is enumeration-safe;
> profile email uniqueness and Settings API synchronization are implemented.
> Email canonicalization and atomic partial preference updates are covered by
> migration V9 and regression tests.

| Module | Tính năng/nghiệp vụ thiếu | Bằng chứng | Việc cần làm |
|---|---|---|---|
| Auth | Resend verification user-enumeration safe | `UserService.resendVerificationEmail` throw not found tại `UserService.java:111` | Trả response chung, chỉ gửi email nếu tồn tại |
| Profile | Backend preferences chưa nối frontend settings | `ProfileApi.java:113`, `SettingsContext.tsx:34` | Hydrate/sync settings qua API |
| Profile | Identity/address fields chưa có update flow | `V1__init.sql:34`, `ProfileService.java:90` | DTO/UI/service mapping hoặc remove |
| Villas/Rooms | Public DTO/pagination/sorting implemented (2026-09-11) | Catalogue remains locally filtered in the current UI | Move UI paging/filter state fully server-side when inventory grows beyond 100 rooms |
| Villas/Rooms | Admin room form chưa cover amenities/location/policies | `AdminApi.java:279`, `AdminDashboardPage.tsx:587` | Form đầy đủ + validation |
| Booking | State machine FE-BE chưa thống nhất | `BookingStatus.java:3`, `AdminDashboardPage.tsx:93` | Single enum contract + generated client/types |
| Booking | Check-in/check-out UI thiếu | `AdminApi.java:473`, `client.ts:145` | Add API paths + admin actions |
| Booking | Cancel reason/refund quote chưa khép kín | `BookingApi.java:149`, `BookingService.java:454` | Quote endpoint, reason persistence, gateway refund |
| Payment | Stripe webhook production chưa chạy đúng | `GatewaySignatureFilter.java:39`, `PaymentApi.java:313` | Stripe-specific signature flow |
| Payment | Payment admin UI/list chưa đầy đủ | `PaymentApi.java:469`, no frontend `API_PATHS.payments.all` usage in admin | Add payment operations UI |
| Admin | Contact/audit/revenue/occupancy/refunds UI chưa nối đủ | `AdminApi.java:596`, `AdminApi.java:634`, `AdminApi.java:662` | Add tabs/filters/actions |
| Reviews | Review eligibility chưa thống nhất | `ReviewService.java:44`, `BookingDetailPage.tsx:104` | Backend-only rule after checkout |
| Wishlist | Concurrent toggle handling thiếu | `WishlistService.java:40` | Atomic upsert/catch duplicate |
| Notifications | Notification center không dùng notification table/API | `NotificationsPage.tsx:145`, `NotificationApi.java:34` | Consume notification API, persist read/delete |
| AI | Input length/moderation/circuit breaker thiếu | `AiApi.java:44`, `GeminiService.java:118` | DTO validation, async/circuit breaker |
| Contact | Account support broken, admin handling incomplete | `SupportPage.tsx:72`, `AdminDashboardPage.tsx:83` | Fix path, admin contact queue |
| FAQ | Admin FAQ CRUD API/UI thiếu dù service có methods | `FaqService.java:62` | Add admin FAQ endpoints/UI |
| Promotions | Promotion admin UI chưa đầy đủ | `AdminApi.java:519`, admin tabs at `AdminDashboardPage.tsx:83` | Add promotions tab/form |
| Reports | Export UI thiếu | `ReportApi.java:27`, `client.ts:167` | Download buttons + date/room filters |
| Upload | SVG policy chưa quyết định | `FileStorageService.java:24` | Remove SVG or sanitize SVG |
| Database | Production migration V2 hỏng | `V2__add_performance_indexes.sql:7` | Rewrite and test migrations |
| Legacy | Legacy MVC chưa được archive/remove | `src/main/java/com/hsf/hotel/controller/*` | Remove from production path |
