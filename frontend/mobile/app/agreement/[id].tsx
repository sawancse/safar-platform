import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, StyleSheet, ActivityIndicator, Alert,
} from 'react-native';
import { useLocalSearchParams, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';

const STATUS_STYLE: Record<string, { bg: string; text: string }> = {
  DRAFT: { bg: '#f3f4f6', text: '#374151' },
  PENDING_PAYMENT: { bg: '#fef9c3', text: '#854d0e' },
  PAID: { bg: '#dbeafe', text: '#1e40af' },
  STAMPED: { bg: '#e0e7ff', text: '#3730a3' },
  SIGNED: { bg: '#dcfce7', text: '#14532d' },
  REGISTERED: { bg: '#dcfce7', text: '#14532d' },
  DELIVERED: { bg: '#dcfce7', text: '#14532d' },
  REJECTED: { bg: '#fee2e2', text: '#7f1d1d' },
  CANCELLED: { bg: '#fee2e2', text: '#7f1d1d' },
};

function pretty(s?: string) {
  if (!s) return '';
  return s.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
}
function safeParse(s?: string) { try { return s ? JSON.parse(s) : null; } catch { return null; } }

export default function AgreementDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const [agreement, setAgreement] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);

  const load = useCallback(async () => {
    const token = await getAccessToken();
    if (!token || !id) { setLoading(false); return; }
    try { setAgreement(await api.getAgreementDetail(id, token)); }
    catch { setAgreement(null); }
    finally { setLoading(false); }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  async function generateDraft() {
    const token = await getAccessToken();
    if (!token || !id) return;
    setGenerating(true);
    try {
      const updated = await api.generateAgreementDraft(id, token);
      setAgreement(updated);
      Alert.alert('Draft generated', 'Your draft document is ready.');
    } catch (e: any) {
      Alert.alert('Failed', e.message ?? 'Try again.');
    } finally {
      setGenerating(false);
    }
  }

  if (loading) return <View style={styles.center}><ActivityIndicator size="large" color="#f97316" /></View>;
  if (!agreement) return <View style={styles.center}><Text style={styles.emptyTitle}>Agreement not found</Text></View>;

  const s = STATUS_STYLE[agreement.status] ?? STATUS_STYLE.DRAFT;
  const parties = agreement.parties ?? safeParse(agreement.partyDetailsJson) ?? [];
  const prop = safeParse(agreement.propertyDetailsJson) ?? {};

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ padding: 16, paddingBottom: 40 }}>
      <Stack.Screen options={{ title: pretty(agreement.agreementType) || 'Agreement' }} />

      <View style={styles.headerCard}>
        <Text style={styles.title}>{pretty(agreement.agreementType)}</Text>
        <View style={[styles.badge, { backgroundColor: s.bg }]}><Text style={[styles.badgeText, { color: s.text }]}>{pretty(agreement.status)}</Text></View>
        <Text style={styles.ref}>Ref: {agreement.id?.slice(0, 8)}</Text>
        {agreement.createdAt ? <Text style={styles.ref}>Created {new Date(agreement.createdAt).toLocaleDateString('en-IN')}</Text> : null}
      </View>

      {agreement.status === 'REJECTED' && agreement.rejectionReason ? (
        <View style={styles.alert}><Text style={styles.alertText}>{agreement.rejectionReason}</Text></View>
      ) : null}

      {/* Fees */}
      <Text style={styles.sectionTitle}>Fees</Text>
      <View style={styles.card}>
        <Row label="Stamp duty" value={formatPaise(agreement.stampDutyPaise ?? 0)} />
        <Row label="Registration" value={formatPaise(agreement.registrationFeePaise ?? 0)} />
        <Row label="Service fee" value={formatPaise(agreement.serviceFeePaise ?? 0)} />
        <View style={styles.divider} />
        <Row label="Total" value={formatPaise(agreement.totalAmountPaise ?? 0)} bold />
      </View>

      {/* Property */}
      {(prop.propertyAddress || agreement.city) ? (
        <>
          <Text style={styles.sectionTitle}>Property</Text>
          <View style={styles.card}>
            {prop.propertyAddress ? <Text style={styles.bodyText}>{prop.propertyAddress}</Text> : null}
            {(prop.state || agreement.state) ? <Text style={styles.bodyMuted}>{prop.state ?? agreement.state}</Text> : null}
            {prop.propertyValuePaise ? <Text style={styles.bodyMuted}>Value: {formatPaise(prop.propertyValuePaise)}</Text> : null}
          </View>
        </>
      ) : null}

      {/* Parties */}
      {parties.length > 0 ? (
        <>
          <Text style={styles.sectionTitle}>Parties</Text>
          <View style={styles.card}>
            {parties.map((p: any, i: number) => (
              <View key={i} style={styles.partyRow}>
                <Text style={styles.partyName}>{p.name}</Text>
                <Text style={styles.partyRole}>{pretty(p.role)}</Text>
              </View>
            ))}
          </View>
        </>
      ) : null}

      {agreement.status === 'DRAFT' ? (
        <TouchableOpacity style={[styles.primaryBtn, generating && { opacity: 0.6 }]} disabled={generating} onPress={generateDraft}>
          {generating ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryBtnText}>Generate Draft</Text>}
        </TouchableOpacity>
      ) : null}
      {agreement.draftPdfUrl ? (
        <Text style={styles.noteText}>Draft document is ready — download from your email or the web dashboard.</Text>
      ) : null}
    </ScrollView>
  );
}

