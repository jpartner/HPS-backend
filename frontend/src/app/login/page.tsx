'use client';

import { useState, type FormEvent } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Mail, Lock, LogIn } from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { useLanguage } from '@/lib/i18n';
import { authApi, ApiError } from '@/lib/api';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';

export default function LoginPage() {
  const router = useRouter();
  const auth = useAuth();
  const { t } = useLanguage();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await authApi.login({ email, password });
      auth.login(res.accessToken, res.refreshToken);
      // Parse the token to get the user role and redirect accordingly
      const payload = res.accessToken.split('.')[1];
      const padded = payload + '='.repeat((4 - payload.length % 4) % 4);
      const decoded = JSON.parse(atob(padded));
      if (decoded.role === 'PROVIDER') {
        router.push('/dashboard');
      } else {
        router.push('/');
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError(t.common.error);
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-rose-50 to-pink-50 px-4 py-12">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-full bg-rose-100 mb-4">
            <LogIn className="h-7 w-7 text-rose-600" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">{t.auth.loginTitle}</h1>
          <p className="mt-1 text-sm text-gray-500">{t.auth.loginSubtitle}</p>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 sm:p-8">
          {error && (
            <div className="mb-4 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="relative">
              <Input
                label={t.auth.email}
                type="email"
                name="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
              />
              <Mail className="absolute right-3 top-[2.15rem] h-4 w-4 text-gray-400 pointer-events-none" />
            </div>

            <div className="relative">
              <Input
                label={t.auth.password}
                type="password"
                name="password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
              />
              <Lock className="absolute right-3 top-[2.15rem] h-4 w-4 text-gray-400 pointer-events-none" />
            </div>

            <Button
              type="submit"
              loading={loading}
              className="w-full"
              size="lg"
            >
              {t.nav.login}
            </Button>
          </form>
        </div>

        <p className="mt-6 text-center text-sm text-gray-500">
          {t.auth.noAccount}{' '}
          <Link
            href="/register"
            className="font-medium text-rose-600 hover:text-rose-500 transition-colors"
          >
            {t.auth.signUpLink}
          </Link>
        </p>
      </div>
    </div>
  );
}
