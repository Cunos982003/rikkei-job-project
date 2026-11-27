import { RefreshCw } from "lucide-react";
import type { MatchResult, MatchSkillItem } from "../../api/endpoints/match.api";
import { Card, CardContent, CardHeader, CardTitle } from "../common/Card";
import { Badge } from "../common/Badge";
import { Button } from "../common/Button";
import { Skeleton } from "../feedback/Skeleton";

export interface MatchScoreCardProps {
  result?: MatchResult;
  isLoading?: boolean;
  isError?: boolean;
  onRetry?: () => void;
  title?: string;
  compact?: boolean;
}

function SkillBadge({ required }: { required: boolean }) {
  return <Badge variant={required ? "danger" : "primary"} size="sm" showIcon={false}>{required ? "Bắt buộc" : "Ưu tiên"}</Badge>;
}

function ProgressCircle({ score, compact = false }: { score: number; compact?: boolean }) {
  const radius = compact ? 36 : 48;
  const size = compact ? 88 : 116;
  const stroke = compact ? 8 : 10;
  const circumference = 2 * Math.PI * radius;
  const clamped = Math.max(0, Math.min(100, score));
  const offset = circumference - (clamped / 100) * circumference;
  const color = score >= 80 ? "#10b981" : score >= 50 ? "#f59e0b" : "#f43f5e";

  return (
    <div className="relative shrink-0" style={{ width: size, height: size }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke="currentColor" strokeWidth={stroke} className="text-slate-200" />
        <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke={color} strokeWidth={stroke} strokeLinecap="round" strokeDasharray={circumference} strokeDashoffset={offset} />
      </svg>
      <div className="absolute inset-0 flex items-center justify-center">
        <span className={compact ? "text-xl" : "text-2xl"} style={{ color, fontWeight: 800 }}>{score.toFixed(1)}%</span>
      </div>
    </div>
  );
}

function SkillMet({ item }: { item: MatchSkillItem }) {
  return (
    <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3">
      <div className="flex items-start justify-between gap-2">
        <p className="text-xs font-semibold text-emerald-900">{item.skill}</p>
        <SkillBadge required={item.required} />
      </div>
      <p className="mt-1.5 text-[11px] text-emerald-800">≥ {item.minScore} điểm · bạn: {item.candidateScore}</p>
    </div>
  );
}

function SkillMissing({ item }: { item: MatchSkillItem }) {
  return (
    <div className="rounded-lg border border-rose-200 bg-rose-50 p-3">
      <div className="flex items-start justify-between gap-2">
        <p className="text-xs font-semibold text-rose-900">{item.skill}</p>
        <SkillBadge required={item.required} />
      </div>
      <p className="mt-1.5 text-[11px] text-rose-800">thiếu {item.gap} điểm · cần {item.minScore}, bạn có {item.candidateScore}</p>
    </div>
  );
}

export function MatchScoreCard({
  result,
  isLoading = false,
  isError = false,
  onRetry,
  title = "Độ khớp kỹ năng",
  compact = false,
}: MatchScoreCardProps) {
  if (isLoading) {
    return (
      <Card>
        <CardHeader><Skeleton variant="text" className="h-5 w-40" /></CardHeader>
        <CardContent className="flex items-center gap-4">
          <Skeleton variant="circle" className="h-24 w-24" />
          <div className="flex-1 space-y-2"><Skeleton variant="text" /><Skeleton variant="text" className="w-2/3" /></div>
        </CardContent>
      </Card>
    );
  }

  if (isError) {
    return (
      <Card>
        <CardHeader><CardTitle className="text-base">{title}</CardTitle></CardHeader>
        <CardContent className="text-center">
          <div className="rounded-lg border border-rose-200 bg-rose-50 p-4 text-xs text-rose-800">Chưa tính được độ khớp lúc này</div>
          {onRetry && (
            <Button variant="outline" size="sm" className="mt-3" onClick={onRetry} leftIcon={<RefreshCw className="h-3.5 w-3.5" />}>Thử lại</Button>
          )}
        </CardContent>
      </Card>
    );
  }

  if (!result) return null;

  const met = result.met || [];
  const missing = result.missing || [];
  const noSkills = met.length === 0 && missing.length === 0;

  if (compact) {
    return (
      <Card>
        <CardHeader><CardTitle className="text-base">{title}</CardTitle></CardHeader>
        <CardContent>
          <div className="flex items-center gap-3">
            <ProgressCircle score={result.score} compact />
            <div>
              <p className="text-xs font-semibold text-foreground">{missing.length} kỹ năng còn thiếu</p>
              <p className="mt-1 text-[11px] text-muted">Dựa trên điểm đã xác thực</p>
            </div>
          </div>
          {result.belowMinimum && <p className="mt-3 rounded-lg border border-rose-200 bg-rose-50 p-2.5 text-[11px] text-rose-800">Có kỹ năng bắt buộc dưới 50% yêu cầu tối thiểu</p>}
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader><CardTitle className="text-base">{title}</CardTitle></CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-col items-center">
          <ProgressCircle score={result.score} />
          <p className="mt-1 text-[11px] text-muted">Dựa trên điểm đã xác thực</p>
        </div>

        {result.belowMinimum && (
          <div className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-xs font-medium text-rose-800">
            Có kỹ năng bắt buộc dưới 50% yêu cầu tối thiểu
          </div>
        )}

        {noSkills ? (
          <p className="rounded-lg border border-border-subtle bg-slate-50 p-3 text-xs text-muted">Tin tuyển dụng này chưa khai báo kỹ năng nên chưa tính được độ khớp</p>
        ) : (
          <>
            {met.length > 0 && (
              <div>
                <h4 className="mb-2 text-xs font-bold text-foreground">Kỹ năng đạt ({met.length})</h4>
                <div className="grid gap-2 sm:grid-cols-2">{met.map((item) => <SkillMet key={item.skillCode} item={item} />)}</div>
              </div>
            )}

            {missing.length > 0 && (
              <div>
                <h4 className="mb-2 text-xs font-bold text-foreground">Kỹ năng còn thiếu ({missing.length})</h4>
                <div className="grid gap-2 sm:grid-cols-2">{missing.map((item) => <SkillMissing key={item.skillCode} item={item} />)}</div>
              </div>
            )}

            {result.score === 0 && met.length === 0 && missing.length > 0 && (
              <p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs text-amber-800">
                Bạn chưa có điểm xác thực cho các kỹ năng này. Hãy hoàn thành bài đánh giá hoặc minh chứng để được tính điểm.
              </p>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
}
