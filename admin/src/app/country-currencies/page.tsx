'use client';

import { useState, useEffect, useCallback } from 'react';
import { Pencil } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { DataTable } from '@/components/ui/DataTable';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { adminCountryCurrencyApi, type CountryCurrency } from '@/lib/api';

export default function CountryCurrenciesPage() {
  const [items, setItems] = useState<CountryCurrency[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<CountryCurrency | null>(null);
  const [formPrimary, setFormPrimary] = useState('');
  const [formSecondary, setFormSecondary] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchItems = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminCountryCurrencyApi.list();
      setItems(Array.isArray(data) ? data : []);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchItems(); }, [fetchItems]);

  function openEdit(item: CountryCurrency) {
    setEditing(item);
    setFormPrimary(item.primaryCurrency);
    setFormSecondary(item.secondaryCurrency ?? '');
    setShowModal(true);
  }

  function closeModal() {
    setShowModal(false);
    setEditing(null);
    setFormPrimary('');
    setFormSecondary('');
  }

  async function handleSave() {
    if (!editing) return;
    setSubmitting(true);
    try {
      await adminCountryCurrencyApi.upsert(editing.countryId, {
        primaryCurrency: formPrimary.toUpperCase().trim(),
        secondaryCurrency: formSecondary.trim() ? formSecondary.toUpperCase().trim() : null,
      });
      closeModal();
      fetchItems();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to save');
    } finally {
      setSubmitting(false);
    }
  }

  const columns = [
    { key: 'isoCode', label: 'Code', render: (r: CountryCurrency) => <span className="font-mono font-medium">{r.isoCode}</span> },
    { key: 'countryName', label: 'Country' },
    { key: 'primaryCurrency', label: 'Primary Currency', render: (r: CountryCurrency) => <span className="font-mono">{r.primaryCurrency}</span> },
    { key: 'secondaryCurrency', label: 'Secondary Currency', render: (r: CountryCurrency) => r.secondaryCurrency ? <span className="font-mono">{r.secondaryCurrency}</span> : <span className="text-slate-400">-</span> },
    {
      key: 'actions', label: '', render: (r: CountryCurrency) => (
        <div className="flex justify-end">
          <button onClick={(e) => { e.stopPropagation(); openEdit(r); }} className="text-slate-400 hover:text-primary">
            <Pencil className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Country Currencies</h1>
          <p className="text-sm text-gray-500 mt-1">Manage primary and secondary currencies for each country.</p>
        </div>

        {error && <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>}

        <DataTable
          columns={columns as any}
          data={items}
          isLoading={loading}
          emptyMessage="No country currencies configured"
        />
      </div>

      <Modal open={showModal} onClose={closeModal} title={editing ? `Edit ${editing.countryName} (${editing.isoCode})` : 'Edit Currency'}>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Primary Currency</label>
            <Input value={formPrimary} onChange={(e) => setFormPrimary(e.target.value)} placeholder="e.g. EUR" maxLength={3} />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Secondary Currency (optional)</label>
            <Input value={formSecondary} onChange={(e) => setFormSecondary(e.target.value)} placeholder="e.g. USD" maxLength={3} />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={closeModal}>Cancel</Button>
            <Button onClick={handleSave} disabled={submitting || !formPrimary.trim()}>
              {submitting ? 'Saving...' : 'Save'}
            </Button>
          </div>
        </div>
      </Modal>
    </AdminLayout>
  );
}
