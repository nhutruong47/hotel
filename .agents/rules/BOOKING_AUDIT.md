# BOOKING_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Create booking | 80% | Không có idempotency key, overlap date sai ở boundary. | Idempotency, half-open date conflict, E2E concurrent booking. |
| Payment status flow | 65% | Status frontend/backend lệch; approve admin set PAID ngay. | Định nghĩa state machine duy nhất và update UI. |
| Cancel/refund | 60% | Cancel không nhận/lưu reason từ API user; refund không gọi gateway trong booking cancel. | Lưu reason, quote refund trước cancel, gateway refund flow. |
| Modify booking | 55% | Không có frontend đầy đủ; không audit request trong API. | Add UI modify + price delta + audit. |
| Check-in/check-out/no-show | 60% | Backend có API/scheduler; admin UI chưa dùng checkin/checkout. | UI operational actions + tests theo ngày. |

## Bugs

### BOOKING-001 - Critical - Frontend dùng status không tồn tại trong backend
- Mô tả: Backend enum chỉ có `PENDING_PAYMENT`, `PAID`, `CHECKED_IN`, `CHECKED_OUT`, `COMPLETED`, `CANCELLED`, `EXPIRED`, `NO_SHOW`; frontend vẫn dùng `PENDING`, `AWAITING_PAYMENT`, `AWAITING_APPROVAL`, `CONFIRMED`, `REJECTED`.
- Nguyên nhân: State machine FE-BE drift.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/model/BookingStatus.java:3`; `frontend/src/features/admin/AdminDashboardPage.tsx:93`; `frontend/src/features/account/BookingsPage.tsx:124`; `frontend/src/features/account/NotificationsPage.tsx:72`.
- Cách tái hiện: Vào Admin bookings, chọn filter `CONFIRMED` hoặc update status `CONFIRMED`; backend `BookingStatus.valueOf` reject.
- Ảnh hưởng: Filter/action sai, notification/upcoming stay không hiện đúng, user không thấy nút Pay Now cho `PENDING_PAYMENT`.
- Hướng khắc phục: Xóa status cũ khỏi FE/i18n, dùng enum từ API `statuses`, map `PENDING_PAYMENT` cho Pay Now.

### BOOKING-002 - High - Cancel reason từ frontend/API không được lưu
- Mô tả: `BookingApi.cancel` đọc `req.reason` chỉ để audit string; `BookingService.cancelBooking` luôn ghi transition reason `"Cancelled by user"` và không set `cancellationReason`.
- Nguyên nhân: Service method không nhận reason.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/BookingApi.java:149`; `hotel/src/main/java/com/hsf/hotel/service/BookingService.java:454`.
- Cách tái hiện: POST `/api/v1/bookings/{id}/cancel` body `{ "reason": "date changed" }`, kiểm tra booking/timeline.
- Ảnh hưởng: Mất dữ liệu nghiệp vụ phục vụ chăm sóc khách hàng/refund dispute.
- Hướng khắc phục: Thêm overload `cancelBooking(id,user,reason)`, set `cancellationReason`, ghi transition reason.

### BOOKING-003 - High - Payment confirmation không kiểm tra amount khớp total
- Mô tả: `BookingService.confirmPayment` nhận `amount` nhưng không so với `booking.totalPrice`.
- Nguyên nhân: Thiếu validation amount.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/service/BookingService.java:353`.
- Cách tái hiện: POST `/api/v1/bookings/{id}/payment` với `amount=1` cho booking total lớn hơn.
- Ảnh hưởng: Booking chuyển `PAID` dù số tiền sai.
- Hướng khắc phục: Require amount == total hoặc >= outstanding amount; reject mismatch, lưu payment record chuẩn.

### BOOKING-004 - Medium - Admin check-in/check-out API chưa được UI dùng
- Mô tả: Backend có `/admin/bookings/{id}/checkin` và `/checkout`, nhưng `API_PATHS.admin` không định nghĩa route này và AdminDashboard action map chỉ có approve/reject/complete/cancel.
- Nguyên nhân: UI chưa nối operational flow.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/AdminApi.java:473`; `frontend/src/shared/api/client.ts:145`; `frontend/src/features/admin/AdminDashboardPage.tsx:153`.
- Cách tái hiện: Vào admin UI với booking `PAID`, không có action check-in.
- Ảnh hưởng: Luồng lưu trú không khép kín trên UI.
- Hướng khắc phục: Thêm action check-in/check-out/no-show theo status/date.
