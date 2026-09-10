# BÁO CÁO KIỂM TOÁN TOÀN DIỆN HỆ THỐNG NHU VILLAS
**Audit Roles**: Senior Software Architect • Senior Backend Engineer • SEO Technical Specialist • Booking System Business Analyst  
**Ngày thực hiện**: 10/09/2026  
**Trạng thái hệ thống**: Baseline Audit Report  

---

## 1. EXECUTIVE SUMMARY

Hệ thống **Nhu Villas** là nền tảng đặt phòng biệt thự trực tuyến gồm:
- **Frontend**: Next.js 15.5 (App Router) + React 19 + TypeScript + TailwindCSS 4 + TanStack Query + Three.js / Canvas WebGL.
- **Backend**: Spring Boot 3.4.1 (Java 21) + Spring Data JPA + Hibernate 6.6 + Spring Security (Session Bridge + CSRF Protection) + PostgreSQL 17.

### Đánh giá tổng quan mức độ sẵn sàng Production:
| Tiêu chí | Điểm số (1-10) | Trạng thái | Đánh giá tóm tắt |
| :--- | :---: | :---: | :--- |
| **1. Booking Nghiệp Vụ** | **7.5 / 10** | Khá | Đã có flow cơ bản, kiểm tra capacity, validate ngày, audit transition. Thiếu check-in/out nghiệp vụ thực tế và timeline chi tiết. |
| **2. Availability & Chống Double Booking** | **8.0 / 10** | Tốt | Đã có query overlap chuẩn ($CheckIn < End \land CheckOut > Start$), dùng Pessimistic Row Lock (`PESSIMISTIC_WRITE`) trên Room. Thiếu bảng `VillaMaintenance` và `ReservationHold` độc lập. |
| **3. Payment** | **6.5 / 10** | Trung bình | Stripe Checkout + Webhook + Idempotency hoàn thiện. VietQR / SePay mới chỉ dừng lại ở UI sinh QR, thiếu Webhook nhận diện tự động & đối soát server-side. |
| **4. Pricing Engine** | **6.0 / 10** | Trung bình | Tính giá chạy 100% Backend (subtotal, 8% service fee, 10% VAT, voucher cap). Chưa hỗ trợ Weekend Rate, Seasonal Rate, Extra Guest Fee. |
| **5. Cancellation & Refund** | **7.0 / 10** | Khá | Chính sách hoàn tiền theo mốc thời gian (>3 ngày 100%, 1-3 ngày 50%, <1 ngày 0%), tự động gọi Stripe Refund. Chưa có bảng `CancellationRequest` / `RefundTransaction` riêng. |
| **6. Role & Permission** | **6.5 / 10** | Trung bình | Có `USER` và `ADMIN`, phòng chống IDOR tốt trên API booking/invoice. Chưa có role `MANAGER` / `STAFF` cho lễ tân. |
| **7. Technical SEO & Performance** | **4.0 / 10** | Kém (Cần khắc phục) | Trang chi tiết dùng Client Component (`'use client'`), thiếu SSR Dynamic Metadata, thiếu JSON-LD Structured Data, URL dùng ID số thay vì SEO slug, WebGL/ThreeJS gây rủi ro LCP/INP trên mobile. |
| **8. AI Concierge** | **5.5 / 10** | Trung bình | Đã tích hợp Gemini qua backend, inject context phòng trống vào prompt. Chưa dùng Tool Calling / Function Calling để truy vấn động database. |

---

## 2. CURRENT ARCHITECTURE

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                   FRONTEND (Next.js 15)                                │
│  - App Router: app/villas, app/booking, app/checkout, app/admin, app/profile           │
│  - State & Fetch: TanStack React Query + Custom Fetch Wrapper                          │
│  - Presentation: TailwindCSS 4, Framer Motion, GSAP, Lenis, Three.js / Canvas WebGL    │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ HTTP / JSON (Cookie Session)
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              BACKEND (Spring Boot 3.4.1 / Java 21)                     │
│  - REST Controllers (@ApiController: ApiPaths.V1)                                      │
│  - Security: SessionAuthBridgeFilter, RateLimitFilter, SecurityHeadersFilter, CSRF     │
│  - Domain Services: BookingService, PaymentService, RoomService, VoucherService        │
│  - Asynchronous & Jobs: BookingScheduler (15-min hold release & deadline sweep)       │
│  - Persistence: Spring Data JPA + Hibernate 6.6 + Pessimistic Locking                  │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ JDBC (HikariPool)
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                               DATABASE (PostgreSQL 17)                                 │
│  - 21 Tables: rooms, bookings, payments, vouchers, promotions, users, audit_logs...    │
│  - Schema Management: Hibernate ddl-auto=update / Flyway migration support             │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. EXISTING FEATURE MAP

