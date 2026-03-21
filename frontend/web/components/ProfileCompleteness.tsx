'use client';

interface Props {
  completion: number; // 0-100
  size?: number;
}

export default function ProfileCompleteness({ completion, size = 80 }: Props) {
  const radius = (size - 8) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (completion / 100) * circumference;
  const color = completion >= 80 ? '#10b981' : completion >= 50 ? '#f59e0b' : '#ef4444';

  return (
    <div className="relative inline-flex items-center justify-center" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle cx={size/2} cy={size/2} r={radius} fill="none" stroke="#e5e7eb" strokeWidth={4} />
        <circle cx={size/2} cy={size/2} r={radius} fill="none" stroke={color} strokeWidth={4}
          strokeDasharray={circumference} strokeDashoffset={offset}
          strokeLinecap="round" className="transition-all duration-500" />
      </svg>
      <span className="absolute text-sm font-bold" style={{ color }}>{completion}%</span>
    </div>
  );
}
