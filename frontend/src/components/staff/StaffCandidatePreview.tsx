import { useState } from "react";
import { ChevronDown, ChevronUp, Award, User, Calendar, CheckCircle2, Building2, GraduationCap, FileText, Star, Sparkles } from "lucide-react";
import { Badge } from "@/components/common/Badge";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/common/Card";
import { Separator } from "@/components/ui/separator";
import { Progress } from "@/components/ui/progress";
import { cn } from "@/utils/cn";
import type { StaffPreviewProfile } from "@/api/endpoints/profile.api";

function formatDate(dateStr?: string): string {
  if (!dateStr) return "—";
  const d = new Date(dateStr);
  if (Number.isNaN(d.getTime())) return dateStr;
  return d.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
}

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return "—";
  const d = new Date(dateStr);
  if (Number.isNaN(d.getTime())) return dateStr;
  return d.toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
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

function getScoreColor(score: number) {
  if (score >= 80) return "text-green-700 bg-green-100";
  if (score >= 60) return "text-yellow-700 bg-yellow-100";
  if (score >= 40) return "text-orange-700 bg-orange-100";
  return "text-red-700 bg-red-100";
}

interface StaffCandidatePreviewProps {
  profile: StaffPreviewProfile;
  isLoading?: boolean;
}

export function StaffCandidatePreview({ profile, isLoading = false }: StaffCandidatePreviewProps) {
  const [expandedSkills, setExpandedSkills] = useState<Set<string>>(new Set());

  if (isLoading) {
    return (
      <div className="space-y-6 animate-pulse">
        <Card>
          <CardHeader>
            <div className="h-6 w-48 bg-slate-200 rounded" />
            <div className="h-4 w-64 bg-slate-200 rounded mt-2" />
          </CardHeader>
        </Card>
        {[...Array(3)].map((_, i) => (
          <Card key={i} className="h-32">
            <CardContent>
              <div className="h-6 w-3/4 bg-slate-200 rounded mb-4" />
              <div className="h-4 w-1/2 bg-slate-200 rounded mb-2" />
              <div className="h-4 w-1/4 bg-slate-200 rounded" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  const skills = profile.skills ?? [];
  const groups = skills.reduce((acc, skill) => {
    if (!acc[skill.group]) acc[skill.group] = [];
    acc[skill.group].push(skill);
    return acc;
  }, {} as Record<string, typeof skills>);

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card>
        <CardHeader>
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <h2 className="text-xl font-bold text-foreground">{profile.fullName || "Ứng viên"}</h2>
                <Badge variant="outline" size="sm" className="gap-1">
                  <Building2 className="h-3 w-3" />
                  {profile.classCode || "Chưa có lớp"}
                </Badge>
                <Badge variant={profile.visibility === "PUBLIC" ? "verified" : profile.visibility === "PENDING" ? "warning" : profile.visibility === "REJECTED" ? "danger" : "default"} size="sm" showIcon>
                  {profile.visibility === "PUBLIC" && <CheckCircle2 className="h-3 w-3" />}
                  {profile.visibility === "PENDING" && <Sparkles className="h-3 w-3" />}
                  {profile.visibility === "REJECTED" && <span className="text-danger">Trả lại</span>}
                  {profile.visibility === "PRIVATE" && <span>Riêng tư</span>}
                  {profile.visibility === "RECRUITER" && <span>Chỉ DN</span>}
                </Badge>
              </div>
              <p className="text-sm text-muted">
                {profile.program || "Chưa có chương trình"} • Khóa: {profile.courseName || "Chưa có khóa"}
              </p>
            </div>
            <div className="flex items-center gap-4 text-sm text-muted">
              <div className="flex items-center gap-1">
                <Calendar className="h-3.5 w-3.5" />
                <span>Cập nhật: {formatDateTime(profile.updatedAt)}</span>
              </div>
              {profile.consentAt && (
                <div className="flex items-center gap-1 text-success">
                  <CheckCircle2 className="h-3.5 w-3.5" />
                  <span>Đồng ý: {formatDateTime(profile.consentAt)}</span>
                </div>
              )}
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Contact Info (Masked) */}
          <div className="grid sm:grid-cols-2 gap-4 p-3 bg-slate-50 rounded-lg border border-border-subtle">
            <div className="flex items-center gap-2">
              <span className="text-muted w-24 shrink-0">Email:</span>
              <span className="font-mono text-sm text-foreground">{profile.maskedEmail || profile.email || "—"}</span>
              {profile.maskedEmail && profile.email && profile.email !== profile.maskedEmail && (
                <Badge variant="outline" size="sm" className="text-muted">Đã che</Badge>
              )}
            </div>
            <div className="flex items-center gap-2">
              <span className="text-muted w-24 shrink-0">Điện thoại:</span>
              <span className="font-mono text-sm text-foreground">{profile.maskedPhone || profile.phone || "—"}</span>
              {profile.maskedPhone && profile.phone && profile.phone !== profile.maskedPhone && (
                <Badge variant="outline" size="sm" className="text-muted">Đã che</Badge>
              )}
            </div>
          </div>

          {/* Headline & Summary */}
          {(profile.headline || profile.summary) && (
            <div className="space-y-3">
              {profile.headline && (
                <div>
                  <label className="block text-xs font-medium text-muted mb-1">Tiêu đề hồ sơ</label>
                  <p className="text-foreground">{profile.headline}</p>
                </div>
              )}
              {profile.summary && (
                <div>
                  <label className="block text-xs font-medium text-muted mb-1">Giới thiệu bản thân</label>
                  <p className="text-foreground whitespace-pre-wrap">{profile.summary}</p>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Skills */}
      {Object.keys(groups).length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2">
                <Star className="h-5 w-5 text-warning" />
                <span>Kỹ năng ({skills.length})</span>
              </CardTitle>
              <div className="flex items-center gap-2 text-xs text-muted">
                <Badge variant="verified" className="gap-1">{skills.filter(s => s.verifiedScore !== null).length} đã xác thực</Badge>
                <Badge variant="self" className="gap-1">{skills.filter(s => s.verifiedScore === null && s.selfScore !== null).length} tự khai</Badge>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {Object.entries(groups).map(([group, groupSkills]) => (
              <section key={group} className="space-y-3">
                <div className="flex items-center justify-between pb-2 border-b border-border-subtle">
                  <h3 className="text-lg font-semibold text-foreground">{group}</h3>
                  <Badge variant="outline" size="sm">{groupSkills.length} kỹ năng</Badge>
                </div>
                <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
                  {groupSkills.map((skill) => (
                    <SkillCard key={skill.skillId} skill={skill} expandedSkills={expandedSkills} onToggleExpand={setExpandedSkills} />
                  ))}
                </div>
              </section>
            ))}
          </CardContent>
        </Card>
      )}

      {/* Educations */}
      {(profile.educations?.length ?? 0) > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <GraduationCap className="h-5 w-5 text-info" />
              <span>Học vấn ({profile.educations?.length})</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {profile.educations?.map((edu) => (
              <div key={edu.id} className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 p-3 bg-slate-50 rounded-lg border border-border-subtle">
                <div className="flex-1 space-y-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <h4 className="font-medium text-foreground">{edu.school}</h4>
                    <Badge variant="primary" size="sm">{edu.major}</Badge>
                  </div>
                  <div className="flex items-center gap-4 text-xs text-muted">
                    <span className="flex items-center gap-1">
                      <GraduationCap className="h-3.5 w-3.5" />
                      {formatDate(edu.fromDate)} - {edu.toDate ? formatDate(edu.toDate) : "Hiện tại"}
                    </span>
                    {edu.gpa != null && (
                      <span className="flex items-center gap-1">
                        <Star className="h-3.5 w-3.5 text-amber-500" />
                        GPA: {edu.gpa}
                      </span>
                    )}
                  </div>
                </div>
                {edu.note && <p className="text-sm text-muted sm:w-64">{edu.note}</p>}
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {/* Certificates */}
      {(profile.certificates?.length ?? 0) > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <FileText className="h-5 w-5 text-info" />
              <span>Chứng chỉ ({profile.certificates?.length})</span>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {profile.certificates?.map((cert) => (
              <div key={cert.id} className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 p-3 bg-slate-50 rounded-lg border border-border-subtle">
                <div className="flex-1 space-y-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <h4 className="font-medium text-foreground">{cert.name}</h4>
                    <Badge variant="verified" size="sm">{cert.issuer}</Badge>
                  </div>
                  <div className="flex items-center gap-4 text-xs text-muted">
                    <span className="flex items-center gap-1">
                      <FileText className="h-3.5 w-3.5" />
                      Ngày cấp: {formatDate(cert.issuedAt)}
                    </span>
                    {cert.expiresAt && (
                      <span className="flex items-center gap-1">
                        <Calendar className="h-3.5 w-3.5" />
                        Hết hạn: {formatDate(cert.expiresAt)}
                      </span>
                    )}
                  </div>
                </div>
                {cert.fileUrl && (
                  <a href={cert.fileUrl} target="_blank" rel="noopener noreferrer" className="flex items-center gap-1 text-primary hover:underline text-sm">
                    <FileText className="h-3.5 w-3.5" /> Xem tệp
                  </a>
                )}
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {/* Review History */}
      {(profile.reviewHistory?.length ?? 0) > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Sparkles className="h-5 w-5 text-primary" />
              <span>Lịch sử duyệt ({profile.reviewHistory?.length})</span>
            </CardTitle>
            <CardDescription>Số lần bị trả lại: {profile.rejectionCount}</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto rounded-lg border border-border-subtle">
              <table className="w-full text-xs">
                <thead className="bg-slate-50 text-muted">
                  <tr>
                    <th className="text-left font-medium px-3 py-2">Hành động</th>
                    <th className="text-left font-medium px-3 py-2">Lý do</th>
                    <th className="text-left font-medium px-3 py-2">Người thực hiện</th>
                    <th className="text-left font-medium px-3 py-2">Thời gian</th>
                  </tr>
                </thead>
                <tbody>
                  {profile.reviewHistory?.map((item) => (
                    <tr key={item.id} className="border-t border-border-subtle">
                      <td className="px-3 py-2">
                        <Badge variant={
                          item.action === "APPROVE" ? "verified" :
                          item.action === "REJECT" ? "danger" :
                          item.action === "SUBMIT" ? "warning" : "default"
                        } size="sm" className="gap-1">
                          {item.action === "SUBMIT" && <Sparkles className="h-3 w-3" />}
                          {item.action === "APPROVE" && <CheckCircle2 className="h-3 w-3" />}
                          {item.action === "REJECT" && <span className="text-danger">✕</span>}
                          {item.action === "WITHDRAW" && <span>↩</span>}
                          {item.action}
                        </Badge>
                      </td>
                      <td className="px-3 py-2 text-muted">{item.reason || "—"}</td>
                      <td className="px-3 py-2 text-muted">{item.actorId === profile.userId ? "Ứng viên" : "Cán bộ tuyển dụng"}</td>
                      <td className="px-3 py-2 text-muted whitespace-nowrap">{formatDateTime(item.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function SkillCard({ skill, expandedSkills, onToggleExpand }: {
  skill: StaffPreviewProfile["skills"][0];
  expandedSkills: Set<string>;
  onToggleExpand: (skills: Set<string>) => void;
}) {
  const isExpanded = expandedSkills.has(skill.skillId);
  const displayScore = skill.verifiedScore ?? skill.selfScore ?? 0;

  return (
    <Card className="overflow-hidden transition-shadow hover:shadow-md">
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between">
          <div className="flex-1 min-w-0">
            <h4 className="font-semibold text-foreground truncate">{skill.skillName}</h4>
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
              onClick={() => onToggleExpand(new Set(expandedSkills).add(skill.skillId))}
              className="p-1 hover:bg-accent rounded transition-colors"
              aria-label={isExpanded ? "Ẩn bằng chứng" : "Hiện bằng chứng"}
            >
              {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </button>
          </div>
        </div>
      </CardHeader>

      <CardContent className="space-y-3 pb-3">
        <div className="flex items-end justify-between gap-2">
          <Progress value={displayScore} className="flex-1" />
          <span className="text-lg font-bold text-foreground whitespace-nowrap">{displayScore}</span>
        </div>

        <div className="flex items-center justify-between">
          <Badge variant="outline" className="text-xs">{skill.level}</Badge>
          {skill.evidences.length > 0 && (
            <button
              onClick={() => onToggleExpand(isExpanded ? new Set([...expandedSkills].filter(s => s !== skill.skillId)) : new Set(expandedSkills).add(skill.skillId))}
              className="text-xs text-primary hover:underline"
            >
              {isExpanded ? "Ẩn bằng chứng" : `${skill.evidences.length} bằng chứng`}
            </button>
          )}
        </div>

        {isExpanded && skill.evidences.length > 0 && (
          <>
            <Separator />
            <div className="space-y-2 max-h-60 overflow-y-auto">
              {skill.evidences.map((evidence, index) => (
                <div
                  key={`${evidence.source}-${evidence.title}-${index}`}
                  className="p-3 bg-card border rounded-lg hover:bg-accent/50 transition-colors"
                >
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <div className="flex-1 min-w-0">
                      <h5 className="font-medium text-foreground truncate">{evidence.title}</h5>
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
                        getScoreColor(evidence.score)
                      )}>
                        {evidence.score}/100
                      </span>
                      <span className="flex items-center gap-1 text-muted-foreground">
                        <Calendar className="h-3 w-3" />
                        {formatDate(evidence.recordedAt)}
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