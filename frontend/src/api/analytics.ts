import { api, buildQuery } from "./client";
import type {
  AnalyticsSummaryResponse,
  BoardMemberAnalyticsResponse,
  DepartmentAnalyticsResponse,
  RegistryResponse,
  TransferRequestAnalyticsResponse,
} from "./types";

export const analyticsApi = {
  summary: (departmentId?: string) =>
    api.get<AnalyticsSummaryResponse>(`/analytics/summary${buildQuery({ departmentId })}`),
  departments: () => api.get<DepartmentAnalyticsResponse[]>("/analytics/departments"),
  boardMembers: () => api.get<BoardMemberAnalyticsResponse[]>("/analytics/board-members"),
  transferRequests: () => api.get<TransferRequestAnalyticsResponse>("/analytics/transfer-requests"),
  registry: (departmentId?: string) => api.get<RegistryResponse>(`/analytics/registry${buildQuery({ departmentId })}`),
};
