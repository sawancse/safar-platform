'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import type { Review } from '@/types';

export default function MyReviewsPage() {
  const router = useRouter();
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);

  const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

  useEffect(() => {
    const token = localStorage.getItem('access_token');
    if (!token) {
      router.push('/auth?redirect=/dashboard/reviews');
      return;
    }

    fetch(`${apiUrl}/api/v1/reviews/me`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.ok ? r.json() : [])
      .then((data) => setReviews(Array.isArray(data) ? data : data?.content ?? []))
      .catch(() => setReviews([]))
      .finally(() => setLoading(false));
  }, [router, apiUrl]);

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center text-gray-400">
        <div className="text-4xl mb-4 animate-spin">&#x23F3;</div>
        <p>Loading your reviews...</p>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold mb-2">My Reviews</h1>
      <p className="text-sm text-gray-500 mb-6">Reviews you&apos;ve written for properties you&apos;ve stayed at</p>

      {reviews.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <p className="text-4xl mb-3">&#x2B50;</p>
          <p className="text-sm font-medium">No reviews yet</p>
          <p className="text-xs text-gray-400 mt-1">After completing a stay, you can leave a review</p>
          <Link
            href="/search"
            className="inline-block mt-4 bg-orange-500 text-white px-6 py-2 rounded-xl font-semibold hover:bg-orange-600 text-sm"
          >
            Explore stays
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {reviews.map((review) => (
            <div key={review.id} className="border rounded-2xl p-5">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <span className="bg-orange-500 text-white text-xs font-bold px-2 py-0.5 rounded">
                    {review.rating.toFixed(1)}
                  </span>
                  <span className="text-xs text-gray-400">
                    {new Date(review.createdAt).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' })}
                  </span>
                </div>
                <Link href={`/listings/${review.listingId}`} className="text-xs text-orange-500 hover:underline">
                  View listing
                </Link>
              </div>
              {review.comment && <p className="text-sm text-gray-700">{review.comment}</p>}
              {review.reply && (
                <div className="mt-3 bg-gray-50 rounded-xl px-3 py-2">
                  <p className="text-xs font-medium text-gray-500 mb-1">Host reply</p>
                  <p className="text-sm text-gray-600">{review.reply}</p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
