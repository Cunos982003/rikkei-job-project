# Tài Liệu Quy Chuẩn Dự Án (Conventions) - Rikkei Jobs

> **Phiên bản**: 1.0  
> **Áp dụng cho**: Toàn bộ thành viên phát triển Backend, Frontend, Mock Service và QA/Tester.  
> **Mục đích**: Đồng bộ tiêu chuẩn mã nguồn, quy trình làm việc Git, thiết kế API, phân quyền, dữ liệu mẫu và tích hợp hệ thống ngoài.

---

## 1. Quy Ước Quản Lý Mã Nguồn & Git Flow

### 1.1. Cấu trúc thư mục dự án
Repository chuẩn của nhóm gồm 4 thư mục bắt buộc ở cấp gốc:
```
Rikkei-Jobs-Project/
├── backend/        # Mã nguồn các microservices Java (Spring Boot)
├── frontend/       # Mã nguồn giao diện người dùng React 19 (TypeScript, Vite, Tailwind CSS)
├── mock/           # Mock API Server hệ thống ngoài (LMS, Khảo thí, PM, Meet) chạy cổng 9000
└── docs/           # Tài liệu đặc tả kiến trúc, tech stack, conventions, hướng dẫn API
```

### 1.2. Chiến lược phân nhánh (Branching Strategy)
- **`main`**: Nhánh chính (Production-ready). Chỉ chứa mã nguồn đã kiểm thử và sẵn sàng triển khai. Không bao giờ commit trực tiếp lên `main`.
- **`develop`** (hoặc `dev`): Nhánh tích hợp dùng cho môi trường kiểm thử và tích hợp chung của toàn nhóm.
- **`feature/<mã-task>-<tên-ngắn>`**: Nhánh làm tính năng mới, tạo từ `develop`.
  - *Ví dụ*: `feature/NT-01-login`, `feature/NT-02-mock-system`, `feature/NT-05-job-crud`.
- **`bugfix/<mã-task>-<tên-ngắn>`**: Nhánh sửa lỗi phát sinh trên `develop`.
- **`hotfix/<mã-task>-<tên-ngắn>`**: Nhánh sửa lỗi khẩn cấp tạo trực tiếp từ `main`.

### 1.3. Quy tắc Pull Request (PR) & Commit
- **Đích đến**: **Mọi Pull Request tính năng đều phải trỏ vào nhánh `develop`**.
- **Tiêu đề PR**: Bắt buộc bắt đầu bằng mã định danh công việc theo cú pháp:
  ```
  [<Mã-Task>] <Mô tả ngắn gọn công việc thực hiện>
  ```
  - *Ví dụ*: `[NT-01] Xây dựng màn hình đăng nhập và xác thực JWT`
  - *Ví dụ*: `[NT-02] Khởi tạo Mock server cổng 9000 với xác thực X-Service-Key`
