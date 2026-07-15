# 22. DATABASE GUIDE FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là bộ khung (Framework) quy định cấu trúc và nguyên tắc thiết kế Cơ sở dữ liệu (Database Design). Mục tiêu là đảm bảo tính toàn vẹn dữ liệu (Data Integrity), tối ưu truy vấn (Performance), và khả năng mở rộng hệ thống (Scalability) bất kể RDBMS đang sử dụng là MySQL, PostgreSQL hay SQL Server.

---

## 1. DATABASE DESIGN PRINCIPLE (NGUYÊN TẮC THIẾT KẾ)
- **Single Source of Truth:** Mọi dữ liệu chỉ nên có một nguồn duy nhất và chính xác nhất. Hạn chế tối đa sự dư thừa dữ liệu (Data redundancy).
- **Tính toàn vẹn (Integrity):** Database là chốt chặn cuối cùng bảo vệ dữ liệu. Không tin tưởng Validation ở Frontend hay Backend. Mọi quy tắc cốt lõi phải được cài đặt bằng Constraints (Ràng buộc) dưới DB.

## 2. NAMING CONVENTION (QUY CHUẨN ĐẶT TÊN)

### 2.1 Table Naming
- Sử dụng chữ cái thường (lowercase), ngăn cách bằng dấu gạch dưới (`snake_case`).
- Tên bảng phải là **Danh từ số nhiều** (Ví dụ: `users`, `orders`, `products`).
- Bảng trung gian (Junction table) trong quan hệ N-N nên kết hợp tên của 2 bảng liên quan theo thứ tự Alphabet (Ví dụ: `product_tags` thay vì `tag_product`).

### 2.2 Column Naming
- Sử dụng `snake_case`.
- Phải rõ nghĩa, không viết tắt đánh đố (Ví dụ: `registration_date` thay vì `reg_dt`).
- Các trường Boolean nên bắt đầu bằng `is_`, `has_`, `can_` (Ví dụ: `is_active`, `has_discount`).

## 3. KEYS & STRATEGIES (KHÓA & CHIẾN LƯỢC)

### 3.1 Primary Key Rule (Khóa chính)
- Mọi bảng ĐỀU PHẢI có một Khóa chính (`id`).
- Cấm sử dụng thông tin có thể thay đổi (như `email` hay `username`) làm Khóa chính.

### 3.2 UUID Strategy
- Ưu tiên sử dụng `UUID` (Universally Unique Identifier) thay vì `Auto-increment ID` (INT) cho các bảng public ra bên ngoài, để tránh việc hacker dò đoán số lượng bản ghi bằng cách thay đổi ID trên URL (IDOR Attack).
- Nếu dùng Auto-increment ID cho tốc độ Index nhanh, tuyệt đối không được expose ID thực sự ra API, mà phải mã hóa (Hashids) hoặc tạo một cột `uuid` phụ để giao tiếp.

### 3.3 Foreign Key Rule (Khóa ngoại)
- Bắt buộc phải thiết lập Foreign Key (FK) ở tầng Database để đảm bảo toàn vẹn tham chiếu. Cấm việc "chỉ tạo FK ảo bằng code Backend".
- Khi xóa bản ghi cha, quy định rõ hành vi ở bản ghi con: `ON DELETE CASCADE` (xóa theo) hoặc `ON DELETE RESTRICT` (cấm xóa nếu còn con).

## 4. CONSTRAINTS & COLUMNS (RÀNG BUỘC VÀ CỘT)

### 4.1 Constraint Rule
- `NOT NULL`: Mọi cột không cho phép để trống BẮT BUỘC phải set `NOT NULL`.
- `UNIQUE`: Cấu hình chống trùng lặp dữ liệu (Ví dụ: `email`, `phone`).
- `DEFAULT`: Sử dụng giá trị mặc định hợp lý ở tầng DB thay vì ép Backend phải luôn truyền xuống.

### 4.2 Audit Columns (Cột kiểm toán)
Mọi bảng dữ liệu nghiệp vụ bắt buộc phải có 4 cột kiểm toán:
- `created_at` (Timestamp)
- `updated_at` (Timestamp)
- `created_by` (Lưu User ID)
- `updated_by` (Lưu User ID)

