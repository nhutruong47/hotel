# 21. API DESIGN GUIDE FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là bộ khung (Framework) quy chuẩn thiết kế RESTful API. Hệ thống API là "Hợp đồng" (Contract) giao tiếp giữa Frontend và Backend. Việc thiết kế API bừa bãi sẽ dẫn đến hậu quả nghiêm trọng về hiệu năng, bảo mật và khả năng bảo trì. Mọi API phải được thiết kế theo đúng quy chuẩn dưới đây.

---

## 1. API DESIGN PRINCIPLES (NGUYÊN TẮC THIẾT KẾ)
- **Dễ dự đoán (Predictable):** Cấu trúc endpoint và dữ liệu trả về phải tuân theo một quy luật duy nhất. Không được lúc thì trả về Array, lúc thì trả về Object cho cùng một danh sách.
- **Client-agnostic (Không phụ thuộc Client):** API được sinh ra để phục vụ dữ liệu nghiệp vụ, không phục vụ riêng cho một màn hình UI cụ thể nào của Frontend/Mobile.
- **Stateless (Không lưu trạng thái):** Mỗi request phải chứa đủ toàn bộ thông tin để server xử lý (qua Token/Header), server không lưu trữ session ở bộ nhớ tạm.

## 2. RESTFUL CONVENTION & RESOURCE NAMING (QUY CHUẨN ĐẶT TÊN)
- Tên tài nguyên (Resource) trong URI phải luôn là **Danh từ số nhiều (Plural nouns)**. Không dùng động từ. Động từ đã được thể hiện qua HTTP Method.
  - ✅ ĐÚNG: `/users`, `/articles`, `/users/123/orders`
  - ❌ SAI: `/getUser`, `/createArticle`, `/user/123/order`
- Đặt tên theo chuẩn **kebab-case** (chữ thường, ngăn cách bằng dấu gạch ngang) cho các từ ghép.
  - ✅ ĐÚNG: `/user-profiles`
  - ❌ SAI: `/userProfiles`, `/User_Profiles`

## 3. HTTP METHOD RULE (QUY CHUẨN PHƯƠNG THỨC)
Sử dụng chính xác phương thức HTTP để biểu thị hành động:
- `GET`: Lấy dữ liệu. (Tuyệt đối không làm thay đổi trạng thái database).
- `POST`: Tạo mới một tài nguyên (Create) hoặc thực hiện hành động không thuộc chuẩn CRUD (VD: `/users/login`).
- `PUT`: Cập nhật TOÀN BỘ tài nguyên (Replace). Cần gửi đủ mọi fields.
- `PATCH`: Cập nhật MỘT PHẦN tài nguyên (Modify). Chỉ gửi các fields cần đổi.
- `DELETE`: Xóa tài nguyên. Có thể là Hard delete hoặc Soft delete.

## 4. STATUS CODE RULE (MÃ TRẠNG THÁI HTTP)
- **2xx (Thành công):**
  - `200 OK`: Thành công chuẩn.
  - `201 Created`: Tạo mới thành công (Nên trả về object vừa tạo kèm Header `Location`).
  - `204 No Content`: Thành công nhưng không có dữ liệu trả về (Thường dùng cho DELETE).
- **4xx (Lỗi từ Client):**
  - `400 Bad Request`: Validation fail, data thiếu hoặc sai định dạng.
  - `401 Unauthorized`: Chưa đăng nhập hoặc Token hết hạn/sai.
  - `403 Forbidden`: Đã đăng nhập nhưng không đủ quyền (Role) để xem.
  - `404 Not Found`: API không tồn tại hoặc ID không tìm thấy trong DB.
  - `429 Too Many Requests`: Vượt quá số lượng request cho phép (Rate limiting).
- **5xx (Lỗi từ Server):**
  - `500 Internal Server Error`: Code Backend bị crash. Đừng bao giờ ném lỗi này ra nếu có thể bắt (catch) được.

## 5. RESPONSE FORMAT STANDARD (ĐỊNH DẠNG TRẢ VỀ)
Mọi API JSON phải bọc dữ liệu trong một Envelope (Vỏ bọc) chuẩn xác:

