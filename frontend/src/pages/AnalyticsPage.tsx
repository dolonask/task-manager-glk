import { useQuery } from "@tanstack/react-query";
import { AppShell } from "../components/AppShell";
import { KpiCard } from "../components/KpiCard";
import { ProgressBar } from "../components/ProgressBar";
import { TaskStatusBadge } from "../components/StatusBadge";
import { analyticsApi } from "../api/analytics";
import { useAuth } from "../auth/AuthContext";

export function AnalyticsPage() {
  const { user } = useAuth();
  const canSeeBoardAnalytics = user?.role === "admin" || user?.role === "board";

  const summaryQuery = useQuery({ queryKey: ["analytics", "summary"], queryFn: () => analyticsApi.summary() });
  const departmentsQuery = useQuery({ queryKey: ["analytics", "departments"], queryFn: analyticsApi.departments });
  const registryQuery = useQuery({ queryKey: ["analytics", "registry"], queryFn: () => analyticsApi.registry() });
  const boardMembersQuery = useQuery({
    queryKey: ["analytics", "board-members"],
    queryFn: analyticsApi.boardMembers,
    enabled: canSeeBoardAnalytics,
  });
  const transferStatsQuery = useQuery({
    queryKey: ["analytics", "transfer-requests"],
    queryFn: analyticsApi.transferRequests,
    enabled: canSeeBoardAnalytics,
  });

  const summary = summaryQuery.data;
  const registry = [...(registryQuery.data?.upcoming ?? []), ...(registryQuery.data?.overdue ?? [])].sort((a, b) =>
    a.currentDeadline.localeCompare(b.currentDeadline),
  );

  return (
    <AppShell title="Аналитика">
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 16, marginBottom: 20 }}>
        <KpiCard label="Процент исполнения" value={summary ? `${Math.round(summary.completionRate * 100)}%` : "—"} size={26} />
        <KpiCard label="Соблюдение сроков" value={summary ? `${Math.round(summary.onTimeRate * 100)}%` : "—"} size={26} />
        <KpiCard label="Всего заявок на перенос" value={transferStatsQuery.data?.total ?? "—"} size={26} />
        <KpiCard label="Просрочено задач" value={summary?.overdueTasks ?? "—"} size={26} />
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginBottom: 16 }}>
        <div className="card">
          <div style={{ fontWeight: 700, marginBottom: 14 }}>Нагрузка по структурным подразделениям</div>
          {departmentsQuery.data?.map((d) => (
            <div key={d.departmentId} style={{ padding: "8px 0", borderTop: "1px solid var(--color-row-divider)" }}>
              <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13, marginBottom: 6 }}>
                <span>{d.departmentName}</span>
                <span style={{ color: "var(--color-text-muted)" }}>
                  {d.doneTasks}/{d.totalTasks} · просрочено {d.overdueTasks}
                </span>
              </div>
              <ProgressBar value={d.completionRate} height={8} />
            </div>
          ))}
        </div>

        <div className="card">
          <div style={{ fontWeight: 700, marginBottom: 14 }}>Задачи по членам Правления</div>
          {!canSeeBoardAnalytics && (
            <div style={{ color: "var(--color-text-muted)", fontSize: 13 }}>Доступно членам Правления и администратору</div>
          )}
          {boardMembersQuery.data?.map((bm) => (
            <div key={bm.boardMemberId} style={{ padding: "8px 0", borderTop: "1px solid var(--color-row-divider)", fontSize: 13 }}>
              <div style={{ fontWeight: 600, marginBottom: 4 }}>{bm.fullName}</div>
              <div style={{ color: "var(--color-text-muted)" }}>
                поставлено {bm.totalTasks} · выполнено {bm.statusBreakdown.done ?? 0} · просрочено {bm.statusBreakdown.overdue ?? 0}
              </div>
            </div>
          ))}
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1.4fr 1fr", gap: 16 }}>
        <div className="card" style={{ padding: 0, overflow: "hidden" }}>
          <div style={{ fontWeight: 700, padding: "18px 20px 0" }}>Реестр ближайших сроков и просрочек</div>
          <table>
            <thead>
              <tr>
                <th>Задача</th>
                <th>СП</th>
                <th>Срок</th>
                <th>Статус</th>
              </tr>
            </thead>
            <tbody>
              {registry.map((t) => (
                <tr key={t.id}>
                  <td>{t.title}</td>
                  <td>{t.departmentName}</td>
                  <td>{t.currentDeadline}</td>
                  <td>
                    <TaskStatusBadge status={t.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="card">
          <div style={{ fontWeight: 700, marginBottom: 14 }}>Заявки на перенос срока</div>
          {canSeeBoardAnalytics && transferStatsQuery.data ? (
            <div style={{ display: "flex", flexDirection: "column", gap: 8, fontSize: 13, marginBottom: 16 }}>
              <StatRow label="Всего заявок" value={String(transferStatsQuery.data.total)} />
              <StatRow label="% согласовано" value={`${Math.round(transferStatsQuery.data.approvedRate * 100)}%`} />
              <StatRow label="% отклонено" value={`${Math.round(transferStatsQuery.data.rejectedRate * 100)}%`} />
              <StatRow label="Средний перенос, дней" value={transferStatsQuery.data.avgShiftDays.toFixed(1)} />
            </div>
          ) : (
            <div style={{ color: "var(--color-text-muted)", fontSize: 13, marginBottom: 16 }}>
              Доступно членам Правления и администратору
            </div>
          )}

          <div style={{ display: "flex", gap: 8 }}>
            <button className="btn-secondary" disabled title="Экспорт доступен после серверной реализации">
              Экспорт XLSX
            </button>
            <button className="btn-secondary" disabled title="Экспорт доступен после серверной реализации">
              Экспорт PDF
            </button>
          </div>
        </div>
      </div>
    </AppShell>
  );
}

function StatRow({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between" }}>
      <span style={{ color: "var(--color-text-secondary)" }}>{label}</span>
      <span style={{ fontWeight: 700 }}>{value}</span>
    </div>
  );
}
