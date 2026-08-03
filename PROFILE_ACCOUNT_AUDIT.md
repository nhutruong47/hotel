# PROFILE_ACCOUNT_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Xem/cập nhật profile | 75% | Chưa validate unique email khi đổi email; chưa dùng các field định danh trong UI. | Validate email trùng, audit thay đổi PII, test multipart/FormData. |
| Avatar upload | 70% | Backend hỗ trợ upload; frontend có FormData nhưng client API chưa xử lý CSRF. | Sửa CSRF client, test invalid image/large image. |
| Preferences | 60% | Backend có `/profile/preferences`; frontend settings chủ yếu dùng localStorage. | Đồng bộ settings FE với API preferences. |
| Account shell/protected pages | 70% | Route guard frontend có, nhưng legacy `/profile`, `/my-bookings` vẫn song song. | Gỡ route legacy khỏi deploy chính hoặc redirect nhất quán. |

## Bugs

### PROFILE-001 - High - Đổi email không kiểm tra trùng
- Mô tả: `ProfileService.updateProfile` set email mới và gửi verify token, nhưng không kiểm tra email đã thuộc user khác.
- Nguyên nhân: Thiếu `userRepository.findByEmail` trước khi `user.setEmail(dto.getEmail())`.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/service/ProfileService.java:79` `ProfileService.updateProfile`.
- Cách tái hiện: User A đổi email sang email đang dùng bởi User B.
- Ảnh hưởng: Trùng email ở DB vì `users.email` trong V1 không UNIQUE; reset password/notification email có thể sai người.
- Hướng khắc phục: Add unique constraint cho `users.email`, validate trước update, migration cleanup dữ liệu trùng.

### PROFILE-002 - Medium - Preferences backend chưa được frontend account settings dùng
- Mô tả: `ProfileApi` có GET/PUT `/profile/preferences`, nhưng `SettingsContext` đọc/ghi `localStorage`.
- Nguyên nhân: Settings state chỉ local, không gọi API.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/ProfileApi.java:113` `getPreferences`; `frontend/src/shared/settings/SettingsContext.tsx:34`, `frontend/src/shared/settings/SettingsContext.tsx:75`.
- Cách tái hiện: Đổi settings ở một browser, đăng nhập ở browser khác; setting không theo account.
- Ảnh hưởng: Field `users.preferences_json` không có giá trị nghiệp vụ ổn định.
- Hướng khắc phục: Khi authenticated, hydrate/update settings qua `API_PATHS.profile/preferences`.

### PROFILE-003 - Low - User identity fields có DB nhưng không có flow hoàn chỉnh
- Mô tả: V1 có `identity_type`, `identity_number`, address/city/country/postal_code; `ProfileService.updateProfile` chỉ xử lý fullName/email/avatar/phone/dateOfBirth/gender/nationality/emergency contact.
- Nguyên nhân: DTO/service không map toàn bộ field profile.
- File/Class/Method: `hotel/src/main/resources/db/migration/V1__init.sql:34`; `hotel/src/main/java/com/hsf/hotel/service/ProfileService.java:90`.
- Cách tái hiện: Tìm UI/API cập nhật identity/address; không có mapping service tương ứng.
- Ảnh hưởng: DB field PII chết, không phục vụ check-in/legal compliance.
- Hướng khắc phục: Hoặc remove fields, hoặc thêm DTO validation + UI + audit log.
