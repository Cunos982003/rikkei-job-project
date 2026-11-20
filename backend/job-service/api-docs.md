# Tài Liệu Đặc Tả API - Job Service (Quản Lý Việc Làm & Doanh Nghiệp)

> **Dự án**: Rikkei Jobs  
> **Service**: `job-service`  
> **Cổng nội bộ**: `http://localhost:8085`  
> **Cổng qua API Gateway**: `http://localhost:8080` (Route rewrite: `/api/v1/jobs/**` → `/jobs/**`, `/api/v1/companies/**` → `/companies/**`, `/api/v1/staff/**` → `/staff/**`)  
> **Đối chiếu quy chuẩn phản hồi**: [docs/quy-chuan-api-response.md](../../docs/quy-chuan-api-response.md)

---

## 1. Tổng Quan Về Quyền Hạn & Headers

### 1.1. Headers Xác Thực
- **Public Endpoints**: `/api/v1/jobs`, `/api/v1/jobs/{jobId}` (Không yêu cầu token).
- **HR Endpoints**: `/api/v1/companies/me`, `/api/v1/companies/me/logo` (Yêu cầu `Authorization: Bearer <TOKEN>` vai trò `HR`).
- **Staff / Admin Endpoints**: `/api/v1/staff/companies/**`, `/api/v1/staff/hr-accounts/**` (Yêu cầu `Authorization: Bearer <TOKEN>` vai trò `STAFF` hoặc `ADMIN`).

---

## 2. Bảng Tổng Hợp Danh Sách API

| STT | Method | Đường dẫn (Qua Gateway :8080) | Đường dẫn (Nội bộ :8085) | Vai trò yêu cầu | Mô tả |
| :-: | :----: | :---------------------------- | :----------------------- | :-------------- | :---- |
| 1 | `GET` | `/api/v1/jobs` | `/jobs` hoặc `/api/v1/jobs` | Public | Tìm kiếm danh sách tin tuyển dụng việc làm (hỗ trợ lọc, tìm kiếm, phân trang) |
| 2 | `GET` | `/api/v1/jobs/{jobId}` | `/jobs/{jobId}` | Public | Xem thông tin chi tiết một tin tuyển dụng theo ID |
| 3 | `GET` | `/api/v1/companies/me` | `/companies/me` | `HR` | Xem hồ sơ công ty trực thuộc của tài khoản HR đang đăng nhập |
| 4 | `PUT` | `/api/v1/companies/me` | `/companies/me` | `HR` | Cập nhật thông tin hồ sơ công ty (trừ MST và trạng thái) |
| 5 | `PUT` | `/api/v1/companies/{id}` | `/companies/{id}` | `HR`, `STAFF`, `ADMIN` | Cập nhật thông tin doanh nghiệp theo ID |
| 6 | `POST` | `/api/v1/companies/me/logo` | `/companies/me/logo` | `HR` | Tải lên logo công ty (Multipart File JPG/PNG <= 2MB) |
| 7 | `POST` | `/api/v1/staff/companies` | `/staff/companies` | `STAFF`, `ADMIN` | Cán bộ tạo mới hồ sơ doanh nghiệp (yêu cầu MST 10 hoặc 13 số duy nhất) |
| 8 | `GET` | `/api/v1/staff/companies` | `/staff/companies` | `STAFF`, `ADMIN` | Cán bộ tra cứu danh sách doanh nghiệp (lọc tên, MST, trạng thái) |
| 9 | `GET` | `/api/v1/staff/companies/{id}` | `/staff/companies/{id}` | `STAFF`, `ADMIN` | Cán bộ xem chi tiết doanh nghiệp và danh sách tài khoản HR liên kết |
| 10 | `POST` | `/api/v1/staff/companies/{id}/hr-accounts` | `/staff/companies/{id}/hr-accounts` | `STAFF`, `ADMIN` | Cán bộ cấp tài khoản HR đại diện cho doanh nghiệp |
| 11 | `POST` | `/api/v1/staff/companies/{id}/logo` | `/staff/companies/{id}/logo` | `STAFF`, `ADMIN` | Cán bộ tải lên logo cho doanh nghiệp |
| 12 | `POST` | `/api/v1/staff/hr-accounts/{id}/lock` | `/staff/hr-accounts/{id}/lock` | `STAFF`, `ADMIN` | Cán bộ khóa tài khoản HR có ghi nhận lý do |
| 13 | `POST` | `/api/v1/staff/hr-accounts/{id}/unlock` | `/staff/hr-accounts/{id}/unlock` | `STAFF`, `ADMIN` | Cán bộ mở khóa tài khoản HR |

