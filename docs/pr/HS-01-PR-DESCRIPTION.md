# [HS-01] Hồ sơ năng lực ứng viên: tổng hợp minh chứng, tính điểm và xếp bậc

## 1. Mô tả tổng quan
- **Mục tiêu**: Hệ thống gom minh chứng năng lực của ứng viên từ nhiều nguồn (LMS, Khảo thí, dự án IOC, tự khai), tính điểm đã xác thực và điểm tự đánh giá cho từng kỹ năng, xếp bậc theo thang chuẩn 4 mức, và hiển thị hồ sơ năng lực cho ứng viên.
- **Service Backend**: `backend/profile-service` (cổng 8082).
- **Frontend**: `frontend/src/features/profile/SkillProfilePage.tsx` (route `/profile/skills`).
- **Quan hệ với HS-02**: HS-02 ghi kỹ năng tự đánh giá vào cùng bảng `skill_evidences` với `source = 'SELF'`; HS-01 đọc lại và tách riêng thành `self_score`, không trộn vào điểm đã xác thực.

---

## 2. Thay đổi chi tiết

### 2.1. Cơ sở dữ liệu (`backend/profile-service/src/main/resources/schema.sql`)

Bốn bảng, tách rõ **minh chứng thô** khỏi **điểm tổng hợp**:

- **`skills`** — danh mục kỹ năng: `code` (UNIQUE), `name`, `group`.
- **`skill_mappings`** — ánh xạ từ mã môn / kỳ thi / dự án của hệ thống ngoài sang kỹ năng, kèm trọng số:
  - `source` ∈ `LMS` | `EXAM` | `PM`
  - `source_code` — mã phía hệ thống ngoài (ví dụ `JAVA01`, `JAVA_MID`, `IOC_JAVA`)
  - `weight` — `NUMERIC(3,2)`, ràng buộc `> 0 AND <= 1`
  - UNIQUE `(source, source_code, skill_id)` — cho phép **một mã nguồn ánh xạ tới nhiều kỹ năng** với trọng số khác nhau (ví dụ `JAVA01` → Java Core 0.70 + SQL 0.30)
- **`skill_evidences`** — từng minh chứng thô:
  - `source` ∈ `LMS` | `EXAM` | `PM` | `INTERNSHIP` | `MENTOR` | `SELF`
  - `source_ref` — mã tham chiếu phía nguồn
  - `score` — `SMALLINT`, ràng buộc `0–100`
  - `verified` — minh chứng do hệ thống đối chiếu hay do ứng viên tự khai
  - UNIQUE `(user_id, skill_id, source, source_ref)` — bảo đảm **tính lũy đẳng** khi đồng bộ lại
- **`skill_scores`** — điểm tổng hợp theo `(user_id, skill_id)`:
  - `verified_score`, `self_score` — hai điểm tách biệt
  - `level` ∈ `Cơ bản` | `Khá` | `Vững` | `Nâng cao`

Chỉ mục: `idx_skill_evidences_user_skill`, `idx_skill_mappings_source_code`.

### 2.2. Danh sách API Backend

Mọi endpoint xác định danh tính qua header `X-User-Id` (UUID do gateway truyền xuống). Thiếu header trả về **401**.

| STT | Phương thức | Đường dẫn | Mô tả |
|---|---|---|---|
| 1 | `GET` | `/api/v1/profiles/me/skills` | Hồ sơ năng lực của chính mình |
| 2 | `GET` | `/api/v1/profiles/{userId}/skills` | Hồ sơ năng lực của ứng viên khác; **404** nếu hồ sơ chưa công khai |

Mỗi phần tử trả về gồm `skillId`, `skillCode`, `skillName`, `group`, `verifiedScore`, `selfScore`, `level` và mảng `evidences` (`source`, `title`, `score`, `recordedAt`, `verified`).

