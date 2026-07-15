# 02. INFORMATION ARCHITECTURE FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là bộ khung (Framework) quy chuẩn để thiết kế Kiến trúc Thông tin (Information Architecture - IA) cho bất kỳ dự án nào. Tài liệu này cung cấp các nguyên tắc, không đi sâu vào một dự án cụ thể. Mọi AI và Designer phải áp dụng framework này trước khi vẽ Wireframe hoặc UI.

---

## 1. SITEMAP (BẢN ĐỒ TRANG)
- **Mục đích:** Xác định toàn bộ cấu trúc các trang trong hệ thống và mối quan hệ giữa chúng.
- **Tiêu chuẩn:**
  - Luôn sử dụng mô hình cây phân cấp (Tree Hierarchy) tối đa 3-4 mức độ sâu (Depth).
  - Phân tách rõ ràng giữa **Core Pages** (Trang chính: Home, About, Services) và **Support Pages** (Trang phụ: Policy, FAQ, 404).
  - Cần có sơ đồ trực quan hoặc danh sách dạng cây rõ ràng trước khi code.

## 2. NAVIGATION SYSTEM (HỆ THỐNG ĐIỀU HƯỚNG)
- **Mục đích:** Giúp người dùng tìm kiếm và di chuyển qua lại giữa các nội dung một cách dễ dàng nhất.
- **Tiêu chuẩn:**
  - **Global Navigation (Header/Navbar):** Chứa tối đa 5-7 mục quan trọng nhất.
  - **Local Navigation (Sidebar/Tabs):** Dành cho các phân mục con trong cùng một trang hoặc ngữ cảnh.
  - **Utility Navigation:** Các chức năng tiện ích (Search, Login, Cart, Language) nằm tách biệt (thường ở góc phải trên cùng).
  - **Footer Navigation:** Chứa các liên kết hỗ trợ, pháp lý, social, và một bản tóm tắt sitemap rút gọn.

## 3. PAGE HIERARCHY (PHÂN CẤP TRANG)
- **Mục đích:** Xác định sự phụ thuộc và kế thừa giữa các trang.
- **Tiêu chuẩn:**
  - **Level 1 (Homepage):** Điểm xuất phát trung tâm.
  - **Level 2 (Category/Landing Pages):** Phân chia theo chủ đề lớn.
  - **Level 3 (Detail Pages):** Thông tin chi tiết, điểm cuối của phễu thông tin (Bài viết, Chi tiết sản phẩm).
  - Mỗi trang ở Level thấp phải có đường lùi (Back/Breadcrumb) về Level cao hơn.

## 4. USER FLOW (LUỒNG NGƯỜI DÙNG)
- **Mục đích:** Vạch ra đường đi tối ưu nhất để người dùng hoàn thành một mục tiêu (Conversion).
- **Tiêu chuẩn:**
  - Xác định rõ **Entry Points** (Điểm vào: Organic search, Ads, Social).
  - Giảm thiểu số click để đạt được mục tiêu (Nguyên tắc 3-clicks).
  - Định tuyến các **Exit Points** (Trang cảm ơn, Xác nhận thanh toán) trở lại vòng lặp tương tác (Ví dụ: "Xem thêm sản phẩm").

## 5. URL STRUCTURE (CẤU TRÚC URL)
- **Mục đích:** Tạo URL thân thiện với cả người dùng và công cụ tìm kiếm (SEO).
- **Tiêu chuẩn:**
  - Phản ánh đúng Page Hierarchy: `domain.com/category/subcategory/item`
  - Sử dụng chữ thường (lowercase) và dấu gạch ngang (`-`) để nối từ. Cấm dùng underscore (`_`) hoặc khoảng trắng.
  - Giữ URL ngắn gọn, dễ đọc, không chứa các tham số động không cần thiết (trừ query search/filter).

## 6. BREADCRUMB STRATEGY (CHIẾN LƯỢC BREADCRUMB)
- **Mục đích:** Cung cấp context vị trí hiện tại và lối thoát nhanh cho người dùng.
- **Tiêu chuẩn:**
  - Bắt buộc áp dụng cho các website có cấu trúc sâu từ 3 tầng trở lên.
  - Hiển thị theo định dạng: `Home > Category > Current Page`.
  - Item cuối cùng (Current Page) không được click.
  - Tích hợp **Breadcrumb Schema Markup** (JSON-LD) cho SEO.

## 7. INTERNAL LINKING (LIÊN KẾT NỘI BỘ)
- **Mục đích:** Điều hướng người dùng, giữ chân họ lâu hơn và phân bổ sức mạnh SEO (Link Juice).
- **Tiêu chuẩn:**
  - Liên kết theo ngữ cảnh (Contextual links) trong nội dung văn bản.
  - Sử dụng Anchor Text tự nhiên, mô tả chính xác nội dung đích đến. Không lạm dụng "Click here".
  - Có các block gợi ý: "Bài viết liên quan", "Sản phẩm tương tự", "Có thể bạn quan tâm".

## 8. CONTENT HIERARCHY (PHÂN CẤP NỘI DUNG)
- **Mục đích:** Cách sắp xếp thông tin trong cùng một trang để dẫn dắt mắt người xem.
- **Tiêu chuẩn:**
  - Tuân thủ quy luật đọc chữ F (F-pattern) hoặc chữ Z (Z-pattern).
  - **Hero Section:** Nằm ở trên cùng (Above the fold), chứa thông điệp quan trọng nhất (H1) + Primary CTA.
  - Cấu trúc tiêu đề chuẩn SEO: 1 thẻ H1 duy nhất; H2, H3 chia các đoạn nội dung logic.

## 9. INFORMATION PRIORITY (ĐỘ ƯU TIÊN THÔNG TIN)
- **Mục đích:** Quyết định cái gì cần hiển thị trước, cái gì giấu đi hoặc đẩy xuống dưới.
- **Tiêu chuẩn:**
  - **High Priority:** Luôn hiển thị ngay lập tức (Primary CTA, Tiêu đề chính, Giá).
  - **Medium Priority:** Cần scroll để thấy hoặc nằm ở submenu.
  - **Low Priority:** Nằm trong Footer, Accordion (FAQ), hoặc ẩn đi bằng các tab/modal/tooltip.

---

## 10. NAVIGATION CHECKLIST
Trước khi chốt hệ thống Navigation, phải pass các câu hỏi sau:
- [ ] Label của các menu item có dễ hiểu, không dùng từ lóng (Jargon) không?
- [ ] Số lượng item trên Main Menu có dưới 7 không?
- [ ] Người dùng có luôn biết họ đang ở trang nào không? (Active state).
- [ ] Có ô tìm kiếm (Search bar) nếu nội dung quá lớn không?
- [ ] Navigation trên Mobile có dễ chạm (Touch-target size ≥ 44px) và dễ đóng/mở không?

## 11. IA REVIEW CHECKLIST
Trước khi chuyển sang bước thiết kế UI/Wireframe, phải pass các tiêu chí sau:
- [ ] IA đã bao phủ 100% yêu cầu chức năng (Business requirements) chưa?
- [ ] Cấu trúc URL đã logic và chuẩn SEO chưa?
- [ ] Nếu một trang bị lỗi 404, người dùng có dễ dàng quay lại trang chủ không?
- [ ] Các trang cụt (Dead-end pages) đã được loại bỏ hoặc gắn thêm Internal Link chưa?
- [ ] IA đã được test thử bằng kịch bản (Scenario) của User Persona chưa?
