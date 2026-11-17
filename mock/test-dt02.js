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

async function runTests() {
  console.log('🧪 Bắt đầu kiểm tra endpoint LMS cho task DT-02:');

  // Test 1: GET /lms/courses
  const res1 = await simulateRequest({
    method: 'GET',
    url: '/lms/courses',
    headers: { 'x-service-key': TEST_SERVICE_KEY }
  });
  console.log('1. GET /lms/courses:', res1.statusCode === 200 ? '✅ PASS' : '❌ FAIL', `(Found ${res1.body?.length} courses)`);

  // Test 2: POST /lms/enrollments (Thành công)
  const res2 = await simulateRequest({
    method: 'POST',
    url: '/lms/enrollments',
    headers: { 'x-service-key': TEST_SERVICE_KEY, 'content-type': 'application/json' }
  }, { studentId: 'std-test-01', courseId: 'CRS-01' });
  console.log('2. POST /lms/enrollments (Thành công):', res2.statusCode === 201 && res2.body?.status === 'IN_PROGRESS' ? '✅ PASS' : '❌ FAIL');

  // Test 3: POST /lms/enrollments (Mô phỏng lỗi AC4 qua courseId: CRS-ERROR)
  const res3 = await simulateRequest({
    method: 'POST',
    url: '/lms/enrollments',
    headers: { 'x-service-key': TEST_SERVICE_KEY, 'content-type': 'application/json' }
  }, { studentId: 'std-test-01', courseId: 'CRS-ERROR' });
  console.log('3. POST /lms/enrollments (Mô phỏng lỗi AC4 courseId=CRS-ERROR):', res3.statusCode === 500 ? '✅ PASS' : '❌ FAIL');

  // Test 4: POST /lms/enrollments (Mô phỏng lỗi AC4 qua header X-Mock-Fail)
  const res4 = await simulateRequest({
    method: 'POST',
    url: '/lms/enrollments',
    headers: { 'x-service-key': TEST_SERVICE_KEY, 'x-mock-fail': 'true', 'content-type': 'application/json' }
  }, { studentId: 'std-test-01', courseId: 'CRS-02' });
  console.log('4. POST /lms/enrollments (Mô phỏng lỗi AC4 header X-Mock-Fail):', res4.statusCode === 500 ? '✅ PASS' : '❌ FAIL');

  // Test 5: GET /lms/enrollments/:studentId
  const res5 = await simulateRequest({
    method: 'GET',
    url: '/lms/enrollments/std-test-01',
    headers: { 'x-service-key': TEST_SERVICE_KEY }
  });
  console.log('5. GET /lms/enrollments/std-test-01:', res5.statusCode === 200 && Array.isArray(res5.body) && res5.body.length > 0 ? '✅ PASS' : '❌ FAIL');

  // Test 6: POST /lms/mock/complete-course (AC3 - Hoàn thành khóa học kèm điểm)
  const res6 = await simulateRequest({
    method: 'POST',
    url: '/lms/mock/complete-course',
    headers: { 'x-service-key': TEST_SERVICE_KEY, 'content-type': 'application/json' }
  }, { studentId: 'std-test-01', courseId: 'CRS-01', score: 92.5 });
  console.log('6. POST /lms/mock/complete-course (AC3):', res6.statusCode === 200 && res6.body?.enrollment?.status === 'COMPLETED' && res6.body?.enrollment?.score === 92.5 ? '✅ PASS' : '❌ FAIL');

  // Test 7: GET /lms/enrollments/:studentId sau khi hoàn thành
  const res7 = await simulateRequest({
    method: 'GET',
    url: '/lms/enrollments/std-test-01',
    headers: { 'x-service-key': TEST_SERVICE_KEY }
  });
  const completed = res7.body?.find(e => e.courseId === 'CRS-01');
  console.log('7. Kiểm tra trạng thái COMPLETED & Score:', completed?.status === 'COMPLETED' && completed?.progress === 100 ? '✅ PASS' : '❌ FAIL');
}

runTests();
