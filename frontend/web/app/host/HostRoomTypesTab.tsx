'use client';

import { useEffect, useState, useCallback } from 'react';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import type { Listing, RoomType } from '@/types';
import HostInclusionsPanel from './HostInclusionsPanel';

const BED_TYPES = ['Double', 'Twin', 'King', 'Queen', 'Single', 'Sofa', 'Bunk'];
const COMMON_AMENITIES = ['WiFi', 'AC', 'TV', 'Mini-bar', 'Balcony', 'Safe', 'Hair Dryer', 'Coffee Maker'];

const SHARING_TYPES = [
  { value: 'PRIVATE', label: 'Private Room' },
  { value: 'TWO_SHARING', label: '2-Sharing' },
  { value: 'THREE_SHARING', label: '3-Sharing' },
  { value: 'FOUR_SHARING', label: '4-Sharing' },
  { value: 'DORMITORY', label: 'Dormitory' },
];
const ROOM_VARIANTS = [
  { value: 'AC', label: 'AC' },
  { value: 'NON_AC', label: 'Non-AC' },
  { value: 'FURNISHED', label: 'Furnished' },
  { value: 'SEMI_FURNISHED', label: 'Semi-Furnished' },
];
const STAY_MODES = [
  { value: 'NIGHTLY', label: 'Per Night' },
  { value: 'HOURLY', label: 'Per Hour' },
  { value: 'MONTHLY', label: 'Per Month' },
];

interface Props {
  token: string;
  listings: Listing[];
}

interface RoomTypeFormData {
  name: string;
  description: string;
  count: string;
  basePriceRupees: string;
  maxGuests: string;
  bedType: string;
  bedCount: string;
  areaSqft: string;
  amenities: string[];
  stayMode: string;
  sharingType: string;
  roomVariant: string;
  primaryPhotoUrl: string;
  photoUrls: string[];
}

const EMPTY_FORM: RoomTypeFormData = {
  name: '',
  description: '',
  count: '',
  basePriceRupees: '',
  maxGuests: '2',
  bedType: '',
  bedCount: '1',
  areaSqft: '',
  amenities: [],
  stayMode: 'NIGHTLY',
  sharingType: '',
  roomVariant: '',
  primaryPhotoUrl: '',
  photoUrls: [],
};

