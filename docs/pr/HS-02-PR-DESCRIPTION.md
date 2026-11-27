# [HS-02] Hồ sơ ứng viên: đồng bộ LMS, học vấn, chứng chỉ, kỹ năng tự đánh giá và tải tệp an toàn

## 1. Mô tả tổng quan
- **Mục tiêu**: Ứng viên có trang hồ sơ đầy đủ. Hồ sơ được tự động tạo và điền từ Rikkei LMS ở lần đăng nhập đầu tiên; ứng viên tự quản lý học vấn, chứng chỉ, thông tin cá nhân và kỹ năng tự đánh giá; tải lên tệp minh chứng an toàn.
- **Service Backend**: `backend/profile-service` (cổng 8082).
- **Frontend**: `frontend/src/pages/candidate/CandidateProfilePage.tsx` (route `/candidate/profile`).
- **Mock server**: `mock/server.js` (cổng 9001) — phản chiếu đúng hợp đồng API của backend để phát triển frontend.
- **Tài liệu liên quan**: [`HS-01-PR-DESCRIPTION.md`](./HS-01-PR-DESCRIPTION.md) — hai task dùng chung bảng `skill_evidences`, `skill_scores` và `LevelConstants`; bộ test của hai task nằm cùng module `profile-service`.

---

## 2. Thay đổi chi tiết

### 2.1. Cơ sở dữ liệu (`backend/profile-service/src/main/resources/schema.sql`)

- **Bảng `candidate_profiles`** — hồ sơ ứng viên, khóa chính là `user_id` (UUID):
  - `full_name`, `email`, `program`, `class_code`, `course_name`: **đồng bộ từ LMS, bất biến**
  - `dob`, `gender`, `phone`, `address`, `avatar_url`, `headline`, `summary`: ứng viên tự sửa
  - `visibility`: `PRIVATE` | `PUBLIC` | `RECRUITER`
- **Bảng `educations`** — `school`, `major`, `from_date`, `to_date`, `gpa`, `note`
- **Bảng `certificates`** — `name`, `issuer`, `issued_at`, `expires_at`, `file_url`, `file_type`

Kỹ năng tự đánh giá **không tạo bảng mới**: ghi vào `skill_evidences` (HS-01) với `source = 'SELF'` và cập nhật `self_score` trong `skill_scores`, giữ nguyên `verified_score` do HS-01 tính.

### 2.2. Danh sách API Backend

Mọi endpoint đều xác định danh tính qua header `X-User-Id` (UUID do gateway truyền xuống). Thiếu header trả về **401**.

| STT | Phương thức | Đường dẫn | Mô tả |
|---|---|---|---|
| 1 | `GET` | `/api/v1/profiles/me` | Lấy hồ sơ; nếu chưa có thì **tự tạo từ LMS** |
| 2 | `PUT` | `/api/v1/profiles/me` | Cập nhật hồ sơ. Trường LMS bị bỏ qua; trường không gửi giữ nguyên giá trị cũ |
| 3 | `PUT` | `/api/v1/profiles/me/self-skills` | Ghi đè danh sách kỹ năng tự đánh giá `[{skillId, selfScore}]`, điểm 0–100 |
| 4 | `GET` | `/api/v1/profiles/me/educations` | Danh sách học vấn |
| 5 | `GET` | `/api/v1/profiles/me/educations/{id}` | Chi tiết một học vấn |
| 6 | `POST` | `/api/v1/profiles/me/educations` | Thêm học vấn (201) |
| 7 | `PUT` | `/api/v1/profiles/me/educations/{id}` | Sửa học vấn |
| 8 | `DELETE` | `/api/v1/profiles/me/educations/{id}` | Xóa học vấn (204) |
| 9 | `GET` | `/api/v1/profiles/me/certificates` | Danh sách chứng chỉ |
| 10 | `GET` | `/api/v1/profiles/me/certificates/{id}` | Chi tiết một chứng chỉ |
| 11 | `POST` | `/api/v1/profiles/me/certificates` | Thêm chứng chỉ (201) |
| 12 | `PUT` | `/api/v1/profiles/me/certificates/{id}` | Sửa chứng chỉ |
| 13 | `DELETE` | `/api/v1/profiles/me/certificates/{id}` | Xóa chứng chỉ (204) |
| 14 | `POST` | `/api/v1/files` | Tải tệp lên (PDF/JPG/PNG ≤ 5MB), trả `{ url }` |
| 15 | `GET` | `/api/v1/files/{storedName}` | Tải tệp xuống |

