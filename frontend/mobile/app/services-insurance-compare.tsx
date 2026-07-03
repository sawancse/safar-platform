import { useState } from 'react';
import {
  View, Text, ScrollView, StyleSheet, ActivityIndicator, TouchableOpacity,
  Modal, TextInput, Alert, Linking,
} from 'react-native';
import { useLocalSearchParams, useRouter, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';
import CookPaymentWebView from '@/components/CookPaymentWebView';
import type { PaymentOrder } from '@/lib/payment';

type Product = 'health' | 'term' | 'motor' | 'travel';

const PRODUCT_META: Record<Product, { label: string; coverageType: string }> = {
  health: { label: 'Health Insurance', coverageType: 'HEALTH' },
  term:   { label: 'Term Life',        coverageType: 'LIFE_TERM' },
  motor:  { label: 'Motor Insurance',  coverageType: 'MOTOR' },
  travel: { label: 'Travel Insurance', coverageType: 'INTERNATIONAL_TRAVEL' },
};

export default function InsuranceCompareScreen() {
  const { product: productParam } = useLocalSearchParams<{ product?: string }>();
  const router = useRouter();
  const product = (['health', 'term', 'motor', 'travel'].includes(productParam || '')
    ? productParam : 'health') as Product;
  const meta = PRODUCT_META[product];

  const [step, setStep] = useState<'form' | 'results'>('form');
  const [loading, setLoading] = useState(false);
  const [plans, setPlans] = useState<any[]>([]);
  const [sortBy, setSortBy] = useState<'premium' | 'cover' | 'claim'>('premium');
  const [selectedAddOns, setSelectedAddOns] = useState<Record<string, string[]>>({});
  const [detailFor, setDetailFor] = useState<any>(null);

  // Form fields (superset; used per product)
  const [age, setAge] = useState('30');
  const [members, setMembers] = useState('30');   // comma-separated ages for health/travel
  const [city, setCity] = useState('');
  const [smoker, setSmoker] = useState(false);
  const [tenureYears, setTenureYears] = useState('1');
  const [regNo, setRegNo] = useState('');
  const [destination, setDestination] = useState('');
  const [tripDays, setTripDays] = useState('7');
  const [intl, setIntl] = useState(true);

  // Buy modal
  const [buyFor, setBuyFor] = useState<any>(null);
  const [form, setForm] = useState({ fullName: '', contactEmail: '', contactPhone: '' });
  const [buying, setBuying] = useState(false);
  const [result, setResult] = useState<any>(null);
  const [payCtx, setPayCtx] = useState<{ policyId: string; order: PaymentOrder } | null>(null);

  function parseAges(csv: string): number[] {
    return csv.split(',').map((s) => parseInt(s.trim(), 10)).filter((n) => !isNaN(n) && n > 0);
  }

  async function loadPlans() {
    setLoading(true);
    try {
      const body: any = { coverageType: meta.coverageType };
      if (product === 'health') {
        body.ages = parseAges(members);
      } else if (product === 'travel') {
        body.ages = parseAges(members);
        body.coverageType = intl ? 'INTERNATIONAL_TRAVEL' : 'DOMESTIC_TRAVEL';
        body.tenureDays = Math.max(1, parseInt(tripDays, 10) || 7);
        if (destination) body.destinationCode = destination;
      } else if (product === 'term') {
        body.ages = [parseInt(age, 10) || 30];
        body.tenureDays = (parseInt(tenureYears, 10) || 1) * 365;
      } else if (product === 'motor') {
        body.ages = [parseInt(age, 10) || 30];
      }
      const res = await api.compareInsurance(body);
      setPlans(res.plans ?? []);
      setStep('results');
    } catch (e: any) {
      Alert.alert('Could not load plans', e.message ?? 'Try again.');
    } finally {
      setLoading(false);
    }
  }

  function toggleAddOn(quoteId: string, code: string) {
    setSelectedAddOns((prev) => {
      const cur = prev[quoteId] ?? [];
      return { ...prev, [quoteId]: cur.includes(code) ? cur.filter((c) => c !== code) : [...cur, code] };
    });
  }

  function planTotal(p: any): number {
    const addOns = selectedAddOns[p.quoteId] ?? [];
    const extra = (p.addOns ?? [])
      .filter((a: any) => addOns.includes(a.code))
      .reduce((s: number, a: any) => s + (a.premiumPaise ?? 0), 0);
    return (p.premiumPaise ?? 0) + extra;
  }

  const sortedPlans = [...plans].sort((a, b) => {
    if (sortBy === 'premium') return (a.premiumPaise ?? 0) - (b.premiumPaise ?? 0);
    if (sortBy === 'cover') return (b.sumInsuredPaise ?? 0) - (a.sumInsuredPaise ?? 0);
    return (b.claimSettlementRatio ?? 0) - (a.claimSettlementRatio ?? 0);
  });

  async function confirmBuy() {
    if (!buyFor) return;
    if (!form.fullName || !form.contactEmail || !form.contactPhone) {
      Alert.alert('Missing details', 'Please enter name, email and phone.');
      return;
    }
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    setBuying(true);
    try {
      const order = await api.createInsuranceOrder({
        quoteId: buyFor.quoteId,
        coverageType: meta.coverageType,
        fullName: form.fullName,
        contactEmail: form.contactEmail,
        contactPhone: form.contactPhone,
        addOnCodes: selectedAddOns[buyFor.quoteId] ?? [],
      }, token);
      if (order.razorpayEnabled && order.razorpayOrderId) {
        setPayCtx({
          policyId: order.policyId,
          order: {
            orderId: order.razorpayOrderId,
            amount: order.amountPaise ?? order.premiumPaise,
            currency: 'INR', bookingId: '',
            razorpayKeyId: order.razorpayKeyId,
          },
        });
        setBuying(false);
      } else {
        const done = await api.confirmInsurancePayment({ policyId: order.policyId }, token);
        finishBuy(done);
      }
    } catch (e: any) {
      setBuying(false);
      Alert.alert('Purchase failed', e.message ?? 'Try again.');
    }
  }

  async function onPaymentSuccess(res: { paymentId: string; orderId: string; signature: string }) {
    if (!payCtx) return;
    const token = await getAccessToken();
    if (!token) return;
    try {
      const done = await api.confirmInsurancePayment({
        policyId: payCtx.policyId,
        razorpayOrderId: res.orderId,
        razorpayPaymentId: res.paymentId,
        razorpaySignature: res.signature,
      }, token);
      setPayCtx(null);
      finishBuy(done);
    } catch (e: any) {
      setPayCtx(null);
      Alert.alert('Confirmation failed', e.message ?? 'Check My Policies.');
    }
  }

  function finishBuy(done: any) {
    setResult(done);
    setBuying(false);
  }

  function openCertificate(policyRef: string) {
    Linking.openURL(api.insuranceCertificateUrl(policyRef)).catch(() => {});
  }

  return (
    <>
      <ScrollView style={styles.container} contentContainerStyle={{ padding: 16, paddingBottom: 48 }}>
        <Stack.Screen options={{ title: meta.label }} />
        <Text style={styles.title}>{meta.label}</Text>

        {step === 'form' ? (
          <View style={styles.card}>
            {product === 'health' || product === 'travel' ? (
              <>
                <Text style={styles.label}>Ages of members (comma separated)</Text>
                <TextInput style={styles.input} placeholder="30, 28, 5" placeholderTextColor="#9ca3af"
                  value={members} onChangeText={setMembers} keyboardType="numbers-and-punctuation" />
              </>
            ) : (
              <>
                <Text style={styles.label}>Age</Text>
                <TextInput style={styles.input} placeholder="30" placeholderTextColor="#9ca3af"
                  value={age} onChangeText={setAge} keyboardType="number-pad" />
              </>
            )}

            {product === 'health' || product === 'motor' ? (
              <>
                <Text style={styles.label}>City</Text>
                <TextInput style={styles.input} placeholder="Bengaluru" placeholderTextColor="#9ca3af"
                  value={city} onChangeText={setCity} />
              </>
            ) : null}

            {product === 'term' ? (
              <>
                <Text style={styles.label}>Tenure (years)</Text>
                <TextInput style={styles.input} placeholder="1" placeholderTextColor="#9ca3af"
                  value={tenureYears} onChangeText={setTenureYears} keyboardType="number-pad" />
                <TouchableOpacity style={styles.toggle} onPress={() => setSmoker((s) => !s)}>
                  <Text style={styles.toggleText}>{smoker ? '☑' : '☐'} Smoker</Text>
                </TouchableOpacity>
              </>
            ) : null}

            {product === 'motor' ? (
              <>
                <Text style={styles.label}>Registration number</Text>
                <TextInput style={styles.input} placeholder="KA01AB1234" placeholderTextColor="#9ca3af"
                  value={regNo} onChangeText={setRegNo} autoCapitalize="characters" />
              </>
            ) : null}

            {product === 'travel' ? (
              <>
                <TouchableOpacity style={styles.toggle} onPress={() => setIntl((s) => !s)}>
                  <Text style={styles.toggleText}>{intl ? '☑' : '☐'} International trip</Text>
                </TouchableOpacity>
                <Text style={styles.label}>Destination (optional)</Text>
                <TextInput style={styles.input} placeholder="Thailand" placeholderTextColor="#9ca3af"
                  value={destination} onChangeText={setDestination} />
                <Text style={styles.label}>Trip length (days)</Text>
                <TextInput style={styles.input} placeholder="7" placeholderTextColor="#9ca3af"
                  value={tripDays} onChangeText={setTripDays} keyboardType="number-pad" />
              </>
            ) : null}

            <TouchableOpacity style={[styles.primaryBtn, loading && { opacity: 0.6 }]} disabled={loading} onPress={loadPlans}>
              {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryBtnText}>See plans</Text>}
            </TouchableOpacity>
          </View>
        ) : (
          <>
            <View style={styles.sortRow}>
              {(['premium', 'cover', 'claim'] as const).map((s) => (
                <TouchableOpacity key={s} style={[styles.sortChip, sortBy === s && styles.sortChipActive]} onPress={() => setSortBy(s)}>
                  <Text style={[styles.sortChipText, sortBy === s && styles.sortChipTextActive]}>
                    {s === 'premium' ? 'Lowest price' : s === 'cover' ? 'Highest cover' : 'Best claims'}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
            <TouchableOpacity onPress={() => setStep('form')}><Text style={styles.editLink}>← Edit details</Text></TouchableOpacity>

            {sortedPlans.length === 0 ? (
              <Text style={styles.muted}>No plans found. Try adjusting your details.</Text>
            ) : sortedPlans.map((p) => (
              <View key={p.quoteId} style={styles.planCard}>
                {p.recommended ? <View style={styles.recBadge}><Text style={styles.recBadgeText}>RECOMMENDED</Text></View> : null}
                <Text style={styles.insurer}>{p.insurer}</Text>
                <Text style={styles.planName}>{p.planName}</Text>
                {p.tagline ? <Text style={styles.cardTagline}>{p.tagline}</Text> : null}
                <View style={styles.planMetaRow}>
                  <Text style={styles.planMeta}>Cover {formatPaise(p.sumInsuredPaise)}</Text>
                  {p.claimSettlementRatio ? <Text style={styles.planMeta}>CSR {p.claimSettlementRatio}%</Text> : null}
                  {p.insurerRating ? <Text style={styles.planMetaStar}>★ {p.insurerRating}</Text> : null}
                </View>

                {(p.addOns ?? []).length > 0 ? (
                  <View style={{ marginTop: 8 }}>
                    {p.addOns.map((a: any) => {
                      const on = (selectedAddOns[p.quoteId] ?? []).includes(a.code);
                      return (
                        <TouchableOpacity key={a.code} style={styles.addOnRow} onPress={() => toggleAddOn(p.quoteId, a.code)}>
                          <Text style={styles.addOnText}>{on ? '☑' : '☐'} {a.label}</Text>
                          <Text style={styles.addOnPrice}>+{formatPaise(a.premiumPaise)}</Text>
                        </TouchableOpacity>
                      );
                    })}
                  </View>
                ) : null}

                <View style={styles.planFooter}>
                  <Text style={styles.premium}>{formatPaise(planTotal(p))}</Text>
                  <View style={{ flexDirection: 'row', gap: 8 }}>
                    <TouchableOpacity style={styles.detailBtn} onPress={() => setDetailFor(p)}>
                      <Text style={styles.detailBtnText}>Details</Text>
                    </TouchableOpacity>
                    <TouchableOpacity style={styles.buyBtn} onPress={() => { setResult(null); setBuyFor(p); }}>
                      <Text style={styles.buyBtnText}>Buy now</Text>
                    </TouchableOpacity>
                  </View>
                </View>
              </View>
            ))}
          </>
        )}
      </ScrollView>

      {/* Plan detail modal */}
      <Modal visible={!!detailFor} transparent animationType="slide" onRequestClose={() => setDetailFor(null)}>
        <View style={styles.modalWrap}>
          <View style={styles.modalCard}>
            {detailFor ? (
              <ScrollView>
                <Text style={styles.modalTitle}>{detailFor.insurer} · {detailFor.planName}</Text>
                <Text style={styles.modalBody}>Cover: {formatPaise(detailFor.sumInsuredPaise)}</Text>
                <Text style={styles.modalBody}>Premium: {formatPaise(detailFor.premiumPaise)}</Text>
                {(detailFor.features ?? []).map((f: string, i: number) => (
                  <Text key={i} style={styles.highlight}>• {f}</Text>
                ))}
                {detailFor.wordingUrl ? (
                  <TouchableOpacity onPress={() => Linking.openURL(detailFor.wordingUrl).catch(() => {})}>
                    <Text style={styles.certLink}>Policy wording (PDF) →</Text>
                  </TouchableOpacity>
                ) : null}
                <TouchableOpacity style={styles.linkBtn} onPress={() => setDetailFor(null)}>
                  <Text style={styles.linkBtnText}>Close</Text>
                </TouchableOpacity>
              </ScrollView>
            ) : null}
          </View>
        </View>
      </Modal>

      {/* Buy modal */}
      <Modal visible={!!buyFor} transparent animationType="slide" onRequestClose={() => setBuyFor(null)}>
        <View style={styles.modalWrap}>
          <View style={styles.modalCard}>
            {result ? (
              <>
                <Text style={styles.modalTitle}>Policy issued 🎉</Text>
                <Text style={styles.modalBody}>Ref: {result.policyRef}</Text>
                {result.policyRef ? (
                  <TouchableOpacity style={styles.primaryBtn} onPress={() => openCertificate(result.policyRef)}>
                    <Text style={styles.primaryBtnText}>View / download certificate</Text>
                  </TouchableOpacity>
                ) : null}
                <TouchableOpacity style={styles.linkBtn} onPress={() => { setBuyFor(null); setResult(null); }}>
                  <Text style={styles.linkBtnText}>Done</Text>
                </TouchableOpacity>
              </>
            ) : (
              <>
                <Text style={styles.modalTitle}>{buyFor?.insurer} · {buyFor?.planName}</Text>
                <Text style={styles.modalBody}>Total: {buyFor ? formatPaise(planTotal(buyFor)) : ''}</Text>
                <TextInput style={styles.input} placeholder="Full name" placeholderTextColor="#9ca3af"
                  value={form.fullName} onChangeText={(v) => setForm((f) => ({ ...f, fullName: v }))} />
                <TextInput style={styles.input} placeholder="Email" placeholderTextColor="#9ca3af" autoCapitalize="none" keyboardType="email-address"
                  value={form.contactEmail} onChangeText={(v) => setForm((f) => ({ ...f, contactEmail: v }))} />
                <TextInput style={styles.input} placeholder="Phone" placeholderTextColor="#9ca3af" keyboardType="phone-pad"
                  value={form.contactPhone} onChangeText={(v) => setForm((f) => ({ ...f, contactPhone: v }))} />
                <TouchableOpacity style={[styles.primaryBtn, buying && { opacity: 0.6 }]} disabled={buying} onPress={confirmBuy}>
                  {buying ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryBtnText}>Pay & get policy</Text>}
                </TouchableOpacity>
                <TouchableOpacity style={styles.linkBtn} onPress={() => setBuyFor(null)}>
                  <Text style={styles.linkBtnText}>Cancel</Text>
                </TouchableOpacity>
              </>
            )}
          </View>
        </View>
      </Modal>

      {/* Razorpay payment webview */}
      <Modal visible={!!payCtx} animationType="slide" onRequestClose={() => setPayCtx(null)}>
        {payCtx ? (
          <CookPaymentWebView
            order={payCtx.order}
            prefill={{ name: form.fullName, email: form.contactEmail, phone: form.contactPhone }}
            onSuccess={onPaymentSuccess}
            onFailure={(err) => { setPayCtx(null); Alert.alert('Payment failed', err); }}
            onDismiss={() => setPayCtx(null)}
          />
        ) : null}
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#f9fafb' },
  title:        { fontSize: 22, fontWeight: '800', color: '#111827', marginBottom: 12 },
  card:         { backgroundColor: '#fff', borderRadius: 14, padding: 16, borderWidth: 1, borderColor: '#f3f4f6' },
  label:        { fontSize: 12, fontWeight: '600', color: '#6b7280', marginTop: 10, marginBottom: 4 },
  input:        { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: '#111827', backgroundColor: '#f9fafb' },
  toggle:       { paddingVertical: 10 },
  toggleText:   { fontSize: 14, color: '#374151', fontWeight: '600' },
  primaryBtn:   { backgroundColor: '#0ea5e9', borderRadius: 12, paddingVertical: 13, alignItems: 'center', marginTop: 16 },
  primaryBtnText: { color: '#fff', fontWeight: '700', fontSize: 15 },
  sortRow:      { flexDirection: 'row', gap: 8, marginBottom: 8 },
  sortChip:     { paddingHorizontal: 12, paddingVertical: 7, borderRadius: 100, backgroundColor: '#f3f4f6' },
  sortChipActive: { backgroundColor: '#0ea5e9' },
  sortChipText: { fontSize: 12, fontWeight: '600', color: '#374151' },
  sortChipTextActive: { color: '#fff' },
  editLink:     { fontSize: 13, color: '#0ea5e9', fontWeight: '600', marginBottom: 10 },
  muted:        { fontSize: 13, color: '#9ca3af', marginTop: 20, textAlign: 'center' },
  planCard:     { backgroundColor: '#fff', borderRadius: 14, padding: 16, marginBottom: 12, borderWidth: 1, borderColor: '#f3f4f6' },
  recBadge:     { alignSelf: 'flex-start', backgroundColor: '#dcfce7', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 100, marginBottom: 6 },
  recBadgeText: { fontSize: 9, fontWeight: '800', color: '#14532d', letterSpacing: 1 },
  insurer:      { fontSize: 12, color: '#6b7280', fontWeight: '600' },
  planName:     { fontSize: 16, fontWeight: '800', color: '#111827', marginTop: 2 },
  cardTagline:  { fontSize: 12, color: '#6b7280', marginTop: 3 },
  planMetaRow:  { flexDirection: 'row', flexWrap: 'wrap', gap: 12, marginTop: 8 },
  planMeta:     { fontSize: 12, color: '#374151', fontWeight: '600' },
  planMetaStar: { fontSize: 12, color: '#d97706', fontWeight: '700' },
  addOnRow:     { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 6 },
  addOnText:    { fontSize: 13, color: '#374151' },
  addOnPrice:   { fontSize: 13, color: '#0f766e', fontWeight: '600' },
  planFooter:   { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 12, borderTopWidth: 1, borderTopColor: '#f3f4f6', paddingTop: 12 },
  premium:      { fontSize: 18, fontWeight: '800', color: '#111827' },
  detailBtn:    { borderWidth: 1.5, borderColor: '#0ea5e9', borderRadius: 10, paddingHorizontal: 14, paddingVertical: 9 },
  detailBtnText:{ color: '#0ea5e9', fontWeight: '700', fontSize: 13 },
  buyBtn:       { backgroundColor: '#0ea5e9', borderRadius: 10, paddingHorizontal: 18, paddingVertical: 9 },
  buyBtnText:   { color: '#fff', fontWeight: '700', fontSize: 13 },
  highlight:    { fontSize: 13, color: '#374151', marginTop: 4 },
  certLink:     { fontSize: 13, fontWeight: '700', color: '#0ea5e9', marginTop: 10 },
  modalWrap:    { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  modalCard:    { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, paddingBottom: 32, maxHeight: '85%' },
  modalTitle:   { fontSize: 18, fontWeight: '800', color: '#111827', marginBottom: 10 },
  modalBody:    { fontSize: 14, color: '#374151', marginBottom: 6 },
  linkBtn:      { alignItems: 'center', paddingVertical: 12, marginTop: 4 },
  linkBtnText:  { fontSize: 14, color: '#6b7280', fontWeight: '600' },
});
