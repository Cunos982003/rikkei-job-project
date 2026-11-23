/**
 * Test Suite kiểm thử tự động Mock Server
 * Chạy bằng lệnh: node mock/test.js
 */

const { EventEmitter } = require('events');

const TEST_SERVICE_KEY = 'test-only-service-key';
process.env.LMS_SERVICE_KEY = TEST_SERVICE_KEY;

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

  // TEST 4: Gọi LMS với header X-Service-Key hợp lệ
  const res4 = await simulateRequest({
    method: 'GET',
    url: '/lms/students/std-001/results',
    headers: { 'x-service-key': TEST_SERVICE_KEY }
  });
  assert(res4.statusCode === 200 && Array.isArray(res4.body) && res4.body.length > 0,
    '4. Gọi API LMS với X-Service-Key hợp lệ trả về bảng điểm của std-001');

  // TEST 5: Gọi Khảo thí với X-Service-Key hợp lệ
  const res5 = await simulateRequest({
    method: 'GET',
    url: '/exam/students/std-001/results',
    headers: { 'x-service-key': TEST_SERVICE_KEY }
  });
  assert(res5.statusCode === 200 && res5.body[0].examCode === 'EXAM-JV-2026',
    '5. Gọi API Khảo thí trả về kết quả thi chính thức');

  // TEST 6: Gọi LMS-PM với X-Service-Key hợp lệ
  const res6 = await simulateRequest({
    method: 'GET',
    url: '/pm/students/std-003/internships',
    headers: { 'x-service-key': TEST_SERVICE_KEY }
  });
  assert(res6.statusCode === 200 && res6.body[0].topicName.includes('Rikkei Jobs'),
    '6. Gọi API LMS-PM trả về kết quả thực tập dự án IOC của std-003');

  // TEST 7: Gọi Rikkei Meet sinh phòng họp
  const res7 = await simulateRequest({
    method: 'POST',
    url: '/meet/rooms',
    headers: { 'x-service-key': TEST_SERVICE_KEY }
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

  // TEST 15: Đổi mật khẩu qua Mock Server
  const res15 = await simulateRequest({
    method: 'POST',
    url: '/api/v1/auth/change-password',
    headers: { authorization: 'Bearer mock-jwt-token-for-hr' }
  }, { currentPassword: 'Test@123', newPassword: 'NewPassword123' });
  assert(res15.statusCode === 200 && res15.body.message === 'Đổi mật khẩu thành công.',
    '15. Endpoint /api/v1/auth/change-password đổi mật khẩu thành công');

  // TEST 16: Phục vụ Cổng Đào Tạo Học Viên LMS Portal HTML
  const res16 = await simulateRequest({ method: 'GET', url: '/lms/portal' });
  assert(res16.statusCode === 200 && typeof res16.body === 'string' && res16.body.includes('Rikkei Education LMS'),
    '16. Endpoint /lms/portal trả về giao diện HTML Cổng Sinh Viên LMS thành công');

  // TEST 17: Tạo vé SSO từ Cổng LMS trả về ticket và redirectUrl
  const res17 = await simulateRequest({
    method: 'POST',
    url: '/lms/sso/tickets'
  }, { lmsUserId: 'std-002' });
  assert(res17.statusCode === 200 && res17.body.ticket.includes('STD-002') && res17.body.redirectUrl.includes('5173/login?ticket='),
    '17. Endpoint POST /lms/sso/tickets cấp vé SSO hợp lệ kèm URL chuyển tiếp tự động');

  // TEST 18: Đặt mật khẩu lần đầu không cần mật khẩu tạm qua Mock Server
  const res18 = await simulateRequest({
    method: 'POST',
    url: '/api/v1/auth/first-time-password',
    headers: { authorization: 'Bearer mock-jwt-token-for-hr' }
  }, { newPassword: 'BrandNewPassword123' });
  assert(res18.statusCode === 200 && res18.body.message.includes('thành công'),
    '18. Endpoint POST /api/v1/auth/first-time-password đặt mật khẩu mới lần đầu thành công');

  // TEST 19: Cấp vé SSO động cho sinh viên mới std-004 và xác thực qua /lms/sso/verify
  const res19Ticket = await simulateRequest({
    method: 'POST',
    url: '/lms/sso/tickets'
  }, { lmsUserId: 'std-004' });
  const res19Verify = await simulateRequest({
    method: 'POST',
    url: '/lms/sso/verify',
    headers: { 'x-service-key': TEST_SERVICE_KEY }
  }, { ticket: res19Ticket.body.ticket });
  assert(res19Verify.statusCode === 200 && res19Verify.body.lmsUserId === 'std-004' && res19Verify.body.email === 'candidate4@rikkeijobs.test',
    '19. Xác thực vé SSO động qua /lms/sso/verify chính xác cho std-004 (Phạm Thu Dung)');

  // TEST 20: Đăng nhập SSO qua /api/v1/auth/sso cho sinh viên std-020 (Hà Thị Trang)
  const res20Ticket = await simulateRequest({
    method: 'POST',
    url: '/lms/sso/tickets'
  }, { lmsUserId: 'std-020' });
  const res20Login = await simulateRequest({
    method: 'POST',
    url: '/api/v1/auth/sso'
  }, { ticket: res20Ticket.body.ticket });
  assert(res20Login.statusCode === 200 && res20Login.body.user.email === 'candidate20@rikkeijobs.test',
    '20. Đăng nhập SSO tự động qua /api/v1/auth/sso thành công cho std-020 (Hà Thị Trang)');

  // TEST 21: Kiểm tra Giao diện LMS Portal chứa cả Quick Select và Tab Đăng Nhập Email/Password
  const res21 = await simulateRequest({ method: 'GET', url: '/lms/portal' });
  const htmlContent = res21.body || '';
  assert(
    res21.statusCode === 200 &&
    htmlContent.includes('tabBtnEmail') &&
    htmlContent.includes('seedAccountsList') &&
    htmlContent.includes('studentSelectDropdown'),
    '21. Giao diện LMS Portal hiển thị đầy đủ Tab Đăng Nhập Email, Datalist Seed và Dropdown 40 sinh viên'
  );

  // TEST 22 [UT-07]: Gửi offer khi vòng phỏng vấn cuối chưa PASS -> 409
  const res22 = await simulateRequest({
    method: 'POST',
    url: '/api/v1/applications/77070000-0000-4000-8000-000000000002/offer',
    headers: { 'x-user-id': 'f4c530f6-3b46-4e5d-b4db-ce699cd3db25', 'x-user-role': 'HR' }
  }, {
    position: 'Frontend Developer',
    salary: 18000000,
    startDate: '2026-12-15',
    workLocation: 'Hà Nội',
    responseDeadline: new Date(Date.now() + 3 * 86400000).toISOString()
  });
  assert(res22.statusCode === 409, '22. [UT-07] Gửi offer khi chưa có vòng PASS trả về HTTP 409');

  // TEST 23 [UT-07]: Gửi offer hợp lệ (INTERVIEW + vòng cuối PASS) -> 201 + SENT
  const res23 = await simulateRequest({
    method: 'POST',
    url: '/api/v1/applications/77070000-0000-4000-8000-000000000001/offer',
    headers: { 'x-user-id': 'f4c530f6-3b46-4e5d-b4db-ce699cd3db25', 'x-user-role': 'HR' }
  }, {
    position: 'Java Backend Developer (Junior)',
    salary: 19500000,
    startDate: '2026-12-10',
    workLocation: 'Tầng 21 Handico, Hà Nội',
    responseDeadline: new Date(Date.now() + 3 * 86400000).toISOString(),
    note: 'Chào mừng gia nhập!'
  });
  assert(res23.statusCode === 201 && res23.body.status === 'SENT',
    '23. [UT-07] HR gửi offer hợp lệ trả về HTTP 201 và trạng thái SENT');

  // TEST 24 [UT-07]: Ứng viên khác mở offer này -> 404
  const res24 = await simulateRequest({
    method: 'GET',
    url: `/api/v1/me/offers/${res23.body.id}`,
    headers: { 'x-user-id': '797c5663-386b-4e96-9112-6d3e414b4449', 'x-user-role': 'CANDIDATE' }
  });
  assert(res24.statusCode === 404, '24. [UT-07] Ứng viên khác mở offer trả về HTTP 404');

  // TEST 25 [UT-07]: Từ chối offer không nhập lý do -> 400
  const res25 = await simulateRequest({
    method: 'POST',
    url: '/api/v1/me/offers/77073000-0000-4000-8000-000000000003/decline',
    headers: { 'x-user-id': 'ed854c07-f3eb-48f1-a186-4e7135f54645', 'x-user-role': 'CANDIDATE' }
  }, { reason: '   ' });
  assert(res25.statusCode === 400, '25. [UT-07] Từ chối offer thiếu lý do trả về HTTP 400');

  // TEST 26 [UT-07]: Quá hạn trả lời (EXPIRED) không bấm Nhận được -> 409
  const res26 = await simulateRequest({
    method: 'POST',
    url: '/api/v1/me/offers/77073000-0000-4000-8000-000000000005/accept',
    headers: { 'x-user-id': 'ed854c07-f3eb-48f1-a186-4e7135f54645', 'x-user-role': 'CANDIDATE' }
  });
  assert(res26.statusCode === 409, '26. [UT-07] Offer quá hạn trả lời (EXPIRED) không thể chấp nhận (HTTP 409)');

  // TEST 27 [UT-07]: Ứng viên chấp nhận offer -> HIRED + tạo employment_records + HR xác nhận onboard
  const res27Accept = await simulateRequest({
    method: 'POST',
    url: `/api/v1/me/offers/${res23.body.id}/accept`,
    headers: { 'x-user-id': 'ed854c07-f3eb-48f1-a186-4e7135f54645', 'x-user-role': 'CANDIDATE' }
  });
  const res27Emp = await simulateRequest({
    method: 'GET',
    url: '/api/v1/me/employment-records',
    headers: { 'x-user-id': 'ed854c07-f3eb-48f1-a186-4e7135f54645', 'x-user-role': 'CANDIDATE' }
  });
  const res27Onboard = await simulateRequest({
    method: 'POST',
    url: '/api/v1/applications/77070000-0000-4000-8000-000000000001/offer/confirm-onboard',
    headers: { 'x-user-id': 'f4c530f6-3b46-4e5d-b4db-ce699cd3db25', 'x-user-role': 'HR' }
  });
  assert(
    res27Accept.statusCode === 200 && res27Accept.body.status === 'ACCEPTED' && res27Accept.body.applicationStatus === 'HIRED' &&
    res27Emp.statusCode === 200 && res27Emp.body.some(e => e.position === 'Java Backend Developer (Junior)') &&
    res27Onboard.statusCode === 200 && Boolean(res27Onboard.body.confirmedAt),
    '27. [UT-07] Ứng viên nhận offer -> HIRED, hồ sơ hiện kinh nghiệm mới, HR xác nhận onboard ghi confirmedAt'
  );

  // 28. GET /api/v1/jobs/job-001 (Chi tiết tin tuyển dụng)
  const res28 = await simulateRequest({
    method: 'GET',
    url: '/api/v1/jobs/job-001'
  });
  assert(
    res28.statusCode === 200 && res28.body.success === true && res28.body.data.id === 'job-001',
    '28. [Candidate] Lấy chi tiết tin tuyển dụng /api/v1/jobs/:id trả về HTTP 200 kèm jobSkills'
  );

  // 29. GET /api/v1/jobs/non-existent-job (Trả về 404)
  const res29 = await simulateRequest({
    method: 'GET',
    url: '/api/v1/jobs/non-existent-job'
  });
  assert(
    res29.statusCode === 404 && res29.body.success === false,
    '29. [Candidate] Lấy tin tuyển dụng không tồn tại trả về HTTP 404'
  );

  // 30. GET /api/v1/candidate/saved-jobs
  const res30 = await simulateRequest({
    method: 'GET',
    url: '/api/v1/candidate/saved-jobs',
    headers: { 'x-user-id': 'cand-001', 'x-user-role': 'CANDIDATE' }
  });
  assert(
    res30.statusCode === 200 && res30.body.success === true && Array.isArray(res30.body.data),
    '30. [Candidate] Lấy danh sách việc làm đã lưu trả về HTTP 200 kèm danh sách công việc'
  );

  // 31. POST /api/v1/candidate/saved-jobs (Lưu việc làm) & DELETE (Bỏ lưu)
  const res31Save = await simulateRequest({
    method: 'POST',
    url: '/api/v1/candidate/saved-jobs',
    headers: { 'x-user-id': 'cand-001', 'x-user-role': 'CANDIDATE', 'content-type': 'application/json' }
  }, { jobId: 'job-003' });
  const res31Delete = await simulateRequest({
    method: 'DELETE',
    url: '/api/v1/candidate/saved-jobs/job-003',
    headers: { 'x-user-id': 'cand-001', 'x-user-role': 'CANDIDATE' }
  });
  assert(
    res31Save.statusCode === 201 && res31Save.body.data.saved === true &&
    res31Delete.statusCode === 200 && res31Delete.body.data.saved === false,
    '31. [Candidate] Thêm và xóa việc làm đã lưu (thả tim) hoạt động chính xác'
  );

  // 32. GET /api/v1/learning/me/gap-analysis?jobId=job-001
  const res32 = await simulateRequest({
    method: 'GET',
    url: '/api/v1/learning/me/gap-analysis?jobId=job-001',
    headers: { 'x-user-id': 'cand-001', 'x-user-role': 'CANDIDATE' }
  });
  assert(
    res32.statusCode === 200 && res32.body.success === true &&
    typeof res32.body.data.readiness === 'number' &&
    Array.isArray(res32.body.data.items),
    '32. [Candidate] Phân tích khoảng thiếu hụt năng lực theo JD trả về đầy đủ readiness và items'
  );

  // 33. GET /api/v1/learning/me/gap-analysis với UUID job từ PostgreSQL seed
  const res33 = await simulateRequest({
    method: 'GET',
    url: '/api/v1/learning/me/gap-analysis?jobId=d0000000-0000-4000-8000-000000000025',
    headers: { 'x-user-id': 'cand-001', 'x-user-role': 'CANDIDATE' }
  });
  assert(
    res33.statusCode === 200 && res33.body.success === true &&
    res33.body.data.target.id === 'd0000000-0000-4000-8000-000000000025' &&
    typeof res33.body.data.readiness === 'number',
    '33. [Candidate] Phân tích năng lực với UUID job từ DB seed trả về kết quả chính xác'
  );

  // 34. GET /api/v1/learning/me/course-suggestions
  const res34 = await simulateRequest({
    method: 'GET',
    url: '/api/v1/learning/me/course-suggestions',
    headers: { 'x-user-id': 'cand-001', 'x-user-role': 'CANDIDATE' }
  });
  assert(
    res34.statusCode === 200 && res34.body.success === true &&
    Array.isArray(res34.body.data),
    '34. [Candidate] Lấy danh sách khóa học LMS gợi ý hoạt động ổn định'
  );

  console.log('\n====================================================');
  console.log(`📊 KẾT QUẢ KIỂM THỬ: ${passed} PASS, ${failed} FAIL`);
  console.log('====================================================');

  if (failed > 0) {
    process.exit(1);
  }
}

runTests();

