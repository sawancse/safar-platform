import { useEffect, useState } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, Image,
  StyleSheet, ActivityIndicator, Dimensions, Modal, TextInput, Alert,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import Constants from 'expo-constants';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const API_URL = Constants.expoConfig?.extra?.apiUrl ?? 'http://localhost:8080';
const ORANGE = '#f97316';
const SCREEN_WIDTH = Dimensions.get('window').width;

function formatPrice(paise: number): string {
  const rupees = paise / 100;
  if (rupees >= 10000000) return `₹${(rupees / 10000000).toFixed(2)} Cr`;
  if (rupees >= 100000) return `₹${(rupees / 100000).toFixed(2)} L`;
  return `₹${rupees.toLocaleString('en-IN')}`;
}

function estimateEmi(pricePaise: number, ratePercent: number, years: number): string {
  const principal = pricePaise / 100;
  const monthlyRate = ratePercent / 12 / 100;
  const months = years * 12;
  if (monthlyRate === 0) return formatPrice((principal / months) * 100);
  const emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, months)) / (Math.pow(1 + monthlyRate, months) - 1);
  return `₹${Math.round(emi).toLocaleString('en-IN')}`;
}

export default function BuyPropertyDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const [property, setProperty] = useState<any>(null);
  const [photos, setPhotos] = useState<string[]>([]);
  const [photoIndex, setPhotoIndex] = useState(0);
  const [loading, setLoading] = useState(true);
  const [showInquiry, setShowInquiry] = useState(false);
  const [showVisit, setShowVisit] = useState(false);
  const [inquiryMessage, setInquiryMessage] = useState('');
  const [inquiryPhone, setInquiryPhone] = useState('');
  const [visitDate, setVisitDate] = useState('');
  const [visitTime, setVisitTime] = useState('');
  const [visitNote, setVisitNote] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!id) return;
    (async () => {
      try {
        const token = await getAccessToken();
        const data = await api.getSaleProperty(id, token || undefined);
        setProperty(data);
        const mediaPhotos = (data.photos || []).map((p: any) =>
          typeof p === 'string' ? (p.startsWith('http') ? p : API_URL + p) : (p.url?.startsWith('http') ? p.url : API_URL + p.url)
        ).filter(Boolean);
        if (mediaPhotos.length > 0) {
          setPhotos(mediaPhotos);
        } else if (data.primaryPhotoUrl) {
          setPhotos([data.primaryPhotoUrl.startsWith('http') ? data.primaryPhotoUrl : API_URL + data.primaryPhotoUrl]);
        }
      } catch {
        setProperty(null);
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  const handleInquiry = async () => {
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    if (!inquiryMessage.trim()) { Alert.alert('Required', 'Please enter a message'); return; }
    setSubmitting(true);
    try {
      await api.createInquiry({
        salePropertyId: id,
        message: inquiryMessage.trim(),
        phone: inquiryPhone.trim() || undefined,
      }, token);
      Alert.alert('Sent', 'Your inquiry has been sent to the seller.');
      setShowInquiry(false);
      setInquiryMessage('');
      setInquiryPhone('');
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to send inquiry');
    } finally {
      setSubmitting(false);
    }
  };

  const handleScheduleVisit = async () => {
    const token = await getAccessToken();
    if (!token) { router.push('/auth'); return; }
    if (!visitDate.trim()) { Alert.alert('Required', 'Please enter a date (YYYY-MM-DD)'); return; }
    if (!visitTime.trim()) { Alert.alert('Required', 'Please enter a time (e.g. 10:00 AM)'); return; }
    setSubmitting(true);
    try {
      await api.scheduleSiteVisit({
        salePropertyId: id,
        preferredDate: visitDate.trim(),
        preferredTime: visitTime.trim(),
        note: visitNote.trim() || undefined,
      }, token);
      Alert.alert('Scheduled', 'Your visit request has been sent. The seller will confirm shortly.');
      setShowVisit(false);
      setVisitDate('');
      setVisitTime('');
      setVisitNote('');
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to schedule visit');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <View style={styles.center}><ActivityIndicator size="large" color={ORANGE} /></View>;
  }

  if (!property) {
    return <View style={styles.center}><Text style={{ color: '#9ca3af' }}>Property not found</Text></View>;
  }

  const overviewItems = [
    property.bhk ? { label: 'BHK', value: `${property.bhk} BHK` } : null,
    property.bathrooms ? { label: 'Bathrooms', value: String(property.bathrooms) } : null,
    property.areaSqft ? { label: 'Area', value: `${property.areaSqft} sq.ft` } : null,
    property.floor ? { label: 'Floor', value: `${property.floor}${property.totalFloors ? `/${property.totalFloors}` : ''}` } : null,
    property.facing ? { label: 'Facing', value: property.facing } : null,
    property.furnishing ? { label: 'Furnishing', value: property.furnishing } : null,
    property.propertyAge ? { label: 'Age', value: `${property.propertyAge} yrs` } : null,
    property.parking ? { label: 'Parking', value: property.parking } : null,
  ].filter(Boolean);

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: '#fff' }}>
      <ScrollView style={styles.container} contentContainerStyle={{ paddingBottom: 100 }}>
        {/* Photo Gallery */}
        <View>
          {photos.length > 0 ? (
            <View>
              <ScrollView
                horizontal pagingEnabled showsHorizontalScrollIndicator={false}
                onMomentumScrollEnd={(e) => {
                  const idx = Math.round(e.nativeEvent.contentOffset.x / SCREEN_WIDTH);
                  setPhotoIndex(idx);
                }}
              >
                {photos.map((url, i) => (
                  <Image key={i} source={{ uri: url }} style={{ width: SCREEN_WIDTH, height: 280 }} resizeMode="cover" />
                ))}
              </ScrollView>
              {photos.length > 1 && (
                <View style={styles.dotsRow}>
                  {photos.map((_, i) => (
                    <View key={i} style={[styles.dot, i === photoIndex && styles.dotActive]} />
                  ))}
                </View>
              )}
              <View style={styles.photoCounter}>
                <Text style={styles.photoCounterText}>{photoIndex + 1}/{photos.length}</Text>
              </View>
            </View>
          ) : (
            <View style={styles.noPhoto}><Text style={{ fontSize: 60 }}>🏠</Text></View>
          )}
          <TouchableOpacity style={styles.backFloating} onPress={() => router.back()}>
            <Text style={{ fontSize: 18, color: '#374151', fontWeight: '700' }}>‹</Text>
          </TouchableOpacity>
        </View>

        {/* Title + Badges */}
        <View style={styles.badgeRow}>
          {property.propertyType && (
            <View style={[styles.badge, { backgroundColor: '#f3f4f6' }]}>
              <Text style={[styles.badgeText, { color: '#4b5563' }]}>{property.propertyType}</Text>
            </View>
          )}
          {property.reraApproved && (
            <View style={[styles.badge, { backgroundColor: '#dcfce7' }]}>
              <Text style={[styles.badgeText, { color: '#15803d' }]}>RERA Approved</Text>
            </View>
          )}
          {property.vastuCompliant && (
            <View style={[styles.badge, { backgroundColor: '#dbeafe' }]}>
              <Text style={[styles.badgeText, { color: '#1d4ed8' }]}>Vastu Compliant</Text>
            </View>
          )}
          {property.readyToMove && (
            <View style={[styles.badge, { backgroundColor: '#fef3c7' }]}>
              <Text style={[styles.badgeText, { color: '#92400e' }]}>Ready to Move</Text>
            </View>
          )}
        </View>

        <Text style={styles.title}>{property.title || 'Untitled Property'}</Text>
        <Text style={styles.location}>
          {[property.locality, property.city, property.state].filter(Boolean).join(', ')}
        </Text>

        {/* Price + EMI */}
        <View style={styles.priceCard}>
          <Text style={styles.priceLabel}>Price</Text>
          <Text style={styles.priceValue}>{formatPrice(property.pricePaise || 0)}</Text>
          {property.pricePerSqft && (
            <Text style={styles.pricePerSqft}>₹{property.pricePerSqft.toLocaleString('en-IN')} / sq.ft</Text>
          )}
          <View style={styles.emiRow}>
            <Text style={styles.emiLabel}>EMI (approx.):</Text>
            <Text style={styles.emiValue}>
              {estimateEmi(property.pricePaise || 0, 8.5, 20)}/mo
            </Text>
          </View>
          <Text style={styles.emiNote}>*Based on 8.5% for 20 years, 80% LTV</Text>
        </View>

        {/* Overview Grid */}
        {overviewItems.length > 0 && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Overview</Text>
            <View style={styles.overviewGrid}>
              {overviewItems.map((item: any, i: number) => (
                <View key={i} style={styles.overviewCell}>
                  <Text style={styles.overviewLabel}>{item.label}</Text>
                  <Text style={styles.overviewValue}>{item.value}</Text>
                </View>
              ))}
            </View>
          </View>
        )}

        {/* Description */}
        {property.description && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Description</Text>
            <Text style={styles.descText}>{property.description}</Text>
          </View>
        )}

        {/* Amenities */}
        {property.amenities && property.amenities.length > 0 && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Amenities</Text>
            <View style={styles.amenitiesWrap}>
              {property.amenities.map((a: string) => (
                <View key={a} style={styles.amenityChip}>
                  <Text style={styles.amenityText}>{a.replace(/_/g, ' ')}</Text>
                </View>
              ))}
            </View>
          </View>
        )}

        {/* RERA Info */}
        {property.reraNumber && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>RERA Information</Text>
            <View style={styles.reraCard}>
              <Text style={styles.reraLabel}>RERA Number</Text>
              <Text style={styles.reraValue}>{property.reraNumber}</Text>
              {property.reraExpiry && (
                <>
                  <Text style={[styles.reraLabel, { marginTop: 8 }]}>Valid Until</Text>
                  <Text style={styles.reraValue}>{property.reraExpiry}</Text>
                </>
              )}
            </View>
          </View>
        )}

        {/* Rental History */}
        {property.linkedListingId && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Rental History</Text>
            <TouchableOpacity
              style={styles.rentalLink}
              onPress={() => router.push(`/listing/${property.linkedListingId}`)}
            >
              <Text style={styles.rentalLinkText}>This property is also listed for rent. View rental listing</Text>
              <Text style={{ color: ORANGE, fontSize: 16 }}>›</Text>
            </TouchableOpacity>
          </View>
        )}

        {/* Address */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Address</Text>
          <Text style={styles.descText}>
            {[property.addressLine1, property.addressLine2, property.locality, property.city, property.state, property.pincode].filter(Boolean).join(', ')}
          </Text>
        </View>
      </ScrollView>

      {/* Bottom Fixed Bar */}
      <View style={styles.bottomBar}>
        <TouchableOpacity
          style={[styles.bottomBtn, styles.bottomBtnOutline]}
          onPress={() => setShowInquiry(true)}
          activeOpacity={0.8}
        >
          <Text style={styles.bottomBtnOutlineText}>Contact Seller</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.bottomBtn, styles.bottomBtnFill]}
          onPress={() => setShowVisit(true)}
          activeOpacity={0.8}
        >
          <Text style={styles.bottomBtnFillText}>Schedule Visit</Text>
        </TouchableOpacity>
      </View>

      {/* Inquiry Modal */}
      <Modal visible={showInquiry} transparent animationType="slide" onRequestClose={() => setShowInquiry(false)}>
        <View style={styles.modalOverlay}>
          <View style={styles.modalSheet}>
            <View style={styles.modalHandle} />
            <Text style={styles.modalTitle}>Contact Seller</Text>
            <Text style={styles.modalSub}>Send an inquiry about this property</Text>
            <TextInput
              style={[styles.modalInput, { height: 100, textAlignVertical: 'top' }]}
              placeholder="Your message to the seller..."
              placeholderTextColor="#9ca3af"
              multiline
              value={inquiryMessage}
              onChangeText={setInquiryMessage}
            />
            <TextInput
              style={styles.modalInput}
              placeholder="Your phone number (optional)"
              placeholderTextColor="#9ca3af"
              keyboardType="phone-pad"
              value={inquiryPhone}
              onChangeText={setInquiryPhone}
            />
            <View style={styles.modalActions}>
              <TouchableOpacity style={styles.modalCancel} onPress={() => setShowInquiry(false)}>
                <Text style={styles.modalCancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.modalSubmit} onPress={handleInquiry} disabled={submitting}>
                {submitting ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.modalSubmitText}>Send Inquiry</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* Schedule Visit Modal */}
      <Modal visible={showVisit} transparent animationType="slide" onRequestClose={() => setShowVisit(false)}>
        <View style={styles.modalOverlay}>
          <View style={styles.modalSheet}>
            <View style={styles.modalHandle} />
            <Text style={styles.modalTitle}>Schedule Site Visit</Text>
            <Text style={styles.modalSub}>Pick your preferred date and time</Text>
            <TextInput
              style={styles.modalInput}
              placeholder="Date (YYYY-MM-DD)"
              placeholderTextColor="#9ca3af"
              value={visitDate}
              onChangeText={setVisitDate}
            />
            <TextInput
              style={styles.modalInput}
              placeholder="Time (e.g. 10:00 AM)"
              placeholderTextColor="#9ca3af"
              value={visitTime}
              onChangeText={setVisitTime}
            />
            <TextInput
              style={[styles.modalInput, { height: 80, textAlignVertical: 'top' }]}
              placeholder="Additional notes (optional)"
              placeholderTextColor="#9ca3af"
              multiline
              value={visitNote}
              onChangeText={setVisitNote}
            />
            <View style={styles.modalActions}>
              <TouchableOpacity style={styles.modalCancel} onPress={() => setShowVisit(false)}>
                <Text style={styles.modalCancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.modalSubmit} onPress={handleScheduleVisit} disabled={submitting}>
                {submitting ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.modalSubmitText}>Request Visit</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#fff' },

  dotsRow: { position: 'absolute', bottom: 12, left: 0, right: 0, flexDirection: 'row', justifyContent: 'center', gap: 4 },
  dot: { width: 6, height: 6, borderRadius: 3, backgroundColor: 'rgba(255,255,255,0.5)' },
  dotActive: { backgroundColor: '#fff', width: 8, height: 8, borderRadius: 4 },
  photoCounter: { position: 'absolute', bottom: 12, right: 16, backgroundColor: 'rgba(0,0,0,0.6)', borderRadius: 10, paddingHorizontal: 8, paddingVertical: 3 },
  photoCounterText: { color: '#fff', fontSize: 11, fontWeight: '600' },
  noPhoto: { width: SCREEN_WIDTH, height: 280, backgroundColor: '#f3f4f6', alignItems: 'center', justifyContent: 'center' },
  backFloating: {
    position: 'absolute', top: 12, left: 16, width: 36, height: 36, borderRadius: 18,
    backgroundColor: 'rgba(255,255,255,0.9)', alignItems: 'center', justifyContent: 'center',
  },

  badgeRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, paddingHorizontal: 16, paddingTop: 16 },
  badge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 8 },
  badgeText: { fontSize: 11, fontWeight: '700' },

  title: { fontSize: 20, fontWeight: '800', color: '#111827', paddingHorizontal: 16, marginTop: 8 },
  location: { fontSize: 13, color: '#6b7280', paddingHorizontal: 16, marginTop: 4 },

  priceCard: {
    marginHorizontal: 16, marginTop: 16, backgroundColor: '#fffbeb', borderRadius: 14,
    padding: 16, borderWidth: 1, borderColor: '#fde68a',
  },
  priceLabel: { fontSize: 12, fontWeight: '600', color: '#92400e' },
  priceValue: { fontSize: 26, fontWeight: '800', color: '#111827', marginTop: 2 },
  pricePerSqft: { fontSize: 13, color: '#6b7280', marginTop: 2 },
  emiRow: { flexDirection: 'row', alignItems: 'center', marginTop: 10, gap: 6 },
  emiLabel: { fontSize: 13, color: '#6b7280' },
  emiValue: { fontSize: 14, fontWeight: '700', color: ORANGE },
  emiNote: { fontSize: 10, color: '#9ca3af', marginTop: 4 },

  section: { paddingHorizontal: 16, paddingTop: 20 },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: '#111827', marginBottom: 10 },

  overviewGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  overviewCell: {
    width: (SCREEN_WIDTH - 32 - 20) / 3, backgroundColor: '#f9fafb', borderRadius: 10,
    padding: 12, alignItems: 'center',
  },
  overviewLabel: { fontSize: 11, color: '#9ca3af', fontWeight: '500' },
  overviewValue: { fontSize: 14, fontWeight: '700', color: '#111827', marginTop: 4 },

  descText: { fontSize: 14, color: '#374151', lineHeight: 22 },

  amenitiesWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  amenityChip: { backgroundColor: '#f3f4f6', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 100 },
  amenityText: { fontSize: 12, color: '#374151' },

  reraCard: { backgroundColor: '#f0fdf4', borderRadius: 10, padding: 14 },
  reraLabel: { fontSize: 11, color: '#6b7280', fontWeight: '500' },
  reraValue: { fontSize: 14, fontWeight: '700', color: '#111827', marginTop: 2 },

  rentalLink: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    backgroundColor: '#fff7ed', borderRadius: 10, padding: 14, borderWidth: 1, borderColor: '#fed7aa',
  },
  rentalLinkText: { flex: 1, fontSize: 13, color: '#374151', lineHeight: 19 },

  bottomBar: {
    position: 'absolute', bottom: 0, left: 0, right: 0, flexDirection: 'row',
    padding: 16, gap: 10, backgroundColor: '#fff', borderTopWidth: 1, borderTopColor: '#f3f4f6',
  },
  bottomBtn: { flex: 1, paddingVertical: 14, borderRadius: 12, alignItems: 'center' },
  bottomBtnOutline: { borderWidth: 2, borderColor: ORANGE },
  bottomBtnFill: { backgroundColor: ORANGE },
  bottomBtnOutlineText: { color: ORANGE, fontWeight: '700', fontSize: 14 },
  bottomBtnFillText: { color: '#fff', fontWeight: '700', fontSize: 14 },

  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  modalSheet: {
    backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20,
    padding: 20, paddingTop: 12,
  },
  modalHandle: { width: 36, height: 4, borderRadius: 2, backgroundColor: '#d1d5db', alignSelf: 'center', marginBottom: 12 },
  modalTitle: { fontSize: 18, fontWeight: '700', color: '#111827' },
  modalSub: { fontSize: 13, color: '#6b7280', marginTop: 2, marginBottom: 16 },
  modalInput: {
    backgroundColor: '#f3f4f6', borderRadius: 10, paddingHorizontal: 14, paddingVertical: 12,
    fontSize: 14, color: '#111827', marginBottom: 10,
  },
  modalActions: { flexDirection: 'row', gap: 10, marginTop: 6 },
  modalCancel: { flex: 1, paddingVertical: 14, borderRadius: 12, backgroundColor: '#f3f4f6', alignItems: 'center' },
  modalCancelText: { fontWeight: '600', color: '#6b7280', fontSize: 14 },
  modalSubmit: { flex: 1, paddingVertical: 14, borderRadius: 12, backgroundColor: ORANGE, alignItems: 'center' },
  modalSubmitText: { color: '#fff', fontWeight: '700', fontSize: 14 },
});
