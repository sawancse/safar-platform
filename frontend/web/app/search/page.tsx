'use client';

import { useSearchParams, useRouter } from 'next/navigation';
import { useEffect, useState, useCallback, useRef } from 'react';
import { api } from '@/lib/api';
import ListingCard from '@/components/ListingCard';
import type { Listing, FilterAggregations } from '@/types';

/* ── Debounce hook ─────────────────────────────────────────── */
function useDebounce(value: string, delay: number) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

const CITY_ICONS: Record<string, string> = {
  goa: '🏖️', mumbai: '🏙️', delhi: '🏛️', bangalore: '🌆', bengaluru: '🌆',
  jaipur: '🏰', manali: '🏔️', shimla: '🏔️', udaipur: '🌅', kochi: '🛶',
  rishikesh: '🧘', hyderabad: '🕌', chennai: '🏙️', kolkata: '🏙️',
  pune: '🌆', ahmedabad: '🏙️', varanasi: '🛕', agra: '🕌', ooty: '🌿',
};

const POPULAR_CITIES = [
  { label: 'Goa', subtitle: 'Beaches & resorts', icon: '🏖️' },
  { label: 'Mumbai', subtitle: 'Maharashtra', icon: '🏙️' },
  { label: 'Delhi', subtitle: 'Capital city', icon: '🏛️' },
  { label: 'Jaipur', subtitle: 'Rajasthan', icon: '🏰' },
  { label: 'Manali', subtitle: 'Himachal Pradesh', icon: '🏔️' },
  { label: 'Bangalore', subtitle: 'Karnataka', icon: '🌆' },
];

interface GuestCounts {
  adults: number;
  children: number;
  infants: number;
  pets: number;
}

const SORT_OPTIONS = [
  { value: '', label: 'Relevance' },
  { value: 'price_asc', label: 'Price: Low to High' },
  { value: 'price_desc', label: 'Price: High to Low' },
  { value: 'rating_desc', label: 'Top Rated' },
  { value: 'newest', label: 'Newest' },
];

const TYPE_LABELS: Record<string, string> = {
  HOME: 'Entire Home', ROOM: 'Private Room', UNIQUE: 'Unique Stay',
  COMMERCIAL: 'Commercial', VILLA: 'Villa', RESORT: 'Resort',
  HOMESTAY: 'Homestay', HOSTEL: 'Hostel', GUESTHOUSE: 'Guesthouse',
  FARMSTAY: 'Farm Stay', BNB: 'Bed & Breakfast', LODGE: 'Lodge',
  CHALET: 'Chalet', APARTMENT: 'Apartment', VACATION_HOME: 'Vacation Home',
  PG: 'Paying Guest', COLIVING: 'Co-living', HOTEL: 'Hotel',
  BUDGET_HOTEL: 'Budget Hotel', HOSTEL_DORM: 'Hostel/Dorm',
};

const MEAL_LABELS: Record<string, string> = {
  NONE: 'No meals', BREAKFAST: 'Breakfast included', HALF_BOARD: 'Breakfast & dinner',
  FULL_BOARD: 'All meals included', ALL_INCLUSIVE: 'All-inclusive',
  KITCHEN_AVAILABLE: 'Kitchen facilities',
};

const AMENITY_CATEGORIES: Record<string, { label: string; items: string[] }> = {
  popular: {
    label: 'Popular Filters',
    items: ['wifi', 'ac', 'parking', 'pool', 'kitchen', 'tv', 'hot_water', 'power_backup'],
  },
  facilities: {
    label: 'Facilities',
    items: ['parking', 'gym', 'restaurant', 'room_service', 'elevator', 'security', 'cctv', 'laundry'],
  },
  room: {
    label: 'Room Facilities',
    items: ['ac', 'wifi', 'tv', 'balcony', 'workspace', 'refrigerator', 'microwave', 'iron', 'hair_dryer', 'safe'],
  },
  safety: {
    label: 'Safety',
    items: ['fire_extinguisher', 'smoke_detector', 'first_aid', 'cctv'],
  },
};

const AMENITY_LABELS: Record<string, string> = {
  wifi: 'WiFi', free_wifi: 'Free WiFi', ac: 'Air Conditioning', air_conditioning: 'AC',
  parking: 'Parking', free_parking: 'Free Parking', pool: 'Swimming Pool', swimming_pool: 'Pool',
  gym: 'Fitness Center', fitness_center: 'Gym', kitchen: 'Kitchen', kitchenette: 'Kitchenette',
  tv: 'TV', smart_tv: 'Smart TV', hot_water: 'Hot Water', geyser: 'Geyser',
  power_backup: 'Power Backup', inverter: 'Inverter', balcony: 'Balcony', terrace: 'Terrace',
  garden: 'Garden', elevator: 'Elevator', lift: 'Lift', security: 'Security', cctv: 'CCTV',
  laundry: 'Laundry', washer: 'Washing Machine', workspace: 'Workspace', desk: 'Work Desk',
  restaurant: 'Restaurant', room_service: 'Room Service', breakfast: 'Breakfast', meals: 'Meals',
  iron: 'Iron', hair_dryer: 'Hair Dryer', refrigerator: 'Refrigerator', fridge: 'Fridge',
  microwave: 'Microwave', safe: 'Safe/Locker', fire_extinguisher: 'Fire Extinguisher',
  smoke_detector: 'Smoke Detector', first_aid: 'First Aid Kit', coffee: 'Coffee/Tea Maker',
  water_purifier: 'Water Purifier', ro_water: 'RO Water', mosquito_net: 'Mosquito Net',
  fan: 'Ceiling Fan', pet_friendly: 'Pet Friendly',
};

