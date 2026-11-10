# [DN-01] Quản lý doanh nghiệp, cấp tài khoản HR và cập nhật hồ sơ công ty

## 1. Mô tả tổng quan
- **Mục tiêu**: Cán bộ tuyển dụng (STAFF) tạo doanh nghiệp và cấp tài khoản nhân sự (HR); HR tự quản lý và chỉnh sửa hồ sơ công ty của mình; upload logo công ty an toàn.
- **Service Backend**: `backend/job-service` (cổng 8085).
- **Frontend**: `frontend/src/features/hr/CompanyProfilePage.tsx`, `frontend/src/features/staff/StaffCompanyPage.tsx`, `frontend/src/App.tsx`.
- **OpenAPI**: `docs/api/openapi/job-service-dn01.yaml`.

---

## 2. Thay đổi chi tiết

### 2.1. Cơ sở dữ liệu (Flyway Migration: `V1__init_company_and_user.sql`)
- **Bảng `companies`**:
  - `id`: Khóa chính (BIGSERIAL)
  - `name`: Tên doanh nghiệp (VARCHAR NOT NULL)
  - `tax_code`: Mã số thuế 10 hoặc 13 chữ số (VARCHAR UNIQUE NOT NULL)
  - `industry`: Ngành nghề hoạt động
  - `size`: Quy mô nhân sự (`1-50` | `51-200` | `201-1000` | `1000+`)
  - `address`: Địa chỉ trụ sở chính
  - `city`: Tỉnh / Thành phố
  - `website`: Địa chỉ trang web
  - `logo_url`: Đường dẫn ảnh logo công ty
  - `description`: Mô tả giới thiệu
  - `status`: Trạng thái (`ACTIVE` | `LOCKED`)
  - `created_by`: Người tạo (email cán bộ)
  - `created_at`, `updated_at`: Thời gian tạo và cập nhật
- **Bảng `users` (Bổ sung tối thiểu phục vụ NT-01/NT-02)**:
  - `id`: Khóa chính (BIGSERIAL)
  - `email`: Email đăng nhập (VARCHAR UNIQUE NOT NULL)
  - `full_name`: Họ và tên
  - `password_hash`: Mật khẩu băm (BCrypt)
  - `role`: Vai trò (`ROLE_STAFF`, `ROLE_HR`)
  - `company_id`: Khóa ngoại tham chiếu `companies(id)` (mỗi HR thuộc 1 công ty)
  - `must_change_password`: Cờ bắt buộc đổi mật khẩu lần đầu (BOOLEAN NOT NULL DEFAULT FALSE)
  - `created_at`: Thời gian tạo

### 2.2. Danh sách API Backend
| STT | Phương thức | Đường dẫn | Phân quyền | Mô tả |
|---|---|---|---|---|
| 1 | `POST` | `/staff/companies` | `ROLE_STAFF` | Cán bộ tạo doanh nghiệp mới. Mã số thuế 10 hoặc 13 chữ số, trùng trả về 409 |
| 2 | `POST` | `/staff/companies/{id}/hr-accounts` | `ROLE_STAFF` | Cán bộ cấp tài khoản HR, sinh mật khẩu tạm ngẫu nhiên, `must_change_password = true`, gửi email thông tin đăng nhập. Trùng email trả về 409 |
| 3 | `GET` | `/companies/me` | `ROLE_HR` | HR lấy thông tin chi tiết về công ty của mình |
| 4 | `PUT` | `/companies/me` | `ROLE_HR` | HR tự cập nhật hồ sơ công ty. **Không được sửa `tax_code` và `status`** |
| 5 | `PUT` | `/companies/{id}` | `ROLE_HR`, `STAFF` | Sửa công ty theo ID. Nếu HR công ty A gọi sửa công ty B trả về **404 Not Found** |
| 6 | `POST` | `/companies/me/logo` | `ROLE_HR` | Upload ảnh logo (định dạng JPG/PNG, dung lượng $\le$ 2MB). Quá 2MB hoặc sai định dạng trả về 400 |
| 7 | `POST` | `/api/v1/auth/login` | Public | Đăng nhập hệ thống (kiểm tra `must_change_password`) |
| 8 | `POST` | `/api/v1/auth/change-password` | Authenticated | Đổi mật khẩu lần đầu (xóa cờ `must_change_password`) |

