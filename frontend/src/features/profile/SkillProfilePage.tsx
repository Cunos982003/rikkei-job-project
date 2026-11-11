import { Suspense, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { SkillGroupCard } from "./components/SkillGroupCard";
import { groupSkillsByGroup, isSelfOnly } from "@/lib/utils";
import { useSkillProfile } from "@/api/hooks/useSkillProfile";
import type { SkillProfileResponse } from "@/api/types/skill";


function SkillProfileContent() {
  const { userId } = useParams<{ userId?: string }>();
  const isOwnProfile = !userId;
  const { data, isLoading, error, refetch } = useSkillProfile(userId);

  // For demo purposes, we'll show mock data when there's no backend
  const [useMockData, setUseMockData] = useState(false);

  useEffect(() => {
    // If backend is not available, fall back to mock data after a short delay
    const timer = setTimeout(() => {
      if (isLoading && !error) {
        // Check if we can connect to backend
        fetch("/api/v1/profiles/me/skills")
          .catch(() => setUseMockData(true));
      }
    }, 2000);
    return () => clearTimeout(timer);
  }, [isLoading, error]);

  // Mock data for demo when backend is not available
  const mockData: SkillProfileResponse[] = [
    {
      skillId: "1",
      skillCode: "JAVA_CORE",
      skillName: "Java Core",
      group: "Programming",
      verifiedScore: 85,
      selfScore: null,
      level: "Nâng cao",
      evidences: [
        { source: "LMS", sourceRef: "JAVA01", title: "Java Core Course", score: 85, recordedAt: "2026-10-15T10:30:00Z", verified: true },
        { source: "EXAM", sourceRef: "JAVA_MID", title: "Java Midterm", score: 90, recordedAt: "2026-09-20T14:00:00Z", verified: true },
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
        { source: "LMS", sourceRef: "SPRING01", title: "Spring Boot Course", score: 80, recordedAt: "2026-10-10T10:00:00Z", verified: true },
        { source: "PM", sourceRef: "IOC_JAVA", title: "IOC Java Project", score: 70, recordedAt: "2026-08-15T16:00:00Z", verified: true },
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
        { source: "LMS", sourceRef: "JAVA01", title: "Java Core Course (SQL part)", score: 75, recordedAt: "2026-10-15T10:30:00Z", verified: true },
        { source: "EXAM", sourceRef: "SQL_FINAL", title: "SQL Final Exam", score: 75, recordedAt: "2026-09-25T10:00:00Z", verified: true },
      ],
    },
    {
      skillId: "4",
      skillCode: "REACT",
      skillName: "React",
      group: "Frontend",
      verifiedScore: null,
      selfScore: 88,
      level: "Nâng cao",
      evidences: [
        { source: "SELF", sourceRef: "SELF_REACT", title: "Self-reported React", score: 88, recordedAt: "2026-10-01T12:00:00Z", verified: false },
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
        { source: "LMS", sourceRef: "WEB01", title: "JavaScript Basics", score: 65, recordedAt: "2026-09-01T10:00:00Z", verified: true },
        { source: "SELF", sourceRef: "SELF_JS", title: "Self-reported JavaScript", score: 70, recordedAt: "2026-10-01T12:00:00Z", verified: false },
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
        { source: "LMS", sourceRef: "DOCKER01", title: "Docker Fundamentals", score: 60, recordedAt: "2026-08-20T10:00:00Z", verified: true },
      ],
    },
    {
      skillId: "7",
      skillCode: "COMMUNICATION",
      skillName: "Communication",
      group: "Soft Skill",
      verifiedScore: null,
      selfScore: 82,
      level: "Nâng cao",
      evidences: [
        { source: "SELF", sourceRef: "SELF_COMM", title: "Self-reported Communication", score: 82, recordedAt: "2026-10-01T12:00:00Z", verified: false },
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
        { source: "PM", sourceRef: "IOC_JAVA", title: "IOC Java Project (Teamwork)", score: 75, recordedAt: "2026-08-15T16:00:00Z", verified: true },
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
        { source: "LMS", sourceRef: "GIT01", title: "Version Control with Git", score: 70, recordedAt: "2026-09-10T10:00:00Z", verified: true },
      ],
    },
  ];

  const skills = useMockData ? mockData : (data ?? []);
  const groups = groupSkillsByGroup(skills);

  if (isLoading && !useMockData) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold">Hồ sơ kỹ năng</h1>
          <Badge variant="secondary">Đang tải...</Badge>
        </div>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[...Array(6)].map((_, i) => (
            <Card key={i} className="h-32">
              <CardContent>
                <Skeleton className="h-6 w-3/4 mb-4" />
                <Skeleton className="h-4 w-1/2 mb-2" />
                <Skeleton className="h-4 w-1/4" />
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  if (error && !useMockData) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600 mb-4">Không thể tải hồ sơ kỹ năng</p>
        <button
          onClick={() => refetch()}
          className="px-4 py-2 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90"
        >
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">
            {isOwnProfile ? "Hồ sơ kỹ năng của tôi" : "Hồ sơ kỹ năng ứng viên"}
          </h1>
          <p className="text-muted-foreground">
            {skills.length} kỹ năng •
            {skills.filter((s: SkillProfileResponse) => s.verifiedScore !== null).length} đã xác thực •
            {skills.filter((s: SkillProfileResponse) => isSelfOnly(s)).length} tự khai
          </p>
        </div>
      </div>


      {Object.entries(groups).map(([group, groupSkills]) => (
        <section key={group} className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-foreground">{group}</h2>
            <Badge variant="outline" className="text-xs">
              {groupSkills.length} kỹ năng
            </Badge>
          </div>

          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {groupSkills.map((skill) => (
              <SkillGroupCard key={skill.skillId} skill={skill} />
            ))}
          </div>
        </section>
      ))}

      {skills.length === 0 && (
        <div className="text-center py-12">
          <p className="text-muted-foreground mb-4">
            {isOwnProfile
              ? "Bạn chưa có kỹ năng nào. Hãy cập nhật hồ sơ hoặc chờ đồng bộ từ LMS."
              : "Ứng viên này chưa có kỹ năng nào."}
          </p>
        </div>
      )}
    </div>
  );
}

export function SkillProfilePage() {
  return (
    <div className="container mx-auto py-6 px-4">
      <Suspense fallback={
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <Skeleton className="h-8 w-48" />
            <Skeleton className="h-6 w-24" />
          </div>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {[...Array(6)].map((_, i) => (
              <Card key={i} className="h-32">
                <CardContent>
                  <Skeleton className="h-6 w-3/4 mb-4" />
                  <Skeleton className="h-4 w-1/2 mb-2" />
                  <Skeleton className="h-4 w-1/4" />
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      }>
        <SkillProfileContent />
      </Suspense>
    </div>
  );
}
