import { useState } from "react";
import { useAuthStore } from "../../store/useAuthStore";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Button } from "../../components/common/Button";
import { Input } from "../../components/common/Input";
import { Badge } from "../../components/common/Badge";
import {
  FileText,
  Plus,
  Edit2,
  Trash2,
  Eye,
  Download,
  ExternalLink,
  Loader2,
  X,
  Save,
  ChevronLeft,
  Settings,
  Copy,
  Check,
  AlertCircle,
} from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  type CV,
  type CVSection,
  type CVSectionType,
  type CreateCVRequest,
  type UpdateCVRequest,
  type UpdateCVSectionRequest,
  type GeneratePDFResponse,
  getMyCVs,
  createCV,
  updateCV,
  deleteCV,
  upsertCVSection,
  deleteCVSection,
  generateCVPDF,
} from "../../api/endpoints/profile.api";
import type { ApiResponse } from "../../api/types/api";
import { candidateKeys } from "../../api/queryKeys";

const SECTION_TYPES: { type: CVSectionType; label: string; icon: React.ReactNode }[] = [
  { type: "personal_info", label: "Thông tin cá nhân", icon: <FileText className="h-4 w-4" /> },
  { type: "education", label: "Học vấn", icon: <FileText className="h-4 w-4" /> },
  { type: "experience", label: "Kinh nghiệm làm việc", icon: <FileText className="h-4 w-4" /> },
  { type: "skills", label: "Kỹ năng", icon: <FileText className="h-4 w-4" /> },
  { type: "projects", label: "Dự án", icon: <FileText className="h-4 w-4" /> },
  { type: "certificates", label: "Chứng chỉ", icon: <FileText className="h-4 w-4" /> },
  { type: "summary", label: "Tóm tắt", icon: <FileText className="h-4 w-4" /> },
];

const SECTION_TEMPLATES: Record<CVSectionType, string> = {
  personal_info: JSON.stringify({
    fullName: "",
    email: "",
    phone: "",
    address: "",
    dob: "",
    gender: "",
    headline: "",
  }, null, 2),
  education: JSON.stringify({
    educations: [
      {
        school: "",
        major: "",
        fromDate: "",
        toDate: "",
        gpa: "",
        note: "",
      },
    ],
  }, null, 2),
  experience: JSON.stringify({
    experiences: [
      {
        position: "",
        company: "",
        fromDate: "",
        toDate: "",
        description: "",
      },
    ],
  }, null, 2),
  skills: JSON.stringify({
    skills: [
      {
        name: "",
        level: "Cơ bản",
      },
    ],
  }, null, 2),
  projects: JSON.stringify({
    projects: [
      {
        name: "",
        role: "",
        techStack: "",
        description: "",
        repoUrl: "",
        demoUrl: "",
      },
    ],
  }, null, 2),
  certificates: JSON.stringify({
    certificates: [
      {
        name: "",
        issuer: "",
        issuedAt: "",
        expiresAt: "",
      },
    ],
  }, null, 2),
  summary: JSON.stringify({
    summary: "",
  }, null, 2),
};

function getSectionLabel(type: CVSectionType): string {
  return SECTION_TYPES.find(s => s.type === type)?.label || type;
}

