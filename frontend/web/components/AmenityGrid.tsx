'use client';

import { useState, useMemo } from 'react';

const AMENITY_ICONS: Record<string, string> = {
  ac: '❄️', air_conditioning: '❄️',
  wifi: '📶', free_wifi: '📶',
  tv: '📺', television: '📺', smart_tv: '📺',
  parking: '🅿️', free_parking: '🅿️',
  pool: '🏊', swimming_pool: '🏊',
  gym: '🏋️', fitness_center: '🏋️',
  kitchen: '🍳', kitchenette: '🍳',
  washer: '🧺', washing_machine: '🧺', laundry: '🧺',
  hot_water: '🚿', geyser: '🚿',
  balcony: '🌇', terrace: '🌇',
  garden: '🌿',
  elevator: '🛗', lift: '🛗',
  security: '🔒', cctv: '📹',
  power_backup: '🔋', inverter: '🔋',
  pet_friendly: '🐾', pets_allowed: '🐾',
  breakfast: '🍳', meals: '🍽️',
  room_service: '🛎️',
  iron: '👔', ironing: '👔',
  hair_dryer: '💨',
  first_aid: '🏥', first_aid_kit: '🏥',
  fire_extinguisher: '🧯',
  smoke_detector: '🔔',
  heater: '🔥', heating: '🔥',
  refrigerator: '🧊', fridge: '🧊',
  microwave: '📡',
  workspace: '💻', desk: '💻', work_desk: '💻',
  coffee: '☕', tea: '☕', coffee_maker: '☕',
  toiletries: '🧴',
  towels: '🛁',
  bedding: '🛏️', linen: '🛏️',
  wardrobe: '👗', closet: '👗',
  safe: '🔐', locker: '🔐',
  intercom: '📞',
  water_purifier: '💧', ro_water: '💧',
  mosquito_net: '🦟',
  fan: '🌀', ceiling_fan: '🌀',
  spa: '💆', yoga: '🧘', sauna: '🧖',
  bbq: '🍖', bonfire: '🔥',
  housekeeping: '🧹',
};

interface CategoryDef {
  label: string;
  keys: string[];
}

const CATEGORIES: CategoryDef[] = [
  {
    label: 'Basic Facilities',
    keys: ['wifi', 'free_wifi', 'ac', 'air_conditioning', 'tv', 'television', 'smart_tv', 'hot_water', 'geyser', 'power_backup', 'inverter', 'fan', 'ceiling_fan', 'heater', 'heating', 'elevator', 'lift'],
  },
  {
    label: 'General Services',
    keys: ['room_service', 'laundry', 'washer', 'washing_machine', 'housekeeping', 'iron', 'ironing', 'parking', 'free_parking', 'cctv', 'security', 'intercom'],
  },
  {
    label: 'Health & Wellness',
    keys: ['pool', 'swimming_pool', 'gym', 'fitness_center', 'spa', 'yoga', 'sauna'],
  },
  {
    label: 'Kitchen & Dining',
    keys: ['kitchen', 'kitchenette', 'refrigerator', 'fridge', 'microwave', 'coffee', 'tea', 'coffee_maker', 'water_purifier', 'ro_water', 'breakfast', 'meals'],
  },
  {
    label: 'Room Amenities',
    keys: ['balcony', 'workspace', 'desk', 'work_desk', 'safe', 'locker', 'wardrobe', 'closet', 'toiletries', 'towels', 'bedding', 'linen', 'hair_dryer'],
  },
  {
    label: 'Safety',
    keys: ['fire_extinguisher', 'smoke_detector', 'first_aid', 'first_aid_kit'],
  },
  {
    label: 'Outdoor',
    keys: ['garden', 'terrace', 'bbq', 'bonfire'],
  },
];

const INITIAL_VISIBLE = 5;

function normalize(amenity: string): string {
  return amenity.toLowerCase().replace(/\s+/g, '_');
}

function getIcon(amenity: string): string {
  return AMENITY_ICONS[normalize(amenity)] || '✦';
}

function formatName(amenity: string): string {
  return amenity.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
}

interface CategorizedGroup {
  label: string;
  items: string[];
}

