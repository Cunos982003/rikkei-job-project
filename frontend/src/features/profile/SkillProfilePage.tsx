import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/common/card";
import { Badge } from "@/components/common/badge";
import { Progress } from "@/components/ui/progress";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Badge as BadgeUI } from "@/components/ui/badge";
import { Card as CardUI, CardContent as CardContentUI, CardHeader as CardHeaderUI } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { useParams } from "react-router-dom";
import { useSkillProfile } from "@/api/hooks/useSkillProfile";
import type { SkillProfileResponse } from "@/api/types/skill";
import { cn } from "@/utils/cn";
import { Award, User, ChevronDown, ChevronUp, Calendar, CheckCircle2 } from "lucide-react";
import { useState } from "react";

function SkillProfileContent() {
  const { userId } = useParams<{ userId?: string }>();
  const isOwnProfile = !userId;
  const { data, isLoading, error, refetch } = useSkillProfile(userId);
  const navigate = useNavigate();

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
      level: "Nâng cao",
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
      level: "Nâng cao",
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
              <SkillCard key={skill.skillId} skill={skill} />
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

function SkillCard({ skill }: { skill: SkillProfileResponse }) {
  const [showEvidences, setShowEvidences] = useState(false);
  const displayScore = skill.verifiedScore ?? skill.selfScore ?? 0;
  const isSelfOnly = skill.verifiedScore === null && skill.selfScore !== null;

  return (
    <Card className="overflow-hidden transition-shadow hover:shadow-md">
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between">
          <div className="flex-1 min-w-0">
            <h3 className="font-semibold text-foreground truncate">{skill.skillName}</h3>
            <p className="text-sm text-muted-foreground">{skill.skillCode}</p>
          </div>
          <div className="flex items-center gap-1.5">
            {skill.verifiedScore !== null && (
              <Badge variant="default" className="text-xs gap-1">
                <Award className="h-3 w-3" />
                Xác thực
              </Badge>
            )}
            {skill.verifiedScore === null && skill.selfScore !== null && (
              <Badge variant="secondary" className="text-xs gap-1">
                <User className="h-3 w-3" />
                Tự khai
              </Badge>
            )}
            <button
              onClick={() => setShowEvidences(!showEvidences)}
              className="p-1 hover:bg-accent rounded transition-colors"
              aria-label={showEvidences ? "Ẩn bằng chứng" : "Hiện bằng chứng"}
            >
              {showEvidences ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </button>
          </div>
        </CardHeader>

        <CardContent className="space-y-3 pb-3">
          <div className="flex items-end justify-between gap-2">
            <Progress value={displayScore} max={100} className="flex-1" />
            <span className="text-lg font-bold text-foreground whitespace-nowrap">
              {displayScore}
            </span>
          </div>

          <div className="flex items-center justify-between">
            <Badge variant="outline" className="text-xs">
              {skill.level}
            </Badge>
            {skill.evidences.length > 0 && (
              <button
                onClick={() => setShowEvidences(!showEvidences)}
                className="text-xs text-primary hover:underline"
              >
                {showEvidences ? "Ẩn bằng chứng" : `${skill.evidences.length} bằng chứng`}
              </button>
            )}
          </div>

          {showEvidences && skill.evidences.length > 0 && (
            <>
              <Separator />
              <div className="space-y-2 max-h-60 overflow-y-auto">
                {skill.evidences.map((evidence, index) => (
                  <div
                    key={`${evidence.source}-${evidence.sourceRef}-${index}`}
                    className="p-3 bg-card border rounded-lg hover:bg-accent/50 transition-colors"
                  >
                    <div className="flex items-start justify-between gap-2 mb-2">
                      <div className="flex-1 min-w-0">
                        <h4 className="font-medium text-foreground truncate">{evidence.title}</h4>
                        <p className="text-sm text-muted-foreground">{evidence.sourceRef}</p>
                      </div>
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <Badge variant={getSourceVariant(evidence.source)} className="gap-1">
                          {getSourceIcon(evidence.source)}
                          {evidence.source}
                        </Badge>
                        <Badge variant={evidence.verified ? "default" : "destructive"} className="gap-1">
                          {evidence.verified ? (
                            <>
                              <CheckCircle2 className="h-3 w-3" />
                              Đã xác thực
                            </>
                          ) : (
                            <>
                              <User className="h-3 w-3" />
                              Chưa xác thực
                            </>
                          )}
                        </Badge>
                      </div>
                    </div>

                    <div className="flex items-center justify-between text-sm">
                      <div className="flex items-center gap-2">
                        <span className={cn(
                          "font-semibold px-2 py-0.5 rounded",
                          evidence.score >= 80 && "text-green-700 bg-green-100",
                          evidence.score >= 60 && evidence.score < 80 && "text-yellow-700 bg-yellow-100",
                          evidence.score >= 40 && evidence.score < 60 && "text-orange-700 bg-orange-100",
                          evidence.score < 40 && "text-red-700 bg-red-100"
                        )}>
                          {evidence.score}/100
                        </span>
                        <span className="flex items-center gap-1 text-muted-foreground">
                          <Calendar className="h-3 w-3" />
                          {new Date(evidence.recordedAt).toLocaleDateString("vi-VN")}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </CardContent>
      </Card>
    );
  }

export function SkillProfilePage() {
  return (
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
    </Suspense>
      <div className="container mx-auto py-6 px-4">
        <SkillProfileContent />
      </div>
    </Suspense>
  );
}






}

// Helper functions
function groupSkillsByGroup(skills: SkillProfileResponse[]): Record<string, SkillProfileResponse[]> {
  return skills.reduce((acc, skill) => {
    if (!acc[skill.group]) {
      acc[skill.group] = [];
    }
    acc[skill.group].push(skill);
    return acc;
  }, {} as Record<string, SkillProfileResponse[]>);
}

function isSelfOnly(skill: SkillProfileResponse): boolean {
  return skill.verifiedScore === null && skill.selfScore !== null;
}

function getSourceVariant(source: string): "default" | "secondary" | "outline" | "destructive" {
  const variants: Record<string, "default" | "secondary" | "outline" | "destructive"> = {
    LMS: "default",
    EXAM: "default",
    PM: "secondary",
    INTERNSHIP: "secondary",
    MENTOR: "outline",
    SELF: "destructive",
  };
  return variants[source] || "outline";
}

function getSourceIcon(source: string) {
  const icons: Record<string, React.ReactNode> = {
    LMS: <Award className="h-3 w-3" />,
    EXAM: <Award className="h-3 w-3" />,
    PM: <Award className="h-3 w-3" />,
    INTERNSHIP: <User className="h-3 w-3" />,
    MENTOR: <User className="h-3 w-3" />,
    SELF: <User className="h-3 w-3" />,
  };
  return icons[source] || <User className="h-3 w-3" />;
}