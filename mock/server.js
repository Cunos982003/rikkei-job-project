/**
 * Rikkei Jobs Mock Server
 * Cổng: 9000
 * Các endpoint hệ thống ngoài (LMS, Khảo thí, PM, Meet) yêu cầu X-Service-Key.
 * Hỗ trợ: CORS, JSON parsing, phân trang (?page=1&size=20), mã lỗi HTTP chuẩn (400, 401, 403, 404, 409, 429, 500)
 */

const http = require('http');
const url = require('url');
const fs = require('fs');
const path = require('path');
const { renderLmsPortalHtml } = require('./portal');

const PORT = Number(process.env.PORT || 9000);
const DEFAULT_LMS_SERVICE_KEY = 'fbc368ffa97a999a71577c552ecc2546815f245fd4ebee014732afcba1b52870';
const SERVICE_KEY = process.env.LMS_SERVICE_KEY || DEFAULT_LMS_SERVICE_KEY;

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
const companies = loadData('companies.json') || [];
const offerStore = loadData('offers.json') || { applications: [], offers: [] };
const mockApplications = offerStore.applications || [];
const mockOffers = offerStore.offers || [];
const mockEmploymentRecords = loadData('employment_records.json') || [];

// Lịch sử duyệt hồ sơ (HS-05), chỉ tồn tại trong bộ nhớ của tiến trình mock
const profileReviews = [];

// Bỏ trường password trước khi trả user ra ngoài (đăng nhập, /auth/me).
function publicUser(user) {
  if (!user) return user;
  const { password, ...safe } = user;
  return safe;
}

const UUID_TO_LMS = {
  'ed854c07-f3eb-48f1-a186-4e7135f54645': 'std-001',
  '797c5663-386b-4e96-9112-6d3e414b4449': 'std-002',
  '4a09b051-d7ab-40a5-8322-963ec9ebdbd7': 'std-003',
  'user-001': 'std-001',
  'user-002': 'std-002',
  'user-003': 'std-003',
  'cand-001': 'std-001',
  'cand-002': 'std-002',
  'cand-003': 'std-003'
};

function resolveStudent(idOrUuid) {
  if (!idOrUuid) return null;
  const key = String(idOrUuid).toLowerCase();
  if (lmsData.students && lmsData.students[key]) return lmsData.students[key];
  const mapped = UUID_TO_LMS[idOrUuid];
  if (mapped && lmsData.students && lmsData.students[mapped.toLowerCase()]) return lmsData.students[mapped.toLowerCase()];
  if (lmsData.students) {
    const byEmail = Object.values(lmsData.students).find(s => s.email && s.email.toLowerCase() === key);
    if (byEmail) return byEmail;
  }
  const u = users.find(u => u.id === idOrUuid || u.email === idOrUuid || (u.lmsUserId && u.lmsUserId.toLowerCase() === key));
  if (u && u.lmsUserId && lmsData.students && lmsData.students[u.lmsUserId.toLowerCase()]) {
    return lmsData.students[u.lmsUserId.toLowerCase()];
  }
  return lmsData.students ? lmsData.students['std-001'] : null;
}

