# 24. PROJECT READY CHECKLIST

> **TÀI LIỆU TIÊU CHUẨN:** Đây là Bảng kiểm soát chất lượng TỔNG THỂ (Master Checklist) cho toàn bộ dự án. Cấm Deploy lên Production nếu các hạng mục cốt lõi trong "DONE CHECKLIST" chưa được tích đủ. Quá trình làm việc chia làm 3 giai đoạn: Chuẩn bị (TODO), Đánh giá (REVIEW), và Hoàn tất (DONE).

---

## 1. UX (TRẢI NGHIỆM NGƯỜI DÙNG)
- **TODO Checklist:** Xây dựng Persona, thiết kế User Flow.
- **REVIEW Checklist:** Flow có bị thừa bước không? Pain points cũ đã giải quyết chưa?
- **DONE Checklist:**
  - [ ] User Journey không còn "điểm chết" khiến người dùng bối rối.
  - [ ] Empty State và Error Message đã thân thiện và có tính hướng dẫn.

## 2. UI & COMPONENT
- **TODO Checklist:** Thiết kế UI System, Xây dựng Atoms/Molecules Component.
- **REVIEW Checklist:** UI có đúng khoảng cách Base-8 không? Màu sắc có đồng bộ theo Token chưa?
- **DONE Checklist:**
  - [ ] 100% Component tuân thủ Design System (Không có mã màu/font kích thước lạ).
  - [ ] Mọi Component (Button, Input, Card) đã bao gồm Hover, Focus, Active, Disabled, Loading state.

## 3. MOTION & ANIMATION
- **TODO Checklist:** Xây dựng Motion Storyboard, Áp dụng Ease và Timing chuẩn.
- **REVIEW Checklist:** Scroll có bị khựng (Layout Shift) do Animation không? Parallax có gây chóng mặt không?
- **DONE Checklist:**
  - [ ] Không có giật lag, FPS ổn định ở mức 60.
  - [ ] Tắt hoàn toàn Animation vô nghĩa nếu user bật `prefers-reduced-motion`.

## 4. FRONTEND ARCHITECTURE
- **TODO Checklist:** Setup Folder Structure, State Management, Router.
- **REVIEW Checklist:** Feature Layers có bị import chéo (Circular dependency) không?
- **DONE Checklist:**
  - [ ] Bundle Size được cắt nhỏ bằng Code Splitting (React.lazy/Suspense).
  - [ ] Global Error Boundary đã được bọc quanh App để chống trắng trang.

## 5. BACKEND & API DESIGN
- **TODO Checklist:** Lên cấu trúc API (RESTful), Xác định Input/Output payload.
- **REVIEW Checklist:** Status Code đã chính xác chưa? Response có bị rò rỉ dữ liệu thừa (Ví dụ Password hash) không?
- **DONE Checklist:**
  - [ ] Mọi API đều có Data Validation ở tầng Backend.
  - [ ] Định dạng trả về chuẩn (Envelope: `data`, `meta`, `error`).

## 6. DATABASE DESIGN
- **TODO Checklist:** Lên sơ đồ ERD, tạo file Migration, Seeders.
- **REVIEW Checklist:** Khóa ngoại đầy đủ chưa? Các bảng đã chuẩn hóa (3NF) chưa?
- **DONE Checklist:**
  - [ ] Có đầy đủ Audit Columns (`created_at`, `updated_at`) và Soft Delete.
  - [ ] Các trường Search/Where quan trọng đã được đánh Index đúng cách.

## 7. SEO (TỐI ƯU TÌM KIẾM)
- **TODO Checklist:** Setup thẻ Meta, Open Graph, Cấu trúc Heading H1-H6.
- **REVIEW Checklist:** H1 có duy nhất một thẻ trên mỗi trang không? Search Intent có thỏa mãn chưa?
- **DONE Checklist:**
  - [ ] Robots.txt và Sitemap.xml tự động cập nhật được cấu hình đúng.
  - [ ] Alt text được gắn đủ cho hình ảnh nội dung.

