# [DT-04] Cán bộ theo dõi đào tạo bổ sung theo lớp, khoá

## 1. Mô tả tổng quan
- **Mã Task**: `[Đào tạo bổ sung · DT-04]`
- **Tiêu đề PR**: `[DT-04] Cán bộ theo dõi đào tạo bổ sung theo lớp, khoá`
- **Nhánh nguồn (Head branch)**: `feature/DT-04-theo-doi-dao-tao-bo-sung`
- **Nhánh đích (Base branch)**: `dev` (develop)
- **Mục tiêu**:
  - Cung cấp cho **Cán bộ quản lý (STAFF / ADMIN)** công cụ theo dõi toàn diện việc học tập và bồi dưỡng năng lực của học viên theo từng lớp học (`classCode`) và khóa đào tạo (`courseName`).
  - Xây dựng hệ thống truy vấn gộp tổng hợp đa nguồn: thông tin học viên (`candidate_profiles`), mục tiêu vị trí/JD (`candidate_targets`), khoảng thiếu hụt năng lực (`gap_results`), tiến độ ghi danh LMS (`enrollments`) và tỷ lệ sẵn sàng (`readiness`).
  - Thống kê tự động: Thẻ số tổng ứng viên đang học, đã hoàn thành; biểu đồ phân tích Top 5 kỹ năng thiếu hụt nhiều nhất để phục vụ công tác điều phối giảng dạy.
  - Cung cấp tính năng **Xuất báo cáo Excel (.xlsx)** chuẩn định dạng OpenXML, tự động áp dụng đúng bộ lọc đang chọn của người dùng.
  - Thiết lập ma trận phân quyền RBAC nghiêm ngặt: Chỉ cho phép `STAFF` hoặc `ADMIN` truy cập, chặn tuyệt đối `HR` và `CANDIDATE` (HTTP 403 Forbidden).
- **Phụ thuộc**: Hoàn thiện tiếp nối các task nền tảng `DT-01`, `DT-02`, `DT-03`.
- **Tài liệu API chi tiết**:
  - Markdown Docs: [backend/learning-service/api-docs.md](../../backend/learning-service/api-docs.md)
  - OpenAPI Specification: [docs/api/openapi/learning-service-dt04.yaml](../api/openapi/learning-service-dt04.yaml)

---

## 2. Thay đổi chi tiết theo từng tầng kiến trúc

### 2.1. Cơ sở dữ liệu (Flyway Migrations & Seed Data)
1. **Migration `learning-service`**: [V5__init_dt04_staff_upskilling.sql](../../backend/learning-service/src/main/resources/db/migration/V5__init_dt04_staff_upskilling.sql)
   - Tạo bảng `candidate_profiles` với các trường: `id`, `user_id`, `full_name`, `email`, `phone_number`, `class_code`, `course_name`, `created_at`, `updated_at`.
   - Tạo chỉ mục truy vấn tối ưu:
     - `idx_candidate_profiles_class_course` trên `candidate_profiles(class_code, course_name)`.
     - `idx_enrollments_status` trên `enrollments(status)`.
2. **Migration `profile-service`**: [V6__add_candidate_profiles_class_course_index.sql](../../backend/profile-service/src/main/resources/db/migration/V6__add_candidate_profiles_class_course_index.sql)
   - Đồng bộ bổ sung chỉ mục tìm kiếm `idx_candidate_profiles_class_course` trên bảng `candidate_profiles` của `profile_db`.
3. **Dữ liệu Seed Data mẫu**: [infra/seed/05_seed_learning_db.sql](../../infra/seed/05_seed_learning_db.sql)
   - Khởi tạo 40 học viên mẫu trải đều trên 4 lớp học (`RA-HN-JV-01`, `RA-DN-FE-01`, `RA-SG-FS-01`, `RA-HN-TEST-01`) và 4 khóa đào tạo tương ứng.
   - Dữ liệu gap analysis và enrollments được thiết lập chuẩn xác để kết quả Top 5 kỹ năng thiếu khớp chính xác với yêu cầu nghiệp vụ.

