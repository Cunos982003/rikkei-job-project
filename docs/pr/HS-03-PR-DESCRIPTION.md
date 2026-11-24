# [HS-03] Hồ sơ ứng viên: Quản lý Portfolio dự án (dự án & ảnh)

## 1. Mô tả tổng quan
- **Mục tiêu**: Ứng viên có thể trình bày các dự án tiêu biểu (portfolio) trên hồ sơ để nhà tuyển dụng đánh giá năng lực thực tế. Hỗ trợ tạo/sửa/xóa dự án, tải lên/xóa/sắp xếp ảnh minh chứng (tối đa 6 ảnh/dự án), điều khiển hiển thị công khai/riêng tư.
- **Service Backend**: `backend/profile-service` (cổng 8082).
- **Frontend**: `frontend/src/pages/candidate/CandidateProfilePage.tsx` (thẻ "Portfolio" tại route `/candidate/profile`).
- **Bảng dữ liệu mới**: `portfolio_projects`, `portfolio_media`.
- **Tài liệu liên quan**: [`HS-02-PR-DESCRIPTION.md`](./HS-02-PR-DESCRIPTION.md) — cùng `profile-service`, dùng chung `X-User-Id` header và cơ chế file upload an toàn (magic bytes, 5MB).

---

## 2. Thay đổi chi tiết

### 2.1. Cơ sở dữ liệu (`backend/profile-service/src/main/resources/db/migration/V3__portfolio_hs03.sql`)

- **Bảng `portfolio_projects`** — các dự án portfolio của ứng viên
  - `id` UUID PK, `user_id` FK → `candidate_profiles.user_id`
  - `name` (NOT NULL, ≤300), `role` (≤300), `description` (≤2000), `tech_stack` (≤500)
  - `repo_url`, `demo_url` (≤500, validate URL)
  - `start_date`, `end_date` (DATE, `end_date` nullable = đang thực hiện)
  - `visible` BOOLEAN DEFAULT true (công khai/riêng tư)
  - `sort_order` INT DEFAULT 0 (thứ tự hiển thị)
  - `source` VARCHAR(20): `PM` | `SELF` | `IOC` | `MANUAL`
  - `source_ref` VARCHAR(255) (ID tham chiếu hệ thống gốc, ví dụ `internship_id`)
  - `internship_id` UUID (liên kết thực tập IOC, nullable)

- **Bảng `portfolio_media`** — ảnh minh chứng cho dự án
  - `id` UUID PK, `project_id` FK → `portfolio_projects.id` (CASCADE DELETE)
  - `url` VARCHAR(500) (đường dẫn relative `/files/{uuid}.jpg|png`)
  - `media_type` VARCHAR(20) DEFAULT 'IMAGE'
  - `sort_order` INT DEFAULT 0 (thứ tự ảnh trong dự án)
  - Ràng buộc: tối đa 6 ảnh/dự án (thực thi ở tầng service)

### 2.2. Danh sách API Backend

Tất cả endpoint xác thực qua header `X-User-Id` (UUID). Thiếu header → **401**.

| STT | Phương thức | Đường dẫn | Mô tả |
|---|---|---|---|
| 1 | `GET` | `/api/v1/profiles/me/portfolio` | Lấy toàn bộ portfolio của chính mình (kèm media, sort theo `sort_order`) |
| 2 | `GET` | `/api/v1/profiles/{userId}/portfolio` | Lấy portfolio công khai của người khác (`visible = true`) |
| 3 | `POST` | `/api/v1/profiles/me/portfolio` | Tạo dự án mới (201). Dự án IOC **không** cho tạo thủ công |
| 4 | `PUT` | `/api/v1/profiles/me/portfolio/{projectId}` | Cập nhật dự án. Dự án IOC: chỉ cho sửa trường được phép, cấm đổi `source`, `source_ref`, `internship_id` |
| 5 | `DELETE` | `/api/v1/profiles/me/portfolio/{projectId}` | Xóa dự án (204). Dự án IOC **không** cho xóa. Xóa kèm ảnh trên MinIO |
| 6 | `PUT` | `/api/v1/profiles/me/portfolio/order` | Sắp xếp lại thứ tự dự án (`{ projectIds: UUID[] }`) |
| 7 | `POST` | `/api/v1/profiles/me/portfolio/{projectId}/media` | Tải ảnh lên (multipart `file`, JPG/PNG ≤ 5MB). Tối đa 6 ảnh/dự án. Trả `{ id, url, mediaType, sortOrder }` |
| 8 | `DELETE` | `/api/v1/profiles/me/portfolio/{projectId}/media/{mediaId}` | Xóa ảnh (204). Xóa file trên MinIO |
| 9 | `PUT` | `/api/v1/profiles/me/portfolio/{projectId}/media/order` | Sắp xếp lại thứ tự ảnh (`{ mediaIds: UUID[] }`) |

