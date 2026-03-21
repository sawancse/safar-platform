import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  RefreshControl,
} from 'react-native';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

interface Notification {
  id: string;
  type: string;
  title: string;
  body: string;
  read: boolean;
  referenceType?: string;
  referenceId?: string;
  createdAt: string;
}

const TYPE_ICONS: Record<string, { name: keyof typeof Ionicons.glyphMap; color: string; bg: string }> = {
  BOOKING_CONFIRMED:  { name: 'checkmark-circle',  color: '#16a34a', bg: '#dcfce7' },
  BOOKING_CANCELLED:  { name: 'close-circle',      color: '#dc2626', bg: '#fef2f2' },
  BOOKING_CREATED:    { name: 'calendar',           color: '#2563eb', bg: '#dbeafe' },
  PAYMENT_RECEIVED:   { name: 'card',               color: '#16a34a', bg: '#dcfce7' },
  PAYMENT_FAILED:     { name: 'card',               color: '#dc2626', bg: '#fef2f2' },
  REVIEW_RECEIVED:    { name: 'star',               color: '#eab308', bg: '#fef9c3' },
  MESSAGE:            { name: 'chatbubble',         color: '#8b5cf6', bg: '#ede9fe' },
  LISTING_APPROVED:   { name: 'home',               color: '#16a34a', bg: '#dcfce7' },
  LISTING_REJECTED:   { name: 'home',               color: '#dc2626', bg: '#fef2f2' },
  SYSTEM:             { name: 'information-circle', color: '#6b7280', bg: '#f3f4f6' },
};

function getTypeIcon(type: string) {
  return TYPE_ICONS[type] ?? TYPE_ICONS.SYSTEM;
}

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'Just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  if (days < 7) return `${days}d ago`;
  const weeks = Math.floor(days / 7);
  if (weeks < 4) return `${weeks}w ago`;
  return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

