import type { MentorEvaluation } from '@/api/types/evaluation';

export const MOCK_MENTOR_EVALUATIONS: MentorEvaluation[] = [
    {
        id: "eval-001",
        templateName: "Java Backend – Thực tập",
        mentorName: "Thầy Nguyễn Văn Minh",
        context: "INTERNSHIP",
        batchId: "batch-java-k20",
        interviewRoundId: null,
        overallComment: "Ứng viên có tư duy logic rất tốt, nắm vững kiến thức nền tảng về Java. Trong quá trình thực tập, em làm việc chủ động, giao tiếp tốt với team. Cần cải thiện thêm kỹ năng tối ưu hóa câu lệnh SQL và viết Unit Test.",
        submittedAt: "2026-11-10T08:30:00Z",
        scores: [
            {
                criterionId: "crit-java",
                skillName: "Java Core",
                description: "Nắm vững OOP, Collections, Multithreading",
                weight: 1.0,
                score: 90,
                comment: "Code sạch, hiểu sâu về đa hình và xử lý đa luồng."
            },
            {
                criterionId: "crit-spring",
                skillName: "Spring Boot",
                description: "Xây dựng REST API, làm việc với JPA",
                weight: 1.2,
                score: 85,
                comment: "Biết thiết kế API chuẩn RESTful, sử dụng Spring Data JPA thành thạo."
            },
            {
                criterionId: "crit-sql",
                skillName: "SQL",
                description: "Viết truy vấn phức tạp, tối ưu Index",
                weight: 1.0,
                score: 65,
                comment: "Viết được query cơ bản, nhưng chưa biết cách phân tích Explain Plan để tối ưu."
            },
            {
                criterionId: "crit-git",
                skillName: "Git",
                description: "Quản lý source code, làm việc nhóm",
                weight: 0.8,
                score: 80,
                comment: "Sử dụng thành thạo branch, merge và resolve conflict."
            }
        ]
    },
    {
        id: "eval-002",
        templateName: "Phỏng vấn kỹ thuật chung",
        mentorName: "Chị Trần Thị Hương",
        context: "INTERVIEW",
        batchId: null,
        interviewRoundId: "round-tech-01",
        overallComment: "Tự tin, giao tiếp tốt. Khả năng giải quyết vấn đề ổn. Có tiềm năng phát triển thành Fullstack Developer.",
        submittedAt: "2026-11-11T14:15:00Z",
        scores: [
            {
                criterionId: "crit-logic",
                skillName: "Tư duy logic",
                description: "Giải quyết thuật toán, cấu trúc dữ liệu",
                weight: 1.2,
                score: 80,
                comment: "Giải được bài toán tìm đường đi ngắn nhất trên đồ thị."
            },
            {
                criterionId: "crit-comm",
                skillName: "Giao tiếp",
                description: "Trình bày ý tưởng, xử lý tình huống",
                weight: 0.8,
                score: 88,
                comment: "Trình bày rành mạch, biết lắng nghe và đặt câu hỏi ngược lại cho NTD."
            },
            {
                criterionId: "crit-attitude",
                skillName: "Thái độ",
                description: "Tinh thần học hỏi, cầu tiến",
                weight: 1.0,
                score: 95,
                comment: "Rất ham học hỏi, thái độ tích cực."
            }
        ]
    }
];