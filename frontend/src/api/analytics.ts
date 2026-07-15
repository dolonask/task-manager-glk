import { api, buildQuery, triggerBrowserDownload } from "./client";
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

export const exportApi = {
  async analytics(format: "xlsx" | "pdf", departmentId?: string) {
    const { blob, filename } = await api.download(`/export/analytics${buildQuery({ format, departmentId })}`);
    triggerBrowserDownload(blob, filename ?? `analytics.${format}`);
  },
  async tasks(
    format: "xlsx" | "pdf",
    filters: { status?: string; departmentId?: string; creatorId?: string; priority?: string; q?: string } = {},
  ) {
    const { blob, filename } = await api.download(`/export/tasks${buildQuery({ format, ...filters })}`);
    triggerBrowserDownload(blob, filename ?? `tasks.${format}`);
  },
};
