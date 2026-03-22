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

/* ── Types ─────────────────────────────────────────────────── */

interface EarningsSummary {
  totalRevenuePaise: number;
  totalPayoutsPaise: number;
  commissionPaidPaise: number;
  pendingPayoutsPaise: number;
}

interface GstInvoice {
  id: string;
  invoiceNumber: string;
  date: string;
  grossAmountPaise: number;
  gstAmountPaise: number;
  netAmountPaise: number;
  status: string;
}

interface TdsReport {
  financialYear: string;
  totalEarningsPaise: number;
  tdsDeductedPaise: number;
  tdsRate: number;
}

interface PnlSummary {
  revenuePaise: number;
  expensesPaise: number;
  netProfitPaise: number;
}

/* ── Constants ─────────────────────────────────────────────── */

type TabKey = 'earnings' | 'invoices' | 'tax';
const TABS: { key: TabKey; label: string }[] = [
  { key: 'earnings', label: 'Earnings' },
  { key: 'invoices', label: 'Invoices' },
  { key: 'tax', label: 'Tax Reports' },
];

type DateRange = 'this_month' | 'last_month' | 'last_3_months' | 'custom';
const DATE_RANGES: { key: DateRange; label: string }[] = [
  { key: 'this_month', label: 'This Month' },
  { key: 'last_month', label: 'Last Month' },
  { key: 'last_3_months', label: 'Last 3 Months' },
  { key: 'custom', label: 'Custom' },
];

const INVOICE_STATUS: Record<string, { bg: string; text: string }> = {
  PAID:      { bg: '#dcfce7', text: '#14532d' },
  PENDING:   { bg: '#fef9c3', text: '#854d0e' },
  OVERDUE:   { bg: '#fee2e2', text: '#7f1d1d' },
  CANCELLED: { bg: '#f3f4f6', text: '#374151' },
};

const FY_OPTIONS = ['2025-26', '2024-25', '2023-24'];

/* ── Helpers ───────────────────────────────────────────────── */

