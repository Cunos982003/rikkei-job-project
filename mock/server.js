/**
 * Rikkei Jobs Mock Server
 * Cổng: 9000
 * Yêu cầu: Xác thực header X-Service-Key: dev-key cho các endpoint hệ thống ngoài (LMS, Khảo thí, PM, Meet)
 * Hỗ trợ: CORS, JSON parsing, phân trang (?page=1&size=20), mã lỗi HTTP chuẩn (400, 401, 403, 404, 409, 429, 500)
 */

const http = require('http');
const url = require('url');
const fs = require('fs');
const path = require('path');

const PORT = 9000;
const SERVICE_KEY = 'dev-key';

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

const users = loadData('users.json') || [];
const candidates = loadData('candidates.json') || [];
const lmsData = loadData('lms.json') || { students: {}, courses: [], enrollments: [] };
const examData = loadData('exam.json') || {};
const pmData = loadData('pm.json') || { internships: {}, batches: [] };
const skillsData = loadData('skills.json') || [];
const jobsData = loadData('jobs.json') || [];

// Helper helper gửi response JSON
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

