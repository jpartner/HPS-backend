'use client';

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';
import { useAuth } from './auth';
import { tenantApi, type Tenant } from './api';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface TenantContextValue {
  tenants: Tenant[];
  selectedTenant: Tenant | null;
  setTenant: (tenant: Tenant) => void;
  isLoading: boolean;
}

const STORAGE_KEY = 'hps_admin_tenant';

// ---------------------------------------------------------------------------
// Context
// ---------------------------------------------------------------------------

const TenantContext = createContext<TenantContextValue | undefined>(undefined);

export function TenantProvider({ children }: { children: ReactNode }) {
  const { user, token } = useAuth();
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [selectedTenant, setSelectedTenant] = useState<Tenant | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Fetch tenants when authenticated
  useEffect(() => {
    if (!token || !user) {
      setTenants([]);
      setSelectedTenant(null);
      setIsLoading(false);
      return;
    }

    let cancelled = false;

    async function loadTenants() {
      try {
        const res = await tenantApi.list({ size: 100 });
        if (cancelled) return;

        let available = res.content;

        // ADMINs see only their assigned tenants
        if (user!.role === 'ADMIN' && user!.tenantIds?.length) {
          available = available.filter((t) => user!.tenantIds!.includes(t.id));
        }

        setTenants(available);

        // Restore previously selected tenant
        const storedId = localStorage.getItem(STORAGE_KEY);
        const stored = available.find((t) => t.id === storedId);
        setSelectedTenant(stored ?? available[0] ?? null);
      } catch {
        // API not available yet - that's fine during dev
        setTenants([]);
        setSelectedTenant(null);
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    loadTenants();
    return () => {
      cancelled = true;
    };
  }, [token, user]);

  const setTenant = useCallback((tenant: Tenant) => {
    setSelectedTenant(tenant);
    localStorage.setItem(STORAGE_KEY, tenant.id);
  }, []);

  return (
    <TenantContext.Provider
      value={{ tenants, selectedTenant, setTenant, isLoading }}
    >
      {children}
    </TenantContext.Provider>
  );
}

export function useTenant(): TenantContextValue {
  const ctx = useContext(TenantContext);
  if (!ctx) throw new Error('useTenant must be used within TenantProvider');
  return ctx;
}
