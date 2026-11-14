import { Navigate, Outlet } from "react-router-dom";
import { useAuthStore, normalizeRole } from "../../store/useAuthStore";
import type { Role } from "../../api/endpoints/auth.api";

interface RoleGuardProps {
  allowedRoles: Role[];
  children?: React.ReactNode;
}

export function RoleGuard({ allowedRoles, children }: RoleGuardProps) {
  const { user, isAuthenticated } = useAuthStore();

  // Nếu chưa xác thực hoặc chưa có user, đợi (ProtectedRoute nên bắt case này trước)
  if (!isAuthenticated || !user) {
    return <Navigate to="/unauthorized" replace />;
  }

  const userRole = normalizeRole(user.role);
  const normalizedAllowedRoles = allowedRoles.map((r) => normalizeRole(r));

  if (!normalizedAllowedRoles.includes(userRole)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return children ? <>{children}</> : <Outlet />;
}