| Feature | Trạng thái | Files liên quan | Ghi chú & Nhận xét |
| :--- | :---: | :--- | :--- |
| **Pessimistic Lock Overlap** | **ĐÃ CÓ** | `BookingService.java`, `BookingRepository.java` | Khóa `Room` bằng `LockModeType.PESSIMISTIC_WRITE` khi đặt phòng. |
| **Pricing Calculation** | **ĐÃ CÓ** | `BookingService.java`, `BookingApi.java` | Tính toán 100% server-side: Subtotal + 8% Service Fee + 10% VAT. |
| **Voucher / Promo Engine** | **ĐÃ CÓ** | `VoucherService.java`, `VoucherApi.java` | Validate hạn dùng, số lượt dùng, min nights, min amount, room applicability. |
| **Stripe Checkout & Webhook** | **ĐÃ CÓ** | `PaymentApi.java`, `StripePaymentAdapter.java` | Tạo session, verify webhook signature, xử lý hoàn tiền tự động. |
| **SePay / VietQR Transfer** | **CHƯA ĐỦ** | `CheckoutPage.tsx`, `PaymentApi.java` | Frontend tạo mã VietQR tĩnh/động, Backend chưa có webhook tự động nhận diện giao dịch ngân hàng. |
| **Cancellation Policy** | **ĐÃ CÓ** | `BookingService.java` | Quy tắc hoàn tiền 3 ngày / 1 ngày, tích hợp Stripe refund. |
| **Booking Hold Scheduler** | **ĐÃ CÓ** | `BookingScheduler.java`, `BookingRepository.java` | Quét giải phóng đơn giữ chỗ sau 15 phút hoặc quá deadline. |
| **Audit Logs** | **ĐÃ CÓ** | `AuditLogService.java`, `AuditLog.java` | Ghi nhận IP, user, action, target entity. |
| **Villa Maintenance** | **CHƯA CÓ** | `Room.java` | Chỉ có cờ boolean `isAvailable`, không có khoảng thời gian bảo trì theo lịch (`VillaMaintenance`). |
| **Dynamic SEO Metadata** | **CHƯA CÓ** | `app/villas/[villaId]/page.tsx` | Trang chi tiết là Client Component, không có `generateMetadata`. |
| **Structured Data (JSON-LD)** | **CHƯA CÓ** | `layout.tsx`, `VillaDetailPage.tsx` | Chưa có schema `LodgingBusiness` / `VacationRental` / `BreadcrumbList`. |
| **SEO Slugs cho Villa** | **CHƯA CÓ** | `types.ts`, `VillasPage.tsx` | Route đang dùng ID số `/villas/1` thay vì slug thân thiện `/villas/pool-villa-da-lat`. |
| **AI Tool Calling** | **CHƯA ĐỦ** | `GeminiService.java`, `AiApi.java` | Mới chỉ gửi danh sách phòng thô vào prompt, chưa có function calling API. |

---

## 4. CRITICAL ISSUES (CÁC VẤN ĐỀ NGHIÊM TRỌNG)

