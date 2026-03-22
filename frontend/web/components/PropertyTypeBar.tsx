'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';

const PROPERTY_TYPES = [
  { key: 'ALL',        label: 'All',           icon: '🏠' },
  { key: 'HOME',       label: 'Homes',         icon: '🏡' },
  { key: 'ROOM',       label: 'Rooms',         icon: '🛏️' },
  { key: 'VILLA',      label: 'Villas',        icon: '🏰' },
  { key: 'HOTEL',      label: 'Hotels',        icon: '🏨' },
  { key: 'RESORT',     label: 'Resorts',       icon: '🌴' },
  { key: 'HOMESTAY',   label: 'Homestays',     icon: '🏘️' },
  { key: 'PG',         label: 'PG',            icon: '🛌' },
  { key: 'COLIVING',   label: 'Co-living',     icon: '👥' },
  { key: 'FARMSTAY',   label: 'Farm Stays',    icon: '🌾' },
  { key: 'HOSTEL',     label: 'Hostels',       icon: '🎒' },
  { key: 'UNIQUE',     label: 'Unique Stays',  icon: '✨' },
  { key: 'COMMERCIAL', label: 'Commercial',    icon: '🏢' },
];

export default function PropertyTypeBar() {
  const router = useRouter();
  const [active, setActive] = useState('ALL');

  function handleSelect(key: string) {
    setActive(key);
    if (key === 'ALL') {
      router.push('/search');
    } else {
      router.push(`/search?type=${key}`);
    }
  }

  return (
    <div className="sticky top-0 z-40 bg-white border-b border-gray-100 shadow-sm">
      <div className="max-w-[1400px] mx-auto px-4">
        <div className="flex items-center gap-1 overflow-x-auto scrollbar-hide py-3 -mx-1">
          {PROPERTY_TYPES.map(({ key, label, icon }) => (
            <button
              key={key}
              onClick={() => handleSelect(key)}
              className={`flex flex-col items-center gap-1 px-4 py-2 rounded-xl whitespace-nowrap transition-all shrink-0
                ${active === key
                  ? 'bg-orange-50 border-b-2 border-orange-500 text-orange-600'
                  : 'text-gray-500 hover:text-gray-800 hover:bg-gray-50 border-b-2 border-transparent'
                }`}
            >
              <span className="text-xl leading-none">{icon}</span>
              <span className="text-xs font-medium">{label}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
