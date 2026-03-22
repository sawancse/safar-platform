'use client';

import { useState } from 'react';

interface Props {
  text: string;
  title?: string;
}

export default function ExpandableText({ text, title = 'About this Safar' }: Props) {
  const [expanded, setExpanded] = useState(false);

  if (!text) return null;

  return (
    <div>
      <h2 className="text-xl font-semibold mb-2">{title}</h2>
      <p className={`text-gray-700 leading-relaxed ${expanded ? 'whitespace-pre-line' : 'line-clamp-1'}`}>
        {text}
      </p>
      {text.length > 80 && (
        <button
          onClick={() => setExpanded(!expanded)}
          className="text-sm font-semibold text-orange-600 hover:text-orange-700 mt-1"
        >
          {expanded ? 'Show less' : 'More'}
        </button>
      )}
    </div>
  );
}
