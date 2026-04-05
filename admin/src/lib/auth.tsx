'use client';

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface JwtPayload {
  sub: string;
  email: string;
  role: string;
  tenantIds?: string[];
  exp: number;
}

export interface AuthUser {
  id: string;
  email: string;
  role: 'ADMIN' | 'SUPER_ADMIN';
  tenantIds?: string[];
}

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  isLoading: boolean;
  isSuperAdmin: boolean;
  login: (token: string, refreshToken: string) => void;
  logout: () => void;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const TOKEN_KEY = 'hps_admin_token';
const REFRESH_KEY = 'hps_admin_refresh_token';

function parseJwt(token: string): JwtPayload | null {
  try {
    const base64 = token.split('.')[1];
    const json = atob(base64.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

function extractUser(token: string): AuthUser | null {
  const payload = parseJwt(token);
  if (!payload) return null;

  // Check expiry
  if (payload.exp * 1000 < Date.now()) return null;

  // Only allow admin roles
  if (payload.role !== 'ADMIN' && payload.role !== 'SUPER_ADMIN') return null;

  return {
    id: payload.sub,
    email: payload.email,
    role: payload.role as 'ADMIN' | 'SUPER_ADMIN',
    tenantIds: payload.tenantIds,
  };
}

// ---------------------------------------------------------------------------
// Context
// ---------------------------------------------------------------------------

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Hydrate from localStorage on mount
  useEffect(() => {
    const stored = localStorage.getItem(TOKEN_KEY);
    if (stored) {
      const parsed = extractUser(stored);
      if (parsed) {
        setToken(stored);
        setUser(parsed);
      } else {
        // Token invalid or expired - clean up
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(REFRESH_KEY);
      }
    }
    setIsLoading(false);
  }, []);

  const login = useCallback((newToken: string, refreshToken: string) => {
    const parsed = extractUser(newToken);
    if (!parsed) {
      throw new Error('Invalid token or insufficient permissions. Admin role required.');
    }
    localStorage.setItem(TOKEN_KEY, newToken);
    localStorage.setItem(REFRESH_KEY, refreshToken);
    setToken(newToken);
    setUser(parsed);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem('hps_admin_tenant');
    setToken(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isLoading,
        isSuperAdmin: user?.role === 'SUPER_ADMIN',
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
