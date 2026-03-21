'use server';

import { cookies } from 'next/headers';

export async function getAccessToken(): Promise<string | null> {
  const store = cookies();
  return store.get('access_token')?.value ?? null;
}

export function setAuthCookies(accessToken: string, refreshToken: string) {
  const store = cookies();
  store.set('access_token', accessToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    maxAge: 60 * 15, // 15 min
    path: '/',
  });
  store.set('refresh_token', refreshToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    maxAge: 60 * 60 * 24 * 30, // 30 days
    path: '/',
  });
}

export function clearAuthCookies() {
  const store = cookies();
  store.delete('access_token');
  store.delete('refresh_token');
}
