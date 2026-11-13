# HS-05: Công khai hồ sơ ứng viên (Profile Visibility Workflow)

> Tài liệu chạy thử, dữ liệu mẫu và tiêu chí nghiệm thu cho HS-05.
> Hợp đồng API đầy đủ: [`docs/api/openapi/profile-service-hs05.yaml`](api/openapi/profile-service-hs05.yaml).

**Mục tiêu:** doanh nghiệp chỉ thấy hồ sơ khi **ứng viên đồng ý công khai** và **cán bộ đã duyệt**.

---

## 1. Luồng trạng thái

```
PRIVATE ──(ứng viên gửi duyệt, consent=true)──► PENDING ──(cán bộ duyệt)──► PUBLIC
   ▲                                              │
   │                                              └──(cán bộ trả lại, có lý do)──► REJECTED
   │                                                                                   │
   │◄──(ứng viên rút, hiệu lực ngay)── PENDING/PUBLIC                                   │
   │                                                                                   │
   └─────────────────(ứng viên sửa rồi gửi lại, consent=true)◄─────────────────────────┘
```

| Chuyển trạng thái | Ai làm | Điều kiện |
|---|---|---|
| `PRIVATE` → `PENDING` | Ứng viên | `consent = true` |
| `REJECTED` → `PENDING` | Ứng viên | `consent = true` |
| `PENDING` → `PUBLIC` | Cán bộ | — |
| `PENDING` → `REJECTED` | Cán bộ | `reason` bắt buộc |
| `PENDING` / `PUBLIC` → `PRIVATE` | Ứng viên | — |

Chuyển sai luật → **409**. Thiếu `consent` hoặc thiếu `reason` → **400**.

---

## 2. Chạy nhanh (Mock Server + Frontend)

Không cần cơ sở dữ liệu — mock server phản chiếu đúng hợp đồng API backend, gồm cả luồng HS-05.

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
Khối **“Công khai hồ sơ”** nằm ở tab **Thông tin**.

> Mock server giữ lịch sử duyệt trong bộ nhớ. Khởi động lại là về trạng thái seed ban đầu.

---

## 3. Chạy Backend thật (cần PostgreSQL)

```bash
# Cần PostgreSQL tại localhost:5432 (database profile_db)
cd backend/profile-service
./gradlew bootRun
```

Frontend trỏ vào backend thật:

```bash
cd frontend
VITE_BACKEND_URL=http://localhost:8082 npm run dev
```

Bảng `profile_reviews` và hai cột `consent_at`, `consent_withdrawn_at` được tạo tự động bởi
`schema.sql` (`spring.sql.init.mode=always`), không cần chạy migration thủ công.

---

## 4. Chạy kiểm thử

```bash
# Backend (cần Docker cho Testcontainers)
cd backend/profile-service
./gradlew test          # 103 test

# Frontend
cd frontend
npm run build           # tsc -b && vite build
npm run lint
```

---

## 5. Dữ liệu mẫu

### 5.1 Mock Server (cổng 9001)

| Tài khoản | Email | Mật khẩu | `X-User-Id` | `X-User-Role` | Trạng thái hồ sơ ban đầu |
|---|---|---|---|---|---|
| Ứng viên 1 | `candidate1@rikkeijobs.test` | `Test@123` | `user-001` | `CANDIDATE` | `PRIVATE` — demo luồng gửi duyệt |
| Ứng viên 2 | `candidate2@rikkeijobs.test` | `Test@123` | `user-002` | `CANDIDATE` | `PENDING` — demo danh sách chờ duyệt |
| Ứng viên 3 | `candidate3@rikkeijobs.test` | `Test@123` | `user-003` | `CANDIDATE` | `PUBLIC` — demo hồ sơ đã công khai |
| Cán bộ tuyển dụng | `staff1@rikkeijobs.test` | `Test@123` | `user-007` | `STAFF` | — |

> Mock server dùng `user-00N` làm ID. Backend thật (:8082) dùng UUID.

