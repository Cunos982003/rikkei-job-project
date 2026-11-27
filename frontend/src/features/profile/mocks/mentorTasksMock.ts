import type { EvaluationTask, EvaluationFormData } from '@/api/types/evaluation';

export const MOCK_MENTOR_TASKS: EvaluationTask[] = [
    { id: "task-1", candidateName: "Nguyễn Văn A", context: "INTERNSHIP", batchOrRoundName: "Đợt thực tập Java K20", deadline: "2026-11-20T23:59:59Z", status: "PENDING" },
    { id: "task-2", candidateName: "Trần Thị B", context: "INTERVIEW", batchOrRoundName: "Vòng phỏng vấn Kỹ thuật 1", deadline: "2026-11-15T23:59:59Z", status: "DRAFT" },
    { id: "task-3", candidateName: "Lê Hoàng Cường", context: "INTERNSHIP", batchOrRoundName: "Đợt thực tập Java K20", deadline: "2026-11-22T23:59:59Z", status: "PENDING" },
    { id: "task-4", candidateName: "Phạm Minh Dũng", context: "INTERNSHIP", batchOrRoundName: "Đợt thực tập Frontend K21", deadline: "2026-11-21T23:59:59Z", status: "SUBMITTED" },
    { id: "task-5", candidateName: "Vũ Thị Hương", context: "INTERVIEW", batchOrRoundName: "Vòng phỏng vấn Kỹ thuật 2", deadline: "2026-11-18T23:59:59Z", status: "PENDING" },
    { id: "task-6", candidateName: "Hoàng Anh Tuấn", context: "INTERNSHIP", batchOrRoundName: "Đợt thực tập DevOps K20", deadline: "2026-11-25T23:59:59Z", status: "PENDING" },
    { id: "task-7", candidateName: "Đặng Thu Thảo", context: "INTERVIEW", batchOrRoundName: "Vòng phỏng vấn Kỹ thuật 1", deadline: "2026-11-16T23:59:59Z", status: "DRAFT" },
    { id: "task-8", candidateName: "Bùi Văn Nam", context: "INTERNSHIP", batchOrRoundName: "Đợt thực tập Java K20", deadline: "2026-11-23T23:59:59Z", status: "PENDING" },
    { id: "task-9", candidateName: "Ngô Thị Mai", context: "INTERNSHIP", batchOrRoundName: "Đợt thực tập Frontend K21", deadline: "2026-11-24T23:59:59Z", status: "PENDING" },
    { id: "task-10", candidateName: "Đỗ Quang Huy", context: "INTERVIEW", batchOrRoundName: "Vòng phỏng vấn Kỹ thuật 3", deadline: "2026-11-19T23:59:59Z", status: "SUBMITTED" },
];

// Tạo dữ liệu form riêng cho từng ứng viên (Dùng Record để tra cứu nhanh)
export const MOCK_FORM_DATAS: Record<string, EvaluationFormData> = {
    "Nguyễn Văn A": {
        id: "task-1", candidateName: "Nguyễn Văn A", skillsSummary: "Java, Spring Boot, SQL",
        internshipTasks: [{ title: "Thiết kế Database", score: 9 }, { title: "API Đăng nhập", score: 8 }],
        criteria: [
            { id: "c1", name: "Java Core", description: "Hiểu sâu về OOP", weight: 1.0 },
            { id: "c2", name: "Spring Boot", description: "Xây dựng REST API", weight: 1.2 },
            { id: "c3", name: "SQL", description: "Viết truy vấn, tối ưu index", weight: 1.0 }
        ]
    },
    "Trần Thị B": {
        id: "task-2", candidateName: "Trần Thị B", skillsSummary: "ReactJS, TypeScript, Tailwind",
        internshipTasks: [{ title: "UI Trang chủ", score: 8 }, { title: "Tích hợp API", score: 7 }],
        criteria: [
            { id: "f1", name: "ReactJS", description: "Component lifecycle, Hooks", weight: 1.0 },
            { id: "f2", name: "TypeScript", description: "Type safety, Interface", weight: 1.0 },
            { id: "f3", name: "Tailwind CSS", description: "Responsive design", weight: 0.8 }
        ]
    },
    "Lê Hoàng Cường": {
        id: "task-3", candidateName: "Lê Hoàng Cường", skillsSummary: "Java, Spring Boot, Microservices",
        internshipTasks: [{ title: "Tối ưu hóa API", score: 9 }, { title: "Cấu hình Docker", score: 8 }],
        criteria: [
            { id: "m1", name: "Spring Boot", description: "REST API, Data JPA", weight: 1.2 },
            { id: "m2", name: "Microservices", description: "Giao tiếp giữa các service", weight: 1.5 }
        ]
    },
    // Các ứng viên còn lại có thể dùng chung template cơ bản hoặc tạo riêng nếu muốn
};

// Dữ liệu mặc định (Fallback)
export const MOCK_FORM_DATA: EvaluationFormData = MOCK_FORM_DATAS["Nguyễn Văn A"];