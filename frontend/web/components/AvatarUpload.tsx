'use client';

import { useState, useRef } from 'react';
import { api } from '@/lib/api';

interface Props {
  currentAvatarUrl?: string;
  userName?: string;
  size?: number;
  onUpload?: (url: string) => void;
}

function getInitials(name?: string): string {
  if (!name) return '?';
  return name.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase();
}

function initialsColor(name?: string): string {
  const colors = ['#f97316', '#3b82f6', '#10b981', '#8b5cf6', '#ef4444', '#ec4899', '#14b8a6', '#f59e0b'];
  const hash = (name || '').split('').reduce((acc, c) => acc + c.charCodeAt(0), 0);
  return colors[hash % colors.length];
}

function resolveUrl(url?: string): string | undefined {
  if (!url) return undefined;
  if (url.startsWith('http')) return url;
  return `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}${url}`;
}

export default function AvatarUpload({ currentAvatarUrl, userName, size = 80, onUpload }: Props) {
  const [uploading, setUploading] = useState(false);
  const [avatarUrl, setAvatarUrl] = useState(currentAvatarUrl);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Client-side validation
    if (!file.type.startsWith('image/')) {
      alert('Please select an image file');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      alert('Image must be under 5MB');
      return;
    }

    setUploading(true);
    try {
      const token = localStorage.getItem('access_token') || '';
      const result = await api.uploadAvatar(file, token);
      const url = result.avatarUrl;
      setAvatarUrl(url);
      localStorage.setItem('user_avatar', url);
      onUpload?.(url);
    } catch {
      alert('Failed to upload photo');
    }
    setUploading(false);
    if (inputRef.current) inputRef.current.value = '';
  };

  const resolved = resolveUrl(avatarUrl);

  return (
    <div className="relative inline-block">
      <button
        onClick={() => inputRef.current?.click()}
        disabled={uploading}
        className="relative rounded-full overflow-hidden border-2 border-gray-200 hover:border-orange-400 transition-colors focus:outline-none focus:ring-2 focus:ring-orange-400"
        style={{ width: size, height: size }}
      >
        {resolved ? (
          <img src={resolved} alt={userName || 'Avatar'} className="w-full h-full object-cover" />
        ) : (
          <div
            className="w-full h-full flex items-center justify-center text-white font-bold"
            style={{ backgroundColor: initialsColor(userName), fontSize: size * 0.35 }}
          >
            {getInitials(userName)}
          </div>
        )}
        {/* Hover overlay */}
        <div className="absolute inset-0 bg-black/0 hover:bg-black/30 transition-colors flex items-center justify-center">
          <span className="text-white opacity-0 hover:opacity-100 transition-opacity text-xs font-medium">
            {uploading ? '...' : '\uD83D\uDCF7'}
          </span>
        </div>
      </button>
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={handleUpload}
      />
      {/* Camera badge */}
      {!resolved && (
        <div className="absolute -bottom-1 -right-1 w-6 h-6 bg-orange-500 rounded-full flex items-center justify-center text-white text-xs border-2 border-white">
          +
        </div>
      )}
    </div>
  );
}

// Export utility functions for use elsewhere
export { getInitials, initialsColor, resolveUrl as resolveAvatarUrl };
