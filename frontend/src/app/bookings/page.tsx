'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { format } from 'date-fns';
import {
  Calendar,
  Clock,
  MapPin,
  ChevronDown,
  ChevronUp,
  Loader2,
  AlertCircle,
  CheckCircle2,
  XCircle,
  DollarSign,
} from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { useLanguage } from '@/lib/i18n';
import { bookingsApi, ApiError, type BookingDto } from '@/lib/api';
import Badge from '@/components/ui/Badge';
import Button from '@/components/ui/Button';

const STATUS_CLASSES: Record<string, string> = {
  REQUESTED: 'bg-yellow-50 text-yellow-700 border-yellow-200',
  CONFIRMED: 'bg-blue-50 text-blue-700 border-blue-200',
  QUOTED: 'bg-orange-50 text-orange-700 border-orange-200',
  COMPLETED: 'bg-green-50 text-green-700 border-green-200',
  CANCELLED: 'bg-red-50 text-red-700 border-red-200',
  CANCELLED_BY_CLIENT: 'bg-red-50 text-red-700 border-red-200',
  CANCELLED_BY_PROVIDER: 'bg-red-50 text-red-700 border-red-200',
  IN_PROGRESS: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  NO_SHOW: 'bg-gray-50 text-gray-700 border-gray-200',
};

