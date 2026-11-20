# Database Design — Nhu Villas (PostgreSQL 16)

| Thuộc tính | Giá trị |
|---|---|
| Ngày | 2026-11-20 |
| Engine mục tiêu | PostgreSQL 16 (duy nhất) — [ADR-0002](../adr/0002-single-flyway-postgresql-track.md) |
| Migration | Flyway, `backend/src/main/resources/db/postgresql`, forward-only |
| Chống double-booking | [ADR-0003](../adr/0003-double-booking-prevention.md) |

---

## 1. Nguyên tắc

1. **Một track migration PostgreSQL**; H2/SQL Server chỉ còn là lịch sử (archive).
2. **Không sửa migration đã deploy**; mọi thay đổi là migration mới `V<n>__<mô_tả>.sql` (`docs/BOOKING_LIFECYCLE_AND_RBAC.md:154-156`).
3. **Không tự sửa dữ liệu production trong migration**. Migration thêm ràng buộc phải có **preflight DO-block** dừng với danh sách bản ghi vi phạm; dữ liệu được xử lý thủ công, có biên bản.
4. Mỗi migration có **rollback script** tương ứng tại `backend/src/main/resources/db/rollback/U<n>__<mô_tả>.sql` (ngoài classpath Flyway location; Flyway Community không có undo), chạy tay và xóa dòng `flyway_schema_history` tương ứng.
5. Thay đổi phá vỡ (đổi tên/đổi kiểu cột) theo **expand → migrate → contract** qua ≥ 2 release.
6. Invariant nghiệp vụ quan trọng có **ràng buộc DB** (CHECK, UNIQUE, EXCLUDE, FK), không chỉ dựa vào code.

---

## 2. Schema hiện tại

Sau `V12__align_entity_schema.sql`. Có 25 bảng. Khóa chính `BIGSERIAL` trừ khi ghi chú khác. Tiền tệ `DECIMAL(12,0)` (VND).

| # | Bảng | Ràng buộc chính | Thiếu sót |
|---|---|---|---|
| 1 | `users` | `username` UNIQUE; `uq_users_email_normalized` (V9); CHECK role (V6) | `date_of_birth VARCHAR`; không `updated_at` |
| 2 | `room_types` | `name` UNIQUE | không audit fields |
| 3 | `amenities` | `name` UNIQUE | không audit fields |
| 4 | `rooms` | `room_number` UNIQUE; FK `room_type_id` | `slug` không UNIQUE; `version` nullable; `check_in_time VARCHAR` |
| 5 | `room_gallery_images` | FK `room_id` | không PK, không index `room_id` |
| 6 | `room_amenities` | PK (room_id, amenity_id) | — |
| 7 | `vouchers` | `code` UNIQUE | không CHECK amount/quantity ≥ 0; không `@Version` |
| 8 | `promotions` | index active/date/category | `promo_code` không UNIQUE; không CHECK start ≤ end |
| 9 | `promotion_rooms` | PK (promotion_id, room_id) | không index `room_id` |
| 10 | `bookings` | `version` NOT NULL; FK user/room; CHECK status (V6); 9 index | **không EXCLUDE overlap**; không CHECK ngày, giá, số khách; `approved_by` không FK; `applied_voucher_code` chứa cả voucher code lẫn promo code; refund bank fields plaintext |
| 11 | `booking_status_transitions` | FK booking/user | không CHECK status |
| 12 | `reviews` | FK user/room/booking | không CHECK rating 1–5; không UNIQUE per booking |
| 13 | `review_photos` | FK review | không PK, không index |
| 14 | `wishlists` | UNIQUE (user_id, room_id) | — |
| 15 | `payments` | `transaction_ref` UNIQUE; `intent_id` UNIQUE; `version`; `review_required`/`review_reason` + partial index (V14) | status/method/gateway không CHECK |
| 16 | `payment_refunds` | UNIQUE (payment_id, idempotency_key); CHECK status (thêm `PENDING_MANUAL` ở V14), amount > 0; `bank_reference`, FK `confirmed_by`, `confirmed_at`, CHECK `confirmed_at IS NULL OR bank_reference IS NOT NULL` (V14) | — |
| 17 | `notifications` | FK user; index (user_id, is_read) | — |
| 18 | `notification_outbox` | CHECK status, attempts ≥ 0; `version` | — |
| 19 | `audit_logs` | index entity | `user_id` không FK (chủ đích, append-only) |
| 20 | `faq_items` | GIN trigram (V4) | — |
| 21 | `blogs` | — | không slug, không `updated_at` |
| 22 | `chat_messages` | FK user | không retention |
| 23 | `contact_messages` | FK assigned_to | không CHECK status/priority |
| 24 | `villa_maintenances` | CHECK end > start | status không CHECK; overlap chỉ kiểm ở code |
| 25 | `villa_inspections` | FK room (CASCADE) | `SERIAL/INT` + `TIMESTAMPTZ` lệch chuẩn |

