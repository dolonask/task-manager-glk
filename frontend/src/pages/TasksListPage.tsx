import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { AppShell } from "../components/AppShell";
import { ProgressBar } from "../components/ProgressBar";
import { PriorityText, TaskStatusBadge } from "../components/StatusBadge";
import { tasksApi } from "../api/tasks";
import { departmentsApi } from "../api/departments";
import { usersApi } from "../api/users";
import { useAuth } from "../auth/AuthContext";
import type { Priority, TaskStatus } from "../api/types";
import { ApiError } from "../api/client";

const statusOptions: { value: TaskStatus | ""; label: string }[] = [
  { value: "", label: "Все статусы" },
  { value: "new", label: "Новая" },
  { value: "in_progress", label: "В работе" },
  { value: "done", label: "Выполнено" },
  { value: "overdue", label: "Просрочено" },
];

const priorityOptions: { value: Priority | ""; label: string }[] = [
  { value: "", label: "Все приоритеты" },
  { value: "high", label: "Высокий" },
  { value: "medium", label: "Средний" },
  { value: "low", label: "Низкий" },
];

export function TasksListPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [q, setQ] = useState("");
  const [status, setStatus] = useState<TaskStatus | "">("");
  const [departmentId, setDepartmentId] = useState("");
  const [priority, setPriority] = useState<Priority | "">("");
  const [showCreateForm, setShowCreateForm] = useState(false);

  const departmentsQuery = useQuery({ queryKey: ["departments"], queryFn: departmentsApi.list });
  const tasksQuery = useQuery({
    queryKey: ["tasks", { q, status, departmentId, priority }],
    queryFn: () => tasksApi.list({ q: q || undefined, status: status || undefined, departmentId: departmentId || undefined, priority: priority || undefined }),
  });

  const canCreateTask = user?.role === "admin" || user?.role === "board";

  return (
    <AppShell title="Задачи">
      <div style={{ display: "flex", flexWrap: "wrap", gap: 10, marginBottom: 16, alignItems: "center" }}>
        <input
          type="search"
          placeholder="Поиск по названию"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          style={{ minWidth: 220 }}
        />
        <select value={status} onChange={(e) => setStatus(e.target.value as TaskStatus | "")}>
          {statusOptions.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
        <select value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
          <option value="">Все СП</option>
          {departmentsQuery.data?.map((d) => (
            <option key={d.id} value={d.id}>
              {d.name}
            </option>
          ))}
        </select>
        <select value={priority} onChange={(e) => setPriority(e.target.value as Priority | "")}>
          {priorityOptions.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
        <div style={{ flex: 1 }} />
        {canCreateTask && (
          <button className="btn-primary" onClick={() => setShowCreateForm((v) => !v)}>
            + Новая задача
          </button>
        )}
      </div>

      {showCreateForm && (
        <CreateTaskForm
          departments={departmentsQuery.data ?? []}
          onCancel={() => setShowCreateForm(false)}
          onCreated={() => {
            setShowCreateForm(false);
            queryClient.invalidateQueries({ queryKey: ["tasks"] });
          }}
        />
      )}

      <div className="card" style={{ padding: 0, overflow: "hidden" }}>
        <table>
          <thead>
            <tr>
              <th style={{ width: "32%" }}>Задача</th>
              <th>СП</th>
              <th>Ответственный</th>
              <th>Приоритет</th>
              <th>Срок</th>
              <th>Статус</th>
            </tr>
          </thead>
          <tbody>
            {tasksQuery.data?.map((task) => (
              <tr key={task.id} className="clickable" onClick={() => navigate(`/tasks/${task.id}`)}>
                <td>
                  <div style={{ fontWeight: 600, marginBottom: 6 }}>{task.title}</div>
                  <ProgressBar value={task.progress} width={180} />
                </td>
                <td>{task.departmentName}</td>
                <td>{task.assigneeName ?? "—"}</td>
                <td>
                  <PriorityText priority={task.priority} />
                </td>
                <td>{task.currentDeadline}</td>
                <td>
                  <TaskStatusBadge status={task.status} />
                </td>
              </tr>
            ))}
            {tasksQuery.data?.length === 0 && (
              <tr>
                <td colSpan={6} style={{ textAlign: "center", color: "var(--color-text-muted)", padding: 24 }}>
                  Ничего не найдено
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </AppShell>
  );
}

function CreateTaskForm({
  departments,
  onCancel,
  onCreated,
}: {
  departments: { id: string; name: string }[];
  onCancel: () => void;
  onCreated: () => void;
}) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [deptId, setDeptId] = useState(departments[0]?.id ?? "");
  const [assigneeId, setAssigneeId] = useState("");
  const [priority, setPriority] = useState<Priority>("medium");
  const [deadline, setDeadline] = useState("");
  const [error, setError] = useState<string | null>(null);

  const departmentUsersQuery = useQuery({
    queryKey: ["users", { departmentId: deptId }],
    queryFn: () => usersApi.list({ departmentId: deptId }),
    enabled: !!deptId,
  });
  const assigneeOptions = (departmentUsersQuery.data ?? []).filter((u) => u.role !== "board");

  const createMutation = useMutation({
    mutationFn: () =>
      tasksApi.create({
        title,
        description: description || undefined,
        departmentId: deptId,
        assigneeId: assigneeId || undefined,
        priority,
        initialDeadline: deadline,
      }),
    onSuccess: onCreated,
    onError: (err) => setError(err instanceof ApiError ? err.message : "Не удалось создать задачу"),
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !deptId || !deadline) return;
    createMutation.mutate();
  };

  return (
    <form onSubmit={handleSubmit} className="card" style={{ marginBottom: 16 }}>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 12 }}>
        <input
          placeholder="Название задачи"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
          style={{ gridColumn: "1 / -1" }}
        />
        <textarea
          placeholder="Описание"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={2}
          style={{ gridColumn: "1 / -1" }}
        />
        <select
          value={deptId}
          onChange={(e) => {
            setDeptId(e.target.value);
            setAssigneeId("");
          }}
          required
        >
          <option value="">Выберите СП</option>
          {departments.map((d) => (
            <option key={d.id} value={d.id}>
              {d.name}
            </option>
          ))}
        </select>
        <select value={assigneeId} onChange={(e) => setAssigneeId(e.target.value)} disabled={!deptId}>
          <option value="">Ответственный — по умолчанию (начальник СП)</option>
          {assigneeOptions.map((u) => (
            <option key={u.id} value={u.id}>
              {u.fullName}
            </option>
          ))}
        </select>
        <select value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
          <option value="high">Высокий</option>
          <option value="medium">Средний</option>
          <option value="low">Низкий</option>
        </select>
        <input type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} required />
      </div>
      {error && <div style={{ color: "var(--priority-high)", fontSize: 13, marginBottom: 12 }}>{error}</div>}
      <div style={{ display: "flex", gap: 10 }}>
        <button type="button" className="btn-secondary" onClick={onCancel}>
          Отмена
        </button>
        <button type="submit" className="btn-primary" disabled={createMutation.isPending}>
          Создать
        </button>
      </div>
    </form>
  );
}
