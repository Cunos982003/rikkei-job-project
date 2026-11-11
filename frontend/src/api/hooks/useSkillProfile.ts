// frontend/src/api/hooks/useSkillProfile.ts
import { useQuery } from '@tanstack/react-query';
import type { SkillProfileItem } from '../endpoints/profile.api';

// --- MOCK DATA (Matches SkillProfileItem interface) ---
const MOCK_DATA: SkillProfileItem[] = [
  {
    skillId: "1",
    skillCode: "JAVA_CORE",
    skillName: "Java Core",
    group: "Programming",
    verifiedScore: 85,
    selfScore: null,
    level: "Năng cao",
    evidences: [
      { source: "LMS", title: "Java Core Course", score: 85, recordedAt: "2026-10-15T10:30:00Z", verified: true },
      { source: "EXAM", title: "Java Midterm", score: 90, recordedAt: "2026-09-20T14:00:00Z", verified: true },
    ],
  },
  {
    skillId: "2",
    skillCode: "SPRING_BOOT",
    skillName: "Spring Boot",
    group: "Framework",
    verifiedScore: 78,
    selfScore: null,
    level: "Vững",
    evidences: [
      { source: "LMS", title: "Spring Boot Course", score: 80, recordedAt: "2026-10-10T10:00:00Z", verified: true },
      { source: "PM", title: "IOC Java Project", score: 70, recordedAt: "2026-08-15T16:00:00Z", verified: true },
    ],
  },
  {
    skillId: "3",
    skillCode: "SQL",
    skillName: "SQL",
    group: "Database",
    verifiedScore: 72,
    selfScore: null,
    level: "Vững",
    evidences: [
      { source: "LMS", title: "Java Core Course (SQL part)", score: 75, recordedAt: "2026-10-15T10:30:00Z", verified: true },
      { source: "EXAM", title: "SQL Final Exam", score: 75, recordedAt: "2026-09-25T10:00:00Z", verified: true },
    ],
  },
  {
    skillId: "4",
    skillCode: "REACT",
    skillName: "React",
    group: "Frontend",
    verifiedScore: null,
    selfScore: 88,
    level: "Năng cao",
    evidences: [
      { source: "SELF", title: "Self-reported React", score: 88, recordedAt: "2026-10-01T12:00:00Z", verified: false },
    ],
  },
  {
    skillId: "5",
    skillCode: "JAVASCRIPT",
    skillName: "JavaScript",
    group: "Frontend",
    verifiedScore: 65,
    selfScore: 70,
    level: "Khá",
    evidences: [
      { source: "LMS", title: "JavaScript Basics", score: 65, recordedAt: "2026-09-01T10:00:00Z", verified: true },
      { source: "SELF", title: "Self-reported JavaScript", score: 70, recordedAt: "2026-10-01T12:00:00Z", verified: false },
    ],
  },
  {
    skillId: "6",
    skillCode: "DOCKER",
    skillName: "Docker",
    group: "DevOps",
    verifiedScore: 60,
    selfScore: null,
    level: "Khá",
    evidences: [
      { source: "LMS", title: "Docker Fundamentals", score: 60, recordedAt: "2026-08-20T10:00:00Z", verified: true },
    ],
  },
  {
    skillId: "7",
    skillCode: "COMMUNICATION",
    skillName: "Communication",
    group: "Soft Skill",
    verifiedScore: null,
    selfScore: 82,
    level: "Năng cao",
    evidences: [
      { source: "SELF", title: "Self-reported Communication", score: 82, recordedAt: "2026-10-01T12:00:00Z", verified: false },
    ],
  },
  {
    skillId: "8",
    skillCode: "TEAMWORK",
    skillName: "Teamwork",
    group: "Soft Skill",
    verifiedScore: 75,
    selfScore: null,
    level: "Vững",
    evidences: [
      { source: "PM", title: "IOC Java Project (Teamwork)", score: 75, recordedAt: "2026-08-15T16:00:00Z", verified: true },
    ],
  },
  {
    skillId: "9",
    skillCode: "GIT",
    skillName: "Git",
    group: "Tool",
    verifiedScore: 70,
    selfScore: null,
    level: "Vững",
    evidences: [
      { source: "LMS", title: "Version Control with Git", score: 70, recordedAt: "2026-09-10T10:00:00Z", verified: true },
    ],
  },
];
// -------------------------------------------------------------

export function useSkillProfile(userId: string | undefined) {
  return useQuery({
    queryKey: ['skill-profile', userId],
    queryFn: async () => {
      // WHEN BACKEND READY: Uncomment below and remove mock return
      // const response = await fetch(`/api/v1/profiles/${userId || 'me'}/skills`);
      // const data = await response.json();
      // return data.data || data;

      // TEMPORARILY USE MOCK FOR FE TESTING
      await new Promise(resolve => setTimeout(resolve, 800)); // Simulate network delay
      return MOCK_DATA;
    },
    // Only call API when userId is available (or for own profile when undefined)
    enabled: true, // Always enabled for demo
  });
}