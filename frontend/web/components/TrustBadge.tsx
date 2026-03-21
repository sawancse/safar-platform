'use client';

interface Props {
  score: number;
  size?: 'sm' | 'md' | 'lg';
  showScore?: boolean;
}

const BADGES = [
  { min: 90, label: 'Superhost', color: 'bg-purple-100 text-purple-700 border-purple-300', icon: '\u2B50' },
  { min: 70, label: 'Verified Host', color: 'bg-green-100 text-green-700 border-green-300', icon: '\u2713' },
  { min: 50, label: 'Trusted', color: 'bg-blue-100 text-blue-700 border-blue-300', icon: '\u{1F6E1}' },
  { min: 30, label: 'ID Verified', color: 'bg-gray-100 text-gray-700 border-gray-300', icon: '\u2713' },
  { min: 0, label: 'New Host', color: 'bg-gray-50 text-gray-500 border-gray-200', icon: '' },
];

export default function TrustBadge({ score, size = 'sm', showScore = false }: Props) {
  const badge = BADGES.find(b => score >= b.min) || BADGES[BADGES.length - 1];

  const sizeClass = {
    sm: 'text-xs px-2 py-0.5',
    md: 'text-sm px-3 py-1',
    lg: 'text-base px-4 py-1.5',
  }[size];

  return (
    <span className={`inline-flex items-center gap-1 rounded-full border font-medium ${badge.color} ${sizeClass}`}>
      {badge.icon && <span>{badge.icon}</span>}
      {badge.label}
      {showScore && <span className="opacity-60">({score})</span>}
    </span>
  );
}
