# 06. MOTION SPEC FRAMEWORK

> **TÀI LIỆU KỸ THUẬT TIÊU CHUẨN:** Đây là tài liệu quy định thông số kỹ thuật (Motion Specification) dành riêng cho Frontend Developer khi triển khai Animation. Khác với Storyboard (cảm xúc), tài liệu này định lượng mọi chuyển động thành code và thông số cụ thể. 

---

## 1. ANIMATION SPECIFICATION (THÔNG SỐ CHUẨN)
Mọi chuyển động phải được chuẩn hóa thành các hằng số (constants) để đảm bảo tính nhất quán trên toàn dự án. Cấm hard-code tùy tiện các giá trị thời gian và easing ở từng component riêng lẻ.

## 2. TIMELINE & CHOREOGRAPHY
- Không sử dụng các biến đổi độc lập lộn xộn. Các chuỗi chuyển động phức tạp phải được gói gọn trong **GSAP Timeline (`gsap.timeline()`)**.
- **Stagger:** Khoảng cách trễ (delay) giữa các phần tử liên tiếp (ví dụ: các ký tự trong một dòng chữ, hoặc các card trong một grid) thường là `0.05s - 0.15s` (GSAP: `stagger: 0.1`).

## 3. EASE (HÀM GIA TỐC)
- Cấm sử dụng Linear (đều đặn) trừ trường hợp infinite loop (xoay vòng lặp).
- Các Easing tiêu chuẩn bắt buộc (Tương đương GSAP):
  - **Nhấn mạnh, sắc nét:** `power4.inOut` hoặc `expo.out`.
  - **Mượt mà, tự nhiên (Default):** `power3.out`.
  - **Nảy nhẹ (Tránh lạm dụng):** `back.out(1.2)`.

## 4. DURATION (THỜI LƯỢNG)
Định nghĩa hệ thống biến thời lượng tiêu chuẩn:
- `fast`: `0.2s` - Dùng cho Hover state, Micro-interactions.
- `normal`: `0.6s` - Dùng cho Reveal text đơn giản, Slide-in nhẹ.
- `slow`: `1.2s` - Dùng cho Hero reveal, Page transitions.
- `epic`: `2.0s+` - Dùng cho Parallax background, Cinematic panning.

## 5. TRIGGER (ĐIỂM KÍCH HOẠT)
- **Viewport Trigger:** `start: "top 80%"` (Kích hoạt khi cạnh trên phần tử chạm vào 80% màn hình từ trên xuống). Không để `start: "top bottom"` vì có thể kích hoạt quá sớm.
- **Toggle Actions (GSAP):** Thường sử dụng cấu hình `toggleActions: "play none none reverse"` (Chạy khi cuộn xuống, đảo ngược khi cuộn lên).

---

## CÁC BIẾN ĐỔI VẬT LÝ (PHYSICAL TRANSFORMS)

### 6. OPACITY (ĐỘ MỜ)
- Mặc định: Fade-in từ `opacity: 0` lên `opacity: 1`.
- Kết hợp: Không bao giờ dùng Fade-in đơn thuần (trừ background). Phải kết hợp với Y-Transform (TranslateY) hoặc Blur để tạo cảm giác có chiều sâu.

### 7. SCALE (TỶ LỆ)
- **Scale Up:** Từ `scale: 0.9` (hoặc `1.1` cho ảnh) về `scale: 1`. Tránh scale từ 0 (gây cảm giác hoạt hình rẻ tiền).
- Thường dùng `scale: 1.05 -> 1` cho Image Reveal.

### 8. BLUR (ĐỘ NHÒE)
- Tạo hiệu ứng Focus: Bắt đầu từ `filter: blur(10px)` về `blur(0px)`. 
- Chú ý: Filter Blur rất ngốn tài nguyên (GPU/CPU), cấm lạm dụng trên nhiều phần tử cùng lúc.

### 9. TRANSFORM (DỊCH CHUYỂN)
- Ưu tiên sử dụng `x`, `y` (tương đương `transform: translate3d`) để kích hoạt Hardware Acceleration (GPU).
- Cấm sử dụng `top`, `left`, `margin` để làm animation vì gây re-layout, giảm FPS nghiêm trọng.
- **Thông số chuẩn:** Dịch chuyển nhẹ (`y: 40px` hoặc `yPercent: 10`), không để khoảng cách dịch chuyển quá lớn gây ảo giác bay nhảy.

