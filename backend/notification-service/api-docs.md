# Tài Liệu Đặc Tả API - Notification Service (Thông Báo Hệ Thống & Email)

> **Dự án**: Rikkei Jobs  
> **Service**: `notification-service`  
> **Cổng nội bộ**: `http://localhost:8087`  
> **Cổng qua API Gateway**: `http://localhost:8080` (Route: `/api/services/notification/**`)  
> **Trạng thái dịch vụ**: 🚧 **Skeleton / Stub Service**  
> **Đối chiếu quy chuẩn phản hồi**: [docs/quy-chuan-api-response.md](../../docs/quy-chuan-api-response.md)

---

## 1. Hiện Trạng Triển Khai

Hiện tại, `notification-service` là dịch vụ khung (Skeleton Application). Việc gửi thông báo (email mời nhận việc, email lịch phỏng vấn, email cấp tài khoản HR) đang được xử lý bất đồng bộ hoặc giả lập ghi log trong các service nghiệp vụ (`recruitment-service`, `auth-service`, `job-service`).

---

## 2. Đặc Tả Tuyến API Dự Kiến Triển Khai

| STT | Method | Tuyến Qua Gateway (`:8080`) | Tuyến Nội Bộ (`:8087`) | Vai Trò | Mô Tả Nghiệp Vụ |
| :-: | :----: | :-------------------------- | :--------------------- | :------ | :-------------- |
| 1 | `GET` | `/api/services/notification/me` | `/me` | Bearer Token | Lấy danh sách thông báo in-app của người dùng |
| 2 | `PUT` | `/api/services/notification/{id}/read` | `/{id}/read` | Bearer Token | Đánh dấu thông báo đã đọc |
| 3 | `POST`| `/api/services/notification/email/send` | `/email/send` | Internal Token | Gửi email thông báo cho ứng viên / HR |

---

## 3. Quy Chuẩn Bắt Buộc Khi Triển Khai (`quy-chuan-api-response.md`)

1. Cấu trúc phản hồi Envelope `ApiResponse<T>` 5 trường.
2. Phân trang thông báo theo chuẩn `{ items: [...], pagination: { page, size, totalElements, totalPages, isFirst, isLast } }`.
3. Thông báo tiếng Việt 100%.

