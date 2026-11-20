# Architecture Decision Records

Định dạng: Status · Date · Context · Problem · Options Considered · Decision · Consequences · Trade-offs. Mỗi ADR giải thích vì sao lựa chọn phù hợp **riêng cho Nhu Villas** (single-property, đội nhỏ, tiền thật qua Stripe/SePay).

| ADR | Tiêu đề | Status | Ngày |
|---|---|---|---|
| [0001](0001-modular-monolith.md) | Modular monolith với ranh giới module được ArchUnit kiểm soát | Accepted | 2026-11-20 |
| [0002](0002-single-flyway-postgresql-track.md) | Một track migration Flyway duy nhất cho PostgreSQL | Accepted | 2026-11-20 |
| [0003](0003-double-booking-prevention.md) | Chống double-booking: pessimistic lock + exclusion constraint | Accepted | 2026-11-20 |
| [0004](0004-transactional-outbox-rabbitmq.md) | Transactional outbox + RabbitMQ (bổ sung DLQ, consumer idempotency) | Accepted | 2026-11-20 |
| [0005](0005-session-auth-spring-session-redis.md) | Giữ server session, chuyển sang Spring Session Redis | Accepted | 2026-11-20 |
| [0006](0006-testcontainers-integration-testing.md) | Testcontainers cho integration test | Accepted | 2026-11-20 |
| [0007](0007-docker-compose-vps-deployment.md) | Triển khai bằng Docker Compose trên VPS | Accepted | 2026-11-20 |
| [0008](0008-single-property-multi-property-ready.md) | Single-property, multi-property ready | Accepted | 2026-11-20 |
| [0009](0009-business-policy-decisions.md) | Chốt chính sách nghiệp vụ Q1–Q11 (day-use, no-show, sửa booking, đối soát, hoàn tiền thủ công, VND, VAT, lưu trữ, min/max stay) | Accepted | 2026-11-20 |
| [0010](0010-p2-schema-hardening-scope.md) | Phạm vi P2: siết schema tại DB (V15–V20), một track Flyway, giữ `Integer` id ở Java | Accepted | 2026-11-20 |

## Quy trình

1. Tạo file `NNNN-ten-ngan.md` bằng số tiếp theo, Status `Proposed`.
2. Review trong PR; khi merge chuyển `Accepted`.
3. Không sửa nội dung ADR đã Accepted; thay đổi quyết định bằng ADR mới với `Supersedes: ADR-xxxx` và cập nhật Status cũ thành `Superseded by ADR-yyyy`.

## ADR dự kiến

> Số migration theo đánh số lại 2026-11-20 (V14 đã dùng cho P1). ADR-0008 (Accepted) vẫn ghi `V19` cho bảng `properties` — số thực tế dự kiến nay là `V20`, xem [database-design.md §4](../architecture/database-design.md#4-thay-đổi-schema-mục-tiêu-p2-và-sau-đó).

| Chủ đề | Khi nào |
|---|---|
| Chuẩn hóa `TIMESTAMP` vs `TIMESTAMPTZ` toàn hệ thống | P2 (V18) |
| ~~Chính sách xử lý thanh toán trùng (auto-refund vs đối soát tay)~~ | Đã quyết trong ADR-0009 Q4 (đối soát tay, không auto-refund) |
| Mã hóa dữ liệu ngân hàng hoàn tiền (key management) | P2 (V24) |
| Mô hình housekeeping & role Housekeeper | P4 |
| Observability stack (Prometheus/Grafana/Loki vs SaaS) | P5 |
