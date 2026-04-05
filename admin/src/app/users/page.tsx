'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { format } from 'date-fns';
import clsx from 'clsx';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { DataTable } from '@/components/ui/DataTable';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { adminUserApi } from '@/lib/api';

interface User {
  id: string;
  email: string;
  name: string;
  role: string;
  status: string;
  createdAt: string;
}

const ROLE_TABS = ['All', 'Client', 'Provider', 'Admin'] as const;

export default function UsersPage() {
  const router = useRouter();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState<string>('All');
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const fetchUsers = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminUserApi.list();
      setUsers(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load users';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const filteredUsers =
    activeTab === 'All'
      ? users
      : users.filter((u) => u.role.toUpperCase() === activeTab.toUpperCase());

  async function handleAction(userId: string, action: 'activate' | 'suspend') {
    setActionLoading(userId);
    try {
      if (action === 'activate') {
        await adminUserApi.activate(userId);
      } else {
        await adminUserApi.suspend(userId);
      }
      fetchUsers();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : `Failed to ${action} user`;
      setError(message);
    } finally {
      setActionLoading(null);
    }
  }

  const columns = [
    { key: 'email', label: 'Email' },
    { key: 'name', label: 'Name' },
    {
      key: 'role',
      label: 'Role',
      render: (user: User) => {
        const variants: Record<string, string> = {
          ADMIN: 'primary',
          SUPER_ADMIN: 'primary',
          PROVIDER: 'info',
          CLIENT: 'secondary',
        };
        return <Badge variant={variants[user.role] || 'secondary'}>{user.role}</Badge>;
      },
    },
    {
      key: 'status',
      label: 'Status',
      render: (user: User) => (
        <Badge variant={user.status === 'active' ? 'success' : user.status === 'suspended' ? 'danger' : 'secondary'}>
          {user.status}
        </Badge>
      ),
    },
    {
      key: 'createdAt',
      label: 'Created',
      render: (user: User) => {
        try {
          return format(new Date(user.createdAt), 'MMM d, yyyy');
        } catch {
          return '--';
        }
      },
    },
    {
      key: 'actions',
      label: '',
      render: (user: User) => (
        <div className="flex gap-2 justify-end">
          {user.status !== 'active' && (
            <button
              onClick={(e) => { e.stopPropagation(); handleAction(user.id, 'activate'); }}
              disabled={actionLoading === user.id}
              className="text-xs text-green-600 hover:text-green-800 font-medium disabled:opacity-50"
            >
              Activate
            </button>
          )}
          {user.status === 'active' && (
            <button
              onClick={(e) => { e.stopPropagation(); handleAction(user.id, 'suspend'); }}
              disabled={actionLoading === user.id}
              className="text-xs text-red-600 hover:text-red-800 font-medium disabled:opacity-50"
            >
              Suspend
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Users</h1>
          <p className="text-sm text-gray-500 mt-1">Manage user accounts and roles</p>
        </div>

        {error && (
          <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
        )}

        {/* Role Filter Tabs */}
        <div className="flex gap-1 border-b border-gray-200">
          {ROLE_TABS.map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={clsx(
                'px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors',
                activeTab === tab
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300',
              )}
            >
              {tab}
            </button>
          ))}
        </div>

        <DataTable
          columns={columns as any}
          data={filteredUsers}
          isLoading={loading}
          onRowClick={() => {}}
          emptyMessage="No users found"
        />
      </div>
    </AdminLayout>
  );
}
