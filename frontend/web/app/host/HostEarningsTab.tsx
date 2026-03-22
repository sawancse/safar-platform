'use client';

import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';

interface PnlStatement {
  hostId: string;
  year: number;
  grossRevenuePaise: number;
  expensesPaise: number;
  platformFeesPaise: number;
  tdsDeductedPaise: number;
  netProfitPaise: number;
}

export default function HostEarningsTab({ token }: { token: string }) {
  const [pnl, setPnl] = useState<PnlStatement | null>(null);
  const [loading, setLoading] = useState(true);
  const [year, setYear] = useState(new Date().getFullYear());

  useEffect(() => {
    setLoading(true);
    api.getPnl(year, token)
      .then(setPnl)
      .catch(() => setPnl(null))
      .finally(() => setLoading(false));
  }, [token, year]);

  if (loading) {
    return <div className="text-center py-20 text-gray-400"><div className="text-4xl mb-4 animate-spin">&#8987;</div><p>Loading earnings...</p></div>;
  }

  return (
    <div className="space-y-6">
      {/* Year selector */}
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">Earnings &amp; Revenue</h2>
        <select value={year} onChange={(e) => setYear(Number(e.target.value))}
          className="border rounded-lg px-3 py-1.5 text-sm">
          {[2024, 2025, 2026].map(y => <option key={y} value={y}>{y}</option>)}
        </select>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-green-50 border border-green-200 rounded-xl p-4">
          <p className="text-xs text-green-600 font-semibold uppercase">Gross Revenue</p>
          <p className="text-xl font-bold text-green-700 mt-1">{formatPaise(pnl?.grossRevenuePaise ?? 0)}</p>
        </div>
        <div className="bg-red-50 border border-red-200 rounded-xl p-4">
          <p className="text-xs text-red-600 font-semibold uppercase">Expenses</p>
          <p className="text-xl font-bold text-red-700 mt-1">{formatPaise(pnl?.expensesPaise ?? 0)}</p>
        </div>
        <div className="bg-orange-50 border border-orange-200 rounded-xl p-4">
          <p className="text-xs text-orange-600 font-semibold uppercase">Platform Fees + TDS</p>
          <p className="text-xl font-bold text-orange-700 mt-1">{formatPaise((pnl?.platformFeesPaise ?? 0) + (pnl?.tdsDeductedPaise ?? 0))}</p>
        </div>
        <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
          <p className="text-xs text-blue-600 font-semibold uppercase">Net Profit</p>
          <p className="text-xl font-bold text-blue-700 mt-1">{formatPaise(pnl?.netProfitPaise ?? 0)}</p>
        </div>
      </div>

      {/* P&L Breakdown */}
      {pnl && (
        <div className="bg-white border rounded-xl p-6">
          <h3 className="font-semibold text-gray-900 mb-4">Profit &amp; Loss — {year}</h3>
          <div className="space-y-3 text-sm">
            <div className="flex justify-between py-2 border-b">
              <span className="text-gray-600">Gross Revenue (bookings)</span>
              <span className="font-semibold text-green-700">{formatPaise(pnl.grossRevenuePaise)}</span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-gray-600">Less: Expenses</span>
              <span className="font-semibold text-red-600">-{formatPaise(pnl.expensesPaise)}</span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-gray-600">Less: Platform commission</span>
              <span className="font-semibold text-red-600">-{formatPaise(pnl.platformFeesPaise)}</span>
            </div>
            <div className="flex justify-between py-2 border-b">
              <span className="text-gray-600">Less: TDS deducted (1%)</span>
              <span className="font-semibold text-red-600">-{formatPaise(pnl.tdsDeductedPaise)}</span>
            </div>
            <div className="flex justify-between py-3 border-t-2 border-gray-900">
              <span className="font-bold text-gray-900">Net Profit</span>
              <span className="font-bold text-blue-700 text-lg">{formatPaise(pnl.netProfitPaise)}</span>
            </div>
          </div>
        </div>
      )}

      {/* No data state */}
      {!pnl && (
        <div className="text-center py-16 text-gray-400">
          <p className="text-5xl mb-3">&#128200;</p>
          <p className="font-medium">No earnings data for {year}</p>
          <p className="text-sm mt-1">Complete bookings to see your earnings here</p>
        </div>
      )}
    </div>
  );
}
