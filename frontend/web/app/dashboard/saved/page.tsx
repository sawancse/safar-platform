'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { api } from '@/lib/api';
import type { BucketListItem } from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

function resolveUrl(url?: string): string | undefined {
  if (!url) return undefined;
  if (url.startsWith('http')) return url;
  return `${API_URL}${url}`;
}

export default function SavedPage() {
  const router = useRouter();
  const [items, setItems] = useState<BucketListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [token, setToken] = useState('');

  useEffect(() => {
    const t = localStorage.getItem('access_token') ?? '';
    if (!t) {
      router.push('/auth?redirect=/dashboard/saved');
      return;
    }
    setToken(t);
    api.getBucketList(t)
      .then((data: any) => setItems(Array.isArray(data) ? data : data?.content ?? []))
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
  }, [router]);

  async function handleRemove(listingId: string) {
    try {
      await api.removeFromBucketList(listingId, token);
      setItems((prev) => prev.filter((i) => i.listingId !== listingId));
    } catch {}
  }

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center text-gray-400">
        <div className="text-4xl mb-4 animate-spin">&#x23F3;</div>
        <p>Loading saved properties...</p>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold mb-2">Saved Properties</h1>
      <p className="text-sm text-gray-500 mb-6">Your bucket list of favourite stays</p>

      {items.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <p className="text-4xl mb-3">&#x2764;</p>
          <p className="text-sm font-medium">No saved properties yet</p>
          <p className="text-xs text-gray-400 mt-1">Save properties you love to plan your next trip</p>
          <Link
            href="/search"
            className="inline-block mt-4 bg-orange-500 text-white px-6 py-2 rounded-xl font-semibold hover:bg-orange-600 text-sm"
          >
            Explore stays
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {items.map((item) => (
            <div key={item.id} className="border rounded-2xl p-4 flex gap-4 items-center">
              {item.listingImageUrl ? (
                <img
                  src={resolveUrl(item.listingImageUrl)}
                  alt={item.listingTitle}
                  className="w-20 h-16 rounded-lg object-cover shrink-0"
                />
              ) : (
                <div className="w-20 h-16 rounded-lg bg-gray-100 flex items-center justify-center text-2xl shrink-0">
                  &#x1F3E0;
                </div>
              )}
              <div className="flex-1 min-w-0">
                <Link href={`/listings/${item.listingId}`} className="font-semibold text-sm hover:text-orange-500 truncate block">
                  {item.listingTitle}
                </Link>
                <p className="text-xs text-gray-500">{item.listingCity}</p>
                <p className="text-xs text-gray-400 mt-1">
                  Saved {new Date(item.addedAt).toLocaleDateString('en-IN', { month: 'short', day: 'numeric' })}
                </p>
              </div>
              <button
                onClick={() => handleRemove(item.listingId)}
                className="text-sm text-red-400 hover:text-red-600 shrink-0"
              >
                Remove
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
