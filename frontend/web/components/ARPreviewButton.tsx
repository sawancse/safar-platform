'use client';

import { useState, useEffect } from 'react';

interface Props {
  listingName: string;
}

export default function ARPreviewButton({ listingName }: Props) {
  const [arSupported, setArSupported] = useState(false);
  const [showTooltip, setShowTooltip] = useState(false);

  useEffect(() => {
    // Check for WebXR support
    if ((navigator as any).xr) {
      (navigator as any).xr.isSessionSupported('immersive-ar').then(setArSupported).catch(() => {});
    }
  }, []);

  if (!arSupported) return null;

  return (
    <div className="relative">
      <button
        onClick={() => setShowTooltip(!showTooltip)}
        className="inline-flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-full text-sm font-medium shadow-lg hover:shadow-xl transition-all"
      >
        <span>📱</span>
        View in AR
      </button>
      {showTooltip && (
        <div className="absolute bottom-12 left-0 bg-gray-900 text-white p-3 rounded-lg text-xs max-w-[200px] shadow-xl">
          AR Preview for &quot;{listingName}&quot; coming soon! You&apos;ll be able to visualize room dimensions in your space.
          <button onClick={() => setShowTooltip(false)} className="block mt-2 text-purple-300 hover:text-white">Got it</button>
        </div>
      )}
    </div>
  );
}
