# Tài Liệu Đặc Tả API - API Gateway (Cổng Giao Tiếp Hợp Nhất & Điều Hướng)

> **Dự án**: Rikkei Jobs  
> **Service**: `api-gateway`  
> **Cổng chính thức**: `http://localhost:8080`  
> **Đối chiếu quy chuẩn phản hồi**: [docs/quy-chuan-api-response.md](../../docs/quy-chuan-api-response.md)

---

## 1. Vai Trò Của API Gateway

API Gateway là điểm truy cập duy nhất (Single Point of Entry) cho toàn bộ ứng dụng Frontend và Client:
1. **Định tuyến (Routing)**: Tiếp nhận request tại cổng `8080` và chuyển tiếp đến đúng microservice đích.
2. **Xác thực tập trung (JWT Authentication)**: Giải mã JWT token trong header `Authorization: Bearer <TOKEN>`, trích xuất thông tin người dùng và gắn thêm 2 header gửi xuống downstream services:
   - `X-User-Id`: UUID của người dùng.
   - `X-User-Role`: Vai trò (`CANDIDATE`, `HR`, `STAFF`, `ADMIN`, `MENTOR`).
3. **Giới hạn lưu lượng (Rate Limiting)**: Sử dụng Redis để giới hạn 100 request/phút theo từng địa chỉ IP, trả về `429 Too Many Requests` khi vượt ngưỡng.
4. **CORS & Bảo mật**: Xử lý toàn diện CORS Header cho Frontend (`http://localhost:5173`).

---

## 2. Bảng Ma Trận Định Tuyến Của API Gateway

| Service Đích | Cổng Nội Bộ | Tiền Tố Đường Dẫn (Routing Path) | Quy Tắc Rewrite / Filter |
| :----------- | :---------: | :-------------------------------- | :----------------------- |
| **`auth-service`** | `:8081` | `/api/v1/auth/**`, `/auth/**` | Chuyển tiếp trực tiếp, lọc CORS trùng lặp |
| **`profile-service`** | `:8082` | `/api/v1/profiles/**`, `/api/v1/files/**`, `/api/v1/candidates/**`, `/api/v1/skills`, `/api/v1/staff/profile-reviews`, `/api/v1/staff/profiles/**` | Chuyển tiếp trực tiếp, gắn `X-User-Id`, `X-User-Role` |
| **`evidence-service`** | `:8083` | `/api/services/evidence/**` | `StripPrefix=3` |
| **`assessment-service`** | `:8084` | `/api/services/assessment/**` | `StripPrefix=3` |
| **`job-service`** | `:8085` | `/api/v1/jobs/**`, `/api/v1/companies/**`, `/api/v1/staff/**` | `RewritePath=/api/v1/(?<segment>.*), /${segment}` |
| **`recruitment-service`**| `:8086` | `/api/v1/jobs/*/recruit`, `/api/v1/me/recruitments/**`, `/api/v1/recruitments/**`, `/api/v1/rounds/**`, `/api/v1/me/interviews/**`, `/api/v1/companies/me/interviews`, `/api/v1/hr/applications/**`, `/api/v1/me/offers/**` | Chuyển tiếp trực tiếp kèm Bearer Token |
| **`notification-service`**| `:8087`| `/api/services/notification/**` | `StripPrefix=3` |
| **`audit-service`** | `:8088` | `/api/services/audit/**` | `StripPrefix=3` |
| **`learning-service`** | `:8089` | `/api/v1/learning/**`, `/api/v1/me/target`, `/api/v1/me/gap-analysis`, `/api/v1/me/course-suggestions`, `/api/v1/me/enrollments/**`, `/api/v1/me/learning-path/**` | Chuyển tiếp trực tiếp kèm Bearer Token |
| **`ai_career_service`** | `:8090` | `/me/job-recommendations/**`, `/ai-career/**` | StripPrefix=1 đối với `/ai-career/**` |

---

## 3. Định Dạng Phản Hồi Lỗi Tại Gateway (`GatewayError.java`)

Khi request bị chặn tại Gateway (chưa kịp đến microservice downstream), Gateway trả về lỗi dạng:

```json
{
  "timestamp": "2026-11-20T08:00:00.1234567",
  "path": "/api/v1/profiles/me",
  "status": 401,
  "error": "Unauthorized",
  "message": "Token JWT không hợp lệ hoặc đã hết hạn",
  "requestId": "gw-req-12345"
}
```

---

## 4. Đánh Giá Đối Chiếu Quy Chuẩn Response (`quy-chuan-api-response.md`)

| Tiêu Chí Đánh Giá | Hiện Trạng Của `api-gateway` | Mức Độ Tuân Thủ | Ghi Chú |
| :---------------- | :--------------------------- | :-------------: | :------ |
| **Lọc và chuyển tiếp Header** | Giải mã và forward đầy đủ `X-User-Id`, `X-User-Role`, `Authorization` đến các microservice. | ✅ **Đạt** | Đảm bảo tính nhất quán định danh trên toàn bộ hệ thống. |
| **Phản hồi lỗi tại Gateway** | Lớp `GatewayError` sử dụng định dạng Spring WebFlux chuẩn `{ timestamp, path, status, error, message, requestId }` thay vì `ApiResponse<T>`. | ⚠️ **Cần chuẩn hóa** | Cần đồng bộ cấu trúc lỗi tại Gateway về Envelope `{ success: false, statusCode, message, data: null, timestamp }`. |

