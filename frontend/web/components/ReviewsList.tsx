'use client';

import { useState } from 'react';
import type { Review, ReviewStats } from '@/types';
import { getRatingLabel } from '@/lib/rating';

interface Props {
  reviews: Review[];
  avgRating?: number | null;
  stats?: ReviewStats | null;
}

const CATEGORY_KEYS: { key: keyof Review; statsKey: keyof ReviewStats; label: string; icon: string }[] = [
  { key: 'ratingCleanliness', statsKey: 'avgCleanliness', label: 'Cleanliness', icon: '🧹' },
  { key: 'ratingStaff', statsKey: 'avgStaff', label: 'Staff', icon: '👨‍💼' },
  { key: 'ratingFacilities', statsKey: 'avgFacilities', label: 'Facilities', icon: '🏢' },
  { key: 'ratingComfort', statsKey: 'avgComfort', label: 'Comfort', icon: '🛋️' },
  { key: 'ratingValue', statsKey: 'avgValue', label: 'Value', icon: '💰' },
  { key: 'ratingLocation', statsKey: 'avgLocation', label: 'Location', icon: '📍' },
  { key: 'ratingFreeWifi', statsKey: 'avgFreeWifi', label: 'Free WiFi', icon: '📶' },
  { key: 'ratingCommunication', statsKey: 'avgCommunication', label: 'Communication', icon: '💬' },
  { key: 'ratingCheckIn', statsKey: 'avgCheckIn', label: 'Check-in', icon: '🔑' },
  { key: 'ratingAccuracy', statsKey: 'avgAccuracy', label: 'Accuracy', icon: '✓' },
];

function StarRating({ rating, size = 'sm' }: { rating: number; size?: 'sm' | 'lg' }) {
  const cls = size === 'lg' ? 'text-lg' : 'text-sm';
  return (
    <div className={`flex gap-0.5 ${cls}`}>
      {[1, 2, 3, 4, 5].map((star) => (
        <span key={star} className={star <= Math.round(rating) ? 'text-yellow-400' : 'text-gray-200'}>
          &#9733;
        </span>
      ))}
    </div>
  );
}

function RatingBar({ label, value, icon }: { label: string; value: number; icon: string }) {
  const pct = (value / 5) * 100;
  return (
    <div className="flex items-center gap-2">
      <span className="text-sm shrink-0">{icon}</span>
      <span className="text-xs text-gray-600 w-24 shrink-0">{label}</span>
      <div className="flex-1 h-2 bg-gray-200 rounded-full overflow-hidden">
        <div className="h-full bg-green-500 rounded-full transition-all" style={{ width: `${pct}%` }} />
      </div>
      <span className="text-xs font-semibold text-gray-800 w-7 text-right">{value.toFixed(1)}</span>
    </div>
  );
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
}

function MiniCategoryBar({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex items-center gap-1.5">
      <span className="text-[11px] text-gray-500 w-20 shrink-0">{label}</span>
      <div className="flex-1 h-1 bg-gray-200 rounded-full overflow-hidden">
        <div className="h-full bg-yellow-400 rounded-full" style={{ width: `${(value / 5) * 100}%` }} />
      </div>
      <span className="text-[11px] font-medium text-gray-600 w-4 text-right">{value}</span>
    </div>
  );
}

