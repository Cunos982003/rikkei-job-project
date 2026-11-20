# Tài Liệu Đặc Tả API - Profile Service (Hồ Sơ Năng Lực & Kỹ Năng Ứng Viên)

> **Dự án**: Rikkei Jobs  
> **Service**: `profile-service`  
> **Cổng nội bộ**: `http://localhost:8082`  
> **Cổng qua API Gateway**: `http://localhost:8080` (Route: `/api/v1/profiles/**`, `/api/v1/files/**`, `/api/v1/candidates/**`, `/api/v1/skills`, `/api/v1/staff/**`)  
> **Đối chiếu quy chuẩn phản hồi**: [docs/quy-chuan-api-response.md](../../docs/quy-chuan-api-response.md)

---

## 1. Tổng Quan Về Quyền Hạn & Headers

### 1.1. Headers Xác Thực
- **Public**: `/api/v1/skills`, `/api/v1/candidates` (không bắt buộc token; hỗ trợ xem ứng viên ở trạng thái `PUBLIC` hoặc `RECRUITER`).
- **Ứng viên (`CANDIDATE`)**: Yêu cầu header `X-User-Id` (do Gateway trích xuất từ JWT).
- **Cán bộ (`STAFF`, `ADMIN`)**: Yêu cầu header `X-User-Id` và `X-User-Role` (`STAFF` hoặc `ADMIN`).
- **Tích hợp LMS / Nội bộ**: Endpoint `/api/v1/profiles/internal/lms-evidence` nhận dữ liệu hoàn thành khóa học từ hệ thống LMS.

---

## 2. Bảng Tổng Hợp Danh Sách API

| STT | Method | Đường dẫn (Qua Gateway :8080) | Vai trò yêu cầu | Mô tả |
| :-: | :----: | :---------------------------- | :-------------- | :---- |
| 1 | `GET` | `/api/v1/skills` | Public | Danh mục 20 kỹ năng chuẩn của hệ thống |
| 2 | `GET` | `/api/v1/candidates` | Public / Recruiter | Danh sách ứng viên tìm kiếm (có lọc từ khóa, phân trang) |
| 3 | `GET` | `/api/v1/candidates/{userId}` | Public / Recruiter | Xem chi tiết hồ sơ công khai của ứng viên |
| 4 | `GET` | `/api/v1/profiles/me` | `CANDIDATE` | Lấy toàn bộ thông tin hồ sơ cá nhân của mình |
| 5 | `PUT` | `/api/v1/profiles/me` | `CANDIDATE` | Cập nhật thông tin cá nhân (họ tên, phone, bio, vị trí...) |
| 6 | `PUT` | `/api/v1/profiles/me/self-skills` | `CANDIDATE` | Tự khai báo danh sách kỹ năng cá nhân |
| 7 | `POST` | `/api/v1/profiles/me/publish` | `CANDIDATE` | Gửi hồ sơ để cán bộ kiểm duyệt (`PENDING`) |
| 8 | `POST` | `/api/v1/profiles/me/withdraw` | `CANDIDATE` | Rút lại hồ sơ đã công khai hoặc đang chờ duyệt (`DRAFT`) |
| 9 | `GET` | `/api/v1/profiles/me/review-history`| `CANDIDATE` | Xem lịch sử các lần cán bộ phê duyệt / từ chối |
| 10 | `GET` | `/api/v1/profiles/me/skills` | `CANDIDATE` | Lấy bảng điểm kỹ năng tính toán từ bằng chứng LMS/PM |
| 11 | `GET` | `/api/v1/profiles/{userId}/skills` | Protected | Xem bảng điểm kỹ năng của ứng viên khác (nếu được phép) |
| 12 | `POST` | `/api/v1/profiles/me/certificates` | `CANDIDATE` | Thêm mới chứng chỉ / bằng cấp |
| 13 | `GET` | `/api/v1/profiles/me/certificates` | `CANDIDATE` | Danh sách chứng chỉ của ứng viên |
| 14 | `GET` | `/api/v1/profiles/me/certificates/{id}` | `CANDIDATE` | Chi tiết một chứng chỉ |
| 15 | `PUT` | `/api/v1/profiles/me/certificates/{id}` | `CANDIDATE` | Cập nhật thông tin chứng chỉ |
| 16 | `DELETE`| `/api/v1/profiles/me/certificates/{id}` | `CANDIDATE` | Xóa chứng chỉ |
| 17 | `POST` | `/api/v1/profiles/me/educations` | `CANDIDATE` | Thêm quá trình học vấn |
| 18 | `GET` | `/api/v1/profiles/me/educations` | `CANDIDATE` | Danh sách học vấn của ứng viên |
| 19 | `GET` | `/api/v1/profiles/me/educations/{id}` | `CANDIDATE` | Chi tiết một mục học vấn |
| 20 | `PUT` | `/api/v1/profiles/me/educations/{id}` | `CANDIDATE` | Cập nhật mục học vấn |
| 21 | `DELETE`| `/api/v1/profiles/me/educations/{id}` | `CANDIDATE` | Xóa mục học vấn |
| 22 | `GET` | `/api/v1/profiles/me/internships` | `CANDIDATE` | Đồng bộ và lấy dữ liệu thực tập dự án từ `lms-pm` |
| 23 | `GET` | `/api/v1/profiles/{userId}/internships` | Protected | Xem lịch sử thực tập của ứng viên |
| 24 | `POST` | `/api/v1/files` | Protected | Tải lên tệp đính kèm (CV, PDF, ảnh đại diện) |
| 25 | `GET` | `/api/v1/files/{storedName}` | Public | Xem / Tải file nhị phân theo tên đã lưu trữ |
| 26 | `GET` | `/api/v1/staff/profile-reviews` | `STAFF`, `ADMIN` | Cán bộ xem danh sách hồ sơ chờ duyệt (phân trang, lọc) |
| 27 | `GET` | `/api/v1/staff/profiles/{userId}/preview` | `STAFF`, `ADMIN` | Cán bộ xem trước hồ sơ đầy đủ trước khi duyệt |
| 28 | `PUT` | `/api/v1/staff/profiles/{userId}/self-declared` | `STAFF`, `ADMIN` | Cán bộ sửa đổi thông tin tự khai báo nếu có sai lệch |
| 29 | `POST` | `/api/v1/staff/profiles/{userId}/approve` | `STAFF`, `ADMIN` | Cán bộ phê duyệt hồ sơ công khai |
| 30 | `POST` | `/api/v1/staff/profiles/{userId}/reject` | `STAFF`, `ADMIN` | Cán bộ từ chối hồ sơ kèm lý do |
| 31 | `POST` | `/api/v1/profiles/internal/lms-evidence` | Internal | Nhận bằng chứng điểm số LMS để tính toán lại điểm kỹ năng |

