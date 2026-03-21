'use client';

import { useEffect, useState } from 'react';
import ListingCard from './ListingCard';
import type { Listing } from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export default function StillInterested() {
  const [listings, setListings] = useState<Listing[]>([]);

  useEffect(() => {
    try {
      const ids: string[] = JSON.parse(localStorage.getItem('recently_viewed') || '[]');
      if (ids.length === 0) return;

      // Fetch up to 4 recently viewed listings
      Promise.all(
        ids.slice(0, 4).map((id) =>
          fetch(`${API_URL}/api/v1/listings/${id}`)
            .then((r) => (r.ok ? r.json() : null))
            .catch(() => null)
        )
      ).then((results) => {
        setListings(results.filter(Boolean));
      });
    } catch {}
  }, []);

  if (listings.length === 0) return null;

  return (
    <section className="max-w-6xl mx-auto px-4 py-8">
      <h2 className="text-xl font-semibold mb-4">Still interested?</h2>
      <p className="text-sm text-gray-500 mb-6">Properties you recently viewed</p>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {listings.map((listing) => (
          <ListingCard key={listing.id} listing={listing} />
        ))}
      </div>
    </section>
  );
}
