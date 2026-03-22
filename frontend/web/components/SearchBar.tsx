'use client';

import { useState, useRef, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import type { LocationSuggestion } from '@/types';

/* ── Types ─────────────────────────────────────────────────── */
interface GuestCounts {
  adults: number;
  children: number;
  infants: number;
  pets: number;
}

interface RecentSearch {
  city: string;
  checkIn?: string;
  checkOut?: string;
  guests?: string;
  timestamp: number;
}

interface Suggestion {
  type: 'city' | 'locality' | 'landmark' | 'recent' | 'popular';
  label: string;
  subtitle?: string;
  icon: string;
  city: string;
  lat?: number;
  lng?: number;
  radiusKm?: number;
}

/* ── Location type config ─────────────────────────────────── */
const LOCATION_TYPE_ICONS: Record<string, string> = {
  CITY: '\u{1F3D9}\uFE0F',      // 🏙️
  LOCALITY: '\u{1F4CD}',        // 📍
  IT_PARK: '\u{1F3E2}',         // 🏢
  COLLEGE: '\u{1F393}',         // 🎓
  HOSPITAL: '\u{1F3E5}',        // 🏥
  TRANSIT: '\u{1F689}',         // 🚉
};

const LOCATION_TYPE_LABELS: Record<string, string> = {
  CITY: 'Cities',
  LOCALITY: 'Neighborhoods',
  IT_PARK: 'IT Parks',
  COLLEGE: 'Colleges & Universities',
  HOSPITAL: 'Hospitals',
  TRANSIT: 'Transit',
};

const LOCATION_TYPE_SUGGESTION: Record<string, Suggestion['type']> = {
  CITY: 'city',
  LOCALITY: 'locality',
  IT_PARK: 'landmark',
  COLLEGE: 'landmark',
  HOSPITAL: 'landmark',
  TRANSIT: 'landmark',
};

/* ── Popular Destinations (India) ──────────────────────────── */
const POPULAR_DESTINATIONS: Suggestion[] = [
  { type: 'popular', label: 'Goa', subtitle: 'Beaches, nightlife, resorts', icon: '\u{1F3D6}\uFE0F', city: 'Goa' },
  { type: 'popular', label: 'Mumbai', subtitle: 'Maharashtra, India', icon: '\u{1F3D9}\uFE0F', city: 'Mumbai' },
  { type: 'popular', label: 'Delhi', subtitle: 'Capital city, heritage', icon: '\u{1F3DB}\uFE0F', city: 'Delhi' },
  { type: 'popular', label: 'Bangalore', subtitle: 'Karnataka, tech hub', icon: '\u{1F306}', city: 'Bangalore' },
  { type: 'popular', label: 'Jaipur', subtitle: 'Rajasthan, palaces', icon: '\u{1F3F0}', city: 'Jaipur' },
  { type: 'popular', label: 'Manali', subtitle: 'Himachal Pradesh, mountains', icon: '\u{1F3D4}\uFE0F', city: 'Manali' },
  { type: 'popular', label: 'Udaipur', subtitle: 'Rajasthan, lakes', icon: '\u{1F305}', city: 'Udaipur' },
  { type: 'popular', label: 'Kochi', subtitle: 'Kerala, backwaters', icon: '\u{1F6F6}', city: 'Kochi' },
  { type: 'popular', label: 'Rishikesh', subtitle: 'Uttarakhand, yoga & adventure', icon: '\u{1F9D8}', city: 'Rishikesh' },
  { type: 'popular', label: 'Hyderabad', subtitle: 'Telangana, historic city', icon: '\u{1F54C}', city: 'Hyderabad' },
];

const CITY_ICONS: Record<string, string> = {
  goa: '\u{1F3D6}\uFE0F', mumbai: '\u{1F3D9}\uFE0F', delhi: '\u{1F3DB}\uFE0F', bangalore: '\u{1F306}', bengaluru: '\u{1F306}',
  jaipur: '\u{1F3F0}', manali: '\u{1F3D4}\uFE0F', shimla: '\u{1F3D4}\uFE0F', udaipur: '\u{1F305}', kochi: '\u{1F6F6}',
  rishikesh: '\u{1F9D8}', hyderabad: '\u{1F54C}', chennai: '\u{1F3D9}\uFE0F', kolkata: '\u{1F3D9}\uFE0F',
  pune: '\u{1F306}', ahmedabad: '\u{1F3D9}\uFE0F', varanasi: '\u{1F6D5}', agra: '\u{1F54C}', ooty: '\u{1F33F}',
  darjeeling: '\u{1F343}', leh: '\u{1F3D4}\uFE0F', pondicherry: '\u{1F3D6}\uFE0F', mysore: '\u{1F3F0}',
  jodhpur: '\u{1F3F0}', amritsar: '\u{1F6D5}', alleppey: '\u{1F6F6}', munnar: '\u{1F33F}',
  coorg: '\u{1F33F}', lonavala: '\u{1F33F}', mahabaleshwar: '\u{1F33F}', kodaikanal: '\u{1F33F}',
};

/* ── Debounce Hook ─────────────────────────────────────────── */
function useDebounce(value: string, delay: number) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}

