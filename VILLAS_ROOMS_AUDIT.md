# VILLAS_ROOMS_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Public villa list/detail | 80% | Trả entity JPA trực tiếp, chưa pagination/sorting server-side. | DTO riêng, pagination/filter chuẩn, contract test FE-BE. |
| Availability search | 75% | Query có filter date, nhưng overlap logic có khả năng chặn checkout/checkin sát ngày. | Chuẩn hóa half-open interval `[checkIn, checkOut)`. |
| Admin room CRUD | 70% | UI chỉ gửi subset field, amenities/gallery/location chưa đủ. | Form đầy đủ, upload image, validation numeric. |
| Room cache | 65% | Cache all/available, nhưng detail không cache và DTO chưa tách. | Cache theo query phổ biến, evict mọi mutation liên quan. |

## Bugs

### ROOM-001 - High - Overlap availability coi checkout cùng ngày là conflict
- Mô tả: Conflict query dùng `b.checkInDate <= :checkOut AND b.checkOutDate >= :checkIn`.
- Nguyên nhân: Điều kiện overlap inclusive ở cả hai đầu.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/repository/BookingRepository.java:60` `findConflictingBookings`; `hotel/src/main/java/com/hsf/hotel/repository/BookingRepository.java:68` `findConflictingBookingsForUpdate`.
- Cách tái hiện: Booking A 2026-08-01 -> 2026-08-03, booking B 2026-08-03 -> 2026-08-05 cùng room.
- Ảnh hưởng: Mất doanh thu vì phòng bị coi là bận vào ngày checkout.
- Hướng khắc phục: Dùng `b.checkInDate < :checkOut AND b.checkOutDate > :checkIn` nhất quán; đã có pattern này ở `RoomRepository.findAvailableRooms` dòng 31-32.

### ROOM-002 - Medium - Public API trả entity trực tiếp
- Mô tả: `RoomApi.getRooms` đưa `List<Room>` vào response, `getRoomDetail` đưa `Room` và `Review` entity.
- Nguyên nhân: Không có DTO mapper cho room/review public surface.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/RoomApi.java:61`; `hotel/src/main/java/com/hsf/hotel/api/RoomApi.java:72`.
- Cách tái hiện: GET `/api/v1/rooms/1`, inspect JSON gồm nested entity fields.
- Ảnh hưởng: Dễ leak field nội bộ, dễ lỗi lazy serialization, contract frontend bị phụ thuộc entity.
- Hướng khắc phục: Tạo `RoomListDTO`, `RoomDetailDTO`, `ReviewSummaryDTO`.

### ROOM-003 - Medium - Admin room form không gửi amenityIds/location/policies
- Mô tả: Backend `RoomPayload` hỗ trợ `amenityIds`, location, nearby fields, min/max stay; frontend `RoomForm` payload chỉ gửi roomNumber, roomTypeId, price, capacity, bedrooms, description, imageUrl, isAvailable.
- Nguyên nhân: UI form chưa cover payload backend.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/AdminApi.java:279`; `frontend/src/features/admin/AdminDashboardPage.tsx:587`.
- Cách tái hiện: Tạo/sửa phòng từ admin UI, amenities không đổi vì không có field gửi lên.
- Ảnh hưởng: Backend có chức năng nhưng UI không hoàn thiện; catalog thiếu dữ liệu.
- Hướng khắc phục: Bổ sung controls cho amenities, gallery/image upload, location/policies/min/max stay.
