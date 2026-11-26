-- ==============================================================================
-- V2__update_notification_schema.sql: Mở rộng trường thông báo, dọn dẹp constraint legacy & seed data
-- ==============================================================================

-- 1. Bổ sung các cột theo JPA Entity mới (Notification.java) nếu chưa tồn tại
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS user_id VARCHAR(100);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS recipient_role VARCHAR(50);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS action_url VARCHAR(500);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS metadata TEXT;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS type VARCHAR(50);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS title VARCHAR(255);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS body TEXT;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT now();

-- 2. Xử lý đồng bộ dữ liệu và loại bỏ ràng buộc NOT NULL của các cột legacy (customer_id, v.v.)
DO $$
DECLARE
    rec RECORD;
BEGIN
    -- 2.1. Nếu có cột customer_id từ schema cũ:
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'notifications' AND column_name = 'customer_id'
    ) THEN
        -- Copy dữ liệu từ customer_id sang user_id nếu user_id đang null
        EXECUTE 'UPDATE notifications SET user_id = customer_id::text WHERE user_id IS NULL';
        -- Gỡ bỏ ràng buộc NOT NULL của customer_id
        ALTER TABLE notifications ALTER COLUMN customer_id DROP NOT NULL;
    END IF;

    -- 2.2. Đảm bảo kiểu dữ liệu user_id là VARCHAR(100)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' AND table_name = 'notifications' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE notifications ALTER COLUMN user_id TYPE VARCHAR(100);
    END IF;

    -- 2.3. Tự động DROP NOT NULL cho TẤT CẢ các cột legacy không thuộc entity Notification hiện tại
    -- Chỉ giữ NOT NULL cho các trường bắt buộc của schema mới: 'id', 'user_id', 'type', 'title', 'body', 'created_at'
    FOR rec IN 
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND table_name = 'notifications' 
          AND is_nullable = 'NO'
          AND column_name NOT IN ('id', 'user_id', 'type', 'title', 'body', 'created_at')
    LOOP
        EXECUTE format('ALTER TABLE notifications ALTER COLUMN %I DROP NOT NULL', rec.column_name);
    END LOOP;
END $$;

-- 3. Tạo các chỉ mục tối ưu truy vấn theo user_id và created_at
CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications(user_id, read_at) WHERE read_at IS NULL;

-- 4. Bổ sung dữ liệu mẫu cho các ứng viên kiểm thử (cand-001, std-001, ed854c07-f3eb-48f1-a186-4e7135f54645)
INSERT INTO notifications (id, user_id, recipient_role, type, title, body, action_url, read_at, created_at)
VALUES
    ('c0000000-0000-0000-0000-000000000011', 'cand-001', 'CANDIDATE', 'OFFER_RECEIVED', 'Thư mời nhận việc mới từ Công ty FPT Software', 'Chúc mừng bạn đã nhận được Thư mời nhận việc cho vị trí Fullstack Java Developer với mức lương 18.000.000 VNĐ.', '/candidate/offers', NULL, now() - interval '15 minutes'),
    ('c0000000-0000-0000-0000-000000000012', 'cand-001', 'CANDIDATE', 'INTERVIEW_INVITATION', 'Lời mời phỏng vấn: Java Backend Engineer', 'Tập đoàn Rikkei Digital trân trọng mời bạn tham gia vòng phỏng vấn kỹ thuật vào lúc 09:30 ngày mai.', '/candidate/interviews', NULL, now() - interval '2 hours'),
    ('c0000000-0000-0000-0000-000000000013', 'cand-001', 'CANDIDATE', 'PROFILE_REVIEWED', 'Hồ sơ năng lực LMS của bạn đã được xác thực', 'Mentor Phạm Minh Đức đã đánh giá và xác nhận năng lực Java Spring Boot của bạn đạt 88/100 điểm.', '/profile/evaluations', now() - interval '1 day', now() - interval '1 day'),
    ('c0000000-0000-0000-0000-000000000014', 'cand-001', 'CANDIDATE', 'APPLICATION_STATUS_CHANGED', 'Hồ sơ ứng tuyển đã được duyệt qua vòng sơ loại', 'Đơn ứng tuyển vị trí React Frontend Intern tại CMC Global đã được chuyển sang trạng thái: Đã duyệt.', '/candidate/recruitments', now() - interval '3 days', now() - interval '3 days'),
    ('c0000000-0000-0000-0000-000000000015', 'ed854c07-f3eb-48f1-a186-4e7135f54645', 'CANDIDATE', 'OFFER_RECEIVED', 'Thư mời nhận việc mới từ Công ty FPT Software', 'Chúc mừng bạn đã nhận được Thư mời nhận việc cho vị trí Fullstack Java Developer với mức lương 18.000.000 VNĐ.', '/candidate/offers', NULL, now() - interval '15 minutes'),
    ('c0000000-0000-0000-0000-000000000016', 'ed854c07-f3eb-48f1-a186-4e7135f54645', 'CANDIDATE', 'INTERVIEW_INVITATION', 'Lời mời phỏng vấn: Java Backend Engineer', 'Tập đoàn Rikkei Digital trân trọng mời bạn tham gia vòng phỏng vấn kỹ thuật vào lúc 09:30 ngày mai.', '/candidate/interviews', NULL, now() - interval '2 hours'),
    ('c0000000-0000-0000-0000-000000000017', 'std-001', 'CANDIDATE', 'OFFER_RECEIVED', 'Thư mời nhận việc mới từ Công ty FPT Software', 'Chúc mừng bạn đã nhận được Thư mời nhận việc cho vị trí Fullstack Java Developer với mức lương 18.000.000 VNĐ.', '/candidate/offers', NULL, now() - interval '15 minutes')
ON CONFLICT (id) DO NOTHING;

