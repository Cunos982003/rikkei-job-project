# Phân Tích Sự Cố: Lỗi 401 Invalid API Key từ AI Provider

## 1. Hiện tượng & Log ghi nhận

Từ log của container `ai-career-service-1`:
```log
ai-career-service-1 | WARN c.e.a.service.InterviewPracticeService : AI response parsing failed on first attempt: AI provider returned HTTP 401: {
ai-career-service-1 | "error": {
ai-career-service-1 | "message": "Incorrect API key provided: CHANGE_M***************************IDER. You can find your API key at https://platform.openai.com/account/api-keys.",
ai-career-service-1 | "type": "invalid_request_error",
ai-career-service-1 | "code": "invalid_api_key"
ai-career-service-1 | }
ai-career-service-1 | }
ai-career-service-1 | ERROR c.e.a.service.InterviewPracticeService : AI response failed on retry. Raw response: null. Using fallback response.
```

---

## 2. Giải thích nguyên nhân & Các giả định

### Giả định về cấu hình trong file `.env`
1. **Biến `AI_PROVIDER`**: Đang được cấu hình là `openai` (hoặc hệ thống đang trỏ vào OpenAI Provider).
2. **Biến `AI_OPENAI_API_KEY`**: Đang chứa giá trị mẫu (placeholder) mặc định từ template, dạng:
   ```env
   AI_OPENAI_API_KEY=CHANGE_ME_OR_SET_AI_PROVIDER
   ```
   (hoặc `CHANGE_ME_...`).
3. **Kết quả xác thực từ OpenAI**:
   - Khi dịch vụ `ai-career-service` gọi API `https://api.openai.com/v1/chat/completions`, OpenAI kiểm tra Authorization header và nhận thấy chuỗi key này không hợp lệ.
   - OpenAI từ chối request với mã lỗi **HTTP 401 Unauthorized** kèm thông báo `"Incorrect API key provided: CHANGE_M..."`.

### Điểm tích cực từ cơ chế Fallback
- Nhờ cơ chế **Fallback** vừa được cập nhật (`Using fallback response`), hệ thống **không bị sập hay ném ra lỗi 500** như trước. Thay vào đó, nó ghi nhận log cảnh báo và tự động cung cấp dữ liệu dự phòng chuẩn để ứng viên vẫn có thể tiếp tục luồng luyện tập phỏng vấn.

---

## 3. Hướng dẫn cấu hình lại file `.env`

Tùy theo nhu cầu sử dụng thực tế, bạn có thể chọn 1 trong 3 phương án cấu hình dưới đây trong file `.env` trên VPS:

### Phương án 1: Dùng chế độ Mock (Khuyên dùng khi chưa có API Key trả phí)
> **Ưu điểm**: Hoàn toàn miễn phí, tốc độ phản hồi tức thì (< 100ms), không phụ thuộc vào internet hay quota, câu hỏi và chấm điểm luôn sẵn sàng và ổn định 100%.

Trong file `.env`:
```env
AI_PROVIDER=mock
```

---

### Phương án 2: Dùng OpenAI thật (Yêu cầu có tài khoản OpenAI nạp tiền)
> Cần đăng ký và lấy API Key tại [platform.openai.com/account/api-keys](https://platform.openai.com/account/api-keys).

Trong file `.env`:
```env
AI_PROVIDER=openai
AI_OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
AI_OPENAI_MODEL=gpt-4o-mini
AI_OPENAI_ENDPOINT=https://api.openai.com/v1/chat/completions
```

---

### Phương án 3: Dùng Google Gemini (Có gói Free Tier, không tốn phí)
> Lấy API Key miễn phí từ Google AI Studio tại [aistudio.google.com](https://aistudio.google.com).

Trong file `.env`:
```env
AI_PROVIDER=gemini
AI_GEMINI_API_KEY=AIzaSyxxxxxxxxxxxxxxxxxxxxxxxxxxxx
AI_GEMINI_ENDPOINT=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent
```

---

## 4. Áp dụng cấu hình sau khi sửa `.env`

Sau khi lưu file `.env` trên VPS:
```bash
# Khởi động lại container ai-career-service để nhận cấu hình mới:
docker compose up -d ai-career-service

# Theo dõi log kiểm tra:
docker compose logs -f ai-career-service
```
