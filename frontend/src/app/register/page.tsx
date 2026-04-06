'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { UserPlus, Briefcase, User } from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { useLanguage } from '@/lib/i18n';

export default function RegisterPage() {
  const router = useRouter();
  const { user } = useAuth();
  const { t } = useLanguage();

  if (user) {
    router.replace(user.role === 'PROVIDER' ? '/dashboard' : '/');
    return null;
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-rose-50 to-pink-50 px-4 py-12">
      <div className="w-full max-w-lg">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-full bg-rose-100 mb-4">
            <UserPlus className="h-7 w-7 text-rose-600" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">{t.auth.registerTitle}</h1>
          <p className="mt-1 text-sm text-gray-500">{t.auth.registerSubtitle}</p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Link
            href="/register/customer"
            className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 hover:border-rose-300 hover:shadow-md transition-all group"
          >
            <div className="flex flex-col items-center text-center">
              <div className="w-14 h-14 rounded-full bg-blue-50 flex items-center justify-center mb-4 group-hover:bg-blue-100 transition-colors">
                <User className="h-7 w-7 text-blue-600" />
              </div>
              <h2 className="text-lg font-semibold text-gray-900 mb-1">{t.auth.asClient}</h2>
              <p className="text-sm text-gray-500">
                Browse services and book appointments with providers
              </p>
            </div>
          </Link>

          <Link
            href="/register/provider"
            className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 hover:border-rose-300 hover:shadow-md transition-all group"
          >
            <div className="flex flex-col items-center text-center">
              <div className="w-14 h-14 rounded-full bg-rose-50 flex items-center justify-center mb-4 group-hover:bg-rose-100 transition-colors">
                <Briefcase className="h-7 w-7 text-rose-600" />
              </div>
              <h2 className="text-lg font-semibold text-gray-900 mb-1">{t.auth.asProvider}</h2>
              <p className="text-sm text-gray-500">
                Set up your business profile and offer services
              </p>
            </div>
          </Link>
        </div>

        <p className="mt-6 text-center text-sm text-gray-500">
          {t.auth.hasAccount}{' '}
          <Link href="/login" className="font-medium text-rose-600 hover:text-rose-500 transition-colors">
            {t.auth.loginLink}
          </Link>
        </p>
      </div>
    </div>
  );
}