function categorize(amenities: string[]): CategorizedGroup[] {
  const used = new Set<string>();
  const groups: CategorizedGroup[] = [];

  for (const cat of CATEGORIES) {
    const keySet = new Set(cat.keys);
    const matched = amenities.filter((a) => {
      const key = normalize(a);
      return keySet.has(key) && !used.has(key);
    });
    matched.forEach((a) => used.add(normalize(a)));
    if (matched.length > 0) {
      groups.push({ label: cat.label, items: matched });
    }
  }

  // Uncategorized leftovers
  const remaining = amenities.filter((a) => !used.has(normalize(a)));
  if (remaining.length > 0) {
    groups.push({ label: 'Other', items: remaining });
  }

  return groups;
}

function AmenityItem({ amenity }: { amenity: string }) {
  return (
    <div className="flex items-center gap-3 p-3 rounded-xl border border-gray-100 bg-gray-50/50">
      <span className="text-xl">{getIcon(amenity)}</span>
      <span className="text-sm text-gray-700">{formatName(amenity)}</span>
    </div>
  );
}

function CategoryCard({ group }: { group: CategorizedGroup }) {
  const [expanded, setExpanded] = useState(false);
  const visible = expanded ? group.items : group.items.slice(0, INITIAL_VISIBLE);
  const hiddenCount = group.items.length - INITIAL_VISIBLE;

  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5">
      <h3 className="text-base font-semibold text-gray-900 mb-3">{group.label}</h3>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
        {visible.map((a) => (
          <AmenityItem key={a} amenity={a} />
        ))}
      </div>
      {hiddenCount > 0 && !expanded && (
        <button
          onClick={() => setExpanded(true)}
          className="mt-3 text-sm font-medium text-indigo-600 hover:text-indigo-800 transition-colors"
        >
          +{hiddenCount} more
        </button>
      )}
      {expanded && hiddenCount > 0 && (
        <button
          onClick={() => setExpanded(false)}
          className="mt-3 text-sm font-medium text-gray-500 hover:text-gray-700 transition-colors"
        >
          Show less
        </button>
      )}
    </div>
  );
}

function AmenityModal({
  groups,
  onClose,
}: {
  groups: CategorizedGroup[];
  onClose: () => void;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={onClose}
      />
      {/* Modal */}
      <div className="relative z-10 bg-white rounded-2xl shadow-2xl w-full max-w-3xl max-h-[85vh] overflow-hidden mx-4">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">All Amenities</h2>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-gray-100 transition-colors text-gray-500 hover:text-gray-800"
            aria-label="Close"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fillRule="evenodd"
                d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                clipRule="evenodd"
              />
            </svg>
          </button>
        </div>
        {/* Body */}
        <div className="p-6 overflow-y-auto max-h-[calc(85vh-72px)] space-y-6">
          {groups.map((group) => (
            <div key={group.label}>
              <h3 className="text-base font-semibold text-gray-900 mb-3">
                {group.label}
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {group.items.map((a) => (
                  <AmenityItem key={a} amenity={a} />
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

interface Props {
  amenities: string[];
}

export default function AmenityGrid({ amenities }: Props) {
  const [modalOpen, setModalOpen] = useState(false);

  const groups = useMemo(
    () => categorize(amenities || []),
    [amenities]
  );

  if (!amenities || amenities.length === 0) return null;

  // Show only first 4 amenities inline, rest in modal
  const previewItems = amenities.slice(0, 4);
  const moreCount = amenities.length - 4;

  return (
    <div>
      <h2 className="text-xl font-semibold mb-3">Amenities</h2>
      <div className="flex flex-wrap gap-2">
        {previewItems.map((a) => (
          <div key={a} className="flex items-center gap-2 px-3 py-2 rounded-lg border border-gray-200 bg-gray-50/50">
            <span className="text-lg">{getIcon(a)}</span>
            <span className="text-sm text-gray-700">{formatName(a)}</span>
          </div>
        ))}
        {moreCount > 0 && (
          <button
            onClick={() => setModalOpen(true)}
            className="flex items-center gap-1.5 px-4 py-2 rounded-lg border border-gray-300 text-sm font-medium text-gray-700 hover:bg-gray-100 transition"
          >
            +{moreCount} more
          </button>
        )}
      </div>
      {amenities.length <= 4 ? null : (
        <button
          onClick={() => setModalOpen(true)}
          className="mt-3 text-sm font-semibold text-orange-600 hover:text-orange-700"
        >
          View all {amenities.length} amenities
        </button>
      )}
      {modalOpen && (
        <AmenityModal groups={groups} onClose={() => setModalOpen(false)} />
      )}
    </div>
  );
}
