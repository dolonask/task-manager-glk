import { api, buildQuery } from "./client";
import type { CreateTransferRequestRequest, DecisionRequest, TransferRequestResponse } from "./types";

export interface TransferRequestFilters {
  status?: string;
  taskId?: string;
  departmentId?: string;
  initiatorId?: string;
  approverId?: string;
  [key: string]: string | undefined;
}

export const transferRequestsApi = {
  list: (filters: TransferRequestFilters = {}) =>
    api.get<TransferRequestResponse[]>(`/transfer-requests${buildQuery(filters)}`),
  get: (id: string) => api.get<TransferRequestResponse>(`/transfer-requests/${id}`),
  create: (taskId: string, body: CreateTransferRequestRequest) =>
    api.post<TransferRequestResponse>(`/tasks/${taskId}/transfer-requests`, body),
  submit: (id: string) => api.patch<TransferRequestResponse>(`/transfer-requests/${id}/submit`),
  approve: (id: string) => api.patch<TransferRequestResponse>(`/transfer-requests/${id}/approve`),
  reject: (id: string, body: DecisionRequest) =>
    api.patch<TransferRequestResponse>(`/transfer-requests/${id}/reject`, body),
  returnForRevision: (id: string, body: DecisionRequest) =>
    api.patch<TransferRequestResponse>(`/transfer-requests/${id}/return`, body),
  apply: (id: string) => api.patch<TransferRequestResponse>(`/transfer-requests/${id}/apply`),
};