---

## 3. Chi Tiết Từng API & Dữ Liệu Input/Output

### 3.1. Danh Sách Việc Làm Tuyển Dụng (`GET /api/v1/jobs`)
- **Query Params**:
  - `keyword` (String, optional): Từ khóa tìm theo tiêu đề việc làm, tên công ty, mô tả.
  - `location` (String, optional): Địa điểm làm việc (Hà Nội, TP.HCM, Đà Nẵng...).
  - `jobType` (String, optional): `FULL_TIME`, `PART_TIME`, `INTERNSHIP`...
  - `status` (String, optional, default: `APPROVED`).
  - `page` (Integer, default: 0).
  - `size` (Integer, default: 50).
- **Response hiện tại**: *(Raw JSON Map - Vi phạm Envelope)*
  ```json
  {
    "items": [
      {
        "id": "44444444-4444-4444-4444-444444444401",
        "companyId": "33333333-3333-3333-3333-333333333301",
        "companyName": "Công ty Cổ phần Rikkeisoft",
        "title": "Java Backend Developer (Fresher/Junior)",
        "description": "Tham gia phát triển hệ thống backend phân tán...",
        "requirements": "Nắm vững Java Core, Spring Boot, SQL...",
        "experienceLevel": "FRESHER",
        "jobType": "FULL_TIME",
        "status": "APPROVED",
        "salaryMin": 10000000,
        "salaryMax": 18000000,
        "workLocation": "Hà Nội",
        "expiresAt": "2026-12-31T23:59:59Z",
        "createdAt": "2026-11-01T08:00:00Z"
      }
    ],
    "total": 1,
    "page": 0,
    "size": 50
  }
  ```

### 3.2. Chi Tiết Việc Làm (`GET /api/v1/jobs/{jobId}`)
- **Path Variable**: `jobId` (UUID)
- **Response hiện tại**: *(Raw JSON Map - Vi phạm Envelope)*
  ```json
  {
    "id": "44444444-4444-4444-4444-444444444401",
    "companyId": "33333333-3333-3333-3333-333333333301",
    "companyName": "Công ty Cổ phần Rikkeisoft",
    "title": "Java Backend Developer (Fresher/Junior)",
    "description": "Chi tiết mô tả công việc...",
    "requirements": "Yêu cầu kỹ năng...",
    "experienceLevel": "FRESHER",
    "jobType": "FULL_TIME",
    "status": "APPROVED",
    "salaryMin": 10000000,
    "salaryMax": 18000000,
    "workLocation": "Hà Nội",
    "expiresAt": "2026-12-31T23:59:59Z",
    "createdAt": "2026-11-01T08:00:00Z"
  }
  ```

### 3.3. Xem Hồ Sơ Doanh Nghiệp Của HR (`GET /api/v1/companies/me`)
- **Header**: `Authorization: Bearer <HR_JWT_TOKEN>`
- **Response hiện tại**: *(Raw DTO `CompanyResponse` - Vi phạm Envelope)*
  ```json
  {
    "id": "33333333-3333-3333-3333-333333333301",
    "taxCode": "0106037891",
    "name": "Công ty Cổ phần Rikkeisoft",
    "industry": "Công nghệ thông tin",
    "address": "Tầng 21, Tòa Handico, Mễ Trì, Nam Từ Liêm, Hà Nội",
    "website": "https://rikkeisoft.com",
    "contactEmail": "contact@rikkeisoft.com",
    "contactPhone": "02436231685",
    "logoUrl": "/uploads/logos/rikkei.png",
    "status": "ACTIVE"
  }
  ```

### 3.4. Cập Nhật Hồ Sơ Công Ty Của HR (`PUT /api/v1/companies/me`)
- **Header**: `Authorization: Bearer <HR_JWT_TOKEN>`
- **Request Body**:
  ```json
  {
    "name": "Công ty Cổ phần Rikkeisoft Việt Nam",
    "industry": "Công nghệ thông tin & Viễn thông",
    "address": "Tầng 21, Tòa Handico, Nam Từ Liêm, Hà Nội",
    "website": "https://rikkeisoft.com",
    "contactEmail": "hr@rikkeisoft.com",
    "contactPhone": "02436231685"
  }
  ```
