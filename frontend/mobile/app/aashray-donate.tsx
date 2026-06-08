import { useEffect, useState } from 'react';
import {
  View, Text, ScrollView, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator, Alert, Modal, Switch, Linking,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';
import CookPaymentWebView from '@/components/CookPaymentWebView';
import type { PaymentOrder } from '@/lib/payment';

const PRESETS = [
  { amount: 500, label: '1 night of shelter' },
  { amount: 1000, label: '2 nights of shelter' },
  { amount: 2500, label: '1 week of shelter', popular: true },
  { amount: 5000, label: '2 weeks of shelter' },
  { amount: 10000, label: '1 month of shelter' },
  { amount: 25000, label: '3 months of shelter' },
];

const IMPACT_TIERS = [
  { amount: 500, label: '1 night of safe shelter', icon: '🛏️' },
  { amount: 2500, label: '1 week in a safe home', icon: '🏠' },
  { amount: 10000, label: '1 full month of housing', icon: '🏡' },
  { amount: 25000, label: '3 months — a fresh start', icon: '💛' },
];

const FUND_USAGE = [
  { pct: 70, label: 'Rent & Deposits', desc: 'Paid directly to Aashray hosts', color: '#14b8a6' },
  { pct: 15, label: 'Essential Supplies', desc: 'Bedding, kitchenware, furnishings', color: '#3b82f6' },
  { pct: 10, label: 'Case Worker Support', desc: 'NGO coordination & matching', color: '#f59e0b' },
  { pct: 5, label: 'Platform Operations', desc: 'Tech, verification, processing', color: '#9ca3af' },
];

const DONOR_TIERS = [
  { min: 500, name: 'Shelter Friend', badge: '🤝' },
  { min: 2000, name: 'Shelter Builder', badge: '🔨' },
  { min: 5000, name: 'Shelter Champion', badge: '🏆' },
  { min: 15000, name: 'Shelter Patron', badge: '👑' },
];

function donorTier(amount: number) {
  let t = null;
  for (const tier of DONOR_TIERS) if (amount >= tier.min) t = tier;
  return t;
}

export default function AashrayDonateScreen() {
  const router = useRouter();
  const [amount, setAmount] = useState(2500);
  const [customMode, setCustomMode] = useState(false);
  const [customText, setCustomText] = useState('');
  const [monthly, setMonthly] = useState(false);

  const [donorName, setDonorName] = useState('');
  const [donorEmail, setDonorEmail] = useState('');
  const [donorPan, setDonorPan] = useState('');
  const [dedicate, setDedicate] = useState(false);
  const [dedicatedTo, setDedicatedTo] = useState('');

  const [stats, setStats] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [order, setOrder] = useState<PaymentOrder | null>(null);
  const [result, setResult] = useState<{ receiptNumber?: string; donationRef?: string } | null>(null);

  useEffect(() => { api.getDonationStats().then(setStats).catch(() => {}); }, []);

  const effectiveAmount = customMode ? (Number(customText) || 0) : amount;
  const taxSaving = Math.round(effectiveAmount * 0.5);
  const tier = donorTier(effectiveAmount);

  async function handleDonate() {
    if (effectiveAmount < 1) { Alert.alert('Enter an amount', 'Minimum donation is ₹1.'); return; }
    const token = await getAccessToken();
    setLoading(true);
    try {
      const res = await api.createDonation({
        amountPaise: effectiveAmount * 100,
        frequency: monthly ? 'MONTHLY' : 'ONE_TIME',
        donorName: donorName || undefined,
        donorEmail: donorEmail || undefined,
        donorPan: donorPan || undefined,
        dedicatedTo: dedicate ? (dedicatedTo || undefined) : undefined,
      }, token ?? undefined);
      if (!res.razorpayOrderId || !res.razorpayKeyId) {
        Alert.alert('Could not start payment', 'Donation gateway is not configured yet.');
        return;
      }
      setOrder({
        orderId: res.razorpayOrderId,
        amount: res.amountPaise ?? effectiveAmount * 100,
        currency: res.currency ?? 'INR',
        bookingId: res.donationRef ?? '',
        razorpayKeyId: res.razorpayKeyId,
      });
    } catch (e: any) {
      Alert.alert('Could not start payment', e.message ?? 'Please try again.');
    } finally {
      setLoading(false);
    }
  }

  async function onPaymentSuccess(res: { paymentId: string; orderId: string; signature: string }) {
    setOrder(null);
    try {
      const verified = await api.verifyDonation({
        razorpayOrderId: res.orderId, razorpayPaymentId: res.paymentId, razorpaySignature: res.signature,
      });
      setResult({ receiptNumber: verified?.receiptNumber, donationRef: verified?.donationRef });
    } catch {
      setResult({});
    }
  }

  function shareWhatsApp() {
    const msg = `I just donated ${formatPaise(effectiveAmount * 100)} to Safar Aashray to help house displaced families. Join me 💛`;
    Linking.openURL(`https://wa.me/?text=${encodeURIComponent(msg)}`).catch(() => {});
  }

  // ── Razorpay WebView ──
  if (order) {
    return (
      <Modal visible animationType="slide">
        <CookPaymentWebView
          order={order}
          prefill={{ name: donorName, email: donorEmail, phone: '' }}
          onSuccess={onPaymentSuccess}
          onFailure={(err) => { Alert.alert('Payment failed', err); setOrder(null); }}
          onDismiss={() => setOrder(null)}
        />
      </Modal>
    );
  }

  // ── Success screen ──
  if (result) {
    return (
      <View style={styles.successWrap}>
        <Text style={styles.successIcon}>🙏</Text>
        <Text style={styles.successTitle}>Thank You{donorName ? `, ${donorName}` : ''}!</Text>
        {tier ? <Text style={styles.successTier}>{tier.badge} {tier.name}</Text> : null}
        <Text style={styles.successAmount}>{formatPaise(effectiveAmount * 100)}{monthly ? ' / month' : ''}</Text>
        {result.receiptNumber ? <Text style={styles.successReceipt}>80G Receipt: {result.receiptNumber}</Text> : null}
        <Text style={styles.successMsg}>A confirmation and your 80G receipt will be emailed to you. Your gift directly funds safe housing.</Text>
        <TouchableOpacity style={styles.primaryBtn} onPress={shareWhatsApp}>
          <Text style={styles.primaryBtnText}>Share on WhatsApp</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => router.replace('/aashray')}><Text style={styles.link}>Back to Aashray</Text></TouchableOpacity>
      </View>
    );
  }

  const raisedPct = stats?.goalPaise ? Math.min(100, Math.round((stats.totalRaisedPaise / stats.goalPaise) * 100)) : 0;

  return (
    <View style={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.heroTitle}>Give the gift of shelter</Text>
        <Text style={styles.heroSub}>Your donation directly funds safe housing for displaced families across India.</Text>
        <View style={styles.trustRow}>
          <Text style={styles.trustChip}>🛡️ 80G Certified</Text>
          <Text style={styles.trustChip}>✓ NGO Verified</Text>
          <Text style={styles.trustChip}>🔒 Razorpay Secure</Text>
        </View>
      </View>

      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        {stats ? (
          <View style={styles.statsCard}>
            <View style={styles.progressBg}><View style={[styles.progressFill, { width: `${raisedPct}%` }]} /></View>
            <Text style={styles.progressLabel}>{formatPaise(stats.totalRaisedPaise ?? 0)} raised{stats.goalPaise ? ` of ${formatPaise(stats.goalPaise)}` : ''}</Text>
            <View style={styles.statsRow}>
              <View style={styles.statCol}><Text style={styles.statNum}>{stats.totalDonors ?? 0}</Text><Text style={styles.statCap}>Donors</Text></View>
              <View style={styles.statCol}><Text style={styles.statNum}>{stats.familiesHoused ?? 0}</Text><Text style={styles.statCap}>Families housed</Text></View>
              <View style={styles.statCol}><Text style={styles.statNum}>{stats.monthlyDonors ?? 0}</Text><Text style={styles.statCap}>Monthly donors</Text></View>
            </View>
          </View>
        ) : null}

        {/* Frequency toggle */}
        <View style={styles.freqRow}>
          <TouchableOpacity style={[styles.freqBtn, !monthly && styles.freqBtnActive]} onPress={() => setMonthly(false)}>
            <Text style={[styles.freqText, !monthly && styles.freqTextActive]}>One-time</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.freqBtn, monthly && styles.freqBtnActive]} onPress={() => setMonthly(true)}>
            <Text style={[styles.freqText, monthly && styles.freqTextActive]}>Monthly (SIP)</Text>
          </TouchableOpacity>
        </View>

        {/* Presets */}
        <View style={styles.presetGrid}>
          {PRESETS.map((p) => {
            const on = !customMode && amount === p.amount;
            return (
              <TouchableOpacity key={p.amount} style={[styles.presetCard, on && styles.presetCardActive]} onPress={() => { setCustomMode(false); setAmount(p.amount); }}>
                {p.popular ? <View style={styles.popBadge}><Text style={styles.popBadgeText}>Popular</Text></View> : null}
                <Text style={[styles.presetAmt, on && styles.presetAmtActive]}>₹{p.amount.toLocaleString('en-IN')}</Text>
                <Text style={styles.presetLabel}>{p.label}</Text>
              </TouchableOpacity>
            );
          })}
        </View>
        <TouchableOpacity onPress={() => setCustomMode(true)}>
          <Text style={styles.customLink}>{customMode ? 'Custom amount:' : 'Enter a custom amount'}</Text>
        </TouchableOpacity>
        {customMode ? (
          <TextInput style={styles.input} value={customText} onChangeText={setCustomText} keyboardType="numeric" placeholder="₹ amount" placeholderTextColor="#9ca3af" />
        ) : null}

        {/* Tax box */}
        {effectiveAmount > 0 ? (
          <View style={styles.taxBox}>
            <Text style={styles.taxText}>You save <Text style={styles.taxBold}>{formatPaise(taxSaving * 100)}</Text> under Section 80G (50% deduction)</Text>
            <Text style={styles.taxSub}>Effective cost: {formatPaise((effectiveAmount - taxSaving) * 100)}</Text>
          </View>
        ) : null}
        {tier ? (
          <View style={styles.tierBox}><Text style={styles.tierText}>{tier.badge} You'll be a {tier.name}</Text></View>
        ) : null}

        {/* Donor info */}
        <Text style={styles.sectionTitle}>Your details</Text>
        <Field label="Name (for 80G receipt)"><TextInput style={styles.input} value={donorName} onChangeText={setDonorName} placeholder="Full name" placeholderTextColor="#9ca3af" /></Field>
        <Field label="Email (for receipt)"><TextInput style={styles.input} value={donorEmail} onChangeText={setDonorEmail} placeholder="you@email.com" placeholderTextColor="#9ca3af" autoCapitalize="none" keyboardType="email-address" /></Field>
        <Field label="PAN (optional, for 80G certificate)"><TextInput style={styles.input} value={donorPan} onChangeText={(t) => setDonorPan(t.toUpperCase())} placeholder="ABCDE1234F" placeholderTextColor="#9ca3af" maxLength={10} autoCapitalize="characters" /></Field>
        <View style={styles.dedRow}>
          <Text style={styles.label}>Dedicate this donation</Text>
          <Switch value={dedicate} onValueChange={setDedicate} trackColor={{ false: '#d1d5db', true: '#fed7aa' }} thumbColor={dedicate ? '#f97316' : '#9ca3af'} />
        </View>
        {dedicate ? <TextInput style={styles.input} value={dedicatedTo} onChangeText={setDedicatedTo} placeholder="In honor of…" placeholderTextColor="#9ca3af" /> : null}

        {/* Impact checklist */}
        <Text style={styles.sectionTitle}>Your impact</Text>
        {IMPACT_TIERS.map((t) => {
          const reached = effectiveAmount >= t.amount;
          return (
            <View key={t.amount} style={styles.impactRow}>
              <Text style={styles.impactIcon}>{reached ? '✅' : t.icon}</Text>
              <Text style={[styles.impactLabel, reached && styles.impactLabelOn]}>{t.label}</Text>
            </View>
          );
        })}

        {/* Where money goes */}
        <Text style={styles.sectionTitle}>Where your money goes</Text>
        <View style={styles.fundBar}>
          {FUND_USAGE.map((f) => <View key={f.label} style={{ flex: f.pct, height: 10, backgroundColor: f.color }} />)}
        </View>
        {FUND_USAGE.map((f) => (
          <View key={f.label} style={styles.fundRow}>
            <View style={[styles.fundDot, { backgroundColor: f.color }]} />
            <Text style={styles.fundLabel}>{f.pct}% — {f.label}</Text>
            {effectiveAmount > 0 ? <Text style={styles.fundAmt}>{formatPaise(Math.round(effectiveAmount * f.pct / 100) * 100)}</Text> : null}
          </View>
        ))}

        {/* Donate button */}
        <TouchableOpacity style={[styles.primaryBtn, (loading || effectiveAmount < 1) && { opacity: 0.6 }]} disabled={loading || effectiveAmount < 1} onPress={handleDonate}>
          {loading ? <ActivityIndicator color="#fff" /> : (
            <Text style={styles.primaryBtnText}>Donate {formatPaise(effectiveAmount * 100)}{monthly ? ' / month' : ''}</Text>
          )}
        </TouchableOpacity>
        <Text style={styles.payHint}>UPI · Cards · Net Banking · Wallets — secured by Razorpay</Text>

        <View style={{ height: 40 }} />
      </ScrollView>
    </View>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return <View style={styles.field}><Text style={styles.label}>{label}</Text>{children}</View>;
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#f9fafb' },
  hero:         { backgroundColor: '#f97316', paddingHorizontal: 20, paddingTop: 18, paddingBottom: 22 },
  heroTitle:    { fontSize: 24, fontWeight: '800', color: '#fff' },
  heroSub:      { fontSize: 13, color: '#ffedd5', marginTop: 8, lineHeight: 19 },
  trustRow:     { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 14 },
  trustChip:    { fontSize: 11, fontWeight: '700', color: '#fff', backgroundColor: 'rgba(255,255,255,0.2)', paddingHorizontal: 10, paddingVertical: 5, borderRadius: 100, overflow: 'hidden' },
  scroll:       { padding: 16 },
  statsCard:    { backgroundColor: '#fff', borderRadius: 16, padding: 16, borderWidth: 1, borderColor: '#f3f4f6', marginBottom: 16 },
  progressBg:   { height: 10, backgroundColor: '#f3f4f6', borderRadius: 100, overflow: 'hidden' },
  progressFill: { height: 10, backgroundColor: '#f97316', borderRadius: 100 },
  progressLabel:{ fontSize: 12, color: '#374151', marginTop: 8, fontWeight: '600' },
  statsRow:     { flexDirection: 'row', justifyContent: 'space-between', marginTop: 14 },
  statCol:      { alignItems: 'center', flex: 1 },
  statNum:      { fontSize: 18, fontWeight: '800', color: '#f97316' },
  statCap:      { fontSize: 10, color: '#6b7280', marginTop: 2, textAlign: 'center' },
  freqRow:      { flexDirection: 'row', gap: 8, backgroundColor: '#f3f4f6', borderRadius: 100, padding: 4, marginBottom: 16 },
  freqBtn:      { flex: 1, paddingVertical: 9, borderRadius: 100, alignItems: 'center' },
  freqBtnActive:{ backgroundColor: '#fff' },
  freqText:     { fontSize: 13, fontWeight: '600', color: '#6b7280' },
  freqTextActive:{ color: '#f97316' },
  presetGrid:   { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  presetCard:   { width: '47.5%' as any, backgroundColor: '#fff', borderRadius: 14, padding: 14, borderWidth: 1, borderColor: '#e5e7eb' },
  presetCardActive: { borderColor: '#f97316', backgroundColor: '#fff7ed' },
  popBadge:     { position: 'absolute', top: 8, right: 8, backgroundColor: '#fef3c7', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 100 },
  popBadgeText: { fontSize: 8, fontWeight: '800', color: '#92400e' },
  presetAmt:    { fontSize: 18, fontWeight: '800', color: '#111827' },
  presetAmtActive: { color: '#f97316' },
  presetLabel:  { fontSize: 11, color: '#6b7280', marginTop: 2 },
  customLink:   { fontSize: 13, fontWeight: '600', color: '#f97316', marginTop: 12, marginBottom: 6 },
  taxBox:       { backgroundColor: '#f0fdf4', borderRadius: 12, padding: 14, marginTop: 12, borderWidth: 1, borderColor: '#bbf7d0' },
  taxText:      { fontSize: 13, color: '#166534' },
  taxBold:      { fontWeight: '800' },
  taxSub:       { fontSize: 12, color: '#16a34a', marginTop: 4 },
  tierBox:      { backgroundColor: '#fffbeb', borderRadius: 12, padding: 12, marginTop: 10, borderWidth: 1, borderColor: '#fde68a' },
  tierText:     { fontSize: 13, fontWeight: '700', color: '#92400e' },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: '#111827', marginTop: 22, marginBottom: 10 },
  field:        { marginBottom: 14 },
  label:        { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 6 },
  input:        { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: '#111827' },
  dedRow:       { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 },
  impactRow:    { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 8 },
  impactIcon:   { fontSize: 18, width: 24 },
  impactLabel:  { fontSize: 13, color: '#9ca3af' },
  impactLabelOn:{ color: '#111827', fontWeight: '600' },
  fundBar:      { flexDirection: 'row', borderRadius: 100, overflow: 'hidden', marginBottom: 12 },
  fundRow:      { flexDirection: 'row', alignItems: 'center', marginBottom: 8 },
  fundDot:      { width: 10, height: 10, borderRadius: 5, marginRight: 8 },
  fundLabel:    { fontSize: 13, color: '#374151', flex: 1 },
  fundAmt:      { fontSize: 13, fontWeight: '700', color: '#111827' },
  primaryBtn:   { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 15, alignItems: 'center', marginTop: 18 },
  primaryBtnText:{ color: '#fff', fontSize: 16, fontWeight: '700' },
  payHint:      { fontSize: 11, color: '#9ca3af', textAlign: 'center', marginTop: 10 },
  successWrap:  { flex: 1, backgroundColor: '#f9fafb', alignItems: 'center', justifyContent: 'center', padding: 28 },
  successIcon:  { fontSize: 60 },
  successTitle: { fontSize: 24, fontWeight: '800', color: '#111827', marginTop: 12 },
  successTier:  { fontSize: 15, fontWeight: '700', color: '#92400e', marginTop: 6 },
  successAmount:{ fontSize: 20, fontWeight: '800', color: '#f97316', marginTop: 8 },
  successReceipt:{ fontSize: 13, color: '#6b7280', marginTop: 6, fontWeight: '600' },
  successMsg:   { fontSize: 13, color: '#6b7280', textAlign: 'center', marginTop: 12, lineHeight: 20 },
  link:         { color: '#f97316', fontSize: 14, fontWeight: '600', marginTop: 16 },
});
