# ADR-0009: Chốt các chính sách nghiệp vụ còn mở (Q1–Q11)

- **Status**: Accepted
- **Date**: 2026-11-20
- **Quyết định bởi**: Repo owner ủy quyền cho nhóm kiến trúc ("cứ thay đổi theo chính sách hợp lý nhất")
- **Liên quan**: [business-analysis.md §9](../business/business-analysis.md#9-câu-hỏi-mở-tổng-hợp-cần-xác-nhận), [ADR-0003](0003-double-booking-prevention.md)

## Context

Bản phân tích nghiệp vụ đã để lại 11 câu hỏi **[CẦN XÁC NHẬN]**. Một số câu hỏi đang chặn việc sửa lỗi thanh toán ở P1. Ví dụ: hệ thống tạo Stripe Checkout bằng `vnd`, trong khi dữ liệu seed lại ghi giá theo thang USD (450, 720…). Kết quả là một villa "450" bị thu **450 VND**. Repo owner đã ủy quyền cho nhóm tự chốt theo hướng hợp lý nhất.

## Problem

Cần một bộ quy tắc nhất quán, có căn cứ (pháp lý, thông lệ ngành, đặc thù một villa resort), để code và tài liệu cùng tuân theo. Mỗi quy tắc phải có thể đổi lại về sau mà không làm hỏng dữ liệu.

## Options Considered → Decision

Nguyên tắc chung khi chọn:
1. Ưu tiên phương án **không làm mất tiền hoặc dấu vết tiền**.
2. Phương án **không giải phóng một villa có thể đang có khách**.
3. Phương án **đúng quy định Việt Nam**.
4. Phương án **ít thay đổi hành vi hiện có nhất**.

| # | Câu hỏi | Các phương án | **Quyết định** | Lý do |
|---|---|---|---|---|
| Q1 | Day-use | Bỏ day-use / chiếm theo khung giờ / chiếm trọn ngày | **Giữ day-use, chiếm trọn ngày `[d, d+1)`**. Day-use không bị ràng buộc bởi min-stay. | Day-use là tính năng đã có (`BUSINESS_ANALYSIS.md:81`). Bán theo giờ cần `tsrange` và vận hành buồng phòng, quá lớn cho P1. Chiếm trọn ngày là phương án an toàn nhất. |
| Q2 | No-show | Tự động sau ngày check-in (night audit) / giữ như cũ | **Giữ như cũ**: scheduler chỉ đánh dấu `NO_SHOW` sau ngày check-out (để sổ sách gọn). STAFF có thể đánh dấu thủ công từ ngày sau check-in. Không hoàn tiền (theo BR-R01, hủy dưới 1 ngày được 0%). | Nếu tự giải phóng sớm mà nhân viên quên bấm check-in, villa đang có khách có thể bị bán lần nữa. Thiệt hại đó lớn hơn nhiều so với lợi ích bán lại vài đêm cuối của một villa sang trọng. |
| Q3 | Sửa booking làm đổi giá | Thu thêm hoặc hoàn chênh lệch tự động / chặn | **`PENDING_PAYMENT`**: được sửa ngày/khách, giá tính lại đầy đủ (phụ thu khách, bảo trì, min/max stay). Phiên thanh toán cũ không còn hợp lệ vì số tiền đã đổi. **`PAID`**: chỉ được tự sửa nếu tổng tiền **không đổi**. Nếu đổi thì trả `422 MODIFICATION_REQUIRES_SUPPORT`; khách hủy theo chính sách hoàn tiền rồi đặt lại, hoặc nhờ ADMIN xử lý. | Thu thêm hoặc hoàn tự động cần payment phụ, hóa đơn điều chỉnh và đối soát, chưa tương xứng với quy mô một property. Chặn rõ ràng thì không bao giờ làm lệch sổ sách. |
| Q4 | Thanh toán trùng / trễ / sai số tiền | Tự động hoàn / từ chối / ghi nhận và đối soát | **Luôn ghi nhận khoản tiền đã nhận** (`PAID`) với cờ `review_required` và lý do, đồng thời báo cho ADMIN (thông báo trong app). **Không tự động hoàn tiền.** Webhook không bao giờ trả lỗi chỉ vì lệch số tiền. | Tiền đã vào tài khoản thì phải có bản ghi. Trước đây webhook Stripe ném lỗi, rollback rồi bị retry vô hạn, còn SePay sai số tiền thì không lưu gì. Tự động hoàn tiền dễ hoàn nhầm hai lần. |
| Q5 | Hoàn tiền chuyển khoản | Giả lập thành công (hiện trạng) / quy trình thủ công | **Quy trình thủ công 2 bước**: refund tạo ở trạng thái `PENDING_MANUAL`; ADMIN chuyển khoản rồi xác nhận kèm mã giao dịch ngân hàng thì mới `SUCCEEDED`. Chỉ thanh toán thẻ qua Stripe thật mới hoàn tự động. Khi kiểm tra vượt số tiền đã trả, các refund đang chờ cũng được tính vào. SLA mục tiêu: 5 ngày làm việc. | Hệ thống không được ghi "đã hoàn" khi chưa có tiền nào được chuyển. |
| Q6 | Tiền tệ, phụ thu khách thêm | Hệ thống USD / VND / đa tiền tệ | **Một tiền tệ duy nhất là VND.** Phụ thu cấu hình qua `app.pricing.extra-guest-fee-per-night`, mặc định **625.000 VND** cho mỗi khách từ khách thứ 3 mỗi đêm (bằng 25 USD × 25.000, giữ đúng ý định ban đầu). Giá seed được quy đổi theo tỷ lệ ×25.000. Khi khởi động, hệ thống cảnh báo villa nào có giá dưới 100.000 VND (dấu hiệu dữ liệu còn theo USD). Stripe từ chối tạo phiên nếu tổng tiền dưới 10.000 VND. | Schema `DECIMAL(12,0)` không có phần lẻ, nên không biểu diễn được cent của USD. SePay/VietQR chỉ nhận VND, hóa đơn VAT tại Việt Nam cũng bằng VND. |
| Q7 | Phí dịch vụ/VAT tính trên giá nào | Trước giảm giá (hiện trạng) / sau giảm giá | **Phí dịch vụ 8% tính trên `subtotal − discount`. VAT 10% tính trên `(subtotal − discount) + phí dịch vụ`.** | Theo Thông tư 219/2013/TT-BTC Điều 7: giảm giá ghi trên hóa đơn thì giá tính thuế là giá đã giảm. Theo thông lệ "++" của ngành khách sạn Việt Nam, phí phục vụ là doanh thu chịu VAT. |
| Q8 | Housekeeping | — | Để sang P4. Role Housekeeper tạm gộp vào STAFF. | Ngoài phạm vi P1. |
| Q9 | Lưu trữ dữ liệu | — | **Audit log**: hành động thanh toán/hoàn tiền (`PAYMENT_*`) lưu **10 năm**, các hành động khác lưu 180 ngày. **Thông tin ngân hàng hoàn tiền** trên booking bị xóa sau **180 ngày** kể từ khi hủy, nếu booking không còn refund nào đang chờ. | Luật Kế toán 2015 Điều 41 yêu cầu lưu tài liệu kế toán ≥ 10 năm. Nghị định 13/2023/NĐ-CP yêu cầu tối thiểu hóa dữ liệu cá nhân. |
| Q10 | SLA | — | 99.5%/tháng; RPO ≤ 24h; RTO ≤ 4h (mục tiêu P5). | Phù hợp với một VPS đơn kèm backup hằng ngày (ADR-0007). |
| Q11 | Min/max stay | Không áp dụng / áp dụng | **Áp dụng cho booking qua đêm**: số đêm phải nằm trong `[minimum_stay, maximum_stay]` của villa (mặc định 1–30). Day-use được miễn. | Cột đã có sẵn và admin đã nhập được, nhưng trước đây không hề được kiểm tra. |

## Consequences

- Code: tính giá được gom về `PricingPolicy`; thêm migration V14 (cờ đối soát payment và refund thủ công); thêm luồng xác nhận refund; tái sử dụng phiên Stripe; giải phóng lượt dùng promotion; viết lại phần kiểm tra của `modifyBooking`.
- Tổng tiền của các booking **mới** thay đổi nhẹ theo Q7. Booking cũ giữ nguyên số tiền đã chốt.
- Môi trường đang dùng giá theo USD phải quy đổi giá villa sang VND (xem cảnh báo khi khởi động và [deployment notes](../roadmap/implementation-plan.md)).
- Frontend phải hiển thị số tiền do server tính (`pricing-preview` có thêm tham số mã giảm giá), không tự tính lại.

## Trade-offs

- Chặn sửa booking `PAID` khi đổi giá làm giảm tiện lợi cho khách. Đổi lại là sổ sách không bao giờ lệch.
- Refund thủ công tốn thời gian của ADMIN. Đổi lại, hệ thống không bao giờ ghi nhận sai rằng đã hoàn tiền.
- Mức 625.000 VND chỉ suy ra từ con số cũ. Có thể đổi bằng cấu hình mà không cần sửa code.
- Mỗi quyết định trên có thể đảo lại bằng một ADR mới. Dữ liệu cũ không bị sửa tự động; trường hợp duy nhất là giá seed, vốn được ghi đè khi ứng dụng chạy với profile `seed`.
