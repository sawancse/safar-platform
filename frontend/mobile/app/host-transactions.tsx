import { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  RefreshControl,
} from 'react-native';
import { useRouter } from 'expo-router';
import { api } from '@/lib/api';
import { getAccessToken } from '@/lib/auth';

/* ── Types ─────────────────────────────────────────────────── */

interface Transaction {
  id: string;
  transactionRef: string;
  date: string;
  guestName: string;
  bookingRef: string;
  amountPaise: number;
  type: 'RECEIVED' | 'REFUND';
  status: 'CAPTURED' | 'REFUNDED' | 'PENDING' | 'FAILED';
  paymentMethod: 'UPI' | 'CARD' | 'NETBANKING' | 'WALLET' | string;
}

interface TransactionPage {
  content: Transaction[];
  totalElements: number;
  totalPages: number;
}

/* ── Constants ─────────────────────────────────────────────── */

type FilterKey = 'ALL' | 'RECEIVED' | 'REFUNDED' | 'PENDING';
const FILTERS: { key: FilterKey; label: string }[] = [
  { key: 'ALL', label: 'All' },
  { key: 'RECEIVED', label: 'Received' },
  { key: 'REFUNDED', label: 'Refunded' },
  { key: 'PENDING', label: 'Pending' },
];

const STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  CAPTURED: { bg: '#dcfce7', text: '#14532d' },
  REFUNDED: { bg: '#fee2e2', text: '#7f1d1d' },
  PENDING:  { bg: '#fef9c3', text: '#854d0e' },
  FAILED:   { bg: '#f3f4f6', text: '#374151' },
};

const PAYMENT_METHOD_LABELS: Record<string, { icon: string; label: string }> = {
  UPI:        { icon: '📱', label: 'UPI' },
  CARD:       { icon: '💳', label: 'Card' },
  NETBANKING: { icon: '🏦', label: 'Net Banking' },
  WALLET:     { icon: '👛', label: 'Wallet' },
};

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

/* ── Component ─────────────────────────────────────────────── */