export default function ReviewsList({ reviews, avgRating, stats }: Props) {
  const [showAll, setShowAll] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);

  if (!reviews || reviews.length === 0) {
    return (
      <div>
        <h2 className="text-xl font-semibold mb-3">Reviews</h2>
        <p className="text-gray-400 text-sm">No reviews yet. Be the first to review!</p>
      </div>
    );
  }

  const score = avgRating ?? stats?.averageRating ?? 0;
  const totalReviews = stats?.totalReviews ?? reviews.length;
  const ratingInfo = score > 0 ? getRatingLabel(score) : null;

  // Build category bars from stats
  const categoryBars = CATEGORY_KEYS
    .filter(c => stats && (stats[c.statsKey] as number) > 0)
    .map(c => ({ ...c, value: (stats?.[c.statsKey] as number) ?? 0 }));

  // Filter reviews by category — show only reviews that rated this category
  const filteredReviews = selectedCategory
    ? reviews.filter(r => {
        const cat = CATEGORY_KEYS.find(c => c.label === selectedCategory);
        if (!cat) return true;
        return (r[cat.key] as number) > 0;
      })
    : reviews;
  const displayReviews = showAll ? filteredReviews : filteredReviews.slice(0, 6);

  // Get review's category ratings + comments for inline display
  function getReviewCategories(review: Review): { label: string; value: number; comment?: string }[] {
    let comments: Record<string, string> = {};
    if (review.categoryComments) {
      try { comments = JSON.parse(review.categoryComments); } catch {}
    }
    // Map category keys to comment keys
    const commentKeyMap: Record<string, string> = {
      ratingCleanliness: 'cleanliness', ratingLocation: 'location', ratingValue: 'value',
      ratingCommunication: 'communication', ratingCheckIn: 'checkIn', ratingAccuracy: 'accuracy',
      ratingStaff: 'staff', ratingFacilities: 'facilities', ratingComfort: 'comfort', ratingFreeWifi: 'freeWifi',
    };
    return CATEGORY_KEYS
      .filter(c => (review[c.key] as number) > 0)
      .map(c => ({
        label: c.label,
        value: review[c.key] as number,
        comment: comments[commentKeyMap[c.key]] || undefined,
      }));
  }

  return (
    <div id="reviews">
      {/* ── Header: rating badge + score + count ── */}
      <div className="flex items-center gap-3 mb-6">
        <h2 className="text-xl font-semibold">Reviews</h2>
        {ratingInfo && (
          <span className={`${ratingInfo.bg} ${ratingInfo.text} text-sm font-bold px-2.5 py-1 rounded`}>
            {score.toFixed(1)}
          </span>
        )}
        {ratingInfo && (
          <span className="text-sm font-semibold text-gray-700">{ratingInfo.label}</span>
        )}
        <span className="text-sm text-gray-400">{totalReviews} review{totalReviews !== 1 ? 's' : ''}</span>
      </div>

      {/* ── Category breakdown (2-col grid) ── */}
      {categoryBars.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-10 gap-y-2.5 mb-6 p-4 bg-gray-50 rounded-xl">
          {categoryBars.map(cat => (
            <RatingBar key={cat.label} label={cat.label} value={cat.value} icon={cat.icon} />
          ))}
        </div>
      )}

      {/* ── Category filter pills ── */}
      <div className="mb-6">
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => setSelectedCategory(null)}
            className={`px-3 py-1.5 rounded-full text-sm font-medium border transition ${
              !selectedCategory ? 'border-gray-900 bg-gray-900 text-white' : 'border-gray-300 bg-white text-gray-700 hover:border-gray-500'
            }`}
          >
            All ({reviews.length})
          </button>
          {CATEGORY_KEYS.map(cat => {
            const count = reviews.filter(r => (r[cat.key] as number) > 0).length;
            if (count === 0) return null;
            return (
              <button
                key={cat.label}
                onClick={() => setSelectedCategory(selectedCategory === cat.label ? null : cat.label)}
                className={`px-3 py-1.5 rounded-full text-sm font-medium border transition ${
                  selectedCategory === cat.label
                    ? 'border-gray-900 bg-gray-900 text-white'
                    : 'border-gray-300 bg-white text-gray-700 hover:border-gray-500'
                }`}
              >
                {cat.icon} {cat.label} ({count})
              </button>
            );
          })}
        </div>
      </div>

      {/* ── Review cards (2-col grid) ── */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-x-10 gap-y-6">
        {displayReviews.map((review) => {
          const cats = getReviewCategories(review);
          return (
            <div key={review.id} className="border-b border-gray-100 pb-5 last:border-0">
              {/* Avatar + name + date */}
              <div className="flex items-center gap-3 mb-2">
                <div className="w-10 h-10 rounded-full bg-gray-900 flex items-center justify-center text-sm font-bold text-white shrink-0">
                  {(review.guestName || 'G')[0].toUpperCase()}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-sm text-gray-900">{review.guestName || 'Guest'}</p>
                  <p className="text-xs text-gray-500">{formatDate(review.createdAt)}</p>
                </div>
                <div className="flex items-center gap-1 shrink-0">
                  <StarRating rating={review.rating} />
                  <span className="text-sm font-bold text-gray-700 ml-1">{review.rating}</span>
                </div>
              </div>

              {/* Comment */}
              {review.comment && (
                <p className="text-gray-700 text-sm leading-relaxed mb-3">{review.comment}</p>
              )}

              {/* Category ratings + comments inline */}
              {cats.length > 0 && (
                <div className="mb-3 bg-gray-50 rounded-lg p-3 space-y-1.5">
                  <div className="grid grid-cols-2 gap-x-4 gap-y-1">
                    {cats.map(c => (
                      <MiniCategoryBar key={c.label} label={c.label} value={c.value} />
                    ))}
                  </div>
                  {cats.some(c => c.comment) && (
                    <div className="border-t border-gray-200 pt-2 mt-1.5 space-y-1">
                      {cats.filter(c => c.comment).map(c => (
                        <p key={c.label} className="text-xs text-gray-600">
                          <span className="font-medium text-gray-700">{c.label}:</span> {c.comment}
                        </p>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* Guest photos */}
              {review.guestPhotoUrls && review.guestPhotoUrls.length > 0 && (
                <div className="flex gap-2 mb-3">
                  {review.guestPhotoUrls.map((url, i) => (
                    <img key={i} src={url} alt="" className="w-20 h-20 rounded-lg object-cover" />
                  ))}
                </div>
              )}

              {/* Host reply */}
              {review.reply && (
                <div className="bg-gray-50 rounded-xl p-3 text-sm">
                  <p className="font-semibold text-gray-700 mb-1">Host reply</p>
                  <p className="text-gray-600">{review.reply}</p>
                  {review.replyUpdatedAt && (
                    <span className="text-[10px] text-gray-400 mt-1 block">Edited</span>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Show all / Show less */}
      {filteredReviews.length > 6 && (
        <button
          onClick={() => setShowAll(!showAll)}
          className="mt-6 px-6 py-2.5 border border-gray-900 rounded-lg text-sm font-semibold text-gray-900 hover:bg-gray-50 transition"
        >
          {showAll ? 'Show less' : `Show all ${filteredReviews.length} reviews`}
        </button>
      )}
    </div>
  );
}