- **Tiêu chuẩn Commit**: Tuân thủ chuẩn [Conventional Commits](https://www.conventionalcommits.org/):
  - `feat:` Tính năng mới cho người dùng.
  - `fix:` Sửa lỗi mã nguồn.
  - `refactor:` Tái cấu trúc mã nguồn không thay đổi hành vi nghiệp vụ.
  - `docs:` Thêm hoặc sửa đổi tài liệu.
  - `test:` Bổ sung hoặc chỉnh sửa unit test/integration test.
  - `chore:` Thay đổi cấu hình build, dependency, công cụ.

---

## 2. Quy Chuẩn Thiết Kế API

### 2.1. Đường dẫn & Header
- **Tiền tố API**: `/api/v1` (ví dụ: `/api/v1/jobs`, `/api/v1/candidates`).
- **Tên tài nguyên**: Danh từ số nhiều, chữ thường, cách nhau bằng gạch ngang (kebab-case).
- **Định dạng dữ liệu**: `Content-Type: application/json; charset=UTF-8`.
- **Header xác thực người dùng**:
  ```http
  Authorization: Bearer <JWT_ACCESS_TOKEN>
  ```
- **Thời gian**: Chuỗi thời gian chuẩn ISO 8601 UTC (`YYYY-MM-DDTHH:mm:ss.sssZ`).

### 2.2. Định dạng phản hồi khi có lỗi (Error Response)
Khi xử lý thất bại hoặc phát sinh lỗi, API **bắt buộc** trả về cấu trúc gồm `code` và `message`:
```json
{
  "code": 400,
  "message": "Dữ liệu yêu cầu không hợp lệ"
}
```

Các mã lỗi HTTP chuẩn được quy định như sau:
| Mã HTTP | Tên chuẩn | Trường hợp sử dụng | Ví dụ `message` |
|:---:|---|---|---|
| **400** | Bad Request | Dữ liệu gửi lên sai định dạng, thiếu trường bắt buộc | `"Dữ liệu yêu cầu không hợp lệ"` |
| **401** | Unauthorized | Thiếu token, token hết hạn hoặc sai chữ ký | `"Chưa xác thực hoặc phiên đăng nhập đã hết hạn"` |
| **403** | Forbidden | Người dùng đã đăng nhập nhưng không có quyền truy cập | `"Bạn không có quyền thực hiện thao tác này"` |
| **404** | Not Found | Không tìm thấy tài nguyên theo ID | `"Không tìm thấy tài nguyên yêu cầu"` |
| **409** | Conflict | Xung đột dữ liệu (trùng email, nộp đơn trùng vị trí) | `"Dữ liệu đã tồn tại hoặc đang bị xung đột trạng thái"` |
| **429** | Too Many Requests | Vượt quá giới hạn tần suất gọi API (Rate limit) | `"Bạn đã gửi quá nhiều yêu cầu, vui lòng thử lại sau ít phút"` |
| **500** | Internal Error | Lỗi hệ thống server không mong muốn | `"Đã xảy ra sự cố trên hệ thống máy chủ, vui lòng thử lại sau"` |

### 2.3. Định dạng phân trang danh sách (Pagination)
- **Tham số truy vấn gửi lên**:
  - `page`: Số trang hiện tại, **bắt đầu từ 1** (mặc định: `1`).
  - `size`: Số lượng bản ghi trên một trang (mặc định: `20`).
  - *Ví dụ*: `GET /api/v1/jobs?page=1&size=20&sort=createdAt,desc`
- **Định dạng dữ liệu trả về**:
```json
{
  "items": [
    {
      "id": "job-001",
      "title": "Java Backend Developer",
      "companyName": "Công ty Cổ phần Rikkeisoft",
      "status": "PUBLISHED"
    }
  ],
  "total": 85,
  "page": 1,
  "size": 20
}
```

---

## 3. Hệ Thống 5 Vai Trò Người Dùng (Roles)

Hệ thống phân quyền dựa trên 5 vai trò cố định:

| # | Mã vai trò | Tên vai trò | Mô tả quyền hạn chính |
|---|---|---|---|
| 1 | **`CANDIDATE`** | Ứng viên | Học viên/sinh viên tốt nghiệp từ Rikkei Education. Có quyền: Quản lý hồ sơ cá nhân (Skill Profile), xem danh sách JD, nộp hồ sơ ứng tuyển, theo dõi trạng thái ứng tuyển, xem lộ trình học bổ sung từ LMS. |
| 2 | **`HR`** | Doanh nghiệp | Nhà tuyển dụng thuộc doanh nghiệp liên kết. Có quyền: Quản lý hồ sơ công ty, đăng và quản lý tin tuyển dụng (JD), tìm kiếm hồ sơ ứng viên công khai, quản lý phỏng vấn, gửi thư mời nhận việc (offer). |
| 3 | **`MENTOR`** | Giảng viên / Người hướng dẫn | Chuyên gia hướng dẫn tại Rikkei Education và dự án IOC. Có quyền: Đánh giá năng lực học viên, chấm điểm thực tập, điền phiếu đánh giá kỹ năng. |
| 4 | **`STAFF`** | Cán bộ tuyển dụng / Đào tạo | Cán bộ vận hành hệ thống Rikkei Jobs. Có quyền: Duyệt tin tuyển dụng của HR, ghép học viên vào các đợt thực tập IOC, mở khóa thông tin liên hệ của ứng viên, đồng bộ điểm số. |
| 5 | **`ADMIN`** | Quản trị viên hệ thống | Quản trị cấp cao nhất. Có quyền: Quản trị tài khoản, phân quyền, cấu hình trọng số tính điểm năng lực, xem nhật ký kiểm toán bất biến (Audit logs). |

---

## 4. Danh Sách Tài Khoản Mẫu (Dùng cho Môi Trường Phát Triển & Demo)

Toàn bộ tài khoản mẫu được kích hoạt sẵn với các quy tắc:
- **Mật khẩu dùng chung**: `Test@123`
- **Đuôi email thống nhất**: `@rikkeijobs.test`

| STT | Email đăng nhập | Mật khẩu | Vai trò | Người dùng / Doanh nghiệp đại diện |
|---|---|---|---|---|
| 1 | `candidate1@rikkeijobs.test` | `Test@123` | `CANDIDATE` | **Nguyễn Văn An** (Chuyên môn Java / Spring Boot) |
| 2 | `candidate2@rikkeijobs.test` | `Test@123` | `CANDIDATE` | **Trần Thị Bình** (Chuyên môn ReactJS / Frontend) |
| 3 | `candidate3@rikkeijobs.test` | `Test@123` | `CANDIDATE` | **Lê Hoàng Cường** (Thực tập sinh Fullstack IOC) |
| 4 | `hr1@rikkeijobs.test` | `Test@123` | `HR` | **Công ty A** (Công ty Cổ phần Rikkeisoft) |
| 5 | `hr2@rikkeijobs.test` | `Test@123` | `HR` | **Công ty B** (Rikkei Digital Solutions) |
| 6 | `mentor1@rikkeijobs.test` | `Test@123` | `MENTOR` | **Phạm Minh Đức** (Lead Mentor môn Java/Web) |
| 7 | `staff1@rikkeijobs.test` | `Test@123` | `STAFF` | **Hoàng Thu Hà** (Cán bộ Phòng Tuyển sinh & Hướng nghiệp) |
| 8 | `admin1@rikkeijobs.test` | `Test@123` | `ADMIN` | **Quản trị viên Hệ thống** |

---

## 5. Danh Mục Kỹ Năng Mẫu & Thang Bậc Đánh Giá

### 5.1. Thang bậc năng lực chuẩn (Skill Level Scale)
Chỉ số kỹ năng được tính theo thang điểm từ **0 đến 100**, phân thành 4 bậc chuẩn hóa:

| Khoảng điểm | Tên bậc | Tên tiếng Anh | Mô tả năng lực |
|:---:|:---:|:---:|---|
| **0 – 39** | **Cơ bản** | Basic / Novice | Đã tiếp cận khái niệm, hiểu cú pháp cơ bản, cần có người hướng dẫn chi tiết khi làm việc thực tế. |
| **40 – 69** | **Khá** | Proficient | Nắm vững kiến thức nền tảng, có khả năng hoàn thành các tính năng tiêu chuẩn theo yêu cầu dự án. |
| **70 – 84** | **Tốt** | Advanced | Làm việc độc lập vững vàng, có kỹ năng gỡ lỗi tốt, áp dụng tốt các mẫu thiết kế và chuẩn code. |
| **85 – 100** | **Xuất sắc** | Expert / Master | Hiểu sâu về cấu trúc bên dưới, có khả năng tối ưu hiệu năng, giải quyết bài toán phức tạp và hướng dẫn thành viên khác. |

### 5.2. Danh mục 20 kỹ năng chuẩn
1. **Java**: Lập trình hướng đối tượng, Java 17+, Collections, Stream API, Concurrency.
2. **Spring Boot**: Spring MVC, Spring Data JPA, Spring Security, Dependency Injection.
3. **SQL (PostgreSQL/MySQL)**: Thiết kế CSDL quan hệ, viết truy vấn phức tạp, tối ưu Index, Transaction.
4. **Git**: Phân nhánh, merge, rebase, giải quyết xung đột mã nguồn trên GitHub/GitLab.
5. **REST API**: Thiết kế API chuẩn RESTful, status code, JSON format, tài liệu hóa với OpenAPI/Swagger.
6. **HTML/CSS**: Ngôn ngữ đánh dấu trang web, CSS layout, Flexbox, Grid, Responsive Design.
7. **JavaScript**: ES6+, xử lý bất đồng bộ (Promise, Async/Await), DOM manipulation.
8. **ReactJS**: Component lifecycle, React Hooks, State Management, Virtual DOM.
9. **TypeScript**: Kiểu dữ liệu tĩnh, Interface, Type aliases, Generics, tích hợp với React.
10. **Docker**: Đóng gói ứng dụng thành container, viết Dockerfile, chạy multi-container với Docker Compose.
11. **Tiếng Anh (English)**: Đọc hiểu tài liệu kỹ thuật, giao tiếp cơ bản trong công việc.
12. **Python**: Lập trình Python 3+, xử lý dữ liệu, làm việc với FastAPI/Flask.
13. **Kiến trúc Microservices**: Tách miền nghiệp vụ, API Gateway, giao tiếp đồng bộ (REST) và bất đồng bộ.
14. **Redis**: In-memory caching, phân tán khóa (Distributed lock), Rate limiting, TTL.
15. **Apache Kafka**: Message broker phân tán, Event-driven architecture, Producer & Consumer, Transactional Outbox.
16. **Unit Testing**: Viết unit test với JUnit 5, Mockito, Test-Driven Development (TDD) cơ bản.
17. **CI/CD**: Tự động hóa kiểm thử và đóng gói bản phát hành với GitHub Actions.
18. **Linux / Bash**: Thao tác dòng lệnh, quản lý tiến trình, phân quyền file, viết shell script cơ bản.
19. **Tailwind CSS**: Utility-first CSS framework, tối ưu giao diện UI hiện đại và responsive.
20. **Làm việc nhóm & Giao tiếp (Soft skills)**: Giao tiếp hiệu quả, báo cáo tiến độ, tư duy phản biện, phối hợp Agile/Scrum.

---

## 6. Quy Ước Mock Hệ Thống Ngoài

### 6.1. Nguyên tắc hoạt động
- **Thư mục phục vụ**: `mock/`
- **Cổng dịch vụ**: **`9000`** (`http://localhost:9000`)
- **Yêu cầu bảo mật dịch vụ nội bộ**:
  - Mọi request gửi tới các endpoint của hệ thống ngoài (LMS, Khảo thí, LMS-PM, Rikkei Meet) **bắt buộc phải có header**:
    ```http
    X-Service-Key: dev-key
    ```
  - Nếu thiếu header hoặc giá trị khác `dev-key`, Mock Server lập tức trả về:
    - **HTTP Status**: `401 Unauthorized`
    - **Nội dung JSON**:
      ```json
      {
        "code": 401,
        "message": "Khóa dịch vụ không hợp lệ hoặc bị thiếu"
      }
      ```
  - Nếu có khóa `dev-key` hợp lệ, server trả về dữ liệu mẫu cố định cho 3 ứng viên.

### 6.2. Danh sách các API hệ thống ngoài được giả lập

#### A. Hệ thống Đào tạo (Rikkei LMS)
- `POST /lms/sso/tickets`  
  - Body: `{ "lmsUserId": "std-001" }`
  - Response: `{ "ticket": "TICKET-XYZ123" }` *(Mô phỏng học viên bấm nút đăng nhập từ cổng LMS)*
- `POST /lms/sso/verify`  
  - Body: `{ "ticket": "TICKET-XYZ123" }`
  - Response: `{ "lmsUserId": "std-001", "email": "candidate1@rikkeijobs.test", "fullName": "Nguyễn Văn An", "role": "STUDENT", "issuedAt": "2026-11-10T08:00:00Z" }`
- `GET /lms/students/{id}`  
  - Response: `{ "program": "Lập trình Java Fullstack", "classCode": "RA-JAVA-2026", "courseName": "Khóa đào tạo Kỹ sư Phần mềm Chuyên sâu" }`
- `GET /lms/students/{id}/results`  
  - Response: Mảng danh sách kết quả học tập: `[{ "subjectCode": "JAVA-01", "subjectName": "Java Core", "score": 8.5, "attendanceRate": 98.0, "homeworkRate": 95.0, "rpoint": 120 }, ...]`
- `GET /lms/courses`  
  - Response: Mảng danh sách khóa học và kỹ năng đào tạo: `[{ "courseId": "CRS-01", "name": "Lập trình Spring Boot nâng cao", "skills": ["Spring Boot", "REST API", "PostgreSQL"], "level": "Advanced" }, ...]`
- `POST /lms/enrollments`  
  - Body: `{ "studentId": "std-001", "courseId": "CRS-01" }` → Trả về thông tin đăng ký môn học bù/học bổ sung.
- `GET /lms/enrollments/{id}`  
  - Response: Thông tin tiến độ học tập môn bổ sung.

#### B. Hệ thống Khảo thí (Rikkei Examination)
- `GET /exam/students/{id}/results`  
  - Response: Mảng kết quả thi sát hạch: `[{ "examCode": "EXAM-JV-FINAL", "subjectCode": "JAVA-01", "score": 88.0, "takenAt": "2026-10-15T09:00:00Z" }, ...]`

#### C. Hệ thống Quản trị Thực tập IOC (LMS-PM)
- `GET /pm/students/{id}/internships`  
  - Response: Lịch sử và kết quả thực tập dự án IOC
- `POST /pm/batches/{id}/members`  
  - Body: `{ "lmsUserId": "std-001" }` → Ghép ứng viên vào đợt thực tập IOC.

#### D. Nền tảng Phỏng vấn trực tuyến (Rikkei Meet)
- `POST /meet/rooms`  
  - Body: `{ "title": "Phỏng vấn Vòng 1 - Nguyễn Văn An", "startAt": "2026-11-15T09:00:00Z", "endAt": "2026-11-15T10:00:00Z" }`
  - Response: `{ "roomUrl": "https://meet.rikkei.edu.vn/room/mock-a1b2c3d4-interview" }`

---

*Tài liệu này là quy chuẩn kỹ thuật bắt buộc của dự án Rikkei Jobs. Mọi PR kiểm thử phải tuân thủ nghiêm ngặt các quy định trên.*

