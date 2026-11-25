import type { InternshipDemand, CompanyIntern } from '@/api/types/internship';

export const MOCK_DEMANDS: InternshipDemand[] = [
    {
        jobId: "job-1",
        jobTitle: "Thực tập sinh Java Backend",
        companyName: "Công ty Rikkei Software",
        preferredBatch: "Đợt thực tập Java K20",
        durationWeeks: 12,
        hiredCandidates: [
            { userId: "u-1", candidateName: "Nguyễn Văn A", applicationId: "app-1" },
            { userId: "u-2", candidateName: "Trần Thị B", applicationId: "app-2" }
        ]
    },
    {
        jobId: "job-2",
        jobTitle: "Thực tập sinh Frontend React",
        companyName: "Công ty Rikkei Software",
        preferredBatch: "Đợt thực tập FE K21",
        durationWeeks: 10,
        hiredCandidates: [
            { userId: "u-3", candidateName: "Lê Văn C", applicationId: "app-3" }
        ]
    }
];

export const MOCK_COMPANY_INTERNS: CompanyIntern[] = [
    {
        id: "int-1",
        candidateName: "Nguyễn Văn A",
        batchName: "Đợt thực tập Java K20",
        mentorEmail: "mentor1@rikkeijobs.test",
        acceptedTasksCount: 8,
        currentScore: 8.5
    },
    {
        id: "int-2",
        candidateName: "Trần Thị B",
        batchName: "Đợt thực tập Java K20",
        mentorEmail: "mentor2@rikkeijobs.test",
        acceptedTasksCount: 10,
        currentScore: 9.0
    }
];