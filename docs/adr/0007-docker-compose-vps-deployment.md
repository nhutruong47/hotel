# ADR-0007: Triển khai production bằng Docker Compose trên một VPS

- **Status**: Accepted
- **Date**: 2026-11-20
- **Liên quan**: [target-architecture.md §7](../architecture/target-architecture.md#7-deployment-diagram-vps--docker-compose)

## Context

Hiện tại backend chạy trên Render free tier, giới hạn 512 MB. JVM bị ép `-Xmx220m` với SerialGC (`backend/Dockerfile`). Frontend chạy trên Vercel. PostgreSQL, Redis và RabbitMQ chỉ có trong `docker-compose.yml`, nên production thực tế đang thiếu Redis/RabbitMQ hoặc phải dùng dịch vụ rời rạc.

Thêm vào đó: không có backup, không có monitoring, và nginx không có TLS. Người dùng đã chọn đích deploy là "Docker Compose trên VPS" (2026-11-20).

## Problem

Cần một nền tảng production:
- chạy đủ PostgreSQL, Redis, RabbitMQ, backend, frontend, nginx TLS, monitoring và backup;
- chi phí thấp và đội 5 người vận hành được;
- tách được các môi trường dev / test / staging / prod.

## Options Considered

| Option | Chi phí/tháng (ước lượng) | Nhận xét |
|---|---|---|
| A. Giữ Render + Vercel, thêm managed Postgres/Redis/CloudAMQP | Thấp đến trung bình | Phân tán qua 4–5 nhà cung cấp; giới hạn RAM; khó dựng monitoring và backup thống nhất |
| B. AWS managed (ECS Fargate + RDS + Amazon MQ + ElastiCache + CloudWatch) | Cao (Amazon MQ và ElastiCache tốn nhiều dù tải nhỏ) | Ít phải tự vận hành nhưng thừa so với một property; cần chuyên môn AWS/IaC |
| C. Kubernetes (k3s) | Trung bình | Thêm độ phức tạp mà đội nhỏ chưa cần |
| D. **Docker Compose trên 1 VPS** (4 vCPU / 8 GB), staging trên VPS nhỏ hoặc cùng VPS với project name khác | Thấp | Kế thừa `docker-compose.yml` sẵn có; toàn quyền cấu hình; vận hành bằng SSH; không HA |

## Decision

Chọn **D**.
- File compose tách theo môi trường: `docker-compose.yml` (base) cộng `docker-compose.dev.yml`, `docker-compose.staging.yml`, `docker-compose.prod.yml`.
- **Mạng**: `edge` (nginx), `app`, `data`, `obs`. Ở prod, chỉ nginx publish cổng 80/443. RabbitMQ UI và Grafana chỉ truy cập qua SSH tunnel hoặc nginx có xác thực.
- **TLS**: Let's Encrypt (certbot), HSTS, CSP. Security header nằm trong file `include` dùng chung cho mọi `location`.
- **Image**: GitHub Actions build, tag theo commit SHA, push GHCR. Deploy bằng `ssh` + `docker compose pull && docker compose up -d`. Rollback bằng tag SHA trước đó.
- **Secret**: file `.env` trên VPS (quyền 600, không commit). Compose dùng `${VAR:?required}` và không có default yếu.
- **Backup**:
  - `pg_dump -Fc` hằng ngày, mã hóa bằng age/gpg, đẩy offsite (S3-compatible) bằng rclone.
  - Giữ 7 bản hằng ngày, 4 bản hằng tuần, 12 bản hằng tháng.
  - Mỗi tháng restore drill trên staging. Mục tiêu RPO 24h / RTO 4h **[CẦN XÁC NHẬN]**.
- **Monitoring**: Prometheus + Grafana + Loki/Promtail, alert qua email/Telegram. Uptime check bên ngoài.
- **Hardening VPS**: UFW, SSH chỉ bằng key, fail2ban, unattended-upgrades.

## Consequences

- VPS là single point of failure. Giảm thiểu bằng backup offsite, runbook khôi phục và mục tiêu RTO 4h.
- Đội phải tự vá OS và nâng cấp image. Cần checklist bảo trì hằng tháng.
- Không còn giới hạn 512 MB: JVM có thể dùng G1GC với `-Xmx1g`.
- Có thể chuyển sang B (AWS) sau này mà không phải sửa ứng dụng, vì ứng dụng đã container hóa và cấu hình qua env.

## Trade-offs

- Rẻ và đơn giản, đổi lại không có HA hay auto-scaling. Với một khu villa, downtime ngắn có kế hoạch là chấp nhận được. **[CẦN XÁC NHẬN]** SLA 99.5%.
- Phải tự vận hành PostgreSQL thay vì dùng RDS: backup và nâng cấp major version là trách nhiệm của đội.
