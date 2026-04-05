'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { ArrowLeft } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { adminProviderApi } from '@/lib/api';

export default function ProviderDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();

  const [provider, setProvider] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const fetchProvider = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminProviderApi.get(params.id);
      setProvider(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load provider');
    } finally {
      setLoading(false);
    }
  }, [params.id]);

  useEffect(() => { fetchProvider(); }, [fetchProvider]);

  async function handleVerify(verified: boolean) {
    setActionLoading(true);
    try {
      await adminProviderApi.verify(params.id, {
        verificationStatus: verified ? 'APPROVED' : 'REJECTED',
      });
      fetchProvider();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to update verification');
    } finally {
      setActionLoading(false);
    }
  }

  if (loading) {
    return <AdminLayout><div className="flex items-center justify-center h-64 text-gray-500">Loading...</div></AdminLayout>;
  }

  if (!provider) {
    return <AdminLayout><div className="text-center text-gray-500 py-16">Provider not found</div></AdminLayout>;
  }

  return (
    <AdminLayout>
      <div className="space-y-8">
        <div className="flex items-center gap-4">
          <button onClick={() => router.push('/providers')} className="text-gray-400 hover:text-gray-600">
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              {provider.businessName || 'Provider'}
            </h1>
            <p className="text-sm text-gray-500 mt-1">Provider profile</p>
          </div>
        </div>

        {error && <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>}

        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <p className="text-xs font-medium text-gray-500 mb-1">Business Name</p>
              <p className="text-sm text-gray-900">{provider.businessName || '--'}</p>
            </div>
            <div>
              <p className="text-xs font-medium text-gray-500 mb-1">Verification</p>
              <Badge variant={provider.isVerified ? 'success' : 'secondary'}>
                {provider.isVerified ? 'Verified' : 'Unverified'}
              </Badge>
            </div>
            <div>
              <p className="text-xs font-medium text-gray-500 mb-1">Rating</p>
              <p className="text-sm text-gray-900">
                {provider.avgRating ?? '--'} ({provider.reviewCount ?? 0} reviews)
              </p>
            </div>
            <div>
              <p className="text-xs font-medium text-gray-500 mb-1">Mobile</p>
              <p className="text-sm text-gray-900">{provider.isMobile ? 'Yes' : 'No'}</p>
            </div>
            {provider.addressLine && (
              <div className="md:col-span-2">
                <p className="text-xs font-medium text-gray-500 mb-1">Address</p>
                <p className="text-sm text-gray-900">{provider.addressLine}</p>
              </div>
            )}
            {provider.description && (
              <div className="md:col-span-2">
                <p className="text-xs font-medium text-gray-500 mb-1">Description</p>
                <p className="text-sm text-gray-900">{provider.description}</p>
              </div>
            )}
          </div>

          <div className="flex justify-end mt-6 pt-4 border-t border-gray-100 gap-2">
            {!provider.isVerified ? (
              <Button onClick={() => handleVerify(true)} disabled={actionLoading}>
                {actionLoading ? 'Updating...' : 'Approve Verification'}
              </Button>
            ) : (
              <Button variant="danger" onClick={() => handleVerify(false)} disabled={actionLoading}>
                {actionLoading ? 'Updating...' : 'Revoke Verification'}
              </Button>
            )}
          </div>
        </div>
      </div>
    </AdminLayout>
  );
}
