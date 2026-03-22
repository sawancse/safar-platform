import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  RefreshControl,
  Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* -- Constants ------------------------------------------------------------ */

const CATEGORIES = [
  { key: 'ratingCleanliness', label: 'Cleanliness', color: '#16a34a' },
  { key: 'ratingLocation', label: 'Location', color: '#2563eb' },
  { key: 'ratingValue', label: 'Value', color: '#7c3aed' },
  { key: 'ratingCommunication', label: 'Comms', color: '#0891b2' },
  { key: 'ratingCheckIn', label: 'Check-in', color: '#ea580c' },
  { key: 'ratingAccuracy', label: 'Accuracy', color: '#db2777' },
] as const;

const COMMENT_COLLAPSE_LENGTH = 150;

function ratingColor(val: number): string {
  if (val >= 4.5) return '#16a34a';
  if (val >= 3.5) return '#eab308';
  return '#dc2626';
}

function renderStars(rating: number, size: number = 16) {
  const stars: string[] = [];
  for (let i = 1; i <= 5; i++) {
    stars.push(i <= Math.round(rating) ? '\u2605' : '\u2606');
  }
  return (
    <Text style={{ fontSize: size, color: ratingColor(rating), letterSpacing: 2 }}>
      {stars.join('')}
    </Text>
  );
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

/* -- Component ------------------------------------------------------------ */

export default function MyReviewsScreen() {
  const router = useRouter();

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [reviews, setReviews] = useState<any[]>([]);
  const [pendingBookings, setPendingBookings] = useState<any[]>([]);
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

  /* -- Data fetching ------------------------------------------------------ */

  const fetchData = useCallback(async (refresh = false) => {
    const token = await getAccessToken();
    if (!token) {
      router.push('/auth');
      return;
    }

    try {
      const [reviewsRes, bookingsRes] = await Promise.all([
        api.getMyReviews(token),
        api.getMyBookings(token),
      ]);

      const reviewList: any[] = reviewsRes?.content || reviewsRes || [];
      setReviews(reviewList);

      // Find completed bookings from last 30 days that have no review yet
      const bookingList: any[] = bookingsRes?.content || bookingsRes || [];
      const thirtyDaysAgo = new Date();
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

      const reviewedBookingIds = new Set(reviewList.map((r: any) => r.bookingId));
      const pending = bookingList.filter((b: any) => {
        if (b.status !== 'COMPLETED') return false;
        const completedDate = new Date(b.checkOutDate || b.updatedAt || b.createdAt);
        if (completedDate < thirtyDaysAgo) return false;
        return !reviewedBookingIds.has(b.id);
      });
      setPendingBookings(pending);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to load reviews');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, []);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    fetchData(true);
  }, [fetchData]);

  /* -- Computed stats ----------------------------------------------------- */

  const totalReviews = reviews.length;
  const averageRating =
    totalReviews > 0
      ? reviews.reduce((sum: number, r: any) => sum + (r.rating || 0), 0) / totalReviews
      : 0;

  /* -- Expand / collapse -------------------------------------------------- */

  function toggleExpand(id: string) {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  /* -- Render helpers ----------------------------------------------------- */

  function renderStatsSummary() {
    return (
      <View style={styles.statsContainer}>
        <View style={styles.statBox}>
          <Text style={styles.statNumber}>{totalReviews}</Text>
          <Text style={styles.statLabel}>Reviews Written</Text>
        </View>
        <View style={styles.statDivider} />
        <View style={styles.statBox}>
          <Text style={[styles.statNumber, { color: ratingColor(averageRating) }]}>
            {averageRating > 0 ? averageRating.toFixed(1) : '--'}
          </Text>
          {averageRating > 0 && renderStars(averageRating, 18)}
          <Text style={styles.statLabel}>Avg Rating Given</Text>
        </View>
      </View>
    );
  }

  function renderPendingSection() {
    if (pendingBookings.length === 0) return null;

    return (
      <View style={styles.pendingSection}>
        <Text style={styles.sectionTitle}>Pending Reviews</Text>
        <Text style={styles.pendingSubtext}>
          {pendingBookings.length} completed {pendingBookings.length === 1 ? 'stay' : 'stays'} awaiting your review
        </Text>
        {pendingBookings.map((booking: any) => (
          <View key={booking.id} style={styles.pendingCard}>
            <View style={{ flex: 1 }}>
              <Text style={styles.pendingTitle} numberOfLines={1}>
                {booking.listingTitle || booking.listing?.title || 'Your Stay'}
              </Text>
              <Text style={styles.pendingDates}>
                {booking.checkInDate && formatDate(booking.checkInDate)}
                {booking.checkOutDate && ` - ${formatDate(booking.checkOutDate)}`}
              </Text>
            </View>
            <TouchableOpacity
              style={styles.writeReviewBtn}
              activeOpacity={0.7}
              onPress={() => router.push(`/review?bookingId=${booking.id}`)}
            >
              <Text style={styles.writeReviewBtnText}>Write Review</Text>
            </TouchableOpacity>
          </View>
        ))}
      </View>
    );
  }

  function renderCategoryDots(review: any) {
    const dots = CATEGORIES.filter((cat) => review[cat.key]);
    if (dots.length === 0) return null;

    return (
      <View style={styles.catDotsRow}>
        {dots.map((cat) => {
          const val = review[cat.key];
          return (
            <View key={cat.key} style={styles.catDot}>
              <View style={[styles.catDotCircle, { backgroundColor: cat.color }]}>
                <Text style={styles.catDotText}>{val}</Text>
              </View>
              <Text style={styles.catDotLabel} numberOfLines={1}>
                {cat.label}
              </Text>
            </View>
          );
        })}
      </View>
    );
  }

  function renderReviewCard(review: any) {
    const listingTitle = review.listingTitle || review.listing?.title || 'Listing';
    const listingId = review.listingId || review.listing?.id;
    const date = review.createdAt || review.reviewDate;
    const rating = review.rating || 0;
    const comment = review.comment || '';
    const isLong = comment.length > COMMENT_COLLAPSE_LENGTH;
    const isExpanded = expandedIds.has(review.id);

    return (
      <View key={review.id} style={styles.reviewCard}>
        {/* Listing title */}
        <TouchableOpacity
          activeOpacity={0.7}
          onPress={() => listingId && router.push(`/listing/${listingId}`)}
          disabled={!listingId}
        >
          <Text style={styles.listingTitle} numberOfLines={2}>
            {listingTitle}
          </Text>
        </TouchableOpacity>

        {/* Date and rating row */}
        <View style={styles.dateRatingRow}>
          {date && <Text style={styles.reviewDate}>{formatDate(date)}</Text>}
          <View style={styles.ratingBadge}>{renderStars(rating, 16)}</View>
        </View>

        {/* Category dots */}
        {renderCategoryDots(review)}

        {/* Comment */}
        {!!comment && (
          <View style={styles.commentSection}>
            <Text style={styles.reviewComment}>
              {isLong && !isExpanded
                ? comment.slice(0, COMMENT_COLLAPSE_LENGTH) + '...'
                : comment}
            </Text>
            {isLong && (
              <TouchableOpacity activeOpacity={0.6} onPress={() => toggleExpand(review.id)}>
                <Text style={styles.expandText}>
                  {isExpanded ? 'Show less' : 'Read more'}
                </Text>
              </TouchableOpacity>
            )}
          </View>
        )}

        {/* Host reply if present */}
        {!!(review.hostReply || review.reply) && (
          <View style={styles.hostReplyBox}>
            <Text style={styles.hostReplyLabel}>Host reply</Text>
            <Text style={styles.hostReplyText}>{review.hostReply || review.reply}</Text>
          </View>
        )}
      </View>
    );
  }

  /* -- Main render -------------------------------------------------------- */

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#f97316" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} activeOpacity={0.7} style={styles.backBtn}>
          <Text style={styles.backText}>{'\u2039'}</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>My Reviews</Text>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#f97316" />
        }
      >
        {/* Stats summary */}
        {renderStatsSummary()}

        {/* Pending reviews */}
        {renderPendingSection()}

        {/* Reviews list */}
        <View style={styles.reviewsSection}>
          <Text style={styles.sectionTitle}>Your Reviews</Text>
          {reviews.length === 0 ? (
            <View style={styles.emptyState}>
              <Text style={styles.emptyIcon}>{'\u2606'}</Text>
              <Text style={styles.emptyText}>You haven't written any reviews yet</Text>
              <Text style={styles.emptySubtext}>
                After completing a stay, you can share your experience here
              </Text>
            </View>
          ) : (
            reviews.map((review) => renderReviewCard(review))
          )}
        </View>
      </ScrollView>
    </View>
  );
}

/* -- Styles --------------------------------------------------------------- */

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f9fafb' },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#f9fafb' },

  /* Header */
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 54,
    paddingBottom: 14,
    paddingHorizontal: 16,
    backgroundColor: '#f97316',
  },
  backBtn: { width: 40, height: 40, justifyContent: 'center', alignItems: 'center' },
  backText: { fontSize: 28, color: '#fff', fontWeight: '600', marginTop: -2 },
  headerTitle: { fontSize: 18, fontWeight: '700', color: '#fff' },

  /* Scroll */
  scrollView: { flex: 1 },
  scrollContent: { paddingBottom: 40 },

  /* Stats */
  statsContainer: {
    flexDirection: 'row',
    margin: 16,
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 20,
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowOffset: { width: 0, height: 2 },
    shadowRadius: 8,
    elevation: 2,
  },
  statBox: { flex: 1, alignItems: 'center' },
  statDivider: { width: 1, backgroundColor: '#e5e7eb', marginHorizontal: 16 },
  statNumber: { fontSize: 36, fontWeight: '800', color: '#111827', lineHeight: 42 },
  statLabel: { fontSize: 12, color: '#6b7280', marginTop: 4 },

  /* Pending section */
  pendingSection: {
    marginHorizontal: 16,
    marginBottom: 16,
  },
  pendingSubtext: { fontSize: 13, color: '#6b7280', marginBottom: 10 },
  pendingCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff7ed',
    borderRadius: 12,
    padding: 14,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#fed7aa',
  },
  pendingTitle: { fontSize: 14, fontWeight: '600', color: '#111827' },
  pendingDates: { fontSize: 12, color: '#9ca3af', marginTop: 2 },
  writeReviewBtn: {
    backgroundColor: '#f97316',
    borderRadius: 8,
    paddingVertical: 8,
    paddingHorizontal: 14,
    marginLeft: 10,
  },
  writeReviewBtnText: { fontSize: 13, fontWeight: '700', color: '#fff' },

  /* Section title */
  sectionTitle: { fontSize: 17, fontWeight: '700', color: '#111827', marginBottom: 12 },

  /* Reviews section */
  reviewsSection: { paddingHorizontal: 16 },

  /* Review card */
  reviewCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOpacity: 0.04,
    shadowOffset: { width: 0, height: 1 },
    shadowRadius: 4,
    elevation: 1,
  },
  listingTitle: { fontSize: 15, fontWeight: '600', color: '#f97316', marginBottom: 6 },
  dateRatingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  reviewDate: { fontSize: 12, color: '#9ca3af' },
  ratingBadge: {},

  /* Category dots */
  catDotsRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 10 },
  catDot: { alignItems: 'center', width: 52 },
  catDotCircle: {
    width: 26,
    height: 26,
    borderRadius: 13,
    justifyContent: 'center',
    alignItems: 'center',
  },
  catDotText: { fontSize: 11, fontWeight: '700', color: '#fff' },
  catDotLabel: { fontSize: 9, color: '#6b7280', marginTop: 2, textAlign: 'center' },

  /* Comment */
  commentSection: { marginBottom: 10 },
  reviewComment: { fontSize: 14, color: '#374151', lineHeight: 20 },
  expandText: { fontSize: 13, fontWeight: '600', color: '#f97316', marginTop: 4 },

  /* Host reply */
  hostReplyBox: {
    backgroundColor: '#f9fafb',
    borderRadius: 8,
    padding: 10,
    borderLeftWidth: 3,
    borderLeftColor: '#e5e7eb',
  },
  hostReplyLabel: { fontSize: 11, fontWeight: '600', color: '#6b7280', marginBottom: 4 },
  hostReplyText: { fontSize: 13, color: '#374151', lineHeight: 18 },

  /* Empty state */
  emptyState: { alignItems: 'center', paddingVertical: 48 },
  emptyIcon: { fontSize: 48, color: '#d1d5db', marginBottom: 12 },
  emptyText: { fontSize: 16, fontWeight: '600', color: '#6b7280' },
  emptySubtext: { fontSize: 13, color: '#9ca3af', marginTop: 4, textAlign: 'center', paddingHorizontal: 32 },
});
