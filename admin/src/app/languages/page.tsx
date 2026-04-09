'use client';

import { useState, useEffect, useCallback } from 'react';
import { Plus, Trash2, Star } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { adminLanguageApi, type LanguageDto } from '@/lib/api';
import { useLanguages } from '@/lib/use-languages';

const ALL_LANGUAGES: LanguageDto[] = [
  { code: 'en', name: 'English' },
  { code: 'pl', name: 'Polish' },
  { code: 'uk', name: 'Ukrainian' },
  { code: 'de', name: 'German' },
  { code: 'fr', name: 'French' },
  { code: 'es', name: 'Spanish' },
  { code: 'it', name: 'Italian' },
  { code: 'pt', name: 'Portuguese' },
  { code: 'nl', name: 'Dutch' },
  { code: 'cs', name: 'Czech' },
  { code: 'sk', name: 'Slovak' },
  { code: 'ro', name: 'Romanian' },
  { code: 'hu', name: 'Hungarian' },
  { code: 'bg', name: 'Bulgarian' },
  { code: 'hr', name: 'Croatian' },
  { code: 'sr', name: 'Serbian' },
  { code: 'sl', name: 'Slovenian' },
  { code: 'lt', name: 'Lithuanian' },
  { code: 'lv', name: 'Latvian' },
  { code: 'et', name: 'Estonian' },
  { code: 'fi', name: 'Finnish' },
  { code: 'sv', name: 'Swedish' },
  { code: 'da', name: 'Danish' },
  { code: 'no', name: 'Norwegian' },
  { code: 'el', name: 'Greek' },
  { code: 'tr', name: 'Turkish' },
  { code: 'ar', name: 'Arabic' },
  { code: 'he', name: 'Hebrew' },
  { code: 'zh', name: 'Chinese' },
  { code: 'ja', name: 'Japanese' },
  { code: 'ko', name: 'Korean' },
  { code: 'th', name: 'Thai' },
  { code: 'vi', name: 'Vietnamese' },
  { code: 'ru', name: 'Russian' },
  { code: 'hi', name: 'Hindi' },
];

export default function LanguagesPage() {
  const { refresh } = useLanguages();
  const [languages, setLanguages] = useState<LanguageDto[]>([]);
  const [defaultLang, setDefaultLang] = useState('en');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);
  const [addSearch, setAddSearch] = useState('');

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const config = await adminLanguageApi.get();
      setLanguages(config.supportedLangs);
      setDefaultLang(config.defaultLang);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load languages');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  async function save(langs: LanguageDto[], newDefault?: string) {
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const config = await adminLanguageApi.update({
        defaultLang: newDefault ?? defaultLang,
        supportedLangs: langs.map((l) => l.code),
      });
      setLanguages(config.supportedLangs);
      setDefaultLang(config.defaultLang);
      setSuccess('Languages updated.');
      refresh();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to update languages');
    } finally {
      setSaving(false);
    }
  }

  function handleAdd(lang: LanguageDto) {
    if (languages.some((l) => l.code === lang.code)) return;
    const updated = [...languages, lang];
    setShowAddModal(false);
    setAddSearch('');
    save(updated);
  }

  function handleRemove(code: string) {
    if (code === defaultLang) return; // Can't remove default
    const updated = languages.filter((l) => l.code !== code);
    save(updated);
  }

  function handleSetDefault(code: string) {
    setDefaultLang(code);
    save(languages, code);
  }

  const activeCodes = new Set(languages.map((l) => l.code));
  const availableToAdd = ALL_LANGUAGES.filter(
    (l) => !activeCodes.has(l.code) && (
      !addSearch ||
      l.name.toLowerCase().includes(addSearch.toLowerCase()) ||
      l.code.toLowerCase().includes(addSearch.toLowerCase())
    ),
  );

  return (
    <AdminLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Languages</h1>
            <p className="text-sm text-gray-500 mt-1">
              Manage supported languages for content translations
            </p>
          </div>
          <Button onClick={() => { setShowAddModal(true); setAddSearch(''); }} disabled={saving}>
            <Plus className="h-4 w-4 mr-2" />
            Add Language
          </Button>
        </div>

        {error && <div className="p-3 rounded bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>}
        {success && <div className="p-3 rounded bg-green-50 border border-green-200 text-green-700 text-sm">{success}</div>}

        {loading ? (
          <div className="flex items-center justify-center h-32 text-gray-500">Loading...</div>
        ) : (
          <div className="bg-white rounded-lg border border-gray-200">
            <div className="divide-y divide-gray-100">
              {languages.map((lang) => (
                <div key={lang.code} className="flex items-center justify-between px-6 py-4">
                  <div className="flex items-center gap-4">
                    <span className="inline-flex items-center justify-center w-10 h-10 rounded-lg bg-indigo-50 text-indigo-700 text-sm font-bold uppercase">
                      {lang.code}
                    </span>
                    <div>
                      <p className="text-sm font-medium text-gray-900">{lang.name}</p>
                      <p className="text-xs text-gray-500">{lang.code}</p>
                    </div>
                    {lang.code === defaultLang && (
                      <Badge variant="info">Default</Badge>
                    )}
                  </div>
                  <div className="flex items-center gap-2">
                    {lang.code !== defaultLang && (
                      <>
                        <button
                          onClick={() => handleSetDefault(lang.code)}
                          className="text-gray-400 hover:text-indigo-600 p-1"
                          title="Set as default"
                          disabled={saving}
                        >
                          <Star className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => handleRemove(lang.code)}
                          className="text-gray-400 hover:text-red-600 p-1"
                          title="Remove language"
                          disabled={saving}
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <p className="text-sm text-blue-800">
            Supported languages determine which translation tabs appear when editing categories, service templates,
            reference lists, and other translatable content. The default language is used as a fallback when
            a translation is missing.
          </p>
        </div>
      </div>

      <Modal open={showAddModal} onClose={() => setShowAddModal(false)} title="Add Language">
        <div className="space-y-4">
          <input
            type="text"
            value={addSearch}
            onChange={(e) => setAddSearch(e.target.value)}
            placeholder="Search languages..."
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            autoFocus
          />
          <div className="max-h-64 overflow-y-auto divide-y divide-gray-100 border border-gray-200 rounded-lg">
            {availableToAdd.length === 0 ? (
              <p className="text-sm text-gray-500 text-center py-4">No matching languages found</p>
            ) : (
              availableToAdd.map((lang) => (
                <button
                  key={lang.code}
                  onClick={() => handleAdd(lang)}
                  className="flex items-center gap-3 w-full px-4 py-3 text-left hover:bg-gray-50 transition-colors"
                >
                  <span className="inline-flex items-center justify-center w-8 h-8 rounded bg-gray-100 text-gray-600 text-xs font-bold uppercase">
                    {lang.code}
                  </span>
                  <span className="text-sm text-gray-900">{lang.name}</span>
                </button>
              ))
            )}
          </div>
        </div>
      </Modal>
    </AdminLayout>
  );
}