### 2.2. Backend `learning-service` (Port: 8089)
1. **Mô hình DTO**:
   - [CandidateInfoDto.java](../../backend/learning-service/src/main/java/com/example/learningservice/model/dto/response/staff/CandidateInfoDto.java): Thông tin học viên (id, fullName, email, phone).
   - [MissingSkillDto.java](../../backend/learning-service/src/main/java/com/example/learningservice/model/dto/response/staff/MissingSkillDto.java): Kỹ năng thiếu hụt (skillId, skillName, required, current, gap, priority).
   - [EnrollmentProgressDto.java](../../backend/learning-service/src/main/java/com/example/learningservice/model/dto/response/staff/EnrollmentProgressDto.java): Môn học LMS, tiến độ %, điểm số, trạng thái.
   - [UpskillingCandidateDto.java](../../backend/learning-service/src/main/java/com/example/learningservice/model/dto/response/staff/UpskillingCandidateDto.java): DTO dòng ứng viên đầy đủ (candidate, classCode, courseName, target, missingSkills, enrollments, readiness, status).
   - [TopMissingSkillDto.java](../../backend/learning-service/src/main/java/com/example/learningservice/model/dto/response/staff/TopMissingSkillDto.java): Thống kê kỹ năng thiếu (skillId, skillName, missingCount, percentage).
   - [UpskillingSummaryResponse.java](../../backend/learning-service/src/main/java/com/example/learningservice/model/dto/response/staff/UpskillingSummaryResponse.java): Thống kê tổng quan (`totalStudying`, `totalCompleted`, `topMissingSkills`).
2. **Truy vấn gộp & Xử lý nghiệp vụ**:
   - [GapResultRepository.java](../../backend/learning-service/src/main/java/com/example/learningservice/repository/GapResultRepository.java): Bổ sung phương thức native query `findTopMissingSkills(Pageable)` thống kê Top 5 kỹ năng bị thiếu nhiều nhất.
   - [StaffUpskillingService.java](../../backend/learning-service/src/main/java/com/example/learningservice/service/StaffUpskillingService.java) & [StaffUpskillingServiceImpl.java](../../backend/learning-service/src/main/java/com/example/learningservice/service/impl/StaffUpskillingServiceImpl.java): Thực hiện truy vấn gộp in-memory, ghép nối các bảng dữ liệu, lọc theo lớp (`classCode`), khóa (`courseName`), trạng thái (`status`), kỹ năng (`skillId`), tính toán tỷ lệ sẵn sàng `readiness` và hỗ trợ phân trang chuẩn.
