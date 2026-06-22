import { useEffect, useState } from 'react';
import {
  View, Text, ScrollView, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator, Alert, Modal,
} from 'react-native';
import { useLocalSearchParams, useRouter, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';
import CookPaymentWebView from '@/components/CookPaymentWebView';
import {
  OCCASIONS, PUJAS, LANGUAGES, ARRIVAL_SLOTS, formatSlot, pujasForOccasion,
  panditCountFor, samagriTierFor,
} from '@/lib/panditCatalog';

const GST_RATE = 0.18;
const ADVANCE_PCT = 0.60;

export default function PanditBookScreen() {
  const { puja: prePuja } = useLocalSearchParams<{ puja?: string }>();
  const router = useRouter();

  const preService = PUJAS.find(p => p.key === prePuja) || null;
  const [occasion, setOccasion] = useState(preService?.tags[0] ?? 'HOUSEWARMING');
  const [pujaKey, setPujaKey] = useState(prePuja ?? '');
  const [eventDate, setEventDate] = useState('');
  const [arrivalSlot, setArrivalSlot] = useState('');
  const [language, setLanguage] = useState('Hindi');
  const [gotra, setGotra] = useState('');
  const [familyNames, setFamilyNames] = useState('');

  const [address, setAddress] = useState('');
  const [city, setCity] = useState('');
  const [pincode, setPincode] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [customerEmail, setCustomerEmail] = useState('');
  const [specialRequests, setSpecialRequests] = useState('');

  const [matched, setMatched] = useState<any[]>([]);
  const [preferredId, setPreferredId] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [payOrder, setPayOrder] = useState<any>(null);
  const [done, setDone] = useState<any>(null);

  const pujas = pujasForOccasion(occasion);
  const selected = PUJAS.find(p => p.key === pujaKey) || null;

  // Reset puja if it no longer fits the occasion
  useEffect(() => {
    if (pujaKey && selected && !selected.tags.includes(occasion)) setPujaKey('');
  }, [occasion]); // eslint-disable-line

  // Pandit matcher — debounced; preserve pick if still in list
  useEffect(() => {
    if (!pujaKey || !language) { setMatched([]); return; }
    const h = setTimeout(() => {
      api.matchPandits({ occasion, language, gotra: gotra.trim() || undefined, city: city.trim() || undefined })
        .then((list: any[]) => {
          const arr = Array.isArray(list) ? list : [];
          setMatched(arr);
          setPreferredId(prev => (prev && arr.some(p => p.listingId === prev) ? prev : ''));
        })
        .catch(() => setMatched([]));
    }, 400);
    return () => clearTimeout(h);
  }, [occasion, pujaKey, language, gotra, city]);

  const price = (() => {
    if (!selected) return null;
    const service = selected.pricePaise;
    const gst = Math.round(service * GST_RATE);
    const total = service + gst;
    const advance = Math.round(total * ADVANCE_PCT);
    return { service, gst, total, advance, balance: total - advance, dakshina: selected.recommendedDakshinaPaise };
  })();

  async function submit() {
    if (!selected || !price) { Alert.alert('Pick a puja', 'Please choose a puja.'); return; }
    if (!eventDate || !arrivalSlot) { Alert.alert('Missing details', 'Pick a date and arrival time.'); return; }
    if (!customerName || !customerPhone || !address || !city || !pincode) {
      Alert.alert('Missing details', 'Fill name, phone, address, city and pincode.'); return;
    }
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    setSubmitting(true);
    try {
      const panditCount = panditCountFor(selected);
      const samagriTier = samagriTierFor(selected);
      const preferred = matched.find(m => m.listingId === preferredId);
      const menuDescription = JSON.stringify({
        type: 'PANDIT_PUJA', occasion, arrivalSlot, pujaKey: selected.key, pujaLabel: selected.label,
        pujaTier: selected.tier, inclusions: selected.inclusions, samagri: selected.samagri,
        durationHours: selected.durationHours, panditCount, samagriTier, language, gotra, familyNames,
        preferredPanditId: preferredId || undefined, preferredPanditName: preferred?.businessName || undefined,
      });
      const booking = await api.createEventBooking({
        eventType: occasion, eventDate, eventTime: arrivalSlot,
        durationHours: selected.durationHours + 1, guestCount: 1,
        venueAddress: address, city, pincode, customerName, customerPhone, customerEmail,
        menuDescription, specialRequests, panditCount, samagriTier,
      }, token);
      const order = await api.createCookPaymentOrder(booking.id, price.advance, token);
      setPayOrder({ order, booking });
    } catch (e: any) {
      Alert.alert('Booking failed', e.message ?? 'Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  async function onPaymentSuccess(res: { paymentId: string; orderId: string }) {
    const token = await getAccessToken();
    const booking = payOrder.booking;
    try {
      if (token) await api.markEventAdvancePaid(booking.id, res.orderId, res.paymentId, token);
      setPayOrder(null);
      setDone(booking);
    } catch (e: any) {
      Alert.alert('Payment confirmation failed', e.message ?? 'Contact support with your payment id.');
      setPayOrder(null);
    }
  }

  if (payOrder) {
    return (
      <Modal visible animationType="slide">
        <CookPaymentWebView
          order={payOrder.order}
          prefill={{ name: customerName, email: customerEmail, phone: customerPhone }}
          onSuccess={onPaymentSuccess}
          onFailure={(err) => { Alert.alert('Payment failed', err); setPayOrder(null); }}
          onDismiss={() => setPayOrder(null)}
        />
      </Modal>
    );
  }

  if (done) {
    return (
      <View style={styles.center}>
        <Stack.Screen options={{ title: 'Booked' }} />
        <Text style={{ fontSize: 56 }}>🪔</Text>
        <Text style={styles.successTitle}>Pandit booked!</Text>
        {done.bookingRef ? <Text style={styles.successRef}>Ref: {done.bookingRef}</Text> : null}
        <Text style={styles.successMsg}>Our team will confirm the pandit on a call. Balance + dakshina are payable on the day.</Text>
        <TouchableOpacity style={styles.primaryBtn} onPress={() => router.replace('/cook-bookings')}>
          <Text style={styles.primaryBtnText}>View my bookings</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => router.replace('/pandit')}><Text style={styles.linkText}>Back to pujas</Text></TouchableOpacity>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
      <Stack.Screen options={{ title: 'Book a pandit' }} />

      {/* Occasion */}
      <Text style={styles.label}>Occasion</Text>
      <View style={styles.pillRow}>
        {OCCASIONS.map(o => (
          <TouchableOpacity key={o.key} style={[styles.pill, occasion === o.key && styles.pillActive]} onPress={() => setOccasion(o.key)}>
            <Text style={[styles.pillText, occasion === o.key && styles.pillTextActive]}>{o.icon} {o.label}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Puja */}
      <Text style={styles.label}>Select puja</Text>
      {pujas.map(p => (
        <TouchableOpacity key={p.key} style={[styles.pujaRow, pujaKey === p.key && styles.pujaRowOn]} onPress={() => setPujaKey(p.key)}>
          <View style={{ flex: 1 }}>
            <Text style={styles.pujaName}>{p.label}</Text>
            <Text style={styles.pujaMeta}>{p.tier} · {p.durationHours}h · {p.inclusions[0]}</Text>
          </View>
          <Text style={styles.pujaPrice}>{formatPaise(p.pricePaise)}</Text>
          <Text style={[styles.check, pujaKey === p.key && styles.checkOn]}>{pujaKey === p.key ? '✓' : '+'}</Text>
        </TouchableOpacity>
      ))}

      {/* Date + slot */}
      <View style={styles.row}>
        <Field label="Date *" flex><TextInput style={styles.input} value={eventDate} onChangeText={setEventDate} placeholder="YYYY-MM-DD" placeholderTextColor="#9ca3af" /></Field>
      </View>
      <Text style={styles.label}>Arrival time</Text>
      <View style={styles.pillRow}>
        {ARRIVAL_SLOTS.map(s => (
          <TouchableOpacity key={s} style={[styles.pill, arrivalSlot === s && styles.pillActive]} onPress={() => setArrivalSlot(s)}>
            <Text style={[styles.pillText, arrivalSlot === s && styles.pillTextActive]}>{formatSlot(s)}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Language */}
      <Text style={styles.label}>Preferred language</Text>
      <View style={styles.pillRow}>
        {LANGUAGES.map(l => (
          <TouchableOpacity key={l} style={[styles.pill, language === l && styles.pillActive]} onPress={() => setLanguage(l)}>
            <Text style={[styles.pillText, language === l && styles.pillTextActive]}>{l}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <View style={styles.row}>
        <Field label="Gotra (optional)" flex><TextInput style={styles.input} value={gotra} onChangeText={setGotra} placeholder="e.g. Bharadwaj" placeholderTextColor="#9ca3af" /></Field>
        <Field label="Family names (sankalp)" flex><TextInput style={styles.input} value={familyNames} onChangeText={setFamilyNames} placeholder="e.g. Rohan & Priya" placeholderTextColor="#9ca3af" /></Field>
      </View>

      {/* Recommended pandits */}
      {pujaKey && matched.length > 0 && (
        <>
          <Text style={styles.label}>Recommended pandits for you</Text>
          <TouchableOpacity style={[styles.matchRow, preferredId === '' && styles.matchRowOn]} onPress={() => setPreferredId('')}>
            <Text style={styles.matchName}>No preference <Text style={styles.matchSub}>— assign the best-matched</Text></Text>
          </TouchableOpacity>
          {matched.map(m => (
            <TouchableOpacity key={m.listingId} style={[styles.matchRow, preferredId === m.listingId && styles.matchRowOn]} onPress={() => setPreferredId(m.listingId)}>
              <View style={{ flex: 1 }}>
                <Text style={styles.matchName}>{m.businessName}{m.trustTier === 'TOP_RATED' ? '  ⭐' : ''}</Text>
                <Text style={styles.matchSub}>
                  {m.rating ? `★ ${Number(m.rating).toFixed(1)}` : 'New'}{m.city ? ` · ${m.city}` : ''}
                  {Array.isArray(m.matchReasons) && m.matchReasons.length ? ` · ${m.matchReasons.slice(0, 2).join(', ')}` : ''}
                </Text>
              </View>
              {preferredId === m.listingId && <Text style={styles.checkOn}>✓</Text>}
            </TouchableOpacity>
          ))}
        </>
      )}

      {/* Venue + contact */}
      <Field label="Your name *"><TextInput style={styles.input} value={customerName} onChangeText={setCustomerName} placeholder="Full name" placeholderTextColor="#9ca3af" /></Field>
      <Field label="Phone *"><TextInput style={styles.input} value={customerPhone} onChangeText={setCustomerPhone} placeholder="10-digit mobile" placeholderTextColor="#9ca3af" keyboardType="phone-pad" maxLength={10} /></Field>
      <Field label="Email (optional)"><TextInput style={styles.input} value={customerEmail} onChangeText={setCustomerEmail} placeholder="you@example.com" placeholderTextColor="#9ca3af" keyboardType="email-address" autoCapitalize="none" /></Field>
      <Field label="Venue address *"><TextInput style={[styles.input, styles.textarea]} value={address} onChangeText={setAddress} placeholder="Flat, building, street, area" placeholderTextColor="#9ca3af" multiline /></Field>
      <View style={styles.row}>
        <Field label="City *" flex><TextInput style={styles.input} value={city} onChangeText={setCity} placeholder="City" placeholderTextColor="#9ca3af" /></Field>
        <Field label="Pincode *" flex><TextInput style={styles.input} value={pincode} onChangeText={setPincode} placeholder="6-digit" placeholderTextColor="#9ca3af" keyboardType="numeric" maxLength={6} /></Field>
      </View>
      <Field label="Special requests"><TextInput style={[styles.input, styles.textarea]} value={specialRequests} onChangeText={setSpecialRequests} placeholder="Mantras, avoidances, elder-sensitive timing…" placeholderTextColor="#9ca3af" multiline /></Field>

      {/* Price */}
      {price && (
        <View style={styles.priceBox}>
          <Text style={styles.priceLine}>Service charge <Text style={styles.priceVal}>{formatPaise(price.service)}</Text></Text>
          <Text style={styles.priceLine}>GST 18% <Text style={styles.priceVal}>{formatPaise(price.gst)}</Text></Text>
          <Text style={[styles.priceLine, styles.priceTotal]}>Total <Text style={styles.priceVal}>{formatPaise(price.total)}</Text></Text>
          <Text style={styles.priceLine}>Pay now (60%) <Text style={styles.priceAdvance}>{formatPaise(price.advance)}</Text></Text>
          <Text style={styles.priceHint}>Balance {formatPaise(price.balance)} + dakshina {formatPaise(price.dakshina)} (cash) on the day.</Text>
        </View>
      )}

      <TouchableOpacity style={[styles.primaryBtn, submitting && { opacity: 0.6 }]} disabled={submitting} onPress={submit}>
        {submitting ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryBtnText}>{price ? `Pay 60% advance (${formatPaise(price.advance)})` : 'Pick a puja to continue'}</Text>}
      </TouchableOpacity>
    </ScrollView>
  );
}

function Field({ label, children, flex }: { label: string; children: React.ReactNode; flex?: boolean }) {
  return <View style={[styles.field, flex && { flex: 1 }]}><Text style={styles.label}>{label}</Text>{children}</View>;
}

const styles = StyleSheet.create({
  container:   { flex: 1, backgroundColor: '#f9fafb' },
  center:      { flex: 1, backgroundColor: '#f9fafb', alignItems: 'center', justifyContent: 'center', padding: 24 },
  field:       { marginBottom: 14 },
  label:       { fontSize: 13, fontWeight: '700', color: '#374151', marginBottom: 6, marginTop: 4 },
  input:       { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: '#111827' },
  textarea:    { height: 80, textAlignVertical: 'top' },
  row:         { flexDirection: 'row', gap: 12 },
  pillRow:     { flexDirection: 'row', gap: 8, flexWrap: 'wrap', marginBottom: 10 },
  pill:        { paddingHorizontal: 12, paddingVertical: 7, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb', backgroundColor: '#fff' },
  pillActive:  { backgroundColor: '#ea580c', borderColor: '#ea580c' },
  pillText:    { fontSize: 12, fontWeight: '600', color: '#374151' },
  pillTextActive: { color: '#fff' },
  pujaRow:     { flexDirection: 'row', alignItems: 'center', gap: 10, backgroundColor: '#fff', borderRadius: 12, padding: 12, marginBottom: 8, borderWidth: 1, borderColor: '#f3f4f6' },
  pujaRowOn:   { borderColor: '#ea580c', backgroundColor: '#fff7ed' },
  pujaName:    { fontSize: 14, fontWeight: '700', color: '#111827' },
  pujaMeta:    { fontSize: 11, color: '#6b7280', marginTop: 2 },
  pujaPrice:   { fontSize: 13, fontWeight: '800', color: '#111827' },
  check:       { fontSize: 18, fontWeight: '800', color: '#9ca3af', width: 22, textAlign: 'center' },
  checkOn:     { color: '#ea580c' },
  matchRow:    { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderRadius: 10, padding: 12, marginBottom: 8, borderWidth: 1, borderColor: '#f3f4f6' },
  matchRowOn:  { borderColor: '#ea580c', backgroundColor: '#fff7ed' },
  matchName:   { fontSize: 13, fontWeight: '700', color: '#111827' },
  matchSub:    { fontSize: 11, color: '#6b7280', fontWeight: '400', marginTop: 2 },
  priceBox:    { backgroundColor: '#fff', borderRadius: 12, padding: 14, marginVertical: 12, borderWidth: 1, borderColor: '#f3f4f6' },
  priceLine:   { fontSize: 13, color: '#374151', marginBottom: 4 },
  priceVal:    { fontWeight: '700', color: '#111827' },
  priceTotal:  { fontSize: 15, fontWeight: '800', marginTop: 4 },
  priceAdvance:{ fontWeight: '800', color: '#ea580c' },
  priceHint:   { fontSize: 11, color: '#6b7280', marginTop: 6 },
  primaryBtn:  { backgroundColor: '#ea580c', borderRadius: 12, paddingVertical: 15, alignItems: 'center', marginTop: 4 },
  primaryBtnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
  linkText:    { color: '#ea580c', fontSize: 14, fontWeight: '600', marginTop: 16 },
  successTitle:{ fontSize: 22, fontWeight: '800', color: '#111827', marginTop: 12 },
  successRef:  { fontSize: 14, color: '#6b7280', marginTop: 4, fontWeight: '600' },
  successMsg:  { fontSize: 14, color: '#6b7280', textAlign: 'center', marginTop: 12, lineHeight: 20 },
});
