# ADR-0010: Phạm vi P2 — siết schema tại DB, không đổi kiểu ID ở Java

- **Status**: Accepted
- **Date**: 2026-11-20
- **Liên quan**: [ADR-0002](0002-single-flyway-postgresql-track.md), [ADR-0008](0008-single-property-multi-property-ready.md), [database-design.md §4](../architecture/database-design.md)

## Context

Kế hoạch P2 ban đầu gồm bốn phần:
- Các migration V15–V20: ràng buộc, FK, CHECK, audit columns, bảng `properties`.
- Đổi kiểu `Integer → Long` cho 23 entity.
- Chuẩn hóa timestamp.
- Gỡ track migration H2 và các script SQL Server.

`PostgresMigrationIT` (P0) cho thấy Hibernate `ddl-auto=validate` **chấp nhận** id Java `Integer` trên cột `BIGINT`. Vì vậy "drift" Integer/BIGINT chưa gây lỗi khi chạy.

## Problem

Cần chọn phần nào của P2 cho nhiều độ an toàn nhất trên mỗi đơn vị rủi ro, mà không làm hệ thống mất khả năng chạy.

## Options Considered

| Option | Ưu | Nhược |
|---|---|---|
| A. Làm toàn bộ, gồm `Integer → Long` | Kiểu dữ liệu đồng nhất tuyệt đối | Đổi hàng trăm chữ ký method, `@PathVariable`, repository generic và test cùng lúc. Rủi ro hồi quy cao mà **không có lợi ích nghiệp vụ**: một resort không thể đạt 2,1 tỷ bản ghi. |
| B. **Siết schema tại DB (V15–V20) + một track Flyway; giữ `Integer` ở Java** | Mỗi bất biến quan trọng có ràng buộc DB kèm preflight. Thay đổi code nhỏ và kiểm thử được. | Còn lệch kiểu Java/DB (đã được `validate` chấp nhận) |
| C. Chỉ dọn dẹp | Rủi ro thấp nhất | Không đạt mục tiêu "Database có thiết kế hợp lý" |

## Decision

Chọn **B**:
- **V15**: `rooms.slug` UNIQUE, `rooms.version` NOT NULL, các index FK còn thiếu.
- **V16**: `bookings.voucher_id` và `promotion_id` có FK, backfill từ `applied_voucher_code`. Cột cũ được giữ lại theo mô hình expand/contract. Thêm FK `approved_by`.
- **V17**: CHECK cho tiền, rating, guests, các cột enum lưu dạng VARCHAR; UNIQUE review theo booking; UNIQUE promo code không phân biệt hoa thường.
- **V18**: đổi khóa `villa_inspections` sang `BIGINT`. Không đổi timestamp: TIMESTAMPTZ chỉ có ở bảng này và chưa có nhu cầu cụ thể.
- **V19**: thêm `created_at/updated_at/created_by/updated_by` cho các bảng do staff quản lý, ghi qua `AuditStampListener`. Id người thao tác lấy từ `SessionAuthBridgeFilter`. Không dùng Spring Data Auditing, vì các entity cũ đã có sẵn cách xử lý `createdAt` và association `created_by` riêng.
- **V20**: thêm bảng `properties` (seed `NHU-DALAT`), `rooms.property_id` NOT NULL mặc định 1, `vouchers.property_id` và `promotions.property_id` nullable.
- **Một track Flyway**: profile `postgres` dùng Flyway kèm `validate`, giống prod. `dev` (H2) và `test` (H2) tắt Flyway, schema sinh từ entity. Xóa `db/migration` (H2), `db/postgresql-legacy` và các script SQL Server (`migration.sql`, `update_constraint.sql` với trạng thái cũ, `cleanup_development_data.sql`).
- **Không** đổi `Integer → Long` trong P2. Chỉ xem xét lại khi một entity có khả năng vượt 10⁸ bản ghi, hoặc khi tách module ở P3 (đổi theo từng module).

## Consequences

- Mọi migration mới đều có preflight: gặp dữ liệu vi phạm thì dừng và liệt kê, không tự sửa. Mỗi migration có rollback `db/rollback/U<n>`. Đã kiểm thử trên PostgreSQL 16.6: U20→U15, preflight V15/V17 dừng đúng, rồi áp lại.
- Thêm giá trị enum mới ở Java thì **bắt buộc** phải có migration cập nhật CHECK tương ứng. Việc này nằm trong checklist review.
- Developer đang có DB PostgreSQL local được tạo bằng `ddl-auto=update` phải tạo lại DB, vì Flyway và `validate` không chạy được trên schema sinh tự do.
- `applied_voucher_code` vẫn được ghi song song. Bước contract (xóa cột) để một release sau, khi mọi đọc/ghi đã chuyển sang dùng id.

## Trade-offs

- Giữ `Integer` nghĩa là còn một điểm lệch kiểu giữa Java và DB, đổi lại tránh được một refactor rủi ro cao mà không có giá trị nghiệp vụ.
- CHECK theo enum làm việc thêm giá trị enum chậm hơn một bước. Đổi lại DB không bao giờ chứa trạng thái "lạ".
- H2 không còn chạy migration, nên mọi thứ phụ thuộc schema thật phải được kiểm chứng bằng các `*IT` trên PostgreSQL. CI đã chạy chúng bằng Testcontainers.
