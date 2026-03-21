import { useState, useCallback, useEffect, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  ScrollView,
  Image,
  Modal,
  Animated,
} from 'react-native';
import { useRouter } from 'expo-router';
import Constants from 'expo-constants';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import { useTranslation } from '@/lib/i18n';

const API_URL = Constants.expoConfig?.extra?.apiUrl ?? 'http://localhost:8080';

interface Listing {
  id: string;
  title: string;
  city: string;
  state: string;
  basePricePaise: number;
  avgRating?: number;
  status: string;
  instantBook?: boolean;
  primaryPhotoUrl?: string;
}

interface ExperienceItem {
  id: string;
  title: string;
  category: string;
  city: string;
  pricePaise: number;
  durationMinutes: number;
  avgRating?: number;
  hostName: string;
}

const EXPERIENCE_ICONS: Record<string, string> = {
  CULINARY: '🍳', CULTURAL: '🏛️', WELLNESS: '🧘', ADVENTURE: '🏔️', CREATIVE: '🎨',
};

const TYPE_LABELS: Record<string, string> = {
  HOME: 'Home', ROOM: 'Room', UNIQUE: 'Unique', COMMERCIAL: 'Commercial',
  PG: 'PG', HOTEL: 'Hotel', COLIVING: 'Co-living', HOSTEL_DORM: 'Hostel',
};

const PROPERTY_TYPES = ['HOME', 'ROOM', 'UNIQUE', 'COMMERCIAL', 'PG', 'HOTEL', 'COLIVING', 'HOSTEL_DORM'];

const SORT_OPTIONS = [
  { value: '', label: 'Relevance' },
  { value: 'price_asc', label: 'Price: Low → High' },
  { value: 'price_desc', label: 'Price: High → Low' },
  { value: 'rating_desc', label: 'Top Rated' },
  { value: 'newest', label: 'Newest' },
];

