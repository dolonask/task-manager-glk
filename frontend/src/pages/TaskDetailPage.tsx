import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { AppShell } from "../components/AppShell";
import { TaskStatusBadge, TransferStatusBadge } from "../components/StatusBadge";
import { tasksApi } from "../api/tasks";
import { subtasksApi } from "../api/subtasks";
import { transferRequestsApi } from "../api/transferRequests";
import { usersApi } from "../api/users";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";

const ACTIVE_TRANSFER_STATUSES = new Set(["draft", "pending", "revision"]);

export function TaskDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [showTransferForm, setShowTransferForm] = useState(false);

  const taskQuery = useQuery({
    queryKey: ["tasks", id],
    queryFn: () => tasksApi.get(id!),
    enabled: !!id,
  });

  const invalidateTask = () => {
    queryClient.invalidateQueries({ queryKey: ["tasks", id] });
    queryClient.invalidateQueries({ queryKey: ["tasks"] });
    queryClient.invalidateQueries({ queryKey: ["transfer-requests"] });
  };

  const toggleMutation = useMutation({
    mutationFn: (subtaskId: string) => subtasksApi.toggle(subtaskId),
    onSuccess: invalidateTask,
  });

  const task = taskQuery.data;

  if (taskQuery.isLoading) {
    return (
      <AppShell title="Карточка задачи">
        <div>Загрузка...</div>
      </AppShell>
    );
  }
  if (!task) {
    return (
      <AppShell title="Карточка задачи">
        <div>Задача не найдена</div>
      </AppShell>
    );
  }

  const canToggle = (assigneeId: string | null) =>
    user?.role === "admin" ||
    (user?.role === "head" && user.departmentId === task.departmentId) ||
    (user?.role === "employee" && assigneeId === user.id);

  const canEditAssignee = user?.role === "admin" || (user?.role === "board" && task.creatorId === user.id);

  const hasActiveTransfer = task.transferRequests.some((tr) => ACTIVE_TRANSFER_STATUSES.has(tr.status));
  const canRequestTransfer =
    task.status !== "done" &&
    !hasActiveTransfer &&
    (user?.role === "admin" || (user?.role === "head" && user.departmentId === task.departmentId));

  const doneCount = task.subtasks.filter((s) => s.done).length;
  const historyDesc = [...task.transferRequests].reverse();

  return (
    <AppShell title="Карточка задачи">
      <button
        className="btn-secondary"
        style={{ border: "none", background: "none", color: "var(--color-gold-dark)", fontWeight: 700, padding: 0, marginBottom: 16 }}
        onClick={() => navigate("/tasks")}
      >
        ← Назад к задачам
      </button>

      <div className="card" style={{ marginBottom: 16 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 16 }}>
          <div style={{ maxWidth: 640 }}>
            <div style={{ fontSize: 19, fontWeight: 700, marginBottom: 8 }}>{task.title}</div>
            <div style={{ fontSize: 13, color: "var(--color-text-secondary)" }}>{task.description}</div>
          </div>
          <TaskStatusBadge status={task.status} />
        </div>
        <div style={{ borderTop: "1px solid var(--color-border)", paddingTop: 16, display: "grid", gridTemplateColumns: "repeat(5, 1fr)", gap: 12 }}>
          <Meta label="СП-исполнитель" value={task.departmentName} />
          <Meta label="Постановщик" value={task.creatorName} />
          {canEditAssignee ? (
            <AssigneeEditor
              taskId={task.id}
              departmentId={task.departmentId}
              currentAssigneeName={task.assigneeName}
              onSaved={invalidateTask}
            />
          ) : (
            <Meta label="Ответственный" value={task.assigneeName ?? "—"} />
          )}
          <Meta label="Первоначальный срок" value={task.initialDeadline} />
          <Meta label="Текущий срок" value={task.currentDeadline} />
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1.2fr 1fr", gap: 16 }}>
        <div className="card">
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 14 }}>
            <div style={{ fontWeight: 700 }}>Подзадачи</div>
            <div style={{ fontSize: 12, color: "var(--color-text-muted)" }}>
              {task.subtasks.length > 0 ? `${doneCount} из ${task.subtasks.length} выполнено` : "подзадач пока нет"}
            </div>
          </div>

          {task.subtasks.map((s) => (
            <label
              key={s.id}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 10,
                padding: "10px 0",
                borderTop: "1px solid var(--color-row-divider)",
              }}
            >
              <input
                type="checkbox"
                checked={s.done}
                disabled={!canToggle(s.assigneeId) || toggleMutation.isPending}
                onChange={() => toggleMutation.mutate(s.id)}
              />
              <div style={{ flex: 1 }}>
                <div style={{ textDecoration: s.done ? "line-through" : "none", color: s.done ? "var(--color-text-muted)" : "var(--color-text)" }}>
                  {s.title}
                </div>
              </div>
              <div style={{ fontSize: 12, color: "var(--color-text-muted)" }}>{s.assigneeName ?? "—"}</div>
              <div style={{ fontSize: 12, color: "var(--color-text-muted)" }}>{s.deadline}</div>
            </label>
          ))}

          {canRequestTransfer && !showTransferForm && (
            <button className="btn-outline-gold" style={{ marginTop: 16 }} onClick={() => setShowTransferForm(true)}>
              Подать заявку на перенос срока
            </button>
          )}
          {canRequestTransfer && showTransferForm && (
            <TransferRequestForm
              taskId={task.id}
              onCancel={() => setShowTransferForm(false)}
              onSubmitted={() => {
                setShowTransferForm(false);
                invalidateTask();
              }}
            />
          )}
        </div>

        <div className="card">
          <div style={{ fontWeight: 700, marginBottom: 14 }}>История и заявки на перенос срока</div>
          {historyDesc.length === 0 && (
            <div style={{ color: "var(--color-text-muted)", fontSize: 13 }}>Заявок по этой задаче ещё не было</div>
          )}
          {historyDesc.map((tr) => (
            <div key={tr.id} style={{ padding: "12px 0", borderTop: "1px solid var(--color-row-divider)" }}>
              <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 6 }}>
                <div style={{ fontSize: 13 }}>
                  {tr.currentDeadline} → <span style={{ color: "var(--color-gold-dark)", fontWeight: 700 }}>{tr.proposedDeadline}</span>
                </div>
                <TransferStatusBadge status={tr.status} />
              </div>
              <div style={{ fontSize: 13, marginBottom: 6 }}>{tr.justification}</div>
              <div style={{ fontSize: 12, color: "var(--color-text-muted)" }}>
                Инициатор: {tr.initiatorName} · Согласующий: {tr.approverName ?? "—"}
              </div>
              {tr.decisionReason && (
                <div style={{ fontSize: 12, color: "var(--priority-high)", marginTop: 6 }}>{tr.decisionReason}</div>
              )}
            </div>
          ))}
        </div>
      </div>
    </AppShell>
  );
}

