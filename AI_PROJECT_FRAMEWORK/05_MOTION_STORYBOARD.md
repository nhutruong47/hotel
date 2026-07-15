# 05. MOTION STORYBOARD FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là bộ khung (Framework) quy chuẩn để thiết kế Kịch bản Chuyển động (Motion Storyboard) cho website. Giai đoạn này tập trung mô tả ý tưởng không gian, nhịp điệu và cảm xúc (Không chứa bất kỳ dòng code hay thư viện kỹ thuật nào). Thiết kế web hiện đại là thiết kế một thước phim.

---

## 1. STORY ARC (ĐƯỜNG DÂY CÂU CHUYỆN)
Website không phải là một tập hợp các khối thông tin rời rạc, mà là một tuyến truyện tuyến tính.
- **Mở bài (Hook/Arrival):** Thu hút sự chú ý ngay lập tức, thiết lập không gian và nhịp điệu ban đầu.
- **Thân bài (Exploration):** Tăng dần cao trào. Người dùng khám phá thông tin thông qua tương tác (scroll) với các nhịp điệu nhanh/chậm đan xen.
- **Cao trào (Climax):** Điểm bùng nổ về mặt thị giác hoặc cảm xúc, nơi khao khát sở hữu sản phẩm/dịch vụ đạt mức cao nhất.
- **Kết luận (Resolution/Action):** Nhịp điệu lắng xuống, hướng sự tập trung hoàn toàn vào CTA (Call to Action).

## 2. SCENE TEMPLATE (CẤU TRÚC MỘT CẢNH QUAY)
Bất kỳ một Section nào trên website cũng phải được xem như một "Cảnh quay" (Scene) và cần được xác định rõ:
- **Tên Cảnh (Scene Name):** Ví dụ: *Cảnh 1 - Cánh cổng gỗ.*
- **Chủ thể (Subject):** Element nào là trung tâm của sự chú ý? (Tiêu đề chính, hay Sản phẩm nổi bật).
- **Phông nền (Background):** Tĩnh hay Động? Sáng hay Tối?
- **Tiêu điểm (Focal Point):** Mắt người dùng phải nhìn vào đâu đầu tiên khi cảnh này bắt đầu?

## 3. CAMERA MOVEMENT (CHUYỂN ĐỘNG MÁY QUAY)
Tư duy di chuyển màn hình như một góc máy quay ảo:
- **Dolly In / Out:** Camera tiến lại gần hoặc lùi ra xa (Tương đương hiệu ứng Zoom in/out vào hình ảnh, tạo chiều sâu).
- **Pan / Tracking:** Máy quay lướt ngang (Tương đương việc Scroll ngang màn hình).
- **Tilt:** Máy quay ngước lên hoặc cúi xuống.
- **Parallax:** Phông nền trượt chậm hơn tiền cảnh, tạo cảm giác không gian 3D hùng vĩ.

## 4. SCROLL TIMELINE (DÒNG THỜI GIAN CUỘN)
Cuộn (Scroll) chính là đạo diễn của website.
- Không để mọi thứ xuất hiện cùng lúc. Quy định rõ thứ tự xuất hiện: `Yếu tố A -> Yếu tố B -> Yếu tố C`.
- **Khoảng dừng (Pacing):** Giữa các cảnh phải có những khoảng trắng tĩnh lặng. Cho mắt người xem thời gian nghỉ ngơi trước khi nhồi nhét thông tin mới.
- Khóa cuộn (Pinning): Giữ khung hình đứng yên trong khi nội dung bên trong biến đổi, giúp người dùng tập trung hoàn toàn vào một thông điệp.

## 5. TRIGGER (ĐIỂM KÍCH HOẠT)
Xác định chính xác điều kiện gì sẽ kích hoạt chuyển động:
- **On Enter:** Khi mép trên của cảnh vừa chạm vào đáy màn hình.
- **On Center:** Khi chủ thể nằm ngay giữa màn hình (thường dùng để kích hoạt các animation quan trọng nhất).
- **On Leave:** Cảnh mờ đi hoặc tan biến khi người dùng lướt qua.
- **Hover / Click:** Kích hoạt do thao tác chủ động từ người dùng.

## 6. TRANSITION (SỰ CHUYỂN CẢNH)
Cách thức Scene này nối tiếp Scene kia:
- **Cut:** Chuyển cảnh đột ngột, sắc nét (Dành cho thông tin mạnh mẽ, gãy gọn).
- **Fade / Dissolve:** Chuyển cảnh hòa tan mượt mà, tĩnh lặng, chậm rãi.
- **Wipe / Mask Reveal:** Mở màn bằng cách quét qua một lớp mặt nạ (Che khuất rồi từ từ hé lộ nội dung).

## 7. DURATION (THỜI LƯỢNG & NHỊP ĐIỆU)
- **Cực nhanh (0.1s - 0.2s):** Dành cho Micro-interactions (Hover nút bấm, Menu dropdown). Đảm bảo tính phản hồi tức thì.
- **Bình thường (0.4s - 0.8s):** Dành cho Fade-in text, Slide hình ảnh.
- **Chậm rãi, Cảm xúc (1s - 2.5s):** Dành cho các Scene lớn, hé lộ không gian, Parallax background. Đòi hỏi sự chiêm nghiệm.

## 8. USER EMOTION (CẢM XÚC NGƯỜI DÙNG)
Mỗi Scene phải gắn liền với một mục tiêu cảm xúc:
- *Ví dụ 1:* Cảm giác choáng ngợp, vĩ đại (Màn hình mở rộng, text hiện ra chậm).
- *Ví dụ 2:* Cảm giác bình yên, tĩnh tại (Chuyển động nhẹ như hơi thở, lơ lửng).
- *Ví dụ 3:* Sự khẩn trương, thúc giục (Animation nhanh, dứt khoát).

## 9. UX GOAL (MỤC TIÊU TRẢI NGHIỆM)
Motion không được phép làm hỏng Usability (Tính khả dụng).
- Nó có giúp người dùng hiểu cấu trúc trang tốt hơn không?
- Nó có che giấu thời gian chờ (Loading) không?
- Nó có dẫn dắt mắt người dùng tới đúng Nút Đặt hàng không?

---

## 10. STORYBOARD CHECKLIST
Trước khi phê duyệt Storyboard để chuyển sang bước lập trình Motion:
- [ ] Website đã có một "Story Arc" rõ ràng từ Mở bài đến Kết luận chưa?
- [ ] Animation có phục vụ mục đích kể chuyện hoặc điều hướng, hay chỉ đang "làm màu" vô nghĩa?
- [ ] Nhịp điệu (Pacing) có đủ các khoảng nghỉ tĩnh lặng, hay đang khiến người xem bị quá tải thị giác?
- [ ] Chuyển động Camera (Zoom, Pan) có gây hiệu ứng chóng mặt (Motion Sickness) cho người dùng nhạy cảm không?
- [ ] Có phân định rõ ràng giữa chuyển động độc lập theo thời gian (Time-based) và chuyển động gắn liền với thao tác cuộn (Scroll-linked) chưa?