Drift giữa entity và schema: 23 entity dùng `Integer id`, schema dùng `BIGINT`. Chỉ `PaymentRefund`, `NotificationOutbox` và `AuditLog` dùng `Long`. `PostgresMigrationIT` (P0, PostgreSQL 16.6) xác nhận `ddl-auto=validate` **chấp nhận** drift này; vẫn là nợ kỹ thuật (TD-19) cần chuẩn hóa ở P2.

---

## 3. V13 — Booking integrity (P0)

### 3.1 Mục tiêu

- Đặt lưới an toàn ở DB cho invariant BR-B03: một villa không có hai booking hiệu lực chồng lịch.
- Day-use (`check_in_date = check_out_date`, BR-B09) **được giữ** và chiếm trọn ngày `[d, d+1)`. Đây là cách khớp với `booking/model/Occupancy.java` trên nhánh `feature/p0-hardening`. Q1 đã chốt trong [ADR-0009](../adr/0009-business-policy-decisions.md) (giữ day-use, chiếm trọn ngày). Nếu sau này bỏ day-use, chỉ cần đổi CHECK thành `>` ở migration mới.
- Status "không chiếm chỗ" gồm `CANCELLED`, `EXPIRED`, `NO_SHOW`. Danh sách này phải đồng bộ với `BookingRepository` và `BookingStatus`.

### 3.2 Script: `db/postgresql/V13__booking_integrity.sql` (thiết kế)

```sql
-- btree_gist là "trusted extension" từ PG13: chủ sở hữu DB tạo được, không cần superuser.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Preflight: dừng migration nếu dữ liệu hiện có vi phạm. KHÔNG tự sửa dữ liệu.
DO $$
DECLARE
    v_bad text;
BEGIN
    SELECT string_agg(id::text, ', ' ORDER BY id)
      INTO v_bad
      FROM bookings
     WHERE check_out_date < check_in_date;
    IF v_bad IS NOT NULL THEN
        RAISE EXCEPTION 'V13 preflight: bookings với check_out_date < check_in_date: %', v_bad;
    END IF;

    SELECT string_agg(a.id || '<->' || b.id, ', ')
      INTO v_bad
      FROM bookings a
      JOIN bookings b
        ON a.room_id = b.room_id
       AND a.id < b.id
       AND a.status NOT IN ('CANCELLED', 'EXPIRED', 'NO_SHOW')
       AND b.status NOT IN ('CANCELLED', 'EXPIRED', 'NO_SHOW')
       AND daterange(a.check_in_date, GREATEST(a.check_out_date, a.check_in_date + 1), '[)')
        && daterange(b.check_in_date, GREATEST(b.check_out_date, b.check_in_date + 1), '[)');
    IF v_bad IS NOT NULL THEN
        RAISE EXCEPTION 'V13 preflight: booking chồng lịch cần xử lý thủ công: %', v_bad;
    END IF;
END $$;

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_date_order
    CHECK (check_out_date >= check_in_date);

ALTER TABLE bookings
    ADD CONSTRAINT bookings_no_overlap
    EXCLUDE USING gist (
        room_id WITH =,
        daterange(check_in_date, GREATEST(check_out_date, check_in_date + 1), '[)') WITH &&
    )
    WHERE (status NOT IN ('CANCELLED', 'EXPIRED', 'NO_SHOW'));
```

Ghi chú thiết kế:

