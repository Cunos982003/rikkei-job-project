# Tài Liệu Đặc Tả API - Auth Service (Quản Lý Xác Thực & Tài Khoản)

> **Dự án**: Rikkei Jobs  
> **Service**: `auth-service`  
> **Cổng nội bộ**: `http://localhost:8081`  
> **Cổng qua API Gateway**: `http://localhost:8080` (Route prefix: `/api/v1/auth/**` hoặc `/auth/**`)  
> **Đối chiếu quy chuẩn phản hồi**: [docs/quy-chuan-api-response.md](../../docs/quy-chuan-api-response.md)

---

## 1. Tổng Quan Về Quyền Hạn & Headers

### 1.1. Headers Xác Thực
- **Public Endpoints**: Không yêu cầu token (`/api/v1/auth/sso`, `/api/v1/auth/login`, `/api/v1/auth/forgot-password`, `/api/v1/auth/reset-password`).
- **Protected Endpoints**: Yêu cầu header `Authorization: Bearer <JWT_ACCESS_TOKEN>`.
- **Internal Service Endpoints**: Yêu cầu header `X-Internal-Service-Token: <INTERNAL_SECRET_KEY>` để xác thực liên-service (Job Service gọi Auth Service tạo tài khoản HR).

### 1.2. Danh Sách Mã Lỗi Phổ Biến
- `200 OK`: Thao tác thành công.
- `201 Created`: Tạo tài khoản / bản ghi thành công.
- `400 Bad Request`: Dữ liệu đầu vào không hợp lệ hoặc mật khẩu xác nhận không khớp.
- `401 Unauthorized`: Sai thông tin đăng nhập, token hết hạn hoặc không hợp lệ.
- `403 Forbidden`: Tài khoản bị khóa, chưa kích hoạt hoặc không có quyền truy cập.
- `404 Not Found`: Không tìm thấy tài khoản tương ứng với email/token.
- `409 Conflict`: Email hoặc mã HR đã tồn tại trong hệ thống.

---

## 2. Bảng Tổng Hợp Danh Sách API

| STT | Method | Đường dẫn (Qua Gateway) | Đường dẫn (Nội bộ :8081) | Yêu cầu Xác thực | Mô tả |
| :-: | :----: | :---------------------- | :----------------------- | :--------------- | :---- |
| 1 | `POST` | `/api/v1/auth/sso` | `/api/v1/auth/sso` | Public | Đăng nhập một chạm SSO qua vé (ticket) từ LMS |
| 2 | `POST` | `/api/v1/auth/login` | `/api/v1/auth/login` | Public | Đăng nhập tài khoản bằng Email & Mật khẩu |
| 3 | `POST` | `/api/v1/auth/forgot-password` | `/api/v1/auth/forgot-password` | Public | Yêu cầu gửi email đặt lại mật khẩu |
| 4 | `POST` | `/api/v1/auth/reset-password` | `/api/v1/auth/reset-password` | Public | Đặt lại mật khẩu mới qua mã reset token |
| 5 | `POST` | `/api/v1/auth/first-time-password`| `/api/v1/auth/first-time-password`| Bearer Token | Đổi mật khẩu lần đầu bắt buộc cho tài khoản mới |
| 6 | `POST` | `/api/v1/auth/change-password` | `/api/v1/auth/change-password` | Bearer Token | Đổi mật khẩu chủ động (nhập mật khẩu cũ) |
| 7 | `GET` | `/api/v1/auth/me` | `/api/v1/auth/me` | Bearer Token | Lấy thông tin tài khoản và vai trò đang đăng nhập |
| 8 | `POST` | `/api/v1/auth/internal/hr-accounts` | `/api/v1/auth/internal/hr-accounts` | Internal Token | Tạo tài khoản HR từ Job Service (Nội bộ) |
| 9 | `PUT` | `/api/v1/auth/internal/hr-accounts/{id}/status` | `/api/v1/auth/internal/hr-accounts/{id}/status` | Internal Token | Khóa / Mở khóa tài khoản HR (Nội bộ) |
| 10 | `POST`| `/api/v1/auth/internal/hr-accounts/{id}/resend-activation` | `/api/v1/auth/internal/hr-accounts/{id}/resend-activation` | Internal Token | Gửi lại email kích hoạt tài khoản HR (Nội bộ) |

