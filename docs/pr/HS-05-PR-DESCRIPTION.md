# [HS-05] Công khai hồ sơ ứng viên (Profile Visibility Workflow)

## 1. Mô tả tổng quan
- **Mục tiêu**: Triển khai luồng duyệt hồ sơ ứng viên — ứng viên gửi yêu cầu công khai, cán bộ tuyển dụng duyệt/trả lại, hồ sơ mới hiển thị cho nhà tuyển dụng. Đảm bảo: doanh nghiệp chỉ thấy hồ sơ khi **ứng viên đồng ý công khai** VÀ **cán bộ đã duyệt**.
- **Service Backend**: `backend/profile-service` (cổng 8082).
- **Frontend**: `frontend/src/features/profile/components/VisibilitySection.tsx` (trong trang `/candidate/profile`).
- **Mock server**: `mock/server.js` (cổng 9001) — phản chiếu đúng hợp đồng API backend.
- **Tài liệu liên quan**: [`HS-02-PR-DESCRIPTION.md`](./HS-02-PR-DESCRIPTION.md) (nền tảng hồ sơ), [`HS-06-PR-DESCRIPTION.md`](./HS-06-PR-DESCRIPTION.md) (kiến trúc mới & API Response chuẩn hóa).
- **OpenAPI**: `docs/api/openapi/profile-service-hs05.yaml`.
- **Cập nhật từ HS-06**: `profile-service` đã tái cấu trúc theo kiến trúc lớp (layered architecture) giống `auth-service` và chuẩn hóa API Response theo `docs/quy-chuan-api-response.md` (thông báo tiếng Việt, envelope `ApiResponse`, phân trang `PageData`, `GlobalExceptionHandler` tập trung).

---

## 2. Thay đổi chi tiết

### 2.1. Cơ sở dữ liệu (`backend/profile-service/src/main/resources/schema.sql`)

Bảng mới cho luồng duyệt:

```sql
CREATE TABLE profile_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    old_visibility VARCHAR(20) NOT NULL,
    new_visibility VARCHAR(20) NOT NULL,
    actor_id UUID,
    reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    consent_at TIMESTAMPTZ,
    consent_withdrawn_at TIMESTAMPTZ
);
CREATE INDEX idx_profile_reviews_user_id ON profile_reviews(user_id);
```

Cột mới trong `candidate_profiles` (tự động qua `schema.sql` với `spring.sql.init.mode=always`):
- `consent_at` — thời điểm ứng viên đồng ý công khai
- `consent_withdrawn_at` — thời điểm ứng viên rút lại sự đồng ý

### 2.2. Luồng trạng thái (State Machine)

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

Chuyển sai luật → **409 Conflict**. Thiếu `consent` hoặc thiếu `reason` → **400 Bad Request**.

### 2.3. Danh sách API Backend

Mọi endpoint xác định danh tính qua header `X-User-Id` (UUID). Thiếu header → **401**.
Endpoint cán bộ bắt buộc `X-User-Role: ROLE_STAFF` hoặc `ROLE_ADMIN`; thiếu → **403**.

**Tất cả phản hồi tuân thủ chuẩn `ApiResponse<T>`** (5 trường: `success`, `statusCode`, `message`, `data`, `timestamp`) với **thông báo tiếng Việt**. Phân trang dùng `PageData<T>` với `PaginationInfo`.

| STT | Phương thức | Đường dẫn | Phân quyền | Mô tả |
|---|---|---|---|---|
| 1 | `POST` | `/api/v1/profiles/me/publish` | `CANDIDATE` | Gửi duyệt công khai (body: `{consent: true}`) |
| 2 | `POST` | `/api/v1/profiles/me/withdraw` | `CANDIDATE` | Rút hồ sơ (về `PRIVATE`) |
| 3 | `GET` | `/api/v1/profiles/me/review-history` | `CANDIDATE` | Lịch sử duyệt hồ sơ |
| 4 | `GET` | `/api/v1/staff/profile-reviews` | `STAFF/ADMIN` | Danh sách hồ sơ chờ duyệt (phân trang, lọc `status`, `q`, `classCode`) |
| 5 | `GET` | `/api/v1/staff/profiles/{userId}/preview` | `STAFF/ADMIN` | Xem preview hồ sơ ứng viên |
| 6 | `POST` | `/api/v1/staff/profiles/{userId}/approve` | `STAFF/ADMIN` | Duyệt hồ sơ (`PENDING` → `PUBLIC`) |
| 7 | `POST` | `/api/v1/staff/profiles/{userId}/reject` | `STAFF/ADMIN` | Trả lại hồ sơ (`PENDING` → `REJECTED`, body: `{reason}`) |
| 8 | `PUT` | `/api/v1/staff/profiles/{userId}/self-declared` | `STAFF/ADMIN` | Cán bộ sửa trường tự khai hộ ứng viên |

