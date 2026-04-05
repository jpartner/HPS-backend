'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { CheckCircle, XCircle, Star } from 'lucide-react';
import clsx from 'clsx';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { DataTable } from '@/components/ui/DataTable';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { adminProviderApi } from '@/lib/api';

interface Provider {
  id: string;
  businessName: string;
  city: string;
  categories: string[];
  verified: boolean;
  rating: number;
  servicesCount: number;
}

export default function ProvidersPage() {
  const router = useRouter();
  const [providers, setProviders] = useState<Provider[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState<'all' | 'verified' | 'unverified'>('all');
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  // Detail modal
  const [selectedProvider, setSelectedProvider] = useState<Provider | null>(null);
  const [showDetail, setShowDetail] = useState(false);

  const fetchProviders = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminProviderApi.list();
      setProviders(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load providers';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProviders();
  }, [fetchProviders]);

  const filteredProviders =
    filter === 'all'
      ? providers
      : providers.filter((p) => (filter === 'verified' ? p.verified : !p.verified));

  async function handleVerify(providerId: string, verify: boolean) {
    setActionLoading(providerId);
    try {
      if (verify) {
        await adminProviderApi.verify(providerId);
      } else {
        await adminProviderApi.unverify(providerId);
      }
      fetchProviders();
      setShowDetail(false);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to update verification';
      setError(message);
    } finally {
      setActionLoading(null);
    }
  }

  function openDetail(provider: Provider) {
    setSelectedProvider(provider);
    setShowDetail(true);
  }

  const columns = [
    { key: 'businessName', label: 'Business Name' },
    { key: 'city', label: 'City' },
    {
      key: 'categories',
      label: 'Categories',
      render: (p: Provider) => (
        <div className="flex flex-wrap gap-1">
          {(p.categories || []).slice(0, 3).map((cat) => (
            <Badge key={cat} variant="secondary">{cat}</Badge>
          ))}
          {(p.categories || []).length > 3 && (
            <span className="text-xs text-gray-400">+{p.categories.length - 3}</span>
          )}
        </div>
      ),
    },
    {
      key: 'verified',
      label: 'Verified',
      render: (p: Provider) =>
        p.verified ? (
          <CheckCircle className="h-4 w-4 text-green-500" />
        ) : (
          <XCircle className="h-4 w-4 text-gray-300" />
        ),
    },
    {
      key: 'rating',
      label: 'Rating',
      render: (p: Provider) => (
        <div className="flex items-center gap-1">
          <Star className="h-3.5 w-3.5 text-yellow-400 fill-yellow-400" />
          <span className="text-sm">{p.rating?.toFixed(1) || '--'}</span>
        </div>
      ),
    },
    {
      key: 'servicesCount',
      label: 'Services',
      render: (p: Provider) => <span className="text-sm text-gray-600">{p.servicesCount}</span>,
    },
  ];

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Providers</h1>
          <p className="text-sm text-gray-500 mt-1">Manage service providers</p>
        </div>

        {error && (
          <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
        )}

        {/* Filter */}
        <div className="flex gap-1 border-b border-gray-200">
          {(['all', 'verified', 'unverified'] as const).map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={clsx(
                'px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors capitalize',
                filter === f
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300',
              )}
            >
              {f}
            </button>
          ))}
        </div>

        <DataTable
          columns={columns as any}
          data={filteredProviders}
          isLoading={loading}
          onRowClick={openDetail}
          emptyMessage="No providers found"
        />
      </div>

      {/* Provider Detail Modal */}
      <Modal
        open={showDetail}
        onClose={() => setShowDetail(false)}
        title="Provider Details"
      >
        {selectedProvider && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-gray-500">Business Name</p>
                <p className="font-medium text-gray-900">{selectedProvider.businessName}</p>
              </div>
              <div>
                <p className="text-gray-500">City</p>
                <p className="font-medium text-gray-900">{selectedProvider.city}</p>
              </div>
              <div>
                <p className="text-gray-500">Rating</p>
                <div className="flex items-center gap-1">
                  <Star className="h-4 w-4 text-yellow-400 fill-yellow-400" />
                  <span className="font-medium">{selectedProvider.rating?.toFixed(1) || '--'}</span>
                </div>
              </div>
              <div>
                <p className="text-gray-500">Services</p>
                <p className="font-medium text-gray-900">{selectedProvider.servicesCount}</p>
              </div>
              <div className="col-span-2">
                <p className="text-gray-500 mb-1">Categories</p>
                <div className="flex flex-wrap gap-1">
                  {(selectedProvider.categories || []).map((cat) => (
                    <Badge key={cat} variant="secondary">{cat}</Badge>
                  ))}
                </div>
              </div>
              <div className="col-span-2">
                <p className="text-gray-500">Verification Status</p>
                <div className="flex items-center gap-2 mt-1">
                  {selectedProvider.verified ? (
                    <Badge variant="success">Verified</Badge>
                  ) : (
                    <Badge variant="secondary">Unverified</Badge>
                  )}
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-gray-100">
              {selectedProvider.verified ? (
                <Button
                  variant="danger"
                  onClick={() => handleVerify(selectedProvider.id, false)}
                  disabled={actionLoading === selectedProvider.id}
                >
                  {actionLoading === selectedProvider.id ? 'Processing...' : 'Unverify Provider'}
                </Button>
              ) : (
                <Button
                  onClick={() => handleVerify(selectedProvider.id, true)}
                  disabled={actionLoading === selectedProvider.id}
                >
                  {actionLoading === selectedProvider.id ? 'Processing...' : 'Verify Provider'}
                </Button>
              )}
            </div>
          </div>
        )}
      </Modal>
    </AdminLayout>
  );
}