**Quy tắc xem chéo người dùng**: `ProfileVisibilityService.canViewProfile` hiện chỉ cho phép xem hồ sơ của chính mình; hồ sơ người khác trả **404** thay vì 403 để tránh dò tìm sự tồn tại của tài khoản. Đây là điểm móc nối để HS-05 thay bằng điều kiện "hồ sơ đã công khai".

### 2.3. Thuật toán tính điểm (`skill/application/SkillRecomputeService.java`)

`recomputeSkills(userId, externalRecords)` chạy trong một transaction:

1. Với mỗi bản ghi ngoài, tra `skill_mappings` theo `(source, sourceCode)`; mỗi ánh xạ sinh một minh chứng `verified = true` với điểm **giữ nguyên điểm gốc** của hệ thống ngoài. Trọng số ánh xạ chưa nhân vào — để dành cho DG-03.
2. Ghi minh chứng theo kiểu upsert: xóa bản ghi cũ theo `(user_id, skill_id, source, source_ref)` rồi lưu bản mới, nên chạy lại nhiều lần không sinh bản trùng.
3. Tính `verified_score` = trung bình có **suy giảm theo thời gian** của các minh chứng đã xác thực: minh chứng trong vòng **365 ngày** giữ nguyên, cũ hơn nhân hệ số **0.5**, làm tròn `HALF_UP`.
4. Tính `self_score` = trung bình cộng đơn giản của các minh chứng **chưa** xác thực.
5. Xếp `level` theo `effectiveScore = verifiedScore ?? selfScore`, ngưỡng **≥85 Nâng cao, ≥70 Vững, ≥50 Khá, còn lại Cơ bản**.

`verified_score` và `self_score` **không bao giờ trộn vào nhau**: kỹ năng chỉ có tự khai sẽ có `verifiedScore = null` và giao diện gắn nhãn "Tự khai".

### 2.4. Thang bậc dùng chung (`common/LevelConstants.java`)

Bốn hằng số bậc (`Cơ bản`, `Khá`, `Vững`, `Nâng cao`) được tách thành một lớp dùng chung cho cả `SkillRecomputeService` (HS-01) và `ProfileService` (HS-02), để hai luồng tính bậc không lệch nhau.

### 2.5. Giao diện người dùng (Frontend)

Route `/profile/skills`, trang `SkillProfilePage`:

- **Nhóm theo `group`** của kỹ năng (Programming, Framework, Database, Frontend, DevOps, Soft Skill…), mỗi nhóm là một lưới thẻ.
- **Dòng tóm tắt** đầu trang: tổng số kỹ năng • số kỹ năng đã xác thực • số kỹ năng tự khai.
- **Thẻ kỹ năng**: thanh tiến độ theo điểm hiển thị (`verifiedScore`, nếu không có thì `selfScore`), nhãn **Xác thực** / **Tự khai**, nhãn bậc, nút mở/đóng danh sách minh chứng.
- **Danh sách minh chứng**: mỗi minh chứng hiển thị nguồn (`LMS`/`EXAM`/`PM`/`SELF`…), tiêu đề, điểm tô màu theo mức (≥80 xanh, 60–79 vàng, 40–59 cam, <40 đỏ), trạng thái đã/chưa xác thực và ngày ghi nhận.
- **Trạng thái tải và lỗi**: khung xương khi đang tải, thông báo lỗi kèm nút "Thử lại".

---

## 3. Dữ liệu mẫu

### 3.1. Backend (`backend/profile-service/src/main/resources/data.sql`)
- **20 kỹ năng** thuộc 9 nhóm: Programming, Framework, Database, Frontend, Tool, DevOps, Quality, Process, Soft Skill, Language, CS Fundamental, Architecture.
- **14 ánh xạ** phủ 3 nguồn `LMS`, `EXAM`, `PM`, ví dụ:
  - `LMS/JAVA01` → Java Core 0.70 + SQL 0.30
  - `LMS/SPRING01` → Spring Boot 0.80 + Java Core 0.20
  - `EXAM/JAVA_MID` → Java Core 1.00
  - `PM/IOC_JAVA` → Java Core 0.60 + Teamwork 0.40
