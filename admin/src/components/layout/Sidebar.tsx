'use client';

import { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { clsx } from 'clsx';
import {
  LayoutDashboard,
  FolderTree,
  FileText,
  SlidersHorizontal,
  Users,
  ShieldCheck,
  KeyRound,
  Building2,
  ChevronDown,
  LogOut,
  Menu,
  X,
} from 'lucide-react';
import { useAuth } from '@/lib/auth';
import { useTenant } from '@/lib/tenant-context';

interface NavItem {
  label: string;
  href: string;
  icon: React.ReactNode;
  superAdminOnly?: boolean;
}

interface NavSection {
  title: string;
  items: NavItem[];
}

const NAV_SECTIONS: NavSection[] = [
  {
    title: '',
    items: [
      { label: 'Dashboard', href: '/', icon: <LayoutDashboard size={18} /> },
    ],
  },
  {
    title: 'Content',
    items: [
      { label: 'Categories', href: '/categories', icon: <FolderTree size={18} /> },
      { label: 'Service Templates', href: '/service-templates', icon: <FileText size={18} /> },
      { label: 'Attributes', href: '/attributes', icon: <SlidersHorizontal size={18} /> },
    ],
  },
  {
    title: 'Users',
    items: [
      { label: 'Users', href: '/users', icon: <Users size={18} /> },
      { label: 'Providers', href: '/providers', icon: <ShieldCheck size={18} /> },
    ],
  },
  {
    title: 'Settings',
    items: [
      { label: 'API Keys', href: '/api-keys', icon: <KeyRound size={18} /> },
      { label: 'Tenants', href: '/tenants', icon: <Building2 size={18} />, superAdminOnly: true },
    ],
  },
];

export function Sidebar() {
  const pathname = usePathname();
  const { user, isSuperAdmin, logout } = useAuth();
  const { tenants, selectedTenant, setTenant } = useTenant();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [tenantDropdownOpen, setTenantDropdownOpen] = useState(false);

  const showTenantSwitcher = tenants.length > 1;

  return (
    <>
      {/* Mobile hamburger */}
      <button
        onClick={() => setMobileOpen(true)}
        className="fixed top-4 left-4 z-50 rounded-lg bg-sidebar p-2 text-sidebar-text-bright lg:hidden"
        aria-label="Open menu"
      >
        <Menu size={20} />
      </button>

      {/* Backdrop */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 lg:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={clsx(
          'fixed inset-y-0 left-0 z-50 flex w-64 flex-col bg-sidebar transition-transform duration-200 lg:translate-x-0',
          mobileOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        {/* Header */}
        <div className="flex h-16 items-center justify-between px-5">
          <Link href="/" className="text-lg font-bold text-sidebar-text-bright">
            HPS Admin
          </Link>
          <button
            onClick={() => setMobileOpen(false)}
            className="text-sidebar-text lg:hidden"
            aria-label="Close menu"
          >
            <X size={20} />
          </button>
        </div>

        {/* Tenant switcher */}
        {showTenantSwitcher && (
          <div className="relative px-4 pb-3">
            <button
              onClick={() => setTenantDropdownOpen(!tenantDropdownOpen)}
              className="flex w-full items-center justify-between rounded-lg bg-sidebar-hover px-3 py-2 text-sm text-sidebar-text-bright hover:bg-slate-600 transition-colors"
            >
              <span className="truncate">{selectedTenant?.name ?? 'Select tenant'}</span>
              <ChevronDown
                size={14}
                className={clsx('ml-2 shrink-0 transition-transform', tenantDropdownOpen && 'rotate-180')}
              />
            </button>
            {tenantDropdownOpen && (
              <div className="absolute left-4 right-4 top-full z-10 mt-1 rounded-lg border border-slate-600 bg-sidebar shadow-lg">
                {tenants.map((t) => (
                  <button
                    key={t.id}
                    onClick={() => {
                      setTenant(t);
                      setTenantDropdownOpen(false);
                    }}
                    className={clsx(
                      'block w-full px-3 py-2 text-left text-sm transition-colors first:rounded-t-lg last:rounded-b-lg',
                      t.id === selectedTenant?.id
                        ? 'bg-sidebar-active text-white'
                        : 'text-sidebar-text hover:bg-sidebar-hover hover:text-sidebar-text-bright',
                    )}
                  >
                    {t.name}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto px-3 py-2">
          {NAV_SECTIONS.map((section) => {
            const visibleItems = section.items.filter(
              (item) => !item.superAdminOnly || isSuperAdmin,
            );
            if (visibleItems.length === 0) return null;

            return (
              <div key={section.title || 'main'} className="mb-4">
                {section.title && (
                  <p className="mb-1 px-3 text-xs font-semibold uppercase tracking-wider text-slate-500">
                    {section.title}
                  </p>
                )}
                {visibleItems.map((item) => {
                  const active = item.href === '/'
                    ? pathname === '/'
                    : pathname === item.href || pathname.startsWith(item.href + '/');
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      onClick={() => setMobileOpen(false)}
                      className={clsx(
                        'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                        active
                          ? 'bg-sidebar-active text-white'
                          : 'text-sidebar-text hover:bg-sidebar-hover hover:text-sidebar-text-bright',
                      )}
                    >
                      {item.icon}
                      {item.label}
                    </Link>
                  );
                })}
              </div>
            );
          })}
        </nav>

        {/* User info + logout */}
        <div className="border-t border-slate-700 p-4">
          <div className="mb-2 truncate text-sm text-sidebar-text">
            {user?.email}
          </div>
          <div className="mb-3 text-xs text-slate-500">
            {user?.role === 'SUPER_ADMIN' ? 'Super Admin' : 'Admin'}
          </div>
          <button
            onClick={logout}
            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-sidebar-text hover:bg-sidebar-hover hover:text-sidebar-text-bright transition-colors"
          >
            <LogOut size={16} />
            Sign out
          </button>
        </div>
      </aside>
    </>
  );
}
