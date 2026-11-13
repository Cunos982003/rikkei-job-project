# [DT-01] Phân tích khoảng thiếu hụt năng lực theo JD và vị trí mục tiêu

## 1. Mô tả tổng quan
- **Mã Task**: `[Đào tạo bổ sung · DT-01]`
- **Mục tiêu**: 
  - Cho phép học viên / ứng viên (CANDIDATE) lựa chọn vị trí mục tiêu nghề nghiệp chuẩn hóa (4 lộ trình đào tạo: Java Backend Fresher, Frontend ReactJS Fresher, Tester, Fullstack) hoặc chọn một tin tuyển dụng (JD) cụ thể để làm mục tiêu phát triển.
  - Xây dựng **Core Engine Phân tích Năng lực**: Tự động so sánh hồ sơ năng lực thực tế đã kiểm chứng qua LMS/Khảo thí (`verified_score`) với bộ kỹ năng yêu cầu của vị trí hoặc JD, tính toán điểm sẵn sàng (`readiness` %), xác định mức độ ưu tiên (`HIGH`, `MEDIUM`, `LOW`), loại bỏ kỹ năng đã đạt chuẩn và sắp xếp khoảng thiếu (`gap`) giảm dần.
  - Cung cấp giao diện trực quan tại route `/candidate/learning` và nút **"Phân tích so với JD này"** tại danh sách / chi tiết việc làm `/candidate/jobs`.
- **Microservice Backend**: `backend/learning-service` (cổng nội bộ: `8089`).
- **Cổng API Gateway chính thức**: `backend/api-gateway` (cổng: `8080`).
- **Frontend SPA**: `frontend` (React 19 + TypeScript + Vite + Tailwind CSS v4).
- **Tài liệu OpenAPI**: [docs/api/openapi/learning-service-dt01.yaml](../api/openapi/learning-service-dt01.yaml).
- **Tài liệu chi tiết API**: [backend/learning-service/api-docs.md](../../backend/learning-service/api-docs.md).

---

## 2. Thay đổi chi tiết

### 2.1. Cơ sở dữ liệu (Flyway Migration: `V1__init_learning_gap.sql`)
- **Bảng `target_positions`**: Lưu danh mục 4 vị trí mục tiêu định nghĩa sẵn (`JAVA_BACKEND_FRESHER`, `FRONTEND_REACT_FRESHER`, `TESTER`, `FULLSTACK`).
- **Bảng `position_skills`**: Lưu danh sách kỹ năng chuẩn của từng vị trí kèm `min_score` và cờ bắt buộc `is_mandatory`.
- **Bảng `candidate_targets`**: Lưu mục tiêu nghề nghiệp của từng ứng viên theo `user_id` (chọn `POSITION` hoặc `JOB`).
- **Bảng `gap_results`**: Lưu trữ snapshot kết quả phân tích khoảng thiếu hụt năng lực của từng ứng viên.

### 2.2. Danh sách REST API Backend (Qua Gateway `:8080`)
Toàn bộ phản hồi tuân thủ 100% Envelope Pattern `ApiResponse<T>` với thông báo tiếng Việt theo quy chuẩn dự án:

| STT | Phương thức | Đường dẫn qua Gateway `:8080` | Phân quyền | Mô tả chi tiết |
|:---:|:---:|---|:---:|---|
| 1 | `GET` | `/api/v1/learning/target-positions` | Public | Lấy danh mục 4 vị trí mục tiêu chuẩn hóa và bộ kỹ năng sàn |
| 2 | `GET` | `/api/v1/learning/target-positions/{id}` | Public | Xem chi tiết yêu cầu điểm sàn của một vị trí mục tiêu |
| 3 | `PUT` | `/api/v1/learning/me/target` | `CANDIDATE` | Thiết lập hoặc thay đổi mục tiêu nghề nghiệp (`positionId` hoặc `jobId`) |
| 4 | `GET` | `/api/v1/learning/me/target` | `CANDIDATE` | Lấy mục tiêu nghề nghiệp hiện tại của ứng viên đăng nhập |
| 5 | `GET` | `/api/v1/learning/me/gap-analysis` | `CANDIDATE` | Phân tích khoảng thiếu hụt năng lực theo mục tiêu hoặc `?jobId=` ghi đè |

