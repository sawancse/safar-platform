import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet, ActivityIndicator, ScrollView, Image,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';

const CHEF_TYPES = [
  { code: 'DOMESTIC',         label: '🏠 Home Cooks',     desc: 'Daily meals at home' },
  { code: 'PROFESSIONAL',     label: '👨‍🍳 Pro Chefs',      desc: 'Restaurant-grade chefs' },
  { code: 'EVENT_SPECIALIST', label: '🎉 Event Caterers', desc: 'Parties & big events' },
] as const;

const CUISINES = [
  'SOUTH_INDIAN', 'NORTH_INDIAN', 'BENGALI', 'PUNJABI', 'HYDERABADI', 'CHINESE',
  'CONTINENTAL', 'MUGHLAI', 'ITALIAN', 'RAJASTHANI', 'STREET_FOOD', 'JAIN',
];

const CITIES = ['Hyderabad', 'Bangalore', 'Mumbai', 'Delhi', 'Chennai', 'Pune', 'Kolkata', 'Goa'];

const OCCASIONS = [
  { code: 'BIRTHDAY', label: '🎂 Birthday' },
  { code: 'WEDDING', label: '💍 Wedding' },
  { code: 'HOUSEWARMING', label: '🏡 Housewarming' },
  { code: 'CORPORATE', label: '💼 Corporate' },
  { code: 'ANNIVERSARY', label: '❤️ Anniversary' },
  { code: 'POOJA', label: '🪔 Pooja' },
];

interface Chef {
  id: string;
  name: string;
  profilePhotoUrl?: string;
  rating?: number;
  reviewCount?: number;
  city?: string;
  state?: string;
  verified?: boolean;
  chefType?: string;
  cuisines?: string;
  experienceYears?: number;
  totalBookings?: number;
  dailyRatePaise?: number;
}

function pretty(s?: string) {
  if (!s) return '';
  return s.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
}

