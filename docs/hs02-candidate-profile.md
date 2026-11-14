# HS-02: Hồ sơ ứng viên (Candidate Profile)

> Tài liệu chạy thử và dữ liệu mẫu cho HS-02. Xem đầy đủ chi tiết kỹ thuật tại [`docs/pr/HS-02-PR-DESCRIPTION.md`](../pr/HS-02-PR-DESCRIPTION.md).

---

## 1. Chạy nhanh (Mock Server + Frontend)

Không cần cơ sở dữ liệu — mock server phản chiếu đúng hợp đồng API backend.

```bash
# Terminal 1 — Mock server (cổng 9001)
cd mock
node server.js

# Terminal 2 — Frontend (cổng 5173, proxy /api -> 9001)
cd frontend
npm install
npm run dev
```

Đăng nhập: `candidate1@rikkeijobs.test` / `Test@123` → tự chuyển tới `/candidate/profile`.

---

## 2. Chạy Backend thật (cần PostgreSQL)

```bash
# Cần PostgreSQL tại localhost:5432 (database profile_db)
cd backend/profile-service
./gradlew bootRun
```

Frontend trỏ vào backend thật:
```bash
VITE_BACKEND_URL=http://localhost:8082 npm run dev
```

---

## 3. Chạy kiểm thử

```bash
# Backend (cần Docker cho Testcontainers)
cd backend/profile-service
./gradlew test          # 71 test

# Frontend
cd frontend
npm run build           # tsc -b && vite build
```

---

## 4. Dữ liệu mẫu

### 4.1 Mock Server (cổng 9001)

| Tài khoản | Email | Mật khẩu | `X-User-Id` |
|---|---|---|---|
| Ứng viên 1 | `candidate1@rikkeijobs.test` | `Test@123` | `user-001` |
| Ứng viên 2 | `candidate2@rikkeijobs.test` | `Test@123` | `user-002` |
| Ứng viên 3 | `candidate3@rikkeijobs.test` | `Test@123` | `user-003` |
| Mentor | `mentor1@rikkeijobs.test` | `Test@123` | `user-006` |

> **Lưu ý**: Mock server dùng `user-00N` làm ID. Backend thật (:8082) dùng UUID.

### 4.2 Backend thật (cổng 8082)

Không seed sẵn hồ sơ — hồ sơ được **tự động tạo từ LMS** ở lần truy cập đầu tiên.
Dùng UUID bất kỳ hợp lệ, ví dụ: `550e8400-e29b-41d4-a716-446655440000`.

---

## 5. API nhanh (cURL)

Biến dùng chung (mock server):
```bash
UID1="user-001"
BASE="http://localhost:9001/api/v1"
```

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

### 6) Tự đánh giá kỹ năng
```bash
curl -X PUT "$BASE/profiles/me/self-skills" \
  -H "X-User-Id: $UID1" \
  -H "Content-Type: application/json" \
  -d '[{"skillId": "<uuid-kỹ-năng>", "selfScore": 88}]'
```

Kiểm tra trong hồ sơ kỹ năng HS-01:
```bash
curl -X GET "$BASE/profiles/me/skills" -H "X-User-Id: $UID1"
```

---

## 6. Tiêu chí nghiệm thu (tóm tắt)

