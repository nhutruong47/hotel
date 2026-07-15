# 19. AI WORKFLOW FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là quy trình (Workflow) và quy tắc tương tác giữa Người dùng (User) và Trợ lý AI (AI Assistant) trong suốt vòng đời dự án. Mục tiêu là biến AI từ một "trợ lý thụ động" thành một "thành viên chủ động", có khả năng tự đánh giá, bám sát framework và thực thi code chính xác.

---

## 1. AI INITIALIZATION (KHỞI TẠO AI)
Mỗi khi bắt đầu một phiên làm việc (Session) mới, AI BẮT BUỘC phải thực hiện các bước sau trước khi thực hiện bất kỳ lệnh nào:
1. Đọc và ghi nhớ file `00_PROJECT_RULE.md` (Source of truth).
2. Quét nhanh cấu trúc thư mục hiện tại để nắm ngữ cảnh dự án (Context).
3. Đọc tóm tắt các công việc đã thực hiện trước đó (nếu có).
4. Xác nhận lại với User rằng AI đã sẵn sàng tuân thủ toàn bộ Framework.

## 2. FILE READING ORDER (THỨ TỰ ĐỌC FILE)
Khi nhận một task cụ thể, AI cần tự động tra cứu các file quy chuẩn liên quan theo thứ tự:
- **Task về Giao diện (UI):** Đọc `04_UI_SYSTEM.md` -> `08_COMPONENT_LIBRARY.md` -> UI Framework (VD: Tailwind config).
- **Task về Chuyển động (Motion):** Đọc `06_MOTION_SPEC.md` -> `05_MOTION_STORYBOARD.md`.
- **Task về Cấu trúc (Structure/API):** Đọc `07_FRONTEND_ARCHITECTURE.md` -> `18_SECURITY_GUIDE.md`.

## 3. PROMPT CONVENTION (QUY CHUẨN ĐẶT LỆNH)
User nên giao việc cho AI theo cấu trúc chuẩn sau để đạt độ chính xác cao nhất:
- **Ngữ cảnh (Context):** "Tôi đang làm trang chi tiết Villa..."
- **Mục tiêu (Goal):** "...cần tạo component Image Gallery."
- **Ràng buộc (Constraints):** "...dùng Tailwind v4, không dùng library ngoài, hỗ trợ responsive di động."
- **Tham chiếu (Reference):** "...bám sát chuẩn Masonry trong `08_COMPONENT_LIBRARY.md`."

## 4. SPRINT WORKFLOW (QUY TRÌNH CHẠY SPRINT)
Áp dụng phương pháp làm việc cắt lớp nhỏ (Agile/Iterative) để tránh rủi ro AI "ảo giác" (Hallucinate) ra một đống code rác:
1. **Phân tích (Analyze):** AI tóm tắt lại hiểu biết về Task và đưa ra kế hoạch (Implementation Plan).
2. **Review:** User phê duyệt (Approve) kế hoạch.
3. **Thực thi (Execute):** AI viết code.
4. **Kiểm tra (Verify):** AI chạy test (nếu có tool) hoặc review lại code vừa sinh ra.
5. **Báo cáo (Report):** AI đưa ra Walkthrough tóm tắt những gì vừa làm.

## 5. TASK TEMPLATE (MẪU QUẢN LÝ TASK)
Mọi công việc phức tạp phải được chia nhỏ thành Task List. Khuyến khích AI sử dụng tính năng tạo Artifact `task.md` với định dạng:
```markdown
- [x] Bước 1: Setup component.
- [/] Bước 2: Tích hợp API (Đang thực hiện).
- [ ] Bước 3: Đổ dữ liệu ra UI.
- [ ] Bước 4: Tối ưu Accessibility & Performance.
```

## 6. AI CODING RULE (QUY TẮC VIẾT CODE CỦA AI)
- **Cấm phá vỡ Base:** Cấm tự ý thay đổi Design System (Màu, Font) hoặc config gốc (`vite.config.ts`, `tailwind.config`) nếu không có lệnh rõ ràng.
- **Micro-commits:** Cứ xong một task nhỏ, yêu cầu User kiểm tra hoặc AI tự động đánh dấu hoàn tất. Đừng sửa 10 file một lúc rồi hỏi "Tôi làm đúng không?".
- **Bảo tồn comments:** AI không được xóa các comment giải thích logic cũ của dự án khi update code.

## 7. AI MEMORY STRATEGY (CHIẾN LƯỢC GHI NHỚ)
Để tránh việc AI bị "quên" (Context limit) trong một chuỗi chat dài:
- Cứ sau 5-10 lượt hội thoại lớn, AI nên chủ động **Tóm tắt (Summarize)** lại tiến độ hiện tại, những quyết định kỹ thuật đã chốt, và ghi đè vào một file `ARCHITECTURE_DECISION.md` hoặc `walkthrough.md` để lưu vết.
- Các hằng số (Constants) hoặc quy luật quan trọng phải được lưu trữ vào file vật lý trong thư mục `AI_PROJECT_FRAMEWORK` thay vì phụ thuộc vào bộ nhớ tạm của LLM.

## 8. REVIEW PROCESS & APPROVAL FLOW (QUY TRÌNH KIỂM DUYỆT)
- **AI Tự kiểm duyệt (Self-Critique):** Ngay sau khi viết xong code, AI phải giả vờ đóng vai Reviewer để đối chiếu với `00_PROJECT_RULE.md`. Nếu phát hiện vi phạm, tự động sửa ngay trong lượt chat đó trước khi trình bày cho User.
- **User Phê duyệt (Approval Flow):** Đối với các thay đổi rủi ro cao (Xóa file, Sửa file config cốt lõi), AI phải dừng lại và yêu cầu User xác nhận "Proceed" (Tiếp tục) mới được thực thi tool sửa file.

## 9. AI COLLABORATION GUIDE (HƯỚNG DẪN AI HỢP TÁC VỚI NHAU)
Trong trường hợp dự án sử dụng nhiều Sub-agents (Các luồng AI xử lý riêng biệt):
- Phải truyền đạt lại (Hand-off) nguyên vẹn ngữ cảnh (Context) thông qua Artifacts.
- Agent Frontend và Agent Backend giao tiếp với nhau bằng các bản đặc tả API (API Spec/Swagger), không giao tiếp bằng lời nói suông.

---

## 10. AI REVIEW CHECKLIST
Bảng kiểm (Checklist) bắt buộc AI phải "lẩm nhẩm" (Thought process) trước khi gửi kết quả cho User:

- [ ] Code vừa tạo ra có đi ngược lại Triết lý Wabi-sabi Minimalist không?
- [ ] Code có vi phạm quy định cấm dùng `any` trong TypeScript không?
- [ ] Tính Responsive (Desktop/Tablet/Mobile) đã được giải quyết chưa?
- [ ] Giao diện có tương thích với Bàn phím (Tab) và tuân thủ Accessibility chưa?
- [ ] Nếu có Animation, đã bao bọc nó bằng `prefers-reduced-motion` chưa?
- [ ] Đã dọn dẹp các dòng `console.log` và code nháp chưa?
- [ ] (Quan trọng nhất) - Bạn có thực sự trả lời đúng trọng tâm câu hỏi của User không, hay đang "vẽ rắn thêm chân"?