### 🔴 Issue 1: SePay / VietQR Thiếu Webhook Đối Soát Tự Động
- **Mức độ nghiêm trọng**: **P0 (Critical)**
- **Hiện trạng**: Khách hàng chọn chuyển khoản VietQR ở Frontend, hệ thống tạo Booking ở trạng thái `PENDING_PAYMENT`. Tuy nhiên, Backend không có webhook tiếp nhận callback từ SePay/Ngân hàng để tự động chuyển sang `PAID`. Admin buộc phải vào bấm thủ công "Mark as Paid".
- **Hậu quả**: Nếu admin không trực 24/7, đơn booking sẽ bị scheduler quét hủy sau thời gian giữ chỗ (15 phút / 24 giờ) dù khách đã chuyển tiền.
- **Giải pháp**: Xây dựng `POST /api/v1/payments/webhook/sepay` kiểm tra API Key/Signature, khớp cú pháp mã đơn `NV-{bookingId}`, đối soát số tiền và kích hoạt `confirmPayment`.

### 🔴 Issue 2: Thiếu Quản Lý Lịch Bảo Trì Biệt Thự (`VillaMaintenance`)
- **Mức độ nghiêm trọng**: **P1 (High)**
- **Hiện trạng**: `Room` chỉ có trường `is_available (boolean)`. Khi một villa cần bảo trì từ ngày 15 đến 20 tháng sau, admin không thể đặt lịch trước. Nếu tắt `is_available = false` thì khách không thể đặt cho ngày hôm nay; nếu để `true` thì khách vẫn đặt trúng ngày bảo trì.
- **Giải pháp**: Tạo bảng `villa_maintenances (id, room_id, start_date, end_date, reason, status)`. Đưa điều kiện kiểm tra bảo trì vào `findConflictingBookings` và `RoomSpecification.isNotBooked`.

### 🔴 Issue 3: SEO Villa Detail Bị Triệt Tiêu Do Dùng Client-Side Rendering
- **Mức độ nghiêm trọng**: **P1 (High - SEO)**
- **Hiện trạng**: `frontend/src/app/villas/[villaId]/page.tsx` bọc trực tiếp `VillaDetailPage.tsx` có `'use client'`. Không có `generateMetadata()`, không có OpenGraph động, không có Canonical URL theo từng phòng, Googlebot chỉ nhận được khung HTML rỗng.
- **Giải pháp**: Tách server component tại `app/villas/[villaId]/page.tsx`, fetch dữ liệu villa trên server để xuất `generateMetadata()` và nhúng thẻ JSON-LD `<script type="application/ld+json">`.

### 🟡 Issue 4: AI Concierge Chưa Có Tool Calling Động
- **Mức độ nghiêm trọng**: **P2 (Medium)**
- **Hiện trạng**: `GeminiService.java` lấy toàn bộ phòng đang có cờ `isAvailable=true` nhét vào chuỗi prompt tĩnh. AI không kiểm tra được ngày khách muốn đi xem phòng đó có bị trùng lịch hay không.
- **Giải pháp**: Tích hợp Tool Calling (`checkAvailability(roomId, checkIn, checkOut)`, `getPricing(roomId, nights)`).

---

## 5. BOOKING & AVAILABILITY AUDIT

### 5.1. Quy tắc Overlap hiện tại trong Codebase
Trong `BookingRepository.java`:
```sql
SELECT b FROM Booking b WHERE b.room = :room
  AND b.status NOT IN ('CANCELLED', 'EXPIRED', 'NO_SHOW')
  AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)
```
**Kiểm tra các Edge Cases:**
1. **Existing: 10/09 - 15/09, Request: 05/09 - 10/09**:
   - $10 < 10$ (False) $\rightarrow$ **Available** (ĐÚNG chuẩn khách trả phòng sáng ngày 10, khách mới nhận chiều ngày 10).
2. **Existing: 10/09 - 15/09, Request: 15/09 - 20/09**:
   - $15 > 15$ (False) $\rightarrow$ **Available** (ĐÚNG chuẩn).
3. **Existing: 10/09 - 15/09, Request: 12/09 - 14/09 (Nằm trọn bên trong)**:
   - $10 < 14 \land 15 > 12$ (True) $\rightarrow$ **Conflict / Blocked** (ĐÚNG).
4. **Existing: 10/09 - 15/09, Request: 08/09 - 18/09 (Bao trùm toàn bộ)**:
   - $10 < 18 \land 15 > 08$ (True) $\rightarrow$ **Conflict / Blocked** (ĐÚNG).

