# Tài liệu Nhu Villas — Chỉ mục

Cập nhật: 2026-11-20. Tài liệu cũ **không bị xóa**. Khi thông tin mâu thuẫn, dùng thứ tự ưu tiên sau:

1. [`BOOKING_LIFECYCLE_AND_RBAC.md`](BOOKING_LIFECYCLE_AND_RBAC.md): nguồn chân lý về nghiệp vụ booking/payment/RBAC.
2. Bộ tài liệu kiến trúc 2026-11-20 (bảng bên dưới).
3. Mã nguồn.
4. Các tài liệu còn lại, có thể đã lỗi thời.

## 1. Bộ tài liệu kiến trúc 2026-11-20 (hiện hành)

| Tài liệu | Nội dung |
|---|---|
| [reports/architecture-assessment.md](reports/architecture-assessment.md) | Đánh giá kiến trúc, điểm chất lượng, phát hiện FE/BE/DB/Security/Infra, những gì giữ lại, refactor hoặc thiết kế lại |
| [reports/technical-debt.md](reports/technical-debt.md) | Danh sách nợ kỹ thuật TD-01..TD-50 (vị trí, tác động, effort, ưu tiên) và trạng thái xử lý P0/P1 |
| [reports/risk-register.md](reports/risk-register.md) | Danh sách rủi ro R-01..R-27, heat map, owner, trạng thái, bằng chứng đóng rủi ro P0/P1 |
| [architecture/current-architecture.md](architecture/current-architecture.md) | Kiến trúc hiện tại (C4 L1–L3, luồng booking/payment, bảo mật, hạ tầng) |
| [architecture/target-architecture.md](architecture/target-architecture.md) | Kiến trúc mục tiêu: modular monolith, module, C4, deployment, dependency |
| [architecture/database-design.md](architecture/database-design.md) | Schema, V13 (chống overlap) và V14 (đối soát payment, refund thủ công) kèm rollback, lộ trình V15–V25, index/locking/retention |
| [business/business-analysis.md](business/business-analysis.md) | Actor, business rule, workflow, FR/NFR, acceptance criteria; câu hỏi Q1–Q11 (đã chốt phần lớn trong ADR-0009) |
| [roadmap/implementation-plan.md](roadmap/implementation-plan.md) | Priority matrix, các phase P0–P7 (kết quả P0/P1), migration/cutover plan, Definition of Done |
| [adr/](adr/README.md) | Architecture Decision Records 0001–0011 |
| [adr/0009-business-policy-decisions.md](adr/0009-business-policy-decisions.md) | Chốt chính sách nghiệp vụ: day-use, no-show, sửa booking, đối soát, hoàn tiền thủ công, VND, VAT, lưu trữ, min/max stay |
| [operations/deployment-guide.md](operations/deployment-guide.md) | Triển khai VPS: compose dev/staging/prod, TLS, CI/CD, giám sát, backup |
| [operations/runbook.md](operations/runbook.md) | Xử lý từng alert, failed deploy, khôi phục từ backup |
| [operations/p2-release-notes.md](operations/p2-release-notes.md) | Release notes P2: migration V15–V20, preflight, checklist deploy/rollback, thay đổi profile cho developer |
| [operations/p1-release-notes.md](operations/p1-release-notes.md) | Release notes P1: thay đổi API, biến môi trường mới, checklist deploy (chuyển giá sang VND, SMTP), thay đổi người dùng thấy |

## 2. Tài liệu hiện có và trạng thái

