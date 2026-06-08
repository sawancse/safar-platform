import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, ScrollView, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator, Alert,
} from 'react-native';
import { useLocalSearchParams, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const STATUS_STEPS = ['CREATED', 'DOCUMENTS_UPLOADED', 'UNDER_REVIEW', 'VERIFICATION_IN_PROGRESS', 'REPORT_READY', 'COMPLETED'];

const RISK_STYLE: Record<string, { label: string; bg: string; text: string }> = {
  GREEN: { label: 'Low Risk', bg: '#dcfce7', text: '#14532d' },
  YELLOW: { label: 'Medium Risk', bg: '#fef9c3', text: '#854d0e' },
  RED: { label: 'High Risk', bg: '#fee2e2', text: '#7f1d1d' },
};

const VERIFICATION_ITEMS = [
  { key: 'TITLE_CHAIN', label: 'Title Chain Verification' },
  { key: 'ENCUMBRANCE', label: 'Encumbrance Check' },
  { key: 'GOVT_APPROVAL', label: 'Government Approvals' },
  { key: 'LITIGATION', label: 'Litigation Check' },
  { key: 'TAX', label: 'Tax Verification' },
  { key: 'SURVEY', label: 'Survey Match' },
];

function pretty(s?: string) {
  if (!s) return '';
  return s.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
}
function itemIcon(status?: string) {
  if (status === 'CLEAN') return '✅';
  if (status === 'ISSUE') return '⚠️';
  return '⏳';
}

export default function LegalCaseScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const [legalCase, setLegalCase] = useState<any>(null);
  const [documents, setDocuments] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [scheduledAt, setScheduledAt] = useState('');
  const [scheduling, setScheduling] = useState(false);

  const load = useCallback(async () => {
    const token = await getAccessToken();
    if (!token || !id) { setLoading(false); return; }
    try {
      const [c, docs] = await Promise.all([api.getLegalCase(id, token), api.getLegalCaseDocuments(id, token)]);
      setLegalCase(c); setDocuments(docs ?? []);
    } catch { setLegalCase(null); }
    finally { setLoading(false); }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  async function schedule() {
    if (!scheduledAt.trim()) { Alert.alert('Pick date/time', 'Enter date and time, e.g. 2026-06-15T11:00'); return; }
    const token = await getAccessToken();
    if (!token || !id) return;
    setScheduling(true);
    try {
      await api.scheduleLegalConsultation(id, scheduledAt, token);
      Alert.alert('Consultation scheduled', 'An advocate will call you at the chosen time.');
      setScheduledAt('');
    } catch (e: any) {
      Alert.alert('Failed', e.message ?? 'Try again.');
    } finally {
      setScheduling(false);
    }
  }

  if (loading) return <View style={styles.center}><ActivityIndicator size="large" color="#f97316" /></View>;
  if (!legalCase) return <View style={styles.center}><Text style={styles.emptyTitle}>Case not found</Text></View>;

  const currentStep = STATUS_STEPS.indexOf(legalCase.status);
  const risk = RISK_STYLE[legalCase.riskLevel];
  const items = legalCase.verificationItems ?? {};
  const canReport = legalCase.status === 'REPORT_READY' || legalCase.status === 'COMPLETED';

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
      <Stack.Screen options={{ title: `Case #${(legalCase.id ?? '').slice(0, 6)}` }} />

      <View style={styles.headerCard}>
        <Text style={styles.title}>{legalCase.packageName ?? pretty(legalCase.packageType)}</Text>
        <View style={styles.badgeRow}>
          <View style={[styles.badge, { backgroundColor: '#e0e7ff' }]}><Text style={[styles.badgeText, { color: '#3730a3' }]}>{pretty(legalCase.status)}</Text></View>
          {risk ? <View style={[styles.badge, { backgroundColor: risk.bg }]}><Text style={[styles.badgeText, { color: risk.text }]}>{risk.label}</Text></View> : null}
        </View>
        {legalCase.propertyAddress ? <Text style={styles.sub}>{legalCase.propertyAddress}</Text> : null}
      </View>

      {/* Progress */}
      <Text style={styles.sectionTitle}>Progress</Text>
      <View style={styles.card}>
        {STATUS_STEPS.map((s, i) => (
          <View key={s} style={styles.progRow}>
            <View style={[styles.progDot, i <= currentStep && styles.progDotOn]}><Text style={styles.progDotText}>{i < currentStep ? '✓' : i + 1}</Text></View>
            <Text style={[styles.progLabel, i <= currentStep && styles.progLabelOn]}>{pretty(s)}</Text>
          </View>
        ))}
      </View>

      {/* Advocate */}
      {legalCase.advocateName ? (
        <>
          <Text style={styles.sectionTitle}>Assigned advocate</Text>
          <View style={styles.card}>
            <Text style={styles.advName}>{legalCase.advocateName}</Text>
            {legalCase.advocateSpecialization ? <Text style={styles.sub}>{legalCase.advocateSpecialization}</Text> : null}
            {legalCase.advocateBarCouncil ? <Text style={styles.sub}>Bar Council: {legalCase.advocateBarCouncil}</Text> : null}
            {legalCase.advocateRating ? <Text style={styles.sub}>★ {legalCase.advocateRating}</Text> : null}
          </View>
        </>
      ) : null}

      {/* Verification checklist */}
      <Text style={styles.sectionTitle}>Verification checklist</Text>
      <View style={styles.card}>
        {VERIFICATION_ITEMS.map((vi) => (
          <View key={vi.key} style={styles.checkRow}>
            <Text style={styles.checkIcon}>{itemIcon(items[vi.key])}</Text>
            <Text style={styles.checkLabel}>{vi.label}</Text>
            {items[vi.key] ? <Text style={styles.checkStatus}>{pretty(items[vi.key])}</Text> : null}
          </View>
        ))}
      </View>

      {/* Documents */}
      {documents.length > 0 ? (
        <>
          <Text style={styles.sectionTitle}>Documents</Text>
          <View style={styles.card}>
            {documents.map((d) => (
              <View key={d.id} style={styles.docRow}>
                <Text style={styles.docName}>{d.name ?? pretty(d.documentType)}</Text>
                <Text style={styles.docStatus}>{pretty(d.verificationStatus)}</Text>
              </View>
            ))}
          </View>
        </>
      ) : null}

      {canReport ? (
        <View style={styles.reportBox}><Text style={styles.reportText}>📄 Your risk report is ready — download it from your email or the web dashboard.</Text></View>
      ) : null}

      {/* Consultation */}
      <Text style={styles.sectionTitle}>Schedule a consultation</Text>
      <TextInput style={styles.input} value={scheduledAt} onChangeText={setScheduledAt} placeholder="YYYY-MM-DDTHH:MM" placeholderTextColor="#9ca3af" />
      <TouchableOpacity style={[styles.primaryBtn, scheduling && { opacity: 0.6 }]} disabled={scheduling} onPress={schedule}>
        {scheduling ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryBtnText}>Schedule Call</Text>}
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container:   { flex: 1, backgroundColor: '#f9fafb' },
  center:      { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb' },
  emptyTitle:  { fontSize: 16, fontWeight: '700', color: '#374151' },
  headerCard:  { backgroundColor: '#fff', borderRadius: 14, padding: 16, borderWidth: 1, borderColor: '#f3f4f6' },
  title:       { fontSize: 18, fontWeight: '800', color: '#111827' },
  badgeRow:    { flexDirection: 'row', gap: 6, marginTop: 8 },
  badge:       { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 100 },
  badgeText:   { fontSize: 11, fontWeight: '700' },
  sub:         { fontSize: 12, color: '#6b7280', marginTop: 4 },
  sectionTitle:{ fontSize: 16, fontWeight: '700', color: '#111827', marginTop: 22, marginBottom: 8 },
  card:        { backgroundColor: '#fff', borderRadius: 12, padding: 14, borderWidth: 1, borderColor: '#f3f4f6' },
  progRow:     { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 10 },
  progDot:     { width: 26, height: 26, borderRadius: 13, backgroundColor: '#e5e7eb', alignItems: 'center', justifyContent: 'center' },
  progDotOn:   { backgroundColor: '#f97316' },
  progDotText: { fontSize: 12, fontWeight: '700', color: '#fff' },
  progLabel:   { fontSize: 13, color: '#9ca3af' },
  progLabelOn: { color: '#111827', fontWeight: '600' },
  advName:     { fontSize: 14, fontWeight: '700', color: '#111827' },
  checkRow:    { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 8 },
  checkIcon:   { fontSize: 16, width: 22 },
  checkLabel:  { fontSize: 13, color: '#374151', flex: 1 },
  checkStatus: { fontSize: 11, color: '#6b7280', fontWeight: '600' },
  docRow:      { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 6 },
  docName:     { fontSize: 13, color: '#374151', flex: 1 },
  docStatus:   { fontSize: 11, color: '#6b7280', fontWeight: '600' },
  reportBox:   { backgroundColor: '#f0fdf4', borderRadius: 12, padding: 14, marginTop: 16, borderWidth: 1, borderColor: '#bbf7d0' },
  reportText:  { fontSize: 13, color: '#166534' },
  input:       { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: '#111827' },
  primaryBtn:  { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 15, alignItems: 'center', marginTop: 10 },
  primaryBtnText:{ color: '#fff', fontSize: 16, fontWeight: '700' },
});
