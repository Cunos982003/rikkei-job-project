# Tài Liệu Đặc Tả API - Assessment Service (Khảo Thí & Sát Hạch Năng Lực)

> **Dự án**: Rikkei Jobs  
> **Service**: `assessment-service`  
> **Cổng nội bộ**: `http://localhost:8084`  
> **Cổng qua API Gateway**: `http://localhost:8080` (Route: `/api/services/assessment/**`)  
> **Trạng thái dịch vụ**: 🚧 **Skeleton / Stub Service** (Khung khởi tạo Spring Boot, đang được giả lập bởi `mock-server` cổng 9000)  
> **Đối chiếu quy chuẩn phản hồi**: [docs/quy-chuan-api-response.md](../../docs/quy-chuan-api-response.md)

---

## 1. Hiện Trạng Triển Khai

Hiện tại, `assessment-service` là dịch vụ khung (Skeleton Application). Các nghiệp vụ liên quan đến kết quả thi sát hạch lý thuyết và thực hành đang được kết nối và mô phỏng thông qua Mock Server (`/exam/**` cổng 9000) hoặc tích hợp trực tiếp qua hệ thống Khảo thí.

---

## 2. Đặc Tả Tuyến API Dự Kiến Triển Khai

Khi hoàn thiện controller nội bộ, service sẽ cung cấp các endpoint sau tuân thủ Gateway routing:

| STT | Method | Tuyến Qua Gateway (`:8080`) | Tuyến Nội Bộ (`:8084`) | Vai Trò | Mô Tả Nghiệp Vụ |
| :-: | :----: | :-------------------------- | :--------------------- | :------ | :-------------- |
| 1 | `GET` | `/api/services/assessment/candidates/{id}/results` | `/candidates/{id}/results` | `CANDIDATE`, `STAFF` | Lấy lịch sử và kết quả các kỳ thi sát hạch của ứng viên |
| 2 | `POST`| `/api/services/assessment/exams/sync` | `/exams/sync` | `ADMIN`, System | Đồng bộ điểm thi chính thức từ phân hệ Khảo thí |

---

## 3. Quy Chuẩn Bắt Buộc Khi Triển Khai (`quy-chuan-api-response.md`)

Mọi Controller khi bổ sung vào `assessment-service` **bắt buộc** phải tuân thủ:
1. Đóng gói 100% dữ liệu trả về qua Envelope `ApiResponse<T>`:
   ```json
   {
     "success": true,
     "statusCode": 200,
     "message": "Lấy kết quả khảo thí thành công",
     "data": { ... },
     "timestamp": "2026-11-20T08:00:00.1234567"
   }
   ```
2. Thông điệp `message` viết bằng Tiếng Việt thân thiện.
3. Toàn bộ exception phải bắt qua `@RestControllerAdvice` trả về cấu trúc lỗi chuẩn 5 trường.

