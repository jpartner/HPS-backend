'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Plus, Trash2 } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { DataTable } from '@/components/ui/DataTable';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { adminReferenceListApi, type ReferenceListData } from '@/lib/api';

export default function ReferenceListsPage() {
  const router = useRouter();
  const [lists, setLists] = useState<ReferenceListData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formKey, setFormKey] = useState('');
  const [formName, setFormName] = useState('');

  const fetchLists = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminReferenceListApi.list();
      setLists(Array.isArray(data) ? data : []);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load reference lists');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchLists(); }, [fetchLists]);

  async function handleCreate() {
    setSubmitting(true);
    try {
      await adminReferenceListApi.create({ key: formKey, name: formName });
      setShowModal(false);
      setFormKey('');
      setFormName('');
      fetchLists();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to create');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id: string, e: React.MouseEvent) {
    e.stopPropagation();
    if (!confirm('Delete this reference list?')) return;
    try {
      await adminReferenceListApi.delete(id);
      fetchLists();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to delete');
    }
  }

  const columns = [
    { key: 'key', label: 'Key', render: (l: ReferenceListData) => <span className="font-mono text-xs">{l.key}</span> },
    { key: 'name', label: 'Name' },
    { key: 'items', label: 'Items', render: (l: ReferenceListData) => <Badge variant="secondary">{l.items.length}</Badge> },
    {
      key: 'actions', label: '', render: (l: ReferenceListData) => (
        <div className="flex justify-end">
          <button onClick={(e) => handleDelete(l.id, e)} className="text-gray-400 hover:text-red-600">
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
            <h1 className="text-2xl font-bold text-gray-900">Reference Lists</h1>
            <p className="text-sm text-gray-500 mt-1">System-wide lists for nationalities, languages, etc.</p>
          </div>
          <Button onClick={() => setShowModal(true)}>
            <Plus className="h-4 w-4 mr-2" />Add List
          </Button>
        </div>

        {error && <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>}

        <DataTable
          columns={columns as any}
          data={lists}
          isLoading={loading}
          onRowClick={(l: ReferenceListData) => router.push(`/reference-lists/${l.id}`)}
          emptyMessage="No reference lists defined"
        />
      </div>

      <Modal open={showModal} onClose={() => setShowModal(false)} title="Create Reference List">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Key</label>
            <Input value={formKey} onChange={(e) => setFormKey(e.target.value)} placeholder="e.g. nationalities" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Display Name</label>
            <Input value={formName} onChange={(e) => setFormName(e.target.value)} placeholder="e.g. Nationalities" />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => setShowModal(false)}>Cancel</Button>
            <Button onClick={handleCreate} disabled={submitting || !formKey || !formName}>
              {submitting ? 'Creating...' : 'Create'}
            </Button>
          </div>
        </div>
      </Modal>
    </AdminLayout>
  );
}
