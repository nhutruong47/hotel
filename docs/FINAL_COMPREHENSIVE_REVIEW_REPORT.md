# BÁO CÁO TỔNG QUAN & ĐÁNH GIÁ KỸ THUẬT DỰ ÁN NHU VILLAS
**Nền Tảng Đặt Phòng & Vận Hành Khu Nghỉ Dưỡng Cao Cấp (Enterprise-Oriented Luxury Resort Management Platform)**  
*Phiên bản*: `2.0.0-CAPSTONE-RC` | *Trạng thái*: **Hoàn thiện cho mục tiêu trình diễn, thẩm định kỹ thuật và bảo vệ đồ án**

---

## 📑 MỤC LỤC BÁO CÁO
1. [Tầm Nhìn & Phạm Vi Nghiệp Vụ (Project Scope & Vision)](#1-tầm-nhìn--phạm-vi-nghiệp-vụ)
2. [Kiến Trúc Hệ Thống & Phân Tầng Kỹ Thuật (Architecture & Tech Stack)](#2-kiến-trúc-hệ-thống--phân-tầng-kỹ-thuật)
3. [Phân Hệ Nghiệp Vụ Trọng Yếu & Cơ Chế Concurrency](#3-phân-hệ-nghiệp-vụ-trọng-yếu--cơ-chế-concurrency)
4. [Cơ Sở Dữ Liệu, Quan Hệ & Quản Lý Migration (PostgreSQL 17)](#4-cơ-sở-dữ-liệu-quan-hệ--quản-lý-migration)
5. [Kiến Trúc Bảo Mật, RBAC & Chống IDOR (Security Architecture)](#5-kiến-trúc-bảo-mật-rbac--chống-idor)
6. [Độ Tin Cậy, Cơ Chế Dự Phòng Caching & Outbox Messaging](#6-độ-tin-cậy-cơ-chế-dự-phòng-caching--outbox-messaging)
7. [Hiện Thực Hóa Quy Trình Drone Inspection & Giới Hạn Nghiệp Vụ](#7-hiện-thực-hóa-quy-trình-drone-inspection--giới-hạn-nghiệp-vụ)
8. [Hiện Trạng Kiểm Thử & Đảm Bảo Chất Lượng (QA & Test Strategy)](#8-hiện-trạng-kiểm-thử--đảm-bảo-chất-lượng)
9. [Đánh Giá Khách Quan, Rủi Ro Kiến Trúc & Hướng Mở Rộng](#9-đánh-giá-khách-quan-rủi-ro-kiến-trúc--hướng-mở-rộng)
10. [Danh Mục Tài Liệu Kỹ Thuật Bàn Giao](#10-danh-mục-tài-liệu-kỹ-thuật-bàn-giao)

---

## 1. Tầm Nhìn & Phạm Vi Nghiệp Vụ

**Nhu Villas** được thiết kế theo định hướng **Enterprise-Oriented Capstone Platform**, nhằm mô phỏng và giải quyết các bài toán kỹ thuật thực tế trong ngành quản trị khu nghỉ dưỡng cao cấp:
- **Trải nghiệm khách hàng (Customer-Facing Experience)**: Tìm kiếm villa theo ngày, phân loại vị trí, áp dụng voucher khuyến mãi, thanh toán đa kênh và nhận tư vấn thông minh từ AI Concierge.
- **Tính toàn vẹn giao dịch (Transaction Integrity)**: Ngăn ngừa race condition đặt trùng phòng trong mùa cao điểm thông qua cơ chế khóa dòng ở mức Transaction và kiểm tra khoảng giao thời gian.
- **Quy trình kiểm định chất lượng phòng (Pre-Arrival Quality Control Workflow)**: Số hóa quy trình thanh tra villa trước giờ đón khách VIP, bao gồm ghi nhận dữ liệu viễn trắc bay drone và checklist tiêu chuẩn 5 sao.
- **Vận hành tập trung (Back-Office Studio)**: Quản lý biểu giá, 15+ trường dữ liệu chi tiết của villa, quản lý voucher, theo dõi doanh thu và phân quyền nhân sự.

---

## 2. Kiến Trúc Hệ Thống & Phân Tầng Kỹ Thuật

Dự án được xây dựng theo mô hình **Modular Monolith với Phân tầng Layered Architecture**, kết hợp Frontend SSR tách biệt:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       PRESENTATION LAYER (Next.js 15)                   │
│   - React 19 + TypeScript Strict + TailwindCSS                          │
│   - Server-Side Rendering (SSR) cho Catalog Discovery & SEO             │
│   - Dynamic Client Components cho Drone Mission Control & Admin Studio  │
│   - Type-Safe API Client (client.ts) với Global Error Boundaries        │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ JSON over HTTPS (JWT / Bearer & Cookie)
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    APPLICATION CORE (Spring Boot 3.4)                   │
│   - Java 21 LTS + Spring Security 6                                     │
│   - Layered Domain Modules: auth, room, booking, payment, review,       │
│     voucher, notification, admin, inspection, ai                        │
│   - DTO Layer Isolation: Phân tách hoàn toàn Entity JPA với REST API    │
└──────────────────┬──────────────────────────────────┬───────────────────┘
                   │                                  │
                   ▼                                  ▼
┌──────────────────────────────────────┐  ┌───────────────────────────────┐
│      DATABASE LAYER (PostgreSQL 17)  │  │    RESILIENCE & MESSAGING     │
│   - 23 Bảng quan hệ định chuẩn       │  │  - Redis Cache                │
│   - Flyway Migrations (V1 -> V11)    │  │    (Graceful Local Fallback)  │
│   - B-Tree & Composite Indexes       │  │  - RabbitMQ Outbox Publisher  │
└──────────────────────────────────────┘  └───────────────────────────────┘
```

### Chi Tiết Phân Tầng Công Nghệ

| Phân Tầng | Công Nghệ / Thư Viện | Vai Trò & Cơ Chế Thực Thi |
| :--- | :--- | :--- |
| **Frontend Web** | Next.js 15, React 19, TailwindCSS | Giao diện khách hàng, SSR SEO, Client State Management, Form Studio 5 tab. |
| **Backend REST API** | Spring Boot 3.4.x, Java 21 LTS | REST Controllers (`@RestController`), Request Validation, Transaction boundaries. |
| **Bảo Mật & Xác Thực** | Spring Security 6, JJWT, BCrypt | Bộ lọc `JwtAuthenticationFilter`, Stateless Token Verification, Method Security. |
| **Dữ Liệu & ORM** | PostgreSQL 17, Spring Data JPA, Hibernate 6 | Quản lý quan hệ thực thể, Pessimistic Locking, Flyway Database Versioning. |
| **Caching & Async** | Redis 7, RabbitMQ (kèm In-Memory Fallback) | Caching danh mục phòng, hàng đợi gửi email thông báo qua Outbox Pattern. |

---

## 3. Phân Hệ Nghiệp Vụ Trọng Yếu & Cơ Chế Concurrency

```
                                  LUỒNG TẠO BOOKING AN TOÀN
┌──────────────┐      ┌────────────────────────┐      ┌─────────────────────────┐      ┌──────────────┐
│ User Request │ ───▶ │  BEGIN TRANSACTION     │ ───▶ │ SELECT Room FOR UPDATE  │ ───▶ │ Check Overlap│
└──────────────┘      │  (@Transactional)      │      │ (PESSIMISTIC_WRITE)     │      │ Query Check  │
                      └────────────────────────┘      └─────────────────────────┘      └──────┬───────┘
                                                                                              │
                                         ┌────────────────────────────────────────────────────┴────────┐
                                         ▼                                                             ▼
                             [Có xung đột / Đang bảo trì]                                    [Phòng Trống Hợp Lệ]
                                         │                                                             │
                                         ▼                                                             ▼
                             Throw BusinessRuleException                                 Consume Voucher & Persist
                             (Rollback Transaction)                                      (COMMIT TRANSACTION)
```

### 3.1 Concurrency-Safe Booking & Overbooking Prevention
Để ngăn ngừa hiện tượng race condition khi nhiều người dùng cùng nhấn đặt một villa trong cùng khoảng thời gian:
1. **Pessimistic Write Lock (`LockModeType.PESSIMISTIC_WRITE`)**:
   Trong phương thức `@Transactional createBooking()`, hệ thống thực thi `entityManager.find(Room.class, roomId, LockModeType.PESSIMISTIC_WRITE)` để khóa dòng tương ứng trên PostgreSQL (`SELECT ... FOR UPDATE`).
2. **Kiểm Tra Trùng Lịch Trong Phạm Vi Khóa**:
   Hệ thống chạy truy vấn `findConflictingBookingsForUpdate()` và `findConflictingMaintenances()`. Bất kỳ xung đột nào sẽ kích hoạt `BusinessRuleException`, hủy giao dịch và giải phóng khóa an toàn.
3. **Tự Động Hủy Giữ Chỗ Quá Hạn (Auto Hold Release)**:
   `BookingScheduler` quét định kỳ các đơn ở trạng thái `PENDING` quá 15 phút chưa hoàn tất thanh toán để chuyển về `CANCELLED` và hoàn lại voucher.

### 3.2 Cổng Thanh Toán & Webhook Idempotency
- **Hỗ trợ đa phương thức**: VNPay, Stripe, MoMo, PayPal, và Tiền mặt tại quầy (Cash).
- **Quy trình Xử lý Webhook / IPN An toàn**:
  1. Kiểm tra chữ ký HMAC (SHA512 với VNPay / Webhook Secret với Stripe).
  2. Truy vấn giao dịch theo mã tham chiếu (`transactionReference`).
  3. Kiểm tra trạng thái hiện tại: Nếu giao dịch đã ở trạng thái `PAID`, hệ thống phản hồi `200 OK` ngay lập tức để đảm bảo tính **Idempotent**, ngăn ngừa việc ghi nhận doanh thu hai lần.
  4. Nếu hợp lệ, cập nhật trạng thái `PENDING -> PAID` và chuyển trạng thái booking sang `CONFIRMED`.

### 3.3 Admin Villa Studio (15+ Thuộc Tính)
Form quản trị villa tại [`AdminDashboardPage.tsx`](file:///c:/Users/TTN/Downloads/HotelMian/hotel/frontend/src/features/admin/AdminDashboardPage.tsx) được cấu trúc thành 5 tab chức năng:
- **Thông tin cơ bản**: Tên, số phòng, loại villa (`OCEAN_VIEW`, `BEACHFRONT`, `GARDEN_RETREAT`), giá, sức chứa, ảnh đại diện.
- **Bộ chọn Tiện ích (Amenities)**: Gắn thẻ tiện ích phân nhóm chuẩn hóa.
- **Bộ sưu tập ảnh (Gallery)**: Danh sách URL ảnh chi tiết từng góc của villa.
- **Vị trí & Xung quanh**: Tọa độ GPS (Latitude, Longitude), chỉ đường, danh sách nhà hàng/quán cafe/sân bay lân cận.
- **Chính sách & Quy định**: Giờ check-in/check-out, nội quy villa, chính sách hoàn hủy cọc, giới hạn số đêm lưu trú tối thiểu/tối đa.

---

## 4. Cơ Sở Dữ Liệu, Quan Hệ & Quản Lý Migration

Hệ thống quản lý lịch sử tiến hóa cơ sở dữ liệu trên PostgreSQL 17 thông qua **Flyway Migration** (11 phiên bản tuần tự):

```
V1__init_schema.sql ──────────▶ V2__add_payment_and_vouchers.sql ───▶ V3__add_reviews_and_ratings.sql
          │                                                                      │
V6__room_extended_fields.sql ◀─ V5__add_wishlist.sql ◀──────────────── V4__add_audit_logs.sql
          │
V7__add_notifications.sql ────▶ V8__add_amenities_catalog.sql ─────▶ V9__add_promotions_and_faqs.sql
                                                                                 │
V11__add_villa_inspections.sql ◀─── V10__add_performance_indexes.sql ◀──────────┘
```

### Cấu Trúc Bảng Dữ Liệu Chính (23 Tables)
- **Quản lý Tài khoản & Quyền**: `users`, `roles`, `user_roles`.
- **Danh mục Villa & Tiện ích**: `rooms`, `room_types`, `amenities`, `room_amenities`, `room_galleries`.
- **Giao dịch Đặt phòng & Thanh toán**: `bookings`, `booking_details`, `payments`, `vouchers`, `voucher_usages`.
- **Chất lượng & Vận hành**: `villa_inspections`, `maintenances`, `reviews`, `audit_logs`.
- **Khách hàng & Trải nghiệm**: `wishlists`, `notifications`, `notification_outbox`, `promotions`, `faqs`, `contacts`.

---

## 5. Kiến Trúc Bảo Mật, RBAC & Chống IDOR

### 5.1 Phân Quyền 5 Cấp (Role-Based Access Control)
Hệ thống thiết lập ma trận phân quyền rõ ràng qua `@PreAuthorize`:
- `ROLE_CUSTOMER`: Tìm kiếm phòng, tạo booking cá nhân, thanh toán, viết đánh giá, trò chuyện với AI.
- `ROLE_STAFF`: Xem danh sách booking tổng, thực hiện Check-in / Check-out cho khách tại quầy.
- `ROLE_INSPECTOR`: Tiếp nhận lịch kiểm định villa, ghi nhận thông số bay drone & checklist, ký duyệt chất lượng.
- `ROLE_MANAGER`: Cấu hình giá, tạo voucher, duyệt bảo trì, theo dõi báo cáo doanh thu.
- `ROLE_ADMIN`: Toàn quyền cấu hình hệ thống, quản lý tài khoản, xem audit logs.

### 5.2 Kiểm Soát Sở Hữu Dữ Liệu (Resource Ownership / IDOR Prevention)
Bên cạnh kiểm tra Role ở tầng Controller, hệ thống áp dụng xác thực quyền sở hữu tài nguyên ở tầng logic nghiệp vụ:
```java
// Kiểm tra IDOR trong BookingApi.java
if (!booking.getUser().getId().equals(currentUser.getId()) && !"ADMIN".equals(currentUser.getRole())) {
    throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, "Không có quyền truy cập tài nguyên này");
}
```

### 5.3 Phân Tách DTO (Entity Isolation)
- 100% API trả về các DTO độc lập ([`AdminResponseDtos.java`](file:///c:/Users/TTN/Downloads/HotelMian/hotel/backend/src/main/java/com/hsf/hotel/admin/dto/AdminResponseDtos.java), [`ReviewDetailResponse.java`](file:///c:/Users/TTN/Downloads/HotelMian/hotel/backend/src/main/java/com/hsf/hotel/review/dto/ReviewDetailResponse.java), [`InspectionDto.java`](file:///c:/Users/TTN/Downloads/HotelMian/hotel/backend/src/main/java/com/hsf/hotel/room/dto/InspectionDto.java)).
- Ngăn chặn triệt để việc serialize mật khẩu (`passwordHash`), token nhạy cảm hoặc gây lỗi `LazyInitializationException` của Hibernate.

---

## 6. Độ Tin Cậy, Cơ Chế Dự Phòng Caching & Outbox Messaging

### 6.1 Cơ Chế Dự Phòng Caching (Graceful Cache Degradation)
Trong [`CacheConfig.java`](file:///c:/Users/TTN/Downloads/HotelMian/hotel/backend/src/main/java/com/hsf/hotel/config/CacheConfig.java):
- Hệ thống ưu tiên kết nối Redis cho việc lưu trữ cache danh mục phòng và cấu hình.
- **Fallback**: Nếu Redis mất kết nối, hệ thống ghi log cảnh báo và tự động chuyển sang sử dụng `ConcurrentMapCacheManager` nội bộ trong bộ nhớ JVM (Single-Node Graceful Degradation), đảm bảo luồng đặt phòng không bị gián đoạn.

### 6.2 Transactional Outbox Pattern Cho Messaging
Để giải quyết bài toán Dual-Write khi vừa lưu dữ liệu vào DB vừa bắn message gửi email/thông báo:
1. Sự kiện thông báo được ghi đồng thời vào bảng `notification_outbox` trong cùng một `@Transactional` của nghiệp vụ chính.
2. Tác vụ ngầm định kỳ quét các bản ghi `PENDING` trong outbox để đẩy vào RabbitMQ, có cơ chế retry và cập nhật trạng thái `SENT` / `FAILED`.

---

## 7. Hiện Thực Hóa Quy Trình Drone Inspection & Giới Hạn Nghiệp Vụ

### 7.1 Mục Đích Nghiệp Vụ
Quy trình được thiết kế nhằm số hóa khâu kiểm định phòng cao cấp:
- Inspector tiếp nhận nhiệm vụ kiểm tra trước khi khách VIP nhận villa.
- Ghi nhận thông số kỹ thuật: Tình trạng pin bay, độ cao hành trình, điểm quét nhiệt mái ngói, điểm độ sạch hồ bơi, độ ồn điều hòa HVAC (dB).
- Ký duyệt `PASSED` (đủ tiêu chuẩn đón khách) hoặc `ACTION_REQUIRED` (chuyển sang lịch bảo trì).

### 7.2 Giới Hạn Triển Khai Thực Tế (Implementation Scope Transparency)
> **Lưu ý kỹ thuật minh bạch**: Dữ liệu viễn trắc và checklist kiểm định hiện tại được **nhập liệu và ghi nhận thông qua giao diện số hóa của Inspector** (hoặc payload giả lập thiết bị), chưa tích hợp trực tiếp SDK phần cứng camera quang học hay Computer Vision thời gian thực từ drone vật lý.

---

## 8. Hiện Trạng Kiểm Thử & Đảm Bảo Chất Lượng

Hệ thống đã hoàn thành bộ kiểm thử tự động với kết quả ghi nhận như sau:

```
========================================================================
                      BÁO CÁO TỔNG HỢP KIỂM THỬ TỰ ĐỘNG
========================================================================
[1] BACKEND TEST SUITE (Maven Surefire / JUnit 5 / Mockito)
    - Số lượng tests thực thi  : 166 tests
    - Phạm vi kiểm thử         :
        + Unit Tests           : Logic định giá, Voucher, User validation
        + Integration Tests    : Review visibility, Payment adapters, Wishlist concurrency
        + Security & Role Tests: UserRole RBAC validation
    - Kết quả                  : 166 PASS / 0 FAIL / 0 ERROR / 0 SKIPPED
    - Thời gian chạy           : 1 phút 25 giây (BUILD SUCCESS)

[2] FRONTEND VALIDATION (Next.js 15 & TypeScript Compiler)
    - TypeScript Type-Check    : Hoàn thành không có lỗi biên dịch (0 type errors)
    - Next.js Production Build : 34 / 34 Routes tĩnh & động được tối ưu thành công
    - Trạng thái biên dịch     : Exit code 0

[3] DATABASE MIGRATION INTEGRITY (Flyway)
    - Schema Validation        : Flyway v1 -> v11 áp dụng thành công trên PostgreSQL 17
========================================================================
```

---

## 9. Đánh Giá Khách Quan, Rủi Ro Kiến Trúc & Hướng Mở Rộng

### 9.1 Điểm Mạnh Của Dự Án
1. **Giải quyết đúng trọng tâm bài toán Concurrency**: Áp dụng đúng `PESSIMISTIC_WRITE` và transaction boundary để bảo vệ tính toàn vẹn của dữ liệu đặt phòng.
2. **Thiết kế phân tầng sạch sẽ**: Tách biệt rõ DTO và Entity, kiểm soát IDOR chặt chẽ ở cấp độ tài nguyên.
3. **Ý tưởng nghiệp vụ sát thực tế**: Kết hợp giữa quản trị vận hành cao cấp, kiểm định chất lượng và hỗ trợ thanh toán đa kênh.

### 9.2 Các Điểm Cần Cải Thiện Trong Tương Lai (Future Roadmaps)
1. **Multi-Node Cache Synchronization**: Khi triển khai nhiều instance backend phía sau Load Balancer, cần bổ sung Redis Sentinel/Cluster để tránh hiện tượng phân mảnh cache nội bộ khi Redis gặp sự cố.
2. **Testcontainers Cho PostgreSQL**: Chuyển các bài kiểm thử tích hợp từ H2 sang sử dụng Testcontainers chạy PostgreSQL thực tế để kiểm tra triệt để các hàm đặc thù của Postgres.
3. **Tích Hợp IoT / Drone SDK Thực Tế**: Mở rộng webhook API nhận dữ liệu telemetry tự động trực tiếp từ DJI Cloud API.

---

## 10. Danh Mục Tài Liệu Kỹ Thuật Bàn Giao

| Tên Tài Liệu | Đường Dẫn | Nội Dung Chi Tiết |
| :--- | :--- | :--- |
| **Đặc Tả REST API** | [`docs/API_DOCUMENTATION.md`](file:///c:/Users/TTN/Downloads/HotelMian/hotel/docs/API_DOCUMENTATION.md) | Tài liệu OpenAPI chuẩn cho toàn bộ 100% REST endpoints, Request/Response payload và mã lỗi. |
| **Bộ Câu Hỏi Bảo Vệ & Phỏng Vấn** | [`docs/FINAL_PROJECT_INTERVIEW_QA.md`](file:///c:/Users/TTN/Downloads/HotelMian/hotel/docs/FINAL_PROJECT_INTERVIEW_QA.md) | 10 chuyên đề phỏng vấn kỹ thuật chuyên sâu bảo vệ đồ án (Concurrency, DB, Security, Drone, Next.js). |
| **Hướng Dẫn Cài Đặt & Chạy Hệ Thống** | [`README.md`](file:///c:/Users/TTN/Downloads/HotelMian/hotel/README.md) | Cấu hình biến môi trường, thiết lập PostgreSQL 17 local và hướng dẫn khởi chạy. |
| **Nhật Ký Nghiệm Thu** | [`walkthrough.md`](file:///C:/Users/TTN/.gemini/antigravity-ide/brain/8f5aa0f4-917a-4fa8-a33a-9a8d7c6ad11c/walkthrough.md) | Ghi nhận chi tiết các bước hoàn thiện và kết quả kiểm thử. |
