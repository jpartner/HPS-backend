'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Save, Trash2, ArrowLeft, Plus, X } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { TranslationForm } from '@/components/ui/TranslationForm';
import { Badge } from '@/components/ui/Badge';
import { adminAttributeApi, adminReferenceListApi, type ReferenceListData } from '@/lib/api';

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

  // Validation rules (for NUMBER)
  const [validMin, setValidMin] = useState('');
  const [validMax, setValidMax] = useState('');

  // Options (for SELECT/MULTI_SELECT)
  const [options, setOptions] = useState<string[]>([]);
  const [newOption, setNewOption] = useState('');

  // Reference list linking
  const [referenceListId, setReferenceListId] = useState<string>('');
  const [referenceListKey, setReferenceListKey] = useState<string>('');
  const [availableRefLists, setAvailableRefLists] = useState<ReferenceListData[]>([]);
  const [useRefList, setUseRefList] = useState(false);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const [data, refLists] = await Promise.all([
        adminAttributeApi.get(params.id),
        adminReferenceListApi.list().catch(() => []),
      ]);
      const raw = data as any;
      setDomain(raw.domain || '');
      setKey(raw.key || '');
      setDataType(raw.dataType || '');
      setIsRequired(raw.isRequired === true);
      setIsActive(raw.isActive !== false);
      setSortOrder(String(raw.sortOrder ?? 0));
      setLabels(arrToRecord(raw.translations, 'label'));
      setHints(arrToRecord(raw.translations, 'hint'));
      setOptions(raw.options || []);

      // Validation
      if (raw.validation) {
        setValidMin(raw.validation.min != null ? String(raw.validation.min) : '');
        setValidMax(raw.validation.max != null ? String(raw.validation.max) : '');
      }

      // Reference list
      if (raw.referenceListId) {
        setReferenceListId(raw.referenceListId);
        setReferenceListKey(raw.referenceListKey || '');
        setUseRefList(true);
      }
      setAvailableRefLists(Array.isArray(refLists) ? refLists : []);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load attribute');
    } finally {
      setLoading(false);
    }
  }, [params.id]);

  useEffect(() => { fetchData(); }, [fetchData]);

  function addOption() {
    if (newOption.trim() && !options.includes(newOption.trim())) {
      setOptions([...options, newOption.trim()]);
      setNewOption('');
    }
  }

  function removeOption(index: number) {
    setOptions(options.filter((_, i) => i !== index));
  }

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

      const validation: any = {};
      if (dataType === 'NUMBER') {
        if (validMin) validation.min = parseFloat(validMin);
        if (validMax) validation.max = parseFloat(validMax);
      }

      const payload: any = {
        isRequired,
        isActive,
        sortOrder: parseInt(sortOrder, 10) || 0,
        translations,
        validation: Object.keys(validation).length > 0 ? validation : undefined,
      };

      const isSelectType = dataType === 'SELECT' || dataType === 'MULTI_SELECT';
      if (isSelectType) {
        if (useRefList && referenceListId) {
          payload.referenceListId = referenceListId;
        } else {
          payload.clearReferenceList = true;
          payload.options = options;
        }
      }

      await adminAttributeApi.update(params.id, payload);
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

  const isSelectType = dataType === 'SELECT' || dataType === 'MULTI_SELECT';
  const isNumberType = dataType === 'NUMBER';

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
                <Badge variant="secondary" className="ml-2">{dataType}</Badge>
              </p>
            </div>
          </div>
          <Button variant="danger" onClick={handleDelete}>
            <Trash2 className="h-4 w-4 mr-2" />Delete
          </Button>
        </div>

        {error && <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>}
        {success && <div className="p-3 rounded bg-green-50 border border-green-200 text-green-700 text-sm">{success}</div>}

        <div className="bg-white rounded-lg border border-gray-200 p-6 space-y-6">
          <TranslationForm label="Label" values={labels} onChange={setLabels} />
          <TranslationForm label="Hint" values={hints} onChange={setHints} />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Sort Order</label>
              <Input type="number" value={sortOrder} onChange={(e) => setSortOrder(e.target.value)} />
            </div>
          </div>

          <div className="flex items-center gap-6">
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

          {/* Validation rules for NUMBER */}
          {isNumberType && (
            <div>
              <h3 className="text-sm font-semibold text-gray-900 mb-3">Validation Rules</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Minimum</label>
                  <Input type="number" value={validMin} onChange={(e) => setValidMin(e.target.value)} placeholder="No minimum" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Maximum</label>
                  <Input type="number" value={validMax} onChange={(e) => setValidMax(e.target.value)} placeholder="No maximum" />
                </div>
              </div>
            </div>
          )}

          {/* Options for SELECT/MULTI_SELECT */}
          {isSelectType && (
            <div>
              <h3 className="text-sm font-semibold text-gray-900 mb-3">Options Source</h3>

              <div className="flex gap-4 mb-4">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input type="radio" checked={!useRefList} onChange={() => { setUseRefList(false); setReferenceListId(''); }}
                    className="h-4 w-4 text-indigo-600 focus:ring-indigo-500" />
                  <span className="text-sm text-gray-700">Inline options</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input type="radio" checked={useRefList} onChange={() => setUseRefList(true)}
                    className="h-4 w-4 text-indigo-600 focus:ring-indigo-500" />
                  <span className="text-sm text-gray-700">Reference list</span>
                </label>
              </div>

              {useRefList ? (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Reference List</label>
                  <select
                    value={referenceListId}
                    onChange={(e) => setReferenceListId(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  >
                    <option value="">Select a reference list...</option>
                    {availableRefLists.map((rl) => (
                      <option key={rl.id} value={rl.id}>{rl.name} ({rl.key})</option>
                    ))}
                  </select>
                  {referenceListKey && !referenceListId && (
                    <p className="text-xs text-gray-500 mt-1">Currently linked to: {referenceListKey}</p>
                  )}
                </div>
              ) : (
                <div>
                  <div className="flex flex-wrap gap-2 mb-3">
                    {options.map((opt, i) => (
                      <span key={i} className="inline-flex items-center gap-1 px-2 py-1 bg-gray-100 rounded text-sm">
                        {opt}
                        <button onClick={() => removeOption(i)} className="text-gray-400 hover:text-red-600">
                          <X className="h-3 w-3" />
                        </button>
                      </span>
                    ))}
                  </div>
                  <div className="flex gap-2">
                    <Input
                      value={newOption}
                      onChange={(e) => setNewOption(e.target.value)}
                      placeholder="Add option..."
                      onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); addOption(); } }}
                    />
                    <Button variant="secondary" onClick={addOption} disabled={!newOption.trim()}>
                      <Plus className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}

          <div className="flex justify-end pt-4 border-t border-gray-100">
            <Button onClick={handleSave} disabled={saving}>
              <Save className="h-4 w-4 mr-2" />{saving ? 'Saving...' : 'Save Changes'}
            </Button>
          </div>
        </div>
      </div>
    </AdminLayout>
  );
}
