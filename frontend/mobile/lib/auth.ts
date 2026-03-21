import * as SecureStore from 'expo-secure-store';

const KEYS = {
  ACCESS_TOKEN:  'safar_access_token',
  REFRESH_TOKEN: 'safar_refresh_token',
  USER_ID:       'safar_user_id',
  USER_ROLE:     'safar_user_role',
};

export async function saveTokens(tokens: {
  accessToken: string;
  refreshToken: string;
  userId: string;
  role: string;
}) {
  await Promise.all([
    SecureStore.setItemAsync(KEYS.ACCESS_TOKEN,  tokens.accessToken),
    SecureStore.setItemAsync(KEYS.REFRESH_TOKEN, tokens.refreshToken),
    SecureStore.setItemAsync(KEYS.USER_ID,       tokens.userId),
    SecureStore.setItemAsync(KEYS.USER_ROLE,     tokens.role),
  ]);
}

export async function getAccessToken(): Promise<string | null> {
  return SecureStore.getItemAsync(KEYS.ACCESS_TOKEN);
}

export async function getUserRole(): Promise<string | null> {
  return SecureStore.getItemAsync(KEYS.USER_ROLE);
}

export async function clearTokens() {
  await Promise.all(Object.values(KEYS).map((k) => SecureStore.deleteItemAsync(k)));
}
