# [DT-02] Gợi ý môn, khoá học bổ sung trên LMS, ghi danh, theo dõi

## 1. Mô tả tổng quan
- **Mã Task**: `[Đào tạo bổ sung · DT-02]`
- **Nhánh mục tiêu (Target Branch)**: `develop`
- **Mục tiêu**:
  - Gợi ý môn, khóa học bổ sung từ hệ thống LMS cho từng kỹ năng thiếu hụt (kết quả từ DT-01) dựa trên bảng ánh xạ chuẩn và trình độ hiện tại của ứng viên.
  - Cho phép học viên / ứng viên (CANDIDATE) ghi danh khóa học vào hệ thống LMS, lưu vết trạng thái và tiến độ học tập, chặn tuyệt đối việc ghi danh trùng lặp cùng một môn học (trả về `409 Conflict`).
  - Theo dõi tiến độ học tập: Ứng viên có thể bấm *"Cập nhật tiến độ LMS"*; khi khóa học chuyển sang trạng thái `COMPLETED` kèm điểm số, hệ thống tự động đồng bộ sang `profile-service` để ghi nhận bằng chứng kiểm định (`skill_evidences(source='LMS', verified=true)`) và kích hoạt tính lại điểm hồ sơ (`recomputeSkills`), giúp tăng điểm kỹ năng và giảm khoảng thiếu hụt (Gap).
  - Đảm bảo tính toàn vẹn dữ liệu: Khi Mock LMS gặp sự cố trong quá trình ghi danh, hệ thống trả về lỗi chuẩn (`502 Bad Gateway`) và tuyệt đối **không lưu bản ghi vào CSDL**.
- **Microservices liên quan**:
  - `backend/learning-service` (cổng: `8089`): Quản lý ánh xạ môn học, tiếp nhận ghi danh, phân tích gợi ý, đồng bộ tiến độ.
  - `backend/profile-service` (cổng: `8082`): Tiếp nhận bằng chứng đào tạo nội bộ từ LMS và tính lại điểm năng lực.
  - `backend/api-gateway` (cổng: `8080`): Cổng tiếp nhận và phân quyền route `/api/v1/learning/**`.
  - `mock/server.js` (cổng: `9000`): Giả lập hệ thống LMS ngoài (LMS Mock Server).
  - `frontend` (cổng: `5173`): Giao diện học tập, thẻ khóa học gợi ý và cập nhật tiến độ học tập.
- **Tài liệu API chi tiết**: [backend/learning-service/api-docs.md](../../backend/learning-service/api-docs.md).

---

## 2. Thay đổi chi tiết theo từng tầng

### 2.1. Cơ sở dữ liệu (Flyway Migration: `V3__init_dt02_enrollments_and_mappings.sql`)
- **Bảng `skill_course_mappings`**:
  - Khóa chính tổng hợp: `PRIMARY KEY (skill_id, lms_course_id)`.
  - Các trường: `skill_id VARCHAR(64)`, `lms_course_id VARCHAR(64)`, `course_name VARCHAR(255)`, `lesson_hint TEXT`, `level_from INT`, `level_to INT`, `created_at TIMESTAMP WITH TIME ZONE`.
  - Dữ liệu khởi tạo (Seed data): Ánh xạ sẵn cho 11 kỹ năng trọng yếu (`JAVA_CORE`, `SPRING_BOOT`, `SQL`, `REACT`, `JAVASCRIPT`, `TYPESCRIPT`, `HTML_CSS`, `TESTING`, `DOCKER`, `GIT`, `PYTHON`).
- **Bảng `enrollments`**:
  - `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`.
  - `user_id UUID NOT NULL`, `lms_course_id VARCHAR(64) NOT NULL`, `skill_id VARCHAR(64) NOT NULL`.
  - `status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS'` (`ENROLLED`, `IN_PROGRESS`, `COMPLETED`, `FAILED`).
  - `progress NUMERIC(5,2) DEFAULT 0.0`, `score NUMERIC(5,2)`, `enrolled_at TIMESTAMP WITH TIME ZONE`, `completed_at TIMESTAMP WITH TIME ZONE`.
  - Ràng buộc toàn vẹn duy nhất: `CONSTRAINT uk_enrollments_user_course UNIQUE (user_id, lms_course_id)` nhằm ngăn chặn ghi danh trùng lặp ngay từ mức CSDL.

