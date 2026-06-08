import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, ScrollView, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator, Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';

type Tab = 'browse' | 'mine';

const DEFAULT_PACKAGES = [
  { id: 'TITLE_SEARCH', name: 'Title Search', pricePaise: 299900, turnaround: '3-5 days', features: ['Title chain verification', 'Encumbrance check'] },
  { id: 'BASIC_VERIFICATION', name: 'Basic Verification', pricePaise: 499900, turnaround: '5-7 days', features: ['Title chain', 'Encumbrance', 'Tax dues', 'Litigation check'] },
  { id: 'COMPREHENSIVE', name: 'Comprehensive', pricePaise: 999900, turnaround: '5-8 days', popular: true, features: ['Everything in Basic', 'Govt approvals', 'Survey match', 'Risk report'] },
  { id: 'PREMIUM', name: 'Premium + Advocate', pricePaise: 1999900, turnaround: '7-10 days', features: ['Everything in Comprehensive', 'Advocate consultation', 'Legal opinion'] },
];

const STATES = ['Andhra Pradesh', 'Karnataka', 'Kerala', 'Maharashtra', 'Tamil Nadu', 'Telangana', 'Delhi', 'Gujarat', 'Rajasthan', 'Uttar Pradesh', 'West Bengal'];

const STEPS = ['Upload Documents', 'Advocate Review', 'Verification', 'Report Delivery'];

const STATUS_STYLE: Record<string, { bg: string; text: string }> = {
  CREATED: { bg: '#f3f4f6', text: '#374151' },
  DOCUMENTS_UPLOADED: { bg: '#dbeafe', text: '#1e40af' },
  UNDER_REVIEW: { bg: '#e0e7ff', text: '#3730a3' },
  VERIFICATION_IN_PROGRESS: { bg: '#fef9c3', text: '#854d0e' },
  REPORT_READY: { bg: '#dcfce7', text: '#14532d' },
  COMPLETED: { bg: '#dcfce7', text: '#14532d' },
  CANCELLED: { bg: '#fee2e2', text: '#7f1d1d' },
};
const RISK_STYLE: Record<string, { label: string; bg: string; text: string }> = {
  GREEN: { label: 'Low Risk', bg: '#dcfce7', text: '#14532d' },
  YELLOW: { label: 'Medium Risk', bg: '#fef9c3', text: '#854d0e' },
  RED: { label: 'High Risk', bg: '#fee2e2', text: '#7f1d1d' },
};

function pretty(s?: string) {
  if (!s) return '';
  return s.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
}

