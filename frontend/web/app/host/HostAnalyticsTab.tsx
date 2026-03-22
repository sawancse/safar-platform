'use client';

import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import type { Booking, Listing } from '@/types';

interface Props {
  token: string;
}

export default function HostAnalyticsTab({ token }: Props) {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [listings, setListings] = useState<Listing[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const t = localStorage.getItem('access_token') || token;
    Promise.all([
      api.getHostBookings(t).catch(() => []),
      api.getMyListings(t).catch(() => []),
    ]).then(([b, l]) => {
      setBookings(Array.isArray(b) ? b : []);
      setListings(Array.isArray(l) ? l : []);
    }).finally(() => setLoading(false));
  }, [token]);

  if (loading) {
    return <div className="text-center py-20 text-gray-400"><div className="text-4xl mb-4 animate-spin">&#8987;</div><p>Loading analytics...</p></div>;
  }

  // Calculations
  const confirmed = bookings.filter(b => ['CONFIRMED', 'CHECKED_IN', 'COMPLETED'].includes(b.status));
  const totalRevenue = confirmed.reduce((s, b) => s + (b.hostPayoutPaise ?? b.baseAmountPaise), 0);
  const completedBookings = bookings.filter(b => b.status === 'COMPLETED');
  const cancelledBookings = bookings.filter(b => b.status === 'CANCELLED');

  // Occupancy (rough: booked nights / 30 days * listings)
  const totalNights = confirmed.reduce((s, b) => {
    const nights = Math.max(1, Math.ceil((new Date(b.checkOut).getTime() - new Date(b.checkIn).getTime()) / 86400000));
    return s + nights;
  }, 0);
  const availableNights = listings.length * 30;
  const occupancyRate = availableNights > 0 ? Math.round((totalNights / availableNights) * 100) : 0;

  // Avg nightly rate
  const avgRate = totalNights > 0 ? Math.round(totalRevenue / totalNights) : 0;

  // Monthly revenue (last 6 months)
  const months: { label: string; revenue: number }[] = [];
  for (let i = 5; i >= 0; i--) {
    const d = new Date();
    d.setMonth(d.getMonth() - i);
    const label = d.toLocaleDateString('en-IN', { month: 'short', year: '2-digit' });
    const monthStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
    const revenue = confirmed
      .filter(b => b.checkIn.startsWith(monthStr))
      .reduce((s, b) => s + (b.hostPayoutPaise ?? b.baseAmountPaise), 0);
    months.push({ label, revenue });
  }
  const maxRevenue = Math.max(...months.map(m => m.revenue), 1);

  // Bookings by status
  const statusCounts: Record<string, number> = {};
  bookings.forEach(b => { statusCounts[b.status] = (statusCounts[b.status] || 0) + 1; });

  // Per-listing breakdown
  const listingStats = listings.map(l => {
    const lb = bookings.filter(b => b.listingId === l.id);
    const lConfirmed = lb.filter(b => ['CONFIRMED', 'CHECKED_IN', 'COMPLETED'].includes(b.status));
    const rev = lConfirmed.reduce((s, b) => s + (b.hostPayoutPaise ?? b.baseAmountPaise), 0);
    return { id: l.id, title: l.title, bookings: lb.length, revenue: rev, rating: l.avgRating || 0 };
  });

  return (
    <div className="space-y-6">
      <h2 className="text-lg font-bold text-gray-900">Analytics</h2>

      {/* Key Metrics */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-green-50 border border-green-200 rounded-xl p-4">
          <p className="text-xs text-green-600 font-semibold uppercase">Total Revenue</p>
          <p className="text-xl font-bold text-green-700 mt-1">{formatPaise(totalRevenue)}</p>
        </div>
        <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
          <p className="text-xs text-blue-600 font-semibold uppercase">Occupancy Rate</p>
          <p className="text-xl font-bold text-blue-700 mt-1">{occupancyRate}%</p>
          <p className="text-xs text-blue-400">{totalNights} nights booked</p>
        </div>
        <div className="bg-purple-50 border border-purple-200 rounded-xl p-4">
          <p className="text-xs text-purple-600 font-semibold uppercase">Avg Nightly Rate</p>
          <p className="text-xl font-bold text-purple-700 mt-1">{formatPaise(avgRate)}</p>
        </div>
        <div className="bg-orange-50 border border-orange-200 rounded-xl p-4">
          <p className="text-xs text-orange-600 font-semibold uppercase">Total Bookings</p>
          <p className="text-xl font-bold text-orange-700 mt-1">{bookings.length}</p>
          <p className="text-xs text-orange-400">{cancelledBookings.length} cancelled</p>
        </div>
      </div>

      {/* Revenue Chart */}
      <div className="bg-white border rounded-xl p-6">
        <h3 className="font-semibold text-gray-900 mb-4">Revenue — Last 6 Months</h3>
        <div className="flex items-end gap-3 h-40">
          {months.map((m, i) => (
            <div key={i} className="flex-1 flex flex-col items-center gap-1">
              <span className="text-xs text-gray-500 font-medium">{formatPaise(m.revenue)}</span>
              <div className="w-full bg-orange-100 rounded-t-lg relative" style={{ height: `${Math.max(4, (m.revenue / maxRevenue) * 100)}%` }}>
                <div className="absolute inset-0 bg-orange-500 rounded-t-lg" style={{ height: '100%' }} />
              </div>
              <span className="text-xs text-gray-400">{m.label}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Bookings by Status */}
      <div className="bg-white border rounded-xl p-6">
        <h3 className="font-semibold text-gray-900 mb-4">Bookings by Status</h3>
        <div className="flex gap-4 flex-wrap">
          {Object.entries(statusCounts).map(([status, count]) => {
            const colors: Record<string, string> = {
              CONFIRMED: 'bg-blue-100 text-blue-700',
              COMPLETED: 'bg-green-100 text-green-700',
              CANCELLED: 'bg-red-100 text-red-600',
              PENDING_PAYMENT: 'bg-yellow-100 text-yellow-700',
              CHECKED_IN: 'bg-purple-100 text-purple-700',
              NO_SHOW: 'bg-gray-100 text-gray-600',
            };
            return (
              <div key={status} className={`px-4 py-2 rounded-xl ${colors[status] || 'bg-gray-100 text-gray-600'}`}>
                <span className="text-lg font-bold">{count}</span>
                <span className="text-xs ml-1">{status.replace(/_/g, ' ')}</span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Per-Listing Breakdown */}
      {listingStats.length > 0 && (
        <div className="bg-white border rounded-xl p-6">
          <h3 className="font-semibold text-gray-900 mb-4">Per-Listing Performance</h3>
          <div className="space-y-3">
            {listingStats.sort((a, b) => b.revenue - a.revenue).map(ls => (
              <div key={ls.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-sm truncate">{ls.title}</p>
                  <p className="text-xs text-gray-400">{ls.bookings} bookings · {ls.rating > 0 ? `★ ${ls.rating.toFixed(1)}` : 'No reviews'}</p>
                </div>
                <p className="font-bold text-green-700">{formatPaise(ls.revenue)}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Empty state */}
      {bookings.length === 0 && (
        <div className="text-center py-16 text-gray-400">
          <p className="text-5xl mb-3">&#128202;</p>
          <p className="font-medium">No analytics data yet</p>
          <p className="text-sm mt-1">Complete bookings to see your analytics here</p>
        </div>
      )}
    </div>
  );
}