export function CandidateCVPage() {
  const { user } = useAuthStore();
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<"list" | "editor" | "preview">("list");
  const [editingCV, setEditingCV] = useState<CV | null>(null);
  const [editingSection, setEditingSection] = useState<CVSection | null>(null);
  const [selectedCVForPreview, setSelectedCVForPreview] = useState<CV | null>(null);
  const [cvForm, setCvForm] = useState<CreateCVRequest>({ title: "" });
  const [sectionForm, setSectionForm] = useState<UpdateCVSectionRequest>({
    sectionType: "personal_info",
    contentJson: SECTION_TEMPLATES.personal_info,
    sortOrder: 0,
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [sectionErrors, setSectionErrors] = useState<Record<string, string>>({});
  const [generatingPDF, setGeneratingPDF] = useState<{ cvId: string } | null>(null);
  const [copiedPublicUrl, setCopiedPublicUrl] = useState<string | null>(null);

  // Fetch CVs
  const {
    data: cvsData,
    isLoading: cvsLoading,
    error: cvsError,
    refetch: refetchCVs,
  } = useQuery({
    queryKey: candidateKeys.profile.cvs.list(user?.id),
    queryFn: () => getMyCVs(),
    enabled: Boolean(user?.id),
    staleTime: 5 * 60 * 1000,
  });

  const cvs = cvsData?.data ?? [];

  // Mutations
  const createCVMutation = useMutation({
    mutationFn: (data: CreateCVRequest) => createCV(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: candidateKeys.profile.cvs.all() });
      closeCVModal();
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setErrors({ form: error?.response?.data?.message || "Tạo CV thất bại" });
    },
  });

  const updateCVMutation = useMutation({
    mutationFn: ({ cvId, data }: { cvId: string; data: UpdateCVRequest }) => updateCV(cvId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: candidateKeys.profile.cvs.all() });
      closeCVModal();
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setErrors({ form: error?.response?.data?.message || "Cập nhật CV thất bại" });
    },
  });

  const deleteCVMutation = useMutation({
    mutationFn: (cvId: string) => deleteCV(cvId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: candidateKeys.profile.cvs.all() });
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setErrors({ form: error?.response?.data?.message || "Xóa CV thất bại" });
    },
  });

  const upsertSectionMutation = useMutation({
    mutationFn: ({ cvId, data }: { cvId: string; data: UpdateCVSectionRequest }) => upsertCVSection(cvId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: candidateKeys.profile.cvs.all() });
      closeSectionModal();
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setSectionErrors({ form: error?.response?.data?.message || "Cập nhật section thất bại" });
    },
  });

  const deleteSectionMutation = useMutation({
    mutationFn: ({ cvId, sectionType }: { cvId: string; sectionType: CVSectionType }) => deleteCVSection(cvId, sectionType),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: candidateKeys.profile.cvs.all() });
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setSectionErrors({ form: error?.response?.data?.message || "Xóa section thất bại" });
    },
  });

  const generatePDFMutation = useMutation({
    mutationFn: (cvId: string) => generateCVPDF(cvId),
    onSuccess: (res: ApiResponse<GeneratePDFResponse>) => {
      setGeneratingPDF(null);
      queryClient.invalidateQueries({ queryKey: candidateKeys.profile.cvs.all() });
      // Open PDF in new tab
      if (res.data?.pdfUrl) {
        window.open(`/api/v1/files/minio/${res.data.pdfUrl}`, "_blank");
      }
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setGeneratingPDF(null);
      setErrors({ form: error?.response?.data?.message || "Tạo PDF thất bại" });
    },
  });

  const copyPublicUrlMutation = useMutation({
    mutationFn: async (url: string) => {
      await navigator.clipboard.writeText(url);
      return url;
    },
    onSuccess: (url) => {
      setCopiedPublicUrl(url);
      setTimeout(() => setCopiedPublicUrl(null), 2000);
    },
  });

  // Handlers
  const closeCVModal = () => {
    setEditingCV(null);
    setCvForm({ title: "" });
    setErrors({});
    setActiveTab("list");
  };

  const closeSectionModal = () => {
    setEditingSection(null);
    setSectionForm({
      sectionType: "personal_info",
      contentJson: SECTION_TEMPLATES.personal_info,
      sortOrder: 0,
    });
    setSectionErrors({});
    setActiveTab("list");
  };

  const openCVModal = (cv?: CV) => {
    setErrors({});
    if (cv) {
      setEditingCV(cv);
      setCvForm({ title: cv.title });
    } else {
      setEditingCV(null);
      setCvForm({ title: "" });
    }
    setActiveTab("editor");
  };

  const handleCVSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const nextErrors: Record<string, string> = {};
    if (!cvForm.title.trim()) nextErrors.title = "Tiêu đề CV không được để trống";
    if (cvForm.title.length > 200) nextErrors.title = "Tiêu đề tối đa 200 ký tự";
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    if (editingCV?.id) {
      updateCVMutation.mutate({ cvId: editingCV.id, data: cvForm });
    } else {
      createCVMutation.mutate(cvForm);
    }
  };

  const handleDeleteCV = (cv: CV) => {
    if (confirm("Bạn có chắc chắn muốn xóa CV này? Hành động này không thể hoàn tác.")) {
      deleteCVMutation.mutate(cv.id);
    }
  };

  const openSectionModal = (section?: CVSection, cv?: CV) => {
    setSectionErrors({});
    if (section) {
      setEditingSection(section);
      setSectionForm({
        sectionType: section.sectionType,
        contentJson: section.contentJson,
        sortOrder: section.sortOrder,
      });
    } else {
      setEditingSection(null);
      setSectionForm({
        sectionType: "personal_info",
        contentJson: SECTION_TEMPLATES.personal_info,
        sortOrder: (cv?.sections?.length || 0),
      });
    }
    setActiveTab("editor");
  };

  const handleSectionSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const nextErrors: Record<string, string> = {};
    if (!sectionForm.sectionType) nextErrors.sectionType = "Loại section không được để trống";
    if (!sectionForm.contentJson.trim()) nextErrors.contentJson = "Nội dung section không được để trống";
    setSectionErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    if (!editingCV?.id) return;
    upsertSectionMutation.mutate({ cvId: editingCV.id, data: sectionForm });
  };

  const handleDeleteSection = (cvId: string, sectionType: CVSectionType) => {
    if (confirm("Bạn có chắc chắn muốn xóa section này?")) {
      deleteSectionMutation.mutate({ cvId, sectionType });
    }
  };

  const handleGeneratePDF = (cvId: string) => {
    setGeneratingPDF({ cvId });
    generatePDFMutation.mutate(cvId);
  };

  const handlePreviewCV = (cv: CV) => {
    setSelectedCVForPreview(cv);
    setActiveTab("preview");
  };

  const closePreview = () => {
    setSelectedCVForPreview(null);
    setActiveTab("list");
  };

  const handleCopyPublicUrl = (publicUrl: string) => {
    copyPublicUrlMutation.mutate(publicUrl);
  };

  const renderSectionContent = (section: CVSection) => {
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
  };

  // Render
  if (cvsLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (cvsError && !cvsData?.data) {
    return (
      <div className="text-center py-12 p-6 rounded-2xl border border-dashed border-border bg-surface max-w-md mx-auto my-12 shadow-xs">
        <AlertCircle className="h-12 w-12 text-rose-500 mx-auto mb-3" />
        <h3 className="text-base font-bold text-foreground">Không thể tải danh sách CV</h3>
        <Button variant="outline" size="sm" onClick={() => refetchCVs()} leftIcon={<Loader2 className="h-4 w-4" />}>
          Thử lại
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-primary/10">
            <FileText className="h-6 w-6 text-primary" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-foreground">CV của tôi</h2>
            <p className="text-sm text-muted mt-0.5">
              Tạo và quản lý CV, xuất PDF với mã QR để chia sẻ dễ dàng
            </p>
          </div>
        </div>
        <Button
          size="sm"
          variant="primary"
          onClick={() => openCVModal()}
          leftIcon={<Plus className="h-4 w-4" />}
          className="gap-2"
          disabled={cvs.length >= 5}
        >
          <span className="hidden sm:inline">Tạo CV mới</span>
          <span className="sm:hidden">Tạo</span>
        </Button>
      </div>

      {cvs.length > 0 && (
        <div className="flex flex-wrap items-center gap-4 text-sm text-muted bg-slate-50 rounded-lg p-4">
          <span className="flex items-center gap-1">
            <FileText className="h-4 w-4" />
            <strong className="text-foreground">{cvs.length}</strong> / 5 CV
          </span>
          {cvs.length >= 5 && (
            <Badge variant="warning" size="sm">Đã đạt tối đa</Badge>
          )}
        </div>
      )}

      {/* List View */}
      {activeTab === "list" && (
        <div className="space-y-4">
          {cvsError && !cvsData?.data ? null : cvs.length === 0 ? (
            <Card className="border-dashed border-primary/20 bg-primary/5">
              <CardContent className="py-16 text-center">
                <div className="mx-auto mb-4 p-4 rounded-full bg-primary/10 w-20 h-20 flex items-center justify-center">
                  <FileText className="h-10 w-10 text-primary" />
                </div>
                <h3 className="text-lg font-semibold text-foreground mb-2">Chưa có CV nào</h3>
                <p className="text-muted text-sm mb-6 max-w-sm mx-auto">
                  Hãy tạo CV đầu tiên để bắt đầu xây dựng hồ sơ xin việc chuyên nghiệp
                </p>
                <Button
                  variant="primary"
                  onClick={() => openCVModal()}
                  leftIcon={<Plus className="h-4 w-4" />}
                  size="lg"
                  className="gap-2"
                  disabled={cvs.length >= 5}
                >
                  Tạo CV đầu tiên
                </Button>
              </CardContent>
            </Card>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {cvs.map((cv: CV, index: number) => (
                <Card
                  key={cv.id}
                  hoverable
                  className="h-full flex flex-col relative overflow-hidden transition-all duration-300 group"
                  style={{ animationDelay: `${index * 50}ms` }}
                >
                  <CardContent className="p-5 flex flex-1 flex-col gap-3">
                    <div className="flex items-start justify-between gap-2">
                      <h4 className="font-semibold text-foreground truncate min-w-0 group-hover:text-primary transition-colors"
                          title={cv.title}>
                        {cv.title}
                      </h4>
                      <div className="flex items-center gap-1 shrink-0">
                        {cv.isPublic ? (
                          <Badge variant="verified" size="sm" showIcon className="gap-1 whitespace-nowrap">
                            <Eye className="h-2.5 w-2.5" />
                            Công khai
                          </Badge>
                        ) : (
                          <Badge variant="outline" size="sm" showIcon className="gap-1 whitespace-nowrap">
                            <Eye className="h-2.5 w-2.5" />
                            Riêng tư
                          </Badge>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-4 text-xs text-muted">
                      <span className="flex items-center gap-1">
                        <FileText className="h-3.5 w-3.5" />
                        {cv.sections?.length || 0} sections
                      </span>
                      {cv.pdfUrl && (
                        <span className="flex items-center gap-1 text-emerald-600">
                          <Download className="h-3.5 w-3.5" />
                          Có PDF
                        </span>
                      )}
                    </div>

                    <div className="pt-2 border-t border-border flex flex-col gap-2">
                      <div className="flex flex-wrap gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="flex-1 flex items-center justify-center gap-1"
                          onClick={() => openCVModal(cv)}
                          title="Chỉnh sửa CV"
                        >
                          <Edit2 className="h-4 w-4" />
                          <span className="hidden sm:inline">Sửa</span>
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          className="flex-1 flex items-center justify-center gap-1"
                          onClick={() => handlePreviewCV(cv)}
                          title="Xem trước"
                        >
                          <Eye className="h-4 w-4" />
                          <span className="hidden sm:inline">Xem</span>
                        </Button>
                        {cv.pdfUrl ? (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="flex-1 flex items-center justify-center gap-1"
                            onClick={() => window.open(`/api/v1/files/minio/${cv.pdfUrl}`, "_blank")}
                            title="Tải PDF"
                          >
                            <Download className="h-4 w-4" />
                            <span className="hidden sm:inline">PDF</span>
                          </Button>
                        ) : (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="flex-1 flex items-center justify-center gap-1"
                            onClick={() => handleGeneratePDF(cv.id)}
                            title="Tạo PDF"
                            isLoading={generatingPDF?.cvId === cv.id}
                          >
                            <Loader2 className="h-4 w-4" />
                            <span className="hidden sm:inline">Tạo PDF</span>
                          </Button>
                        )}
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {cv.isPublic && (
                          <Button
                            variant="outline"
                            size="sm"
                            className="flex-1 flex items-center justify-center gap-1"
                            onClick={() => handleCopyPublicUrl(`${window.location.origin}/p/${cv.publicToken}`)}
                            title="Sao chép link công khai"
                          >
                            <ExternalLink className="h-4 w-4" />
                            <span className="hidden sm:inline">Chia sẻ</span>
                            {copiedPublicUrl === `${window.location.origin}/p/${cv.publicToken}` && (
                              <Check className="h-4 w-4 text-emerald-500" />
                            )}
                          </Button>
                        )}
                        <Button
                          variant="danger"
                          size="sm"
                          className="flex-1 flex items-center justify-center gap-1"
                          onClick={() => handleDeleteCV(cv)}
                          title="Xóa CV"
                        >
                          <Trash2 className="h-4 w-4" />
                          <span className="hidden sm:inline">Xóa</span>
                        </Button>
                      </div>
                    </div>

                    {/* Sections preview */}
                    {cv.sections && cv.sections.length > 0 && (
                      <div className="pt-2 border-t border-border">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-xs font-medium text-muted flex items-center gap-1">
                            <FileText className="h-3.5 w-3.5" />
                            Các section
                          </span>
                          <Button
                            variant="outline"
                            size="sm"
                            leftIcon={<Plus className="h-3 w-3" />}
                            onClick={() => openSectionModal(undefined, cv)}
                            disabled={cv.sections.length >= 7}
                          >
                            Thêm section
                          </Button>
                        </div>
                        <div className="space-y-2 max-h-60 overflow-y-auto">
                          {cv.sections.map((section: CVSection) => (
                            <div key={section.id} className="flex items-center justify-between p-2 bg-slate-50 rounded-lg text-xs">
                              <span className="flex items-center gap-1 truncate">
                                <Badge variant="outline" size="sm" className="shrink-0">
                                  {getSectionLabel(section.sectionType)}
                                </Badge>
                                <span className="text-muted">#{section.sortOrder + 1}</span>
                              </span>
                              <div className="flex items-center gap-1 shrink-0">
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  className="h-7 w-7"
                                  onClick={() => openSectionModal(section, cv)}
                                  title="Chỉnh sửa"
                                >
                                  <Edit2 className="h-3.5 w-3.5" />
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  className="h-7 w-7 text-rose-500 hover:bg-rose-50"
                                  onClick={() => handleDeleteSection(cv.id, section.sectionType)}
                                  title="Xóa"
                                >
                                  <Trash2 className="h-3.5 w-3.5" />
                                </Button>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Editor View */}
      {activeTab === "editor" && (
        <div className="space-y-6">
          {/* CV Info Form */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <FileText className="h-5 w-5 text-primary" />
                <span>{editingCV ? "Chỉnh sửa CV" : "Tạo CV mới"}</span>
              </CardTitle>
              <CardDescription>Thông tin cơ bản của CV</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCVSubmit} className="space-y-4">
                <Input
                  label="Tiêu đề CV *"
                  value={cvForm.title}
                  onChange={(e) => setCvForm({ ...cvForm, title: e.target.value })}
                  placeholder="Ví dụ: CV Nguyễn Văn A - Java Developer"
                  error={errors.title}
                  maxLength={200}
                />
                {editingCV && (
                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="isPublic"
                      checked={editingCV.isPublic}
                      onChange={(e) => updateCVMutation.mutate({ cvId: editingCV.id, data: { isPublic: e.target.checked } })}
                      className="h-4 w-4 rounded border-border text-primary focus:ring-primary"
                    />
                    <label htmlFor="isPublic" className="text-sm text-foreground">
                      Hiển thị công khai (có thể truy cập qua link /p/{"{"}publicToken{"}"})
                    </label>
                  </div>
                )}
                {errors.form && <p className="text-xs text-danger font-medium">{errors.form}</p>}
                <div className="flex justify-end gap-3 pt-2">
                  <Button type="button" variant="outline" onClick={closeCVModal}>
                    <X className="h-4 w-4" />
                    Hủy
                  </Button>
                  <Button
                    type="submit"
                    variant="primary"
                    isLoading={createCVMutation.isPending || updateCVMutation.isPending}
                  >
                    <Save className="h-4 w-4" />
                    {editingCV ? "Cập nhật" : "Tạo"}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>

          {/* Section Editor */}
          {editingCV && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Settings className="h-5 w-5 text-primary" />
                  <span>{editingSection ? "Chỉnh sửa section" : "Thêm section mới"}</span>
                </CardTitle>
                <CardDescription>Chọn loại section và nhập nội dung (định dạng JSON)</CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleSectionSubmit} className="space-y-4">
                  <div className="grid sm:grid-cols-2 gap-4">
                    <div className="w-full space-y-1.5">
                      <label className="block text-sm font-medium text-foreground">Loại section *</label>
                      <select
                        value={sectionForm.sectionType}
                        onChange={(e) => {
                          const type = e.target.value as CVSectionType;
                          setSectionForm(prev => ({
                            ...prev,
                            sectionType: type,
                            contentJson: SECTION_TEMPLATES[type] || "{}",
                          }));
                        }}
                        className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
                      >
                        {SECTION_TYPES.map(({ type, label }: { type: CVSectionType; label: string }) => (
                          <option key={type} value={type}>{label}</option>
                        ))}
                      </select>
                    </div>
                    <div className="w-full space-y-1.5">
                      <label className="block text-sm font-medium text-foreground">Thứ tự hiển thị</label>
                      <input
                        type="number"
                        value={sectionForm.sortOrder}
                        onChange={(e) => setSectionForm(prev => ({ ...prev, sortOrder: parseInt(e.target.value) || 0 }))}
                        min="0"
                        className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
                      />
                    </div>
                  </div>

                  <div className="w-full space-y-1.5">
                    <label className="block text-sm font-medium text-foreground">Nội dung JSON *</label>
                    <textarea
                      value={sectionForm.contentJson}
                      onChange={(e) => setSectionForm(prev => ({ ...prev, contentJson: e.target.value }))}
                      rows={12}
                      className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm font-mono text-foreground placeholder:text-muted outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
                      placeholder={SECTION_TEMPLATES[sectionForm.sectionType]}
                      spellCheck={false}
                    />
                    <p className="text-xs text-muted">
                      Xem mẫu cấu trúc JSON cho từng loại section. Hãy giữ đúng định dạng để hiển thị đúng khi xuất PDF.
                    </p>
                    {sectionErrors.contentJson && <p className="text-xs text-danger font-medium">{sectionErrors.contentJson}</p>}
                    {sectionErrors.form && <p className="text-xs text-danger font-medium">{sectionErrors.form}</p>}
                  </div>

                  <div className="flex justify-end gap-3 pt-2">
                    <Button type="button" variant="outline" onClick={() => { setEditingSection(null); setActiveTab("list"); }}>
                      <X className="h-4 w-4" />
                      Hủy
                    </Button>
                    <Button
                      type="submit"
                      variant="primary"
                      isLoading={upsertSectionMutation.isPending}
                    >
                      <Save className="h-4 w-4" />
                      {editingSection ? "Cập nhật" : "Thêm"}
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>
          )}

          {/* Available Sections */}
          {editingCV && !editingSection && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <FileText className="h-5 w-5 text-primary" />
                  <span>Các loại section có sẵn</span>
                </CardTitle>
                <CardDescription>Chọn một loại section để thêm vào CV</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {SECTION_TYPES.map(({ type, label, icon }: { type: CVSectionType; label: string; icon: React.ReactNode }) => {
                    const hasSection = editingCV.sections?.some(s => s.sectionType === type);
                    return (
                      <Button
                        key={type}
                        variant={hasSection ? "secondary" : "outline"}
                        size="sm"
                        className="h-24 flex flex-col items-center justify-center gap-2 text-left"
                        onClick={() => openSectionModal(undefined, editingCV!)}
                        disabled={hasSection || (editingCV.sections?.length || 0) >= 7}
                      >
                        <div className="text-2xl">{icon}</div>
                        <span className="font-medium">{label}</span>
                        {hasSection && <Badge variant="verified" size="sm">Đã có</Badge>}
                      </Button>
                    );
                  })}
                </div>
                {editingCV.sections && editingCV.sections.length >= 7 && (
                  <p className="text-xs text-muted text-center mt-4">CV tối đa 7 section (mỗi loại một section)</p>
                )}
              </CardContent>
            </Card>
          )}
        </div>
      )}

      {/* Preview View */}
      {activeTab === "preview" && selectedCVForPreview && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <Button variant="ghost" size="sm" onClick={closePreview} leftIcon={<ChevronLeft className="h-4 w-4" />}>
              Quay lại
            </Button>
            <h2 className="text-xl font-bold text-foreground">{selectedCVForPreview.title}</h2>
            <div className="flex items-center gap-2">
              <Badge variant={selectedCVForPreview.isPublic ? "verified" : "outline"} size="sm">
                {selectedCVForPreview.isPublic ? "Công khai" : "Riêng tư"}
              </Badge>
              {selectedCVForPreview.pdfUrl && (
                <Button
                  variant="outline"
                  size="sm"
                  leftIcon={<Download className="h-4 w-4" />}
                  onClick={() => window.open(`/api/v1/files/minio/${selectedCVForPreview.pdfUrl}`, "_blank")}
                >
                  Tải PDF
                </Button>
              )}
            </div>
          </div>

          <Card className="bg-white">
            <CardContent className="p-8 max-w-3xl mx-auto">
              <div className="space-y-6">
                {selectedCVForPreview.sections?.map((section: CVSection) => (
                  <div key={section.id} className="space-y-2">
                    <div className="flex items-center gap-2 pb-2 border-b border-border">
                      <FileText className="h-5 w-5 text-primary" />
                      <h3 className="text-lg font-semibold text-foreground">{getSectionLabel(section.sectionType)}</h3>
                    </div>
                    <div className="pt-2">
                      {renderSectionContent(section)}
                    </div>
                  </div>
                ))}
                {(!selectedCVForPreview.sections || selectedCVForPreview.sections.length === 0) && (
                  <p className="text-muted text-center py-8">CV này chưa có section nào</p>
                )}
              </div>
            </CardContent>
          </Card>

          {selectedCVForPreview.isPublic && (
            <Card className="bg-primary/5 border-primary/20">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-medium text-foreground">Link công khai CV</p>
                    <p className="text-sm text-muted truncate max-w-md">
                      {window.location.origin}/p/{selectedCVForPreview.publicToken}
                    </p>
                  </div>
                  <Button
                    variant="primary"
                    size="sm"
                    leftIcon={<Copy className="h-4 w-4" />}
                    onClick={() => handleCopyPublicUrl(`${window.location.origin}/p/${selectedCVForPreview.publicToken}`)}
                  >
                    {copiedPublicUrl === `${window.location.origin}/p/${selectedCVForPreview.publicToken}` ? "Đã sao chép" : "Sao chép link"}
                  </Button>
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      )}
    </div>
  );
}