/* ── Guest Dropdown ────────────────────────────────────────── */
function GuestDropdown({ value, onChange }: { value: GuestCounts; onChange: (v: GuestCounts) => void }) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const total = value.adults + value.children;
  const parts = [
    `${value.adults} adult${value.adults !== 1 ? 's' : ''}`,
    value.children > 0 ? `${value.children} child${value.children !== 1 ? 'ren' : ''}` : '',
    value.infants > 0 ? `${value.infants} infant${value.infants !== 1 ? 's' : ''}` : '',
    value.pets > 0 ? `${value.pets} pet${value.pets !== 1 ? 's' : ''}` : '',
  ].filter(Boolean);
  const label = total <= 1 && !value.infants && !value.pets ? '1 guest' : parts.join(', ');

  function update(field: keyof GuestCounts, delta: number) {
    const next = { ...value, [field]: Math.max(0, value[field] + delta) };
    if (next.adults < 1) next.adults = 1;
    if (next.adults + next.children > 16) return;
    if (next.infants > 5) return;
    if (next.pets > 5) return;
    onChange(next);
  }

  const rows: { key: keyof GuestCounts; label: string; subtitle: string; min: number; max: number }[] = [
    { key: 'adults', label: 'Adults', subtitle: 'Age 13+', min: 1, max: 16 - value.children },
    { key: 'children', label: 'Children', subtitle: 'Age 2-12', min: 0, max: 16 - value.adults },
    { key: 'infants', label: 'Infants', subtitle: 'Under 2', min: 0, max: 5 },
    { key: 'pets', label: 'Pets', subtitle: 'Service animals welcome', min: 0, max: 5 },
  ];

  return (
    <div ref={ref} className="relative">
      <button type="button" onClick={() => setOpen(!open)}
        className="w-full text-left px-4 py-3 text-gray-900 text-sm rounded-xl outline-none truncate">
        {label}
      </button>
      {open && (
        <div className="absolute top-full mt-2 bg-white border rounded-xl shadow-lg p-4 z-30 space-y-4 w-72
                        right-0 sm:right-0 max-sm:left-1/2 max-sm:-translate-x-1/2 max-sm:right-auto">
          {rows.map((row) => (
            <div key={row.key} className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-800">{row.label}</p>
                <p className="text-xs text-gray-400">{row.subtitle}</p>
              </div>
              <div className="flex items-center gap-3">
                <button type="button" onClick={() => update(row.key, -1)}
                  disabled={value[row.key] <= row.min}
                  className="w-8 h-8 rounded-full border border-gray-300 text-gray-600 flex items-center justify-center hover:border-gray-500 disabled:opacity-30 disabled:cursor-not-allowed transition">
                  -
                </button>
                <span className="w-6 text-center text-sm font-medium">{value[row.key]}</span>
                <button type="button" onClick={() => update(row.key, 1)}
                  disabled={value[row.key] >= row.max}
                  className="w-8 h-8 rounded-full border border-gray-300 text-gray-600 flex items-center justify-center hover:border-gray-500 disabled:opacity-30 disabled:cursor-not-allowed transition">
                  +
                </button>
              </div>
            </div>
          ))}
          <button type="button" onClick={() => setOpen(false)}
            className="w-full text-sm font-semibold text-orange-600 hover:text-orange-700 py-1">
            Done
          </button>
        </div>
      )}
    </div>
  );
}

