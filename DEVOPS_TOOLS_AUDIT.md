# DEVOPS_TOOLS_AUDIT

## Tính năng

| Tính năng | Hoàn thành | Thiếu gì | Cần làm để production |
|---|---:|---|---|
| Docker/start scripts | 60% | Config hiện có nhưng nhiều generated `.next` dirty trong repo. | Ignore/clean generated artifacts, CI clean checkout. |
| Tools scripts | 50% | Nhiều script migration/fix tạm thời, không có tests. | Archive hoặc document purpose, remove stale scripts. |
| Env examples | 65% | Có env examples, nhưng placeholder secret/test password nằm nhiều nơi. | Secret scanning CI, no real/test credentials in prod docs. |

## Bugs

### DEVOPS-001 - Medium - Generated Next build artifacts tracked/dirty
- Mô tả: `git status` cho thấy nhiều `frontend/.next/**` modified/deleted/untracked.
- Nguyên nhân: Build output không được loại khỏi working tree hiện tại.
- File/Class/Method: `frontend/.next/**`; `.gitignore`.
- Cách tái hiện: `git status --short`.
- Ảnh hưởng: Review nhiễu, dễ commit artifact, CI khó tái lập.
- Hướng khắc phục: Add `.next/` vào ignore nếu chưa có, remove tracked artifacts bằng git rm cached.

### DEVOPS-002 - Low - Tool scripts là maintenance one-off chưa có guard/test
- Mô tả: `tools/fix_*.py`, `recover_frontend.py`, `migrate_links.py` tồn tại nhưng không có README/test.
- Nguyên nhân: Refactor scripts để lại trong repo.
- File/Class/Method: `tools/fix_client_components.py`, `tools/fix_suspense.py`, `tools/recover_frontend.py`.
- Cách tái hiện: Inspect `tools/`.
- Ảnh hưởng: Dev có thể chạy nhầm script gây thay đổi hàng loạt.
- Hướng khắc phục: Archive scripts hoặc thêm README + dry-run + tests.
