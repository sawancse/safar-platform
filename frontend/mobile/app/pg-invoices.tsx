import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  RefreshControl,
  FlatList,
  Alert,
  Modal,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ── Types ─────────────────────────────────────────────────── */

interface Invoice {
  id: string;
  invoiceNumber: string;
  billingMonth: number;
  billingYear: number;
  rentPaise: number;
  packagesPaise: number;
  electricityPaise: number;
  waterPaise: number;
  gstPaise: number;
  latePenaltyPaise: number;
  grandTotalPaise: number;
  status: 'PAID' | 'GENERATED' | 'OVERDUE' | 'SENT';
  dueDate: string;
  paidDate: string | null;
}

/* ── Constants ─────────────────────────────────────────────── */

const STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  PAID:      { bg: '#dcfce7', text: '#14532d' },
  GENERATED: { bg: '#dbeafe', text: '#1e3a8a' },
  OVERDUE:   { bg: '#fee2e2', text: '#7f1d1d' },
  SENT:      { bg: '#f3e8ff', text: '#581c87' },
};

const MONTH_NAMES = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

/* ── Helpers ───────────────────────────────────────────────── */

const formatPaise = (p: number) => '₹' + (p / 100).toLocaleString('en-IN');

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

/* ── Component ─────────────────────────────────────────────── */

export default function PGInvoicesScreen() {
  const router = useRouter();

  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchInvoices = useCallback(async (isRefresh = false) => {
    try {
      if (isRefresh) setRefreshing(true);
      else setLoading(true);
      setError(null);

      const token = await getAccessToken();
      if (!token) { router.replace('/auth'); return; }

      const dashboard = await api.getTenantDashboard(token);
      const tenancyId = dashboard.data?.id || dashboard.data?.tenancyId;
      if (!tenancyId) { setError('No active tenancy found'); return; }

      const res = await api.getTenancyInvoices(tenancyId, token);
      setInvoices(res.data?.content || []);
    } catch (e: any) {
      setError(e?.response?.data?.detail || e.message || 'Failed to load invoices');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [router]);

  useEffect(() => { fetchInvoices(); }, [fetchInvoices]);

  /* ── Derived ─────────────────────────────────── */

  const outstandingTotal = invoices
    .filter((i) => i.status !== 'PAID')
    .reduce((sum, i) => sum + i.grandTotalPaise, 0);

  const paidTotal = invoices
    .filter((i) => i.status === 'PAID')
    .reduce((sum, i) => sum + i.grandTotalPaise, 0);

  /* ── Renders ─────────────────────────────────── */

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#F97316" />
        <Text style={styles.loadingText}>Loading invoices...</Text>
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>{error}</Text>
        <TouchableOpacity style={styles.retryBtn} onPress={() => fetchInvoices()}>
          <Text style={styles.retryText}>Retry</Text>
        </TouchableOpacity>
      </View>
    );
  }

  const renderInvoice = ({ item }: { item: Invoice }) => {
    const sc = STATUS_COLORS[item.status] || STATUS_COLORS.GENERATED;
    return (
      <View style={styles.card}>
        {/* Header */}
        <View style={styles.cardHeader}>
          <View>
            <Text style={styles.cardMonth}>
              {MONTH_NAMES[item.billingMonth - 1]} {item.billingYear}
            </Text>
            <Text style={styles.invoiceNum}>{item.invoiceNumber}</Text>
          </View>
          <View style={[styles.badge, { backgroundColor: sc.bg }]}>
            <Text style={[styles.badgeText, { color: sc.text }]}>{item.status}</Text>
          </View>
        </View>

        {/* Breakdown */}
        <View style={styles.breakdown}>
          <Row label="Rent" value={item.rentPaise} />
          {item.packagesPaise > 0 && <Row label="Packages" value={item.packagesPaise} />}
          {item.electricityPaise > 0 && <Row label="Electricity" value={item.electricityPaise} />}
          {item.waterPaise > 0 && <Row label="Water" value={item.waterPaise} />}
          {item.gstPaise > 0 && <Row label="GST" value={item.gstPaise} />}
          {item.latePenaltyPaise > 0 && (
            <Row label="Late Penalty" value={item.latePenaltyPaise} highlight />
          )}
          <View style={styles.divider} />
          <View style={styles.row}>
            <Text style={styles.totalLabel}>Total</Text>
            <Text style={styles.totalValue}>{formatPaise(item.grandTotalPaise)}</Text>
          </View>
        </View>

        {/* Footer */}
        <View style={styles.cardFooter}>
          <Text style={styles.footerText}>Due: {formatDate(item.dueDate)}</Text>
          {item.paidDate && (
            <Text style={styles.footerText}>Paid: {formatDate(item.paidDate)}</Text>
          )}
        </View>
      </View>
    );
  };

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>{'<'} Back</Text>
        </TouchableOpacity>
        <Text style={styles.title}>PG Invoices</Text>
      </View>

      {/* Summary */}
      <View style={styles.summaryRow}>
        <View style={[styles.summaryCard, { backgroundColor: '#fef2f2' }]}>
          <Text style={styles.summaryLabel}>Outstanding</Text>
          <Text style={[styles.summaryAmount, { color: '#dc2626' }]}>
            {formatPaise(outstandingTotal)}
          </Text>
        </View>
        <View style={[styles.summaryCard, { backgroundColor: '#f0fdf4' }]}>
          <Text style={styles.summaryLabel}>Paid</Text>
          <Text style={[styles.summaryAmount, { color: '#16a34a' }]}>
            {formatPaise(paidTotal)}
          </Text>
        </View>
      </View>

      {/* Invoice List */}
      {invoices.length === 0 ? (
        <View style={styles.emptyState}>
          <Text style={styles.emptyIcon}>📄</Text>
          <Text style={styles.emptyTitle}>No Invoices Yet</Text>
          <Text style={styles.emptyDesc}>
            Your monthly invoices will appear here once generated by your host.
          </Text>
        </View>
      ) : (
        <FlatList
          data={invoices}
          keyExtractor={(item) => item.id}
          renderItem={renderInvoice}
          contentContainerStyle={styles.list}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={() => fetchInvoices(true)}
              colors={['#F97316']}
            />
          }
        />
      )}
    </View>
  );
}

