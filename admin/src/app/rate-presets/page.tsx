'use client';

import { useState, useEffect, useCallback } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { DataTable } from '@/components/ui/DataTable';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { adminRateDurationPresetApi, type RateDurationPreset } from '@/lib/api';

function formatDuration(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m}min`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}min`;
}

export default function RatePresetsPage() {
  const [items, setItems] = useState<RateDurationPreset[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [showModal, setShowModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formDuration, setFormDuration] = useState('');
  const [formLabel, setFormLabel] = useState('');
  const [formSortOrder, setFormSortOrder] = useState('0');

  const fetchItems = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminRateDurationPresetApi.list();
      setItems(Array.isArray(data) ? data : []);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchItems(); }, [fetchItems]);

  function closeModal() {
    setShowModal(false);
    setFormDuration('');
    setFormLabel('');
    setFormSortOrder('0');
  }

  async function handleCreate() {
    setSubmitting(true);
    try {
      await adminRateDurationPresetApi.create({
        durationMinutes: parseInt(formDuration, 10),
        label: formLabel || undefined,
        sortOrder: parseInt(formSortOrder, 10) || 0,
      });
      closeModal();
      fetchItems();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to create');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id: string, e: React.MouseEvent) {
    e.stopPropagation();
    if (!confirm('Deactivate this preset?')) return;
    try {
      await adminRateDurationPresetApi.delete(id);
      fetchItems();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to delete');
    }
  }

  async function handleToggleActive(item: RateDurationPreset, e: React.MouseEvent) {
    e.stopPropagation();
    try {
      await adminRateDurationPresetApi.update(item.id, { isActive: !item.isActive });
      fetchItems();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to update');
    }
  }

  const columns = [
    {
      key: 'durationMinutes', label: 'Duration', render: (r: RateDurationPreset) => (
        <span className="font-medium">{formatDuration(r.durationMinutes)}</span>
      ),
    },
    {
      key: 'label', label: 'Label', render: (r: RateDurationPreset) => (
        <span className="text-gray-600">{r.label || '--'}</span>
      ),
    },
    { key: 'sortOrder', label: 'Sort Order' },
    {
      key: 'isActive', label: 'Status', render: (r: RateDurationPreset) => (
        <button onClick={(e) => handleToggleActive(r, e)}>
          <Badge variant={r.isActive ? 'success' : 'default'}>{r.isActive ? 'Active' : 'Inactive'}</Badge>
        </button>
      ),
    },
    {
      key: 'actions', label: '', render: (r: RateDurationPreset) => (
        <div className="flex justify-end">
          <button onClick={(e) => handleDelete(r.id, e)} className="text-gray-400 hover:text-red-600">
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
            <h1 className="text-2xl font-bold text-gray-900">Rate Duration Presets</h1>
            <p className="text-sm text-gray-500 mt-1">Configure available durations for provider rate cards.</p>
          </div>
          <Button onClick={() => setShowModal(true)}>
            <Plus className="h-4 w-4 mr-2" />Add Preset
          </Button>
        </div>

        {error && <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>}

        <DataTable
          columns={columns as any}
          data={items}
          isLoading={loading}
          emptyMessage="No rate presets defined"
        />
      </div>

      <Modal open={showModal} onClose={closeModal} title="Create Rate Preset">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Duration (minutes)</label>
            <Input type="number" value={formDuration} onChange={(e) => setFormDuration(e.target.value)} placeholder="e.g. 60" min={1} />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Label (optional)</label>
            <Input value={formLabel} onChange={(e) => setFormLabel(e.target.value)} placeholder="e.g. Overnight, Weekend" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Sort Order</label>
            <Input type="number" value={formSortOrder} onChange={(e) => setFormSortOrder(e.target.value)} placeholder="0" />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={closeModal}>Cancel</Button>
            <Button onClick={handleCreate} disabled={submitting || !formDuration}>
              {submitting ? 'Creating...' : 'Create'}
            </Button>
          </div>
        </div>
      </Modal>
    </AdminLayout>
  );
}
