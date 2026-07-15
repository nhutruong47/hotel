# 16. ANIMATION LIBRARY FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là thư viện Tiêu chuẩn hóa các hiệu ứng Chuyển động (Animation Library). Tài liệu này không chứa code, mà đóng vai trò là "Từ điển" định nghĩa tên gọi, cảm giác vật lý và ngữ cảnh sử dụng cho từng loại hiệu ứng. Mọi animation trong dự án đều phải trích xuất từ các mẫu chuẩn dưới đây.

---

## 1. BASIC TRANSITIONS (CÁC BIẾN ĐỔI CƠ BẢN)

### Fade
- **Quy chuẩn:** Từ trong suốt (opacity: 0) chuyển dần sang rõ nét (opacity: 1). Tốc độ mặc định là Normal (0.4s - 0.6s).
- **Ứng dụng:** Dùng cho background, tooltip, hoặc các thay đổi trạng thái tĩnh (ví dụ chuyển tab) không cần nhấn mạnh hướng di chuyển. Cấm dùng Fade thuần túy cho văn bản (Nên kết hợp với Slide hoặc Blur).

### Slide
- **Quy chuẩn:** Dịch chuyển theo trục X hoặc Y một khoảng cách ngắn (thường là 20-40px). Luôn đi kèm với Fade.
- **Ứng dụng:** Dùng để xuất hiện các đoạn văn bản (Slide up), Menu trượt vào từ cạnh (Slide in left/right). Tốc độ mượt mà, ease out.

### Scale
- **Quy chuẩn:** Phóng to hoặc thu nhỏ đối tượng.
  - Scale Up: Từ 0.95 lên 1.0 (Không bao giờ scale từ 0 trừ các Pop-up/Badge vui nhộn).
  - Scale Down: Từ 1.05 về 1.0.
- **Ứng dụng:** Kết hợp với Fade để làm xuất hiện Modal/Dialog, mang lại cảm giác đối tượng nổi lên từ bên dưới màn hình.

### Blur
- **Quy chuẩn:** Từ mờ nhòe (Blur 10px-20px) về nét (Blur 0px). Rất ngốn tài nguyên, chỉ áp dụng trên text hoặc ảnh hero.
- **Ứng dụng:** Hiệu ứng "Tỉnh giấc" (Awaken) ở màn hình Intro hoặc Hero section. Tuyệt đối không dùng cho toàn bộ bài viết dài.

---

## 2. ADVANCED REVEALS (CÁC HIỆU ỨNG XUẤT HIỆN NÂNG CAO)

### Reveal (Staggered Text)
- **Quy chuẩn:** Chữ xuất hiện theo từng dòng (Line), từng từ (Word) hoặc từng ký tự (Character) với một độ trễ (Delay) đều đặn giữa các phần tử.
- **Ứng dụng:** Dành riêng cho các Tiêu đề chính (H1, H2) để định hình nhịp đọc của người dùng.

### Mask (Clip-path Reveal)
- **Quy chuẩn:** Phần tử bị giấu đi bởi một mặt nạ (Mask/Clip-path), sau đó mặt nạ kéo ra để hé lộ nội dung (Thường quét từ dưới lên, hoặc từ trái qua phải).
- **Ứng dụng:** Dành riêng cho việc load Hình ảnh (Images). Mang lại cảm giác điện ảnh (Cinematic) và che giấu quá trình tải ảnh.

### Image Sequence
- **Quy chuẩn:** Chạy liên tiếp hàng chục/trăm khung hình ảnh tạo thành một video tương tác được theo thao tác cuộn chuột (Tương tự Apple Product Pages).
- **Ứng dụng:** Trình diễn sản phẩm 3D hoặc quy trình phức tạp. Đòi hỏi nén ảnh cực kỳ khắt khe để đảm bảo hiệu năng.

---

## 3. SPATIAL & SCROLL EFFECTS (HIỆU ỨNG KHÔNG GIAN & CUỘN)

