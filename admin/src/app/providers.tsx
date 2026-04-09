'use client';

import { type ReactNode } from 'react';
import { AuthProvider } from '@/lib/auth';
import { TenantProvider } from '@/lib/tenant-context';
import { LanguagesProvider } from '@/lib/use-languages';

export function Providers({ children }: { children: ReactNode }) {
  return (
    <AuthProvider>
      <TenantProvider>
        <LanguagesProvider>{children}</LanguagesProvider>
      </TenantProvider>
    </AuthProvider>
  );
}
