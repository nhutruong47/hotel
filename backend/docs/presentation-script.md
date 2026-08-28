# 🎤 Lời Thuyết Trình - Hotel Booking System

## Giới Thiệu Chung

> Kính chào thầy/cô và các bạn! Hôm nay em xin trình bày về hệ thống **Hotel Booking System** - một ứng dụng đặt phòng khách sạn được xây dựng trên nền tảng **Spring Boot**.

Hệ thống bao gồm **3 workflow chính**:
1. 🔐 **Đăng ký & Đăng nhập** - Authentication với email verification
2. 🏨 **Đặt phòng** - Booking workflow với trạng thái và thanh toán
3. 🤖 **Chat AI Recommend** - Tích hợp AI gợi ý phòng

---

## 📌 WORKFLOW 1: ĐĂNG KÝ & ĐĂNG NHẬP

### 1.1. Đăng Ký (Registration)

> "Bắt đầu với luồng đăng ký, khi người dùng muốn tạo tài khoản mới..."

**Các bước thực hiện:**

1. **User truy cập form đăng ký** tại `/login?register`
2. **Nhập thông tin**: username, password, email, họ tên
3. **Hệ thống validate**:
   - Username chưa tồn tại trong database
   - Email đúng định dạng và chưa được sử dụng
   - Password có ít nhất 6 ký tự
4. **Mã hóa password** bằng BCrypt (bảo mật cấp độ cao)
5. **Tạo verification token** (UUID ngẫu nhiên)
6. **Lưu user** vào database với `emailVerified = false`
7. **Gửi email xác thực** chứa link verify

**Các class liên quan:**
| Class | Vai trò |
|-------|---------|
| `WebController` | Nhận request từ form |
| `UserService.registerUser()` | Business logic đăng ký |
| `EmailService` | Gửi email xác thực |
| `User` entity | Lưu thông tin user |

**Code snippet quan trọng:**
```java
// UserService.java - Đăng ký user
user.setPassword(passwordEncoder.encode(password)); // Mã hóa BCrypt
user.setEmailVerified(false);
user.setVerificationToken(UUID.randomUUID().toString());
emailService.sendVerificationEmail(user);
```

---

### 1.2. Xác Thực Email

> "Sau khi đăng ký, user cần xác thực email để kích hoạt tài khoản..."

**Luồng xử lý:**
1. User nhấp link trong email: `/verify?token=xxx`
2. Hệ thống tìm user theo token
3. Kiểm tra token chưa hết hạn (24 giờ)
4. Set `emailVerified = true`, xóa token
5. Redirect về trang login với thông báo thành công

---

### 1.3. Đăng Nhập (Login)

> "Khi user đã có tài khoản và đăng nhập vào hệ thống..."

**Luồng xử lý:**
1. User truy cập `/login`, nhập username và password
2. **Spring Security** xử lý authentication:
   - Load user từ database qua `UserDetailsService`
   - So sánh password với BCrypt
3. Nếu thành công:
   - Lưu user vào Session
   - Redirect về trang chủ (hoặc `/admin` nếu là Admin)
4. Nếu thất bại: Hiển thị lỗi

**Điểm nổi bật:**
- ✅ Sử dụng **Spring Security** chuẩn
- ✅ Password mã hóa **BCrypt**
- ✅ Session-based authentication
- ✅ Role-based access control (USER/ADMIN)

---

### 1.4. Quên Mật Khẩu (Forgot Password)

> "Khi user quên mật khẩu, hệ thống cho phép khôi phục đơn giản qua email..."

**Luồng xử lý (ĐƠN GIẢN - không cần gửi email):**
1. User truy cập `/forgot-password`
2. Nhập **email** đã đăng ký
3. Hệ thống kiểm tra email có tồn tại trong database không
4. **Nếu email đúng** → Chuyển thẳng đến form reset password
5. **Nếu email sai** → Báo lỗi "Email không tồn tại"

**Các class liên quan:**
| Class | Vai trò |
|-------|---------|
| `ProfileController` | Xử lý request `/forgot-password` |
| `ProfileService.findByEmail()` | Kiểm tra email tồn tại |

**Code quan trọng:**
```java
// ProfileController.java - Kiểm tra email và redirect
Optional<User> userOpt = profileService.findByEmail(email);
if (userOpt.isEmpty()) {
    redirectAttributes.addFlashAttribute("error", "Email không tồn tại trong hệ thống.");
    return "redirect:/forgot-password";
}
// Email đúng → chuyển thẳng đến form reset password
redirectAttributes.addFlashAttribute("email", email);
return "redirect:/reset-password";
```