### 2.3. Kiểm tra tệp tải lên (`file/MagicBytesValidator.java`, `file/FileUploadService.java`)

- **Nhận dạng bằng magic bytes**, không dựa vào phần mở rộng: PDF `%PDF`, JPG `FF D8 FF`, PNG `89 50 4E 47 0D 0A 1A 0A`. Tệp `.exe` đổi tên thành `.pdf` bị từ chối.
- Dung lượng tối đa **5MB**, giới hạn multipart của Spring đặt cao hơn (10MB) để tầng nghiệp vụ trả thông báo lỗi rõ ràng thay vì lỗi hạ tầng.
- Tên tệp lưu là **UUID sinh tự động** + phần mở rộng suy ra từ nội dung, chống ghi đè và path traversal.
- Khi tải xuống, tên tệp phải khớp `[0-9a-fA-F-]{36}\.(pdf|jpg|png)` và đường dẫn sau khi chuẩn hóa phải nằm trong thư mục upload.

### 2.4. Giao diện người dùng (Frontend)

Trang hồ sơ gồm 4 thẻ:

- **Thông tin**
  - Khối "Thông tin từ LMS" hiển thị `fullName`, `email`, `program`, `classCode`, `courseName` ở trạng thái **disabled kèm biểu tượng khóa** và nhãn "Đồng bộ từ LMS".
  - Khối "Thông tin cá nhân" cho sửa `phone`, `dob`, `gender`, `address`, `avatarUrl`, `headline`, `summary`.
  - **Kiểm tra số điện thoại VN**: 10 số, bắt đầu bằng `03/05/07/08/09`; sai định dạng bị chặn ngay tại client.
  - Nút bật/tắt **trạng thái công khai** hồ sơ (`PRIVATE` ↔ `PUBLIC`).
- **Học vấn**: danh sách + modal thêm/sửa/xóa (`school`, `major`, `fromDate`, `toDate`, `gpa`, `note`), có kiểm tra bắt buộc.
- **Chứng chỉ**: danh sách + modal thêm/sửa/xóa (`name`, `issuer`, `issuedAt`, `expiresAt`) kèm **tải tệp đính kèm** qua endpoint `/api/v1/files`.
- **Kỹ năng**: tách rõ **kỹ năng đã xác thực** (chỉ đọc, từ LMS/Khảo thí/PM) và **kỹ năng tự đánh giá** (thanh trượt 0–100, lưu với `source=SELF`).

---

## 3. Ba lỗi thực tế đã sửa kèm kiểm thử

| # | Lỗi | Ảnh hưởng | Cách sửa |
|---|---|---|---|
| 1 | `ProfileService.updateProfile` ghi đè `fullName` và `email` dù chú thích ghi là bất biến | Ứng viên có thể sửa tên/email đã xác thực từ LMS | Loại hai trường khỏi luồng cập nhật; thay bằng `UpdateProfileRequest` chỉ chứa trường được phép ghi |
| 2 | Cập nhật một phần xóa các trường không gửi (và nút công khai âm thầm reset hồ sơ) | Mất dữ liệu hồ sơ | `UpdateProfileRequest` xử lý `null` = "giữ nguyên"; `visibility` chỉ đổi khi được gửi |
| 3 | `LevelConstants.LEVEL_NANG_CAO` dùng `"Năng cao"` (ă, U+0103) trong khi ràng buộc `skill_scores_level_check` và phần còn lại của hệ thống dùng `"Nâng cao"` (â, U+00E2) | **Tự đánh giá kỹ năng từ 85 điểm trở lên thất bại với lỗi 500 trên cơ sở dữ liệu thật** | Sửa hằng số về đúng `"Nâng cao"`; `SkillRecomputeService` dùng chung `LevelConstants` thay vì lặp lại chuỗi |

Lỗi #3 chỉ lộ ra khi chạy trên PostgreSQL thật: các unit test dùng Mockito nên không chạm tới ràng buộc CHECK của cơ sở dữ liệu.

---

## 4. Dữ liệu mẫu

### 4.1. Backend
Không seed sẵn hồ sơ — đúng theo yêu cầu, hồ sơ được **tạo tự động từ LMS** ở lần truy cập đầu tiên. Kỹ năng và ánh xạ kỹ năng đã có sẵn trong `data.sql` (dùng chung với HS-01).

