# Recruitment APIs

## API ứng tuyển của ứng viên

Các đường dẫn API công khai dưới đây có prefix `/api/v1`:

- `POST /api/v1/jobs/{id}/recruit` với dữ liệu `{ "cvId": "...", "coverLetter": "..." }`
- `GET /api/v1/me/recruitments?status=SUBMITTED`
- `GET /api/v1/me/recruitments/{id}` (bao gồm lịch sử trạng thái)
- `POST /api/v1/me/recruitments/{id}/withdraw` với dữ liệu `{ "reason": "..." }`

Các API dành cho ứng viên yêu cầu bearer JWT có claim `userId` (UUID hoặc `subject` là UUID), `role` và email. `JWT_SECRET` phải trùng với khóa ký của Auth Service. Ứng viên chỉ được có một recruitment chưa rút cho mỗi công việc; nộp trùng trả về `409 Conflict`. Sau khi rút, ứng viên có thể nộp lại và lần nộp mới tạo một recruitment riêng.

## API dành cho HR và STAFF

Các API HR/STAFF cũng yêu cầu bearer JWT. HR phải có `companyId`; STAFF phải có role STAFF. HR chỉ được xem hoặc thay đổi recruitment nếu công ty trên JWT vẫn là chủ sở hữu hiện tại của job. Việc này được kiểm tra bằng dữ liệu hiện thời từ Job Service cho từng thao tác của HR. STAFF có quyền đọc trên phạm vi nền tảng, nhưng không được đổi trạng thái hoặc sửa vòng phỏng vấn.

| Method and path | Permission | Behavior |
|---|---|---|
| `GET /api/v1/recruitments/{id}` | HR của công ty đang sở hữu job hoặc STAFF | Trả thông tin recruitment, candidate ID/email, cover letter, CV snapshot, match score, interview rounds và status history. |
| `POST /api/v1/recruitments/{id}/transition` | HR của công ty đang sở hữu job | Body: `{ "toStatus": "SCREENING", "note": "..." }`; note tùy chọn, tối đa 1000 ký tự. |
| `POST /api/v1/recruitments/{id}/rounds` | HR của công ty đang sở hữu job | Tạo vòng tiếp theo, số thứ tự tự tăng. Body `{}`; `scheduled_at` ban đầu là `null`, kết quả ban đầu là `PENDING`. |
| `PUT /api/v1/recruitments/{id}/rounds/{roundId}` | HR của công ty đang sở hữu job | Body: `{ "result": "PASS", "score": 85 }`; score tùy chọn, từ 0 đến 100. |
| `GET /api/v1/staff/recruitments?companyId=…&jobId=…&status=…&page=0` | STAFF | Danh sách lọc tùy chọn, mới nhất trước; `page` bắt đầu từ 0, kích thước trang là 20. Bỏ `companyId` để xem toàn nền tảng. |

Mọi thay đổi trạng thái đều ghi status trước/sau, người thực hiện và note vào `recruitment_status_history`. Quy tắc chuyển trạng thái:

- `SUBMITTED` → `SCREENING` hoặc `REJECTED`
- `SCREENING` → `INTERVIEW` hoặc `REJECTED`
- `INTERVIEW` → `REJECTED`
- `OFFER` → `HIRED` hoặc `REJECTED`
- `HIRED`, `REJECTED` và `WITHDRAWN` là trạng thái cuối.

API transition thông thường **không** cho phép `INTERVIEW` → `OFFER`. UT-07 là luồng riêng ngoài phạm vi hiện tại; chỉ luồng đó mới có thể đưa recruitment sang `OFFER`, sau khi mọi vòng đã có kết quả và vòng cuối cùng là `PASS`. Các vòng chỉ được tạo/sửa khi recruitment đang ở `INTERVIEW`. Điểm vòng phỏng vấn và match score đều dùng thang điểm 0–100.

STAFF list lọc công ty dựa trên snapshot `company_id` được ghi lúc nộp hồ sơ; HR detail và mutation vẫn kiểm tra quyền sở hữu live từ Job Service. Snapshot `company_id` nullable để tương thích dữ liệu cũ: recruitment legacy có `company_id = null` vẫn xuất hiện trong STAFF list không lọc công ty, nhưng không xuất hiện khi lọc theo `companyId` cho đến khi được backfill từ job tương ứng. Cần backfill các hàng cũ trước khi cần company-filter đầy đủ; không có lookup job lặp lại theo từng recruitment khi liệt kê.

## Tích hợp AI và giới hạn hồ sơ ứng viên