### 5.2. Double Booking & Race Condition Prevention
- **Cơ chế**: Khi `BookingService.createBooking` chạy, dòng code sau được thực thi:
  ```java
  Room lockedRoom = entityManager.find(Room.class, room.getId(), LockModeType.PESSIMISTIC_WRITE);
  List<Booking> conflicts = bookingRepository.findConflictingBookingsForUpdate(lockedRoom, checkIn, checkOut);
  ```
- **Đánh giá**: Cơ chế này đạt tiêu chuẩn Enterprise cho RDBMS, ngăn chặn hoàn toàn việc 2 transaction cùng ghi đè một khoảng thời gian trên cùng 1 phòng.

---

## 6. PAYMENT AUDIT

| Cổng thanh toán | Backend Adapter | Xử lý Webhook | Idempotency | Tự động cập nhật Booking |
| :--- | :--- | :--- | :--- | :--- |
| **Stripe Card** | `StripePaymentAdapter.java` | Có (`/webhook/stripe`) | Có (Check terminal status) | Có (`PAID`) |
| **VietQR / SePay** | Chưa có adapter riêng | Chưa có webhook | Chưa có | Chưa (Cần bấm tay) |
| **Pay at Property** | `PaymentMethod.CASH` | Không cần | N/A | Cần lễ tân check-in xác nhận |

---

## 7. PRICING ENGINE AUDIT

### 7.1. Logic tính giá hiện tại (`BookingService.java`)
```
Subtotal = PricePerNight × NumberOfNights
ServiceFee = Subtotal × 8% (Làm tròn HALF_UP)
TaxAmount = Subtotal × 10% (VAT)
Discount = Voucher / Promotion deduction
FinalTotal = (Subtotal - Discount) + ServiceFee + TaxAmount (Không âm)
```

### 7.2. Điểm cần nâng cấp cho Production
1. **Phụ thu cuối tuần (Weekend Surcharge)**: Giá đêm Thứ 6, Thứ 7 cao hơn ngày thường 15-20%.
2. **Phụ thu ngày lễ / mùa vụ (Seasonal & Holiday Surcharge)**: Định nghĩa khoảng ngày Tết / Lễ.
3. **Phụ thu thêm người (Extra Guest Fee)**: Nếu phòng tiêu chuẩn 4 người mà ở 5-6 người, cộng thêm phí/người/đêm.

---

## 8. CANCELLATION & REFUND AUDIT

### 8.1. Chính sách hiện tại trong `BookingService.cancelBooking`
- **Hủy trước ngày nhận phòng > 3 ngày**: Hoàn 100% tổng tiền.
- **Hủy trước 1 đến 3 ngày**: Hoàn 50% tổng tiền.
- **Hủy dưới 24 giờ**: Không hoàn tiền (0%).
- Tự động gọi API `stripeAdapter.refundPayment` nếu thanh toán qua cổng Stripe.

### 8.2. Điểm cần bổ sung
- Cần lưu lại thông tin tài khoản ngân hàng của khách (Tên ngân hàng, Số tài khoản, Tên thụ hưởng) trong trường hợp hủy đơn chuyển khoản ngân hàng (SePay/VietQR) để kế toán thực hiện lệnh hoàn tiền thủ công.

---

## 9. ROLE & AUTHORIZATION AUDIT

### 9.1. Ma trận phân quyền hiện tại
| Endpoint Group | Vai trò yêu cầu | Cơ chế kiểm tra |
| :--- | :--- | :--- |
| `/api/v1/admin/**` | `ROLE_ADMIN` | `@PreAuthorize("hasRole('ADMIN')")` + Filter |
| `/api/v1/bookings` (Tạo đơn) | `ROLE_USER` / `ROLE_ADMIN` | Kiểm tra Session User tồn tại |
| `/api/v1/bookings/{id}` | Chủ đơn hoặc `ADMIN` | So khớp `booking.user.id == session.user.id` |
| `/api/v1/bookings/{id}/invoice`| Chủ đơn hoặc `ADMIN` | So khớp IDOR check nghiêm ngặt |

