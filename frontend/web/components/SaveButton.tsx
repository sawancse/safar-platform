'use client';

import { useState, useEffect } from 'react';
import { api } from '@/lib/api';

export default function SaveButton({ listingId }: { listingId: string }) {
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('access_token');
    if (!token) return;
    api.getBucketList(token)
      .then((data: any) => {
        const items = Array.isArray(data) ? data : data?.content ?? [];
        if (items.some((i: any) => i.listingId === listingId)) setSaved(true);
      })
      .catch(() => {});
  }, [listingId]);

  async function toggle() {
    const token = localStorage.getItem('access_token');
    if (!token) {
      window.location.href = `/auth?redirect=/listings/${listingId}`;
      return;
    }
    setLoading(true);
    try {
      if (saved) {
        await api.removeFromBucketList(listingId, token);
        setSaved(false);
      } else {
        await api.addToBucketList(listingId, token);
        setSaved(true);
      }
    } catch {}
    setLoading(false);
  }

  return (
    <button
      onClick={toggle}
      disabled={loading}
      className="text-2xl hover:scale-110 transition-transform disabled:opacity-50"
      title={saved ? 'Remove from saved' : 'Save to bucket list'}
    >
      {saved ? '\u2764\uFE0F' : '\u2661'}
    </button>
  );
}