> **Đặc điểm:** Hệ thống KHÔNG gửi email xác thực OTP - chỉ cần nhập đúng email là được reset password ngay. Đơn giản và nhanh chóng!

---

### 1.5. Reset Mật Khẩu (Reset Password)

> "Sau khi xác nhận email đúng, user đặt lại mật khẩu mới ngay..."

**Luồng xử lý:**
1. Hệ thống hiển thị form reset password với email đã xác nhận
2. User nhập: **Mật khẩu mới** + **Xác nhận mật khẩu**
3. Hệ thống **validate**:
   - Mật khẩu mới ≥ 6 ký tự
   - Mật khẩu mới = Xác nhận mật khẩu
4. Mã hóa mật khẩu mới bằng **BCrypt**
5. Lưu vào database
6. Redirect về `/login` với thông báo thành công

**Code quan trọng:**
```java
// ProfileService.java - Reset password bằng email
public void resetPasswordByEmail(String email, String newPassword) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
}
```

**Tính năng:**
- ✅ **Đơn giản**: Không cần check email, không chờ OTP
- ✅ **Nhanh chóng**: Reset password chỉ trong 30 giây
- ✅ **Bảo mật**: Mật khẩu vẫn được mã hóa BCrypt

---

## 📌 WORKFLOW 2: ĐẶT PHÒNG (BOOKING)

### 2.1. Tổng Quan Trạng Thái

> "Luồng đặt phòng là workflow phức tạp nhất, với nhiều trạng thái chuyển đổi..."

```
PENDING → AWAITING_PAYMENT → CONFIRMED → COMPLETED
    ↓           ↓
 REJECTED   CANCELLED (timeout hoặc user hủy)
```

| Trạng thái | Ý nghĩa |
|------------|---------|
| `PENDING` | Chờ Admin duyệt |
| `AWAITING_PAYMENT` | Đã duyệt, chờ thanh toán (24h) |
| `CONFIRMED` | Đã thanh toán thành công |
| `REJECTED` | Admin từ chối |
| `CANCELLED` | Bị hủy (user hoặc timeout) |
| `COMPLETED` | Đã hoàn thành |

---

### 2.2. Bước 1 - Tạo Booking

> "Khi khách hàng chọn phòng và điền form đặt phòng..."

**Luồng xử lý:**
1. User xem danh sách phòng tại `/rooms`
2. Chọn phòng → Vào form `/booking/{roomId}`
3. Điền thông tin: ngày check-in/out, tên khách, số điện thoại
4. Hệ thống **validate**:
   - Ngày check-in phải từ hôm nay trở đi
   - Ngày check-out phải sau check-in
   - Phòng còn trống trong khoảng thời gian đó
5. **Tính tổng tiền** = số đêm × giá/đêm
6. Tạo booking với status `PENDING`
7. **Gửi email thông báo Admin** có đơn mới

**Code quan trọng:**
```java
// BookingService.java
booking.setStatus(BookingStatus.PENDING);
booking.setTotalPrice(room.getPricePerNight()
    .multiply(BigDecimal.valueOf(numberOfNights)));
bookingRepository.save(booking);
emailService.sendNewBookingNotification(booking);
```

---

### 2.3. Bước 2 - Admin Duyệt

> "Admin vào trang quản lý để xem và duyệt các đơn đặt phòng..."

**Khi Admin duyệt (`APPROVE`):**
1. Status → `AWAITING_PAYMENT`
2. Set `paymentDeadline` = now + 24 giờ
3. Gửi email thông báo user "Đơn đã được duyệt, vui lòng thanh toán"

**Khi Admin từ chối (`REJECT`):**
1. Status → `REJECTED`
2. Lưu lý do từ chối
3. Gửi email thông báo user

---

### 2.4. Bước 3 - Thanh Toán

> "Sau khi được duyệt, user có 24 giờ để thanh toán..."

**Luồng thanh toán:**
1. User vào `/booking/{id}/payment`
2. Hiển thị thông tin: tổng tiền, QR code, thông tin chuyển khoản
3. User xác nhận thanh toán (giả lập)
4. Status → `CONFIRMED`, set `paidAt = now()`
5. Gửi email xác nhận booking thành công

**Auto-cancel khi quá hạn:**
```java
// BookingScheduler.java - Chạy mỗi 5 phút
@Scheduled(fixedRate = 300000)
public void cancelExpiredBookings() {
    List<Booking> expired = bookingService.getExpiredPaymentBookings();
    for (Booking b : expired) {
        b.setStatus(BookingStatus.CANCELLED);
    }
}
```

---

### 2.5. Các Class Liên Quan

