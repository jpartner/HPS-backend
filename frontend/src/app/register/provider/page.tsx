'use client';

import { useState, useRef, useEffect, type FormEvent } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Briefcase, ArrowRight, ArrowLeft, Check } from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { useLanguage } from '@/lib/i18n';
import {
  authApi, messagingApi, providersApi, categoriesApi, geoApi,
  ApiError,
  type Category, type Country, type Region, type City,
} from '@/lib/api';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';

function flattenCategories(cats: Category[], prefix = ''): { id: string; name: string }[] {
  const result: { id: string; name: string }[] = [];
  for (const c of cats) {
    result.push({ id: c.id, name: prefix + c.name });
    if (c.children?.length) result.push(...flattenCategories(c.children, prefix + '  '));
  }
  return result;
}

export default function ProviderRegisterPage() {
  const router = useRouter();
  const auth = useAuth();
  const { t, lang } = useLanguage();

  const [step, setStep] = useState(1);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  // Step 1: Account
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '', handle: '' });
  const [handleStatus, setHandleStatus] = useState<'idle' | 'checking' | 'available' | 'taken'>('idle');
  const handleTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [token, setToken] = useState('');

  // Step 2: Business profile
  const [businessName, setBusinessName] = useState('');
  const [description, setDescription] = useState('');
  const [selectedCategoryIds, setSelectedCategoryIds] = useState<string[]>([]);
  const [isMobile, setIsMobile] = useState(false);

  // Geo
  const [selectedCountry, setSelectedCountry] = useState('');
  const [selectedRegion, setSelectedRegion] = useState('');
  const [selectedCity, setSelectedCity] = useState('');

  // Data
  const [categories, setCategories] = useState<{ id: string; name: string }[]>([]);
  const [countries, setCountries] = useState<Country[]>([]);
  const [regions, setRegions] = useState<Region[]>([]);
  const [cities, setCities] = useState<City[]>([]);

  // Load categories and countries
  useEffect(() => {
    categoriesApi.list(lang).then((data) => setCategories(flattenCategories(data))).catch(() => {});
    geoApi.countries(lang).then(setCountries).catch(() => {});
  }, [lang]);

  // Load regions when country changes
  useEffect(() => {
    if (!selectedCountry) { setRegions([]); setSelectedRegion(''); return; }
    geoApi.regions(selectedCountry, lang).then(setRegions).catch(() => {});
    setSelectedRegion('');
    setSelectedCity('');
  }, [selectedCountry, lang]);

  // Load cities when region changes
  useEffect(() => {
    if (!selectedRegion) { setCities([]); setSelectedCity(''); return; }
    geoApi.cities(selectedRegion, lang).then(setCities).catch(() => {});
    setSelectedCity('');
  }, [selectedRegion, lang]);

  function updateField(field: string, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
    setFieldErrors((prev) => ({ ...prev, [field]: '' }));

    if (field === 'handle') {
      const h = value.toLowerCase().replace(/[^a-z0-9_]/g, '');
      setForm((prev) => ({ ...prev, handle: h }));
      if (handleTimerRef.current) clearTimeout(handleTimerRef.current);
      if (h.length < 3) { setHandleStatus('idle'); return; }
      setHandleStatus('checking');
      handleTimerRef.current = setTimeout(async () => {
        try {
          const res = await messagingApi.handleAvailable(h);
          setHandleStatus(res.available ? 'available' : 'taken');
          if (!res.available && res.reason) setFieldErrors((prev) => ({ ...prev, handle: res.reason! }));
        } catch { setHandleStatus('idle'); }
      }, 400);
    }
  }

  function validateStep1(): boolean {
    const errors: Record<string, string> = {};
    if (!form.firstName.trim()) errors.firstName = 'First name is required';
    if (!form.lastName.trim()) errors.lastName = 'Last name is required';
    if (!form.email.trim()) errors.email = 'Email is required';
    if (form.password.length < 8) errors.password = 'Password must be at least 8 characters';
    if (!form.handle.trim()) errors.handle = 'Handle is required';
    else if (form.handle.length < 3) errors.handle = 'Handle must be at least 3 characters';
    if (handleStatus === 'taken') errors.handle = 'Handle is not available';
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  function validateStep2(): boolean {
    const errors: Record<string, string> = {};
    if (!businessName.trim()) errors.businessName = 'Business name is required';
    if (selectedCategoryIds.length === 0) errors.categories = 'Select at least one category';
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleStep1(e: FormEvent) {
    e.preventDefault();
    setError('');
    if (!validateStep1()) return;
    setLoading(true);
    try {
      const res = await authApi.register({
        email: form.email,
        password: form.password,
        firstName: form.firstName,
        lastName: form.lastName,
        handle: form.handle,
      });
      setToken(res.accessToken);
      auth.login(res.accessToken, res.refreshToken);
      setStep(2);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : t.common.error);
    } finally {
      setLoading(false);
    }
  }

  async function handleStep2(e: FormEvent) {
    e.preventDefault();
    setError('');
    if (!validateStep2()) return;
    setLoading(true);
    try {
      await providersApi.createProfile({
        businessName,
        description: description || undefined,
        categoryIds: selectedCategoryIds,
        isMobile,
        cityId: selectedCity || undefined,
      }, token);
      router.push('/dashboard');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : t.common.error);
    } finally {
      setLoading(false);
    }
  }

  function toggleCategory(id: string) {
    setSelectedCategoryIds((prev) =>
      prev.includes(id) ? prev.filter((c) => c !== id) : [...prev, id]
    );
    setFieldErrors((prev) => ({ ...prev, categories: '' }));
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-rose-50 to-pink-50 px-4 py-12">
      <div className="w-full max-w-lg">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-full bg-rose-100 mb-4">
            <Briefcase className="h-7 w-7 text-rose-600" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Create Provider Account</h1>

          {/* Step indicator */}
          <div className="flex items-center justify-center gap-3 mt-4">
            <div className={`flex items-center gap-1.5 text-sm font-medium ${step >= 1 ? 'text-rose-600' : 'text-gray-400'}`}>
              <span className={`w-7 h-7 rounded-full flex items-center justify-center text-xs ${step > 1 ? 'bg-rose-600 text-white' : step === 1 ? 'bg-rose-100 text-rose-600' : 'bg-gray-100 text-gray-400'}`}>
                {step > 1 ? <Check className="h-4 w-4" /> : '1'}
              </span>
              Account
            </div>
            <div className="w-8 h-px bg-gray-300" />
            <div className={`flex items-center gap-1.5 text-sm font-medium ${step >= 2 ? 'text-rose-600' : 'text-gray-400'}`}>
              <span className={`w-7 h-7 rounded-full flex items-center justify-center text-xs ${step === 2 ? 'bg-rose-100 text-rose-600' : 'bg-gray-100 text-gray-400'}`}>
                2
              </span>
              Business
            </div>
          </div>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 sm:p-8">
          {error && (
            <div className="mb-4 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">{error}</div>
          )}

          {/* Step 1: Account Details */}
          {step === 1 && (
            <form onSubmit={handleStep1} className="space-y-5">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input label={t.auth.firstName} value={form.firstName} onChange={(e) => updateField('firstName', e.target.value)} error={fieldErrors.firstName} required autoComplete="given-name" />
                <Input label={t.auth.lastName} value={form.lastName} onChange={(e) => updateField('lastName', e.target.value)} error={fieldErrors.lastName} required autoComplete="family-name" />
              </div>

              <div>
                <Input label="Handle" value={form.handle} onChange={(e) => updateField('handle', e.target.value)} error={fieldErrors.handle} placeholder="e.g. janes_spa" autoComplete="username" required />
                <div className="mt-1 h-4 text-xs">
                  {form.handle.length >= 3 && handleStatus === 'checking' && <span className="text-gray-400">Checking...</span>}
                  {form.handle.length >= 3 && handleStatus === 'available' && <span className="text-green-600">@{form.handle} is available</span>}
                  {form.handle.length >= 3 && handleStatus === 'taken' && !fieldErrors.handle && <span className="text-red-600">@{form.handle} is not available</span>}
                </div>
              </div>

              <Input label={t.auth.email} type="email" value={form.email} onChange={(e) => updateField('email', e.target.value)} error={fieldErrors.email} required autoComplete="email" />
              <Input label={t.auth.password} type="password" value={form.password} onChange={(e) => updateField('password', e.target.value)} error={fieldErrors.password} placeholder="At least 8 characters" required autoComplete="new-password" />

              <Button type="submit" loading={loading} className="w-full" size="lg">
                Continue <ArrowRight className="h-4 w-4 ml-1" />
              </Button>
            </form>
          )}

          {/* Step 2: Business Profile */}
          {step === 2 && (
            <form onSubmit={handleStep2} className="space-y-5">
              <Input label="Business Name" value={businessName} onChange={(e) => { setBusinessName(e.target.value); setFieldErrors((p) => ({ ...p, businessName: '' })); }} error={fieldErrors.businessName} required placeholder="e.g. Jane's Beauty Spa" />

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  rows={3}
                  placeholder="Tell clients about your services..."
                  className="w-full px-3 py-2 border border-gray-200 rounded-xl text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-rose-500/30 focus:border-rose-500 resize-none"
                />
              </div>

              {/* Categories */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Categories <span className="text-red-500">*</span>
                </label>
                {fieldErrors.categories && <p className="text-sm text-red-600 mb-2">{fieldErrors.categories}</p>}
                <div className="max-h-48 overflow-y-auto border border-gray-200 rounded-xl p-2 space-y-1">
                  {categories.map((cat) => (
                    <label key={cat.id} className="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-gray-50 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={selectedCategoryIds.includes(cat.id)}
                        onChange={() => toggleCategory(cat.id)}
                        className="h-4 w-4 rounded border-gray-300 text-rose-600 focus:ring-rose-500"
                      />
                      <span className="text-sm text-gray-700">{cat.name}</span>
                    </label>
                  ))}
                </div>
              </div>

              {/* Location */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Location</label>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <select
                    value={selectedCountry}
                    onChange={(e) => setSelectedCountry(e.target.value)}
                    className="px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-rose-500/30 focus:border-rose-500"
                  >
                    <option value="">Country</option>
                    {countries.map((c) => <option key={c.id} value={c.isoCode}>{c.name}</option>)}
                  </select>
                  <select
                    value={selectedRegion}
                    onChange={(e) => setSelectedRegion(e.target.value)}
                    disabled={!selectedCountry}
                    className="px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-rose-500/30 focus:border-rose-500 disabled:bg-gray-50 disabled:text-gray-400"
                  >
                    <option value="">Region</option>
                    {regions.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                  </select>
                  <select
                    value={selectedCity}
                    onChange={(e) => setSelectedCity(e.target.value)}
                    disabled={!selectedRegion}
                    className="px-3 py-2 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-rose-500/30 focus:border-rose-500 disabled:bg-gray-50 disabled:text-gray-400"
                  >
                    <option value="">City</option>
                    {cities.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                </div>
              </div>

              {/* Mobile service */}
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={isMobile}
                  onChange={(e) => setIsMobile(e.target.checked)}
                  className="h-4 w-4 rounded border-gray-300 text-rose-600 focus:ring-rose-500"
                />
                <span className="text-sm text-gray-700">I offer mobile/outcall services</span>
              </label>

              <div className="flex gap-3">
                <Button type="button" variant="outline" onClick={() => setStep(1)} className="flex-1" size="lg">
                  <ArrowLeft className="h-4 w-4 mr-1" /> Back
                </Button>
                <Button type="submit" loading={loading} className="flex-1" size="lg">
                  Complete Setup
                </Button>
              </div>
            </form>
          )}
        </div>

        <p className="mt-6 text-center text-sm text-gray-500">
          {t.auth.hasAccount}{' '}
          <Link href="/login" className="font-medium text-rose-600 hover:text-rose-500 transition-colors">{t.auth.loginLink}</Link>
          {' · '}
          <Link href="/register/customer" className="font-medium text-rose-600 hover:text-rose-500 transition-colors">Register as Client</Link>
        </p>
      </div>
    </div>
  );
}