### 2.3. Bảo vệ dự án IOC (Internship Sync)

- Dự án IOC (`source = 'IOC'`) được đồng bộ từ hệ thống PM/Thực tập, **không** cho ứng viên tạo/sửa/xóa thủ công
- Các trường bị khóa: `source`, `source_ref`, `internship_id`
- Các trường cho phép sửa: `name`, `role`, `description`, `tech_stack`, `repo_url`, `demo_url`, `start_date`, `end_date`, `visible`, `sort_order`
- UI hiển thị badge **IOC** (màu đỏ) và badge **MANUAL** (outline) để phân biệt

### 2.4. Tải ảnh an toàn (Magic Bytes)

- Dùng chung `MagicBytesValidator` và `MinioFileUploadService` từ HS-02
- Chỉ chấp nhận JPG (`FF D8 FF`) và PNG (`89 50 4E 47 0D 0A 1A 0A`)
- Dung lượng tối đa **5MB**, multipart limit 10MB (Spring config)
- Tên file lưu: UUID + extension từ nội dung file
- Xóa ảnh → xóa luôn file trên MinIO

### 2.5. Giao diện người dùng (Frontend)

**Thẻ "Portfolio" trên `/candidate/profile`**:

- **Header**: Tiêu đề, mô tả, nút "Thêm dự án", thống kê nhanh (số dự án, số ảnh, công khai, IOC)
- **Trạng thái loading**: Skeleton grid 3 cột
- **Empty state**: Card border-dashed với illustration Briefcase, nút "Tạo dự án đầu tiên"
- **Grid dự án**: Responsive `1 cột (mobile) → 2 cột (tablet) → 3 cột (desktop)`
- **Card dự án** (tất cả cùng bố cục nhất quán):
  - **Header row**: Tiêu đề (có tooltip tên đầy đủ) + Badge "Công khai"/"Riêng tư" trong flow bình thường + Badge Role (nếu có)
  - **Source badge** (góc phải): IOC (primary) / MANUAL (outline) + tooltip đầy đủ (`Source: IOC | Ref: ...`)
  - **Mô tả** (chỉ hiển thị khi có)
  - **Tech Stack** (chỉ hiển thị khi có): Các tag chip tối đa 5, chip "+N nữa" cho phần thừa
  - **Meta Info** (ngày, Repo, Demo) — divider chỉ hiện khi có mô tả/tech stack
  - **Media Gallery** (luôn hiển thị):
    - Khi có ảnh: Grid 2→3→4→6 cột, hover zoom + overlay actions (xem lớn, xóa)
    - Khi chưa có ảnh: Khung nét đứt vuông (`aspect-square`), icon + chữ "Kéo thả hoặc click để tải ảnh" căn giữa, không xuống dòng
    - Dự án IOC: Thông báo "Dự án IOC không hỗ trợ tải ảnh"
    - Loading placeholder khi đang upload
- **Modal Tạo/Sửa dự án**: Form đầy đủ (name*, role, description, techStack, repoUrl, demoUrl, startDate, endDate, visible checkbox), validation client:
  - Name bắt buộc
  - URL phải bắt đầu bằng `http://` hoặc `https://`
  - `startDate ≤ endDate` (nếu cả hai có)
- **Modal xác nhận xóa ảnh**: Hỏi trước khi xóa

---

## 3. Các lỗi thực tế đã sửa

