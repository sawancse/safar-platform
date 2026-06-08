import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, StyleSheet, ActivityIndicator, Alert,
} from 'react-native';
import { useLocalSearchParams, useRouter, Stack } from 'expo-router';
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

function parsePax(b: any): any[] {
  if (Array.isArray(b?.passengers)) return b.passengers;
  if (b?.passengersJson) { try { return JSON.parse(b.passengersJson); } catch { return []; } }
  return [];
}

export default function FlightBookingDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const [booking, setBooking] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);

  const load = useCallback(async () => {
    if (!id) return;
    const token = await getAccessToken();
    if (!token) { router.replace('/auth'); return; }
    try {
      setBooking(await api.getFlightBooking(id, token));
    } catch {
      setBooking(null);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  function confirmCancel() {
    Alert.alert('Cancel booking?', 'This may be subject to airline cancellation charges.', [
      { text: 'Keep booking', style: 'cancel' },
      { text: 'Cancel flight', style: 'destructive', onPress: doCancel },
    ]);
  }

  async function doCancel() {
    const token = await getAccessToken();
    if (!token || !id) return;
    setCancelling(true);
    try {
      await api.cancelFlightBooking(id, token);
      Alert.alert('Cancelled', 'Your flight booking was cancelled.');
      load();
    } catch (e: any) {
      Alert.alert('Could not cancel', e.message ?? 'Try again');
    } finally {
      setCancelling(false);
    }
  }

  if (loading) return <View style={styles.center}><ActivityIndicator color="#f97316" size="large" /></View>;
  if (!booking) return <View style={styles.center}><Text style={styles.muted}>Booking not found</Text></View>;

  const status = booking.status ?? 'CONFIRMED';
  const sc = STATUS_COLORS[status] ?? { bg: '#f3f4f6', fg: '#374151' };
  const pax = parsePax(booking);
  const paid = !!booking.paidAt || ['CONFIRMED', 'TICKETED', 'COMPLETED'].includes(status);
  const confirmed = ['CONFIRMED', 'TICKETED', 'COMPLETED'].includes(status);
  const timeline = [
    { label: 'Booking created', done: true },
    { label: 'Payment received', done: paid },
    { label: 'Confirmed', done: confirmed },
    { label: 'Check-in open', done: ['TICKETED', 'COMPLETED'].includes(status) },
    { label: 'Departed', done: status === 'COMPLETED' },
  ];

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: 'Flight booking' }} />
      <ScrollView contentContainerStyle={{ paddingBottom: 40 }}>
        {/* Header */}
        <View style={styles.header}>
          <View style={styles.headerTop}>
            <Text style={styles.ref}>{booking.bookingRef ?? booking.bookingId ?? id}</Text>
            <View style={[styles.badge, { backgroundColor: sc.bg }]}><Text style={[styles.badgeText, { color: sc.fg }]}>{status.replace(/_/g, ' ')}</Text></View>
          </View>
          <Text style={styles.route}>{booking.origin} → {booking.destination}</Text>
          <Text style={styles.sub}>{booking.airline}{booking.flightNumber ? ` · ${booking.flightNumber}` : ''}</Text>
          <Text style={styles.sub}>{booking.departureDate}{booking.returnDate ? ` · returns ${booking.returnDate}` : ''} · {cabinLabel(booking.cabinClass)}</Text>
        </View>

        {/* Timeline */}
        <Section title="Status">
          {timeline.map((t, i) => (
            <View key={i} style={styles.tlRow}>
              <Text style={[styles.tlDot, t.done && styles.tlDotDone]}>{t.done ? '●' : '○'}</Text>
              <Text style={[styles.tlLabel, t.done && styles.tlLabelDone]}>{t.label}</Text>
            </View>
          ))}
        </Section>

        {/* Passengers */}
        <Section title={`Passengers (${pax.length})`}>
          {pax.map((p, i) => (
            <View key={i} style={styles.paxRow}>
              <Text style={styles.paxName}>{p.title} {p.firstName} {p.lastName}</Text>
              <Text style={styles.paxMeta}>{p.dateOfBirth}{p.nationality ? ` · ${p.nationality}` : ''}{p.passportNumber ? ` · ${p.passportNumber}` : ''}</Text>
            </View>
          ))}
        </Section>

        {/* Payment */}
        <Section title="Payment">
          <Row label="Base fare" value={formatPaise(booking.baseFarePaise ?? 0)} />
          <Row label="Taxes & fees" value={formatPaise(booking.taxesPaise ?? 0)} />
          <Row label="Platform fee" value={formatPaise(booking.platformFeePaise ?? 0)} />
          <Row label="Total" value={formatPaise(booking.totalAmountPaise ?? 0)} bold />
          {booking.paymentStatus ? <Row label="Payment status" value={booking.paymentStatus} /> : null}
        </Section>

        {/* Actions */}
        <View style={styles.actions}>
          {['CONFIRMED', 'TICKETED'].includes(status) && (
            <TouchableOpacity style={[styles.cancelBtn, cancelling && styles.btnDisabled]} disabled={cancelling} onPress={confirmCancel}>
              {cancelling ? <ActivityIndicator color="#dc2626" /> : <Text style={styles.cancelText}>Cancel booking</Text>}
            </TouchableOpacity>
          )}
          {status === 'CANCELLED' && (
            <TouchableOpacity style={styles.primaryBtn} onPress={() => router.replace('/flights')}><Text style={styles.primaryText}>Rebook flight</Text></TouchableOpacity>
          )}
          <TouchableOpacity style={styles.secondaryBtn} onPress={() => router.replace('/my-flights')}><Text style={styles.secondaryText}>My flights</Text></TouchableOpacity>
        </View>
      </ScrollView>
    </View>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      {children}
    </View>
  );
}

