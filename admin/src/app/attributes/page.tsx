'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Plus, ChevronDown, ChevronRight, Trash2 } from 'lucide-react';
import clsx from 'clsx';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { DataTable } from '@/components/ui/DataTable';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { TranslationForm } from '@/components/ui/TranslationForm';
import { adminAttributeApi } from '@/lib/api';

interface AttributeOption {
  value: string;
  label: Record<string, string>;
}

interface Attribute {
  id: string;
  domain: string;
  key: string;
  label: string;
  dataType: string;
  required: boolean;
  options: AttributeOption[];
}

type GroupedAttributes = Record<string, Attribute[]>;

const DATA_TYPES = ['TEXT', 'NUMBER', 'BOOLEAN', 'SELECT', 'MULTI_SELECT'];
const DOMAINS = ['PROVIDER', 'SERVICE', 'BOOKING', 'USER'];

export default function AttributesPage() {
  const router = useRouter();
  const [attributes, setAttributes] = useState<Attribute[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expandedDomains, setExpandedDomains] = useState<Set<string>>(new Set(DOMAINS));
  const [showModal, setShowModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Form state
  const [formDomain, setFormDomain] = useState('PROVIDER');
  const [formKey, setFormKey] = useState('');
  const [formDataType, setFormDataType] = useState('STRING');
  const [formRequired, setFormRequired] = useState(false);
  const [formLabels, setFormLabels] = useState<Record<string, string>>({});
  const [formHints, setFormHints] = useState<Record<string, string>>({});
  const [formOptions, setFormOptions] = useState<{ value: string; labels: Record<string, string> }[]>([]);

  const fetchAttributes = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminAttributeApi.list();
      setAttributes(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load attributes';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAttributes();
  }, [fetchAttributes]);

  function toggleDomain(domain: string) {
    setExpandedDomains((prev) => {
      const next = new Set(prev);
      if (next.has(domain)) next.delete(domain);
      else next.add(domain);
      return next;
    });
  }

  function resetForm() {
    setFormDomain('PROVIDER');
    setFormKey('');
    setFormDataType('STRING');
    setFormRequired(false);
    setFormLabels({});
    setFormHints({});
    setFormOptions([]);
  }

  function addOption() {
    setFormOptions([...formOptions, { value: '', labels: {} }]);
  }

  function updateOption(index: number, field: 'value' | 'labels', val: string | Record<string, string>) {
    const updated = [...formOptions];
    if (field === 'value') {
      updated[index] = { ...updated[index], value: val as string };
    } else {
      updated[index] = { ...updated[index], labels: val as Record<string, string> };
    }
    setFormOptions(updated);
  }

  function removeOption(index: number) {
    setFormOptions(formOptions.filter((_, i) => i !== index));
  }

  async function handleCreate() {
    setSubmitting(true);
    try {
      await adminAttributeApi.create({
        domain: formDomain,
        key: formKey,
        dataType: formDataType,
        required: formRequired,
        label: formLabels,
        hint: formHints,
        options: formOptions.map((o) => ({ value: o.value, label: o.labels })),
      });
      setShowModal(false);
      resetForm();
      fetchAttributes();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to create attribute';
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }

  const grouped: GroupedAttributes = {};
  for (const attr of attributes) {
    if (!grouped[attr.domain]) grouped[attr.domain] = [];
    grouped[attr.domain].push(attr);
  }

  const showOptionsField = formDataType === 'SELECT' || formDataType === 'MULTI_SELECT';

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Attributes</h1>
            <p className="text-sm text-gray-500 mt-1">Define attribute schemas for entities</p>
          </div>
          <Button onClick={() => { resetForm(); setShowModal(true); }}>
            <Plus className="h-4 w-4 mr-2" />
            Add Attribute
          </Button>
        </div>

        {error && (
          <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
        )}

        {loading ? (
          <div className="flex items-center justify-center h-32 text-gray-500">Loading...</div>
        ) : attributes.length === 0 ? (
          <div className="bg-white rounded-lg border border-gray-200 p-8 text-center text-gray-500">
            No attributes defined yet.
          </div>
        ) : (
          <div className="space-y-4">
            {Object.entries(grouped).map(([domain, attrs]) => (
              <div key={domain} className="bg-white rounded-lg border border-gray-200">
                <button
                  onClick={() => toggleDomain(domain)}
                  className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-gray-50"
                >
                  <div className="flex items-center gap-2">
                    {expandedDomains.has(domain) ? (
                      <ChevronDown className="h-4 w-4 text-gray-400" />
                    ) : (
                      <ChevronRight className="h-4 w-4 text-gray-400" />
                    )}
                    <span className="font-semibold text-gray-900">{domain}</span>
                    <Badge variant="secondary">{attrs.length}</Badge>
                  </div>
                </button>

                {expandedDomains.has(domain) && (
                  <div className="border-t border-gray-100">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b border-gray-100 bg-gray-50">
                          <th className="text-left px-4 py-2 font-medium text-gray-500">Key</th>
                          <th className="text-left px-4 py-2 font-medium text-gray-500">Label</th>
                          <th className="text-left px-4 py-2 font-medium text-gray-500">Data Type</th>
                          <th className="text-left px-4 py-2 font-medium text-gray-500">Required</th>
                          <th className="text-left px-4 py-2 font-medium text-gray-500">Options</th>
                        </tr>
                      </thead>
                      <tbody>
                        {attrs.map((attr) => (
                          <tr
                            key={attr.id}
                            className="border-b border-gray-50 hover:bg-gray-50 cursor-pointer"
                            onClick={() => router.push(`/attributes/${attr.id}`)}
                          >
                            <td className="px-4 py-2 font-mono text-xs text-gray-700">{attr.key}</td>
                            <td className="px-4 py-2 text-gray-900">{attr.label}</td>
                            <td className="px-4 py-2">
                              <Badge variant="secondary">{attr.dataType}</Badge>
                            </td>
                            <td className="px-4 py-2">
                              {attr.required ? (
                                <span className="text-indigo-600 font-medium">Yes</span>
                              ) : (
                                <span className="text-gray-400">No</span>
                              )}
                            </td>
                            <td className="px-4 py-2 text-gray-500">
                              {(attr.options || []).length > 0
                                ? `${attr.options.length} options`
                                : '--'}
                            </td>
                            <td className="px-4 py-2">
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  if (confirm('Delete this attribute?')) {
                                    adminAttributeApi.delete(attr.id).then(fetchAttributes);
                                  }
                                }}
                                className="text-gray-400 hover:text-red-600"
                              >
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
            ))}
          </div>
        )}
      </div>

      <Modal open={showModal} onClose={() => setShowModal(false)} title="Add Attribute">
        <div className="space-y-4 max-h-[70vh] overflow-y-auto">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Domain</label>
            <select
              value={formDomain}
              onChange={(e) => setFormDomain(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              {DOMAINS.map((d) => (
                <option key={d} value={d}>{d}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Key</label>
            <Input value={formKey} onChange={(e) => setFormKey(e.target.value)} placeholder="attribute_key" />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Data Type</label>
            <select
              value={formDataType}
              onChange={(e) => setFormDataType(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              {DATA_TYPES.map((dt) => (
                <option key={dt} value={dt}>{dt}</option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-2">
            <input
              id="required"
              type="checkbox"
              checked={formRequired}
              onChange={(e) => setFormRequired(e.target.checked)}
              className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
            />
            <label htmlFor="required" className="text-sm font-medium text-gray-700">Required</label>
          </div>

          <TranslationForm label="Label" values={formLabels} onChange={setFormLabels} />
          <TranslationForm label="Hint" values={formHints} onChange={setFormHints} />

          {showOptionsField && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <label className="block text-sm font-medium text-gray-700">Options</label>
                <button
                  type="button"
                  onClick={addOption}
                  className="text-sm text-indigo-600 hover:text-indigo-800"
                >
                  + Add Option
                </button>
              </div>
              {formOptions.map((opt, idx) => (
                <div key={idx} className="border border-gray-200 rounded-md p-3 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-gray-500 font-medium">Option {idx + 1}</span>
                    <button
                      type="button"
                      onClick={() => removeOption(idx)}
                      className="text-xs text-red-500 hover:text-red-700"
                    >
                      Remove
                    </button>
                  </div>
                  <Input
                    value={opt.value}
                    onChange={(e) => updateOption(idx, 'value', e.target.value)}
                    placeholder="Option value"
                  />
                  <TranslationForm
                    label="Option Label"
                    values={opt.labels}
                    onChange={(labels) => updateOption(idx, 'labels', labels)}
                  />
                </div>
              ))}
            </div>
          )}

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => setShowModal(false)}>Cancel</Button>
            <Button onClick={handleCreate} disabled={submitting}>
              {submitting ? 'Creating...' : 'Create Attribute'}
            </Button>
          </div>
        </div>
      </Modal>
    </AdminLayout>
  );
}