### 2.3. Quy tắc cốt lõi của Core Engine (Gap Analysis)
1. **Điểm hiện tại (`current`)**: Lấy từ `verified_score` qua `profile-service` (nếu chưa có thì mặc định bằng 0).
2. **Loại trừ kỹ năng đạt**: Kỹ năng có `current >= required` (tức `gap <= 0`) **không xuất hiện** trong bảng thiếu hụt.
3. **Độ ưu tiên (`priority`)**:
   - `HIGH`: Kỹ năng bắt buộc (`isMandatory == true`) **VÀ** thiếu > 20 điểm (`gap > 20`).
   - `MEDIUM`: Thiếu > 10 điểm (`gap > 10`).
   - `LOW`: Các trường hợp còn lại (`gap <= 10`).
4. **Tỷ lệ sẵn sàng (`readiness`)**: `% kỹ năng đạt = (passedSkillsCount / totalRequiredSkills) * 100.0`.
5. **Sắp xếp**: Danh sách thiếu hụt sắp xếp theo khoảng thiếu (`gap`) giảm dần.

### 2.4. Giao diện người dùng (Frontend React 19)
- **Trang `/candidate/learning` ([CandidateLearningPage.tsx](../../frontend/src/pages/candidate/CandidateLearningPage.tsx))**:
  - **Thước đo sẵn sàng ([ReadinessGaugeCard.tsx](../../frontend/src/features/learning/components/ReadinessGaugeCard.tsx))**: Thanh tiến độ màu sắc động kèm 3 thẻ đo: Tổng số kỹ năng, Đã đạt chuẩn, Còn thiếu hụt.
  - **Bảng kỹ năng cần bồi dưỡng ([SkillGapTable.tsx](../../frontend/src/features/learning/components/SkillGapTable.tsx))**: Hiển thị chi tiết từng kỹ năng thiếu hụt, điểm hiện tại, điểm yêu cầu, khoảng thiếu, badge độ ưu tiên (Đỏ/Vàng/Xanh), cờ bắt buộc và nút *"Học trên LMS"*. Empty state chúc mừng khi sẵn sàng 100%.
  - **Bộ chọn mục tiêu ([TargetSelectorCard.tsx](../../frontend/src/features/learning/components/TargetSelectorCard.tsx))**: 4 thẻ vị trí chuẩn hóa với badge *"Đang chọn"* và tab chuyển sang các JD việc làm mẫu.
  - **Banner phân tích theo JD ([JobAnalysisBanner.tsx](../../frontend/src/features/learning/components/JobAnalysisBanner.tsx))**: Hiển thị khi có tham số `?jobId=...`, kèm nút *"Đặt làm mục tiêu chính"* và *"Quay về mục tiêu đã lưu"*.
- **Trang `/candidate/jobs` ([CandidateJobsPage.tsx](../../frontend/src/pages/candidate/CandidateJobsPage.tsx))**:
  - Danh sách tin tuyển dụng có nút **"Phân tích so với JD này"** với biểu tượng Target, bấm vào sẽ tự động chuyển sang trang phân tích.
- **Màn hình Đăng nhập ([LoginPage.tsx](../../frontend/src/pages/auth/LoginPage.tsx))**:
  - Hỗ trợ đăng nhập SSO tức thì cho Học viên bằng vé LMS một lần kèm nút **"Lấy vé mẫu từ LMS"**.
  - Lưu trữ phiên đăng nhập bền vững với Zustand `persist` và `localStorage`, reload (F5) không bị mất session.