export default function HostRoomTypesTab({ token, listings }: Props) {
  const [selectedListingId, setSelectedListingId] = useState<string>('');
  const selectedListing = listings.find(l => l.id === selectedListingId);
  const isPgType = selectedListing?.type === 'PG' || selectedListing?.type === 'COLIVING';
  const isHotelType = selectedListing?.type === 'HOTEL' || selectedListing?.type === 'BUDGET_HOTEL' || selectedListing?.type === 'HOSTEL_DORM';
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Modal state
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<RoomTypeFormData>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  // Delete confirmation
  const [deletingId, setDeletingId] = useState<string | null>(null);

  // Set first listing as default
  useEffect(() => {
    if (listings.length > 0 && !selectedListingId) {
      setSelectedListingId(listings[0].id);
    }
  }, [listings, selectedListingId]);

  const fetchRoomTypes = useCallback(async () => {
    if (!selectedListingId || !token) return;
    setLoading(true);
    setError('');
    try {
      const data = await api.getRoomTypes(selectedListingId);
      setRoomTypes(data);
    } catch (e: any) {
      setError(e.message || 'Failed to load room types');
    } finally {
      setLoading(false);
    }
  }, [selectedListingId, token]);

  useEffect(() => {
    fetchRoomTypes();
  }, [fetchRoomTypes]);

  // Summary
  const totalRooms = roomTypes.reduce((sum, rt) => sum + rt.count, 0);
  const totalTypes = roomTypes.length;

  function openAddModal() {
    setEditingId(null);
    // Auto-set stayMode based on listing type
    const autoMode = isPgType ? 'MONTHLY' : isHotelType ? 'NIGHTLY' : 'NIGHTLY';
    setForm({ ...EMPTY_FORM, stayMode: autoMode });
    setFormError('');
    setShowModal(true);
  }

  function openEditModal(rt: RoomType) {
    setEditingId(rt.id);
    setForm({
      name: rt.name,
      description: rt.description || '',
      count: String(rt.count),
      basePriceRupees: String(rt.basePricePaise / 100),
      maxGuests: String(rt.maxGuests),
      bedType: rt.bedType || '',
      bedCount: String(rt.bedCount || 1),
      areaSqft: rt.areaSqft ? String(rt.areaSqft) : '',
      amenities: rt.amenities || [],
      stayMode: rt.stayMode || 'NIGHTLY',
      sharingType: rt.sharingType || '',
      roomVariant: rt.roomVariant || '',
      primaryPhotoUrl: rt.primaryPhotoUrl || '',
      photoUrls: rt.photoUrls || [],
    });
    setFormError('');
    setShowModal(true);
  }

  function closeModal() {
    setShowModal(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormError('');
  }

  function toggleAmenity(amenity: string) {
    setForm((prev) => ({
      ...prev,
      amenities: prev.amenities.includes(amenity)
        ? prev.amenities.filter((a) => a !== amenity)
        : [...prev.amenities, amenity],
    }));
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    if (!form.name.trim()) { setFormError('Name is required'); return; }
    if (!form.count || Number(form.count) < 1) { setFormError('Count must be at least 1'); return; }
    if (!form.basePriceRupees || Number(form.basePriceRupees) <= 0) { setFormError('Price must be greater than 0'); return; }

    setSaving(true);
    setFormError('');
    try {
      const payload = {
        name: form.name.trim(),
        description: form.description.trim() || null,
        count: Number(form.count),
        basePricePaise: Math.round(Number(form.basePriceRupees) * 100),
        maxGuests: Number(form.maxGuests) || 2,
        bedType: form.bedType || null,
        bedCount: Number(form.bedCount) || 1,
        areaSqft: form.areaSqft ? Number(form.areaSqft) : null,
        amenities: form.amenities.length > 0 ? form.amenities : null,
        stayMode: form.stayMode || null,
        sharingType: form.sharingType || null,
        roomVariant: form.roomVariant || null,
        primaryPhotoUrl: form.primaryPhotoUrl || null,
        photoUrls: form.photoUrls.length > 0 ? form.photoUrls : null,
      };

      if (editingId) {
        const updated = await api.updateRoomType(selectedListingId, editingId, payload, token);
        setRoomTypes((prev) => prev.map((rt) => (rt.id === editingId ? updated : rt)));
      } else {
        const created = await api.createRoomType(selectedListingId, payload, token);
        setRoomTypes((prev) => [...prev, created]);
      }
      closeModal();
    } catch (e: any) {
      setFormError(e.message || 'Failed to save room type');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(roomTypeId: string) {
    setDeletingId(roomTypeId);
    try {
      await api.deleteRoomType(selectedListingId, roomTypeId, token);
      setRoomTypes((prev) => prev.filter((rt) => rt.id !== roomTypeId));
    } catch (e: any) {
      setError(e.message || 'Failed to delete room type');
    } finally {
      setDeletingId(null);
    }
  }

  if (listings.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        No listings found. Create a listing first to manage room types.
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Listing Selector */}
      <div className="flex items-center gap-4 flex-wrap">
        <label className="text-sm font-medium text-gray-700">Listing:</label>
        <select
          value={selectedListingId}
          onChange={(e) => setSelectedListingId(e.target.value)}
          className="border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
        >
          {listings.map((l) => (
            <option key={l.id} value={l.id}>{l.title} ({l.city})</option>
          ))}
        </select>
      </div>

      {/* Summary Bar */}
      {totalTypes > 0 && (
        <div className="flex items-center justify-between bg-white border rounded-xl p-4 shadow-sm">
          <div className="text-sm text-gray-600">
            Total rooms: <span className="font-semibold text-gray-800">{totalRooms}</span> across{' '}
            <span className="font-semibold text-gray-800">{totalTypes}</span> room type{totalTypes !== 1 ? 's' : ''}
          </div>
          <button
            onClick={openAddModal}
            className="bg-orange-500 hover:bg-orange-600 text-white font-semibold px-4 py-2 rounded-xl text-sm transition"
          >
            + Add Room Type
          </button>
        </div>
      )}

      {error && <p className="text-red-600 text-sm">{error}</p>}

      {loading ? (
        <div className="text-center py-12 text-gray-400">Loading room types...</div>
      ) : roomTypes.length === 0 ? (
        /* Empty State */
        <div className="text-center py-16 bg-white border rounded-xl shadow-sm">
          <div className="text-4xl mb-4">🏨</div>
          <h3 className="text-lg font-semibold text-gray-700 mb-2">No room types yet</h3>
          <p className="text-sm text-gray-500 mb-6 max-w-md mx-auto">
            Add your first room type to enable multi-room bookings.
          </p>
          <button
            onClick={openAddModal}
            className="bg-orange-500 hover:bg-orange-600 text-white font-semibold px-6 py-2.5 rounded-xl text-sm transition"
          >
            + Add Room Type
          </button>
        </div>
      ) : (
        /* Room Type Cards */
        <div className="grid gap-4 md:grid-cols-2">
          {roomTypes.map((rt) => (
            <div key={rt.id} className="bg-white border rounded-xl overflow-hidden shadow-sm hover:shadow-md transition">
              {/* Room photo */}
              {rt.primaryPhotoUrl ? (
                <img src={rt.primaryPhotoUrl} alt={rt.name} className="w-full h-36 object-cover" />
              ) : (
                <div className="w-full h-20 bg-gradient-to-r from-gray-100 to-gray-50 flex items-center justify-center">
                  <span className="text-3xl">🛏️</span>
                </div>
              )}
              {rt.photoUrls && rt.photoUrls.length > 0 && (
                <div className="flex gap-1 px-2 -mt-4 relative z-10">
                  {rt.photoUrls.slice(0, 4).map((url, i) => (
                    <img key={i} src={url} alt="" className="w-12 h-10 object-cover rounded border-2 border-white shadow-sm" />
                  ))}
                  {rt.photoUrls.length > 4 && (
                    <span className="w-12 h-10 rounded bg-black/50 text-white text-xs flex items-center justify-center border-2 border-white">
                      +{rt.photoUrls.length - 4}
                    </span>
                  )}
                </div>
              )}
              <div className="p-5">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <h4 className="font-semibold text-gray-800">{rt.name}</h4>
                  {rt.description && (
                    <p className="text-xs text-gray-500 mt-1 line-clamp-2">{rt.description}</p>
                  )}
                </div>
                <div className="text-right">
                  <div className="text-lg font-bold text-orange-600">{formatPaise(rt.basePricePaise)}</div>
                  <div className="text-xs text-gray-400">
                    {rt.stayMode === 'MONTHLY' || isPgType ? 'per month' : rt.stayMode === 'HOURLY' ? 'per hour' : 'per night'}
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2 text-sm text-gray-600 mb-3">
                <div>
                  <span className="text-gray-400">Guests:</span>{' '}
                  <span className="font-medium">{rt.maxGuests}</span>
                </div>
                {rt.bedType && (
                  <div>
                    <span className="text-gray-400">Bed:</span>{' '}
                    <span className="font-medium">{rt.bedType} &times; {rt.bedCount || 1}</span>
                  </div>
                )}
                <div>
                  <span className="text-gray-400">Rooms:</span>{' '}
                  <span className="font-medium">{rt.count}</span>
                </div>
                {rt.sharingType && (
                  <div>
                    <span className="text-gray-400">Sharing:</span>{' '}
                    <span className="font-medium">{rt.sharingType.replace('_', '-').replace('TWO', '2').replace('THREE', '3').replace('FOUR', '4')}</span>
                  </div>
                )}
                {rt.roomVariant && (
                  <div>
                    <span className="text-gray-400">Variant:</span>{' '}
                    <span className="font-medium">{rt.roomVariant.replace('_', '-')}</span>
                  </div>
                )}
                {rt.totalBeds != null && (
                  <div>
                    <span className="text-gray-400">Beds:</span>{' '}
                    <span className="font-medium">{rt.occupiedBeds || 0}/{rt.totalBeds} occupied</span>
                  </div>
                )}
                {rt.stayMode === 'HOURLY' && (
                  <div>
                    <span className="text-gray-400">Mode:</span>{' '}
                    <span className="font-medium text-blue-600">Hourly</span>
                  </div>
                )}
                {rt.areaSqft && (
                  <div>
                    <span className="text-gray-400">Area:</span>{' '}
                    <span className="font-medium">{rt.areaSqft} sq ft</span>
                  </div>
                )}
              </div>

              {/* Amenities */}
              {rt.amenities && rt.amenities.length > 0 && (
                <div className="flex flex-wrap gap-1.5 mb-3">
                  {rt.amenities.map((a) => (
                    <span key={a} className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
                      {a}
                    </span>
                  ))}
                </div>
              )}

              {/* Inclusions & Perks */}
              <HostInclusionsPanel
                token={token}
                listingId={selectedListingId}
                roomTypeId={rt.id}
                roomTypeName={rt.name}
                inclusions={rt.inclusions || []}
                onUpdate={fetchRoomTypes}
              />

              {/* Actions */}
              <div className="flex gap-2 pt-2 border-t mt-3">
                <button
                  onClick={() => openEditModal(rt)}
                  className="flex-1 text-sm font-medium text-orange-600 hover:text-orange-700 py-1.5 rounded-lg hover:bg-orange-50 transition"
                >
                  Edit
                </button>
                <button
                  onClick={() => handleDelete(rt.id)}
                  disabled={deletingId === rt.id}
                  className="flex-1 text-sm font-medium text-red-500 hover:text-red-600 py-1.5 rounded-lg hover:bg-red-50 transition disabled:opacity-50"
                >
                  {deletingId === rt.id ? 'Deleting...' : 'Delete'}
                </button>
              </div>
              </div>{/* close p-5 */}
            </div>
          ))}
        </div>
      )}

      {/* ── Add/Edit Modal ──────────────────────────────────────── */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              <div className="flex items-center justify-between mb-5">
                <h3 className="text-lg font-semibold text-gray-800">
                  {editingId ? 'Edit Room Type' : 'Add Room Type'}
                </h3>
                <button onClick={closeModal} className="text-gray-400 hover:text-gray-600 text-xl leading-none">
                  &times;
                </button>
              </div>

              {formError && <p className="text-red-600 text-sm mb-4">{formError}</p>}

              <form onSubmit={handleSave} className="space-y-4">
                {/* Name */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Name *</label>
                  <input
                    type="text"
                    value={form.name}
                    onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                    placeholder="e.g. Deluxe Double Room"
                    className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                    required
                  />
                </div>

                {/* Description */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                  <textarea
                    value={form.description}
                    onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                    placeholder="Spacious room with city view..."
                    rows={3}
                    className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                  />
                </div>

                {/* Count & Price row */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Number of rooms *</label>
                    <input
                      type="number"
                      min="1"
                      value={form.count}
                      onChange={(e) => setForm((f) => ({ ...f, count: e.target.value }))}
                      placeholder="e.g. 5"
                      className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Price {form.stayMode === 'HOURLY' ? 'per hour' : form.stayMode === 'MONTHLY' ? 'per month' : 'per night'} (INR) *
                    </label>
                    <input
                      type="number"
                      min="1"
                      step="1"
                      value={form.basePriceRupees}
                      onChange={(e) => setForm((f) => ({ ...f, basePriceRupees: e.target.value }))}
                      placeholder="e.g. 2500"
                      className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                      required
                    />
                  </div>
                </div>

                {/* Max Guests & Bed Type */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Max guests</label>
                    <input
                      type="number"
                      min="1"
                      value={form.maxGuests}
                      onChange={(e) => setForm((f) => ({ ...f, maxGuests: e.target.value }))}
                      className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Bed type</label>
                    <select
                      value={form.bedType}
                      onChange={(e) => setForm((f) => ({ ...f, bedType: e.target.value }))}
                      className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                    >
                      <option value="">Select...</option>
                      {BED_TYPES.map((bt) => (
                        <option key={bt} value={bt}>{bt}</option>
                      ))}
                    </select>
                  </div>
                </div>

                {/* Bed Count & Area */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Bed count</label>
                    <input
                      type="number"
                      min="1"
                      value={form.bedCount}
                      onChange={(e) => setForm((f) => ({ ...f, bedCount: e.target.value }))}
                      className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Area (sq ft)</label>
                    <input
                      type="number"
                      min="0"
                      value={form.areaSqft}
                      onChange={(e) => setForm((f) => ({ ...f, areaSqft: e.target.value }))}
                      placeholder="Optional"
                      className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                    />
                  </div>
                </div>

                {/* Stay Mode — all property types */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Pricing Mode</label>
                    <div className="flex gap-2">
                      {STAY_MODES.map((sm) => (
                        <button key={sm.value} type="button"
                          onClick={() => setForm((f) => ({ ...f, stayMode: sm.value }))}
                          className={`px-4 py-2 rounded-lg text-sm font-medium border transition ${
                            form.stayMode === sm.value
                              ? 'bg-orange-500 text-white border-orange-500'
                              : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50'
                          }`}>
                          {sm.label}
                        </button>
                      ))}
                    </div>
                </div>

                {/* PG Sharing Type & Room Variant */}
                {isPgType && (
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Sharing Type</label>
                      <select value={form.sharingType}
                        onChange={(e) => setForm((f) => ({ ...f, sharingType: e.target.value }))}
                        className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500">
                        <option value="">Select...</option>
                        {SHARING_TYPES.map((st) => (
                          <option key={st.value} value={st.value}>{st.label}</option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Room Variant</label>
                      <select value={form.roomVariant}
                        onChange={(e) => setForm((f) => ({ ...f, roomVariant: e.target.value }))}
                        className="w-full border rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-500 focus:border-orange-500">
                        <option value="">Select...</option>
                        {ROOM_VARIANTS.map((rv) => (
                          <option key={rv.value} value={rv.value}>{rv.label}</option>
                        ))}
                      </select>
                    </div>
                  </div>
                )}

                {/* Amenities */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Amenities</label>
                  <div className="flex flex-wrap gap-2">
                    {COMMON_AMENITIES.map((amenity) => (
                      <button
                        key={amenity}
                        type="button"
                        onClick={() => toggleAmenity(amenity)}
                        className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition ${
                          form.amenities.includes(amenity)
                            ? 'bg-orange-500 text-white border-orange-500'
                            : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50'
                        }`}
                      >
                        {amenity}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Room Photos */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Room Photos</label>
                  {/* Primary photo */}
                  <div className="mb-3">
                    <label className="text-xs text-gray-500 block mb-1">Cover photo URL</label>
                    <input
                      className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-400"
                      placeholder="https://cdn.example.com/room-cover.jpg"
                      value={form.primaryPhotoUrl}
                      onChange={(e) => setForm({ ...form, primaryPhotoUrl: e.target.value })}
                    />
                    {form.primaryPhotoUrl && (
                      <img src={form.primaryPhotoUrl} alt="Cover" className="mt-2 w-full h-32 object-cover rounded-lg border" />
                    )}
                  </div>
                  {/* Gallery photos */}
                  <label className="text-xs text-gray-500 block mb-1">Gallery photo URLs (one per line, max 10)</label>
                  <textarea
                    className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-400"
                    rows={3}
                    placeholder={"https://cdn.example.com/room-1.jpg\nhttps://cdn.example.com/room-2.jpg"}
                    value={form.photoUrls.join('\n')}
                    onChange={(e) => setForm({ ...form, photoUrls: e.target.value.split('\n').map(u => u.trim()).filter(Boolean) })}
                  />
                  {form.photoUrls.length > 0 && (
                    <div className="flex gap-2 mt-2 overflow-x-auto">
                      {form.photoUrls.filter(Boolean).map((url, i) => (
                        <img key={i} src={url} alt={`Photo ${i + 1}`} className="w-20 h-16 object-cover rounded-lg border shrink-0" />
                      ))}
                    </div>
                  )}
                </div>

                {/* Actions */}
                <div className="flex gap-3 pt-2">
                  <button
                    type="button"
                    onClick={closeModal}
                    className="flex-1 border border-gray-300 text-gray-600 font-semibold px-4 py-2.5 rounded-xl text-sm hover:bg-gray-50 transition"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={saving}
                    className="flex-1 bg-orange-500 hover:bg-orange-600 text-white font-semibold px-4 py-2.5 rounded-xl text-sm transition disabled:opacity-50"
                  >
                    {saving ? 'Saving...' : editingId ? 'Update' : 'Create'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
