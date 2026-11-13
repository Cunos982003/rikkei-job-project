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

const PORT = process.env.PORT || 9000;
const SERVICE_KEY = process.env.SERVICE_KEY || 'dev-key';

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

// Đọc dữ liệu từ mock/data.json
function loadMockDataJson() {
  try {
    const filePath = path.join(__dirname, 'data.json');
    if (fs.existsSync(filePath)) {
      return JSON.parse(fs.readFileSync(filePath, 'utf8'));
    }
  } catch (error) {
    console.error('[Error] Không thể đọc file data.json:', error.message);
  }
  return {
    users: [],
    lmsResults: {},
    examResults: {},
    internships: {},
    batches: [],
    courses: []
  };
}

let mockData = loadMockDataJson();
const users = loadData('users.json') || [];
const candidates = loadData('candidates.json') || [];
const lmsData = loadData('lms.json') || { students: {}, courses: [], enrollments: [] };
const examData = loadData('exam.json') || {};
const pmData = loadData('pm.json') || { internships: {}, batches: [] };
const skillsData = loadData('skills.json') || [];
const jobsData = loadData('jobs.json') || [];
const evaluationsData = loadData('evaluations.json') || [];

// In-memory test state
let tickets = {};
let enrollments = [];
let batchMembers = {};
let requestLog = [];

// Helper gửi response JSON
function sendJson(res, statusCode, data) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Service-Key'
  });
  res.end(JSON.stringify(data, null, 2));
}

// Helper gửi lỗi chuẩn { code, message }
function sendError(res, code, message, errorCode) {
  sendJson(res, typeof code === 'number' ? code : 400, {
    code: errorCode || code,
    message: message
  });
}

// Helper parse body cho request POST/PUT/PATCH (có cache)
function parseBody(req) {
  if (req._parsedBody !== undefined) {
    return Promise.resolve(req._parsedBody);
  }
  return new Promise((resolve) => {
    let body = '';
    req.on('data', chunk => {
      body += chunk.toString();
    });
    req.on('end', () => {
      if (!body) {
        req._parsedBody = {};
        resolve({});
        return;
      }
      try {
        req._parsedBody = JSON.parse(body);
        resolve(req._parsedBody);
      } catch (err) {
        req._parsedBody = {};
        resolve({});
      }
    });
  });
}

