import { useState, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet, ActivityIndicator, RefreshControl,
} from 'react-native';
import { useRouter, useFocusEffect, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';

type Tab = 'bookings' | 'events' | 'subscriptions';

const STATUS_COLORS: Record<string, { bg: string; fg: string }> = {
  PENDING_PAYMENT: { bg: '#fef3c7', fg: '#b45309' },
  PENDING:         { bg: '#fef3c7', fg: '#b45309' },
  INQUIRY:         { bg: '#e0e7ff', fg: '#4338ca' },
  QUOTED:          { bg: '#dbeafe', fg: '#1d4ed8' },
  CONFIRMED:       { bg: '#dcfce7', fg: '#15803d' },
  ADVANCE_PAID:    { bg: '#dcfce7', fg: '#15803d' },
  IN_PROGRESS:     { bg: '#cffafe', fg: '#0e7490' },
  COMPLETED:       { bg: '#f3f4f6', fg: '#374151' },
  CANCELLED:       { bg: '#fee2e2', fg: '#b91c1c' },
};

function pretty(s?: string) {
  if (!s) return '';
  return s.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
}

function StatusBadge({ status }: { status?: string }) {
  const c = STATUS_COLORS[status ?? ''] ?? { bg: '#f3f4f6', fg: '#374151' };
  return <View style={[styles.badge, { backgroundColor: c.bg }]}><Text style={[styles.badgeText, { color: c.fg }]}>{pretty(status)}</Text></View>;
}

export default function CookBookingsScreen() {
  const router = useRouter();
  const [tab, setTab] = useState<Tab>('bookings');
  const [bookings, setBookings] = useState<any[]>([]);
  const [events, setEvents] = useState<any[]>([]);
  const [subs, setSubs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [needsAuth, setNeedsAuth] = useState(false);

  const load = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) { setNeedsAuth(true); setLoading(false); return; }
    setNeedsAuth(false);
    try {
      const [b, e, s] = await Promise.all([
        api.getMyChefBookings(token).catch(() => []),
        api.getMyEventBookings(token).catch(() => []),
        api.getMyChefSubscriptions(token).catch(() => []),
      ]);
      setBookings(b ?? []);
      setEvents(e ?? []);
      setSubs(s ?? []);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { setLoading(true); load(); }, [load]));

  if (needsAuth) {
    return (
      <View style={styles.center}>
        <Stack.Screen options={{ title: 'My cook bookings' }} />
        <Text style={styles.muted}>Sign in to see your cook bookings.</Text>
        <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/auth')}><Text style={styles.primaryBtnText}>Sign in</Text></TouchableOpacity>
      </View>
    );
  }

  const data = tab === 'bookings' ? bookings : tab === 'events' ? events : subs;

  function renderSubscription({ item }: { item: any }) {
    return (
      <View style={styles.card}>
        <View style={styles.cardTop}>
          <View style={{ flex: 1 }}>
            <Text style={styles.cardTitle}>{item.chefName || 'Cook subscription'}</Text>
            {item.bookingRef || item.subscriptionRef ? <Text style={styles.cardRef}>{item.bookingRef || item.subscriptionRef}</Text> : null}
          </View>
          <StatusBadge status={item.status} />
        </View>
        {item.plan ? <Text style={styles.cardLine}>📦 {item.plan}{item.mealsPerDay ? ` · ${item.mealsPerDay} meal(s)/day` : ''}</Text> : null}
        {item.schedule ? <Text style={styles.cardLine}>🗓️ {item.schedule}{item.startDate ? ` · from ${item.startDate}` : ''}</Text> : null}
        {item.mealType ? <Text style={styles.cardLine}>🍽️ {pretty(item.mealType)}</Text> : null}
        {item.address ? <Text style={styles.cardLine} numberOfLines={1}>📍 {item.address}</Text> : null}
        {item.monthlyRatePaise ? <Text style={[styles.cardAmount, { marginTop: 12 }]}>{formatPaise(item.monthlyRatePaise)}/mo</Text> : null}
      </View>
    );
  }

  function renderBooking({ item }: { item: any }) {
    const isEvent = tab === 'events';
    const id = item.id;
    const date = isEvent ? item.eventDate : item.serviceDate;
    const time = isEvent ? item.eventTime : item.serviceTime;
    const balanceDue = (item.balanceAmountPaise ?? 0) > 0 && item.paymentStatus !== 'FULLY_PAID';
    return (
      <TouchableOpacity activeOpacity={0.7} style={styles.card} onPress={() => router.push(`/cook-booking/${id}?kind=${isEvent ? 'event' : 'chef'}`)}>
        <View style={styles.cardTop}>
          <View style={{ flex: 1 }}>
            <Text style={styles.cardTitle}>
              {isEvent ? `${pretty(item.eventType) || 'Event'} — ${item.chefName || item.vendorBusinessName || 'Cook'}` : (item.chefName || 'Cook booking')}
            </Text>
            {item.bookingRef ? <Text style={styles.cardRef}>{item.bookingRef}</Text> : null}
          </View>
          <StatusBadge status={item.status} />
        </View>
        <Text style={styles.cardLine}>📅 {date}{time ? ` · ${time}` : ''}</Text>
        {!isEvent && item.mealType ? <Text style={styles.cardLine}>🍽️ {pretty(item.mealType)} · {item.guestsCount} pax</Text> : null}
        {isEvent && item.guestCount ? <Text style={styles.cardLine}>👥 {item.guestCount} guests</Text> : null}
        {(item.address || item.venueAddress) ? <Text style={styles.cardLine} numberOfLines={1}>📍 {item.address || item.venueAddress}</Text> : null}

        <View style={styles.cardFooter}>
          {item.totalAmountPaise ? <Text style={styles.cardAmount}>{formatPaise(item.totalAmountPaise)}</Text> : <View />}
          {item.status === 'PENDING_PAYMENT' ? (
            <View style={styles.payTag}><Text style={styles.payTagText}>Payment pending</Text></View>
          ) : balanceDue ? (
            <View style={styles.balTag}><Text style={styles.balTagText}>Balance {formatPaise(item.balanceAmountPaise)}</Text></View>
          ) : null}
        </View>

        {item.startJobOtp && !item.jobStartedAt ? (
          <View style={styles.otpRow}><Text style={styles.otpLabel}>Start OTP</Text><Text style={styles.otpVal}>{item.startJobOtp}</Text></View>
        ) : null}
      </TouchableOpacity>
    );
  }

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: 'My cook bookings' }} />
      <View style={styles.tabBar}>
        {(['bookings', 'events', 'subscriptions'] as Tab[]).map((t) => (
          <TouchableOpacity key={t} style={[styles.tab, tab === t && styles.tabActive]} onPress={() => setTab(t)}>
            <Text style={[styles.tabText, tab === t && styles.tabTextActive]} numberOfLines={1}>
              {t === 'bookings' ? `Bookings (${bookings.length})` : t === 'events' ? `Events (${events.length})` : `Plans (${subs.length})`}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {loading ? (
        <ActivityIndicator color="#f97316" size="large" style={{ marginTop: 40 }} />
      ) : (
        <FlatList
          data={data}
          keyExtractor={(item) => item.id}
          renderItem={tab === 'subscriptions' ? renderSubscription : renderBooking}
          contentContainerStyle={data.length === 0 ? styles.emptyBox : styles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} tintColor="#f97316" />}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Text style={styles.emptyIcon}>🍽️</Text>
              <Text style={styles.emptyTitle}>No {tab} yet</Text>
              <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/cooks')}><Text style={styles.primaryBtnText}>Browse cooks</Text></TouchableOpacity>
            </View>
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container:     { flex: 1, backgroundColor: '#f9fafb' },
  center:        { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb', padding: 24 },
  muted:         { color: '#6b7280', fontSize: 15, marginBottom: 16 },

  tabBar:        { flexDirection: 'row', backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  tab:           { flex: 1, paddingVertical: 14, alignItems: 'center', borderBottomWidth: 2, borderBottomColor: 'transparent' },
  tabActive:     { borderBottomColor: '#f97316' },
  tabText:       { fontSize: 14, fontWeight: '600', color: '#6b7280' },
  tabTextActive: { color: '#f97316' },

  list:          { padding: 16, gap: 12 },
  emptyBox:      { flexGrow: 1 },

  card:          { backgroundColor: '#fff', borderRadius: 14, padding: 14, borderWidth: 1, borderColor: '#f3f4f6' },
  cardTop:       { flexDirection: 'row', alignItems: 'flex-start', gap: 8 },
  cardTitle:     { fontSize: 15, fontWeight: '700', color: '#111827' },
  cardRef:       { fontSize: 11, color: '#9ca3af', marginTop: 2 },
  cardLine:      { fontSize: 13, color: '#4b5563', marginTop: 6 },
  cardFooter:    { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 12 },
  cardAmount:    { fontSize: 16, fontWeight: '800', color: '#111827' },
  payTag:        { backgroundColor: '#fef3c7', borderRadius: 8, paddingHorizontal: 10, paddingVertical: 5 },
  payTagText:    { fontSize: 12, fontWeight: '700', color: '#b45309' },
  balTag:        { backgroundColor: '#fff7ed', borderRadius: 8, paddingHorizontal: 10, paddingVertical: 5 },
  balTagText:    { fontSize: 12, fontWeight: '700', color: '#c2410c' },

  badge:         { borderRadius: 100, paddingHorizontal: 10, paddingVertical: 3 },
  badgeText:     { fontSize: 11, fontWeight: '700' },

  otpRow:        { flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 12, backgroundColor: '#f0fdf4', borderRadius: 8, padding: 10 },
  otpLabel:      { fontSize: 12, color: '#15803d', fontWeight: '600' },
  otpVal:        { fontSize: 18, fontWeight: '800', color: '#15803d', letterSpacing: 3, fontVariant: ['tabular-nums'] },

  empty:         { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 80 },
  emptyIcon:     { fontSize: 48, marginBottom: 12 },
  emptyTitle:    { fontSize: 18, fontWeight: '600', color: '#374151', marginBottom: 16 },

  primaryBtn:    { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 13, paddingHorizontal: 28, alignItems: 'center' },
  primaryBtnText:{ color: '#fff', fontSize: 15, fontWeight: '700' },
});
