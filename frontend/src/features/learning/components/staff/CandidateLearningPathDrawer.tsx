import { X, BookOpen, AlertCircle, Target, Mail, Phone, Award } from "lucide-react";
import type { UpskillingCandidateItem } from "../../../../api/endpoints/learning.api";

interface CandidateLearningPathDrawerProps {
  candidate: UpskillingCandidateItem | null;
  onClose: () => void;
}

const ENROLLMENT_STATUS_BADGE: Record<string, { label: string; className: string }> = {
  COMPLETED: { label: "Hoàn thành", className: "bg-emerald-50 text-emerald-700 border-emerald-200" },
  IN_PROGRESS: { label: "Đang học", className: "bg-blue-50 text-blue-700 border-blue-200" },
  ENROLLED: { label: "Đã ghi danh", className: "bg-amber-50 text-amber-700 border-amber-200" },
  FAILED: { label: "Chưa đạt", className: "bg-rose-50 text-rose-700 border-rose-200" },
};

export function CandidateLearningPathDrawer({
  candidate,
  onClose,
}: CandidateLearningPathDrawerProps) {
  if (!candidate) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-slate-900/40 backdrop-blur-[1px]">
      <div
        className="fixed inset-0"
        onClick={onClose}
        aria-hidden="true"
      />

      <div className="relative z-10 w-full max-w-xl bg-white h-full shadow-2xl flex flex-col overflow-hidden animate-in slide-in-from-right duration-200">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
          <div>
            <span className="inline-block px-2 py-0.5 rounded bg-blue-100 text-blue-800 text-xs font-bold mb-1">
              {candidate.classCode}
            </span>
            <h3 className="text-lg font-bold text-slate-900">
              Lộ trình học bổ sung — {candidate.candidate.fullName}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-200/60 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* Thông tin ứng viên & Mục tiêu */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 bg-slate-50 rounded-xl p-4 border border-slate-200">
            <div className="space-y-1.5">
              <p className="text-xs font-semibold uppercase text-slate-500">
                Thông tin liên hệ
              </p>
              <p className="text-sm text-slate-700 flex items-center gap-2">
                <Mail className="w-4 h-4 text-slate-400" />
                {candidate.candidate.email}
              </p>
              <p className="text-sm text-slate-700 flex items-center gap-2">
                <Phone className="w-4 h-4 text-slate-400" />
                {candidate.candidate.phone || "Chưa cập nhật"}
              </p>
            </div>
            <div className="space-y-1.5">
              <p className="text-xs font-semibold uppercase text-slate-500">
                Mục tiêu & Sẵn sàng
              </p>
              <p className="text-sm font-semibold text-slate-900 flex items-center gap-2">
                <Target className="w-4 h-4 text-blue-600" />
                {candidate.target}
              </p>
              <p className="text-sm text-slate-700 flex items-center gap-2">
                <Award className="w-4 h-4 text-emerald-600" />
                Mức sẵn sàng: <strong className="text-emerald-700">{candidate.readiness}%</strong>
              </p>
            </div>
          </div>

          {/* Kỹ năng còn thiếu */}
          <div>
            <h4 className="text-sm font-bold text-slate-900 flex items-center gap-2 mb-3">
              <AlertCircle className="w-4 h-4 text-rose-500" />
              Kỹ năng còn thiếu ({candidate.missingSkills.length})
            </h4>
            {candidate.missingSkills.length === 0 ? (
              <p className="text-sm text-emerald-600 bg-emerald-50 p-3 rounded-lg border border-emerald-200">
                Ứng viên đã đáp ứng toàn bộ kỹ năng yêu cầu của mục tiêu.
              </p>
            ) : (
              <div className="space-y-2.5">
                {candidate.missingSkills.map((sk) => (
                  <div
                    key={sk.skillId}
                    className="flex items-center justify-between p-3 rounded-lg border border-slate-200 bg-white"
                  >
                    <div>
                      <p className="text-sm font-semibold text-slate-900">
                        {sk.skillName}
                      </p>
                      <p className="text-xs text-slate-500">
                        Hiện tại: {sk.current}đ / Yêu cầu: {sk.required}đ
                      </p>
                    </div>
                    <div className="text-right">
                      <span className="inline-block px-2 py-0.5 rounded-full text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200">
                        Thiếu {sk.gap}đ
                      </span>
                      <p className="text-[11px] text-slate-400 mt-0.5">
                        Ưu tiên: {sk.priority}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Lộ trình các môn học đang ghi danh */}
          <div>
            <h4 className="text-sm font-bold text-slate-900 flex items-center gap-2 mb-3">
              <BookOpen className="w-4 h-4 text-blue-600" />
              Lộ trình các môn học bổ sung ({candidate.enrollments.length})
            </h4>
            {candidate.enrollments.length === 0 ? (
              <p className="text-sm text-slate-500 bg-slate-50 p-3 rounded-lg border border-slate-200">
                Ứng viên chưa ghi danh khoá học bổ sung nào trên LMS.
              </p>
            ) : (
              <div className="space-y-3">
                {candidate.enrollments.map((enr, idx) => {
                  const badge =
                    ENROLLMENT_STATUS_BADGE[enr.status] ||
                    ENROLLMENT_STATUS_BADGE.ENROLLED;

                  return (
                    <div
                      key={`${enr.course}-${idx}`}
                      className="p-3.5 rounded-xl border border-slate-200 bg-slate-50/50 space-y-2"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <p className="text-sm font-semibold text-slate-900">
                          {enr.course}
                        </p>
                        <span
                          className={`shrink-0 px-2 py-0.5 rounded-full text-xs font-semibold border ${badge.className}`}
                        >
                          {badge.label}
                        </span>
                      </div>

                      <div className="flex items-center justify-between text-xs text-slate-600">
                        <span>Tiến độ: <strong>{enr.progress}%</strong></span>
                        <span>
                          Điểm đánh giá:{" "}
                          <strong>{enr.score !== null && enr.score !== undefined ? `${enr.score}đ` : "Chưa có"}</strong>
                        </span>
                      </div>

                      <div className="h-2 w-full bg-slate-200 rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full transition-all ${
                            enr.status === "COMPLETED"
                              ? "bg-emerald-500"
                              : enr.status === "FAILED"
                              ? "bg-rose-500"
                              : "bg-blue-600"
                          }`}
                          style={{ width: `${Math.min(100, Math.max(0, enr.progress || 0))}%` }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3.5 border-t border-slate-200 bg-slate-50 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-900 text-white text-sm font-medium transition-colors"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
}
