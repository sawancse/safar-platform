'use client';

import { useState } from 'react';

interface NearbyPlace {
  name: string;
  type: string; // restaurant, cafe, metro, hospital, beach, park, market
  distance: string; // "5 min walk"
  icon: string; // emoji
}

interface Props {
  address: string;
  lat: number;
  lng: number;
  neighborhoodPhotos?: string[];
  className?: string;
}

const DEFAULT_PLACES: NearbyPlace[] = [
  { name: 'Metro Station', type: 'transit', distance: '5 min walk', icon: '🚇' },
  { name: 'Restaurant', type: 'food', distance: '2 min walk', icon: '🍽️' },
  { name: 'Supermarket', type: 'shopping', distance: '8 min walk', icon: '🛒' },
  { name: 'Hospital', type: 'medical', distance: '10 min drive', icon: '🏥' },
  { name: 'ATM', type: 'finance', distance: '3 min walk', icon: '🏧' },
];

export default function NeighborhoodGallery({ address, lat, lng, neighborhoodPhotos = [], className = '' }: Props) {
  const [showMap, setShowMap] = useState(false);

  const mapUrl = `https://www.openstreetmap.org/export/embed.html?bbox=${lng-0.01},${lat-0.01},${lng+0.01},${lat+0.01}&layer=mapnik&marker=${lat},${lng}`;

  return (
    <div className={`bg-white rounded-xl border ${className}`}>
      <div className="px-6 py-4 border-b">
        <h3 className="text-lg font-semibold text-gray-900">Neighborhood</h3>
        <p className="text-sm text-gray-500 mt-1">{address}</p>
      </div>

      {/* Map */}
      <div className="relative">
        {showMap ? (
          <iframe
            src={mapUrl}
            className="w-full h-64 border-0"
            loading="lazy"
            title="Property location"
          />
        ) : (
          <button
            onClick={() => setShowMap(true)}
            className="w-full h-48 bg-gray-100 flex flex-col items-center justify-center gap-2 hover:bg-gray-200 transition-colors"
          >
            <span className="text-4xl">🗺️</span>
            <span className="text-sm font-medium text-gray-700">Click to load map</span>
          </button>
        )}
      </div>

      {/* Nearby places */}
      <div className="px-6 py-4">
        <h4 className="font-medium text-gray-900 mb-3">What&apos;s nearby</h4>
        <div className="grid grid-cols-2 gap-3">
          {DEFAULT_PLACES.map((place, i) => (
            <div key={i} className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50">
              <span className="text-xl">{place.icon}</span>
              <div>
                <p className="text-sm font-medium text-gray-900">{place.name}</p>
                <p className="text-xs text-gray-500">{place.distance}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Neighborhood photos */}
      {neighborhoodPhotos.length > 0 && (
        <div className="px-6 py-4 border-t">
          <h4 className="font-medium text-gray-900 mb-3">Neighborhood photos</h4>
          <div className="flex gap-2 overflow-x-auto pb-2">
            {neighborhoodPhotos.map((url, i) => (
              <img
                key={i}
                src={url}
                alt={`Neighborhood ${i + 1}`}
                className="w-32 h-24 rounded-lg object-cover flex-shrink-0"
                loading="lazy"
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
