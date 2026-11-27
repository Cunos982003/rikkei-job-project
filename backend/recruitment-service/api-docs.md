# Tài Liệu Đặc Tả API - Recruitment Service (Quy Trình Tuyển Dụng & Phỏng Vấn)

> **Dự án**: Rikkei Jobs  
> **Service**: `recruitment-service`  
> **Cổng nội bộ**: `http://localhost:8086`  
> **Cổng qua API Gateway**: `http://localhost:8080` (Route: `/api/v1/jobs/*/recruit`, `/api/v1/me/recruitments/**`, `/api/v1/recruitments/**`, `/api/v1/rounds/**`, `/api/v1/me/interviews/**`, `/api/v1/companies/me/interviews`, `/api/v1/hr/applications/**`, `/api/v1/me/offers/**`)  
> **Đối chiếu quy chuẩn phản hồi**: [docs/quy-chuan-api-response.md](../../docs/quy-chuan-api-response.md)

---

## 1. Tổng Quan Về Quyền Hạn & Headers

### 1.1. Headers Xác Thực
- Mọi endpoint của service đều yêu cầu xác thực người dùng qua JWT Bearer token:
  `Authorization: Bearer <JWT_ACCESS_TOKEN>`
- Các vai trò chính:
  - **`CANDIDATE`**: Nộp hồ sơ ứng tuyển, xem danh sách đơn đã nộp, chọn khung giờ phỏng vấn, nhận/từ chối offer, rút đơn.
  - **`HR`**: Quản lý các đơn ứng tuyển của công ty, chuyển trạng thái tuyển dụng, đề xuất khung giờ phỏng vấn, gửi offer, xác nhận onboard.
  - **`STAFF` / `ADMIN`**: Tra cứu và hỗ trợ tiến trình tuyển dụng.

---

## 2. Bảng Tổng Hợp Danh Sách API

| STT | Method | Đường dẫn (Qua Gateway :8080) | Vai trò yêu cầu | Mô tả |
| :-: | :----: | :---------------------------- | :-------------- | :---- |
| 1 | `POST` | `/api/v1/jobs/{id}/recruit` hoặc `/apply` | `CANDIDATE` | Ứng viên nộp hồ sơ ứng tuyển vào vị trí việc làm (tự động tạo CV Snapshot) |
| 2 | `GET` | `/api/v1/me/recruitments` | `CANDIDATE` | Xem danh sách các công việc đã ứng tuyển kèm trạng thái hiện tại |
| 3 | `GET` | `/api/v1/me/recruitments/{id}` | `CANDIDATE` | Xem chi tiết tiến trình một đơn ứng tuyển |
| 4 | `POST` | `/api/v1/recruitments/{id}/withdraw` | `CANDIDATE` | Ứng viên tự rút đơn ứng tuyển kèm lý do |
| 5 | `GET` | `/api/v1/recruitments/{id}` hoặc `/applications/{id}` | `HR`, `STAFF` | HR xem chi tiết đánh giá ứng viên (kèm điểm phù hợp, kỹ năng) |
| 6 | `POST` | `/api/v1/recruitments/{id}/transition` | `HR`, `STAFF` | HR chuyển trạng thái tuyển dụng (vòng 1, vòng 2, offer, từ chối...) |
| 7 | `POST` | `/api/v1/recruitments/{id}/rounds` | `HR`, `STAFF` | Tạo vòng phỏng vấn cho ứng viên |
| 8 | `PUT` | `/api/v1/rounds/{roundId}` | `HR`, `STAFF` | Cập nhật thông tin vòng phỏng vấn (tên vòng, hình thức, người phỏng vấn) |
| 9 | `DELETE`| `/api/v1/rounds/{roundId}` | `HR`, `STAFF` | Xóa/hủy vòng phỏng vấn |
| 10 | `GET` | `/api/v1/recruitments/{id}/cv-snapshot` | `HR`, `CANDIDATE` | Tải xuống bản chụp PDF hồ sơ năng lực lúc nộp đơn (file nhị phân) |
| 11 | `POST` | `/api/v1/rounds/{roundId}/slots` | `HR` | Doanh nghiệp đề xuất danh sách khung giờ phỏng vấn |
| 12 | `GET` | `/api/v1/rounds/{roundId}/slots` | `HR`, `CANDIDATE` | Xem danh sách các khung giờ phỏng vấn đã đề xuất |
| 13 | `POST` | `/api/v1/rounds/{roundId}/reschedule` | `HR`, `CANDIDATE` | Yêu cầu đổi lịch phỏng vấn và đề xuất các khung giờ mới |
| 14 | `GET` | `/api/v1/me/interviews` | `CANDIDATE` | Ứng viên xem danh sách các buổi phỏng vấn của mình |
| 15 | `POST` | `/api/v1/me/interviews/{roundId}/pick`| `CANDIDATE` | Ứng viên chọn khung giờ phỏng vấn phù hợp (tạo link Meet tự động) |
| 16 | `GET` | `/api/v1/companies/me/interviews` | `HR` | Doanh nghiệp xem toàn bộ lịch phỏng vấn của công ty (lọc theo khoảng thời gian) |
| 17 | `GET` | `/api/v1/hr/applications` | `HR` | Doanh nghiệp xem danh sách ứng viên đã nộp đơn vào các tin tuyển dụng của mình |
| 18 | `POST` | `/api/v1/applications/{id}/offer` | `HR` | Doanh nghiệp phát hành thư mời nhận việc (Offer) chính thức |
| 19 | `GET` | `/api/v1/me/offers` | `CANDIDATE` | Ứng viên xem danh sách các offer nhận việc |
| 20 | `GET` | `/api/v1/me/offers/{id}` | `CANDIDATE` | Xem chi tiết chế độ, lương thưởng của thư mời nhận việc |
| 21 | `POST` | `/api/v1/me/offers/{id}/accept` | `CANDIDATE` | Ứng viên đồng ý chấp nhận offer |
| 22 | `POST` | `/api/v1/me/offers/{id}/decline` | `CANDIDATE` | Ứng viên từ chối offer kèm lý do |
| 23 | `POST` | `/api/v1/applications/{id}/offer/confirm-onboard` | `HR` | Doanh nghiệp xác nhận ứng viên đã chính thức đi làm |

