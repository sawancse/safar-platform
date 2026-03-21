import { useEffect } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import * as SplashScreen from 'expo-splash-screen';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { I18nProvider } from '@/lib/i18n';

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  useEffect(() => {
    SplashScreen.hideAsync();
  }, []);

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <I18nProvider>
          <StatusBar style="auto" />
          <Stack
            screenOptions={{
              headerStyle: { backgroundColor: '#fff' },
              headerTintColor: '#f97316',
              headerTitleStyle: { fontWeight: 'bold' },
            }}
          >
            <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
            <Stack.Screen name="auth" options={{ title: 'Sign in', presentation: 'modal' }} />
            <Stack.Screen name="listing/[id]" options={{ title: 'Listing details' }} />
            <Stack.Screen name="book/[listingId]" options={{ title: 'Confirm booking' }} />
            <Stack.Screen name="miles" options={{ title: 'Property Miles' }} />
            <Stack.Screen name="experiences" options={{ title: 'Experiences' }} />
            <Stack.Screen name="co-travelers" options={{ title: 'Co-travelers' }} />
            <Stack.Screen name="subscription" options={{ title: 'Host Subscription' }} />
            <Stack.Screen name="medical" options={{ title: 'Medical Tourism' }} />
            <Stack.Screen name="nomad" options={{ title: 'Nomad Network' }} />
            <Stack.Screen name="payment-methods" options={{ title: 'Payment Methods' }} />
            <Stack.Screen name="saved" options={{ title: 'Saved Stays' }} />
            <Stack.Screen name="notifications" options={{ title: 'Notifications', headerShown: false }} />
            <Stack.Screen name="host" options={{ title: 'Host Dashboard' }} />
            <Stack.Screen name="profile-edit" options={{ title: 'Edit Profile' }} />
          </Stack>
        </I18nProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
