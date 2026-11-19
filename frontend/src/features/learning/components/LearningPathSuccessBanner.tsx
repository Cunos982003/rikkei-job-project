import React from "react";
import { Trophy, Sparkles, CheckCircle2, ArrowRight, Briefcase } from "lucide-react";
import { useNavigate } from "react-router-dom";

interface LearningPathSuccessBannerProps {
  targetName: string;
  onExploreJobs?: () => void;
}

export const LearningPathSuccessBanner: React.FC<LearningPathSuccessBannerProps> = ({
  targetName,
  onExploreJobs,
}) => {
  const navigate = useNavigate();

  const handleExploreJobs = () => {
    if (onExploreJobs) {
      onExploreJobs();
    } else {
      navigate("/jobs");
    }
  };

  return (
    <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-emerald-600 via-teal-600 to-emerald-700 p-6 sm:p-8 text-white shadow-md">
      {/* Background decorative glow patterns */}
      <div className="absolute -top-12 -right-12 w-48 h-48 rounded-full bg-white/10 blur-2xl pointer-events-none" />
      <div className="absolute -bottom-10 -left-10 w-40 h-40 rounded-full bg-white/10 blur-xl pointer-events-none" />

      <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-6">
        {/* Left: Icon & Celebration message */}
        <div className="flex items-start gap-4 sm:gap-5 text-center md:text-left">
          <div className="w-14 h-14 sm:w-16 sm:h-16 rounded-2xl bg-white/20 backdrop-blur-md border border-white/30 flex items-center justify-center shrink-0 shadow-inner">
            <Trophy className="h-8 w-8 sm:h-9 sm:w-9 text-amber-300 drop-shadow-sm animate-bounce" />
          </div>

          <div className="space-y-1.5">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-white/20 border border-white/30 text-amber-200">
              <Sparkles className="h-3.5 w-3.5" />
              <span>SẴN SÀNG 100% ỨNG TUYỂN</span>
            </div>

            <h3 className="text-xl sm:text-2xl font-extrabold tracking-tight">
              Xuất sắc! Bạn đã hoàn thành toàn bộ lộ trình!
            </h3>

            <p className="text-sm sm:text-base text-emerald-100 max-w-2xl leading-relaxed">
              Tất cả các kỹ năng cốt lõi cho mục tiêu{" "}
              <strong className="text-white font-bold underline decoration-amber-300 decoration-2">
                {targetName}
              </strong>{" "}
              đã được kiểm chứng và đạt chuẩn tuyển dụng. Đây là thời điểm vàng để ứng tuyển!
            </p>
          </div>
        </div>

        {/* Right: Action Buttons */}
        <div className="flex flex-col sm:flex-row md:flex-col lg:flex-row items-center gap-3 shrink-0 w-full md:w-auto">
          <button
            type="button"
            onClick={handleExploreJobs}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-white text-emerald-800 font-bold text-sm shadow-md hover:bg-emerald-50 active:scale-95 transition-all cursor-pointer"
          >
            <Briefcase className="h-4 w-4 text-emerald-700" />
            <span>Khám phá việc làm phù hợp</span>
            <ArrowRight className="h-4 w-4 text-emerald-700" />
          </button>

          <button
            type="button"
            onClick={() => navigate("/candidate/profile")}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-4 py-3 rounded-xl bg-emerald-700/60 hover:bg-emerald-700 text-white font-medium text-sm border border-white/30 backdrop-blur-xs transition-all cursor-pointer"
          >
            <CheckCircle2 className="h-4 w-4 text-amber-300" />
            <span>Xem hồ sơ năng lực</span>
          </button>
        </div>
      </div>
    </div>
  );
};
