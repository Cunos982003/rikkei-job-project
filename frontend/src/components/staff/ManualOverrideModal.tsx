import React, { useState, useEffect } from "react";
import { Modal } from "@/components/common/Modal";
import { Button } from "@/components/common/Button";
import { syncApi, type ExternalRecordDetail } from "@/api/endpoints/sync.api";
import { toast } from "sonner";
import { AlertTriangle, ShieldAlert, ChevronDown, ChevronUp, Code2, History } from "lucide-react";

const KNOWN_SUBJECTS: Record<string, { title: string; score: number; extra?: string; attendance?: number; homework?: number }> = {
  "JV-01": { title: "Java Core & Lập trình Hướng đối tượng (OOP)", score: 8.8, attendance: 100, homework: 96 },
  "DB-01": { title: "Cơ sở dữ liệu Quan hệ & Thiết kế SQL", score: 8.5, attendance: 98, homework: 92 },
  "SB-01": { title: "Lập trình Backend với Spring Boot Framework", score: 9.0, attendance: 100, homework: 95 },
  "GIT-01": { title: "Quản lý mã nguồn nhóm với Git & GitHub", score: 9.5, attendance: 100, homework: 98 },
  "FE-01": { title: "Thiết kế Web Responsive HTML5 & CSS3", score: 8.5, attendance: 95, homework: 90 },
  "JS-01": { title: "Lập trình JavaScript Nâng cao & ES6+", score: 8.7, attendance: 98, homework: 94 },
  "RC-01": { title: "Phát triển ứng dụng Web Frontend với React", score: 9.0, attendance: 100, homework: 95 },
  "EXAM-JV-2026": { title: "Khảo thí Thực hành Lập trình Java 2026", score: 8.5 },
  "EXAM-DB-2026": { title: "Khảo thí Truy vấn Cơ sở dữ liệu SQL 2026", score: 8.0 },
  "EXAM-SB-2026": { title: "Khảo thí Xây dựng RESTful API Spring Boot 2026", score: 8.8 },
  "IOC-2026-B1": { title: "Dự án Thực tập Doanh nghiệp Đợt 1 (IOC)", score: 8.7 },
};

interface ManualOverrideModalProps {
  isOpen: boolean;
  onClose: () => void;
  record: ExternalRecordDetail | null;
  onSuccess: () => void;
}

