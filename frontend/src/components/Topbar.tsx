import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function Topbar({ title }: { title: string }) {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/login", { replace: true });
  };

  return (
    <div
      style={{
        height: "var(--topbar-height)",
        background: "var(--color-surface)",
        borderBottom: "1px solid var(--color-border)",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        padding: "0 28px",
      }}
    >
      <div style={{ fontSize: 17, fontWeight: 700 }}>{title}</div>
      <button className="btn-secondary" onClick={handleLogout}>
        Выйти
      </button>
    </div>
  );
}