export default function HostTransactionsScreen() {
  const router = useRouter();

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [authed, setAuthed] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);

  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [filter, setFilter] = useState<FilterKey>('ALL');

  /* ── Data loader ──────────────────────────────────── */

  const loadTransactions = useCallback(
    async (pageNum = 0, append = false, silent = false) => {
      const token = await getAccessToken();
      if (!token) {
        setLoading(false);
        return;
      }
      setAuthed(true);
      if (!silent && !append) setLoading(true);
      if (append) setLoadingMore(true);

      try {
        const data: TransactionPage = await api.getHostTransactions(token, pageNum);
        const list = Array.isArray(data?.content) ? data.content : [];
        if (append) {
          setTransactions(prev => [...prev, ...list]);
        } else {
          setTransactions(list);
        }
        setTotalPages(data?.totalPages ?? 0);
        setPage(pageNum);
      } catch {
        if (!append) setTransactions([]);
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    },
    [],
  );

  /* ── Effects ──────────────────────────────────────── */

  useEffect(() => {
    loadTransactions(0);
  }, [loadTransactions]);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadTransactions(0, false, true);
    setRefreshing(false);
  }, [loadTransactions]);

  /* ── Filtered list ────────────────────────────────── */

  const filtered = transactions.filter(tx => {
    if (filter === 'ALL') return true;
    if (filter === 'RECEIVED') return tx.type === 'RECEIVED' || tx.status === 'CAPTURED';
    if (filter === 'REFUNDED') return tx.type === 'REFUND' || tx.status === 'REFUNDED';
    if (filter === 'PENDING') return tx.status === 'PENDING';
    return true;
  });

  /* ── Summary calculations ─────────────────────────── */

  const totalReceived = transactions
    .filter(tx => tx.status === 'CAPTURED' || tx.type === 'RECEIVED')
    .reduce((sum, tx) => sum + tx.amountPaise, 0);

  const totalRefunded = transactions
    .filter(tx => tx.status === 'REFUNDED' || tx.type === 'REFUND')
    .reduce((sum, tx) => sum + tx.amountPaise, 0);

  const netAmount = totalReceived - totalRefunded;

  /* ── Auth guard ───────────────────────────────────── */

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
        <Text style={s.emptySubtitle}>Please sign in to view your transactions.</Text>
        <TouchableOpacity style={s.primaryBtn} onPress={() => router.push('/auth')}>
          <Text style={s.primaryBtnText}>Sign In</Text>
        </TouchableOpacity>
      </View>
    );
  }

  /* ── Render ───────────────────────────────────────── */

  return (
    <View style={s.container}>
      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => router.back()} style={s.backBtn}>
          <Text style={s.backIcon}>{'←'}</Text>
        </TouchableOpacity>
        <Text style={s.headerTitle}>Transactions</Text>
        <View style={s.headerSpacer} />
      </View>

      <ScrollView
        style={s.body}
        contentContainerStyle={s.bodyContent}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#f97316" />
        }
      >
        {/* Summary Cards */}
        <View style={s.summaryRow}>
          <View style={[s.summaryCard, { borderLeftColor: '#059669' }]}>
            <Text style={s.summaryLabel}>Total Received</Text>
            <Text style={[s.summaryAmount, { color: '#059669' }]}>
              {formatAmount(totalReceived)}
            </Text>
          </View>
          <View style={[s.summaryCard, { borderLeftColor: '#dc2626' }]}>
            <Text style={s.summaryLabel}>Total Refunded</Text>
            <Text style={[s.summaryAmount, { color: '#dc2626' }]}>
              {formatAmount(totalRefunded)}
            </Text>
          </View>
        </View>

        <View style={s.netCard}>
          <Text style={s.netLabel}>Net Amount</Text>
          <Text style={s.netAmount}>{formatAmount(netAmount)}</Text>
        </View>

        {/* Filter Pills */}
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          style={s.filterRow}
          contentContainerStyle={s.filterRowContent}
        >
          {FILTERS.map(f => (
            <TouchableOpacity
              key={f.key}
              style={[s.filterChip, filter === f.key && s.filterChipActive]}
              onPress={() => setFilter(f.key)}
            >
              <Text style={[s.filterChipText, filter === f.key && s.filterChipTextActive]}>
                {f.label}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        {/* Transaction List */}
        {filtered.length === 0 ? (
          <View style={s.emptyBlock}>
            <Text style={s.emptyTitle}>No Transactions</Text>
            <Text style={s.emptySubtitle}>
              {filter === 'ALL'
                ? 'Payment transactions will appear here.'
                : `No ${filter.toLowerCase()} transactions found.`}
            </Text>
          </View>
        ) : (
          filtered.map(tx => {
            const isRefund = tx.type === 'REFUND' || tx.status === 'REFUNDED';
            const statusStyle = STATUS_COLORS[tx.status] || STATUS_COLORS.PENDING;
            const method = PAYMENT_METHOD_LABELS[tx.paymentMethod] || {
              icon: '💰',
              label: tx.paymentMethod,
            };

            return (
              <View key={tx.id} style={s.txCard}>
                <View style={s.txTopRow}>
                  <View style={s.txIdBlock}>
                    <Text style={s.txRef} numberOfLines={1}>
                      {tx.transactionRef || tx.id}
                    </Text>
                    <Text style={s.txDate}>{formatDate(tx.date)}</Text>
                  </View>
                  <Text style={[s.txAmount, { color: isRefund ? '#dc2626' : '#059669' }]}>
                    {isRefund ? '- ' : '+ '}
                    {formatAmount(tx.amountPaise)}
                  </Text>
                </View>

                <View style={s.txMidRow}>
                  <Text style={s.txGuest} numberOfLines={1}>
                    {tx.guestName}
                  </Text>
                  <Text style={s.txBookingRef}>Booking: {tx.bookingRef}</Text>
                </View>

                <View style={s.txBottomRow}>
                  <View style={s.txMethodBadge}>
                    <Text style={s.txMethodIcon}>{method.icon}</Text>
                    <Text style={s.txMethodLabel}>{method.label}</Text>
                  </View>
                  <View style={[s.statusBadge, { backgroundColor: statusStyle.bg }]}>
                    <Text style={[s.statusText, { color: statusStyle.text }]}>{tx.status}</Text>
                  </View>
                </View>
              </View>
            );
          })
        )}

        {/* Load More */}
        {page + 1 < totalPages && (
          <TouchableOpacity
            style={s.loadMoreBtn}
            onPress={() => loadTransactions(page + 1, true)}
            disabled={loadingMore}
          >
            {loadingMore ? (
              <ActivityIndicator size="small" color="#f97316" />
            ) : (
              <Text style={s.loadMoreText}>Load More</Text>
            )}
          </TouchableOpacity>
        )}
      </ScrollView>
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

  /* Summary Cards */
  summaryRow: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 12,
  },
  summaryCard: {
    flex: 1,
    backgroundColor: '#fafafa',
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: '#f3f4f6',
    borderLeftWidth: 4,
  },
  summaryLabel: {
    fontSize: 12,
    color: '#6b7280',
    marginBottom: 8,
    fontWeight: '500',
  },
  summaryAmount: {
    fontSize: 18,
    fontWeight: '700',
  },

  netCard: {
    backgroundColor: '#111827',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  netLabel: {
    fontSize: 14,
    color: '#d1d5db',
    fontWeight: '500',
  },
  netAmount: {
    fontSize: 22,
    fontWeight: '800',
    color: '#fff',
  },

  /* Filter Pills */
  filterRow: {
    marginBottom: 16,
  },
  filterRowContent: {
    gap: 8,
  },
  filterChip: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: '#f3f4f6',
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

  /* Transaction Card */
  txCard: {
    backgroundColor: '#fafafa',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#f3f4f6',
  },
  txTopRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8,
  },
  txIdBlock: {
    flex: 1,
    marginRight: 12,
  },
  txRef: {
    fontSize: 14,
    fontWeight: '700',
    color: '#111827',
  },
  txDate: {
    fontSize: 12,
    color: '#9ca3af',
    marginTop: 2,
  },
  txAmount: {
    fontSize: 16,
    fontWeight: '700',
  },
  txMidRow: {
    marginBottom: 10,
  },
  txGuest: {
    fontSize: 13,
    fontWeight: '500',
    color: '#374151',
  },
  txBookingRef: {
    fontSize: 12,
    color: '#9ca3af',
    marginTop: 2,
  },
  txBottomRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  txMethodBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#f3f4f6',
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  txMethodIcon: {
    fontSize: 14,
    marginRight: 4,
  },
  txMethodLabel: {
    fontSize: 12,
    fontWeight: '500',
    color: '#374151',
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
});