### 2.4. Domain Model & Events (Cập nhật từ HS-06)

**Value Object `Visibility`** (`common/domain/Visibility.java`):
- Enum type-safe: `PRIVATE`, `PENDING`, `PUBLIC`, `REJECTED`, `RECRUITER`

**Entity `CandidateProfile`** — Domain Events:
- `domainEvents: List<DomainEvent>` — gom sự kiện trong transaction
- `submitForReview(consent, actorId)` → phát `ProfileVisibilityChangedEvent(PRIVATE→PENDING)`
- `approve(actorId)` → phát `ProfileVisibilityChangedEvent(PENDING→PUBLIC)`
- `reject(actorId, reason)` → phát `ProfileVisibilityChangedEvent(PENDING→REJECTED)`
- `withdraw(actorId)` → phát `ProfileVisibilityChangedEvent(PENDING/PUBLIC→PRIVATE)`
- `getDomainEvents()`, `clearDomainEvents()` — để publisher dispatch

**Domain Events** (`common/event/`):
- `ProfileVisibilityChangedEvent` — `eventId`, `occurredAt`, `userId`, `oldVisibility`, `newVisibility`
- `ProfileCreatedEvent`, `ProfileUpdatedEvent`

### 2.5. Service Layer (Cập nhật từ HS-06)

`VisibilityService` (`service/VisibilityService.java`) — trung tâm logic duyệt:
- Inject: `SkillProfileService`, `ProfileQueryService`, `CandidateProfileRepository`, `ProfileReviewRepository`
- `submitForReview(userId, consent)` — validate `consent=true`, thay đổi visibility, lưu review
- `approve(userId, staffId)` — chỉ từ `PENDING`, lưu review, cập nhật `consent_at`
- `reject(userId, staffId, reason)` — validate `reason` không rỗng, lưu review
- `withdraw(userId)` — từ `PENDING`/`PUBLIC` về `PRIVATE`, cập nhật `consent_withdrawn_at`
- `listByStatus(visibility, keyword, classCode, pageable)` — trả `Page<PendingReviewDto>` cho phân trang chuẩn
- `getPreviewProfile(userId)` — trả `StaffPreviewProfileDto` đầy đủ
- `editSelfDeclared(userId, staffId, request)` — cán bộ sửa hộ, ghi `SkillEvidence(source=MENTOR)`
- `getReviewHistory(userId)` — lịch sử chuyển trạng thái

### 2.6. Controllers (Cập nhật từ HS-06)

| Controller | Endpoints | Thay đổi HS-06 |
|---|---|---|
| `ProfileController` | `/profiles/me/publish`, `/withdraw`, `/review-history` | Vietnamese messages, xóa `@ExceptionHandler`, dùng `ApiResponse` |
| `StaffProfileController` | `/staff/profile-reviews`, `/staff/profiles/{id}/preview|approve|reject|self-declared` | Vietnamese messages, `PageData<PendingReviewDto>`, xóa `@ExceptionHandler` |

### 2.7. Xử lý ngoại lệ tập trung (HS-06)

- `GlobalExceptionHandler` (`common/exception/GlobalExceptionHandler.java`):
  - `MethodArgumentNotValidException` → 400 (Validation, tiếng Việt)
  - `InvalidTransitionException` → 409 (Conflict, tiếng Việt)
  - `IllegalArgumentException` → 400 (tiếng Việt)
  - `Exception` → 500 (tiếng Việt)
- Đã xóa `@ExceptionHandler` ở `ProfileController` và `StaffProfileController`.

### 2.8. Giao diện người dùng (Frontend)

Khối **"Công khai hồ sơ"** trong tab **Thông tin** của `/candidate/profile`
(`src/features/profile/components/VisibilitySection.tsx`):

