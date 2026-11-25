import { Loader2 } from "lucide-react";

export function RouteLoadingFallback() {
  return (
    <div className="w-full py-12 flex flex-col items-center justify-center space-y-4 animate-in fade-in duration-200">
      <div className="p-3 bg-slate-100 rounded-2xl text-primary animate-spin">
        <Loader2 className="h-6 w-6" />
      </div>
      <div className="text-center space-y-1">
        <p className="text-xs font-semibold text-foreground tracking-wide">
          Đang tải trang...
        </p>
        <p className="text-[11px] text-muted">
          Vui lòng đợi trong giây lát
        </p>
      </div>

      {/* Subtle skeleton wireframe */}
      <div className="w-full max-w-3xl space-y-3 pt-6 opacity-60">
        <div className="h-8 bg-slate-200/70 rounded-lg w-1/4 animate-pulse" />
        <div className="h-4 bg-slate-200/50 rounded-md w-1/2 animate-pulse" />
        <div className="h-48 bg-slate-200/40 rounded-xl mt-4 animate-pulse" />
      </div>
    </div>
  );
}

export default RouteLoadingFallback;

