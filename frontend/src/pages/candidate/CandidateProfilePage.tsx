import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../../store/useAuthStore";
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from "../../components/common/Card";
import { cn } from "../../utils/cn";
import { Badge } from "../../components/common/Badge";
import { Button } from "../../components/common/Button";
import { Input } from "../../components/common/Input";
import { Modal } from "../../components/common/Modal";
import {
  Award,
  BookOpen,
  Briefcase,
  Sparkles,
  User,
  Mail,
  Phone,
  GraduationCap,
  FileText,
  Star,
  Plus,
  Edit2,
  Trash2,
  Save,
  X,
  Loader2,
  AlertCircle,
  Lock,
  Paperclip,
  CheckCircle2,
  Gift,
} from "lucide-react";
import { offersApi } from "../../api/endpoints/offers.api";
import {
  profileApi,
  type CandidateProfile,
  type EducationItem,
  type CertificateItem,
  type PortfolioProject,
  type CreatePortfolioProjectRequest,
  type UpdatePortfolioProjectRequest,
} from "../../api/endpoints/profile.api";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { VisibilitySection } from "../../features/profile/components/VisibilitySection";
// BỔ SUNG: Import InternshipBlock
import { InternshipBlock } from "../../features/profile/components/InternshipBlock";

const VN_PHONE_REGEX = /^(03|05|07|08|09)[0-9]{8}$/;
const MAX_UPLOAD_BYTES = 5 * 1024 * 1024;

function formatDate(dateStr?: string | null): string {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  if (Number.isNaN(d.getTime())) return dateStr;
  return d.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
}

function emptyEducation(): EducationItem {
  return { school: "", major: "", fromDate: "", toDate: "", gpa: undefined, note: "" };
}

function emptyCertificate(): CertificateItem {
  return { name: "", issuer: "", issuedAt: "", expiresAt: "", fileUrl: "", fileType: "" };
}

// MediaDropzone component for drag & drop image upload
interface MediaDropzoneProps {
  projectId: string;
  isIOC: boolean;
  isUploading: boolean;
  currentMediaCount: number;
  onUpload: (projectId: string, file: File) => void;
}

function MediaDropzone({
  projectId,
  isIOC,
  isUploading,
  currentMediaCount,
  onUpload,
}: MediaDropzoneProps) {
  const [isDragOver, setIsDragOver] = useState(false);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) onUpload(projectId, file);
  };

  const handleClick = () => {
    if (isUploading) return;
    const input = document.createElement("input");
    input.type = "file";
    input.accept = "image/jpeg,image/png";
    input.onchange = (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (file) onUpload(projectId, file);
    };
    input.click();
  };

  if (isIOC) {
    return (
      <div className="aspect-square rounded-xl border-2 border-dashed border-border flex items-center justify-center bg-slate-50/50 min-h-[160px]">
        <div className="text-center text-muted px-4">
          <Image className="h-8 w-8 mx-auto mb-2 opacity-50" />
          <p className="text-sm">Dự án IOC không hỗ trợ tải ảnh</p>
        </div>
      </div>
    );
  }

  return (
    <div
      className={cn(
        "aspect-square rounded-xl border-2 border-dashed flex items-center justify-center bg-slate-50/50 min-h-[160px] cursor-pointer transition-all",
        isDragOver && "border-primary bg-primary/5",
        !isDragOver && "border-border hover:border-primary/50 hover:bg-primary/5"
      )}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      onClick={handleClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          handleClick();
        }
      }}
    >
      <div className="text-center px-4 py-8">
        <div className={cn(
          "mx-auto mb-3 transition-transform",
          isDragOver && "scale-110 text-primary",
          !isDragOver && "text-muted"
        )}>
          <Upload className="h-10 w-10" />
        </div>
        <p className="text-sm font-medium text-foreground whitespace-nowrap">
          Kéo thả hoặc click để tải ảnh
        </p>
        <p className="text-xs text-muted mt-1">JPG, PNG — tối đa 5MB</p>
        {currentMediaCount > 0 && (
          <p className="text-xs text-muted mt-1">Đã có {currentMediaCount}/6 ảnh</p>
        )}
      </div>
    </div>
  );
}

