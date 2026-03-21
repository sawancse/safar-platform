'use client';

import { useEffect, useState } from 'react';
import ListingCard from './ListingCard';
import type { Listing } from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

const POPULAR_CITIES = ['Mumbai', 'Delhi', 'Goa', 'Bangalore', 'Jaipur', 'Udaipur'];

export default function Recommendations() {
  const [listings, setListings] = useState<Listing[]>([]);
  const [activeCity, setActiveCity] = useState('');

  useEffect(() => {
    loadRecommendations();
  }, []);

  async function loadRecommendations(city?: string) {
    try {
      const params = new URLSearchParams({ size: '6' });
      if (city) params.set('city', city);
      const res = await fetch(`${API_URL}/api/v1/listings?${params}`, { cache: 'no-store' });
      if (res.ok) {
        const data = await res.json();
        setListings(data.content ?? []);
      }
    } catch {}
  }

  function handleCityFilter(city: string) {
    if (activeCity === city) {
      setActiveCity('');
      loadRecommendations();
    } else {
      setActiveCity(city);
      loadRecommendations(city);
    }
  }

  return (
    <section className="max-w-6xl mx-auto px-4 py-8">
      <h2 className="text-xl font-semibold mb-2">Recommended for you</h2>
      <p className="text-sm text-gray-500 mb-4">Handpicked stays across India</p>

      {/* City filter pills */}
      <div className="flex gap-2 mb-6 overflow-x-auto pb-2">
        {POPULAR_CITIES.map((city) => (
          <button
            key={city}
            onClick={() => handleCityFilter(city)}
            className={`px-4 py-1.5 rounded-full text-sm font-medium border transition whitespace-nowrap ${
              activeCity === city
                ? 'bg-orange-500 text-white border-orange-500'
                : 'border-gray-300 text-gray-600 hover:border-orange-500 hover:text-orange-500'
            }`}
          >
            {city}
          </button>
        ))}
      </div>

      {listings.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {listings.map((listing) => (
            <ListingCard key={listing.id} listing={listing} />
          ))}
        </div>
      ) : (
        <div className="text-center py-12 text-gray-400">
          <p className="text-3xl mb-2">&#x1F50D;</p>
          <p className="text-sm">No properties found{activeCity ? ` in ${activeCity}` : ''}. Try another city!</p>
        </div>
      )}
    </section>
  );
}