### 4.3 Soft Delete Strategy (Xóa mềm)
- Tuyệt đối cấm xóa vật lý (Hard delete) các dữ liệu sinh ra dòng tiền hoặc lịch sử thao tác (Ví dụ: Đơn hàng, Giao dịch, User).
- Sử dụng chiến lược Soft Delete: Thêm cột `deleted_at` (Timestamp, mặc định NULL). Khi cần xóa, cập nhật thời gian vào cột này. Dữ liệu chưa xóa là dữ liệu có `deleted_at IS NULL`.

## 5. NORMALIZATION & DENORMALIZATION (CHUẨN HÓA & PHẢN CHUẨN HÓA)
- **Chuẩn hóa (Normalization):** Đưa DB về Chuẩn 3 (3NF) để đảm bảo không trùng lặp dữ liệu, không có các cột tính toán (Calculated fields).
- **Phản chuẩn hóa (Denormalization):** Chỉ áp dụng khi truy vấn quá chậm do phải `JOIN` quá nhiều bảng lớn. Chấp nhận lưu lại dư thừa dữ liệu (VD: Lưu thêm cột `total_orders` vào bảng `users`) nhưng phải có Trigger/Job đồng bộ để giữ tính chính xác.

## 6. INDEX & PERFORMANCE (CHỈ MỤC & HIỆU NĂNG)
- Đánh Index trên mọi cột thường xuyên được dùng trong mệnh đề `WHERE`, `JOIN`, `ORDER BY`.
- Cấm lạm dụng Index: Đánh Index bừa bãi làm chậm tốc độ `INSERT`/`UPDATE` một cách nghiêm trọng.
- Tránh dùng hàm trong mệnh đề WHERE (Ví dụ: `WHERE YEAR(created_at) = 2023`) vì nó sẽ vô hiệu hóa Index (Table scan). Thay vào đó dùng khoảng so sánh: `WHERE created_at >= '2023-01-01' AND created_at < '2024-01-01'`.

## 7. MIGRATION & SEEDING STRATEGY
- **Migration:** Không bao giờ tạo/sửa bảng bằng tay (Click chuột trên GUI). Mọi thay đổi cấu trúc DB phải thông qua file Migration (code) để có thể theo dõi version và tự động chạy trên mọi môi trường.
- **Seed Data:** Có file Seed sinh ra các dữ liệu ban đầu bắt buộc (Roles, Permissions, Danh mục hệ thống) và file Fake Data để phục vụ môi trường Dev/Staging.

## 8. SECURITY & BACKUP (BẢO MẬT & SAO LƯU)
- Chặn mọi kết nối từ bên ngoài Internet vào Cổng DB (VD: 3306, 5432). Chỉ cho phép App Server trong cùng Mạng riêng ảo (VPC) được phép kết nối.
- Thiết lập kịch bản Backup DB tự động mỗi ngày (Cronjob) và lưu trữ bản Backup lên Cloud (S3) để phòng ngừa rủi ro mất dữ liệu/Ransomware.

---

## 9. DATABASE CHECKLIST
Trước khi Apply file Migration cấu trúc mới vào Database:
- [ ] Tên bảng và tên cột có tuân thủ đúng chuẩn `snake_case` số nhiều không?
- [ ] Các bảng nghiệp vụ quan trọng đã có đủ 4 Audit Columns (`created_at`, `updated_at`, `created_by`, `updated_by`) chưa?
- [ ] Đã thiết lập Khóa ngoại (Foreign Keys) để ràng buộc dữ liệu mồ côi chưa?
- [ ] Cột `deleted_at` đã được thêm vào thay vì áp dụng Hard Delete chưa?
- [ ] Các trường thường dùng để Search/Filter đã được đánh Index đúng cách chưa?
- [ ] Cột kiểu chuỗi giới hạn (như Status/Role) đã dùng kiểu `ENUM` hoặc `TINYINT` thay cho `VARCHAR(255)` dư thừa chưa?
