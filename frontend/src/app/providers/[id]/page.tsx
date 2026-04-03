'use client';

import { use, useState, useEffect, useMemo } from 'react';
import Link from 'next/link';
import clsx from 'clsx';
import {
  Star,
  ChevronLeft,
  ChevronRight,
  MapPin,
  BadgeCheck,
  Clock,
  MessageCircle,
  Loader2,
  Check,
  X,
  Image as ImageIcon,
} from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { useLanguage } from '@/lib/i18n';
import {
  providersApi,
  servicesApi,
  type ProviderDetail,
  type ProviderAttribute,
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
  const [attributes, setAttributes] = useState<ProviderAttribute[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Service selection for booking
  const [selectedServiceIds, setSelectedServiceIds] = useState<Set<string>>(
    new Set()
  );

  // Gallery
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const [lightboxOpen, setLightboxOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [providerData, serviceData, attrData] = await Promise.all([
          providersApi.get(providerId, lang),
          servicesApi.listByProvider(providerId, lang),
          providersApi.attributes(providerId, lang).catch(() => [] as ProviderAttribute[]),
        ]);
        if (!cancelled) {
          setProvider(providerData);
          setServices(serviceData.filter((s) => s.isActive));
          setAttributes(attrData);
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
            <div className="flex gap-4">
              {provider.avatarUrl && (
                <img
                  src={provider.avatarUrl}
                  alt={provider.businessName || ''}
                  className="h-16 w-16 rounded-full object-cover border-2 border-rose-100 sm:h-20 sm:w-20"
                />
              )}
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

        {/* Provider Attributes */}
        {(() => {
          // Split attributes into physical details and offered services
          const physicalAttrs = attributes.filter(a => a.definition.key !== 'offered_services' && a.value != null);
          const offeredServices = attributes.find(a => a.definition.key === 'offered_services');
          const extras = Array.isArray(offeredServices?.value) ? offeredServices.value as string[] : [];
          const extrasLabels = offeredServices?.definition?.optionLabels || {};

          // Helper to translate a SELECT/MULTI_SELECT value using optionLabels
          const translateValue = (attr: typeof attributes[0]) => {
            const val = attr.value;
            const labels = attr.definition.optionLabels;
            if (labels && typeof val === 'string' && labels[val]) return labels[val];
            return String(val);
          };

          if (physicalAttrs.length === 0 && extras.length === 0) return null;

          return (
            <section className="mt-6 rounded-xl bg-white p-6 shadow-sm">
              {/* Physical attributes */}
              {physicalAttrs.length > 0 && (
                <div>
                  <h2 className="text-lg font-semibold text-gray-900 mb-3">Details</h2>
                  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
                    {physicalAttrs.map(attr => (
                      <div key={attr.definition.key} className="rounded-lg bg-gray-50 px-3 py-2">
                        <p className="text-xs text-gray-500">{attr.definition.label}</p>
                        <p className="text-sm font-medium text-gray-900 mt-0.5">
                          {translateValue(attr)}
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Offered services / extras */}
              {extras.length > 0 && (
                <div className={physicalAttrs.length > 0 ? 'mt-5 pt-5 border-t border-gray-100' : ''}>
                  <h2 className="text-lg font-semibold text-gray-900 mb-3">Services Offered</h2>
                  <div className="flex flex-wrap gap-2">
                    {extras.map((extra, i) => (
                      <span
                        key={i}
                        className="inline-flex items-center rounded-full bg-rose-50 px-3 py-1 text-xs font-medium text-rose-700"
                      >
                        {extrasLabels[extra] || extra}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </section>
          );
        })()}

        {/* Gallery */}
        {provider.galleryImages && provider.galleryImages.length > 0 && (
          <section className="mt-6 rounded-xl bg-white p-6 shadow-sm">
            <div className="flex items-center gap-2 mb-4">
              <ImageIcon className="h-5 w-5 text-rose-500" />
              <h2 className="text-lg font-semibold text-gray-900">Gallery</h2>
            </div>

            {/* Main image */}
            <div className="relative">
              <button
                onClick={() => setLightboxOpen(true)}
                className="w-full cursor-pointer"
              >
                <img
                  src={provider.galleryImages[activeImageIndex].url}
                  alt={provider.galleryImages[activeImageIndex].caption || ''}
                  className="w-full aspect-[16/9] object-cover rounded-lg"
                />
              </button>

              {/* Prev/Next on main image */}
              {activeImageIndex > 0 && (
                <button
                  onClick={() => setActiveImageIndex(activeImageIndex - 1)}
                  className="absolute left-2 top-1/2 -translate-y-1/2 rounded-full bg-black/40 p-1.5 text-white hover:bg-black/60 transition cursor-pointer"
                >
                  <ChevronLeft className="h-5 w-5" />
                </button>
              )}
              {activeImageIndex < provider.galleryImages.length - 1 && (
                <button
                  onClick={() => setActiveImageIndex(activeImageIndex + 1)}
                  className="absolute right-2 top-1/2 -translate-y-1/2 rounded-full bg-black/40 p-1.5 text-white hover:bg-black/60 transition cursor-pointer"
                >
                  <ChevronRight className="h-5 w-5" />
                </button>
              )}

              {/* Caption overlay */}
              {provider.galleryImages[activeImageIndex].caption && (
                <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/60 to-transparent rounded-b-lg px-4 py-3">
                  <p className="text-sm text-white">{provider.galleryImages[activeImageIndex].caption}</p>
                </div>
              )}
            </div>

            {/* Thumbnail strip */}
            {provider.galleryImages.length > 1 && (
              <div className="mt-3 flex gap-2 overflow-x-auto pb-1">
                {provider.galleryImages.map((img, index) => (
                  <button
                    key={img.id}
                    onClick={() => setActiveImageIndex(index)}
                    className={clsx(
                      'flex-shrink-0 rounded-md overflow-hidden transition-all cursor-pointer',
                      index === activeImageIndex
                        ? 'ring-2 ring-rose-500 opacity-100'
                        : 'opacity-60 hover:opacity-90'
                    )}
                  >
                    <img
                      src={img.url}
                      alt={img.caption || `Thumbnail ${index + 1}`}
                      className="h-16 w-24 sm:h-20 sm:w-28 object-cover"
                      loading="lazy"
                    />
                  </button>
                ))}
              </div>
            )}
          </section>
        )}

        {/* Lightbox */}
        {lightboxOpen && provider.galleryImages && (
          <div
            className="fixed inset-0 z-[60] flex flex-col items-center justify-center bg-black/95"
            onClick={() => setLightboxOpen(false)}
          >
            <button
              onClick={() => setLightboxOpen(false)}
              className="absolute top-4 right-4 rounded-full bg-white/10 p-2 text-white hover:bg-white/20 transition cursor-pointer z-10"
            >
              <X className="h-6 w-6" />
            </button>

            <div className="relative flex items-center justify-center flex-1 w-full max-w-5xl px-12" onClick={(e) => e.stopPropagation()}>
              {activeImageIndex > 0 && (
                <button
                  onClick={() => setActiveImageIndex(activeImageIndex - 1)}
                  className="absolute left-2 rounded-full bg-white/10 p-2 text-white hover:bg-white/20 transition cursor-pointer"
                >
                  <ChevronLeft className="h-6 w-6" />
                </button>
              )}

              <img
                src={provider.galleryImages[activeImageIndex].url}
                alt={provider.galleryImages[activeImageIndex].caption || ''}
                className="max-h-[70vh] w-auto mx-auto rounded-lg object-contain"
              />

              {activeImageIndex < provider.galleryImages.length - 1 && (
                <button
                  onClick={() => setActiveImageIndex(activeImageIndex + 1)}
                  className="absolute right-2 rounded-full bg-white/10 p-2 text-white hover:bg-white/20 transition cursor-pointer"
                >
                  <ChevronRight className="h-6 w-6" />
                </button>
              )}
            </div>

            {/* Caption */}
            {provider.galleryImages[activeImageIndex].caption && (
              <p className="mt-2 text-center text-sm text-white/80">
                {provider.galleryImages[activeImageIndex].caption}
              </p>
            )}

            {/* Thumbnail strip in lightbox */}
            <div className="mt-4 mb-4 flex gap-2 overflow-x-auto px-4 max-w-full" onClick={(e) => e.stopPropagation()}>
              {provider.galleryImages.map((img, index) => (
                <button
                  key={img.id}
                  onClick={() => setActiveImageIndex(index)}
                  className={clsx(
                    'flex-shrink-0 rounded overflow-hidden transition-all cursor-pointer',
                    index === activeImageIndex
                      ? 'ring-2 ring-rose-500 opacity-100'
                      : 'opacity-40 hover:opacity-80'
                  )}
                >
                  <img
                    src={img.url}
                    alt=""
                    className="h-14 w-20 object-cover"
                  />
                </button>
              ))}
            </div>
          </div>
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