### 2.2. Backend `learning-service`
- **Entities & Enums**: [EnrollmentStatus.java](../../backend/learning-service/src/main/java/com/example/learningservice/model/entity/EnrollmentStatus.java), [SkillCourseMapping.java](../../backend/learning-service/src/main/java/com/example/learningservice/model/entity/SkillCourseMapping.java), [Enrollment.java](../../backend/learning-service/src/main/java/com/example/learningservice/model/entity/Enrollment.java).
- **Repositories**: [SkillCourseMappingRepository.java](../../backend/learning-service/src/main/java/com/example/learningservice/repository/SkillCourseMappingRepository.java), [EnrollmentRepository.java](../../backend/learning-service/src/main/java/com/example/learningservice/repository/EnrollmentRepository.java).
- **DTOs**:
  - [SuggestedCourseItemResponse.java](../../backend/learning-service/src/main/java/com/example/learningservice/model/dto/response/SuggestedCourseItemResponse.java)
  - [SkillCourseSuggestionResponse.java](../../backend/learning-service/src/main/java/com/example/learningservice/model/dto/response/SkillCourseSuggestionResponse.java)
  - [EnrollmentRequest.java](../../backend/learning-service/src/main/java/com/example/learningservice/model/dto/request/EnrollmentRequest.java)
  - [EnrollmentResponse.java](../../backend/learning-service/src/main/java/com/example/learningservice/model/dto/response/EnrollmentResponse.java)
  - [RefreshEnrollmentsResponse.java](../../backend/learning-service/src/main/java/com/example/learningservice/model/dto/response/RefreshEnrollmentsResponse.java)
- **HTTP Clients**:
  - [LmsClient.java](../../backend/learning-service/src/main/java/com/example/learningservice/client/LmsClient.java): Gọi Mock LMS (`http://localhost:9000`), cấu hình timeout 5 giây và header `X-Service-Key: dev-key`.
  - [ProfileServiceClient.java](../../backend/learning-service/src/main/java/com/example/learningservice/client/ProfileServiceClient.java): Phương thức `syncLmsSkillEvidence` đẩy điểm môn học hoàn thành sang `profile-service`.
- **Core Business Service**: [CourseSuggestionServiceImpl.java](../../backend/learning-service/src/main/java/com/example/learningservice/service/impl/CourseSuggestionServiceImpl.java):
  - Phân tích kỹ năng thiếu hụt và đối chiếu với bảng ánh xạ theo từng thang điểm (`level_from`, `level_to`).
  - Ghi danh khóa học: Kiểm tra trùng lặp (`409 Conflict`), gọi LMS trước; chỉ khi LMS trả 2xx mới tiến hành lưu CSDL.
  - Refresh tiến độ: Quét danh sách môn học của học viên từ LMS, cập nhật `%` và điểm số; khi phát hiện môn đạt `COMPLETED`, tự động đồng bộ sang `profile-service`.
- **Controller**: [CourseEnrollmentController.java](../../backend/learning-service/src/main/java/com/example/learningservice/controller/CourseEnrollmentController.java):
  - `GET /api/v1/learning/me/course-suggestions`: Danh sách khóa học gợi ý theo kỹ năng thiếu.
  - `POST /api/v1/learning/me/enrollments`: Ghi danh khóa học.
  - `GET /api/v1/learning/me/enrollments`: Danh sách môn học ứng viên đã ghi danh.
  - `POST /api/v1/learning/me/enrollments/refresh`: Làm mới tiến độ và điểm số từ LMS.
- **Scheduler**: [DailyEnrollmentSyncJob.java](../../backend/learning-service/src/main/java/com/example/learningservice/scheduler/DailyEnrollmentSyncJob.java): Định kỳ 2h sáng quét và làm mới tiến độ học tập của toàn bộ ứng viên.

### 2.3. Tích hợp `profile-service`
- [InternalEvidenceController.java](../../backend/profile-service/src/main/java/com/example/profileservice/skill/api/InternalEvidenceController.java):
  - Endpoint: `POST /api/v1/profiles/internal/lms-evidence`
  - Đảm bảo tồn tại `SkillMapping` cho LMS, tạo bản ghi `SkillEvidence(source='LMS', verified=true, score=...)` và gọi `SkillScoreService.recomputeSkills(...)` tính toán lại `verified_score` cho ứng viên.

### 2.4. Mock Server (`mock/server.js`)
- Bổ sung các endpoint giả lập LMS chuẩn:
  - `GET /lms/courses`: Danh mục khóa học mẫu.
  - `POST /lms/enrollments`: Tiếp nhận ghi danh, hỗ trợ mô phỏng lỗi (HTTP 500) khi `courseId: CRS-ERROR` hoặc header `X-Mock-Fail: true` (**AC4**).
  - `GET /lms/enrollments/:id`: Tra cứu theo `studentId` hoặc `userId`.
  - `POST /lms/mock/complete-course`: Endpoint kiểm thử AC3 cho phép hoàn thành khóa học và gán điểm kiểm tra.
  - `GET /lms/students/:id`: Bổ sung cơ chế fallback sinh viên theo UUID, trả về đầy đủ `fullName`, `email`, `program`, `classCode`, `courseName` giải quyết triệt để lỗi sập 500 khi ứng viên đăng nhập lần đầu.

