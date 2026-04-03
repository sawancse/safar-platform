import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  RefreshControl,
  Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ── Types ─────────────────────────────────────────────────────── */

interface Tenancy {
  id: string;
  tenancyRef: string;
  status: 'ACTIVE' | 'NOTICE_PERIOD' | 'VACATED';
  moveInDate: string;
  moveOutDate?: string;
  monthlyRentPaise: number;
  securityDepositPaise: number;
}

interface Agreement {
  status: string;
  agreementNumber: string;
  pdfUrl?: string;
}

interface Invoice {
  id: string;
  invoiceNumber: string;
  grandTotalPaise: number;
  dueDate: string;
  status: 'PAID' | 'OVERDUE' | 'GENERATED' | 'CANCELLED';
}

interface Subscription {
  status: string;
  razorpaySubscriptionId?: string;
}

interface TenantDashboard {
  tenancy: Tenancy;
  agreement: Agreement;
  currentInvoice: Invoice | null;
  totalPaidPaise: number;
  outstandingPaise: number;
  openMaintenanceRequests: number;
  subscription: Subscription;
}

/* ── Constants ─────────────────────────────────────────────────── */

const TENANCY_STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  ACTIVE:        { bg: '#dcfce7', text: '#14532d' },
  NOTICE_PERIOD: { bg: '#fff7ed', text: '#9a3412' },
  VACATED:       { bg: '#f3f4f6', text: '#374151' },
};

const INVOICE_STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  PAID:      { bg: '#dcfce7', text: '#14532d' },
  OVERDUE:   { bg: '#fee2e2', text: '#991b1b' },
  GENERATED: { bg: '#dbeafe', text: '#1e3a8a' },
  CANCELLED: { bg: '#f3f4f6', text: '#374151' },
};

function formatPaise(p: number): string {
  return '\u20B9' + (p / 100).toLocaleString('en-IN');
}

