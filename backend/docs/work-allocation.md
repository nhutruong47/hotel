# Phân Chia Công Việc — Dự Án Hotel Booking System

Phiên bản: 1.0
Ngày tạo: 2026-01-27

Mục tiêu: Tài liệu này phân chia rõ ràng nhiệm vụ, đầu ra mong đợi và tiêu chí nghiệm thu cho các thành viên trong nhóm.

---

## Tổng quan nhanh
- Phúc: REVIEW & RATING SYSTEM
- Ý: REPORTING & ANALYTICS
- Bảo: USER PROFILE + 🔴 SECURITY
- Triều: Slide & Diagram (presentation + sơ đồ luồng)
- Như: Hoàn thiện các chức năng còn lại (remaining features + bug fixes)

## Chức năng chính (tóm tắt nhanh)
- Phúc — REVIEW & RATING SYSTEM:
  - Thiết kế & triển khai Review entity, service, controller, templates.
  - Tính toán rating trung bình, kiểm soát duplicate review, admin moderation.

- Ý — REPORTING & ANALYTICS:
  - Backend reporting endpoints, `ReportingService`, DTOs.
  - Admin dashboard (biểu đồ/bảng), CSV export cho báo cáo chính.

- Bảo — USER PROFILE & SECURITY:
  - Trang profile (cập nhật, đổi mật khẩu, avatar) và các cải tiến bảo mật (password policy, CSRF, session, account lockout).

- Triều — SLIDES & DIAGRAMS:
  - Chuẩn bị slide thuyết trình, diagrams (architecture, booking flow, ERD) và assets cho demo.

- Như — REMAINING FEATURES & BUG FIXES:
  - Hoàn thiện core flows (auth, booking, payment), UI polish, tests, CI và xử lý các bug/PR còn lại.

---

## Hướng dẫn chung
- Mỗi task: tạo branch tên theo mẫu `feat/<tên-tính-năng>-<tên>` hoặc `fix/<mô-tả>-<tên>`.
- Code phải có unit test (nếu có thể), commit rõ ràng, tạo PR và gán reviewer.
- Mỗi PR cần mô tả ngắn, ảnh chụp màn hình (UI), bước tái tạo (nếu bug), và checklist kiểm thử.
- Ưu tiên: bảo mật, xử lý lỗi, và trải nghiệm người dùng.

---

## 1) REVIEW & RATING SYSTEM — Phúc
Scope (mô tả):
- Thiết kế và triển khai hệ thống đánh giá (reviews) cho phòng.
- Cho phép user tạo, sửa, xóa đánh giá; hiển thị rating trung bình; trang quản lý review cho admin.

Subtasks:
- Model & Repository: `Review` entity, `ReviewRepository` (CRUD, findByRoom, findByUser).
- Service: `ReviewService` (create/update/delete, validate rating 1-5, kiểm soát duplicate reviews per booking).
- Controller: endpoints cho front-end: viết `ReviewController` với các route cần thiết.
- Frontend templates: form đánh giá, hiển thị list review & rating stars fragment (tái sử dụng `fragments/rating-stars.html`).
- Admin: trang admin để duyệt/xóa review (nếu có chứa nội dung không phù hợp).
- Tests: unit tests cho `ReviewService` (happy path + invalid rating + duplicate review).

Acceptance criteria:
- User đã đặt phòng mới được phép đánh giá phòng tương ứng.
- Rating lưu được (1..5), comment tối thiểu 5 ký tự.
- Trung bình rating phòng được tính chính xác và hiển thị trên trang phòng.
- Admin có khả năng xóa/ẩn review; hành vi này ghi log để audit.

Dependencies & notes:
- Dùng `Booking` để kiểm tra điều kiện review (ownership & completed booking).
- Tích hợp `fragments/rating-stars.html` sẵn có để giảm trùng lặp UI.

Estimated time: 3-5 ngày.

---

## 2) REPORTING & ANALYTICS — Ý
Scope (mô tả):
- Xây dựng báo cáo & dashboard analytics cho admin: doanh thu, occupancy, bookings theo ngày/tuần/tháng, top phòng, review summary.

Subtasks:
- Backend endpoints: `ReportingController` trả JSON cho các biểu đồ.
- Service layer: `ReportingService` với các queries tổng hợp (sử dụng JPQL/Criteria hoặc native SQL nếu cần tối ưu).
- DTOs: các object nhẹ trả về cho frontend (e.g., RevenueDto, OccupancyDto).
- Frontend: trang admin dashboard (dùng chart library nhẹ hoặc server-rendered table). Có thể dùng Chart.js hoặc render bảng tóm tắt.
- Export: CSV export cho 2 báo cáo chính (doanh thu, bookings list).
- Tests: unit tests cho `ReportingService` với dữ liệu mẫu.

Acceptance criteria:
- Dashboard hiển thị đúng số liệu cho khoảng thời gian chọn.
- CSV export mở được bằng Excel và có header rõ ràng.
- Queries hoạt động trong giới hạn dataset hiện tại (với ~thousands records) — nếu chậm, đưa vào backlog tối ưu hoá.