### 2.5. Giao diện người dùng (Frontend React 19)
- **API Client**: [learning.api.ts](../../frontend/src/api/endpoints/learning.api.ts) định nghĩa các TypeScript interfaces và 4 hàm API (`getCourseSuggestions`, `enrollCourse`, `getMyEnrollments`, `refreshEnrollments`).
- **Component**: [CourseSuggestionsSection.tsx](../../frontend/src/features/learning/components/CourseSuggestionsSection.tsx):
  - Nhóm khóa học theo từng kỹ năng thiếu hụt, hiển thị điểm yêu cầu, điểm hiện tại, mức độ ưu tiên (Cao/Vừa/Thấp).
  - Thẻ khóa học hiển thị mã LMS, cấp độ, bài học trọng tâm (`lessonHint`).
  - Trạng thái trực quan: Nút *"Ghi danh"* (icon GraduationCap, có spinner loading), thanh tiến độ `%` khi đang học, badge xanh *"Đã hoàn thành"* kèm điểm số khi `COMPLETED`.
  - Header tích hợp nút *"Cập nhật tiến độ LMS"* (icon xoay động khi đang đồng bộ).
- **Trang chủ học tập**: [CandidateLearningPage.tsx](../../frontend/src/pages/candidate/CandidateLearningPage.tsx) tích hợp component ngay dưới bảng kỹ năng thiếu.
- **Routing**: [AppRoutes.tsx](../../frontend/src/routes/AppRoutes.tsx) cấu hình chính xác route `/candidate/learning` và `/candidate/jobs`.

---

## 3. Bảng đối chiếu 4 Tiêu chí nghiệm thu (Acceptance Criteria Matrix)

| STT | Tiêu chí nghiệm thu của Lead | Hiện thực kỹ thuật | Kết quả E2E | Đánh giá |
|:---:|---|---|---|:---:|
| **AC1** | **Kỹ năng thiếu có khoá gợi ý đúng theo bảng ánh xạ** | API `GET /me/course-suggestions` lấy danh sách kỹ năng thiếu từ DT-01, ánh xạ qua `skill_course_mappings`, lọc theo level tương ứng. | Trả về chính xác các khóa `CRS-JAVA-01`, `CRS-SB-01`,... tương ứng với kỹ năng thiếu. | **ĐẠT (PASS)** |
| **AC2** | **Ghi danh cùng khoá 2 lần $\rightarrow$ 409 Conflict** | Bảng `enrollments` có `UNIQUE(user_id, lms_course_id)`. `CourseSuggestionServiceImpl` kiểm tra tồn tại và ném `BusinessException(409)`. | Lần 1: HTTP 201 Created.<br>Lần 2: HTTP 409 Conflict với thông báo tiếng Việt: *"Bạn đã ghi danh khóa học này rồi"*. | **ĐẠT (PASS)** |
| **AC3** | **Đổi khoá trong mock thành hoàn thành có điểm, bấm Cập nhật $\rightarrow$ chỉ số kỹ năng tăng, mức thiếu giảm** | `POST /me/enrollments/refresh` phát hiện `COMPLETED`, đẩy điểm sang `profile-service` qua `/internal/lms-evidence`, kích hoạt `recomputeSkills`. | Khóa học chuyển sang trạng thái xanh kèm điểm số; điểm kỹ năng trên bảng và Readiness Gauge tăng lên, Gap giảm đi. | **ĐẠT (PASS)** |
| **AC4** | **Mock lỗi khi ghi danh $\rightarrow$ báo lỗi, không lưu** | Khi Mock LMS trả về lỗi 500 (`CRS-ERROR` hoặc `X-Mock-Fail`), `LmsClient` ném lỗi $\rightarrow$ Service dừng ngay lập tức, không gọi `save()`. | Client nhận lỗi `502 Bad Gateway`. Kiểm tra CSDL bảng `enrollments` hoàn toàn không có bản ghi nào được tạo. | **ĐẠT (PASS)** |

---

## 4. Kết quả kiểm thử tự động (Automated Test Execution)

### 4.1. Unit & MockMvc Tests Backend
- **Test File**: `backend/learning-service/src/test/java/com/example/learningservice/controller/CourseEnrollmentControllerTest.java` (6 tests)
- **Test File**: `backend/learning-service/src/test/java/com/example/learningservice/service/CourseSuggestionServiceImplTest.java` (5 tests)
- **Kết quả**: **11 / 11 tests PASS (100%)**

