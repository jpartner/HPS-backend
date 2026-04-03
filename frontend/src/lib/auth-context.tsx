'use client';

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';

interface User {
  id: string;
  email: string;
  role: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  isLoading: boolean;
  login: (accessToken: string, refreshToken: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

function parseToken(token: string): User {
  const payload = token.split('.')[1];
  const padded = payload + '='.repeat((4 - payload.length % 4) % 4);
  const decoded = JSON.parse(atob(padded));
  return { id: decoded.sub, email: decoded.email, role: decoded.role };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const stored = localStorage.getItem('hps_token');
    const storedRefresh = localStorage.getItem('hps_refresh');
    if (stored) {
      try {
        setUser(parseToken(stored));
        setToken(stored);
        setRefreshToken(storedRefresh);
      } catch {
        localStorage.removeItem('hps_token');
        localStorage.removeItem('hps_refresh');
      }
    }
    setIsLoading(false);
  }, []);

  const login = useCallback((accessToken: string, refreshToken: string) => {
    localStorage.setItem('hps_token', accessToken);
    localStorage.setItem('hps_refresh', refreshToken);
    setToken(accessToken);
    setRefreshToken(refreshToken);
    setUser(parseToken(accessToken));
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('hps_token');
    localStorage.removeItem('hps_refresh');
    setToken(null);
    setRefreshToken(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, token, refreshToken, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
