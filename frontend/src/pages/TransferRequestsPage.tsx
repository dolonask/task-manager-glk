import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppShell } from "../components/AppShell";
import { TransferStatusBadge } from "../components/StatusBadge";
import { transferRequestsApi } from "../api/transferRequests";
import { useAuth } from "../auth/AuthContext";
import type { TransferRequestResponse, TransferStatus } from "../api/types";
import { ApiError } from "../api/client";

const filterTabs: { value: TransferStatus | ""; label: string }[] = [
  { value: "", label: "Все" },
  { value: "pending", label: "На согласовании" },
  { value: "approved", label: "Согласованные" },
  { value: "rejected", label: "Отклонённые" },
  { value: "applied", label: "Исполненные" },
];

export function TransferRequestsPage() {
  const [filter, setFilter] = useState<TransferStatus | "">("");

  const requestsQuery = useQuery({
    queryKey: ["transfer-requests", { status: filter }],
    queryFn: () => transferRequestsApi.list({ status: filter || undefined }),
  });

  const requests = requestsQuery.data ?? [];

  return (
    <AppShell title="Заявки на перенос срока">
      <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
        {filterTabs.map((tab) => {
          const active = tab.value === filter;
          return (
            <button
              key={tab.value}
              onClick={() => setFilter(tab.value)}
              style={{
                borderRadius: "var(--radius-pill)",
                padding: "8px 16px",
                border: active ? "1px solid var(--color-text)" : "1px solid var(--color-border)",
                background: active ? "var(--color-text)" : "#fff",
                color: active ? "#fff" : "var(--color-text-secondary)",
              }}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {requests.length === 0 && (
        <div className="card" style={{ color: "var(--color-text-muted)", textAlign: "center" }}>
          Заявок не найдено
        </div>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
        {requests.map((tr) => (
          <TransferRequestCard key={tr.id} request={tr} />
        ))}
      </div>
    </AppShell>
  );
}

function TransferRequestCard({ request }: { request: TransferRequestResponse }) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [reasonMode, setReasonMode] = useState<"reject" | "return" | null>(null);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["transfer-requests"] });
    queryClient.invalidateQueries({ queryKey: ["tasks"] });
    queryClient.invalidateQueries({ queryKey: ["analytics"] });
  };

  const approveMutation = useMutation({
    mutationFn: () => transferRequestsApi.approve(request.id),
    onSuccess: invalidate,
    onError: (err) => setError(err instanceof ApiError ? err.message : "Не удалось согласовать заявку"),
  });
  const applyMutation = useMutation({
    mutationFn: () => transferRequestsApi.apply(request.id),
    onSuccess: invalidate,
    onError: (err) => setError(err instanceof ApiError ? err.message : "Не удалось применить новый срок"),
  });
  const decisionMutation = useMutation({
    mutationFn: () =>
      reasonMode === "reject"
        ? transferRequestsApi.reject(request.id, { decisionReason: reason })
        : transferRequestsApi.returnForRevision(request.id, { decisionReason: reason }),
    onSuccess: () => {
      setReasonMode(null);
      setReason("");
      invalidate();
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Не удалось сохранить решение"),
  });

  const canApprove = (user?.role === "board" || user?.role === "admin") && request.status === "pending";
  const canApply = user?.role === "admin" && request.status === "approved";

  return (
    <div className="card">
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 8 }}>
        <div>
          <div style={{ fontWeight: 700 }}>{request.taskTitle}</div>
          <div style={{ fontSize: 12, color: "var(--color-text-muted)" }}>
            {request.departmentName} · Инициатор: {request.initiatorName} · Согласующий: {request.approverName ?? "—"}
          </div>
        </div>
        <TransferStatusBadge status={request.status} />
      </div>

      <div style={{ fontSize: 13, marginBottom: 8 }}>
        {request.currentDeadline} → <span style={{ color: "var(--color-gold-dark)", fontWeight: 700 }}>{request.proposedDeadline}</span>
      </div>
      <div style={{ fontSize: 13, color: "var(--color-text-secondary)", marginBottom: 8 }}>{request.justification}</div>
      {request.decisionReason && (
        <div style={{ fontSize: 13, color: "var(--priority-high)", marginBottom: 8 }}>{request.decisionReason}</div>
      )}
      {error && <div style={{ fontSize: 13, color: "var(--priority-high)", marginBottom: 8 }}>{error}</div>}

      {reasonMode && (
        <div style={{ marginBottom: 8 }}>
          <textarea
            placeholder="Причина решения"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={2}
            style={{ width: "100%", marginBottom: 8 }}
          />
          <div style={{ display: "flex", gap: 8 }}>
            <button className="btn-secondary" onClick={() => setReasonMode(null)}>
              Отмена
            </button>
            <button
              className="btn-primary"
              disabled={!reason.trim() || decisionMutation.isPending}
              onClick={() => decisionMutation.mutate()}
            >
              Подтвердить
            </button>
          </div>
        </div>
      )}

      {!reasonMode && (
        <div style={{ display: "flex", gap: 8 }}>
          {canApprove && (
            <>
              <button className="btn-primary" disabled={approveMutation.isPending} onClick={() => approveMutation.mutate()}>
                Согласовать
              </button>
              <button className="btn-secondary" onClick={() => setReasonMode("return")}>
                Вернуть на доработку
              </button>
              <button className="btn-outline-danger" onClick={() => setReasonMode("reject")}>
                Отклонить
              </button>
            </>
          )}
          {canApply && (
            <button className="btn-primary" disabled={applyMutation.isPending} onClick={() => applyMutation.mutate()}>
              Применить новый срок к задаче
            </button>
          )}
        </div>
      )}
    </div>
  );
}
