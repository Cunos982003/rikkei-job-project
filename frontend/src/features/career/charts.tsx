import type { JobRecommendation } from "../../api/endpoints/career.api";

function formatRoleTitle(title?: string): string | null {
  if (!title || !title.trim()) return null;
  return title.replace(/\s*#\d+\s*$/, "").trim();
}

function normalizeRoleKey(title?: string): string | null {
  const formatted = formatRoleTitle(title);
  return formatted ? formatted.toLowerCase() : null;
}

export function ScoreChart({ recommendations }: { recommendations: JobRecommendation[] }) {
  const sorted = [...recommendations].sort((a, b) => b.score - a.score);
  const seenRoles = new Set<string>();
  const pickedIds = new Set<string>();
  const diverse: JobRecommendation[] = [];

  for (const item of sorted) {
    if (diverse.length >= 7) break;
    const key = normalizeRoleKey(item.jobTitle);
    if (!key || !seenRoles.has(key)) {
      if (key) seenRoles.add(key);
      diverse.push(item);
      pickedIds.add(item.jobId);
    }
  }
  if (diverse.length < 7) {
    for (const item of sorted) {
      if (diverse.length >= 7) break;
      if (!pickedIds.has(item.jobId)) {
        diverse.push(item);
        pickedIds.add(item.jobId);
      }
    }
  }
  diverse.sort((a, b) => b.score - a.score);

  return (
    <div className="space-y-3" aria-label="Điểm phù hợp theo vị trí">
      {diverse.map((item) => {
        const displayName = formatRoleTitle(item.jobTitle) || item.jobId;
        return (
          <div key={item.jobId} className="flex items-center gap-3 text-xs">
            <span
              className="w-36 sm:w-48 truncate text-slate-400 font-medium hover:text-slate-200 transition-colors"
              title={displayName}
            >
              {displayName}
            </span>
            <div className="h-3 flex-1 overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-primary"
                style={{ width: `${Math.min(100, Math.max(0, item.score))}%` }}
              />
            </div>
            <span className="w-10 text-right font-semibold">{Math.round(item.score)}%</span>
          </div>
        );
      })}
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
