# Báo cáo Kiểm tra & Sửa lỗi AI-01 (Gợi ý việc làm phù hợp)

## 1. Kết quả kiểm tra đối chiếu yêu cầu

| Hạng mục | Trạng thái trước khi sửa | Quyết định xử lý |
|---|---|---|
| **Quy ước AI chung trong `README.md`** | Chưa có mục quy ước riêng về `AI_PROVIDER = openai \| gemini \| claude \| mock`, chế độ `mock` cho mentor không cần key, và quy ước mọi lời gọi AI qua 1 service chung | Bổ sung mục **Quy ước AI chung (`ai_career_service`)** vào `README.md` |
| **DB `job_recommendations`** | Đã có đủ bảng `(id, user_id, job_id, score, explanation, created_at, input_hash)` | Giữ nguyên |
| **API `GET /me/job-recommendations?limit=10`** | Đã có top 5 gọi AI `<= 60` từ, prompt chỉ gồm kỹ năng + chỉ số xác thực (không PII), cache 24h theo `input_hash`, timeout 10s fallback từ dữ liệu khớp. **Tuy nhiên** `HttpRecommendationDataSource` chưa lọc JD hết hạn (`deadline`) | Bổ sung lọc `isExpired(deadline)` trong `HttpRecommendationDataSource` để chỉ giữ JD `APPROVED` còn hạn |
| **FE — Nhãn `"Do AI tạo"` & nút `"Làm mới"`** | `RecommendationCard` đã có `% khớp`, nhưng thiếu nhãn `"Do AI tạo"`; `RecommendationsPage` thiếu nút `"Làm mới"` | Thêm nhãn `"Do AI tạo"` vào `RecommendationCard` và nút `"Làm mới"` vào `RecommendationsPage` |
| **FE — Khối `"Việc làm phù hợp với bạn"` ở trang chủ ứng viên** | Trang chủ ứng viên (`/candidate/jobs` — `CandidateJobsPage.tsx`) chưa hiển thị khối gợi ý AI | Bổ sung khối **"Việc làm phù hợp với bạn"** lên đầu `CandidateJobsPage.tsx` gồm thẻ JD, `% khớp`, giải thích có nhãn `"Do AI tạo"` và nút `"Làm mới"` |

## 2. Chi tiết các file đã chỉnh sửa

1. `README.md`
   - Bổ sung mục **Quy ước AI chung (`ai_career_service`)** mô tả rõ `AI_PROVIDER = openai | gemini | claude | mock`, vai trò chế độ `mock` và luồng gọi AI tập trung qua `ai_career_service` (`AIProvider`).
2. `ai_career_service/src/main/java/com/example/ai_career_service/service/HttpRecommendationDataSource.java`
   - Thêm hàm `isExpired(Object deadlineObj)` (tính theo múi giờ `Asia/Ho_Chi_Minh`) để loại bỏ các JD đã quá hạn `deadline` khi lấy danh sách `APPROVED`.
3. `frontend/src/features/career/components/CareerShared.tsx`
   - Bổ sung badge **`Do AI tạo`** ngay phía trên đoạn giải thích trong `RecommendationCard`.
4. `frontend/src/pages/career/RecommendationsPage.tsx`
   - Bổ sung nút **`Làm mới`** gọi `refetch()` trong `PageIntro`.
5. `frontend/src/pages/candidate/CandidateJobsPage.tsx`
   - Bổ sung section **`Việc làm phù hợp với bạn`** (dùng `useRecommendations(5)`) hiển thị tối đa 3 thẻ JD nổi bật kèm `% khớp`, giải thích có nhãn **`Do AI tạo`**, nút **`Làm mới`** và nút **`Xem tất cả`** dẫn sang `/candidate/career/recommendations`.
6. `backend/api-gateway/src/main/resources/application.yaml`
   - Đưa route `ai-career-direct` (`/me/job-recommendations/**`) và `ai-interview-practice` (`/me/interview-practices/**`) lên **trước** route `profile` (`/me/**`) để request trực tiếp tới `/me/job-recommendations` qua Gateway không bị `profile` nuốt mất.

## 3. Kiểm thử sau chỉnh sửa

- Backend (`ai_career_service`): `.\gradlew.bat test`
- Frontend (`frontend`): `npm.cmd run build` và `npm.cmd run lint`