## 8. ACCESSIBILITY (TRỢ NĂNG A11Y)
- **TODO Checklist:** Thiết lập ARIA Labels, Màu sắc tương phản (Contrast Ratio).
- **REVIEW Checklist:** Screen Reader đọc trang có bị ngắt quãng không?
- **DONE Checklist:**
  - [ ] Có thể sử dụng toàn bộ trang web chỉ bằng Bàn phím (Phím Tab, Enter).
  - [ ] Các Modal khi bật lên đã bẫy được Focus (Focus Trap) chưa?

## 9. PERFORMANCE (HIỆU NĂNG)
- **TODO Checklist:** Nén ảnh WebP, Lazy load image, Code splitting JS.
- **REVIEW Checklist:** Chỉ số LCP, CLS, INP trên Lighthouse có đỏ không?
- **DONE Checklist:**
  - [ ] Score Lighthouse >= 90. Không render-blocking tài nguyên.
  - [ ] Hero Image (LCP) được ưu tiên tải trước (`fetchPriority="high"`).

## 10. RESPONSIVE DESIGN
- **TODO Checklist:** Thiết lập Breakpoints (Mobile, Tablet, Desktop).
- **REVIEW Checklist:** Layout có xuất hiện thanh cuộn ngang rác trên màn 320px không?
- **DONE Checklist:**
  - [ ] Thích ứng hoàn hảo mọi màn hình (Không vỡ bảng Table, không đè lấp Text).
  - [ ] Menu Hamburger trên Mobile hoạt động trơn tru.

## 11. SECURITY (BẢO MẬT)
- **TODO Checklist:** Setup JWT, CORS, CSP Headers, XSS Sanitize.
- **REVIEW Checklist:** Các API quan trọng có lỗ hổng IDOR (Sửa ID người khác trên URL) không?
- **DONE Checklist:**
  - [ ] Mọi Input từ User đều được Sanitize để chống XSS.
  - [ ] Cookie mang Session phải cấu hình `HttpOnly` và `Secure`.

## 12. TESTING (KIỂM THỬ)
- **TODO Checklist:** Cài đặt Jest, Cypress, viết Unit Test cho Core Logic.
- **REVIEW Checklist:** Code Coverage có đạt chuẩn (VD: > 80%) không?
- **DONE Checklist:**
  - [ ] CI Pipeline xanh hoàn toàn (Lint, Typecheck, Test passed).
  - [ ] Regression Test chứng minh tính năng mới không làm hỏng tính năng cũ.

## 13. DOCUMENTATION (TÀI LIỆU)
- **TODO Checklist:** Viết README, Cập nhật Swagger API.
- **REVIEW Checklist:** Hướng dẫn Start dự án trên máy mới có copy/paste ăn ngay không?
- **DONE Checklist:**
  - [ ] CHANGELOG.md được cập nhật tính năng mới/lỗi đã sửa.
  - [ ] Có file ADR giải trình cho các quyết định đổi mới kiến trúc.

## 14. DEPLOYMENT (TRIỂN KHAI)
- **TODO Checklist:** Cấu hình Vercel/VPS, Dockerfile, setup Môi trường (Env).
- **REVIEW Checklist:** Git Flow đã tuân thủ chưa? (Merge `release` sang `main`).
- **DONE Checklist:**
  - [ ] Không có `.env` bị lộ trên Git, Không bật `sourcemap` trên Prod.
  - [ ] Cấu hình xong cơ chế Backup DB tự động hàng ngày.

---

# 🚀 FINAL: PROJECT READY CHECKLIST
*(Chỉ khi Manager / Tech Lead đánh giá "Pass" TOÀN BỘ tiêu chí mới được phép chạy Release Pipeline).*

- [ ] Lịch sử Git sạch sẽ, đánh Tag Semantic Versioning rõ ràng.
- [ ] CI/CD Pipeline Build Success (0 Warning, 0 Error).
- [ ] Không rò rỉ bất kỳ Secret Key (JWT, AWS, DB Pass) nào trên Client.
- [ ] Database Schema và API Swagger khớp nhau 100% với Code thực tế.
- [ ] Lighthouse Mobile Perf Score > 90, Accesibility = 100, SEO = 100.
- [ ] Chiến lược Rollback (Khôi phục tức thì về Version cũ) đã sẵn sàng nếu sập Production.
