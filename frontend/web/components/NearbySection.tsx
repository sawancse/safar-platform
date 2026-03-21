'use client';

import { useState, useMemo } from 'react';

interface Props {
  lat?: number | null;
  lng?: number | null;
  city: string;
  state: string;
  addressLine1?: string;
  addressLine2?: string;
  pincode?: string;
}

type NearbyTab = 'places' | 'restaurants' | 'transport';

interface NearbyItem {
  icon: string;
  name: string;
  detail: string;
  distance: string;
  distanceKm: number;
}

const TABS: { id: NearbyTab; label: string; icon: string; query: string }[] = [
  { id: 'places', label: 'Places to Visit', icon: '🏛️', query: 'tourist+attractions' },
  { id: 'restaurants', label: 'Restaurants', icon: '🍽️', query: 'restaurants' },
  { id: 'transport', label: 'Transportation', icon: '🚌', query: 'transit+stations' },
];

function buildNearbyData(city: string): Record<NearbyTab, NearbyItem[]> {
  return {
    places: [
      { icon: '🏛️', name: `${city} Fort`, detail: 'Historic fort and heritage site', distance: '2.8 km', distanceKm: 2.8 },
      { icon: '🕌', name: `${city} Central Mosque`, detail: 'Architectural landmark', distance: '1.5 km', distanceKm: 1.5 },
      { icon: '🛕', name: `Sri Lakshmi Temple`, detail: 'Popular temple with daily aarti', distance: '0.8 km', distanceKm: 0.8 },
      { icon: '🏞️', name: `${city} Botanical Garden`, detail: 'Lush green park with walking trails', distance: '1.2 km', distanceKm: 1.2 },
      { icon: '🛍️', name: `${city} Central Mall`, detail: 'Shopping, dining and entertainment', distance: '3.5 km', distanceKm: 3.5 },
      { icon: '🎭', name: `${city} Heritage Museum`, detail: 'Art gallery and cultural exhibits', distance: '4.2 km', distanceKm: 4.2 },
      { icon: '🏞️', name: 'Nehru Park', detail: 'Public park with boating and play area', distance: '0.5 km', distanceKm: 0.5 },
      { icon: '🛍️', name: 'Sadar Bazaar', detail: 'Traditional market for local goods', distance: '2.0 km', distanceKm: 2.0 },
    ],
    restaurants: [
      { icon: '🍛', name: 'Local Thali House', detail: 'Authentic regional thali meals', distance: '0.3 km', distanceKm: 0.3 },
      { icon: '☕', name: 'Chai & Brew Cafe', detail: 'Coffee, chai and fresh pastries', distance: '0.5 km', distanceKm: 0.5 },
      { icon: '🍕', name: 'Multi-Cuisine Restaurant', detail: 'Indian, Chinese and Continental', distance: '1.0 km', distanceKm: 1.0 },
      { icon: '🥤', name: 'Street Food Corner', detail: 'Chaat, pani puri and local snacks', distance: '0.2 km', distanceKm: 0.2 },
      { icon: '🍛', name: 'South Indian Dosa Hub', detail: 'Dosas, idli and filter coffee', distance: '0.7 km', distanceKm: 0.7 },
      { icon: '🍗', name: 'Biryani Palace', detail: 'Dum biryani and kebab specials', distance: '1.5 km', distanceKm: 1.5 },
      { icon: '🍰', name: 'Sweet Shop & Bakery', detail: 'Indian sweets and fresh cakes', distance: '0.4 km', distanceKm: 0.4 },
      { icon: '🥗', name: 'Health Bowl Kitchen', detail: 'Salads, smoothies and light bites', distance: '1.2 km', distanceKm: 1.2 },
    ],
    transport: [
      { icon: '🚌', name: 'City Bus Stop', detail: 'Multiple bus routes available', distance: '0.2 km', distanceKm: 0.2 },
      { icon: '🚇', name: 'Metro Station', detail: 'Nearest metro line connectivity', distance: '0.3 km', distanceKm: 0.3 },
      { icon: '🛺', name: 'Auto-Rickshaw Stand', detail: 'Ola, Uber and local autos', distance: '0.1 km', distanceKm: 0.1 },
      { icon: '🚂', name: `${city} Railway Station`, detail: 'Main railway station', distance: '2.5 km', distanceKm: 2.5 },
      { icon: '✈️', name: `${city} Airport`, detail: 'Domestic and international flights', distance: '15.0 km', distanceKm: 15.0 },
      { icon: '🚕', name: 'Taxi Stand', detail: 'Prepaid taxi and cab services', distance: '0.4 km', distanceKm: 0.4 },
    ],
  };
}