- **Minh chứng và điểm mẫu** cho người dùng demo `550e8400-e29b-41d4-a716-446655440000`: 7 minh chứng (5 đã xác thực, 2 tự khai) và 5 dòng `skill_scores`.

### 3.2. Mock server hệ thống ngoài (`mock/data/`)
- `lms.json`, `exam.json`, `pm.json` — kết quả học tập, kết quả thi và đánh giá dự án IOC cho 3 ứng viên.
- `skills.json` — danh mục kỹ năng dùng cho giao diện.
- Mock server chạy cổng **9001** (`mock/server.js`), phản chiếu hợp đồng API của backend để phát triển frontend không cần cơ sở dữ liệu.

| Tài khoản | Email | Mật khẩu | `X-User-Id` (mock :9001) |
|---|---|---|---|
| Ứng viên 1 | `candidate1@rikkeijobs.test` | `Test@123` | `user-001` |
| Ứng viên 2 | `candidate2@rikkeijobs.test` | `Test@123` | `user-002` |
| Ứng viên 3 | `candidate3@rikkeijobs.test` | `Test@123` | `user-003` |

> **Phân biệt hai môi trường.** Mock server (:9001) khớp `X-User-Id` trực tiếp với `users[].id` trong `mock/data/users.json`, tức **chỉ nhận `user-00N`**. Backend thật (:8082) khai báo `X-User-Id` kiểu `UUID`, nên cần một UUID hợp lệ bất kỳ — ví dụ `550e8400-e29b-41d4-a716-446655440000`.

---

## 4. Hướng dẫn chạy & Kiểm thử

### 4.1. Chạy Mock server + Frontend (không cần cơ sở dữ liệu)
```bash
# Terminal 1 — mock server (cổng 9001)
cd mock
node server.js

# Terminal 2 — frontend (cổng 5173, proxy /api -> 9001)
cd frontend
npm install
npm run dev
```
Đăng nhập `candidate1@rikkeijobs.test` / `Test@123` → mở `/profile/skills`.

### 4.2. Chạy Backend thật
```bash
# Cần PostgreSQL tại localhost:5432 (database profile_db)
cd backend/profile-service
./gradlew bootRun
```
Frontend trỏ vào backend thật:
```bash
VITE_BACKEND_URL=http://localhost:8082 npm run dev
```

### 4.3. Chạy kiểm thử
```bash
cd backend/profile-service
./gradlew test          # 71 test, cần Docker cho Testcontainers
```
```bash
cd frontend
npm run build           # tsc -b && vite build
```

---

## 5. Kết quả kiểm tra nghiệm thu

