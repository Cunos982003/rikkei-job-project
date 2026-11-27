# [NT-05] Kết nối dữ liệu: LMS, khảo thí, lms-pm (IOC)

## 1. Thông tin Pull Request

* **Tiêu đề PR:** `[NT-05] Kết nối dữ liệu: LMS, khảo thí, lms-pm (IOC)`
* **Target Branch:** `develop`
* **Source Branch:** `feature/TA65388`
* **Module:** `profile-service` (BE), `frontend` (FE), `api-gateway`, `docs`
* **Techstack:** Spring Boot 3 / Java 17, PostgreSQL / Flyway, React / TypeScript / Vite, TanStack Query, TailwindCSS, Lucide Icons

---

## 2. Mô tả tính năng & Các thành phần thực hiện

Mục tiêu của task là kết nối và kéo dữ liệu học tập (LMS), kết quả thi cử (EXAM), và quá trình thực tập dự án IOC (LMS-PM) từ mock server về cơ sở dữ liệu của `profile-service`, làm nguồn bằng chứng cho hồ sơ kỹ năng (HS-01).

### 2.1. Cơ sở dữ liệu (Database Migration - `V5__nt05_external_sync.sql`)
1. **`external_records`**:
   - `(id, user_id, source LMS|EXAM|PM, source_key, payload JSONB, synced_at)`
   - Ràng buộc: `UNIQUE (source, source_key)` đảm bảo đồng bộ nhiều lần không sinh bản ghi trùng lặp.
2. **`manual_overrides`**:
   - `(id, record_id, field, value, reason, staff_id, created_at)`
   - Ràng buộc: `UNIQUE (record_id, field)` lưu lại lịch sử cán bộ sửa tay và lý do phúc khảo/điều chỉnh.
3. **`sync_runs`**:
   - `(id, trigger NIGHTLY|ON_OPEN|MANUAL, user_id, status SUCCESS|FAILED, started_at, finished_at, error, counts JSONB)`
   - Theo dõi chi tiết lịch sử, trạng thái, thời gian chạy và số lượng bản ghi của từng đợt đồng bộ.
4. **`skill_mappings`**:
   - Bổ sung ánh xạ mã môn từ LMS/EXAM/PM sang kỹ năng tương ứng trong hệ thống (Java Core, Spring Boot, SQL, Git, React, Teamwork...).

### 2.2. Backend (`profile-service` & `api-gateway`)
1. **`ExternalMockClient.java`**:
   - Gọi mock endpoints qua WebClient: `/lms/students/{id}/results`, `/exam/students/{id}/results`, `/pm/students/{id}/internships`.
   - Cấu hình header `X-Service-Key`, timeout 5 giây, cơ chế thử lại (retry) 2 lần khi gặp sự cố mạng.
2. **`DataSyncService.java`**:
   - `syncUser(userId, trigger)`: Kéo dữ liệu từ mock, upsert `external_records` theo `source_key`, merge và bảo toàn các trường đã có trong `manual_overrides`.
   - Tự động gọi `skillRecomputeService.recomputeSkills(userId, dtos)` để cập nhật điểm kỹ năng cho HS-01 ngay sau khi đồng bộ.
   - Ghi nhận `sync_runs` chi tiết (`SUCCESS` hoặc `FAILED` kèm message lỗi).
   - `syncAllCandidates(trigger)`: Quét và đồng bộ cho toàn bộ ứng viên trong hệ thống.
   - `triggerBackgroundSyncIfStale(userId)`: Khi ứng viên mở hồ sơ (`GET /api/v1/profiles/me`), nếu lần đồng bộ cuối đã quá 1 giờ hoặc chưa từng đồng bộ thì tự động chạy nền không chặn tải trang.
3. **`SyncScheduler.java`**:
   - Lập lịch tự động chạy vào **01:00 AM mỗi đêm** cho toàn bộ ứng viên qua cron expression `${app.sync.nightly-cron:0 0 1 * * ?}`.
