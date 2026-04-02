import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';

const CATEGORIES = ['CULINARY', 'CULTURAL', 'WELLNESS', 'ADVENTURE', 'CREATIVE'] as const;

const CATEGORY_ICONS: Record<string, string> = {
  CULINARY:  '🍳',
  CULTURAL:  '🏛️',
  WELLNESS:  '🧘',
  ADVENTURE: '🏔️',
  CREATIVE:  '🎨',
};

interface Experience {
  id: string;
  title: string;
  category: string;
  city: string;
  pricePaise: number;
  durationMinutes: number;
  avgRating?: number;
  reviewCount?: number;
  hostName: string;
}

export default function ExperiencesScreen() {
  const router = useRouter();
  const [experiences, setExperiences] = useState<Experience[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState('');

  const fetchExperiences = useCallback(async () => {
    setLoading(true);
    try {
      const params: { category?: string } = {};
      if (selectedCategory) params.category = selectedCategory;
      const result = await api.getExperiences(params);
      setExperiences(result ?? []);
    } catch {
      setExperiences([]);
    } finally {
      setLoading(false);
    }
  }, [selectedCategory]);

  useEffect(() => {
    fetchExperiences();
  }, [fetchExperiences]);

  function formatDuration(mins: number): string {
    if (mins < 60) return `${mins}m`;
    const hrs = Math.floor(mins / 60);
    const rem = mins % 60;
    return rem > 0 ? `${hrs}h ${rem}m` : `${hrs}h`;
  }

  function renderItem({ item }: { item: Experience }) {
    const icon = CATEGORY_ICONS[item.category] ?? '🎯';
    return (
      <TouchableOpacity onPress={() => router.push(`/experience/${item.id}`)} activeOpacity={0.7}>
      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <Text style={styles.cardIcon}>{icon}</Text>
        </View>
        <View style={styles.cardBody}>
          <View style={styles.cardTopRow}>
            <View style={styles.categoryBadge}>
              <Text style={styles.categoryText}>{item.category}</Text>
            </View>
            {item.avgRating != null && item.avgRating > 0 && (
              <Text style={styles.rating}>
                ★ {item.avgRating.toFixed(1)}
              </Text>
            )}
          </View>
          <Text style={styles.cardTitle} numberOfLines={2}>{item.title}</Text>
          <Text style={styles.cardCity}>{item.city}</Text>
          <View style={styles.cardFooter}>
            <Text style={styles.cardDuration}>{formatDuration(item.durationMinutes)}</Text>
            <Text style={styles.cardHost}>by {item.hostName}</Text>
          </View>
          <Text style={styles.cardPrice}>{formatPaise(item.pricePaise)}</Text>
        </View>
      </View>
      </TouchableOpacity>
    );
  }

  return (
    <View style={styles.container}>
      {/* Category filter */}
      <View style={styles.filterRow}>
        <TouchableOpacity
          style={[styles.filterChip, !selectedCategory && styles.filterChipActive]}
          onPress={() => setSelectedCategory('')}
        >
          <Text style={[styles.filterChipText, !selectedCategory && styles.filterChipTextActive]}>
            All
          </Text>
        </TouchableOpacity>
        {CATEGORIES.map((cat) => (
          <TouchableOpacity
            key={cat}
            style={[styles.filterChip, selectedCategory === cat && styles.filterChipActive]}
            onPress={() => setSelectedCategory(selectedCategory === cat ? '' : cat)}
          >
            <Text style={[styles.filterChipText, selectedCategory === cat && styles.filterChipTextActive]}>
              {CATEGORY_ICONS[cat]} {cat.charAt(0) + cat.slice(1).toLowerCase()}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {loading ? (
        <ActivityIndicator color="#f97316" style={{ marginTop: 40 }} size="large" />
      ) : (
        <FlatList
          data={experiences}
          keyExtractor={(item) => item.id}
          renderItem={renderItem}
          contentContainerStyle={experiences.length === 0 ? styles.emptyContainer : styles.list}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Text style={styles.emptyIcon}>🎭</Text>
              <Text style={styles.emptyTitle}>No experiences found</Text>
              <Text style={styles.emptySubtitle}>Try a different category</Text>
            </View>
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container:            { flex: 1, backgroundColor: '#f9fafb' },
  filterRow:            { flexDirection: 'row', gap: 8, padding: 16, paddingBottom: 8, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6', flexWrap: 'wrap' },
  filterChip:           { paddingHorizontal: 12, paddingVertical: 8, borderRadius: 100, borderWidth: 1, borderColor: '#e5e7eb' },
  filterChipActive:     { backgroundColor: '#f97316', borderColor: '#f97316' },
  filterChipText:       { fontSize: 12, fontWeight: '600', color: '#374151' },
  filterChipTextActive: { color: '#fff' },

  list:                 { padding: 16, gap: 12 },
  emptyContainer:       { flex: 1 },

  card:                 { backgroundColor: '#fff', borderRadius: 16, overflow: 'hidden', borderWidth: 1, borderColor: '#f3f4f6', marginBottom: 12 },
  cardHeader:           { height: 100, backgroundColor: '#fff7ed', alignItems: 'center', justifyContent: 'center' },
  cardIcon:             { fontSize: 40 },
  cardBody:             { padding: 12 },
  cardTopRow:           { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  categoryBadge:        { backgroundColor: '#fff7ed', paddingHorizontal: 8, paddingVertical: 2, borderRadius: 100 },
  categoryText:         { fontSize: 10, fontWeight: '700', color: '#c2410c' },
  rating:               { fontSize: 13, fontWeight: '600', color: '#374151' },
  cardTitle:            { fontSize: 15, fontWeight: '700', color: '#111827', marginTop: 6 },
  cardCity:             { fontSize: 12, color: '#6b7280', marginTop: 2 },
  cardFooter:           { flexDirection: 'row', gap: 12, marginTop: 6 },
  cardDuration:         { fontSize: 11, color: '#6b7280' },
  cardHost:             { fontSize: 11, color: '#6b7280' },
  cardPrice:            { fontSize: 15, fontWeight: '800', color: '#f97316', marginTop: 6 },

  empty:                { alignItems: 'center', paddingTop: 80 },
  emptyIcon:            { fontSize: 48, marginBottom: 12 },
  emptyTitle:           { fontSize: 18, fontWeight: '600', color: '#374151' },
  emptySubtitle:        { fontSize: 14, color: '#9ca3af', marginTop: 4 },
});
