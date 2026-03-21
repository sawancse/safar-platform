'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import { exportToCsv, formatPaiseForCsv } from '@/lib/csv-export';
import DateRangePicker from '@/components/DateRangePicker';
import type { Booking, Listing } from '@/types';

const STATUS_BADGE: Record<string, { bg: string; label: string }> = {
  PENDING_PAYMENT: { bg: 'bg-yellow-100 text-yellow-700', label: 'Pending Payment' },
  CONFIRMED:       { bg: 'bg-blue-100 text-blue-700', label: 'Confirmed' },
  CHECKED_IN:      { bg: 'bg-purple-100 text-purple-700', label: 'Checked In' },
  COMPLETED:       { bg: 'bg-green-100 text-green-700', label: 'Completed' },
  CANCELLED:       { bg: 'bg-red-100 text-red-600', label: 'Cancelled' },
  NO_SHOW:         { bg: 'bg-gray-100 text-gray-600', label: 'No Show' },
};

const FILTERS = [
  { label: 'All', value: '' },
  { label: 'Upcoming', value: 'CONFIRMED' },
  { label: 'Checked In', value: 'CHECKED_IN' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Pending', value: 'PENDING_PAYMENT' },
  { label: 'Cancelled', value: 'CANCELLED' },
];

function fmtDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-IN', {
    weekday: 'short', day: 'numeric', month: 'short',
  });
}

function fmtDateFull(iso: string) {
  return new Date(iso).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  });
}

function daysBetween(a: string, b: string) {
  return Math.max(1, Math.ceil((new Date(b).getTime() - new Date(a).getTime()) / 86400000));
}

function getToken() {
  return typeof window !== 'undefined' ? (localStorage.getItem('access_token') ?? '') : '';
}

