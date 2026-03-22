'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { api } from '@/lib/api';
import type { MilesBalance, MilesHistoryEntry, MilesHistoryResponse } from '@/types';

const TIER_COLORS: Record<string, { bg: string; text: string; border: string }> = {
  BRONZE:   { bg: 'bg-amber-100',   text: 'text-amber-800',   border: 'border-amber-300' },
  SILVER:   { bg: 'bg-gray-200',    text: 'text-gray-700',    border: 'border-gray-400' },
  GOLD:     { bg: 'bg-yellow-100',  text: 'text-yellow-700',  border: 'border-yellow-400' },
  PLATINUM: { bg: 'bg-purple-100',  text: 'text-purple-700',  border: 'border-purple-400' },
};

export default function DashboardMilesPage() {
  const router = useRouter();
  const [balance, setBalance] = useState<MilesBalance | null>(null);
  const [history, setHistory] = useState<MilesHistoryEntry[]>([]);
  const [historyPage, setHistoryPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [redeemBookingId, setRedeemBookingId] = useState('');
  const [redeemMiles, setRedeemMilesVal] = useState('');
  const [redeemLoading, setRedeemLoading] = useState(false);
  const [redeemMsg, setRedeemMsg] = useState('');

  const token = typeof window !== 'undefined' ? localStorage.getItem('access_token') ?? '' : '';

  useEffect(() => {
    if (!token) {
      router.push('/auth?redirect=/dashboard/miles');
      return;
    }
    fetchData();
  }, [token, router]);

  async function fetchData() {
    setLoading(true);
    try {
      const [bal, hist] = await Promise.all([
        api.getMilesBalance(token),
        api.getMilesHistory(token, 0),
      ]);
      setBalance(bal);
      setHistory(hist.content ?? []);
      setTotalPages(hist.totalPages ?? 0);
    } catch {
      setError('Failed to load miles data.');
    } finally {
      setLoading(false);
    }
  }

  async function loadHistoryPage(page: number) {
    try {
      const hist: MilesHistoryResponse = await api.getMilesHistory(token, page);
      setHistory(hist.content ?? []);
      setHistoryPage(page);
      setTotalPages(hist.totalPages ?? 0);
    } catch {
      // ignore
    }
  }

  async function handleRedeem(e: React.FormEvent) {
    e.preventDefault();
    if (!redeemBookingId.trim() || !redeemMiles) return;
    setRedeemLoading(true);
    setRedeemMsg('');
    try {
      await api.redeemMiles(redeemBookingId.trim(), Number(redeemMiles), token);
      setRedeemMsg('Miles redeemed successfully!');
      setRedeemBookingId('');
      setRedeemMilesVal('');
      fetchData();
    } catch (err: any) {
      setRedeemMsg(err.message || 'Redemption failed.');
    } finally {
      setRedeemLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center text-gray-400">
        <div className="text-4xl mb-4 animate-spin">&#8987;</div>
        <p>Loading your miles...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center text-red-500">
        <p>{error}</p>
      </div>
    );
  }

  const tier = balance?.tier ?? 'BRONZE';
  const tierStyle = TIER_COLORS[tier] ?? TIER_COLORS.BRONZE;

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-sm text-gray-400 mb-6">
        <Link href="/dashboard" className="hover:text-orange-500">Dashboard</Link>
        <span>/</span>
        <span className="text-gray-700">Miles</span>
      </div>

      <h1 className="text-2xl font-bold mb-8">Property Miles</h1>

      {/* Balance card */}
      <div className="bg-gradient-to-r from-orange-500 to-pink-500 rounded-2xl p-8 text-white mb-8">
        <div className="flex items-center justify-between flex-wrap gap-4">
          <div>
            <p className="text-sm opacity-80 mb-1">Current Balance</p>
            <p className="text-4xl font-bold">{(balance?.balance ?? 0).toLocaleString('en-IN')} miles</p>
            <p className="text-sm opacity-80 mt-2">
              Lifetime earned: {(balance?.lifetimeMiles ?? 0).toLocaleString('en-IN')} miles
            </p>
          </div>
          <span
            className={`inline-block px-4 py-2 rounded-full text-sm font-bold border-2 ${tierStyle.bg} ${tierStyle.text} ${tierStyle.border}`}
          >
            {tier}
          </span>
        </div>
      </div>

      {/* Redeem form */}
      <div className="bg-white border rounded-2xl p-6 mb-8">
        <h2 className="text-lg font-semibold mb-4">Redeem Miles</h2>
        <form onSubmit={handleRedeem} className="flex flex-col sm:flex-row gap-3">
          <input
            className="flex-1 border rounded-lg px-3 py-2 text-sm"
            placeholder="Booking ID"
            value={redeemBookingId}
            onChange={(e) => setRedeemBookingId(e.target.value)}
            required
          />
          <input
            className="w-32 border rounded-lg px-3 py-2 text-sm"
            placeholder="Miles"
            type="number"
            min="1"
            max={balance?.balance ?? 0}
            value={redeemMiles}
            onChange={(e) => setRedeemMilesVal(e.target.value)}
            required
          />
          <button
            type="submit"
            disabled={redeemLoading}
            className="bg-orange-500 text-white px-6 py-2 rounded-lg font-semibold hover:bg-orange-600 disabled:opacity-50 transition text-sm"
          >
            {redeemLoading ? 'Redeeming...' : 'Redeem'}
          </button>
        </form>
        {redeemMsg && (
          <p className={`mt-3 text-sm ${redeemMsg.includes('success') ? 'text-green-600' : 'text-red-500'}`}>
            {redeemMsg}
          </p>
        )}
      </div>

      {/* History */}
      <div className="bg-white border rounded-2xl p-6">
        <h2 className="text-lg font-semibold mb-4">Miles History</h2>
        {history.length === 0 ? (
          <p className="text-gray-400 text-center py-8">No history yet.</p>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b text-left text-gray-500">
                    <th className="pb-2 pr-4">Date</th>
                    <th className="pb-2 pr-4">Type</th>
                    <th className="pb-2 pr-4">Miles</th>
                    <th className="pb-2">Description</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((entry) => (
                    <tr key={entry.id} className="border-b last:border-0">
                      <td className="py-3 pr-4 text-gray-500 whitespace-nowrap">
                        {new Date(entry.createdAt).toLocaleDateString('en-IN')}
                      </td>
                      <td className="py-3 pr-4">
                        <span
                          className={`text-xs font-semibold px-2 py-1 rounded-full ${
                            entry.type === 'EARNED'
                              ? 'bg-green-100 text-green-700'
                              : 'bg-orange-100 text-orange-700'
                          }`}
                        >
                          {entry.type}
                        </span>
                      </td>
                      <td className="py-3 pr-4 font-semibold">
                        {entry.type === 'EARNED' ? '+' : '-'}{entry.miles.toLocaleString('en-IN')}
                      </td>
                      <td className="py-3 text-gray-600">{entry.description}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {totalPages > 1 && (
              <div className="flex justify-center gap-2 mt-6">
                <button
                  disabled={historyPage === 0}
                  onClick={() => loadHistoryPage(historyPage - 1)}
                  className="px-3 py-1 border rounded-lg text-sm disabled:opacity-30 hover:bg-gray-50"
                >
                  Previous
                </button>
                <span className="px-3 py-1 text-sm text-gray-500">
                  Page {historyPage + 1} of {totalPages}
                </span>
                <button
                  disabled={historyPage >= totalPages - 1}
                  onClick={() => loadHistoryPage(historyPage + 1)}
                  className="px-3 py-1 border rounded-lg text-sm disabled:opacity-30 hover:bg-gray-50"
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
