'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { ArrowLeft } from 'lucide-react';
import { format } from 'date-fns';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { adminUserApi } from '@/lib/api';

export default function UserDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();

  const [user, setUser] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const fetchUser = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminUserApi.get(params.id);
      setUser(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load user');
    } finally {
      setLoading(false);
    }
  }, [params.id]);

  useEffect(() => { fetchUser(); }, [fetchUser]);

  async function handleToggleStatus() {
    if (!user) return;
    setActionLoading(true);
    try {
      const newStatus = user.isActive ? 'INACTIVE' : 'ACTIVE';
      await adminUserApi.updateStatus(params.id, newStatus);
      fetchUser();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to update status');
    } finally {
      setActionLoading(false);
    }
  }

  if (loading) {
    return <AdminLayout><div className="flex items-center justify-center h-64 text-gray-500">Loading...</div></AdminLayout>;
  }

  if (!user) {
    return <AdminLayout><div className="text-center text-gray-500 py-16">User not found</div></AdminLayout>;
  }

  const roleVariants: Record<string, string> = {
    ADMIN: 'primary', SUPER_ADMIN: 'primary', PROVIDER: 'info', CLIENT: 'secondary',
  };

  return (
    <AdminLayout>
      <div className="space-y-8">
        <div className="flex items-center gap-4">
          <button onClick={() => router.push('/users')} className="text-gray-400 hover:text-gray-600">
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">User Detail</h1>
            <p className="text-sm text-gray-500 mt-1">{user.email}</p>
          </div>
        </div>

        {error && <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>}

        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <p className="text-xs font-medium text-gray-500 mb-1">Email</p>
              <p className="text-sm text-gray-900">{user.email}</p>
            </div>
            <div>
              <p className="text-xs font-medium text-gray-500 mb-1">Name</p>
              <p className="text-sm text-gray-900">
                {user.firstName || user.lastName
                  ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim()
                  : '--'}
              </p>
            </div>
            <div>
              <p className="text-xs font-medium text-gray-500 mb-1">Role</p>
              <Badge variant={roleVariants[user.role] || 'secondary'}>{user.role}</Badge>
            </div>
            <div>
              <p className="text-xs font-medium text-gray-500 mb-1">Status</p>
              <Badge variant={user.isActive ? 'success' : 'danger'}>
                {user.isActive ? 'Active' : 'Inactive'}
              </Badge>
            </div>
            <div>
              <p className="text-xs font-medium text-gray-500 mb-1">Language</p>
              <p className="text-sm text-gray-900">{user.preferredLang || 'en'}</p>
            </div>
            <div>
              <p className="text-xs font-medium text-gray-500 mb-1">Created</p>
              <p className="text-sm text-gray-900">
                {user.createdAt ? format(new Date(user.createdAt), 'MMM d, yyyy HH:mm') : '--'}
              </p>
            </div>
          </div>

          <div className="flex justify-end mt-6 pt-4 border-t border-gray-100">
            <Button
              variant={user.isActive ? 'danger' : 'primary'}
              onClick={handleToggleStatus}
              disabled={actionLoading}
            >
              {actionLoading ? 'Updating...' : user.isActive ? 'Deactivate User' : 'Activate User'}
            </Button>
          </div>
        </div>
      </div>
    </AdminLayout>
  );
}
