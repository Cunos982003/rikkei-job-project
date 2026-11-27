# [NT-08] Kế hoạch & Kịch bản kiểm thử xuyên suốt 7 module, bảo mật

> **Mục tiêu:** Kiểm thử toàn diện luồng nghiệp vụ end-to-end cho 5 vai trò người dùng, kiểm thử phân quyền RBAC đa dịch vụ, bảo vệ thông tin liên hệ ứng viên, kiểm thử bảo mật cơ bản và kiểm tải hệ thống trước buổi bảo vệ. Lập danh mục backlog lỗi bàn giao cho NT-11.

---

## 1. Thông tin chung & Ma trận kiểm thử

- **Hệ thống:** Nền tảng Rikkei Jobs Platform (7 module nghiệp vụ, 11 microservices).
- **5 Vai trò kiểm thử:**
  1. `CANDIDATE` (Học viên / Ứng viên tìm việc)
  2. `HR` (Nhà tuyển dụng doanh nghiệp)
  3. `STAFF` (Cán bộ tuyển sinh & quan hệ doanh nghiệp)
  4. `MENTOR` (Giảng viên / Chuyên gia đánh giá & phỏng vấn)
  5. `ADMIN` (Quản trị viên hệ thống)
- **Quy chuẩn đánh giá:**
  - **Đạt (Pass):** Đáp ứng đúng HTTP status code, cấu trúc JSON phản hồi, logic nghiệp vụ và ràng buộc bảo mật.
  - **Không đạt (Fail):** Xảy ra lỗi 500, sai mã trạng thái, hở PII hoặc vi phạm quyền truy cập.

---

## 2. Kịch bản kiểm thử chi tiết theo 5 vai trò (≥ 8 bước / vai trò)

### 2.1. Vai trò 1: CANDIDATE (Ứng viên tìm việc) — 9 bước xuyên suốt

| Mã bước | Tên bước / Thao tác | Endpoint / Giao diện | Dữ liệu đầu vào | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| **TC-CAN-01** | Đăng nhập SSO qua vé LMS | `POST /api/v1/auth/sso` | `ticket: "ticket-new-user-01"` | HTTP 200, trả về JWT token với role `CANDIDATE` và thông tin user | HTTP 200, sinh token Bearer JWT hợp lệ | **Đạt** |
| **TC-CAN-02** | Cập nhật hồ sơ cá nhân & kỹ năng | `PUT /api/v1/profile` | `fullName: "Nguyen Van A", headline: "Java Backend", skills: ["Java", "Spring"]` | HTTP 200, cập nhật thành công hồ sơ cá nhân | HTTP 200, dữ liệu hồ sơ được lưu vào DB | **Đạt** |
| **TC-CAN-03** | Tải lên tệp CV PDF snapshot | `POST /api/v1/profile/cv` | Multipart file: `cv_nguyen_van_a.pdf` (dung lượng 1.5MB) | HTTP 201/200, lưu trữ CV thành công và trả về `cvSnapshotUrl` | HTTP 200, tệp PDF được lưu vào `uploads/` | **Đạt** |
| **TC-CAN-04** | Gửi yêu cầu công khai hồ sơ | `POST /api/v1/profile/visibility/request` | `consent: true` | HTTP 200, trạng thái chuyển từ `PRIVATE` sang `PENDING` | HTTP 200, profile review được tạo ở trạng thái PENDING | **Đạt** |
| **TC-CAN-05** | Tìm kiếm tin tuyển dụng phù hợp | `GET /api/v1/jobs?keyword=Java` | `keyword="Java", status="PUBLISHED"` | HTTP 200, danh sách việc làm phù hợp, có phân trang | HTTP 200, trả về danh sách job đang mở tuyển | **Đạt** |
| **TC-CAN-06** | Nộp đơn ứng tuyển vào việc làm | `POST /api/v1/jobs/{id}/apply` | Header: `Bearer <jwt_candidate>`, Body: `coverLetter: "Em mong muốn ứng tuyển vị trí này"` | HTTP 201, tạo đơn ứng tuyển mới với trạng thái `APPLIED` | HTTP 201, `RecruitmentSummary` trạng thái APPLIED | **Đạt** |
| **TC-CAN-07** | Xác nhận lịch phỏng vấn | `GET /api/v1/me/recruitments/{id}` | Header: `Bearer <jwt_candidate>` | HTTP 200, thấy chi tiết đơn ứng tuyển, trạng thái vòng phỏng vấn và link meeting | HTTP 200, hiển thị đầy đủ thông tin vòng phỏng vấn | **Đạt** |
| **TC-CAN-08** | Nhận Offer và đồng ý nhận việc | `POST /api/v1/offers/{offerId}/accept` | Header: `Bearer <jwt_candidate>` | HTTP 200, chuyển trạng thái offer sang `ACCEPTED`, tạo bản ghi tuyển dụng | HTTP 200, offer được chấp nhận, trạng thái đơn cập nhật | **Đạt** |
| **TC-CAN-09** | Hoàn tất thủ tục Onboarding | `GET /api/v1/me/recruitments/{id}` | Header: `Bearer <jwt_candidate>` | HTTP 200, trạng thái tuyển dụng chuyển sang `ONBOARDING` / `EMPLOYED` | HTTP 200, hiển thị thông tin tiếp nhận việc làm | **Đạt** |

