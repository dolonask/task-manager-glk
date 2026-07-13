import { NavLink } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const roleLabels: Record<string, string> = {
  admin: "Администратор",
  board: "Член Правления",
  head: "Начальник СП",
  employee: "Сотрудник СП",
  observer: "Наблюдатель",
};

const navItemStyle = (active: boolean) => ({
  display: "block",
  padding: "10px 12px",
  borderRadius: 5,
  fontSize: 13,
  fontWeight: 600,
  color: active ? "#fff" : "var(--sidebar-text-inactive)",
  background: active ? "var(--sidebar-active-bg)" : "transparent",
});

export function Sidebar() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <div
      style={{
        width: "var(--sidebar-width)",
        minWidth: "var(--sidebar-width)",
        height: "100vh",
        background: "var(--sidebar-bg)",
        color: "var(--sidebar-text)",
        padding: "22px 16px",
        display: "flex",
        flexDirection: "column",
        position: "sticky",
        top: 0,
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 28, padding: "0 8px" }}>
        <img src="/glk-logo.png" alt="GLK" style={{ height: 26, width: "auto", objectFit: "contain" }} />
        <div style={{ fontSize: 11, fontWeight: 700, letterSpacing: "0.04em", textTransform: "uppercase", color: "var(--sidebar-text-inactive)" }}>
          Диспетчерская задач
        </div>
      </div>

      <nav style={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <NavLink to="/dashboard" style={({ isActive }) => navItemStyle(isActive)}>
          Дашборд
        </NavLink>
        <NavLink to="/tasks" style={({ isActive }) => navItemStyle(isActive)}>
          Задачи
        </NavLink>
        <NavLink to="/transfer-requests" style={({ isActive }) => navItemStyle(isActive)}>
          Заявки на перенос срока
        </NavLink>
        <NavLink to="/analytics" style={({ isActive }) => navItemStyle(isActive)}>
          Аналитика
        </NavLink>
        {user.role === "admin" && (
          <NavLink to="/admin" style={({ isActive }) => navItemStyle(isActive)}>
            Пользователи и СП
          </NavLink>
        )}
        {(user.role === "admin" || user.role === "board") && (
          <NavLink to="/audit-log" style={({ isActive }) => navItemStyle(isActive)}>
            Журнал аудита
          </NavLink>
        )}
      </nav>

      <div style={{ flex: 1 }} />

      <div style={{ borderTop: "1px solid #3D3527", paddingTop: 14 }}>
        <div style={{ fontSize: 13, fontWeight: 700, color: "#fff" }}>{user.fullName}</div>
        <div style={{ fontSize: 12, color: "var(--sidebar-text-inactive)" }}>{roleLabels[user.role]}</div>
      </div>
    </div>
  );
}
