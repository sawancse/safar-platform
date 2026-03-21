'use client';

import { useEffect, useState, useRef, useCallback } from 'react';
import dynamic from 'next/dynamic';

// Types for the inner map component
export interface MapLocation {
  lat: number;
  lng: number;
}

interface Props {
  lat: number;
  lng: number;
  onLocationChange: (loc: MapLocation & { address?: string; city?: string; state?: string; pincode?: string }) => void;
  className?: string;
}

// Debounce helper
function useDebounce(value: string, delay: number) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

// Reverse geocode via Nominatim
async function reverseGeocode(lat: number, lng: number) {
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&addressdetails=1`,
      { headers: { 'User-Agent': 'SafarApp/1.0' } }
    );
    if (!res.ok) return null;
    const data = await res.json();
    const addr = data.address || {};
    return {
      address: [addr.road, addr.neighbourhood, addr.suburb].filter(Boolean).join(', ') || data.display_name?.split(',')[0] || '',
      city: addr.city || addr.town || addr.village || addr.county || '',
      state: addr.state || '',
      pincode: addr.postcode || '',
    };
  } catch { return null; }
}

// Forward geocode search
async function searchLocation(query: string) {
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&countrycodes=in&limit=5&addressdetails=1`,
      { headers: { 'User-Agent': 'SafarApp/1.0' } }
    );
    if (!res.ok) return [];
    return res.json();
  } catch { return []; }
}