| Điểm | Giải thích |
|---|---|
| `'[)'` half-open | Khách A trả phòng 12/12 và khách B nhận phòng 12/12 không xung đột, khớp query JPQL hiện có |
| `GREATEST(..., check_in_date + 1)` | Day-use `[d, d]` sẽ là range rỗng và không bao giờ `&&` với range khác (lỗi gốc của P0-02). Biểu thức này biến nó thành `[d, d+1)`. `date + integer` và `GREATEST` là immutable nên dùng được trong index |
| `WHERE (status NOT IN ...)` | Partial exclusion: booking đã hủy/hết hạn/no-show không chiếm chỗ. Chuyển một booking từ `CANCELLED` về trạng thái hiệu lực cũng bị kiểm tra |
| Tương tác với pessimistic lock | App vẫn lock row `rooms` trước. Constraint chỉ là lưới cuối: nếu hai transaction lọt qua, transaction commit sau nhận `23P01 exclusion_violation` |
| Map lỗi | `GlobalExceptionHandler` map SQLState `23P01` thành 409 `ROOM_UNAVAILABLE` (cùng mã với service-level check) |
| Chi phí | GiST index trên `bookings` (quy mô hàng nghìn dòng mỗi năm) không đáng kể. Lock khi `ADD CONSTRAINT` là `ACCESS EXCLUSIVE` trong thời gian build index, chỉ vài giây ở quy mô hiện tại. Nên deploy ngoài giờ cao điểm |
| `database/init-db.sql` | Thêm `CREATE EXTENSION IF NOT EXISTS btree_gist;` để môi trường mới khởi tạo giống prod |

### 3.3 Rollback: `db/rollback/U13__booking_integrity.sql`

```sql
-- Chạy thủ công khi cần quay lui V13. Không làm mất dữ liệu.
BEGIN;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_no_overlap;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS chk_bookings_date_order;
-- Giữ extension btree_gist (vô hại, có thể đang được dùng bởi object khác).
DELETE FROM flyway_schema_history WHERE version = '13';
COMMIT;
```

Sau rollback phải deploy lại bản ứng dụng trước V13, hoặc giữ bản mới. Code hiện tại không phụ thuộc vào sự tồn tại của constraint.

### 3.4 Kiểm thử

| Test | Kỳ vọng |
|---|---|
| `PostgresMigrationIT` | V1 → V13 chạy trên PostgreSQL 16 sạch; Hibernate `validate` pass |
| `BookingOverlapConstraintIT` | INSERT trực tiếp một booking `PAID` chồng lịch bị từ chối với SQLSTATE `23P01`. Day-use trùng ngày với day-use khác bị từ chối. Booking chồng lịch với một booking `CANCELLED` được chấp nhận |
| `BookingConcurrencyPostgresIT` | 20 luồng đặt cùng villa và cùng ngày: đúng 1 thành công |
| Preflight | Seed sẵn 2 booking chồng lịch: migration fail với thông báo liệt kê id |

---

## 3b. V14 — Payment review & manual refunds (P1)

File: `backend/src/main/resources/db/postgresql/V14__payment_review_and_manual_refunds.sql`. Quyết định nghiệp vụ: [ADR-0009](../adr/0009-business-policy-decisions.md) Q4 (ghi nhận + đối soát tiền trễ/trùng/sai số tiền) và Q5 (hoàn tiền chuyển khoản thủ công 2 bước). **Chỉ thêm (additive)**, không sửa dòng dữ liệu hiện có.

| Bảng | Thay đổi | Ghi chú |
|---|---|---|
| `payments` | `review_required BOOLEAN NOT NULL DEFAULT FALSE`; `review_reason VARCHAR(500)` | Entity `Payment.reviewRequired/reviewReason`, `Payment.flagForReview(reason)` |
| `payments` | `idx_payments_review_required ON payments (created_at DESC) WHERE review_required` | Partial index cho hàng đợi `GET /payments?reviewRequired=true` |
| `payment_refunds` | CHECK `chk_payment_refund_status` thêm `PENDING_MANUAL` (`PROCESSING, PENDING_MANUAL, SUCCEEDED, FAILED`) | Thiết kế ban đầu ghi `MANUAL_PENDING`; tên thực tế là `PENDING_MANUAL` |
| `payment_refunds` | `bank_reference VARCHAR(128)`; `confirmed_by BIGINT` + FK `fk_payment_refunds_confirmed_by → users(id)`; `confirmed_at TIMESTAMP` | Entity `PaymentRefund.bankReference/confirmedBy/confirmedAt` |
| `payment_refunds` | CHECK `chk_payment_refund_manual_reference`: `confirmed_at IS NULL OR bank_reference IS NOT NULL` | Refund thủ công đã xác nhận bắt buộc có mã giao dịch ngân hàng |