---

## 3. Dữ liệu mẫu (Seed Data)
1. **4 Vị trí mục tiêu chuẩn hóa**:
   - `JAVA_BACKEND_FRESHER` (`11111111-1111-1111-1111-111111111101`): Java Core (75), Spring Boot (70), SQL (70), REST API (70), Git (60).
   - `FRONTEND_REACT_FRESHER` (`11111111-1111-1111-1111-111111111102`): ReactJS (75), TypeScript (70), HTML/CSS (70), Git (65).
   - `TESTER` (`11111111-1111-1111-1111-111111111103`): Manual Testing (75), SQL (70), Postman (70), Test Case Design (75).
   - `FULLSTACK` (`11111111-1111-1111-1111-111111111104`): Java Core (70), Spring Boot (70), ReactJS (70), SQL (70), Git (65).
2. **Tin tuyển dụng mẫu trong Mock Server (:9000)**:
   - `job-001`: Java Backend Developer (Fresher / Junior)
   - `job-002`: Frontend ReactJS Developer
   - `job-003`: QA / Automation Tester

---

## 4. Kết quả nghiệm thu E2E (Acceptance Criteria)

| STT | Tiêu chí nghiệm thu | Kết quả thực tế | Đánh giá |
|:---:|---|---|:---:|
| 1 | **Seed ứng viên mẫu có điểm biết trước** $\rightarrow$ Kết quả thiếu đúng từng điểm | `candidate1` có Java Core 80, Spring Boot 65, SQL 70, Git 60, REST API 0. Đối chiếu Java Backend Fresher: Thiếu REST API (-70) và Spring Boot (-5). Điểm sẵn sàng đúng $60.0\%$. | **ĐẠT** |
| 2 | **Đổi mục tiêu** $\rightarrow$ Kết quả thay đổi tức thì | Chuyển sang Frontend React Fresher hoặc Tester $\rightarrow$ Bảng kỹ năng và thước đo Readiness cập nhật lập tức theo bộ kỹ năng mục tiêu mới. | **ĐẠT** |
| 3 | **Kỹ năng đã đạt KHÔNG xuất hiện trong bảng thiếu** | Các kỹ năng Java Core (80 $\ge$ 75), SQL (70 $\ge$ 70), Git (60 $\ge$ 60) hoàn toàn bị loại khỏi danh sách `items`. | **ĐẠT** |
| 4 | **Phân tích so với JD dùng đúng `min_score` của JD** | Khi gọi API hoặc bấm nút *"Phân tích so với JD này"* (`jobId=job-001`), hệ thống lấy chính xác điểm sàn từ JD để so khớp. | **ĐẠT** |

---

## 5. Hướng dẫn khởi chạy & kiểm thử

### Khởi chạy các dịch vụ:
```bash
# 1. Khởi động Mock Server dịch vụ ngoài (Port 9000)
node mock/server.js

# 2. Khởi động Auth Service (Port 8081)
cd backend/auth-service && ./gradlew bootRun

# 3. Khởi động Learning Service (Port 8089)
cd backend/learning-service && ./gradlew bootRun

# 4. Khởi động API Gateway (Port 8080)
cd backend/api-gateway && ./gradlew bootRun

# 5. Khởi động Frontend React (Port 5173)
cd frontend && npm run dev
```

### Thao tác kiểm thử trên giao diện:
1. Mở `http://localhost:5173/login`.
2. Bấm nút **"Lấy vé mẫu từ LMS"** $\rightarrow$ Bấm **"Xác thực & Đăng nhập SSO"**.
3. Trang tự động chuyển vào `/candidate/learning`.
4. Quan sát tỷ lệ sẵn sàng, bảng kỹ năng thiếu hụt và đổi thử giữa các vị trí mục tiêu.
5. Chuyển sang `/candidate/jobs`, chọn một tin tuyển dụng và bấm **"Phân tích so với JD này"**.

