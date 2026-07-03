import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, ScrollView, StyleSheet, ActivityIndicator, TouchableOpacity,
  Modal, TextInput, Alert, Linking, RefreshControl,
} from 'react-native';
import { useRouter, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';
import CookPaymentWebView from '@/components/CookPaymentWebView';
import type { PaymentOrder } from '@/lib/payment';

// Coverage types that have a full PolicyBazaar-style compare flow.
const COMPARE_PRODUCT: Record<string, string> = {
  HEALTH: 'health',
  LIFE_TERM: 'term',
  MOTOR: 'motor',
  INTERNATIONAL_TRAVEL: 'travel',
};

const STATUS_STYLE: Record<string, { bg: string; text: string }> = {
  ACTIVE: { bg: '#dcfce7', text: '#14532d' },
  ISSUED: { bg: '#dcfce7', text: '#14532d' },
  PENDING_PAYMENT: { bg: '#fef9c3', text: '#854d0e' },
  CANCELLED: { bg: '#fee2e2', text: '#7f1d1d' },
  REFUNDED: { bg: '#e0e7ff', text: '#3730a3' },
  EXPIRED: { bg: '#f3f4f6', text: '#374151' },
};

export default function InsuranceHubScreen() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [insurance, setInsurance] = useState<any[]>([]);
  const [loans, setLoans] = useState<any[]>([]);
  const [quotes, setQuotes] = useState<Record<string, any>>({});
  const [policies, setPolicies] = useState<any[]>([]);
  const [quoting, setQuoting] = useState<string | null>(null);

  // Buy modal
  const [buyFor, setBuyFor] = useState<any>(null);
  const [form, setForm] = useState({ fullName: '', contactEmail: '', contactPhone: '' });
  const [buying, setBuying] = useState(false);
  const [result, setResult] = useState<any>(null);

  // Razorpay webview
  const [payCtx, setPayCtx] = useState<{ policyId: string; order: PaymentOrder } | null>(null);

  // Advisor modal
  const [advisorOpen, setAdvisorOpen] = useState(false);
  const [advisor, setAdvisor] = useState({ name: '', phone: '', preferredTime: '' });

  const load = useCallback(async () => {
    try {
      const products = await api.getInsuranceProducts();
      setInsurance(products.filter((p) => p.category === 'INSURANCE'));
      setLoans(products.filter((p) => p.category === 'LOAN'));
      const token = await getAccessToken();
      if (token) {
        const my = await api.getMyInsurancePolicies(token);
        setPolicies(my.content ?? []);
      }
    } catch {
      /* keep empty */
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => { load(); }, [load]);

  async function getQuote(p: any) {
    setQuoting(p.key);
    try {
      const q = await api.quoteInsurance({ coverageType: p.coverageType });
      setQuotes((prev) => ({ ...prev, [p.key]: q }));
    } catch (e: any) {
      Alert.alert('Could not fetch price', e.message ?? 'Try again.');
    } finally {
      setQuoting(null);
    }
  }

  function openBuy(p: any) {
    setResult(null);
    setBuyFor(p);
  }

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
      const quote = quotes[buyFor.key];
      const order = await api.createInsuranceOrder({
        quoteId: quote?.quoteId,
        coverageType: buyFor.coverageType,
        fullName: form.fullName,
        contactEmail: form.contactEmail,
        contactPhone: form.contactPhone,
      }, token);

      if (order.razorpayEnabled && order.razorpayOrderId) {
        // Live: open Razorpay checkout in a WebView.
        setPayCtx({
          policyId: order.policyId,
          order: {
            orderId: order.razorpayOrderId,
            amount: order.amountPaise ?? order.premiumPaise,
            currency: 'INR',
            bookingId: '',
            razorpayKeyId: order.razorpayKeyId,
          },
        });
        setBuying(false);
      } else {
        // Sandbox: confirm directly.
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
      Alert.alert('Confirmation failed', e.message ?? 'Payment taken but confirmation failed. Check My Policies.');
    }
  }

  function finishBuy(done: any) {
    setResult(done);
    setBuying(false);
    load();
  }

  async function submitAdvisor() {
    if (!advisor.name || !advisor.phone) {
      Alert.alert('Missing details', 'Name and phone are required.');
      return;
    }
    const token = await getAccessToken();
    try {
      await api.insuranceAdvisorCallback({
        name: advisor.name, phone: advisor.phone, preferredTime: advisor.preferredTime,
      }, token || undefined);
      setAdvisorOpen(false);
      setAdvisor({ name: '', phone: '', preferredTime: '' });
      Alert.alert('Request received', 'Our advisor will call you shortly.');
    } catch (e: any) {
      Alert.alert('Failed', e.message ?? 'Try again.');
    }
  }

  function openCertificate(policyRef: string) {
    Linking.openURL(api.insuranceCertificateUrl(policyRef)).catch(() => {});
  }

  if (loading) {
    return <View style={styles.center}><ActivityIndicator size="large" color="#f97316" /></View>;
  }

  return (
    <>
      <ScrollView
        style={styles.container}
        contentContainerStyle={{ padding: 16, paddingBottom: 48 }}
        refreshControl={<RefreshControl refreshing={false} onRefresh={load} />}
      >
        <Stack.Screen options={{ title: 'Insurance & Loans' }} />

        <Text style={styles.kicker}>PROTECT WHAT MATTERS</Text>
        <Text style={styles.title}>Insurance & Loans</Text>
        <Text style={styles.subtitle}>Compare, buy and manage policies — powered by leading insurers.</Text>

        <TouchableOpacity style={styles.claimsLink} onPress={() => router.push('/services-insurance-claims')}>
          <Text style={styles.claimsLinkText}>Need to file a claim? Get assistance →</Text>
        </TouchableOpacity>

        {/* Insurance products */}
        <Text style={styles.sectionTitle}>Insurance</Text>
        {insurance.map((p) => {
          const compare = p.coverageType ? COMPARE_PRODUCT[p.coverageType] : null;
          const quote = quotes[p.key];
          return (
            <View key={p.key} style={styles.card}>
              <Text style={styles.cardTitle}>{p.title}</Text>
              {p.tagline ? <Text style={styles.cardTagline}>{p.tagline}</Text> : null}
              {Array.isArray(p.highlights) && p.highlights.length > 0 ? (
                <View style={{ marginTop: 6 }}>
                  {p.highlights.slice(0, 3).map((h: string, i: number) => (
                    <Text key={i} style={styles.highlight}>• {h}</Text>
                  ))}
                </View>
              ) : null}

              {compare ? (
                <TouchableOpacity
                  style={styles.primaryBtn}
                  onPress={() => router.push(`/services-insurance-compare?product=${compare}`)}
                >
                  <Text style={styles.primaryBtnText}>Compare plans</Text>
                </TouchableOpacity>
              ) : quote ? (
                <>
                  <Text style={styles.priceLine}>
                    From {formatPaise(quote.premiumPaise)}
                    {quote.sumInsuredPaise ? ` · cover ${formatPaise(quote.sumInsuredPaise)}` : ''}
                  </Text>
                  <TouchableOpacity style={styles.primaryBtn} onPress={() => openBuy(p)}>
                    <Text style={styles.primaryBtnText}>Buy now</Text>
                  </TouchableOpacity>
                </>
              ) : (
                <TouchableOpacity
                  style={[styles.secondaryBtn, quoting === p.key && { opacity: 0.6 }]}
                  disabled={quoting === p.key}
                  onPress={() => getQuote(p)}
                >
                  {quoting === p.key
                    ? <ActivityIndicator color="#f97316" />
                    : <Text style={styles.secondaryBtnText}>Get price</Text>}
                </TouchableOpacity>
              )}
            </View>
          );
        })}

        {/* Loan products */}
        {loans.length > 0 ? (
          <>
            <Text style={styles.sectionTitle}>Loans</Text>
            {loans.map((p) => (
              <TouchableOpacity
                key={p.key}
                style={styles.card}
                activeOpacity={0.85}
                onPress={() => p.applyPath && Linking.openURL(
                  p.applyPath.startsWith('http') ? p.applyPath : `https://bhramankaro.com${p.applyPath}`,
                ).catch(() => {})}
              >
                <Text style={styles.cardTitle}>{p.title}</Text>
                {p.tagline ? <Text style={styles.cardTagline}>{p.tagline}</Text> : null}
                <Text style={styles.applyLink}>Apply →</Text>
              </TouchableOpacity>
            ))}
          </>
        ) : null}

        {/* My policies */}
        {policies.length > 0 ? (
          <>
            <Text style={styles.sectionTitle}>My policies</Text>
            {policies.map((pol, i) => {
              const st = STATUS_STYLE[pol.status] ?? STATUS_STYLE.EXPIRED;
              return (
                <View key={pol.policyRef ?? i} style={styles.card}>
                  <View style={styles.policyRow}>
                    <Text style={styles.policyRef}>{pol.policyRef}</Text>
                    <View style={[styles.badge, { backgroundColor: st.bg }]}>
                      <Text style={[styles.badgeText, { color: st.text }]}>{pol.status}</Text>
                    </View>
                  </View>
                  <Text style={styles.cardTagline}>{pol.coverageType} · {formatPaise(pol.premiumPaise ?? 0)}</Text>
                  {(pol.status === 'ACTIVE' || pol.status === 'ISSUED') && pol.policyRef ? (
                    <TouchableOpacity onPress={() => openCertificate(pol.policyRef)}>
                      <Text style={styles.certLink}>View / download certificate →</Text>
                    </TouchableOpacity>
                  ) : null}
                </View>
              );
            })}
          </>
        ) : null}

        <TouchableOpacity style={styles.advisorBtn} onPress={() => setAdvisorOpen(true)}>
          <Text style={styles.advisorBtnText}>📞 Talk to an insurance advisor</Text>
        </TouchableOpacity>
      </ScrollView>

      {/* Buy modal */}
      <Modal visible={!!buyFor} transparent animationType="slide" onRequestClose={() => setBuyFor(null)}>
        <View style={styles.modalWrap}>
          <View style={styles.modalCard}>
            {result ? (
              <>
                <Text style={styles.modalTitle}>Policy issued 🎉</Text>
                <Text style={styles.modalBody}>Ref: {result.policyRef}</Text>
                <Text style={styles.modalBody}>Premium: {formatPaise(result.premiumPaise ?? 0)}</Text>
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
                <Text style={styles.modalTitle}>{buyFor?.title}</Text>
                {quotes[buyFor?.key] ? (
                  <Text style={styles.modalBody}>Premium: {formatPaise(quotes[buyFor?.key].premiumPaise)}</Text>
                ) : null}
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

      {/* Advisor modal */}
      <Modal visible={advisorOpen} transparent animationType="slide" onRequestClose={() => setAdvisorOpen(false)}>
        <View style={styles.modalWrap}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>Request a callback</Text>
            <TextInput style={styles.input} placeholder="Your name" placeholderTextColor="#9ca3af"
              value={advisor.name} onChangeText={(v) => setAdvisor((a) => ({ ...a, name: v }))} />
            <TextInput style={styles.input} placeholder="Phone" placeholderTextColor="#9ca3af" keyboardType="phone-pad"
              value={advisor.phone} onChangeText={(v) => setAdvisor((a) => ({ ...a, phone: v }))} />
            <TextInput style={styles.input} placeholder="Preferred time (optional)" placeholderTextColor="#9ca3af"
              value={advisor.preferredTime} onChangeText={(v) => setAdvisor((a) => ({ ...a, preferredTime: v }))} />
            <TouchableOpacity style={styles.primaryBtn} onPress={submitAdvisor}>
              <Text style={styles.primaryBtnText}>Request callback</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.linkBtn} onPress={() => setAdvisorOpen(false)}>
              <Text style={styles.linkBtnText}>Close</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#f9fafb' },
  center:       { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb' },
  kicker:       { fontSize: 11, fontWeight: '700', letterSpacing: 2, color: '#0ea5e9' },
  title:        { fontSize: 24, fontWeight: '800', color: '#111827', marginTop: 4 },
  subtitle:     { fontSize: 13, color: '#6b7280', marginTop: 4 },
  claimsLink:   { marginTop: 12, marginBottom: 4 },
  claimsLinkText: { fontSize: 13, fontWeight: '700', color: '#0ea5e9' },
  sectionTitle: { fontSize: 17, fontWeight: '800', color: '#111827', marginTop: 22, marginBottom: 10 },
  card:         { backgroundColor: '#fff', borderRadius: 14, padding: 16, marginBottom: 12, borderWidth: 1, borderColor: '#f3f4f6' },
  cardTitle:    { fontSize: 16, fontWeight: '800', color: '#111827' },
  cardTagline:  { fontSize: 12, color: '#6b7280', marginTop: 3 },
  highlight:    { fontSize: 12, color: '#374151', marginTop: 2 },
  priceLine:    { fontSize: 14, fontWeight: '700', color: '#0f766e', marginTop: 10 },
  primaryBtn:   { backgroundColor: '#0ea5e9', borderRadius: 12, paddingVertical: 12, alignItems: 'center', marginTop: 12 },
  primaryBtnText: { color: '#fff', fontWeight: '700', fontSize: 14 },
  secondaryBtn: { borderWidth: 1.5, borderColor: '#0ea5e9', borderRadius: 12, paddingVertical: 11, alignItems: 'center', marginTop: 12 },
  secondaryBtnText: { color: '#0ea5e9', fontWeight: '700', fontSize: 14 },
  applyLink:    { fontSize: 13, fontWeight: '700', color: '#0ea5e9', marginTop: 10 },
  policyRow:    { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  policyRef:    { fontSize: 14, fontWeight: '700', color: '#111827' },
  badge:        { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 100 },
  badgeText:    { fontSize: 10, fontWeight: '700' },
  certLink:     { fontSize: 13, fontWeight: '700', color: '#0ea5e9', marginTop: 8 },
  advisorBtn:   { marginTop: 24, backgroundColor: '#f0f9ff', borderRadius: 12, paddingVertical: 14, alignItems: 'center', borderWidth: 1, borderColor: '#bae6fd' },
  advisorBtnText: { color: '#0369a1', fontWeight: '700', fontSize: 14 },
  modalWrap:    { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  modalCard:    { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, paddingBottom: 32 },
  modalTitle:   { fontSize: 18, fontWeight: '800', color: '#111827', marginBottom: 10 },
  modalBody:    { fontSize: 14, color: '#374151', marginBottom: 6 },
  input:        { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: '#111827', backgroundColor: '#f9fafb', marginTop: 8 },
  linkBtn:      { alignItems: 'center', paddingVertical: 12, marginTop: 4 },
  linkBtnText:  { fontSize: 14, color: '#6b7280', fontWeight: '600' },
});
