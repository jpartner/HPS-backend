'use client';

import { useState, useRef, useEffect } from 'react';
import { useCountry } from '@/lib/country-context';
import { useLanguage } from '@/lib/i18n';

const FLAG_MAP: Record<string, string> = {
  DE: '\u{1F1E9}\u{1F1EA}',
  PL: '\u{1F1F5}\u{1F1F1}',
  UA: '\u{1F1FA}\u{1F1E6}',
  SK: '\u{1F1F8}\u{1F1F0}',
  HU: '\u{1F1ED}\u{1F1FA}',
  HR: '\u{1F1ED}\u{1F1F7}',
  LV: '\u{1F1F1}\u{1F1FB}',
  LT: '\u{1F1F1}\u{1F1F9}',
  EE: '\u{1F1EA}\u{1F1EA}',
  CZ: '\u{1F1E8}\u{1F1FF}',
};

function getFlag(isoCode: string): string {
  return FLAG_MAP[isoCode.toUpperCase()] ?? '\u{1F3F3}\u{FE0F}';
}

export default function CountrySelector() {
  const { countries, selectedCountry, setCountry, isLoading } = useCountry();
  const { t } = useLanguage();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const buttonLabel = selectedCountry
    ? `${getFlag(selectedCountry.isoCode)} ${selectedCountry.isoCode}`
    : `\u{1F30D} ${t.providers.allCountries}`;

  return (
    <div ref={ref} style={{ position: 'relative', display: 'inline-block' }}>
      <button
        onClick={() => setOpen((v) => !v)}
        disabled={isLoading}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          padding: '6px 12px',
          border: '1px solid #d1d5db',
          borderRadius: '8px',
          background: 'white',
          cursor: isLoading ? 'wait' : 'pointer',
          fontSize: '14px',
          color: '#374151',
          opacity: isLoading ? 0.6 : 1,
        }}
        aria-label="Select country"
      >
        <span>{buttonLabel}</span>
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
            minWidth: '180px',
            maxHeight: '320px',
            overflowY: 'auto',
          }}
        >
          {/* All Countries option */}
          <button
            onClick={() => {
              setCountry('');
              setOpen(false);
            }}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              width: '100%',
              padding: '10px 14px',
              border: 'none',
              background: !selectedCountry ? '#f3f4f6' : 'white',
              cursor: 'pointer',
              fontSize: '14px',
              color: '#374151',
              textAlign: 'left',
            }}
            onMouseEnter={(e) => {
              (e.target as HTMLButtonElement).style.background = '#f9fafb';
            }}
            onMouseLeave={(e) => {
              (e.target as HTMLButtonElement).style.background = !selectedCountry
                ? '#f3f4f6'
                : 'white';
            }}
          >
            <span>{'\u{1F30D}'}</span>
            <span>{t.providers.allCountries}</span>
          </button>

          {countries.map((c) => {
            const isSelected = selectedCountry?.isoCode === c.isoCode;
            return (
              <button
                key={c.id}
                onClick={() => {
                  setCountry(c.isoCode);
                  setOpen(false);
                }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  width: '100%',
                  padding: '10px 14px',
                  border: 'none',
                  background: isSelected ? '#f3f4f6' : 'white',
                  cursor: 'pointer',
                  fontSize: '14px',
                  color: '#374151',
                  textAlign: 'left',
                }}
                onMouseEnter={(e) => {
                  (e.target as HTMLButtonElement).style.background = '#f9fafb';
                }}
                onMouseLeave={(e) => {
                  (e.target as HTMLButtonElement).style.background = isSelected
                    ? '#f3f4f6'
                    : 'white';
                }}
              >
                <span>{getFlag(c.isoCode)}</span>
                <span>{c.name}</span>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
