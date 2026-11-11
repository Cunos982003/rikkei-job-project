// frontend/src/api/hooks/useProfileEvaluations.ts
import { useQuery } from '@tanstack/react-query';
import type { MentorEvaluation } from '../types/evaluation';
// import { client } from '../client'; // Mở comment này khi gọi API thật

// --- DỮ LIỆU MOCK (Đã khớp 100% với interface của bạn) ---
const MOCK_DATA: MentorEvaluation[] = [
  {
    id: "eval-101", // Dùng string vì Interface của bạn định nghĩa id là string
    templateName: "Java Backend – Thực tập",
    mentorName: "Nguyễn Văn Mentor",
    context: "INTERNSHIP",
    batchId: "batch-01",
    interviewRoundId: null,
    overallComment: "Ứng viên có tiềm năng, tư duy tốt. Cần cải thiện thêm kỹ năng SQL.",
    submittedAt: "2026-11-10T08:30:00Z",
    scores: [
      {
        criterionId: "crit-1",
        skillName: "Java Core",
        description: "Nắm vững OOP",
        weight: 1.0,
        score: 85,
        comment: "Code sạch, hiểu OOP."
      },
      {
        criterionId: "crit-2",
        skillName: "Spring Boot",
        description: "Xây dựng REST API",
        weight: 1.2,
        score: 75,
        comment: "Biết viết REST API cơ bản."
      },
      {
        criterionId: "crit-3",
        skillName: "SQL",
        description: "Viết truy vấn",
        weight: 1.0,
        score: 60,
        comment: "Cần luyện thêm về JOIN."
      },
    ]
  },
  {
    id: "eval-102",
    templateName: "Phỏng vấn kỹ thuật chung",
    mentorName: "Trần Thị Mentor",
    context: "INTERVIEW",
    batchId: null,
    interviewRoundId: "round-01",
    overallComment: "Giao tiếp tốt, tự tin.",
    submittedAt: "2026-11-11T09:00:00Z",
    scores: [
      {
        criterionId: "crit-4",
        skillName: "Tư duy logic",
        description: "Giải quyết vấn đề",
        weight: 1.2,
        score: 90,
        comment: "Phản xạ nhanh."
      },
      {
        criterionId: "crit-5",
        skillName: "Giao tiếp",
        description: "Trình bày giải pháp",
        weight: 0.8,
        score: 85,
        comment: "Trình bày rõ ràng."
      },
    ]
  }
];
// -------------------------------------------------------------

export function useProfileEvaluations(userId: string | undefined) {
  return useQuery({
    queryKey: ['profile-evaluations', userId],
    queryFn: async () => {
      // KHI BE XONG: Mở comment dòng dưới và xóa đoạn return MOCK_DATA
      // return client.get(`/api/v1/profiles/${userId}/evaluations`).then(res => res.data);

      // TẠM THỜI DÙNG MOCK ĐỂ TEST FE
      await new Promise(resolve => setTimeout(resolve, 800)); // Giả lập đợi mạng
      return MOCK_DATA;
    },
    // Chỉ gọi API khi có userId
    enabled: !!userId,
  });
}