---

## 3. Chi Tiết Một Số API Điển Hình & Input/Output

### 3.1. Danh Sách Ứng Viên (`GET /api/v1/candidates`)
- **Query Params**: `keyword` (String), `page` (default: 0), `size` (default: 50).
- **Response hiện tại**: *(Raw JSON Map - Vi phạm Envelope)*
  ```json
  {
    "items": [
      {
        "id": "11111111-1111-1111-1111-111111111101",
        "userId": "11111111-1111-1111-1111-111111111101",
        "fullName": "Nguyễn Văn An",
        "email": "candidate1@rikkeisoft.com",
        "phone": "0912345678",
        "bio": "Lập trình viên Java nhiệt huyết...",
        "targetPosition": "Java Backend Developer",
        "visibility": "PUBLIC",
        "skills": [
          { "code": "JAVA_CORE", "name": "Java Core", "score": 85, "level": "PROFICIENT" }
        ]
      }
    ],
    "total": 1,
    "page": 0,
    "size": 50
  }
  ```

### 3.2. Xem Hồ Sơ Cá Nhân (`GET /api/v1/profiles/me`)
- **Header**: `X-User-Id: <UUID>`
- **Response hiện tại**: *(Chuẩn Envelope `ApiResponse<T>` 5 trường)*
  ```json
  {
    "success": true,
    "statusCode": 200,
    "message": "Profile retrieved successfully",
    "data": {
      "id": "b0000000-0000-0000-0000-000000000001",
      "userId": "11111111-1111-1111-1111-111111111101",
      "fullName": "Nguyễn Văn An",
      "email": "candidate1@rikkeisoft.com",
      "visibility": "PUBLIC",
      "avatarUrl": "/api/v1/files/avatar-001.png"
    },
    "timestamp": "2026-11-20T08:00:00.1234567"
  }
  ```

### 3.3. Tải Lên Tệp CV / Avatar (`POST /api/v1/files`)
- **Body**: Form-data key `file` (MultipartFile)
- **Response hiện tại**:
  ```json
  {
    "success": true,
    "statusCode": 200,
    "message": "File uploaded successfully",
    "data": {
      "url": "/api/v1/files/b4c81a29-cv-an.pdf"
    },
    "timestamp": "2026-11-20T08:00:00.1234567"
  }
  ```

---

## 4. Đánh Giá Đối Chiếu Quy Chuẩn Response (`quy-chuan-api-response.md`)

| Tiêu Chí Đánh Giá | Hiện Trạng Của `profile-service` | Mức Độ Tuân Thủ | Ghi Chú |
| :---------------- | :------------------------------- | :-------------: | :------ |
| **Cấu trúc Envelope 5 trường** | Đa số các controller (`ProfileController`, `SkillProfileController`, `CertificateController`, `EducationController`, `StaffProfileController`, `FileController`, `InternshipController`) **đều bọc Envelope `ApiResponse<T>` chuẩn 5 trường**. | ⚠️ **Tuân thủ 80%** | Riêng `CandidateDirectoryController.java` (`/api/v1/skills`, `/api/v1/candidates`, `/api/v1/candidates/{id}`) lại **trả về raw Map / raw List**, không bọc Envelope. |
| **Cấu trúc Phân trang** | `GET /candidates` trả về raw `{ items, total, page, size }`, `GET /staff/profile-reviews` trả về `ApiResponse<Page<...>>` của Spring Data. | ⚠️ **Cần chuẩn hóa** | Chưa đồng bộ trường `pagination: { page, size, totalElements, totalPages, isFirst, isLast }`. |
| **Ngôn ngữ thông báo (`message`)** | Đại đa số message trả về bằng **Tiếng Anh** (ví dụ: `"Profile retrieved successfully"`, `"Certificates retrieved successfully"`, `"Self skills updated successfully"`). | ❌ **Vi phạm quy tắc ngôn ngữ** | Quy chuẩn yêu cầu 100% tiếng Việt thân thiện với người dùng để hiển thị trực tiếp lên giao diện UI Toast/Alert. |

