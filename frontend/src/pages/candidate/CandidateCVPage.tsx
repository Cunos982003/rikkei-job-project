import { useState, useEffect } from "react";
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
  Target,
  LayoutDashboard,
  QrCode,
  GripVertical,
  Sparkles,
} from "lucide-react";
import { AIDraftCompareModal } from "../../components/cv/AIDraftCompareModal";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import { arrayMove, SortableContext, sortableKeyboardCoordinates, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import {
  type CV,
  type CVSectionDto,
  type SectionType,
  type CreateCVRequest,
  type UpdateCVRequest,
  type UpdateCVSectionRequest,
  type GeneratePDFResponse,
  type CVPreviewResponse,
  type SectionPreviewData,
  type EducationPreview,
  type ProjectPreview,
  type CertificatePreview,
  type SkillPreview,
  type InternshipPreview,
  type SummaryPreview,
  type ReorderCVSectionsRequest,
  getMyCVs,
  createCV,
  updateCV,
  deleteCV,
  upsertCVSection,
  deleteCVSection,
  getCVPreview,
  generateCVPDF,
  downloadPublicPdf,
  reorderCVSections,
} from "../../api/endpoints/profile.api";
import { profileApi } from "../../api/endpoints/profile.api";
import type { ApiResponse } from "../../api/types/api";
import { candidateKeys } from "../../api/queryKeys";
import { QRCodeSVG } from "qrcode.react";

const SECTION_TYPES: { type: SectionType; label: string; icon: React.ReactNode; description: string }[] = [
  { type: "SUMMARY", label: "Tóm tắt", icon: <FileText className="h-4 w-4" />, description: "Mục tiêu nghề nghiệp, điểm mạnh chính" },
  { type: "SKILLS", label: "Kỹ năng", icon: <Target className="h-4 w-4" />, description: "Kỹ năng chuyên môn, mềm, ngôn ngữ" },
  { type: "EDUCATION", label: "Học vấn", icon: <LayoutDashboard className="h-4 w-4" />, description: "Trình độ đào tạo, chứng chỉ, khóa học" },
  { type: "PROJECTS", label: "Dự án", icon: <LayoutDashboard className="h-4 w-4" />, description: "Dự án tiêu biểu, vai trò, công nghệ" },
  { type: "CERTIFICATES", label: "Chứng chỉ", icon: <FileText className="h-4 w-4" />, description: "Chứng chỉ chuyên ngành, đào tạo" },
  { type: "INTERNSHIP", label: "Thực tập", icon: <Target className="h-4 w-4" />, description: "Kinh nghiệm thực tập, đề tài, nhóm" },
];

const SECTION_ORDER = ["SUMMARY", "SKILLS", "EDUCATION", "PROJECTS", "CERTIFICATES", "INTERNSHIP"] as const;

function getSectionLabel(type: SectionType): string {
  return SECTION_TYPES.find(s => s.type === type)?.label || type;
}

function getSectionIcon(type: SectionType): React.ReactNode {
  return SECTION_TYPES.find(s => s.type === type)?.icon || <FileText className="h-4 w-4" />;
}

function sortSections(sections: CVSectionDto[]): CVSectionDto[] {
  return [...sections].sort((a, b) => {
    const orderA = SECTION_ORDER.indexOf(a.sectionType);
    const orderB = SECTION_ORDER.indexOf(b.sectionType);
    if (orderA !== orderB) return orderA - orderB;
    return a.sortOrder - b.sortOrder;
  });
}

export function CandidateCVPage() {
  const { user } = useAuthStore();
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<"list" | "editor" | "preview">("list");
  const [editingCV, setEditingCV] = useState<CV | null>(null);
  const [editingSection, setEditingSection] = useState<CVSectionDto | null>(null);
  const [aiDraftTarget, setAiDraftTarget] = useState<{
    cvId: string;
    section: "SUMMARY" | "PROJECT";
    sectionTitle: string;
    projectId?: string | null;
    currentText: string;
  } | null>(null);
  const [selectedCVForPreview, setSelectedCVForPreview] = useState<CV | null>(null);
  const [cvForm, setCvForm] = useState<CreateCVRequest & { isPublic?: boolean }>({ title: "", targetPosition: "", template: "RIKKEI_STANDARD", isPublic: false });
  const [sectionForm, setSectionForm] = useState<UpdateCVSectionRequest>({
    sectionType: "SUMMARY",
    itemIds: [],
    included: true,
    sortOrder: 0,
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [sectionErrors, setSectionErrors] = useState<Record<string, string>>({});
  const [generatingPDF, setGeneratingPDF] = useState<{ cvId: string } | null>(null);
  const [copiedPublicUrl, setCopiedPublicUrl] = useState<string | null>(null);
  const [previewData, setPreviewData] = useState<CVPreviewResponse | null>(null);
  const [isTogglingItems, setIsTogglingItems] = useState(false);

  // DnD sensors - must be at top level (Rules of Hooks)
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
  );

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

  // Preview query
  const {
    data: previewQueryData,
    isLoading: previewQueryLoading,
    refetch: refetchPreview,
  } = useQuery({
    queryKey: editingCV ? candidateKeys.profile.cvs.preview(editingCV.id) : [],
    queryFn: () => getCVPreview(editingCV!.id),
    enabled: Boolean(editingCV && activeTab === "editor"),
    staleTime: 30 * 1000,
  });

  // Update preview data when query result changes
  useEffect(() => {
    if (previewQueryData?.data) {
      setPreviewData(previewQueryData.data);
    }
  }, [previewQueryData]);

  // Profile data queries for section editor (fetch live data from profile tables)
  const { data: profileData } = useQuery({
    queryKey: candidateKeys.profile.detail(user?.id),
    queryFn: () => profileApi.getMyProfile(),
    enabled: Boolean(user?.id && activeTab === "editor"),
    staleTime: 5 * 60 * 1000,
  });

  const { data: educationsData } = useQuery({
    queryKey: candidateKeys.profile.educations(user?.id),
    queryFn: () => profileApi.getEducations(),
    enabled: Boolean(user?.id && activeTab === "editor"),
    staleTime: 5 * 60 * 1000,
  });

  const { data: certificatesData } = useQuery({
    queryKey: candidateKeys.profile.certificates(user?.id),
    queryFn: () => profileApi.getCertificates(),
    enabled: Boolean(user?.id && activeTab === "editor"),
    staleTime: 5 * 60 * 1000,
  });

  const { data: portfolioData } = useQuery({
    queryKey: candidateKeys.profile.portfolio(user?.id),
    queryFn: () => profileApi.getMyPortfolio(),
    enabled: Boolean(user?.id && activeTab === "editor"),
    staleTime: 5 * 60 * 1000,
  });

  const { data: internshipsData } = useQuery({
    queryKey: candidateKeys.profile.internships(user?.id),
    queryFn: async (): Promise<{ success: boolean; data: any[] }> => {
      try {
        return await profileApi.getMyInternships();
      } catch {
        return { success: true, data: [] };
      }
    },
    enabled: Boolean(user?.id && activeTab === "editor"),
    staleTime: 5 * 60 * 1000,
    retry: false,
  });

  const { data: skillsData } = useQuery({
    queryKey: candidateKeys.skills.byUser(user?.id),
    queryFn: () => profileApi.getMySkills(),
    enabled: Boolean(user?.id && activeTab === "editor"),
    staleTime: 5 * 60 * 1000,
  });

  // Mutations
  const createCVMutation = useMutation({
    mutationFn: (data: CreateCVRequest) => createCV(data),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: candidateKeys.profile.cvs.all() });
      if (response.data && activeTab === "editor") {
        setEditingCV(response.data);
        setCvForm({ title: response.data.title, targetPosition: response.data.targetPosition || "", template: response.data.template || "RIKKEI_STANDARD" });
        refetchPreview();
      } else {
        closeCVModal();
      }
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setErrors({ form: error?.response?.data?.message || "Tạo CV thất bại" });
    },
  });

  const updateCVMutation = useMutation({
    mutationFn: ({ cvId, data }: { cvId: string; data: UpdateCVRequest }) => updateCV(cvId, data),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: candidateKeys.profile.cvs.all() });
      if (activeTab === "editor" && response.data) {
        setEditingCV(prev => prev ? { ...prev, ...response.data } : null);
        refetchPreview();
        closeCVModal(); // Return to list view after successful update
      }
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
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: candidateKeys.profile.cvs.all() });
      if (activeTab === "editor" && editingCV && response.data) {
        // Update the section in editingCV
        setEditingCV(prev => {
          if (!prev) return null;
          const sections = prev.sections || [];
          const existingIndex = sections.findIndex(s => s.sectionType === response.data?.sectionType);
          const newSection = response.data!;
          if (existingIndex >= 0) {
            const newSections = [...sections];
            newSections[existingIndex] = newSection;
            return { ...prev, sections: newSections };
          } else {
            return { ...prev, sections: [...sections, newSection] };
          }
        });
        refetchPreview();
      }
      // Only close modal if not just toggling items (which uses a different flow)
      if (editingSection && !isTogglingItems) {
        closeSectionModal();
      }
      setIsTogglingItems(false);
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setSectionErrors({ form: error?.response?.data?.message || "Cập nhật section thất bại" });
    },
  });

  const deleteSectionMutation = useMutation({
    mutationFn: ({ cvId, sectionType }: { cvId: string; sectionType: SectionType }) => deleteCVSection(cvId, sectionType),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: candidateKeys.profile.cvs.all() });
      if (activeTab === "editor" && editingCV) {
        setEditingCV(prev => {
          if (!prev) return null;
          return { ...prev, sections: (prev.sections || []).filter(s => s.sectionType !== variables.sectionType) };
        });
        refetchPreview();
      }
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

  const downloadPublicPdfMutation = useMutation({
    mutationFn: (publicToken: string) => downloadPublicPdf(publicToken),
    onSuccess: (response, publicToken) => {
      const blob = response.data;
      if (blob) {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `CV-${publicToken}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      }
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setErrors({ form: error?.response?.data?.message || "Tải PDF thất bại" });
    },
  });

  const reorderCVSectionsMutation = useMutation({
    mutationFn: ({ cvId, data }: { cvId: string; data: ReorderCVSectionsRequest }) =>
      reorderCVSections(cvId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: candidateKeys.profile.cvs.all() });
      if (activeTab === "editor" && editingCV) {
        refetchPreview();
      }
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setErrors({ form: error?.response?.data?.message || "Sắp xếp lại thất bại" });
    },
  });

  // Handlers
  const closeCVModal = () => {
    setEditingCV(null);
    setCvForm({ title: "", targetPosition: "", template: "RIKKEI_STANDARD", isPublic: false });
    setErrors({});
    setActiveTab("list");
    setPreviewData(null);
  };

  const closeSectionModal = () => {
    setEditingSection(null);
    setSectionForm({
      sectionType: "SUMMARY",
      itemIds: [],
      included: true,
      sortOrder: 0,
    });
    setSectionErrors({});
  };

  const openCVModal = (cv?: CV) => {
    setErrors({});
    if (cv) {
      setEditingCV(cv);
      setCvForm({ title: cv.title, targetPosition: cv.targetPosition || "", template: cv.template || "RIKKEI_STANDARD", isPublic: cv.isPublic });
      // refetchPreview will be triggered by the query's enabled condition when editingCV changes
    } else {
      setEditingCV(null);
      setCvForm({ title: "", targetPosition: "", template: "RIKKEI_STANDARD", isPublic: false });
      setPreviewData(null);
    }
    setActiveTab("editor");
  };

  const handleCVSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const nextErrors: Record<string, string> = {};
    if (!cvForm.title.trim()) nextErrors.title = "Tiêu đề CV không được để trống";
    if (cvForm.title.length > 200) nextErrors.title = "Tiêu đề tối đa 200 ký tự";
    if (cvForm.targetPosition && cvForm.targetPosition.length > 200) nextErrors.targetPosition = "Vị trí mục tiêu tối đa 200 ký tự";
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

  const openSectionModal = (section?: CVSectionDto, cv?: CV, sectionType?: SectionType) => {
    setSectionErrors({});
    if (cv) {
      setEditingCV(cv);
      setActiveTab("editor");
    }
    if (section) {
      setEditingSection(section);
      setSectionForm({
        sectionType: section.sectionType,
        itemIds: section.itemIds || [],
        included: section.included,
        sortOrder: section.sortOrder,
      });
    } else {
      setEditingSection({ sectionType: sectionType || "SUMMARY" } as CVSectionDto);
      const existingSections = cv?.sections || [];
      const nextOrder = existingSections.length > 0 ? Math.max(...existingSections.map(s => s.sortOrder)) + 1 : 0;
      setSectionForm({
        sectionType: sectionType || "SUMMARY",
        itemIds: [],
        included: true,
        sortOrder: nextOrder,
      });
    }
  };

  const handleSectionSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const nextErrors: Record<string, string> = {};
    if (!sectionForm.sectionType) nextErrors.sectionType = "Loại section không được để trống";
    setSectionErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    if (!editingCV?.id) return;
    upsertSectionMutation.mutate({ cvId: editingCV.id, data: sectionForm });
  };

  const handleDeleteSection = (cvId: string, sectionType: SectionType) => {
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

  const handleTogglePublic = (cv: CV) => {
    const newIsPublic = !cv.isPublic;
    // Optimistically update local state for immediate UI feedback
    if (editingCV?.id === cv.id) {
      setEditingCV(prev => prev ? { ...prev, isPublic: newIsPublic } : null);
    }
    updateCVMutation.mutate({ cvId: cv.id, data: { isPublic: newIsPublic } });
  };

  const handleDownloadPublicPdf = (publicToken: string) => {
    downloadPublicPdfMutation.mutate(publicToken);
  };

  // Get available items for a section type from profile data queries
  const getAvailableItems = (sectionType: SectionType) => {
    switch (sectionType) {
      case "SUMMARY":
        return profileData?.data ? [{ ...profileData.data, id: "summary" }] : [];
      case "EDUCATION":
        return educationsData?.data ?? [];
      case "CERTIFICATES":
        return certificatesData?.data ?? [];
      case "PROJECTS":
        return portfolioData?.data ?? [];
      case "INTERNSHIP":
        return internshipsData?.data ?? [];
      case "SKILLS":
        return skillsData?.data ?? [];
      default:
        return [];
    }
  };

  // Check if item is selected in section
  const isItemSelected = (sectionType: SectionType, itemId: string) => {
    const section = editingCV?.sections?.find(s => s.sectionType === sectionType);
    return section?.itemIds?.includes(itemId) ?? false;
  };

  // Toggle item selection
  const toggleItemSelection = (sectionType: SectionType, itemId: string) => {
    if (!editingCV) return;
    const section = editingCV.sections?.find(s => s.sectionType === sectionType);
    const currentItemIds = section?.itemIds || [];
    const newItemIds = currentItemIds.includes(itemId)
      ? currentItemIds.filter(id => id !== itemId)
      : [...currentItemIds, itemId];

    // Update local form state so it stays in sync
    setSectionForm(prev => ({ ...prev, itemIds: newItemIds }));

    setIsTogglingItems(true);
    upsertSectionMutation.mutate({
      cvId: editingCV.id,
      data: {
        sectionType,
        itemIds: newItemIds,
        included: sectionForm.included,
        sortOrder: sectionForm.sortOrder
      },
    });
  };

  // Get display name for item
  const getItemDisplayName = (sectionType: SectionType, item: any): string => {
    switch (sectionType) {
      case "EDUCATION":
        return item.school;
      case "PROJECTS":
        return item.name;
      case "CERTIFICATES":
        return item.name;
      case "SKILLS":
        return item.skillName;
      case "INTERNSHIP":
        return item.batchName || item.topicName || "Thực tập";
      default:
        return item.name || item.title || "Không tên";
    }
  };

  // Get display subtitle for item
  const getItemSubtitle = (sectionType: SectionType, item: any): string => {
    switch (sectionType) {
      case "EDUCATION":
        return `${item.major} | ${item.fromDate} - ${item.toDate || "Hiện tại"}`;
      case "PROJECTS":
        return `${item.role || "Thành viên"} | ${item.techStack || "N/A"}`;
      case "CERTIFICATES":
        return `${item.issuer} | ${item.issuedAt}`;
      case "SKILLS":
        return `${item.level} • ${item.verificationStatus}`;
      case "INTERNSHIP":
        return `${item.topicName || ""} ${item.groupName ? `| ${item.groupName}` : ""}`;
      default:
        return "";
    }
  };

  // Render preview section
  const renderPreviewSection = (sectionPreview: SectionPreviewData) => {
    if (!sectionPreview.included) return null;

    const { sectionType, data } = sectionPreview;
    const icon = getSectionIcon(sectionType);
    const label = getSectionLabel(sectionType);

    return (
      <div key={sectionType} className="space-y-3">
        <div className="flex items-center gap-2 pb-2 border-b border-border">
          {icon}
          <h3 className="text-base font-semibold text-foreground">{label}</h3>
        </div>
        <div className="pt-1 space-y-2">
          {renderSectionContent(sectionType, data)}
        </div>
      </div>
    );
  };

  // Render section content based on type
  const renderSectionContent = (sectionType: SectionType, data: any) => {
    switch (sectionType) {
      case "SUMMARY":
        return (
          <div className="space-y-2">
            <div className="text-sm whitespace-pre-wrap text-foreground/80">
              {(data as SummaryPreview).summary || "Chưa có tóm tắt"}
            </div>
            {editingCV && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                leftIcon={<Sparkles className="h-3.5 w-3.5 text-primary" />}
                onClick={() =>
                  setAiDraftTarget({
                    cvId: editingCV.id,
                    section: "SUMMARY",
                    sectionTitle: "Giới thiệu bản thân",
                    currentText: (data as SummaryPreview).summary || profileData?.data?.summary || "",
                  })
                }
              >
                AI viết giúp
              </Button>
            )}
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
            )) || <p className="text-xs text-muted">Chưa có học vấn nào được chọn</p>}
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
                {editingCV && proj.id && (
                  <div className="pt-1">
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="h-7 px-2 text-xs text-primary"
                      leftIcon={<Sparkles className="h-3 w-3" />}
                      onClick={() =>
                        setAiDraftTarget({
                          cvId: editingCV.id,
                          section: "PROJECT",
                          sectionTitle: `Dự án: ${proj.name}`,
                          projectId: proj.id,
                          currentText: proj.description || "",
                        })
                      }
                    >
                      AI viết giúp
                    </Button>
                  </div>
                )}
                <div className="flex gap-2 mt-1">
                  {proj.repoUrl && <a href={proj.repoUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-primary underline">Repo</a>}
                  {proj.demoUrl && <a href={proj.demoUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-primary underline">Demo</a>}
                </div>
              </div>
            )) || <p className="text-xs text-muted">Chưa có dự án nào được chọn</p>}
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
            )) || <p className="text-xs text-muted">Chưa có chứng chỉ nào được chọn</p>}
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
            )) || <p className="text-xs text-muted">Chưa có kỹ năng nào được chọn</p>}
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
            )) || <p className="text-xs text-muted">Chưa có thực tập nào được chọn</p>}
          </div>
        );
      default:
        return <p className="text-xs text-muted">Loại section không được hỗ trợ</p>;
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
        <Button variant="outline" size="sm" onClick={() => refetchCVs()} leftIcon={<Loader2 className="h-4 w-4" />} className="gap-2">
          Thử lại
        </Button>
      </div>
    );
  }

  const publicBaseUrl = window.location.origin;

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

                    {cv.targetPosition && (
                      <div className="flex items-center gap-2 text-xs text-muted">
                        <Target className="h-3.5 w-3.5" />
                        <span className="truncate max-w-[200px]">{cv.targetPosition}</span>
                      </div>
                    )}

                    <div className="flex items-center gap-4 text-xs text-muted">
                      <span className="flex items-center gap-1">
                        <FileText className="h-3.5 w-3.5" />
                        {cv.sections?.length || 0} phần
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
                        <Button
                          variant="ghost"
                          size="sm"
                          className="flex-1 flex items-center justify-center gap-1"
                          onClick={() => cv.pdfUrl ? window.open(`/api/v1/files/minio/${cv.pdfUrl}`, "_blank") : handleGeneratePDF(cv.id)}
                          title={cv.pdfUrl ? "Tải PDF" : "Tạo PDF"}
                          isLoading={generatingPDF?.cvId === cv.id}
                        >
                          {cv.pdfUrl ? (
                            <>
                              <Download className="h-4 w-4" />
                              <span className="hidden sm:inline">PDF</span>
                            </>
                          ) : (
                            <>
                              <Loader2 className="h-4 w-4" />
                              <span className="hidden sm:inline">Tạo PDF</span>
                            </>
                          )}
                        </Button>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {cv.isPublic && (
                          <Button
                            variant="outline"
                            size="sm"
                            className="flex-1 flex items-center justify-center gap-1"
                            onClick={() => handleCopyPublicUrl(`${publicBaseUrl}/p/${cv.publicToken}`)}
                            title="Sao chép link công khai"
                          >
                            <ExternalLink className="h-4 w-4" />
                            <span className="hidden sm:inline">Chia sẻ</span>
                            {copiedPublicUrl === `${publicBaseUrl}/p/${cv.publicToken}` && (
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
                            Các phần
                          </span>
                          <Button
                            variant="outline"
                            size="sm"
                            leftIcon={<Plus className="h-3 w-3" />}
                            onClick={() => openSectionModal(undefined, cv)}
                            disabled={cv.sections.length >= 6}
                          >
                            Thêm phần
                          </Button>
                        </div>
                        <div className="space-y-2 max-h-60 overflow-y-auto">
                          {sortSections(cv.sections).map((section: CVSectionDto) => (
                            <div key={section.id} className="flex items-center justify-between p-2 bg-slate-50 rounded-lg text-xs">
                              <span className="flex items-center gap-1 truncate">
                                <Badge variant={section.included ? "default" : "outline"} size="sm" className="shrink-0">
                                  {getSectionLabel(section.sectionType)}
                                </Badge>
                                <span className="text-muted">#{section.sortOrder + 1}</span>
                                {!section.included && <span className="text-rose-500">(ẩn)</span>}
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

      {/* Editor View - 2 Column Layout */}
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
                <div className="grid sm:grid-cols-2 gap-4">
                  <Input
                    label="Tiêu đề CV *"
                    value={cvForm.title}
                    onChange={(e) => setCvForm({ ...cvForm, title: e.target.value })}
                    placeholder="Ví dụ: CV Nguyễn Văn A - Java Developer"
                    error={errors.title}
                    maxLength={200}
                  />
                  <Input
                    label="Vị trí mục tiêu"
                    value={cvForm.targetPosition}
                    onChange={(event) => setCvForm({ ...cvForm, targetPosition: event.target.value })}
                    placeholder="Ví dụ: Senior Java Developer"
                    error={errors.targetPosition}
                    maxLength={200}
                  />
                </div>
                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    id="isPublic"
                    checked={editingCV?.isPublic ?? cvForm.isPublic ?? false}
                    onChange={(e) => {
                      if (editingCV) {
                        handleTogglePublic(editingCV);
                      } else {
                        setCvForm(prev => ({ ...prev, isPublic: e.target.checked }));
                      }
                    }}
                    className="h-4 w-4 rounded border-border text-primary focus:ring-primary"
                  />
                  <label htmlFor="isPublic" className="text-sm text-foreground">
                    Hiển thị công khai
                  </label>
                </div>
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

          {/* 2 Column Layout: Section Manager + Live Preview */}
          <div className="grid lg:grid-cols-12 gap-6">
            {/* Left Column: Section Manager (8 cols on lg) */}
            <div className="lg:col-span-8 space-y-6">
              {/* Section Editor Modal */}
              {editingSection && (
                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <Settings className="h-5 w-5 text-primary" />
                      <span>{editingSection ? "Chỉnh sửa phần" : "Thêm phần mới"}</span>
                    </CardTitle>
                    <CardDescription>Chọn loại phần và các mục từ hồ sơ để đưa vào CV</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <form onSubmit={handleSectionSubmit} className="space-y-4">
                      <div className="grid sm:grid-cols-2 gap-4">
                        <div className="w-full space-y-1.5">
                          <label className="block text-sm font-medium text-foreground">Loại phần *</label>
                          <select
                            value={sectionForm.sectionType}
                            onChange={(event) => {
                              const type = event.target.value as SectionType;
                              setSectionForm(prev => ({
                                ...prev,
                                sectionType: type,
                                itemIds: [], // Reset item selection when changing section type
                              }));
                            }}
                            className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
                          >
                            {SECTION_TYPES.map(({ type, label }: { type: SectionType; label: string }) => (
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
                        <label className="block text-sm font-medium text-foreground">Bao gồm trong CV</label>
                        <div className="flex items-center gap-2">
                          <input
                            type="checkbox"
                            id="included"
                            checked={sectionForm.included}
                            onChange={(e) => setSectionForm(prev => ({ ...prev, included: e.target.checked }))}
                            className="h-4 w-4 rounded border-border text-primary focus:ring-primary"
                          />
                          <label htmlFor="included" className="text-sm text-foreground">
                            Hiển thị phần này trong CV (bỏ chọn để ẩn tạm thời)
                          </label>
                        </div>
                      </div>

                      {/* Item Selection */}
                      <div className="w-full space-y-1.5">
                        <label className="block text-sm font-medium text-foreground flex items-center justify-between">
                          Chọn mục từ hồ sơ
                          <span className="text-xs text-muted">
                            Dữ liệu lấy trực tiếp từ hồ sơ, cập nhật tự động khi xuất PDF
                          </span>
                        </label>
                        <div className="border border-border rounded-lg bg-surface max-h-64 overflow-y-auto p-2">
                          {(() => {
                            const availableItems = getAvailableItems(sectionForm.sectionType);
                            if (availableItems.length === 0) {
                              return (
                                <p className="text-sm text-muted text-center py-8">
                                  Chưa có dữ liệu cho phần này. Hãy cập nhật hồ sơ cá nhân trước.
                                </p>
                              );
                            }
                            return (
                              <div className="space-y-1">
                                {availableItems.map((item: any) => {
                                  const itemId = item.id || item.skillId;
                                  const displayName = getItemDisplayName(sectionForm.sectionType, item);
                                  const subtitle = getItemSubtitle(sectionForm.sectionType, item);
                                  const selected = isItemSelected(sectionForm.sectionType, itemId);
                                  return (
                                    <label
                                      key={itemId}
                                      className="flex items-center gap-3 p-2 hover:bg-slate-100 rounded cursor-pointer transition-colors"
                                    >
                                      <input
                                        type="checkbox"
                                        checked={selected}
                                        onChange={() => toggleItemSelection(sectionForm.sectionType, itemId)}
                                        className="h-4 w-4 rounded border-border text-primary focus:ring-primary"
                                      />
                                      <div className="flex-1 min-w-0">
                                        <p className="text-sm font-medium text-foreground truncate">{displayName}</p>
                                        {subtitle && <p className="text-xs text-muted truncate">{subtitle}</p>}
                                      </div>
                                      {sectionForm.sectionType === "PROJECTS" && editingCV && (
                                        <Button
                                          type="button"
                                          variant="outline"
                                          size="sm"
                                          className="shrink-0 gap-1"
                                          leftIcon={<Sparkles className="h-3.5 w-3.5 text-primary" />}
                                          onClick={(e) => {
                                            e.preventDefault();
                                            e.stopPropagation();
                                            setAiDraftTarget({
                                              cvId: editingCV.id,
                                              section: "PROJECT",
                                              sectionTitle: `Dự án: ${item.name || displayName}`,
                                              projectId: item.id,
                                              currentText: item.description || "",
                                            });
                                          }}
                                        >
                                          AI viết giúp
                                        </Button>
                                      )}
                                      {sectionForm.sectionType === "SUMMARY" && editingCV && (
                                        <Button
                                          type="button"
                                          variant="outline"
                                          size="sm"
                                          className="shrink-0 gap-1"
                                          leftIcon={<Sparkles className="h-3.5 w-3.5 text-primary" />}
                                          onClick={(e) => {
                                            e.preventDefault();
                                            e.stopPropagation();
                                            setAiDraftTarget({
                                              cvId: editingCV.id,
                                              section: "SUMMARY",
                                              sectionTitle: "Giới thiệu bản thân",
                                              currentText: item.summary || profileData?.data?.summary || "",
                                            });
                                          }}
                                        >
                                          AI viết giúp
                                        </Button>
                                      )}
                                      {sectionForm.sectionType === "SKILLS" && item.verificationStatus && (
                                        <Badge
                                          variant={item.verificationStatus === "Đã xác thực" ? "verified" : "outline"}
                                          size="sm"
                                          className="shrink-0"
                                        >
                                          {item.verificationStatus}
                                        </Badge>
                                      )}
                                    </label>
                                  );
                                })}
                              </div>
                            );
                          })()}
                        </div>
                      </div>

                      {sectionErrors.form && <p className="text-xs text-danger font-medium">{sectionErrors.form}</p>}
                      <div className="flex justify-end gap-3 pt-2">
                        <Button type="button" variant="outline" onClick={closeSectionModal}>
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

              {/* Available Sections Grid with Drag and Drop */}
              {!editingSection && editingCV && (
                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <FileText className="h-5 w-5 text-primary" />
                      <span>Quản lý các phần CV</span>
                    </CardTitle>
                    <CardDescription>Kéo thả để sắp xếp, click để chỉnh sửa từng phần</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <DndContext
                      sensors={sensors}
                      collisionDetection={closestCenter}
                      onDragEnd={(event: DragEndEvent) => {
                        const { active, over } = event;
                        if (over && active.id !== over.id) {
                          // Get current section order from editingCV
                          const currentSections = editingCV?.sections || [];
                          const sectionTypes = currentSections.map(s => s.sectionType);
                          const newOrder = arrayMove(sectionTypes, sectionTypes.indexOf(active.id as SectionType), sectionTypes.indexOf(over.id as SectionType));
                          reorderCVSectionsMutation.mutate({
                            cvId: editingCV!.id,
                            data: { sectionIds: newOrder.map((t) => editingCV!.sections.find(s => s.sectionType === t)?.id).filter(Boolean) as string[] }
                          });
                        }
                      }}
                    >
                      <SortableContext
                        items={editingCV.sections.map(s => s.sectionType)}
                        strategy={verticalListSortingStrategy}
                      >
                        <div className="space-y-3">
                          {SECTION_TYPES.map(({ type, label, icon, description }: { type: SectionType; label: string; icon: React.ReactNode; description: string }) => {
                            const section = editingCV?.sections?.find(s => s.sectionType === type);
                            const hasSection = !!section;
                            const isIncluded = section?.included ?? true;
                            const itemCount = section?.itemIds?.length || 0;
                            const availableItems = getAvailableItems(type).length;

                        return (
                          <div
                            key={type}
                            className={`flex items-center gap-4 p-4 rounded-lg border transition-all ${
                              hasSection ? "bg-slate-50 border-primary/20" : "bg-surface border-border hover:border-primary/50"
                            }`}
                          >
                            <div className="p-2 rounded-lg bg-primary/10 text-primary shrink-0">
                              {icon}
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center gap-2">
                                <span className="font-medium text-foreground">{label}</span>
                                {hasSection && (
                                  <Badge variant={isIncluded ? "default" : "outline"} size="sm">
                                    {isIncluded ? "Đã bật" : "Đã ẩn"}
                                  </Badge>
                                )}
                                {!hasSection && (
                                  <Badge variant="outline" size="sm">Chưa thêm</Badge>
                                )}
                              </div>
                              <p className="text-xs text-muted mt-0.5">{description}</p>
                              {hasSection && (
                                <p className="text-xs text-muted mt-1">
                                  {itemCount} / {availableItems} mục được chọn
                                </p>
                              )}
                            </div>
                            <div className="flex items-center gap-2 shrink-0">
                              {type === "SUMMARY" && editingCV && (
                                <Button
                                  type="button"
                                  variant="outline"
                                  size="sm"
                                  leftIcon={<Sparkles className="h-3.5 w-3.5 text-primary" />}
                                  onClick={() =>
                                    setAiDraftTarget({
                                      cvId: editingCV.id,
                                      section: "SUMMARY",
                                      sectionTitle: "Giới thiệu bản thân",
                                      currentText: profileData?.data?.summary || "",
                                    })
                                  }
                                >
                                  AI viết giúp
                                </Button>
                              )}
                              {hasSection ? (
                                <>
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => editingCV && openSectionModal(section!, editingCV)}
                                    className="gap-1"
                                  >
                                    <Edit2 className="h-3.5 w-3.5" />
                                    Chỉnh sửa
                                  </Button>
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    className="h-7 w-7 text-rose-500 hover:bg-rose-50"
                                    onClick={() => editingCV && handleDeleteSection(editingCV.id, type)}
                                    title="Xóa phần"
                                  >
                                    <Trash2 className="h-3.5 w-3.5" />
                                  </Button>
                                </>
                              ) : (
                                <Button
                                  variant="primary"
                                  size="sm"
                                  onClick={() => openSectionModal(undefined, editingCV || undefined)}
                                  disabled={editingCV?.sections && editingCV.sections.length >= 6}
                                  className="gap-1"
                                >
                                  <Plus className="h-3.5 w-3.5" />
                                  Thêm
                                </Button>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                    {editingCV?.sections && editingCV.sections.length >= 6 && (
                      <p className="text-xs text-muted text-center mt-4">CV tối đa 6 phần (mỗi loại một phần)</p>
                    )}
                  </CardContent>
                </Card>
              )}
            </div>

            {/* Right Column: Live Preview (4 cols on lg) */}
            <div className="lg:col-span-4">
              <Card className="sticky top-6 h-[calc(100vh-8rem)] flex flex-col">
                <CardHeader className="flex-shrink-0">
                  <div className="flex items-center justify-between">
                    <CardTitle className="flex items-center gap-2">
                      <Eye className="h-5 w-5 text-primary" />
                      <span>Xem trước</span>
                    </CardTitle>
                    <Badge variant={previewQueryLoading ? "default" : "verified"} size="sm" className="gap-1">
                      <Loader2 className={`h-3 w-3 ${previewQueryLoading ? "animate-spin" : ""}`} />
                      {previewQueryLoading ? "Đang tải..." : "Cập nhật tự động"}
                    </Badge>
                  </div>
                  <CardDescription className="text-xs">A4 preview trên nền xám - dữ liệu live từ hồ sơ</CardDescription>
                </CardHeader>
                <CardContent className="flex-1 overflow-auto p-0 bg-slate-100">
                  {/* A4 Preview Area */}
                  <div className="flex items-center justify-center min-h-[700px] p-4">
                    <div className="w-full max-w-[210mm] bg-white shadow-xl" style={{ aspectRatio: "210/297" }}>
                      <div className="p-6 h-full overflow-auto">
                        {/* Preview Header */}
                        <div className="mb-6 pb-4 border-b border-border">
                          <h1 className="text-2xl font-bold text-foreground mb-1">{editingCV?.title || cvForm.title || "CV mới"}</h1>
                          {(editingCV?.targetPosition || cvForm.targetPosition) && (
                            <p className="text-sm text-primary font-medium">{editingCV?.targetPosition || cvForm.targetPosition}</p>
                          )}
                        </div>

                        {/* Preview Sections */}
                        {previewData?.sections && previewData.sections.length > 0 ? (
                          <div className="space-y-5">
                            {previewData.sections
                              .filter(s => s.included)
                              .sort((a, b) => {
                                const orderA = SECTION_ORDER.indexOf(a.sectionType);
                                const orderB = SECTION_ORDER.indexOf(b.sectionType);
                                if (orderA !== orderB) return orderA - orderB;
                                return a.sortOrder - b.sortOrder;
                              })
                              .map((sectionPreview) => renderPreviewSection(sectionPreview))
                            }
                          </div>
                        ) : (
                          <div className="text-center py-12 text-muted">
                            <FileText className="h-12 w-12 mx-auto mb-3 text-slate-300" />
                            <p>Chưa có phần nào được bật</p>
                            <p className="text-xs mt-1">Thêm và bật các phần từ bên trái để xem preview</p>
                          </div>
                        )}

                        {/* QR Code in Preview */}
                        {editingCV?.isPublic && editingCV?.publicToken && (
                          <div className="mt-8 pt-4 border-t border-border text-center">
                            <div className="inline-flex flex-col items-center gap-2 p-3 bg-slate-50 rounded-lg border border-border">
                              <QRCodeSVG
                                value={`${publicBaseUrl}/p/${editingCV.publicToken}`}
                                size={80}
                                level="M"
                                includeMargin={true}
                              />
                              <p className="text-xs text-muted">Quét để xem CV công khai</p>
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
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
                  className="gap-2"
                >
                  Tải PDF
                </Button>
              )}
            </div>
          </div>

          <Card className="bg-white">
            <CardContent className="p-8 max-w-3xl mx-auto">
              <div className="space-y-6">
                {selectedCVForPreview.sections?.map((section: CVSectionDto) => (
                  <div key={section.id} className="space-y-2">
                    <div className="flex items-center gap-2 pb-2 border-b border-border">
                      <FileText className="h-5 w-5 text-primary" />
                      <h3 className="text-lg font-semibold text-foreground">{getSectionLabel(section.sectionType)}</h3>
                    </div>
                    <div className="pt-2">
                      <p className="text-sm text-muted">
                        {section.included ? `${section.itemIds?.length || 0} mục được chọn` : "Đã ẩn"}
                      </p>
                      <p className="text-xs text-muted">
                        Dữ liệu chi tiết sẽ hiển thị khi xuất PDF (đọc live từ hồ sơ)
                      </p>
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
                <div className="flex items-center justify-between flex-wrap gap-4">
                  <div>
                    <p className="font-medium text-foreground">Link công khai CV</p>
                    <p className="text-sm text-muted truncate max-w-md">
                      {publicBaseUrl}/p/{selectedCVForPreview.publicToken}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="primary"
                      size="sm"
                      leftIcon={<Copy className="h-4 w-4" />}
                      onClick={() => handleCopyPublicUrl(`${publicBaseUrl}/p/${selectedCVForPreview.publicToken}`)}
                      className="gap-2"
                    >
                      {copiedPublicUrl === `${publicBaseUrl}/p/${selectedCVForPreview.publicToken}` ? "Đã sao chép" : "Sao chép link"}
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      leftIcon={<QrCode className="h-4 w-4" />}
                      onClick={() => window.open(`${publicBaseUrl}/p/${selectedCVForPreview.publicToken}`, "_blank")}
                      className="gap-2"
                    >
                      Xem công khai
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      leftIcon={<Download className="h-4 w-4" />}
                      onClick={() => handleDownloadPublicPdf(selectedCVForPreview.publicToken)}
                      className="gap-2"
                    >
                      Tải PDF
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      )}

      {aiDraftTarget && (
        <AIDraftCompareModal
          open={!!aiDraftTarget}
          cvId={aiDraftTarget.cvId}
          section={aiDraftTarget.section}
          sectionTitle={aiDraftTarget.sectionTitle}
          projectId={aiDraftTarget.projectId}
          currentText={aiDraftTarget.currentText}
          onClose={() => setAiDraftTarget(null)}
          onAccepted={() => {
            queryClient.invalidateQueries({ queryKey: candidateKeys.profile.cvs.all() });
            queryClient.invalidateQueries({ queryKey: candidateKeys.profile.me(user?.id) });
            queryClient.invalidateQueries({ queryKey: candidateKeys.profile.portfolio(user?.id) });
          }}
        />
      )}
    </div>
  );
}