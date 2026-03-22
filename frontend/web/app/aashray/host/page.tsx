'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import type { Listing } from '@/types';

export default function AashrayHostPage() {
  const router = useRouter();
  const [token, setToken] = useState('');
  const [listings, setListings] = useState<Listing[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<string | null>(null);

  useEffect(() => {
    const t = localStorage.getItem('access_token') ?? '';
    if (!t) { router.push('/auth?redirect=/aashray/host'); return; }
    setToken(t);
    api.getMyListings(t)
      .then(setListings)
      .catch(() => setListings([]))
      .finally(() => setLoading(false));
  }, [router]);

  async function toggleAashray(listingId: string, currentValue: boolean) {
    setSaving(listingId);
    try {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
      const res = await fetch(`${apiUrl}/api/v1/listings/${listingId}/aashray`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({ aashrayReady: !currentValue }),
      });
      if (res.ok) {
        const updated = await res.json();
        setListings(prev => prev.map(l =>
          l.id === listingId ? { ...l, ...updated } : l
        ));
      }
    } catch {} finally { setSaving(null); }
  }

  if (loading) {
    return <div className="max-w-3xl mx-auto px-4 py-20 text-center text-gray-400"><p>Loading your listings...</p></div>;
  }

  const verifiedListings = listings.filter(l => l.status === 'VERIFIED');
  const aashrayCount = listings.filter(l => l.aashrayReady).length;

  return (
    <div className="max-w-3xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold mb-2">Become an Aashray Host</h1>
      <p className="text-gray-500 mb-8">
        Open your verified listings to displaced persons. NGOs will fund the stays — you receive guaranteed monthly payments.
      </p>

      {/* Benefits */}
      <div className="grid grid-cols-3 gap-4 mb-8">
        {[
          { icon: '💰', title: 'Guaranteed Income', desc: 'Monthly payments from NGOs' },
          { icon: '📜', title: '80G Tax Benefits', desc: 'Discounts qualify as donations' },
          { icon: '🛡️', title: 'Damage Protection', desc: 'NGO-funded guarantee coverage' },
        ].map(item => (
          <div key={item.title} className="border rounded-xl p-4 text-center">
            <span className="text-2xl">{item.icon}</span>
            <p className="text-sm font-semibold mt-2">{item.title}</p>
            <p className="text-xs text-gray-500 mt-1">{item.desc}</p>
          </div>
        ))}
      </div>

      {/* Status */}
      {aashrayCount > 0 && (
        <div className="bg-teal-50 border border-teal-200 rounded-2xl p-4 mb-6 flex items-center gap-3">
          <span className="text-2xl">🤝</span>
          <div>
            <p className="text-sm font-semibold text-teal-800">You're an Aashray Host!</p>
            <p className="text-xs text-teal-600">{aashrayCount} listing{aashrayCount > 1 ? 's' : ''} open for displaced persons</p>
          </div>
        </div>
      )}

      {/* Listings */}
      {verifiedListings.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <p className="text-5xl mb-4">🏠</p>
          <p className="text-lg font-medium">No verified listings</p>
          <p className="text-sm mt-1">You need at least one verified listing to become an Aashray Host</p>
          <button onClick={() => router.push('/host')}
            className="mt-4 bg-orange-500 hover:bg-orange-600 text-white font-semibold px-6 py-2 rounded-xl transition text-sm">
            Go to Host Dashboard
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          <p className="text-sm font-semibold text-gray-600 mb-2">
            Toggle "Aashray Ready" on your verified listings:
          </p>
          {verifiedListings.map(listing => {
            const isAashray = listing.aashrayReady;
            return (
              <div key={listing.id}
                className={`border-2 rounded-2xl p-5 transition ${
                  isAashray ? 'border-teal-400 bg-teal-50/50' : 'border-gray-200'
                }`}>
                <div className="flex items-center justify-between">
                  <div className="flex-1 min-w-0">
                    <p className="font-semibold text-sm">{listing.title}</p>
                    <p className="text-xs text-gray-500">{listing.city}, {listing.state}</p>
                    <p className="text-sm font-bold text-gray-800 mt-1">{formatPaise(listing.basePricePaise)} / night</p>
                  </div>
                  <div className="flex items-center gap-3">
                    {isAashray && (
                      <span className="text-xs font-semibold text-teal-700 bg-teal-100 px-3 py-1 rounded-full">
                        Aashray Ready
                      </span>
                    )}
                    <button
                      onClick={() => toggleAashray(listing.id, isAashray ?? false)}
                      disabled={saving === listing.id}
                      className={`relative w-12 h-7 rounded-full transition-colors ${
                        isAashray ? 'bg-teal-500' : 'bg-gray-300'
                      }`}>
                      <span className={`absolute top-0.5 w-6 h-6 rounded-full bg-white shadow transition-transform ${
                        isAashray ? 'translate-x-5' : 'translate-x-0.5'
                      }`} />
                    </button>
                  </div>
                </div>
                {isAashray && (
                  <div className="mt-3 pt-3 border-t border-teal-200 text-xs text-teal-700">
                    This listing is now visible to NGO case workers searching for Aashray housing.
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Info */}
      <div className="mt-8 bg-blue-50 border border-blue-200 rounded-2xl p-5">
        <h3 className="font-semibold text-blue-900 mb-2">How Aashray works for hosts</h3>
        <ul className="text-sm text-blue-800 space-y-2">
          <li>• NGO case workers search for Aashray-ready listings and book 30-360 day stays</li>
          <li>• You receive monthly payments directly from the NGO — guaranteed and on time</li>
          <li>• Safar charges 0% commission on Aashray stays</li>
          <li>• Damage protection is provided by the NGO's guarantee fund</li>
          <li>• You can disable Aashray anytime — existing bookings are honored</li>
        </ul>
      </div>
    </div>
  );
}
