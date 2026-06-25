import { useEffect, useMemo, useState } from 'react';
import {
  View, Text, ScrollView, StyleSheet, ActivityIndicator, Image, TouchableOpacity, RefreshControl,
} from 'react-native';
import { useRouter, Stack } from 'expo-router';
import { api } from '@/lib/api';

const TYPE_META: Record<string, { emoji: string; label: string }> = {
  CAKE_DESIGNER:    { emoji: '🎂', label: 'Cakes' },
  DECORATOR:        { emoji: '🌸', label: 'Decor' },
  PANDIT:           { emoji: '🪔', label: 'Pandits' },
  SINGER:           { emoji: '🎤', label: 'Singers' },
  STAFF_HIRE:       { emoji: '🧑‍🍳', label: 'Staff' },
  APPLIANCE_RENTAL: { emoji: '🍳', label: 'Appliances' },
  COOK:             { emoji: '👨‍🍳', label: 'Cooks' },
};

const TRUST_BADGE: Record<string, { label: string; bg: string; text: string }> = {
  SAFAR_VERIFIED: { label: '✓ Verified', bg: '#dbeafe', text: '#1e40af' },
  TOP_RATED:      { label: '★ Top Rated', bg: '#fef3c7', text: '#92400e' },
};

export default function ServiceVendorsScreen() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [all, setAll] = useState<any[]>([]);
  const [filter, setFilter] = useState<string>('ALL');

  const load = async () => {
    try {
      const v = await api.browseServiceListings({});
      setAll(Array.isArray(v) ? v : []);
    } catch {
      setAll([]);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => { load(); }, []);

  const types = useMemo(() => Array.from(new Set(all.map(v => v.serviceType).filter(Boolean))), [all]);
  const vendors = filter === 'ALL' ? all : all.filter(v => v.serviceType === filter);

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={{ padding: 16, paddingBottom: 40 }}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={() => { setLoading(true); load(); }} />}
    >
      <Stack.Screen options={{ title: 'Service Vendors' }} />

      <Text style={styles.kicker}>VERIFIED LOCAL PARTNERS</Text>
      <Text style={styles.title}>Vendors on BhramanKaro</Text>
      <Text style={styles.subtitle}>Cakes, decor, pandits, performers and more — book direct.</Text>

      {types.length > 1 ? (
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipRow} contentContainerStyle={{ gap: 8 }}>
          {['ALL', ...types].map(t => {
            const meta = TYPE_META[t];
            const active = filter === t;
            return (
              <TouchableOpacity key={t} onPress={() => setFilter(t)} style={[styles.chip, active && styles.chipActive]}>
                <Text style={[styles.chipText, active && styles.chipTextActive]}>
                  {t === 'ALL' ? 'All' : `${meta?.emoji ?? ''} ${meta?.label ?? t}`}
                </Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      ) : null}

      {loading ? (
        <View style={styles.center}><ActivityIndicator size="large" color="#f97316" /></View>
      ) : vendors.length === 0 ? (
        <View style={styles.center}>
          <Text style={styles.emptyIcon}>🛍️</Text>
          <Text style={styles.emptyTitle}>No vendors yet</Text>
          <Text style={styles.muted}>Check back soon — partners are joining every week.</Text>
        </View>
      ) : (
        vendors.map((v) => {
          const meta = TYPE_META[v.serviceType] ?? { emoji: '🏷️', label: v.serviceType };
          const badge = v.trustTier && v.trustTier !== 'LISTED' ? TRUST_BADGE[v.trustTier] : null;
          return (
            <TouchableOpacity key={v.id} style={styles.card} activeOpacity={0.85} onPress={() => router.push(`/storefront/${v.vendorSlug}`)}>
              {v.heroImageUrl ? (
                <Image source={{ uri: v.heroImageUrl }} style={styles.hero} resizeMode="cover" />
              ) : (
                <View style={[styles.hero, styles.heroPlaceholder]}><Text style={styles.heroEmoji}>{meta.emoji}</Text></View>
              )}
              <View style={styles.cardBody}>
                <View style={styles.titleRow}>
                  <Text style={styles.bizName} numberOfLines={1}>{v.businessName}</Text>
                  {badge ? (
                    <View style={[styles.badge, { backgroundColor: badge.bg }]}>
                      <Text style={[styles.badgeText, { color: badge.text }]}>{badge.label}</Text>
                    </View>
                  ) : null}
                </View>
                {v.tagline ? <Text style={styles.tagline} numberOfLines={1}>{v.tagline}</Text> : null}
                <View style={styles.metaRow}>
                  <Text style={styles.meta}>{meta.emoji} {meta.label}</Text>
                  {v.homeCity ? <Text style={styles.meta}>📍 {v.homeCity}</Text> : null}
                  {v.ratingCount > 0 ? <Text style={styles.metaStar}>★ {Number(v.avgRating ?? 0).toFixed(1)} ({v.ratingCount})</Text> : null}
                </View>
                <Text style={styles.cta}>View storefront →</Text>
              </View>
            </TouchableOpacity>
          );
        })
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#f9fafb' },
  kicker:       { fontSize: 11, fontWeight: '700', letterSpacing: 2, color: '#e11d48' },
  title:        { fontSize: 24, fontWeight: '800', color: '#111827', marginTop: 4 },
  subtitle:     { fontSize: 13, color: '#6b7280', marginTop: 4, marginBottom: 12 },
  chipRow:      { marginBottom: 14 },
  chip:         { paddingHorizontal: 14, paddingVertical: 7, borderRadius: 100, backgroundColor: '#f3f4f6' },
  chipActive:   { backgroundColor: '#e11d48' },
  chipText:     { fontSize: 13, fontWeight: '600', color: '#374151' },
  chipTextActive: { color: '#fff' },
  center:       { alignItems: 'center', justifyContent: 'center', paddingVertical: 60 },
  emptyIcon:    { fontSize: 44, marginBottom: 10 },
  emptyTitle:   { fontSize: 17, fontWeight: '700', color: '#374151' },
  muted:        { fontSize: 13, color: '#9ca3af', marginTop: 4 },
  card:         { backgroundColor: '#fff', borderRadius: 16, overflow: 'hidden', borderWidth: 1, borderColor: '#f3f4f6', marginBottom: 14 },
  hero:         { width: '100%', height: 150, backgroundColor: '#f3f4f6' },
  heroPlaceholder: { alignItems: 'center', justifyContent: 'center' },
  heroEmoji:    { fontSize: 44 },
  cardBody:     { padding: 14 },
  titleRow:     { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  bizName:      { fontSize: 16, fontWeight: '800', color: '#111827', flex: 1 },
  badge:        { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 100 },
  badgeText:    { fontSize: 10, fontWeight: '700' },
  tagline:      { fontSize: 12, color: '#6b7280', marginTop: 2 },
  metaRow:      { flexDirection: 'row', flexWrap: 'wrap', gap: 12, marginTop: 8 },
  meta:         { fontSize: 12, color: '#6b7280', fontWeight: '600' },
  metaStar:     { fontSize: 12, color: '#d97706', fontWeight: '700' },
  cta:          { fontSize: 13, fontWeight: '700', color: '#e11d48', marginTop: 10 },
});
