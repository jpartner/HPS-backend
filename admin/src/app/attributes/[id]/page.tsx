'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Save, Trash2, ArrowLeft } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { TranslationForm } from '@/components/ui/TranslationForm';
import { Badge } from '@/components/ui/Badge';
import { adminAttributeApi } from '@/lib/api';

function arrToRecord(translations: any[], field: string): Record<string, string> {
  const result: Record<string, string> = {};
  if (Array.isArray(translations)) {
    for (const t of translations) result[t.lang] = t[field] ?? '';
  }
  return result;
}

export default function AttributeDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [domain, setDomain] = useState('');
  const [key, setKey] = useState('');
  const [dataType, setDataType] = useState('');
  const [isRequired, setIsRequired] = useState(false);
  const [isActive, setIsActive] = useState(true);
  const [sortOrder, setSortOrder] = useState('0');
  const [labels, setLabels] = useState<Record<string, string>>({});
  const [hints, setHints] = useState<Record<string, string>>({});
  const [options, setOptions] = useState<string[]>([]);

  const fetchAttribute = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminAttributeApi.get(params.id) as any;
      setDomain(data.domain || '');
      setKey(data.key || '');
      setDataType(data.dataType || '');
      setIsRequired(data.isRequired === true);
      setIsActive(data.isActive !== false);
      setSortOrder(String(data.sortOrder ?? 0));
      setLabels(arrToRecord(data.translations, 'label'));
      setHints(arrToRecord(data.translations, 'hint'));
      setOptions(data.options || []);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load attribute');
    } finally {
      setLoading(false);
    }
  }, [params.id]);

  useEffect(() => { fetchAttribute(); }, [fetchAttribute]);

  async function handleSave() {
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const translations = Object.keys({ ...labels, ...hints }).reduce<any[]>((acc, lang) => {
        if (labels[lang] || hints[lang]) {
          acc.push({ lang, label: labels[lang] || '', hint: hints[lang] || '' });
        }
        return acc;
      }, []);
      await adminAttributeApi.update(params.id, {
        dataType,
        isRequired,
        isActive,
        sortOrder: parseInt(sortOrder, 10) || 0,
        options: options.length > 0 ? options : undefined,
        translations,
      });
      setSuccess('Attribute updated.');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to update');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!confirm('Delete this attribute?')) return;
    try {
      await adminAttributeApi.delete(params.id);
      router.push('/attributes');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to delete');
    }
  }

  if (loading) {
    return <AdminLayout><div className="flex items-center justify-center h-64 text-gray-500">Loading...</div></AdminLayout>;
  }

  return (
    <AdminLayout>
      <div className="space-y-8">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button onClick={() => router.push('/attributes')} className="text-gray-400 hover:text-gray-600">
              <ArrowLeft className="h-5 w-5" />
            </button>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Edit Attribute</h1>
              <p className="text-sm text-gray-500 mt-1">
                <Badge variant="secondary">{domain}</Badge>
                <span className="ml-2 font-mono">{key}</span>
              </p>
            </div>
          </div>
          <Button variant="danger" onClick={handleDelete}>
            <Trash2 className="h-4 w-4 mr-2" />Delete
          </Button>
        </div>

        {error && <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>}
        {success && <div className="p-3 rounded bg-green-50 border border-green-200 text-green-700 text-sm">{success}</div>}

        <div className="bg-white rounded-lg border border-gray-200 p-6 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Domain</label>
              <Input value={domain} disabled />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Key</label>
              <Input value={key} disabled />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Data Type</label>
              <Input value={dataType} disabled />
            </div>
          </div>

          <TranslationForm label="Label" values={labels} onChange={setLabels} />
          <TranslationForm label="Hint" values={hints} onChange={setHints} />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Sort Order</label>
              <Input type="number" value={sortOrder} onChange={(e) => setSortOrder(e.target.value)} />
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <input id="required" type="checkbox" checked={isRequired} onChange={(e) => setIsRequired(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" />
              <label htmlFor="required" className="text-sm font-medium text-gray-700">Required</label>
            </div>
            <div className="flex items-center gap-2">
              <input id="active" type="checkbox" checked={isActive} onChange={(e) => setIsActive(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" />
              <label htmlFor="active" className="text-sm font-medium text-gray-700">Active</label>
            </div>
          </div>

          {options.length > 0 && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Options</label>
              <div className="flex flex-wrap gap-2">
                {options.map((opt, i) => (
                  <Badge key={i} variant="secondary">{opt}</Badge>
                ))}
              </div>
            </div>
          )}

          <div className="flex justify-end">
            <Button onClick={handleSave} disabled={saving}>
              <Save className="h-4 w-4 mr-2" />{saving ? 'Saving...' : 'Save Changes'}
            </Button>
          </div>
        </div>
      </div>
    </AdminLayout>
  );
}
