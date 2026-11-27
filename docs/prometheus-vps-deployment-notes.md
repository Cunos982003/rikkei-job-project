# Lưu ý triển khai Prometheus & Exporters lên VPS (OBS-PLAT-01)

Tài liệu này tổng hợp các điểm cần kiểm tra và xử lý trước khi hoàn thiện cấu hình monitoring (`prometheus`, `postgres-exporter`, `redis-exporter`, `nginx-exporter`) và đẩy lên VPS với `docker-compose.prod.yml`.

---

## 1. Ràng buộc port trong `docker-compose.prod.yml`

- File `docker-compose.prod.yml` hiện chưa khai báo `prometheus`, `postgres-exporter`, `redis-exporter`, `nginx-exporter`.
- Khi bổ sung các container monitoring vào `docker-compose.prod.yml`, **không được mở `ports:` ra host**, vì script kiểm tra `infra/tests/validate-production-compose.ps1` bắt buộc chỉ duy nhất `frontend` được publish port (`127.0.0.1:8080`).
- Nếu cần truy cập UI Prometheus từ máy cá nhân, dùng SSH tunnel tới mạng nội bộ Docker thay vì mở port công khai.

## 2. Tạo user PostgreSQL read-only (`pg_exporter`) trên VPS

- Script `infra/init-db/02-create-monitoring-user.sql` chỉ tự chạy bởi `docker-entrypoint-initdb.d` khi volume `pgdata` **trống** (lần khởi tạo cluster đầu tiên).
- Trong `docker-compose.prod.yml`, `auth-service` đang đặt `SPRING_FLYWAY_ENABLED: "false"`, nên migration `backend/auth-service/src/main/resources/db/migration/V6__create_monitoring_readonly_user.sql` **sẽ không tự chạy** trên VPS.
- **Bắt buộc chạy tay 1 lần trên VPS** trước khi bật `postgres-exporter`:

  ```bash
  docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres \
    psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < infra/init-db/02-create-monitoring-user.sql
  ```

- Nên đổi mật khẩu mặc định `pg_exporter_readonly` sang mật khẩu mạnh riêng trong `.env.prod` (và truyền qua biến môi trường cho `DATA_SOURCE_NAME` của `postgres-exporter`).

## 3. Bật `stub_status` và chặn `/actuator/prometheus` trong Nginx

- `infra/docker/frontend.prod.conf` (và `infra/docker/frontend.conf`) cần bật `stub_status on;` tại `/nginx_status` (chỉ cho phép dải mạng nội bộ Docker/localhost) để `nginx-exporter` scrape được metric.
- Hiện tại Nginx đang proxy `/api/` và `/ai-career/` vào `api-gateway:8080` (nơi `/actuator/prometheus` đang mở `permitAll()`). Cần chặn truy cập công khai tới `/actuator/prometheus` ngay tại Nginx (`location ~* /actuator/prometheus { deny all; return 403; }`) để không lộ metric qua `/api/services/*/actuator/prometheus` hoặc `/ai-career/actuator/prometheus`.

## 4. Trạng thái 4/11 service chưa chạy trong `docker-compose.prod.yml`

- `infra/monitoring/prometheus/prometheus.yml` đang cấu hình scrape đủ **11 Spring Boot services**, nhưng `docker-compose.prod.yml` chỉ chạy 7 service (`assessment-service`, `evidence-service`, `notification-service`, `audit-service` không nằm trong stack prod và bị kiểm tra loại trừ trong `validate-production-compose.ps1`).
- Trên VPS, 4 target chưa chạy sẽ hiển thị `DOWN` trong Prometheus; có thể chấp nhận hoặc tách file cấu hình riêng chỉ gồm 7 service cho môi trường prod để tránh cảnh báo giả.

## 5. Giới hạn lưu trữ dữ liệu Prometheus trên VPS

- Khi thêm service `prometheus` vào `docker-compose.prod.yml`, cần mount volume `prometheus-data:/prometheus` và đặt `--storage.tsdb.retention.time=15d` (hoặc `7d`) để tránh dữ liệu TSDB làm đầy ổ đĩa VPS.

## 6. Kiểm tra trước khi deploy lên VPS

1. Chạy kiểm tra cấu hình Compose:
   ```powershell
   .\infra\tests\validate-production-compose.ps1
   ```
   hoặc:
   ```bash
   docker compose --env-file .env.prod -f docker-compose.prod.yml config -q
   ```
2. Nếu có chỉnh sửa `infra/docker/frontend.prod.conf` hoặc code Java (`build.gradle`, `SecurityConfig`), cần build và push lại image mới lên GHCR trước khi `docker compose pull && up -d` trên VPS.

---

## Phụ lục: Hướng dẫn ngắn thêm service mới vào Prometheus scrape

1. Đảm bảo service mới đã thêm `spring-boot-starter-actuator` + `micrometer-registry-prometheus` và mở `management.endpoints.web.exposure.include=health,info,prometheus`.
2. Cho phép `/actuator/prometheus` trong `SecurityConfig` nội bộ của service (nhưng vẫn chặn ở Nginx public).
3. Mở `infra/monitoring/prometheus/prometheus.yml` và thêm 1 job mới dưới `scrape_configs`:

   ```yaml
   - job_name: "<ten-service>"
     metrics_path: "/actuator/prometheus"
     static_configs:
       - targets: ["<ten-service>:<port>"]
         labels:
           service: "<ten-service>"
           env: "prod"
   ```

4. Reload Prometheus (hoặc gửi `POST /-/reload` nếu bật `--web.enable-lifecycle`) và kiểm tra target chuyển sang `UP`.
