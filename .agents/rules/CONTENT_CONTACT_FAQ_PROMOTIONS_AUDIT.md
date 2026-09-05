# CONTENT_CONTACT_FAQ_PROMOTIONS_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Contact/support | 45% | Account support gọi sai URL; public API thiếu validation/envelope; admin UI chưa xử lý tickets. | Fix client path, DTO validation, admin contact queue. |
| FAQ | 65% | Public list/search/helpful có; admin CRUD service có nhưng API/UI thiếu. | Admin FAQ CRUD + anti-spam helpful. |
| Promotions/offers | 65% | Public API có; admin CRUD có backend nhưng UI chưa đầy đủ; promo code validation chưa cover all business rules. | Admin promotion UI, usage atomicity, validation by room/date. |
| Blog/content/info | 35% | Backend blog/info minimal; frontend content chủ yếu static. | CMS/admin content flow hoặc remove dead API. |

## Bugs

### CONTENT-001 - High - Account support form gọi sai URL
- Mô tả: `api.post('/api/v1/contact', body)` bị client prefix thêm `/api/v1`, thành `/api/v1/api/v1/contact`.
- Nguyên nhân: Dùng hardcoded absolute API path thay vì `API_PATHS.contact`.
- File/Class/Method: `frontend/src/features/account/SupportPage.tsx:72`; `frontend/src/shared/api/client.ts:1`.
- Cách tái hiện: Submit form trong `/account/support`, request 404 tới `/api/v1/api/v1/contact`.
- Ảnh hưởng: Support ticket từ account không gửi được.
- Hướng khắc phục: `api.post(API_PATHS.contact, body)` và add integration test.

### CONTENT-002 - High - Contact API thiếu validation request
- Mô tả: `ContactApi.submitContact` nhận `Map<String,String>` và service set thẳng name/email/subject/message vào entity có nullable=false.
- Nguyên nhân: Không có DTO `@NotBlank/@Email/@Size`.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/ContactApi.java:22`; `hotel/src/main/java/com/hsf/hotel/service/ContactService.java:29`; `hotel/src/main/java/com/hsf/hotel/model/ContactMessage.java:52`.
- Cách tái hiện: POST `/api/v1/contact` body `{}`.
- Ảnh hưởng: DB constraint exception/500 thay vì lỗi validation sạch.
- Hướng khắc phục: DTO validated, normalize type/category, return `ApiResponse` envelope.

### CONTENT-003 - Medium - Contact API admin-like endpoints không tự kiểm quyền trong controller
- Mô tả: `ContactApi` có GET stats, GET by id, PUT status/priority; SecurityConfig chỉ bảo vệ `/api/v1/**` authenticated, không admin-only cho `/api/v1/contact/stats` hoặc `/api/v1/contact/{id}`.
- Nguyên nhân: Admin endpoints đặt trong public contact controller.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/ContactApi.java:53`; `hotel/src/main/java/com/hsf/hotel/config/SecurityConfig.java:102`.
- Cách tái hiện: Đăng nhập user thường, GET `/api/v1/contact/1`.
- Ảnh hưởng: User thường có thể đọc contact ticket/PII.
- Hướng khắc phục: Chuyển admin contact endpoints dưới `/api/v1/admin/contacts` hoặc add role check/matcher.

### CONTENT-004 - Medium - FAQ helpful không chống spam
- Mô tả: `FaqApi.markHelpful` public POST tăng counter mỗi lần gọi.
- Nguyên nhân: Không session/ip/user dedupe.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/FaqApi.java:45`; `hotel/src/main/java/com/hsf/hotel/service/FaqService.java:50`.
- Cách tái hiện: Loop POST `/api/v1/faqs/{id}/helpful?helpful=true`.
- Ảnh hưởng: Metrics helpful/not helpful không đáng tin.
- Hướng khắc phục: Rate limit/dedupe theo session/IP/user.
