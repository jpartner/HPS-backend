'use client';

import React, { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { useLanguage } from '@/lib/i18n';
import { geoApi, type Country } from '@/lib/api';

const STORAGE_KEY = 'hps_country';

interface CountryContextValue {
  countries: Country[];
  selectedCountry: Country | null;
  setCountry: (isoCode: string) => void;
  isLoading: boolean;
}

const CountryContext = createContext<CountryContextValue>({
  countries: [],
  selectedCountry: null,
  setCountry: () => {},
  isLoading: true,
});

export function CountryProvider({ children }: { children: ReactNode }) {
  const { lang } = useLanguage();
  const [countries, setCountries] = useState<Country[]>([]);
  const [selectedIso, setSelectedIso] = useState<string>('');
  const [isLoading, setIsLoading] = useState(true);
  const [mounted, setMounted] = useState(false);

  // Read stored country on mount
  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      setSelectedIso(stored);
    }
    setMounted(true);
  }, []);

  // Fetch countries when language changes
  useEffect(() => {
    if (!mounted) return;
    setIsLoading(true);
    geoApi
      .countries(lang)
      .then((data) => {
        setCountries(data);
      })
      .catch(() => {
        setCountries([]);
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [lang, mounted]);

  const setCountry = (isoCode: string) => {
    setSelectedIso(isoCode);
    if (isoCode) {
      localStorage.setItem(STORAGE_KEY, isoCode);
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  };

  const selectedCountry = countries.find((c) => c.isoCode === selectedIso) ?? null;

  if (!mounted) {
    return <>{children}</>;
  }

  return (
    <CountryContext.Provider value={{ countries, selectedCountry, setCountry, isLoading }}>
      {children}
    </CountryContext.Provider>
  );
}

export function useCountry() {
  const context = useContext(CountryContext);
  if (!context) {
    throw new Error('useCountry must be used within a CountryProvider');
  }
  return context;
}
