import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { AppShell } from "../components/AppShell";
import { KpiCard } from "../components/KpiCard";
import { TaskStatusBadge } from "../components/StatusBadge";
import { analyticsApi } from "../api/analytics";
import { transferRequestsApi } from "../api/transferRequests";
import { useAuth } from "../auth/AuthContext";

export function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const summaryQuery = useQuery({ queryKey: ["analytics", "summary"], queryFn: () => analyticsApi.summary() });
  const registryQuery = useQuery({ queryKey: ["analytics", "registry"], queryFn: () => analyticsApi.registry() });

  const attentionStatus = user?.role === "board" ? "pending" : user?.role === "admin" ? "approved" : null;
  const attentionQuery = useQuery({
    queryKey: ["transfer-requests", "attention", attentionStatus],
    queryFn: () => transferRequestsApi.list({ status: attentionStatus! }),
    enabled: attentionStatus !== null,
  });

  const summary = summaryQuery.data;
  const upcoming = registryQuery.data?.upcoming.slice(0, 5) ?? [];
  const attention = attentionQuery.data ?? [];

  return (
    <AppShell title="Дашборд">
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 16, marginBottom: 20 }}>
        <KpiCard label="Всего задач" value={summary?.totalTasks ?? "—"} />
        <KpiCard label="Выполнено" value={summary?.doneTasks ?? "—"} />
        <KpiCard label="В работе" value={summary?.inProgressTasks ?? "—"} />
        <KpiCard label="Просрочено" value={summary?.overdueTasks ?? "—"} />
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1.3fr 1fr", gap: 16 }}>
        <div className="card">
          <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 14 }}>Ближайшие сроки</div>
          {upcoming.length === 0 && (
            <div style={{ color: "var(--color-text-muted)", fontSize: 13 }}>Нет задач с приближающимся сроком</div>
          )}
          {upcoming.map((task) => (
            <div
              key={task.id}
              onClick={() => navigate(`/tasks/${task.id}`)}
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                padding: "10px 0",
                borderTop: "1px solid var(--color-row-divider)",
                cursor: "pointer",
              }}
            >
              <div style={{ minWidth: 0 }}>
                <div style={{ fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                  {task.title}
                </div>
                <div style={{ fontSize: 12, color: "var(--color-text-muted)" }}>
                  {task.departmentName} · {task.assigneeName ?? "—"}
                </div>
              </div>
              <div style={{ display: "flex", alignItems: "center", gap: 10, flexShrink: 0 }}>
                <span style={{ fontSize: 12, color: "var(--color-text-secondary)" }}>{task.currentDeadline}</span>
                <TaskStatusBadge status={task.status} />
              </div>
            </div>
          ))}
        </div>

        <div className="card">
          <div style={{ fontWeight: 700, fontSize: 14, marginBottom: 14 }}>Требует внимания</div>
          {attentionStatus === null && (
            <div style={{ color: "var(--color-text-muted)", fontSize: 13 }}>Нет заявок, требующих действия</div>
          )}
          {attentionStatus !== null && attention.length === 0 && (
            <div style={{ color: "var(--color-text-muted)", fontSize: 13 }}>Нет заявок, требующих действия</div>
          )}
          {attention.map((tr) => (
            <div
              key={tr.id}
              onClick={() => navigate("/transfer-requests")}
              style={{
                padding: "10px 0",
                borderTop: "1px solid var(--color-row-divider)",
                cursor: "pointer",
              }}
            >
              <div style={{ fontWeight: 600 }}>{tr.taskTitle}</div>
              <div style={{ fontSize: 12, color: "var(--color-text-muted)" }}>
                {tr.departmentName} · {tr.currentDeadline} → {tr.proposedDeadline}
              </div>
            </div>
          ))}
        </div>
      </div>
    </AppShell>
  );
}
