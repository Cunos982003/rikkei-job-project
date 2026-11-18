import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useQuery } from "@tanstack/react-query";
import {
  ArrowLeft,
  Save,
  Send,
  AlertTriangle,
  AlertCircle,
  CheckCircle2,
  Lock,
  Layers,
  FileText,
} from "lucide-react";
import { toast } from "sonner";
import {
  hrJobsApi,
  type JobResponse,
  type CreateJobDto,
} from "../../api/endpoints/hrJobs.api";
import { Button } from "../../components/common/Button";
import { Input } from "../../components/common/Input";
import { Card, CardHeader, CardTitle, CardContent } from "../../components/common/Card";
import { Modal } from "../../components/common/Modal";
import { JobSkillPicker, type SelectedSkillItem, toCanonicalSkillCode } from "../../components/hr/JobSkillPicker";
import { JobStatusBadge } from "../../components/hr/JobStatusBadge";
import { cn } from "../../utils/cn";

// Schema xác thực Zod cho Bước 1
const jobFormSchema = z
  .object({
    title: z
      .string()
      .trim()
      .min(1, "Tiêu đề không được để trống")
      .max(255, "Tiêu đề không được vượt quá 255 ký tự"),
    position: z
      .string()
      .trim()
      .min(1, "Vị trí không được để trống")
      .max(255, "Vị trí không được vượt quá 255 ký tự"),
    level: z.enum(["INTERN", "FRESHER", "JUNIOR", "MIDDLE", "SENIOR"] as const, {
      message: "Vui lòng chọn cấp bậc",
    }),
    type: z.enum(["FULLTIME", "PARTTIME", "INTERNSHIP"] as const, {
      message: "Vui lòng chọn hình thức làm việc",
    }),
    quantity: z
      .number({ message: "Số lượng tuyển phải là số" })
      .int("Số lượng phải là số nguyên")
      .min(1, "Số lượng tuyển phải lớn hơn hoặc bằng 1"),
    city: z.string().optional(),
    address: z.string().optional(),
    workMode: z.enum(["ONSITE", "REMOTE", "HYBRID"] as const, {
      message: "Vui lòng chọn chế độ làm việc",
    }),
    salaryMin: z.number().optional().nullable(),
    salaryMax: z.number().optional().nullable(),
    deadline: z
      .string()
      .min(1, "Hạn nộp hồ sơ không được để trống")
      .refine((val) => {
        if (!val) return false;
        const today = new Date().toISOString().split("T")[0];
        return val >= today;
      }, "Hạn nộp hồ sơ phải từ hôm nay trở đi"),
    description: z.string().optional(),
    requirements: z.string().optional(),
    benefits: z.string().optional(),
  })
  .refine(
    (data) => {
      if (
        data.salaryMin != null &&
        data.salaryMax != null &&
        !isNaN(data.salaryMin) &&
        !isNaN(data.salaryMax) &&
        data.salaryMin > 0 &&
        data.salaryMax > 0
      ) {
        return data.salaryMin <= data.salaryMax;
      }
      return true;
    },
    {
      message: "Lương tối thiểu không được lớn hơn lương tối đa",
      path: ["salaryMin"],
    }
  );

type JobFormValues = z.infer<typeof jobFormSchema>;

function getDefaultDeadline(): string {
  const d = new Date();
  d.setDate(d.getDate() + 30);
  return d.toISOString().split("T")[0];
}

