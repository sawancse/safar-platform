import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity,
  StyleSheet, ActivityIndicator, Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';
import { formatPaise } from '@/lib/utils';

interface SavedItem {
  id: string;
  listingId: string;
  title?: string;
  city?: string;
  basePricePaise?: number;
  primaryPhotoUrl?: string;
}

export default function SavedScreen() {
  const router = useRouter();
  const [items, setItems] = useState<SavedItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState<string | null>(null);

  const loadSaved = useCallback(async (t: string) => {
    try {
      const data = await api.getBucketList(t);
      setItems(data ?? []);
    } catch {
      setItems([]);
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
      loadSaved(t);
    })();
  }, []);

  async function handleRemove(listingId: string) {
    if (!token) return;
    Alert.alert('Remove', 'Remove this listing from saved?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Remove',
        style: 'destructive',
        onPress: async () => {
          try {
            await api.removeFromBucketList(listingId, token);
            setItems((prev) => prev.filter((i) => (i.listingId ?? i.id) !== listingId));
          } catch (err: any) {
            Alert.alert('Error', err.message ?? 'Failed to remove');
          }
        },
      },
    ]);
  }

  function renderItem({ item }: { item: SavedItem }) {
    const lid = item.listingId ?? item.id;
    return (
      <TouchableOpacity
        style={styles.card}
        activeOpacity={0.85}
        onPress={() => router.push(`/listing/${lid}`)}
      >
        <View style={styles.cardImage}>
          <Text style={{ fontSize: 32 }}>🏠</Text>
        </View>
        <View style={styles.cardBody}>
          <Text style={styles.cardTitle} numberOfLines={1}>{item.title ?? 'Listing'}</Text>
          {item.city && <Text style={styles.cardCity}>{item.city}</Text>}
          {item.basePricePaise != null && (
            <Text style={styles.cardPrice}>{formatPaise(item.basePricePaise)} / night</Text>
          )}
        </View>
        <TouchableOpacity style={styles.removeBtn} onPress={() => handleRemove(lid)}>
          <Text style={styles.removeText}>Remove</Text>
        </TouchableOpacity>
      </TouchableOpacity>
    );
  }

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>‹ Back</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Saved Stays</Text>
        <View style={{ width: 60 }} />
      </View>

      {loading ? (
        <ActivityIndicator size="large" color="#f97316" style={{ marginTop: 40 }} />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item) => item.listingId ?? item.id}
          renderItem={renderItem}
          contentContainerStyle={items.length === 0 ? styles.emptyContainer : styles.list}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Text style={styles.emptyIcon}>♡</Text>
              <Text style={styles.emptyTitle}>No saved stays yet</Text>
              <Text style={styles.emptySubtitle}>Listings you save will appear here</Text>
              <TouchableOpacity style={styles.exploreBtn} onPress={() => router.push('/')}>
                <Text style={styles.exploreBtnText}>Explore</Text>
              </TouchableOpacity>
            </View>
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container:      { flex: 1, backgroundColor: '#f9fafb' },
  header:         { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: '#fff', paddingTop: 52, paddingBottom: 12, paddingHorizontal: 16, borderBottomWidth: 1, borderBottomColor: '#f3f4f6' },
  backBtn:        { width: 60 },
  backText:       { fontSize: 16, color: '#f97316', fontWeight: '600' },
  headerTitle:    { fontSize: 17, fontWeight: '700', color: '#111827' },
  list:           { padding: 16 },
  emptyContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  card:           { backgroundColor: '#fff', borderRadius: 16, overflow: 'hidden', borderWidth: 1, borderColor: '#f3f4f6', marginBottom: 12, flexDirection: 'row', alignItems: 'center' },
  cardImage:      { width: 80, height: 80, backgroundColor: '#f3f4f6', alignItems: 'center', justifyContent: 'center' },
  cardBody:       { flex: 1, padding: 12 },
  cardTitle:      { fontSize: 14, fontWeight: '600', color: '#111827' },
  cardCity:       { fontSize: 12, color: '#6b7280', marginTop: 2 },
  cardPrice:      { fontSize: 13, fontWeight: '700', color: '#111827', marginTop: 4 },
  removeBtn:      { paddingHorizontal: 14, paddingVertical: 8 },
  removeText:     { fontSize: 12, fontWeight: '600', color: '#ef4444' },
  empty:          { alignItems: 'center', paddingTop: 80 },
  emptyIcon:      { fontSize: 48, color: '#d1d5db', marginBottom: 12 },
  emptyTitle:     { fontSize: 18, fontWeight: '600', color: '#374151' },
  emptySubtitle:  { fontSize: 14, color: '#9ca3af', marginTop: 4 },
  exploreBtn:     { backgroundColor: '#f97316', borderRadius: 12, paddingHorizontal: 24, paddingVertical: 12, marginTop: 20 },
  exploreBtnText: { color: '#fff', fontWeight: '600', fontSize: 14 },
});
