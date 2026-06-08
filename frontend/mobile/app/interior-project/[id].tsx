import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, StyleSheet, ActivityIndicator,
} from 'react-native';
import { useLocalSearchParams, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';

type Tab = 'overview' | 'rooms' | 'quote' | 'milestones';

const STATUS_STYLE: Record<string, { bg: string; text: string }> = {
  CONSULTATION: { bg: '#f3f4f6', text: '#374151' },
  DESIGNING: { bg: '#e0e7ff', text: '#3730a3' },
  IN_PROGRESS: { bg: '#fef9c3', text: '#854d0e' },
  COMPLETED: { bg: '#dcfce7', text: '#14532d' },
};

function pretty(s?: string) {
  if (!s) return '';
  return s.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
}

export default function InteriorProjectScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const [tab, setTab] = useState<Tab>('overview');
  const [project, setProject] = useState<any>(null);
  const [rooms, setRooms] = useState<any[]>([]);
  const [milestones, setMilestones] = useState<any[]>([]);
  const [quote, setQuote] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    const token = await getAccessToken();
    if (!token || !id) { setLoading(false); return; }
    try {
      const [p, r, m, q] = await Promise.all([
        api.getInteriorProject(id, token),
        api.getInteriorRooms(id, token),
        api.getInteriorMilestones(id, token),
        api.getInteriorQuote(id, token),
      ]);
      setProject(p); setRooms(r ?? []); setMilestones(m ?? []); setQuote(q);
    } catch { setProject(null); }
    finally { setLoading(false); }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <View style={styles.center}><ActivityIndicator size="large" color="#f97316" /></View>;
  if (!project) return <View style={styles.center}><Text style={styles.emptyTitle}>Project not found</Text></View>;

  const s = STATUS_STYLE[project.status] ?? STATUS_STYLE.CONSULTATION;

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: pretty(project.projectType) || 'Project' }} />
      <View style={styles.headerCard}>
        <Text style={styles.title}>{pretty(project.projectType) || 'Interior project'}</Text>
        <View style={[styles.badge, { backgroundColor: s.bg }]}><Text style={[styles.badgeText, { color: s.text }]}>{pretty(project.status)}</Text></View>
        {project.designerName ? <Text style={styles.sub}>Designer: {project.designerName}</Text> : null}
      </View>

      <View style={styles.tabRow}>
        {(['overview', 'rooms', 'quote', 'milestones'] as Tab[]).map((t) => (
          <TouchableOpacity key={t} style={[styles.tabBtn, tab === t && styles.tabBtnActive]} onPress={() => setTab(t)}>
            <Text style={[styles.tabText, tab === t && styles.tabTextActive]}>{pretty(t)}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        {tab === 'overview' && (
          <>
            <View style={styles.card}>
              <Text style={styles.label}>Progress</Text>
              <View style={styles.progressBg}><View style={[styles.progressFill, { width: `${project.progressPercent ?? 0}%` }]} /></View>
              <Text style={styles.progressText}>{project.progressPercent ?? 0}% complete</Text>
            </View>
            <View style={styles.card}>
              <Row label="Start date" value={project.startDate ?? '—'} />
              <Row label="Est. completion" value={project.estimatedEndDate ?? '—'} />
              <Row label="Budget" value={formatPaise(project.budgetPaise ?? 0)} />
              <Row label="Paid" value={formatPaise(project.paidPaise ?? 0)} />
            </View>
            {(project.designerPhone || project.designerEmail) ? (
              <View style={styles.card}>
                <Text style={styles.label}>Designer contact</Text>
                {project.designerPhone ? <Text style={styles.bodyText}>📞 {project.designerPhone}</Text> : null}
                {project.designerEmail ? <Text style={styles.bodyText}>✉️ {project.designerEmail}</Text> : null}
              </View>
            ) : null}
          </>
        )}

        {tab === 'rooms' && (
          rooms.length === 0 ? <Text style={styles.muted}>Room designs appear once the design phase begins.</Text> :
          rooms.map((r) => (
            <View key={r.id} style={styles.card}>
              <Text style={styles.roomName}>{r.roomName ?? r.name}</Text>
              {r.designStyle ? <Text style={styles.sub}>{r.designStyle}</Text> : null}
              {r.description ? <Text style={styles.bodyText}>{r.description}</Text> : null}
              <View style={styles.assetRow}>
                {r.has3dRender ? <Text style={styles.assetChip}>3D Render</Text> : null}
                {r.hasFloorPlan ? <Text style={styles.assetChip}>Floor Plan</Text> : null}
                {r.hasMoodBoard ? <Text style={styles.assetChip}>Mood Board</Text> : null}
              </View>
              {Array.isArray(r.materials) && r.materials.length > 0 ? (
                r.materials.map((m: any, i: number) => (
                  <View key={i} style={styles.matRow}>
                    <Text style={styles.matName}>{m.name}{m.brand ? ` · ${m.brand}` : ''}</Text>
                    <Text style={styles.matPrice}>{formatPaise(m.pricePaise ?? 0)}</Text>
                  </View>
                ))
              ) : null}
            </View>
          ))
        )}

        {tab === 'quote' && (
          !quote ? <Text style={styles.muted}>The quote is prepared after design approval.</Text> :
          <View style={styles.card}>
            <Row label="Materials" value={formatPaise(quote.materialsCostPaise ?? 0)} />
            <Row label="Labour" value={formatPaise(quote.laborCostPaise ?? 0)} />
            <Row label="Hardware" value={formatPaise(quote.hardwareCostPaise ?? 0)} />
            <Row label="Overhead" value={formatPaise(quote.overheadPaise ?? 0)} />
            {quote.discountPaise ? <Row label="Discount" value={`- ${formatPaise(quote.discountPaise)}`} /> : null}
            <View style={styles.divider} />
            <Row label="Total" value={formatPaise(quote.totalPaise ?? 0)} bold />
            {Array.isArray(quote.paymentSchedule) && quote.paymentSchedule.length > 0 ? (
              <>
                <Text style={[styles.label, { marginTop: 12 }]}>Payment schedule</Text>
                {quote.paymentSchedule.map((ps: any, i: number) => (
                  <View key={i} style={styles.scheduleRow}>
                    <Text style={styles.bodyText}>{ps.milestone}{ps.dueDate ? ` · ${ps.dueDate}` : ''}</Text>
                    <Text style={[styles.matPrice, ps.paid && { color: '#16a34a' }]}>{formatPaise(ps.amountPaise ?? 0)}{ps.paid ? ' ✓' : ''}</Text>
                  </View>
                ))}
              </>
            ) : null}
          </View>
        )}

        {tab === 'milestones' && (
          milestones.length === 0 ? <Text style={styles.muted}>Milestones appear once execution is scheduled.</Text> :
          milestones.map((m) => (
            <View key={m.id} style={styles.card}>
              <View style={styles.mileHead}>
                <Text style={styles.roomName}>{m.name ?? m.title}</Text>
                <Text style={styles.mileStatus}>{pretty(m.status)}</Text>
              </View>
              {m.scheduledDate ? <Text style={styles.sub}>Scheduled: {m.scheduledDate}</Text> : null}
              {m.completedDate ? <Text style={styles.sub}>Completed: {m.completedDate}</Text> : null}
              {m.description ? <Text style={styles.bodyText}>{m.description}</Text> : null}
              {m.linkedPaymentPaise ? <Text style={styles.matPrice}>Payment: {formatPaise(m.linkedPaymentPaise)}</Text> : null}
            </View>
          ))
        )}

        <View style={{ height: 40 }} />
      </ScrollView>
    </View>
  );
}

function Row({ label, value, bold }: { label: string; value: string; bold?: boolean }) {
  return <View style={styles.row}><Text style={[styles.rowLabel, bold && styles.bold]}>{label}</Text><Text style={[styles.rowVal, bold && styles.bold]}>{value}</Text></View>;
}

const styles = StyleSheet.create({
  container:   { flex: 1, backgroundColor: '#f9fafb' },
  center:      { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb' },
  emptyTitle:  { fontSize: 16, fontWeight: '700', color: '#374151' },
  headerCard:  { backgroundColor: '#fff', padding: 16, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  title:       { fontSize: 19, fontWeight: '800', color: '#111827' },
  badge:       { alignSelf: 'flex-start', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 100, marginTop: 8 },
  badgeText:   { fontSize: 11, fontWeight: '700' },
  sub:         { fontSize: 12, color: '#6b7280', marginTop: 4 },
  tabRow:      { flexDirection: 'row', backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#e5e7eb' },
  tabBtn:      { flex: 1, paddingVertical: 12, alignItems: 'center' },
  tabBtnActive:{ borderBottomWidth: 3, borderBottomColor: '#f97316' },
  tabText:     { fontSize: 13, fontWeight: '600', color: '#6b7280' },
  tabTextActive:{ color: '#f97316' },
  scroll:      { padding: 16 },
  card:        { backgroundColor: '#fff', borderRadius: 12, padding: 14, marginBottom: 12, borderWidth: 1, borderColor: '#f3f4f6' },
  label:       { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 8 },
  progressBg:  { height: 8, backgroundColor: '#f3f4f6', borderRadius: 100, overflow: 'hidden' },
  progressFill:{ height: 8, backgroundColor: '#7c3aed', borderRadius: 100 },
  progressText:{ fontSize: 13, color: '#374151', fontWeight: '600', marginTop: 8 },
  row:         { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 6 },
  rowLabel:    { fontSize: 13, color: '#6b7280' },
  rowVal:      { fontSize: 13, color: '#374151', fontWeight: '600' },
  bold:        { fontWeight: '800', color: '#111827', fontSize: 15 },
  divider:     { height: 1, backgroundColor: '#f3f4f6', marginVertical: 8 },
  bodyText:    { fontSize: 13, color: '#374151', marginTop: 4, lineHeight: 19 },
  muted:       { fontSize: 13, color: '#9ca3af' },
  roomName:    { fontSize: 14, fontWeight: '700', color: '#111827' },
  assetRow:    { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 8 },
  assetChip:   { fontSize: 10, color: '#3730a3', backgroundColor: '#e0e7ff', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 100, overflow: 'hidden' },
  matRow:      { flexDirection: 'row', justifyContent: 'space-between', marginTop: 6 },
  matName:     { fontSize: 12, color: '#374151', flex: 1 },
  matPrice:    { fontSize: 12, fontWeight: '700', color: '#7c3aed' },
  scheduleRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 6 },
  mileHead:    { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  mileStatus:  { fontSize: 11, fontWeight: '700', color: '#6b7280' },
});
