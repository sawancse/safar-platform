import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, ScrollView, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator, Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';

const PROPERTY_TYPES = ['APARTMENT', 'VILLA', 'INDEPENDENT_HOUSE', 'PENTHOUSE'];
const CITIES = ['Bangalore', 'Mumbai', 'Delhi', 'Hyderabad', 'Chennai', 'Pune', 'Kolkata', 'Ahmedabad'];
const BUDGET_RANGES = [
  { label: 'Under 5L', min: 0, max: 50000000 },
  { label: '5L - 10L', min: 50000000, max: 100000000 },
  { label: '10L - 20L', min: 100000000, max: 200000000 },
  { label: '20L - 50L', min: 200000000, max: 500000000 },
  { label: 'Above 50L', min: 500000000, max: 1000000000 },
];
const SERVICE_PACKAGES = [
  { name: 'Modular Kitchen', range: '₹1.5L - 5L' },
  { name: 'Wardrobe', range: '₹80K - 3L' },
  { name: 'Full Room', range: '₹2L - 6L' },
  { name: 'Full Home', range: '₹5L - 20L' },
  { name: 'Renovation', range: '₹8L - 30L' },
];
const HOW_IT_WORKS = ['Book consultation', 'Property measurement', '3D designs & floor plans', 'Review designs & quote', 'Professional execution', 'Final QC & warranty'];
const MATERIAL_CATEGORIES = ['FLOORING', 'WALL', 'COUNTERTOP', 'CABINET', 'PAINT', 'LIGHTING', 'HARDWARE'];

const STATUS_STYLE: Record<string, { bg: string; text: string }> = {
  CONSULTATION: { bg: '#f3f4f6', text: '#374151' },
  MEASUREMENT: { bg: '#dbeafe', text: '#1e40af' },
  DESIGNING: { bg: '#e0e7ff', text: '#3730a3' },
  QUOTE_SENT: { bg: '#fef9c3', text: '#854d0e' },
  APPROVED: { bg: '#dcfce7', text: '#14532d' },
  IN_PROGRESS: { bg: '#fef9c3', text: '#854d0e' },
  QUALITY_CHECK: { bg: '#e0e7ff', text: '#3730a3' },
  COMPLETED: { bg: '#dcfce7', text: '#14532d' },
};

function pretty(s?: string) {
  if (!s) return '';
  return s.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
}

