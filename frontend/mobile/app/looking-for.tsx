import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, TextInput, Modal,
  StyleSheet, ActivityIndicator, Alert, ScrollView, Image,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ---------- Types ---------- */

interface LookingForRequest {
  id: string;
  city: string;
  propertyType: string;
  checkIn: string;
  checkOut: string;
  budgetMinPaise?: number;
  budgetMaxPaise?: number;
  guests: number;
  specialRequirements?: string;
  status: 'ACTIVE' | 'MATCHED' | 'EXPIRED';
  matchesCount?: number;
  createdAt?: string;
}

interface MatchedListing {
  id: string;
  listingId: string;
  title: string;
  city: string;
  pricePaise: number;
  rating?: number;
  photoUrl?: string;
  matchScore: number;
}

const PROPERTY_TYPES = ['HOME', 'ROOM', 'HOTEL', 'PG', 'VILLA', 'APARTMENT', 'HOSTEL', 'FARMHOUSE'];

const STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  ACTIVE: { bg: '#dcfce7', text: '#16a34a' },
  MATCHED: { bg: '#dbeafe', text: '#2563eb' },
  EXPIRED: { bg: '#f3f4f6', text: '#6b7280' },
};

function formatPaise(paise: number): string {
  return '\u20B9' + (paise / 100).toLocaleString('en-IN');
}

/* ---------- Component ---------- */