| # | Tiêu chí | Test xác thực |
|---|---|---|
| 1 | Hồ sơ tự tạo từ LMS lần đầu | `ProfileIntegrationTest.profile_is_auto_created_from_lms_on_first_access` |
| 2 | Trường LMS bất biến | `ProfileIntegrationTest.updating_profile_keeps_lms_fields_and_applies_editable_ones` |
| 3 | Cập nhật một phần không mất dữ liệu | `ProfileIntegrationTest.partial_update_does_not_wipe_other_fields` |
| 4 | CRUD học vấn | `ProfileIntegrationTest.education_crud_round_trip` |
| 5 | CRUD chứng chỉ | `ProfileIntegrationTest.certificate_crd_round_trip` |
| 6 | Kỹ năng tự đánh giá `source=SELF` | `ProfileIntegrationTest.self_assessed_skill_shows_up_in_hs01_skill_profile` |
| 7 | Tải tệp lên/xuống nguyên vẹn | `ProfileIntegrationTest.uploaded_file_can_be_downloaded_back_unchanged` |
| 8 | Tệp giả dạng bị từ chối | `FileControllerTest.post_files_returns_400_when_content_is_not_an_allowed_type` |
| 9 | Tệp > 5MB bị từ chối | `FileControllerTest.post_files_returns_400_when_too_large` |
| 10 | Chặn path traversal | `FileControllerTest.get_files_returns_400_when_stored_name_escapes_the_upload_directory` |
| 11 | Thiếu `X-User-Id` → 401 | `ProfileIntegrationTest.profile_endpoints_require_identity_header` |
| 12 | SĐT VN sai định dạng bị chặn | Kiểm thử thủ công trên trình duyệt |
| 13 | Toàn bộ test backend xanh | `./gradlew test` → 71/71 pass |
| 14 | Frontend build sạch | `npm run build` thành công |

---

## 7. Cấu trúc giao diện (Frontend)

Trang `/candidate/profile` gồm 4 thẻ:

1. **Thông tin**
   - Khối "Thông tin từ LMS" (disabled, icon khóa, nhãn "Đồng bộ từ LMS"): `fullName`, `email`, `program`, `classCode`, `courseName`
   - Khối "Thông tin cá nhân" (có sửa): `phone` (validate VN 10 số), `dob`, `gender`, `address`, `avatarUrl`, `headline`, `summary`
   - Nút bật/tắt công khai hồ sơ (`PRIVATE` ↔ `PUBLIC`)

2. **Học vấn**: Danh sách + modal thêm/sửa/xóa (`school`, `major`, `fromDate`, `toDate`, `gpa`, `note`)

3. **Chứng chỉ**: Danh sách + modal thêm/sửa/xóa (`name`, `issuer`, `issuedAt`, `expiresAt`) + tải tệp đính kèm (PDF/JPG/PNG ≤ 5MB)

4. **Kỹ năng**: 
   - Kỹ năng đã xác thực (chỉ đọc, từ LMS/Khảo thí/PM)
   - Kỹ năng tự đánh giá (thanh trượt 0–100, lưu `source=SELF`)

---

## 8. Ảnh chụp màn hình (Placeholder)

> [TODO: Thêm ảnh chụp màn hình khi có giao diện hoàn thiện]
> - Trang hồ sơ: 4 thẻ (Thông tin, Học vấn, Chứng chỉ, Kỹ năng)
> - Modal thêm/sửa học vấn
> - Modal thêm/sửa chứng chỉ với upload tệp
> - Modal tự đánh giá kỹ năng (slider 0-100)
> - Trạng thái khóa trường LMS (icon khóa + "Đồng bộ từ LMS")

---

## 9. Các lỗi đã sửa (tóm tắt từ PR Description)

1. **LMS fields overwritten** — `ProfileService.updateProfile` ghi đè `fullName`/`email` → Sửa: dùng `UpdateProfileRequest` chỉ chứa trường cho phép ghi
2. **Partial update wipes fields** — Cập nhật một phần xóa các trường không gửi → Sửa: `null` = "giữ nguyên"
3. **Level constant typo** — `LEVEL_NANG_CAO` dùng `"Năng cao"` (ă) thay vì `"Nâng cao"` (â) → gây lỗi 500 trên PostgreSQL thật do CHECK constraint → Sửa: thống nhất `"Nâng cao"`

---

## 10. Liên kết hữu ích

- [PR Description đầy đủ](../pr/HS-02-PR-DESCRIPTION.md)
- [HS-01 PR Description](../pr/HS-01-PR-DESCRIPTION.md) (chia sẻ `skill_evidences`, `skill_scores`)
- [Kiến trúc dự án](../../kien-truc-du-an.md)
- [Quy chuẩn API Response](../../quy-chuan-api-response.md)