# 📚 Tài Liệu Dự Án Hotel Booking System

## Mục Lục
1. [Tổng Quan Kiến Trúc](#tổng-quan-kiến-trúc)
2. [Workflow 1: Đăng Ký & Đăng Nhập](#workflow-1-đăng-ký--đăng-nhập)
3. [Workflow 2: Đặt Phòng](#workflow-2-đặt-phòng)
4. [Workflow 3: Chat AI Recommend](#workflow-3-chat-ai-recommend)
5. [Tổng Hợp Các Class](#tổng-hợp-các-class)

---

## Tổng Quan Kiến Trúc

### Cấu Trúc Dự Án

```
src/main/java/com/hsf/hotel/
├── config/                    # Cấu hình
│   ├── SecurityConfig.java    # Spring Security
│   └── DataInitializer.java   # Dữ liệu mẫu
├── controller/                # Xử lý HTTP Request
│   ├── WebController.java     # Đăng ký, đăng nhập
│   ├── BookingController.java # Đặt phòng
│   ├── AiController.java      # Chat AI
│   ├── AdminController.java   # Quản trị
│   ├── ProfileController.java # Hồ sơ cá nhân
│   └── ...
├── service/                   # Business Logic
│   ├── UserService.java       # Xử lý user
│   ├── BookingService.java    # Xử lý booking
│   ├── OllamaService.java     # Gọi AI
│   ├── EmailService.java      # Gửi email
│   └── ...
├── repository/                # Data Access
│   ├── UserRepository.java
│   ├── BookingRepository.java
│   ├── RoomRepository.java
│   └── ...
├── model/                     # Entity
│   ├── User.java
│   ├── Room.java
│   ├── Booking.java
│   ├── ChatMessage.java
│   └── ...
└── dto/                       # Data Transfer Object
    ├── ProfileDTO.java
    └── PasswordDTO.java
```

### Luồng Dữ Liệu Tổng Quát

```
┌─────────────┐     HTTP      ┌─────────────┐    Method    ┌─────────────┐
│   Browser   │ ───────────▶  │  Controller │ ──────────▶  │   Service   │
│  (Frontend) │               │   Layer     │              │    Layer    │
└─────────────┘               └─────────────┘              └─────────────┘
                                                                  │
                                                            Repository
                                                                  │
                                                                  ▼
                                                          ┌─────────────┐
                                                          │  Database   │
                                                          └─────────────┘
```

---

## Workflow 1: Đăng Ký & Đăng Nhập

### 1.1 Đăng Ký (Registration)

#### Sơ Đồ Luồng

```
User nhập form          WebController           UserService              Database
      │                      │                       │                      │
      │ POST /register       │                       │                      │
      │─────────────────────▶│                       │                      │
      │                      │ registerUser()        │                      │
      │                      │──────────────────────▶│                      │
      │                      │                       │ findByUsername()     │
      │                      │                       │─────────────────────▶│
      │                      │                       │     (check unique)   │
      │                      │                       │◀─────────────────────│
      │                      │                       │ findByEmail()        │
      │                      │                       │─────────────────────▶│
      │                      │                       │◀─────────────────────│
      │                      │                       │ encode(password)     │
      │                      │                       │ save(user)           │
      │                      │                       │─────────────────────▶│
      │                      │                       │◀─────────────────────│
      │                      │                       │ sendVerificationEmail│
      │ redirect:/login      │◀──────────────────────│                      │
      │◀─────────────────────│                       │                      │
```

#### Classes Liên Quan

| Class | File | Vai Trò |
|-------|------|---------|
| `WebController` | `controller/WebController.java` | Nhận HTTP request |
| `UserService` | `service/UserService.java` | Business logic đăng ký |
| `UserRepository` | `repository/UserRepository.java` | CRUD database |
| `User` | `model/User.java` | Entity lưu thông tin user |
| `EmailService` | `service/EmailService.java` | Gửi email xác thực |

#### Hàm Quan Trọng

**`WebController.registerUser()`**
```java
@PostMapping("/register")
public String registerUser(@RequestParam String username,
        @RequestParam String password,
        @RequestParam String email,
        @RequestParam(required = false) String fullName,
        RedirectAttributes redirectAttributes) {
    try {
        userService.registerUser(username, password, email, fullName);
        redirectAttributes.addFlashAttribute("success",
                "Đăng ký thành công! Vui lòng kiểm tra email để xác thực.");
        return "redirect:/login";  // ← REDIRECT ĐẾN LOGIN
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/login?register";  // ← QUAY LẠI FORM ĐĂNG KÝ
    }
}
```
> **Quyết định:** Nếu thành công → redirect `/login`. Nếu lỗi → redirect `/login?register`

**`UserService.registerUser()`**
```java
@Transactional
public User registerUser(String username, String password, String email, String fullName) {
    // Step 1: Validate username unique
    if (userRepository.findByUsername(username).isPresent()) {
        throw new RuntimeException("Tên đăng nhập đã tồn tại");
    }

    // Step 2: Validate email
    if (!EMAIL_PATTERN.matcher(email).matches()) {
        throw new RuntimeException("Email không hợp lệ");
    }
    if (userRepository.findByEmail(email).isPresent()) {
        throw new RuntimeException("Email đã được sử dụng");
    }

    // Step 3: Validate password
    if (password == null || password.length() < 6) {
        throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự");
    }

    // Step 4-5: Create user
    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));  // ← MÃ HÓA BCRYPT
    user.setEmail(email);
    user.setFullName(fullName);
    user.setRole("USER");
    user.setEmailVerified(false);
    user.setVerificationToken(UUID.randomUUID().toString());
    user.setTokenExpiry(LocalDateTime.now().plusHours(24));

    User savedUser = userRepository.save(user);

    // Step 6: Send verification email
    emailService.sendVerificationEmail(savedUser);

    return savedUser;
}
```
> **Quyết định:** Throw exception nếu validate fail. Nếu thành công → lưu DB và gửi email.

---

### 1.2 Xác Thực Email

#### Sơ Đồ Luồng

```
User click link         WebController           UserService              Database
      │                      │                       │                      │
      │ GET /verify?token=x  │                       │                      │
      │─────────────────────▶│                       │                      │
      │                      │ verifyEmail(token)    │                      │
      │                      │──────────────────────▶│                      │
      │                      │                       │ findByVerificationToken
      │                      │                       │─────────────────────▶│
      │                      │                       │◀─────────────────────│
      │                      │                       │ check token expiry   │
      │                      │                       │ set emailVerified=true
      │                      │                       │ save(user)           │
      │                      │                       │─────────────────────▶│
      │ redirect:/login      │◀──────────────────────│                      │
      │◀─────────────────────│                       │                      │
```

#### Hàm Quan Trọng

**`WebController.verifyEmail()`**
```java
@GetMapping("/verify")
public String verifyEmail(@RequestParam String token, RedirectAttributes redirectAttributes) {
    boolean success = userService.verifyEmail(token);

    if (success) {
        redirectAttributes.addFlashAttribute("success",
                "Email đã được xác thực! Bạn có thể đăng nhập.");
    } else {
        redirectAttributes.addFlashAttribute("error",
                "Link xác thực không hợp lệ hoặc đã hết hạn.");
    }

    return "redirect:/login";  // ← LUÔN REDIRECT VỀ LOGIN
}
```

**`UserService.verifyEmail()`**
```java
@Transactional
public boolean verifyEmail(String token) {
    Optional<User> userOpt = userRepository.findByVerificationToken(token);

    if (userOpt.isEmpty()) {
        return false;  // ← TOKEN KHÔNG TỒN TẠI
    }

    User user = userOpt.get();

    // Check token expiry
    if (user.getTokenExpiry() != null && 
        user.getTokenExpiry().isBefore(LocalDateTime.now())) {
        return false;  // ← TOKEN HẾT HẠN
    }

    // Verify email
    user.setEmailVerified(true);
    user.setVerificationToken(null);
    user.setTokenExpiry(null);
    userRepository.save(user);

    return true;  // ← XÁC THỰC THÀNH CÔNG
}
```

---

### 1.3 Đăng Nhập (Login)

#### Sơ Đồ Luồng (Spring Security)

```
User POST /login       SecurityFilterChain    UserDetailsService         Database
      │                      │                       │                      │
      │ username, password   │                       │                      │
      │─────────────────────▶│                       │                      │
      │                      │ loadUserByUsername()  │                      │
      │                      │──────────────────────▶│                      │
      │                      │                       │ findByUsername()     │
      │                      │                       │─────────────────────▶│
      │                      │                       │◀─────────────────────│
      │                      │◀──────────────────────│ UserDetails          │
      │                      │ verify BCrypt password│                      │
      │                      │                       │                      │
      │                      │ [if success]          │                      │
      │                      │──▶AuthenticationSuccessHandler               │
      │                      │   session.setAttribute("user", user)         │
      │ redirect:/           │◀──response.sendRedirect("/")                 │
      │◀─────────────────────│                       │                      │
      │                      │ [if fail]             │                      │
      │ redirect:/login?error│◀──failureUrl("/login?error=true")            │
      │◀─────────────────────│                       │                      │
```

#### Classes Liên Quan

| Class | Vai Trò |
|-------|---------|
| `SecurityConfig` | Cấu hình Spring Security |
| `SecurityConfig.userDetailsService()` | Load user từ DB |
| `SecurityConfig.authenticationSuccessHandler()` | Xử lý sau khi login thành công |
| `SecurityConfig.filterChain()` | Cấu hình authorization rules |

#### Hàm Quan Trọng

**`SecurityConfig.userDetailsService()`**
```java
@Bean
public UserDetailsService userDetailsService() {
    return username -> {
        return userRepository.findByUsername(username)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPassword())  // Password đã mã hóa BCrypt
                        .roles(user.getRole())         // "USER" hoặc "ADMIN"
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));
    };
}
```
> **Quyết định:** Load user từ DB, trả về UserDetails cho Spring Security xử lý.

**`SecurityConfig.authenticationSuccessHandler()`**
```java
@Bean
public AuthenticationSuccessHandler authenticationSuccessHandler() {
    return new AuthenticationSuccessHandler() {
        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, 
                HttpServletResponse response,
                Authentication authentication) throws IOException {
            String username = authentication.getName();
            userRepository.findByUsername(username)
                    .ifPresent(user -> request.getSession().setAttribute("user", user));
            // Redirect to home
            response.sendRedirect(request.getContextPath() + "/");
        }
    };
}
```
> **Quyết định:** Login thành công → Lưu user vào session → Redirect về trang chủ `/`

**`SecurityConfig.filterChain()`** - Authorization Rules
```java
.authorizeHttpRequests(authorize -> authorize
    // Public pages - không cần đăng nhập
    .requestMatchers("/", "/login", "/register", "/forgot-password", "/reset-password")
    .permitAll()
    .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**")
    .permitAll()
    .requestMatchers("/rooms", "/room/**", "/search").permitAll()

    // Admin pages - chỉ ADMIN
    .requestMatchers("/admin/**").hasRole("ADMIN")

    // Tất cả trang khác - yêu cầu đăng nhập
    .anyRequest().authenticated())
```

---

### 1.4 Quên Mật Khẩu

#### Sơ Đồ Luồng

```
User nhập email      ProfileController       ProfileService             Database
      │                      │                       │                      │
      │ POST /forgot-password│                       │                      │
      │─────────────────────▶│                       │                      │
      │                      │ findByEmail(email)    │                      │
      │                      │──────────────────────▶│                      │
      │                      │                       │ findByEmail()        │
      │                      │                       │─────────────────────▶│
      │                      │◀──────────────────────│ Optional<User>       │
      │                      │                       │                      │
      │                      │ [if email exists]     │                      │
      │ redirect:/reset-password?email=xxx          │                      │
      │◀─────────────────────│                       │                      │
      │                      │ [if email not found]  │                      │
      │ redirect:/forgot-password (error)           │                      │
      │◀─────────────────────│                       │                      │
```

#### Hàm Quan Trọng

**`ProfileController.forgotPassword()`**
```java
@PostMapping("/forgot-password")
public String forgotPassword(@RequestParam String email, 
        RedirectAttributes redirectAttributes) {
    try {
        Optional<User> userOpt = profileService.findByEmail(email);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", 
                    "Email không tồn tại trong hệ thống.");
            return "redirect:/forgot-password";  // ← EMAIL SAI
        }
        // Email đúng → chuyển thẳng đến form reset password
        redirectAttributes.addFlashAttribute("email", email);
        redirectAttributes.addFlashAttribute("success", 
                "Đã xác nhận email. Vui lòng nhập mật khẩu mới.");
        return "redirect:/reset-password";  // ← EMAIL ĐÚNG
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/forgot-password";
}
```

---

### 1.5 Reset Mật Khẩu

#### Sơ Đồ Luồng

```
User nhập password   ProfileController       ProfileService             Database
      │                      │                       │                      │
      │ POST /reset-password │                       │                      │
      │ (email, newPassword) │                       │                      │
      │─────────────────────▶│                       │                      │
      │                      │ validate password     │                      │
      │                      │ resetPasswordByEmail()│                      │
      │                      │──────────────────────▶│                      │
      │                      │                       │ findByEmail()        │
      │                      │                       │─────────────────────▶│
      │                      │                       │ encode(newPassword)  │
      │                      │                       │ save(user)           │
      │                      │                       │─────────────────────▶│
      │ redirect:/login      │◀──────────────────────│                      │
      │◀─────────────────────│                       │                      │
```

#### Hàm Quan Trọng

**`ProfileController.resetPasswordSubmit()`**
```java
@PostMapping("/reset-password")
public String resetPasswordSubmit(
        @RequestParam(required = false) String token,
        @RequestParam(required = false) String email,
        @RequestParam String newPassword,
        @RequestParam String confirmPassword,
        RedirectAttributes redirectAttributes) {
    try {
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }
        
        // Reset bằng email (luồng mới)
        if (email != null && !email.isEmpty()) {
            profileService.resetPasswordByEmail(email, newPassword);
            redirectAttributes.addFlashAttribute("success",
                    "Đã đặt lại mật khẩu thành công.");
            return "redirect:/login";  // ← THÀNH CÔNG
        }
        
        throw new RuntimeException("Thiếu thông tin email");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/reset-password?email=" + email;  // ← LỖI
    }
}
```

**`ProfileService.resetPasswordByEmail()`**
```java
@Transactional
public void resetPasswordByEmail(String email, String newPassword) {
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
    user.setPassword(passwordEncoder.encode(newPassword));  // ← MÃ HÓA BCRYPT
    userRepository.save(user);
}
```

---

## Workflow 2: Đặt Phòng

### 2.1 Tổng Quan Trạng Thái

```
                    ┌─────────────┐
                    │   PENDING   │ ← User tạo booking
                    │ (Chờ duyệt) │
                    └──────┬──────┘
                           │
            ┌──────────────┼──────────────┐
            │              │              │
            ▼              ▼              ▼
    ┌───────────────┐ ┌─────────────┐ ┌───────────────┐
    │   REJECTED    │ │  CANCELLED  │ │AWAITING_PAYMENT│
    │ (Bị từ chối)  │ │  (User hủy) │ │ (Chờ thanh toán)│
    └───────────────┘ └─────────────┘ └───────┬───────┘
                                              │
                               ┌──────────────┼──────────────┐
                               │              │              │
                               ▼              ▼              ▼
                       ┌───────────────┐ ┌─────────────┐ ┌───────────────┐
                       │   CONFIRMED   │ │  CANCELLED  │ │  (Timeout)    │
                       │ (Đã thanh toán)│ │  (User hủy) │ │ → CANCELLED   │
                       └───────┬───────┘ └─────────────┘ └───────────────┘
                               │
                               ▼
                       ┌───────────────┐
                       │   COMPLETED   │
                       │ (Hoàn thành)  │
                       └───────────────┘
```

### 2.2 Classes Liên Quan

| Class | File | Vai Trò |
|-------|------|---------|
| `BookingController` | `controller/BookingController.java` | Xử lý HTTP request đặt phòng |
| `AdminController` | `controller/AdminController.java` | Admin duyệt/từ chối |
| `BookingService` | `service/BookingService.java` | Business logic đặt phòng |
| `RoomService` | `service/RoomService.java` | Quản lý phòng |
| `BookingRepository` | `repository/BookingRepository.java` | CRUD booking |
| `RoomRepository` | `repository/RoomRepository.java` | CRUD phòng |
| `Booking` | `model/Booking.java` | Entity booking |
| `Room` | `model/Room.java` | Entity phòng |
| `BookingStatus` | `model/BookingStatus.java` | Enum trạng thái |
| `BookingScheduler` | `scheduler/BookingScheduler.java` | Auto-cancel quá hạn |

### 2.3 Bước 1: Xem Danh Sách Phòng

#### Sơ Đồ Luồng

```
User GET /rooms      RoomController          RoomService                Database
      │                      │                       │                      │
      │─────────────────────▶│                       │                      │
      │                      │ getAllAvailableRooms()│                      │
      │                      │──────────────────────▶│                      │
      │                      │                       │ findByIsAvailableTrue│
      │                      │                       │─────────────────────▶│
      │                      │◀──────────────────────│ List<Room>           │
      │ rooms.html           │◀──────────────────────│                      │
      │◀─────────────────────│                       │                      │
```

### 2.4 Bước 2: Tạo Booking

#### Sơ Đồ Luồng

```
User POST /booking   BookingController       BookingService             Database
      │                      │                       │                      │
      │ roomId, dates, info  │                       │                      │
      │─────────────────────▶│                       │                      │
      │                      │ validate dates        │                      │
      │                      │ getRoomById(roomId)   │                      │
      │                      │──────────────────────▶│                      │
      │                      │◀──────────────────────│ Room                 │
      │                      │ createBooking()       │                      │
      │                      │──────────────────────▶│                      │
      │                      │                       │ isRoomAvailable()    │
      │                      │                       │─────────────────────▶│
      │                      │                       │ calculateTotalPrice()│
      │                      │                       │ save(booking)        │
      │                      │                       │─────────────────────▶│
      │                      │                       │ sendNotification()   │
      │ redirect:/my-bookings│◀──────────────────────│                      │
      │◀─────────────────────│                       │                      │
```

#### Hàm Quan Trọng

**`BookingController.createBooking()`**
```java
@PostMapping("/booking")
public String createBooking(@RequestParam Integer roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
        @RequestParam String guestName,
        @RequestParam(required = false) String guestPhone,
        @RequestParam(required = false) String notes,
        HttpSession session,
        RedirectAttributes redirectAttributes) {
    
    User user = (User) session.getAttribute("user");
    if (user == null) {
        return "redirect:/login";  // ← CHƯA ĐĂNG NHẬP
    }

    // Validate dates
    if (checkIn.isBefore(LocalDate.now())) {
        redirectAttributes.addFlashAttribute("error", "Ngày check-in không được trước hôm nay");
        return "redirect:/booking/" + roomId;  // ← DATE KHÔNG HỢP LỆ
    }
    if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
        redirectAttributes.addFlashAttribute("error", "Ngày check-out phải sau ngày check-in");
        return "redirect:/booking/" + roomId;
    }

    try {
        Room room = roomService.getRoomById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        bookingService.createBooking(user, room, checkIn, checkOut, guestName, guestPhone, notes);
        redirectAttributes.addFlashAttribute("success", "Đặt phòng thành công!");
        return "redirect:/my-bookings";  // ← THÀNH CÔNG
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/booking/" + roomId;  // ← LỖI
    }
}
```

**`BookingService.createBooking()`**
```java
@Transactional
public Booking createBooking(User user, Room room, LocalDate checkIn, LocalDate checkOut,
        String guestName, String guestPhone, String notes) {
    
    // Step 1: Validate room available
    if (!isRoomAvailable(room, checkIn, checkOut)) {
        throw new RuntimeException("Phòng không còn trống trong thời gian này");
    }

    // Step 2: Calculate total price
    long numberOfNights = ChronoUnit.DAYS.between(checkIn, checkOut);
    BigDecimal totalPrice = room.getPricePerNight()
            .multiply(BigDecimal.valueOf(numberOfNights));

    // Step 3: Create booking
    Booking booking = new Booking();
    booking.setUser(user);
    booking.setRoom(room);
    booking.setCheckInDate(checkIn);
    booking.setCheckOutDate(checkOut);
    booking.setGuestName(guestName);
    booking.setGuestPhone(guestPhone);
    booking.setNotes(notes);
    booking.setTotalPrice(totalPrice);
    booking.setStatus(BookingStatus.PENDING);  // ← TRẠNG THÁI BAN ĐẦU

    Booking savedBooking = bookingRepository.save(booking);

    // Step 4: Send notification to admin
    emailService.sendNewBookingNotification(savedBooking);

    return savedBooking;
}
```

### 2.5 Bước 3: Admin Duyệt/Từ Chối

#### Sơ Đồ Luồng - Duyệt

```
Admin POST /admin/booking/{id}/approve
      │           AdminController           BookingService             Database
      │                      │                       │                      │
      │─────────────────────▶│                       │                      │
      │                      │ approveBooking(id, admin)                    │
      │                      │──────────────────────▶│                      │
      │                      │                       │ getBookingById()     │
      │                      │                       │─────────────────────▶│
      │                      │                       │ status = AWAITING_PAYMENT
      │                      │                       │ paymentDeadline = +24h
      │                      │                       │ save(booking)        │
      │                      │                       │─────────────────────▶│
      │                      │                       │ sendApprovalEmail()  │
      │ redirect:/admin/bookings                    │                      │
      │◀─────────────────────│◀──────────────────────│                      │
```

#### Hàm Quan Trọng

**`AdminController.approveBooking()`**
```java
@PostMapping("/admin/booking/{id}/approve")
public String approveBooking(@PathVariable Integer id, HttpSession session,
        RedirectAttributes redirectAttributes) {
    User admin = (User) session.getAttribute("user");
    if (admin == null || !"ADMIN".equals(admin.getRole())) {
        return "redirect:/login";
    }

    try {
        bookingService.approveBooking(id, admin);
        redirectAttributes.addFlashAttribute("success", "Đã duyệt đơn đặt phòng");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/bookings";  // ← QUAY VỀ DANH SÁCH BOOKING
}
```

**`BookingService.approveBooking()`**
```java
@Transactional
public void approveBooking(Integer bookingId, User adminUser) {
    Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

    if (booking.getStatus() != BookingStatus.PENDING) {
        throw new RuntimeException("Chỉ có thể duyệt booking đang chờ");
    }

    booking.setStatus(BookingStatus.AWAITING_PAYMENT);  // ← CHUYỂN TRẠNG THÁI
    booking.setApprovedBy(adminUser);
    booking.setApprovedAt(LocalDateTime.now());
    booking.setPaymentDeadline(LocalDateTime.now().plusHours(paymentDeadlineHours));

    bookingRepository.save(booking);

    // Send email to user
    emailService.sendBookingApprovedEmail(booking);
}
```

**`BookingService.rejectBooking()`**
```java
@Transactional
public void rejectBooking(Integer bookingId, User adminUser, String reason) {
    Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

    booking.setStatus(BookingStatus.REJECTED);  // ← CHUYỂN TRẠNG THÁI
    booking.setApprovedBy(adminUser);
    booking.setApprovedAt(LocalDateTime.now());
    booking.setRejectionReason(reason);

    bookingRepository.save(booking);

    // Send email to user
    emailService.sendBookingRejectedEmail(booking);
}
```

### 2.6 Bước 4: Thanh Toán

#### Sơ Đồ Luồng

```
User POST /booking/{id}/pay
      │           BookingController         BookingService             Database
      │                      │                       │                      │
      │─────────────────────▶│                       │                      │
      │                      │ check ownership       │                      │
      │                      │ confirmPayment(id)    │                      │
      │                      │──────────────────────▶│                      │
      │                      │                       │ getBookingById()     │
      │                      │                       │ status = CONFIRMED   │
      │                      │                       │ paidAt = now()       │
      │                      │                       │ save(booking)        │
      │                      │                       │─────────────────────▶│
      │ redirect:/my-bookings│◀──────────────────────│                      │
      │◀─────────────────────│                       │                      │
```

#### Hàm Quan Trọng

**`BookingController.processPayment()`**
```java
@PostMapping("/booking/{id}/pay")
public String processPayment(@PathVariable Integer id,
        HttpSession session,
        RedirectAttributes redirectAttributes) {
    User user = (User) session.getAttribute("user");
    if (user == null) {
        return "redirect:/login";
    }

    try {
        Booking booking = bookingService.getBookingById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn"));

        // Check ownership
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền thanh toán đơn này");
        }

        // Process payment (simulated)
        bookingService.confirmPayment(id);
        redirectAttributes.addFlashAttribute("success", 
                "Thanh toán thành công! Đặt phòng đã được xác nhận.");
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/my-bookings";  // ← QUAY VỀ DANH SÁCH
}
```

**`BookingService.confirmPayment()`**
```java
@Transactional
public void confirmPayment(Integer bookingId) {
    Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

    if (booking.getStatus() != BookingStatus.AWAITING_PAYMENT) {
        throw new RuntimeException("Booking không ở trạng thái chờ thanh toán");
    }

    booking.setStatus(BookingStatus.CONFIRMED);  // ← CHUYỂN TRẠNG THÁI
    booking.setPaidAt(LocalDateTime.now());

    bookingRepository.save(booking);

    // Send confirmation email
    emailService.sendPaymentConfirmationEmail(booking);
}
```

### 2.7 Auto-Cancel Booking Quá Hạn

**`BookingScheduler.cancelExpiredBookings()`**
```java
@Scheduled(fixedRate = 300000)  // Chạy mỗi 5 phút
public void cancelExpiredBookings() {
    List<Booking> expiredBookings = bookingService.getExpiredPaymentBookings();
    
    for (Booking booking : expiredBookings) {
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        emailService.sendBookingExpiredEmail(booking);
        System.out.println("⏰ Auto-cancelled booking #" + booking.getId());
    }
}
```

---

## Workflow 3: Chat AI Recommend

### 3.1 Classes Liên Quan

| Class | File | Vai Trò |
|-------|------|---------|
| `AiController` | `controller/AiController.java` | Xử lý HTTP request chat |
| `OllamaService` | `service/OllamaService.java` | Gọi Ollama AI API |
| `ChatMessageRepository` | `repository/ChatMessageRepository.java` | CRUD tin nhắn |
| `ChatMessage` | `model/ChatMessage.java` | Entity tin nhắn |
| `RoomRepository` | `repository/RoomRepository.java` | Lấy danh sách phòng |

### 3.2 Sơ Đồ Luồng

```
User gửi tin nhắn        AiController          OllamaService            Ollama AI
      │                      │                       │                      │
      │ POST /ai-recommend   │                       │                      │
      │ {message: "..."}     │                       │                      │
      │─────────────────────▶│                       │                      │
      │                      │ getAiRecommendation() │                      │
      │                      │──────────────────────▶│                      │
      │                      │                       │ findByIsAvailableTrue│
      │                      │                       │───────▶RoomRepository│
      │                      │                       │◀──────────────────────
      │                      │                       │ build prompt         │
      │                      │                       │ POST /api/generate   │
      │                      │                       │─────────────────────▶│
      │                      │                       │◀─────────────────────│
      │                      │                       │ [if fail]            │
      │                      │                       │ getFallbackRecommendation
      │                      │◀──────────────────────│ AI response          │
      │                      │ save ChatMessage      │                      │
      │                      │──────────▶ChatMessageRepository              │
      │ {response: "..."}    │◀──────────────────────│                      │
      │◀─────────────────────│                       │                      │
```

### 3.3 Hàm Quan Trọng

**`AiController.aiRecommendPage()`** - Hiển thị trang chat
```java
@GetMapping("/ai-recommend")
public String aiRecommendPage(HttpSession session, Model model) {
    User user = (User) session.getAttribute("user");
    if (user == null) {
        return "redirect:/login";  // ← CHƯA ĐĂNG NHẬP
    }

    // Load chat history (last 20 messages)
    List<ChatMessage> history = chatMessageRepository
            .findTop20ByUserOrderByCreatedAtDesc(user);
    Collections.reverse(history);  // Oldest first
    model.addAttribute("chatHistory", history);

    return "ai-recommend";  // ← HIỂN THỊ TRANG CHAT
}
```

**`AiController.getAiRecommendation()`** - Xử lý tin nhắn
```java
@PostMapping("/ai-recommend")
@ResponseBody
public Map<String, String> getAiRecommendation(
        @RequestBody Map<String, String> request,
        HttpSession session) {
    User user = (User) session.getAttribute("user");
    if (user == null) {
        return Map.of("error", "Vui lòng đăng nhập");
    }

    String userMessage = request.get("message");
    if (userMessage == null || userMessage.trim().isEmpty()) {
        return Map.of("error", "Vui lòng nhập yêu cầu của bạn");
    }

    // Get AI response
    String aiResponse = ollamaService.getAiRecommendation(userMessage);

    // Save to database
    try {
        ChatMessage chatMessage = new ChatMessage(user, userMessage, aiResponse);
        chatMessageRepository.save(chatMessage);
    } catch (Exception e) {
        System.err.println("Failed to save chat message: " + e.getMessage());
    }

    return Map.of("response", aiResponse);  // ← TRẢ VỀ JSON
}
```

**`OllamaService.getAiRecommendation()`** - Gọi AI
```java
public String getAiRecommendation(String userRequest) {
    // Lấy danh sách phòng trống
    List<Room> availableRooms = roomRepository.findByIsAvailableTrue();

    if (availableRooms.isEmpty()) {
        return "Hiện tại không có phòng trống. Vui lòng quay lại sau!";
    }

    // Build context từ danh sách phòng
    String roomsContext = availableRooms.stream()
            .map(room -> String.format(
                    "- Phòng %s (%s): %s - Giá: %s VNĐ/đêm",
                    room.getRoomNumber(),
                    room.getRoomType().getDisplayName(),
                    room.getDescription(),
                    room.getPricePerNight().toString()))
            .collect(Collectors.joining("\n"));

    // Build prompt
    String prompt = String.format("""
            Bạn là trợ lý AI của khách sạn Như Hotel.

            PHÒNG TRỐNG:
            %s

            KHÁCH CẦN: %s

            Gợi ý 1-2 phòng phù hợp nhất, giải thích ngắn gọn. Trả lời tiếng Việt.
            """, roomsContext, userRequest);

    try {
        // Gọi Ollama API
        Map<String, Object> requestBody = Map.of(
                "model", "qwen2.5:7b",
                "prompt", prompt,
                "stream", false);

        Map<String, Object> result = webClient.post()
                .uri("/api/generate")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(120))
                .block();

        if (result != null && result.containsKey("response")) {
            return (String) result.get("response");  // ← TRẢ VỀ AI RESPONSE
        }
    } catch (Exception e) {
        // Fallback khi AI không khả dụng
        return "⚠️ Trợ lý AI hiện không khả dụng.\n\n" 
                + getFallbackRecommendation(availableRooms, userRequest);
    }

    return "Không nhận được phản hồi từ AI.";
}
```

**`OllamaService.getFallbackRecommendation()`** - Keyword matching
```java
private String getFallbackRecommendation(List<Room> rooms, String userRequest) {
    String request = userRequest.toLowerCase();
    List<Room> suggestions;

    // Keyword matching
    if (request.contains("vip") || request.contains("sang trọng")) {
        suggestions = rooms.stream()
                .filter(r -> r.getRoomType().name().contains("VIP") 
                        || r.getRoomType().name().contains("SUITE"))
                .limit(2).toList();
    } else if (request.contains("gia đình") || request.contains("4 người")) {
        suggestions = rooms.stream()
                .filter(r -> r.getRoomType().name().contains("FAMILY") 
                        || r.getRoomType().name().contains("DELUXE"))
                .limit(2).toList();
    } else if (request.contains("cặp đôi") || request.contains("lãng mạn")) {
        suggestions = rooms.stream()
                .filter(r -> r.getRoomType().name().contains("DOUBLE") 
                        || r.getRoomType().name().contains("DELUXE"))
                .limit(2).toList();
    } else if (request.contains("rẻ") || request.contains("tiết kiệm")) {
        suggestions = rooms.stream()
                .sorted((a, b) -> a.getPricePerNight().compareTo(b.getPricePerNight()))
                .limit(2).toList();
    } else {
        // Default: return first 2 rooms
        suggestions = rooms.stream().limit(2).toList();
    }

    // Build response
    StringBuilder sb = new StringBuilder();
    for (Room room : suggestions) {
        sb.append(String.format("🏨 **Phòng %s** (%s)\n", 
                room.getRoomNumber(), room.getRoomType().getDisplayName()));
        sb.append(String.format("   Giá: %s VNĐ/đêm\n", room.getPricePerNight()));
    }
    
    return sb.toString();
}
```

---

## Tổng Hợp Các Class

### Model (Entity)

| Class | Mô tả | Fields chính |
|-------|-------|--------------|
| `User` | Người dùng | id, username, password, email, role, emailVerified |
| `Room` | Phòng khách sạn | id, roomNumber, roomType, pricePerNight, isAvailable |
| `Booking` | Đơn đặt phòng | id, user, room, checkIn, checkOut, status, totalPrice |
| `ChatMessage` | Tin nhắn chat AI | id, user, userMessage, aiResponse, createdAt |
| `Review` | Đánh giá | id, user, room, rating, comment |

### Enum

| Enum | Giá trị |
|------|---------|
| `RoomType` | STANDARD, DELUXE, SUITE, VIP |
| `BookingStatus` | PENDING, AWAITING_PAYMENT, CONFIRMED, REJECTED, CANCELLED, COMPLETED |

### Repository

| Repository | Entity | Methods quan trọng |
|------------|--------|-------------------|
| `UserRepository` | User | findByUsername(), findByEmail(), findByVerificationToken() |
| `RoomRepository` | Room | findByIsAvailableTrue() |
| `BookingRepository` | Booking | findByUser(), findByStatus() |
| `ChatMessageRepository` | ChatMessage | findTop20ByUserOrderByCreatedAtDesc() |

### Service

| Service | Chức năng chính |
|---------|-----------------|
| `UserService` | Đăng ký, xác thực email |
| `ProfileService` | Cập nhật profile, đổi password, reset password |
| `BookingService` | Tạo/duyệt/từ chối/thanh toán booking |
| `RoomService` | CRUD phòng |
| `OllamaService` | Gọi AI, fallback recommendation |
| `EmailService` | Gửi email thông báo |

### Controller

| Controller | Endpoints |
|------------|-----------|
| `WebController` | /login, /register, /verify |
| `ProfileController` | /profile, /change-password, /forgot-password, /reset-password |
| `RoomController` | /rooms, /room/{id} |
| `BookingController` | /booking, /my-bookings, /booking/{id}/pay |
| `AiController` | /ai-recommend |
| `AdminController` | /admin/** |

---

## Kết Luận

Dự án Hotel Booking System được xây dựng theo kiến trúc MVC với 3 workflow chính:

1. **Authentication Flow**: Đăng ký → Xác thực email → Đăng nhập → Quên/Reset mật khẩu
2. **Booking Flow**: Xem phòng → Đặt phòng → Admin duyệt → Thanh toán → Hoàn thành
3. **AI Recommendation Flow**: Chat với AI → Nhận gợi ý phòng

Mỗi workflow được phân tách rõ ràng giữa **Controller** (nhận request), **Service** (business logic), và **Repository** (data access).
