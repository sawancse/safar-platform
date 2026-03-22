'use client';

import { useEffect, useState, useCallback } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import { formatPaise } from '@/lib/utils';
import type { Experience, ExperienceCategory } from '@/types';

const CATEGORIES: ExperienceCategory[] = ['CULINARY', 'CULTURAL', 'WELLNESS', 'ADVENTURE', 'CREATIVE'];

const CATEGORY_ICONS: Record<string, string> = {
  CULINARY:  '🍳',
  CULTURAL:  '🏛️',
  WELLNESS:  '🧘',
  ADVENTURE: '🏔️',
  CREATIVE:  '🎨',
};

export const dynamic = 'force-dynamic';

export default function ExperiencesPage() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const [experiences, setExperiences] = useState<Experience[]>([]);
  const [loading, setLoading] = useState(true);

  const city = searchParams.get('city') ?? '';
  const category = searchParams.get('category') ?? '';

  const fetchExperiences = useCallback(async () => {
    setLoading(true);
    try {
      const params: { city?: string; category?: string } = {};
      if (city) params.city = city;
      if (category) params.category = category;
      const result = await api.getExperiences(params);
      setExperiences(result ?? []);
    } catch {
      setExperiences([]);
    } finally {
      setLoading(false);
    }
  }, [city, category]);

  useEffect(() => {
    fetchExperiences();
  }, [fetchExperiences]);

  function updateParam(key: string, value: string) {
    const params = new URLSearchParams(searchParams.toString());
    if (value) params.set(key, value);
    else params.delete(key);
    router.push(`/experiences?${params.toString()}`);
  }

  function formatDuration(mins: number): string {
    if (mins < 60) return `${mins} min`;
    const hrs = Math.floor(mins / 60);
    const remainder = mins % 60;
    return remainder > 0 ? `${hrs}h ${remainder}m` : `${hrs}h`;
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-2">Experiences</h1>
        <p className="text-gray-500">Discover unique activities curated by local hosts across India</p>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-4 mb-8">
        <div className="flex-1">
          <input
            className="w-full border rounded-lg px-4 py-2.5 text-sm"
            placeholder="Filter by city (e.g. Jaipur)"
            defaultValue={city}
            onBlur={(e) => updateParam('city', e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') updateParam('city', (e.target as HTMLInputElement).value);
            }}
          />
        </div>
        <div className="flex gap-2 flex-wrap">
          <button
            onClick={() => updateParam('category', '')}
            className={`px-4 py-2 rounded-full text-sm font-medium border transition ${
              !category
                ? 'bg-orange-500 text-white border-orange-500'
                : 'border-gray-300 hover:border-orange-400 hover:text-orange-500'
            }`}
          >
            All
          </button>
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              onClick={() => updateParam('category', cat)}
              className={`px-4 py-2 rounded-full text-sm font-medium border transition ${
                category === cat
                  ? 'bg-orange-500 text-white border-orange-500'
                  : 'border-gray-300 hover:border-orange-400 hover:text-orange-500'
              }`}
            >
              {CATEGORY_ICONS[cat]} {cat.charAt(0) + cat.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      {/* Results */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="rounded-xl bg-gray-100 h-80 animate-pulse" />
          ))}
        </div>
      ) : experiences.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {experiences.map((exp) => (
            <div
              key={exp.id}
              className="border rounded-2xl overflow-hidden hover:shadow-md transition"
            >
              {/* Image placeholder */}
              <div className="h-40 bg-gradient-to-br from-orange-100 to-pink-100 flex items-center justify-center">
                <span className="text-5xl">{CATEGORY_ICONS[exp.category] ?? '🎯'}</span>
              </div>

              <div className="p-4">
                <div className="flex items-start justify-between gap-2 mb-1">
                  <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-orange-100 text-orange-700">
                    {exp.category}
                  </span>
                  {exp.avgRating != null && exp.avgRating > 0 && (
                    <span className="text-sm font-semibold text-gray-700">
                      ★ {exp.avgRating.toFixed(1)}
                      {exp.reviewCount != null && (
                        <span className="text-gray-400 font-normal"> ({exp.reviewCount})</span>
                      )}
                    </span>
                  )}
                </div>

                <h3 className="font-semibold text-lg mt-2 line-clamp-2">{exp.title}</h3>
                <p className="text-sm text-gray-500 mt-1">{exp.city}</p>

                <div className="flex items-center justify-between mt-3 pt-3 border-t">
                  <div className="text-sm text-gray-500">
                    <span className="mr-3">&#9201; {formatDuration(exp.durationMinutes)}</span>
                    <span>by {exp.hostName}</span>
                  </div>
                  <span className="font-bold text-orange-600">
                    {formatPaise(exp.pricePaise)}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="text-center py-20 text-gray-400">
          <p className="text-5xl mb-4">🎭</p>
          <p className="text-lg font-medium">No experiences found</p>
          <p className="text-sm mt-1">Try a different city or category</p>
        </div>
      )}
    </div>
  );
}
