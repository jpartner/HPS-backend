'use client';

import { use, useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import clsx from 'clsx';
import { Star, ChevronLeft, ChevronRight, MapPin, Loader2 } from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { useLanguage } from '@/lib/i18n';
import { useCountry } from '@/lib/country-context';
import {
  categoriesApi,
  providersApi,
  geoApi,
  type Category,
  type ProviderSummary,
  type PageMeta,
  type Country,
  type Region,
  type City,
} from '@/lib/api';

// Check if a string looks like a UUID
function isUUID(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
}

export default function CategoryPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id: paramValue } = use(params);
  const { token } = useAuth();
  const { lang, t } = useLanguage();
  const { selectedCountry: contextCountry, setCountry: setContextCountry } = useCountry();

  const [category, setCategory] = useState<Category | null>(null);
  const [resolvedCategoryId, setResolvedCategoryId] = useState<string | null>(
    isUUID(paramValue) ? paramValue : null
  );
  const [providers, setProviders] = useState<ProviderSummary[]>([]);
  const [meta, setMeta] = useState<PageMeta | null>(null);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Geo filter state
  const [countries, setCountries] = useState<Country[]>([]);
  const [regions, setRegions] = useState<Region[]>([]);
  const [cities, setCities] = useState<City[]>([]);
  const [selectedCountry, setSelectedCountry] = useState(contextCountry?.isoCode ?? '');
  const [selectedRegion, setSelectedRegion] = useState('');
  const [selectedCity, setSelectedCity] = useState('');

  // Sync from country context -> local filter
  useEffect(() => {
    const iso = contextCountry?.isoCode ?? '';
    setSelectedCountry(iso);
  }, [contextCountry]);

  // Load countries on mount
  useEffect(() => {
    geoApi.countries(lang).then(setCountries).catch(() => {});
  }, [lang]);

  // Load regions when country changes
  useEffect(() => {
    setRegions([]);
    setCities([]);
    setSelectedRegion('');
    setSelectedCity('');
    if (selectedCountry) {
      geoApi.regions(selectedCountry, lang).then(setRegions).catch(() => {});
    }
  }, [selectedCountry, lang]);

  // Load cities when region changes
  useEffect(() => {
    setCities([]);
    setSelectedCity('');
    if (selectedRegion) {
      geoApi.cities(selectedRegion, lang).then(setCities).catch(() => {});
    }
  }, [selectedRegion, lang]);

  // Load category - resolve slug to ID if needed
  useEffect(() => {
    categoriesApi
      .list(lang)
      .then((cats) => {
        let found: Category | undefined;
        if (isUUID(paramValue)) {
          found = cats.find((c) => c.id === paramValue);
        } else {
          // Param is a slug - search top-level and children
          found = cats.find((c) => c.slug === paramValue);
          if (!found) {
            for (const cat of cats) {
              const child = cat.children.find((ch) => ch.slug === paramValue);
              if (child) {
                found = child;
                break;
              }
            }
          }
        }
        if (found) {
          setCategory(found);
          setResolvedCategoryId(found.id);
        }
      })
      .catch(() => {});
  }, [paramValue, lang]);

  // Load providers
  const fetchProviders = useCallback(async () => {
    if (!resolvedCategoryId) return;
    setLoading(true);
    setError(null);
    try {
      const queryParams: Record<string, string> = {
        categoryId: resolvedCategoryId,
        page: String(page - 1),
        size: '12',
      };
      if (selectedCountry) queryParams.countryCode = selectedCountry;
      if (selectedRegion) queryParams.regionId = selectedRegion;
      if (selectedCity) queryParams.cityId = selectedCity;

      const result = await providersApi.list(queryParams, lang);
      setProviders(result.data);
      setMeta(result.meta);
    } catch {
      setError(t.common.error);
    } finally {
      setLoading(false);
    }
  }, [resolvedCategoryId, page, selectedCountry, selectedRegion, selectedCity, lang]);

  useEffect(() => {
    fetchProviders();
  }, [fetchProviders]);

  // Reset page when filters change
  useEffect(() => {
    setPage(1);
  }, [selectedCountry, selectedRegion, selectedCity]);

  const totalPages = meta ? Math.ceil(meta.totalItems / meta.pageSize) : 1;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header with optional category image banner */}
      {category?.imageUrl ? (
        <div className="relative h-48 sm:h-64 bg-gray-900">
          <img
            src={category.imageUrl}
            alt={category.name}
            className="h-full w-full object-cover opacity-70"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/30 to-transparent" />
          <div className="absolute inset-0 flex flex-col justify-end">
            <div className="mx-auto w-full max-w-5xl px-4 pb-6">
              <Link
                href="/"
                className="inline-flex items-center gap-1 text-sm text-white/80 hover:text-white transition"
              >
                <ChevronLeft className="h-4 w-4" />
                {t.categories.title}
              </Link>
              <h1 className="mt-2 text-2xl font-bold text-white sm:text-3xl drop-shadow">
                {category.name}
              </h1>
            </div>
          </div>
        </div>
      ) : (
        <div className="bg-white border-b border-gray-100">
          <div className="mx-auto max-w-5xl px-4 py-6">
            <Link
              href="/"
              className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-rose-500 transition"
            >
              <ChevronLeft className="h-4 w-4" />
              {t.categories.title}
            </Link>
            <h1 className="mt-2 text-2xl font-bold text-gray-900 sm:text-3xl">
              {category?.name ?? t.providers.title}
            </h1>
          </div>
        </div>
      )}

      <div className="mx-auto max-w-5xl px-4 py-6">
        {/* Filter Bar */}
        <div className="flex flex-col gap-3 rounded-xl bg-white p-4 shadow-sm sm:flex-row sm:items-end">
          <div className="flex-1">
            <label className="block text-xs font-medium text-gray-500 mb-1">
              {t.providers.country}
            </label>
            <select
              value={selectedCountry}
              onChange={(e) => {
                setSelectedCountry(e.target.value);
                setContextCountry(e.target.value);
              }}
              className="w-full rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-700 focus:border-rose-300 focus:outline-none focus:ring-1 focus:ring-rose-300"
            >
              <option value="">{t.providers.allCountries}</option>
              {countries.map((c) => (
                <option key={c.id} value={c.isoCode}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
          <div className="flex-1">
            <label className="block text-xs font-medium text-gray-500 mb-1">
              {t.providers.region}
            </label>
            <select
              value={selectedRegion}
              onChange={(e) => setSelectedRegion(e.target.value)}
              disabled={!selectedCountry}
              className={clsx(
                'w-full rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-700 focus:border-rose-300 focus:outline-none focus:ring-1 focus:ring-rose-300',
                !selectedCountry && 'opacity-50 cursor-not-allowed'
              )}
            >
              <option value="">{t.providers.allRegions}</option>
              {regions.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name}
                </option>
              ))}
            </select>
          </div>
          <div className="flex-1">
            <label className="block text-xs font-medium text-gray-500 mb-1">
              {t.providers.city}
            </label>
            <select
              value={selectedCity}
              onChange={(e) => setSelectedCity(e.target.value)}
              disabled={!selectedRegion}
              className={clsx(
                'w-full rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-700 focus:border-rose-300 focus:outline-none focus:ring-1 focus:ring-rose-300',
                !selectedRegion && 'opacity-50 cursor-not-allowed'
              )}
            >
              <option value="">{t.providers.allCities}</option>
              {cities.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Loading / Error / Results */}
        {loading ? (
          <div className="mt-12 flex justify-center">
            <Loader2 className="h-8 w-8 animate-spin text-rose-400" />
          </div>
        ) : error ? (
          <div className="mt-12 text-center">
            <p className="text-red-500">{error}</p>
            <button
              onClick={fetchProviders}
              className="mt-4 rounded-lg bg-rose-500 px-4 py-2 text-sm font-medium text-white hover:bg-rose-600 transition"
            >
              {t.common.retry}
            </button>
          </div>
        ) : providers.length === 0 ? (
          <p className="mt-12 text-center text-gray-400">
            {t.providers.noProviders}
          </p>
        ) : (
          <>
            {/* Provider Cards Grid */}
            <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {providers.map((provider) => (
                <div
                  key={provider.id}
                  className="flex flex-col rounded-xl border border-gray-100 bg-white p-5 shadow-sm transition hover:shadow-md"
                >
                  <h3 className="text-lg font-semibold text-gray-900">
                    {provider.businessName}
                  </h3>

                  <div className="mt-1 flex items-center gap-1 text-sm text-gray-500">
                    <MapPin className="h-3.5 w-3.5" />
                    {provider.cityName}
                  </div>

                  {/* Rating */}
                  <div className="mt-2 flex items-center gap-1">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <Star
                        key={i}
                        className={clsx(
                          'h-4 w-4',
                          i < Math.round(provider.avgRating)
                            ? 'fill-amber-400 text-amber-400'
                            : 'text-gray-200'
                        )}
                      />
                    ))}
                    <span className="ml-1 text-xs text-gray-400">
                      ({provider.reviewCount})
                    </span>
                  </div>

                  {/* Category badges */}
                  <div className="mt-3 flex flex-wrap gap-1.5">
                    {provider.categories.map((cat) => (
                      <span
                        key={cat.id}
                        className="rounded-full bg-rose-50 px-2.5 py-0.5 text-xs font-medium text-rose-600"
                      >
                        {cat.name}
                      </span>
                    ))}
                  </div>

                  <div className="mt-auto pt-4">
                    <Link
                      href={`/providers/${provider.id}`}
                      className="block w-full rounded-lg bg-rose-500 py-2 text-center text-sm font-medium text-white transition hover:bg-rose-600"
                    >
                      {t.providers.viewProfile}
                    </Link>
                  </div>
                </div>
              ))}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="mt-8 flex items-center justify-center gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(1, p - 1))}
                  disabled={page <= 1}
                  className={clsx(
                    'flex h-9 w-9 items-center justify-center rounded-lg border text-sm transition',
                    page <= 1
                      ? 'border-gray-100 text-gray-300 cursor-not-allowed'
                      : 'border-gray-200 text-gray-600 hover:border-rose-300 hover:text-rose-500'
                  )}
                >
                  <ChevronLeft className="h-4 w-4" />
                </button>
                <span className="text-sm text-gray-500">
                  Page {page} of {totalPages}
                </span>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                  disabled={page >= totalPages}
                  className={clsx(
                    'flex h-9 w-9 items-center justify-center rounded-lg border text-sm transition',
                    page >= totalPages
                      ? 'border-gray-100 text-gray-300 cursor-not-allowed'
                      : 'border-gray-200 text-gray-600 hover:border-rose-300 hover:text-rose-500'
                  )}
                >
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
