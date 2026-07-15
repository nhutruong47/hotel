# 20. GIT WORKFLOW FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là quy chuẩn quản lý mã nguồn (Source Code Management) dùng chung cho mọi dự án. Mục tiêu của tài liệu này là đảm bảo lịch sử Git luôn sạch sẽ, dễ truy vết (traceable), dễ bảo trì và hỗ trợ tối đa cho quy trình CI/CD.

---

## 1. GIT FLOW STRATEGY (CHIẾN LƯỢC PHÂN NHÁNH)
Dự án áp dụng mô hình **Git Flow** tiêu chuẩn kết hợp với cơ chế Pull Request bắt buộc.
- **`main` / `master`:** Nhánh Production. Chứa code đã được release và đang chạy trực tiếp trên môi trường thật. Bắt buộc khóa Push trực tiếp (Protected Branch).
- **`develop`:** Nhánh tích hợp chính (Integration Branch). Chứa code mới nhất đã qua kiểm thử, chuẩn bị cho đợt Release tiếp theo. Nguồn gốc của mọi nhánh Feature.
- **`feature/*`:** Nhánh phát triển tính năng mới. Tách ra từ `develop` và merge ngược lại vào `develop`.
- **`release/*`:** Nhánh chuẩn bị đóng gói phiên bản mới. Tách ra từ `develop`. Chỉ dùng để sửa lỗi lặt vặt (bug fix), cập nhật version number, tuyệt đối không code tính năng mới ở đây. Merge vào cả `main` và `develop` khi hoàn tất.
- **`hotfix/*`:** Nhánh sửa lỗi khẩn cấp trên Production. Tách ra trực tiếp từ `main`. Sau khi sửa xong, merge ngược lại vào CẢ `main` VÀ `develop`.

## 2. BRANCH NAMING CONVENTION (QUY TẮC ĐẶT TÊN NHÁNH)
Tên nhánh phải viết thường (lowercase), phân cách bằng dấu gạch ngang (`-`).
- Cấu trúc: `<loại-nhánh>/<mã-task>-<mô-tả-ngắn gọn>`
- **Ví dụ:**
  - `feature/TICKET-123-add-login-form`
  - `bugfix/TICKET-456-fix-header-responsive`
  - `hotfix/critical-payment-error`
  - `release/v1.2.0`

## 3. COMMIT MESSAGE CONVENTION (QUY CHUẨN COMMIT)
Dự án áp dụng **Conventional Commits**. Commit message phải tuân thủ cú pháp:
`<type>[optional scope]: <description>`

- **Các Type bắt buộc:**
  - `feat:` Thêm tính năng mới (Feature).
  - `fix:` Sửa lỗi (Bug fix).
  - `docs:` Chỉ cập nhật tài liệu (README, Guide).
  - `style:` Chỉnh sửa format code (Khoảng trắng, dấu phẩy, v.v. Không đổi logic).
  - `refactor:` Viết lại code nhưng không đổi logic, không thêm tính năng, không sửa lỗi.
  - `perf:` Cải thiện hiệu năng.
  - `test:` Thêm hoặc sửa test case.
  - `chore:` Cập nhật thư viện, cấu hình build, công cụ (Không đụng đến source code `src`).
- **Ví dụ:** `feat(auth): implement JWT token persistence in local storage`

## 4. PULL REQUEST CONVENTION (QUY CHUẨN TẠO PULL REQUEST)
- **Tiêu đề PR:** Phải theo chuẩn Conventional Commits (VD: `feat: Add shopping cart UI`).
- **Nội dung PR (Description):**
  - Mục tiêu của PR này là gì? (Tóm tắt hoặc link tới Ticket/Jira).
  - Có Breaking Changes (lỗi tương thích ngược) không?
  - Bắt buộc đính kèm ảnh chụp màn hình (Screenshot) hoặc Video nếu PR có thay đổi về giao diện (UI).
- **Quy mô:** Không bao giờ gộp 2 tính năng hoàn toàn khác nhau vào 1 PR. Chia nhỏ PR để Code Reviewer không bị choáng ngợp (dưới 500 lines of code là lý tưởng).

