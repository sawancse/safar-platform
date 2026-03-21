import { useEffect, useState } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity,
  StyleSheet, ActivityIndicator, Dimensions, Image, Alert,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import Constants from 'expo-constants';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import { getAccessToken } from '@/lib/auth';

const API_URL = Constants.expoConfig?.extra?.apiUrl ?? 'http://localhost:8080';

const SCREEN_WIDTH = Dimensions.get('window').width;

export default function ListingDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const [listing, setListing] = useState<any>(null);
  const [reviews, setReviews] = useState<any[]>([]);
  const [photos, setPhotos] = useState<string[]>([]);
  const [photoIndex, setPhotoIndex] = useState(0);
  const [loading, setLoading] = useState(true);
  const [isSaved, setIsSaved] = useState(false);
  const [savingToggle, setSavingToggle] = useState(false);

  useEffect(() => {
    if (!id) return;
    Promise.all([
      api.getListing(id).then(setListing),
      api.getListingReviews(id).then(setReviews).catch(() => []),
      // Fetch all media photos
      fetch(`${API_URL}/api/v1/listings/${id}/media`)
        .then(r => r.ok ? r.json() : [])
        .then((media: any[]) => {
          const urls = media
            .filter(m => m.type === 'PHOTO' || m.type === 'IMAGE')
            .map(m => m.url?.startsWith('http') ? m.url : API_URL + m.url)
            .filter(Boolean);
          setPhotos(urls);
        })
        .catch(() => {}),
    ]).finally(() => setLoading(false));

    // Check if listing is in bucket list
    (async () => {
      const token = await getAccessToken();
      if (!token) return;
      try {
        const bucketList = await api.getBucketList(token);
        const found = (bucketList ?? []).some(
          (item: any) => (item.listingId ?? item.id) === id,
        );
        setIsSaved(found);
      } catch {}
    })();
  }, [id]);

  async function toggleSaved() {
    const token = await getAccessToken();
    if (!token) {
      router.push('/auth');
      return;
    }
    if (!id || savingToggle) return;
    setSavingToggle(true);
    try {
      if (isSaved) {
        await api.removeFromBucketList(id, token);
        setIsSaved(false);
      } else {
        await api.addToBucketList(id, token);
        setIsSaved(true);
      }
    } catch (err: any) {
      Alert.alert('Error', err.message ?? 'Failed to update saved list');
    } finally {
      setSavingToggle(false);
    }
  }

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#f97316" />
      </View>
    );
  }

  if (!listing) {
    return (
      <View style={styles.center}>
        <Text style={{ color: '#9ca3af' }}>Listing not found</Text>
      </View>
    );
  }

  return (
    <View style={{ flex: 1 }}>
      <ScrollView style={styles.container} contentContainerStyle={{ paddingBottom: 100 }}>
        {/* Listing media — swipeable gallery */}
        <View>
          {(() => {
            const displayPhotos = photos.length > 0
              ? photos
              : listing.primaryPhotoUrl
                ? [listing.primaryPhotoUrl.startsWith('http') ? listing.primaryPhotoUrl : API_URL + listing.primaryPhotoUrl]
                : [];
            return displayPhotos.length > 0 ? (
              <View>
                <ScrollView
                  horizontal pagingEnabled
                  showsHorizontalScrollIndicator={false}
                  onMomentumScrollEnd={(e) => {
                    const idx = Math.round(e.nativeEvent.contentOffset.x / SCREEN_WIDTH);
                    setPhotoIndex(idx);
                  }}
                >
                  {displayPhotos.map((url, i) => (
                    <Image key={i} source={{ uri: url }}
                      style={{ width: SCREEN_WIDTH, height: 260 }} resizeMode="cover" />
                  ))}
                </ScrollView>
                {/* Dots */}
                {displayPhotos.length > 1 && (
                  <View style={styles.dotsRow}>
                    {displayPhotos.map((_, i) => (
                      <View key={i} style={[styles.dot, i === photoIndex && styles.dotActive]} />
                    ))}
                  </View>
                )}
                {/* Photo counter */}
                <View style={styles.photoCounter}>
                  <Text style={styles.photoCounterText}>{photoIndex + 1}/{displayPhotos.length}</Text>
                </View>
              </View>
            ) : (
              <View style={styles.media}>
                <Text style={{ fontSize: 60 }}>🏠</Text>
              </View>
            );
          })()}
          <TouchableOpacity
            style={styles.heartBtn}
            onPress={toggleSaved}
            disabled={savingToggle}
            activeOpacity={0.7}
          >
            <Text style={[styles.heartIcon, isSaved && styles.heartSaved]}>
              {isSaved ? '\u2665' : '\u2661'}
            </Text>
          </TouchableOpacity>
        </View>

        {/* Badges */}
        <View style={styles.badges}>
          {listing.status === 'VERIFIED' && (
            <Text style={[styles.badge, styles.badgeVerified]}>✓ Verified</Text>
          )}
          {listing.instantBook && (
            <Text style={[styles.badge, styles.badgeInstant]}>⚡ Instant Book</Text>
          )}
          <Text style={[styles.badge, styles.badgeType]}>{listing.type}</Text>
        </View>

        <Text style={styles.title}>{listing.title}</Text>
        <Text style={styles.location}>{listing.city}, {listing.state}</Text>

        {/* Stats */}
        <View style={styles.stats}>
          {listing.maxGuests && <Text style={styles.stat}>👥 Up to {listing.maxGuests}</Text>}
          {listing.bedrooms != null && <Text style={styles.stat}>🛏 {listing.bedrooms} br</Text>}
          {listing.bathrooms != null && <Text style={styles.stat}>🚿 {listing.bathrooms} ba</Text>}
          {listing.avgRating != null && listing.avgRating > 0 && (
            <Text style={styles.stat}>★ {listing.avgRating.toFixed(1)}</Text>
          )}
        </View>

        {/* Description */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>About this place</Text>
          <Text style={styles.description}>{listing.description}</Text>
        </View>

        {/* Amenities */}
        {listing.amenities?.length > 0 && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Amenities</Text>
            <View style={styles.amenities}>
              {listing.amenities.map((a: string) => (
                <Text key={a} style={styles.amenity}>{a.replace(/_/g, ' ')}</Text>
              ))}
            </View>
          </View>
        )}

        {/* Reviews */}
        {reviews.length > 0 && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Reviews ({reviews.length})</Text>
            {reviews.slice(0, 5).map((r: any) => (
              <View key={r.id} style={styles.review}>
                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                  <View style={{ width: 28, height: 28, borderRadius: 14, backgroundColor: '#fed7aa', alignItems: 'center', justifyContent: 'center' }}>
                    <Text style={{ fontSize: 12, fontWeight: '700', color: '#ea580c' }}>{(r.guestName || 'G')[0].toUpperCase()}</Text>
                  </View>
                  <Text style={{ fontSize: 13, fontWeight: '600', color: '#1f2937' }}>{r.guestName || 'Guest'}</Text>
                </View>
                <Text style={styles.reviewStars}>{'★'.repeat(r.rating)}{'☆'.repeat(5 - r.rating)}</Text>
                {r.comment ? <Text style={styles.reviewComment}>{r.comment}</Text> : null}
                {r.reply ? (
                  <View style={{ backgroundColor: '#f9fafb', borderRadius: 10, padding: 10, marginTop: 8 }}>
                    <Text style={{ fontSize: 11, fontWeight: '600', color: '#6b7280', marginBottom: 2 }}>Host reply</Text>
                    <Text style={{ fontSize: 13, color: '#4b5563', lineHeight: 19 }}>{r.reply}</Text>
                  </View>
                ) : null}
              </View>
            ))}
          </View>
        )}
      </ScrollView>

      {/* Bottom CTA */}
      <View style={styles.cta}>
        <View>
          <Text style={styles.ctaPrice}>{formatPaise(listing.basePricePaise)}</Text>
          <Text style={styles.ctaPerNight}>/ night</Text>
        </View>
        <TouchableOpacity
          style={styles.ctaBtn}
          onPress={() => router.push(`/book/${listing.id}`)}
        >
          <Text style={styles.ctaBtnText}>Reserve</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#fff' },
  center:       { flex: 1, alignItems: 'center', justifyContent: 'center' },
  media:        { width: SCREEN_WIDTH, height: 260, backgroundColor: '#f3f4f6', alignItems: 'center', justifyContent: 'center' },
  mediaImage:   { width: SCREEN_WIDTH, height: 260 },
  dotsRow:      { position: 'absolute', bottom: 12, left: 0, right: 0, flexDirection: 'row', justifyContent: 'center', gap: 4 },
  dot:          { width: 6, height: 6, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.5)' },
  dotActive:    { backgroundColor: '#fff', width: 8, height: 8, borderRadius: 4 },
  photoCounter: { position: 'absolute', bottom: 12, right: 16, backgroundColor: 'rgba(0,0,0,0.6)', borderRadius: 10, paddingHorizontal: 8, paddingVertical: 3 },
  photoCounterText: { color: '#fff', fontSize: 11, fontWeight: '600' },
  heartBtn:     { position: 'absolute', top: 44, right: 16, width: 40, height: 40, borderRadius: 20, backgroundColor: 'rgba(255,255,255,0.85)', alignItems: 'center', justifyContent: 'center' },
  heartIcon:    { fontSize: 24, color: '#9ca3af' },
  heartSaved:   { color: '#ef4444' },
  badges:       { flexDirection: 'row', flexWrap: 'wrap', gap: 6, padding: 16, paddingBottom: 0 },
  badge:        { fontSize: 11, fontWeight: '600', paddingHorizontal: 8, paddingVertical: 4, borderRadius: 100 },
  badgeVerified:{ backgroundColor: '#dcfce7', color: '#15803d' },
  badgeInstant: { backgroundColor: '#dbeafe', color: '#1d4ed8' },
  badgeType:    { backgroundColor: '#f3f4f6', color: '#4b5563' },
  title:        { fontSize: 20, fontWeight: '700', color: '#111827', paddingHorizontal: 16, marginTop: 8 },
  location:     { fontSize: 13, color: '#6b7280', paddingHorizontal: 16, marginTop: 2 },
  stats:        { flexDirection: 'row', gap: 16, paddingHorizontal: 16, marginTop: 12, paddingBottom: 12, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  stat:         { fontSize: 13, color: '#4b5563' },
  section:      { paddingHorizontal: 16, paddingTop: 16 },
  sectionTitle: { fontSize: 16, fontWeight: '600', color: '#111827', marginBottom: 8 },
  description:  { fontSize: 14, color: '#374151', lineHeight: 22 },
  amenities:    { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  amenity:      { backgroundColor: '#f3f4f6', color: '#374151', fontSize: 12, paddingHorizontal: 10, paddingVertical: 5, borderRadius: 100 },
  review:       { marginBottom: 12, paddingBottom: 12, borderBottomWidth: 1, borderBottomColor: '#f9fafb' },
  reviewStars:  { color: '#facc15', fontSize: 14, marginBottom: 4 },
  reviewComment:{ fontSize: 13, color: '#374151', lineHeight: 20 },
  cta:          { position: 'absolute', bottom: 0, left: 0, right: 0, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', backgroundColor: '#fff', padding: 16, borderTopWidth: 1, borderTopColor: '#f3f4f6' },
  ctaPrice:     { fontSize: 18, fontWeight: '700', color: '#111827' },
  ctaPerNight:  { fontSize: 12, color: '#6b7280' },
  ctaBtn:       { backgroundColor: '#f97316', borderRadius: 12, paddingHorizontal: 28, paddingVertical: 14 },
  ctaBtnText:   { color: '#fff', fontWeight: '700', fontSize: 15 },
});
