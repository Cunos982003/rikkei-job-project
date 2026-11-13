/**
 * Test Suite kiểm thử tự động Mock Server
 * Chạy bằng lệnh: node mock/test.js
 */

const { EventEmitter } = require('events');
const { requestHandler } = require('./server');

// Tạo mock request và response để kiểm thử handler
function simulateRequest(options, bodyData = null) {
  return new Promise((resolve) => {
    const req = new EventEmitter();
    req.method = options.method || 'GET';
    req.url = options.url || '/';
    req.headers = options.headers || {};

    let responseStatusCode = 200;
    let responseHeaders = {};
    let responseBody = '';

    const res = {
      writeHead(status, headers) {
        responseStatusCode = status;
        responseHeaders = { ...responseHeaders, ...headers };
      },
      end(chunk) {
        if (chunk) responseBody += chunk;
        let parsed = null;
        try {
          parsed = JSON.parse(responseBody);
        } catch (e) {
          parsed = responseBody;
        }
        resolve({
          statusCode: responseStatusCode,
          headers: responseHeaders,
          body: parsed
        });
      }
    };

    // Gọi request handler
    requestHandler(req, res);

    // Gửi body nếu có
    if (bodyData) {
      req.emit('data', typeof bodyData === 'string' ? bodyData : JSON.stringify(bodyData));
    }
    req.emit('end');
  });
}