export default function ExploreScreen() {
  const router = useRouter();
  const { t } = useTranslation();
  const [query, setQuery] = useState('');
  const [listings, setListings] = useState<Listing[]>([]);
  const [totalHits, setTotalHits] = useState(0);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  // Experiences near you
  const [experiences, setExperiences] = useState<ExperienceItem[]>([]);
  const [experiencesLoading, setExperiencesLoading] = useState(true);

  // Filters (applied after search)
  const [filterType, setFilterType] = useState('');
  const [sortBy, setSortBy] = useState('');
  const [instantBook, setInstantBook] = useState(false);
  const [minRating, setMinRating] = useState(false); // 4+ star filter
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');

  // Bottom sheet modals
  const [showPriceSheet, setShowPriceSheet] = useState(false);
  const [showSortSheet, setShowSortSheet] = useState(false);

  // Temp price values for bottom sheet
  const [tempMinPrice, setTempMinPrice] = useState('');
  const [tempMaxPrice, setTempMaxPrice] = useState('');

  // Track last searched city for result count header
  const [searchedCity, setSearchedCity] = useState('');

  // Autocomplete
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const autocompleteTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Autocomplete fetch
  function onQueryChange(text: string) {
    setQuery(text);
    if (autocompleteTimer.current) clearTimeout(autocompleteTimer.current);
    if (text.trim().length < 2) { setSuggestions([]); setShowSuggestions(false); return; }
    autocompleteTimer.current = setTimeout(async () => {
      try {
        const results = await api.autocomplete(text.trim());
        setSuggestions(results.slice(0, 6));
        setShowSuggestions(results.length > 0);
      } catch { setSuggestions([]); }
    }, 300);
  }

  function selectSuggestion(city: string) {
    setQuery(city);
    setSuggestions([]);
    setShowSuggestions(false);
    // Auto-search on selection
    setTimeout(() => doSearch(), 100);
  }

  const POPULAR_CITIES = [
    { name: 'Goa', icon: '🏖️' },
    { name: 'Mumbai', icon: '🏙️' },
    { name: 'Delhi', icon: '🏛️' },
    { name: 'Jaipur', icon: '🏰' },
    { name: 'Bangalore', icon: '🌆' },
    { name: 'Manali', icon: '🏔️' },
  ];

  // Count active filters
  const activeFilterCount = [
    filterType !== '',
    sortBy !== '',
    instantBook,
    minRating,
    minPrice !== '' || maxPrice !== '',
  ].filter(Boolean).length;

  useEffect(() => {
    (async () => {
      try {
        const result = await api.getExperiences();
        setExperiences((result ?? []).slice(0, 10));
      } catch {
        setExperiences([]);
      } finally {
        setExperiencesLoading(false);
      }
    })();
  }, []);

  const doSearch = useCallback(async (overrides?: {
    type?: string; sort?: string; instant?: boolean; rating?: boolean; pMin?: string; pMax?: string;
  }) => {
    if (!query.trim()) return;
    setLoading(true);
    const fType = overrides?.type ?? filterType;
    const fSort = overrides?.sort ?? sortBy;
    const fInstant = overrides?.instant ?? instantBook;
    const fRating = overrides?.rating ?? minRating;
    const fMinPrice = overrides?.pMin ?? minPrice;
    const fMaxPrice = overrides?.pMax ?? maxPrice;

    try {
      const params: Record<string, string> = { city: query.trim(), size: '20' };
      if (fType) params.type = fType;
      if (fSort) params.sort = fSort;
      if (fInstant) params.instantBook = 'true';
      if (fRating) params.minRating = '4.0';
      if (fMinPrice) params.priceMin = fMinPrice;
      if (fMaxPrice) params.priceMax = fMaxPrice;
      const result = await api.search(params);
      setListings(result.listings ?? result.content ?? []);
      setTotalHits(result.totalHits ?? result.listings?.length ?? 0);
      setSearchedCity(query.trim());
    } catch {
      setListings([]);
      setTotalHits(0);
    } finally {
      setLoading(false);
      setSearched(true);
    }
  }, [query, filterType, sortBy, instantBook, minRating, minPrice, maxPrice]);

  // Re-search when filters change (only if already searched)
  function toggleType(type: string) {
    const newType = filterType === type ? '' : type;
    setFilterType(newType);
    if (searched) doSearch({ type: newType });
  }

  function toggleSort(value: string) {
    const newSort = sortBy === value ? '' : value;
    setSortBy(newSort);
    setShowSortSheet(false);
    if (searched) doSearch({ sort: newSort });
  }

  function toggleInstantBook() {
    const newVal = !instantBook;
    setInstantBook(newVal);
    if (searched) doSearch({ instant: newVal });
  }

  function toggleMinRating() {
    const newVal = !minRating;
    setMinRating(newVal);
    if (searched) doSearch({ rating: newVal });
  }

  function applyPrice() {
    setMinPrice(tempMinPrice);
    setMaxPrice(tempMaxPrice);
    setShowPriceSheet(false);
    if (searched) doSearch({ pMin: tempMinPrice, pMax: tempMaxPrice });
  }

  function clearAllFilters() {
    setFilterType('');
    setSortBy('');
    setInstantBook(false);
    setMinRating(false);
    setMinPrice('');
    setMaxPrice('');
    setTempMinPrice('');
    setTempMaxPrice('');
    if (searched) doSearch({ type: '', sort: '', instant: false, rating: false, pMin: '', pMax: '' });
  }

  function renderItem({ item }: { item: Listing }) {
    return (
      <TouchableOpacity
        style={styles.card}
        onPress={() => router.push(`/listing/${item.id}`)}
        activeOpacity={0.85}
      >
        {item.primaryPhotoUrl ? (
          <Image
            source={{ uri: API_URL + item.primaryPhotoUrl }}
            style={styles.cardImage}
            resizeMode="cover"
          />
        ) : (
          <View style={styles.cardPlaceholder}>
            <Text style={styles.cardPlaceholderIcon}>🏠</Text>
          </View>
        )}
        <View style={styles.cardInfo}>
          <View style={styles.cardRow}>
            <Text style={styles.cardTitle} numberOfLines={1}>{item.title}</Text>
            {item.avgRating != null && item.avgRating > 0 && (
              <Text style={styles.rating}>★ {item.avgRating.toFixed(1)}</Text>
            )}
          </View>
          <Text style={styles.cardCity}>{item.city}, {item.state}</Text>
          <Text style={styles.cardPrice}>
            {formatPaise(item.basePricePaise)} / {t('listing.perNight')}
          </Text>
          <View style={styles.badges}>
            {item.status === 'VERIFIED' && (
              <Text style={[styles.badge, styles.badgeVerified]}>✓ {t('listing.verified')}</Text>
            )}
            {item.instantBook && (
              <Text style={[styles.badge, styles.badgeInstant]}>⚡ {t('listing.instant')}</Text>
            )}
          </View>
        </View>
      </TouchableOpacity>
    );
  }

  return (
    <View style={styles.container}>
      {/* Search bar */}
      <View style={styles.searchRow}>
        <View style={{ flex: 1, position: 'relative' }}>
          <TextInput
            style={styles.input}
            placeholder={t('search.placeholder')}
            placeholderTextColor="#9ca3af"
            value={query}
            onChangeText={onQueryChange}
            onSubmitEditing={() => { setShowSuggestions(false); doSearch(); }}
            onFocus={() => { if (query.length < 2) setShowSuggestions(true); }}
            returnKeyType="search"
          />
          {/* Autocomplete dropdown */}
          {showSuggestions && (
            <View style={styles.suggestionsBox}>
              {suggestions.length > 0 ? (
                <>
                  <Text style={styles.suggestionsLabel}>DESTINATIONS</Text>
                  {suggestions.map((city) => (
                    <TouchableOpacity key={city} style={styles.suggestionItem}
                      onPress={() => selectSuggestion(city)}>
                      <Text style={styles.suggestionIcon}>📍</Text>
                      <Text style={styles.suggestionText}>{city}</Text>
                    </TouchableOpacity>
                  ))}
                </>
              ) : query.length < 2 ? (
                <>
                  <Text style={styles.suggestionsLabel}>POPULAR DESTINATIONS</Text>
                  {POPULAR_CITIES.map((c) => (
                    <TouchableOpacity key={c.name} style={styles.suggestionItem}
                      onPress={() => selectSuggestion(c.name)}>
                      <Text style={styles.suggestionIcon}>{c.icon}</Text>
                      <Text style={styles.suggestionText}>{c.name}</Text>
                    </TouchableOpacity>
                  ))}
                </>
              ) : null}
            </View>
          )}
        </View>
        <TouchableOpacity style={styles.searchBtn} onPress={() => { setShowSuggestions(false); doSearch(); }}>
          <Text style={styles.searchBtnText}>{t('search.button')}</Text>
        </TouchableOpacity>
      </View>

      {/* Filter chip bar — only shown after first search */}
      {searched && (
        <View style={styles.filterBar}>
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.filterChipsScroll}
          >
            {/* Sort chip */}
            <TouchableOpacity
              style={[styles.filterChip, sortBy !== '' && styles.filterChipActive]}
              onPress={() => setShowSortSheet(true)}
            >
              <Text style={[styles.filterChipText, sortBy !== '' && styles.filterChipTextActive]}>
                {sortBy ? SORT_OPTIONS.find(o => o.value === sortBy)?.label : 'Sort'}
              </Text>
              <Text style={[styles.chipArrow, sortBy !== '' && styles.filterChipTextActive]}>▾</Text>
            </TouchableOpacity>

            {/* Type chips */}
            {PROPERTY_TYPES.map((type) => (
              <TouchableOpacity
                key={type}
                style={[styles.filterChip, filterType === type && styles.filterChipActive]}
                onPress={() => toggleType(type)}
              >
                <Text style={[styles.filterChipText, filterType === type && styles.filterChipTextActive]}>
                  {TYPE_LABELS[type] ?? type.charAt(0) + type.slice(1).toLowerCase()}
                </Text>
              </TouchableOpacity>
            ))}

            {/* Price chip */}
            <TouchableOpacity
              style={[styles.filterChip, (minPrice || maxPrice) ? styles.filterChipActive : null]}
              onPress={() => {
                setTempMinPrice(minPrice);
                setTempMaxPrice(maxPrice);
                setShowPriceSheet(true);
              }}
            >
              <Text style={[styles.filterChipText, (minPrice || maxPrice) ? styles.filterChipTextActive : null]}>
                {minPrice || maxPrice
                  ? `₹${minPrice || '0'} – ₹${maxPrice || '∞'}`
                  : 'Price'}
              </Text>
              <Text style={[styles.chipArrow, (minPrice || maxPrice) ? styles.filterChipTextActive : null]}>▾</Text>
            </TouchableOpacity>

            {/* Instant Book chip */}
            <TouchableOpacity
              style={[styles.filterChip, instantBook && styles.filterChipActive]}
              onPress={toggleInstantBook}
            >
              <Text style={[styles.filterChipText, instantBook && styles.filterChipTextActive]}>
                ⚡ Instant
              </Text>
            </TouchableOpacity>

            {/* Rating 4+ chip */}
            <TouchableOpacity
              style={[styles.filterChip, minRating && styles.filterChipActive]}
              onPress={toggleMinRating}
            >
              <Text style={[styles.filterChipText, minRating && styles.filterChipTextActive]}>
                ★ 4+
              </Text>
            </TouchableOpacity>

            {/* Clear all */}
            {activeFilterCount > 0 && (
              <TouchableOpacity style={styles.clearChip} onPress={clearAllFilters}>
                <Text style={styles.clearChipText}>Clear all</Text>
              </TouchableOpacity>
            )}
          </ScrollView>
        </View>
      )}

      {/* Result count header */}
      {searched && !loading && (
        <View style={styles.resultCountBar}>
          <Text style={styles.resultCountText}>
            {totalHits} {totalHits === 1 ? 'stay' : 'stays'} in {searchedCity}
          </Text>
          {activeFilterCount > 0 && (
            <Text style={styles.filterCountBadge}>{activeFilterCount} filter{activeFilterCount > 1 ? 's' : ''}</Text>
          )}
        </View>
      )}

      {loading ? (
        <ActivityIndicator color="#f97316" style={{ marginTop: 40 }} size="large" />
      ) : (
        <FlatList
          data={listings}
          keyExtractor={(item) => item.id}
          renderItem={renderItem}
          contentContainerStyle={listings.length === 0 ? styles.emptyContainer : styles.list}
          ListEmptyComponent={
            searched ? (
              <View style={styles.empty}>
                <Text style={styles.emptyIcon}>🔍</Text>
                <Text style={styles.emptyTitle}>{t('search.noResults')}</Text>
                <Text style={styles.emptySubtitle}>{t('search.tryDifferent')}</Text>
              </View>
            ) : (
              <View style={styles.empty}>
                <Text style={styles.emptyIcon}>🧳</Text>
                <Text style={styles.emptyTitle}>{t('search.findStay')}</Text>
                <Text style={styles.emptySubtitle}>{t('search.searchAbove')}</Text>
              </View>
            )
          }
          ListFooterComponent={
            <View style={styles.experiencesSection}>
              <View style={styles.experiencesHeader}>
                <Text style={styles.experiencesTitle}>Experiences near you</Text>
                <TouchableOpacity onPress={() => router.push('/experiences')}>
                  <Text style={styles.experiencesSeeAll}>See all</Text>
                </TouchableOpacity>
              </View>
              {experiencesLoading ? (
                <ActivityIndicator color="#f97316" style={{ marginVertical: 20 }} />
              ) : experiences.length > 0 ? (
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 12 }}>
                  {experiences.map((exp) => (
                    <TouchableOpacity
                      key={exp.id}
                      style={styles.expCard}
                      onPress={() => router.push('/experiences')}
                      activeOpacity={0.85}
                    >
                      <View style={styles.expCardIcon}>
                        <Text style={{ fontSize: 28 }}>{EXPERIENCE_ICONS[exp.category] ?? '🎯'}</Text>
                      </View>
                      <Text style={styles.expCardCategory}>{exp.category}</Text>
                      <Text style={styles.expCardTitle} numberOfLines={2}>{exp.title}</Text>
                      <Text style={styles.expCardCity}>{exp.city}</Text>
                      <Text style={styles.expCardPrice}>{formatPaise(exp.pricePaise)}</Text>
                    </TouchableOpacity>
                  ))}
                </ScrollView>
              ) : (
                <Text style={styles.expEmptyText}>No experiences available yet.</Text>
              )}
            </View>
          }
        />
      )}

      {/* Sort Bottom Sheet */}
      <Modal visible={showSortSheet} transparent animationType="slide" onRequestClose={() => setShowSortSheet(false)}>
        <TouchableOpacity style={styles.sheetOverlay} activeOpacity={1} onPress={() => setShowSortSheet(false)}>
          <View style={styles.sheetContent}>
            <View style={styles.sheetHandle} />
            <Text style={styles.sheetTitle}>Sort by</Text>
            {SORT_OPTIONS.map((opt) => (
              <TouchableOpacity
                key={opt.value}
                style={[styles.sheetOption, sortBy === opt.value && styles.sheetOptionActive]}
                onPress={() => toggleSort(opt.value)}
              >
                <Text style={[styles.sheetOptionText, sortBy === opt.value && styles.sheetOptionTextActive]}>
                  {opt.label}
                </Text>
                {sortBy === opt.value && <Text style={styles.sheetCheck}>✓</Text>}
              </TouchableOpacity>
            ))}
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Price Bottom Sheet */}
      <Modal visible={showPriceSheet} transparent animationType="slide" onRequestClose={() => setShowPriceSheet(false)}>
        <TouchableOpacity style={styles.sheetOverlay} activeOpacity={1} onPress={() => setShowPriceSheet(false)}>
          <View style={styles.sheetContent} onStartShouldSetResponder={() => true}>
            <View style={styles.sheetHandle} />
            <Text style={styles.sheetTitle}>Price range (per night)</Text>
            <View style={styles.priceInputRow}>
              <View style={{ flex: 1 }}>
                <Text style={styles.priceInputLabel}>Min price</Text>
                <TextInput
                  style={styles.priceInput}
                  placeholder="₹ 0"
                  placeholderTextColor="#9ca3af"
                  value={tempMinPrice}
                  onChangeText={setTempMinPrice}
                  keyboardType="numeric"
                />
              </View>
              <Text style={styles.priceSep}>—</Text>
              <View style={{ flex: 1 }}>
                <Text style={styles.priceInputLabel}>Max price</Text>
                <TextInput
                  style={styles.priceInput}
                  placeholder="₹ No limit"
                  placeholderTextColor="#9ca3af"
                  value={tempMaxPrice}
                  onChangeText={setTempMaxPrice}
                  keyboardType="numeric"
                />
              </View>
            </View>
            <Text style={styles.priceHint}>Enter amount in paise (e.g. 100000 = ₹1,000)</Text>
            <View style={styles.sheetActions}>
              <TouchableOpacity
                style={styles.sheetClearBtn}
                onPress={() => { setTempMinPrice(''); setTempMaxPrice(''); }}
              >
                <Text style={styles.sheetClearBtnText}>Clear</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.sheetApplyBtn} onPress={applyPrice}>
                <Text style={styles.sheetApplyBtnText}>Apply</Text>
              </TouchableOpacity>
            </View>
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container:          { flex: 1, backgroundColor: '#f9fafb' },
  searchRow:          { flexDirection: 'row', gap: 8, padding: 16, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  input:              { flex: 1, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, paddingHorizontal: 12, paddingVertical: 10, fontSize: 14, backgroundColor: '#fff' },
  searchBtn:          { backgroundColor: '#f97316', borderRadius: 12, paddingHorizontal: 16, justifyContent: 'center' },
  searchBtnText:      { color: '#fff', fontWeight: '600', fontSize: 14 },

  // Autocomplete suggestions
  suggestionsBox:     { position: 'absolute', top: 48, left: 0, right: 0, backgroundColor: '#fff', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 12, zIndex: 100, elevation: 10, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.15, shadowRadius: 8, maxHeight: 240, overflow: 'hidden' },
  suggestionsLabel:   { fontSize: 10, fontWeight: '700', color: '#9ca3af', letterSpacing: 0.5, paddingHorizontal: 12, paddingTop: 10, paddingBottom: 4 },
  suggestionItem:     { flexDirection: 'row', alignItems: 'center', gap: 10, paddingHorizontal: 12, paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  suggestionIcon:     { fontSize: 18, width: 28, textAlign: 'center' },
  suggestionText:     { fontSize: 14, fontWeight: '500', color: '#111827' },

  // Filter chip bar
  filterBar:          { backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  filterChipsScroll:  { paddingHorizontal: 12, paddingVertical: 10, gap: 8, flexDirection: 'row' },
  filterChip:         { flexDirection: 'row', alignItems: 'center', borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 100, paddingHorizontal: 14, paddingVertical: 7, backgroundColor: '#fff' },
  filterChipActive:   { borderColor: '#f97316', backgroundColor: '#fff7ed' },
  filterChipText:     { fontSize: 12, fontWeight: '600', color: '#6b7280' },
  filterChipTextActive: { color: '#f97316' },
  chipArrow:          { fontSize: 10, color: '#9ca3af', marginLeft: 4 },
  clearChip:          { borderWidth: 1, borderColor: '#fca5a5', borderRadius: 100, paddingHorizontal: 12, paddingVertical: 7, backgroundColor: '#fef2f2' },
  clearChipText:      { fontSize: 12, fontWeight: '600', color: '#dc2626' },

  // Result count bar
  resultCountBar:     { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 8, backgroundColor: '#f9fafb' },
  resultCountText:    { fontSize: 13, fontWeight: '600', color: '#374151' },
  filterCountBadge:   { fontSize: 11, fontWeight: '600', color: '#f97316', backgroundColor: '#fff7ed', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 100, overflow: 'hidden' },

  // Cards
  cardImage:          { width: '100%', height: 140 },
  list:               { padding: 16, gap: 12 },
  emptyContainer:     { flex: 1 },
  card:               { backgroundColor: '#fff', borderRadius: 16, overflow: 'hidden', borderWidth: 1, borderColor: '#f3f4f6', marginBottom: 12 },
  cardPlaceholder:    { height: 140, backgroundColor: '#f3f4f6', alignItems: 'center', justifyContent: 'center' },
  cardPlaceholderIcon:{ fontSize: 40 },
  cardInfo:           { padding: 12 },
  cardRow:            { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  cardTitle:          { fontSize: 14, fontWeight: '600', color: '#111827', flex: 1 },
  rating:             { fontSize: 13, fontWeight: '600', color: '#374151', marginLeft: 8 },
  cardCity:           { fontSize: 12, color: '#6b7280', marginTop: 2 },
  cardPrice:          { fontSize: 14, fontWeight: '700', color: '#111827', marginTop: 4 },
  badges:             { flexDirection: 'row', gap: 6, marginTop: 6 },
  badge:              { fontSize: 11, fontWeight: '600', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 100 },
  badgeVerified:      { backgroundColor: '#dcfce7', color: '#15803d' },
  badgeInstant:       { backgroundColor: '#dbeafe', color: '#1d4ed8' },

  // Empty state
  empty:              { alignItems: 'center', paddingTop: 80 },
  emptyIcon:          { fontSize: 48, marginBottom: 12 },
  emptyTitle:         { fontSize: 18, fontWeight: '600', color: '#374151' },
  emptySubtitle:      { fontSize: 14, color: '#9ca3af', marginTop: 4 },

  // Bottom sheet
  sheetOverlay:       { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  sheetContent:       { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 20, paddingBottom: 32 },
  sheetHandle:        { width: 36, height: 4, borderRadius: 2, backgroundColor: '#e5e7eb', alignSelf: 'center', marginBottom: 16 },
  sheetTitle:         { fontSize: 17, fontWeight: '700', color: '#111827', marginBottom: 16 },
  sheetOption:        { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  sheetOptionActive:  { backgroundColor: '#fff7ed' },
  sheetOptionText:    { fontSize: 15, color: '#374151' },
  sheetOptionTextActive: { color: '#f97316', fontWeight: '600' },
  sheetCheck:         { fontSize: 16, color: '#f97316', fontWeight: '700' },

  // Price bottom sheet
  priceInputRow:      { flexDirection: 'row', alignItems: 'flex-end', gap: 12, marginBottom: 8 },
  priceInputLabel:    { fontSize: 12, fontWeight: '600', color: '#6b7280', marginBottom: 4 },
  priceInput:         { borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 12, paddingVertical: 10, fontSize: 14, color: '#111827', backgroundColor: '#f9fafb' },
  priceSep:           { fontSize: 14, color: '#9ca3af', paddingBottom: 12 },
  priceHint:          { fontSize: 11, color: '#9ca3af', marginBottom: 16 },
  sheetActions:       { flexDirection: 'row', gap: 10 },
  sheetClearBtn:      { flex: 1, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingVertical: 12, alignItems: 'center' },
  sheetClearBtnText:  { fontSize: 14, fontWeight: '600', color: '#6b7280' },
  sheetApplyBtn:      { flex: 1, backgroundColor: '#f97316', borderRadius: 10, paddingVertical: 12, alignItems: 'center' },
  sheetApplyBtnText:  { color: '#fff', fontWeight: '700', fontSize: 14 },

  // Experiences near you
  experiencesSection: { paddingTop: 24, paddingBottom: 16, paddingHorizontal: 16 },
  experiencesHeader:  { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
  experiencesTitle:   { fontSize: 17, fontWeight: '700', color: '#111827' },
  experiencesSeeAll:  { fontSize: 13, fontWeight: '600', color: '#f97316' },
  expCard:            { width: 160, backgroundColor: '#fff', borderRadius: 14, overflow: 'hidden', borderWidth: 1, borderColor: '#f3f4f6' },
  expCardIcon:        { height: 70, backgroundColor: '#fff7ed', alignItems: 'center', justifyContent: 'center' },
  expCardCategory:    { fontSize: 9, fontWeight: '700', color: '#c2410c', backgroundColor: '#fff7ed', alignSelf: 'flex-start', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 100, marginLeft: 8, marginTop: 8 },
  expCardTitle:       { fontSize: 13, fontWeight: '600', color: '#111827', paddingHorizontal: 8, marginTop: 4 },
  expCardCity:        { fontSize: 11, color: '#6b7280', paddingHorizontal: 8, marginTop: 2 },
  expCardPrice:       { fontSize: 13, fontWeight: '800', color: '#f97316', paddingHorizontal: 8, paddingBottom: 10, marginTop: 4 },
  expEmptyText:       { fontSize: 13, color: '#9ca3af', textAlign: 'center', paddingVertical: 16 },
});