### 5.2 Backend thật (cổng 8082)

Không seed sẵn hồ sơ — hồ sơ được **tự động tạo từ LMS** ở lần truy cập đầu tiên, mặc định `PRIVATE`.
Dùng UUID bất kỳ hợp lệ, ví dụ: `550e8400-e29b-41d4-a716-446655440000`.

Endpoint cán bộ bắt buộc gửi kèm `X-User-Role: ROLE_STAFF` (hoặc `ROLE_ADMIN`); thiếu header trả **403**.

---

## 6. API nhanh (cURL)

Biến dùng chung (mock server):

```bash
BASE="http://localhost:9001/api/v1"
CAND="user-001"
STAFF="user-007"
```

### 1) Gửi duyệt khi chưa tích đồng ý → 400

```bash
curl -i -X POST "$BASE/profiles/me/publish" \
  -H "X-User-Id: $CAND" -H "Content-Type: application/json" \
  -d '{"consent": false}'
# HTTP/1.1 400 Bad Request
```

### 2) Gửi duyệt khi đã đồng ý → PENDING

```bash
curl -s -X POST "$BASE/profiles/me/publish" \
  -H "X-User-Id: $CAND" -H "Content-Type: application/json" \
  -d '{"consent": true}' | grep visibility
# "visibility": "PENDING"
```

### 3) Gửi duyệt lần nữa khi đang PENDING → 409

```bash
curl -i -X POST "$BASE/profiles/me/publish" \
  -H "X-User-Id: $CAND" -H "Content-Type: application/json" \
  -d '{"consent": true}'
# HTTP/1.1 409 Conflict
```

### 4) Cán bộ xem danh sách chờ duyệt

```bash
curl -s "$BASE/staff/profile-reviews?status=PENDING" \
  -H "X-User-Id: $STAFF" -H "X-User-Role: ROLE_STAFF"
```

### 5) Cán bộ trả lại không có lý do → 400

```bash
curl -i -X POST "$BASE/staff/profiles/$CAND/reject" \
  -H "X-User-Id: $STAFF" -H "X-User-Role: ROLE_STAFF" \
  -H "Content-Type: application/json" -d '{}'
# HTTP/1.1 400 Bad Request
```

### 6) Cán bộ trả lại kèm lý do → REJECTED

```bash
curl -s -X POST "$BASE/staff/profiles/$CAND/reject" \
  -H "X-User-Id: $STAFF" -H "X-User-Role: ROLE_STAFF" \
  -H "Content-Type: application/json" \
  -d '{"reason":"Thiếu minh chứng kỹ năng"}'
```

### 7) Ứng viên đọc lý do bị trả lại trong lịch sử

```bash
curl -s "$BASE/profiles/me/review-history" -H "X-User-Id: $CAND"
# [ { "action": "REJECT", "reason": "Thiếu minh chứng kỹ năng", "actorId": "user-007", ... },
#   { "action": "SUBMIT", ... } ]
```

### 8) Gửi lại → PENDING, cán bộ duyệt → PUBLIC

```bash
curl -s -X POST "$BASE/profiles/me/publish" \
  -H "X-User-Id: $CAND" -H "Content-Type: application/json" -d '{"consent": true}'

curl -s -X POST "$BASE/staff/profiles/$CAND/approve" \
  -H "X-User-Id: $STAFF" -H "X-User-Role: ROLE_STAFF"
# {"userId":"user-001","visibility":"PUBLIC"}
```

### 9) Ứng viên rút → PRIVATE ngay

```bash
curl -s -X POST "$BASE/profiles/me/withdraw" -H "X-User-Id: $CAND" | grep visibility
# "visibility": "PRIVATE"
```

### 10) Duyệt hồ sơ đang PRIVATE → 409

```bash
curl -i -X POST "$BASE/staff/profiles/$CAND/approve" \
  -H "X-User-Id: $STAFF" -H "X-User-Role: ROLE_STAFF"
# HTTP/1.1 409 Conflict
```

