import { api, buildQuery } from "./client";
import type { CreateUserRequest, UpdateUserRequest, UserResponse } from "./types";

export interface UserFilters {
  role?: string;
  departmentId?: string;
  isActive?: boolean;
  [key: string]: string | boolean | undefined;
}

export const usersApi = {
  list: (filters: UserFilters = {}) => api.get<UserResponse[]>(`/users${buildQuery(filters)}`),
  get: (id: string) => api.get<UserResponse>(`/users/${id}`),
  create: (body: CreateUserRequest) => api.post<UserResponse>("/users", body),
  update: (id: string, body: UpdateUserRequest) => api.patch<UserResponse>(`/users/${id}`, body),
};