---

### 2.2. Vai trò 2: HR (Nhà tuyển dụng doanh nghiệp) — 9 bước xuyên suốt

| Mã bước | Tên bước / Thao tác | Endpoint / Giao diện | Dữ liệu đầu vào | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| **TC-HR-01** | Đăng nhập tài khoản HR | `POST /api/v1/auth/login` | `email: "hr1@rikkei.vn", password: "Password123@"` | HTTP 200, trả về JWT chứa role `ROLE_HR` và `companyId` | HTTP 200, đăng nhập thành công | **Đạt** |
| **TC-HR-02** | Đăng tin tuyển dụng mới | `POST /api/v1/jobs` | `title: "Senior Java Developer", salaryMin: 20000000, skills: [...]` | HTTP 201, tạo tin tuyển dụng thành công ở trạng thái `PUBLISHED` | HTTP 201, Job entity được tạo với ID hợp lệ | **Đạt** |
| **TC-HR-03** | Tìm kiếm ứng viên trên Directory (chưa mở liên hệ) | `GET /api/v1/candidates?keyword=Java` | Header: `Bearer <jwt_hr>` | HTTP 200, trả về danh sách ứng viên, **`email` và `phone` phải là null/bị ẩn** | HTTP 200, không lộ email và số điện thoại | **Đạt** |
| **TC-HR-04** | Gửi yêu cầu mở thông tin liên hệ | `POST /api/v1/candidates/{userId}/contact-requests` | `jobId: <uuid>, reason: "Ứng viên phù hợp vị trí Backend"` | HTTP 201, tạo yêu cầu mở liên hệ ở trạng thái `PENDING` gửi cán bộ | HTTP 201, ContactRequest được lưu thành công | **Đạt** |
| **TC-HR-05** | Xem Kanban ứng viên nộp đơn | `GET /api/v1/recruitments/{id}` | Header: `Bearer <jwt_hr>` | HTTP 200, xem chi tiết đánh giá ứng viên, điểm match AI, lịch sử vòng tuyển | HTTP 200, trả về `RecruitmentReviewDetails` | **Đạt** |
| **TC-HR-06** | Tạo vòng và lên lịch phỏng vấn | `POST /api/v1/recruitments/{id}/rounds` | `roundNo: 1, name: "Phỏng vấn Kỹ thuật", scheduledAt: "2026-12-01T09:00:00Z"` | HTTP 201, tạo vòng phỏng vấn thành công, chuyển trạng thái đơn sang `INTERVIEW` | HTTP 201, sinh vòng phỏng vấn và gửi thông báo | **Đạt** |
| **TC-HR-07** | Nhập kết quả & điểm số phỏng vấn | `PUT /api/v1/recruitments/{id}/rounds/{roundId}` | `status: "PASSED", score: 85, note: "Nắm vững Spring Boot và Database"` | HTTP 200, cập nhật điểm số và kết quả vòng phỏng vấn thành công | HTTP 200, vòng phỏng vấn chuyển sang PASSED | **Đạt** |
| **TC-HR-08** | Tạo và gửi Offer tuyển dụng | `POST /api/v1/offers` | `recruitmentId: <uuid>, salary: 25000000, deadline: "2026-12-10"` | HTTP 201, Offer chuyển sang `SENT`, đơn tuyển dụng chuyển sang `OFFER` | HTTP 201, Offer được tạo và gửi email thông báo | **Đạt** |
| **TC-HR-09** | Xác nhận nhân sự tiếp nhận Onboard | `POST /api/v1/offers/{offerId}/confirm-onboard` | Header: `Bearer <jwt_hr>` | HTTP 200, xác nhận ứng viên đã đi làm, tạo hồ sơ việc làm chính thức | HTTP 200, trạng thái hoàn tất chu trình tuyển dụng | **Đạt** |

---

### 2.3. Vai trò 3: STAFF (Cán bộ tuyển sinh & quan hệ doanh nghiệp) — 8 bước xuyên suốt

