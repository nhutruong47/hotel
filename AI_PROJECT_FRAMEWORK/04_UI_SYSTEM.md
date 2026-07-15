# 04. UI SYSTEM FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là hệ thống thiết kế (Design System) cốt lõi áp dụng cho toàn bộ dự án. Mục tiêu là đảm bảo tính nhất quán (Consistency), khả năng mở rộng (Scalability) và thẩm mỹ tinh tế (Aesthetic). Mọi Component và UI Elements đều phải được kế thừa từ các quy tắc dưới đây.

---

## 1. DESIGN TOKEN
Design Tokens là các giá trị cốt lõi được định nghĩa dưới dạng biến số (Variables) để tái sử dụng xuyên suốt dự án.
- **Naming Convention:** Sử dụng định dạng ngữ nghĩa (Semantic naming) như `--color-brand-primary` thay vì `--color-red-500`.
- Mọi giá trị Hard-code trong CSS/Tailwind (ví dụ: `#F4F1EB`, `14px`) đều bị cấm. Bắt buộc phải ánh xạ qua Token.

## 2. COLOR SYSTEM (HỆ THỐNG MÀU SẮC)
- **Brand Colors:** Màu đặc trưng của thương hiệu (Primary, Secondary, Accent).
- **Neutral Colors:** Hệ màu trung tính (Black, White, và các thang độ xám/nâu/be) dành cho nền, viền và chữ.
- **Semantic Colors:** Màu mang ý nghĩa trạng thái (Success, Warning, Error, Info).
- **Quy tắc:** Không dùng màu đen/trắng thuần túy tuyệt đối (`#000000` / `#FFFFFF`) ở các mảng nền lớn để tránh làm chói mắt. Thay vào đó, dùng `Off-white` hoặc `Soft-black`.

## 3. TYPOGRAPHY (KIỂU CHỮ)
- **Font Families:** Tối đa 2 loại font. 
  - *Primary Font (Serif/Display):* Dành riêng cho Headings (H1, H2, H3) để tạo điểm nhấn sang trọng.
  - *Secondary Font (Sans-serif):* Dành cho Body text, Subtitle, UI Elements để đảm bảo tính dễ đọc.
- **Scale:** Hệ thống tỷ lệ chữ (Type scale) phải tuyến tính (VD: 12px, 14px, 16px, 20px, 24px, 32px, 48px, 64px, 80px).
- **Line Height:** Body text tối thiểu 1.5 - 1.6 để dễ đọc. Headings dao động từ 1.1 - 1.2.

## 4. SPACING (KHOẢNG CÁCH)
- **Base-8 System:** Mọi khoảng cách (Margin/Padding) đều phải là bội số của 8 (4, 8, 16, 24, 32, 40, 48, 64...).
- Khoảng trắng (White-space) phải được sử dụng rộng rãi để định hình khối và tạo cảm giác "Luxury". Đừng nhồi nhét nội dung.

## 5. GRID (HỆ THỐNG LƯỚI)
- **Desktop (≥1024px):** 12 Columns, Gutter 24px/32px, Margin tương thích.
- **Tablet (≥768px):** 8 Columns, Gutter 16px/24px.
- **Mobile (<768px):** 4 Columns, Gutter 16px.
- Container luôn phải có giới hạn Max-width (VD: 1280px hoặc 1440px).

## 6. BORDER RADIUS (BO GÓC)
- Độ cong của góc quy định tính cách của thương hiệu:
  - Góc vuông/Bo nhẹ (`2px - 4px`): Trưởng thành, chuyên nghiệp, xa xỉ.
  - Bo tròn nhiều (`8px - 16px`): Hiện đại, thân thiện, công nghệ.
  - Hình viên thuốc (Pill-shape `9999px`): Dành riêng cho Badge, Tag hoặc một số nút CTA đặc thù.
- Phải áp dụng đồng nhất toàn hệ thống. Cấm pha trộn vô tội vạ.

## 7. SHADOW (BÓNG ĐỔ)
- Hạn chế sử dụng Drop Shadow. Ưu tiên thiết kế phẳng (Flat Design) và phân cấp bằng màu sắc hoặc đường viền (Border).
- Nếu dùng shadow, bóng phải cực mềm, mờ, lan rộng (Large blur radius) và có opacity rất thấp (≤ 10%). Tuyệt đối cấm bóng đen gắt.

