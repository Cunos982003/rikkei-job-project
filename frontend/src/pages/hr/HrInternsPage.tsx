import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Users,
  Star,
  Mail,
  Briefcase,
  Search,
  CheckCircle2,
  AlertCircle,
  GraduationCap,
} from "lucide-react";
import { internshipsApi } from "@/api/endpoints/internships.api";

export default function HrInternsPage() {
  const [searchKeyword, setSearchKeyword] = useState("");

  const {
    data: internsResponse,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ["hr", "interns"],
    queryFn: () => internshipsApi.getCompanyInterns(),
    staleTime: 60 * 1000,
  });

  const interns = internsResponse?.data ?? [];

  const filteredInterns = interns.filter((intern) => {
    const q = searchKeyword.trim().toLowerCase();
    if (!q) return true;
    return (
      intern.candidateName.toLowerCase().includes(q) ||
      intern.batchName.toLowerCase().includes(q) ||
      intern.mentorEmail.toLowerCase().includes(q)
    );
  });

  return (
    <div className="container mx-auto py-8 px-4 max-w-6xl space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-border pb-5">
        <div className="flex items-center gap-3">
          <div className="bg-primary/10 p-2.5 rounded-xl text-primary border border-primary/20">
            <Users className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Thực tập sinh của tôi</h1>
            <p className="text-muted text-sm mt-0.5">
              Theo dõi tiến độ, số lượng task và kết quả đánh giá thực tập của sinh viên tại doanh nghiệp.
            </p>
          </div>
        </div>

        {/* Search Input */}
        <div className="relative w-full sm:w-72">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted pointer-events-none" />
          <input
            type="text"
            placeholder="Tìm theo tên, đợt, mentor..."
            value={searchKeyword}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchKeyword(e.target.value)}
            className="w-full pl-9 pr-3 py-2 bg-surface text-sm text-foreground border border-input rounded-xl focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
          />
        </div>
      </div>

      {/* Main Table Card */}
      <Card className="shadow-2xs border-border overflow-hidden bg-surface">
        {/* Banner Header */}
        <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-primary-dark p-5 text-white">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold flex items-center gap-2">
              <Briefcase className="w-5 h-5 text-primary-light" /> Danh sách thực tập sinh hiện tại
            </h2>
            <Badge className="bg-white/15 text-white border border-white/20 hover:bg-white/25 text-xs px-2.5 py-1">
              Tổng số: {interns.length} sinh viên
            </Badge>
          </div>
        </div>

        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-6 space-y-3">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="flex items-center justify-between gap-4 py-2 border-b border-border/50">
                  <div className="flex items-center gap-3">
                    <Skeleton className="w-9 h-9 rounded-full" />
                    <div className="space-y-1.5">
                      <Skeleton className="h-4 w-32" />
                      <Skeleton className="h-3 w-48" />
                    </div>
                  </div>
                  <Skeleton className="h-6 w-20" />
                  <Skeleton className="h-6 w-16" />
                </div>
              ))}
            </div>
          ) : isError ? (
            <div className="p-10 text-center space-y-3">
              <AlertCircle className="w-10 h-10 text-danger mx-auto" />
              <p className="text-sm text-foreground font-medium">Không thể tải danh sách thực tập sinh</p>
              <button
                onClick={() => refetch()}
                className="px-4 py-1.5 text-xs bg-primary text-white rounded-lg hover:bg-primary-hover transition-colors"
              >
                Thử lại
              </button>
            </div>
          ) : filteredInterns.length === 0 ? (
            <div className="p-12 text-center space-y-3">
              <GraduationCap className="w-12 h-12 text-muted/50 mx-auto" />
              <p className="text-sm font-medium text-foreground">Không tìm thấy thực tập sinh phù hợp</p>
              <p className="text-xs text-muted">Vui lòng thử lại với từ khóa tìm kiếm khác.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="bg-slate-50/80 border-b border-border text-xs text-muted uppercase tracking-wider">
                  <tr>
                    <th className="py-3.5 px-4 font-semibold">Tên thực tập sinh</th>
                    <th className="py-3.5 px-4 font-semibold">Đợt thực tập</th>
                    <th className="py-3.5 px-4 font-semibold">Mentor hướng dẫn</th>
                    <th className="py-3.5 px-4 font-semibold text-center">Tasks nghiệm thu</th>
                    <th className="py-3.5 px-4 font-semibold text-center">Điểm đánh giá</th>
                    <th className="py-3.5 px-4 font-semibold text-center">Trạng thái</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/60">
                  {filteredInterns.map((intern) => (
                    <tr key={intern.id} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-2.5">
                          <div className="w-8 h-8 rounded-full bg-primary/10 text-primary border border-primary/20 flex items-center justify-center font-bold text-xs shrink-0">
                            {intern.candidateName.charAt(0)}
                          </div>
                          <div>
                            <span className="font-semibold text-foreground">{intern.candidateName}</span>
                            <p className="text-[11px] text-muted font-mono">{intern.id}</p>
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 text-foreground/80 font-medium">{intern.batchName}</td>
                      <td className="py-3.5 px-4 text-muted">
                        <div className="flex items-center gap-1.5">
                          <Mail className="w-3.5 h-3.5 text-muted/70 shrink-0" />
                          <span className="truncate max-w-[200px]">{intern.mentorEmail}</span>
                        </div>
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200 text-xs px-2 py-0.5">
                          {intern.acceptedTasksCount} tasks
                        </Badge>
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        <div className="inline-flex items-center gap-1 font-bold text-amber-600 bg-amber-50 border border-amber-200 px-2.5 py-0.5 rounded-full text-xs">
                          {intern.currentScore} <Star className="w-3.5 h-3.5 fill-amber-500 text-amber-500" />
                        </div>
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        {intern.status === "WARNING" ? (
                          <Badge variant="outline" className="bg-amber-50 text-amber-700 border-amber-300 text-[11px] px-2 py-0.5">
                            Cần nhắc nhở
                          </Badge>
                        ) : (
                          <Badge variant="outline" className="bg-emerald-50 text-emerald-700 border-emerald-300 text-[11px] px-2 py-0.5 inline-flex items-center gap-1">
                            <CheckCircle2 className="w-3 h-3" /> Đang thực tập
                          </Badge>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
