# 08. COMPONENT LIBRARY FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là danh mục tiêu chuẩn cho các UI Components. Tài liệu này không chứa code, mà đóng vai trò như Bản thiết kế kỹ thuật (Blueprint) quy định hành vi, hình thái và tính tương tác của từng Component. Mọi Component trong dự án phải được xây dựng dựa trên các chuẩn mực này.

---

## 1. COMPONENT NAMING & CONVENTION
- Tên Component luôn sử dụng `PascalCase` (VD: `HeroSection`, `ReviewCard`).
- Đặt tiền tố nếu Component thuộc một phạm vi hẹp (VD: `AdminTable`, `AuthForm`).
- Các biến thể (Variants) được kiểm soát qua Props (VD: `variant="outline"`, `size="lg"`). Không tạo Component mới nếu chỉ khác màu sắc hay kích thước.

---

## 2. ATOMS (THÀNH PHẦN CƠ BẢN)

### Button
- **Mô tả:** Thành phần kích hoạt hành động. Có các biến thể Primary (Thực tâm), Secondary (Viền), Tertiary (Chữ trơn), Ghost (Không viền).
- **Quy tắc:** Phải có đủ trạng thái Hover, Active, Focus, Disabled, và Loading. Trạng thái Loading cần chặn click.

### Input
- **Mô tả:** Nhận dữ liệu text/number từ người dùng.
- **Quy tắc:** Bắt buộc có Label (Tiêu đề), Placeholder (Ví dụ), và hỗ trợ hiển thị Icon (trái/phải). Trạng thái Lỗi (Error) phải hiển thị rõ thông báo màu đỏ bên dưới.

### Badge
- **Mô tả:** Nhãn đánh dấu trạng thái (New, Hot, Pending) hoặc bộ đếm (Ví dụ: số lượng trong giỏ hàng).
- **Quy tắc:** Nhỏ, bo góc mạnh (Pill shape), màu sắc tương phản cao so với nền chữ.

### Tooltip
- **Mô tả:** Hộp thoại nhỏ hiện ra khi Hover/Focus vào một element.
- **Quy tắc:** Chứa text ngắn gọn giải thích chức năng (VD: "Xóa item"). Biến mất tức thì khi chuột rời đi. Không bao giờ chứa tương tác (Không bỏ link hay button vào Tooltip).

---

## 3. MOLECULES (THÀNH PHẦN TỔ HỢP)

### Card
- **Mô tả:** Khối chứa thông tin tóm tắt về một đối tượng (Sản phẩm, Bài viết).
- **Quy tắc:** Có thể chứa Thumbnail, Title, Meta, Description, CTA. Cả Card nên click được. Phải có hiệu ứng Hover rõ ràng (Nổi lên hoặc Focus hình ảnh).

### Review Card
- **Mô tả:** Biến thể của Card dùng để hiển thị Đánh giá/Testimonial.
- **Quy tắc:** Chứa Avatar, Tên, Rating (Ngôi sao), và Trích dẫn. Thường dùng chung với Masonry hoặc Carousel.

### Form
- **Mô tả:** Tập hợp nhiều Inputs và một Submit Button.
- **Quy tắc:** Label đặt trên Input (Top-aligned) để dễ đọc trên di động. Phân tách rõ các cụm trường thông tin (VD: Info, Billing, Password).

### Accordion
- **Mô tả:** Danh sách mở rộng/thu gọn (Thường dùng cho FAQ).
- **Quy tắc:** Chỉ mở một item tại một thời điểm hoặc cho phép mở nhiều (Tùy cấu hình). Phải có Icon chỉ thị trạng thái (Dấu +/-, Mũi tên lên/xuống).

### Tabs
- **Mô tả:** Chuyển đổi qua lại giữa các nội dung trên cùng một không gian.
- **Quy tắc:** Item đang Active phải nổi bật nhất (Gạch chân hoặc đổi màu nền). Khi đổi tab, không load lại trang.

---

## 4. ORGANISMS (THÀNH PHẦN PHỨC TẠP)

### Hero
- **Mô tả:** Khối giao diện trên cùng của trang (Above the fold). Chứa thông điệp quan trọng nhất.
- **Quy tắc:** Phông nền ấn tượng (Ảnh/Video), H1 lớn, Primary CTA nổi bật. Tương thích cực tốt trên Mobile.

### Navbar
- **Mô tả:** Thanh điều hướng chính nằm trên cùng.
- **Quy tắc:** Có Sticky (Bám dính) hoặc Auto-hide (Cuộn xuống thì ẩn, cuộn lên thì hiện). Cần có Hamburger menu trên màn hình nhỏ.