| Trạng thái | Hiển thị |
|---|---|
| `PRIVATE` | Nhãn "Chưa công khai", ô tích đồng ý điều khoản, nút **Gửi duyệt công khai** (khóa khi chưa tích) |
| `PENDING` | Nhãn "Đang chờ duyệt", nút **Rút lại yêu cầu duyệt** |
| `PUBLIC` | Nhãn "Đã công khai", nút **Rút công khai hồ sơ** |
| `REJECTED` | Nhãn đỏ "Bị trả lại", hộp **Lý do bị trả lại**, ô tích đồng ý, nút **Gửi duyệt lại** |

Bên dưới là bảng **Lịch sử duyệt hồ sơ**: Hành động · Lý do · Người thực hiện · Thời gian.

---

## 3. Dữ liệu mẫu

### 3.1 Mock Server (cổng 9001)

| Tài khoản | Email | Mật khẩu | `X-User-Id` | `X-User-Role` | Trạng thái hồ sơ ban đầu |
|---|---|---|---|---|---|---|
| Ứng viên 1 | `candidate1@rikkeijobs.test` | `Test@123` | `user-001` | `CANDIDATE` | `PRIVATE` — demo luồng gửi duyệt |
| Ứng viên 2 | `candidate2@rikkeijobs.test` | `Test@123` | `user-002` | `CANDIDATE` | `PENDING` — demo danh sách chờ duyệt |
| Ứng viên 3 | `candidate3@rikkeijobs.test` | `Test@123` | `user-003` | `CANDIDATE` | `PUBLIC` — demo hồ sơ đã công khai |
| Cán bộ tuyển dụng | `staff1@rikkeijobs.test` | `Test@123` | `user-007` | `STAFF` | — |

> Mock server dùng `user-00N` làm ID. Backend thật (:8082) dùng UUID.

### 3.2 Backend thật (cổng 8082)

Không seed sẵn hồ sơ — hồ sơ được **tự động tạo từ LMS** ở lần truy cập đầu tiên, mặc định `PRIVATE`.
Dùng UUID bất kỳ hợp lệ, ví dụ: `550e8400-e29b-41d4-a716-446655440000`.

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
Đăng nhập `candidate1@rikkeijobs.test` / `Test@123` → tự chuyển tới `/candidate/profile` → tab Thông tin → khối "Công khai hồ sơ".

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
./gradlew test          # 113 test, cần Docker cho Testcontainers
```
```bash
cd frontend
npm run build           # tsc -b && vite build
npm run lint
```

---

## 5. Kết quả kiểm tra nghiệm thu

| # | Tiêu chí nghiệm thu | Kết quả | Chi tiết kiểm tra |
|---|---|:---:|---|
| 1 | Chưa tích đồng ý → không gửi duyệt được | **ĐẠT** | `POST /profiles/me/publish` với `consent:false` → **400**, message VN: "Thiếu tham số bắt buộc: consent" |
| 2 | Cán bộ trả lại không có lý do → 400 | **ĐẠT** | `POST /staff/profiles/{id}/reject` body rỗng → **400**, message VN: "Lý do trả lại là bắt buộc" |
| 3 | Có lý do → ứng viên thấy lý do | **ĐẠT** | `GET /profiles/me/review-history` → phần tử `REJECT` có `reason` |
| 4 | Hồ sơ PUBLIC → HR tìm thấy | **ĐẠT** | `PublicProfileQuery.isVisibleToRecruiters(userId)` → `true` |
| 5 | Ứng viên rút → HR tìm lại không thấy ngay | **ĐẠT** | sau `POST /profiles/me/withdraw` → `isVisibleToRecruiters` → `false` |
| 6 | Approve hồ sơ đang PRIVATE → 409 | **ĐẠT** | `POST /staff/profiles/{id}/approve` khi PRIVATE → **409**, message VN |
| 7 | Lịch sử ghi đủ từng lần chuyển, người làm, thời điểm | **ĐẠT** | `GET /profiles/me/review-history` → mỗi bản ghi có `action`, `actorId`, `createdAt` |
| 8 | OpenAPI cập nhật endpoint mới | **ĐẠT** | `docs/api/openapi/profile-service-hs05.yaml` |
| 9 | **API Response chuẩn hóa** — Tất cả endpoint HS-05 dùng `ApiResponse` envelope, thông báo tiếng Việt | **ĐẠT** | `StaffProfileControllerTest`, `ProfileControllerTest` — response có `success`, `statusCode`, `message` (VN), `data`, `timestamp` |
| 10 | **Phân trang chuẩn hóa** — `GET /staff/profile-reviews` trả `PageData<PendingReviewDto>` | **ĐẠT** | `StaffProfileControllerTest.listProfileReviews_returnsPageData` — `data.items`, `data.pagination` |
| 11 | **Xử lý ngoại lệ tập trung** — Không còn `@ExceptionHandler` ở controller, dùng `GlobalExceptionHandler` | **ĐẠT** | Validation 400, NotFound 404, Conflict 409, Internal 500 — tất cả tiếng Việt |
| 12 | Domain Events phát đúng khi thay đổi visibility | **ĐẠT** | `CandidateProfileDomainEventTest` — 4 transitions: submit, approve, reject, withdraw |
| 13 | Toàn bộ test backend xanh | **ĐẠT** | `./gradlew test` → **113/113 pass, 0 failure, 0 error** |
| 14 | Frontend build sạch | **ĐẠT** | `npm run build` → `tsc -b` không lỗi, `vite build` thành công |

---

## 6. Minh họa cURL kiểm thử nhanh

Biến dùng chung (mock server):
```bash
BASE="http://localhost:9001/api/v1"
CAND="user-001"
STAFF="user-007"
```

> Khi thử với **backend thật** cổng 8082, đổi `BASE="http://localhost:8082/api/v1"` và dùng UUID hợp lệ.
> **Tất cả phản hồi tuân thủ `ApiResponse<T>`** với **thông báo tiếng Việt**.

### 1) Gửi duyệt khi chưa tích đồng ý → 400
```bash
curl -i -X POST "$BASE/profiles/me/publish" \
  -H "X-User-Id: $CAND" -H "Content-Type: application/json" \
  -d '{"consent": false}'