- **Response hiện tại**: *(Raw DTO `CompanyResponse` - Vi phạm Envelope)*

### 3.5. Cán Bộ Tạo Doanh Nghiệp Mới (`POST /api/v1/staff/companies`)
- **Header**: `Authorization: Bearer <STAFF_JWT_TOKEN>`
- **Request Body**:
  ```json
  {
    "taxCode": "0106037899",
    "name": "Công ty Cổ phần Giải pháp Phần mềm ABC",
    "industry": "Công nghệ thông tin",
    "address": "Duy Tân, Cầu Giấy, Hà Nội",
    "website": "https://abc.com",
    "contactEmail": "hr@abc.com",
    "contactPhone": "0987654321"
  }
  ```
- **Response hiện tại**: *(Raw DTO `CompanyResponse` - HTTP 201 Created - Vi phạm Envelope)*

### 3.6. Cán Bộ Cấp Tài Khoản HR Cho Doanh Nghiệp (`POST /api/v1/staff/companies/{id}/hr-accounts`)
- **Header**: `Authorization: Bearer <STAFF_JWT_TOKEN>`
- **Request Body**:
  ```json
  {
    "email": "hr.lead@rikkeisoft.com",
    "fullName": "Nguyễn Thị Mai HR",
    "phone": "0912345678"
  }
  ```
- **Response hiện tại**: *(Raw DTO `HrAccountResponse` - HTTP 201 Created - Vi phạm Envelope)*
  ```json
  {
    "id": "55555555-5555-5555-5555-555555555501",
    "companyId": "33333333-3333-3333-3333-333333333301",
    "email": "hr.lead@rikkeisoft.com",
    "fullName": "Nguyễn Thị Mai HR",
    "phone": "0912345678",
    "status": "ACTIVE"
  }
  ```

### 3.7. Cán Bộ Khóa / Mở Khóa Tài Khoản HR (`POST /api/v1/staff/hr-accounts/{id}/lock` & `unlock`)
- **Request Body (Lock)**:
  ```json
  {
    "reason": "Doanh nghiệp yêu cầu thu hồi quyền quản trị"
  }
  ```
- **Response hiện tại**: *(Raw DTO `StaffHrAccountResponse` - Vi phạm Envelope)*
  ```json
  {
    "id": "55555555-5555-5555-5555-555555555501",
    "status": "LOCKED",
    "lockedReason": "Doanh nghiệp yêu cầu thu hồi quyền quản trị"
  }
  ```

---

## 4. Đánh Giá Đối Chiếu Quy Chuẩn Response (`quy-chuan-api-response.md`)

| Tiêu Chí Đánh Giá | Hiện Trạng Của `job-service` | Mức Độ Tuân Thủ | Ghi Chú |
| :---------------- | :--------------------------- | :-------------: | :------ |
| **Cấu trúc Envelope 5 trường** | Toàn bộ 13 endpoints trong `JobQueryController`, `CompanyController`, `StaffCompanyController`, `StaffHrAccountController` **đều trả về raw data** (Map, CompanyResponse, PageResponse). Không hề bọc Envelope `ApiResponse<T>`. | ❌ **Vi phạm 100%** | Đây là vi phạm nghiêm trọng nhất, Frontend không thể dùng interceptor chung để bóc tách `data`. |
| **Cấu trúc Phân trang** | `GET /jobs` trả về `{ items, total, page, size }`, `GET /staff/companies` trả về raw `PageResponse`. | ❌ **Vi phạm chuẩn phân trang** | Quy chuẩn yêu cầu dạng `{ items: [...], pagination: { page, size, totalElements, totalPages, isFirst, isLast } }`. |
| **Thông báo kết quả (`message`)** | Không có trường `message` trong dữ liệu trả về thành công. | ❌ **Vi phạm** | Thiếu thông báo kết quả thân thiện tiếng Việt cho người dùng. |
| **Xử lý Exception** | Trực tiếp ném `ResponseStatusException` hoặc Spring mặc định, không có Global Handle Exception chuẩn 5 trường. | ❌ **Vi phạm** | Khi có lỗi trả về format mặc định của Spring Web MVC. |