| Class | Vai trò |
|-------|---------|
| `BookingController` | Xử lý HTTP request |
| `BookingService` | Business logic đặt phòng |
| `AdminController` | Admin duyệt/từ chối |
| `BookingScheduler` | Auto-cancel booking quá hạn |
| `Booking` entity | Lưu thông tin booking |
| `Room` entity | Thông tin phòng |

---

## 📌 WORKFLOW 3: CHAT AI RECOMMEND

### 3.1. Tổng Quan

> "Đây là tính năng đặc biệt - tích hợp AI để gợi ý phòng cho khách hàng..."

**Công nghệ sử dụng:**
- **Ollama** - Local AI server
- **Model**: `qwen2.5:7b` - Model AI tiếng Việt
- **WebClient** - Gọi REST API

---

### 3.2. Luồng Xử Lý

> "Khi user muốn được AI tư vấn chọn phòng..."

**Bước 1 - Mở trang chat:**
1. User vào `/ai-recommend`
2. Load lịch sử chat (20 tin gần nhất)
3. Hiển thị giao diện chat

**Bước 2 - Gửi tin nhắn:**
1. User nhập: *"Tôi cần phòng cho cặp đôi, view đẹp, ngân sách 1 triệu"*
2. Frontend gửi POST `/ai-recommend` với JSON body

**Bước 3 - Xử lý backend:**
1. Lấy danh sách phòng trống từ database
2. Build **prompt** với context phòng:
```
Bạn là trợ lý AI của khách sạn Như Hotel.
PHÒNG TRỐNG:
- Phòng 101 (Deluxe): View biển - 800,000 VNĐ/đêm
- Phòng 102 (Suite): Phòng suite cao cấp - 1,500,000 VNĐ/đêm
...
KHÁCH CẦN: Tôi cần phòng cho cặp đôi, view đẹp, ngân sách 1 triệu
Gợi ý 1-2 phòng phù hợp nhất.
```
3. Gọi Ollama API (timeout 120s)
4. Nhận response từ AI

**Bước 4 - Trả về kết quả:**
1. Lưu ChatMessage vào database
2. Return JSON response cho frontend
3. Hiển thị tin nhắn AI

---

### 3.3. Fallback Logic

> "Khi Ollama không khả dụng, hệ thống có cơ chế dự phòng..."

**Keyword matching:**
```java
if (request.contains("vip") || request.contains("sang trọng"))
    → Filter VIP, SUITE rooms
    
if (request.contains("gia đình") || request.contains("4 người"))
    → Filter FAMILY, DELUXE rooms
    
if (request.contains("cặp đôi") || request.contains("lãng mạn"))
    → Filter DOUBLE, DELUXE rooms
    
if (request.contains("rẻ") || request.contains("tiết kiệm"))
    → Sort by price ascending
```

> "Nhờ có fallback, hệ thống vẫn hoạt động ngay cả khi AI server gặp sự cố!"

---

### 3.4. Các Class Liên Quan

| Class | Vai trò |
|-------|---------|
| `AiController` | Xử lý HTTP request `/ai-recommend` |
| `OllamaService` | Gọi Ollama API, build prompt |
| `ChatMessage` entity | Lưu lịch sử chat |
| `ChatMessageRepository` | CRUD tin nhắn |

---

## 🎯 Tổng Kết

> "Tóm lại, hệ thống Hotel Booking có 3 workflow chính..."

| Workflow | Đặc điểm nổi bật |
|----------|------------------|
| **Đăng ký/Đăng nhập** | BCrypt + Email verification + Spring Security |
| **Đặt phòng** | State machine + Payment deadline + Auto-cancel |
| **Chat AI** | Ollama integration + Fallback logic |

**Công nghệ sử dụng:**
- 🔧 **Backend**: Spring Boot 3.x, Spring Security, Spring Data JPA
- 🗄️ **Database**: SQL Server / H2
- 📧 **Email**: JavaMailSender
- 🤖 **AI**: Ollama (qwen2.5:7b)
- 🎨 **Frontend**: Thymeleaf + Bootstrap

> "Cảm ơn thầy/cô và các bạn đã lắng nghe! Em sẵn sàng trả lời các câu hỏi."

---

## 📝 Ghi Chú Thuyết Trình

**Thời gian ước tính:** 10-15 phút

**Chuẩn bị demo:**
1. Chạy ứng dụng: `./mvnw spring-boot:run`
2. Mở browser tại `http://localhost:8080`
3. Demo từng workflow theo thứ tự

**Câu hỏi dự kiến:**
- *"Tại sao dùng BCrypt?"* → Thuật toán hash một chiều, có salt, chống rainbow table
- *"Sao không dùng JWT?"* → Session-based phù hợp với MVC, đơn giản hơn
- *"Ollama là gì?"* → Local AI server, chạy model trên máy, không cần internet