// The actual map component — loaded client-side only
function MapInner({ lat, lng, onLocationChange, className }: Props) {
  const [L, setL] = useState<any>(null);
  const [RL, setRL] = useState<any>(null);
  const [mounted, setMounted] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [searching, setSearching] = useState(false);
  const [locating, setLocating] = useState(false);
  const debouncedQuery = useDebounce(searchQuery, 500);
  const mapRef = useRef<any>(null);
  const markerRef = useRef<any>(null);

  // Dynamic import Leaflet (not SSR-safe)
  useEffect(() => {
    Promise.all([
      import('leaflet'),
      import('react-leaflet'),
    ]).then(([leaflet, reactLeaflet]) => {
      setL(leaflet.default || leaflet);
      setRL(reactLeaflet);
      setMounted(true);
    });
    // Inject leaflet CSS via link tag
    if (!document.getElementById('leaflet-css')) {
      const link = document.createElement('link');
      link.id = 'leaflet-css';
      link.rel = 'stylesheet';
      link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
      document.head.appendChild(link);
    }
  }, []);

  // Search on debounced query
  useEffect(() => {
    if (debouncedQuery.length < 3) { setSearchResults([]); return; }
    setSearching(true);
    searchLocation(debouncedQuery)
      .then(setSearchResults)
      .finally(() => setSearching(false));
  }, [debouncedQuery]);

  // Handle pin drag end
  const handleDragEnd = useCallback(async (e: any) => {
    const { lat: newLat, lng: newLng } = e.target.getLatLng();
    const geo = await reverseGeocode(newLat, newLng);
    onLocationChange({ lat: newLat, lng: newLng, ...geo });
  }, [onLocationChange]);

  // Handle map click
  const handleMapClick = useCallback(async (e: any) => {
    const { lat: newLat, lng: newLng } = e.latlng;
    const geo = await reverseGeocode(newLat, newLng);
    onLocationChange({ lat: newLat, lng: newLng, ...geo });
  }, [onLocationChange]);

  // Handle search result select
  const handleSelectResult = useCallback(async (result: any) => {
    const newLat = parseFloat(result.lat);
    const newLng = parseFloat(result.lon);
    const addr = result.address || {};
    onLocationChange({
      lat: newLat, lng: newLng,
      address: [addr.road, addr.neighbourhood, addr.suburb].filter(Boolean).join(', ') || '',
      city: addr.city || addr.town || addr.village || '',
      state: addr.state || '',
      pincode: addr.postcode || '',
    });
    setSearchQuery('');
    setSearchResults([]);
    if (mapRef.current) {
      mapRef.current.flyTo([newLat, newLng], 16);
    }
  }, [onLocationChange]);

  // GPS location
  const handleGPS = useCallback(() => {
    if (!navigator.geolocation) return;
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const newLat = pos.coords.latitude;
        const newLng = pos.coords.longitude;
        const geo = await reverseGeocode(newLat, newLng);
        onLocationChange({ lat: newLat, lng: newLng, ...geo });
        if (mapRef.current) mapRef.current.flyTo([newLat, newLng], 16);
        setLocating(false);
      },
      () => { setLocating(false); alert('Could not get your location'); },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }, [onLocationChange]);

  if (!mounted || !L || !RL) {
    return (
      <div className={`bg-gray-100 rounded-xl flex items-center justify-center ${className || 'h-64'}`}>
        <p className="text-sm text-gray-400">Loading map...</p>
      </div>
    );
  }

  const { MapContainer, TileLayer, Marker, useMapEvents } = RL;

  // Fix default marker icon (Leaflet issue with bundlers)
  const defaultIcon = L.icon({
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41],
  });

  // Click handler component
  function MapClickHandler() {
    useMapEvents({ click: handleMapClick });
    return null;
  }

  // Map ref setter
  function MapRefSetter() {
    const map = RL.useMap();
    useEffect(() => { mapRef.current = map; }, [map]);
    // Fly to new coords when lat/lng change from outside
    useEffect(() => {
      if (lat && lng && lat !== 0 && lng !== 0) {
        map.flyTo([lat, lng], Math.max(map.getZoom(), 14));
      }
    }, [lat, lng, map]);
    return null;
  }

  const center: [number, number] = lat && lng && lat !== 0 ? [lat, lng] : [20.5937, 78.9629]; // India center
  const zoom = lat && lng && lat !== 0 ? 15 : 5;

  return (
    <div className={`relative ${className || ''}`}>
      {/* Search bar + GPS */}
      <div className="absolute top-3 left-3 right-3 z-[1000] flex gap-2">
        <div className="relative flex-1">
          <input
            className="w-full bg-white border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none shadow-md focus:ring-2 focus:ring-orange-400 pr-8"
            placeholder="Search address or landmark..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {searching && (
            <div className="absolute right-2 top-2.5">
              <div className="w-4 h-4 border-2 border-orange-400 border-t-transparent rounded-full animate-spin" />
            </div>
          )}
          {/* Search results dropdown */}
          {searchResults.length > 0 && (
            <div className="absolute top-full left-0 right-0 mt-1 bg-white border rounded-lg shadow-xl max-h-48 overflow-y-auto z-[1001]">
              {searchResults.map((r: any, i: number) => (
                <button key={i} type="button"
                  onClick={() => handleSelectResult(r)}
                  className="w-full text-left px-3 py-2 text-sm hover:bg-gray-50 border-b last:border-0 flex items-center gap-2">
                  <span className="text-gray-400 flex-shrink-0">📍</span>
                  <span className="truncate text-gray-700">{r.display_name}</span>
                </button>
              ))}
            </div>
          )}
        </div>
        <button type="button" onClick={handleGPS} disabled={locating}
          className="bg-white border border-gray-300 rounded-lg px-3 py-2 shadow-md hover:bg-gray-50 disabled:opacity-50 transition flex-shrink-0"
          title="Use my current location">
          {locating ? (
            <div className="w-5 h-5 border-2 border-orange-400 border-t-transparent rounded-full animate-spin" />
          ) : (
            <svg className="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3A8.994 8.994 0 0013 3.06V1h-2v2.06A8.994 8.994 0 003.06 11H1v2h2.06A8.994 8.994 0 0011 20.94V23h2v-2.06A8.994 8.994 0 0020.94 13H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z" />
            </svg>
          )}
        </button>
      </div>

      {/* Map */}
      <MapContainer
        center={center}
        zoom={zoom}
        style={{ height: '100%', width: '100%', borderRadius: '0.75rem' }}
        scrollWheelZoom={true}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {lat !== 0 && lng !== 0 && (
          <Marker
            position={[lat, lng]}
            icon={defaultIcon}
            draggable={true}
            ref={markerRef}
            eventHandlers={{ dragend: handleDragEnd }}
          />
        )}
        <MapClickHandler />
        <MapRefSetter />
      </MapContainer>

      {/* Coordinates bar */}
      {lat !== 0 && lng !== 0 && (
        <div className="absolute bottom-3 left-3 right-3 z-[1000] bg-white/90 backdrop-blur rounded-lg px-3 py-2 shadow-md flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-green-600 text-sm font-semibold">✓</span>
            <span className="text-xs text-gray-600">
              {lat.toFixed(6)}, {lng.toFixed(6)}
            </span>
          </div>
          <span className="text-xs text-green-600 font-medium">Location confirmed</span>
        </div>
      )}
    </div>
  );
}

// Dynamic export — no SSR
const MapLocationPicker = dynamic(() => Promise.resolve(MapInner), {
  ssr: false,
  loading: () => (
    <div className="bg-gray-100 rounded-xl h-64 flex items-center justify-center">
      <p className="text-sm text-gray-400">Loading map...</p>
    </div>
  ),
});

export default MapLocationPicker;
