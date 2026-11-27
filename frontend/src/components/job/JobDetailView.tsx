import type React from "react";
import {
  ArrowLeft,
  Calendar,
  Clock,
  MapPin,
  Users,
  BriefcaseBusiness,
  BadgeDollarSign,
  Layers3,
  Laptop,
  CalendarDays,
  ShieldCheck,
} from "lucide-react";
import type { JobResponse } from "../../api/endpoints/hrJobs.api";
import { Card, CardContent, CardHeader, CardTitle } from "../common/Card";
import { JobStatusBadge } from "../hr/JobStatusBadge";
import {
  LEVEL_LABELS,
  TYPE_LABELS,
  WORK_MODE_LABELS,
  formatDate,
  formatSalaryVND,
} from "../../utils/jobLabels";

export interface JobDetailViewProps {
  job: JobResponse;
  actions?: React.ReactNode;
  backTo?: { label: string; onClick: () => void };
  viewer: "hr" | "staff";
}

function getInitials(name?: string) {
  if (!name?.trim()) return "TK";
  const words = name.trim().split(/\s+/);
  return words.length === 1
    ? words[0].slice(0, 2).toUpperCase()
    : (words[0][0] + words[words.length - 1][0]).toUpperCase();
}

function formatDateTime(value?: string | null) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function deadlineStatus(deadline: string) {
  if (!deadline) return null;
  const deadlineDate = /^\d{4}-\d{2}-\d{2}$/.test(deadline)
    ? new Date(`${deadline}T23:59:59`)
    : new Date(deadline);
  if (Number.isNaN(deadlineDate.getTime())) return null;
  const now = new Date();
  const remainingMs = deadlineDate.getTime() - now.getTime();
  if (remainingMs < 0) return { label: "Đã hết hạn", className: "bg-rose-50 text-rose-700 border-rose-200" };
  const days = Math.ceil(remainingMs / (24 * 60 * 60 * 1000));
  if (days <= 7) return { label: `Còn ${days} ngày`, className: "bg-amber-50 text-amber-800 border-amber-200" };
  return { label: `Còn ${days} ngày`, className: "bg-emerald-50 text-emerald-700 border-emerald-200" };
}

function SectionTitle({ title, emerald = false }: { title: string; emerald?: boolean }) {
  return (
    <h2 className={`text-base font-bold text-foreground flex items-center gap-2 border-l-4 ${emerald ? "border-emerald-500" : "border-primary"} pl-3`}>
      {title}
    </h2>
  );
}