### 4.2. Mock server (`mock/data/`)
- `users.json`: 8 tài khoản mẫu. Trường `id` là **chuỗi dạng `user-001`…`user-008`**, không phải UUID.
- `candidates.json`: 3 ứng viên kèm `educations` và `certificates` đúng tên trường của DTO backend.

| Tài khoản | Email | Mật khẩu | `X-User-Id` (mock :9001) |
|---|---|---|---|
| Ứng viên 1 | `candidate1@rikkeijobs.test` | `Test@123` | `user-001` |
| Ứng viên 2 | `candidate2@rikkeijobs.test` | `Test@123` | `user-002` |
| Ứng viên 3 | `candidate3@rikkeijobs.test` | `Test@123` | `user-003` |
| Mentor | `mentor1@rikkeijobs.test` | `Test@123` | `user-006` |

> **Phân biệt hai môi trường.** Mock server (:9001) khớp `X-User-Id` trực tiếp với `users[].id`, nên **chỉ nhận `user-00N`**; gửi UUID sẽ trả `404`. Backend thật (:8082) khai báo `X-User-Id` kiểu `UUID` trong controller, nên cần một UUID hợp lệ bất kỳ — hồ sơ sẽ được tạo tự động từ LMS ở lần gọi đầu (ví dụ `550e8400-e29b-41d4-a716-446655440000`).

---

## 5. Hướng dẫn chạy & Kiểm thử

### 5.1. Chạy Mock server + Frontend (không cần cơ sở dữ liệu)
```bash
# Terminal 1 — mock server (cổng 9001)
cd mock
node server.js

# Terminal 2 — frontend (cổng 5173, proxy /api -> 9001)
cd frontend
npm install
npm run dev
```
Đăng nhập `candidate1@rikkeijobs.test` / `Test@123` → tự chuyển tới `/candidate/profile`.

### 5.2. Chạy Backend thật
```bash
# Cần PostgreSQL tại localhost:5432 (database profile_db)
cd backend/profile-service
./gradlew bootRun
```
Frontend trỏ vào backend thật bằng biến môi trường:
```bash
VITE_BACKEND_URL=http://localhost:8082 npm run dev
```

### 5.3. Chạy kiểm thử
```bash
cd backend/profile-service
./gradlew test          # 71 test, cần Docker cho Testcontainers
```
```bash
cd frontend
npm run build           # tsc -b && vite build
```

---

## 6. Kết quả kiểm tra nghiệm thu

| # | Tiêu chí nghiệm thu | Kết quả | Chi tiết kiểm tra |
|---|---|:---:|---|
| 1 | Lần đầu truy cập, hồ sơ tự tạo và điền từ LMS | **ĐẠT** | `ProfileIntegrationTest.profile_is_auto_created_from_lms_on_first_access` — `fullName`, `email`, `program`, `classCode`, `courseName` khớp dữ liệu LMS, `visibility = PRIVATE` |
| 2 | Trường LMS không sửa được | **ĐẠT** | `ProfileIntegrationTest.updating_profile_keeps_lms_fields_and_applies_editable_ones` — gửi kèm `fullName`/`email`/`program`/`classCode`/`courseName` giả, tất cả giữ nguyên giá trị LMS; trên giao diện các ô này ở trạng thái `disabled` |
| 3 | Cập nhật một phần không làm mất dữ liệu | **ĐẠT** | `ProfileIntegrationTest.partial_update_does_not_wipe_other_fields` — gửi mỗi `visibility` vẫn giữ `phone`, `headline` |
| 4 | Thêm/sửa/xóa học vấn | **ĐẠT** | `ProfileIntegrationTest.education_crud_round_trip` (201 → 200 → 204 → danh sách rỗng) |
| 5 | Thêm/sửa/xóa chứng chỉ | **ĐẠT** | `ProfileIntegrationTest.certificate_crud_round_trip` |
| 6 | Kỹ năng tự đánh giá lưu với `source=SELF` và không tạo điểm xác thực | **ĐẠT** | `ProfileIntegrationTest.self_assessed_skill_shows_up_in_hs01_skill_profile` — `selfScore = 88`, `verifiedScore = null`, evidence `source=SELF` |
| 7 | Tệp tải lên tải xuống nguyên vẹn | **ĐẠT** | `ProfileIntegrationTest.uploaded_file_can_be_downloaded_back_unchanged` — nội dung byte-for-byte, `Content-Type: application/pdf` |
| 8 | Tệp giả dạng bị từ chối | **ĐẠT** | `FileControllerTest.post_files_returns_400_when_content_is_not_an_allowed_type` — header `MZ` (PE) đổi tên `.pdf` bị trả 400 |
| 9 | Tệp vượt 5MB bị từ chối | **ĐẠT** | `FileControllerTest.post_files_returns_400_when_too_large` |
| 10 | Chặn path traversal khi tải tệp | **ĐẠT** | `FileControllerTest.get_files_returns_400_when_stored_name_escapes_the_upload_directory` và `..._is_not_a_generated_uuid` |
| 11 | Thiếu `X-User-Id` trả 401 | **ĐẠT** | `ProfileIntegrationTest.profile_endpoints_require_identity_header` |
| 12 | Số điện thoại VN sai định dạng bị chặn | **ĐẠT** | Kiểm chứng trên trình duyệt: nhập `12345` → hiện "Số điện thoại không hợp lệ", không gửi request; sửa thành `0987654321` → lưu thành công |
| 13 | Toàn bộ test backend xanh | **ĐẠT** | `./gradlew test` → **71/71 pass, 0 failure, 0 error** |
| 14 | Frontend build sạch | **ĐẠT** | `npm run build` → `tsc -b` không lỗi, `vite build` thành công |
| 15 | Giao diện chạy đúng trên trình duyệt thật | **ĐẠT** | Chrome headless (Playwright) đăng nhập và thao tác toàn bộ luồng: 4 thẻ, modal học vấn/chứng chỉ/kỹ năng, tải tệp PDF thật, bật/tắt công khai, tự đánh giá — **0 lỗi console** |

