import { useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet } from 'react-native';
import { useRouter, Stack } from 'expo-router';
import { formatPaise } from '@/lib/utils';
import { OCCASIONS, PUJAS, pujasForOccasion } from '@/lib/panditCatalog';

const STEPS = [
  { icon: '🪔', title: 'Pick your puja',     body: 'Browse by occasion. Samagri is included.' },
  { icon: '📅', title: 'Date + 60% advance', body: 'Choose a date & slot, pay a secure advance.' },
  { icon: '📞', title: 'Confirmation call',  body: 'We validate gotra, language and requests.' },
  { icon: '🙏', title: 'Pandit arrives',     body: 'On-time, with the kit, in your language.' },
];

export default function PanditScreen() {
  const router = useRouter();
  const [active, setActive] = useState('HOUSEWARMING');
  const pujas = pujasForOccasion(active);
  const activeOcc = OCCASIONS.find(o => o.key === active)!;

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ paddingBottom: 40 }}>
      <Stack.Screen options={{ title: 'Pandit & Puja' }} />

      {/* Hero */}
      <View style={styles.hero}>
        <Text style={styles.heroKicker}>AUTHENTIC · SHASTRA-BACKED</Text>
        <Text style={styles.heroTitle}>Pandit & Puja Services</Text>
        <Text style={styles.heroSub}>Verified, multi-lingual pandits for every occasion. Samagri kit included. ₹1,500 onwards.</Text>
        <View style={styles.heroChips}>
          <Text style={styles.heroChip}>✓ Samagri included</Text>
          <Text style={styles.heroChip}>✓ 11 languages</Text>
          <Text style={styles.heroChip}>✓ Vedic-certified</Text>
        </View>
      </View>

      {/* Trust band */}
      <View style={styles.trustBand}>
        {[['500+', 'Puja types'], ['2L+', 'Pujas done'], ['11', 'Languages']].map(([n, l]) => (
          <View key={l} style={styles.trustItem}>
            <Text style={styles.trustNum}>{n}</Text>
            <Text style={styles.trustLbl}>{l}</Text>
          </View>
        ))}
      </View>

      {/* Occasion pills */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.pillRow}>
        {OCCASIONS.map(o => (
          <TouchableOpacity key={o.key} style={[styles.pill, active === o.key && styles.pillActive]} onPress={() => setActive(o.key)}>
            <Text style={[styles.pillText, active === o.key && styles.pillTextActive]}>{o.icon} {o.label}</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      {/* Puja list */}
      <View style={{ paddingHorizontal: 16 }}>
        <Text style={styles.sectionTitle}>{activeOcc.icon} {activeOcc.label}</Text>
        <Text style={styles.sectionSub}>{pujas.length} pujas · all include samagri kit</Text>
        {pujas.map(p => (
          <TouchableOpacity key={p.key} style={styles.card}
            onPress={() => router.push(`/pandit-book?puja=${p.key}`)}>
            <View style={{ flex: 1 }}>
              <View style={styles.cardTitleRow}>
                <Text style={styles.cardTitle}>{p.label}</Text>
                <Text style={[styles.tierTag, p.tier === 'LUXURY' ? styles.tierLux : p.tier === 'PREMIUM' ? styles.tierPrem : styles.tierStd]}>{p.tier}</Text>
              </View>
              <Text style={styles.cardSub} numberOfLines={1}>{p.inclusions[0]}</Text>
              <View style={styles.cardMetaRow}>
                <Text style={styles.cardPrice}>from {formatPaise(p.pricePaise)}</Text>
                <Text style={styles.cardMeta}>{p.durationHours}h · +dakshina {formatPaise(p.recommendedDakshinaPaise)}</Text>
              </View>
            </View>
            <Text style={styles.chevron}>›</Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* How it works */}
      <View style={styles.howWrap}>
        <Text style={styles.sectionTitle}>How it works</Text>
        {STEPS.map((s, i) => (
          <View key={i} style={styles.stepRow}>
            <Text style={styles.stepIcon}>{s.icon}</Text>
            <View style={{ flex: 1 }}>
              <Text style={styles.stepTitle}>{i + 1}. {s.title}</Text>
              <Text style={styles.stepBody}>{s.body}</Text>
            </View>
          </View>
        ))}
      </View>

      <TouchableOpacity style={styles.cta} onPress={() => router.push('/pandit-book')}>
        <Text style={styles.ctaText}>Book a pandit →</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container:   { flex: 1, backgroundColor: '#f9fafb' },
  hero:        { backgroundColor: '#fff7ed', padding: 20, paddingTop: 24 },
  heroKicker:  { fontSize: 10, fontWeight: '700', letterSpacing: 2, color: '#c2410c' },
  heroTitle:   { fontSize: 26, fontWeight: '800', color: '#111827', marginTop: 8 },
  heroSub:     { fontSize: 14, color: '#374151', marginTop: 8, lineHeight: 20 },
  heroChips:   { flexDirection: 'row', flexWrap: 'wrap', gap: 12, marginTop: 12 },
  heroChip:    { fontSize: 12, color: '#374151', fontWeight: '600' },
  trustBand:   { flexDirection: 'row', backgroundColor: '#111827', paddingVertical: 16 },
  trustItem:   { flex: 1, alignItems: 'center' },
  trustNum:    { fontSize: 20, fontWeight: '800', color: '#fbbf24' },
  trustLbl:    { fontSize: 10, color: '#d1d5db', marginTop: 2, textTransform: 'uppercase' },
  pillRow:     { paddingHorizontal: 16, paddingVertical: 12, gap: 8 },
  pill:        { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb', backgroundColor: '#fff' },
  pillActive:  { backgroundColor: '#ea580c', borderColor: '#ea580c' },
  pillText:    { fontSize: 12, fontWeight: '600', color: '#374151' },
  pillTextActive: { color: '#fff' },
  sectionTitle:{ fontSize: 18, fontWeight: '800', color: '#111827', marginTop: 8 },
  sectionSub:  { fontSize: 12, color: '#6b7280', marginTop: 2, marginBottom: 12 },
  card:        { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', borderRadius: 14, padding: 14, marginBottom: 10, borderWidth: 1, borderColor: '#f3f4f6' },
  cardTitleRow:{ flexDirection: 'row', alignItems: 'center', gap: 8 },
  cardTitle:   { fontSize: 15, fontWeight: '700', color: '#111827', flexShrink: 1 },
  tierTag:     { fontSize: 9, fontWeight: '800', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 6, overflow: 'hidden' },
  tierStd:     { backgroundColor: '#f3f4f6', color: '#374151' },
  tierPrem:    { backgroundColor: '#f59e0b', color: '#fff' },
  tierLux:     { backgroundColor: '#7c3aed', color: '#fff' },
  cardSub:     { fontSize: 12, color: '#6b7280', marginTop: 3 },
  cardMetaRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 6 },
  cardPrice:   { fontSize: 14, fontWeight: '800', color: '#111827' },
  cardMeta:    { fontSize: 10, color: '#9ca3af' },
  chevron:     { fontSize: 26, color: '#d1d5db', marginLeft: 8 },
  howWrap:     { padding: 16, marginTop: 8 },
  stepRow:     { flexDirection: 'row', gap: 12, alignItems: 'flex-start', marginTop: 12 },
  stepIcon:    { fontSize: 24 },
  stepTitle:   { fontSize: 14, fontWeight: '700', color: '#111827' },
  stepBody:    { fontSize: 12, color: '#6b7280', marginTop: 2, lineHeight: 17 },
  cta:         { backgroundColor: '#ea580c', borderRadius: 12, paddingVertical: 15, alignItems: 'center', margin: 16 },
  ctaText:     { color: '#fff', fontSize: 16, fontWeight: '700' },
});