export function JobDetailView({ job, actions, backTo, viewer }: JobDetailViewProps) {
  const deadlineInfo = deadlineStatus(job.deadline);
  const locationText = [job.address, job.city].filter((value): value is string => Boolean(value?.trim())).join(", ");
  const skills = job.skills?.length ? job.skills : (job.jobSkills || []);
  const level = job.level ? LEVEL_LABELS[job.level] || job.level : "";
  const type = job.type ? TYPE_LABELS[job.type] || job.type : "";
  const workMode = job.workMode ? WORK_MODE_LABELS[job.workMode] || job.workMode : "";

  return (
    <div className="mx-auto max-w-7xl space-y-6 py-4 pb-16 sm:py-6">
      <div className="flex items-center justify-between gap-3 text-xs text-muted">
        {backTo ? (
          <button type="button" onClick={backTo.onClick} className="inline-flex items-center gap-1.5 font-medium hover:text-primary transition-colors">
            <ArrowLeft className="h-4 w-4" />
            <span>{backTo.label}</span>
          </button>
        ) : <span />}
        <div className="hidden min-w-0 items-center gap-2 sm:flex">
          <span>{viewer === "hr" ? "Tin tuyển dụng" : "Kiểm duyệt tin tuyển dụng"}</span>
          <span>/</span>
          <span className="max-w-[260px] truncate font-semibold text-foreground">{job.title}</span>
        </div>
      </div>

      <div className="rounded-3xl border border-border bg-surface p-6 shadow-xs sm:p-8">
        <div className="flex flex-col gap-6 md:flex-row md:items-start md:justify-between">
          <div className="flex min-w-0 flex-1 items-start gap-4">
            <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-primary to-indigo-700 text-xl font-extrabold text-white shadow-sm sm:h-20 sm:w-20 sm:text-2xl">
              {getInitials(job.companyName)}
            </div>
            <div className="min-w-0 space-y-3">
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-xl font-extrabold tracking-tight text-foreground sm:text-2xl">{job.title}</h1>
              </div>
              {job.companyName && <p className="text-sm font-semibold text-slate-800">{job.companyName}</p>}

              <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-muted">
                {locationText && <span className="flex items-center gap-1.5"><MapPin className="h-4 w-4" />{locationText}</span>}
                {job.deadline && (
                  <span className="flex items-center gap-1.5"><Calendar className="h-4 w-4" />Hạn nộp: {formatDate(job.deadline)}</span>
                )}
                {deadlineInfo && <span className={`rounded-md border px-2 py-1 text-[11px] font-semibold ${deadlineInfo.className}`}>{deadlineInfo.label}</span>}
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <span className="rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-1 text-sm font-extrabold text-emerald-700">
                  {formatSalaryVND(job.salaryMin, job.salaryMax)}
                </span>
                {level && <span className="rounded-lg border border-border bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-700">{level}</span>}
                {type && <span className="rounded-lg border border-border bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-700">{type}</span>}
                {workMode && <span className="rounded-lg border border-border bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-700">{workMode}</span>}
                <JobStatusBadge status={job.status} />
              </div>
            </div>
          </div>
          {actions && <div className="flex shrink-0 flex-wrap items-center gap-2 md:w-auto md:flex-col md:items-end">{actions}</div>}
        </div>
      </div>

      {(job.status === "REJECTED" && job.rejectedReason) || job.reviewedBy || job.reviewedAt ? (
        <div className="space-y-2">
          {job.status === "REJECTED" && job.rejectedReason && (
            <div className="rounded-lg border border-rose-200 bg-rose-50 p-3.5 text-xs text-rose-800">
              <span className="font-semibold">Lý do trả lại: </span>{job.rejectedReason}
            </div>
          )}
          {(job.reviewedBy || job.reviewedAt) && (
            <div className="rounded-lg border border-border bg-slate-50 p-3 text-xs">
              {job.reviewedBy && <p className="font-medium text-foreground">Người kiểm duyệt: <span className="font-semibold text-primary">{job.reviewedBy}</span></p>}
              {job.reviewedAt && <p className="mt-1 text-muted">Thời gian duyệt: {formatDateTime(job.reviewedAt)}</p>}
            </div>
          )}
        </div>
      ) : null}

      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-3">
        <Card className="rounded-3xl lg:col-span-2">
          <CardContent className="space-y-8 p-6 sm:p-8">
            <div className="space-y-3">
              <SectionTitle title="Mô tả công việc" />
              <p className="whitespace-pre-line pl-4 text-sm leading-relaxed text-slate-700">{job.description?.trim() || "Chưa có nội dung"}</p>
            </div>
            <div className="space-y-3">
              <SectionTitle title="Yêu cầu ứng viên" />
              <p className="whitespace-pre-line pl-4 text-sm leading-relaxed text-slate-700">{job.requirements?.trim() || "Chưa có nội dung"}</p>
            </div>
            <div className="space-y-3">
              <SectionTitle title="Quyền lợi được hưởng" emerald />
              <p className="whitespace-pre-line pl-4 text-sm leading-relaxed text-slate-700">{job.benefits?.trim() || "Chưa có nội dung"}</p>
            </div>
            <div className="space-y-3">
              <SectionTitle title="Kỹ năng chuyên môn và điểm chuẩn tối thiểu" />
              {skills.length > 0 ? (
                <div className="grid grid-cols-1 gap-3 pl-4 sm:grid-cols-2">
                  {skills.map((skill) => {
                    const score = Math.max(0, Math.min(100, skill.minScore || 0));
                    return (
                      <div key={skill.skillId || skill.skillCode || skill.skillName} className="rounded-2xl border border-border bg-slate-50/70 p-3">
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <p className="text-xs font-bold text-foreground">{skill.skillName || skill.skillCode}</p>
                            <p className="mt-1 text-[11px] text-muted">tối thiểu {skill.minScore} điểm</p>
                          </div>
                          <span className={`rounded px-1.5 py-0.5 text-[10px] font-semibold ${skill.required ? "border border-rose-200 bg-rose-50 text-rose-700" : "border border-blue-200 bg-blue-50 text-blue-700"}`}>
                            {skill.required ? "Bắt buộc" : "Ưu tiên"}
                          </span>
                        </div>
                        <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-200"><div className="h-full rounded-full bg-primary" style={{ width: `${score}%` }} /></div>
                      </div>
                    );
                  })}
                </div>
              ) : <p className="pl-4 text-xs text-muted">Chưa có nội dung</p>}
            </div>
            {locationText && (
              <div className="space-y-3">
                <SectionTitle title="Địa điểm làm việc" />
                <p className="flex items-start gap-2 pl-4 text-sm text-slate-700"><MapPin className="mt-0.5 h-4 w-4 shrink-0 text-muted" />{locationText}</p>
              </div>
            )}
          </CardContent>
        </Card>

        <div className="space-y-6 lg:sticky lg:top-24">
          <Card className="rounded-3xl">
            <CardHeader><CardTitle className="text-sm">Thông tin chung</CardTitle></CardHeader>
            <CardContent className="space-y-3 pt-1 text-xs">
              {job.position && <InfoRow icon={<BriefcaseBusiness />} label="Vị trí" value={job.position} />}
              {level && <InfoRow icon={<Layers3 />} label="Cấp bậc" value={level} />}
              {type && <InfoRow icon={<BadgeDollarSign />} label="Hình thức" value={type} />}
              {workMode && <InfoRow icon={<Laptop />} label="Hình thức làm việc" value={workMode} />}
              {job.quantity != null && <InfoRow icon={<Users />} label="Số lượng tuyển" value={`${job.quantity} người`} />}
              {job.deadline && <InfoRow icon={<CalendarDays />} label="Hạn nộp" value={formatDate(job.deadline)} />}
              {job.createdAt && <InfoRow icon={<Clock />} label="Ngày tạo" value={formatDateTime(job.createdAt)} />}
              {job.updatedAt && <InfoRow icon={<ShieldCheck />} label="Cập nhật lần cuối" value={formatDateTime(job.updatedAt)} />}
            </CardContent>
          </Card>

          <Card className="rounded-3xl">
            <CardHeader><CardTitle className="text-sm">Thông tin doanh nghiệp</CardTitle></CardHeader>
            <CardContent className="pt-1">
              <div className="flex items-center gap-3">
                <div className="flex h-12 w-12 items-center justify-center rounded-xl border border-border bg-slate-100 text-base font-bold text-primary">{getInitials(job.companyName)}</div>
                <div className="min-w-0">
                  {job.companyName && <p className="truncate text-sm font-bold text-foreground">{job.companyName}</p>}
                  {locationText && <p className="mt-1 text-xs text-muted">{locationText}</p>}
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

function InfoRow({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-start justify-between gap-3">
      <span className="flex items-center gap-1.5 text-muted">{icon && <span className="[&>svg]:h-3.5 [&>svg]:w-3.5 [&>svg]:text-muted">{icon}</span>}{label}</span>
      <span className="text-right font-semibold text-foreground">{value}</span>
    </div>
  );
}
