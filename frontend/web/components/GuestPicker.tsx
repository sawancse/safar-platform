'use client';

import { useState, useRef, useEffect } from 'react';

interface GuestCounts {
  adults: number;
  children: number;
  infants: number;
  pets: number;
  childrenAges: number[];
}

interface Props {
  value: GuestCounts;
  onChange: (counts: GuestCounts) => void;
  maxGuests: number;
  petFriendly?: boolean;
  maxPets?: number;
  inline?: boolean;
}

function buildLabel(value: GuestCounts): string {
  const parts: string[] = [];

  parts.push(`${value.adults} adult${value.adults !== 1 ? 's' : ''}`);

  if (value.children > 0) {
    const ages = value.childrenAges ?? [];
    if (value.children === 1) {
      const ageStr = ages.length > 0 ? ` (age ${ages[0]})` : '';
      parts.push(`1 child${ageStr}`);
    } else {
      const ageStr = ages.length > 0 ? ` (ages ${ages.join(', ')})` : '';
      parts.push(`${value.children} children${ageStr}`);
    }
  }

  if (value.infants > 0) {
    parts.push(`${value.infants} infant${value.infants !== 1 ? 's' : ''}`);
  }

  if (value.pets > 0) {
    parts.push(`${value.pets} pet${value.pets !== 1 ? 's' : ''}`);
  }

  return parts.join(', ');
}

function Stepper({
  label,
  subtitle,
  count,
  min,
  max,
  onDecrement,
  onIncrement,
}: {
  label: string;
  subtitle: string;
  count: number;
  min: number;
  max: number;
  onDecrement: () => void;
  onIncrement: () => void;
}) {
  return (
    <div className="flex items-center justify-between">
      <div>
        <p className="text-sm font-medium text-gray-800">{label}</p>
        <p className="text-xs text-gray-400">{subtitle}</p>
      </div>
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onDecrement}
          disabled={count <= min}
          className="w-8 h-8 rounded-full border border-gray-300 text-gray-600 flex items-center justify-center hover:border-gray-500 disabled:opacity-30 disabled:cursor-not-allowed transition"
        >
          -
        </button>
        <span className="w-6 text-center text-sm font-medium">{count}</span>
        <button
          type="button"
          onClick={onIncrement}
          disabled={count >= max}
          className="w-8 h-8 rounded-full border border-gray-300 text-gray-600 flex items-center justify-center hover:border-gray-500 disabled:opacity-30 disabled:cursor-not-allowed transition"
        >
          +
        </button>
      </div>
    </div>
  );
}

function ChildAgeSelector({
  ages,
  onChangeAge,
}: {
  ages: number[];
  onChangeAge: (index: number, age: number) => void;
}) {
  if (ages.length === 0) return null;

  return (
    <div className="pl-4 border-l-2 border-gray-100 space-y-2">
      {ages.map((age, i) => (
        <div key={i} className="flex items-center justify-between">
          <label className="text-xs text-gray-500">Child {i + 1}</label>
          <select
            value={age}
            onChange={(e) => onChangeAge(i, Number(e.target.value))}
            className="text-sm border border-gray-200 rounded-lg px-2 py-1 bg-white text-gray-700 focus:outline-none focus:ring-1 focus:ring-orange-400 cursor-pointer"
          >
            {Array.from({ length: 18 }, (_, a) => (
              <option key={a} value={a}>
                {a === 0 ? 'Under 1' : a === 1 ? '1 year' : `${a} years`}
              </option>
            ))}
          </select>
        </div>
      ))}
    </div>
  );
}

export default function GuestPicker({
  value,
  onChange,
  maxGuests,
  petFriendly = false,
  maxPets = 0,
  inline = false,
}: Props) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const childrenAges = value.childrenAges ?? [];
  const totalGuests = value.adults + value.children;
  const totalLabel = buildLabel(value);

  function update(field: keyof GuestCounts, delta: number) {
    if (field === 'childrenAges') return;

    const currentVal = value[field] as number;
    const nextVal = Math.max(0, currentVal + delta);
    const next: GuestCounts = { ...value, [field]: nextVal };

    // Ensure childrenAges array exists
    next.childrenAges = [...(value.childrenAges ?? [])];

    // Enforce minimums
    if (next.adults < 1) next.adults = 1;

    // Enforce maxGuests (adults + children)
    if (next.adults + next.children > maxGuests) return;

    // Enforce maxPets
    if (next.pets > maxPets) return;

    // Sync childrenAges array with children count
    if (field === 'children') {
      if (delta > 0) {
        // Add a new age entry (default 0)
        next.childrenAges.push(0);
      } else if (delta < 0 && next.childrenAges.length > next.children) {
        // Remove the last age entry
        next.childrenAges = next.childrenAges.slice(0, next.children);
      }
    }

    onChange(next);
  }

  function handleChildAgeChange(index: number, age: number) {
    const newAges = [...childrenAges];
    newAges[index] = age;
    onChange({ ...value, childrenAges: newAges });
  }

  const rows: { key: keyof GuestCounts; label: string; subtitle: string; min: number; max: number }[] = [
    { key: 'adults', label: 'Adults', subtitle: 'Age 13+', min: 1, max: maxGuests - value.children },
    { key: 'children', label: 'Children', subtitle: 'Age 0-17', min: 0, max: maxGuests - value.adults },
    { key: 'infants', label: 'Infants', subtitle: 'Under 2', min: 0, max: 5 },
  ];

  if (petFriendly && maxPets > 0) {
    rows.push({ key: 'pets', label: 'Pets', subtitle: 'Service animals always welcome', min: 0, max: maxPets });
  }

  const content = (
    <div className="space-y-4">
      {rows.map((row) => (
        <div key={row.key}>
          <Stepper
            label={row.label}
            subtitle={row.subtitle}
            count={value[row.key] as number}
            min={row.min}
            max={row.max}
            onDecrement={() => update(row.key, -1)}
            onIncrement={() => update(row.key, 1)}
          />
          {row.key === 'children' && value.children > 0 && (
            <div className="mt-2">
              <ChildAgeSelector ages={childrenAges} onChangeAge={handleChildAgeChange} />
            </div>
          )}
        </div>
      ))}
      {totalGuests >= maxGuests && (
        <p className="text-xs text-orange-500">Maximum {maxGuests} guests allowed</p>
      )}
    </div>
  );

  // Inline mode — render steppers directly without dropdown
  if (inline) {
    return content;
  }

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="w-full text-left text-sm py-0.5 outline-none"
      >
        {totalLabel}
      </button>
      {open && (
        <div className="absolute top-full left-0 right-0 mt-2 bg-white border rounded-xl shadow-lg p-4 z-30">
          {content}
          <button
            type="button"
            onClick={() => setOpen(false)}
            className="w-full text-sm font-semibold text-orange-600 hover:text-orange-700 py-1 mt-4"
          >
            Done
          </button>
        </div>
      )}
    </div>
  );
}
