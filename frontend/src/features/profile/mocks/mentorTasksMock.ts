import type { EvaluationTask, EvaluationFormData } from '@/api/types/evaluation';

export const MOCK_MENTOR_TASKS: EvaluationTask[] = [
    {
        id: "task-1",
        candidateName: "Nguyễn Văn A",
        context: "INTERNSHIP",
        batchOrRoundName: "Đợt thực tập Java K20",
        deadline: "2026-11-20T23:59:59Z",
        status: "PENDING",
    },
    {
        id: "task-2",
        candidateName: "Trần Thị B",
        context: "INTERVIEW",
        batchOrRoundName: "Vòng phỏng vấn Kỹ thuật 1",
        deadline: "2026-11-15T23:59:59Z",
        status: "DRAFT",
    },
];

export const MOCK_FORM_DATA: EvaluationFormData = {
    id: "task-1",
    candidateName: "Nguyễn Văn A",
    skillsSummary: "Java, Spring Boot, SQL",
    internshipTasks: [
        { title: "Thiết kế Database", score: 9 },
        { title: "API Đăng nhập", score: 8 }
    ],
    criteria: [
        { id: "c1", name: "Java Core", description: "Hiểu sâu về OOP", weight: 1.0 },
        { id: "c2", name: "Spring Boot", description: "Xây dựng REST API", weight: 1.2 },
        { id: "c3", name: "SQL", description: "Viết truy vấn, tối ưu index", weight: 1.0 }
    ]
};