'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Plus } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { DataTable } from '@/components/ui/DataTable';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { TranslationForm } from '@/components/ui/TranslationForm';
import { TranslationData } from '@/components/ui/TranslationForm';
import { adminTemplateApi, adminCategoryApi, type AdminTemplateDto, type AdminCategoryDto } from '@/lib/api';

interface CategoryOption {
  id: string;
  name: string;
}

function translationName(translations: { lang: string; name: string }[]): string {
  return translations.find((t) => t.lang === 'en')?.name ?? translations[0]?.name ?? '';
}

function translationTitle(translations: { lang: string; title: string }[]): string {
  return translations.find((t) => t.lang === 'en')?.title ?? translations[0]?.title ?? '';
}

function formatDuration(minutes: number | null): string {
  if (minutes == null) return '--';
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m}min`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}min`;
}

export default function ServiceTemplatesPage() {
  const router = useRouter();
  const [templates, setTemplates] = useState<AdminTemplateDto[]>([]);
  const [categories, setCategories] = useState<CategoryOption[]>([]);
  const [categoryMap, setCategoryMap] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Form state
  const [formTranslations, setFormTranslations] = useState<TranslationData>({});
  const [formCategoryId, setFormCategoryId] = useState('');
  const [formSlug, setFormSlug] = useState('');
  const [formDurationH, setFormDurationH] = useState('1');
  const [formDurationM, setFormDurationM] = useState('0');

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const [templatesData, categoriesData] = await Promise.all([
        adminTemplateApi.list(),
        adminCategoryApi.list(),
      ]);
      setTemplates(templatesData);

      // Flatten categories for the select and build a lookup map
      const flatCats: CategoryOption[] = [];
      const catMap: Record<string, string> = {};
      function flatten(cats: AdminCategoryDto[], prefix = '') {
        for (const cat of cats) {
          const name = translationName(cat.translations);
          flatCats.push({ id: cat.id, name: prefix + name });
          catMap[cat.id] = name;
          if (cat.children?.length) flatten(cat.children, prefix + '  ');
        }
      }
      flatten(categoriesData);
      setCategories(flatCats);
      setCategoryMap(catMap);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load data';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  function resetForm() {
    setFormTranslations({});
    setFormCategoryId('');
    setFormSlug('');
    setFormDurationH('1');
    setFormDurationM('0');
  }

  async function handleCreate() {
    setSubmitting(true);
    setError('');
    try {
      // Build translations array from the multi-field data
      const translations = Object.entries(formTranslations)
        .filter(([, fields]) => fields.title || fields.description)
        .map(([lang, fields]) => ({
          lang,
          title: fields.title || '',
          description: fields.description || undefined,
        }));

      const totalMinutes = (parseInt(formDurationH, 10) || 0) * 60 + (parseInt(formDurationM, 10) || 0);

      await adminTemplateApi.create({
        slug: formSlug,
        categoryId: formCategoryId,
        defaultDurationMinutes: totalMinutes || null,
        translations,
      });
      setShowModal(false);
      resetForm();
      fetchData();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to create template';
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }

  const columns = [
    {
      key: 'title',
      label: 'Title',
      render: (t: AdminTemplateDto) => translationTitle(t.translations) || <span className="text-gray-400 italic">No title</span>,
    },
    {
      key: 'category',
      label: 'Category',
      render: (t: AdminTemplateDto) => categoryMap[t.categoryId] || <span className="text-gray-400">--</span>,
    },
    {
      key: 'slug',
      label: 'Slug',
      render: (t: AdminTemplateDto) => <span className="font-mono text-xs text-gray-500">{t.slug}</span>,
    },
    {
      key: 'defaultDurationMinutes',
      label: 'Duration',
      render: (t: AdminTemplateDto) => <span>{formatDuration(t.defaultDurationMinutes)}</span>,
    },
    {
      key: 'isActive',
      label: 'Status',
      render: (t: AdminTemplateDto) => (
        <Badge variant={t.isActive ? 'success' : 'secondary'}>{t.isActive ? 'Active' : 'Inactive'}</Badge>
      ),
    },
  ];

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Service Templates</h1>
            <p className="text-sm text-gray-500 mt-1">Manage service template definitions</p>
          </div>
          <Button onClick={() => { resetForm(); setShowModal(true); }}>
            <Plus className="h-4 w-4 mr-2" />
            Add Template
          </Button>
        </div>

        {error && (
          <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
        )}

        <DataTable
          columns={columns as any}
          data={templates as any}
          isLoading={loading}
          onRowClick={(template: AdminTemplateDto) => router.push(`/service-templates/${template.id}`)}
          emptyMessage="No service templates found"
        />
      </div>

      <Modal open={showModal} onClose={() => setShowModal(false)} title="Add Service Template">
        <div className="space-y-4">
          <TranslationForm
            fields={[
              { key: 'title', label: 'Title', required: true },
              { key: 'description', label: 'Description', type: 'textarea' },
            ]}
            value={formTranslations}
            onChange={setFormTranslations}
          />

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
            <select
              value={formCategoryId}
              onChange={(e) => setFormCategoryId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value="">Select category</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>{cat.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Slug</label>
            <Input value={formSlug} onChange={(e) => setFormSlug(e.target.value)} placeholder="template-slug" />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Default Duration</label>
            <div className="flex items-center gap-2">
              <Input
                type="number"
                value={formDurationH}
                onChange={(e) => setFormDurationH(e.target.value)}
                className="w-20"
                min="0"
              />
              <span className="text-sm text-gray-500">hours</span>
              <Input
                type="number"
                value={formDurationM}
                onChange={(e) => setFormDurationM(e.target.value)}
                className="w-20"
                min="0"
                max="59"
              />
              <span className="text-sm text-gray-500">minutes</span>
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => setShowModal(false)}>Cancel</Button>
            <Button onClick={handleCreate} disabled={submitting || !formCategoryId || !formSlug}>
              {submitting ? 'Creating...' : 'Create Template'}
            </Button>
          </div>
        </div>
      </Modal>
    </AdminLayout>
  );
}