// Handler xử lý toàn bộ request
async function requestHandler(req, res) {
  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost:9000'}`);
  const pathname = parsedUrl.pathname;
  const method = req.method.toUpperCase();
  const query = Object.fromEntries(parsedUrl.searchParams.entries());

  // Xử lý preflight CORS
  if (method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Service-Key'
    });
    res.end();
    return;
  }

  // Parse body một lần duy nhất
  const body = (method === 'POST' || method === 'PUT' || method === 'PATCH') ? await parseBody(req) : {};

  // Ghi nhật ký request (bỏ qua /mock/*)
  const isMockToolRoute = pathname.startsWith('/mock');
  if (!isMockToolRoute) {
    requestLog.push({
      at: new Date().toISOString(),
      method: method,
      path: pathname,
      hasKey: Boolean(req.headers['x-service-key']),
      body: (method === 'POST' || method === 'PUT' || method === 'PATCH') ? body : null
    });
    if (requestLog.length > 100) {
      requestLog.shift();
    }
  }

  console.log(`[${new Date().toISOString()}] ${method} ${pathname}`);

  // -------------------------------------------------------------
  // 0. CÔNG CỤ KIỂM THỬ (/mock/*) - Không yêu cầu X-Service-Key
  // -------------------------------------------------------------

  // 0.1 POST /mock/reload: Nạp lại file data.json
  if (method === 'POST' && pathname === '/mock/reload') {
    mockData = loadMockDataJson();
    return sendJson(res, 200, { ok: true, message: 'Nạp lại mock data.json thành công' });
  }

  // 0.2 GET /mock/requests: Trả về 100 request gần nhất
  if (method === 'GET' && pathname === '/mock/requests') {
    return sendJson(res, 200, requestLog);
  }

  // 0.3 PATCH /mock/enrollments: Cập nhật progress, score, status của lượt ghi danh
  if (method === 'PATCH' && pathname === '/mock/enrollments') {
    const { lmsUserId, courseId, progress, score, status } = body;
    const enr = enrollments.find(e => (e.lmsUserId === lmsUserId || e.studentId === lmsUserId) && e.courseId === courseId);
    if (!enr) {
      return sendError(res, 404, 'Không tìm thấy thông tin ghi danh', 'ENROLLMENT_NOT_FOUND');
    }
    if (progress !== undefined) enr.progress = progress;
    if (score !== undefined) enr.score = score;
    if (status !== undefined) enr.status = status;
    return sendJson(res, 200, enr);
  }

  // 0.4 POST /mock/reset: Xóa vé tạm, ghi danh, thành viên đợt và nhật ký request
  if (method === 'POST' && pathname === '/mock/reset') {
    tickets = {};
    enrollments = [];
    batchMembers = {};
    requestLog = [];
    return sendJson(res, 200, { ok: true, message: 'Đã reset bộ nhớ mock server' });
  }

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
      return sendError(res, 401, 'Thiếu hoặc sai X-Service-Key', 'INVALID_SERVICE_KEY');
    }
  }

  // -------------------------------------------------------------
  // A. HỆ THỐNG ĐÀO TẠO (RIKKEI LMS)
  // -------------------------------------------------------------

  // 1. POST /lms/sso/tickets (Giả lập bấm nút từ LMS tạo ticket)
  if (method === 'POST' && pathname === '/lms/sso/tickets') {
    const lmsUserId = body.lmsUserId || 'lms-sv-001';
    let matchedUser = (mockData.users || []).find(u => u.lmsUserId === lmsUserId || u.email === lmsUserId);
    if (!matchedUser) {
      if (lmsUserId === 'std-001') matchedUser = { lmsUserId: 'std-001', email: 'candidate1@rikkeijobs.test', fullName: 'Nguyễn Văn An', role: 'CANDIDATE' };
      else if (lmsUserId === 'std-002') matchedUser = { lmsUserId: 'std-002', email: 'candidate2@rikkeijobs.test', fullName: 'Trần Thị Bình', role: 'CANDIDATE' };
      else if (lmsUserId === 'std-003') matchedUser = { lmsUserId: 'std-003', email: 'candidate3@rikkeijobs.test', fullName: 'Lê Minh Châu', role: 'CANDIDATE' };
      else if (lmsData.students && lmsData.students[lmsUserId]) {
        const s = lmsData.students[lmsUserId];
        matchedUser = { lmsUserId: s.lmsUserId || lmsUserId, email: s.email, fullName: s.fullName, role: s.role || 'CANDIDATE' };
      }
    }
    if (!matchedUser) {
      return sendError(res, 404, 'Người dùng không tồn tại', 'USER_NOT_FOUND');
    }
    const ticket = `TICKET-${(matchedUser.lmsUserId || lmsUserId).toUpperCase()}-${Date.now()}`;
    tickets[ticket] = matchedUser;
    return sendJson(res, 201, {
      ticket: ticket,
      issuedAt: new Date().toISOString()
    });
  }

  // 2. POST /lms/sso/verify (Backend xác thực ticket với LMS)
  if (method === 'POST' && pathname === '/lms/sso/verify') {
    const ticket = body.ticket || '';
    if (tickets[ticket]) {
      const u = tickets[ticket];
      return sendJson(res, 200, {
        lmsUserId: u.lmsUserId,
        email: u.email,
        fullName: u.fullName,
        role: u.role,
        issuedAt: new Date().toISOString()
      });
    }

    // Pattern matching fallback
    let matched = null;
    const upper = ticket.toUpperCase();
    if (upper.includes('STD-001') || upper.includes('LMS-SV-001')) {
      matched = (mockData.users || []).find(u => u.lmsUserId === 'lms-sv-001') || lmsData.students['std-001'];
    } else if (upper.includes('STD-002') || upper.includes('LMS-SV-002')) {
      matched = (mockData.users || []).find(u => u.lmsUserId === 'lms-sv-002') || lmsData.students['std-002'];
    } else if (upper.includes('STD-003') || upper.includes('LMS-SV-003')) {
      matched = (mockData.users || []).find(u => u.lmsUserId === 'lms-sv-003') || lmsData.students['std-003'];
    } else if (upper.includes('LMS-MT-001') || upper.includes('MENTOR')) {
      matched = (mockData.users || []).find(u => u.lmsUserId === 'lms-mt-001');
    } else if (upper.includes('LMS-ST-001') || upper.includes('STAFF')) {
      matched = (mockData.users || []).find(u => u.lmsUserId === 'lms-st-001');
    } else if (upper.includes('LMS-AD-001') || upper.includes('ADMIN')) {
      matched = (mockData.users || []).find(u => u.lmsUserId === 'lms-ad-001');
    }

    if (!matched) {
      return sendError(res, 404, 'Vé không tồn tại hoặc đã hết hạn', 'TICKET_NOT_FOUND');
    }

    return sendJson(res, 200, {
      lmsUserId: matched.lmsUserId,
      email: matched.email,
      fullName: matched.fullName,
      role: matched.role || 'CANDIDATE',
      issuedAt: new Date().toISOString()
    });
  }

  // Helper phân giải studentId (hỗ trợ UUID, lmsUserId, std-xxx, email)
  function resolveStudentId(id) {
    if (!id) return 'std-001';
    if (id.startsWith('std-')) return id;
    const uuidMap = {
      '00000000-0000-0000-0000-000000000001': 'std-001',
      '00000000-0000-0000-0000-000000000002': 'std-002',
      '00000000-0000-0000-0000-000000000003': 'std-003',
      'ed854c07-f3eb-48f1-a186-4e7135f54645': 'std-001',
      '550e8400-e29b-41d4-a716-446655440000': 'std-001',
      'lms-sv-001': 'std-001',
      'lms-sv-002': 'std-002',
      'lms-sv-003': 'std-003',
      'candidate1@rikkeijobs.test': 'std-001',
      'candidate2@rikkeijobs.test': 'std-002',
      'candidate3@rikkeijobs.test': 'std-003'
    };
    return uuidMap[id] || 'std-001';
  }

  // 3. GET /lms/students/:id
  const lmsStudentMatch = pathname.match(/^\/lms\/students\/([^\/]+)$/);
  if (method === 'GET' && lmsStudentMatch) {
    const rawId = lmsStudentMatch[1];
    const stdId = resolveStudentId(rawId);
    const student = (lmsData.students && lmsData.students[stdId]) || {
      lmsUserId: stdId,
      fullName: 'Nguyễn Văn An',
      email: 'candidate1@rikkeijobs.test',
      program: 'Lập trình Java Fullstack Chuyên sâu',
      classCode: 'RA-JAVA-2026',
      courseName: 'Khóa đào tạo Kỹ sư Phần mềm Java'
    };

    return sendJson(res, 200, {
      lmsUserId: student.lmsUserId || stdId,
      fullName: student.fullName || 'Nguyễn Văn An',
      email: student.email || 'candidate1@rikkeijobs.test',
      program: student.program || 'Lập trình Java Fullstack Chuyên sâu',
      classCode: student.classCode || 'RA-JAVA-2026',
      courseName: student.courseName || 'Khóa đào tạo Kỹ sư Phần mềm Java'
    });
  }

  // 4. GET /lms/students/:id/results
  const lmsResultsMatch = pathname.match(/^\/lms\/students\/([^\/]+)\/results$/);
  if (method === 'GET' && lmsResultsMatch) {
    const rawId = lmsResultsMatch[1];
    const stdId = resolveStudentId(rawId);
    let results = (lmsData.students && lmsData.students[stdId]) ? lmsData.students[stdId].results : null;
    if (!results) {
      results = (mockData.lmsResults || {})[rawId] || (mockData.lmsResults || {})[stdId];
    }
    return sendJson(res, 200, results || []);
  }

  // 5. GET /lms/courses
  if (method === 'GET' && pathname === '/lms/courses') {
    let list = (mockData.courses && mockData.courses.length > 0) ? mockData.courses : (lmsData.courses || []);
    if (query.skill) {
      const s = query.skill.toLowerCase();
      list = list.filter(c => Array.isArray(c.skills) && c.skills.some(sk => sk.toLowerCase() === s));
    }
    return sendJson(res, 200, list);
  }

  // 6. POST /lms/enrollments
  if (method === 'POST' && pathname === '/lms/enrollments') {
    const lmsUserId = body.lmsUserId || body.studentId;
    const courseId = body.courseId;
    if (!lmsUserId || !courseId) {
      return sendError(res, 400, 'Thiếu lmsUserId hoặc courseId', 'BAD_REQUEST');
    }
    // Check user exists
    const userExists = (mockData.users || []).some(u => u.lmsUserId === lmsUserId) ||
                       ['std-001', 'std-002', 'std-003'].includes(lmsUserId) ||
                       (lmsData.students && lmsData.students[lmsUserId]);
    if (!userExists) {
      return sendError(res, 404, `Không tìm thấy người dùng ${lmsUserId}`, 'USER_NOT_FOUND');
    }
    // Check course exists
    const allCourses = (mockData.courses && mockData.courses.length > 0) ? mockData.courses : (lmsData.courses || []);
    const courseExists = allCourses.some(c => c.courseId === courseId);
    if (!courseExists) {
      return sendError(res, 404, `Không tìm thấy khóa học ${courseId}`, 'COURSE_NOT_FOUND');
    }
    // Check already enrolled
    const already = enrollments.some(e => (e.lmsUserId === lmsUserId || e.studentId === lmsUserId) && e.courseId === courseId);
    if (already) {
      return sendError(res, 409, 'Đã ghi danh khóa học này', 'ALREADY_ENROLLED');
    }

    const newEnrollment = {
      id: `enr-${Date.now()}`,
      lmsUserId: lmsUserId,
      studentId: lmsUserId,
      courseId: courseId,
      progress: 0,
      score: null,
      status: 'ENROLLED',
      enrolledAt: new Date().toISOString()
    };
    enrollments.push(newEnrollment);
    return sendJson(res, 201, newEnrollment);
  }

  // 7. GET /lms/enrollments/:target (hỗ trợ cả enr-xxx và lmsUserId)
  const enrollmentMatch = pathname.match(/^\/lms\/enrollments\/([^\/]+)$/);
  if (method === 'GET' && enrollmentMatch) {
    const target = enrollmentMatch[1];
    if (target.startsWith('enr-')) {
      const single = enrollments.find(e => e.id === target) || (lmsData.enrollments && lmsData.enrollments.find(e => e.id === target));
      if (!single) {
        return sendError(res, 404, `Không tìm thấy thông tin đăng ký môn học ${target}`, 'ENROLLMENT_NOT_FOUND');
      }
      return sendJson(res, 200, single);
    }
    const userEnrollments = enrollments.filter(e => e.lmsUserId === target || e.studentId === target);
    return sendJson(res, 200, userEnrollments);
  }

  // -------------------------------------------------------------
  // B. HỆ THỐNG KHẢO THÍ (RIKKEI EXAMINATION)
  // -------------------------------------------------------------

  // GET /exam/students/:id/results
  const examMatch = pathname.match(/^\/exam\/students\/([^\/]+)\/results$/);
  if (method === 'GET' && examMatch) {
    const rawId = examMatch[1];
    const stdId = resolveStudentId(rawId);
    let results = examData[stdId] || (mockData.examResults || {})[rawId] || (mockData.examResults || {})[stdId];
    return sendJson(res, 200, results || []);
  }

  // -------------------------------------------------------------
  // C. HỆ THỐNG QUẢN LÝ THỰC TẬP IOC (LMS-PM)
  // -------------------------------------------------------------

  // 1. GET /pm/students/:id/internships
  const pmInternMatch = pathname.match(/^\/pm\/students\/([^\/]+)\/internships$/);
  if (method === 'GET' && pmInternMatch) {
    const rawId = pmInternMatch[1];
    const stdId = resolveStudentId(rawId);
    let list = (pmData.internships && pmData.internships[stdId]) || (mockData.internships || {})[rawId] || (mockData.internships || {})[stdId];
    return sendJson(res, 200, list || []);
  }

  // 2. POST /pm/batches/:id/members
  const pmBatchMatch = pathname.match(/^\/pm\/batches\/([^\/]+)\/members$/);
  if (method === 'POST' && pmBatchMatch) {
    const batchId = pmBatchMatch[1];
    const lmsUserId = body.lmsUserId;
    if (!lmsUserId) {
      return sendError(res, 400, 'Thiếu thông tin lmsUserId trong body', 'BAD_REQUEST');
    }
    // Check batch exists
    const allBatches = (mockData.batches && mockData.batches.length > 0) ? mockData.batches : (pmData.batches || []);
    const batchExists = allBatches.some(b => b.batchId === batchId);
    if (!batchExists) {
      return sendError(res, 404, `Đợt thực tập ${batchId} không tồn tại`, 'BATCH_NOT_FOUND');
    }
    // Check user exists
    const userExists = (mockData.users || []).some(u => u.lmsUserId === lmsUserId) ||
                       ['std-001', 'std-002', 'std-003'].includes(lmsUserId) ||
                       (lmsData.students && lmsData.students[lmsUserId]);
    if (!userExists) {
      return sendError(res, 404, `Học viên ${lmsUserId} không tồn tại`, 'USER_NOT_FOUND');
    }
    if (!batchMembers[batchId]) {
      batchMembers[batchId] = new Set();
    }
    if (batchMembers[batchId].has(lmsUserId)) {
      return sendError(res, 409, 'Học viên đã thuộc đợt thực tập này', 'ALREADY_MEMBER');
    }
    batchMembers[batchId].add(lmsUserId);

    return sendJson(res, 201, {
      batchId: batchId,
      lmsUserId: lmsUserId,
      addedAt: new Date().toISOString(),
      message: 'Ghép học viên vào đợt thực tập IOC thành công'
    });
  }

  // -------------------------------------------------------------
  // D. NỀN TẢNG PHỎNG VẤN TRỰC TUYẾN (RIKKEI MEET)
  // -------------------------------------------------------------

  // POST /meet/rooms
  if (method === 'POST' && pathname === '/meet/rooms') {
    if (!body.title) {
      return sendError(res, 400, 'Thiếu thông tin tiêu đề phòng họp (title)', 'BAD_REQUEST');
    }
    const roomId = 'room-' + Math.random().toString(36).substring(2, 9);
    return sendJson(res, 201, {
      roomId: roomId,
      title: body.title,
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
    if (password !== 'Test@123') {
      return sendError(res, 401, 'Mật khẩu không chính xác (mật khẩu chuẩn: Test@123)');
    }
    const user = users.find(u => u.email === email);
    if (!user) {
      return sendError(res, 404, 'Không tìm thấy tài khoản với email này');
    }
    return sendJson(res, 200, {
      accessToken: `mock-jwt-token-for-${user.role.toLowerCase()}-${Date.now()}`,
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: user
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
    return sendJson(res, 200, matchedUser);
  }

  // 1.2 GET /api/v1/profiles/:userId/skills & /profiles/me/skills (Hồ sơ kỹ năng cho Frontend)
  const profileSkillsMatch = pathname.match(/^\/(?:api\/v1\/)?profiles\/(me|[^\/]+)\/skills$/);
  if (method === 'GET' && profileSkillsMatch) {
    const targetId = profileSkillsMatch[1];
    let cand = candidates[0];
    if (targetId !== 'me') {
      cand = candidates.find(c => c.id === targetId || c.lmsUserId === targetId || c.email === targetId) || candidates[0];
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

  // 1. GET /profiles/me
  if (method === 'GET' && profilePath === '/profiles/me') {
    return sendJson(res, 200, currentCandidate);
  }

  // 2. PUT /profiles/me — bỏ qua các trường đồng bộ từ LMS
  if (method === 'PUT' && profilePath === '/profiles/me') {
    const body = await parseBody(req);
    const editable = ['phone', 'dob', 'gender', 'address', 'avatarUrl', 'headline', 'summary', 'visibility'];
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