export default function LookingForScreen() {
  const router = useRouter();
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // My requests
  const [requests, setRequests] = useState<LookingForRequest[]>([]);

  // Create form
  const [showForm, setShowForm] = useState(false);
  const [city, setCity] = useState('');
  const [propertyType, setPropertyType] = useState('HOME');
  const [checkIn, setCheckIn] = useState('');
  const [checkOut, setCheckOut] = useState('');
  const [budgetMin, setBudgetMin] = useState('');
  const [budgetMax, setBudgetMax] = useState('');
  const [guests, setGuests] = useState('2');
  const [specialReqs, setSpecialReqs] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Type picker
  const [showTypePicker, setShowTypePicker] = useState(false);

  // Matches modal
  const [matchesModalVisible, setMatchesModalVisible] = useState(false);
  const [matches, setMatches] = useState<MatchedListing[]>([]);
  const [loadingMatches, setLoadingMatches] = useState(false);
  const [matchesForCity, setMatchesForCity] = useState('');

  /* ---------- Load data ---------- */

  const loadRequests = useCallback(async (t: string) => {
    try {
      const data = await api.getMyLookingFor(t);
      setRequests(data ?? []);
    } catch {
      setRequests([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    (async () => {
      const t = await getAccessToken();
      if (!t) {
        router.replace('/auth');
        return;
      }
      setToken(t);
      loadRequests(t);
    })();
  }, []);

  /* ---------- Create request ---------- */

  function resetForm() {
    setCity('');
    setPropertyType('HOME');
    setCheckIn('');
    setCheckOut('');
    setBudgetMin('');
    setBudgetMax('');
    setGuests('2');
    setSpecialReqs('');
  }

  async function handlePost() {
    if (!token) return;
    if (!city.trim()) {
      Alert.alert('Required', 'Please enter a city');
      return;
    }
    if (!checkIn.trim() || !checkOut.trim()) {
      Alert.alert('Required', 'Please enter check-in and check-out dates');
      return;
    }
    const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
    if (!dateRegex.test(checkIn.trim()) || !dateRegex.test(checkOut.trim())) {
      Alert.alert('Invalid Date', 'Dates must be in YYYY-MM-DD format');
      return;
    }
    if (checkIn.trim() >= checkOut.trim()) {
      Alert.alert('Invalid Dates', 'Check-out must be after check-in');
      return;
    }
    const guestCount = parseInt(guests, 10);
    if (isNaN(guestCount) || guestCount < 1) {
      Alert.alert('Invalid', 'Guests must be at least 1');
      return;
    }

    setSubmitting(true);
    try {
      const body: any = {
        city: city.trim(),
        propertyType,
        checkIn: checkIn.trim(),
        checkOut: checkOut.trim(),
        guests: guestCount,
      };
      if (budgetMin.trim()) body.budgetMinPaise = parseInt(budgetMin.trim(), 10) * 100;
      if (budgetMax.trim()) body.budgetMaxPaise = parseInt(budgetMax.trim(), 10) * 100;
      if (specialReqs.trim()) body.specialRequirements = specialReqs.trim();

      const created = await api.createLookingFor(body, token);
      setRequests((prev) => [created, ...prev]);
      setShowForm(false);
      resetForm();
      Alert.alert('Posted', 'Your request has been posted. We will match you with properties.');
    } catch (err: any) {
      Alert.alert('Error', err.message ?? 'Failed to post request');
    } finally {
      setSubmitting(false);
    }
  }

  /* ---------- View matches ---------- */

  async function handleViewMatches(req: LookingForRequest) {
    if (!token) return;
    setMatchesForCity(req.city);
    setMatchesModalVisible(true);
    setLoadingMatches(true);
    try {
      const data = await api.getLookingForMatches(req.id, token);
      setMatches(data ?? []);
    } catch {
      setMatches([]);
    } finally {
      setLoadingMatches(false);
    }
  }

  /* ---------- Render request card ---------- */

  function renderRequestCard({ item }: { item: LookingForRequest }) {
    const statusStyle = STATUS_COLORS[item.status] ?? STATUS_COLORS.ACTIVE;

    return (
      <View style={styles.card}>
        <View style={styles.cardTopRow}>
          <View style={{ flex: 1 }}>
            <Text style={styles.cardCity}>{item.city}</Text>
            <Text style={styles.cardType}>{item.propertyType}</Text>
          </View>
          <View style={[styles.statusBadge, { backgroundColor: statusStyle.bg }]}>
            <Text style={[styles.statusText, { color: statusStyle.text }]}>{item.status}</Text>
          </View>
        </View>

        <View style={styles.cardDetails}>
          <Text style={styles.detailText}>
            {item.checkIn} to {item.checkOut}
          </Text>
          {(item.budgetMinPaise != null || item.budgetMaxPaise != null) && (
            <Text style={styles.detailText}>
              Budget: {item.budgetMinPaise != null ? formatPaise(item.budgetMinPaise) : '-'}
              {' - '}
              {item.budgetMaxPaise != null ? formatPaise(item.budgetMaxPaise) : '-'}
            </Text>
          )}
          <Text style={styles.detailText}>
            {item.guests} guest{item.guests !== 1 ? 's' : ''}
          </Text>
        </View>

        {item.specialRequirements ? (
          <Text style={styles.specialReqText} numberOfLines={2}>
            {item.specialRequirements}
          </Text>
        ) : null}

        <View style={styles.cardFooter}>
          {(item.matchesCount ?? 0) > 0 && (
            <View style={styles.matchCountBadge}>
              <Text style={styles.matchCountText}>{item.matchesCount} match{item.matchesCount !== 1 ? 'es' : ''}</Text>
            </View>
          )}
          <TouchableOpacity
            style={[styles.viewMatchesBtn, (item.matchesCount ?? 0) === 0 && { opacity: 0.5 }]}
            onPress={() => handleViewMatches(item)}
            disabled={(item.matchesCount ?? 0) === 0}
          >
            <Text style={styles.viewMatchesBtnText}>View Matches</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  /* ---------- Render match card ---------- */

  function renderMatchCard({ item }: { item: MatchedListing }) {
    const listingId = item.listingId ?? item.id;
    return (
      <View style={styles.matchCard}>
        <View style={styles.matchPhoto}>
          {item.photoUrl ? (
            <Image source={{ uri: item.photoUrl }} style={styles.matchImage} />
          ) : (
            <Text style={{ fontSize: 28 }}>🏠</Text>
          )}
        </View>
        <View style={styles.matchBody}>
          <Text style={styles.matchTitle} numberOfLines={1}>{item.title}</Text>
          <Text style={styles.matchCity}>{item.city}</Text>
          <View style={styles.matchMeta}>
            <Text style={styles.matchPrice}>{formatPaise(item.pricePaise)}</Text>
            {item.rating != null && (
              <Text style={styles.matchRating}>★ {item.rating.toFixed(1)}</Text>
            )}
          </View>
          <View style={styles.matchScoreBadge}>
            <Text style={styles.matchScoreText}>{item.matchScore}% match</Text>
          </View>
        </View>
        <TouchableOpacity
          style={styles.viewListingBtn}
          onPress={() => {
            setMatchesModalVisible(false);
            router.push(`/listing/${listingId}`);
          }}
        >
          <Text style={styles.viewListingBtnText}>View</Text>
        </TouchableOpacity>
      </View>
    );
  }

  /* ---------- Main render ---------- */

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>‹ Back</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Looking For</Text>
        <View style={{ width: 60 }} />
      </View>

      {/* Toggle between form and list */}
      <View style={styles.topBar}>
        <Text style={styles.subtitle}>AI Scout finds properties for you</Text>
        <TouchableOpacity
          style={styles.addBtn}
          onPress={() => setShowForm(!showForm)}
        >
          <Text style={styles.addBtnText}>{showForm ? 'My Requests' : '+ New Request'}</Text>
        </TouchableOpacity>
      </View>

      {showForm ? (
        /* ---------- Create Request Form ---------- */
        <ScrollView style={styles.formScroll} contentContainerStyle={styles.formContainer}>
          <Text style={styles.formTitle}>What are you looking for?</Text>

          <Text style={styles.label}>City *</Text>
          <TextInput
            style={styles.input}
            value={city}
            onChangeText={setCity}
            placeholder="e.g. Hyderabad, Mumbai, Goa"
            placeholderTextColor="#9ca3af"
          />

          <Text style={styles.label}>Property Type</Text>
          <TouchableOpacity
            style={styles.pickerBtn}
            onPress={() => setShowTypePicker(true)}
          >
            <Text style={styles.pickerText}>{propertyType}</Text>
            <Text style={styles.pickerArrow}>▼</Text>
          </TouchableOpacity>

          <View style={styles.row}>
            <View style={styles.halfField}>
              <Text style={styles.label}>Check-in *</Text>
              <TextInput
                style={styles.input}
                value={checkIn}
                onChangeText={setCheckIn}
                placeholder="YYYY-MM-DD"
                placeholderTextColor="#9ca3af"
              />
            </View>
            <View style={styles.halfField}>
              <Text style={styles.label}>Check-out *</Text>
              <TextInput
                style={styles.input}
                value={checkOut}
                onChangeText={setCheckOut}
                placeholder="YYYY-MM-DD"
                placeholderTextColor="#9ca3af"
              />
            </View>
          </View>

          <View style={styles.row}>
            <View style={styles.halfField}>
              <Text style={styles.label}>Min Budget (INR)</Text>
              <TextInput
                style={styles.input}
                value={budgetMin}
                onChangeText={setBudgetMin}
                placeholder="e.g. 1000"
                placeholderTextColor="#9ca3af"
                keyboardType="numeric"
              />
            </View>
            <View style={styles.halfField}>
              <Text style={styles.label}>Max Budget (INR)</Text>
              <TextInput
                style={styles.input}
                value={budgetMax}
                onChangeText={setBudgetMax}
                placeholder="e.g. 5000"
                placeholderTextColor="#9ca3af"
                keyboardType="numeric"
              />
            </View>
          </View>

          <Text style={styles.label}>Number of Guests</Text>
          <TextInput
            style={styles.input}
            value={guests}
            onChangeText={setGuests}
            keyboardType="numeric"
            placeholder="2"
            placeholderTextColor="#9ca3af"
          />

          <Text style={styles.label}>Special Requirements</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            value={specialReqs}
            onChangeText={setSpecialReqs}
            placeholder="e.g. pet-friendly, near beach, wheelchair accessible..."
            placeholderTextColor="#9ca3af"
            multiline
            numberOfLines={4}
            textAlignVertical="top"
          />

          <TouchableOpacity
            style={[styles.postBtn, submitting && { opacity: 0.6 }]}
            onPress={handlePost}
            disabled={submitting}
          >
            {submitting ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.postBtnText}>Post Request</Text>
            )}
          </TouchableOpacity>
        </ScrollView>
      ) : (
        /* ---------- My Requests List ---------- */
        loading ? (
          <ActivityIndicator size="large" color="#f97316" style={{ marginTop: 40 }} />
        ) : (
          <FlatList
            data={requests}
            keyExtractor={(item) => item.id}
            renderItem={renderRequestCard}
            contentContainerStyle={requests.length === 0 ? styles.emptyContainer : styles.list}
            ListEmptyComponent={
              <View style={styles.empty}>
                <Text style={styles.emptyIcon}>🔍</Text>
                <Text style={styles.emptyTitle}>No requests yet</Text>
                <Text style={styles.emptySubtitle}>
                  Tell us what you are looking for and our AI Scout will match you with the best properties
                </Text>
                <TouchableOpacity style={styles.emptyAddBtn} onPress={() => setShowForm(true)}>
                  <Text style={styles.emptyAddBtnText}>Create Request</Text>
                </TouchableOpacity>
              </View>
            }
          />
        )
      )}

      {/* Property Type Picker Modal */}
      <Modal visible={showTypePicker} animationType="fade" transparent>
        <TouchableOpacity
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setShowTypePicker(false)}
        >
          <View style={styles.pickerModal}>
            <Text style={styles.pickerModalTitle}>Select Property Type</Text>
            {PROPERTY_TYPES.map((type) => (
              <TouchableOpacity
                key={type}
                style={[
                  styles.pickerOption,
                  propertyType === type && styles.pickerOptionActive,
                ]}
                onPress={() => {
                  setPropertyType(type);
                  setShowTypePicker(false);
                }}
              >
                <Text
                  style={[
                    styles.pickerOptionText,
                    propertyType === type && styles.pickerOptionTextActive,
                  ]}
                >
                  {type}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Matches Modal */}
      <Modal visible={matchesModalVisible} animationType="slide" transparent>
        <View style={styles.modalOverlay}>
          <View style={styles.matchesModal}>
            <View style={styles.matchesHeader}>
              <Text style={styles.matchesTitle}>Matches in {matchesForCity}</Text>
              <TouchableOpacity onPress={() => setMatchesModalVisible(false)}>
                <Text style={styles.closeText}>Close</Text>
              </TouchableOpacity>
            </View>

            {loadingMatches ? (
              <ActivityIndicator size="large" color="#f97316" style={{ marginTop: 40 }} />
            ) : matches.length === 0 ? (
              <View style={styles.emptyMatches}>
                <Text style={styles.emptyMatchesText}>No matches found yet. Check back soon.</Text>
              </View>
            ) : (
              <FlatList
                data={matches}
                keyExtractor={(item) => item.id}
                renderItem={renderMatchCard}
                contentContainerStyle={{ paddingBottom: 20 }}
              />
            )}
          </View>
        </View>
      </Modal>
    </View>
  );
}

/* ---------- Styles ---------- */

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },

  /* Header */
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 54,
    paddingBottom: 12,
    paddingHorizontal: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  backBtn: { width: 60 },
  backText: { fontSize: 17, color: '#f97316', fontWeight: '600' },
  headerTitle: { fontSize: 17, fontWeight: '700', color: '#111827' },

  /* Top bar */
  topBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  subtitle: { fontSize: 13, color: '#6b7280', flex: 1, marginRight: 8 },
  addBtn: {
    backgroundColor: '#f97316',
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 8,
  },
  addBtnText: { color: '#fff', fontWeight: '600', fontSize: 13 },

  /* Form */
  formScroll: { flex: 1 },
  formContainer: { padding: 16, paddingBottom: 40 },
  formTitle: { fontSize: 20, fontWeight: '700', color: '#111827', marginBottom: 16 },
  label: { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 4, marginTop: 12 },
  input: {
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 15,
    color: '#111827',
    backgroundColor: '#f9fafb',
  },
  textArea: { minHeight: 90 },
  row: { flexDirection: 'row', gap: 12 },
  halfField: { flex: 1 },

  /* Property type picker button */
  pickerBtn: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    backgroundColor: '#f9fafb',
  },
  pickerText: { fontSize: 15, color: '#111827' },
  pickerArrow: { fontSize: 12, color: '#6b7280' },

  /* Post button */
  postBtn: {
    backgroundColor: '#f97316',
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 24,
  },
  postBtnText: { color: '#fff', fontWeight: '700', fontSize: 16 },

  /* Request cards */
  list: { padding: 16, paddingBottom: 40 },
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    padding: 16,
    marginBottom: 12,
  },
  cardTopRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  cardCity: { fontSize: 17, fontWeight: '700', color: '#111827' },
  cardType: { fontSize: 13, color: '#6b7280', marginTop: 2 },
  statusBadge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  statusText: { fontSize: 11, fontWeight: '700' },
  cardDetails: { marginTop: 10 },
  detailText: { fontSize: 13, color: '#374151', marginBottom: 2 },
  specialReqText: { fontSize: 12, color: '#9ca3af', fontStyle: 'italic', marginTop: 6 },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 12,
    paddingTop: 10,
    borderTopWidth: 1,
    borderTopColor: '#f3f4f6',
  },
  matchCountBadge: {
    backgroundColor: '#dbeafe',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  matchCountText: { fontSize: 12, fontWeight: '600', color: '#2563eb' },
  viewMatchesBtn: {
    backgroundColor: '#f97316',
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 8,
  },
  viewMatchesBtnText: { color: '#fff', fontWeight: '600', fontSize: 13 },

  /* Empty state */
  emptyContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  empty: { alignItems: 'center', paddingHorizontal: 32 },
  emptyIcon: { fontSize: 48, marginBottom: 12 },
  emptyTitle: { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 8 },
  emptySubtitle: { fontSize: 14, color: '#6b7280', textAlign: 'center', lineHeight: 20 },
  emptyAddBtn: {
    backgroundColor: '#f97316',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 10,
    marginTop: 20,
  },
  emptyAddBtnText: { color: '#fff', fontWeight: '700', fontSize: 15 },

  /* Modals */
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },

  /* Type picker modal */
  pickerModal: {
    backgroundColor: '#fff',
    borderRadius: 16,
    padding: 20,
    width: '80%',
    maxHeight: '70%',
  },
  pickerModalTitle: { fontSize: 17, fontWeight: '700', color: '#111827', marginBottom: 12 },
  pickerOption: {
    paddingVertical: 12,
    paddingHorizontal: 12,
    borderRadius: 8,
    marginBottom: 4,
  },
  pickerOptionActive: { backgroundColor: '#fff7ed' },
  pickerOptionText: { fontSize: 15, color: '#374151' },
  pickerOptionTextActive: { color: '#f97316', fontWeight: '600' },

  /* Matches modal */
  matchesModal: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    width: '100%',
    height: '80%',
    position: 'absolute',
    bottom: 0,
    paddingHorizontal: 16,
    paddingTop: 16,
  },
  matchesHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  matchesTitle: { fontSize: 17, fontWeight: '700', color: '#111827' },
  closeText: { fontSize: 15, fontWeight: '600', color: '#f97316' },
  emptyMatches: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  emptyMatchesText: { fontSize: 14, color: '#6b7280', textAlign: 'center' },

  /* Match cards */
  matchCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#e5e7eb',
    padding: 12,
    marginBottom: 10,
  },
  matchPhoto: {
    width: 64,
    height: 64,
    borderRadius: 10,
    backgroundColor: '#f3f4f6',
    justifyContent: 'center',
    alignItems: 'center',
    overflow: 'hidden',
  },
  matchImage: { width: 64, height: 64, borderRadius: 10 },
  matchBody: { flex: 1, marginLeft: 12 },
  matchTitle: { fontSize: 15, fontWeight: '600', color: '#111827' },
  matchCity: { fontSize: 12, color: '#6b7280', marginTop: 1 },
  matchMeta: { flexDirection: 'row', alignItems: 'center', marginTop: 4, gap: 8 },
  matchPrice: { fontSize: 14, fontWeight: '700', color: '#111827' },
  matchRating: { fontSize: 12, color: '#f59e0b', fontWeight: '600' },
  matchScoreBadge: {
    backgroundColor: '#f0fdf4',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 8,
    alignSelf: 'flex-start',
    marginTop: 4,
  },
  matchScoreText: { fontSize: 11, fontWeight: '700', color: '#16a34a' },
  viewListingBtn: {
    backgroundColor: '#f97316',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    marginLeft: 8,
  },
  viewListingBtnText: { color: '#fff', fontWeight: '600', fontSize: 12 },
});
