'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Plus, ChevronRight, ChevronDown, GripVertical } from 'lucide-react';
import clsx from 'clsx';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { TranslationForm } from '@/components/ui/TranslationForm';
import { adminCategoryApi } from '@/lib/api';

interface Category {
  id: string;
  icon: string;
  name: string;
  slug: string;
  imageUrl?: string;
  sortOrder: number;
  children?: Category[];
}

export default function CategoriesPage() {
  const router = useRouter();
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expanded, setExpanded] = useState<Set<string>>(new Set());
  const [showModal, setShowModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Form state
  const [formNames, setFormNames] = useState<Record<string, string>>({});
  const [formIcon, setFormIcon] = useState('');
  const [formSlug, setFormSlug] = useState('');
  const [formParentId, setFormParentId] = useState('');
  const [formImageUrl, setFormImageUrl] = useState('');
  const [formSortOrder, setFormSortOrder] = useState('0');

  const fetchCategories = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminCategoryApi.list();
      setCategories(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load categories';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  function toggleExpand(id: string) {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  function resetForm() {
    setFormNames({});
    setFormIcon('');
    setFormSlug('');
    setFormParentId('');
    setFormImageUrl('');
    setFormSortOrder('0');
  }

  async function handleCreate() {
    setSubmitting(true);
    try {
      await adminCategoryApi.create({
        name: formNames,
        icon: formIcon,
        slug: formSlug,
        parentId: formParentId || undefined,
        imageUrl: formImageUrl || undefined,
        sortOrder: parseInt(formSortOrder, 10) || 0,
      });
      setShowModal(false);
      resetForm();
      fetchCategories();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to create category';
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }

  function flattenForSelect(cats: Category[], prefix = ''): { id: string; label: string }[] {
    const result: { id: string; label: string }[] = [];
    for (const cat of cats) {
      result.push({ id: cat.id, label: prefix + cat.name });
      if (cat.children) {
        result.push(...flattenForSelect(cat.children, prefix + '  '));
      }
    }
    return result;
  }

  function renderCategory(category: Category, depth = 0) {
    const hasChildren = (category.children || []).length > 0;
    const isExpanded = expanded.has(category.id);

    return (
      <div key={category.id}>
        <div
          className={clsx(
            'flex items-center gap-3 px-4 py-3 border-b border-gray-100 hover:bg-gray-50 cursor-pointer group',
          )}
          style={{ paddingLeft: `${depth * 24 + 16}px` }}
        >
          <GripVertical className="h-4 w-4 text-gray-300 opacity-0 group-hover:opacity-100" />

          {hasChildren ? (
            <button
              onClick={(e) => {
                e.stopPropagation();
                toggleExpand(category.id);
              }}
              className="text-gray-400 hover:text-gray-600"
            >
              {isExpanded ? (
                <ChevronDown className="h-4 w-4" />
              ) : (
                <ChevronRight className="h-4 w-4" />
              )}
            </button>
          ) : (
            <span className="w-4" />
          )}

          <span className="text-lg" title="icon">
            {category.icon || '--'}
          </span>

          <span
            className="flex-1 text-sm font-medium text-gray-900 hover:text-indigo-600"
            onClick={() => router.push(`/categories/${category.id}`)}
          >
            {category.name}
          </span>

          <span className="text-xs text-gray-400 font-mono">{category.slug}</span>

          <span className="text-xs text-gray-500 min-w-[80px] text-right">
            {(category.children || []).length} subcategories
          </span>
        </div>

        {isExpanded &&
          hasChildren &&
          (category.children || []).map((child) => renderCategory(child, depth + 1))}
      </div>
    );
  }

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Categories</h1>
            <p className="text-sm text-gray-500 mt-1">Manage service category hierarchy</p>
          </div>
          <Button onClick={() => { resetForm(); setShowModal(true); }}>
            <Plus className="h-4 w-4 mr-2" />
            Add Category
          </Button>
        </div>

        {error && (
          <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
        )}

        <div className="bg-white rounded-lg border border-gray-200">
          {loading ? (
            <div className="flex items-center justify-center h-32 text-gray-500">Loading...</div>
          ) : categories.length === 0 ? (
            <div className="flex items-center justify-center h-32 text-gray-500">
              No categories found
            </div>
          ) : (
            categories.map((cat) => renderCategory(cat))
          )}
        </div>
      </div>

      <Modal open={showModal} onClose={() => setShowModal(false)} title="Add Category">
        <div className="space-y-4">
          <TranslationForm
            label="Name"
            values={formNames}
            onChange={setFormNames}
          />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Icon</label>
            <Input
              value={formIcon}
              onChange={(e) => setFormIcon(e.target.value)}
              placeholder="e.g. emoji or icon name"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Slug</label>
            <Input
              value={formSlug}
              onChange={(e) => setFormSlug(e.target.value)}
              placeholder="category-slug"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Parent Category</label>
            <select
              value={formParentId}
              onChange={(e) => setFormParentId(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
              <option value="">None (top-level)</option>
              {flattenForSelect(categories).map((opt) => (
                <option key={opt.id} value={opt.id}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Image URL</label>
            <Input
              value={formImageUrl}
              onChange={(e) => setFormImageUrl(e.target.value)}
              placeholder="https://..."
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Sort Order</label>
            <Input
              type="number"
              value={formSortOrder}
              onChange={(e) => setFormSortOrder(e.target.value)}
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => setShowModal(false)}>
              Cancel
            </Button>
            <Button onClick={handleCreate} disabled={submitting}>
              {submitting ? 'Creating...' : 'Create Category'}
            </Button>
          </div>
        </div>
      </Modal>
    </AdminLayout>
  );
}
