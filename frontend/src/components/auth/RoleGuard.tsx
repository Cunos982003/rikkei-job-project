import { Navigate, Outlet } from "react-router-dom";
import { useAuthStore } from "../../store/useAuthStore";
import type { Role } from "../../api/endpoints/auth.api";

interface RoleGuardProps {
  allowedRoles: Role[];
  children?: React.ReactNode;
}

export function RoleGuard({ allowedRoles, children }: RoleGuardProps) {
  const { user, isAuthenticated } = useAuthStore();

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (!allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" replace state={{ allowedRoles, userRole: user.role }} />;
  }

  return children ? <>{children}</> : <Outlet />;
}

