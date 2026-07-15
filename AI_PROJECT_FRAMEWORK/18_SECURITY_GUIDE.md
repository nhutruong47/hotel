# 18. SECURITY GUIDE FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là bộ khung chuẩn hóa các nguyên tắc Bảo mật (Security) trong quá trình phát triển Frontend và tương tác với Backend. Bảo mật không phải là một chức năng thêm vào, mà là lớp nền móng bắt buộc từ ngày code dòng đầu tiên. Mục tiêu là triệt tiêu hoàn toàn các lỗ hổng OWASP Top 10.

---

## 1. AUTHENTICATION (XÁC THỰC DANH TÍNH)
- Không bao giờ lưu trữ mật khẩu thuần (Plain text). Backend phải mã hóa (Bcrypt/Argon2).
- **JWT (JSON Web Token):** Nếu sử dụng JWT, hạn sử dụng của Access Token phải cực kỳ ngắn (Ví dụ: 15 phút). Dùng Refresh Token để lấy lại Access Token mới.
- Hỗ trợ/Bắt buộc MFA (Xác thực đa yếu tố) cho các tính năng nhạy cảm (Đăng nhập hệ thống quản trị, Chuyển tiền).
- Khóa tài khoản (Account Lockout) sau N lần nhập sai mật khẩu để chống Brute Force.

## 2. AUTHORIZATION (PHÂN QUYỀN TRUY CẬP)
- Trách nhiệm phân quyền (RBAC - Role Based Access Control) là của Backend. Frontend chỉ ẩn/hiện UI tương ứng với quyền của người dùng.
- Mọi API endpoint nhạy cảm đều phải được kiểm tra lại quyền (Permission check) tại Backend, không phụ thuộc vào việc Frontend đã ẩn nút hay chưa.

## 3. XSS (CROSS-SITE SCRIPTING)
XSS xảy ra khi tin tặc chèn mã độc Javascript vào dữ liệu người dùng (Ví dụ: ô bình luận) và đoạn mã đó được thực thi trên máy của người khác.
- **Phòng chống:** Mặc định React/Vue đã tự động escape dữ liệu khi render ra DOM. TUYỆT ĐỐI CẤM sử dụng `dangerouslySetInnerHTML` hoặc `v-html` đối với dữ liệu người dùng nhập vào.
- Nếu bắt buộc phải render HTML từ người dùng (như Rich Text Editor), phải dùng thư viện thanh lọc (Sanitizer) như `DOMPurify` trước khi render.

## 4. CSRF (CROSS-SITE REQUEST FORGERY)
Xảy ra khi tin tặc lừa người dùng nhấp vào một link/form độc hại khiến trình duyệt tự động gửi Request (kèm Cookie hợp lệ) tới Server mục tiêu.
- **Phòng chống (Frontend):** Nếu dùng kiến trúc SPA + API, không dùng thẻ `<form action="...">` để POST dữ liệu. Thay vào đó, dùng `fetch`/`axios` đọc dữ liệu và gửi đi kèm Custom Headers (như `Authorization: Bearer <token>`). Các framework backend mặc định từ chối các request không chứa Token hoặc CSRF Token riêng biệt.

## 5. CSP (CONTENT SECURITY POLICY)
Cơ chế mạnh mẽ nhất để chống lại XSS và Data Injection.
- Cấu hình thẻ `<meta http-equiv="Content-Security-Policy">` hoặc thiết lập CSP Headers từ Server.
- Chỉ định rõ ràng danh sách các domain (Whitelists) được phép load file JS, CSS, Font, Image, Iframe.
- **Quy tắc Vàng:** `default-src 'self'`. Ngăn chặn việc chạy các script nội tuyến (inline scripts `script-src 'unsafe-inline'`) hoặc hàm `eval()`.

## 6. SECURE HEADERS (TIÊU ĐỀ BẢO MẬT)
Cấu hình máy chủ (Nginx, Vercel, Netlify) trả về các HTTP Headers sau:
- `Strict-Transport-Security (HSTS):` Bắt buộc trình duyệt chỉ kết nối bằng HTTPS.
- `X-Frame-Options: DENY` (hoặc `SAMEORIGIN`): Chống Clickjacking (Bị nhúng web mình vào một iframe của trang web giả mạo).
- `X-Content-Type-Options: nosniff`: Ngăn trình duyệt tự ý đoán định dạng file, tránh việc kẻ thù biến file ảnh thành file thực thi mã độc.

## 7. COOKIE STRATEGY (CHIẾN LƯỢC QUẢN LÝ COOKIE)
Nếu dự án dùng Cookie để lưu trữ Session ID hoặc Refresh Token, Cookie ĐÓ BẮT BUỘC PHẢI CÓ các thuộc tính sau (Thiết lập từ Backend):
- `HttpOnly: true` -> Javascript phía Client không thể đọc được cookie này (Chống XSS lấy trộm cookie).
- `Secure: true` -> Chỉ gửi cookie qua đường truyền HTTPS được mã hóa.
- `SameSite: Strict` (hoặc `Lax`) -> Trình duyệt không gửi cookie đính kèm trong các request xuyên miền (Chống CSRF).

## 8. ENVIRONMENT VARIABLES & SECRET MANAGEMENT
- File `.env` chứa các keys kết nối Backend/API. **Tuyệt đối cấm** push file này lên Git.
- Phân biệt rõ **Public Keys** (được nhúng vào file JS ở Client) và **Secret Keys** (chỉ tồn tại ở Server). Trong Vite, chỉ các biến bắt đầu bằng `VITE_` mới bị nhúng vào Client. Đừng bao giờ đặt `VITE_DB_PASSWORD`.
- Quản lý Secret thông qua các công cụ của CI/CD (GitHub Secrets) hoặc Key Vault (AWS KMS).

## 9. API SECURITY (BẢO MẬT GIAO TIẾP API)
- Rate Limiting (Giới hạn tỷ lệ): Backend phải chặn nếu một IP gửi quá N request/giây để chống Spam/DDoS.
- Frontend không nên lưu trữ bất kỳ cấu trúc Business Logic quá phức tạp hoặc thuật toán tính giá cả nào (những thứ dễ bị user soi code qua F12). Thuật toán phải nằm ở Server.

## 10. UPLOAD SECURITY (BẢO MẬT TẢI LÊN)
- Giới hạn kích thước file tải lên (Ví dụ: Max 5MB).
- **Frontend Validation:** Kiểm tra định dạng đuôi file (`.jpg`, `.png`).
- **Backend Validation:** Không tin tưởng Frontend. Backend BẮT BUỘC phải đọc "Magic Number" (bytes đầu tiên của file) để kiểm chứng đó là ảnh thật sự chứ không phải file virus `.exe` được đổi tên thành `.jpg`.

---

## 11. FRONTEND SECURITY CHECKLIST
Trước khi triển khai lên Production, Developer phải xác nhận:

- [ ] Dự án hoàn toàn KHÔNG sử dụng `dangerouslySetInnerHTML` bừa bãi?
- [ ] File `.env` và các thư mục config nhạy cảm đã bị `.gitignore` chưa?
- [ ] Headers tĩnh của server đã được bổ sung `X-Frame-Options`, `CSP` và `HSTS` chưa?
- [ ] Không có password hard-code hay Secret Key (Ví dụ JWT Secret) nào nằm phơi bày trong mã nguồn Client-side?
- [ ] Toàn bộ các API gọi ra bên ngoài đã dùng giao thức `HTTPS` chưa?
- [ ] Mọi input form từ người dùng đều được giới hạn số ký tự (MaxLength) chưa?
