'use client';

import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import type { Review, HostReviewStats } from '@/types';

interface Props {
  token: string;
}

function StarRating({ rating }: { rating: number }) {
  return (
    <span className="text-yellow-400">
      {[1, 2, 3, 4, 5].map((s) => (
        <span key={s} className={s <= rating ? 'text-yellow-400' : 'text-gray-200'}>★</span>
      ))}
    </span>
  );
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' });
}

export default function HostReviewsTab({ token }: Props) {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [stats, setStats] = useState<HostReviewStats | null>(null);
  const [filter, setFilter] = useState<'all' | 'pending' | 'replied'>('all');
  const [loading, setLoading] = useState(true);
  const [replyText, setReplyText] = useState<Record<string, string>>({});
  const [replyingId, setReplyingId] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [listingNames, setListingNames] = useState<Record<string, string>>({});

  useEffect(() => {
    loadReviews();
    api.getHostReviewStats(token).then(setStats).catch(() => {});
  }, [token]);

  // Resolve listing names for reviews
  useEffect(() => {
    const unknownIds = reviews
      .map(r => r.listingId)
      .filter((id, i, arr) => id && !listingNames[id] && arr.indexOf(id) === i);
    if (unknownIds.length === 0) return;
    Promise.all(unknownIds.map(id =>
      api.getListing(id).then(l => ({ id, title: l.title })).catch(() => ({ id, title: 'Unknown listing' }))
    )).then(results => {
      setListingNames(prev => {
        const next = { ...prev };
        results.forEach(r => { next[r.id] = r.title; });
        return next;
      });
    });
  }, [reviews]);

  useEffect(() => {
    setPage(0);
    loadReviews(0);
  }, [filter]);

  async function loadReviews(p = page) {
    setLoading(true);
    try {
      const res = await api.getHostReviews(token, filter, p, 10);
      const list = Array.isArray(res) ? res : (res?.content ?? []);
      setReviews(list);
      setTotalPages(res?.totalPages ?? 1);
    } catch {
      setReviews([]);
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmitReply(reviewId: string) {
    const text = replyText[reviewId]?.trim();
    if (!text) return;
    setSubmitting(true);
    try {
      const updated = await api.addHostReply(reviewId, text, token);
      setReviews((prev) => prev.map((r) => (r.id === reviewId ? updated : r)));
      setReplyingId(null);
      setEditingId(null);
      setReplyText((prev) => ({ ...prev, [reviewId]: '' }));
      // Refresh stats
      api.getHostReviewStats(token).then(setStats).catch(() => {});
    } catch (err: any) {
      alert(err.message || 'Failed to submit reply');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteReply(reviewId: string) {
    if (!confirm('Delete your reply?')) return;
    try {
      await api.deleteHostReply(reviewId, token);
      setReviews((prev) =>
        prev.map((r) => (r.id === reviewId ? { ...r, reply: undefined, repliedAt: undefined, replyUpdatedAt: undefined } : r))
      );
      api.getHostReviewStats(token).then(setStats).catch(() => {});
    } catch (err: any) {
      alert(err.message || 'Failed to delete reply');
    }
  }

  function startEdit(review: Review) {
    setEditingId(review.id);
    setReplyText((prev) => ({ ...prev, [review.id]: review.reply || '' }));
  }

  return (
    <div>
      {/* Stats cards */}
      {stats && (
        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="bg-white border rounded-xl p-4 text-center">
            <div className="text-2xl font-bold text-gray-800">{stats.totalReviews}</div>
            <div className="text-xs text-gray-500 mt-1">Total Reviews</div>
          </div>
          <div className="bg-white border rounded-xl p-4 text-center">
            <div className="text-2xl font-bold text-orange-500">{stats.pendingReplies}</div>
            <div className="text-xs text-gray-500 mt-1">Pending Replies</div>
          </div>
          <div className="bg-white border rounded-xl p-4 text-center">
            <div className="text-2xl font-bold text-green-600">{stats.repliedReviews}</div>
            <div className="text-xs text-gray-500 mt-1">Replied</div>
          </div>
        </div>
      )}

      {/* Filter tabs */}
      <div className="flex gap-2 mb-4">
        {(['all', 'pending', 'replied'] as const).map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium transition ${
              filter === f ? 'bg-orange-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            {f === 'all' ? 'All' : f === 'pending' ? 'Needs Reply' : 'Replied'}
          </button>
        ))}
      </div>

      {/* Reviews list */}
      {loading ? (
        <div className="text-center py-12 text-gray-400">Loading reviews...</div>
      ) : reviews.length === 0 ? (
        <div className="text-center py-12 text-gray-400">
          <p className="text-3xl mb-2">&#x2B50;</p>
          <p className="text-sm">{filter === 'pending' ? 'No reviews awaiting reply' : 'No reviews yet'}</p>
        </div>
      ) : (
        <div className="space-y-4">
          {reviews.map((review) => (
            <div key={review.id} className="border rounded-2xl p-5 bg-white">
              {/* Listing name */}
              {review.listingId && listingNames[review.listingId] && (
                <a href={`/listings/${review.listingId}`}
                  className="text-xs font-medium text-orange-600 hover:underline mb-2 block">
                  {listingNames[review.listingId]}
                </a>
              )}

              {/* Header */}
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-full bg-orange-100 flex items-center justify-center text-sm font-bold text-orange-600">
                    {(review.guestName || 'G')[0].toUpperCase()}
                  </div>
                  <div>
                    <div className="font-medium text-sm text-gray-800">{review.guestName || 'Guest'}</div>
                    <div className="text-xs text-gray-400">{formatDate(review.createdAt)}</div>
                  </div>
                </div>
                <StarRating rating={review.rating} />
              </div>

              {/* Category ratings */}
              {review.ratingCleanliness && (
                <div className="grid grid-cols-3 gap-x-4 gap-y-1 mb-3 text-xs text-gray-500">
                  {review.ratingCleanliness && <span>Cleanliness: {'★'.repeat(review.ratingCleanliness)}</span>}
                  {review.ratingLocation && <span>Location: {'★'.repeat(review.ratingLocation)}</span>}
                  {review.ratingValue && <span>Value: {'★'.repeat(review.ratingValue)}</span>}
                  {review.ratingCommunication && <span>Communication: {'★'.repeat(review.ratingCommunication)}</span>}
                  {review.ratingCheckIn && <span>Check-in: {'★'.repeat(review.ratingCheckIn)}</span>}
                  {review.ratingAccuracy && <span>Accuracy: {'★'.repeat(review.ratingAccuracy)}</span>}
                </div>
              )}

              {/* Comment */}
              {review.comment && <p className="text-sm text-gray-700 mb-3">{review.comment}</p>}

              {/* Guest photos */}
              {review.guestPhotoUrls && review.guestPhotoUrls.length > 0 && (
                <div className="flex gap-2 mb-3">
                  {review.guestPhotoUrls.map((url, i) => (
                    <img key={i} src={url} alt="" className="w-16 h-16 rounded-lg object-cover" />
                  ))}
                </div>
              )}

              {/* Existing reply */}
              {review.reply && editingId !== review.id && (
                <div className="bg-gray-50 rounded-xl p-3 mt-2">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs font-semibold text-gray-600">Your reply</span>
                    <div className="flex gap-2">
                      <button onClick={() => startEdit(review)} className="text-xs text-orange-500 hover:underline">Edit</button>
                      <button onClick={() => handleDeleteReply(review.id)} className="text-xs text-red-400 hover:underline">Delete</button>
                    </div>
                  </div>
                  <p className="text-sm text-gray-600">{review.reply}</p>
                  {review.replyUpdatedAt && (
                    <span className="text-[10px] text-gray-400 mt-1 block">Edited {formatDate(review.replyUpdatedAt)}</span>
                  )}
                </div>
              )}

              {/* Reply / Edit form */}
              {(replyingId === review.id || editingId === review.id) && (
                <div className="mt-3">
                  <textarea
                    className="w-full border rounded-xl p-3 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-orange-300"
                    rows={3}
                    placeholder="Write your reply..."
                    value={replyText[review.id] || ''}
                    onChange={(e) => setReplyText((prev) => ({ ...prev, [review.id]: e.target.value }))}
                  />
                  <div className="flex gap-2 mt-2">
                    <button
                      onClick={() => handleSubmitReply(review.id)}
                      disabled={submitting || !replyText[review.id]?.trim()}
                      className="bg-orange-500 text-white px-4 py-1.5 rounded-lg text-sm font-medium hover:bg-orange-600 disabled:opacity-50"
                    >
                      {submitting ? 'Sending...' : editingId === review.id ? 'Update Reply' : 'Send Reply'}
                    </button>
                    <button
                      onClick={() => { setReplyingId(null); setEditingId(null); }}
                      className="text-gray-500 text-sm hover:underline"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              )}

              {/* Reply button (when no reply and not currently replying) */}
              {!review.reply && replyingId !== review.id && (
                <button
                  onClick={() => setReplyingId(review.id)}
                  className="mt-2 text-sm text-orange-500 font-medium hover:underline"
                >
                  Reply
                </button>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-6">
          <button
            disabled={page === 0}
            onClick={() => { setPage(page - 1); loadReviews(page - 1); }}
            className="px-3 py-1.5 rounded-lg text-sm bg-gray-100 hover:bg-gray-200 disabled:opacity-40"
          >
            Previous
          </button>
          <span className="px-3 py-1.5 text-sm text-gray-500">
            Page {page + 1} of {totalPages}
          </span>
          <button
            disabled={page >= totalPages - 1}
            onClick={() => { setPage(page + 1); loadReviews(page + 1); }}
            className="px-3 py-1.5 rounded-lg text-sm bg-gray-100 hover:bg-gray-200 disabled:opacity-40"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
