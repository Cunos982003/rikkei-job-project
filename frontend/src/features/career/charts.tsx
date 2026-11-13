import type { JobRecommendation } from "../../api/endpoints/career.api";

export function ScoreChart({ recommendations }: { recommendations: JobRecommendation[] }) {
  return (
    <div className="space-y-3" aria-label="Điểm phù hợp theo vị trí">
      {recommendations.slice(0, 7).map((item) => (
        <div key={item.jobId} className="flex items-center gap-3 text-xs">
          <span className="w-28 truncate text-muted" title={item.jobId}>{item.jobId}</span>
          <div className="h-3 flex-1 overflow-hidden rounded-full bg-slate-100">
            <div className="h-full rounded-full bg-primary" style={{ width: `${Math.min(100, Math.max(0, item.score))}%` }} />
          </div>
          <span className="w-10 text-right font-semibold">{Math.round(item.score)}%</span>
        </div>
      ))}
    </div>
  );
}

export function SkillComparisonChart({ data }: {
  data: { skillName: string; chartLabel: string; verified: number; requiredIndex: number }[];
}) {
  return (
    <div className="space-y-4" aria-label="So sánh kỹ năng đã xác thực và mức yêu cầu">
      <p className="text-xs text-muted"><span className="text-primary">■</span> Đã xác thực &nbsp; <span className="text-slate-400">■</span> Mức yêu cầu</p>
      {data.map((skill) => (
        <div key={skill.skillName} className="text-xs">
          <p className="mb-1 font-medium" title={skill.skillName}>{skill.chartLabel}</p>
          {([
            ["Đã xác thực", skill.verified, "bg-primary"],
            ["Mức yêu cầu", skill.requiredIndex, "bg-slate-400"],
          ] as const).map(([label, value, color]) => (
            <div key={label} className="mt-1 flex items-center gap-2">
              <span className="sr-only">{label}</span>
              <div className="h-2 flex-1 overflow-hidden rounded-full bg-slate-100">
                <div className={`h-full rounded-full ${color}`} style={{ width: `${Math.min(100, Math.max(0, value))}%` }} />
              </div>
              <span className="w-8 text-right text-muted">{value}</span>
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}
