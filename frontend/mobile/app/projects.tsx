import { useEffect, useState, useCallback, useRef } from 'react';
import {
  View, Text, TextInput, TouchableOpacity, FlatList, Image,
  StyleSheet, ActivityIndicator, Dimensions, Modal, ScrollView,
  RefreshControl,
} from 'react-native';
import { useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '@/lib/api';

const ORANGE = '#f97316';
const SCREEN_WIDTH = Dimensions.get('window').width;
const CARD_WIDTH = SCREEN_WIDTH - 32;

const POPULAR_CITIES = [
  { name: 'Mumbai', emoji: '' },
  { name: 'Delhi', emoji: '' },
  { name: 'Bangalore', emoji: '' },
  { name: 'Hyderabad', emoji: '' },
  { name: 'Chennai', emoji: '' },
  { name: 'Pune', emoji: '' },
  { name: 'Gurgaon', emoji: '' },
  { name: 'Noida', emoji: '' },
  { name: 'Ahmedabad', emoji: '' },
  { name: 'Kolkata', emoji: '' },
  { name: 'Jaipur', emoji: '' },
  { name: 'Goa', emoji: '' },
];

const STATUS_FILTERS = [
  { key: '', label: 'All' },
  { key: 'UPCOMING', label: 'Upcoming' },
  { key: 'UNDER_CONSTRUCTION', label: 'Under Construction' },
  { key: 'READY_TO_MOVE', label: 'Ready' },
];

const SORT_OPTIONS = [
  { key: 'RELEVANCE', label: 'Relevance' },
  { key: 'PRICE_LOW', label: 'Price: Low to High' },
  { key: 'PRICE_HIGH', label: 'Price: High to Low' },
  { key: 'NEWEST', label: 'Newest First' },
  { key: 'POSSESSION', label: 'Possession: Soonest' },
];

function formatPrice(paise: number): string {
  const rupees = paise / 100;
  if (rupees >= 10000000) return `${(rupees / 10000000).toFixed(2)} Cr`;
  if (rupees >= 100000) return `${(rupees / 100000).toFixed(2)} L`;
  return rupees.toLocaleString('en-IN');
}

function formatPriceRange(minPaise?: number, maxPaise?: number): string {
  if (!minPaise && !maxPaise) return 'Price on request';
  if (minPaise && maxPaise) return `${formatPrice(minPaise)} - ${formatPrice(maxPaise)}`;
  if (minPaise) return `From ${formatPrice(minPaise)}`;
  return `Up to ${formatPrice(maxPaise!)}`;
}

export default function ProjectsScreen() {
  const router = useRouter();

  const [projects, setProjects] = useState<any[]>([]);
  const [totalHits, setTotalHits] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const hasMore = useRef(true);

  // Search & filters
  const [query, setQuery] = useState('');
  const [city, setCity] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [sort, setSort] = useState('RELEVANCE');
  const [showSort, setShowSort] = useState(false);
  const [showCityPicker, setShowCityPicker] = useState(false);

  const buildParams = useCallback((pageNum: number) => {
    const p: Record<string, any> = { page: pageNum, size: 20, sort };
    if (query.trim()) p.q = query.trim();
    if (city) p.city = city;
    if (statusFilter) p.status = statusFilter;
    return p;
  }, [query, city, statusFilter, sort]);

  const fetchProjects = useCallback(async (pageNum: number, append = false) => {
    try {
      const data = await api.searchBuilderProjects(buildParams(pageNum));
      const list = data?.projects || data?.content || [];
      const total = data?.totalHits ?? data?.totalElements ?? 0;
      setTotalHits(total);
      hasMore.current = list.length === 20;
      if (append) {
        setProjects(prev => [...prev, ...list]);
      } else {
        setProjects(list);
      }
    } catch {
      if (!append) setProjects([]);
    } finally {
      setLoading(false);
      setLoadingMore(false);
      setRefreshing(false);
    }
  }, [buildParams]);

  useEffect(() => {
    setLoading(true);
    setPage(0);
    hasMore.current = true;
    fetchProjects(0);
  }, [fetchProjects]);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    setPage(0);
    hasMore.current = true;
    fetchProjects(0);
  }, [fetchProjects]);

  const loadMore = useCallback(() => {
    if (loadingMore || !hasMore.current) return;
    setLoadingMore(true);
    const next = page + 1;
    setPage(next);
    fetchProjects(next, true);
  }, [page, loadingMore, fetchProjects]);

  const renderProject = ({ item }: { item: any }) => {
    const photo = item.coverPhoto || item.photos?.[0];
    const progressPct = item.constructionProgress ?? 0;
    return (
      <TouchableOpacity
        style={styles.card}
        onPress={() => router.push(`/project-detail/${item.id}`)}
        activeOpacity={0.85}
      >
        {photo ? (
          <Image source={{ uri: photo }} style={styles.cardImage} resizeMode="cover" />
        ) : (
          <View style={[styles.cardImage, styles.cardImagePlaceholder]}>
            <Text style={styles.placeholderText}>No Photo</Text>
          </View>
        )}

        {item.reraNumber && (
          <View style={styles.reraBadge}>
            <Text style={styles.reraBadgeText}>RERA</Text>
          </View>
        )}

        <View style={styles.cardBody}>
          <Text style={styles.builderName} numberOfLines={1}>
            {item.builderName || 'Builder'}
          </Text>
          <Text style={styles.projectName} numberOfLines={1}>
            {item.projectName || item.name}
          </Text>
          <Text style={styles.location} numberOfLines={1}>
            {[item.locality, item.city].filter(Boolean).join(', ')}
          </Text>

          {/* BHK range */}
          {item.bhkRange && (
            <Text style={styles.bhkText}>{item.bhkRange}</Text>
          )}

          {/* Price range */}
          <Text style={styles.priceText}>
            {formatPriceRange(item.minPricePaise, item.maxPricePaise)}
          </Text>

          {/* Possession */}
          {item.possessionDate && (
            <Text style={styles.possessionText}>
              Possession: {item.possessionDate}
            </Text>
          )}

          {/* Progress bar */}
          <View style={styles.progressRow}>
            <View style={styles.progressBarBg}>
              <View style={[styles.progressBarFill, { width: `${Math.min(progressPct, 100)}%` }]} />
            </View>
            <Text style={styles.progressText}>{progressPct}%</Text>
          </View>

          {/* Status chip */}
          {item.status && (
            <View style={[styles.statusChip, statusStyle(item.status)]}>
              <Text style={[styles.statusChipText, statusTextStyle(item.status)]}>
                {item.status.replace(/_/g, ' ')}
              </Text>
            </View>
          )}
        </View>
      </TouchableOpacity>
    );
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>{'<'}</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>New Projects</Text>
        <View style={{ width: 40 }} />
      </View>

      {/* Search bar */}
      <View style={styles.searchRow}>
        <TextInput
          style={styles.searchInput}
          placeholder="Search projects, builders, cities..."
          placeholderTextColor="#9ca3af"
          value={query}
          onChangeText={setQuery}
          onSubmitEditing={() => { setPage(0); fetchProjects(0); }}
          returnKeyType="search"
        />
      </View>

      {/* City picker button */}
      <View style={styles.filterRow}>
        <TouchableOpacity
          style={[styles.filterChip, city ? styles.filterChipActive : null]}
          onPress={() => setShowCityPicker(true)}
        >
          <Text style={[styles.filterChipText, city ? styles.filterChipTextActive : null]}>
            {city || 'City'}
          </Text>
        </TouchableOpacity>

        {/* Status filters */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ flex: 1 }}>
          {STATUS_FILTERS.map(s => (
            <TouchableOpacity
              key={s.key}
              style={[styles.filterChip, statusFilter === s.key ? styles.filterChipActive : null]}
              onPress={() => setStatusFilter(s.key)}
            >
              <Text style={[styles.filterChipText, statusFilter === s.key ? styles.filterChipTextActive : null]}>
                {s.label}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        <TouchableOpacity style={styles.sortBtn} onPress={() => setShowSort(true)}>
          <Text style={styles.sortBtnText}>Sort</Text>
        </TouchableOpacity>
      </View>

      {/* Results count */}
      {!loading && (
        <Text style={styles.resultCount}>{totalHits} project{totalHits !== 1 ? 's' : ''} found</Text>
      )}

      {/* Project list */}
      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={ORANGE} />
        </View>
      ) : projects.length === 0 ? (
        <View style={styles.center}>
          <Text style={styles.emptyTitle}>No projects found</Text>
          <Text style={styles.emptySubtitle}>Try changing your search or filters</Text>
        </View>
      ) : (
        <FlatList
          data={projects}
          keyExtractor={item => item.id}
          renderItem={renderProject}
          contentContainerStyle={{ padding: 16, paddingBottom: 32 }}
          onEndReached={loadMore}
          onEndReachedThreshold={0.3}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={ORANGE} />}
          ListFooterComponent={loadingMore ? <ActivityIndicator size="small" color={ORANGE} style={{ marginVertical: 16 }} /> : null}
        />
      )}

      {/* City picker modal */}
      <Modal visible={showCityPicker} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Select City</Text>
            <TouchableOpacity
              style={[styles.modalOption, !city ? styles.modalOptionActive : null]}
              onPress={() => { setCity(''); setShowCityPicker(false); }}
            >
              <Text style={[styles.modalOptionText, !city ? styles.modalOptionTextActive : null]}>All Cities</Text>
            </TouchableOpacity>
            <ScrollView style={{ maxHeight: 400 }}>
              {POPULAR_CITIES.map(c => (
                <TouchableOpacity
                  key={c.name}
                  style={[styles.modalOption, city === c.name ? styles.modalOptionActive : null]}
                  onPress={() => { setCity(c.name); setShowCityPicker(false); }}
                >
                  <Text style={[styles.modalOptionText, city === c.name ? styles.modalOptionTextActive : null]}>
                    {c.name}
                  </Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
            <TouchableOpacity style={styles.modalClose} onPress={() => setShowCityPicker(false)}>
              <Text style={styles.modalCloseText}>Cancel</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* Sort modal */}
      <Modal visible={showSort} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Sort By</Text>
            {SORT_OPTIONS.map(opt => (
              <TouchableOpacity
                key={opt.key}
                style={[styles.modalOption, sort === opt.key ? styles.modalOptionActive : null]}
                onPress={() => { setSort(opt.key); setShowSort(false); }}
              >
                <Text style={[styles.modalOptionText, sort === opt.key ? styles.modalOptionTextActive : null]}>
                  {opt.label}
                </Text>
              </TouchableOpacity>
            ))}
            <TouchableOpacity style={styles.modalClose} onPress={() => setShowSort(false)}>
              <Text style={styles.modalCloseText}>Cancel</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

function statusStyle(status: string) {
  switch (status) {
    case 'UPCOMING': return { backgroundColor: '#dbeafe' };
    case 'UNDER_CONSTRUCTION': return { backgroundColor: '#fef3c7' };
    case 'READY_TO_MOVE': return { backgroundColor: '#dcfce7' };
    default: return { backgroundColor: '#f3f4f6' };
  }
}

function statusTextStyle(status: string) {
  switch (status) {
    case 'UPCOMING': return { color: '#1d4ed8' };
    case 'UNDER_CONSTRUCTION': return { color: '#92400e' };
    case 'READY_TO_MOVE': return { color: '#15803d' };
    default: return { color: '#6b7280' };
  }
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  header: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: 16, paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: '#f3f4f6',
  },
  backBtn: { width: 40, height: 40, justifyContent: 'center', alignItems: 'center' },
  backText: { fontSize: 22, color: ORANGE, fontWeight: '600' },
  headerTitle: { fontSize: 18, fontWeight: '700', color: '#111827' },
  searchRow: { paddingHorizontal: 16, paddingTop: 12, paddingBottom: 4 },
  searchInput: {
    height: 44, borderRadius: 10, backgroundColor: '#f9fafb', borderWidth: 1, borderColor: '#e5e7eb',
    paddingHorizontal: 14, fontSize: 15, color: '#111827',
  },
  filterRow: {
    flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16,
    paddingVertical: 8, gap: 8,
  },
  filterChip: {
    paddingHorizontal: 14, paddingVertical: 7, borderRadius: 20,
    backgroundColor: '#f3f4f6', marginRight: 6,
  },
  filterChipActive: { backgroundColor: ORANGE },
  filterChipText: { fontSize: 13, color: '#374151', fontWeight: '500' },
  filterChipTextActive: { color: '#fff' },
  sortBtn: {
    paddingHorizontal: 12, paddingVertical: 7, borderRadius: 20,
    borderWidth: 1, borderColor: '#d1d5db',
  },
  sortBtnText: { fontSize: 13, color: '#374151', fontWeight: '500' },
  resultCount: { paddingHorizontal: 16, paddingBottom: 4, fontSize: 13, color: '#6b7280' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 32 },
  emptyTitle: { fontSize: 17, fontWeight: '600', color: '#374151', marginBottom: 6 },
  emptySubtitle: { fontSize: 14, color: '#9ca3af' },
  card: {
    borderRadius: 14, backgroundColor: '#fff', marginBottom: 16,
    shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.08, shadowRadius: 8,
    elevation: 3, overflow: 'hidden',
  },
  cardImage: { width: CARD_WIDTH, height: 200, backgroundColor: '#f3f4f6' },
  cardImagePlaceholder: { justifyContent: 'center', alignItems: 'center' },
  placeholderText: { color: '#9ca3af', fontSize: 14 },
  reraBadge: {
    position: 'absolute', top: 12, right: 12, backgroundColor: '#15803d',
    paddingHorizontal: 8, paddingVertical: 3, borderRadius: 4,
  },
  reraBadgeText: { color: '#fff', fontSize: 11, fontWeight: '700' },
  cardBody: { padding: 14 },
  builderName: { fontSize: 12, color: '#6b7280', fontWeight: '500', marginBottom: 2 },
  projectName: { fontSize: 17, fontWeight: '700', color: '#111827', marginBottom: 4 },
  location: { fontSize: 13, color: '#6b7280', marginBottom: 6 },
  bhkText: { fontSize: 14, color: '#374151', fontWeight: '500', marginBottom: 4 },
  priceText: { fontSize: 16, fontWeight: '700', color: ORANGE, marginBottom: 4 },
  possessionText: { fontSize: 13, color: '#6b7280', marginBottom: 8 },
  progressRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 8 },
  progressBarBg: { flex: 1, height: 6, borderRadius: 3, backgroundColor: '#e5e7eb' },
  progressBarFill: { height: 6, borderRadius: 3, backgroundColor: ORANGE },
  progressText: { fontSize: 12, color: '#6b7280', marginLeft: 8, fontWeight: '600', width: 36 },
  statusChip: {
    alignSelf: 'flex-start', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 12,
  },
  statusChipText: { fontSize: 12, fontWeight: '600' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  modalContent: {
    backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20,
    paddingHorizontal: 20, paddingTop: 20, paddingBottom: 32, maxHeight: '70%',
  },
  modalTitle: { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 16 },
  modalOption: { paddingVertical: 12, paddingHorizontal: 12, borderRadius: 10, marginBottom: 2 },
  modalOptionActive: { backgroundColor: '#fff7ed' },
  modalOptionText: { fontSize: 15, color: '#374151' },
  modalOptionTextActive: { color: ORANGE, fontWeight: '600' },
  modalClose: { marginTop: 12, alignItems: 'center', paddingVertical: 12 },
  modalCloseText: { fontSize: 15, color: '#6b7280', fontWeight: '500' },
});
