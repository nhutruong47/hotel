# ADR-0004: Transactional Outbox + RabbitMQ cho thông báo bất đồng bộ

- **Status**: Accepted (ghi nhận quyết định đã có, bổ sung DLQ và consumer idempotency)
- **Date**: 2026-11-20
- **Liên quan**: `docs/BOOKING_LIFECYCLE_AND_RBAC.md:66-79`, `db/postgresql/V8__notification_transactional_outbox.sql`

## Context

Mỗi khi trạng thái booking hay payment thay đổi, khách và admin phải nhận email (ví dụ xác nhận thanh toán, hủy, hết hạn). Gửi email ngay trong transaction gây ra hai vấn đề:
- Nếu transaction rollback, email vẫn đã đi.
- Nếu SMTP chậm, request bị chặn theo.

Hệ thống đã có `notification_outbox` (V8), một dispatcher khóa batch rồi publish lên RabbitMQ có publisher confirm, và `NotificationListener` để gửi email.

## Problem

Cần cơ chế giao thông báo tin cậy khi RabbitMQ hoặc SMTP tạm thời không hoạt động. Cơ chế phải không gửi email cho transaction đã rollback, phải có retry, có DLQ, và chịu được việc thông điệp bị giao trùng.

## Options Considered

| Option | Nhận xét |
|---|---|
| A. Gửi email đồng bộ trong service | Đơn giản nhưng sai khi rollback, chặn request, mất email khi SMTP lỗi |
| B. `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` | Không gửi khi rollback, nhưng nếu app crash sau commit thì mất sự kiện |
| C. **Outbox trong DB + dispatcher + RabbitMQ** (hiện trạng) | Bảo đảm at-least-once; sống sót qua lúc RabbitMQ ngừng; đã có code và test |
| D. Outbox + gửi SMTP trực tiếp từ dispatcher (bỏ RabbitMQ) | Bớt một thành phần, nhưng retry/DLQ phải tự viết; mất khả năng fan-out cho webhook/SMS sau này |
| E. Kafka / Debezium CDC | Thừa với quy mô hiện tại |

## Decision

Giữ **C** và bổ sung:
1. **DLQ**: `email.queue` có `x-dead-letter-exchange` trỏ tới `email.dlq`. Consumer retry có giới hạn, quá số lần thì đẩy vào DLQ. Có alert khi DLQ > 0.
2. **Consumer idempotency**: bảng `processed_messages(message_id PK, processed_at)`. Consumer bỏ qua `message_id` đã xử lý, giảm email trùng sau khi app crash giữa lúc broker đã confirm và DB chưa commit.
3. **Metrics**: số dòng outbox ở trạng thái `PENDING`/`FAILED`, tuổi của dòng cũ nhất, độ sâu DLQ (Prometheus).
4. Bỏ `WEBHOOK_QUEUE` không dùng, hoặc chỉ khai báo lại khi có consumer.

## Consequences

- Khi RabbitMQ ngừng: booking và payment vẫn commit, outbox giữ sự kiện, dispatcher retry với backoff (AC-08).
- Email có thể vẫn trùng trong cửa sổ rất hẹp; nội dung email phải chịu được việc gửi lặp (đã có trong `BOOKING_LIFECYCLE_AND_RBAC.md:77-79`).
- Có thêm bảng và job dọn dẹp.

## Trade-offs

- Tốn thêm thành phần vận hành (RabbitMQ). Đổi lại có retry/DLQ chuẩn và mở rộng được sang SMS/Zalo/webhook đối tác.
- Độ trễ email vài giây, tùy chu kỳ dispatcher. Chấp nhận được cho email giao dịch.
