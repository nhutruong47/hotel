# ADR-0008: Single-property hiện tại, thiết kế sẵn cho multi-property

- **Status**: Accepted
- **Date**: 2026-11-20
- **Liên quan**: [business-analysis.md](../business/business-analysis.md) D1/D2, FR-20

## Context

Nhu Villas vận hành **một** khu villa. Mã nguồn và tài liệu nghiệp vụ đều mặc định như vậy (`docs/BUSINESS_ANALYSIS.md:12`): schema không có `hotels`, `branches` hay `tenant`.

Yêu cầu chuyển đổi nhắc tới "Hotel/Branch Management", và `docs/ENTERPRISE_TRANSFORMATION_PLAN.md` có tham vọng multi-tenant. Người dùng đã chọn (2026-11-20): giữ single-property, nhưng thiết kế sẵn để mở rộng.

## Problem

Cần tránh hai sai lầm:
- Xây multi-branch đầy đủ ngay bây giờ: tăng độ phức tạp ở RBAC, báo cáo, giá, mọi truy vấn, mà chưa có nhu cầu.
- Bỏ qua hoàn toàn: sau này phải sửa mọi bảng và mọi truy vấn.

## Options Considered

| Option | Nhận xét |
|---|---|
| A. Bỏ qua multi-property | Rẻ nhất bây giờ, nhưng chuyển đổi sau này rất tốn kém |
| B. Multi-branch đầy đủ ngay (property trên mọi bảng, RBAC theo branch, UI chọn branch) | Tăng 30–50% effort cho mọi tính năng, chưa có người dùng thật |
| C. Multi-tenant (SaaS, cô lập theo tenant) | Không phải mô hình kinh doanh hiện tại |
| D. **Thêm bảng `properties` và `rooms.property_id` (mặc định 1)**; các bảng con suy ra property qua `rooms`; chưa có RBAC/UI theo property | Tốn ít; đường mở rộng rõ ràng |

## Decision

Chọn **D**.
- **V19** (P2):
  - Thêm bảng `properties` (code, name, timezone, check_in_time, check_out_time, currency), seed 1 dòng.
  - `rooms.property_id NOT NULL DEFAULT 1` có FK.
  - `vouchers.property_id` và `promotions.property_id` nullable (NULL nghĩa là áp dụng toàn hệ thống).
- Module `property` sở hữu `properties`.
- Booking, payment và housekeeping **không** thêm `property_id`; khi cần thì suy ra qua `room_id` (một nguồn chân lý duy nhất).
- Giờ nhận/trả phòng dời dần từ cấp `rooms` lên `properties`, villa có thể override.
- Khi có property thứ hai, mới làm tiếp các bước (mỗi bước có ADR riêng):
  - role gắn theo property (`user_property_roles`);
  - báo cáo lọc theo property;
  - UI chọn property;
  - cân nhắc denormalize `property_id` vào `bookings` để báo cáo nhanh hơn.

## Consequences

- Truy vấn hiện tại không phải sửa.
- Code mới (housekeeping, reporting) nên nhận `propertyId` làm tham số, mặc định là property duy nhất, để không phải sửa chữ ký hàm sau này.
- Chưa có chuỗi khách sạn/chi nhánh: FR-20 được đánh dấu "Won't" ở giai đoạn này.

## Trade-offs

- Chấp nhận phần nhỏ "thiết kế trước" (bảng `properties` hiện chỉ có 1 dòng) để tránh migration phá vỡ sau này.
- Chưa giải quyết isolation giữa các property (RBAC, dữ liệu). Phải làm trước khi bán cho chủ khác hoặc vận hành property thứ hai.
