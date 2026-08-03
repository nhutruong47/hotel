# FILE_UPLOAD_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Avatar/image upload | 75% | Validate size/type/dimensions có; SVG allowlist mâu thuẫn với ImageIO. | Bỏ SVG hoặc sanitize SVG riêng. |
| Serve uploads | 70% | Public `/uploads/**`; chưa auth/private ACL. | Chỉ public nếu asset không nhạy cảm, set cache/content-type rõ. |

## Bugs

### UPLOAD-001 - High - Allow SVG nhưng sanitizer ImageIO không decode SVG
- Mô tả: `ALLOWED_EXTENSIONS` và content types cho phép `.svg`/`image/svg+xml`, nhưng `ImageProcessor.sanitize` dùng `ImageIO.read`, không hỗ trợ SVG mặc định.
- Nguyên nhân: Allowlist không khớp sanitizer.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/service/FileStorageService.java:24`; `hotel/src/main/java/com/hsf/hotel/service/ImageProcessor.java:66`.
- Cách tái hiện: Upload avatar SVG hợp lệ.
- Ảnh hưởng: Người dùng thấy loại file được allow theo content-type nhưng request fail; nếu bypass sanitizer tương lai thì SVG có XSS risk.
- Hướng khắc phục: Remove SVG khỏi allowlist hoặc dùng sanitizer SVG an toàn và serve CSP chặt.

### UPLOAD-002 - Low - Upload cleanup best-effort không audit lỗi
- Mô tả: Khi thay avatar, delete avatar cũ catch Exception rồi bỏ qua.
- Nguyên nhân: Cleanup không log/audit.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/ProfileApi.java:81`.
- Cách tái hiện: Làm file cũ không xóa được, update avatar.
- Ảnh hưởng: Rác storage tăng dần.
- Hướng khắc phục: Log warn, background cleanup orphan files.
