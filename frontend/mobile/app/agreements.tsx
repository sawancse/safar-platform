import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, ScrollView, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator, Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';

type Tab = 'new' | 'mine';

const AGREEMENT_TYPES = [
  { value: 'SALE_AGREEMENT', label: 'Sale Agreement' },
  { value: 'SALE_DEED', label: 'Sale Deed' },
  { value: 'RENTAL_AGREEMENT', label: 'Rental Agreement' },
  { value: 'LEAVE_LICENSE', label: 'Leave & License' },
  { value: 'PG_AGREEMENT', label: 'PG Agreement' },
];

const PACKAGES = [
  { value: 'BASIC', label: 'Basic', pricePaise: 0 },
  { value: 'ESTAMP', label: 'E-Stamp', pricePaise: 149900 },
  { value: 'REGISTERED', label: 'Registered', pricePaise: 499900, popular: true },
  { value: 'PREMIUM', label: 'Premium', pricePaise: 999900 },
];

const STATES = ['Maharashtra', 'Karnataka', 'Telangana', 'Tamil Nadu', 'Delhi', 'Gujarat', 'Rajasthan', 'Uttar Pradesh', 'West Bengal', 'Kerala'];

const STATUS_STYLE: Record<string, { bg: string; text: string }> = {
  DRAFT: { bg: '#f3f4f6', text: '#374151' },
  PENDING_PAYMENT: { bg: '#fef9c3', text: '#854d0e' },
  PAID: { bg: '#dbeafe', text: '#1e40af' },
  STAMPED: { bg: '#e0e7ff', text: '#3730a3' },
  SIGNED: { bg: '#dcfce7', text: '#14532d' },
  REGISTERED: { bg: '#dcfce7', text: '#14532d' },
  DELIVERED: { bg: '#dcfce7', text: '#14532d' },
  CANCELLED: { bg: '#fee2e2', text: '#7f1d1d' },
};

function pretty(s?: string) {
  if (!s) return '';
  return s.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
}

