'use client';

import { useState, useCallback, type ChangeEvent } from 'react';
import { clsx } from 'clsx';
import { Input } from './Input';
import { useLanguages } from '@/lib/use-languages';

// Complex multi-field mode
export interface FieldConfig {
  key: string;
  label: string;
  type?: 'text' | 'textarea';
  required?: boolean;
}

export type TranslationData = Record<string, Record<string, string>>;

interface MultiFieldProps {
  fields: FieldConfig[];
  value: TranslationData;
  onChange: (data: TranslationData) => void;
  errors?: Record<string, Record<string, string>>;
  // discriminator
  label?: never;
  values?: never;
  multiline?: never;
}

// Simple single-field mode
interface SingleFieldProps {
  label: string;
  values: Record<string, string>;
  onChange: (values: Record<string, string>) => void;
  multiline?: boolean;
  // discriminator
  fields?: never;
  value?: never;
  errors?: never;
}

type TranslationFormProps = MultiFieldProps | SingleFieldProps;

export function TranslationForm(props: TranslationFormProps) {
  const { languages, defaultLang } = useLanguages();
  const [activeLang, setActiveLang] = useState(defaultLang);

  // Ensure activeLang is valid when languages change
  const currentLang = languages.some((l) => l.code === activeLang) ? activeLang : languages[0]?.code ?? 'en';

  const langTabs = (
    <div className="mb-3 flex gap-1 rounded-lg bg-slate-100 p-1">
      {languages.map((lang) => (
        <button
          key={lang.code}
          type="button"
          onClick={() => setActiveLang(lang.code)}
          className={clsx(
            'flex-1 rounded-md px-3 py-1.5 text-sm font-medium transition-colors',
            currentLang === lang.code
              ? 'bg-white text-slate-900 shadow-sm'
              : 'text-slate-500 hover:text-slate-700',
          )}
        >
          {lang.code.toUpperCase()}
        </button>
      ))}
    </div>
  );

  // Simple single-field mode
  if ('label' in props && props.label !== undefined) {
    const { label, values, onChange, multiline } = props;
    const fieldValue = values[currentLang] ?? '';

    return (
      <div>
        <label className="mb-1.5 block text-sm font-medium text-slate-700">{label}</label>
        {langTabs}
        {multiline ? (
          <textarea
            value={fieldValue}
            onChange={(e: ChangeEvent<HTMLTextAreaElement>) =>
              onChange({ ...values, [currentLang]: e.target.value })
            }
            rows={3}
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-1 focus:border-primary focus:ring-primary transition-colors"
          />
        ) : (
          <Input
            value={fieldValue}
            onChange={(e) => onChange({ ...values, [currentLang]: e.target.value })}
          />
        )}
      </div>
    );
  }

  // Complex multi-field mode
  const { fields, value, onChange, errors } = props as MultiFieldProps;

  return (
    <div>
      {langTabs}
      <div className="space-y-4">
        {fields.map((field) => {
          const fieldValue = value[currentLang]?.[field.key] ?? '';
          const fieldError = errors?.[currentLang]?.[field.key];

          if (field.type === 'textarea') {
            return (
              <div key={field.key}>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  {field.label}
                  {field.required && currentLang === defaultLang && (
                    <span className="ml-1 text-red-500">*</span>
                  )}
                </label>
                <textarea
                  value={fieldValue}
                  onChange={(e: ChangeEvent<HTMLTextAreaElement>) => {
                    const updated = { ...value };
                    updated[currentLang] = { ...(updated[currentLang] || {}), [field.key]: e.target.value };
                    onChange(updated);
                  }}
                  rows={4}
                  className={clsx(
                    'w-full rounded-lg border bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-1 transition-colors',
                    fieldError
                      ? 'border-red-500 focus:border-red-500 focus:ring-red-500'
                      : 'border-slate-200 focus:border-primary focus:ring-primary',
                  )}
                />
                {fieldError && <p className="mt-1 text-sm text-red-500">{fieldError}</p>}
              </div>
            );
          }

          return (
            <Input
              key={field.key}
              label={`${field.label}${field.required && currentLang === defaultLang ? ' *' : ''}`}
              value={fieldValue}
              onChange={(e) => {
                const updated = { ...value };
                updated[currentLang] = { ...(updated[currentLang] || {}), [field.key]: e.target.value };
                onChange(updated);
              }}
              error={fieldError}
            />
          );
        })}
      </div>
    </div>
  );
}