Dependencies & notes:
- Dùng `BookingRepository` và `RoomRepository` làm nguồn dữ liệu.
- Cần thảo luận chỉ số nào quan trọng (Ý đề xuất: doanh thu, occupancy rate, avg. length of stay, top 5 phòng theo doanh thu).

Estimated time: 4-6 ngày.

---

## 3) USER PROFILE + 🔴 SECURITY — Bảo
Scope (mô tả):
- Hoàn thiện trang profile (cập nhật thông tin, đổi mật khẩu, ảnh đại diện) + gia cố bảo mật (CSRF, session management, password policy, account lockout, validation).

Subtasks:
- Profile features: `ProfileController` + templates cho update profile, change password, upload avatar (tối đa kích thước & validate loại file).
- Security hardening:
  - Review `SecurityConfig` và cập nhật rules: CSRF, secure cookies, session fixation protection.
  - Enforce password policy (min length 8, complexity optional) và rate-limit endpoints quan trọng (login, forgot-password) — nếu chưa có, đưa note để tích hợp later.
  - Account lockout after N failed attempts (simple in-memory counter or DB-backed failedAttempts + reset policy).
  - Ensure sensitive pages require HTTPS (note trong config + docs).
- Tests: unit/integration tests cho change password flow và avatar upload validation.

Acceptance criteria:
- Profile update hoạt động, avatar upload lưu/hiển thị đúng đường dẫn, file type và size validated.
- Password change/forgot/reset an toàn (mã hóa BCrypt), password policy enforced.
- Login có cơ chế hạn chế brute-force và session bảo mật tối thiểu.

Dependencies & notes:
- Có thể reuse `EmailService` cho reset password.
- Nếu cần thư viện thêm cho rate-limit, báo sớm để lên plan thay đổi dependencies.

Estimated time: 4-7 ngày.

---

## 4) SLIDES & DIAGRAMS — Triều
Scope (mô tả):
- Chuẩn bị slide thuyết trình và diagram kiến trúc/luồng chính để trình bày trong buổi báo cáo (picking up the diagrams already in `docs` and polish them).

Subtasks:
- Slide deck (PDF/PowerPoint): nội dung project overview, workflow, demo screenshots, metrics, phân chia công việc, roadmap.
- Diagrams: system architecture (MVC), AI recommend flow, booking lifecycle, database ERD (ísolated high level diagram).
- Provide exportable PNG/PDF of diagrams and include them under `docs/diagrams/`.

Acceptance criteria:
- Slide deck ≤ 15 slides, có nội dung rõ ràng và visuals.
- Diagrams đủ để hiểu luồng cho người không viết code.

Estimated time: 1-2 ngày.

---

## 5) REMAINING FEATURES & BUG FIXES — Như
Scope (mô tả):
- Như đảm nhận toàn bộ các chức năng còn lại chưa có chủ: tích hợp UI, bug fixes, polishing, và merge-ready PRs.

Subtasks (mô tả chi tiết):
- Project audit & backlog grooming (Priority: High)
  - Kiểm tra tất cả issue mở, TODO/NOTE trong code, PR pending; lập danh sách ưu tiên theo impact (security > user-facing > cosmetic).
  - Xác định blockers cho các task của Phúc, Ý, Bảo — phối hợp để unblock.
  - Ghi lại tasks nhỏ (subtasks) để có PR nhỏ, dễ review.
  - Estimated: 1 ngày.

- Core user flows (Priority: High)
  - Đăng ký / đăng nhập: kiểm tra edge-cases (duplicate email, email verification, social auth nếu có), xác thực form, hiển thị lỗi rõ ràng.
  - Booking flow: kiểm tra availability, form validation, xử lý cạnh tranh booking (concurrency), trạng thái booking (pending/confirmed/cancelled), edge cases khi thanh toán thất bại.
  - Payment integration: validate callbacks/webhooks, xử lý trạng thái thanh toán, rollback/notify khi lỗi.
  - Estimated: 2-4 ngày (tùy scope các issue hiện tại).

- AI-recommend & Search polish (Priority: Medium)
  - Kiểm tra trang AI recommend (`ai-recommend.html`) — đảm bảo inputs/outputs hợp lý, xử lý empty results và thời gian chờ (show spinner, timeout user-friendly).
  - Search & filter: đảm bảo bộ lọc (date/price/guests) đúng và tương thích với booking availability.
  - Estimated: 1-2 ngày.

- Admin panels & management (Priority: Medium)
  - Kiểm tra admin pages (rooms, users, bookings, reviews) — CRUD hoạt động, phân quyền đúng.
  - Fix UI/UX inconsistencies: table columns, pagination, empty states, flash messages.
  - CSV exports & downloads testable.
  - Estimated: 1-2 ngày.

