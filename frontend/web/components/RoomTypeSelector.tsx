'use client';

import { useState } from 'react';
import { formatPaise } from '@/lib/utils';
import type { RoomType, RoomTypeInclusion } from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export interface RoomSelection {
  roomTypeId: string;
  roomTypeName: string;
  count: number;
  pricePerUnitPaise: number;
  maxGuests: number;
}

interface Props {
  roomTypes: RoomType[];
  perNightLabel: string;
  listingId: string;
  onSelect?: (selections: RoomSelection[]) => void;
}

function resolveUrl(url: string): string {
  if (url.startsWith('http')) return url;
  return `${API_URL}${url}`;
}

export default function RoomTypeSelector({ roomTypes, perNightLabel, listingId, onSelect }: Props) {
  const [quantities, setQuantities] = useState<Record<string, number>>({});
  const [expandedAddons, setExpandedAddons] = useState<string | null>(null);
  const [photoModal, setPhotoModal] = useState<{ urls: string[]; index: number } | null>(null);

  function updateQuantity(rt: RoomType, delta: number) {
    const current = quantities[rt.id] || 0;
    const next = Math.max(0, Math.min(rt.count, current + delta));
    const newQty = { ...quantities, [rt.id]: next };
    setQuantities(newQty);

    // Emit selections to parent
    const selections: RoomSelection[] = Object.entries(newQty)
      .filter(([, count]) => count > 0)
      .map(([id, count]) => {
        const room = roomTypes.find(r => r.id === id)!;
        return { roomTypeId: id, roomTypeName: room.name, count, pricePerUnitPaise: room.basePricePaise, maxGuests: room.maxGuests };
      });
    onSelect?.(selections);
  }

  function openPhotos(rt: RoomType, e: React.MouseEvent) {
    e.stopPropagation();
    const urls: string[] = [];
    if (rt.primaryPhotoUrl) urls.push(resolveUrl(rt.primaryPhotoUrl));
    if (rt.photoUrls) urls.push(...rt.photoUrls.map(resolveUrl));
    if (urls.length > 0) setPhotoModal({ urls, index: 0 });
  }

  if (roomTypes.length === 0) return null;

  const totalRooms = Object.values(quantities).reduce((s, c) => s + c, 0);
  const totalPaise = Object.entries(quantities)
    .filter(([, c]) => c > 0)
    .reduce((sum, [id, count]) => {
      const rt = roomTypes.find(r => r.id === id);
      return sum + (rt ? rt.basePricePaise * count : 0);
    }, 0);

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-semibold">Choose Your Rooms</h2>
        {totalRooms > 0 && (
          <span className="text-sm font-medium text-orange-600">
            {totalRooms} room{totalRooms > 1 ? 's' : ''} · {formatPaise(totalPaise)}/{perNightLabel.replace('per ', '')}
          </span>
        )}
      </div>

      <div className="space-y-3">
        {roomTypes.map((rt) => {
          const qty = quantities[rt.id] || 0;
          const isSelected = qty > 0;
          const included = rt.inclusions?.filter(i => i.inclusionMode === 'INCLUDED' || i.inclusionMode === 'COMPLIMENTARY') || [];
          const addons = rt.inclusions?.filter(i => i.inclusionMode === 'PAID_ADDON') || [];
          const showAddons = expandedAddons === rt.id;

          return (
            <div key={rt.id}
              className={`bg-white border-2 rounded-xl p-4 transition ${
                isSelected ? 'border-orange-500 ring-1 ring-orange-200 shadow-md' : 'border-gray-200 hover:border-gray-300'
              }`}>
              <div className="flex gap-4">
                {/* Room photo */}
                {rt.primaryPhotoUrl && (
                  <div className="relative shrink-0 cursor-zoom-in group/photo"
                    onClick={(e) => openPhotos(rt, e)}>
                    <img src={resolveUrl(rt.primaryPhotoUrl)} alt={rt.name}
                      className="w-36 h-24 rounded-lg object-cover group-hover/photo:brightness-90 transition" />
                    {rt.photoUrls && rt.photoUrls.length > 0 && (
                      <span className="absolute bottom-1 right-1 bg-black/60 text-white text-[10px] px-1.5 py-0.5 rounded">+{rt.photoUrls.length}</span>
                    )}
                  </div>
                )}

                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="font-semibold text-gray-800 text-sm">{rt.name}</h3>
                      {rt.description && <p className="text-xs text-gray-500 mt-0.5 line-clamp-1">{rt.description}</p>}
                    </div>
                    <div className="text-right shrink-0 ml-3">
                      <p className="text-lg font-bold text-gray-900">{formatPaise(rt.basePricePaise)}</p>
                      <p className="text-[11px] text-gray-400">{perNightLabel}</p>
                    </div>
                  </div>

                  {/* Room specs */}
                  <div className="flex flex-wrap gap-1.5 mt-2">
                    {rt.bedType && (
                      <span className="text-[11px] bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded">
                        🛏 {rt.bedType}{rt.bedCount && rt.bedCount > 1 ? ` x${rt.bedCount}` : ''}
                      </span>
                    )}
                    <span className="text-[11px] bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded">👥 {rt.maxGuests} guests</span>
                    {rt.areaSqft && <span className="text-[11px] bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded">📐 {rt.areaSqft} sqft</span>}
                    <span className={`text-[11px] px-1.5 py-0.5 rounded ${rt.count <= 3 ? 'bg-red-50 text-red-600' : 'bg-blue-50 text-blue-600'}`}>
                      {rt.availableCount ?? rt.count} available
                    </span>
                  </div>

                  {/* Inclusions */}
                  {included.length > 0 && (
                    <div className="flex flex-wrap gap-1 mt-1.5">
                      {included.slice(0, 4).map(inc => (
                        <span key={inc.id} className="text-[11px] text-green-700 bg-green-50 px-1.5 py-0.5 rounded">✓ {inc.name}</span>
                      ))}
                    </div>
                  )}

                  {/* Add-ons expandable */}
                  {addons.length > 0 && (
                    <div className="mt-1.5">
                      <span onClick={(e) => { e.stopPropagation(); setExpandedAddons(showAddons ? null : rt.id); }}
                        className="text-[11px] text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded cursor-pointer hover:bg-blue-100 transition inline-flex items-center gap-1">
                        +{addons.length} add-on{addons.length > 1 ? 's' : ''}
                        <svg className={`w-3 h-3 transition-transform ${showAddons ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
                        </svg>
                      </span>
                      {showAddons && (
                        <div className="mt-1.5 space-y-1 bg-blue-50/50 rounded-lg p-2 border border-blue-100">
                          {addons.map(a => (
                            <div key={a.id} className="flex items-center justify-between text-[11px]">
                              <span className="font-medium text-gray-800">{a.name}</span>
                              <span className="text-blue-600 font-semibold">
                                {a.chargePaise > 0 ? `+${formatPaise(a.chargePaise)}/${a.chargeType.replace('PER_', '').toLowerCase()}` : 'Free'}
                              </span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  {/* Quantity stepper */}
                  <div className="flex items-center justify-between mt-3 pt-2 border-t border-gray-100">
                    <span className="text-xs text-gray-500">
                      {qty > 0 ? `${qty} room${qty > 1 ? 's' : ''} selected` : 'Select rooms'}
                    </span>
                    <div className="flex items-center gap-2">
                      <button type="button"
                        onClick={() => updateQuantity(rt, -1)}
                        disabled={qty <= 0}
                        className="w-8 h-8 rounded-full border border-gray-300 text-gray-600 flex items-center justify-center hover:border-orange-400 hover:text-orange-600 disabled:opacity-30 disabled:cursor-not-allowed transition text-sm font-bold">
                        −
                      </button>
                      <span className={`w-6 text-center text-sm font-bold ${qty > 0 ? 'text-orange-600' : 'text-gray-400'}`}>{qty}</span>
                      <button type="button"
                        onClick={() => updateQuantity(rt, 1)}
                        disabled={qty >= rt.count}
                        className="w-8 h-8 rounded-full border border-gray-300 text-gray-600 flex items-center justify-center hover:border-orange-400 hover:text-orange-600 disabled:opacity-30 disabled:cursor-not-allowed transition text-sm font-bold">
                        +
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Photo Lightbox */}
      {photoModal && (
        <div className="fixed inset-0 bg-black/90 z-50 flex items-center justify-center" onClick={() => setPhotoModal(null)}>
          <button onClick={() => setPhotoModal(null)} className="absolute top-4 right-4 text-white/80 hover:text-white p-2 z-10">
            <svg className="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
          {photoModal.urls.length > 1 && (
            <>
              <button onClick={(e) => { e.stopPropagation(); setPhotoModal(p => p ? { ...p, index: (p.index - 1 + p.urls.length) % p.urls.length } : null); }}
                className="absolute left-4 bg-white/20 hover:bg-white/30 text-white rounded-full w-10 h-10 flex items-center justify-center z-10">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
                </svg>
              </button>
              <button onClick={(e) => { e.stopPropagation(); setPhotoModal(p => p ? { ...p, index: (p.index + 1) % p.urls.length } : null); }}
                className="absolute right-4 bg-white/20 hover:bg-white/30 text-white rounded-full w-10 h-10 flex items-center justify-center z-10">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                </svg>
              </button>
            </>
          )}
          <img src={photoModal.urls[photoModal.index]} alt="Room photo"
            className="max-w-[90vw] max-h-[85vh] object-contain rounded-lg" onClick={e => e.stopPropagation()} />
          <div className="absolute bottom-4 text-white/70 text-sm">{photoModal.index + 1} / {photoModal.urls.length}</div>
        </div>
      )}
    </div>
  );
}