function formatDate(iso: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

/* ── Component ─────────────────────────────────────────────────── */

export default function PgDashboardScreen() {
  const router = useRouter();

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [authed, setAuthed] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [dashboard, setDashboard] = useState<TenantDashboard | null>(null);

  /* ── Data loading ──────────────────────────────────────── */

  const loadDashboard = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) {
      setLoading(false);
      return;
    }
    setAuthed(true);
    try {
      const data = await api.getTenantDashboard(token);
      setDashboard(data);
    } catch (e: any) {
      Alert.alert('Error', e?.message || 'Failed to load dashboard');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    loadDashboard();
  }, [loadDashboard]);

  /* ── Sign agreement action ──────────────────────────────── */

  const handleSignAgreement = async () => {
    const token = await getAccessToken();
    if (!token || !dashboard) return;
    setActionLoading(true);
    try {
      await api.tenantSignAgreement(token);
      Alert.alert('Success', 'Agreement signed successfully');
      loadDashboard();
    } catch (e: any) {
      Alert.alert('Error', e?.message || 'Failed to sign agreement');
    } finally {
      setActionLoading(false);
    }
  };

  /* ── Render helpers ─────────────────────────────────────── */

  const renderStatusBadge = (
    status: string,
    colorMap: Record<string, { bg: string; text: string }>,
  ) => {
    const colors = colorMap[status] || { bg: '#f3f4f6', text: '#374151' };
    return (
      <View style={[styles.badge, { backgroundColor: colors.bg }]}>
        <Text style={[styles.badgeText, { color: colors.text }]}>
          {status.replace(/_/g, ' ')}
        </Text>
      </View>
    );
  };

  /* ── Loading / Auth gates ───────────────────────────────── */

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#F97316" />
        <Text style={styles.loadingText}>Loading dashboard...</Text>
      </View>
    );
  }

  if (!authed) {
    return (
      <View style={styles.center}>
        <Text style={styles.emptyTitle}>Sign in required</Text>
        <Text style={styles.emptySubtitle}>Please sign in to view your PG dashboard.</Text>
        <TouchableOpacity style={styles.primaryBtn} onPress={() => router.push('/auth')}>
          <Text style={styles.primaryBtnText}>Sign In</Text>
        </TouchableOpacity>
      </View>
    );
  }

  if (!dashboard) {
    return (
      <View style={styles.center}>
        <Text style={styles.emptyTitle}>No Active Tenancy</Text>
        <Text style={styles.emptySubtitle}>You don't have an active PG tenancy.</Text>
      </View>
    );
  }

  const { tenancy, agreement, currentInvoice, totalPaidPaise, outstandingPaise, openMaintenanceRequests, subscription } = dashboard;

  /* ── Main render ────────────────────────────────────────── */

  return (
    <ScrollView
      style={styles.container}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#F97316" />}
    >
      {/* ── Header ──────────────────────────────────────────── */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>My PG</Text>
        <View style={[styles.badge, { backgroundColor: '#fff7ed' }]}>
          <Text style={[styles.badgeText, { color: '#9a3412' }]}>{tenancy.tenancyRef}</Text>
        </View>
      </View>

      {/* ── Status Card ─────────────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Tenancy Status</Text>
        <View style={styles.statusRow}>
          {renderStatusBadge(tenancy.status, TENANCY_STATUS_COLORS)}
        </View>
        <View style={styles.detailRow}>
          <Text style={styles.detailLabel}>Move-in Date</Text>
          <Text style={styles.detailValue}>{formatDate(tenancy.moveInDate)}</Text>
        </View>
        {tenancy.moveOutDate && (
          <View style={styles.detailRow}>
            <Text style={styles.detailLabel}>Move-out Date</Text>
            <Text style={styles.detailValue}>{formatDate(tenancy.moveOutDate)}</Text>
          </View>
        )}
        <View style={styles.detailRow}>
          <Text style={styles.detailLabel}>Subscription</Text>
          {renderStatusBadge(subscription.status, {
            ACTIVE: { bg: '#dcfce7', text: '#14532d' },
            PENDING: { bg: '#fef9c3', text: '#854d0e' },
            CANCELLED: { bg: '#fee2e2', text: '#991b1b' },
          })}
        </View>
      </View>

      {/* ── Quick Stats (2x2) ───────────────────────────────── */}
      <View style={styles.statsGrid}>
        <View style={styles.statCard}>
          <Text style={styles.statLabel}>Monthly Rent</Text>
          <Text style={styles.statValue}>{formatPaise(tenancy.monthlyRentPaise)}</Text>
        </View>
        <View style={[styles.statCard, outstandingPaise > 0 && styles.statCardAlert]}>
          <Text style={styles.statLabel}>Outstanding</Text>
          <Text style={[styles.statValue, outstandingPaise > 0 && { color: '#dc2626' }]}>
            {formatPaise(outstandingPaise)}
          </Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statLabel}>Total Paid</Text>
          <Text style={[styles.statValue, { color: '#16a34a' }]}>{formatPaise(totalPaidPaise)}</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statLabel}>Security Deposit</Text>
          <Text style={styles.statValue}>{formatPaise(tenancy.securityDepositPaise)}</Text>
        </View>
      </View>

      {/* ── Agreement Section ───────────────────────────────── */}
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Rental Agreement</Text>
        <View style={styles.detailRow}>
          <Text style={styles.detailLabel}>Status</Text>
          {renderStatusBadge(agreement.status, {
            SIGNED:              { bg: '#dcfce7', text: '#14532d' },
            PENDING_TENANT_SIGN: { bg: '#fef9c3', text: '#854d0e' },
            PENDING_HOST_SIGN:   { bg: '#dbeafe', text: '#1e3a8a' },
            DRAFT:               { bg: '#f3f4f6', text: '#374151' },
          })}
        </View>
        {agreement.agreementNumber && (
          <View style={styles.detailRow}>
            <Text style={styles.detailLabel}>Agreement #</Text>
            <Text style={styles.detailValue}>{agreement.agreementNumber}</Text>
          </View>
        )}
        {agreement.status === 'PENDING_TENANT_SIGN' && (
          <TouchableOpacity
            style={[styles.primaryBtn, actionLoading && { opacity: 0.6 }]}
            onPress={handleSignAgreement}
            disabled={actionLoading}
          >
            {actionLoading ? (
              <ActivityIndicator size="small" color="#fff" />
            ) : (
              <Text style={styles.primaryBtnText}>Sign Agreement</Text>
            )}
          </TouchableOpacity>
        )}
      </View>

      {/* ── Current Invoice ─────────────────────────────────── */}
      {currentInvoice && (
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Current Invoice</Text>
          <View style={styles.detailRow}>
            <Text style={styles.detailLabel}>Invoice #</Text>
            <Text style={styles.detailValue}>{currentInvoice.invoiceNumber}</Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.detailLabel}>Amount</Text>
            <Text style={[styles.detailValue, { fontWeight: '700' }]}>
              {formatPaise(currentInvoice.grandTotalPaise)}
            </Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.detailLabel}>Due Date</Text>
            <Text style={styles.detailValue}>{formatDate(currentInvoice.dueDate)}</Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.detailLabel}>Status</Text>
            {renderStatusBadge(currentInvoice.status, INVOICE_STATUS_COLORS)}
          </View>
        </View>
      )}

      {/* ── Maintenance badge ───────────────────────────────── */}
      {openMaintenanceRequests > 0 && (
        <View style={styles.maintenanceBanner}>
          <Text style={styles.maintenanceBannerText}>
            {openMaintenanceRequests} open maintenance request{openMaintenanceRequests > 1 ? 's' : ''}
          </Text>
        </View>
      )}

      {/* ── Quick Actions ───────────────────────────────────── */}
      <Text style={styles.sectionTitle}>Quick Actions</Text>
      <View style={styles.actionsGrid}>
        <TouchableOpacity
          style={styles.actionBtn}
          onPress={() => router.push('/pg-maintenance')}
        >
          <Text style={styles.actionIcon}>🔧</Text>
          <Text style={styles.actionLabel}>Maintenance{'\n'}Requests</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.actionBtn}
          onPress={() => router.push('/pg-utility-readings')}
        >
          <Text style={styles.actionIcon}>⚡</Text>
          <Text style={styles.actionLabel}>Utility{'\n'}Readings</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.actionBtn}
          onPress={() => router.push('/pg-invoices')}
        >
          <Text style={styles.actionIcon}>🧾</Text>
          <Text style={styles.actionLabel}>Invoices</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.actionBtn}
          onPress={() => router.push('/pg-give-notice')}
        >
          <Text style={styles.actionIcon}>📋</Text>
          <Text style={styles.actionLabel}>Give{'\n'}Notice</Text>
        </TouchableOpacity>
      </View>

      <View style={{ height: 40 }} />
    </ScrollView>
  );
}

