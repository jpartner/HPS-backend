'use client';

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';
import { adminLanguageApi, type LanguageDto } from './api';
import { useAuth } from './auth';
import { useTenant } from './tenant-context';

interface LanguagesContextValue {
  languages: LanguageDto[];
  defaultLang: string;
  isLoading: boolean;
  refresh: () => void;
}

const LanguagesContext = createContext<LanguagesContextValue | undefined>(undefined);

export function LanguagesProvider({ children }: { children: ReactNode }) {
  const { token } = useAuth();
  const { selectedTenant } = useTenant();
  const [languages, setLanguages] = useState<LanguageDto[]>([{ code: 'en', name: 'English' }]);
  const [defaultLang, setDefaultLang] = useState('en');
  const [isLoading, setIsLoading] = useState(true);

  const load = useCallback(async () => {
    if (!token || !selectedTenant) return;
    try {
      setIsLoading(true);
      const config = await adminLanguageApi.get();
      setLanguages(config.supportedLangs);
      setDefaultLang(config.defaultLang);
    } catch {
      // Fallback to English if endpoint fails
      setLanguages([{ code: 'en', name: 'English' }]);
      setDefaultLang('en');
    } finally {
      setIsLoading(false);
    }
  }, [token, selectedTenant]);

  useEffect(() => { load(); }, [load]);

  return (
    <LanguagesContext.Provider value={{ languages, defaultLang, isLoading, refresh: load }}>
      {children}
    </LanguagesContext.Provider>
  );
}

export function useLanguages(): LanguagesContextValue {
  const ctx = useContext(LanguagesContext);
  if (!ctx) throw new Error('useLanguages must be used within LanguagesProvider');
  return ctx;
}
