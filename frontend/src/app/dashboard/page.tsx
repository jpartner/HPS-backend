'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { format } from 'date-fns';
import {
  LayoutDashboard,
  Calendar,
  MessageSquare,
  Clock,
  Briefcase,
  Settings,
  Loader2,
  AlertCircle,
  ArrowRight,
  ChevronRight,
} from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { useLanguage } from '@/lib/i18n';
import { bookingsApi, messagingApi, ApiError, type BookingDto } from '@/lib/api';
import Badge from '@/components/ui/Badge';
import Card from '@/components/ui/Card';

const STATUS_STYLES: Record<string, string> = {
  REQUESTED: 'bg-yellow-50 text-yellow-700 border-yellow-200',
  CONFIRMED: 'bg-blue-50 text-blue-700 border-blue-200',
  QUOTED: 'bg-orange-50 text-orange-700 border-orange-200',
  COMPLETED: 'bg-green-50 text-green-700 border-green-200',
  CANCELLED: 'bg-red-50 text-red-700 border-red-200',
  IN_PROGRESS: 'bg-indigo-50 text-indigo-700 border-indigo-200',
};

export default function DashboardPage() {
  const router = useRouter();
  const { user, token, isLoading: authLoading } = useAuth();
  const { t } = useLanguage();

  const [bookings, setBookings] = useState<BookingDto[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchData = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError('');

    try {
      const [bookingsData, unreadData] = await Promise.all([
        bookingsApi.list(token),
        messagingApi.unreadCount(token),
      ]);
      setBookings(bookingsData);
      setUnreadCount(unreadData.unread);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load dashboard data');
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
    if (user.role !== 'PROVIDER') {
      router.push('/');
      return;
    }
    fetchData();
  }, [user, token, authLoading, router, fetchData]);

  const upcomingBookings = bookings.filter(
    (b) =>
      (b.status === 'CONFIRMED' || b.status === 'REQUESTED') &&
      new Date(b.scheduledAt) >= new Date()
  );
  const recentBookings = bookings
    .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
    .slice(0, 5);

  if (authLoading || (!user && !error)) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <Loader2 className="h-8 w-8 animate-spin text-rose-500" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-5xl mx-auto px-4 py-8 sm:py-12">
        {/* Header */}
        <div className="flex items-center gap-3 mb-8">
          <div className="w-10 h-10 rounded-full bg-rose-100 flex items-center justify-center">
            <LayoutDashboard className="h-5 w-5 text-rose-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{t.dashboard.title}</h1>
            <p className="text-sm text-gray-500">Welcome back, {user?.email}</p>
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
        ) : (
          <>
            {/* Stats Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
              <Card>
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-xl bg-blue-50 flex items-center justify-center">
                    <Calendar className="h-6 w-6 text-blue-600" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-gray-900">
                      {upcomingBookings.length}
                    </p>
                    <p className="text-sm text-gray-500">{t.dashboard.upcomingBookings}</p>
                  </div>
                </div>
              </Card>

              <Card>
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-xl bg-rose-50 flex items-center justify-center">
                    <MessageSquare className="h-6 w-6 text-rose-600" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-gray-900">{unreadCount}</p>
                    <p className="text-sm text-gray-500">{t.dashboard.unreadMessages}</p>
                  </div>
                </div>
              </Card>

              <Card className="sm:col-span-2 lg:col-span-1">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-xl bg-green-50 flex items-center justify-center">
                    <Briefcase className="h-6 w-6 text-green-600" />
                  </div>
                  <div>
                    <p className="text-2xl font-bold text-gray-900">
                      {bookings.filter((b) => b.status === 'COMPLETED').length}
                    </p>
                    <p className="text-sm text-gray-500">{t.dashboard.completedBookings}</p>
                  </div>
                </div>
              </Card>
            </div>

            {/* Quick Links */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
              <Link
                href="/dashboard/services"
                className="flex items-center gap-3 bg-white rounded-xl border border-gray-100 shadow-sm px-5 py-4 hover:shadow-md transition-shadow group"
              >
                <div className="w-10 h-10 rounded-lg bg-rose-50 flex items-center justify-center">
                  <Settings className="h-5 w-5 text-rose-500" />
                </div>
                <div className="flex-1">
                  <p className="text-sm font-semibold text-gray-900">{t.dashboard.manageServices}</p>
                  <p className="text-xs text-gray-400">{t.common.edit}</p>
                </div>
                <ChevronRight className="h-4 w-4 text-gray-300 group-hover:text-rose-400 transition-colors" />
              </Link>

              <Link
                href="/bookings"
                className="flex items-center gap-3 bg-white rounded-xl border border-gray-100 shadow-sm px-5 py-4 hover:shadow-md transition-shadow group"
              >
                <div className="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
                  <Calendar className="h-5 w-5 text-blue-500" />
                </div>
                <div className="flex-1">
                  <p className="text-sm font-semibold text-gray-900">{t.dashboard.viewBookings}</p>
                  <p className="text-xs text-gray-400">{t.common.viewAll}</p>
                </div>
                <ChevronRight className="h-4 w-4 text-gray-300 group-hover:text-rose-400 transition-colors" />
              </Link>

              <Link
                href="/messages"
                className="flex items-center gap-3 bg-white rounded-xl border border-gray-100 shadow-sm px-5 py-4 hover:shadow-md transition-shadow group"
              >
                <div className="w-10 h-10 rounded-lg bg-amber-50 flex items-center justify-center">
                  <MessageSquare className="h-5 w-5 text-amber-500" />
                </div>
                <div className="flex-1">
                  <p className="text-sm font-semibold text-gray-900">{t.dashboard.viewMessages}</p>
                  <p className="text-xs text-gray-400">
                    {unreadCount > 0 ? `${unreadCount} ${t.messaging.unread}` : t.common.noResults}
                  </p>
                </div>
                <ChevronRight className="h-4 w-4 text-gray-300 group-hover:text-rose-400 transition-colors" />
              </Link>
            </div>

            {/* Recent Bookings */}
            <Card
              header={
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-semibold text-gray-900">{t.dashboard.recentBookings}</h3>
                  <Link
                    href="/bookings"
                    className="text-sm text-rose-500 hover:text-rose-600 font-medium flex items-center gap-1"
                  >
                    {t.common.viewAll} <ArrowRight className="h-3.5 w-3.5" />
                  </Link>
                </div>
              }
            >
              {recentBookings.length === 0 ? (
                <div className="text-center py-8 text-gray-400">
                  <Calendar className="h-10 w-10 mx-auto mb-2" />
                  <p className="text-sm">{t.dashboard.noRecentBookings}</p>
                </div>
              ) : (
                <div className="divide-y divide-gray-50 -mx-5 -my-4">
                  {recentBookings.map((booking) => (
                    <Link
                      key={booking.id}
                      href="/bookings"
                      className="flex items-center gap-4 px-5 py-3.5 hover:bg-gray-50 transition-colors"
                    >
                      <div className="shrink-0">
                        <div className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center">
                          <Clock className="h-4 w-4 text-gray-500" />
                        </div>
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-gray-900 truncate">
                          {booking.clientName}
                        </p>
                        <div className="flex items-center gap-2 mt-0.5">
                          <span className="text-xs text-gray-400">
                            {format(new Date(booking.scheduledAt), 'MMM d, h:mm a')}
                          </span>
                          <span className="text-xs text-gray-300">|</span>
                          <span className="text-xs text-gray-500 truncate">
                            {booking.services.map((s) => s.serviceTitle).join(', ')}
                          </span>
                        </div>
                      </div>
                      <Badge className={STATUS_STYLES[booking.status] || ''}>
                        {(t.booking.status as Record<string, string>)[booking.status] || booking.status}
                      </Badge>
                    </Link>
                  ))}
                </div>
              )}
            </Card>
          </>
        )}
      </div>
    </div>
  );
}
