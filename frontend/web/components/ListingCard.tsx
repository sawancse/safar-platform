'use client';

import Link from 'next/link';
import { useState, useEffect } from 'react';
import { useSearchParams } from 'next/navigation';
import { formatPaise } from '@/lib/utils';
import { api } from '@/lib/api';
import type { Listing } from '@/types';

interface Props {
  listing: Listing;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

function resolveUrl(url?: string): string | undefined {
  if (!url) return undefined;
  if (url.startsWith('http')) return url;
  return `${API_URL}${url}`;
}

interface MediaItem {
  id: string;
  url: string;
  type: string;
  isPrimary: boolean;
}

function getRatingLabel(rating: number): { label: string; color: string; bg: string } {
  if (rating >= 4.5) return { label: 'Excellent', color: 'text-white', bg: 'bg-green-600' };
  if (rating >= 4.0) return { label: 'Very Good', color: 'text-white', bg: 'bg-blue-600' };
  if (rating >= 3.5) return { label: 'Good', color: 'text-white', bg: 'bg-teal-600' };
  if (rating >= 3.0) return { label: 'Average', color: 'text-white', bg: 'bg-orange-500' };
  return { label: 'Below Average', color: 'text-white', bg: 'bg-gray-500' };
}

function getMealPlanLabel(mealPlan: string): string | null {
  switch (mealPlan) {
    case 'HALF_BOARD': return 'Half Board';
    case 'FULL_BOARD': return 'Full Board';
    case 'ALL_INCLUSIVE': return 'All Inclusive';
    default: return null;
  }
}

export default function ListingCard({ listing }: Props) {
  const searchParams = useSearchParams();
  const [photos, setPhotos] = useState<string[]>([]);
  const [index, setIndex] = useState(0);
  const [loaded, setLoaded] = useState(false);
  const [saved, setSaved] = useState(false);

  // Build listing URL with date/guest params forwarded
  const listingUrl = (() => {
    const params = new URLSearchParams();
    const checkIn = searchParams.get('checkIn');
    const checkOut = searchParams.get('checkOut');
    const guests = searchParams.get('guests');
    const children = searchParams.get('children');
    const infants = searchParams.get('infants');
    const pets = searchParams.get('pets');
    if (checkIn) params.set('checkIn', checkIn);
    if (checkOut) params.set('checkOut', checkOut);
    if (guests) params.set('guests', guests);
    if (children) params.set('children', children);
    if (infants) params.set('infants', infants);
    if (pets) params.set('pets', pets);
    const qs = params.toString();
    return `/listings/${listing.id}${qs ? `?${qs}` : ''}`;
  })();

  useEffect(() => {
    const token = localStorage.getItem('access_token');
    if (!token) return;
    api.getBucketList(token)
      .then((data: any) => {
        const items = Array.isArray(data) ? data : data?.content ?? [];
        if (items.some((i: any) => i.listingId === listing.id)) setSaved(true);
      })
      .catch(() => {});
  }, [listing.id]);

  function toggleSave(e: React.MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    const token = localStorage.getItem('access_token');
    if (!token) {
      window.location.href = `/auth?redirect=/listings/${listing.id}`;
      return;
    }
    if (saved) {
      api.removeFromBucketList(listing.id, token).then(() => setSaved(false)).catch(() => {});
    } else {
      api.addToBucketList(listing.id, token).then(() => setSaved(true)).catch(() => {});
    }
  }

  // Lazy-load media on first hover
  function loadMedia() {
    if (loaded) return;
    setLoaded(true);
    fetch(`${API_URL}/api/v1/listings/${listing.id}/media`)
      .then((r) => r.json())
      .then((items: MediaItem[]) => {
        const urls = items
          .filter((m) => m.type === 'PHOTO' || m.type === 'IMAGE')
          .map((m) => resolveUrl(m.url)!)
          .filter(Boolean);
        if (urls.length > 0) setPhotos(urls);
      })
      .catch(() => {});
  }

  const displayPhotos = photos.length > 0 ? photos : (listing.primaryPhotoUrl ? [resolveUrl(listing.primaryPhotoUrl)!] : []);
  const hasMultiple = displayPhotos.length > 1;

  function prev(e: React.MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    setIndex((i) => (i === 0 ? displayPhotos.length - 1 : i - 1));
  }

  function next(e: React.MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    setIndex((i) => (i === displayPhotos.length - 1 ? 0 : i + 1));
  }

  // Build inclusion badges
  const inclusions: string[] = [];
  if (listing.breakfastIncluded) inclusions.push('Breakfast Included');
  if (listing.freeCancellation) inclusions.push('Free Cancellation');
  if (listing.noPrepayment) inclusions.push('No Prepayment');
  if (listing.mealPlan) {
    const mealLabel = getMealPlanLabel(listing.mealPlan);
    if (mealLabel) inclusions.push(mealLabel);
  }

  const priceUnit =
    listing.type === 'PG' || listing.type === 'COLIVING' ? '/ month' :
    listing.pricingUnit === 'HOUR' ? '/ hour' : '/ night';

  return (
    <Link
      href={listingUrl}
      className="group block bg-white rounded-xl overflow-hidden border border-gray-200 hover:shadow-xl transition-shadow duration-300"
      onMouseEnter={loadMedia}
    >
      {/* Photo Carousel */}
      <div className="relative aspect-[4/3] bg-gray-100 overflow-hidden">
        {displayPhotos.length > 0 ? (
          <img
            src={displayPhotos[index]}
            alt={listing.title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-gray-300 text-4xl">
            <svg className="w-12 h-12" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909M3.75 21h16.5A2.25 2.25 0 0022.5 18.75V5.25A2.25 2.25 0 0020.25 3H3.75A2.25 2.25 0 001.5 5.25v13.5A2.25 2.25 0 003.75 21z" /></svg>
          </div>
        )}

        {/* Carousel arrows */}
        {hasMultiple && (
          <>
            <button
              onClick={prev}
              className="absolute left-2 top-1/2 -translate-y-1/2 bg-white/80 hover:bg-white text-gray-800 rounded-full w-7 h-7 flex items-center justify-center shadow opacity-0 group-hover:opacity-100 transition-opacity text-sm font-bold"
            >
              &#8249;
            </button>
            <button
              onClick={next}
              className="absolute right-2 top-1/2 -translate-y-1/2 bg-white/80 hover:bg-white text-gray-800 rounded-full w-7 h-7 flex items-center justify-center shadow opacity-0 group-hover:opacity-100 transition-opacity text-sm font-bold"
            >
              &#8250;
            </button>
            {/* Dot indicators */}
            <div className="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1">
              {displayPhotos.map((_, i) => (
                <span
                  key={i}
                  className={`block w-1.5 h-1.5 rounded-full transition ${
                    i === index ? 'bg-white' : 'bg-white/50'
                  }`}
                />
              ))}
            </div>
          </>
        )}

        {/* Top-left badges stack */}
        <div className="absolute top-2.5 left-2.5 flex flex-col gap-1.5">
          {listing.instantBook && (
            <span className="bg-blue-600 text-white text-[10px] font-semibold px-2 py-0.5 rounded-full w-fit">
              Instant
            </span>
          )}
          {listing.status === 'VERIFIED' && (
            <span className="bg-green-600 text-white text-[10px] font-semibold px-2 py-0.5 rounded-full w-fit">
              Verified
            </span>
          )}
          {listing.medicalStay && (
            <span className="bg-blue-500 text-white text-[10px] font-bold px-1.5 py-0.5 rounded w-fit">
              Medical
            </span>
          )}
          {(listing.type === 'PG' || listing.type === 'COLIVING') && listing.occupancyType && (
            <span className="bg-purple-500 text-white text-[10px] font-bold px-1.5 py-0.5 rounded w-fit">
              {listing.occupancyType === 'MALE' ? '\u2642 Boys' :
               listing.occupancyType === 'FEMALE' ? '\u2640 Girls' : '\u26A5 Co-ed'}
            </span>
          )}
          {listing.coupleFriendly && (
            <span className="bg-pink-500 text-white text-[10px] font-bold px-1.5 py-0.5 rounded w-fit">
              Couple Friendly
            </span>
          )}
        </div>

        {/* Preferred badge bottom-left */}
        {listing.preferredPartner && (
          <span className="absolute bottom-3 left-3 bg-amber-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 shadow">
            <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" /></svg>
            Preferred
          </span>
        )}

        {/* Save / Heart button */}
        <button
          onClick={toggleSave}
          className="absolute top-2 right-2 w-8 h-8 flex items-center justify-center rounded-full bg-white/80 hover:bg-white shadow transition-all hover:scale-110 z-10"
          title={saved ? 'Remove from saved' : 'Save'}
        >
          <span className="text-lg leading-none">{saved ? '\u2764\uFE0F' : '\u2661'}</span>
        </button>
      </div>

      {/* Card Body */}
      <div className="p-4">
        {/* Title + Rating row */}
        <div className="flex items-start justify-between gap-2">
          <div className="flex-1 min-w-0">
            <h3 className="font-semibold text-sm text-gray-900 truncate">{listing.title}</h3>
            <p className="text-xs text-gray-500 mt-0.5">{listing.city}, {listing.state}</p>
          </div>

          {/* Rating badge - MakeMyTrip style */}
          {listing.avgRating != null && listing.avgRating > 0 && (() => {
            const { label, bg } = getRatingLabel(listing.avgRating);
            return (
              <div className={`${bg} text-white px-2 py-1 rounded-md flex items-center gap-1.5 shrink-0`}>
                <span className="text-xs font-bold">{listing.avgRating.toFixed(1)}</span>
                <span className="text-[10px] font-medium">{label}</span>
              </div>
            );
          })()}
        </div>

        {/* Location highlight */}
        {listing.locationHighlight && (
          <p className="text-[11px] text-teal-700 mt-1 flex items-center gap-1">
            <svg className="w-3 h-3 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
            {listing.locationHighlight}
          </p>
        )}

        {/* Inclusion pills */}
        {inclusions.length > 0 && (
          <div className="flex flex-wrap gap-1.5 mt-2">
            {inclusions.map((inc) => (
              <span
                key={inc}
                className="inline-flex items-center text-[10px] font-medium text-green-700 bg-green-50 border border-green-200 rounded-full px-2 py-0.5"
              >
                <svg className="w-2.5 h-2.5 mr-0.5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" /></svg>
                {inc}
              </span>
            ))}
          </div>
        )}

        {/* Deal badges row */}
        <div className="flex flex-wrap gap-1.5 mt-2">
          {listing.zeroPaymentBooking && (
            <span className="inline-flex items-center text-[10px] font-semibold text-green-700 bg-green-50 border border-green-300 rounded px-2 py-0.5">
              Book with &#8377;0 payment
            </span>
          )}
          {listing.earlyBirdDiscountPercent != null && listing.earlyBirdDiscountPercent > 0 && (
            <span className="inline-flex items-center text-[10px] font-semibold text-amber-700 bg-amber-50 border border-amber-300 rounded px-2 py-0.5">
              Early Bird: {listing.earlyBirdDiscountPercent}% off
            </span>
          )}
        </div>

        {/* Price row */}
        <div className="mt-3 flex items-baseline gap-1">
          <span className="text-lg font-bold text-gray-900">{formatPaise(listing.basePricePaise)}</span>
          <span className="text-xs text-gray-500">{priceUnit}</span>
        </div>

        {listing.maxGuests && (
          <p className="text-[11px] text-gray-400 mt-1">Up to {listing.maxGuests} guests</p>
        )}
      </div>
    </Link>
  );
}
