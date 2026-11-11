# Tài liệu các service

Tài liệu này giải thích chức năng các lớp và hàm trong package
`com.example.ai_career_service.service`, tập trung vào luồng gợi ý công việc.

## Tổng quan luồng xử lý

1. `CareerService.getRecommendations` yêu cầu nguồn dữ liệu tải hồ sơ kỹ năng và
   danh sách công việc đủ điều kiện.
2. `JobMatchScorer.score` tính điểm phù hợp, kỹ năng còn thiếu và khoảng cách
   kỹ năng cho từng công việc.
3. Danh sách được sắp xếp theo điểm giảm dần và giới hạn theo tham số `limit`.
4. Với từng công việc, hệ thống kiểm tra kết quả đã lưu có còn mới và đầu vào
   có khớp hay không. Kết quả khớp được dùng lại.
5. Nếu cần tạo kết quả mới, tối đa năm công việc đầu tiên được yêu cầu AI giải
   thích. Công việc còn lại dùng lời giải thích tạo từ dữ liệu.
6. Kết quả được lưu trong repository, chuyển sang DTO response qua mapper, rồi
   trả về cho controller.

> Hiện tại `UnavailableRecommendationDataSource` chưa tích hợp các dịch vụ hồ
> sơ, việc làm và đánh giá. Vì vậy, khi dùng implementation này, yêu cầu tải dữ
> liệu sẽ trả HTTP 503.

## `CareerService`

Service điều phối việc tải dữ liệu, chấm điểm, dùng cache, tạo giải thích, lưu
kết quả và dựng response. Các dependency được Spring inject qua constructor.

### Các hằng số

- `AI_EXPLANATION_LIMIT = 5`: chỉ năm công việc có thứ hạng cao nhất được gửi
  đến AI để tạo giải thích.
- `CACHE_TTL = 24 giờ`: giới hạn tuổi của kết quả gợi ý được phép dùng lại.
- `AI_TIMEOUT_SECONDS = 10`: giới hạn thời gian chờ AI cho một lời giải thích.

### `getRecommendations(UUID userId, int limit)`

Hàm chính của luồng gợi ý.

1. Gọi `dataSource.loadSnapshot(userId)` để tải snapshot gồm kỹ năng đã xác
   thực và công việc đủ điều kiện.
2. Gọi `JobMatchScorer.score` cho từng công việc; sắp xếp điểm giảm dần rồi
   lấy tối đa `limit` phần tử.
3. Tạo `RecommendationPrompt` và hash đầu vào cho từng công việc.
4. Tìm bản ghi theo cặp người dùng/công việc. Chỉ dùng cache nếu bản ghi được
   tạo trong 24 giờ gần nhất và `inputHash` bằng hash mới.
5. Nếu không có cache hợp lệ:
   - Với tối đa năm kết quả đầu: chạy AI trên `aiExecutor`, chờ tối đa 10 giây.
     Nếu AI lỗi, trả nội dung rỗng hoặc chỉ có khoảng trắng, ghi cảnh báo log và
     dùng giải thích dự phòng.
   - Với các kết quả còn lại: tạo giải thích dự phòng ngay, không gọi AI.
6. Chờ các tác vụ giải thích, giới hạn nội dung tối đa 60 từ, cập nhật hoặc tạo
   `JobRecommendation`, lưu repository và chuyển thành response bằng
   `JobRecommendationMapper`.
7. Trả danh sách response theo thứ tự điểm đã xếp hạng.

Kết quả cache vẫn được map với `MatchResult` vừa tính để response chứa danh
sách kỹ năng thiếu và khoảng cách kỹ năng hiện tại.

### `inputHash(RecommendationSnapshot snapshot, EligibleJob job, boolean aiExplanation)`

Hàm private tạo dấu vân tay của các đầu vào ảnh hưởng đến kết quả. Chuỗi đầu
vào bao gồm phiên bản thuật toán, phiên bản hồ sơ, phiên bản công việc, việc
có bật giải thích AI hay không, danh sách kỹ năng đã xác thực và yêu cầu kỹ
năng của công việc. Danh sách kỹ năng được sắp xếp trước khi ghép thành chuỗi.
Hàm băm chuỗi đó bằng SHA-256 và trả về dạng hexadecimal.

