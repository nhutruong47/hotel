# ADMIN_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Dashboard | 70% | Dữ liệu trả entity trực tiếp, chưa pagination. | DTO + pagination cho recent bookings. |
| Booking management | 55% | Status drift, thiếu checkin/checkout UI, update status quá rộng. | State transition buttons theo rule. |
| Room/voucher/user CRUD | 65% | Form thiếu nhiều field, user delete hard-delete. | Soft-disable/delete policy, validate role. |
| Reports/audit logs | 55% | Audit logs API có, UI chưa consume. | UI audit log + filters. |

## Bugs

### ADMIN-001 - High - Admin UI có filter/status sai enum
- Mô tả: `STATUS_FILTERS` có `PENDING`, `AWAITING_PAYMENT`, `CONFIRMED`, `REJECTED`, không tồn tại backend.
- Nguyên nhân: Frontend status model cũ.
- File/Class/Method: `frontend/src/features/admin/AdminDashboardPage.tsx:93`; `hotel/src/main/java/com/hsf/hotel/model/BookingStatus.java:3`.
- Cách tái hiện: Chọn status `CONFIRMED` trong admin.
- Ảnh hưởng: API trả bad request hoặc UI hiển thị sai trạng thái.
- Hướng khắc phục: Sử dụng `data.statuses` từ backend, bỏ hardcoded fallback sai.

### ADMIN-002 - High - Update role không validate role
- Mô tả: `UserApi.updateRole` set thẳng `body.getRole()`.
- Nguyên nhân: DTO `PasswordDTO.UpdateRole` không được kiểm tra allowlist trong controller.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/UserApi.java:93`.
- Cách tái hiện: PUT `/api/v1/users/{id}/role` body `{ "role": "SUPERADMIN" }`.
- Ảnh hưởng: Role rác phá RBAC và UI.
- Hướng khắc phục: Enum role hoặc allowlist `USER`, `STAFF`, `ADMIN`; cấm self-demote nếu là admin cuối.

### ADMIN-003 - Medium - User delete là hard delete
- Mô tả: Admin delete gọi `userRepository.deleteById(id)`.
- Nguyên nhân: Không dùng soft disable/delete-request flow dù `User` có disabled/delete fields.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/UserApi.java:138`; `hotel/src/main/resources/db/migration/V1__init.sql:36`.
- Cách tái hiện: Delete user có booking/review; DB FK có thể fail hoặc mất audit intent.
- Ảnh hưởng: Rủi ro mất liên kết lịch sử booking/payment/review.
- Hướng khắc phục: Replace bằng disable/soft-delete; chỉ hard purge theo GDPR workflow riêng.

### ADMIN-004 - Medium - Contact admin endpoints tồn tại nhưng admin UI không dùng
- Mô tả: Backend có `/api/v1/admin/contacts`, `/contacts/{id}/status`; frontend admin tabs chỉ overview/bookings/rooms/vouchers/users.
- Nguyên nhân: UI chưa nối contact module.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/AdminApi.java:596`; `frontend/src/features/admin/AdminDashboardPage.tsx:83`.
- Cách tái hiện: Gửi contact form, vào admin UI không có tab Contact.
- Ảnh hưởng: Ticket support không được xử lý trong app.
- Hướng khắc phục: Thêm Contact tab, status update, assignee, priority.
