import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  TextInput,
  Alert,
  ActivityIndicator,
  RefreshControl,
  Modal,
  FlatList,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ── Constants ─────────────────────────────────────────────── */

const CATEGORIES = [
  { key: 'ratingStaff', label: 'Staff' },
  { key: 'ratingFacilities', label: 'Facilities' },
  { key: 'ratingCleanliness', label: 'Cleanliness' },
  { key: 'ratingComfort', label: 'Comfort' },
  { key: 'ratingValue', label: 'Value' },
  { key: 'ratingLocation', label: 'Location' },
  { key: 'ratingWifi', label: 'WiFi' },
  { key: 'ratingCommunication', label: 'Communication' },
  { key: 'ratingCheckIn', label: 'Check-in' },
  { key: 'ratingAccuracy', label: 'Accuracy' },
] as const;

function ratingColor(val: number): string {
  if (val >= 4.5) return '#16a34a';
  if (val >= 3.5) return '#eab308';
  return '#dc2626';
}

function renderStars(rating: number, size: number = 16) {
  const stars: string[] = [];
  for (let i = 1; i <= 5; i++) {
    stars.push(i <= Math.round(rating) ? '★' : '☆');
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

/* ── Component ─────────────────────────────────────────────── */

export default function HostReviewsScreen() {
  const router = useRouter();

  // State
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [stats, setStats] = useState<any>(null);
  const [reviews, setReviews] = useState<any[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);

  // Listings filter
  const [listings, setListings] = useState<any[]>([]);
  const [selectedListingId, setSelectedListingId] = useState<string | null>(null);
  const [showListingPicker, setShowListingPicker] = useState(false);

  // Reply modal
  const [replyModalVisible, setReplyModalVisible] = useState(false);
  const [replyReviewId, setReplyReviewId] = useState<string | null>(null);
  const [replyText, setReplyText] = useState('');
  const [submittingReply, setSubmittingReply] = useState(false);

  /* ── Data fetching ───────────────────────────────────────── */

  const fetchData = useCallback(async (refresh = false) => {
    const token = await getAccessToken();
    if (!token) {
      router.push('/auth');
      return;
    }

    if (refresh) {
      setPage(0);
      setHasMore(true);
    }

    try {
      const [statsRes, listingsRes, reviewsRes] = await Promise.all([
        api.getHostReviewStats(token),
        api.getMyListings(token),
        api.getHostReviews(token, { listingId: selectedListingId, page: 0 }),
      ]);
      setStats(statsRes);
      setListings(listingsRes?.content || listingsRes || []);
      const reviewList = reviewsRes?.content || reviewsRes || [];
      setReviews(reviewList);
      setHasMore(reviewList.length >= 10);
      setPage(0);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to load reviews');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [selectedListingId]);

  const loadMore = useCallback(async () => {
    if (!hasMore || loadingMore) return;
    const token = await getAccessToken();
    if (!token) return;

    setLoadingMore(true);
    try {
      const nextPage = page + 1;
      const res = await api.getHostReviews(token, { listingId: selectedListingId, page: nextPage });
      const newReviews = res?.content || res || [];
      setReviews((prev) => [...prev, ...newReviews]);
      setPage(nextPage);
      setHasMore(newReviews.length >= 10);
    } catch {
      // silent
    } finally {
      setLoadingMore(false);
    }
  }, [page, hasMore, loadingMore, selectedListingId]);

  useEffect(() => {
    setLoading(true);
    fetchData();
  }, [selectedListingId]);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    fetchData(true);
  }, [fetchData]);

  /* ── Reply actions ───────────────────────────────────────── */

  async function handleSubmitReply() {
    if (!replyReviewId || !replyText.trim()) return;
    const token = await getAccessToken();
    if (!token) return;

    setSubmittingReply(true);
    try {
      await api.addHostReply(replyReviewId, replyText.trim(), token);
      setReplyModalVisible(false);
      setReplyText('');
      setReplyReviewId(null);
      fetchData(true);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to submit reply');
    } finally {
      setSubmittingReply(false);
    }
  }

  async function handleDeleteReply(reviewId: string) {
    Alert.alert('Delete Reply', 'Are you sure you want to delete your reply?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          const token = await getAccessToken();
          if (!token) return;
          try {
            await api.deleteHostReply(reviewId, token);
            fetchData(true);
          } catch (err: any) {
            Alert.alert('Error', err.message || 'Failed to delete reply');
          }
        },
      },
    ]);
  }

  /* ── Render helpers ──────────────────────────────────────── */

  function renderCategoryBar(label: string, value: number) {
    const pct = (value / 5) * 100;
    return (
      <View key={label} style={styles.categoryBarRow}>
        <Text style={styles.categoryBarLabel}>{label}</Text>
        <View style={styles.categoryBarTrack}>
          <View style={[styles.categoryBarFill, { width: `${pct}%`, backgroundColor: ratingColor(value) }]} />
        </View>
        <Text style={[styles.categoryBarValue, { color: ratingColor(value) }]}>{value.toFixed(1)}</Text>
      </View>
    );
  }

  function renderStatsSection() {
    if (!stats) return null;

    const avg = stats.averageRating || 0;
    const total = stats.totalReviews || 0;
    const catAverages = stats.categoryAverages || {};

    return (
      <View style={styles.statsContainer}>
        {/* Overall rating */}
        <View style={styles.overallRow}>
          <View style={styles.overallLeft}>
            <Text style={[styles.overallNumber, { color: ratingColor(avg) }]}>{avg.toFixed(1)}</Text>
            {renderStars(avg, 22)}
          </View>
          <View style={styles.overallRight}>
            <Text style={styles.totalReviews}>{total}</Text>
            <Text style={styles.totalLabel}>reviews</Text>
          </View>
        </View>

        {/* Category breakdown */}
        <View style={styles.categoryBreakdown}>
          {CATEGORIES.map((cat) => {
            const val = catAverages[cat.key] ?? 0;
            return renderCategoryBar(cat.label, val);
          })}
        </View>
      </View>
    );
  }

  function renderCategoryDots(review: any) {
    return (
      <View style={styles.catDotsRow}>
        {CATEGORIES.map((cat) => {
          const val = review[cat.key];
          if (!val) return null;
          return (
            <View key={cat.key} style={styles.catDot}>
              <View style={[styles.catDotCircle, { backgroundColor: ratingColor(val) }]}>
                <Text style={styles.catDotText}>{val}</Text>
              </View>
              <Text style={styles.catDotLabel} numberOfLines={1}>{cat.label}</Text>
            </View>
          );
        })}
      </View>
    );
  }

  function renderReviewCard(review: any) {
    const guestName = review.guestName || review.guestFirstName || 'Guest';
    const date = review.createdAt || review.reviewDate;
    const rating = review.rating || 0;
    const comment = review.comment || '';
    const hostReply = review.hostReply || review.reply;

    return (
      <View key={review.id} style={styles.reviewCard}>
        {/* Header */}
        <View style={styles.reviewHeader}>
          <View style={styles.reviewGuestInfo}>
            <View style={styles.avatarCircle}>
              <Text style={styles.avatarText}>{guestName.charAt(0).toUpperCase()}</Text>
            </View>
            <View style={{ flex: 1 }}>
              <Text style={styles.guestName}>{guestName}</Text>
              {date && <Text style={styles.reviewDate}>{formatDate(date)}</Text>}
            </View>
          </View>
          <View style={styles.ratingBadge}>
            {renderStars(rating, 14)}
          </View>
        </View>

        {/* Comment */}
        {!!comment && <Text style={styles.reviewComment}>{comment}</Text>}

        {/* Category dots */}
        {renderCategoryDots(review)}

        {/* Reply section */}
        <View style={styles.replySection}>
          {hostReply ? (
            <View style={styles.replyBox}>
              <View style={styles.replyHeader}>
                <Text style={styles.replyLabel}>Your reply</Text>
                <TouchableOpacity onPress={() => handleDeleteReply(review.id)} activeOpacity={0.6}>
                  <Text style={styles.deleteReplyText}>Delete</Text>
                </TouchableOpacity>
              </View>
              <Text style={styles.replyContent}>{hostReply}</Text>
            </View>
          ) : (
            <TouchableOpacity
              style={styles.replyButton}
              activeOpacity={0.7}
              onPress={() => {
                setReplyReviewId(review.id);
                setReplyText('');
                setReplyModalVisible(true);
              }}
            >
              <Text style={styles.replyButtonText}>Reply</Text>
            </TouchableOpacity>
          )}
        </View>
      </View>
    );
  }

  /* ── Listing filter ──────────────────────────────────────── */

  const selectedListingTitle = selectedListingId
    ? (listings.find((l: any) => l.id === selectedListingId)?.title || 'Unknown')
    : 'All Listings';

  /* ── Main render ─────────────────────────────────────────── */

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
          <Text style={styles.backText}>{'‹'}</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Reviews</Text>
        <View style={{ width: 40 }} />
      </View>

      {/* Listing filter */}
      <TouchableOpacity
        style={styles.filterBar}
        activeOpacity={0.7}
        onPress={() => setShowListingPicker(true)}
      >
        <Text style={styles.filterLabel}>Listing:</Text>
        <Text style={styles.filterValue} numberOfLines={1}>{selectedListingTitle}</Text>
        <Text style={styles.filterArrow}>{'▾'}</Text>
      </TouchableOpacity>

      <ScrollView
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#f97316" />}
        onScroll={({ nativeEvent }) => {
          const { layoutMeasurement, contentOffset, contentSize } = nativeEvent;
          if (layoutMeasurement.height + contentOffset.y >= contentSize.height - 100) {
            loadMore();
          }
        }}
        scrollEventThrottle={400}
      >
        {/* Stats */}
        {renderStatsSection()}

        {/* Reviews list */}
        <View style={styles.reviewsSection}>
          <Text style={styles.sectionTitle}>Guest Reviews</Text>
          {reviews.length === 0 ? (
            <View style={styles.emptyState}>
              <Text style={styles.emptyIcon}>☆</Text>
              <Text style={styles.emptyText}>No reviews yet</Text>
              <Text style={styles.emptySubtext}>Reviews from guests will appear here</Text>
            </View>
          ) : (
            reviews.map((review) => renderReviewCard(review))
          )}
          {loadingMore && (
            <ActivityIndicator size="small" color="#f97316" style={{ marginVertical: 16 }} />
          )}
        </View>
      </ScrollView>

      {/* Reply Modal */}
      <Modal
        visible={replyModalVisible}
        transparent
        animationType="slide"
        onRequestClose={() => setReplyModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Reply to Review</Text>
            <TextInput
              style={styles.replyInput}
              placeholder="Write your reply..."
              placeholderTextColor="#9ca3af"
              multiline
              numberOfLines={4}
              value={replyText}
              onChangeText={setReplyText}
              textAlignVertical="top"
            />
            <View style={styles.modalActions}>
              <TouchableOpacity
                style={styles.modalCancelBtn}
                activeOpacity={0.7}
                onPress={() => setReplyModalVisible(false)}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.modalSubmitBtn, !replyText.trim() && { opacity: 0.5 }]}
                activeOpacity={0.7}
                onPress={handleSubmitReply}
                disabled={!replyText.trim() || submittingReply}
              >
                {submittingReply ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.modalSubmitText}>Submit</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* Listing Picker Modal */}
      <Modal
        visible={showListingPicker}
        transparent
        animationType="fade"
        onRequestClose={() => setShowListingPicker(false)}
      >
        <TouchableOpacity
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setShowListingPicker(false)}
        >
          <View style={styles.pickerContent}>
            <Text style={styles.pickerTitle}>Filter by Listing</Text>
            <TouchableOpacity
              style={[styles.pickerItem, !selectedListingId && styles.pickerItemActive]}
              activeOpacity={0.7}
              onPress={() => {
                setSelectedListingId(null);
                setShowListingPicker(false);
              }}
            >
              <Text style={[styles.pickerItemText, !selectedListingId && styles.pickerItemTextActive]}>
                All Listings
              </Text>
            </TouchableOpacity>
            <FlatList
              data={listings}
              keyExtractor={(item: any) => item.id}
              renderItem={({ item }) => (
                <TouchableOpacity
                  style={[styles.pickerItem, selectedListingId === item.id && styles.pickerItemActive]}
                  activeOpacity={0.7}
                  onPress={() => {
                    setSelectedListingId(item.id);
                    setShowListingPicker(false);
                  }}
                >
                  <Text
                    style={[
                      styles.pickerItemText,
                      selectedListingId === item.id && styles.pickerItemTextActive,
                    ]}
                    numberOfLines={1}
                  >
                    {item.title}
                  </Text>
                </TouchableOpacity>
              )}
              style={{ maxHeight: 300 }}
            />
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
}