---

## 3. Chi Tiết Từng API

### 3.1. Đăng Nhập SSO LMS (`POST /api/v1/auth/sso`)
- **Mục đích**: Ứng viên / Giảng viên từ hệ thống LMS chuyển sang Rikkei Jobs không cần gõ mật khẩu.
- **Request Body**:
  ```json
  {
    "ticket": "sso-ticket-uuid-hoac-jwt"
  }
  ```
- **Response hiện tại**: *(Raw DTO `AuthResponse` - Vi phạm Envelope)*
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "tokenType": "Bearer",
    "user": {
      "id": "11111111-1111-1111-1111-111111111101",
      "email": "candidate1@rikkeisoft.com",
      "fullName": "Nguyễn Văn An",
      "role": "CANDIDATE",
      "mustChangePassword": false,
      "status": "ACTIVE"
    }
  }
  ```

### 3.2. Đăng Nhập Tiêu Chuẩn (`POST /api/v1/auth/login`)
- **Mục đích**: Đăng nhập bằng tài khoản và mật khẩu thông thường.
- **Request Body**:
  ```json
  {
    "email": "candidate1@rikkeisoft.com",
    "password": "Password@123"
  }
  ```
- **Response hiện tại**: *(Raw DTO `AuthResponse` - Vi phạm Envelope)*
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "tokenType": "Bearer",
    "user": {
      "id": "11111111-1111-1111-1111-111111111101",
      "email": "candidate1@rikkeisoft.com",
      "fullName": "Nguyễn Văn An",
      "role": "CANDIDATE",
      "mustChangePassword": false,
      "status": "ACTIVE"
    }
  }
  ```

### 3.3. Quên Mật Khẩu (`POST /api/v1/auth/forgot-password`)
- **Request Body**:
  ```json
  {
    "email": "candidate1@rikkeisoft.com"
  }
  ```
- **Response hiện tại**: *(Raw DTO `MessageResponse` - Vi phạm Envelope)*
  ```json
  {
    "message": "Nếu email tồn tại trong hệ thống, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu"
  }
  ```

### 3.4. Đổi Mật Khẩu Lần Đầu Bắt Buộc (`POST /api/v1/auth/first-time-password`)
- **Header**: `Authorization: Bearer <JWT_ACCESS_TOKEN>`
- **Request Body**:
  ```json
  {
    "newPassword": "NewSecurePassword@123",
    "confirmPassword": "NewSecurePassword@123"
  }
  ```
- **Response hiện tại**: *(Raw DTO `MessageResponse` - Vi phạm Envelope)*
  ```json
  {
    "message": "Mật khẩu đã được cập nhật thành công"
  }
  ```

### 3.5. Đổi Mật Khẩu Chủ Động (`POST /api/v1/auth/change-password`)
- **Header**: `Authorization: Bearer <JWT_ACCESS_TOKEN>`
- **Request Body**:
  ```json
  {
    "currentPassword": "OldPassword@123",
    "newPassword": "NewSecurePassword@123",
    "confirmPassword": "NewSecurePassword@123"
  }
  ```
- **Response hiện tại**: *(Raw DTO `MessageResponse` - Vi phạm Envelope)*
  ```json
  {
    "message": "Mật khẩu đã được cập nhật thành công"
  }
  ```

### 3.6. Đặt Lại Mật Khẩu Qua Token (`POST /api/v1/auth/reset-password`)
- **Request Body**:
  ```json
  {
    "token": "reset-token-xyz",
    "newPassword": "NewSecurePassword@123",
    "confirmPassword": "NewSecurePassword@123"
  }
  ```
- **Response hiện tại**: *(Raw DTO `MessageResponse` - Vi phạm Envelope)*
  ```json
  {
    "message": "Mật khẩu đã được đặt lại thành công"
  }
  ```

### 3.7. Lấy Thông Tin Người Dùng Hiện Tại (`GET /api/v1/auth/me`)
- **Header**: `Authorization: Bearer <JWT_ACCESS_TOKEN>`
- **Response hiện tại**: *(Raw DTO `UserDto` - Vi phạm Envelope)*
  ```json
  {
    "id": "11111111-1111-1111-1111-111111111101",
    "email": "candidate1@rikkeisoft.com",
    "fullName": "Nguyễn Văn An",
    "role": "CANDIDATE",
    "mustChangePassword": false,
    "status": "ACTIVE"
  }
  ```