### 9.2. Đề xuất mở rộng phân quyền
Nên mở rộng `UserRole` thành:
- `ADMIN`: Quản trị toàn quyền (Cấu hình, tài chính, phân quyền user).
- `STAFF` / `MANAGER`: Quản lý phòng, lịch bảo trì, thực hiện Check-in, Check-out, hỗ trợ khách tại quầy.
- `USER`: Khách hàng đặt phòng, xem lịch sử và đánh giá.

---

## 10. TECHNICAL SEO & METADATA AUDIT

### 10.1. Điểm yếu lớn nhất hiện tại
- **SSR vs CSR**: Trang `/villas` và `/villas/[villaId]` đang render hoàn toàn ở Client. View Page Source chỉ chứa placeholder Javascript.
- **URL Structure**: Hiện là `/villas/1`, `/villas/2` $\rightarrow$ Cần chuyển thành `/villas/garden-pool-villa-da-lat`.
- **Dynamic Metadata**: Chưa có hàm `generateMetadata({ params })` để Google hiển thị Rich Snippets hình ảnh, giá cả và số sao đánh giá.

### 10.2. Cấu trúc Structured Data JSON-LD cần bổ sung
```json
{
  "@context": "https://schema.org",
  "@type": "LodgingBusiness",
  "name": "Nhu Villas",
  "image": "https://nhuvillas.com/images/nhu-hero-villa-4k.jpg",
  "address": {
    "@type": "PostalAddress",
    "streetAddress": "Tuyen Lam Lake",
    "addressLocality": "Da Lat",
    "addressRegion": "Lam Dong",
    "addressCountry": "VN"
  },
  "priceRange": "$$$$",
  "aggregateRating": {
    "@type": "AggregateRating",
    "ratingValue": "4.9",
    "reviewCount": "128"
  }
}
```

---

## 11. PERFORMANCE & CORE WEB VITALS AUDIT

| Thành phần | Vấn đề tiềm ẩn | Giải pháp tối ưu |
| :--- | :--- | :--- |
| **WebGL WaterEffect & Three.js** | Tải CPU/GPU cao trên thiết bị di động, cản trở INP và LCP. | Tự động tắt WebGL trên màn hình mobile/tablet, chỉ hiển thị ảnh Hero tĩnh tối ưu WebP. |
| **Ảnh Villa** | Một số ảnh dùng kích thước gốc lớn chưa có thuộc tính `sizes` và `priority`. | Dùng `next/image` với format WebP/AVIF, đặt `priority` duy nhất cho ảnh Hero LCP đầu trang. |
| **Leaflet Map** | Thư viện map tải JS nặng khi render ban đầu. | Tiếp tục duy trì `dynamic(() => import(...), { ssr: false })` và chỉ load khi scroll đến gần viewport. |

---

## 12. AI CONCIERGE AUDIT

### Kiến trúc nâng cấp đề xuất:
Thay vì truyền raw text prompt, nâng cấp lên **Gemini Tool Calling**:
```
User Prompt: "Tôi muốn đặt villa 2 phòng ngủ có hồ bơi riêng từ ngày 12/10 đến 15/10/2026"
    │
    ▼
LLM Function Calling Decision:
    -> Gọi tool: checkAvailableVillas(checkIn="2026-10-12", checkOut="2026-10-15", bedrooms=2, amenities=["Private Pool"])
    │
    ▼
Backend Service truy vấn PostgreSQL
    -> Trả về kết quả thực tế: Villa "GV-01 - Garden Pool Villa", Giá: 450$/đêm
    │
    ▼
LLM sinh câu trả lời tư vấn chính xác 100% dựa trên database thực tế.
```

---

## 13. DATABASE GAP ANALYSIS

### Các bảng dữ liệu hiện có (21 bảng):
`rooms`, `room_types`, `amenities`, `room_amenities`, `room_gallery_images`, `bookings`, `booking_status_transitions`, `payments`, `vouchers`, `promotions`, `promotion_rooms`, `users`, `reviews`, `review_photos`, `wishlists`, `audit_logs`, `blogs`, `contact_messages`, `faq_items`, `notifications`, `chat_messages`.