/* ── Styles ──────────────────────────────────────────────────── */

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9fafb',
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
    backgroundColor: '#f9fafb',
  },
  loadingText: {
    marginTop: 12,
    fontSize: 14,
    color: '#6b7280',
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
    marginBottom: 20,
  },

  /* Header */
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingTop: 56,
    paddingBottom: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: '800',
    color: '#111827',
  },

  /* Badge */
  badge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  badgeText: {
    fontSize: 12,
    fontWeight: '600',
  },

  /* Card */
  card: {
    backgroundColor: '#fff',
    marginHorizontal: 16,
    marginTop: 16,
    borderRadius: 12,
    padding: 16,
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowOffset: { width: 0, height: 2 },
    shadowRadius: 8,
    elevation: 2,
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#111827',
    marginBottom: 12,
  },
  statusRow: {
    flexDirection: 'row',
    marginBottom: 12,
  },
  detailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
    borderTopWidth: 1,
    borderTopColor: '#f3f4f6',
  },
  detailLabel: {
    fontSize: 14,
    color: '#6b7280',
  },
  detailValue: {
    fontSize: 14,
    fontWeight: '600',
    color: '#111827',
  },

  /* Stats grid */
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: 12,
    marginTop: 16,
  },
  statCard: {
    width: '47%',
    backgroundColor: '#fff',
    margin: '1.5%',
    borderRadius: 12,
    padding: 16,
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowOffset: { width: 0, height: 2 },
    shadowRadius: 8,
    elevation: 2,
  },
  statCardAlert: {
    borderWidth: 1,
    borderColor: '#fca5a5',
  },
  statLabel: {
    fontSize: 12,
    color: '#6b7280',
    marginBottom: 4,
  },
  statValue: {
    fontSize: 18,
    fontWeight: '700',
    color: '#111827',
  },

  /* Section title */
  sectionTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#111827',
    paddingHorizontal: 16,
    marginTop: 24,
    marginBottom: 12,
  },

  /* Actions grid */
  actionsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: 12,
  },
  actionBtn: {
    width: '47%',
    margin: '1.5%',
    backgroundColor: '#fff',
    borderRadius: 12,
    paddingVertical: 20,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOpacity: 0.05,
    shadowOffset: { width: 0, height: 2 },
    shadowRadius: 8,
    elevation: 2,
  },
  actionIcon: {
    fontSize: 28,
    marginBottom: 8,
  },
  actionLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: '#374151',
    textAlign: 'center',
  },

  /* Maintenance banner */
  maintenanceBanner: {
    marginHorizontal: 16,
    marginTop: 16,
    backgroundColor: '#fff7ed',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#fed7aa',
    padding: 12,
    alignItems: 'center',
  },
  maintenanceBannerText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#9a3412',
  },

  /* Buttons */
  primaryBtn: {
    backgroundColor: '#F97316',
    borderRadius: 10,
    paddingVertical: 12,
    alignItems: 'center',
    marginTop: 12,
  },
  primaryBtnText: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '700',
  },
});
