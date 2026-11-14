-- =============================================================================
-- AI DB: AI Career Service Data
-- =============================================================================
-- Run against ai_db

CREATE TABLE IF NOT EXISTS ai_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    session_id UUID NOT NULL,
    title VARCHAR(500),
    model VARCHAR(100),
    provider VARCHAR(50),
    total_tokens INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ai_conversations_user ON ai_conversations(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_conversations_session ON ai_conversations(session_id);
CREATE INDEX IF NOT EXISTS idx_ai_conversations_status ON ai_conversations(status);

CREATE TABLE IF NOT EXISTS ai_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    content TEXT NOT NULL,
    tokens_used INT DEFAULT 0,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ai_messages_conversation ON ai_messages(conversation_id, created_at);

CREATE TABLE IF NOT EXISTS ai_skill_recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    skill_code VARCHAR(100) NOT NULL,
    skill_name VARCHAR(255) NOT NULL,
    reason TEXT,
    confidence DECIMAL(3,2) CHECK (confidence >= 0 AND confidence <= 1),
    source VARCHAR(50) CHECK (source IN ('JOB_MARKET', 'PROFILE_GAP', 'CAREER_PATH', 'TRENDING')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'DISMISSED', 'IN_PROGRESS')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ai_skill_recommendations_user ON ai_skill_recommendations(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_skill_recommendations_status ON ai_skill_recommendations(status);

CREATE TABLE IF NOT EXISTS ai_career_paths (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    target_role VARCHAR(255) NOT NULL,
    "current_role" VARCHAR(255),
    recommended_skills JSONB,
    learning_resources JSONB,
    estimated_months INT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'ABANDONED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ai_career_paths_user ON ai_career_paths(user_id);

CREATE TABLE IF NOT EXISTS ai_usage_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    conversation_id UUID,
    model VARCHAR(100),
    provider VARCHAR(50),
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    cost_usd DECIMAL(10,6),
    request_type VARCHAR(50),
    success BOOLEAN NOT NULL DEFAULT true,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ai_usage_logs_user ON ai_usage_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_usage_logs_created ON ai_usage_logs(created_at);

-- ==============================================================================
-- SEED DATA (using valid UUIDs with only a-f, 0-9)
-- ==============================================================================

-- Sample conversations
INSERT INTO ai_conversations (id, user_id, session_id, title, model, provider, total_tokens, status, created_at) VALUES
('c1000000-0000-0000-0000-000000000001', '550e8400-e29b-41d4-a716-446655440000', 'd1000000-0000-0000-0000-000000000001', 'Java Career Advice', 'gpt-4o-mini', 'openai', 1250, 'ACTIVE', now() - interval '5 days'),
('c1000000-0000-0000-0000-000000000002', '550e8400-e29b-41d4-a716-446655440000', 'd1000000-0000-0000-0000-000000000002', 'React Learning Path', 'claude-3-5-haiku', 'anthropic', 2100, 'ACTIVE', now() - interval '3 days'),
('c1000000-0000-0000-0000-000000000003', '660e8400-e29b-41d4-a716-446655440000', 'd1000000-0000-0000-0000-000000000003', 'Frontend Interview Prep', 'gpt-4o-mini', 'openai', 890, 'ACTIVE', now() - interval '2 days'),
('c1000000-0000-0000-0000-000000000004', '770e8400-e29b-41d4-a716-446655440000', 'd1000000-0000-0000-0000-000000000004', 'Fullstack Architecture', 'gemini-2.0-flash', 'google', 3200, 'ACTIVE', now() - interval '1 day'),
('c1000000-0000-0000-0000-000000000005', '880e8400-e29b-41d4-a716-446655440000', 'd1000000-0000-0000-0000-000000000005', 'DevOps Career Transition', 'gpt-4o-mini', 'openai', 1560, 'ACTIVE', now() - interval '12 hours')
ON CONFLICT (id) DO NOTHING;

-- Sample messages
INSERT INTO ai_messages (id, conversation_id, role, content, tokens_used, created_at) VALUES
(gen_random_uuid(), 'c1000000-0000-0000-0000-000000000001', 'USER', 'Tôi muốn phát triển sự nghiệp Java Backend, nên học gì tiếp theo?', 120, now() - interval '5 days'),
(gen_random_uuid(), 'c1000000-0000-0000-0000-000000000001', 'ASSISTANT', 'Dựa trên hồ sơ của bạn (Java Core: 85, Spring Boot: 78), tôi khuyên bạn nên tập trung vào: 1) Spring Cloud/Microservices, 2) Kafka/Event-driven, 3) Kubernetes, 4) System Design. Bạn có kinh nghiệm IOC tốt, nên chuyển sang Microservices là tự nhiên.', 450, now() - interval '5 days' + interval '1 minute'),
(gen_random_uuid(), 'c1000000-0000-0000-0000-000000000001', 'USER', 'Cảm ơn! Tôi sẽ học Spring Cloud trước.', 40, now() - interval '5 days' + interval '2 minutes'),
(gen_random_uuid(), 'c1000000-0000-0000-0000-000000000002', 'USER', 'Roadmap học React từ cơ bản đến nâng cao như thế nào?', 110, now() - interval '3 days'),
(gen_random_uuid(), 'c1000000-0000-0000-0000-000000000002', 'ASSISTANT', 'Roadmap React 6 tháng:\nTháng 1-2: JavaScript ES6+, TypeScript cơ bản\nTháng 3: React fundamentals (Components, Hooks, State)\nTháng 4: State management (Redux Toolkit/Zustand), React Query\nTháng 5: Testing (Jest, RTL), Performance optimization\nTháng 6: Next.js, TypeScript advanced, Deployment\nBạn đã có HTML/CSS: 85, JS: 80 - rất tốt để bắt đầu!', 680, now() - interval '3 days' + interval '1 minute'),
(gen_random_uuid(), 'c1000000-0000-0000-0000-000000000003', 'USER', 'Các câu hỏi phỏng vấn Frontend thường gặp?', 95, now() - interval '2 days'),
(gen_random_uuid(), 'c1000000-0000-0000-0000-000000000003', 'ASSISTANT', 'Top 10 câu hỏi Frontend:\n1. Virtual DOM vs Real DOM\n2. React Hooks (useEffect cleanup)\n3. Closure trong JavaScript\n4. Event Loop & Microtasks\n5. CSS Box Model & Flexbox/Grid\n6. REST vs GraphQL\n7. State management patterns\n8. Performance optimization (lazy loading, memo)\n9. TypeScript utility types\n10. Testing strategies\nBạn đã sẵn sàng cho React (78) và TypeScript (70)!', 520, now() - interval '2 days' + interval '1 minute'),
(gen_random_uuid(), 'c1000000-0000-0000-0000-000000000004', 'USER', 'Kiến trúc Microservices cho ứng dụng Fullstack?', 105, now() - interval '1 day'),
(gen_random_uuid(), 'c1000000-0000-0000-0000-000000000004', 'ASSISTANT', 'Kiến trúc đề xuất:\n- API Gateway (Spring Cloud Gateway)\n- Auth Service (JWT, OAuth2)\n- User Service\n- Job Service\n- Profile Service\n- Notification Service (Kafka)\n- Frontend: Next.js (BFF pattern)\n- Database per service\n- Event-driven với Kafka\n- Monitoring: Prometheus + Grafana\nBạn có Kafka (82) và Docker (85) - foundation tốt!', 890, now() - interval '1 day' + interval '1 minute'),
(gen_random_uuid(), 'c1000000-0000-0000-0000-000000000005', 'USER', 'Chuyển từ Java sang DevOps đường nào?', 98, now() - interval '12 hours'),
(gen_random_uuid(), 'c1000000-0000-0000-0000-000000000005', 'ASSISTANT', 'DevOps Transition Path (6 tháng):\nTháng 1: Linux, Shell scripting, Networking basics\nTháng 2: Docker nâng cao, Docker Compose\nTháng 3: Kubernetes (CKA level), Helm\nTháng 4: CI/CD (GitLab CI, GitHub Actions, ArgoCD)\nTháng 5: IaC (Terraform), Cloud (AWS/GCP)\nTháng 6: Monitoring (Prometheus, Grafana, ELK), Security\nLợi thế: Bạn đã có Java (hiểu app), Docker (60), Kafka (82) - chỉ cần bổ sung K8s + Cloud!', 760, now() - interval '12 hours' + interval '1 minute')
ON CONFLICT (id) DO NOTHING;

-- Skill recommendations
INSERT INTO ai_skill_recommendations (id, user_id, skill_code, skill_name, reason, confidence, source, status, created_at) VALUES
('e1000000-0000-0000-0000-000000000001', '550e8400-e29b-41d4-a716-446655440000', 'SPRING_CLOUD', 'Spring Cloud', 'Bổ sung Microservices cho Spring Boot (78)', 0.92, 'PROFILE_GAP', 'PENDING', now() - interval '4 days'),
('e1000000-0000-0000-0000-000000000002', '550e8400-e29b-41d4-a716-446655440000', 'KUBERNETES', 'Kubernetes', 'Triển khai Microservices, DevOps trend', 0.88, 'JOB_MARKET', 'PENDING', now() - interval '3 days'),
('e1000000-0000-0000-0000-000000000003', '660e8400-e29b-41d4-a716-446655440000', 'NEXTJS', 'Next.js', 'Fullstack React trend, SSR/SSG', 0.85, 'TRENDING', 'ACCEPTED', now() - interval '2 days'),
('e1000000-0000-0000-0000-000000000004', '770e8400-e29b-41d4-a716-446655440000', 'TERRAFORM', 'Terraform', 'IaC cho DevOps/Cloud engineer', 0.90, 'CAREER_PATH', 'IN_PROGRESS', now() - interval '1 day'),
('e1000000-0000-0000-0000-000000000005', '880e8400-e29b-41d4-a716-446655440000', 'HELM', 'Helm', 'Package manager cho K8s', 0.87, 'JOB_MARKET', 'PENDING', now() - interval '12 hours'),
('e1000000-0000-0000-0000-000000000006', '990e8400-e29b-41d4-a716-446655440000', 'PLAYWRIGHT', 'Playwright', 'E2E testing cho Frontend', 0.82, 'PROFILE_GAP', 'PENDING', now() - interval '6 hours'),
('e1000000-0000-0000-0000-000000000007', '550e8400-e29b-41d4-a716-446655440000', 'GRAPHQL', 'GraphQL', 'Thay thế REST cho flexible API', 0.78, 'TRENDING', 'DISMISSED', now() - interval '5 days')
ON CONFLICT (id) DO NOTHING;

-- Career paths
INSERT INTO ai_career_paths (id, user_id, target_role, "current_role", recommended_skills, learning_resources, estimated_months, status, created_at) VALUES
('f1000000-0000-0000-0000-000000000001', '550e8400-e29b-41d4-a716-446655440000', 'Senior Java Backend Engineer', 'Junior Java Developer', '["Spring Cloud", "Kubernetes", "System Design", "Kafka", "Observability"]', '{"courses": ["Spring Cloud Netflix", "Kubernetes CKA", "System Design Primer"], "books": ["Designing Data-Intensive Applications", "Kubernetes in Action"]}', 12, 'ACTIVE', now() - interval '10 days'),
('f1000000-0000-0000-0000-000000000002', '660e8400-e29b-41d4-a716-446655440000', 'Senior Frontend Engineer', 'Frontend Developer', '["Next.js", "TypeScript Advanced", "Testing", "Performance", "Architecture"]', '{"courses": ["Next.js Complete", "TypeScript Deep Dive", "Testing React"], "books": ["React Patterns", "High Performance React"]}', 8, 'ACTIVE', now() - interval '5 days'),
('f1000000-0000-0000-0000-000000000003', '770e8400-e29b-41d4-a716-446655440000', 'DevOps Engineer', 'Fullstack Intern', '["Kubernetes", "Terraform", "Helm", "ArgoCD", "Prometheus"]', '{"courses": ["CKA", "Terraform Associate", "GitOps with ArgoCD"], "books": ["Kubernetes Up & Running", "Terraform: Up & Running"]}', 10, 'ACTIVE', now() - interval '3 days'),
('f1000000-0000-0000-0000-000000000004', '880e8400-e29b-41d4-a716-446655440000', 'Platform Engineer', 'DevOps Fresher', '["Crossplane", "Backstage", "Policy as Code", "Golden Paths"]', '{"courses": ["Platform Engineering", "Backstage.io"], "books": ["Platform Engineering", "Team Topologies"]}', 12, 'DRAFT', now() - interval '1 day'),
('f1000000-0000-0000-0000-000000000005', '990e8400-e29b-41d4-a716-446655440000', 'QA Automation Lead', 'Tester Fresher', '["Playwright", "Cypress", "API Testing", "Performance Testing", "Test Architecture"]', '{"courses": ["Playwright Advanced", "Performance Testing with k6"], "books": ["Continuous Testing", "Accelerate"]}', 8, 'DRAFT', now())
ON CONFLICT (id) DO NOTHING;

-- Usage logs
INSERT INTO ai_usage_logs (id, user_id, conversation_id, model, provider, prompt_tokens, completion_tokens, total_tokens, cost_usd, request_type, success, created_at) VALUES
('a1000000-0000-0000-0000-000000000001', '550e8400-e29b-41d4-a716-446655440000', 'c1000000-0000-0000-0000-000000000001', 'gpt-4o-mini', 'openai', 450, 320, 770, 0.00154, 'chat_completion', true, now() - interval '5 days'),
('a1000000-0000-0000-0000-000000000002', '550e8400-e29b-41d4-a716-446655440000', 'c1000000-0000-0000-0000-000000000002', 'claude-3-5-haiku', 'anthropic', 680, 420, 1100, 0.00220, 'chat_completion', true, now() - interval '3 days'),
('a1000000-0000-0000-0000-000000000003', '660e8400-e29b-41d4-a716-446655440000', 'c1000000-0000-0000-0000-000000000003', 'gpt-4o-mini', 'openai', 320, 180, 500, 0.00100, 'chat_completion', true, now() - interval '2 days'),
('a1000000-0000-0000-0000-000000000004', '770e8400-e29b-41d4-a716-446655440000', 'c1000000-0000-0000-0000-000000000004', 'gemini-2.0-flash', 'google', 1200, 850, 2050, 0.00082, 'chat_completion', true, now() - interval '1 day'),
('a1000000-0000-0000-0000-000000000005', '880e8400-e29b-41d4-a716-446655440000', 'c1000000-0000-0000-0000-000000000005', 'gpt-4o-mini', 'openai', 580, 340, 920, 0.00184, 'chat_completion', true, now() - interval '12 hours'),
('a1000000-0000-0000-0000-000000000006', '990e8400-e29b-41d4-a716-446655440000', NULL, 'gpt-4o-mini', 'openai', 200, 150, 350, 0.00070, 'skill_recommendation', true, now() - interval '6 hours'),
('a1000000-0000-0000-0000-000000000007', '770e8400-e29b-41d4-a716-446655440000', NULL, 'claude-3-5-haiku', 'anthropic', 400, 280, 680, 0.00136, 'career_path', true, now() - interval '3 hours')
ON CONFLICT (id) DO NOTHING;