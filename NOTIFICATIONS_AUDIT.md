# NOTIFICATIONS_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Backend notification CRUD | 75% | Có list/read/read-all/delete; chưa SSE/push. | FE consume API, pagination UI, realtime optional. |
| Frontend notification center | 35% | UI tự tổng hợp từ bookings, không dùng notification API. | Dùng `/api/v1/notifications`, persist read state. |

## Bugs

### NOTIF-001 - High - Frontend notification center không dùng backend notifications
- Mô tả: UI gọi `API_PATHS.bookings.list` rồi synthesize notifications; không có `API_PATHS.notifications`.
- Nguyên nhân: Frontend chưa nối NotificationApi.
- File/Class/Method: `frontend/src/features/account/NotificationsPage.tsx:145`; `hotel/src/main/java/com/hsf/hotel/api/NotificationApi.java:34`; `frontend/src/shared/api/client.ts:89`.
- Cách tái hiện: Backend tạo notification bằng booking transition, mở `/account/notifications`; UI không gọi `/notifications`.
- Ảnh hưởng: Read/unread/delete backend không có tác dụng trên UI.
- Hướng khắc phục: Thêm API paths, query `/notifications`, mutation mark read/read-all/delete.

### NOTIF-002 - Medium - UI dùng status `CONFIRMED` nên bỏ sót paid/upcoming
- Mô tả: Upcoming stay chỉ xét `CONFIRMED`, backend dùng `PAID`.
- Nguyên nhân: Status drift.
- File/Class/Method: `frontend/src/features/account/NotificationsPage.tsx:111`; `hotel/src/main/java/com/hsf/hotel/model/BookingStatus.java:5`.
- Cách tái hiện: Booking status `PAID` check-in trong 7 ngày, notification upcoming không xuất hiện.
- Ảnh hưởng: Mất reminder khách hàng.
- Hướng khắc phục: Dùng backend notification records hoặc map `PAID` là confirmed.
