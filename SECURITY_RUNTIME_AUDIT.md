# SECURITY_RUNTIME_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| RBAC | 70% | Một số endpoint contact admin-like chưa admin-only; role update thiếu allowlist. | Method security/role annotations theo endpoint. |
| CSRF | 40% | Filter yêu cầu token nhưng frontend không gửi token. | Client echo cookie token; test tất cả mutation. |
| Rate limiting | 60% | Có cả Filter và Interceptor, double-limit không thống nhất. | Chọn một implementation, memory cleanup. |
| Security headers | 70% | CSP trong Spring config rỗng. | CSP policy cụ thể và test header. |

## Bugs

### SEC-001 - Critical - CSRF filter chặn mọi mutation frontend
- Mô tả: `CsrfCookieFilter` yêu cầu `X-XSRF-TOKEN` cho POST/PUT/PATCH/DELETE, nhưng `api.request` chỉ gửi `Accept`/`Content-Type` và không đọc cookie.
- Nguyên nhân: Backend custom CSRF và frontend client không tích hợp.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/security/CsrfCookieFilter.java:58`; `frontend/src/shared/api/client.ts:28`.
- Cách tái hiện: GET page để nhận cookie, sau đó POST `/api/v1/bookings` từ frontend client; request thiếu header.
- Ảnh hưởng: Booking, wishlist, profile, admin, payment mutation bị 403 nếu filter active.
- Hướng khắc phục: Client đọc `XSRF-TOKEN` cookie và gửi `X-XSRF-TOKEN`; hoặc dùng Spring CSRF repository chuẩn.

### SEC-002 - High - Contact ticket PII có thể bị user authenticated đọc
- Mô tả: `/api/v1/contact/{id}` không check admin; SecurityConfig chỉ yêu cầu authenticated cho `/api/v1/**`.
- Nguyên nhân: Admin contact methods nằm trong ContactApi public.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/ContactApi.java:58`; `hotel/src/main/java/com/hsf/hotel/config/SecurityConfig.java:116`.
- Cách tái hiện: Login user thường, GET `/api/v1/contact/1`.
- Ảnh hưởng: IDOR/PII leakage.
- Hướng khắc phục: Admin-only matcher or move endpoints under `/api/v1/admin`.

### SEC-003 - Medium - Rate limit double implementation và memory map không cleanup định kỳ
- Mô tả: Có `RateLimitFilter` và `RateLimiterInterceptor`, cả hai maintain in-memory map per IP.
- Nguyên nhân: Hai implementation song song.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/config/RateLimitFilter.java:46`; `hotel/src/main/java/com/hsf/hotel/config/RateLimiterInterceptor.java:51`; `hotel/src/main/java/com/hsf/hotel/config/WebMvcConfig.java:34`.
- Cách tái hiện: Gửi 61 request/min tới API; behavior chịu cả filter/interceptor tùy path.
- Ảnh hưởng: Khó dự đoán limit, memory tăng theo IP cardinality.
- Hướng khắc phục: Một limiter duy nhất, TTL cleanup, Redis/shared limiter nếu multi-node.

### SEC-004 - Medium - CSP config rỗng
- Mô tả: Spring Security gọi `.contentSecurityPolicy(csp -> {})` không set directives.
- Nguyên nhân: Placeholder chưa hoàn tất.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/config/SecurityConfig.java:85`.
- Cách tái hiện: Inspect response headers.
- Ảnh hưởng: XSS blast radius lớn hơn, đặc biệt có `dangerouslySetInnerHTML` frontend.
- Hướng khắc phục: Set CSP explicit theo Next/static assets/API.
