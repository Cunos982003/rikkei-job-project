# [DN-02] Quản lý tin tuyển dụng (JD) theo vị trí và kỹ năng yêu cầu

## 1. Mô tả tổng quan
- **Mục tiêu**: Hỗ trợ Nhà tuyển dụng (HR) tạo bản nháp tin tuyển dụng (JD), chỉnh sửa thông tin vị trí, chế độ, đãi ngộ, thiết lập danh sách kỹ năng bắt buộc / ưu tiên từ danh mục kỹ năng chuẩn của hệ thống, gửi phê duyệt tới Cán bộ (STAFF) và đóng tin khi hết nhu cầu. Đồng thời cung cấp API tra cứu JD cho ứng viên và cán bộ vận hành.
- **Service Backend chính**: `backend/job-service` (cổng 8085).
- **Service phụ trợ (bước 0)**: `backend/profile-service` (cổng 8082, bổ sung API `GET /api/v1/skills`).
- **Routing API Gateway**: `backend/api-gateway` (cổng 8080, route `profile-skills` và `job-jobs`).
- **OpenAPI Specification**: `docs/api/openapi/job-service-dn02.yaml`.

---

## 2. Danh sách API Backend (7 Endpoints DN-02)

| STT | Phương thức | Đường dẫn | Phân quyền | Mô tả |
|:---:|:---:|:---|:---:|:---|
| 1 | `POST` | `/jobs` | `HR` | Tạo mới bản nháp tin tuyển dụng (trạng thái `DRAFT`), kiểm tra deadline $\ge$ hôm nay, lương hợp lệ |
| 2 | `PUT` | `/jobs/{id}` | `HR` | Chỉnh sửa thông tin JD. Nếu đang `APPROVED` tự động chuyển về `PENDING`; đang `PENDING` hoặc `CLOSED` trả về `409` |
| 3 | `PUT` | `/jobs/{id}/skills` | `HR` | Cập nhật toàn bộ danh sách kỹ năng của JD (tối đa 30 kỹ năng, kiểm tra trùng lặp và tra cứu qua catalog) |
| 4 | `POST` | `/jobs/{id}/submit` | `HR` | Gửi duyệt tin tuyển dụng. Chỉ gửi từ `DRAFT` hoặc `REJECTED`, yêu cầu $\ge$ 1 kỹ năng bắt buộc (`required=true`) |
| 5 | `POST` | `/jobs/{id}/close` | `HR` | Đóng tin tuyển dụng. Áp dụng cho mọi trạng thái trừ `CLOSED` (`CLOSED` trả về `409`) |
| 6 | `GET` | `/companies/me/jobs` | `HR` | Danh sách tin tuyển dụng của công ty mình có phân trang (`page`, `size`) và lọc `status` |
| 7 | `GET` | `/jobs/{id}` | Authenticated | Xem chi tiết JD. HR chỉ xem được JD công ty mình; STAFF/ADMIN xem được mọi JD; Ứng viên chỉ xem được JD `APPROVED` |

---

## 3. Luồng trạng thái tin tuyển dụng (Job State Transitions)

```
        +--------------+
        |    Tạo mới   |
        +-------+------+
                |
                v
          +-----------+         Chỉnh sửa JD / skills
          |   DRAFT   +<-----------------------------------+
          +-----+-----+                                    |
                | (submit: >=1 kỹ năng bắt buộc)           |
                v                                          |
         +-------------+                                   |
   +---->+   PENDING   +<-----+ (Sửa JD đang APPROVED)    |
   |     +------+------+      |                            |
   |            |             |                            |
   |  [DN-07]   | [DN-07]     |                            |
   |  Từ chối   | Duyệt tin   |                            |
   |            v             |                            |
+--+---+ +-------------+      |                            |
|REJECT| |  APPROVED   +------+                            |
+------+ +------+------+                                   |
                |                                          |
                +-----------------+                        |
                |                 |                        |
                v                 v                        |
          +-----------+     +-----------+                  |
          |  CLOSED   |     |  CLOSED   +------------------+
          +-----------+     +-----------+
```

- **Chỉnh sửa khi `APPROVED`**: Tin lập tức chuyển về `PENDING`, xóa `rejectedReason`, chờ Cán bộ duyệt lại nội dung mới.
- **Bảo vệ tính toàn vẹn**: Không cho phép sửa khi tin đang ở trạng thái `PENDING` (chờ duyệt) hoặc `CLOSED` (đã đóng) $\rightarrow$ trả về `409 Conflict`.
- **Gửi duyệt**: Chỉ được phép gửi khi đang ở `DRAFT` hoặc `REJECTED`. Bắt buộc phải có ít nhất 1 kỹ năng có `required = true` và `deadline` $\ge$ ngày hiện tại.

---

## 4. Phần thêm ở profile-service (GET /api/v1/skills)
- Đã thống nhất với thành viên phụ trách phân hệ Hồ sơ (HS-01) để tái sử dụng bảng danh mục chuẩn `skills`.
- Endpoint: `GET /api/v1/skills`
  - Tham số tùy chọn: `?group=` (lọc theo nhóm kỹ năng) và `?q=` (tìm kiếm theo tên hoặc mã code không phân biệt hoa thường).
  - Định dạng Envelope chuẩn: `{ success: true, statusCode: 200, message: "...", data: [...], timestamp: "..." }`.
  - Phân quyền: Yêu cầu đăng nhập (truyền `Authorization: Bearer <token>` hoặc header `X-User-Id`). Thiếu cả hai sẽ trả về `401 Unauthorized`.
  - Thứ tự sắp xếp mặc định: `group ASC`, sau đó đến `name ASC`.

