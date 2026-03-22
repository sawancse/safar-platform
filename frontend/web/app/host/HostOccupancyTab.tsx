'use client';

import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import type { OccupancyReport } from '@/types';

interface Props {
  token: string;
}

export default function HostOccupancyTab({ token }: Props) {
  const currentYear = new Date().getFullYear();
  const [from, setFrom] = useState(`${currentYear}-01-01`);
  const [to, setTo] = useState(`${currentYear}-12-31`);
  const [report, setReport] = useState<OccupancyReport | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchReport = () => {
    setLoading(true);
    const t = localStorage.getItem('access_token') || token;
    api.getOccupancyReport(from, to, t)
      .then(r => setReport(r))
      .catch(() => setReport(null))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchReport();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  if (loading) {
    return (
      <div className="text-center py-20 text-gray-400">
        <div className="text-4xl mb-4 animate-spin">&#8987;</div>
        <p>Loading occupancy report...</p>
      </div>
    );
  }

  const maxMonthlyOccupancy = report?.monthlyBreakdown
    ? Math.max(...report.monthlyBreakdown.map(m => m.occupancyPercent), 1)
    : 100;

  return (
    <div className="space-y-6">
      {/* Date Range Picker */}
      <div className="bg-white border rounded-xl p-4 flex flex-wrap items-end gap-4">
        <div>
          <label className="block text-xs text-gray-500 mb-1">From</label>
          <input
            type="date"
            value={from}
            onChange={e => setFrom(e.target.value)}
            className="border rounded-lg px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">To</label>
          <input
            type="date"
            value={to}
            onChange={e => setTo(e.target.value)}
            className="border rounded-lg px-3 py-2 text-sm"
          />
        </div>
        <button
          onClick={fetchReport}
          className="bg-orange-500 hover:bg-orange-600 text-white px-4 py-2 rounded-lg text-sm font-medium transition"
        >
          Update
        </button>
      </div>

      {!report ? (
        <div className="text-center py-12 text-gray-400">
          <p>No occupancy data available for the selected period.</p>
        </div>
      ) : (
        <>
          {/* Summary Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <SummaryCard
              label="Occupancy Rate"
              value={`${report.overallOccupancyPercent.toFixed(1)}%`}
              color="text-blue-600"
            />
            <SummaryCard
              label="ADR (Avg Daily Rate)"
              value={formatPaise(report.adrPaise)}
              color="text-green-600"
            />
            <SummaryCard
              label="RevPAR"
              value={formatPaise(report.revparPaise)}
              color="text-purple-600"
            />
            <SummaryCard
              label="Total Revenue"
              value={formatPaise(report.totalRevenuePaise)}
              color="text-orange-600"
            />
          </div>

          {/* Extra stats row */}
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            <div className="bg-white border rounded-xl p-4 text-center">
              <p className="text-2xl font-bold text-gray-800">{report.totalBookings}</p>
              <p className="text-xs text-gray-500 mt-1">Total Bookings</p>
            </div>
            <div className="bg-white border rounded-xl p-4 text-center">
              <p className="text-2xl font-bold text-gray-800">{report.totalNights}</p>
              <p className="text-xs text-gray-500 mt-1">Booked Nights</p>
            </div>
            <div className="bg-white border rounded-xl p-4 text-center">
              <p className="text-2xl font-bold text-gray-800">{report.listings.length}</p>
              <p className="text-xs text-gray-500 mt-1">Active Listings</p>
            </div>
          </div>

          {/* Monthly Occupancy Bar Chart (CSS-based) */}
          {report.monthlyBreakdown.length > 0 && (
            <div className="bg-white border rounded-xl p-6">
              <h3 className="font-semibold text-gray-800 mb-4">Monthly Occupancy</h3>
              <div className="flex items-end gap-2 h-48">
                {report.monthlyBreakdown.map(m => {
                  const pct = maxMonthlyOccupancy > 0
                    ? (m.occupancyPercent / maxMonthlyOccupancy) * 100
                    : 0;
                  const label = formatMonthLabel(m.month);
                  return (
                    <div key={m.month} className="flex-1 flex flex-col items-center justify-end h-full">
                      <span className="text-xs text-gray-500 mb-1">
                        {m.occupancyPercent.toFixed(0)}%
                      </span>
                      <div
                        className="w-full rounded-t-md bg-orange-400 hover:bg-orange-500 transition-all min-h-[4px]"
                        style={{ height: `${Math.max(pct, 2)}%` }}
                        title={`${label}: ${m.occupancyPercent.toFixed(1)}% occupancy, ${formatPaise(m.revenuePaise)} revenue, ${m.bookingCount} bookings`}
                      />
                      <span className="text-[10px] text-gray-400 mt-1 whitespace-nowrap">{label}</span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Per-Listing Table */}
          {report.listings.length > 0 && (
            <div className="bg-white border rounded-xl overflow-hidden">
              <h3 className="font-semibold text-gray-800 p-4 border-b">Per-Listing Breakdown</h3>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="text-left px-4 py-3 font-medium text-gray-600">Listing</th>
                      <th className="text-right px-4 py-3 font-medium text-gray-600">Occupancy</th>
                      <th className="text-right px-4 py-3 font-medium text-gray-600">Revenue</th>
                      <th className="text-right px-4 py-3 font-medium text-gray-600">Booked Nights</th>
                      <th className="text-right px-4 py-3 font-medium text-gray-600">Bookings</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {report.listings.map(l => (
                      <tr key={l.listingId} className="hover:bg-gray-50">
                        <td className="px-4 py-3 font-medium text-gray-800 max-w-xs truncate">
                          {l.listingTitle}
                        </td>
                        <td className="px-4 py-3 text-right">
                          <span className={`font-semibold ${
                            l.occupancyPercent >= 70 ? 'text-green-600' :
                            l.occupancyPercent >= 40 ? 'text-yellow-600' : 'text-red-500'
                          }`}>
                            {l.occupancyPercent.toFixed(1)}%
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right text-gray-700">{formatPaise(l.revenuePaise)}</td>
                        <td className="px-4 py-3 text-right text-gray-700">{l.bookedNights}</td>
                        <td className="px-4 py-3 text-right text-gray-700">{l.bookingCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function SummaryCard({ label, value, color }: { label: string; value: string; color: string }) {
  return (
    <div className="bg-white border rounded-xl p-4 text-center">
      <p className={`text-2xl font-bold ${color}`}>{value}</p>
      <p className="text-xs text-gray-500 mt-1">{label}</p>
    </div>
  );
}

function formatMonthLabel(month: string): string {
  // month format: "2026-03"
  const [year, m] = month.split('-');
  const date = new Date(parseInt(year), parseInt(m) - 1);
  return date.toLocaleDateString('en-IN', { month: 'short', year: '2-digit' });
}
