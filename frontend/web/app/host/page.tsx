'use client';

import { useEffect, useState, useRef } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import { geocodeIndianAddress } from '@/lib/geocode';
import type { HostSubscription, Listing, SubscriptionTier } from '@/types';
import HostBookingsTab from './HostBookingsTab';
import HostKycTab from './HostKycTab';
import HostEarningsTab from './HostEarningsTab';
import HostInvoicesTab from './HostInvoicesTab';
import HostAnalyticsTab from './HostAnalyticsTab';
import HostTransactionsTab from './HostTransactionsTab';
import HostCalendarTab from './HostCalendarTab';
import HostRoomTypesTab from './HostRoomTypesTab';
import HostOccupancyTab from './HostOccupancyTab';
import HostReviewsTab from './HostReviewsTab';
import HostPricingRulesTab from './HostPricingRulesTab';
import HostPgPackagesTab from './HostPgPackagesTab';
import HostMessagesTab from './HostMessagesTab';
import EditListingModal from '@/components/EditListingModal';

const STATUS_STYLE: Record<string, string> = {
  DRAFT:    'bg-gray-100 text-gray-600',
  PENDING_VERIFICATION: 'bg-yellow-100 text-yellow-700',
  VERIFIED: 'bg-green-100 text-green-700',
  PAUSED:   'bg-blue-100 text-blue-700',
  REJECTED: 'bg-red-100 text-red-600',
  ARCHIVED: 'bg-purple-100 text-purple-700',
  SUSPENDED: 'bg-red-200 text-red-800',
};

interface MediaItem {
  id: string;
  url: string;
  type: string;
  isPrimary: boolean;
}

interface Readiness {
  photoCount: number;
  primaryPhoto: boolean;
  videoCount: number;
  ready: boolean;
}

interface UploadJob {
  file: File;
  status: 'queued' | 'uploading' | 'done' | 'error';
  preview: string;
  mediaType: string;
}