/* ── Search Suggestion Dropdown ────────────────────────────── */
function SuggestionDropdown({
  query, focused, onSelect, onClose,
}: {
  query: string;
  focused: boolean;
  onSelect: (suggestion: Suggestion) => void;
  onClose: () => void;
}) {
  const [apiResults, setApiResults] = useState<Record<string, LocationSuggestion[]>>({});
  const [loading, setLoading] = useState(false);
  const [recentSearches, setRecentSearches] = useState<RecentSearch[]>([]);
  const [nearMeLoading, setNearMeLoading] = useState(false);
  const [nearMeError, setNearMeError] = useState('');
  const debouncedQuery = useDebounce(query, 250);
  const ref = useRef<HTMLDivElement>(null);
  const [activeIdx, setActiveIdx] = useState(-1);

  // Load recent searches
  useEffect(() => {
    try {
      const stored = JSON.parse(localStorage.getItem('recent_searches') || '[]');
      setRecentSearches(stored.slice(0, 5));
    } catch { setRecentSearches([]); }
  }, [focused]);

  // Fetch location suggestions from API
  useEffect(() => {
    if (!debouncedQuery || debouncedQuery.length < 2) {
      setApiResults({});
      return;
    }
    setLoading(true);
    api.locationSuggest(debouncedQuery)
      .then(results => setApiResults(results || {}))
      .catch(() => {
        // Fallback to old autocomplete API
        api.autocomplete(debouncedQuery)
          .then(cities => {
            const fallback: Record<string, LocationSuggestion[]> = {};
            if (cities.length > 0) {
              fallback['CITY'] = cities.slice(0, 8).map(c => ({
                id: c,
                name: c,
                displayName: c + ', India',
                type: 'CITY',
                city: c,
                lat: 0,
                lng: 0,
                defaultRadiusKm: 0,
              }));
            }
            setApiResults(fallback);
          })
          .catch(() => setApiResults({}));
      })
      .finally(() => setLoading(false));
  }, [debouncedQuery]);

  // Reset active index on results change
  useEffect(() => { setActiveIdx(-1); }, [apiResults, query]);

  // Clear near-me error after 3 seconds
  useEffect(() => {
    if (nearMeError) {
      const timer = setTimeout(() => setNearMeError(''), 3000);
      return () => clearTimeout(timer);
    }
  }, [nearMeError]);

  // Property type keywords for smart suggestions (matches backend SmartQueryParser)
  const TYPE_KEYWORDS: Record<string, { label: string; type: string; icon: string }> = {
    'pg': { label: 'PG (Paying Guest)', type: 'PG', icon: '🏘️' },
    'paying guest': { label: 'PG (Paying Guest)', type: 'PG', icon: '🏘️' },
    'coliving': { label: 'Co-living', type: 'COLIVING', icon: '🏘️' },
    'co-living': { label: 'Co-living', type: 'COLIVING', icon: '🏘️' },
    'co living': { label: 'Co-living', type: 'COLIVING', icon: '🏘️' },
    'hotel': { label: 'Hotel', type: 'HOTEL', icon: '🏨' },
    'hotels': { label: 'Hotel', type: 'HOTEL', icon: '🏨' },
    'budget hotel': { label: 'Budget Hotel', type: 'BUDGET_HOTEL', icon: '🏨' },
    'hostel': { label: 'Hostel', type: 'HOSTEL', icon: '🛏️' },
    'hostels': { label: 'Hostel', type: 'HOSTEL', icon: '🛏️' },
    'villa': { label: 'Villa', type: 'VILLA', icon: '🏡' },
    'villas': { label: 'Villa', type: 'VILLA', icon: '🏡' },
    'resort': { label: 'Resort', type: 'RESORT', icon: '🏖️' },
    'resorts': { label: 'Resort', type: 'RESORT', icon: '🏖️' },
    'homestay': { label: 'Homestay', type: 'HOMESTAY', icon: '🏠' },
    'homestays': { label: 'Homestay', type: 'HOMESTAY', icon: '🏠' },
    'apartment': { label: 'Apartment', type: 'APARTMENT', icon: '🏢' },
    'apartments': { label: 'Apartment', type: 'APARTMENT', icon: '🏢' },
    'flat': { label: 'Apartment', type: 'APARTMENT', icon: '🏢' },
    'flats': { label: 'Apartment', type: 'APARTMENT', icon: '🏢' },
    'farmstay': { label: 'Farmstay', type: 'FARMSTAY', icon: '🌾' },
    'farm stay': { label: 'Farmstay', type: 'FARMSTAY', icon: '🌾' },
    'guesthouse': { label: 'Guesthouse', type: 'GUESTHOUSE', icon: '🏡' },
    'guest house': { label: 'Guesthouse', type: 'GUESTHOUSE', icon: '🏡' },
    'home': { label: 'Home', type: 'HOME', icon: '🏠' },
    'homes': { label: 'Home', type: 'HOME', icon: '🏠' },
    'house': { label: 'Home', type: 'HOME', icon: '🏠' },
    'room': { label: 'Room', type: 'ROOM', icon: '🚪' },
    'rooms': { label: 'Room', type: 'ROOM', icon: '🚪' },
    'cottage': { label: 'Cottage', type: 'COTTAGE', icon: '🏡' },
    'lodge': { label: 'Lodge', type: 'LODGE', icon: '🏨' },
    'unique': { label: 'Unique Stay', type: 'UNIQUE', icon: '✨' },
    'unique stay': { label: 'Unique Stay', type: 'UNIQUE', icon: '✨' },
    'commercial': { label: 'Commercial Space', type: 'COMMERCIAL', icon: '🏢' },
    'office': { label: 'Commercial Space', type: 'COMMERCIAL', icon: '🏢' },
  };

  // Build suggestions list
  const suggestions: Suggestion[] = [];

  // Check if query matches a property type keyword
  const queryLower = query.toLowerCase().trim();
  const matchedType = TYPE_KEYWORDS[queryLower];
  if (matchedType && query.length >= 2) {
    suggestions.push({
      type: 'type' as any,
      label: `Search all ${matchedType.label} listings`,
      subtitle: 'All cities',
      icon: matchedType.icon,
      city: '',
    });
  }

  if (query.length >= 2 && Object.keys(apiResults).length > 0) {
    // Grouped API results - maintain order: CITY, LOCALITY, IT_PARK, COLLEGE, HOSPITAL, TRANSIT
    const typeOrder = ['CITY', 'LOCALITY', 'IT_PARK', 'COLLEGE', 'HOSPITAL', 'TRANSIT'];
    for (const locType of typeOrder) {
      const items = apiResults[locType];
      if (!items || items.length === 0) continue;
      for (const item of items) {
        const icon = LOCATION_TYPE_ICONS[locType] || '\u{1F4CD}';
        const suggType = LOCATION_TYPE_SUGGESTION[locType] || 'landmark';
        const subtitle = item.state ? `${item.city}, ${item.state}` : item.city;
        suggestions.push({
          type: suggType,
          label: item.displayName || item.name,
          subtitle: locType === 'CITY' ? (item.state || 'India') : subtitle,
          icon,
          city: item.city,
          lat: item.lat || undefined,
          lng: item.lng || undefined,
          radiusKm: item.defaultRadiusKm || undefined,
        });
      }
    }
  } else if (query.length >= 2 && !loading) {
    // Filter popular destinations by query
    POPULAR_DESTINATIONS
      .filter(d => d.label.toLowerCase().includes(query.toLowerCase()))
      .forEach(d => suggestions.push(d));
  }

  if (query.length < 2) {
    // Show recent searches first
    if (recentSearches.length > 0) {
      recentSearches.forEach(rs => {
        const icon = CITY_ICONS[rs.city.toLowerCase()] || '\u{1F550}';
        suggestions.push({
          type: 'recent', label: rs.city,
          subtitle: rs.checkIn ? `${rs.checkIn}${rs.checkOut ? ' \u2192 ' + rs.checkOut : ''}` : 'Recent search',
          icon, city: rs.city,
        });
      });
    }

    // Then popular destinations
    POPULAR_DESTINATIONS.slice(0, 6).forEach(d => {
      if (!suggestions.some(s => s.city.toLowerCase() === d.city.toLowerCase())) {
        suggestions.push(d);
      }
    });
  }

  if (!focused) return null;
  // Show dropdown even if suggestions empty (for Near Me button)

  function handleNearMe() {
    if (!navigator.geolocation) {
      setNearMeError('Geolocation is not supported by your browser');
      return;
    }
    setNearMeLoading(true);
    setNearMeError('');
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setNearMeLoading(false);
        onSelect({
          type: 'landmark',
          label: 'Near Me',
          subtitle: 'Current location',
          icon: '\u{1F4CD}',
          city: '',
          lat: position.coords.latitude,
          lng: position.coords.longitude,
          radiusKm: 5,
        });
      },
      (error) => {
        setNearMeLoading(false);
        switch (error.code) {
          case error.PERMISSION_DENIED:
            setNearMeError('Location permission denied');
            break;
          case error.POSITION_UNAVAILABLE:
            setNearMeError('Location unavailable');
            break;
          case error.TIMEOUT:
            setNearMeError('Location request timed out');
            break;
          default:
            setNearMeError('Could not get your location');
        }
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 300000 }
    );
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiveIdx(prev => Math.min(prev + 1, suggestions.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiveIdx(prev => Math.max(prev - 1, -1));
    } else if (e.key === 'Enter' && activeIdx >= 0) {
      e.preventDefault();
      onSelect(suggestions[activeIdx]);
    } else if (e.key === 'Escape') {
      onClose();
    }
  }

  function clearRecent(city: string, e: React.MouseEvent) {
    e.stopPropagation();
    try {
      const stored = JSON.parse(localStorage.getItem('recent_searches') || '[]');
      const filtered = stored.filter((s: RecentSearch) => s.city.toLowerCase() !== city.toLowerCase());
      localStorage.setItem('recent_searches', JSON.stringify(filtered));
      setRecentSearches(filtered);
    } catch {}
  }

  // Determine section headers based on suggestion types
  // For API results with grouped location types, use location type headers
  const hasApiGroupedResults = query.length >= 2 && Object.keys(apiResults).length > 0;

  let currentSection = '';

  return (
    <div ref={ref}
      className="absolute top-full left-0 right-0 mt-2 bg-white border border-gray-200 rounded-2xl shadow-2xl z-50 overflow-hidden max-h-[400px] overflow-y-auto"
      onKeyDown={handleKeyDown}>

      {/* Near Me button */}
      <button type="button" onClick={handleNearMe}
        disabled={nearMeLoading}
        className="w-full text-left px-4 py-2.5 flex items-center gap-3 hover:bg-gray-50 transition-colors border-b border-gray-100">
        <span className="text-xl w-8 h-8 flex items-center justify-center bg-orange-50 rounded-lg flex-shrink-0">
          {nearMeLoading ? (
            <div className="w-4 h-4 border-2 border-orange-400 border-t-transparent rounded-full animate-spin" />
          ) : (
            <svg className="w-5 h-5 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          )}
        </span>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-orange-600">
            {nearMeLoading ? 'Getting your location...' : 'Near Me'}
          </p>
          <p className="text-xs text-gray-400">Find stays near your current location</p>
        </div>
      </button>

      {/* Near Me error */}
      {nearMeError && (
        <div className="px-4 py-2 text-xs text-red-500 bg-red-50 flex items-center gap-2">
          <svg className="w-3.5 h-3.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
          </svg>
          {nearMeError}
        </div>
      )}

      {loading && (
        <div className="px-4 py-3 text-sm text-gray-400 flex items-center gap-2">
          <div className="w-4 h-4 border-2 border-orange-400 border-t-transparent rounded-full animate-spin" />
          Searching...
        </div>
      )}
      {suggestions.map((suggestion, idx) => {
        let sectionHeader = null;

        if (hasApiGroupedResults) {
          // Use location-type-based section headers
          // Determine the location type for this suggestion based on its position
          let sectionLabel = '';
          if (suggestion.type === 'city') sectionLabel = LOCATION_TYPE_LABELS['CITY'] || 'Cities';
          else if (suggestion.type === 'locality') sectionLabel = LOCATION_TYPE_LABELS['LOCALITY'] || 'Neighborhoods';
          else if (suggestion.type === 'landmark') {
            // Determine specific landmark type from icon
            const reverseIcon = Object.entries(LOCATION_TYPE_ICONS).find(([, v]) => v === suggestion.icon);
            if (reverseIcon) {
              sectionLabel = LOCATION_TYPE_LABELS[reverseIcon[0]] || reverseIcon[0];
            } else {
              sectionLabel = 'Landmarks';
            }
          }

          if (sectionLabel && sectionLabel !== currentSection) {
            currentSection = sectionLabel;
            sectionHeader = (
              <div className="px-4 pt-3 pb-1">
                <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide">{sectionLabel}</p>
              </div>
            );
          }
        } else {
          // Use type-based section headers (recent, popular, etc.)
          const sectionLabel =
            suggestion.type === 'recent' ? 'Recent searches' :
            suggestion.type === 'popular' ? 'Popular destinations' :
            suggestion.type === 'city' ? 'Destinations' : '';

          if (sectionLabel && sectionLabel !== currentSection) {
            currentSection = sectionLabel;
            sectionHeader = (
              <div className="px-4 pt-3 pb-1 flex items-center justify-between">
                <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide">{sectionLabel}</p>
                {suggestion.type === 'recent' && (
                  <button type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      localStorage.removeItem('recent_searches');
                      setRecentSearches([]);
                    }}
                    className="text-xs text-orange-500 hover:text-orange-600 font-medium">
                    Clear all
                  </button>
                )}
              </div>
            );
          }
        }

        return (
          <div key={`${suggestion.type}-${suggestion.label}-${idx}`}>
            {sectionHeader}
            <button type="button"
              onClick={() => onSelect(suggestion)}
              className={`w-full text-left px-4 py-2.5 flex items-center gap-3 transition-colors ${
                idx === activeIdx ? 'bg-orange-50' : 'hover:bg-gray-50'
              }`}>
              <span className="text-xl w-8 h-8 flex items-center justify-center bg-gray-100 rounded-lg flex-shrink-0">
                {suggestion.icon}
              </span>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">
                  {query.length >= 2 ? highlightMatch(suggestion.label, query) : suggestion.label}
                </p>
                {suggestion.subtitle && (
                  <p className="text-xs text-gray-400 truncate">{suggestion.subtitle}</p>
                )}
              </div>
              {suggestion.type === 'recent' && (
                <button type="button"
                  onClick={(e) => clearRecent(suggestion.city, e)}
                  className="text-gray-300 hover:text-gray-500 p-1 flex-shrink-0"
                  title="Remove">
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              )}
            </button>
          </div>
        );
      })}
      {!loading && suggestions.length === 0 && query.length >= 2 && (
        <div className="px-4 py-3 text-sm text-gray-400">No results found</div>
      )}
    </div>
  );
}

