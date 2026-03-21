import React, { useEffect, useState } from 'react';
import { View, TouchableOpacity, StyleSheet, Text } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { getBadgeCount } from '../lib/notifications';

interface NotificationBellProps {
  color?: string;
  size?: number;
}

export default function NotificationBell({ color = '#1e293b', size = 24 }: NotificationBellProps) {
  const router = useRouter();
  const [badgeCount, setBadgeCount] = useState(0);

  useEffect(() => {
    const checkBadge = async () => {
      const count = await getBadgeCount();
      setBadgeCount(count);
    };
    checkBadge();
    const interval = setInterval(checkBadge, 30000); // Check every 30 seconds
    return () => clearInterval(interval);
  }, []);

  return (
    <TouchableOpacity
      style={styles.container}
      onPress={() => router.push('/notifications' as any)}
    >
      <Ionicons name="notifications-outline" size={size} color={color} />
      {badgeCount > 0 && (
        <View style={styles.badge}>
          <Text style={styles.badgeText}>
            {badgeCount > 99 ? '99+' : badgeCount}
          </Text>
        </View>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 4,
    position: 'relative',
  },
  badge: {
    position: 'absolute',
    top: 0,
    right: 0,
    backgroundColor: '#ef4444',
    borderRadius: 10,
    minWidth: 18,
    height: 18,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 4,
  },
  badgeText: {
    color: '#fff',
    fontSize: 10,
    fontWeight: '700',
  },
});
