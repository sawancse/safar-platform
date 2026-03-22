'use client';

import { useState, useCallback } from 'react';
import { api } from '@/lib/api';

interface MediaItem {
  id: string;
  url: string;
  type: string;
  isPrimary: boolean;
  category?: string;
}

const CATEGORIES = [
  'Bedroom', 'Bathroom', 'Kitchen', 'Living Room',
  'Exterior', 'View', 'Amenities', 'Video Tour'
];

interface Props {
  listingId: string;
  media: MediaItem[];
  onMediaChange: () => void; // callback to refresh media list
}

export default function PhotoManager({ listingId, media, onMediaChange }: Props) {
  const [uploading, setUploading] = useState(false);
  const [draggedId, setDraggedId] = useState<string | null>(null);
  const [editingCategory, setEditingCategory] = useState<string | null>(null);

  // Delete handler
  const handleDelete = async (mediaId: string) => {
    if (!confirm('Delete this photo?')) return;
    try {
      await api.deleteMedia(listingId, mediaId);
      onMediaChange();
    } catch (e) {
      alert('Failed to delete');
    }
  };

  // Set primary handler
  const handleSetPrimary = async (mediaId: string) => {
    try {
      await fetch(`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/v1/listings/${listingId}/media/${mediaId}/primary`, {
        method: 'PUT',
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
      });
      onMediaChange();
    } catch (e) {
      alert('Failed to set cover photo');
    }
  };

  // Category update handler
  const handleCategoryChange = async (mediaId: string, category: string) => {
    try {
      await api.updateMediaCategory(listingId, mediaId, category);
      setEditingCategory(null);
      onMediaChange();
    } catch (e) {
      alert('Failed to update category');
    }
  };

  // Upload handler — uses S3 presigned URL flow via media-service
  const handleUpload = async (files: FileList, category?: string, mediaType?: string) => {
    setUploading(true);
    for (const file of Array.from(files)) {
      const type = mediaType || (file.type.startsWith('video') ? 'VIDEO' : 'PHOTO');
      try {
        // Step 1: Get presigned URL from media-service
        const presign = await api.presignUpload(listingId, type, file.type);

        // Step 2: Upload file directly to S3
        await fetch(presign.uploadUrl, {
          method: 'PUT',
          headers: { 'Content-Type': file.type },
          body: file,
        });

        // Step 3: Confirm upload — triggers Kafka event → listing-service saves record
        await api.confirmUpload({
          mediaId: presign.mediaId,
          listingId,
          s3Key: presign.s3Key,
          mediaType: type,
          durationSeconds: 0,
        });

        // Step 4: If first photo, set as primary; if category provided, tag it
        // Small delay to let Kafka consumer create the record
        await new Promise(r => setTimeout(r, 500));
      } catch (e) {
        console.error('Upload failed:', e);
      }
    }
    setUploading(false);
    onMediaChange();
  };

  // Drag-and-drop reorder
  const handleDragStart = (id: string) => setDraggedId(id);

  const handleDrop = async (targetId: string) => {
    if (!draggedId || draggedId === targetId) return;
    const ids = media.map(m => m.id);
    const fromIdx = ids.indexOf(draggedId);
    const toIdx = ids.indexOf(targetId);
    ids.splice(fromIdx, 1);
    ids.splice(toIdx, 0, draggedId);
    setDraggedId(null);
    try {
      await api.reorderMedia(listingId, ids);
      onMediaChange();
    } catch (e) {
      alert('Failed to reorder');
    }
  };

  const photos = media.filter(m => m.type === 'PHOTO' || m.type === 'IMAGE');
  const videos = media.filter(m => m.type === 'VIDEO');

  const resolveUrl = (url: string) => {
    if (url.startsWith('http')) return url;
    return `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}${url}`;
  };

  return (
    <div className="space-y-6">
      {/* Upload Section */}
      <div className="border-2 border-dashed border-gray-300 rounded-xl p-8 text-center hover:border-orange-400 transition-colors">
        <input
          type="file"
          multiple
          accept="image/*,video/*"
          onChange={(e) => e.target.files && handleUpload(e.target.files)}
          className="hidden"
          id="photo-upload"
          disabled={uploading}
        />
        <label htmlFor="photo-upload" className="cursor-pointer">
          <div className="text-4xl mb-2">📷</div>
          <p className="text-sm font-medium text-gray-900">
            {uploading ? 'Uploading...' : 'Drop photos & videos here, or click to browse'}
          </p>
          <p className="text-xs text-gray-500 mt-1">JPG, PNG, MP4 — max 50MB each</p>
        </label>
      </div>

      {/* Special uploads row */}
      <div className="flex gap-3">
        <label className="flex-1 flex items-center gap-2 px-4 py-3 border rounded-lg cursor-pointer hover:bg-gray-50">
          <span>🔄</span>
          <span className="text-sm font-medium">360° Panorama</span>
          <input type="file" accept="image/*" className="hidden"
            onChange={(e) => e.target.files && handleUpload(e.target.files, undefined, 'PANORAMA')} />
        </label>
        <label className="flex-1 flex items-center gap-2 px-4 py-3 border rounded-lg cursor-pointer hover:bg-gray-50">
          <span>📐</span>
          <span className="text-sm font-medium">Floor Plan</span>
          <input type="file" accept="image/*,.pdf,.svg" className="hidden"
            onChange={(e) => e.target.files && handleUpload(e.target.files, undefined, 'FLOOR_PLAN')} />
        </label>
        <label className="flex-1 flex items-center gap-2 px-4 py-3 border rounded-lg cursor-pointer hover:bg-gray-50">
          <span>🎥</span>
          <span className="text-sm font-medium">Video Tour</span>
          <input type="file" accept="video/*" className="hidden"
            onChange={(e) => e.target.files && handleUpload(e.target.files, 'Video Tour', 'VIDEO')} />
        </label>
      </div>

      {/* Photo count */}
      <div className="flex items-center justify-between">
        <h4 className="font-semibold text-gray-900">
          Photos ({photos.length}) {videos.length > 0 && `· Videos (${videos.length})`}
        </h4>
        <p className="text-xs text-gray-500">Drag to reorder · First photo is the cover</p>
      </div>

      {/* Photo Grid with management */}
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
        {media.map((item) => (
          <div
            key={item.id}
            draggable
            onDragStart={() => handleDragStart(item.id)}
            onDragOver={(e) => e.preventDefault()}
            onDrop={() => handleDrop(item.id)}
            className={`relative group rounded-lg overflow-hidden aspect-[4/3] bg-gray-100 border-2 transition-all ${
              draggedId === item.id ? 'opacity-50 border-orange-400' : 'border-transparent hover:border-gray-300'
            }`}
          >
            {item.type === 'VIDEO' ? (
              <video src={resolveUrl(item.url)} className="w-full h-full object-cover" muted />
            ) : (
              <img src={resolveUrl(item.url)} alt="" className="w-full h-full object-cover" loading="lazy" />
            )}

            {/* Badges */}
            <div className="absolute top-2 left-2 flex gap-1">
              {item.isPrimary && (
                <span className="px-2 py-0.5 bg-orange-500 text-white text-xs rounded-full font-medium">Cover</span>
              )}
              {item.type === 'VIDEO' && (
                <span className="px-2 py-0.5 bg-blue-500 text-white text-xs rounded-full">Video</span>
              )}
              {item.category && (
                <span className="px-2 py-0.5 bg-gray-800/70 text-white text-xs rounded-full">{item.category}</span>
              )}
            </div>

            {/* Action buttons (visible on hover) */}
            <div className="absolute inset-0 bg-black/0 group-hover:bg-black/30 transition-colors flex items-end justify-between p-2 opacity-0 group-hover:opacity-100">
              <div className="flex gap-1">
                {!item.isPrimary && (
                  <button
                    onClick={() => handleSetPrimary(item.id)}
                    className="px-2 py-1 bg-white text-xs rounded font-medium hover:bg-orange-50"
                    title="Set as cover"
                  >
                    Set Cover
                  </button>
                )}
                <button
                  onClick={() => setEditingCategory(editingCategory === item.id ? null : item.id)}
                  className="px-2 py-1 bg-white text-xs rounded font-medium hover:bg-blue-50"
                  title="Set category"
                >
                  Tag
                </button>
              </div>
              <button
                onClick={() => handleDelete(item.id)}
                className="w-7 h-7 bg-red-500 hover:bg-red-600 text-white rounded-full flex items-center justify-center text-xs"
                title="Delete"
              >
                ✕
              </button>
            </div>

            {/* Category dropdown */}
            {editingCategory === item.id && (
              <div className="absolute bottom-0 left-0 right-0 bg-white border-t p-2 shadow-lg z-10">
                <select
                  value={item.category || ''}
                  onChange={(e) => handleCategoryChange(item.id, e.target.value)}
                  className="w-full text-xs border rounded px-2 py-1"
                >
                  <option value="">No category</option>
                  {CATEGORIES.map(cat => (
                    <option key={cat} value={cat}>{cat}</option>
                  ))}
                </select>
              </div>
            )}
          </div>
        ))}
      </div>

      {media.length === 0 && (
        <div className="text-center py-12 text-gray-400">
          <p className="text-5xl mb-3">📸</p>
          <p className="font-medium">No photos yet</p>
          <p className="text-sm">Upload at least 5 photos to attract more guests</p>
        </div>
      )}
    </div>
  );
}
