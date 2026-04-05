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
import { adminTemplateApi, adminCategoryApi } from '@/lib/api';

interface ServiceTemplate {
  id: string;
  title: string;
  category: string;
  slug: string;
  defaultDuration: number;
  status: string;
}

interface CategoryOption {
  id: string;
  name: string;
}

export default function ServiceTemplatesPage() {
  const router = useRouter();
  const [templates, setTemplates] = useState<ServiceTemplate[]>([]);
  const [categories, setCategories] = useState<CategoryOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Form state
  const [formTitles, setFormTitles] = useState<Record<string, string>>({});
  const [formDescriptions, setFormDescriptions] = useState<Record<string, string>>({});
  const [formCategoryId, setFormCategoryId] = useState('');
  const [formSlug, setFormSlug] = useState('');
  const [formDuration, setFormDuration] = useState('60');

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const [templatesData, categoriesData] = await Promise.all([
        adminTemplateApi.list(),
        adminCategoryApi.list(),
      ]);
      setTemplates(templatesData);
      // Flatten categories for the select
      const flatCats: CategoryOption[] = [];
      function flatten(cats: { id: string; name: string; children?: { id: string; name: string; children?: unknown[] }[] }[], prefix = '') {
        for (const cat of cats) {
          flatCats.push({ id: cat.id, name: prefix + cat.name });
          if (cat.children) flatten(cat.children as { id: string; name: string; children?: { id: string; name: string; children?: unknown[] }[] }[], prefix + '  ');
        }
      }
      flatten(categoriesData);
      setCategories(flatCats);
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
    setFormTitles({});
    setFormDescriptions({});
    setFormCategoryId('');
    setFormSlug('');
    setFormDuration('60');
  }

  async function handleCreate() {
    setSubmitting(true);
    try {
      await adminTemplateApi.create({
        title: formTitles,
        description: formDescriptions,
        categoryId: formCategoryId,
        slug: formSlug,
        defaultDuration: parseInt(formDuration, 10) || 60,
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
    { key: 'title', label: 'Title' },
    { key: 'category', label: 'Category' },
    { key: 'slug', label: 'Slug', render: (t: ServiceTemplate) => <span className="font-mono text-xs text-gray-500">{t.slug}</span> },
    {
      key: 'defaultDuration',
      label: 'Duration',
      render: (t: ServiceTemplate) => <span>{t.defaultDuration} min</span>,
    },
    {
      key: 'status',
      label: 'Status',
      render: (t: ServiceTemplate) => (
        <Badge variant={t.status === 'active' ? 'success' : 'secondary'}>{t.status}</Badge>
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
          onRowClick={(template: ServiceTemplate) => router.push(`/service-templates/${template.id}`)}
          emptyMessage="No service templates found"
        />
      </div>

      <Modal open={showModal} onClose={() => setShowModal(false)} title="Add Service Template">
        <div className="space-y-4">
          <TranslationForm label="Title" values={formTitles} onChange={setFormTitles} />
          <TranslationForm label="Description" values={formDescriptions} onChange={setFormDescriptions} multiline />

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
            <label className="block text-sm font-medium text-gray-700 mb-1">Default Duration (minutes)</label>
            <Input type="number" value={formDuration} onChange={(e) => setFormDuration(e.target.value)} />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => setShowModal(false)}>Cancel</Button>
            <Button onClick={handleCreate} disabled={submitting}>
              {submitting ? 'Creating...' : 'Create Template'}
            </Button>
          </div>
        </div>
      </Modal>
    </AdminLayout>
  );
}