export default function InteriorsScreen() {
  const router = useRouter();
  const [projectType, setProjectType] = useState('Full Home');
  const [propertyType, setPropertyType] = useState('APARTMENT');
  const [city, setCity] = useState('Bangalore');
  const [roomCount, setRoomCount] = useState('2');
  const [budgetIdx, setBudgetIdx] = useState(2);
  const [consultDate, setConsultDate] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [booked, setBooked] = useState(false);

  const [designers, setDesigners] = useState<any[]>([]);
  const [materials, setMaterials] = useState<any[]>([]);
  const [materialCat, setMaterialCat] = useState('FLOORING');
  const [projects, setProjects] = useState<any[]>([]);

  const loadProjects = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) return;
    try { setProjects(await api.getMyInteriorProjects(token) ?? []); } catch {}
  }, []);

  useEffect(() => { api.getInteriorDesigners(city).then(setDesigners).catch(() => {}); }, [city]);
  useEffect(() => { api.getInteriorMaterials(materialCat).then(setMaterials).catch(() => {}); }, [materialCat]);
  useEffect(() => { loadProjects(); }, [loadProjects]);

  async function bookConsultation() {
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    setSubmitting(true);
    try {
      const range = BUDGET_RANGES[budgetIdx];
      await api.bookInteriorConsultation({
        projectType, propertyType, city, roomCount: Number(roomCount) || 1,
        budgetMinPaise: range.min, budgetMaxPaise: range.max,
        consultationDate: consultDate || null,
      }, token);
      setBooked(true);
      loadProjects();
    } catch (e: any) {
      Alert.alert('Failed', e.message ?? 'Try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.hero}>
        <Text style={styles.heroTitle}>Home Interiors</Text>
        <Text style={styles.heroSub}>Designers, 3D designs, materials & end-to-end execution.</Text>
      </View>

      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        {/* Consultation form */}
        <Text style={styles.sectionTitle}>Book a free consultation</Text>
        {booked ? (
          <View style={styles.successCard}>
            <Text style={styles.successIcon}>✅</Text>
            <Text style={styles.successTitle}>Consultation booked!</Text>
            <Text style={styles.successSub}>A designer will reach out to confirm your slot.</Text>
          </View>
        ) : (
          <View style={styles.card}>
            <Text style={styles.label}>Project</Text>
            <View style={styles.pillRow}>
              {SERVICE_PACKAGES.map((p) => (
                <TouchableOpacity key={p.name} style={[styles.pill, projectType === p.name && styles.pillActive]} onPress={() => setProjectType(p.name)}>
                  <Text style={[styles.pillText, projectType === p.name && styles.pillTextActive]}>{p.name}</Text>
                </TouchableOpacity>
              ))}
            </View>
            <Text style={styles.label}>Property type</Text>
            <View style={styles.pillRow}>
              {PROPERTY_TYPES.map((p) => (
                <TouchableOpacity key={p} style={[styles.pill, propertyType === p && styles.pillActive]} onPress={() => setPropertyType(p)}>
                  <Text style={[styles.pillText, propertyType === p && styles.pillTextActive]}>{pretty(p)}</Text>
                </TouchableOpacity>
              ))}
            </View>
            <Text style={styles.label}>City</Text>
            <View style={styles.pillRow}>
              {CITIES.map((c) => (
                <TouchableOpacity key={c} style={[styles.pill, city === c && styles.pillActive]} onPress={() => setCity(c)}>
                  <Text style={[styles.pillText, city === c && styles.pillTextActive]}>{c}</Text>
                </TouchableOpacity>
              ))}
            </View>
            <View style={styles.row}>
              <View style={{ flex: 1 }}>
                <Text style={styles.label}>Rooms</Text>
                <TextInput style={styles.input} value={roomCount} onChangeText={setRoomCount} keyboardType="numeric" />
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.label}>Consultation date</Text>
                <TextInput style={styles.input} value={consultDate} onChangeText={setConsultDate} placeholder="YYYY-MM-DD" placeholderTextColor="#9ca3af" />
              </View>
            </View>
            <Text style={styles.label}>Budget</Text>
            <View style={styles.pillRow}>
              {BUDGET_RANGES.map((b, i) => (
                <TouchableOpacity key={b.label} style={[styles.pill, budgetIdx === i && styles.pillActive]} onPress={() => setBudgetIdx(i)}>
                  <Text style={[styles.pillText, budgetIdx === i && styles.pillTextActive]}>{b.label}</Text>
                </TouchableOpacity>
              ))}
            </View>
            <TouchableOpacity style={[styles.primaryBtn, submitting && { opacity: 0.6 }]} disabled={submitting} onPress={bookConsultation}>
              {submitting ? <ActivityIndicator color="#fff" /> : <Text style={styles.primaryBtnText}>Book Free Consultation</Text>}
            </TouchableOpacity>
          </View>
        )}

        {/* My projects */}
        {projects.length > 0 ? (
          <>
            <Text style={styles.sectionTitle}>My projects</Text>
            {projects.map((p) => {
              const s = STATUS_STYLE[p.status] ?? STATUS_STYLE.CONSULTATION;
              return (
                <TouchableOpacity key={p.id} style={styles.projCard} onPress={() => router.push(`/interior-project/${p.id}` as any)}>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.projTitle}>{pretty(p.projectType) || p.name}</Text>
                    {p.designerName ? <Text style={styles.projSub}>{p.designerName}</Text> : null}
                    <View style={styles.progressBg}><View style={[styles.progressFill, { width: `${p.progressPercent ?? 0}%` }]} /></View>
                  </View>
                  <View style={[styles.badge, { backgroundColor: s.bg }]}><Text style={[styles.badgeText, { color: s.text }]}>{pretty(p.status)}</Text></View>
                </TouchableOpacity>
              );
            })}
          </>
        ) : null}

        {/* Designers */}
        {designers.length > 0 ? (
          <>
            <Text style={styles.sectionTitle}>Designers in {city}</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 12 }}>
              {designers.map((d) => (
                <View key={d.id} style={styles.designerCard}>
                  <View style={styles.designerAvatar}><Text style={styles.designerAvatarText}>{(d.name ?? '?').charAt(0)}</Text></View>
                  <Text style={styles.designerName} numberOfLines={1}>{d.name}</Text>
                  <Text style={styles.designerMeta}>{d.experience ? `${d.experience} yrs` : ''}{d.rating ? `  ★ ${d.rating}` : ''}</Text>
                  {d.projectsCompleted ? <Text style={styles.designerMeta}>{d.projectsCompleted} projects</Text> : null}
                </View>
              ))}
            </ScrollView>
          </>
        ) : null}

        {/* Service packages */}
        <Text style={styles.sectionTitle}>Packages</Text>
        {SERVICE_PACKAGES.map((p) => (
          <View key={p.name} style={styles.pkgRow}>
            <Text style={styles.pkgName}>{p.name}</Text>
            <Text style={styles.pkgRange}>{p.range}</Text>
          </View>
        ))}

        {/* How it works */}
        <Text style={styles.sectionTitle}>How it works</Text>
        {HOW_IT_WORKS.map((s, i) => (
          <View key={s} style={styles.stepRow}>
            <View style={styles.stepCircle}><Text style={styles.stepNum}>{i + 1}</Text></View>
            <Text style={styles.stepLabel}>{s}</Text>
          </View>
        ))}

        {/* Materials */}
        <Text style={styles.sectionTitle}>Material catalog</Text>
        <View style={styles.pillRow}>
          {MATERIAL_CATEGORIES.map((c) => (
            <TouchableOpacity key={c} style={[styles.pill, materialCat === c && styles.pillActive]} onPress={() => setMaterialCat(c)}>
              <Text style={[styles.pillText, materialCat === c && styles.pillTextActive]}>{pretty(c)}</Text>
            </TouchableOpacity>
          ))}
        </View>
        {materials.length === 0 ? (
          <Text style={styles.muted}>No materials listed in this category yet.</Text>
        ) : (
          materials.map((m) => (
            <View key={m.id} style={styles.matRow}>
              <View style={{ flex: 1 }}>
                <Text style={styles.matName}>{m.name}</Text>
                {m.brand ? <Text style={styles.matBrand}>{m.brand}</Text> : null}
              </View>
              <Text style={styles.matPrice}>{formatPaise(m.unitPricePaise ?? m.pricePerUnitPaise ?? 0)}{m.unit ? `/${m.unit}` : ''}</Text>
            </View>
          ))
        )}

        <View style={{ height: 40 }} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#f9fafb' },
  hero:         { backgroundColor: '#7c3aed', paddingHorizontal: 20, paddingTop: 18, paddingBottom: 20 },
  heroTitle:    { fontSize: 22, fontWeight: '800', color: '#fff' },
  heroSub:      { fontSize: 13, color: '#ede9fe', marginTop: 6, lineHeight: 18 },
  scroll:       { padding: 16 },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: '#111827', marginTop: 22, marginBottom: 10 },
  card:         { backgroundColor: '#fff', borderRadius: 14, padding: 14, borderWidth: 1, borderColor: '#f3f4f6' },
  label:        { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 6 },
  input:        { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 11, fontSize: 14, color: '#111827', marginBottom: 14 },
  row:          { flexDirection: 'row', gap: 12 },
  pillRow:      { flexDirection: 'row', gap: 8, marginBottom: 14, flexWrap: 'wrap' },
  pill:         { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb', backgroundColor: '#fff' },
  pillActive:   { backgroundColor: '#7c3aed', borderColor: '#7c3aed' },
  pillText:     { fontSize: 12, fontWeight: '600', color: '#374151' },
  pillTextActive:{ color: '#fff' },
  primaryBtn:   { backgroundColor: '#f97316', borderRadius: 12, paddingVertical: 15, alignItems: 'center', marginTop: 4 },
  primaryBtnText:{ color: '#fff', fontSize: 16, fontWeight: '700' },
  successCard:  { backgroundColor: '#fff', borderRadius: 14, padding: 24, alignItems: 'center', borderWidth: 1, borderColor: '#f3f4f6' },
  successIcon:  { fontSize: 40 },
  successTitle: { fontSize: 17, fontWeight: '800', color: '#111827', marginTop: 8 },
  successSub:   { fontSize: 13, color: '#6b7280', marginTop: 4, textAlign: 'center' },
  projCard:     { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderRadius: 14, padding: 14, marginBottom: 10, borderWidth: 1, borderColor: '#f3f4f6' },
  projTitle:    { fontSize: 14, fontWeight: '700', color: '#111827' },
  projSub:      { fontSize: 12, color: '#6b7280', marginTop: 2, marginBottom: 6 },
  progressBg:   { height: 6, backgroundColor: '#f3f4f6', borderRadius: 100, overflow: 'hidden' },
  progressFill: { height: 6, backgroundColor: '#7c3aed', borderRadius: 100 },
  badge:        { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 100, marginLeft: 8 },
  badgeText:    { fontSize: 11, fontWeight: '700' },
  designerCard: { width: 130, backgroundColor: '#fff', borderRadius: 14, padding: 12, borderWidth: 1, borderColor: '#f3f4f6' },
  designerAvatar:{ width: 40, height: 40, borderRadius: 20, backgroundColor: '#ede9fe', alignItems: 'center', justifyContent: 'center', marginBottom: 8 },
  designerAvatarText: { fontSize: 18, fontWeight: '700', color: '#7c3aed' },
  designerName: { fontSize: 13, fontWeight: '700', color: '#111827' },
  designerMeta: { fontSize: 11, color: '#6b7280', marginTop: 2 },
  pkgRow:       { flexDirection: 'row', justifyContent: 'space-between', backgroundColor: '#fff', borderRadius: 10, padding: 12, marginBottom: 8, borderWidth: 1, borderColor: '#f3f4f6' },
  pkgName:      { fontSize: 13, fontWeight: '600', color: '#111827' },
  pkgRange:     { fontSize: 13, fontWeight: '700', color: '#7c3aed' },
  stepRow:      { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 10 },
  stepCircle:   { width: 28, height: 28, borderRadius: 14, backgroundColor: '#7c3aed', alignItems: 'center', justifyContent: 'center' },
  stepNum:      { color: '#fff', fontWeight: '800', fontSize: 13 },
  stepLabel:    { fontSize: 13, color: '#374151', fontWeight: '600', flex: 1 },
  muted:        { fontSize: 13, color: '#9ca3af' },
  matRow:       { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderRadius: 10, padding: 12, marginBottom: 8, borderWidth: 1, borderColor: '#f3f4f6' },
  matName:      { fontSize: 13, fontWeight: '600', color: '#111827' },
  matBrand:     { fontSize: 11, color: '#9ca3af', marginTop: 2 },
  matPrice:     { fontSize: 13, fontWeight: '700', color: '#7c3aed' },
});
