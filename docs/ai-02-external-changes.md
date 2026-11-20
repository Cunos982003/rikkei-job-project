# AI-02: Phụ thuộc & Hướng dẫn Cấu hình Hệ thống Ngoài

Tài liệu này ghi nhận các phụ thuộc nghiệp vụ và lưu ý tích hợp đối với các microservices nằm ngoài phạm vi chỉnh sửa trực tiếp (`ai_career_service`, `api-gateway`, `frontend`).

---

## 1. Phụ thuộc DN-02 (Job Service)
- **Cổng dịch vụ**: 8085 (mặc định qua `JOB_SERVICE_URL`).
- **Endpoint tiêu thụ**:
  ```http
  GET /jobs/{id}
  ```
  Trả về thông tin chi tiết của tin tuyển dụng (`JobResponse`), bao gồm `title`, `description`, `requirements`, và danh sách kỹ năng `jobSkills` / `requiredSkills`.
- **Trạng thái**: Đã sẵn sàng trên Job Service hiện tại thông qua `JobQueryController`, không yêu cầu chỉnh sửa database hay code của Job Service.

---

## 2. Phụ thuộc AI-01 (Nền tảng AI Career Service)
- **Triển khai**: `InterviewPracticeService` được tích hợp trực tiếp bên trong `ai_career_service` (port 8090).
- **Cơ chế AI Provider**:
  - Tận dụng `AIProvider.chat(systemPrompt, userPrompt)` mới mở rộng.
  - Hỗ trợ các provider cấu hình qua biến môi trường:
    - `AI_PROVIDER=mock`: Trả về dữ liệu mock 5 câu hỏi và chấm điểm nhanh, không tốn token (mặc định cho test/dev).
    - `AI_PROVIDER=gemini`: Yêu cầu `AI_GEMINI_API_KEY`.
    - `AI_PROVIDER=openai`: Yêu cầu `AI_OPENAI_API_KEY`.
    - `AI_PROVIDER=claude`: Yêu cầu `AI_CLAUDE_API_KEY`.

---

## 3. Database Migration Script (PostgreSQL - ai_db)
Nếu không sử dụng Hibernate `ddl-auto: update`, quản trị viên DB có thể chạy script DDL sau để tạo bảng:

```sql
-- Bảng lưu các phiên luyện phỏng vấn
CREATE TABLE IF NOT EXISTS interview_practices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    job_id UUID NOT NULL,
    job_title VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    overall_score DOUBLE PRECISION,
    summary TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_interview_practices_user_id ON interview_practices(user_id, created_at DESC);

-- Bảng lưu từng câu hỏi và câu trả lời trong phiên
CREATE TABLE IF NOT EXISTS practice_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    practice_id UUID NOT NULL REFERENCES interview_practices(id) ON DELETE CASCADE,
    q_order INT NOT NULL,
    question TEXT NOT NULL,
    type VARCHAR(20) NOT NULL,
    answer TEXT,
    score INT,
    feedback TEXT,
    suggestion TEXT
);

CREATE INDEX IF NOT EXISTS idx_practice_questions_practice_id ON practice_questions(practice_id, q_order ASC);
```

---

## 4. API Gateway Route
Đã được cấu hình trong `backend/api-gateway/src/main/resources/application.yaml`:
```yaml
- id: ai-interview-practice
  uri: ${GATEWAY_AI_URL:http://localhost:8090}
  predicates:
    - Path=/me/interview-practices,/me/interview-practices/**
  filters: *cors-response-filters
```
