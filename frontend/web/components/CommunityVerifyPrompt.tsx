'use client';

import { useState } from 'react';

interface Props {
  listingId: string;
  onComplete?: () => void;
}

export default function CommunityVerifyPrompt({ listingId, onComplete }: Props) {
  const [visible, setVisible] = useState(true);
  const [photosMatch, setPhotosMatch] = useState<boolean | null>(null);
  const [amenitiesMatch, setAmenitiesMatch] = useState<boolean | null>(null);
  const [feltSafe, setFeltSafe] = useState<boolean | null>(null);
  const [submitted, setSubmitted] = useState(false);
  const [milesEarned, setMilesEarned] = useState(0);

  if (!visible || submitted) {
    if (submitted) {
      return (
        <div className="bg-green-50 border border-green-200 rounded-xl p-4 text-center">
          <p className="text-green-700 font-semibold">Thanks for verifying! You earned {milesEarned} Safar Miles</p>
        </div>
      );
    }
    return null;
  }

  const allAnswered = photosMatch !== null && amenitiesMatch !== null && feltSafe !== null;

  const handleSubmit = async () => {
    if (!allAnswered) return;
    const token = localStorage.getItem('access_token') || '';
    try {
      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/v1/listings/${listingId}/community-verify?photosMatch=${photosMatch}&amenitiesMatch=${amenitiesMatch}&feltSafe=${feltSafe}`,
        { method: 'POST', headers: { Authorization: `Bearer ${token}` } }
      );
      const data = await res.json();
      setMilesEarned(data.milesEarned || 200);
      setSubmitted(true);
      onComplete?.();
    } catch {
      alert('Failed to submit verification');
    }
  };

  const YesNo = ({ value, onChange }: { value: boolean | null; onChange: (v: boolean) => void }) => (
    <div className="flex gap-2">
      <button onClick={() => onChange(true)}
        className={`px-4 py-1.5 rounded-lg text-sm font-medium transition ${value === true ? 'bg-green-500 text-white' : 'bg-gray-100 hover:bg-gray-200'}`}>
        Yes
      </button>
      <button onClick={() => onChange(false)}
        className={`px-4 py-1.5 rounded-lg text-sm font-medium transition ${value === false ? 'bg-red-500 text-white' : 'bg-gray-100 hover:bg-gray-200'}`}>
        No
      </button>
    </div>
  );

  return (
    <div className="bg-blue-50 border border-blue-200 rounded-xl p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-semibold text-blue-900">Help verify this listing — earn 200 Miles</h3>
        <button onClick={() => setVisible(false)} className="text-gray-400 hover:text-gray-600 text-sm">Dismiss</button>
      </div>
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <span className="text-sm text-gray-700">Did the listing match the photos?</span>
          <YesNo value={photosMatch} onChange={setPhotosMatch} />
        </div>
        <div className="flex items-center justify-between">
          <span className="text-sm text-gray-700">Were listed amenities available?</span>
          <YesNo value={amenitiesMatch} onChange={setAmenitiesMatch} />
        </div>
        <div className="flex items-center justify-between">
          <span className="text-sm text-gray-700">Did you feel safe?</span>
          <YesNo value={feltSafe} onChange={setFeltSafe} />
        </div>
      </div>
      <button onClick={handleSubmit} disabled={!allAnswered}
        className="mt-4 w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2.5 rounded-xl disabled:opacity-50 transition">
        Submit Verification
      </button>
    </div>
  );
}
