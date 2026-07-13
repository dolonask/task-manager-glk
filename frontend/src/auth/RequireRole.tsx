import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import type { Role } from "../api/types";
import { useAuth } from "./AuthContext";

export function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { user } = useAuth();
  if (!user || !roles.includes(user.role)) {
    return <Navigate to="/dashboard" replace />;
  }
  return <>{children}</>;
}