/* ── Styles ────────────────────────────────────────────────── */

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

  /* Filter bar */
  filterBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  filterLabel: { fontSize: 14, color: '#6b7280', marginRight: 6 },
  filterValue: { flex: 1, fontSize: 14, fontWeight: '600', color: '#111827' },
  filterArrow: { fontSize: 14, color: '#6b7280', marginLeft: 4 },

  /* Scroll */
  scrollView: { flex: 1 },
  scrollContent: { paddingBottom: 40 },

  /* Stats */
  statsContainer: {
    margin: 16,
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowOffset: { width: 0, height: 2 },
    shadowRadius: 8,
    elevation: 2,
  },
  overallRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 20,
    paddingBottom: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  overallLeft: { alignItems: 'center' },
  overallNumber: { fontSize: 48, fontWeight: '800', lineHeight: 54 },
  overallRight: { alignItems: 'center' },
  totalReviews: { fontSize: 32, fontWeight: '700', color: '#111827' },
  totalLabel: { fontSize: 13, color: '#6b7280', marginTop: 2 },

  /* Category bars */
  categoryBreakdown: { gap: 8 },
  categoryBarRow: { flexDirection: 'row', alignItems: 'center' },
  categoryBarLabel: { width: 100, fontSize: 13, color: '#374151' },
  categoryBarTrack: {
    flex: 1,
    height: 8,
    backgroundColor: '#f3f4f6',
    borderRadius: 4,
    overflow: 'hidden',
    marginHorizontal: 8,
  },
  categoryBarFill: { height: '100%', borderRadius: 4 },
  categoryBarValue: { width: 32, textAlign: 'right', fontSize: 13, fontWeight: '600' },

  /* Reviews section */
  reviewsSection: { paddingHorizontal: 16 },
  sectionTitle: { fontSize: 17, fontWeight: '700', color: '#111827', marginBottom: 12 },

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
  reviewHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  reviewGuestInfo: { flexDirection: 'row', alignItems: 'center', flex: 1 },
  avatarCircle: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: '#fed7aa',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 10,
  },
  avatarText: { fontSize: 16, fontWeight: '700', color: '#ea580c' },
  guestName: { fontSize: 15, fontWeight: '600', color: '#111827' },
  reviewDate: { fontSize: 12, color: '#9ca3af', marginTop: 1 },
  ratingBadge: { marginLeft: 8 },
  reviewComment: { fontSize: 14, color: '#374151', lineHeight: 20, marginBottom: 10 },

  /* Category dots */
  catDotsRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 12 },
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

  /* Reply */
  replySection: { borderTopWidth: 1, borderTopColor: '#f3f4f6', paddingTop: 10 },
  replyButton: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#f97316',
    alignSelf: 'flex-start',
  },
  replyButtonText: { fontSize: 13, fontWeight: '600', color: '#f97316' },
  replyBox: { backgroundColor: '#fff7ed', borderRadius: 8, padding: 12 },
  replyHeader: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 6 },
  replyLabel: { fontSize: 12, fontWeight: '600', color: '#ea580c' },
  deleteReplyText: { fontSize: 12, fontWeight: '600', color: '#dc2626' },
  replyContent: { fontSize: 13, color: '#374151', lineHeight: 18 },

  /* Empty */
  emptyState: { alignItems: 'center', paddingVertical: 48 },
  emptyIcon: { fontSize: 48, color: '#d1d5db', marginBottom: 12 },
  emptyText: { fontSize: 16, fontWeight: '600', color: '#6b7280' },
  emptySubtext: { fontSize: 13, color: '#9ca3af', marginTop: 4 },

  /* Reply modal */
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 20,
    paddingBottom: 36,
  },
  modalTitle: { fontSize: 17, fontWeight: '700', color: '#111827', marginBottom: 14 },
  replyInput: {
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderRadius: 10,
    padding: 12,
    fontSize: 14,
    color: '#111827',
    minHeight: 100,
    backgroundColor: '#f9fafb',
  },
  modalActions: { flexDirection: 'row', justifyContent: 'flex-end', marginTop: 14, gap: 10 },
  modalCancelBtn: { paddingVertical: 10, paddingHorizontal: 20, borderRadius: 8 },
  modalCancelText: { fontSize: 14, fontWeight: '600', color: '#6b7280' },
  modalSubmitBtn: {
    paddingVertical: 10,
    paddingHorizontal: 24,
    borderRadius: 8,
    backgroundColor: '#f97316',
  },
  modalSubmitText: { fontSize: 14, fontWeight: '700', color: '#fff' },

  /* Listing picker */
  pickerContent: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 20,
    paddingBottom: 36,
    maxHeight: '60%',
  },
  pickerTitle: { fontSize: 17, fontWeight: '700', color: '#111827', marginBottom: 14 },
  pickerItem: {
    paddingVertical: 12,
    paddingHorizontal: 14,
    borderRadius: 8,
    marginBottom: 4,
  },
  pickerItemActive: { backgroundColor: '#fff7ed' },
  pickerItemText: { fontSize: 14, color: '#374151' },
  pickerItemTextActive: { color: '#f97316', fontWeight: '600' },
});
