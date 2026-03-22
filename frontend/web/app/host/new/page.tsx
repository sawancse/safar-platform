'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { geocodeIndianAddress } from '@/lib/geocode';
import MapLocationPicker from '@/components/MapLocationPicker';
import PricingCalculator from '@/components/PricingCalculator';
import type { ListingType } from '@/types';

/* ── Constants ─────────────────────────────────────────────── */

const PROPERTY_CATEGORIES = [
  {
    label: 'Homes',
    description: 'Apartments, vacation homes, villas, etc.',
    icon: '🏠',
    types: [
      { value: 'HOME', label: 'Entire Home', desc: 'Guests have the whole place to themselves' },
      { value: 'APARTMENT', label: 'Apartment', desc: 'Furnished and self-catering accommodation' },
      { value: 'VILLA', label: 'Villa', desc: 'Spacious property, often with a pool or garden' },
      { value: 'VACATION_HOME', label: 'Vacation Home', desc: 'Holiday property for short-term stays' },
      { value: 'ROOM', label: 'Private Room', desc: 'A private room within a shared property' },
    ],
  },
  {
    label: 'Hotels & B&Bs',
    description: 'Hotels, guest houses, hostels, B&Bs, etc.',
    icon: '🏨',
    types: [
      { value: 'HOTEL', label: 'Hotel', desc: 'Full-service hotel with daily housekeeping' },
      { value: 'BUDGET_HOTEL', label: 'Budget Hotel', desc: 'Affordable hotel with essential amenities' },
      { value: 'GUESTHOUSE', label: 'Guesthouse', desc: 'Private home with separate facilities for guests' },
      { value: 'BNB', label: 'Bed & Breakfast', desc: 'Overnight stays with breakfast included' },
      { value: 'HOMESTAY', label: 'Homestay', desc: 'Shared living with the host family' },
      { value: 'HOSTEL_DORM', label: 'Hostel / Dorm', desc: 'Budget accommodation with dorm-style beds' },
      { value: 'RESORT', label: 'Resort', desc: 'On-site restaurants, activities, and luxury feel' },
      { value: 'LODGE', label: 'Lodge', desc: 'Surrounded by nature — forest, mountains' },
    ],
  },
  {
    label: 'PG & Co-living',
    description: 'Paying guest, co-living, and long-term stays',
    icon: '🏘️',
    types: [
      { value: 'PG', label: 'Paying Guest (PG)', desc: 'Monthly rental with meals, shared or private rooms' },
      { value: 'COLIVING', label: 'Co-living', desc: 'Community-oriented long-term shared living spaces' },
    ],
  },
  {
    label: 'Unique Stays',
    description: 'Farm stays, chalets, and unique experiences',
    icon: '🌿',
    types: [
      { value: 'FARMSTAY', label: 'Farm Stay', desc: 'Private farm with simple accommodation' },
      { value: 'CHALET', label: 'Chalet', desc: 'Cosy mountain or countryside retreat' },
      { value: 'UNIQUE', label: 'Unique Stay', desc: 'Treehouses, boats, heritage havelis, etc.' },
    ],
  },
  {
    label: 'Commercial',
    description: 'Meeting rooms, co-working, event venues',
    icon: '🏢',
    types: [
      { value: 'COMMERCIAL:MEETING_ROOM', label: 'Meeting Room', desc: 'Boardrooms, conference rooms for corporate meetings' },
      { value: 'COMMERCIAL:COWORKING_SPACE', label: 'Coworking Space', desc: 'Hot desks, dedicated desks, private cabins' },
      { value: 'COMMERCIAL:EVENT_VENUE', label: 'Event Venue', desc: 'Banquet halls, party venues, wedding spaces' },
      { value: 'COMMERCIAL:PHOTO_STUDIO', label: 'Photo Studio', desc: 'Professional photography studios with lighting' },
      { value: 'COMMERCIAL:PODCAST_STUDIO', label: 'Podcast Studio', desc: 'Soundproofed recording studios' },
      { value: 'COMMERCIAL:COMMERCIAL_KITCHEN', label: 'Commercial Kitchen', desc: 'Licensed kitchens for food businesses' },
      { value: 'COMMERCIAL:TRAINING_ROOM', label: 'Training Room', desc: 'Classrooms, workshop spaces, seminar halls' },
      { value: 'COMMERCIAL:ROOFTOP_TERRACE', label: 'Rooftop Terrace', desc: 'Open-air spaces for events & gatherings' },
    ],
  },
];

const COMMERCIAL_FACILITY_GROUPS = [
  {
    label: 'Tech & AV',
    items: [
      { key: 'wifi', label: 'High-Speed WiFi' },
      { key: 'projector', label: 'Projector / Screen' },
      { key: 'whiteboard', label: 'Whiteboard' },
      { key: 'tv', label: 'TV / Monitor' },
      { key: 'video_conferencing', label: 'Video Conferencing' },
      { key: 'sound_system', label: 'Sound System / PA' },
      { key: 'printer', label: 'Printer / Scanner' },
    ],
  },
  {
    label: 'Comfort',
    items: [
      { key: 'ac', label: 'Air Conditioning' },
      { key: 'natural_light', label: 'Natural Light' },
      { key: 'soundproofing', label: 'Soundproofing' },
      { key: 'ergonomic_chairs', label: 'Ergonomic Chairs' },
    ],
  },
  {
    label: 'Services',
    items: [
      { key: 'reception', label: 'Reception / Front Desk' },
      { key: 'catering', label: 'Catering Available' },
      { key: 'coffee', label: 'Coffee / Tea / Beverages' },
      { key: 'housekeeping', label: 'Housekeeping' },
    ],
  },
  {
    label: 'Access & Safety',
    items: [
      { key: 'parking', label: 'Parking' },
      { key: 'wheelchair_accessible', label: 'Wheelchair Accessible' },
      { key: 'elevator', label: 'Elevator' },
      { key: '24hr_access', label: '24-Hour Access' },
      { key: 'security', label: 'Security / CCTV' },
      { key: 'power_backup', label: 'Power Backup' },
    ],
  },
];

