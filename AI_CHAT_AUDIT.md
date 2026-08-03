# AI_CHAT_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| AI recommendation | 65% | Input length/rate riêng chưa đủ, prompt không lọc PII, blocking call. | Limit message length, async timeout/circuit breaker, moderation. |
| Chat history | 70% | Lưu history, clear history; response trả entity trực tiếp. | DTO + retention policy. |

## Bugs

### AI-001 - Medium - AI request không giới hạn độ dài message
- Mô tả: `AiApi.recommend` chỉ kiểm tra blank, không giới hạn length.
- Nguyên nhân: Request body là `Map<String,String>` không DTO validation.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/AiApi.java:44`.
- Cách tái hiện: POST `/api/v1/ai/recommend` với message nhiều MB.
- Ảnh hưởng: Tăng token/cost, chậm request, có thể vượt memory/log.
- Hướng khắc phục: DTO `@Size(max=1000)`, truncate/sanitize trước prompt.

### AI-002 - Medium - AI service block thread tới 60s và retry sleep trong request thread
- Mô tả: `callGeminiApi().block()` timeout 60s, retry sleep 20s/40s/60s hoặc 5s.
- Nguyên nhân: Synchronous blocking WebClient trong request path.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/service/GeminiService.java:118`; `hotel/src/main/java/com/hsf/hotel/service/GeminiService.java:86`; `hotel/src/main/java/com/hsf/hotel/service/GeminiService.java:98`.
- Cách tái hiện: Gemini rate-limit 429, gửi nhiều request AI.
- Ảnh hưởng: Cạn servlet threads, giảm availability.
- Hướng khắc phục: Async job/queue, circuit breaker, retry non-blocking, rate limit riêng AI.

### AI-003 - Low - History API trả entity trực tiếp
- Mô tả: `AiApi.history` trả `List<ChatMessage>`.
- Nguyên nhân: Không có DTO.
- File/Class/Method: `hotel/src/main/java/com/hsf/hotel/api/AiApi.java:36`.
- Cách tái hiện: GET `/api/v1/ai/history`.
- Ảnh hưởng: Contract phụ thuộc entity, khó áp retention/masking.
- Hướng khắc phục: `ChatMessageDTO` chỉ gồm userMessage/aiResponse/createdAt.
