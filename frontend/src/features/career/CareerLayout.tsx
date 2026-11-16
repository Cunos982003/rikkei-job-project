import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { Button } from "../../components/common/Button";
import type { JobRecommendation } from "../../api/endpoints/career.api";
import { useRecommendations } from "./hooks/useRecommendations";
import { useAuthStore } from "../../store/useAuthStore";

export interface CareerOutletContext {
  recommendations: JobRecommendation[];
  isLoading: boolean;
  isSuccess: boolean;
  error: unknown;
  refetch: () => void;
}

export function CareerLayout() {
  const [limit, setLimit] = useState(10);
  const [draftLimit, setDraftLimit] = useState("10");
  const [limitError, setLimitError] = useState("");
  const token = useAuthStore((state) => state.accessToken);
  const userId = useAuthStore((state) => state.user?.id);
  const query = useRecommendations(limit);
  const authenticated = Boolean(token && userId);
  const context: CareerOutletContext = {
    recommendations: authenticated ? query.data ?? [] : [],
    isLoading: authenticated && query.isPending,
    isSuccess: authenticated && query.isSuccess,
    error: authenticated ? query.error : new Error("Bạn cần đăng nhập để xem đề xuất."),
    refetch: () => {
      if (authenticated) void query.refetch();
    },
  };

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4 border-b border-border pb-4">
        <nav aria-label="AI Career" className="flex flex-wrap gap-2">
          {[
            { to: "/candidate/career", label: "Tổng quan", end: true },
            { to: "/candidate/career/recommendations", label: "Việc làm phù hợp", end: false },
            { to: "/candidate/career/skills", label: "Phân tích kỹ năng", end: false },
          ].map(({ to, label, end }) => (
            <NavLink key={to} to={to} end={end} className={({ isActive }) =>
              `rounded-lg px-3 py-2 text-sm font-medium ${isActive ? "bg-primary-light text-primary" : "text-muted hover:bg-slate-100"}`
            }>
              {label}
            </NavLink>
          ))}
        </nav>
        <form className="flex items-end gap-2" onSubmit={(event) => {
          event.preventDefault();
          const value = Number(draftLimit);
          if (!Number.isInteger(value) || value < 1 || value > 50) {
            setLimitError("Nhập số nguyên từ 1 đến 50.");
            return;
          }
          setLimitError("");
          setLimit(value);
        }}>
          <div>
            <label htmlFor="career-limit" className="block text-xs text-muted">Số gợi ý</label>
            <input id="career-limit" type="number" min={1} max={50} value={draftLimit}
              onChange={(event) => setDraftLimit(event.target.value)} aria-invalid={Boolean(limitError)}
              aria-describedby={limitError ? "career-limit-error" : undefined}
              className="mt-1 w-20 rounded-lg border border-border bg-surface px-2 py-2 text-sm focus:border-primary focus:outline-none" />
            {limitError && <p id="career-limit-error" role="alert" className="text-xs text-danger">{limitError}</p>}
          </div>
          <Button type="submit" size="sm">Cập nhật</Button>
        </form>
      </div>
      <Outlet context={context} />
    </div>
  );
}
