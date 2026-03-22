import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  RefreshControl,
  Modal,
  FlatList,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ── Types ─────────────────────────────────────────────────── */

interface OccupancyMonth {
  month: string;
  occupancyPercent: number;
  adr: number;
  revpar: number;
  revenue: number;
}

interface OccupancyOverall {
  totalBookings: number;
  cancelledBookings: number;
  noShows: number;
  avgOccupancy: number;
  avgAdr: number;
  avgRevpar: number;
}

interface OccupancyReport {
  months: OccupancyMonth[];
  overall: OccupancyOverall;
}

interface ListingItem {
  id: string;
  title: string;
}

/* ── Constants ─────────────────────────────────────────────── */

type DateRange = 'this_month' | 'last_month' | 'last_3_months';
const DATE_RANGES: { key: DateRange; label: string }[] = [
  { key: 'this_month', label: 'This Month' },
  { key: 'last_month', label: 'Last Month' },
  { key: 'last_3_months', label: 'Last 3 Months' },
];

/* ── Helpers ───────────────────────────────────────────────── */

function formatAmount(paise: number): string {
  return '\u20B9' + (paise / 100).toLocaleString('en-IN');
}

function formatAmountRupees(rupees: number): string {
  return '\u20B9' + rupees.toLocaleString('en-IN');
}

function getDateRange(range: DateRange): { from: string; to: string } {
  const now = new Date();
  let from: Date;
  let to: Date = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);

  switch (range) {
    case 'last_month': {
      from = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      to = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59);
      break;
    }
    case 'last_3_months': {
      from = new Date(now.getFullYear(), now.getMonth() - 3, 1);
      break;
    }
    case 'this_month':
    default: {
      from = new Date(now.getFullYear(), now.getMonth(), 1);
      break;
    }
  }

  return { from: from.toISOString(), to: to.toISOString() };
}

/* ── Component ─────────────────────────────────────────────── */