| # | Lỗi | Ảnh hưởng | Cách sửa |
|---|---|---|---|
| 1 | Badge "Công khai" đè lên tiêu đề (absolute positioning) | Tiêu đề bị che, UX kém | Di chuyển badge vào flex flow bên cạnh tiêu đề |
| 2 | Tiêu đề bị truncate quá sớm | Không thấy tên đầy đủ | Bỏ `truncate`, dùng `flex-1 min-w-0` + `title` tooltip |
| 3 | Double border/empty space khi không có mô tả/tech stack | Card trông không đồng bộ | Conditional divider: chỉ render `border-t` khi có content phía trên |
| 4 | Upload area text wrap, không căn giữa | UI lộn xộn trên card ngắn | `aspect-square min-h-[160px]` + `whitespace-nowrap` + center flex |
| 5 | Layout không đồng bộ giữa card IOC và MANUAL | UX kém nhất quán | Chuẩn hóa thứ tự blocks, spacing `space-y-4` trong CardContent |
| 6 | SourceRef bị cắt không có tooltip | Không biết ID gốc đầy đủ | Thêm `title` attribute trên badge source và span sourceRef |
| 7 | Shared error state giữa các form tab | Lỗi portfolio lẫn vào form khác | Thêm `portfolioErrors` state riêng biệt |
| 8 | File upload không accessible (document.createElement) | Không dùng bàn phím được | (Đã note để cải tiến: dùng hidden `<input ref>` + label) |

---

## 4. Dữ liệu mẫu

### 4.1. Backend
Không seed sẵn portfolio — ứng viên tự tạo. Flyway migration `V3__portfolio_hs03.sql` chỉ tạo bảng + index.

### 4.2. Mock server (`mock/data/`)
Cập nhật `candidates.json` thêm trường `portfolio` (mảng rỗng hoặc dữ liệu mẫu) để frontend dev không bị undefined.

---

## 5. Hướng dẫn chạy & Kiểm thử

### 5.1. Chạy Mock server + Frontend (không cần DB)
```bash
# Terminal 1 — mock server (cổng 9001)
cd mock
node server.js

# Terminal 2 — frontend (cổng 5173, proxy /api -> 9001)
cd frontend
npm install
npm run dev
```
Đăng nhập `candidate1@rikkeijobs.test` / `Test@123` → `/candidate/profile` → thẻ **Portfolio**.

### 5.2. Chạy Backend thật
```bash
# Cần PostgreSQL tại localhost:5432 (database profile_db)
cd backend/profile-service
./gradlew bootRun
```
Frontend trỏ vào backend thật:
```bash
VITE_BACKEND_URL=http://localhost:8082 npm run dev
```

### 5.3. Chạy kiểm thử
```bash
cd backend/profile-service
./gradlew test          # Cần Docker cho Testcontainers
```
```bash
cd frontend
npm run build           # tsc -b && vite build
```

---

## 6. Kết quả kiểm tra nghiệm thu