### 3.8. Tạo Tài Khoản HR Nội Bộ (`POST /api/v1/auth/internal/hr-accounts`)
- **Header**: `X-Internal-Service-Token: <SECRET>`
- **Request Body**:
  ```json
  {
    "email": "hr.lead@fpt.com",
    "companyId": "33333333-3333-3333-3333-333333333301",
    "hrCode": "HR001",
    "staffRole": "RECRUITER",
    "temporaryPassword": "TempPassword@123"
  }
  ```
- **Response hiện tại**: *(Có bọc `ApiResponse<HrAccountResponse>`)*
  ```json
  {
    "success": true,
    "statusCode": 200,
    "message": "Tạo tài khoản HR thành công",
    "data": {
      "id": "a0000000-0000-0000-0000-000000000001",
      "email": "hr.lead@fpt.com",
      "companyId": "33333333-3333-3333-3333-333333333301",
      "hrCode": "HR001",
      "status": "ACTIVE",
      "createdAt": "2026-11-20T08:00:00Z"
    },
    "timestamp": "2026-11-20T08:00:00.1234567"
  }
  ```

### 3.9. Cập Nhật Trạng Thái HR Nội Bộ (`PUT /api/v1/auth/internal/hr-accounts/{hrAccountId}/status`)
- **Header**: `X-Internal-Service-Token: <SECRET>`
- **Request Body**:
  ```json
  {
    "status": "LOCKED"
  }
  ```
- **Response hiện tại**: *(Có bọc `ApiResponse<HrAccountResponse>`)*
  ```json
  {
    "success": true,
    "statusCode": 200,
    "message": "Cập nhật trạng thái tài khoản HR thành công",
    "data": {
      "id": "a0000000-0000-0000-0000-000000000001",
      "status": "LOCKED"
    },
    "timestamp": "2026-11-20T08:00:00.1234567"
  }
  ```

### 3.10. Gửi Lại Email Kích Hoạt HR Nội Bộ (`POST /api/v1/auth/internal/hr-accounts/{hrAccountId}/resend-activation`)
- **Header**: `X-Internal-Service-Token: <SECRET>`
- **Response hiện tại**: *(Có bọc `ApiResponse<Void>`)*
  ```json
  {
    "success": true,
    "statusCode": 200,
    "message": "Đã gửi lại email kích hoạt tài khoản HR",
    "data": null,
    "timestamp": "2026-11-20T08:00:00.1234567"
  }
  ```

---

## 4. Đánh Giá Đối Chiếu Quy Chuẩn Response (`quy-chuan-api-response.md`)

| Tiêu Chí Đánh Giá | Hiện Trạng Của `auth-service` | Mức Độ Tuân Thủ | Ghi Chú |
| :---------------- | :---------------------------- | :-------------: | :------ |
| **Cấu trúc Envelope 5 trường** | Toàn bộ 100% endpoints trong `AuthController.java` và `HrAccountAdminController.java` đã được bọc `ApiResponse<T>` chuẩn 5 trường (`success`, `statusCode`, `message`, `data`, `timestamp`). | ✅ **Đạt 100%** | Đã được chuẩn hóa thành công trong PR #91. |
| **Xử lý Exception & Error Response**| `GlobalExceptionHandler.java` bắt toàn bộ các lỗi (`SsoException`, `BadCredentialsException`, `LockedException`, `MethodArgumentNotValidException`, etc.) trả về `ApiResponse.error(...)` chuẩn 5 trường. | ✅ **Đạt 100%** | Đã bỏ cấu trúc `ErrorResponse` cũ, đồng bộ hoàn toàn với hệ thống. |
| **Ngôn ngữ thông báo (`message`)** | 100% các câu thông báo thành công và thông báo lỗi đều bằng Tiếng Việt thân thiện, rõ nghĩa. | ✅ **Đạt 100%** | Sẵn sàng hiển thị trực tiếp lên UI Toast/Snackbar. |

