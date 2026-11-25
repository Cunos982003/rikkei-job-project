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
1. **`RestAuthClient`**: Sử dụng `RestClient` kết hợp `JdkClientHttpRequestFactory` (connect timeout 5s, read timeout 20s), gọi các REST endpoint nội bộ của `auth-service` (`/api/v1/auth/internal/hr-accounts/...`), chuyển tiếp header `Authorization: Bearer <token>`, giải nén envelope `{success, statusCode, message, data, timestamp}` và xử lý ánh xạ lỗi:
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

## Quản lý Tin Tuyển Dụng & Kỹ Năng Yêu Cầu (DN-02)

### 1. Kiến trúc & Tích hợp danh mục kỹ năng
- `job-service` quản lý bảng `jobs` và `job_skills`.
- Kỹ năng yêu cầu cho JD được tra cứu và chuẩn hóa từ `profile-service` thông qua `SkillCatalogClient` (`GET /api/v1/skills`).
- `RestSkillCatalogClient`: kết nối `profile-service` (cổng 8082), cache in-memory 5 phút, timeout kết nối 3s, timeout đọc 5s. Nếu dịch vụ gián đoạn trả lỗi 502 Bad Gateway.
- `StubSkillCatalogClient`: kích hoạt với `skill.catalog.stub=true` (mặc định profile `local` và `test`), cung cấp sẵn 20 kỹ năng mẫu từ `data.sql`.

### 2. Biến cấu hình mới:
| Tên biến môi trường | Giá trị mặc định | Mô tả |
|---------------------|------------------|-------|
| `PROFILE_SERVICE_URL` | `http://localhost:8082` | URL gốc của `profile-service` để lấy danh mục kỹ năng chuẩn |
| `SKILL_CATALOG_STUB` | `false` (prod), `true` (local/test) | `true`: dùng `StubSkillCatalogClient` in-memory; `false`: kết nối REST tới `profile-service` |

### 3. API theo DN-02:
- `POST /jobs`: HR tạo bản nháp tin tuyển dụng (`DRAFT`).
- `PUT /jobs/{id}`: HR cập nhật thông tin JD. Nếu đang `APPROVED` sẽ chuyển sang `PENDING`; đang `PENDING` hoặc `CLOSED` trả về `409 Conflict`.
- `PUT /jobs/{id}/skills`: HR cập nhật danh sách kỹ năng yêu cầu (tối đa 30 kỹ năng, không trùng lặp, mã kỹ năng phải tồn tại trong catalog).
- `POST /jobs/{id}/submit`: HR gửi duyệt tin tuyển dụng (chỉ gửi từ `DRAFT` hoặc `REJECTED`, yêu cầu ít nhất 1 kỹ năng bắt buộc và `deadline` $\ge$ hôm nay).
- `POST /jobs/{id}/close`: HR đóng tin tuyển dụng (không áp dụng cho tin đã `CLOSED`).
- `GET /companies/me/jobs`: HR xem danh sách JD của công ty mình có phân trang và lọc `status`.
- `GET /jobs/{id}`: Xem chi tiết JD (HR chỉ xem tin công ty mình, STAFF/ADMIN xem mọi tin, Ứng viên chỉ xem tin `APPROVED`).

## Chạy kiểm thử

```powershell
./gradlew clean compileJava test
```

## Điểm Khớp Kỹ Năng Ứng Viên Với JD (DN-06)

### 1. Kiến trúc & Tích hợp điểm kỹ năng ứng viên
- `job-service` lưu điểm khớp và chi tiết so khớp vào bảng `match_scores`.
- Điểm kỹ năng đã kiểm định của ứng viên (`verifiedScore`) được lấy từ `profile-service` qua `CandidateSkillClient` (`GET /api/v1/profiles/internal/{userId}/skill-scores`).
- `RestCandidateSkillClient`: gọi REST sang `profile-service`, chuyển tiếp header `Authorization`, timeout kết nối 3s, đọc 5s.
- `StubCandidateSkillClient`: dùng trong test và chạy local (`candidate.skills.stub=true`).
- Cơ chế stale hook: khi JD cập nhật kỹ năng (`PUT /jobs/{id}/skills`) hoặc thông tin (`PUT /jobs/{id}`), các bản ghi `match_scores` liên quan sẽ được tự động đánh dấu `stale = true`. Khi có yêu cầu tra cứu độ khớp, hệ thống luôn tính toán lại và cập nhật bản ghi mới nhất (`stale = false`).

### 2. Biến cấu hình:
| Biến môi trường | Mặc định | Mô tả |
|---|---|---|
| `CANDIDATE_SKILLS_STUB` | `false` (prod), `true` (local/test) | `true`: dùng stub in-memory; `false`: gọi REST tới `profile-service` |
| `PROFILE_SERVICE_URL` | `http://localhost:8082` | URL gốc của `profile-service` |

### 3. Thử nhanh khi chạy local:
Khởi động service với profile `local` hoặc thiết lập biến môi trường `CANDIDATE_SKILLS_STUB=true`:
```powershell
$env:CANDIDATE_SKILLS_STUB="true"
./gradlew bootRun
```