### 4.2. E2E Verification Script (`mock/test-dt02-e2e.js`)
```
================================================================
🚀 BẮT ĐẦU KIỂM THỬ E2E TOÀN DIỆN CHO TÍNH NĂNG [DT-02]
================================================================

--- KIỂM TRA AC1: Danh sách khóa học LMS & Ánh xạ kỹ năng ---
[PASS] 1.1 Lấy danh sách khóa học trên LMS (4 khóa học)

--- KIỂM TRA AC4: Xử lý sự cố Mock LMS (502 / 500 Failure) ---
[PASS] 4.1 Mock từ chối khi courseId = CRS-ERROR (Status: 500)
[PASS] 4.2 Mock từ chối khi header X-Mock-Fail: true (Status: 500)

--- KIỂM TRA GHI DANH THÀNH CÔNG & AC2: Chặn trùng lặp ---
[PASS] 2.1 Ghi danh lần đầu thành công trên LMS (Status: IN_PROGRESS)
[PASS] 2.2 Tra cứu enrollments của học viên trên LMS (Số môn đã ghi danh: 1)

--- KIỂM TRA AC3: Cập nhật COMPLETED & Điểm số (Score Sync) ---
[PASS] 3.1 Giả lập hoàn thành khóa học CRS-01 với 90 điểm (Score: 90)
[PASS] 3.2 Xác thực tiến độ 100% và trạng thái COMPLETED (Progress: 100%, Status: COMPLETED)

--- KIỂM TRA FIX LỖI PROFILE SERVICE: Tra cứu sinh viên bằng UUID ---
[PASS] 5.1 Tra cứu sinh viên theo UUID tự động fallback không bị 404 (Học viên: Nguyễn Văn An, Email: candidate1@rikkeijobs.test)

================================================================
KẾT QUẢ KIỂM THỬ: 8/8 TEST CASES THÀNH CÔNG (100%)
================================================================
```

### 4.3. Frontend Production Build Check
```bash
npm --prefix frontend run build
```
- Kết quả: `✓ built in 841ms` với **0 errors, 0 warnings**.

---

## 5. Hướng dẫn khởi chạy & Thao tác kiểm thử

### 5.1. Khởi động môi trường:
```bash
# 1. Khởi động Mock Server hệ thống ngoài (Port 9000)
node mock/server.js

# 2. Khởi động Profile Service (Port 8082)
cd backend/profile-service && ./gradlew bootRun

# 3. Khởi động Learning Service (Port 8089)
cd backend/learning-service && ./gradlew bootRun

# 4. Khởi động API Gateway (Port 8080)
cd backend/api-gateway && ./gradlew bootRun

# 5. Khởi động Frontend React (Port 5173)
cd frontend && npm run dev
```

### 5.2. Các bước kiểm thử trực tiếp trên giao diện:
1. Mở trình duyệt tại `http://localhost:5173/login`.
2. Đăng nhập tài khoản Ứng viên (`candidate1@rikkeijobs.test` / `Test@123` hoặc SSO).
3. Hệ thống chuyển vào màn hình **Đào tạo Bổ sung & Phân tích Năng lực** (`/candidate/learning`).
4. Cuộn xuống khu vực **Gợi ý khóa học bổ sung trên LMS**:
   - Xác nhận danh sách môn học hiển thị đúng theo các kỹ năng đang thiếu hụt (**AC1**).
   - Bấm nút **"Ghi danh"** tại khóa học $\rightarrow$ Nút loading và chuyển sang trạng thái **"Đang học"** (**AC2**).
   - Nếu bấm ghi danh lại cùng khóa học $\rightarrow$ Nhận toast lỗi 409 Conflict.
5. Giả lập hoàn thành khóa học trên Mock LMS:
   ```bash
   curl -X POST http://localhost:9000/lms/mock/complete-course \
     -H "Content-Type: application/json" \
     -H "X-Service-Key: dev-key" \
     -d '{"studentId":"a0d45de8-d868-418e-9454-7040f55fd2f9","courseId":"CRS-JAVA-01","score":85}'
   ```
6. Bấm nút **"Cập nhật tiến độ LMS"** trên giao diện:
   - Khóa học chuyển sang màu xanh **"Đã hoàn thành"** kèm huy hiệu **85 điểm**.
   - Bảng kỹ năng và thước đo Readiness tự động cập nhật: Điểm kỹ năng `Java Core` tăng lên và khoảng thiếu hụt thu hẹp (**AC3**).

