import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { AppShell } from "../components/AppShell";
import { auditLogApi } from "../api/auditLog";

export function AuditLogPage() {
  const [page, setPage] = useState(1);
  const pageSize = 20;

  const query = useQuery({
    queryKey: ["audit-log", page],
    queryFn: () => auditLogApi.search({ page, pageSize }),
  });

  const data = query.data;

  return (
    <AppShell title="Журнал аудита">
      <div className="card" style={{ padding: 0, overflow: "hidden", marginBottom: 16 }}>
        <table>
          <thead>
            <tr>
              <th>Дата/время</th>
              <th>Пользователь</th>
              <th>Действие</th>
              <th>Объект</th>
            </tr>
          </thead>
          <tbody>
            {data?.items.map((entry) => (
              <tr key={entry.id}>
                <td>{new Date(entry.ts).toLocaleString("ru-RU")}</td>
                <td>{entry.actorName ?? "—"}</td>
                <td>{entry.action}</td>
                <td>
                  {entry.entityType} · {entry.entityId.slice(0, 8)}
                </td>
              </tr>
            ))}
            {data?.items.length === 0 && (
              <tr>
                <td colSpan={4} style={{ textAlign: "center", color: "var(--color-text-muted)", padding: 24 }}>
                  Записей нет
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <button className="btn-secondary" disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>
            Назад
          </button>
          <span style={{ fontSize: 13, color: "var(--color-text-secondary)" }}>
            Стр. {data.page} из {data.totalPages}
          </span>
          <button className="btn-secondary" disabled={page >= data.totalPages} onClick={() => setPage((p) => p + 1)}>
            Вперёд
          </button>
        </div>
      )}
    </AppShell>
  );
}
