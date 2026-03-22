'use client';

interface Props {
  propertyType: string;
  city?: string;
}

export default function VisualSearchButton({ propertyType, city }: Props) {
  const searchUrl = `/search?type=${encodeURIComponent(propertyType)}${city ? `&city=${encodeURIComponent(city)}` : ''}`;

  return (
    <a
      href={searchUrl}
      className="inline-flex items-center gap-2 px-4 py-2 bg-white/90 hover:bg-white rounded-full shadow-lg text-sm font-medium text-gray-900 transition-all"
    >
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
      Find similar stays
    </a>
  );
}
