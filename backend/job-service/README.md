# Job Service - Rikkei Jobs

Dịch vụ quản lý thông tin doanh nghiệp (Company) và tích hợp tài khoản HR (DN-01, DN-05).

## Kiến trúc & Trách nhiệm (DN-01 & DN-05)

- **`job-service`** chỉ sở hữu và quản lý bảng `companies` (hồ sơ doanh nghiệp, logo, thông tin liên hệ).
- **`auth-service`** sở hữu và quản lý bảng `users` (tài khoản người dùng: CANDIDATE, HR, STAFF, ADMIN).
- Toàn bộ việc quản lý tài khoản người dùng thuộc về `auth-service`. `job-service` không còn bảng `users` cục bộ và không xử lý xác thực đăng nhập hay đổi mật khẩu.
- Mọi thao tác quản lý tài khoản HR từ cán bộ tuyển dụng (Staff) được `job-service` ủy quyền gọi sang `auth-service` thông qua `AuthClient`:
  - Cấp tài khoản HR (`createHrAccount`)
  - Khóa tài khoản HR (`lockHrAccount`)
  - Mở khóa tài khoản HR (`unlockHrAccount`)
  - Lấy danh sách tài khoản HR theo doanh nghiệp (`listHrAccounts`)
  - Đếm số lượng HR theo danh sách doanh nghiệp (`countHrAccounts`)

## Cấu hình môi trường (Environment Variables)

| Tên biến môi trường | Giá trị mặc định | Mô tả |
|---------------------|------------------|-------|
| `JWT_SECRET`        | *(Bắt buộc trên prod, có sẵn trên local)* | Chuỗi bí mật HMAC-SHA dùng để giải mã và xác thực token JWT do `auth-service` phát hành |
| `AUTH_BASE_URL`     | `http://localhost:8081` | URL gốc của `auth-service` để gọi REST API nội bộ |
| `AUTH_CLIENT_STUB`  | `false` (prod), `true` (local/test) | `true`: dùng `StubAuthClient` in-memory khi auth-service chưa sẵn sàng; `false`: dùng `RestAuthClient` gọi API thực |

### Cơ chế hoạt động của AuthClient:
1. **`RestAuthClient`**: Sử dụng `RestClient` kết hợp `JdkClientHttpRequestFactory` (connect timeout 3s, read timeout 5s), gọi các REST endpoint nội bộ của `auth-service` (`/api/v1/auth/internal/hr-accounts/...`), chuyển tiếp header `Authorization: Bearer <token>`, giải nén envelope `{success, statusCode, message, data, timestamp}` và xử lý ánh xạ lỗi:
   - 409 Conflict: `createHrAccount` trả lỗi "Email đã tồn tại"; các thao tác `lock`/`unlock`/`list`/`count` giữ nguyên thông báo từ `auth-service`.
   - 404 Not Found: trả `ResourceNotFoundException`.
   - 4xx Bad Request: trả `BadRequestException`.
   - 5xx / Lỗi kết nối / Timeout: trả `BadGatewayException` (502).
2. **`StubAuthClient`**: Kích hoạt trên profile `local` hoặc `test` khi `auth.client.stub=true`. Lưu trữ dữ liệu HR trong bộ nhớ (`ConcurrentHashMap`) phục vụ phát triển độc lập và chạy test tự động mà không cần chạy `auth-service`.

## Chạy ứng dụng

```powershell
# Chạy với profile local (sử dụng H2 in-memory và kết nối auth-service)
./gradlew bootRun --args="--spring.profiles.active=local"
```

Swagger UI: http://localhost:8085/swagger-ui.html  
OpenAPI JSON: http://localhost:8085/v3/api-docs

## Hồ sơ công ty cho FE

- HR có thể sửa `name`, `industry`, `size`, `address`, `city`, `website`, `description` và tải logo lên; `taxCode` và `status` chỉ hiển thị, không cho sửa.
- `size` chọn một trong `1-50`, `51-200`, `201-1000`, `1000+`.
- Logo chỉ nhận JPG/PNG, tối đa 2 MB; FE nên kiểm tra và báo lỗi ngay khi chọn tệp.
- `logoUrl` là đường dẫn tương đối (ví dụ `/uploads/logos/company-...png`); FE ghép với base URL của API.
- Khi đăng nhập có `mustChangePassword=true`, FE bắt buộc mở màn đổi mật khẩu và chỉ dùng token mới trả về sau khi đổi mật khẩu thành công.

## API theo DN-01 & DN-05

- STAFF/ADMIN: `POST /staff/companies`, `GET /staff/companies`, `GET /staff/companies/{id}`, `POST /staff/companies/{id}/hr-accounts`, `POST /staff/companies/{id}/hr-accounts/{userId}/lock`, `POST /staff/companies/{id}/hr-accounts/{userId}/unlock`.
- HR: `GET /companies/me`, `PUT /companies/me`, `POST /companies/me/logo`.
- `PUT /companies/{id}` được giữ: STAFF/ADMIN cập nhật theo ID; HR chỉ cập nhật công ty của mình, công ty khác trả 404.

## Chạy kiểm thử

```powershell
./gradlew clean compileJava test
```
