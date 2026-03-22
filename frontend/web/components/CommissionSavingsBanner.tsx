'use client';

import { useMemo } from 'react';
import { formatPaise } from '@/lib/utils';
import type { Booking } from '@/types';

interface Props {
  currentTier: string;
  bookings: Booking[];
  onUpgrade?: () => void;
}

export default function CommissionSavingsBanner({ currentTier, bookings, onUpgrade }: Props) {
  const savings = useMemo(() => {
    if (currentTier !== 'STARTER' && currentTier !== 'FREE') return null;

    const confirmed = bookings.filter(b =>
      ['CONFIRMED', 'CHECKED_IN', 'COMPLETED'].includes(b.status)
    );

    // Calculate what they paid in commission at 18%
    const totalRevenue = confirmed.reduce((s, b) => s + b.totalAmountPaise, 0);
    const currentCommission = Math.floor(totalRevenue * 0.18);
    const proCommission = Math.floor(totalRevenue * 0.12);
    const monthlySavings = currentCommission - proCommission;

    if (monthlySavings <= 0) return null;

    return {
      monthlySavings,
      totalRevenue,
      currentCommission,
      proCommission,
      subscriptionCost: 249900, // Rs 2,499
      netBenefit: monthlySavings - 249900,
    };
  }, [currentTier, bookings]);

  if (!savings || savings.netBenefit <= 0) return null;

  return (
    <div className="bg-gradient-to-r from-orange-50 to-amber-50 border border-orange-200 rounded-xl p-5 mb-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="font-bold text-gray-900">
            Save {formatPaise(savings.monthlySavings)}/month with Pro
          </p>
          <p className="text-sm text-gray-600 mt-1">
            Based on your recent bookings, upgrading from Starter (18%) to Pro (12%)
            would save you {formatPaise(savings.monthlySavings)} in commission.
            After the Rs 2,499 subscription, you still save {formatPaise(savings.netBenefit)}.
          </p>
        </div>
        <button
          onClick={onUpgrade}
          className="bg-orange-500 hover:bg-orange-600 text-white font-semibold px-5 py-2.5 rounded-lg transition whitespace-nowrap ml-4"
        >
          Upgrade to Pro
        </button>
      </div>
    </div>
  );
}
