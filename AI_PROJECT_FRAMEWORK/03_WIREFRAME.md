# 03. WIREFRAME FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là bộ khung (Framework) quy chuẩn để thiết kế Wireframe cho mọi dự án. Trọng tâm của giai đoạn này là Cấu trúc (Structure), Tỷ lệ (Proportion) và Luồng (Flow). Tuyệt đối cấm sử dụng các yếu tố cảm xúc (Màu sắc, Hình ảnh thật, Animation) ở giai đoạn này.

---

## 1. LOW FIDELITY RULE (QUY TẮC ĐỘ CHÂN THỰC THẤP)
- **Chỉ sử dụng Grayscale:** Wireframe chỉ được phép sử dụng các sắc độ xám (White, Light Gray, Dark Gray, Black) để biểu diễn nội dung và phân cấp ưu tiên.
- **Không hình ảnh thật:** Dùng các block chữ nhật có dấu chéo (X) hoặc placeholder text (ví dụ: `[Image: Sản phẩm]`) để biểu thị khu vực chứa hình ảnh.
- **Không Animation/Tương tác mượt:** Bỏ qua toàn bộ tư duy về scroll, hiệu ứng transition, hover. Chỉ tập trung vào trạng thái tĩnh (Static state).
- **Font chữ cơ bản:** Sử dụng font sans-serif hệ thống cơ bản nhất (Roboto, Arial, Inter) để không làm phân tâm người xem bởi typography.

## 2. LAYOUT GRID (HỆ THỐNG LƯỚI)
- Lưới (Grid) là xương sống của wireframe. Mọi element phải snap vào grid.
- **Desktop:** Lưới 12 cột (12-column grid), margin lớn, gutter (khoảng cách giữa các cột) chuẩn mực (ví dụ: 24px hoặc 32px).
- **Tablet:** Lưới 8 cột (8-column grid).
- **Mobile:** Lưới 4 cột (4-column grid), margin nhỏ (ví dụ: 16px hoặc 20px).
- Chiều ngang tổng thể (Max-width) phải được giới hạn (ví dụ: 1280px hoặc 1440px trên Desktop) để chống tình trạng UI tràn mép màn hình siêu lớn.

## 3. SPACING RULE (QUY TẮC KHOẢNG TRẮNG)
- Sử dụng **Hệ thống khoảng cách tỷ lệ Base-8** (8, 16, 24, 32, 40, 48, 64, 80, 120, 160...).
- Tôn trọng nguyên tắc Gestalt: Các phần tử liên quan (Title & Description) phải nằm gần nhau (ví dụ: 16px). Các section khác biệt phải nằm cách xa nhau (ví dụ: 120px hoặc 160px).
- Khoảng trắng (Negative Space) chính là yếu tố tạo nên sự rõ ràng. Đừng cố lấp đầy không gian.

## 4. CTA PLACEMENT (VỊ TRÍ NÚT KÊU GỌI HÀNH ĐỘNG)
- Bắt buộc phải có hệ thống phân cấp nút rõ ràng:
  - **Primary CTA:** Dùng màu đen/xám đậm nhất. Chỉ xuất hiện 1-2 lần trên màn hình quan trọng.
  - **Secondary CTA:** Nút viền (Outline button) hoặc màu xám nhạt.
  - **Text Link:** Dùng cho các hành động ít quan trọng.
- Vị trí Primary CTA: Luôn phải nằm ở các điểm "nóng" trong luồng mắt (Góc phải trên Navbar, Dưới thông điệp Hero, Cuối trang).

## 5. INFORMATION PRIORITY (ĐỘ ƯU TIÊN THÔNG TIN)
- Kích thước của các khối xám trong wireframe phản ánh trực tiếp tầm quan trọng của thông tin.
- **Tiêu đề (H1/H2):** Size chữ lớn, bold, nằm trên cùng hoặc đầu section.
- **Văn bản phụ trợ:** Size chữ nhỏ, gray.
- Khối nào cần người dùng tương tác ngay (Form, Pricing) phải được bố trí ở khu vực trung tâm hoặc chia cột chiếm tỷ lệ lớn.

---

## 6. DESKTOP LAYOUT
- Đặc trưng bởi không gian rộng.
- Hỗ trợ cuộn ngang (Horizontal scroll) hoặc Layout chia cột phức tạp (3-4 cột).
- Có thể sử dụng Sidebar cố định, Sticky Header lớn.
- Khuyến khích sử dụng các khoảng trắng bất đối xứng (Asymmetric whitespace) để layout bớt nhàm chán.

## 7. TABLET LAYOUT
- Trạng thái chuyển giao. Thường chuyển từ Grid 3-4 cột của Desktop sang Grid 2 cột.
- Font size nhỏ lại so với Desktop nhưng không quá bé như Mobile.
- Touch-target size bắt đầu được chú trọng (Element phải dễ dàng chạm bằng ngón tay).
- Navigation thường chuyển từ Full Menu sang Hamburger Menu.

## 8. MOBILE LAYOUT
- **Stacked Layout:** Hầu hết mọi thứ sẽ chuyển thành xếp chồng theo chiều dọc (1 cột).
- **Cấm cuộn ngang (Horizontal Scroll):** Trừ các Carousel/Slider hoặc danh sách thẻ, tuyệt đối không để layout bị overflow ngang gây vỡ màn hình.
- Nút bấm và CTA nên thiết kế tràn viền (Full-width) hoặc có kích thước lớn để dễ bấm khi cầm một tay.
- Tiết kiệm không gian dọc: Các menu phụ, bộ lọc (Filters) nên được đẩy vào trong Drawer/Modal.

---

## 9. WIREFRAME CHECKLIST
Trong quá trình phác thảo Wireframe, phải liên tục kiểm tra:
- [ ] Mọi element đã bám sát hệ thống lưới (Grid) chưa?
- [ ] Đã tuân thủ quy tắc Grayscale hoàn toàn chưa? (Đảm bảo không có màu sắc nào lọt vào).
- [ ] Spacing giữa các khối đã đủ lớn để không bị rối mắt chưa?
- [ ] Phân cấp thị giác (Tiêu đề to/đậm > Phụ đề nhỏ/nhạt) đã rõ ràng chưa?
- [ ] Đã đánh dấu (X) tất cả vị trí dành cho hình ảnh chưa?

## 10. REVIEW CHECKLIST
Trước khi kết thúc quá trình Wireframe để chuyển sang UI/Visual Design:
- [ ] Wireframe đã giải quyết được trọn vẹn User Flow được đề ra ở bước IA chưa?
- [ ] Vị trí các nút bấm (CTA) có logic và thúc đẩy tỷ lệ chuyển đổi (Conversion) không?
- [ ] Layout có khả năng scale tốt trên cả 3 màn hình: Desktop, Tablet, Mobile chưa?
- [ ] Đã thảo luận và chốt phương án hiển thị dữ liệu lỗi (Error states), dữ liệu rỗng (Empty states) chưa?
- [ ] Wireframe có đủ chi tiết để Frontend Developer mường tượng ra cấu trúc HTML chưa?