export default function HostBookingsTab({ token: initialToken }: { token: string }) {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [listings, setListings] = useState<Record<string, Listing>>({});
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('');
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [cancelTarget, setCancelTarget] = useState<string | null>(null);
  const [cancelReason, setCancelReason] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);

  // Enhanced filter state
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [listingFilter, setListingFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState<'checkIn' | 'createdAt' | 'amount'>('checkIn');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');

  // Always read fresh token — auto-refresh may have updated localStorage
  const token = getToken() || initialToken;

  useEffect(() => {
    const t = getToken() || initialToken;
    Promise.all([
      api.getHostBookings(t),
      api.getMyListings(t),
    ]).then(([bks, lsts]) => {
      setBookings(bks);
      const map: Record<string, Listing> = {};
      lsts.forEach((l: Listing) => { map[l.id] = l; });
      setListings(map);
    }).catch(() => {
      setBookings([]);
    }).finally(() => setLoading(false));
  }, [token]);

  // Enhanced filtering logic
  const filtered = bookings
    .filter(b => !filter || b.status === filter)
    .filter(b => !listingFilter || b.listingId === listingFilter)
    .filter(b => {
      if (!dateFrom || !dateTo) return true;
      const checkIn = b.checkIn.split('T')[0];
      return checkIn >= dateFrom && checkIn <= dateTo;
    })
    .filter(b => {
      if (!searchQuery) return true;
      const q = searchQuery.toLowerCase();
      return (b.guestFirstName?.toLowerCase().includes(q)) ||
             (b.guestLastName?.toLowerCase().includes(q)) ||
             (b.bookingRef?.toLowerCase().includes(q));
    })
    .sort((a, b) => {
      const dir = sortDir === 'asc' ? 1 : -1;
      if (sortBy === 'checkIn') return dir * (new Date(a.checkIn).getTime() - new Date(b.checkIn).getTime());
      if (sortBy === 'createdAt') return dir * (new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
      return dir * ((a.totalAmountPaise || 0) - (b.totalAmountPaise || 0));
    });

  // Stats
  const totalEarnings = bookings
    .filter(b => ['CONFIRMED', 'CHECKED_IN', 'COMPLETED'].includes(b.status))
    .reduce((sum, b) => sum + (b.hostEarningsPaise ?? b.hostPayoutPaise ?? b.baseAmountPaise), 0);
  const activeCount = bookings.filter(b => ['CONFIRMED', 'CHECKED_IN'].includes(b.status)).length;
  const completedCount = bookings.filter(b => b.status === 'COMPLETED').length;

  const hasActiveFilters = !!(listingFilter || dateFrom || searchQuery || filter);

  function clearAllFilters() {
    setFilter('');
    setListingFilter('');
    setDateFrom('');
    setDateTo('');
    setSearchQuery('');
  }

  function replaceBooking(updated: Booking) {
    setBookings((prev) => prev.map((b) => (b.id === updated.id ? updated : b)));
  }

  async function handleAction(id: string, action: () => Promise<Booking>) {
    setActionLoading(id);
    try {
      const updated = await action();
      replaceBooking(updated);
    } catch (e: any) {
      alert(e.message || 'Action failed');
    } finally {
      setActionLoading(null);
    }
  }

  async function handleCancel() {
    if (!cancelTarget || !cancelReason.trim()) return;
    setActionLoading(cancelTarget);
    try {
      const updated = await api.cancelBooking(cancelTarget, cancelReason.trim(), token);
      replaceBooking(updated);
      setCancelTarget(null);
      setCancelReason('');
    } catch (e: any) {
      alert(e.message || 'Cancellation failed');
    } finally {
      setActionLoading(null);
    }
  }

  function handleExportCsv() {
    const headers = ['Booking Ref', 'Guest', 'Listing', 'Check-in', 'Check-out', 'Nights', 'Guests', 'Status', 'Total (Rs)', 'Platform Fee (Rs)', 'Your Earnings (Rs)'];
    const rows = filtered.map(b => [
      b.bookingRef || '',
      `${b.guestFirstName} ${b.guestLastName}`,
      listings[b.listingId]?.title || b.listingId,
      b.checkIn?.split('T')[0] || '',
      b.checkOut?.split('T')[0] || '',
      String(b.nights || daysBetween(b.checkIn, b.checkOut)),
      String(b.guestsCount),
      b.status,
      formatPaiseForCsv(b.totalAmountPaise),
      formatPaiseForCsv(b.platformFeePaise ?? 0),
      formatPaiseForCsv(b.hostEarningsPaise ?? b.hostPayoutPaise ?? b.baseAmountPaise),
    ]);
    exportToCsv(`safar-bookings-${new Date().toISOString().split('T')[0]}.csv`, headers, rows);
  }

  if (loading) {
    return (
      <div className="text-center py-20 text-gray-400">
        <div className="text-4xl mb-4 animate-spin">&#8987;</div>
        <p>Loading bookings...</p>
      </div>
    );
  }

  return (
    <div>
      {/* Earnings summary bar */}
      {bookings.length > 0 && (
        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="bg-green-50 border border-green-200 rounded-xl p-4 text-center">
            <p className="text-xs text-green-600 font-semibold uppercase">Total Earnings</p>
            <p className="text-xl font-bold text-green-700 mt-1">{formatPaise(totalEarnings)}</p>
          </div>
          <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 text-center">
            <p className="text-xs text-blue-600 font-semibold uppercase">Active</p>
            <p className="text-xl font-bold text-blue-700 mt-1">{activeCount}</p>
          </div>
          <div className="bg-gray-50 border border-gray-200 rounded-xl p-4 text-center">
            <p className="text-xs text-gray-500 font-semibold uppercase">Completed</p>
            <p className="text-xl font-bold text-gray-700 mt-1">{completedCount}</p>
          </div>
        </div>
      )}

      {/* Enhanced Filter Bar */}
      <div className="bg-gray-50 rounded-xl p-4 mb-4 space-y-3">
        <div className="flex flex-wrap gap-3">
          {/* Listing filter */}
          <select value={listingFilter} onChange={(e) => setListingFilter(e.target.value)}
            className="border rounded-lg px-3 py-2 text-sm bg-white min-w-[180px]">
            <option value="">All properties</option>
            {Object.values(listings).map(l => (
              <option key={l.id} value={l.id}>{l.title}</option>
            ))}
          </select>

          {/* Date range */}
          <DateRangePicker from={dateFrom} to={dateTo} onChange={(f, t) => { setDateFrom(f); setDateTo(t); }} />

          {/* Search */}
          <div className="relative flex-1 min-w-[200px]">
            <input type="text" placeholder="Search guest name or booking ref..."
              value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm pl-8" />
            <svg className="absolute left-2.5 top-2.5 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>

          {/* Sort */}
          <select value={`${sortBy}-${sortDir}`}
            onChange={(e) => { const [s, d] = e.target.value.split('-'); setSortBy(s as any); setSortDir(d as any); }}
            className="border rounded-lg px-3 py-2 text-sm bg-white">
            <option value="checkIn-desc">Check-in (newest)</option>
            <option value="checkIn-asc">Check-in (oldest)</option>
            <option value="createdAt-desc">Booked (newest)</option>
            <option value="createdAt-asc">Booked (oldest)</option>
            <option value="amount-desc">Amount (highest)</option>
            <option value="amount-asc">Amount (lowest)</option>
          </select>

          {/* Export */}
          <button onClick={handleExportCsv}
            className="border rounded-lg px-4 py-2 text-sm font-medium bg-white hover:bg-gray-50 flex items-center gap-1.5 transition">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Export CSV
          </button>
        </div>

        {/* Active filters summary */}
        <div className="flex items-center gap-2 text-xs text-gray-500">
          <span>Showing {filtered.length} of {bookings.length} bookings</span>
          {hasActiveFilters && (
            <button onClick={clearAllFilters}
              className="text-orange-600 hover:text-orange-700 font-medium">
              Clear all filters
            </button>
          )}
        </div>
      </div>

      {/* Status filter pills */}
      <div className="flex flex-wrap gap-2 mb-6">
        {FILTERS.map((f) => {
          const count = f.value ? bookings.filter(b => b.status === f.value).length : bookings.length;
          return (
            <button key={f.value} onClick={() => setFilter(f.value)}
              className={`text-sm px-3 py-1.5 rounded-full font-medium transition flex items-center gap-1.5 ${
                filter === f.value
                  ? 'bg-orange-500 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}>
              {f.label}
              <span className={`text-xs ${filter === f.value ? 'text-orange-200' : 'text-gray-400'}`}>
                {count}
              </span>
            </button>
          );
        })}
      </div>

      {filtered.length === 0 ? (
        <div className="text-center py-20 text-gray-400">
          <p className="text-5xl mb-4">&#128203;</p>
          <p className="text-lg font-medium">No bookings yet</p>
          <p className="text-sm mt-1">
            {hasActiveFilters ? 'No bookings match this filter' : 'Bookings from guests will appear here'}
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map((b) => {
            const status = STATUS_BADGE[b.status] ?? { bg: 'bg-gray-100 text-gray-600', label: b.status };
            const listing = listings[b.listingId];
            const nights = b.nights || daysBetween(b.checkIn, b.checkOut);
            const perNightPaise = nights > 0 ? Math.round(b.baseAmountPaise / nights) : b.baseAmountPaise;
            const hostPayout = b.hostEarningsPaise ?? b.hostPayoutPaise ?? b.baseAmountPaise;
            const effectiveRate = b.hostPayoutPaise && b.totalAmountPaise > 0
              ? Math.round((1 - b.hostPayoutPaise / b.totalAmountPaise) * 100)
              : null;
            const isExpanded = expandedId === b.id;

            return (
              <div key={b.id} className="border rounded-2xl bg-white shadow-sm overflow-hidden">
                {/* Top section: listing + guest + status */}
                <div className="p-5">
                  <div className="flex items-start justify-between gap-3 mb-4">
                    <div className="flex-1 min-w-0">
                      {/* Listing name */}
                      {listing ? (
                        <Link href={`/listings/${b.listingId}`}
                          className="text-sm font-semibold text-orange-600 hover:underline truncate block">
                          {listing.title}
                        </Link>
                      ) : (
                        <p className="text-sm font-semibold text-gray-600 truncate">Listing</p>
                      )}
                      {/* Booking ref */}
                      <p className="text-xs font-mono text-gray-400 mt-0.5">#{b.bookingRef}</p>
                    </div>
                    <span className={`text-xs font-semibold px-2.5 py-1 rounded-full whitespace-nowrap ${status.bg}`}>
                      {status.label}
                    </span>
                  </div>

                  {/* Guest info */}
                  <div className="flex items-center gap-3 mb-4">
                    <div className="w-10 h-10 rounded-full bg-orange-100 text-orange-600 flex items-center justify-center text-sm font-bold flex-shrink-0">
                      {(b.guestFirstName?.[0] ?? '?').toUpperCase()}{(b.guestLastName?.[0] ?? '').toUpperCase()}
                    </div>
                    <div className="min-w-0">
                      <p className="font-semibold text-gray-800 text-sm">
                        {b.guestFirstName} {b.guestLastName}
                      </p>
                      <div className="flex flex-wrap gap-x-3 text-xs text-gray-400">
                        {b.guestPhone && <span>{b.guestPhone}</span>}
                        {b.guestEmail && <span>{b.guestEmail}</span>}
                      </div>
                    </div>
                  </div>

                  {/* Stay details grid */}
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-sm">
                    <div className="bg-gray-50 rounded-lg p-2.5">
                      <p className="text-xs text-gray-400 mb-0.5">Check-in</p>
                      <p className="font-semibold text-gray-800">{fmtDate(b.checkIn)}</p>
                    </div>
                    <div className="bg-gray-50 rounded-lg p-2.5">
                      <p className="text-xs text-gray-400 mb-0.5">Check-out</p>
                      <p className="font-semibold text-gray-800">{fmtDate(b.checkOut)}</p>
                    </div>
                    <div className="bg-gray-50 rounded-lg p-2.5">
                      <p className="text-xs text-gray-400 mb-0.5">Duration</p>
                      <p className="font-semibold text-gray-800">{nights} night{nights > 1 ? 's' : ''}</p>
                    </div>
                    <div className="bg-gray-50 rounded-lg p-2.5">
                      <p className="text-xs text-gray-400 mb-0.5">Guests</p>
                      <p className="font-semibold text-gray-800">{b.guestsCount} guest{b.guestsCount > 1 ? 's' : ''}</p>
                    </div>
                  </div>

                  {/* Price summary — always visible */}
                  <div className="mt-4 flex items-center justify-between">
                    <div>
                      <p className="text-xs text-gray-400">Your earnings</p>
                      <p className="text-xl font-bold text-gray-900">{formatPaise(hostPayout)}</p>
                    </div>
                    <button type="button" onClick={() => setExpandedId(isExpanded ? null : b.id)}
                      className="text-xs text-orange-600 hover:text-orange-700 font-semibold flex items-center gap-1">
                      {isExpanded ? 'Hide' : 'View'} breakdown
                      <svg className={`w-3.5 h-3.5 transition-transform ${isExpanded ? 'rotate-180' : ''}`}
                        fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                      </svg>
                    </button>
                  </div>
                </div>

                {/* Expanded price breakdown */}
                {isExpanded && (
                  <div className="border-t bg-gray-50/50 px-5 py-4 space-y-2 text-sm">
                    <div className="flex justify-between text-gray-600">
                      <span>{formatPaise(perNightPaise)} x {nights} night{nights > 1 ? 's' : ''}</span>
                      <span>{formatPaise(b.baseAmountPaise)}</span>
                    </div>
                    {(b.cleaningFeePaise ?? 0) > 0 && (
                      <div className="flex justify-between text-gray-600">
                        <span>Cleaning fee</span>
                        <span>{formatPaise(b.cleaningFeePaise!)}</span>
                      </div>
                    )}
                    {b.gstAmountPaise > 0 && (
                      <div className="flex justify-between text-gray-400">
                        <span>GST (18%)</span>
                        <span>{formatPaise(b.gstAmountPaise)}</span>
                      </div>
                    )}
                    {b.insuranceAmountPaise > 0 && (
                      <div className="flex justify-between text-gray-400">
                        <span>Micro-insurance</span>
                        <span>{formatPaise(b.insuranceAmountPaise)}</span>
                      </div>
                    )}
                    <div className="flex justify-between text-gray-600 border-t pt-2">
                      <span>Guest pays</span>
                      <span className="font-semibold">{formatPaise(b.totalAmountPaise)}</span>
                    </div>
                    {b.platformFeePaise != null && b.platformFeePaise > 0 ? (
                      <>
                        <div className="flex justify-between text-gray-400">
                          <span>Platform fee ({b.commissionRate != null ? `${Math.round(b.commissionRate * 100)}%` : effectiveRate ? `~${effectiveRate}%` : ''})</span>
                          <span>-{formatPaise(b.platformFeePaise)}</span>
                        </div>
                        <div className="flex justify-between font-bold text-green-700 border-t pt-2">
                          <span>Your earnings</span>
                          <span>{formatPaise(b.hostEarningsPaise ?? hostPayout)}</span>
                        </div>
                      </>
                    ) : b.hostPayoutPaise != null && b.hostPayoutPaise !== b.totalAmountPaise ? (
                      <>
                        <div className="flex justify-between text-gray-400">
                          <span>Platform fee{effectiveRate ? ` (~${effectiveRate}%)` : ''}</span>
                          <span>-{formatPaise(b.totalAmountPaise - b.hostPayoutPaise)}</span>
                        </div>
                        <div className="flex justify-between font-bold text-green-700 border-t pt-2">
                          <span>Your earnings</span>
                          <span>{formatPaise(b.hostPayoutPaise)}</span>
                        </div>
                      </>
                    ) : (
                      <div className="flex justify-between font-bold text-green-700 border-t pt-2">
                        <span>Your earnings</span>
                        <span>{formatPaise(hostPayout)}</span>
                      </div>
                    )}
                    <p className="text-xs text-gray-400 pt-1">
                      Booked {fmtDateFull(b.createdAt)}
                    </p>
                  </div>
                )}

                {/* Special requests */}
                {b.specialRequests && (
                  <div className="border-t px-5 py-3 bg-amber-50">
                    <p className="text-xs font-semibold text-amber-700 mb-0.5">Special requests</p>
                    <p className="text-sm text-amber-800">{b.specialRequests}</p>
                  </div>
                )}

                {/* Cancellation reason */}
                {b.status === 'CANCELLED' && b.cancellationReason && (
                  <div className="border-t px-5 py-3 bg-red-50">
                    <p className="text-xs font-semibold text-red-600 mb-0.5">Cancellation reason</p>
                    <p className="text-sm text-red-700">{b.cancellationReason}</p>
                  </div>
                )}

                {/* Action buttons */}
                {(b.status === 'PENDING_PAYMENT' || b.status === 'CONFIRMED' || b.status === 'CHECKED_IN') && (
                  <div className="border-t px-5 py-3 flex flex-wrap gap-2">
                    {b.status === 'PENDING_PAYMENT' && (
                      <>
                        <button onClick={() => handleAction(b.id, () => api.confirmBooking(b.id, token))}
                          disabled={actionLoading === b.id}
                          className="text-sm px-4 py-2 rounded-xl bg-blue-500 hover:bg-blue-600 text-white font-semibold disabled:opacity-50 transition">
                          {actionLoading === b.id ? 'Confirming...' : 'Confirm Booking'}
                        </button>
                        <button onClick={() => { setCancelTarget(b.id); setCancelReason(''); }}
                          disabled={actionLoading === b.id}
                          className="text-sm px-4 py-2 rounded-xl border-2 border-red-300 text-red-600 hover:bg-red-50 font-semibold disabled:opacity-50 transition">
                          Reject
                        </button>
                      </>
                    )}
                    {b.status === 'CONFIRMED' && (
                      <>
                        <button onClick={() => handleAction(b.id, () => api.checkInBooking(b.id, token))}
                          disabled={actionLoading === b.id}
                          className="text-sm px-4 py-2 rounded-xl bg-green-500 hover:bg-green-600 text-white font-semibold disabled:opacity-50 transition">
                          {actionLoading === b.id ? 'Processing...' : 'Check In Guest'}
                        </button>
                        <button onClick={() => handleAction(b.id, () => api.markNoShow(b.id, token))}
                          disabled={actionLoading === b.id}
                          className="text-sm px-4 py-2 rounded-xl bg-gray-200 hover:bg-gray-300 text-gray-700 font-semibold disabled:opacity-50 transition">
                          No Show
                        </button>
                        <button onClick={() => { setCancelTarget(b.id); setCancelReason(''); }}
                          disabled={actionLoading === b.id}
                          className="text-sm px-4 py-2 rounded-xl border-2 border-red-300 text-red-600 hover:bg-red-50 font-semibold disabled:opacity-50 transition">
                          Cancel
                        </button>
                      </>
                    )}
                    {b.status === 'CHECKED_IN' && (
                      <button onClick={() => handleAction(b.id, () => api.completeBooking(b.id, token))}
                        disabled={actionLoading === b.id}
                        className="text-sm px-4 py-2 rounded-xl bg-green-500 hover:bg-green-600 text-white font-semibold disabled:opacity-50 transition">
                        {actionLoading === b.id ? 'Processing...' : 'Mark Completed'}
                      </button>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Cancel Modal */}
      {cancelTarget && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4"
          onClick={() => setCancelTarget(null)}>
          <div className="bg-white rounded-2xl max-w-md w-full p-6"
            onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-bold mb-3">Cancel Booking</h3>
            <p className="text-sm text-gray-500 mb-4">Please provide a reason for cancellation.</p>
            <textarea rows={3}
              className="w-full border rounded-xl px-3 py-2 text-sm outline-none focus:border-orange-400 resize-none mb-4"
              placeholder="Reason for cancellation..."
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)} />
            <div className="flex gap-2 justify-end">
              <button onClick={() => setCancelTarget(null)}
                className="text-sm px-4 py-2 rounded-xl border border-gray-300 text-gray-600 hover:bg-gray-50 transition">
                Go Back
              </button>
              <button onClick={handleCancel}
                disabled={!cancelReason.trim() || actionLoading === cancelTarget}
                className="text-sm px-4 py-2 rounded-xl bg-red-500 hover:bg-red-600 text-white font-semibold disabled:opacity-50 transition">
                {actionLoading === cancelTarget ? 'Cancelling...' : 'Confirm Cancel'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