Vòng đời refund:

| Từ | Đến | Kích hoạt | Ảnh hưởng tổng tiền |
|---|---|---|---|
| — | `PROCESSING` | ADMIN refund payment thẻ Stripe thật (`PaymentGateway.supportsAutomaticRefund = true`) | Chưa |
| `PROCESSING` | `SUCCEEDED` / `FAILED` | Stripe trả kết quả | `SUCCEEDED`: cộng `payments.refund_amount`, đồng bộ `bookings.refund_amount` |
| — | `PENDING_MANUAL` | ADMIN refund payment SePay/chuyển khoản/manual/mock | Chưa; số tiền đã được "giữ chỗ" khi kiểm tra over-refund |
| `PENDING_MANUAL` | `SUCCEEDED` | `POST /payments/{id}/refunds/{refundId}/confirm {bankReference}` | Cộng tổng hoàn; hoàn 100% booking `PAID` → `CANCELLED` |
| `PENDING_MANUAL` | `FAILED` | `POST /payments/{id}/refunds/{refundId}/reject` | Không |

### 3b.1 Rollback: `db/rollback/U14__payment_review_and_manual_refunds.sql`

- **Guard**: DO-block `RAISE EXCEPTION` nếu còn refund `PENDING_MANUAL` (CHECK cũ không chứa trạng thái này). Phải xác nhận hoặc hủy các yêu cầu đó trước.
- Xóa CHECK `chk_payment_refund_manual_reference`, FK `fk_payment_refunds_confirmed_by`, các cột `confirmed_at`, `confirmed_by`, `bank_reference`; khôi phục CHECK status 3 giá trị; xóa index và cột `review_*` của `payments`; xóa dòng `flyway_schema_history` version `14`.
- **Lưu ý**: rollback làm **mất** dữ liệu `bank_reference`/`confirmed_*` và cờ review. Backup trước khi chạy.

### 3b.2 Kiểm chứng (PostgreSQL 16.6, 2026-11-20)

| Kiểm thử | Kết quả |
|---|---|
| `PostgresMigrationIT` (V1→V14 + Hibernate `validate`) | Pass; `manualRefundWorkflowSchemaExists` xác nhận CHECK có `PENDING_MANUAL` và cột `review_required` |
| `ManualRefundIT` | Refund chuyển khoản → `PENDING_MANUAL` (tổng không đổi) → job retention giữ bank details → xác nhận `VCB-IT-001` → `REFUNDED`, refund `SUCCEEDED` → job xóa bank details |
| Chạy tay | Chèn refund `PENDING_MANUAL` → U14 dừng với `U14 aborted: PENDING_MANUAL refunds exist…`; chuyển sang `FAILED` → U14 rollback thành công (cột `review_required` biến mất) → chạy lại V14 thành công |

---

## 4. Thay đổi schema mục tiêu (P2 và sau đó)

Thứ tự đề xuất. Số version thực tế cấp phát khi triển khai. **Đánh số lại 2026-11-20**: V14 đã được dùng cho payment review/manual refunds (P1), nên các migration kế hoạch dịch lên một số.

