# Tài Liệu Đặc Tả API - Mock Server (Giả Lập Hệ Thống Bên Thứ Ba & Mẫu E2E)

> **Dự án**: Rikkei Jobs  
> **Service**: `mock` (Node.js Server)  
> **Cổng nội bộ**: `http://localhost:9000`  
> **Mục đích**: Giả lập các hệ sinh thái bên ngoài của Rikkei (LMS, Khảo thí Sát hạch, Dự án Thực tập IOC lms-pm, Rikkei Meet) và cung cấp bộ dữ liệu mẫu cho kiểm thử tích hợp E2E.  
> **Đối chiếu quy chuẩn phản hồi**: [docs/quy-chuan-api-response.md](../docs/quy-chuan-api-response.md)

---

## 1. Cơ Chế Xác Thực & Quy Định Khóa Dịch Vụ (`X-Service-Key`)

Mọi endpoint giả lập hệ thống ngoài (`/lms/*`, `/exam/*`, `/pm/*`, `/meet/*`) **bắt buộc** phải truyền header:
```http
X-Service-Key: fbc368ffa97a999a71577c552ecc2546815f245fd4ebee014732afcba1b52870
```
- **Nếu thiếu hoặc sai header**: Trả về `HTTP 401 Unauthorized`:
  ```json
  {
    "code": 401,
    "message": "Khóa dịch vụ không hợp lệ hoặc bị thiếu (yêu cầu header X-Service-Key)"
  }
  ```
- **Nếu hợp lệ**: Trả về `HTTP 200 OK` cùng dữ liệu giả lập.

---

## 2. Bảng Tổng Hợp Danh Sách API Giả Lập Hệ Thống Ngoài

| Phân Hệ | Method | Endpoint | Headers Yêu Cầu | Dữ Liệu Input Mẫu | Mô Tả Nghiệp Vụ |
| :------ | :----: | :------- | :-------------- | :---------------- | :-------------- |
| **LMS** | `POST` | `/lms/sso/tickets` | `X-Service-Key` | `{"lmsUserId": "std-001"}` | Sinh mã vé SSO đăng nhập một chạm |
| **LMS** | `POST` | `/lms/sso/verify` | `X-Service-Key` | `{"ticket": "..."}` | Xác thực vé SSO lấy thông tin học viên |
| **LMS** | `GET` | `/lms/students/:id` | `X-Service-Key` | Không | Lấy thông tin lớp học, chuyên ngành của học viên |
| **LMS** | `GET` | `/lms/students/:id/results` | `X-Service-Key` | Không | Lấy bảng điểm quá trình, bài tập, chuyên cần |
| **LMS** | `GET` | `/lms/courses` | `X-Service-Key` | Không | Danh mục khóa học đào tạo bổ sung |
| **LMS** | `POST` | `/lms/enrollments` | `X-Service-Key` | `{"studentId": "std-001", "courseId": "CRS-001"}` | Đăng ký học môn bổ sung |
| **LMS** | `GET` | `/lms/enrollments/:id` | `X-Service-Key` | Không | Tra cứu tiến độ học và điểm môn bổ sung |
| **Khảo thí** | `GET` | `/exam/students/:id/results` | `X-Service-Key` | Không | Kết quả thi sát hạch lý thuyết & thực hành |
| **lms-pm** | `GET` | `/pm/students/:id/internships` | `X-Service-Key` | Không | Kết quả thực tập IOC, điểm từng task |
| **lms-pm** | `POST` | `/pm/batches/:id/members` | `X-Service-Key` | `{"lmsUserId": "..."}` | Thêm ứng viên vào đợt thực tập |
| **Meet** | `POST` | `/meet/rooms` | `X-Service-Key` | `{"title": "Phỏng vấn..."}` | Sinh phòng họp trực tuyến Rikkei Meet |

---

## 3. Bảng Tổng Hợp Danh Sách API Mock Nội Bộ

| Method | Endpoint | Yêu Cầu Header | Mô Tả |
| :----: | :------- | :------------- | :---- |
| `POST` | `/api/v1/auth/login` | Không | Đăng nhập với 8 tài khoản mẫu (Mật khẩu: `Test@123`) |
| `GET` | `/api/v1/candidates` | Không | Lấy danh sách 3 ứng viên mẫu (`std-001`, `std-002`, `std-003`) |
| `GET` | `/api/v1/candidates/:id` | Không | Xem chi tiết hồ sơ năng lực ứng viên mẫu |
| `GET` | `/api/v1/skills` | Không | Lấy danh mục 20 kỹ năng mẫu |
| `GET` | `/api/v1/jobs` | Không | Danh sách tin tuyển dụng mẫu |
| `GET` | `/api/v1/mock/error-:code` | Không | Kiểm thử phản hồi mã lỗi HTTP: 400, 401, 403, 404, 409, 429, 500 |
| `GET` | `/health` | Không | Kiểm tra trạng thái hoạt động của Mock Server |

---

## 4. Đánh Giá Đối Chiếu Quy Chuẩn Response (`quy-chuan-api-response.md`)

| Tiêu Chí Đánh Giá | Hiện Trạng Của `mock-server` | Đánh Giá & Lý Do |
| :---------------- | :--------------------------- | :--------------- |
| **API Phân hệ ngoài (`/lms/*`, `/exam/*`, `/pm/*`, `/meet/*`)** | Trả về payload raw theo đặc tả kỹ thuật riêng của đối tác LMS, Khảo thí, Redmine/PM, Meet. | ✅ **Phù hợp thực tế**: Đây là các dịch vụ bên thứ ba (Third-party integrations), không chịu sự điều phối của Envelope nội bộ Rikkei Jobs. |
| **API Mock nội bộ (`/api/v1/*`)** | Trả về raw JSON mô phỏng ban đầu của dự án. | ⚠️ **Lưu ý**: Chỉ dùng làm dữ liệu fallback hoặc chạy unit/integration test độc lập, không thay thế API thật của các backend service. |