const FACILITY_GROUPS = [
  {
    label: 'Most Popular',
    items: [
      { key: 'wifi', label: 'Free WiFi' },
      { key: 'ac', label: 'Air Conditioning' },
      { key: 'parking', label: 'Parking' },
      { key: 'pool', label: 'Swimming Pool' },
      { key: 'kitchen', label: 'Kitchen' },
      { key: 'tv', label: 'TV' },
      { key: 'hot_water', label: 'Hot Water / Geyser' },
      { key: 'power_backup', label: 'Power Backup' },
    ],
  },
  {
    label: 'Guest Services',
    items: [
      { key: 'restaurant', label: 'Restaurant' },
      { key: 'room_service', label: 'Room Service' },
      { key: 'laundry', label: 'Laundry' },
      { key: 'iron', label: 'Iron' },
      { key: 'hair_dryer', label: 'Hair Dryer' },
      { key: 'coffee', label: 'Coffee / Tea Maker' },
      { key: 'toiletries', label: 'Toiletries' },
    ],
  },
  {
    label: 'Outdoor & Leisure',
    items: [
      { key: 'garden', label: 'Garden' },
      { key: 'terrace', label: 'Terrace / Balcony' },
      { key: 'gym', label: 'Fitness Center' },
      { key: 'spa', label: 'Spa' },
      { key: 'hot_tub', label: 'Hot Tub / Jacuzzi' },
    ],
  },
  {
    label: 'Safety & Security',
    items: [
      { key: 'cctv', label: 'CCTV' },
      { key: 'security', label: '24-hour Security' },
      { key: 'fire_extinguisher', label: 'Fire Extinguisher' },
      { key: 'smoke_detector', label: 'Smoke Detector' },
      { key: 'first_aid', label: 'First Aid Kit' },
    ],
  },
  {
    label: 'Accessibility',
    items: [
      { key: 'elevator', label: 'Elevator' },
      { key: 'wheelchair_accessible', label: 'Wheelchair Accessible' },
    ],
  },
  {
    label: 'India Essentials',
    items: [
      { key: 'water_purifier', label: 'Water Purifier / RO' },
      { key: 'mosquito_net', label: 'Mosquito Net' },
      { key: 'fan', label: 'Ceiling Fan' },
      { key: 'inverter', label: 'Inverter / UPS' },
    ],
  },
];

const STAR_OPTIONS = [
  { value: 0, label: 'N/A' },
  { value: 1, label: '1 Star' },
  { value: 2, label: '2 Stars' },
  { value: 3, label: '3 Stars' },
  { value: 4, label: '4 Stars' },
  { value: 5, label: '5 Stars' },
];

const INDIAN_STATES = [
  'Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chhattisgarh',
  'Goa', 'Gujarat', 'Haryana', 'Himachal Pradesh', 'Jharkhand', 'Karnataka',
  'Kerala', 'Madhya Pradesh', 'Maharashtra', 'Manipur', 'Meghalaya', 'Mizoram',
  'Nagaland', 'Odisha', 'Punjab', 'Rajasthan', 'Sikkim', 'Tamil Nadu',
  'Telangana', 'Tripura', 'Uttar Pradesh', 'Uttarakhand', 'West Bengal',
  'Delhi', 'Chandigarh', 'Puducherry', 'Jammu and Kashmir', 'Ladakh',
];

/* ── Wizard State ──────────────────────────────────────────── */
interface WizardData {
  // Step 1
  category: string;
  type: ListingType;
  commercialCategory: string;
  // Step 2
  title: string;
  description: string;
  addressLine1: string;
  city: string;
  state: string;
  pincode: string;
  lat: number;
  lng: number;
  starRating: number;
  basePricePaise: number;
  maxGuests: number;
  bedrooms: number;
  bathrooms: number;
  pricingUnit: 'NIGHT' | 'HOUR';
  // Commercial-specific
  areaSqft: number;
  minBookingHours: number;
  operatingHoursFrom: string;
  operatingHoursUntil: string;
  gstin: string;
  // Step 3
  amenities: string[];
  bedTypes: string[];
  mealPlan: string;
  accessibilityFeatures: string[];
  breakfastIncluded: boolean;
  parkingType: 'NONE' | 'FREE' | 'PAID';
  petFriendly: boolean;
  maxPets: number;
  childrenAllowed: boolean;
  cancellationPolicy: 'FREE' | 'MODERATE' | 'STRICT';
  freeCancellation: boolean;
  instantBook: boolean;
  checkInFrom: string;
  checkInUntil: string;
  checkOutFrom: string;
  checkOutUntil: string;
  // PG/Co-living specific
  occupancyType: 'MALE' | 'FEMALE' | 'COED' | '';
  foodType: 'VEG' | 'NON_VEG' | 'BOTH' | 'NONE';
  gateClosingTime: string;
  noticePeriodDays: number;
  securityDepositPaise: number;
  // Hotel specific
  hotelChain: string;
  frontDesk24h: boolean;
  checkinTime: string;
  checkoutTime: string;
  // Discount rules
  weeklyDiscountPercent: string;
  monthlyDiscountPercent: string;
  // Visibility boost
  visibilityBoostPercent: number;
}

const INITIAL_DATA: WizardData = {
  category: '', type: 'HOME' as ListingType, commercialCategory: '',
  title: '', description: '', addressLine1: '', city: '', state: '', pincode: '',
  lat: 0, lng: 0, starRating: 0, basePricePaise: 0, maxGuests: 2,
  bedrooms: 1, bathrooms: 1, pricingUnit: 'NIGHT',
  areaSqft: 0, minBookingHours: 1, operatingHoursFrom: '09:00', operatingHoursUntil: '21:00', gstin: '',
  occupancyType: '', foodType: 'NONE', gateClosingTime: '22:00', noticePeriodDays: 30, securityDepositPaise: 0,
  hotelChain: '', frontDesk24h: false, checkinTime: '14:00', checkoutTime: '11:00',
  amenities: [], bedTypes: [], mealPlan: 'NONE', accessibilityFeatures: [], breakfastIncluded: false, parkingType: 'NONE',
  petFriendly: false, maxPets: 0, childrenAllowed: true,
  cancellationPolicy: 'MODERATE', freeCancellation: false, instantBook: false,
  checkInFrom: '14:00', checkInUntil: '23:00',
  checkOutFrom: '06:00', checkOutUntil: '11:00',
  weeklyDiscountPercent: '', monthlyDiscountPercent: '',
  visibilityBoostPercent: 0,
};

