'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { format } from 'date-fns';
import { Plus } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { DataTable } from '@/components/ui/DataTable';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { useAuth } from '@/lib/auth';
import { tenantApi } from '@/lib/api';

interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: string;
  createdAt: string;
}

export default function TenantsPage() {
  const router = useRouter();
  const { isSuperAdmin } = useAuth();
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    slug: '',
    defaultLanguage: 'en',
    currencies: 'EUR',
  });
  const [submitting, setSubmitting] = useState(false);

  const fetchTenants = useCallback(async () => {
    try {
      setLoading(true);
      const data = await tenantApi.list();
      setTenants(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load tenants';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isSuperAdmin) {
      router.replace('/');
      return;
    }
    fetchTenants();
  }, [isSuperAdmin, router, fetchTenants]);

  async function handleCreate() {
    setSubmitting(true);
    try {
      await tenantApi.create({
        name: formData.name,
        slug: formData.slug,
        defaultLanguage: formData.defaultLanguage,
        currencies: formData.currencies.split(',').map((c) => c.trim()),
      });
      setShowModal(false);
      setFormData({ name: '', slug: '', defaultLanguage: 'en', currencies: 'EUR' });
      fetchTenants();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to create tenant';
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }

  if (!isSuperAdmin) return null;

  const columns = [
    { key: 'name', label: 'Name' },
    { key: 'slug', label: 'Slug' },
    {
      key: 'status',
      label: 'Status',
      render: (tenant: Tenant) => (
        <Badge variant={tenant.status === 'active' ? 'success' : 'secondary'}>
          {tenant.status}
        </Badge>
      ),
    },
    {
      key: 'createdAt',
      label: 'Created',
      render: (tenant: Tenant) => {
        try {
          return format(new Date(tenant.createdAt), 'MMM d, yyyy');
        } catch {
          return '--';
        }
      },
    },
  ];

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Tenants</h1>
            <p className="text-sm text-gray-500 mt-1">Manage marketplace tenants</p>
          </div>
          <Button onClick={() => setShowModal(true)}>
            <Plus className="h-4 w-4 mr-2" />
            New Tenant
          </Button>
        </div>

        {error && (
          <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">
            {error}
          </div>
        )}

        <DataTable
          columns={columns as any}
          data={tenants as any}
          isLoading={loading}
          onRowClick={(tenant: Tenant) => router.push(`/tenants/${tenant.id}`)}
          emptyMessage="No tenants found"
        />
      </div>

      <Modal open={showModal} onClose={() => setShowModal(false)} title="New Tenant">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
            <Input
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="Tenant name"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Slug</label>
            <Input
              value={formData.slug}
              onChange={(e) => setFormData({ ...formData, slug: e.target.value })}
              placeholder="tenant-slug"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Default Language</label>
            <Input
              value={formData.defaultLanguage}
              onChange={(e) => setFormData({ ...formData, defaultLanguage: e.target.value })}
              placeholder="en"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Currencies (comma-separated)
            </label>
            <Input
              value={formData.currencies}
              onChange={(e) => setFormData({ ...formData, currencies: e.target.value })}
              placeholder="EUR, USD"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => setShowModal(false)}>
              Cancel
            </Button>
            <Button onClick={handleCreate} disabled={submitting}>
              {submitting ? 'Creating...' : 'Create Tenant'}
            </Button>
          </div>
        </div>
      </Modal>
    </AdminLayout>
  );
}
