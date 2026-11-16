/**
 * Rikkei Jobs Mock Server
 * Cổng: 9001
 * Yêu cầu: Xác thực header X-Service-Key: dev-key cho các endpoint hệ thống ngoài (LMS, Khảo thí, PM, Meet)
 * Hỗ trợ: CORS, JSON parsing, phân trang (?page=1&size=20), mã lỗi HTTP chuẩn (400, 401, 403, 404, 409, 429, 500)
 */

const http = require('http');
const url = require('url');
const fs = require('fs');
const path = require('path');

const PORT = 9001;
const SERVICE_KEY = process.env.LMS_SERVICE_KEY || 'dev-key';

// Đọc dữ liệu mẫu từ mock/data/
function loadData(fileName) {
  try {
    const filePath = path.join(__dirname, 'data', fileName);
    return JSON.parse(fs.readFileSync(filePath, 'utf8'));
  } catch (error) {
    console.error(`[Error] Không thể đọc file data/${fileName}:`, error.message);
    return null;
  }
}

// Ghi dữ liệu mẫu vào mock/data/
function saveData(fileName, data) {
  try {
    const filePath = path.join(__dirname, 'data', fileName);
    fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf8');
  } catch (error) {
    console.error(`[Error] Không thể ghi file data/${fileName}:`, error.message);
  }
}

const users = loadData('users.json') || [];
const candidates = loadData('candidates.json') || [];
const lmsData = loadData('lms.json') || { students: {}, courses: [], enrollments: [] };
const examData = loadData('exam.json') || {};
const pmData = loadData('pm.json') || { internships: {}, batches: [] };
const skillsData = loadData('skills.json') || [];
const jobsData = loadData('jobs.json') || [];
const evaluationsData = loadData('evaluations.json') || [];

// Lịch sử duyệt hồ sơ (HS-05), chỉ tồn tại trong bộ nhớ của tiến trình mock
const profileReviews = [];

// Bỏ trường password trước khi trả user ra ngoài (đăng nhập, /auth/me).
function publicUser(user) {
  if (!user) return user;
  const { password, ...safe } = user;
  return safe;
}

// Helper helper gửi response JSON
function sendJson(res, statusCode, data) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Service-Key, X-User-Id, X-User-Role'
  });
  res.end(JSON.stringify(data, null, 2));
}

// Helper gửi lỗi chuẩn { code, message }
function sendError(res, code, message) {
  sendJson(res, code, {
    code: code,
    message: message
  });
}

// Helper parse body cho request POST/PUT/PATCH
function parseBody(req) {
  return new Promise((resolve) => {
    let body = '';
    req.on('data', chunk => {
      body += chunk.toString();
    });
    req.on('end', () => {
      if (!body) {
        resolve({});
        return;
      }
      try {
        resolve(JSON.parse(body));
      } catch (err) {
        resolve({});
      }
    });
  });
}