3. **Dịch vụ xuất Excel OpenXML Zero-Dependency**:
   - [ExcelExportService.java](../../backend/learning-service/src/main/java/com/example/learningservice/service/ExcelExportService.java): Tự xây dựng file `.xlsx` chuẩn nén ZIP (`ZipOutputStream`), tạo cấu trúc thư mục OpenXML (`xl/worksheets/sheet1.xml`, `xl/styles.xml`, `xl/workbook.xml`, `[Content_Types].xml`), hỗ trợ tiếng Việt có dấu, định dạng border, màu header (#1E40AF), căn lề và format số/%.
4. **Bảo mật & RBAC**:
   - [UserContextHelper.java](../../backend/learning-service/src/main/java/com/example/learningservice/security/UserContextHelper.java): Thêm hàm `requireStaffOrAdmin(HttpServletRequest)` chặn quyền: trả về HTTP `401 Unauthorized` nếu chưa đăng nhập, `403 Forbidden` nếu là `HR` hoặc `CANDIDATE`.
5. **REST Controller**:
   - [StaffUpskillingController.java](../../backend/learning-service/src/main/java/com/example/learningservice/controller/StaffUpskillingController.java) cung cấp các endpoints:
     - `GET /api/v1/staff/upskilling` (và alias `/staff/upskilling`): Danh sách ứng viên có lọc và phân trang.
     - `GET /api/v1/staff/upskilling/summary` (và alias `/staff/upskilling/summary`): Thống kê tổng hợp số lượng & top 5 kỹ năng thiếu.
     - `GET /api/v1/staff/upskilling/export` (và alias `/staff/upskilling/export`): Xuất báo cáo file Excel `.xlsx` theo bộ lọc.

### 2.3. Cổng API Gateway & Mock Server
1. **API Gateway**: Cập nhật [backend/api-gateway/src/main/resources/application.yaml](../../backend/api-gateway/src/main/resources/application.yaml) định tuyến các request `/api/v1/staff/upskilling/**` và `/staff/upskilling/**` sang cổng `:8089`.
2. **Frontend Proxy**: Cấu hình `vite.config.ts` chuyển tiếp `/api/v1/staff/upskilling` sang cổng `:8089`.
3. **Mock Server**: Cập nhật [mock/server.js](../../mock/server.js) bổ sung đầy đủ mock data, logic lọc, RBAC và endpoint xuất Excel.
4. **Script E2E**: Xây dựng [mock/test-dt04-e2e.js](../../mock/test-dt04-e2e.js) kiểm thử độc lập 4 tiêu chí nghiệm thu.

### 2.4. Giao diện người dùng Frontend (React 19 + TypeScript + Tailwind CSS)
1. **API Client**: [learning.api.ts](../../frontend/src/api/endpoints/learning.api.ts) bổ sung các hàm `getStaffUpskillingList`, `getStaffUpskillingSummary`, `exportStaffUpskillingExcel` và định nghĩa kiểu dữ liệu TypeScript.
2. **Thành phần giao diện chuyên biệt**:
   - [UpskillingSummaryCards.tsx](../../frontend/src/features/learning/components/staff/UpskillingSummaryCards.tsx): 3 thẻ KPI trực quan (Đang học bổ sung, Đã hoàn thành khóa học, Kỹ năng thiếu phổ biến nhất).
   - [TopMissingSkillsChart.tsx](../../frontend/src/features/learning/components/staff/TopMissingSkillsChart.tsx): Biểu đồ cột ngang hiển thị Top 5 kỹ năng thiếu, hỗ trợ click vào từng cột để tự động lọc danh sách theo kỹ năng đó.
   - [UpskillingFilterBar.tsx](../../frontend/src/features/learning/components/staff/UpskillingFilterBar.tsx): Thanh công cụ lọc linh hoạt theo Mã lớp, Khóa đào tạo, Trạng thái (Đang học/Hoàn thành), Kỹ năng thiếu, kèm nút Làm mới và nút **"Xuất Excel"**.
   - [UpskillingTable.tsx](../../frontend/src/features/learning/components/staff/UpskillingTable.tsx): Bảng dữ liệu hiển thị ứng viên, lớp, khóa, mục tiêu, các môn LMS đang học kèm thanh tiến độ %, badge mức độ ưu tiên kỹ năng thiếu, cột Tỷ lệ sẵn sàng kèm màu cảnh báo và phân trang.
   - [CandidateLearningPathDrawer.tsx](../../frontend/src/features/learning/components/staff/CandidateLearningPathDrawer.tsx): Drawer bên phải mở ra khi bấm vào một dòng bất kỳ trên bảng để xem chi tiết toàn bộ lộ trình học tập, từng môn LMS và khoảng cách năng lực của học viên.
3. **Trang chức năng chính**:
   - [StaffUpskillingPage.tsx](../../frontend/src/pages/staff/StaffUpskillingPage.tsx) tại tuyến đường `/staff/upskilling`.
4. **Điều hướng hệ thống**:
   - Đăng ký route trong [AppRoutes.tsx](../../frontend/src/routes/AppRoutes.tsx).
   - Tích hợp liên kết *"Đào tạo bổ sung"* vào [Sidebar.tsx](../../frontend/src/components/layout/Sidebar.tsx) và [AppHeader.tsx](../../frontend/src/components/layout/AppHeader.tsx) dành riêng cho vai trò Cán bộ.

---

## 3. Bảng đối chiếu 4 Tiêu chí nghiệm thu (Acceptance Criteria Matrix)

| Mã AC | Tiêu chí nghiệm thu của Lead | Hiện thực kỹ thuật | Kết quả kiểm thử | Đánh giá |
|:---:|---|---|---|:---:|
| **AC-1** | **Lọc theo lớp ra đúng ứng viên của lớp đó** | Endpoint tiếp nhận query `?classCode=...`, lọc qua trường `candidate_profiles.class_code` và hỗ trợ cả lọc `courseName`, `status`, `skillId`. | - Lọc `RA-HN-JV-01` ra chính xác 100% học viên lớp Java.<br>- Lọc `RA-DN-FE-01` ra đúng học viên lớp Frontend.<br>- Đã xác minh qua Unit test và E2E script. | **ĐẠT (PASS)** |
| **AC-2** | **Top kỹ năng thiếu khớp dữ liệu seed** | Endpoint `GET /staff/upskilling/summary` truy vấn thống kê từ `gap_results` và trả về danh sách 5 kỹ năng thiếu nhiều nhất. | Kết quả trả về đúng chuẩn seed:<br>1. TypeScript: 10 học viên<br>2. Java Core: 8 học viên<br>3. React: 7 học viên<br>4. Spring Boot: 7 học viên<br>5. SQL: 5 học viên | **ĐẠT (PASS)** |
| **AC-3** | **File Excel khớp với bảng đang lọc** | Endpoint `GET /staff/upskilling/export` tái sử dụng toàn bộ logic lọc (`classCode`, `courseName`, `status`, `skillId`), xuất file `.xlsx` nén hợp lệ kèm dữ liệu khớp bảng. | Tải file `.xlsx` thành công, MIME type `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, cấu trúc bảng tính chuẩn xác, dữ liệu lọc khớp 100%. | **ĐẠT (PASS)** |
| **AC-4** | **HR hoặc CANDIDATE gọi API $\rightarrow$ 403** | `UserContextHelper.requireStaffOrAdmin` kiểm tra `X-User-Role` và `Authorization` token: chỉ cho phép `STAFF` hoặc `ADMIN`. | - Chưa đăng nhập $\rightarrow$ **401 Unauthorized**.<br>- Role `HR` $\rightarrow$ **403 Forbidden**.<br>- Role `CANDIDATE` $\rightarrow$ **403 Forbidden**.<br>- Role `STAFF`/`ADMIN` $\rightarrow$ **200 OK**. | **ĐẠT (PASS)** |

---

## 4. Kết quả kiểm thử tự động (Automated Test Execution)

### 4.1. Backend Unit & Integration Tests (`learning-service`)
- Lệnh chạy: `./gradlew test`
- Số lượng test cases:
  - `StaffUpskillingControllerTest.java`: 8 tests (Phân quyền RBAC 401/403/200, lọc query params, summary, export Excel).
  - `StaffUpskillingServiceTest.java`: 5 tests (Truy vấn gộp, tính readiness, lọc lớp, top 5 kỹ năng).
  - `ExcelExportServiceTest.java`: 2 tests (Cấu trúc ZIP OpenXML, nội dung XML).
  - `CandidateProfileRepositoryTest.java`: 2 tests (Lưu và truy vấn theo class_code).
- Kết quả: **`BUILD SUCCESSFUL in 1m` (100% PASS, 0 failure)**.

### 4.2. E2E Script Verification (`mock/test-dt04-e2e.js`)
- Lệnh chạy: `node mock/test-dt04-e2e.js`
- Kết quả kiểm thử: **11/11 Test cases thành công (100% PASS)**:
  - `[PASS] 4.1 Không có token đăng nhập -> 401 Unauthorized`
  - `[PASS] 4.2 CANDIDATE gọi API -> 403 Forbidden`
  - `[PASS] 4.3 HR gọi API Summary -> 403 Forbidden`
  - `[PASS] 4.4 STAFF gọi API -> 200 OK`
  - `[PASS] 1.1 Lọc classCode=RA-HN-JV-01 ra đúng ứng viên của lớp đó`
  - `[PASS] 1.2 Lọc classCode=RA-DN-FE-01 ra đúng ứng viên của lớp đó`
  - `[PASS] 1.3 Lọc courseName=Java Fresher ra đúng các ứng viên khoá Java`
  - `[PASS] 2.1 API Summary trả về 200 OK và có dữ liệu`
  - `[PASS] 2.2 Top 5 kỹ năng thiếu khớp chính xác dữ liệu seed`
  - `[PASS] 3.1 Xuất Excel thành công với MIME type openxmlformats`
  - `[PASS] 3.2 Header Content-Disposition có tên file .xlsx`

### 4.3. Frontend Production Build Check
- Lệnh chạy: `npm --prefix frontend run build`
- Kết quả: **`✓ built in 5.49s` (0 TypeScript errors, 0 warnings)**.
- Bundle output: Tạo thành công file phân mảnh `StaffUpskillingPage-BiB8Z2DN.js` (24.08 kB).

---

## 5. Hướng dẫn khởi chạy & Thao tác kiểm thử

### 5.1. Khởi động môi trường:
```bash
# 1. Khởi động Mock Server (Port 9000)
node mock/server.js

# 2. Khởi động Backend Learning Service (Port 8089)
cd backend/learning-service
./gradlew bootRun

# 3. Khởi động API Gateway (Port 8080)
cd backend/api-gateway
./gradlew bootRun

# 4. Khởi động Frontend React (Port 5173)
cd frontend
npm run dev
```

### 5.2. Các bước kiểm thử trực tiếp trên giao diện:
1. Mở trình duyệt tại địa chỉ `http://localhost:5173/login`.
2. Đăng nhập với tài khoản Cán bộ (Role: `STAFF`).
3. Truy cập vào menu **"Đào tạo bổ sung"** (hoặc truy cập trực tiếp `http://localhost:5173/staff/upskilling`).
4. **Kiểm tra AC-2**: Quan sát 3 thẻ tổng hợp và biểu đồ Top 5 kỹ năng thiếu:
   - Thẻ hiển thị số lượng học viên đang học và đã hoàn thành.
   - Biểu đồ cột hiển thị Top 5 kỹ năng: TypeScript (10), Java Core (8), React (7), Spring Boot (7), SQL (5).
5. **Kiểm tra AC-1**:
   - Tại thanh bộ lọc, chọn Lớp học `RA-HN-JV-01` $\rightarrow$ Danh sách lập tức chỉ hiển thị học viên của lớp này.
   - Bấm vào một cột kỹ năng trên biểu đồ (ví dụ: `React`) $\rightarrow$ Bảng tự động lọc ra các ứng viên đang thiếu kỹ năng React.
6. **Kiểm tra Xem Lộ Trình**:
   - Bấm vào bất kỳ dòng nào trong bảng $\rightarrow$ Drawer trượt ra từ bên phải hiển thị chi tiết tiến độ từng môn học LMS và các kỹ năng thiếu hụt của học viên.
7. **Kiểm tra AC-3**:
   - Chọn bộ lọc (ví dụ: lớp `RA-HN-JV-01`), bấm nút **"Xuất Excel"** $\rightarrow$ File `.xlsx` được tải về máy. Mở file để kiểm tra tiêu đề, màu sắc và các cột dữ liệu khớp với bộ lọc đang chọn.
8. **Kiểm tra AC-4**:
   - Đăng xuất và đăng nhập lại bằng tài khoản `HR` hoặc `CANDIDATE`.
   - Cố tình truy cập vào `/staff/upskilling` hoặc dùng Postman/curl gọi API $\rightarrow$ Hệ thống lập tức trả về `403 Forbidden`.
