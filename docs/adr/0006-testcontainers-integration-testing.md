# ADR-0006: Testcontainers cho integration test (PostgreSQL, sau đó RabbitMQ/Redis)

- **Status**: Accepted
- **Date**: 2026-11-20
- **Liên quan**: [ADR-0002](0002-single-flyway-postgresql-track.md), [ADR-0003](0003-double-booking-prevention.md)

## Context

Backend có khoảng 235 `@Test` trong 25 file. Phần lớn là unit test Mockito; 7 class `@SpringBootTest` chạy trên **H2** (`src/test/resources/application-test.properties`).

CI (`.github/workflows/ci.yml`) chạy `./mvnw test` mà không có PostgreSQL. Vì vậy:
- Migration `db/postgresql` chưa bao giờ được chạy trong CI.
- Drift `Integer` ↔ `BIGINT` chưa được kiểm chứng.
- Constraint EXCLUDE ở V13 không thể test được.

Máy phát triển hiện tại cũng không có Docker.

## Problem

Cần test tích hợp chạy đúng engine và đúng migration của production, và kiểm thử được:
- concurrency thật (nhiều connection, lock thật);
- webhook gửi trùng;
- kịch bản lỗi (DB ngừng, RabbitMQ ngừng).

## Options Considered

| Option | Nhận xét |
|---|---|
| A. Giữ H2 | Nhanh; nhưng sai ngữ nghĩa lock/EXCLUDE/`daterange` và không bắt được lỗi migration |
| B. Embedded PostgreSQL (zonky) | Không cần Docker; nhưng là binary riêng, khó đồng bộ version và không có RabbitMQ/Redis |
| C. PostgreSQL service trong GitHub Actions | Không cần thư viện; nhưng dev local phải tự chạy DB, khó dựng kịch bản lỗi (dừng DB giữa chừng) |
| D. **Testcontainers** (`spring-boot-testcontainers`, `@ServiceConnection`) | Dùng đúng image `postgres:16-alpine` như prod; mỗi suite một DB sạch; điều khiển được container cho chaos test; được Spring Boot 3.1+ hỗ trợ sẵn |

## Decision

Chọn **D**.
- Đặt tên `*IT`, chạy bằng `maven-failsafe-plugin` ở phase `verify`. Unit test vẫn chạy bằng `surefire` ở `test`.
- Base class `AbstractPostgresIT`:
  - `@Testcontainers(disabledWithoutDocker = true)` để máy không có Docker vẫn chạy được `mvn test`;
  - singleton container `postgres:16-alpine`;
  - Flyway `db/postgresql` với `ddl-auto=validate`.
- CI chuyển từ `./mvnw -B test` sang `./mvnw -B verify`. Runner `ubuntu-latest` đã có Docker.
- P6 bổ sung `RabbitMQContainer` và Redis (GenericContainer) cho các kịch bản AC-08 (RabbitMQ ngừng), retry thông báo, và Toxiproxy cho DB/gateway timeout.

## Consequences

- Test đầu tiên bắt buộc phải có là `PostgresMigrationIT`: V1 → V13 cộng với Hibernate validate.
- Thời gian CI tăng khoảng 1–2 phút.
- Dev không có Docker sẽ không chạy được IT ở local mà phải dựa vào CI. Ghi rõ điều này trong README/CONTRIBUTING.

## Trade-offs

- Đổi tốc độ lấy độ chính xác. Chấp nhận được, vì những lỗi đắt nhất của hệ thống (tiền, tồn kho) nằm đúng ở tầng DB.
- Phụ thuộc Docker trên CI runner. GitHub-hosted runner có sẵn Docker; self-hosted runner phải cài thêm.
