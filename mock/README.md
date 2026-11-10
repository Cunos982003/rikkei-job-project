# Rikkei Jobs Mock Server (Cổng 9000)

Mock API Server giả lập các hệ thống bên thứ ba và cung cấp dữ liệu cố định cho 3 ứng viên mẫu của dự án **Rikkei Jobs**.

---

## 1. Khởi Chạy Nhanh

Server được viết bằng Node.js thuần (Zero-dependency), không cần cài thêm gói ngoài, khởi chạy tức thì:

```bash
cd mock
npm start
# Hoặc chạy trực tiếp: node server.js
```

Server sẽ lắng nghe tại: **`http://localhost:9000`**

---

## 2. Quy Tắc Bảo Mật: `X-Service-Key`

Mọi endpoint giả lập hệ thống ngoài (`/lms/*`, `/exam/*`, `/pm/*`, `/meet/*`) **bắt buộc** phải truyền header:
```http
X-Service-Key: dev-key
```

- **Nếu thiếu hoặc sai header**: Server trả về `HTTP 401 Unauthorized`:
  ```json
  {
    "code": 401,
    "message": "Khóa dịch vụ không hợp lệ hoặc bị thiếu (yêu cầu header X-Service-Key: dev-key)"
  }
  ```
- **Nếu có header hợp lệ**: Server trả về `HTTP 200 OK` cùng dữ liệu chuẩn.

---

## 3. Lệnh Kiểm Thử Mẫu (cURL / PowerShell)

### 3.1. Test kiểm tra xác thực khóa dịch vụ

#### A. Gọi khi KHÔNG CÓ khóa (Kỳ vọng: HTTP 401):
```bash
curl -i http://localhost:9000/lms/students/std-001/results
```

#### B. Gọi khi CÓ KHÓA HỢP LỆ (Kỳ vọng: HTTP 200):
```bash
curl -i -H "X-Service-Key: dev-key" http://localhost:9000/lms/students/std-001/results
```

---

### 3.2. Danh sách các API hệ thống ngoài (Yêu cầu `X-Service-Key: dev-key`)

| Hệ thống | Method | Endpoint | Mô tả |
|---|---|---|---|
| **LMS** | `POST` | `/lms/sso/tickets` | Body: `{"lmsUserId": "std-001"}` → Sinh vé SSO |
| **LMS** | `POST` | `/lms/sso/verify` | Body: `{"ticket": "..."}` → Xác thực vé SSO |
| **LMS** | `GET` | `/lms/students/:id` | Thông tin khóa học, lớp học của sinh viên |
| **LMS** | `GET` | `/lms/students/:id/results` | Điểm số, chuyên cần, bài tập, rpoint |
| **LMS** | `GET` | `/lms/courses` | Danh mục khóa học và kỹ năng đào tạo |
| **LMS** | `POST` | `/lms/enrollments` | Đăng ký môn học bổ sung |
| **LMS** | `GET` | `/lms/enrollments/:id` | Tiến độ học môn bổ sung |
| **Khảo thí** | `GET` | `/exam/students/:id/results` | Kết quả thi sát hạch chính thức |
| **lms-pm** | `GET` | `/pm/students/:id/internships` | Kết quả thực tập IOC, điểm từng task |
| **lms-pm** | `POST` | `/pm/batches/:id/members` | Body: `{"lmsUserId": "..."}` → Ghép vào đợt IOC |
| **Rikkei Meet** | `POST` | `/meet/rooms` | Body: `{"title": "..."}` → Sinh URL phòng họp giả lập |

---

### 3.3. API Nội Bộ (Không bắt buộc `X-Service-Key`)

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Đăng nhập với 8 tài khoản mẫu (Mật khẩu: `Test@123`) |
| `GET` | `/api/v1/candidates` | Danh sách 3 ứng viên mẫu (Hỗ trợ `?page=1&size=20`) |
| `GET` | `/api/v1/candidates/:id` | Xem chi tiết hồ sơ năng lực ứng viên (`std-001`, `std-002`, `std-003`) |
| `GET` | `/api/v1/skills` | Danh mục 20 kỹ năng mẫu |
| `GET` | `/api/v1/jobs` | Danh sách việc làm tuyển dụng (Có phân trang) |
| `GET` | `/api/v1/mock/error-:code` | Test các mã lỗi HTTP: 400, 401, 403, 404, 409, 429, 500 |
| `GET` | `/health` | Kiểm tra trạng thái hoạt động của Mock Server |

