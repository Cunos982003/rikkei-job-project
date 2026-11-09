rikkei-jobs/
├── .github/
│   └── workflows/
│       ├── backend-ci.yml             # build + test backend (path filter theo service)
│       ├── frontend-ci.yml            # lint + test + build frontend
│       ├── ai-service-ci.yml          # ruff + pytest ai-career-service
│       └── e2e.yml                    # Playwright cho 5 luồng chính
│
├── backend/
│   ├── pom.xml                        # parent POM: Java 17, Spring Boot 4.1.1, dependencyManagement
│   │
│   ├── common/                        # thư viện dùng chung, không chạy độc lập
│   │   ├── pom.xml
│   │   └── src/main/java/vn/rikkei/jobs/common/
│   │       ├── error/                 # ProblemDetail handler, BusinessException
│   │       ├── security/              # JWT converter, hằng số role, cấu hình method security
│   │       ├── audit/                 # AuditLogWriter (chỉ INSERT)
│   │       ├── outbox/                # OutboxEvent, OutboxPublisher (scheduler → Kafka)
│   │       ├── web/                   # helper phân trang, cấu hình CORS
│   │       └── config/                # Jackson, Clock bean, JPA auditing
│   │
│   ├── api-gateway/ +++++++
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── src/main/
│   │       ├── java/vn/rikkei/jobs/gateway/
│   │       │   ├── GatewayApplication.java
│   │       │   ├── config/            # route theo /api/v1/**, rate limiter (Redis)
│   │       │   └── filter/            # gắn request-id, kiểm tra JWT
│   │       └── resources/application.yml
│   │
│   ├── auth-service/ +++++++                 # MẪU: service đầy đủ cấu trúc
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/vn/rikkei/jobs/auth/
│   │       │   │   ├── AuthServiceApplication.java
│   │       │   │   ├── sso/                    # MODULE mẫu
│   │       │   │   │   ├── api/                # SsoController, ExchangeRequest, ExchangeResponse
│   │       │   │   │   ├── application/        # SsoExchangeUseCase
│   │       │   │   │   ├── domain/             # SsoTicket (value object), SsoException
│   │       │   │   │   └── infrastructure/     # RedisTicketStore (GETDEL, TTL 60s)
│   │       │   │   ├── hraccount/              # HrAccount entity, repository, lockout policy
│   │       │   │   ├── password/               # Argon2 encoder, reset token (hash, 30 phút)
│   │       │   │   ├── token/                  # JwtIssuer, JwksController, KeyProvider (RSA)
│   │       │   │   └── config/                 # SecurityConfig, RedisConfig
│   │       │   └── resources/
│   │       │       ├── application.yml
│   │       │       ├── application-dev.yml
│   │       │       └── db/migration/
│   │       │           └── V1__init_auth.sql
│   │       └── test/java/vn/rikkei/jobs/auth/   # cấu trúc giống main
│   │
│   ├── core-service/  +++++++                # service lớn nhất, nhiều module
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/vn/rikkei/jobs/core/
│   │       │   │   ├── CoreServiceApplication.java
│   │       │   │   ├── profile/               # hồ sơ, CV, portfolio, chứng chỉ, avatar
│   │       │   │   │   ├── api/               # ProfileController, CvController
│   │       │   │   │   │   └── dto/           # request/response record
│   │       │   │   │   ├── application/       # ProfileService, CvService, UploadService
│   │       │   │   │   ├── domain/            # CandidateProfile, Cv, PortfolioItem, enum, Repository
│   │       │   │   │   └── infrastructure/    # S3StorageAdapter, ClamAvScanner, TikaMimeDetector
│   │       │   │   ├── consent/               # VisibilityConsent, rút/đồng ý công khai
│   │       │   │   ├── company/               # hồ sơ doanh nghiệp, tài khoản HR gắn công ty
│   │       │   │   ├── job/                   # JobPosting, JobSkillRequirement, duyệt JD
│   │       │   │   ├── recruitment/           # Application, state machine, status history
│   │       │   │   │   ├── api/
│   │       │   │   │   ├── application/       # ApplicationService, ScreeningService
│   │       │   │   │   ├── domain/            # ApplicationStatus, transition rules
│   │       │   │   │   └── infrastructure/
│   │       │   │   ├── interview/             # lịch, vòng phỏng vấn, offer
│   │       │   │   ├── ioc/                   # InternshipDemand, IocBatch, IocPlacement
│   │       │   │   ├── notification/          # EmailSender, NotificationService, Quartz jobs
│   │       │   │   ├── integration/           # RikkeiMeetClient (interface), MockRikkeiMeetClient
│   │       │   │   └── config/                # Quartz, S3, Mail, Resilience4j
│   │       │   └── resources/
│   │       │       ├── application.yml
│   │       │       ├── application-dev.yml
│   │       │       ├── templates/email/      # Thymeleaf: nhắc lịch, nhận offer, đơn mới
│   │       │       └── db/migration/
│   │       │           ├── V1__init_core.sql
│   │       │           └── V2__add_search_index.sql
│   │       └── test/java/vn/rikkei/jobs/core/
│   │
│   ├── evidence-service/ +++++++
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── src/main/
│   │       ├── java/vn/rikkei/jobs/evidence/
│   │       │   ├── EvidenceServiceApplication.java
│   │       │   ├── source/                    # client từng nguồn, mỗi nguồn một package
│   │       │   │   ├── lms/                   # LmsClient (RestClient + Resilience4j)
│   │       │   │   ├── exam/                  # ExamClient (Khảo thí)
│   │       │   │   └── pm/                    # PmClient (lms-pm)
│   │       │   ├── sync/                      # Spring Batch job đêm, sync theo yêu cầu
│   │       │   │   ├── batch/                 # Job, Step, ItemReader, ItemProcessor, ItemWriter
│   │       │   │   └── SyncService.java
│   │       │   ├── evidence/                  # SourceRecord, upsert theo khóa nguồn
│   │       │   ├── override/                  # EvidenceOverride (chỉ INSERT), báo sai
│   │       │   └── config/                    # BatchConfig, Kafka producer
│   │       └── resources/
│   │           ├── application.yml
│   │           └── db/migration/V1__init_evidence.sql
│   │
│   └── assessment-service/ +++++++
│       ├── pom.xml
│       ├── Dockerfile
│       └── src/main/
│           ├── java/vn/rikkei/jobs/assessment/
│           │   ├── AssessmentServiceApplication.java
│           │   ├── catalog/                   # Skill, danh mục kỹ năng, SkillWeightConfig
│           │   ├── form/                      # phiếu tự đánh giá và phiếu Mentor (Nháp/Đã gửi)
│           │   ├── scoring/                   # công thức trọng số, SkillScoreCalculator
│           │   ├── badge/                     # điều kiện kép, cấp/thu hồi badge
│           │   ├── matching/                  # điểm khớp JD, cache Redis
│           │   ├── gap/                       # khoảng thiếu hụt
│           │   ├── learning/                  # ánh xạ kỹ năng → môn/bài LMS
│           │   ├── messaging/                 # consumer evidence.changed, jd.published
│           │   └── config/
│           └── resources/
│               ├── application.yml
│               └── db/migration/V1__init_assessment.sql
│
├── ai-career-service/                 # Python FastAPI
│   ├── pyproject.toml                 # quản lý bằng uv
│   ├── uv.lock
│   ├── Dockerfile
│   ├── .env.example
│   ├── app/
│   │   ├── main.py                    # khởi tạo FastAPI, đăng ký router
│   │   ├── config.py                  # đọc biến môi trường (pydantic-settings)
│   │   ├── api/                       # chat.py, cv_review.py, mock_interview.py, jobs.py
│   │   ├── schemas/                   # Pydantic request/response
│   │   ├── security/                  # dependency đọc và kiểm tra JWT người dùng
│   │   ├── clients/                   # httpx: core_client.py, assessment_client.py
│   │   ├── masking/                   # pii_masker.py, rules.py (regex email, SĐT, mã học viên)
│   │   ├── quota/                     # rate_limiter.py (Redis, theo ngày/tài khoản)
│   │   ├── services/                  # career_advisor.py, cv_reviewer.py, mock_interview.py
│   │   ├── llm/                       # llm_client.py (bọc SDK của nhà cung cấp)
│   │   ├── prompts/                   # template prompt, tách khỏi code
│   │   ├── persistence/               # models.py, session.py, ai_interaction_log repo
│   │   └── jobs/                      # purge_logs.py (xóa log > 90 ngày)
│   └── tests/
│       ├── test_masking.py
│       ├── test_quota.py
│       └── test_api/
│
├── frontend/                          # React + Vite + Tailwind
│   ├── package.json
│   ├── vite.config.ts                 # proxy /api → gateway :8080
│   ├── tsconfig.json
│   ├── components.json                # cấu hình shadcn/ui
│   ├── eslint.config.js
│   ├── .prettierrc
│   ├── .env.example
│   ├── index.html
│   ├── public/
│   ├── e2e/                           # Playwright: sso.spec.ts, hr-post-jd.spec.ts, ...
│   └── src/
│       ├── main.tsx                   # mount App, providers
│       ├── index.css                  # Tailwind import, biến màu theme
│       ├── app/
│       │   ├── router.tsx             # khai báo route theo vai trò
│       │   ├── providers.tsx          # QueryClientProvider, AuthProvider
│       │   └── guards/                # RequireAuth, RequireRole (chỉ điều hướng)
│       ├── api/
│       │   ├── client.ts              # Axios instance, interceptor Bearer và refresh 401
│       │   ├── errors.ts              # chuẩn hóa ProblemDetail
│       │   ├── types/                 # sinh từ OpenAPI (openapi-typescript), không sửa tay
│       │   └── endpoints/             # auth.api.ts, profile.api.ts, jobs.api.ts, ...
│       ├── auth/
│       │   ├── AuthContext.tsx        # access token trong bộ nhớ
│       │   └── SsoCallbackPage.tsx    # route /sso nhận vé từ LMS
│       ├── components/
│       │   ├── ui/                    # component shadcn/ui (sao chép vào, không tải dependency)
│       │   ├── layout/                # AppShell, Sidebar theo vai trò, Header
│       │   └── shared/                # SkillBadge, ScoreChart, FileViewer, EmptyState
│       ├── features/
│       │   ├── candidate/             # hồ sơ, CV, portfolio, kỹ năng, ứng tuyển, AI, học bù
│       │   │   ├── pages/
│       │   │   ├── components/
│       │   │   ├── hooks/             # useProfile, useApplications (TanStack Query)
│       │   │   └── schemas/           # Zod schema cho form
│       │   ├── hr/                    # doanh nghiệp, JD, tìm ứng viên, đơn, phỏng vấn, offer, IOC
│       │   ├── mentor/                # danh sách phiếu được giao, form đánh giá
│       │   ├── staff/                 # duyệt hồ sơ, duyệt JD, mở khóa liên hệ
│       │   └── admin/                 # danh mục kỹ năng, trọng số, audit log, báo sai
│       ├── hooks/                     # hook dùng chung
│       ├── lib/                       # format ngày, tiền, utils
│       └── types/                     # type chỉ dùng cho UI, không có trong API
│
├── infra/
│   ├── docker-compose.yml             # postgres, redis, kafka, minio, clamav, mailpit, wiremock
│   ├── init-db/
│   │   └── 01-create-databases.sql    # tạo auth_db, core_db, evidence_db, assessment_db, ai_db
│   ├── wiremock/
│   │   ├── mappings/                  # lms/, exam/, pm/, meet/
│   │   └── __files/                   # dữ liệu JSON mẫu trả về
│   ├── monitoring/
│   │   └── prometheus.yml
│   └── k8s/                           # để dành giai đoạn sau
│
├── docs/
│   ├── tech-stack.md                  # tài liệu tech stack đã làm
│   ├── adr/
│   │   ├── 0001-microservice-mức-vừa.md
│   │   └── template.md                # mẫu ghi quyết định kiến trúc
│   └── api/
│       ├── bruno/                     # collection request theo service
│       └── openapi/                   # file OpenAPI xuất từ các service
│
├── .gitignore
├── .env.example                       # biến dùng chung cho docker compose
└── README.md                          # cách chạy local, thứ tự khởi động