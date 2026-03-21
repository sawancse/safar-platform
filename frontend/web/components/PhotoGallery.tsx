'use client';

import { useState, useEffect, useRef, useCallback, useMemo } from 'react';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

function resolveUrl(url: string): string {
  if (url.startsWith('http')) return url;
  return `${API_URL}${url}`;
}

interface MediaItem {
  id: string;
  url: string;
  type: string;
  isPrimary: boolean;
  caption?: string;
  category?: string;
}

interface PhotoGalleryProps {
  media: MediaItem[];
  listingName?: string;
  panoramaUrl?: string;
  videoTourUrl?: string;
}

// Photo sub-categories for property photos
const PHOTO_CATEGORIES = [
  { key: 'all', label: 'All' },
  { key: 'room', label: 'Room' },
  { key: 'bathroom', label: 'Bathroom' },
  { key: 'pool', label: 'Pool' },
  { key: 'facade', label: 'Facade' },
  { key: 'spa', label: 'Spa' },
  { key: 'restaurant', label: 'Restaurant' },
  { key: 'reception', label: 'Reception' },
  { key: 'common', label: 'Common Area' },
  { key: 'gym', label: 'Gym' },
  { key: 'conference', label: 'Conference' },
  { key: 'washroom', label: 'Washroom' },
  { key: 'garden', label: 'Garden' },
  { key: 'view', label: 'View' },
];

// Main gallery tabs
type GalleryTab = 'property' | 'traveller' | '360view';

function inferCategoryFromText(item: MediaItem): string | null {
  const url = item.url.toLowerCase();
  const caption = (item.caption || '').toLowerCase();
  const text = url + ' ' + caption;
  if (text.includes('toilet') || text.includes('bath') || text.includes('washroom') || text.includes('shower')) return 'bathroom';
  if (text.includes('pool') || text.includes('swim')) return 'pool';
  if (text.includes('facade') || text.includes('exterior') || text.includes('front') || text.includes('building') || text.includes('entrance')) return 'facade';
  if (text.includes('spa') || text.includes('wellness') || text.includes('massage')) return 'spa';
  if (text.includes('restaurant') || text.includes('dining') || text.includes('food') || text.includes('buffet')) return 'restaurant';
  if (text.includes('reception') || text.includes('lobby') || text.includes('checkin') || text.includes('check-in')) return 'reception';
  if (text.includes('common') || text.includes('lounge') || text.includes('sitting')) return 'common';
  if (text.includes('gym') || text.includes('fitness') || text.includes('workout')) return 'gym';
  if (text.includes('conference') || text.includes('meeting') || text.includes('banquet')) return 'conference';
  if (text.includes('garden') || text.includes('terrace') || text.includes('lawn') || text.includes('outdoor')) return 'garden';
  if (text.includes('view') || text.includes('balcony') || text.includes('scenic') || text.includes('sunset')) return 'view';
  if (text.includes('traveller') || text.includes('guest') || text.includes('traveler') || text.includes('visitor')) return 'traveller';
  if (text.includes('room') || text.includes('bedroom') || text.includes('bed') || text.includes('suite')) return 'room';
  if (text.includes('kitchen') || text.includes('cook')) return 'kitchen';
  return null; // unknown
}

// Distribute uncategorized photos across categories by position
const ORDER_CATEGORIES = ['facade', 'reception', 'room', 'room', 'bathroom', 'room', 'view', 'common', 'restaurant', 'garden', 'pool', 'gym', 'spa'];

function assignCategories(items: MediaItem[]): Map<string, string> {
  const assignments = new Map<string, string>();
  let orderIdx = 0;

  for (const item of items) {
    // 1. Use explicit category from API
    const apiCat = (item.category || '').toLowerCase().trim();
    if (apiCat) {
      // Map backend values to our display keys
      const mapped: Record<string, string> = {
        bedroom: 'room', living: 'common', exterior: 'facade', amenities: 'common', video_tour: 'room',
        bathroom: 'bathroom', kitchen: 'kitchen', view: 'view',
      };
      assignments.set(item.id, mapped[apiCat] || apiCat);
      continue;
    }

    // 2. Try text-based inference
    const inferred = inferCategoryFromText(item);
    if (inferred) {
      assignments.set(item.id, inferred);
      continue;
    }

    // 3. Distribute by position in upload order
    const positionCat = ORDER_CATEGORIES[orderIdx % ORDER_CATEGORIES.length];
    assignments.set(item.id, positionCat);
    orderIdx++;
  }

  return assignments;
}