export default function LegalScreen() {
  const router = useRouter();
  const [tab, setTab] = useState<Tab>('browse');
  const [packages, setPackages] = useState<any[]>(DEFAULT_PACKAGES);

  // create form
  const [selectedPkg, setSelectedPkg] = useState('COMPREHENSIVE');
  const [propertyAddress, setPropertyAddress] = useState('');
  const [propertyCity, setPropertyCity] = useState('');
  const [propertyState, setPropertyState] = useState('Telangana');
  const [surveyNumber, setSurveyNumber] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // mine
  const [cases, setCases] = useState<any[]>([]);
  const [loadingMine, setLoadingMine] = useState(false);
  const [authed, setAuthed] = useState(false);

  useEffect(() => { api.getLegalPackages().then((p) => { if (p && p.length) setPackages(p); }).catch(() => {}); }, []);

  const loadMine = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) { setAuthed(false); setLoadingMine(false); return; }
    setAuthed(true); setLoadingMine(true);
    try { setCases(await api.getMyLegalCases(token) ?? []); }
    catch { setCases([]); }
    finally { setLoadingMine(false); }
  }, []);

  useEffect(() => { if (tab === 'mine') loadMine(); }, [tab, loadMine]);

  async function submit() {
    if (!propertyAddress.trim() || !propertyCity.trim()) { Alert.alert('Missing', 'Enter property address and city.'); return; }
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    setSubmitting(true);
    try {
      const res = await api.createLegalCase({ packageType: selectedPkg, propertyAddress, propertyCity, propertyState, surveyNumber: surveyNumber || undefined }, token);
      Alert.alert('Case created', 'Track verification under My Cases.');
      if (res?.id) router.push(`/legal-case/${res.id}` as any);
      else setTab('mine');
    } catch (e: any) {
      Alert.alert('Failed', e.message ?? 'Try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.heroTitle}>Legal Verification</Text>
        <Text style={styles.heroSub}>Verify property title, encumbrance & litigation before you buy.</Text>
      </View>

      <View style={styles.tabRow}>
        <TouchableOpacity style={[styles.tabBtn, tab === 'browse' && styles.tabBtnActive]} onPress={() => setTab('browse')}>
          <Text style={[styles.tabText, tab === 'browse' && styles.tabTextActive]}>Packages</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[styles.tabBtn, tab === 'mine' && styles.tabBtnActive]} onPress={() => setTab('mine')}>
          <Text style={[styles.tabText, tab === 'mine' && styles.tabTextActive]}>My Cases</Text>
        </TouchableOpacity>
      </View>

      {tab === 'browse' ? (
        <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
          {packages.map((p) => (
            <TouchableOpacity key={p.id} style={[styles.pkgCard, selectedPkg === p.id && styles.pkgCardActive]} onPress={() => setSelectedPkg(p.id)}>
              <View style={styles.pkgHead}>
                <Text style={styles.pkgName}>{p.name}{p.popular ? '  ⭐' : ''}</Text>
                <Text style={styles.pkgPrice}>{formatPaise(p.pricePaise)}</Text>
              </View>
              <Text style={styles.pkgTurn}>{p.turnaround}</Text>
              {(p.features ?? []).slice(0, 4).map((f: string) => <Text key={f} style={styles.pkgFeat}>• {f}</Text>)}
            </TouchableOpacity>
          ))}

          <Text style={styles.sectionTitle}>How it works</Text>
          {STEPS.map((s, i) => (
            <View key={s} style={styles.stepRow}>
              <View style={styles.stepCircle}><Text style={styles.stepNum}>{i + 1}</Text></View>
              <Text style={styles.stepLabel}>{s}</Text>
            </View>
          ))}

          <Text style={styles.sectionTitle}>Start verification</Text>
          <Field label="Property address *"><TextInput style={[styles.input, styles.textarea]} value={propertyAddress} onChangeText={setPropertyAddress} placeholder="Full address" placeholderTextColor="#9ca3af" multiline /></Field>
          <Field label="City *"><TextInput style={styles.input} value={propertyCity} onChangeText={setPropertyCity} placeholderTextColor="#9ca3af" /></Field>
          <Text style={styles.label}>State</Text>
          <View style={styles.pillRow}>
            {STATES.map((s) => (
              <TouchableOpacity key={s} style={[styles.pill, propertyState === s && styles.pillActive]} onPress={() => setPropertyState(s)}>
                <Text style={[styles.pillText, propertyState === s && styles.pillTextActive]}>{s}</Text>
              </TouchableOpacity>
            ))}
          </View>
          <Field label="Survey number"><TextInput style={styles.input} value={surveyNumber} onChangeText={setSurveyNumber} placeholder="optional" placeholderTextColor="#9ca3af" /></Field>

          <TouchableOpacity style={[styles.primaryBtn, submitting && { opacity: 0.6 }]} disabled={submitting} onPress={submit}>
            {submitting ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryBtnText}>Submit for Verification</Text>}
          </TouchableOpacity>
          <View style={{ height: 40 }} />
        </ScrollView>
      ) : (
        <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
          {!authed ? (
            <View style={styles.gate}>
              <Text style={styles.gateText}>Sign in to view your cases.</Text>
              <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/auth')}><Text style={styles.primaryBtnText}>Sign In</Text></TouchableOpacity>
            </View>
          ) : loadingMine ? (
            <ActivityIndicator color="#f97316" style={{ marginTop: 24 }} size="large" />
          ) : cases.length === 0 ? (
            <View style={styles.gate}><Text style={styles.gateText}>No cases yet. Start one from Packages.</Text></View>
          ) : (
            cases.map((c) => {
              const s = STATUS_STYLE[c.status] ?? STATUS_STYLE.CREATED;
              const risk = RISK_STYLE[c.riskLevel];
              return (
                <TouchableOpacity key={c.id} style={styles.itemCard} onPress={() => router.push(`/legal-case/${c.id}` as any)}>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.itemTitle}>{c.packageName ?? pretty(c.packageType)}</Text>
                    <Text style={styles.itemSub} numberOfLines={1}>{c.propertyAddress ?? 'Property verification'}</Text>
                    <View style={styles.badgeRow}>
                      <View style={[styles.badge, { backgroundColor: s.bg }]}><Text style={[styles.badgeText, { color: s.text }]}>{pretty(c.status)}</Text></View>
                      {risk ? <View style={[styles.badge, { backgroundColor: risk.bg }]}><Text style={[styles.badgeText, { color: risk.text }]}>{risk.label}</Text></View> : null}
                    </View>
                  </View>
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
  pkgCard:      { backgroundColor: '#fff', borderRadius: 12, padding: 14, marginBottom: 10, borderWidth: 1, borderColor: '#e5e7eb' },
  pkgCardActive:{ borderColor: '#f97316', backgroundColor: '#fff7ed' },
  pkgHead:      { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  pkgName:      { fontSize: 15, fontWeight: '700', color: '#111827' },
  pkgPrice:     { fontSize: 15, fontWeight: '800', color: '#f97316' },
  pkgTurn:      { fontSize: 12, color: '#6b7280', marginTop: 2, marginBottom: 6 },
  pkgFeat:      { fontSize: 12, color: '#374151', marginTop: 2 },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: '#111827', marginTop: 22, marginBottom: 10 },
  stepRow:      { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 10 },
  stepCircle:   { width: 28, height: 28, borderRadius: 14, backgroundColor: '#1e3a8a', alignItems: 'center', justifyContent: 'center' },
  stepNum:      { color: '#fff', fontWeight: '800', fontSize: 13 },
  stepLabel:    { fontSize: 13, color: '#374151', fontWeight: '600' },
  label:        { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 6 },
  field:        { marginBottom: 14 },
  input:        { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: '#111827' },
  textarea:     { height: 72, textAlignVertical: 'top' },
  pillRow:      { flexDirection: 'row', gap: 8, marginBottom: 14, flexWrap: 'wrap' },
  pill:         { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb', backgroundColor: '#fff' },
  pillActive:   { backgroundColor: '#f97316', borderColor: '#f97316' },
  pillText:     { fontSize: 12, fontWeight: '600', color: '#374151' },
  pillTextActive:{ color: '#fff' },
  primaryBtn:   { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 15, alignItems: 'center', marginTop: 4 },
  primaryBtnText:{ color: '#fff', fontSize: 16, fontWeight: '700' },
  gate:         { backgroundColor: '#fff', borderRadius: 14, padding: 24, alignItems: 'center', marginTop: 16, borderWidth: 1, borderColor: '#f3f4f6' },
  gateText:     { fontSize: 14, color: '#374151', marginBottom: 12, textAlign: 'center' },
  itemCard:     { backgroundColor: '#fff', borderRadius: 14, padding: 14, marginBottom: 10, borderWidth: 1, borderColor: '#f3f4f6' },
  itemTitle:    { fontSize: 14, fontWeight: '700', color: '#111827' },
  itemSub:      { fontSize: 12, color: '#6b7280', marginTop: 2 },
  badgeRow:     { flexDirection: 'row', gap: 6, marginTop: 8 },
  badge:        { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 100 },
  badgeText:    { fontSize: 11, fontWeight: '700' },
});
