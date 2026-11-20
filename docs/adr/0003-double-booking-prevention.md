# ADR-0003: Chống double-booking bằng pessimistic lock + PostgreSQL exclusion constraint

- **Status**: Accepted
- **Date**: 2026-11-20
- **Liên quan**: [database-design.md §3](../architecture/database-design.md#3-v13--booking-integrity-p0), BR-B03, BR-B09

## Context

Mỗi villa là một đơn vị tồn kho duy nhất: không có "loại phòng còn 5 phòng" như khách sạn thông thường. Vì vậy một lần đặt trùng là một sự cố thật với khách. `BookingService.createBooking` đã khóa row `rooms` bằng `PESSIMISTIC_WRITE` rồi chạy conflict query (`booking/service/BookingService.java:233-241`), và `BookingConcurrencyIntegrationTest` kiểm chứng điều này cho booking qua đêm.

Audit phát hiện booking day-use (`check_in_date = check_out_date`, giá 50%) tạo ra range rỗng nên không bao giờ khớp điều kiện overlap. Ngoài ra DB không có ràng buộc nào bảo vệ invariant này.

## Problem

Cần một cơ chế bảo đảm **không bao giờ** có hai booking hiệu lực chồng lịch trên cùng villa. Cơ chế phải đúng cả khi:
- nhiều request đến đồng thời;
- code có bug (như day-use ở trên);
- dữ liệu được ghi ngoài đường đi của ứng dụng (admin SQL, script, job mới).

## Options Considered

| Option | Ưu | Nhược |
|---|---|---|
| A. Chỉ lock ở ứng dụng (hiện trạng) | Đơn giản, đã có test | Một bug ở query là lọt ngay; không bảo vệ trước việc ghi ngoài ứng dụng |
| B. `SERIALIZABLE` isolation | Không cần lock thủ công | Phải bắt `40001` và retry ở mọi chỗ; khó suy luận; ảnh hưởng các transaction khác |
| C. Bảng `room_nights(room_id, night)` UNIQUE, mỗi đêm một dòng | Chạy trên mọi DB | Ghi N dòng cho mỗi booking; dễ lệch khi đổi ngày/hủy; day-use vẫn cần quy ước riêng |
| D. Redis distributed lock | Nhanh | Thêm điểm lỗi; vẫn không có bảo đảm ở DB; thừa với một property |
| E. **Giữ A + thêm EXCLUDE `btree_gist` trên `daterange`**, có điều kiện theo status | Bảo đảm tuyệt đối ở DB; chỉ thêm 1 constraint; lock ở ứng dụng vẫn cho thông báo lỗi thân thiện | Chỉ dùng được trên PostgreSQL ([ADR-0002](0002-single-flyway-postgresql-track.md) đã chấp nhận); phải đồng bộ danh sách status giữa code và constraint |

## Decision

Chọn **E**. Có hai lớp:
1. **Ứng dụng**: lock row `rooms` rồi conflict query với khoảng chiếm chỗ do `booking/model/Occupancy.java` tính. Day-use chiếm `[d, d+1)`, booking qua đêm chiếm `[check_in, check_out)`.
2. **Database** (V13):
   ```sql
   EXCLUDE USING gist (room_id WITH =,
       daterange(check_in_date, GREATEST(check_out_date, check_in_date + 1), '[)') WITH &&)
   WHERE (status NOT IN ('CANCELLED','EXPIRED','NO_SHOW'))
   ```
   kèm CHECK `check_out_date >= check_in_date`. Lỗi `23P01` được map thành 409 `ROOM_UNAVAILABLE` — cùng mã với kiểm tra ở service, để client xử lý một kiểu (`GlobalExceptionHandler.isBookingOverlapViolation`).

Cách xử lý day-use (chiếm trọn ngày) là quyết định tạm, chờ PO trả lời Q1 trong [business-analysis.md](../business/business-analysis.md).

## Consequences

- Kể cả khi lock ở ứng dụng có bug, DB vẫn từ chối booking thứ hai.
- Testcontainers bắt buộc phải có, vì H2 không chạy được EXCLUDE ([ADR-0006](0006-testcontainers-integration-testing.md)).
- Thêm status "không chiếm chỗ" mới thì phải viết migration cập nhật constraint. Điều này được ghi trong checklist review.
- Migration có preflight để dừng lại nếu dữ liệu cũ đang chồng lịch, không tự sửa.

## Trade-offs

- Chỉ chạy trên PostgreSQL.
- Ngữ nghĩa day-use bị cố định ở DB. Nếu sau này bán theo giờ (khung giờ trong ngày), phải chuyển sang `tsrange` với `check_in_at/check_out_at`. Đây là một thay đổi lớn hơn nhưng có đường đi rõ ràng.
- Lock row `rooms` tuần tự hóa các booking trên cùng villa. Ở quy mô một property thì không đáng kể.
