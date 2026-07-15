# 13. DEPLOY & DEVOPS GUIDE FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là quy trình Tiêu chuẩn hóa việc Triển khai mã nguồn (Deployment) và Quản lý Cơ sở hạ tầng (DevOps). Mục tiêu là đạt được "Zero-downtime deployment" (Triển khai không gián đoạn) và tự động hóa tối đa quy trình đưa code từ máy cá nhân lên môi trường Production.

---

## 1. QUẢN LÝ MÃ NGUỒN (VERSION CONTROL)

### 1.1 Git Flow & Branch Strategy
Dự án áp dụng mô hình Git Flow rút gọn (Trunk-based development hoặc GitHub Flow tùy quy mô).
- **`main` / `master`:** Nhánh Production. Code ở nhánh này BẮT BUỘC phải luôn trong trạng thái có thể Deploy bất cứ lúc nào. Cấm push trực tiếp lên nhánh này.
- **`staging` / `develop`:** Nhánh Tiền kiểm thử. Nơi gom tính năng để QC/Tester thực hiện test trước khi release.
- **`feature/<name>`:** Nhánh tính năng mới, rẽ nhánh từ `develop`.
- **`hotfix/<name>`:** Nhánh sửa lỗi khẩn cấp trên Production, rẽ nhánh trực tiếp từ `main`.
- **Quy tắc Merge:** Bắt buộc phải thông qua Pull Request (PR) hoặc Merge Request. Cần ít nhất 1 người (Code Reviewer) Approve trước khi merge.

---

## 2. CI/CD (TÍCH HỢP & TRIỂN KHAI LIÊN TỤC)

### 2.1 Build Process (Quy trình Build)
Sử dụng GitHub Actions, GitLab CI hoặc Bitbucket Pipelines để chạy các tác vụ tự động ngay khi có code mới push lên.
1. **Lint & Format:** Chạy ESLint, Prettier. Ngưng Build nếu code sai chuẩn.
2. **Type Check:** Chạy `tsc --noEmit`. Ngưng Build nếu sai kiểu dữ liệu TypeScript.
3. **Test:** Chạy toàn bộ Unit Test & Integration Test. Ngưng Build nếu Test fail.
4. **Build:** Chạy lệnh build (VD: `npm run build`).

### 2.2 Environment (Môi trường)
Tách biệt nghiêm ngặt 3 môi trường:
- **Development (Local):** Dữ liệu giả, chạy trên máy Dev (`localhost`).
- **Staging/Preview:** Môi trường test, giống Production 99%, kết nối với Staging Database.
- **Production:** Môi trường thật, kết nối với Live Database.

---

## 3. CHIẾN LƯỢC HOSTING (HOSTING STRATEGIES)

### 3.1 Dành cho Jamstack / SPA (Vercel & Netlify)
- Đây là lựa chọn số 1 cho React/Vite SPA hoặc Next.js.
- **Vercel / Netlify:** Tự động hóa hoàn toàn CI/CD. Tự động tạo Preview Link cho mỗi Pull Request. Tự phân phối tài nguyên qua hệ thống Edge CDN toàn cầu. Tự động cấp phát SSL.

### 3.2 Dành cho Máy chủ riêng (VPS / Dedicated Server)
Sử dụng trong trường hợp hệ thống đòi hỏi tính bảo mật cao, kết nối trực tiếp với Database nội bộ hoặc code backend.
- **Docker:** Mọi ứng dụng (Frontend, Backend, DB) BẮT BUỘC phải được container hóa (Containerization) bằng Docker. Khởi chạy thông qua `docker-compose.yml`. Cấm việc cài đặt thẳng Node.js/NPM rác lên VPS.
- **Nginx:** Đóng vai trò là Reverse Proxy. Xử lý điều hướng domain, cân bằng tải (Load Balancing) và nén Gzip/Brotli trước khi trả file về cho Client.
- **PM2:** (Nếu không dùng Docker), sử dụng PM2 để chạy tiến trình Node.js dưới nền và tự động khởi động lại khi server restart.

---

## 4. TỐI ƯU HẠ TẦNG MẠNG (INFRASTRUCTURE)

### 4.1 Domain & DNS
- Trỏ Domain thông qua bản ghi `A` (vào IP của VPS) hoặc bản ghi `CNAME` (nếu dùng Vercel/Netlify).
- Cấu hình bản ghi MX cho Email và TXT để verify domain.

### 4.2 SSL / HTTPS
- Tuyệt đối cấm các giao thức `HTTP` trên Production. Website phải có "Ổ khóa xanh".
- Tự động hóa gia hạn chứng chỉ SSL miễn phí bằng **Let's Encrypt** (thông qua Certbot trên VPS) hoặc mặc định của Vercel/Cloudflare.

### 4.3 CDN (Mạng phân phối nội dung)
- Tích hợp Cloudflare (Bật đám mây cam) để hứng chịu các đợt tấn công DDoS, cache tài nguyên tĩnh ở Edge Server gần người dùng nhất và giấu IP thật của VPS.

---

## 5. QUẢN TRỊ & BẢO TRÌ

### 5.1 Monitoring (Giám sát)
- Theo dõi Uptime: Sử dụng UptimeRobot hoặc Better Uptime để nhận cảnh báo (Email/Slack/Telegram) nếu Website bị "sập" (Downtime).
- Theo dõi Lỗi Frontend (Error Tracking): Tích hợp **Sentry** để bắt các lỗi Javascript chưa xử lý bên dưới máy client, gởi report về dashboard cho Dev.
- Theo dõi Traffic & Event: Tích hợp Google Analytics (GA4) hoặc Plausible.

### 5.2 Rollback Strategy (Chiến lược khôi phục)
- Nếu bản Release mới gặp lỗi nghiêm trọng (Critical Bug) trên Production:
  - **Trên Vercel/Netlify:** Nhấn nút "Instant Rollback" để revert hệ thống về bản build trước đó trong vòng 1 giây.
  - **Trên VPS/Docker:** Deploy lại image của version trước (VD: `docker run myapp:v1.0.1` thay vì `v1.0.2`), hoặc git revert commit lỗi và đẩy hotfix lên nhánh `main`.

---

## 6. DEPLOY CHECKLIST
Trước khi ấn nút Deploy hoặc Merge vào nhánh `main`, Tech Lead phải kiểm tra:

- [ ] Các biến môi trường (.env) trên Production đã được set up chính xác chưa? (Tuyệt đối không nhầm API endpoint của Staging).
- [ ] Tính năng mã hóa/Bảo mật đã được cấu hình chưa? (CORS allowed origins có chặn các domain lạ không).
- [ ] Toàn bộ Source Map của JS/CSS có bị lộ ra ngoài Production không? (Nên tắt `sourcemap: false` khi build prod để bảo mật source code).
- [ ] Robots.txt và Sitemap.xml có đang chặn Googlebot không? (Rất hay quên khi cấu hình từ Staging sang Production).
- [ ] Quá trình CI/CD có chạy thành công 100% (Pass mọi Test/Lint) không?
- [ ] Đã backup Database (nếu có cập nhật liên quan đến dữ liệu) trước khi Release bản mới chưa?
