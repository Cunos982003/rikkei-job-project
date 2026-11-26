import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Loader2, FileText, Eye, Download, AlertCircle, CheckCircle2, Smartphone } from "lucide-react";
import { type CVSection, getPublicCV } from "../../api/endpoints/profile.api";
import { candidateKeys } from "../../api/queryKeys";
import { Card, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";
import { Button } from "../../components/common/Button";
import { QRCodeSVG } from "qrcode.react";

function getSectionLabel(type: string): string {
  const labels: Record<string, string> = {
    personal_info: "Thông tin cá nhân",
    education: "Học vấn",
    experience: "Kinh nghiệm làm việc",
    skills: "Kỹ năng",
    projects: "Dự án",
    certificates: "Chứng chỉ",
    summary: "Tóm tắt",
  };
  return labels[type] || type;
}

function renderSectionContent(section: CVSection) {
  try {
    const content = JSON.parse(section.contentJson);
    switch (section.sectionType) {
      case "personal_info":
        return (
          <div className="space-y-2 text-sm">
            {content.fullName && <p><strong>Họ tên:</strong> {content.fullName}</p>}
            {content.email && <p><strong>Email:</strong> {content.email}</p>}
            {content.phone && <p><strong>SĐT:</strong> {content.phone}</p>}
            {content.address && <p><strong>Địa chỉ:</strong> {content.address}</p>}
            {content.dob && <p><strong>Ngày sinh:</strong> {content.dob}</p>}
            {content.gender && <p><strong>Giới tính:</strong> {content.gender}</p>}
            {content.headline && <p><strong>Headline:</strong> {content.headline}</p>}
          </div>
        );
      case "education":
        return (
          <div className="space-y-3">
            {content.educations?.map((edu: any, idx: number) => (
              <div key={idx} className="border-l-2 border-border pl-3 space-y-1">
                <p className="font-medium">{edu.school}</p>
                <p className="text-sm text-muted">{edu.major} | {edu.fromDate} - {edu.toDate || "Hiện tại"}</p>
                {edu.gpa && <p className="text-sm text-muted">GPA: {edu.gpa}</p>}
                {edu.note && <p className="text-sm text-muted">{edu.note}</p>}
              </div>
            ))}
          </div>
        );
      case "experience":
        return (
          <div className="space-y-3">
            {content.experiences?.map((exp: any, idx: number) => (
              <div key={idx} className="border-l-2 border-border pl-3 space-y-1">
                <p className="font-medium">{exp.position}</p>
                <p className="text-sm text-muted">{exp.company} | {exp.fromDate} - {exp.toDate || "Hiện tại"}</p>
                {exp.description && <p className="text-sm">{exp.description}</p>}
              </div>
            ))}
          </div>
        );
      case "skills":
        return (
          <div className="flex flex-wrap gap-2">
            {content.skills?.map((skill: any, idx: number) => (
              <Badge key={idx} variant="outline" size="sm">
                {skill.name} ({skill.level})
              </Badge>
            ))}
          </div>
        );
      case "projects":
        return (
          <div className="space-y-3">
            {content.projects?.map((proj: any, idx: number) => (
              <div key={idx} className="border-l-2 border-border pl-3 space-y-1">
                <p className="font-medium">{proj.name}</p>
                {proj.role && <p className="text-sm text-muted">Vai trò: {proj.role}</p>}
                {proj.techStack && <p className="text-sm text-muted">Công nghệ: {proj.techStack}</p>}
                {proj.description && <p className="text-sm">{proj.description}</p>}
                {proj.repoUrl && <a href={proj.repoUrl} target="_blank" rel="noopener noreferrer" className="text-sm text-primary underline">Repo</a>}
                {proj.demoUrl && <a href={proj.demoUrl} target="_blank" rel="noopener noreferrer" className="text-sm text-primary underline ml-2">Demo</a>}
              </div>
            ))}
          </div>
        );
      case "certificates":
        return (
          <div className="space-y-3">
            {content.certificates?.map((cert: any, idx: number) => (
              <div key={idx} className="border-l-2 border-border pl-3 space-y-1">
                <p className="font-medium">{cert.name}</p>
                <p className="text-sm text-muted">{cert.issuer} | {cert.issuedAt} {cert.expiresAt ? ` - {cert.expiresAt}` : ""}</p>
              </div>
            ))}
          </div>
        );
      case "summary":
        return (
          <div className="text-sm whitespace-pre-wrap">
            {content.summary}
          </div>
        );
      default:
        return <pre className="text-xs bg-slate-100 p-3 rounded overflow-auto">{section.contentJson}</pre>;
    }
  } catch {
    return <pre className="text-xs bg-slate-100 p-3 rounded overflow-auto">{section.contentJson}</pre>;
  }
}

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
            <h3 className="text-lg font-semibold text-foreground mb-2">Không tìm thấy CV</h3>
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
            <div className="space-y-8">
              {cv.sections?.map((section: CVSection) => (
                <div key={section.id} className="space-y-3">
                  <div className="flex items-center gap-2 pb-3 border-b border-border">
                    <FileText className="h-6 w-6 text-primary" />
                    <h2 className="text-xl font-semibold text-foreground">{getSectionLabel(section.sectionType)}</h2>
                  </div>
                  <div className="pt-2">
                    {renderSectionContent(section)}
                  </div>
                </div>
              ))}
              {(!cv.sections || cv.sections.length === 0) && (
                <p className="text-muted text-center py-8">CV này chưa có nội dung</p>
              )}
            </div>
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
      </div>
    </div>
  );
}