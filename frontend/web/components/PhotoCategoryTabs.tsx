'use client';

import { useMemo } from 'react';
import type { MediaItem } from '@/types';

const CATEGORY_ORDER = ['All', 'Bedroom', 'Bathroom', 'Kitchen', 'Living Room', 'Exterior', 'View', 'Amenities', 'Video Tour'];

interface Props {
  media: MediaItem[];
  activeCategory: string;
  onCategoryChange: (category: string) => void;
}

export default function PhotoCategoryTabs({ media, activeCategory, onCategoryChange }: Props) {
  const categoryCounts = useMemo(() => {
    const counts: Record<string, number> = { All: media.length };
    media.forEach(m => {
      const cat = m.category || 'Uncategorized';
      counts[cat] = (counts[cat] || 0) + 1;
    });
    return counts;
  }, [media]);

  const visibleCategories = CATEGORY_ORDER.filter(c => c === 'All' || (categoryCounts[c] && categoryCounts[c] > 0));

  return (
    <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-hide">
      {visibleCategories.map(cat => (
        <button
          key={cat}
          onClick={() => onCategoryChange(cat)}
          className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
            activeCategory === cat
              ? 'bg-gray-900 text-white'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          }`}
        >
          {cat} ({categoryCounts[cat] || 0})
        </button>
      ))}
    </div>
  );
}
