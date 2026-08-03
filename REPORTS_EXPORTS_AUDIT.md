# REPORTS_EXPORTS_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Excel/PDF export | 70% | Backend API có; frontend admin chưa expose export buttons đầy đủ. | UI export, date validation, content-disposition filename. |
| Analytics reports | 65% | Revenue/occupancy/refunds API có; UI chưa dùng hết. | Charts + filters + pagination. |

## Bugs

### REPORT-001 - Medium - Report API chưa được frontend admin dùng đầy đủ
- Mô tả: `ReportApi` có `/admin/reports/excel`, `/pdf`, `/excel/room`, `/pdf/room`; frontend `API_PATHS.admin.reports` chỉ trỏ `/admin/reports`, AdminDashboard không gọi export endpoints.
- Nguyên nhân: API export chưa nối UI.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/ReportApi.java:27`; `frontend/src/shared/api/client.ts:167`; `frontend/src/features/admin/AdminDashboardPage.tsx:103`.
- Cách tái hiện: Vào admin UI, không có action export Excel/PDF từ các endpoint này.
- Ảnh hưởng: Tính năng export backend chưa hoàn thiện với người dùng cuối.
- Hướng khắc phục: Add report tab/buttons, date range, room selector, download handling.

### REPORT-002 - Medium - Scheduler cleanup audit log là placeholder
- Mô tả: `cleanupOldAuditLogs` chỉ log completed, comment nói would call `AuditLogService`.
- Nguyên nhân: Chưa implement retention cleanup.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/scheduler/BookingScheduler.java:142`.
- Cách tái hiện: Chạy job; không có delete/archive audit log nào.
- Ảnh hưởng: DB audit_logs tăng vô hạn.
- Hướng khắc phục: Implement retention policy có cấu hình, archive trước xóa nếu cần compliance.
