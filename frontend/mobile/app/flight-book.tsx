import { useState } from 'react';
import {
  View, Text, ScrollView, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator, Alert,
} from 'react-native';
import { useLocalSearchParams, useRouter, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';
import { findAirport } from '@/lib/airports';

interface Pax {
  title: string; firstName: string; lastName: string; dateOfBirth: string;
  gender: string; nationality: string; passportNumber: string; passportExpiry: string;
}

const emptyPax = (): Pax => ({ title: 'Mr', firstName: '', lastName: '', dateOfBirth: '', gender: 'male', nationality: 'IN', passportNumber: '', passportExpiry: '' });

export default function FlightBookScreen() {
  const params = useLocalSearchParams<Record<string, string>>();
  const router = useRouter();
  const intl = params.international === 'true';
  const count = Math.max(1, parseInt(params.passengers ?? '1', 10));
  const perPax = parseInt(params.totalPaise ?? '0', 10);

  const [pax, setPax] = useState<Pax[]>(Array.from({ length: count }, emptyPax));
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const origin = findAirport(params.origin ?? '');
  const destination = findAirport(params.destination ?? '');

  const baseFare = perPax * count;
  const taxes = Math.round(baseFare * 0.05);
  const platformFee = Math.round(baseFare * 0.02);
  const grandTotal = baseFare + taxes + platformFee;

  function update(i: number, field: keyof Pax, value: string) {
    setPax((prev) => prev.map((p, idx) => (idx === i ? { ...p, [field]: value } : p)));
  }

  async function submit() {
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    for (const p of pax) {
      if (!p.firstName || !p.lastName || !p.dateOfBirth) { Alert.alert('Missing details', 'Each passenger needs name and date of birth.'); return; }
      if (intl && (!p.passportNumber || !p.passportExpiry)) { Alert.alert('Passport required', 'International flights need passport details for each passenger.'); return; }
    }
    if (!email || !phone) { Alert.alert('Contact required', 'Enter contact email and phone.'); return; }

    setSubmitting(true);
    try {
      const booking = await api.createFlightBooking({
        offerId: params.offerId, origin: params.origin, destination: params.destination,
        departureDate: params.departureDate, cabinClass: params.cabinClass,
        airline: params.airline, flightNumber: params.flightNumber, international: intl,
        passengers: pax.map((p) => ({
          title: p.title, firstName: p.firstName, lastName: p.lastName, dateOfBirth: p.dateOfBirth,
          gender: p.gender, nationality: p.nationality,
          ...(intl ? { passportNumber: p.passportNumber, passportExpiry: p.passportExpiry } : {}),
        })),
        contactEmail: email, contactPhone: phone, totalAmountPaise: grandTotal,
      }, token);
      const bookingId = booking.id ?? booking.bookingId;
      // MVP: auto-confirm payment with placeholder ids (mirrors web flow).
      const ts = Date.now();
      await api.confirmFlightPayment(bookingId, `order_safar_${ts}`, `pay_safar_${ts}`, token).catch(() => {});
      router.replace(`/flight/${bookingId}`);
    } catch (e: any) {
      Alert.alert('Booking failed', e.message ?? 'Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
      <Stack.Screen options={{ title: 'Passenger details' }} />

      <View style={styles.summary}>
        <Text style={styles.summaryRoute}>{origin?.city} ({params.origin}) → {destination?.city} ({params.destination})</Text>
        <Text style={styles.summarySub}>{params.airline}{params.flightNumber ? ` · ${params.flightNumber}` : ''} · {params.departureDate}</Text>
      </View>

      {pax.map((p, i) => (
        <View key={i} style={styles.paxCard}>
          <Text style={styles.paxTitle}>Passenger {i + 1}</Text>
          <View style={styles.pillRow}>
            {['Mr', 'Mrs', 'Ms'].map((t) => (
              <TouchableOpacity key={t} style={[styles.pill, p.title === t && styles.pillActive]} onPress={() => update(i, 'title', t)}>
                <Text style={[styles.pillText, p.title === t && styles.pillTextActive]}>{t}</Text>
              </TouchableOpacity>
            ))}
          </View>
          <View style={styles.row}>
            <TextInput style={[styles.input, { flex: 1 }]} value={p.firstName} onChangeText={(v) => update(i, 'firstName', v)} placeholder="First name" placeholderTextColor="#9ca3af" />
            <TextInput style={[styles.input, { flex: 1 }]} value={p.lastName} onChangeText={(v) => update(i, 'lastName', v)} placeholder="Last name" placeholderTextColor="#9ca3af" />
          </View>
          <TextInput style={styles.input} value={p.dateOfBirth} onChangeText={(v) => update(i, 'dateOfBirth', v)} placeholder="Date of birth (YYYY-MM-DD)" placeholderTextColor="#9ca3af" />
          <View style={styles.pillRow}>
            {['male', 'female', 'other'].map((g) => (
              <TouchableOpacity key={g} style={[styles.pill, p.gender === g && styles.pillActive]} onPress={() => update(i, 'gender', g)}>
                <Text style={[styles.pillText, p.gender === g && styles.pillTextActive]}>{g[0].toUpperCase() + g.slice(1)}</Text>
              </TouchableOpacity>
            ))}
          </View>
          {intl && (
            <>
              <TextInput style={styles.input} value={p.nationality} onChangeText={(v) => update(i, 'nationality', v.toUpperCase())} placeholder="Nationality (e.g. IN)" placeholderTextColor="#9ca3af" autoCapitalize="characters" maxLength={2} />
              <View style={styles.row}>
                <TextInput style={[styles.input, { flex: 1 }]} value={p.passportNumber} onChangeText={(v) => update(i, 'passportNumber', v.toUpperCase())} placeholder="Passport no." placeholderTextColor="#9ca3af" autoCapitalize="characters" />
                <TextInput style={[styles.input, { flex: 1 }]} value={p.passportExpiry} onChangeText={(v) => update(i, 'passportExpiry', v)} placeholder="Expiry YYYY-MM-DD" placeholderTextColor="#9ca3af" />
              </View>
            </>
          )}
        </View>
      ))}

      <View style={styles.paxCard}>
        <Text style={styles.paxTitle}>Contact details</Text>
        <TextInput style={styles.input} value={email} onChangeText={setEmail} placeholder="Email" placeholderTextColor="#9ca3af" keyboardType="email-address" autoCapitalize="none" />
        <TextInput style={styles.input} value={phone} onChangeText={setPhone} placeholder="Phone" placeholderTextColor="#9ca3af" keyboardType="phone-pad" />
      </View>

      <View style={styles.priceCard}>
        <PriceRow label={`Base fare (${count} pax)`} value={formatPaise(baseFare)} />
        <PriceRow label="Taxes & fees" value={formatPaise(taxes)} />
        <PriceRow label="Platform fee" value={formatPaise(platformFee)} />
        <View style={styles.priceDivider} />
        <PriceRow label="Total" value={formatPaise(grandTotal)} bold />
      </View>

      <TouchableOpacity style={[styles.payBtn, submitting && styles.btnDisabled]} disabled={submitting} onPress={submit}>
        {submitting ? <ActivityIndicator color="#fff" /> : <Text style={styles.payBtnText}>Proceed to pay {formatPaise(grandTotal)}</Text>}
      </TouchableOpacity>
    </ScrollView>
  );
}

function PriceRow({ label, value, bold }: { label: string; value: string; bold?: boolean }) {
  return (
    <View style={styles.priceRow}>
      <Text style={[styles.priceLabel, bold && styles.priceBold]}>{label}</Text>
      <Text style={[styles.priceValue, bold && styles.priceBold]}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container:     { flex: 1, backgroundColor: '#f9fafb' },
  summary:       { backgroundColor: '#fff7ed', borderRadius: 12, padding: 14, marginBottom: 16, borderWidth: 1, borderColor: '#fed7aa' },
  summaryRoute:  { fontSize: 15, fontWeight: '700', color: '#111827' },
  summarySub:    { fontSize: 12, color: '#6b7280', marginTop: 4 },

  paxCard:       { backgroundColor: '#fff', borderRadius: 14, padding: 14, marginBottom: 14, borderWidth: 1, borderColor: '#f3f4f6' },
  paxTitle:      { fontSize: 15, fontWeight: '700', color: '#111827', marginBottom: 10 },
  row:           { flexDirection: 'row', gap: 10 },
  input:         { backgroundColor: '#f9fafb', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: '#111827', marginBottom: 10 },
  pillRow:       { flexDirection: 'row', gap: 8, marginBottom: 10 },
  pill:          { paddingHorizontal: 14, paddingVertical: 7, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb', backgroundColor: '#fff' },
  pillActive:    { backgroundColor: '#f97316', borderColor: '#f97316' },
  pillText:      { fontSize: 12, fontWeight: '600', color: '#374151' },
  pillTextActive:{ color: '#fff' },

  priceCard:     { backgroundColor: '#fff', borderRadius: 14, padding: 16, marginBottom: 16, borderWidth: 1, borderColor: '#f3f4f6' },
  priceRow:      { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 5 },
  priceLabel:    { fontSize: 13, color: '#6b7280' },
  priceValue:    { fontSize: 13, color: '#111827', fontWeight: '600' },
  priceDivider:  { height: 1, backgroundColor: '#f3f4f6', marginVertical: 8 },
  priceBold:     { fontSize: 16, fontWeight: '800', color: '#111827' },

  payBtn:        { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 15, alignItems: 'center' },
  btnDisabled:   { opacity: 0.6 },
  payBtnText:    { color: '#fff', fontSize: 16, fontWeight: '700' },
});