---

## 7. Tiêu chí nghiệm thu

| # | Tiêu chí | Cách kiểm chứng |
|---|---|---|
| 1 | Chưa tích đồng ý → không gửi duyệt được | `POST /profiles/me/publish` với `consent:false` → **400** |
| 2 | Cán bộ trả lại không có lý do → 400 | `POST /staff/profiles/{id}/reject` body rỗng → **400** |
| 3 | Có lý do → ứng viên thấy lý do | `GET /profiles/me/review-history` → phần tử `REJECT` có `reason` |
| 4 | Hồ sơ PUBLIC → HR tìm thấy | `PublicProfileQuery.isVisibleToRecruiters(userId)` → `true` |
| 5 | Ứng viên rút → HR tìm lại không thấy ngay | sau `POST /profiles/me/withdraw` → `isVisibleToRecruiters` → `false` |
| 6 | Approve hồ sơ đang PRIVATE → 409 | `POST /staff/profiles/{id}/approve` khi PRIVATE → **409** |
| 7 | Lịch sử ghi đủ từng lần chuyển, người làm, thời điểm | `GET /profiles/me/review-history` → mỗi bản ghi có `action`, `actorId`, `createdAt` |
| 8 | OpenAPI cập nhật endpoint mới | [`docs/api/openapi/profile-service-hs05.yaml`](api/openapi/profile-service-hs05.yaml) |

Toàn bộ tiêu chí trên được kiểm bằng test tự động:
`backend/profile-service/src/test/java/com/example/profileservice/profile/integration/VisibilityWorkflowIntegrationTest.java`

---

## 8. Cấu trúc giao diện (Frontend)

Khối **“Công khai hồ sơ”** trong tab **Thông tin** của `/candidate/profile`
(`src/features/profile/components/VisibilitySection.tsx`):

| Trạng thái | Hiển thị |
|---|---|
| `PRIVATE` | Nhãn “Chưa công khai”, ô tích đồng ý điều khoản, nút **Gửi duyệt công khai** (khóa khi chưa tích) |
| `PENDING` | Nhãn “Đang chờ duyệt”, nút **Rút lại yêu cầu duyệt** |
| `PUBLIC` | Nhãn “Đã công khai”, nút **Rút công khai hồ sơ** |
| `REJECTED` | Nhãn đỏ “Bị trả lại”, hộp **Lý do bị trả lại**, ô tích đồng ý, nút **Gửi duyệt lại** |

Bên dưới là bảng **Lịch sử duyệt hồ sơ**: Hành động · Lý do · Người thực hiện · Thời gian.

---

## 9. Ảnh chụp màn hình (Placeholder)

> Cần bổ sung ảnh thật trước khi nộp PR.

| # | Nội dung | Đường dẫn |
|---|---|---|
| 1 | Trạng thái PRIVATE — ô tích đồng ý + nút Gửi duyệt (nút khóa) | `docs/images/hs05-01-private.png` |
| 2 | Trạng thái PENDING — nút Rút lại | `docs/images/hs05-02-pending.png` |
| 3 | Trạng thái REJECTED — hộp lý do bị trả lại | `docs/images/hs05-03-rejected.png` |
| 4 | Trạng thái PUBLIC — nút Rút công khai | `docs/images/hs05-04-public.png` |
| 5 | Bảng lịch sử duyệt hồ sơ | `docs/images/hs05-05-history.png` |

---

## 10. Liên kết hữu ích

- OpenAPI: [`docs/api/openapi/profile-service-hs05.yaml`](api/openapi/profile-service-hs05.yaml)
- Quy chuẩn API response: [`docs/quy-chuan-api-response.md`](quy-chuan-api-response.md)
- Tài liệu HS-02 (nền tảng hồ sơ): [`docs/hs02-candidate-profile.md`](hs02-candidate-profile.md)
- Phụ thuộc: **HS-02**. Màn cán bộ đầy đủ làm ở **HS-06**.