export default function NearbySection({ lat, lng, city, state, addressLine1, addressLine2, pincode }: Props) {
  const [activeTab, setActiveTab] = useState<NearbyTab>('places');
  const [searchQuery, setSearchQuery] = useState('');

  const currentTab = TABS.find((t) => t.id === activeTab)!;
  const hasCoords = lat != null && lng != null && lat !== 0 && lng !== 0;
  const apiKey = process.env.NEXT_PUBLIC_GOOGLE_MAPS_KEY || '';
  const locationStr = encodeURIComponent(`${city}, ${state}, India`);

  // Build full address
  const addressParts = [addressLine1, addressLine2, city, state, pincode].filter(Boolean);
  const fullAddress = addressParts.join(', ');

  // Build map URLs
  const embedMapSrc = apiKey
    ? `https://www.google.com/maps/embed/v1/search?key=${apiKey}&q=${currentTab.query}+near+${locationStr}${hasCoords ? `&center=${lat},${lng}&zoom=14` : ''}`
    : '';

  const openStreetMapSrc = hasCoords
    ? `https://www.openstreetmap.org/export/embed.html?bbox=${lng! - 0.03},${lat! - 0.02},${lng! + 0.03},${lat! + 0.02}&layer=mapnik&marker=${lat},${lng}`
    : `https://www.openstreetmap.org/export/embed.html?bbox=72.8,18.9,72.95,19.1&layer=mapnik`;

  const directionsUrl = hasCoords
    ? `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`
    : `https://www.google.com/maps/search/?api=1&query=${locationStr}`;

  const openInMapsUrl = hasCoords
    ? `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`
    : `https://www.google.com/maps/search/?api=1&query=${locationStr}`;

  const nearbyData = useMemo(() => buildNearbyData(city), [city]);

  const filteredItems = useMemo(() => {
    const items = nearbyData[activeTab];
    if (!searchQuery.trim()) return items;
    const q = searchQuery.toLowerCase();
    return items.filter(
      (item) => item.name.toLowerCase().includes(q) || item.detail.toLowerCase().includes(q)
    );
  }, [nearbyData, activeTab, searchQuery]);

  return (
    <div>
      {/* Section heading */}
      <h2 className="text-2xl font-bold mb-1">Location</h2>

      {/* Full property address */}
      {fullAddress && (
        <div className="flex items-start gap-2 mb-5">
          <svg className="w-4 h-4 mt-0.5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <p className="text-gray-600 text-sm leading-relaxed">{fullAddress}</p>
        </div>
      )}

      {/* Map + Tabs layout */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-5">
        {/* Map column */}
        <div className="md:col-span-3">
          <div className="rounded-2xl overflow-hidden border border-gray-200 shadow-sm">
            {apiKey ? (
              <iframe
                src={embedMapSrc}
                width="100%"
                height="320"
                style={{ border: 0 }}
                allowFullScreen
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
              />
            ) : (
              <iframe
                src={openStreetMapSrc}
                width="100%"
                height="320"
                style={{ border: 0 }}
                loading="lazy"
              />
            )}
          </div>

          {/* Map action links */}
          <div className="flex items-center gap-4 mt-3">
            <a
              href={directionsUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 text-sm text-orange-600 hover:text-orange-700 font-medium transition-colors"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
              </svg>
              Get Directions
            </a>
            <a
              href={openInMapsUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700 font-medium transition-colors"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
              Open in Maps
            </a>
          </div>
        </div>

        {/* Tabs + cards column */}
        <div className="md:col-span-2">
          {/* Category tabs */}
          <div className="flex gap-1.5 mb-4">
            {TABS.map((tab) => (
              <button
                key={tab.id}
                onClick={() => { setActiveTab(tab.id); setSearchQuery(''); }}
                className={`flex-1 flex flex-col items-center gap-1 px-3 py-2.5 rounded-xl text-xs font-medium transition-all ${
                  activeTab === tab.id
                    ? 'bg-orange-50 text-orange-600 border-2 border-orange-300 shadow-sm'
                    : 'bg-gray-50 text-gray-500 border-2 border-transparent hover:bg-gray-100'
                }`}
              >
                <span className="text-lg">{tab.icon}</span>
                {tab.label}
              </button>
            ))}
          </div>

          {/* Search / filter input */}
          <div className="relative mb-3">
            <svg
              className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={`Filter ${currentTab.label.toLowerCase()}...`}
              className="w-full pl-9 pr-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-orange-200 focus:border-orange-300 bg-white placeholder-gray-400 transition-all"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            )}
          </div>

          {/* Info cards */}
          <div className="space-y-2 max-h-[340px] overflow-y-auto pr-1">
            {filteredItems.length === 0 ? (
              <div className="text-center py-6 text-gray-400 text-sm">
                No results found for &ldquo;{searchQuery}&rdquo;
              </div>
            ) : (
              filteredItems.map((item) => (
                <div
                  key={item.name}
                  className="flex items-center gap-3 p-3 rounded-xl bg-white border border-gray-100 hover:border-gray-200 hover:shadow-sm transition-all"
                >
                  <span className="text-xl w-9 h-9 flex items-center justify-center bg-gray-50 rounded-lg flex-shrink-0">
                    {item.icon}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-gray-800">{item.name}</p>
                    <p className="text-xs text-gray-500 truncate">{item.detail}</p>
                  </div>
                  <span className="text-xs font-medium text-orange-600 bg-orange-50 px-2 py-1 rounded-lg flex-shrink-0 whitespace-nowrap">
                    {item.distance}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
