# P2 Release Notes — Database hardening (V15–V20)

- **Nhánh**: `feature/p2-database` (dựa trên `feature/p1-security-payment`)
- **Ngày**: 2026-11-20
- **Quyết định**: [ADR-0010](../adr/0010-p2-schema-hardening-scope.md) · thiết kế: [database-design.md §4](../architecture/database-design.md)

## 1. Thay đổi schema

| Migration | Nội dung | Thay đổi dữ liệu |
|---|---|---|
| V15 | `rooms.slug` UNIQUE; `rooms.version` NOT NULL DEFAULT 0; 8 index FK còn thiếu | `UPDATE rooms SET version = 0 WHERE version IS NULL` (kỹ thuật) |
| V16 | `bookings.voucher_id` / `promotion_id` (FK, `ON DELETE SET NULL`); FK `bookings.approved_by → users` | Backfill id từ `applied_voucher_code` (cột cũ giữ nguyên) |
| V17 | CHECK tiền ≥ 0, rating 1..5, guests > 0, enum `payments`/`contact_messages`/`villa_maintenances`/`villa_inspections`/`booking_status_transitions`, `rooms` (giá, min/max stay, capacity); UNIQUE review/booking; UNIQUE promo code (không phân biệt hoa thường) | Không |
| V18 | `villa_inspections.id/room_id/inspector_id` → `BIGINT` | Không (mở rộng kiểu) |
| V19 | `created_at/updated_at/created_by/updated_by` cho rooms, room_types, amenities, vouchers, promotions, faq_items, villa_inspections, blogs, villa_maintenances | Không |
| V20 | Bảng `properties` + dòng `NHU-DALAT` (id 1); `rooms.property_id` NOT NULL DEFAULT 1; `vouchers/promotions.property_id` nullable | Seed 1 property |

Mọi migration có **preflight**: nếu dữ liệu hiện tại vi phạm, Flyway dừng với thông báo nêu bảng, quy tắc và số dòng (V15, V16 liệt kê id). Ứng dụng **không khởi động** cho tới khi dữ liệu được sửa có chủ đích — đây là hành vi mong muốn, không phải lỗi.

## 2. Checklist deploy

1. **Backup**: `pg_dump -Fc` ngay trước deploy, ghi lại tên file.
2. **Chạy preflight trên bản sao prod** (khuyến nghị): restore backup vào DB tạm, chạy ứng dụng với `SPRING_DATASOURCE_URL` trỏ DB tạm; nếu Flyway dừng ở V15/V16/V17, đọc thông báo và sửa dữ liệu thật trước khi deploy.
3. Deploy — Flyway tự áp V15→V20 khi khởi động.
4. Smoke test: tạo booking có voucher, kiểm tra `SELECT voucher_id FROM bookings ORDER BY id DESC LIMIT 1`; sửa một villa trong admin, kiểm tra `rooms.updated_by`.
5. Rollback (nếu cần): chạy `db/rollback/U20…U15` theo thứ tự ngược bằng `psql -v ON_ERROR_STOP=1 -f`, rồi deploy bản trước; U20 từ chối chạy nếu đã có hơn 1 property.

## 3. Thay đổi cho developer

| Trước | Sau |
|---|---|
| Profile `postgres`: Flyway tắt, `ddl-auto=update` (schema tự sinh, khác prod) | Flyway `db/postgresql` + `ddl-auto=validate` — **giống prod**. DB local cũ tạo bằng `update` cần **tạo lại** |
| Profile `dev` (H2): Flyway chạy track `db/migration` riêng | H2 là sandbox: Flyway tắt, schema từ entity |
| Track `db/migration` (H2), `db/postgresql-legacy`, script SQL Server | **Đã xóa** (git history vẫn giữ) |
| `docker-compose.yml` mount `./init-db.sql` (không tồn tại) | Mount `./database/init-db.sql` (tạo sẵn `btree_gist`, `pg_trgm`) — chỉ có hiệu lực với volume mới |
| Thêm giá trị enum Java | **Bắt buộc** thêm migration cập nhật CHECK tương ứng (V17) |

## 4. Kiểm chứng

- 227 unit/H2 test xanh.
- 15 PostgreSQL IT xanh trên PostgreSQL 16.6: `SchemaIntegrityIT` (slug unique, audit stamping, property mặc định, `voucher_id`, CHECK/UNIQUE), `PostgresMigrationIT` (latest = 20), `BookingIntegrityIT`, `DuplicatePaymentWebhookIT`, `ManualRefundIT`.
- Thủ công trên PostgreSQL 16.6: rollback U20→U15 → chèn slug trùng → V15 dừng (`rooms 1,9`) → sửa → V15/V16 áp → chèn status `BOGUS` → V17 dừng → sửa → V17–V20 áp.

## 5. Còn lại (không thuộc P2)

`payment_webhook_events`, `booking_idempotency` (tùy chọn), `shedlock`, mã hóa số tài khoản hoàn tiền, contract `applied_voucher_code`, đổi tên `bookings.modified_at`, `Integer → Long` theo module (P3), housekeeping (P4).