const BED_TYPE_LABELS: Record<string, string> = {
  double_bed: 'Double Bed', twin_beds: 'Twin Beds', king_bed: 'King Bed',
  queen_bed: 'Queen Bed', single_bed: 'Single Bed', sofa_bed: 'Sofa Bed', cribs: 'Cribs',
  bunk_bed: 'Bunk Bed',
};

const ACCESSIBILITY_LABELS: Record<string, string> = {
  wheelchair_accessible: 'Wheelchair Accessible', elevator_access: 'Upper Floors by Elevator',
  ground_floor: 'Ground Floor Unit', grab_rails: 'Toilet Grab Rails',
  raised_toilet: 'Raised Toilet', lowered_sink: 'Lowered Sink',
  roll_in_shower: 'Roll-in Shower', walk_in_shower: 'Walk-in Shower',
  shower_chair: 'Shower Chair', braille: 'Visual Aids (Braille)',
  tactile_signs: 'Tactile Signs', auditory_guidance: 'Auditory Guidance',
  emergency_cord: 'Emergency Cord in Bathroom', adapted_bath: 'Adapted Bath',
};

const RATING_OPTIONS = [
  { key: '9+', label: 'Wonderful: 9+', value: 9 },
  { key: '8+', label: 'Very Good: 8+', value: 8 },
  { key: '7+', label: 'Good: 7+', value: 7 },
  { key: '6+', label: 'Pleasant: 6+', value: 6 },
];

const PRICE_RANGES = [
  { key: '0-1000', label: 'Under ₹1,000', min: '', max: '1000' },
  { key: '1000-3000', label: '₹1,000 – ₹3,000', min: '1000', max: '3000' },
  { key: '3000-5000', label: '₹3,000 – ₹5,000', min: '3000', max: '5000' },
  { key: '5000-10000', label: '₹5,000 – ₹10,000', min: '5000', max: '10000' },
  { key: '10000-20000', label: '₹10,000 – ₹20,000', min: '10000', max: '20000' },
  { key: '20000+', label: '₹20,000+', min: '20000', max: '' },
];

/* ── Collapsible filter section ────────────────────────────── */
function FilterSection({ title, children, defaultOpen = true }: {
  title: string; children: React.ReactNode; defaultOpen?: boolean;
}) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className="border-b border-gray-100 py-4">
      <button
        onClick={() => setOpen(!open)}
        className="flex items-center justify-between w-full text-left"
      >
        <span className="text-sm font-semibold text-gray-800">{title}</span>
        <svg className={`w-4 h-4 text-gray-400 transition-transform ${open ? 'rotate-180' : ''}`}
          fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>
      {open && <div className="mt-3 space-y-2">{children}</div>}
    </div>
  );
}

function CheckboxFilter({ label, count, checked, onChange }: {
  label: string; count?: number; checked: boolean; onChange: () => void;
}) {
  return (
    <label className="flex items-center gap-2 cursor-pointer group">
      <input type="checkbox" checked={checked} onChange={onChange}
        className="w-4 h-4 rounded border-gray-300 text-orange-500 focus:ring-orange-400" />
      <span className="text-sm text-gray-700 group-hover:text-gray-900 flex-1">{label}</span>
      {count !== undefined && count > 0 && (
        <span className="text-xs text-gray-400">{count}</span>
      )}
    </label>
  );
}

function Stepper({ label, value, onChange }: {
  label: string; value: number; onChange: (v: number) => void;
}) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-sm text-gray-700">{label}</span>
      <div className="flex items-center gap-2">
        <button onClick={() => onChange(Math.max(0, value - 1))}
          disabled={value <= 0}
          className="w-7 h-7 rounded-full border border-gray-300 flex items-center justify-center text-gray-600 disabled:opacity-30 hover:border-gray-400">
          -
        </button>
        <span className="text-sm w-4 text-center">{value}</span>
        <button onClick={() => onChange(value + 1)}
          className="w-7 h-7 rounded-full border border-gray-300 flex items-center justify-center text-gray-600 hover:border-gray-400">
          +
        </button>
      </div>
    </div>
  );
}