| Mã bước | Tên bước / Thao tác | Endpoint / Giao diện | Dữ liệu đầu vào | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| **TC-STF-01** | Đăng nhập tài khoản STAFF | `POST /api/v1/auth/sso` | `ticket: "ticket-staff-01"` | HTTP 200, trả về JWT có authority `ROLE_STAFF` | HTTP 200, xác thực STAFF thành công | **Đạt** |
| **TC-STF-02** | Xem danh sách hồ sơ chờ duyệt | `GET /api/v1/staff/profile-reviews` | Header: `Bearer <jwt_staff>`, `status="PENDING"` | HTTP 200, trả về danh sách hồ sơ ứng viên đang chờ phê duyệt | HTTP 200, danh sách hồ sơ ở trạng thái PENDING | **Đạt** |
| **TC-STF-03** | Phê duyệt hồ sơ ứng viên sang PUBLIC | `POST /api/v1/staff/profile-reviews/{id}/approve` | Header: `Bearer <jwt_staff>` | HTTP 200, hồ sơ chuyển sang `PUBLIC`, hiển thị trên trang tìm kiếm doanh nghiệp | HTTP 200, cập nhật trạng thái hồ sơ sang PUBLIC | **Đạt** |
| **TC-STF-04** | Phê duyệt yêu cầu mở liên hệ của HR | `POST /api/v1/staff/contact-requests/{id}/approve` | Header: `Bearer <jwt_staff>` | HTTP 200, phê duyệt mở liên hệ, cấp quyền cho HR xem SĐT/Email ứng viên | HTTP 200, ContactRequest chuyển sang APPROVED | **Đạt** |
| **TC-STF-05** | Giám sát toàn bộ đơn tuyển dụng sàn | `GET /api/v1/staff/recruitments` | Header: `Bearer <jwt_staff>` | HTTP 200, xem toàn bộ đơn của mọi công ty với quyền giám sát của trường | HTTP 200, trả về `PageResult<RecruitmentStaffSummary>` | **Đạt** |
| **TC-STF-06** | Hỗ trợ điều phối lịch phỏng vấn | `PUT /api/v1/recruitments/{id}/rounds/{roundId}` | Header: `Bearer <jwt_staff>`, `interviewerEmail: "mentor1@rikkei.edu.vn"` | HTTP 200, cập nhật người phỏng vấn và phòng phỏng vấn thành công | HTTP 200, cập nhật điều phối thành công | **Đạt** |
| **TC-STF-07** | Quản lý danh mục kỹ năng (Skill Catalog) | `POST /api/v1/skills` | `code: "GOLANG", name: "Golang", group: "BACKEND"` | HTTP 201, thêm mới kỹ năng vào danh mục đào tạo thành công | HTTP 201, Skill entity được lưu vào DB | **Đạt** |
| **TC-STF-08** | Khóa tài khoản HR vi phạm quy định | `POST /api/v1/auth/admin/hr-accounts/{id}/lock` | `reason: "Spam ứng viên không đúng quy định"` | HTTP 200, khóa tài khoản HR thành công, chặn đăng nhập | HTTP 200, tài khoản HR chuyển sang LOCKED | **Đạt** |

---

### 2.4. Vai trò 4: MENTOR (Giảng viên / Chuyên gia đánh giá) — 8 bước xuyên suốt

| Mã bước | Tên bước / Thao tác | Endpoint / Giao diện | Dữ liệu đầu vào | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| **TC-MEN-01** | Đăng nhập SSO vai trò MENTOR | `POST /api/v1/auth/sso` | `ticket: "ticket-mentor-01"` | HTTP 200, trả về JWT có authority `ROLE_MENTOR` | HTTP 200, xác thực MENTOR thành công | **Đạt** |
| **TC-MEN-02** | Xem danh sách học viên phụ trách | `GET /api/v1/mentor/students` | Header: `Bearer <jwt_mentor>` | HTTP 200, danh sách học viên kèm tiến độ học tập và kỹ năng hiện tại | HTTP 200, trả về học viên thuộc lớp phụ trách | **Đạt** |
| **TC-MEN-03** | Đánh giá & chấm điểm kỹ năng | `POST /api/v1/mentor/skill-evaluations` | `studentId: <uuid>, skillCode: "JAVA", score: 90` | HTTP 200, cập nhật `verifiedScore` trong hồ sơ kỹ năng của học viên | HTTP 200, lưu điểm kỹ năng đã xác thực | **Đạt** |
| **TC-MEN-04** | Phê duyệt chứng chỉ / bằng cấp | `POST /api/v1/mentor/certificates/{id}/verify` | `status: "VERIFIED", note: "Chứng chỉ AWS hợp lệ"` | HTTP 200, chứng chỉ được gắn nhãn verified trên hồ sơ ứng viên | HTTP 200, cập nhật trạng thái chứng chỉ | **Đạt** |
| **TC-MEN-05** | Mở link đánh giá buổi phỏng vấn | `GET /api/v1/evaluations/access?token=<eval_token>` | Query token đánh giá phỏng vấn | HTTP 200, trả về thông tin ứng viên và các tiêu chí chấm điểm vòng | HTTP 200, mở form chấm điểm hợp lệ | **Đạt** |
| **TC-MEN-06** | Nhập điểm và nhận xét phỏng vấn | `POST /api/v1/evaluations/submit` | `token: <eval_token>, score: 88, feedback: "Kỹ năng OOP tốt"` | HTTP 200, lưu kết quả đánh giá phỏng vấn vào hồ sơ ứng tuyển | HTTP 200, lưu feedback phỏng vấn thành công | **Đạt** |
| **TC-MEN-07** | Đề xuất khóa học bổ trợ LMS | `POST /api/v1/mentor/recommend-course` | `studentId: <uuid>, courseCode: "SPRING_MICROSERVICES"` | HTTP 200, gửi gợi ý khóa học vào thông báo của học viên | HTTP 200, tạo gợi ý đào tạo thành công | **Đạt** |
| **TC-MEN-08** | Tra cứu lịch sử đánh giá kỹ năng | `GET /api/v1/mentor/students/{id}/skill-history` | Header: `Bearer <jwt_mentor>` | HTTP 200, xem biểu đồ và lịch sử thay đổi điểm kỹ năng theo thời gian | HTTP 200, hiển thị lịch sử đánh giá | **Đạt** |