---

## 10. GSAP MAPPING (QUY CHUẨN GSAP TRONG REACT)
- **Tương thích:** Luôn sử dụng hook `useGSAP()` (được cung cấp bởi `@gsap/react`) để quản lý Scope và tự động dọn dẹp bộ nhớ (Cleanup).
- Nếu không dùng `useGSAP`, bắt buộc dùng `gsap.context()` bên trong `useEffect()` và gọi `ctx.revert()` lúc unmount. Quên revert = Bug Memory Leak rò rỉ bộ nhớ.
- Khi truy xuất phần tử DOM, dùng `useRef`. Tránh query bằng string class/id (`gsap.to('.my-class')`) để không ảnh hưởng toàn cục.

## 11. LENIS INTEGRATION (TÍCH HỢP LENIS SMOOTH SCROLL)
- Lenis là engine cuộn mượt (Smooth Scrolling) duy nhất được sử dụng.
- **Đồng bộ hóa GSAP Ticker:** Bắt buộc phải đồng bộ Lenis `requestAnimationFrame` với GSAP Ticker để khắc phục tình trạng giật lag của ScrollTrigger:
  ```javascript
  lenis.on('scroll', ScrollTrigger.update)
  gsap.ticker.add((time) => { lenis.raf(time * 1000) })
  gsap.ticker.lagSmoothing(0)
  ```

## 12. SCROLLTRIGGER RULE (QUY TẮC SCROLLTRIGGER)
- **Scrubbing:** Khi dùng `scrub: true` (hoặc `scrub: 1`), hiệu ứng cuộn phải mượt, chỉ áp dụng cho Parallax Image hoặc Text Reveal chạy dài. Cấm dùng Scrub cho các element quá nhỏ hoặc click được (Button).
- **Pinning:** Khi dùng `pin: true`, phải cấu hình `anticipatePin: 1` để tránh hiện tượng nhấp nháy giật hình lúc phần tử bắt đầu khóa lại.
- **Invalidate On Refresh:** Đối với các animation phụ thuộc vào kích thước động (VD: Resize cửa sổ), luôn set `invalidateOnRefresh: true`.

## 13. MOTION PERFORMANCE (TỐI ƯU HIỆU SUẤT)
- **Will-Change:** Nếu một phần tử bị giật, thêm CSS `will-change: transform, opacity;` vào phần tử đó, nhưng phải gỡ bỏ (remove) sau khi animation kết thúc (Dùng callback `onComplete` của GSAP).
- Cấm kích hoạt animation nếu tab trình duyệt đang ẩn hoặc người dùng không nhìn thấy (Sử dụng Intersection Observer hoặc GSAP ScrollTrigger để culling).
- **Accessibility Guard:** BẮT BUỘC có khối điều kiện kiểm tra `prefers-reduced-motion: reduce`. Nếu `true`, hủy toàn bộ biến đổi phức tạp, rút ngắn `duration` về `0` hoặc loại bỏ ScrollTrigger.

---

## 14. MOTION CHECKLIST
Trước khi Frontend Merge Code chứa Animation, Developer phải kiểm tra:
- [ ] Không sử dụng CSS Transitions xen lẫn GSAP trên cùng một thuộc tính của cùng một phần tử (Gây xung đột giật hình).
- [ ] Hàm Cleanup (Revert) của GSAP đã được gọi chính xác khi Component Unmount chưa?
- [ ] Cuộn liên tục lên xuống nhiều lần với tốc độ cao, các phần tử có bị kẹt ở trạng thái lơ lửng không?
- [ ] Có xuất hiện lỗi giật cục (Layout Jumps) do `pin: true` tạo class tạm của GSAP chưa?
- [ ] Đã test trên Safari/iOS chưa? (ScrollTrigger thường có lỗi bar-address trên mobile, cần dùng `ScrollTrigger.normalizeScroll(true)` nếu thực sự cần thiết).