## 5. MERGE STRATEGY (CHIẾN LƯỢC GỘP NHÁNH)
- Từ `feature` vào `develop`: Sử dụng **Squash and Merge**. Gộp tất cả các commit lắt nhắt (như `fix typo`, `update css`) thành 1 commit duy nhất mang ý nghĩa trọn vẹn để giữ lịch sử `develop` sạch sẽ.
- Từ `release`/`hotfix` vào `main`: Sử dụng **Create a merge commit** để giữ lại dấu vết của đợt release.

## 6. CONFLICT RESOLUTION (GIẢI QUYẾT XUNG ĐỘT)
- Không bao giờ giải quyết Conflict trực tiếp trên giao diện GitHub/GitLab nếu conflict phức tạp.
- Developer sở hữu nhánh `feature` có trách nhiệm chủ động kéo (pull) code mới nhất từ `develop` về nhánh của mình và tự `resolve conflict` dưới máy Local trước khi báo Reviewer duyệt PR.

## 7. TAG & SEMANTIC VERSIONING (ĐÁNH DẤU PHIÊN BẢN)
Tuân thủ **SemVer (Semantic Versioning) - `vX.Y.Z`**:
- **X (Major):** Thay đổi lớn, phá vỡ tính tương thích ngược (Breaking change).
- **Y (Minor):** Thêm tính năng mới, vẫn tương thích ngược.
- **Z (Patch):** Fix lỗi nhỏ, không thêm tính năng mới.
- Sau khi merge nhánh `release` vào `main`, BẮT BUỘC phải tạo một Git Tag (VD: `v1.2.0`) trên commit cuối cùng của `main`.

## 8. GIT IGNORE STRATEGY
File `.gitignore` phải được thiết lập đúng ngay từ giây phút khởi tạo dự án.
- Bỏ qua mọi thư mục sinh ra do biên dịch: `node_modules`, `dist`, `build`, `.next`.
- Bỏ qua các file môi trường nhạy cảm: `.env`, `.env.local`, `.env.production`.
- Bỏ qua các file cấu hình IDE cá nhân: `.idea/`, `.vscode/`, `.DS_Store`.

## 9. RELEASE PROCESS & ROLLBACK (QUY TRÌNH PHÁT HÀNH & KHÔI PHỤC)
- **Release:** 
  1. Tạo nhánh `release/vX.Y.Z` từ `develop`.
  2. QC test cuối cùng.
  3. Merge `release` vào `main`.
  4. Tag version `vX.Y.Z`.
  5. Pipeline tự động deploy `main` lên Production.
- **Rollback (Khôi phục):** Nếu phát hiện lỗi nghiêm trọng trên Production:
  - Chạy pipeline CI/CD để Re-deploy lại Git Tag của phiên bản ổn định trước đó ngay lập tức (Thời gian phục hồi < 1 phút).
  - Song song, tạo nhánh `hotfix` từ `main` để sửa lỗi gốc rễ.

## 10. GIT HOOKS & CI TRIGGER
- Tích hợp **Husky** và **lint-staged** ở máy Local: Tự động chặn lệnh `git commit` nếu code sai chuẩn Eslint/Prettier hoặc sai cấu trúc Commit Message.
- **CI Trigger Rule:** Không chạy Build Pipeline toàn bộ hệ thống nếu chỉ thay đổi file `.md` (Tài liệu). Tối ưu hóa thời gian chạy CI.

---

## 11. CODE REVIEW CHECKLIST (DÀNH CHO REVIEWER)
- [ ] PR có quá lớn để review không? (Nếu > 1000 dòng, yêu cầu chia nhỏ).
- [ ] Tên nhánh và Commit message có tuân thủ đúng chuẩn không?
- [ ] Có console.log(), comment rác, hoặc code thừa chưa xóa không?
- [ ] Code có lặp lại (WET) và cần được cấu trúc lại thành hàm dùng chung (DRY) không?

## 12. GIT BEST PRACTICES CHECKLIST (DÀNH CHO DEVELOPER)
- [ ] Tôi đã commit thường xuyên chưa? (Đừng làm nguyên 1 tuần rồi mới đẩy 1 commit khổng lồ).
- [ ] Tôi có chắc chắn không vô tình commit file `.env` chứa mật khẩu lên nhánh chung không?
- [ ] Trước khi tạo PR, tôi đã tự review lại Diff của mình để bắt các lỗi sơ đẳng chưa?
- [ ] Nhánh của tôi đã đồng bộ (Pull) cập nhật mới nhất từ `develop` chưa?
