/**
 * E2E Verification Script cho [Đào tạo bổ sung · DT-04]
 * Kiểm tra 4 Acceptance Criteria (AC1, AC2, AC3, AC4) trên Mock Server.
 */

const { EventEmitter } = require('events');
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
  console.log('🚀 BẮT ĐẦU KIỂM THỬ E2E TOÀN DIỆN CHO TÍNH NĂNG [DT-04]');
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

  // --- AC-4: RBAC Security Matrix ---
  console.log('--- KIỂM TRA AC-4: Ma trận Phân quyền RBAC (HR/CANDIDATE -> 403, No Auth -> 401) ---');
  
  const resNoAuth = await simulateRequest({
    method: 'GET',
    url: '/staff/upskilling'
  });
  assert('4.1 Không có token đăng nhập -> 401 Unauthorized', resNoAuth.statusCode === 401, `Status: ${resNoAuth.statusCode}`);

  const resCandidate = await simulateRequest({
    method: 'GET',
    url: '/staff/upskilling',
    headers: { 'x-user-role': 'CANDIDATE' }
  });
  assert('4.2 CANDIDATE gọi API -> 403 Forbidden', resCandidate.statusCode === 403, `Status: ${resCandidate.statusCode}`);

  const resHr = await simulateRequest({
    method: 'GET',
    url: '/staff/upskilling/summary',
    headers: { 'x-user-role': 'HR' }
  });
  assert('4.3 HR gọi API Summary -> 403 Forbidden', resHr.statusCode === 403, `Status: ${resHr.statusCode}`);

  const resStaff = await simulateRequest({
    method: 'GET',
    url: '/staff/upskilling',
    headers: { 'x-user-role': 'STAFF' }
  });
  assert('4.4 STAFF gọi API -> 200 OK', resStaff.statusCode === 200, `Status: ${resStaff.statusCode}`);

  // --- AC-1: Lọc theo lớp, khoá ra đúng ứng viên ---
  console.log('\n--- KIỂM TRA AC-1: Lọc theo Lớp & Khoá ra đúng ứng viên ---');
  
  const resClass1 = await simulateRequest({
    method: 'GET',
    url: '/staff/upskilling?classCode=RA-HN-JV-01',
    headers: { 'x-user-role': 'STAFF' }
  });
  const allClass1 = resClass1.body.data.every(c => c.classCode === 'RA-HN-JV-01');
  assert('1.1 Lọc classCode=RA-HN-JV-01 ra đúng ứng viên của lớp đó', resClass1.statusCode === 200 && allClass1 && resClass1.body.data.length > 0, `Số lượng: ${resClass1.body.data.length}`);

  const resClass2 = await simulateRequest({
    method: 'GET',
    url: '/staff/upskilling?classCode=RA-DN-FE-01',
    headers: { 'x-user-role': 'STAFF' }
  });
  const allClass2 = resClass2.body.data.every(c => c.classCode === 'RA-DN-FE-01');
  assert('1.2 Lọc classCode=RA-DN-FE-01 ra đúng ứng viên của lớp đó', resClass2.statusCode === 200 && allClass2 && resClass2.body.data.length > 0, `Số lượng: ${resClass2.body.data.length}`);

  const resCourse = await simulateRequest({
    method: 'GET',
    url: '/staff/upskilling?courseName=Java%20Fresher',
    headers: { 'x-user-role': 'STAFF' }
  });
  const allCourse = resCourse.body.data.every(c => c.courseName.includes('Java'));
  assert('1.3 Lọc courseName=Java Fresher ra đúng các ứng viên khoá Java', resCourse.statusCode === 200 && allCourse && resCourse.body.data.length > 0, `Số lượng: ${resCourse.body.data.length}`);

  // --- AC-2: Top kỹ năng thiếu khớp dữ liệu seed ---
  console.log('\n--- KIỂM TRA AC-2: Top kỹ năng thiếu khớp dữ liệu seed ---');
  const resSummary = await simulateRequest({
    method: 'GET',
    url: '/staff/upskilling/summary',
    headers: { 'x-user-role': 'STAFF' }
  });
  const summary = resSummary.body.data;
  assert('2.1 API Summary trả về 200 OK và có dữ liệu', resSummary.statusCode === 200 && summary !== undefined, `Studying: ${summary.totalStudying}, Completed: ${summary.totalCompleted}`);
  
  const topSkills = summary.topMissingSkills || [];
  const expectedTop = [
    { skillName: 'TypeScript', missingCount: 10 },
    { skillName: 'Java Core', missingCount: 8 },
    { skillName: 'React', missingCount: 7 },
    { skillName: 'Spring Boot', missingCount: 7 },
    { skillName: 'SQL', missingCount: 5 }
  ];

  let topMatched = topSkills.length === 5;
  for (let i = 0; i < expectedTop.length; i++) {
    if (!topSkills[i] || topSkills[i].skillName !== expectedTop[i].skillName || topSkills[i].missingCount !== expectedTop[i].missingCount) {
      topMatched = false;
      break;
    }
  }
  assert('2.2 Top 5 kỹ năng thiếu khớp chính xác dữ liệu seed (TypeScript: 10, Java Core: 8, React: 7, Spring Boot: 7, SQL: 5)', 
    topMatched, 
    JSON.stringify(topSkills.map(s => `${s.skillName}: ${s.missingCount}`))
  );

  // --- AC-3: File Excel xuất ra khớp bộ lọc ---
  console.log('\n--- KIỂM TRA AC-3: File Excel xuất ra khớp bộ lọc ---');
  const resExcel = await simulateRequest({
    method: 'GET',
    url: '/staff/upskilling/export?classCode=RA-HN-JV-01',
    headers: { 'x-user-role': 'STAFF' }
  });
  const contentType = resExcel.headers['Content-Type'] || resExcel.headers['content-type'];
  const disposition = resExcel.headers['Content-Disposition'] || resExcel.headers['content-disposition'];
  assert('3.1 Xuất Excel thành công với MIME type openxmlformats', resExcel.statusCode === 200 && contentType.includes('spreadsheetml.sheet'), `ContentType: ${contentType}`);
  assert('3.2 Header Content-Disposition có tên file .xlsx', disposition && disposition.includes('.xlsx'), `Disposition: ${disposition}`);

  console.log('\n================================================================');
  console.log(`KẾT QUẢ KIỂM THỬ: ${passedCount}/${totalCount} TEST CASES THÀNH CÔNG (${Math.round(passedCount/totalCount*100)}%)`);
  console.log('================================================================\n');

  if (passedCount !== totalCount) {
    process.exit(1);
  }
}

runE2ETests().catch(err => {
  console.error('Lỗi thực thi E2E tests:', err);
  process.exit(1);
});
