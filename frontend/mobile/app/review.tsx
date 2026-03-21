import { useState } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity,
  StyleSheet, TextInput, Alert, ActivityIndicator,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const CATEGORIES = [
  { key: 'ratingCleanliness', label: 'Cleanliness' },
  { key: 'ratingLocation', label: 'Location' },
  { key: 'ratingValue', label: 'Value' },
  { key: 'ratingCommunication', label: 'Communication' },
  { key: 'ratingCheckIn', label: 'Check-in' },
  { key: 'ratingAccuracy', label: 'Accuracy' },
] as const;

function StarRow({ label, value, onChange }: { label: string; value: number; onChange: (v: number) => void }) {
  return (
    <View style={styles.categoryRow}>
      <Text style={styles.categoryLabel}>{label}</Text>
      <View style={styles.starsRow}>
        {[1, 2, 3, 4, 5].map((s) => (
          <TouchableOpacity key={s} onPress={() => onChange(s)} activeOpacity={0.6}>
            <Text style={[styles.star, s <= value && styles.starActive]}>{s <= value ? '★' : '☆'}</Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );
}

export default function ReviewScreen() {
  const { bookingId } = useLocalSearchParams<{ bookingId: string }>();
  const router = useRouter();
  const [overall, setOverall] = useState(0);
  const [comment, setComment] = useState('');
  const [categories, setCategories] = useState<Record<string, number>>({});
  const [submitting, setSubmitting] = useState(false);

  function setCat(key: string, val: number) {
    setCategories((prev) => ({ ...prev, [key]: val }));
  }

  async function handleSubmit() {
    if (!bookingId) return;
    if (overall === 0) {
      Alert.alert('Rating required', 'Please select an overall rating');
      return;
    }
    const token = await getAccessToken();
    if (!token) {
      router.push('/auth');
      return;
    }
    setSubmitting(true);
    try {
      await api.createReview({
        bookingId,
        rating: overall,
        comment: comment.trim() || undefined,
        ratingCleanliness: categories.ratingCleanliness || undefined,
        ratingLocation: categories.ratingLocation || undefined,
        ratingValue: categories.ratingValue || undefined,
        ratingCommunication: categories.ratingCommunication || undefined,
        ratingCheckIn: categories.ratingCheckIn || undefined,
        ratingAccuracy: categories.ratingAccuracy || undefined,
      }, token);
      Alert.alert('Thank you!', 'Your review has been submitted.', [
        { text: 'OK', onPress: () => router.back() },
      ]);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to submit review');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={{ paddingBottom: 40 }}>
      <Text style={styles.heading}>Write a Review</Text>
      <Text style={styles.sub}>Share your experience to help other travelers</Text>

      {/* Overall rating */}
      <Text style={styles.sectionTitle}>Overall Rating</Text>
      <View style={styles.overallRow}>
        {[1, 2, 3, 4, 5].map((s) => (
          <TouchableOpacity key={s} onPress={() => setOverall(s)} activeOpacity={0.6}>
            <Text style={[styles.bigStar, s <= overall && styles.bigStarActive]}>
              {s <= overall ? '★' : '☆'}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Category ratings */}
      <Text style={styles.sectionTitle}>Rate by Category (optional)</Text>
      {CATEGORIES.map((cat) => (
        <StarRow
          key={cat.key}
          label={cat.label}
          value={categories[cat.key] || 0}
          onChange={(v) => setCat(cat.key, v)}
        />
      ))}

      {/* Comment */}
      <Text style={styles.sectionTitle}>Your Review</Text>
      <TextInput
        style={styles.input}
        placeholder="Tell others about your experience..."
        multiline
        numberOfLines={5}
        textAlignVertical="top"
        value={comment}
        onChangeText={setComment}
      />

      {/* Submit */}
      <TouchableOpacity
        style={[styles.submitBtn, (overall === 0 || submitting) && styles.submitBtnDisabled]}
        onPress={handleSubmit}
        disabled={overall === 0 || submitting}
        activeOpacity={0.8}
      >
        {submitting ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.submitBtnText}>Submit Review</Text>
        )}
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container:    { flex: 1, backgroundColor: '#fff', padding: 20 },
  heading:      { fontSize: 22, fontWeight: '700', color: '#111827', marginTop: 10 },
  sub:          { fontSize: 13, color: '#6b7280', marginTop: 4, marginBottom: 20 },
  sectionTitle: { fontSize: 15, fontWeight: '600', color: '#111827', marginTop: 20, marginBottom: 10 },
  overallRow:   { flexDirection: 'row', gap: 8, justifyContent: 'center', marginBottom: 10 },
  bigStar:      { fontSize: 40, color: '#d1d5db' },
  bigStarActive:{ color: '#facc15' },
  categoryRow:  { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 8, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  categoryLabel:{ fontSize: 14, color: '#374151' },
  starsRow:     { flexDirection: 'row', gap: 4 },
  star:         { fontSize: 22, color: '#d1d5db' },
  starActive:   { color: '#facc15' },
  input:        { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, padding: 14, fontSize: 14, color: '#111827', minHeight: 120, backgroundColor: '#fafafa' },
  submitBtn:    { backgroundColor: '#f97316', borderRadius: 14, paddingVertical: 16, alignItems: 'center', marginTop: 24 },
  submitBtnDisabled: { opacity: 0.5 },
  submitBtnText:{ color: '#fff', fontSize: 16, fontWeight: '700' },
});
