'use client';

import { use, useState, useEffect, useMemo } from 'react';
import Link from 'next/link';
import clsx from 'clsx';
import {
  Star,
  ChevronLeft,
  MapPin,
  BadgeCheck,
  Clock,
  MessageCircle,
  Loader2,
  Check,
} from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { useLanguage } from '@/lib/i18n';
import {
  providersApi,
  servicesApi,
  type ProviderDetail,
  type ServiceDto,
} from '@/lib/api';

export default function ProviderPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id: providerId } = use(params);
  const { user, token } = useAuth();
  const { lang, t } = useLanguage();

  const [provider, setProvider] = useState<ProviderDetail | null>(null);
  const [services, setServices] = useState<ServiceDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Service selection for booking
  const [selectedServiceIds, setSelectedServiceIds] = useState<Set<string>>(
    new Set()
  );

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [providerData, serviceData] = await Promise.all([
          providersApi.get(providerId, lang),
          servicesApi.listByProvider(providerId, lang),
        ]);
        if (!cancelled) {
          setProvider(providerData);
          setServices(serviceData.filter((s) => s.isActive));
        }
      } catch {
        if (!cancelled) {
          setError(t.common.error);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [providerId, lang]);

  const toggleService = (serviceId: string) => {
    setSelectedServiceIds((prev) => {
      const next = new Set(prev);
      if (next.has(serviceId)) {
        next.delete(serviceId);
      } else {
        next.add(serviceId);
      }
      return next;
    });
  };

  const runningTotal = useMemo(() => {
    return services
      .filter((s) => selectedServiceIds.has(s.id))
      .reduce((sum, s) => sum + s.priceAmount, 0);
  }, [services, selectedServiceIds]);

  const totalDuration = useMemo(() => {
    return services
      .filter((s) => selectedServiceIds.has(s.id))
      .reduce((sum, s) => sum + s.durationMinutes, 0);
  }, [services, selectedServiceIds]);

  const currency = services.length > 0 ? services[0].priceCurrency : 'EUR';

  function formatPrice(amount: number, cur: string) {
    return new Intl.NumberFormat('en', {
      style: 'currency',
      currency: cur,
    }).format(amount);
  }

  function formatDuration(minutes: number) {
    if (minutes < 60) return `${minutes} min`;
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return m > 0 ? `${h}h ${m}min` : `${h}h`;
  }

  function formatPricingType(type: string) {
    switch (type.toUpperCase()) {
      case 'FIXED':
        return 'Fixed price';
      case 'STARTING_AT':
        return 'Starting at';
      case 'HOURLY':
        return 'Per hour';
      case 'UPON_CONSULTATION':
        return 'Upon consultation';
      default:
        return type;
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <Loader2 className="h-8 w-8 animate-spin text-rose-400" />
      </div>
    );
  }

  if (error || !provider) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-gray-50 gap-4">
        <p className="text-red-500">{error ?? t.common.noResults}</p>
        <Link
          href="/"
          className="rounded-lg bg-rose-500 px-4 py-2 text-sm font-medium text-white hover:bg-rose-600 transition"
        >
          {t.common.back}
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Provider Header */}
      <div className="bg-white border-b border-gray-100">
        <div className="mx-auto max-w-5xl px-4 py-6">
          <Link
            href="/"
            className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-rose-500 transition"
          >
            <ChevronLeft className="h-4 w-4" />
            {t.common.back}
          </Link>

          <div className="mt-4 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-2xl font-bold text-gray-900 sm:text-3xl">
                  {provider.businessName}
                </h1>
                {provider.isVerified && (
                  <BadgeCheck className="h-6 w-6 text-blue-500" />
                )}
              </div>

              <div className="mt-1 flex items-center gap-1 text-sm text-gray-500">
                <MapPin className="h-3.5 w-3.5" />
                {[provider.cityName, provider.areaName]
                  .filter(Boolean)
                  .join(', ')}
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
                <span className="ml-1 text-sm text-gray-500">
                  {provider.avgRating.toFixed(1)} ({provider.reviewCount}{' '}
                  {t.providers.reviews})
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
            </div>

            {/* Action buttons */}
            <div className="flex gap-2 sm:flex-col">
              <button
                onClick={() => {
                  if (!user) {
                    alert(t.provider.loginToMessage);
                    return;
                  }
                  window.location.href = `/messages?providerId=${provider.id}`;
                }}
                className="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2 text-sm font-medium text-gray-700 transition hover:border-rose-300 hover:text-rose-500"
              >
                <MessageCircle className="h-4 w-4" />
                {t.provider.sendMessage}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-5xl px-4 py-8">
        {/* Description */}
        {provider.description && (
          <section className="rounded-xl bg-white p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-gray-900">{t.provider.about}</h2>
            <p className="mt-2 text-gray-600 whitespace-pre-line leading-relaxed">
              {provider.description}
            </p>
          </section>
        )}

        {/* Services */}
        <section className="mt-6">
          <h2 className="text-lg font-semibold text-gray-900">{t.provider.services}</h2>

          {services.length === 0 ? (
            <p className="mt-4 text-gray-400">
              {t.common.noResults}
            </p>
          ) : (
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              {services.map((service) => {
                const isSelected = selectedServiceIds.has(service.id);
                return (
                  <div
                    key={service.id}
                    className={clsx(
                      'relative flex flex-col rounded-xl border bg-white p-5 shadow-sm transition cursor-pointer',
                      isSelected
                        ? 'border-rose-400 ring-1 ring-rose-400'
                        : 'border-gray-100 hover:border-rose-200'
                    )}
                    onClick={() => toggleService(service.id)}
                    role="checkbox"
                    aria-checked={isSelected}
                    tabIndex={0}
                    onKeyDown={(e) => {
                      if (e.key === ' ' || e.key === 'Enter') {
                        e.preventDefault();
                        toggleService(service.id);
                      }
                    }}
                  >
                    {/* Checkbox indicator */}
                    <div
                      className={clsx(
                        'absolute top-4 right-4 flex h-5 w-5 items-center justify-center rounded border transition',
                        isSelected
                          ? 'border-rose-500 bg-rose-500 text-white'
                          : 'border-gray-300 bg-white'
                      )}
                    >
                      {isSelected && <Check className="h-3.5 w-3.5" />}
                    </div>

                    <h3 className="pr-8 text-base font-semibold text-gray-900">
                      {service.title}
                    </h3>

                    {service.description && (
                      <p className="mt-1 text-sm text-gray-500 line-clamp-2">
                        {service.description}
                      </p>
                    )}

                    <div className="mt-auto flex items-center justify-between gap-4 pt-3">
                      <div className="flex items-center gap-3 text-sm text-gray-500">
                        <span className="flex items-center gap-1">
                          <Clock className="h-3.5 w-3.5" />
                          {formatDuration(service.durationMinutes)}
                        </span>
                        <span className="text-xs text-gray-400">
                          {formatPricingType(service.pricingType)}
                        </span>
                      </div>

                      <span className="text-base font-bold text-rose-600">
                        {formatPrice(service.priceAmount, service.priceCurrency)}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>

        {/* Sticky Booking Bar */}
        {selectedServiceIds.size > 0 && (
          <div className="fixed bottom-0 inset-x-0 z-50 border-t border-gray-200 bg-white/95 backdrop-blur-sm shadow-lg">
            <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-3">
              <div>
                <p className="text-sm text-gray-500">
                  {selectedServiceIds.size} {t.provider.selected}
                  <span className="mx-1.5 text-gray-300">|</span>
                  {formatDuration(totalDuration)}
                </p>
                <p className="text-lg font-bold text-gray-900">
                  {t.provider.total}: {formatPrice(runningTotal, currency)}
                </p>
              </div>
              <button
                onClick={() => {
                  if (!user) {
                    alert(t.provider.loginToBook);
                    return;
                  }
                  const serviceIds = Array.from(selectedServiceIds).join(',');
                  window.location.href = `/book?providerId=${provider.id}&services=${serviceIds}`;
                }}
                className="rounded-lg bg-rose-500 px-6 py-2.5 text-sm font-semibold text-white shadow transition hover:bg-rose-600"
              >
                {t.provider.bookNow}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