function resolveUserFromRequest(req) {
  const userId = req.headers['x-user-id'];
  if (userId) {
    let found = users.find(u => u.id === userId || u.lmsUserId === userId);
    if (found) return found;
    const lmsId = UUID_TO_LMS[userId];
    if (lmsId) {
      found = users.find(u => u.lmsUserId === lmsId);
      if (found) return found;
    }
  }
  const authHeader = req.headers['authorization'];
  if (authHeader && authHeader.startsWith('Bearer ')) {
    try {
      const parts = authHeader.split(' ')[1].split('.');
      if (parts.length === 3) {
        const payload = JSON.parse(Buffer.from(parts[1], 'base64').toString('utf8'));
        if (payload.email) {
          const found = users.find(u => u.email === payload.email);
          if (found) return found;
        }
        if (payload.lmsUserId) {
          const found = users.find(u => u.lmsUserId === payload.lmsUserId);
          if (found) return found;
        }
      }
    } catch (e) {}
  }
  return users.find(u => u.role === 'CANDIDATE') || users[0];
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

// Helper gửi response HTML (phục vụ giao diện Portal Sinh viên LMS)
function sendHtml(res, statusCode, html) {
  res.writeHead(statusCode, {
    'Content-Type': 'text/html; charset=utf-8',
    'Access-Control-Allow-Origin': '*'
  });
  res.end(html);
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
  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost:' + PORT}`);
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
  // Tuyến đường Cổng Đào Tạo Học Viên LMS (Mock LMS Portal HTML)
  // Phục vụ tại: http://localhost:9000/ hoặc /lms/portal hoặc /lms/login
  // -------------------------------------------------------------
  const isPortalRoute = pathname === '/' || pathname === '/lms' || pathname === '/lms/portal' || pathname === '/lms/login';
  if (method === 'GET' && isPortalRoute) {
    const isJsonRequested = req.headers.accept && req.headers.accept.includes('application/json') && !req.headers.accept.includes('text/html');
    if (pathname === '/' && isJsonRequested) {
      return sendJson(res, 200, {
        status: 'UP',
        service: 'Rikkei Jobs Mock Server',
        port: PORT,
        timestamp: new Date().toISOString()
      });
    }
    return sendHtml(res, 200, renderLmsPortalHtml(lmsData, users));
  }

  // -------------------------------------------------------------
  // Require the configured service key for external-system routes.
  // Tuyến cấp vé SSO (/lms/sso/tickets) được mở để browser/portal gọi trực tiếp.
  // -------------------------------------------------------------
  const isSsoTicketCreation = method === 'POST' && pathname === '/lms/sso/tickets';
  const isExternalSystemRoute = 
    !isPortalRoute &&
    !isSsoTicketCreation &&
    (pathname.startsWith('/lms') || 
     pathname.startsWith('/exam') || 
     pathname.startsWith('/pm') || 
     pathname.startsWith('/meet'));

  if (isExternalSystemRoute) {
    const serviceKey = req.headers['x-service-key'];
    if (!serviceKey || (serviceKey !== SERVICE_KEY && serviceKey !== 'dev-key' && serviceKey !== DEFAULT_LMS_SERVICE_KEY)) {
      return sendError(res, 401, 'Khóa dịch vụ không hợp lệ hoặc bị thiếu (yêu cầu header X-Service-Key)');
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
    return sendJson(res, 200, {
      ticket,
      lmsUserId,
      expiresIn: 60,
      redirectUrl: `http://localhost:5173/login?ticket=${encodeURIComponent(ticket)}&auto=true`
    });
  }

  // 2. POST /lms/sso/verify (Backend xác thực ticket với LMS)
  if (method === 'POST' && pathname === '/lms/sso/verify') {
    const body = await parseBody(req);
    const ticket = body.ticket || '';
    
    // Tìm sinh viên theo ticket dynamically
    let matchedStudent = null;
    const ticketMatch = ticket.match(/STD-(\d+)/i);
    if (ticketMatch) {
      const studentKey = `std-${ticketMatch[1].padStart(3, '0')}`.toLowerCase();
      matchedStudent = lmsData.students[studentKey];
    }
    if (!matchedStudent) {
      matchedStudent = lmsData.students['std-001'];
    }

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
    const student = resolveStudent(studentId);
    if (!student) {
      return sendError(res, 404, `Không tìm thấy thông tin sinh viên ${studentId} trên LMS`);
    }
    return sendJson(res, 200, {
      fullName: student.fullName || 'Nguyễn Văn An',
      email: student.email || 'candidate1@rikkeijobs.test',
      program: student.program,
      classCode: student.classCode,
      courseName: student.courseName
    });
  }

  // 4. GET /lms/students/:id/results
  const lmsResultsMatch = pathname.match(/^\/lms\/students\/([^\/]+)\/results$/);
  if (method === 'GET' && lmsResultsMatch) {
    const studentId = lmsResultsMatch[1];
    const student = resolveStudent(studentId);
    if (!student) {
      return sendError(res, 404, `Không tìm thấy kết quả học tập của sinh viên ${studentId} trên LMS`);
    }
    return sendJson(res, 200, student.results || []);
  }

  // 5. GET /lms/courses
  if (method === 'GET' && pathname === '/lms/courses') {
    return sendJson(res, 200, lmsData.courses);
  }

  // 6. POST /lms/enrollments
  if (method === 'POST' && pathname === '/lms/enrollments') {
    const body = await parseBody(req);
    // Hỗ trợ mô phỏng lỗi LMS cho AC4
    if (req.headers['x-mock-fail'] === 'true' ||
        parsedUrl.query?.fail === 'true' ||
        body.mockError === true ||
        body.courseId === 'CRS-ERROR' ||
        body.lmsCourseId === 'CRS-ERROR') {
      return sendError(res, 500, 'LMS_ERROR: Máy chủ LMS gặp sự cố nội bộ khi ghi danh (Simulated Failure)');
    }

    const studentId = body.studentId || body.lmsUserId || body.userId || 'std-001';
    const courseId = body.courseId || body.lmsCourseId || 'CRS-01';

    const newEnrollment = {
      id: `enr-${Date.now()}`,
      studentId,
      userId: studentId,
      courseId,
      lmsCourseId: courseId,
      status: 'IN_PROGRESS',
      progress: 0.0,
      score: null,
      enrolledAt: new Date().toISOString()
    };
    lmsData.enrollments.push(newEnrollment);
    return sendJson(res, 201, newEnrollment);
  }

  // 6b. POST /lms/mock/complete-course (Endpoint hỗ trợ test AC3: cập nhật hoàn thành môn học kèm điểm)
  if (method === 'POST' && (pathname === '/lms/mock/complete-course' || pathname === '/lms/enrollments/complete')) {
    const body = await parseBody(req);
    const studentId = body.studentId || body.lmsUserId || body.userId;
    const courseId = body.courseId || body.lmsCourseId;
    const score = body.score !== undefined ? Number(body.score) : 85.0;

    let enr = lmsData.enrollments.find(e => 
      (e.courseId === courseId || e.lmsCourseId === courseId) &&
      (!studentId || e.studentId === studentId || e.userId === studentId)
    );

    if (!enr) {
      enr = {
        id: `enr-${Date.now()}`,
        studentId: studentId || 'std-001',
        userId: studentId || 'std-001',
        courseId: courseId || 'CRS-01',
        lmsCourseId: courseId || 'CRS-01',
        status: 'COMPLETED',
        progress: 100.0,
        score: score,
        enrolledAt: new Date(Date.now() - 7 * 86400000).toISOString(),
        completedAt: new Date().toISOString()
      };
      lmsData.enrollments.push(enr);
    } else {
      enr.status = 'COMPLETED';
      enr.progress = 100.0;
      enr.score = score;
      enr.completedAt = new Date().toISOString();
    }

    return sendJson(res, 200, {
      message: 'Cập nhật hoàn thành khóa học trên Mock LMS thành công',
      enrollment: enr
    });
  }

  // 7. GET /lms/enrollments/:param (param có thể là enrollmentId hoặc lmsUserId/studentId)
  const enrollmentMatch = pathname.match(/^\/lms\/enrollments\/([^\/]+)$/);
  if (method === 'GET' && enrollmentMatch) {
    const enrParam = enrollmentMatch[1];
    
    // Tìm danh sách đăng ký theo studentId / lmsUserId
    const studentEnrollments = lmsData.enrollments.filter(e => 
      e.studentId === enrParam || e.userId === enrParam || e.lmsUserId === enrParam
    );
    if (studentEnrollments.length > 0) {
      return sendJson(res, 200, studentEnrollments);
    }

    // Hoặc tìm bản ghi đơn lẻ theo enrollmentId
    const single = lmsData.enrollments.find(e => e.id === enrParam);
    if (single) {
      return sendJson(res, 200, single);
    }

    // Nếu hỏi theo dạng ID người dùng mà chưa có môn nào
    return sendJson(res, 200, []);
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
    const allowedPasswordRoles = ['HR', 'MENTOR', 'STAFF', 'ADMIN', 'CANDIDATE'];
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

    // Map ticket to student (giả lập LMS verify dynamically)
    let matchedStudent = null;
    const ticketMatch = ticket.match(/STD-(\d+)/i);
    if (ticketMatch) {
      const studentKey = `std-${ticketMatch[1].padStart(3, '0')}`.toLowerCase();
      matchedStudent = lmsData.students[studentKey];
    }
    if (!matchedStudent) {
      matchedStudent = lmsData.students['std-001'];
    }

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

  // 1.16 POST /api/v1/auth/first-time-password & /auth/first-time-password (Đặt mật khẩu mới lần đầu - không cần mật khẩu tạm)
  if (method === 'POST' && (pathname === '/api/v1/auth/first-time-password' || pathname === '/auth/first-time-password')) {
    const authHeader = req.headers['authorization'] || '';
    if (!authHeader.startsWith('Bearer ')) {
      return sendError(res, 401, 'Yêu cầu đăng nhập để thiết lập mật khẩu mới');
    }
    const body = await parseBody(req);
    const { newPassword } = body;
    if (!newPassword) {
      return sendError(res, 400, 'Vui lòng cung cấp mật khẩu mới');
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

    matchedUser.password = newPassword;
    matchedUser.mustChangePassword = false;
    saveData('users.json', users);
    return sendJson(res, 200, { message: 'Đặt mật khẩu mới thành công. Tài khoản của bạn đã được kích hoạt.' });
  }

  // 1.2 GET /api/v1/profiles/:userId/skills & /profiles/me/skills (Hồ sơ kỹ năng cho Frontend)
  const profileSkillsMatch = pathname.match(/^\/(?:api\/v1\/)?profiles\/(me|[^\/]+)\/skills$/);
  if (method === 'GET' && profileSkillsMatch) {
    const targetId = profileSkillsMatch[1];
    const requester = resolveUserFromRequest(req);
    const cand = requester ? (candidates.find(c => c.email === requester.email) || candidates[0]) : candidates[0];
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

  // 1.4 GET /api/v1/staff/companies & /staff/companies (Danh sách doanh nghiệp - STAFF)
  const staffCompaniesMatch = pathname === '/api/v1/staff/companies' || pathname === '/staff/companies';
  if (method === 'GET' && staffCompaniesMatch) {
    const q = (query.q || '').trim().toLowerCase();
    const status = (query.status || '').trim().toUpperCase();
    const page = parseInt(query.page) || 0;
    const size = parseInt(query.size) || 10;

    let filtered = companies;
    if (q) {
      filtered = filtered.filter(c => 
        (c.name && c.name.toLowerCase().includes(q)) ||
        (c.taxCode && c.taxCode.includes(q))
      );
    }
    if (status && status !== 'ALL') {
      filtered = filtered.filter(c => (c.status || 'ACTIVE') === status);
    }

    const items = filtered.map(c => ({
      id: c.id,
      name: c.name,
      taxCode: c.taxCode,
      industry: c.industry || null,
      size: c.size || null,
      address: c.address || null,
      city: c.city || null,
      logoUrl: c.logoUrl || null,
      status: c.status || 'ACTIVE',
      totalHr: users.filter(u => String(u.companyId) === String(c.id) && u.role === 'HR').length,
      totalJd: (jobsData || []).filter(j => String(j.companyId) === String(c.id)).length
    }));

    const startIndex = page * size;
    const paginatedItems = items.slice(startIndex, startIndex + size);
    const totalElements = items.length;
    const totalPages = Math.ceil(totalElements / size) || 0;

    return sendJson(res, 200, {
      items: paginatedItems,
      pagination: {
        page: page,
        size: size,
        totalElements: totalElements,
        totalPages: totalPages,
        isFirst: page === 0,
        isLast: page >= totalPages - 1,
        hasNext: page < totalPages - 1,
        hasPrevious: page > 0
      },
      totalElements: totalElements,
      totalPages: totalPages,
      page: page,
      size: size
    });
  }

  // 1.5 GET /api/v1/staff/companies/:id & /staff/companies/:id (Chi tiết doanh nghiệp - STAFF)
  const staffCompanyDetailMatch = pathname.match(/^\/(?:api\/v1\/)?staff\/companies\/([^\/]+)$/);
  if (method === 'GET' && staffCompanyDetailMatch) {
    const companyId = staffCompanyDetailMatch[1];
    const company = companies.find(c => String(c.id) === String(companyId));
    if (!company) {
      return sendError(res, 404, `Không tìm thấy doanh nghiệp ${companyId}`);
    }

    const hrAccounts = users
      .filter(u => String(u.companyId) === String(companyId) && u.role === 'HR')
      .map(u => ({
        id: u.id,
        fullName: u.fullName,
        email: u.email,
        status: u.status || 'ACTIVE',
        lockedReason: u.lockedReason || null,
        lockedAt: u.lockedAt || null,
        createdAt: u.createdAt || new Date().toISOString()
      }));

    return sendJson(res, 200, {
      id: company.id,
      name: company.name,
      taxCode: company.taxCode,
      industry: company.industry || null,
      size: company.size || null,
      address: company.address || null,
      city: company.city || null,
      website: company.website || null,
      logoUrl: company.logoUrl || null,
      description: company.description || null,
      status: company.status || 'ACTIVE',
      createdAt: company.createdAt || new Date().toISOString(),
      updatedAt: company.updatedAt || new Date().toISOString(),
      hrAccounts: hrAccounts
    });
  }

  // 1.6 POST /api/v1/staff/companies & /staff/companies (Tạo doanh nghiệp - STAFF)
  if (method === 'POST' && staffCompaniesMatch) {
    const body = await parseBody(req);
    if (!body.name || !body.taxCode) {
      return sendError(res, 400, 'Tên doanh nghiệp và mã số thuế là bắt buộc');
    }
    const newCompany = {
      id: 'comp-' + String(companies.length + 1).padStart(3, '0'),
      name: body.name,
      taxCode: body.taxCode,
      industry: body.industry || null,
      size: body.size || null,
      address: body.address || null,
      city: body.city || null,
      website: body.website || null,
      description: body.description || null,
      logoUrl: null,
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    companies.unshift(newCompany);
    saveData('companies.json', companies);
    return sendJson(res, 201, newCompany);
  }

  // 1.7 POST /api/v1/staff/companies/:id/hr-accounts (Cấp tài khoản HR)
  const staffCreateHrMatch = pathname.match(/^\/(?:api\/v1\/)?staff\/companies\/([^\/]+)\/hr-accounts$/);
  if (method === 'POST' && staffCreateHrMatch) {
    const companyId = staffCreateHrMatch[1];
    const company = companies.find(c => String(c.id) === String(companyId));
    const body = await parseBody(req);
    if (!body.email || !body.fullName) {
      return sendError(res, 400, 'Email và họ tên là bắt buộc');
    }
    const newHr = {
      id: 'user-' + String(users.length + 1).padStart(3, '0'),
      email: body.email,
      password: 'Test@123',
      role: 'HR',
      fullName: body.fullName,
      companyId: companyId,
      companyName: company ? company.name : '',
      mustChangePassword: true,
      status: 'ACTIVE',
      createdAt: new Date().toISOString()
    };
    users.push(newHr);
    saveData('users.json', users);
    return sendJson(res, 201, {
      id: newHr.id,
      email: newHr.email,
      fullName: newHr.fullName,
      role: 'HR',
      companyId: companyId,
      mustChangePassword: true,
      temporaryPassword: 'Test@123',
      message: 'Cấp tài khoản HR thành công'
    });
  }

  // 1.8 POST /api/v1/staff/companies/:id/logo
  const staffLogoMatch = pathname.match(/^\/(?:api\/v1\/)?staff\/companies\/([^\/]+)\/logo$/);
  if (method === 'POST' && staffLogoMatch) {
    const companyId = staffLogoMatch[1];
    return sendJson(res, 200, {
      logoUrl: `/uploads/logos/company-${companyId}.png`,
      message: 'Tải logo thành công'
    });
  }

  // 1.9 POST /api/v1/staff/hr-accounts/:id/lock & unlock
  const staffHrLockMatch = pathname.match(/^\/(?:api\/v1\/)?staff\/hr-accounts\/([^\/]+)\/(lock|unlock)$/);
  if (method === 'POST' && staffHrLockMatch) {
    const targetHrId = staffHrLockMatch[1];
    const action = staffHrLockMatch[2];
    const targetUser = users.find(u => String(u.id) === String(targetHrId));
    if (!targetUser) {
      return sendError(res, 404, `Không tìm thấy tài khoản HR ${targetHrId}`);
    }
    if (action === 'lock') {
      const body = await parseBody(req);
      targetUser.status = 'LOCKED';
      targetUser.lockedReason = body.reason || 'Bị khóa bởi Cán bộ';
      targetUser.lockedAt = new Date().toISOString();
    } else {
      targetUser.status = 'ACTIVE';
      targetUser.lockedReason = null;
      targetUser.lockedAt = null;
    }
    saveData('users.json', users);
    return sendJson(res, 200, {
      id: targetUser.id,
      fullName: targetUser.fullName,
      email: targetUser.email,
      status: targetUser.status,
      lockedReason: targetUser.lockedReason,
      lockedAt: targetUser.lockedAt,
      createdAt: targetUser.createdAt || new Date().toISOString()
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

  const isProfileRoute =
    profilePath === '/profiles/me' ||
    profilePath.startsWith('/profiles/me/');

  const currentUser = isProfileRoute ? resolveUserFromRequest(req) : null;
  const currentCandidate = currentUser
    ? (candidates.find(c => c.email === currentUser.email) || candidates[0])
    : candidates[0];

  if (isProfileRoute && !currentCandidate) {
    return sendError(res, 404, 'Không tìm thấy hồ sơ ứng viên');
  }

  // Gắn định danh người gọi và trạng thái mặc định để FE và luồng HS-05 dùng chung một id
  if (currentCandidate) {
    currentCandidate.userId = req.headers['x-user-id'] || (currentUser ? currentUser.id : 'user-001');
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

  // 16. GET /staff/profile-reviews?status=PENDING&q=&classCode= — chỉ STAFF/ADMIN
  if (method === 'GET' && profilePath === '/staff/profile-reviews') {
    const role = (req.headers['x-user-role'] || '').toUpperCase().replace('ROLE_', '');
    if (!currentUserId) {
      return sendError(res, 401, 'Thiếu header X-User-Id');
    }
    if (role !== 'STAFF' && role !== 'ADMIN') {
      return sendError(res, 403, 'Chức năng chỉ dành cho Cán bộ tuyển dụng (STAFF)');
    }
    const status = (query.status || 'PENDING').toUpperCase();
    const q = (query.q || '').toLowerCase();
    const classCode = query.classCode || '';
    let matched = candidates.filter(c => (c.visibility || 'PRIVATE') === status);
    if (q) {
      matched = matched.filter(c =>
        (c.fullName || '').toLowerCase().includes(q) ||
        (c.email || '').toLowerCase().includes(q)
      );
    }
    if (classCode) {
      matched = matched.filter(c => c.classCode === classCode);
    }
    // Sort by updatedAt ascending (oldest first)
    matched.sort((a, b) => new Date(a.updatedAt || 0) - new Date(b.updatedAt || 0));
    const page = parseInt(query.page) || 0;
    const size = parseInt(query.size) || 20;
    // Calculate rejection counts
    const rejectionCounts = {};
    profileReviews.filter(r => r.action === 'REJECT').forEach(r => {
      rejectionCounts[r.userId] = (rejectionCounts[r.userId] || 0) + 1;
    });
    return sendJson(res, 200, {
      content: matched.slice(page * size, page * size + size).map(c => ({
        userId: c.userId || c.id,
        fullName: c.fullName,
        email: c.email,
        headline: c.headline || null,
        avatarUrl: c.avatarUrl || null,
        classCode: c.classCode || null,
        submittedAt: (profileReviews.find(r => r.userId === (c.userId || c.id) && r.action === 'SUBMIT') || {}).createdAt || null,
        rejectionCount: rejectionCounts[c.userId || c.id] || 0
      })),
      totalElements: matched.length,
      totalPages: Math.ceil(matched.length / size) || 0,
      number: page,
      size: size
    });
  }

  // 17. GET /staff/profiles/:userId/preview — chỉ STAFF/ADMIN
  const previewMatch = profilePath.match(/^\/staff\/profiles\/([^\/]+)\/preview$/);
  if (method === 'GET' && previewMatch) {
    const role = (req.headers['x-user-role'] || '').toUpperCase().replace('ROLE_', '');
    if (!currentUserId) {
      return sendError(res, 401, 'Thiếu header X-User-Id');
    }
    if (role !== 'STAFF' && role !== 'ADMIN') {
      return sendError(res, 403, 'Chức năng chỉ dành cho Cán bộ tuyển dụng (STAFF)');
    }
    const targetId = previewMatch[1];
    const candidate = candidates.find(c => c.userId === targetId || c.id === targetId);
    if (!candidate) {
      return sendError(res, 404, `Không tìm thấy hồ sơ ứng viên ${targetId}`);
    }
    // Mask email and phone
    const maskEmail = (email) => {
      if (!email || !email.includes('@')) return email;
      const [local, domain] = email.split('@');
      if (local.length <= 3) return '***@' + domain;
      return local.substring(0, 3) + '***@' + domain;
    };
    const maskPhone = (phone) => {
      if (!phone || phone.length < 4) return phone;
      return phone.substring(0, 3) + '***' + phone.substring(phone.length - 1);
    };
    // Get review history
    const history = profileReviews.filter(r => r.userId === targetId).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    const rejectionCount = history.filter(r => r.action === 'REJECT').length;
    // Get skills (mock)
    const skills = candidate.skills || [];
    // Get educations
    const educations = candidate.educations || [];
    // Get certificates
    const certificates = candidate.certificates || [];
    return sendJson(res, 200, {
      userId: candidate.userId || candidate.id,
      fullName: candidate.fullName,
      email: candidate.email,
      phone: candidate.phone,
      maskedEmail: maskEmail(candidate.email),
      maskedPhone: maskPhone(candidate.phone),
      dob: candidate.dob,
      gender: candidate.gender,
      address: candidate.address,
      avatarUrl: candidate.avatarUrl,
      headline: candidate.headline,
      summary: candidate.summary,
      program: candidate.program,
      classCode: candidate.classCode,
      courseName: candidate.courseName,
      visibility: candidate.visibility || 'PRIVATE',
      consentAt: candidate.consentAt,
      consentWithdrawnAt: candidate.consentWithdrawnAt,
      updatedAt: candidate.updatedAt,
      skills: skills,
      educations: educations,
      certificates: certificates,
      reviewHistory: history.map(r => ({
        id: r.id,
        action: r.action,
        reason: r.reason,
        actorId: r.actorId,
        createdAt: r.createdAt
      })),
      rejectionCount: rejectionCount
    });
  }

  // 18. PUT /staff/profiles/:userId/self-declared — chỉ STAFF/ADMIN
  const editSelfDeclaredMatch = profilePath.match(/^\/staff\/profiles\/([^\/]+)\/self-declared$/);
  if (method === 'PUT' && editSelfDeclaredMatch) {
    const role = (req.headers['x-user-role'] || '').toUpperCase().replace('ROLE_', '');
    if (!currentUserId) {
      return sendError(res, 401, 'Thiếu header X-User-Id');
    }
    if (role !== 'STAFF' && role !== 'ADMIN') {
      return sendError(res, 403, 'Chức năng chỉ dành cho Cán bộ tuyển dụng (STAFF)');
    }
    const targetId = editSelfDeclaredMatch[1];
    const candidate = candidates.find(c => c.userId === targetId || c.id === targetId);
    if (!candidate) {
      return sendError(res, 404, `Không tìm thấy hồ sơ ứng viên ${targetId}`);
    }
    const body = await parseBody(req);
    const { field, value, reason } = body;
    if (!field || !value || !reason) {
      return sendError(res, 400, 'Thiếu field, value hoặc reason');
    }
    let oldValue;
    switch (field) {
      case 'HEADLINE':
        oldValue = candidate.headline || '';
        candidate.headline = value;
        break;
      case 'SUMMARY':
        oldValue = candidate.summary || '';
        candidate.summary = value;
        break;
      case 'SELF_SKILLS':
        oldValue = JSON.stringify(candidate.skills || []);
        // value should be JSON array
        try {
          candidate.skills = JSON.parse(value);
        } catch (e) {
          return sendError(res, 400, 'SELF_SKILLS value must be valid JSON array');
        }
        break;
      default:
        return sendError(res, 400, 'Trường không được phép sửa: ' + field);
    }
    candidate.updatedAt = new Date().toISOString();
    // Log to profileEdits (in-memory)
    if (!global.profileEdits) global.profileEdits = [];
    global.profileEdits.unshift({
      id: 'edit-' + Date.now(),
      userId: targetId,
      field: field,
      oldValue: oldValue,
      newValue: value,
      reason: reason,
      staffId: currentUserId,
      createdAt: new Date().toISOString()
    });
    return sendJson(res, 200, {
      userId: targetId,
      field: field,
      oldValue: oldValue,
      newValue: value
    });
  }

  // 19. POST /staff/profiles/:userId/approve | /reject
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

  // =============================================================
  // H. UT-07 OFFER MANAGEMENT & EMPLOYMENT RECORD MOCK ENDPOINTS
  // =============================================================
  const ut07Path = pathname.replace(/^\/api\/v1/, '');

  function resolveMockActor(req) {
    const userId = req.headers['x-user-id'];
    if (userId) {
      const found = users.find(u => u.id === userId || u.lmsUserId === userId);
      if (found) return found;
    }
    const roleHeader = (req.headers['x-user-role'] || '').toUpperCase().replace('ROLE_', '');
    if (roleHeader === 'HR') return users.find(u => u.role === 'HR') || users[0];
    if (roleHeader === 'STAFF') return users.find(u => u.role === 'STAFF') || users[0];
    return resolveUserFromRequest(req);
  }

  // GET /hr/applications & /applications (HR)
  if (method === 'GET' && (ut07Path === '/hr/applications' || ut07Path === '/applications')) {
    const actor = resolveMockActor(req);
    const statusFilter = query.status ? query.status.toUpperCase() : null;
    let items = mockApplications.filter(a => !actor.companyId || a.companyId === actor.companyId);
    if (statusFilter) {
      items = items.filter(a => a.status === statusFilter);
    }
    const page = parseInt(query.page) || 0;
    const size = 20;
    const slice = items.slice(page * size, page * size + size).map(a => ({
      id: a.id,
      jobId: a.jobId,
      candidateId: a.candidateId,
      candidateEmail: a.candidateEmail,
      status: a.status,
      createdAt: a.createdAt,
      updatedAt: a.updatedAt
    }));
    return sendJson(res, 200, {
      content: slice,
      totalElements: items.length,
      totalPages: Math.max(1, Math.ceil(items.length / size)),
      size,
      number: page,
      first: page === 0,
      last: (page + 1) * size >= items.length,
      empty: slice.length === 0
    });
  }

  // GET /applications/:id & /recruitments/:id (HR/STAFF detail with offer & employmentRecord)
  const appDetailMatch = ut07Path.match(/^\/(?:applications|recruitments)\/([^\/]+)$/);
  if (method === 'GET' && appDetailMatch) {
    const appId = appDetailMatch[1];
    const app = mockApplications.find(a => a.id === appId);
    if (!app) return sendError(res, 404, 'Không tìm thấy đơn ứng tuyển');
    const offer = mockOffers.find(o => o.applicationId === appId) || null;
    const employmentRecord = mockEmploymentRecords.find(e => e.applicationId === appId) || null;
    return sendJson(res, 200, {
      recruitment: {
        id: app.id,
        jobId: app.jobId,
        cvId: null,
        cvSnapshotUrl: app.cvSnapshotUrl,
        coverLetter: app.coverLetter,
        status: app.status,
        createdAt: app.createdAt,
        updatedAt: app.updatedAt
      },
      candidateId: app.candidateId,
      candidateEmail: app.candidateEmail,
      coverLetter: app.coverLetter,
      cvSnapshotUrl: app.cvSnapshotUrl,
      score: app.score,
      rounds: app.rounds || [],
      history: app.history || [],
      offer: offer ? { ...offer, confirmedAt: employmentRecord ? employmentRecord.confirmedAt : null } : null,
      employmentRecord
    });
  }

  // POST /applications/:id/offer & /recruitments/:id/offer (HR)
  const sendOfferMatch = ut07Path.match(/^\/(?:applications|recruitments)\/([^\/]+)\/offer$/);
  if (method === 'POST' && sendOfferMatch) {
    const appId = sendOfferMatch[1];
    const app = mockApplications.find(a => a.id === appId);
    if (!app) return sendError(res, 404, 'Không tìm thấy đơn ứng tuyển');
    if (app.status !== 'INTERVIEW') {
      return sendError(res, 409, 'Chỉ có thể gửi offer khi đơn đang ở trạng thái INTERVIEW');
    }
    const rounds = app.rounds || [];
    const hasPending = rounds.some(r => !r.result || r.result === 'PENDING');
    const lastRound = rounds[rounds.length - 1];
    if (rounds.length === 0 || hasPending || !lastRound || lastRound.result !== 'PASS') {
      return sendError(res, 409, 'Vòng phỏng vấn cuối cùng chưa PASS, không thể gửi offer');
    }
    if (mockOffers.some(o => o.applicationId === appId)) {
      return sendError(res, 409, 'Đơn ứng tuyển này đã có offer trước đó');
    }
    const body = await parseBody(req);
    if (!body.position || !body.salary || Number(body.salary) <= 0 || !body.startDate || !body.workLocation || !body.responseDeadline) {
      return sendError(res, 400, 'Thiếu thông tin bắt buộc khi gửi offer');
    }
    if (new Date(body.responseDeadline).getTime() <= Date.now()) {
      return sendError(res, 400, 'Hạn trả lời offer phải ở trong tương lai');
    }
    const nowIso = new Date().toISOString();
    app.status = 'OFFER';
    app.updatedAt = nowIso;
    const newOffer = {
      id: 'offer-mock-' + Date.now(),
      applicationId: app.id,
      jobId: app.jobId,
      companyId: app.companyId,
      companyName: body.companyName || app.companyName || 'Công ty Cổ phần Rikkeisoft',
      candidateId: app.candidateId,
      candidateEmail: app.candidateEmail,
      position: String(body.position).trim(),
      salary: Number(body.salary),
      startDate: body.startDate,
      workLocation: String(body.workLocation).trim(),
      note: body.note ? String(body.note).trim() : null,
      responseDeadline: body.responseDeadline,
      status: 'SENT',
      applicationStatus: 'OFFER',
      respondedAt: null,
      declineReason: null,
      confirmedAt: null,
      createdAt: nowIso,
      updatedAt: nowIso
    };
    mockOffers.unshift(newOffer);
    return sendJson(res, 201, newOffer);
  }

  // POST /applications/:id/offer/confirm-onboard (HR)
  const confirmOnboardMatch = ut07Path.match(/^\/(?:applications|recruitments)\/([^\/]+)\/offer\/confirm-onboard$/);
  if (method === 'POST' && confirmOnboardMatch) {
    const appId = confirmOnboardMatch[1];
    const app = mockApplications.find(a => a.id === appId);
    if (!app) return sendError(res, 404, 'Không tìm thấy đơn ứng tuyển');
    const offer = mockOffers.find(o => o.applicationId === appId);
    if (!offer || offer.status !== 'ACCEPTED' || app.status !== 'HIRED') {
      return sendError(res, 409, 'Chỉ có thể xác nhận đi làm khi ứng viên đã chấp nhận offer');
    }
    const emp = mockEmploymentRecords.find(e => e.applicationId === appId);
    if (!emp) return sendError(res, 404, 'Không tìm thấy bản ghi nhận việc');
    if (emp.confirmedAt) return sendError(res, 409, 'Đã xác nhận ứng viên đi làm trước đó');
    emp.confirmedAt = new Date().toISOString();
    offer.confirmedAt = emp.confirmedAt;
    return sendJson(res, 200, emp);
  }

  // GET /me/offers (CANDIDATE)
  if (method === 'GET' && ut07Path === '/me/offers') {
    const candidate = resolveMockActor(req);
    const list = mockOffers
      .filter(o => o.candidateId === candidate.id || o.candidateEmail === candidate.email)
      .map(o => {
        const emp = mockEmploymentRecords.find(e => e.applicationId === o.applicationId);
        return { ...o, confirmedAt: emp ? emp.confirmedAt : null };
      });
    return sendJson(res, 200, list);
  }

  // GET /me/offers/:id (CANDIDATE — 404 if belongs to another candidate)
  const myOfferDetailMatch = ut07Path.match(/^\/me\/offers\/([^\/]+)$/);
  if (method === 'GET' && myOfferDetailMatch) {
    const candidate = resolveMockActor(req);
    const offer = mockOffers.find(o => o.id === myOfferDetailMatch[1] && (o.candidateId === candidate.id || o.candidateEmail === candidate.email));
    if (!offer) return sendError(res, 404, 'Không tìm thấy offer');
    return sendJson(res, 200, offer);
  }

  // POST /me/offers/:id/accept (CANDIDATE)
  const acceptOfferMatch = ut07Path.match(/^\/me\/offers\/([^\/]+)\/accept$/);
  if (method === 'POST' && acceptOfferMatch) {
    const candidate = resolveMockActor(req);
    const offer = mockOffers.find(o => o.id === acceptOfferMatch[1] && (o.candidateId === candidate.id || o.candidateEmail === candidate.email));
    if (!offer) return sendError(res, 404, 'Không tìm thấy offer');
    if (offer.status === 'EXPIRED' || new Date(offer.responseDeadline).getTime() <= Date.now()) {
      offer.status = 'EXPIRED';
      return sendError(res, 409, 'Offer đã quá hạn phản hồi và không thể chấp nhận');
    }
    if (offer.status !== 'SENT') {
      return sendError(res, 409, 'Offer không còn ở trạng thái chờ phản hồi');
    }
    const nowIso = new Date().toISOString();
    offer.status = 'ACCEPTED';
    offer.applicationStatus = 'HIRED';
    offer.respondedAt = nowIso;
    offer.updatedAt = nowIso;
    const app = mockApplications.find(a => a.id === offer.applicationId);
    if (app) {
      app.status = 'HIRED';
      app.updatedAt = nowIso;
    }
    if (!mockEmploymentRecords.some(e => e.applicationId === offer.applicationId)) {
      mockEmploymentRecords.unshift({
        id: 'emp-mock-' + Date.now(),
        userId: candidate.id,
        companyId: offer.companyId,
        companyName: offer.companyName,
        jobId: offer.jobId,
        applicationId: offer.applicationId,
        position: offer.position,
        startDate: offer.startDate,
        confirmedAt: null,
        createdAt: nowIso
      });
    }
    return sendJson(res, 200, offer);
  }

  // POST /me/offers/:id/decline (CANDIDATE — reason required)
  const declineOfferMatch = ut07Path.match(/^\/me\/offers\/([^\/]+)\/decline$/);
  if (method === 'POST' && declineOfferMatch) {
    const body = await parseBody(req);
    if (!body.reason || !String(body.reason).trim()) {
      return sendError(res, 400, 'Lý do từ chối offer là bắt buộc');
    }
    const candidate = resolveMockActor(req);
    const offer = mockOffers.find(o => o.id === declineOfferMatch[1] && (o.candidateId === candidate.id || o.candidateEmail === candidate.email));
    if (!offer) return sendError(res, 404, 'Không tìm thấy offer');
    if (offer.status === 'EXPIRED' || new Date(offer.responseDeadline).getTime() <= Date.now()) {
      offer.status = 'EXPIRED';
      return sendError(res, 409, 'Offer đã quá hạn phản hồi');
    }
    if (offer.status !== 'SENT') {
      return sendError(res, 409, 'Offer không còn ở trạng thái chờ phản hồi');
    }
    const nowIso = new Date().toISOString();
    offer.status = 'DECLINED';
    offer.applicationStatus = 'REJECTED';
    offer.respondedAt = nowIso;
    offer.declineReason = String(body.reason).trim();
    offer.updatedAt = nowIso;
    const app = mockApplications.find(a => a.id === offer.applicationId);
    if (app) {
      app.status = 'REJECTED';
      app.updatedAt = nowIso;
    }
    return sendJson(res, 200, offer);
  }

  // GET /me/employment-records (CANDIDATE)
  if (method === 'GET' && ut07Path === '/me/employment-records') {
    const candidate = resolveMockActor(req);
    const records = mockEmploymentRecords.filter(e => e.userId === candidate.id);
    return sendJson(res, 200, records);
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
    console.log('🔐 Các API hệ thống ngoài yêu cầu header X-Service-Key');
    console.log(`📚 Hệ thống hỗ trợ: LMS, Khảo thí, LMS-PM, Rikkei Meet, Auth & Candidate APIs`);
    console.log(`=======================================================`);
  });
}

module.exports = { server, requestHandler };

