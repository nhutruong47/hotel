# AUTH_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Đăng ký | 75% | Chưa auto-login sau register ở backend; resend verification lộ email tồn tại. | Chuẩn hóa response, không leak email tồn tại, test race duplicate username/email. |
| Đăng nhập/session/logout | 80% | Session cookie auth hoạt động nhưng CSRF client chưa tích hợp. | Client phải echo `X-XSRF-TOKEN`, test login/logout/multi-tab/session timeout. |
| Email verification | 70% | Resend verification trả lỗi nếu email không tồn tại. | Response user-enumeration safe. |
| Forgot/reset password | 80% | Chưa revoke session hiện có sau reset password. | Invalidate active sessions hoặc rotate credential version. |

## Bugs

### AUTH-001 - Medium - Resend verification leak email tồn tại
- Mô tả: `resendVerification` gọi service và service throw `ResourceNotFoundException` khi email không tồn tại.
- Nguyên nhân: Endpoint public nhưng không giữ cùng nguyên tắc user-enumeration safe như forgot password.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/AuthApi.java:137` `AuthApi.resendVerification`; `hotel/src/main/java/com/hsf/hotel/service/UserService.java:109` `UserService.resendVerificationEmail`.
- Cách tái hiện: POST `/api/v1/auth/resend-verification?email=not-found@example.com`, so sánh response với email có tồn tại.
- Ảnh hưởng: Cho phép dò danh sách email đã đăng ký.
- Hướng khắc phục: Luôn trả 200 với message chung; chỉ gửi email nếu tồn tại và chưa verified.

### AUTH-002 - Low - Login cho phép tài khoản chưa verified
- Mô tả: Login thành công ngay cả khi `emailVerified=false`.
- Nguyên nhân: `UserService.login` chỉ log khi email chưa verified, không chặn.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/service/UserService.java:140` `UserService.login`.
- Cách tái hiện: Register user mới, không verify email, POST `/api/v1/auth/login`.
- Ảnh hưởng: Email verification không thực sự bảo vệ account lifecycle.
- Hướng khắc phục: Quyết định business rule rõ; nếu verification bắt buộc thì trả `EMAIL_NOT_VERIFIED` và UI hiển thị resend action.

### AUTH-003 - Medium - Password update duplicate bỏ qua current password
- Mô tả: `/api/v1/users/me` đổi mật khẩu bằng `newPassword` mà không kiểm tra `currentPassword`.
- Nguyên nhân: `UserApi.updatePassword` gọi `userService.updateUser(username, newPassword)`.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/UserApi.java:57` `UserApi.updatePassword`; `hotel/src/main/java/com/hsf/hotel/service/UserService.java:203` `UserService.updateUser`.
- Cách tái hiện: Đăng nhập, PUT `/api/v1/users/me` với body chỉ có `newPassword` hợp lệ.
- Ảnh hưởng: Nếu session bị chiếm, attacker đổi password không cần biết password hiện tại.
- Hướng khắc phục: Xóa endpoint duplicate hoặc route về `ProfileService.changePassword`.
