'use client';

import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import ListingCard from '@/components/ListingCard';

const PROPERTY_TYPES = [
  { label: 'All', value: '' },
  { label: 'Homes', value: 'HOME' },
  { label: 'Rooms', value: 'ROOM' },
  { label: 'Commercial', value: 'COMMERCIAL' },
  { label: 'Unique Stays', value: 'UNIQUE' },
  { label: 'Villas', value: 'VILLA' },
  { label: 'Resorts', value: 'RESORT' },
  { label: 'Homestays', value: 'HOMESTAY' },
  { label: 'Farm Stays', value: 'FARMSTAY' },
  { label: 'PG', value: 'PG' },
  { label: 'Hotels', value: 'HOTEL' },
  { label: 'Co-living', value: 'COLIVING' },
  { label: 'Hostels', value: 'HOSTEL_DORM' },
];

export default function TrendingListings() {
  const [activeType, setActiveType] = useState('');
  const [listings, setListings] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    const params: Record<string, string> = { size: '6' };
    if (activeType) params.type = activeType;

    api.search(params)
      .then((res) => setListings(res.content ?? []))
      .catch(() => setListings([]))
      .finally(() => setLoading(false));
  }, [activeType]);

  return (
    <section className="max-w-6xl mx-auto px-4 py-8">
      {/* Property type tabs */}
      <div className="flex gap-3 mb-8 overflow-x-auto pb-2">
        {PROPERTY_TYPES.map((pt) => (
          <button
            key={pt.label}
            onClick={() => setActiveType(pt.value)}
            className={`px-4 py-2 rounded-full border whitespace-nowrap text-sm font-medium transition ${
              activeType === pt.value
                ? 'border-orange-500 bg-orange-500 text-white'
                : 'border-gray-300 hover:border-orange-500 hover:text-orange-500'
            }`}
          >
            {pt.label}
          </button>
        ))}
      </div>

      {/* Trending listings */}
      <h2 className="text-2xl font-semibold mb-6">
        {activeType
          ? `${PROPERTY_TYPES.find((pt) => pt.value === activeType)?.label ?? ''} properties`
          : 'Trending properties'}
      </h2>

      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="rounded-xl bg-gray-100 h-72 animate-pulse" />
          ))}
        </div>
      ) : listings.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {listings.map((listing) => (
            <ListingCard key={listing.id} listing={listing} />
          ))}
        </div>
      ) : (
        <div className="text-center py-16 text-gray-400">
          <p className="text-4xl mb-4">&#x1F3E0;</p>
          <p>
            {activeType
              ? `No ${PROPERTY_TYPES.find((pt) => pt.value === activeType)?.label?.toLowerCase() ?? ''} properties yet`
              : 'Be the first to list your property on Safar!'}
          </p>
          <a
            href="/host"
            className="mt-4 inline-block bg-orange-500 text-white px-6 py-2 rounded-xl font-semibold hover:bg-orange-600"
          >
            List your space &#x2192;
          </a>
        </div>
      )}
    </section>
  );
}
