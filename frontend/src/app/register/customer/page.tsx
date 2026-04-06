'use client';

import { useState, useRef, type FormEvent } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { User } from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { useLanguage } from '@/lib/i18n';
import { authApi, messagingApi, ApiError } from '@/lib/api';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';

export default function CustomerRegisterPage() {
  const router = useRouter();
  const auth = useAuth();
  const { t } = useLanguage();

  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '', handle: '' });
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

  function validate(): boolean {
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
        handle: form.handle,
      });
      auth.login(res.accessToken, res.refreshToken);
      router.push('/');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : t.common.error);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-rose-50 to-pink-50 px-4 py-12">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-full bg-blue-100 mb-4">
            <User className="h-7 w-7 text-blue-600" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Create Client Account</h1>
          <p className="mt-1 text-sm text-gray-500">Browse and book services</p>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 sm:p-8">
          {error && (
            <div className="mb-4 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">{error}</div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input label={t.auth.firstName} value={form.firstName} onChange={(e) => updateField('firstName', e.target.value)} error={fieldErrors.firstName} required autoComplete="given-name" />
              <Input label={t.auth.lastName} value={form.lastName} onChange={(e) => updateField('lastName', e.target.value)} error={fieldErrors.lastName} required autoComplete="family-name" />
            </div>

            <div>
              <Input label="Handle" value={form.handle} onChange={(e) => updateField('handle', e.target.value)} error={fieldErrors.handle} placeholder="e.g. bobby123" autoComplete="username" required />
              <div className="mt-1 h-4 text-xs">
                {form.handle.length >= 3 && handleStatus === 'checking' && <span className="text-gray-400">Checking...</span>}
                {form.handle.length >= 3 && handleStatus === 'available' && <span className="text-green-600">@{form.handle} is available</span>}
                {form.handle.length >= 3 && handleStatus === 'taken' && !fieldErrors.handle && <span className="text-red-600">@{form.handle} is not available</span>}
              </div>
            </div>

            <Input label={t.auth.email} type="email" value={form.email} onChange={(e) => updateField('email', e.target.value)} error={fieldErrors.email} required autoComplete="email" />
            <Input label={t.auth.password} type="password" value={form.password} onChange={(e) => updateField('password', e.target.value)} error={fieldErrors.password} placeholder="At least 8 characters" required autoComplete="new-password" />

            <Button type="submit" loading={loading} className="w-full" size="lg">Create Account</Button>
          </form>
        </div>

        <p className="mt-6 text-center text-sm text-gray-500">
          {t.auth.hasAccount}{' '}
          <Link href="/login" className="font-medium text-rose-600 hover:text-rose-500 transition-colors">{t.auth.loginLink}</Link>
          {' · '}
          <Link href="/register/provider" className="font-medium text-rose-600 hover:text-rose-500 transition-colors">Register as Provider</Link>
        </p>
      </div>
    </div>
  );
}
