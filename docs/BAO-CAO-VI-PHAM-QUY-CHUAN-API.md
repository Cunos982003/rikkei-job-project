# Báo Cáo Đối Chiếu Quy Chuẩn API Response (Cập Nhật Sau Khi Pull Code Mới Nhất)

> **Dự án**: Rikkei Jobs  
> **Tài liệu quy chuẩn đối chiếu**: [docs/quy-chuan-api-response.md](docs/quy-chuan-api-response.md)  
> **Thời điểm cập nhật**: Tháng 11/2026 (Sau commit `065f2d3` - PR #91 `fix api auth-service`)  
> **Trạng thái**: Đã rà soát 100% mã nguồn toàn bộ các service sau khi kéo code mới nhất từ nhánh `dev`.

---

## 1. Bảng Tổng Hợp Hiện Trạng Tuân Thủ Của Toàn Bộ Các Service

| STT | Service | Cổng | Envelope 5 trường | Cấu trúc Phân trang | Ngôn ngữ Tiếng Việt | Trạng Thái Tuân Thủ | Tình Trạng Sau Khi Pull Code |
| :-: | :------ | :--: | :---------------: | :-----------------: | :-----------------: | :------------------: | :--------------------------- |
| 1 | **`backend/auth-service`** | `:8081` | ✅ **100%** | N/A | ✅ **100%** | **ĐÃ HOÀN TOÀN ĐẠT CHUẨN** | 🎉 **Vừa được sửa thành công trong PR #91** |
| 2 | **`backend/learning-service`** | `:8089` | ✅ **100%** | ✅ Đạt chuẩn | ✅ **100%** | **CHUẨN MỰC XUẤT SẮC** | Đã hoàn thành 100% (DT-01, DT-02, DT-03) |
| 3 | **`ai_career_service`** | `:8090` | ✅ **100%** | N/A | ✅ **100%** | **RẤT TỐT** | Đạt chuẩn 100% |
| 4 | **`backend/recruitment-service`** | `:8086` | ✅ **100%** | ⚠️ Dùng Spring Page | ✅ **100%** | **RẤT TỐT** | Đạt chuẩn Envelope & 100% Tiếng Việt |
| 5 | **`backend/profile-service`** | `:8082` | ⚠️ **85%** (Mất ở Directory) | ⚠️ Chưa chuẩn | ❌ **15%** (Toàn Tiếng Anh)| ❌ **CHƯA SỬA** | Cần Việt hóa & bọc Envelope Directory |
| 6 | **`backend/job-service`** | `:8085` | ❌ **0%** (13/13 API raw) | ❌ Sai chuẩn | ❌ Không có message | ❌ **CHƯA SỬA (Vi phạm nặng nhất)** | Chưa được bọc `ApiResponse<T>` |
| 7 | **`backend/api-gateway`** | `:8080` | ⚠️ WebFlux format | N/A | ⚠️ 50% | ⚠️ Cần chuẩn hóa nhẹ | `GatewayError` chưa theo Envelope |
| 8 | **`mock`** | `:9000` | N/A | N/A | N/A | Hệ thống ngoài | Giữ nguyên theo đối tác LMS, Exam, PM |

---

## 2. Tin Vui: Những Gì Đã Được Sửa Trong Lần Pull Code Vừa Rồi

Trong commit `5a23ff8` (PR #91 `fix api auth-service`):
1. **`backend/auth-service` đã chuyển từ vi phạm nặng sang đạt chuẩn 100%**:
   - `AuthController.java`: Tất cả các endpoint (`/sso`, `/login`, `/forgot-password`, `/change-password`, `/first-time-password`, `/reset-password`, `/me`) đều đã được bọc `ResponseEntity<ApiResponse<T>>`.
   - `GlobalExceptionHandler.java`: Đã bắt toàn bộ exception trả về `ApiResponse.error(...)` chuẩn 5 trường với 100% thông điệp Tiếng Việt.
2. **`EvidenceDecayCalculator.java` (thuộc `profile-service`)**:
   - Đã được bổ sung `@Component` để sửa lỗi thiếu Bean khi khởi động.

---

## 3. Danh Sách Các Service VẪN CHƯA SỬA & Chi Tiết Các Điểm Vi Phạm

---

### DỊCH VỤ 1: `backend/job-service` (Chưa sửa - Vi phạm nghiêm trọng nhất)

Hiện tại `job-service` **chưa có bất kỳ thay đổi nào** về định dạng response.

#### Các điểm vi phạm cụ thể:
1. **Không có Envelope `ApiResponse<T>` (13/13 APIs)**:
   - `JobQueryController.java`:
     - `GET /api/v1/jobs`: Trả về trực tiếp `Map<String, Object>`.
     - `GET /api/v1/jobs/{jobId}`: Trả về trực tiếp `Map<String, Object>`.
   - `CompanyController.java`:
     - `GET /companies/me`: Trả về trực tiếp `CompanyResponse`.
     - `PUT /companies/me`: Trả về trực tiếp `CompanyResponse`.
     - `PUT /companies/{id}`: Trả về trực tiếp `CompanyResponse`.
     - `POST /companies/me/logo`: Trả về trực tiếp `LogoUploadResponse`.
   - `StaffCompanyController.java`:
     - `POST /staff/companies`: Trả về raw `CompanyResponse`.
     - `POST /staff/companies/{id}/hr-accounts`: Trả về raw `HrAccountResponse`.
     - `GET /staff/companies`: Trả về raw `PageResponse<CompanyListItemResponse>`.
     - `GET /staff/companies/{id}`: Trả về raw `StaffCompanyDetailResponse`.
     - `POST /staff/companies/{id}/logo`: Trả về raw `LogoUploadResponse`.
   - `StaffHrAccountController.java`:
     - `POST /staff/hr-accounts/{id}/lock` & `unlock`: Trả về raw `StaffHrAccountResponse`.
2. **Sai chuẩn phân trang**:
   - `GET /api/v1/jobs`: Trả về `{ items, total, page, size }` phẳng ngoài cùng thay vì đặt trong `data.pagination`.
   - `GET /staff/companies`: Dùng DTO riêng `PageResponse` gồm `{ content, page, size, totalElements, totalPages, last }`.
3. **Thiếu thông điệp `message` Tiếng Việt**:
   - Tất cả các endpoint thành công đều không có trường `message` tiếng Việt thân thiện để hiển thị lên Toast của Frontend.
4. **Exception Handling**:
   - Chưa có `@RestControllerAdvice` GlobalExceptionHandler để bọc các lỗi runtime/validation về Envelope 5 trường.

---

### DỊCH VỤ 2: `backend/profile-service` (Chưa sửa - Vi phạm Ngôn ngữ & Candidate Directory)

Mặc dù `profile-service` đã được sửa lỗi Bean `@Component` để khởi động được, nhưng về **quy chuẩn response** thì vẫn **chưa được sửa**.

#### Các điểm vi phạm cụ thể:
1. **Toàn bộ thông điệp trả về là TIẾNG ANH (Vi phạm nguyên tắc 3)**:
   Quy chuẩn yêu cầu 100% `message` bằng Tiếng Việt thân thiện, nhưng các controller hiện tại đang trả về tiếng Anh:
   - `ProfileController.java`:
     - `"Profile retrieved successfully"` ➔ Cần sửa thành `"Lấy thông tin hồ sơ thành công"`
     - `"Profile updated successfully"` ➔ Cần sửa thành `"Cập nhật hồ sơ thành công"`
     - `"Self skills updated successfully"` ➔ Cần sửa thành `"Cập nhật kỹ năng tự khai báo thành công"`
     - `"Profile submitted for review"` ➔ Cần sửa thành `"Hồ sơ đã được gửi để cán bộ phê duyệt"`
     - `"Profile withdrawn successfully"` ➔ Cần sửa thành `"Thu hồi hồ sơ thành công"`
   - `CertificateController.java`:
     - `"Certificate created successfully"` ➔ `"Thêm chứng chỉ thành công"`
     - `"Certificates retrieved successfully"` ➔ `"Lấy danh sách chứng chỉ thành công"`
     - `"Certificate updated successfully"` ➔ `"Cập nhật chứng chỉ thành công"`
     - `"Certificate deleted successfully"` ➔ `"Xóa chứng chỉ thành công"`
   - `EducationController.java`:
     - `"Education created successfully"` ➔ `"Thêm học vấn thành công"`
     - `"Educations retrieved successfully"` ➔ `"Lấy danh sách học vấn thành công"`
     - `"Education updated successfully"` ➔ `"Cập nhật học vấn thành công"`
     - `"Education deleted successfully"` ➔ `"Xóa học vấn thành công"`
   - `StaffProfileController.java`:
     - `"Profile reviews retrieved successfully"` ➔ `"Lấy danh sách hồ sơ chờ duyệt thành công"`
     - `"Profile preview retrieved successfully"` ➔ `"Lấy thông tin xem trước hồ sơ thành công"`
     - `"Self-declared field updated successfully"` ➔ `"Cập nhật thông tin tự khai báo thành công"`
   - `SkillProfileController.java`:
     - `"Skills retrieved successfully"` ➔ `"Lấy danh sách kỹ năng thành công"`
   - `FileController.java`:
     - `"File uploaded successfully"` ➔ `"Tải lên tệp thành công"`
2. **Mất Envelope ở `CandidateDirectoryController.java`**:
   - `GET /api/v1/skills`: Trả về raw `List<Map<String, Object>>` (Chưa bọc `ApiResponse`).
   - `GET /api/v1/candidates`: Trả về raw `Map<String, Object>` phẳng `{ items, total, page, size }`.
   - `GET /api/v1/candidates/{userId}`: Trả về raw `Map<String, Object>`.
3. **Cấu trúc phân trang**:
   - `GET /candidates` và `GET /staff/profile-reviews` chưa đưa thông tin phân trang vào object con `pagination`.

---

### DỊCH VỤ 3: `backend/api-gateway` (Cần chuẩn hóa nhẹ)

- File `backend/api-gateway/src/main/java/com/example/apigateway/security/GatewayError.java`:
  - Khi token JWT hết hạn hoặc không hợp lệ, Gateway trả về format của Spring WebFlux `{ timestamp, path, status, error, message, requestId }`.
  - Cần đổi sang Envelope chuẩn `{ success: false, statusCode: 401, message: "...", data: null, timestamp: "..." }`.

---

## 4. Bảng Tổng Hợp So Sánh Trước & Sau Khi Pull Code

| Service | Trước Khi Pull Code | Sau Khi Pull Code Mới Nhất | Cần Làm Gì Tiếp Theo? |
| :------ | :------------------ | :------------------------- | :--------------------- |
| **`auth-service`** | ❌ Vi phạm Envelope & Error format | ✅ **Đạt chuẩn 100%** | Không cần làm gì thêm, đã xong! |
| **`learning-service`**| ✅ Đạt chuẩn 100% | ✅ **Đạt chuẩn 100%** | Hoàn thành |
| **`ai_career_service`**| ✅ Đạt chuẩn 100% | ✅ **Đạt chuẩn 100%** | Hoàn thành |
| **`recruitment-service`**| ✅ Đạt chuẩn 95% | ✅ **Đạt chuẩn 95%** | Rất tốt (chỉ còn phân trang nhẹ) |
| **`job-service`** | ❌ 100% API vi phạm Envelope | ❌ **VẪN CHƯA SỬA** | **Cần thành viên phụ trách refactor 13 controller endpoints bọc ApiResponse<T>** |
| **`profile-service`**| ⚠️ Tiếng Anh & thiếu bọc Directory| ❌ **VẪN CHƯA SỬA** | **Cần Việt hóa message và bọc ApiResponse ở CandidateDirectoryController** |
| **`api-gateway`** | ⚠️ Format lỗi WebFlux | ⚠️ **VẪN CHƯA SỬA** | Cần đổi GatewayError sang chuẩn 5 trường |

---

> **Kết luận**: Nhờ PR #91, **`auth-service`** đã được chuẩn hóa xuất sắc 100%. Hiện tại trong toàn bộ hệ sinh thái backend, chỉ còn **2 service chính chưa sửa để khớp quy chuẩn** là **`backend/job-service`** và **`backend/profile-service`** (cùng 1 điểm nhỏ tại `api-gateway`).