### 2.3. Giao diện người dùng (Frontend)
- **Form Hồ sơ công ty (HR)**:
  - Hiển thị logo, thông tin doanh nghiệp, trạng thái hoạt động.
  - Chỉnh sửa: Tên công ty, Ngành nghề, Quy mô (1-50, 51-200, 201-1000, 1000+), Địa chỉ, Thành phố, Website, Mô tả.
  - Các trường `tax_code` và `status` bị khóa (disabled/readonly), ngăn HR can thiệp.
  - **Upload Logo**: Chọn tệp ảnh JPG/PNG, kiểm tra dung lượng $\le$ 2MB ngay tại client và server, xem trước ảnh, hiển thị thông báo lỗi rõ ràng nếu vượt dung lượng hoặc sai loại tệp.
- **Màn hình Cán bộ (Staff)**:
  - Form tạo doanh nghiệp với kiểm tra mã số thuế 10 hoặc 13 chữ số.
  - Form cấp tài khoản HR cho doanh nghiệp, hiển thị mật khẩu tạm và thông báo email gửi thành công.
- **Hộp thoại Đổi mật khẩu lần đầu**:
  - Hỗ trợ HR đăng nhập bằng mật khẩu tạm đổi sang mật khẩu mới.

---

## 3. Dữ liệu mẫu (Seed Data)
Hệ thống đã chuẩn bị sẵn dữ liệu mẫu trong migration:
1. **Tài khoản Cán bộ (Staff)**:
   - Email: `staff1@rikkei.vn`
   - Mật khẩu: `Password@123`
   - Vai trò: `ROLE_STAFF`
2. **Doanh nghiệp A**:
   - ID: `1`
   - Tên: `Công ty Cổ phần Công nghệ Rikkei A`
   - Mã số thuế: `0101234567` (10 chữ số)
   - HR phụ trách: `hr1@rikkei.vn` (Mật khẩu: `Password@123`, `ROLE_HR`)
3. **Doanh nghiệp B**:
   - ID: `2`
   - Tên: `Tập đoàn Giải pháp Công nghệ B`
   - Mã số thuế: `0107654321` (10 chữ số)
   - HR phụ trách: `hr2@rikkei.vn` (Mật khẩu: `Password@123`, `ROLE_HR`)

---

## 4. Hướng dẫn chạy & Kiểm thử

