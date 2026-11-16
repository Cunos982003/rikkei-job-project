export const MOCK_MENTOR_EVALUATIONS = [
    {
        id: "eval-001", templateName: "Java Backend – Thực tập", mentorName: "Thầy Nguyễn Văn Minh", context: "INTERNSHIP", submittedAt: "2026-11-10T08:30:00Z",
        overallComment: "Ứng viên có tư duy logic rất tốt, nắm vững kiến thức nền tảng.",
        scores: [
            { criterionId: "crit-java", skillName: "Java Core", description: "OOP", weight: 1.0, score: 90, comment: "Code sạch." },
            { criterionId: "crit-spring", skillName: "Spring Boot", description: "API", weight: 1.2, score: 85, comment: "Thiết kế API chuẩn." },
            { criterionId: "crit-sql", skillName: "SQL", description: "Query", weight: 1.0, score: 65, comment: "Cần cải thiện tối ưu." }
        ]
    }
];