/* ── Progress Bar ──────────────────────────────────────────── */
function ProgressBar({ step, labels }: { step: number; labels: string[] }) {
  return (
    <div className="flex items-center gap-0 mb-8">
      {labels.map((label, i) => {
        const done = i < step;
        const active = i === step;
        return (
          <div key={i} className="flex-1 flex items-center">
            <div className="flex flex-col items-center flex-1">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-colors ${
                done ? 'bg-green-500 text-white' : active ? 'bg-orange-500 text-white' : 'bg-gray-200 text-gray-500'
              }`}>
                {done ? '✓' : i + 1}
              </div>
              <span className={`text-xs mt-1 text-center ${active ? 'text-orange-600 font-semibold' : 'text-gray-400'}`}>
                {label}
              </span>
            </div>
            {i < labels.length - 1 && (
              <div className={`h-0.5 flex-1 -mt-4 ${done ? 'bg-green-500' : 'bg-gray-200'}`} />
            )}
          </div>
        );
      })}
    </div>
  );
}

/* ── Main Wizard ───────────────────────────────────────────── */
export default function NewListingWizard() {
  const router = useRouter();
  const [step, setStep] = useState(0);
  const [data, setData] = useState<WizardData>(INITIAL_DATA);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [geocoding, setGeocoding] = useState(false);
  const [token, setToken] = useState('');

  useEffect(() => {
    const t = localStorage.getItem('access_token') ?? '';
    if (!t) { router.push('/auth?redirect=/host/new'); return; }
    setToken(t);
  }, [router]);

  // Auto-geocode on pincode + city
  useEffect(() => {
    if (data.pincode.length === 6 && data.city.trim()) {
      setGeocoding(true);
      geocodeIndianAddress(data.pincode, data.city, data.state).then((coords) => {
        if (coords) update({ lat: coords.lat, lng: coords.lng });
        setGeocoding(false);
      });
    }
  }, [data.pincode, data.city, data.state]);

  function update(partial: Partial<WizardData>) {
    setData(prev => ({ ...prev, ...partial }));
  }

  function toggleAmenity(key: string) {
    setData(prev => ({
      ...prev,
      amenities: prev.amenities.includes(key)
        ? prev.amenities.filter(a => a !== key)
        : [...prev.amenities, key],
    }));
  }

  const isCommercial = data.type === 'COMMERCIAL';
  const isPG = data.type === 'PG' || data.type === 'COLIVING';
  const isHotel = data.type === 'HOTEL' || data.type === 'BUDGET_HOTEL';
  const STEP_LABELS = ['Property Type', 'Details', isCommercial ? 'Facilities & Policies' : 'Facilities & Rules', 'Review'];

  /* ── Validation ──────────────────────────────────────────── */
  function canProceed(): boolean {
    if (step === 0) return !!data.type;
    if (step === 1) return !!(data.title && data.city && data.state && data.pincode && data.basePricePaise > 0);
    return true;
  }

  /* ── Submit ──────────────────────────────────────────────── */
  async function handleSubmit() {
    setSubmitting(true);
    setError('');
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
      const body: Record<string, any> = {
        title: data.title,
        description: data.description || data.title,
        type: data.type,
        addressLine1: data.addressLine1 || data.city,
        city: data.city,
        state: data.state,
        pincode: data.pincode,
        lat: data.lat || 0.0,
        lng: data.lng || 0.0,
        maxGuests: data.maxGuests,
        amenities: data.amenities,
        basePricePaise: data.basePricePaise,
        pricingUnit: isCommercial ? 'HOUR' : data.pricingUnit,
        instantBook: data.instantBook,
        cancellationPolicy: data.cancellationPolicy,
        freeCancellation: data.freeCancellation,
        parkingType: data.parkingType,
        weeklyDiscountPercent: data.weeklyDiscountPercent ? Number(data.weeklyDiscountPercent) : null,
        monthlyDiscountPercent: data.monthlyDiscountPercent ? Number(data.monthlyDiscountPercent) : null,
        visibilityBoostPercent: data.visibilityBoostPercent || 0,
      };
      if (isPG) {
        body.pricingUnit = 'NIGHT'; // stored as nightly but displayed as monthly
        if (data.occupancyType) body.occupancyType = data.occupancyType;
        body.foodType = data.foodType;
        body.gateClosingTime = data.gateClosingTime;
        body.noticePeriodDays = data.noticePeriodDays;
        body.securityDepositPaise = data.securityDepositPaise > 0 ? data.securityDepositPaise : null;
        body.minStayDays = 30;
      }
      if (isHotel) {
        if (data.hotelChain) body.hotelChain = data.hotelChain;
        body.frontDesk24h = data.frontDesk24h;
        body.checkinTime = data.checkinTime;
        body.checkoutTime = data.checkoutTime;
      }
      if (isCommercial) {
        body.commercialCategory = data.commercialCategory;
        body.areaSqft = data.areaSqft > 0 ? data.areaSqft : null;
        body.minBookingHours = data.minBookingHours;
        body.operatingHoursFrom = data.operatingHoursFrom;
        body.operatingHoursUntil = data.operatingHoursUntil;
        if (data.gstin) body.gstin = data.gstin;
        body.gstApplicable = true;
      } else {
        body.bedrooms = data.bedrooms || null;
        body.bathrooms = data.bathrooms || null;
        body.bedTypes = data.bedTypes.length > 0 ? data.bedTypes : null;
        body.mealPlan = data.mealPlan !== 'NONE' ? data.mealPlan : null;
        body.accessibilityFeatures = data.accessibilityFeatures.length > 0 ? data.accessibilityFeatures : null;
        body.petFriendly = data.petFriendly;
        body.maxPets = data.maxPets;
        body.starRating = data.starRating > 0 ? data.starRating : null;
        body.breakfastIncluded = data.breakfastIncluded;
        body.childrenAllowed = data.childrenAllowed;
        body.checkInFrom = data.checkInFrom;
        body.checkInUntil = data.checkInUntil;
        body.checkOutFrom = data.checkOutFrom;
        body.checkOutUntil = data.checkOutUntil;
      }
      const res = await fetch(`${apiUrl}/api/v1/listings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const text = await res.text().catch(() => '');
        let detail = 'Failed to create listing';
        try { const err = JSON.parse(text); detail = err.detail || err.message || detail; } catch { if (text) detail = text; }
        throw new Error(detail);
      }
      // Upgrade role in localStorage
      const currentRole = localStorage.getItem('user_role');
      if (currentRole === 'GUEST') localStorage.setItem('user_role', 'HOST');
      router.push('/host');
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSubmitting(false);
    }
  }

  /* ── Step 0: Property Type ───────────────────────────────── */
  function renderStep0() {
    return (
      <div>
        <h2 className="text-xl font-bold text-gray-900 mb-2">What type of property are you listing?</h2>
        <p className="text-sm text-gray-500 mb-6">Select the category that best describes your property</p>
        <div className="space-y-6">
          {PROPERTY_CATEGORIES.map((cat) => (
            <div key={cat.label}>
              <div className="flex items-center gap-2 mb-3">
                <span className="text-2xl">{cat.icon}</span>
                <div>
                  <h3 className="text-sm font-bold text-gray-800">{cat.label}</h3>
                  <p className="text-xs text-gray-500">{cat.description}</p>
                </div>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {cat.types.map((t) => {
                  const isComm = t.value.startsWith('COMMERCIAL:');
                  const typeVal = isComm ? 'COMMERCIAL' : t.value;
                  const commCat = isComm ? t.value.split(':')[1] : '';
                  const selected = isComm
                    ? (data.type === 'COMMERCIAL' && data.commercialCategory === commCat)
                    : data.type === t.value;
                  return (
                  <button key={t.value} type="button"
                    onClick={() => update({
                      type: typeVal as ListingType,
                      category: cat.label,
                      commercialCategory: commCat,
                      pricingUnit: isComm ? 'HOUR' : 'NIGHT',
                    })}
                    className={`text-left border-2 rounded-xl p-4 transition-all hover:shadow-md ${
                      selected
                        ? 'border-orange-500 bg-orange-50 shadow-sm'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}>
                    <p className="font-semibold text-sm text-gray-800">{t.label}</p>
                    <p className="text-xs text-gray-500 mt-1">{t.desc}</p>
                  </button>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  /* ── Step 1: Location & Details ──────────────────────────── */
  function renderStep1() {
    const typeLabel = isCommercial
      ? PROPERTY_CATEGORIES.find(c => c.label === 'Commercial')?.types.find(t => t.value === `COMMERCIAL:${data.commercialCategory}`)?.label ?? 'Commercial Space'
      : PROPERTY_CATEGORIES.flatMap(c => c.types).find(t => t.value === data.type)?.label ?? data.type;

    return (
      <div>
        <h2 className="text-xl font-bold text-gray-900 mb-2">
          {isCommercial ? 'Tell us about your space' : 'Tell us about your property'}
        </h2>
        <p className="text-sm text-gray-500 mb-6">
          You're listing a <span className="font-semibold text-orange-600">{typeLabel}</span>
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {/* Name */}
          <div className="sm:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {isCommercial ? 'Space Name' : 'Property Name'} *
            </label>
            <input required className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
              placeholder={isCommercial ? 'e.g. WeWork BKC Meeting Room A' : 'e.g. Cosy Studio in Bandra West'}
              value={data.title} onChange={e => update({ title: e.target.value })} />
          </div>
          {/* Description */}
          <div className="sm:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <textarea rows={3} className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400 resize-none"
              placeholder={isCommercial ? 'Describe your space, equipment, and what makes it ideal...' : 'Describe what makes your place special...'}
              value={data.description} onChange={e => update({ description: e.target.value })} />
          </div>
          {/* Address */}
          <div className="sm:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">Address</label>
            <input className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
              placeholder="123, MG Road, Near Metro Station"
              value={data.addressLine1} onChange={e => update({ addressLine1: e.target.value })} />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">City *</label>
            <input required className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
              placeholder="Mumbai"
              value={data.city} onChange={e => update({ city: e.target.value })} />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">State *</label>
            <select className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400 bg-white"
              value={data.state} onChange={e => update({ state: e.target.value })}>
              <option value="">Select state</option>
              {INDIAN_STATES.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Pincode *</label>
            <input required maxLength={6} className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
              placeholder="400050"
              value={data.pincode} onChange={e => update({ pincode: e.target.value.replace(/\D/g, '') })} />
            {geocoding && <p className="text-xs text-orange-500 mt-1">Locating...</p>}
          </div>

          {/* Map location picker */}
          <div className="sm:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Pin your location on the map
            </label>
            <p className="text-xs text-gray-500 mb-2">
              Search, click the map, or drag the pin to set the exact location. You can also use the GPS button.
            </p>
            <div className="h-72 rounded-xl overflow-hidden border border-gray-200">
              <MapLocationPicker
                lat={data.lat}
                lng={data.lng}
                onLocationChange={(loc) => {
                  const updates: Partial<typeof data> = { lat: loc.lat, lng: loc.lng };
                  if (loc.city && !data.city) updates.city = loc.city;
                  if (loc.state && !data.state) updates.state = loc.state;
                  if (loc.pincode && !data.pincode) updates.pincode = loc.pincode;
                  if (loc.address && !data.addressLine1) updates.addressLine1 = loc.address;
                  update(updates);
                }}
                className="h-full"
              />
            </div>
            {data.lat !== 0 && data.lng !== 0 && (
              <p className="text-xs text-green-600 mt-1 flex items-center gap-1">
                <span>✓</span> Location set ({data.lat.toFixed(4)}, {data.lng.toFixed(4)})
              </p>
            )}
          </div>

          {/* ── Residential-only fields ──────────────────── */}
          {!isCommercial && (
            <>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Star Rating</label>
                <div className="flex gap-2 flex-wrap">
                  {STAR_OPTIONS.map(opt => (
                    <button key={opt.value} type="button"
                      onClick={() => update({ starRating: opt.value })}
                      className={`px-3 py-2 rounded-lg text-sm font-medium border transition ${
                        data.starRating === opt.value
                          ? 'border-orange-500 bg-orange-50 text-orange-600'
                          : 'border-gray-200 text-gray-600 hover:border-gray-300'
                      }`}>
                      {opt.label}
                    </button>
                  ))}
                </div>
              </div>
            </>
          )}

          {/* Price */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Price per {isCommercial ? 'hour' : isPG ? 'month' : 'night'} (INR) *
            </label>
            <input required type="number" min="1"
              className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
              placeholder={isCommercial ? '500' : '2500'}
              value={data.basePricePaise ? data.basePricePaise / 100 : ''}
              onChange={e => update({ basePricePaise: Math.round(Number(e.target.value) * 100) })} />
          </div>

          {/* Commission Preview */}
          {data.basePricePaise > 0 && (
            <PricingCalculator basePricePaise={data.basePricePaise} tier="STARTER" editable={false} />
          )}

          {/* Discount Rules */}
          <div className="grid grid-cols-2 gap-4 mt-4">
            <div>
              <label className="block text-xs font-semibold text-gray-600 mb-1">Weekly discount (%)</label>
              <input type="number" value={data.weeklyDiscountPercent} onChange={e => update({ weeklyDiscountPercent: e.target.value })}
                className="w-full border rounded-xl px-3 py-2 text-sm" min={0} max={50} placeholder="Optional" />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-600 mb-1">Monthly discount (%)</label>
              <input type="number" value={data.monthlyDiscountPercent} onChange={e => update({ monthlyDiscountPercent: e.target.value })}
                className="w-full border rounded-xl px-3 py-2 text-sm" min={0} max={50} placeholder="Optional" />
            </div>
          </div>

          {/* Capacity */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {isCommercial ? 'Max Capacity (people)' : 'Max Guests'} *
            </label>
            <input type="number" min="1" max={isCommercial ? 500 : 50}
              className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
              value={data.maxGuests} onChange={e => update({ maxGuests: Number(e.target.value) || 1 })} />
          </div>

          {/* ── Commercial-only fields ───────────────────── */}
          {isCommercial && (
            <>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Area (sq. ft.)</label>
                <input type="number" min="0"
                  className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                  placeholder="500"
                  value={data.areaSqft || ''} onChange={e => update({ areaSqft: Number(e.target.value) || 0 })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Min Booking Hours</label>
                <input type="number" min="1" max="24"
                  className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                  value={data.minBookingHours} onChange={e => update({ minBookingHours: Number(e.target.value) || 1 })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Operating Hours (Open)</label>
                <input type="time" className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                  value={data.operatingHoursFrom} onChange={e => update({ operatingHoursFrom: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Operating Hours (Close)</label>
                <input type="time" className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                  value={data.operatingHoursUntil} onChange={e => update({ operatingHoursUntil: e.target.value })} />
              </div>
              <div className="sm:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">GSTIN (required for commercial)</label>
                <input className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400 font-mono uppercase"
                  maxLength={15} placeholder="22AAAAA0000A1Z5"
                  value={data.gstin} onChange={e => update({ gstin: e.target.value.toUpperCase() })} />
              </div>
            </>
          )}

          {/* ── Residential bedrooms/bathrooms ──────────── */}
          {!isCommercial && (
            <>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Bedrooms</label>
                <input type="number" min="0" max="20"
                  className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                  value={data.bedrooms} onChange={e => update({ bedrooms: Number(e.target.value) || 0 })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Bathrooms</label>
                <input type="number" min="0" max="20"
                  className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                  value={data.bathrooms} onChange={e => update({ bathrooms: Number(e.target.value) || 0 })} />
              </div>
            </>
          )}

          {/* ── PG / Co-living specific fields ─────────── */}
          {isPG && (
            <>
              <div className="sm:col-span-2 border-t pt-4 mt-2">
                <h3 className="text-sm font-bold text-gray-800 mb-3">PG / Co-living Details</h3>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Occupancy Type *</label>
                <div className="flex gap-3">
                  {[
                    { value: 'MALE' as const, label: 'Male Only' },
                    { value: 'FEMALE' as const, label: 'Female Only' },
                    { value: 'COED' as const, label: 'Co-ed' },
                  ].map(opt => (
                    <button key={opt.value} type="button"
                      onClick={() => update({ occupancyType: opt.value })}
                      className={`flex-1 px-3 py-2 rounded-lg text-sm font-medium border transition ${
                        data.occupancyType === opt.value
                          ? 'border-orange-500 bg-orange-50 text-orange-600'
                          : 'border-gray-200 text-gray-600 hover:border-gray-300'
                      }`}>
                      {opt.label}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Food Type</label>
                <div className="flex gap-3 flex-wrap">
                  {[
                    { value: 'VEG' as const, label: 'Veg' },
                    { value: 'NON_VEG' as const, label: 'Non-veg' },
                    { value: 'BOTH' as const, label: 'Both' },
                    { value: 'NONE' as const, label: 'No meals' },
                  ].map(opt => (
                    <button key={opt.value} type="button"
                      onClick={() => update({ foodType: opt.value })}
                      className={`px-4 py-2 rounded-lg text-sm font-medium border transition ${
                        data.foodType === opt.value
                          ? 'border-orange-500 bg-orange-50 text-orange-600'
                          : 'border-gray-200 text-gray-600 hover:border-gray-300'
                      }`}>
                      {opt.label}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Gate Closing Time</label>
                <input type="time" className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                  value={data.gateClosingTime} onChange={e => update({ gateClosingTime: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Notice Period (days)</label>
                <input type="number" min="0" max="90"
                  className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                  value={data.noticePeriodDays} onChange={e => update({ noticePeriodDays: Number(e.target.value) || 0 })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Security Deposit (INR)</label>
                <input type="number" min="0"
                  className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                  placeholder="5000"
                  value={data.securityDepositPaise ? data.securityDepositPaise / 100 : ''}
                  onChange={e => update({ securityDepositPaise: Math.round(Number(e.target.value) * 100) })} />
              </div>
            </>
          )}

          {/* ── Hotel / Budget Hotel specific fields ──── */}
          {isHotel && (
            <>
              <div className="sm:col-span-2 border-t pt-4 mt-2">
                <h3 className="text-sm font-bold text-gray-800 mb-3">Hotel Details</h3>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Hotel Chain (optional)</label>
                <input className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                  placeholder="e.g. OYO, Taj, ITC"
                  value={data.hotelChain} onChange={e => update({ hotelChain: e.target.value })} />
              </div>
              <div>
                <label className="flex items-center gap-3 cursor-pointer mt-6">
                  <input type="checkbox" className="w-5 h-5 rounded text-orange-500 focus:ring-orange-400"
                    checked={data.frontDesk24h}
                    onChange={e => update({ frontDesk24h: e.target.checked })} />
                  <div>
                    <span className="text-sm font-medium text-gray-700">24-hour Front Desk</span>
                    <p className="text-xs text-gray-500">Staff available around the clock</p>
                  </div>
                </label>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Check-in Time</label>
                <input type="time" className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                  value={data.checkinTime} onChange={e => update({ checkinTime: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Check-out Time</label>
                <input type="time" className="w-full border rounded-xl px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-orange-400"
                  value={data.checkoutTime} onChange={e => update({ checkoutTime: e.target.value })} />
              </div>
            </>
          )}
        </div>
      </div>
    );
  }

  /* ── Step 2: Facilities & House Rules ────────────────────── */
  function renderStep2() {
    return (
      <div>
        <h2 className="text-xl font-bold text-gray-900 mb-2">
          {isCommercial ? 'Facilities & Policies' : 'Facilities & House Rules'}
        </h2>
        <p className="text-sm text-gray-500 mb-6">
          {isCommercial ? 'What equipment and services does your space offer?' : 'What can guests use at your property?'}
        </p>

        {/* Facilities checklist */}
        {(isCommercial ? COMMERCIAL_FACILITY_GROUPS : FACILITY_GROUPS).map((group) => (
          <div key={group.label} className="mb-6">
            <h3 className="text-sm font-bold text-gray-700 mb-3">{group.label}</h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
              {group.items.map((item) => (
                <label key={item.key}
                  className={`flex items-center gap-2 border rounded-lg px-3 py-2.5 cursor-pointer transition ${
                    data.amenities.includes(item.key)
                      ? 'border-orange-400 bg-orange-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}>
                  <input type="checkbox" className="w-4 h-4 rounded text-orange-500 focus:ring-orange-400"
                    checked={data.amenities.includes(item.key)}
                    onChange={() => toggleAmenity(item.key)} />
                  <span className="text-sm text-gray-700">{item.label}</span>
                </label>
              ))}
            </div>
          </div>
        ))}

        {/* Bed Types — residential only */}
        {!isCommercial && (
        <div className="mb-4">
          <p className="text-sm font-semibold text-gray-700 mb-2">Bed Types</p>
          <div className="flex flex-wrap gap-2">
            {['Single', 'Double', 'Queen', 'King', 'Sofa Bed', 'Bunk Bed', 'Floor Mattress'].map(bt => (
              <button key={bt} type="button"
                onClick={() => update({ bedTypes: data.bedTypes.includes(bt) ? data.bedTypes.filter(b => b !== bt) : [...data.bedTypes, bt] })}
                className={`px-3 py-1.5 rounded-full text-xs font-medium border transition ${
                  data.bedTypes.includes(bt) ? 'bg-orange-500 text-white border-orange-500' : 'bg-white text-gray-600 border-gray-300'
                }`}>{bt}</button>
            ))}
          </div>
        </div>
        )}

        {/* Meal Plan — residential only */}
        {!isCommercial && (
        <div className="mb-4">
          <p className="text-sm font-semibold text-gray-700 mb-2">Meal Plan</p>
          <select value={data.mealPlan} onChange={e => update({ mealPlan: e.target.value })}
            className="border rounded-lg px-3 py-2 text-sm w-full">
            <option value="NONE">No meals</option>
            <option value="BREAKFAST">Breakfast included</option>
            <option value="HALF_BOARD">Half board</option>
            <option value="FULL_BOARD">Full board</option>
            <option value="ALL_INCLUSIVE">All inclusive</option>
          </select>
        </div>
        )}

        {/* Accessibility Features */}
        {!isCommercial && (
        <div className="mb-4">
          <p className="text-sm font-semibold text-gray-700 mb-2">Accessibility Features</p>
          <div className="grid grid-cols-2 gap-2">
            {['Wheelchair accessible', 'Step-free access', 'Wide doorways', 'Accessible bathroom', 'Grab bars', 'Roll-in shower', 'Elevator'].map(af => (
              <label key={af} className="flex items-center gap-2 text-sm cursor-pointer">
                <input type="checkbox" checked={data.accessibilityFeatures.includes(af)}
                  onChange={() => update({ accessibilityFeatures: data.accessibilityFeatures.includes(af) ? data.accessibilityFeatures.filter(a => a !== af) : [...data.accessibilityFeatures, af] })}
                  className="rounded text-orange-500" />
                {af}
              </label>
            ))}
          </div>
        </div>
        )}

        {/* Services */}
        <div className="border-t pt-6 mt-6 space-y-5">
          <h3 className="text-base font-bold text-gray-900">Services</h3>

          {/* Breakfast — residential only */}
          {!isCommercial && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Do you serve breakfast?</label>
            <div className="flex gap-3">
              {[true, false].map(val => (
                <button key={String(val)} type="button"
                  onClick={() => update({ breakfastIncluded: val })}
                  className={`px-5 py-2 rounded-lg text-sm font-medium border transition ${
                    data.breakfastIncluded === val
                      ? 'border-orange-500 bg-orange-50 text-orange-600'
                      : 'border-gray-200 text-gray-600 hover:border-gray-300'
                  }`}>
                  {val ? 'Yes' : 'No'}
                </button>
              ))}
            </div>
          </div>
          )}

          {/* Parking */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Is parking available?</label>
            <div className="flex gap-3">
              {[
                { value: 'FREE' as const, label: 'Yes, free' },
                { value: 'PAID' as const, label: 'Yes, paid' },
                { value: 'NONE' as const, label: 'No' },
              ].map(opt => (
                <button key={opt.value} type="button"
                  onClick={() => update({ parkingType: opt.value })}
                  className={`px-5 py-2 rounded-lg text-sm font-medium border transition ${
                    data.parkingType === opt.value
                      ? 'border-orange-500 bg-orange-50 text-orange-600'
                      : 'border-gray-200 text-gray-600 hover:border-gray-300'
                  }`}>
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {/* Cancellation */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Cancellation policy</label>
            <div className="flex gap-3 flex-wrap">
              {[
                { value: 'FREE' as const, label: 'Free cancellation', desc: 'Guests can cancel anytime' },
                { value: 'MODERATE' as const, label: 'Moderate', desc: 'Free up to 5 days before' },
                { value: 'STRICT' as const, label: 'Strict', desc: 'Non-refundable' },
              ].map(opt => (
                <button key={opt.value} type="button"
                  onClick={() => update({
                    cancellationPolicy: opt.value,
                    freeCancellation: opt.value === 'FREE',
                  })}
                  className={`flex-1 min-w-[120px] text-left border-2 rounded-xl px-4 py-3 transition ${
                    data.cancellationPolicy === opt.value
                      ? 'border-orange-500 bg-orange-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}>
                  <p className="text-sm font-semibold text-gray-800">{opt.label}</p>
                  <p className="text-xs text-gray-500 mt-0.5">{opt.desc}</p>
                </button>
              ))}
            </div>
          </div>

          {/* Instant Book */}
          <label className="flex items-center gap-3 cursor-pointer">
            <input type="checkbox" className="w-5 h-5 rounded text-orange-500 focus:ring-orange-400"
              checked={data.instantBook}
              onChange={e => update({ instantBook: e.target.checked })} />
            <div>
              <span className="text-sm font-medium text-gray-700">Instant Book</span>
              <p className="text-xs text-gray-500">Guests can book without waiting for approval</p>
            </div>
          </label>

          {/* Visibility Boost */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Visibility Boost</label>
            <p className="text-xs text-gray-500 mb-3">
              Boost your listing&apos;s position in search results. Higher boosts mean more visibility to potential guests.
            </p>
            <div className="flex gap-3 flex-wrap">
              {[
                { value: 0, label: 'None (0%)', desc: 'Standard placement' },
                { value: 3, label: '+3% Boost', desc: 'Moderate visibility increase' },
                { value: 5, label: '+5% Boost', desc: 'Maximum visibility' },
              ].map(opt => (
                <button key={opt.value} type="button"
                  onClick={() => update({ visibilityBoostPercent: opt.value })}
                  className={`flex-1 min-w-[120px] text-left border-2 rounded-xl px-4 py-3 transition ${
                    data.visibilityBoostPercent === opt.value
                      ? 'border-orange-500 bg-orange-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}>
                  <p className="text-sm font-semibold text-gray-800">{opt.label}</p>
                  <p className="text-xs text-gray-500 mt-0.5">{opt.desc}</p>
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* House Rules — residential only */}
        {!isCommercial && (
        <div className="border-t pt-6 mt-6 space-y-5">
          <h3 className="text-base font-bold text-gray-900">House Rules</h3>

          {/* Check-in/out times */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Check-in from</label>
              <input type="time" className="w-full border rounded-lg px-3 py-2 text-sm"
                value={data.checkInFrom} onChange={e => update({ checkInFrom: e.target.value })} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Check-in until</label>
              <input type="time" className="w-full border rounded-lg px-3 py-2 text-sm"
                value={data.checkInUntil} onChange={e => update({ checkInUntil: e.target.value })} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Check-out from</label>
              <input type="time" className="w-full border rounded-lg px-3 py-2 text-sm"
                value={data.checkOutFrom} onChange={e => update({ checkOutFrom: e.target.value })} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Check-out until</label>
              <input type="time" className="w-full border rounded-lg px-3 py-2 text-sm"
                value={data.checkOutUntil} onChange={e => update({ checkOutUntil: e.target.value })} />
            </div>
          </div>

          {/* Children */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Do you allow children?</label>
            <div className="flex gap-3">
              {[true, false].map(val => (
                <button key={String(val)} type="button"
                  onClick={() => update({ childrenAllowed: val })}
                  className={`px-5 py-2 rounded-lg text-sm font-medium border transition ${
                    data.childrenAllowed === val
                      ? 'border-orange-500 bg-orange-50 text-orange-600'
                      : 'border-gray-200 text-gray-600 hover:border-gray-300'
                  }`}>
                  {val ? 'Yes' : 'No'}
                </button>
              ))}
            </div>
          </div>

          {/* Pets */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Do you allow pets?</label>
            <div className="flex gap-3">
              {[true, false].map(val => (
                <button key={String(val)} type="button"
                  onClick={() => update({ petFriendly: val, maxPets: val ? 2 : 0 })}
                  className={`px-5 py-2 rounded-lg text-sm font-medium border transition ${
                    data.petFriendly === val
                      ? 'border-orange-500 bg-orange-50 text-orange-600'
                      : 'border-gray-200 text-gray-600 hover:border-gray-300'
                  }`}>
                  {val ? 'Yes' : 'No'}
                </button>
              ))}
            </div>
          </div>
        </div>
        )}
      </div>
    );
  }

  /* ── Step 3: Review & Create ─────────────────────────────── */
  function renderStep3() {
    const typeLabel = isCommercial
      ? PROPERTY_CATEGORIES.find(c => c.label === 'Commercial')?.types.find(t => t.value === `COMMERCIAL:${data.commercialCategory}`)?.label ?? 'Commercial Space'
      : PROPERTY_CATEGORIES.flatMap(c => c.types).find(t => t.value === data.type)?.label ?? data.type;
    return (
      <div>
        <h2 className="text-xl font-bold text-gray-900 mb-2">Review your listing</h2>
        <p className="text-sm text-gray-500 mb-6">Make sure everything looks good before creating</p>

        <div className="space-y-4">
          {/* Property overview card */}
          <div className="border rounded-2xl p-5 bg-orange-50/50">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-xs text-orange-600 font-semibold uppercase">{typeLabel}</p>
                <h3 className="text-lg font-bold text-gray-900 mt-1">{data.title || 'Untitled'}</h3>
                <p className="text-sm text-gray-500 mt-1">{data.addressLine1 && `${data.addressLine1}, `}{data.city}, {data.state} {data.pincode}</p>
              </div>
              <div className="text-right">
                <p className="text-lg font-bold text-gray-900">
                  {(data.basePricePaise / 100).toLocaleString('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })}
                </p>
                <p className="text-xs text-gray-500">per {data.pricingUnit === 'HOUR' ? 'hour' : isPG ? 'month' : 'night'}</p>
              </div>
            </div>
            {data.starRating > 0 && (
              <p className="text-sm text-gray-600 mt-2">{'★'.repeat(data.starRating)} {data.starRating}-star property</p>
            )}
          </div>

          {/* Details grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {(isCommercial ? [
              { label: 'Capacity', value: data.maxGuests },
              { label: 'Area (sq.ft)', value: data.areaSqft || '—' },
              { label: 'Min Hours', value: data.minBookingHours },
              { label: 'Facilities', value: data.amenities.length },
            ] : [
              { label: 'Guests', value: data.maxGuests },
              { label: 'Bedrooms', value: data.bedrooms },
              { label: 'Bathrooms', value: data.bathrooms },
              { label: 'Amenities', value: data.amenities.length },
            ]).map(item => (
              <div key={item.label} className="border rounded-xl p-3 text-center">
                <p className="text-lg font-bold text-gray-800">{item.value}</p>
                <p className="text-xs text-gray-500">{item.label}</p>
              </div>
            ))}
          </div>

          {/* Services summary */}
          <div className="border rounded-xl p-4 space-y-2">
            <h4 className="text-sm font-bold text-gray-800">{isCommercial ? 'Space Details' : 'Services & Policies'}</h4>
            <div className="grid grid-cols-2 gap-2 text-sm">
              {isCommercial && (
                <>
                  <p className="text-gray-600">Operating: <span className="font-medium">{data.operatingHoursFrom} – {data.operatingHoursUntil}</span></p>
                  {data.gstin && <p className="text-gray-600">GSTIN: <span className="font-medium font-mono">{data.gstin}</span></p>}
                </>
              )}
              {!isCommercial && (
                <p className="text-gray-600">Breakfast: <span className="font-medium">{data.breakfastIncluded ? 'Included' : 'Not included'}</span></p>
              )}
              <p className="text-gray-600">Parking: <span className="font-medium">{data.parkingType === 'FREE' ? 'Free' : data.parkingType === 'PAID' ? 'Paid' : 'None'}</span></p>
              <p className="text-gray-600">Cancellation: <span className="font-medium capitalize">{data.cancellationPolicy.toLowerCase()}</span></p>
              <p className="text-gray-600">Instant Book: <span className="font-medium">{data.instantBook ? 'Yes' : 'No'}</span></p>
              <p className="text-gray-600">Visibility Boost: <span className="font-medium">{data.visibilityBoostPercent > 0 ? `+${data.visibilityBoostPercent}%` : 'None'}</span></p>
              {isPG && (
                <>
                  <p className="text-gray-600">Occupancy: <span className="font-medium">{data.occupancyType === 'MALE' ? 'Male Only' : data.occupancyType === 'FEMALE' ? 'Female Only' : 'Co-ed'}</span></p>
                  <p className="text-gray-600">Food: <span className="font-medium">{data.foodType === 'VEG' ? 'Veg' : data.foodType === 'NON_VEG' ? 'Non-veg' : data.foodType === 'BOTH' ? 'Both' : 'No meals'}</span></p>
                  <p className="text-gray-600">Gate Closing: <span className="font-medium">{data.gateClosingTime}</span></p>
                  <p className="text-gray-600">Notice Period: <span className="font-medium">{data.noticePeriodDays} days</span></p>
                  {data.securityDepositPaise > 0 && (
                    <p className="text-gray-600">Security Deposit: <span className="font-medium">{(data.securityDepositPaise / 100).toLocaleString('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })}</span></p>
                  )}
                </>
              )}
              {isHotel && (
                <>
                  {data.hotelChain && <p className="text-gray-600">Hotel Chain: <span className="font-medium">{data.hotelChain}</span></p>}
                  <p className="text-gray-600">24h Front Desk: <span className="font-medium">{data.frontDesk24h ? 'Yes' : 'No'}</span></p>
                  <p className="text-gray-600">Check-in: <span className="font-medium">{data.checkinTime}</span></p>
                  <p className="text-gray-600">Check-out: <span className="font-medium">{data.checkoutTime}</span></p>
                </>
              )}
              {!isCommercial && (
                <>
                  <p className="text-gray-600">Children: <span className="font-medium">{data.childrenAllowed ? 'Allowed' : 'Not allowed'}</span></p>
                  <p className="text-gray-600">Pets: <span className="font-medium">{data.petFriendly ? 'Allowed' : 'Not allowed'}</span></p>
                  {data.mealPlan !== 'NONE' && <p className="text-gray-600">Meal Plan: <span className="font-medium">{data.mealPlan.replace('_', ' ').toLowerCase()}</span></p>}
                  {data.bedTypes.length > 0 && <p className="text-gray-600">Beds: <span className="font-medium">{data.bedTypes.join(', ')}</span></p>}
                  {data.accessibilityFeatures.length > 0 && <p className="text-gray-600">Accessibility: <span className="font-medium">{data.accessibilityFeatures.length} feature(s)</span></p>}
                </>
              )}
            </div>
          </div>

          {/* House rules / Operating hours */}
          <div className="border rounded-xl p-4">
            <h4 className="text-sm font-bold text-gray-800 mb-2">
              {isCommercial ? 'Operating Hours' : 'House Rules'}
            </h4>
            <p className="text-sm text-gray-600">
              {isCommercial
                ? `Open: ${data.operatingHoursFrom} – ${data.operatingHoursUntil}`
                : `Check-in: ${data.checkInFrom} – ${data.checkInUntil} | Check-out: ${data.checkOutFrom} – ${data.checkOutUntil}`
              }
            </p>
          </div>

          {/* Amenities tags */}
          {data.amenities.length > 0 && (
            <div className="border rounded-xl p-4">
              <h4 className="text-sm font-bold text-gray-800 mb-2">Facilities ({data.amenities.length})</h4>
              <div className="flex flex-wrap gap-2">
                {data.amenities.map(a => (
                  <span key={a} className="bg-gray-100 text-gray-700 text-xs px-2.5 py-1 rounded-full">
                    {a.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Info note */}
          <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
            <p className="text-sm text-blue-800">
              Your listing will be created as a <span className="font-semibold">Draft</span>. You'll need to upload photos and submit for verification before it goes live.
            </p>
          </div>
        </div>

        {error && (
          <div className="mt-4 bg-red-50 text-red-600 rounded-xl px-4 py-3 text-sm">{error}</div>
        )}
      </div>
    );
  }

  /* ── Render ──────────────────────────────────────────────── */
  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <ProgressBar step={step} labels={STEP_LABELS} />

      {step === 0 && renderStep0()}
      {step === 1 && renderStep1()}
      {step === 2 && renderStep2()}
      {step === 3 && renderStep3()}

      {/* Navigation */}
      <div className="flex items-center justify-between mt-8 pt-6 border-t">
        <button type="button"
          onClick={() => step === 0 ? router.push('/host') : setStep(step - 1)}
          className="text-sm font-medium text-gray-600 hover:text-gray-800 px-4 py-2">
          {step === 0 ? 'Cancel' : 'Back'}
        </button>
        {step < 3 ? (
          <button type="button"
            onClick={() => setStep(step + 1)}
            disabled={!canProceed()}
            className="bg-orange-500 hover:bg-orange-600 text-white font-semibold px-8 py-2.5 rounded-xl text-sm transition disabled:opacity-50 disabled:cursor-not-allowed">
            Continue
          </button>
        ) : (
          <button type="button"
            onClick={handleSubmit}
            disabled={submitting}
            className="bg-orange-500 hover:bg-orange-600 text-white font-semibold px-8 py-2.5 rounded-xl text-sm transition disabled:opacity-50">
            {submitting ? 'Creating...' : 'Create Listing'}
          </button>
        )}
      </div>
    </div>
  );
}
