'use client';

import { useState } from 'react';
import Link from 'next/link';

interface Props {
  userName?: string;
  hasAvatar?: boolean;
  profileCompletion?: number;
}

export default function CompleteProfileBanner({ userName, hasAvatar, profileCompletion = 0 }: Props) {
  const [dismissed, setDismissed] = useState(false);

  if (dismissed || profileCompletion >= 80) return null;

  const tips = [];
  if (!hasAvatar) tips.push('Add a profile photo');
  if (profileCompletion < 50) tips.push('Complete your profile details');

  return (
    <div className="bg-gradient-to-r from-orange-50 to-amber-50 border border-orange-200 rounded-xl p-4 mb-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="text-3xl">{'\uD83D\uDC4B'}</div>
          <div>
            <p className="font-semibold text-gray-900">
              Welcome{userName ? `, ${userName}` : ''}! Complete your profile
            </p>
            <p className="text-sm text-gray-600 mt-0.5">
              {tips.join(' \u00B7 ')} — hosts and guests trust complete profiles more
            </p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <Link href="/dashboard/account"
            className="bg-orange-500 hover:bg-orange-600 text-white text-sm font-semibold px-4 py-2 rounded-lg transition">
            Complete Profile
          </Link>
          <button onClick={() => setDismissed(true)} className="text-gray-400 hover:text-gray-600 text-sm">
            Dismiss
          </button>
        </div>
      </div>
    </div>
  );
}
