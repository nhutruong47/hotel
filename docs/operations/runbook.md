# Runbook vận hành — Nhu Villas

Mỗi alert trong `ops/prometheus/alerts.yml` trỏ tới một mục trong tài liệu này. Các lệnh bên dưới chạy trên VPS, trong thư mục app, với alias sau:

```bash
alias dc='docker compose --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml'
```

## BackendDown

**Triệu chứng**: Prometheus không scrape được `backend:8081` trong 2 phút. Trang web thường trả 502.

1. Xem trạng thái và log khởi động: `dc ps backend` và `dc logs --tail=300 backend`.
2. Nếu Flyway dừng với lỗi `Vxx aborted: ...`, đó là preflight của migration (dữ liệu vi phạm ràng buộc mới).
   - Đọc thông báo, sửa dữ liệu một cách có chủ đích, rồi `dc up -d backend`.
   - Hoặc quay lại image trước: `REGISTRY=... sh ops/deploy/deploy.sh <sha-trước>`.
3. Nếu gặp `OutOfMemoryError` hoặc container bị kill do OOM: tăng `deploy.resources.limits.memory` của backend, hoặc kiểm tra alert `JvmHeapPressure`.
4. Nếu DB không lên: xử lý theo mục [PostgresDown](#postgresdown). Alertmanager đã chặn alert BackendDown trùng lặp trong trường hợp này.

## PostgresDown

1. Chạy `dc ps db` và `dc logs --tail=200 db`.
2. Nếu ổ đầy, xử lý theo mục [DiskAlmostFull](#diskalmostfull).
3. Nếu volume bị hỏng: xử lý theo mục [Restore from backup](#restore-from-backup).

## RabbitMQDown

Booking và thanh toán **vẫn chạy bình thường**. Email được giữ trong transactional outbox (ADR-0004) và tự gửi lại khi RabbitMQ lên lại.

1. Chạy `dc restart rabbitmq`.
2. Theo dõi `hotel_outbox_events{status="pending"}` giảm dần về 0.

## HighServerErrorRate

1. Grafana → bảng "API requests per second by status" để xác định thời điểm bắt đầu.
2. Tìm log lỗi: `dc logs --since=30m backend | grep '"log.level":"ERROR"'`.
3. Lấy `requestId` trong log rồi đối chiếu với `request_id` trong log nginx: `dc logs nginx | grep <id>`.
4. Nếu lỗi xuất hiện ngay sau một lần deploy, rollback về SHA trước bằng `deploy.sh`.

## OutboxBacklogStale

Có thông báo đã chờ quá 10 phút mà chưa được gửi.

1. Kiểm tra RabbitMQ (`dc ps rabbitmq`) và log của dispatcher: `dc logs backend | grep -i outbox`.
2. Nếu RabbitMQ không có vấn đề: kiểm tra `NOTIFICATION_OUTBOX_ENABLED` và cấu hình SMTP (`APP_EMAIL_ENABLED`, `SMTP_*`).

## OutboxDeadLetters

Có sự kiện đã hết số lần thử lại (trạng thái `FAILED`).

1. Xem các sự kiện lỗi:
   ```sql
   SELECT id, event_type, attempts, last_error FROM notification_outbox WHERE status = 'FAILED' ORDER BY created_at DESC LIMIT 20;
   ```
2. Sửa nguyên nhân gốc (thường là SMTP), rồi đưa sự kiện về hàng chờ:
   ```sql
   UPDATE notification_outbox SET status = 'PENDING', attempts = 0, available_at = now() WHERE id IN (...);
   ```

## PaymentsNeedReconciliation

Đã nhận tiền nhưng không tự áp được vào booking: tiền về trễ, trùng, hoặc sai số tiền (ADR-0009 Q4).

1. Vào Admin → Analytics → Payments: dòng có badge **Needs reconciliation** kèm lý do. Hoặc gọi API `GET /api/v1/payments?reviewRequired=true` (MANAGER trở lên).
2. Xử lý tùy trường hợp:
   - **Thiếu tiền**: liên hệ khách để chuyển phần còn thiếu, hoặc hoàn lại.
   - **Thừa hoặc trùng**: hoàn phần thừa bằng nút **Refund**. Với chuyển khoản, việc này tạo yêu cầu hoàn thủ công; xem mục [ManualRefundsWaiting](#manualrefundswaiting).
   - **Tiền về khi booking đã hết hạn**: nếu villa còn trống, ADMIN tạo lại booking cho khách. Nếu không, hoàn tiền.

## ManualRefundsWaiting

Có yêu cầu hoàn tiền chuyển khoản đã chờ quá 3 ngày. SLA mục tiêu là 5 ngày làm việc (ADR-0009 Q5).

1. Vào Admin → **Manual refunds awaiting bank transfer**. Màn hình hiển thị số tài khoản khách cung cấp.
2. Chuyển khoản qua ngân hàng, rồi nhập mã giao dịch và bấm **Confirm transfer**. Chỉ khi đó số tiền đã hoàn mới được cập nhật.
3. Nếu không hoàn (ví dụ khách đổi ý): bấm **Reject**.

## DiskAlmostFull

1. Xem dung lượng: `df -h` và `docker system df`.
2. Dọn image cũ: `docker image prune -a --filter "until=168h"`. Không dùng `docker volume prune`, vì lệnh này sẽ **xóa dữ liệu**.
3. Nếu thư mục backup đầy: giảm `BACKUP_RETENTION_DAYS` sau khi đã xác nhận backup off-site có bản đầy đủ.

## BackupMissing

1. Xem log của job backup: `dc logs --tail=100 backup`. Tìm dòng `FAILED`.
2. Chạy backup thủ công: `dc exec backup sh -c '. /etc/backup.env && sh /ops/backup/pg-backup.sh'`.
3. Nguyên nhân thường gặp: ổ đầy, DB không truy cập được, `BACKUP_OFFSITE_CMD` lỗi (lệnh này chạy trước khi ghi metric thành công).

## Failed deploy

`deploy.sh` đã tự rollback image khi backend không đạt `healthy`. Riêng migration DB không được rollback tự động.

1. Xem log để biết migration nào đã chạy: `dc logs backend | grep -i flyway`.
2. Nếu bản cũ không chạy được trên schema mới, chọn một trong hai:
   - Chạy script `backend/src/main/resources/db/rollback/U<n>__*.sql` theo thứ tự ngược.
   - Khôi phục từ bản backup tạo ngay trước deploy (mục bên dưới).

## Restore from backup

Mục tiêu RTO ≤ 4 giờ. **Cần dừng ghi vào hệ thống.**

```bash
dc stop backend frontend nginx                     # dừng nhận booking/thanh toán
ls -1t /srv/nhu-villas/backups/hotel-*.dump | head  # chọn bản dump
DUMP=/backups/hotel-<timestamp>.dump
dc exec backup sh -c "cd /backups && sha256sum -c $(basename $DUMP).sha256"
dc exec backup sh -c ". /etc/backup.env && psql -d postgres -c 'DROP DATABASE hotel WITH (FORCE)' && psql -d postgres -c 'CREATE DATABASE hotel'"
dc exec backup sh -c ". /etc/backup.env && pg_restore --no-owner --no-privileges --exit-on-error -d hotel $DUMP"
dc up -d backend frontend nginx
```

Sau khi khôi phục:
- Đối chiếu các payment phát sinh sau thời điểm backup với dashboard Stripe và sao kê SePay.
- Ghi nhận lại các khoản tiền bị thiếu. Những khoản này sẽ xuất hiện dưới dạng webhook trùng hoặc trễ, đã được xử lý theo ADR-0009 Q4.