| Tài liệu | Mục đích | Trạng thái |
|---|---|---|
| [BOOKING_LIFECYCLE_AND_RBAC.md](BOOKING_LIFECYCLE_AND_RBAC.md) | Vòng đời booking, payment integrity, RBAC | ✅ Hiện hành (nguồn chân lý) |
| [API_STANDARDS.md](API_STANDARDS.md) | Hợp đồng controller/envelope (có architecture test) | ✅ Hiện hành |
| [API_DOCUMENTATION.md](API_DOCUMENTATION.md) | Mô tả REST API | ⚠ Cần đối chiếu: response admin/payment đổi sang DTO ở P0; endpoint/ mã lỗi mới của P1 chỉ có trong [operations/p1-release-notes.md](operations/p1-release-notes.md) §2 |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Layering, xử lý lỗi | ⚠ **Một phần lỗi thời**: mục DB (khoảng dòng 19, 102–107, 170) nói `ddl-auto=update`, Flyway chưa bật, SQL Server. Xem `architecture/database-design.md` |
| [ARCHITECTURE_DECISION.md](ARCHITECTURE_DECISION.md) | Chọn runtime frontend + REST | ⚠ Đường dẫn cũ (`hotel/`, `src/`, nay là `backend/`, `legacy-mvc/`) |
| [BUSINESS_ANALYSIS.md](BUSINESS_ANALYSIS.md) | Gap analysis cũ | ⚠ **Lỗi thời**: mô tả trước khi có payment, STAFF/MANAGER, Next.js. Thay thế bởi `business/business-analysis.md` |
| [BUSINESS_AUDIT.md](BUSINESS_AUDIT.md) | Issue log 07/2026 | 🗄 Lịch sử |
| [ENTERPRISE_REPORT.md](ENTERPRISE_REPORT.md) | Báo cáo hardening 14 phase | 🗄 Lịch sử |
| [ENTERPRISE_TRANSFORMATION_PLAN.md](ENTERPRISE_TRANSFORMATION_PLAN.md) | Kế hoạch 10 phase, hướng multi-tenant | ⚠ Thay thế bởi `roadmap/implementation-plan.md` và ADR-0008 (không làm multi-tenant) |
| [FINAL_COMPREHENSIVE_REVIEW_REPORT.md](FINAL_COMPREHENSIVE_REVIEW_REPORT.md) | Review capstone | ⚠ Sơ đồ migration V1→V11 không khớp file thật |
| [FINAL_PROJECT_INTERVIEW_QA.md](FINAL_PROJECT_INTERVIEW_QA.md) | Hỏi đáp bảo vệ đồ án | 🗄 Lịch sử |
| [LUXURY_EXPERIENCE_BLUEPRINT.md](LUXURY_EXPERIENCE_BLUEPRINT.md) | Nguồn chân lý UI | ✅ Hiện hành (UI) |
| [UI_QUALITY_CHECKLIST.md](UI_QUALITY_CHECKLIST.md) | Scorecard UI | ✅ Hiện hành (UI) |
| [MASTER_PROJECT_PLAN.md](MASTER_PROJECT_PLAN.md) | Epic/sprint | ⚠ Thay thế bởi `roadmap/implementation-plan.md` |
| [PROJECT_BIBLE.md](PROJECT_BIBLE.md) | Đặc tả sản phẩm | ⚠ Chương 13 dùng status cũ (Pending/Approved/Rejected). Chương 15 dùng làm tham khảo cho audit fields |
| [PROJECT_REPORT.md](PROJECT_REPORT.md), [presentation-script.md](presentation-script.md), [project-documentation.md](project-documentation.md) | Tài liệu thời Thymeleaf | 🗄 Lịch sử |
| [SPRINT_1_1_REPO_AUDIT.md](SPRINT_1_1_REPO_AUDIT.md) | Bản đồ repo | ⚠ Lỗi thời một phần |
| [chat-recommend-flow.md](chat-recommend-flow.md) | Luồng gợi ý AI (Ollama) | ⚠ Nay dùng Gemini |
| [i18n.md](i18n.md) | MessageSource backend | ✅ |
| [work-allocation.md](work-allocation.md) | Phân công nhóm | 🗄 Lịch sử |

## 3. Thông tin sai đã biết ngoài thư mục `docs/`

| Vị trí | Nội dung sai | Thực tế |
|---|---|---|
| `README.md` (root) dòng 8, 25, 37, 91 | PostgreSQL 17 | Compose dùng `postgres:16-alpine` |
| `README.md` dòng 6, 37, 76, 103 | Flyway V1–V11 chạy khi khởi động với profile `postgres` | Có V12 (và V13 ở P0); profile `postgres` **tắt** Flyway (`application-postgres.properties:16`), chỉ profile `prod` bật |
| `README.md` dòng 27 | "Strict FKs & GiST Indexes" | Không có GiST cho booking cho tới V13; thiếu một số FK |
| `README.md` dòng 35 | JJWT; role `CUSTOMER`, `INSPECTOR` | Không có thư viện JWT (xác thực bằng session); role là `USER`, `STAFF`, `MANAGER`, `ADMIN` |
| `.agents/skills/deploy/SKILL.md`, `.agents/skills/run-locally/SKILL.md` | SQL Server `HotelDB`, `hotel-0.0.1-SNAPSHOT.jar`, `d:\sab\hotel` | PostgreSQL, `hotel-backend.jar` |
| `.agents/rules/*.md` | Đường dẫn `hotel/src/...` | `backend/src/...`; nhiều mục đã có banner "resolved" |
| `.env.example` | `SPRING_DATASOURCE_URL` trỏ SQL Server | PostgreSQL; thiếu `DB_PASSWORD`, `STRIPE_*`, `SMTP_*` |

Các mục này sẽ được sửa ở P5 (docs và DevOps); tài liệu này không sửa chúng.
