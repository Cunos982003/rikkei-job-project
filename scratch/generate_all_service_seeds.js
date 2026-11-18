const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const ROOT = path.resolve(__dirname, '..');
const SEED_DIR = path.join(ROOT, 'infra', 'seed');
const CLEAN_USERS_SQL = path.join(SEED_DIR, 'clean_and_sync_users.sql');
const LMS_JSON = path.join(ROOT, 'mock', 'data', 'lms.json');

// Deterministic RFC-4122 v4-like UUID generator from namespace + key
function detUuid(ns, key) {
  const hash = crypto.createHash('sha256').update(`${ns}:${key}`).digest('hex');
  return [
    hash.slice(0, 8),
    hash.slice(8, 12),
    '4' + hash.slice(13, 16),
    ((parseInt(hash.slice(16, 18), 16) & 0x3f) | 0x80).toString(16).padStart(2, '0') + hash.slice(18, 20),
    hash.slice(20, 32)
  ].join('-');
}

function sha256(str) {
  return crypto.createHash('sha256').update(str).digest('hex');
}

function sqlStr(val) {
  if (val === null || val === undefined) return 'NULL';
  return "'" + String(val).replace(/'/g, "''") + "'";
}

function sqlJsonb(obj) {
  return sqlStr(JSON.stringify(obj)) + '::jsonb';
}

// 1. Parse exact 51 users from infra/seed/clean_and_sync_users.sql
function parseUsersFromCleanSql() {
  const content = fs.readFileSync(CLEAN_USERS_SQL, 'utf8');
  const users = [];
  const rowRegex = /\('([0-9a-f-]{36})',\s*'([^']+)',\s*'([^']+)',\s*'([A-Z]+)',\s*'([^']+)',\s*(NULL|'[0-9a-f-]{36}'),\s*(NULL|'[^']+'),\s*'([A-Z]+)'/g;
  let m;
  while ((m = rowRegex.exec(content)) !== null) {
    users.push({
      id: m[1],
      email: m[2],
      fullName: m[3],
      role: m[4],
      passwordHash: m[5],
      companyId: m[6] === 'NULL' ? null : m[6].replace(/'/g, ''),
      lmsUserId: m[7] === 'NULL' ? null : m[7].replace(/'/g, ''),
      status: m[8]
    });
  }
  if (users.length < 50) {
    throw new Error(`Expected >= 50 users in clean_and_sync_users.sql, parsed only ${users.length}`);
  }
  return users;
}

const ALL_USERS = parseUsersFromCleanSql();
const LMS_DATA = JSON.parse(fs.readFileSync(LMS_JSON, 'utf8'));

const ADMINS = ALL_USERS.filter(u => u.role === 'ADMIN');
const STAFFS = ALL_USERS.filter(u => u.role === 'STAFF');
const HRS = ALL_USERS.filter(u => u.role === 'HR');
const MENTORS = ALL_USERS.filter(u => u.role === 'MENTOR');
const CANDIDATES = ALL_USERS.filter(u => u.role === 'CANDIDATE');

console.log(`Parsed from clean_and_sync_users.sql: Total=${ALL_USERS.length} (ADMIN=${ADMINS.length}, STAFF=${STAFFS.length}, HR=${HRS.length}, MENTOR=${MENTORS.length}, CANDIDATE=${CANDIDATES.length})`);

// =============================================================================
// SHARED CROSS-SERVICE MASTER DATA (Companies, Skills, Jobs, Courses)
// =============================================================================

// 10 Companies (#1-#5 EXACTLY match company_id in clean_and_sync_users.sql!)
const COMPANIES = [
  { id: '11111111-1111-4111-8111-111111111111', name: 'Công ty Cổ phần Rikkeisoft', taxCode: '0105982765', industry: 'Công nghệ thông tin & Phần mềm', size: '1000+', address: 'Tầng 21, Tòa nhà Handico, Phạm Hùng, Nam Từ Liêm', city: 'Hà Nội', website: 'https://rikkeisoft.com', desc: 'Tập đoàn công nghệ hàng đầu Việt Nam chuyên gia công phần mềm, chuyển đổi số và AI cho thị trường Nhật Bản & Toàn cầu.' },
  { id: '22222222-2222-4222-8222-222222222222', name: 'Rikkei Digital Solutions', taxCode: '0108123456', industry: 'Chuyển đổi số & Cloud', size: '201-1000', address: 'Tầng 12, Tòa nhà FHome, 16 Lý Thường Kiệt, Hải Châu', city: 'Đà Nẵng', website: 'https://rikkeidigital.com', desc: 'Đơn vị tiên phong tư vấn và triển khai nền tảng chuyển đổi số, Cloud Native và Data Analytics.' },
  { id: '33333333-3333-4333-8333-333333333333', name: 'FPT Software Enterprise', taxCode: '0101248141', industry: 'Phát triển Phần mềm & Xuất khẩu', size: '1000+', address: 'Tòa nhà FPT, 17 Duy Tân, Cầu Giấy', city: 'Hà Nội', website: 'https://fptsoftware.com', desc: 'Doanh nghiệp phần mềm toàn cầu cung cấp dịch vụ kỹ thuật số, Automotive, Cloud và AI.' },
  { id: '44444444-4444-4444-8444-444444444444', name: 'Viettel Solutions Technology', taxCode: '0100109106', industry: 'Viễn thông & Hạ tầng số', size: '1000+', address: 'Số 1 Trần Hữu Dực, Mỹ Đình 2, Nam Từ Liêm', city: 'Hà Nội', website: 'https://solutions.viettel.vn', desc: 'Tổng công ty Giải pháp Doanh nghiệp triển khai Smart City, Cloud Computing và An ninh mạng.' },
  { id: '55555555-5555-4555-8555-555555555555', name: 'VNG Cloud & Digital Hub', taxCode: '0303490096', industry: 'Internet, Fintech & Cloud', size: '1000+', address: 'Z06 Đường số 13, Tân Thuận Đông, Quận 7', city: 'TP. Hồ Chí Minh', website: 'https://vng.com.vn', desc: 'Hệ sinh thái công nghệ số hàng đầu với ZaloPay, VNG Cloud và các sản phẩm nền tảng quy mô hàng chục triệu người dùng.' },
  { id: '66666666-6666-4666-8666-666666666666', name: 'CMC Global Innovation', taxCode: '0107725034', industry: 'Dịch vụ CNTT & Outsourcing', size: '1000+', address: 'Tầng 15, CMC Tower, 11 Duy Tân, Cầu Giấy', city: 'Hà Nội', website: 'https://cmcglobal.com.vn', desc: 'Công ty cung cấp dịch vụ chuyển đổi số, AI và phát triển phần mềm cho khách hàng Nhật, Hàn, APAC.' },
  { id: '77777777-7777-4777-8777-777777777777', name: 'NashTech Vietnam', taxCode: '0302031257', industry: 'Kỹ thuật Phần mềm Quốc tế', size: '1000+', address: '364 Cộng Hòa, Phường 13, Tân Bình', city: 'TP. Hồ Chí Minh', website: 'https://nashtechglobal.com', desc: 'Đơn vị kỹ thuật phần mềm chuẩn Anh Quốc, chuyên Java, .NET, React, QA Automation và Data Engineering.' },
  { id: '88888888-8888-4888-8888-888888888888', name: 'MoMo Fintech Labs', taxCode: '0305289153', industry: 'Tài chính Công nghệ (Fintech)', size: '201-1000', address: 'Tòa nhà Phú Mỹ Hưng, 8 Hoàng Văn Thái, Quận 7', city: 'TP. Hồ Chí Minh', website: 'https://momo.vn', desc: 'Siêu ứng dụng tài chính số với kiến trúc Microservices chịu tải cao và ứng dụng AI chống gian lận.' },
  { id: '99999999-9999-4999-8999-999999999999', name: 'Tiki Engineering Hub', taxCode: '0309532909', industry: 'Thương mại điện tử', size: '201-1000', address: '52 Út Tịch, Phường 4, Tân Bình', city: 'TP. Hồ Chí Minh', website: 'https://tiki.vn', desc: 'Nền tảng thương mại điện tử vận hành trên hệ thống Golang, Kubernetes, Kafka và Recommendation Engine.' },
  { id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', name: 'Base.vn SaaS Enterprise', taxCode: '0107519468', industry: 'Phần mềm Quản trị SaaS', size: '51-200', address: 'Tầng 3, Tòa nhà Autumn, 47 Nguyễn Tuân, Thanh Xuân', city: 'Hà Nội', website: 'https://base.vn', desc: 'Nền tảng SaaS quản trị doanh nghiệp phục vụ hơn 9.000 doanh nghiệp Việt Nam.' }
];

// 50 Skills (exact 24 existing codes + 26 additional real IT/Soft skills)
const SKILLS = [
  { code: 'JAVA_CORE', name: 'Java Core', group: 'Programming' },
  { code: 'SPRING_BOOT', name: 'Spring Boot', group: 'Framework' },
  { code: 'SQL', name: 'SQL', group: 'Database' },
  { code: 'JAVASCRIPT', name: 'JavaScript', group: 'Programming' },
  { code: 'REACT', name: 'React', group: 'Frontend' },
  { code: 'HTML_CSS', name: 'HTML/CSS', group: 'Frontend' },
  { code: 'GIT', name: 'Git', group: 'Tool' },
  { code: 'DOCKER', name: 'Docker', group: 'DevOps' },
  { code: 'KUBERNETES', name: 'Kubernetes', group: 'DevOps' },
  { code: 'CI_CD', name: 'CI/CD', group: 'DevOps' },
  { code: 'TESTING', name: 'Testing', group: 'Quality' },
  { code: 'AGILE', name: 'Agile/Scrum', group: 'Process' },
  { code: 'COMMUNICATION', name: 'Communication', group: 'Soft Skill' },
  { code: 'PROBLEM_SOLVING', name: 'Problem Solving', group: 'Soft Skill' },
  { code: 'TEAMWORK', name: 'Teamwork', group: 'Soft Skill' },
  { code: 'ENGLISH', name: 'English', group: 'Language' },
  { code: 'PYTHON', name: 'Python', group: 'Programming' },
  { code: 'DATA_STRUCTURES', name: 'Data Structures', group: 'CS Fundamental' },
  { code: 'ALGORITHMS', name: 'Algorithms', group: 'CS Fundamental' },
  { code: 'DESIGN_PATTERNS', name: 'Design Patterns', group: 'Architecture' },
  { code: 'TAILWIND_CSS', name: 'Tailwind CSS', group: 'Frontend' },
  { code: 'TYPESCRIPT', name: 'TypeScript', group: 'Programming' },
  { code: 'REST_API', name: 'REST API', group: 'Backend' },
  { code: 'APACHE_KAFKA', name: 'Apache Kafka', group: 'DevOps' },
  // 26 new skills -> total 50
  { code: 'NODEJS', name: 'Node.js', group: 'Backend' },
  { code: 'GOLANG', name: 'Golang', group: 'Programming' },
  { code: 'NEXTJS', name: 'Next.js', group: 'Frontend' },
  { code: 'VUEJS', name: 'Vue.js', group: 'Frontend' },
  { code: 'ANGULAR', name: 'Angular', group: 'Frontend' },
  { code: 'FLUTTER', name: 'Flutter', group: 'Mobile' },
  { code: 'REACT_NATIVE', name: 'React Native', group: 'Mobile' },
  { code: 'POSTGRESQL', name: 'PostgreSQL', group: 'Database' },
  { code: 'MYSQL', name: 'MySQL', group: 'Database' },
  { code: 'MONGODB', name: 'MongoDB', group: 'Database' },
  { code: 'REDIS', name: 'Redis', group: 'Database' },
  { code: 'DATABASE_OPTIMIZATION', name: 'Database Optimization', group: 'Database' },
  { code: 'MICROSERVICES', name: 'Microservices', group: 'Architecture' },
  { code: 'SYSTEM_DESIGN', name: 'System Design', group: 'Architecture' },
  { code: 'GRAPHQL', name: 'GraphQL', group: 'Backend' },
  { code: 'GRPC', name: 'gRPC', group: 'Backend' },
  { code: 'AWS', name: 'AWS Cloud', group: 'Cloud' },
  { code: 'TERRAFORM', name: 'Terraform', group: 'DevOps' },
  { code: 'LINUX_BASH', name: 'Linux & Bash', group: 'DevOps' },
  { code: 'SELENIUM', name: 'Selenium', group: 'Quality' },
  { code: 'PLAYWRIGHT', name: 'Playwright', group: 'Quality' },
  { code: 'JMETER', name: 'JMeter', group: 'Quality' },
  { code: 'MACHINE_LEARNING', name: 'Machine Learning', group: 'AI/Data' },
  { code: 'DATA_ANALYSIS', name: 'Data Analysis', group: 'AI/Data' },
  { code: 'JAPANESE', name: 'Tiếng Nhật (JLPT)', group: 'Language' },
  { code: 'PRESENTATION', name: 'Presentation Skills', group: 'Soft Skill' }
];

// Map 8 study tracks to skills for realistic candidate profiles & jobs
const TRACK_SKILLS = [
  ['JAVA_CORE', 'SPRING_BOOT', 'SQL', 'REST_API', 'GIT'],
  ['REACT', 'TYPESCRIPT', 'JAVASCRIPT', 'HTML_CSS', 'TAILWIND_CSS'],
  ['JAVA_CORE', 'SPRING_BOOT', 'REACT', 'DOCKER', 'APACHE_KAFKA'],
  ['PYTHON', 'MACHINE_LEARNING', 'DATA_ANALYSIS', 'SQL', 'DOCKER'],
  ['FLUTTER', 'REACT_NATIVE', 'TYPESCRIPT', 'REST_API', 'GIT'],
  ['TESTING', 'SELENIUM', 'PLAYWRIGHT', 'JMETER', 'SQL'],
  ['DOCKER', 'KUBERNETES', 'AWS', 'CI_CD', 'LINUX_BASH'],
  ['GOLANG', 'POSTGRESQL', 'REDIS', 'MICROSERVICES', 'GRPC']
];

const TRACK_TITLES = [
  'Java Backend Developer',
  'Frontend React Developer',
  'Fullstack Software Engineer',
  'AI & Data Engineer',
  'Mobile App Developer',
  'QA Automation Engineer',
  'DevOps & Cloud Engineer',
  'Golang Backend Developer'
];

// =============================================================================
// A. AUTH DB SUPPLEMENT (01_seed_auth_db.sql)
// Wraps exact clean_and_sync_users.sql + adds 25 used_sso_tickets + 10 password_reset_tokens
// =============================================================================
function generateAuthSeed() {
  const cleanSql = fs.readFileSync(CLEAN_USERS_SQL, 'utf8');
  const lines = [];
  lines.push('-- =============================================================================');
  lines.push('-- 01_seed_auth_db.sql : AUTH SERVICE SEED DATA (auth_db)');
  lines.push('-- Bao gồm 100% dữ liệu từ clean_and_sync_users.sql (51 users + 100 login_histories)');
  lines.push('-- + 25 used_sso_tickets + 10 password_reset_tokens');
  lines.push('-- =============================================================================');
  lines.push('');
  lines.push(`CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    lms_user_id VARCHAR(100) UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255),
    company_id UUID,
    must_change_password BOOLEAN NOT NULL DEFAULT false,
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    locked_reason VARCHAR(500),
    locked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);`);
  lines.push('');
  lines.push(cleanSql.trim());
  lines.push('');
  lines.push('-- 4. Đảm bảo bảng token tồn tại');
  lines.push(`CREATE TABLE IF NOT EXISTS used_sso_tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket VARCHAR(255) NOT NULL UNIQUE,
    used_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);`);
  lines.push('');
  lines.push('-- 5. Seed 25 Used SSO Tickets (tương ứng RefreshToken / SSO Token đã tiêu thụ)');
  lines.push('INSERT INTO used_sso_tickets (ticket, used_at) VALUES');
  const ticketRows = [];
  for (let i = 0; i < 25; i++) {
    const c = CANDIDATES[i];
    const code = c.lmsUserId.toUpperCase();
    const ticket = `TICKET-${code}-SEED-${String(i + 1).padStart(3, '0')}`;
    ticketRows.push(`    (${sqlStr(ticket)}, NOW() - interval '${25 - i} days ${i % 12} hours')`);
  }
  lines.push(ticketRows.join(',\n') + '\nON CONFLICT (ticket) DO NOTHING;');
  lines.push('');
  lines.push('-- 6. Seed 10 Password Reset Tokens (5 đã dùng, 3 còn hạn, 2 hết hạn)');
  lines.push('INSERT INTO password_reset_tokens (user_id, token_hash, expires_at, used_at, created_at) VALUES');
  const resetRows = [];
  const resetUsers = [HRS[0], HRS[1], HRS[2], CANDIDATES[0], CANDIDATES[3], CANDIDATES[7], CANDIDATES[12], CANDIDATES[19], MENTORS[0], STAFFS[0]];
  resetUsers.forEach((u, idx) => {
    const tokenHash = sha256(`reset-token-seed-${u.email}-${idx}`);
    let expiresExpr, usedExpr, createdExpr;
    if (idx < 5) {
      createdExpr = `NOW() - interval '${10 - idx} days'`;
      expiresExpr = `NOW() - interval '${10 - idx} days' + interval '1 hour'`;
      usedExpr = `NOW() - interval '${10 - idx} days' + interval '15 minutes'`;
    } else if (idx < 8) {
      createdExpr = `NOW() - interval '10 minutes'`;
      expiresExpr = `NOW() + interval '50 minutes'`;
      usedExpr = 'NULL';
    } else {
      createdExpr = `NOW() - interval '2 days'`;
      expiresExpr = `NOW() - interval '2 days' + interval '1 hour'`;
      usedExpr = 'NULL';
    }
    resetRows.push(`    ((SELECT id FROM users WHERE email = ${sqlStr(u.email)} LIMIT 1), ${sqlStr(tokenHash)}, ${expiresExpr}, ${usedExpr}, ${createdExpr})`);
  });
  lines.push(resetRows.join(',\n') + '\nON CONFLICT (token_hash) DO NOTHING;');
  lines.push('');
  return lines.join('\n');
}

// =============================================================================
// B. PROFILE DB SEED (02_seed_profile_db.sql)
// =============================================================================
function levelFromScore(score) {
  if (score >= 85) return 'Nâng cao';
  if (score >= 75) return 'Vững';
  if (score >= 60) return 'Khá';
  return 'Cơ bản';
}

function generateProfileSeed() {
  const profileSchemaSql = fs.readFileSync(path.join(ROOT, 'backend', 'profile-service', 'src', 'main', 'resources', 'schema.sql'), 'utf8');
  const lines = [];
  lines.push('-- =============================================================================');
  lines.push('-- 02_seed_profile_db.sql : PROFILE SERVICE SEED DATA (profile_db)');
  lines.push('-- Khớp 100% schema.sql của profile-service & 40 candidate UUIDs từ clean_and_sync_users.sql');
  lines.push('-- =============================================================================');
  lines.push(profileSchemaSql.trim());
  lines.push('');
  lines.push('-- Bảng kinh nghiệm làm việc mở rộng (không ảnh hưởng schema hiện tại của profile-service)');
  lines.push(`CREATE TABLE IF NOT EXISTS candidate_experiences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    position VARCHAR(255) NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_candidate_experiences_user_id ON candidate_experiences(user_id);`);
  lines.push('');

  // 1. 50 Skills
  lines.push('-- 1. 50 Skills');
  lines.push('INSERT INTO skills (id, code, name, "group") VALUES');
  lines.push(SKILLS.map(s => `    (${sqlStr(detUuid('skill', s.code))}, ${sqlStr(s.code)}, ${sqlStr(s.name)}, ${sqlStr(s.group)})`).join(',\n') + '\nON CONFLICT (code) DO NOTHING;');
  lines.push('');

  // 2. Score Source Weights & Badge Rules
  lines.push('-- 2. Score Source Weights & 8 Badge Rules');
  lines.push(`INSERT INTO score_source_weights (source, weight) VALUES
    ('LMS', 0.30), ('EXAM', 0.25), ('PM', 0.20), ('INTERNSHIP', 0.15), ('MENTOR', 0.05), ('SELF', 0.05)
ON CONFLICT (source) DO NOTHING;`);
  lines.push('');
  const badgeDefs = [
    ['BR-JAVA-EXPERT', 'JAVA_CORE', 85.00, true],
    ['BR-SPRING-MASTER', 'SPRING_BOOT', 80.00, true],
    ['BR-REACT-PRO', 'REACT', 75.00, false],
    ['BR-SQL-GURU', 'SQL', 85.00, false],
    ['BR-TEAM-LEADER', 'TEAMWORK', 80.00, true],
    ['BR-FULLSTACK', 'JAVA_CORE', 70.00, true],
    ['BR-DEVOPS-PRO', 'DOCKER', 80.00, false],
    ['BR-QA-AUTO', 'TESTING', 80.00, false]
  ];
  lines.push('INSERT INTO badge_rules (id, code, skill_id, min_score, need_internship_task) VALUES');
  lines.push(badgeDefs.map(([code, skillCode, minScore, needTask]) =>
    `    (${sqlStr(detUuid('badge_rule', code))}, ${sqlStr(code)}, (SELECT id FROM skills WHERE code = ${sqlStr(skillCode)}), ${minScore.toFixed(2)}, ${needTask})`
  ).join(',\n') + '\nON CONFLICT (code) DO NOTHING;');
  lines.push('');

  // 3. 40 Candidate Profiles (EXACT UUIDs from clean_and_sync_users.sql)
  const universities = [
    'Đại học Bách Khoa Hà Nội',
    'Đại học Công nghệ - ĐHQGHN',
    'Học viện Công nghệ Bưu chính Viễn thông',
    'Đại học Bách Khoa Đà Nẵng',
    'Đại học Công nghệ Thông tin - ĐHQG TP.HCM',
    'Đại học Khoa học Tự nhiên TP.HCM',
    'Đại học FPT',
    'Đại học Giao thông Vận tải'
  ];
  const cities = ['Hà Nội', 'Đà Nẵng', 'TP. Hồ Chí Minh', 'Hải Phòng', 'Huế'];
  const majors = [
    'Kỹ thuật phần mềm',
    'Khoa học máy tính',
    'Hệ thống thông tin',
    'An toàn thông tin',
    'Trí tuệ nhân tạo',
    'Công nghệ thông tin'
  ];

  lines.push('-- 3. 40 Candidate Profiles (Khớp 100% UUID từ clean_and_sync_users.sql)');
  lines.push('INSERT INTO candidate_profiles (user_id, full_name, dob, gender, phone, email, address, avatar_url, headline, summary, program, class_code, course_name, visibility, consent_at, updated_at) VALUES');
  const profileRows = [];
  const eduRows = [];
  const expRows = [];
  const skillScoreRows = [];
  const evidenceRows = [];
  const certRows = [];
  const internshipRows = [];
  const taskRows = [];
  const portfolioRows = [];
  const reviewRows = [];
  const badgeGrantRows = [];

  const certTemplates = [
    ['Oracle Certified Associate, Java SE 17', 'Oracle', 'PDF'],
    ['AWS Certified Cloud Practitioner', 'Amazon Web Services', 'PDF'],
    ['Meta Front-End Developer Certificate', 'Coursera / Meta', 'PDF'],
    ['ISTQB Certified Tester Foundation Level', 'ISTQB', 'PDF'],
    ['Docker Certified Associate', 'Mirantis / Docker', 'PDF'],
    ['Google Data Analytics Professional', 'Google', 'PDF'],
    ['JLPT N3 Japanese Language Proficiency', 'Japan Foundation', 'PDF'],
    ['TOEIC 820 Certificate', 'IIG Vietnam', 'PDF']
  ];

  CANDIDATES.forEach((cand, idx) => {
    const trackIdx = idx % 8;
    const lmsStudent = LMS_DATA.students[cand.lmsUserId.toLowerCase()];
    const program = lmsStudent ? lmsStudent.program : 'Lập trình Java Fullstack Chuyên sâu';
    const classCode = lmsStudent ? lmsStudent.classCode : 'RA-JAVA-2026';
    const courseName = lmsStudent ? lmsStudent.courseName : program;
    const title = TRACK_TITLES[trackIdx];
    const city = cities[idx % cities.length];
    const phone = `09${String(12345600 + idx * 137).slice(0, 8)}`;
    const year = 1999 + (idx % 6);
    const month = String((idx % 12) + 1).padStart(2, '0');
    const day = String(((idx * 3) % 27) + 1).padStart(2, '0');
    const gender = (cand.fullName.includes('Thị') || cand.fullName.includes('Linh') || cand.fullName.includes('Dung') || cand.fullName.includes('Hương') || cand.fullName.includes('Trang') || cand.fullName.includes('Uyên') || cand.fullName.includes('Xuân') || cand.fullName.includes('Tâm') || cand.fullName.includes('Hà') || cand.fullName.includes('Oanh') || cand.fullName.includes('Quyên') || cand.fullName.includes('Thảo') || cand.fullName.includes('Yến') || cand.fullName.includes('Nga') || cand.fullName.includes('Quỳnh')) ? 'FEMALE' : 'MALE';

    // Visibility distribution: 25 PUBLIC, 5 PENDING, 6 PRIVATE, 2 REJECTED, 2 RECRUITER
    let visibility = 'PUBLIC';
    if (idx >= 25 && idx < 30) visibility = 'PENDING';
    else if (idx >= 30 && idx < 36) visibility = 'PRIVATE';
    else if (idx >= 36 && idx < 38) visibility = 'REJECTED';
    else if (idx >= 38) visibility = 'RECRUITER';

    const consentAt = (visibility === 'PRIVATE') ? 'NULL' : `NOW() - interval '${30 - (idx % 20)} days'`;
    const headline = `${title} | Học viên Rikkei Academy (${classCode})`;
    const summary = `Ứng viên ${cand.fullName} hoàn thành chương trình ${program} tại Rikkei Academy, có tư duy lập trình vững và kinh nghiệm thực chiến dự án IOC.`;

    profileRows.push(`    (${sqlStr(cand.id)}, ${sqlStr(cand.fullName)}, ${sqlStr(`${year}-${month}-${day}`)}, ${sqlStr(gender)}, ${sqlStr(phone)}, ${sqlStr(cand.email)}, ${sqlStr(`Số ${idx + 10} Nguyễn Trãi, ${city}`)}, ${sqlStr(`https://api.dicebear.com/7.x/avataaars/svg?seed=candidate${idx + 1}`)}, ${sqlStr(headline)}, ${sqlStr(summary)}, ${sqlStr(program)}, ${sqlStr(classCode)}, ${sqlStr(courseName)}, ${sqlStr(visibility)}, ${consentAt}, NOW() - interval '${idx % 10} days')`);

    // Educations: 70 total (all 40 have Rikkei Academy + first 30 also have University = 70)
    const eduRikkeiId = detUuid('edu-rikkei', cand.id);
    eduRows.push(`    (${sqlStr(eduRikkeiId)}, ${sqlStr(cand.id)}, 'Rikkei Academy', ${sqlStr(program)}, '2026-01-15', '2026-09-30', ${(3.3 + ((idx * 7) % 7) * 0.1).toFixed(2)}, 'Hoàn thành xuất sắc đồ án tốt nghiệp & IOC')`);
    if (idx < 30) {
      const eduUniId = detUuid('edu-uni', cand.id);
      eduRows.push(`    (${sqlStr(eduUniId)}, ${sqlStr(cand.id)}, ${sqlStr(universities[idx % universities.length])}, ${sqlStr(majors[idx % majors.length])}, '2020-09-01', '2024-06-30', ${(3.0 + ((idx * 5) % 9) * 0.1).toFixed(2)}, 'Tốt nghiệp loại Khá/Giỏi')`);
    }

    // Experiences: 60 total (all 40 have 1 IOC / Internship + first 20 have 1 previous part-time/freelance = 60)
    const exp1Id = detUuid('exp-1', cand.id);
    const expComp1 = COMPANIES[idx % COMPANIES.length];
    expRows.push(`    (${sqlStr(exp1Id)}, ${sqlStr(cand.id)}, ${sqlStr(expComp1.name)}, ${sqlStr(`Thực tập sinh ${title}`)}, '2026-06-01', '2026-09-30', FALSE, ${sqlStr(`Tham gia phát triển tính năng thực tế trong dự án IOC, áp dụng quy trình Agile/Scrum và Code Review chuẩn doanh nghiệp.`)})`);
    if (idx < 20) {
      const exp2Id = detUuid('exp-2', cand.id);
      const expComp2 = COMPANIES[(idx + 3) % COMPANIES.length];
      expRows.push(`    (${sqlStr(exp2Id)}, ${sqlStr(cand.id)}, ${sqlStr(expComp2.name)}, ${sqlStr(`Cộng tác viên / Junior ${title}`)}, '2025-07-01', '2026-05-15', FALSE, 'Xây dựng module web/API, viết unit test và hỗ trợ bảo trì hệ thống production.')`);
    }

    // Skills & Evidences: 4-5 skills per candidate -> 180 skill_scores, ~220 evidences
    const candSkillCodes = TRACK_SKILLS[trackIdx].slice(0, idx % 2 === 0 ? 5 : 4);
    candSkillCodes.forEach((sc, sIdx) => {
      const verifiedScore = Math.min(98, Math.max(58, 72 + ((idx * 7 + sIdx * 9) % 25)));
      const selfScore = Math.min(100, verifiedScore + ((idx + sIdx) % 7) - 2);
      const level = levelFromScore(verifiedScore);
      skillScoreRows.push(`    (${sqlStr(cand.id)}, (SELECT id FROM skills WHERE code = ${sqlStr(sc)}), ${verifiedScore}, ${selfScore}, ${sqlStr(level)}, NOW() - interval '${(idx + sIdx) % 15} days')`);

      // LMS evidence for every skill (180)
      const evLmsId = detUuid('ev-lms', `${cand.id}-${sc}`);
      evidenceRows.push(`    (${sqlStr(evLmsId)}, ${sqlStr(cand.id)}, (SELECT id FROM skills WHERE code = ${sqlStr(sc)}), 'LMS', ${sqlStr(`LMS-${cand.lmsUserId.toUpperCase()}-${sc}`)}, ${sqlStr(`Điểm học phần ${sc} trên Rikkei LMS`)}, ${verifiedScore}, NOW() - interval '${20 - (sIdx * 2)} days', TRUE)`);

      // EXAM evidence for the first skill of every candidate (+40 -> 220 evidences)
      if (sIdx === 0) {
        const examScore = Math.min(100, verifiedScore + 3);
        const evExamId = detUuid('ev-exam', `${cand.id}-${sc}`);
        evidenceRows.push(`    (${sqlStr(evExamId)}, ${sqlStr(cand.id)}, (SELECT id FROM skills WHERE code = ${sqlStr(sc)}), 'EXAM', ${sqlStr(`EXAM-${cand.lmsUserId.toUpperCase()}-${sc}`)}, ${sqlStr(`Kết quả khảo thí chuẩn hóa ${sc}`)}, ${examScore}, NOW() - interval '${12 - (idx % 5)} days', TRUE)`);
      }
    });

    // Certificates: 28 total (first 28 candidates)
    if (idx < 28) {
      const [cName, cIssuer, cType] = certTemplates[idx % certTemplates.length];
      const certId = detUuid('cert', cand.id);
      certRows.push(`    (${sqlStr(certId)}, ${sqlStr(cand.id)}, ${sqlStr(cName)}, ${sqlStr(cIssuer)}, '2026-06-15', '2029-06-15', ${sqlStr(`https://credly.com/badges/${cand.lmsUserId}-${idx + 1}`)}, ${sqlStr(cType)})`);
    }

    // Internships & Tasks: 36 internships + 72 tasks
    if (idx < 36) {
      const intId = detUuid('internship', cand.id);
      const mentorEmail = MENTORS[idx % MENTORS.length].email;
      const finalScore = (7.8 + ((idx * 3) % 20) * 0.1).toFixed(2);
      internshipRows.push(`    (${sqlStr(intId)}, ${sqlStr(cand.id)}, ${sqlStr(`IOC-2026-B${(idx % 4) + 1}`)}, ${sqlStr(`Đợt thực tập IOC Q${(idx % 4) + 1}/2026`)}, ${sqlStr(`Nhóm ${(idx % 6) + 1}`)}, ${sqlStr(`Dự án thực tế ${title} - Rikkei Jobs`)}, ${sqlStr(mentorEmail)}, '2026-07-01', '2026-09-30', ${finalScore}, NOW() - interval '${idx % 10} days')`);

      taskRows.push(`    (${sqlStr(detUuid('int-task-1', cand.id))}, ${sqlStr(intId)}, ${sqlStr(`Phân tích & thiết kế kiến trúc module ${candSkillCodes[0]}`)}, ${(80 + (idx % 18)).toFixed(2)}, NOW() - interval '25 days')`);
      taskRows.push(`    (${sqlStr(detUuid('int-task-2', cand.id))}, ${sqlStr(intId)}, ${sqlStr(`Triển khai tính năng & viết Unit Test cho ${candSkillCodes[1]}`)}, ${(78 + ((idx * 3) % 20)).toFixed(2)}, NOW() - interval '15 days')`);
    }

    // Portfolio Projects: 40 total (1 per candidate)
    portfolioRows.push(`    (${sqlStr(detUuid('portfolio', cand.id))}, ${sqlStr(cand.id)}, 'IOC', ${sqlStr(`PRJ-${cand.lmsUserId.toUpperCase()}`)}, ${sqlStr(`Hệ thống ${title} Capstone #${idx + 1}`)}, ${sqlStr(title)}, NOW() - interval '${15 - (idx % 10)} days')`);

    // Profile Reviews: 32 rows (strictly respecting uq_profile_reviews_user_approve & uq_profile_reviews_user_reject)
    const staffActor = STAFFS[idx % STAFFS.length];
    if (visibility === 'PUBLIC' || visibility === 'RECRUITER') {
      reviewRows.push(`    (${sqlStr(detUuid('rev-submit', cand.id))}, ${sqlStr(cand.id)}, 'SUBMIT', 'Gửi hồ sơ công khai tìm việc', ${sqlStr(cand.id)}, NOW() - interval '12 days')`);
      reviewRows.push(`    (${sqlStr(detUuid('rev-approve', cand.id))}, ${sqlStr(cand.id)}, 'APPROVE', 'Hồ sơ đầy đủ thông tin và minh chứng xác thực', ${sqlStr(staffActor.id)}, NOW() - interval '10 days')`);
    } else if (visibility === 'PENDING') {
      reviewRows.push(`    (${sqlStr(detUuid('rev-submit', cand.id))}, ${sqlStr(cand.id)}, 'SUBMIT', 'Đề nghị duyệt hồ sơ công khai', ${sqlStr(cand.id)}, NOW() - interval '2 days')`);
    } else if (visibility === 'REJECTED') {
      reviewRows.push(`    (${sqlStr(detUuid('rev-submit', cand.id))}, ${sqlStr(cand.id)}, 'SUBMIT', 'Gửi duyệt hồ sơ', ${sqlStr(cand.id)}, NOW() - interval '5 days')`);
      reviewRows.push(`    (${sqlStr(detUuid('rev-reject', cand.id))}, ${sqlStr(cand.id)}, 'REJECT', 'Vui lòng bổ sung mô tả dự án và số điện thoại liên hệ hợp lệ', ${sqlStr(staffActor.id)}, NOW() - interval '3 days')`);
    }

    // Profile Badges: 35 granted badges
    if (idx < 35) {
      const bCode = badgeDefs[idx % badgeDefs.length][0];
      badgeGrantRows.push(`    (${sqlStr(detUuid('pbadge', cand.id))}, ${sqlStr(cand.id)}, (SELECT id FROM badge_rules WHERE code = ${sqlStr(bCode)}), 'AUTO', NOW() - interval '${idx % 15} days', NULL)`);
    }
  });

  lines.push(profileRows.join(',\n') + `\nON CONFLICT (user_id) DO UPDATE SET full_name = EXCLUDED.full_name, email = EXCLUDED.email, headline = EXCLUDED.headline, program = EXCLUDED.program, class_code = EXCLUDED.class_code, course_name = EXCLUDED.course_name, visibility = EXCLUDED.visibility, updated_at = NOW();`);
  lines.push('');

  lines.push(`-- 4. ${eduRows.length} Educations`);
  lines.push('INSERT INTO educations (id, user_id, school, major, from_date, to_date, gpa, note) VALUES');
  lines.push(eduRows.join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 5. ${expRows.length} Candidate Experiences`);
  lines.push('INSERT INTO candidate_experiences (id, user_id, company_name, position, from_date, to_date, is_current, description) VALUES');
  lines.push(expRows.join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 6. ${skillScoreRows.length} Skill Scores`);
  lines.push('INSERT INTO skill_scores (user_id, skill_id, verified_score, self_score, level, updated_at) VALUES');
  lines.push(skillScoreRows.join(',\n') + '\nON CONFLICT (user_id, skill_id) DO UPDATE SET verified_score = EXCLUDED.verified_score, self_score = EXCLUDED.self_score, level = EXCLUDED.level, updated_at = NOW();');
  lines.push('');

  lines.push(`-- 7. ${evidenceRows.length} Skill Evidences`);
  lines.push('INSERT INTO skill_evidences (id, user_id, skill_id, source, source_ref, title, score, recorded_at, verified) VALUES');
  lines.push(evidenceRows.join(',\n') + '\nON CONFLICT (user_id, skill_id, source, source_ref) DO NOTHING;');
  lines.push('');

  lines.push(`-- 8. ${certRows.length} Certificates`);
  lines.push('INSERT INTO certificates (id, user_id, name, issuer, issued_at, expires_at, file_url, file_type) VALUES');
  lines.push(certRows.join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 9. ${internshipRows.length} Internships & ${taskRows.length} Internship Tasks`);
  lines.push('INSERT INTO internships (id, user_id, batch_id, batch_name, group_name, topic_name, mentor_email, start_date, end_date, final_score, synced_at) VALUES');
  lines.push(internshipRows.join(',\n') + '\nON CONFLICT (user_id, batch_id) DO NOTHING;');
  lines.push('');
  lines.push('INSERT INTO internship_tasks (id, internship_id, title, accepted_score, accepted_at) VALUES');
  lines.push(taskRows.join(',\n') + '\nON CONFLICT (internship_id, title) DO NOTHING;');
  lines.push('');

  lines.push(`-- 10. ${portfolioRows.length} Portfolio Projects`);
  lines.push('INSERT INTO portfolio_projects (id, user_id, source, source_ref, name, role, created_at) VALUES');
  lines.push(portfolioRows.join(',\n') + '\nON CONFLICT (user_id, source, source_ref) DO NOTHING;');
  lines.push('');

  lines.push(`-- 11. ${reviewRows.length} Profile Reviews`);
  lines.push('INSERT INTO profile_reviews (id, user_id, action, reason, actor_id, created_at) VALUES');
  lines.push(reviewRows.join(',\n') + '\nON CONFLICT DO NOTHING;');
  lines.push('');

  lines.push(`-- 12. ${badgeGrantRows.length} Profile Badges`);
  lines.push('INSERT INTO profile_badges (id, user_id, badge_rule_id, granted_type, granted_at, revoked_at) VALUES');
  lines.push(badgeGrantRows.join(',\n') + '\nON CONFLICT (user_id, badge_rule_id) DO NOTHING;');
  lines.push('');

  return {
    sql: lines.join('\n'),
    counts: {
      skills: SKILLS.length,
      candidate_profiles: profileRows.length,
      educations: eduRows.length,
      candidate_experiences: expRows.length,
      skill_scores: skillScoreRows.length,
      skill_evidences: evidenceRows.length,
      certificates: certRows.length,
      internships: internshipRows.length,
      portfolio_projects: portfolioRows.length
    }
  };
}

// =============================================================================
// C. JOB DB SEED (03_seed_job_db.sql)
// 10 Companies, 12 JobCategories, 24 JobLocations, 120 Jobs (70/10/10/10), 360 JobSkills
// =============================================================================
const JOB_CATEGORIES = [
  { code: 'BACKEND', name: 'Backend Engineering' },
  { code: 'FRONTEND', name: 'Frontend Engineering' },
  { code: 'FULLSTACK', name: 'Fullstack Development' },
  { code: 'MOBILE', name: 'Mobile App Development' },
  { code: 'DEVOPS', name: 'DevOps & Cloud Infrastructure' },
  { code: 'QA_QC', name: 'QA / QC & Test Automation' },
  { code: 'DATA_AI', name: 'Data Science & AI' },
  { code: 'DATABASE', name: 'Database Engineering' },
  { code: 'SECURITY', name: 'Cyber Security' },
  { code: 'SYSTEM_ARCH', name: 'Solution & System Architecture' },
  { code: 'BA_PM', name: 'Business Analyst & Project Management' },
  { code: 'UI_UX', name: 'UI/UX Product Design' }
];

const JOB_LOCATIONS = [
  { code: 'HN-CG', city: 'Hà Nội', district: 'Cầu Giấy', country: 'Việt Nam' },
  { code: 'HN-NTL', city: 'Hà Nội', district: 'Nam Từ Liêm', country: 'Việt Nam' },
  { code: 'HN-TX', city: 'Hà Nội', district: 'Thanh Xuân', country: 'Việt Nam' },
  { code: 'HN-DD', city: 'Hà Nội', district: 'Đống Đa', country: 'Việt Nam' },
  { code: 'HN-HBT', city: 'Hà Nội', district: 'Hai Bà Trưng', country: 'Việt Nam' },
  { code: 'HN-HL', city: 'Hà Nội', district: 'Hòa Lạc Hi-Tech Park', country: 'Việt Nam' },
  { code: 'HCM-Q1', city: 'TP. Hồ Chí Minh', district: 'Quận 1', country: 'Việt Nam' },
  { code: 'HCM-Q3', city: 'TP. Hồ Chí Minh', district: 'Quận 3', country: 'Việt Nam' },
  { code: 'HCM-Q7', city: 'TP. Hồ Chí Minh', district: 'Quận 7', country: 'Việt Nam' },
  { code: 'HCM-TB', city: 'TP. Hồ Chí Minh', district: 'Tân Bình', country: 'Việt Nam' },
  { code: 'HCM-TD', city: 'TP. Hồ Chí Minh', district: 'TP. Thủ Đức', country: 'Việt Nam' },
  { code: 'HCM-BT', city: 'TP. Hồ Chí Minh', district: 'Bình Thạnh', country: 'Việt Nam' },
  { code: 'DN-HC', city: 'Đà Nẵng', district: 'Hải Châu', country: 'Việt Nam' },
  { code: 'DN-ST', city: 'Đà Nẵng', district: 'Sơn Trà', country: 'Việt Nam' },
  { code: 'DN-NHS', city: 'Đà Nẵng', district: 'Ngũ Hành Sơn', country: 'Việt Nam' },
  { code: 'HUE-TT', city: 'Huế', district: 'TP. Huế', country: 'Việt Nam' },
  { code: 'CT-NK', city: 'Cần Thơ', district: 'Ninh Kiều', country: 'Việt Nam' },
  { code: 'HP-HB', city: 'Hải Phòng', district: 'Hồng Bàng', country: 'Việt Nam' },
  { code: 'JP-TYO', city: 'Tokyo', district: 'Minato-ku', country: 'Nhật Bản' },
  { code: 'JP-OSA', city: 'Osaka', district: 'Kita-ku', country: 'Nhật Bản' },
  { code: 'JP-FUK', city: 'Fukuoka', district: 'Hakata-ku', country: 'Nhật Bản' },
  { code: 'SG-CBD', city: 'Singapore', district: 'Marina Bay', country: 'Singapore' },
  { code: 'VN-REMOTE', city: 'Remote', district: 'Toàn quốc', country: 'Việt Nam' },
  { code: 'VN-HYBRID', city: 'Hybrid', district: 'Hà Nội / TP.HCM / Đà Nẵng', country: 'Việt Nam' }
];

const JOBS = [];
const EXP_LEVELS = ['FRESHER', 'JUNIOR', 'MIDDLE', 'SENIOR'];
const JOB_TYPES = ['FULL_TIME', 'FULL_TIME', 'FULL_TIME', 'INTERNSHIP', 'PART_TIME', 'REMOTE'];

for (let i = 0; i < 120; i++) {
  const id = `d0000000-0000-4000-8000-${String(i + 1).padStart(12, '0')}`;
  const company = COMPANIES[i % COMPANIES.length];
  const trackIdx = i % 8;
  const cat = JOB_CATEGORIES[i % JOB_CATEGORIES.length];
  const loc = JOB_LOCATIONS[i % JOB_LOCATIONS.length];
  const level = EXP_LEVELS[i % EXP_LEVELS.length];
  const jobType = JOB_TYPES[i % JOB_TYPES.length];

  // Exact distribution: 84 APPROVED (70%), 12 PENDING (10%), 12 REJECTED (10%), 12 EXPIRED (10%)
  let status = 'APPROVED';
  if (i >= 84 && i < 96) status = 'PENDING';
  else if (i >= 96 && i < 108) status = 'REJECTED';
  else if (i >= 108) status = 'EXPIRED';

  const baseSalary = level === 'FRESHER' ? 12000000 : level === 'JUNIOR' ? 18000000 : level === 'MIDDLE' ? 28000000 : 42000000;
  const salaryMin = baseSalary;
  const salaryMax = baseSalary + (level === 'FRESHER' ? 6000000 : level === 'JUNIOR' ? 8000000 : 15000000);

  const title = `${TRACK_TITLES[trackIdx]} (${level}) #${i + 1}`;
  const skillCodes = TRACK_SKILLS[trackIdx].slice(0, 3);
  const hrOwner = HRS[i % HRS.length];

  JOBS.push({
    id,
    companyId: company.id,
    companyName: company.name,
    categoryCode: cat.code,
    locationCode: loc.code,
    locationLabel: `${loc.district}, ${loc.city}`,
    title,
    level,
    jobType,
    status,
    salaryMin,
    salaryMax,
    skillCodes,
    createdBy: hrOwner.id,
    expiresInDays: status === 'EXPIRED' ? -(5 + (i % 20)) : (20 + (i % 40))
  });
}

function generateJobSeed() {
  const lines = [];
  lines.push('-- =============================================================================');
  lines.push('-- 03_seed_job_db.sql : JOB SERVICE SEED DATA (job_db)');
  lines.push('-- 10 Companies (khớp UUID company_id trong clean_and_sync_users.sql)');
  lines.push('-- + 12 JobCategories + 24 JobLocations + 120 Jobs (70% APPROVED, 10% PENDING, 10% REJECTED, 10% EXPIRED) + 360 JobSkills');
  lines.push('-- =============================================================================');
  lines.push('');
  lines.push(`CREATE TABLE IF NOT EXISTS companies (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tax_code VARCHAR(13) NOT NULL UNIQUE,
    industry VARCHAR(255),
    size VARCHAR(20),
    address VARCHAR(500),
    city VARCHAR(100),
    website VARCHAR(255),
    logo_url VARCHAR(500),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_categories (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS job_locations (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(150),
    country VARCHAR(100) NOT NULL DEFAULT 'Việt Nam'
);

CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    category_id UUID REFERENCES job_categories(id),
    location_id UUID REFERENCES job_locations(id),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    requirements TEXT,
    experience_level VARCHAR(30) NOT NULL CHECK (experience_level IN ('FRESHER', 'JUNIOR', 'MIDDLE', 'SENIOR')),
    job_type VARCHAR(30) NOT NULL CHECK (job_type IN ('FULL_TIME', 'PART_TIME', 'INTERNSHIP', 'REMOTE')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'CLOSED')),
    salary_min NUMERIC(15,2),
    salary_max NUMERIC(15,2),
    work_location VARCHAR(255),
    expires_at TIMESTAMPTZ,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_jobs_company_id ON jobs(company_id);
CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status);

CREATE TABLE IF NOT EXISTS job_skills (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill_code VARCHAR(50) NOT NULL,
    skill_name VARCHAR(100) NOT NULL,
    min_score INT NOT NULL CHECK (min_score BETWEEN 0 AND 100),
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (job_id, skill_code)
);
CREATE INDEX IF NOT EXISTS idx_job_skills_job_id ON job_skills(job_id);`);
  lines.push('');

  // 1. 10 Companies
  lines.push('-- 1. 10 Companies');
  lines.push('INSERT INTO companies (id, name, tax_code, industry, size, address, city, website, logo_url, description, status, created_by, created_at, updated_at) VALUES');
  lines.push(COMPANIES.map((c, idx) =>
    `    (${sqlStr(c.id)}, ${sqlStr(c.name)}, ${sqlStr(c.taxCode)}, ${sqlStr(c.industry)}, ${sqlStr(c.size)}, ${sqlStr(c.address)}, ${sqlStr(c.city)}, ${sqlStr(c.website)}, ${sqlStr(`https://api.dicebear.com/7.x/initials/svg?seed=comp${idx + 1}`)}, ${sqlStr(c.desc)}, 'ACTIVE', ${sqlStr(STAFFS[idx % STAFFS.length].email)}, NOW() - interval '${60 - idx * 3} days', NOW())`
  ).join(',\n') + '\nON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, industry = EXCLUDED.industry, size = EXCLUDED.size, status = EXCLUDED.status, updated_at = NOW();');
  lines.push('');

  // 2. 12 Categories
  lines.push('-- 2. 12 Job Categories');
  lines.push('INSERT INTO job_categories (id, code, name) VALUES');
  lines.push(JOB_CATEGORIES.map(c =>
    `    (${sqlStr(detUuid('job_cat', c.code))}, ${sqlStr(c.code)}, ${sqlStr(c.name)})`
  ).join(',\n') + '\nON CONFLICT (code) DO NOTHING;');
  lines.push('');

  // 3. 24 Locations
  lines.push('-- 3. 24 Job Locations');
  lines.push('INSERT INTO job_locations (id, code, city, district, country) VALUES');
  lines.push(JOB_LOCATIONS.map(l =>
    `    (${sqlStr(detUuid('job_loc', l.code))}, ${sqlStr(l.code)}, ${sqlStr(l.city)}, ${sqlStr(l.district)}, ${sqlStr(l.country)})`
  ).join(',\n') + '\nON CONFLICT (code) DO NOTHING;');
  lines.push('');

  // 4. 120 Jobs
  lines.push('-- 4. 120 Jobs (84 APPROVED [70%], 12 PENDING [10%], 12 REJECTED [10%], 12 EXPIRED [10%])');
  lines.push('INSERT INTO jobs (id, company_id, category_id, location_id, title, description, requirements, experience_level, job_type, status, salary_min, salary_max, work_location, expires_at, created_by, created_at, updated_at) VALUES');
  lines.push(JOBS.map((j, idx) => {
    const expiresSql = j.expiresInDays < 0
      ? `NOW() - interval '${Math.abs(j.expiresInDays)} days'`
      : `NOW() + interval '${j.expiresInDays} days'`;
    const desc = `Cơ hội gia nhập ${j.companyName} ở vị trí ${j.title}. Tham gia phát triển các hệ thống quy mô lớn, môi trường Agile chuyên nghiệp, hỗ trợ đào tạo và thưởng dự án hấp dẫn.`;
    const req = `Thành thạo ${j.skillCodes.join(', ')}. Cấp độ mong muốn: ${j.level}. Tư duy giải quyết vấn đề tốt và chủ động trong công việc.`;
    return `    (${sqlStr(j.id)}, ${sqlStr(j.companyId)}, ${sqlStr(detUuid('job_cat', j.categoryCode))}, ${sqlStr(detUuid('job_loc', j.locationCode))}, ${sqlStr(j.title)}, ${sqlStr(desc)}, ${sqlStr(req)}, ${sqlStr(j.level)}, ${sqlStr(j.jobType)}, ${sqlStr(j.status)}, ${j.salaryMin}, ${j.salaryMax}, ${sqlStr(j.locationLabel)}, ${expiresSql}, ${sqlStr(j.createdBy)}, NOW() - interval '${30 - (idx % 25)} days', NOW())`;
  }).join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');

  // 5. 360 Job Skills
  const skillMap = Object.fromEntries(SKILLS.map(s => [s.code, s.name]));
  const jobSkillRows = [];
  JOBS.forEach(j => {
    const baseMin = j.level === 'FRESHER' ? 65 : j.level === 'JUNIOR' ? 70 : j.level === 'MIDDLE' ? 80 : 85;
    j.skillCodes.forEach((sc, idx) => {
      jobSkillRows.push(`    (${sqlStr(detUuid('job_skill', `${j.id}-${sc}`))}, ${sqlStr(j.id)}, ${sqlStr(sc)}, ${sqlStr(skillMap[sc] || sc)}, ${baseMin - idx * 5}, ${idx < 2 ? 'TRUE' : 'FALSE'})`);
    });
  });
  lines.push(`-- 5. ${jobSkillRows.length} Job Skills`);
  lines.push('INSERT INTO job_skills (id, job_id, skill_code, skill_name, min_score, is_mandatory) VALUES');
  lines.push(jobSkillRows.join(',\n') + '\nON CONFLICT (job_id, skill_code) DO NOTHING;');
  lines.push('');

  return {
    sql: lines.join('\n'),
    counts: {
      companies: COMPANIES.length,
      job_categories: JOB_CATEGORIES.length,
      job_locations: JOB_LOCATIONS.length,
      jobs: JOBS.length,
      job_skills: jobSkillRows.length
    }
  };
}

// =============================================================================
// D. RECRUITMENT DB SEED (04_seed_recruitment_db.sql)
// Matches Recruitment.java (status SMALLINT ordinal 0..6!), RecruitmentStatusHistory.java,
// InterviewRound.java (result VARCHAR 'PENDING'|'PASS'|'FAIL'), + offers & employment_records!
// =============================================================================
function generateRecruitmentSeed() {
  const approvedJobs = JOBS.filter(j => j.status === 'APPROVED'); // 84 jobs
  const lines = [];
  lines.push('-- =============================================================================');
  lines.push('-- 04_seed_recruitment_db.sql : RECRUITMENT SERVICE SEED DATA (recruitment_db)');
  lines.push('-- Khớp 100% Recruitment.java (status SMALLINT ordinal 0..6), InterviewRound.java,');
  lines.push('-- RecruitmentStatusHistory.java + UT-07 offers & employment_records');
  lines.push('-- Ordinal RecruitmentStatus: 0=SUBMITTED, 1=SCREENING, 2=INTERVIEW, 3=OFFER, 4=HIRED, 5=REJECTED, 6=WITHDRAWN');
  lines.push('-- =============================================================================');
  lines.push('');
  lines.push(`CREATE TABLE IF NOT EXISTS recruitments (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    company_id UUID,
    user_id UUID NOT NULL,
    cv_id UUID,
    cv_snapshot_url VARCHAR(500),
    cover_letter TEXT,
    candidate_email VARCHAR(255),
    status SMALLINT NOT NULL CHECK (status BETWEEN 0 AND 6),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_recruitments_user ON recruitments(user_id);
CREATE INDEX IF NOT EXISTS idx_recruitments_company ON recruitments(company_id);
CREATE INDEX IF NOT EXISTS idx_recruitments_job ON recruitments(job_id);

CREATE TABLE IF NOT EXISTS recruitment_status_history (
    id UUID PRIMARY KEY,
    recruitment_id UUID NOT NULL,
    from_status SMALLINT NOT NULL CHECK (from_status BETWEEN 0 AND 6),
    to_status SMALLINT NOT NULL CHECK (to_status BETWEEN 0 AND 6),
    actor_id UUID NOT NULL,
    note TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_recruitment_history_rec ON recruitment_status_history(recruitment_id);

CREATE TABLE IF NOT EXISTS interview_rounds (
    id UUID PRIMARY KEY,
    recruitment_id UUID NOT NULL REFERENCES recruitments(id) ON DELETE CASCADE,
    round_no INTEGER NOT NULL,
    scheduled_at TIMESTAMP WITHOUT TIME ZONE,
    result VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (result IN ('PENDING', 'PASS', 'FAIL')),
    score INTEGER CHECK (score IS NULL OR (score >= 0 AND score <= 100)),
    UNIQUE (recruitment_id, round_no)
);

CREATE TABLE IF NOT EXISTS interview_feedbacks (
    id UUID PRIMARY KEY,
    interview_round_id UUID NOT NULL REFERENCES interview_rounds(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL,
    technical_score SMALLINT CHECK (technical_score BETWEEN 0 AND 100),
    communication_score SMALLINT CHECK (communication_score BETWEEN 0 AND 100),
    comment TEXT NOT NULL,
    recommendation VARCHAR(20) NOT NULL CHECK (recommendation IN ('STRONG_HIRE', 'HIRE', 'NO_HIRE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS offers (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL UNIQUE REFERENCES recruitments(id) ON DELETE CASCADE,
    company_name VARCHAR(255),
    position VARCHAR(255) NOT NULL,
    salary NUMERIC(15,2) NOT NULL CHECK (salary > 0),
    start_date DATE NOT NULL,
    work_location VARCHAR(500) NOT NULL,
    note TEXT,
    response_deadline TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SENT', 'ACCEPTED', 'DECLINED', 'EXPIRED')),
    responded_at TIMESTAMPTZ,
    decline_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS partner_accounts (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS shortlists (
    company_id UUID NOT NULL,
    user_id UUID NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (company_id, user_id)
);

CREATE TABLE IF NOT EXISTS employment_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    company_id UUID NOT NULL,
    company_name VARCHAR(255),
    job_id UUID NOT NULL,
    application_id UUID UNIQUE REFERENCES recruitments(id) ON DELETE SET NULL,
    position VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_employment_records_user ON employment_records(user_id);`);
  lines.push('');

  // Status distribution for 180 applications:
  // 0..29   (30): SUBMITTED (0)
  // 30..59  (30): SCREENING (1)
  // 60..94  (35): INTERVIEW (2)
  // 95..104 (10): OFFER (3)      -> 8 SENT + 2 SENT/soon-expiring
  // 105..112(8):  HIRED (4)      -> 8 ACCEPTED offers + 8 employment_records
  // 113..159(47): REJECTED (5)   -> includes 2 DECLINED offers + 2 EXPIRED offers + 43 regular rejections
  // 160..179(20): WITHDRAWN (6)
  // Total = 180 applications!
  // Offers = 10 (OFFER/SENT) + 8 (HIRED/ACCEPTED) + 2 (DECLINED) + 2 (EXPIRED) = 22? Let's keep Offers = 18 (within 10-20!):
  // 95..102 (8): OFFER (3) -> 8 SENT
  // 103..108 (6): HIRED (4) -> 6 ACCEPTED + 6 employment_records
  // 109..110 (2): REJECTED (5) via DECLINED offer
  // 111..112 (2): REJECTED (5) via EXPIRED offer
  // 113..159 (47): REJECTED (5) normal
  // 160..179 (20): WITHDRAWN (6)

  const recRows = [];
  const histRows = [];
  const roundRows = [];
  const fbRows = [];
  const offerRows = [];
  const empRows = [];

  for (let i = 0; i < 180; i++) {
    const recId = detUuid('recruitment', `app-${i + 1}`);
    const cand = CANDIDATES[i % CANDIDATES.length];
    // Pick unique (user, job) pair deterministically across 84 approved jobs
    const job = approvedJobs[(i * 7 + Math.floor(i / 40) * 13) % approvedJobs.length];
    const hrActor = HRS.find(h => h.companyId === job.companyId) || HRS[i % HRS.length];
    const mentorActor = MENTORS[i % MENTORS.length];
    const cvId = detUuid('cv', cand.id);
    const cvUrl = `/recruitments/${recId}/cv-snapshot`;

    let finalStatus;
    let pathSteps = []; // list of [from, to, actorId, note, daysAgo]
    if (i < 30) {
      finalStatus = 0; // SUBMITTED
      pathSteps = [[0, 0, cand.id, 'Ứng viên nộp hồ sơ ứng tuyển', 20 - (i % 10)]];
    } else if (i < 60) {
      finalStatus = 1; // SCREENING
      pathSteps = [
        [0, 0, cand.id, 'Ứng viên nộp hồ sơ ứng tuyển', 22],
        [0, 1, hrActor.id, 'HR tiếp nhận và bắt đầu sàng lọc CV', 18]
      ];
    } else if (i < 95) {
      finalStatus = 2; // INTERVIEW (35 apps)
      pathSteps = [
        [0, 0, cand.id, 'Ứng viên nộp hồ sơ ứng tuyển', 24],
        [0, 1, hrActor.id, 'Hồ sơ đạt yêu cầu sàng lọc', 20],
        [1, 2, hrActor.id, 'Mời ứng viên tham gia phỏng vấn kỹ thuật', 14]
      ];
    } else if (i < 103) {
      finalStatus = 3; // OFFER (8 apps -> SENT)
      pathSteps = [
        [0, 0, cand.id, 'Ứng viên nộp hồ sơ ứng tuyển', 26],
        [0, 1, hrActor.id, 'Đạt vòng sàng lọc CV', 22],
        [1, 2, hrActor.id, 'Chuyển sang vòng phỏng vấn', 17],
        [2, 3, hrActor.id, 'Đạt vòng phỏng vấn cuối, HR gửi thư mời nhận việc (Offer)', 3]
      ];
    } else if (i < 109) {
      finalStatus = 4; // HIRED (6 apps -> ACCEPTED)
      pathSteps = [
        [0, 0, cand.id, 'Ứng viên nộp hồ sơ ứng tuyển', 28],
        [0, 1, hrActor.id, 'Đạt vòng sàng lọc CV', 25],
        [1, 2, hrActor.id, 'Chuyển sang vòng phỏng vấn', 20],
        [2, 3, hrActor.id, 'Đạt phỏng vấn, gửi Offer', 10],
        [3, 4, cand.id, 'Ứng viên chấp nhận Offer (HIRED)', 6]
      ];
    } else if (i < 111) {
      finalStatus = 5; // REJECTED via DECLINED offer (2 apps)
      pathSteps = [
        [0, 0, cand.id, 'Ứng viên nộp hồ sơ ứng tuyển', 27],
        [0, 1, hrActor.id, 'Đạt vòng sàng lọc', 23],
        [1, 2, hrActor.id, 'Mời phỏng vấn', 18],
        [2, 3, hrActor.id, 'Gửi Offer sau khi PASS phỏng vấn', 9],
        [3, 5, cand.id, 'Ứng viên từ chối Offer', 5]
      ];
    } else if (i < 113) {
      finalStatus = 5; // REJECTED via EXPIRED offer (2 apps)
      pathSteps = [
        [0, 0, cand.id, 'Ứng viên nộp hồ sơ ứng tuyển', 27],
        [0, 1, hrActor.id, 'Đạt vòng sàng lọc', 23],
        [1, 2, hrActor.id, 'Mời phỏng vấn', 18],
        [2, 3, hrActor.id, 'Gửi Offer sau khi PASS phỏng vấn', 11],
        [3, 5, hrActor.id, 'Offer hết hạn phản hồi (EXPIRED)', 2]
      ];
    } else if (i < 160) {
      finalStatus = 5; // REJECTED normal (47 apps)
      pathSteps = [
        [0, 0, cand.id, 'Ứng viên nộp hồ sơ ứng tuyển', 25],
        [0, 1, hrActor.id, 'HR sàng lọc hồ sơ', 21],
        [1, 5, hrActor.id, 'Kỹ năng hiện tại chưa phù hợp yêu cầu vị trí', 16]
      ];
    } else {
      finalStatus = 6; // WITHDRAWN (20 apps)
      pathSteps = [
        [0, 0, cand.id, 'Ứng viên nộp hồ sơ ứng tuyển', 20],
        [0, 6, cand.id, 'Ứng viên chủ động rút hồ sơ để tập trung vị trí khác', 12]
      ];
    }

    const coverLetter = `Kính gửi Bộ phận Tuyển dụng ${job.companyName}, tôi là ${cand.fullName} mong muốn ứng tuyển vị trí ${job.title}. Tôi đã hoàn thành chương trình đào tạo tại Rikkei Academy và sẵn sàng đóng góp cho đội ngũ.`;
    recRows.push(`    (${sqlStr(recId)}, ${sqlStr(job.id)}, ${sqlStr(job.companyId)}, ${sqlStr(cand.id)}, ${sqlStr(cvId)}, ${sqlStr(cvUrl)}, ${sqlStr(coverLetter)}, ${sqlStr(cand.email)}, ${finalStatus}, NOW() - interval '${28 - (i % 15)} days', NOW() - interval '${i % 5} days')`);

    pathSteps.forEach(([fromSt, toSt, actorId, note, daysAgo], hIdx) => {
      histRows.push(`    (${sqlStr(detUuid('rec_hist', `${recId}-${hIdx}`))}, ${sqlStr(recId)}, ${fromSt}, ${toSt}, ${sqlStr(actorId)}, ${sqlStr(note)}, NOW() - interval '${daysAgo} days')`);
    });

    // Interview rounds (~45 rounds across selected INTERVIEW + all 18 OFFER/HIRED/DECLINED/EXPIRED)
    const hasOffer = i >= 95 && i < 113;
    const isInterviewWithRound = (i >= 60 && i < 84); // 24 interview apps + 18 offer apps = 42 apps -> ~46 rounds
    if (isInterviewWithRound || hasOffer) {
      const round1Id = detUuid('round', `${recId}-1`);
      let r1Result = 'PASS';
      let r1Score = 78 + (i % 20);
      if (i >= 76 && i < 84) {
        r1Result = 'PENDING';
        r1Score = null;
      } else if (i >= 72 && i < 76) {
        r1Result = 'FAIL';
        r1Score = 45 + (i % 15);
      }
      roundRows.push(`    (${sqlStr(round1Id)}, ${sqlStr(recId)}, 1, NOW() - interval '${12 - (i % 8)} days', ${sqlStr(r1Result)}, ${r1Score === null ? 'NULL' : r1Score})`);

      // Feedback for completed rounds (~34 feedbacks)
      if (r1Result !== 'PENDING') {
        const recVal = r1Result === 'PASS' ? (r1Score >= 88 ? 'STRONG_HIRE' : 'HIRE') : 'NO_HIRE';
        const fbComment = r1Result === 'PASS'
          ? `Ứng viên ${cand.fullName} nắm chắc kiến thức nền tảng, giải quyết bài toán thực tế mạch lạc và giao tiếp tự tin.`
          : `Ứng viên cần ôn luyện thêm kiến thức hệ thống và tối ưu truy vấn trước khi ứng tuyển lại.`;
        fbRows.push(`    (${sqlStr(detUuid('round_fb', `${recId}-1`))}, ${sqlStr(round1Id)}, ${sqlStr(mentorActor.id)}, ${r1Score}, ${Math.min(100, (r1Score || 65) + 5)}, ${sqlStr(fbComment)}, ${sqlStr(recVal)}, NOW() - interval '${10 - (i % 6)} days')`);
      }

      // Add round 2 for first 4 HIRED candidates -> total rounds = 46
      if (i >= 103 && i < 107) {
        const round2Id = detUuid('round', `${recId}-2`);
        const r2Score = 88 + (i % 10);
        roundRows.push(`    (${sqlStr(round2Id)}, ${sqlStr(recId)}, 2, NOW() - interval '12 days', 'PASS', ${r2Score})`);
      }
    }

    // Offers (18 total: 8 SENT, 6 ACCEPTED, 2 DECLINED, 2 EXPIRED)
    if (hasOffer) {
      const offerId = detUuid('offer', recId);
      let offerStatus = 'SENT';
      let deadlineSql = `NOW() + interval '${3 + (i % 5)} days'`;
      let respondedSql = 'NULL';
      let declineReasonSql = 'NULL';

      if (i >= 103 && i < 109) {
        offerStatus = 'ACCEPTED';
        deadlineSql = `NOW() + interval '5 days'`;
        respondedSql = `NOW() - interval '6 days'`;
      } else if (i >= 109 && i < 111) {
        offerStatus = 'DECLINED';
        deadlineSql = `NOW() + interval '2 days'`;
        respondedSql = `NOW() - interval '5 days'`;
        declineReasonSql = sqlStr(i === 109 ? 'Ứng viên đã nhận đề nghị làm việc gần nhà hơn' : 'Chưa thống nhất được thời gian bắt đầu làm việc');
      } else if (i >= 111 && i < 113) {
        offerStatus = 'EXPIRED';
        deadlineSql = `NOW() - interval '${2 + (i % 3)} days'`;
      }

      const offerSalary = job.salaryMin + 2000000;
      offerRows.push(`    (${sqlStr(offerId)}, ${sqlStr(recId)}, ${sqlStr(job.companyName)}, ${sqlStr(job.title)}, ${offerSalary}, '2026-12-01', ${sqlStr(job.locationLabel)}, 'Chào mừng bạn gia nhập đội ngũ kỹ thuật của công ty!', ${deadlineSql}, ${sqlStr(offerStatus)}, ${respondedSql}, ${declineReasonSql}, NOW() - interval '9 days', NOW() - interval '2 days')`);

      // Employment records for all 6 ACCEPTED offers
      if (offerStatus === 'ACCEPTED') {
        const empId = detUuid('emp_rec', recId);
        const confirmedSql = i < 107 ? `NOW() - interval '1 day'` : 'NULL';
        empRows.push(`    (${sqlStr(empId)}, ${sqlStr(cand.id)}, ${sqlStr(job.companyId)}, ${sqlStr(job.companyName)}, ${sqlStr(job.id)}, ${sqlStr(recId)}, ${sqlStr(job.title)}, '2026-12-01', ${confirmedSql}, NOW() - interval '6 days')`);
      }
    }
  }

  lines.push(`-- 1. ${recRows.length} Recruitments (Applications)`);
  lines.push('INSERT INTO recruitments (id, job_id, company_id, user_id, cv_id, cv_snapshot_url, cover_letter, candidate_email, status, created_at, updated_at) VALUES');
  lines.push(recRows.join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 2. ${histRows.length} Recruitment Status History Records`);
  lines.push('INSERT INTO recruitment_status_history (id, recruitment_id, from_status, to_status, actor_id, note, created_at) VALUES');
  lines.push(histRows.join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 3. ${roundRows.length} Interview Rounds`);
  lines.push('INSERT INTO interview_rounds (id, recruitment_id, round_no, scheduled_at, result, score) VALUES');
  lines.push(roundRows.join(',\n') + '\nON CONFLICT (recruitment_id, round_no) DO NOTHING;');
  lines.push('');

  lines.push(`-- 4. ${fbRows.length} Interview Feedbacks`);
  lines.push('INSERT INTO interview_feedbacks (id, interview_round_id, reviewer_id, technical_score, communication_score, comment, recommendation, created_at) VALUES');
  lines.push(fbRows.join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 5. ${offerRows.length} Offers (8 SENT, 6 ACCEPTED, 2 DECLINED, 2 EXPIRED)`);
  lines.push('INSERT INTO offers (id, application_id, company_name, position, salary, start_date, work_location, note, response_deadline, status, responded_at, decline_reason, created_at, updated_at) VALUES');
  lines.push(offerRows.join(',\n') + '\nON CONFLICT (application_id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 6. ${empRows.length} Employment Records`);
  lines.push('INSERT INTO employment_records (id, user_id, company_id, company_name, job_id, application_id, position, start_date, confirmed_at, created_at) VALUES');
  lines.push(empRows.join(',\n') + '\nON CONFLICT (application_id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 7. ${HRS.length} Partner Accounts (khớp 100% HR users từ clean_and_sync_users.sql) & 30 Shortlists`);
  lines.push('INSERT INTO partner_accounts (id, company_id, email, password_hash, full_name, must_change_password, created_at) VALUES');
  lines.push(HRS.map(h =>
    `    (${sqlStr(h.id)}, ${sqlStr(h.companyId || COMPANIES[0].id)}, ${sqlStr(h.email)}, ${sqlStr(h.passwordHash)}, ${sqlStr(h.fullName)}, FALSE, NOW() - interval '30 days')`
  ).join(',\n') + '\nON CONFLICT (email) DO NOTHING;');
  lines.push('');

  const shortlistRows = [];
  for (let s = 0; s < 30; s++) {
    const comp = COMPANIES[s % 5];
    const cand = CANDIDATES[s];
    shortlistRows.push(`    (${sqlStr(comp.id)}, ${sqlStr(cand.id)}, ${sqlStr(`Ứng viên tiềm năng ${cand.fullName} (${cand.lmsUserId})`)}, NOW() - interval '${s % 15} days')`);
  }
  lines.push('INSERT INTO shortlists (company_id, user_id, note, created_at) VALUES');
  lines.push(shortlistRows.join(',\n') + '\nON CONFLICT (company_id, user_id) DO NOTHING;');
  lines.push('');

  return {
    sql: lines.join('\n'),
    counts: {
      recruitments: recRows.length,
      recruitment_status_history: histRows.length,
      interview_rounds: roundRows.length,
      interview_feedbacks: fbRows.length,
      offers: offerRows.length,
      employment_records: empRows.length
    }
  };
}

// =============================================================================
// E. LEARNING DB SEED (05_seed_learning_db.sql)
// 6 Categories, 18 Courses (5 BE, 5 FE, 3 DevOps, 3 DB, 2 Soft Skills),
// 54 Modules, 135 Lessons, 68 Enrollments, 150 LearningProgress, 25 Reviews,
// + 40 CandidateTargets & 120 GapResults
// =============================================================================
const COURSE_CATEGORIES = [
  { code: 'BACKEND', name: 'Lập trình Backend' },
  { code: 'FRONTEND', name: 'Lập trình Frontend' },
  { code: 'DEVOPS', name: 'DevOps & Cloud' },
  { code: 'DATABASE', name: 'Cơ sở dữ liệu' },
  { code: 'SOFT_SKILLS', name: 'Kỹ năng mềm & Giao tiếp' },
  { code: 'QA_DATA', name: 'Kiểm thử & Dữ liệu' }
];

const COURSES = [
  // 5 Backend
  { id: '22222222-2222-2222-2222-222222222201', code: 'CRS-01', cat: 'BACKEND', name: 'Lập trình Spring Boot nâng cao & Microservices', level: 'Tốt', skills: [['SPRING_BOOT', 0.45], ['MICROSERVICES', 0.35], ['JAVA_CORE', 0.20]] },
  { id: '22222222-2222-2222-2222-222222222207', code: 'CRS-07', cat: 'BACKEND', name: 'Java Core & Lập trình Hướng đối tượng thực chiến', level: 'Khá', skills: [['JAVA_CORE', 0.60], ['DESIGN_PATTERNS', 0.40]] },
  { id: '22222222-2222-2222-2222-222222222208', code: 'CRS-08', cat: 'BACKEND', name: 'Xây dựng RESTful API chuẩn Enterprise với Node.js', level: 'Khá', skills: [['NODEJS', 0.55], ['REST_API', 0.45]] },
  { id: '22222222-2222-2222-2222-222222222209', code: 'CRS-09', cat: 'BACKEND', name: 'Lập trình Golang & gRPC Hiệu năng cao', level: 'Tốt', skills: [['GOLANG', 0.60], ['GRPC', 0.40]] },
  { id: '22222222-2222-2222-2222-222222222210', code: 'CRS-10', cat: 'BACKEND', name: 'Kiến trúc Hệ thống Phân tán & Apache Kafka', level: 'Tốt', skills: [['APACHE_KAFKA', 0.50], ['SYSTEM_DESIGN', 0.50]] },
  // 5 Frontend
  { id: '22222222-2222-2222-2222-222222222202', code: 'CRS-02', cat: 'FRONTEND', name: 'React 19 & Next.js Fullstack Architecture', level: 'Tốt', skills: [['REACT', 0.45], ['NEXTJS', 0.30], ['TYPESCRIPT', 0.25]] },
  { id: '22222222-2222-2222-2222-222222222211', code: 'CRS-11', cat: 'FRONTEND', name: 'HTML5, CSS3 & Tailwind CSS Chuyên sâu', level: 'Cơ bản', skills: [['HTML_CSS', 0.50], ['TAILWIND_CSS', 0.50]] },
  { id: '22222222-2222-2222-2222-222222222212', code: 'CRS-12', cat: 'FRONTEND', name: 'JavaScript ES6+ & Lập trình Bất đồng bộ', level: 'Khá', skills: [['JAVASCRIPT', 0.70], ['TYPESCRIPT', 0.30]] },
  { id: '22222222-2222-2222-2222-222222222213', code: 'CRS-13', cat: 'FRONTEND', name: 'TypeScript Nâng cao & Design System cho Web', level: 'Tốt', skills: [['TYPESCRIPT', 0.60], ['REACT', 0.40]] },
  { id: '22222222-2222-2222-2222-222222222214', code: 'CRS-14', cat: 'FRONTEND', name: 'Vue 3 & Pinia State Management Hiện đại', level: 'Khá', skills: [['VUEJS', 0.65], ['JAVASCRIPT', 0.35]] },
  // 3 DevOps
  { id: '22222222-2222-2222-2222-222222222204', code: 'CRS-04', cat: 'DEVOPS', name: 'DevOps cơ bản: Docker, CI/CD và Linux', level: 'Khá', skills: [['DOCKER', 0.45], ['CI_CD', 0.35], ['LINUX_BASH', 0.20]] },
  { id: '22222222-2222-2222-2222-222222222215', code: 'CRS-15', cat: 'DEVOPS', name: 'Kubernetes Production & Helm Charts', level: 'Tốt', skills: [['KUBERNETES', 0.65], ['DOCKER', 0.35]] },
  { id: '22222222-2222-2222-2222-222222222216', code: 'CRS-16', cat: 'DEVOPS', name: 'AWS Cloud Architecture & Terraform IaC', level: 'Tốt', skills: [['AWS', 0.60], ['TERRAFORM', 0.40]] },
  // 3 Database
  { id: '22222222-2222-2222-2222-222222222203', code: 'CRS-03', cat: 'DATABASE', name: 'Tối ưu hóa CSDL quan hệ PostgreSQL', level: 'Tốt', skills: [['SQL', 0.45], ['POSTGRESQL', 0.35], ['DATABASE_OPTIMIZATION', 0.20]] },
  { id: '22222222-2222-2222-2222-222222222217', code: 'CRS-17', cat: 'DATABASE', name: 'Thiết kế CSDL MySQL & Truy vấn SQL Nâng cao', level: 'Khá', skills: [['MYSQL', 0.55], ['SQL', 0.45]] },
  { id: '22222222-2222-2222-2222-222222222218', code: 'CRS-18', cat: 'DATABASE', name: 'NoSQL với MongoDB & Caching tốc độ cao Redis', level: 'Khá', skills: [['MONGODB', 0.50], ['REDIS', 0.50]] },
  // 2 Soft Skills / Testing
  { id: '22222222-2222-2222-2222-222222222205', code: 'CRS-05', cat: 'QA_DATA', name: 'Kiểm thử tự động & Chất lượng phần mềm', level: 'Tốt', skills: [['TESTING', 0.60], ['SELENIUM', 0.40]] },
  { id: '22222222-2222-2222-2222-222222222219', code: 'CRS-19', cat: 'SOFT_SKILLS', name: 'Kỹ năng Giao tiếp, Làm việc nhóm & Phỏng vấn IT', level: 'Cơ bản', skills: [['COMMUNICATION', 0.45], ['TEAMWORK', 0.35], ['PRESENTATION', 0.20]] }
];

function generateLearningSeed() {
  const v1Learn = fs.readFileSync(path.join(ROOT, 'backend', 'learning-service', 'src', 'main', 'resources', 'db', 'migration', 'V1__init_learning_gap.sql'), 'utf8');
  const v2Learn = fs.readFileSync(path.join(ROOT, 'backend', 'learning-service', 'src', 'main', 'resources', 'db', 'migration', 'V2__init_courses_and_skill_map.sql'), 'utf8');
  const v3Learn = fs.readFileSync(path.join(ROOT, 'backend', 'learning-service', 'src', 'main', 'resources', 'db', 'migration', 'V3__init_dt02_enrollments_and_mappings.sql'), 'utf8');
  const lines = [];
  lines.push('-- =============================================================================');
  lines.push('-- 05_seed_learning_db.sql : LEARNING SERVICE SEED DATA (learning_db)');
  lines.push('-- Khớp 100% V1..V3 migrations + 18 khóa học (5 BE, 5 FE, 3 DevOps, 3 DB, 2 Bổ trợ)');
  lines.push('-- =============================================================================');
  lines.push('CREATE EXTENSION IF NOT EXISTS pgcrypto;');
  lines.push(v1Learn.trim());
  lines.push('');
  lines.push(v2Learn.trim());
  lines.push('');
  lines.push(v3Learn.trim());
  lines.push('');
  lines.push(`CREATE TABLE IF NOT EXISTS course_categories (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL
);

CREATE TABLE IF NOT EXISTS course_modules (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    order_index INT NOT NULL,
    UNIQUE (course_id, order_index)
);

CREATE TABLE IF NOT EXISTS course_lessons (
    id UUID PRIMARY KEY,
    module_id UUID NOT NULL REFERENCES course_modules(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 30,
    order_index INT NOT NULL,
    UNIQUE (module_id, order_index)
);

CREATE TABLE IF NOT EXISTS learning_progress (
    id UUID PRIMARY KEY,
    enrollment_id UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    lesson_id UUID NOT NULL REFERENCES course_lessons(id) ON DELETE CASCADE,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    watched_seconds INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (enrollment_id, lesson_id)
);

CREATE TABLE IF NOT EXISTS course_reviews (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (course_id, user_id)
);`);
  lines.push('');

  lines.push('-- 1. 6 Course Categories');
  lines.push('INSERT INTO course_categories (id, code, name) VALUES');
  lines.push(COURSE_CATEGORIES.map(c => `    (${sqlStr(detUuid('course_cat', c.code))}, ${sqlStr(c.code)}, ${sqlStr(c.name)})`).join(',\n') + '\nON CONFLICT (code) DO NOTHING;');
  lines.push('');

  lines.push(`-- 2. ${COURSES.length} Courses`);
  lines.push('INSERT INTO courses (id, code, name, description, level, created_at) VALUES');
  lines.push(COURSES.map(c =>
    `    (${sqlStr(c.id)}, ${sqlStr(c.code)}, ${sqlStr(c.name)}, ${sqlStr(`Khóa học chuyên sâu thuộc nhóm ${c.cat}, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.`)}, ${sqlStr(c.level)}, NOW() - interval '30 days')`
  ).join(',\n') + '\nON CONFLICT (code) DO NOTHING;');
  lines.push('');

  const csmRows = [];
  const scmRows = [];
  const modRows = [];
  const lesRows = [];
  const courseLessonMap = {};

  COURSES.forEach(c => {
    c.skills.forEach(([sk, w]) => {
      csmRows.push(`    (${sqlStr(c.id)}, ${sqlStr(sk)}, ${w.toFixed(2)})`);
      scmRows.push(`    (${sqlStr(sk)}, ${sqlStr(c.code)}, ${sqlStr(c.name)}, ${sqlStr(`Lộ trình học thực chiến bổ sung kỹ năng ${sk}`)}, 0, 80)`);
    });
    courseLessonMap[c.code] = [];
    // 3 Modules/course = 54 modules; 2-3 lessons/module = 135 lessons
    for (let m = 1; m <= 3; m++) {
      const modId = detUuid('course_mod', `${c.code}-M${m}`);
      const modTitle = m === 1 ? `Phần 1: Nền tảng & Kiến trúc ${c.code}` : m === 2 ? `Phần 2: Thực hành Dự án & Best Practices` : `Phần 3: Tối ưu hóa & Bài tập Tổng hợp`;
      modRows.push(`    (${sqlStr(modId)}, ${sqlStr(c.id)}, ${sqlStr(modTitle)}, ${m})`);
      const lessonCount = m === 3 ? 2 : 3; // 3+3+2 = 8? 18*7.5 = 135 -> m=1:3, m=2:3, m=3:2 (144, within 100-150!)
      for (let l = 1; l <= lessonCount; l++) {
        const lesId = detUuid('course_les', `${c.code}-M${m}-L${l}`);
        courseLessonMap[c.code].push(lesId);
        lesRows.push(`    (${sqlStr(lesId)}, ${sqlStr(modId)}, ${sqlStr(`Bài ${m}.${l}: Thực chiến ${c.name.slice(0, 45)}`)}, ${20 + ((m * l * 7) % 35)}, ${l})`);
      }
    }
  });

  lines.push(`-- 3. ${csmRows.length} Course Skill Maps & ${scmRows.length} Skill Course Mappings`);
  lines.push('INSERT INTO course_skill_map (course_id, skill_id, weight) VALUES');
  lines.push(csmRows.join(',\n') + '\nON CONFLICT (course_id, skill_id) DO NOTHING;');
  lines.push('');
  lines.push('INSERT INTO skill_course_mappings (skill_id, lms_course_id, course_name, lesson_hint, level_from, level_to) VALUES');
  lines.push(scmRows.join(',\n') + '\nON CONFLICT (skill_id, lms_course_id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 4. ${modRows.length} Modules & ${lesRows.length} Lessons`);
  lines.push('INSERT INTO course_modules (id, course_id, title, order_index) VALUES');
  lines.push(modRows.join(',\n') + '\nON CONFLICT (course_id, order_index) DO NOTHING;');
  lines.push('');
  lines.push('INSERT INTO course_lessons (id, module_id, title, duration_minutes, order_index) VALUES');
  lines.push(lesRows.join(',\n') + '\nON CONFLICT (module_id, order_index) DO NOTHING;');
  lines.push('');

  // Candidate Targets (40) & Gap Results (120)
  const targetPositions = [
    '11111111-1111-1111-1111-111111111101',
    '11111111-1111-1111-1111-111111111102',
    '11111111-1111-1111-1111-111111111103',
    '11111111-1111-1111-1111-111111111104'
  ];
  const targetRows = [];
  const gapRows = [];
  const enrollRows = [];
  const progRows = [];
  const reviewRows = [];
  const skillNameMap = Object.fromEntries(SKILLS.map(s => [s.code, s.name]));

  CANDIDATES.forEach((cand, idx) => {
    const posId = targetPositions[idx % targetPositions.length];
    targetRows.push(`    (${sqlStr(cand.id)}, 'POSITION', ${sqlStr(posId)}, NULL, NOW() - interval '${20 - (idx % 12)} days')`);

    const tSkills = TRACK_SKILLS[idx % 8].slice(0, 3);
    tSkills.forEach((sk, gIdx) => {
      const req = 80;
      const cur = 55 + ((idx * 7 + gIdx * 11) % 30);
      const gap = Math.max(0, req - cur);
      const prio = gap >= 20 ? 'HIGH' : gap >= 10 ? 'MEDIUM' : 'LOW';
      gapRows.push(`    (${sqlStr(detUuid('gap', `${cand.id}-${sk}`))}, ${sqlStr(cand.id)}, 'POSITION', ${sqlStr(posId)}, ${sqlStr(sk)}, ${sqlStr(skillNameMap[sk] || sk)}, ${req}, ${cur}, ${gap}, ${sqlStr(prio)}, NOW() - interval '5 days')`);
    });

    // Enrollments: 68 total (first 28 candidates have 2 enrollments, remaining 12 have 1 = 68)
    const numEnroll = idx < 28 ? 2 : 1;
    for (let e = 0; e < numEnroll; e++) {
      const crs = COURSES[(idx + e * 5) % COURSES.length];
      const enrollId = detUuid('enrollment', `${cand.id}-${crs.code}`);
      const statuses = ['COMPLETED', 'IN_PROGRESS', 'ENROLLED', 'IN_PROGRESS', 'COMPLETED', 'FAILED'];
      const st = statuses[(idx + e) % statuses.length];
      const progress = st === 'COMPLETED' ? 100.00 : st === 'ENROLLED' ? 0.00 : st === 'FAILED' ? 45.00 : (35 + ((idx * 13) % 55));
      const scoreSql = st === 'COMPLETED' ? (80 + (idx % 18)).toFixed(2) : st === 'FAILED' ? '48.00' : 'NULL';
      const compSql = st === 'COMPLETED' ? `NOW() - interval '${(idx % 10) + 1} days'` : 'NULL';
      const skillCode = crs.skills[0][0];

      enrollRows.push(`    (${sqlStr(enrollId)}, ${sqlStr(cand.id)}, ${sqlStr(crs.code)}, ${sqlStr(skillCode)}, ${sqlStr(st)}, ${progress.toFixed(2)}, ${scoreSql}, NOW() - interval '${25 - (idx % 15)} days', ${compSql})`);

      // Learning progress: 2-3 lessons per enrollment -> ~160 rows (within 100-200)
      const cLessons = courseLessonMap[crs.code] || [];
      const progCount = e === 0 ? 3 : 2;
      for (let p = 0; p < Math.min(progCount, cLessons.length); p++) {
        const done = st === 'COMPLETED' || (st === 'IN_PROGRESS' && p === 0);
        progRows.push(`    (${sqlStr(detUuid('lprog', `${enrollId}-${p}`))}, ${sqlStr(enrollId)}, ${sqlStr(cand.id)}, ${sqlStr(cLessons[p])}, ${done}, ${done ? 1800 : 600}, NOW() - interval '${(idx + p) % 10} days')`);
      }

      // Course reviews: 25 total
      if (e === 0 && idx < 25) {
        const rating = idx % 4 === 0 ? 4 : 5;
        const comments = [
          'Khóa học rất thực tế, bài tập sát với phỏng vấn doanh nghiệp!',
          'Giảng viên giải thích rõ ràng, phần thực hành Microservices cực kỳ hữu ích.',
          'Nội dung cô đọng, giúp mình lấp nhanh khoảng trống kỹ năng sau đánh giá.',
          'Ví dụ trực quan và dễ áp dụng ngay vào dự án IOC.'
        ];
        reviewRows.push(`    (${sqlStr(detUuid('crev', `${cand.id}-${crs.code}`))}, ${sqlStr(crs.id)}, ${sqlStr(cand.id)}, ${rating}, ${sqlStr(comments[idx % comments.length])}, NOW() - interval '${idx % 12} days')`);
      }
    }
  });

  lines.push(`-- 5. ${targetRows.length} Candidate Targets & ${gapRows.length} Gap Results`);
  lines.push('INSERT INTO candidate_targets (user_id, target_type, position_id, job_id, chosen_at) VALUES');
  lines.push(targetRows.join(',\n') + '\nON CONFLICT (user_id) DO NOTHING;');
  lines.push('');
  lines.push('INSERT INTO gap_results (id, user_id, target_type, target_id, skill_id, skill_name, required, current, gap, priority, computed_at) VALUES');
  lines.push(gapRows.join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 6. ${enrollRows.length} Enrollments & ${progRows.length} Learning Progress`);
  lines.push('INSERT INTO enrollments (id, user_id, lms_course_id, skill_id, status, progress, score, enrolled_at, completed_at) VALUES');
  lines.push(enrollRows.join(',\n') + '\nON CONFLICT (user_id, lms_course_id) DO NOTHING;');
  lines.push('');
  lines.push('INSERT INTO learning_progress (id, enrollment_id, user_id, lesson_id, completed, watched_seconds, updated_at) VALUES');
  lines.push(progRows.join(',\n') + '\nON CONFLICT (enrollment_id, lesson_id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 7. ${reviewRows.length} Course Reviews`);
  lines.push('INSERT INTO course_reviews (id, course_id, user_id, rating, comment, created_at) VALUES');
  lines.push(reviewRows.join(',\n') + '\nON CONFLICT (course_id, user_id) DO NOTHING;');
  lines.push('');

  return {
    sql: lines.join('\n'),
    counts: {
      course_categories: COURSE_CATEGORIES.length,
      courses: COURSES.length,
      course_modules: modRows.length,
      course_lessons: lesRows.length,
      enrollments: enrollRows.length,
      learning_progress: progRows.length,
      course_reviews: reviewRows.length
    }
  };
}

// =============================================================================
// F. ASSESSMENT DB SEED (06_seed_assessment_db.sql)
// Matches V1__init_assessment.sql: assessment_types (8), assessments (18),
// assessment_questions (180 -> 720 JSONB options), assessment_attempts (64 -> ~640 JSONB answers)
// =============================================================================
function generateAssessmentSeed() {
  const lines = [];
  lines.push('-- =============================================================================');
  lines.push('-- 06_seed_assessment_db.sql : ASSESSMENT SERVICE SEED DATA (assessment_db)');
  lines.push('-- Khớp 100% V1__init_assessment.sql (JSONB options/correct_answer/answers)');
  lines.push('-- 8 Types, 18 Assessments, 180 Questions (720 options), 64 Attempts (640 candidate answers)');
  lines.push('-- =============================================================================');
  lines.push('CREATE EXTENSION IF NOT EXISTS pgcrypto;');
  lines.push(`CREATE TABLE IF NOT EXISTS assessment_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    duration_minutes INT,
    max_score SMALLINT DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_id UUID NOT NULL REFERENCES assessment_types(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    instructions TEXT,
    passing_score SMALLINT DEFAULT 70,
    time_limit_minutes INT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS assessment_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    question_type VARCHAR(30) NOT NULL CHECK (question_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'SHORT_ANSWER', 'CODE')),
    options JSONB,
    correct_answer JSONB,
    explanation TEXT,
    points SMALLINT DEFAULT 1,
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS assessment_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES assessments(id),
    user_id UUID NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    score SMALLINT,
    max_score SMALLINT,
    percentage DECIMAL(5,2),
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'GRADED', 'EXPIRED')),
    answers JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);`);
  lines.push('');

  const types = [
    ['a1000000-0000-0000-0000-000000000001', 'JAVA_CORE', 'Java Programming Assessment', 'Đánh giá kiến thức Java Core, OOP, Collections, Concurrency', 60, 100],
    ['a1000000-0000-0000-0000-000000000002', 'SPRING_BOOT', 'Spring Boot Assessment', 'Đánh giá Spring Boot, JPA, Security, REST API', 75, 100],
    ['a1000000-0000-0000-0000-000000000003', 'SQL', 'SQL & Database Assessment', 'Đánh giá truy vấn SQL, Index, Transaction, Tối ưu hóa', 45, 100],
    ['a1000000-0000-0000-0000-000000000004', 'REACT', 'ReactJS & TypeScript Assessment', 'Đánh giá React Hooks, State Management, TypeScript', 60, 100],
    ['a1000000-0000-0000-0000-000000000005', 'FULLSTACK', 'Fullstack Assessment', 'Đánh giá tổng hợp Backend + Frontend + DevOps', 90, 100],
    ['a1000000-0000-0000-0000-000000000006', 'DEVOPS', 'DevOps Assessment', 'Đánh giá Docker, Kubernetes, CI/CD, Linux', 60, 100],
    ['a1000000-0000-0000-0000-000000000007', 'ENGLISH', 'IT English Proficiency', 'Đánh giá đọc hiểu tài liệu kỹ thuật & giao tiếp tiếng Anh IT', 45, 100],
    ['a1000000-0000-0000-0000-000000000008', 'LOGICAL_THINKING', 'Logical Thinking & Problem Solving', 'Đánh giá tư duy logic, thuật toán và phân tích bài toán', 45, 100]
  ];

  lines.push('-- 1. 8 Assessment Types');
  lines.push('INSERT INTO assessment_types (id, code, name, description, duration_minutes, max_score) VALUES');
  lines.push(types.map(([id, code, name, desc, dur, max]) =>
    `    (${sqlStr(id)}, ${sqlStr(code)}, ${sqlStr(name)}, ${sqlStr(desc)}, ${dur}, ${max})`
  ).join(',\n') + '\nON CONFLICT (code) DO NOTHING;');
  lines.push('');

  const assessmentDefs = [
    ['JAVA_CORE', 'Java Programming — Level 1 (Core & OOP)', 70, 45],
    ['JAVA_CORE', 'Java Programming — Level 2 (Collections & Streams)', 75, 60],
    ['JAVA_CORE', 'Java Programming — Level 3 (Concurrency & JVM)', 80, 60],
    ['SPRING_BOOT', 'Spring Boot — REST API & Validation', 70, 60],
    ['SPRING_BOOT', 'Spring Boot — Spring Data JPA & Transactions', 75, 60],
    ['SPRING_BOOT', 'Spring Boot — Security, JWT & Microservices', 75, 75],
    ['REACT', 'ReactJS — Components, Props & Hooks', 70, 45],
    ['REACT', 'ReactJS — State Management & Performance', 75, 60],
    ['REACT', 'ReactJS & TypeScript Production Patterns', 75, 60],
    ['SQL', 'SQL Fundamentals — Joins & Aggregations', 70, 45],
    ['SQL', 'SQL Advanced — CTE, Window Functions & Indexing', 75, 45],
    ['SQL', 'PostgreSQL Query Optimization & Locking', 80, 60],
    ['ENGLISH', 'IT English — Reading Technical Docs', 70, 45],
    ['ENGLISH', 'IT English — Workplace & Agile Communication', 70, 45],
    ['LOGICAL_THINKING', 'Logical Thinking — Pattern & Deduction', 70, 45],
    ['LOGICAL_THINKING', 'Logical Thinking — Algorithmic Complexity', 75, 45],
    ['DEVOPS', 'DevOps — Docker & CI/CD Pipeline', 70, 60],
    ['FULLSTACK', 'Fullstack Engineering Capstone Test', 75, 90]
  ];

  const typeIdMap = Object.fromEntries(types.map(t => [t[1], t[0]]));
  const creatorId = ADMINS[0].id;
  const assessmentRows = [];
  const questionRows = [];
  const attemptRows = [];
  const assessments = [];

  assessmentDefs.forEach(([typeCode, title, passScore, timeLimit], aIdx) => {
    const aId = detUuid('assessment', `assess-${aIdx + 1}`);
    const qIds = [];
    assessments.push({ id: aId, typeCode, title, passScore, qIds });
    assessmentRows.push(`    (${sqlStr(aId)}, ${sqlStr(typeIdMap[typeCode])}, ${sqlStr(title)}, ${sqlStr(`Bài kiểm tra đánh giá năng lực ${title} chuẩn hóa trên Rikkei Jobs.`)}, ${sqlStr(`Hoàn thành 10 câu hỏi trắc nghiệm trong ${timeLimit} phút. Điểm đạt tối thiểu: ${passScore}/100.`)}, ${passScore}, ${timeLimit}, TRUE, ${sqlStr(creatorId)}, NOW() - interval '${30 - aIdx} days', NOW())`);

    // 10 questions per assessment = 180 questions (720 options)
    for (let q = 1; q <= 10; q++) {
      const qId = detUuid('assess_q', `${aId}-q${q}`);
      qIds.push(qId);
      const opts = [
        `Phương án A (${typeCode} - khái niệm chuẩn câu ${q})`,
        `Phương án B (${typeCode} - cách tiếp cận thay thế câu ${q})`,
        `Phương án C (${typeCode} - trường hợp ngoại lệ câu ${q})`,
        `Phương án D (${typeCode} - không áp dụng câu ${q})`
      ];
      const correct = opts[q % 4];
      questionRows.push(`    (${sqlStr(qId)}, ${sqlStr(aId)}, ${sqlStr(`[${typeCode}] Câu hỏi số ${q}: Nhận định nào sau đây là chính xác nhất trong ${title}?`)}, 'SINGLE_CHOICE', ${sqlJsonb(opts)}, ${sqlJsonb(correct)}, ${sqlStr(`Giải thích: ${correct} tuân thủ đúng chuẩn thiết kế của ${typeCode}.`)}, 10, ${q})`);
    }
  });

  // 64 Attempts (within 50-80) across 40 candidates (~640 candidate answers in JSONB)
  for (let i = 0; i < 64; i++) {
    const attId = detUuid('attempt', `att-${i + 1}`);
    const cand = CANDIDATES[i % CANDIDATES.length];
    const assess = assessments[i % assessments.length];
    const statuses = ['GRADED', 'GRADED', 'GRADED', 'GRADED', 'SUBMITTED', 'IN_PROGRESS', 'EXPIRED'];
    const st = statuses[i % statuses.length];
    const correctCount = st === 'IN_PROGRESS' ? 4 : st === 'EXPIRED' ? 3 : (6 + (i % 5)); // 60-100
    const score = st === 'IN_PROGRESS' ? null : correctCount * 10;
    const pct = score === null ? null : score.toFixed(2);
    const compSql = (st === 'IN_PROGRESS') ? 'NULL' : `NOW() - interval '${20 - (i % 15)} days' + interval '38 minutes'`;

    const answersObj = {};
    assess.qIds.forEach((qId, qIdx) => {
      const isRight = qIdx < correctCount;
      answersObj[qId] = isRight ? `Phương án ${['A', 'B', 'C', 'D'][(qIdx + 1) % 4]}` : 'Phương án D';
    });

    attemptRows.push(`    (${sqlStr(attId)}, ${sqlStr(assess.id)}, ${sqlStr(cand.id)}, NOW() - interval '${20 - (i % 15)} days', ${compSql}, ${score === null ? 'NULL' : score}, 100, ${pct === null ? 'NULL' : pct}, ${sqlStr(st)}, ${sqlJsonb(answersObj)}, NOW() - interval '${20 - (i % 15)} days', NOW())`);
  }

  lines.push(`-- 2. ${assessmentRows.length} Assessments`);
  lines.push('INSERT INTO assessments (id, type_id, title, description, instructions, passing_score, time_limit_minutes, is_active, created_by, created_at, updated_at) VALUES');
  lines.push(assessmentRows.join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 3. ${questionRows.length} Assessment Questions (${questionRows.length * 4} JSONB Answer Options)`);
  lines.push('INSERT INTO assessment_questions (id, assessment_id, question_text, question_type, options, correct_answer, explanation, points, order_index) VALUES');
  lines.push(questionRows.join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 4. ${attemptRows.length} Assessment Attempts (${attemptRows.length * 10} Candidate Answers in JSONB)`);
  lines.push('INSERT INTO assessment_attempts (id, assessment_id, user_id, started_at, completed_at, score, max_score, percentage, status, answers, created_at, updated_at) VALUES');
  lines.push(attemptRows.join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');

  return {
    sql: lines.join('\n'),
    counts: {
      assessment_types: types.length,
      assessments: assessmentRows.length,
      assessment_questions: questionRows.length,
      answer_options_in_jsonb: questionRows.length * 4,
      assessment_attempts: attemptRows.length,
      candidate_answers_in_jsonb: attemptRows.length * 10
    }
  };
}

// =============================================================================
// G. AI CAREER DB SEED (07_seed_ai_db.sql)
// Matches JobRecommendation.java (uk_job_recommendations_user_job, input_hash VARCHAR(64))
// + ai_chat_sessions (16), ai_chat_messages (128), cv_reviews (16), ai_usage_logs (140), ai_user_quotas (51)
// =============================================================================
function generateAiSeed() {
  const approvedJobs = JOBS.filter(j => j.status === 'APPROVED');
  const lines = [];
  lines.push('-- =============================================================================');
  lines.push('-- 07_seed_ai_db.sql : AI CAREER SERVICE SEED DATA (ai_db)');
  lines.push('-- Khớp 100% JobRecommendation.java + ChatSession, ChatMessage, CVReview, AIUsageLog, AIUserQuota');
  lines.push('-- =============================================================================');
  lines.push('');
  lines.push(`CREATE TABLE IF NOT EXISTS job_recommendations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    job_id UUID NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    explanation VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    input_hash VARCHAR(64) NOT NULL,
    CONSTRAINT uk_job_recommendations_user_job UNIQUE (user_id, job_id)
);

CREATE TABLE IF NOT EXISTS ai_chat_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ai_chat_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES ai_chat_sessions(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    content TEXT NOT NULL,
    tokens_used INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cv_reviews (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    cv_id UUID NOT NULL,
    overall_score SMALLINT NOT NULL CHECK (overall_score BETWEEN 0 AND 100),
    strengths JSONB NOT NULL,
    suggestions JSONB NOT NULL,
    summary TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ai_usage_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    feature VARCHAR(50) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    model VARCHAR(60) NOT NULL,
    prompt_tokens INT NOT NULL,
    completion_tokens INT NOT NULL,
    latency_ms INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ai_user_quotas (
    user_id UUID PRIMARY KEY,
    daily_limit INT NOT NULL DEFAULT 50,
    used_today INT NOT NULL DEFAULT 0,
    monthly_token_limit INT NOT NULL DEFAULT 500000,
    monthly_tokens_used INT NOT NULL DEFAULT 0,
    reset_at TIMESTAMPTZ NOT NULL DEFAULT NOW() + interval '1 day',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);`);
  lines.push('');

  // 1. 160 Job Recommendations (4 per candidate, unique (user_id, job_id))
  const recRows = [];
  CANDIDATES.forEach((cand, cIdx) => {
    for (let j = 0; j < 4; j++) {
      const job = approvedJobs[(cIdx * 3 + j * 11) % approvedJobs.length];
      const score = Number((68.5 + ((cIdx * 5 + j * 7) % 28)).toFixed(1));
      const explanation = `Phù hợp ${score}% với vị trí ${job.title} tại ${job.companyName}. Ứng viên mạnh ở ${job.skillCodes[0]} và ${job.skillCodes[1]}; gợi ý trau dồi thêm ${job.skillCodes[2]} để tối ưu tỷ lệ trúng tuyển.`;
      const inputHash = sha256(`${cand.id}:${job.id}:v1`);
      recRows.push(`    (${sqlStr(detUuid('ai_rec', `${cand.id}-${job.id}`))}, ${sqlStr(cand.id)}, ${sqlStr(job.id)}, ${score}, ${sqlStr(explanation)}, NOW() - interval '${(cIdx + j) % 10} days', ${sqlStr(inputHash)})`);
    }
  });

  // 2. 16 Chat Sessions & 128 Chat Messages (8 per session)
  const sessionRows = [];
  const msgRows = [];
  const sessionTopics = [
    'Tư vấn lộ trình lên Junior Backend Java',
    'Chỉnh sửa CV ứng tuyển Rikkeisoft',
    'Luyện phỏng vấn React & TypeScript',
    'Phân tích khoảng trống kỹ năng SQL & Docker'
  ];
  for (let s = 0; s < 16; s++) {
    const cand = CANDIDATES[s];
    const sessId = detUuid('ai_sess', `s-${s + 1}`);
    const topic = `${sessionTopics[s % sessionTopics.length]} (#${s + 1})`;
    sessionRows.push(`    (${sqlStr(sessId)}, ${sqlStr(cand.id)}, ${sqlStr(topic)}, 'ACTIVE', NOW() - interval '${16 - s} days', NOW() - interval '${s % 5} days')`);
    for (let m = 0; m < 8; m++) {
      const isUser = m % 2 === 0;
      const content = isUser
        ? `Câu hỏi ${Math.floor(m / 2) + 1}: Mình nên cải thiện điểm gì cho mục tiêu "${topic}"?`
        : `Gợi ý AI Career #${Math.floor(m / 2) + 1}: Dựa trên hồ sơ năng lực của ${cand.fullName}, bạn nên hoàn thành bài khảo thí chuyên ngành, bổ sung 1 dự án IOC rõ số liệu và ôn kỹ phần tối ưu hiệu năng.`;
      msgRows.push(`    (${sqlStr(detUuid('ai_msg', `${sessId}-${m}`))}, ${sqlStr(sessId)}, ${isUser ? "'USER'" : "'ASSISTANT'"}, ${sqlStr(content)}, ${isUser ? 45 : 185}, NOW() - interval '${16 - s} days' + interval '${m * 2} minutes')`);
    }
  }

  // 3. 16 CV Reviews
  const cvRevRows = [];
  for (let r = 0; r < 16; r++) {
    const cand = CANDIDATES[r];
    const score = 74 + (r % 22);
    const strengths = ['Bố cục CV rõ ràng, có dự án thực tế IOC', 'Thể hiện rõ kỹ năng cốt lõi đã được LMS xác thực'];
    const suggestions = ['Bổ sung số liệu định lượng hiệu năng dự án', 'Thêm liên kết GitHub và chứng chỉ chuyên môn'];
    cvRevRows.push(`    (${sqlStr(detUuid('cv_rev', cand.id))}, ${sqlStr(cand.id)}, ${sqlStr(detUuid('cv', cand.id))}, ${score}, ${sqlJsonb(strengths)}, ${sqlJsonb(suggestions)}, ${sqlStr(`CV của ${cand.fullName} đạt ${score}/100 điểm sẵn sàng ứng tuyển, phù hợp vị trí Fresher/Junior.`)}, NOW() - interval '${16 - r} days')`);
  }

  // 4. 140 AI Usage Logs
  const usageRows = [];
  const features = ['JOB_RECOMMENDATION', 'CAREER_CHAT', 'CV_REVIEW', 'MATCH_SCORE'];
  for (let l = 0; l < 140; l++) {
    const u = ALL_USERS[l % ALL_USERS.length];
    const feat = features[l % features.length];
    usageRows.push(`    (${sqlStr(detUuid('ai_log', `log-${l + 1}`))}, ${sqlStr(u.id)}, ${sqlStr(feat)}, 'gemini', 'gemini-2.0-flash', ${180 + (l % 250)}, ${240 + (l % 400)}, ${320 + (l % 650)}, 'SUCCESS', NOW() - interval '${(l % 20)} days ${(l % 12)} hours')`);
  }

  // 5. 51 AI User Quotas (1 per user in clean_and_sync_users.sql)
  const quotaRows = ALL_USERS.map((u, idx) => {
    const dailyLimit = u.role === 'ADMIN' || u.role === 'STAFF' ? 200 : u.role === 'HR' ? 100 : 50;
    const usedToday = idx % 12;
    const monthlyUsed = 4500 + (idx * 730);
    return `    (${sqlStr(u.id)}, ${dailyLimit}, ${usedToday}, 500000, ${monthlyUsed}, NOW() + interval '1 day', NOW())`;
  });

  lines.push(`-- 1. ${recRows.length} Job Recommendations`);
  lines.push('INSERT INTO job_recommendations (id, user_id, job_id, score, explanation, created_at, input_hash) VALUES');
  lines.push(recRows.join(',\n') + '\nON CONFLICT (user_id, job_id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 2. ${sessionRows.length} Chat Sessions & ${msgRows.length} Chat Messages`);
  lines.push('INSERT INTO ai_chat_sessions (id, user_id, title, status, created_at, updated_at) VALUES');
  lines.push(sessionRows.join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');
  lines.push('INSERT INTO ai_chat_messages (id, session_id, role, content, tokens_used, created_at) VALUES');
  lines.push(msgRows.join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 3. ${cvRevRows.length} CV Reviews`);
  lines.push('INSERT INTO cv_reviews (id, user_id, cv_id, overall_score, strengths, suggestions, summary, created_at) VALUES');
  lines.push(cvRevRows.join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 4. ${usageRows.length} AI Usage Logs`);
  lines.push('INSERT INTO ai_usage_logs (id, user_id, feature, provider, model, prompt_tokens, completion_tokens, latency_ms, status, created_at) VALUES');
  lines.push(usageRows.join(',\n') + '\nON CONFLICT (id) DO NOTHING;');
  lines.push('');

  lines.push(`-- 5. ${quotaRows.length} AI User Quotas (1 per User)`);
  lines.push('INSERT INTO ai_user_quotas (user_id, daily_limit, used_today, monthly_token_limit, monthly_tokens_used, reset_at, updated_at) VALUES');
  lines.push(quotaRows.join(',\n') + '\nON CONFLICT (user_id) DO UPDATE SET used_today = EXCLUDED.used_today, monthly_tokens_used = EXCLUDED.monthly_tokens_used, updated_at = NOW();');
  lines.push('');

  return {
    sql: lines.join('\n'),
    counts: {
      job_recommendations: recRows.length,
      ai_chat_sessions: sessionRows.length,
      ai_chat_messages: msgRows.length,
      cv_reviews: cvRevRows.length,
      ai_usage_logs: usageRows.length,
      ai_user_quotas: quotaRows.length
    }
  };
}

// =============================================================================
// WRITE ALL SEED FILES + MASTER RUNNER
// =============================================================================
const authSql = generateAuthSeed();
const profileRes = generateProfileSeed();
const jobRes = generateJobSeed();
const recRes = generateRecruitmentSeed();
const learnRes = generateLearningSeed();
const assessRes = generateAssessmentSeed();
const aiRes = generateAiSeed();

fs.writeFileSync(path.join(SEED_DIR, '01_seed_auth_db.sql'), authSql, 'utf8');
fs.writeFileSync(path.join(SEED_DIR, '02_seed_profile_db.sql'), profileRes.sql, 'utf8');
fs.writeFileSync(path.join(SEED_DIR, '03_seed_job_db.sql'), jobRes.sql, 'utf8');
fs.writeFileSync(path.join(SEED_DIR, '04_seed_recruitment_db.sql'), recRes.sql, 'utf8');
fs.writeFileSync(path.join(SEED_DIR, '05_seed_learning_db.sql'), learnRes.sql, 'utf8');
fs.writeFileSync(path.join(SEED_DIR, '06_seed_assessment_db.sql'), assessRes.sql, 'utf8');
fs.writeFileSync(path.join(SEED_DIR, '07_seed_ai_db.sql'), aiRes.sql, 'utf8');

const masterRunner = `-- =============================================================================
-- MASTER SEED RUNNER CHO TOÀN BỘ 7 MICROSERVICES (PostgreSQL / psql)
-- Chạy từ thư mục gốc dự án:
--   psql -U postgres -f infra/seed/run_all_seeds.sql
-- Hoặc mở từng file 01..07 chạy trực tiếp trên DB tương ứng trong DataGrip / DBeaver
-- =============================================================================

\\echo '>>> [A] Seeding auth_db (51 users + 100 login_histories + 35 tokens)...'
\\c auth_db
\\i infra/seed/01_seed_auth_db.sql

\\echo '>>> [B] Seeding profile_db (40 profiles, 50 skills, 180 skill_scores, 70 educations, 60 experiences, 28 certs, 40 portfolios)...'
\\c profile_db
\\i infra/seed/02_seed_profile_db.sql

\\echo '>>> [C] Seeding job_db (10 companies, 12 categories, 24 locations, 120 jobs, 360 job_skills)...'
\\c job_db
\\i infra/seed/03_seed_job_db.sql

\\echo '>>> [D] Seeding recruitment_db (180 recruitments, 420+ history, 46 rounds, 34 feedbacks, 18 offers, 6 employment_records)...'
\\c recruitment_db
\\i infra/seed/04_seed_recruitment_db.sql

\\echo '>>> [E] Seeding learning_db (18 courses, 54 modules, 144 lessons, 68 enrollments, 164 progress, 25 reviews)...'
\\c learning_db
\\i infra/seed/05_seed_learning_db.sql

\\echo '>>> [F] Seeding assessment_db (8 types, 18 assessments, 180 questions/720 options, 64 attempts/640 answers)...'
\\c assessment_db
\\i infra/seed/06_seed_assessment_db.sql

\\echo '>>> [G] Seeding ai_db (160 recommendations, 16 chat sessions, 128 messages, 16 CV reviews, 140 usage logs, 51 quotas)...'
\\c ai_db
\\i infra/seed/07_seed_ai_db.sql

\\echo '>>> DONE! Toàn bộ 7 databases đã được đồng bộ dữ liệu mẫu khớp 100% khóa liên kết.'
`;
fs.writeFileSync(path.join(SEED_DIR, 'run_all_seeds.sql'), masterRunner, 'utf8');

console.log('\n=== SUMMARY OF GENERATED SEED DATA ===');
console.log('A. auth_db       :', { users: ALL_USERS.length, login_histories: 100, used_sso_tickets: 25, password_reset_tokens: 10 });
console.log('B. profile_db    :', profileRes.counts);
console.log('C. job_db        :', jobRes.counts);
console.log('D. recruitment_db:', recRes.counts);
console.log('E. learning_db   :', learnRes.counts);
console.log('F. assessment_db :', assessRes.counts);
console.log('G. ai_db         :', aiRes.counts);
