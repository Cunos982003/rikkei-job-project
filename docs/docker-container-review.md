# Ghi chú phần cần thay đổi ngoài Dockerfile và Docker Compose

Phạm vi chỉnh sửa trong lượt rà soát này chỉ gồm các `Dockerfile` và
`infra/docker-compose.yml`. Các điểm dưới đây chưa được sửa vì cần thay đổi
source code hoặc tài liệu khác.

## Các điểm cần xử lý

1. **Các route gateway trỏ tới service chưa chạy được HTTP**
   - Gateway hiện khai báo route tới `assessment`, `audit`, `evidence` và
     `notification`; các module này chỉ có `spring-boot-starter`, chưa có web
     server và không được đưa vào Compose.
   - Cần triển khai HTTP server cho các module đó hoặc bỏ/tắt các route tương
     ứng trong cấu hình gateway trước khi sử dụng các API này.

## Cấu hình trong lượt này

- Mock API và service clients sử dụng port `9000`; WireMock dùng host port
  `9001`.
- Profile service lưu file upload vào named volume `profile-uploads`.
- Các thông tin PostgreSQL/JWT/LMS bắt buộc được lấy từ `.env`; Compose không
  còn dùng mật khẩu PostgreSQL mặc định.
- Job service nhận `APP_JWT_SECRET` từ biến dùng chung `JWT_SECRET`.
- Frontend gọi `/lms` qua Vite proxy trong môi trường phát triển và Nginx trong
  Docker; `LMS_SERVICE_KEY` chỉ được gắn ở phía proxy, không nằm trong bundle.
- Các lệnh Compose trong README nạp `.env` gốc bằng `--env-file .env`.