**Trả về List/Phân trang (Pagination):**
```json
{
  "data": [
    { "id": 1, "name": "Item 1" }
  ],
  "meta": {
    "page": 1,
    "limit": 20,
    "totalRecords": 150,
    "totalPages": 8
  }
}
```

**Trả về Object đơn:**
```json
{
  "data": { "id": 1, "name": "Item 1" }
}
```

## 6. ERROR RESPONSE STANDARD (ĐỊNH DẠNG LỖI)
Bắt buộc phải trả về một cấu trúc lỗi thống nhất để Frontend dễ dàng parse và hiển thị:
```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Thông tin bạn nhập không hợp lệ.",
    "details": [
      { "field": "email", "issue": "Email không đúng định dạng" },
      { "field": "password", "issue": "Mật khẩu quá ngắn" }
    ]
  }
}
```
*Lưu ý: Tuyệt đối không ném Stacktrace (Lỗi chi tiết dòng code) ra môi trường Production.*

## 7. PAGINATION, FILTERING, SORTING & SEARCH
Mọi hành động thao tác với danh sách (List) phải thông qua Query Parameters (URL `?key=value`).
- **Pagination:** Sử dụng Limit/Offset hoặc Page/Limit. (VD: `?page=2&limit=15`).
- **Sorting:** Quy định field và chiều sắp xếp. Tiền tố `-` thường biểu thị DESC. (VD: `?sort=-createdAt,price`).
- **Filtering:** Lọc theo các trường cụ thể. (VD: `?status=active&role=admin`).
- **Search:** Dùng tham số `q` hoặc `search` cho tìm kiếm Full-text (VD: `?q=nguyen+van+a`).

## 8. VERSIONING STRATEGY (CHIẾN LƯỢC PHIÊN BẢN)
Mọi API phải được gắn phiên bản ngay từ ngày đầu tiên để không phá vỡ (Breaking change) các hệ thống cũ khi cập nhật.
- Sử dụng URI Versioning: `/api/v1/users`, `/api/v2/users`.
- Chỉ tăng `v2` khi sự thay đổi đó đập bỏ cấu trúc trả về hoặc yêu cầu dữ liệu bắt buộc mới mà Client `v1` không thể đáp ứng.

## 9. API SECURITY (BẢO MẬT API) & AUTHENTICATION
- Mọi API (ngoại trừ public API như Login/Register) phải yêu cầu xác thực qua Header: `Authorization: Bearer <token>`.
- **CORS:** Cấu hình chuẩn xác danh sách Domain được phép gọi API (Whitelists). Tránh dùng cờ `*` (All) trên Production.
- **Rate Limiting:** Thiết lập chặn nếu 1 IP gửi > 100 requests/phút vào chung một endpoint để chống DDoS và dò mật khẩu.
- **Request Validation:** Backend phải validate lại toàn bộ dữ liệu gửi lên. Không bao giờ tin tưởng Validation của Frontend.

## 10. OPENAPI / SWAGGER CONVENTION & DOCUMENTATION
- API chưa có tài liệu (Documentation) được coi là API chưa hoàn thành.
- Mọi dự án bắt buộc sử dụng chuẩn OpenAPI 3.0 (Sinh ra giao diện Swagger hoặc Redoc).
- Tài liệu phải định nghĩa rõ: Input/Output Schemas, Data Types, Ràng buộc Required/Optional, và mô tả rõ ý nghĩa của các Error Codes.

---

## 11. API CHECKLIST
Trước khi Backend Developer bàn giao API cho Frontend ghép (Integrate), hãy kiểm tra:
- [ ] URI đã dùng danh từ số nhiều và chuẩn kebab-case chưa?
- [ ] Phương thức HTTP có vi phạm nguyên tắc không? (Dùng POST để lấy dữ liệu thay vì GET?).
- [ ] Status Code có trả về chính xác không hay mọi thứ đều là HTTP 200 kèm theo `{"status": false}`?
- [ ] Cấu trúc JSON trả về có đúng chuẩn `{ data, meta }` không?
- [ ] Swagger Docs đã cập nhật chính xác các field mới được thêm vào DB chưa?
