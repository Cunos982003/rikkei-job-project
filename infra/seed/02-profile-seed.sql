-- =============================================================================
-- DEMO PHASE SEED DATA - Profile Service
-- Target: ~360–490 records
-- Run on profile_service database
-- =============================================================================

-- =============================================================================
-- CANDIDATE PROFILES (40) - one per candidate user
-- =============================================================================
-- Batch 1: RA-JAVA-2026 (8 candidates)
INSERT INTO candidate_profiles (id, user_id, full_name, email, program, class_code, course_name, headline, summary, phone, dob, gender, address, avatar_url, visibility, consent_at, created_at, updated_at) VALUES
(gen_random_uuid(), 'user-cand-001', 'Nguyễn Văn An', 'candidate1@rikkeijobs.test', 'Lập trình Java Fullstack Chuyên sâu', 'RA-JAVA-2026', 'Java Fullstack Chuyên sâu', 'Java Backend Developer | 2 năm kinh nghiệm Spring Boot, Microservices', 'Tôi là một Java Developer đam mê xây dựng hệ thống có khả năng mở rộng. Có kinh nghiệm với Spring Boot, PostgreSQL, Kafka, Docker. Tốt nghiệp khóa Java Fullstack tại Rikkei Academy.', '0987654321', '2003-01-15', 'MALE', 'Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate1', 'PENDING', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', NOW()),
(gen_random_uuid(), 'user-cand-002', 'Trần Thị Bình', 'candidate2@rikkeijobs.test', 'Lập trình Java Fullstack Chuyên sâu', 'RA-JAVA-2026', 'Java Fullstack Chuyên sâu', 'Junior Java Developer | Spring Boot, SQL, Git', 'Sinh viên năm cuối chuyên ngành CNTT. Yêu thích lập trình backend. Đã hoàn thành 3 dự án thực tập tại Rikkei Academy với Spring Boot, JPA, PostgreSQL.', '0912345678', '2003-04-12', 'FEMALE', '45 Nguyễn Trãi, Thanh Xuân, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate2', 'PENDING', NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days', NOW()),
(gen_random_uuid(), 'user-cand-003', 'Lê Hoàng Cường', 'candidate3@rikkeijobs.test', 'Lập trình Java Fullstack Chuyên sâu', 'RA-JAVA-2026', 'Java Fullstack Chuyên sâu', 'Java Developer | Microservices, Kafka, Redis', 'Có nền tảng vững chắc về Java Core, Spring Framework. Đã tham gia phát triển hệ thống thanh toán dùng Kafka, Redis cache. Mong muốn gia nhập môi trường chuyên nghiệp.', '0923456789', '2002-08-20', 'MALE', '12 Trần Phú, Ba Đình, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate3', 'PUBLIC', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days', NOW()),
(gen_random_uuid(), 'user-cand-004', 'Phạm Thị Dung', 'candidate4@rikkeijobs.test', 'Lập trình Java Fullstack Chuyên sâu', 'RA-JAVA-2026', 'Java Fullstack Chuyên sâu', 'Backend Developer | Java, Spring Boot, PostgreSQL', 'Tốt nghiệp loại Khá khóa Java Fullstack. Có portfolio 2 dự án: E-commerce và Hospital Management. Thành thạo JPA, Hibernate, REST API design.', '0934567890', '2003-11-05', 'FEMALE', '78 Giải Phóng, Hai Bà Trưng, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate4', 'PENDING', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', NOW()),
(gen_random_uuid(), 'user-cand-005', 'Võ Minh Đức', 'candidate5@rikkeijobs.test', 'Lập trình Java Fullstack Chuyên sâu', 'RA-JAVA-2026', 'Java Fullstack Chuyên sâu', 'Fresher Java Developer | Học nhanh, làm việc nhóm tốt', 'Mới tốt nghiệp, đang tìm kiếm vị trí Junior Java Developer. Có tư duy logic tốt, đã tự học thêm Docker, Kubernetes cơ bản. Sẵn sàng học hỏi công nghệ mới.', '0945678901', '2004-02-28', 'MALE', '234 Láng Hạ, Đống Đa, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate5', 'REJECTED', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days', NOW()),
(gen_random_uuid(), 'user-cand-006', 'Ngô Thị Giang', 'candidate6@rikkeijobs.test', 'Lập trình Java Fullstack Chuyên sâu', 'RA-JAVA-2026', 'Java Fullstack Chuyên sâu', 'Java Developer | 1 năm kinh nghiệm thực tập', 'Đã thực tập 1 năm tại Rikkeisoft. Tham gia module thanh toán, tích hợp VNPay, MoMo. Biết Spring Security, OAuth2. Tiếng Anh TOEIC 750.', '0956789012', '2003-06-18', 'FEMALE', '56 Nguyễn Đình Chiểu, Hai Bà Trưng, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate6', 'PUBLIC', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', NOW()),
(gen_random_uuid(), 'user-cand-007', 'Đặng Văn Hiếu', 'candidate7@rikkeijobs.test', 'Lập trình Java Fullstack Chuyên sâu', 'RA-JAVA-2026', 'Java Fullstack Chuyên sâu', 'Backend Engineer | Spring Cloud, Kubernetes', 'Có kinh nghiệm deploy lên Kubernetes, dùng Helm chart. Biết Spring Cloud Gateway, Config Server, Eureka. Đã hoàn thành chứng chỉ AWS Cloud Practitioner.', '0967890123', '2002-12-03', 'MALE', '89 Xuân Thủy, Cầu Giấy, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate7', 'PENDING', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW()),
(gen_random_uuid(), 'user-cand-008', 'Bùi Thị Hoa', 'candidate8@rikkeijobs.test', 'Lập trình Java Fullstack Chuyên sâu', 'RA-JAVA-2026', 'Java Fullstack Chuyên sâu', 'Java Developer | Clean Code, Unit Test, CI/CD', 'Yêu thích viết code sạch, áp dụng SOLID principles. Có kinh nghiệm setup CI/CD với GitHub Actions, SonarQube. Tự học thêm GraphQL, gRPC.', '0978901234', '2003-09-22', 'FEMALE', '123 Kim Mã, Ba Đình, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate8', 'PUBLIC', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW())

ON CONFLICT (user_id) DO NOTHING;

-- Batch 2: RA-FE-2026 (8 candidates)
INSERT INTO candidate_profiles (id, user_id, full_name, email, program, class_code, course_name, headline, summary, phone, dob, gender, address, avatar_url, visibility, consent_at, created_at, updated_at) VALUES
(gen_random_uuid(), 'user-cand-009', 'Huỳnh Văn Khang', 'candidate9@rikkeijobs.test', 'Lập trình Web Frontend Chuyên nghiệp', 'RA-FE-2026', 'Frontend Chuyên nghiệp', 'React Developer | TypeScript, Next.js, Tailwind', 'Chuyên Frontend 2 năm. Thành thạo React 18, TypeScript, Next.js 14, Tailwind CSS, Zustand. Có kinh nghiệm tối ưu hiệu suất, SSR, code splitting.', '0989012345', '2003-03-10', 'MALE', '34 Nguyễn Văn Cừ, Long Biên, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate9', 'PUBLIC', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', NOW()),
(gen_random_uuid(), 'user-cand-010', 'Đỗ Thị Lan', 'candidate10@rikkeijobs.test', 'Lập trình Web Frontend Chuyên nghiệp', 'RA-FE-2026', 'Frontend Chuyên nghiệp', 'Frontend Developer | React, Redux, Jest', 'Tốt nghiệp loại Giỏi khóa Frontend. Có 3 dự án portfolio: Dashboard admin, E-commerce, Social app. Biết testing với Jest, React Testing Library.', '0990123456', '2003-07-25', 'FEMALE', '67 Trần Hưng Đạo, Hoàn Kiếm, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate10', 'PENDING', NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days', NOW()),
(gen_random_uuid(), 'user-cand-011', 'Phan Văn Minh', 'candidate11@rikkeijobs.test', 'Lập trình Web Frontend Chuyên nghiệp', 'RA-FE-2026', 'Frontend Chuyên nghiệp', 'React/Next.js Developer | 1.5 năm kinh nghiệm', 'Đã làm việc tại startup fintech 1.5 năm. Xây dựng dashboard realtime với WebSocket, Chart.js. Biết Next.js App Router, Server Components, Prisma ORM.', '0901234567', '2002-11-14', 'MALE', '12 Lê Duẩn, Hai Bà Trưng, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate11', 'REJECTED', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days', NOW()),
(gen_random_uuid(), 'user-cand-012', 'Nguyễn Thị Nga', 'candidate12@rikkeijobs.test', 'Lập trình Web Frontend Chuyên nghiệp', 'RA-FE-2026', 'Frontend Chuyên nghiệp', 'Frontend Engineer | Vue.js, Nuxt, Pinia', 'Chuyển từ Vue 3 sang React. Có nền tảng tốt về component-driven development. Biết Storybook, Chromatic, accessibility (WCAG 2.1).', '0912345678', '2003-05-30', 'FEMALE', '234 Tây Sơn, Đống Đa, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate12', 'PUBLIC', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', NOW()),
(gen_random_uuid(), 'user-cand-013', 'Trần Văn Oanh', 'candidate13@rikkeijobs.test', 'Lập trình Web Frontend Chuyên nghiệp', 'RA-FE-2026', 'Frontend Chuyên nghiệp', 'Junior Frontend Developer | Học nhanh, thích thách thức', 'Mới ra trường, portfolio 4 dự án React. Đã tự học Next.js 14, tRPC, Prisma. Tham gia hackathon Rikkei Academy đạt giải nhì. Sẵn sàng on-site.', '0923456789', '2004-01-08', 'MALE', '456 Nguyễn Xiển, Thanh Xuân, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate13', 'PENDING', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days', NOW()),
(gen_random_uuid(), 'user-cand-014', 'Lê Thị Phương', 'candidate14@rikkeijobs.test', 'Lập trình Web Frontend Chuyên nghiệp', 'RA-FE-2026', 'Frontend Chuyên nghiệp', 'React Developer | UI/UX sensitivity, Figma', 'Có background thiết kế (Figma, Adobe XD). Chuyển sang dev 1 năm. Yêu thích building beautiful interfaces. Biết Framer Motion, GSAP animation.', '0934567890', '2003-08-17', 'FEMALE', '78 Nguyễn Chí Thanh, Đống Đa, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate14', 'PUBLIC', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', NOW()),
(gen_random_uuid(), 'user-cand-015', 'Võ Văn Quang', 'candidate15@rikkeijobs.test', 'Lập trình Web Frontend Chuyên nghiệp', 'RA-FE-2026', 'Frontend Chuyên nghiệp', 'Frontend Developer | Performance, SEO, PWA', 'Chuyên tối ưu Core Web Vitals. Đã cải thiện LCP từ 4.2s xuống 1.8s cho dự án e-commerce. Biết Service Worker, Workbox, Lighthouse CI.', '0945678901', '2002-04-22', 'MALE', '23 Phạm Văn Đồng, Cầu Giấy, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate15', 'PENDING', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW()),
(gen_random_uuid(), 'user-cand-016', 'Ngô Thị Quyên', 'candidate16@rikkeijobs.test', 'Lập trình Web Frontend Chuyên nghiệp', 'RA-FE-2026', 'Frontend Chuyên nghiệp', 'React/TypeScript Developer | Testing, CI/CD', 'Có 2 năm kinh nghiệm. Thiết lập testing pipeline: Unit (Jest), E2E (Cypress), Visual (Chromatic). Biết Monorepo (Nx, Turborepo).', '0956789012', '2003-12-11', 'FEMALE', '345 Hoàng Hoa Thám, Ba Đình, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate16', 'REJECTED', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW())

ON CONFLICT (user_id) DO NOTHING;

-- Batch 3: RA-DO-2026 (8 candidates)
INSERT INTO candidate_profiles (id, user_id, full_name, email, program, class_code, course_name, headline, summary, phone, dob, gender, address, avatar_url, visibility, consent_at, created_at, updated_at) VALUES
(gen_random_uuid(), 'user-cand-017', 'Đặng Văn Sơn', 'candidate17@rikkeijobs.test', 'DevOps & Cloud Engineering', 'RA-DO-2026', 'DevOps Engineering', 'DevOps Engineer | AWS, Kubernetes, Terraform', '3 năm DevOps. Quản lý cluster EKS 50+ nodes. IaC với Terraform, Helm. CI/CD GitHub Actions, ArgoCD. Monitoring: Prometheus, Grafana, ELK.', '0967890123', '2001-06-15', 'MALE', '123 Trần Duy Hưng, Cầu Giấy, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate17', 'PUBLIC', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', NOW()),
(gen_random_uuid(), 'user-cand-018', 'Bùi Thị Thảo', 'candidate18@rikkeijobs.test', 'DevOps & Cloud Engineering', 'RA-DO-2026', 'DevOps Engineering', 'Cloud Engineer | AWS, GCP, Serverless', 'Chứng chỉ AWS Solutions Architect Associate. Kinh nghiệm Lambda, API Gateway, DynamoDB, EventBridge. Biết Pulumi, CDK. Tự học thêm GCP.', '0978901234', '2002-09-28', 'FEMALE', '456 Nguyễn Trãi, Thanh Xuân, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate18', 'PENDING', NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days', NOW()),
(gen_random_uuid(), 'user-cand-019', 'Huỳnh Văn Út', 'candidate19@rikkeijobs.test', 'DevOps & Cloud Engineering', 'RA-DO-2026', 'DevOps Engineering', 'Site Reliability Engineer | K8s, Observability', 'Quản lý production K8s cluster. Thiết lập SLO/SLI, error budgeting. Biết Istio, Linkerd, Cert-Manager. On-call rotation 24/7.', '0989012345', '2000-12-05', 'MALE', '789 Giải Phóng, Hai Bà Trưng, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate19', 'PUBLIC', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days', NOW()),
(gen_random_uuid(), 'user-cand-020', 'Đỗ Thị Vân', 'candidate20@rikkeijobs.test', 'DevOps & Cloud Engineering', 'RA-DO-2026', 'DevOps Engineering', 'DevOps Engineer | GitOps, ArgoCD, Helm', 'Triển khai GitOps cho 20+ microservices. Tự viết Helm chart, Kustomize. Biết FluxCD, Tekton pipeline. Chứng thị CKAD đang ôn.', '0990123456', '2003-02-14', 'FEMALE', '234 Đại Cồ Việt, Hai Bà Trưng, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate20', 'PENDING', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', NOW()),
(gen_random_uuid(), 'user-cand-021', 'Phan Văn Xuân', 'candidate21@rikkeijobs.test', 'DevOps & Cloud Engineering', 'RA-DO-2026', 'DevOps Engineering', 'Platform Engineer | Internal Developer Platform', 'Xây dựng IDP cho team dev. Backstage, Crossplane, ArgoCD. Tự động hóa provision DB, Kafka, Redis. Giảm lead time từ 2 tuần xuống 2 giờ.', '0901234567', '2001-03-22', 'MALE', '567 Trần Phú, Ba Đình, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate21', 'REJECTED', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days', NOW()),
(gen_random_uuid(), 'user-cand-022', 'Nguyễn Thị Yến', 'candidate22@rikkeijobs.test', 'DevOps & Cloud Engineering', 'RA-DO-2026', 'DevOps Engineering', 'Cloud Engineer | Multi-cloud, Cost Optimization', 'Quản lý AWS + Azure hybrid. FinOps: giảm chi phí cloud 30% qua Rightsizing, Spot Instance. Biết CloudHealth, Kubecost.', '0912345678', '2002-07-19', 'FEMALE', '890 Nguyễn Xiển, Thanh Xuân, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate22', 'PUBLIC', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', NOW()),
(gen_random_uuid(), 'user-cand-023', 'Trần Văn Ánh', 'candidate23@rikkeijobs.test', 'DevOps & Cloud Engineering', 'RA-DO-2026', 'DevOps Engineering', 'Junior DevOps | Linux, Docker, Jenkins', 'Mới tốt nghiệp khóa DevOps. Thành thạo Linux administration, Docker, Docker Compose. CI/CD với Jenkins, GitLab CI. Đang học Kubernetes.', '0923456789', '2004-05-03', 'MALE', '1234 Láng Hạ, Đống Đa, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate23', 'PENDING', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW()),
(gen_random_uuid(), 'user-cand-024', 'Lê Thị Bình', 'candidate24@rikkeijobs.test', 'DevOps & Cloud Engineering', 'RA-DO-2026', 'DevOps Engineering', 'DevOps Engineer | Security, Compliance', 'Chuyên DevSecOps. Trích xuất SBOM, scan vulnerability (Trivy, Grype). Policy as Code với OPA, Kyverno. Tuân thủ ISO 27001.', '0934567890', '2003-10-27', 'FEMALE', '5678 Kim Mã, Ba Đình, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate24', 'PUBLIC', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW())

ON CONFLICT (user_id) DO NOTHING;

-- Batch 4: RA-DA-2026 (8 candidates)
INSERT INTO candidate_profiles (id, user_id, full_name, email, program, class_code, course_name, headline, summary, phone, dob, gender, address, avatar_url, visibility, consent_at, created_at, updated_at) VALUES
(gen_random_uuid(), 'user-cand-025', 'Võ Văn Cường', 'candidate25@rikkeijobs.test', 'Data Science & AI Engineering', 'RA-DA-2026', 'Data/AI Engineering', 'Data Engineer | Python, Spark, Airflow', '3 năm Data Engineering. Xây dựng data lake trên S3 + Athena. ETL với Airflow, dbt, Spark. Data quality với Great Expectations.', '0945678901', '2001-01-12', 'MALE', '2345 Nguyễn Văn Cừ, Long Biên, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate25', 'PUBLIC', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', NOW()),
(gen_random_uuid(), 'user-cand-026', 'Ngô Thị Diễm', 'candidate26@rikkeijobs.test', 'Data Science & AI Engineering', 'RA-DA-2026', 'Data/AI Engineering', 'ML Engineer | MLOps, Kubeflow, MLflow', 'Triển khai ML models lên production. CI/CD cho ML với Kubeflow Pipelines. Feature store Feast. A/B testing framework. Biết TensorFlow, PyTorch.', '0956789012', '2002-04-08', 'FEMALE', '6789 Trần Hưng Đạo, Hoàn Kiếm, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate26', 'PENDING', NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days', NOW()),
(gen_random_uuid(), 'user-cand-027', 'Đặng Văn Em', 'candidate27@rikkeijobs.test', 'Data Science & AI Engineering', 'RA-DA-2026', 'Data/AI Engineering', 'Data Scientist | NLP, Computer Vision', 'Phát triển chatbot tiếng Việt (PhoBERT). Object detection YOLOv8 cho retail. Biết HuggingFace, ONNX, TensorRT. Có 2 bài báo conference.', '0967890123', '2000-08-30', 'MALE', '12345 Lê Duẩn, Hai Bà Trưng, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate27', 'REJECTED', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days', NOW()),
(gen_random_uuid(), 'user-cand-028', 'Bùi Thị Phượng', 'candidate28@rikkeijobs.test', 'Data Science & AI Engineering', 'RA-DA-2026', 'Data/AI Engineering', 'AI Engineer | LLM, RAG, Vector DB', 'Xây dựng RAG system cho enterprise search. Pinecone, Weaviate, Qdrant. Fine-tuning Llama 2, Mistral. Prompt engineering, LangChain, LlamaIndex.', '0978901234', '2003-11-16', 'FEMALE', '67890 Nguyễn Chí Thanh, Đống Đa, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate28', 'PUBLIC', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', NOW()),
(gen_random_uuid(), 'user-cand-029', 'Huỳnh Văn Giang', 'candidate29@rikkeijobs.test', 'Data Science & AI Engineering', 'RA-DA-2026', 'Data/AI Engineering', 'Data Analyst | SQL, Tableau, PowerBI', '4 năm BI. Dashboard cho C-level. Data modeling Kimball. DBT transformation. Self-service analytics. Chuyển sang Data Engineering.', '0989012345', '2001-06-23', 'MALE', '23456 Phạm Văn Đồng, Cầu Giấy, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate29', 'PENDING', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days', NOW()),
(gen_random_uuid(), 'user-cand-030', 'Đỗ Thị Hương', 'candidate30@rikkeijobs.test', 'Data Science & AI Engineering', 'RA-DA-2026', 'Data/AI Engineering', 'ML Engineer | Recommender Systems', 'Xây dựng recommender cho e-commerce (collaborative filtering, content-based). Implicit feedback, matrix factorization. Online learning với River.', '0990123456', '2002-09-07', 'FEMALE', '78901 Tây Sơn, Đống Đa, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate30', 'PUBLIC', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', NOW()),
(gen_random_uuid(), 'user-cand-031', 'Phan Văn Hùng', 'candidate31@rikkeijobs.test', 'Data Science & AI Engineering', 'RA-DA-2026', 'Data/AI Engineering', 'AI Research Engineer | Generative AI', 'Nghiên cứu Stable Diffusion, ControlNet. Fine-tuning LoRA, DreamBooth. ComfyUI workflows. Có GitHub stars 500+. Open source contributor.', '0901234567', '2000-12-19', 'MALE', '123456 Giải Phóng, Hai Bà Trưng, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate31', 'PENDING', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW()),
(gen_random_uuid(), 'user-cand-032', 'Nguyễn Thị Loan', 'candidate32@rikkeijobs.test', 'Data Science & AI Engineering', 'RA-DA-2026', 'Data/AI Engineering', 'Data Engineer | Streaming, Flink, Kafka', 'Real-time analytics với Flink SQL. Change Data Capture Debezium. Exactly-once semantics. Windowing, watermark. Event-driven architecture.', '0912345678', '2003-03-14', 'FEMALE', '678901 Đại Cồ Việt, Hai Bà Trưng, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate32', 'REJECTED', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW())

ON CONFLICT (user_id) DO NOTHING;

-- Batch 5: RA-MQ-2026 (8 candidates)
INSERT INTO candidate_profiles (id, user_id, full_name, email, program, class_code, course_name, headline, summary, phone, dob, gender, address, avatar_url, visibility, consent_at, created_at, updated_at) VALUES
(gen_random_uuid(), 'user-cand-033', 'Trần Văn Minh', 'candidate33@rikkeijobs.test', 'Mobile Development & QA', 'RA-MQ-2026', 'Mobile/QA Engineering', 'Mobile Developer | Flutter, React Native', '3 năm mobile. Flutter (Dart), React Native. State management: Riverpod, Bloc, Redux. CI/CD Fastlane, CodePush. App Store/Play Store release.', '0923456789', '2001-05-11', 'MALE', '234567 Nguyễn Trãi, Thanh Xuân, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate33', 'PUBLIC', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', NOW()),
(gen_random_uuid(), 'user-cand-034', 'Lê Thị Nga', 'candidate34@rikkeijobs.test', 'Mobile Development & QA', 'RA-MQ-2026', 'Mobile/QA Engineering', 'QA Engineer | Automation, Playwright, Cypress', 'QA lead 2 năm. Test strategy, automation framework. API test (RestAssured), UI test (Playwright). Contract testing Pact. Shift-left testing.', '0934567890', '2002-08-24', 'FEMALE', '789012 Nguyễn Văn Cừ, Long Biên, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate34', 'PENDING', NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days', NOW()),
(gen_random_uuid(), 'user-cand-035', 'Võ Văn Oanh', 'candidate35@rikkeijobs.test', 'Mobile Development & QA', 'RA-MQ-2026', 'Mobile/QA Engineering', 'iOS Developer | Swift, SwiftUI, Combine', 'Native iOS 3 năm. Swift 5.9, SwiftUI, Combine, Async/Await. Core Data, Realm. App Clips, Widgets. TestFlight, TestFairy. App Store optimization.', '0945678901', '2000-11-02', 'MALE', '1234567 Trần Hưng Đạo, Hoàn Kiếm, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate35', 'REJECTED', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days', NOW()),
(gen_random_uuid(), 'user-cand-036', 'Ngô Thị Quyên', 'candidate36@rikkeijobs.test', 'Mobile Development & QA', 'RA-MQ-2026', 'Mobile/QA Engineering', 'Android Developer | Kotlin, Jetpack Compose', 'Native Android 2 năm. Kotlin, Coroutines, Flow. Jetpack Compose, Room, Hilt. Modularization (Clean Architecture). Play Console, Firebase Crashlytics.', '0956789012', '2003-02-17', 'FEMALE', '6789012 Lê Duẩn, Hai Bà Trưng, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate36', 'PUBLIC', NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', NOW()),
(gen_random_uuid(), 'user-cand-037', 'Đặng Văn Sơn', 'candidate37@rikkeijobs.test', 'Mobile Development & QA', 'RA-MQ-2026', 'Mobile/QA Engineering', 'SDET | Test Automation, Performance', 'SDET 3 năm. Framework từ đầu (Java + TestNG + Selenium). Performance test JMeter, k6. Chaos engineering Litmus. CI/CD integration.', '0967890123', '2001-07-09', 'MALE', '2345678 Phạm Văn Đồng, Cầu Giấy, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate37', 'PENDING', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days', NOW()),
(gen_random_uuid(), 'user-cand-038', 'Bùi Thị Thảo', 'candidate38@rikkeijobs.test', 'Mobile Development & QA', 'RA-MQ-2026', 'Mobile/QA Engineering', 'QA Engineer | Mobile Testing, Appium', 'Chuyên mobile testing. Appium (iOS/Android), Detox, EarlGrey. Device farm (BrowserStack, SauceLabs). Visual regression Applitools.', '0978901234', '2002-10-21', 'FEMALE', '7890123 Tây Sơn, Đống Đa, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate38', 'PUBLIC', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', NOW()),
(gen_random_uuid(), 'user-cand-039', 'Huỳnh Văn Út', 'candidate39@rikkeijobs.test', 'Mobile Development & QA', 'RA-MQ-2026', 'Mobile/QA Engineering', 'Flutter Developer | Clean Architecture, TDD', 'Flutter 2 năm. Clean Architecture, Bloc/Cubit. TDD với mocktail. CI/CD Codemagic. Flavor (dev/staging/prod). Unit/Widget/Integration test.', '0989012345', '2000-03-05', 'MALE', '12345678 Nguyễn Chí Thanh, Đống Đa, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate39', 'PENDING', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW()),
(gen_random_uuid(), 'user-cand-040', 'Đỗ Thị Vân', 'candidate40@rikkeijobs.test', 'Mobile Development & QA', 'RA-MQ-2026', 'Mobile/QA Engineering', 'QA Lead | Process, Metrics, Leadership', 'QA Lead quản lý team 5 QA. Định nghĩa QA process, metrics (defect density, escape rate). Mentor junior. Test management Xray, Zephyr.', '0990123456', '2003-06-28', 'FEMALE', '67890123 Giải Phóng, Hai Bà Trưng, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate40', 'REJECTED', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW())

ON CONFLICT (user_id) DO NOTHING;

-- =============================================================================
-- EDUCATIONS (80) - 2 per candidate
-- =============================================================================
-- Will be inserted programmatically - sample for first 5 candidates:
INSERT INTO educations (id, profile_id, school, major, from_date, to_date, gpa, note, created_at, updated_at)
SELECT
    gen_random_uuid(),
    cp.id,
    CASE (RANDOM() * 5)::int
        WHEN 0 THEN 'Đại học Bách Khoa Hà Nội'
        WHEN 1 THEN 'Đại học Công nghệ Thông tin'
        WHEN 2 THEN 'Đại học Quốc gia Hà Nội'
        WHEN 3 THEN 'Học viện Kỹ thuật Quân sự'
        ELSE 'Đại học FPT'
    END,
    CASE (RANDOM() * 5)::int
        WHEN 0 THEN 'Khoa học Máy tính'
        WHEN 1 THEN 'Kỹ thuật phần mềm'
        WHEN 2 THEN 'Hệ thống thông tin'
        WHEN 3 THEN 'An toàn thông tin'
        ELSE 'Trí tuệ nhân tạo'
    END,
    DATE '2020-09-01',
    DATE '2024-06-30',
    ROUND((2.5 + RANDOM() * 1.5)::numeric, 2),
    'Tốt nghiệp loại ' || CASE WHEN RANDOM() > 0.5 THEN 'Giỏi' ELSE 'Khá' END,
    NOW(), NOW()
FROM candidate_profiles cp
LIMIT 40;

INSERT INTO educations (id, profile_id, school, major, from_date, to_date, gpa, note, created_at, updated_at)
SELECT
    gen_random_uuid(),
    cp.id,
    CASE (RANDOM() * 3)::int
        WHEN 0 THEN 'Rikkei Academy'
        WHEN 1 THEN 'CodeGym'
        ELSE 'TechMaster'
    END,
    'Chương trình chuyên sâu ' ||
    CASE (RANDOM() * 5)::int
        WHEN 0 THEN 'Java Fullstack'
        WHEN 1 THEN 'Frontend React'
        WHEN 2 THEN 'DevOps Cloud'
        WHEN 3 THEN 'Data Science AI'
        ELSE 'Mobile QA'
    END,
    DATE '2024-01-01',
    DATE '2024-12-31',
    ROUND((3.0 + RANDOM() * 1.0)::numeric, 2),
    'Hoàn thành khóa học chuyên sâu 12 tháng',
    NOW(), NOW()
FROM candidate_profiles cp
LIMIT 40
ON CONFLICT DO NOTHING;

-- =============================================================================
-- CERTIFICATES (30) - ~1 per candidate (some have multiple)
-- =============================================================================
INSERT INTO certificates (id, profile_id, name, issuer, issued_at, expires_at, file_url, file_type, created_at, updated_at)
SELECT
    gen_random_uuid(),
    cp.id,
    cert.name,
    cert.issuer,
    (NOW() - (RANDOM() * INTERVAL '730 days'))::date,
    CASE WHEN RANDOM() > 0.3 THEN (NOW() + (RANDOM() * INTERVAL '1095 days'))::date ELSE NULL END,
    'https://rikkei-jobs.s3.amazonaws.com/certs/' || cp.user_id || '_' || cert.name || '.pdf',
    'pdf',
    NOW(), NOW()
FROM candidate_profiles cp
CROSS JOIN LATERAL (
    SELECT name, issuer FROM (VALUES
        ('AWS Certified Solutions Architect - Associate', 'Amazon Web Services'),
        ('AWS Certified Developer - Associate', 'Amazon Web Services'),
        ('CKAD - Certified Kubernetes Application Developer', 'CNCF'),
        ('CKA - Certified Kubernetes Administrator', 'CNCF'),
        ('Google Cloud Professional Cloud Architect', 'Google Cloud'),
        ('Oracle Certified Professional: Java SE 17 Developer', 'Oracle'),
        ('Spring Professional Certification', 'VMware'),
        ('Professional Scrum Master I (PSM I)', 'Scrum.org'),
        ('ISTQB Foundation Level', 'ISTQB'),
        ('TOEIC 850', 'ETS'),
        ('IELTS 7.0', 'British Council'),
        ('MongoDB Certified Developer', 'MongoDB'),
        ('Redis Certified Developer', 'Redis'),
        ('Confluent Certified Developer for Apache Kafka', 'Confluent'),
        ('Databricks Certified Data Engineer Associate', 'Databricks')
    ) AS t(name, issuer) ORDER BY RANDOM() LIMIT 1
) cert
WHERE RANDOM() > 0.25
LIMIT 30
ON CONFLICT DO NOTHING;

-- =============================================================================
-- SKILLS (50) - already exist in mock data, ensure in DB
-- =============================================================================
INSERT INTO skills (id, code, name, group_name, description, created_at, updated_at) VALUES
('skl-01', 'JAVA_CORE', 'Java', 'Programming', 'Lập trình hướng đối tượng, Java 17+, Stream API', NOW(), NOW()),
('skl-02', 'SPRING_BOOT', 'Spring Boot', 'Framework', 'Spring MVC, Spring Data JPA, Spring Security', NOW(), NOW()),
('skl-03', 'SQL', 'SQL', 'Database', 'PostgreSQL, MySQL, thiết kế quan hệ, tối ưu truy vấn', NOW(), NOW()),
('skl-04', 'GIT', 'Git', 'DevOps', 'Quản lý mã nguồn, Git flow, conflict resolution', NOW(), NOW()),
('skl-05', 'REST_API', 'REST API', 'Architecture', 'Thiết kế chuẩn RESTful, status code, OpenAPI/Swagger', NOW(), NOW()),
('skl-06', 'HTML_CSS', 'HTML/CSS', 'Frontend', 'Semantic HTML5, Flexbox, Grid, Responsive', NOW(), NOW()),
('skl-07', 'JAVASCRIPT', 'JavaScript', 'Frontend', 'ES6+, Async/Await, Promise, DOM', NOW(), NOW()),
('skl-08', 'REACTJS', 'ReactJS', 'Frontend', 'React 19, Hooks, State management, Component lifecycle', NOW(), NOW()),
('skl-09', 'TYPESCRIPT', 'TypeScript', 'Frontend', 'Static typing, Interfaces, Generics với React', NOW(), NOW()),
('skl-10', 'DOCKER', 'Docker', 'DevOps', 'Dockerfile, Docker Compose, containerization', NOW(), NOW()),
('skl-11', 'ENGLISH', 'Tiếng Anh', 'Language', 'Đọc hiểu tài liệu kỹ thuật, giao tiếp cơ bản', NOW(), NOW()),
('skl-12', 'PYTHON', 'Python', 'Backend / AI', 'Lập trình Python 3+, FastAPI, xử lý dữ liệu', NOW(), NOW()),
('skl-13', 'MICROSERVICES', 'Microservices', 'Architecture', 'Kiến trúc vi dịch vụ, API Gateway, Service discovery', NOW(), NOW()),
('skl-14', 'REDIS', 'Redis', 'Database', 'In-memory caching, Distributed lock, TTL', NOW(), NOW()),
('skl-15', 'KAFKA', 'Apache Kafka', 'Messaging', 'Event-driven, Pub/Sub, Transactional Outbox', NOW(), NOW()),
('skl-16', 'UNIT_TESTING', 'Unit Testing', 'Testing', 'JUnit 5, Mockito, AssertJ, Vitest', NOW(), NOW()),
('skl-17', 'CI_CD', 'CI/CD', 'DevOps', 'GitHub Actions, automated build and test pipeline', NOW(), NOW()),
('skl-18', 'LINUX_BASH', 'Linux / Bash', 'System', 'Lệnh Linux, quản lý tiến trình, shell scripting', NOW(), NOW()),
('skl-19', 'TAILWIND_CSS', 'Tailwind CSS', 'Frontend', 'Utility-first styling, responsive UI, shadcn/ui', NOW(), NOW()),
('skl-20', 'TEAMWORK', 'Làm việc nhóm', 'Soft Skills', 'Phối hợp Scrum/Agile, giao tiếp và giải quyết vấn đề', NOW(), NOW()),
('skl-21', 'KUBERNETES', 'Kubernetes', 'DevOps', 'Container orchestration, Helm, Kustomize', NOW(), NOW()),
('skl-22', 'TERRAFORM', 'Terraform', 'DevOps', 'Infrastructure as Code, AWS/GCP/Azure provider', NOW(), NOW()),
('skl-23', 'AWS', 'Amazon Web Services', 'Cloud', 'EC2, S3, RDS, Lambda, EKS, IAM', NOW(), NOW()),
('skl-24', 'POSTGRESQL', 'PostgreSQL', 'Database', 'Advanced SQL, Partitioning, Replication, Tuning', NOW(), NOW()),
('skl-25', 'MONGODB', 'MongoDB', 'Database', 'Document database, Aggregation, Sharding', NOW(), NOW()),
('skl-26', 'GRAPHQL', 'GraphQL', 'Architecture', 'Schema design, Resolver, Federation, Apollo', NOW(), NOW()),
('skl-27', 'GRPC', 'gRPC', 'Architecture', 'Protocol Buffers, Unary/Streaming, Interceptors', NOW(), NOW()),
('skl-28', 'NEXTJS', 'Next.js', 'Frontend', 'App Router, Server Components, SSR/SSG/ISR', NOW(), NOW()),
('skl-29', 'ZUSTAND', 'Zustand', 'Frontend', 'Lightweight state management, middleware', NOW(), NOW()),
('skl-30', 'REACT_QUERY', 'TanStack Query', 'Frontend', 'Server state management, caching, invalidation', NOW(), NOW()),
('skl-31', 'JEST', 'Jest', 'Testing', 'Unit testing, mocking, snapshot testing', NOW(), NOW()),
('skl-32', 'PLAYWRIGHT', 'Playwright', 'Testing', 'E2E testing, cross-browser, trace viewer', NOW(), NOW()),
('skl-33', 'CYRESS', 'Cypress', 'Testing', 'E2E testing, time-travel debugging', NOW(), NOW()),
('skl-34', 'STORYBOOK', 'Storybook', 'Frontend', 'Component development, documentation, visual testing', NOW(), NOW()),
('skl-35', 'FIGMA', 'Figma', 'Design', 'UI design, prototyping, design systems', NOW(), NOW()),
('skl-36', 'FRAMER_MOTION', 'Framer Motion', 'Frontend', 'Animation library, layout animations, gestures', NOW(), NOW()),
('skl-37', 'WORKBOX', 'Workbox', 'Frontend', 'Service Worker, PWA, offline support', NOW(), NOW()),
('skl-38', 'LIGHTHOUSE', 'Lighthouse CI', 'Frontend', 'Performance auditing, Core Web Vitals', NOW(), NOW()),
('skl-39', 'NX', 'Nx', 'Frontend', 'Monorepo tool, computation caching, affected', NOW(), NOW()),
('skl-40', 'TURBOREPO', 'Turborepo', 'Frontend', 'Monorepo build system, remote caching', NOW(), NOW()),
('skl-41', 'HELM', 'Helm', 'DevOps', 'Kubernetes package manager, charts, values', NOW(), NOW()),
('skl-42', 'ARGOCD', 'ArgoCD', 'DevOps', 'GitOps continuous delivery, K8s native', NOW(), NOW()),
('skl-43', 'PROMETHEUS', 'Prometheus', 'DevOps', 'Metrics collection, Alertmanager, PromQL', NOW(), NOW()),
('skl-44', 'GRAFANA', 'Grafana', 'DevOps', 'Dashboards, alerting, Loki, Tempo', NOW(), NOW()),
('skl-45', 'ELK', 'ELK Stack', 'DevOps', 'Elasticsearch, Logstash, Kibana, Filebeat', NOW(), NOW()),
('skl-46', 'AIRFLOW', 'Apache Airflow', 'Data', 'Workflow orchestration, DAGs, operators', NOW(), NOW()),
('skl-47', 'SPARK', 'Apache Spark', 'Data', 'Distributed computing, DataFrame, SQL', NOW(), NOW()),
('skl-48', 'DBT', 'dbt', 'Data', 'Data transformation, testing, documentation', NOW(), NOW()),
('skl-49', 'PYTORCH', 'PyTorch', 'AI', 'Deep learning, autograd, distributed training', NOW(), NOW()),
('skl-50', 'LANGCHAIN', 'LangChain', 'AI', 'LLM orchestration, chains, agents, memory', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- CANDIDATE SKILLS (200) - ~5 per candidate with scores
-- =============================================================================
-- Java batch skills
INSERT INTO candidate_skills (id, profile_id, skill_id, self_score, verified_score, level, source, evidence_ref, created_at, updated_at)
SELECT
    gen_random_uuid(),
    cp.id,
    s.id,
    (50 + RANDOM() * 50)::int,
    CASE WHEN RANDOM() > 0.4 THEN (50 + RANDOM() * 50)::int ELSE NULL END,
    CASE
        WHEN (50 + RANDOM() * 50)::int >= 85 THEN 'Nâng cao'
        WHEN (50 + RANDOM() * 50)::int >= 70 THEN 'Vững'
        WHEN (50 + RANDOM() * 50)::int >= 50 THEN 'Khá'
        ELSE 'Cơ bản'
    END,
    CASE WHEN RANDOM() > 0.5 THEN 'LMS' ELSE 'SELF' END,
    'EVIDENCE_' || gen_random_uuid(),
    NOW(), NOW()
FROM candidate_profiles cp
JOIN skills s ON s.code IN ('JAVA_CORE', 'SPRING_BOOT', 'SQL', 'GIT', 'REST_API', 'DOCKER', 'KUBERNETES', 'AWS', 'POSTGRESQL', 'MICROSERVICES')
WHERE cp.class_code = 'RA-JAVA-2026'
LIMIT 80
ON CONFLICT DO NOTHING;

-- Frontend batch skills
INSERT INTO candidate_skills (id, profile_id, skill_id, self_score, verified_score, level, source, evidence_ref, created_at, updated_at)
SELECT
    gen_random_uuid(),
    cp.id,
    s.id,
    (50 + RANDOM() * 50)::int,
    CASE WHEN RANDOM() > 0.4 THEN (50 + RANDOM() * 50)::int ELSE NULL END,
    CASE
        WHEN (50 + RANDOM() * 50)::int >= 85 THEN 'Nâng cao'
        WHEN (50 + RANDOM() * 50)::int >= 70 THEN 'Vững'
        WHEN (50 + RANDOM() * 50)::int >= 50 THEN 'Khá'
        ELSE 'Cơ bản'
    END,
    CASE WHEN RANDOM() > 0.5 THEN 'LMS' ELSE 'SELF' END,
    'EVIDENCE_' || gen_random_uuid(),
    NOW(), NOW()
FROM candidate_profiles cp
JOIN skills s ON s.code IN ('HTML_CSS', 'JAVASCRIPT', 'REACTJS', 'TYPESCRIPT', 'TAILWIND_CSS', 'NEXTJS', 'ZUSTAND', 'REACT_QUERY', 'JEST', 'PLAYWRIGHT', 'STORYBOOK', 'FIGMA', 'FRAMER_MOTION', 'WORKBOX', 'LIGHTHOUSE', 'NX', 'TURBOREPO')
WHERE cp.class_code = 'RA-FE-2026'
LIMIT 80
ON CONFLICT DO NOTHING;

-- DevOps batch skills
INSERT INTO candidate_skills (id, profile_id, skill_id, self_score, verified_score, level, source, evidence_ref, created_at, updated_at)
SELECT
    gen_random_uuid(),
    cp.id,
    s.id,
    (50 + RANDOM() * 50)::int,
    CASE WHEN RANDOM() > 0.4 THEN (50 + RANDOM() * 50)::int ELSE NULL END,
    CASE
        WHEN (50 + RANDOM() * 50)::int >= 85 THEN 'Nâng cao'
        WHEN (50 + RANDOM() * 50)::int >= 70 THEN 'Vững'
        WHEN (50 + RANDOM() * 50)::int >= 50 THEN 'Khá'
        ELSE 'Cơ bản'
    END,
    CASE WHEN RANDOM() > 0.5 THEN 'LMS' ELSE 'SELF' END,
    'EVIDENCE_' || gen_random_uuid(),
    NOW(), NOW()
FROM candidate_profiles cp
JOIN skills s ON s.code IN ('DOCKER', 'KUBERNETES', 'TERRAFORM', 'HELM', 'ARGOCD', 'AWS', 'PROMETHEUS', 'GRAFANA', 'ELK', 'CI_CD', 'LINUX_BASH', 'GIT', 'REDIS', 'KAFKA', 'MICROSERVICES', 'POSTGRESQL')
WHERE cp.class_code = 'RA-DO-2026'
LIMIT 80
ON CONFLICT DO NOTHING;

-- Data/AI batch skills
INSERT INTO candidate_skills (id, profile_id, skill_id, self_score, verified_score, level, source, evidence_ref, created_at, updated_at)
SELECT
    gen_random_uuid(),
    cp.id,
    s.id,
    (50 + RANDOM() * 50)::int,
    CASE WHEN RANDOM() > 0.4 THEN (50 + RANDOM() * 50)::int ELSE NULL END,
    CASE
        WHEN (50 + RANDOM() * 50)::int >= 85 THEN 'Nâng cao'
        WHEN (50 + RANDOM() * 50)::int >= 70 THEN 'Vững'
        WHEN (50 + RANDOM() * 50)::int >= 50 THEN 'Khá'
        ELSE 'Cơ bản'
    END,
    CASE WHEN RANDOM() > 0.5 THEN 'LMS' ELSE 'SELF' END,
    'EVIDENCE_' || gen_random_uuid(),
    NOW(), NOW()
FROM candidate_profiles cp
JOIN skills s ON s.code IN ('PYTHON', 'SQL', 'POSTGRESQL', 'MONGODB', 'AIRFLOW', 'SPARK', 'DBT', 'PYTORCH', 'LANGCHAIN', 'KAFKA', 'REDIS', 'DOCKER', 'KUBERNETES', 'AWS', 'GIT', 'UNIT_TESTING')
WHERE cp.class_code = 'RA-DA-2026'
LIMIT 80
ON CONFLICT DO NOTHING;

-- Mobile/QA batch skills
INSERT INTO candidate_skills (id, profile_id, skill_id, self_score, verified_score, level, source, evidence_ref, created_at, updated_at)
SELECT
    gen_random_uuid(),
    cp.id,
    s.id,
    (50 + RANDOM() * 50)::int,
    CASE WHEN RANDOM() > 0.4 THEN (50 + RANDOM() * 50)::int ELSE NULL END,
    CASE
        WHEN (50 + RANDOM() * 50)::int >= 85 THEN 'Nâng cao'
        WHEN (50 + RANDOM() * 50)::int >= 70 THEN 'Vững'
        WHEN (50 + RANDOM() * 50)::int >= 50 THEN 'Khá'
        ELSE 'Cơ bản'
    END,
    CASE WHEN RANDOM() > 0.5 THEN 'LMS' ELSE 'SELF' END,
    'EVIDENCE_' || gen_random_uuid(),
    NOW(), NOW()
FROM candidate_profiles cp
JOIN skills s ON s.code IN ('REACTJS', 'TYPESCRIPT', 'JEST', 'PLAYWRIGHT', 'CYRESS', 'DOCKER', 'CI_CD', 'GIT', 'LINUX_BASH', 'HTML_CSS', 'JAVASCRIPT', 'REST_API', 'UNIT_TESTING', 'TERRAFORM', 'KUBERNETES', 'AWS')
WHERE cp.class_code = 'RA-MQ-2026'
LIMIT 80
ON CONFLICT DO NOTHING;

-- =============================================================================
-- PROJECTS/PORTFOLIO (40) - 1 per candidate
-- =============================================================================
INSERT INTO projects (id, profile_id, name, description, tech_stack, role, from_date, to_date, github_url, demo_url, created_at, updated_at)
SELECT
    gen_random_uuid(),
    cp.id,
    'Dự án ' || cp.class_code || '_' || ROW_NUMBER() OVER(),
    'Mô tả dự án thực tế: ' ||
    CASE cp.class_code
        WHEN 'RA-JAVA-2026' THEN 'Hệ thống quản lý đơn hàng, thanh toán online, tích hợp VNPay, MoMo. Sử dụng Spring Boot, PostgreSQL, Kafka, Redis, Docker.'
        WHEN 'RA-FE-2026' THEN 'E-commerce PWA với Next.js 14, TypeScript, Tailwind. Tối ưu Core Web Vitals, SSR, ISR. CI/CD GitHub Actions + Vercel.'
        WHEN 'RA-DO-2026' THEN 'Internal Developer Platform trên Kubernetes. Backstage, ArgoCD, Crossplane. Tự động provision DB, Kafka, Redis. Giảm lead time 95%.'
        WHEN 'RA-DA-2026' THEN 'Real-time recommender system cho e-commerce. Flink + Kafka streaming. Feature store Feast. A/B testing. Latency < 50ms.'
        ELSE 'Mobile app Flutter cho delivery food. Clean Architecture, Bloc, CI/CD Codemagic. iOS/Android release. Performance profiling.'
    END,
    CASE cp.class_code
        WHEN 'RA-JAVA-2026' THEN '["Java 17", "Spring Boot 3", "PostgreSQL", "Kafka", "Redis", "Docker", "Kubernetes"]'
        WHEN 'RA-FE-2026' THEN '["React 18", "Next.js 14", "TypeScript", "Tailwind CSS", "Zustand", "TanStack Query", "Jest", "Playwright"]'
        WHEN 'RA-DO-2026' THEN '["Kubernetes", "ArgoCD", "Crossplane", "Backstage", "Terraform", "Helm", "Prometheus", "Grafana"]'
        WHEN 'RA-DA-2026' THEN '["Python", "Apache Flink", "Kafka", "Feast", "Redis", "PostgreSQL", "Docker", "Kubernetes"]'
        ELSE '["Flutter 3", "Dart", "Bloc", "Clean Architecture", "Codemagic", "Firebase", "Riverpod"]'
    END,
    CASE (RANDOM() * 3)::int
        WHEN 0 THEN 'Backend Developer'
        WHEN 1 THEN 'Fullstack Developer'
        ELSE 'Team Lead'
    END,
    (NOW() - (RANDOM() * INTERVAL '365 days'))::date,
    CASE WHEN RANDOM() > 0.3 THEN (NOW() - (RANDOM() * INTERVAL '30 days'))::date ELSE NULL END,
    'https://github.com/rikkei-academy/' || LOWER(cp.class_code) || '-project-' || ROW_NUMBER() OVER(),
    'https://demo.rikkei-jobs.vn/' || LOWER(cp.class_code) || '-project-' || ROW_NUMBER() OVER(),
    NOW(), NOW()
FROM candidate_profiles cp
LIMIT 40
ON CONFLICT DO NOTHING;

-- =============================================================================
-- VERIFICATION
-- =============================================================================
SELECT 'candidate_profiles' as table_name, COUNT(*) FROM candidate_profiles
UNION ALL SELECT 'educations', COUNT(*) FROM educations
UNION ALL SELECT 'certificates', COUNT(*) FROM certificates
UNION ALL SELECT 'skills', COUNT(*) FROM skills
UNION ALL SELECT 'candidate_skills', COUNT(*) FROM candidate_skills
UNION ALL SELECT 'projects', COUNT(*) FROM projects;