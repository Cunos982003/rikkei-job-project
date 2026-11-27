# [NT-08] Kiểm thử xuyên suốt 7 module, bảo mật và kiểm tải

## 1. Thông tin Pull Request

* **Tiêu đề PR:** `[NT-08] Kiểm thử xuyên suốt 7 module, bảo mật và kiểm tải`
* **Target Branch:** `develop`
* **Source Branch:** `feature/TA65388`
* **Module ảnh hưởng:** `profile-service`, `recruitment-service`, `auth-service`, `job-service`, `learning-service`, `assessment-service`, `ai_career_service`, `api-gateway`, `docs`, `tests`.

---

## 2. Tóm tắt các thay đổi chính (Summary of Changes)

1. **Test Plan 5 vai trò ([docs/test-plan.md](file:///d:/Project/rikkei-job-project/docs/test-plan.md)):**
   * Xây dựng kịch bản kiểm thử xuyên suốt luồng người dùng cho 5 vai trò: `CANDIDATE` (9 bước), `HR` (9 bước), `STAFF` (8 bước), `MENTOR` (8 bước), `ADMIN` (8 bước) = **42 bước kiểm thử**, tất cả đều có kết quả đánh giá **Đạt**.
2. **Khắc phục lộ thông tin liên hệ PII của ứng viên & Bổ sung Test ([CandidateDirectoryController.java](file:///d:/Project/rikkei-job-project/backend/profile-service/src/main/java/com/example/profileservice/controller/CandidateDirectoryController.java)):**
   * Ngăn chặn việc trả về email và số điện thoại khi HR tra cứu danh bạ (`/api/v1/candidates`) hoặc xem chi tiết (`/api/v1/candidates/{id}`) khi chưa được mở liên hệ (`contactLocked: true`).
   * Viết bộ integration test [CandidateContactPrivacyTest.java](file:///d:/Project/rikkei-job-project/backend/profile-service/src/test/java/com/example/profileservice/controller/CandidateContactPrivacyTest.java) chứng minh HR không nhận được email/phone trước khi được phê duyệt mở liên hệ.
3. **Bộ test kiểm quyền tự động RBAC chạy 1 lệnh xuyên suốt 7 module ([run-all-rbac-tests.ps1](file:///d:/Project/rikkei-job-project/tests/run-all-rbac-tests.ps1)):**
   * Test token sai vai trò trả về `403 Forbidden`, truy cập dữ liệu công ty khác trả về `404 Not Found`.
   * Test kiểm tra JWT hết hạn và JWT chữ ký giả bị từ chối xác thực tại [RbacSecurityIntegrationTest.java](file:///d:/Project/rikkei-job-project/backend/recruitment-service/src/test/java/com/example/recruitment_service/security/RbacSecurityIntegrationTest.java).
4. **Bộ test bảo mật cơ bản (SQLi, XSS, File, DB Constraints):**
   * Test SQL Injection `' OR '1'='1` trên search query param không làm crash hệ thống tại [SecurityVulnerabilityIntegrationTest.java](file:///d:/Project/rikkei-job-project/backend/recruitment-service/src/test/java/com/example/recruitment_service/security/SecurityVulnerabilityIntegrationTest.java).
   * Test XSS injection `<script>alert(1)</script>` trong trường nhận xét/ghi chú được xử lý an toàn.
   * Test File upload từ chối file > 5MB và file thực thi `.exe` tại [FileControllerTest.java](file:///d:/Project/rikkei-job-project/backend/profile-service/src/test/java/com/example/profileservice/controller/FileControllerTest.java).
   * Test kiểm tra ràng buộc CSDL: Not Null, Unique 1-1, Compound Unique constraint.
5. **Kịch bản & Báo cáo kiểm tải k6 với 1.000 hồ sơ ([docs/load-test-report.md](file:///d:/Project/rikkei-job-project/docs/load-test-report.md)):**
   * Sinh dữ liệu mẫu 1.000 hồ sơ ứng viên (NT-16) tại [generate-1000-candidates.js](file:///d:/Project/rikkei-job-project/tests/load/generate-1000-candidates.js).
   * Kịch bản k6 [k6-candidate-search.js](file:///d:/Project/rikkei-job-project/tests/load/k6-candidate-search.js) chạy 20 VUs đồng thời trong 30s.
   * Kết quả tải: **4,820 requests**, Throughput **160.6 req/s**, **p95 = 114.7 ms** (SLA yêu cầu $< 500\text{ ms}$), **0 request lỗi (0.00%)**.
6. **Danh sách Backlog lỗi hệ thống ([docs/bugs.md](file:///d:/Project/rikkei-job-project/docs/bugs.md)):**
   * Lập danh mục 12 lỗi chi tiết đầy đủ các trường (Bước tái hiện, Kết quả mong đợi, Kết quả thực tế, Mức độ Cao/Trung bình/Thấp, Module, Người phụ trách) để bàn giao cho task **NT-11**.
7. **Cập nhật đặc tả OpenAPI ([docs/api/openapi/profile-service-nt08.yaml](file:///d:/Project/rikkei-job-project/docs/api/openapi/profile-service-nt08.yaml)):**
   * Khai báo OpenAPI 3.0.3 cho toàn bộ các endpoint tra cứu danh bạ ứng viên và kiểm soát mở liên hệ.

---

## 3. Hướng dẫn chạy kiểm thử (How to Run)

### 3.1. Chạy toàn bộ test kiểm quyền 7 module bằng 1 lệnh duy nhất
* Trên Windows (PowerShell):
  ```powershell
  ./tests/run-all-rbac-tests.ps1
  ```
* Trên Linux / macOS (Bash):
  ```bash
  chmod +x tests/run-all-rbac-tests.sh
  ./tests/run-all-rbac-tests.sh
  ```

### 3.2. Chạy riêng từng test suite
* **Test bảo mật liên hệ PII của ứng viên (Profile Service):**
  ```bash
  cd backend/profile-service
  ./gradlew test --tests "*CandidateContactPrivacyTest*"
  ```
* **Test kiểm quyền RBAC (Recruitment Service):**
  ```bash
  cd backend/recruitment-service
  ./gradlew test --tests "*RbacSecurityIntegrationTest*"
  ```
* **Test bảo mật SQL Injection, XSS, DB Constraints:**
  ```bash
  cd backend/recruitment-service
  ./gradlew test --tests "*SecurityVulnerabilityIntegrationTest*"
  ```
* **Test kiểm tra kích thước file & định dạng cấm:**
  ```bash
  cd backend/profile-service
  ./gradlew test --tests "*FileControllerTest*"
  ```

### 3.3. Chạy kịch bản kiểm tải k6
1. Sinh tập dữ liệu 1.000 hồ sơ:
   ```bash
   node tests/load/generate-1000-candidates.js
   ```
2. Chạy test tải với k6:
   ```bash
   k6 run tests/load/k6-candidate-search.js --env API_BASE_URL=http://localhost:8080
   ```

---

## 4. Dữ liệu mẫu kiểm thử (Sample Test Data)

* File dữ liệu 1.000 ứng viên: [candidates-1000.json](file:///d:/Project/rikkei-job-project/tests/load/candidates-1000.json)
* Tài khoản mẫu kiểm thử:
  * **CANDIDATE:** `candidate1@rikkeijobs.test` / Pass: `123456`
  * **HR (Công ty A):** `hr.rikkeisoft@rikkeijobs.test` / Pass: `123456`
  * **STAFF:** `staff.admin@rikkei.edu.vn` / Pass: `123456`
  * **MENTOR:** `mentor.java@rikkei.edu.vn` / Pass: `123456`
  * **ADMIN:** `admin.system@rikkei.edu.vn` / Pass: `123456`

---

## 5. Danh mục tài liệu nghiệm thu (Deliverables Checklist)

- [x] `docs/test-plan.md`: Đủ 5 vai trò, mỗi vai trò $\ge 8$ bước (tổng cộng 42 bước), có cột Đạt/Không đạt.
- [x] Bộ test kiểm quyền chạy bằng 1 lệnh (`tests/run-all-rbac-tests.ps1`), phủ $\ge 1$ endpoint mỗi module.
- [x] Test chứng minh HR không nhận được email/phone trước khi được mở liên hệ (`CandidateContactPrivacyTest.java`).
- [x] Báo cáo kiểm tải có p95 (114.7 ms) và số request lỗi (0 lỗi) tại `docs/load-test-report.md`.
- [x] Backlog $\ge 10$ lỗi (12 lỗi) đủ trường thông tin, đã giao người phụ trách tại `docs/bugs.md`.
- [x] OpenAPI specification cập nhật tại `docs/api/openapi/profile-service-nt08.yaml`.
- [x] Đã chuẩn bị sẵn sàng cho reviewer và mentor phê duyệt PR.
