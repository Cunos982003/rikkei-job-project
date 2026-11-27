# [UT-07] Quản lý Offer tuyển dụng và Ghi nhận lịch sử nhận việc

## 1. Tóm tắt thay đổi
Triển khai trọn vẹn luồng **Offer & Employment Record** dựa trên nền tảng UT-02 (`recruitment-service`):

- **Database (`recruitment_db`)**:
  - Migration [V2__add_offers_and_employment_records.sql](file:///d:/Project/rikkei-job-project/backend/recruitment-service/src/main/resources/db/migration/V2__add_offers_and_employment_records.sql) tạo bảng `offers` và `employment_records`.
  - Seed mẫu kiểm thử đầy đủ trạng thái: [ut07_offer_seed.sql](file:///d:/Project/rikkei-job-project/infra/seed/ut07_offer_seed.sql).
- **Backend (`recruitment-service` + `api-gateway`)**:
  - `POST /api/v1/applications/{id}/offer` (HR): kiểm tra quyền sở hữu job, trạng thái `INTERVIEW`, vòng phỏng vấn cuối `PASS` (sai → `409`), chuyển đơn → `OFFER`, gửi email ứng viên qua `OfferSentEvent` + `RecruitmentEmailNotifier`.
  - `GET /api/v1/me/offers`, `GET /api/v1/me/offers/{id}` (Ứng viên; ứng viên khác mở → `404`).
  - `POST /api/v1/me/offers/{id}/accept` (Ứng viên): đơn → `HIRED`, tạo `employment_records`, giữ nguyên các đơn khác; quá hạn → `409`.
  - `POST /api/v1/me/offers/{id}/decline` (Ứng viên): bắt buộc `reason` (`400` nếu trống), offer → `DECLINED`, đơn → `REJECTED`.
  - `POST /api/v1/applications/{id}/offer/confirm-onboard` (HR): ghi `confirmed_at` khi đã `ACCEPTED`/`HIRED`.
  - `GET /api/v1/me/employment-records` (Ứng viên) & `GET /api/v1/hr/applications` (HR).
  - Job tự động chạy mỗi giờ (`@Scheduled(cron = "0 0 * * * *")`): offer `SENT` quá `response_deadline` → `EXPIRED`, đơn → `REJECTED`.
  - Cập nhật route Gateway (`backend/api-gateway/src/main/resources/application.yaml`).
- **Mock Server**:
  - Bổ sung dữ liệu mẫu `mock/data/offers.json`, `mock/data/employment_records.json` và đầy đủ endpoint UT-07 trong `mock/server.js` + 6 test case tự động trong `mock/test.js`.
- **Frontend (`frontend/`)**:
  - HR: [HrApplicationsPage.tsx](file:///d:/Project/rikkei-job-project/frontend/src/pages/hr/HrApplicationsPage.tsx) (`/hr/interviews`) và tích hợp Form gửi Offer, thẻ trạng thái Offer (hiển thị lý do từ chối), nút **Xác nhận đã đi làm** ngay trong [ApplicationDetailPage.tsx](file:///d:/Project/rikkei-job-project/frontend/src/pages/hr/ApplicationDetailPage.tsx) (`/hr/applications/:id`).
  - Ứng viên: [CandidateOffersPage.tsx](file:///d:/Project/rikkei-job-project/frontend/src/pages/candidate/CandidateOffersPage.tsx) (`/candidate/offers`) với bộ đếm ngược hạn trả lời theo giây, modal Từ chối bắt buộc lý do, tự khóa nút khi hết hạn; cập nhật [CandidateProfilePage.tsx](file:///d:/Project/rikkei-job-project/frontend/src/pages/candidate/CandidateProfilePage.tsx) hiển thị **`"Kinh nghiệm: <vị trí> tại <công ty>"`**.
- **OpenAPI**:
  - [recruitment-service-ut07.yaml](file:///d:/Project/rikkei-job-project/docs/api/openapi/recruitment-service-ut07.yaml).

---

## 2. Hướng dẫn chạy & kiểm thử

### 2.1. Dữ liệu mẫu (PostgreSQL)
```bash
psql -h localhost -U postgres -d recruitment_db -f infra/seed/ut07_offer_seed.sql
```
Tài khoản thử nghiệm (mật khẩu chung `Test@123`):
- **HR**: `hr1@rikkeijobs.test` (Company `11111111-1111-4111-8111-111111111111`)
- **Ứng viên 1**: `candidate1@rikkeijobs.test` (có Offer `SENT`, Offer `EXPIRED`, và đơn `INTERVIEW` đã `PASS`)
- **Ứng viên 2**: `candidate2@rikkeijobs.test` (có đơn `INTERVIEW` vòng cuối `PENDING` để test `409`, dùng để test mở Offer của ứng viên 1 trả `404`)
- **Ứng viên 3**: `candidate3@rikkeijobs.test` (có Offer `DECLINED` kèm lý do để HR kiểm tra)

### 2.2. Chạy Backend, Gateway, Mock & Frontend
```bash
# 1. Recruitment Service (:8086)
cd backend/recruitment-service && ./gradlew bootRun

# 2. API Gateway (:8080)
cd backend/api-gateway && ./gradlew bootRun

# 3. Mock Server (:9000 - nếu test FE độc lập)
node mock/server.js

# 4. Frontend (:5173)
npm run dev --prefix frontend
```

### 2.3. Chạy bộ kiểm thử tự động
```bash
cd backend/recruitment-service && ./gradlew test
cd backend/api-gateway && ./gradlew test
node mock/test.js
npm run build --prefix frontend
```

---

## 3. Tiêu chí nghiệm thu (Checklist)
- [x] Gửi offer khi chưa có vòng `PASS` → `409 Conflict` (`OfferServiceTest`, `mock/test.js` #22)
- [x] Ứng viên nhận → đơn `HIRED`, hồ sơ hiện mục `"Kinh nghiệm: <vị trí> tại <công ty>"` (`OfferServiceTest`, `OfferControllerTest`, `CandidateProfilePage`, `mock/test.js` #27)
- [x] Quá hạn trả lời → `EXPIRED`, ứng viên không bấm Nhận được nữa (`OfferServiceTest`, `OfferExpirationSchedulerTest`, `CandidateOffersPage`, `mock/test.js` #26)
- [x] Từ chối phải nhập lý do (`400` nếu trống); HR thấy lý do trên trang chi tiết đơn (`OfferControllerTest`, `ApplicationDetailPage`, `mock/test.js` #25)
- [x] Ứng viên khác mở offer này → `404 Not Found` (`OfferRepositoryTest`, `OfferServiceTest`, `OfferControllerTest`, `mock/test.js` #24)
- [x] OpenAPI cập nhật đầy đủ endpoint mới (`docs/api/openapi/recruitment-service-ut07.yaml`)

---

## 4. Ảnh màn hình (Screenshots)
> Đính kèm khi mở PR trên GitHub:
- [ ] Màn hình HR: Form gửi Offer & trạng thái Offer / lý do từ chối / nút Xác nhận đã đi làm (`/hr/applications/:id`)
- [ ] Màn hình Ứng viên: Trang Offer kèm bộ đếm ngược & modal Từ chối (`/candidate/offers`)
- [ ] Màn hình Hồ sơ Ứng viên: Hiển thị `"Kinh nghiệm: <vị trí> tại <công ty>"` sau khi nhận Offer (`/candidate/profile`)