### 4.1. Chạy Backend (`job-service`)
```bash
cd backend/job-service

# Cách 1: Chạy trực tiếp bằng Gradle Wrapper (mặc định kết nối PostgreSQL hoặc H2 fallback)
./gradlew bootRun

# Cách 2: Chạy profile local dùng H2 in-memory không cần cài PostgreSQL
./gradlew bootRun --args='--spring.profiles.active=local'
```
Swagger UI: [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html)  
OpenAPI JSON: [http://localhost:8085/v3/api-docs](http://localhost:8085/v3/api-docs)

### 4.2. Chạy Frontend
```bash
cd frontend
npm install
npm run dev
```
Truy cập: [http://localhost:5173](http://localhost:5173)

---

## 5. Kết quả kiểm tra nghiệm thu (Acceptance Criteria)

| # | Tiêu chí nghiệm thu | Kết quả | Chi tiết kiểm tra |
|---|---|:---:|---|
| 1 | `staff1` tạo công ty + HR → có email chứa mật khẩu tạm; HR đăng nhập bị bắt đổi mật khẩu | **ĐẠT** | `MockEmailService` ghi nhận email gửi đi; API trả về `mustChangePassword = true`; HR login nhận thông báo bắt đổi mật khẩu |
| 2 | Tạo HR trùng email → 409 Conflict | **ĐẠT** | Gọi cấp HR với email đã có `hr1@rikkei.vn` trả về `409 Conflict` kèm thông báo chi tiết |
| 3 | `hr1` sửa được công ty A; gọi API sửa công ty B → 404 Not Found | **ĐẠT** | `hr1` gọi `PUT /companies/me` hoặc `PUT /companies/1` thành công (200); gọi `PUT /companies/2` bị trả về `404 Not Found` |
| 4 | Logo > 2MB → bị từ chối | **ĐẠT** | Upload tệp > 2MB bị trả về `400 Bad Request` ("Kích thước tệp logo không được vượt quá 2MB") |
| 5 | Logo sai định dạng (không phải JPG/PNG) → bị từ chối | **ĐẠT** | Upload PDF/SVG/TXT bị từ chối với thông báo "Chỉ chấp nhận ảnh định dạng JPG hoặc PNG" |
| 6 | Mã số thuế sai định dạng → báo lỗi | **ĐẠT** | Mã số thuế không đúng 10 hoặc 13 chữ số (ví dụ 9 số, 11 số, chứa chữ) bị từ chối với lỗi 400 |
| 7 | Không sửa `tax_code`, `status` | **ĐẠT** | HR gửi cập nhật không thể thay đổi `tax_code` hay `status`, hệ thống giữ nguyên giá trị gốc |
| 8 | OpenAPI cập nhật endpoint mới | **ĐẠT** | Đã xuất đặc tả đầy đủ tại `docs/api/openapi/job-service-dn01.yaml` |

---

## 6. Minh họa cURL kiểm thử nhanh

### 1) Tạo doanh nghiệp mới (STAFF)
```bash
curl -X POST http://localhost:8085/staff/companies \
  -H "Content-Type: application/json" \
  -H "X-User-Email: staff1@rikkei.vn" \
  -H "X-User-Role: ROLE_STAFF" \
  -d '{
    "name": "Công ty Cổ phần Rikkei Mới",
    "taxCode": "0109998888",
    "industry": "Công nghệ thông tin",
    "size": "201-1000",
    "address": "Tầng 21 Keangnam",
    "city": "Hà Nội"
  }'
```

### 2) Cấp tài khoản HR
```bash
curl -X POST http://localhost:8085/staff/companies/1/hr-accounts \
  -H "Content-Type: application/json" \
  -H "X-User-Email: staff1@rikkei.vn" \
  -H "X-User-Role: ROLE_STAFF" \
  -d '{
    "email": "hr.moi@rikkei.vn",
    "fullName": "Nguyễn Văn HR Mới"
  }'
```

### 3) HR lấy hồ sơ công ty mình
```bash
curl -X GET http://localhost:8085/companies/me \
  -H "X-User-Email: hr1@rikkei.vn" \
  -H "X-User-Role: ROLE_HR" \
  -H "X-Company-Id: 1"
```

### 4) HR sửa hồ sơ công ty mình
```bash
curl -X PUT http://localhost:8085/companies/me \
  -H "Content-Type: application/json" \
  -H "X-User-Email: hr1@rikkei.vn" \
  -H "X-User-Role: ROLE_HR" \
  -H "X-Company-Id: 1" \
  -d '{
    "name": "Công ty Cổ phần Công nghệ Rikkei A - Cập nhật",
    "industry": "Fintech & AI",
    "size": "201-1000",
    "city": "Hà Nội"
  }'
```

### 5) HR 1 cố tình sửa công ty B (ID: 2) -> 404
```bash
curl -X PUT http://localhost:8085/companies/2 \
  -H "Content-Type: application/json" \
  -H "X-User-Email: hr1@rikkei.vn" \
  -H "X-User-Role: ROLE_HR" \
  -H "X-Company-Id: 1" \
  -d '{"name": "Sửa trái phép"}'
```
Response:
```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Không tìm thấy doanh nghiệp có id: 2",
  "timestamp": "2026-11-10T11:00:00Z"
}
```

