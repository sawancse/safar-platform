'use client';

import { useState } from 'react';

interface Props {
  highlights: string; // comma-separated
  locationHighlight?: string;
  coupleFriendly?: boolean;
  earlyBirdDiscountPercent?: number;
  earlyBirdDaysBefore?: number;
  zeroPaymentBooking?: boolean;
  freeCancellation?: boolean;
  breakfastIncluded?: boolean;
}

const HIGHLIGHT_ICONS: Record<string, string> = {
  'buffet breakfast': '🍳',
  'breakfast': '🍳',
  'swimming pool': '🏊',
  'pool': '🏊',
  'spa': '🧖',
  'gym': '🏋️',
  'fitness': '🏋️',
  'metro': '🚇',
  'airport': '✈️',
  'wifi': '📶',
  'parking': '🅿️',
  'restaurant': '🍽️',
  'bar': '🍸',
  'rooftop': '🌆',
  'garden': '🌿',
  'beach': '🏖️',
  'mountain': '⛰️',
  'view': '🌅',
  'heritage': '🏛️',
  'temple': '🛕',
  'lake': '🏞️',
  'market': '🛍️',
  'mall': '🛒',
  'clean': '✨',
  'safe': '🔒',
  'family': '👨‍👩‍👧‍👦',
  'couple': '💑',
  'business': '💼',
  'conference': '📋',
  'pet': '🐾',
  'kids': '👶',
};

function getHighlightIcon(text: string): string {
  const lower = text.toLowerCase();
  for (const [key, icon] of Object.entries(HIGHLIGHT_ICONS)) {
    if (lower.includes(key)) return icon;
  }
  return '✦';
}

export default function PropertyHighlights({
  highlights, locationHighlight, coupleFriendly, earlyBirdDiscountPercent,
  earlyBirdDaysBefore, zeroPaymentBooking, freeCancellation, breakfastIncluded,
}: Props) {
  const [showModal, setShowModal] = useState(false);

  const items = highlights
    ? highlights.split(',').map(h => h.trim()).filter(Boolean)
    : [];

  // Build all highlight entries
  const allHighlights: { icon: string; text: string; color: string }[] = [];

  if (locationHighlight) {
    allHighlights.push({ icon: '📍', text: locationHighlight, color: 'text-blue-700 bg-blue-50' });
  }
  if (breakfastIncluded) {
    allHighlights.push({ icon: '🍳', text: 'Delicious buffet breakfast', color: 'text-green-700 bg-green-50' });
  }
  if (coupleFriendly) {
    allHighlights.push({ icon: '💑', text: 'Couple friendly', color: 'text-pink-700 bg-pink-50' });
  }
  if (zeroPaymentBooking) {
    allHighlights.push({ icon: '🆓', text: 'Book with ₹0 payment', color: 'text-emerald-700 bg-emerald-50' });
  }
  if (earlyBirdDiscountPercent && earlyBirdDiscountPercent > 0) {
    allHighlights.push({
      icon: '🐦',
      text: `Early bird deal — ${earlyBirdDiscountPercent}% off when booked ${earlyBirdDaysBefore ?? 30}+ days ahead`,
      color: 'text-amber-700 bg-amber-50',
    });
  }
  if (freeCancellation) {
    allHighlights.push({ icon: '✓', text: 'Free cancellation available', color: 'text-teal-700 bg-teal-50' });
  }
  items.forEach(item => {
    allHighlights.push({ icon: getHighlightIcon(item), text: item, color: 'text-gray-700 bg-gray-50' });
  });

  if (allHighlights.length === 0) return null;

  const visibleCount = 4;
  const visible = allHighlights.slice(0, visibleCount);
  const hasMore = allHighlights.length > visibleCount;

  return (
    <>
      <div className="border rounded-xl p-5">
        <h3 className="text-lg font-semibold text-gray-800 mb-3">Property Highlights</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
          {visible.map((h, i) => (
            <div key={i} className={`flex items-center gap-2.5 px-3 py-2.5 rounded-lg ${h.color}`}>
              <span className="text-lg shrink-0">{h.icon}</span>
              <span className="text-sm font-medium">{h.text}</span>
            </div>
          ))}
        </div>
        {hasMore && (
          <button
            onClick={() => setShowModal(true)}
            className="mt-3 text-sm font-semibold text-orange-600 hover:text-orange-700"
          >
            View all {allHighlights.length} highlights
          </button>
        )}
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={() => setShowModal(false)}>
          <div className="bg-white rounded-2xl shadow-xl max-w-lg w-full mx-4 max-h-[80vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="sticky top-0 bg-white border-b px-6 py-4 flex items-center justify-between">
              <h3 className="text-lg font-semibold">Property Highlights</h3>
              <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-gray-600 text-xl">&times;</button>
            </div>
            <div className="p-6 space-y-3">
              {allHighlights.map((h, i) => (
                <div key={i} className={`flex items-center gap-3 px-4 py-3 rounded-xl ${h.color}`}>
                  <span className="text-xl shrink-0">{h.icon}</span>
                  <span className="text-sm font-medium">{h.text}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
