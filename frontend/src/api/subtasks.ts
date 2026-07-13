import { api } from "./client";
import type { CreateSubtaskRequest, SubtaskResponse, UpdateSubtaskRequest } from "./types";

export const subtasksApi = {
  create: (taskId: string, body: CreateSubtaskRequest) =>
    api.post<SubtaskResponse>(`/tasks/${taskId}/subtasks`, body),
  update: (id: string, body: UpdateSubtaskRequest) => api.patch<SubtaskResponse>(`/subtasks/${id}`, body),
  toggle: (id: string) => api.patch<SubtaskResponse>(`/subtasks/${id}/toggle`),
  remove: (id: string) => api.delete<void>(`/subtasks/${id}`),
};
