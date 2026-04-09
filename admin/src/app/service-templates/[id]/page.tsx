'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Save, Trash2, ArrowLeft } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { TranslationForm, type TranslationData } from '@/components/ui/TranslationForm';
import { adminTemplateApi, adminCategoryApi, type AdminCategoryDto } from '@/lib/api';

function translationName(translations: { lang: string; name: string }[]): string {
  return translations.find((t) => t.lang === 'en')?.name ?? translations[0]?.name ?? '';
}

export default function ServiceTemplateDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [translations, setTranslations] = useState<TranslationData>({});
  const [slug, setSlug] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [durationH, setDurationH] = useState('1');
  const [durationM, setDurationM] = useState('0');
  const [sortOrder, setSortOrder] = useState('0');
  const [isActive, setIsActive] = useState(true);
  const [categories, setCategories] = useState<{ id: string; name: string }[]>([]);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const [tmpl, cats] = await Promise.all([
        adminTemplateApi.get(params.id),
        adminCategoryApi.list(),
      ]);
      // Convert array translations to TranslationData record
      const tData: TranslationData = {};
      for (const t of tmpl.translations) {
        tData[t.lang] = { title: t.title || '', description: t.description || '' };
      }
      setTranslations(tData);
      setSlug(tmpl.slug || '');
      setCategoryId(tmpl.categoryId || '');
      const mins = tmpl.defaultDurationMinutes ?? 60;
      setDurationH(String(Math.floor(mins / 60)));
      setDurationM(String(mins % 60));
      setSortOrder(String(tmpl.sortOrder ?? 0));
      setIsActive(tmpl.isActive !== false);

      const flatCats: { id: string; name: string }[] = [];
      function flatten(list: AdminCategoryDto[], prefix = '') {
        for (const c of list) {
          const name = translationName(c.translations);
          flatCats.push({ id: c.id, name: prefix + name });
          if (c.children?.length) flatten(c.children, prefix + '  ');
        }
      }
      flatten(Array.isArray(cats) ? cats : []);
      setCategories(flatCats);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load template');
    } finally {
      setLoading(false);
    }
  }, [params.id]);

  useEffect(() => { fetchData(); }, [fetchData]);

  async function handleSave() {
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const translationsList = Object.entries(translations)
        .filter(([, fields]) => fields.title || fields.description)
        .map(([lang, fields]) => ({
          lang,
          title: fields.title || '',
          description: fields.description || undefined,
        }));

      const totalMinutes = (parseInt(durationH, 10) || 0) * 60 + (parseInt(durationM, 10) || 0);

      await adminTemplateApi.update(params.id, {
        defaultDurationMinutes: totalMinutes || null,
        sortOrder: parseInt(sortOrder, 10) || 0,
        isActive,
        translations: translationsList,
      });
      setSuccess('Template updated.');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to update template');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!confirm('Delete this service template?')) return;
    try {
      await adminTemplateApi.delete(params.id);
      router.push('/service-templates');
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
            <button onClick={() => router.push('/service-templates')} className="text-gray-400 hover:text-gray-600">
              <ArrowLeft className="h-5 w-5" />
            </button>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Edit Service Template</h1>
              <p className="text-sm text-gray-500 mt-1">{slug}</p>
            </div>
          </div>
          <Button variant="danger" onClick={handleDelete}>
            <Trash2 className="h-4 w-4 mr-2" />Delete
          </Button>
        </div>

        {error && <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>}
        {success && <div className="p-3 rounded bg-green-50 border border-green-200 text-green-700 text-sm">{success}</div>}

        <div className="bg-white rounded-lg border border-gray-200 p-6 space-y-4">
          <TranslationForm
            fields={[
              { key: 'title', label: 'Title', required: true },
              { key: 'description', label: 'Description', type: 'textarea' },
            ]}
            value={translations}
            onChange={setTranslations}
          />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
              <select
                value={categoryId}
                disabled
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm bg-gray-50"
              >
                {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Slug</label>
              <Input value={slug} disabled />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Default Duration</label>
              <div className="flex items-center gap-2">
                <Input type="number" value={durationH} onChange={(e) => setDurationH(e.target.value)} className="w-20" min="0" />
                <span className="text-sm text-gray-500">h</span>
                <Input type="number" value={durationM} onChange={(e) => setDurationM(e.target.value)} className="w-20" min="0" max="59" />
                <span className="text-sm text-gray-500">min</span>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Sort Order</label>
              <Input type="number" value={sortOrder} onChange={(e) => setSortOrder(e.target.value)} />
            </div>
          </div>

          <div className="flex items-center gap-2">
            <input id="active" type="checkbox" checked={isActive} onChange={(e) => setIsActive(e.target.checked)}
              className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" />
            <label htmlFor="active" className="text-sm font-medium text-gray-700">Active</label>
          </div>

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
