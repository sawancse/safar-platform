'use client';

import { useState, useEffect } from 'react';
import { api } from '@/lib/api';

interface LocalityMapOverlayProps {
  city: string;
  selectedLocality?: string;
  onSelectLocality: (locality: string, polygon?: any) => void;
}

export default function LocalityMapOverlay({ city, selectedLocality, onSelectLocality }: LocalityMapOverlayProps) {
  const [localities, setLocalities] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [polygonData, setPolygonData] = useState<any>(null);

  useEffect(() => {
    if (city) loadLocalities();
  }, [city]);

  useEffect(() => {
    if (selectedLocality && city) loadPolygon();
  }, [selectedLocality]);

  async function loadLocalities() {
    setLoading(true);
    try {
      const data = await api.getLocalitiesByCity(city);
      setLocalities(data || []);
    } catch { setLocalities([]); }
    setLoading(false);
  }

  async function loadPolygon() {
    try {
      const data = await api.getLocalityByName(selectedLocality!, city);
      if (data?.boundaryGeoJson) {
        setPolygonData(JSON.parse(data.boundaryGeoJson));
      }
    } catch { setPolygonData(null); }
  }

  return (
    <div className="space-y-3">
      {/* Locality Selector */}
      <div>
        <label className="text-sm font-medium text-gray-700 block mb-1">Locality</label>
        <select
          value={selectedLocality || ''}
          onChange={e => onSelectLocality(e.target.value, polygonData)}
          className="w-full border rounded-lg px-3 py-2 text-sm"
        >
          <option value="">All areas in {city}</option>
          {loading ? (
            <option disabled>Loading localities...</option>
          ) : (
            localities.map((loc: any) => (
              <option key={loc.id} value={loc.name}>
                {loc.name} ({loc.listingCount} listings)
              </option>
            ))
          )}
        </select>
      </div>

      {/* Polygon info */}
      {selectedLocality && polygonData && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm">
          <div className="flex items-center gap-2 text-blue-700">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
            </svg>
            Showing results within {selectedLocality} boundary
          </div>
        </div>
      )}

      {/* Quick locality chips */}
      {localities.length > 0 && !selectedLocality && (
        <div className="flex flex-wrap gap-2">
          {localities.slice(0, 8).map((loc: any) => (
            <button key={loc.id}
              onClick={() => onSelectLocality(loc.name)}
              className="text-xs bg-white border rounded-full px-3 py-1 hover:bg-gray-50 transition-colors">
              {loc.name}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
