import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "sonner";
import {
  UserPlus,
  ChevronDown,
  Building2,
  CalendarDays,
  Users,
  CheckCircle2,
  AlertCircle,
  Clock,
  Layers,
} from "lucide-react";
import { internshipsApi } from "@/api/endpoints/internships.api";

export default function StaffPlacementPage() {
  const queryClient = useQueryClient();
  const [selections, setSelections] = useState<Record<string, string>>({});

  const {
    data: demandsResponse,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ["staff", "placements"],
    queryFn: () => internshipsApi.getPlacementDemands(),
    staleTime: 60 * 1000,
  });

  const demands = demandsResponse?.data ?? [];

  const assignMutation = useMutation({
    mutationFn: (payload: { userId: string; jobId: string; batchName: string }) =>
      internshipsApi.assignPlacement(payload),
    onSuccess: (res, variables) => {
      toast.success(res.message || `Đã ghép thành công ứng viên vào ${variables.batchName}`);
      // Xóa selection của ứng viên vừa ghép
      setSelections((prev) => {
        const next = { ...prev };
        delete next[variables.userId];
        return next;
      });
      queryClient.invalidateQueries({ queryKey: ["staff", "placements"] });
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || "Không thể ghép đợt thực tập cho ứng viên";
      toast.error(msg);
    },
  });

  const handlePlace = (userId: string, jobId: string, candidateName: string) => {
    const batchName = selections[userId];
    if (!batchName) {
      toast.warning(`Vui lòng chọn đợt thực tập cho ứng viên ${candidateName} trước khi ghép!`);
      return;
    }
    assignMutation.mutate({ userId, jobId, batchName });
  };

  const totalPendingCandidates = demands.reduce(
    (acc, cur) => acc + (cur.hiredCandidates?.length || 0),
    0
  );

  return (
    <div className="container mx-auto py-8 px-4 max-w-6xl space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-border pb-5">
        <div className="flex items-center gap-3">
          <div className="bg-primary/10 p-2.5 rounded-xl text-primary border border-primary/20">
            <UserPlus className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Ghép đợt thực tập IOC</h1>
            <p className="text-muted text-sm mt-0.5">
              Phân bổ ứng viên đã trúng tuyển vào các đợt thực tập phù hợp theo yêu cầu của doanh nghiệp.
            </p>
          </div>
        </div>

        <Badge variant="outline" className="bg-primary-light text-primary border-primary-border px-3 py-1 text-xs self-start sm:self-auto">
          {totalPendingCandidates} ứng viên đang chờ ghép đợt
        </Badge>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          {[1, 2].map((i) => (
            <Card key={i} className="p-6 space-y-4">
              <Skeleton className="h-6 w-1/3" />
              <Skeleton className="h-4 w-1/2" />
              <div className="space-y-2 pt-2">
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
              </div>
            </Card>
          ))}
        </div>
      ) : isError ? (
        <div className="p-10 text-center space-y-3 bg-surface rounded-2xl border border-border">
          <AlertCircle className="w-10 h-10 text-danger mx-auto" />
          <p className="text-sm text-foreground font-medium">Không thể tải danh sách nhu cầu thực tập</p>
          <button
            onClick={() => refetch()}
            className="px-4 py-1.5 text-xs bg-primary text-white rounded-lg hover:bg-primary-hover transition-colors"
          >
            Thử lại
          </button>
        </div>
      ) : demands.length === 0 || totalPendingCandidates === 0 ? (
        <Card className="p-12 text-center space-y-3 bg-surface border-border">
          <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto" />
          <p className="text-base font-semibold text-foreground">Tất cả ứng viên đã được ghép đợt thực tập</p>
          <p className="text-xs text-muted">Hiện không có ứng viên nào đang trong hàng chờ phân bổ.</p>
        </Card>
      ) : (
        <div className="space-y-6">
          {demands.map((demand) => {
            const candidates = demand.hiredCandidates || [];
            if (candidates.length === 0) return null;

            return (
              <Card key={demand.jobId} className="shadow-2xs border-border overflow-hidden bg-surface">
                {/* Header Card */}
                <CardHeader className="bg-gradient-to-r from-slate-900 via-slate-800 to-primary-dark text-white p-5">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                      <CardTitle className="text-base font-semibold flex items-center gap-2">
                        <Building2 className="w-4 h-4 text-primary-light" />
                        {demand.jobTitle}
                      </CardTitle>
                      <p className="text-xs text-slate-300 mt-1 flex items-center gap-2">
                        <span>{demand.companyName}</span>
                        <span>•</span>
                        <span className="inline-flex items-center gap-1">
                          <CalendarDays className="w-3.5 h-3.5 text-slate-400" /> {demand.durationWeeks} tuần
                        </span>
                      </p>
                    </div>
                    <div className="bg-white/10 border border-white/15 px-3 py-1 rounded-lg text-xs font-medium backdrop-blur-sm flex items-center gap-1.5 self-start sm:self-auto">
                      <Users className="w-3.5 h-3.5 text-primary-light" />
                      {candidates.length} ứng viên chờ ghép
                    </div>
                  </div>
                </CardHeader>

                <CardContent className="p-0">
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm text-left">
                      <thead className="bg-slate-50/80 border-b border-border text-xs text-muted uppercase tracking-wider">
                        <tr>
                          <th className="py-3 px-4 font-semibold">Tên ứng viên</th>
                          <th className="py-3 px-4 font-semibold">Mã đơn trúng tuyển</th>
                          <th className="py-3 px-4 font-semibold">Chọn đợt thực tập</th>
                          <th className="py-3 px-4 font-semibold text-right">Thao tác</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-border/60">
                        {candidates.map((candidate) => (
                          <tr key={candidate.userId} className="hover:bg-slate-50/70 transition-colors">
                            <td className="py-3.5 px-4">
                              <span className="font-semibold text-foreground">{candidate.candidateName}</span>
                            </td>
                            <td className="py-3.5 px-4">
                              <span className="font-mono text-xs text-muted bg-slate-100 px-2 py-0.5 rounded border border-slate-200">
                                {candidate.applicationId}
                              </span>
                            </td>
                            <td className="py-3.5 px-4">
                              <div className="relative w-full max-w-[280px]">
                                <select
                                  className="w-full appearance-none bg-surface border border-input rounded-lg p-2 pr-8 text-xs text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all cursor-pointer"
                                  value={selections[candidate.userId] || ""}
                                  onChange={(e) =>
                                    setSelections((prev) => ({
                                      ...prev,
                                      [candidate.userId]: e.target.value,
                                    }))
                                  }
                                >
                                  <option value="">-- Chọn đợt thực tập --</option>
                                  {demand.preferredBatch && (
                                    <option value={demand.preferredBatch}>
                                      {demand.preferredBatch} (Khuyến nghị)
                                    </option>
                                  )}
                                  <option value="Đợt thực tập IOC K20">Đợt thực tập IOC K20</option>
                                  <option value="Đợt thực tập IOC K21">Đợt thực tập IOC K21</option>
                                  <option value="Đợt thực tập dự phòng">Đợt thực tập dự phòng</option>
                                </select>
                                <ChevronDown className="absolute right-2.5 top-2.5 w-3.5 h-3.5 text-muted pointer-events-none" />
                              </div>
                            </td>
                            <td className="py-3.5 px-4 text-right">
                              <button
                                onClick={() => handlePlace(candidate.userId, demand.jobId, candidate.candidateName)}
                                disabled={assignMutation.isPending}
                                className="px-4 py-1.5 bg-primary text-white rounded-lg text-xs font-medium hover:bg-primary-hover transition-colors shadow-2xs disabled:opacity-50 inline-flex items-center gap-1.5"
                              >
                                {assignMutation.isPending ? (
                                  <Clock className="w-3.5 h-3.5 animate-spin" />
                                ) : (
                                  <Layers className="w-3.5 h-3.5" />
                                )}
                                Ghép đợt
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