---

## 5. Hướng dẫn chạy và Biến cấu hình

### Biến môi trường mới:
- `SKILL_CATALOG_STUB`:
  - `false` (mặc định cho production): Kết nối tới `profile-service` qua REST API `GET /api/v1/skills` với bộ nhớ đệm (cache) 5 phút, timeout kết nối 3s, timeout đọc 5s.
  - `true` (mặc định cho local và test): Kích hoạt `StubSkillCatalogClient` cung cấp sẵn 20 kỹ năng chuẩn, không cần phụ thuộc vào việc chạy đồng thời `profile-service`.
- `PROFILE_SERVICE_URL`: URL dịch vụ `profile-service` (mặc định `http://localhost:8082`).

### Cách chạy kiểm thử tự động:
```bash
# 1. Chạy toàn bộ test của job-service
cd backend/job-service
./gradlew test

# 2. Chạy test của profile-service
cd backend/profile-service
./gradlew test

# 3. Chạy test của api-gateway
cd backend/api-gateway
./gradlew test
```

---

## 6. Dữ liệu mẫu (Seed Data)
Dữ liệu mẫu nạp tự động khi khởi chạy với profile `local`/`test` từ tệp `mock/data/jobs.json`:
1. `Lập trình viên Java Backend (Fresher / Junior)`: Công ty Cổ phần Rikkeisoft (`comp-001`), trạng thái ban đầu `DRAFT`.
2. `Frontend React Developer (Fresher)`: Rikkei Digital Solutions (`comp-002`), trạng thái ban đầu `PENDING`.
3. `Thực tập sinh Fullstack (Dự án IOC)`: Công ty Cổ phần Rikkeisoft (`comp-001`), hình thức `INTERNSHIP`, trạng thái ban đầu `APPROVED`.

---

## 7. Bảng đối chiếu Tiêu chí nghiệm thu với Test

| STT | Tiêu chí nghiệm thu | Tên Test Case tương ứng | File Test | Kết quả |
|:---:|:---|:---|:---|:---:|
| 1 | JD không có kỹ năng bắt buộc $\rightarrow$ submit `400` | `submitJob_noSkills_returns400` | `JobControllerIntegrationTest` | PASS |
| 2 | Kỹ năng chỉ có ưu tiên (`required=false`) $\rightarrow$ submit `400` | `submitJob_onlyOptionalSkills_returns400` | `JobControllerIntegrationTest` | PASS |
| 3 | Hạn nộp quá khứ hoặc lương min > max $\rightarrow$ create `400` | `createJob_invalidContent_returns400` | `JobControllerIntegrationTest` | PASS |
| 4 | HR công ty A thao tác JD công ty B $\rightarrow$ `404 Not Found` | `hr1_accessJobCompanyB_returns404` | `JobControllerIntegrationTest` | PASS |
| 5 | Sửa JD APPROVED $\rightarrow$ PENDING; sửa PENDING $\rightarrow$ 409; CLOSED $\rightarrow$ 409 | `updateJob_statusTransitions_andConflicts` | `JobControllerIntegrationTest` | PASS |
| 6 | PUT skills mã không tồn tại hoặc bị trùng $\rightarrow$ `400` | `replaceSkills_validationErrors_returns400` | `JobControllerIntegrationTest` | PASS |
| 7 | Tạo tin JD loại INTERNSHIP thành công | `createJob_internship_success` | `JobControllerIntegrationTest` | PASS |
| 8 | Luồng đầy đủ: Create $\rightarrow$ PUT skills $\rightarrow$ Submit $\rightarrow$ PENDING $\rightarrow$ List phân trang | `fullFlow_createSkillsSubmitAndList` | `JobControllerIntegrationTest` | PASS |
| 9 | Ứng viên xem JD APPROVED được (`200`), xem JD DRAFT bị `404` | `candidateViewJob_onlyApproved` | `JobControllerIntegrationTest` | PASS |
| 10 | Kiểm thử đơn vị các quy tắc nghiệp vụ trạng thái (Clock cố định) | Toàn bộ 11 tests trong `JobServiceTest` | `JobServiceTest` | PASS |
| 11 | Danh mục kỹ năng tại profile-service: sắp xếp, lọc, tìm kiếm | `getSkills_sortedByGroupThenName`, `filterByGroup`, `searchByQuery` | `SkillCatalogServiceTest` | PASS |
| 12 | Controller danh mục kỹ năng: 401 khi thiếu token, 200 khi có token | `getSkills_unauthorized_whenNoAuthHeaders`, `getSkills_success_*` | `SkillCatalogControllerTest` | PASS |

---

## 8. Hình ảnh minh họa (Screenshots)
> *Dán ảnh chụp màn hình kiểm thử giao diện hoặc kết quả Postman / Swagger UI tại đây.*

