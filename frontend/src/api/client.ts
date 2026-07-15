import type { ApiErrorBody } from "./types";

export class ApiError extends Error {
  status: number;
  code: string;
  field?: string;

  constructor(status: number, code: string, message: string, field?: string) {
    super(message);
    this.status = status;
    this.code = code;
    this.field = field;
  }
}

interface ApiClientConfig {
  getAccessToken: () => string | null;
  getRefreshToken: () => string | null;
  onTokensRefreshed: (tokens: { accessToken: string; refreshToken: string }) => void;
  onAuthExpired: () => void;
}

let config: ApiClientConfig = {
  getAccessToken: () => null,
  getRefreshToken: () => null,
  onTokensRefreshed: () => {},
  onAuthExpired: () => {},
};

export function configureApiClient(next: ApiClientConfig) {
  config = next;
}

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  if (refreshPromise) {
    return refreshPromise;
  }
  refreshPromise = (async () => {
    const refreshToken = config.getRefreshToken();
    if (!refreshToken) {
      return null;
    }
    const res = await fetch("/api/v1/auth/refresh", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) {
      return null;
    }
    const data = await res.json();
    config.onTokensRefreshed(data);
    return data.accessToken as string;
  })();
  try {
    return await refreshPromise;
  } finally {
    refreshPromise = null;
  }
}

async function doFetch(path: string, options: RequestInit, token: string | null): Promise<Response> {
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  return fetch(`/api/v1${path}`, { ...options, headers });
}

async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  let res = await doFetch(path, options, config.getAccessToken());

  if (res.status === 401 && config.getRefreshToken()) {
    const newToken = await refreshAccessToken();
    if (newToken) {
      res = await doFetch(path, options, newToken);
    } else {
      config.onAuthExpired();
    }
  } else if (res.status === 401) {
    config.onAuthExpired();
  }

  if (!res.ok) {
    let body: ApiErrorBody | null = null;
    try {
      body = await res.json();
    } catch {
      // no JSON body
    }
    throw new ApiError(
      res.status,
      body?.error?.code ?? "UNKNOWN",
      body?.error?.message ?? res.statusText,
      body?.error?.field,
    );
  }

  if (res.status === 204) {
    return undefined as T;
  }
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

async function apiDownload(path: string): Promise<{ blob: Blob; filename: string | null }> {
  let res = await doFetch(path, { method: "GET" }, config.getAccessToken());

  if (res.status === 401 && config.getRefreshToken()) {
    const newToken = await refreshAccessToken();
    if (newToken) {
      res = await doFetch(path, { method: "GET" }, newToken);
    } else {
      config.onAuthExpired();
    }
  } else if (res.status === 401) {
    config.onAuthExpired();
  }

  if (!res.ok) {
    let body: ApiErrorBody | null = null;
    try {
      body = await res.json();
    } catch {
      // no JSON body
    }
    throw new ApiError(
      res.status,
      body?.error?.code ?? "UNKNOWN",
      body?.error?.message ?? res.statusText,
      body?.error?.field,
    );
  }

  const disposition = res.headers.get("Content-Disposition");
  const match = disposition?.match(/filename="?([^";]+)"?/);
  return { blob: await res.blob(), filename: match ? match[1] : null };
}

export function triggerBrowserDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export function buildQuery(params: Record<string, string | number | boolean | undefined | null>): string {
  const sp = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") {
      sp.set(key, String(value));
    }
  }
  const qs = sp.toString();
  return qs ? `?${qs}` : "";
}

export const api = {
  get: <T>(path: string) => apiRequest<T>(path, { method: "GET" }),
  post: <T>(path: string, body?: unknown) =>
    apiRequest<T>(path, { method: "POST", body: body !== undefined ? JSON.stringify(body) : undefined }),
  patch: <T>(path: string, body?: unknown) =>
    apiRequest<T>(path, { method: "PATCH", body: body !== undefined ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => apiRequest<T>(path, { method: "DELETE" }),
  download: (path: string) => apiDownload(path),
};
