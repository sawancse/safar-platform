'use client';

import { useEffect } from 'react';

export default function TrackView({ listingId }: { listingId: string }) {
  useEffect(() => {
    try {
      const stored: string[] = JSON.parse(localStorage.getItem('recently_viewed') || '[]');
      const filtered = stored.filter((id) => id !== listingId);
      filtered.unshift(listingId);
      localStorage.setItem('recently_viewed', JSON.stringify(filtered.slice(0, 10)));
    } catch {}
  }, [listingId]);

  return null;
}
