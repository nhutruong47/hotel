# REVIEWS_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Create/update/delete review | 75% | Cho review khi booking mới PAID/CHECKED_IN; frontend detail chỉ cho CHECKED_OUT/COMPLETED. | Rule duy nhất: chỉ sau checkout/completed. |
| Room review list | 75% | Public trả entity trực tiếp, hidden review có thể vẫn lộ nếu repo không filter. | DTO + filter `isHidden=false`. |
| Admin moderation | 60% | Reply/hide/delete API có; UI admin chưa expose đầy đủ. | Moderation UI + audit visible. |

## Bugs

### REVIEW-001 - High - Backend cho review trước khi khách hoàn tất lưu trú
- Mô tả: `ReviewService.createReview` cho status `PAID` và `CHECKED_IN`.
- Nguyên nhân: Business rule quá rộng.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/service/ReviewService.java:44`.
- Cách tái hiện: Tạo booking, thanh toán thành `PAID`, POST `/api/v1/reviews/booking/{id}`.
- Ảnh hưởng: Review trước khi trải nghiệm thực tế, sai nghiệp vụ và rating.
- Hướng khắc phục: Chỉ cho `CHECKED_OUT` hoặc `COMPLETED`; đồng bộ frontend.

### REVIEW-002 - Medium - UI và backend rule review không đồng bộ
- Mô tả: Booking detail chỉ enable review với `CHECKED_OUT` hoặc `COMPLETED`, trong khi backend cho `PAID/CHECKED_IN`.
- Nguyên nhân: Rule được implement ở hai nơi khác nhau.
- File/Class/Method: `frontend/src/features/account/BookingDetailPage.tsx:104`; `hotel/src/main/java/com/hsf/hotel/service/ReviewService.java:44`.
- Cách tái hiện: Booking `PAID`; UI không cho review ở detail nhưng API vẫn nhận.
- Ảnh hưởng: Người dùng có thể bypass UI qua API.
- Hướng khắc phục: Backend là source of truth; frontend chỉ phản chiếu.

### REVIEW-003 - Medium - Hidden review có nguy cơ vẫn xuất hiện ở public room detail
- Mô tả: `RoomApi.getRoomDetail` gọi `reviewService.getReviewsByRoom(room)`; service gọi repository `findByRoomOrderByCreatedAtDesc`.
- Nguyên nhân: Không thấy filter `isHidden=false` ở service path này.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/RoomApi.java:75`; `hotel/src/main/java/com/hsf/hotel/service/ReviewService.java:133`.
- Cách tái hiện: Admin hide review, GET `/api/v1/rooms/{id}`.
- Ảnh hưởng: Moderation không có hiệu lực trên public detail.
- Hướng khắc phục: Repository method `findByRoomAndIsHiddenFalseOrderByCreatedAtDesc`.
