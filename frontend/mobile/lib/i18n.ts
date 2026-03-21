import { createContext, useContext, useState, useEffect, useCallback, ReactNode, createElement } from 'react';
import * as SecureStore from 'expo-secure-store';

import en from '@/locales/en.json';
import hi from '@/locales/hi.json';

/* ── Types ──────────────────────────────────────────────────────── */

export type Locale = 'en' | 'hi';

type Translations = Record<string, any>;

interface I18nContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => Promise<void>;
  t: (key: string, params?: Record<string, string | number>) => string;
  ready: boolean;
}

/* ── Constants ──────────────────────────────────────────────────── */

const STORAGE_KEY = 'safar_locale';
const DEFAULT_LOCALE: Locale = 'en';
const SUPPORTED_LOCALES: Locale[] = ['en', 'hi'];

const TRANSLATIONS: Record<Locale, Translations> = { en, hi };

/* ── Context ────────────────────────────────────────────────────── */

const I18nContext = createContext<I18nContextValue>({
  locale: DEFAULT_LOCALE,
  setLocale: async () => {},
  t: (key) => key,
  ready: false,
});

/* ── Helpers ────────────────────────────────────────────────────── */

function resolveKey(translations: Translations, key: string): string {
  const parts = key.split('.');
  let current: any = translations;
  for (const part of parts) {
    if (current == null || typeof current !== 'object') return key;
    current = current[part];
  }
  return typeof current === 'string' ? current : key;
}

function interpolate(
  template: string,
  params?: Record<string, string | number>
): string {
  if (!params) return template;
  return template.replace(/\{(\w+)\}/g, (_, name) =>
    params[name] != null ? String(params[name]) : `{${name}}`
  );
}

/* ── Provider ───────────────────────────────────────────────────── */

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(DEFAULT_LOCALE);
  const [ready, setReady] = useState(false);

  // Load persisted locale on mount
  useEffect(() => {
    (async () => {
      try {
        const stored = await SecureStore.getItemAsync(STORAGE_KEY);
        if (stored && SUPPORTED_LOCALES.includes(stored as Locale)) {
          setLocaleState(stored as Locale);
        }
      } catch {
        // Ignore read failures — will use default
      }
      setReady(true);
    })();
  }, []);

  const setLocale = useCallback(async (newLocale: Locale) => {
    if (!SUPPORTED_LOCALES.includes(newLocale)) return;
    setLocaleState(newLocale);
    try {
      await SecureStore.setItemAsync(STORAGE_KEY, newLocale);
    } catch {
      // Ignore persistence failures
    }

    // Optionally notify backend about language preference
    try {
      const token = await SecureStore.getItemAsync('safar_access_token');
      if (token) {
        // Fire-and-forget
        fetch('http://localhost:8080/api/v1/users/me/language', {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ language: newLocale }),
        }).catch(() => {});
      }
    } catch {
      // Silently ignore
    }
  }, []);

  const t = useCallback(
    (key: string, params?: Record<string, string | number>): string => {
      const translations = TRANSLATIONS[locale] ?? TRANSLATIONS[DEFAULT_LOCALE];
      const raw = resolveKey(translations, key);
      return interpolate(raw, params);
    },
    [locale]
  );

  return createElement(
    I18nContext.Provider,
    { value: { locale, setLocale, t, ready } },
    children
  );
}

/* ── Hook ───────────────────────────────────────────────────────── */

export function useTranslation() {
  const ctx = useContext(I18nContext);
  if (!ctx) {
    throw new Error('useTranslation must be used within an I18nProvider');
  }
  return ctx;
}