export default function BookingsPage() {
  const router = useRouter();
  const { user, token, isLoading: authLoading } = useAuth();
  const { t } = useLanguage();

  const [bookings, setBookings] = useState<BookingDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const fetchBookings = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError('');
    try {
      const data = await bookingsApi.list(token);
      setBookings(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load bookings');
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    if (authLoading) return;
    if (!user || !token) {
      router.push('/login');
      return;
    }
    fetchBookings();
  }, [user, token, authLoading, router, fetchBookings]);

  async function handleStatusUpdate(bookingId: string, status: string) {
    if (!token) return;
    setActionLoading(bookingId);
    try {
      const updated = await bookingsApi.updateStatus(bookingId, { status }, token);
      setBookings((prev) => prev.map((b) => (b.id === bookingId ? updated : b)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to update booking');
    } finally {
      setActionLoading(null);
    }
  }

  function getActions(booking: BookingDto) {
    if (!user) return [];
    const isProvider = user.id === booking.providerId;
    const isClient = user.id === booking.clientId;
    const actions: { label: string; status: string; variant: 'primary' | 'danger' | 'outline' }[] = [];

    if (isProvider) {
      if (booking.status === 'REQUESTED') {
        actions.push({ label: t.booking.confirm, status: 'CONFIRMED', variant: 'primary' });
        actions.push({ label: t.booking.cancel, status: 'CANCELLED', variant: 'danger' });
      }
      if (booking.status === 'CONFIRMED') {
        actions.push({ label: t.booking.complete, status: 'COMPLETED', variant: 'primary' });
        actions.push({ label: t.booking.cancel, status: 'CANCELLED', variant: 'danger' });
      }
    }

    if (isClient) {
      if (booking.status === 'REQUESTED' || booking.status === 'CONFIRMED' || booking.status === 'QUOTED') {
        actions.push({ label: t.booking.cancel, status: 'CANCELLED', variant: 'danger' });
      }
      if (booking.status === 'QUOTED') {
        actions.push({ label: t.booking.confirm, status: 'CONFIRMED', variant: 'primary' });
      }
    }

    return actions;
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
      <div className="max-w-3xl mx-auto px-4 py-8 sm:py-12">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{t.booking.title}</h1>
            <p className="mt-1 text-sm text-gray-500">
              {bookings.length} booking{bookings.length !== 1 ? 's' : ''}
            </p>
          </div>
        </div>

        {error && (
          <div className="mb-6 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 flex items-center gap-2">
            <AlertCircle className="h-4 w-4 shrink-0" />
            {error}
          </div>
        )}

        {loading ? (
          <div className="flex flex-col items-center justify-center py-20 text-gray-400">
            <Loader2 className="h-8 w-8 animate-spin mb-3" />
            <p className="text-sm">{t.common.loading}</p>
          </div>
        ) : bookings.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-gray-400">
            <Calendar className="h-12 w-12 mb-3" />
            <p className="text-base font-medium text-gray-500">{t.booking.noBookings}</p>
            <p className="text-sm mt-1">{t.booking.browseProviders}</p>
          </div>
        ) : (
          <div className="space-y-4">
            {bookings.map((booking) => {
              const isExpanded = expandedId === booking.id;
              const actions = getActions(booking);

              return (
                <div
                  key={booking.id}
                  className="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden"
                >
                  <button
                    type="button"
                    onClick={() => setExpandedId(isExpanded ? null : booking.id)}
                    className="w-full text-left px-4 py-4 sm:px-6 cursor-pointer"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <p className="font-semibold text-gray-900 truncate">
                            {user?.id === booking.clientId
                              ? booking.providerName
                              : booking.clientName}
                          </p>
                          <Badge className={STATUS_CLASSES[booking.status] || STATUS_CLASSES.REQUESTED}>
                            {(t.booking.status as Record<string, string>)[booking.status] || booking.status}
                          </Badge>
                        </div>

                        <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-gray-500">
                          <span className="inline-flex items-center gap-1">
                            <Calendar className="h-3.5 w-3.5" />
                            {format(new Date(booking.scheduledAt), 'MMM d, yyyy')}
                          </span>
                          <span className="inline-flex items-center gap-1">
                            <Clock className="h-3.5 w-3.5" />
                            {format(new Date(booking.scheduledAt), 'h:mm a')}
                          </span>
                          {booking.priceAmount > 0 && (
                            <span className="inline-flex items-center gap-1">
                              <DollarSign className="h-3.5 w-3.5" />
                              {booking.priceAmount.toFixed(2)} {booking.priceCurrency}
                            </span>
                          )}
                        </div>

                        {booking.services.length > 0 && (
                          <p className="mt-1.5 text-sm text-gray-500 truncate">
                            {booking.services.map((s) => s.serviceTitle).join(', ')}
                          </p>
                        )}
                      </div>

                      <div className="shrink-0 text-gray-400">
                        {isExpanded ? (
                          <ChevronUp className="h-5 w-5" />
                        ) : (
                          <ChevronDown className="h-5 w-5" />
                        )}
                      </div>
                    </div>
                  </button>

                  {isExpanded && (
                    <div className="px-4 pb-4 sm:px-6 sm:pb-6 border-t border-gray-50">
                      <div className="pt-4 space-y-3">
                        {booking.addressText && (
                          <div className="flex items-start gap-2 text-sm text-gray-600">
                            <MapPin className="h-4 w-4 shrink-0 mt-0.5 text-gray-400" />
                            <span>{booking.addressText}</span>
                          </div>
                        )}

                        {booking.services.length > 0 && (
                          <div>
                            <p className="text-xs font-medium text-gray-400 uppercase tracking-wider mb-2">
                              {t.provider.services}
                            </p>
                            <div className="space-y-1.5">
                              {booking.services.map((svc) => (
                                <div
                                  key={svc.serviceId}
                                  className="flex items-center justify-between text-sm"
                                >
                                  <span className="text-gray-700">
                                    {svc.serviceTitle}
                                    {svc.quantity > 1 && (
                                      <span className="text-gray-400"> x{svc.quantity}</span>
                                    )}
                                  </span>
                                  <span className="text-gray-500 font-medium">
                                    {svc.lineTotal.toFixed(2)} {booking.priceCurrency}
                                  </span>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}

                        {booking.clientNotes && (
                          <div>
                            <p className="text-xs font-medium text-gray-400 uppercase tracking-wider mb-1">
                              {t.booking.notes}
                            </p>
                            <p className="text-sm text-gray-600">{booking.clientNotes}</p>
                          </div>
                        )}

                        {booking.providerNotes && (
                          <div>
                            <p className="text-xs font-medium text-gray-400 uppercase tracking-wider mb-1">
                              {t.booking.providerNotes}
                            </p>
                            <p className="text-sm text-gray-600">{booking.providerNotes}</p>
                          </div>
                        )}

                        {actions.length > 0 && (
                          <div className="flex flex-wrap gap-2 pt-2">
                            {actions.map((action) => (
                              <Button
                                key={action.status}
                                variant={action.variant}
                                size="sm"
                                loading={actionLoading === booking.id}
                                onClick={() => handleStatusUpdate(booking.id, action.status)}
                              >
                                {action.variant === 'primary' && (
                                  <CheckCircle2 className="h-3.5 w-3.5" />
                                )}
                                {action.variant === 'danger' && (
                                  <XCircle className="h-3.5 w-3.5" />
                                )}
                                {action.label}
                              </Button>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