Hash khác nhau làm cache cũ không được dùng lại khi hồ sơ, công việc, yêu cầu
kỹ năng hoặc chế độ giải thích thay đổi. Nếu JVM không hỗ trợ SHA-256, hàm ném
`IllegalStateException` thay vì trả hash giả.

### Các record nội bộ

- `ScoredJob`: gom một `EligibleJob` với `MatchResult` tương ứng để sắp xếp và
  dựng response mà không phải tính điểm lại.
- `PendingRecommendation`: giữ vị trí kết quả, công việc, kết quả chấm điểm,
  hash đầu vào, bản ghi cache cũ (nếu có), future giải thích và thời điểm tạo.
  Đây là dữ liệu trung gian giữa giai đoạn khởi chạy giải thích và giai đoạn
  lưu kết quả.

## `JobMatchScorer`

Lớp tiện ích không có trạng thái, thực hiện thuật toán tính mức độ phù hợp.

### `score(List<VerifiedSkill> verifiedSkills, List<SkillRequirement> requirements)`

Tính điểm, khoảng cách và kỹ năng bắt buộc chưa đạt:

- Chuyển kỹ năng đã xác thực thành bảng tra cứu tên kỹ năng → chỉ số.
- Với từng yêu cầu, tính `gap = max(0, requiredIndex - verifiedIndex)`. Nếu
  chưa có kỹ năng thì xem chỉ số hiện tại là `0`; trường `verifiedIndex` trong
  kết quả vẫn là `null`.
- Điểm mỗi yêu cầu là `0` nếu chưa có kỹ năng; là `100` nếu đã xác thực và
  mức yêu cầu bằng `0`; các trường hợp còn lại dùng
  `min(100, verifiedIndex * 100 / requiredIndex)`.
- Điểm tổng là trung bình cộng điểm của mọi yêu cầu, gồm cả `REQUIRED` và
  `PREFERRED`. Nếu không có yêu cầu thì điểm bằng `0`.
- `missingRequiredSkills` chỉ liệt kê yêu cầu `REQUIRED` chưa có kỹ năng hoặc
  có chỉ số thấp hơn mức yêu cầu.

Hàm trả về `MatchResult` chứa điểm tổng, danh sách kỹ năng bắt buộc còn thiếu
và danh sách `SkillGap` cho tất cả yêu cầu.

### `verifiedIndexes(List<VerifiedSkill> verifiedSkills)`

Hàm package-private tạo `Map` để tra cứu chỉ số kỹ năng theo tên đã chuẩn hóa.
Nếu hai kỹ năng có cùng tên sau khi chuẩn hóa, hàm ném `IllegalStateException`
để tránh âm thầm ghi đè dữ liệu trùng.

### `normalize(String name)`

Xóa khoảng trắng ở hai đầu và chuyển tên kỹ năng thành chữ thường theo
`Locale.ROOT`. Nhờ đó việc đối chiếu không phân biệt chữ hoa/thường và ổn định
theo locale của máy chạy.

### Các record kết quả

- `MatchResult`: chứa điểm phù hợp, tên các kỹ năng bắt buộc chưa đạt và các
  khoảng cách kỹ năng.
- `SkillGap`: chứa tên kỹ năng, chỉ số đã xác thực (có thể `null`), chỉ số yêu
  cầu, khoảng cách và loại yêu cầu.

## `RecommendationExplanation`

Lớp tiện ích tạo dữ liệu đầu vào cho AI và giải thích dự phòng.

### `createPrompt(RecommendationSnapshot snapshot, EligibleJob job)`

So sánh kỹ năng đã xác thực với từng yêu cầu của công việc bằng tên kỹ năng đã
chuẩn hóa. Kỹ năng có chỉ số hiện tại lớn hơn hoặc bằng yêu cầu được đưa vào
`matchedSkills`; các trường hợp còn lại vào `missingSkills`. Mỗi mục giữ tên,
chỉ số hiện tại, chỉ số yêu cầu và cờ cho biết yêu cầu có bắt buộc hay không.
Danh sách kết quả được sao chép bất biến trước khi đưa vào `RecommendationPrompt`.

### `fallback(RecommendationPrompt prompt)`

