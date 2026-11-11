const CURRENT_YEAR = new Date().getFullYear();

export function Footer() {
  return (
    <footer className="border-t border-border bg-surface py-5 px-4 sm:px-6 text-center sm:text-left text-xs text-muted">
      <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-3">
        <p>
          © {CURRENT_YEAR}{" "}
          <span className="font-semibold text-foreground">Rikkei Education</span>. Hệ sinh thái kết nối việc làm theo năng lực xác thực.
        </p>
        <div className="flex items-center gap-4 text-[11px]">
          <span className="hover:text-foreground cursor-pointer transition-colors">
            Quy chuẩn năng lực
          </span>
          <span className="hover:text-foreground cursor-pointer transition-colors">
            Bảo mật PII
          </span>
          <span className="hover:text-foreground cursor-pointer transition-colors">
            Trợ giúp
          </span>
        </div>
      </div>
    </footer>
  );
}
