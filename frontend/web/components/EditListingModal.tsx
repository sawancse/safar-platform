'use client';

import { useState } from 'react';
import { api } from '@/lib/api';
import type { Listing } from '@/types';

interface Props {
  listing: Listing;
  onClose: () => void;
  onSaved: () => void;
  token: string;
}

const BED_TYPES = ['Single', 'Double', 'Queen', 'King', 'Sofa Bed', 'Bunk Bed', 'Floor Mattress'];
const ACCESSIBILITY = ['Wheelchair accessible', 'Step-free access', 'Wide doorways', 'Accessible bathroom', 'Grab bars', 'Roll-in shower', 'Elevator', 'Braille signage'];
const MEAL_PLANS = [
  { value: 'NONE', label: 'No meals' },
  { value: 'BREAKFAST', label: 'Breakfast included' },
  { value: 'HALF_BOARD', label: 'Half board (breakfast + dinner)' },
  { value: 'FULL_BOARD', label: 'Full board (all meals)' },
  { value: 'ALL_INCLUSIVE', label: 'All inclusive' },
];

export default function EditListingModal({ listing, onClose, onSaved, token }: Props) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [activeSection, setActiveSection] = useState('basic');

  // Form state -- initialized from listing
  const [title, setTitle] = useState(listing.title || '');
  const [description, setDescription] = useState(listing.description || '');
  const [price, setPrice] = useState(listing.basePricePaise || 0);
  const [pricingUnit, setPricingUnit] = useState(listing.pricingUnit || 'NIGHT');
  const [maxGuests, setMaxGuests] = useState(listing.maxGuests || 2);
  const [bedrooms, setBedrooms] = useState(listing.bedrooms || 0);
  const [bathrooms, setBathrooms] = useState(listing.bathrooms || 0);
  const [bedTypes, setBedTypes] = useState<string[]>(listing.bedTypes || []);
  const [accessibilityFeatures, setAccessibilityFeatures] = useState<string[]>(listing.accessibilityFeatures || []);
  const [mealPlan, setMealPlan] = useState(listing.mealPlan || 'NONE');
  const [cancellationPolicy, setCancellationPolicy] = useState(listing.cancellationPolicy || 'MODERATE');
  const [instantBook, setInstantBook] = useState(listing.instantBook || false);
  const [petFriendly, setPetFriendly] = useState(listing.petFriendly || false);
  const [maxPets, setMaxPets] = useState(listing.maxPets || 0);
  const [checkInFrom, setCheckInFrom] = useState(listing.checkInFrom || '14:00');
  const [checkInUntil, setCheckInUntil] = useState(listing.checkInUntil || '23:00');
  const [checkOutFrom, setCheckOutFrom] = useState(listing.checkOutFrom || '06:00');
  const [checkOutUntil, setCheckOutUntil] = useState(listing.checkOutUntil || '11:00');
  const [childrenAllowed, setChildrenAllowed] = useState(listing.childrenAllowed !== false);
  const [longTermMonthlyPaise, setLongTermMonthlyPaise] = useState(listing.longTermMonthlyPaise || 0);
  const [breakfastIncluded, setBreakfastIncluded] = useState(listing.breakfastIncluded || false);

  const handleSave = async () => {
    setSaving(true);
    setError('');
    try {
      await api.updateListing(listing.id, {
        title, description,
        basePricePaise: price,
        pricingUnit,
        maxGuests, bedrooms, bathrooms,
        bedTypes, accessibilityFeatures,
        mealPlan, cancellationPolicy,
        instantBook, petFriendly, maxPets,
        checkInFrom, checkInUntil, checkOutFrom, checkOutUntil,
        childrenAllowed, longTermMonthlyPaise,
        breakfastIncluded,
      }, token);
      onSaved();
    } catch (e: any) {
      setError(e.message || 'Failed to save');
    }
    setSaving(false);
  };

  const SECTIONS = [
    { key: 'basic', label: 'Basic Info' },
    { key: 'rooms', label: 'Rooms & Beds' },
    { key: 'pricing', label: 'Pricing' },
    { key: 'facilities', label: 'Facilities' },
    { key: 'rules', label: 'House Rules' },
    { key: 'accessibility', label: 'Accessibility' },
  ];

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={onClose}>
      <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-hidden flex flex-col" onClick={e => e.stopPropagation()}>
        {/* Header */}
        <div className="border-b px-6 py-4 flex items-center justify-between">
          <h2 className="text-lg font-bold">Edit: {listing.title}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl">&times;</button>
        </div>

        {/* Section tabs */}
        <div className="border-b px-6 py-2 flex gap-1 overflow-x-auto">
          {SECTIONS.map(s => (
            <button key={s.key} onClick={() => setActiveSection(s.key)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition ${
                activeSection === s.key ? 'bg-orange-100 text-orange-700' : 'text-gray-500 hover:bg-gray-100'
              }`}>{s.label}</button>
          ))}
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto px-6 py-4 space-y-4">
          {error && <div className="bg-red-50 text-red-600 text-sm rounded-lg px-4 py-2">{error}</div>}

          {activeSection === 'basic' && (
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
                <input value={title} onChange={e => setTitle(e.target.value)}
                  className="w-full border rounded-lg px-3 py-2 text-sm" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea value={description} onChange={e => setDescription(e.target.value)}
                  rows={4} className="w-full border rounded-lg px-3 py-2 text-sm" />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Max Guests</label>
                  <input type="number" value={maxGuests} onChange={e => setMaxGuests(Number(e.target.value))}
                    className="w-full border rounded-lg px-3 py-2 text-sm" min={1} />
                </div>
              </div>
            </div>
          )}

          {activeSection === 'rooms' && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Bedrooms</label>
                  <input type="number" value={bedrooms} onChange={e => setBedrooms(Number(e.target.value))}
                    className="w-full border rounded-lg px-3 py-2 text-sm" min={0} />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Bathrooms</label>
                  <input type="number" value={bathrooms} onChange={e => setBathrooms(Number(e.target.value))}
                    className="w-full border rounded-lg px-3 py-2 text-sm" min={0} />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Bed Types</label>
                <div className="flex flex-wrap gap-2">
                  {BED_TYPES.map(bt => (
                    <button key={bt} type="button" onClick={() => setBedTypes(prev => prev.includes(bt) ? prev.filter(b => b !== bt) : [...prev, bt])}
                      className={`px-3 py-1.5 rounded-full text-xs font-medium border transition ${
                        bedTypes.includes(bt) ? 'bg-orange-500 text-white border-orange-500' : 'bg-white text-gray-600 border-gray-300 hover:border-gray-400'
                      }`}>{bt}</button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {activeSection === 'pricing' && (
            <div className="space-y-4">
              {/* Pricing Unit Selector */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Pricing basis</label>
                <div className="flex gap-2">
                  {[
                    { value: 'NIGHT', label: 'Per Night', hint: 'Standard stays' },
                    { value: 'MONTH', label: 'Per Month', hint: 'PG / Co-living / Long-term' },
                    { value: 'HOUR', label: 'Per Hour', hint: 'Hotels / Meeting rooms' },
                  ].map(opt => (
                    <button key={opt.value} type="button"
                      onClick={() => setPricingUnit(opt.value as 'NIGHT' | 'HOUR' | 'MONTH')}
                      className={`flex-1 p-3 rounded-lg border text-sm text-left transition ${
                        pricingUnit === opt.value
                          ? 'border-orange-500 bg-orange-50 ring-1 ring-orange-500'
                          : 'border-gray-200 hover:border-gray-300'
                      }`}>
                      <div className="font-medium">{opt.label}</div>
                      <div className="text-xs text-gray-400 mt-0.5">{opt.hint}</div>
                    </button>
                  ))}
                </div>
              </div>

              {/* Base Price */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Base price (₹ / {pricingUnit === 'MONTH' ? 'month' : pricingUnit === 'HOUR' ? 'hour' : 'night'})
                </label>
                <input type="number" value={price / 100} onChange={e => setPrice(Number(e.target.value) * 100)}
                  className="w-full border rounded-lg px-3 py-2 text-sm" min={1}
                  placeholder={pricingUnit === 'MONTH' ? 'e.g. 8000' : pricingUnit === 'HOUR' ? 'e.g. 500' : 'e.g. 2000'} />
                <p className="text-xs text-gray-400 mt-1">
                  {pricingUnit === 'MONTH' && 'Guests will be charged this amount per month. Partial months are prorated daily.'}
                  {pricingUnit === 'HOUR' && 'Guests will be charged this amount per hour of stay.'}
                  {pricingUnit === 'NIGHT' && 'Guests will be charged this amount per night.'}
                </p>
              </div>

              {/* Long-term monthly rate (only for nightly-priced listings) */}
              {pricingUnit === 'NIGHT' && listing.type !== 'COMMERCIAL' && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Long-term monthly rate (₹, optional)</label>
                  <input type="number" value={(longTermMonthlyPaise || 0) / 100}
                    onChange={e => setLongTermMonthlyPaise(Number(e.target.value) * 100)}
                    className="w-full border rounded-lg px-3 py-2 text-sm" min={0}
                    placeholder="Discounted rate for 30+ day stays" />
                  <p className="text-xs text-gray-400 mt-1">If set, guests booking 30+ nights will see this monthly rate instead.</p>
                </div>
              )}
            </div>
          )}

          {activeSection === 'facilities' && (
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Meal Plan</label>
                <div className="space-y-2">
                  {MEAL_PLANS.map(mp => (
                    <label key={mp.value} className="flex items-center gap-2 cursor-pointer">
                      <input type="radio" name="mealPlan" value={mp.value} checked={mealPlan === mp.value}
                        onChange={() => setMealPlan(mp.value as any)} className="text-orange-500" />
                      <span className="text-sm">{mp.label}</span>
                    </label>
                  ))}
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Cancellation Policy</label>
                {['FREE', 'MODERATE', 'STRICT'].map(cp => (
                  <label key={cp} className="flex items-center gap-2 cursor-pointer mb-1">
                    <input type="radio" name="cancellation" value={cp} checked={cancellationPolicy === cp}
                      onChange={() => setCancellationPolicy(cp as any)} className="text-orange-500" />
                    <span className="text-sm">{cp === 'FREE' ? 'Free cancellation (48h before)' : cp === 'MODERATE' ? 'Moderate (50% refund 24-48h)' : 'Strict (no refund <24h)'}</span>
                  </label>
                ))}
              </div>
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={instantBook} onChange={e => setInstantBook(e.target.checked)} className="rounded" />
                <span className="text-sm font-medium">Instant Book</span>
              </label>
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={breakfastIncluded} onChange={e => setBreakfastIncluded(e.target.checked)} className="rounded" />
                <span className="text-sm font-medium">Breakfast included</span>
              </label>
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={petFriendly} onChange={e => setPetFriendly(e.target.checked)} className="rounded" />
                <span className="text-sm font-medium">Pet friendly</span>
              </label>
              {petFriendly && (
                <div>
                  <label className="block text-sm text-gray-600 mb-1">Max pets</label>
                  <input type="number" value={maxPets} onChange={e => setMaxPets(Number(e.target.value))}
                    className="w-24 border rounded-lg px-3 py-2 text-sm" min={1} max={5} />
                </div>
              )}
            </div>
          )}

          {activeSection === 'rules' && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Check-in from</label>
                  <input type="time" value={checkInFrom} onChange={e => setCheckInFrom(e.target.value)}
                    className="w-full border rounded-lg px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Check-in until</label>
                  <input type="time" value={checkInUntil} onChange={e => setCheckInUntil(e.target.value)}
                    className="w-full border rounded-lg px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Check-out from</label>
                  <input type="time" value={checkOutFrom} onChange={e => setCheckOutFrom(e.target.value)}
                    className="w-full border rounded-lg px-3 py-2 text-sm" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Check-out until</label>
                  <input type="time" value={checkOutUntil} onChange={e => setCheckOutUntil(e.target.value)}
                    className="w-full border rounded-lg px-3 py-2 text-sm" />
                </div>
              </div>
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={childrenAllowed} onChange={e => setChildrenAllowed(e.target.checked)} className="rounded" />
                <span className="text-sm font-medium">Children allowed</span>
              </label>
            </div>
          )}

          {activeSection === 'accessibility' && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Accessibility Features</label>
              <div className="grid grid-cols-2 gap-2">
                {ACCESSIBILITY.map(af => (
                  <label key={af} className="flex items-center gap-2 cursor-pointer p-2 rounded-lg hover:bg-gray-50">
                    <input type="checkbox" checked={accessibilityFeatures.includes(af)}
                      onChange={() => setAccessibilityFeatures(prev => prev.includes(af) ? prev.filter(a => a !== af) : [...prev, af])}
                      className="rounded text-orange-500" />
                    <span className="text-sm">{af}</span>
                  </label>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="border-t px-6 py-4 flex justify-end gap-3">
          <button onClick={onClose} className="px-4 py-2 border rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-50">Cancel</button>
          <button onClick={handleSave} disabled={saving}
            className="px-6 py-2 bg-orange-500 text-white rounded-lg text-sm font-semibold hover:bg-orange-600 disabled:opacity-50 transition">
            {saving ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </div>
    </div>
  );
}
