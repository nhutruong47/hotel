# ADR-0001: Modular Monolith với ranh giới module được ArchUnit kiểm soát

- **Status**: Accepted
- **Date**: 2026-11-20
- **Deciders**: Architecture team
- **Liên quan**: [target-architecture.md](../architecture/target-architecture.md)

## Context

Nhu Villas là **một khu villa** với vài chục căn, lưu lượng ước tính vài nghìn booking mỗi năm. Đội phát triển khoảng 5 người (`docs/work-allocation.md`), và chi phí hạ tầng phải thấp (hiện đang chạy trên container 512 MB của Render).

Backend hiện là một Spring Boot app, tổ chức theo **feature package** (`booking`, `payment`, `room`, …). Ranh giới giữa các package bị vi phạm ở nhiều chỗ:
- `PaymentService` ghi thẳng vào `BookingRepository` (`payment/service/PaymentService.java:392,452`).
- `AdminApi` (781 dòng) gọi repository của nhiều module.
- `common/service` gom các thứ không liên quan với nhau.

## Problem

Cần một kiến trúc giúp code dễ bảo trì, test và mở rộng (thêm housekeeping, inventory, có thể nhiều property sau này). Đồng thời không được tăng gánh vận hành vượt quá sức của đội nhỏ.

## Options Considered

| Option | Ưu | Nhược với Nhu Villas |
|---|---|---|
| A. Giữ nguyên layered/feature package lỏng | Không tốn công | Coupling tiếp tục tăng; không có gì ngăn module gọi repository của module khác |
| B. **Modular monolith** + ArchUnit | Một deployable, transaction ACID xuyên module khi cần (booking + payment), refactor dần được | Phải giữ kỷ luật; ArchUnit chỉ phát hiện vi phạm lúc build |
| C. Modular monolith + Spring Modulith | Kiểm tra module, event publication registry, tài liệu tự sinh | Đòi hỏi package theo quy ước của Modulith (module ở top-level, nội bộ ở `internal`). Codebase hiện tại lệch nhiều nên phải di chuyển lượng lớn code một lần. Thêm dependency mới vào một codebase đang ổn định |
| D. Microservices (booking, payment, notification…) | Scale và deploy độc lập | Booking + payment cần nhất quán mạnh: tách ra thì phải dùng saga và distributed transaction. Phải vận hành nhiều service, nhiều DB và service discovery, trong khi tải hiện tại không đòi hỏi |

## Decision

Chọn **B: Modular monolith**. Các module là `identity`, `property`, `booking`, `guest`, `payment`, `housekeeping`, `inventory`, `notification`, `reporting`, `audit`, `content`, tổ chức theo cấu trúc `api / application / domain / infrastructure`.
- Module khác chỉ được gọi qua `<Module>Facade` hoặc domain event.
- ArchUnit test trong CI kiểm tra các quy tắc:
  - Không truy cập `..infrastructure..` của module khác.
  - `api` không phụ thuộc `infrastructure`.
  - Controller không trả về `@Entity`.
  - Không có vòng phụ thuộc giữa các module.
- Đánh giá lại Spring Modulith khi P3 hoàn tất, tức là khi package đã sát với quy ước của nó.

## Consequences

- Refactor được làm dần theo kiểu strangler, từng module một: `payment` → `booking` → `property` → các module còn lại. Mỗi bước giữ test hồi quy xanh.
- Booking và payment vẫn chung transaction được khi cần, ví dụ webhook vừa set payment `PAID` vừa set booking `PAID`.
- Sau này nếu một module cần tách ra (ví dụ notification), Facade và event đã là "API" sẵn, tách dễ hơn.

## Trade-offs

- Đổi lấy sự đơn giản khi vận hành, ta chấp nhận rằng mọi module scale cùng nhau và deploy cùng nhau.
- ArchUnit không ngăn được lỗi lúc viết code, chỉ chặn lúc CI, nên cần code review.
- Thêm lớp Facade làm tăng một chút boilerplate.