/* ── Row Sub-component ─────────────────────────────────────── */

function Row({
  label,
  value,
  highlight,
}: {
  label: string;
  value: number;
  highlight?: boolean;
}) {
  return (
    <View style={styles.row}>
      <Text style={[styles.rowLabel, highlight && { color: '#dc2626' }]}>{label}</Text>
      <Text style={[styles.rowValue, highlight && { color: '#dc2626' }]}>
        {formatPaise(value)}
      </Text>
    </View>
  );
}

/* ── Styles ────────────────────────────────────────────────── */

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f9fafb' },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 24 },
  loadingText: { marginTop: 12, color: '#6b7280', fontSize: 14 },
  errorText: { color: '#dc2626', fontSize: 15, textAlign: 'center', marginBottom: 16 },
  retryBtn: {
    backgroundColor: '#F97316',
    paddingHorizontal: 24,
    paddingVertical: 10,
    borderRadius: 8,
  },
  retryText: { color: '#fff', fontWeight: '600', fontSize: 15 },

  /* Header */
  header: {
    backgroundColor: '#fff',
    paddingTop: 56,
    paddingBottom: 16,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  backBtn: { marginBottom: 8 },
  backText: { color: '#F97316', fontSize: 15, fontWeight: '500' },
  title: { fontSize: 22, fontWeight: '700', color: '#111827' },

  /* Summary */
  summaryRow: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 12,
  },
  summaryCard: {
    flex: 1,
    borderRadius: 12,
    padding: 14,
  },
  summaryLabel: { fontSize: 13, color: '#6b7280', marginBottom: 4 },
  summaryAmount: { fontSize: 20, fontWeight: '700' },

  /* List */
  list: { paddingHorizontal: 16, paddingBottom: 24 },

  /* Card */
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowOffset: { width: 0, height: 2 },
    shadowRadius: 8,
    elevation: 2,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 12,
  },
  cardMonth: { fontSize: 17, fontWeight: '700', color: '#111827' },
  invoiceNum: { fontSize: 12, color: '#9ca3af', marginTop: 2 },
  badge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 12 },
  badgeText: { fontSize: 11, fontWeight: '700', textTransform: 'uppercase' },

  /* Breakdown */
  breakdown: {
    backgroundColor: '#f9fafb',
    borderRadius: 8,
    padding: 12,
    marginBottom: 12,
  },
  row: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 4 },
  rowLabel: { fontSize: 13, color: '#6b7280' },
  rowValue: { fontSize: 13, color: '#374151', fontWeight: '500' },
  divider: {
    height: 1,
    backgroundColor: '#e5e7eb',
    marginVertical: 6,
  },
  totalLabel: { fontSize: 14, fontWeight: '700', color: '#111827' },
  totalValue: { fontSize: 14, fontWeight: '700', color: '#F97316' },

  /* Footer */
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  footerText: { fontSize: 12, color: '#9ca3af' },

  /* Empty */
  emptyState: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 32 },
  emptyIcon: { fontSize: 48, marginBottom: 12 },
  emptyTitle: { fontSize: 18, fontWeight: '700', color: '#111827', marginBottom: 8 },
  emptyDesc: { fontSize: 14, color: '#6b7280', textAlign: 'center', lineHeight: 20 },
});
