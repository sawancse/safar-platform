import { useEffect, useState, useCallback, useRef } from 'react';
import {
  View, Text, TextInput, TouchableOpacity, FlatList, Image,
  StyleSheet, ActivityIndicator, Dimensions, Modal, ScrollView,
  RefreshControl,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const ORANGE = '#f97316';
const SCREEN_WIDTH = Dimensions.get('window').width;

const SORT_OPTIONS = [
  { key: 'RELEVANCE', label: 'Relevance' },
  { key: 'PRICE_LOW', label: 'Price: Low to High' },
  { key: 'PRICE_HIGH', label: 'Price: High to Low' },
  { key: 'NEWEST', label: 'Newest First' },
  { key: 'AREA', label: 'Area: Largest First' },
];

const PROPERTY_TYPES = ['Apartment', 'Villa', 'Independent House', 'Plot', 'Penthouse', 'Studio', 'Commercial'];
const FURNISHING_OPTIONS = ['Unfurnished', 'Semi-Furnished', 'Fully Furnished'];
const FACING_OPTIONS = ['North', 'South', 'East', 'West', 'North-East', 'North-West', 'South-East', 'South-West'];
const STATUS_OPTIONS = ['Ready to Move', 'Under Construction', 'New Launch'];

function formatPrice(paise: number): string {
  const rupees = paise / 100;
  if (rupees >= 10000000) return `₹${(rupees / 10000000).toFixed(2)} Cr`;
  if (rupees >= 100000) return `₹${(rupees / 100000).toFixed(2)} L`;
  return `₹${rupees.toLocaleString('en-IN')}`;
}

export default function BuySearchScreen() {
  const params = useLocalSearchParams<{
    city?: string; locality?: string; bhk?: string;
    minPrice?: string; maxPrice?: string;
  }>();
  const router = useRouter();

  const [properties, setProperties] = useState<any[]>([]);
  const [totalHits, setTotalHits] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const hasMore = useRef(true);

  // Filters
  const [sort, setSort] = useState('RELEVANCE');
  const [showSort, setShowSort] = useState(false);
  const [showFilters, setShowFilters] = useState(false);

  const [filterCity, setFilterCity] = useState(params.city || '');
  const [filterLocality, setFilterLocality] = useState(params.locality || '');
  const [filterBhk, setFilterBhk] = useState(params.bhk || '');
  const [filterType, setFilterType] = useState('');
  const [filterMinPrice, setFilterMinPrice] = useState(params.minPrice || '');
  const [filterMaxPrice, setFilterMaxPrice] = useState(params.maxPrice || '');
  const [filterFurnishing, setFilterFurnishing] = useState('');
  const [filterFacing, setFilterFacing] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterRera, setFilterRera] = useState(false);
  const [filterVastu, setFilterVastu] = useState(false);

  const buildParams = useCallback((pageNum: number) => {
    const p: Record<string, string> = { page: String(pageNum), size: '20', sort };
    if (filterCity.trim()) p.city = filterCity.trim();
    if (filterLocality.trim()) p.locality = filterLocality.trim();
    if (filterBhk) p.bhk = filterBhk;
    if (filterType) p.propertyType = filterType;
    if (filterMinPrice) p.minPrice = filterMinPrice;
    if (filterMaxPrice) p.maxPrice = filterMaxPrice;
    if (filterFurnishing) p.furnishing = filterFurnishing;
    if (filterFacing) p.facing = filterFacing;
    if (filterStatus) p.status = filterStatus;
    if (filterRera) p.reraApproved = 'true';
    if (filterVastu) p.vastuCompliant = 'true';
    return p;
  }, [sort, filterCity, filterLocality, filterBhk, filterType, filterMinPrice, filterMaxPrice, filterFurnishing, filterFacing, filterStatus, filterRera, filterVastu]);

  const fetchProperties = useCallback(async (pageNum: number, replace: boolean) => {
    try {
      const token = await getAccessToken();
      const data = await api.searchSaleProperties(buildParams(pageNum), token || undefined);
      const items = data.properties || [];
      if (replace) {
        setProperties(items);
      } else {
        setProperties((prev) => [...prev, ...items]);
      }
      setTotalHits(data.totalHits || 0);
      hasMore.current = items.length === 20;
      setPage(pageNum);
    } catch {
      if (replace) setProperties([]);
    } finally {
      setLoading(false);
      setLoadingMore(false);
      setRefreshing(false);
    }
  }, [buildParams]);

  useEffect(() => {
    setLoading(true);
    fetchProperties(0, true);
  }, [sort]);

  const onRefresh = () => {
    setRefreshing(true);
    fetchProperties(0, true);
  };

  const onEndReached = () => {
    if (!hasMore.current || loadingMore || loading) return;
    setLoadingMore(true);
    fetchProperties(page + 1, false);
  };

  const applyFilters = () => {
    setShowFilters(false);
    setLoading(true);
    fetchProperties(0, true);
  };

  const clearFilters = () => {
    setFilterCity('');
    setFilterLocality('');
    setFilterBhk('');
    setFilterType('');
    setFilterMinPrice('');
    setFilterMaxPrice('');
    setFilterFurnishing('');
    setFilterFacing('');
    setFilterStatus('');
    setFilterRera(false);
    setFilterVastu(false);
  };

  const summaryParts: string[] = [];
  if (filterBhk) summaryParts.push(`${filterBhk} BHK`);
  if (filterType) summaryParts.push(filterType);
  if (filterCity) summaryParts.push(filterCity);
  if (filterLocality) summaryParts.push(filterLocality);
  const summaryText = summaryParts.length > 0 ? summaryParts.join(' in ') : 'All Properties';

  const renderProperty = ({ item }: { item: any }) => (
    <TouchableOpacity
      style={styles.card}
      onPress={() => router.push(`/buy-property/${item.id}`)}
      activeOpacity={0.8}
    >
      {item.primaryPhotoUrl ? (
        <Image source={{ uri: item.primaryPhotoUrl }} style={styles.cardImage} resizeMode="cover" />
      ) : (
        <View style={[styles.cardImage, styles.cardImagePlaceholder]}>
          <Text style={{ fontSize: 36 }}>🏠</Text>
        </View>
      )}
      <View style={styles.cardBody}>
        <Text style={styles.cardPrice}>{formatPrice(item.pricePaise || 0)}</Text>
        <Text style={styles.cardTitle} numberOfLines={1}>{item.title || 'Untitled Property'}</Text>
        <View style={styles.cardMetaRow}>
          {item.bhk ? <Text style={styles.cardMeta}>{item.bhk} BHK</Text> : null}
          {item.areaSqft ? <Text style={styles.cardMeta}>{item.areaSqft} sq.ft</Text> : null}
          {item.floor ? <Text style={styles.cardMeta}>Floor {item.floor}</Text> : null}
        </View>
        <Text style={styles.cardLocation} numberOfLines={1}>
          {[item.locality, item.city].filter(Boolean).join(', ')}
        </Text>
        {/* Badges */}
        <View style={styles.badgeRow}>
          {item.reraApproved && (
            <View style={[styles.badge, { backgroundColor: '#dcfce7' }]}>
              <Text style={[styles.badgeText, { color: '#15803d' }]}>RERA</Text>
            </View>
          )}
          {item.vastuCompliant && (
            <View style={[styles.badge, { backgroundColor: '#dbeafe' }]}>
              <Text style={[styles.badgeText, { color: '#1d4ed8' }]}>Vastu</Text>
            </View>
          )}
          {item.readyToMove && (
            <View style={[styles.badge, { backgroundColor: '#fef3c7' }]}>
              <Text style={[styles.badgeText, { color: '#92400e' }]}>Ready</Text>
            </View>
          )}
          {item.furnishing && (
            <View style={[styles.badge, { backgroundColor: '#f3f4f6' }]}>
              <Text style={[styles.badgeText, { color: '#4b5563' }]}>{item.furnishing}</Text>
            </View>
          )}
        </View>
      </View>
    </TouchableOpacity>
  );

  return (
    <SafeAreaView style={styles.safeArea}>
      {/* Summary Bar */}
      <View style={styles.summaryBar}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backArrow}>‹</Text>
        </TouchableOpacity>
        <View style={{ flex: 1 }}>
          <Text style={styles.summaryTitle} numberOfLines={1}>{summaryText}</Text>
          <Text style={styles.summaryCount}>{totalHits} properties found</Text>
        </View>
      </View>

      {/* Filter + Sort Bar */}
      <View style={styles.actionBar}>
        <TouchableOpacity style={styles.actionBtn} onPress={() => setShowFilters(true)}>
          <Text style={styles.actionBtnText}>Filters</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.actionBtn} onPress={() => setShowSort(true)}>
          <Text style={styles.actionBtnText}>Sort: {SORT_OPTIONS.find((s) => s.key === sort)?.label}</Text>
        </TouchableOpacity>
      </View>

      {/* Property List */}
      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={ORANGE} />
        </View>
      ) : properties.length === 0 ? (
        <View style={styles.center}>
          <Text style={{ fontSize: 40, marginBottom: 12 }}>🏠</Text>
          <Text style={{ fontSize: 15, fontWeight: '600', color: '#374151' }}>No properties found</Text>
          <Text style={{ fontSize: 13, color: '#9ca3af', marginTop: 4 }}>Try adjusting your filters</Text>
        </View>
      ) : (
        <FlatList
          data={properties}
          keyExtractor={(item) => item.id}
          renderItem={renderProperty}
          contentContainerStyle={{ padding: 16, paddingBottom: 30 }}
          onEndReached={onEndReached}
          onEndReachedThreshold={0.3}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={ORANGE} />}
          ListFooterComponent={loadingMore ? <ActivityIndicator size="small" color={ORANGE} style={{ marginVertical: 16 }} /> : null}
        />
      )}

      {/* Sort Modal */}
      <Modal visible={showSort} transparent animationType="slide" onRequestClose={() => setShowSort(false)}>
        <View style={styles.modalOverlay}>
          <View style={styles.modalSheet}>
            <View style={styles.modalHandle} />
            <Text style={styles.modalTitle}>Sort By</Text>
            {SORT_OPTIONS.map((opt) => (
              <TouchableOpacity
                key={opt.key}
                style={[styles.sortOption, sort === opt.key && styles.sortOptionActive]}
                onPress={() => { setSort(opt.key); setShowSort(false); }}
              >
                <Text style={[styles.sortOptionText, sort === opt.key && styles.sortOptionTextActive]}>
                  {opt.label}
                </Text>
                {sort === opt.key && <Text style={{ color: ORANGE, fontSize: 16 }}>✓</Text>}
              </TouchableOpacity>
            ))}
          </View>
        </View>
      </Modal>

      {/* Filters Modal */}
      <Modal visible={showFilters} transparent animationType="slide" onRequestClose={() => setShowFilters(false)}>
        <View style={styles.modalOverlay}>
          <View style={[styles.modalSheet, { maxHeight: '85%' }]}>
            <View style={styles.modalHandle} />
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Filters</Text>
              <TouchableOpacity onPress={clearFilters}>
                <Text style={{ color: ORANGE, fontWeight: '600', fontSize: 14 }}>Clear All</Text>
              </TouchableOpacity>
            </View>
            <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 20 }}>
              {/* City */}
              <Text style={styles.fLabel}>City</Text>
              <TextInput
                style={styles.fInput}
                value={filterCity}
                onChangeText={setFilterCity}
                placeholder="Enter city"
                placeholderTextColor="#9ca3af"
              />

              {/* Locality */}
              <Text style={styles.fLabel}>Locality</Text>
              <TextInput
                style={styles.fInput}
                value={filterLocality}
                onChangeText={setFilterLocality}
                placeholder="Enter locality"
                placeholderTextColor="#9ca3af"
              />

              {/* Budget */}
              <Text style={styles.fLabel}>Budget (in paise)</Text>
              <View style={{ flexDirection: 'row', gap: 8 }}>
                <TextInput
                  style={[styles.fInput, { flex: 1 }]}
                  value={filterMinPrice}
                  onChangeText={setFilterMinPrice}
                  placeholder="Min"
                  placeholderTextColor="#9ca3af"
                  keyboardType="numeric"
                />
                <TextInput
                  style={[styles.fInput, { flex: 1 }]}
                  value={filterMaxPrice}
                  onChangeText={setFilterMaxPrice}
                  placeholder="Max"
                  placeholderTextColor="#9ca3af"
                  keyboardType="numeric"
                />
              </View>

              {/* BHK */}
              <Text style={styles.fLabel}>BHK</Text>
              <View style={styles.fChipRow}>
                {['1', '2', '3', '4', '5+'].map((bhk) => (
                  <TouchableOpacity
                    key={bhk}
                    style={[styles.fChip, filterBhk === bhk && styles.fChipActive]}
                    onPress={() => setFilterBhk(filterBhk === bhk ? '' : bhk)}
                  >
                    <Text style={[styles.fChipText, filterBhk === bhk && styles.fChipTextActive]}>{bhk}</Text>
                  </TouchableOpacity>
                ))}
              </View>

              {/* Property Type */}
              <Text style={styles.fLabel}>Property Type</Text>
              <View style={styles.fChipRow}>
                {PROPERTY_TYPES.map((t) => (
                  <TouchableOpacity
                    key={t}
                    style={[styles.fChip, filterType === t && styles.fChipActive]}
                    onPress={() => setFilterType(filterType === t ? '' : t)}
                  >
                    <Text style={[styles.fChipText, filterType === t && styles.fChipTextActive]}>{t}</Text>
                  </TouchableOpacity>
                ))}
              </View>

              {/* Status */}
              <Text style={styles.fLabel}>Status</Text>
              <View style={styles.fChipRow}>
                {STATUS_OPTIONS.map((s) => (
                  <TouchableOpacity
                    key={s}
                    style={[styles.fChip, filterStatus === s && styles.fChipActive]}
                    onPress={() => setFilterStatus(filterStatus === s ? '' : s)}
                  >
                    <Text style={[styles.fChipText, filterStatus === s && styles.fChipTextActive]}>{s}</Text>
                  </TouchableOpacity>
                ))}
              </View>

              {/* Furnishing */}
              <Text style={styles.fLabel}>Furnishing</Text>
              <View style={styles.fChipRow}>
                {FURNISHING_OPTIONS.map((f) => (
                  <TouchableOpacity
                    key={f}
                    style={[styles.fChip, filterFurnishing === f && styles.fChipActive]}
                    onPress={() => setFilterFurnishing(filterFurnishing === f ? '' : f)}
                  >
                    <Text style={[styles.fChipText, filterFurnishing === f && styles.fChipTextActive]}>{f}</Text>
                  </TouchableOpacity>
                ))}
              </View>

              {/* Facing */}
              <Text style={styles.fLabel}>Facing</Text>
              <View style={styles.fChipRow}>
                {FACING_OPTIONS.map((f) => (
                  <TouchableOpacity
                    key={f}
                    style={[styles.fChip, filterFacing === f && styles.fChipActive]}
                    onPress={() => setFilterFacing(filterFacing === f ? '' : f)}
                  >
                    <Text style={[styles.fChipText, filterFacing === f && styles.fChipTextActive]}>{f}</Text>
                  </TouchableOpacity>
                ))}
              </View>

              {/* RERA & Vastu toggles */}
              <View style={{ flexDirection: 'row', gap: 12, marginTop: 12 }}>
                <TouchableOpacity
                  style={[styles.fToggle, filterRera && styles.fToggleActive]}
                  onPress={() => setFilterRera(!filterRera)}
                >
                  <Text style={[styles.fToggleText, filterRera && styles.fToggleTextActive]}>RERA Approved</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.fToggle, filterVastu && styles.fToggleActive]}
                  onPress={() => setFilterVastu(!filterVastu)}
                >
                  <Text style={[styles.fToggleText, filterVastu && styles.fToggleTextActive]}>Vastu Compliant</Text>
                </TouchableOpacity>
              </View>
            </ScrollView>

            <TouchableOpacity style={styles.applyBtn} onPress={applyFilters} activeOpacity={0.8}>
              <Text style={styles.applyBtnText}>Apply Filters</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#fff' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },

  summaryBar: {
    flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 12,
    borderBottomWidth: 1, borderBottomColor: '#f3f4f6', gap: 10,
  },
  backBtn: { width: 32, height: 32, borderRadius: 16, backgroundColor: '#f3f4f6', alignItems: 'center', justifyContent: 'center' },
  backArrow: { fontSize: 22, color: '#374151', fontWeight: '600', marginTop: -2 },
  summaryTitle: { fontSize: 15, fontWeight: '700', color: '#111827' },
  summaryCount: { fontSize: 12, color: '#6b7280', marginTop: 1 },

  actionBar: {
    flexDirection: 'row', paddingHorizontal: 16, paddingVertical: 8, gap: 10,
    borderBottomWidth: 1, borderBottomColor: '#f3f4f6',
  },
  actionBtn: {
    flex: 1, paddingVertical: 8, borderRadius: 8, backgroundColor: '#f3f4f6',
    alignItems: 'center',
  },
  actionBtnText: { fontSize: 13, fontWeight: '600', color: '#374151' },

  card: {
    backgroundColor: '#fff', borderRadius: 14, marginBottom: 14, overflow: 'hidden',
    elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.06, shadowRadius: 4,
    borderWidth: 1, borderColor: '#f3f4f6',
  },
  cardImage: { width: '100%', height: 180, backgroundColor: '#f3f4f6' },
  cardImagePlaceholder: { alignItems: 'center', justifyContent: 'center' },
  cardBody: { padding: 14 },
  cardPrice: { fontSize: 18, fontWeight: '800', color: '#111827' },
  cardTitle: { fontSize: 14, fontWeight: '500', color: '#374151', marginTop: 2 },
  cardMetaRow: { flexDirection: 'row', gap: 12, marginTop: 6 },
  cardMeta: { fontSize: 12, color: '#6b7280', fontWeight: '500' },
  cardLocation: { fontSize: 12, color: '#9ca3af', marginTop: 4 },
  badgeRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 8 },
  badge: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 6 },
  badgeText: { fontSize: 10, fontWeight: '700' },

  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  modalSheet: {
    backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20,
    padding: 20, paddingTop: 12,
  },
  modalHandle: {
    width: 36, height: 4, borderRadius: 2, backgroundColor: '#d1d5db',
    alignSelf: 'center', marginBottom: 12,
  },
  modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
  modalTitle: { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 12 },

  sortOption: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: '#f3f4f6',
  },
  sortOptionActive: {},
  sortOptionText: { fontSize: 15, color: '#374151' },
  sortOptionTextActive: { color: ORANGE, fontWeight: '600' },

  fLabel: { fontSize: 13, fontWeight: '600', color: '#374151', marginTop: 14, marginBottom: 6 },
  fInput: {
    backgroundColor: '#f3f4f6', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 10,
    fontSize: 14, color: '#111827',
  },
  fChipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  fChip: {
    paddingHorizontal: 14, paddingVertical: 8, borderRadius: 20,
    backgroundColor: '#f3f4f6', borderWidth: 1, borderColor: '#e5e7eb',
  },
  fChipActive: { backgroundColor: '#fff7ed', borderColor: ORANGE },
  fChipText: { fontSize: 12, fontWeight: '600', color: '#6b7280' },
  fChipTextActive: { color: ORANGE },

  fToggle: {
    flex: 1, paddingVertical: 10, borderRadius: 10, backgroundColor: '#f3f4f6',
    alignItems: 'center', borderWidth: 1, borderColor: '#e5e7eb',
  },
  fToggleActive: { backgroundColor: '#fff7ed', borderColor: ORANGE },
  fToggleText: { fontSize: 13, fontWeight: '600', color: '#6b7280' },
  fToggleTextActive: { color: ORANGE },

  applyBtn: {
    backgroundColor: ORANGE, borderRadius: 12, paddingVertical: 14,
    alignItems: 'center', marginTop: 12,
  },
  applyBtnText: { color: '#fff', fontWeight: '700', fontSize: 15 },
});
