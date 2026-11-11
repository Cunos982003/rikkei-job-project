import { useNavigate } from "react-router-dom";
import { Compass, Home } from "lucide-react";
import { Button } from "../../components/common/Button";

export function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center p-6 text-center">
      <div className="w-16 h-16 rounded-2xl bg-slate-100 border border-slate-200 text-muted flex items-center justify-center mb-5">
        <Compass className="h-8 w-8" />
      </div>

      <h1 className="text-4xl font-extrabold text-foreground mb-2">404</h1>
      <h2 className="text-xl font-semibold text-foreground mb-2">
        Không tìm thấy trang yêu cầu
      </h2>

      <p className="text-xs sm:text-sm text-muted max-w-sm leading-relaxed mb-6">
        Đường dẫn bạn vừa truy cập có thể đã bị thay đổi, bị xóa hoặc không tồn tại trong hệ thống Rikkei Jobs.
      </p>

      <Button
        variant="primary"
        leftIcon={<Home className="h-4 w-4" />}
        onClick={() => navigate("/")}
      >
        Về trang chủ
      </Button>
    </div>
  );
}