| # | Tiêu chí nghiệm thu | Kết quả | Chi tiết kiểm tra |
|---|---|:---:|---|
| 1 | Tạo dự án mới (MANUAL) thành công | **ĐẠT** | POST `/portfolio` → 201, trả DTO kèm media rỗng, `sort_order` tự gán cuối |
| 2 | Không cho tạo dự án IOC thủ công | **ĐẠT** | POST với `internshipId` → 400 "Dự án thực tập (IOC) không thể tạo thủ công" |
| 3 | Cập nhật dự án MANUAL đầy đủ trường | **ĐẠT** | PUT `/portfolio/{id}` → 200, mọi trường cập nhật đúng |
| 4 | Cập nhật dự án IOC chỉ cho phép trường whitelist | **ĐẠT** | PUT IOC với `source`/`sourceRef`/`internshipId` mới → giá trị cũ giữ nguyên |
| 5 | Xóa dự án MANUAL thành công, xóa kèm ảnh MinIO | **ĐẠT** | DELETE → 204, `portfolio_media` cascade, file MinIO xóa |
| 6 | Không cho xóa dự án IOC | **ĐẠT** | DELETE IOC → 400 "Không thể xóa dự án thực tập (IOC)" |
| 7 | Sắp xếp lại thứ tự dự án | **ĐẠT** | PUT `/portfolio/order` với mảng ID → `sort_order` cập nhật đúng vị trí |
| 8 | Tải ảnh lên (JPG/PNG ≤ 5MB) thành công | **ĐẠT** | POST `/portfolio/{id}/media` multipart → 201, trả media DTO, file có trên MinIO |
| 9 | Từ chối file không phải ảnh (magic bytes) | **ĐẠT** | File `.exe` đổi tên `.jpg` → 400 "Định dạng tệp không được hỗ trợ" |
| 10 | Từ chối file > 5MB | **ĐẠT** | File 6MB → 400 "Tệp vượt quá dung lượng cho phép (tối đa 5MB)" |
| 11 | Giới hạn 6 ảnh/dự án | **ĐẠT** | Upload ảnh thứ 7 → 400 "Mỗi dự án tối đa 6 ảnh" |
| 12 | Xóa ảnh thành công, xóa file MinIO | **ĐẠT** | DELETE `/media/{mediaId}` → 204, file MinIO xóa |
| 13 | Sắp xếp lại thứ tự ảnh | **ĐẠT** | PUT `/media/order` → `sort_order` ảnh cập nhật đúng |
| 14 | Lấy portfolio công khai chỉ thấy `visible=true` | **ĐẠT** | GET `/profiles/{userId}/portfolio` → không trả dự án `visible=false` |
| 15 | UI: Badge "Công khai" không đè tiêu đề | **ĐẠT** | Kiểm tra Chrome DevTools: badge trong flow, không `absolute` |
| 16 | UI: Tiêu đề dài có tooltip đầy đủ | **ĐẠT** | Hover title → hiện tooltip tên đầy đủ |
| 17 | UI: Không double border khi thiếu mô tả/tech stack | **ĐẠT** | Card chỉ có 1 đường kẻ ngang giữa các section có nội dung |
| 18 | UI: Upload area căn giữa, không wrap | **ĐẠT** | Khung vuông, icon + chữ một dòng, centered |
| 19 | UI: Layout card IOC/MANUAL đồng bộ | **ĐẠT** | Cùng thứ tự blocks, cùng spacing, source badge góc phải |
| 20 | SourceRef hiển thị tooltip đầy đủ | **ĐẠT** | Hover badge source / span sourceRef → hiện ref đầy đủ |
| 21 | Thiếu `X-User-Id` trả 401 | **ĐẠT** | Tất cả endpoint portfolio trả 401 khi thiếu header |
| 22 | Toàn bộ test backend xanh | **ĐẠT** | `./gradlew test` → pass |
| 23 | Frontend build sạch | **ĐẠT** | `npm run build` → `tsc -b` không lỗi, `vite build` thành công |
| 24 | Giao diện chạy đúng trên trình duyệt thật | **ĐẠT** | Chrome: tạo/sửa/xóa dự án, upload/xóa ảnh, reorder (drag-drop backend ready), toggle visible, IOC protection — **0 lỗi console** |

---

## 7. Minh họa cURL kiểm thử nhanh

Biến dùng chung (mock server cổng 9001):
```bash
UID1="user-001"
BASE="http://localhost:9001/api/v1"
```

> Backend thật cổng 8082: `BASE="http://localhost:8082/api/v1"`, `UID1` = UUID hợp lệ.

### 1) Lấy portfolio của chính mình
```bash
curl -X GET "$BASE/profiles/me/portfolio" -H "X-User-Id: $UID1"
```

### 2) Tạo dự án mới
```bash
curl -X POST "$BASE/profiles/me/portfolio" \
  -H "X-User-Id: $UID1" -H "Content-Type: application/json" \
  -d '{
    "name": "Hệ thống quản lý tuyển dụng",
    "role": "Fullstack Developer",
    "description": "Xây dựng từ đầu đến cuối: React + Node.js + PostgreSQL",
    "techStack": "React, Node.js, PostgreSQL, Docker",
    "repoUrl": "https://github.com/user/recruitment-system",
    "demoUrl": "https://demo.recruitment.test",
    "startDate": "2024-01-15",
    "endDate": "2024-06-30",
    "visible": true
  }'
```

### 3) Cập nhật dự án
```bash
curl -X PUT "$BASE/profiles/me/portfolio/<PROJECT_ID>" \
  -H "X-User-Id: $UID1" -H "Content-Type: application/json" \
  -d '{
    "name": "Hệ thống quản lý tuyển dụng v2",
    "role": "Tech Lead",
    "visible": false
  }'
```

