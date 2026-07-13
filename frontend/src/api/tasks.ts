import { api, buildQuery } from "./client";
import type { CreateTaskRequest, TaskDetailResponse, TaskListItemResponse, UpdateTaskRequest } from "./types";

export interface TaskFilters {
  status?: string;
  departmentId?: string;
  creatorId?: string;
  priority?: string;
  q?: string;
  [key: string]: string | undefined;
}

export const tasksApi = {
  list: (filters: TaskFilters = {}) => api.get<TaskListItemResponse[]>(`/tasks${buildQuery(filters)}`),
  get: (id: string) => api.get<TaskDetailResponse>(`/tasks/${id}`),
  create: (body: CreateTaskRequest) => api.post<TaskDetailResponse>("/tasks", body),
  update: (id: string, body: UpdateTaskRequest) => api.patch<TaskDetailResponse>(`/tasks/${id}`, body),
  close: (id: string) => api.patch<TaskDetailResponse>(`/tasks/${id}/close`),
  remove: (id: string) => api.delete<void>(`/tasks/${id}`),
};
