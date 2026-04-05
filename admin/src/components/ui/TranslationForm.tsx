'use client';

import { useState, useCallback, type ChangeEvent } from 'react';
import { clsx } from 'clsx';
import { Input } from './Input';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

const LANGUAGES = [
  { code: 'en', label: 'EN' },
  { code: 'pl', label: 'PL' },
  { code: 'uk', label: 'UK' },
  { code: 'de', label: 'DE' },
] as const;

type LangCode = (typeof LANGUAGES)[number]['code'];

export interface FieldConfig {
  key: string;
  label: string;
  type?: 'text' | 'textarea';
  required?: boolean;
}

export type TranslationData = Record<
  string,
  Record<string, string>
>;

interface TranslationFormProps {
  fields: FieldConfig[];
  value: TranslationData;
  onChange: (data: TranslationData) => void;
  errors?: Record<string, Record<string, string>>;
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function TranslationForm({
  fields,
  value,
  onChange,
  errors,
}: TranslationFormProps) {
  const [activeLang, setActiveLang] = useState<LangCode>('en');

  const handleFieldChange = useCallback(
    (lang: string, fieldKey: string, fieldValue: string) => {
      const updated = { ...value };
      updated[lang] = { ...(updated[lang] || {}), [fieldKey]: fieldValue };
      onChange(updated);
    },
    [value, onChange],
  );

  return (
    <div>
      {/* Language tabs */}
      <div className="mb-4 flex gap-1 rounded-lg bg-slate-100 p-1">
        {LANGUAGES.map((lang) => (
          <button
            key={lang.code}
            type="button"
            onClick={() => setActiveLang(lang.code)}
            className={clsx(
              'flex-1 rounded-md px-3 py-1.5 text-sm font-medium transition-colors',
              activeLang === lang.code
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-500 hover:text-slate-700',
            )}
          >
            {lang.label}
          </button>
        ))}
      </div>

      {/* Fields for active language */}
      <div className="space-y-4">
        {fields.map((field) => {
          const fieldValue = value[activeLang]?.[field.key] ?? '';
          const fieldError = errors?.[activeLang]?.[field.key];

          if (field.type === 'textarea') {
            return (
              <div key={field.key}>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  {field.label}
                  {field.required && activeLang === 'en' && (
                    <span className="ml-1 text-error">*</span>
                  )}
                </label>
                <textarea
                  value={fieldValue}
                  onChange={(e: ChangeEvent<HTMLTextAreaElement>) =>
                    handleFieldChange(activeLang, field.key, e.target.value)
                  }
                  rows={4}
                  className={clsx(
                    'w-full rounded-lg border bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-1 transition-colors',
                    fieldError
                      ? 'border-error focus:border-error focus:ring-error'
                      : 'border-slate-200 focus:border-primary focus:ring-primary',
                  )}
                />
                {fieldError && (
                  <p className="mt-1 text-sm text-error">{fieldError}</p>
                )}
              </div>
            );
          }

          return (
            <Input
              key={field.key}
              label={`${field.label}${field.required && activeLang === 'en' ? ' *' : ''}`}
              value={fieldValue}
              onChange={(e) =>
                handleFieldChange(activeLang, field.key, e.target.value)
              }
              error={fieldError}
            />
          );
        })}
      </div>
    </div>
  );
}
