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
  assert(res2.statusCode === 401 && res2.body.code === 401, '2. Gọi API LMS không có X-Service-Key trả về HTTP 401');

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
  assert(res7.statusCode === 200 && res7.body.roomUrl.includes('meet.rikkei.edu.vn'), 
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

  console.log('\n====================================================');
  console.log(`📊 KẾT QUẢ KIỂM THỬ: ${passed} PASS, ${failed} FAIL`);
  console.log('====================================================');

  if (failed > 0) {
    process.exit(1);
  }
}

runTests();