function formatAmount(paise: number): string {
  return '\u20B9' + (paise / 100).toLocaleString('en-IN');
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
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

export default function HostEarningsScreen() {
  const router = useRouter();

  const [tab, setTab] = useState<TabKey>('earnings');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [authed, setAuthed] = useState(false);

  // Earnings state
  const [dateRange, setDateRange] = useState<DateRange>('this_month');
  const [earnings, setEarnings] = useState<EarningsSummary | null>(null);

  // Invoices state
  const [invoices, setInvoices] = useState<GstInvoice[]>([]);
  const [invoicePage, setInvoicePage] = useState(0);
  const [hasMoreInvoices, setHasMoreInvoices] = useState(true);

  // Tax state
  const [financialYear, setFinancialYear] = useState(FY_OPTIONS[0]);
  const [tdsReport, setTdsReport] = useState<TdsReport | null>(null);
  const [pnl, setPnl] = useState<PnlSummary | null>(null);

  /* ── Data loaders ───────────────────────────────────── */

  const loadEarnings = useCallback(async (silent = false) => {
    const token = await getAccessToken();
    if (!token) { setLoading(false); return; }
    setAuthed(true);
    if (!silent) setLoading(true);
    try {
      const range = getDateRange(dateRange);
      const data = await api.getHostEarnings(token, range);
      setEarnings(data);
    } catch {
      setEarnings(null);
    } finally {
      setLoading(false);
    }
  }, [dateRange]);

  const loadInvoices = useCallback(async (page = 0, append = false) => {
    const token = await getAccessToken();
    if (!token) return;
    try {
      const data = await api.getGstInvoices(token, page);
      const list = Array.isArray(data?.content) ? data.content : Array.isArray(data) ? data : [];
      if (append) {
        setInvoices(prev => [...prev, ...list]);
      } else {
        setInvoices(list);
      }
      setHasMoreInvoices(list.length >= 10);
      setInvoicePage(page);
    } catch {
      if (!append) setInvoices([]);
    }
  }, []);

  const loadTax = useCallback(async () => {
    const token = await getAccessToken();
    if (!token) return;
    try {
      const fyParts = financialYear.split('-');
      const from = `${fyParts[0]}-04-01T00:00:00.000Z`;
      const to = `20${fyParts[1]}-03-31T23:59:59.000Z`;
      const [tds, pnlData] = await Promise.all([
        api.getTdsReport(token, financialYear),
        api.getPnl(token, { from, to }),
      ]);
      setTdsReport(tds);
      setPnl(pnlData);
    } catch {
      setTdsReport(null);
      setPnl(null);
    }
  }, [financialYear]);

  /* ── Effects ────────────────────────────────────────── */

  useEffect(() => {
    if (tab === 'earnings') loadEarnings();
    else if (tab === 'invoices') loadInvoices(0);
    else loadTax();
  }, [tab, loadEarnings, loadInvoices, loadTax]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    if (tab === 'earnings') await loadEarnings(true);
    else if (tab === 'invoices') await loadInvoices(0);
    else await loadTax();
    setRefreshing(false);
  }, [tab, loadEarnings, loadInvoices, loadTax]);

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
        <Text style={s.emptySubtitle}>Please sign in to view your earnings.</Text>
        <TouchableOpacity style={s.primaryBtn} onPress={() => router.push('/auth')}>
          <Text style={s.primaryBtnText}>Sign In</Text>
        </TouchableOpacity>
      </View>
    );
  }

  /* ── Render ─────────────────────────────────────────── */

  return (
    <View style={s.container}>
      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
          <Text style={s.backIcon}>{'←'}</Text>
        </TouchableOpacity>
        <Text style={s.headerTitle}>Earnings</Text>
        <View style={s.headerSpacer} />
      </View>

      {/* Tabs */}
      <View style={s.tabBar}>
        {TABS.map(t => (
          <TouchableOpacity
            key={t.key}
            style={[s.tabItem, tab === t.key && s.tabItemActive]}
            onPress={() => setTab(t.key)}
          >
            <Text style={[s.tabLabel, tab === t.key && s.tabLabelActive]}>{t.label}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <ScrollView
        style={s.body}
        contentContainerStyle={s.bodyContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#f97316" />}
      >
        {tab === 'earnings' && renderEarnings()}
        {tab === 'invoices' && renderInvoices()}
        {tab === 'tax' && renderTax()}
      </ScrollView>
    </View>
  );

  /* ── Earnings Tab ───────────────────────────────────── */

  function renderEarnings() {
    return (
      <View>
        {/* Date range filter */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={s.filterRow}>
          {DATE_RANGES.map(r => (
            <TouchableOpacity
              key={r.key}
              style={[s.filterChip, dateRange === r.key && s.filterChipActive]}
              onPress={() => {
                if (r.key === 'custom') {
                  Alert.alert('Custom Range', 'Custom date picker coming soon.');
                  return;
                }
                setDateRange(r.key);
              }}
            >
              <Text style={[s.filterChipText, dateRange === r.key && s.filterChipTextActive]}>
                {r.label}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        {/* Summary cards */}
        {earnings ? (
          <View style={s.cardGrid}>
            <SummaryCard label="Total Revenue" amount={earnings.totalRevenuePaise} color="#059669" />
            <SummaryCard label="Total Payouts" amount={earnings.totalPayoutsPaise} color="#2563eb" />
            <SummaryCard label="Commission Paid" amount={earnings.commissionPaidPaise} color="#f97316" />
            <SummaryCard label="Pending Payouts" amount={earnings.pendingPayoutsPaise} color="#dc2626" />
          </View>
        ) : (
          <View style={s.emptyBlock}>
            <Text style={s.emptySubtitle}>No earnings data for this period.</Text>
          </View>
        )}
      </View>
    );
  }

  /* ── Invoices Tab ───────────────────────────────────── */

  function renderInvoices() {
    if (invoices.length === 0) {
      return (
        <View style={s.emptyBlock}>
          <Text style={s.emptyTitle}>No Invoices</Text>
          <Text style={s.emptySubtitle}>GST invoices will appear here once generated.</Text>
        </View>
      );
    }

    return (
      <View>
        {invoices.map(inv => {
          const statusStyle = INVOICE_STATUS[inv.status] || INVOICE_STATUS.PENDING;
          return (
            <View key={inv.id} style={s.invoiceCard}>
              <View style={s.invoiceHeader}>
                <Text style={s.invoiceNumber}>{inv.invoiceNumber}</Text>
                <View style={[s.statusBadge, { backgroundColor: statusStyle.bg }]}>
                  <Text style={[s.statusText, { color: statusStyle.text }]}>{inv.status}</Text>
                </View>
              </View>
              <Text style={s.invoiceDate}>{formatDate(inv.date)}</Text>
              <View style={s.invoiceRow}>
                <Text style={s.invoiceLabel}>Gross Amount</Text>
                <Text style={s.invoiceValue}>{formatAmount(inv.grossAmountPaise)}</Text>
              </View>
              <View style={s.invoiceRow}>
                <Text style={s.invoiceLabel}>GST</Text>
                <Text style={s.invoiceValue}>{formatAmount(inv.gstAmountPaise)}</Text>
              </View>
              <View style={[s.invoiceRow, s.invoiceRowNet]}>
                <Text style={s.invoiceLabelBold}>Net Amount</Text>
                <Text style={s.invoiceValueBold}>{formatAmount(inv.netAmountPaise)}</Text>
              </View>
            </View>
          );
        })}

        {hasMoreInvoices && (
          <TouchableOpacity
            style={s.loadMoreBtn}
            onPress={() => loadInvoices(invoicePage + 1, true)}
          >
            <Text style={s.loadMoreText}>Load More</Text>
          </TouchableOpacity>
        )}
      </View>
    );
  }

  /* ── Tax Tab ────────────────────────────────────────── */

  function renderTax() {
    return (
      <View>
        {/* Financial year selector */}
        <Text style={s.sectionTitle}>Financial Year</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={s.filterRow}>
          {FY_OPTIONS.map(fy => (
            <TouchableOpacity
              key={fy}
              style={[s.filterChip, financialYear === fy && s.filterChipActive]}
              onPress={() => setFinancialYear(fy)}
            >
              <Text style={[s.filterChipText, financialYear === fy && s.filterChipTextActive]}>
                FY {fy}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        {/* TDS Report */}
        <Text style={s.sectionTitle}>TDS Report</Text>
        {tdsReport ? (
          <View style={s.taxCard}>
            <View style={s.taxRow}>
              <Text style={s.taxLabel}>Total Earnings</Text>
              <Text style={s.taxValue}>{formatAmount(tdsReport.totalEarningsPaise)}</Text>
            </View>
            <View style={s.taxRow}>
              <Text style={s.taxLabel}>TDS Deducted</Text>
              <Text style={[s.taxValue, { color: '#dc2626' }]}>
                {formatAmount(tdsReport.tdsDeductedPaise)}
              </Text>
            </View>
            <View style={s.taxRow}>
              <Text style={s.taxLabel}>TDS Rate</Text>
              <Text style={s.taxValue}>{tdsReport.tdsRate}%</Text>
            </View>
          </View>
        ) : (
          <View style={s.emptyBlock}>
            <Text style={s.emptySubtitle}>No TDS data for FY {financialYear}.</Text>
          </View>
        )}

        {/* P&L Summary */}
        <Text style={s.sectionTitle}>Profit & Loss Summary</Text>
        {pnl ? (
          <View style={s.taxCard}>
            <View style={s.taxRow}>
              <Text style={s.taxLabel}>Revenue</Text>
              <Text style={[s.taxValue, { color: '#059669' }]}>{formatAmount(pnl.revenuePaise)}</Text>
            </View>
            <View style={s.taxRow}>
              <Text style={s.taxLabel}>Expenses</Text>
              <Text style={[s.taxValue, { color: '#dc2626' }]}>{formatAmount(pnl.expensesPaise)}</Text>
            </View>
            <View style={[s.taxRow, s.taxRowNet]}>
              <Text style={s.taxLabelBold}>Net Profit</Text>
              <Text
                style={[
                  s.taxValueBold,
                  { color: pnl.netProfitPaise >= 0 ? '#059669' : '#dc2626' },
                ]}
              >
                {formatAmount(pnl.netProfitPaise)}
              </Text>
            </View>
          </View>
        ) : (
          <View style={s.emptyBlock}>
            <Text style={s.emptySubtitle}>No P&L data for FY {financialYear}.</Text>
          </View>
        )}
      </View>
    );
  }
}

/* ── Summary Card Component ────────────────────────────────── */

function SummaryCard({ label, amount, color }: { label: string; amount: number; color: string }) {
  return (
    <View style={s.summaryCard}>
      <Text style={s.summaryLabel}>{label}</Text>
      <Text style={[s.summaryAmount, { color }]}>{formatAmount(amount)}</Text>
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

  /* Tabs */
  tabBar: {
    flexDirection: 'row',
    borderBottomWidth: 1,
    borderBottomColor: '#f3f4f6',
    backgroundColor: '#fff',
  },
  tabItem: {
    flex: 1,
    paddingVertical: 14,
    alignItems: 'center',
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  tabItemActive: {
    borderBottomColor: '#f97316',
  },
  tabLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: '#9ca3af',
  },
  tabLabelActive: {
    color: '#f97316',
    fontWeight: '700',
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

  /* Summary cards */
  cardGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  summaryCard: {
    width: '47%' as any,
    backgroundColor: '#fafafa',
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: '#f3f4f6',
  },
  summaryLabel: {
    fontSize: 12,
    color: '#6b7280',
    marginBottom: 8,
    fontWeight: '500',
  },
  summaryAmount: {
    fontSize: 20,
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

  /* Invoice card */
  invoiceCard: {
    backgroundColor: '#fafafa',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#f3f4f6',
  },
  invoiceHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  invoiceNumber: {
    fontSize: 15,
    fontWeight: '700',
    color: '#111827',
  },
  invoiceDate: {
    fontSize: 12,
    color: '#9ca3af',
    marginBottom: 12,
  },
  invoiceRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 4,
  },
  invoiceRowNet: {
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
    marginTop: 8,
    paddingTop: 8,
  },
  invoiceLabel: {
    fontSize: 13,
    color: '#6b7280',
  },
  invoiceValue: {
    fontSize: 13,
    color: '#374151',
    fontWeight: '500',
  },
  invoiceLabelBold: {
    fontSize: 14,
    color: '#111827',
    fontWeight: '700',
  },
  invoiceValueBold: {
    fontSize: 14,
    color: '#111827',
    fontWeight: '700',
  },

  /* Status badge */
  statusBadge: {
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 12,
  },
  statusText: {
    fontSize: 11,
    fontWeight: '600',
  },

  /* Load more */
  loadMoreBtn: {
    alignItems: 'center',
    paddingVertical: 14,
    borderRadius: 12,
    backgroundColor: '#f3f4f6',
    marginTop: 4,
  },
  loadMoreText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#f97316',
  },

  /* Section title */
  sectionTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#111827',
    marginBottom: 12,
    marginTop: 8,
  },

  /* Tax card */
  taxCard: {
    backgroundColor: '#fafafa',
    borderRadius: 12,
    padding: 16,
    marginBottom: 20,
    borderWidth: 1,
    borderColor: '#f3f4f6',
  },
  taxRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 6,
  },
  taxRowNet: {
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
    marginTop: 8,
    paddingTop: 10,
  },
  taxLabel: {
    fontSize: 14,
    color: '#6b7280',
  },
  taxValue: {
    fontSize: 14,
    color: '#374151',
    fontWeight: '600',
  },
  taxLabelBold: {
    fontSize: 15,
    color: '#111827',
    fontWeight: '700',
  },
  taxValueBold: {
    fontSize: 15,
    fontWeight: '700',
  },
});