---

## 7. Minh họa cURL kiểm thử nhanh

Biến dùng chung (chạy với **mock server** cổng 9001):
```bash
UID1="user-001"
BASE="http://localhost:9001/api/v1"
```

> Khi thử với **backend thật** cổng 8082, đổi `BASE="http://localhost:8082/api/v1"` và `UID1` thành một UUID hợp lệ, ví dụ `550e8400-e29b-41d4-a716-446655440000`.

### 1) Lấy hồ sơ (tự tạo từ LMS ở lần đầu)
```bash
curl -X GET "$BASE/profiles/me" -H "X-User-Id: $UID1"
```

### 2) Cập nhật hồ sơ — thử cả trường LMS để thấy bị bỏ qua
```bash
curl -X PUT "$BASE/profiles/me" \
  -H "X-User-Id: $UID1" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "0987654321",
    "headline": "Java Backend Developer",
    "visibility": "PUBLIC",
    "fullName": "TÊN GIẢ",
    "email": "gia@evil.test"
  }'
```
Kết quả: `phone`, `headline`, `visibility` đổi; `fullName` và `email` giữ nguyên giá trị LMS.

### 3) Thêm học vấn
```bash
curl -X POST "$BASE/profiles/me/educations" \
  -H "X-User-Id: $UID1" \
  -H "Content-Type: application/json" \
  -d '{
    "school": "Đại học Bách Khoa Hà Nội",
    "major": "Kỹ thuật phần mềm",
    "fromDate": "2021-09-01",
    "toDate": "2025-06-30",
    "gpa": 3.45,
    "note": "Học bổng loại Giỏi"
  }'
```

### 4) Thêm chứng chỉ
```bash
curl -X POST "$BASE/profiles/me/certificates" \
  -H "X-User-Id: $UID1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Oracle Certified Professional, Java SE 11",
    "issuer": "Oracle",
    "issuedAt": "2026-05-20",
    "fileUrl": "/files/1f2e3d4c-0000-4000-8000-000000000000.pdf",
    "fileType": "PDF"
  }'
```

### 5) Tải tệp lên
```bash
curl -X POST "$BASE/files" \
  -H "X-User-Id: $UID1" \
  -F "file=@/duong/dan/chung-chi.pdf"
```
Response: `{ "url": "/files/<uuid>.pdf" }`

Tệp không phải PDF/JPG/PNG (kể cả khi đổi đuôi) hoặc vượt 5MB đều trả `400` kèm thông báo tiếng Việt.

### 6) Tự đánh giá kỹ năng
```bash
curl -X PUT "$BASE/profiles/me/self-skills" \
  -H "X-User-Id: $UID1" \
  -H "Content-Type: application/json" \
  -d '[{"skillId": "<uuid-kỹ-năng>", "selfScore": 88}]'
```
Kiểm tra kết quả trong hồ sơ kỹ năng HS-01:
```bash
curl -X GET "$BASE/profiles/me/skills" -H "X-User-Id: $UID1"
```