- Templates & Frontend consistency (Priority: Medium)
  - Chuẩn hoá fragments (header/footer/nav/rating-stars) để tái sử dụng.
  - Kiểm tra responsive behavior cho mobile/tablet; fix layout breakage.
  - Validate form client-side + server-side.
  - Estimated: 1-3 ngày.

- Email & Notifications (Priority: Medium)
  - Test tất cả email templates (verification, reset password, booking notifications) — placeholders đúng, links hoạt động.
  - Ensure `EmailService` handles failures gracefully (retry/logging).
  - Estimated: 0.5-1.5 ngày.

- File upload & media (Priority: Low/Medium)
  - Avatar upload, room images: validate file type, size limits, storage path, cleanup orphan files.
  - Serve static/uploads securely (no directory listing), sanitize filenames.
  - Estimated: 0.5-1.5 ngày.

- Validation, Error handling & Logging (Priority: High)
  - Standardize error pages & API error responses, user-friendly messages.
  - Ensure server-side validation everywhere (not only client-side).
  - Add/verify structured logs for critical flows (auth, booking, payments) to help debugging.
  - Estimated: 1-2 ngày.

- Tests & CI (Priority: High)
  - Add/maintain unit tests for bug-prone services (BookingService, UserService, Payment listener).
  - Add integration/smoke tests for critical flows (signup -> login, create booking -> payment success/fail, review flow).
  - Ensure tests run in CI (if CI exists) and PRs include green test run.
  - Estimated: ongoing; initial push 2-3 ngày.

- Security & privacy checklist (Priority: High)
  - Work with Bảo on security hardening tasks that require coordination (password policy, rate-limit endpoints, account lockout login attempts tracking).
  - Make sure secrets not checked into repo; scan config files for exposed credentials.
  - Estimated: 0.5-1 day to coordinate + fixes as needed.

- Performance & Scalability quick wins (Priority: Low)
  - Identify slow queries from code (Repository methods) and add indexes or optimize queries where trivial.
  - Cache frequently-read but rarely-changed values (e.g., room static metadata) if appropriate.
  - Estimated: 1-2 days (optional, backlog if time-constrained).

- Documentation & Demo readiness (Priority: Medium)
  - Update `README` or `docs` with local run instructions, env variables, common troubleshooting.
  - Prepare demo checklist + sample accounts and data for the presentation (Triều's slides).
  - Estimated: 0.5-1 day.

- Release & merge tasks (Priority: High)
  - Ensure each fix/feature is a small PR with clear description and test steps.
  - Coordinate merges to avoid conflicts; run a final smoke test on `main` after merges.
  - Tag release or create a release branch if needed for presentation/demo.

Acceptance criteria (cho rõ ràng):
- Core flows (register/login, booking, payment, profile) hoạt động từ đầu đến cuối trong môi trường local hoặc staging.
- Không có bug nghiêm trọng liên quan security hoặc data-loss trước khi demo.
- PRs nhỏ, có test/manual test steps, reviewed and merged.
- All email templates render correctly; CSV exports open in Excel.

Suggested working pattern & checklist cho Như (short checklist):
1. Chạy một audit nhanh: `mvn test` + local manual smoke test (signup -> booking -> pay -> review).
2. Lấy 3 issues priority cao nhất; xử lý từng cái một, mỗi cái 1 PR nhỏ.
3. Thêm/hoàn thiện tests cho mỗi bug fix.
4. PR: mô tả + steps + ảnh chụp màn hình (nếu UI) + reviewer assigned.
5. Sau merge: run a quick smoke test on `main`.

Branch naming & PR convention (nhắc lại):
- Branch: `fix/<mô-tả>-nhu` hoặc `feat/<tên-tính-năng>-nhu`.
- PR title: `[Fix|Feat] <short description> (Như)`.

Estimated total time: 1-2 tuần (tùy số lượng issue hiện có) — ưu tiên các bug/flows critical trước.

---

## Đồng bộ, timeline & giao tiếp
- Họp sync ngắn 15 phút mỗi ngày (standup) hoặc 2-3 lần/tuần nếu không tiện daily.
- Milestone ngắn: 2 tuần sprint cho các tính năng chính.
- Mốc gợi ý: tuần 1 — thiết kế + API skeleton; tuần 2 — implement + tests + PR.
- Giao tiếp: Slack/Teams (channel riêng), PR comments cho review, issues tracker cho bug.

---

## PR Checklist (áp dụng cho mọi PR)
- [ ] Mô tả rõ ràng mục tiêu PR.
- [ ] Tests (unit/integration) hoặc manual test steps.
- [ ] Lint/format theo project convention.
- [ ] Không chứa secrets/credentials.
- [ ] Đã chạy local smoke test (build + start + quick path test).

---

## Ghi chú cuối
Nếu ai cần thay đổi scope/đổi task, cập nhật file này và tag team lead để reassign.


---

Người tạo: Nhóm Hotel Booking System
Trạng thái: Draft — cập nhật theo tiến độ