/* ── Main search page ──────────────────────────────────────── */
export default function SearchPage() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const [listings, setListings] = useState<Listing[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [aggs, setAggs] = useState<FilterAggregations | null>(null);
  const [showMobileFilters, setShowMobileFilters] = useState(false);

  // Read all filter state from URL
  const city = searchParams.get('city') ?? '';
  const query = searchParams.get('query') ?? '';
  const lat = searchParams.get('lat') ?? '';
  const lng = searchParams.get('lng') ?? '';
  const radiusKm = searchParams.get('radiusKm') ?? '';
  const checkIn = searchParams.get('checkIn') ?? '';
  const checkOut = searchParams.get('checkOut') ?? '';
  const guestsParam = searchParams.get('guests') ?? '';
  const childrenParam = searchParams.get('children') ?? '';
  const infantsParam = searchParams.get('infants') ?? '';
  const petsParam = searchParams.get('pets') ?? '';
  const selectedTypes = searchParams.getAll('type');
  const minPrice = searchParams.get('minPrice') ?? '';
  const maxPrice = searchParams.get('maxPrice') ?? '';
  const sort = searchParams.get('sort') ?? '';
  const instantBook = searchParams.get('instantBook') === 'true';
  const petFriendly = searchParams.get('petFriendly') === 'true';
  const freeCancellation = searchParams.get('freeCancellation') === 'true';
  const noPrepayment = searchParams.get('noPrepayment') === 'true';
  const minRating = searchParams.get('minRating') ?? '';
  const starRating = searchParams.get('starRating') ?? '';
  const mealPlan = searchParams.get('mealPlan') ?? '';
  const minBedrooms = searchParams.get('minBedrooms') ?? '';
  const minBathrooms = searchParams.get('minBathrooms') ?? '';
  const medicalStay = searchParams.get('medicalStay') === 'true';
  const selectedAmenities = searchParams.getAll('amenities');
  const selectedBedTypes = searchParams.getAll('bedTypes');
  const selectedAccessibility = searchParams.getAll('accessibilityFeatures');
  const occupancyFilter = searchParams.getAll('occupancy');
  const foodFilter = searchParams.getAll('food');

  const [cityInput, setCityInput] = useState(city || query || (lat && lng ? 'Near Me' : ''));
  const [dateIn, setDateIn] = useState(checkIn);
  const [dateOut, setDateOut] = useState(checkOut);
  const [guests, setGuests] = useState<GuestCounts>({
    adults: Math.max(1, Number(guestsParam) - Number(childrenParam) || 1),
    children: Number(childrenParam) || 0,
    infants: Number(infantsParam) || 0,
    pets: Number(petsParam) || 0,
  });
  const [cityFocused, setCityFocused] = useState(false);
  const [citySuggestions, setCitySuggestions] = useState<string[]>([]);
  const [showGuestPicker, setShowGuestPicker] = useState(false);
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [calMonth, setCalMonth] = useState(() => new Date());
  const debouncedCity = useDebounce(cityInput, 250);

  const sortRef = useRef<HTMLDivElement>(null);
  const cityWrapRef = useRef<HTMLDivElement>(null);
  const guestRef = useRef<HTMLDivElement>(null);
  const dateRef = useRef<HTMLDivElement>(null);
  const [showSort, setShowSort] = useState(false);

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (sortRef.current && !sortRef.current.contains(e.target as Node)) setShowSort(false);
      if (cityWrapRef.current && !cityWrapRef.current.contains(e.target as Node)) setCityFocused(false);
      if (guestRef.current && !guestRef.current.contains(e.target as Node)) setShowGuestPicker(false);
      if (dateRef.current && !dateRef.current.contains(e.target as Node)) setShowDatePicker(false);
    }
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  useEffect(() => { setCityInput(city); }, [city]);
  useEffect(() => { setDateIn(checkIn); }, [checkIn]);
  useEffect(() => { setDateOut(checkOut); }, [checkOut]);

  // Fetch city autocomplete
  useEffect(() => {
    if (debouncedCity.length < 2) { setCitySuggestions([]); return; }
    api.autocomplete(debouncedCity)
      .then(r => setCitySuggestions(r.slice(0, 6)))
      .catch(() => setCitySuggestions([]));
  }, [debouncedCity]);

  const activeFilterCount = [
    selectedTypes.length > 0, sort !== '', instantBook, petFriendly,
    freeCancellation, noPrepayment, medicalStay, minPrice || maxPrice, minRating,
    starRating, mealPlan, minBedrooms, minBathrooms,
    selectedAmenities.length > 0, selectedBedTypes.length > 0,
    selectedAccessibility.length > 0,
  ].filter(Boolean).length;

  const fetchResults = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string> = {};
      if (city) params.city = city;
      if (query) params.query = query;
      if (lat) params.lat = lat;
      if (lng) params.lng = lng;
      if (radiusKm) params.radiusKm = radiusKm;
      // Auto-sort by distance for geo searches
      if (lat && lng && !sort) params.sort = 'distance_asc';
      if (selectedTypes.length) params.type = selectedTypes.join(',');
      if (sort) params.sort = sort;
      if (instantBook) params.instantBook = 'true';
      if (petFriendly) params.petFriendly = 'true';
      if (freeCancellation) params.freeCancellation = 'true';
      if (noPrepayment) params.noPrepayment = 'true';
      if (medicalStay) params.medicalStay = 'true';
      if (minRating) params.minRating = minRating;
      if (starRating) params.starRating = starRating;
      if (mealPlan) params.mealPlan = mealPlan;
      if (minBedrooms) params.minBedrooms = minBedrooms;
      if (minBathrooms) params.minBathrooms = minBathrooms;
      if (minPrice) {
        params.priceMin = String(Number(minPrice) * 100);
        params.minPricePaise = params.priceMin;
      }
      if (maxPrice) {
        params.priceMax = String(Number(maxPrice) * 100);
        params.maxPricePaise = params.priceMax;
      }
      // Array filters: join with comma — Spring Boot splits them into List<String>
      if (selectedAmenities.length) params.amenities = selectedAmenities.join(',');
      if (selectedBedTypes.length) params.bedTypes = selectedBedTypes.join(',');
      if (selectedAccessibility.length) params.accessibilityFeatures = selectedAccessibility.join(',');
      params.size = '20';

      const result = await api.search(params);
      setListings(result.content ?? []);
      setTotal(result.totalElements ?? 0);
      setAggs(result.aggregations ?? null);
    } catch {
      setListings([]);
    } finally {
      setLoading(false);
    }
  }, [city, query, lat, lng, radiusKm, selectedTypes.join(','), minPrice, maxPrice, sort, instantBook,
      petFriendly, freeCancellation, noPrepayment, medicalStay, minRating, starRating,
      mealPlan, minBedrooms, minBathrooms, selectedAmenities.join(','),
      selectedBedTypes.join(','), selectedAccessibility.join(',')]);

  useEffect(() => { fetchResults(); }, [fetchResults]);

  /* ── URL helpers ───────────────────────────────────────────── */
  function updateParam(key: string, value: string) {
    const params = new URLSearchParams(searchParams.toString());
    if (value) params.set(key, value);
    else params.delete(key);
    router.push(`/search?${params.toString()}`);
  }

  function toggleArrayParam(key: string, value: string) {
    const params = new URLSearchParams(searchParams.toString());
    const existing = params.getAll(key);
    if (existing.includes(value)) {
      params.delete(key);
      existing.filter(v => v !== value).forEach(v => params.append(key, v));
    } else {
      params.append(key, value);
    }
    router.push(`/search?${params.toString()}`);
  }

  function clearAllFilters() {
    const params = new URLSearchParams();
    if (city) params.set('city', city);
    if (dateIn) params.set('checkIn', dateIn);
    if (dateOut) params.set('checkOut', dateOut);
    const totalGuests = guests.adults + guests.children;
    if (totalGuests > 1) params.set('guests', String(totalGuests));
    if (guests.children > 0) params.set('children', String(guests.children));
    if (guests.infants > 0) params.set('infants', String(guests.infants));
    if (guests.pets > 0) params.set('pets', String(guests.pets));
    router.push(`/search?${params.toString()}`);
  }

  function handleSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    setCityFocused(false);
    const params = new URLSearchParams(searchParams.toString());
    // Smart search: if input matches a known city → filter by city, otherwise → text query
    const input = cityInput.trim();
    const isKnownCity = citySuggestions.some(c => c.toLowerCase() === input.toLowerCase())
      || POPULAR_CITIES.some(c => c.label.toLowerCase() === input.toLowerCase());
    if (input) {
      if (isKnownCity) {
        params.set('city', input);
        params.delete('query');
      } else {
        params.set('query', input);
        params.delete('city');
      }
    } else {
      params.delete('city');
      params.delete('query');
    }
    if (dateIn) params.set('checkIn', dateIn); else params.delete('checkIn');
    if (dateOut) params.set('checkOut', dateOut); else params.delete('checkOut');
    const totalGuests = guests.adults + guests.children;
    if (totalGuests > 1) params.set('guests', String(totalGuests)); else params.delete('guests');
    if (guests.children > 0) params.set('children', String(guests.children)); else params.delete('children');
    if (guests.infants > 0) params.set('infants', String(guests.infants)); else params.delete('infants');
    if (guests.pets > 0) params.set('pets', String(guests.pets)); else params.delete('pets');
    // Save to recent searches
    if (cityInput.trim()) {
      try {
        const stored = JSON.parse(localStorage.getItem('recent_searches') || '[]');
        const entry = { city: cityInput.trim(), checkIn: dateIn, checkOut: dateOut, guests: String(totalGuests), timestamp: Date.now() };
        const filtered = stored.filter((s: any) => s.city.toLowerCase() !== cityInput.trim().toLowerCase());
        filtered.unshift(entry);
        localStorage.setItem('recent_searches', JSON.stringify(filtered.slice(0, 5)));
      } catch {}
    }
    router.push(`/search?${params.toString()}`);
  }

  function selectCity(c: string) {
    setCityInput(c);
    setCityFocused(false);
    setCitySuggestions([]);
  }

  const [nearMeLoading, setNearMeLoading] = useState(false);

  function handleNearMe() {
    if (!navigator.geolocation) return;
    setNearMeLoading(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setNearMeLoading(false);
        setCityInput('Near Me');
        setCityFocused(false);
        const params = new URLSearchParams(searchParams.toString());
        params.set('lat', String(pos.coords.latitude));
        params.set('lng', String(pos.coords.longitude));
        params.set('radiusKm', '10');
        params.set('sort', 'distance_asc');
        params.delete('city');
        router.push(`/search?${params.toString()}`);
      },
      () => { setNearMeLoading(false); },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  function updateGuest(field: keyof GuestCounts, delta: number) {
    setGuests(prev => {
      const next = { ...prev, [field]: Math.max(0, prev[field] + delta) };
      if (next.adults < 1) next.adults = 1;
      if (next.adults + next.children > 16) return prev;
      if (next.infants > 5) return prev;
      if (next.pets > 5) return prev;
      return next;
    });
  }

  const totalGuestLabel = (() => {
    const total = guests.adults + guests.children;
    const parts = [
      `${guests.adults} adult${guests.adults !== 1 ? 's' : ''}`,
      guests.children > 0 ? `${guests.children} child${guests.children !== 1 ? 'ren' : ''}` : '',
      guests.infants > 0 ? `${guests.infants} infant${guests.infants !== 1 ? 's' : ''}` : '',
      guests.pets > 0 ? `${guests.pets} pet${guests.pets !== 1 ? 's' : ''}` : '',
    ].filter(Boolean);
    return total <= 1 && !guests.infants && !guests.pets ? '1 guest' : parts.join(', ');
  })();

  function highlightMatch(text: string, query: string) {
    const idx = text.toLowerCase().indexOf(query.toLowerCase());
    if (idx === -1) return <>{text}</>;
    return <>{text.slice(0, idx)}<span className="font-bold text-orange-600">{text.slice(idx, idx + query.length)}</span>{text.slice(idx + query.length)}</>;
  }

  /* ── Filter sidebar content (shared between desktop and mobile) ── */
  const filterContent = (
    <div className="space-y-0">
      {/* Price Range */}
      <FilterSection title="Your Budget (per night)">
        {PRICE_RANGES.map((pr) => (
          <CheckboxFilter key={pr.key} label={pr.label}
            count={aggs?.priceRanges?.[pr.key]}
            checked={minPrice === pr.min && maxPrice === pr.max}
            onChange={() => {
              if (minPrice === pr.min && maxPrice === pr.max) {
                const p = new URLSearchParams(searchParams.toString());
                p.delete('minPrice'); p.delete('maxPrice');
                router.push(`/search?${p.toString()}`);
              } else {
                const p = new URLSearchParams(searchParams.toString());
                if (pr.min) p.set('minPrice', pr.min); else p.delete('minPrice');
                if (pr.max) p.set('maxPrice', pr.max); else p.delete('maxPrice');
                router.push(`/search?${p.toString()}`);
              }
            }}
          />
        ))}
      </FilterSection>

      {/* Popular Filters */}
      <FilterSection title="Popular Filters">
        <CheckboxFilter label="Free Cancellation"
          count={aggs?.freeCancellationCount}
          checked={freeCancellation}
          onChange={() => updateParam('freeCancellation', freeCancellation ? '' : 'true')} />
        <CheckboxFilter label="Instant Book"
          count={aggs?.instantBookCount}
          checked={instantBook}
          onChange={() => updateParam('instantBook', instantBook ? '' : 'true')} />
        <CheckboxFilter label="Pet Friendly"
          count={aggs?.petFriendlyCount}
          checked={petFriendly}
          onChange={() => updateParam('petFriendly', petFriendly ? '' : 'true')} />
        <CheckboxFilter label="No Prepayment"
          count={aggs?.noPrepaymentCount}
          checked={noPrepayment}
          onChange={() => updateParam('noPrepayment', noPrepayment ? '' : 'true')} />
      </FilterSection>

      {/* Medical Stay Filter */}
      <FilterSection title="Medical Tourism" defaultOpen={false}>
        <label className="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={medicalStay}
            onChange={() => updateParam('medicalStay', medicalStay ? '' : 'true')}
            className="w-4 h-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
          />
          <span className="text-sm text-gray-700">Medical stay properties only</span>
          <span className="text-blue-500">🏥</span>
        </label>
      </FilterSection>

      {/* PG / Co-living Filters — shown when PG or COLIVING type is selected */}
      {(selectedTypes.includes('PG') || selectedTypes.includes('COLIVING')) && (
        <FilterSection title="PG Filters">
          <p className="text-xs font-medium text-gray-500 mb-1">Occupancy</p>
          {[
            { key: 'MALE', label: 'Male' },
            { key: 'FEMALE', label: 'Female' },
            { key: 'COED', label: 'Co-ed' },
          ].map((opt) => (
            <CheckboxFilter key={opt.key} label={opt.label}
              checked={occupancyFilter.includes(opt.key)}
              onChange={() => toggleArrayParam('occupancy', opt.key)}
            />
          ))}
          <p className="text-xs font-medium text-gray-500 mb-1 mt-3">Food</p>
          {[
            { key: 'VEG', label: 'Veg' },
            { key: 'NON_VEG', label: 'Non-veg' },
            { key: 'BOTH', label: 'Both' },
          ].map((opt) => (
            <CheckboxFilter key={opt.key} label={opt.label}
              checked={foodFilter.includes(opt.key)}
              onChange={() => toggleArrayParam('food', opt.key)}
            />
          ))}
        </FilterSection>
      )}

      {/* Review Score */}
      <FilterSection title="Review Score">
        {RATING_OPTIONS.map((opt) => (
          <CheckboxFilter key={opt.key} label={opt.label}
            count={aggs?.ratingRanges?.[opt.key]}
            checked={minRating === String(opt.value)}
            onChange={() => updateParam('minRating', minRating === String(opt.value) ? '' : String(opt.value))}
          />
        ))}
      </FilterSection>

      {/* Property Type */}
      <FilterSection title="Property Type">
        {Object.entries(TYPE_LABELS)
          .filter(([key]) => !aggs?.types || aggs.types[key] !== undefined || selectedTypes.includes(key))
          .map(([key, label]) => (
            <CheckboxFilter key={key} label={label}
              count={aggs?.types?.[key]}
              checked={selectedTypes.includes(key)}
              onChange={() => toggleArrayParam('type', key)}
            />
          ))}
      </FilterSection>

      {/* Star Rating */}
      <FilterSection title="Property Rating" defaultOpen={false}>
        {[5, 4, 3, 2, 1].map((star) => (
          <CheckboxFilter key={star} label={`${star} star${star > 1 ? 's' : ''}`}
            count={aggs?.starRatings?.[String(star)]}
            checked={starRating === String(star)}
            onChange={() => updateParam('starRating', starRating === String(star) ? '' : String(star))}
          />
        ))}
      </FilterSection>

      {/* Meals */}
      <FilterSection title="Meals" defaultOpen={false}>
        {Object.entries(MEAL_LABELS).map(([key, label]) => (
          <CheckboxFilter key={key} label={label}
            count={aggs?.mealPlans?.[key]}
            checked={mealPlan === key}
            onChange={() => updateParam('mealPlan', mealPlan === key ? '' : key)}
          />
        ))}
      </FilterSection>

      {/* Bedrooms & Bathrooms */}
      <FilterSection title="Bedrooms & Bathrooms" defaultOpen={false}>
        <Stepper label="Bedrooms" value={Number(minBedrooms) || 0}
          onChange={(v) => updateParam('minBedrooms', v > 0 ? String(v) : '')} />
        <Stepper label="Bathrooms" value={Number(minBathrooms) || 0}
          onChange={(v) => updateParam('minBathrooms', v > 0 ? String(v) : '')} />
      </FilterSection>

      {/* Amenities */}
      <FilterSection title="Amenities" defaultOpen={false}>
        {aggs?.amenities && Object.keys(aggs.amenities).length > 0
          ? Object.entries(aggs.amenities)
              .sort(([, a], [, b]) => b - a)
              .slice(0, 20)
              .map(([key, count]) => (
                <CheckboxFilter key={key}
                  label={AMENITY_LABELS[key] || key.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())}
                  count={count}
                  checked={selectedAmenities.includes(key)}
                  onChange={() => toggleArrayParam('amenities', key)}
                />
              ))
          : AMENITY_CATEGORIES.popular.items.map((key) => (
              <CheckboxFilter key={key}
                label={AMENITY_LABELS[key] || key}
                checked={selectedAmenities.includes(key)}
                onChange={() => toggleArrayParam('amenities', key)}
              />
            ))}
      </FilterSection>

      {/* Bed Preference */}
      <FilterSection title="Bed Preference" defaultOpen={false}>
        {Object.entries(BED_TYPE_LABELS).map(([key, label]) => (
          <CheckboxFilter key={key} label={label}
            count={aggs?.bedTypes?.[key]}
            checked={selectedBedTypes.includes(key)}
            onChange={() => toggleArrayParam('bedTypes', key)}
          />
        ))}
      </FilterSection>

      {/* Accessibility */}
      <FilterSection title="Accessibility" defaultOpen={false}>
        {Object.entries(ACCESSIBILITY_LABELS).map(([key, label]) => (
          <CheckboxFilter key={key} label={label}
            count={aggs?.accessibilityFeatures?.[key]}
            checked={selectedAccessibility.includes(key)}
            onChange={() => toggleArrayParam('accessibilityFeatures', key)}
          />
        ))}
      </FilterSection>
    </div>
  );

  return (
    <div className="max-w-[1400px] mx-auto px-4 py-6">
      {/* Search bar — city + dates + guests */}
      <form onSubmit={handleSearchSubmit}
        className="flex flex-col sm:flex-row gap-2 mb-4 bg-white border border-gray-200 rounded-2xl p-2 shadow-sm">
        {/* City with autocomplete */}
        <div ref={cityWrapRef} className="relative flex-1">
          <div className="flex items-center">
            <span className="pl-2 text-gray-400">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </span>
            <input className="flex-1 px-3 py-2.5 text-sm text-gray-900 outline-none placeholder-gray-400 bg-transparent"
              placeholder="Where are you going?"
              value={cityInput}
              onChange={(e) => setCityInput(e.target.value)}
              onFocus={() => setCityFocused(true)}
              autoComplete="off" />
            {cityInput && (
              <button type="button" onClick={() => { setCityInput(''); setCityFocused(true); }}
                className="pr-2 text-gray-300 hover:text-gray-500">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            )}
          </div>
          {/* Suggestion dropdown */}
          {cityFocused && (
            <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-gray-200 rounded-xl shadow-xl z-50 overflow-hidden max-h-[380px] overflow-y-auto">
              {/* Near Me button — always shown at top */}
              <button type="button" onClick={handleNearMe} disabled={nearMeLoading}
                className="w-full text-left px-3 py-2.5 flex items-center gap-3 hover:bg-orange-50 transition border-b border-gray-100">
                <span className="text-lg w-7 h-7 flex items-center justify-center bg-orange-50 rounded-lg">
                  {nearMeLoading ? (
                    <span className="w-4 h-4 border-2 border-orange-400 border-t-transparent rounded-full animate-spin inline-block" />
                  ) : (
                    <svg className="w-4 h-4 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                        d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                  )}
                </span>
                <div>
                  <p className="text-sm font-medium text-orange-600">{nearMeLoading ? 'Getting location...' : 'Near Me'}</p>
                  <p className="text-xs text-gray-400">Find stays near your current location</p>
                </div>
              </button>

              {citySuggestions.length > 0 ? (
                <>
                  <p className="px-3 pt-2 pb-1 text-xs font-semibold text-gray-400 uppercase">Results</p>
                  {citySuggestions.map(c => {
                    const isCity = POPULAR_CITIES.some(pc => pc.label.toLowerCase() === c.toLowerCase())
                      || Object.keys(CITY_ICONS).includes(c.toLowerCase());
                    return (
                      <button key={c} type="button" onClick={() => selectCity(c)}
                        className="w-full text-left px-3 py-2 flex items-center gap-3 hover:bg-gray-50 transition">
                        <span className="text-lg w-7 h-7 flex items-center justify-center bg-gray-100 rounded-lg">
                          {isCity ? (CITY_ICONS[c.toLowerCase()] || '📍') : '🏠'}
                        </span>
                        <div>
                          <span className="text-sm font-medium text-gray-900">
                            {highlightMatch(c, cityInput)}
                          </span>
                          {!isCity && <p className="text-xs text-gray-400">Listing</p>}
                        </div>
                      </button>
                    );
                  })}
                </>
              ) : cityInput.length < 2 ? (
                <>
                  {/* Recent searches */}
                  {(() => {
                    try {
                      const recent = JSON.parse(localStorage.getItem('recent_searches') || '[]').slice(0, 3);
                      if (recent.length === 0) return null;
                      return (
                        <>
                          <p className="px-3 pt-2 pb-1 text-xs font-semibold text-gray-400 uppercase">Recent</p>
                          {recent.map((r: any) => (
                            <button key={r.city} type="button" onClick={() => selectCity(r.city)}
                              className="w-full text-left px-3 py-2 flex items-center gap-3 hover:bg-gray-50 transition">
                              <span className="text-lg w-7 h-7 flex items-center justify-center bg-gray-100 rounded-lg">🕐</span>
                              <div>
                                <p className="text-sm font-medium text-gray-900">{r.city}</p>
                                {r.checkIn && <p className="text-xs text-gray-400">{r.checkIn}{r.checkOut ? ` → ${r.checkOut}` : ''}</p>}
                              </div>
                            </button>
                          ))}
                        </>
                      );
                    } catch { return null; }
                  })()}
                  {/* Popular */}
                  <p className="px-3 pt-2 pb-1 text-xs font-semibold text-gray-400 uppercase">Popular destinations</p>
                  {POPULAR_CITIES.map(c => (
                    <button key={c.label} type="button" onClick={() => selectCity(c.label)}
                      className="w-full text-left px-3 py-2 flex items-center gap-3 hover:bg-gray-50 transition">
                      <span className="text-lg w-7 h-7 flex items-center justify-center bg-gray-100 rounded-lg">{c.icon}</span>
                      <div>
                        <p className="text-sm font-medium text-gray-900">{c.label}</p>
                        <p className="text-xs text-gray-400">{c.subtitle}</p>
                      </div>
                    </button>
                  ))}
                </>
              ) : null}
            </div>
          )}
        </div>

        <div className="hidden sm:block w-px bg-gray-200 my-1" />

        {/* ── Airbnb-style unified date range picker ── */}
        <div ref={dateRef} className="relative">
          <button type="button" onClick={() => setShowDatePicker(!showDatePicker)}
            className="flex items-center gap-2 px-3 py-2.5 text-sm text-gray-700 hover:bg-gray-50 rounded-lg transition">
            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <span className={dateIn ? 'text-gray-900 font-medium' : 'text-gray-400'}>
              {dateIn ? new Date(dateIn + 'T00:00').toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : 'Check-in'}
            </span>
            <span className="text-gray-300">—</span>
            <span className={dateOut ? 'text-gray-900 font-medium' : 'text-gray-400'}>
              {dateOut ? new Date(dateOut + 'T00:00').toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : 'Check-out'}
            </span>
            {dateIn && dateOut && (
              <span className="text-xs text-orange-500 font-medium ml-1">
                {Math.ceil((new Date(dateOut).getTime() - new Date(dateIn).getTime()) / 86400000)}N
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
                  const startDay = month.getDay(); // 0=Sun
                  const today = new Date().toISOString().split('T')[0];
                  if (offset === 1) {
                    // Hide second month on mobile
                  }
                  return (
                    <div key={offset} className={`flex-1 ${offset === 1 ? 'hidden sm:block' : ''}`}>
                      <div className="grid grid-cols-7 gap-0 mb-1">
                        {['Su','Mo','Tu','We','Th','Fr','Sa'].map(d => (
                          <div key={d} className="text-xs text-gray-400 text-center py-1 font-medium">{d}</div>
                        ))}
                      </div>
                      <div className="grid grid-cols-7 gap-0">
                        {Array.from({ length: startDay }).map((_, i) => <div key={`empty-${i}`} />)}
                        {Array.from({ length: daysInMonth }).map((_, i) => {
                          const day = i + 1;
                          const dateStr = `${month.getFullYear()}-${String(month.getMonth() + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
                          const isPast = dateStr < today;
                          const isCheckIn = dateStr === dateIn;
                          const isCheckOut = dateStr === dateOut;
                          const isInRange = dateIn && dateOut && dateStr > dateIn && dateStr < dateOut;
                          const isSelected = isCheckIn || isCheckOut;

                          return (
                            <button key={day} type="button" disabled={isPast}
                              onClick={() => {
                                if (!dateIn || (dateIn && dateOut) || dateStr <= dateIn) {
                                  setDateIn(dateStr);
                                  setDateOut('');
                                } else {
                                  setDateOut(dateStr);
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

              {/* Footer */}
              <div className="flex items-center justify-between mt-4 pt-3 border-t border-gray-100">
                <div className="text-xs text-gray-500">
                  {dateIn && !dateOut && 'Select check-out date'}
                  {dateIn && dateOut && `${Math.ceil((new Date(dateOut).getTime() - new Date(dateIn).getTime()) / 86400000)} nights`}
                  {!dateIn && 'Select check-in date'}
                </div>
                <div className="flex gap-2">
                  <button type="button" onClick={() => { setDateIn(''); setDateOut(''); }}
                    className="text-xs text-gray-500 hover:text-gray-700 underline">Clear</button>
                  <button type="button" onClick={() => setShowDatePicker(false)}
                    className="text-xs font-semibold text-orange-600 hover:text-orange-700">Done</button>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="hidden sm:block w-px bg-gray-200 my-1" />

        {/* ── Guest picker (fixed overflow) ── */}
        <div ref={guestRef} className="relative">
          <button type="button" onClick={() => setShowGuestPicker(!showGuestPicker)}
            className="px-3 py-2.5 text-sm text-gray-700 whitespace-nowrap flex items-center gap-1">
            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            {totalGuestLabel}
          </button>
          {showGuestPicker && (
            <div className="absolute top-full mt-1 bg-white border rounded-xl shadow-xl z-50 p-4 w-72 space-y-4
                            right-0 sm:right-0 max-sm:left-1/2 max-sm:-translate-x-1/2 max-sm:right-auto">
              {([
                { key: 'adults' as const, label: 'Adults', sub: 'Age 13+', min: 1 },
                { key: 'children' as const, label: 'Children', sub: 'Age 2–12', min: 0 },
                { key: 'infants' as const, label: 'Infants', sub: 'Under 2', min: 0 },
                { key: 'pets' as const, label: 'Pets', sub: 'Service animals', min: 0 },
              ]).map(row => (
                <div key={row.key} className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-800">{row.label}</p>
                    <p className="text-xs text-gray-400">{row.sub}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <button type="button" onClick={() => updateGuest(row.key, -1)}
                      disabled={guests[row.key] <= row.min}
                      className="w-8 h-8 rounded-full border border-gray-300 text-gray-600 flex items-center justify-center hover:border-gray-500 disabled:opacity-30 transition">
                      -
                    </button>
                    <span className="w-5 text-center text-sm font-medium">{guests[row.key]}</span>
                    <button type="button" onClick={() => updateGuest(row.key, 1)}
                      className="w-8 h-8 rounded-full border border-gray-300 text-gray-600 flex items-center justify-center hover:border-gray-500 transition">
                      +
                    </button>
                  </div>
                </div>
              ))}
              <button type="button" onClick={() => setShowGuestPicker(false)}
                className="w-full text-sm font-semibold text-orange-600 hover:text-orange-700 pt-1">
                Done
              </button>
            </div>
          )}
        </div>

        {/* Search button */}
        <button type="submit"
          className="bg-orange-500 hover:bg-orange-600 text-white font-semibold rounded-xl px-5 py-2.5 text-sm transition-colors flex items-center gap-1.5">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          Search
        </button>
      </form>

      {/* Top bar: Sort + filter count + mobile filter toggle */}
      <div className="flex items-center gap-2 mb-4">
        {/* Sort dropdown */}
        <div ref={sortRef} className="relative">
          <button onClick={() => setShowSort(!showSort)}
            className={`inline-flex items-center gap-1.5 border rounded-full px-4 py-2 text-sm font-medium whitespace-nowrap transition-colors ${
              sort ? 'border-orange-400 bg-orange-50 text-orange-600' : 'border-gray-200 text-gray-600 hover:border-gray-300'
            }`}>
            {sort ? SORT_OPTIONS.find(o => o.value === sort)?.label : 'Sort'}
            <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
          </button>
          {showSort && (
            <div className="absolute top-full left-0 mt-2 bg-white border rounded-xl shadow-lg z-20 min-w-[200px] py-1">
              {SORT_OPTIONS.map((opt) => (
                <button key={opt.value}
                  onClick={() => { updateParam('sort', opt.value); setShowSort(false); }}
                  className={`w-full text-left px-4 py-2.5 text-sm flex items-center justify-between hover:bg-gray-50 ${
                    sort === opt.value ? 'text-orange-600 font-semibold bg-orange-50' : 'text-gray-700'
                  }`}>
                  {opt.label}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Mobile filter button */}
        <button onClick={() => setShowMobileFilters(true)}
          className="lg:hidden inline-flex items-center gap-1.5 border border-gray-200 rounded-full px-4 py-2 text-sm font-medium text-gray-600 hover:border-gray-300">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 010 2H4a1 1 0 01-1-1zm4 6a1 1 0 011-1h8a1 1 0 010 2H8a1 1 0 01-1-1zm2 6a1 1 0 011-1h4a1 1 0 010 2h-4a1 1 0 01-1-1z" />
          </svg>
          Filters
          {activeFilterCount > 0 && (
            <span className="bg-orange-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
              {activeFilterCount}
            </span>
          )}
        </button>

        {/* Result count */}
        <div className="flex-1 text-right">
          <span className="text-sm text-gray-600">
            {loading ? 'Searching...' : `${total.toLocaleString('en-IN')} ${total === 1 ? 'stay' : 'stays'}${city ? ` in ${city}` : ''}`}
          </span>
        </div>

        {activeFilterCount > 0 && (
          <button onClick={clearAllFilters}
            className="border border-red-200 bg-red-50 text-red-500 rounded-full px-3 py-1.5 text-xs font-medium hover:bg-red-100 transition-colors">
            Clear all ({activeFilterCount})
          </button>
        )}
      </div>

      {/* Main content: sidebar + results */}
      <div className="flex gap-6">
        {/* Desktop sidebar */}
        <aside className="hidden lg:block w-72 flex-shrink-0">
          <div className="sticky top-4 max-h-[calc(100vh-120px)] overflow-y-auto pr-2 scrollbar-thin">
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-base font-bold text-gray-900">Filter by</h2>
              {activeFilterCount > 0 && (
                <button onClick={clearAllFilters} className="text-xs text-orange-600 hover:underline">
                  Clear all
                </button>
              )}
            </div>
            {filterContent}
          </div>
        </aside>

        {/* Results grid */}
        <main className="flex-1 min-w-0">
          {loading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="rounded-xl bg-gray-100 h-72 animate-pulse" />
              ))}
            </div>
          ) : listings.length > 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
              {listings.map((listing) => (
                <ListingCard key={listing.id} listing={listing} />
              ))}
            </div>
          ) : (
            <div className="text-center py-20 text-gray-400">
              <p className="text-5xl mb-4">🔍</p>
              <p className="text-lg font-medium">No properties found</p>
              <p className="text-sm mt-1">Try a different city or remove filters</p>
            </div>
          )}
        </main>
      </div>

      {/* Mobile filter bottom sheet */}
      {showMobileFilters && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div className="absolute inset-0 bg-black/50" onClick={() => setShowMobileFilters(false)} />
          <div className="absolute bottom-0 left-0 right-0 bg-white rounded-t-2xl max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between px-4 py-3 border-b">
              <h2 className="text-lg font-bold text-gray-900">Filters</h2>
              <div className="flex items-center gap-3">
                {activeFilterCount > 0 && (
                  <button onClick={clearAllFilters} className="text-sm text-orange-600">Clear all</button>
                )}
                <button onClick={() => setShowMobileFilters(false)}
                  className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-gray-100">
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>
            <div className="overflow-y-auto px-4 pb-4 flex-1">
              {filterContent}
            </div>
            <div className="border-t px-4 py-3">
              <button onClick={() => setShowMobileFilters(false)}
                className="w-full bg-orange-500 hover:bg-orange-600 text-white font-semibold rounded-xl py-3 text-sm transition-colors">
                Show {total.toLocaleString('en-IN')} results
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
