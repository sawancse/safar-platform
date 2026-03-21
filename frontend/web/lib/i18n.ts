'use client';

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  ReactNode,
  createElement,
} from 'react';

/* ── Types ──────────────────────────────────────────────────────── */

export type Locale = 'en' | 'hi';

type Translations = Record<string, any>;

interface I18nContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: (key: string, params?: Record<string, string | number>) => string;
  ready: boolean;
}

/* ── Constants ──────────────────────────────────────────────────── */

const STORAGE_KEY = 'safar_locale';
const DEFAULT_LOCALE: Locale = 'en';
const SUPPORTED_LOCALES: Locale[] = ['en', 'hi'];

/* ── Context ────────────────────────────────────────────────────── */

const I18nContext = createContext<I18nContextValue>({
  locale: DEFAULT_LOCALE,
  setLocale: () => {},
  t: (key) => key,
  ready: false,
});

/* ── Helpers ────────────────────────────────────────────────────── */

/**
 * Resolve a dotted key like "nav.home" from a nested translations object.
 */
function resolveKey(translations: Translations, key: string): string {
  const parts = key.split('.');
  let current: any = translations;
  for (const part of parts) {
    if (current == null || typeof current !== 'object') return key;
    current = current[part];
  }
  return typeof current === 'string' ? current : key;
}

/**
 * Interpolate `{param}` placeholders in a translated string.
 */
function interpolate(
  template: string,
  params?: Record<string, string | number>
): string {
  if (!params) return template;
  return template.replace(/\{(\w+)\}/g, (_, name) =>
    params[name] != null ? String(params[name]) : `{${name}}`
  );
}

/**
 * Read the persisted locale from localStorage.
 */
function getStoredLocale(): Locale {
  if (typeof window === 'undefined') return DEFAULT_LOCALE;
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored && SUPPORTED_LOCALES.includes(stored as Locale)) {
    return stored as Locale;
  }
  return DEFAULT_LOCALE;
}

/* ── Translation cache ──────────────────────────────────────────── */

const translationCache: Partial<Record<Locale, Translations>> = {};

async function loadTranslations(locale: Locale): Promise<Translations> {
  if (translationCache[locale]) return translationCache[locale]!;
  const res = await fetch(`/locales/${locale}.json`);
  const data: Translations = await res.json();
  translationCache[locale] = data;
  return data;
}

/* ── Provider ───────────────────────────────────────────────────── */

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(DEFAULT_LOCALE);
  const [translations, setTranslations] = useState<Translations>({});
  const [ready, setReady] = useState(false);

  // Load initial locale from localStorage
  useEffect(() => {
    const stored = getStoredLocale();
    setLocaleState(stored);
    loadTranslations(stored).then((t) => {
      setTranslations(t);
      setReady(true);
    });
  }, []);

  const setLocale = useCallback((newLocale: Locale) => {
    if (!SUPPORTED_LOCALES.includes(newLocale)) return;
    localStorage.setItem(STORAGE_KEY, newLocale);
    setLocaleState(newLocale);
    loadTranslations(newLocale).then(setTranslations);

    // Update <html lang> attribute
    document.documentElement.lang = newLocale;

    // Optionally update backend if user is logged in
    const token = localStorage.getItem('access_token');
    if (token) {
      const apiUrl =
        process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
      fetch(`${apiUrl}/api/v1/users/me/language`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ language: newLocale }),
      }).catch(() => {
        // Silently ignore — language is persisted client-side regardless
      });
    }
  }, []);

  const t = useCallback(
    (key: string, params?: Record<string, string | number>): string => {
      const raw = resolveKey(translations, key);
      return interpolate(raw, params);
    },
    [translations]
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