### 4. API theo DN-06:
- `GET /jobs/{id}/match/me`: Ứng viên (`ROLE_CANDIDATE`) xem điểm khớp của chính mình với JD `APPROVED`.
- `GET /jobs/{id}/match/{userId}`: `ROLE_HR`, `ROLE_STAFF`, `ROLE_ADMIN` xem điểm khớp của ứng viên với JD (HR chỉ xem được JD công ty mình).

## Cán bộ Duyệt JD & Quản lý Doanh nghiệp (DN-07)

### 1. Kiến trúc & Cơ chế vận hành
- **Phê duyệt / Từ chối JD**:
  - Cán bộ (`ROLE_STAFF`, `ROLE_ADMIN`) duyệt tin tuyển dụng ở trạng thái `PENDING` sang `APPROVED` hoặc từ chối sang `REJECTED`.
  - Khi từ chối, lý do trả lại (`rejectedReason`) là bắt buộc, không được để trống và tối đa 1000 ký tự.
  - Hệ thống tự động ghi nhận định danh cán bộ (`reviewedBy`) và thời điểm (`reviewedAt`) vào entity `Job`, đồng thời ghi một bản ghi lịch sử `APPROVE_JD` hoặc `REJECT_JD` vào bảng `company_actions`.
  - Doanh nghiệp có thể chỉnh sửa tin bị từ chối (`REJECTED`) và gửi duyệt lại (`PENDING`).
- **Khoá / Mở khoá doanh nghiệp & Cascade tài khoản HR**:
  - Khi khoá công ty (`POST /staff/companies/{id}/lock`), công ty chuyển sang `LOCKED`. Hệ thống tự động gọi sang `auth-service` khoá toàn bộ tài khoản HR đang `ACTIVE` với tiền tố lý do `[COMPANY_LOCK] <reason>`. Ghi bản ghi `LOCK` vào `company_actions`.
  - Khi mở khoá (`POST /staff/companies/{id}/unlock`), công ty chuyển sang `ACTIVE`. Hệ thống chỉ mở khoá các tài khoản HR bị khoá có `lockedReason` bắt đầu bằng `[COMPANY_LOCK]`. Những HR bị khoá riêng vì lý do vi phạm khác vẫn giữ nguyên trạng thái `LOCKED`. Ghi bản ghi `UNLOCK` vào `company_actions`.
  - Nếu quá trình gọi sang `auth-service` gặp sự cố, transaction sẽ rollback và trả mã lỗi `502 Bad Gateway`, bảo đảm tính nhất quán dữ liệu giữa 2 service.
- **Chặn thao tác & Ẩn dữ liệu của doanh nghiệp bị khoá**:
  - Toàn bộ thao tác của HR thuộc công ty bị khoá (tạo JD, sửa JD, cập nhật kỹ năng, gửi duyệt, đóng tin, sửa hồ sơ công ty, upload logo) đều bị chặn và trả về `403 Forbidden`.
  - Tin tuyển dụng của doanh nghiệp bị khoá tự động bị ẩn khỏi danh sách công khai (`GET /jobs`). Nếu ứng viên hoặc guest gọi trực tiếp `GET /jobs/{id}` của doanh nghiệp bị khoá sẽ nhận mã lỗi `404 Not Found`.
- **Tự động đóng tin tuyển dụng hết hạn (Cron Scheduler)**:
  - Tiến trình scheduler chạy định kỳ (mặc định lúc 00:05 mỗi ngày theo múi giờ `Asia/Ho_Chi_Minh`) quét các JD đang `APPROVED` có `deadline < ngày hiện tại`, tự động chuyển trạng thái sang `CLOSED` và đánh dấu `stale` trong bảng điểm khớp `match_scores`.
  - Cho phép bật/tắt qua cấu hình `job.expiry.enabled` (mặc định `true`, tắt trong test profile) và tùy chỉnh biểu thức cron qua `job.expiry.cron`.

### 2. Biến cấu hình:
| Biến môi trường | Giá trị mặc định | Mô tả |
|-----------------|------------------|-------|
| `JOB_EXPIRY_ENABLED` | `true` (prod/local), `false` (test) | Bật/tắt tiến trình tự động đóng tin tuyển dụng quá hạn |
| `JOB_EXPIRY_CRON` | `0 5 0 * * *` | Biểu thức cron chạy lúc 00:05 hàng ngày theo giờ Việt Nam |

### 3. API theo DN-07:
- `POST /staff/jobs/{id}/approve`: Cán bộ duyệt JD `PENDING` sang `APPROVED`.
- `POST /staff/jobs/{id}/reject`: Cán bộ từ chối JD `PENDING` sang `REJECTED` kèm lý do.
- `GET /staff/jobs?status=PENDING&companyId=...`: Lọc danh sách JD theo trạng thái và doanh nghiệp.
- `POST /staff/companies/{id}/lock`: Cán bộ khoá doanh nghiệp và cascade khoá HR.
- `POST /staff/companies/{id}/unlock`: Cán bộ mở khoá doanh nghiệp và cascade mở HR.
- `GET /staff/companies/{id}/history`: Cán bộ xem lịch sử thao tác tác động lên doanh nghiệp.


