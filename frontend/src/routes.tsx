import { Navigate, Route, Routes } from "react-router-dom";
import { RequireAuth } from "./auth/RequireAuth";
import { RequireRole } from "./auth/RequireRole";
import { LoginPage } from "./pages/LoginPage";
import { DashboardPage } from "./pages/DashboardPage";
import { TasksListPage } from "./pages/TasksListPage";
import { TaskDetailPage } from "./pages/TaskDetailPage";
import { TransferRequestsPage } from "./pages/TransferRequestsPage";
import { AnalyticsPage } from "./pages/AnalyticsPage";
import { AdminPage } from "./pages/AdminPage";
import { AuditLogPage } from "./pages/AuditLogPage";

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/dashboard" element={<RequireAuth><DashboardPage /></RequireAuth>} />
      <Route path="/tasks" element={<RequireAuth><TasksListPage /></RequireAuth>} />
      <Route path="/tasks/:id" element={<RequireAuth><TaskDetailPage /></RequireAuth>} />
      <Route path="/transfer-requests" element={<RequireAuth><TransferRequestsPage /></RequireAuth>} />
      <Route path="/analytics" element={<RequireAuth><AnalyticsPage /></RequireAuth>} />
      <Route
        path="/admin"
        element={
          <RequireAuth>
            <RequireRole roles={["admin"]}>
              <AdminPage />
            </RequireRole>
          </RequireAuth>
        }
      />
      <Route
        path="/audit-log"
        element={
          <RequireAuth>
            <RequireRole roles={["admin", "board"]}>
              <AuditLogPage />
            </RequireRole>
          </RequireAuth>
        }
      />
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