function inferCategory(item: MediaItem, categoryMap?: Map<string, string>): string {
  if (categoryMap?.has(item.id)) return categoryMap.get(item.id)!;
  const apiCat = (item.category || '').toLowerCase().trim();
  if (apiCat) return apiCat;
  return inferCategoryFromText(item) || 'room';
}

function isTravellerPhoto(item: MediaItem): boolean {
  const cat = inferCategory(item);
  return cat === 'traveller' || cat === 'guest';
}

export default function PhotoGallery({ media, listingName, panoramaUrl, videoTourUrl }: PhotoGalleryProps) {
  const [lightboxOpen, setLightboxOpen] = useState(false);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [mobileIndex, setMobileIndex] = useState(0);
  const [loaded, setLoaded] = useState<Set<number>>(new Set());
  const [zoomed, setZoomed] = useState(false);
  const [panOffset, setPanOffset] = useState({ x: 0, y: 0 });
  const [isMobile, setIsMobile] = useState(false);
  const [activeTab, setActiveTab] = useState<GalleryTab>('property');
  const [photoFilter, setPhotoFilter] = useState('all');
  const [viewMode, setViewMode] = useState<'grid' | 'single'>('grid'); // grid = category gallery, single = fullscreen viewer

  const touchStart = useRef<{ x: number; y: number } | null>(null);
  const pinchStartDist = useRef<number | null>(null);
  const lightboxImgRef = useRef<HTMLImageElement | null>(null);
  const thumbnailStripRef = useRef<HTMLDivElement | null>(null);

  const photos = media.filter(
    (m) => m.type === 'PHOTO' || m.type === 'IMAGE' || m.type?.startsWith('image')
  );
  const videos = media.filter(
    (m) => m.type === 'VIDEO' || m.type?.startsWith('video')
  );

  // Compute category assignments once for all photos
  const categoryMap = useMemo(() => assignCategories([...photos, ...videos]), [media]);

  // Split into property vs traveller photos
  const propertyPhotos = photos.filter(p => categoryMap.get(p.id) !== 'traveller');
  const travellerPhotos = photos.filter(p => categoryMap.get(p.id) === 'traveller');
  const has360 = !!(panoramaUrl || videoTourUrl);

  // Items for the current view (grid always shows all, lightbox filters)
  const allItems = [...photos, ...videos];

  // Lightbox items based on active tab + filter
  const lightboxItems = activeTab === 'property'
    ? (photoFilter === 'all'
        ? [...propertyPhotos, ...videos]
        : propertyPhotos.filter(p => inferCategory(p) === photoFilter))
    : activeTab === 'traveller'
      ? travellerPhotos
      : []; // 360 view handled separately

  // Available sub-categories (only show tabs that have photos)
  const availableCategories = PHOTO_CATEGORIES.filter(cat => {
    if (cat.key === 'all') return true;
    return propertyPhotos.some(p => inferCategory(p) === cat.key);
  });

  // Detect mobile
  useEffect(() => {
    const check = () => setIsMobile(window.innerWidth < 768);
    check();
    window.addEventListener('resize', check);
    return () => window.removeEventListener('resize', check);
  }, []);

  // Body scroll lock
  useEffect(() => {
    if (lightboxOpen) document.body.style.overflow = 'hidden';
    else document.body.style.overflow = '';
    return () => { document.body.style.overflow = ''; };
  }, [lightboxOpen]);

  // Keyboard navigation
  useEffect(() => {
    if (!lightboxOpen) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') closeLightbox();
      if (e.key === 'ArrowLeft') prevPhoto();
      if (e.key === 'ArrowRight') nextPhoto();
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [lightboxOpen, lightboxItems.length]);

  // Scroll thumbnail strip to current
  useEffect(() => {
    if (lightboxOpen && thumbnailStripRef.current) {
      const thumb = thumbnailStripRef.current.children[currentIndex] as HTMLElement;
      if (thumb) thumb.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
    }
  }, [currentIndex, lightboxOpen]);

  const markLoaded = useCallback((index: number) => {
    setLoaded((prev) => new Set(prev).add(index));
  }, []);

  const openLightbox = (index: number, mode: 'grid' | 'single' = 'grid') => {
    setCurrentIndex(index);
    setZoomed(false);
    setPanOffset({ x: 0, y: 0 });
    setActiveTab('property');
    setPhotoFilter('all');
    setViewMode(mode);
    setLightboxOpen(true);
  };

  const closeLightbox = () => {
    setLightboxOpen(false);
    setZoomed(false);
    setPanOffset({ x: 0, y: 0 });
  };

  const prevPhoto = () => {
    const len = lightboxItems.length || 1;
    setCurrentIndex((i) => (i === 0 ? len - 1 : i - 1));
    setZoomed(false);
    setPanOffset({ x: 0, y: 0 });
  };

  const nextPhoto = () => {
    const len = lightboxItems.length || 1;
    setCurrentIndex((i) => (i === len - 1 ? 0 : i + 1));
    setZoomed(false);
    setPanOffset({ x: 0, y: 0 });
  };

  // Touch handlers for mobile carousel
  const onMobileTouchStart = (e: React.TouchEvent) => {
    touchStart.current = { x: e.touches[0].clientX, y: e.touches[0].clientY };
  };
  const onMobileTouchEnd = (e: React.TouchEvent) => {
    if (!touchStart.current) return;
    const dx = e.changedTouches[0].clientX - touchStart.current.x;
    if (Math.abs(dx) > 50) {
      if (dx > 0) setMobileIndex((i) => (i === 0 ? allItems.length - 1 : i - 1));
      else setMobileIndex((i) => (i === allItems.length - 1 ? 0 : i + 1));
    }
    touchStart.current = null;
  };

  // Touch handlers for lightbox
  const onLightboxTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length === 2) {
      pinchStartDist.current = Math.hypot(
        e.touches[0].clientX - e.touches[1].clientX,
        e.touches[0].clientY - e.touches[1].clientY
      );
    } else {
      touchStart.current = { x: e.touches[0].clientX, y: e.touches[0].clientY };
    }
  };
  const onLightboxTouchEnd = (e: React.TouchEvent) => {
    if (pinchStartDist.current !== null) { pinchStartDist.current = null; return; }
    if (!touchStart.current) return;
    const dx = e.changedTouches[0].clientX - touchStart.current.x;
    const dy = e.changedTouches[0].clientY - touchStart.current.y;
    if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 50) {
      if (dx > 0) prevPhoto(); else nextPhoto();
    } else if (dy > 100) closeLightbox();
    touchStart.current = null;
  };
  const onLightboxTouchMove = (e: React.TouchEvent) => {
    if (e.touches.length === 2 && pinchStartDist.current !== null) {
      const dist = Math.hypot(
        e.touches[0].clientX - e.touches[1].clientX,
        e.touches[0].clientY - e.touches[1].clientY
      );
      if (dist > pinchStartDist.current * 1.3) setZoomed(true);
      else if (dist < pinchStartDist.current * 0.7) { setZoomed(false); setPanOffset({ x: 0, y: 0 }); }
    }
  };

  const onDoubleClick = () => {
    if (zoomed) { setZoomed(false); setPanOffset({ x: 0, y: 0 }); }
    else setZoomed(true);
  };
  const onMouseMove = (e: React.MouseEvent) => {
    if (!zoomed || !lightboxImgRef.current) return;
    const rect = lightboxImgRef.current.getBoundingClientRect();
    setPanOffset({ x: (0.5 - (e.clientX - rect.left) / rect.width) * 100, y: (0.5 - (e.clientY - rect.top) / rect.height) * 100 });
  };

  // Empty state
  if (allItems.length === 0 && !has360) {
    return (
      <div className="rounded-2xl overflow-hidden bg-gray-100 aspect-video flex items-center justify-center text-gray-300 text-6xl">
        <svg className="w-16 h-16" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909M3.75 21h16.5A2.25 2.25 0 0022.5 18.75V5.25A2.25 2.25 0 0020.25 3H3.75A2.25 2.25 0 001.5 5.25v13.5A2.25 2.25 0 003.75 21z" />
        </svg>
      </div>
    );
  }

  function renderGridImage(item: MediaItem, idx: number, className: string, eager: boolean) {
    const isVideo = item.type === 'VIDEO' || item.type?.startsWith('video');
    return (
      <button key={item.id} onClick={() => openLightbox(idx)}
        className={`relative overflow-hidden group ${className}`}>
        <div className="absolute inset-0 bg-gray-200 animate-pulse" />
        {isVideo ? (
          <div className="absolute inset-0 bg-gray-800 flex items-center justify-center">
            <svg className="w-12 h-12 text-white/80" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z" /></svg>
          </div>
        ) : (
          <img src={resolveUrl(item.url)}
            alt={listingName ? `${listingName} - ${idx + 1}` : `Photo ${idx + 1}`}
            className={`absolute inset-0 w-full h-full object-cover group-hover:brightness-90 transition-all duration-300 ${loaded.has(idx) ? 'opacity-100' : 'opacity-0'}`}
            loading={eager ? 'eager' : 'lazy'}
            onLoad={() => markLoaded(idx)} />
        )}
        {/* Category label */}
        {item.category && (
          <span className="absolute bottom-1 left-1 bg-black/50 text-white text-[10px] px-1.5 py-0.5 rounded capitalize">
            {item.category}
          </span>
        )}
      </button>
    );
  }

  // Group photos by category for grid gallery view
  function getPhotosByCategory(): { category: string; label: string; items: MediaItem[] }[] {
    const catGroups = new Map<string, MediaItem[]>();
    for (const item of [...propertyPhotos, ...videos]) {
      const cat = categoryMap.get(item.id) || 'room';
      if (!catGroups.has(cat)) catGroups.set(cat, []);
      catGroups.get(cat)!.push(item);
    }
    // Map category keys to display labels
    const labelMap: Record<string, string> = {
      room: 'Room', bathroom: 'Bathroom', pool: 'Pool', facade: 'Facade & Exterior',
      spa: 'Spa & Wellness', restaurant: 'Restaurant & Dining', reception: 'Reception & Lobby',
      common: 'Common Area', gym: 'Gym & Fitness', conference: 'Conference Room',
      washroom: 'Washroom', garden: 'Garden & Outdoor', view: 'View & Balcony',
    };
    // Order categories logically
    const catOrder = ['facade', 'reception', 'room', 'bathroom', 'kitchen', 'common', 'view', 'restaurant', 'garden', 'pool', 'gym', 'spa', 'conference'];
    const groups: { category: string; label: string; items: MediaItem[] }[] = [];
    for (const cat of catOrder) {
      if (catGroups.has(cat)) groups.push({ category: cat, label: labelMap[cat] || cat.charAt(0).toUpperCase() + cat.slice(1), items: catGroups.get(cat)! });
    }
    // Add any remaining categories not in the order
    for (const [cat, items] of catGroups) {
      if (!catOrder.includes(cat)) groups.push({ category: cat, label: labelMap[cat] || cat.charAt(0).toUpperCase() + cat.slice(1), items });
    }
    return groups;
  }

  function renderGridGallery() {
    const groups = getPhotosByCategory();
    const allItemsFlat = [...propertyPhotos, ...videos];

    return (
      <div className="fixed inset-0 bg-white z-50 flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b shrink-0">
          <h2 className="text-lg font-semibold text-gray-800">
            {listingName} — All Photos ({allItemsFlat.length})
          </h2>
          <button onClick={closeLightbox}
            className="p-2 rounded-full hover:bg-gray-100 text-gray-500 hover:text-gray-800 transition">
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Gallery tabs */}
        {(travellerPhotos.length > 0 || has360) && (
          <div className="flex gap-2 px-6 py-3 border-b shrink-0">
            <button onClick={() => setActiveTab('property')}
              className={`text-sm px-4 py-1.5 rounded-full font-medium transition ${
                activeTab === 'property' ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}>
              Property Photos ({propertyPhotos.length + videos.length})
            </button>
            {travellerPhotos.length > 0 && (
              <button onClick={() => setActiveTab('traveller')}
                className={`text-sm px-4 py-1.5 rounded-full font-medium transition ${
                  activeTab === 'traveller' ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}>
                Traveller Photos ({travellerPhotos.length})
              </button>
            )}
            {has360 && (
              <button onClick={() => { setActiveTab('360view'); setViewMode('single'); }}
                className={`text-sm px-4 py-1.5 rounded-full font-medium transition ${
                  activeTab === '360view' ? 'bg-gray-900 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}>
                360° View
              </button>
            )}
          </div>
        )}

        {/* Scrollable content */}
        <div className="flex-1 overflow-y-auto px-6 py-4">
          {activeTab === 'property' && groups.map((group) => (
            <div key={group.category} className="mb-8">
              <h3 className="text-base font-semibold text-gray-700 mb-3 sticky top-0 bg-white py-1">
                {group.label} ({group.items.length})
              </h3>
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2">
                {group.items.map((item) => {
                  const globalIdx = allItemsFlat.findIndex(i => i.id === item.id);
                  const isVideo = item.type === 'VIDEO' || item.type?.startsWith('video');
                  return (
                    <button key={item.id}
                      onClick={() => { setCurrentIndex(globalIdx >= 0 ? globalIdx : 0); setViewMode('single'); }}
                      className="relative aspect-[4/3] rounded-lg overflow-hidden group">
                      {isVideo ? (
                        <div className="w-full h-full bg-gray-800 flex items-center justify-center">
                          <svg className="w-10 h-10 text-white/80" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z" /></svg>
                        </div>
                      ) : (
                        <img src={resolveUrl(item.url)} alt={item.caption || group.label}
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                          loading="lazy" />
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          ))}

          {activeTab === 'traveller' && (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2">
              {travellerPhotos.map((item, i) => (
                <button key={item.id}
                  onClick={() => { setCurrentIndex(i); setViewMode('single'); setActiveTab('traveller'); }}
                  className="relative aspect-[4/3] rounded-lg overflow-hidden group">
                  <img src={resolveUrl(item.url)} alt={item.caption || 'Traveller photo'}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                    loading="lazy" />
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    );
  }

  function renderLightbox() {
    // Grid gallery view (category-based)
    if (viewMode === 'grid') {
      return renderGridGallery();
    }

    // 360 view tab
    if (activeTab === '360view') {
      return (
        <div className="fixed inset-0 bg-black z-50 flex flex-col">
          <div className="flex items-center justify-between px-4 py-3 flex-shrink-0">
            <div className="text-white/70 text-sm font-medium">{listingName}</div>
            <button onClick={closeLightbox} className="text-white hover:text-white/80 p-2">
              <svg className="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          {renderGalleryTabs()}
          <div className="flex-1 flex items-center justify-center p-4">
            {panoramaUrl ? (
              <iframe src={panoramaUrl} className="w-full h-full rounded-xl border-0" allowFullScreen loading="lazy" />
            ) : videoTourUrl ? (
              <video src={resolveUrl(videoTourUrl)} controls className="max-w-full max-h-[80vh] rounded-xl" />
            ) : (
              <p className="text-white/50 text-sm">No 360 view available</p>
            )}
          </div>
        </div>
      );
    }

    const items = lightboxItems;
    if (items.length === 0) {
      return (
        <div className="fixed inset-0 bg-black z-50 flex flex-col">
          <div className="flex items-center justify-between px-4 py-3">
            <div className="text-white/70 text-sm font-medium">{listingName}</div>
            <button onClick={closeLightbox} className="text-white hover:text-white/80 p-2">
              <svg className="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          {renderGalleryTabs()}
          <div className="flex-1 flex items-center justify-center">
            <p className="text-white/50 text-sm">No photos in this category</p>
          </div>
        </div>
      );
    }

    const safeIndex = Math.min(currentIndex, items.length - 1);
    const current = items[safeIndex];
    const isVideo = current.type === 'VIDEO' || current.type?.startsWith('video');
    const src = resolveUrl(current.url);
    const caption = current.caption || current.category || '';

    return (
      <div className="fixed inset-0 bg-black z-50 flex flex-col"
        onTouchStart={onLightboxTouchStart} onTouchEnd={onLightboxTouchEnd} onTouchMove={onLightboxTouchMove}>
        {/* Top bar */}
        <div className="flex items-center justify-between px-4 py-3 flex-shrink-0">
          <div className="flex items-center gap-3">
            <button onClick={() => setViewMode('grid')}
              className="text-white/70 hover:text-white text-sm font-medium flex items-center gap-1 transition">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
              </svg>
              All Photos
            </button>
            <span className="text-white/40">|</span>
            <span className="text-white/70 text-sm">{listingName}</span>
          </div>
          <button onClick={closeLightbox} className="text-white hover:text-white/80 p-2">
            <svg className="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Gallery tabs */}
        {renderGalleryTabs()}

        {/* Sub-category filter for property photos */}
        {activeTab === 'property' && availableCategories.length > 2 && (
          <div className="flex gap-1.5 px-4 pb-2 overflow-x-auto flex-shrink-0">
            {availableCategories.map(cat => (
              <button key={cat.key}
                onClick={() => { setPhotoFilter(cat.key); setCurrentIndex(0); }}
                className={`text-xs px-3 py-1 rounded-full whitespace-nowrap transition ${
                  photoFilter === cat.key
                    ? 'bg-white text-black font-semibold'
                    : 'bg-white/10 text-white/70 hover:bg-white/20'
                }`}>
                {cat.label}
                {cat.key !== 'all' && (
                  <span className="ml-1 text-white/40">
                    ({propertyPhotos.filter(p => inferCategory(p) === cat.key).length})
                  </span>
                )}
              </button>
            ))}
          </div>
        )}

        {/* Main image area */}
        <div className="flex-1 flex items-center justify-center relative min-h-0 px-4">
          {items.length > 1 && (
            <button onClick={prevPhoto}
              className="absolute left-4 z-10 bg-white/20 hover:bg-white/30 text-white rounded-full w-12 h-12 flex items-center justify-center transition">
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
          )}

          <div className="max-w-5xl w-full flex items-center justify-center"
            style={{ maxHeight: '75vh' }} onDoubleClick={onDoubleClick} onMouseMove={onMouseMove}>
            {isVideo ? (
              <video key={src} src={src} controls className="max-w-full max-h-[75vh] object-contain" />
            ) : (
              <img ref={lightboxImgRef} key={src} src={src}
                alt={listingName ? `${listingName} - ${safeIndex + 1}` : `Photo ${safeIndex + 1}`}
                className={`max-w-full object-contain transition-all duration-300 ${zoomed ? 'cursor-move' : 'cursor-zoom-in'}`}
                style={{
                  maxHeight: '75vh',
                  transform: zoomed ? `scale(2) translate(${panOffset.x}%, ${panOffset.y}%)` : 'scale(1)',
                  transition: zoomed ? 'none' : 'transform 0.3s ease',
                }}
                loading="eager" draggable={false} />
            )}
          </div>

          {items.length > 1 && (
            <button onClick={nextPhoto}
              className="absolute right-4 z-10 bg-white/20 hover:bg-white/30 text-white rounded-full w-12 h-12 flex items-center justify-center transition">
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          )}
        </div>

        {/* Caption + Counter */}
        <div className="text-center py-2 flex-shrink-0">
          <div className="text-white/90 text-sm font-medium">{safeIndex + 1} / {items.length}</div>
          {caption && <div className="text-white/60 text-xs mt-0.5 capitalize">{caption}</div>}
        </div>

        {/* Thumbnail strip */}
        {items.length > 1 && (
          <div ref={thumbnailStripRef}
            className="flex gap-1.5 overflow-x-auto px-4 pb-4 flex-shrink-0" style={{ scrollbarWidth: 'thin' }}>
            {items.map((item, i) => {
              const isVid = item.type === 'VIDEO' || item.type?.startsWith('video');
              return (
                <button key={item.id}
                  onClick={() => { setCurrentIndex(i); setZoomed(false); setPanOffset({ x: 0, y: 0 }); }}
                  className={`shrink-0 w-16 h-[60px] rounded overflow-hidden border-2 transition ${
                    i === safeIndex ? 'border-white' : 'border-transparent opacity-50 hover:opacity-80'
                  }`}>
                  {isVid ? (
                    <div className="w-full h-full bg-gray-800 flex items-center justify-center text-white">
                      <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z" /></svg>
                    </div>
                  ) : (
                    <img src={resolveUrl(item.url)} alt={`Thumbnail ${i + 1}`}
                      className="w-full h-full object-cover" loading="lazy" />
                  )}
                </button>
              );
            })}
          </div>
        )}
      </div>
    );
  }

  function renderGalleryTabs() {
    const tabs: { key: GalleryTab; label: string; count?: number; icon: string }[] = [
      { key: 'property', label: 'Property Photos', count: propertyPhotos.length + videos.length, icon: '📸' },
    ];
    if (travellerPhotos.length > 0) {
      tabs.push({ key: 'traveller', label: 'Traveller Photos', count: travellerPhotos.length, icon: '🧳' });
    }
    if (has360) {
      tabs.push({ key: '360view', label: '360° View', icon: '🌐' });
    }
    if (tabs.length <= 1) return null;

    return (
      <div className="flex gap-1 px-4 pb-3 flex-shrink-0">
        {tabs.map(tab => (
          <button key={tab.key}
            onClick={() => { setActiveTab(tab.key); setPhotoFilter('all'); setCurrentIndex(0); }}
            className={`text-sm px-4 py-1.5 rounded-full font-medium transition ${
              activeTab === tab.key
                ? 'bg-white text-black'
                : 'bg-white/10 text-white/70 hover:bg-white/20'
            }`}>
            {tab.icon} {tab.label}
            {tab.count != null && <span className="ml-1 text-xs opacity-60">({tab.count})</span>}
          </button>
        ))}
      </div>
    );
  }

  // --- RENDER: Mobile Carousel ---
  if (isMobile) {
    return (
      <>
        <div className="relative w-full overflow-hidden rounded-xl" style={{ aspectRatio: '4/3' }}
          onTouchStart={onMobileTouchStart} onTouchEnd={onMobileTouchEnd}>
          <div className="absolute inset-0 bg-gray-200 animate-pulse" />
          <img src={resolveUrl(allItems[mobileIndex].url)}
            alt={listingName ? `${listingName} - ${mobileIndex + 1}` : `Photo ${mobileIndex + 1}`}
            className={`relative w-full h-full object-cover cursor-pointer transition-opacity duration-300 ${loaded.has(mobileIndex) ? 'opacity-100' : 'opacity-0'}`}
            loading="eager" onLoad={() => markLoaded(mobileIndex)}
            onClick={() => openLightbox(mobileIndex)} />
          <div className="absolute top-3 right-3 bg-black/60 text-white text-xs px-2.5 py-1 rounded-full font-medium">
            {mobileIndex + 1} / {allItems.length}
          </div>
          {allItems.length > 1 && allItems.length <= 10 && (
            <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5">
              {allItems.map((_, i) => (
                <div key={i} className={`w-1.5 h-1.5 rounded-full transition-all ${i === mobileIndex ? 'bg-white w-3' : 'bg-white/50'}`} />
              ))}
            </div>
          )}
          {/* 360 badge */}
          {has360 && (
            <button onClick={() => { setActiveTab('360view'); setLightboxOpen(true); }}
              className="absolute bottom-3 left-3 bg-black/60 text-white text-xs px-2.5 py-1 rounded-full font-medium hover:bg-black/80 transition">
              🌐 360° View
            </button>
          )}
        </div>
        {lightboxOpen && renderLightbox()}
      </>
    );
  }

  // --- RENDER: Desktop 5-Grid ---
  const gridImages = allItems.slice(0, 5);
  const count = gridImages.length;

  return (
    <>
      <div className="rounded-xl overflow-hidden" style={{ height: '400px' }}>
        {count === 1 && (
          <div className="relative h-full">
            {renderGridImage(gridImages[0], 0, 'w-full h-full', true)}
            <ShowAllButton count={allItems.length} has360={has360} onClick={() => openLightbox(0)}
              on360={() => { setActiveTab('360view'); setLightboxOpen(true); }} />
          </div>
        )}
        {count === 2 && (
          <div className="relative grid grid-cols-2 gap-1 h-full">
            {renderGridImage(gridImages[0], 0, 'h-full', true)}
            {renderGridImage(gridImages[1], 1, 'h-full', false)}
            <ShowAllButton count={allItems.length} has360={has360} onClick={() => openLightbox(0)}
              on360={() => { setActiveTab('360view'); setLightboxOpen(true); }} />
          </div>
        )}
        {count === 3 && (
          <div className="relative grid grid-cols-2 gap-1 h-full">
            {renderGridImage(gridImages[0], 0, 'h-full', true)}
            <div className="grid grid-rows-2 gap-1 h-full">
              {renderGridImage(gridImages[1], 1, 'h-full', false)}
              {renderGridImage(gridImages[2], 2, 'h-full', false)}
            </div>
            <ShowAllButton count={allItems.length} has360={has360} onClick={() => openLightbox(0)}
              on360={() => { setActiveTab('360view'); setLightboxOpen(true); }} />
          </div>
        )}
        {count === 4 && (
          <div className="relative grid grid-cols-2 gap-1 h-full">
            {renderGridImage(gridImages[0], 0, 'h-full', true)}
            <div className="grid grid-cols-2 grid-rows-2 gap-1 h-full">
              {renderGridImage(gridImages[1], 1, 'h-full', false)}
              {renderGridImage(gridImages[2], 2, 'h-full', false)}
              {renderGridImage(gridImages[3], 3, 'h-full col-span-2', false)}
            </div>
            <ShowAllButton count={allItems.length} has360={has360} onClick={() => openLightbox(0)}
              on360={() => { setActiveTab('360view'); setLightboxOpen(true); }} />
          </div>
        )}
        {count >= 5 && (
          <div className="relative grid grid-cols-4 grid-rows-2 gap-1 h-full">
            {renderGridImage(gridImages[0], 0, 'col-span-2 row-span-2', true)}
            {renderGridImage(gridImages[1], 1, '', false)}
            {renderGridImage(gridImages[2], 2, '', false)}
            {renderGridImage(gridImages[3], 3, '', false)}
            {renderGridImage(gridImages[4], 4, '', false)}
            <ShowAllButton count={allItems.length} has360={has360} onClick={() => openLightbox(0)}
              on360={() => { setActiveTab('360view'); setLightboxOpen(true); }} />
          </div>
        )}
      </div>
      {lightboxOpen && renderLightbox()}
    </>
  );
}

function ShowAllButton({ count, has360, onClick, on360 }: {
  count: number; has360: boolean; onClick: () => void; on360: () => void;
}) {
  return (
    <div className="absolute bottom-4 right-4 flex gap-2 z-10">
      {has360 && (
        <button onClick={on360}
          className="bg-white text-gray-800 text-sm font-medium px-4 py-2 rounded-full shadow-md hover:shadow-lg transition flex items-center gap-2">
          🌐 360° View
        </button>
      )}
      <button onClick={onClick}
        className="bg-white text-gray-800 text-sm font-medium px-4 py-2 rounded-full shadow-md hover:shadow-lg transition flex items-center gap-2">
        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
        </svg>
        Show all photos{count > 5 ? ` (${count})` : ''}
      </button>
    </div>
  );
}