---

## 3. Chi Tiết Một Số API & Dữ Liệu Input/Output

### 3.1. Ứng Viên Nộp Đơn Ứng Tuyển (`POST /api/v1/jobs/{id}/recruit`)
- **Headers**: `Authorization: Bearer <TOKEN>`
- **Request Body**:
  ```json
  {
    "coverLetter": "Em rất quan tâm đến vị trí Java Developer của công ty...",
    "customCvUrl": "/api/v1/files/my-tailored-cv.pdf"
  }
  ```
- **Response mẫu**: *(Chuẩn Envelope `ApiResponse<T>` - HTTP 201 Created)*
  ```json
  {
    "success": true,
    "statusCode": 201,
    "message": "Nộp đơn ứng tuyển thành công",
    "data": {
      "id": "e0000000-0000-0000-0000-000000000001",
      "jobId": "44444444-4444-4444-4444-444444444401",
      "candidateId": "11111111-1111-1111-1111-111111111101",
      "status": "APPLIED",
      "appliedAt": "2026-11-20T08:30:00Z",
      "matchScore": 88
    },
    "timestamp": "2026-11-20T08:30:00.1234567"
  }
  ```

### 3.2. Chuyển Trạng Thái Tuyển Dụng (`POST /api/v1/recruitments/{id}/transition`)
- **Request Body**:
  ```json
  {
    "toStatus": "INTERVIEW_ROUND_1",
    "note": "Hồ sơ năng lực đạt yêu cầu, tiến hành phỏng vấn kỹ thuật"
  }
  ```
- **Response mẫu**:
  ```json
  {
    "success": true,
    "statusCode": 200,
    "message": "Chuyển trạng thái tuyển dụng thành công",
    "data": {
      "id": "e0000000-0000-0000-0000-000000000001",
      "status": "INTERVIEW_ROUND_1",
      "updatedAt": "2026-11-20T09:00:00Z"
    },
    "timestamp": "2026-11-20T09:00:00.1234567"
  }
  ```

### 3.3. Doanh Nghiệp Gửi Thư Mời Nhận Việc (`POST /api/v1/applications/{id}/offer`)
- **Request Body**:
  ```json
  {
    "salary": 15000000,
    "startDate": "2026-12-01",
    "expirationDate": "2026-11-27",
    "benefits": "Thưởng hiệu quả dự án, bảo hiểm sức khỏe VIP, phụ cấp ăn trưa",
    "note": "Chào mừng bạn gia nhập Rikkeisoft!"
  }
  ```
- **Response mẫu**: *(HTTP 201 Created)*
  ```json
  {
    "success": true,
    "statusCode": 201,
    "message": "Gửi thư mời nhận việc (Offer) thành công",
    "data": {
      "offerId": "f0000000-0000-0000-0000-000000000001",
      "applicationId": "e0000000-0000-0000-0000-000000000001",
      "status": "OFFER_SENT",
      "salary": 15000000,
      "startDate": "2026-12-01"
    },
    "timestamp": "2026-11-20T09:15:00.1234567"
  }
  ```

---

## 4. Đánh Giá Đối Chiếu Quy Chuẩn Response (`quy-chuan-api-response.md`)

| Tiêu Chí Đánh Giá | Hiện Trạng Của `recruitment-service` | Mức Độ Tuân Thủ | Ghi Chú |
| :---------------- | :----------------------------------- | :-------------: | :------ |
| **Cấu trúc Envelope 5 trường** | 100% tất cả các endpoints trả về JSON đều bọc `ApiResponse<T>` chuẩn 5 trường (`success`, `statusCode`, `message`, `data`, `timestamp`). | ✅ **Đạt 100%** | Tuân thủ tuyệt đối quy chuẩn kiến trúc. |
| **Ngôn ngữ thông báo (`message`)** | 100% các câu thông báo đều bằng **Tiếng Việt thân thiện** (ví dụ: *"Nộp đơn ứng tuyển thành công"*, *"Xác nhận lịch phỏng vấn thành công"*, *"Gửi thư mời nhận việc (Offer) thành công"*). | ✅ **Đạt 100%** | Sẵn sàng hiển thị trực tiếp lên UI Toast/Snackbar. |
| **Ngoại lệ tải tệp nhị phân** | Endpoint `GET /recruitments/{id}/cv-snapshot` trả về `byte[]` nhị phân kèm header `Content-Type: application/pdf`. | ✅ **Hợp lệ** | Đúng theo ngoại lệ được nêu trong mục 4 của quy chuẩn cho stream file/PDF. |
| **Cấu trúc Phân trang** | `GET /hr/applications` trả về `ApiResponse<Page<...>>`. | ⚠️ **Cần chuẩn hóa nhẹ** | Dữ liệu `data` đang là Page object của Spring Framework thay vì object `{ items, pagination }`. |

