# Tài Liệu Đặc Tả API - AI Career Service (Gợi Ý Việc Làm & Chấm Điểm Phù Hợp)

> **Dự án**: Rikkei Jobs  
> **Service**: `ai_career_service`  
> **Cổng nội bộ**: `http://localhost:8090`  
> **Cổng qua API Gateway**: `http://localhost:8080` (Route: `/me/job-recommendations` hoặc `/ai-career/**`)  
> **Đối chiếu quy chuẩn phản hồi**: [docs/quy-chuan-api-response.md](../docs/quy-chuan-api-response.md)

---

## 1. Tổng Quan Về Quyền Hạn & Headers

### 1.1. Headers Xác Thực
- **API Ứng viên (`/me/job-recommendations`)**: Yêu cầu xác thực Bearer Token qua API Gateway. Gateway giải mã UUID ứng viên thành Principal.
- **API Tính điểm phù hợp Nội bộ (`/api/v1/recruitments/.../match-score`)**: Yêu cầu header bí mật `X-Internal-Service-Token: <INTERNAL_SECRET_TOKEN>` để chỉ cho phép `recruitment-service` hoặc các service nội bộ gọi sang.

---

## 2. Bảng Tổng Hợp Danh Sách API

| STT | Method | Đường dẫn (Qua Gateway :8080) | Đường dẫn (Nội bộ :8090) | Xác thực yêu cầu | Mô tả |
| :-: | :----: | :---------------------------- | :----------------------- | :--------------- | :---- |
| 1 | `GET` | `/me/job-recommendations` hoặc `/ai-career/me/job-recommendations` | `/me/job-recommendations` | Bearer Token (`CANDIDATE`) | Lấy danh sách việc làm được AI gợi ý dựa trên hồ sơ và kỹ năng của ứng viên |
| 2 | `GET` | *(Không mở qua Gateway)* | `/api/v1/recruitments/{userId}/jobs/{jobId}/match-score` | `X-Internal-Service-Token` | Tính điểm phù hợp (%) giữa ứng viên và công việc (Bọc `ApiResponse`) |
| 3 | `GET` | *(Không mở qua Gateway)* | `/internal/v1/recruitments/{userId}/jobs/{jobId}/match-score` | `X-Internal-Service-Token` | Tính điểm phù hợp (%) trả về raw DTO cho Feign client |

---

## 3. Chi Tiết Từng API & Dữ Liệu Input/Output

### 3.1. Gợi Ý Việc Làm Cá Nhân Hóa (`GET /me/job-recommendations`)
- **Query Params**:
  - `limit` (Integer, default: 10, min: 1, max: 50): Số lượng việc làm gợi ý tối đa.
- **Response mẫu**: *(Chuẩn Envelope `ApiResponse<T>` 5 trường)*
  ```json
  {
    "success": true,
    "statusCode": 200,
    "message": "Lấy danh sách gợi ý việc làm thành công",
    "data": [
      {
        "jobId": "44444444-4444-4444-4444-444444444401",
        "jobTitle": "Java Backend Developer (Fresher/Junior)",
        "companyName": "Công ty Cổ phần Rikkeisoft",
        "workLocation": "Hà Nội",
        "matchScore": 88,
        "matchReason": "Kỹ năng Java Core (85đ) và Spring Boot (80đ) rất phù hợp với yêu cầu tuyển dụng.",
        "salaryMin": 10000000,
        "salaryMax": 18000000
      }
    ],
    "timestamp": "2026-11-20T08:00:00.1234567"
  }
  ```

### 3.2. Chấm Điểm Phù Hợp Giữa Ứng Viên Và Việc Làm (`GET /api/v1/recruitments/{userId}/jobs/{jobId}/match-score`)
- **Headers**: `X-Internal-Service-Token: <SECRET_KEY>`
- **Path Variables**:
  - `userId` (UUID): ID ứng viên
  - `jobId` (UUID): ID tin tuyển dụng
- **Response mẫu**:
  ```json
  {
    "success": true,
    "statusCode": 200,
    "message": "Lấy điểm phù hợp thành công",
    "data": {
      "userId": "11111111-1111-1111-1111-111111111101",
      "jobId": "44444444-4444-4444-4444-444444444401",
      "matchScore": 88
    },
    "timestamp": "2026-11-20T08:00:00.1234567"
  }
  ```

---

## 4. Đánh Giá Đối Chiếu Quy Chuẩn Response (`quy-chuan-api-response.md`)

| Tiêu Chí Đánh Giá | Hiện Trạng Của `ai_career_service` | Mức Độ Tuân Thủ | Ghi Chú |
| :---------------- | :--------------------------------- | :-------------: | :------ |
| **Cấu trúc Envelope 5 trường** | Sử dụng lớp `ApiResponse<T>` chuẩn 5 trường (`success`, `statusCode`, `message`, `data`, `timestamp`) cho các API public và external. | ✅ **Đạt 100%** | Tuân thủ tuyệt đối quy chuẩn. |
| **Ngôn ngữ thông báo (`message`)** | 100% thông báo trả về bằng **Tiếng Việt** (ví dụ: *"Lấy danh sách gợi ý việc làm thành công"*, *"Lấy điểm phù hợp thành công"*). | ✅ **Đạt 100%** | Đúng yêu cầu thân thiện với người dùng. |
| **Endpoint nội bộ raw** | Endpoint `/internal/v1/.../match-score` trả về raw DTO. | ✅ **Hợp lệ** | Dành riêng cho giao tiếp inter-service giữa các microservice qua Spring Cloud OpenFeign. |

