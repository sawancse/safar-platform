import * as Notifications from 'expo-notifications';
import * as Device from 'expo-device';
import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';
import axios from 'axios';

const API_BASE = 'http://localhost:8080/api/v1';

// Configure notification behavior
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

/**
 * Register for push notifications and send token to server.
 */
export async function registerForPushNotifications(): Promise<string | null> {
  if (!Device.isDevice) {
    console.log('Push notifications require a physical device');
    return null;
  }

  // Check/request permissions
  const { status: existingStatus } = await Notifications.getPermissionsAsync();
  let finalStatus = existingStatus;

  if (existingStatus !== 'granted') {
    const { status } = await Notifications.requestPermissionsAsync();
    finalStatus = status;
  }

  if (finalStatus !== 'granted') {
    console.log('Push notification permission not granted');
    return null;
  }

  // Get Expo push token
  const tokenData = await Notifications.getExpoPushTokenAsync({
    projectId: process.env.EXPO_PUBLIC_PROJECT_ID,
  });
  const pushToken = tokenData.data;

  // Android notification channel
  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('default', {
      name: 'Default',
      importance: Notifications.AndroidImportance.MAX,
      vibrationPattern: [0, 250, 250, 250],
      lightColor: '#f97316',
    });

    await Notifications.setNotificationChannelAsync('bookings', {
      name: 'Booking Updates',
      importance: Notifications.AndroidImportance.HIGH,
      description: 'Booking confirmations, check-in reminders, and cancellations',
    });

    await Notifications.setNotificationChannelAsync('messages', {
      name: 'Messages',
      importance: Notifications.AndroidImportance.HIGH,
      description: 'Host-guest messages',
    });

    await Notifications.setNotificationChannelAsync('payments', {
      name: 'Payment Updates',
      importance: Notifications.AndroidImportance.DEFAULT,
      description: 'Payment confirmations and refunds',
    });
  }

  // Send token to server
  await sendTokenToServer(pushToken);

  return pushToken;
}

/**
 * Send push token to notification service.
 */
async function sendTokenToServer(pushToken: string): Promise<void> {
  try {
    const accessToken = await SecureStore.getItemAsync('access_token');
    if (!accessToken) return;

    await axios.post(
      `${API_BASE}/notifications/register-device`,
      {
        pushToken,
        platform: Platform.OS,
        deviceName: Device.modelName,
      },
      { headers: { Authorization: `Bearer ${accessToken}` } }
    );
  } catch (error) {
    console.error('Failed to register push token:', error);
  }
}

/**
 * Setup notification listeners for foreground and tap handling.
 */
export function setupNotificationHandlers(
  onNotificationReceived?: (notification: Notifications.Notification) => void,
  onNotificationTapped?: (response: Notifications.NotificationResponse) => void,
): () => void {
  // Foreground notification
  const foregroundSub = Notifications.addNotificationReceivedListener((notification) => {
    onNotificationReceived?.(notification);
  });

  // Notification tapped (background/killed)
  const tapSub = Notifications.addNotificationResponseReceivedListener((response) => {
    const data = response.notification.request.content.data;
    onNotificationTapped?.(response);

    // Handle navigation based on notification type
    const type = data?.type as string;
    switch (type) {
      case 'booking_update':
        // Navigate to booking details
        break;
      case 'message':
        // Navigate to messages
        break;
      case 'payment':
        // Navigate to payment status
        break;
      case 'review_request':
        // Navigate to review form
        break;
    }
  });

  // Return cleanup function
  return () => {
    foregroundSub.remove();
    tapSub.remove();
  };
}

/**
 * Get badge count.
 */
export async function getBadgeCount(): Promise<number> {
  return Notifications.getBadgeCountAsync();
}

/**
 * Clear badge count.
 */
export async function clearBadge(): Promise<void> {
  await Notifications.setBadgeCountAsync(0);
}
