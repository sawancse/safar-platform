'use client';

import { useEffect, useState } from 'react';
import { useParams, useSearchParams, useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import RazorpayButton from '@/components/RazorpayButton';
import { formatPaise } from '@/lib/utils';
import type { Listing, Booking, RoomType, RoomTypeInclusion } from '@/types';
import GuestListForm, { type GuestInfo } from '@/components/GuestListForm';

function fmtDate(iso: string) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
}

export default function BookPage() {
  const { listingId } = useParams<{ listingId: string }>();
  const searchParams = useSearchParams();
  const router = useRouter();

  // Editable booking params (initialized from URL)
  const [checkIn, setCheckIn] = useState(searchParams.get('checkIn') ?? '');
  const [checkOut, setCheckOut] = useState(searchParams.get('checkOut') ?? '');
  const [rooms, setRooms] = useState(Number(searchParams.get('rooms') ?? '1'));
  const [adults, setAdults] = useState(Number(searchParams.get('adults') ?? searchParams.get('guests') ?? '1'));
  const [childrenCount, setChildrenCount] = useState(Number(searchParams.get('children') ?? '0'));
  const [infants, setInfants] = useState(Number(searchParams.get('infants') ?? '0'));
  const [pets, setPets] = useState(Number(searchParams.get('pets') ?? '0'));
  const existingBookingId = searchParams.get('bookingId');
  const preselectedRoomTypeId = searchParams.get('roomTypeId');

  // Parse multi-room selections from URL
  const urlRoomSelections: { id: string; c: number }[] = (() => {
    try {
      const raw = searchParams.get('roomSelections');
      if (!raw) return [];
      return JSON.parse(raw);
    } catch { return []; }
  })();

  // Edit mode toggles
  const [editingDates, setEditingDates] = useState(false);
  const [editingGuests, setEditingGuests] = useState(false);

  const [listing, setListing] = useState<Listing | null>(null);
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([]);
  const [selectedRoomType, setSelectedRoomType] = useState<RoomType | null>(null);
  const [selectedRoomSelections, setSelectedRoomSelections] = useState(urlRoomSelections);
  const [selectedInclusionIds, setSelectedInclusionIds] = useState<string[]>([]);
  const [guestList, setGuestList] = useState<GuestInfo[]>([]);
  const [booking, setBooking] = useState<Booking | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [token, setToken] = useState('');
  const [ready, setReady] = useState(false);

  // Guest details form
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [bookingFor, setBookingFor] = useState<'self' | 'other'>('self');
  const [travelForWork, setTravelForWork] = useState<'yes' | 'no' | ''>('');
  const [paperlessConfirm, setPaperlessConfirm] = useState(true);
  const [airportShuttle, setAirportShuttle] = useState(false);
  const [specialRequests, setSpecialRequests] = useState('');
  const [arrivalTime, setArrivalTime] = useState('');

  const today = new Date().toISOString().split('T')[0];

  useEffect(() => {
    const t = localStorage.getItem('access_token') ?? '';
    if (!t) {
      router.push(`/auth?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`);
      return;
    }
    setToken(t);
    api.getListing(listingId).then((l) => {
      setListing(l);
      // Auto-adjust rooms if guest count exceeds per-room capacity
      const lTotalRooms = l.totalRooms ?? 1;
      const perRoom = lTotalRooms > 1 && l.maxGuests
        ? Math.max(1, Math.floor(l.maxGuests / lTotalRooms))
        : Math.min(l.maxGuests ?? 2, 4);
      const neededRooms = Math.ceil((adults + childrenCount) / perRoom);
      if (neededRooms > rooms) setRooms(Math.min(neededRooms, lTotalRooms));
      setReady(true);
    }).catch(() => setError('Listing not found'));

    if (existingBookingId) {
      api.getBooking(existingBookingId, t).then((b) => {
        if (b.status === 'PENDING_PAYMENT') setBooking(b);
      }).catch(() => {});
    }
  }, [listingId, router, existingBookingId]);

  // Fetch room types — available if dates set, all otherwise
  useEffect(() => {
    if (!listingId) return;
    const fetchFn = checkIn && checkOut
      ? api.getAvailableRoomTypes(listingId, checkIn, checkOut)
      : api.getRoomTypes(listingId);
    fetchFn
      .then((types) => {
        setRoomTypes(types);
        // Pre-select room type from URL param
        if (preselectedRoomTypeId && !selectedRoomType) {
          const match = types.find(t => t.id === preselectedRoomTypeId);
          if (match) setSelectedRoomType(match);
        }
        // Auto-select if only one room type and it wasn't selected before
        else if (types.length === 1 && !selectedRoomType) {
          setSelectedRoomType(types[0]);
        }
        // If previously selected room type is no longer available, deselect
        if (selectedRoomType && !types.find(t => t.id === selectedRoomType.id)) {
          setSelectedRoomType(null);
        }
      })
      .catch(() => setRoomTypes([]));
  }, [listingId, checkIn, checkOut]);

  // PG / Hotel helpers
  const isPG = listing?.type === 'PG' || listing?.type === 'COLIVING';
  const isHotel = listing?.type === 'HOTEL' || listing?.type === 'BUDGET_HOTEL';

  // ── Pricing unit from listing (source of truth) ──
  const pricingUnit = listing?.pricingUnit || (isPG ? 'MONTH' : 'NIGHT');
  const isMonthly = pricingUnit === 'MONTH';
  const isHourly = pricingUnit === 'HOUR';
  const isNightly = pricingUnit === 'NIGHT';

  // Calculations
  const nights = checkIn && checkOut
    ? Math.max(1, Math.ceil((new Date(checkOut).getTime() - new Date(checkIn).getTime()) / 86400000))
    : 0;
  const hours = checkIn && checkOut
    ? Math.max(1, Math.ceil((new Date(checkOut).getTime() - new Date(checkIn).getTime()) / 3600000))
    : 0;
  const fullMonths = isMonthly ? Math.floor(nights / 30) : 0;
  const remainingDays = isMonthly ? nights % 30 : 0;
  const totalGuests = adults + childrenCount;

  // Base rate = price per unit (per night / per month / per hour)
  const hasMultiRoom = selectedRoomSelections.length > 0 && roomTypes.length > 0;
  const baseRate = selectedRoomType ? selectedRoomType.basePricePaise : (listing?.basePricePaise ?? 0);

  // Calculate subtotal based on pricing unit
  let roomSubtotalPaise = 0;
  const proratedDaysPaise = isMonthly && remainingDays > 0 ? Math.round(baseRate * remainingDays / 30) : 0;
  if (hasMultiRoom) {
    // Multi-room: sum each room type × count × duration
    for (const sel of selectedRoomSelections) {
      const rt = roomTypes.find(r => r.id === sel.id);
      const price = rt?.basePricePaise ?? baseRate;
      if (isMonthly) {
        const prorated = remainingDays > 0 ? Math.round(price * remainingDays / 30) : 0;
        roomSubtotalPaise += (price * fullMonths + prorated) * sel.c;
      } else if (isHourly) {
        roomSubtotalPaise += price * hours * sel.c;
      } else {
        roomSubtotalPaise += price * nights * sel.c;
      }
    }
  } else {
    if (isMonthly) {
      roomSubtotalPaise = (baseRate * fullMonths + proratedDaysPaise) * rooms;
    } else if (isHourly) {
      roomSubtotalPaise = baseRate * hours * rooms;
    } else {
      roomSubtotalPaise = baseRate * nights * rooms;
    }
  }

  const securityDepositPaise = isPG ? (listing?.securityDepositPaise ?? 0) : 0;
  const cleaningFeePaise = isMonthly ? 0 : (listing?.cleaningFeePaise ?? 0);
  const gstPaise = listing?.gstApplicable ? Math.round(roomSubtotalPaise * 0.18) : 0;
  const totalPaise = roomSubtotalPaise + cleaningFeePaise + gstPaise + securityDepositPaise;

  // Unit labels
  const unitLabel = isMonthly ? 'month' : isHourly ? 'hour' : 'night';
  const durationValue = isMonthly ? fullMonths : isHourly ? hours : nights;
  const durationLabel = isMonthly
    ? `${fullMonths} month${fullMonths !== 1 ? 's' : ''}${remainingDays > 0 ? ` ${remainingDays} day${remainingDays !== 1 ? 's' : ''}` : ''}`
    : isHourly
      ? `${hours} hour${hours !== 1 ? 's' : ''}`
      : `${nights} night${nights > 1 ? 's' : ''}`;

  const isFormValid = firstName.trim() && lastName.trim() && email.trim() && phone.trim() && (isHourly ? hours > 0 : nights > 0);

  const guestLabel = [
    `${adults} adult${adults !== 1 ? 's' : ''}`,
    childrenCount > 0 ? `${childrenCount} child${childrenCount !== 1 ? 'ren' : ''}` : '',
    infants > 0 ? `${infants} infant${infants !== 1 ? 's' : ''}` : '',
    pets > 0 ? `${pets} pet${pets !== 1 ? 's' : ''}` : '',
  ].filter(Boolean).join(', ');

  async function handleCreateBooking() {
    if (!token || !isFormValid || booking) return;
    setLoading(true);
    setError('');
    try {
      const b = await api.createBooking(
        {
          listingId,
          checkIn: checkIn + 'T14:00:00',
          checkOut: checkOut + 'T11:00:00',
          guestsCount: totalGuests,
          adultsCount: adults,
          childrenCount,
          infantsCount: infants,
          petsCount: pets,
          roomsCount: rooms,
          roomTypeId: selectedRoomType?.id,
          guestFirstName: firstName.trim(),
          guestLastName: lastName.trim(),
          guestEmail: email.trim(),
          guestPhone: phone.trim(),
          bookingFor,
          travelForWork: travelForWork === 'yes',
          airportShuttle,
          specialRequests: specialRequests.trim() || undefined,
          arrivalTime: arrivalTime || undefined,
          selectedInclusionIds: selectedInclusionIds.length > 0 ? selectedInclusionIds : undefined,
          roomSelections: selectedRoomSelections.length > 0
            ? selectedRoomSelections.map(s => ({ roomTypeId: s.id, count: s.c }))
            : undefined,
          guests: guestList.length > 0
            ? guestList.filter(g => g.fullName.trim()).map(g => ({
                fullName: g.fullName.trim(),
                email: g.email.trim() || undefined,
                phone: g.phone.trim() || undefined,
                age: g.age ? Number(g.age) : undefined,
                idType: g.idType || undefined,
                idNumber: g.idNumber.trim() || undefined,
                roomAssignment: g.roomAssignment || undefined,
                isPrimary: g.isPrimary,
              }))
            : undefined,
        } as any,
        token
      );
      setBooking(b);
    } catch (e: any) {
      setError(e.message || 'Booking failed. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  function onPaymentSuccess() {
    setSuccess(true);
    setTimeout(() => router.push('/dashboard'), 2000);
  }

  if (!ready || !listing) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center text-gray-400">
        {error ? <p className="text-red-500">{error}</p> : (
          <><div className="animate-spin text-4xl mb-4">&#x23F3;</div><p>Loading...</p></>
        )}
      </div>
    );
  }

  if (success) {
    return (
      <div className="max-w-lg mx-auto px-4 py-20 text-center">
        <div className="text-6xl mb-4">&#x1F389;</div>
        <h1 className="text-2xl font-bold text-green-600 mb-2">Booking confirmed!</h1>
        <p className="text-gray-600">Redirecting to your dashboard...</p>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-10">
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-8">
        {/* Left column — Form */}
        <div className="lg:col-span-3 space-y-6">
          <h1 className="text-2xl font-bold">Enter your details</h1>
          <p className="text-sm text-gray-500">Almost done! Just fill in the <span className="text-red-500">*</span> required info</p>

          {/* Guest details */}
          <div className="border rounded-2xl p-5 space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">First name <span className="text-red-500">*</span></label>
                <input type="text" value={firstName} onChange={(e) => setFirstName(e.target.value)}
                  className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500" placeholder="First name" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Last name <span className="text-red-500">*</span></label>
                <input type="text" value={lastName} onChange={(e) => setLastName(e.target.value)}
                  className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500" placeholder="Last name" />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email address <span className="text-red-500">*</span></label>
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500" placeholder="email@example.com" />
              <p className="text-xs text-gray-400 mt-1">Confirmation email sent to this address</p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Phone number <span className="text-red-500">*</span></label>
              <div className="flex gap-2">
                <div className="flex items-center border rounded-lg px-3 py-2 text-sm bg-gray-50 text-gray-600 shrink-0">+91</div>
                <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)}
                  className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500" placeholder="Phone number" />
              </div>
            </div>
            <label className="flex items-start gap-2 cursor-pointer">
              <input type="checkbox" checked={paperlessConfirm} onChange={(e) => setPaperlessConfirm(e.target.checked)} className="mt-0.5 accent-orange-500" />
              <span className="text-sm text-gray-700">Yes, I want free paperless confirmation (recommended)</span>
            </label>
          </div>

          {/* Booking for */}
          <div className="border rounded-2xl p-5 space-y-3">
            <p className="text-sm font-medium text-gray-700">Who are you booking for?</p>
            {(['self', 'other'] as const).map(v => (
              <label key={v} className="flex items-center gap-2 cursor-pointer">
                <input type="radio" name="bookingFor" checked={bookingFor === v} onChange={() => setBookingFor(v)} className="accent-orange-500" />
                <span className="text-sm text-gray-700">{v === 'self' ? "I'm the main guest" : "I'm booking for someone else"}</span>
              </label>
            ))}
          </div>

          {/* Traveling for work */}
          <div className="border rounded-2xl p-5 space-y-3">
            <p className="text-sm font-medium text-gray-700">Are you traveling for work?</p>
            <div className="flex gap-4">
              {(['yes', 'no'] as const).map(v => (
                <label key={v} className="flex items-center gap-2 cursor-pointer">
                  <input type="radio" name="travelWork" checked={travelForWork === v} onChange={() => setTravelForWork(v)} className="accent-orange-500" />
                  <span className="text-sm text-gray-700 capitalize">{v}</span>
                </label>
              ))}
            </div>
          </div>

          {/* Add to your stay */}
          <div className="border rounded-2xl p-5 space-y-3">
            <h3 className="font-semibold text-sm">Add to your stay</h3>
            <label className="flex items-start gap-2 cursor-pointer">
              <input type="checkbox" checked={airportShuttle} onChange={(e) => setAirportShuttle(e.target.checked)} className="mt-0.5 accent-orange-500" />
              <div>
                <p className="text-sm text-gray-700">I'm interested in requesting an airport shuttle</p>
                <p className="text-xs text-gray-400">We'll tell your accommodation so they can provide details and costs.</p>
              </div>
            </label>
          </div>

          {/* Multi-room selections summary (from listing page) */}
          {selectedRoomSelections.length > 0 && roomTypes.length > 0 && (
            <div className="border rounded-2xl p-5 space-y-3">
              <h3 className="font-semibold text-sm">Your Room Selection</h3>
              <div className="space-y-2">
                {selectedRoomSelections.map((sel) => {
                  const rt = roomTypes.find(r => r.id === sel.id);
                  if (!rt) return null;
                  return (
                    <div key={sel.id} className="flex items-center justify-between bg-orange-50 border border-orange-200 rounded-xl p-3">
                      <div className="flex items-center gap-3">
                        {rt.primaryPhotoUrl && (
                          <img src={rt.primaryPhotoUrl.startsWith('http') ? rt.primaryPhotoUrl : `http://localhost:8080${rt.primaryPhotoUrl}`}
                            alt={rt.name} className="w-14 h-10 rounded-lg object-cover" />
                        )}
                        <div>
                          <p className="text-sm font-medium text-gray-800">{rt.name}</p>
                          <p className="text-xs text-gray-500">Max {rt.maxGuests} guests · {rt.bedType || 'Standard bed'}</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="text-sm font-bold text-orange-600">{sel.c} room{sel.c > 1 ? 's' : ''}</p>
                        <p className="text-xs text-gray-500">{formatPaise(rt.basePricePaise)}/night each</p>
                      </div>
                    </div>
                  );
                })}
              </div>
              <p className="text-xs text-gray-400">
                Total: {selectedRoomSelections.reduce((s, r) => s + r.c, 0)} room{selectedRoomSelections.reduce((s, r) => s + r.c, 0) > 1 ? 's' : ''}
              </p>
            </div>
          )}

          {/* Room type picker (single select — shown when no multi-room selections) */}
          {selectedRoomSelections.length === 0 && roomTypes.length > 0 && (
            <div className="border rounded-2xl p-5 space-y-3">
              <h3 className="font-semibold text-sm">Select room type</h3>
              <p className="text-xs text-gray-400">Choose the room type for your stay</p>
              <div className="space-y-2">
                {roomTypes.map((rt) => (
                  <label key={rt.id}
                    className={`flex items-start gap-3 p-3 border rounded-xl cursor-pointer transition ${
                      selectedRoomType?.id === rt.id ? 'border-orange-500 bg-orange-50' : 'hover:border-gray-400'
                    }`}>
                    <input
                      type="radio"
                      name="roomType"
                      checked={selectedRoomType?.id === rt.id}
                      onChange={() => { setSelectedRoomType(rt); setSelectedInclusionIds([]); }}
                      className="mt-1 accent-orange-500"
                    />
                    {/* Room type photo */}
                    {rt.primaryPhotoUrl && (
                      <img src={rt.primaryPhotoUrl.startsWith('http') ? rt.primaryPhotoUrl : `http://localhost:8080${rt.primaryPhotoUrl}`} alt={rt.name}
                        className="w-20 h-16 rounded-lg object-cover shrink-0" />
                    )}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <p className="font-medium text-sm">{rt.name}</p>
                        <p className="font-semibold text-sm text-orange-600">{formatPaise(rt.basePricePaise)}/{unitLabel}</p>
                      </div>
                      {rt.description && <p className="text-xs text-gray-500 mt-0.5">{rt.description}</p>}
                      <div className="flex flex-wrap gap-2 mt-1.5">
                        {rt.bedType && (
                          <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">
                            {rt.bedType}{rt.bedCount && rt.bedCount > 1 ? ` x${rt.bedCount}` : ''}
                          </span>
                        )}
                        <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">
                          Max {rt.maxGuests} guest{rt.maxGuests > 1 ? 's' : ''}
                        </span>
                        {rt.areaSqft && (
                          <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">{rt.areaSqft} sq ft</span>
                        )}
                        {rt.availableCount != null && (
                          <span className={`text-xs px-2 py-0.5 rounded ${
                            rt.availableCount <= 2 ? 'bg-red-100 text-red-600' : 'bg-green-100 text-green-600'
                          }`}>
                            {rt.availableCount} left
                          </span>
                        )}
                      </div>
                      {rt.amenities && rt.amenities.length > 0 && (
                        <p className="text-xs text-gray-400 mt-1">{rt.amenities.join(', ')}</p>
                      )}
                      {/* Inclusions highlights */}
                      {rt.inclusions && rt.inclusions.length > 0 && (
                        <div className="mt-2 space-y-1">
                          {rt.inclusions.filter(i => i.inclusionMode === 'INCLUDED' || i.inclusionMode === 'COMPLIMENTARY').map(inc => (
                            <span key={inc.id} className="inline-flex items-center gap-1 text-xs text-green-700 bg-green-50 px-2 py-0.5 rounded mr-1">
                              &#10003; {inc.name}
                            </span>
                          ))}
                          {rt.inclusions.filter(i => i.inclusionMode === 'PAID_ADDON').map(inc => (
                            <label key={inc.id}
                              className="flex items-center gap-1.5 text-xs text-blue-700 bg-blue-50 px-2 py-1 rounded cursor-pointer hover:bg-blue-100 transition"
                              onClick={e => e.stopPropagation()}>
                              <input type="checkbox"
                                checked={selectedInclusionIds.includes(inc.id)}
                                onChange={e => {
                                  if (!selectedRoomType || selectedRoomType.id !== rt.id) setSelectedRoomType(rt);
                                  setSelectedInclusionIds(prev =>
                                    e.target.checked ? [...prev, inc.id] : prev.filter(id => id !== inc.id)
                                  );
                                }}
                                className="rounded border-blue-300 text-blue-600 focus:ring-blue-500" />
                              + {inc.name}
                              {inc.chargePaise > 0 && (
                                <span className="text-blue-500 font-medium ml-auto">
                                  +{formatPaise(inc.chargePaise)}/{inc.chargeType.replace('PER_', '').toLowerCase()}
                                </span>
                              )}
                            </label>
                          ))}
                        </div>
                      )}
                    </div>
                  </label>
                ))}
              </div>
            </div>
          )}

          {/* Special requests */}
          <div className="border rounded-2xl p-5 space-y-3">
            <h3 className="font-semibold text-sm">Special requests</h3>
            <p className="text-xs text-gray-400">Special requests can't be guaranteed, but the property will do its best.</p>
            <textarea value={specialRequests} onChange={(e) => setSpecialRequests(e.target.value)} rows={3}
              className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500 resize-none"
              placeholder="Please write your requests here (optional)" />
          </div>

          {/* Guest list */}
          <GuestListForm
            roomNames={
              selectedRoomSelections.length > 0
                ? selectedRoomSelections.flatMap(sel => {
                    const rt = roomTypes.find(r => r.id === sel.id);
                    if (!rt) return [];
                    return sel.c > 1
                      ? Array.from({ length: sel.c }, (_, i) => `${rt.name} ${i + 1}`)
                      : [rt.name];
                  })
                : selectedRoomType
                  ? [selectedRoomType.name]
                  : roomTypes.map(rt => rt.name)
            }
            onUpdate={setGuestList}
            primaryName={firstName && lastName ? `${firstName} ${lastName}`.trim() : undefined}
            primaryEmail={email}
            primaryPhone={phone}
            totalGuests={totalGuests}
          />

          {/* Your arrival time */}
          <div className="border rounded-2xl p-5 space-y-3">
            <h3 className="font-semibold text-sm">Your arrival time</h3>
            <div className="flex items-start gap-2 text-sm text-gray-600">
              <span className="text-green-600 text-base mt-0.5">&#x2713;</span>
              <span>Your rooms will be ready for check-in at {listing.checkinTime || '14:00'}</span>
            </div>
            <div className="flex items-start gap-2 text-sm text-gray-600">
              <span className="text-blue-500 text-base mt-0.5">&#x1F6CE;</span>
              <span>24-hour front desk – Help whenever you need it!</span>
            </div>
            <div className="mt-2">
              <label className="block text-sm text-gray-700 mb-1">Add your estimated arrival time <span className="text-xs text-gray-400">(optional)</span></label>
              <select value={arrivalTime} onChange={(e) => setArrivalTime(e.target.value)}
                className="w-full border rounded-lg px-3 py-2 text-sm outline-none focus:border-orange-500 bg-white text-gray-700">
                <option value="">Please select</option>
                <option value="I don't know">I don't know</option>
                {Array.from({ length: 24 }, (_, i) => {
                  const h = i.toString().padStart(2, '0');
                  const next = ((i + 1) % 24).toString().padStart(2, '0');
                  return (
                    <option key={i} value={`${h}:00-${next}:00`}>
                      {`${h}:00 – ${next}:00`}
                    </option>
                  );
                })}
              </select>
            </div>
          </div>

          {error && <div className="bg-red-50 text-red-600 rounded-xl px-4 py-3 text-sm">{error}</div>}

          {/* Action button */}
          {!booking ? (
            <button onClick={handleCreateBooking} disabled={loading || !isFormValid}
              className="w-full bg-orange-500 hover:bg-orange-600 text-white font-semibold py-3 rounded-xl disabled:opacity-50 transition">
              {loading ? 'Creating booking...' : `Proceed to Payment · ${formatPaise(totalPaise)}`}
            </button>
          ) : (
            <div className="space-y-3">
              <p className="text-sm font-semibold text-gray-700">
                Choose payment method · Pay {formatPaise(booking.totalAmountPaise)}
              </p>
              <RazorpayButton bookingId={booking.id} amountPaise={booking.totalAmountPaise} token={token} onSuccess={onPaymentSuccess} />
              <div className="relative">
                <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-gray-200" /></div>
                <div className="relative flex justify-center text-xs"><span className="bg-white px-3 text-gray-400">or</span></div>
              </div>
              <button onClick={async () => {
                setLoading(true);
                try { await api.confirmBooking(booking.id, token); setSuccess(true); setTimeout(() => router.push('/dashboard'), 2000); }
                catch (e: any) { setError(e.message || 'Failed to confirm booking'); }
                finally { setLoading(false); }
              }} disabled={loading}
                className="w-full border-2 border-green-500 text-green-700 hover:bg-green-50 font-semibold py-3 rounded-xl disabled:opacity-50 transition flex items-center justify-center gap-2">
                &#x1F4B5; {loading ? 'Confirming...' : 'Cash on Arrival'}
              </button>
              <p className="text-xs text-gray-400 text-center">Pay directly at the property. Host will confirm your booking.</p>
            </div>
          )}
        </div>

        {/* Right column — Booking summary (sticky) */}
        <div className="lg:col-span-2">
          <div className="border rounded-2xl p-5 sticky top-20 space-y-4">
            <h3 className="font-semibold">Your booking summary</h3>

            {/* Listing card */}
            <div className="flex gap-3">
              {listing.primaryPhotoUrl && (
                <img src={listing.primaryPhotoUrl.startsWith('http') ? listing.primaryPhotoUrl : `http://localhost:8080${listing.primaryPhotoUrl}`}
                  alt={listing.title} className="w-20 h-16 rounded-lg object-cover shrink-0" />
              )}
              <div>
                <p className="font-semibold text-sm">{listing.title}</p>
                <p className="text-xs text-gray-500">{listing.city}, {listing.state}</p>
                {listing.avgRating != null && listing.avgRating > 0 && (
                  <div className="flex items-center gap-1 mt-1">
                    <span className="bg-orange-500 text-white text-xs font-bold px-1.5 py-0.5 rounded">{listing.avgRating.toFixed(1)}</span>
                    {listing.reviewCount != null && <span className="text-xs text-gray-400">{listing.reviewCount} reviews</span>}
                  </div>
                )}
              </div>
            </div>

            {/* Dates — editable */}
            <div className="border-t pt-3">
              <div className="flex items-center justify-between mb-2">
                <p className="text-xs font-semibold text-gray-500 uppercase">Your stay</p>
                {!booking && (
                  <button type="button" onClick={() => setEditingDates(!editingDates)}
                    className="text-xs text-orange-600 hover:text-orange-700 font-semibold">
                    {editingDates ? 'Done' : 'Change'}
                  </button>
                )}
              </div>
              {editingDates ? (
                <div className="space-y-3">
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-xs text-gray-500 mb-1">Check-in</label>
                      <input type="date" className="w-full border rounded-lg px-2.5 py-2 text-sm outline-none focus:border-orange-500"
                        value={checkIn} min={today}
                        onChange={(e) => {
                          setCheckIn(e.target.value);
                          const minDays = isMonthly ? 30 : 1;
                          if (!checkOut || e.target.value >= checkOut) {
                            const d = new Date(e.target.value); d.setDate(d.getDate() + minDays);
                            setCheckOut(d.toISOString().split('T')[0]);
                          }
                        }} />
                    </div>
                    <div>
                      <label className="block text-xs text-gray-500 mb-1">Check-out</label>
                      <input type="date" className="w-full border rounded-lg px-2.5 py-2 text-sm outline-none focus:border-orange-500"
                        value={checkOut} min={checkIn || today}
                        onChange={(e) => setCheckOut(e.target.value)} />
                    </div>
                  </div>
                </div>
              ) : (
                <div className="space-y-1.5 text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-500">Check-in</span>
                    <span className="font-medium">{fmtDate(checkIn)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Check-out</span>
                    <span className="font-medium">{fmtDate(checkOut)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Duration</span>
                    <span className="font-medium">
                      {durationLabel}{isMonthly ? ` (${nights} nights)` : ''}
                    </span>
                  </div>
                </div>
              )}
            </div>

            {/* Hotel check-in/out times */}
            {isHotel && (listing.checkinTime || listing.checkoutTime) && (
              <div className="border-t pt-3">
                <p className="text-xs font-semibold text-gray-500 uppercase mb-2">Hotel Times</p>
                <div className="flex gap-4 text-sm">
                  {listing.checkinTime && (
                    <div className="flex-1 bg-green-50 rounded-lg p-2 text-center">
                      <p className="text-xs text-gray-500">Check-in</p>
                      <p className="font-semibold text-green-700">{listing.checkinTime}</p>
                    </div>
                  )}
                  {listing.checkoutTime && (
                    <div className="flex-1 bg-red-50 rounded-lg p-2 text-center">
                      <p className="text-xs text-gray-500">Check-out</p>
                      <p className="font-semibold text-red-700">{listing.checkoutTime}</p>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Rooms & Guests — editable */}
            <div className="border-t pt-3">
              <div className="flex items-center justify-between mb-2">
                <p className="text-xs font-semibold text-gray-500 uppercase">Rooms & Guests</p>
                {!booking && (
                  <button type="button" onClick={() => setEditingGuests(!editingGuests)}
                    className="text-xs text-orange-600 hover:text-orange-700 font-semibold">
                    {editingGuests ? 'Done' : 'Change'}
                  </button>
                )}
              </div>
              {editingGuests ? (
                <div className="space-y-3">
                  {(() => {
                    // Per-room capacity: room type > derived from listing > default 2
                    const lTotalRooms = listing.totalRooms ?? 1;
                    // Per-room max: use selected room type(s), fallback to listing
                    let perRoomMax: number;
                    let maxGuestTotal: number;
                    let maxRooms: number;

                    if (selectedRoomSelections.length > 0) {
                      // Multi-room: max guests = sum of each (roomType.maxGuests × count)
                      maxGuestTotal = selectedRoomSelections.reduce((sum, sel) => {
                        const rt = roomTypes.find(r => r.id === sel.id);
                        return sum + (rt?.maxGuests ?? 2) * sel.c;
                      }, 0);
                      // Per-room = min across selected types (for display label)
                      perRoomMax = Math.min(...selectedRoomSelections.map(sel => {
                        const rt = roomTypes.find(r => r.id === sel.id);
                        return rt?.maxGuests ?? 2;
                      }));
                      maxRooms = selectedRoomSelections.reduce((s, sel) => s + sel.c, 0);
                    } else if (selectedRoomType) {
                      perRoomMax = selectedRoomType.maxGuests;
                      maxGuestTotal = perRoomMax * rooms;
                      maxRooms = selectedRoomType.count ?? listing.totalRooms ?? 10;
                    } else {
                      perRoomMax = lTotalRooms > 1 && listing.maxGuests
                        ? Math.max(1, Math.floor(listing.maxGuests / lTotalRooms))
                        : Math.min(listing.maxGuests ?? 2, 4);
                      maxGuestTotal = perRoomMax * rooms;
                      maxRooms = listing.totalRooms ?? 10;
                    }

                    // Auto-adjust rooms when guests increase beyond capacity
                    const setAdultsAuto = (val: number) => {
                      setAdults(val);
                      const needed = Math.ceil((val + childrenCount) / perRoomMax);
                      if (needed > rooms && needed <= maxRooms) setRooms(needed);
                    };
                    const setChildrenAuto = (val: number) => {
                      setChildrenCount(val);
                      const needed = Math.ceil((adults + val) / perRoomMax);
                      if (needed > rooms && needed <= maxRooms) setRooms(needed);
                    };
                    // Auto-adjust guests when rooms change
                    const setRoomsAuto = (val: number) => {
                      setRooms(val);
                      const maxTotal = val * perRoomMax;
                      // Rooms increased: ensure at least 1 adult per room
                      if (adults < val) setAdults(val);
                      // Rooms decreased: cap guests to new capacity
                      const totalNow = adults + childrenCount;
                      if (totalNow > maxTotal) {
                        const excessChildren = Math.max(0, childrenCount - Math.max(0, maxTotal - adults));
                        const newChildren = childrenCount - excessChildren;
                        const newAdults = Math.min(adults, maxTotal - newChildren);
                        setAdults(Math.max(1, newAdults));
                        setChildrenCount(newChildren);
                      }
                    };

                    const guestRows = [
                      { key: 'rooms', label: 'Rooms', sub: `${perRoomMax} guests per room`, value: rooms,
                        set: setRoomsAuto, min: 1, max: maxRooms },
                      { key: 'adults', label: 'Adults', sub: 'Age 13+', value: adults,
                        set: setAdultsAuto, min: 1, max: maxGuestTotal - childrenCount },
                      { key: 'children', label: 'Children', sub: 'Age 2-12', value: childrenCount,
                        set: setChildrenAuto, min: 0, max: maxGuestTotal - adults },
                      { key: 'infants', label: 'Infants', sub: 'Under 2', value: infants,
                        set: setInfants, min: 0, max: 5 },
                    ];
                    return guestRows.map(row => (
                      <div key={row.key} className="flex items-center justify-between">
                        <div>
                          <p className="text-sm font-medium text-gray-800">{row.label}</p>
                          <p className="text-xs text-gray-400">{row.sub}</p>
                        </div>
                        <div className="flex items-center gap-3">
                          <button type="button"
                            onClick={() => row.set(Math.max(row.min, row.value - 1))}
                            disabled={row.value <= row.min}
                            className="w-8 h-8 rounded-full border border-gray-300 text-gray-600 flex items-center justify-center hover:border-gray-500 disabled:opacity-30 transition text-sm">
                            -
                          </button>
                          <span className="w-6 text-center text-sm font-semibold">{row.value}</span>
                          <button type="button"
                            onClick={() => row.set(Math.min(row.max, row.value + 1))}
                            disabled={row.value >= row.max}
                            className="w-8 h-8 rounded-full border border-gray-300 text-gray-600 flex items-center justify-center hover:border-gray-500 disabled:opacity-30 transition text-sm">
                            +
                          </button>
                        </div>
                      </div>
                    ));
                  })()}
                  {/* Pets */}
                  {listing.petFriendly && (
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-medium text-gray-800">Pets</p>
                        <p className="text-xs text-gray-400">Service animals welcome</p>
                      </div>
                      <div className="flex items-center gap-3">
                        <button type="button" onClick={() => setPets(Math.max(0, pets - 1))} disabled={pets <= 0}
                          className="w-8 h-8 rounded-full border border-gray-300 text-gray-600 flex items-center justify-center hover:border-gray-500 disabled:opacity-30 transition text-sm">-</button>
                        <span className="w-6 text-center text-sm font-semibold">{pets}</span>
                        <button type="button" onClick={() => setPets(Math.min(listing.maxPets ?? 3, pets + 1))}
                          disabled={pets >= (listing.maxPets ?? 3)}
                          className="w-8 h-8 rounded-full border border-gray-300 text-gray-600 flex items-center justify-center hover:border-gray-500 disabled:opacity-30 transition text-sm">+</button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="space-y-1.5 text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-500">Rooms</span>
                    <span className="font-medium">{rooms}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Guests</span>
                    <span className="font-medium">{guestLabel}</span>
                  </div>
                </div>
              )}
            </div>

            {/* Price breakdown */}
            <div className="border-t pt-3 space-y-2 text-sm">
              <p className="text-xs font-semibold text-gray-500 uppercase mb-2">Price Details</p>

              {booking ? (
                /* ── After booking: show backend-confirmed breakdown ── */
                <>
                  {/* Room selections */}
                  {booking.roomSelections && booking.roomSelections.length > 0 ? (
                    booking.roomSelections.map(rs => (
                      <div key={rs.id} className="flex justify-between text-gray-600">
                        <span>{rs.count}x {rs.roomTypeName} — {formatPaise(rs.pricePerUnitPaise)}/night</span>
                        <span>{formatPaise(rs.totalPaise)}</span>
                      </div>
                    ))
                  ) : (
                    <div className="flex justify-between text-gray-600">
                      <span>
                        {booking.roomTypeName ? `${booking.roomTypeName} — ` : ''}
                        {formatPaise(booking.baseAmountPaise)} base
                      </span>
                      <span>{formatPaise(booking.baseAmountPaise)}</span>
                    </div>
                  )}

                  {/* Insurance */}
                  {booking.insuranceAmountPaise > 0 && (
                    <div className="flex justify-between text-gray-600">
                      <span>Micro-insurance</span>
                      <span>{formatPaise(booking.insuranceAmountPaise)}</span>
                    </div>
                  )}

                  {/* Cleaning fee */}
                  {booking.cleaningFeePaise != null && booking.cleaningFeePaise > 0 && (
                    <div className="flex justify-between text-gray-600">
                      <span>Cleaning fee</span>
                      <span>{formatPaise(booking.cleaningFeePaise)}</span>
                    </div>
                  )}

                  {/* Inclusions / Add-ons */}
                  {booking.inclusionsTotalPaise != null && booking.inclusionsTotalPaise > 0 && (
                    <div className="flex justify-between text-gray-600">
                      <span>Add-ons</span>
                      <span>{formatPaise(booking.inclusionsTotalPaise)}</span>
                    </div>
                  )}

                  {/* GST */}
                  {booking.gstAmountPaise > 0 && (
                    <div className="flex justify-between text-gray-400">
                      <span>GST (18%)</span>
                      <span>{formatPaise(booking.gstAmountPaise)}</span>
                    </div>
                  )}

                  {/* Total */}
                  <div className="flex justify-between font-bold text-base border-t pt-2">
                    <span>Total</span>
                    <span>{formatPaise(booking.totalAmountPaise)}</span>
                  </div>
                </>
              ) : (
                /* ── Before booking: show frontend estimate ── */
                <>
                  {hasMultiRoom ? (
                    selectedRoomSelections.map(sel => {
                      const rt = roomTypes.find(r => r.id === sel.id);
                      const price = rt?.basePricePaise ?? 0;
                      const lineTotal = price * sel.c * nights;
                      return (
                        <div key={sel.id} className="flex justify-between text-gray-600">
                          <span>{sel.c}x {rt?.name ?? 'Room'} — {formatPaise(price)} x {nights} night{nights > 1 ? 's' : ''}</span>
                          <span>{formatPaise(lineTotal)}</span>
                        </div>
                      );
                    })
                  ) : (
                    <div className="flex justify-between text-gray-600">
                      <span>
                        {isMonthly ? (
                          <>
                            {formatPaise(baseRate)} x {fullMonths} month{fullMonths !== 1 ? 's' : ''}
                            {remainingDays > 0 && <> + {remainingDays} day{remainingDays !== 1 ? 's' : ''}</>}
                            {rooms > 1 ? ` x ${rooms} rooms` : ''}
                          </>
                        ) : isHourly ? (
                          <>
                            {formatPaise(baseRate)} x {hours} hour{hours !== 1 ? 's' : ''}
                            {rooms > 1 ? ` x ${rooms} rooms` : ''}
                          </>
                        ) : (
                          <>
                            {formatPaise(baseRate)} x {nights} night{nights > 1 ? 's' : ''}
                            {rooms > 1 ? ` x ${rooms} room${rooms > 1 ? 's' : ''}` : ''}
                          </>
                        )}
                      </span>
                      <span>{formatPaise(roomSubtotalPaise)}</span>
                    </div>
                  )}
                  {hasMultiRoom && (
                    <div className="flex justify-between text-gray-700 font-medium border-t border-dashed pt-1">
                      <span>Room subtotal</span>
                      <span>{formatPaise(roomSubtotalPaise)}</span>
                    </div>
                  )}
                  {securityDepositPaise > 0 && (
                    <div className="flex justify-between text-gray-600">
                      <span>Security deposit</span>
                      <span>{formatPaise(securityDepositPaise)}</span>
                    </div>
                  )}
                  {cleaningFeePaise > 0 && (
                    <div className="flex justify-between text-gray-600">
                      <span>Cleaning fee</span>
                      <span>{formatPaise(cleaningFeePaise)}</span>
                    </div>
                  )}
                  {gstPaise > 0 && (
                    <div className="flex justify-between text-gray-400">
                      <span>GST (18%)</span>
                      <span>{formatPaise(gstPaise)}</span>
                    </div>
                  )}
                  <div className="flex justify-between font-bold text-base border-t pt-2">
                    <span>Estimated Total</span>
                    <span>{formatPaise(totalPaise)}</span>
                  </div>
                  <p className="text-[11px] text-gray-400">* Insurance & taxes confirmed after booking</p>
                </>
              )}

              {isPG && listing?.noticePeriodDays != null && (
                <p className="text-xs text-orange-600 mt-1">
                  Notice period: {listing.noticePeriodDays} days
                </p>
              )}
              {!booking && rooms > 1 && !isPG && (
                <p className="text-xs text-gray-400 text-right">
                  {formatPaise(Math.round(totalPaise / nights))} avg per night for {rooms} rooms
                </p>
              )}
              {isPG ? (
                <p className="text-xs text-gray-400">Security deposit refunded on checkout after deductions</p>
              ) : (
                <p className="text-xs text-gray-400">Zero deposit · Micro-insurance included</p>
              )}
            </div>

            {/* Cancellation policy */}
            <div className="border-t pt-3">
              <p className="text-xs font-semibold text-gray-500 uppercase mb-1">Cancellation Policy</p>
              <p className="text-sm text-gray-600 capitalize">
                {listing.cancellationPolicy?.toLowerCase() ?? 'Moderate'} cancellation
              </p>
              {listing.freeCancellation && (
                <p className="text-xs text-green-600 mt-0.5">Free cancellation available</p>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
