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
            <Stack.Screen name="aashray" options={{ title: 'Aashray' }} />
            <Stack.Screen name="medical" options={{ title: 'Medical Tourism' }} />
            <Stack.Screen name="nomad" options={{ title: 'Nomad Network' }} />
            <Stack.Screen name="payment-methods" options={{ title: 'Payment Methods' }} />
            <Stack.Screen name="saved" options={{ title: 'Saved Stays' }} />
            <Stack.Screen name="notifications" options={{ title: 'Notifications', headerShown: false }} />
            <Stack.Screen name="host" options={{ title: 'Host Dashboard' }} />
            <Stack.Screen name="host-calendar" options={{ title: 'Calendar', headerShown: false }} />
            <Stack.Screen name="host-earnings" options={{ title: 'Earnings', headerShown: false }} />
            <Stack.Screen name="host-kyc" options={{ title: 'KYC Verification', headerShown: false }} />
            <Stack.Screen name="host-reviews" options={{ title: 'Reviews', headerShown: false }} />
            <Stack.Screen name="host-rooms" options={{ title: 'Room Types', headerShown: false }} />
            <Stack.Screen name="host-analytics" options={{ title: 'Analytics', headerShown: false }} />
            <Stack.Screen name="host-transactions" options={{ title: 'Transactions', headerShown: false }} />
            <Stack.Screen name="host-pricing" options={{ title: 'Pricing Rules', headerShown: false }} />
            <Stack.Screen name="host-packages" options={{ title: 'PG Packages', headerShown: false }} />
            <Stack.Screen name="host-channels" options={{ title: 'Channel Manager', headerShown: false }} />
            <Stack.Screen name="host-tenants" options={{ title: 'Tenants', headerShown: false }} />
            <Stack.Screen name="host-new-listing" options={{ title: 'New Listing', headerShown: false }} />
            <Stack.Screen name="host-messages" options={{ title: 'Guest Messages', headerShown: false }} />
            <Stack.Screen name="looking-for" options={{ title: 'Looking For', headerShown: false }} />
            <Stack.Screen name="dashboard" options={{ title: 'Dashboard', headerShown: false }} />
            <Stack.Screen name="my-reviews" options={{ title: 'My Reviews', headerShown: false }} />
            <Stack.Screen name="messages" options={{ title: 'Messages' }} />
            <Stack.Screen name="review" options={{ title: 'Write Review' }} />
            <Stack.Screen name="profile-edit" options={{ title: 'Edit Profile' }} />
            <Stack.Screen name="buy" options={{ title: 'Buy Property', headerShown: false }} />
            <Stack.Screen name="buy-search" options={{ title: 'Search Properties', headerShown: false }} />
            <Stack.Screen name="buy-property/[id]" options={{ title: 'Property Details', headerShown: false }} />
            <Stack.Screen name="sell" options={{ title: 'Sell Property', headerShown: false }} />
            <Stack.Screen name="seller-dashboard" options={{ title: 'Seller Dashboard', headerShown: false }} />
            <Stack.Screen name="site-visits" options={{ title: 'Site Visits', headerShown: false }} />
            <Stack.Screen name="projects" options={{ title: 'New Projects', headerShown: false }} />
            <Stack.Screen name="project-detail/[id]" options={{ title: 'Project Details', headerShown: false }} />
            <Stack.Screen name="builder-dashboard" options={{ title: 'Builder Dashboard', headerShown: false }} />
          </Stack>
        </I18nProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
