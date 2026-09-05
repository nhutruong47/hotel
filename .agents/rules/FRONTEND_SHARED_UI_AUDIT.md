# FRONTEND_SHARED_UI_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| API client | 60% | Không xử lý CSRF, hardcoded base `/api/v1`, không hỗ trợ blob download. | CSRF, env/proxy config, download helper. |
| Routing/pages | 75% | Có Next app routes/account/admin/public; một số legacy aliases `/my-bookings`, `/profile`, `/wishlist`. | Redirect aliases hoặc dùng một IA. |
| UI states | 65% | Nhiều loading/empty có; error/a11y không đều. | Componentized error/loading states. |
| i18n/settings | 60% | i18n có status cũ; settings localStorage. | Sync status enum, backend preferences. |

## Bugs

### FE-001 - Critical - API client không gửi CSRF header
- Mô tả: Client không đọc `XSRF-TOKEN` cookie để gửi `X-XSRF-TOKEN`.
- Nguyên nhân: Request headers chỉ set Accept và Content-Type.
- File/Class/Method: `frontend/src/shared/api/client.ts:28`; `hotel/src/main/java/com/hsf/hotel/security/CsrfCookieFilter.java:58`.
- Cách tái hiện: Any non-auth POST/PUT/PATCH/DELETE từ frontend.
- Ảnh hưởng: Mutation bị 403 hoặc nếu CSRF filter bị tắt thì thiếu bảo vệ.
- Hướng khắc phục: Add cookie parser, set header cho unsafe methods, test all mutation.

### FE-002 - High - Contact support hardcode sai API path
- Mô tả: `SupportPage` truyền path có `/api/v1` vào client đã prefix sẵn.
- Nguyên nhân: Không dùng `API_PATHS.contact`.
- File/Class/Method: `frontend/src/features/account/SupportPage.tsx:72`; `frontend/src/shared/api/client.ts:1`.
- Cách tái hiện: Submit account support.
- Ảnh hưởng: Ticket không gửi.
- Hướng khắc phục: Sửa path và thêm lint/test cấm hardcoded `/api/v1`.

### FE-003 - Medium - `dangerouslySetInnerHTML` dùng trong UI text
- Mô tả: ErrorBoundary, AI greeting, terms text dùng `dangerouslySetInnerHTML`.
- Nguyên nhân: Translation/rendering HTML inline.
- File/Class/Method: `frontend/src/shared/components/ErrorBoundary.tsx:62`; `frontend/src/features/ai/AiChatWidget.tsx:112`; `frontend/src/features/auth/AuthPages.tsx:547`.
- Cách tái hiện: Thay translation chứa HTML/script nếu source translation bị compromise.
- Ảnh hưởng: XSS blast radius tăng.
- Hướng khắc phục: Render React nodes/link components thay vì HTML string; CSP.

### FE-004 - Medium - Next rewrite hardcoded localhost backend
- Mô tả: `next.config.js` rewrite `/api/:path*` tới `http://localhost:8081`.
- Nguyên nhân: Không dùng env var.
- File/Class/Method: `frontend/next.config.js:5`.
- Cách tái hiện: Deploy frontend container với backend host khác.
- Ảnh hưởng: Production API calls fail nếu không có proxy cùng host.
- Hướng khắc phục: `process.env.API_INTERNAL_URL` với fallback dev.
