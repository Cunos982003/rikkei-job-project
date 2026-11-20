# Tài Liệu Đặc Tả API - Audit Service (Nhật Ký Kiểm Toán & Truy Vết Hoạt Động)

> **Dự án**: Rikkei Jobs  
> **Service**: `audit-service`  
> **Cổng nội bộ**: `http://localhost:8088`  
> **Cổng qua API Gateway**: `http://localhost:8080` (Route: `/api/services/audit/**`)  
> **Trạng thái dịch vụ**: 🚧 **Skeleton / Stub Service**  
> **Đối chiếu quy chuẩn phản hồi**: [docs/quy-chuan-api-response.md](../../docs/quy-chuan-api-response.md)

---

## 1. Hiện Trạng Triển Khai

Hiện tại, `audit-service` là dịch vụ khung (Skeleton Application). Các bảng lưu trữ lịch sử kiểm duyệt (`profile_reviews`, `recruitment_timeline`, `staff_audit_logs`) hiện đang được lưu trực tiếp tại cơ sở dữ liệu của từng service nghiệp vụ tương ứng (`profile_db`, `recruitment_db`, `job_db`).

---

## 2. Đặc Tả Tuyến API Dự Kiến Triển Khai

| STT | Method | Tuyến Qua Gateway (`:8080`) | Tuyến Nội Bộ (`:8088`) | Vai Trò | Mô Tả Nghiệp Vụ |
| :-: | :----: | :-------------------------- | :--------------------- | :------ | :-------------- |
| 1 | `GET` | `/api/services/audit/logs` | `/logs` | `ADMIN` | Tra cứu nhật ký truy cập và thao tác nhạy cảm của cán bộ |
| 2 | `POST`| `/api/services/audit/events` | `/events` | Internal Token | Nhận sự kiện kiểm toán bất đồng bộ từ các microservice |

---

## 3. Quy Chuẩn Bắt Buộc Khi Triển Khai (`quy-chuan-api-response.md`)

1. Cấu trúc phản hồi Envelope `ApiResponse<T>` 5 trường chuẩn.
2. Cấu trúc phân trang `{ items, pagination }`.
3. Mã lỗi và thông báo bằng Tiếng Việt.

