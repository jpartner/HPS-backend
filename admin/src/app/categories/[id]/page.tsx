'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Save, Trash2, ArrowLeft } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { TranslationForm } from '@/components/ui/TranslationForm';
import { adminCategoryApi, type Category } from '@/lib/api';

function translationsToRecord(translations: Category['translations']): Record<string, string> {
  const result: Record<string, string> = {};
  if (translations) {
    for (const [lang, t] of Object.entries(translations)) {
      if (t?.title) result[lang] = t.title;
    }
  }
  return result;
}

export default function CategoryDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();

  const [category, setCategory] = useState<Category | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Form fields
  const [names, setNames] = useState<Record<string, string>>({});
  const [slug, setSlug] = useState('');
  const [sortOrder, setSortOrder] = useState('0');

  const fetchCategory = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminCategoryApi.get(params.id);
      setCategory(data);

      // Backend returns translations as array [{lang, name, description}]
      // but our API types have it as Translations object
      const rawTranslations = (data as any).translations;
      if (Array.isArray(rawTranslations)) {
        const rec: Record<string, string> = {};
        for (const t of rawTranslations) {
          rec[t.lang] = t.name;
        }
        setNames(rec);
      } else {
        setNames(translationsToRecord(data.translations));
      }

      setSlug(data.slug || '');
      setSortOrder(String(data.sortOrder ?? 0));
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load category';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [params.id]);

  useEffect(() => {
    fetchCategory();
  }, [fetchCategory]);

  async function handleSave() {
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const translations = Object.entries(names)
        .filter(([, v]) => v.trim())
        .map(([lang, name]) => ({ lang, name }));

      await adminCategoryApi.update(params.id, {
        slug,
        sortOrder: parseInt(sortOrder, 10) || 0,
        translations,
      });
      setSuccess('Category updated successfully.');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to update category';
      setError(message);
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!confirm('Delete this category? This action cannot be undone.')) return;
    try {
      await adminCategoryApi.delete(params.id);
      router.push('/categories');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to delete category';
      setError(message);
    }
  }

  if (loading) {
    return (
      <AdminLayout>
        <div className="flex items-center justify-center h-64 text-gray-500">Loading category...</div>
      </AdminLayout>
    );
  }

  const children = category?.children || [];

  return (
    <AdminLayout>
      <div className="space-y-8">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button onClick={() => router.push('/categories')} className="text-gray-400 hover:text-gray-600">
              <ArrowLeft className="h-5 w-5" />
            </button>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Edit Category</h1>
              <p className="text-sm text-gray-500 mt-1">{slug}</p>
            </div>
          </div>
          <Button variant="danger" onClick={handleDelete}>
            <Trash2 className="h-4 w-4 mr-2" />
            Delete
          </Button>
        </div>

        {error && (
          <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
        )}
        {success && (
          <div className="p-3 rounded bg-green-50 border border-green-200 text-green-700 text-sm">{success}</div>
        )}

        <div className="bg-white rounded-lg border border-gray-200 p-6 space-y-4">
          <h2 className="text-lg font-semibold text-gray-900">Category Details</h2>

          <TranslationForm label="Name" values={names} onChange={setNames} />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Slug</label>
              <Input value={slug} onChange={(e) => setSlug(e.target.value)} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Sort Order</label>
              <Input type="number" value={sortOrder} onChange={(e) => setSortOrder(e.target.value)} />
            </div>
          </div>

          <div className="flex justify-end">
            <Button onClick={handleSave} disabled={saving}>
              <Save className="h-4 w-4 mr-2" />
              {saving ? 'Saving...' : 'Save Changes'}
            </Button>
          </div>
        </div>

        {children.length > 0 && (
          <div className="bg-white rounded-lg border border-gray-200 p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">
              Subcategories ({children.length})
            </h2>
            <div className="divide-y divide-gray-100">
              {children.map((child) => {
                const rawT = (child as any).translations;
                const childName = Array.isArray(rawT)
                  ? (rawT.find((t: any) => t.lang === 'en')?.name ?? rawT[0]?.name ?? child.slug)
                  : child.slug;
                return (
                  <div
                    key={child.id}
                    className="flex items-center justify-between py-3 cursor-pointer hover:bg-gray-50 px-2 rounded"
                    onClick={() => router.push(`/categories/${child.id}`)}
                  >
                    <div>
                      <p className="text-sm font-medium text-gray-900">{childName}</p>
                      <p className="text-xs text-gray-400 font-mono">{child.slug}</p>
                    </div>
                    <span className="text-xs text-gray-400">Order: {child.sortOrder}</span>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