export default function AgreementsScreen() {
  const router = useRouter();
  const [tab, setTab] = useState<Tab>('new');

  // New agreement form
  const [agreementType, setAgreementType] = useState('SALE_AGREEMENT');
  const [packageType, setPackageType] = useState('REGISTERED');
  const [sellerName, setSellerName] = useState('');
  const [buyerName, setBuyerName] = useState('');
  const [propertyAddress, setPropertyAddress] = useState('');
  const [state, setState] = useState('Maharashtra');
  const [propertyValue, setPropertyValue] = useState('');
  const [stampDuty, setStampDuty] = useState<any>(null);
  const [calculating, setCalculating] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // My agreements
  const [agreements, setAgreements] = useState<any[]>([]);
  const [loadingMine, setLoadingMine] = useState(false);
  const [authed, setAuthed] = useState(false);

  const loadMine = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) { setAuthed(false); setLoadingMine(false); return; }
    setAuthed(true);
    setLoadingMine(true);
    try { setAgreements(await api.getMyAgreements(token) ?? []); }
    catch { setAgreements([]); }
    finally { setLoadingMine(false); }
  }, []);

  useEffect(() => { if (tab === 'mine') loadMine(); }, [tab, loadMine]);

  async function calcStampDuty() {
    const val = Number(propertyValue);
    if (!val) { Alert.alert('Enter value', 'Enter the property value first.'); return; }
    setCalculating(true);
    try {
      const res = await api.calculateStampDuty(state, agreementType, val * 100);
      setStampDuty(res);
    } catch (e: any) {
      Alert.alert('Could not calculate', e.message ?? 'Try again.');
    } finally {
      setCalculating(false);
    }
  }

  async function submit() {
    if (!sellerName.trim() || !buyerName.trim() || !propertyAddress.trim()) {
      Alert.alert('Missing', 'Fill both party names and the property address.'); return;
    }
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    setSubmitting(true);
    try {
      const isRental = agreementType === 'RENTAL_AGREEMENT' || agreementType === 'LEAVE_LICENSE' || agreementType === 'PG_AGREEMENT';
      const parties = [
        { name: sellerName, role: isRental ? 'LANDLORD' : 'SELLER' },
        { name: buyerName, role: isRental ? 'TENANT' : 'BUYER' },
      ];
      const res = await api.createAgreement({
        agreementType, packageType,
        partyDetailsJson: JSON.stringify(parties),
        propertyDetailsJson: JSON.stringify({ propertyAddress, state, propertyValuePaise: (Number(propertyValue) || 0) * 100 }),
        clausesJson: JSON.stringify({ clauses: ['disputeResolution', 'encumbrance', 'titleWarranty'] }),
      }, token);
      Alert.alert('Agreement created', 'Track it under My Agreements.');
      if (res?.id) router.push(`/agreement/${res.id}` as any);
      else setTab('mine');
    } catch (e: any) {
      Alert.alert('Failed', e.message ?? 'Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  const pkg = PACKAGES.find((p) => p.value === packageType)!;
  const total = (pkg.pricePaise) + (stampDuty?.stampDutyPaise ?? 0) + (stampDuty?.registrationFeePaise ?? 0);

  return (
    <View style={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.heroTitle}>Property Agreements</Text>
        <Text style={styles.heroSub}>Draft, e-stamp, e-sign and register agreements — fully online.</Text>
      </View>

      <View style={styles.tabRow}>
        <TouchableOpacity style={[styles.tabBtn, tab === 'new' && styles.tabBtnActive]} onPress={() => setTab('new')}>
          <Text style={[styles.tabText, tab === 'new' && styles.tabTextActive]}>New Agreement</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[styles.tabBtn, tab === 'mine' && styles.tabBtnActive]} onPress={() => setTab('mine')}>
          <Text style={[styles.tabText, tab === 'mine' && styles.tabTextActive]}>My Agreements</Text>
        </TouchableOpacity>
      </View>

      {tab === 'new' ? (
        <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
          <Text style={styles.label}>Agreement type</Text>
          <View style={styles.pillRow}>
            {AGREEMENT_TYPES.map((t) => (
              <TouchableOpacity key={t.value} style={[styles.pill, agreementType === t.value && styles.pillActive]} onPress={() => { setAgreementType(t.value); setStampDuty(null); }}>
                <Text style={[styles.pillText, agreementType === t.value && styles.pillTextActive]}>{t.label}</Text>
              </TouchableOpacity>
            ))}
          </View>

          <Text style={styles.label}>Package</Text>
          {PACKAGES.map((p) => (
            <TouchableOpacity key={p.value} style={[styles.pkgCard, packageType === p.value && styles.pkgCardActive]} onPress={() => setPackageType(p.value)}>
              <View style={{ flex: 1 }}>
                <Text style={styles.pkgName}>{p.label}{p.popular ? '  ⭐' : ''}</Text>
              </View>
              <Text style={styles.pkgPrice}>{p.pricePaise === 0 ? 'Free' : formatPaise(p.pricePaise)}</Text>
            </TouchableOpacity>
          ))}

          <Text style={[styles.label, { marginTop: 16 }]}>Parties</Text>
          <Field label="First party (seller/landlord) *"><TextInput style={styles.input} value={sellerName} onChangeText={setSellerName} placeholder="Full name" placeholderTextColor="#9ca3af" /></Field>
          <Field label="Second party (buyer/tenant) *"><TextInput style={styles.input} value={buyerName} onChangeText={setBuyerName} placeholder="Full name" placeholderTextColor="#9ca3af" /></Field>

          <Text style={[styles.label, { marginTop: 8 }]}>Property</Text>
          <Field label="Property address *"><TextInput style={[styles.input, styles.textarea]} value={propertyAddress} onChangeText={setPropertyAddress} placeholder="Full address" placeholderTextColor="#9ca3af" multiline /></Field>
          <Text style={styles.label}>State</Text>
          <View style={styles.pillRow}>
            {STATES.map((s) => (
              <TouchableOpacity key={s} style={[styles.pill, state === s && styles.pillActive]} onPress={() => { setState(s); setStampDuty(null); }}>
                <Text style={[styles.pillText, state === s && styles.pillTextActive]}>{s}</Text>
              </TouchableOpacity>
            ))}
          </View>
          <Field label="Property value (₹)"><TextInput style={styles.input} value={propertyValue} onChangeText={(t) => { setPropertyValue(t); setStampDuty(null); }} keyboardType="numeric" placeholder="e.g. 5000000" placeholderTextColor="#9ca3af" /></Field>

          <TouchableOpacity style={styles.calcBtn} onPress={calcStampDuty} disabled={calculating}>
            <Text style={styles.calcBtnText}>{calculating ? 'Calculating…' : 'Calculate stamp duty'}</Text>
          </TouchableOpacity>

          {stampDuty ? (
            <View style={styles.feeBox}>
              <FeeRow label="Package" value={formatPaise(pkg.pricePaise)} />
              <FeeRow label="Stamp duty" value={formatPaise(stampDuty.stampDutyPaise ?? 0)} />
              <FeeRow label="Registration" value={formatPaise(stampDuty.registrationFeePaise ?? 0)} />
              <View style={styles.feeDivider} />
              <FeeRow label="Total" value={formatPaise(total)} bold />
            </View>
          ) : null}

          <TouchableOpacity style={[styles.primaryBtn, submitting && { opacity: 0.6 }]} disabled={submitting} onPress={submit}>
            {submitting ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryBtnText}>Create Agreement</Text>}
          </TouchableOpacity>
          <View style={{ height: 40 }} />
        </ScrollView>
      ) : (
        <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
          {!authed ? (
            <View style={styles.gate}>
              <Text style={styles.gateText}>Sign in to view your agreements.</Text>
              <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/auth')}><Text style={styles.primaryBtnText}>Sign In</Text></TouchableOpacity>
            </View>
          ) : loadingMine ? (
            <ActivityIndicator color="#f97316" style={{ marginTop: 24 }} size="large" />
          ) : agreements.length === 0 ? (
            <View style={styles.gate}><Text style={styles.gateText}>No agreements yet. Create one from the New tab.</Text></View>
          ) : (
            agreements.map((a) => {
              const s = STATUS_STYLE[a.status] ?? STATUS_STYLE.DRAFT;
              return (
                <TouchableOpacity key={a.id} style={styles.itemCard} onPress={() => router.push(`/agreement/${a.id}` as any)}>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.itemTitle}>{pretty(a.agreementType)}</Text>
                    <Text style={styles.itemSub} numberOfLines={1}>{a.propertyAddress ?? a.city ?? 'Property agreement'}</Text>
                    <Text style={styles.itemDate}>{a.createdAt ? new Date(a.createdAt).toLocaleDateString('en-IN') : ''}</Text>
                  </View>
                  <View style={[styles.badge, { backgroundColor: s.bg }]}><Text style={[styles.badgeText, { color: s.text }]}>{pretty(a.status)}</Text></View>
                </TouchableOpacity>
              );
            })
          )}
          <View style={{ height: 40 }} />
        </ScrollView>
      )}
    </View>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return <View style={styles.field}><Text style={styles.label}>{label}</Text>{children}</View>;
}
function FeeRow({ label, value, bold }: { label: string; value: string; bold?: boolean }) {
  return <View style={styles.feeRow}><Text style={[styles.feeLabel, bold && styles.feeBold]}>{label}</Text><Text style={[styles.feeVal, bold && styles.feeBold]}>{value}</Text></View>;
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#f9fafb' },
  hero:         { backgroundColor: '#1e3a8a', paddingHorizontal: 20, paddingTop: 18, paddingBottom: 20 },
  heroTitle:    { fontSize: 22, fontWeight: '800', color: '#fff' },
  heroSub:      { fontSize: 13, color: '#dbeafe', marginTop: 6, lineHeight: 18 },
  tabRow:       { flexDirection: 'row', backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#e5e7eb' },
  tabBtn:       { flex: 1, paddingVertical: 13, alignItems: 'center' },
  tabBtnActive: { borderBottomWidth: 3, borderBottomColor: '#f97316' },
  tabText:      { fontSize: 14, fontWeight: '600', color: '#6b7280' },
  tabTextActive:{ color: '#f97316' },
  scroll:       { padding: 16 },
  label:        { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 6 },
  field:        { marginBottom: 14 },
  input:        { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: '#111827' },
  textarea:     { height: 72, textAlignVertical: 'top' },
  pillRow:      { flexDirection: 'row', gap: 8, marginBottom: 14, flexWrap: 'wrap' },
  pill:         { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb', backgroundColor: '#fff' },
  pillActive:   { backgroundColor: '#f97316', borderColor: '#f97316' },
  pillText:     { fontSize: 12, fontWeight: '600', color: '#374151' },
  pillTextActive:{ color: '#fff' },
  pkgCard:      { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderRadius: 12, padding: 14, marginBottom: 8, borderWidth: 1, borderColor: '#e5e7eb' },
  pkgCardActive:{ borderColor: '#f97316', backgroundColor: '#fff7ed' },
  pkgName:      { fontSize: 14, fontWeight: '700', color: '#111827' },
  pkgPrice:     { fontSize: 14, fontWeight: '800', color: '#f97316' },
  calcBtn:      { borderWidth: 1, borderColor: '#1e3a8a', borderRadius: 10, paddingVertical: 12, alignItems: 'center', marginBottom: 14 },
  calcBtnText:  { fontSize: 14, fontWeight: '700', color: '#1e3a8a' },
  feeBox:       { backgroundColor: '#fff', borderRadius: 12, padding: 14, marginBottom: 16, borderWidth: 1, borderColor: '#f3f4f6' },
  feeRow:       { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 6 },
  feeLabel:     { fontSize: 13, color: '#6b7280' },
  feeVal:       { fontSize: 13, color: '#374151', fontWeight: '600' },
  feeBold:      { fontWeight: '800', color: '#111827', fontSize: 15 },
  feeDivider:   { height: 1, backgroundColor: '#f3f4f6', marginVertical: 8 },
  primaryBtn:   { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 15, alignItems: 'center', marginTop: 4 },
  primaryBtnText:{ color: '#fff', fontSize: 16, fontWeight: '700' },
  gate:         { backgroundColor: '#fff', borderRadius: 14, padding: 24, alignItems: 'center', marginTop: 16, borderWidth: 1, borderColor: '#f3f4f6' },
  gateText:     { fontSize: 14, color: '#374151', marginBottom: 12, textAlign: 'center' },
  itemCard:     { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderRadius: 14, padding: 14, marginBottom: 10, borderWidth: 1, borderColor: '#f3f4f6' },
  itemTitle:    { fontSize: 14, fontWeight: '700', color: '#111827' },
  itemSub:      { fontSize: 12, color: '#6b7280', marginTop: 2 },
  itemDate:     { fontSize: 11, color: '#9ca3af', marginTop: 4 },
  badge:        { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 100, marginLeft: 8 },
  badgeText:    { fontSize: 11, fontWeight: '700' },
});
