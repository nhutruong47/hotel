# ADR-0011: Observability bằng Prometheus, Grafana và Alertmanager; log JSON ra stdout

- **Status**: Accepted
- **Date**: 2026-11-20
- **Liên quan**: [ADR-0007](0007-docker-compose-vps-deployment.md), [deployment-guide.md](../operations/deployment-guide.md), [runbook.md](../operations/runbook.md), rủi ro R-16 và R-17

## Context

Trước P5, hệ thống chỉ expose actuator `health,info`. Log ở dạng text tự do, không có metric hay cảnh báo nào. Vì vậy các sự cố như outbox bị kẹt, webhook lỗi hay backup không chạy sẽ không ai biết. Hệ thống chạy trên **một VPS duy nhất** do chủ villa tự vận hành, không có đội SRE chuyên trách.

## Problem

Cần phát hiện sự cố và chẩn đoán được nguyên nhân, nhưng chi phí và độ phức tạp vận hành phải vừa với một VPS.

## Options Considered

| Option | Ưu | Nhược |
|---|---|---|
| A. SaaS (Datadog, New Relic, Grafana Cloud) | Không phải tự vận hành | Chi phí hằng tháng; dữ liệu khách rời khỏi hệ thống |
| B. **Prometheus + Alertmanager + Grafana self-host, log JSON ra stdout** | Miễn phí, chuẩn de facto, cấu hình nằm trong git và được CI kiểm chứng | Tốn khoảng 1 GB RAM; phải tự backup Grafana và Prometheus (không bắt buộc) |
| C. ELK hoặc Loki cho log | Tìm kiếm log tập trung | Elasticsearch nặng; Loki thêm 2 service nữa. Chưa cần thiết cho một host |
| D. OpenTelemetry tracing (Tempo, Jaeger) | Trace phân tán | Hệ thống là monolith một tiến trình; `requestId` xuyên nginx → backend là đủ |

## Decision

Chọn **B**. Loki/ELK và tracing hoãn lại cho tới khi có từ hai host trở lên.

- **Metrics**: Micrometer kèm `micrometer-registry-prometheus`.
  - `/actuator/prometheus` chạy trên **port quản trị nội bộ 8081**, không publish và không đi qua nginx.
  - Mặc định vẫn chỉ mở `health,info` trên port chính, để Render không bị ảnh hưởng.
  - Có histogram cho `http.server.requests`.
  - Gauge nghiệp vụ trong `OperationalMetrics`: outbox (pending, failed, tuổi bản ghi cũ nhất), payment cần đối soát, refund thủ công đang chờ, booking chờ thanh toán.
- **Exporter**: `postgres-exporter`, `node-exporter` (đĩa, kèm *textfile collector* cho metric backup), và plugin prometheus có sẵn của RabbitMQ.
- **Cảnh báo**: 13 rule trong `ops/prometheus/alerts.yml`, mỗi rule trỏ tới một mục trong runbook. Alertmanager gửi email; cấu hình chứa secret nên được tạo từ file `.example`.
- **Log**: ở profile `prod` (hoặc `json-logs`), backend ghi JSON theo chuẩn ECS ra stdout bằng `StructuredLogEncoder` của Spring Boot 3.4. nginx ghi JSON kèm `request_id`, giá trị này được chuyển tiếp vào backend qua header `X-Request-Id`. Driver `json-file` của Docker xoay vòng 20 MB × 5 file.
- **Backup**: chạy `pg_dump` hằng ngày, kiểm tra archive bằng `pg_restore --list`, rồi tạo checksum. Restore drill chạy hằng tháng. Metric `hotel_backup_last_success_timestamp_seconds` cấp dữ liệu cho alert `BackupMissing` (RPO 24 giờ).
- **CI** (`infra-validate`): chạy `docker compose config` cho cả 3 môi trường; kiểm tra không service dữ liệu nào publish port; chạy `nginx -t`, `promtool check config/rules`, `amtool check-config`, shellcheck, và một restore drill thật trên PostgreSQL 16.

## Consequences

- Mọi alert đều có quy trình xử lý ghi sẵn. Riêng alert `ManualRefundsWaiting` và `PaymentsNeedReconciliation` theo dõi trực tiếp các chính sách ở ADR-0009.
- Đã bắt được một lỗi thật nhờ test: `spring.main.lazy-initialization=true` ở prod khiến bean metrics không bao giờ được khởi tạo. Đã sửa bằng `@Lazy(false)`.
- Grafana và Prometheus chỉ bind vào `127.0.0.1`. Muốn xem phải mở SSH tunnel; đánh đổi ở đây là ưu tiên an toàn.

## Trade-offs

- Tìm kiếm log tạm thời bằng `docker compose logs | grep`. Chấp nhận được với một host; thêm Loki khi số host tăng.
- Nếu VPS sập hoàn toàn, hệ thống giám sát cũng sập theo, nên cần thêm một uptime check bên ngoài. Đây là hạng mục còn mở, xem release notes.
