import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Loader2, FileText, Eye, Download, AlertCircle, CheckCircle2, Smartphone, QrCode, ExternalLink } from "lucide-react";
import {
  type SectionType,
  type SectionPreviewData,
  type EducationPreview,
  type ProjectPreview,
  type CertificatePreview,
  type SkillPreview,
  type InternshipPreview,
  type SummaryPreview,
  getPublicCV,
} from "../../api/endpoints/profile.api";
import { candidateKeys } from "../../api/queryKeys";
import { Card, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";
import { Button } from "../../components/common/Button";
import { QRCodeSVG } from "qrcode.react";

const SECTION_TYPES: { type: SectionType; label: string; icon: React.ReactNode }[] = [
  { type: "SUMMARY", label: "Tóm tắt", icon: <FileText className="h-4 w-4" /> },
  { type: "SKILLS", label: "Kỹ năng", icon: <FileText className="h-4 w-4" /> },
  { type: "EDUCATION", label: "Học vấn", icon: <FileText className="h-4 w-4" /> },
  { type: "PROJECTS", label: "Dự án", icon: <FileText className="h-4 w-4" /> },
  { type: "CERTIFICATES", label: "Chứng chỉ", icon: <FileText className="h-4 w-4" /> },
  { type: "INTERNSHIP", label: "Thực tập", icon: <FileText className="h-4 w-4" /> },
];

const SECTION_ORDER = ["SUMMARY", "SKILLS", "EDUCATION", "PROJECTS", "CERTIFICATES", "INTERNSHIP"] as const;

function getSectionLabel(type: SectionType): string {
  return SECTION_TYPES.find(s => s.type === type)?.label || type;
}

function getSectionIcon(type: SectionType): React.ReactNode {
  return SECTION_TYPES.find(s => s.type === type)?.icon || <FileText className="h-4 w-4" />;
}

function sortSections(sections: SectionPreviewData[]): SectionPreviewData[] {
  return [...sections].sort((a, b) => {
    const orderA = SECTION_ORDER.indexOf(a.sectionType);
    const orderB = SECTION_ORDER.indexOf(b.sectionType);
    if (orderA !== orderB) return orderA - orderB;
    return a.sortOrder - b.sortOrder;
  });
}

const renderSectionContent = (sectionPreview: SectionPreviewData) => {
  const { sectionType, data } = sectionPreview;

  switch (sectionType) {
    case "SUMMARY":
      return (
        <div className="text-sm whitespace-pre-wrap text-foreground/80">
          {(data as SummaryPreview).summary || "Chưa có tóm tắt"}
        </div>
      );
    case "EDUCATION":
      return (
        <div className="space-y-3">
          {(data as EducationPreview[])?.map((edu, idx) => (
            <div key={idx} className="border-l-2 border-border pl-3 space-y-1">
              <p className="font-medium text-sm">{edu.school}</p>
              <p className="text-xs text-muted">{edu.major} | {edu.fromDate} - {edu.toDate || "Hiện tại"}</p>
              {edu.gpa && <p className="text-xs text-muted">GPA: {edu.gpa}</p>}
              {edu.note && <p className="text-xs text-muted">{edu.note}</p>}
            </div>
          )) || <p className="text-xs text-muted">Chưa có học vấn nào</p>}
        </div>
      );
    case "PROJECTS":
      return (
        <div className="space-y-3">
          {(data as ProjectPreview[])?.map((proj, idx) => (
            <div key={idx} className="border-l-2 border-border pl-3 space-y-1">
              <p className="font-medium text-sm">{proj.name}</p>
              {proj.role && <p className="text-xs text-muted">Vai trò: {proj.role}</p>}
              {proj.techStack && <p className="text-xs text-muted">Công nghệ: {proj.techStack}</p>}
              {proj.description && <p className="text-xs text-foreground/80">{proj.description}</p>}
              <div className="flex gap-2 mt-1">
                {proj.repoUrl && <a href={proj.repoUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-primary underline">Repo</a>}
                {proj.demoUrl && <a href={proj.demoUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-primary underline">Demo</a>}
              </div>
            </div>
          )) || <p className="text-xs text-muted">Chưa có dự án nào</p>}
        </div>
      );
    case "CERTIFICATES":
      return (
        <div className="space-y-3">
          {(data as CertificatePreview[])?.map((cert, idx) => (
            <div key={idx} className="border-l-2 border-border pl-3 space-y-1">
              <p className="font-medium text-sm">{cert.name}</p>
              <p className="text-xs text-muted">{cert.issuer} | {cert.issuedAt} {cert.expiresAt ? ` - ${cert.expiresAt}` : ""}</p>
            </div>
          )) || <p className="text-xs text-muted">Chưa có chứng chỉ nào</p>}
        </div>
      );
    case "SKILLS":
      return (
        <div className="flex flex-wrap gap-2">
          {(data as SkillPreview[])?.map((skill, idx) => (
            <Badge key={idx} variant={skill.verificationStatus === "Đã xác thực" ? "verified" : "outline"} size="sm" className="gap-1">
              {skill.skillName}
              <span className="text-xs">{skill.level}</span>
            </Badge>
          )) || <p className="text-xs text-muted">Chưa có kỹ năng nào</p>}
        </div>
      );
    case "INTERNSHIP":
      return (
        <div className="space-y-3">
          {(data as InternshipPreview[])?.map((intern, idx) => (
            <div key={idx} className="border-l-2 border-border pl-3 space-y-1">
              <p className="font-medium text-sm">{intern.batchName || intern.topicName || "Thực tập"}</p>
              {intern.topicName && <p className="text-xs text-muted">Đề tài: {intern.topicName}</p>}
              {intern.groupName && <p className="text-xs text-muted">Nhóm: {intern.groupName}</p>}
              {intern.startDate && intern.endDate && <p className="text-xs text-muted">{intern.startDate} - {intern.endDate}</p>}
              {intern.finalScore && <p className="text-xs text-muted">Điểm: {intern.finalScore}</p>}
              {intern.tasks && intern.tasks.length > 0 && (
                <div className="mt-1 space-y-1 ml-2 border-l border-border/50 pl-2">
                  {intern.tasks.map((task, tIdx) => (
                    <div key={tIdx} className="text-xs text-muted">
                      {task.title} {task.acceptedScore && `(${task.acceptedScore})`}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )) || <p className="text-xs text-muted">Chưa có thực tập nào</p>}
        </div>
      );
    default:
      return <p className="text-xs text-muted">Loại phần không được hỗ trợ</p>;
  }
};

export function PublicCVPage() {
  const { publicToken } = useParams<{ publicToken: string }>();

  const { data, isLoading, isError } = useQuery({
    queryKey: candidateKeys.profile.cvs.public(publicToken!),
    queryFn: () => getPublicCV(publicToken!),
    enabled: !!publicToken,
    retry: false,
  });

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="text-center">
          <Loader2 className="h-12 w-12 animate-spin text-primary mx-auto mb-4" />
          <p className="text-muted">Đang tải CV...</p>
        </div>
      </div>
    );
  }

  if (isError || !data?.data) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <Card className="max-w-md w-full mx-4">
          <CardContent className="py-12 text-center">
            <div className="mx-auto mb-4 p-4 rounded-full bg-rose-100 w-16 h-16 flex items-center justify-center">
              <AlertCircle className="h-8 w-8 text-rose-500" />
            </div>
            <h3 className="text-lg font-semibold text-foreground mb-2">Hồ sơ không công khai</h3>
            <p className="text-muted text-sm mb-6">
              CV này không tồn tại hoặc chưa được công khai.
            </p>
            <a href="/" className="text-primary hover:underline text-sm">
              ← Quay về trang chủ
            </a>
          </CardContent>
        </Card>
      </div>
    );
  }

  const cv = data.data;

  // Fetch preview data for richer display
  const { data: previewData } = useQuery({
    queryKey: ["public", "cv", "preview", publicToken],
    queryFn: async () => {
      const res = await fetch(`/api/v1/profiles/public/${publicToken}/preview`);
      if (!res.ok) return null;
      return res.json();
    },
    enabled: !!publicToken && cv.isPublic,
    retry: false,
  });

  return (
    <div className="min-h-screen bg-slate-50 py-12 px-4">
      <div className="max-w-3xl mx-auto">
        {/* Header */}
        <div className="mb-8 text-center">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 border border-primary/20 text-primary text-sm font-medium mb-4">
            <CheckCircle2 className="h-4 w-4" />
            CV công khai
          </div>
          <h1 className="text-3xl font-bold text-foreground mb-2">{cv.title}</h1>
          {cv.targetPosition && (
            <p className="text-primary font-medium mb-2">{cv.targetPosition}</p>
          )}
          <Badge variant="verified" size="md" className="gap-1">
            <Eye className="h-3.5 w-3.5" />
            Đang công khai
          </Badge>
        </div>

        {/* QR Code */}
        <div className="mb-8 text-center">
          <div className="inline-flex flex-col items-center gap-3 p-4 bg-white rounded-xl border border-border shadow-sm">
            <div className="bg-white p-4 rounded-lg border border-border">
              <QRCodeSVG
                value={window.location.href}
                size={120}
                level="M"
                includeMargin={true}
              />
            </div>
            <div className="flex items-center gap-2 text-sm text-muted">
              <Smartphone className="h-4 w-4" />
              <span>Quét mã QR để xem CV trên điện thoại</span>
            </div>
          </div>
        </div>

        {/* CV Content */}
        <Card className="bg-white shadow-sm">
          <CardContent className="p-8">
            {previewData?.data ? (
              // Use preview data with live profile data
              <div className="space-y-8">
                {sortSections(previewData.data.sections)
                  .filter((s: SectionPreviewData) => s.included)
                  .map((sectionPreview) => (
                    <div key={sectionPreview.sectionType} className="space-y-3">
                      <div className="flex items-center gap-2 pb-3 border-b border-border">
                        {getSectionIcon(sectionPreview.sectionType)}
                        <h2 className="text-xl font-semibold text-foreground">{getSectionLabel(sectionPreview.sectionType)}</h2>
                      </div>
                      <div className="pt-2">
                        {renderSectionContent(sectionPreview)}
                      </div>
                    </div>
                  ))}
                {(previewData.data.sections.length === 0 || previewData.data.sections.every((s: SectionPreviewData) => !s.included)) && (
                  <p className="text-muted text-center py-8">CV này chưa có nội dung được bật</p>
                )}
              </div>
            ) : (
              // Fallback to stored CV data (for backward compatibility)
              <div className="space-y-8">
                {cv.sections?.map((section: any) => (
                  <div key={section.id} className="space-y-3">
                    <div className="flex items-center gap-2 pb-3 border-b border-border">
                      <FileText className="h-6 w-6 text-primary" />
                      <h2 className="text-xl font-semibold text-foreground">{getSectionLabel(section.sectionType)}</h2>
                    </div>
                    <div className="pt-2">
                      {(() => {
                        try {
                          const content = JSON.parse(section.contentJson || "{}");
                          return renderSectionContent({ sectionType: section.sectionType, included: true, sortOrder: section.sortOrder, data: content });
                        } catch {
                          return <pre className="text-xs bg-slate-100 p-3 rounded overflow-auto">{section.contentJson}</pre>;
                        }
                      })()}
                    </div>
                  </div>
                ))}
                {(!cv.sections || cv.sections.length === 0) && (
                  <p className="text-muted text-center py-8">CV này chưa có nội dung</p>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Footer */}
        <div className="mt-8 text-center text-sm text-muted">
          <p>CV được tạo từ <strong>Rikkei Jobs</strong></p>
          <p className="mt-1">Mã định danh: {cv.publicToken}</p>
        </div>

        {/* PDF Download if available */}
        {cv.pdfUrl && (
          <div className="mt-6 text-center">
            <Button
              variant="outline"
              leftIcon={<Download className="h-4 w-4" />}
              onClick={() => window.open(`/api/v1/files/minio/${cv.pdfUrl}`, "_blank")}
              className="gap-2"
            >
              Tải PDF
            </Button>
          </div>
        )}

        {/* Share QR Code Section */}
        <Card className="mt-8 bg-primary/5 border-primary/20">
          <CardContent className="p-6">
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="text-center sm:text-left">
                <p className="font-medium text-foreground mb-1">Chia sẻ CV này</p>
                <p className="text-sm text-muted truncate max-w-xs sm:max-w-md">
                  {window.location.href}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="primary"
                  size="sm"
                  leftIcon={<ExternalLink className="h-4 w-4" />}
                  onClick={() => navigator.clipboard.writeText(window.location.href)}
                  className="gap-2"
                >
                  Sao chép link
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  leftIcon={<QrCode className="h-4 w-4" />}
                  onClick={() => {}}
                  className="gap-2"
                >
                  Mã QR
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}