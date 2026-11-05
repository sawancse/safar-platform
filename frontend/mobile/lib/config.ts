import Constants from 'expo-constants';

/**
 * Single source of truth for the API gateway URL.
 *
 * Resolution order:
 *   1. app.config.js `extra.apiUrl` (API_URL / EXPO_PUBLIC_API_URL env, else app.json)
 *   2. EXPO_PUBLIC_API_URL inlined at bundle time
 *   3. dev builds only: http://localhost:8080
 *   4. release builds: the live gateway
 *
 * Never fall back to localhost in a release build — on a phone, localhost is the
 * device itself, so every request would silently fail.
 */
const PRODUCTION_API_URL = 'https://api.bhramankaro.com';

function resolveApiUrl(): string {
  const configured =
    (Constants.expoConfig?.extra?.apiUrl as string | undefined) ||
    process.env.EXPO_PUBLIC_API_URL;
  const url = configured && configured.trim() ? configured.trim() : (__DEV__ ? 'http://localhost:8080' : PRODUCTION_API_URL);
  return url.replace(/\/+$/, '');
}

export const API_BASE_URL: string = resolveApiUrl();
export const API_V1_URL: string = `${API_BASE_URL}/api/v1`;