Recruitment Service gọi AI Career Service bằng endpoint nội bộ `GET /internal/v1/recruitments/{userId}/jobs/{jobId}/match-score`, gửi header `X-Internal-Service-Token`. Cả hai service phải nhận cùng một `INTERNAL_SERVICE_TOKEN`; URL trong container là `http://ai-career-service:8090`. Match score được tính theo thang 0–100.

Detail hiện chỉ trả dữ liệu do Recruitment Service sở hữu, không bao gồm các trường profile đầy đủ như tên, kỹ năng hoặc lịch sử việc làm. Profile Service hiện chưa có endpoint nội bộ để lấy toàn bộ hồ sơ của một candidate theo ID; cần bổ sung hợp đồng Profile Service trong tương lai nếu muốn mở rộng detail.

## Hợp đồng tích hợp với các dịch vụ khác

Dịch vụ này không quản lý dữ liệu việc làm và CV; các deployment phải cung cấp các hợp đồng tích hợp sau:

- `JOB_SERVICE_URL` + `JOB_DETAILS_PATH` (mặc định `/jobs/{jobId}`): API `GET` có xác thực, trả về JSON gồm `status` (phải là `APPROVED` mới cho phép ứng tuyển), `expiresAt` (thời điểm ISO-8601 có múi giờ; `null` nghĩa là không có hạn), `companyId` (UUID) và `hrEmails` (danh sách địa chỉ email HR).
- `PROFILE_SERVICE_URL` + `CV_PDF_PATH` (mặc định `/api/v1/cvs/{cvId}/pdf`): API `GET` có xác thực, trả về CV thuộc ứng viên dưới dạng `recruitment/pdf`. Profile service phải xác minh quyền sở hữu CV bằng bearer token được chuyển tiếp.

Dịch vụ ứng tuyển sao chép PDF vào `CV_SNAPSHOT_DIRECTORY` và lưu URL được bảo vệ `/recruitments/{id}/cv-snapshot` trong trường `cv_snapshot_url`. API snapshot cho phép chính ứng viên hoặc HR của công ty hiện đang sở hữu job tải đúng bản CV đã nộp. Gateway hiện chuyển tiếp endpoint này; đặt `PUBLIC_BASE_URL` nếu URL lưu trong dữ liệu cần là địa chỉ công khai đầy đủ.

## Vận hành và các giới hạn

- Dịch vụ dùng PostgreSQL với các biến `RECRUITMENT_DATABASE_URL`, `RECRUITMENT_DATABASE_USERNAME`, `RECRUITMENT_DATABASE_PASSWORD` trong `.env` tại thư mục gốc. Xem README gốc để tạo database; H2 chỉ dùng cho kiểm thử.
- Dịch vụ gửi email cho HR sau khi tạo recruitment và cho candidate sau mỗi lần đổi trạng thái (bao gồm rút hồ sơ), sau khi giao dịch đã commit. Candidate email được lưu thành snapshot `candidate_email` khi nộp; recruitment cũ thiếu email cần backfill trước khi đổi trạng thái. Nếu SMTP chưa cấu hình hoặc gửi email thất bại, giao dịch đã commit vẫn giữ nguyên và lỗi được ghi log; email không được bảo đảm có retry. Để bảo đảm giao nhận cần bổ sung giám sát, thử lại hoặc transactional outbox. Cấu hình `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH` và `SMTP_STARTTLS`.
- CV phải là PDF và không vượt quá `CV_MAX_PDF_BYTES` (mặc định 10 MiB).
- Không thể rút recruitment ở trạng thái `OFFER`, `WITHDRAWN`, `REJECTED` hoặc `HIRED`.
- Mỗi lần trạng thái thay đổi, dịch vụ ghi lại trạng thái trước/sau, người thực hiện và ghi chú trong `recruitment_status_history`. Bản ghi khởi tạo dùng `SUBMITTED` làm cả trạng thái trước và sau do entity hiện tại không có trạng thái `DRAFT`.
- Schema mới không đặt ràng buộc duy nhất trên `(job_id, user_id)` để cho phép nộp lại sau khi rút. Nếu cơ sở dữ liệu đã tạo ràng buộc này từ phiên bản trước, cần gỡ ràng buộc đó khi triển khai phiên bản mới.
- Đảm bảo migration hoặc cơ chế tạo schema của môi trường triển khai bao gồm cả hai entity recruitment hiện có; đồng thời cần cấp phát nơi lưu snapshot CV riêng tư và có tính bền vững. Thư mục cục bộ mặc định không được chia sẻ giữa các replica.
