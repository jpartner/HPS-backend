'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Save, ArrowLeft, Plus, Trash2 } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { adminReferenceListApi, type ReferenceListItem } from '@/lib/api';
import { useLanguages } from '@/lib/use-languages';

export default function ReferenceListDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const { languages } = useLanguages();
  const langCodes = languages.map((l) => l.code);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [name, setName] = useState('');
  const [key, setKey] = useState('');
  const [items, setItems] = useState<ReferenceListItem[]>([]);

  const fetchList = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminReferenceListApi.get(params.id);
      setName(data.name);
      setKey(data.key);
      setItems(data.items);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load');
    } finally {
      setLoading(false);
    }
  }, [params.id]);

  useEffect(() => { fetchList(); }, [fetchList]);

  function addItem() {
    setItems([...items, {
      value: '',
      sortOrder: items.length,
      isActive: true,
      translations: langCodes.map(lang => ({ lang, label: '' })),
    }]);
  }

  function removeItem(index: number) {
    setItems(items.filter((_, i) => i !== index));
  }

  function updateItemValue(index: number, value: string) {
    const updated = [...items];
    updated[index] = { ...updated[index], value };
    setItems(updated);
  }

  function updateItemLabel(index: number, lang: string, label: string) {
    const updated = [...items];
    const item = { ...updated[index] };
    const translations = [...item.translations];
    const existing = translations.findIndex(t => t.lang === lang);
    if (existing >= 0) {
      translations[existing] = { lang, label };
    } else {
      translations.push({ lang, label });
    }
    item.translations = translations;
    updated[index] = item;
    setItems(updated);
  }

  function getLabel(item: ReferenceListItem, lang: string): string {
    return item.translations.find(t => t.lang === lang)?.label ?? '';
  }

  async function handleSave() {
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      // Filter out empty translations and set sort orders
      const cleanItems = items.map((item, idx) => ({
        ...item,
        sortOrder: idx,
        translations: item.translations.filter(t => t.label.trim()),
      }));
      await adminReferenceListApi.update(params.id, { name, items: cleanItems });
      setSuccess('Reference list saved.');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to save');
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <AdminLayout><div className="flex items-center justify-center h-64 text-gray-500">Loading...</div></AdminLayout>;
  }

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button onClick={() => router.push('/reference-lists')} className="text-gray-400 hover:text-gray-600">
              <ArrowLeft className="h-5 w-5" />
            </button>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Edit Reference List</h1>
              <p className="text-sm text-gray-500 mt-1 font-mono">{key}</p>
            </div>
          </div>
          <Button onClick={handleSave} disabled={saving}>
            <Save className="h-4 w-4 mr-2" />{saving ? 'Saving...' : 'Save'}
          </Button>
        </div>

        {error && <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>}
        {success && <div className="p-3 rounded bg-green-50 border border-green-200 text-green-700 text-sm">{success}</div>}

        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-1">Display Name</label>
            <Input value={name} onChange={(e) => setName(e.target.value)} />
          </div>

          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Items ({items.length})</h2>
            <Button variant="secondary" onClick={addItem}>
              <Plus className="h-4 w-4 mr-2" />Add Item
            </Button>
          </div>

          {items.length === 0 ? (
            <p className="text-sm text-gray-500 text-center py-8">No items yet. Click &quot;Add Item&quot; to start.</p>
          ) : (
            <div className="border border-gray-200 rounded-lg overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-gray-50 border-b border-gray-200">
                    <th className="text-left px-3 py-2 font-medium text-gray-500 w-32">Value</th>
                    {langCodes.map(lang => (
                      <th key={lang} className="text-left px-3 py-2 font-medium text-gray-500">{lang.toUpperCase()}</th>
                    ))}
                    <th className="w-10"></th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((item, idx) => (
                    <tr key={idx} className="border-b border-gray-100">
                      <td className="px-3 py-2">
                        <Input
                          value={item.value}
                          onChange={(e) => updateItemValue(idx, e.target.value)}
                          placeholder="value"
                        />
                      </td>
                      {langCodes.map(lang => (
                        <td key={lang} className="px-3 py-2">
                          <Input
                            value={getLabel(item, lang)}
                            onChange={(e) => updateItemLabel(idx, lang, e.target.value)}
                            placeholder={`${lang} label`}
                          />
                        </td>
                      ))}
                      <td className="px-3 py-2">
                        <button onClick={() => removeItem(idx)} className="text-gray-400 hover:text-red-600">
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </AdminLayout>
  );
}
