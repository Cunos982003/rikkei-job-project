# Tài Liệu Đặc Tả API - Evidence Service (Bằng Chứng Năng Lực & Minh Chứng Kỹ Năng)

> **Dự án**: Rikkei Jobs  
> **Service**: `evidence-service`  
> **Cổng nội bộ**: `http://localhost:8083`  
> **Cổng qua API Gateway**: `http://localhost:8080` (Route: `/api/services/evidence/**`)  
> **Trạng thái dịch vụ**: 🚧 **Skeleton / Stub Service** (Khung khởi tạo Spring Boot; logic tính toán bằng chứng hiện đang nằm tại `profile-service`)  
> **Đối chiếu quy chuẩn phản hồi**: [docs/quy-chuan-api-response.md](../../docs/quy-chuan-api-response.md)

---

## 1. Hiện Trạng Triển Khai

Hiện tại, `evidence-service` là dịch vụ khung (Skeleton Application). Nghiệp vụ lưu trữ và tính toán bằng chứng năng lực (Evidence & Scoring) từ LMS, Dự án thực tập PM và Khảo thí đang được xử lý tập trung bên trong `backend/profile-service` (thông qua bảng `skill_evidences`, `portfolio_projects`, và endpoint `/api/v1/profiles/internal/lms-evidence`).

---

## 2. Đặc Tả Tuyến API Dự Kiến Bóc Tách

Khi tách độc lập microservice, các endpoint quản lý minh chứng sẽ bao gồm:

| STT | Method | Tuyến Qua Gateway (`:8080`) | Tuyến Nội Bộ (`:8083`) | Vai Trò | Mô Tả Nghiệp Vụ |
| :-: | :----: | :-------------------------- | :--------------------- | :------ | :-------------- |
| 1 | `GET` | `/api/services/evidence/users/{id}/skills/{skillId}` | `/users/{id}/skills/{skillId}` | Protected | Xem chi tiết các nguồn bằng chứng cấu thành điểm kỹ năng |
| 2 | `POST`| `/api/services/evidence/records` | `/records` | Internal | Tiếp nhận bản ghi bằng chứng từ LMS, PM, Exam |

---

## 3. Quy Chuẩn Bắt Buộc Khi Triển Khai (`quy-chuan-api-response.md`)

Khi xây dựng controller cho service, bắt buộc tuân thủ:
1. Envelope pattern `ApiResponse<T>` 5 trường chuẩn (`success`, `statusCode`, `message`, `data`, `timestamp`).
2. Thông báo 100% tiếng Việt thân thiện với người dùng.

