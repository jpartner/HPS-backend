'use client';

import { useState, useRef, type FormEvent } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { UserPlus, Info } from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { useLanguage } from '@/lib/i18n';
import { authApi, messagingApi, ApiError } from '@/lib/api';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';

type SelectedRole = 'CLIENT' | 'PROVIDER';

export default function RegisterPage() {
  const router = useRouter();
  const auth = useAuth();
  const { t } = useLanguage();

  const [selectedRole, setSelectedRole] = useState<SelectedRole>('CLIENT');
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    handle: '',
  });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [handleStatus, setHandleStatus] = useState<'idle' | 'checking' | 'available' | 'taken'>('idle');
  const handleTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  function updateField(field: string, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
    setFieldErrors((prev) => ({ ...prev, [field]: '' }));

    if (field === 'handle') {
      const h = value.toLowerCase().replace(/[^a-z0-9_]/g, '');
      setForm((prev) => ({ ...prev, handle: h }));
      if (handleTimerRef.current) clearTimeout(handleTimerRef.current);
      if (h.length < 3) {
        setHandleStatus('idle');
        return;
      }
      setHandleStatus('checking');
      handleTimerRef.current = setTimeout(async () => {
        try {
          const res = await messagingApi.handleAvailable(h);
          setHandleStatus(res.available ? 'available' : 'taken');
          if (!res.available && res.reason) {
            setFieldErrors((prev) => ({ ...prev, handle: res.reason! }));
          }
        } catch {
          setHandleStatus('idle');
        }
      }, 400);
    }
  }

  function validate(): boolean {
    const errors: Record<string, string> = {};
    if (!form.firstName.trim()) errors.firstName = 'First name is required';
    if (!form.lastName.trim()) errors.lastName = 'Last name is required';
    if (!form.email.trim()) errors.email = 'Email is required';
    if (form.password.length < 8) errors.password = 'Password must be at least 8 characters';
    if (form.handle && form.handle.length < 3) errors.handle = 'Handle must be at least 3 characters';
    if (handleStatus === 'taken') errors.handle = 'Handle is not available';
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');

    if (!validate()) return;

    setLoading(true);
    try {
      const res = await authApi.register({
        email: form.email,
        password: form.password,
        firstName: form.firstName,
        lastName: form.lastName,
        ...(form.handle ? { handle: form.handle } : {}),
      });
      auth.login(res.accessToken, res.refreshToken);
      if (selectedRole === 'PROVIDER') {
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
            <UserPlus className="h-7 w-7 text-rose-600" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">{t.auth.registerTitle}</h1>
          <p className="mt-1 text-sm text-gray-500">{t.auth.registerSubtitle}</p>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 sm:p-8">
          {/* Role selector tabs */}
          <div className="flex rounded-lg border border-gray-200 mb-6 overflow-hidden">
            <button
              type="button"
              onClick={() => setSelectedRole('CLIENT')}
              className={`flex-1 px-4 py-2.5 text-sm font-medium transition-colors cursor-pointer ${
                selectedRole === 'CLIENT'
                  ? 'bg-rose-600 text-white'
                  : 'bg-white text-gray-600 hover:bg-gray-50'
              }`}
            >
              {t.auth.asClient}
            </button>
            <button
              type="button"
              onClick={() => setSelectedRole('PROVIDER')}
              className={`flex-1 px-4 py-2.5 text-sm font-medium transition-colors cursor-pointer ${
                selectedRole === 'PROVIDER'
                  ? 'bg-rose-600 text-white'
                  : 'bg-white text-gray-600 hover:bg-gray-50'
              }`}
            >
              {t.auth.asProvider}
            </button>
          </div>

          {selectedRole === 'PROVIDER' && (
            <div className="mb-4 rounded-lg bg-blue-50 border border-blue-200 px-4 py-3 text-sm text-blue-700 flex items-start gap-2">
              <Info className="h-4 w-4 mt-0.5 flex-shrink-0" />
              <span>{t.auth.providerSetupNote}</span>
            </div>
          )}

          {error && (
            <div className="mb-4 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label={t.auth.firstName}
                name="firstName"
                placeholder="Jane"
                value={form.firstName}
                onChange={(e) => updateField('firstName', e.target.value)}
                error={fieldErrors.firstName}
                required
                autoComplete="given-name"
              />
              <Input
                label={t.auth.lastName}
                name="lastName"
                placeholder="Doe"
                value={form.lastName}
                onChange={(e) => updateField('lastName', e.target.value)}
                error={fieldErrors.lastName}
                required
                autoComplete="family-name"
              />
            </div>

            <Input
              label={t.auth.email}
              type="email"
              name="email"
              placeholder="you@example.com"
              value={form.email}
              onChange={(e) => updateField('email', e.target.value)}
              error={fieldErrors.email}
              required
              autoComplete="email"
            />

            <Input
              label={t.auth.password}
              type="password"
              name="password"
              placeholder="At least 8 characters"
              value={form.password}
              onChange={(e) => updateField('password', e.target.value)}
              error={fieldErrors.password}
              required
              autoComplete="new-password"
            />

            <div>
              <Input
                label="Handle"
                name="handle"
                placeholder="e.g. bobby123"
                value={form.handle}
                onChange={(e) => updateField('handle', e.target.value)}
                error={fieldErrors.handle}
                autoComplete="username"
              />
              <div className="mt-1 h-4 text-xs">
                {form.handle.length >= 3 && handleStatus === 'checking' && (
                  <span className="text-gray-400">Checking...</span>
                )}
                {form.handle.length >= 3 && handleStatus === 'available' && (
                  <span className="text-green-600">@{form.handle} is available</span>
                )}
                {form.handle.length >= 3 && handleStatus === 'taken' && !fieldErrors.handle && (
                  <span className="text-red-600">@{form.handle} is not available</span>
                )}
              </div>
            </div>

            <Button
              type="submit"
              loading={loading}
              className="w-full"
              size="lg"
            >
              {t.nav.register}
            </Button>
          </form>
        </div>

        <p className="mt-6 text-center text-sm text-gray-500">
          {t.auth.hasAccount}{' '}
          <Link
            href="/login"
            className="font-medium text-rose-600 hover:text-rose-500 transition-colors"
          >
            {t.auth.loginLink}
          </Link>
        </p>
      </div>
    </div>
  );
}
