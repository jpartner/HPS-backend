'use client';

import { useEffect } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import { useTenant } from '@/lib/tenant-context';
import { Sidebar } from './Sidebar';

const PAGE_TITLES: Record<string, string> = {
  '/': 'Dashboard',
  '/categories': 'Categories',
  '/service-templates': 'Service Templates',
  '/attributes': 'Attributes',
  '/users': 'Users',
  '/providers': 'Providers',
  '/api-keys': 'API Keys',
  '/reference-lists': 'Reference Lists',
  '/country-currencies': 'Country Currencies',
  '/rate-presets': 'Rate Duration Presets',
  '/tenants': 'Tenants',
};

function getPageTitle(pathname: string): string {
  // Exact match
  if (PAGE_TITLES[pathname]) return PAGE_TITLES[pathname];
  // Check prefix matches (e.g. /categories/123 -> Categories)
  for (const [path, title] of Object.entries(PAGE_TITLES)) {
    if (pathname.startsWith(path + '/')) return title;
  }
  return 'Admin';
}

export function AdminLayout({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth();
  const { isLoading: tenantLoading } = useTenant();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace('/login');
    }
  }, [isLoading, user, router]);

  if (isLoading || tenantLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (!user) return null;

  const title = getPageTitle(pathname);

  return (
    <div className="min-h-screen">
      <Sidebar />
      <main className="lg:pl-64">
        <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/80 backdrop-blur-sm">
          <div className="flex h-16 items-center px-6 pl-16 lg:pl-6">
            <h1 className="text-xl font-semibold text-slate-900">{title}</h1>
          </div>
        </header>
        <div className="p-6">{children}</div>
      </main>
    </div>
  );
}
