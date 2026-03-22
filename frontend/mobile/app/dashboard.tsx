import { useEffect, useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const STATUS_STYLE: Record<string, { label: string; bg: string; text: string }> = {
  CONFIRMED:  { label: 'Confirmed',  bg: '#dcfce7', text: '#14532d' },
  CHECKED_IN: { label: 'Checked In', bg: '#dbeafe', text: '#1e40af' },
};

interface QuickAction {
  label: string;
  icon: string;
  route: string;
}

const QUICK_ACTIONS: QuickAction[] = [
  { label: 'Search Stays', icon: '🔍', route: '/' },
  { label: 'My Saved',     icon: '♡',  route: '/saved' },
  { label: 'Messages',     icon: '💬', route: '/messages' },
  { label: 'Miles',        icon: '✈',  route: '/miles' },
];

function formatDate(dateStr: string): string {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

function renderStars(rating: number): string {
  const full = Math.round(rating);
  return '★'.repeat(full) + '☆'.repeat(5 - full);
}

function computeProfileCompleteness(profile: any): number {
  if (!profile) return 0;
  let filled = 0;
  const fields = ['name', 'email', 'phone', 'avatarUrl', 'idVerified'];
  for (const f of fields) {
    if (f === 'idVerified') {
      if (profile.idVerified || profile.kycStatus === 'VERIFIED') filled++;
    } else if (profile[f]) {
      filled++;
    }
  }
  return Math.round((filled / fields.length) * 100);
}

export default function DashboardScreen() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState<any>(null);
  const [upcomingTrips, setUpcomingTrips] = useState<any[]>([]);
  const [recentReviews, setRecentReviews] = useState<any[]>([]);
  const [milesBalance, setMilesBalance] = useState(0);
  const [savedCount, setSavedCount] = useState(0);
  const [totalTrips, setTotalTrips] = useState(0);
  const [reviewCount, setReviewCount] = useState(0);

  useEffect(() => {
    (async () => {
      const token = await getAccessToken();
      if (!token) {
        router.replace('/auth');
        return;
      }

      try {
        const [profileData, bookingsData, reviewsData, milesData, savedData] =
          await Promise.allSettled([
            api.getMyProfile(token),
            api.getMyBookings(token),
            api.getMyReviews(token),
            api.getMilesBalance(token),
            api.getBucketList(token),
          ]);

        if (profileData.status === 'fulfilled') {
          setProfile(profileData.value);
        }

        if (bookingsData.status === 'fulfilled') {
          const bookings = bookingsData.value ?? [];
          setTotalTrips(bookings.length);
          const upcoming = bookings
            .filter((b: any) => b.status === 'CONFIRMED' || b.status === 'CHECKED_IN')
            .sort((a: any, b: any) =>
              new Date(a.checkInDate).getTime() - new Date(b.checkInDate).getTime()
            )
            .slice(0, 3);
          setUpcomingTrips(upcoming);
        }

        if (reviewsData.status === 'fulfilled') {
          const reviews = reviewsData.value?.content ?? reviewsData.value ?? [];
          setReviewCount(reviews.length);
          setRecentReviews(reviews.slice(0, 3));
        }

        if (milesData.status === 'fulfilled') {
          setMilesBalance(milesData.value?.balance ?? 0);
        }

        if (savedData.status === 'fulfilled') {
          const saved = savedData.value ?? [];
          setSavedCount(saved.length);
        }
      } catch {
        // graceful degradation
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#f97316" />
      </View>
    );
  }

  const userName = profile?.name || 'Traveller';
  const avatarLetter = userName.charAt(0).toUpperCase();
  const profilePct = computeProfileCompleteness(profile);

  const stats = [
    { label: 'Total Trips',      value: totalTrips },
    { label: 'Reviews Written',  value: reviewCount },
    { label: 'Miles Balance',    value: milesBalance },
    { label: 'Saved Stays',      value: savedCount },
  ];

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>‹ Back</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Dashboard</Text>
        <View style={{ width: 60 }} />
      </View>

      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        {/* Welcome Section */}
        <View style={styles.welcomeRow}>
          {profile?.avatarUrl ? (
            <View style={styles.avatar}>
              <Text style={styles.avatarText}>{avatarLetter}</Text>
            </View>
          ) : (
            <View style={styles.avatar}>
              <Text style={styles.avatarText}>{avatarLetter}</Text>
            </View>
          )}
          <View style={styles.welcomeTextWrap}>
            <Text style={styles.welcomeLabel}>Welcome back,</Text>
            <Text style={styles.welcomeName}>{userName}</Text>
          </View>
        </View>

        {/* Quick Stats */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.statsRow}
        >
          {stats.map((s) => (
            <View key={s.label} style={styles.statCard}>
              <Text style={styles.statValue}>{s.value.toLocaleString('en-IN')}</Text>
              <Text style={styles.statLabel}>{s.label}</Text>
            </View>
          ))}
        </ScrollView>

        {/* Upcoming Trips */}
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>Upcoming Trips</Text>
            <TouchableOpacity onPress={() => router.push('/trips')}>
              <Text style={styles.sectionLink}>View All Trips</Text>
            </TouchableOpacity>
          </View>

          {upcomingTrips.length === 0 ? (
            <View style={styles.emptyCard}>
              <Text style={styles.emptyText}>No upcoming trips</Text>
            </View>
          ) : (
            upcomingTrips.map((trip) => {
              const statusStyle = STATUS_STYLE[trip.status] ?? {
                label: trip.status,
                bg: '#f3f4f6',
                text: '#374151',
              };
              return (
                <TouchableOpacity
                  key={trip.id}
                  style={styles.tripCard}
                  activeOpacity={0.85}
                  onPress={() => router.push(`/listing/${trip.listingId}`)}
                >
                  <View style={styles.tripBody}>
                    <Text style={styles.tripTitle} numberOfLines={1}>
                      {trip.listingTitle ?? trip.title ?? 'Booking'}
                    </Text>
                    <Text style={styles.tripCity}>{trip.city ?? ''}</Text>
                    <Text style={styles.tripDates}>
                      {formatDate(trip.checkInDate)} — {formatDate(trip.checkOutDate)}
                    </Text>
                  </View>
                  <View style={[styles.badge, { backgroundColor: statusStyle.bg }]}>
                    <Text style={[styles.badgeText, { color: statusStyle.text }]}>
                      {statusStyle.label}
                    </Text>
                  </View>
                </TouchableOpacity>
              );
            })
          )}
        </View>

        {/* Recent Reviews */}
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>Recent Reviews</Text>
            <TouchableOpacity onPress={() => router.push('/my-reviews')}>
              <Text style={styles.sectionLink}>View All Reviews</Text>
            </TouchableOpacity>
          </View>

          {recentReviews.length === 0 ? (
            <View style={styles.emptyCard}>
              <Text style={styles.emptyText}>No reviews yet</Text>
            </View>
          ) : (
            recentReviews.map((review, idx) => (
              <View key={review.id ?? idx} style={styles.reviewCard}>
                <Text style={styles.reviewTitle} numberOfLines={1}>
                  {review.listingTitle ?? 'Listing'}
                </Text>
                <Text style={styles.reviewStars}>
                  {renderStars(review.overallRating ?? review.rating ?? 0)}
                </Text>
                <Text style={styles.reviewDate}>
                  {review.createdAt ? formatDate(review.createdAt) : ''}
                </Text>
              </View>
            ))
          )}
        </View>

        {/* Quick Actions */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Quick Actions</Text>
          <View style={styles.actionsGrid}>
            {QUICK_ACTIONS.map((action) => (
              <TouchableOpacity
                key={action.label}
                style={styles.actionCard}
                activeOpacity={0.85}
                onPress={() => router.push(action.route as any)}
              >
                <Text style={styles.actionIcon}>{action.icon}</Text>
                <Text style={styles.actionLabel}>{action.label}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* Profile Completeness */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Profile Completeness</Text>
          <View style={styles.profileCard}>
            <View style={styles.progressBarBg}>
              <View style={[styles.progressBarFill, { width: `${profilePct}%` }]} />
            </View>
            <Text style={styles.profilePct}>{profilePct}% complete</Text>
            {profilePct < 100 && (
              <TouchableOpacity
                style={styles.completeBtn}
                onPress={() => router.push('/profile-edit')}
              >
                <Text style={styles.completeBtnText}>Complete Profile</Text>
              </TouchableOpacity>
            )}
          </View>
        </View>

        <View style={{ height: 40 }} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container:      { flex: 1, backgroundColor: '#f9fafb' },
  center:         { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#f9fafb' },
  header:         { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#fff', paddingTop: 52, paddingBottom: 12, paddingHorizontal: 16, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  backBtn:        { width: 60 },
  backText:       { fontSize: 16, color: '#f97316', fontWeight: '600' },
  headerTitle:    { fontSize: 17, fontWeight: '700', color: '#111827' },
  scroll:         { padding: 16 },

  // Welcome
  welcomeRow:     { flexDirection: 'row', alignItems: 'center', marginBottom: 20 },
  avatar:         { width: 56, height: 56, borderRadius: 28, backgroundColor: '#f97316', alignItems: 'center', justifyContent: 'center' },
  avatarText:     { fontSize: 22, fontWeight: '700', color: '#fff' },
  welcomeTextWrap:{ marginLeft: 14 },
  welcomeLabel:   { fontSize: 14, color: '#6b7280' },
  welcomeName:    { fontSize: 20, fontWeight: '700', color: '#111827', marginTop: 2 },

  // Stats
  statsRow:       { paddingBottom: 4, marginBottom: 20, gap: 10 },
  statCard:       { backgroundColor: '#fff', borderRadius: 14, padding: 16, minWidth: 120, alignItems: 'center', borderWidth: 1, borderColor: '#f3f4f6' },
  statValue:      { fontSize: 22, fontWeight: '800', color: '#f97316' },
  statLabel:      { fontSize: 11, fontWeight: '500', color: '#6b7280', marginTop: 4, textAlign: 'center' },

  // Section
  section:        { marginBottom: 24 },
  sectionHeader:  { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 },
  sectionTitle:   { fontSize: 16, fontWeight: '700', color: '#111827' },
  sectionLink:    { fontSize: 13, fontWeight: '600', color: '#f97316' },

  // Empty
  emptyCard:      { backgroundColor: '#fff', borderRadius: 12, padding: 20, alignItems: 'center', borderWidth: 1, borderColor: '#f3f4f6' },
  emptyText:      { fontSize: 13, color: '#9ca3af' },

  // Trip Card
  tripCard:       { backgroundColor: '#fff', borderRadius: 14, padding: 14, marginBottom: 10, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderWidth: 1, borderColor: '#f3f4f6' },
  tripBody:       { flex: 1, marginRight: 10 },
  tripTitle:      { fontSize: 14, fontWeight: '600', color: '#111827' },
  tripCity:       { fontSize: 12, color: '#6b7280', marginTop: 2 },
  tripDates:      { fontSize: 12, color: '#9ca3af', marginTop: 4 },
  badge:          { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 8 },
  badgeText:      { fontSize: 11, fontWeight: '600' },

  // Review Card
  reviewCard:     { backgroundColor: '#fff', borderRadius: 14, padding: 14, marginBottom: 10, borderWidth: 1, borderColor: '#f3f4f6' },
  reviewTitle:    { fontSize: 14, fontWeight: '600', color: '#111827' },
  reviewStars:    { fontSize: 14, color: '#f97316', marginTop: 4 },
  reviewDate:     { fontSize: 11, color: '#9ca3af', marginTop: 4 },

  // Quick Actions
  actionsGrid:    { flexDirection: 'row', flexWrap: 'wrap', gap: 10, marginTop: 10 },
  actionCard:     { width: '48%' as any, backgroundColor: '#fff', borderRadius: 14, paddingVertical: 20, alignItems: 'center', borderWidth: 1, borderColor: '#f3f4f6' },
  actionIcon:     { fontSize: 28, marginBottom: 8 },
  actionLabel:    { fontSize: 13, fontWeight: '600', color: '#111827' },

  // Profile Completeness
  profileCard:    { backgroundColor: '#fff', borderRadius: 14, padding: 16, marginTop: 10, borderWidth: 1, borderColor: '#f3f4f6' },
  progressBarBg:  { height: 8, backgroundColor: '#f3f4f6', borderRadius: 4, overflow: 'hidden' },
  progressBarFill:{ height: 8, backgroundColor: '#f97316', borderRadius: 4 },
  profilePct:     { fontSize: 13, fontWeight: '600', color: '#374151', marginTop: 8 },
  completeBtn:    { backgroundColor: '#f97316', borderRadius: 10, paddingVertical: 10, alignItems: 'center', marginTop: 12 },
  completeBtnText:{ color: '#fff', fontWeight: '600', fontSize: 14 },
});