### Footer
- **Mô tả:** Thanh thông tin và điều hướng chân trang.
- **Quy tắc:** Chứa Logo, Sitemap thu gọn, Social Links, Newsletter Form, Copyright, Legal Links.

### Carousel / Gallery
- **Mô tả:** Danh sách lướt ngang các hình ảnh hoặc Card.
- **Quy tắc:** Hỗ trợ vuốt trên Mobile (Swipeable). Hỗ trợ nút Previous/Next và Pagination dots (Chấm định vị). Tự động dừng cuộn khi Hover.

### Masonry
- **Mô tả:** Bố cục xếp gạch (Tương tự Pinterest) cho các hình ảnh/thẻ có chiều cao khác nhau.
- **Quy tắc:** Tối ưu hóa render để không nhảy layout khi load ảnh.

### Table
- **Mô tả:** Hiển thị dữ liệu cấu trúc dạng hàng/cột.
- **Quy tắc:** Phải hỗ trợ Scroll ngang trên Mobile. Tiêu đề cột nên Sticky nếu danh sách dài.

### Timeline
- **Mô tả:** Hiển thị chuỗi sự kiện theo dòng thời gian (Ngang hoặc dọc).
- **Quy tắc:** Mốc thời gian hiện tại (Active) phải nổi bật nhất.

### CTA (Call To Action Block)
- **Mô tả:** Khối kêu gọi hành động kích thước lớn cuối trang.
- **Quy tắc:** Title đánh mạnh vào cảm xúc, thiết kế tối giản, tập trung toàn bộ điểm nhìn vào Button duy nhất.

---

## 5. OVERLAYS (THÀNH PHẦN PHỦ TRÊN CÙNG)

### Modal / Dialog
- **Mô tả:** Hộp thoại cảnh báo hoặc yêu cầu xác nhận. Ngắt toàn bộ luồng thao tác hiện tại.
- **Quy tắc:** Màn hình nền mờ (Backdrop overlay). Khóa cuộn trang nền (Scroll lock). Nút Đóng (X) hoặc Hủy phải rõ ràng.

### Drawer
- **Mô tả:** Bảng trượt ra từ mép màn hình (Thường dùng cho Giỏ hàng, Mobile Menu, Filter).
- **Quy tắc:** Tương tự Modal nhưng có diện tích hiển thị lớn hơn theo chiều dọc. Hỗ trợ thao tác vuốt để đóng trên Mobile.

---

## 6. SYSTEM STATES (CÁC TRẠNG THÁI HỆ THỐNG)

### Skeleton
- **Mô tả:** Giao diện giả lập (Khối xám nhấp nháy) hiển thị khi dữ liệu đang load.
- **Quy tắc:** Kích thước và hình dáng Skeleton phải giống 90% so với dữ liệu thật để tránh hiện tượng dội Layout (Layout Shift) khi load xong.

### Loading State
- **Mô tả:** Trạng thái đang tải (Spinner, Progress Bar).
- **Quy tắc:** Không che toàn màn hình nếu chỉ đang load một phần nhỏ. Nút bấm khi loading phải thay chữ bằng Spinner và vô hiệu hóa click.

### Empty State
- **Mô tả:** Trạng thái khi không có dữ liệu (VD: Giỏ hàng trống, Không tìm thấy kết quả).
- **Quy tắc:** Bắt buộc có: Hình ảnh minh họa (Icon/Graphic), Lời giải thích thân thiện, và một Nút CTA điều hướng người dùng làm bước tiếp theo (VD: "Tiếp tục mua sắm").

### Error State
- **Mô tả:** Trạng thái khi hệ thống có lỗi (Lỗi 404, Lỗi 500, Lỗi mạng).
- **Quy tắc:** Không dùng thuật ngữ kỹ thuật khó hiểu. Đưa ra hướng giải quyết (VD: "Tải lại trang", "Quay về trang chủ").

---

## 7. COMPONENT CHECKLIST
Trước khi một Component được đưa vào thư viện chung (`src/shared/components`), cần kiểm tra:
- [ ] Component đã xử lý đủ mọi trạng thái tương tác (Hover, Active, Focus) chưa?
- [ ] Mọi Property (Props) cấu hình Component đã được định nghĩa TypeScript đầy đủ chưa?
- [ ] Khả năng tùy biến linh hoạt không? (Có cho phép override class qua prop `className` không?)
- [ ] Giao diện trên thiết bị siêu nhỏ (Màn hình rộng 320px) có bị vỡ Layout không?
- [ ] Có thể sử dụng bằng Bàn phím (Tab/Enter/Space) mà không cần chuột không?
