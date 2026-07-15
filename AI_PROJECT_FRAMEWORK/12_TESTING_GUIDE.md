# 12. TESTING GUIDE FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là quy trình Kiểm thử (Testing Protocol) áp dụng cho mọi dự án. Code không thể đưa lên Môi trường Production nếu chưa vượt qua toàn bộ các bài kiểm tra định lượng và định tính được liệt kê trong tài liệu này.

---

## KIỂM THỬ TỰ ĐỘNG (AUTOMATED TESTING)

### 1. UNIT TEST (KIỂM THỬ MỨC ĐƠN VỊ)
- **Mục tiêu:** Đảm bảo các hàm (Utility functions), hooks và các UI Component độc lập hoạt động chính xác theo từng logic cụ thể.
- **Tiêu chuẩn:**
  - Viết test cho 100% các hàm tiện ích quan trọng (Format tiền, Xử lý ngày tháng, Validate Form).
  - Không test giao diện trực quan (Visual) trong Unit Test. Tập trung vào logic (Input -> Output).
  - Tool khuyến nghị: `Vitest` hoặc `Jest` kết hợp với `React Testing Library`.

### 2. INTEGRATION TEST (KIỂM THỬ TÍCH HỢP)
- **Mục tiêu:** Kiểm tra xem các components khi ghép lại với nhau, hoặc khi component gọi API (mocked API) có hoạt động đúng luồng dữ liệu không.
- **Tiêu chuẩn:**
  - Test các sự kiện tương tác của người dùng: Nhấn nút Submit -> Chờ Loading -> Hiển thị Success/Error Message.
  - Sử dụng thư viện Mock Service Worker (MSW) để giả lập Backend API. Không dùng API thật để chạy Integration Test.

### 3. E2E TEST (KIỂM THỬ END-TO-END)
- **Mục tiêu:** Kiểm thử toàn bộ hệ thống từ góc nhìn của User thực tế, chạy trên trình duyệt thực với Database thực (hoặc Staging DB).
- **Tiêu chuẩn:**
  - Viết E2E cho các luồng sinh ra tiền (Critical Flows): Luồng Đăng nhập/Đăng ký, Luồng Thêm vào giỏ hàng, Luồng Checkout.
  - Cấm lạm dụng E2E cho mọi chức năng nhỏ lẻ vì E2E test chạy rất chậm và tốn kém bảo trì.
  - Tool khuyến nghị: `Cypress` hoặc `Playwright`.

---

## KIỂM THỬ GIAO DIỆN & TRẢI NGHIỆM (UI/UX TESTING)

### 4. RESPONSIVE TEST (KIỂM THỬ THÍCH ỨNG)
- **Tiêu chuẩn:**
  - Giao diện KHÔNG BAO GIỜ được xuất hiện thanh cuộn ngang (Horizontal Scrollbar) gây vỡ layout ở mọi kích thước từ `320px` đến `1920px`.
  - Phải test cụ thể trên các điểm gãy (Breakpoints): `375px` (Mobile), `768px` (Tablet Portrait), `1024px` (Tablet Landscape/Laptop nhỏ), `1440px` (Desktop).
  - Nút bấm và CTA trên mobile phải đủ lớn để chạm (Tối thiểu `44x44px`).

### 5. BROWSER COMPATIBILITY (ĐỘ TƯƠNG THÍCH TRÌNH DUYỆT)
- Không giả định "Chạy tốt trên Chrome của tôi thì sẽ chạy tốt ở mọi nơi".
- **Tiêu chuẩn test đa nền tảng:**
  - Chromium (Chrome, Edge): Ưu tiên số 1.
  - WebKit (Safari trên iOS/macOS): Bắt buộc test. Thường xuyên xuất hiện lỗi vỡ layout Flexbox/Grid, lỗi `100vh` trên iOS, lỗi giật lag của ScrollTrigger.
  - Firefox: Đảm bảo layout hiển thị không sai lệch móp méo.

### 6. ANIMATION TEST (KIỂM THỬ CHUYỂN ĐỘNG)
- **Performance:** Bật Chrome DevTools -> Rendering -> FPS meter. Đảm bảo animation không làm tuột FPS xuống dưới 60. Nếu giật, kiểm tra lại xem có trigger Repaint/Reflow không (chỉ được animate `transform` và `opacity`).
- **Edge cases:**
  - Cuộn thật nhanh lên xuống liên tục xem GSAP ScrollTrigger có bị kẹt (stuck) hoặc treo giao diện không.
  - Resize trình duyệt xem Animation có tính toán sai vị trí không (Kiểm tra `invalidateOnRefresh`).

---

## KIỂM THỬ HIỆU NĂNG & CHẤT LƯỢNG (AUDITS)

### 7. LIGHTHOUSE TEST (HIỆU NĂNG)
- Chạy Google Lighthouse dưới chế độ **Incognito** (để tránh bị Chrome Extensions làm sai lệch kết quả) trên phiên bản **Mobile**.
- Điểm Performance mục tiêu: **≥ 90**. Khắc phục ngay lập tức các cờ đỏ báo lỗi LCP (Largest Contentful Paint) hoặc CLS (Layout Shift).

### 8. ACCESSIBILITY AUDIT (A11Y)
- Sử dụng phím `Tab` duyệt từ đầu đến cuối trang xem có rơi vào trạng thái "tàng hình" (mất focus ring) hay bị kẹt (Focus trap) không thoát ra được không.
- Bật thử Screen Reader (VoiceOver / NVDA) nhắm mắt lướt qua một form nhập liệu.
- Điểm Accessibility mục tiêu trên Lighthouse: **100**.

### 9. SEO AUDIT
- Kiểm tra các thẻ Meta (Title, Description) có render đúng trong source HTML không (View Page Source).
- Kiểm tra thẻ Heading (H1, H2) có đúng thứ bậc không.
- Chạy thử link trên công cụ Facebook Sharing Debugger hoặc Twitter Card Validator để xem hình ảnh xem trước (Open Graph Image) có load đúng không.

---

## 10. REGRESSION TEST (KIỂM THỬ HỒI QUY)
- **Mục tiêu:** Đảm bảo code mới thêm vào không làm vỡ các chức năng cũ đang chạy tốt.
- Chạy lại toàn bộ suite Unit Test & E2E Test trước mỗi lần Merge Code vào nhánh `main` hoặc `production`.

---

## 11. MANUAL QA CHECKLIST
Bảng kiểm tra thủ công (Manual Test) cho Developer trước khi đẩy code cho QC/Tester:
- [ ] Giao diện (UI) đã giống thiết kế Figma ít nhất 95% chưa? (Chú ý Margin/Padding/Typography).
- [ ] Đã test trạng thái Empty (Không có dữ liệu) và Loading (Đang tải) chưa?
- [ ] Đã test ngắt kết nối mạng (Offline) hoặc làm chậm mạng (Slow 3G) chưa? Ứng dụng có bị crash trắng trang không?
- [ ] Có `console.error` hoặc `console.warn` nào đang báo đỏ trên trình duyệt không?
- [ ] Link "Skip to content" ẩn và cấu hình `prefers-reduced-motion` có hoạt động chính xác không?
