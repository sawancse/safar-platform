import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, ScrollView, TextInput, TouchableOpacity,
  StyleSheet, ActivityIndicator, FlatList, Image, Dimensions,
  RefreshControl,
} from 'react-native';
import { useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const ORANGE = '#f97316';
const SCREEN_WIDTH = Dimensions.get('window').width;

const POPULAR_CITIES = [
  { name: 'Mumbai', emoji: '🏙️' },
  { name: 'Delhi', emoji: '🕌' },
  { name: 'Bangalore', emoji: '💻' },
  { name: 'Hyderabad', emoji: '🏰' },
  { name: 'Chennai', emoji: '🛕' },
  { name: 'Pune', emoji: '🏞️' },
  { name: 'Gurgaon', emoji: '🏢' },
  { name: 'Noida', emoji: '🌆' },
  { name: 'Ahmedabad', emoji: '🎪' },
  { name: 'Kolkata', emoji: '🌉' },
  { name: 'Jaipur', emoji: '🏰' },
  { name: 'Goa', emoji: '🏖️' },
];

const BHK_OPTIONS = ['1', '2', '3', '4', '5+'];

const BUDGET_RANGES = [
  { label: 'Under 30L', min: 0, max: 3000000 },
  { label: '30L - 50L', min: 3000000, max: 5000000 },
  { label: '50L - 1Cr', min: 5000000, max: 10000000 },
  { label: '1Cr - 2Cr', min: 10000000, max: 20000000 },
  { label: '2Cr+', min: 20000000, max: 0 },
];

function formatPrice(paise: number): string {
  const rupees = paise / 100;
  if (rupees >= 10000000) {
    return `₹${(rupees / 10000000).toFixed(2)} Cr`;
  }
  if (rupees >= 100000) {
    return `₹${(rupees / 100000).toFixed(2)} L`;
  }
  return `₹${rupees.toLocaleString('en-IN')}`;
}

export default function BuyHomepage() {
  const router = useRouter();
  const [city, setCity] = useState('');
  const [locality, setLocality] = useState('');
  const [selectedBhk, setSelectedBhk] = useState('');
  const [selectedBudget, setSelectedBudget] = useState<number>(-1);
  const [recentProperties, setRecentProperties] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const loadRecent = useCallback(async () => {
    try {
      const token = await getAccessToken();
      const data = await api.searchSaleProperties(
        { sort: 'NEWEST', page: '0', size: '10' },
        token || undefined
      );
      setRecentProperties(data.properties || []);
    } catch {
      setRecentProperties([]);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadRecent();
  }, [loadRecent]);

  const onRefresh = () => {
    setRefreshing(true);
    loadRecent();
  };

  const handleSearch = () => {
    const params: Record<string, string> = {};
    if (city.trim()) params.city = city.trim();
    if (locality.trim()) params.locality = locality.trim();
    if (selectedBhk) params.bhk = selectedBhk;
    if (selectedBudget >= 0) {
      const range = BUDGET_RANGES[selectedBudget];
      if (range.min > 0) params.minPrice = String(range.min * 100);
      if (range.max > 0) params.maxPrice = String(range.max * 100);
    }
    const qs = new URLSearchParams(params).toString();
    router.push(`/buy-search?${qs}`);
  };

  const handleCityPress = (cityName: string) => {
    router.push(`/buy-search?city=${encodeURIComponent(cityName)}`);
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView
        style={styles.container}
        contentContainerStyle={{ paddingBottom: 40 }}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={ORANGE} />}
      >
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Buy Property</Text>
          <Text style={styles.headerSub}>Find your dream home across India</Text>
        </View>

        {/* Search Section */}
        <View style={styles.searchCard}>
          <TextInput
            style={styles.searchInput}
            placeholder="City (e.g. Mumbai, Bangalore)"
            placeholderTextColor="#9ca3af"
            value={city}
            onChangeText={setCity}
          />
          <TextInput
            style={styles.searchInput}
            placeholder="Locality (e.g. Bandra, Whitefield)"
            placeholderTextColor="#9ca3af"
            value={locality}
            onChangeText={setLocality}
          />

          {/* BHK Buttons */}
          <Text style={styles.filterLabel}>BHK</Text>
          <View style={styles.chipRow}>
            {BHK_OPTIONS.map((bhk) => (
              <TouchableOpacity
                key={bhk}
                style={[styles.chip, selectedBhk === bhk && styles.chipActive]}
                onPress={() => setSelectedBhk(selectedBhk === bhk ? '' : bhk)}
              >
                <Text style={[styles.chipText, selectedBhk === bhk && styles.chipTextActive]}>
                  {bhk} BHK
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          {/* Budget Ranges */}
          <Text style={styles.filterLabel}>Budget</Text>
          <View style={styles.chipRow}>
            {BUDGET_RANGES.map((range, i) => (
              <TouchableOpacity
                key={i}
                style={[styles.chip, selectedBudget === i && styles.chipActive]}
                onPress={() => setSelectedBudget(selectedBudget === i ? -1 : i)}
              >
                <Text style={[styles.chipText, selectedBudget === i && styles.chipTextActive]}>
                  {range.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          <TouchableOpacity style={styles.searchBtn} onPress={handleSearch} activeOpacity={0.8}>
            <Text style={styles.searchBtnText}>Search Properties</Text>
          </TouchableOpacity>
        </View>

        {/* Recently Listed */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Recently Listed</Text>
          {loading ? (
            <ActivityIndicator size="small" color={ORANGE} style={{ marginVertical: 20 }} />
          ) : recentProperties.length === 0 ? (
            <Text style={styles.emptyText}>No properties listed yet</Text>
          ) : (
            <FlatList
              horizontal
              data={recentProperties}
              showsHorizontalScrollIndicator={false}
              keyExtractor={(item) => item.id}
              contentContainerStyle={{ paddingRight: 16 }}
              renderItem={({ item }) => (
                <TouchableOpacity
                  style={styles.recentCard}
                  onPress={() => router.push(`/buy-property/${item.id}`)}
                  activeOpacity={0.8}
                >
                  {item.primaryPhotoUrl ? (
                    <Image source={{ uri: item.primaryPhotoUrl }} style={styles.recentImage} resizeMode="cover" />
                  ) : (
                    <View style={[styles.recentImage, styles.recentImagePlaceholder]}>
                      <Text style={{ fontSize: 32 }}>🏠</Text>
                    </View>
                  )}
                  <View style={styles.recentInfo}>
                    <Text style={styles.recentPrice}>{formatPrice(item.pricePaise || 0)}</Text>
                    <Text style={styles.recentTitle} numberOfLines={1}>{item.title || 'Untitled Property'}</Text>
                    <Text style={styles.recentMeta} numberOfLines={1}>
                      {item.bhk ? `${item.bhk} BHK` : ''}{item.bhk && item.areaSqft ? ' · ' : ''}{item.areaSqft ? `${item.areaSqft} sq.ft` : ''}
                    </Text>
                    <Text style={styles.recentLocation} numberOfLines={1}>
                      {[item.locality, item.city].filter(Boolean).join(', ')}
                    </Text>
                  </View>
                </TouchableOpacity>
              )}
            />
          )}
        </View>

        {/* Popular Cities */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Popular Cities</Text>
          <View style={styles.cityGrid}>
            {POPULAR_CITIES.map((c) => (
              <TouchableOpacity
                key={c.name}
                style={styles.cityCard}
                onPress={() => handleCityPress(c.name)}
                activeOpacity={0.7}
              >
                <Text style={styles.cityEmoji}>{c.emoji}</Text>
                <Text style={styles.cityName}>{c.name}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* List Your Property CTA */}
        <TouchableOpacity
          style={styles.ctaBanner}
          onPress={() => router.push('/sell')}
          activeOpacity={0.85}
        >
          <View style={styles.ctaBannerInner}>
            <Text style={styles.ctaBannerEmoji}>🏷️</Text>
            <View style={{ flex: 1 }}>
              <Text style={styles.ctaBannerTitle}>List your property for sale</Text>
              <Text style={styles.ctaBannerSub}>Reach thousands of buyers on Safar</Text>
            </View>
            <Text style={styles.ctaBannerArrow}>›</Text>
          </View>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#fff' },
  container: { flex: 1, backgroundColor: '#f9fafb' },
  header: { backgroundColor: ORANGE, paddingHorizontal: 20, paddingTop: 16, paddingBottom: 24 },
  headerTitle: { fontSize: 24, fontWeight: '800', color: '#fff' },
  headerSub: { fontSize: 14, color: 'rgba(255,255,255,0.85)', marginTop: 4 },

  searchCard: {
    backgroundColor: '#fff', marginHorizontal: 16, marginTop: -12,
    borderRadius: 16, padding: 16, elevation: 4,
    shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 8,
  },
  searchInput: {
    backgroundColor: '#f3f4f6', borderRadius: 10, paddingHorizontal: 14, paddingVertical: 12,
    fontSize: 14, color: '#111827', marginBottom: 10,
  },
  filterLabel: { fontSize: 13, fontWeight: '600', color: '#374151', marginBottom: 6, marginTop: 4 },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 10 },
  chip: {
    paddingHorizontal: 14, paddingVertical: 8, borderRadius: 20,
    backgroundColor: '#f3f4f6', borderWidth: 1, borderColor: '#e5e7eb',
  },
  chipActive: { backgroundColor: '#fff7ed', borderColor: ORANGE },
  chipText: { fontSize: 12, fontWeight: '600', color: '#6b7280' },
  chipTextActive: { color: ORANGE },
  searchBtn: {
    backgroundColor: ORANGE, borderRadius: 12, paddingVertical: 14,
    alignItems: 'center', marginTop: 6,
  },
  searchBtnText: { color: '#fff', fontWeight: '700', fontSize: 15 },

  section: { paddingHorizontal: 16, marginTop: 24 },
  sectionTitle: { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 12 },
  emptyText: { fontSize: 13, color: '#9ca3af', textAlign: 'center', paddingVertical: 20 },

  recentCard: {
    width: SCREEN_WIDTH * 0.6, backgroundColor: '#fff', borderRadius: 14, marginLeft: 4, marginRight: 8,
    overflow: 'hidden', elevation: 2,
    shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.06, shadowRadius: 4,
  },
  recentImage: { width: '100%', height: 120, backgroundColor: '#f3f4f6' },
  recentImagePlaceholder: { alignItems: 'center', justifyContent: 'center' },
  recentInfo: { padding: 10 },
  recentPrice: { fontSize: 16, fontWeight: '800', color: '#111827' },
  recentTitle: { fontSize: 13, fontWeight: '500', color: '#374151', marginTop: 2 },
  recentMeta: { fontSize: 12, color: '#6b7280', marginTop: 2 },
  recentLocation: { fontSize: 11, color: '#9ca3af', marginTop: 2 },

  cityGrid: {
    flexDirection: 'row', flexWrap: 'wrap', gap: 10,
  },
  cityCard: {
    width: (SCREEN_WIDTH - 32 - 30) / 4, backgroundColor: '#fff', borderRadius: 12,
    paddingVertical: 14, alignItems: 'center', elevation: 1,
    shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.04, shadowRadius: 2,
    borderWidth: 1, borderColor: '#f3f4f6',
  },
  cityEmoji: { fontSize: 24, marginBottom: 4 },
  cityName: { fontSize: 11, fontWeight: '600', color: '#374151' },

  ctaBanner: {
    marginHorizontal: 16, marginTop: 24, marginBottom: 16, backgroundColor: '#fff7ed',
    borderRadius: 14, borderWidth: 1, borderColor: '#fed7aa',
  },
  ctaBannerInner: {
    flexDirection: 'row', alignItems: 'center', padding: 16, gap: 12,
  },
  ctaBannerEmoji: { fontSize: 28 },
  ctaBannerTitle: { fontSize: 15, fontWeight: '700', color: '#111827' },
  ctaBannerSub: { fontSize: 12, color: '#6b7280', marginTop: 2 },
  ctaBannerArrow: { fontSize: 28, color: ORANGE, fontWeight: '300' },
});
