'use client';

import { useState, useMemo } from 'react';
import { formatPaise } from '@/lib/utils';

interface Props {
  basePricePaise: number;
  tier: string; // STARTER, PRO, COMMERCIAL
  bookingType?: string; // MEDICAL, AASHRAY, etc.
  onPriceChange?: (pricePaise: number) => void;
  editable?: boolean;
}

const RATES: Record<string, { rate: number; label: string }> = {
  STARTER: { rate: 0.18, label: 'Starter (18%)' },
  PRO: { rate: 0.12, label: 'Pro (12%)' },
  COMMERCIAL: { rate: 0.10, label: 'Commercial (10%)' },
  MEDICAL: { rate: 0.08, label: 'Medical (8%)' },
  AASHRAY: { rate: 0, label: 'Aashray (0%)' },
};

export default function PricingCalculator({ basePricePaise, tier, bookingType, onPriceChange, editable = false }: Props) {
  const [price, setPrice] = useState(basePricePaise);

  const effectiveTier = bookingType === 'AASHRAY' ? 'AASHRAY' : bookingType === 'MEDICAL' ? 'MEDICAL' : (tier || 'STARTER');
  const rateInfo = RATES[effectiveTier] || RATES.STARTER;

  const breakdown = useMemo(() => {
    const commission = Math.floor(price * rateInfo.rate);
    const gstOnFee = Math.round(commission * 0.18); // 18% GST on platform fee
    const tds = Math.round(price * 0.01); // 1% TDS
    const netEarnings = price - commission - tds;

    // Pro upgrade savings
    const proCommission = Math.floor(price * 0.12);
    const proNet = price - proCommission - tds;
    const upgradeWouldSave = effectiveTier === 'STARTER' ? proNet - netEarnings : 0;

    return { commission, gstOnFee, tds, netEarnings, upgradeWouldSave };
  }, [price, rateInfo.rate, effectiveTier]);

  const handlePriceChange = (value: string) => {
    const paise = Math.round(parseFloat(value) * 100) || 0;
    setPrice(paise);
    onPriceChange?.(paise);
  };

  return (
    <div className="bg-gray-50 border rounded-xl p-5 space-y-3">
      <h4 className="font-semibold text-gray-900 text-sm">Earnings Calculator</h4>

      {/* Price input */}
      {editable ? (
        <div className="flex items-center gap-2">
          <span className="text-gray-500 text-sm">Price per night:</span>
          <div className="relative">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">Rs</span>
            <input
              type="number"
              value={(price / 100).toFixed(0)}
              onChange={(e) => handlePriceChange(e.target.value)}
              className="border rounded-lg pl-10 pr-3 py-2 text-sm w-32 font-semibold"
            />
          </div>
        </div>
      ) : (
        <div className="flex items-center justify-between text-sm">
          <span className="text-gray-500">Listing price</span>
          <span className="font-semibold">{formatPaise(price)}/night</span>
        </div>
      )}

      {/* Breakdown */}
      <div className="space-y-2 text-sm">
        <div className="flex justify-between text-gray-600">
          <span>Guest pays</span>
          <span>{formatPaise(price)}</span>
        </div>
        <div className="flex justify-between text-red-500">
          <span>Platform fee ({rateInfo.label})</span>
          <span>-{formatPaise(breakdown.commission)}</span>
        </div>
        <div className="flex justify-between text-gray-400">
          <span>TDS (1%)</span>
          <span>-{formatPaise(breakdown.tds)}</span>
        </div>
        <div className="flex justify-between font-bold text-green-700 border-t pt-2">
          <span>You earn</span>
          <span>{formatPaise(breakdown.netEarnings)}/night</span>
        </div>
      </div>

      {/* Upgrade CTA */}
      {breakdown.upgradeWouldSave > 0 && (
        <div className="bg-orange-50 border border-orange-200 rounded-lg p-3 mt-2">
          <p className="text-xs text-orange-700 font-medium">
            Upgrade to Pro (Rs 2,499/mo) &rarr; earn {formatPaise(breakdown.upgradeWouldSave)} more per night
          </p>
          <a href="/host" className="text-xs text-orange-600 font-semibold hover:underline">
            Upgrade now &rarr;
          </a>
        </div>
      )}
    </div>
  );
}
