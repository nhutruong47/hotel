# 10. PERFORMANCE GUIDE FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là bộ khung (Framework) tối ưu hóa hiệu năng (Performance Optimization) cho mọi dự án Web. Hiệu năng không phải là bước "làm thêm" ở cuối dự án, mà là một phần cốt lõi của Kiến trúc và Trải nghiệm Người dùng. Mục tiêu mặc định là điểm Lighthouse Performance ≥ 95.

---

## 1. CORE WEB VITALS (CÁC CHỈ SỐ CỐT LÕI)
Google định nghĩa 3 chỉ số quan trọng nhất quyết định hiệu năng thực tế và thứ hạng SEO:
- **LCP (Largest Contentful Paint):** Thời gian hiển thị phần tử lớn nhất.
- **CLS (Cumulative Layout Shift):** Mức độ xô lệch bố cục trang.
- **INP (Interaction to Next Paint):** Độ trễ phản hồi tương tác.

### 1.1 LCP (Largest Contentful Paint)
- **Tiêu chuẩn:** LCP phải < 2.5 giây.
- **Quy tắc:**
  - Phần tử LCP thường là Hero Image, Video tĩnh hoặc H1 Text.
  - Bắt buộc dùng `fetchPriority="high"` và `loading="eager"` cho ảnh LCP.
  - Cấm sử dụng Lazy Loading cho LCP.
  - Preload ảnh LCP bằng thẻ `<link rel="preload" as="image" href="...">`.

### 1.2 CLS (Cumulative Layout Shift)
- **Tiêu chuẩn:** CLS phải < 0.1.
- **Quy tắc:**
  - Mọi thẻ `<img>` và `<video>` đều PHẢI có sẵn thuộc tính `width` và `height`, hoặc CSS `aspect-ratio` để trình duyệt giữ chỗ trước khi ảnh tải xong.
  - Cấm chèn quảng cáo, popup, banner động đẩy nội dung chính xuống dưới mà không có bộ khung (Skeleton) giữ chỗ trước.
  - Font web phải dùng `font-display: swap` kết hợp với font fallback có cùng kích thước để không bị nhảy chữ khi font web load xong.

### 1.3 INP (Interaction to Next Paint)
- **Tiêu chuẩn:** INP phải < 200 milliseconds.
- **Quy tắc:**
  - Không block Main Thread (Luồng chính).
  - Tránh các vòng lặp Javascript khổng lồ hoặc các logic tính toán nặng ngay lúc user click. Nếu có, đẩy sang Web Worker.
  - Đảm bảo các micro-interaction (như nút hover/active) phản hồi ngay lập tức bằng CSS thay vì phụ thuộc vào JS React state.

### 1.4 TTFB (Time to First Byte)
- **Tiêu chuẩn:** TTFB phải < 800 milliseconds.
- **Quy tắc:**
  - Giảm thiểu thời gian xử lý server-side.
  - Áp dụng các tầng Caching hoặc CDN để trả file tĩnh ngay lập tức.

---

## 2. ASSETS OPTIMIZATION (TỐI ƯU TÀI NGUYÊN)

### 2.1 Image Optimization
- Định dạng: Bắt buộc dùng **WebP** hoặc **AVIF**. Chấm dứt sử dụng PNG/JPG dung lượng lớn trừ thẻ Open Graph.
- Độ phân giải: Sử dụng `srcSet` và `sizes` để trình duyệt tải ảnh phù hợp với thiết bị (Mobile tải ảnh 640w, Desktop tải ảnh 1920w).
- Không được phục vụ một bức ảnh 4K cho một khung hình 300x300 trên Mobile.

### 2.2 Video Optimization
- Background Video: Bắt buộc loại bỏ Audio track (muted), nén sang WebM hoặc MP4 tối ưu (H.264). Bật tính năng `playsinline`, `loop`.
- Không tự động tải video nếu người dùng đang dùng 3G/4G (sử dụng thuộc tính `preload="none"` hoặc `preload="metadata"` cho video không quan trọng).

### 2.3 Font Optimization
- Chỉ load các `font-weight` thực sự cần dùng (VD: 300, 400, 600). Đừng load toàn bộ Typeface.
- Preconnect tới Google Fonts: `<link rel="preconnect" href="https://fonts.googleapis.com">`.
- Bắt buộc dùng `font-display: swap`.

---

## 3. RENDERING & JAVASCRIPT TỐI ƯU

### 3.1 Lazy Loading (Tải lười)
- Tất cả các hình ảnh, video nằm bên dưới màn hình đầu tiên (Below the fold) phải có `loading="lazy"`.
- Bổ sung `decoding="async"` để giải mã hình ảnh chạy dưới background, không chặn main thread.

### 3.2 Dynamic Import & Code Splitting
- Chia nhỏ Bundle JS. Không bắt User tải toàn bộ ứng dụng ở trang đầu tiên.
- Các Route không nhìn thấy phải được tải bất đồng bộ: `const Dashboard = React.lazy(() => import('./Dashboard'))`.
- Các thư viện nặng (VD: Mapbox, Chart.js, Lottie) phải được **Dynamic Import** chỉ khi nào phần tử chứa nó chuẩn bị cuộn tới màn hình.

### 3.3 Tree Shaking
- Đảm bảo Bundler (Vite/Webpack) loại bỏ các module code không sử dụng (Dead Code).
- Hạn chế dùng các thư viện import toàn cục (Ví dụ: `import _ from 'lodash'`). Hãy dùng import định danh (`import { debounce } from 'lodash'`).

---

## 4. NETWORK STRATEGY

### 4.1 Caching (Lưu bộ nhớ đệm)
- Static Assets (JS, CSS, Images, Fonts): Header `Cache-Control` phải thiết lập Max-age dài nhất có thể (1 năm) + Immutable. Khi có thay đổi, đổi tên file bằng hash (VD: `app.[hash].js`).
- HTML: Header `Cache-Control` phải là `no-cache` để luôn kiểm tra phiên bản mới nhất.

### 4.2 CDN Strategy
- Mọi tài nguyên tĩnh phải được phân phối qua Mạng phân phối nội dung (CDN) như Cloudflare, Vercel Edge, AWS CloudFront để giảm độ trễ vật lý.

---

## 5. LIGHTHOUSE CHECKLIST
Trước khi Deploy lên Production, Developer phải Audit bằng Lighthouse (Incognito Mode) và pass các điều kiện sau:
- [ ] Điểm Performance tổng ≥ 95 (trên Desktop).
- [ ] Hero Image được cấp `fetchPriority="high"` và KHÔNG bị lazy load.
- [ ] Mọi hình ảnh (cả ảnh layout và ảnh từ API) đều có explicit `width` và `height` (hoặc `aspect-ratio`).
- [ ] Không có báo cáo "Reduce unused JavaScript" nghiêm trọng.
- [ ] Render-blocking resources (CSS/JS chặn render) đã được giảm thiểu tối đa.
- [ ] Web Fonts không gây tàng hình chữ (FOIT) trong lúc tải.