export function HrJobFormPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditing = Boolean(id);

  const [activeStep, setActiveStep] = useState<1 | 2>(1);
  const [createdJobId, setCreatedJobId] = useState<string | null>(null);
  const [selectedSkills, setSelectedSkills] = useState<SelectedSkillItem[]>([]);
  const [isSaving, setIsSaving] = useState(false);
  const [isApprovedConfirmOpen, setIsApprovedConfirmOpen] = useState(false);

  // Form setup
  const {
    register,
    reset,
    getValues,
    trigger,
    formState: { errors },
  } = useForm<JobFormValues>({
    resolver: zodResolver(jobFormSchema),
    defaultValues: {
      title: "",
      position: "",
      level: "JUNIOR",
      type: "FULLTIME",
      quantity: 1,
      city: "Hà Nội",
      address: "",
      workMode: "ONSITE",
      salaryMin: undefined,
      salaryMax: undefined,
      deadline: getDefaultDeadline(),
      description: "",
      requirements: "",
      benefits: "",
    },
  });

  // Tải thông tin JD khi sửa
  const {
    data: jobData,
    isLoading: isJobLoading,
    refetch,
  } = useQuery({
    queryKey: ["hr", "job", id],
    queryFn: async () => {
      if (!id) return null;
      const res = await hrJobsApi.getJob(id);
      return res.data;
    },
    enabled: isEditing,
  });

  const job: JobResponse | null = jobData ?? null;

  // Điền dữ liệu vào form khi đã tải xong
  useEffect(() => {
    if (job) {
      reset({
        title: job.title || "",
        position: job.position || "",
        level: job.level || "JUNIOR",
        type: job.type || "FULLTIME",
        quantity: job.quantity || 1,
        city: job.city || "",
        address: job.address || "",
        workMode: job.workMode || "ONSITE",
        salaryMin: job.salaryMin != null ? Number(job.salaryMin) : undefined,
        salaryMax: job.salaryMax != null ? Number(job.salaryMax) : undefined,
        deadline: job.deadline || "",
        description: job.description || "",
        requirements: job.requirements || "",
        benefits: job.benefits || "",
      });

      if (job.skills) {
        setSelectedSkills(
          job.skills.map((s) => ({
            skillId: s.skillId,
            skillCode: s.skillCode,
            skillName: s.skillName,
            required: s.required,
            minScore: s.minScore,
          }))
        );
      }
    }
  }, [job, reset]);

  // Kiểm tra trạng thái chỉ đọc
  const isReadOnly = job?.status === "PENDING" || job?.status === "CLOSED";

  // Hàm lưu dữ liệu chung (tạo mới hoặc sửa + replace skills)
  const saveJobData = async (
    formValues: JobFormValues
  ): Promise<string | null> => {
    const payload: CreateJobDto = {
      title: formValues.title.trim(),
      position: formValues.position.trim(),
      level: formValues.level,
      type: formValues.type,
      quantity: formValues.quantity,
      city: formValues.city?.trim() || undefined,
      address: formValues.address?.trim() || undefined,
      workMode: formValues.workMode,
      salaryMin: formValues.salaryMin != null && !isNaN(formValues.salaryMin) && formValues.salaryMin > 0 ? formValues.salaryMin : undefined,
      salaryMax: formValues.salaryMax != null && !isNaN(formValues.salaryMax) && formValues.salaryMax > 0 ? formValues.salaryMax : undefined,
      deadline: formValues.deadline,
      description: formValues.description?.trim() || undefined,
      requirements: formValues.requirements?.trim() || undefined,
      benefits: formValues.benefits?.trim() || undefined,
    };

    const effectiveJobId = id || createdJobId;
    let targetJobId = effectiveJobId;

    if (!effectiveJobId) {
      // 1. Tạo mới JD
      const createRes = await hrJobsApi.createJob(payload);
      targetJobId = createRes.data.id;
      setCreatedJobId(createRes.data.id);
    } else {
      // 2. Cập nhật JD cũ
      await hrJobsApi.updateJob(effectiveJobId, payload);
    }

    // 3. Cập nhật kỹ năng nếu có targetJobId
    if (targetJobId) {
      const skillItems = selectedSkills.map((s) => ({
        skillId: toCanonicalSkillCode(s.skillName, s.skillCode || s.skillId),
        required: s.required,
        minScore: s.minScore,
      }));
      await hrJobsApi.replaceJobSkills(targetJobId, skillItems);
    }

    return targetJobId || null;
  };

  // Nút "Lưu nháp"
  const handleSaveDraft = async () => {
    const isValid = await trigger();
    if (!isValid) {
      setActiveStep(1);
      toast.error("Vui lòng kiểm tra lại thông tin chung");
      return;
    }

    // Nếu tin đang APPROVED, mở modal xác nhận trước khi lưu
    if (job?.status === "APPROVED") {
      setIsApprovedConfirmOpen(true);
      return;
    }

    executeSaveDraft();
  };

  const executeSaveDraft = async () => {
    try {
      setIsSaving(true);
      const values = getValues();
      const savedId = await saveJobData(values);

      toast.success("Đã lưu nháp tin tuyển dụng thành công");

      if (!isEditing && savedId) {
        navigate(`/hr/jobs/${savedId}/edit`);
      } else {
        refetch();
      }
    } catch (err: unknown) {
      const errorWithResponse = err as { response?: { data?: { detail?: string; message?: string } } };
      const detail = errorWithResponse.response?.data?.detail;
      if (detail) {
        toast.error(detail);
      }
    } finally {
      setIsSaving(false);
      setIsApprovedConfirmOpen(false);
    }
  };

  // Nút "Gửi duyệt"
  const handleSubmitForReview = async () => {
    const isValid = await trigger();
    if (!isValid) {
      setActiveStep(1);
      toast.error("Vui lòng điền đầy đủ thông tin hợp lệ ở Bước 1");
      return;
    }

    // Chặn tại client nếu chưa có >= 1 kỹ năng bắt buộc
    const hasRequiredSkill = selectedSkills.some((s) => s.required);
    if (!hasRequiredSkill) {
      setActiveStep(2);
      toast.error("Cần ít nhất 1 kỹ năng bắt buộc để gửi duyệt");
      return;
    }

    try {
      setIsSaving(true);
      const values = getValues();
      const savedId = await saveJobData(values);

      if (savedId) {
        await hrJobsApi.submitJob(savedId);
        toast.success("Đã gửi duyệt tin tuyển dụng thành công");
        navigate("/hr/jobs");
      }
    } catch (err: unknown) {
      const errorWithResponse = err as { response?: { data?: { detail?: string; message?: string } } };
      const detail = errorWithResponse.response?.data?.detail;
      if (detail) {
        toast.error(detail);
      }
    } finally {
      setIsSaving(false);
    }
  };

  // Điều hướng chuyển bước có kiểm tra validation bước 1
  const handleNextStep = async () => {
    const isValid = await trigger();
    if (isValid) {
      setActiveStep(2);
    }
  };

  if (isEditing && isJobLoading) {
    return (
      <div className="mx-auto max-w-5xl py-12 text-center text-sm text-muted">
        Đang tải thông tin tin tuyển dụng...
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6 pb-12">
      {/* Nút quay lại & Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-4">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate("/hr/jobs")}
            leftIcon={<ArrowLeft className="h-4 w-4" />}
          >
            Danh sách tin
          </Button>
          <div>
            <h1 className="text-xl font-bold text-foreground">
              {isEditing ? "Chỉnh Sửa Tin Tuyển Dụng" : "Tạo Tin Tuyển Dụng Mới"}
            </h1>
            <p className="text-xs text-muted mt-0.5">
              {isEditing
                ? "Cập nhật nội dung JD và danh mục kỹ năng yêu cầu"
                : "Điền thông tin vị trí và các kỹ năng bắt buộc đối với ứng viên"}
            </p>
          </div>
        </div>

        {job && (
          <div className="flex items-center gap-2">
            <span className="text-xs text-muted">Trạng thái:</span>
            <JobStatusBadge status={job.status} />
          </div>
        )}
      </div>

      {/* Banners thông báo trạng thái */}
      {job?.status === "REJECTED" && (
        <div className="p-4 rounded-xl border border-rose-200 bg-rose-50 flex items-start gap-3 text-danger">
          <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
          <div className="text-sm">
            <p className="font-bold">Tin tuyển dụng bị trả lại</p>
            <p className="mt-1 text-xs text-rose-900 leading-relaxed">
              Lý do bị trả lại:{" "}
              <strong>{job.rejectedReason || "Vui lòng xem lại nội dung tin và bổ sung thông tin chính xác."}</strong>
            </p>
          </div>
        </div>
      )}

      {job?.status === "PENDING" && (
        <div className="p-4 rounded-xl border border-amber-200 bg-amber-50 flex items-start gap-3 text-amber-800">
          <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5" />
          <div className="text-sm">
            <p className="font-bold">Tin đang chờ duyệt, không thể chỉnh sửa</p>
            <p className="mt-1 text-xs text-amber-900 leading-relaxed">
              Tin tuyển dụng đang được Cán bộ tuyển sinh & hướng nghiệp (STAFF) kiểm tra. Trong thời gian này thông tin được đặt ở chế độ chỉ đọc.
            </p>
          </div>
        </div>
      )}

      {job?.status === "APPROVED" && (
        <div className="p-4 rounded-xl border border-emerald-200 bg-emerald-50 flex items-start gap-3 text-emerald-800">
          <CheckCircle2 className="h-5 w-5 shrink-0 mt-0.5" />
          <div className="text-sm">
            <p className="font-bold">Tin tuyển dụng đã được phê duyệt</p>
            <p className="mt-1 text-xs text-emerald-900 leading-relaxed">
              Lưu ý: Nếu bạn chỉnh sửa và bấm Lưu, tin sẽ tự động chuyển về trạng thái <strong>Chờ duyệt (PENDING)</strong> và cần được Cán bộ xem xét lại.
            </p>
          </div>
        </div>
      )}

      {job?.status === "CLOSED" && (
        <div className="p-4 rounded-xl border border-slate-200 bg-slate-100 flex items-start gap-3 text-slate-700">
          <Lock className="h-5 w-5 shrink-0 mt-0.5" />
          <div className="text-sm">
            <p className="font-bold">Tin tuyển dụng đã đóng</p>
            <p className="mt-1 text-xs text-slate-600 leading-relaxed">
              Tin này đã ngừng nhận hồ sơ và không thể chỉnh sửa lại.
            </p>
          </div>
        </div>
      )}

      {/* Thanh Stepper 2 bước */}
      <div className="flex border-b border-border">
        <button
          type="button"
          onClick={() => setActiveStep(1)}
          className={cn(
            "flex items-center gap-2 px-6 py-3 border-b-2 font-semibold text-sm transition-all cursor-pointer",
            activeStep === 1
              ? "border-primary text-primary"
              : "border-transparent text-muted hover:text-foreground"
          )}
        >
          <FileText className="h-4 w-4" />
          <span>1. Thông tin chung</span>
        </button>

        <button
          type="button"
          onClick={handleNextStep}
          className={cn(
            "flex items-center gap-2 px-6 py-3 border-b-2 font-semibold text-sm transition-all cursor-pointer",
            activeStep === 2
              ? "border-primary text-primary"
              : "border-transparent text-muted hover:text-foreground"
          )}
        >
          <Layers className="h-4 w-4" />
          <span>2. Kỹ năng yêu cầu</span>
          <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 font-bold">
            {selectedSkills.length}
          </span>
        </button>
      </div>

      {/* Form Content */}
      <form onSubmit={(e) => e.preventDefault()}>
        {/* BƯỚC 1: THÔNG TIN CHUNG */}
        {activeStep === 1 && (
          <Card>
            <CardHeader>
              <CardTitle>Thông tin cơ bản về vị trí tuyển dụng</CardTitle>
            </CardHeader>

            <CardContent className="space-y-5">
              {/* Tiêu đề & Vị trí */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  label="Tiêu đề tin tuyển dụng"
                  required
                  disabled={isReadOnly}
                  placeholder="Ví dụ: Lập trình viên Java Backend (Fresher / Junior)"
                  error={errors.title?.message}
                  {...register("title")}
                />

                <Input
                  label="Vị trí tuyển dụng"
                  required
                  disabled={isReadOnly}
                  placeholder="Ví dụ: Backend Developer, Frontend Engineer..."
                  error={errors.position?.message}
                  {...register("position")}
                />
              </div>

              {/* Cấp bậc, Hình thức, Số lượng */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="space-y-1.5">
                  <label className="block text-sm font-medium text-foreground">
                    Cấp bậc <span className="text-danger">*</span>
                  </label>
                  <select
                    disabled={isReadOnly}
                    className="w-full h-10 px-3 py-2 text-sm rounded-lg border border-border bg-surface text-foreground outline-none focus:border-primary disabled:bg-slate-50 disabled:cursor-not-allowed"
                    {...register("level")}
                  >
                    <option value="INTERN">Thực tập (Intern)</option>
                    <option value="FRESHER">Fresher</option>
                    <option value="JUNIOR">Junior</option>
                    <option value="MIDDLE">Middle</option>
                    <option value="SENIOR">Senior</option>
                  </select>
                  {errors.level && (
                    <p className="text-xs text-danger">{errors.level.message}</p>
                  )}
                </div>

                <div className="space-y-1.5">
                  <label className="block text-sm font-medium text-foreground">
                    Hình thức làm việc <span className="text-danger">*</span>
                  </label>
                  <select
                    disabled={isReadOnly}
                    className="w-full h-10 px-3 py-2 text-sm rounded-lg border border-border bg-surface text-foreground outline-none focus:border-primary disabled:bg-slate-50 disabled:cursor-not-allowed"
                    {...register("type")}
                  >
                    <option value="FULLTIME">Toàn thời gian (Full-time)</option>
                    <option value="PARTTIME">Bán thời gian (Part-time)</option>
                    <option value="INTERNSHIP">Thực tập (Internship)</option>
                  </select>
                  {errors.type && (
                    <p className="text-xs text-danger">{errors.type.message}</p>
                  )}
                </div>

                <Input
                  label="Số lượng tuyển"
                  type="number"
                  required
                  disabled={isReadOnly}
                  min={1}
                  placeholder="1"
                  error={errors.quantity?.message}
                  {...register("quantity", { valueAsNumber: true })}
                />
              </div>

              {/* Địa điểm, Chế độ làm việc */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <Input
                  label="Tỉnh / Thành phố"
                  disabled={isReadOnly}
                  placeholder="Hà Nội, Đà Nẵng, TP. HCM..."
                  error={errors.city?.message}
                  {...register("city")}
                />

                <Input
                  label="Địa chỉ làm việc"
                  disabled={isReadOnly}
                  placeholder="Tòa nhà, số nhà, tên đường..."
                  error={errors.address?.message}
                  {...register("address")}
                />

                <div className="space-y-1.5">
                  <label className="block text-sm font-medium text-foreground">
                    Chế độ làm việc <span className="text-danger">*</span>
                  </label>
                  <select
                    disabled={isReadOnly}
                    className="w-full h-10 px-3 py-2 text-sm rounded-lg border border-border bg-surface text-foreground outline-none focus:border-primary disabled:bg-slate-50 disabled:cursor-not-allowed"
                    {...register("workMode")}
                  >
                    <option value="ONSITE">Làm việc tại văn phòng (On-site)</option>
                    <option value="HYBRID">Linh hoạt (Hybrid)</option>
                    <option value="REMOTE">Làm từ xa (Remote)</option>
                  </select>
                  {errors.workMode && (
                    <p className="text-xs text-danger">{errors.workMode.message}</p>
                  )}
                </div>
              </div>

              {/* Mức lương, Hạn nộp */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <Input
                  label="Lương tối thiểu (VNĐ)"
                  type="number"
                  disabled={isReadOnly}
                  placeholder="Ví dụ: 10000000"
                  error={errors.salaryMin?.message}
                  {...register("salaryMin", { valueAsNumber: true })}
                />

                <Input
                  label="Lương tối đa (VNĐ)"
                  type="number"
                  disabled={isReadOnly}
                  placeholder="Ví dụ: 20000000"
                  error={errors.salaryMax?.message}
                  {...register("salaryMax", { valueAsNumber: true })}
                />

                <Input
                  label="Hạn nộp hồ sơ"
                  type="date"
                  required
                  disabled={isReadOnly}
                  error={errors.deadline?.message}
                  {...register("deadline")}
                />
              </div>

              {/* Mô tả, Yêu cầu, Quyền lợi */}
              <div className="space-y-4 pt-2 border-t border-border-subtle">
                <div className="space-y-1.5">
                  <label className="block text-sm font-medium text-foreground">
                    Mô tả công việc
                  </label>
                  <textarea
                    rows={4}
                    disabled={isReadOnly}
                    placeholder="Chi tiết công việc cần làm, dự án tham gia..."
                    className="w-full rounded-lg border border-border bg-surface p-3 text-sm text-foreground outline-none focus:border-primary disabled:bg-slate-50 disabled:cursor-not-allowed"
                    {...register("description")}
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="block text-sm font-medium text-foreground">
                    Yêu cầu ứng viên
                  </label>
                  <textarea
                    rows={4}
                    disabled={isReadOnly}
                    placeholder="Kiến thức chuyên môn, kinh nghiệm, thái độ..."
                    className="w-full rounded-lg border border-border bg-surface p-3 text-sm text-foreground outline-none focus:border-primary disabled:bg-slate-50 disabled:cursor-not-allowed"
                    {...register("requirements")}
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="block text-sm font-medium text-foreground">
                    Quyền lợi & Đãi ngộ
                  </label>
                  <textarea
                    rows={3}
                    disabled={isReadOnly}
                    placeholder="Chế độ lương thưởng, bảo hiểm, đào tạo..."
                    className="w-full rounded-lg border border-border bg-surface p-3 text-sm text-foreground outline-none focus:border-primary disabled:bg-slate-50 disabled:cursor-not-allowed"
                    {...register("benefits")}
                  />
                </div>
              </div>

              {/* Nút chuyển sang bước 2 */}
              <div className="flex justify-end pt-4">
                <Button variant="primary" onClick={handleNextStep}>
                  Tiếp tục: Kỹ năng yêu cầu
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* BƯỚC 2: KỸ NĂNG YÊU CẦU */}
        {activeStep === 2 && (
          <div className="space-y-6">
            <JobSkillPicker
              selectedSkills={selectedSkills}
              onChange={setSelectedSkills}
              readOnly={isReadOnly}
            />

            <div className="flex items-center justify-between pt-4 border-t border-border">
              <Button
                variant="outline"
                onClick={() => setActiveStep(1)}
                leftIcon={<ArrowLeft className="h-4 w-4" />}
              >
                Quay lại: Thông tin chung
              </Button>

              <div className="flex items-center gap-2">
                {!isReadOnly && (
                  <Button
                    variant="outline"
                    onClick={handleSaveDraft}
                    isLoading={isSaving}
                    leftIcon={<Save className="h-4 w-4" />}
                  >
                    Lưu nháp
                  </Button>
                )}

                {!isReadOnly && (!job || job.status === "DRAFT" || job.status === "REJECTED") && (
                  <Button
                    variant="primary"
                    onClick={handleSubmitForReview}
                    isLoading={isSaving}
                    leftIcon={<Send className="h-4 w-4" />}
                  >
                    Gửi duyệt
                  </Button>
                )}
              </div>
            </div>
          </div>
        )}
      </form>

      {/* Thanh cố định bên dưới để lưu/gửi nhanh ở bất kỳ bước nào */}
      {!isReadOnly && activeStep === 1 && (
        <div className="flex items-center justify-end gap-3 pt-2">
          <Button
            variant="outline"
            onClick={handleSaveDraft}
            isLoading={isSaving}
            leftIcon={<Save className="h-4 w-4" />}
          >
            Lưu nháp
          </Button>

          {(!job || job.status === "DRAFT" || job.status === "REJECTED") && (
            <Button
              variant="primary"
              onClick={handleSubmitForReview}
              isLoading={isSaving}
              leftIcon={<Send className="h-4 w-4" />}
            >
              Gửi duyệt
            </Button>
          )}
        </div>
      )}

      {/* Modal xác nhận lưu khi JD đang ở trạng thái APPROVED */}
      <Modal
        isOpen={isApprovedConfirmOpen}
        onClose={() => setIsApprovedConfirmOpen(false)}
        title="Xác nhận cập nhật tin tuyển dụng đã duyệt"
        description="Tin tuyển dụng này hiện đang ở trạng thái ĐÃ DUYỆT (APPROVED)."
        footer={
          <div className="flex items-center justify-end gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsApprovedConfirmOpen(false)}
              disabled={isSaving}
            >
              Hủy
            </Button>
            <Button
              variant="primary"
              size="sm"
              isLoading={isSaving}
              onClick={executeSaveDraft}
            >
              Tôi đồng ý lưu
            </Button>
          </div>
        }
      >
        <p className="text-sm text-foreground leading-relaxed">
          Sau khi lưu, tin sẽ tự động quay về trạng thái <strong>Chờ duyệt (PENDING)</strong> và cần được Cán bộ kiểm tra, phê duyệt lại trước khi tiếp tục hiển thị công khai.
        </p>
      </Modal>
    </div>
  );
}
