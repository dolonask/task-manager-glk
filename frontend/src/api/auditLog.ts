import { api, buildQuery } from "./client";
import type { AuditLogEntryResponse, PageResponse } from "./types";

export interface AuditLogFilters {
  entityType?: string;
  entityId?: string;
  actorId?: string;
  from?: string;
  to?: string;
  page?: number;
  pageSize?: number;
  [key: string]: string | number | undefined;
}

export const auditLogApi = {
  search: (filters: AuditLogFilters = {}) =>
    api.get<PageResponse<AuditLogEntryResponse>>(`/audit-log${buildQuery(filters)}`),
};
