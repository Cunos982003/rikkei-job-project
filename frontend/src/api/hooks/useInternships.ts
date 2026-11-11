import { useQuery } from '@tanstack/react-query';
import type { Internship } from '../types/evaluation';

const MOCK_DATA: Internship[] = [
    {
        id: "1",
        user_id: "user-123",
        batch_id: "batch-01",
        batch_name: "Đợt thực tập Java K20",
        group_name: "Nhóm 1",
        topic_name: "Xây dựng hệ thống Rikkei Jobs",
        mentor_email: "mentor@rikkei.edu.vn",
        final_score: 8.5,
        synced_at: "2026-11-11T10:00:00Z",
        tasks: [
            { id: "t1", title: "Thiết kế DB", accepted_score: 9, accepted_at: "2026-10-01" },
            { id: "t2", title: "API Đăng nhập", accepted_score: 8, accepted_at: "2026-10-15" }
        ]
    }
];

export function useInternships(userId: string | undefined) {
    return useQuery({
        queryKey: ['internships', userId],
        queryFn: async () => {
            // Khi BE xong: return client.get(`/api/v1/profiles/${userId}/internships`, { headers: { 'X-User-Id': '...' } })
            await new Promise(r => setTimeout(r, 500));
            return MOCK_DATA;
        },
        enabled: !!userId,
    });
}