export default function NotificationsScreen() {
  const router = useRouter();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loadingMore, setLoadingMore] = useState(false);
  const [markingAll, setMarkingAll] = useState(false);

  const hasUnread = notifications.some((n) => !n.read);
  const hasMore = notifications.length < totalElements;

  const loadNotifications = useCallback(
    async (pageNum: number, append = false) => {
      const token = await getAccessToken();
      if (!token) {
        setLoading(false);
        return;
      }
      try {
        const res = await api.getNotifications(token, pageNum, 20);
        const items: Notification[] = res.content ?? [];
        setTotalElements(res.totalElements ?? 0);
        if (append) {
          setNotifications((prev) => [...prev, ...items]);
        } else {
          setNotifications(items);
        }
        setPage(pageNum);
      } catch {
        if (!append) setNotifications([]);
      } finally {
        setLoading(false);
        setRefreshing(false);
        setLoadingMore(false);
      }
    },
    []
  );

  useEffect(() => {
    loadNotifications(0);
  }, [loadNotifications]);

  function handleRefresh() {
    setRefreshing(true);
    loadNotifications(0);
  }

  function handleLoadMore() {
    if (loadingMore || !hasMore) return;
    setLoadingMore(true);
    loadNotifications(page + 1, true);
  }

  async function handleMarkAllRead() {
    const token = await getAccessToken();
    if (!token || markingAll) return;
    setMarkingAll(true);
    try {
      await api.markAllNotificationsRead(token);
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    } catch {}
    finally {
      setMarkingAll(false);
    }
  }

  async function handlePress(item: Notification) {
    // Mark as read
    if (!item.read) {
      const token = await getAccessToken();
      if (token) {
        api.markNotificationRead(item.id, token).catch(() => {});
        setNotifications((prev) =>
          prev.map((n) => (n.id === item.id ? { ...n, read: true } : n))
        );
      }
    }

    // Navigate based on reference type
    const ref = item.referenceType?.toUpperCase();
    if (ref === 'BOOKING' && item.referenceId) {
      router.push('/trips');
    } else if (ref === 'REVIEW') {
      router.push('/review');
    } else if (ref === 'PAYMENT') {
      router.push('/trips');
    } else if (ref === 'MESSAGE' || ref === 'CONVERSATION') {
      router.push('/messages');
    } else if (ref === 'LISTING' && item.referenceId) {
      router.push(`/listing/${item.referenceId}` as any);
    }
  }

  function renderItem({ item }: { item: Notification }) {
    const icon = getTypeIcon(item.type);

    return (
      <TouchableOpacity
        style={[styles.notifRow, !item.read && styles.notifUnread]}
        onPress={() => handlePress(item)}
        activeOpacity={0.7}
      >
        {!item.read && <View style={styles.unreadDot} />}
        <View style={[styles.iconBox, { backgroundColor: icon.bg }]}>
          <Ionicons name={icon.name} size={20} color={icon.color} />
        </View>
        <View style={styles.notifContent}>
          <Text style={[styles.notifTitle, !item.read && styles.notifTitleUnread]} numberOfLines={1}>
            {item.title}
          </Text>
          <Text style={styles.notifBody} numberOfLines={2}>
            {item.body}
          </Text>
          <Text style={styles.notifTime}>{timeAgo(item.createdAt)}</Text>
        </View>
      </TouchableOpacity>
    );
  }

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#f97316" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Notifications</Text>
        {hasUnread && (
          <TouchableOpacity onPress={handleMarkAllRead} disabled={markingAll}>
            {markingAll ? (
              <ActivityIndicator size="small" color="#f97316" />
            ) : (
              <Text style={styles.markAllText}>Mark all read</Text>
            )}
          </TouchableOpacity>
        )}
      </View>

      <FlatList
        data={notifications}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={handleRefresh} tintColor="#f97316" />
        }
        onEndReached={handleLoadMore}
        onEndReachedThreshold={0.3}
        ListFooterComponent={
          loadingMore ? (
            <View style={styles.footerLoader}>
              <ActivityIndicator size="small" color="#f97316" />
            </View>
          ) : null
        }
        ListEmptyComponent={
          <View style={styles.emptyCenter}>
            <Ionicons name="notifications-off-outline" size={56} color="#d1d5db" />
            <Text style={styles.emptyTitle}>No notifications yet</Text>
            <Text style={styles.emptySubtitle}>
              We'll let you know when something important happens.
            </Text>
          </View>
        }
        contentContainerStyle={notifications.length === 0 ? styles.emptyList : undefined}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f9fafb' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },

  // Header
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: 56,
    paddingBottom: 12,
    paddingHorizontal: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  headerTitle: { fontSize: 22, fontWeight: '700', color: '#111827' },
  markAllText: { fontSize: 13, fontWeight: '600', color: '#f97316' },

  // Notification row
  notifRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    paddingVertical: 14,
    paddingHorizontal: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  notifUnread: {
    backgroundColor: '#fffbeb',
  },
  unreadDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#f97316',
    position: 'absolute',
    left: 6,
    top: 20,
  },
  iconBox: {
    width: 40,
    height: 40,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  notifContent: { flex: 1 },
  notifTitle: { fontSize: 14, fontWeight: '500', color: '#374151', marginBottom: 2 },
  notifTitleUnread: { fontWeight: '700', color: '#111827' },
  notifBody: { fontSize: 13, color: '#6b7280', lineHeight: 18, marginBottom: 4 },
  notifTime: { fontSize: 11, color: '#9ca3af' },

  // Empty state
  emptyList: { flex: 1 },
  emptyCenter: { alignItems: 'center', justifyContent: 'center', paddingTop: 120 },
  emptyTitle: { fontSize: 18, fontWeight: '600', color: '#374151', marginTop: 16, marginBottom: 6 },
  emptySubtitle: { fontSize: 13, color: '#9ca3af', textAlign: 'center', paddingHorizontal: 40 },

  // Footer
  footerLoader: { paddingVertical: 16, alignItems: 'center' },
});
