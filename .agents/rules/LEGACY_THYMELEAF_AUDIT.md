# LEGACY_THYMELEAF_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Legacy MVC auth/booking/admin/profile/review | 50% | Song song với REST/Next, nhiều rule cũ khác backend chính. | Không deploy legacy hoặc migrate từng flow. |
| Legacy templates | 45% | Có nhiều template còn tồn tại nhưng backend chính `hotel/` không dùng Thymeleaf. | Xác định ownership: archive hoặc remove khỏi artifact. |

## Bugs

### LEGACY-001 - High - Hai backend source tree cùng package `com.hsf.hotel`
- Mô tả: Root `src/main/java` và `hotel/src/main/java` đều có `com.hsf.hotel.*` controllers/services/models.
- Nguyên nhân: Legacy MVC chưa tách khỏi backend chính.
- File/Class/Method: `src/main/java/com/hsf/hotel/controller/BookingController.java:27`; `hotel/src/main/java/com/hsf/hotel/api/BookingApi.java:35`.
- Cách tái hiện: So sánh `rg --files src/main/java hotel/src/main/java`.
- Ảnh hưởng: Dễ sửa nhầm module, test/build root vs `hotel/` cho kết quả khác nhau.
- Hướng khắc phục: Archive legacy dưới module riêng hoặc remove khỏi production build root.

### LEGACY-002 - Medium - Legacy reset password flow tài liệu/template khác REST token flow
- Mô tả: Legacy templates/controller vẫn có `/forgot-password`, `/reset-password`; backend chính dùng `/api/v1/auth/forgot-password` token flow.
- Nguyên nhân: Hai flow auth cùng tồn tại.
- File/Class/Method: `src/main/java/com/hsf/hotel/controller/ProfileController.java:162`; `hotel/src/main/java/com/hsf/hotel/api/AuthApi.java:186`.
- Cách tái hiện: Chạy root Spring app vs `hotel` app, forgot/reset behavior khác nhau.
- Ảnh hưởng: QA/security test dễ bỏ sót bề mặt cũ.
- Hướng khắc phục: Chỉ deploy một app; legacy route redirect hoặc delete.

### LEGACY-003 - Low - File reject còn trong templates
- Mô tả: `src/main/resources/templates/index.html.rej` tồn tại trong source.
- Nguyên nhân: Artifact merge conflict/reject chưa dọn.
- File/Class/Method: `src/main/resources/templates/index.html.rej`.
- Cách tái hiện: `rg --files src/main/resources/templates`.
- Ảnh hưởng: Technical debt, có thể bị package nhầm.
- Hướng khắc phục: Xóa file `.rej` sau khi xác nhận không cần.
