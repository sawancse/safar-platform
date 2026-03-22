'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import type { Booking } from '@/types';

const STATUS_LABEL: Record<string, { label: string; color: string }> = {
  PENDING_PAYMENT: { label: 'Pending Payment', color: 'bg-yellow-100 text-yellow-700' },
  CONFIRMED:       { label: 'Confirmed',        color: 'bg-green-100 text-green-700' },
  CANCELLED:       { label: 'Cancelled',        color: 'bg-red-100 text-red-600' },
  COMPLETED:       { label: 'Completed',        color: 'bg-gray-100 text-gray-600' },
  CHECKED_IN:      { label: 'Checked In',       color: 'bg-blue-100 text-blue-700' },
  NO_SHOW:         { label: 'No Show',          color: 'bg-gray-100 text-gray-500' },
};

type TabKey = 'upcoming' | 'past' | 'cancelled';

export default function DashboardPage() {
  const router = useRouter();
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState('');
  const [activeTab, setActiveTab] = useState<TabKey>('upcoming');
  const [cancelModal, setCancelModal] = useState<{ open: boolean; bookingId: string; ref: string }>({ open: false, bookingId: '', ref: '' });
  const [cancelReason, setCancelReason] = useState('');
  const [cancelling, setCancelling] = useState(false);
  const [reviewModal, setReviewModal] = useState<{ open: boolean; bookingId: string; listingId: string }>({ open: false, bookingId: '', listingId: '' });
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewCategories, setReviewCategories] = useState<Record<string, number>>({
    cleanliness: 0, accuracy: 0, communication: 0, location: 0, checkIn: 0, value: 0,
    staff: 0, facilities: 0, comfort: 0, freeWifi: 0,
  });
  const [reviewCategoryComments, setReviewCategoryComments] = useState<Record<string, string>>({});
  const [submittingReview, setSubmittingReview] = useState(false);
  const [reviewedBookings, setReviewedBookings] = useState<Set<string>>(new Set());
  // Guest management
  const [guestModal, setGuestModal] = useState<{ open: boolean; bookingId: string; ref: string }>({ open: false, bookingId: '', ref: '' });
  const [guestList, setGuestList] = useState<any[]>([]);
  const [guestLoading, setGuestLoading] = useState(false);
  const [guestForm, setGuestForm] = useState({ fullName: '', email: '', phone: '', age: '', idType: '', idNumber: '', roomAssignment: '' });
  const [editingGuestId, setEditingGuestId] = useState<string | null>(null);
  const [guestSaving, setGuestSaving] = useState(false);

  useEffect(() => {
    const t = localStorage.getItem('access_token') ?? '';
    if (!t) {
      router.push('/auth?redirect=/dashboard');
      return;
    }
    setToken(t);
    loadBookings(t);
  }, [router]);

  function loadBookings(t: string) {
    setLoading(true);
    api.getMyBookings(t)
      .then(setBookings)
      .catch(() => setBookings([]))
      .finally(() => setLoading(false));
  }

  async function handleCancel() {
    if (!token || !cancelModal.bookingId) return;
    setCancelling(true);
    try {
      await api.cancelBooking(cancelModal.bookingId, cancelReason || 'Cancelled by guest', token);
      setCancelModal({ open: false, bookingId: '', ref: '' });
      setCancelReason('');
      loadBookings(token);
    } catch (e: any) {
      alert(e.message || 'Failed to cancel booking');
    } finally {
      setCancelling(false);
    }
  }

  function resetReviewForm() {
    setReviewRating(5);
    setReviewComment('');
    setReviewCategories({ cleanliness: 0, accuracy: 0, communication: 0, location: 0, checkIn: 0, value: 0, staff: 0, facilities: 0, comfort: 0, freeWifi: 0 });
    setReviewCategoryComments({});
  }

  async function handleSubmitReview() {
    if (!token || !reviewModal.bookingId) return;
    setSubmittingReview(true);
    const c = reviewCategories;
    try {
      await api.createReview(
        {
          bookingId: reviewModal.bookingId,
          rating: reviewRating,
          comment: reviewComment.trim() || undefined,
          ratingCleanliness: c.cleanliness || undefined,
          ratingAccuracy: c.accuracy || undefined,
          ratingCommunication: c.communication || undefined,
          ratingLocation: c.location || undefined,
          ratingCheckIn: c.checkIn || undefined,
          ratingValue: c.value || undefined,
          ratingStaff: c.staff || undefined,
          ratingFacilities: c.facilities || undefined,
          ratingComfort: c.comfort || undefined,
          ratingFreeWifi: c.freeWifi || undefined,
          categoryComments: Object.values(reviewCategoryComments).some(v => v.trim())
            ? JSON.stringify(reviewCategoryComments)
            : undefined,
        },
        token
      );
      setReviewedBookings((prev) => new Set(prev).add(reviewModal.bookingId));
      setReviewModal({ open: false, bookingId: '', listingId: '' });
      resetReviewForm();
    } catch (e: any) {
      alert(e.message || 'Failed to submit review');
    } finally {
      setSubmittingReview(false);
    }
  }

  // ── Guest management functions ──
  useEffect(() => {
    if (guestModal.open && guestModal.bookingId) loadGuests(guestModal.bookingId);
  }, [guestModal.open, guestModal.bookingId]);

  async function loadGuests(bookingId: string) {
    if (!token) return;
    setGuestLoading(true);
    try {
      const guests = await api.getBookingGuests(bookingId, token);
      setGuestList(guests);
    } catch { setGuestList([]); }
    finally { setGuestLoading(false); }
  }

  async function handleSaveGuest() {
    if (!token || !guestModal.bookingId || !guestForm.fullName.trim()) return;
    setGuestSaving(true);
    try {
      const payload = {
        fullName: guestForm.fullName.trim(),
        email: guestForm.email.trim() || undefined,
        phone: guestForm.phone.trim() || undefined,
        age: guestForm.age ? Number(guestForm.age) : undefined,
        idType: guestForm.idType || undefined,
        idNumber: guestForm.idNumber.trim() || undefined,
        roomAssignment: guestForm.roomAssignment || undefined,
      };
      if (editingGuestId) {
        await api.updateBookingGuest(guestModal.bookingId, editingGuestId, payload, token);
      } else {
        await api.addBookingGuest(guestModal.bookingId, payload, token);
      }
      setGuestForm({ fullName: '', email: '', phone: '', age: '', idType: '', idNumber: '', roomAssignment: '' });
      setEditingGuestId(null);
      loadGuests(guestModal.bookingId);
    } catch (e: any) {
      alert(e.message || 'Failed to save guest');
    } finally { setGuestSaving(false); }
  }

  async function handleRemoveGuest(guestId: string) {
    if (!token || !guestModal.bookingId) return;
    try {
      await api.removeBookingGuest(guestModal.bookingId, guestId, token);
      loadGuests(guestModal.bookingId);
    } catch (e: any) {
      alert(e.message || 'Failed to remove guest');
    }
  }

  function openGuestEdit(guest: any) {
    setEditingGuestId(guest.id);
    setGuestForm({
      fullName: guest.fullName || '',
      email: guest.email || '',
      phone: guest.phone || '',
      age: guest.age ? String(guest.age) : '',
      idType: guest.idType || '',
      idNumber: guest.idNumber || '',
      roomAssignment: guest.roomAssignment || '',
    });
  }

  // Filter bookings by tab
  const upcoming = bookings.filter((b) => ['PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN'].includes(b.status));
  const past = bookings.filter((b) => ['COMPLETED', 'NO_SHOW'].includes(b.status));
  const cancelled = bookings.filter((b) => b.status === 'CANCELLED');

  const filtered = activeTab === 'upcoming' ? upcoming : activeTab === 'past' ? past : cancelled;

  const tabs: { key: TabKey; label: string; count: number }[] = [
    { key: 'upcoming', label: 'Upcoming', count: upcoming.length },
    { key: 'past', label: 'Past', count: past.length },
    { key: 'cancelled', label: 'Cancelled', count: cancelled.length },
  ];

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center text-gray-400">
        <div className="text-4xl mb-4 animate-spin">&#x23F3;</div>
        <p>Loading your bookings...</p>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold mb-2">My Trips</h1>
      <p className="text-sm text-gray-500 mb-6">Manage your bookings and travel plans</p>

      {/* Quick links */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 mb-8">
        {[
          { href: '/messages', icon: '💬', label: 'Messages', color: 'bg-amber-50 hover:bg-amber-100' },
          { href: '/dashboard/miles', icon: '🎖️', label: 'Miles Balance', color: 'bg-orange-50 hover:bg-orange-100' },
          { href: '/experiences', icon: '🎭', label: 'Experiences', color: 'bg-pink-50 hover:bg-pink-100' },
          { href: '/nomad', icon: '🌍', label: 'Nomad Network', color: 'bg-blue-50 hover:bg-blue-100' },
          { href: '/medical', icon: '🏥', label: 'Medical Stays', color: 'bg-green-50 hover:bg-green-100' },
        ].map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={`rounded-2xl p-4 text-center transition ${item.color}`}
          >
            <span className="text-2xl block mb-1">{item.icon}</span>
            <span className="text-sm font-medium text-gray-700">{item.label}</span>
          </Link>
        ))}
      </div>

      {/* Tabs */}
      <div className="flex gap-1 mb-6 border-b">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2.5 text-sm font-medium border-b-2 transition ${
              activeTab === tab.key
                ? 'border-orange-500 text-orange-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            {tab.label}
            {tab.count > 0 && (
              <span className={`ml-1.5 text-xs px-1.5 py-0.5 rounded-full ${
                activeTab === tab.key ? 'bg-orange-100 text-orange-600' : 'bg-gray-100 text-gray-500'
              }`}>
                {tab.count}
              </span>
            )}
          </button>
        ))}
      </div>

      {/* Bookings list */}
      {filtered.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <p className="text-4xl mb-3">
            {activeTab === 'upcoming' ? '🗺️' : activeTab === 'past' ? '📋' : '🚫'}
          </p>
          <p className="text-sm font-medium">
            {activeTab === 'upcoming'
              ? 'No upcoming trips'
              : activeTab === 'past'
              ? 'No past trips yet'
              : 'No cancelled bookings'}
          </p>
          {activeTab === 'upcoming' && (
            <Link
              href="/search"
              className="inline-block mt-4 bg-orange-500 text-white px-6 py-2 rounded-xl font-semibold hover:bg-orange-600 text-sm"
            >
              Explore stays
            </Link>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          {filtered.map((booking) => {
            const s = STATUS_LABEL[booking.status] ?? { label: booking.status, color: 'bg-gray-100 text-gray-600' };
            const canCancel = ['PENDING_PAYMENT', 'CONFIRMED'].includes(booking.status);

            return (
              <div key={booking.id} className="border rounded-2xl p-5 hover:shadow-sm transition">
                {/* Header row */}
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs font-mono text-gray-400">{booking.bookingRef}</span>
                      <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${s.color}`}>
                        {s.label}
                      </span>
                    </div>
                    <Link
                      href={`/listings/${booking.listingId}`}
                      className="font-semibold hover:text-orange-500 transition text-sm"
                    >
                      {booking.listingTitle || 'View listing details'}
                    </Link>
                    {booking.guestFirstName && (
                      <p className="text-xs text-gray-400 mt-0.5">
                        Guest: {booking.guestFirstName} {booking.guestLastName}
                      </p>
                    )}
                  </div>
                  <div className="text-right shrink-0">
                    <p className="text-base font-bold">{formatPaise(booking.totalAmountPaise)}</p>
                    <p className="text-xs text-gray-400">{booking.nights} night{booking.nights > 1 ? 's' : ''}</p>
                  </div>
                </div>

                {/* Dates & guests */}
                <div className="flex flex-wrap gap-4 mt-3 text-sm text-gray-600">
                  <div className="flex items-center gap-1.5">
                    <span className="text-gray-400">Check-in</span>
                    <span className="font-medium">{booking.checkIn?.split('T')[0]}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="text-gray-400">Check-out</span>
                    <span className="font-medium">{booking.checkOut?.split('T')[0]}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="text-gray-400">Guests</span>
                    <span className="font-medium">{booking.guestsCount}</span>
                  </div>
                </div>

                {/* Price breakdown (expandable on click) */}
                <div className="mt-3 flex flex-wrap gap-3 text-xs text-gray-500">
                  <span>Base: {formatPaise(booking.baseAmountPaise)}</span>
                  <span>Insurance: {formatPaise(booking.insuranceAmountPaise)}</span>
                  <span>GST: {formatPaise(booking.gstAmountPaise)}</span>
                </div>

                {/* Cancellation reason */}
                {booking.status === 'CANCELLED' && booking.cancellationReason && (
                  <div className="mt-3 bg-red-50 rounded-xl px-3 py-2 text-xs text-red-600">
                    Reason: {booking.cancellationReason}
                  </div>
                )}

                {/* Actions */}
                <div className="flex flex-wrap gap-2 mt-4">
                  {booking.status === 'PENDING_PAYMENT' && (
                    <Link
                      href={`/book/${booking.listingId}?checkIn=${booking.checkIn?.split('T')[0]}&checkOut=${booking.checkOut?.split('T')[0]}&guests=${booking.guestsCount}&bookingId=${booking.id}`}
                      className="text-sm px-4 py-1.5 rounded-lg bg-orange-500 text-white hover:bg-orange-600 font-medium transition"
                    >
                      Complete Payment
                    </Link>
                  )}
                  {(() => {
                    const isReviewed = booking.hasReview || reviewedBookings.has(booking.id);
                    const isReviewable = booking.status === 'COMPLETED' || booking.status === 'CONFIRMED';
                    const checkOutDate = booking.checkOut ? new Date(booking.checkOut) : null;
                    const daysSinceCheckout = checkOutDate ? Math.floor((Date.now() - checkOutDate.getTime()) / (1000 * 60 * 60 * 24)) : 0;
                    const reviewWindowExpired = daysSinceCheckout > 30;

                    if (isReviewed) {
                      return (
                        <span className="text-sm px-4 py-1.5 rounded-lg bg-green-50 text-green-600 font-medium flex items-center gap-1.5">
                          {booking.reviewRating ? (
                            <span className="text-yellow-500">{'★'.repeat(booking.reviewRating)}{'☆'.repeat(5 - booking.reviewRating)}</span>
                          ) : null}
                          Review submitted{booking.reviewedAt ? ` on ${new Date(booking.reviewedAt).toLocaleDateString()}` : ''}
                        </span>
                      );
                    }
                    if (isReviewable && reviewWindowExpired) {
                      return (
                        <span className="text-sm px-4 py-1.5 rounded-lg bg-gray-50 text-gray-400 font-medium">
                          Review window expired
                        </span>
                      );
                    }
                    if (isReviewable && !reviewWindowExpired) {
                      return (
                        <button
                          onClick={() => setReviewModal({ open: true, bookingId: booking.id, listingId: booking.listingId })}
                          className="text-sm px-4 py-1.5 rounded-lg bg-orange-500 text-white hover:bg-orange-600 font-medium transition"
                        >
                          Write Review
                        </button>
                      );
                    }
                    return null;
                  })()}
                  {canCancel && (
                    <button
                      onClick={() => setCancelModal({ open: true, bookingId: booking.id, ref: booking.bookingRef })}
                      className="text-sm px-4 py-1.5 rounded-lg border border-red-200 text-red-500 hover:bg-red-50 font-medium transition"
                    >
                      Cancel Booking
                    </button>
                  )}
                  {booking.status !== 'CANCELLED' && booking.status !== 'COMPLETED' && (
                    <button
                      onClick={() => setGuestModal({ open: true, bookingId: booking.id, ref: booking.bookingRef })}
                      className="text-sm px-4 py-1.5 rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 font-medium transition"
                    >
                      Manage Guests
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Review Modal */}
      {reviewModal.open && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={() => !submittingReview && setReviewModal({ open: false, bookingId: '', listingId: '' })}>
          <div className="bg-white rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto p-6" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-bold mb-1">How was your stay?</h3>
            <p className="text-sm text-gray-500 mb-5">Rate each aspect of your experience</p>

            {/* Overall rating */}
            <div className="mb-5">
              <label className="block text-sm font-semibold text-gray-800 mb-2">Overall rating</label>
              <div className="flex gap-2">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setReviewRating(star)}
                    className={`text-3xl transition ${star <= reviewRating ? 'text-yellow-400' : 'text-gray-200'} hover:scale-110`}
                  >
                    &#9733;
                  </button>
                ))}
              </div>
              <p className="text-xs text-gray-400 mt-1">
                {reviewRating === 5 ? 'Exceptional' : reviewRating === 4 ? 'Very Good' : reviewRating === 3 ? 'Average' : reviewRating === 2 ? 'Below Average' : 'Poor'}
              </p>
            </div>

            {/* Category ratings */}
            <div className="mb-5 space-y-3">
              <label className="block text-sm font-semibold text-gray-800">Rate by category</label>
              {([
                { key: 'cleanliness', label: 'Cleanliness', desc: 'How clean was the property?' },
                { key: 'accuracy', label: 'Accuracy', desc: 'Was it as described in the listing?' },
                { key: 'communication', label: 'Communication', desc: 'How was the host communication?' },
                { key: 'location', label: 'Location', desc: 'How was the location?' },
                { key: 'checkIn', label: 'Check-in', desc: 'Was check-in smooth?' },
                { key: 'value', label: 'Value', desc: 'Was it worth the price?' },
                { key: 'staff', label: 'Staff', desc: 'How helpful was the staff?' },
                { key: 'facilities', label: 'Facilities', desc: 'Were facilities well maintained?' },
                { key: 'comfort', label: 'Comfort', desc: 'How comfortable was your stay?' },
                { key: 'freeWifi', label: 'Free WiFi', desc: 'How was the internet?' },
              ] as const).map(({ key, label, desc }) => (
                <div key={key} className="py-2 border-b border-gray-100 last:border-0">
                  <div className="flex items-center justify-between">
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-gray-700">{label}</p>
                      <p className="text-xs text-gray-400">{desc}</p>
                    </div>
                    <div className="flex gap-1 shrink-0 ml-3">
                      {[1, 2, 3, 4, 5].map((star) => (
                        <button
                          key={star}
                          type="button"
                          onClick={() => setReviewCategories(prev => ({ ...prev, [key]: prev[key] === star ? 0 : star }))}
                          className={`text-xl transition ${star <= (reviewCategories[key] || 0) ? 'text-yellow-400' : 'text-gray-200'} hover:scale-110`}
                        >
                          &#9733;
                        </button>
                      ))}
                    </div>
                  </div>
                  {/* Category comment — shows when rated */}
                  {(reviewCategories[key] || 0) > 0 && (
                    <input
                      type="text"
                      value={reviewCategoryComments[key] || ''}
                      onChange={(e) => setReviewCategoryComments(prev => ({ ...prev, [key]: e.target.value }))}
                      placeholder={`What about ${label.toLowerCase()}? (optional)`}
                      className="mt-1.5 w-full border border-gray-200 rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400 bg-gray-50"
                    />
                  )}
                </div>
              ))}
              <p className="text-xs text-gray-400">Tap stars to rate, add optional comments per category</p>
            </div>

            {/* Comment */}
            <div className="mb-5">
              <label className="block text-sm font-semibold text-gray-800 mb-1">Your review</label>
              <textarea
                value={reviewComment}
                onChange={(e) => setReviewComment(e.target.value)}
                rows={4}
                className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500 resize-none"
                placeholder="Tell others about your stay — what did you like? What could be improved?"
              />
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => { setReviewModal({ open: false, bookingId: '', listingId: '' }); resetReviewForm(); }}
                disabled={submittingReview}
                className="flex-1 px-4 py-2.5 rounded-xl border text-sm font-medium hover:bg-gray-50 transition disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                onClick={handleSubmitReview}
                disabled={submittingReview}
                className="flex-1 px-4 py-2.5 rounded-xl bg-orange-500 text-white text-sm font-medium hover:bg-orange-600 transition disabled:opacity-50"
              >
                {submittingReview ? 'Submitting...' : 'Submit Review'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Cancel Modal */}
      {cancelModal.open && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={() => !cancelling && setCancelModal({ open: false, bookingId: '', ref: '' })}>
          <div className="bg-white rounded-2xl max-w-md w-full p-6" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-bold mb-1">Cancel Booking</h3>
            <p className="text-sm text-gray-500 mb-4">
              Are you sure you want to cancel booking <span className="font-mono font-semibold">{cancelModal.ref}</span>?
            </p>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">Reason for cancellation</label>
              <select
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500 mb-2"
              >
                <option value="">Select a reason</option>
                <option value="Change of plans">Change of plans</option>
                <option value="Found a better deal">Found a better deal</option>
                <option value="Travel dates changed">Travel dates changed</option>
                <option value="Personal reasons">Personal reasons</option>
                <option value="Duplicate booking">Duplicate booking</option>
                <option value="Other">Other</option>
              </select>
              {cancelReason === 'Other' && (
                <textarea
                  placeholder="Please specify..."
                  rows={2}
                  className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500 resize-none"
                  onChange={(e) => setCancelReason(e.target.value)}
                />
              )}
            </div>

            <div className="bg-yellow-50 border border-yellow-200 rounded-xl px-3 py-2 text-xs text-yellow-700 mb-4">
              Cancellation is subject to the property's cancellation policy. Any applicable refund will be processed within 5-7 business days.
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => { setCancelModal({ open: false, bookingId: '', ref: '' }); setCancelReason(''); }}
                disabled={cancelling}
                className="flex-1 px-4 py-2.5 rounded-xl border text-sm font-medium hover:bg-gray-50 transition disabled:opacity-50"
              >
                Keep Booking
              </button>
              <button
                onClick={handleCancel}
                disabled={cancelling || !cancelReason}
                className="flex-1 px-4 py-2.5 rounded-xl bg-red-500 text-white text-sm font-medium hover:bg-red-600 transition disabled:opacity-50"
              >
                {cancelling ? 'Cancelling...' : 'Yes, Cancel'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Guest Management Modal */}
      {guestModal.open && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4"
          onClick={() => { setGuestModal({ open: false, bookingId: '', ref: '' }); setEditingGuestId(null); setGuestForm({ fullName: '', email: '', phone: '', age: '', idType: '', idNumber: '', roomAssignment: '' }); }}>
          <div className="bg-white rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto p-6" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-lg font-bold">Manage Guests</h3>
                <p className="text-xs text-gray-400">Booking {guestModal.ref}</p>
              </div>
              <button onClick={() => { setGuestModal({ open: false, bookingId: '', ref: '' }); setEditingGuestId(null); }}
                className="text-gray-400 hover:text-gray-600 text-xl">&times;</button>
            </div>

            {/* Current guests */}
            {guestLoading ? (
              <p className="text-sm text-gray-400 py-4 text-center">Loading guests...</p>
            ) : guestList.length === 0 ? (
              <p className="text-sm text-gray-400 py-4 text-center">No guests added yet</p>
            ) : (
              <div className="space-y-2 mb-4">
                {guestList.map((g: any) => (
                  <div key={g.id} className={`flex items-center justify-between p-3 rounded-xl ${g.isPrimary ? 'bg-orange-50 border border-orange-200' : 'bg-gray-50 border border-gray-200'}`}>
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-gray-800">
                        {g.isPrimary && <span className="text-orange-500 mr-1">●</span>}
                        {g.fullName}
                      </p>
                      <div className="flex flex-wrap gap-2 text-xs text-gray-400 mt-0.5">
                        {g.email && <span>{g.email}</span>}
                        {g.phone && <span>{g.phone}</span>}
                        {g.age && <span>Age {g.age}</span>}
                        {g.roomAssignment && <span className="text-blue-500">{g.roomAssignment}</span>}
                        {g.idType && <span>{g.idType}: {g.idNumber}</span>}
                      </div>
                    </div>
                    <div className="flex gap-1 shrink-0 ml-2">
                      <button onClick={() => openGuestEdit(g)}
                        className="text-xs text-orange-600 hover:text-orange-700 px-2 py-1">Edit</button>
                      <button onClick={() => handleRemoveGuest(g.id)}
                        className="text-xs text-red-500 hover:text-red-600 px-2 py-1">Remove</button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Add/Edit guest form */}
            <div className="border-t pt-4">
              <p className="text-sm font-semibold text-gray-700 mb-2">
                {editingGuestId ? 'Edit Guest' : 'Add New Guest'}
              </p>
              <div className="grid grid-cols-2 gap-2">
                <input type="text" placeholder="Full name *" value={guestForm.fullName}
                  onChange={e => setGuestForm(f => ({ ...f, fullName: e.target.value }))}
                  className="col-span-2 border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400" />
                <input type="email" placeholder="Email" value={guestForm.email}
                  onChange={e => setGuestForm(f => ({ ...f, email: e.target.value }))}
                  className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400" />
                <input type="tel" placeholder="Phone" value={guestForm.phone}
                  onChange={e => setGuestForm(f => ({ ...f, phone: e.target.value }))}
                  className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400" />
                <input type="number" placeholder="Age" min="0" max="120" value={guestForm.age}
                  onChange={e => setGuestForm(f => ({ ...f, age: e.target.value }))}
                  className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400" />
                <input type="text" placeholder="Room assignment" value={guestForm.roomAssignment}
                  onChange={e => setGuestForm(f => ({ ...f, roomAssignment: e.target.value }))}
                  className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400" />
                <select value={guestForm.idType}
                  onChange={e => setGuestForm(f => ({ ...f, idType: e.target.value }))}
                  className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400 bg-white">
                  <option value="">ID type (optional)</option>
                  <option value="AADHAAR">Aadhaar</option>
                  <option value="PASSPORT">Passport</option>
                  <option value="DRIVING_LICENSE">Driving License</option>
                  <option value="VOTER_ID">Voter ID</option>
                  <option value="PAN">PAN Card</option>
                </select>
                <input type="text" placeholder="ID number" value={guestForm.idNumber}
                  onChange={e => setGuestForm(f => ({ ...f, idNumber: e.target.value }))}
                  className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400" />
              </div>
              <div className="flex gap-2 mt-3">
                {editingGuestId && (
                  <button onClick={() => { setEditingGuestId(null); setGuestForm({ fullName: '', email: '', phone: '', age: '', idType: '', idNumber: '', roomAssignment: '' }); }}
                    className="flex-1 text-sm font-medium text-gray-600 border rounded-lg py-2 hover:bg-gray-50 transition">
                    Cancel Edit
                  </button>
                )}
                <button onClick={handleSaveGuest} disabled={guestSaving || !guestForm.fullName.trim()}
                  className="flex-1 text-sm font-medium bg-orange-500 text-white rounded-lg py-2 hover:bg-orange-600 transition disabled:opacity-50">
                  {guestSaving ? 'Saving...' : editingGuestId ? 'Update Guest' : 'Add Guest'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
