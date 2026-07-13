import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { authApi } from "../api/auth";
import { configureApiClient } from "../api/client";
import type { UserSummary } from "../api/types";

const REFRESH_TOKEN_KEY = "glk.refreshToken";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  user: UserSummary | null;
  status: AuthStatus;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const accessTokenRef = useRef<string | null>(null);
  const refreshTokenRef = useRef<string | null>(sessionStorage.getItem(REFRESH_TOKEN_KEY));
  const [user, setUser] = useState<UserSummary | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");

  const clearSession = useCallback(() => {
    accessTokenRef.current = null;
    refreshTokenRef.current = null;
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    setUser(null);
    setStatus("unauthenticated");
  }, []);

  useEffect(() => {
    configureApiClient({
      getAccessToken: () => accessTokenRef.current,
      getRefreshToken: () => refreshTokenRef.current,
      onTokensRefreshed: (tokens) => {
        accessTokenRef.current = tokens.accessToken;
        refreshTokenRef.current = tokens.refreshToken;
        sessionStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
      },
      onAuthExpired: clearSession,
    });
  }, [clearSession]);

  useEffect(() => {
    const existingRefreshToken = refreshTokenRef.current;
    if (!existingRefreshToken) {
      setStatus("unauthenticated");
      return;
    }
    (async () => {
      try {
        const tokens = await authApi.refresh(existingRefreshToken);
        accessTokenRef.current = tokens.accessToken;
        refreshTokenRef.current = tokens.refreshToken;
        sessionStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
        const me = await authApi.me();
        setUser(me);
        setStatus("authenticated");
      } catch {
        clearSession();
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const data = await authApi.login({ username, password });
    accessTokenRef.current = data.accessToken;
    refreshTokenRef.current = data.refreshToken;
    sessionStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
    setUser(data.user);
    setStatus("authenticated");
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = refreshTokenRef.current;
    try {
      if (refreshToken) {
        await authApi.logout(refreshToken);
      }
    } catch {
      // best-effort — clear local session regardless
    }
    clearSession();
  }, [clearSession]);

  return <AuthContext.Provider value={{ user, status, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