async function runTests() {
  console.log('====================================================');
  console.log('🧪 BẮT ĐẦU KIỂM THỬ TỰ ĐỘNG RIKKEI JOBS MOCK SERVER');
  console.log('====================================================\n');

  let passed = 0;
  let failed = 0;

  function assert(condition, testName, details = '') {
    if (condition) {
      console.log(`✅ [PASS] ${testName}`);
      passed++;
    } else {
      console.error(`❌ [FAIL] ${testName} - ${details}`);
      failed++;
    }
  }

  // TEST 1: Health check
  const res1 = await simulateRequest({ method: 'GET', url: '/health' });
  assert(res1.statusCode === 200 && res1.body.status === 'UP', '1. Endpoint Health Check hoạt động bình thường');

  // TEST 2: Gọi LMS không có header X-Service-Key (Phải trả về 401)
  const res2 = await simulateRequest({ method: 'GET', url: '/lms/students/std-001/results' });
  assert(res2.statusCode === 401 && (res2.body.code === 401 || res2.body.code === 'INVALID_SERVICE_KEY'), '2. Gọi API LMS không có X-Service-Key trả về HTTP 401');

  // TEST 3: Gọi LMS với header X-Service-Key sai (Phải trả về 401)
  const res3 = await simulateRequest({
    method: 'GET',
    url: '/lms/students/std-001/results',
    headers: { 'x-service-key': 'sai-khoa' }
  });
  assert(res3.statusCode === 401, '3. Gọi API LMS với X-Service-Key sai trả về HTTP 401');

  // TEST 4: Gọi LMS với header đúng X-Service-Key: dev-key (Phải trả về 200 và kết quả học tập)
  const res4 = await simulateRequest({
    method: 'GET',
    url: '/lms/students/std-001/results',
    headers: { 'x-service-key': 'dev-key' }
  });
  assert(res4.statusCode === 200 && Array.isArray(res4.body) && res4.body.length > 0,
    '4. Gọi API LMS với X-Service-Key: dev-key trả về 200 và bảng điểm của std-001');

  // TEST 5: Gọi Khảo thí với X-Service-Key: dev-key
  const res5 = await simulateRequest({
    method: 'GET',
    url: '/exam/students/std-001/results',
    headers: { 'x-service-key': 'dev-key' }
  });
  assert(res5.statusCode === 200 && res5.body[0].examCode === 'EXAM-JV-2026',
    '5. Gọi API Khảo thí trả về kết quả thi chính thức');

  // TEST 6: Gọi LMS-PM với X-Service-Key: dev-key
  const res6 = await simulateRequest({
    method: 'GET',
    url: '/pm/students/std-003/internships',
    headers: { 'x-service-key': 'dev-key' }
  });
  assert(res6.statusCode === 200 && res6.body[0].topicName.includes('Rikkei Jobs'),
    '6. Gọi API LMS-PM trả về kết quả thực tập dự án IOC của std-003');

  // TEST 7: Gọi Rikkei Meet sinh phòng họp
  const res7 = await simulateRequest({
    method: 'POST',
    url: '/meet/rooms',
    headers: { 'x-service-key': 'dev-key' }
  }, { title: 'Phỏng vấn ứng viên Nguyễn Văn An' });
  assert((res7.statusCode === 200 || res7.statusCode === 201) && res7.body.roomUrl && res7.body.roomUrl.includes('meet.rikkei.edu.vn'),
    '7. Gọi API Rikkei Meet sinh URL phòng họp giả lập thành công');

  // TEST 8: Phân trang ứng viên ?page=1&size=20
  const res8 = await simulateRequest({
    method: 'GET',
    url: '/api/v1/candidates?page=1&size=20'
  });
  assert(res8.statusCode === 200 && res8.body.page === 1 && res8.body.total === 3 && res8.body.items.length === 3,
    '8. Danh sách ứng viên trả về chuẩn phân trang { items, total: 3, page: 1, size: 20 }');

  // TEST 9: Danh mục 20 kỹ năng mẫu
  const res9 = await simulateRequest({ method: 'GET', url: '/api/v1/skills' });
  assert(res9.statusCode === 200 && res9.body.length === 20,
    '9. Endpoint /api/v1/skills trả về đầy đủ 20 kỹ năng mẫu');

  // TEST 10: Đăng nhập với tài khoản mẫu và mật khẩu Test@123
  const res10 = await simulateRequest({
    method: 'POST',
    url: '/api/v1/auth/login'
  }, { email: 'candidate1@rikkeijobs.test', password: 'Test@123' });
  assert(res10.statusCode === 200 && res10.body.accessToken && res10.body.user.role === 'CANDIDATE',
    '10. Đăng nhập thành công với tài khoản mẫu candidate1@rikkeijobs.test');

  // TEST 11: Đăng nhập mật khẩu sai
  const res11 = await simulateRequest({
    method: 'POST',
    url: '/api/v1/auth/login'
  }, { email: 'candidate1@rikkeijobs.test', password: 'SaiMatKhau' });
  assert(res11.statusCode === 401, '11. Đăng nhập mật khẩu sai trả về HTTP 401');

  // TEST 12: Hồ sơ kỹ năng cá nhân cho Frontend
  const res12 = await simulateRequest({ method: 'GET', url: '/api/v1/profiles/me/skills' });
  assert(res12.statusCode === 200 && Array.isArray(res12.body) && res12.body.length >= 8 && res12.body[0].evidences,
    '12. Endpoint /api/v1/profiles/me/skills trả về danh sách kỹ năng chuẩn format với evidences');

  // TEST 13: Danh sách đánh giá Mentor từ evaluations.json
  const res13 = await simulateRequest({ method: 'GET', url: '/api/v1/profiles/me/evaluations' });
  assert(res13.statusCode === 200 && Array.isArray(res13.body) && res13.body.length >= 2 && res13.body[0].scores,
    '13. Endpoint /api/v1/profiles/me/evaluations trả về phiếu đánh giá mentor từ mock/data/evaluations.json');

  // TEST 14: Danh sách tài khoản demo phục vụ đăng nhập nhanh
  const res14 = await simulateRequest({ method: 'GET', url: '/api/v1/auth/demo-accounts' });
  assert(res14.statusCode === 200 && Array.isArray(res14.body) && res14.body.length === 5 && res14.body[0].role === 'CANDIDATE', 
    '14. Endpoint /api/v1/auth/demo-accounts trả về 5 tài khoản mẫu cho 5 vai trò');

  // TEST 15: Mock tool POST /mock/reload
  const res15 = await simulateRequest({ method: 'POST', url: '/mock/reload' });
  assert(res15.statusCode === 200 && res15.body.ok === true,
    '15. Endpoint /mock/reload nạp lại data.json thành công');

  // TEST 16: Mock tool GET /mock/requests
  const res16 = await simulateRequest({ method: 'GET', url: '/mock/requests' });
  assert(res16.statusCode === 200 && Array.isArray(res16.body) && res16.body.length > 0 && res16.body[0].method,
    '16. Endpoint /mock/requests ghi nhận lịch sử các request đã gọi');

  // TEST 17: Ghi danh khóa học POST /lms/enrollments và PATCH /mock/enrollments
  const res17a = await simulateRequest({
    method: 'POST',
    url: '/lms/enrollments',
    headers: { 'x-service-key': 'dev-key' }
  }, { lmsUserId: 'lms-sv-001', courseId: 'LMS-C-JAVA-02' });
  assert(res17a.statusCode === 201 && res17a.body.status === 'ENROLLED' && res17a.body.courseId === 'LMS-C-JAVA-02',
    '17a. Ghi danh khóa học POST /lms/enrollments thành công (HTTP 201)');

  const res17b = await simulateRequest({
    method: 'PATCH',
    url: '/mock/enrollments'
  }, { lmsUserId: 'lms-sv-001', courseId: 'LMS-C-JAVA-02', progress: 100, score: 9.5, status: 'COMPLETED' });
  assert(res17b.statusCode === 200 && res17b.body.status === 'COMPLETED' && res17b.body.progress === 100,
    '17b. Cập nhật tiến độ học tập PATCH /mock/enrollments thành công');

  // TEST 18: Ghi danh trùng lặp trả về 409 ALREADY_ENROLLED
  const res18 = await simulateRequest({
    method: 'POST',
    url: '/lms/enrollments',
    headers: { 'x-service-key': 'dev-key' }
  }, { lmsUserId: 'lms-sv-001', courseId: 'LMS-C-JAVA-02' });
  assert(res18.statusCode === 409 && (res18.body.code === 409 || res18.body.code === 'ALREADY_ENROLLED'),
    '18. Ghi danh trùng lặp trả về HTTP 409 ALREADY_ENROLLED');

  // TEST 19: Ghép học viên vào đợt thực tập POST /pm/batches/:id/members và duplicate 409
  const res19a = await simulateRequest({
    method: 'POST',
    url: '/pm/batches/IOC-JB-2026-09/members',
    headers: { 'x-service-key': 'dev-key' }
  }, { lmsUserId: 'lms-sv-001' });
  assert(res19a.statusCode === 201 && res19a.body.batchId === 'IOC-JB-2026-09',
    '19a. Ghép học viên vào đợt thực tập POST /pm/batches/:id/members thành công');

  const res19b = await simulateRequest({
    method: 'POST',
    url: '/pm/batches/IOC-JB-2026-09/members',
    headers: { 'x-service-key': 'dev-key' }
  }, { lmsUserId: 'lms-sv-001' });
  assert(res19b.statusCode === 409 && (res19b.body.code === 409 || res19b.body.code === 'ALREADY_MEMBER'),
    '19b. Ghép học viên trùng lặp vào đợt thực tập trả về HTTP 409 ALREADY_MEMBER');

  // TEST 20: Luồng SSO ticket theo contract
  const res20a = await simulateRequest({
    method: 'POST',
    url: '/lms/sso/tickets',
    headers: { 'x-service-key': 'dev-key' }
  }, { lmsUserId: 'lms-sv-002' });
  assert(res20a.statusCode === 201 && res20a.body.ticket && res20a.body.ticket.includes('LMS-SV-002'),
    '20a. Tạo SSO ticket POST /lms/sso/tickets trả về HTTP 201 và ticket');

  const res20b = await simulateRequest({
    method: 'POST',
    url: '/lms/sso/verify',
    headers: { 'x-service-key': 'dev-key' }
  }, { ticket: res20a.body.ticket });
  assert(res20b.statusCode === 200 && res20b.body.email === 'candidate2@rikkeijobs.test' && res20b.body.role === 'CANDIDATE',
    '20b. Xác thực SSO ticket POST /lms/sso/verify trả về thông tin đúng người dùng');

  // TEST 21: Lấy thông tin sinh viên GET /lms/students/:id theo data.json
  const res21 = await simulateRequest({
    method: 'GET',
    url: '/lms/students/lms-sv-001',
    headers: { 'x-service-key': 'dev-key' }
  });
  assert(res21.statusCode === 200 && res21.body.lmsUserId === 'lms-sv-001' && res21.body.classCode === 'KS25-CNTT1',
    '21. Lấy thông tin sinh viên GET /lms/students/lms-sv-001 chính xác');

  // TEST 22: Lọc khóa học theo kỹ năng GET /lms/courses?skill=JAVA
  const res22 = await simulateRequest({
    method: 'GET',
    url: '/lms/courses?skill=JAVA',
    headers: { 'x-service-key': 'dev-key' }
  });
  assert(res22.statusCode === 200 && Array.isArray(res22.body) && res22.body.length > 0 && res22.body.every(c => c.skills.includes('JAVA')),
    '22. Lọc danh sách khóa học theo kỹ năng JAVA chính xác');

  // TEST 23: Mock tool POST /mock/reset
  const res23 = await simulateRequest({ method: 'POST', url: '/mock/reset' });
  assert(res23.statusCode === 200 && res23.body.ok === true,
    '23. Endpoint /mock/reset làm sạch toàn bộ trạng thái tạm thời');

  console.log('\n====================================================');
  console.log(`📊 KẾT QUẢ KIỂM THỬ: ${passed} PASS, ${failed} FAIL`);
  console.log('====================================================');

  if (failed > 0) {
    process.exit(1);
  }
}

runTests();