export const ManualOverrideModal: React.FC<ManualOverrideModalProps> = ({
  isOpen,
  onClose,
  record,
  onSuccess,
}) => {
  const [field, setField] = useState("score");
  const [customFieldName, setCustomFieldName] = useState("");
  const [value, setValue] = useState("");
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const [showRawJson, setShowRawJson] = useState(false);

  useEffect(() => {
    if (record) {
      const payload = record.payload || {};
      const latestOverride =
        record.overrides && record.overrides.length > 0
          ? record.overrides[record.overrides.length - 1]
          : null;

      // Find known meta
      let defaultScore = 8.8;
      for (const [key, meta] of Object.entries(KNOWN_SUBJECTS)) {
        if (record.sourceKey.includes(key)) {
          defaultScore = meta.score;
          break;
        }
      }

      if (record.source === "PM") {
        setField("finalScore");
        const val =
          latestOverride?.value ??
          (payload.finalScore && typeof payload.finalScore !== "boolean" ? payload.finalScore : defaultScore);
        setValue(String(val));
      } else {
        setField("score");
        const val =
          latestOverride?.value ??
          (payload.score && typeof payload.score !== "boolean" ? payload.score : defaultScore);
        setValue(String(val));
      }

      setCustomFieldName("");
      setReason(latestOverride?.reason || "");
      setShowRawJson(false);
    }
  }, [record]);

  if (!record) return null;

  const payload = record.payload || {};
  const effectiveField = field === "custom" ? customFieldName.trim() : field;
  const latestOverride =
    record.overrides && record.overrides.length > 0
      ? record.overrides[record.overrides.length - 1]
      : null;

  // Resolve meta
  let knownMeta = { title: record.sourceKey, score: 8.8, attendance: 100, homework: 95 };
  for (const [key, meta] of Object.entries(KNOWN_SUBJECTS)) {
    if (record.sourceKey.includes(key)) {
      knownMeta = { ...knownMeta, ...meta };
      break;
    }
  }

  const displayTitle =
    payload.subjectName ||
    payload.examTitle ||
    payload.topicName ||
    payload.batchName ||
    knownMeta.title;

  const displayScore =
    latestOverride && (latestOverride.field === "score" || latestOverride.field === "finalScore")
      ? latestOverride.value
      : payload.score && typeof payload.score !== "boolean"
      ? payload.score
      : payload.finalScore && typeof payload.finalScore !== "boolean"
      ? payload.finalScore
      : knownMeta.score;

  const displayAttendance =
    typeof payload.attendanceRate === "number"
      ? payload.attendanceRate
      : knownMeta.attendance;

  const displayHomework =
    typeof payload.homeworkRate === "number"
      ? payload.homeworkRate
      : knownMeta.homework;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!effectiveField) {
      toast.error("Vui lòng chọn hoặc nhập tên trường cần can thiệp sửa đổi.");
      return;
    }
    if (!value.trim()) {
      toast.error("Vui lòng nhập giá trị mới.");
      return;
    }
    if (!reason.trim()) {
      toast.error("Vui lòng nhập lý do can thiệp sửa tay (Bắt buộc phục vụ Audit).");
      return;
    }

    try {
      setLoading(true);
      await syncApi.overrideRecordField(record.id, {
        field: effectiveField,
        value: value.trim(),
        reason: reason.trim(),
      });
      toast.success("Can thiệp sửa tay thành công! Điểm năng lực đã được tính toán lại.");
      onSuccess();
      onClose();
    } catch (err: any) {
      toast.error(
        err?.response?.data?.message || err?.message || "Lỗi khi lưu can thiệp sửa tay."
      );
    } finally {
      setLoading(false);
    }
  };

  const fieldOptions =
    record.source === "PM"
      ? [
          { key: "finalScore", label: "Điểm tổng kết thực tập (finalScore)" },
          { key: "acceptedScore", label: "Điểm nghiệm thu (acceptedScore)" },
          { key: "evaluation", label: "Đánh giá chung (evaluation)" },
          { key: "custom", label: "Trường khác (nhập tùy chỉnh)..." },
        ]
      : [
          { key: "score", label: "Điểm số chính thức (score)" },
          { key: "attendanceRate", label: "Tỷ lệ chuyên cần % (attendanceRate)" },
          { key: "homeworkRate", label: "Tỷ lệ bài tập % (homeworkRate)" },
          { key: "rpoint", label: "Điểm rpoint (nếu có)" },
          { key: "custom", label: "Trường khác (nhập tùy chỉnh)..." },
        ];

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={
        <div className="flex items-center gap-2.5 text-foreground font-bold">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-amber-50 text-amber-600 border border-amber-200">
            <ShieldAlert className="h-5 w-5" />
          </div>
          <div>
            <div className="text-base font-bold text-foreground">
              Can thiệp sửa tay dữ liệu nguồn ngoài
            </div>
            <div className="text-xs font-normal text-muted">
              Cán bộ có thể điều chỉnh điểm số hoặc chuyên cần khi có quyết định phúc khảo
            </div>
          </div>
        </div>
      }
      size="lg"
    >
      <form onSubmit={handleSubmit} className="space-y-5 text-sm">
        {/* 1. THẺ THÔNG TIN BẢN GHI HIỆN TẠI */}
        <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4 space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200 pb-3">
            <div className="flex items-center gap-2">
              <span
                className={`inline-flex items-center px-2.5 py-0.5 rounded-md text-xs font-bold ${
                  record.source === "LMS"
                    ? "bg-sky-100 text-sky-800 border border-sky-200"
                    : record.source === "EXAM"
                    ? "bg-purple-100 text-purple-800 border border-purple-200"
                    : "bg-emerald-100 text-emerald-800 border border-emerald-200"
                }`}
              >
                {record.source}
              </span>
              <span className="font-semibold text-foreground text-sm">
                {displayTitle}
              </span>
            </div>
            <span className="font-mono text-xs text-slate-500 bg-white border border-slate-200 px-2 py-0.5 rounded">
              {record.sourceKey}
            </span>
          </div>

          {/* Lưới hiển thị dữ liệu thân thiện */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
            {record.source === "LMS" && (
              <>
                <div className="bg-white p-2.5 rounded-lg border border-slate-200 shadow-2xs">
                  <div className="text-muted text-[11px]">Điểm môn học</div>
                  <div className="text-base font-bold text-emerald-700 mt-0.5">
                    {displayScore}
                  </div>
                </div>
                <div className="bg-white p-2.5 rounded-lg border border-slate-200 shadow-2xs">
                  <div className="text-muted text-[11px]">Chuyên cần</div>
                  <div className="text-sm font-semibold text-foreground mt-0.5">
                    {displayAttendance}%
                  </div>
                </div>
                <div className="bg-white p-2.5 rounded-lg border border-slate-200 shadow-2xs">
                  <div className="text-muted text-[11px]">Bài tập</div>
                  <div className="text-sm font-semibold text-foreground mt-0.5">
                    {displayHomework}%
                  </div>
                </div>
                <div className="bg-white p-2.5 rounded-lg border border-slate-200 shadow-2xs">
                  <div className="text-muted text-[11px]">Trạng thái</div>
                  <div className="text-xs font-semibold text-foreground mt-0.5">
                    {latestOverride ? (
                      <span className="text-amber-700 font-bold">Đã sửa tay</span>
                    ) : (
                      "Nguyên bản"
                    )}
                  </div>
                </div>
              </>
            )}

            {record.source === "EXAM" && (
              <>
                <div className="bg-white p-2.5 rounded-lg border border-slate-200 shadow-2xs col-span-2">
                  <div className="text-muted text-[11px]">Điểm thi khảo thí</div>
                  <div className="text-base font-bold text-purple-700 mt-0.5">
                    {displayScore}
                  </div>
                </div>
                <div className="bg-white p-2.5 rounded-lg border border-slate-200 shadow-2xs col-span-2">
                  <div className="text-muted text-[11px]">Trạng thái</div>
                  <div className="text-xs font-semibold text-foreground mt-0.5">
                    {latestOverride ? (
                      <span className="text-amber-700 font-bold">Đã sửa tay</span>
                    ) : (
                      "Nguyên bản"
                    )}
                  </div>
                </div>
              </>
            )}

            {record.source === "PM" && (
              <>
                <div className="bg-white p-2.5 rounded-lg border border-slate-200 shadow-2xs col-span-2">
                  <div className="text-muted text-[11px]">Điểm tổng kết thực tập</div>
                  <div className="text-base font-bold text-emerald-700 mt-0.5">
                    {displayScore}
                  </div>
                </div>
                <div className="bg-white p-2.5 rounded-lg border border-slate-200 shadow-2xs col-span-2">
                  <div className="text-muted text-[11px]">Trạng thái</div>
                  <div className="text-xs font-semibold text-foreground mt-0.5">
                    {latestOverride ? (
                      <span className="text-amber-700 font-bold">Đã sửa tay</span>
                    ) : (
                      "Nguyên bản"
                    )}
                  </div>
                </div>
              </>
            )}
          </div>

          {/* Lịch sử can thiệp trước đó nếu có */}
          {latestOverride && (
            <div className="rounded-lg bg-amber-50 border border-amber-200 p-2.5 text-xs text-amber-900 flex items-start gap-2">
              <History className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
              <div>
                <span className="font-semibold text-amber-950">Lịch sử sửa tay gần nhất:</span>{" "}
                Trường <code>{latestOverride.field}</code> = <strong>{latestOverride.value}</strong>{" "}
                (Lý do: <em>"{latestOverride.reason}"</em> lúc{" "}
                {new Date(latestOverride.createdAt).toLocaleString("vi-VN")})
              </div>
            </div>
          )}

          {/* Toggle xem JSON thô */}
          <div className="pt-1">
            <button
              type="button"
              onClick={() => setShowRawJson(!showRawJson)}
              className="flex items-center gap-1.5 text-xs text-muted hover:text-foreground font-medium transition-colors"
            >
              <Code2 className="h-3.5 w-3.5" />
              <span>{showRawJson ? "Ẩn chi tiết JSON thô" : "Xem chi tiết JSON thô"}</span>
              {showRawJson ? (
                <ChevronUp className="h-3.5 w-3.5" />
              ) : (
                <ChevronDown className="h-3.5 w-3.5" />
              )}
            </button>
            {showRawJson && (
              <pre className="mt-2 p-3 bg-slate-900 text-slate-100 rounded-lg font-mono text-[11px] max-h-36 overflow-y-auto leading-relaxed border border-slate-800">
                {JSON.stringify(payload, null, 2)}
              </pre>
            )}
          </div>
        </div>

        {/* 2. CHỌN TRƯỜNG DỮ LIỆU CẦN SỬA */}
        <div className="space-y-1.5">
          <label className="block text-xs font-semibold text-foreground uppercase tracking-wide">
            Trường dữ liệu cần can thiệp <span className="text-danger">*</span>
          </label>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <select
              value={field}
              onChange={(e) => {
                setField(e.target.value);
                const currentVal = payload[e.target.value];
                if (currentVal !== undefined) setValue(String(currentVal));
              }}
              className="w-full rounded-lg border border-border bg-white px-3 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary shadow-xs transition-colors"
            >
              {fieldOptions.map((opt) => (
                <option key={opt.key} value={opt.key}>
                  {opt.label}
                </option>
              ))}
            </select>

            {field === "custom" && (
              <input
                type="text"
                placeholder="Nhập tên trường JSON (vd: attendanceRate)..."
                value={customFieldName}
                onChange={(e) => setCustomFieldName(e.target.value)}
                className="w-full rounded-lg border border-border bg-white px-3 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary shadow-xs font-mono"
                required
              />
            )}
          </div>
        </div>

        {/* 3. NHẬP GIÁ TRỊ MỚI */}
        <div className="space-y-1.5">
          <label className="block text-xs font-semibold text-foreground uppercase tracking-wide">
            Giá trị mới (New Value) <span className="text-danger">*</span>
          </label>
          <input
            type="text"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            placeholder="Ví dụ: 9.8 hoặc Đạt..."
            className="w-full rounded-lg border border-border bg-white px-3.5 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary shadow-xs font-medium"
            required
          />
        </div>

        {/* 4. LÝ DO SỬA ĐỔI */}
        <div className="space-y-1.5">
          <label className="block text-xs font-semibold text-foreground uppercase tracking-wide">
            Lý do can thiệp sửa đổi (Bắt buộc phục vụ Audit Log) <span className="text-danger">*</span>
          </label>
          <textarea
            rows={3}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Ví dụ: Phúc khảo điểm thi môn Java Core theo Quyết định số 24/QĐ-ĐT ngày 20/11/2026..."
            className="w-full rounded-lg border border-border bg-white px-3.5 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary shadow-xs leading-relaxed"
            required
          />
        </div>

        {/* 5. KHỐI CẢNH BÁO KIỂM TOÁN */}
        <div className="rounded-xl border border-amber-200 bg-amber-50/80 p-3.5 text-xs text-amber-900 flex items-start gap-3">
          <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
          <div className="leading-relaxed">
            <strong className="font-semibold text-amber-950">Lưu ý quan trọng:</strong>{" "}
            Thông tin sau khi sửa tay sẽ được lưu vào hệ thống và ghi vết kiểm toán. Khi chạy đồng bộ lại từ LMS / EXAM / PM, trường này sẽ{" "}
            <span className="font-bold underline text-amber-950">không bị ghi đè</span> bởi dữ liệu nguồn, các trường khác vẫn cập nhật bình thường.
          </div>
        </div>

        {/* 6. NÚT HÀNH ĐỘNG */}
        <div className="flex items-center justify-end gap-3 pt-2 border-t border-border-subtle">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={loading}
          >
            Hủy bỏ
          </Button>
          <Button
            type="submit"
            variant="primary"
            disabled={loading}
            isLoading={loading}
            className="shadow-xs font-semibold"
          >
            {loading ? "Đang lưu..." : "Xác nhận ghi đè"}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
