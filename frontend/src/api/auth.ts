import { api } from "./client";
import type { LoginRequest, LoginResponse, TokenResponse, UserSummary } from "./types";

export const authApi = {
  login: (body: LoginRequest) => api.post<LoginResponse>("/auth/login", body),
  refresh: (refreshToken: string) => api.post<TokenResponse>("/auth/refresh", { refreshToken }),
  logout: (refreshToken: string) => api.post<void>("/auth/logout", { refreshToken }),
  me: () => api.get<UserSummary>("/auth/me"),
};
