import { Outlet, Link } from "react-router-dom";
import { CheckCircle2, ShieldCheck, Award, Sparkles, Building2, GraduationCap, Users } from "lucide-react";

const CURRENT_YEAR = new Date().getFullYear();

export function AuthLayout() {
  return (
    <div className="min-h-screen w-full flex flex-col lg:grid lg:grid-cols-12 bg-surface antialiased selection:bg-primary-light selection:text-primary">
      {/* CỘT TRÁI: Brand & Ecosystem Showcase (Toàn màn hình desktop 100vh, dark background phong cách Rikkei) */}
      <div className="hidden lg:flex lg:col-span-6 xl:col-span-7 flex-col justify-between min-h-screen bg-slate-950 text-white p-10 xl:p-14 relative overflow-hidden border-r border-slate-800/80">
        {/* Glow hiệu ứng trang trí nền */}
        <div className="absolute -top-32 -left-32 w-96 h-96 rounded-full bg-primary/25 blur-3xl pointer-events-none" />
        <div className="absolute -bottom-32 -right-32 w-[30rem] h-[30rem] rounded-full bg-primary/20 blur-3xl pointer-events-none" />
        <div className="absolute top-1/2 left-1/3 -translate-y-1/2 w-64 h-64 rounded-full bg-red-600/10 blur-3xl pointer-events-none" />

        {/* Top Header trong cột trái */}
        <div className="relative z-10 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-3 group">
            <div className="w-10 h-10 rounded-xl bg-primary flex items-center justify-center text-white font-bold text-lg shadow-md group-hover:scale-105 transition-transform">
              RJ
            </div>
            <div>
              <span className="font-bold text-xl text-white tracking-tight">
                Rikkei <span className="text-red-400">Jobs</span>
              </span>
              <span className="block text-[11px] text-slate-400 tracking-wider uppercase font-medium">
                Verified Skill Platform
              </span>
            </div>
          </Link>
          <span className="px-3 py-1 rounded-full bg-white/10 backdrop-blur-xs text-slate-300 text-xs font-medium border border-white/10">
            Hệ sinh thái Rikkei Education
          </span>
        </div>

        {/* Khối thông tin cốt lõi (Hero Value Propositions) */}
        <div className="relative z-10 my-auto py-10 max-w-xl space-y-8">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-primary/20 text-red-300 border border-primary/30 text-xs font-semibold">
            <Sparkles className="h-3.5 w-3.5" />
            Nền tảng tuyển dụng & việc làm thế hệ mới
          </div>

          <div className="space-y-4">
            <h1 className="text-3xl xl:text-4xl font-extrabold leading-tight tracking-tight text-white">
              Kết nối việc làm dựa trên{" "}
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-red-400 to-rose-300">
                Năng lực thực tế
              </span>{" "}
              đã được xác thực
            </h1>
            <p className="text-slate-300 text-sm xl:text-base leading-relaxed">
              Đồng bộ dữ liệu học tập từ Rikkei LMS, kết quả Khảo thí chuẩn hóa và dự án thực chiến IOC. Xóa bỏ hoàn toàn nỗi lo gian lận CV, tạo lập hồ sơ năng lực minh bạch cho nhà tuyển dụng.
            </p>
          </div>

          {/* 3 Thẻ tính năng nổi bật */}
          <div className="grid sm:grid-cols-3 gap-3 pt-2">
            <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 backdrop-blur-xs space-y-1.5">
              <div className="flex items-center gap-2 text-emerald-400 font-semibold text-xs">
                <CheckCircle2 className="h-4 w-4 shrink-0" />
                <span>Minh bạch 100%</span>
              </div>
              <p className="text-[11px] text-slate-400 leading-normal">
                Bằng chứng kỹ năng xác thực tự động từ LMS & Khảo thí
              </p>
            </div>

            <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 backdrop-blur-xs space-y-1.5">
              <div className="flex items-center gap-2 text-amber-400 font-semibold text-xs">
                <Award className="h-4 w-4 shrink-0" />
                <span>Matching Score</span>
              </div>
              <p className="text-[11px] text-slate-400 leading-normal">
                Tính điểm phù hợp tự động theo chuẩn yêu cầu JD
              </p>
            </div>

            <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 backdrop-blur-xs space-y-1.5">
              <div className="flex items-center gap-2 text-blue-400 font-semibold text-xs">
                <ShieldCheck className="h-4 w-4 shrink-0" />
                <span>Bảo mật tối đa</span>
              </div>
              <p className="text-[11px] text-slate-400 leading-normal">
                Mở khóa liên hệ theo quy trình xét duyệt bảo mật
              </p>
            </div>
          </div>

          {/* Thống kê mạng lưới */}
          <div className="pt-6 border-t border-slate-800/80 grid grid-cols-3 gap-4 text-slate-300 text-xs">
            <div className="flex items-center gap-2">
              <GraduationCap className="h-4 w-4 text-primary-300 shrink-0" />
              <span>10,000+ Học viên</span>
            </div>
            <div className="flex items-center gap-2">
              <Building2 className="h-4 w-4 text-primary-300 shrink-0" />
              <span>500+ Doanh nghiệp</span>
            </div>
            <div className="flex items-center gap-2">
              <Users className="h-4 w-4 text-primary-300 shrink-0" />
              <span>100% Mentor sát cánh</span>
            </div>
          </div>
        </div>

        {/* Footer trong cột trái */}
        <div className="relative z-10 flex items-center justify-between text-xs text-slate-500 pt-6 border-t border-slate-900">
          <span>© {CURRENT_YEAR} Rikkei Jobs Platform.</span>
          <span>Bản quyền thuộc Rikkei Education</span>
        </div>
      </div>

      {/* CỘT PHẢI: Form Container (Toàn bộ chiều cao màn hình, không còn dạng cửa sổ/hộp nổi ở giữa) */}
      <div className="flex-1 lg:col-span-6 xl:col-span-5 flex flex-col justify-between min-h-screen bg-surface px-6 sm:px-12 lg:px-16 xl:px-20 py-8 lg:py-12 overflow-y-auto">
        {/* Mobile Header (chỉ hiện trên màn hình nhỏ khi cột trái ẩn) */}
        <div className="flex items-center justify-between lg:hidden mb-8">
          <Link to="/" className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-primary flex items-center justify-center text-white font-bold text-base shadow-xs">
              RJ
            </div>
            <span className="font-bold text-lg text-foreground tracking-tight">
              Rikkei <span className="text-primary">Jobs</span>
            </span>
          </Link>
          <span className="text-xs text-muted">
            Rikkei Education
          </span>
        </div>

        {/* Desktop Header góc phải */}
        <div className="hidden lg:flex items-center justify-end text-xs text-muted">
          <span>Cổng thông tin xác thực năng lực IT</span>
        </div>

        {/* Vùng Form Outlet ở giữa */}
        <div className="my-auto py-6 w-full max-w-md mx-auto">
          <Outlet />
        </div>

        {/* Footer cột phải */}
        <footer className="mt-8 pt-4 border-t border-border-subtle text-center text-xs text-muted">
          Cần hỗ trợ đăng nhập? Liên hệ Ban Đào tạo hoặc Bộ phận Hướng nghiệp Rikkei Education.
        </footer>
      </div>
    </div>
  );
}
