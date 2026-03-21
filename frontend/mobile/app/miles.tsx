import { useEffect, useState } from 'react';
import {
  View,
  Text,
  FlatList,
  StyleSheet,
  ActivityIndicator,
  TouchableOpacity,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const TIER_COLORS: Record<string, { bg: string; text: string }> = {
  BRONZE:   { bg: '#fef3c7', text: '#92400e' },
  SILVER:   { bg: '#e5e7eb', text: '#374151' },
  GOLD:     { bg: '#fef9c3', text: '#854d0e' },
  PLATINUM: { bg: '#ede9fe', text: '#6d28d9' },
};

interface MilesBalanceData {
  balance: number;
  tier: string;
  lifetimeMiles: number;
}

interface HistoryEntry {
  id: string;
  type: 'EARNED' | 'REDEEMED';
  miles: number;
  description: string;
  createdAt: string;
}

export default function MilesScreen() {
  const router = useRouter();
  const [balance, setBalance] = useState<MilesBalanceData | null>(null);
  const [history, setHistory] = useState<HistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [authed, setAuthed] = useState(false);

  useEffect(() => {
    (async () => {
      const token = await getAccessToken();
      if (!token) {
        setLoading(false);
        return;
      }
      setAuthed(true);
      try {
        const [bal, hist] = await Promise.all([
          api.getMilesBalance(token),
          api.getMilesHistory(token, 0),
        ]);
        setBalance(bal);
        setHistory(hist.content ?? []);
      } catch {
        // graceful degradation
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#f97316" />
      </View>
    );
  }

  if (!authed) {
    return (
      <View style={styles.center}>
        <Text style={styles.emptyIcon}>🔒</Text>
        <Text style={styles.emptyTitle}>Sign in to view miles</Text>
        <TouchableOpacity style={styles.button} onPress={() => router.push('/auth')}>
          <Text style={styles.buttonText}>Sign in</Text>
        </TouchableOpacity>
      </View>
    );
  }

  const tier = balance?.tier ?? 'BRONZE';
  const tierStyle = TIER_COLORS[tier] ?? TIER_COLORS.BRONZE;

  return (
    <View style={styles.container}>
      {/* Balance card */}
      <View style={styles.balanceCard}>
        <View style={styles.balanceRow}>
          <View style={{ flex: 1 }}>
            <Text style={styles.balanceLabel}>Current Balance</Text>
            <Text style={styles.balanceValue}>
              {(balance?.balance ?? 0).toLocaleString('en-IN')} miles
            </Text>
            <Text style={styles.lifetimeLabel}>
              Lifetime: {(balance?.lifetimeMiles ?? 0).toLocaleString('en-IN')}
            </Text>
          </View>
          <View style={[styles.tierBadge, { backgroundColor: tierStyle.bg }]}>
            <Text style={[styles.tierText, { color: tierStyle.text }]}>{tier}</Text>
          </View>
        </View>
      </View>

      {/* History */}
      <Text style={styles.sectionTitle}>Recent History</Text>

      <FlatList
        data={history}
        keyExtractor={(item) => item.id}
        contentContainerStyle={history.length === 0 ? styles.center : styles.list}
        ListEmptyComponent={
          <View style={styles.emptyInner}>
            <Text style={styles.emptyIcon}>🎖️</Text>
            <Text style={styles.emptyTitle}>No history yet</Text>
            <Text style={styles.emptySubtitle}>Start booking to earn miles!</Text>
          </View>
        }
        renderItem={({ item }) => (
          <View style={styles.historyCard}>
            <View style={styles.historyRow}>
              <View style={{ flex: 1 }}>
                <Text style={styles.historyDesc}>{item.description}</Text>
                <Text style={styles.historyDate}>
                  {new Date(item.createdAt).toLocaleDateString('en-IN')}
                </Text>
              </View>
              <View style={{ alignItems: 'flex-end' }}>
                <View
                  style={[
                    styles.typeBadge,
                    {
                      backgroundColor: item.type === 'EARNED' ? '#dcfce7' : '#ffedd5',
                    },
                  ]}
                >
                  <Text
                    style={[
                      styles.typeBadgeText,
                      { color: item.type === 'EARNED' ? '#15803d' : '#c2410c' },
                    ]}
                  >
                    {item.type}
                  </Text>
                </View>
                <Text style={styles.historyMiles}>
                  {item.type === 'EARNED' ? '+' : '-'}
                  {item.miles.toLocaleString('en-IN')}
                </Text>
              </View>
            </View>
          </View>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container:      { flex: 1, backgroundColor: '#f9fafb' },
  center:         { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32 },
  list:           { padding: 16, paddingTop: 0 },

  balanceCard:    { margin: 16, backgroundColor: '#f97316', borderRadius: 20, padding: 20 },
  balanceRow:     { flexDirection: 'row', alignItems: 'center' },
  balanceLabel:   { color: 'rgba(255,255,255,0.8)', fontSize: 13 },
  balanceValue:   { color: '#fff', fontSize: 28, fontWeight: '800', marginTop: 4 },
  lifetimeLabel:  { color: 'rgba(255,255,255,0.7)', fontSize: 12, marginTop: 4 },
  tierBadge:      { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 100, borderWidth: 2, borderColor: 'rgba(255,255,255,0.4)' },
  tierText:       { fontSize: 12, fontWeight: '800' },

  sectionTitle:   { fontSize: 17, fontWeight: '700', color: '#111827', paddingHorizontal: 16, paddingTop: 8, paddingBottom: 12 },

  historyCard:    { backgroundColor: '#fff', borderRadius: 14, padding: 14, marginBottom: 10, borderWidth: 1, borderColor: '#f3f4f6' },
  historyRow:     { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  historyDesc:    { fontSize: 13, fontWeight: '600', color: '#111827', flex: 1 },
  historyDate:    { fontSize: 11, color: '#9ca3af', marginTop: 4 },
  typeBadge:      { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 100, marginBottom: 4 },
  typeBadgeText:  { fontSize: 10, fontWeight: '700' },
  historyMiles:   { fontSize: 14, fontWeight: '700', color: '#111827' },

  emptyInner:     { alignItems: 'center', paddingTop: 60 },
  emptyIcon:      { fontSize: 48, marginBottom: 12 },
  emptyTitle:     { fontSize: 18, fontWeight: '600', color: '#374151', marginBottom: 8 },
  emptySubtitle:  { fontSize: 14, color: '#9ca3af' },
  button:         { backgroundColor: '#f97316', borderRadius: 12, paddingHorizontal: 24, paddingVertical: 12, marginTop: 8 },
  buttonText:     { color: '#fff', fontWeight: '600', fontSize: 14 },
});