Tạo lời giải thích tiếng Việt từ tên các kỹ năng phù hợp và chưa đạt. Tên trùng
được loại bỏ. Khi cả hai danh sách đều rỗng, hàm trả lời rằng chưa có dữ liệu
kỹ năng đã xác thực. Nếu chỉ một phía rỗng, hàm hiển thị cụm “chưa có” hoặc
“không có” tương ứng.

### `limitWords(String explanation)`

Chuẩn hóa lời giải thích bằng cách xử lý `null` như chuỗi rỗng, bỏ khoảng trắng
đầu/cuối và gộp chuỗi khoảng trắng liên tiếp thành một dấu cách. Nếu nội dung
dài hơn 60 từ, hàm cắt còn 60 từ; nếu không thì giữ nguyên nội dung đã chuẩn hóa.

### `names(List<SkillComparison> skills)`

Hàm private lấy tên kỹ năng, loại bỏ tên trùng theo luồng xử lý của Java Stream
và ghép các tên còn lại bằng dấu phẩy. Danh sách rỗng cho kết quả chuỗi rỗng.

## `RecommendationDataSource`

Đây là ranh giới tích hợp với các dịch vụ hồ sơ, việc làm và đánh giá.

### `loadSnapshot(UUID userId)`

Hợp đồng yêu cầu tải dữ liệu của đúng người dùng:

- Chỉ lấy chỉ số kỹ năng đã xác thực; trọng số nguồn DG-03 phải được áp dụng
  trước khi trả dữ liệu và không đưa giá trị tự khai vào.
- Chỉ lấy công việc đã được duyệt, chưa hết hạn, kèm các ngưỡng kỹ năng bắt
  buộc và ưu tiên.
- Trả dữ liệu dưới dạng `RecommendationSnapshot`.

Interface không tự thực hiện truy vấn hoặc xử lý dữ liệu. Implementation chịu
trách nhiệm cung cấp snapshot đúng hợp đồng.

## `UnavailableRecommendationDataSource`

Implementation mặc định của `RecommendationDataSource` trong khi chưa cấu hình
tích hợp dữ liệu.

### `loadSnapshot(UUID userId)`

Hiện luôn ném `ResponseStatusException` với HTTP 503 và thông báo các tích hợp
dữ liệu chưa được cấu hình. Đây là hành vi chủ ý để báo rõ hệ thống chưa sẵn
sàng thay vì trả dữ liệu giả hoặc danh sách rỗng.

## `RecommendationSnapshot`

Record bất biến làm mô hình dữ liệu đầu vào cho quy trình gợi ý. Các record con
được khai báo bên trong snapshot.

### Constructor của `RecommendationSnapshot`

Kiểm tra `profileRevision` khác `null`, đồng thời tạo bản sao bất biến cho
`verifiedSkills` và `eligibleJobs`. `List.copyOf` cũng từ chối danh sách null
hoặc phần tử null.

### Constructor của `VerifiedSkill`

Kiểm tra tên kỹ năng không null/rỗng/trắng và chỉ số nằm trong khoảng 0–100.

### Constructor của `EligibleJob`

Kiểm tra `jobId` và `jobRevision` khác `null`, rồi tạo bản sao bất biến của
danh sách yêu cầu kỹ năng.

### Constructor của `SkillRequirement`

Kiểm tra tên kỹ năng không null/rỗng/trắng, chỉ số yêu cầu trong khoảng 0–100
và `type` khác `null`.

### `requireSkillName(String name)`

Hàm private dùng chung để từ chối tên kỹ năng null hoặc chỉ chứa khoảng trắng.

### `requireIndex(int index)`

Hàm private dùng chung để từ chối chỉ số ngoài khoảng 0–100.

### `RequirementType`

Enum phân biệt yêu cầu kỹ năng bắt buộc (`REQUIRED`) và ưu tiên (`PREFERRED`).

## Ghi chú về giới hạn hiện tại

- Tích hợp `RecommendationDataSource` chưa được cấu hình nên luồng API hiện
  không thể lấy dữ liệu thật qua implementation mặc định.
- Điểm được tính là trung bình đều theo từng yêu cầu kỹ năng; cả bắt buộc lẫn
  ưu tiên cùng tham gia vào điểm tổng. Loại bắt buộc chỉ quyết định danh sách
  `missingRequiredSkills`.
- Giải thích AI chỉ được dùng cho năm kết quả đứng đầu sau khi đã sắp xếp và
  giới hạn danh sách.