# HTTP/1.1 400 Bad Request
# ApiResponse { success: false, statusCode: 400, message: "Thiếu tham số bắt buộc: consent" }
```

### 2) Gửi duyệt khi đã đồng ý → PENDING
```bash
curl -s -X POST "$BASE/profiles/me/publish" \
  -H "X-User-Id: $CAND" -H "Content-Type: application/json" \
  -d '{"consent": true}'
# ApiResponse { message: "Gửi hồ sơ duyệt thành công", data: { visibility: "PENDING", ... } }
```

### 3) Gửi duyệt lần nữa khi đang PENDING → 409
```bash
curl -i -X POST "$BASE/profiles/me/publish" \
  -H "X-User-Id: $CAND" -H "Content-Type: application/json" \
  -d '{"consent": true}'
# HTTP/1.1 409 Conflict
# ApiResponse { success: false, statusCode: 409, message: "Không thể gửi duyệt: hồ sơ không ở trạng thái cho phép" }
```

### 4) Cán bộ xem danh sách chờ duyệt (phân trang chuẩn)
```bash
curl -s "$BASE/staff/profile-reviews?status=PENDING" \
  -H "X-User-Id: $STAFF" -H "X-User-Role: ROLE_STAFF"
# ApiResponse { 
#   message: "Lấy danh sách hồ sơ chờ duyệt thành công", 
#   data: { 
#     items: [...], 
#     pagination: { page: 0, size: 20, totalElements: 5, totalPages: 1, isFirst: true, isLast: true, hasNext: false, hasPrevious: false }
#   }
# }
```

### 5) Cán bộ trả lại không có lý do → 400
```bash
curl -i -X POST "$BASE/staff/profiles/$CAND/reject" \
  -H "X-User-Id: $STAFF" -H "X-User-Role: ROLE_STAFF" \
  -H "Content-Type: application/json" -d '{}'
# HTTP/1.1 400 Bad Request
# ApiResponse { success: false, statusCode: 400, message: "Lý do trả lại là bắt buộc" }
```

### 6) Cán bộ trả lại kèm lý do → REJECTED
```bash
curl -s -X POST "$BASE/staff/profiles/$CAND/reject" \
  -H "X-User-Id: $STAFF" -H "X-User-Role: ROLE_STAFF" \
  -H "Content-Type: application/json" \
  -d '{"reason":"Thiếu minh chứng kỹ năng"}'