function highlightMatch(text: string, query: string) {
  const idx = text.toLowerCase().indexOf(query.toLowerCase());
  if (idx === -1) return text;
  return (
    <>
      {text.slice(0, idx)}
      <span className="font-bold text-orange-600">{text.slice(idx, idx + query.length)}</span>
      {text.slice(idx + query.length)}
    </>
  );
}

/* ── Main SearchBar ────────────────────────────────────────── */
export default function SearchBar() {
  const router = useRouter();
  const [city, setCity] = useState('');
  const [checkIn, setCheckIn] = useState('');
  const [checkOut, setCheckOut] = useState('');
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [calMonth, setCalMonth] = useState(() => new Date());
  const datePickerRef = useRef<HTMLDivElement>(null);
  const [guestCounts, setGuestCounts] = useState<GuestCounts>({
    adults: 1, children: 0, infants: 0, pets: 0,
  });
  const [cityFocused, setCityFocused] = useState(false);
  const [selectedLocation, setSelectedLocation] = useState<{
    lat?: number;
    lng?: number;
    radiusKm?: number;
  } | null>(null);
  const wrapperRef = useRef<HTMLDivElement>(null);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setCityFocused(false);
      }
      if (datePickerRef.current && !datePickerRef.current.contains(e.target as Node)) {
        setShowDatePicker(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  function handleSelectLocation(suggestion: Suggestion) {
    // Type keyword suggestion — trigger search immediately
    if ((suggestion as any).type === 'type') {
      setCityFocused(false);
      setCity(city); // keep the typed query
      setSelectedLocation(null);
      // Navigate to search with the type query
      const params = new URLSearchParams();
      params.set('q', city.trim());
      if (checkIn) params.set('checkIn', checkIn);
      if (checkOut) params.set('checkOut', checkOut);
      params.set('guests', String(guestCounts.adults + guestCounts.children));
      router.push(`/search?${params.toString()}`);
      return;
    }

    setCity(suggestion.label);
    setCityFocused(false);

    // Store lat/lng for non-city selections
    if (suggestion.lat && suggestion.lng) {
      setSelectedLocation({
        lat: suggestion.lat,
        lng: suggestion.lng,
        radiusKm: suggestion.radiusKm,
      });
    } else {
      setSelectedLocation(null);
    }

    // Focus the check-in date input after selection
    const checkInInput = document.querySelector<HTMLInputElement>('input[data-field="checkin"]');
    if (checkInInput) checkInInput.focus();
  }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    setCityFocused(false);
    const params = new URLSearchParams();

    // If a location with lat/lng was selected, use geo search
    if (selectedLocation?.lat && selectedLocation?.lng) {
      params.set('lat', String(selectedLocation.lat));
      params.set('lng', String(selectedLocation.lng));
      if (selectedLocation.radiusKm) {
        params.set('radiusKm', String(selectedLocation.radiusKm));
      }
      params.set('sort', 'distance_asc');
    } else if (city.trim()) {
      params.set('city', city.trim());
    }

    if (checkIn) params.set('checkIn', checkIn);
    if (checkOut) params.set('checkOut', checkOut);
    const totalGuests = guestCounts.adults + guestCounts.children;
    if (totalGuests > 1) params.set('guests', String(totalGuests));
    if (guestCounts.children > 0) params.set('children', String(guestCounts.children));
    if (guestCounts.infants > 0) params.set('infants', String(guestCounts.infants));
    if (guestCounts.pets > 0) params.set('pets', String(guestCounts.pets));

    // Save to recent searches
    if (city.trim()) {
      try {
        const stored = JSON.parse(localStorage.getItem('recent_searches') || '[]');
        const entry: RecentSearch = { city: city.trim(), checkIn, checkOut, guests: String(totalGuests), timestamp: Date.now() };
        const filtered = stored.filter((s: RecentSearch) => s.city.toLowerCase() !== city.trim().toLowerCase());
        filtered.unshift(entry);
        localStorage.setItem('recent_searches', JSON.stringify(filtered.slice(0, 5)));
      } catch {}
    }

    router.push(`/search?${params.toString()}`);
  }

  // Clear stored location when user types manually
  function handleCityInputChange(value: string) {
    setCity(value);
    if (selectedLocation) {
      setSelectedLocation(null);
    }
  }

  return (
    <form onSubmit={handleSearch}
      className="bg-white rounded-2xl shadow-xl p-2 flex flex-col sm:flex-row gap-2">
      {/* City input with suggestions */}
      <div ref={wrapperRef} className="relative flex-1">
        <div className="flex items-center">
          <span className="pl-3 text-gray-400">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </span>
          <input
            className="flex-1 px-3 py-3 text-gray-900 text-sm rounded-xl outline-none placeholder-gray-400"
            placeholder="Where are you going?"
            value={city}
            onChange={(e) => handleCityInputChange(e.target.value)}
            onFocus={() => setCityFocused(true)}
            autoComplete="off"
          />
          {city && (
            <button type="button" onClick={() => { setCity(''); setSelectedLocation(null); setCityFocused(true); }}
              className="pr-2 text-gray-300 hover:text-gray-500">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          )}
        </div>
        <SuggestionDropdown
          query={city}
          focused={cityFocused}
          onSelect={handleSelectLocation}
          onClose={() => setCityFocused(false)}
        />
      </div>

      {/* Divider */}
      <div className="hidden sm:block w-px bg-gray-200 my-2" />

      {/* ── Airbnb-style unified date range picker ── */}
      <div ref={datePickerRef} className="relative">
        <button type="button" onClick={() => { setShowDatePicker(!showDatePicker); setCityFocused(false); }}
          className="flex items-center gap-2 px-3 py-3 text-sm text-gray-700 hover:bg-gray-50 rounded-xl transition">
          <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <span className={checkIn ? 'text-gray-900 font-medium' : 'text-gray-400'}>
            {checkIn ? new Date(checkIn + 'T00:00').toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : 'Check-in'}
          </span>
          <span className="text-gray-300">—</span>
          <span className={checkOut ? 'text-gray-900 font-medium' : 'text-gray-400'}>
            {checkOut ? new Date(checkOut + 'T00:00').toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : 'Check-out'}
          </span>
          {checkIn && checkOut && (
            <span className="text-xs text-orange-500 font-semibold">
              {Math.ceil((new Date(checkOut).getTime() - new Date(checkIn).getTime()) / 86400000)}N
            </span>
          )}
        </button>

        {showDatePicker && (
          <div className="absolute top-full left-1/2 -translate-x-1/2 mt-2 bg-white border border-gray-200 rounded-2xl shadow-2xl z-50 p-5 w-[340px] sm:w-[620px]">
            {/* Month navigation */}
            <div className="flex items-center justify-between mb-4">
              <button type="button" onClick={() => setCalMonth(m => { const d = new Date(m); d.setMonth(d.getMonth() - 1); return d; })}
                className="w-8 h-8 rounded-full hover:bg-gray-100 flex items-center justify-center text-gray-500">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
              </button>
              <div className="flex gap-8">
                <span className="text-sm font-semibold text-gray-800">
                  {calMonth.toLocaleDateString('en-IN', { month: 'long', year: 'numeric' })}
                </span>
                <span className="text-sm font-semibold text-gray-800 hidden sm:inline">
                  {new Date(calMonth.getFullYear(), calMonth.getMonth() + 1).toLocaleDateString('en-IN', { month: 'long', year: 'numeric' })}
                </span>
              </div>
              <button type="button" onClick={() => setCalMonth(m => { const d = new Date(m); d.setMonth(d.getMonth() + 1); return d; })}
                className="w-8 h-8 rounded-full hover:bg-gray-100 flex items-center justify-center text-gray-500">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
              </button>
            </div>

            <div className="flex gap-6">
              {[0, 1].map(offset => {
                const month = new Date(calMonth.getFullYear(), calMonth.getMonth() + offset, 1);
                const daysInMonth = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
                const startDay = month.getDay();
                const today = new Date().toISOString().split('T')[0];
                return (
                  <div key={offset} className={`flex-1 ${offset === 1 ? 'hidden sm:block' : ''}`}>
                    <div className="grid grid-cols-7 gap-0 mb-1">
                      {['Su','Mo','Tu','We','Th','Fr','Sa'].map(d => (
                        <div key={d} className="text-xs text-gray-400 text-center py-1 font-medium">{d}</div>
                      ))}
                    </div>
                    <div className="grid grid-cols-7 gap-0">
                      {Array.from({ length: startDay }).map((_, i) => <div key={`e-${i}`} />)}
                      {Array.from({ length: daysInMonth }).map((_, i) => {
                        const day = i + 1;
                        const dateStr = `${month.getFullYear()}-${String(month.getMonth() + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
                        const isPast = dateStr < today;
                        const isCheckIn = dateStr === checkIn;
                        const isCheckOut = dateStr === checkOut;
                        const isInRange = checkIn && checkOut && dateStr > checkIn && dateStr < checkOut;
                        const isSelected = isCheckIn || isCheckOut;
                        return (
                          <button key={day} type="button" disabled={isPast}
                            onClick={() => {
                              if (!checkIn || (checkIn && checkOut) || dateStr <= checkIn) {
                                setCheckIn(dateStr);
                                setCheckOut('');
                              } else {
                                setCheckOut(dateStr);
                                setShowDatePicker(false);
                              }
                            }}
                            className={`h-9 w-full text-sm rounded-full transition-all
                              ${isPast ? 'text-gray-200 cursor-not-allowed' : 'hover:bg-gray-100 cursor-pointer'}
                              ${isSelected ? 'bg-orange-500 text-white font-bold hover:bg-orange-600' : ''}
                              ${isInRange ? 'bg-orange-50 text-orange-700' : ''}
                              ${!isSelected && !isInRange && !isPast ? 'text-gray-700' : ''}
                            `}>
                            {day}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="flex items-center justify-between mt-4 pt-3 border-t border-gray-100">
              <div className="text-xs text-gray-500">
                {checkIn && !checkOut && 'Select check-out date'}
                {checkIn && checkOut && `${Math.ceil((new Date(checkOut).getTime() - new Date(checkIn).getTime()) / 86400000)} nights`}
                {!checkIn && 'Select check-in date'}
              </div>
              <div className="flex gap-2">
                <button type="button" onClick={() => { setCheckIn(''); setCheckOut(''); }}
                  className="text-xs text-gray-500 hover:text-gray-700 underline">Clear</button>
                <button type="button" onClick={() => setShowDatePicker(false)}
                  className="text-xs font-semibold text-orange-600 hover:text-orange-700">Done</button>
              </div>
            </div>
          </div>
        )}
      </div>

      <div className="hidden sm:block w-px bg-gray-200 my-2" />

      {/* Guests (fixed overflow) */}
      <GuestDropdown value={guestCounts} onChange={setGuestCounts} />

      {/* Search button */}
      <button type="submit"
        className="bg-orange-500 hover:bg-orange-600 text-white font-semibold px-6 py-3 rounded-xl transition whitespace-nowrap flex items-center gap-2">
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        Search
      </button>
    </form>
  );
}
