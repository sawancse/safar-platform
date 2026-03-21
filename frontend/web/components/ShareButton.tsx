'use client';

import { useState, useRef, useEffect } from 'react';

interface Props {
  title: string;
  city: string;
  state: string;
  price: string;
  imageUrl?: string;
  listingId: string;
}

const BASE_URL = typeof window !== 'undefined' ? window.location.origin : 'https://safar.in';

export default function ShareButton({ title, city, state, price, imageUrl, listingId }: Props) {
  const [open, setOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const modalRef = useRef<HTMLDivElement>(null);

  const shareUrl = `${BASE_URL}/listings/${listingId}?utm_source=share&utm_medium=social`;
  const shareText = `Check out "${title}" in ${city}, ${state} on Safar — ${price}/night`;

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (modalRef.current && !modalRef.current.contains(e.target as Node)) setOpen(false);
    }
    if (open) document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, [open]);

  async function handleShare() {
    // Try native Web Share API first (mobile)
    if (typeof navigator !== 'undefined' && navigator.share) {
      try {
        await navigator.share({ title, text: shareText, url: shareUrl });
        return;
      } catch {
        // User cancelled or API not supported — fall through to modal
      }
    }
    setOpen(true);
  }

  async function copyLink() {
    try {
      await navigator.clipboard.writeText(shareUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback
      const input = document.createElement('input');
      input.value = shareUrl;
      document.body.appendChild(input);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  }

  const shareOptions = [
    {
      label: 'Copy link',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
        </svg>
      ),
      action: copyLink,
      extra: copied ? 'Copied!' : undefined,
    },
    {
      label: 'WhatsApp',
      icon: <span className="text-xl">💬</span>,
      action: () => window.open(`https://wa.me/?text=${encodeURIComponent(shareText + '\n' + shareUrl)}`, '_blank'),
    },
    {
      label: 'Email',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
        </svg>
      ),
      action: () => window.open(`mailto:?subject=${encodeURIComponent(`Check out this stay on Safar: ${title}`)}&body=${encodeURIComponent(shareText + '\n\n' + shareUrl)}`, '_blank'),
    },
    {
      label: 'Messages',
      icon: <span className="text-xl">💬</span>,
      action: () => window.open(`sms:?body=${encodeURIComponent(shareText + ' ' + shareUrl)}`, '_blank'),
    },
    {
      label: 'Facebook',
      icon: <span className="text-xl font-bold text-blue-600">f</span>,
      action: () => window.open(`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(shareUrl)}`, '_blank', 'width=600,height=400'),
    },
    {
      label: 'X',
      icon: <span className="text-lg font-bold">𝕏</span>,
      action: () => window.open(`https://twitter.com/intent/tweet?text=${encodeURIComponent(shareText)}&url=${encodeURIComponent(shareUrl)}`, '_blank', 'width=600,height=400'),
    },
  ];

  return (
    <>
      <button onClick={handleShare}
        className="flex items-center gap-1.5 text-sm font-semibold text-gray-700 hover:text-gray-900 underline decoration-gray-300 underline-offset-4 hover:decoration-gray-500 transition">
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
            d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
        </svg>
        Share
      </button>

      {/* Share Modal */}
      {open && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div ref={modalRef} className="bg-white rounded-2xl max-w-md w-full overflow-hidden">
            {/* Header */}
            <div className="flex items-center justify-between px-5 py-4 border-b">
              <h2 className="text-lg font-bold text-gray-900">Share this place</h2>
              <button onClick={() => setOpen(false)}
                className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-gray-100 transition">
                <svg className="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            {/* Listing preview */}
            <div className="px-5 py-4 flex gap-4 items-start">
              {imageUrl ? (
                <img src={imageUrl.startsWith('http') ? imageUrl : `http://localhost:8080${imageUrl}`}
                  alt={title} className="w-16 h-16 rounded-xl object-cover flex-shrink-0" />
              ) : (
                <div className="w-16 h-16 rounded-xl bg-gray-100 flex items-center justify-center text-2xl flex-shrink-0">🏠</div>
              )}
              <div className="min-w-0">
                <p className="font-semibold text-gray-900 text-sm leading-tight">{title}</p>
                <p className="text-xs text-gray-500 mt-0.5">{city}, {state}</p>
                <p className="text-xs text-gray-500">{price} / night</p>
              </div>
            </div>

            {/* Share options grid */}
            <div className="px-5 pb-5 grid grid-cols-2 gap-3">
              {shareOptions.map((opt) => (
                <button key={opt.label} onClick={() => { opt.action(); if (opt.label !== 'Copy link') setOpen(false); }}
                  className="flex items-center gap-3 border border-gray-200 rounded-xl px-4 py-3 hover:bg-gray-50 transition text-left">
                  <span className="w-8 h-8 flex items-center justify-center bg-gray-100 rounded-lg flex-shrink-0">
                    {opt.icon}
                  </span>
                  <div>
                    <p className="text-sm font-medium text-gray-800">{opt.label}</p>
                    {opt.extra && <p className="text-xs text-green-600 font-semibold">{opt.extra}</p>}
                  </div>
                </button>
              ))}
            </div>

            {/* Share URL display */}
            <div className="px-5 pb-5">
              <div className="flex items-center gap-2 bg-gray-50 border rounded-xl px-3 py-2">
                <input readOnly value={shareUrl}
                  className="flex-1 text-xs text-gray-600 bg-transparent outline-none truncate" />
                <button onClick={copyLink}
                  className={`text-xs font-semibold px-3 py-1 rounded-lg transition ${
                    copied ? 'bg-green-100 text-green-700' : 'bg-orange-100 text-orange-700 hover:bg-orange-200'
                  }`}>
                  {copied ? 'Copied!' : 'Copy'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
