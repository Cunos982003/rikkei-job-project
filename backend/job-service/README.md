# Job Service — DN-01

## Chạy local

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Profile `local` dùng H2 và chạy seed ở `db/dev`. Seed chỉ có trong local/test; không dùng các tài khoản mẫu ở môi trường khác.

| Tài khoản | Email | Mật khẩu |
|---|---|---|
| STAFF | `staff1@rikkei.vn` | `Password@123` |
| HR công ty A | `hr1@rikkei.vn` | `Password@123` |
| HR công ty B | `hr2@rikkei.vn` | `Password@123` |

Email mock và thông tin đăng nhập tạm được in ra log ứng dụng khi tạo tài khoản HR (chỉ dùng trong local/test). SMTP thật được bật bằng `EMAIL_ENABLED=true` và cấu hình các biến `MAIL_*`.

Swagger UI: http://localhost:8085/swagger-ui.html  
OpenAPI JSON: http://localhost:8085/v3/api-docs

## Hồ sơ công ty cho FE

- HR có thể sửa `name`, `industry`, `size`, `address`, `city`, `website`, `description` và tải logo lên; `taxCode` và `status` chỉ hiển thị, không cho sửa.
- `size` chọn một trong `1-50`, `51-200`, `201-1000`, `1000+`.
- Logo chỉ nhận JPG/PNG, tối đa 2 MB; FE nên kiểm tra và báo lỗi ngay khi chọn tệp.
- `logoUrl` là đường dẫn tương đối (ví dụ `/uploads/logos/company-...png`); FE ghép với base URL của API.
- Khi đăng nhập có `mustChangePassword=true`, FE bắt buộc mở màn đổi mật khẩu và chỉ dùng token mới trả về sau khi đổi mật khẩu thành công.

## API theo DN-01

- STAFF/ADMIN: `POST /staff/companies`, `POST /staff/companies/{id}/hr-accounts`, `GET /staff/companies/{id}`.
- HR: `GET /companies/me`, `PUT /companies/me`, `POST /companies/me/logo`.
- `PUT /companies/{id}` được giữ: STAFF/ADMIN cập nhật theo ID; HR chỉ cập nhật công ty của mình, công ty khác trả 404.
