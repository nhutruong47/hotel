# ADR-0002: Một track migration Flyway duy nhất cho PostgreSQL

- **Status**: Accepted
- **Date**: 2026-11-20
- **Liên quan**: [database-design.md](../architecture/database-design.md)

## Context

Hiện có ba "thế hệ" cơ sở dữ liệu cùng tồn tại:
- PostgreSQL + Flyway `db/postgresql` (V1, V4–V12) cho profile `prod` (`application-prod.properties:40-43`).
- H2 + Flyway `db/migration` (V1–V3, V6–V11) kết hợp `ddl-auto=update` cho profile `dev`. Profile `test` dùng H2 với `create-drop`.
- PostgreSQL local qua profile `postgres` nhưng **tắt Flyway** và dùng `ddl-auto=update` (`application-postgres.properties:10,16`).

Ngoài ra còn các script SQL Server chạy tay (`database/migration.sql`, `database/update_constraint.sql` và bản sao trong `backend/`) chứa status đã lỗi thời. CI chỉ chạy H2, nên migration PostgreSQL chưa bao giờ được kiểm thử tự động.

## Problem

Có nhiều track thì schema ở dev/test khác prod. Lỗi chỉ lộ ra khi deploy, ví dụ V12 phải được thêm vào chỉ để "align entity schema". Các tính năng riêng của PostgreSQL mà ta cần (EXCLUDE với `btree_gist`, partial index, `daterange`) không chạy được trên H2.

## Options Considered

| Option | Nhận xét |
|---|---|
| A. Giữ H2 cho dev/test, PostgreSQL cho prod | Nhanh khi khởi động, nhưng không test được EXCLUDE/GiST/trigram và drift vẫn tiếp diễn |
| B. Liquibase thay Flyway | Có rollback, hỗ trợ nhiều DB; nhưng phải chuyển 10 migration đang chạy ổn, và dự án không cần đa DB |
| C. **Flyway, chỉ `db/postgresql`, dùng PostgreSQL ở mọi môi trường** (dev qua compose, test qua Testcontainers) | Hành vi đồng nhất với prod; dùng được trọn tính năng PostgreSQL |

## Decision

Chọn **C**.
- `db/postgresql` là track duy nhất.
- Profile `dev`/`postgres` bật Flyway và `ddl-auto=validate`.
- Test tích hợp chạy PostgreSQL 16 qua Testcontainers ([ADR-0006](0006-testcontainers-integration-testing.md)).
- Lưu trữ `db/migration`, `db/postgresql-legacy` và các script SQL Server vào `database/archive/` ở P2.
- Rollback được viết tay trong `db/rollback/U<n>__*.sql`, theo quy trình ở `database-design.md §9`.

## Consequences

- Dev cần Docker để chạy PostgreSQL. Thực tế compose đã có sẵn PostgreSQL.
- Các test đang dùng H2 sẽ chuyển dần sang PostgreSQL. Trong giai đoạn chuyển tiếp, H2 chỉ còn cho unit test slice không cần schema thật.
- SQL viết riêng cho PostgreSQL là được phép (EXCLUDE, `daterange`, `GREATEST`, `pg_trgm`).

## Trade-offs

- Mất khả năng chạy trên SQL Server, nhưng không còn nhu cầu đó: `legacy-mvc` đã ngừng.
- Test chạy chậm hơn H2 vài giây mỗi suite vì phải khởi động container. Có thể giảm bằng singleton container.
- Flyway Community không có undo; ta bù bằng rollback script viết tay và backup trước mỗi lần deploy.
