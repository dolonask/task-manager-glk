import { useState, type FormEvent } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";

export function LoginPage() {
  const { login, status } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (status === "authenticated") {
    const from = (location.state as { from?: string } | null)?.from ?? "/dashboard";
    return <Navigate to={from} replace />;
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(username, password);
      navigate("/dashboard", { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Не удалось выполнить вход");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      style={{
        width: "100vw",
        height: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "var(--color-bg)",
      }}
    >
      <form
        onSubmit={handleSubmit}
        className="card"
        style={{ width: 420, padding: "40px 36px" }}
      >
        <img
          src="/glk-logo.png"
          alt="GLK"
          style={{ height: 34, width: "auto", display: "block", marginBottom: 28, objectFit: "contain" }}
        />
        <div style={{ fontSize: 19, fontWeight: 700, letterSpacing: "-0.01em", marginBottom: 28 }}>
          Диспетчерская задач
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 14, marginBottom: 22 }}>
          <div>
            <div style={{ fontSize: 12, fontWeight: 600, color: "var(--color-text-secondary)", marginBottom: 6 }}>
              Логин
            </div>
            <div
              style={{
                display: "flex",
                alignItems: "stretch",
                border: "1px solid var(--color-border)",
                borderRadius: 5,
                overflow: "hidden",
              }}
            >
              <input
                type="text"
                placeholder="ivanov.i"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                style={{ flex: 1, border: "none", borderRadius: 0, outline: "none" }}
              />
              <div
                style={{
                  padding: "10px 12px",
                  background: "var(--color-bg)",
                  color: "var(--color-text-muted)",
                  fontSize: 14,
                  borderLeft: "1px solid var(--color-border)",
                  whiteSpace: "nowrap",
                }}
              >
                @glk.kg
              </div>
            </div>
          </div>
          <div>
            <div style={{ fontSize: 12, fontWeight: 600, color: "var(--color-text-secondary)", marginBottom: 6 }}>
              Пароль
            </div>
            <input
              type="password"
              placeholder="••••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              style={{ width: "100%" }}
            />
          </div>
        </div>

        {error && (
          <div
            style={{
              background: "var(--status-overdue-bg)",
              color: "var(--status-overdue-text)",
              borderRadius: 5,
              padding: "10px 12px",
              fontSize: 13,
              marginBottom: 16,
            }}
          >
            {error}
          </div>
        )}

        <button type="submit" className="btn-primary" style={{ width: "100%", padding: 12 }} disabled={submitting}>
          {submitting ? "Вход..." : "Войти"}
        </button>
      </form>
    </div>
  );
}
