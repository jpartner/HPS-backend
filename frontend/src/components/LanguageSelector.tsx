'use client';

import { useState, useRef, useEffect } from 'react';
import { useLanguage, type Lang } from '@/lib/i18n';

const languages: { code: Lang; flag: string; label: string }[] = [
  { code: 'en', flag: '🇬🇧', label: 'English' },
  { code: 'pl', flag: '🇵🇱', label: 'Polski' },
  { code: 'uk', flag: '🇺🇦', label: 'Українська' },
  { code: 'de', flag: '🇩🇪', label: 'Deutsch' },
];

export default function LanguageSelector() {
  const { lang, setLang } = useLanguage();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const current = languages.find((l) => l.code === lang) || languages[0];

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div ref={ref} style={{ position: 'relative', display: 'inline-block' }}>
      <button
        onClick={() => setOpen((v) => !v)}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          padding: '6px 12px',
          border: '1px solid #d1d5db',
          borderRadius: '8px',
          background: 'white',
          cursor: 'pointer',
          fontSize: '14px',
          color: '#374151',
        }}
        aria-label="Select language"
      >
        <span>{current.flag}</span>
        <span>{current.code.toUpperCase()}</span>
        <span style={{ fontSize: '10px', marginLeft: '2px' }}>&#9662;</span>
      </button>

      {open && (
        <div
          style={{
            position: 'absolute',
            top: '100%',
            right: 0,
            marginTop: '4px',
            background: 'white',
            border: '1px solid #d1d5db',
            borderRadius: '8px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
            overflow: 'hidden',
            zIndex: 50,
            minWidth: '160px',
          }}
        >
          {languages.map((l) => (
            <button
              key={l.code}
              onClick={() => {
                setLang(l.code);
                setOpen(false);
              }}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                width: '100%',
                padding: '10px 14px',
                border: 'none',
                background: lang === l.code ? '#f3f4f6' : 'white',
                cursor: 'pointer',
                fontSize: '14px',
                color: '#374151',
                textAlign: 'left',
              }}
              onMouseEnter={(e) => {
                (e.target as HTMLButtonElement).style.background = '#f9fafb';
              }}
              onMouseLeave={(e) => {
                (e.target as HTMLButtonElement).style.background =
                  lang === l.code ? '#f3f4f6' : 'white';
              }}
            >
              <span>{l.flag}</span>
              <span>{l.label}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
