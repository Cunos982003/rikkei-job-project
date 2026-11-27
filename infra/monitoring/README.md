# Monitoring Stack (`infra/monitoring`)

Thư mục này chứa cấu hình cho:
- `prometheus/prometheus.yml`: Thu thập metric từ 11 Spring Boot services (`/actuator/prometheus`, `scrape_interval: 15s`, label `service`, `env`) và 3 exporters (`postgres-exporter`, `redis-exporter`, `nginx-exporter`).
- `loki/loki-config.yml`, `promtail/promtail-config.yml`: Thu thập và lưu trữ log tập trung.
- `grafana/`: Thư mục chứa cấu hình/dashboard Grafana.

## Hướng dẫn nhanh: Thêm service mới vào Prometheus scrape

1. **Bật Actuator + Prometheus trên service mới**:
   - Thêm `spring-boot-starter-actuator` và `micrometer-registry-prometheus` vào `build.gradle`.
   - Cấu hình `application.yaml`:
     ```yaml
     management:
       endpoints:
         web:
           exposure:
             include: health,info,prometheus
       endpoint:
         health:
           probes:
             enabled: true
     ```
   - Cho phép `/actuator/prometheus` trong `SecurityConfig` nội bộ của service (truy cập công khai từ bên ngoài đã bị chặn ở Nginx).

2. **Khai báo job trong `infra/monitoring/prometheus/prometheus.yml`**:
   ```yaml
   - job_name: "<ten-service>"
     metrics_path: "/actuator/prometheus"
     static_configs:
       - targets: ["<ten-service>:<port>"]
         labels:
           service: "<ten-service>"
           env: "prod"
   ```

3. **Áp dụng cấu hình mới**:
   - Reload Prometheus (`POST http://prometheus:9090/-/reload` hoặc khởi động lại container `prometheus`) và kiểm tra target chuyển sang `UP`.

---

## Log tập trung (Loki + Promtail + Grafana)

- **Retention**: Loki cấu hình tự động xóa log quá **14 ngày** (`retention_period: 336h` với `compactor.retention_enabled: true` trong `loki/loki-config.yml`), lưu trên volume riêng `lokidata`.
- **Promtail**: Thu log từ toàn bộ Docker container qua `/var/run/docker.sock`, parse JSON từ `COMMON-02` và chỉ đưa `service`, `level` thành label (không đưa `traceId` thành label để tránh cardinality cao).
- **Grafana**: Tự động provision datasource `Prometheus` và `Loki`, kèm dashboard mẫu `Rikkei Jobs - Centralized Logs (Loki)`.

### Các câu truy vấn LogQL mẫu trong Grafana (Explore / Dashboard)

- **Lọc theo `service` và `level`**:
  ```logql
  {service="recruitment-service", level="ERROR"}
  ```
- **Truy vết toàn bộ luồng theo `traceId` (không đưa `traceId` vào label)**:
  ```logql
  {service=~".+"} |= "<traceId>" | json | traceId="<traceId>"
  ```
- **Tìm lỗi `500` hoặc `INVALID_TRANSITION` trên một service**:
  ```logql
  {service="recruitment-service"} |= "INVALID_TRANSITION" | json
  ```