| # | Tiêu chí nghiệm thu | Kết quả | Chi tiết kiểm tra |
|---|---|:---:|---|
| 1 | Ứng viên xem được hồ sơ năng lực gồm nhiều kỹ năng, mỗi kỹ năng kèm minh chứng đầy đủ nguồn/tiêu đề/ngày | **ĐẠT** | `SkillProfileIntegrationTest.fullFlow_candidate1Sees5SkillsWithEvidences` — mọi minh chứng có `source`, `title`, `recordedAt` khác rỗng |
| 2 | Minh chứng sinh từ ánh xạ trong CSDL, không hard-code trong mã | **ĐẠT** | `SkillProfileIntegrationTest.skillMappingInDatabase_notHardcoded` — dùng ánh xạ `EXAM:JAVA_MID` từ CSDL và ra đúng `verifiedScore = 80` |
| 3 | Đồng bộ lại cùng một nguồn không sinh minh chứng trùng | **ĐẠT** | `SkillRecomputeServiceTest.recomputeSkills_idempotent_noDuplicatesOnRerun` và `SkillEvidenceRepositoryTest.uniqueConstraint_preventsDuplicateEvidences` |
| 4 | Điểm xác thực cập nhật khi có kết quả mới | **ĐẠT** | `SkillProfileIntegrationTest.recompute_afterAddingCourse_updatesScoreCorrectly` — nộp lại `JAVA01` với điểm 90, `verifiedScore` đổi từ 80 thành 90 |
| 5 | Suy giảm theo thời gian đúng ranh giới 365/366 ngày | **ĐẠT** | `SkillRecomputeServiceTest.recomputeSkills_timeDecay_boundary365vs366` |
| 6 | Kỹ năng chỉ có tự khai: `verifiedScore = null`, bậc tính theo `selfScore` | **ĐẠT** | `SkillProfileIntegrationTest.selfOnlySkill_showsSelfLabelNoVerifiedScore` — `selfScore = 90`, `verifiedScore = null`, `level = "Nâng cao"` |
| 7 | Điểm tự khai và điểm xác thực không trộn vào nhau | **ĐẠT** | `SkillRecomputeServiceTest.recomputeSkills_selfOnlySkill_hasNullVerifiedScore` |
| 8 | Xếp bậc đúng thang 4 mức | **ĐẠT** | `SkillRecomputeServiceTest.recomputeSkills_createsVerifiedEvidencesAndCalculatesScore` |
| 9 | Xem hồ sơ người khác chưa công khai → 404 (không lộ sự tồn tại tài khoản) | **ĐẠT** | `SkillProfileIntegrationTest.otherUserPrivateProfile_returns404` |
| 10 | Thiếu `X-User-Id` → 401 | **ĐẠT** | `SkillProfileControllerTest.getUserSkills_unauthorized_returns401` |
| 11 | Lược đồ CSDL có đủ bảng và cột bắt buộc | **ĐẠT** | `SchemaMigrationTest.schemaHasRequiredTablesAndColumns` |
| 12 | Toàn bộ test backend xanh | **ĐẠT** | `./gradlew test` → **71/71 pass, 0 failure, 0 error** (bao gồm cả 16 test của HS-01 và HS-02 dùng chung) |
| 13 | Frontend build sạch | **ĐẠT** | `npm run build` → `tsc -b` không lỗi, `vite build` thành công (2354 module, exit code 0) |

---

## 6. Minh họa cURL kiểm thử nhanh

Biến dùng chung (chạy với **backend thật** cổng 8082):
```bash
UID1="550e8400-e29b-41d4-a716-446655440000"
BASE="http://localhost:8082/api/v1"
```

> Khi thử với **mock server** cổng 9001, đổi `BASE="http://localhost:9001/api/v1"` và `UID1="user-001"`.

### 1) Xem hồ sơ năng lực của chính mình
```bash
curl -X GET "$BASE/profiles/me/skills" -H "X-User-Id: $UID1"
```
Kết quả: mảng kỹ năng, mỗi phần tử có `verifiedScore` hoặc `selfScore`, `level`, và `evidences`.

### 2) Thiếu header danh tính → 401
```bash
curl -i -X GET "$BASE/profiles/me/skills"
```

### 3) Xem hồ sơ người khác khi chưa công khai → 404
```bash
UID2="6ba7b810-9dad-11d1-80b4-00c04fd430c8"
curl -i -X GET "$BASE/profiles/$UID2/skills" -H "X-User-Id: $UID1"
```

### 4) Kiểm tra hồ sơ tự khai sau khi HS-02 ghi điểm
```bash
# Ghi điểm tự đánh giá (endpoint của HS-02)
curl -X PUT "$BASE/profiles/me/self-skills" \
  -H "X-User-Id: $UID1" \
  -H "Content-Type: application/json" \
  -d '[{"skillId": "<uuid-kỹ-năng>", "selfScore": 88}]'

# Đọc lại: kỹ năng đó phải có selfScore = 88 và verifiedScore giữ nguyên (không bị ghi đè)
curl -X GET "$BASE/profiles/me/skills" -H "X-User-Id: $UID1"
```