export default function HostPage() {
  const router = useRouter();
  const [listings, setListings] = useState<Listing[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [mediaPanel, setMediaPanel] = useState<string | null>(null);
  const [mediaItems, setMediaItems] = useState<MediaItem[]>([]);
  const [readiness, setReadiness] = useState<Record<string, Readiness>>({});
  const [guestPreview, setGuestPreview] = useState<string | null>(null);
  const [subscription, setSubscription] = useState<HostSubscription | null>(null);
  const [showUpgradeModal, setShowUpgradeModal] = useState(false);
  const [upgradingTier, setUpgradingTier] = useState<SubscriptionTier | null>(null);
  const [activeTab, setActiveTab] = useState<'listings' | 'bookings' | 'calendar' | 'roomTypes' | 'pricing' | 'packages' | 'reviews' | 'messages' | 'kyc' | 'earnings' | 'invoices' | 'analytics' | 'transactions' | 'occupancy'>('listings');
  const [commissionInfo, setCommissionInfo] = useState<{ commissionPercent: number; tier: string } | null>(null);
  const [editingListing, setEditingListing] = useState<Listing | null>(null);

  // Bulk upload state
  const [uploadJobs, setUploadJobs] = useState<UploadJob[]>([]);
  const [bulkUploading, setBulkUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);

  // New listing form state
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [type, setType] = useState('HOME');
  const [addressLine1, setAddressLine1] = useState('');
  const [pincode, setPincode] = useState('');
  const [price, setPrice] = useState('');
  const [maxGuests, setMaxGuests] = useState('2');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');
  const [resolvedLat, setResolvedLat] = useState<number | null>(null);
  const [resolvedLng, setResolvedLng] = useState<number | null>(null);
  const [geocoding, setGeocoding] = useState(false);

  const [token, setToken] = useState('');

  // Keep token fresh — auto-refresh may update localStorage
  useEffect(() => {
    const interval = setInterval(() => {
      const current = localStorage.getItem('access_token') ?? '';
      if (current && current !== token) setToken(current);
    }, 5000); // check every 5s
    return () => clearInterval(interval);
  }, [token]);

  function fetchListings(t?: string) {
    const tkn = t || token;
    if (!tkn) return;
    api.getMyListings(tkn)
      .then((data) => {
        setListings(data);
        data.filter((l: Listing) => l.status === 'DRAFT').forEach((l: Listing) => {
          fetchReadiness(l.id, tkn);
        });
      })
      .catch(() => setListings([]))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    const t = localStorage.getItem('access_token') ?? '';
    setToken(t);
    if (!t) {
      router.push('/auth?redirect=/host');
      return;
    }
    api.getSubscription(t)
      .then(setSubscription)
      .catch(() => setSubscription(null));
    api.getCommissionRate(t)
      .then(setCommissionInfo)
      .catch(() => setCommissionInfo(null));
    fetchListings(t);
  }, [router]);

  // Auto-geocode when pincode (6 digits) and city are filled
  useEffect(() => {
    if (pincode.length === 6 && city.trim()) {
      setGeocoding(true);
      geocodeIndianAddress(pincode, city, state).then((coords) => {
        if (coords) {
          setResolvedLat(coords.lat);
          setResolvedLng(coords.lng);
        }
        setGeocoding(false);
      });
    }
  }, [pincode, city, state]);

  const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
  const getAuthHeaders = () => ({ Authorization: `Bearer ${token}` });
  const uploadUrl = apiUrl; // Route through API Gateway for CORS support

  // Subscription helpers
  const TIER_LIMITS: Record<string, number> = { STARTER: 2, PRO: 10, COMMERCIAL: Infinity };
  const TIER_PRICES: Record<string, number> = { STARTER: 999, PRO: 2499, COMMERCIAL: 3999 };
  const currentTier = subscription?.tier ?? 'STARTER';
  const tierLimit = TIER_LIMITS[currentTier] ?? 2;
  const activeListingCount = listings.filter((l) => l.status !== 'DRAFT').length;
  const isAtLimit = activeListingCount >= tierLimit;

  async function handleStartTrial(tier: SubscriptionTier) {
    if (!token) return;
    setUpgradingTier(tier);
    try {
      // Try startTrial first; if subscription exists (409), fall back to upgrade
      const sub = await api.startTrial(tier, token);
      setSubscription(sub);
      setShowUpgradeModal(false);
    } catch {
      // Subscription already exists — upgrade instead
      try {
        const sub = await api.upgradeSubscription(tier, token);
        setSubscription(sub);
        setShowUpgradeModal(false);
      } catch (e: any) {
        alert(e.message || 'Failed to upgrade');
      }
    } finally {
      setUpgradingTier(null);
    }
  }

  async function fetchReadiness(listingId: string, t?: string) {
    try {
      const res = await fetch(`${uploadUrl}/api/v1/listings/${listingId}/verification-readiness`, {
        headers: { Authorization: `Bearer ${t || token}` },
      });
      if (res.ok) {
        const data = await res.json();
        setReadiness((prev) => ({ ...prev, [listingId]: data }));
      }
    } catch { /* ignore */ }
  }

  async function uploadFile(listingId: string, file: File, mediaType: string, primary = false) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('mediaType', mediaType);
    formData.append('primary', String(primary));
    const res = await fetch(`${uploadUrl}/api/v1/listings/${listingId}/media/upload`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: formData,
    });
    if (!res.ok) throw new Error(await res.text().catch(() => 'Upload failed'));
    return res.json();
  }

  // ── Bulk Upload ────────────────────────────────────────
  async function handleBulkUpload(listingId: string) {
    if (uploadJobs.length === 0) return;
    setBulkUploading(true);
    for (let i = 0; i < uploadJobs.length; i++) {
      const job = uploadJobs[i];
      if (job.status !== 'queued') continue;
      setUploadJobs((prev) => prev.map((j, idx) => idx === i ? { ...j, status: 'uploading' } : j));
      try {
        const isPrimary = i === 0 && job.mediaType === 'PHOTO';
        const added = await uploadFile(listingId, job.file, job.mediaType, isPrimary);
        setMediaItems((prev) => [...prev, { id: added.id, url: added.url, type: added.type, isPrimary: isPrimary }]);
        setUploadJobs((prev) => prev.map((j, idx) => idx === i ? { ...j, status: 'done' } : j));
      } catch {
        setUploadJobs((prev) => prev.map((j, idx) => idx === i ? { ...j, status: 'error' } : j));
      }
    }
    setBulkUploading(false);
    fetchReadiness(listingId);
  }

  function handleFilesSelected(files: FileList | null, autoUploadListingId?: string) {
    if (!files || files.length === 0) return;
    const jobs: UploadJob[] = Array.from(files).map((file) => ({
      file,
      status: 'queued' as const,
      preview: URL.createObjectURL(file),
      mediaType: file.type.startsWith('video') ? 'VIDEO' : 'PHOTO',
    }));
    setUploadJobs((prev) => [...prev, ...jobs]);
    // Auto-upload immediately if listing ID provided
    if (autoUploadListingId) {
      setTimeout(() => handleBulkUpload(autoUploadListingId), 100);
    }
  }

  function handleDrop(e: React.DragEvent, listingId: string) {
    e.preventDefault();
    e.stopPropagation();
    const files = e.dataTransfer.files;
    if (files.length > 0) handleFilesSelected(files, listingId);
  }

  function handleDragOver(e: React.DragEvent) {
    e.preventDefault();
    e.stopPropagation();
  }

  function removeUploadJob(idx: number) {
    setUploadJobs((prev) => {
      URL.revokeObjectURL(prev[idx].preview);
      return prev.filter((_, i) => i !== idx);
    });
  }

  // ── Actions ────────────────────────────────────────────
  async function handleUnpublishToDraft(listingId: string) {
    if (!confirm('Unpublish this listing? It will go back to DRAFT and you\'ll need to re-verify to go live again.')) return;
    setActionLoading(listingId);
    try {
      const res = await fetch(`${uploadUrl}/api/v1/listings/${listingId}/unpublish`, {
        method: 'POST', headers: getAuthHeaders(),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        alert(err.detail || err.message || 'Failed to unpublish');
        return;
      }
      const updated = await res.json();
      setListings((prev) => prev.map((l) => (l.id === listingId ? updated : l)));
    } catch {
      alert('Failed to unpublish listing');
    } finally {
      setActionLoading(null);
    }
  }

  async function handleSubmitForVerification(listingId: string) {
    setActionLoading(listingId);
    try {
      const res = await fetch(`${uploadUrl}/api/v1/listings/${listingId}/submit`, {
        method: 'POST', headers: getAuthHeaders(),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        alert(err.detail || err.message || 'Failed to submit');
        return;
      }
      const updated = await res.json();
      setListings((prev) => prev.map((l) => (l.id === listingId ? updated : l)));
    } catch {
      alert('Failed to submit for verification');
    } finally {
      setActionLoading(null);
    }
  }

  async function loadMedia(listingId: string) {
    if (mediaPanel === listingId) {
      setMediaPanel(null);
      setUploadJobs([]);
      return;
    }
    try {
      const res = await fetch(`${uploadUrl}/api/v1/listings/${listingId}/media`, {
        headers: getAuthHeaders(),
      });
      if (res.ok) setMediaItems(await res.json());
      else setMediaItems([]);
    } catch {
      setMediaItems([]);
    }
    setMediaPanel(listingId);
    setUploadJobs([]);
  }

  async function handleSetPrimary(listingId: string, mediaId: string) {
    setActionLoading('set-primary');
    try {
      const res = await fetch(`${uploadUrl}/api/v1/listings/${listingId}/media/${mediaId}/primary`, {
        method: 'PUT',
        headers: getAuthHeaders(),
      });
      if (res.ok) {
        setMediaItems((prev) => prev.map((m) => ({ ...m, isPrimary: m.id === mediaId })));
        fetchReadiness(listingId);
      }
    } catch {
      alert('Failed to set primary photo');
    } finally {
      setActionLoading(null);
    }
  }

  async function handleCreateListing(e: React.FormEvent) {
    e.preventDefault();
    if (!token) return;
    setSubmitting(true);
    setFormError('');
    try {
      const body = {
        title, description, city, state, type, addressLine1, pincode,
        lat: resolvedLat ?? 0.0, lng: resolvedLng ?? 0.0,
        basePricePaise: Math.round(Number(price) * 100),
        maxGuests: Number(maxGuests),
      };
      const res = await fetch(`${uploadUrl}/api/v1/listings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const text = await res.text().catch(() => '');
        let detail = 'Failed to create listing';
        try { const err = JSON.parse(text); detail = err.detail || err.message || detail; } catch { if (text) detail = text; }
        throw new Error(detail);
      }
      const newListing = await res.json();
      setListings((prev) => [newListing, ...prev]);
      setShowForm(false);
      setTitle(''); setDescription(''); setCity(''); setState('');
      setType('HOME'); setAddressLine1(''); setPincode('');
      setPrice(''); setMaxGuests('2');
      setResolvedLat(null); setResolvedLng(null);
      // Upgrade role to HOST in localStorage
      const currentRole = localStorage.getItem('user_role');
      if (currentRole === 'GUEST') {
        localStorage.setItem('user_role', 'HOST');
      }
      // Auto-open media panel for the new listing
      loadMedia(newListing.id);
      fetchReadiness(newListing.id);
    } catch (e: any) {
      setFormError(e.message);
    } finally {
      setSubmitting(false);
    }
  }

  // ── Progress Ring Component ────────────────────────────
  function ProgressRing({ r }: { r: Readiness }) {
    let pct = 0;
    if (r.photoCount > 0) pct += 40;
    if (r.primaryPhoto) pct += 40;
    if (r.videoCount > 0) pct += 20;
    const radius = 20;
    const circumference = 2 * Math.PI * radius;
    const offset = circumference - (pct / 100) * circumference;
    const color = pct >= 80 ? '#22c55e' : pct >= 40 ? '#f97316' : '#d1d5db';
    return (
      <div className="flex items-center gap-2">
        <svg width="50" height="50" className="transform -rotate-90">
          <circle cx="25" cy="25" r={radius} fill="none" stroke="#e5e7eb" strokeWidth="4" />
          <circle cx="25" cy="25" r={radius} fill="none" stroke={color} strokeWidth="4"
            strokeDasharray={circumference} strokeDashoffset={offset}
            strokeLinecap="round" className="transition-all duration-500" />
        </svg>
        <span className="text-sm font-semibold" style={{ color }}>{pct}%</span>
      </div>
    );
  }

  // ── Guest Preview Modal ────────────────────────────────
  function GuestPreviewModal({ listing }: { listing: Listing }) {
    const [previewMedia, setPreviewMedia] = useState<MediaItem[]>([]);
    const [loadingPreview, setLoadingPreview] = useState(true);
    const [idx, setIdx] = useState(0);

    // Fetch media for this listing when modal opens
    useEffect(() => {
      setLoadingPreview(true);
      fetch(`${uploadUrl}/api/v1/listings/${listing.id}/media`, { headers: getAuthHeaders() })
        .then(r => r.ok ? r.json() : [])
        .then(data => setPreviewMedia(Array.isArray(data) ? data : []))
        .catch(() => setPreviewMedia([]))
        .finally(() => setLoadingPreview(false));
    }, [listing.id]);

    const photos = previewMedia.filter((m: MediaItem) => m.type === 'PHOTO' || m.type === 'IMAGE' || (m.type && !m.type.startsWith('video')));
    const primary = photos.find((p: MediaItem) => p.isPrimary) || photos[0];
    const current = photos[idx] || primary;
    return (
      <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={() => setGuestPreview(null)}>
        <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-auto" onClick={(e) => e.stopPropagation()}>
          <div className="relative">
            {loadingPreview ? (
              <div className="w-full h-64 bg-gray-100 rounded-t-2xl flex items-center justify-center text-gray-400 animate-pulse">Loading photos...</div>
            ) : current ? (
              <img src={current.url.startsWith('http') ? current.url : `${uploadUrl}${current.url}`} alt={listing.title} className="w-full h-64 object-cover rounded-t-2xl" />
            ) : (
              <div className="w-full h-64 bg-gray-200 rounded-t-2xl flex items-center justify-center text-gray-400">No photos</div>
            )}
            <button onClick={() => setGuestPreview(null)} className="absolute top-3 right-3 bg-white/80 rounded-full w-8 h-8 flex items-center justify-center text-gray-700 hover:bg-white">X</button>
            {photos.length > 1 && (
              <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1">
                {photos.map((_, i) => (
                  <button key={i} onClick={() => setIdx(i)}
                    className={`w-2 h-2 rounded-full transition ${i === idx ? 'bg-white' : 'bg-white/50'}`} />
                ))}
              </div>
            )}
            {photos.length > 1 && (
              <>
                <button onClick={() => setIdx((i) => Math.max(0, i - 1))}
                  className="absolute left-2 top-1/2 -translate-y-1/2 bg-white/80 rounded-full w-8 h-8 flex items-center justify-center">{'<'}</button>
                <button onClick={() => setIdx((i) => Math.min(photos.length - 1, i + 1))}
                  className="absolute right-2 top-1/2 -translate-y-1/2 bg-white/80 rounded-full w-8 h-8 flex items-center justify-center">{'>'}</button>
              </>
            )}
          </div>
          <div className="p-5">
            <h2 className="text-xl font-bold">{listing.title}</h2>
            <p className="text-gray-500 text-sm mt-1">{listing.city}, {listing.state}</p>
            <p className="text-lg font-semibold mt-2">{formatPaise(listing.basePricePaise)} / night</p>
            <div className="flex gap-2 mt-3">
              {photos.length > 0 && <span className="text-xs bg-gray-100 px-2 py-1 rounded">{photos.length} photo{photos.length !== 1 ? 's' : ''}</span>}
              {mediaItems.some((m) => m.type === 'VIDEO') && <span className="text-xs bg-gray-100 px-2 py-1 rounded">Video tour</span>}
            </div>
          </div>
        </div>
      </div>
    );
  }

  // ── Loading State ──────────────────────────────────────
  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-20 text-center text-gray-400">
        <div className="text-4xl mb-4 animate-spin">⏳</div>
        <p>Loading your listings...</p>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold">Host Dashboard</h1>
          <p className="text-gray-500 text-sm mt-1">Manage your properties</p>
        </div>
        <Link href="/host/new"
          className="bg-orange-500 hover:bg-orange-600 text-white font-semibold px-4 py-2 rounded-xl text-sm transition inline-block">
          + New listing
        </Link>
      </div>

      {/* ── Tab Bar ────────────────────────────────────── */}
      <div className="flex gap-1 mb-6 bg-gray-100 rounded-xl p-1 max-w-5xl overflow-x-auto">
        <button
          onClick={() => setActiveTab('listings')}
          className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition ${
            activeTab === 'listings' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Listings
        </button>
        <button
          onClick={() => setActiveTab('bookings')}
          className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition ${
            activeTab === 'bookings' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Bookings
        </button>
        <button
          onClick={() => setActiveTab('calendar')}
          className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition ${
            activeTab === 'calendar' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Calendar
        </button>
        <button
          onClick={() => setActiveTab('roomTypes')}
          className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition whitespace-nowrap ${
            activeTab === 'roomTypes' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Room Types
        </button>
        <button
          onClick={() => setActiveTab('pricing')}
          className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition whitespace-nowrap ${
            activeTab === 'pricing' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Pricing Rules
        </button>
        <button
          onClick={() => setActiveTab('packages')}
          className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition whitespace-nowrap ${
            activeTab === 'packages' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Packages
        </button>
        <button
          onClick={() => setActiveTab('reviews')}
          className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition ${
            activeTab === 'reviews' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Reviews
        </button>
        <button
          onClick={() => setActiveTab('messages')}
          className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition ${
            activeTab === 'messages' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Messages
        </button>
        <button
          onClick={() => setActiveTab('earnings')}
          className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition ${
            activeTab === 'earnings' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Earnings
        </button>
        <button
          onClick={() => setActiveTab('invoices')}
          className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition ${
            activeTab === 'invoices' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Invoices
        </button>
        <button
          onClick={() => setActiveTab('analytics')}
          className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition whitespace-nowrap ${
            activeTab === 'analytics' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Analytics
        </button>
        <button
          onClick={() => setActiveTab('occupancy')}
          className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition whitespace-nowrap ${
            activeTab === 'occupancy' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Occupancy
        </button>
        <button
          onClick={() => setActiveTab('transactions')}
          className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition whitespace-nowrap ${
            activeTab === 'transactions' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Transactions
        </button>
        <button
          onClick={() => setActiveTab('kyc')}
          className={`flex-1 py-2 px-4 rounded-lg text-sm font-medium transition ${
            activeTab === 'kyc' ? 'bg-white shadow text-orange-600' : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Verification
        </button>
      </div>

      {/* ── Bookings Tab ───────────────────────────────── */}
      {activeTab === 'bookings' && <HostBookingsTab token={token} />}

      {/* ── Calendar Tab ─────────────────────────────────── */}
      {activeTab === 'calendar' && <HostCalendarTab token={token} listings={listings} />}

      {/* ── Room Types Tab ──────────────────────────────── */}
      {activeTab === 'roomTypes' && <HostRoomTypesTab token={token} listings={listings} />}

      {/* ── Pricing Rules Tab ──────────────────────────────── */}
      {activeTab === 'pricing' && <HostPricingRulesTab token={token} listings={listings} />}

      {/* ── PG Packages Tab ──────────────────────────────── */}
      {activeTab === 'packages' && <HostPgPackagesTab token={token} listings={listings} />}

      {/* ── Reviews Tab ─────────────────────────────────── */}
      {activeTab === 'reviews' && <HostReviewsTab token={token} />}

      {/* ── Messages Tab ─────────────────────────────────── */}
      {activeTab === 'messages' && <HostMessagesTab token={token} />}

      {/* ── Earnings Tab ────────────────────────────────── */}
      {activeTab === 'earnings' && <HostEarningsTab token={token} />}

      {/* ── Invoices Tab ─────────────────────────────────── */}
      {activeTab === 'invoices' && <HostInvoicesTab token={token} />}

      {/* ── Analytics Tab ─────────────────────────────────── */}
      {activeTab === 'analytics' && <HostAnalyticsTab token={token} />}

      {/* ── Occupancy Tab ────────────────────────────────── */}
      {activeTab === 'occupancy' && <HostOccupancyTab token={token} />}

      {/* ── Transactions Tab ──────────────────────────────── */}
      {activeTab === 'transactions' && <HostTransactionsTab token={token} />}

      {/* ── KYC Tab ─────────────────────────────────────── */}
      {activeTab === 'kyc' && <HostKycTab token={token} />}

      {/* ── Listings Tab ───────────────────────────────── */}
      {activeTab === 'listings' && (<>

      {/* ── Subscription Banner ────────────────────────── */}
      <div className="mb-6 border rounded-2xl p-4 flex items-center justify-between bg-white">
        <div className="flex items-center gap-3">
          <span className={`text-xs font-bold px-2.5 py-1 rounded-full ${
            currentTier === 'COMMERCIAL' ? 'bg-purple-100 text-purple-700' :
            currentTier === 'PRO' ? 'bg-blue-100 text-blue-700' :
            'bg-gray-100 text-gray-600'
          }`}>{currentTier}</span>
          <div className="text-sm text-gray-600">
            <span className="font-semibold">{activeListingCount}</span> / {tierLimit === Infinity ? 'Unlimited' : tierLimit} active listings
            {subscription?.status === 'TRIAL' && subscription.trialEndsAt && (
              <span className="ml-2 text-xs text-orange-500">
                Trial ends {new Date(subscription.trialEndsAt).toLocaleDateString('en-IN')}
              </span>
            )}
          </div>
        </div>
        {currentTier !== 'COMMERCIAL' && (
          <button onClick={() => setShowUpgradeModal(true)}
            className="text-sm font-semibold text-orange-600 hover:text-orange-700 px-3 py-1 rounded-lg border border-orange-200 hover:bg-orange-50 transition">
            Upgrade
          </button>
        )}
      </div>

      {/* ── Commission & Performance Discount Banner ─────── */}
      {commissionInfo && (
        <div className="mb-6 border rounded-2xl p-4 bg-white flex items-center gap-4 flex-wrap">
          <div className="flex items-center gap-2">
            <span className="text-xs font-medium text-gray-500">Commission rate:</span>
            <span className="text-sm font-bold text-gray-800">{commissionInfo.commissionPercent}%</span>
          </div>
          {subscription?.commissionDiscountPercent && subscription.commissionDiscountPercent > 0 && (
            <div className="flex items-center gap-1.5 bg-green-50 border border-green-200 px-3 py-1 rounded-full">
              <svg className="w-3.5 h-3.5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
              <span className="text-xs font-semibold text-green-700">
                You save {subscription.commissionDiscountPercent}% — Performance discount applied
              </span>
            </div>
          )}
          {subscription?.preferredPartner && (
            <span className="flex items-center gap-1 bg-yellow-50 border border-yellow-200 px-3 py-1 rounded-full text-xs font-semibold text-yellow-700">
              <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
              </svg>
              Preferred Partner
            </span>
          )}
        </div>
      )}

      {/* ── At Limit Warning ────────────────────────────── */}
      {isAtLimit && currentTier !== 'COMMERCIAL' && (
        <div className="mb-6 bg-amber-50 border border-amber-200 rounded-2xl p-4 flex items-center justify-between">
          <div>
            <p className="text-sm font-semibold text-amber-800">Listing limit reached</p>
            <p className="text-xs text-amber-600 mt-0.5">
              Your {currentTier} plan allows {tierLimit} active listings. Upgrade to list more properties.
            </p>
          </div>
          <button onClick={() => setShowUpgradeModal(true)}
            className="text-sm font-semibold bg-orange-500 hover:bg-orange-600 text-white px-4 py-2 rounded-xl transition whitespace-nowrap">
            Upgrade Plan
          </button>
        </div>
      )}

      {/* ── Upgrade Modal ──────────────────────────────── */}
      {showUpgradeModal && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={() => setShowUpgradeModal(false)}>
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-xl font-bold">Choose Your Plan</h2>
              <button onClick={() => setShowUpgradeModal(false)} className="text-gray-400 hover:text-gray-600 text-xl">X</button>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              {(['STARTER', 'PRO', 'COMMERCIAL'] as SubscriptionTier[]).map((tier) => {
                const isCurrent = currentTier === tier;
                const limit = TIER_LIMITS[tier];
                const price = TIER_PRICES[tier];
                return (
                  <div key={tier} className={`border-2 rounded-xl p-5 text-center transition ${
                    isCurrent ? 'border-orange-400 bg-orange-50' : 'border-gray-200 hover:border-gray-300'
                  }`}>
                    <p className="text-sm font-bold text-gray-800">{tier}</p>
                    <p className="text-2xl font-bold mt-2">{price.toLocaleString('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })}</p>
                    <p className="text-xs text-gray-500">/month</p>
                    <p className="text-sm text-gray-600 mt-3">
                      {limit === Infinity ? 'Unlimited' : limit} listings
                    </p>
                    <p className="text-xs text-gray-400 mt-1">
                      {tier === 'STARTER' && 'Great for getting started'}
                      {tier === 'PRO' && 'For growing hosts'}
                      {tier === 'COMMERCIAL' && 'Commercial & unlimited'}
                    </p>
                    {isCurrent ? (
                      <p className="mt-4 text-xs font-semibold text-orange-600">Current Plan</p>
                    ) : (
                      <button onClick={() => handleStartTrial(tier)}
                        disabled={upgradingTier === tier}
                        className="mt-4 w-full bg-orange-500 hover:bg-orange-600 text-white font-semibold py-2 rounded-lg text-sm disabled:opacity-50 transition">
                        {upgradingTier === tier ? 'Processing...' : 'Start Free Trial'}
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
            <p className="text-xs text-gray-400 text-center mt-4">90-day free trial on all plans. Cancel anytime.</p>
          </div>
        </div>
      )}

      {/* ── Create Listing Form ───────────────────────── */}
      {showForm && (
        <form onSubmit={handleCreateListing} className="border rounded-2xl p-6 mb-8 space-y-4 bg-orange-50">
          <h2 className="text-lg font-semibold">Create a new listing</h2>
          {formError && (
            <div className="bg-red-50 text-red-600 rounded-xl px-4 py-3 text-sm">
              {formError}
              {formError.toLowerCase().includes('listing limit') && (
                <button type="button" onClick={() => setShowUpgradeModal(true)}
                  className="ml-2 underline font-semibold text-orange-600 hover:text-orange-700">
                  Upgrade Plan
                </button>
              )}
            </div>
          )}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
              <input required className="w-full border rounded-xl px-3 py-2 text-sm outline-none focus:border-orange-400"
                placeholder="Cosy studio in Bandra" value={title} onChange={(e) => setTitle(e.target.value)} />
            </div>
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea required rows={3} className="w-full border rounded-xl px-3 py-2 text-sm outline-none focus:border-orange-400 resize-none"
                placeholder="Describe your space..." value={description} onChange={(e) => setDescription(e.target.value)} />
            </div>
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Address</label>
              <input required className="w-full border rounded-xl px-3 py-2 text-sm outline-none focus:border-orange-400"
                placeholder="123, MG Road" value={addressLine1} onChange={(e) => setAddressLine1(e.target.value)} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">City</label>
              <input required className="w-full border rounded-xl px-3 py-2 text-sm outline-none focus:border-orange-400"
                placeholder="Mumbai" value={city} onChange={(e) => setCity(e.target.value)} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">State</label>
              <input required className="w-full border rounded-xl px-3 py-2 text-sm outline-none focus:border-orange-400"
                placeholder="Maharashtra" value={state} onChange={(e) => setState(e.target.value)} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Pincode</label>
              <input required className="w-full border rounded-xl px-3 py-2 text-sm outline-none focus:border-orange-400"
                placeholder="400050" maxLength={6} value={pincode} onChange={(e) => setPincode(e.target.value.replace(/\D/g, ''))} />
              {geocoding && <p className="text-xs text-orange-500 mt-1">Locating...</p>}
              {!geocoding && resolvedLat && <p className="text-xs text-green-600 mt-1">Location found</p>}
              {!geocoding && pincode.length === 6 && !resolvedLat && <p className="text-xs text-gray-400 mt-1">Location will use city center</p>}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
              <select className="w-full border rounded-xl px-3 py-2 text-sm outline-none focus:border-orange-400"
                value={type} onChange={(e) => setType(e.target.value)}>
                <option value="HOME">Home</option>
                <option value="ROOM">Room</option>
                <option value="COMMERCIAL">Commercial</option>
                <option value="UNIQUE">Unique Stay</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Price per night</label>
              <input required type="number" min="1" className="w-full border rounded-xl px-3 py-2 text-sm outline-none focus:border-orange-400"
                placeholder="2500" value={price} onChange={(e) => setPrice(e.target.value)} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Max guests</label>
              <input required type="number" min="1" max="20" className="w-full border rounded-xl px-3 py-2 text-sm outline-none focus:border-orange-400"
                value={maxGuests} onChange={(e) => setMaxGuests(e.target.value)} />
            </div>
          </div>
          <button type="submit" disabled={submitting}
            className="w-full bg-orange-500 hover:bg-orange-600 text-white font-semibold py-3 rounded-xl disabled:opacity-50">
            {submitting ? 'Creating...' : 'Create listing'}
          </button>
        </form>
      )}

      {/* ── Listings ──────────────────────────────────── */}
      {listings.length === 0 ? (
        <div className="text-center py-20 text-gray-400">
          <p className="text-5xl mb-4">🏠</p>
          <p className="text-lg font-medium">No listings yet</p>
          <p className="text-sm mt-1">Create your first listing to start hosting</p>
        </div>
      ) : (
        <div className="space-y-4">
          {listings.map((listing) => {
            const s = STATUS_STYLE[listing.status] ?? 'bg-gray-100 text-gray-600';
            const r = readiness[listing.id];
            const isDraft = listing.status === 'DRAFT';
            const hasNoMedia = r && r.photoCount === 0 && r.videoCount === 0;

            return (
              <div key={listing.id} className="border rounded-2xl p-5">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1">
                    <Link href={`/listings/${listing.id}`} className="font-semibold hover:text-orange-500 transition">
                      {listing.title}
                    </Link>
                    <p className="text-sm text-gray-500 mt-0.5">{listing.city}, {listing.state}</p>
                    <p className="text-sm font-semibold mt-1 text-gray-800">{formatPaise(listing.basePricePaise)} / night</p>
                  </div>
                  <div className="flex items-center gap-3">
                    {/* Aashray badge for VERIFIED listings */}
                    {listing.status === 'VERIFIED' && (
                      listing.aashrayReady ? (
                        <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-teal-100 text-teal-700">Aashray</span>
                      ) : (
                        <Link href="/aashray/host" className="text-xs text-teal-600 hover:text-teal-700 font-medium">
                          + Aashray
                        </Link>
                      )
                    )}
                    {/* UI #4: Progress Ring for DRAFT listings */}
                    {isDraft && r && <ProgressRing r={r} />}
                    <span className={`text-xs font-semibold px-2 py-1 rounded-full ${s}`}>{listing.status}</span>
                  </div>
                </div>

                {/* UI #1: Welcome Card for DRAFT with 0 media */}
                {isDraft && hasNoMedia && (
                  <div className="mt-4 bg-gradient-to-r from-orange-50 to-amber-50 border border-orange-200 rounded-xl p-5 text-center">
                    <p className="text-3xl mb-2">📸</p>
                    <p className="font-semibold text-gray-800">Your listing is almost ready!</p>
                    <p className="text-sm text-gray-500 mt-1">Add photos so guests can see your space. At least 1 photo with a cover photo is required.</p>
                    <button type="button" onClick={() => loadMedia(listing.id)}
                      className="mt-3 bg-orange-500 hover:bg-orange-600 text-white font-semibold px-5 py-2 rounded-xl text-sm transition">
                      Add Photos
                    </button>
                  </div>
                )}

                {/* Verification checklist (shown when has some media but not ready) */}
                {isDraft && r && !hasNoMedia && !r.ready && (
                  <div className="mt-3 bg-gray-50 rounded-xl p-3 space-y-1">
                    <p className="text-xs font-semibold text-gray-600 mb-1">Verification checklist</p>
                    <div className="flex items-center gap-2 text-sm">
                      <span className={r.photoCount > 0 ? 'text-green-600' : 'text-gray-400'}>{r.photoCount > 0 ? '[x]' : '[ ]'}</span>
                      <span className={r.photoCount > 0 ? 'text-gray-700' : 'text-gray-500'}>Photos uploaded ({r.photoCount})</span>
                    </div>
                    <div className="flex items-center gap-2 text-sm">
                      <span className={r.primaryPhoto ? 'text-green-600' : 'text-gray-400'}>{r.primaryPhoto ? '[x]' : '[ ]'}</span>
                      <span className={r.primaryPhoto ? 'text-gray-700' : 'text-gray-500'}>Cover photo set</span>
                    </div>
                    <div className="flex items-center gap-2 text-sm">
                      <span className={r.videoCount > 0 ? 'text-green-600' : 'text-gray-400'}>{r.videoCount > 0 ? '[x]' : '[ ]'}</span>
                      <span className={r.videoCount > 0 ? 'text-gray-700' : 'text-gray-500'}>Video uploaded ({r.videoCount}) — optional</span>
                    </div>
                  </div>
                )}

                {/* Ready banner */}
                {isDraft && r?.ready && (
                  <div className="mt-3 bg-green-50 border border-green-200 rounded-xl px-4 py-2 text-sm text-green-700 font-medium">
                    Ready to submit for verification!
                  </div>
                )}

                {listing.avgRating != null && listing.avgRating > 0 && (
                  <p className="text-sm text-gray-500 mt-2">
                    ★ {listing.avgRating.toFixed(1)} · {listing.reviewCount} review{listing.reviewCount !== 1 ? 's' : ''}
                  </p>
                )}
                {listing.status === 'PENDING_VERIFICATION' && (
                  <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 mt-2">
                    <p className="text-sm text-yellow-700">Your listing is under review. This usually takes 24 hours.</p>
                  </div>
                )}
                {listing.status === 'REJECTED' && (
                  <div className="mt-2">
                    {listing.verificationNotes ? (
                      <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                        <p className="text-xs font-semibold text-red-600">Rejection reason:</p>
                        <p className="text-sm text-red-700 mt-0.5">{listing.verificationNotes}</p>
                      </div>
                    ) : (
                      <p className="text-xs text-red-600">Listing was rejected. Please update and resubmit.</p>
                    )}
                  </div>
                )}

                {/* Action buttons */}
                <div className="flex flex-wrap gap-2 mt-3">
                  <button type="button" onClick={() => setEditingListing(listing)}
                    className="text-sm px-3 py-1 rounded-lg border border-gray-300 hover:bg-gray-50 font-medium transition">
                    Edit
                  </button>
                  {!hasNoMedia && (
                    <button type="button" onClick={() => loadMedia(listing.id)}
                      className="text-sm px-3 py-1 rounded-lg border border-gray-300 hover:bg-gray-50 transition">
                      {mediaPanel === listing.id ? 'Hide media' : 'Photos / Videos'}
                    </button>
                  )}
                  {/* UI #6: Guest Preview button */}
                  {mediaPanel === listing.id && mediaItems.length > 0 && (
                    <button type="button" onClick={() => setGuestPreview(listing.id)}
                      className="text-sm px-3 py-1 rounded-lg border border-blue-300 text-blue-600 hover:bg-blue-50 transition">
                      Guest Preview
                    </button>
                  )}
                  {isDraft && (
                    <button type="button" onClick={() => handleSubmitForVerification(listing.id)}
                      disabled={actionLoading === listing.id || !(r?.ready)}
                      title={!(r?.ready) ? 'Upload at least 1 photo and set a cover photo first' : ''}
                      className="text-sm px-3 py-1 rounded-lg bg-orange-500 text-white hover:bg-orange-600 disabled:opacity-50 disabled:cursor-not-allowed transition">
                      {actionLoading === listing.id ? 'Submitting...' : 'Submit for Verification'}
                    </button>
                  )}
                  {(listing.status === 'VERIFIED' || listing.status === 'PAUSED' || listing.status === 'REJECTED') && (
                    <button type="button" onClick={() => handleUnpublishToDraft(listing.id)}
                      disabled={actionLoading === listing.id}
                      className="text-sm px-3 py-1 rounded-lg border border-yellow-400 text-yellow-700 hover:bg-yellow-50 disabled:opacity-50 transition">
                      {actionLoading === listing.id ? 'Unpublishing...' : 'Unpublish to Draft'}
                    </button>
                  )}
                </div>

                {/* ── Media Panel ──────────────────────── */}
                {mediaPanel === listing.id && (
                  <div className="mt-4 border-t pt-4 space-y-4">
                    {/* Existing media grid */}
                    {mediaItems.length > 0 ? (
                      <div>
                        <p className="text-xs font-semibold text-gray-500 mb-2">
                          {mediaItems.filter((m) => m.type === 'PHOTO').length} photo(s), {mediaItems.filter((m) => m.type === 'VIDEO').length} video(s)
                        </p>
                        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
                          {mediaItems.map((m, idx) => (
                            <div key={m.id} className={`relative rounded-lg overflow-hidden border-2 group transition ${m.isPrimary ? 'border-orange-500 ring-2 ring-orange-200' : 'border-gray-200'}`}>
                              {m.type === 'PHOTO' ? (
                                <img src={m.url} alt={`Photo ${idx + 1}`} className="w-full h-24 object-cover" />
                              ) : (
                                <div className="w-full h-24 bg-gray-800 flex flex-col items-center justify-center">
                                  <span className="text-white text-lg">▶</span>
                                  <span className="text-white text-[10px] mt-1">Video</span>
                                </div>
                              )}
                              {/* UI #3: Cover photo badge */}
                              {m.isPrimary && (
                                <span className="absolute top-1 left-1 text-[10px] bg-orange-500 text-white px-1.5 py-0.5 rounded font-semibold">
                                  COVER
                                </span>
                              )}
                              {!m.isPrimary && m.type === 'PHOTO' && (
                                <span className="absolute top-1 left-1 text-[10px] bg-black/40 text-white px-1.5 py-0.5 rounded">
                                  {m.type}
                                </span>
                              )}
                              {/* Set as cover button on hover */}
                              {m.type === 'PHOTO' && !m.isPrimary && isDraft && (
                                <button type="button" onClick={() => handleSetPrimary(listing.id, m.id)}
                                  disabled={actionLoading === 'set-primary'}
                                  className="absolute bottom-1 right-1 text-[10px] bg-orange-500 text-white px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition font-medium">
                                  Set as cover
                                </button>
                              )}
                            </div>
                          ))}
                        </div>
                      </div>
                    ) : (
                      <p className="text-sm text-gray-400">No media yet — upload photos to submit for verification</p>
                    )}

                    {/* ── Upload Section ───────────────── */}
                    {isDraft && (
                      <div className="bg-gray-50 rounded-xl p-4 space-y-3">
                        <p className="text-sm font-semibold text-gray-700">Upload media</p>

                        {/* Drag-and-drop zone + file picker */}
                        <div
                          onDrop={(e) => handleDrop(e, listing.id)}
                          onDragOver={handleDragOver}
                          className="border-2 border-dashed border-gray-300 rounded-xl p-6 text-center hover:border-orange-400 hover:bg-orange-50/30 transition-colors cursor-pointer"
                          onClick={() => fileInputRef.current?.click()}
                        >
                          <p className="text-2xl mb-1">📷</p>
                          <p className="text-sm font-medium text-gray-700">Drop photos & videos here, or click to browse</p>
                          <p className="text-xs text-gray-400 mt-1">Select multiple files at once — JPG, PNG, MP4 — max 50MB each</p>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <button type="button" onClick={() => cameraInputRef.current?.click()}
                            className="text-sm px-4 py-2 rounded-lg bg-white border border-gray-300 hover:bg-gray-50 transition font-medium">
                            📷 Take Photo
                          </button>
                        </div>
                        <input ref={fileInputRef} type="file" accept="image/*,video/*" multiple className="hidden"
                          onChange={(e) => { handleFilesSelected(e.target.files, listing.id); if (e.target) e.target.value = ''; }} />
                        <input ref={cameraInputRef} type="file" accept="image/*" capture="environment" className="hidden"
                          onChange={(e) => { handleFilesSelected(e.target.files, listing.id); if (e.target) e.target.value = ''; }} />

                        {/* UI #10: Upload Queue Dashboard */}
                        {uploadJobs.length > 0 && (
                          <div className="space-y-2">
                            <div className="flex items-center justify-between">
                              <p className="text-xs text-gray-500">
                                {uploadJobs.filter((j) => j.status === 'done').length}/{uploadJobs.length} uploaded
                                {uploadJobs.some((j) => j.status === 'uploading') && ' — uploading...'}
                              </p>
                              {!bulkUploading && uploadJobs.some((j) => j.status === 'queued') && (
                                <button type="button" onClick={() => handleBulkUpload(listing.id)}
                                  className="text-sm px-4 py-1.5 rounded-lg bg-orange-500 text-white hover:bg-orange-600 font-medium transition">
                                  Upload All ({uploadJobs.filter((j) => j.status === 'queued').length})
                                </button>
                              )}
                            </div>
                            {/* Upload progress bar */}
                            <div className="w-full h-1.5 bg-gray-200 rounded-full overflow-hidden">
                              <div className="h-full bg-orange-500 rounded-full transition-all duration-300"
                                style={{ width: `${(uploadJobs.filter((j) => j.status === 'done').length / uploadJobs.length) * 100}%` }} />
                            </div>
                            {/* Thumbnail strip */}
                            <div className="flex gap-2 flex-wrap">
                              {uploadJobs.map((job, idx) => (
                                <div key={idx} className="relative w-16 h-16 rounded-lg overflow-hidden border">
                                  {job.mediaType === 'PHOTO' ? (
                                    <img src={job.preview} alt={job.file.name} className="w-full h-full object-cover" />
                                  ) : (
                                    <div className="w-full h-full bg-gray-800 flex items-center justify-center">
                                      <span className="text-white text-xs">▶</span>
                                    </div>
                                  )}
                                  {/* Status overlay */}
                                  <div className={`absolute inset-0 flex items-center justify-center text-white text-xs font-bold
                                    ${job.status === 'done' ? 'bg-green-500/50' : job.status === 'uploading' ? 'bg-orange-500/50' : job.status === 'error' ? 'bg-red-500/50' : 'bg-black/20'}`}>
                                    {job.status === 'done' && '✓'}
                                    {job.status === 'uploading' && '...'}
                                    {job.status === 'error' && '!'}
                                  </div>
                                  {/* Remove button for queued jobs */}
                                  {job.status === 'queued' && (
                                    <button type="button" onClick={() => removeUploadJob(idx)}
                                      className="absolute -top-1 -right-1 bg-red-500 text-white rounded-full w-4 h-4 flex items-center justify-center text-[10px] leading-none">
                                      x
                                    </button>
                                  )}
                                </div>
                              ))}
                            </div>
                          </div>
                        )}

                        {/* First upload hint */}
                        {mediaItems.length === 0 && uploadJobs.length === 0 && (
                          <p className="text-xs text-gray-400">Tip: The first photo you upload will be set as the cover photo automatically.</p>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Guest Preview Modal */}
      {guestPreview && listings.find((l) => l.id === guestPreview) && (
        <GuestPreviewModal listing={listings.find((l) => l.id === guestPreview)!} />
      )}

      {/* Edit Listing Modal */}
      {editingListing && (
        <EditListingModal
          listing={editingListing}
          token={token}
          onClose={() => setEditingListing(null)}
          onSaved={() => { setEditingListing(null); fetchListings(); }}
        />
      )}

      </>)}
    </div>
  );
}