# ApiResponse { message: "Trả lại hồ sơ thành công", data: { userId: "...", visibility: "REJECTED" } }
```

### 7) Ứng viên đọc lý do bị trả lại trong lịch sử
```bash
curl -s "$BASE/profiles/me/review-history" -H "X-User-Id: $CAND"
# ApiResponse { message: "Lấy lịch sử duyệt thành công", data: [ { "action": "REJECT", "reason": "Thiếu minh chứng kỹ năng", "actorId": "user-007", ... }, { "action": "SUBMIT", ... } ] }
```

### 8) Gửi lại → PENDING, cán bộ duyệt → PUBLIC
```bash
curl -s -X POST "$BASE/profiles/me/publish" \
  -H "X-User-Id: $CAND" -H "Content-Type: application/json" -d '{"consent": true}'

curl -s -X POST "$BASE/staff/profiles/$CAND/approve" \
  -H "X-User-Id: $STAFF" -H "X-User-Role: ROLE_STAFF"
# ApiResponse { message: "Duyệt hồ sơ thành công", data: { userId: "user-001", visibility: "PUBLIC" } }
```

### 9) Ứng viên rút → PRIVATE ngay
```bash
curl -s -X POST "$BASE/profiles/me/withdraw" -H "X-User-Id: $CAND"
# ApiResponse { message: "Rút hồ sơ thành công", data: { visibility: "PRIVATE", ... } }
```

### 10) Duyệt hồ sơ đang PRIVATE → 409
```bash
curl -i -X POST "$BASE/staff/profiles/$CAND/approve" \
  -H "X-User-Id: $STAFF" -H "X-User-Role: ROLE_STAFF"
# HTTP/1.1 409 Conflict
# ApiResponse { success: false, statusCode: 409, message: "Không thể duyệt: hồ sơ không ở trạng thái chờ duyệt" }
```

---

## 7. Files Changed Summary (HS-05 + HS-06 Integration)

### Created (HS-05 core)
```
profile/api/dto/PendingReviewDto.java
profile/api/dto/ReviewHistoryItemDto.java
profile/api/dto/StaffPreviewProfileDto.java
profile/api/dto/StaffEditSelfDeclaredRequest.java
profile/api/dto/StaffEditSelfDeclaredResponse.java
profile/api/dto/StaffRejectRequest.java
profile/api/dto/PublishRequest.java
profile/service/VisibilityService.java
profile/api/ProfileController.java          (added publish, withdraw, review-history)
profile/api/StaffProfileController.java     (new controller for staff)
common/event/ProfileVisibilityChangedEvent.java
common/event/ProfileCreatedEvent.java
common/event/ProfileUpdatedEvent.java
```

### Modified (HS-06 restructuring)
```
common/domain/Visibility.java               (new VO, replaces inner enum)
common/domain/Level.java                    (new VO, replaces LevelConstants)
common/event/DomainEvent.java
common/event/DomainEventPublisher.java
common/api/PageData.java
common/api/ApiResponse.java
common/exception/GlobalExceptionHandler.java
common/exception/InvalidTransitionException.java
service/ProfileCommandService.java
service/SkillProfileService.java
service/LmsProfileService.java
service/ProfileMaskingService.java
service/ProfileQueryService.java
entity/CandidateProfile.java                (added domainEvents, emits events)
entity/ProfileReview.java
repository/CandidateProfileRepository.java
repository/ProfileReviewRepository.java
controller/ProfileController.java           (Vietnamese messages, ApiResponse)
controller/StaffProfileController.java      (Vietnamese messages, PageData, ApiResponse)
ProfileServiceApplication.java              (@EntityScan, @EnableJpaRepositories)
```

### Deleted
```
common/LevelConstants.java
profile/service/ProfileService.java         (empty after decomposition)
```

### Tests
```
profile/integration/VisibilityWorkflowIntegrationTest.java
common/event/CandidateProfileDomainEventTest.java
profile/service/VisibilityServiceTest.java
```

---

## 8. Liên kết hữu ích

- OpenAPI: [`docs/api/openapi/profile-service-hs05.yaml`](api/openapi/profile-service-hs05.yaml)
- Quy chuẩn API response: [`docs/quy-chuan-api-response.md`](quy-chuan-api-response.md)
- Tài liệu HS-02 (nền tảng hồ sơ): [`docs/hs02-candidate-profile.md`](hs02-candidate-profile.md)
- Tài liệu HS-06 (Kiến trúc mới & API Response chuẩn hóa): [`docs/pr/HS-06-PR-DESCRIPTION.md`](./HS-06-PR-DESCRIPTION.md)
- Phụ thuộc: **HS-02**. Màn cán bộ đầy đủ làm ở **HS-06**.