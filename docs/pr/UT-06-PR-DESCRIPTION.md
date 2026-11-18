# [UT-06] Đặt lịch phỏng vấn, Tạo phòng họp Rikkei Meet & Nhắc lịch tự động

## 1. Tóm tắt thay đổi
- **Database (`recruitment_db`):**
  - Thêm migration `backend/recruitment-service/src/main/resources/db/migration/V2__add_interview_slots_and_schedules.sql` tạo 2 bảng `interview_slots` (`OPEN|PICKED|CANCELLED`) và `interview_schedules` (`SCHEDULED|DONE|CANCELLED`, `meet_url`, `reminded_24h_at`, `reminded_1h_at`).
  - Thêm dữ liệu mẫu idempotent tại `infra/seed/ut06_interview_scheduling_seed.sql`.
- **Backend (`recruitment-service` & `api-gateway`):**
  - `POST /api/v1/rounds/{roundId}/slots` (HR): đề xuất 1–5 khung giờ trong tương lai, mỗi khung ≥ 30 phút, không chồng lấn (vi phạm → `400`).
  - `GET /api/v1/rounds/{roundId}/slots` (HR/STAFF): lấy khung giờ theo vòng.
  - `POST /api/v1/rounds/{roundId}/reschedule` (HR): hủy lịch cũ + khung cũ, đề xuất 1–5 khung giờ mới.
  - `GET /api/v1/me/interviews` (CANDIDATE): danh sách lời mời kèm khung giờ và lịch đã chốt.
  - `POST /api/v1/me/interviews/{roundId}/pick` (CANDIDATE): chọn khung `OPEN` → gọi Mock Rikkei Meet `POST /meet/rooms` lấy `meet_url`, chuyển các khung còn lại sang `CANCELLED`, lưu `interview_schedules`, gửi email kèm file `.ics` cho cả ứng viên và HR/người phỏng vấn. Chọn khung đã hủy → `409`.
  - `GET /api/v1/companies/me/interviews?from=&to=` (HR): trả danh sách buổi phỏng vấn của doanh nghiệp trong khoảng thời gian.
  - Scheduler `InterviewReminderScheduler` (`0 */15 * * * *`): nhắc lịch 24h và 1h trước giờ phỏng vấn, ghi `reminded_24h_at` / `reminded_1h_at` để không bao giờ gửi trùng.
  - Cập nhật tuyến `api-gateway` và `frontend/vite.config.ts` để `/api/v1/companies/me/interviews`, `/api/v1/rounds/**`, `/api/v1/me/interviews/**` định tuyến về `recruitment-service (:8086)`.
- **Frontend:**
  - HR: Hộp đề xuất/đổi khung giờ & link phòng họp ngay trong khối vòng phỏng vấn tại `ApplicationDetailPage.tsx`.
  - HR: Màn **Lịch phỏng vấn doanh nghiệp dạng tuần** tại `/hr/interviews` (`HrInterviewSchedulePage.tsx`).
  - Ứng viên: Màn **Lịch phỏng vấn & Lời mời của tôi** tại `/candidate/interviews` (`CandidateInterviewsPage.tsx`) với chức năng chọn khung giờ & nút **Vào phòng họp**.
- **OpenAPI:** Cập nhật tại `docs/api/openapi/recruitment-service-ut06.yaml`.

## 2. Cách chạy & Kiểm thử nhanh
1. **Nạp dữ liệu mẫu vào `recruitment_db`:**
   ```bash
   psql -U postgres -h localhost -d recruitment_db -f infra/seed/ut06_interview_scheduling_seed.sql
   ```
2. **Khởi động các service cần thiết:**
   - Mock Rikkei Meet (`:9000`): `node mock/server.js`
   - `auth-service (:8081)`, `job-service (:8085)`, `recruitment-service (:8086)`, `api-gateway (:8080)`
   - Frontend (`:5173`): `npm run dev --prefix frontend`
3. **Chạy bộ test tự động:**
   ```bash
   cd backend/recruitment-service && ./gradlew test
   cd backend/api-gateway && ./gradlew test
   npm run build --prefix frontend
   ```

## 3. Dữ liệu mẫu (`infra/seed/ut06_interview_scheduling_seed.sql`)
- **Đơn 1 (`77060000-0000-4000-8000-000000000001`):** Ứng viên `candidate1@rikkeijobs.test` — Vòng `77062000-0000-4000-8000-000000000001` có sẵn **3 khung giờ `OPEN`** để ứng viên bấm chọn tại `/candidate/interviews`.
- **Đơn 2 (`77060000-0000-4000-8000-000000000002`):** Ứng viên `candidate2@rikkeijobs.test` — Vòng `77062000-0000-4000-8000-000000000002` đã chốt lịch (`https://meet.rikkei.edu.vn/room-ut06-demo01`, hiển thị ngay trên Lịch tuần HR `/hr/interviews`) + có 1 khung `CANCELLED` (`77063000-0000-4000-8000-000000000005`) để kiểm thử chọn khung đã hủy → `409`.

## 4. Kiểm tra tiêu chí nghiệm thu
- [x] Đề xuất khung giờ trong quá khứ hoặc ngắn hơn 30 phút → `400` (`InterviewScheduleServiceTest`)
- [x] Ứng viên chọn khung → tạo `meet_url` từ Mock `/meet/rooms`, các khung khác chuyển `CANCELLED`, gửi email kèm `.ics` cho cả 2 bên (`InterviewScheduleServiceTest`)
- [x] Chọn khung đã hủy → `409` (`InterviewScheduleServiceTest`)
- [x] Lịch còn 1 giờ → nhận đúng 1 email nhắc, chạy job lần nữa không gửi trùng (`InterviewScheduleServiceTest` + `InterviewReminderIntegrationTest`)
- [x] Lịch tuần của HR hiện đúng các buổi (`HrInterviewSchedulePage.tsx` + `GET /api/v1/companies/me/interviews`)
- [x] OpenAPI cập nhật đầy đủ tại `docs/api/openapi/recruitment-service-ut06.yaml`

## 5. Ảnh màn hình (đính kèm khi mở PR trên GitHub)
- [ ] Màn chi tiết đơn HR (`/hr/applications/:id`) — Hộp đề xuất khung giờ & Đổi lịch trong khối vòng phỏng vấn
- [ ] Màn Lịch phỏng vấn tuần của HR (`/hr/interviews`)
- [ ] Màn Lịch phỏng vấn của Ứng viên (`/candidate/interviews`) — Chọn khung giờ & Nút "Vào phòng họp"
