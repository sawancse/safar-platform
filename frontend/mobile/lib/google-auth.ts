import * as AuthSession from 'expo-auth-session';
import * as SecureStore from 'expo-secure-store';
import axios from 'axios';

const API_BASE = 'http://localhost:8080/api/v1';

// Google OAuth configuration
const GOOGLE_CLIENT_ID = process.env.EXPO_PUBLIC_GOOGLE_CLIENT_ID || '';
const REDIRECT_URI = AuthSession.makeRedirectUri({ scheme: 'safar' });

const discovery = {
  authorizationEndpoint: 'https://accounts.google.com/o/oauth2/v2/auth',
  tokenEndpoint: 'https://oauth2.googleapis.com/token',
  revocationEndpoint: 'https://oauth2.googleapis.com/revoke',
};

/**
 * Initiate Google Sign-In flow using expo-auth-session.
 * Returns JWT tokens from Safar backend.
 */
export async function signInWithGoogle(): Promise<{
  success: boolean;
  accessToken?: string;
  refreshToken?: string;
  isNewUser?: boolean;
  error?: string;
}> {
  try {
    const request = new AuthSession.AuthRequest({
      clientId: GOOGLE_CLIENT_ID,
      redirectUri: REDIRECT_URI,
      scopes: ['openid', 'profile', 'email'],
      responseType: AuthSession.ResponseType.Code,
    });

    const result = await request.promptAsync(discovery);

    if (result.type === 'success' && result.params.code) {
      // Exchange code for Google ID token
      const tokenResponse = await AuthSession.exchangeCodeAsync(
        {
          clientId: GOOGLE_CLIENT_ID,
          code: result.params.code,
          redirectUri: REDIRECT_URI,
        },
        discovery
      );

      // Send Google ID token to Safar backend
      const response = await axios.post(`${API_BASE}/auth/google-signin`, {
        idToken: tokenResponse.idToken,
      });

      const { accessToken, refreshToken, isNewUser } = response.data;

      // Store tokens securely
      await SecureStore.setItemAsync('access_token', accessToken);
      await SecureStore.setItemAsync('refresh_token', refreshToken);

      return { success: true, accessToken, refreshToken, isNewUser };
    }

    if (result.type === 'cancel') {
      return { success: false, error: 'Sign-in cancelled' };
    }

    return { success: false, error: 'Sign-in failed' };
  } catch (error: any) {
    console.error('Google Sign-In error:', error);
    return { success: false, error: error.message || 'Google Sign-In failed' };
  }
}

/**
 * Check if Google Sign-In is available (client ID configured).
 */
export function isGoogleSignInAvailable(): boolean {
  return GOOGLE_CLIENT_ID.length > 0;
}
