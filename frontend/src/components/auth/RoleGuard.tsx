import { Navigate, Outlet } from "react-router-dom";
import { useAuthStore, normalizeRole } from "../../store/useAuthStore";
import type { Role } from "../../api/endpoints/auth.api";

interface RoleGuardProps {
  allowedRoles: Role[];
  children?: React.ReactNode;
}

export function RoleGuard({ allowedRoles, children }: RoleGuardProps) {
  const { user } = useAuthStore();

  const userRole = normalizeRole(user?.role);
  const normalizedAllowedRoles = allowedRoles.map((r) => normalizeRole(r));

  if (!user || !normalizedAllowedRoles.includes(userRole)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return children ? <>{children}</> : <Outlet />;
}
