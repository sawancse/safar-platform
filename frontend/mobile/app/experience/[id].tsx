import { useEffect, useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';

const CATEGORY_ICONS: Record<string, string> = {
  CULINARY: '🍳', CULTURAL: '🏛️', WELLNESS: '🧘', ADVENTURE: '🏔️', CREATIVE: '🎨',
};

const CANCELLATION_LABELS: Record<string, string> = {
  FLEXIBLE: 'Free cancellation up to 24h before',
  MODERATE: 'Free cancellation up to 5 days before',
  STRICT: 'Non-refundable',
};

interface Session {
  id: string;
  sessionDate: string;
  startTime: string;
  endTime: string;
  availableSpots: number;
  bookedSpots: number;
  status: string;
}

interface Experience {
  id: string;
  hostId: string;
  title: string;
  description: string;
  category: string;
  city: string;
  locationName?: string;
  pricePaise: number;
  durationMinutes: number;
  maxGuests: number;
  languagesSpoken: string;
  whatsIncluded?: string;
  whatsNotIncluded?: string;
  itinerary?: string;
  meetingPoint?: string;
  accessibility?: string;
  cancellationPolicy?: string;
  minAge?: number;
  groupDiscountPct?: number;
  avgRating?: number;
  reviewCount?: number;
  hostName: string;
  upcomingSessions?: Session[];
}

export default function ExperienceDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const [exp, setExp] = useState<Experience | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedSession, setSelectedSession] = useState<Session | null>(null);
  const [guests, setGuests] = useState(1);
  const [booking, setBooking] = useState(false);

  useEffect(() => {
    if (!id) return;
    api.getExperience(id).then((data: any) => {
      setExp(data);
      if (data?.upcomingSessions?.length) {
        setSelectedSession(data.upcomingSessions[0]);
      }
      setLoading(false);
    }).catch(() => setLoading(false));
  }, [id]);

  async function handleBook() {
    if (!selectedSession) return;
    setBooking(true);
    try {
      await api.bookExperience({ sessionId: selectedSession.id, numGuests: guests });
      Alert.alert('Success', 'Booking confirmed!', [
        { text: 'OK', onPress: () => router.back() },
      ]);
    } catch (err: any) {
      Alert.alert('Error', err?.message || 'Booking failed');
    } finally {
      setBooking(false);
    }
  }

  function formatDuration(mins: number) {
    if (mins < 60) return `${mins} min`;
    const hrs = Math.floor(mins / 60);
    const rem = mins % 60;
    return rem > 0 ? `${hrs}h ${rem}m` : `${hrs} hours`;
  }

  if (loading) {
    return <View style={styles.center}><ActivityIndicator size="large" color="#f97316" /></View>;
  }
  if (!exp) {
    return (
      <View style={styles.center}>
        <Text style={{ fontSize: 48 }}>🔍</Text>
        <Text style={styles.emptyTitle}>Experience not found</Text>
      </View>
    );
  }

  const totalPrice = exp.pricePaise * guests;
  const discount = exp.groupDiscountPct && guests >= 3
    ? totalPrice * exp.groupDiscountPct / 100
    : 0;

  return (
    <View style={styles.container}>
      <ScrollView style={styles.scroll} contentContainerStyle={{ paddingBottom: 120 }}>
        {/* Hero */}
        <View style={styles.hero}>
          <Text style={styles.heroIcon}>{CATEGORY_ICONS[exp.category] ?? '🎯'}</Text>
        </View>

        <View style={styles.content}>
          {/* Category + Title */}
          <View style={styles.categoryRow}>
            <View style={styles.categoryBadge}>
              <Text style={styles.categoryText}>{exp.category}</Text>
            </View>
            {exp.avgRating != null && exp.avgRating > 0 && (
              <Text style={styles.rating}>★ {exp.avgRating.toFixed(1)} ({exp.reviewCount})</Text>
            )}
          </View>
          <Text style={styles.title}>{exp.title}</Text>
          <Text style={styles.meta}>
            {exp.city} • {formatDuration(exp.durationMinutes)} • Up to {exp.maxGuests} guests
          </Text>

          {/* Host */}
          <View style={styles.hostCard}>
            <View style={styles.hostAvatar}>
              <Text style={styles.hostInitial}>{exp.hostName?.charAt(0)}</Text>
            </View>
            <View>
              <Text style={styles.hostName}>Hosted by {exp.hostName}</Text>
              {exp.languagesSpoken && (
                <Text style={styles.hostLanguages}>Speaks {exp.languagesSpoken}</Text>
              )}
            </View>
          </View>

          {/* Description */}
          <Text style={styles.sectionTitle}>About this experience</Text>
          <Text style={styles.body}>{exp.description}</Text>

          {/* Itinerary */}
          {exp.itinerary && (
            <>
              <Text style={styles.sectionTitle}>What you'll do</Text>
              <Text style={styles.body}>{exp.itinerary}</Text>
            </>
          )}

          {/* Included */}
          {exp.whatsIncluded && (
            <>
              <Text style={[styles.sectionTitle, { color: '#16a34a' }]}>What's included</Text>
              {exp.whatsIncluded.split('\n').filter(Boolean).map((item, i) => (
                <Text key={i} style={styles.listItem}>✓ {item}</Text>
              ))}
            </>
          )}

          {/* Not Included */}
          {exp.whatsNotIncluded && (
            <>
              <Text style={styles.sectionTitle}>Not included</Text>
              {exp.whatsNotIncluded.split('\n').filter(Boolean).map((item, i) => (
                <Text key={i} style={styles.listItem}>✗ {item}</Text>
              ))}
            </>
          )}

          {/* Meeting Point */}
          {exp.meetingPoint && (
            <>
              <Text style={styles.sectionTitle}>Meeting point</Text>
              <Text style={styles.body}>{exp.meetingPoint}</Text>
            </>
          )}

          {/* Policies */}
          <View style={styles.policyRow}>
            <View style={styles.policyItem}>
              <Text style={styles.policyLabel}>Cancellation</Text>
              <Text style={styles.policyValue}>
                {CANCELLATION_LABELS[exp.cancellationPolicy ?? 'FLEXIBLE']}
              </Text>
            </View>
            {exp.minAge != null && (
              <View style={styles.policyItem}>
                <Text style={styles.policyLabel}>Min age</Text>
                <Text style={styles.policyValue}>{exp.minAge}+</Text>
              </View>
            )}
          </View>

          {/* Sessions */}
          {exp.upcomingSessions && exp.upcomingSessions.length > 0 && (
            <>
              <Text style={styles.sectionTitle}>Select a date</Text>
              {exp.upcomingSessions.map((session) => {
                const spotsLeft = session.availableSpots - session.bookedSpots;
                const isSelected = selectedSession?.id === session.id;
                return (
                  <TouchableOpacity
                    key={session.id}
                    onPress={() => setSelectedSession(session)}
                    style={[styles.sessionCard, isSelected && styles.sessionCardActive]}
                  >
                    <Text style={styles.sessionDate}>
                      {new Date(session.sessionDate).toLocaleDateString('en-IN', {
                        weekday: 'short', month: 'short', day: 'numeric'
                      })}
                    </Text>
                    <Text style={styles.sessionTime}>
                      {session.startTime?.slice(0, 5)} - {session.endTime?.slice(0, 5)}
                      {spotsLeft > 0 ? ` • ${spotsLeft} spots left` : ' • Full'}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </>
          )}

          {/* Guests */}
          <Text style={styles.sectionTitle}>Guests</Text>
          <View style={styles.guestRow}>
            <TouchableOpacity
              onPress={() => setGuests(Math.max(1, guests - 1))}
              style={styles.guestBtn}
            >
              <Text style={styles.guestBtnText}>-</Text>
            </TouchableOpacity>
            <Text style={styles.guestCount}>{guests}</Text>
            <TouchableOpacity
              onPress={() => setGuests(Math.min(exp.maxGuests, guests + 1))}
              style={styles.guestBtn}
            >
              <Text style={styles.guestBtnText}>+</Text>
            </TouchableOpacity>
          </View>
        </View>
      </ScrollView>

      {/* Bottom bar */}
      <View style={styles.bottomBar}>
        <View>
          <Text style={styles.totalPrice}>{formatPaise(totalPrice - discount)}</Text>
          {discount > 0 && (
            <Text style={styles.discountText}>-{exp.groupDiscountPct}% group discount</Text>
          )}
        </View>
        <TouchableOpacity
          onPress={handleBook}
          disabled={!selectedSession || booking}
          style={[styles.bookBtn, (!selectedSession || booking) && styles.bookBtnDisabled]}
        >
          <Text style={styles.bookBtnText}>
            {booking ? 'Booking...' : 'Reserve'}
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  scroll: { flex: 1 },

  hero: { height: 200, backgroundColor: '#fff7ed', alignItems: 'center', justifyContent: 'center' },
  heroIcon: { fontSize: 80 },

  content: { padding: 16 },

  categoryRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  categoryBadge: { backgroundColor: '#fff7ed', paddingHorizontal: 10, paddingVertical: 3, borderRadius: 100 },
  categoryText: { fontSize: 11, fontWeight: '700', color: '#c2410c' },
  rating: { fontSize: 14, fontWeight: '600', color: '#374151' },

  title: { fontSize: 24, fontWeight: '800', color: '#111827', marginTop: 8 },
  meta: { fontSize: 13, color: '#6b7280', marginTop: 4 },

  hostCard: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 12, backgroundColor: '#f9fafb', borderRadius: 12, marginTop: 16 },
  hostAvatar: { width: 44, height: 44, borderRadius: 22, backgroundColor: '#fed7aa', alignItems: 'center', justifyContent: 'center' },
  hostInitial: { fontSize: 18, fontWeight: '700', color: '#c2410c' },
  hostName: { fontSize: 14, fontWeight: '600', color: '#111827' },
  hostLanguages: { fontSize: 12, color: '#6b7280' },

  sectionTitle: { fontSize: 18, fontWeight: '700', color: '#111827', marginTop: 24, marginBottom: 8 },
  body: { fontSize: 14, color: '#4b5563', lineHeight: 22 },
  listItem: { fontSize: 13, color: '#4b5563', marginBottom: 4 },

  policyRow: { flexDirection: 'row', gap: 16, marginTop: 20, padding: 12, backgroundColor: '#f9fafb', borderRadius: 12 },
  policyItem: { flex: 1 },
  policyLabel: { fontSize: 12, fontWeight: '600', color: '#374151' },
  policyValue: { fontSize: 11, color: '#6b7280', marginTop: 2 },

  sessionCard: { padding: 12, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, marginBottom: 8 },
  sessionCardActive: { borderColor: '#f97316', backgroundColor: '#fff7ed' },
  sessionDate: { fontSize: 14, fontWeight: '600', color: '#111827' },
  sessionTime: { fontSize: 12, color: '#6b7280', marginTop: 2 },

  guestRow: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  guestBtn: { width: 36, height: 36, borderRadius: 18, borderWidth: 1, borderColor: '#d1d5db', alignItems: 'center', justifyContent: 'center' },
  guestBtnText: { fontSize: 18, color: '#374151' },
  guestCount: { fontSize: 20, fontWeight: '700', color: '#111827' },

  bottomBar: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 16, borderTopWidth: 1, borderTopColor: '#f3f4f6', backgroundColor: '#fff' },
  totalPrice: { fontSize: 20, fontWeight: '800', color: '#111827' },
  discountText: { fontSize: 11, color: '#16a34a' },
  bookBtn: { paddingHorizontal: 28, paddingVertical: 14, backgroundColor: '#f97316', borderRadius: 12 },
  bookBtnDisabled: { opacity: 0.5 },
  bookBtnText: { fontSize: 15, fontWeight: '700', color: '#fff' },

  emptyTitle: { fontSize: 18, fontWeight: '600', color: '#374151', marginTop: 12 },
});
