import type { Internship } from '@/api/types/evaluation';

export const MOCK_INTERNSHIPS: Internship[] = [
    {
        id: "int-1",
        batchName: "Đợt thực tập Java K20",
        groupName: "Nhóm 1",
        topicName: "Xây dựng hệ thống Rikkei Jobs",
        mentorEmail: "mentor@rikkeijobs.test",
        startDate: "2026-09-01",
        endDate: "2026-11-30",
        finalScore: 8.5,
        tasks: [
            { id: "t1", title: "Thiết kế Database", acceptedScore: 9, acceptedAt: "2026-10-01" },
            { id: "t2", title: "API Đăng nhập", acceptedScore: 8, acceptedAt: "2026-10-15" },
        ],
    },
];