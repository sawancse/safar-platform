import { useEffect, useState } from 'react';
import {
  View, Text, TouchableOpacity, StyleSheet,
  ScrollView, ActivityIndicator, Alert, TextInput,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';
import { useNetworkStatus } from '@/hooks/useNetworkStatus';
import { queueBooking, getQueuedBookings } from '@/lib/offlineQueue';
import UpiPaymentSheet from '@/components/UpiPaymentSheet';

type PaymentMethod = 'razorpay' | 'upi';

function GuestRow({ label, subtitle, value, onMinus, onPlus, min = 0, max = 16 }: {
  label: string; subtitle: string; value: number; onMinus: () => void; onPlus: () => void; min?: number; max?: number;
}) {
  return (
    <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 8 }}>
      <View>
        <Text style={{ fontSize: 14, fontWeight: '600' }}>{label}</Text>
        <Text style={{ fontSize: 11, color: '#999' }}>{subtitle}</Text>
      </View>
      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12 }}>
        <TouchableOpacity onPress={onMinus} disabled={value <= min}
          style={{ width: 32, height: 32, borderRadius: 16, borderWidth: 1, borderColor: value <= min ? '#e5e5e5' : '#ccc', justifyContent: 'center', alignItems: 'center' }}>
          <Text style={{ color: value <= min ? '#e5e5e5' : '#333', fontSize: 18 }}>-</Text>
        </TouchableOpacity>
        <Text style={{ width: 20, textAlign: 'center', fontWeight: '600' }}>{value}</Text>
        <TouchableOpacity onPress={onPlus} disabled={value >= max}
          style={{ width: 32, height: 32, borderRadius: 16, borderWidth: 1, borderColor: value >= max ? '#e5e5e5' : '#ccc', justifyContent: 'center', alignItems: 'center' }}>
          <Text style={{ color: value >= max ? '#e5e5e5' : '#333', fontSize: 18 }}>+</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

