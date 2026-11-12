import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../../store/useAuthStore";
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";
import { Button } from "../../components/common/Button";
import { Input } from "../../components/common/Input";
import { Modal } from "../../components/common/Modal";
import {
  Award,
  BookOpen,
  Briefcase,
  Sparkles,
  CheckCircle2,
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
} from "lucide-react";
import {
  profileApi,
  type CandidateProfile,
  type EducationItem,
  type CertificateItem,
} from "../../api/endpoints/profile.api";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";

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

export function CandidateProfilePage() {
  const { user, isCandidate } = useAuthStore();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!isCandidate()) {
      navigate("/unauthorized");
    }
  }, [isCandidate, navigate]);

  const [activeTab, setActiveTab] = useState<"info" | "education" | "certificate" | "skills">("info");

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
  const [uploading, setUploading] = useState(false);

  const { data: profileData, isLoading: profileLoading, error: profileError } = useQuery({
    queryKey: ["myProfile"],
    queryFn: () => profileApi.getMyProfile(),
    staleTime: 5 * 60 * 1000,
  });

  const { data: educationsData, isLoading: educationsLoading } = useQuery({
    queryKey: ["myEducations"],
    queryFn: () => profileApi.getEducations(),
    staleTime: 5 * 60 * 1000,
  });

  const { data: certificatesData, isLoading: certificatesLoading } = useQuery({
    queryKey: ["myCertificates"],
    queryFn: () => profileApi.getCertificates(),
    staleTime: 5 * 60 * 1000,
  });

  const { data: skillsData, isLoading: skillsLoading } = useQuery({
    queryKey: ["mySkills"],
    queryFn: () => profileApi.getMySkills(),
    staleTime: 5 * 60 * 1000,
  });

  useEffect(() => {
    const p = profileData?.data;
    if (p) {
      setProfileForm({
        phone: p.phone ?? "",
        dob: p.dob ?? "",
        gender: p.gender ?? "",
        address: p.address ?? "",
        avatarUrl: p.avatarUrl ?? "",
        headline: p.headline ?? "",
        summary: p.summary ?? "",
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

  const setVisibilityMutation = useMutation({
    mutationFn: (visibility: CandidateProfile["visibility"]) =>
      profileApi.updateMyProfile({ visibility }),
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
  const isPublic = profile?.visibility === "PUBLIC";

  const tabs = [
    { id: "info", label: "Thông tin", icon: User },
    { id: "education", label: "Học vấn", icon: GraduationCap },
    { id: "certificate", label: "Chứng chỉ", icon: FileText },
    { id: "skills", label: "Kỹ năng", icon: Star },
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
            <Button variant="primary" size="sm" leftIcon={<Briefcase className="h-4 w-4" />}>
              Xem việc làm phù hợp
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="bg-white/10 text-white border-white/20 hover:bg-white/20"
              leftIcon={<BookOpen className="h-4 w-4" />}
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

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <CheckCircle2 className="h-5 w-5 text-success" />
                  <span>Trạng thái công khai</span>
                </CardTitle>
                <CardDescription>Kiểm soát quyền hiển thị hồ sơ với doanh nghiệp</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="p-3 bg-slate-50 rounded-lg border border-border-subtle text-xs text-muted leading-relaxed">
                  Hồ sơ đang ở trạng thái{" "}
                  <strong className="text-foreground">{isPublic ? "Công khai" : "Riêng tư"}</strong>.{" "}
                  {isPublic
                    ? "Doanh nghiệp có thể tìm thấy hồ sơ của bạn nhưng email và số điện thoại luôn được che giấu an toàn."
                    : "Doanh nghiệp không thể tìm thấy hồ sơ của bạn trong tìm kiếm."}
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  className={`w-full ${isPublic ? "text-danger border-rose-200 hover:bg-rose-50" : "text-primary border-primary/20 hover:bg-primary/5"}`}
                  isLoading={setVisibilityMutation.isPending}
                  onClick={() => setVisibilityMutation.mutate(isPublic ? "PRIVATE" : "PUBLIC")}
                >
                  {isPublic ? "Rút công khai hồ sơ" : "Công khai hồ sơ"}
                </Button>
              </CardContent>
            </Card>
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
    </div>
  );
}