## 8. ICONOGRAPHY (BIỂU TƯỢNG)
- Sử dụng duy nhất 1 thư viện Icon đồng nhất (Cùng stroke weight, cùng style viền hoặc mảng).
- Stroke mặc định: 1.5px hoặc 2px.
- Kích thước chuẩn: 16x16, 20x20, 24x24, 32x32.
- Luôn kèm theo `aria-label` hoặc `<title>` cho screen reader nếu icon hoạt động độc lập (không có text đi kèm).

---

## 9. COMPONENT STATE (TRẠNG THÁI UI)
Mọi Component tương tác đều phải định nghĩa rõ các trạng thái:
- **Default:** Trạng thái nghỉ.
- **Hover:** Trạng thái đưa chuột qua (Chuyển màu nhẹ, scale nhẹ).
- **Active / Pressed:** Trạng thái đang click (Thường tối màu hơn hoặc thu nhỏ lại).
- **Focus / Focus-visible:** Trạng thái khi dùng phím Tab. Bắt buộc có Outline rõ ràng để hỗ trợ Accessibility.
- **Disabled:** Trạng thái vô hiệu hóa (Giảm Opacity xuống 50%, cấm cursor-pointer).
- **Loading:** Trạng thái chờ xử lý (Kèm spinner hoặc skeleton).

---

## 10. CORE COMPONENTS (CÁC THÀNH PHẦN CỐT LÕI)

### 10.1. Button (Nút bấm)
- Phân cấp: Primary (Thực tâm), Secondary (Viền), Tertiary (Chữ trơn).
- Kích cỡ: Small, Medium, Large.
- Chứa Icon: Icon trái (Leading), Icon phải (Trailing), Icon only (Cần tooltip).

### 10.2. Input (Trường nhập liệu)
- Luôn đi kèm: Label (tiêu đề), Placeholder (gợi ý), Helper Text (ghi chú phụ).
- Trạng thái phản hồi: Success (Xanh), Error (Đỏ + Icon + Dòng thông báo lỗi cụ thể dưới bottom).
- Cấm lạm dụng Placeholder thay thế cho Label vì khi nhập liệu sẽ mất dấu.

### 10.3. Card (Thẻ thông tin)
- Thành phần cơ bản: Thumbnail/Image (Tùy chọn), Title, Meta data, Description, Action.
- Cả Card nên là một thẻ link `<a href...>`, nhưng cấu trúc semantic vẫn phải rõ ràng (Dùng `<article>`).

### 10.4. Form (Biểu mẫu)
- Các field liên quan nên được nhóm (Group) lại với khoảng cách gần nhau.
- Chỉ yêu cầu những thông tin cực kỳ cần thiết. Càng nhiều field, Conversion Rate càng thấp.
- Luôn báo lỗi Inline ngay khi người dùng type sai (On Blur), không đợi đến khi Submit mới báo lỗi.

### 10.5. Modal (Hộp thoại nổi)
- Background overlay: Màu tối mờ (VD: Black/50%) kết hợp backdrop-blur.
- Luôn có nút Close (X) góc phải trên.
- Click ra ngoài Overlay hoặc nhấn phím ESC phải đóng được Modal.
- Phải Focus trap: Khi mở Modal, phím Tab chỉ lặp lại trong nội dung Modal.

### 10.6. Toast (Thông báo nổi)
- Dùng cho các phản hồi tự động biến mất (VD: Đã thêm vào giỏ, Gửi thành công).
- Không chứa nội dung sống còn vì nó sẽ biến mất sau 3-5 giây.
- Hiển thị ở góc màn hình (thường là Bottom-Right hoặc Top-Center).

---

## 11. DARK MODE STRATEGY
Nếu dự án có yêu cầu Dark Mode:
- Tránh đảo ngược màu đơn thuần (Invert color).
- Background không dùng #000000 (OLED Black) vì gây mỏi mắt khi scroll text trắng. Sử dụng Dark Gray (#121212 hoặc #18181b).
- Giảm độ chói của Accent Color (Giảm Saturation, tăng Lightness).
- Mọi Image phải được giảm Opacity nhẹ (0.8 - 0.9) để không sáng quắc trên nền đen.

---

## 12. UI CHECKLIST
Trước khi chuyển UI vào giai đoạn Code, Designer/AI phải đảm bảo:
- [ ] 100% màu sắc và font chữ đã được map với Design Token.
- [ ] Spacing và Layout đã hoàn toàn khớp với Base-8 và Grid System.
- [ ] Các Component tương tác đã có đủ trạng thái (Hover/Focus/Disabled).
- [ ] Iconography đã đồng nhất kích cỡ và độ dày nét.
- [ ] Contrast Ratio của text trên background đạt chuẩn AA (Tỷ lệ 4.5:1).