export default function BookScreen() {
  const { listingId, checkIn: paramCheckIn, checkOut: paramCheckOut, guests: paramGuests } = useLocalSearchParams<{ listingId: string; checkIn?: string; checkOut?: string; guests?: string }>();
  const router = useRouter();
  const { isConnected, isInternetReachable, isSyncing, syncNow } = useNetworkStatus();

  const [listing, setListing] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [booking, setBooking] = useState<any>(null);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('upi');
  const [pendingQueueCount, setPendingQueueCount] = useState(0);

  const isOnline = isConnected && isInternetReachable;

  // Date and guest state with URL param defaults
  const todayStr = new Date().toISOString().split('T')[0];
  const tomorrowStr = (() => { const d = new Date(); d.setDate(d.getDate() + 1); return d.toISOString().split('T')[0]; })();

  const [checkIn, setCheckIn] = useState(paramCheckIn || todayStr);
  const [checkOut, setCheckOut] = useState(paramCheckOut || tomorrowStr);

  // Guest breakdown
  const initialGuests = paramGuests ? Math.max(1, parseInt(paramGuests, 10) || 1) : 1;
  const [adults, setAdults] = useState(initialGuests);
  const [children, setChildren] = useState(0);
  const [infants, setInfants] = useState(0);
  const [pets, setPets] = useState(0);
  const guests = adults + children;

  const nights = Math.max(1, Math.ceil((new Date(checkOut).getTime() - new Date(checkIn).getTime()) / 86400000));

  useEffect(() => {
    if (!listingId) return;
    api.getListing(listingId)
      .then(setListing)
      .catch(() => setError('Listing not found'))
      .finally(() => setLoading(false));
  }, [listingId]);

  // Load pending offline bookings count
  useEffect(() => {
    getQueuedBookings().then((q) => setPendingQueueCount(q.length));
  }, []);

  async function handleBook() {
    const token = await getAccessToken();
    if (!token) {
      router.push('/auth');
      return;
    }

    // If offline, queue the booking for later
    if (!isOnline) {
      try {
        await queueBooking({
          listingId: listingId!,
          checkIn: checkIn + 'T14:00:00',
          checkOut: checkOut + 'T11:00:00',
          guestsCount: guests,
        });
        const queue = await getQueuedBookings();
        setPendingQueueCount(queue.length);
        Alert.alert(
          'Saved Offline',
          'You are currently offline. Your booking has been queued and will be submitted automatically when you reconnect.',
          [{ text: 'OK', onPress: () => router.replace('/(tabs)/trips') }],
        );
      } catch (e: any) {
        setError(e.message || 'Failed to queue booking offline');
      }
      return;
    }

    setCreating(true);
    setError('');
    try {
      const b = await api.createBooking(
        { listingId, checkIn: checkIn + 'T14:00:00', checkOut: checkOut + 'T11:00:00', guestsCount: guests },
        token,
      );
      setBooking(b);
    } catch (e: any) {
      setError(e.message || 'Booking failed');
    } finally {
      setCreating(false);
    }
  }

  async function handleRazorpay() {
    if (!booking) return;
    const token = await getAccessToken();
    if (!token) return;
    try {
      const order = await api.createPaymentOrder(booking.id, token);
      // Razorpay RN SDK would open here; show stub success for now
      Alert.alert(
        'Payment',
        `Razorpay order created: ${order.razorpayOrderId}\n\nIn production integrate react-native-razorpay SDK.`,
        [
          {
            text: 'Simulate success',
            onPress: () => router.replace('/(tabs)/trips'),
          },
        ],
      );
    } catch (e: any) {
      setError(e.message || 'Payment failed');
    }
  }

  function handleUpiLaunched(txnRef: string) {
    // UPI intent has been sent to the external app.
    // In production, you would poll the backend to confirm
    // payment status using the txnRef. For now, show a
    // confirmation dialog.
    Alert.alert(
      'UPI Payment Initiated',
      `Transaction ref: ${txnRef}\n\nComplete the payment in your UPI app. We will confirm it automatically.`,
      [
        {
          text: 'I have paid',
          onPress: () => router.replace('/(tabs)/trips'),
        },
        {
          text: 'Cancel',
          style: 'cancel',
        },
      ],
    );
  }

  function handleUpiError(message: string) {
    setError(message);
  }

  async function handleSyncQueue() {
    const result = await syncNow();
    if (result) {
      const queue = await getQueuedBookings();
      setPendingQueueCount(queue.length);
      if (result.synced > 0 && result.failed === 0) {
        Alert.alert('Queue Synced', `${result.synced} booking(s) submitted successfully.`);
      } else if (result.synced > 0 && result.failed > 0) {
        Alert.alert(
          'Partial Sync',
          `${result.synced} submitted, ${result.failed} failed. Failed bookings will retry next time.`,
        );
      } else if (result.failed > 0) {
        Alert.alert('Sync Failed', `${result.failed} booking(s) could not be submitted. They will retry later.`);
      } else {
        Alert.alert('Queue Empty', 'No offline bookings to sync.');
      }
    }
  }

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#f97316" />
      </View>
    );
  }

  if (!listing) {
    return (
      <View style={styles.center}>
        <Text style={{ color: '#9ca3af' }}>{error || 'Listing not found'}</Text>
      </View>
    );
  }

  const subtotalPaise = listing.basePricePaise * nights;
  const gstPaise      = listing.gstApplicable ? Math.round(subtotalPaise * 0.18) : 0;
  const totalPaise    = subtotalPaise + gstPaise;

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
      <Text style={styles.pageTitle}>Confirm booking</Text>

      {/* Offline banner */}
      {!isOnline && (
        <View style={styles.offlineBanner}>
          <Text style={styles.offlineBannerText}>
            You are offline. You can queue your booking and it will be submitted when you reconnect.
          </Text>
        </View>
      )}

      {/* Pending offline bookings */}
      {pendingQueueCount > 0 && (
        <View style={styles.queueBanner}>
          <View style={{ flex: 1 }}>
            <Text style={styles.queueBannerText}>
              {pendingQueueCount} offline booking{pendingQueueCount > 1 ? 's' : ''} pending
            </Text>
          </View>
          {isOnline && (
            <TouchableOpacity
              style={styles.syncBtn}
              onPress={handleSyncQueue}
              disabled={isSyncing}
            >
              {isSyncing ? (
                <ActivityIndicator color="#1d4ed8" size="small" />
              ) : (
                <Text style={styles.syncBtnText}>Sync now</Text>
              )}
            </TouchableOpacity>
          )}
        </View>
      )}

      {/* Date & Guest selection */}
      <View style={styles.card}>
        <View style={styles.dateRow}>
          <View style={{ flex: 1, marginRight: 8 }}>
            <Text style={styles.inputLabel}>Check-in</Text>
            <TextInput
              style={styles.dateInput}
              placeholder="YYYY-MM-DD"
              placeholderTextColor="#9ca3af"
              value={checkIn}
              onChangeText={setCheckIn}
            />
          </View>
          <View style={{ flex: 1, marginLeft: 8 }}>
            <Text style={styles.inputLabel}>Check-out</Text>
            <TextInput
              style={styles.dateInput}
              placeholder="YYYY-MM-DD"
              placeholderTextColor="#9ca3af"
              value={checkOut}
              onChangeText={setCheckOut}
            />
          </View>
        </View>
        <Text style={styles.inputLabel}>Guests</Text>
        <GuestRow label="Adults" subtitle="Ages 13+" value={adults}
          onMinus={() => setAdults((v) => Math.max(1, v - 1))}
          onPlus={() => setAdults((v) => Math.min(listing?.maxGuests || 16, v + 1))}
          min={1} max={listing?.maxGuests || 16} />
        <GuestRow label="Children" subtitle="Ages 2-12" value={children}
          onMinus={() => setChildren((v) => Math.max(0, v - 1))}
          onPlus={() => setChildren((v) => Math.min((listing?.maxGuests || 16) - adults, v + 1))}
          min={0} max={(listing?.maxGuests || 16) - adults} />
        <GuestRow label="Infants" subtitle="Under 2" value={infants}
          onMinus={() => setInfants((v) => Math.max(0, v - 1))}
          onPlus={() => setInfants((v) => Math.min(5, v + 1))}
          min={0} max={5} />
        <GuestRow label="Pets" subtitle="Service animals welcome" value={pets}
          onMinus={() => setPets((v) => Math.max(0, v - 1))}
          onPlus={() => setPets((v) => Math.min(5, v + 1))}
          min={0} max={5} />
      </View>

      {/* Listing summary */}
      <View style={styles.card}>
        <Text style={styles.listingTitle}>{listing.title}</Text>
        <Text style={styles.listingCity}>{listing.city}, {listing.state}</Text>
        <Text style={styles.dates}>{checkIn} → {checkOut} / {nights} night{nights > 1 ? 's' : ''}</Text>
        <Text style={styles.dates}>{guests} guest{guests > 1 ? 's' : ''}</Text>
      </View>

      {/* Price breakdown */}
      <View style={styles.card}>
        <View style={styles.priceRow}>
          <Text style={styles.priceLabel}>{formatPaise(listing.basePricePaise)} x {nights} night</Text>
          <Text style={styles.priceValue}>{formatPaise(subtotalPaise)}</Text>
        </View>
        {gstPaise > 0 && (
          <View style={styles.priceRow}>
            <Text style={[styles.priceLabel, { color: '#9ca3af' }]}>GST (18%)</Text>
            <Text style={[styles.priceValue, { color: '#9ca3af' }]}>{formatPaise(gstPaise)}</Text>
          </View>
        )}
        <View style={[styles.priceRow, styles.priceTotal]}>
          <Text style={styles.priceTotalLabel}>Total</Text>
          <Text style={styles.priceTotalValue}>{formatPaise(totalPaise)}</Text>
        </View>
        <Text style={styles.note}>Zero deposit / Micro-insurance included</Text>
      </View>

      {error !== '' && (
        <View style={styles.errorBox}>
          <Text style={styles.errorText}>{error}</Text>
        </View>
      )}

      {!booking ? (
        /* Step 1: Create booking (or queue offline) */
        <TouchableOpacity
          style={[styles.btn, creating && styles.btnDisabled]}
          disabled={creating}
          onPress={handleBook}
        >
          {creating ? (
            <ActivityIndicator color="#fff" />
          ) : !isOnline ? (
            <Text style={styles.btnText}>Save Booking Offline</Text>
          ) : (
            <Text style={styles.btnText}>Proceed to Payment</Text>
          )}
        </TouchableOpacity>
      ) : (
        /* Step 2: Payment method selection and pay */
        <View>
          {/* Payment method toggle */}
          <View style={styles.card}>
            <Text style={styles.payMethodTitle}>Choose payment method</Text>
            <View style={styles.payMethodRow}>
              <TouchableOpacity
                style={[
                  styles.payMethodOption,
                  paymentMethod === 'upi' && styles.payMethodOptionActive,
                ]}
                onPress={() => setPaymentMethod('upi')}
                activeOpacity={0.85}
              >
                <Text
                  style={[
                    styles.payMethodOptionIcon,
                    paymentMethod === 'upi' && styles.payMethodOptionIconActive,
                  ]}
                >
                  UPI
                </Text>
                <Text
                  style={[
                    styles.payMethodOptionText,
                    paymentMethod === 'upi' && styles.payMethodOptionTextActive,
                  ]}
                >
                  UPI / BHIM
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[
                  styles.payMethodOption,
                  paymentMethod === 'razorpay' && styles.payMethodOptionActive,
                ]}
                onPress={() => setPaymentMethod('razorpay')}
                activeOpacity={0.85}
              >
                <Text
                  style={[
                    styles.payMethodOptionIcon,
                    paymentMethod === 'razorpay' && styles.payMethodOptionIconActive,
                  ]}
                >
                  R
                </Text>
                <Text
                  style={[
                    styles.payMethodOptionText,
                    paymentMethod === 'razorpay' && styles.payMethodOptionTextActive,
                  ]}
                >
                  Razorpay
                </Text>
              </TouchableOpacity>
            </View>
          </View>

          {/* Payment action based on selected method */}
          {paymentMethod === 'upi' ? (
            <UpiPaymentSheet
              amountPaise={booking.totalAmountPaise}
              bookingId={booking.id}
              onPaymentLaunched={handleUpiLaunched}
              onPaymentFailed={handleUpiError}
            />
          ) : (
            <TouchableOpacity
              style={[styles.btn, { backgroundColor: '#1d4ed8' }]}
              onPress={handleRazorpay}
            >
              <Text style={styles.btnText}>Pay {formatPaise(booking.totalAmountPaise)} with Razorpay</Text>
            </TouchableOpacity>
          )}

          {/* Cash on Arrival */}
          <TouchableOpacity
            style={[styles.btn, { backgroundColor: '#16a34a', marginTop: 4 }]}
            onPress={async () => {
              try {
                const token = await getAccessToken();
                if (!token) return;
                await api.confirmBooking(booking.id, token);
                Alert.alert('Booking Confirmed', 'You will pay cash on arrival.', [
                  { text: 'OK', onPress: () => router.replace('/(tabs)/trips') },
                ]);
              } catch (e: any) {
                setError(e.message || 'Failed to confirm booking');
              }
            }}
          >
            <Text style={styles.btnText}>Cash on Arrival</Text>
          </TouchableOpacity>
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container:       { flex: 1, backgroundColor: '#f9fafb' },
  center:          { flex: 1, alignItems: 'center', justifyContent: 'center' },
  pageTitle:       { fontSize: 22, fontWeight: '700', color: '#111827', marginBottom: 16 },
  card:            { backgroundColor: '#fff', borderRadius: 16, padding: 16, marginBottom: 12, borderWidth: 1, borderColor: '#f3f4f6' },
  listingTitle:    { fontSize: 15, fontWeight: '600', color: '#111827' },
  listingCity:     { fontSize: 12, color: '#6b7280', marginTop: 2 },
  dates:           { fontSize: 13, color: '#374151', marginTop: 6 },
  priceRow:        { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
  priceLabel:      { fontSize: 14, color: '#374151' },
  priceValue:      { fontSize: 14, color: '#374151' },
  priceTotal:      { borderTopWidth: 1, borderTopColor: '#f3f4f6', paddingTop: 10, marginTop: 4 },
  priceTotalLabel: { fontSize: 15, fontWeight: '700', color: '#111827' },
  priceTotalValue: { fontSize: 15, fontWeight: '700', color: '#111827' },
  note:            { fontSize: 11, color: '#9ca3af', marginTop: 8 },
  errorBox:        { backgroundColor: '#fef2f2', borderRadius: 12, padding: 12, marginBottom: 12 },
  errorText:       { color: '#dc2626', fontSize: 13 },
  btn:             { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 14, alignItems: 'center', marginBottom: 12 },
  btnDisabled:     { opacity: 0.5 },
  btnText:         { color: '#fff', fontWeight: '700', fontSize: 15 },

  // Date & guest inputs
  dateRow:       { flexDirection: 'row', marginBottom: 12 },
  inputLabel:    { fontSize: 12, fontWeight: '600', color: '#6b7280', marginBottom: 4 },
  dateInput:     { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 10, fontSize: 14, color: '#111827', backgroundColor: '#f9fafb' },
  guestRow:      { flexDirection: 'row', alignItems: 'center', marginTop: 4 },
  guestBtn:      { width: 36, height: 36, borderRadius: 18, borderWidth: 1, borderColor: '#e5e7eb', alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb' },
  guestBtnText:  { fontSize: 18, fontWeight: '600', color: '#374151' },
  guestCount:    { fontSize: 16, fontWeight: '600', color: '#111827', marginHorizontal: 16 },

  // Offline banner
  offlineBanner:     { backgroundColor: '#fef3c7', borderRadius: 12, padding: 12, marginBottom: 12, borderWidth: 1, borderColor: '#fcd34d' },
  offlineBannerText: { fontSize: 13, color: '#92400e', lineHeight: 18 },

  // Queue banner
  queueBanner:     { backgroundColor: '#dbeafe', borderRadius: 12, padding: 12, marginBottom: 12, borderWidth: 1, borderColor: '#93c5fd', flexDirection: 'row', alignItems: 'center' },
  queueBannerText: { fontSize: 13, color: '#1e40af' },
  syncBtn:         { backgroundColor: '#eff6ff', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 6, marginLeft: 8 },
  syncBtnText:     { fontSize: 12, fontWeight: '600', color: '#1d4ed8' },

  // Payment method selector
  payMethodTitle:     { fontSize: 14, fontWeight: '600', color: '#111827', marginBottom: 10 },
  payMethodRow:       { flexDirection: 'row', gap: 10 },
  payMethodOption:    { flex: 1, flexDirection: 'row', alignItems: 'center', borderWidth: 1.5, borderColor: '#e5e7eb', borderRadius: 12, padding: 12 },
  payMethodOptionActive: { borderColor: '#5b21b6', backgroundColor: '#f5f3ff' },
  payMethodOptionIcon: {
    fontSize: 11,
    fontWeight: '800',
    color: '#6b7280',
    backgroundColor: '#f3f4f6',
    paddingHorizontal: 6,
    paddingVertical: 3,
    borderRadius: 4,
    overflow: 'hidden',
    letterSpacing: 0.5,
  },
  payMethodOptionIconActive: { color: '#5b21b6', backgroundColor: '#ede9fe' },
  payMethodOptionText:       { fontSize: 13, fontWeight: '600', color: '#6b7280', marginLeft: 8 },
  payMethodOptionTextActive: { color: '#5b21b6' },
});
