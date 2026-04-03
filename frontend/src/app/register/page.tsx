'use client';

import { useState, type FormEvent } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { UserPlus } from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { authApi, ApiError } from '@/lib/api';
import Input from '@/components/ui/Input';
import Button from '@/components/ui/Button';

export default function RegisterPage() {
  const router = useRouter();
  const auth = useAuth();

  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
  });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  function updateField(field: string, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
    setFieldErrors((prev) => ({ ...prev, [field]: '' }));
  }

  function validate(): boolean {
    const errors: Record<string, string> = {};
    if (!form.firstName.trim()) errors.firstName = 'First name is required';
    if (!form.lastName.trim()) errors.lastName = 'Last name is required';
    if (!form.email.trim()) errors.email = 'Email is required';
    if (form.password.length < 8) errors.password = 'Password must be at least 8 characters';
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
      });
      auth.login(res.accessToken, res.refreshToken);
      router.push('/');
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Something went wrong. Please try again.');
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
          <h1 className="text-2xl font-bold text-gray-900">Create an account</h1>
          <p className="mt-1 text-sm text-gray-500">Get started with your free account</p>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 sm:p-8">
          {error && (
            <div className="mb-4 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="First name"
                name="firstName"
                placeholder="Jane"
                value={form.firstName}
                onChange={(e) => updateField('firstName', e.target.value)}
                error={fieldErrors.firstName}
                required
                autoComplete="given-name"
              />
              <Input
                label="Last name"
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
              label="Email"
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
              label="Password"
              type="password"
              name="password"
              placeholder="At least 8 characters"
              value={form.password}
              onChange={(e) => updateField('password', e.target.value)}
              error={fieldErrors.password}
              required
              autoComplete="new-password"
            />

            <Button
              type="submit"
              loading={loading}
              className="w-full"
              size="lg"
            >
              Create account
            </Button>
          </form>
        </div>

        <p className="mt-6 text-center text-sm text-gray-500">
          Already have an account?{' '}
          <Link
            href="/login"
            className="font-medium text-rose-600 hover:text-rose-500 transition-colors"
          >
            Login
          </Link>
        </p>
      </div>
    </div>
  );
}
