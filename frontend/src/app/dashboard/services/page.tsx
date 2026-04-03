'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import {
  Plus,
  Pencil,
  Trash2,
  Loader2,
  AlertCircle,
  Clock,
  DollarSign,
  Package,
  X,
} from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { useLanguage } from '@/lib/i18n';
import {
  servicesApi,
  categoriesApi,
  ApiError,
  type ServiceDto,
  type Category,
} from '@/lib/api';
import Button from '@/components/ui/Button';
import Input from '@/components/ui/Input';
import Badge from '@/components/ui/Badge';
import Modal from '@/components/ui/Modal';

type PricingType = 'FIXED' | 'HOURLY';
type Lang = 'en' | 'pl' | 'uk';

interface ServiceForm {
  categoryId: string;
  titles: Record<Lang, string>;
  descriptions: Record<Lang, string>;
  pricingType: PricingType;
  priceAmount: string;
  priceCurrency: string;
  durationMinutes: string;
}

const EMPTY_FORM: ServiceForm = {
  categoryId: '',
  titles: { en: '', pl: '', uk: '' },
  descriptions: { en: '', pl: '', uk: '' },
  pricingType: 'FIXED',
  priceAmount: '',
  priceCurrency: 'PLN',
  durationMinutes: '60',
};

const LANG_LABELS: Record<Lang, string> = {
  en: 'English',
  pl: 'Polski',
  uk: 'Ukrainian',
};

function flattenCategories(categories: Category[], depth = 0): { id: string; name: string; depth: number }[] {
  const result: { id: string; name: string; depth: number }[] = [];
  for (const cat of categories) {
    result.push({ id: cat.id, name: cat.name, depth });
    if (cat.children?.length) {
      result.push(...flattenCategories(cat.children, depth + 1));
    }
  }
  return result;
}

