# 23. DOCUMENTATION GUIDE FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là hệ thống quy chuẩn về viết Tài liệu Dự án (Documentation). Một dự án có source code xuất sắc nhưng thiếu Documentation thì dự án đó được coi là "không có khả năng bảo trì" (Unmaintainable). 

---

## 1. DOCUMENTATION STRUCTURE (CẤU TRÚC TÀI LIỆU TOÀN DIỆN)
Dự án phải bao phủ các lớp tài liệu sau:
1. **Dự án (Project-level):** `README.md` (Điểm bắt đầu).
2. **Kiến trúc (Architecture-level):** Sơ đồ, Quyết định thiết kế, Cấu trúc Database.
3. **Mã nguồn (Code-level):** JSDoc, JavaDoc, Inline Comments.
4. **API (Interface-level):** Swagger/OpenAPI.
5. **Tính năng (Feature-level):** Sổ tay Hướng dẫn User, Changelog.

## 2. README STANDARD (TIÊU CHUẨN README.MD)
File `README.md` ở thư mục gốc của Repo là bộ mặt của dự án, bắt buộc phải có:
- **Tên dự án & Tóm tắt:** Hệ thống này làm nhiệm vụ gì?
- **Badges:** Trạng thái Build (CI), Phiên bản, Code Coverage.
- **Tech Stack:** Frontend, Backend, Database, Cloud.
- **Getting Started:** Lệnh clone, cài đặt npm/composer, chạy env, lệnh build (Các bước phải copy-paste chạy được ngay).
- **Environment Variables:** Danh sách liệt kê và ý nghĩa của các biến trong `.env` (Tuyệt đối không điền value thật).
- **Quy tắc đóng góp (Contributing Guidelines):** Trỏ link sang `20_GIT_WORKFLOW.md`.

## 3. ADR (ARCHITECTURE DECISION RECORD)
Mỗi khi đưa ra một quyết định lớn về mặt kỹ thuật (Ví dụ: Đổi từ Redux sang Zustand, Đổi từ MySQL sang PostgreSQL), bắt buộc phải tạo một file ADR trong thư mục `docs/adr/`.
Cấu trúc ADR bao gồm:
- **Ngữ cảnh (Context):** Vấn đề đang đối mặt là gì?
- **Lựa chọn (Options):** Đã cân nhắc những công cụ/giải pháp nào?
- **Quyết định (Decision):** Tại sao lại chọn công cụ này?
- **Hệ quả (Consequences):** Lợi ích đạt được là gì và Rủi ro/Hạn chế phải đánh đổi (Trade-off) là gì?

## 4. API & DATABASE DOCUMENTATION
- **API Documentation:** Không viết tay. Bắt buộc tạo tự động bằng Swagger/OpenAPI Spec. URL của Docs phải dễ truy cập (Ví dụ: `/api/docs`).
- **Database Documentation:** Phải có một file ERD (Entity Relationship Diagram). Nếu dùng Mermaid/PlantUML, phải lưu file mã nguồn `.mmd` để có thể chỉnh sửa lại sau này. Bảng từ điển dữ liệu (Data Dictionary) định nghĩa ý nghĩa của từng Enum/Trạng thái phải được lưu trên Wiki.

## 5. CHANGELOG (NHẬT KÝ THAY ĐỔI)
Duy trì một file `CHANGELOG.md` theo chuẩn "Keep a Changelog". Phân loại sự thay đổi theo:
- `Added`: Các tính năng mới.
- `Changed`: Thay đổi hành vi của tính năng cũ.
- `Deprecated`: Sắp bị loại bỏ trong tương lai.
- `Removed`: Đã bị xóa hoàn toàn.
- `Fixed`: Đã sửa lỗi (Bug fixes).
- `Security`: Sửa các lỗ hổng bảo mật.

## 6. MARKDOWN CONVENTION (QUY CHUẨN ĐỊNH DẠNG MARKDOWN)
- **Tiêu đề (Headings):** Chỉ có một `H1 (#)` duy nhất làm tên tài liệu. Phân cấp rõ ràng bằng `H2 (##)` và `H3 (###)`.
- **Đoạn mã (Code Blocks):** Bắt buộc chỉ định ngôn ngữ (Ví dụ: ```javascript, ```bash) để bật Highlighting.
- **Cảnh báo (Alerts/Callouts):** Sử dụng Blockquotes `> **LƯU Ý:**` để làm nổi bật các thông tin nguy hiểm.
- **Đường dẫn (Links):** Mọi tài liệu liên quan phải được chèn liên kết chéo (Cross-links) để tạo thành một mạng lưới Wiki.

## 7. ĐỊNH DẠNG HÌNH ẢNH & BIỂU ĐỒ (VISUAL ASSETS)
- **Screenshot Rule:** Hình ảnh nhúng vào Markdown phải được thu gọn size, nén định dạng `WebP` hoặc `PNG` tối ưu, và lưu trong thư mục `docs/assets/`.
- **Diagram Rule:** Cấm sử dụng hình ảnh JPG/PNG tĩnh cho các Sơ đồ kiến trúc/Sơ đồ luồng. Bắt buộc dùng `Mermaid JS` (hỗ trợ native trong Markdown GitHub) để vẽ sơ đồ, vì nó là Text nên có thể Version Control bằng Git.

## 8. COMPONENT DOCUMENTATION (TÀI LIỆU FRONTEND COMPONENT)
Đối với Thư viện Component riêng của dự án:
- Khuyến khích sử dụng **Storybook** làm tài liệu sống (Living documentation).
- Nếu viết Markdown, mỗi Component phải có tài liệu chỉ rõ:
  - Ý nghĩa chức năng.
  - Các Props bắt buộc (Required Props) và Tùy chọn (Optional).
  - Code mẫu (Usage Example) có thể copy.

---

## 9. DOCUMENTATION REVIEW CHECKLIST
Trước khi hoàn tất một Task/Tính năng mới, Developer phải kiểm tra:
- [ ] Tính năng này đã được cập nhật vào `CHANGELOG.md` chưa?
- [ ] File README.md có cần thêm biến Môi trường (Environment variables) mới nào vào mục hướng dẫn setup không?
- [ ] Nếu thay đổi Cấu trúc Database, file ERD Schema đã được cập nhật lại chưa?
- [ ] Nếu tạo thêm/Sửa API, Swagger/Postman Collection đã được cập nhật và ghi chú đúng Model trả về chưa?
- [ ] Nếu đó là một Quyết định công nghệ cốt lõi thay đổi Kiến trúc, đã có file ADR giải trình lý do chưa?
