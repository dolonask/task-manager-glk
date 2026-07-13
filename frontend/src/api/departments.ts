import { api } from "./client";
import type { CreateDepartmentRequest, DepartmentResponse, UpdateDepartmentRequest } from "./types";

export const departmentsApi = {
  list: () => api.get<DepartmentResponse[]>("/departments"),
  create: (body: CreateDepartmentRequest) => api.post<DepartmentResponse>("/departments", body),
  update: (id: string, body: UpdateDepartmentRequest) => api.patch<DepartmentResponse>(`/departments/${id}`, body),
};