> **Trạng thái 2026-11-20 (nhánh `feature/p2-database`, [ADR-0010](../adr/0010-p2-schema-hardening-scope.md))**: **V15–V20 đã triển khai** kèm rollback `db/rollback/U15..U20` và kiểm chứng trên PostgreSQL 16.6 (`SchemaIntegrityIT` 5 test, `PostgresMigrationIT` latest = 20; U20→U15 rồi áp lại; preflight V15/V17 dừng đúng khi có dữ liệu vi phạm). Khác biệt so với bảng dưới:
> - **V18**: chỉ đổi khóa `villa_inspections` sang `BIGINT`; **không** đổi timestamp và **không** đổi `Integer → Long` ở Java (đã hoãn có lý do, ADR-0010).
> - **V19**: không dùng Spring Data JPA Auditing mà dùng `common/audit/AuditStampListener` + `AuditStamped` (id người thao tác do `SessionAuthBridgeFilter` gắn vào `Authentication.details`). Việc đổi tên `bookings.modified_at → updated_at` để sau.
> - **V17**: thêm CHECK cho `rooms` (giá ≥ 0, `minimum_stay ≥ 1`, `maximum_stay ≥ minimum_stay`, `capacity > 0`), `bookings.refund_percentage` 0..100, `vouchers.amount ≤ 100` khi là phần trăm; UNIQUE promo code dùng `UPPER(promo_code)`.
> - Track migration H2 (`db/migration`), `db/postgresql-legacy` và các script SQL Server đã bị xóa; profile `postgres` chạy Flyway + `validate` như prod; `dev`/`test` (H2) tắt Flyway.

| Migration | Nội dung | Loại | Preflight / rủi ro | Rollback |
|---|---|---|---|---|
| V15 `rooms_integrity` | `rooms.slug` UNIQUE (preflight trùng slug); `rooms.version` `SET DEFAULT 0` + backfill + `NOT NULL`; index `room_gallery_images(room_id)`, `review_photos(review_id)`, `promotion_rooms(room_id)`; port index V2/V3 còn thiếu (`payments(intent_id)` nếu chưa có, `notifications(user_id, created_at)`) | Additive | Trùng slug → dừng | Drop constraint/index |
| V16 `booking_discount_refs` | Expand: thêm `bookings.voucher_id` FK `vouchers` và `promotion_id` FK `promotions`, backfill từ `applied_voucher_code`, giữ cột cũ. Thêm FK `bookings.approved_by → users(id)` (preflight id mồ côi) | Expand | Không FK thẳng `applied_voucher_code → vouchers.code` được, vì cột chứa cả promo code (`BookingService.java:275,305`) | Drop cột/FK mới |
| V17 `domain_checks` | CHECK: `reviews.rating BETWEEN 1 AND 5`; amount/price ≥ 0 trên bookings/payments/vouchers/promotions; `bookings.guests > 0`; `promotions.start_date <= end_date`; CHECK enum cho `payments.status/method/gateway`, `booking_status_transitions.*_status`, `contact_messages.status/priority`, `villa_maintenances.status`, `villa_inspections.*`; UNIQUE `reviews(booking_id)` (preflight trùng); UNIQUE `promotions(promo_code)` WHERE NOT NULL | Additive | Dữ liệu cũ vi phạm → dừng | Drop constraint |
| V18 `inspection_id_alignment` | `villa_inspections.id/room_id/inspector_id` đổi `INT → BIGINT`; `TIMESTAMPTZ → TIMESTAMP` (hoặc chuẩn hóa toàn hệ thống về `TIMESTAMPTZ`; quyết định ở ADR riêng). Code: mọi entity `Integer id → Long` (không đổi DB vì đã `BIGINT`) | Type change | Rewrite bảng nhỏ | `ALTER TYPE` ngược |
| V19 `audit_columns` | Chuẩn audit: `created_at NOT NULL DEFAULT now()`, `updated_at`, `created_by`, `updated_by` (FK `users`, nullable) cho bảng nghiệp vụ do staff quản lý (rooms, room_types, amenities, vouchers, promotions, villa_maintenances, villa_inspections, blogs, faq_items). `bookings.modified_at → updated_at` theo expand/contract. Bật Spring Data JPA Auditing | Expand | — | Drop cột mới |
| V20 `properties` | Bảng `properties(id, code UNIQUE, name, timezone, check_in_time, check_out_time, currency, ...)`, seed 1 dòng; `rooms.property_id BIGINT NOT NULL DEFAULT 1 REFERENCES properties`; `vouchers.property_id`, `promotions.property_id` nullable (NULL = toàn hệ thống) | Additive | — | Drop cột/bảng |
| V21 `payment_hardening` | `payment_webhook_events(provider, event_id) UNIQUE` để dedupe ở tầng event. ~~`reconciliation_items`~~ và ~~`MANUAL_PENDING`~~ đã thay bằng cờ `payments.review_required` và trạng thái `PENDING_MANUAL` ở V14. **Không** thêm partial UNIQUE 
| V22 `booking_idempotency` (tùy chọn) | `booking_idempotency(user_id, idempotency_key) UNIQUE, request_hash, booking_id, created_at`, retention 24h. P1 đã có natural idempotency (cùng khách/villa/ngày/số khách khi hold còn hiệu lực) — chỉ làm khi client cần retry với payload khác | Additive | — | Drop |
| V23 `shedlock` | Bảng `shedlock` cho scheduler | Additive | — | Drop |
| V24 `refund_bank_encryption` | Expand: `refund_account_number_enc BYTEA`, mã hóa ở tầng ứng dụng (AES-GCM, key từ env/KMS), backfill bằng job, rồi contract xóa plaintext ở release sau | Expand/Contract | Quản lý key | Giữ cột cũ đến contract |
| V25 `housekeeping` (P4) | `room_status(room_id PK, status, updated_at, version)`, `housekeeping_tasks(id, room_id, booking_id, type, status, assignee_id, due_at, started_at, completed_at, inspected_by, version, ...)`, CHECK status, index `(status, due_at)`, `(assignee_id, status)` | Additive | BA xác nhận (Q8) | Drop |
| Dọn dẹp | Archive `db/migration` (H2), `db/postgresql-legacy`, `database/migration.sql`, `database/update_constraint.sql`, `backend/*.sql` vào `database/archive/` kèm README. Chuyển H2 sang scope `test` hoặc bỏ hẳn | Repo | Profile `dev` chuyển sang PostgreSQL qua compose | — |