export default function ServicesManagementPage() {
  const router = useRouter();
  const { user, token, isLoading: authLoading } = useAuth();
  const { lang, t } = useLanguage();

  const [services, setServices] = useState<ServiceDto[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Form state
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<ServiceForm>(EMPTY_FORM);
  const [activeLang, setActiveLang] = useState<Lang>('en');
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState<string | null>(null);

  const flatCategories = flattenCategories(categories);

  const fetchData = useCallback(async () => {
    if (!token || !user) return;
    setLoading(true);
    setError('');

    try {
      const [servicesData, categoriesData] = await Promise.all([
        servicesApi.listByProvider(user.id, lang),
        categoriesApi.list(lang),
      ]);
      setServices(servicesData);
      setCategories(categoriesData);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load services');
    } finally {
      setLoading(false);
    }
  }, [token, user, lang]);

  useEffect(() => {
    if (authLoading) return;
    if (!user || !token) {
      router.push('/login');
      return;
    }
    if (user.role !== 'PROVIDER') {
      router.push('/');
      return;
    }
    fetchData();
  }, [user, token, authLoading, router, fetchData]);

  function openAddForm() {
    setForm(EMPTY_FORM);
    setEditingId(null);
    setFormError('');
    setActiveLang('en');
    setShowForm(true);
  }

  function openEditForm(service: ServiceDto) {
    setForm({
      categoryId: service.categoryId,
      titles: { en: service.title, pl: '', uk: '' },
      descriptions: { en: service.description || '', pl: '', uk: '' },
      pricingType: service.pricingType as PricingType,
      priceAmount: String(service.priceAmount),
      priceCurrency: service.priceCurrency,
      durationMinutes: String(service.durationMinutes),
    });
    setEditingId(service.id);
    setFormError('');
    setActiveLang('en');
    setShowForm(true);
  }

  function closeForm() {
    setShowForm(false);
    setEditingId(null);
    setFormError('');
  }

  async function handleSave() {
    if (!token) return;

    if (!form.categoryId) {
      setFormError('Please select a category');
      return;
    }
    if (!form.titles.en.trim()) {
      setFormError('English title is required');
      setActiveLang('en');
      return;
    }
    if (!form.priceAmount || parseFloat(form.priceAmount) <= 0) {
      setFormError('Please enter a valid price');
      return;
    }

    setSaving(true);
    setFormError('');

    const payload = {
      categoryId: form.categoryId,
      titles: form.titles,
      descriptions: form.descriptions,
      pricingType: form.pricingType,
      priceAmount: parseFloat(form.priceAmount),
      priceCurrency: form.priceCurrency,
      durationMinutes: parseInt(form.durationMinutes, 10) || 60,
    };

    try {
      if (editingId) {
        const updated = await servicesApi.update(editingId, payload, token);
        setServices((prev) => prev.map((s) => (s.id === editingId ? updated : s)));
      } else {
        const created = await servicesApi.create(payload, token);
        setServices((prev) => [...prev, created]);
      }
      closeForm();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Failed to save service');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(serviceId: string) {
    if (!token || !confirm(t.services.confirmDelete)) return;
    setDeleting(serviceId);
    try {
      await servicesApi.delete(serviceId, token);
      setServices((prev) => prev.filter((s) => s.id !== serviceId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to delete service');
    } finally {
      setDeleting(null);
    }
  }

  if (authLoading || (!user && !error)) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <Loader2 className="h-8 w-8 animate-spin text-rose-500" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto px-4 py-8 sm:py-12">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{t.services.title}</h1>
            <p className="mt-1 text-sm text-gray-500">
              {services.length} service{services.length !== 1 ? 's' : ''}
            </p>
          </div>
          <Button onClick={openAddForm} size="md">
            <Plus className="h-4 w-4" />
            {t.services.addService}
          </Button>
        </div>

        {error && (
          <div className="mb-6 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 flex items-center gap-2">
            <AlertCircle className="h-4 w-4 shrink-0" />
            {error}
            <button
              type="button"
              onClick={() => setError('')}
              className="ml-auto text-red-500 hover:text-red-700 cursor-pointer"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        )}

        {loading ? (
          <div className="flex flex-col items-center justify-center py-20 text-gray-400">
            <Loader2 className="h-8 w-8 animate-spin mb-3" />
            <p className="text-sm">{t.common.loading}</p>
          </div>
        ) : services.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-gray-400">
            <Package className="h-12 w-12 mb-3" />
            <p className="text-base font-medium text-gray-500">{t.services.noServices}</p>
            <p className="text-sm mt-1 mb-4">{t.services.addFirst}</p>
            <Button onClick={openAddForm} size="md">
              <Plus className="h-4 w-4" />
              {t.services.addService}
            </Button>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {services.map((service) => (
              <div
                key={service.id}
                className="bg-white rounded-xl border border-gray-100 shadow-sm p-5 hover:shadow-md transition-shadow"
              >
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div className="min-w-0 flex-1">
                    <h3 className="font-semibold text-gray-900 truncate">
                      {service.title}
                    </h3>
                    <p className="text-xs text-gray-400 mt-0.5">{service.categoryName}</p>
                  </div>
                  <Badge
                    variant={service.isActive ? 'success' : 'default'}
                  >
                    {service.isActive ? t.services.active : t.services.inactive}
                  </Badge>
                </div>

                {service.description && (
                  <p className="text-sm text-gray-500 mb-3 line-clamp-2">
                    {service.description}
                  </p>
                )}

                <div className="flex flex-wrap items-center gap-3 text-sm text-gray-600 mb-4">
                  <span className="inline-flex items-center gap-1">
                    <DollarSign className="h-3.5 w-3.5 text-gray-400" />
                    {service.priceAmount.toFixed(2)} {service.priceCurrency}
                    <span className="text-xs text-gray-400">
                      / {service.pricingType === 'HOURLY' ? 'hr' : 'fixed'}
                    </span>
                  </span>
                  <span className="inline-flex items-center gap-1">
                    <Clock className="h-3.5 w-3.5 text-gray-400" />
                    {service.durationMinutes} min
                  </span>
                </div>

                <div className="flex items-center gap-2 pt-3 border-t border-gray-50">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => openEditForm(service)}
                  >
                    <Pencil className="h-3.5 w-3.5" />
                    {t.common.edit}
                  </Button>
                  <Button
                    variant="danger"
                    size="sm"
                    loading={deleting === service.id}
                    onClick={() => handleDelete(service.id)}
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                    {t.common.delete}
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Add/Edit Modal */}
        <Modal
          open={showForm}
          onClose={closeForm}
          title={editingId ? t.services.editService : t.services.addService}
          size="lg"
        >
          <div className="space-y-5">
            {formError && (
              <div className="rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
                {formError}
              </div>
            )}

            {/* Category */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">
                {t.services.category}
              </label>
              <select
                value={form.categoryId}
                onChange={(e) => setForm((prev) => ({ ...prev, categoryId: e.target.value }))}
                className="w-full rounded-lg border border-gray-300 bg-white px-3.5 py-2.5 text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-rose-500/30 focus:border-rose-500"
              >
                <option value="">{t.services.category}...</option>
                {flatCategories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {'  '.repeat(cat.depth)}{cat.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Title translations - tabs */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">
                {t.services.serviceTitle}
              </label>
              <div className="flex gap-1 mb-2">
                {(['en', 'pl', 'uk'] as Lang[]).map((lng) => (
                  <button
                    key={lng}
                    type="button"
                    onClick={() => setActiveLang(lng)}
                    className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors cursor-pointer ${
                      activeLang === lng
                        ? 'bg-rose-500 text-white'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    {LANG_LABELS[lng]}
                    {form.titles[lng] && (
                      <span className="ml-1 inline-block w-1.5 h-1.5 rounded-full bg-green-400" />
                    )}
                  </button>
                ))}
              </div>
              <Input
                placeholder={`Title in ${LANG_LABELS[activeLang]}`}
                value={form.titles[activeLang]}
                onChange={(e) =>
                  setForm((prev) => ({
                    ...prev,
                    titles: { ...prev.titles, [activeLang]: e.target.value },
                  }))
                }
              />
            </div>

            {/* Description (same lang tabs) */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">
                {t.services.serviceDescription} ({LANG_LABELS[activeLang]})
              </label>
              <textarea
                value={form.descriptions[activeLang]}
                onChange={(e) =>
                  setForm((prev) => ({
                    ...prev,
                    descriptions: { ...prev.descriptions, [activeLang]: e.target.value },
                  }))
                }
                rows={3}
                placeholder={`Description in ${LANG_LABELS[activeLang]}`}
                className="w-full rounded-lg border border-gray-300 bg-white px-3.5 py-2.5 text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-rose-500/30 focus:border-rose-500 resize-none"
              />
            </div>

            {/* Pricing */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">
                  {t.services.pricingType}
                </label>
                <select
                  value={form.pricingType}
                  onChange={(e) =>
                    setForm((prev) => ({
                      ...prev,
                      pricingType: e.target.value as PricingType,
                    }))
                  }
                  className="w-full rounded-lg border border-gray-300 bg-white px-3.5 py-2.5 text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-rose-500/30 focus:border-rose-500"
                >
                  <option value="FIXED">Fixed</option>
                  <option value="HOURLY">Hourly</option>
                </select>
              </div>

              <Input
                label={t.services.price}
                type="number"
                step="0.01"
                min="0"
                placeholder="0.00"
                value={form.priceAmount}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, priceAmount: e.target.value }))
                }
              />

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">
                  {t.services.currency}
                </label>
                <select
                  value={form.priceCurrency}
                  onChange={(e) =>
                    setForm((prev) => ({ ...prev, priceCurrency: e.target.value }))
                  }
                  className="w-full rounded-lg border border-gray-300 bg-white px-3.5 py-2.5 text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-rose-500/30 focus:border-rose-500"
                >
                  <option value="PLN">PLN</option>
                  <option value="EUR">EUR</option>
                  <option value="USD">USD</option>
                  <option value="UAH">UAH</option>
                </select>
              </div>
            </div>

            {/* Duration */}
            <Input
              label={t.services.duration}
              type="number"
              min="15"
              step="15"
              placeholder="60"
              value={form.durationMinutes}
              onChange={(e) =>
                setForm((prev) => ({ ...prev, durationMinutes: e.target.value }))
              }
            />

            {/* Actions */}
            <div className="flex items-center justify-end gap-3 pt-2">
              <Button variant="outline" onClick={closeForm}>
                {t.common.cancel}
              </Button>
              <Button onClick={handleSave} loading={saving}>
                {editingId ? t.common.save : t.services.addService}
              </Button>
            </div>
          </div>
        </Modal>
      </div>
    </div>
  );
}
