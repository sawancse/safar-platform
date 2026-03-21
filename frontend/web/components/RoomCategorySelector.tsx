'use client';

import { useState } from 'react';
import { formatPaise } from '@/lib/utils';

interface RoomCategory {
  id: string;
  name: string;
  description: string;
  icon: string;
  multiplier: number;
  features: string[];
}

const ROOM_CATEGORIES: RoomCategory[] = [
  {
    id: 'classic',
    name: 'Classic',
    description: 'Comfortable essentials for a great stay',
    icon: '🛏️',
    multiplier: 1.0,
    features: ['AC', 'Wi-Fi', 'TV', 'Hot Water'],
  },
  {
    id: 'deluxe',
    name: 'Deluxe',
    description: 'Extra space and premium amenities',
    icon: '✨',
    multiplier: 1.4,
    features: ['AC', 'Wi-Fi', 'Smart TV', 'Hot Water', 'Mini Fridge', 'Work Desk'],
  },
  {
    id: 'suite',
    name: 'Suite',
    description: 'Luxury experience with separate living area',
    icon: '👑',
    multiplier: 2.0,
    features: ['AC', 'Wi-Fi', 'Smart TV', 'Hot Water', 'Mini Fridge', 'Work Desk', 'Living Room', 'Bathtub'],
  },
];

interface Props {
  basePricePaise: number;
  pricingUnit: string;
  onSelect?: (categoryId: string, pricePaise: number) => void;
}

export default function RoomCategorySelector({ basePricePaise, pricingUnit, onSelect }: Props) {
  const [selected, setSelected] = useState('classic');

  function handleSelect(cat: RoomCategory) {
    setSelected(cat.id);
    onSelect?.(cat.id, Math.round(basePricePaise * cat.multiplier));
  }

  const unit = pricingUnit === 'HOUR' ? 'hr' : 'night';

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Choose your room</h2>
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {ROOM_CATEGORIES.map((cat) => {
          const price = Math.round(basePricePaise * cat.multiplier);
          const isSelected = selected === cat.id;
          return (
            <button
              key={cat.id}
              onClick={() => handleSelect(cat)}
              className={`text-left p-4 rounded-xl border-2 transition ${
                isSelected
                  ? 'border-orange-500 bg-orange-50 shadow-sm'
                  : 'border-gray-200 hover:border-gray-300 bg-white'
              }`}
            >
              <div className="flex items-center gap-2 mb-2">
                <span className="text-2xl">{cat.icon}</span>
                <div>
                  <p className="font-semibold text-sm">{cat.name}</p>
                  {isSelected && (
                    <span className="text-[10px] bg-orange-500 text-white px-1.5 py-0.5 rounded-full font-medium">
                      Selected
                    </span>
                  )}
                </div>
              </div>
              <p className="text-xs text-gray-500 mb-3">{cat.description}</p>
              <div className="flex flex-wrap gap-1 mb-3">
                {cat.features.map((f) => (
                  <span key={f} className="text-[10px] bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded">
                    {f}
                  </span>
                ))}
              </div>
              <p className="font-bold text-sm">
                {formatPaise(price)}
                <span className="text-gray-400 font-normal text-xs"> / {unit}</span>
              </p>
            </button>
          );
        })}
      </div>
    </div>
  );
}
