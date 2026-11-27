import { useMemo, useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import type { JobRecommendation } from "../../api/endpoints/career.api";
import { jobsApi, type JobPostingItem } from "../../api/endpoints/jobs.api";
import { useRecommendations } from "./hooks/useRecommendations";
import { useAuthStore } from "../../store/useAuthStore";

export interface CareerOutletContext {
  recommendations: JobRecommendation[];
  isLoading: boolean;
  isSuccess: boolean;
  error: unknown;
  refetch: () => void;
  limit: number;
  setLimit: (value: number) => void;
}

export function CareerLayout() {
  const [limit, setLimit] = useState(10);
  const token = useAuthStore((state) => state.accessToken);
  const userId = useAuthStore((state) => state.user?.id);
  const query = useRecommendations(limit);
  const authenticated = Boolean(token && userId);

  // Tra cứu thông tin việc làm để bổ sung jobTitle và companyName khi cần
  const { data: jobsRes } = useQuery({
    queryKey: ["allJobsForCareer"],
    queryFn: async () => {
      try {
        const res = await jobsApi.getJobs({ size: 100 });
        return res;
      } catch {
        return null;
      }
    },
    staleTime: 5 * 60 * 1000,
  });

  const jobMap = useMemo(() => {
    const map = new Map<string, { title: string; companyName: string }>();
    if (jobsRes?.data) {
      let items: JobPostingItem[] = [];
      if (Array.isArray(jobsRes.data)) {
        items = jobsRes.data;
      } else if ("items" in jobsRes.data && Array.isArray(jobsRes.data.items)) {
        items = jobsRes.data.items;
      }
      for (const j of items) {
        if (j.id) {
          map.set(j.id, { title: j.title, companyName: j.companyName });
        }
      }
    }
    return map;
  }, [jobsRes]);

  const enrichedRecommendations: JobRecommendation[] = useMemo(() => {
    const rawList = query.data ?? [];
    return rawList.map((item) => {
      const jobInfo = jobMap.get(item.jobId);
      return {
        ...item,
        jobTitle: item.jobTitle || jobInfo?.title,
        companyName: item.companyName || jobInfo?.companyName,
      };
    });
  }, [query.data, jobMap]);

  const context: CareerOutletContext = {
    recommendations: authenticated ? enrichedRecommendations : [],
    isLoading: authenticated && query.isPending,
    isSuccess: authenticated && query.isSuccess,
    error: authenticated ? query.error : new Error("Bạn cần đăng nhập để xem đề xuất."),
    refetch: () => {
      if (authenticated) void query.refetch();
    },
    limit,
    setLimit,
  };

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4 border-b border-border pb-4">
        <nav aria-label="AI Career" className="flex flex-wrap gap-2">
          {[
            { to: "/candidate/career", label: "Tổng quan", end: true },
            { to: "/candidate/career/assistant", label: "Rikkei Buddy", end: false },
            { to: "/candidate/career/recommendations", label: "Việc làm phù hợp", end: false },
            { to: "/candidate/career/skills", label: "Phân tích kỹ năng", end: false },
            { to: "/candidate/career/interviews", label: "Thực hành phỏng vấn", end: false },
          ].map(({ to, label, end }) => (
            <NavLink key={to} to={to} end={end} className={({ isActive }) =>
              `rounded-lg px-3 py-2 text-sm font-medium ${isActive ? "bg-primary-light text-primary" : "text-muted hover:bg-slate-100"}`
            }>
              {label}
            </NavLink>
          ))}
        </nav>
      </div>
      <Outlet context={context} />
    </div>
  );
}