export function CandidateProfilePage() {
  const { user, isCandidate } = useAuthStore();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!isCandidate()) {
      navigate("/unauthorized");
    }
  }, [isCandidate, navigate]);

  const [activeTab, setActiveTab] = useState<"info" | "education" | "certificate" | "skills" | "portfolio">("info");

  const [isEducationModalOpen, setIsEducationModalOpen] = useState(false);
  const [editingEducation, setEditingEducation] = useState<EducationItem | null>(null);
  const [isCertificateModalOpen, setIsCertificateModalOpen] = useState(false);
  const [editingCertificate, setEditingCertificate] = useState<CertificateItem | null>(null);
  const [isSkillModalOpen, setIsSkillModalOpen] = useState(false);
  const [editingSkill, setEditingSkill] = useState<{ skillId: string; skillName: string; selfScore: number | null } | null>(null);

  const [profileForm, setProfileForm] = useState({
    phone: "",
    dob: "",
    gender: "",
    address: "",
    avatarUrl: "",
    headline: "",
    summary: "",
  });
  const [educationForm, setEducationForm] = useState<EducationItem>(emptyEducation);
  const [certificateForm, setCertificateForm] = useState<CertificateItem>(emptyCertificate);
  const [skillScore, setSkillScore] = useState(0);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [portfolioErrors, setPortfolioErrors] = useState<Record<string, string>>({});
  const [uploading, setUploading] = useState(false);

  const { data: profileData, isLoading: profileLoading, error: profileError } = useQuery({
    queryKey: ["myProfile", user?.id],
    queryFn: () => profileApi.getMyProfile(),
    enabled: Boolean(user?.id),
    staleTime: 5 * 60 * 1000,
  });

  const { data: educationsData, isLoading: educationsLoading } = useQuery({
    queryKey: ["myEducations", user?.id],
    queryFn: () => profileApi.getEducations(),
    enabled: Boolean(user?.id),
    staleTime: 5 * 60 * 1000,
  });

  const { data: certificatesData, isLoading: certificatesLoading } = useQuery({
    queryKey: ["myCertificates", user?.id],
    queryFn: () => profileApi.getCertificates(),
    enabled: Boolean(user?.id),
    staleTime: 5 * 60 * 1000,
  });

  const { data: skillsData, isLoading: skillsLoading } = useQuery({
    queryKey: ["mySkills", user?.id],
    queryFn: () => profileApi.getMySkills(),
    staleTime: 5 * 60 * 1000,
  });

  const {
    data: portfolioData,
    isLoading: portfolioLoading,
    isError: portfolioError,
    refetch: refetchPortfolio,
  } = useQuery({
    queryKey: ["myPortfolio", user?.id],
    queryFn: () => profileApi.getMyPortfolio(),
    enabled: Boolean(user?.id),
    staleTime: 5 * 60 * 1000,
    retry: false,
  });

  const { data: employmentRecordsData } = useQuery({
    queryKey: ["myEmploymentRecords", user?.id],
    queryFn: () => offersApi.listMyEmploymentRecords(),
    staleTime: 60 * 1000,
  });
  const employmentRecords = employmentRecordsData?.data ?? [];

  useEffect(() => {
    const p = profileData?.data;
    if (p) {
      setProfileForm((prev) => {
        if (
          prev.phone === (p.phone ?? "") &&
          prev.dob === (p.dob ?? "") &&
          prev.gender === (p.gender ?? "") &&
          prev.address === (p.address ?? "") &&
          prev.avatarUrl === (p.avatarUrl ?? "") &&
          prev.headline === (p.headline ?? "") &&
          prev.summary === (p.summary ?? "")
        ) {
          return prev;
        }
        return {
          phone: p.phone ?? "",
          dob: p.dob ?? "",
          gender: p.gender ?? "",
          address: p.address ?? "",
          avatarUrl: p.avatarUrl ?? "",
          headline: p.headline ?? "",
          summary: p.summary ?? "",
        };
      });
    }
  }, [profileData]);

  // ========== Mutations ==========

  const updateProfileMutation = useMutation({
    mutationFn: (data: Partial<CandidateProfile>) => profileApi.updateMyProfile(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myProfile"] });
    },
  });

  const createEducationMutation = useMutation({
    mutationFn: (data: EducationItem) => profileApi.createEducation(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myEducations"] });
      closeEducationModal();
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setErrors({ form: error?.response?.data?.message || "Thêm học vấn thất bại" });
    },
  });

  const updateEducationMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: EducationItem }) =>
        profileApi.updateEducation(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myEducations"] });
      closeEducationModal();
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setErrors({ form: error?.response?.data?.message || "Cập nhật học vấn thất bại" });
    },
  });

  const deleteEducationMutation = useMutation({
    mutationFn: (id: string) => profileApi.deleteEducation(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["myEducations"] }),
  });

  const createCertificateMutation = useMutation({
    mutationFn: (data: CertificateItem) => profileApi.createCertificate(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myCertificates"] });
      closeCertificateModal();
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setErrors({ form: error?.response?.data?.message || "Thêm chứng chỉ thất bại" });
    },
  });

  const updateCertificateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: CertificateItem }) =>
        profileApi.updateCertificate(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myCertificates"] });
      closeCertificateModal();
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setErrors({ form: error?.response?.data?.message || "Cập nhật chứng chỉ thất bại" });
    },
  });

  const deleteCertificateMutation = useMutation({
    mutationFn: (id: string) => profileApi.deleteCertificate(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["myCertificates"] }),
  });

  const upsertSelfSkillsMutation = useMutation({
    mutationFn: (skills: { skillId: string; selfScore: number }[]) =>
        profileApi.upsertSelfSkills(skills),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["mySkills"] });
      setIsSkillModalOpen(false);
      setEditingSkill(null);
    },
  });

  // Portfolio mutations
  const createProjectMutation = useMutation({
    mutationFn: (data: CreatePortfolioProjectRequest) => profileApi.createPortfolioProject(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myPortfolio"] });
      closeProjectModal();
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setErrors({ form: error?.response?.data?.message || "Tạo dự án portfolio thất bại" });
    },
  });

  const updateProjectMutation = useMutation({
    mutationFn: ({ projectId, data }: { projectId: string; data: UpdatePortfolioProjectRequest }) =>
        profileApi.updatePortfolioProject(projectId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myPortfolio"] });
      closeProjectModal();
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setErrors({ form: error?.response?.data?.message || "Cập nhật dự án portfolio thất bại" });
    },
  });

  const deleteProjectMutation = useMutation({
    mutationFn: (projectId: string) => profileApi.deletePortfolioProject(projectId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["myPortfolio"] }),
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setErrors({ form: error?.response?.data?.message || "Xóa dự án portfolio thất bại" });
    },
  });

  const uploadMediaMutation = useMutation({
    mutationFn: ({ projectId, file }: { projectId: string; file: File }) => profileApi.uploadPortfolioMedia(projectId, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myPortfolio"] });
      setUploadingMedia(null);
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      setErrors({ form: error?.response?.data?.message || "Tải ảnh lên thất bại" });
      setUploadingMedia(null);
    },
  });

  const deleteMediaMutation = useMutation({
    mutationFn: ({ projectId, mediaId }: { projectId: string; mediaId: string }) =>
        profileApi.deletePortfolioMedia(projectId, mediaId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["myPortfolio"] }),
  });

  // ========== Handlers ==========

  const handleProfileSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const nextErrors: Record<string, string> = {};
    if (profileForm.phone && !VN_PHONE_REGEX.test(profileForm.phone)) {
      nextErrors.phone = "Số điện thoại không hợp lệ (VD: 0987654321)";
    }
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    updateProfileMutation.mutate({
      phone: profileForm.phone,
      dob: profileForm.dob || undefined,
      gender: profileForm.gender || undefined,
      address: profileForm.address,
      avatarUrl: profileForm.avatarUrl,
      headline: profileForm.headline,
      summary: profileForm.summary,
    });
  };

  const closeEducationModal = () => {
    setIsEducationModalOpen(false);
    setEditingEducation(null);
    setEducationForm(emptyEducation());
    setErrors({});
  };

  const closeCertificateModal = () => {
    setIsCertificateModalOpen(false);
    setEditingCertificate(null);
    setCertificateForm(emptyCertificate());
    setErrors({});
  };

  const openEducationModal = (edu?: EducationItem) => {
    setErrors({});
    if (edu) {
      setEditingEducation(edu);
      setEducationForm({
        school: edu.school,
        major: edu.major,
        fromDate: edu.fromDate,
        toDate: edu.toDate ?? "",
        gpa: edu.gpa,
        note: edu.note ?? "",
      });
    } else {
      setEditingEducation(null);
      setEducationForm(emptyEducation());
    }
    setIsEducationModalOpen(true);
  };

  const openCertificateModal = (cert?: CertificateItem) => {
    setErrors({});
    if (cert) {
      setEditingCertificate(cert);
      setCertificateForm({
        name: cert.name,
        issuer: cert.issuer,
        issuedAt: cert.issuedAt,
        expiresAt: cert.expiresAt ?? "",
        fileUrl: cert.fileUrl ?? "",
        fileType: cert.fileType ?? "",
      });
    } else {
      setEditingCertificate(null);
      setCertificateForm(emptyCertificate());
    }
    setIsCertificateModalOpen(true);
  };

  const handleEducationSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const nextErrors: Record<string, string> = {};
    if (!educationForm.school.trim()) nextErrors.school = "Trường học không được để trống";
    if (!educationForm.major.trim()) nextErrors.major = "Chuyên ngành không được để trống";
    if (!educationForm.fromDate) nextErrors.fromDate = "Ngày bắt đầu không được để trống";
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    const payload: EducationItem = {
      ...educationForm,
      toDate: educationForm.toDate || undefined,
      note: educationForm.note || "",
    };

    if (editingEducation?.id) {
      updateEducationMutation.mutate({ id: editingEducation.id, data: payload });
    } else {
      createEducationMutation.mutate(payload);
    }
  };

  const handleCertificateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const nextErrors: Record<string, string> = {};
    if (!certificateForm.name.trim()) nextErrors.name = "Tên chứng chỉ không được để trống";
    if (!certificateForm.issuer.trim()) nextErrors.issuer = "Tổ chức cấp không được để trống";
    if (!certificateForm.issuedAt) nextErrors.issuedAt = "Ngày cấp không được để trống";
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    const payload: CertificateItem = {
      ...certificateForm,
      expiresAt: certificateForm.expiresAt || undefined,
      fileUrl: certificateForm.fileUrl || undefined,
      fileType: certificateForm.fileType || undefined,
    };

    if (editingCertificate?.id) {
      updateCertificateMutation.mutate({ id: editingCertificate.id, data: payload });
    } else {
      createCertificateMutation.mutate(payload);
    }
  };

  const handleCertificateFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > MAX_UPLOAD_BYTES) {
      setErrors({ file: "Tệp vượt quá dung lượng cho phép (tối đa 5MB)" });
      return;
    }

    setErrors({});
    setUploading(true);
    try {
      const res = await profileApi.uploadFile(file);
      setCertificateForm((prev) => ({
        ...prev,
        fileUrl: res.data.url,
        fileType: file.name.split(".").pop()?.toUpperCase() ?? "",
      }));
    } catch {
      setErrors({ file: "Tải tệp thất bại. Chỉ chấp nhận PDF, JPG, PNG" });
    } finally {
      setUploading(false);
      e.target.value = "";
    }
  };

  const handleDeleteEducation = (id?: string) => {
    if (id && confirm("Bạn có chắc chắn muốn xóa học vấn này?")) {
      deleteEducationMutation.mutate(id);
    }
  };

  const handleDeleteCertificate = (id?: string) => {
    if (id && confirm("Bạn có chắc chắn muốn xóa chứng chỉ này?")) {
      deleteCertificateMutation.mutate(id);
    }
  };

  const openSkillModal = (skill: { skillId: string; skillName: string; selfScore: number | null }) => {
    setEditingSkill(skill);
    setSkillScore(skill.selfScore ?? 0);
    setIsSkillModalOpen(true);
  };

  const handleSkillSubmit = () => {
    if (!editingSkill) return;
    upsertSelfSkillsMutation.mutate([
      { skillId: editingSkill.skillId, selfScore: skillScore },
    ]);
  };

  // Portfolio handlers
  function emptyProjectForm(): CreatePortfolioProjectRequest {
    return {
      name: "",
      role: "",
      description: "",
      techStack: "",
      repoUrl: "",
      demoUrl: "",
      startDate: "",
      endDate: "",
      visible: true,
      sortOrder: 0,
    };
  }

  const closeProjectModal = () => {
    setIsProjectModalOpen(false);
    setEditingProject(null);
    setProjectForm(emptyProjectForm());
    setErrors({});
  };

  const openProjectModal = (project?: PortfolioProject) => {
    setErrors({});
    if (project) {
      setEditingProject(project);
      setProjectForm({
        name: project.name,
        role: project.role ?? "",
        description: project.description ?? "",
        techStack: project.techStack ?? "",
        repoUrl: project.repoUrl ?? "",
        demoUrl: project.demoUrl ?? "",
        startDate: project.startDate ?? "",
        endDate: project.endDate ?? "",
        visible: project.visible,
        sortOrder: project.sortOrder,
      });
    } else {
      setEditingProject(null);
      setProjectForm(emptyProjectForm());
    }
    setIsProjectModalOpen(true);
  };

  const handleProjectSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const nextErrors: Record<string, string> = {};
    if (!projectForm.name.trim()) nextErrors.name = "Tên dự án không được để trống";

    // URL validation for repoUrl and demoUrl
    const urlRegex = /^https?:\/\/.+/;
    if (projectForm.repoUrl && !urlRegex.test(projectForm.repoUrl)) {
      nextErrors.repoUrl = "Repo URL phải bắt đầu bằng http:// hoặc https://";
    }
    if (projectForm.demoUrl && !urlRegex.test(projectForm.demoUrl)) {
      nextErrors.demoUrl = "Demo URL phải bắt đầu bằng http:// hoặc https://";
    }

    // Date validation: startDate <= endDate
    if (projectForm.startDate && projectForm.endDate) {
      const start = new Date(projectForm.startDate);
      const end = new Date(projectForm.endDate);
      if (start > end) {
        nextErrors.endDate = "Ngày kết thúc phải sau ngày bắt đầu";
      }
    }

    setPortfolioErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    const payload: CreatePortfolioProjectRequest = {
      ...projectForm,
      role: projectForm.role || undefined,
      description: projectForm.description || undefined,
      techStack: projectForm.techStack || undefined,
      repoUrl: projectForm.repoUrl || undefined,
      demoUrl: projectForm.demoUrl || undefined,
      startDate: projectForm.startDate || undefined,
      endDate: projectForm.endDate || undefined,
    };

    if (editingProject?.id) {
      updateProjectMutation.mutate({ projectId: editingProject.id, data: payload });
    } else {
      createProjectMutation.mutate(payload);
    }
  };

  const handleDeleteProject = (project: PortfolioProject) => {
    if (project.source === "IOC") {
      setPortfolioErrors({ form: "Không thể xóa dự án thực tập (IOC)" });
      return;
    }
    if (confirm("Bạn có chắc chắn muốn xóa dự án này?")) {
      deleteProjectMutation.mutate(project.id);
    }
  };

  const handleMediaUpload = (projectId: string, file: File) => {
    if (file.size > MAX_UPLOAD_BYTES) {
      setPortfolioErrors({ form: "Ảnh vượt quá dung lượng cho phép (tối đa 5MB)" });
      return;
    }
    const validTypes = ["image/jpeg", "image/png"];
    if (!validTypes.includes(file.type)) {
      setPortfolioErrors({ form: "Chỉ chấp nhận file JPG, PNG" });
      return;
    }
    setUploadingMedia({ projectId, file });
    uploadMediaMutation.mutate({ projectId, file });
  };

  const handleDeleteMedia = (projectId: string, mediaId: string) => {
    setDeleteMediaConfirm({ projectId, mediaId });
  };

  const confirmDeleteMedia = () => {
    if (deleteMediaConfirm) {
      deleteMediaMutation.mutate(deleteMediaConfirm);
      setDeleteMediaConfirm(null);
    }
  };

  // ========== Render ==========

  if (profileLoading) {
    return (
        <div className="flex items-center justify-center min-h-[400px]">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
    );
  }

  if (profileError) {
    return (
        <div className="text-center py-12">
          <AlertCircle className="h-12 w-12 text-danger mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-foreground">Không thể tải hồ sơ</h3>
          <p className="text-muted text-sm mt-1">Vui lòng thử lại sau hoặc liên hệ hỗ trợ</p>
        </div>
    );
  }

  const profile = profileData?.data;
  const educations = educationsData?.data ?? [];
  const certificates = certificatesData?.data ?? [];
  const skills = skillsData?.data ?? [];
  const portfolio = portfolioData?.data ?? [];

  const tabs = [
    { id: "info", label: "Thông tin", icon: User },
    { id: "education", label: "Học vấn", icon: GraduationCap },
    { id: "certificate", label: "Chứng chỉ", icon: FileText },
    { id: "skills", label: "Kỹ năng", icon: Star },
    { id: "portfolio", label: "Portfolio", icon: Briefcase },
  ] as const;

  return (
      <div className="space-y-6">
        {/* Top Banner */}
        <div className="rounded-2xl bg-gradient-to-r from-slate-900 to-slate-800 text-white p-6 sm:p-8 relative overflow-hidden shadow-sm">
          <div className="relative z-10 max-w-4xl space-y-3">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/30 border border-primary/40 text-red-300 text-xs font-semibold">
              <Sparkles className="h-3.5 w-3.5" />
              Hồ sơ năng lực xác thực
            </div>
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight">
              Xin chào, {profile?.fullName || user?.fullName || "Ứng viên"}!
            </h1>
            <p className="text-slate-300 text-xs sm:text-sm leading-relaxed">
              Hồ sơ kỹ năng của bạn được đồng bộ tự động từ điểm thi Khảo thí, kết quả học tập Rikkei LMS và nghiệm thu thực tập IOC.
            </p>
            <div className="pt-2 flex flex-wrap gap-2.5">
              <Button
                  variant="primary"
                  size="sm"
                  leftIcon={<Briefcase className="h-4 w-4" />}
                  onClick={() => navigate("/candidate/jobs")}
              >
                Xem việc làm phù hợp
              </Button>
              <Button
                  variant="outline"
                  size="sm"
                  className="bg-white/10 text-white border-white/20 hover:bg-white/20"
                  leftIcon={<BookOpen className="h-4 w-4" />}
                  onClick={() => navigate("/candidate/learning")}
              >
                Lộ trình học bổ sung
              </Button>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="border-b border-border">
          <nav className="flex gap-1 px-1" aria-label="Profile sections">
            {tabs.map(({ id, label, icon: Icon }) => (
                <button
                    key={id}
                    onClick={() => setActiveTab(id)}
                    className={`flex items-center gap-2 px-4 py-3 text-sm font-medium rounded-t-lg transition-all ${
                        activeTab === id
                            ? "bg-surface text-primary border-b-2 border-primary -mb-px z-10"
                            : "text-muted hover:text-foreground hover:bg-slate-50"
                    }`}
                    role="tab"
                    aria-selected={activeTab === id}
                >
                  <Icon className="h-4 w-4" />
                  {label}
                </button>
            ))}
          </nav>
        </div>

        <div className="max-w-4xl">
          {/* ===== Thông tin ===== */}
          {activeTab === "info" && (
              <form onSubmit={handleProfileSubmit} className="space-y-6">
                <Card>
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <CardTitle className="flex items-center gap-2">
                        <User className="h-5 w-5 text-primary" />
                        <span>Thông tin từ LMS (Tự động đồng bộ)</span>
                      </CardTitle>
                      <Badge variant="outline" size="sm">
                        <Lock className="h-3 w-3" />
                        Đã khóa
                      </Badge>
                    </div>
                    <CardDescription>
                      Các trường này được tự động điền từ Rikkei LMS khi bạn đăng nhập lần đầu và không thể chỉnh sửa thủ công.
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="grid sm:grid-cols-2 gap-4">
                    <Input label="Họ và tên" value={profile?.fullName ?? ""} disabled leftIcon={<User className="h-4 w-4" />} hint="Đồng bộ từ LMS" />
                    <Input label="Email" type="email" value={profile?.email ?? ""} disabled leftIcon={<Mail className="h-4 w-4" />} hint="Đồng bộ từ LMS" />
                    <Input label="Chương trình đào tạo" value={profile?.program ?? ""} disabled leftIcon={<BookOpen className="h-4 w-4" />} hint="Đồng bộ từ LMS" />
                    <Input label="Mã lớp" value={profile?.classCode ?? ""} disabled leftIcon={<GraduationCap className="h-4 w-4" />} hint="Đồng bộ từ LMS" />
                    <Input label="Tên khóa học" value={profile?.courseName ?? ""} disabled leftIcon={<BookOpen className="h-4 w-4" />} hint="Đồng bộ từ LMS" />
                  </CardContent>
                </Card>

                {/* BỔ SUNG: Khối Thực tập IOC */}
                <InternshipBlock userId={user?.id} />

                {/* [UT-07] Kinh nghiệm nhận việc từ Offer */}
                <Card>
                  <CardHeader>
                    <div className="flex items-center justify-between gap-2 flex-wrap">
                      <CardTitle className="flex items-center gap-2">
                        <Briefcase className="h-5 w-5 text-emerald-600" />
                        <span>Kinh nghiệm làm việc (Nhận việc qua Rikkei Jobs)</span>
                      </CardTitle>
                      <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          leftIcon={<Gift className="h-3.5 w-3.5" />}
                          onClick={() => navigate("/candidate/offers")}
                      >
                        Xem thư mời Offer
                      </Button>
                    </div>
                    <CardDescription>
                      Tự động ghi nhận khi bạn chấp nhận thư mời nhận việc (Offer) từ doanh nghiệp đối tác.
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    {employmentRecords.length === 0 ? (
                        <p className="text-sm text-muted">
                          Chưa có lịch sử nhận việc nào được ghi nhận từ Offer.
                        </p>
                    ) : (
                        <div className="space-y-3">
                          {employmentRecords.map((rec) => (
                              <div
                                  key={rec.id}
                                  className="rounded-xl border border-emerald-200 bg-emerald-50/50 p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                              >
                                <div className="space-y-1">
                                  <p className="text-sm font-bold text-foreground">
                                    Kinh nghiệm: {rec.position} tại {rec.companyName || "Doanh nghiệp tuyển dụng"}
                                  </p>
                                  <p className="text-xs text-muted">
                                    Ngày bắt đầu: {formatDate(rec.startDate)}
                                  </p>
                                </div>
                                {rec.confirmedAt ? (
                                    <Badge variant="verified" size="sm">
                                      <CheckCircle2 className="h-3 w-3" />
                                      Đã xác nhận đi làm ({formatDate(rec.confirmedAt)})
                                    </Badge>
                                ) : (
                                    <Badge variant="warning" size="sm">
                                      Chờ HR xác nhận đi làm
                                    </Badge>
                                )}
                              </div>
                          ))}
                        </div>
                    )}
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <Briefcase className="h-5 w-5 text-info" />
                      <span>Thông tin cá nhân (Có thể chỉnh sửa)</span>
                    </CardTitle>
                    <CardDescription>Cập nhật để hồ sơ của bạn thêm thu hút với doanh nghiệp</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="grid sm:grid-cols-2 gap-4">
                      <Input
                          label="Số điện thoại (VN)"
                          type="tel"
                          value={profileForm.phone}
                          onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                          placeholder="0987654321"
                          leftIcon={<Phone className="h-4 w-4" />}
                          hint="10 số, bắt đầu bằng 03/05/07/08/09"
                          error={errors.phone}
                      />
                      <Input
                          label="Ngày sinh"
                          type="date"
                          value={profileForm.dob}
                          onChange={(e) => setProfileForm({ ...profileForm, dob: e.target.value })}
                      />
                      <Input
                          label="Giới tính"
                          value={profileForm.gender}
                          onChange={(e) => setProfileForm({ ...profileForm, gender: e.target.value })}
                          placeholder="MALE / FEMALE / OTHER"
                      />
                      <Input
                          label="URL Avatar"
                          type="url"
                          value={profileForm.avatarUrl}
                          onChange={(e) => setProfileForm({ ...profileForm, avatarUrl: e.target.value })}
                          placeholder="https://example.com/avatar.jpg"
                      />
                    </div>
                    <Input
                        label="Địa chỉ"
                        value={profileForm.address}
                        onChange={(e) => setProfileForm({ ...profileForm, address: e.target.value })}
                        placeholder="Số nhà, đường, quận, thành phố"
                    />
                    <Input
                        label="Tiêu đề hồ sơ (Headline)"
                        value={profileForm.headline}
                        onChange={(e) => setProfileForm({ ...profileForm, headline: e.target.value })}
                        placeholder="Ví dụ: Java Backend Developer"
                        maxLength={500}
                    />
                    <div className="w-full space-y-1.5">
                      <label htmlFor="summary" className="block text-sm font-medium text-foreground">
                        Giới thiệu bản thân
                      </label>
                      <textarea
                          id="summary"
                          rows={4}
                          value={profileForm.summary}
                          onChange={(e) => setProfileForm({ ...profileForm, summary: e.target.value })}
                          placeholder="Mô tả ngắn về kinh nghiệm, thế mạnh và mục tiêu nghề nghiệp của bạn"
                          className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-muted outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
                      />
                    </div>
                  </CardContent>
                  <CardFooter className="justify-end">
                    <Button type="submit" variant="primary" isLoading={updateProfileMutation.isPending}>
                      <Save className="h-4 w-4" />
                      Lưu thay đổi
                    </Button>
                  </CardFooter>
                </Card>

                <VisibilitySection visibility={profile?.visibility} userId={profile?.userId} />
              </form>
          )}

          {/* ===== Học vấn ===== */}
          {activeTab === "education" && (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
                    <GraduationCap className="h-5 w-5 text-info" />
                    Học vấn
                  </h2>
                  <Button size="sm" variant="primary" onClick={() => openEducationModal()} leftIcon={<Plus className="h-4 w-4" />}>
                    Thêm học vấn
                  </Button>
                </div>

                {educationsLoading ? (
                    <div className="flex items-center justify-center py-12">
                      <Loader2 className="h-8 w-8 animate-spin text-primary" />
                    </div>
                ) : educations.length === 0 ? (
                    <Card>
                      <CardContent className="py-12 text-center">
                        <GraduationCap className="h-12 w-12 text-muted mx-auto mb-3 opacity-50" />
                        <h3 className="text-foreground font-medium">Chưa có học vấn nào</h3>
                        <p className="text-muted text-sm mt-1">Thêm học vấn đầu tiên để hoàn thiện hồ sơ</p>
                      </CardContent>
                    </Card>
                ) : (
                    <div className="space-y-3">
                      {educations.map((edu) => (
                          <Card key={edu.id} hoverable>
                            <CardContent className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 p-4">
                              <div className="flex-1 space-y-2">
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
                              <div className="flex items-center gap-2">
                                <Button variant="ghost" size="sm" onClick={() => openEducationModal(edu)} leftIcon={<Edit2 className="h-4 w-4" />}>
                                  Sửa
                                </Button>
                                <Button variant="danger" size="sm" onClick={() => handleDeleteEducation(edu.id)} leftIcon={<Trash2 className="h-4 w-4" />}>
                                  Xóa
                                </Button>
                              </div>
                            </CardContent>
                          </Card>
                      ))}
                    </div>
                )}
              </div>
          )}

          {/* ===== Chứng chỉ ===== */}
          {activeTab === "certificate" && (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
                    <FileText className="h-5 w-5 text-info" />
                    Chứng chỉ
                  </h2>
                  <Button size="sm" variant="primary" onClick={() => openCertificateModal()} leftIcon={<Plus className="h-4 w-4" />}>
                    Thêm chứng chỉ
                  </Button>
                </div>

                {certificatesLoading ? (
                    <div className="flex items-center justify-center py-12">
                      <Loader2 className="h-8 w-8 animate-spin text-primary" />
                    </div>
                ) : certificates.length === 0 ? (
                    <Card>
                      <CardContent className="py-12 text-center">
                        <FileText className="h-12 w-12 text-muted mx-auto mb-3 opacity-50" />
                        <h3 className="text-foreground font-medium">Chưa có chứng chỉ nào</h3>
                        <p className="text-muted text-sm mt-1">Thêm chứng chỉ đầu tiên để thể hiện thành tích</p>
                      </CardContent>
                    </Card>
                ) : (
                    <div className="space-y-3">
                      {certificates.map((cert) => (
                          <Card key={cert.id} hoverable>
                            <CardContent className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 p-4">
                              <div className="flex-1 space-y-2">
                                <div className="flex items-center gap-2 flex-wrap">
                                  <h4 className="font-medium text-foreground">{cert.name}</h4>
                                  <Badge variant="verified" size="sm" showIcon>{cert.issuer}</Badge>
                                </div>
                                <div className="flex items-center gap-4 text-xs text-muted flex-wrap">
                          <span className="flex items-center gap-1">
                            <FileText className="h-3.5 w-3.5" />
                            Ngày cấp: {formatDate(cert.issuedAt)}
                          </span>
                                  {cert.fileUrl && (
                                      <a
                                          href={cert.fileUrl}
                                          target="_blank"
                                          rel="noopener noreferrer"
                                          className="flex items-center gap-1 text-primary hover:underline"
                                      >
                                        <Paperclip className="h-3.5 w-3.5" />
                                        Tệp đính kèm
                                      </a>
                                  )}
                                </div>
                              </div>
                              <div className="flex items-center gap-2">
                                <Button variant="ghost" size="sm" onClick={() => openCertificateModal(cert)} leftIcon={<Edit2 className="h-4 w-4" />}>
                                  Sửa
                                </Button>
                                <Button variant="danger" size="sm" onClick={() => handleDeleteCertificate(cert.id)} leftIcon={<Trash2 className="h-4 w-4" />}>
                                  Xóa
                                </Button>
                              </div>
                            </CardContent>
                          </Card>
                      ))}
                    </div>
                )}
              </div>
          )}

          {/* ===== Kỹ năng ===== */}
          {activeTab === "skills" && (
              <div className="space-y-6">
                <h2 className="text-lg font-semibold text-foreground flex items-center gap-2">
                  <Star className="h-5 w-5 text-warning" />
                  Kỹ năng
                </h2>

                {skillsLoading ? (
                    <div className="flex items-center justify-center py-12">
                      <Loader2 className="h-8 w-8 animate-spin text-primary" />
                    </div>
                ) : skills.length === 0 ? (
                    <Card>
                      <CardContent className="py-12 text-center">
                        <Star className="h-12 w-12 text-muted mx-auto mb-3 opacity-50" />
                        <h3 className="text-foreground font-medium">Chưa có kỹ năng nào</h3>
                        <p className="text-muted text-sm mt-1">Kỹ năng được đồng bộ tự động từ LMS, Khảo thí và PM</p>
                      </CardContent>
                    </Card>
                ) : (
                    <>
                      <Card>
                        <CardHeader>
                          <CardTitle className="flex items-center gap-2">
                            <Award className="h-5 w-5 text-primary" />
                            <span>Kỹ năng đã xác thực</span>
                          </CardTitle>
                          <CardDescription>Được xác thực bởi LMS, Khảo thí và PM — không thể tự chỉnh sửa</CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-3">
                          {skills.filter((s) => s.verifiedScore !== null).map((skill) => (
                              <div key={skill.skillId} className="space-y-1.5">
                                <div className="flex justify-between text-xs font-medium">
                          <span className="flex items-center gap-1">
                            {skill.skillName}
                            <Badge variant="verified" size="sm" showIcon>Đã xác thực</Badge>
                          </span>
                                  <span className="text-primary font-bold">
                            {skill.verifiedScore}/100 ({skill.level})
                          </span>
                                </div>
                                <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                                  <div className="h-full bg-primary rounded-full" style={{ width: `${skill.verifiedScore}%` }} />
                                </div>
                                <div className="flex gap-2 flex-wrap">
                                  {skill.evidences?.map((e, idx) => (
                                      <Badge key={idx} variant={e.verified ? "verified" : "self"} size="sm">
                                        {e.source}: {e.score}
                                      </Badge>
                                  ))}
                                </div>
                              </div>
                          ))}
                          {skills.filter((s) => s.verifiedScore !== null).length === 0 && (
                              <p className="text-muted text-sm text-center py-4">Không có kỹ năng đã xác thực</p>
                          )}
                        </CardContent>
                      </Card>

                      <Card>
                        <CardHeader>
                          <CardTitle className="flex items-center gap-2">
                            <User className="h-5 w-5 text-info" />
                            <span>Kỹ năng tự đánh giá</span>
                          </CardTitle>
                          <CardDescription>
                            Điểm tự đánh giá được lưu vào skill_evidences với source=SELF
                          </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-3">
                          {skills.map((skill) => (
                              <div key={skill.skillId} className="space-y-1.5">
                                <div className="flex justify-between text-xs font-medium">
                          <span className="flex items-center gap-1">
                            {skill.skillName}
                            <Badge variant="self" size="sm" showIcon>Tự khai</Badge>
                          </span>
                                  <div className="flex items-center gap-2">
                            <span className="text-info font-bold">
                              {skill.selfScore !== null ? `${skill.selfScore}/100` : "Chưa đánh giá"}
                            </span>
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() =>
                                            openSkillModal({
                                              skillId: skill.skillId,
                                              skillName: skill.skillName,
                                              selfScore: skill.selfScore,
                                            })
                                        }
                                    >
                                      {skill.selfScore !== null ? "Cập nhật" : "Đánh giá"}
                                    </Button>
                                  </div>
                                </div>
                                {skill.selfScore !== null && (
                                    <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                                      <div className="h-full bg-info rounded-full" style={{ width: `${skill.selfScore}%` }} />
                                    </div>
                                )}
                              </div>
                          ))}
                        </CardContent>
                      </Card>
                    </>
                )}
              </div>
          )}

          {/* ===== Portfolio ===== */}
          {activeTab === "portfolio" && (
              <div className="space-y-8">
                {/* Header */}
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                  <div className="flex items-center gap-3">
                    <div className="p-3 rounded-xl bg-primary/10">
                      <Briefcase className="h-6 w-6 text-primary" />
                    </div>
                    <div>
                      <h2 className="text-xl font-bold text-foreground">Portfolio dự án</h2>
                      <p className="text-sm text-muted mt-0.5">
                        Trình bày các dự án tiêu biểu để nhà tuyển dụng đánh giá năng lực
                      </p>
                    </div>
                  </div>
                  <Button
                      size="sm"
                      variant="primary"
                      onClick={() => openProjectModal()}
                      leftIcon={<Plus className="h-4 w-4" />}
                      className="gap-2"
                  >
                    <span className="hidden sm:inline">Thêm dự án</span>
                    <span className="sm:hidden">Thêm</span>
                  </Button>
                </div>

                <div className="h-px bg-border my-4" />

                {/* Stats bar */}
                {portfolio.length > 0 && (
                    <div className="flex flex-wrap items-center gap-4 text-sm text-muted bg-slate-50 rounded-lg p-4">
                      <span className="flex items-center gap-1">
                        <Briefcase className="h-4 w-4" />
                        <strong className="text-foreground">{portfolio.length}</strong> dự án
                      </span>
                      <span className="flex items-center gap-1">
                        <Image className="h-4 w-4" />
                        <strong className="text-foreground">
                          {portfolio.reduce((acc, p) => acc + (p.media?.length || 0), 0)}
                        </strong> ảnh
                      </span>
                      <span className="flex items-center gap-1">
                        <Eye className="h-4 w-4" />
                        <strong className="text-foreground">
                          {portfolio.filter(p => p.visible).length}
                        </strong> công khai
                      </span>
                      <span className="flex items-center gap-1">
                        <Code2 className="h-4 w-4" />
                        <strong className="text-foreground">
                          {portfolio.filter(p => p.source === "IOC").length}
                        </strong> IOC
                      </span>
                    </div>
                )}

                {/* Loading State */}
                {portfolioLoading && (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                      {[1, 2, 3].map((i) => (
                          <Card key={i} className="animate-pulse">
                            <CardContent className="p-5 space-y-3">
                              <div className="h-4 bg-slate-200 rounded w-3/4" />
                              <div className="h-3 bg-slate-200 rounded w-1/2" />
                              <div className="h-3 bg-slate-200 rounded w-1/3" />
                              <div className="h-3 bg-slate-200 rounded w-2/3" />
                              <div className="h-3 bg-slate-200 rounded w-1/4" />
                              <div className="grid grid-cols-3 gap-2 mt-2">
                                <div className="aspect-square bg-slate-200 rounded-lg" />
                                <div className="aspect-square bg-slate-200 rounded-lg" />
                                <div className="aspect-square bg-slate-200 rounded-lg" />
                              </div>
                            </CardContent>
                          </Card>
                      ))}
                    </div>
                )}

                {/* Error State */}
                {!portfolioLoading && portfolioError && (
                    <div className="flex flex-col items-center justify-center py-12 px-4 border border-dashed border-amber-300 bg-amber-50/60 rounded-2xl text-center">
                      <div className="p-3 bg-amber-100 rounded-full mb-3 text-amber-600">
                        <AlertCircle className="h-6 w-6" />
                      </div>
                      <h3 className="font-semibold text-foreground text-base">
                        Chưa thể tải dữ liệu Portfolio
                      </h3>
                      <p className="text-sm text-muted mt-1 max-w-md">
                        Hệ thống đang gặp sự cố khi tải danh sách dự án. Vui lòng kiểm tra lại dịch vụ hoặc thử lại.
                      </p>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => void refetchPortfolio()}
                        className="mt-4 gap-2 border-amber-300 text-amber-800 hover:bg-amber-100"
                      >
                        <RefreshCw className="h-4 w-4" />
                        Thử lại
                      </Button>
                    </div>
                )}

                {/* Empty State */}
                {!portfolioLoading && !portfolioError && portfolio.length === 0 && (
                    <Card className="border-dashed border-primary/20 bg-primary/5">
                      <CardContent className="py-16 text-center">
                        <div className="mx-auto mb-4 p-4 rounded-full bg-primary/10 w-20 h-20 flex items-center justify-center">
                          <Briefcase className="h-10 w-10 text-primary" />
                        </div>
                        <h3 className="text-lg font-semibold text-foreground mb-2">Chưa có dự án nào</h3>
                        <p className="text-muted text-sm mb-6 max-w-sm mx-auto">
                          Hãy thêm dự án đầu tiên để thể hiện kỹ năng thực tế và kinh nghiệm của bạn
                        </p>
                        <Button
                            variant="primary"
                            onClick={() => openProjectModal()}
                            leftIcon={<Plus className="h-4 w-4" />}
                            size="lg"
                            className="gap-2"
                        >
                          Tạo dự án đầu tiên
                        </Button>
                      </CardContent>
                    </Card>
                )}

                {/* Project Grid */}
                {!portfolioLoading && portfolio.length > 0 && (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                      {portfolio.map((project, index) => (
                          <Card
                              key={project.id}
                              hoverable
                              className="h-full flex flex-col relative overflow-hidden transition-all duration-300 group"
                              style={{ animationDelay: `${index * 50}ms` }}
                          >
                            <CardContent className="p-5 flex flex-1 flex-col gap-3">
                              {/* Project Header */}
                              <div className="flex flex-col gap-2">
                                {/* Row 1: Title + Visibility badge */}
                                <div className="flex items-center justify-between gap-2">
                                  <div className="flex items-center gap-2 min-w-0 flex-1">
                                    <h4 className="font-semibold text-foreground truncate min-w-0 group-hover:text-primary transition-colors"
                                        title={project.name}>
                                      {project.name}
                                    </h4>
                                    {/* Visibility badge - always on same line as title */}
                                    {project.visible ? (
                                        <Badge variant="verified" size="sm" showIcon className="gap-1 whitespace-nowrap shrink-0">
                                          <Eye className="h-2.5 w-2.5" />
                                          Công khai
                                        </Badge>
                                    ) : (
                                        <Badge variant="outline" size="sm" showIcon className="gap-1 whitespace-nowrap shrink-0">
                                          <EyeOff className="h-2.5 w-2.5" />
                                          Riêng tư
                                        </Badge>
                                    )}
                                  </div>
                                </div>

                                {/* Row 2: Role badge (if exists) + Source badge + SourceRef + Actions */}
                                <div className="flex items-center justify-between gap-2">
                                  <div className="flex items-center gap-2 flex-wrap">
                                    {project.role && (
                                        <Badge variant="default" size="sm" className="whitespace-nowrap shrink-0">
                                          {project.role}
                                        </Badge>
                                    )}
                                    {/* Source Badge */}
                                    {project.source === "IOC" ? (
                                        <Badge variant="primary" size="sm" className="gap-1 shrink-0" title={`Source: IOC${project.sourceRef ? ` | Ref: ${project.sourceRef}` : ''}`}>
                                          <Code2 className="h-2.5 w-2.5" />
                                          IOC
                                        </Badge>
                                    ) : (
                                        <>
                                          <Badge variant="outline" size="sm" className="gap-1 shrink-0 bg-white/90 backdrop-blur" title={`Source: ${project.source || "MANUAL"}${project.sourceRef ? ` | Ref: ${project.sourceRef}` : ''}`}>
                                            <Code2 className="h-2.5 w-2.5" />
                                            {project.source || "MANUAL"}
                                          </Badge>
                                          {project.sourceRef && (
                                              <span className="text-[10px] text-muted font-mono px-1.5 py-0.5 rounded bg-white/90 backdrop-blur shadow-sm max-w-[110px] truncate shrink-0"
                                                    title={`Source Ref: ${project.sourceRef}`}>
                                                {project.sourceRef}
                                              </span>
                                          )}
                                        </>
                                    )}
                                  </div>
                                  {/* Actions - always visible */}
                                  {project.source !== "IOC" && (
                                      <div className="flex items-center gap-1 flex-shrink-0">
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            className="h-8 w-8"
                                            onClick={() => openProjectModal(project)}
                                            title="Chỉnh sửa"
                                        >
                                          <Edit2 className="h-4 w-4" />
                                        </Button>
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            className="h-8 w-8 text-rose-500 hover:bg-rose-50"
                                            onClick={() => handleDeleteProject(project)}
                                            title="Xóa"
                                        >
                                          <Trash2 className="h-4 w-4" />
                                        </Button>
                                      </div>
                                  )}
                                </div>
                              </div>

                              {/* Description - only render when exists */}
                              {project.description && (
                                  <p className="text-sm text-muted line-clamp-3 leading-relaxed">
                                    {project.description}
                                  </p>
                              )}

                              {/* Tech Stack - only render when exists */}
                              {project.techStack && (
                                  <div className="flex flex-wrap gap-2">
                                    {project.techStack.split(',').slice(0, 5).map((tech, i) => (
                                        <Badge key={i} variant="outline" size="sm" className="gap-1 text-xs">
                                          <Code2 className="h-2.5 w-2.5" />
                                          {tech.trim()}
                                        </Badge>
                                    ))}
                                    {project.techStack.split(',').length > 5 && (
                                        <Badge variant="outline" size="sm" className="text-xs">
                                          +{project.techStack.split(',').length - 5} nữa
                                        </Badge>
                                    )}
                                  </div>
                              )}

                              {/* Meta Info - divider only when there's description or techStack above */}
                              {(project.description || project.techStack) && (
                                  <div className="pt-2 border-t border-border" />
                              )}

                              {/* Meta Info */}
                              <div className="flex flex-wrap items-center gap-4 text-xs text-muted">
                                {project.startDate && (
                                    <span className="flex items-center gap-1">
                                      <Calendar className="h-3.5 w-3.5" />
                                      {formatDate(project.startDate)}
                                      {project.endDate ? ` - ${formatDate(project.endDate)}` : " - Hiện tại"}
                                    </span>
                                )}
                                {project.repoUrl && (
                                    <a
                                        href={project.repoUrl}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="flex items-center gap-1 text-primary hover:underline hover:text-primary/80 transition-colors"
                                        title="Xem repository"
                                    >
                                      <Link2 className="h-3.5 w-3.5" />
                                      Repo
                                    </a>
                                )}
                                {project.demoUrl && (
                                    <a
                                        href={project.demoUrl}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="flex items-center gap-1 text-primary hover:underline hover:text-primary/80 transition-colors"
                                        title="Xem demo"
                                    >
                                      <ExternalLink className="h-3.5 w-3.5" />
                                      Demo
                                    </a>
                                )}
                              </div>

                              {/* Media Gallery */}
                              <div className="pt-2">
                                <div className="flex items-center justify-between mb-2">
                                  <span className="text-xs font-medium text-muted flex items-center gap-1">
                                    <Image className="h-3.5 w-3.5" />
                                    Ảnh dự án
                                    <span className="text-foreground font-semibold">
                                      {project.media?.length || 0}/6
                                    </span>
                                  </span>
                                  {/* Only show "Tải ảnh" button in header when there ARE already images */}
                                  {(project.media?.length || 0) > 0 && project.source !== "IOC" && (
                                      <Button
                                          variant="outline"
                                          size="sm"
                                          leftIcon={<Upload className="h-3 w-3" />}
                                          disabled={(project.media?.length || 0) >= 6 || uploadingMedia?.projectId === project.id}
                                          onClick={() => {
                                            if ((project.media?.length || 0) >= 6 || uploadingMedia?.projectId === project.id) return;
                                            const input = document.createElement("input");
                                            input.type = "file";
                                            input.accept = "image/jpeg,image/png";
                                            input.onchange = (e) => {
                                              const file = (e.target as HTMLInputElement).files?.[0];
                                              if (file) handleMediaUpload(project.id, file);
                                            };
                                            input.click();
                                          }}
                                      >
                                        {uploadingMedia?.projectId === project.id ? (
                                            <> <Loader2 className="h-3 w-3 animate-spin" /> Đang tải... </> ) : ( 'Tải ảnh' )}
                                      </Button>
                                  )}
                                </div>

                                {(project.media?.length || 0) > 0 ? (
                                    <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
                                      {project.media.map((media) => (
                                          <div key={media.id} className="relative group aspect-square rounded-xl overflow-hidden bg-slate-50">
                                            <img
                                                src={media.url}
                                                alt={`Project media ${media.sortOrder + 1}`}
                                                className="w-full h-full object-cover transition-all duration-300 group-hover:scale-105 group-hover:brightness-110"
                                                loading="lazy"
                                            />
                                            <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent flex items-end justify-between p-2 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                                              <span className="text-xs bg-black/60 text-white px-1.5 py-0.5 rounded">
                                                #{media.sortOrder + 1}
                                              </span>
                                              <div className="flex gap-1">
                                                <Button
                                                    variant="ghost"
                                                    size="sm"
                                                    className="h-7 w-7 bg-white/90 backdrop-blur"
                                                    onClick={(e) => {
                                                      e.stopPropagation();
                                                      window.open(media.url, "_blank");
                                                    }}
                                                    title="Xem ảnh lớn"
                                                >
                                                  <Eye className="h-3.5 w-3.5" />
                                                </Button>
                                                {project.source !== "IOC" && (
                                                    <Button
                                                        variant="ghost"
                                                        size="sm"
                                                        className="h-7 w-7 bg-rose-500/90 text-white backdrop-blur hover:bg-rose-600"
                                                        onClick={(e) => {
                                                          e.stopPropagation();
                                                          handleDeleteMedia(project.id, media.id);
                                                        }}
                                                        title="Xóa ảnh"
                                                    >
                                                      <Trash2 className="h-3.5 w-3.5" />
                                                    </Button>
                                                )}
                                              </div>
                                            </div>
                                            {/* Sort order indicator */}
                                            <div className="absolute top-1 left-1">
                                              <span className="text-[10px] bg-black/50 text-white px-1 py-0.5 rounded">
                                                {media.sortOrder + 1}
                                              </span>
                                            </div>
                                          </div>
                                      ))}
                                      {uploadingMedia?.projectId === project.id && (
                                          <div className="relative aspect-square rounded-xl overflow-hidden bg-gradient-to-br from-primary/10 to-primary/5 animate-pulse flex items-center justify-center">
                                            <div className="text-center">
                                              <Loader2 className="h-8 w-8 text-primary mx-auto mb-1" />
                                              <span className="text-xs text-muted">Đang tải...</span>
                                            </div>
                                            <span className="absolute bottom-1 right-1 text-xs bg-black/60 text-white px-1.5 py-0.5 rounded">
                                              #{(project.media?.length || 0) + 1}
                                            </span>
                                          </div>
                                      )}
                                    </div>
                                ) : (
                                    <MediaDropzone
                                      projectId={project.id}
                                      isIOC={project.source === "IOC"}
                                      isUploading={uploadingMedia?.projectId === project.id}
                                      currentMediaCount={project.media?.length || 0}
                                      onUpload={handleMediaUpload}
                                    />
                                )}
                              </div>
                            </CardContent>
                          </Card>
                      ))}
                    </div>
                )}
              </div>
          )}

        </div>

        {/* ===== Education Modal ===== */}
        <Modal
            isOpen={isEducationModalOpen}
            onClose={closeEducationModal}
            title={editingEducation ? "Chỉnh sửa học vấn" : "Thêm học vấn mới"}
            size="md"
        >
          <form onSubmit={handleEducationSubmit} className="space-y-4">
            <Input
                label="Trường học *"
                value={educationForm.school}
                onChange={(e) => setEducationForm({ ...educationForm, school: e.target.value })}
                placeholder="Ví dụ: Đại học Bách Khoa Hà Nội"
                error={errors.school}
            />
            <Input
                label="Chuyên ngành *"
                value={educationForm.major}
                onChange={(e) => setEducationForm({ ...educationForm, major: e.target.value })}
                placeholder="Ví dụ: Kỹ thuật phần mềm"
                error={errors.major}
            />
            <div className="grid sm:grid-cols-2 gap-4">
              <Input
                  label="Ngày bắt đầu *"
                  type="date"
                  value={educationForm.fromDate}
                  onChange={(e) => setEducationForm({ ...educationForm, fromDate: e.target.value })}
                  error={errors.fromDate}
              />
              <Input
                  label="Ngày kết thúc"
                  type="date"
                  value={educationForm.toDate ?? ""}
                  onChange={(e) => setEducationForm({ ...educationForm, toDate: e.target.value })}
                  hint="Để trống nếu đang học"
              />
            </div>
            <Input
                label="GPA (thang 4.0)"
                type="number"
                step="0.01"
                min="0"
                max="4"
                value={educationForm.gpa?.toString() ?? ""}
                onChange={(e) =>
                    setEducationForm({
                      ...educationForm,
                      gpa: e.target.value ? parseFloat(e.target.value) : undefined,
                    })
                }
                placeholder="Ví dụ: 3.5"
            />
            <Input
                label="Ghi chú"
                value={educationForm.note ?? ""}
                onChange={(e) => setEducationForm({ ...educationForm, note: e.target.value })}
                placeholder="Thành tích, hoạt động nổi bật..."
            />
            {errors.form && <p className="text-xs text-danger font-medium">{errors.form}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <Button type="button" variant="outline" onClick={closeEducationModal}>
                <X className="h-4 w-4" />
                Hủy
              </Button>
              <Button
                  type="submit"
                  variant="primary"
                  isLoading={createEducationMutation.isPending || updateEducationMutation.isPending}
              >
                <Save className="h-4 w-4" />
                {editingEducation ? "Cập nhật" : "Thêm"}
              </Button>
            </div>
          </form>
        </Modal>

        {/* ===== Certificate Modal ===== */}
        <Modal
            isOpen={isCertificateModalOpen}
            onClose={closeCertificateModal}
            title={editingCertificate ? "Chỉnh sửa chứng chỉ" : "Thêm chứng chỉ mới"}
            size="md"
        >
          <form onSubmit={handleCertificateSubmit} className="space-y-4">
            <Input
                label="Tên chứng chỉ *"
                value={certificateForm.name}
                onChange={(e) => setCertificateForm({ ...certificateForm, name: e.target.value })}
                placeholder="Ví dụ: Oracle Certified Professional, Java SE 11"
                error={errors.name}
            />
            <Input
                label="Tổ chức cấp *"
                value={certificateForm.issuer}
                onChange={(e) => setCertificateForm({ ...certificateForm, issuer: e.target.value })}
                placeholder="Ví dụ: Oracle"
                error={errors.issuer}
            />
            <div className="grid sm:grid-cols-2 gap-4">
              <Input
                  label="Ngày cấp *"
                  type="date"
                  value={certificateForm.issuedAt}
                  onChange={(e) => setCertificateForm({ ...certificateForm, issuedAt: e.target.value })}
                  error={errors.issuedAt}
              />
              <Input
                  label="Ngày hết hạn"
                  type="date"
                  value={certificateForm.expiresAt ?? ""}
                  onChange={(e) => setCertificateForm({ ...certificateForm, expiresAt: e.target.value })}
              />
            </div>

            <div className="w-full space-y-1.5">
              <label htmlFor="certFile" className="block text-sm font-medium text-foreground">
                Tệp đính kèm
              </label>
              <div className="flex items-center gap-3">
                <input
                    id="certFile"
                    type="file"
                    accept=".pdf,.jpg,.jpeg,.png"
                    onChange={handleCertificateFileChange}
                    className="block w-full text-xs text-muted file:mr-3 file:rounded-lg file:border-0 file:bg-primary-light file:px-3 file:py-2 file:text-xs file:font-medium file:text-primary hover:file:bg-primary-100"
                />
                {uploading && <Loader2 className="h-4 w-4 animate-spin text-primary shrink-0" />}
              </div>
              <p className="text-xs text-muted">PDF, JPG hoặc PNG — tối đa 5MB</p>
              {certificateForm.fileUrl && (
                  <p className="text-xs text-success font-medium break-all">
                    Đã tải lên: {certificateForm.fileUrl}
                  </p>
              )}
              {errors.file && <p className="text-xs text-danger font-medium">{errors.file}</p>}
            </div>

            {errors.form && <p className="text-xs text-danger font-medium">{errors.form}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <Button type="button" variant="outline" onClick={closeCertificateModal}>
                <X className="h-4 w-4" />
                Hủy
              </Button>
              <Button
                  type="submit"
                  variant="primary"
                  isLoading={createCertificateMutation.isPending || updateCertificateMutation.isPending}
              >
                <Save className="h-4 w-4" />
                {editingCertificate ? "Cập nhật" : "Thêm"}
              </Button>
            </div>
          </form>
        </Modal>

        {/* ===== Self-assessment Modal ===== */}
        <Modal
            isOpen={isSkillModalOpen}
            onClose={() => {
              setIsSkillModalOpen(false);
              setEditingSkill(null);
            }}
            title="Tự đánh giá kỹ năng"
            size="sm"
        >
          <div className="space-y-4">
            <div className="space-y-2">
              <p className="text-sm text-foreground">
                Kỹ năng: <strong>{editingSkill?.skillName}</strong>
              </p>
              <p className="text-xs text-muted">
                Điểm này được lưu với source=SELF và không thay thế điểm đã xác thực.
              </p>
            </div>
            <div className="space-y-2">
              <label htmlFor="selfScore" className="block text-sm font-medium text-foreground">
                Điểm tự đánh giá: {skillScore}
              </label>
              <input
                  id="selfScore"
                  type="range"
                  min="0"
                  max="100"
                  step="1"
                  value={skillScore}
                  onChange={(e) => setSkillScore(parseInt(e.target.value, 10))}
                  className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-primary"
              />
              <div className="flex justify-between text-xs text-muted">
                <span>0 - Chưa biết</span>
                <span>50 - Cơ bản</span>
                <span>75 - Tốt</span>
                <span>100 - Xuất sắc</span>
              </div>
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <Button
                  variant="outline"
                  onClick={() => {
                    setIsSkillModalOpen(false);
                    setEditingSkill(null);
                  }}
              >
                <X className="h-4 w-4" />
                Hủy
              </Button>
              <Button variant="primary" onClick={handleSkillSubmit} isLoading={upsertSelfSkillsMutation.isPending}>
                <Save className="h-4 w-4" />
                Lưu đánh giá
              </Button>
            </div>
          </div>
        </Modal>

        {/* ===== Project Modal ===== */}
        <Modal
            isOpen={isProjectModalOpen}
            onClose={closeProjectModal}
            title={editingProject ? "Chỉnh sửa dự án portfolio" : "Thêm dự án portfolio mới"}
            size="lg"
        >
          <form onSubmit={handleProjectSubmit} className="space-y-4">
            <div className="grid sm:grid-cols-2 gap-4">
              <Input
                  label="Tên dự án *"
                  value={projectForm.name}
                  onChange={(e) => setProjectForm({ ...projectForm, name: e.target.value })}
                  placeholder="Ví dụ: Hệ thống quản lý tuyển dụng"
                  error={portfolioErrors.name}
              />
              <Input
                  label="Vai trò"
                  value={projectForm.role}
                  onChange={(e) => setProjectForm({ ...projectForm, role: e.target.value })}
                  placeholder="Ví dụ: Fullstack Developer"
              />
            </div>
            <div className="w-full space-y-1.5">
              <label htmlFor="description" className="block text-sm font-medium text-foreground">
                Mô tả dự án
              </label>
              <textarea
                  id="description"
                  rows={3}
                  value={projectForm.description}
                  onChange={(e) => setProjectForm({ ...projectForm, description: e.target.value })}
                  placeholder="Mô tả ngắn về dự án, công nghệ sử dụng, vai trò của bạn..."
                  className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-muted outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
              />
            </div>
            <Input
                label="Tech Stack"
                value={projectForm.techStack}
                onChange={(e) => setProjectForm({ ...projectForm, techStack: e.target.value })}
                placeholder="Ví dụ: React, Node.js, PostgreSQL, Docker"
            />
            <div className="grid sm:grid-cols-2 gap-4">
              <Input
                  label="Repo URL"
                  type="url"
                  value={projectForm.repoUrl}
                  onChange={(e) => setProjectForm({ ...projectForm, repoUrl: e.target.value })}
                  placeholder="https://github.com/username/repo"
              />
              <Input
                  label="Demo URL"
                  type="url"
                  value={projectForm.demoUrl}
                  onChange={(e) => setProjectForm({ ...projectForm, demoUrl: e.target.value })}
                  placeholder="https://demo.example.com"
              />
            </div>
            <div className="grid sm:grid-cols-2 gap-4">
              <Input
                  label="Ngày bắt đầu"
                  type="date"
                  value={projectForm.startDate}
                  onChange={(e) => setProjectForm({ ...projectForm, startDate: e.target.value })}
              />
              <Input
                  label="Ngày kết thúc"
                  type="date"
                  value={projectForm.endDate}
                  onChange={(e) => setProjectForm({ ...projectForm, endDate: e.target.value })}
                  hint="Để trống nếu đang thực hiện"
              />
            </div>
            <div className="flex items-center gap-2">
              <input
                  type="checkbox"
                  id="visible"
                  checked={projectForm.visible}
                  onChange={(e) => setProjectForm({ ...projectForm, visible: e.target.checked })}
                  className="h-4 w-4 rounded border-border text-primary focus:ring-primary"
              />
              <label htmlFor="visible" className="text-sm text-foreground">
                Hiển thị công khai trên hồ sơ
              </label>
            </div>
            {portfolioErrors.form && <p className="text-xs text-danger font-medium">{portfolioErrors.form}</p>}
            <div className="flex justify-end gap-3 pt-2">
              <Button type="button" variant="outline" onClick={closeProjectModal}>
                <X className="h-4 w-4" />
                Hủy
              </Button>
              <Button
                  type="submit"
                  variant="primary"
                  isLoading={createProjectMutation.isPending || updateProjectMutation.isPending}
              >
                <Save className="h-4 w-4" />
                {editingProject ? "Cập nhật" : "Thêm"}
              </Button>
            </div>
          </form>
        </Modal>

        {/* ===== Delete Media Confirmation Modal ===== */}
        <Modal
            isOpen={!!deleteMediaConfirm}
            onClose={() => setDeleteMediaConfirm(null)}
            title="Xác nhận xóa ảnh"
            size="sm"
        >
          <div className="space-y-4">
            <p className="text-sm text-foreground">
              Bạn có chắc chắn muốn xóa ảnh này? Hành động này không thể hoàn tác.
            </p>
            <div className="flex justify-end gap-3 pt-2">
              <Button variant="outline" onClick={() => setDeleteMediaConfirm(null)}>
                <X className="h-4 w-4" />
                Hủy
              </Button>
              <Button
                  variant="danger"
                  onClick={confirmDeleteMedia}
                  isLoading={deleteMediaMutation.isPending}
              >
                <Trash2 className="h-4 w-4" />
                Xóa
              </Button>
            </div>
          </div>
        </Modal>
      </div>
  );
}