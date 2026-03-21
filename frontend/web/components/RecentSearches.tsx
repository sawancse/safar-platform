'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';

interface RecentSearch {
  city: string;
  checkIn?: string;
  checkOut?: string;
  guests?: string;
  timestamp: number;
}

export default function RecentSearches() {
  const [searches, setSearches] = useState<RecentSearch[]>([]);

  useEffect(() => {
    try {
      const stored = JSON.parse(localStorage.getItem('recent_searches') || '[]');
      setSearches(stored.slice(0, 5));
    } catch {}
  }, []);

  if (searches.length === 0) return null;

  return (
    <section className="max-w-6xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-semibold">Recent searches</h2>
        <button
          onClick={() => {
            localStorage.removeItem('recent_searches');
            setSearches([]);
          }}
          className="text-xs text-gray-400 hover:text-red-500 transition"
        >
          Clear all
        </button>
      </div>
      <div className="flex gap-3 overflow-x-auto pb-2">
        {searches.map((s, i) => {
          const params = new URLSearchParams();
          params.set('city', s.city);
          if (s.checkIn) params.set('checkIn', s.checkIn);
          if (s.checkOut) params.set('checkOut', s.checkOut);
          if (s.guests && s.guests !== '1') params.set('guests', s.guests);

          return (
            <Link
              key={i}
              href={`/search?${params}`}
              className="flex items-center gap-3 border rounded-2xl px-4 py-3 hover:shadow-md transition shrink-0 min-w-[200px]"
            >
              <div className="w-10 h-10 rounded-full bg-orange-100 flex items-center justify-center text-lg shrink-0">
                <svg className="w-5 h-5 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </div>
              <div className="min-w-0">
                <p className="font-medium text-sm truncate">{s.city}</p>
                {s.checkIn && s.checkOut ? (
                  <p className="text-xs text-gray-400">{s.checkIn} - {s.checkOut}</p>
                ) : (
                  <p className="text-xs text-gray-400">Any dates</p>
                )}
              </div>
            </Link>
          );
        })}
      </div>
    </section>
  );
}