### 4) Xóa dự án
```bash
curl -X DELETE "$BASE/profiles/me/portfolio/<PROJECT_ID>" -H "X-User-Id: $UID1"
```

### 5) Sắp xếp lại thứ tự dự án
```bash
curl -X PUT "$BASE/profiles/me/portfolio/order" \
  -H "X-User-Id: $UID1" -H "Content-Type: application/json" \
  -d '{"projectIds": ["<id-3>", "<id-1>", "<id-2>"]}'
```

### 6) Tải ảnh lên cho dự án
```bash
curl -X POST "$BASE/profiles/me/portfolio/<PROJECT_ID>/media" \
  -H "X-User-Id: $UID1" \
  -F "file=@/duong/dan/anh-du-an.jpg"
```

### 7) Xóa ảnh
```bash
curl -X DELETE "$BASE/profiles/me/portfolio/<PROJECT_ID>/media/<MEDIA_ID>" -H "X-User-Id: $UID1"
```

### 8) Sắp xếp lại thứ tự ảnh
```bash
curl -X PUT "$BASE/profiles/me/portfolio/<PROJECT_ID>/media/order" \
  -H "X-User-Id: $UID1" -H "Content-Type: application/json" \
  -d '{"mediaIds": ["<media-2>", "<media-1>", "<media-3>"]}'
```

### 9) Lấy portfolio công khai của người khác
```bash
curl -X GET "$BASE/profiles/<OTHER_USER_ID>/portfolio"
```

---

## 8. File chính đã thay đổi

### Backend (`backend/profile-service/`)
- `src/main/resources/db/migration/V3__portfolio_hs03.sql` — Migration tạo bảng + index
- `src/main/java/com/example/profileservice/entity/PortfolioProject.java`
- `src/main/java/com/example/profileservice/entity/PortfolioMedia.java`
- `src/main/java/com/example/profileservice/repository/PortfolioProjectRepository.java`
- `src/main/java/com/example/profileservice/repository/PortfolioMediaRepository.java`
- `src/main/java/com/example/profileservice/dto/PortfolioProjectDto.java` — DTO + Reorder request DTOs
- `src/main/java/com/example/profileservice/service/PortfolioService.java` — Business logic, IOC protection, validation
- `src/main/java/com/example/profileservice/controller/ProfileController.java` — 9 endpoint portfolio
- `src/main/resources/application.yaml` — `spring.servlet.multipart.max-file-size: 10MB`

### Frontend (`frontend/`)
- `src/api/endpoints/profile.api.ts` — Types & API functions (getMyPortfolio, create/update/delete, upload, reorder)
- `src/pages/candidate/CandidateProfilePage.tsx` — Thẻ Portfolio UI hoàn chỉnh
  - State: `isProjectModalOpen`, `editingProject`, `projectForm`, `uploadingMedia`, `deleteMediaConfirm`, `portfolioErrors`
  - Queries: `getMyPortfolio`
  - Mutations: `createProject`, `updateProject`, `deleteProject`, `uploadMedia`, `deleteMedia`
  - Handlers: `openProjectModal`, `handleProjectSubmit`, `handleDeleteProject`, `handleMediaUpload`, `handleDeleteMedia`
  - UI: Stats bar, Loading skeleton, Empty state, Project grid, Project card (header, description, tech stack, meta, media gallery), Modals

---

## 9. Ghi chú triển khai

1. **Reorder UI**: Backend đã sẵn sàng endpoint `reorderProjects` và `reorderMedia`. Frontend hiện tại chưa có drag-drop UI (chỉ gọi API), có thể bổ sung `@dnd-kit` sau.
2. **File upload accessibility**: Hiện dùng `document.createElement('input')` click. Khuyến nghị refactor sang hidden `<input type="file" ref>` + labelled button để hỗ trợ bàn phím/screen reader.
3. **TechStack delimiter**: Hiện dùng comma `,`. Nếu tên công nghệ có dấu phẩy (ví dụ "React, Redux"), sẽ bị tách sai. Có thể đổi sang `;` hoặc backend lưu `string[]`.
4. **IOC sync**: Endpoint đồng bộ từ PM chưa triển khai (out of scope HS-03). Khi có, chỉ cần INSERT/UPDATE `source='IOC'`, `source_ref=internship_id`.