function Row({ label, value, bold }: { label: string; value: string; bold?: boolean }) {
  return (
    <View style={styles.dataRow}>
      <Text style={styles.dataLabel}>{label}</Text>
      <Text style={[styles.dataValue, bold && styles.dataBold]}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#f9fafb' },
  center:       { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb' },
  muted:        { color: '#9ca3af', fontSize: 14 },

  header:       { backgroundColor: '#fff', padding: 18 },
  headerTop:    { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  ref:          { fontSize: 16, fontWeight: '800', color: '#111827', letterSpacing: 1 },
  badge:        { borderRadius: 100, paddingHorizontal: 10, paddingVertical: 3 },
  badgeText:    { fontSize: 11, fontWeight: '700' },
  route:        { fontSize: 22, fontWeight: '800', color: '#111827', marginTop: 12 },
  sub:          { fontSize: 13, color: '#6b7280', marginTop: 4 },

  section:      { backgroundColor: '#fff', marginTop: 12, padding: 16 },
  sectionTitle: { fontSize: 14, fontWeight: '700', color: '#111827', marginBottom: 12 },

  tlRow:        { flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 5 },
  tlDot:        { fontSize: 14, color: '#d1d5db' },
  tlDotDone:    { color: '#16a34a' },
  tlLabel:      { fontSize: 14, color: '#9ca3af' },
  tlLabelDone:  { color: '#111827', fontWeight: '600' },

  paxRow:       { paddingVertical: 8, borderBottomWidth: 1, borderBottomColor: '#f9fafb' },
  paxName:      { fontSize: 14, fontWeight: '600', color: '#111827' },
  paxMeta:      { fontSize: 12, color: '#6b7280', marginTop: 2 },

  dataRow:      { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 6 },
  dataLabel:    { fontSize: 13, color: '#6b7280' },
  dataValue:    { fontSize: 13, color: '#111827', fontWeight: '600' },
  dataBold:     { fontSize: 16, fontWeight: '800', color: '#f97316' },

  actions:      { padding: 16, gap: 10 },
  cancelBtn:    { borderWidth: 1, borderColor: '#dc2626', borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  cancelText:   { color: '#dc2626', fontWeight: '700', fontSize: 15 },
  primaryBtn:   { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  primaryText:  { color: '#fff', fontWeight: '700', fontSize: 15 },
  secondaryBtn: { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, paddingVertical: 14, alignItems: 'center' },
  secondaryText:{ color: '#374151', fontWeight: '700', fontSize: 15 },
  btnDisabled:  { opacity: 0.6 },
});
