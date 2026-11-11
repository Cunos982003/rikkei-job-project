import { useNavigate } from "react-router-dom";
import { ShieldAlert, ArrowLeft, LogOut } from "lucide-react";
import { useAuthStore } from "../../store/useAuthStore";
import { Button } from "../../components/common/Button";
import { Badge } from "../../components/common/Badge";

export function UnauthorizedPage() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center p-6 text-center">
      <div className="w-16 h-16 rounded-2xl bg-rose-50 border border-rose-200 text-danger flex items-center justify-center mb-5 shadow-xs">
        <ShieldAlert className="h-8 w-8" />
      </div>

      <div className="inline-flex items-center gap-2 mb-3">
        <Badge variant="danger" size="md">
          Mã lỗi 403: Không có quyền truy cập
        </Badge>
      </div>

      <h1 className="text-2xl sm:text-3xl font-bold text-foreground mb-2">
        Bạn không có quyền truy cập trang này
      </h1>

      <p className="text-xs sm:text-sm text-muted max-w-md leading-relaxed mb-6">
        Tài khoản hiện tại của bạn{" "}
        {user ? (
          <span className="font-semibold text-foreground">
            ({user.email} - Vai trò: {user.role})
          </span>
        ) : null}{" "}
        không nằm trong danh sách được phép truy cập vào chức năng hoặc phân hệ này.
      </p>

      <div className="flex flex-wrap items-center justify-center gap-3">
        <Button
          variant="outline"
          leftIcon={<ArrowLeft className="h-4 w-4" />}
          onClick={() => navigate("/")}
        >
          Về trang chủ
        </Button>
        <Button
          variant="danger"
          leftIcon={<LogOut className="h-4 w-4" />}
          onClick={handleLogout}
        >
          Đăng nhập tài khoản khác
        </Button>
      </div>
    </div>
  );
}