4. **`StaffSyncController.java`**:
   - `POST /staff/sync/{userId}`: Cán bộ yêu cầu đồng bộ lại cho một ứng viên.
   - `POST /staff/sync/trigger-nightly`: Endpoint hỗ trợ mentor/cán bộ kích hoạt chạy ngay Job đêm bằng tay.
   - `GET /staff/sync-runs?status=&userId=`: Lấy lịch sử lần chạy đồng bộ có phân trang và bộ lọc.
   - `GET /staff/external-records?userId=&source=`: Lấy danh sách bản ghi ngoài kèm trạng thái sửa tay.
   - `PUT /staff/external-records/{id}/override`: Cán bộ sửa tay một trường bản ghi ngoài kèm lý do.
5. **Định tuyến qua `api-gateway`**:
   - Bổ sung định tuyến cho các prefix `/api/v1/staff/sync/**`, `/api/v1/staff/sync-runs`, `/api/v1/staff/external-records/**` và alias `/staff/**` về `profile-service`.

### 2.3. Frontend (`frontend` - Giao diện Cán bộ Vận hành)
1. **Màn hình "Đồng bộ dữ liệu" (`StaffSyncPage.tsx`)**:
   - Đã liên kết đường dẫn `/staff/sync` trong router chính [AppRoutes.tsx](file:///d:/Project/rikkei-job-project/frontend/src/routes/AppRoutes.tsx), tích hợp trực tiếp trên Sidebar và Menu của Header.
   - **Bảng lịch sử lần chạy (Sync Runs Table):** Hiển thị thời điểm, kiểu trigger (`NIGHTLY`, `ON_OPEN`, `MANUAL`), trạng thái (`SUCCESS`, `FAILED`, `RUNNING`), số lượng bản ghi (`counts`), thông tin lỗi chi tiết nếu thất bại; hỗ trợ bộ lọc trạng thái.
   - **Khối tìm kiếm & Đồng bộ ứng viên:** Chọn nhanh ứng viên mẫu (`candidate1`, `candidate2`, `candidate3`) hoặc nhập UUID ứng viên, bấm nút **"Đồng bộ lại"** hiển thị toast thông báo kết quả.
   - **Nút "Chạy thử nghiệm Job đêm":** Cho phép mentor/cán bộ kích hoạt Job đêm tức thì từ giao diện để kiểm tra.
   - **Bảng danh sách bản ghi ngoài (`external_records`):** Hiển thị dữ liệu kéo về từ LMS, EXAM, PM; đánh dấu badge các bản ghi đã can thiệp sửa tay (`manual_overrides`).
   - **Modal sửa tay (`ManualOverrideModal.tsx`):** Cho phép cán bộ chọn trường cần sửa, nhập giá trị mới và lý do sửa tay.

---

## 3. Hướng dẫn kiểm thử cho Mentor & Reviewer

### 3.1. Khởi động môi trường kiểm thử
1. Khởi động Mock Server:
   ```bash
   cd mock
   node server.js
   # Mock server lắng nghe tại port 9000
   ```
2. Khởi động Profile Service & API Gateway:
   ```bash
   cd backend/profile-service && ./gradlew bootRun
   cd backend/api-gateway && ./gradlew bootRun
   ```
3. Khởi động Frontend:
   ```bash
   cd frontend && npm run dev
   # Truy cập: http://localhost:5173
   ```

### 3.2. Kiểm chứng các tiêu chí nghiệm thu (Acceptance Criteria)

#### AC1: Đổi điểm của candidate1 trong mock, bấm Đồng bộ lại → external_records có điểm mới
1. Đăng nhập tài khoản Staff: `staff.admin@rikkei.edu.vn` / Pass: `123456`.
2. Vào menu **Đồng bộ dữ liệu** (`/staff/sync`).
3. Mở file [mock/data/lms.json](file:///d:/Project/rikkei-job-project/mock/data/lms.json), tìm học viên `std-001` (khóa `candidate1`), sửa điểm môn `JV-01` từ `8.8` thành `9.8`.
4. Trên giao diện `/staff/sync`, chọn ứng viên **Nguyễn Văn An** và bấm nút **"Đồng bộ lại"**.
5. Bảng "Dữ liệu nguồn ngoài" lập tức cập nhật điểm số mới là `9.8`, điểm kỹ năng được tính lại tương ứng.

#### AC2: Tắt mock rồi đồng bộ → sync_runs FAILED kèm lỗi, màn hình hiện lỗi, dữ liệu cũ còn nguyên
1. Dừng tiến trình mock server (`Ctrl + C` ở terminal mock).
2. Trên giao diện `/staff/sync`, bấm nút **"Đồng bộ lại"**.
3. Hệ thống sau khi retry 2 lần sẽ hiển thị toast cảnh báo lỗi.
4. Bảng lịch sử ghi nhận lần chạy mới với trạng thái `FAILED`, cột lỗi hiển thị rõ: `Failed to call external system ... Connection refused`.
5. Bảng dữ liệu nguồn ngoài vẫn hiển thị nguyên vẹn các bản ghi cũ, không bị xóa hay corrupt.

#### AC3: Cán bộ sửa tay 1 trường → Đồng bộ lại không ghi đè trường đó
1. Tại bảng "Dữ liệu nguồn ngoài", chọn một bản ghi (ví dụ: môn `JV-01`), bấm nút icon cây bút **"Sửa tay"**.
2. Nhập trường: `score`, giá trị mới: `9.5`, lý do: `Phúc khảo theo đơn số 123` $\rightarrow$ Bấm **"Lưu can thiệp"**.
3. Bản ghi hiển thị badge cam `Đã can thiệp sửa tay`.
4. Khởi động lại mock server và bấm nút **"Đồng bộ lại"**.
5. Dữ liệu trường `score` vẫn giữ nguyên là `9.5`, trong khi các trường khác (như tỷ lệ chuyên cần, ngày đồng bộ) vẫn được cập nhật bình thường từ mock.

#### AC4: Đồng bộ 2 lần liên tiếp không sinh bản ghi trùng
1. Bấm nút **"Đồng bộ lại"** liên tiếp 2 lần cho cùng một ứng viên.
2. Kiểm tra tổng số bản ghi trong bảng `external_records` không hề tăng gấp đôi nhờ ràng buộc `UNIQUE (source, source_key)`.

#### AC5: Cấu hình Job đêm & Cách chạy tay Job đêm để Mentor thử nghiệm
* **Cấu hình giờ chạy:** File `application.yaml` cấu hình:
  ```yaml
  app:
    sync:
      nightly-cron: "0 0 1 * * ?" # 01:00 AM hàng ngày
      nightly-enabled: true
  ```
* **Cách chạy tay Job đêm bằng lệnh cURL:**
  ```bash
  curl -X POST http://localhost:8080/api/v1/staff/sync/trigger-nightly \
    -H "Authorization: Bearer <STAFF_JWT_TOKEN>" \
    -H "X-User-Role: STAFF"
  ```
* **Cách chạy tay từ giao diện Web:**
  Tại trang `/staff/sync`, bấm vào nút **"Chạy thử nghiệm Job đêm"** (icon mặt trăng). Hệ thống sẽ quét toàn bộ ứng viên và tạo ra một bản ghi `sync_runs` với trigger `NIGHTLY`.

---

## 4. Dữ liệu mẫu kiểm thử (Sample Test Data)

| Mã ứng viên | Học viên LMS | Email | Vai trò kiểm thử |
| :--- | :--- | :--- | :--- |
| `ed854c07-f3eb-48f1-a186-4e7135f54645` | `std-001` | `candidate1@rikkeijobs.test` | Ứng viên Java Fullstack |
| `797c5663-386b-4e96-9112-6d3e414b4449` | `std-002` | `candidate2@rikkeijobs.test` | Ứng viên Frontend React |
| `4a09b051-d7ab-40a5-8322-963ec9ebdbd7` | `std-003` | `candidate3@rikkeijobs.test` | Ứng viên Java Backend |

---

## 5. Đặc tả API OpenAPI 3.0.3 cập nhật

Chi tiết đặc tả các endpoint mới được lưu tại: [docs/api/openapi/profile-service-nt05.yaml](file:///d:/Project/rikkei-job-project/docs/api/openapi/profile-service-nt05.yaml).