function AssigneeEditor({
  taskId,
  departmentId,
  currentAssigneeName,
  onSaved,
}: {
  taskId: string;
  departmentId: string;
  currentAssigneeName: string | null;
  onSaved: () => void;
}) {
  const [editing, setEditing] = useState(false);
  const [assigneeId, setAssigneeId] = useState("");
  const [error, setError] = useState<string | null>(null);

  const usersQuery = useQuery({
    queryKey: ["users", { departmentId }],
    queryFn: () => usersApi.list({ departmentId }),
    enabled: editing,
  });
  const options = (usersQuery.data ?? []).filter((u) => u.role === "head" || u.role === "employee");

  const mutation = useMutation({
    mutationFn: () => tasksApi.update(taskId, { assigneeId }),
    onSuccess: () => {
      setEditing(false);
      onSaved();
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Не удалось изменить ответственного"),
  });

  if (!editing) {
    return (
      <div>
        <div style={{ fontSize: 11, fontWeight: 700, color: "var(--color-text-muted)", marginBottom: 4, textTransform: "uppercase" }}>
          Ответственный
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <span style={{ fontSize: 13, fontWeight: 600 }}>{currentAssigneeName ?? "—"}</span>
          <button
            className="btn-secondary"
            style={{ border: "none", background: "none", padding: 0, color: "var(--color-gold-dark)", fontSize: 12 }}
            onClick={() => setEditing(true)}
          >
            Изменить
          </button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div style={{ fontSize: 11, fontWeight: 700, color: "var(--color-text-muted)", marginBottom: 4, textTransform: "uppercase" }}>
        Ответственный
      </div>
      <select value={assigneeId} onChange={(e) => setAssigneeId(e.target.value)} style={{ width: "100%", marginBottom: 6 }}>
        <option value="">Выберите...</option>
        {options.map((u) => (
          <option key={u.id} value={u.id}>
            {u.fullName}
          </option>
        ))}
      </select>
      {error && <div style={{ color: "var(--priority-high)", fontSize: 12, marginBottom: 6 }}>{error}</div>}
      <div style={{ display: "flex", gap: 8 }}>
        <button type="button" className="btn-secondary" onClick={() => setEditing(false)}>
          Отмена
        </button>
        <button type="button" className="btn-primary" disabled={!assigneeId || mutation.isPending} onClick={() => mutation.mutate()}>
          Сохранить
        </button>
      </div>
    </div>
  );
}

function Meta({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div style={{ fontSize: 11, fontWeight: 700, color: "var(--color-text-muted)", marginBottom: 4, textTransform: "uppercase" }}>
        {label}
      </div>
      <div style={{ fontSize: 13, fontWeight: 600 }}>{value}</div>
    </div>
  );
}

function TransferRequestForm({
  taskId,
  onCancel,
  onSubmitted,
}: {
  taskId: string;
  onCancel: () => void;
  onSubmitted: () => void;
}) {
  const [proposedDeadline, setProposedDeadline] = useState("");
  const [justification, setJustification] = useState("");
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: async () => {
      const created = await transferRequestsApi.create(taskId, { proposedDeadline, justification });
      return transferRequestsApi.submit(created.id);
    },
    onSuccess: onSubmitted,
    onError: (err) => setError(err instanceof ApiError ? err.message : "Не удалось подать заявку"),
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!proposedDeadline || !justification.trim()) return;
    mutation.mutate();
  };

  return (
    <form onSubmit={handleSubmit} style={{ marginTop: 16, borderTop: "1px solid var(--color-border)", paddingTop: 16 }}>
      <div style={{ marginBottom: 10 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 6 }}>Предлагаемый срок</div>
        <input type="date" value={proposedDeadline} onChange={(e) => setProposedDeadline(e.target.value)} required style={{ width: "100%" }} />
      </div>
      <div style={{ marginBottom: 10 }}>
        <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 6 }}>Обоснование</div>
        <textarea value={justification} onChange={(e) => setJustification(e.target.value)} rows={3} required style={{ width: "100%" }} />
      </div>
      {error && <div style={{ color: "var(--priority-high)", fontSize: 13, marginBottom: 10 }}>{error}</div>}
      <div style={{ display: "flex", gap: 10 }}>
        <button type="button" className="btn-secondary" onClick={onCancel}>
          Отмена
        </button>
        <button type="submit" className="btn-primary" disabled={mutation.isPending}>
          Отправить
        </button>
      </div>
    </form>
  );
}
