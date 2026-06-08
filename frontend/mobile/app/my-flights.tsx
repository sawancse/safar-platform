import { useState, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet, ActivityIndicator, RefreshControl,
} from 'react-native';
import { useRouter, useFocusEffect, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';
import { cabinLabel } from '@/lib/airports';

const STATUS_COLORS: Record<string, { bg: string; fg: string }> = {
  PENDING_PAYMENT: { bg: '#fef3c7', fg: '#b45309' },
  CONFIRMED:       { bg: '#dcfce7', fg: '#15803d' },
  TICKETED:        { bg: '#dbeafe', fg: '#1d4ed8' },
  COMPLETED:       { bg: '#f3f4f6', fg: '#374151' },
  CANCELLED:       { bg: '#fee2e2', fg: '#b91c1c' },
  REFUNDED:        { bg: '#f3e8ff', fg: '#7e22ce' },
};

export default function MyFlightsScreen() {
  const router = useRouter();
  const [flights, setFlights] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [needsAuth, setNeedsAuth] = useState(false);

  const load = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) { setNeedsAuth(true); setLoading(false); return; }
    setNeedsAuth(false);
    try {
      const res = await api.getMyFlights(token);
      setFlights(res?.content ?? (Array.isArray(res) ? res : []));
    } catch {
      setFlights([]);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { setLoading(true); load(); }, [load]));

  if (needsAuth) {
    return (
      <View style={styles.center}>
        <Stack.Screen options={{ title: 'My flights' }} />
        <Text style={styles.muted}>Sign in to see your flight bookings.</Text>
        <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/auth')}><Text style={styles.primaryText}>Sign in</Text></TouchableOpacity>
      </View>
    );
  }

  function renderItem({ item }: { item: any }) {
    const sc = STATUS_COLORS[item.status] ?? { bg: '#f3f4f6', fg: '#374151' };
    const bid = item.id ?? item.bookingId;
    return (
      <TouchableOpacity style={styles.card} activeOpacity={0.7} onPress={() => router.push(`/flight/${bid}`)}>
        <View style={styles.cardTop}>
          <Text style={styles.route}>{item.origin} → {item.destination}</Text>
          <View style={[styles.badge, { backgroundColor: sc.bg }]}><Text style={[styles.badgeText, { color: sc.fg }]}>{(item.status ?? '').replace(/_/g, ' ')}</Text></View>
        </View>
        <Text style={styles.sub}>{item.airline}{item.flightNumber ? ` · ${item.flightNumber}` : ''} · {item.departureDate}</Text>
        <View style={styles.cardFooter}>
          <Text style={styles.cabin}>{cabinLabel(item.cabinClass)}{item.bookingRef ? ` · ${item.bookingRef}` : ''}</Text>
          {item.totalAmountPaise ? <Text style={styles.amount}>{formatPaise(item.totalAmountPaise)}</Text> : null}
        </View>
      </TouchableOpacity>
    );
  }

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: 'My flights' }} />
      {loading ? (
        <ActivityIndicator color="#f97316" size="large" style={{ marginTop: 40 }} />
      ) : (
        <FlatList
          data={flights}
          keyExtractor={(item, i) => item.id ?? item.bookingId ?? String(i)}
          renderItem={renderItem}
          contentContainerStyle={flights.length === 0 ? styles.emptyBox : styles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} tintColor="#f97316" />}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Text style={styles.emptyIcon}>✈️</Text>
              <Text style={styles.emptyTitle}>No flights yet</Text>
              <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/flights')}><Text style={styles.primaryText}>Search flights</Text></TouchableOpacity>
            </View>
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container:   { flex: 1, backgroundColor: '#f9fafb' },
  center:      { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb', padding: 24 },
  muted:       { color: '#6b7280', fontSize: 15, marginBottom: 16 },
  list:        { padding: 16, gap: 12 },
  emptyBox:    { flexGrow: 1 },
  card:        { backgroundColor: '#fff', borderRadius: 14, padding: 14, borderWidth: 1, borderColor: '#f3f4f6' },
  cardTop:     { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  route:       { fontSize: 16, fontWeight: '700', color: '#111827' },
  badge:       { borderRadius: 100, paddingHorizontal: 10, paddingVertical: 3 },
  badgeText:   { fontSize: 11, fontWeight: '700' },
  sub:         { fontSize: 13, color: '#6b7280', marginTop: 6 },
  cardFooter:  { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 12 },
  cabin:       { fontSize: 12, color: '#6b7280' },
  amount:      { fontSize: 15, fontWeight: '800', color: '#111827' },
  empty:       { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 80 },
  emptyIcon:   { fontSize: 48, marginBottom: 12 },
  emptyTitle:  { fontSize: 18, fontWeight: '600', color: '#374151', marginBottom: 16 },
  primaryBtn:  { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 13, paddingHorizontal: 28, alignItems: 'center' },
  primaryText: { color: '#fff', fontSize: 15, fontWeight: '700' },
});
