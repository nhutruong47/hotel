# ADR-0005: Giữ xác thực bằng server session, chuyển sang Spring Session Redis

- **Status**: Accepted
- **Date**: 2026-11-20
- **Liên quan**: `config/SessionAuthBridgeFilter.java`, `security/CsrfCookieFilter.java`, `config/RateLimitFilter.java`, `common/service/LoginAttemptService.java`

## Context

Backend hiện xác thực bằng `HttpSession` (JSESSIONID, HttpOnly, Secure ở prod), và `SessionAuthBridgeFilter` nạp lại User từ DB ở mỗi request. Nhờ đó, khi tài khoản bị disable hoặc đổi role thì có hiệu lực ngay. Frontend Next.js gọi API qua rewrite cùng origin với `credentials: 'include'` và không lưu token trong JS.

Session, rate limit và login lockout đều nằm trong bộ nhớ JVM. Restart là mất hết, và không chạy được 2 instance.

## Problem

Cần một cơ chế xác thực an toàn trước XSS và CSRF, thu hồi được phiên ngay lập tức, và cho phép scale ngang hoặc rolling deploy mà không đăng xuất người dùng.

## Options Considered

| Option | Ưu | Nhược với Nhu Villas |
|---|---|---|
| A. Giữ session in-memory | Không đổi gì | Không scale; restart là đăng xuất mọi người; lockout bị reset |
| B. JWT access token + refresh token | Stateless | Thu hồi khó, phải có blacklist (rốt cuộc lại cần Redis). Lưu ở localStorage thì dễ bị XSS; lưu ở cookie thì vẫn phải chống CSRF. Phải viết lại auth ở cả FE và BE |
| C. **Server session lưu ở Redis (Spring Session Data Redis)** | Giữ nguyên mô hình hiện tại và FE; thu hồi tức thì; scale ngang; Redis đã có trong compose | Phụ thuộc Redis; khi Redis ngừng thì mọi người phải đăng nhập lại |
| D. Session trong PostgreSQL (Spring Session JDBC) | Không thêm phụ thuộc | Mỗi request thêm truy vấn vào DB chính; phải dọn session hết hạn |

## Decision

Chọn **C**.
- Session lưu ở Redis với namespace `nhu:session`, timeout 30 phút trượt. **[CẦN XÁC NHẬN]** thời lượng "remember me".
- Cookie session: `HttpOnly; Secure; SameSite=Lax`. Chỉ dùng `None` nếu FE và BE khác site; đích deploy VPS chung domain nên dùng `Lax`.
- CSRF: giữ double-submit `XSRF-TOKEN`. Cookie này **không** được có thuộc tính `HttpOnly` (đã sửa ở P0).
- Rate limit và login lockout chuyển sang Redis (Bucket4j + Redis / `INCR` có TTL).
- Đổi session ID khi đăng nhập (session fixation), và hủy toàn bộ session của user khi đổi mật khẩu hoặc bị disable.

## Consequences

- Rolling deploy không đăng xuất người dùng; chạy được 2 instance phía sau nginx.
- Redis trở thành thành phần quan trọng: cần healthcheck, `requirepass`, persistence AOF và alert.
- `SessionAuthBridgeFilter` vẫn nạp user từ DB. Có thể cache ngắn (ví dụ 30 giây) nếu cần hiệu năng.

## Trade-offs

- Không có tính stateless như JWT. Với ứng dụng web một domain, không có mobile app hay API bên thứ ba, JWT không mang lại lợi ích tương xứng với rủi ro.
- Redis ngừng thì người dùng bị đăng xuất. Chấp nhận được, có alert và cơ chế tự khởi động lại.
