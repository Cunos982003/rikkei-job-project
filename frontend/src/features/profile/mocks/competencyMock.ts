export const MOCK_COMPETENCY = {
    overallScore: 78,
    level: "Khá",
    skills: [
        { skillId: "s1", skillName: "Java Core", verifiedScore: 85, level: "Nâng cao", contributions: [{ source: 'LMS', score: 80, weight: 30 }, { source: 'EXAM', score: 90, weight: 20 }, { source: 'MENTOR', score: 88, weight: 25 }] },
        { skillId: "s2", skillName: "Spring Boot", verifiedScore: 72, level: "Vững", contributions: [{ source: 'LMS', score: 70, weight: 30 }, { source: 'MENTOR', score: 75, weight: 25 }] },
        { skillId: "s3", skillName: "SQL", verifiedScore: 65, level: "Khá", contributions: [{ source: 'EXAM', score: 60, weight: 20 }, { source: 'INTERNSHIP', score: 70, weight: 25 }] }
    ],
    badges: [
        { id: "b1", name: "Java Thành thạo", icon: "☕", ruleDescription: "Điểm Java ≥ 85 và có bằng chứng từ ít nhất 2 nguồn.", isGranted: true, grantedAt: "2026-10-15T08:00:00Z" },
        { id: "b2", name: "Hoàn thành IOC", icon: "🎓", ruleDescription: "Hoàn thành đợt thực tập với điểm cuối kỳ ≥ 7.0.", isGranted: true, grantedAt: "2026-11-01T10:30:00Z" },
        { id: "b3", name: "Bậc thầy SQL", icon: "🗄️", ruleDescription: "Điểm SQL ≥ 90.", isGranted: false }
    ]
};