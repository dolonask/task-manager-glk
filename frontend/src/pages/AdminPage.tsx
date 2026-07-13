import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppShell } from "../components/AppShell";
import { usersApi } from "../api/users";
import { departmentsApi } from "../api/departments";
import type { DepartmentResponse, Role, UserResponse } from "../api/types";
import { ApiError } from "../api/client";

const roleLabels: Record<Role, string> = {
  admin: "Администратор",
  board: "Член Правления",
  head: "Начальник СП",
  employee: "Сотрудник СП",
  observer: "Наблюдатель",
};

type FormMode = { type: "none" } | { type: "create" } | { type: "edit"; user: UserResponse };
type DeptFormMode = { type: "none" } | { type: "create" } | { type: "edit"; department: DepartmentResponse };

export function AdminPage() {
  const queryClient = useQueryClient();
  const [formMode, setFormMode] = useState<FormMode>({ type: "none" });
  const [deptFormMode, setDeptFormMode] = useState<DeptFormMode>({ type: "none" });

  const usersQuery = useQuery({ queryKey: ["users"], queryFn: () => usersApi.list() });
  const departmentsQuery = useQuery({ queryKey: ["departments"], queryFn: departmentsApi.list });

  const departmentNameById = new Map((departmentsQuery.data ?? []).map((d) => [d.id, d.name]));
  const boardUsers = usersQuery.data?.filter((u) => u.role === "board") ?? [];
  const headUsers = usersQuery.data?.filter((u) => u.role === "head") ?? [];

  const closeForm = () => setFormMode({ type: "none" });
  const onSaved = () => {
    closeForm();
    queryClient.invalidateQueries({ queryKey: ["users"] });
  };

  const closeDeptForm = () => setDeptFormMode({ type: "none" });
  const onDeptSaved = () => {
    closeDeptForm();
    queryClient.invalidateQueries({ queryKey: ["departments"] });
  };

  return (
    <AppShell title="Пользователи и СП">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
        <div style={{ fontWeight: 700, fontSize: 15 }}>Пользователи</div>
        {formMode.type === "none" && (
          <button className="btn-primary" onClick={() => setFormMode({ type: "create" })}>
            + Добавить пользователя
          </button>
        )}
      </div>

      {formMode.type === "create" && (
        <UserForm mode="create" departments={departmentsQuery.data ?? []} onCancel={closeForm} onSaved={onSaved} />
      )}
      {formMode.type === "edit" && (
        <UserForm
          mode="edit"
          user={formMode.user}
          departments={departmentsQuery.data ?? []}
          onCancel={closeForm}
          onSaved={onSaved}
        />
      )}

      <div className="card" style={{ padding: 0, overflow: "hidden", marginBottom: 24 }}>
        <table>
          <thead>
            <tr>
              <th>ФИО</th>
              <th>Логин</th>
              <th>Роль</th>
              <th>СП</th>
              <th>Статус</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {usersQuery.data?.map((u) => (
              <tr key={u.id}>
                <td>{u.fullName}</td>
                <td>{u.login}@glk.kg</td>
                <td>{roleLabels[u.role]}</td>
                <td>{u.departmentId ? departmentNameById.get(u.departmentId) ?? "—" : "—"}</td>
                <td>
                  <span style={{ color: u.isActive ? "var(--color-text-secondary)" : "var(--priority-high)" }}>
                    {u.isActive ? "Активен" : "Отключён"}
                  </span>
                </td>
                <td>
                  <button className="btn-secondary" onClick={() => setFormMode({ type: "edit", user: u })}>
                    Изменить
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
        <div style={{ fontWeight: 700, fontSize: 15 }}>Структурные подразделения</div>
        {deptFormMode.type === "none" && (
          <button className="btn-primary" onClick={() => setDeptFormMode({ type: "create" })}>
            + Добавить СП
          </button>
        )}
      </div>

      {deptFormMode.type === "create" && (
        <DepartmentForm mode="create" boardUsers={boardUsers} headUsers={headUsers} onCancel={closeDeptForm} onSaved={onDeptSaved} />
      )}
      {deptFormMode.type === "edit" && (
        <DepartmentForm
          mode="edit"
          department={deptFormMode.department}
          boardUsers={boardUsers}
          headUsers={headUsers}
          onCancel={closeDeptForm}
          onSaved={onDeptSaved}
        />
      )}

      <div className="card" style={{ padding: 0, overflow: "hidden" }}>
        <table>
          <thead>
            <tr>
              <th>СП</th>
              <th>Курирующий член Правления</th>
              <th>Начальник СП</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {departmentsQuery.data?.map((d) => (
              <tr key={d.id}>
                <td>{d.name}</td>
                <td>{d.curatorName ?? "—"}</td>
                <td>{d.headName ?? "—"}</td>
                <td>
                  <button className="btn-secondary" onClick={() => setDeptFormMode({ type: "edit", department: d })}>
                    Изменить
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AppShell>
  );
}

function UserForm({
  mode,
  user,
  departments,
  onCancel,
  onSaved,
}: {
  mode: "create" | "edit";
  user?: UserResponse;
  departments: { id: string; name: string }[];
  onCancel: () => void;
  onSaved: () => void;
}) {
  const [fullName, setFullName] = useState(user?.fullName ?? "");
  const [login, setLogin] = useState(user?.login ?? "");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<Role>(user?.role ?? "head");
  const [departmentId, setDepartmentId] = useState(user?.departmentId ?? "");
  const [isActive, setIsActive] = useState(user?.isActive ?? true);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => {
      if (mode === "create") {
        return usersApi.create({
          fullName,
          login,
          password,
          role,
          departmentId: departmentId || undefined,
        });
      }
      return usersApi.update(user!.id, {
        fullName,
        login,
        password: password || undefined,
        role,
        departmentId: departmentId || undefined,
        isActive,
      });
    },
    onSuccess: onSaved,
    onError: (err) =>
      setError(err instanceof ApiError ? err.message : mode === "create" ? "Не удалось добавить пользователя" : "Не удалось сохранить изменения"),
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!fullName.trim() || !login.trim() || (mode === "create" && !password.trim())) return;
    mutation.mutate();
  };

  return (
    <form onSubmit={handleSubmit} className="card" style={{ marginBottom: 16 }}>
      <div style={{ fontWeight: 700, marginBottom: 12 }}>
        {mode === "create" ? "Новый пользователь" : `Изменить: ${user?.fullName}`}
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 12 }}>
        <input placeholder="ФИО" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
        <input placeholder="Логин (без @glk.kg)" value={login} onChange={(e) => setLogin(e.target.value)} required />
        <input
          type="password"
          placeholder={mode === "create" ? "Пароль" : "Новый пароль (оставьте пустым, чтобы не менять)"}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required={mode === "create"}
        />
        <select value={role} onChange={(e) => setRole(e.target.value as Role)}>
          <option value="admin">Администратор</option>
          <option value="board">Член Правления</option>
          <option value="head">Начальник СП</option>
          <option value="employee">Сотрудник СП</option>
          <option value="observer">Наблюдатель</option>
        </select>
        <select value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}>
          <option value="">Без СП</option>
          {departments.map((d) => (
            <option key={d.id} value={d.id}>
              {d.name}
            </option>
          ))}
        </select>
        {mode === "edit" && (
          <label style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13 }}>
            <input type="checkbox" checked={isActive} onChange={(e) => setIsActive(e.target.checked)} />
            Учётная запись активна
          </label>
        )}
      </div>
      {error && <div style={{ color: "var(--priority-high)", fontSize: 13, marginBottom: 12 }}>{error}</div>}
      <div style={{ display: "flex", gap: 10 }}>
        <button type="button" className="btn-secondary" onClick={onCancel}>
          Отмена
        </button>
        <button type="submit" className="btn-primary" disabled={mutation.isPending}>
          Сохранить
        </button>
      </div>
    </form>
  );
}