// Handler xử lý toàn bộ request
async function requestHandler(req, res) {
  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost:9001'}`);
  const pathname = parsedUrl.pathname;
  const method = req.method.toUpperCase();
  const query = Object.fromEntries(parsedUrl.searchParams.entries());

  // Xử lý preflight CORS
  if (method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Service-Key, X-User-Id, X-User-Role'
    });
    res.end();
    return;
  }

  console.log(`[${new Date().toISOString()}] ${method} ${pathname}`);

  // -------------------------------------------------------------
  // KIỂM TRA BẢO MẬT: X-Service-Key: dev-key cho các hệ thống ngoài
  // -------------------------------------------------------------
  const isExternalSystemRoute = 
    pathname.startsWith('/lms') || 
    pathname.startsWith('/exam') || 
    pathname.startsWith('/pm') || 
    pathname.startsWith('/meet');

  if (isExternalSystemRoute) {
    const serviceKey = req.headers['x-service-key'];
    if (!serviceKey || serviceKey !== SERVICE_KEY) {
      return sendError(res, 401, 'Khóa dịch vụ không hợp lệ hoặc bị thiếu (yêu cầu header X-Service-Key: dev-key)');
    }
  }

  // -------------------------------------------------------------
  // A. HỆ THỐNG ĐÀO TẠO (RIKKEI LMS)
  // -------------------------------------------------------------

  // 1. POST /lms/sso/tickets (Giả lập bấm nút từ LMS tạo ticket)
  if (method === 'POST' && pathname === '/lms/sso/tickets') {
    const body = await parseBody(req);
    const lmsUserId = body.lmsUserId || 'std-001';
    const ticket = `TICKET-${lmsUserId.toUpperCase()}-${Date.now()}`;
    return sendJson(res, 200, { ticket });
  }

  // 2. POST /lms/sso/verify (Backend xác thực ticket với LMS)
  if (method === 'POST' && pathname === '/lms/sso/verify') {
    const body = await parseBody(req);
    const ticket = body.ticket || '';
    
    // Tìm sinh viên theo ticket
    let matchedStudent = lmsData.students['std-001'];
    if (ticket.includes('STD-002')) matchedStudent = lmsData.students['std-002'];
    if (ticket.includes('STD-003')) matchedStudent = lmsData.students['std-003'];

    return sendJson(res, 200, {
      lmsUserId: matchedStudent.lmsUserId,
      email: matchedStudent.email,
      fullName: matchedStudent.fullName,
      role: matchedStudent.role,
      issuedAt: new Date().toISOString()
    });
  }

  // 3. GET /lms/students/:id
  const lmsStudentMatch = pathname.match(/^\/lms\/students\/([^\/]+)$/);
  if (method === 'GET' && lmsStudentMatch) {
    const studentId = lmsStudentMatch[1];
    const student = lmsData.students[studentId];
    if (!student) {
      return sendError(res, 404, `Không tìm thấy thông tin sinh viên ${studentId} trên LMS`);
    }
    return sendJson(res, 200, {
      program: student.program,
      classCode: student.classCode,
      courseName: student.courseName
    });
  }

  // 4. GET /lms/students/:id/results
  const lmsResultsMatch = pathname.match(/^\/lms\/students\/([^\/]+)\/results$/);
  if (method === 'GET' && lmsResultsMatch) {
    const studentId = lmsResultsMatch[1];
    const student = lmsData.students[studentId];
    if (!student) {
      return sendError(res, 404, `Không tìm thấy kết quả học tập của sinh viên ${studentId} trên LMS`);
    }
    return sendJson(res, 200, student.results);
  }

  // 5. GET /lms/courses
  if (method === 'GET' && pathname === '/lms/courses') {
    return sendJson(res, 200, lmsData.courses);
  }

  // 6. POST /lms/enrollments
  if (method === 'POST' && pathname === '/lms/enrollments') {
    const body = await parseBody(req);
    const newEnrollment = {
      id: `enr-${Date.now()}`,
      studentId: body.studentId || 'std-001',
      courseId: body.courseId || 'CRS-01',
      status: 'IN_PROGRESS',
      progress: 0.0,
      enrolledAt: new Date().toISOString()
    };
    lmsData.enrollments.push(newEnrollment);
    return sendJson(res, 201, newEnrollment);
  }

  // 7. GET /lms/enrollments/:id
  const enrollmentMatch = pathname.match(/^\/lms\/enrollments\/([^\/]+)$/);
  if (method === 'GET' && enrollmentMatch) {
    const enrId = enrollmentMatch[1];
    const enrollment = lmsData.enrollments.find(e => e.id === enrId);
    if (!enrollment) {
      return sendError(res, 404, `Không tìm thấy thông tin đăng ký môn học ${enrId}`);
    }
    return sendJson(res, 200, enrollment);
  }

  // -------------------------------------------------------------
  // B. HỆ THỐNG KHẢO THÍ (RIKKEI EXAMINATION)
  // -------------------------------------------------------------

  // GET /exam/students/:id/results
  const examMatch = pathname.match(/^\/exam\/students\/([^\/]+)\/results$/);
  if (method === 'GET' && examMatch) {
    const studentId = examMatch[1];
    const results = examData[studentId];
    if (!results) {
      return sendError(res, 404, `Không tìm thấy kết quả thi của thí sinh ${studentId}`);
    }
    return sendJson(res, 200, results);
  }

  // -------------------------------------------------------------
  // C. HỆ THỐNG QUẢN LÝ THỰC TẬP IOC (LMS-PM)
  // -------------------------------------------------------------

  // 1. GET /pm/students/:id/internships
  const pmInternMatch = pathname.match(/^\/pm\/students\/([^\/]+)\/internships$/);
  if (method === 'GET' && pmInternMatch) {
    const studentId = pmInternMatch[1];
    const internships = pmData.internships[studentId];
    if (!internships) {
      return sendError(res, 404, `Không tìm thấy lịch sử thực tập IOC của ứng viên ${studentId}`);
    }
    return sendJson(res, 200, internships);
  }

  // 2. POST /pm/batches/:id/members
  const pmBatchMatch = pathname.match(/^\/pm\/batches\/([^\/]+)\/members$/);
  if (method === 'POST' && pmBatchMatch) {
    const batchId = pmBatchMatch[1];
    const body = await parseBody(req);
    const lmsUserId = body.lmsUserId;
    if (!lmsUserId) {
      return sendError(res, 400, 'Thiếu thông tin lmsUserId trong body');
    }
    return sendJson(res, 200, {
      message: 'Ghép học viên vào đợt thực tập IOC thành công',
      batchId: batchId,
      lmsUserId: lmsUserId,
      assignedAt: new Date().toISOString()
    });
  }

  // -------------------------------------------------------------
  // D. NỀN TẢNG PHỎNG VẤN TRỰC TUYẾN (RIKKEI MEET)
  // -------------------------------------------------------------

  // POST /meet/rooms
  if (method === 'POST' && pathname === '/meet/rooms') {
    const body = await parseBody(req);
    const roomTitle = body.title || 'Phỏng vấn tuyển dụng Rikkei Jobs';
    const roomId = 'room-' + Math.random().toString(36).substring(2, 9);
    return sendJson(res, 200, {
      title: roomTitle,
      roomUrl: `https://meet.rikkei.edu.vn/${roomId}`,
      startAt: body.startAt || new Date().toISOString(),
      endAt: body.endAt || new Date(Date.now() + 3600000).toISOString()
    });
  }

  // -------------------------------------------------------------
  // E. CORE MOCK API (ỨNG VIÊN, JD, TÀI KHOẢN, PHÂN TRANG)
  // -------------------------------------------------------------

  // 1. POST /api/v1/auth/login (Đăng nhập 8 tài khoản mẫu)
  if (method === 'POST' && pathname === '/api/v1/auth/login') {
    const body = await parseBody(req);
    const { email, password } = body;
    const user = users.find(u => u.email === email);
    if (!user) {
      return sendError(res, 404, 'Không tìm thấy tài khoản với email này');
    }
    const expectedPassword = user.password || 'Test@123';
    if (password !== expectedPassword) {
      return sendError(res, 401, 'Mật khẩu không chính xác');
    }
    // Chỉ cho phép HR, MENTOR, STAFF, ADMIN đăng nhập mật khẩu
    const allowedPasswordRoles = ['HR', 'MENTOR', 'STAFF', 'ADMIN'];
    if (!allowedPasswordRoles.includes(user.role)) {
      return sendError(res, 403, 'Học viên chỉ được đăng nhập qua vé SSO (tab Học viên)');
    }
    return sendJson(res, 200, {
      accessToken: `mock-jwt-token-for-${user.role.toLowerCase()}-${Date.now()}`,
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: publicUser(user)
    });
  }

  // 1.02 POST /api/v1/auth/sso (Đăng nhập SSO bằng vé từ LMS)
  if (method === 'POST' && (pathname === '/api/v1/auth/sso' || pathname === '/auth/sso')) {
    const body = await parseBody(req);
    const ticket = body.ticket || '';

    // Map ticket to student (giả lập LMS verify)
    let matchedStudent = lmsData.students['std-001'];
    if (ticket.includes('STD-002')) matchedStudent = lmsData.students['std-002'];
    if (ticket.includes('STD-003')) matchedStudent = lmsData.students['std-003'];

    if (!matchedStudent) {
      return sendError(res, 404, 'Vé SSO không hợp lệ hoặc đã hết hạn');
    }

    // Tìm user trong hệ thống bằng email
    const user = users.find(u => u.email === matchedStudent.email);
    if (!user) {
      return sendError(res, 404, 'Không tìm thấy tài khoản ứng viên');
    }

    return sendJson(res, 200, {
      accessToken: `mock-jwt-token-for-${user.role.toLowerCase()}-${Date.now()}`,
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: publicUser(user)
    });
  }

  // 1.05 GET /api/v1/auth/demo-accounts (Danh sách tài khoản mẫu hỗ trợ đăng nhập nhanh)
  if (method === 'GET' && (pathname === '/api/v1/auth/demo-accounts' || pathname === '/auth/demo-accounts')) {
    const demoAccounts = [
      { role: "CANDIDATE", email: "candidate1@rikkeijobs.test", label: "Ứng viên (Nguyễn Văn An)" },
      { role: "HR", email: "hr1@rikkeijobs.test", label: "Doanh nghiệp HR (Rikkeisoft)" },
      { role: "MENTOR", email: "mentor1@rikkeijobs.test", label: "Mentor (Phạm Minh Đức)" },
      { role: "STAFF", email: "staff1@rikkeijobs.test", label: "Cán bộ tuyển dụng" },
      { role: "ADMIN", email: "admin1@rikkeijobs.test", label: "Quản trị hệ thống" }
    ];
    return sendJson(res, 200, demoAccounts);
  }

  // 1.1 GET /api/v1/auth/me (Lấy thông tin tài khoản hiện tại)
  if (method === 'GET' && (pathname === '/api/v1/auth/me' || pathname === '/auth/me')) {
    const authHeader = req.headers['authorization'] || '';
    let matchedUser = users[0]; // Mặc định candidate1
    if (authHeader.includes('hr')) matchedUser = users[3]; // hr1
    else if (authHeader.includes('mentor')) matchedUser = users[5]; // mentor1
    else if (authHeader.includes('staff')) matchedUser = users[6]; // staff1
    else if (authHeader.includes('admin')) matchedUser = users[7]; // admin1
    return sendJson(res, 200, publicUser(matchedUser));
  }

  // 1.15 POST /api/v1/auth/change-password & /auth/change-password (Đổi mật khẩu)
  if (method === 'POST' && (pathname === '/api/v1/auth/change-password' || pathname === '/auth/change-password')) {
    const authHeader = req.headers['authorization'] || '';
    if (!authHeader.startsWith('Bearer ')) {
      return sendError(res, 401, 'Yêu cầu đăng nhập để đổi mật khẩu');
    }
    const body = await parseBody(req);
    const { currentPassword, newPassword } = body;
    if (!currentPassword || !newPassword) {
      return sendError(res, 400, 'Vui lòng cung cấp mật khẩu hiện tại và mật khẩu mới');
    }
    if (newPassword.length < 8 || !/[A-Za-z]/.test(newPassword) || !/[0-9]/.test(newPassword)) {
      return sendError(res, 400, 'Mật khẩu mới phải dài ít nhất 8 ký tự và bao gồm cả chữ và số');
    }
    const roleHeader = (req.headers['x-user-role'] || '').toUpperCase();
    const userIdHeader = req.headers['x-user-id'] || '';

    let matchedUser = users.find(u => u.id === userIdHeader) || users[0];
    if (roleHeader === 'HR' || authHeader.includes('hr')) matchedUser = users[3];
    else if (roleHeader === 'MENTOR' || authHeader.includes('mentor')) matchedUser = users[5];
    else if (roleHeader === 'STAFF' || authHeader.includes('staff')) matchedUser = users[6];
    else if (roleHeader === 'ADMIN' || authHeader.includes('admin')) matchedUser = users[7];

    if (matchedUser.password && currentPassword !== matchedUser.password && currentPassword !== 'Test@123') {
      return sendError(res, 401, 'Mật khẩu hiện tại không chính xác');
    }
    matchedUser.password = newPassword;
    matchedUser.mustChangePassword = false;
    saveData('users.json', users);
    return sendJson(res, 200, { message: 'Đổi mật khẩu thành công.' });
  }

  // 1.2 GET /api/v1/profiles/:userId/skills & /profiles/me/skills (Hồ sơ kỹ năng cho Frontend)
  const profileSkillsMatch = pathname.match(/^\/(?:api\/v1\/)?profiles\/(me|[^\/]+)\/skills$/);
  if (method === 'GET' && profileSkillsMatch) {
    const targetId = profileSkillsMatch[1];
    // Backend thật xác định danh tính qua X-User-Id; thiếu header trả 401.
    const requesterId = req.headers['x-user-id'];
    if (!requesterId) {
      return sendError(res, 401, 'Thiếu header X-User-Id');
    }
    // Chỉ xem được kỹ năng của chính mình — giống ProfileVisibilityService:
    // hồ sơ không công khai trả 404 cho người khác (và cho id không tồn tại).
    const requester = users.find(u => u.id === requesterId);
    const cand = requester ? candidates.find(c => c.email === requester.email) : null;
    if (targetId !== 'me' || !cand) {
      return sendError(res, 404, 'Không tìm thấy hồ sơ ứng viên');
    }
    const formattedSkills = (cand.skills || []).map((s, idx) => ({
      skillId: String(idx + 1),
      skillCode: s.skillCode || s.name.toUpperCase().replace(/[^A-Z0-9]/g, '_'),
      skillName: s.name,
      group: s.group || (s.name.includes('Java') || s.name.includes('Spring') ? 'Backend' :
             s.name.includes('React') || s.name.includes('HTML') || s.name.includes('Tailwind') ? 'Frontend' :
             s.name.includes('SQL') ? 'Database' : 'Chung'),
      verifiedScore: s.verified ? (s.verifiedScore !== undefined ? s.verifiedScore : s.score) : null,
      selfScore: s.selfScore !== undefined ? s.selfScore : (!s.verified ? s.score : null),
      level: s.level || 'Khá',
      evidences: s.evidences || [
        {
          source: s.verified ? 'LMS' : 'SELF',
          title: `Đánh giá năng lực môn ${s.name}`,
          score: s.score,
          recordedAt: '2026-10-15T08:00:00Z',
          verified: s.verified
        }
      ]
    }));
    return sendJson(res, 200, formattedSkills);
  }

  // 1.25 GET /api/v1/profiles/:userId/evaluations & /profiles/me/evaluations (Đánh giá Mentor)
  const profileEvaluationsMatch = pathname.match(/^\/(?:api\/v1\/)?profiles\/(me|[^\/]+)\/evaluations$/);
  if (method === 'GET' && profileEvaluationsMatch) {
    const targetId = profileEvaluationsMatch[1];
    if (targetId === 'me') {
      return sendJson(res, 200, evaluationsData);
    }
    const matched = evaluationsData.filter(e => e.userId === targetId || e.candidateId === targetId);
    return sendJson(res, 200, matched.length > 0 ? matched : evaluationsData);
  }

  // 1.3 GET /api/v1/companies/me & /companies/me (Thông tin công ty của HR)
  if (method === 'GET' && (pathname === '/api/v1/companies/me' || pathname === '/companies/me')) {
    return sendJson(res, 200, {
      id: 1,
      name: "Công ty Cổ phần Rikkeisoft",
      taxCode: "0101234567",
      industry: "Công nghệ thông tin",
      size: "1000+",
      address: "Tầng 21 Keangnam Landmark 72",
      city: "Hà Nội",
      website: "https://rikkeisoft.com",
      status: "ACTIVE"
    });
  }

  // 2. GET /api/v1/candidates (Phân trang chuẩn ?page=1&size=20 -> { items, total, page, size })
  if (method === 'GET' && pathname === '/api/v1/candidates') {
    const page = parseInt(query.page) || 1;
    const size = parseInt(query.size) || 20;
    const startIndex = (page - 1) * size;
    const paginatedItems = candidates.slice(startIndex, startIndex + size);

    return sendJson(res, 200, {
      items: paginatedItems,
      total: candidates.length,
      page: page,
      size: size
    });
  }

  // 3. GET /api/v1/candidates/:id
  const candidateDetailMatch = pathname.match(/^\/api\/v1\/candidates\/([^\/]+)$/);
  if (method === 'GET' && candidateDetailMatch) {
    const candId = candidateDetailMatch[1];
    const candidate = candidates.find(c => c.id === candId || c.lmsUserId === candId);
    if (!candidate) {
      return sendError(res, 404, `Không tìm thấy ứng viên với id ${candId}`);
    }
    return sendJson(res, 200, candidate);
  }

  // 4. GET /api/v1/skills (20 kỹ năng mẫu)
  if (method === 'GET' && pathname === '/api/v1/skills') {
    return sendJson(res, 200, skillsData);
  }

  // 5. GET /api/v1/jobs (Danh sách JD có phân trang)
  if (method === 'GET' && pathname === '/api/v1/jobs') {
    const page = parseInt(query.page) || 1;
    const size = parseInt(query.size) || 20;
    const startIndex = (page - 1) * size;
    const paginatedItems = jobsData.slice(startIndex, startIndex + size);

    return sendJson(res, 200, {
      items: paginatedItems,
      total: jobsData.length,
      page: page,
      size: size
    });
  }

  // 6. Test Error Endpoints (Phục vụ Frontend kiểm thử xử lý lỗi)
  if (pathname === '/api/v1/mock/error-400') {
    return sendError(res, 400, 'Dữ liệu gửi lên không đúng định dạng');
  }
  if (pathname === '/api/v1/mock/error-401') {
    return sendError(res, 401, 'Phiên đăng nhập đã hết hạn');
  }
  if (pathname === '/api/v1/mock/error-403') {
    return sendError(res, 403, 'Bạn không có quyền thực hiện thao tác này');
  }
  if (pathname === '/api/v1/mock/error-404') {
    return sendError(res, 404, 'Không tìm thấy tài nguyên yêu cầu');
  }
  if (pathname === '/api/v1/mock/error-409') {
    return sendError(res, 409, 'Xung đột dữ liệu hoặc trạng thái');
  }
  if (pathname === '/api/v1/mock/error-429') {
    return sendError(res, 429, 'Vượt quá số lượt yêu cầu trong thời gian cho phép');
  }
  if (pathname === '/api/v1/mock/error-500') {
    return sendError(res, 500, 'Lỗi hệ thống máy chủ nội bộ');
  }

  // =============================================================
  // F. PROFILE SERVICE MOCK ENDPOINTS (HS-02)
  // Phản chiếu profile-service: xác thực bằng header X-User-Id,
  // cập nhật bằng PUT, DTO trùng tên trường với backend.
  // =============================================================

  // Frontend gọi qua baseURL /api/v1 nên bỏ tiền tố trước khi so khớp
  const profilePath = pathname.replace(/^\/api\/v1/, '');

  // Danh tính do gateway truyền xuống qua X-User-Id (UUID)
  const currentUserId = req.headers['x-user-id'];
  const isProfileRoute =
    profilePath === '/profiles/me' ||
    profilePath.startsWith('/profiles/me/');

  if (isProfileRoute && !currentUserId) {
    return sendError(res, 401, 'Thiếu header X-User-Id');
  }

  const currentUser = currentUserId ? users.find(u => u.id === currentUserId) : null;
  const currentCandidate = currentUser
    ? candidates.find(c => c.email === currentUser.email)
    : null;

  if (isProfileRoute && !currentCandidate) {
    return sendError(res, 404, 'Không tìm thấy hồ sơ ứng viên');
  }

  // Gắn định danh người gọi và trạng thái mặc định để FE và luồng HS-05 dùng chung một id
  if (currentCandidate) {
    currentCandidate.userId = currentUserId;
    if (!currentCandidate.visibility) currentCandidate.visibility = 'PRIVATE';
  }

  // 1. GET /profiles/me
  if (method === 'GET' && profilePath === '/profiles/me') {
    return sendJson(res, 200, currentCandidate);
  }

  // 2. PUT /profiles/me — bỏ qua các trường đồng bộ từ LMS
  if (method === 'PUT' && profilePath === '/profiles/me') {
    const body = await parseBody(req);
    // visibility không nằm trong danh sách: trạng thái công khai chỉ đổi qua luồng duyệt
    const editable = ['phone', 'dob', 'gender', 'address', 'avatarUrl', 'headline', 'summary'];
    editable.forEach(field => {
      if (body[field] !== undefined) currentCandidate[field] = body[field];
    });
    currentCandidate.updatedAt = new Date().toISOString();
    return sendJson(res, 200, currentCandidate);
  }

  // 3. PUT /profiles/me/self-skills — [{ skillId, selfScore }]
  if (method === 'PUT' && profilePath === '/profiles/me/self-skills') {
    const body = await parseBody(req);
    const list = Array.isArray(body) ? body : [];
    list.forEach(({ skillId, selfScore }) => {
      const skill = (currentCandidate.skills || []).find(
        (s, idx) => String(idx + 1) === String(skillId) || s.name === skillId);
      if (!skill) return;
      skill.selfScore = selfScore;
      if (!skill.evidences) skill.evidences = [];
      const existing = skill.evidences.find(e => e.source === 'SELF');
      if (existing) {
        existing.score = selfScore;
        existing.recordedAt = new Date().toISOString();
      } else {
        skill.evidences.push({
          source: 'SELF',
          title: 'Self-reported ' + skill.name,
          score: selfScore,
          recordedAt: new Date().toISOString(),
          verified: false
        });
      }
    });
    return sendJson(res, 200, { success: true });
  }

  // 4. GET /profiles/me/educations
  if (method === 'GET' && profilePath === '/profiles/me/educations') {
    return sendJson(res, 200, currentCandidate.educations || []);
  }

  // 5. POST /profiles/me/educations
  if (method === 'POST' && profilePath === '/profiles/me/educations') {
    const body = await parseBody(req);
    if (!currentCandidate.educations) currentCandidate.educations = [];
    const created = {
      id: 'edu-' + Date.now(),
      userId: currentUserId,
      school: body.school,
      major: body.major,
      fromDate: body.fromDate,
      toDate: body.toDate || null,
      gpa: body.gpa === undefined ? null : body.gpa,
      note: body.note || ''
    };
    currentCandidate.educations.push(created);
    return sendJson(res, 201, created);
  }

  // 6. PUT /profiles/me/educations/:id
  const eduMatch = profilePath.match(/^\/profiles\/me\/educations\/([^\/]+)$/);
  if (method === 'PUT' && eduMatch) {
    const body = await parseBody(req);
    const index = (currentCandidate.educations || []).findIndex(e => e.id === eduMatch[1]);
    if (index === -1) {
      return sendError(res, 404, 'Không tìm thấy học vấn');
    }
    const updated = {
      ...currentCandidate.educations[index],
      school: body.school,
      major: body.major,
      fromDate: body.fromDate,
      toDate: body.toDate || null,
      gpa: body.gpa === undefined ? null : body.gpa,
      note: body.note || ''
    };
    currentCandidate.educations[index] = updated;
    return sendJson(res, 200, updated);
  }

  // 7. DELETE /profiles/me/educations/:id
  if (method === 'DELETE' && eduMatch) {
    const index = (currentCandidate.educations || []).findIndex(e => e.id === eduMatch[1]);
    if (index === -1) {
      return sendError(res, 404, 'Không tìm thấy học vấn');
    }
    currentCandidate.educations.splice(index, 1);
    res.writeHead(204, { 'Access-Control-Allow-Origin': '*' });
    return res.end();
  }

  // 8. GET /profiles/me/certificates
  if (method === 'GET' && profilePath === '/profiles/me/certificates') {
    return sendJson(res, 200, currentCandidate.certificates || []);
  }

  // 9. POST /profiles/me/certificates
  if (method === 'POST' && profilePath === '/profiles/me/certificates') {
    const body = await parseBody(req);
    if (!currentCandidate.certificates) currentCandidate.certificates = [];
    const created = {
      id: 'cert-' + Date.now(),
      userId: currentUserId,
      name: body.name,
      issuer: body.issuer,
      issuedAt: body.issuedAt,
      expiresAt: body.expiresAt || null,
      fileUrl: body.fileUrl || null,
      fileType: body.fileType || null
    };
    currentCandidate.certificates.push(created);
    return sendJson(res, 201, created);
  }

  // 10. PUT /profiles/me/certificates/:id
  const certMatch = profilePath.match(/^\/profiles\/me\/certificates\/([^\/]+)$/);
  if (method === 'PUT' && certMatch) {
    const body = await parseBody(req);
    const index = (currentCandidate.certificates || []).findIndex(c => c.id === certMatch[1]);
    if (index === -1) {
      return sendError(res, 404, 'Không tìm thấy chứng chỉ');
    }
    const updated = {
      ...currentCandidate.certificates[index],
      name: body.name,
      issuer: body.issuer,
      issuedAt: body.issuedAt,
      expiresAt: body.expiresAt || null,
      fileUrl: body.fileUrl || null,
      fileType: body.fileType || null
    };
    currentCandidate.certificates[index] = updated;
    return sendJson(res, 200, updated);
  }

  // 11. DELETE /profiles/me/certificates/:id
  if (method === 'DELETE' && certMatch) {
    const index = (currentCandidate.certificates || []).findIndex(c => c.id === certMatch[1]);
    if (index === -1) {
      return sendError(res, 404, 'Không tìm thấy chứng chỉ');
    }
    currentCandidate.certificates.splice(index, 1);
    res.writeHead(204, { 'Access-Control-Allow-Origin': '*' });
    return res.end();
  }

  // =============================================================
  // G. LUỒNG CÔNG KHAI HỒ SƠ (HS-05)
  // Trạng thái: PRIVATE → PENDING → PUBLIC | REJECTED; rút về PRIVATE.
  // Chuyển trạng thái sai luật trả 409, thiếu consent/lý do trả 400.
  // =============================================================

  function recordReview(userId, action, actorId, reason) {
    profileReviews.unshift({
      id: 'rev-' + Date.now() + '-' + profileReviews.length,
      userId: userId,
      action: action,
      reason: reason || null,
      actorId: actorId,
      createdAt: new Date().toISOString()
    });
  }

  function visibilityOf(userId) {
    const cand = candidates.find(c => c.userId === userId || c.id === userId);
    return cand ? (cand.visibility || 'PRIVATE') : null;
  }

  function setVisibility(userId, visibility) {
    const cand = candidates.find(c => c.userId === userId || c.id === userId);
    if (cand) cand.visibility = visibility;
  }

  // 13. POST /profiles/me/publish {consent}
  if (method === 'POST' && profilePath === '/profiles/me/publish') {
    const body = await parseBody(req);
    if (body.consent !== true) {
      return sendError(res, 400, 'Cần đồng ý điều khoản công khai trước khi gửi duyệt');
    }
    const current = currentCandidate.visibility || 'PRIVATE';
    if (current !== 'PRIVATE' && current !== 'REJECTED') {
      return sendError(res, 409, `Chỉ gửi duyệt được hồ sơ PRIVATE hoặc REJECTED. Hiện tại: ${current}`);
    }
    currentCandidate.visibility = 'PENDING';
    currentCandidate.consentAt = new Date().toISOString();
    currentCandidate.consentWithdrawnAt = null;
    currentCandidate.updatedAt = new Date().toISOString();
    recordReview(currentUserId, 'SUBMIT', currentUserId);
    return sendJson(res, 200, currentCandidate);
  }

  // 14. POST /profiles/me/withdraw
  if (method === 'POST' && profilePath === '/profiles/me/withdraw') {
    const current = currentCandidate.visibility || 'PRIVATE';
    if (current !== 'PENDING' && current !== 'PUBLIC') {
      return sendError(res, 409, `Chỉ rút được hồ sơ PENDING hoặc PUBLIC. Hiện tại: ${current}`);
    }
    currentCandidate.visibility = 'PRIVATE';
    currentCandidate.consentWithdrawnAt = new Date().toISOString();
    currentCandidate.updatedAt = new Date().toISOString();
    recordReview(currentUserId, 'WITHDRAW', currentUserId);
    return sendJson(res, 200, currentCandidate);
  }

  // 15. GET /profiles/me/review-history
  if (method === 'GET' && profilePath === '/profiles/me/review-history') {
    return sendJson(res, 200, profileReviews.filter(r => r.userId === currentUserId));
  }

  // 16. GET /staff/profile-reviews?status=PENDING — chỉ STAFF/ADMIN
  if (method === 'GET' && profilePath === '/staff/profile-reviews') {
    const role = (req.headers['x-user-role'] || '').toUpperCase().replace('ROLE_', '');
    if (!currentUserId) {
      return sendError(res, 401, 'Thiếu header X-User-Id');
    }
    if (role !== 'STAFF' && role !== 'ADMIN') {
      return sendError(res, 403, 'Chức năng chỉ dành cho Cán bộ tuyển dụng (STAFF)');
    }
    const status = (query.status || 'PENDING').toUpperCase();
    const matched = candidates.filter(c => (c.visibility || 'PRIVATE') === status);
    const page = parseInt(query.page) || 0;
    const size = parseInt(query.size) || 20;
    return sendJson(res, 200, {
      content: matched.slice(page * size, page * size + size).map(c => ({
        userId: c.userId || c.id,
        fullName: c.fullName,
        email: c.email,
        headline: c.headline || null,
        avatarUrl: c.avatarUrl || null,
        submittedAt: (profileReviews.find(r => r.userId === (c.userId || c.id) && r.action === 'SUBMIT') || {}).createdAt || null
      })),
      totalElements: matched.length,
      totalPages: Math.ceil(matched.length / size) || 0,
      number: page,
      size: size
    });
  }

  // 17. POST /staff/profiles/:userId/approve | /reject
  const staffActionMatch = profilePath.match(/^\/staff\/profiles\/([^\/]+)\/(approve|reject)$/);
  if (method === 'POST' && staffActionMatch) {
    const role = (req.headers['x-user-role'] || '').toUpperCase().replace('ROLE_', '');
    if (!currentUserId) {
      return sendError(res, 401, 'Thiếu header X-User-Id');
    }
    if (role !== 'STAFF' && role !== 'ADMIN') {
      return sendError(res, 403, 'Chức năng chỉ dành cho Cán bộ tuyển dụng (STAFF)');
    }
    const targetId = staffActionMatch[1];
    const action = staffActionMatch[2];
    const current = visibilityOf(targetId);
    if (current === null) {
      return sendError(res, 404, `Không tìm thấy hồ sơ ứng viên ${targetId}`);
    }
    if (current !== 'PENDING') {
      return sendError(res, 409, `Chỉ duyệt/trả lại được hồ sơ PENDING. Hiện tại: ${current}`);
    }
    if (action === 'approve') {
      setVisibility(targetId, 'PUBLIC');
      recordReview(targetId, 'APPROVE', currentUserId);
      return sendJson(res, 200, { userId: targetId, visibility: 'PUBLIC' });
    }
    const body = await parseBody(req);
    if (!body.reason || !body.reason.trim()) {
      return sendError(res, 400, 'Cần nhập lý do trả lại hồ sơ');
    }
    setVisibility(targetId, 'REJECTED');
    recordReview(targetId, 'REJECT', currentUserId, body.reason);
    return sendJson(res, 200, { userId: targetId, visibility: 'REJECTED' });
  }

  // 12. POST /files — mock upload, trả URL tương đối như backend
  if (method === 'POST' && profilePath === '/files') {
    return sendJson(res, 200, { url: '/files/mock-' + Date.now() + '.pdf' });
  }

  // Health check endpoint
  if (pathname === '/' || pathname === '/health') {
    return sendJson(res, 200, {
      status: 'UP',
      service: 'Rikkei Jobs Mock Server',
      port: PORT,
      timestamp: new Date().toISOString()
    });
  }

  // Tuyến đường không tồn tại
  return sendError(res, 404, `Endpoint ${method} ${pathname} không tồn tại trên Mock Server`);
};

const server = http.createServer(requestHandler);

if (require.main === module) {
  server.listen(PORT, '0.0.0.0', () => {
    console.log(`=======================================================`);
    console.log(`🚀 Rikkei Jobs Mock Server đang chạy tại http://localhost:${PORT}`);
    console.log(`🔐 Yêu cầu Header cho hệ thống ngoài: X-Service-Key: ${SERVICE_KEY}`);
    console.log(`📚 Hệ thống hỗ trợ: LMS, Khảo thí, LMS-PM, Rikkei Meet, Auth & Candidate APIs`);
    console.log(`=======================================================`);
  });
}

module.exports = { server, requestHandler };

