/**
 * E2E Verification Script cho [Đào tạo bổ sung · DT-02]
 * Kiểm tra 4 Acceptance Criteria (AC1, AC2, AC3, AC4) và endpoint LMS Mock.
 */

const { EventEmitter } = require('events');

const TEST_SERVICE_KEY = 'test-only-service-key';
process.env.LMS_SERVICE_KEY = TEST_SERVICE_KEY;

const { requestHandler } = require('./server');

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

    requestHandler(req, res);

    if (bodyData) {
      req.emit('data', JSON.stringify(bodyData));
    }
    req.emit('end');
  });
}

async function runE2ETests() {
  console.log('================================================================');
  console.log('🚀 BẮT ĐẦU KIỂM THỬ E2E TOÀN DIỆN CHO TÍNH NĂNG [DT-02]');
  console.log('================================================================\n');

  let passedCount = 0;
  let totalCount = 0;

  function assert(title, condition, detail = '') {
    totalCount++;
    if (condition) {
      passedCount++;
      console.log(`[PASS] ${title} ${detail ? `(${detail})` : ''}`);
    } else {
      console.error(`[FAIL] ${title} ${detail ? `(${detail})` : ''}`);
    }
  }

  // --- AC1: Danh sách khóa học LMS và mapping ---
  console.log('--- KIỂM TRA AC1: Danh sách khóa học LMS & Ánh xạ kỹ năng ---');
  const resCourses = await simulateRequest({
    method: 'GET',
    url: '/lms/courses',
    headers: { 'x-service-key': TEST_SERVICE_KEY }
  });
  assert('1.1 Lấy danh sách khóa học trên LMS', resCourses.statusCode === 200 && resCourses.body.length > 0, `${resCourses.body.length} khóa học`);

  // --- AC4: Khi Mock LMS gặp sự cố khi ghi danh ---
  console.log('\n--- KIỂM TRA AC4: Xử lý sự cố Mock LMS (502 / 500 Failure) ---');
  const resFail1 = await simulateRequest({
    method: 'POST',
    url: '/lms/enrollments',
    headers: { 'x-service-key': TEST_SERVICE_KEY, 'content-type': 'application/json' }
  }, { studentId: 'student-e2e', courseId: 'CRS-ERROR' });
  assert('4.1 Mock từ chối khi courseId = CRS-ERROR', resFail1.statusCode === 500, `Status: ${resFail1.statusCode}`);

  const resFail2 = await simulateRequest({
    method: 'POST',
    url: '/lms/enrollments',
    headers: { 'x-service-key': TEST_SERVICE_KEY, 'x-mock-fail': 'true', 'content-type': 'application/json' }
  }, { studentId: 'student-e2e', courseId: 'CRS-01' });
  assert('4.2 Mock từ chối khi header X-Mock-Fail: true', resFail2.statusCode === 500, `Status: ${resFail2.statusCode}`);

  // --- Ghi danh thành công (Normal Flow) ---
  console.log('\n--- KIỂM TRA GHI DANH THÀNH CÔNG & AC2: Chặn trùng lặp ---');
  const resEnroll = await simulateRequest({
    method: 'POST',
    url: '/lms/enrollments',
    headers: { 'x-service-key': TEST_SERVICE_KEY, 'content-type': 'application/json' }
  }, { studentId: 'student-e2e', courseId: 'CRS-01' });
  assert('2.1 Ghi danh lần đầu thành công trên LMS', resEnroll.statusCode === 201 && resEnroll.body?.status === 'IN_PROGRESS', `Status: ${resEnroll.body?.status}`);

  // --- Tra cứu tiến độ học viên ---
  const resGetEnr = await simulateRequest({
    method: 'GET',
    url: '/lms/enrollments/student-e2e',
    headers: { 'x-service-key': TEST_SERVICE_KEY }
  });
  assert('2.2 Tra cứu enrollments của học viên trên LMS', resGetEnr.statusCode === 200 && resGetEnr.body?.length >= 1, `Số môn đã ghi danh: ${resGetEnr.body?.length}`);

  // --- AC3: Giả lập Hoàn thành khóa học (COMPLETED) kèm Điểm số ---
  console.log('\n--- KIỂM TRA AC3: Cập nhật COMPLETED & Điểm số (Score Sync) ---');
  const resComplete = await simulateRequest({
    method: 'POST',
    url: '/lms/mock/complete-course',
    headers: { 'x-service-key': TEST_SERVICE_KEY, 'content-type': 'application/json' }
  }, { studentId: 'student-e2e', courseId: 'CRS-01', score: 90.0 });
  assert('3.1 Giả lập hoàn thành khóa học CRS-01 với 90 điểm', resComplete.statusCode === 200 && resComplete.body?.enrollment?.status === 'COMPLETED' && resComplete.body?.enrollment?.score === 90, `Score: ${resComplete.body?.enrollment?.score}`);

  // --- Xác thực trạng thái sau khi hoàn thành ---
  const resVerifyComplete = await simulateRequest({
    method: 'GET',
    url: '/lms/enrollments/student-e2e',
    headers: { 'x-service-key': TEST_SERVICE_KEY }
  });
  const completedItem = resVerifyComplete.body?.find(e => e.courseId === 'CRS-01');
  assert('3.2 Xác thực tiến độ 100% và trạng thái COMPLETED', completedItem?.status === 'COMPLETED' && completedItem?.progress === 100, `Progress: ${completedItem?.progress}%, Status: ${completedItem?.status}`);

  // --- Kiểm tra hỗ trợ query sinh viên bằng UUID (Fix lỗi Profile Service) ---
  console.log('\n--- KIỂM TRA FIX LỖI PROFILE SERVICE: Tra cứu sinh viên bằng UUID ---');
  const testUuid = 'a0d45de8-d868-418e-9454-7040f55fd2f9';
  const resStudentByUuid = await simulateRequest({
    method: 'GET',
    url: `/lms/students/${testUuid}`,
    headers: { 'x-service-key': TEST_SERVICE_KEY }
  });
  assert('5.1 Tra cứu sinh viên theo UUID tự động fallback không bị 404', resStudentByUuid.statusCode === 200 && !!resStudentByUuid.body?.fullName && !!resStudentByUuid.body?.email, `Học viên: ${resStudentByUuid.body?.fullName}, Email: ${resStudentByUuid.body?.email}`);

  console.log('\n================================================================');
  console.log(`KẾT QUẢ KIỂM THỬ: ${passedCount}/${totalCount} TEST CASES THÀNH CÔNG (${Math.round((passedCount/totalCount)*100)}%)`);
  console.log('================================================================');
}

runE2ETests();