function DepartmentForm({
  mode,
  department,
  boardUsers,
  headUsers,
  onCancel,
  onSaved,
}: {
  mode: "create" | "edit";
  department?: DepartmentResponse;
  boardUsers: UserResponse[];
  headUsers: UserResponse[];
  onCancel: () => void;
  onSaved: () => void;
}) {
  const [name, setName] = useState(department?.name ?? "");
  const [curatorId, setCuratorId] = useState(department?.curatorId ?? "");
  const [headId, setHeadId] = useState(department?.headId ?? "");
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => {
      if (mode === "create") {
        return departmentsApi.create({ name, curatorId: curatorId || undefined, headId: headId || undefined });
      }
      return departmentsApi.update(department!.id, { name, curatorId: curatorId || undefined, headId: headId || undefined });
    },
    onSuccess: onSaved,
    onError: (err) =>
      setError(err instanceof ApiError ? err.message : mode === "create" ? "Не удалось добавить СП" : "Не удалось сохранить изменения"),
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    mutation.mutate();
  };

  return (
    <form onSubmit={handleSubmit} className="card" style={{ marginBottom: 16 }}>
      <div style={{ fontWeight: 700, marginBottom: 12 }}>
        {mode === "create" ? "Новое структурное подразделение" : `Изменить: ${department?.name}`}
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 12 }}>
        <input placeholder="Название СП" value={name} onChange={(e) => setName(e.target.value)} required style={{ gridColumn: "1 / -1" }} />
        <select value={curatorId} onChange={(e) => setCuratorId(e.target.value)}>
          <option value="">Курирующий член Правления — не выбран</option>
          {boardUsers.map((u) => (
            <option key={u.id} value={u.id}>
              {u.fullName}
            </option>
          ))}
        </select>
        <select value={headId} onChange={(e) => setHeadId(e.target.value)}>
          <option value="">Начальник СП — не выбран</option>
          {headUsers.map((u) => (
            <option key={u.id} value={u.id}>
              {u.fullName}
            </option>
          ))}
        </select>
      </div>
      {error && <div style={{ color: "var(--priority-high)", fontSize: 13, marginBottom: 12 }}>{error}</div>}
      <div style={{ display: "flex", gap: 10 }}>
        <button type="button" className="btn-secondary" onClick={onCancel}>
          Отмена
        </button>
        <button type="submit" className="btn-primary" disabled={mutation.isPending}>
          Сохранить
        </button>
      </div>
    </form>
  );
}
