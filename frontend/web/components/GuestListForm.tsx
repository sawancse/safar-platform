'use client';

import { useState } from 'react';

export interface GuestInfo {
  fullName: string;
  email: string;
  phone: string;
  age: string;
  idType: string;
  idNumber: string;
  roomAssignment: string;
  isPrimary: boolean;
}

interface Props {
  roomNames: string[];
  onUpdate: (guests: GuestInfo[]) => void;
  primaryName?: string;
  primaryEmail?: string;
  primaryPhone?: string;
  totalGuests?: number;
}

const EMPTY_GUEST: GuestInfo = {
  fullName: '', email: '', phone: '', age: '',
  idType: '', idNumber: '', roomAssignment: '', isPrimary: false,
};

const ID_TYPES = [
  { value: '', label: 'Select ID type' },
  { value: 'AADHAAR', label: 'Aadhaar Card' },
  { value: 'PASSPORT', label: 'Passport' },
  { value: 'DRIVING_LICENSE', label: 'Driving License' },
  { value: 'VOTER_ID', label: 'Voter ID' },
  { value: 'PAN', label: 'PAN Card' },
];

export default function GuestListForm({ roomNames, onUpdate, primaryName, primaryEmail, primaryPhone, totalGuests }: Props) {
  const [guests, setGuests] = useState<GuestInfo[]>([]);
  const [expanded, setExpanded] = useState(false);

  // Pre-fill primary guest data when user clicks "Add Guest" (not auto-add)

  function addGuest() {
    const isFirst = guests.length === 0;
    const newGuest: GuestInfo = isFirst && primaryName
      ? { ...EMPTY_GUEST, fullName: primaryName, email: primaryEmail || '', phone: primaryPhone || '', isPrimary: true, roomAssignment: roomNames.length > 0 ? roomNames[0] : '' }
      : { ...EMPTY_GUEST, isPrimary: false };
    const newGuests = [...guests, newGuest];
    setGuests(newGuests);
    onUpdate(newGuests);
    setExpanded(true);
  }

  function addAllGuests() {
    const count = totalGuests || 2;
    const newGuests: GuestInfo[] = [];
    for (let i = 0; i < count; i++) {
      if (i === 0 && primaryName) {
        newGuests.push({
          ...EMPTY_GUEST,
          fullName: primaryName,
          email: primaryEmail || '',
          phone: primaryPhone || '',
          isPrimary: true,
          roomAssignment: roomNames.length > 0 ? roomNames[0] : '',
        });
      } else {
        newGuests.push({
          ...EMPTY_GUEST,
          roomAssignment: roomNames.length > 0 ? roomNames[Math.min(i, roomNames.length - 1)] : '',
        });
      }
    }
    setGuests(newGuests);
    onUpdate(newGuests);
    setExpanded(true);
  }

  function removeGuest(index: number) {
    const newGuests = guests.filter((_, i) => i !== index);
    if (newGuests.length > 0 && !newGuests.some(g => g.isPrimary)) {
      newGuests[0].isPrimary = true;
    }
    setGuests(newGuests);
    onUpdate(newGuests);
  }

  function updateGuest(index: number, field: keyof GuestInfo, value: string | boolean) {
    const newGuests = [...guests];
    (newGuests[index] as any)[field] = value;
    setGuests(newGuests);
    onUpdate(newGuests);
  }

  return (
    <div className="border rounded-2xl p-5 space-y-3">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="font-semibold text-sm">Guest Details</h3>
          <p className="text-xs text-gray-400">
            {guests.length > 0
              ? `${guests.length} guest${guests.length > 1 ? 's' : ''} added`
              : 'Add names and details for all guests'}
          </p>
        </div>
        {guests.length > 0 && (
          <button type="button" onClick={() => setExpanded(!expanded)}
            className="text-xs text-orange-600 font-semibold hover:text-orange-700">
            {expanded ? 'Collapse' : 'Edit'}
          </button>
        )}
      </div>

      {/* Quick actions */}
      {guests.length === 0 && (
        <div className="flex gap-2">
          <button type="button" onClick={addGuest}
            className="flex-1 text-sm font-medium text-orange-600 border border-orange-200 rounded-lg py-2 hover:bg-orange-50 transition">
            + Add Guest
          </button>
          {(totalGuests ?? 0) > 1 && (
            <button type="button" onClick={addAllGuests}
              className="flex-1 text-sm font-medium text-teal-600 border border-teal-200 rounded-lg py-2 hover:bg-teal-50 transition">
              Add All {totalGuests} Guests
            </button>
          )}
        </div>
      )}

      {/* Guest entries */}
      {expanded && guests.length > 0 && (
        <div className="space-y-3">
          {guests.map((guest, i) => (
            <div key={i} className={`rounded-xl p-3 space-y-2 ${guest.isPrimary ? 'bg-orange-50 border border-orange-200' : 'bg-gray-50 border border-gray-200'}`}>
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-gray-600">
                  Guest {i + 1} {guest.isPrimary && <span className="text-orange-500">(Primary)</span>}
                </span>
                {!guest.isPrimary && (
                  <button type="button" onClick={() => removeGuest(i)}
                    className="text-xs text-red-500 hover:text-red-600">Remove</button>
                )}
              </div>

              <div className="grid grid-cols-2 gap-2">
                <input type="text" placeholder="Full name *" value={guest.fullName}
                  onChange={e => updateGuest(i, 'fullName', e.target.value)}
                  className="col-span-2 border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400" />
                <input type="email" placeholder="Email" value={guest.email}
                  onChange={e => updateGuest(i, 'email', e.target.value)}
                  className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400" />
                <input type="tel" placeholder="Phone" value={guest.phone}
                  onChange={e => updateGuest(i, 'phone', e.target.value)}
                  className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400" />
                <input type="number" placeholder="Age" min="0" max="120" value={guest.age}
                  onChange={e => updateGuest(i, 'age', e.target.value)}
                  className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400" />
                {roomNames.length > 0 && (
                  <select value={guest.roomAssignment}
                    onChange={e => updateGuest(i, 'roomAssignment', e.target.value)}
                    className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400 bg-white">
                    <option value="">Assign room</option>
                    {roomNames.map(name => (
                      <option key={name} value={name}>{name}</option>
                    ))}
                  </select>
                )}
              </div>

              <details className="text-xs">
                <summary className="text-gray-400 cursor-pointer hover:text-gray-600">ID verification (optional)</summary>
                <div className="grid grid-cols-2 gap-2 mt-2">
                  <select value={guest.idType}
                    onChange={e => updateGuest(i, 'idType', e.target.value)}
                    className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400 bg-white">
                    {ID_TYPES.map(t => (
                      <option key={t.value} value={t.value}>{t.label}</option>
                    ))}
                  </select>
                  <input type="text" placeholder="ID number" value={guest.idNumber}
                    onChange={e => updateGuest(i, 'idNumber', e.target.value)}
                    className="border rounded-lg px-3 py-1.5 text-sm outline-none focus:border-orange-400" />
                </div>
              </details>
            </div>
          ))}

          <button type="button" onClick={addGuest}
            className="w-full text-sm font-medium text-orange-600 border border-dashed border-orange-300 rounded-lg py-2 hover:bg-orange-50 transition">
            + Add Another Guest
          </button>
        </div>
      )}

      {/* Collapsed summary */}
      {!expanded && guests.length > 0 && (
        <div className="space-y-1">
          {guests.map((g, i) => (
            <div key={i} className="flex items-center justify-between text-sm">
              <span className="text-gray-700">
                {g.isPrimary && <span className="text-orange-500 mr-1">●</span>}
                {g.fullName || `Guest ${i + 1}`}
              </span>
              {g.roomAssignment && <span className="text-xs text-gray-400">{g.roomAssignment}</span>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
