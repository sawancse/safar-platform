'use client';

import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import DateRangePicker from '@/components/DateRangePicker';
import { exportToCsv, formatPaiseForCsv } from '@/lib/csv-export';

interface Transaction {
  id: string;
  date: string;
  type: string; // BOOKING, PAYOUT, COMMISSION, TDS, REFUND, SUBSCRIPTION
  description: string;
  listingTitle?: string;
  amountPaise: number;
  isCredit: boolean;
}

export default function HostTransactionsTab({ token }: { token: string }) {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    const t = localStorage.getItem('access_token') || token;
    // Build transactions from multiple sources
    Promise.all([
      api.getHostBookings(t).catch(() => []),
      api.getGstInvoices(t).catch(() => []),
      api.getSubscriptionInvoices(t).catch(() => []),
    ]).then(([bookings, gstInvoices, subInvoices]) => {
      const txs: Transaction[] = [];

      // Booking transactions
      (Array.isArray(bookings) ? bookings : []).forEach((b: any) => {
        if (['CONFIRMED', 'CHECKED_IN', 'COMPLETED'].includes(b.status)) {
          txs.push({
            id: `bk-${b.id}`,
            date: b.createdAt,
            type: 'BOOKING',
            description: `Booking #${b.bookingRef} — ${b.guestFirstName} ${b.guestLastName}`,
            amountPaise: b.totalAmountPaise,
            isCredit: true,
          });
          // Commission
          const commission = b.totalAmountPaise - (b.hostPayoutPaise ?? b.totalAmountPaise);
          if (commission > 0) {
            txs.push({
              id: `cm-${b.id}`,
              date: b.createdAt,
              type: 'COMMISSION',
              description: `Platform fee for #${b.bookingRef}`,
              amountPaise: commission,
              isCredit: false,
            });
          }
          // Payout
          if (b.hostPayoutPaise) {
            txs.push({
              id: `po-${b.id}`,
              date: b.updatedAt || b.createdAt,
              type: 'PAYOUT',
              description: `Payout for #${b.bookingRef}`,
              amountPaise: b.hostPayoutPaise,
              isCredit: true,
            });
          }
        }
        if (b.status === 'CANCELLED' && b.totalAmountPaise > 0) {
          txs.push({
            id: `rf-${b.id}`,
            date: b.updatedAt || b.createdAt,
            type: 'REFUND',
            description: `Refund for #${b.bookingRef}`,
            amountPaise: b.totalAmountPaise,
            isCredit: false,
          });
        }
      });

      // GST invoices -> TDS entries
      (Array.isArray(gstInvoices) ? gstInvoices : []).forEach((inv: any) => {
        const tds = Math.round(inv.taxableAmount * 0.01);
        if (tds > 0) {
          txs.push({
            id: `tds-${inv.id}`,
            date: inv.invoiceDate || inv.createdAt,
            type: 'TDS',
            description: `TDS deduction — ${inv.invoiceNumber}`,
            amountPaise: tds,
            isCredit: false,
          });
        }
      });

      // Subscription invoices
      (Array.isArray(subInvoices) ? subInvoices : []).forEach((inv: any) => {
        txs.push({
          id: `sub-${inv.id}`,
          date: inv.createdAt,
          type: 'SUBSCRIPTION',
          description: `${inv.tier} plan — ${inv.invoiceNumber}`,
          amountPaise: inv.totalPaise,
          isCredit: false,
        });
      });

      // Sort by date descending
      txs.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
      setTransactions(txs);
    }).finally(() => setLoading(false));
  }, [token]);

  // Filtering
  const filtered = transactions
    .filter(tx => !typeFilter || tx.type === typeFilter)
    .filter(tx => {
      if (!dateFrom || !dateTo) return true;
      const d = (tx.date || '').split('T')[0];
      return d >= dateFrom && d <= dateTo;
    })
    .filter(tx => {
      if (!searchQuery) return true;
      return tx.description.toLowerCase().includes(searchQuery.toLowerCase());
    });

  // Running balance
  let balance = 0;
  const withBalance = [...filtered].reverse().map(tx => {
    balance += tx.isCredit ? tx.amountPaise : -tx.amountPaise;
    return { ...tx, balance };
  }).reverse();

  const handleExport = () => {
    const headers = ['Date', 'Type', 'Description', 'Credit (Rs)', 'Debit (Rs)', 'Balance (Rs)'];
    const rows = withBalance.map(tx => [
      tx.date ? new Date(tx.date).toLocaleDateString('en-IN') : '',
      tx.type,
      tx.description,
      tx.isCredit ? formatPaiseForCsv(tx.amountPaise) : '',
      !tx.isCredit ? formatPaiseForCsv(tx.amountPaise) : '',
      formatPaiseForCsv(tx.balance),
    ]);
    exportToCsv(`safar-transactions-${new Date().toISOString().split('T')[0]}.csv`, headers, rows);
  };

  const TYPE_COLORS: Record<string, string> = {
    BOOKING: 'bg-green-100 text-green-700',
    PAYOUT: 'bg-blue-100 text-blue-700',
    COMMISSION: 'bg-orange-100 text-orange-700',
    TDS: 'bg-gray-100 text-gray-600',
    REFUND: 'bg-red-100 text-red-600',
    SUBSCRIPTION: 'bg-purple-100 text-purple-700',
  };

  if (loading) {
    return <div className="text-center py-20 text-gray-400"><div className="text-4xl mb-4 animate-spin">&#8987;</div><p>Loading transactions...</p></div>;
  }

  const fmtDate = (d: string) => d ? new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : '-';

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">Transaction History</h2>
        <button onClick={handleExport}
          className="border rounded-lg px-4 py-2 text-sm font-medium bg-white hover:bg-gray-50 flex items-center gap-1.5">
          Export CSV
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm bg-white">
          <option value="">All types</option>
          <option value="BOOKING">Bookings</option>
          <option value="PAYOUT">Payouts</option>
          <option value="COMMISSION">Commission</option>
          <option value="TDS">TDS</option>
          <option value="REFUND">Refunds</option>
          <option value="SUBSCRIPTION">Subscription</option>
        </select>
        <DateRangePicker from={dateFrom} to={dateTo} onChange={(f, t) => { setDateFrom(f); setDateTo(t); }} />
        <input type="text" placeholder="Search transactions..." value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm flex-1 min-w-[200px]" />
      </div>

      <p className="text-xs text-gray-500">{filtered.length} transactions</p>

      {/* Transaction list */}
      {withBalance.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <p className="text-5xl mb-3">&#128176;</p>
          <p className="font-medium">No transactions yet</p>
        </div>
      ) : (
        <div className="border rounded-xl overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 text-gray-500 text-xs uppercase">
              <tr>
                <th className="text-left px-4 py-3">Date</th>
                <th className="text-left px-4 py-3">Type</th>
                <th className="text-left px-4 py-3">Description</th>
                <th className="text-right px-4 py-3">Amount</th>
                <th className="text-right px-4 py-3">Balance</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {withBalance.map(tx => (
                <tr key={tx.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-500 whitespace-nowrap">{fmtDate(tx.date)}</td>
                  <td className="px-4 py-3">
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${TYPE_COLORS[tx.type] || 'bg-gray-100'}`}>
                      {tx.type}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-700 max-w-[300px] truncate">{tx.description}</td>
                  <td className={`px-4 py-3 text-right font-semibold whitespace-nowrap ${tx.isCredit ? 'text-green-700' : 'text-red-600'}`}>
                    {tx.isCredit ? '+' : '-'}{formatPaise(tx.amountPaise)}
                  </td>
                  <td className="px-4 py-3 text-right text-gray-500 whitespace-nowrap">{formatPaise(tx.balance)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
