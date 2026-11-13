# Hợp Đồng API Mock Hệ Thống Ngoài – Rikkei Jobs

Tài liệu quy định 11 endpoint chuẩn giữa Backend Rikkei Jobs và Mock Server hệ thống ngoài (LMS, Khảo thí, LMS-PM, Rikkei Meet).

- **Cổng mặc định**: `9000` (`http://localhost:9000`, đọc từ biến môi trường `PORT`).
- **Khóa dịch vụ**: `dev-key` (đọc từ biến môi trường `SERVICE_KEY`).
- **Quy tắc bắt buộc**:
  - Mọi request vào `/lms`, `/exam`, `/pm`, `/meet` bắt buộc phải có header `X-Service-Key: dev-key`. Thiếu hoặc sai khóa $\rightarrow$ trả về HTTP `401 Unauthorized` `{"code": "INVALID_SERVICE_KEY", "message": "Thiếu hoặc sai X-Service-Key"}`.
  - Các API trả danh sách (kết quả môn, điểm thi, thực tập, ghi danh) trả `[]` khi không có dữ liệu, không trả `404`.
  - Body gửi lên và trả về đều là JSON theo quy ước `camelCase`.

---

## Danh Sách 11 Endpoint Nghiệp Vụ

| # | Phương thức & Đường dẫn | Body gửi lên | Trả về khi đúng | Mã lỗi & Lý do | Task sử dụng |
|:---:|---|---|---|---|---|
| **1** | `POST /lms/sso/tickets` | `{"lmsUserId": "..."}` | `201` `{"ticket": "...", "issuedAt": "..."}` | `404` Người dùng không tồn tại | NT-01 (Giả lập bấm nút từ LMS) |
| **2** | `POST /lms/sso/verify` | `{"ticket": "..."}` | `200` `{"lmsUserId": "...", "email": "...", "fullName": "...", "role": "...", "issuedAt": "..."}` | `404` Vé không tồn tại | NT-01 |
| **3** | `GET /lms/students/{lmsUserId}` | Không | `200` `{"lmsUserId": "...", "fullName": "...", "email": "...", "program": "...", "classCode": "...", "courseName": "..."}` | `404` Không có hoặc không phải CANDIDATE | HS-02 |
| **4** | `GET /lms/students/{lmsUserId}/results` | Không | `200` Mảng kết quả môn học (`lmsResults`) | Trả `[]` nếu chưa có điểm | NT-05, HS-01 |
| **5** | `GET /lms/courses?skill=JAVA` | Không | `200` Mảng khóa học (lọc theo `skill` nếu có) | Trả `[]` nếu không khớp | DT-02 |
| **6** | `POST /lms/enrollments` | `{"lmsUserId": "...", "courseId": "..."}` | `201` `{"lmsUserId": "...", "courseId": "...", "progress": 0, "score": null, "status": "ENROLLED", "enrolledAt": "..."}` | `404` User hoặc khóa học không có<br>`409` Đã ghi danh (`ALREADY_ENROLLED`) | DT-02 |
| **7** | `GET /lms/enrollments/{lmsUserId}` | Không | `200` Mảng ghi danh (`status`: `ENROLLED`, `IN_PROGRESS`, `COMPLETED`) | Trả `[]` nếu chưa có | DT-02, DT-03 |
| **8** | `GET /exam/students/{lmsUserId}/results` | Không | `200` Mảng điểm thi (`examResults`) | Trả `[]` nếu chưa có điểm thi | NT-05, HS-01 |
| **9** | `GET /pm/students/{lmsUserId}/internships` | Không | `200` Mảng đợt thực tập IOC (`internships`) | Trả `[]` nếu chưa có đợt nào | NT-05, TT-01 |
| **10** | `POST /pm/batches/{batchId}/members` | `{"lmsUserId": "..."}` | `201` `{"batchId": "...", "lmsUserId": "...", "addedAt": "..."}` | `404` Đợt hoặc user không có<br>`409` Đã là thành viên (`ALREADY_MEMBER`) | TT-02 |
| **11** | `POST /meet/rooms` | `{"title": "...", "startAt": "...", "endAt": "..."}` | `201` `{"roomId": "...", "roomUrl": "...", "title": "...", "startAt": "...", "endAt": "..."}` | `400` Thiếu trường bắt buộc | UT-06 |

---

## Danh Sách 4 Endpoint Công Cụ Kiểm Thử (`/mock/*`)

Dành riêng cho Mentor / Tester kiểm thử hành vi hệ thống, không yêu cầu `X-Service-Key` và Backend không bao giờ gọi tới:

| Endpoint | Phương thức | Chức năng | Ca kiểm thử sử dụng |
|---|:---:|---|---|
| `/mock/reload` | `POST` | Nạp lại file `data.json` mà không cần khởi động lại tiến trình | NT-05, TT-01: Thay đổi điểm trong mock |
| `/mock/requests` | `GET` | Trả về 100 request gần nhất (thời điểm, method, path, hasKey, body) | TT-02: Kiểm tra backend gửi đúng `lmsUserId`<br>NT-05: Kiểm tra backend có gửi khóa |
| `/mock/enrollments` | `PATCH` | Cập nhật `progress`, `score`, `status` của lượt ghi danh | DT-02, DT-03: Khóa học chuyển trạng thái hoàn thành |
| `/mock/reset` | `POST` | Xóa vé tạm, ghi danh, thành viên đợt và nhật ký request trong bộ nhớ | Reset môi trường trước mỗi lần demo |

