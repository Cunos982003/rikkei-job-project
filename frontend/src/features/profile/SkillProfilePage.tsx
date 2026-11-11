import { useState, Suspense } from "react";
import { useParams } from "react-router-dom";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardHeader } from "@/components/common/Card";
import { Badge } from "@/components/common/Badge";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import { useSkillProfile } from "@/features/profile/hooks";
import type { SkillProfileItem } from "@/api/endpoints/profile.api";
import { cn } from "@/utils/cn";
import { Award, User, ChevronDown, ChevronUp, Calendar, CheckCircle2 } from "lucide-react";

function SkillProfileContent() {
  const { userId } = useParams<{ userId?: string }>();
  const isOwnProfile = !userId;
  const { data, isLoading, error, refetch } = useSkillProfile(userId);

  const skills = data ?? [];
  const groups = groupSkillsByGroup(skills);

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold">Hồ sơ kỹ năng</h1>
          <Badge variant="default">Đang tải...</Badge>
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

  if (error) {
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
            {skills.filter((s: SkillProfileItem) => s.verifiedScore !== null).length} đã xác thực •
            {skills.filter((s: SkillProfileItem) => isSelfOnly(s)).length} tự khai
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

function SkillCard({ skill }: { skill: SkillProfileItem }) {
  const [showEvidences, setShowEvidences] = useState(false);
  const displayScore = skill.verifiedScore ?? skill.selfScore ?? 0;

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
              <Badge variant="verified" className="text-xs gap-1">
                <Award className="h-3 w-3" />
                Xác thực
              </Badge>
            )}
            {skill.verifiedScore === null && skill.selfScore !== null && (
              <Badge variant="self" className="text-xs gap-1">
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
        </div>
      </CardHeader>

      <CardContent className="space-y-3 pb-3">
        <div className="flex items-end justify-between gap-2">
          <Progress value={displayScore} className="flex-1" />
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
              {skill.evidences.map((evidence: typeof skill.evidences[0], index: number) => (
                <div
                  key={`${evidence.source}-${evidence.title}-${index}`}
                  className="p-3 bg-card border rounded-lg hover:bg-accent/50 transition-colors"
                >
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <div className="flex-1 min-w-0">
                      <h4 className="font-medium text-foreground truncate">{evidence.title}</h4>
                      <p className="text-sm text-muted-foreground">{evidence.source}</p>
                    </div>
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <Badge variant={getSourceVariant(evidence.source)} className="gap-1">
                        {getSourceIcon(evidence.source)}
                        {evidence.source}
                      </Badge>
                      <Badge variant={evidence.verified ? "verified" : "self"} className="gap-1">
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
    }>
      <div className="container mx-auto py-6 px-4">
        <SkillProfileContent />
      </div>
    </Suspense>
  );
}

// Helper functions
function groupSkillsByGroup(skills: SkillProfileItem[]): Record<string, SkillProfileItem[]> {
  return skills.reduce((acc, skill) => {
    if (!acc[skill.group]) {
      acc[skill.group] = [];
    }
    acc[skill.group].push(skill);
    return acc;
  }, {} as Record<string, SkillProfileItem[]>);
}

function isSelfOnly(skill: SkillProfileItem): boolean {
  return skill.verifiedScore === null && skill.selfScore !== null;
}

function getSourceVariant(source: string): "default" | "primary" | "warning" | "danger" | "outline" | "verified" | "self" {
  const variants: Record<string, "default" | "primary" | "warning" | "danger" | "outline" | "verified" | "self"> = {
    LMS: "default",
    EXAM: "default",
    PM: "primary",
    INTERNSHIP: "primary",
    MENTOR: "outline",
    SELF: "self",
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