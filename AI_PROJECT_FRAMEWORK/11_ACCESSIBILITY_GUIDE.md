# 11. ACCESSIBILITY GUIDE FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là bộ khung chuẩn hóa (Framework) về Khả năng Tiếp cận (Accessibility - a11y) cho mọi dự án. Thiết kế tốt là thiết kế dành cho TẤT CẢ mọi người, bao gồm cả những người khiếm thị, suy giảm thính lực, hoặc hạn chế về vận động. Mục tiêu tối thiểu của hệ thống là đạt chuẩn **WCAG 2.1 Mức AA**.

---

## 1. MỤC TIÊU WCAG AA
- **Web Content Accessibility Guidelines (WCAG):** Là bộ tiêu chuẩn quốc tế về trợ năng web.
- Hệ thống bắt buộc phải đáp ứng mức **AA** (Mức trung bình khá, phổ biến nhất toàn cầu). Cấm bỏ qua các tiêu chí về độ tương phản, cấu trúc HTML và khả năng dùng bàn phím.

## 2. KEYBOARD NAVIGATION (ĐIỀU HƯỚNG BẰNG BÀN PHÍM)
- **Quy tắc cốt lõi:** Người dùng phải có khả năng sử dụng TOÀN BỘ chức năng của website mà không cần chạm vào chuột, chỉ dùng phím `Tab`, `Shift + Tab`, `Enter`, `Space` và các phím Mũi tên.
- **Skip-to-Content:** Mọi website phải có một link ẩn trên cùng (chỉ hiện ra khi nhấn Tab) có nội dung "Skip to main content" để giúp người dùng bỏ qua Header dài dòng và đi thẳng vào nội dung chính.

## 3. FOCUS MANAGEMENT (QUẢN LÝ TIÊU ĐIỂM)
- **Focus Ring:** Cấm sử dụng `outline: none` mà không có style thay thế. Mọi phần tử tương tác (Button, Link, Input) khi được Focus bằng bàn phím bắt buộc phải có viền nổi bật (sử dụng class `focus-visible:outline` của Tailwind).
- **Focus Trap:** Khi mở Modal, Dialog hoặc Drawer, Focus bắt buộc phải bị khóa lại (Trap) bên trong Modal đó. Người dùng không được phép Tab nhầm ra các phần tử nền phía sau Modal. Trả lại Focus về phần tử cũ sau khi đóng Modal.

## 4. SEMANTIC HTML (HTML NGỮ NGHĨA)
- Không dùng `<div>` hoặc `<span>` có gắn sự kiện `onClick` để làm nút bấm. Bắt buộc dùng `<button>` (cho hành động) hoặc `<a>` (để điều hướng).
- Xây dựng cấu trúc HTML đúng chuẩn: `<header>`, `<nav>`, `<main>`, `<article>`, `<aside>`, `<footer>` để Screen Reader có thể nhận diện các mảng nội dung lớn (Landmarks).
- Hệ thống Tiêu đề (Headings) phải tuần tự (Từ H1 đến H2, H3), không được nhảy cóc (VD: H1 rồi xuống H3 bỏ qua H2).

## 5. MÀN HÌNH ĐỌC (SCREEN READER) & ARIA
- **ARIA (Accessible Rich Internet Applications):** Cung cấp thêm ngữ nghĩa cho Screen Reader (Trình đọc màn hình như VoiceOver, NVDA). Không lạm dụng ARIA nếu Semantic HTML đã đủ (First Rule of ARIA: Do not use ARIA).
- **Thuộc tính quan trọng:**
  - `aria-label`: Dành cho các nút bấm chỉ có Icon mà không có chữ (VD: Dấu X để đóng, Kính lúp để tìm kiếm).
  - `aria-expanded`: Dành cho Accordion, Dropdown, Menu để báo trạng thái Đóng/Mở.
  - `aria-hidden="true"`: Dùng để giấu các phần tử trang trí (Icons, Background shapes) không mang ý nghĩa thông tin khỏi Screen Reader.

## 6. CONTRAST (ĐỘ TƯƠNG PHẢN THỊ GIÁC)
- Tỷ lệ tương phản màu sắc (Contrast Ratio) giữa chữ và nền phải đạt tối thiểu **4.5:1** đối với chữ thường, và **3:1** đối với chữ lớn (Bold hoặc kích thước >24px).
- Tuyệt đối không dùng màu sắc là phương tiện **duy nhất** để truyền đạt thông tin (VD: Không chỉ đổi chữ thành màu đỏ khi báo lỗi, mà phải kèm theo Text/Icon thông báo lỗi).

## 7. ACCESSIBLE FORM (BIỂU MẪU DỄ TIẾP CẬN)
- Mọi `<input>`, `<select>`, `<textarea>` bắt buộc phải được gắn kết với `<label>` thông qua cặp thuộc tính `id` và `htmlFor` (hoặc bọc input bên trong thẻ label).
- Cấm sử dụng `placeholder` làm phương tiện duy nhất thay thế cho `label`, vì placeholder sẽ biến mất khi người dùng gõ phím.
- Thông báo lỗi (Error messages) phải được liên kết với ô nhập liệu bằng thuộc tính `aria-describedby`.

## 8. ACCESSIBLE MODAL & DIALOG
- Thẻ bọc ngoài Modal phải có `role="dialog"` và `aria-modal="true"`.
- Bắt buộc phải có `aria-labelledby` trỏ tới ID của tiêu đề Modal, để khi Modal mở lên, Screen Reader sẽ tự động đọc tiêu đề này.

## 9. MOTION ACCESSIBILITY (TIẾP CẬN CHUYỂN ĐỘNG)
- Khoảng 10-35% người dùng bị hội chứng nhạy cảm với chuyển động (Vestibular Disorders). Các chuyển động Parallax, xoay, lướt ngang liên tục có thể gây chóng mặt, buồn nôn.

### 9.1 Reduced Motion (Giảm thiểu chuyển động)
- MỌI hiệu ứng Animation phức tạp (đặc biệt là GSAP) BẮT BUỘC phải được bọc trong hàm kiểm tra `prefers-reduced-motion: reduce`.
- Nếu User đã bật cấu hình này trên Hệ điều hành, hệ thống phải:
  - Hủy hoàn toàn Parallax.
  - Chuyển tất cả Slide/Translate animation thành Fade-in (chỉ đổi Opacity).
  - Ngừng tự động phát (Auto-play) các Carousel hoặc Video Background.

---

## 10. ACCESSIBILITY CHECKLIST
Trước khi một giao diện hoặc tính năng được coi là "Done", Developer và QA phải vượt qua bài test sau:

- [ ] Bạn có thể điều hướng, tương tác và thoát khỏi mọi luồng trên trang web chỉ bằng phím `Tab`, `Enter` và `Esc` không?
- [ ] Mọi link và button có hiển thị đường viền `focus-visible` rõ ràng khi dùng phím Tab không?
- [ ] Chạy Google Lighthouse (Tab Accessibility), điểm số có đạt tối thiểu 95 không?
- [ ] Mọi hình ảnh (trừ ảnh trang trí) đã có thuộc tính `alt` mô tả ngắn gọn và chính xác chưa?
- [ ] Khi bật chế độ `Reduce Motion` trên máy tính/điện thoại, các hiệu ứng gây chóng mặt đã dừng lại chưa?
- [ ] Contrast Checker đã báo xanh lá (Pass AA) cho toàn bộ màu chữ trên màu nền chưa?
