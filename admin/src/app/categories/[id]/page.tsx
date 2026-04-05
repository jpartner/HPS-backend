'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Save, Trash2, Plus, ArrowLeft, Edit2, X } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { TranslationForm } from '@/components/ui/TranslationForm';
import { adminCategoryApi } from '@/lib/api';

interface Subcategory {
  id: string;
  name: string;
  slug: string;
  icon: string;
  sortOrder: number;
}

interface CategoryDetail {
  id: string;
  name: Record<string, string>;
  icon: string;
  slug: string;
  imageUrl: string;
  sortOrder: number;
  subcategories: Subcategory[];
}

export default function CategoryDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();

  const [category, setCategory] = useState<CategoryDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Form fields
  const [names, setNames] = useState<Record<string, string>>({});
  const [icon, setIcon] = useState('');
  const [slug, setSlug] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [sortOrder, setSortOrder] = useState('0');

  // Subcategory modal
  const [showSubModal, setShowSubModal] = useState(false);
  const [editingSubId, setEditingSubId] = useState<string | null>(null);
  const [subName, setSubName] = useState('');
  const [subSlug, setSubSlug] = useState('');
  const [subIcon, setSubIcon] = useState('');
  const [subSortOrder, setSubSortOrder] = useState('0');
  const [submittingSub, setSubmittingSub] = useState(false);

  const fetchCategory = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminCategoryApi.get(params.id);
      setCategory(data);
      setNames(data.name || {});
      setIcon(data.icon || '');
      setSlug(data.slug || '');
      setImageUrl(data.imageUrl || '');
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
      await adminCategoryApi.update(params.id, {
        name: names,
        icon,
        slug,
        imageUrl: imageUrl || undefined,
        sortOrder: parseInt(sortOrder, 10) || 0,
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

  function openAddSub() {
    setEditingSubId(null);
    setSubName('');
    setSubSlug('');
    setSubIcon('');
    setSubSortOrder('0');
    setShowSubModal(true);
  }

  function openEditSub(sub: Subcategory) {
    setEditingSubId(sub.id);
    setSubName(sub.name);
    setSubSlug(sub.slug);
    setSubIcon(sub.icon);
    setSubSortOrder(String(sub.sortOrder ?? 0));
    setShowSubModal(true);
  }

  async function handleSaveSub() {
    setSubmittingSub(true);
    try {
      if (editingSubId) {
        await adminCategoryApi.updateSubcategory(params.id, editingSubId, {
          name: subName,
          slug: subSlug,
          icon: subIcon,
          sortOrder: parseInt(subSortOrder, 10) || 0,
        });
      } else {
        await adminCategoryApi.createSubcategory(params.id, {
          name: subName,
          slug: subSlug,
          icon: subIcon,
          sortOrder: parseInt(subSortOrder, 10) || 0,
        });
      }
      setShowSubModal(false);
      fetchCategory();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to save subcategory';
      setError(message);
    } finally {
      setSubmittingSub(false);
    }
  }

  async function handleDeleteSub(subId: string) {
    if (!confirm('Delete this subcategory?')) return;
    try {
      await adminCategoryApi.deleteSubcategory(params.id, subId);
      fetchCategory();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to delete subcategory';
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

  return (
    <AdminLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button onClick={() => router.push('/categories')} className="text-gray-400 hover:text-gray-600">
              <ArrowLeft className="h-5 w-5" />
            </button>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Edit Category</h1>
              <p className="text-sm text-gray-500 mt-1">ID: {params.id}</p>
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

        {/* Main Form */}
        <div className="bg-white rounded-lg border border-gray-200 p-6 space-y-4">
          <h2 className="text-lg font-semibold text-gray-900">Category Details</h2>

          <TranslationForm
            label="Name"
            values={names}
            onChange={setNames}
          />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Icon</label>
              <Input value={icon} onChange={(e) => setIcon(e.target.value)} placeholder="Icon emoji or name" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Slug</label>
              <Input value={slug} onChange={(e) => setSlug(e.target.value)} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Image URL</label>
              <Input value={imageUrl} onChange={(e) => setImageUrl(e.target.value)} placeholder="https://..." />
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

        {/* Subcategories */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Subcategories</h2>
            <Button variant="secondary" onClick={openAddSub}>
              <Plus className="h-4 w-4 mr-2" />
              Add Subcategory
            </Button>
          </div>

          {(category?.subcategories || []).length === 0 ? (
            <p className="text-sm text-gray-500">No subcategories.</p>
          ) : (
            <div className="divide-y divide-gray-100">
              {(category?.subcategories || []).map((sub) => (
                <div key={sub.id} className="flex items-center justify-between py-3">
                  <div className="flex items-center gap-3">
                    <span className="text-lg">{sub.icon || '--'}</span>
                    <div>
                      <p className="text-sm font-medium text-gray-900">{sub.name}</p>
                      <p className="text-xs text-gray-400 font-mono">{sub.slug}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-gray-400">Order: {sub.sortOrder}</span>
                    <button onClick={() => openEditSub(sub)} className="text-gray-400 hover:text-indigo-600">
                      <Edit2 className="h-4 w-4" />
                    </button>
                    <button onClick={() => handleDeleteSub(sub.id)} className="text-gray-400 hover:text-red-600">
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Subcategory Modal */}
      <Modal
        open={showSubModal}
        onClose={() => setShowSubModal(false)}
        title={editingSubId ? 'Edit Subcategory' : 'Add Subcategory'}
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
            <Input value={subName} onChange={(e) => setSubName(e.target.value)} placeholder="Subcategory name" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Slug</label>
            <Input value={subSlug} onChange={(e) => setSubSlug(e.target.value)} placeholder="subcategory-slug" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Icon</label>
            <Input value={subIcon} onChange={(e) => setSubIcon(e.target.value)} placeholder="Icon" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Sort Order</label>
            <Input type="number" value={subSortOrder} onChange={(e) => setSubSortOrder(e.target.value)} />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => setShowSubModal(false)}>
              Cancel
            </Button>
            <Button onClick={handleSaveSub} disabled={submittingSub}>
              {submittingSub ? 'Saving...' : editingSubId ? 'Update' : 'Create'}
            </Button>
          </div>
        </div>
      </Modal>
    </AdminLayout>
  );
}
