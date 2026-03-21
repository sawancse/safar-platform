import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
  Modal,
  TextInput,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';

const STATUS_STYLE: Record<string, { label: string; bg: string; text: string }> = {
  PENDING_PAYMENT: { label: 'Pending Payment', bg: '#fef9c3', text: '#854d0e' },
  CONFIRMED:       { label: 'Confirmed',        bg: '#dcfce7', text: '#14532d' },
  CANCELLED:       { label: 'Cancelled',        bg: '#fee2e2', text: '#7f1d1d' },
  CHECKED_IN:      { label: 'Checked In',        bg: '#dbeafe', text: '#1e40af' },
  COMPLETED:       { label: 'Completed',        bg: '#f3f4f6', text: '#374151' },
};

const CANCEL_REASONS = [
  'Change of plans',
  'Found a better deal',
  'Travel dates changed',
  'Personal reasons',
];

export default function TripsScreen() {
  const router = useRouter();
  const [bookings, setBookings] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [authed, setAuthed] = useState(false);

  // Review modal state
  const [reviewModal, setReviewModal] = useState<{ open: boolean; bookingId: string }>({ open: false, bookingId: '' });
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewedBookings, setReviewedBookings] = useState<Set<string>>(new Set());
  const [submittingReview, setSubmittingReview] = useState(false);

  const loadBookings = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) {
      setLoading(false);
      return;
    }
    setAuthed(true);
    try {
      const b = await api.getMyBookings(token);
      setBookings(b);
    } catch {
      setBookings([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadBookings();
  }, [loadBookings]);

  async function handleCancel(bookingId: string) {
    Alert.alert(
      'Cancel Booking',
      'Select a reason for cancellation:',
      [
        ...CANCEL_REASONS.map((reason) => ({
          text: reason,
          onPress: async () => {
            try {
              const token = await getAccessToken();
              if (!token) return;
              await api.cancelBooking(bookingId, reason, token);
              Alert.alert('Cancelled', 'Your booking has been cancelled.');
              setLoading(true);
              loadBookings();
            } catch (e: any) {
              Alert.alert('Error', e.message || 'Failed to cancel booking');
            }
          },
        })),
        { text: 'Back', style: 'cancel' },
      ],
    );
  }

  function openReviewModal(bookingId: string) {
    setReviewRating(5);
    setReviewComment('');
    setReviewModal({ open: true, bookingId });
  }

  async function handleSubmitReview() {
    if (!reviewModal.bookingId) return;
    setSubmittingReview(true);
    try {
      const token = await getAccessToken();
      if (!token) return;
      await api.createReview(
        { bookingId: reviewModal.bookingId, rating: reviewRating, comment: reviewComment || undefined },
        token,
      );
      setReviewedBookings((prev) => new Set(prev).add(reviewModal.bookingId));
      setReviewModal({ open: false, bookingId: '' });
      Alert.alert('Thank you!', 'Your review has been submitted.');
    } catch (e: any) {
      Alert.alert('Error', e.message || 'Failed to submit review');
    } finally {
      setSubmittingReview(false);
    }
  }

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#f97316" />
      </View>
    );
  }

  if (!authed) {
    return (
      <View style={styles.center}>
        <Text style={styles.emptyIcon}>🔒</Text>
        <Text style={styles.emptyTitle}>Sign in to view trips</Text>
        <TouchableOpacity style={styles.button} onPress={() => router.push('/auth')}>
          <Text style={styles.buttonText}>Sign in</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={{ flex: 1 }}>
      <FlatList
        data={bookings}
        keyExtractor={(item) => item.id}
        contentContainerStyle={bookings.length === 0 ? styles.center : styles.list}
        ListEmptyComponent={
          <View style={styles.centerInner}>
            <Text style={styles.emptyIcon}>🗺️</Text>
            <Text style={styles.emptyTitle}>No trips yet</Text>
            <TouchableOpacity style={styles.button} onPress={() => router.push('/')}>
              <Text style={styles.buttonText}>Explore stays</Text>
            </TouchableOpacity>
          </View>
        }
        renderItem={({ item }) => {
          const s = STATUS_STYLE[item.status] ?? STATUS_STYLE.COMPLETED;
          const canCancel = item.status === 'PENDING_PAYMENT' || item.status === 'CONFIRMED';
          const isReviewed = item.hasReview || reviewedBookings.has(item.id);
          const isReviewable = item.status === 'COMPLETED' || item.status === 'CONFIRMED' || item.status === 'CHECKED_IN';
          const checkOutDate = item.checkOut ? new Date(item.checkOut) : null;
          const daysSinceCheckout = checkOutDate ? Math.floor((Date.now() - checkOutDate.getTime()) / (1000 * 60 * 60 * 24)) : 0;
          const reviewWindowExpired = daysSinceCheckout > 30;
          const canReview = isReviewable && !isReviewed && !reviewWindowExpired;
          return (
            <View style={styles.card}>
              <View style={styles.cardRow}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.cardDate}>{item.checkInDate} → {item.checkOutDate}</Text>
                  <Text style={styles.cardGuests}>{item.guests} guest{item.guests > 1 ? 's' : ''}</Text>
                </View>
                <View>
                  <Text style={[styles.statusBadge, { backgroundColor: s.bg, color: s.text }]}>{s.label}</Text>
                  <Text style={styles.cardPrice}>{formatPaise(item.totalAmountPaise)}</Text>
                </View>
              </View>
              {isReviewed && (
                <View style={styles.actionRow}>
                  <View style={styles.reviewedBadge}>
                    <Text style={styles.reviewedBadgeText}>
                      {item.reviewRating ? '\u2605'.repeat(item.reviewRating) + '\u2606'.repeat(5 - item.reviewRating) + ' ' : ''}
                      Review submitted{item.reviewedAt ? ` on ${new Date(item.reviewedAt).toLocaleDateString()}` : ''}
                    </Text>
                  </View>
                </View>
              )}
              {isReviewable && !isReviewed && reviewWindowExpired && (
                <View style={styles.actionRow}>
                  <View style={styles.expiredBadge}>
                    <Text style={styles.expiredBadgeText}>Review window expired</Text>
                  </View>
                </View>
              )}
              {(canCancel || canReview) && (
                <View style={styles.actionRow}>
                  {canCancel && (
                    <TouchableOpacity
                      style={styles.cancelBtn}
                      onPress={() => handleCancel(item.id)}
                    >
                      <Text style={styles.cancelBtnText}>Cancel</Text>
                    </TouchableOpacity>
                  )}
                  {canReview && (
                    <TouchableOpacity
                      style={styles.reviewBtn}
                      onPress={() => openReviewModal(item.id)}
                    >
                      <Text style={styles.reviewBtnText}>Write Review</Text>
                    </TouchableOpacity>
                  )}
                </View>
              )}
            </View>
          );
        }}
      />

      {/* Review Modal */}
      <Modal
        visible={reviewModal.open}
        transparent
        animationType="slide"
        onRequestClose={() => setReviewModal({ open: false, bookingId: '' })}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Write a Review</Text>

            {/* Star rating */}
            <View style={styles.starRow}>
              {[1, 2, 3, 4, 5].map((star) => (
                <TouchableOpacity key={star} onPress={() => setReviewRating(star)}>
                  <Text style={styles.star}>{star <= reviewRating ? '\u2605' : '\u2606'}</Text>
                </TouchableOpacity>
              ))}
            </View>

            {/* Comment input */}
            <TextInput
              style={styles.commentInput}
              placeholder="Share your experience (optional)"
              placeholderTextColor="#9ca3af"
              multiline
              numberOfLines={4}
              textAlignVertical="top"
              value={reviewComment}
              onChangeText={setReviewComment}
            />

            {/* Actions */}
            <View style={styles.modalActions}>
              <TouchableOpacity
                style={styles.modalCancelBtn}
                onPress={() => setReviewModal({ open: false, bookingId: '' })}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.modalSubmitBtn, submittingReview && { opacity: 0.5 }]}
                onPress={handleSubmitReview}
                disabled={submittingReview}
              >
                {submittingReview ? (
                  <ActivityIndicator color="#fff" size="small" />
                ) : (
                  <Text style={styles.modalSubmitText}>Submit</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  center:       { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32 },
  centerInner:  { alignItems: 'center', paddingTop: 60 },
  list:         { padding: 16 },
  emptyIcon:    { fontSize: 48, marginBottom: 12 },
  emptyTitle:   { fontSize: 18, fontWeight: '600', color: '#374151', marginBottom: 16 },
  button:       { backgroundColor: '#f97316', borderRadius: 12, paddingHorizontal: 24, paddingVertical: 12 },
  buttonText:   { color: '#fff', fontWeight: '600', fontSize: 14 },
  card:         { backgroundColor: '#fff', borderRadius: 16, padding: 14, marginBottom: 12, borderWidth: 1, borderColor: '#f3f4f6' },
  cardRow:      { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  cardDate:     { fontSize: 14, fontWeight: '600', color: '#111827' },
  cardGuests:   { fontSize: 12, color: '#6b7280', marginTop: 2 },
  statusBadge:  { fontSize: 11, fontWeight: '600', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 100, overflow: 'hidden', textAlign: 'right' },
  cardPrice:    { fontSize: 13, fontWeight: '700', color: '#111827', marginTop: 4, textAlign: 'right' },

  // Action buttons row
  actionRow:      { flexDirection: 'row', marginTop: 10, gap: 8 },
  cancelBtn:      { borderWidth: 1, borderColor: '#fca5a5', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 7, backgroundColor: '#fef2f2' },
  cancelBtnText:  { fontSize: 12, fontWeight: '600', color: '#dc2626' },
  reviewBtn:      { borderWidth: 1, borderColor: '#93c5fd', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 7, backgroundColor: '#eff6ff' },
  reviewBtnText:  { fontSize: 12, fontWeight: '600', color: '#1d4ed8' },
  reviewedBadge:     { backgroundColor: '#f0fdf4', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 7, borderWidth: 1, borderColor: '#bbf7d0' },
  reviewedBadgeText: { fontSize: 12, fontWeight: '600', color: '#16a34a' },
  expiredBadge:      { backgroundColor: '#f9fafb', borderRadius: 8, paddingHorizontal: 14, paddingVertical: 7 },
  expiredBadgeText:  { fontSize: 12, fontWeight: '500', color: '#9ca3af' },

  // Review modal
  modalOverlay:    { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
  modalContent:    { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, paddingBottom: 32 },
  modalTitle:      { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 16 },
  starRow:         { flexDirection: 'row', marginBottom: 16 },
  star:            { fontSize: 32, color: '#f59e0b', marginRight: 8 },
  commentInput:    { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, padding: 12, fontSize: 14, color: '#111827', minHeight: 100, backgroundColor: '#f9fafb', marginBottom: 16 },
  modalActions:    { flexDirection: 'row', justifyContent: 'flex-end', gap: 10 },
  modalCancelBtn:  { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 20, paddingVertical: 10 },
  modalCancelText: { fontSize: 14, fontWeight: '600', color: '#6b7280' },
  modalSubmitBtn:  { backgroundColor: '#f97316', borderRadius: 10, paddingHorizontal: 20, paddingVertical: 10 },
  modalSubmitText: { fontSize: 14, fontWeight: '700', color: '#fff' },
});
