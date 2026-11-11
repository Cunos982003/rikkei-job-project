# Yêu cầu tích hợp ngoài `ai_career_service`

Các endpoint nháp và vòng đời bản nháp đã được khai báo trong `ai_career_service`.
Trong repository hiện tại chưa có API CV để lấy dữ liệu hồ sơ hoặc ghi nội dung
được chấp nhận vào CV. Vì vậy, hai điểm tích hợp được biểu diễn bằng interface;
implementation mặc định trả HTTP 503 thay vì giả lập dữ liệu hoặc báo thành công.
Không có mã nào ngoài thư mục `ai_career_service` được thay đổi.

## Các điểm tích hợp cần hoàn thiện

### Đọc dữ liệu hồ sơ và kỹ năng JD

Triển khai `AIDraftDataSource.loadInputSnapshot(...)` trong
`com.example.ai_career_service.service`.

Implementation phải:

- Xác minh `userId` sở hữu `cvId`; không tin `cvId` từ URL nếu chưa kiểm tra quyền.
- Chỉ tổng hợp dữ liệu hồ sơ của ứng viên: kỹ năng đã xác thực, dự án, thực tập
  và học vấn. Không đưa dữ liệu tự khai vào trường kỹ năng đã xác thực.
- Khi có `projectId`, xác minh dự án thuộc CV/ứng viên đó.
- Khi có `jobId`, chỉ bổ sung tập kỹ năng/yêu cầu liên quan từ JD để định hướng
  nội dung. Không đưa nguyên văn JD hoặc dữ kiện tuyển dụng khác vào snapshot.
- Trả về `JsonNode` JSON gồm các dữ kiện trên. Các trường `verifiedSkills`,
  `projects`, `internships`, `education` và `jobSkills` (nếu có) là cấu trúc
  khuyến nghị; service hiện tại lưu nguyên snapshot này vào `input_snapshot`
  và dùng nó để kiểm chứng claim.
- Báo lỗi upstream/ủy quyền rõ ràng; không trả snapshot rỗng khi upstream lỗi.

Hiện `UnavailableAIDraftDataSource` trả 503 cho đến khi integration được cấu hình.

### Ghi nội dung đã chấp nhận vào CV

Triển khai `AIDraftCvWriter.writeDraft(AIDraft draft)` để cập nhật CV trong
profile/CV service.

Implementation phải xác minh quyền sở hữu CV lần nữa và ghi `output` vào đúng
`cvId`, `section` và `projectId`. Chỉ đổi trạng thái draft sang `ACCEPTED` sau
khi thao tác ghi thành công. Nên dùng `draft.id` làm idempotency key để tránh
ghi lặp khi request retry. Lỗi ghi phải được trả về cho caller; trạng thái draft
giữ nguyên `DRAFT`.

Hiện `UnavailableAIDraftCvWriter` trả 503; endpoint accept chưa thể cập nhật CV
thật khi thiếu implementation này.

## API được triển khai

- `POST /cvs/{id}/ai-write` với JSON `{ "section": "SUMMARY|PROJECT",
  "projectId": "UUID|null", "jobId": "UUID|null" }`: tạo draft trạng thái
  `DRAFT`, lưu snapshot đầu vào và warnings.
- `POST /ai-drafts/{id}/accept`: ghi draft vào CV qua writer, sau đó đặt trạng
  thái `ACCEPTED`.
- `POST /ai-drafts/{id}/discard`: đặt trạng thái `DISCARDED`.
- `GET /cvs/{id}/ai-drafts`: trả lịch sử theo thời gian tạo giảm dần.

Các route sử dụng UUID ứng viên từ authenticated principal. Tìm kiếm và thao
tác bản nháp đều được giới hạn theo `userId`; draft không thuộc người dùng trả
404. Chỉ draft đang `DRAFT` có thể được accept/discard; các trạng thái còn lại
trả 409.

## Chống bịa và giới hạn kiểm chứng

Prompt yêu cầu AI chỉ dùng JSON đầu vào và trả JSON có `content` cùng danh sách
`skills` bao phủ các tên kỹ năng/công nghệ được nhắc trong nội dung. Backend so
sánh từng claim trong `skills` với snapshot (không phân biệt hoa thường), thêm
warning cho tên không được tìm thấy; backend cũng quét các số liệu trong
`content` và cảnh báo số không xuất hiện trong snapshot.

Kiểm chứng tên kỹ năng/công nghệ phụ thuộc vào trường `skills` do model trả về;
đây không phải bộ phân tích ngôn ngữ có thể chứng minh mọi danh từ trong văn
bản. Trước khi phát hành production, nên có bộ từ điển/ontology kỹ năng được
quản lý hoặc yêu cầu model trả từng claim kèm trích dẫn nguồn, rồi xác minh
trích dẫn đó trong snapshot. Warnings được lưu và trả về, không tự động xóa
nội dung khỏi draft để người dùng có thể xem xét.
