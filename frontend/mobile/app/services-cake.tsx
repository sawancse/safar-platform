import { useEffect, useState } from 'react';
import {
  View, Text, ScrollView, StyleSheet, ActivityIndicator, Image, TouchableOpacity, RefreshControl,
} from 'react-native';
import { useRouter, Stack } from 'expo-router';
import { api } from '@/lib/api';

const TRUST_BADGE: Record<string, { label: string; bg: string; text: string }> = {
  SAFAR_VERIFIED: { label: '✓ Verified', bg: '#dbeafe', text: '#1e40af' },
  TOP_RATED:      { label: '★ Top Rated', bg: '#fef3c7', text: '#92400e' },
};

export default function CakeVendorsScreen() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [vendors, setVendors] = useState<any[]>([]);

  const load = async () => {
    try {
      const v = await api.browseServiceListings({ serviceType: 'CAKE_DESIGNER' });
      setVendors(Array.isArray(v) ? v : []);
    } catch {
      setVendors([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={{ padding: 16, paddingBottom: 40 }}
      refreshControl={<RefreshControl refreshing={loading} onRefresh={() => { setLoading(true); load(); }} />}
    >
      <Stack.Screen options={{ title: 'Designer Cakes' }} />

      <Text style={styles.kicker}>VERIFIED LOCAL BAKERS</Text>
      <Text style={styles.title}>Cake makers near you</Text>
      <Text style={styles.subtitle}>Browse independent designer-cake studios on BhramanKaro.</Text>

      {loading ? (
        <View style={styles.center}><ActivityIndicator size="large" color="#f97316" /></View>
      ) : vendors.length === 0 ? (
        <View style={styles.center}>
          <Text style={styles.emptyIcon}>🎂</Text>
          <Text style={styles.emptyTitle}>No cake vendors yet</Text>
          <Text style={styles.muted}>Check back soon — bakers are joining every week.</Text>
        </View>
      ) : (
        vendors.map((v) => {
          const badge = v.trustTier && v.trustTier !== 'LISTED' ? TRUST_BADGE[v.trustTier] : null;
          return (
            <TouchableOpacity
              key={v.id}
              style={styles.card}
              activeOpacity={0.85}
              onPress={() => router.push(`/storefront/${v.vendorSlug}`)}
            >
              {v.heroImageUrl ? (
                <Image source={{ uri: v.heroImageUrl }} style={styles.hero} resizeMode="cover" />
              ) : (
                <View style={[styles.hero, styles.heroPlaceholder]}><Text style={styles.heroEmoji}>🎂</Text></View>
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
                  {v.homeCity ? <Text style={styles.meta}>📍 {v.homeCity}</Text> : null}
                  {v.ratingCount > 0 ? (
                    <Text style={styles.metaStar}>★ {Number(v.avgRating ?? 0).toFixed(1)} ({v.ratingCount})</Text>
                  ) : null}
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
  subtitle:     { fontSize: 13, color: '#6b7280', marginTop: 4, marginBottom: 16 },
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
