'use client';

import { useTranslation, type Locale } from '@/lib/i18n';

const LOCALES: { value: Locale; label: string }[] = [
  { value: 'en', label: 'English' },
  { value: 'hi', label: 'हिन्दी' },
];

export default function LanguageSelector() {
  const { locale, setLocale } = useTranslation();

  return (
    <div className="flex items-center gap-1 rounded-full border border-gray-200 p-0.5">
      {LOCALES.map(({ value, label }) => (
        <button
          key={value}
          onClick={() => setLocale(value)}
          className={`px-3 py-1 rounded-full text-xs font-medium transition ${
            locale === value
              ? 'bg-orange-500 text-white'
              : 'text-gray-600 hover:text-orange-500'
          }`}
          aria-label={`Switch language to ${label}`}
        >
          {label}
        </button>
      ))}
    </div>
  );
}
