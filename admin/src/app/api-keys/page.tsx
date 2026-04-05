'use client';

import { useState, useEffect, useCallback } from 'react';
import { format } from 'date-fns';
import { Plus, Trash2, Copy, Check } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { DataTable } from '@/components/ui/DataTable';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { apiKeyApi, type ApiKey } from '@/lib/api';
import { useTenant } from '@/lib/tenant-context';



export default function ApiKeysPage() {
  const { selectedTenant } = useTenant();
  const tenantId = selectedTenant?.id || "";
  const [keys, setKeys] = useState<ApiKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Generate modal
  const [showModal, setShowModal] = useState(false);
  const [keyName, setKeyName] = useState('');
  const [generating, setGenerating] = useState(false);
  const [newKey, setNewKey] = useState<ApiKey | null>(null);
  const [copied, setCopied] = useState<'id' | 'secret' | null>(null);

  // Deleting
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const fetchKeys = useCallback(async () => {
    if (!tenantId) return;
    try {
      setLoading(true);
      setError('');
      const data = await apiKeyApi.list(tenantId);
      setKeys(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load API keys';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [tenantId]);

  useEffect(() => {
    fetchKeys();
  }, [fetchKeys]);

  function maskClientId(id: string) {
    if (!id || id.length < 8) return id;
    return id.substring(0, 4) + '****' + id.substring(id.length - 4);
  }

  async function handleGenerate() {
    setGenerating(true);
    try {
      const result = await apiKeyApi.create(tenantId, { name: keyName });
      setNewKey(result);
      setKeyName('');
      fetchKeys();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to generate API key';
      setError(message);
    } finally {
      setGenerating(false);
    }
  }

  async function handleDelete(keyId: string) {
    if (!confirm('Delete this API key? This cannot be undone.')) return;
    setDeletingId(keyId);
    try {
      await apiKeyApi.delete(tenantId, keyId);
      fetchKeys();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to delete API key';
      setError(message);
    } finally {
      setDeletingId(null);
    }
  }

  async function copyToClipboard(text: string, type: 'id' | 'secret') {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(type);
      setTimeout(() => setCopied(null), 2000);
    } catch {
      // Fallback: do nothing
    }
  }

  const columns = [
    { key: 'name', label: 'Name' },
    {
      key: 'clientId',
      label: 'Client ID',
      render: (k: ApiKey) => (
        <span className="font-mono text-xs text-gray-500">{maskClientId((k as any).clientId)}</span>
      ),
    },
    { key: 'tenantName', label: 'Tenant' },
    {
      key: 'lastUsed',
      label: 'Last Used',
      render: (k: ApiKey) => {
        if (!(k as any).lastUsed) return <span className="text-gray-400">Never</span>;
        try {
          return format(new Date((k as any).lastUsed), 'MMM d, yyyy HH:mm');
        } catch {
          return '--';
        }
      },
    },
    {
      key: 'status',
      label: 'Status',
      render: (k: ApiKey) => (
        <Badge variant={(k as any).status === 'active' ? 'success' : 'default'}>{(k as any).status}</Badge>
      ),
    },
    {
      key: 'actions',
      label: '',
      render: (k: ApiKey) => (
        <div className="flex justify-end">
          <button
            onClick={(e) => { e.stopPropagation(); handleDelete(k.id); }}
            disabled={deletingId === k.id}
            className="text-gray-400 hover:text-red-600 disabled:opacity-50"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">API Keys</h1>
            <p className="text-sm text-gray-500 mt-1">Manage API key lifecycle</p>
          </div>
          <Button onClick={() => { setNewKey(null); setKeyName(''); setShowModal(true); }}>
            <Plus className="h-4 w-4 mr-2" />
            Generate Key
          </Button>
        </div>

        {error && (
          <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
        )}

        <DataTable
          columns={columns as any}
          data={keys as any}
          isLoading={loading}
          emptyMessage="No API keys found"
        />
      </div>

      <Modal
        open={showModal}
        onClose={() => setShowModal(false)}
        title={newKey ? 'API Key Generated' : 'Generate API Key'}
      >
        {newKey ? (
          <div className="space-y-4">
            <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-md">
              <p className="text-sm text-yellow-800 font-medium mb-2">
                Copy the client secret now. It will not be shown again.
              </p>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Client ID</label>
              <div className="flex items-center gap-2">
                <code className="flex-1 text-sm font-mono bg-gray-50 px-3 py-2 rounded border border-gray-200 break-all">
                  {(newKey as any).clientId}
                </code>
                <button
                  onClick={() => copyToClipboard((newKey as any).clientId, 'id')}
                  className="text-gray-400 hover:text-gray-600"
                >
                  {copied === 'id' ? <Check className="h-4 w-4 text-green-500" /> : <Copy className="h-4 w-4" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Client Secret</label>
              <div className="flex items-center gap-2">
                <code className="flex-1 text-sm font-mono bg-gray-50 px-3 py-2 rounded border border-gray-200 break-all">
                  {(newKey as any).clientSecret}
                </code>
                <button
                  onClick={() => copyToClipboard((newKey as any).clientSecret, 'secret')}
                  className="text-gray-400 hover:text-gray-600"
                >
                  {copied === 'secret' ? <Check className="h-4 w-4 text-green-500" /> : <Copy className="h-4 w-4" />}
                </button>
              </div>
            </div>

            <div className="flex justify-end pt-2">
              <Button onClick={() => setShowModal(false)}>Done</Button>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Key Name</label>
              <Input
                value={keyName}
                onChange={(e) => setKeyName(e.target.value)}
                placeholder="e.g. Production API Key"
              />
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" onClick={() => setShowModal(false)}>Cancel</Button>
              <Button onClick={handleGenerate} disabled={generating || !keyName}>
                {generating ? 'Generating...' : 'Generate'}
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </AdminLayout>
  );
}
