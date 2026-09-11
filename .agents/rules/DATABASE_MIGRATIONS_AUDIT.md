# DATABASE_MIGRATIONS_AUDIT

> Resolution update (2026-09-11): the invalid H2 migration indexes were
> replaced earlier; V9 enforces canonical unique email and V10 removes the
> PostgreSQL-only `password_hash` column that conflicts with the User entity.
> Clean PostgreSQL validation remains a release gate.

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Flyway baseline | 65% | V1 tương đối đầy đủ nhưng H2-oriented. | Validate trên target DB thực tế. |
| Performance indexes | 10% | V2 sai tên bảng/cột so với V1. | Rewrite V2 theo schema thật. |
| Constraints | 55% | Email không unique, booking overlap không có DB exclusion. | Unique email, DB-level booking overlap strategy. |
| Seed data | 60% | Dev seed có; prod excluded. | Production seed/migration rõ ràng. |

## Bugs

### DB-001 - Critical - V2 migration dùng tên bảng sai so với V1
- Mô tả: V2 tạo index trên `Booking`, `Payment`, `Users`, `AuditLog`, `Review`, `Notification`, `WishlistItem`; V1 tạo `bookings`, `payments`, `users`, `audit_logs`, `reviews`, `notifications`, `wishlists`.
- Nguyên nhân: Migration viết theo entity class/table legacy thay vì actual table names.
- File/Class/Method: `hotel/src/main/resources/db/migration/V2__add_performance_indexes.sql:7`; `hotel/src/main/resources/db/migration/V1__init.sql:176`; `hotel/src/main/resources/db/migration/V1__init.sql:276`.
- Cách tái hiện: Boot profile Flyway trên fresh DB chạy V1 rồi V2.
- Ảnh hưởng: Migration fail, app không boot production.
- Hướng khắc phục: Đổi V2 sang lowercase table names đúng, test migration trên clean Postgres/H2.

### DB-002 - Critical - V2 reference cột không tồn tại `booking_user_id` và `timestamp`
- Mô tả: V2 tạo `idx_payment_user ON Payment(booking_id, booking_user_id)` và `idx_audit_timestamp ON AuditLog(timestamp DESC)`, nhưng V1 payments không có `booking_user_id`, audit_logs có `created_at`.
- Nguyên nhân: Index spec không dựa schema thật.
- File/Class/Method: `hotel/src/main/resources/db/migration/V2__add_performance_indexes.sql:25`; `hotel/src/main/resources/db/migration/V2__add_performance_indexes.sql:34`; `hotel/src/main/resources/db/migration/V1__init.sql:276`; `hotel/src/main/resources/db/migration/V1__init.sql:326`.
- Cách tái hiện: Apply V2.
- Ảnh hưởng: Migration fail ngay cả nếu table names được sửa một phần.
- Hướng khắc phục: Remove invalid indexes; use `payments(booking_id)`, `audit_logs(created_at)`.

### DB-003 - High - `users.email` không unique dù service coi email là duy nhất
- Mô tả: V1 `email VARCHAR(255)` không unique, trong khi register check `findByEmail`.
- Nguyên nhân: Business uniqueness chỉ ở application layer.
- File/Class/Method: `hotel/src/main/resources/db/migration/V1__init.sql:19`; `hotel/src/main/java/com/hsf/hotel/service/UserService.java:60`.
- Cách tái hiện: Hai concurrent register cùng email.
- Ảnh hưởng: Duplicate email, reset password/verification sai.
- Hướng khắc phục: Unique index on normalized email; catch duplicate constraint.