---

## 5. Index strategy

| Truy vấn nóng | Index hiện có | Bổ sung |
|---|---|---|
| Conflict check theo villa/ngày | `idx_bookings_room_dates (room_id, check_in_date, check_out_date)` | GiST của `bookings_no_overlap` (V13) phục vụ luôn truy vấn `&&` nếu chuyển sang dùng `daterange` |
| Scheduler hết hold | `idx_bookings_status_hold_expires` (V5) | — |
| Scheduler no-show / auto-complete | `idx_bookings_dates_status` | Đổi query sang `WHERE status = ? AND check_out_date < ?` thay vì `findAll` |
| Booking của user | `idx_bookings_user` | `(user_id, created_at DESC)` cho phân trang |
| Payment theo booking | `idx_payments_booking_status` | — |
| Outbox dispatch | `(status, available_at, created_at)` (V8) | — |
| Audit tra cứu | `idx_audit_entity`, `idx_audit_created` | — |
| Housekeeping board | — | `(status, due_at)`, `(assignee_id, status)` (V25) |
| Hàng đợi đối soát payment | `idx_payments_review_required` (partial, V14) | — |
| Hàng đợi refund thủ công | `idx_payment_refunds_status` (V7) | — |

Quy tắc: mọi FK có index; index mới tạo bằng `CREATE INDEX CONCURRENTLY` trong migration riêng (Flyway: `-- flyway:executeInTransaction=false`) khi bảng lớn hơn 1 triệu dòng. Hiện chưa cần.

## 6. Transaction & locking strategy

| Use-case | Isolation | Cơ chế | Lý do |
|---|---|---|---|
| Tạo / sửa booking | READ COMMITTED | `SELECT … FOR UPDATE` row `rooms` + conflict query `FOR UPDATE` + EXCLUDE | Tuần tự hóa theo villa; contention thấp ở một property |
| Thanh toán (webhook) | READ COMMITTED | `FOR UPDATE` payment, rồi booking (thứ tự cố định payment → booking để tránh deadlock) | Idempotent, chống race giữa webhook và scheduler |
| Expire hold | READ COMMITTED | Optimistic `@Version` trên booking | Nếu payment vừa đến thì scheduler thua và retry lần sau |
| Refund | READ COMMITTED | `FOR UPDATE` payment + UNIQUE idempotency key | Không hoàn trùng |
| Voucher / promotion | READ COMMITTED | `UPDATE … WHERE quantity > 0` / `current_uses < maximum_uses` (atomic conditional update) | Không cần lock dài |
| Housekeeping task | READ COMMITTED | `@Version` | Xung đột hiếm, UI báo cho người dùng thử lại |
| Scheduler đa instance | — | ShedLock | Một job chỉ chạy trên một instance |

