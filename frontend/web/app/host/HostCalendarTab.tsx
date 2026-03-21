'use client';

import { useEffect, useState, useCallback } from 'react';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import type { AvailabilityDayDto, ICalFeed, Listing } from '@/types';

const DAYS_OF_WEEK = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

interface Props {
  token: string;
  listings: Listing[];
}

export default function HostCalendarTab({ token, listings }: Props) {
  const [selectedListingId, setSelectedListingId] = useState<string>('');
  const [year, setYear] = useState(() => new Date().getFullYear());
  const [month, setMonth] = useState(() => new Date().getMonth() + 1); // 1-based
  const [days, setDays] = useState<AvailabilityDayDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Bulk operation state
  const [rangeStart, setRangeStart] = useState<string | null>(null);
  const [rangeEnd, setRangeEnd] = useState<string | null>(null);
  const [bulkAction, setBulkAction] = useState<'block' | 'unblock' | 'price' | 'minstay'>('block');
  const [bulkPrice, setBulkPrice] = useState('');
  const [bulkMinStay, setBulkMinStay] = useState('');
  const [bulkMaxStay, setBulkMaxStay] = useState('');
  const [bulkLoading, setBulkLoading] = useState(false);

  // Day detail editor
  const [editingDay, setEditingDay] = useState<AvailabilityDayDto | null>(null);

  // iCal state
  const [icalFeeds, setIcalFeeds] = useState<ICalFeed[]>([]);
  const [icalLoading, setIcalLoading] = useState(false);
  const [icalError, setIcalError] = useState('');
  const [newFeedUrl, setNewFeedUrl] = useState('');
  const [newFeedName, setNewFeedName] = useState('');
  const [addingFeed, setAddingFeed] = useState(false);
  const [syncingFeedId, setSyncingFeedId] = useState<string | null>(null);
  const [deletingFeedId, setDeletingFeedId] = useState<string | null>(null);
  const [exportingIcal, setExportingIcal] = useState(false);

  // Set first listing as default
  useEffect(() => {
    if (listings.length > 0 && !selectedListingId) {
      setSelectedListingId(listings[0].id);
    }
  }, [listings, selectedListingId]);

  const fetchMonth = useCallback(async () => {
    if (!selectedListingId || !token) return;
    setLoading(true);
    setError('');
    try {
      const data = await api.getAvailabilityMonth(selectedListingId, year, month, token);
      setDays(data);
    } catch (e: any) {
      setError(e.message || 'Failed to load availability');
    } finally {
      setLoading(false);
    }
  }, [selectedListingId, year, month, token]);

  useEffect(() => {
    fetchMonth();
  }, [fetchMonth]);

  // Fetch iCal feeds when listing changes
  const fetchIcalFeeds = useCallback(async () => {
    if (!selectedListingId || !token) return;
    setIcalLoading(true);
    setIcalError('');
    try {
      const feeds = await api.getICalFeeds(selectedListingId, token);
      setIcalFeeds(feeds ?? []);
    } catch (e: any) {
      setIcalError(e.message || 'Failed to load iCal feeds');
    } finally {
      setIcalLoading(false);
    }
  }, [selectedListingId, token]);

  useEffect(() => {
    fetchIcalFeeds();
  }, [fetchIcalFeeds]);

  const monthName = new Date(year, month - 1, 1).toLocaleString('en-IN', { month: 'long', year: 'numeric' });

  function prevMonth() {
    if (month === 1) { setMonth(12); setYear(y => y - 1); }
    else setMonth(m => m - 1);
  }

  function nextMonth() {
    if (month === 12) { setMonth(1); setYear(y => y + 1); }
    else setMonth(m => m + 1);
  }

  // Calendar grid helpers
  const firstDayOfMonth = new Date(year, month - 1, 1).getDay(); // 0=Sun
  const daysInMonth = new Date(year, month, 0).getDate();
  const dayMap = new Map(days.map(d => [d.date, d]));

  function handleDayClick(dateStr: string) {
    const day = dayMap.get(dateStr);
    if (!rangeStart) {
      setRangeStart(dateStr);
      setRangeEnd(null);
    } else if (!rangeEnd) {
      // Ensure proper ordering
      if (dateStr >= rangeStart) {
        setRangeEnd(dateStr);
      } else {
        setRangeEnd(rangeStart);
        setRangeStart(dateStr);
      }
    } else {
      // Reset selection and start new
      setRangeStart(dateStr);
      setRangeEnd(null);
    }
    // Also allow editing single day
    if (day) setEditingDay(day);
  }

  function isInRange(dateStr: string) {
    if (!rangeStart) return false;
    if (!rangeEnd) return dateStr === rangeStart;
    return dateStr >= rangeStart && dateStr <= rangeEnd;
  }

  async function applyBulk() {
    if (!rangeStart || !selectedListingId) return;
    const from = rangeStart;
    const to = rangeEnd || rangeStart;
    setBulkLoading(true);
    setError('');
    try {
      await api.bulkUpdateAvailability(selectedListingId, {
        fromDate: from,
        toDate: to,
        isAvailable: bulkAction === 'unblock' || bulkAction === 'price' || bulkAction === 'minstay',
        priceOverridePaise: bulkAction === 'price' && bulkPrice ? Math.round(parseFloat(bulkPrice) * 100) : undefined,
        minStayNights: bulkAction === 'minstay' && bulkMinStay ? parseInt(bulkMinStay) : undefined,
        maxStayNights: bulkMaxStay ? parseInt(bulkMaxStay) : undefined,
      }, token);
      setRangeStart(null);
      setRangeEnd(null);
      await fetchMonth();
    } catch (e: any) {
      setError(e.message || 'Bulk update failed');
    } finally {
      setBulkLoading(false);
    }
  }

  function clearSelection() {
    setRangeStart(null);
    setRangeEnd(null);
    setEditingDay(null);
  }

  function cellColor(day: AvailabilityDayDto | undefined) {
    if (!day) return 'bg-white';
    if (day.hasBooking) return 'bg-blue-100 border-blue-300';
    if (!day.isAvailable) return 'bg-red-100 border-red-300';
    if (day.priceOverridePaise) return 'bg-yellow-100 border-yellow-300';
    return 'bg-green-100 border-green-300';
  }

  // iCal handlers
  async function handleExportICal() {
    if (!selectedListingId || !token) return;
    setExportingIcal(true);
    try {
      const res = await fetch(`${API_URL}/api/v1/listings/${selectedListingId}/ical/export`, {
        headers: { Authorization: `Bearer ${token}`, Accept: 'text/calendar' },
      });
      if (!res.ok) throw new Error('Export failed');
      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `safar-calendar-${selectedListingId}.ics`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (e: any) {
      setIcalError(e.message || 'Failed to export calendar');
    } finally {
      setExportingIcal(false);
    }
  }

  async function handleAddFeed() {
    if (!selectedListingId || !token || !newFeedUrl.trim()) return;
    setAddingFeed(true);
    setIcalError('');
    try {
      await api.addICalFeed(selectedListingId, {
        feedUrl: newFeedUrl.trim(),
        feedName: newFeedName.trim() || 'External Calendar',
      }, token);
      setNewFeedUrl('');
      setNewFeedName('');
      await fetchIcalFeeds();
    } catch (e: any) {
      setIcalError(e.message || 'Failed to add feed');
    } finally {
      setAddingFeed(false);
    }
  }

  async function handleSyncFeed(feedId: string) {
    if (!selectedListingId || !token) return;
    setSyncingFeedId(feedId);
    setIcalError('');
    try {
      await api.syncICalFeed(selectedListingId, feedId, token);
      await fetchIcalFeeds();
      await fetchMonth(); // Refresh calendar after sync
    } catch (e: any) {
      setIcalError(e.message || 'Sync failed');
    } finally {
      setSyncingFeedId(null);
    }
  }

  async function handleDeleteFeed(feedId: string) {
    if (!selectedListingId || !token) return;
    if (!confirm('Remove this external calendar feed?')) return;
    setDeletingFeedId(feedId);
    setIcalError('');
    try {
      await api.deleteICalFeed(selectedListingId, feedId, token);
      await fetchIcalFeeds();
    } catch (e: any) {
      setIcalError(e.message || 'Failed to delete feed');
    } finally {
      setDeletingFeedId(null);
    }
  }

  function formatLastSynced(dateStr: string | null) {
    if (!dateStr) return 'Never';
    const d = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return 'Just now';
    if (diffMin < 60) return `${diffMin}m ago`;
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24) return `${diffH}h ago`;
    return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  if (listings.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        No listings found. Create a listing first to manage its calendar.
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Listing Selector */}
      <div className="flex items-center gap-4 flex-wrap">
        <label className="text-sm font-medium text-gray-700">Listing:</label>
        <select
          value={selectedListingId}
          onChange={e => setSelectedListingId(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
        >
          {listings.map(l => (
            <option key={l.id} value={l.id}>{l.title} ({l.city})</option>
          ))}
        </select>
      </div>

      {/* Month Navigation */}
      <div className="flex items-center justify-between">
        <button onClick={prevMonth} className="px-3 py-1.5 rounded-lg border hover:bg-gray-50 text-sm font-medium">
          &larr; Prev
        </button>
        <h3 className="text-lg font-semibold text-gray-800">{monthName}</h3>
        <button onClick={nextMonth} className="px-3 py-1.5 rounded-lg border hover:bg-gray-50 text-sm font-medium">
          Next &rarr;
        </button>
      </div>

      {/* Color Legend */}
      <div className="flex gap-4 flex-wrap text-xs">
        <span className="flex items-center gap-1">
          <span className="w-4 h-4 rounded bg-green-100 border border-green-300 inline-block" /> Available
        </span>
        <span className="flex items-center gap-1">
          <span className="w-4 h-4 rounded bg-red-100 border border-red-300 inline-block" /> Blocked
        </span>
        <span className="flex items-center gap-1">
          <span className="w-4 h-4 rounded bg-blue-100 border border-blue-300 inline-block" /> Booked
        </span>
        <span className="flex items-center gap-1">
          <span className="w-4 h-4 rounded bg-yellow-100 border border-yellow-300 inline-block" /> Custom Price
        </span>
      </div>

      {error && <p className="text-red-600 text-sm">{error}</p>}

      {loading ? (
        <div className="text-center py-12 text-gray-400">Loading calendar...</div>
      ) : (
        <>
          {/* Calendar Grid */}
          <div className="border rounded-xl overflow-hidden">
            {/* Day headers */}
            <div className="grid grid-cols-7 bg-gray-50">
              {DAYS_OF_WEEK.map(d => (
                <div key={d} className="text-center py-2 text-xs font-semibold text-gray-500 border-b">{d}</div>
              ))}
            </div>
            {/* Day cells */}
            <div className="grid grid-cols-7">
              {/* Empty cells for offset */}
              {Array.from({ length: firstDayOfMonth }).map((_, i) => (
                <div key={`empty-${i}`} className="h-20 border-b border-r bg-gray-50" />
              ))}
              {/* Actual days */}
              {Array.from({ length: daysInMonth }).map((_, i) => {
                const dayNum = i + 1;
                const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(dayNum).padStart(2, '0')}`;
                const day = dayMap.get(dateStr);
                const inRange = isInRange(dateStr);
                return (
                  <div
                    key={dateStr}
                    onClick={() => handleDayClick(dateStr)}
                    className={`h-20 border-b border-r p-1 cursor-pointer transition-colors relative ${cellColor(day)} ${
                      inRange ? 'ring-2 ring-orange-500 ring-inset' : ''
                    } hover:ring-2 hover:ring-orange-300 hover:ring-inset`}
                  >
                    <div className="text-xs font-medium text-gray-700">{dayNum}</div>
                    {day?.priceOverridePaise && (
                      <div className="text-[10px] font-semibold text-yellow-700 mt-0.5">
                        {formatPaise(day.priceOverridePaise)}
                      </div>
                    )}
                    {day?.minStayNights && day.minStayNights > 1 && (
                      <div className="text-[10px] bg-orange-200 text-orange-800 rounded px-1 inline-block mt-0.5">
                        min {day.minStayNights}n
                      </div>
                    )}
                    {day?.maxStayNights && (
                      <div className="text-[10px] bg-purple-200 text-purple-800 rounded px-1 inline-block mt-0.5">
                        max {day.maxStayNights}n
                      </div>
                    )}
                    {day && !day.isAvailable && (
                      <div className="text-[10px] text-red-600 font-medium">Blocked</div>
                    )}
                    {day?.hasBooking && (
                      <div className="text-[10px] text-blue-700 font-medium">Booked</div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Bulk Operations Panel */}
          {rangeStart && (
            <div className="border rounded-xl p-4 bg-white shadow-sm space-y-4">
              <div className="flex items-center justify-between">
                <h4 className="font-semibold text-sm text-gray-800">
                  Selected: {rangeStart}{rangeEnd && rangeEnd !== rangeStart ? ` to ${rangeEnd}` : ''}
                </h4>
                <button onClick={clearSelection} className="text-xs text-gray-500 hover:text-gray-700">
                  Clear selection
                </button>
              </div>

              <div className="flex gap-2 flex-wrap">
                {(['block', 'unblock', 'price', 'minstay'] as const).map(action => (
                  <button
                    key={action}
                    onClick={() => setBulkAction(action)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition ${
                      bulkAction === action
                        ? 'bg-orange-500 text-white border-orange-500'
                        : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50'
                    }`}
                  >
                    {action === 'block' ? 'Block dates' :
                     action === 'unblock' ? 'Unblock dates' :
                     action === 'price' ? 'Set price' : 'Set min stay'}
                  </button>
                ))}
              </div>

              {bulkAction === 'price' && (
                <div className="flex items-center gap-2">
                  <label className="text-xs text-gray-600">Price (INR):</label>
                  <input
                    type="number"
                    value={bulkPrice}
                    onChange={e => setBulkPrice(e.target.value)}
                    placeholder="e.g. 2500"
                    className="border rounded-lg px-3 py-1.5 text-sm w-32 focus:ring-2 focus:ring-orange-500"
                  />
                </div>
              )}

              {bulkAction === 'minstay' && (
                <div className="flex items-center gap-4">
                  <div className="flex items-center gap-2">
                    <label className="text-xs text-gray-600">Min nights:</label>
                    <input
                      type="number"
                      value={bulkMinStay}
                      onChange={e => setBulkMinStay(e.target.value)}
                      placeholder="e.g. 2"
                      min="1"
                      className="border rounded-lg px-3 py-1.5 text-sm w-20 focus:ring-2 focus:ring-orange-500"
                    />
                  </div>
                  <div className="flex items-center gap-2">
                    <label className="text-xs text-gray-600">Max nights:</label>
                    <input
                      type="number"
                      value={bulkMaxStay}
                      onChange={e => setBulkMaxStay(e.target.value)}
                      placeholder="e.g. 30"
                      min="1"
                      className="border rounded-lg px-3 py-1.5 text-sm w-20 focus:ring-2 focus:ring-orange-500"
                    />
                  </div>
                </div>
              )}

              <button
                onClick={applyBulk}
                disabled={bulkLoading}
                className="bg-orange-500 hover:bg-orange-600 text-white font-semibold px-4 py-2 rounded-xl text-sm transition disabled:opacity-50"
              >
                {bulkLoading ? 'Applying...' : 'Apply'}
              </button>
            </div>
          )}

          {/* Single Day Detail */}
          {editingDay && !rangeEnd && (
            <div className="border rounded-xl p-4 bg-white shadow-sm">
              <h4 className="font-semibold text-sm text-gray-800 mb-2">Day Details: {editingDay.date}</h4>
              <div className="grid grid-cols-2 gap-2 text-sm text-gray-600">
                <div>Status: <span className={editingDay.isAvailable ? 'text-green-600 font-medium' : 'text-red-600 font-medium'}>
                  {editingDay.hasBooking ? 'Booked' : editingDay.isAvailable ? 'Available' : 'Blocked'}
                </span></div>
                <div>Price override: {editingDay.priceOverridePaise ? formatPaise(editingDay.priceOverridePaise) : 'None'}</div>
                <div>Min stay: {editingDay.minStayNights ?? 'Default'} nights</div>
                <div>Max stay: {editingDay.maxStayNights ?? 'No limit'}</div>
              </div>
            </div>
          )}
        </>
      )}

      {/* ── iCal Sync Section ─────────────────────────────────── */}
      {selectedListingId && (
        <div className="border rounded-xl p-5 bg-white shadow-sm space-y-5">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-semibold text-gray-800">Sync with External Calendars</h3>
            <button
              onClick={handleExportICal}
              disabled={exportingIcal}
              className="flex items-center gap-2 px-4 py-2 rounded-lg border border-orange-200 text-sm font-medium text-orange-600 hover:bg-orange-50 transition disabled:opacity-50"
            >
              {exportingIcal ? (
                <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
              ) : (
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                </svg>
              )}
              Export Safar Calendar (.ics)
            </button>
          </div>

          <p className="text-xs text-gray-500">
            Import calendars from Airbnb, Booking.com, Google Calendar, or any iCal-compatible service to keep availability in sync.
          </p>

          {icalError && <p className="text-red-600 text-sm">{icalError}</p>}

          {/* Add Feed Form */}
          <div className="border rounded-lg p-4 bg-gray-50 space-y-3">
            <h4 className="text-sm font-medium text-gray-700">Add External Calendar</h4>
            <div className="flex flex-col sm:flex-row gap-3">
              <input
                type="url"
                value={newFeedUrl}
                onChange={e => setNewFeedUrl(e.target.value)}
                placeholder="https://www.airbnb.com/calendar/ical/12345.ics"
                className="flex-1 border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
              />
              <input
                type="text"
                value={newFeedName}
                onChange={e => setNewFeedName(e.target.value)}
                placeholder="Feed name (e.g. Airbnb)"
                className="sm:w-48 border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
              />
              <button
                onClick={handleAddFeed}
                disabled={addingFeed || !newFeedUrl.trim()}
                className="px-4 py-2 bg-orange-500 hover:bg-orange-600 text-white font-semibold rounded-lg text-sm transition disabled:opacity-50 whitespace-nowrap"
              >
                {addingFeed ? 'Adding...' : 'Add Feed'}
              </button>
            </div>
          </div>

          {/* Feeds List */}
          {icalLoading ? (
            <div className="text-center py-6 text-gray-400 text-sm">Loading feeds...</div>
          ) : icalFeeds.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-4">No external calendars connected yet.</p>
          ) : (
            <div className="space-y-3">
              {icalFeeds.map(feed => (
                <div key={feed.id} className="border rounded-lg p-4 bg-white flex flex-col sm:flex-row sm:items-center gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold text-gray-800">{feed.feedName}</span>
                      <span className={`text-[10px] px-1.5 py-0.5 rounded-full font-medium ${
                        feed.isActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                      }`}>
                        {feed.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </div>
                    <p className="text-xs text-gray-400 truncate mt-0.5" title={feed.feedUrl}>
                      {feed.feedUrl}
                    </p>
                    <p className="text-xs text-gray-500 mt-1">
                      Last synced: {formatLastSynced(feed.lastSyncedAt)}
                      {feed.syncIntervalHours > 0 && (
                        <span className="ml-2 text-gray-400">
                          (auto-sync every {feed.syncIntervalHours}h)
                        </span>
                      )}
                    </p>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      onClick={() => handleSyncFeed(feed.id)}
                      disabled={syncingFeedId === feed.id}
                      className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-xs font-medium text-gray-600 hover:bg-gray-50 transition disabled:opacity-50"
                    >
                      {syncingFeedId === feed.id ? (
                        <svg className="animate-spin h-3.5 w-3.5" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                        </svg>
                      ) : (
                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                        </svg>
                      )}
                      {syncingFeedId === feed.id ? 'Syncing...' : 'Sync Now'}
                    </button>
                    <button
                      onClick={() => handleDeleteFeed(feed.id)}
                      disabled={deletingFeedId === feed.id}
                      className="px-3 py-1.5 rounded-lg border border-red-200 text-xs font-medium text-red-600 hover:bg-red-50 transition disabled:opacity-50"
                    >
                      {deletingFeedId === feed.id ? 'Removing...' : 'Delete'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