---

### 2.5. Vai trò 5: ADMIN (Quản trị viên hệ thống) — 8 bước xuyên suốt

| Mã bước | Tên bước / Thao tác | Endpoint / Giao diện | Dữ liệu đầu vào | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| **TC-ADM-01** | Đăng nhập tài khoản ADMIN | `POST /api/v1/auth/sso` | `ticket: "ticket-admin-01"` | HTTP 200, trả về JWT có authority `ROLE_ADMIN` | HTTP 200, đăng nhập quản trị thành công | **Đạt** |
| **TC-ADM-02** | Tạo tài khoản HR đối tác mới | `POST /api/v1/auth/admin/hr-accounts` | `email: "hr_fpt@rikkei.vn", fullName: "HR FPT", companyId: <uuid>` | HTTP 201, tạo tài khoản HR thành công và gửi email kích hoạt | HTTP 201, tạo tài khoản HR thành công | **Đạt** |
| **TC-ADM-03** | Khóa/Mở khóa tài khoản người dùng | `POST /api/v1/auth/admin/hr-accounts/{id}/lock` | `reason: "Bảo trì tài khoản"` | HTTP 200, cập nhật trạng thái tài khoản `LOCKED` / `ACTIVE` | HTTP 200, cập nhật trạng thái thành công | **Đạt** |
| **TC-ADM-04** | Quản lý danh mục kỹ năng hệ thống | `PUT /api/v1/skills/{id}` | `name: "Cloud Native & DevOps", group: "DEVOPS"` | HTTP 200, cập nhật thông tin kỹ năng trong catalog | HTTP 200, cập nhật thông tin thành công | **Đạt** |
| **TC-ADM-05** | Cấu hình tiêu chí cấp huy hiệu (Badges) | `POST /api/v1/admin/badges` | `code: "JAVA_MASTER", name: "Java Master", minScore: 90` | HTTP 201, tạo tiêu chí cấp badge tự động cho học viên | HTTP 201, Badge criteria được lưu vào DB | **Đạt** |
| **TC-ADM-06** | Tra cứu Audit Log hệ thống | `GET /api/v1/audit/logs?service=auth-service` | Header: `Bearer <jwt_admin>` | HTTP 200, trả về lịch sử thao tác quản trị, đăng nhập, phân quyền | HTTP 200, danh sách Audit log đầy đủ | **Đạt** |
| **TC-ADM-07** | Giám sát Actuator & Prometheus Metrics | `GET /actuator/prometheus` | Không cần auth hoặc auth ADMIN | HTTP 200, xuất đầy đủ metrics hệ thống: `auth_login_total`, JVM, HTTP request latency | HTTP 200, dữ liệu Prometheus hợp lệ | **Đạt** |
| **TC-ADM-08** | Cấu hình & gửi thông báo toàn sàn | `POST /api/v1/notifications/broadcast` | `title: "Bảo trì hệ thống", content: "Hệ thống bảo trì 23h-24h", targetRole: "ALL"` | HTTP 200, phát thông báo tới tất cả người dùng trong hệ thống | HTTP 200, broadcast thành công | **Đạt** |

---

## 3. Tổng kết nghiệm thu Kịch bản 5 vai trò

- **Tổng số kịch bản:** 42 bước kiểm thử.
- **Tỉ lệ đạt:** **42/42 (100% Đạt)**.
- **Tiêu chí AC:** Đã đáp ứng đầy đủ kịch bản cho 5 vai trò, mỗi vai trò $\ge 8$ bước tuần tự theo luồng chính, có cột kết quả mong đợi, thực tế và trạng thái Đạt/Không đạt.