## 7. Audit fields & soft delete

| Bảng | Audit fields | Soft delete? | Lý do |
|---|---|---|---|
| `bookings`, `payments`, `payment_refunds` | created/updated + transitions/audit log | **Không**, không bao giờ xóa | Chứng từ tài chính |
| `users` | created/updated | `disabled` + ẩn danh hóa khi user yêu cầu xóa (`delete_requested`) | Giữ FK lịch sử, tôn trọng quyền xóa dữ liệu |
| `rooms`, `room_types`, `amenities` | created/updated/by | Lưu trữ (`is_available=false` / `archived_at`) | Booking cũ tham chiếu |
| `promotions`, `vouchers` | created/updated/by | `is_active=false` | Booking cũ tham chiếu |
| `reviews` | created/updated | `is_hidden` | Kiểm duyệt |
| `notifications`, `chat_messages`, `contact_messages` | created | Xóa cứng theo retention | Không có giá trị lịch sử |
| `audit_logs`, `booking_status_transitions` | created | Không, append-only | Truy vết |

Không áp dụng `deleted_at` chung cho mọi bảng (theo yêu cầu "Soft delete chỉ khi nghiệp vụ cần").

## 8. Data retention

| Dữ liệu | Đề xuất | Căn cứ / ghi chú |
|---|---|---|
| Booking, payment, refund, hóa đơn | ≥ 10 năm, không xóa | ✔ ADR-0009 Q9 — Luật Kế toán 2015 Điều 41. Không có job xóa các bảng này |
| `audit_logs` | Hành động thường: **180 ngày**; hành động tài chính `PAYMENT_*` (`PAYMENT_CREATE`, `PAYMENT_CONFIRM`, `PAYMENT_FAIL`, `PAYMENT_REFUND`, `PAYMENT_REFUND_CONFIRM`, `PAYMENT_REFUND_REJECT`, `PAYMENT_WEBHOOK_RECEIVED`): **10 năm** | ✔ ADR-0009 Q9. `AuditLogService.purgeExpiredLogs` (`AuditLogRepository.deleteOperationalBefore` / `deleteFinancialBefore`), job `BookingScheduler.cleanupOldAuditLogs` 00:00 hằng ngày |
| `notification_outbox` đã publish | Payload xóa ngay sau publish; metadata purge theo cấu hình | `BOOKING_LIFECYCLE_AND_RBAC.md:73-75` |
| Thông tin ngân hàng hoàn tiền (`bookings.refund_bank_name/refund_account_number/refund_account_name`) | Xóa (set NULL) **180 ngày** sau `cancelled_at`, **trừ khi** booking còn refund `PENDING_MANUAL` | ✔ ADR-0009 Q9 — Nghị định 13/2023/NĐ-CP (tối thiểu hóa dữ liệu). `BookingRepository.purgeRefundBankDetails`, job `BookingScheduler.purgeRefundBankDetails` 00:30 hằng ngày; kiểm chứng `ManualRefundIT`. Mã hóa khi lưu vẫn là kế hoạch (V24) |
| `chat_messages` (AI) | 90 ngày | Đề xuất |
| `contact_messages` | 2 năm | Đề xuất |
| Backup | Hằng ngày giữ 7, hằng tuần giữ 4, hằng tháng giữ 12 | [ADR-0007](../adr/0007-docker-compose-vps-deployment.md) |

## 9. Quy trình release thay đổi DB

1. Viết migration và rollback script, kèm preflight nếu có ràng buộc mới.
2. `PostgresMigrationIT` xanh trong CI (Testcontainers PostgreSQL 16).
3. Chạy preflight trên bản restore mới nhất của production ở staging.
4. Backup ngay trước deploy (`pg_dump -Fc`) và ghi lại tên file.
5. Deploy. Flyway chạy khi ứng dụng khởi động (`spring.flyway.enabled=true`, `validate-on-migrate=true`).
6. Smoke test. Nếu lỗi: chạy rollback script, hoặc restore từ backup ở bước 4 nếu migration có biến đổi dữ liệu.
