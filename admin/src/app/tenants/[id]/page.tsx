'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Save, Trash2, Plus, ArrowLeft, Key, UserPlus, X } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { tenantApi } from '@/lib/api';

interface ApiKey {
  id: string;
  name: string;
  clientId: string;
  createdAt: string;
}

interface TenantAdmin {
  id: string;
  email: string;
  name: string;
}

interface TenantDetail {
  id: string;
  name: string;
  slug: string;
  domain: string;
  defaultLanguage: string;
  supportedLanguages: string[];
  defaultCurrency: string;
  settings: Record<string, unknown>;
  apiKeys: ApiKey[];
  admins: TenantAdmin[];
}

export default function TenantDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [tenant, setTenant] = useState<TenantDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Form state
  const [formData, setFormData] = useState({
    name: '',
    slug: '',
    domain: '',
    defaultLanguage: '',
    supportedLanguages: '',
    defaultCurrency: '',
    settings: '',
  });

  // API Key modal
  const [showKeyModal, setShowKeyModal] = useState(false);
  const [newKeyName, setNewKeyName] = useState('');
  const [creatingKey, setCreatingKey] = useState(false);
  const [newKeySecret, setNewKeySecret] = useState('');

  // Admin modal
  const [showAdminModal, setShowAdminModal] = useState(false);
  const [adminEmail, setAdminEmail] = useState('');
  const [addingAdmin, setAddingAdmin] = useState(false);

  const fetchTenant = useCallback(async () => {
    try {
      setLoading(true);
      const data = await tenantApi.get(params.id);
      setTenant(data);
      setFormData({
        name: data.name || '',
        slug: data.slug || '',
        domain: data.domain || '',
        defaultLanguage: data.defaultLanguage || '',
        supportedLanguages: (data.supportedLanguages || []).join(', '),
        defaultCurrency: data.defaultCurrency || '',
        settings: JSON.stringify(data.settings || {}, null, 2),
      });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load tenant';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [params.id]);

  useEffect(() => {
    fetchTenant();
  }, [fetchTenant]);

  async function handleSave() {
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      let parsedSettings = {};
      try {
        parsedSettings = JSON.parse(formData.settings);
      } catch {
        setError('Invalid JSON in settings field');
        setSaving(false);
        return;
      }

      await tenantApi.update(params.id, {
        name: formData.name,
        slug: formData.slug,
        domain: formData.domain,
        defaultLanguage: formData.defaultLanguage,
        supportedLanguages: formData.supportedLanguages.split(',').map((l) => l.trim()).filter(Boolean),
        defaultCurrency: formData.defaultCurrency,
        settings: parsedSettings,
      });
      setSuccess('Tenant updated successfully.');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to update tenant';
      setError(message);
    } finally {
      setSaving(false);
    }
  }

  async function handleCreateKey() {
    setCreatingKey(true);
    try {
      const result = await tenantApi.createApiKey(params.id, { name: newKeyName });
      setNewKeySecret(result.clientSecret || '');
      setNewKeyName('');
      fetchTenant();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to create API key';
      setError(message);
    } finally {
      setCreatingKey(false);
    }
  }

  async function handleDeleteKey(keyId: string) {
    if (!confirm('Delete this API key? This cannot be undone.')) return;
    try {
      await tenantApi.deleteApiKey(params.id, keyId);
      fetchTenant();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to delete API key';
      setError(message);
    }
  }

  async function handleAddAdmin() {
    setAddingAdmin(true);
    try {
      await tenantApi.addAdmin(params.id, { email: adminEmail });
      setAdminEmail('');
      setShowAdminModal(false);
      fetchTenant();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to add admin';
      setError(message);
    } finally {
      setAddingAdmin(false);
    }
  }

  async function handleRemoveAdmin(adminId: string) {
    if (!confirm('Remove this admin from the tenant?')) return;
    try {
      await tenantApi.removeAdmin(params.id, adminId);
      fetchTenant();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to remove admin';
      setError(message);
    }
  }

  if (loading) {
    return (
      <AdminLayout>
        <div className="flex items-center justify-center h-64 text-gray-500">Loading tenant...</div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="flex items-center gap-4">
          <button onClick={() => router.push('/tenants')} className="text-gray-400 hover:text-gray-600">
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{tenant?.name || 'Tenant'}</h1>
            <p className="text-sm text-gray-500 mt-1">Tenant ID: {params.id}</p>
          </div>
        </div>

        {error && (
          <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
        )}
        {success && (
          <div className="p-3 rounded bg-green-50 border border-green-200 text-green-700 text-sm">{success}</div>
        )}

        {/* Edit Form */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Tenant Details</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
              <Input
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Slug</label>
              <Input
                value={formData.slug}
                onChange={(e) => setFormData({ ...formData, slug: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Domain</label>
              <Input
                value={formData.domain}
                onChange={(e) => setFormData({ ...formData, domain: e.target.value })}
                placeholder="example.com"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Default Language</label>
              <Input
                value={formData.defaultLanguage}
                onChange={(e) => setFormData({ ...formData, defaultLanguage: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Supported Languages (comma-separated)
              </label>
              <Input
                value={formData.supportedLanguages}
                onChange={(e) => setFormData({ ...formData, supportedLanguages: e.target.value })}
                placeholder="en, de, fr"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Default Currency</label>
              <Input
                value={formData.defaultCurrency}
                onChange={(e) => setFormData({ ...formData, defaultCurrency: e.target.value })}
              />
            </div>
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Settings (JSON)</label>
              <textarea
                value={formData.settings}
                onChange={(e) => setFormData({ ...formData, settings: e.target.value })}
                rows={5}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm font-mono text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              />
            </div>
          </div>
          <div className="flex justify-end mt-4">
            <Button onClick={handleSave} disabled={saving}>
              <Save className="h-4 w-4 mr-2" />
              {saving ? 'Saving...' : 'Save Changes'}
            </Button>
          </div>
        </div>

        {/* API Keys Section */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">API Keys</h2>
            <Button variant="secondary" onClick={() => { setShowKeyModal(true); setNewKeySecret(''); }}>
              <Key className="h-4 w-4 mr-2" />
              Create Key
            </Button>
          </div>
          {(tenant?.apiKeys || []).length === 0 ? (
            <p className="text-sm text-gray-500">No API keys configured.</p>
          ) : (
            <div className="divide-y divide-gray-100">
              {(tenant?.apiKeys || []).map((key) => (
                <div key={key.id} className="flex items-center justify-between py-3">
                  <div>
                    <p className="text-sm font-medium text-gray-900">{key.name}</p>
                    <p className="text-xs text-gray-500 font-mono">{key.clientId}</p>
                  </div>
                  <button
                    onClick={() => handleDeleteKey(key.id)}
                    className="text-red-400 hover:text-red-600"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Admins Section */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Admin Assignments</h2>
            <Button variant="secondary" onClick={() => setShowAdminModal(true)}>
              <UserPlus className="h-4 w-4 mr-2" />
              Add Admin
            </Button>
          </div>
          {(tenant?.admins || []).length === 0 ? (
            <p className="text-sm text-gray-500">No admins assigned.</p>
          ) : (
            <div className="divide-y divide-gray-100">
              {(tenant?.admins || []).map((admin) => (
                <div key={admin.id} className="flex items-center justify-between py-3">
                  <div>
                    <p className="text-sm font-medium text-gray-900">{admin.name}</p>
                    <p className="text-xs text-gray-500">{admin.email}</p>
                  </div>
                  <button
                    onClick={() => handleRemoveAdmin(admin.id)}
                    className="text-red-400 hover:text-red-600"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Create API Key Modal */}
      <Modal open={showKeyModal} onClose={() => setShowKeyModal(false)} title="Create API Key">
        <div className="space-y-4">
          {newKeySecret ? (
            <div>
              <p className="text-sm text-gray-700 mb-2">
                Copy the client secret below. It will not be shown again.
              </p>
              <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-md">
                <p className="text-xs font-medium text-gray-500 mb-1">Client Secret</p>
                <code className="text-sm font-mono text-gray-900 break-all">{newKeySecret}</code>
              </div>
              <div className="flex justify-end mt-4">
                <Button onClick={() => setShowKeyModal(false)}>Done</Button>
              </div>
            </div>
          ) : (
            <>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Key Name</label>
                <Input
                  value={newKeyName}
                  onChange={(e) => setNewKeyName(e.target.value)}
                  placeholder="e.g. Production API"
                />
              </div>
              <div className="flex justify-end gap-2">
                <Button variant="secondary" onClick={() => setShowKeyModal(false)}>
                  Cancel
                </Button>
                <Button onClick={handleCreateKey} disabled={creatingKey || !newKeyName}>
                  {creatingKey ? 'Creating...' : 'Create'}
                </Button>
              </div>
            </>
          )}
        </div>
      </Modal>

      {/* Add Admin Modal */}
      <Modal open={showAdminModal} onClose={() => setShowAdminModal(false)} title="Add Admin">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Admin Email</label>
            <Input
              value={adminEmail}
              onChange={(e) => setAdminEmail(e.target.value)}
              placeholder="admin@example.com"
              type="email"
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setShowAdminModal(false)}>
              Cancel
            </Button>
            <Button onClick={handleAddAdmin} disabled={addingAdmin || !adminEmail}>
              {addingAdmin ? 'Adding...' : 'Add Admin'}
            </Button>
          </div>
        </div>
      </Modal>
    </AdminLayout>
  );
}
