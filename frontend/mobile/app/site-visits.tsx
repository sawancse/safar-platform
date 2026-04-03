import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity,
  StyleSheet, ActivityIndicator, RefreshControl, Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const ORANGE = '#f97316';

const STATUS_CONFIG: Record<string, { bg: string; text: string; label: string }> = {
  PENDING: { bg: '#fef3c7', text: '#92400e', label: 'Pending' },
  CONFIRMED: { bg: '#dcfce7', text: '#15803d', label: 'Confirmed' },
  COMPLETED: { bg: '#dbeafe', text: '#1d4ed8', label: 'Completed' },
  CANCELLED: { bg: '#fee2e2', text: '#dc2626', label: 'Cancelled' },
  RESCHEDULED: { bg: '#fde68a', text: '#92400e', label: 'Rescheduled' },
};

export default function SiteVisitsScreen() {
  const router = useRouter();
  const [visits, setVisits] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const loadVisits = useCallback(async () => {
    try {
      const token = await getAccessToken();
      if (!token) { router.push('/auth'); return; }
      const data = await api.getSellerSiteVisits(token);
      setVisits(Array.isArray(data) ? data : []);
    } catch {
      setVisits([]);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [router]);

  useEffect(() => { loadVisits(); }, [loadVisits]);

  const onRefresh = () => { setRefreshing(true); loadVisits(); };

  const handleAction = async (visitId: string, action: 'confirm' | 'cancel' | 'complete') => {
    const token = await getAccessToken();
    if (!token) return;
    setActionLoading(visitId);
    try {
      const statusMap = { confirm: 'CONFIRMED', cancel: 'CANCELLED', complete: 'COMPLETED' };
      await api.updateSiteVisitStatus(visitId, statusMap[action], token);
      loadVisits();
    } catch (err: any) {
      Alert.alert('Error', err.message || `Failed to ${action} visit`);
    } finally {
      setActionLoading(null);
    }
  };

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const upcomingVisits = visits.filter((v) => {
    if (v.status === 'CANCELLED' || v.status === 'COMPLETED') return false;
    const visitDate = new Date(v.preferredDate);
    return visitDate >= today;
  });

  const pastVisits = visits.filter((v) => {
    if (v.status === 'COMPLETED' || v.status === 'CANCELLED') return true;
    const visitDate = new Date(v.preferredDate);
    return visitDate < today;
  });

  const renderVisitItem = (item: any, isPast: boolean) => {
    const statusCfg = STATUS_CONFIG[item.status] || STATUS_CONFIG.PENDING;
    const isActioning = actionLoading === item.id;

    return (
      <View style={styles.card}>
        <View style={styles.cardHeader}>
          <View style={{ flex: 1 }}>
            <Text style={styles.cardProperty} numberOfLines={1}>{item.propertyTitle || 'Property'}</Text>
            <Text style={styles.cardLocation} numberOfLines={1}>
              {[item.locality, item.city].filter(Boolean).join(', ') || 'Location not specified'}
            </Text>
          </View>
          <View style={[styles.statusBadge, { backgroundColor: statusCfg.bg }]}>
            <Text style={[styles.statusText, { color: statusCfg.text }]}>{statusCfg.label}</Text>
          </View>
        </View>

        <View style={styles.cardDetails}>
          <View style={styles.detailItem}>
            <Text style={styles.detailLabel}>Date</Text>
            <Text style={styles.detailValue}>
              {item.preferredDate ? new Date(item.preferredDate).toLocaleDateString('en-IN', {
                weekday: 'short', day: 'numeric', month: 'short', year: 'numeric',
              }) : 'TBD'}
            </Text>
          </View>
          <View style={styles.detailItem}>
            <Text style={styles.detailLabel}>Time</Text>
            <Text style={styles.detailValue}>{item.preferredTime || 'TBD'}</Text>
          </View>
        </View>

        <View style={styles.buyerSection}>
          <View style={styles.buyerAvatar}>
            <Text style={styles.buyerAvatarText}>{(item.buyerName || 'B')[0].toUpperCase()}</Text>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.buyerName}>{item.buyerName || 'Buyer'}</Text>
            {item.buyerPhone && <Text style={styles.buyerPhone}>{item.buyerPhone}</Text>}
          </View>
        </View>

        {item.note && (
          <View style={styles.noteBox}>
            <Text style={styles.noteLabel}>Note</Text>
            <Text style={styles.noteText}>{item.note}</Text>
          </View>
        )}

        {!isPast && (
          <View style={styles.actionRow}>
            {item.status === 'PENDING' && (
              <>
                <TouchableOpacity
                  style={[styles.actionBtn, styles.actionConfirm]}
                  onPress={() => handleAction(item.id, 'confirm')}
                  disabled={isActioning}
                >
                  {isActioning ? (
                    <ActivityIndicator size="small" color="#fff" />
                  ) : (
                    <Text style={styles.actionConfirmText}>Confirm</Text>
                  )}
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.actionBtn, styles.actionCancel]}
                  onPress={() => {
                    Alert.alert(
                      'Cancel Visit',
                      'Are you sure you want to cancel this visit?',
                      [
                        { text: 'No', style: 'cancel' },
                        { text: 'Yes, Cancel', onPress: () => handleAction(item.id, 'cancel'), style: 'destructive' },
                      ]
                    );
                  }}
                  disabled={isActioning}
                >
                  <Text style={styles.actionCancelText}>Cancel</Text>
                </TouchableOpacity>
              </>
            )}
            {item.status === 'CONFIRMED' && (
              <>
                <TouchableOpacity
                  style={[styles.actionBtn, styles.actionComplete]}
                  onPress={() => handleAction(item.id, 'complete')}
                  disabled={isActioning}
                >
                  {isActioning ? (
                    <ActivityIndicator size="small" color="#fff" />
                  ) : (
                    <Text style={styles.actionCompleteText}>Mark Complete</Text>
                  )}
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.actionBtn, styles.actionCancel]}
                  onPress={() => {
                    Alert.alert(
                      'Cancel Visit',
                      'Are you sure you want to cancel this confirmed visit?',
                      [
                        { text: 'No', style: 'cancel' },
                        { text: 'Yes, Cancel', onPress: () => handleAction(item.id, 'cancel'), style: 'destructive' },
                      ]
                    );
                  }}
                  disabled={isActioning}
                >
                  <Text style={styles.actionCancelText}>Cancel</Text>
                </TouchableOpacity>
              </>
            )}
          </View>
        )}
      </View>
    );
  };

  if (loading) {
    return <View style={styles.center}><ActivityIndicator size="large" color={ORANGE} /></View>;
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.headerBack}>
          <Text style={{ fontSize: 18, color: '#374151', fontWeight: '700' }}>‹</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Site Visits</Text>
        <View style={{ width: 32 }} />
      </View>

      <FlatList
        data={[{ type: 'upcoming' }, { type: 'past' }]}
        keyExtractor={(item) => item.type}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={ORANGE} />}
        contentContainerStyle={{ paddingBottom: 30 }}
        renderItem={({ item: section }) => {
          if (section.type === 'upcoming') {
            return (
              <View>
                <View style={styles.sectionHeader}>
                  <Text style={styles.sectionTitle}>Upcoming Visits</Text>
                  <View style={styles.sectionCount}>
                    <Text style={styles.sectionCountText}>{upcomingVisits.length}</Text>
                  </View>
                </View>
                {upcomingVisits.length === 0 ? (
                  <View style={styles.emptySection}>
                    <Text style={{ fontSize: 32, marginBottom: 8 }}>📅</Text>
                    <Text style={styles.emptyText}>No upcoming visits</Text>
                  </View>
                ) : (
                  upcomingVisits.map((v) => (
                    <View key={v.id} style={{ paddingHorizontal: 16 }}>
                      {renderVisitItem(v, false)}
                    </View>
                  ))
                )}
              </View>
            );
          }
          return (
            <View>
              <View style={styles.sectionHeader}>
                <Text style={styles.sectionTitle}>Past Visits</Text>
                <View style={styles.sectionCount}>
                  <Text style={styles.sectionCountText}>{pastVisits.length}</Text>
                </View>
              </View>
              {pastVisits.length === 0 ? (
                <View style={styles.emptySection}>
                  <Text style={{ fontSize: 32, marginBottom: 8 }}>📋</Text>
                  <Text style={styles.emptyText}>No past visits</Text>
                </View>
              ) : (
                pastVisits.map((v) => (
                  <View key={v.id} style={{ paddingHorizontal: 16 }}>
                    {renderVisitItem(v, true)}
                  </View>
                ))
              )}
            </View>
          );
        }}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#f9fafb' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb' },

  header: {
    flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 12,
    backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#f3f4f6',
  },
  headerBack: {
    width: 32, height: 32, borderRadius: 16, backgroundColor: '#f3f4f6',
    alignItems: 'center', justifyContent: 'center', marginRight: 10,
  },
  headerTitle: { flex: 1, fontSize: 17, fontWeight: '700', color: '#111827' },

  sectionHeader: {
    flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingTop: 20, paddingBottom: 10, gap: 8,
  },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: '#111827' },
  sectionCount: {
    backgroundColor: ORANGE, borderRadius: 10, paddingHorizontal: 8, paddingVertical: 2,
  },
  sectionCountText: { fontSize: 11, fontWeight: '700', color: '#fff' },

  card: {
    backgroundColor: '#fff', borderRadius: 14, marginBottom: 12, padding: 16,
    elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.06, shadowRadius: 4,
  },
  cardHeader: { flexDirection: 'row', alignItems: 'flex-start', gap: 10, marginBottom: 12 },
  cardProperty: { fontSize: 15, fontWeight: '700', color: '#111827' },
  cardLocation: { fontSize: 12, color: '#6b7280', marginTop: 2 },
  statusBadge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 8 },
  statusText: { fontSize: 11, fontWeight: '700' },

  cardDetails: { flexDirection: 'row', gap: 20, marginBottom: 12 },
  detailItem: {},
  detailLabel: { fontSize: 11, color: '#9ca3af', fontWeight: '500' },
  detailValue: { fontSize: 14, fontWeight: '600', color: '#111827', marginTop: 2 },

  buyerSection: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 10 },
  buyerAvatar: {
    width: 36, height: 36, borderRadius: 18, backgroundColor: '#fed7aa',
    alignItems: 'center', justifyContent: 'center',
  },
  buyerAvatarText: { fontSize: 14, fontWeight: '700', color: '#ea580c' },
  buyerName: { fontSize: 13, fontWeight: '600', color: '#374151' },
  buyerPhone: { fontSize: 12, color: '#6b7280', marginTop: 1 },

  noteBox: { backgroundColor: '#f9fafb', borderRadius: 8, padding: 10, marginBottom: 10 },
  noteLabel: { fontSize: 11, fontWeight: '600', color: '#9ca3af', marginBottom: 2 },
  noteText: { fontSize: 13, color: '#374151', lineHeight: 19 },

  actionRow: { flexDirection: 'row', gap: 8, marginTop: 4 },
  actionBtn: { flex: 1, paddingVertical: 10, borderRadius: 10, alignItems: 'center' },
  actionConfirm: { backgroundColor: '#16a34a' },
  actionConfirmText: { color: '#fff', fontWeight: '700', fontSize: 13 },
  actionCancel: { backgroundColor: '#fee2e2' },
  actionCancelText: { color: '#dc2626', fontWeight: '600', fontSize: 13 },
  actionComplete: { backgroundColor: '#2563eb' },
  actionCompleteText: { color: '#fff', fontWeight: '700', fontSize: 13 },

  emptySection: { alignItems: 'center', paddingVertical: 30 },
  emptyText: { fontSize: 13, color: '#9ca3af' },
});
