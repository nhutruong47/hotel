# WISHLIST_AUDIT

> Resolution update (2026-09-11): WISHLIST-001 is fixed with per-user
> pessimistic locking plus idempotent PUT/DELETE endpoints and a concurrent
> integration test. WISHLIST-002 is fixed with `WishlistItemResponse`; the
> owner entity is no longer serialized by the API.

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Wishlist list/toggle | 75% | Race duplicate chưa xử lý user-friendly. | Catch unique constraint, return stable state. |
| Frontend account wishlist | 80% | Có UI account; chưa rõ icon toggle trên villa list/detail phủ đủ. | Add optimistic update + error rollback. |

## Bugs

### WISHLIST-001 - Medium - Toggle wishlist race có thể tạo duplicate exception
- Mô tả: Service check existing rồi save mới; DB có unique `(user_id, room_id)`.
- Nguyên nhân: Check-then-insert không lock hoặc catch duplicate.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/service/WishlistService.java:40`; `hotel/src/main/resources/db/migration/V1__init.sql:256`.
- Cách tái hiện: Gửi 2 request POST `/api/v1/wishlist/toggle` song song cùng user/room khi chưa có wishlist.
- Ảnh hưởng: Một request có thể fail 500/constraint violation.
- Hướng khắc phục: Catch `DataIntegrityViolationException`, re-read state; hoặc atomic upsert.

### WISHLIST-002 - Low - Wishlist API trả entity trực tiếp
- Mô tả: `WishlistApi.list` trả `WishlistItem` entity gồm nested user/room.
- Nguyên nhân: Không có DTO response.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/WishlistApi.java:26`.
- Cách tái hiện: GET `/api/v1/wishlist`, inspect JSON.
- Ảnh hưởng: Contract phụ thuộc entity, nguy cơ leak user fields nếu serialization thay đổi.
- Hướng khắc phục: `WishlistItemDTO` chỉ gồm `id`, room summary, createdAt.
