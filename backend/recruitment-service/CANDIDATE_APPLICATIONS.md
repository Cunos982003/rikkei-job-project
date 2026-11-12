# Chức năng ứng tuyển của ứng viên

Dịch vụ cung cấp các API ứng tuyển sau:

- `POST /jobs/{id}/recruit` với dữ liệu `{ "cvId": "...", "coverLetter": "..." }`
- `GET /me/recruitments?status=SUBMITTED`
- `GET /me/recruitments/{id}` (bao gồm lịch sử trạng thái)
- `POST /me/recruitments/{id}/withdraw` với dữ liệu `{ "reason": "..." }`

Các API dành cho ứng viên yêu cầu bearer JWT có claim `userId` (UUID hoặc `subject` là UUID) và `role`. HR muốn xem CV cũng cần claim `companyId`. Cấu hình `JWT_SECRET` trùng với khóa ký mà auth service sử dụng; giá trị mặc định chỉ dành cho môi trường phát triển cục bộ.

## Hợp đồng tích hợp với các dịch vụ khác

Dịch vụ này không quản lý dữ liệu việc làm và CV. Các job service/profile service hiện có trong repository chưa cung cấp những API dưới đây. Cần cấu hình và triển khai các API này trước khi bật chức năng ứng tuyển trong môi trường tích hợp:

- `JOB_SERVICE_URL` + `JOB_DETAILS_PATH` (mặc định `/jobs/{jobId}`): API `GET` có xác thực, trả về JSON gồm `status` (phải là `APPROVED` mới cho phép ứng tuyển), `expiresAt` (thời điểm ISO-8601 có múi giờ; `null` nghĩa là không có hạn), `companyId` (UUID) và `hrEmails` (danh sách địa chỉ email HR).
- `PROFILE_SERVICE_URL` + `CV_PDF_PATH` (mặc định `/api/v1/cvs/{cvId}/pdf`): API `GET` có xác thực, trả về CV thuộc ứng viên dưới dạng `recruitment/pdf`. Profile service phải xác minh quyền sở hữu CV bằng bearer token được chuyển tiếp.

Dịch vụ ứng tuyển sao chép PDF vào `CV_SNAPSHOT_DIRECTORY` và lưu URL được bảo vệ `/recruitments/{id}/cv-snapshot` trong trường `cv_snapshot_url`. API snapshot cho phép chính ứng viên hoặc HR có `companyId` trong JWT trùng với công ty đăng tuyển tải đúng bản CV đã nộp. Cấu hình API gateway để chuyển tiếp endpoint này khi cần; đặt `PUBLIC_BASE_URL` nếu URL lưu trong dữ liệu cần là địa chỉ công khai đầy đủ.

## Vận hành và các giới hạn

- Dịch vụ gửi email thông báo cho HR sau khi giao dịch tạo đơn ứng tuyển được commit. Cấu hình `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH` và `SMTP_STARTTLS`. Nếu chưa cấu hình SMTP, không có địa chỉ HR hoặc gửi email thất bại, đơn ứng tuyển vẫn được lưu và lỗi được ghi log. Nếu cần bảo đảm gửi email, hãy bổ sung giám sát/cơ chế thử lại hoặc transactional outbox.
- CV phải là PDF và không vượt quá `CV_MAX_PDF_BYTES` (mặc định 10 MiB).
- Ứng viên chỉ được có một đơn chưa rút cho mỗi công việc. Nộp trùng khi đơn trước vẫn đang hoạt động trả về `409 Conflict`. Sau khi rút đơn, ứng viên có thể nộp lại; lần nộp lại tạo một đơn mới và giữ nguyên đơn cùng lịch sử cũ.
- Không thể rút đơn ở trạng thái `OFFER`, `WITHDRAWN`, `REJECTED` hoặc `HIRED`.
- Mỗi lần trạng thái thay đổi, dịch vụ ghi lại trạng thái trước/sau, người thực hiện và ghi chú trong `recruitment_status_history`. Bản ghi khởi tạo dùng `SUBMITTED` làm cả trạng thái trước và sau do entity hiện tại không có trạng thái `DRAFT`.
- Schema mới không đặt ràng buộc duy nhất trên `(job_id, user_id)` để cho phép nộp lại sau khi rút. Nếu cơ sở dữ liệu đã tạo ràng buộc này từ phiên bản trước, cần gỡ ràng buộc đó khi triển khai phiên bản mới.
- Đảm bảo migration hoặc cơ chế tạo schema của môi trường triển khai bao gồm cả hai entity recruitment hiện có; đồng thời cần cấp phát nơi lưu snapshot CV riêng tư và có tính bền vững. Thư mục cục bộ mặc định không được chia sẻ giữa các replica.