export default function HostAnalyticsScreen() {
  const router = useRouter();

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [authed, setAuthed] = useState(false);

  // Data
  const [report, setReport] = useState<OccupancyReport | null>(null);
  const [totalRevenuePaise, setTotalRevenuePaise] = useState(0);

  // Filters
  const [dateRange, setDateRange] = useState<DateRange>('this_month');
  const [listings, setListings] = useState<ListingItem[]>([]);
  const [selectedListingId, setSelectedListingId] = useState<string | null>(null);
  const [showListingPicker, setShowListingPicker] = useState(false);

  /* ── Data loaders ───────────────────────────────────── */

  const loadData = useCallback(async (silent = false) => {
    const token = await getAccessToken();
    if (!token) {
      setLoading(false);
      return;
    }
    setAuthed(true);
    if (!silent) setLoading(true);

    try {
      const [reportData, listingsData, earningsData] = await Promise.all([
        api.getOccupancyReport(token, selectedListingId),
        api.getMyListings(token),
        api.getHostEarnings(token, getDateRange(dateRange)),
      ]);

      setReport(reportData);

      const items: ListingItem[] = Array.isArray(listingsData?.content)
        ? listingsData.content
        : Array.isArray(listingsData)
        ? listingsData
        : [];
      setListings(items);

      setTotalRevenuePaise(earningsData?.totalRevenuePaise ?? 0);
    } catch {
      setReport(null);
    } finally {
      setLoading(false);
    }
  }, [selectedListingId, dateRange]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadData(true);
    setRefreshing(false);
  }, [loadData]);

  /* ── Auth guard ─────────────────────────────────────── */

  if (loading) {
    return (
      <View style={s.centered}>
        <ActivityIndicator size="large" color="#f97316" />
      </View>
    );
  }

  if (!authed) {
    return (
      <View style={s.centered}>
        <Text style={s.emptyTitle}>Sign in required</Text>
        <Text style={s.emptySubtitle}>Please sign in to view analytics.</Text>
        <TouchableOpacity style={s.primaryBtn} onPress={() => router.push('/auth')}>
          <Text style={s.primaryBtnText}>Sign In</Text>
        </TouchableOpacity>
      </View>
    );
  }

  /* ── Derived values ─────────────────────────────────── */

  const overall = report?.overall;
  const months = report?.months ?? [];

  const totalBookings = overall?.totalBookings ?? 0;
  const cancelledBookings = overall?.cancelledBookings ?? 0;
  const noShows = overall?.noShows ?? 0;
  const confirmedBookings = totalBookings - cancelledBookings - noShows;
  const cancellationRate = totalBookings > 0 ? ((cancelledBookings / totalBookings) * 100).toFixed(1) : '0.0';

  const selectedListingTitle =
    selectedListingId === null
      ? 'All Listings'
      : listings.find((l) => l.id === selectedListingId)?.title ?? 'Selected';

  /* ── Render ─────────────────────────────────────────── */

  return (
    <View style={s.container}>
      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
          <Text style={s.backIcon}>{'←'}</Text>
        </TouchableOpacity>
        <Text style={s.headerTitle}>Analytics</Text>
        <View style={s.headerSpacer} />
      </View>

      <ScrollView
        style={s.body}
        contentContainerStyle={s.bodyContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#f97316" />}
      >
        {/* Date Range Filter */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={s.filterRow}>
          {DATE_RANGES.map((r) => (
            <TouchableOpacity
              key={r.key}
              style={[s.filterChip, dateRange === r.key && s.filterChipActive]}
              onPress={() => setDateRange(r.key)}
            >
              <Text style={[s.filterChipText, dateRange === r.key && s.filterChipTextActive]}>
                {r.label}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        {/* Listing Selector */}
        <TouchableOpacity style={s.listingSelector} onPress={() => setShowListingPicker(true)}>
          <Text style={s.listingSelectorLabel}>Listing</Text>
          <Text style={s.listingSelectorValue} numberOfLines={1}>
            {selectedListingTitle} {'▾'}
          </Text>
        </TouchableOpacity>

        {/* KPI Cards */}
        <Text style={s.sectionTitle}>Key Metrics</Text>
        <View style={s.kpiGrid}>
          <KpiCard label="Total Bookings" value={String(totalBookings)} color="#2563eb" />
          <KpiCard
            label="Occupancy Rate"
            value={`${(overall?.avgOccupancy ?? 0).toFixed(1)}%`}
            color="#059669"
          />
          <KpiCard
            label="Avg Daily Rate"
            value={formatAmountRupees(overall?.avgAdr ?? 0)}
            color="#f97316"
          />
          <KpiCard
            label="RevPAR"
            value={formatAmountRupees(overall?.avgRevpar ?? 0)}
            color="#7c3aed"
          />
        </View>

        {/* Occupancy Report */}
        <Text style={s.sectionTitle}>Occupancy Report</Text>
        {months.length > 0 ? (
          <View style={s.card}>
            {months.map((m, idx) => (
              <View key={idx} style={s.occupancyRow}>
                <Text style={s.occupancyMonth}>{m.month}</Text>
                <View style={s.barContainer}>
                  <View style={[s.bar, { width: `${Math.min(m.occupancyPercent, 100)}%` }]} />
                </View>
                <Text style={s.occupancyPercent}>{m.occupancyPercent.toFixed(0)}%</Text>
              </View>
            ))}
            <View style={s.divider} />
            <View style={s.metricsRow}>
              <MetricPill label="ADR" value={formatAmountRupees(overall?.avgAdr ?? 0)} />
              <MetricPill label="RevPAR" value={formatAmountRupees(overall?.avgRevpar ?? 0)} />
              <MetricPill label="Revenue" value={formatAmount(totalRevenuePaise)} />
            </View>
          </View>
        ) : (
          <View style={s.emptyBlock}>
            <Text style={s.emptySubtitle}>No occupancy data available.</Text>
          </View>
        )}

        {/* Booking Stats */}
        <Text style={s.sectionTitle}>Booking Stats</Text>
        <View style={s.card}>
          <View style={s.bookingStatsRow}>
            <BookingBadge label="Confirmed" count={confirmedBookings} bg="#dcfce7" color="#14532d" />
            <BookingBadge label="Cancelled" count={cancelledBookings} bg="#fee2e2" color="#7f1d1d" />
            <BookingBadge label="No-show" count={noShows} bg="#fef9c3" color="#854d0e" />
          </View>
          <View style={s.divider} />
          <View style={s.cancellationRow}>
            <Text style={s.cancellationLabel}>Cancellation Rate</Text>
            <Text
              style={[
                s.cancellationValue,
                { color: Number(cancellationRate) > 5 ? '#dc2626' : '#059669' },
              ]}
            >
              {cancellationRate}%
            </Text>
          </View>
        </View>
      </ScrollView>

      {/* Listing Picker Modal */}
      <Modal visible={showListingPicker} animationType="slide" transparent>
        <View style={s.modalOverlay}>
          <View style={s.modalContent}>
            <View style={s.modalHeader}>
              <Text style={s.modalTitle}>Select Listing</Text>
              <TouchableOpacity onPress={() => setShowListingPicker(false)}>
                <Text style={s.modalClose}>{'✕'}</Text>
              </TouchableOpacity>
            </View>

            <FlatList
              data={[{ id: null, title: 'All Listings' } as any, ...listings]}
              keyExtractor={(item) => item.id ?? 'all'}
              renderItem={({ item }) => (
                <TouchableOpacity
                  style={[
                    s.pickerItem,
                    (item.id ?? null) === selectedListingId && s.pickerItemActive,
                  ]}
                  onPress={() => {
                    setSelectedListingId(item.id ?? null);
                    setShowListingPicker(false);
                  }}
                >
                  <Text
                    style={[
                      s.pickerItemText,
                      (item.id ?? null) === selectedListingId && s.pickerItemTextActive,
                    ]}
                    numberOfLines={1}
                  >
                    {item.title}
                  </Text>
                  {(item.id ?? null) === selectedListingId && (
                    <Text style={s.pickerCheck}>{'✓'}</Text>
                  )}
                </TouchableOpacity>
              )}
            />
          </View>
        </View>
      </Modal>
    </View>
  );
}

/* ── KPI Card ──────────────────────────────────────────────── */

function KpiCard({ label, value, color }: { label: string; value: string; color: string }) {
  return (
    <View style={s.kpiCard}>
      <Text style={s.kpiLabel}>{label}</Text>
      <Text style={[s.kpiValue, { color }]}>{value}</Text>
    </View>
  );
}

/* ── Metric Pill ───────────────────────────────────────────── */

function MetricPill({ label, value }: { label: string; value: string }) {
  return (
    <View style={s.metricPill}>
      <Text style={s.metricPillLabel}>{label}</Text>
      <Text style={s.metricPillValue}>{value}</Text>
    </View>
  );
}

/* ── Booking Badge ─────────────────────────────────────────── */

function BookingBadge({
  label,
  count,
  bg,
  color,
}: {
  label: string;
  count: number;
  bg: string;
  color: string;
}) {
  return (
    <View style={[s.bookingBadge, { backgroundColor: bg }]}>
      <Text style={[s.bookingBadgeCount, { color }]}>{count}</Text>
      <Text style={[s.bookingBadgeLabel, { color }]}>{label}</Text>
    </View>
  );
}

/* ── Styles ────────────────────────────────────────────────── */

const s = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },

  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
    padding: 24,
  },

  /* Header */
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingTop: 56,
    paddingHorizontal: 16,
    paddingBottom: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  backBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#f3f4f6',
    justifyContent: 'center',
    alignItems: 'center',
  },
  backIcon: {
    fontSize: 20,
    color: '#111827',
  },
  headerTitle: {
    flex: 1,
    textAlign: 'center',
    fontSize: 18,
    fontWeight: '700',
    color: '#111827',
  },
  headerSpacer: {
    width: 40,
  },

  /* Body */
  body: {
    flex: 1,
  },
  bodyContent: {
    padding: 16,
    paddingBottom: 40,
  },

  /* Filter chips */
  filterRow: {
    marginBottom: 16,
  },
  filterChip: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: '#f3f4f6',
    marginRight: 8,
  },
  filterChipActive: {
    backgroundColor: '#f97316',
  },
  filterChipText: {
    fontSize: 13,
    fontWeight: '500',
    color: '#6b7280',
  },
  filterChipTextActive: {
    color: '#fff',
  },

  /* Listing selector */
  listingSelector: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#fafafa',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#f3f4f6',
    paddingHorizontal: 16,
    paddingVertical: 14,
    marginBottom: 20,
  },
  listingSelectorLabel: {
    fontSize: 13,
    color: '#6b7280',
    fontWeight: '500',
  },
  listingSelectorValue: {
    flex: 1,
    textAlign: 'right',
    fontSize: 14,
    fontWeight: '600',
    color: '#111827',
    marginLeft: 12,
  },

  /* Section */
  sectionTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#111827',
    marginBottom: 12,
    marginTop: 8,
  },

  /* KPI Grid */
  kpiGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
    marginBottom: 20,
  },
  kpiCard: {
    width: '47%' as any,
    backgroundColor: '#fafafa',
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: '#f3f4f6',
  },
  kpiLabel: {
    fontSize: 12,
    color: '#6b7280',
    marginBottom: 8,
    fontWeight: '500',
  },
  kpiValue: {
    fontSize: 20,
    fontWeight: '700',
  },

  /* Card */
  card: {
    backgroundColor: '#fafafa',
    borderRadius: 12,
    padding: 16,
    marginBottom: 20,
    borderWidth: 1,
    borderColor: '#f3f4f6',
  },

  /* Occupancy bars */
  occupancyRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
  },
  occupancyMonth: {
    width: 50,
    fontSize: 12,
    fontWeight: '600',
    color: '#374151',
  },
  barContainer: {
    flex: 1,
    height: 16,
    backgroundColor: '#e5e7eb',
    borderRadius: 8,
    marginHorizontal: 8,
    overflow: 'hidden',
  },
  bar: {
    height: '100%',
    backgroundColor: '#f97316',
    borderRadius: 8,
  },
  occupancyPercent: {
    width: 38,
    fontSize: 12,
    fontWeight: '700',
    color: '#111827',
    textAlign: 'right',
  },

  /* Divider */
  divider: {
    height: 1,
    backgroundColor: '#e5e7eb',
    marginVertical: 14,
  },

  /* Metrics row */
  metricsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  metricPill: {
    alignItems: 'center',
    flex: 1,
  },
  metricPillLabel: {
    fontSize: 11,
    color: '#6b7280',
    fontWeight: '500',
    marginBottom: 4,
  },
  metricPillValue: {
    fontSize: 14,
    fontWeight: '700',
    color: '#111827',
  },

  /* Booking stats */
  bookingStatsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 10,
  },
  bookingBadge: {
    flex: 1,
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
  },
  bookingBadgeCount: {
    fontSize: 22,
    fontWeight: '700',
  },
  bookingBadgeLabel: {
    fontSize: 11,
    fontWeight: '600',
    marginTop: 4,
  },

  /* Cancellation */
  cancellationRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  cancellationLabel: {
    fontSize: 14,
    color: '#6b7280',
    fontWeight: '500',
  },
  cancellationValue: {
    fontSize: 18,
    fontWeight: '700',
  },

  /* Empty state */
  emptyBlock: {
    alignItems: 'center',
    paddingVertical: 40,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#111827',
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 14,
    color: '#6b7280',
    textAlign: 'center',
  },

  /* Primary button */
  primaryBtn: {
    marginTop: 20,
    backgroundColor: '#f97316',
    borderRadius: 12,
    paddingVertical: 14,
    paddingHorizontal: 32,
  },
  primaryBtnText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '700',
  },

  /* Modal */
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    maxHeight: '60%',
    paddingBottom: 40,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 20,
    paddingBottom: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
  },
  modalTitle: {
    fontSize: 17,
    fontWeight: '700',
    color: '#111827',
  },
  modalClose: {
    fontSize: 20,
    color: '#6b7280',
    padding: 4,
  },

  /* Picker items */
  pickerItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: '#f9fafb',
  },
  pickerItemActive: {
    backgroundColor: '#fff7ed',
  },
  pickerItemText: {
    fontSize: 15,
    color: '#374151',
    flex: 1,
  },
  pickerItemTextActive: {
    color: '#f97316',
    fontWeight: '600',
  },
  pickerCheck: {
    fontSize: 16,
    color: '#f97316',
    fontWeight: '700',
    marginLeft: 8,
  },
});
