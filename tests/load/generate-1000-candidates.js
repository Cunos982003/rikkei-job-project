/**
 * NT-08 / NT-16: Script sinh dữ liệu mẫu 1.000 hồ sơ ứng viên phục vụ kiểm thử tải (Load Testing)
 */
const fs = require('fs');
const path = require('path');

const firstNames = ["Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương"];
const middleNames = ["Văn", "Thị", "Đức", "Thành", "Hải", "Tuấn", "Minh", "Quang", "Anh", "Hồng", "Phương", "Ngọc"];
const lastNames = ["An", "Bình", "Cường", "Dũng", "Em", "Giang", "Huy", "Hùng", "Khánh", "Long", "Nam", "Phong", "Quân", "Sơn", "Thắng", "Tuấn", "Việt", "Xuân"];

const jobTitles = [
    { title: "Java Backend Developer", skills: ["Java", "Spring Boot", "PostgreSQL", "Docker"] },
    { title: "React Frontend Developer", skills: ["React", "TypeScript", "Tailwind CSS", "Redux"] },
    { title: "Fullstack Developer", skills: ["Java", "React", "Spring Boot", "MySQL"] },
    { title: "DevOps Engineer", skills: ["Docker", "Kubernetes", "CI/CD", "Linux", "AWS"] },
    { title: "Node.js Backend Developer", skills: ["Node.js", "Express", "MongoDB", "Redis"] },
    { title: "Mobile Flutter Developer", skills: ["Flutter", "Dart", "REST API", "Firebase"] },
    { title: "Data Engineer / Analyst", skills: ["Python", "SQL", "Spark", "PostgreSQL"] }
];

const programs = [
    "Lập trình Java Fullstack Chuyên sâu",
    "Frontend ReactJS Chuyên nghiệp",
    "Kỹ sư Cầu nối Nhật Bản (BrSE)",
    "DevOps & Cloud Computing",
    "Lập trình Di động Đa nền tảng"
];

function generate1000Candidates() {
    const candidates = [];
    for (let i = 1; i <= 1000; i++) {
        const id = `cand-load-${String(i).padStart(4, '0')}`;
        const fn = firstNames[i % firstNames.length];
        const mn = middleNames[i % middleNames.length];
        const ln = lastNames[i % lastNames.length];
        const fullName = `${fn} ${mn} ${ln}`;
        const roleInfo = jobTitles[i % jobTitles.length];
        const program = programs[i % programs.length];

        const candidate = {
            id: id,
            userId: `00000000-0000-4000-8000-${String(i).padStart(12, '0')}`,
            fullName: fullName,
            email: `candidate.${i}@rikkeijobs.loadtest`,
            phone: `09${String(10000000 + i).substring(1)}`,
            avatarUrl: `https://api.dicebear.com/7.x/avataaars/svg?seed=cand-${i}`,
            title: roleInfo.title,
            headline: `Học viên xuất sắc khóa ${program}, thành thạo ${roleInfo.skills.join(', ')}`,
            program: program,
            overallScore: Math.floor(65 + Math.random() * 30),
            skills: roleInfo.skills.map(s => ({
                name: s,
                score: Math.floor(70 + Math.random() * 28),
                verified: Math.random() > 0.3
            })),
            isPublic: true,
            createdAt: new Date(Date.now() - (1000 - i) * 3600000).toISOString()
        };
        candidates.push(candidate);
    }
    return candidates;
}

const outputPath = path.join(__dirname, 'candidates-1000.json');
const dataset = generate1000Candidates();
fs.writeFileSync(outputPath, JSON.stringify(dataset, null, 2), 'utf-8');
console.log(`Đã tạo thành công 1.000 hồ sơ ứng viên tại: ${outputPath}`);