function Row({ label, value, bold }: { label: string; value: string; bold?: boolean }) {
  return <View style={styles.row}><Text style={[styles.rowLabel, bold && styles.bold]}>{label}</Text><Text style={[styles.rowVal, bold && styles.bold]}>{value}</Text></View>;
}

const styles = StyleSheet.create({
  container:   { flex: 1, backgroundColor: '#f9fafb' },
  center:      { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb' },
  emptyTitle:  { fontSize: 16, fontWeight: '700', color: '#374151' },
  headerCard:  { backgroundColor: '#fff', borderRadius: 14, padding: 16, borderWidth: 1, borderColor: '#f3f4f6' },
  title:       { fontSize: 20, fontWeight: '800', color: '#111827' },
  badge:       { alignSelf: 'flex-start', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 100, marginTop: 8 },
  badgeText:   { fontSize: 11, fontWeight: '700' },
  ref:         { fontSize: 12, color: '#9ca3af', marginTop: 6 },
  alert:       { backgroundColor: '#fef2f2', borderRadius: 12, padding: 14, marginTop: 12, borderWidth: 1, borderColor: '#fecaca' },
  alertText:   { fontSize: 13, color: '#991b1b' },
  sectionTitle:{ fontSize: 16, fontWeight: '700', color: '#111827', marginTop: 22, marginBottom: 8 },
  card:        { backgroundColor: '#fff', borderRadius: 12, padding: 14, borderWidth: 1, borderColor: '#f3f4f6' },
  row:         { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 6 },
  rowLabel:    { fontSize: 13, color: '#6b7280' },
  rowVal:      { fontSize: 13, color: '#374151', fontWeight: '600' },
  bold:        { fontWeight: '800', color: '#111827', fontSize: 15 },
  divider:     { height: 1, backgroundColor: '#f3f4f6', marginVertical: 8 },
  bodyText:    { fontSize: 13, color: '#374151', lineHeight: 19 },
  bodyMuted:   { fontSize: 12, color: '#6b7280', marginTop: 4 },
  partyRow:    { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 6 },
  partyName:   { fontSize: 13, fontWeight: '600', color: '#111827' },
  partyRole:   { fontSize: 12, color: '#6b7280' },
  primaryBtn:  { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 15, alignItems: 'center', marginTop: 18 },
  primaryBtnText:{ color: '#fff', fontSize: 16, fontWeight: '700' },
  noteText:    { fontSize: 12, color: '#6b7280', marginTop: 12, textAlign: 'center' },
});
