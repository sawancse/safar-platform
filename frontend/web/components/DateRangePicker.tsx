'use client';

import { useState } from 'react';

interface Props {
  from: string;
  to: string;
  onChange: (from: string, to: string) => void;
}

const PRESETS = [
  { label: 'Today', fn: () => { const d = new Date().toISOString().split('T')[0]; return [d, d]; } },
  { label: 'This week', fn: () => {
    const now = new Date();
    const start = new Date(now); start.setDate(now.getDate() - now.getDay());
    const end = new Date(start); end.setDate(start.getDate() + 6);
    return [start.toISOString().split('T')[0], end.toISOString().split('T')[0]];
  }},
  { label: 'This month', fn: () => {
    const now = new Date();
    return [new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0],
            new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().split('T')[0]];
  }},
  { label: 'Last 30 days', fn: () => {
    const end = new Date(); const start = new Date(); start.setDate(end.getDate() - 30);
    return [start.toISOString().split('T')[0], end.toISOString().split('T')[0]];
  }},
  { label: 'This year', fn: () => {
    const y = new Date().getFullYear();
    return [`${y}-01-01`, `${y}-12-31`];
  }},
];

export default function DateRangePicker({ from, to, onChange }: Props) {
  const [open, setOpen] = useState(false);

  return (
    <div className="relative">
      <button
        onClick={() => setOpen(!open)}
        className="flex items-center gap-2 border rounded-lg px-3 py-2 text-sm bg-white hover:bg-gray-50 transition"
      >
        <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        <span className="text-gray-700">
          {from && to ? `${from} → ${to}` : 'Select dates'}
        </span>
        {from && (
          <button onClick={(e) => { e.stopPropagation(); onChange('', ''); }} className="text-gray-400 hover:text-gray-600">
            ✕
          </button>
        )}
      </button>

      {open && (
        <div className="absolute top-full left-0 mt-1 bg-white border rounded-xl shadow-xl p-4 z-50 min-w-[300px]">
          <div className="flex flex-wrap gap-2 mb-3">
            {PRESETS.map(p => (
              <button key={p.label} onClick={() => { const [f, t] = p.fn(); onChange(f, t); setOpen(false); }}
                className="px-3 py-1 bg-gray-100 hover:bg-orange-100 text-xs font-medium rounded-full transition">
                {p.label}
              </button>
            ))}
          </div>
          <div className="flex gap-2 items-center">
            <input type="date" value={from} onChange={(e) => onChange(e.target.value, to)}
              className="border rounded-lg px-2 py-1.5 text-sm flex-1" />
            <span className="text-gray-400 text-sm">to</span>
            <input type="date" value={to} onChange={(e) => onChange(from, e.target.value)}
              className="border rounded-lg px-2 py-1.5 text-sm flex-1" />
          </div>
          <button onClick={() => setOpen(false)}
            className="mt-3 w-full bg-orange-500 text-white text-sm font-medium py-2 rounded-lg hover:bg-orange-600 transition">
            Apply
          </button>
        </div>
      )}
    </div>
  );
}
