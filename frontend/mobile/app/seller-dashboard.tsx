import { useEffect, useState, useCallback } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, FlatList, Image,
  StyleSheet, ActivityIndicator, RefreshControl, Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

const ORANGE = '#f97316';

function formatPrice(paise: number): string {
  const rupees = paise / 100;
  if (rupees >= 10000000) return `₹${(rupees / 10000000).toFixed(2)} Cr`;
  if (rupees >= 100000) return `₹${(rupees / 100000).toFixed(2)} L`;
  return `₹${rupees.toLocaleString('en-IN')}`;
}

const STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  ACTIVE: { bg: '#dcfce7', text: '#15803d' },
  PENDING: { bg: '#fef3c7', text: '#92400e' },
  SOLD: { bg: '#dbeafe', text: '#1d4ed8' },
  DRAFT: { bg: '#f3f4f6', text: '#6b7280' },
  PAUSED: { bg: '#fde68a', text: '#92400e' },
  REJECTED: { bg: '#fee2e2', text: '#dc2626' },
};

type TabKey = 'properties' | 'inquiries' | 'visits';

export default function SellerDashboardScreen() {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<TabKey>('properties');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  // Data
  const [properties, setProperties] = useState<any[]>([]);
  const [inquiries, setInquiries] = useState<any[]>([]);
  const [visits, setVisits] = useState<any[]>([]);

  // Stats
  const [stats, setStats] = useState({ listed: 0, active: 0, views: 0, inquiries: 0 });

  const loadData = useCallback(async () => {
    try {
      const token = await getAccessToken();
      if (!token) { router.push('/auth'); return; }

      const [propsData, inqData, visitsData] = await Promise.all([
        api.getSellerSaleProperties(token).catch(() => []),
        api.getSellerInquiries(token).catch(() => []),
        api.getSellerSiteVisits(token).catch(() => []),
      ]);

      const propsList = Array.isArray(propsData) ? propsData : [];
      const inqList = Array.isArray(inqData) ? inqData : [];
      const visitsList = Array.isArray(visitsData) ? visitsData : [];

      setProperties(propsList);
      setInquiries(inqList);
      setVisits(visitsList);

      const activeCount = propsList.filter((p: any) => p.status === 'ACTIVE').length;
      const totalViews = propsList.reduce((sum: number, p: any) => sum + (p.viewCount || 0), 0);

      setStats({
        listed: propsList.length,
        active: activeCount,
        views: totalViews,
        inquiries: inqList.length,
      });
    } catch {
      // keep defaults
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [router]);

  useEffect(() => { loadData(); }, [loadData]);

  const onRefresh = () => { setRefreshing(true); loadData(); };

  const handlePause = async (id: string) => {
    try {
      const token = await getAccessToken();
      if (!token) return;
      await api.updateSalePropertyStatus(id, 'PAUSED', token);
      loadData();
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to pause');
    }
  };

  const handleActivate = async (id: string) => {
    try {
      const token = await getAccessToken();
      if (!token) return;
      await api.updateSalePropertyStatus(id, 'ACTIVE', token);
      loadData();
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to activate');
    }
  };

  const renderProperty = ({ item }: { item: any }) => {
    const statusColor = STATUS_COLORS[item.status] || STATUS_COLORS.DRAFT;
    return (
      <TouchableOpacity
        style={styles.propCard}
        onPress={() => router.push(`/buy-property/${item.id}`)}
        activeOpacity={0.8}
      >
        <View style={styles.propCardRow}>
          {item.primaryPhotoUrl ? (
            <Image source={{ uri: item.primaryPhotoUrl }} style={styles.propThumb} resizeMode="cover" />
          ) : (
            <View style={[styles.propThumb, styles.propThumbPlaceholder]}>
              <Text style={{ fontSize: 24 }}>🏠</Text>
            </View>
          )}
          <View style={styles.propInfo}>
            <Text style={styles.propTitle} numberOfLines={1}>{item.title || 'Untitled'}</Text>
            <Text style={styles.propPrice}>{formatPrice(item.pricePaise || 0)}</Text>
            <Text style={styles.propMeta} numberOfLines={1}>
              {[item.bhk ? `${item.bhk} BHK` : null, item.areaSqft ? `${item.areaSqft} sq.ft` : null, item.city].filter(Boolean).join(' · ')}
            </Text>
            <View style={styles.propBottom}>
              <View style={[styles.statusBadge, { backgroundColor: statusColor.bg }]}>
                <Text style={[styles.statusText, { color: statusColor.text }]}>{item.status}</Text>
              </View>
              {item.viewCount > 0 && (
                <Text style={styles.propViews}>{item.viewCount} views</Text>
              )}
            </View>
          </View>
        </View>
        <View style={styles.propActions}>
          {item.status === 'ACTIVE' && (
            <TouchableOpacity style={styles.actionSmall} onPress={() => handlePause(item.id)}>
              <Text style={styles.actionSmallText}>Pause</Text>
            </TouchableOpacity>
          )}
          {item.status === 'PAUSED' && (
            <TouchableOpacity style={[styles.actionSmall, styles.actionSmallPrimary]} onPress={() => handleActivate(item.id)}>
              <Text style={styles.actionSmallPrimaryText}>Activate</Text>
            </TouchableOpacity>
          )}
        </View>
      </TouchableOpacity>
    );
  };

  const renderInquiry = ({ item }: { item: any }) => (
    <View style={styles.inquiryCard}>
      <View style={styles.inquiryHeader}>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>{(item.buyerName || 'B')[0].toUpperCase()}</Text>
        </View>
        <View style={{ flex: 1 }}>
          <Text style={styles.inquiryName}>{item.buyerName || 'Buyer'}</Text>
          <Text style={styles.inquiryDate}>{item.createdAt ? new Date(item.createdAt).toLocaleDateString('en-IN') : ''}</Text>
        </View>
      </View>
      <Text style={styles.inquiryProperty} numberOfLines={1}>Re: {item.propertyTitle || 'Property'}</Text>
      <Text style={styles.inquiryMessage} numberOfLines={3}>{item.message}</Text>
      {item.phone && <Text style={styles.inquiryPhone}>Phone: {item.phone}</Text>}
    </View>
  );

  const renderVisit = ({ item }: { item: any }) => {
    const statusColor = item.status === 'CONFIRMED'
      ? { bg: '#dcfce7', text: '#15803d' }
      : item.status === 'PENDING'
        ? { bg: '#fef3c7', text: '#92400e' }
        : item.status === 'COMPLETED'
          ? { bg: '#dbeafe', text: '#1d4ed8' }
          : { bg: '#f3f4f6', text: '#6b7280' };

    return (
      <View style={styles.visitCard}>
        <View style={styles.visitRow}>
          <View style={{ flex: 1 }}>
            <Text style={styles.visitProperty} numberOfLines={1}>{item.propertyTitle || 'Property'}</Text>
            <Text style={styles.visitBuyer}>{item.buyerName || 'Buyer'}</Text>
            <Text style={styles.visitDateTime}>{item.preferredDate} at {item.preferredTime}</Text>
          </View>
          <View style={[styles.statusBadge, { backgroundColor: statusColor.bg }]}>
            <Text style={[styles.statusText, { color: statusColor.text }]}>{item.status}</Text>
          </View>
        </View>
        {item.note && <Text style={styles.visitNote}>{item.note}</Text>}
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
        <Text style={styles.headerTitle}>Seller Dashboard</Text>
        <TouchableOpacity onPress={() => router.push('/sell')} style={styles.headerAdd}>
          <Text style={{ fontSize: 20, color: ORANGE, fontWeight: '700' }}>+</Text>
        </TouchableOpacity>
      </View>

      {/* Stats Cards */}
      <View style={styles.statsRow}>
        <View style={styles.statCard}>
          <Text style={styles.statValue}>{stats.listed}</Text>
          <Text style={styles.statLabel}>Listed</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={[styles.statValue, { color: '#16a34a' }]}>{stats.active}</Text>
          <Text style={styles.statLabel}>Active</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statValue}>{stats.views}</Text>
          <Text style={styles.statLabel}>Views</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={[styles.statValue, { color: ORANGE }]}>{stats.inquiries}</Text>
          <Text style={styles.statLabel}>Inquiries</Text>
        </View>
      </View>

      {/* Tabs */}
      <View style={styles.tabBar}>
        {([
          { key: 'properties' as TabKey, label: 'Properties' },
          { key: 'inquiries' as TabKey, label: 'Inquiries' },
          { key: 'visits' as TabKey, label: 'Visits' },
        ]).map((tab) => (
          <TouchableOpacity
            key={tab.key}
            style={[styles.tab, activeTab === tab.key && styles.tabActive]}
            onPress={() => setActiveTab(tab.key)}
          >
            <Text style={[styles.tabText, activeTab === tab.key && styles.tabTextActive]}>{tab.label}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Tab Content */}
      {activeTab === 'properties' && (
        <FlatList
          data={properties}
          keyExtractor={(item) => item.id}
          renderItem={renderProperty}
          contentContainerStyle={styles.listContent}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={ORANGE} />}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={{ fontSize: 40, marginBottom: 12 }}>🏠</Text>
              <Text style={styles.emptyTitle}>No properties listed</Text>
              <Text style={styles.emptyText}>Tap + to list your first property for sale</Text>
            </View>
          }
        />
      )}

      {activeTab === 'inquiries' && (
        <FlatList
          data={inquiries}
          keyExtractor={(item) => item.id}
          renderItem={renderInquiry}
          contentContainerStyle={styles.listContent}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={ORANGE} />}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={{ fontSize: 40, marginBottom: 12 }}>📩</Text>
              <Text style={styles.emptyTitle}>No inquiries yet</Text>
              <Text style={styles.emptyText}>Inquiries from buyers will appear here</Text>
            </View>
          }
        />
      )}

      {activeTab === 'visits' && (
        <FlatList
          data={visits}
          keyExtractor={(item) => item.id}
          renderItem={renderVisit}
          contentContainerStyle={styles.listContent}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={ORANGE} />}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={{ fontSize: 40, marginBottom: 12 }}>📅</Text>
              <Text style={styles.emptyTitle}>No site visits</Text>
              <Text style={styles.emptyText}>Visit requests from buyers will appear here</Text>
            </View>
          }
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#fff' },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#fff' },

  header: {
    flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 12,
    borderBottomWidth: 1, borderBottomColor: '#f3f4f6',
  },
  headerBack: {
    width: 32, height: 32, borderRadius: 16, backgroundColor: '#f3f4f6',
    alignItems: 'center', justifyContent: 'center', marginRight: 10,
  },
  headerTitle: { flex: 1, fontSize: 17, fontWeight: '700', color: '#111827' },
  headerAdd: {
    width: 36, height: 36, borderRadius: 18, backgroundColor: '#fff7ed',
    alignItems: 'center', justifyContent: 'center',
  },

  statsRow: { flexDirection: 'row', padding: 16, gap: 8 },
  statCard: {
    flex: 1, backgroundColor: '#f9fafb', borderRadius: 12, padding: 12, alignItems: 'center',
  },
  statValue: { fontSize: 20, fontWeight: '800', color: '#111827' },
  statLabel: { fontSize: 11, fontWeight: '500', color: '#9ca3af', marginTop: 2 },

  tabBar: {
    flexDirection: 'row', paddingHorizontal: 16, borderBottomWidth: 1, borderBottomColor: '#f3f4f6',
  },
  tab: { flex: 1, paddingVertical: 12, alignItems: 'center', borderBottomWidth: 2, borderBottomColor: 'transparent' },
  tabActive: { borderBottomColor: ORANGE },
  tabText: { fontSize: 14, fontWeight: '600', color: '#9ca3af' },
  tabTextActive: { color: ORANGE },

  listContent: { padding: 16, paddingBottom: 30 },

  propCard: {
    backgroundColor: '#fff', borderRadius: 14, marginBottom: 12, padding: 14,
    elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.06, shadowRadius: 4,
    borderWidth: 1, borderColor: '#f3f4f6',
  },
  propCardRow: { flexDirection: 'row', gap: 12 },
  propThumb: { width: 80, height: 80, borderRadius: 10, backgroundColor: '#f3f4f6' },
  propThumbPlaceholder: { alignItems: 'center', justifyContent: 'center' },
  propInfo: { flex: 1 },
  propTitle: { fontSize: 14, fontWeight: '600', color: '#111827' },
  propPrice: { fontSize: 16, fontWeight: '800', color: '#111827', marginTop: 2 },
  propMeta: { fontSize: 12, color: '#6b7280', marginTop: 2 },
  propBottom: { flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 6 },
  statusBadge: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: 6 },
  statusText: { fontSize: 10, fontWeight: '700' },
  propViews: { fontSize: 11, color: '#9ca3af' },
  propActions: { flexDirection: 'row', justifyContent: 'flex-end', gap: 8, marginTop: 10 },
  actionSmall: { paddingHorizontal: 16, paddingVertical: 7, borderRadius: 8, backgroundColor: '#f3f4f6' },
  actionSmallText: { fontSize: 12, fontWeight: '600', color: '#6b7280' },
  actionSmallPrimary: { backgroundColor: '#fff7ed' },
  actionSmallPrimaryText: { fontSize: 12, fontWeight: '600', color: ORANGE },

  inquiryCard: {
    backgroundColor: '#fff', borderRadius: 14, marginBottom: 12, padding: 14,
    elevation: 1, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.04, shadowRadius: 2,
    borderWidth: 1, borderColor: '#f3f4f6',
  },
  inquiryHeader: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 8 },
  avatar: {
    width: 36, height: 36, borderRadius: 18, backgroundColor: '#fed7aa',
    alignItems: 'center', justifyContent: 'center',
  },
  avatarText: { fontSize: 14, fontWeight: '700', color: '#ea580c' },
  inquiryName: { fontSize: 14, fontWeight: '600', color: '#111827' },
  inquiryDate: { fontSize: 11, color: '#9ca3af', marginTop: 1 },
  inquiryProperty: { fontSize: 12, fontWeight: '600', color: ORANGE, marginBottom: 4 },
  inquiryMessage: { fontSize: 13, color: '#374151', lineHeight: 20 },
  inquiryPhone: { fontSize: 12, color: '#6b7280', marginTop: 6, fontWeight: '500' },

  visitCard: {
    backgroundColor: '#fff', borderRadius: 14, marginBottom: 12, padding: 14,
    elevation: 1, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.04, shadowRadius: 2,
    borderWidth: 1, borderColor: '#f3f4f6',
  },
  visitRow: { flexDirection: 'row', alignItems: 'flex-start', gap: 10 },
  visitProperty: { fontSize: 14, fontWeight: '600', color: '#111827' },
  visitBuyer: { fontSize: 13, color: '#6b7280', marginTop: 2 },
  visitDateTime: { fontSize: 13, fontWeight: '600', color: ORANGE, marginTop: 4 },
  visitNote: { fontSize: 12, color: '#6b7280', marginTop: 8, fontStyle: 'italic' },

  emptyContainer: { alignItems: 'center', paddingVertical: 60 },
  emptyTitle: { fontSize: 16, fontWeight: '700', color: '#374151' },
  emptyText: { fontSize: 13, color: '#9ca3af', marginTop: 4 },
});