### Các bảng & Cột cần bổ sung (Gaps):
1. **Bảng mới `villa_maintenances`**:
   - `id` (SERIAL PRIMARY KEY)
   - `room_id` (INT REFERENCES rooms(id))
   - `start_date` (DATE NOT NULL)
   - `end_date` (DATE NOT NULL)
   - `reason` (VARCHAR(255))
   - `created_by` (INT REFERENCES users(id))
   - `created_at` (TIMESTAMP)
2. **Cột bổ sung trong `rooms`**:
   - `slug` (VARCHAR(255) UNIQUE) — phục vụ SEO URL thân thiện.
3. **Cột bổ sung trong `bookings`**:
   - `refund_bank_name`, `refund_account_number`, `refund_account_name` — phục vụ hoàn tiền chuyển khoản ngân hàng.

---

## 14. TEST COVERAGE GAP

| Nghiệp vụ trọng yếu | Hiện trạng test | Test case cần bổ sung |
| :--- | :---: | :--- |
| **Availability Overlap** | Đã có test cơ bản | Test chi tiết các trường hợp bao trùm, nằm trong, chạm biên check-in/out. |
| **Double Booking Concurrent** | Test mock | Test multithreading mô phỏng 2 request gửi đồng thời cùng 1 mili-giây. |
| **SePay Webhook Idempotency** | Chưa có | Test gửi 2 webhook trùng transaction reference. |
| **Pricing & Extra Guest** | Đã có | Test các mốc phụ thu ngày lễ và voucher vượt subtotal. |

---

## 15. KẾ HOẠCH TRIỂN KHAI THEO GIAI ĐOẠN (IMPLEMENTATION ROADMAP)

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│  PHASE 1: Core Booking & Availability Hardening (P0)                             │
│  • Thêm bảng villa_maintenances & chặn trùng lịch bảo trì                        │
│  • Bổ sung endpoint GET /api/v1/rooms/availability chi tiết                      │
│  • Tối ưu concurrency test chống double booking                                  │
├──────────────────────────────────────────────────────────────────────────────────┤
│  PHASE 2: Payment & Webhook Automation (P0)                                      │
│  • Xây dựng SePay / VietQR automated webhook listener                            │
│  • Tách bảng RefundTransaction & lưu thông tin tài khoản hoàn tiền               │
├──────────────────────────────────────────────────────────────────────────────────┤
│  PHASE 3: Role Permission & Pricing Engine (P1)                                  │
│  • Thêm vai trò STAFF cho lễ tân thực hiện Check-in / Check-out                  │
│  • Mở rộng biểu giá cuối tuần / mùa lễ hội                                      │
├──────────────────────────────────────────────────────────────────────────────────┤
│  PHASE 4: Technical SEO & Performance (P1/P2)                                    │
│  • Chuyển trang chi tiết Villa sang SSR + dynamic slug URL                       │
│  • Tự động sinh sitemap.xml động & JSON-LD Structured Data                       │
│  • Tối ưu Core Web Vitals (Tắt WebGL trên mobile, tối ưu LCP)                   │
├──────────────────────────────────────────────────────────────────────────────────┤
│  PHASE 5: AI Concierge Function Calling (P3)                                     │
│  • Tích hợp Gemini Tool Calling truy vấn trực tiếp DB                            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 16. TIÊU CHÍ NGHIỆM THU PRODUCTION (DEFINITION OF DONE)

1. ✅ **100% Không Double Booking**: Kiểm tra concurrent request chặn đứng việc trùng ngày.
2. ✅ **Availability Server-side**: Tất cả logic lọc phòng trống và bảo trì chạy tại SQL/JPA Specification.
3. ✅ **Pricing Server-side**: Giá, thuế, phí và giảm giá được tính toán và niêm phong tại Backend.
4. ✅ **Tự động hóa Payment Webhook**: Nhận diện tiền về qua SePay/Stripe và kích hoạt đơn tự động.
5. ✅ **SEO Friendly**: 100% các trang Villa có meta title, description, OG image, canonical và Schema.org hợp lệ khi View Source.
6. ✅ **An toàn dữ liệu**: Phòng chống triệt để IDOR, CSRF và phân quyền chặt chẽ giữa User, Staff và Admin.
