import { useQuery } from "@tanstack/react-query";
import { matchApi } from "../../api/endpoints/match.api";
import { matchKeys } from "../../api/queryKeys";
import { Card, CardContent } from "../common/Card";
import { MatchScoreCard } from "./MatchScoreCard";

export interface MatchScorePanelProps {
  jobId: string;
  mode: "me" | "candidate";
  userId?: string;
  enabled?: boolean;
  compact?: boolean;
  title?: string;
}

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function MatchScorePanel({
  jobId,
  mode,
  userId,
  enabled = true,
  compact = false,
  title,
}: MatchScorePanelProps) {
  const canQuery = enabled && Boolean(jobId) && (mode === "me" || Boolean(userId));

  const query = useQuery({
    queryKey: mode === "me" ? matchKeys.me(jobId) : matchKeys.candidate(jobId, userId!),
    queryFn: async () => {
      const res = mode === "me"
        ? await matchApi.getMyMatch(jobId)
        : await matchApi.getCandidateMatch(jobId, userId!);
      return res.data;
    },
    enabled: canQuery && UUID_REGEX.test(jobId),
    retry: false,
    refetchOnWindowFocus: false,
    staleTime: 0,
  });

  if (!UUID_REGEX.test(jobId)) {
    return (
      <Card>
        <CardContent>
          <p className="rounded-lg border border-border-subtle bg-slate-50 p-3 text-xs text-muted">Chưa có dữ liệu độ khớp cho tin mẫu</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <MatchScoreCard
      result={query.data}
      isLoading={query.isLoading}
      isError={query.isError}
      onRetry={() => query.refetch()}
      title={title}
      compact={compact}
    />
  );
}