export default function CooksScreen() {
  const router = useRouter();
  const [chefs, setChefs] = useState<Chef[]>([]);
  const [loading, setLoading] = useState(true);
  const [city, setCity] = useState('');
  const [cuisine, setCuisine] = useState('');
  const [chefType, setChefType] = useState('');

  const fetchChefs = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string> = { size: '40' };
      if (city) params.city = city;
      if (cuisine) params.cuisine = cuisine;
      if (chefType) params.chefType = chefType;
      const result = await api.searchChefs(params);
      setChefs(result?.chefs ?? result?.content ?? (Array.isArray(result) ? result : []));
    } catch {
      setChefs([]);
    } finally {
      setLoading(false);
    }
  }, [city, cuisine, chefType]);

  useEffect(() => { fetchChefs(); }, [fetchChefs]);

  function renderChef({ item }: { item: Chef }) {
    const cuisineList = (item.cuisines ?? '').split(',').map((c) => c.trim()).filter(Boolean);
    return (
      <TouchableOpacity onPress={() => router.push(`/cook/${item.id}`)} activeOpacity={0.7} style={styles.card}>
        <View style={styles.cardHeader}>
          {item.profilePhotoUrl ? (
            <Image source={{ uri: item.profilePhotoUrl }} style={styles.avatar} />
          ) : (
            <View style={[styles.avatar, styles.avatarFallback]}><Text style={styles.avatarIcon}>👨‍🍳</Text></View>
          )}
          <View style={{ flex: 1 }}>
            <View style={styles.nameRow}>
              <Text style={styles.cardName} numberOfLines={1}>{item.name}</Text>
              {item.verified && <Text style={styles.verifiedBadge}>✓</Text>}
            </View>
            <Text style={styles.cardCity}>{item.city}{item.state ? `, ${item.state}` : ''}</Text>
            {item.rating != null && item.rating > 0 && (
              <Text style={styles.rating}>★ {item.rating.toFixed(1)} {item.reviewCount ? `(${item.reviewCount})` : ''}</Text>
            )}
          </View>
        </View>
        {cuisineList.length > 0 && (
          <View style={styles.cuisineRow}>
            {cuisineList.slice(0, 3).map((c) => (
              <View key={c} style={styles.cuisineChip}><Text style={styles.cuisineChipText}>{pretty(c)}</Text></View>
            ))}
            {cuisineList.length > 3 && <Text style={styles.cuisineMore}>+{cuisineList.length - 3}</Text>}
          </View>
        )}
        <View style={styles.cardFooter}>
          <Text style={styles.cardMeta}>
            {item.experienceYears ? `${item.experienceYears} yrs exp` : ''}
            {item.totalBookings ? ` · ${item.totalBookings} bookings` : ''}
          </Text>
          {item.dailyRatePaise ? <Text style={styles.cardPrice}>{formatPaise(item.dailyRatePaise)}/day</Text> : null}
        </View>
      </TouchableOpacity>
    );
  }

  return (
    <View style={styles.container}>
      <FlatList
        data={chefs}
        keyExtractor={(item) => item.id}
        renderItem={renderChef}
        contentContainerStyle={chefs.length === 0 ? styles.emptyContainer : styles.list}
        ListHeaderComponent={
          <View>
            {/* Service type cards */}
            <View style={styles.typeRow}>
              {CHEF_TYPES.map((t) => (
                <TouchableOpacity
                  key={t.code}
                  style={[styles.typeCard, chefType === t.code && styles.typeCardActive]}
                  onPress={() => setChefType(chefType === t.code ? '' : t.code)}
                >
                  <Text style={styles.typeLabel}>{t.label}</Text>
                  <Text style={styles.typeDesc}>{t.desc}</Text>
                </TouchableOpacity>
              ))}
            </View>

            {/* Occasions */}
            <Text style={styles.sectionTitle}>Plan an event</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipScroll}>
              {OCCASIONS.map((o) => (
                <TouchableOpacity
                  key={o.code}
                  style={styles.occasionChip}
                  onPress={() => router.push(`/cook-book?type=EVENT&eventType=${o.code}`)}
                >
                  <Text style={styles.occasionText}>{o.label}</Text>
                </TouchableOpacity>
              ))}
            </ScrollView>

            {/* My bookings link */}
            <TouchableOpacity style={styles.myBookingsBtn} onPress={() => router.push('/cook-bookings')}>
              <Text style={styles.myBookingsText}>📋  My cook bookings</Text>
            </TouchableOpacity>

            {/* City filter */}
            <Text style={styles.sectionTitle}>City</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipScroll}>
              <TouchableOpacity style={[styles.filterChip, !city && styles.filterChipActive]} onPress={() => setCity('')}>
                <Text style={[styles.filterChipText, !city && styles.filterChipTextActive]}>All</Text>
              </TouchableOpacity>
              {CITIES.map((c) => (
                <TouchableOpacity key={c} style={[styles.filterChip, city === c && styles.filterChipActive]} onPress={() => setCity(city === c ? '' : c)}>
                  <Text style={[styles.filterChipText, city === c && styles.filterChipTextActive]}>{c}</Text>
                </TouchableOpacity>
              ))}
            </ScrollView>

            {/* Cuisine filter */}
            <Text style={styles.sectionTitle}>Cuisine</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipScroll}>
              <TouchableOpacity style={[styles.filterChip, !cuisine && styles.filterChipActive]} onPress={() => setCuisine('')}>
                <Text style={[styles.filterChipText, !cuisine && styles.filterChipTextActive]}>All</Text>
              </TouchableOpacity>
              {CUISINES.map((c) => (
                <TouchableOpacity key={c} style={[styles.filterChip, cuisine === c && styles.filterChipActive]} onPress={() => setCuisine(cuisine === c ? '' : c)}>
                  <Text style={[styles.filterChipText, cuisine === c && styles.filterChipTextActive]}>{pretty(c)}</Text>
                </TouchableOpacity>
              ))}
            </ScrollView>

            <Text style={[styles.sectionTitle, { marginBottom: 4 }]}>
              {loading ? 'Finding cooks…' : `${chefs.length} cooks available`}
            </Text>
          </View>
        }
        ListEmptyComponent={
          loading ? (
            <ActivityIndicator color="#f97316" style={{ marginTop: 40 }} size="large" />
          ) : (
            <View style={styles.empty}>
              <Text style={styles.emptyIcon}>👨‍🍳</Text>
              <Text style={styles.emptyTitle}>No cooks found</Text>
              <Text style={styles.emptySubtitle}>Try a different city or cuisine</Text>
            </View>
          )
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container:            { flex: 1, backgroundColor: '#f9fafb' },
  list:                 { padding: 16, gap: 12 },
  emptyContainer:       { padding: 16 },

  typeRow:              { flexDirection: 'row', gap: 8, marginBottom: 4 },
  typeCard:             { flex: 1, backgroundColor: '#fff', borderRadius: 12, padding: 10, borderWidth: 1, borderColor: '#f3f4f6' },
  typeCardActive:       { borderColor: '#f97316', backgroundColor: '#fff7ed' },
  typeLabel:            { fontSize: 12, fontWeight: '700', color: '#111827' },
  typeDesc:             { fontSize: 10, color: '#6b7280', marginTop: 2 },

  sectionTitle:         { fontSize: 14, fontWeight: '700', color: '#111827', marginTop: 16, marginBottom: 8 },
  chipScroll:           { gap: 8, paddingRight: 16 },
  occasionChip:         { backgroundColor: '#fff', borderRadius: 100, paddingHorizontal: 14, paddingVertical: 10, borderWidth: 1, borderColor: '#e5e7eb' },
  occasionText:         { fontSize: 13, fontWeight: '600', color: '#374151' },

  myBookingsBtn:        { backgroundColor: '#fff7ed', borderRadius: 12, padding: 14, marginTop: 12, borderWidth: 1, borderColor: '#fed7aa' },
  myBookingsText:       { fontSize: 14, fontWeight: '700', color: '#c2410c' },

  filterChip:           { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb', backgroundColor: '#fff' },
  filterChipActive:     { backgroundColor: '#f97316', borderColor: '#f97316' },
  filterChipText:       { fontSize: 12, fontWeight: '600', color: '#374151' },
  filterChipTextActive: { color: '#fff' },

  card:                 { backgroundColor: '#fff', borderRadius: 16, padding: 14, borderWidth: 1, borderColor: '#f3f4f6' },
  cardHeader:           { flexDirection: 'row', gap: 12, alignItems: 'center' },
  avatar:               { width: 56, height: 56, borderRadius: 28 },
  avatarFallback:       { backgroundColor: '#fff7ed', alignItems: 'center', justifyContent: 'center' },
  avatarIcon:           { fontSize: 26 },
  nameRow:              { flexDirection: 'row', alignItems: 'center', gap: 6 },
  cardName:             { fontSize: 16, fontWeight: '700', color: '#111827', flexShrink: 1 },
  verifiedBadge:        { fontSize: 12, color: '#fff', backgroundColor: '#16a34a', width: 18, height: 18, borderRadius: 9, textAlign: 'center', overflow: 'hidden', lineHeight: 18 },
  cardCity:             { fontSize: 12, color: '#6b7280', marginTop: 2 },
  rating:               { fontSize: 13, fontWeight: '600', color: '#374151', marginTop: 2 },
  cuisineRow:           { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 10, flexWrap: 'wrap' },
  cuisineChip:          { backgroundColor: '#fff7ed', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 100 },
  cuisineChipText:      { fontSize: 10, fontWeight: '700', color: '#c2410c' },
  cuisineMore:          { fontSize: 11, color: '#6b7280' },
  cardFooter:           { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 10 },
  cardMeta:             { fontSize: 11, color: '#6b7280', flexShrink: 1 },
  cardPrice:            { fontSize: 14, fontWeight: '800', color: '#f97316' },

  empty:                { alignItems: 'center', paddingTop: 60 },
  emptyIcon:            { fontSize: 48, marginBottom: 12 },
  emptyTitle:           { fontSize: 18, fontWeight: '600', color: '#374151' },
  emptySubtitle:        { fontSize: 14, color: '#9ca3af', marginTop: 4 },
});
