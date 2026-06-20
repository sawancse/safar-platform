import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, StyleSheet, ActivityIndicator, Alert, Modal, TextInput,
} from 'react-native';
import { useLocalSearchParams, useRouter, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';
import RazorpayCheckout from '@/components/RazorpayCheckout';
import type { PaymentResult } from '@/lib/payment';

type Tab = 'details' | 'provider' | 'review';

const STATUS_STYLE: Record<string, { label: string; bg: string; text: string }> = {
  PENDING_PAYMENT: { label: 'Pending Payment', bg: '#fef9c3', text: '#854d0e' },
  CONFIRMED:       { label: 'Confirmed', bg: '#dcfce7', text: '#14532d' },
  CANCELLED:       { label: 'Cancelled', bg: '#fee2e2', text: '#7f1d1d' },
  CHECKED_IN:      { label: 'Checked In', bg: '#dbeafe', text: '#1e40af' },
  COMPLETED:       { label: 'Completed', bg: '#f3f4f6', text: '#374151' },
};

export default function BookingDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const [tab, setTab] = useState<Tab>('details');
  const [booking, setBooking] = useState<any>(null);
  const [listing, setListing] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const [balancePay, setBalancePay] = useState<any>(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);
  const [reviewed, setReviewed] = useState(false);

  const load = useCallback(async () => {
    const token = await getAccessToken();
    if (!token || !id) { setLoading(false); return; }
    try {
      const all = await api.getMyBookings(token);
      const b = (all ?? []).find((x: any) => x.id === id);
      setBooking(b ?? null);
      if (b?.listingId) {
        try { setListing(await api.getListing(b.listingId)); } catch {}
      }
    } catch { setBooking(null); }
    finally { setLoading(false); }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  async function payBalance() {
    const token = await getAccessToken();
    if (!token || !booking) return;
    try {
      const order = await api.createCookPaymentOrder(booking.id, booking.dueAtPropertyPaise, token);
      setBalancePay(order);
    } catch (e: any) {
      Alert.alert('Could not start payment', e.message ?? 'Try again.');
    }
  }

  function onBalanceSuccess(_res: PaymentResult) {
    setBalancePay(null);
    Alert.alert('Payment received', 'Your remaining balance has been paid.');
    setTimeout(load, 1500);
  }

  async function submitReview() {
    const token = await getAccessToken();
    if (!token || !booking) return;
    setSubmittingReview(true);
    try {
      await api.createReview({ bookingId: booking.id, rating: reviewRating, comment: reviewComment || undefined }, token);
      setReviewed(true);
      Alert.alert('Thank you!', 'Your review has been submitted.');
    } catch (e: any) {
      Alert.alert('Error', e.message ?? 'Failed to submit review');
    } finally {
      setSubmittingReview(false);
    }
  }

  if (loading) return <View style={styles.center}><ActivityIndicator size="large" color="#f97316" /></View>;
  if (!booking) return <View style={styles.center}><Text style={styles.emptyTitle}>Booking not found</Text></View>;

  const s = STATUS_STYLE[booking.status] ?? STATUS_STYLE.COMPLETED;
  const hostName = listing?.hostName ?? listing?.host?.name ?? booking.hostName ?? 'Your host';
  const hostInitial = (hostName || 'H').charAt(0).toUpperCase();
  const hasBalance = booking.status === 'CONFIRMED' && booking.paymentMode === 'PARTIAL_PREPAID' && (booking.dueAtPropertyPaise ?? 0) > 0;
  const isReviewable = ['COMPLETED', 'CONFIRMED', 'CHECKED_IN'].includes(booking.status);
  const alreadyReviewed = booking.hasReview || reviewed;

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: 'Booking' }} />

      <View style={styles.headerCard}>
        <Text style={styles.title} numberOfLines={2}>{booking.listingTitle ?? listing?.title ?? 'Your stay'}</Text>
        <Text style={styles.sub}>{booking.city ?? listing?.city ?? ''}</Text>
        <View style={[styles.badge, { backgroundColor: s.bg }]}><Text style={[styles.badgeText, { color: s.text }]}>{s.label}</Text></View>
      </View>

      <View style={styles.tabRow}>
        {(['details', 'provider', 'review'] as Tab[]).map((t) => (
          <TouchableOpacity key={t} style={[styles.tabBtn, tab === t && styles.tabBtnActive]} onPress={() => setTab(t)}>
            <Text style={[styles.tabText, tab === t && styles.tabTextActive]}>{t.charAt(0).toUpperCase() + t.slice(1)}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        {tab === 'details' && (
          <>
            <View style={styles.card}>
              <Row label="Check-in" value={booking.checkInDate ?? '—'} />
              <Row label="Check-out" value={booking.checkOutDate ?? '—'} />
              <Row label="Guests" value={String(booking.guests ?? 1)} />
              {booking.bookingRef ? <Row label="Reference" value={booking.bookingRef} /> : null}
            </View>
            <View style={styles.card}>
              <Row label="Total" value={formatPaise(booking.totalAmountPaise ?? 0)} bold />
              {booking.paymentMode ? <Row label="Payment mode" value={booking.paymentMode.replace(/_/g, ' ')} /> : null}
              {(booking.dueAtPropertyPaise ?? 0) > 0 ? <Row label="Balance due" value={formatPaise(booking.dueAtPropertyPaise)} /> : null}
            </View>
            {hasBalance ? (
              <TouchableOpacity style={styles.primaryBtn} onPress={payBalance}>
                <Text style={styles.primaryBtnText}>Pay balance {formatPaise(booking.dueAtPropertyPaise)}</Text>
              </TouchableOpacity>
            ) : null}
          </>
        )}

        {tab === 'provider' && (
          <>
            <View style={styles.hostCard}>
              <View style={styles.hostAvatar}><Text style={styles.hostAvatarText}>{hostInitial}</Text></View>
              <View style={{ flex: 1 }}>
                <Text style={styles.hostName}>{hostName}</Text>
                <Text style={styles.hostSub}>{listing?.isStarHost || listing?.superhost ? '⭐ BhramanKaro Star Host' : 'Property host'}</Text>
                {listing?.hostSince ? <Text style={styles.hostMeta}>Hosting since {listing.hostSince}</Text> : null}
              </View>
            </View>
            <View style={styles.card}>
              {listing?.responseRate ? <Row label="Response rate" value={`${listing.responseRate}%`} /> : null}
              {listing?.responseTime ? <Row label="Responds in" value={listing.responseTime} /> : null}
              <Row label="Listing" value={listing?.title ?? booking.listingTitle ?? '—'} />
              {(listing?.city || booking.city) ? <Row label="Location" value={listing?.city ?? booking.city} /> : null}
              {listing?.checkInTime ? <Row label="Check-in time" value={listing.checkInTime} /> : null}
            </View>
            <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/messages')}>
              <Text style={styles.primaryBtnText}>Message host</Text>
            </TouchableOpacity>
            {booking.listingId ? (
              <TouchableOpacity style={styles.secondaryBtn} onPress={() => router.push(`/listing/${booking.listingId}`)}>
                <Text style={styles.secondaryBtnText}>View listing</Text>
              </TouchableOpacity>
            ) : null}
          </>
        )}

        {tab === 'review' && (
          alreadyReviewed ? (
            <View style={styles.card}><Text style={styles.bodyText}>You've already reviewed this stay. Thank you!</Text></View>
          ) : !isReviewable ? (
            <View style={styles.card}><Text style={styles.bodyText}>You can write a review once your stay is confirmed or completed.</Text></View>
          ) : (
            <View style={styles.card}>
              <Text style={styles.label}>Your rating</Text>
              <View style={styles.starRow}>
                {[1, 2, 3, 4, 5].map((st) => (
                  <TouchableOpacity key={st} onPress={() => setReviewRating(st)}>
                    <Text style={styles.star}>{st <= reviewRating ? '★' : '☆'}</Text>
                  </TouchableOpacity>
                ))}
              </View>
              <TextInput style={styles.commentInput} placeholder="Share your experience (optional)" placeholderTextColor="#9ca3af" multiline value={reviewComment} onChangeText={setReviewComment} textAlignVertical="top" />
              <TouchableOpacity style={[styles.primaryBtn, submittingReview && { opacity: 0.6 }]} disabled={submittingReview} onPress={submitReview}>
                {submittingReview ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryBtnText}>Submit review</Text>}
              </TouchableOpacity>
            </View>
          )
        )}

        <View style={{ height: 40 }} />
      </ScrollView>

      <Modal visible={balancePay !== null} animationType="slide" onRequestClose={() => setBalancePay(null)}>
        {balancePay && (
          <RazorpayCheckout
            order={balancePay}
            prefill={{ name: '', email: '', phone: '' }}
            onSuccess={onBalanceSuccess}
            onFailure={(err) => { Alert.alert('Payment failed', err); setBalancePay(null); }}
            onDismiss={() => setBalancePay(null)}
          />
        )}
      </Modal>
    </View>
  );
}

function Row({ label, value, bold }: { label: string; value: string; bold?: boolean }) {
  return <View style={styles.row}><Text style={[styles.rowLabel, bold && styles.bold]}>{label}</Text><Text style={[styles.rowVal, bold && styles.bold]}>{value}</Text></View>;
}

const styles = StyleSheet.create({
  container:   { flex: 1, backgroundColor: '#f9fafb' },
  center:      { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb' },
  emptyTitle:  { fontSize: 16, fontWeight: '700', color: '#374151' },
  headerCard:  { backgroundColor: '#fff', padding: 16, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  title:       { fontSize: 18, fontWeight: '800', color: '#111827' },
  sub:         { fontSize: 13, color: '#6b7280', marginTop: 2 },
  badge:       { alignSelf: 'flex-start', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 100, marginTop: 8 },
  badgeText:   { fontSize: 11, fontWeight: '700' },
  tabRow:      { flexDirection: 'row', backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#e5e7eb' },
  tabBtn:      { flex: 1, paddingVertical: 12, alignItems: 'center' },
  tabBtnActive:{ borderBottomWidth: 3, borderBottomColor: '#f97316' },
  tabText:     { fontSize: 13, fontWeight: '600', color: '#6b7280' },
  tabTextActive:{ color: '#f97316' },
  scroll:      { padding: 16 },
  card:        { backgroundColor: '#fff', borderRadius: 12, padding: 14, marginBottom: 12, borderWidth: 1, borderColor: '#f3f4f6' },
  row:         { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 6 },
  rowLabel:    { fontSize: 13, color: '#6b7280' },
  rowVal:      { fontSize: 13, color: '#374151', fontWeight: '600', flexShrink: 1, textAlign: 'right', marginLeft: 12 },
  bold:        { fontWeight: '800', color: '#111827', fontSize: 15 },
  bodyText:    { fontSize: 13, color: '#374151', lineHeight: 20 },
  label:       { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 8 },
  hostCard:    { flexDirection: 'row', alignItems: 'center', gap: 14, backgroundColor: '#fff', borderRadius: 12, padding: 16, marginBottom: 12, borderWidth: 1, borderColor: '#f3f4f6' },
  hostAvatar:  { width: 52, height: 52, borderRadius: 26, backgroundColor: '#f97316', alignItems: 'center', justifyContent: 'center' },
  hostAvatarText: { fontSize: 22, fontWeight: '700', color: '#fff' },
  hostName:    { fontSize: 16, fontWeight: '800', color: '#111827' },
  hostSub:     { fontSize: 12, color: '#6b7280', marginTop: 2 },
  hostMeta:    { fontSize: 11, color: '#9ca3af', marginTop: 2 },
  primaryBtn:  { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 14, alignItems: 'center', marginTop: 4 },
  primaryBtnText:{ color: '#fff', fontSize: 15, fontWeight: '700' },
  secondaryBtn:{ borderWidth: 1, borderColor: '#f97316', borderRadius: 12, paddingVertical: 13, alignItems: 'center', marginTop: 10 },
  secondaryBtnText: { color: '#f97316', fontSize: 15, fontWeight: '700' },
  starRow:     { flexDirection: 'row', marginBottom: 14 },
  star:        { fontSize: 34, color: '#f59e0b', marginRight: 8 },
  commentInput:{ borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, padding: 12, fontSize: 14, color: '#111827', minHeight: 100, backgroundColor: '#f9fafb', marginBottom: 14 },
});