### Parallax
- **Quy chuẩn:** Tốc độ di chuyển của Phông nền (Background) chậm hơn Tiền cảnh (Foreground) khi người dùng cuộn trang.
- **Ứng dụng:** Tạo chiều sâu 3D cho giao diện. Áp dụng cho các bức ảnh phong cảnh, banner lớn. Không áp dụng cho các khối Text để tránh chóng mặt.

### Camera Move (Dolly/Pan)
- **Quy chuẩn:** Khóa cuộn trang (Pin), thay vào đó phóng to (Zoom in) vào một chi tiết bức ảnh, hoặc trượt ngang (Pan) để xem hết một bức ảnh panorama.
- **Ứng dụng:** Kể chuyện (Storytelling), hướng sự chú ý tuyệt đối vào một điểm ảnh duy nhất.

### Scroll Timeline
- **Quy chuẩn:** Trục thời gian (Timeline) của Animation bị trói buộc hoàn toàn vào thanh cuộn (Scrollbar). Cuộn xuống thì chạy tới, cuộn lên thì tua ngược.
- **Ứng dụng:** Các biểu đồ động (Data visualization), quá trình lắp ráp sản phẩm, hoặc dải chữ cuộn ngang màn hình (Marquee).

### Scroll Animation (Bình thường)
- **Quy chuẩn:** Animation chỉ kích hoạt (Trigger) 1 lần khi cuộn tới phần tử đó, sau đó chạy độc lập bằng thời gian cố định.
- **Ứng dụng:** Cho 90% các nội dung trên trang (Text fade up, Image reveal). Đảm bảo người dùng lướt nhanh không bị bỏ lỡ nội dung.

---

## 4. INTERACTIVE & STATE EFFECTS (HIỆU ỨNG TƯƠNG TÁC)

### Hover Effect
- **Quy chuẩn:** Phản ứng xảy ra khi con trỏ chuột lướt qua phần tử. Tốc độ phải Cực Nhanh (0.15s - 0.25s).
- **Ứng dụng:**
  - Nút bấm: Đổi màu nền, dời icon 5px, hoặc chạy viền gradient.
  - Thẻ (Card): Hơi nổi lên (Scale 1.02), thêm đổ bóng nhẹ, hoặc hình ảnh bên trong thẻ bị zoom nhẹ (Image Zoom).

### Loading Animation
- **Quy chuẩn:** Chuyển động tuần hoàn (Infinite Loop) thể hiện hệ thống đang xử lý.
- **Ứng dụng:** Spinner, Progress Bar, hoặc Logo nhịp tim (Pulse). Tránh các hiệu ứng chuyển động loạn xạ gây mất tập trung khỏi thông điệp "Vui lòng chờ".

### Page Transition (Chuyển trang)
- **Quy chuẩn:** Quá trình điều hướng từ URL này sang URL khác không bị chớp trắng màn hình. Trang cũ mờ đi (Fade out) hoặc trượt ra, trang mới từ từ hiện lên.
- **Ứng dụng:** Giữ mạch cảm xúc liền mạch cho toàn bộ website. Thường kéo dài 0.6s - 1.0s.

---

## 5. GSAP MAPPING (ÁNH XẠ VỚI THƯ VIỆN KỸ THUẬT)
*Lưu ý cho Developer: Khi nhận thiết kế có nhắc đến các danh từ trên, phải tự động ánh xạ sang các hàm tiêu chuẩn của GSAP.*

- **Fade/Slide:** Ánh xạ với `gsap.from({ opacity: 0, y: 30, ease: "power3.out" })`.
- **Reveal:** Ánh xạ với SplitText plugin + `stagger`.
- **Mask:** Ánh xạ với `clip-path: inset()`.
- **Parallax:** Ánh xạ với `ScrollTrigger` có `scrub: true` và `yPercent`.
- **Camera Move:** Ánh xạ với `ScrollTrigger` có `pin: true`.
- **Scroll Timeline:** Ánh xạ với `Timeline` được đính kèm vào `ScrollTrigger` với `scrub: 1`. 

Mọi hiệu ứng nằm ngoài từ điển này phải được xem xét kỹ lưỡng trước khi đưa vào dự án để tránh làm hỏng Tính nhất quán (Consistency).
