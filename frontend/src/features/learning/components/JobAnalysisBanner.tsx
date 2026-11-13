import React from "react";
import { Target, ArrowLeft, BookmarkCheck } from "lucide-react";
import { Button } from "../../../components/common/Button";
import type { TargetInfo } from "../../../api/endpoints/learning.api";

interface JobAnalysisBannerProps {
  target: TargetInfo;
  isSavingTarget: boolean;
  onSaveAsMyTarget: () => void;
  onClearOverride: () => void;
}

export const JobAnalysisBanner: React.FC<JobAnalysisBannerProps> = ({
  target,
  isSavingTarget,
  onSaveAsMyTarget,
  onClearOverride,
}) => {
  return (
    <div className="rounded-xl border border-blue-200 bg-blue-50/70 p-4 sm:p-5 shadow-xs transition-all">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-start gap-3">
          <div className="p-2 rounded-lg bg-blue-100 text-blue-700 shrink-0 mt-0.5">
            <Target className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-xs font-semibold uppercase tracking-wider text-blue-700 bg-blue-100/80 px-2 py-0.5 rounded">
                Chế độ phân tích theo Tin tuyển dụng (JD)
              </span>
            </div>
            <h3 className="text-base font-bold text-slate-900 mt-1">
              {target.name}
            </h3>
            <p className="text-xs text-slate-600 mt-0.5">
              {target.description || "So sánh khoảng cách năng lực dựa trên điểm sàn tối thiểu của tin tuyển dụng này"}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
          <Button
            variant="outline"
            size="sm"
            onClick={onClearOverride}
            leftIcon={<ArrowLeft className="h-4 w-4" />}
            className="border-slate-300 hover:bg-white"
          >
            Quay về mục tiêu đã lưu
          </Button>

          <Button
            variant="primary"
            size="sm"
            onClick={onSaveAsMyTarget}
            isLoading={isSavingTarget}
            leftIcon={<BookmarkCheck className="h-4 w-4" />}
          >
            Đặt làm mục tiêu chính
          </Button>
        </div>
      </div>
    </div>
  );
};
