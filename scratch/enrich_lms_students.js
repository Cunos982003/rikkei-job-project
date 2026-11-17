const fs = require('fs');
const path = require('path');

const usersPath = path.join(__dirname, '..', 'mock', 'data', 'users.json');
const lmsPath = path.join(__dirname, '..', 'mock', 'data', 'lms.json');

const users = JSON.parse(fs.readFileSync(usersPath, 'utf8'));
const lmsData = JSON.parse(fs.readFileSync(lmsPath, 'utf8'));

const candidates = users.filter(u => u.role === 'CANDIDATE' && u.lmsUserId);

const tracks = [
  {
    program: "Lập trình Java Fullstack Chuyên sâu",
    classCode: "RA-JAVA-2026",
    courseName: "Khóa đào tạo Kỹ sư Phần mềm Java",
    subjects: [
      { code: "JV-01", name: "Java Core & OOP" },
      { code: "DB-01", name: "Cơ sở dữ liệu SQL & PostgreSQL" },
      { code: "SB-01", name: "Spring Boot RESTful Microservices" },
      { code: "GIT-01", name: "Quản trị mã nguồn Git & Docker" }
    ]
  },
  {
    program: "Lập trình Web Frontend Chuyên nghiệp",
    classCode: "RA-FE-2026",
    courseName: "Khóa đào tạo Chuyên viên Frontend React",
    subjects: [
      { code: "FE-01", name: "HTML5, CSS3 & Responsive Design" },
      { code: "JS-01", name: "JavaScript ES6+ & Asynchronous" },
      { code: "RC-01", name: "React 19 & TypeScript" },
      { code: "TW-01", name: "Tailwind CSS & UI Components" }
    ]
  },
  {
    program: "Đào tạo Kỹ sư Phần mềm Toàn diện",
    classCode: "RA-FS-2026",
    courseName: "Khóa đào tạo Kỹ sư Phần mềm Fullstack",
    subjects: [
      { code: "JV-01", name: "Java Core & OOP" },
      { code: "SB-01", name: "Spring Boot & Cloud Native" },
      { code: "RC-01", name: "React 19 & State Management" },
      { code: "DK-01", name: "Docker & Kafka Message Queue" }
    ]
  },
  {
    program: "Kỹ sư Dữ liệu & AI Khởi tạo",
    classCode: "RA-AI-2026",
    courseName: "Khóa đào tạo Kỹ sư Trí tuệ Nhân tạo & Data",
    subjects: [
      { code: "PY-01", name: "Python & Data Analysis Fundamentals" },
      { code: "ML-01", name: "Machine Learning & Deep Learning" },
      { code: "AI-01", name: "RAG & LLM Application Development" },
      { code: "ML-02", name: "Data Pipeline & MLOps Architecture" }
    ]
  },
  {
    program: "Lập trình Di động Đa nền tảng",
    classCode: "RA-MOB-2026",
    courseName: "Khóa đào tạo Lập trình Di động Flutter & React Native",
    subjects: [
      { code: "FL-01", name: "Dart & Flutter Architecture" },
      { code: "RN-01", name: "React Native & Expo Ecosystem" },
      { code: "MOB-01", name: "Mobile UI/UX & State Management" },
      { code: "MOB-02", name: "REST API & Offline SQLite Storage" }
    ]
  },
  {
    program: "Kiểm thử Phần mềm & Tự động hóa QA/QC",
    classCode: "RA-QA-2026",
    courseName: "Khóa đào tạo Chuyên viên Kiểm thử Tự động",
    subjects: [
      { code: "QA-01", name: "Software Testing Fundamentals" },
      { code: "QA-02", name: "Selenium & Playwright Automation" },
      { code: "QA-03", name: "API Testing with Postman & RestAssured" },
      { code: "QA-04", name: "Performance Testing with JMeter" }
    ]
  },
  {
    program: "Kỹ sư Điện toán Đám mây & DevOps",
    classCode: "RA-CLOUD-2026",
    courseName: "Khóa đào tạo Kỹ sư DevOps & Cloud AWS",
    subjects: [
      { code: "LX-01", name: "Linux System Administration" },
      { code: "K8S-01", name: "Docker & Kubernetes Orchestration" },
      { code: "AWS-01", name: "AWS Cloud Infrastructure & Terraform" },
      { code: "CICD-01", name: "CI/CD Pipeline with GitHub Actions" }
    ]
  },
  {
    program: "Lập trình Golang & Microservices Hiệu năng cao",
    classCode: "RA-GO-2026",
    courseName: "Khóa đào tạo Golang Backend Developer",
    subjects: [
      { code: "GO-01", name: "Golang Core & Concurrency Patterns" },
      { code: "GO-02", name: "Gin / Echo REST APIs & GORM" },
      { code: "DB-02", name: "PostgreSQL & Redis Caching High Load" },
      { code: "GRPC-01", name: "Microservices Architecture with gRPC" }
    ]
  }
];

if (!lmsData.students) {
  lmsData.students = {};
}

candidates.forEach((cand, idx) => {
  const stdId = cand.lmsUserId.toLowerCase();
  
  // Keep original std-001, std-002, std-003 exactly as is to guarantee existing tests pass
  if (['std-001', 'std-002', 'std-003'].includes(stdId) && lmsData.students[stdId]) {
    // Ensure email and fullName match user record
    lmsData.students[stdId].email = cand.email;
    lmsData.students[stdId].fullName = cand.fullName;
    return;
  }

  const track = tracks[idx % tracks.length];
  
  // Pseudo-random deterministic scores based on student index
  const baseScore = 7.5 + ((idx * 7) % 22) / 10; // 7.5 to 9.6
  const results = track.subjects.map((sub, sIdx) => {
    const scoreDiff = (((idx * 13 + sIdx * 7) % 15) - 5) / 10; // -0.5 to +0.9
    let score = Math.min(9.8, Math.max(7.2, +(baseScore + scoreDiff).toFixed(1)));
    let attendance = 90 + ((idx * 5 + sIdx * 3) % 11); // 90% to 100%
    let homework = 85 + ((idx * 9 + sIdx * 5) % 16); // 85% to 100%
    let rpoint = Math.round(score * 15 + (homework > 90 ? 20 : 0));

    return {
      subjectCode: sub.code,
      subjectName: sub.name,
      score: score,
      attendanceRate: attendance,
      homeworkRate: homework,
      rpoint: rpoint
    };
  });

  lmsData.students[stdId] = {
    lmsUserId: cand.lmsUserId,
    email: cand.email,
    fullName: cand.fullName,
    role: "STUDENT",
    program: track.program,
    classCode: track.classCode,
    courseName: track.courseName,
    results: results
  };
});

fs.writeFileSync(lmsPath, JSON.stringify(lmsData, null, 2), 'utf8');
console.log(`Successfully updated ${lmsPath}`);
console.log(`Total students now in lms.json: ${Object.keys(lmsData.students).length}`);
