import { useEffect, useState } from 'react';
import {
  View, Text, ScrollView, StyleSheet, ActivityIndicator, Image,
} from 'react-native';
import { useLocalSearchParams, Stack } from 'expo-router';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';

const TYPE_EMOJI: Record<string, string> = {
  CAKE_DESIGNER: '🎂', SINGER: '🎤', PANDIT: '🪔', DECORATOR: '🌸',
  STAFF_HIRE: '🧑‍🍳', COOK: '👨‍🍳', APPLIANCE_RENTAL: '🍳',
};

const TRUST_BADGE: Record<string, { label: string; bg: string; text: string }> = {
  LISTED:         { label: 'Listed', bg: '#f3f4f6', text: '#374151' },
  SAFAR_VERIFIED: { label: '✓ BhramanKaro Verified', bg: '#dbeafe', text: '#1e40af' },
  TOP_RATED:      { label: '★ Top Rated', bg: '#fef3c7', text: '#92400e' },
};

function stars(n: number) {
  const r = Math.max(0, Math.min(5, Math.round(n)));
  return '★'.repeat(r) + '☆'.repeat(5 - r);
}

export default function StorefrontScreen() {
  const { slug } = useLocalSearchParams<{ slug: string }>();
  const [loading, setLoading] = useState(true);
  const [listing, setListing] = useState<any>(null);
  const [items, setItems] = useState<any[]>([]);
  const [reviews, setReviews] = useState<any[]>([]);

  useEffect(() => {
    (async () => {
      if (!slug) return;
      try {
        const l = await api.getStorefront(slug);
        setListing(l);
        if (l?.id) {
          const [it, rv] = await Promise.all([
            api.getStorefrontItems(l.id),
            api.getStorefrontReviews(l.id),
          ]);
          setItems(it ?? []);
          setReviews(rv ?? []);
        }
      } catch {
        setListing(null);
      } finally {
        setLoading(false);
      }
    })();
  }, [slug]);

  if (loading) {
    return <View style={styles.center}><ActivityIndicator size="large" color="#f97316" /></View>;
  }
  if (!listing) {
    return (
      <View style={styles.center}>
        <Text style={styles.emptyIcon}>🔍</Text>
        <Text style={styles.emptyTitle}>Storefront not found</Text>
      </View>
    );
  }

  const badge = TRUST_BADGE[listing.trustTier] ?? TRUST_BADGE.LISTED;
  const emoji = TYPE_EMOJI[listing.serviceType] ?? '🛍️';

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ paddingBottom: 40 }}>
      <Stack.Screen options={{ title: listing.businessName ?? 'Storefront' }} />

      {listing.heroImageUrl ? (
        <Image source={{ uri: listing.heroImageUrl }} style={styles.hero} resizeMode="cover" />
      ) : null}

      <View style={styles.body}>
        <View style={styles.headerRow}>
          <View style={styles.emojiBox}><Text style={styles.emoji}>{emoji}</Text></View>
          <View style={{ flex: 1 }}>
            <Text style={styles.bizName}>{listing.businessName}</Text>
            {listing.tagline ? <Text style={styles.tagline}>{listing.tagline}</Text> : null}
          </View>
        </View>

        <View style={styles.trustRow}>
          <View style={[styles.badge, { backgroundColor: badge.bg }]}><Text style={[styles.badgeText, { color: badge.text }]}>{badge.label}</Text></View>
          {listing.ratingCount > 0 ? (
            <Text style={styles.metaText}>★ {Number(listing.avgRating ?? 0).toFixed(1)} ({listing.ratingCount})</Text>
          ) : null}
          {listing.completedBookingsCount ? <Text style={styles.metaText}>· {listing.completedBookingsCount} completed</Text> : null}
          {listing.homeCity ? <Text style={styles.metaText}>· {listing.homeCity}</Text> : null}
          {listing.outstationCapable ? <Text style={styles.metaGreen}>· travels outstation</Text> : null}
        </View>

        {listing.aboutMd ? (
          <>
            <Text style={styles.sectionTitle}>About</Text>
            <Text style={styles.about}>{listing.aboutMd}</Text>
          </>
        ) : null}

        {/* Items */}
        <Text style={styles.sectionTitle}>{`What ${listing.businessName} offers`}</Text>
        {items.length === 0 ? (
          <Text style={styles.muted}>This vendor takes direct booking requests.</Text>
        ) : (
          items.map((it) => (
            <View key={it.id} style={styles.itemCard}>
              {(it.heroPhotoUrl || (it.photos && it.photos[0])) ? (
                <Image source={{ uri: it.heroPhotoUrl ?? it.photos[0] }} style={styles.itemPhoto} resizeMode="cover" />
              ) : <View style={[styles.itemPhoto, styles.itemPhotoPlaceholder]}><Text style={styles.emoji}>{emoji}</Text></View>}
              <View style={styles.itemBody}>
                <Text style={styles.itemTitle} numberOfLines={2}>{it.title}</Text>
                {it.descriptionMd ? <Text style={styles.itemDesc} numberOfLines={2}>{it.descriptionMd}</Text> : null}
                <Text style={styles.itemPrice}>{formatPaise(it.basePricePaise ?? 0)}</Text>
                {it.leadTimeHours ? <Text style={styles.itemLead}>{it.leadTimeHours}h lead time</Text> : null}
                {Array.isArray(it.occasionTags) && it.occasionTags.length > 0 ? (
                  <View style={styles.tagRow}>
                    {it.occasionTags.slice(0, 3).map((t: string) => <Text key={t} style={styles.tag}>{t}</Text>)}
                  </View>
                ) : null}
              </View>
            </View>
          ))
        )}

        {/* Reviews */}
        {reviews.length > 0 ? (
          <>
            <Text style={styles.sectionTitle}>Reviews ({reviews.length})</Text>
            {reviews.map((r, idx) => (
              <View key={idx} style={styles.reviewCard}>
                <View style={styles.reviewHead}>
                  <View style={styles.reviewAvatar}><Text style={styles.reviewAvatarText}>{(r.reviewerName ?? 'V').charAt(0).toUpperCase()}</Text></View>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.reviewName}>{r.reviewerName ?? 'Verified customer'}</Text>
                    {(r.eventType || r.date) ? (
                      <Text style={styles.reviewMeta}>{[r.eventType, r.date ? new Date(r.date).toLocaleDateString('en-IN') : null].filter(Boolean).join(' · ')}</Text>
                    ) : null}
                  </View>
                  <Text style={styles.reviewStars}>{stars(r.rating ?? 0)}</Text>
                </View>
                <Text style={r.comment ? styles.reviewComment : styles.reviewNoComment}>{r.comment || '(No written comment)'}</Text>
              </View>
            ))}
          </>
        ) : null}

        {/* Service area & policy */}
        <Text style={styles.sectionTitle}>Service area</Text>
        <Text style={styles.about}>
          {Array.isArray(listing.cities) && listing.cities.length > 0 ? listing.cities.join(', ') : (listing.homeCity ?? 'Local only')}
        </Text>
        {listing.outstationCapable ? <Text style={styles.metaGreen}>✓ Available for outstation events</Text> : null}
        {listing.cancellationPolicy ? (
          <>
            <Text style={styles.sectionTitle}>Cancellation</Text>
            <Text style={styles.about}>{listing.cancellationPolicy}</Text>
          </>
        ) : null}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#f9fafb' },
  center:       { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb', padding: 24 },
  emptyIcon:    { fontSize: 44, marginBottom: 10 },
  emptyTitle:   { fontSize: 17, fontWeight: '700', color: '#374151' },
  hero:         { width: '100%', height: 180, backgroundColor: '#e5e7eb' },
  body:         { padding: 16 },
  headerRow:    { flexDirection: 'row', alignItems: 'center', gap: 12 },
  emojiBox:     { width: 56, height: 56, borderRadius: 14, backgroundColor: '#fff7ed', alignItems: 'center', justifyContent: 'center' },
  emoji:        { fontSize: 28 },
  bizName:      { fontSize: 20, fontWeight: '800', color: '#111827' },
  tagline:      { fontSize: 13, color: '#6b7280', marginTop: 2 },
  trustRow:     { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: 8, marginTop: 12 },
  badge:        { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 100 },
  badgeText:    { fontSize: 11, fontWeight: '700' },
  metaText:     { fontSize: 12, color: '#6b7280', fontWeight: '600' },
  metaGreen:    { fontSize: 12, color: '#16a34a', fontWeight: '600' },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: '#111827', marginTop: 22, marginBottom: 8 },
  about:        { fontSize: 13, color: '#374151', lineHeight: 20 },
  muted:        { fontSize: 13, color: '#9ca3af' },
  itemCard:     { flexDirection: 'row', gap: 12, backgroundColor: '#fff', borderRadius: 14, padding: 12, marginBottom: 10, borderWidth: 1, borderColor: '#f3f4f6' },
  itemPhoto:    { width: 84, height: 84, borderRadius: 10, backgroundColor: '#f3f4f6' },
  itemPhotoPlaceholder: { alignItems: 'center', justifyContent: 'center' },
  itemBody:     { flex: 1 },
  itemTitle:    { fontSize: 14, fontWeight: '700', color: '#111827' },
  itemDesc:     { fontSize: 12, color: '#6b7280', marginTop: 2, lineHeight: 16 },
  itemPrice:    { fontSize: 14, fontWeight: '800', color: '#f97316', marginTop: 6 },
  itemLead:     { fontSize: 11, color: '#9ca3af', marginTop: 2 },
  tagRow:       { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 6 },
  tag:          { fontSize: 10, color: '#6b7280', backgroundColor: '#f3f4f6', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 100, overflow: 'hidden' },
  reviewCard:   { backgroundColor: '#fff', borderRadius: 14, padding: 14, marginBottom: 10, borderWidth: 1, borderColor: '#f3f4f6' },
  reviewHead:   { flexDirection: 'row', alignItems: 'center', gap: 10 },
  reviewAvatar: { width: 34, height: 34, borderRadius: 17, backgroundColor: '#f97316', alignItems: 'center', justifyContent: 'center' },
  reviewAvatarText: { color: '#fff', fontWeight: '700', fontSize: 14 },
  reviewName:   { fontSize: 13, fontWeight: '700', color: '#111827' },
  reviewMeta:   { fontSize: 11, color: '#9ca3af', marginTop: 1 },
  reviewStars:  { fontSize: 13, color: '#f59e0b' },
  reviewComment:{ fontSize: 13, color: '#374151', marginTop: 8, lineHeight: 19 },
  reviewNoComment: { fontSize: 13, color: '#9ca3af', fontStyle: 'italic', marginTop: 8 },
});
